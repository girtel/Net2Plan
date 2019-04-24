/*******************************************************************************
 * Copyright (c) 2017 Pablo Pavon Marino and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the 2-clause BSD License 
 * which accompanies this distribution, and is available at
 * https://opensource.org/licenses/BSD-2-Clause
 *
 * Contributors:
 *     Pablo Pavon Marino and others - initial API and implementation
 *******************************************************************************/




package com.net2plan.examples.ocnbook.reports;

import java.io.Closeable;
import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.net2plan.interfaces.networkDesign.Configuration;
import com.net2plan.interfaces.networkDesign.Demand;
import com.net2plan.interfaces.networkDesign.IReport;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.MulticastDemand;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkElement;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.simulation.IEventProcessor;
import com.net2plan.interfaces.simulation.SimEvent;
import com.net2plan.utils.ClassLoaderUtils;
import com.net2plan.utils.InputParameter;
import com.net2plan.utils.Pair;
import com.net2plan.utils.StringUtils;
import com.net2plan.utils.Triple;

/**
 * This report receives as an input a network design. Then it creates, for each demand in the default layer, a traffic peak of the user-defined intensity, and applies the user-defined network 
 * reaction algorithm to simulate the network behavior. With this information, collects statistics on blocked demands etc. and provides statistics. 
 * 
 * @net2plan.keywords 
 * @net2plan.ocnbooksections 
 * @net2plan.inputParameters 
 * @author Pablo Pavon-Marino
 */
public class Report_trafficFluctuationAnalysis implements IReport
{
	private InputParameter provisioningAlgorithm = new InputParameter ("provisioningAlgorithm" , "#eventProcessor#" , "Algorithm to process failure events");
	private InputParameter capacityAnalysys_updateLinkCapacitiesInDesign = new InputParameter ("capacityAnalysys_updateLinkCapacitiesInDesign" , false , "If true, the link capacities are updated with the worst case occupied capacity in the links, removing any previous capacities");
	private InputParameter trafficPeakMultiplicativeFactor = new InputParameter ("trafficPeakMultiplicativeFactor" , (double) 3.0 , "The traffic of a demand is multiplied by this factor" , 0 , false , Double.MAX_VALUE , true);
	private InputParameter maxNumPeakyDemands = new InputParameter ("maxNumPeakyDemands" , (int) 5 , "The demands with higher traffic are tested for having peaks, up to this amount" , 0 , Integer.MAX_VALUE);
	
	private Map<Long , PerDemandInfo> info_d = new HashMap<> ();
	private Map<Long , PerDemandInfo> info_md = new HashMap<> ();
	private Map<Long , PerLinkInfo> info_e = new HashMap<> ();
	
	private IEventProcessor algorithm;
	private final static DecimalFormat dfAv = new DecimalFormat("#.#######");
	private final static DecimalFormat df_6 = new DecimalFormat("#.######");
	
	
	@Override
	public String executeReport(NetPlan netPlan, Map<String, String> reportParameters, Map<String, String> net2planParameters)
	{
		/* Initialize all InputParameter objects defined in this object (this uses Java reflection) */
		InputParameter.initializeAllInputParameterFieldsOfObject(this, reportParameters);

		String algorithmFile = reportParameters.get("provisioningAlgorithm_file");
		String algorithmName = reportParameters.get("provisioningAlgorithm_classname");
		String algorithmParam = reportParameters.get("provisioningAlgorithm_parameters");
		if (algorithmFile.isEmpty() || algorithmName.isEmpty()) throw new Net2PlanException("A provisioning algorithm must be defined");

		Map<String, String> algorithmParameters = StringUtils.stringToMap(algorithmParam);
		netPlan.setAllNodesFailureState(true);
		for (NetworkLayer layer : netPlan.getNetworkLayers ())
			netPlan.setAllLinksFailureState(true , layer);
		
		/* Initialize statistics variables */
		for(NetworkLayer layer : netPlan.getNetworkLayers())
		{
	    	for (Demand d : netPlan.getDemands(layer)) info_d.put(d.getId() , new PerDemandInfo());
	    	for (MulticastDemand d : netPlan.getMulticastDemands(layer)) info_md.put(d.getId() , new PerDemandInfo());
	    	for (Link d : netPlan.getLinks(layer)) info_e.put(d.getId() , new PerLinkInfo());
		}

		/* the up and oversubscribed links that were set as down, are set to up again */
		if (!netPlan.getLinksDownAllLayers().isEmpty() || !netPlan.getNodesDown().isEmpty()) throw new RuntimeException ("Bad");

		this.algorithm = ClassLoaderUtils.getInstance(new File(algorithmFile), algorithmName, IEventProcessor.class , null);
		List<Demand> demandsToApplyPeak = netPlan.getDemands().stream().
				filter(d->!d.isCoupled()).
				sorted((d1,d2)->Double.compare(d2.getOfferedTraffic (), d1.getOfferedTraffic())).
				limit(maxNumPeakyDemands.getInt()).
				collect(Collectors.toCollection(ArrayList::new));
		if (demandsToApplyPeak.size() > maxNumPeakyDemands.getInt()) throw new RuntimeException();
		final int NUMTRAFSTATES = demandsToApplyPeak.size();
		final BiFunction<Integer , NetPlan , Demand> getVaryingDemandFromTrafficState = (s , np) ->demandsToApplyPeak.get(s); 
		double checkProbStatesSumOne = 0;
		for (int s = 0; s < NUMTRAFSTATES ; s ++) 
		{
			final Demand peakyDemand = getVaryingDemandFromTrafficState.apply(s, netPlan);
			final double probThisState = 1.0 / NUMTRAFSTATES;
			checkProbStatesSumOne += probThisState;
			final double originalTrafficOfDemand = peakyDemand.getOfferedTraffic();
			
			final NetPlan auxNetPlan = testTrafficPeakAndRunProvisioningAlgorithmInAuxNetPlan (peakyDemand, originalTrafficOfDemand * trafficPeakMultiplicativeFactor.getDouble() , algorithmParameters , reportParameters , net2planParameters);

			for(int indexLayer = 0 ; indexLayer < auxNetPlan.getNumberOfLayers() ; indexLayer ++)
			{
				final NetworkLayer layer = auxNetPlan.getNetworkLayer (indexLayer);
		    	final SortedMap<Link,SortedMap<String,Pair<Double,Double>>> perLink_qos2occupationAndViolationMap = auxNetPlan.getAllLinksPerQosOccupationAndQosViolationMap(layer);
		    	for (Demand auxDemand : auxNetPlan.getDemands(layer))
		    		info_d.get(auxDemand.getId()).update(auxDemand, probThisState, peakyDemand, perLink_qos2occupationAndViolationMap);
		    	for (MulticastDemand auxDemand : auxNetPlan.getMulticastDemands(layer))
		    		info_md.get(auxDemand.getId()).update(auxDemand, probThisState, peakyDemand, perLink_qos2occupationAndViolationMap);
		    	for (Link auxLink : auxNetPlan.getLinks(layer))
		    		info_e.get(auxLink.getId()).update(auxLink, peakyDemand);
			}
		}
		if (Math.abs(checkProbStatesSumOne - 1.0) > 1e-3) throw new RuntimeException ();
		
		final String report = printReport(netPlan , reportParameters);

		/* At the end, optionally update the link capacities */
		if (capacityAnalysys_updateLinkCapacitiesInDesign.getBoolean())
		{
			for (NetworkLayer layer : netPlan.getNetworkLayers())
				for (Link e : netPlan.getLinks (layer))
					if (!e.isCoupled())
						e.setCapacity(info_e.get(e.getId()).getWcOccupiedCapacityGbps());
		}			
		
		return report;
	}

	@Override
	public String getDescription()
	{
		return "This report receives as an input a network design, the network recovery scheme algorithm, and the network risks (SRGs), and estimates the availability of the network (including individual availabilities for each demand), using an enumerative process that also provides an estimation of the estimation error. ";
	}

	@Override
	public List<Triple<String, String, String>> getParameters()
	{
		/* Returns the parameter information for all the InputParameter objects defined in this object (uses Java reflection) */
		return InputParameter.getInformationAllInputParameterFieldsOfObject(this);
	}

	@Override
	public String getTitle()
	{
		return "Availability report";
	}

	
	private String printReport(NetPlan np , Map<String,String> reportParameters)
	{
		StringBuilder out = new StringBuilder();
		out.append("<html><body>");
		out.append("<head><title>Traffic fluctuation report</title></head>");
		out.append("<h1>Introduction</h1>");
		out.append("<p>This report computes several performances for the different unicast and multicast demands in the different layers of the network." +
				"The network is supposed to be subject to the traffic peaks in some of the demands non-coupled in the default layer. </p>");
		out.append("<p>A numerical method is used that (i) enumerates the possible traffic peak states considered (one for each non-coupled demand, up to a maximum, starting with the ones with higher traffic), "
				+ "(ii) computes the network reaction" +
				"to that traffic peak using a used-defined algorithm, (iii) builds up statistical results from that.</p>");
		out.append("<h1>Global information</h1>");
		out.append("<h2>Input Parameters</h2>");
		out.append("<table border='1'>");
		out.append("<tr><th><b>Name</b></th><th><b>Value</b></th><th><b>Description</b></th>");
		for (Triple<String, String, String> paramDef : getParameters())
		{
			String name = paramDef.getFirst();
			String description = paramDef.getThird();
			String value = reportParameters.get(name);
			out.append("<tr><td>").append(name).append("</td><td>").append(value).append("</td><td>").append(description).append("</td></tr>");
		}
		out.append("</table>");

		out.append("<h1>PER LAYER INFORMATION SUMMARY</h1>");
		for (NetworkLayer layer : np.getNetworkLayers ())
		{
			out.append("<h2>Layer " + layer.getName () + ", index = " + layer.getIndex () + ", id = " + layer.getId () + "</h2>");
			final double totalOffered_d = np.getDemands(layer).stream().mapToDouble(d->d.getOfferedTraffic()).sum();
			final double totalOffered_md = np.getMulticastDemands(layer).stream().mapToDouble(d->d.getOfferedTraffic()).sum();
			final double weightedSurv_d =  totalOffered_d == 0? 0.0 : np.getDemands(layer).stream().mapToDouble(d->(d.getOfferedTraffic() / totalOffered_d) * info_d.get(d.getId()).getTimeWeightedFractionOfOkTraffic()).sum();
			final double weightedSurv_md =  totalOffered_md == 0? 0.0 : np.getMulticastDemands(layer).stream().mapToDouble(d->(d.getOfferedTraffic() / totalOffered_d) * info_md.get(d.getId()).getTimeWeightedFractionOfOkTraffic()).sum();

			final double worstSurv_d =  totalOffered_d == 0? 0.0 : np.getDemands(layer).stream().mapToDouble(d->info_d.get(d.getId()).getTimeWeightedFractionOfOkTraffic()).min().orElse(0.0);
			final double worstSurv_md =  totalOffered_md == 0? 0.0 : np.getMulticastDemands(layer).stream().mapToDouble(d->info_md.get(d.getId()).getTimeWeightedFractionOfOkTraffic()).min().orElse(0.0);

			out.append ("<ul>");
			out.append ("<li>UNICAST TRAFFIC: (Deterministic) Total offered traffic: " + df_6.format (totalOffered_d) + "</li>");
			if (np.getNumberOfDemands (layer) != 0)
			{
				out.append ("<li>UNICAST TRAFFIC: (Estimated) Average fraction of traffic satisfying SLAs: " + dfAv.format(weightedSurv_d)  + "</li>");
				out.append ("<li>UNICAST TRAFFIC: (Estimated) Worst among demand: fraction of traffic satisfying SLA: " + dfAv.format(worstSurv_d)   + "</li>");
				final double wcLat = np.getDemands(layer).stream().mapToDouble(d->info_d.get(d.getId()).getWcLatencyMs()).max().orElse(0.0);
				out.append ("<li>UNICAST TRAFFIC: (Estimated) Worst among demand: latencies (ms): " + (wcLat == Double.MAX_VALUE? "Inf" : df_6.format(wcLat))  + "</li>");
			}			
			out.append ("<li>MULTICAST TRAFFIC: (Deterministic) Total offered traffic: " + df_6.format (totalOffered_md) + "</li>");
			if (np.getNumberOfMulticastDemands(layer) != 0)
			{
				out.append ("<li>UNICAST TRAFFIC: (Estimated) Average fraction of traffic satisfying SLAs: " + dfAv.format(weightedSurv_md)  + "</li>");
				out.append ("<li>UNICAST TRAFFIC: (Estimated) Worst among demand: fraction of traffic satisfying SLA: " + dfAv.format(worstSurv_md)   + "</li>");
				out.append ("<li>MULTICAST TRAFFIC: (Estimated) Worst among demand: latencies (ms): " + df_6.format(np.getDemands(layer).stream().mapToDouble(d->info_md.get(d.getId()).getWcLatencyMs()).max().orElse(0.0))  + "</li>");
			}
		}

		out.append("<h1>PER LAYER INFORMATION DETAILED INFORMATION</h1>");
		for (NetworkLayer layer : np.getNetworkLayers ())
		{
			out.append("<h2>Layer " + layer.getName () + ", index = " + layer.getIndex () + ", id = " + layer.getId () + "</h2>");
			if (np.getNumberOfDemands(layer) != 0)
			{
				out.append("<table border='1'>");
				final List<String> headers = Arrays.asList("Demand" , "Origin node" , "Destination node" , "Offered traffic" ,
						"Average fraction of traffic within SLA" , 
						"WC latency (ms) / peaky demands" , 
						"WC blocking / peaky demands" , 
						"WC oversubscription / peaky demands");
				final List<Function<Demand , String>> vals_d = Arrays.asList(
						d->"<td>" + d.getIndex () + "</td>"  ,
						d->"<td>" + d.getIngressNode().getIndex() + " (" + d.getIngressNode().getName() + ")"+ "</td>",
						d->"<td>" + d.getEgressNode().getIndex() + " (" + d.getEgressNode().getName() + ")"+ "</td>",
						d->"<td>" + df_6.format(d.getOfferedTraffic())+ "</td>",
						d->printAvailability(info_d.get(d.getId()).getTimeWeightedFractionOfOkTraffic()) ,
						d->printWcAndSrgs(info_d.get(d.getId()).getWcLatencyMs() , info_d.get(d.getId()).getPeakyDemands_wcLat() , info_d.get(d.getId()).getWcLatencyMs() + Configuration.precisionFactor > d.getMaximumAcceptableE2EWorstCaseLatencyInMs()) ,
						d->printWcAndSrgs(info_d.get(d.getId()).getWcBlockingGbps() , info_d.get(d.getId()).getPeakyDemands_wcBlocking() , info_d.get(d.getId()).getWcBlockingGbps() > Configuration.precisionFactor) ,
						d->printWcAndSrgs(info_d.get(d.getId()).getWcQoSViolationGbps() , info_d.get(d.getId()).getPeakyDemands_wcQosViolation() , info_d.get(d.getId()).getWcQoSViolationGbps() > Configuration.precisionFactor)
						);
				out.append("<tr>"); for (String h : headers) out.append("<th><b>" + h + "</b></th>"); out.append("</tr>"); 
				for (Demand d : np.getDemands (layer))
				{
					out.append("<tr>");
					for (Function<Demand,String> f : vals_d) out.append(f.apply(d));
					out.append("</tr>");
				}
				out.append("</table>");
	
				out.append("<p></p><p></p>");
			}
			if (np.getNumberOfMulticastDemands(layer) != 0)
			{
				out.append("<table border='1'>");
				final List<String> headers = Arrays.asList("Multicast Demand" , "Origin node" , "# Destination nodes" , "Offered traffic",
						"Average fraction traffic within SLA" , 
						"WC latency (ms) / peaky demands" , 
						"WC blocking / peaky demands" , 
						"WC oversubscription / peaky demands");
				final List<Function<MulticastDemand , String>> vals_md = Arrays.asList(
						d->"<td>" + d.getIndex () + "</td>" ,
						d->"<td>" + d.getIngressNode().getIndex() + " (" + d.getIngressNode().getName() + ")"+ "</td>",
						d->"<td>" + d.getEgressNodes().size() + " nodes"+ "</td>",
						d->"<td>" + df_6.format(d.getOfferedTraffic())+ "</td>",
						d->printAvailability(info_md.get(d.getId()).getTimeWeightedFractionOfOkTraffic()) ,
						d->printWcAndSrgs(info_md.get(d.getId()).getWcLatencyMs() , info_md.get(d.getId()).getPeakyDemands_wcLat() , info_md.get(d.getId()).getWcLatencyMs() + Configuration.precisionFactor > d.getMaximumAcceptableE2EWorstCaseLatencyInMs()) ,
						d->printWcAndSrgs(info_md.get(d.getId()).getWcBlockingGbps() , info_md.get(d.getId()).getPeakyDemands_wcBlocking() , info_md.get(d.getId()).getWcBlockingGbps() > Configuration.precisionFactor) ,
						d->printWcAndSrgs(info_md.get(d.getId()).getWcQoSViolationGbps() , info_md.get(d.getId()).getPeakyDemands_wcQosViolation() , info_md.get(d.getId()).getWcQoSViolationGbps() > Configuration.precisionFactor)
						);
				out.append("<tr>"); for (String h : headers) out.append("<th><b>" + h + "</b></th>"); out.append("</tr>"); 
				for (MulticastDemand d : np.getMulticastDemands (layer))
				{
					out.append("<tr>");
					for (Function<MulticastDemand,String> f : vals_md) out.append(f.apply(d));
					out.append("</tr>");
				}
				out.append("</table>");
				out.append("<p></p><p></p>");
			}			
			if (np.getNumberOfLinks(layer) != 0)
			{
				out.append("<table border='1'>");
				final List<String> headers = Arrays.asList("Link" , "Origin node" , "Destination node" , "Capacity",
						"WC Occupied capacity / peaky demands");
				final List<Function<Link , String>> vals_e = Arrays.asList(
						d->"<td>" + d.getIndex () + "</td>" ,
						d->"<td>" + d.getOriginNode().getIndex() + "(" + d.getOriginNode().getName() + ")" + "</td>",
						d->"<td>" + d.getDestinationNode().getIndex() + "(" + d.getDestinationNode().getName() + ")" + "</td>",
						d->"<td>" + df_6.format(d.getCapacity()) + "</td>",
						d->printWcAndSrgs(info_e.get(d.getId()).getWcOccupiedCapacityGbps() , info_e.get(d.getId()).getPeakyDemands_wcOccupiedCapacity() , info_e.get(d.getId()).getWcOccupiedCapacityGbps() + Configuration.precisionFactor > d.getCapacity())
						);
				out.append("<tr>"); for (String h : headers) out.append("<th><b>" + h + "</b></th>"); out.append("</tr>"); 
				for (Link d : np.getLinks (layer))
				{
					out.append("<tr>");
					for (Function<Link,String> f : vals_e) out.append(f.apply(d));
					out.append("</tr>");
				}
				out.append("</table>");
				out.append("<p></p><p></p>");
			}			
		}
		return out.toString();
	}

	private static String printAvailability (double val)
	{
		return "<td>" + dfAv.format(val) + "</td>";
	}
	private static String printWcAndSrgs (double val , Set<Demand> demands , boolean highlight)
	{
		final StringBuffer st = new StringBuffer ();
		if (val == Double.MAX_VALUE) st.append("Inf"); else st.append(df_6.format(val));
		if (demands.isEmpty()) st.append (" (No Srg Info)");
		else st.append (" (" + demands.size() + " states. E.g. [" + demands.iterator().next() + "])");
		if (highlight) return "<td bgcolor=\"Yellow\">" + st.toString() + "</td>"; else return "<td>" + st.toString() + "</td>";  
	}

	private class PerDemandInfo
	{
		private double av = 0, surv = 0, wcLatMs = 0.0, wcOversubsGbps = 0.0 , wcBlockingGbps = 0.0;
		private Set<Demand> failureStates_wcLat = new HashSet<> () , failureStates_wcOversubs  = new HashSet<> (), failureStates_wcBlocking  = new HashSet<> ();
		public void update (NetworkElement element ,  double prob , Demand peakyDemand , SortedMap<Link,SortedMap<String,Pair<Double,Double>>> perLink_qos2occupationAndViolationMap)
		{
			if (!((element instanceof Demand) || (element instanceof MulticastDemand))) throw new RuntimeException ();
			final boolean isDemand = element instanceof Demand;
			final Demand d = isDemand? (Demand) element : null;
			final MulticastDemand md = isDemand? null : (MulticastDemand) element;
			final String qosType = isDemand? d.getQosType() : md.getQosType();
			final double latMs = isDemand? d.getWorstCasePropagationTimeInMs() : md.getWorseCasePropagationTimeInMs();
			final double oversubsGbps = (isDemand? d.getTraversedLinksAndCarriedTraffic(false).keySet() : md.getTraversedLinksAndCarriedTraffic(false).keySet()).stream().mapToDouble (e -> perLink_qos2occupationAndViolationMap.get(e).get(qosType).getSecond()).max().orElse(0.0);
			final double blockGbps = isDemand? d.getBlockedTraffic() : md.getBlockedTraffic();
			final boolean okLatency = isDemand? d.getMaximumAcceptableE2EWorstCaseLatencyInMs() + Configuration.precisionFactor > latMs : md.getMaximumAcceptableE2EWorstCaseLatencyInMs() + Configuration.precisionFactor > latMs; 
			final double trafFullyOkGbps = Math.max(0.0 ,  !okLatency? 0.0 : (isDemand? d.getCarriedTraffic() : md.getCarriedTraffic()) - oversubsGbps);
			final double fractionTrafficOk = (isDemand? d.getOfferedTraffic() : md.getOfferedTraffic()) < Configuration.precisionFactor? 0.0 : Math.min (1.0 , trafFullyOkGbps /  (isDemand? d.getOfferedTraffic() : md.getOfferedTraffic()));
			final boolean allTrafficOk = trafFullyOkGbps + Configuration.precisionFactor >= (isDemand? d.getOfferedTraffic(): md.getOfferedTraffic());
			if (allTrafficOk) av += prob;
			surv += prob * fractionTrafficOk; 
			if (latMs > wcLatMs) { wcLatMs = latMs; failureStates_wcLat = new HashSet<> (Arrays.asList(peakyDemand)); }
			else if (latMs == wcLatMs) { failureStates_wcLat.add(peakyDemand); }
			if (oversubsGbps > wcOversubsGbps) { wcOversubsGbps = oversubsGbps; failureStates_wcOversubs = new HashSet<> (Arrays.asList(peakyDemand)); }
			else if (oversubsGbps == wcOversubsGbps) { failureStates_wcOversubs.add(peakyDemand); }
			if (blockGbps > wcBlockingGbps) { wcBlockingGbps = blockGbps; failureStates_wcBlocking = new HashSet<> (Arrays.asList(peakyDemand)); }
			else if (blockGbps == wcBlockingGbps) { failureStates_wcBlocking.add(peakyDemand); }
		}
		public double getTimeWeightedFractionOfOkTraffic () { return surv; }
		public double getWcLatencyMs () { return wcLatMs; }
		public double getWcBlockingGbps () { return wcBlockingGbps; }
		public double getWcQoSViolationGbps () { return wcOversubsGbps; }
		public Set<Demand> getPeakyDemands_wcLat () { return failureStates_wcLat; }
		public Set<Demand> getPeakyDemands_wcQosViolation () { return failureStates_wcOversubs; }
		public Set<Demand> getPeakyDemands_wcBlocking () { return failureStates_wcBlocking; }
	}
	private class PerLinkInfo
	{
		private double wcOccupiedCapacityGbps = 0;
		private Set<Demand> peakyStates_wcOccupiedCapacity = new HashSet<> ();
		public void update (Link e ,  Demand peakyDemand)
		{
			final double occupiedCapacityGbps = e.getOccupiedCapacity();
			if (occupiedCapacityGbps > wcOccupiedCapacityGbps) { wcOccupiedCapacityGbps = occupiedCapacityGbps; peakyStates_wcOccupiedCapacity = new HashSet<> (Arrays.asList(peakyDemand)); }
			else if (occupiedCapacityGbps == wcOccupiedCapacityGbps) { peakyStates_wcOccupiedCapacity.add(peakyDemand); }
		}
		public double getWcOccupiedCapacityGbps  () { return wcOccupiedCapacityGbps; }
		public Set<Demand> getPeakyDemands_wcOccupiedCapacity () { return peakyStates_wcOccupiedCapacity; }
	}

	private NetPlan testTrafficPeakAndRunProvisioningAlgorithmInAuxNetPlan (Demand demand , double newOfferedTraffic , Map<String,String> algorithmParameters , Map<String,String> reportParameters , Map<String,String> net2planParameters)
	{
		final NetPlan auxNetPlan = demand.getNetPlan().copy();
		auxNetPlan.getDemandFromId(demand.getId()).setOfferedTraffic(newOfferedTraffic);

		
		this.algorithm.initialize(auxNetPlan , algorithmParameters , reportParameters , net2planParameters);
		try
		{
			/* Apply the reaction algorithm */
			algorithm.processEvent(auxNetPlan, new SimEvent(0, SimEvent.DestinationModule.EVENT_PROCESSOR , -1 , null));
		}
		catch (Throwable e)
		{
			try { ((Closeable) algorithm.getClass().getClassLoader()).close();  }
			catch (Throwable e1) { e.printStackTrace();}					

			throw (e);
		}
		
		/* Only close the class loader if it is a different one than this class. If problems: just do not close the class loader, and wait for garbage collection*/
		if (!this.getClass().getClassLoader().equals(algorithm.getClass().getClassLoader()))
		{
			try { ((Closeable) algorithm.getClass().getClassLoader()).close();	} catch (Throwable e1) { }					
		}
		return auxNetPlan;
	}

}
