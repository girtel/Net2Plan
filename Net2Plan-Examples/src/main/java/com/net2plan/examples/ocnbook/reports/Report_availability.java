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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.function.Function;

import com.net2plan.interfaces.networkDesign.Configuration;
import com.net2plan.interfaces.networkDesign.Demand;
import com.net2plan.interfaces.networkDesign.IReport;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.MulticastDemand;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkElement;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.interfaces.networkDesign.SharedRiskGroup;
import com.net2plan.interfaces.simulation.IEventProcessor;
import com.net2plan.interfaces.simulation.SimEvent;
import com.net2plan.libraries.SRGUtils;
import com.net2plan.utils.ClassLoaderUtils;
import com.net2plan.utils.InputParameter;
import com.net2plan.utils.Pair;
import com.net2plan.utils.StringUtils;
import com.net2plan.utils.Triple;

import cern.colt.function.tdouble.DoubleDoubleFunction;
import cern.colt.function.tdouble.DoubleFunction;
import cern.colt.list.tdouble.DoubleArrayList;
import cern.colt.list.tint.IntArrayList;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;

/**
 * This report receives as an input a network design, the network recovery scheme algorithm, and the network risks (SRGs), and estimates the availability of the 
 * network (including individual availabilities for each demand), using an enumerative process that also provides an estimation of the estimation error. 
 * 
 * @net2plan.keywords Network recovery: protection , Network recovery: restoration
 * @net2plan.ocnbooksections Section 3.7.3
 * @net2plan.inputParameters 
 * @author Pablo Pavon-Marino
 */
public class Report_availability implements IReport
{
	private InputParameter provisioningAlgorithm = new InputParameter ("provisioningAlgorithm" , "#eventProcessor#" , "Algorithm to process failure events");
	private InputParameter analyzeDoubleFailures = new InputParameter ("analyzeDoubleFailures" , true , "Indicates whether double failures are studied");
	private InputParameter defaultMTTFInHours = new InputParameter ("defaultMTTFInHours" , (double) 8748 , "Default value for Mean Time To Fail (hours)" , 0 , false , Double.MAX_VALUE , true);
	private InputParameter defaultMTTRInHours = new InputParameter ("defaultMTTRInHours" , (double) 12 , "Default value for Mean Time To Repair (hours)" , 0 , false , Double.MAX_VALUE , true);
	private InputParameter failureModel = new InputParameter ("failureModel" , "#select# perBidirectionalLinkBundle SRGfromNetPlan perNode perLink perDirectionalLinkBundle" , "Failure model selection: SRGfromNetPlan, perNode, perLink, perDirectionalLinkBundle, perBidirectionalLinkBundle");
	
	
	private Map<Demand , PerDemandInfo> info_d = new HashMap<> ();
	private Map<MulticastDemand , PerDemandInfo> info_md = new HashMap<> ();
	
//	private ArrayList<DoubleMatrix1D> availabilityClassicNoFailure_ld, availabilityWeightedNoFailure_ld, availabilityClassicNoFailure_lmd, availabilityWeightedNoFailure_lmd;
//	private ArrayList<DoubleMatrix1D> availabilityClassicTotal_ld, availabilityWeightedTotal_ld, availabilityClassicTotal_lmd, availabilityWeightedTotal_lmd;
	private double pi_excess;

	private IEventProcessor algorithm;
	final static DecimalFormat dfAv = new DecimalFormat("#.#######");
	
	
	@Override
	public String executeReport(NetPlan netPlan, Map<String, String> reportParameters, Map<String, String> net2planParameters)
	{
		/* Initialize all InputParameter objects defined in this object (this uses Java reflection) */
		InputParameter.initializeAllInputParameterFieldsOfObject(this, reportParameters);

		String algorithmFile = reportParameters.get("provisioningAlgorithm_file");
		String algorithmName = reportParameters.get("provisioningAlgorithm_classname");
		String algorithmParam = reportParameters.get("provisioningAlgorithm_parameters");
		if (algorithmFile.isEmpty() || algorithmName.isEmpty()) throw new Net2PlanException("A provisioning algorithm must be defined");
		final double PRECISION_FACTOR_hd = Double.parseDouble(net2planParameters.get("precisionFactor"));
		final double PRECISION_FACTOR_blocking = PRECISION_FACTOR_hd * PRECISION_FACTOR_hd;

		Map<String, String> algorithmParameters = StringUtils.stringToMap(algorithmParam);
		switch (failureModel.getString ())
		{
			case "SRGfromNetPlan":
				break;

			case "perNode":
				SRGUtils.configureSRGs(netPlan, defaultMTTFInHours.getDouble(), defaultMTTRInHours.getDouble(), SRGUtils.SharedRiskModel.PER_NODE, true);
				break;

			case "perLink":
				SRGUtils.configureSRGs(netPlan, defaultMTTFInHours.getDouble(), defaultMTTRInHours.getDouble(), SRGUtils.SharedRiskModel.PER_LINK, true);
				break;

			case "perDirectionalLinkBundle":
				SRGUtils.configureSRGs(netPlan, defaultMTTFInHours.getDouble(), defaultMTTRInHours.getDouble(), SRGUtils.SharedRiskModel.PER_DIRECTIONAL_LINK_BUNDLE, true);
				break;

			case "perBidirectionalLinkBundle":
				SRGUtils.configureSRGs(netPlan, defaultMTTFInHours.getDouble(), defaultMTTRInHours.getDouble(), SRGUtils.SharedRiskModel.PER_BIDIRECTIONAL_LINK_BUNDLE, true);
				break;

			default:
				throw new Net2PlanException("Failure model not valid. Please, check algorithm parameters description");
		}

		netPlan.setAllNodesFailureState(true);
		for (NetworkLayer layer : netPlan.getNetworkLayers ())
			netPlan.setAllLinksFailureState(true , layer);
		DoubleMatrix1D A_f = netPlan.getVectorSRGAvailability();
		List<SharedRiskGroup> srgs = netPlan.getSRGs();
		DoubleMatrix2D F_s = SRGUtils.getMatrixFailureState2SRG(srgs, true, analyzeDoubleFailures.getBoolean());
		
		/* Compute state probabilities (pi_s) */
		DoubleMatrix1D pi_s = SRGUtils.computeStateProbabilities(F_s , A_f);
		final double sum_pi_s = pi_s.zSum();
		
		final double pi_s0 = pi_s.get(0); 
		
		pi_excess = 1 - sum_pi_s;

		/* Initialize statistics variables */
//		availabilityClassicNoFailure_ld = new ArrayList<DoubleMatrix1D> ();
//		availabilityWeightedNoFailure_ld = new ArrayList<DoubleMatrix1D> ();
//		availabilityClassicNoFailure_lmd = new ArrayList<DoubleMatrix1D> ();
//		availabilityWeightedNoFailure_lmd = new ArrayList<DoubleMatrix1D> ();
//		availabilityClassicTotal_ld = new ArrayList<DoubleMatrix1D> (); 
//		availabilityWeightedTotal_ld = new ArrayList<DoubleMatrix1D> ();
//		availabilityClassicTotal_lmd = new ArrayList<DoubleMatrix1D> ();
//		availabilityWeightedTotal_lmd = new ArrayList<DoubleMatrix1D> ();
//		for(int indexLayer = 0 ; indexLayer < netPlan.getNumberOfLayers() ; indexLayer ++)
//		{
//			final NetworkLayer layer = netPlan.getNetworkLayer (indexLayer);
//			final int D = netPlan.getNumberOfDemands(layer);
//			final int MD = netPlan.getNumberOfMulticastDemands(layer);
//			availabilityClassicTotal_ld.add (DoubleFactory1D.dense.make (D,0.0));
//			availabilityWeightedTotal_ld.add (DoubleFactory1D.dense.make (D,0.0));
//			availabilityClassicTotal_lmd.add (DoubleFactory1D.dense.make (MD,0.0));
//			availabilityWeightedTotal_lmd.add (DoubleFactory1D.dense.make (MD,0.0));
//		}
//
//		List<Link> upAndOversubscribedLinksSetToDown = new LinkedList<Link> ();
//		if (considerTrafficInOversubscribedLinksAsLost.getBoolean())
//		{
//			for (NetworkLayer layer : netPlan.getNetworkLayers ())
//			{
////				System.out.println ("Layer " + layer + ", initial link capacity traffic: " + netPlan.getVectorLinkCapacity(layer));
////				System.out.println ("Layer " + layer + ", initial demand carried traffic: " + netPlan.getVectorDemandCarriedTraffic(layer));
//				for (Link e : netPlan.getLinks (layer)) if (e.isUp() && e.isOversubscribed()) { upAndOversubscribedLinksSetToDown.add (e); }
////				System.out.println ("Layer: " + layer + ", upAndOversubscribedLinksSetToDown: " + upAndOversubscribedLinksSetToDown);
//			}
//			netPlan.setLinksAndNodesFailureState(null , upAndOversubscribedLinksSetToDown , null , null);
//		}
//
//		System.out.println ("Links set to down all layers: " + netPlan.getLinksDownAllLayers());

		/* Statistics for the no-failure state */
		for(int indexLayer = 0 ; indexLayer < netPlan.getNumberOfLayers() ; indexLayer ++)
		{
			final NetworkLayer layer = netPlan.getNetworkLayer (indexLayer);
	    	final SortedMap<Link,SortedMap<String,Pair<Double,Double>>> perLink_qos2occupationAndViolationMap = netPlan.getAllLinksPerQosOccupationAndQosViolationMap(layer);
	    	for (Demand d : netPlan.getDemands(layer))
	    		info_d.get(d).update(d, pi_s0, new HashSet<> (), perLink_qos2occupationAndViolationMap);
	    	for (MulticastDemand d : netPlan.getMulticastDemands(layer))
	    		info_md.get(d).update(d, pi_s0, new HashSet<> (), perLink_qos2occupationAndViolationMap);
		}
//		System.out.println ("Before any failure state: availabilityClassicTotal_ld: " + availabilityClassicTotal_ld);

		
		/* the up and oversubscribed links that were set as down, are set to up again */
//		if (considerTrafficInOversubscribedLinksAsLost.getBoolean())
//			netPlan.setLinksAndNodesFailureState(upAndOversubscribedLinksSetToDown , null , null , null);

		if (!netPlan.getLinksDownAllLayers().isEmpty() || !netPlan.getNodesDown().isEmpty()) throw new RuntimeException ("Bad");

		NetPlan auxNetPlan = netPlan.copy ();
		this.algorithm = ClassLoaderUtils.getInstance(new File(algorithmFile), algorithmName, IEventProcessor.class , null);
		this.algorithm.initialize(auxNetPlan , algorithmParameters , reportParameters , net2planParameters);
		Set<Link> initialLinksDownAllLayers = auxNetPlan.getLinksDownAllLayers();
		Set<Node> initialNodesDown = auxNetPlan.getNodesDown();
		Set<Link> linksAllLayers = new HashSet<Link> (); for (NetworkLayer layer : auxNetPlan.getNetworkLayers()) linksAllLayers.addAll (auxNetPlan.getLinks (layer));

		for (int failureState = 1 ; failureState < F_s.rows () ; failureState ++) // first failure state (no failure) was already considered
		{
			if (!auxNetPlan.getLinksDownAllLayers().equals (initialLinksDownAllLayers) || !auxNetPlan.getNodesDown().equals(initialNodesDown)) throw new RuntimeException ("Bad");

			IntArrayList srgs_thisState = new IntArrayList (); F_s.viewRow (failureState).getNonZeros (srgs_thisState , new DoubleArrayList ());
			final double pi_s_thisState = pi_s.get (failureState);

			Set<Link> linksToSetAsDown = new HashSet<Link> ();
			Set<Node> nodesToSetAsDown = new HashSet<Node> ();
			for (SharedRiskGroup srg : auxNetPlan.getSRGs())
			{
				if (F_s.get(failureState , srg.getIndex ()) != 1) continue;
				nodesToSetAsDown.addAll (srg.getNodes ());
				linksToSetAsDown.addAll (srg.getLinksAllLayers ());
			}

			/* Make the algorithm process the event of nodes and links down */
			SimEvent.NodesAndLinksChangeFailureState failureInfo = new SimEvent.NodesAndLinksChangeFailureState(null , nodesToSetAsDown , null , linksToSetAsDown);
			try
			{
				/* Apply the reaction algorithm */
				algorithm.processEvent(auxNetPlan, new SimEvent(0, SimEvent.DestinationModule.EVENT_PROCESSOR , -1 , failureInfo));
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
			
			for(int indexLayer = 0 ; indexLayer < netPlan.getNumberOfLayers() ; indexLayer ++)
			{
				final NetworkLayer layer = netPlan.getNetworkLayer (indexLayer);
		    	final SortedMap<Link,SortedMap<String,Pair<Double,Double>>> perLink_qos2occupationAndViolationMap = netPlan.getAllLinksPerQosOccupationAndQosViolationMap(layer);
		    	for (Demand d : netPlan.getDemands(layer))
		    		info_d.get(d).update(d, pi_s_thisState, new HashSet<> (), perLink_qos2occupationAndViolationMap);
		    	for (MulticastDemand d : netPlan.getMulticastDemands(layer))
		    		info_md.get(d).update(d, pi_s_thisState, new HashSet<> (), perLink_qos2occupationAndViolationMap);
			}

			failureInfo = new SimEvent.NodesAndLinksChangeFailureState(auxNetPlan.getNodes() , null , linksAllLayers , null);
			algorithm.processEvent(auxNetPlan, new SimEvent(0, SimEvent.DestinationModule.EVENT_PROCESSOR , -1 , failureInfo));
		}

		return printReport(netPlan , reportParameters);
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
		DecimalFormat df_6 = new DecimalFormat("#.######");
		out.append("<html><body>");
		out.append("<head><title>Availability report</title></head>");
		out.append("<h1>Introduction</h1>");
		out.append("<p>This report computes several availability measures for the different unicast and multicast demands in the different layers of the network." +
				"The network is supposed to be subject to the failures defined by the SRGs, and the possible network states considered are: </p>");
		out.append ("<ul>");
		out.append ("<li>No failure occurs in the network</li>");
		out.append ("<li>Single SRG failure: A single SRG fails, while the rest of failures associated to the other SRGs do not occur</li>");
		out.append ("<li>Double SRG failure (optional): Two SRGs simultaneously fail, while the rest of failures associated to the other SRGs do not occur</li>");
		out.append ("</ul>");
		out.append("<p>A numerical method is used that (i) enumerates the possible failure states considered and the probability of occurrence, (ii) computes the network reaction" +
				"to that failure state using a used-defined algorithm, (iii) builds up the availability results for each demand from that.</p>");
		out.append("<p>Then, the method is able to produce an estimation of the availability of each unicast and multicast demand (and from them network-wide availability measures) under the given failures, and" +
				" assuming that the network reacts according to an arbitrary protection/restoration provided algorithm.</p>");
		out.append("<p>Two types of availability metrics are provided:</p>");
		out.append ("<ul>");
		out.append ("<li>Availability of a demand/network: Fraction of time in which the demand/network is carrying the 100% of the offered traffic</li>");
		out.append ("<li>Traffic survivability of a demand/network: Fraction of the offered traffic that the network carries</li>");
		out.append ("</ul>");
		out.append("<p>Traffic survivability is always equals or higher than analogous availability value. For instance, if during some failure states that mean a 1% of the time, a demad is " +
				"carrying just a 50% of the traffic, the network availability is 99% (99% of the time everythign works perfectly well), but traffic survivability is " +
				"0.99*1 + 0.01*0.5 = 99.5%</p>");
		out.append("<p>In each metric, we provide a pessimistic and optimistic estimation. The pessimistic estimation, considers that in the triple, quadruple etc. " +
				"failure states, all the traffic is lost. In the optimistic case, we assume that in such cases, all the traffic is carried. </p>");
		
		out.append("<p>For more information of this method:</p>");
		out.append("<p>P. Pavon Mariño, \"Optimization of computer networks. Modeling and algorithms. A hands-on approach\", Wiley 2016</p>");
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
		out.append("<tr><td>--Estimated error in availability calculations: </td><td>" + pi_excess + "</td><td>Probabilities of triple failure, quadruple etc. of non enumerated network states</td></tr>");
		out.append("</table>");

		out.append("<h1>PER LAYER INFORMATION SUMMARY</h1>");
		for (NetworkLayer layer : np.getNetworkLayers ())
		{
			out.append("<h2>Layer " + layer.getName () + ", index = " + layer.getIndex () + ", id = " + layer.getId () + "</h2>");
			final double totalOffered_d = np.getDemands(layer).stream().mapToDouble(d->d.getOfferedTraffic()).sum();
			final double totalOffered_md = np.getMulticastDemands(layer).stream().mapToDouble(d->d.getOfferedTraffic()).sum();
			final double weightedFractionOfOfTrafficNoFailureAv_d =  totalOffered_d == 0? 0.0 : np.getDemands(layer).stream().mapToDouble(d->(d.getOfferedTraffic() / totalOffered_d) * info_d.get(d).getFractionOkTrafficNoFailure()).sum();
			final double weightedFractionOfOfTrafficNoFailureAv_md =  totalOffered_md == 0? 0.0 : np.getMulticastDemands(layer).stream().mapToDouble(d->(d.getOfferedTraffic() / totalOffered_d) * info_d.get(d).getFractionOkTrafficNoFailure()).sum();
			final double weightedAv_d =  totalOffered_d == 0? 0.0 : np.getDemands(layer).stream().mapToDouble(d->(d.getOfferedTraffic() / totalOffered_d) * info_d.get(d).getAvailability()).sum();
			final double weightedAv_md =  totalOffered_md == 0? 0.0 : np.getMulticastDemands(layer).stream().mapToDouble(d->(d.getOfferedTraffic() / totalOffered_d) * info_d.get(d).getAvailability()).sum();
			final double weightedSurv_d =  totalOffered_d == 0? 0.0 : np.getDemands(layer).stream().mapToDouble(d->(d.getOfferedTraffic() / totalOffered_d) * info_d.get(d).getSurvivability()).sum();
			final double weightedSurv_md =  totalOffered_md == 0? 0.0 : np.getMulticastDemands(layer).stream().mapToDouble(d->(d.getOfferedTraffic() / totalOffered_d) * info_d.get(d).getSurvivability()).sum();

			final double worstAv_d =  totalOffered_d == 0? 0.0 : np.getDemands(layer).stream().mapToDouble(d->info_d.get(d).getAvailability()).min().orElse(0.0);
			final double worstAv_md =  totalOffered_md == 0? 0.0 : np.getMulticastDemands(layer).stream().mapToDouble(d->info_d.get(d).getAvailability()).min().orElse(0.0);
			final double worstSurv_d =  totalOffered_d == 0? 0.0 : np.getDemands(layer).stream().mapToDouble(d->info_d.get(d).getSurvivability()).min().orElse(0.0);
			final double worstSurv_md =  totalOffered_md == 0? 0.0 : np.getMulticastDemands(layer).stream().mapToDouble(d->info_d.get(d).getSurvivability()).min().orElse(0.0);

			
//			final DoubleMatrix1D h_d = np.getVectorDemandOfferedTraffic(layer);
//			final DoubleMatrix1D h_md = np.getVectorMulticastDemandOfferedTraffic(layer);
//			
//			
//			
//			final DoubleMatrix1D avNoFailure_d = np.getDemands(layer).stream().mapToDouble (); 
//					
//					this.availabilityClassicNoFailure_ld.get(layer.getIndex ());
//			final DoubleMatrix1D survNoFailure_d = this.availabilityWeightedNoFailure_ld.get(layer.getIndex ());
//			final DoubleMatrix1D avNoFailure_md = this.availabilityClassicNoFailure_lmd.get(layer.getIndex ());
//			final DoubleMatrix1D survNoFailure_md = this.availabilityWeightedNoFailure_lmd.get(layer.getIndex ());
//			final DoubleMatrix1D avTotal_d = this.availabilityClassicTotal_ld.get(layer.getIndex ());
//			final DoubleMatrix1D survTotal_d = this.availabilityWeightedTotal_ld.get(layer.getIndex ());
//			final DoubleMatrix1D avTotal_md = this.availabilityClassicTotal_lmd.get(layer.getIndex ());
//			final DoubleMatrix1D survTotal_md = this.availabilityWeightedTotal_lmd.get(layer.getIndex ());

			out.append ("<ul>");
			out.append ("<li>UNICAST TRAFFIC: (Deterministic) Total offered traffic: " + df_6.format (totalOffered_d) + "</li>");
			if (np.getNumberOfDemands (layer) != 0)
			{
				out.append ("<li>UNICAST TRAFFIC: (Deterministic) Blocked traffic when no failure occurs: " + df_6.format (weightedFractionOfOfTrafficNoFailureAv_d) + "</li>");
				out.append ("<li>UNICAST TRAFFIC: (Estimated) Weighted average of demand availabilities : " + printAvailability(weightedAv_d, pi_excess) + "</li>");
				out.append ("<li>UNICAST TRAFFIC: (Estimated) Weighted average of demands survivabilities: " + printAvailability(weightedSurv_d, pi_excess)  + "</li>");
				out.append ("<li>UNICAST TRAFFIC: (Estimated) Worst among demand availabilities: " + printAvailability(worstAv_d, pi_excess)   + "</li>");
				out.append ("<li>UNICAST TRAFFIC: (Estimated) Worst among demand survivabilities: " + printAvailability(worstSurv_d, pi_excess)   + "</li>");
			}			
			out.append ("<li>MULTICAST TRAFFIC: (Deterministic) Total offered traffic: " + df_6.format (totalOffered_md) + "</li>");
			if (np.getNumberOfMulticastDemands(layer) != 0)
			{
				out.append ("<li>MULTICAST TRAFFIC: (Deterministic) Blocked traffic when no failure occurs: " + df_6.format (weightedFractionOfOfTrafficNoFailureAv_md) + "</li>");
				out.append ("<li>MULTICAST TRAFFIC: (Estimated) Weighted average of demand availabilities : " + printAvailability(weightedAv_md, pi_excess) + "</li>");
				out.append ("<li>MULTICAST TRAFFIC: (Estimated) Weighted average of demands survivabilities: " + printAvailability(weightedSurv_md, pi_excess)  + "</li>");
				out.append ("<li>MULTICAST TRAFFIC: (Estimated) Worst among demand availabilities: " + printAvailability(worstAv_md, pi_excess)   + "</li>");
				out.append ("<li>MULTICAST TRAFFIC: (Estimated) Worst among demand survivabilities: " + printAvailability(worstSurv_md, pi_excess)   + "</li>");
			}
		}

		out.append("<h1>PER LAYER INFORMATION DETAILED INFORMATION</h1>");
		for (NetworkLayer layer : np.getNetworkLayers ())
		{
			out.append("<h2>Layer " + layer.getName () + ", index = " + layer.getIndex () + ", id = " + layer.getId () + "</h2>");
			final DoubleMatrix1D avTotal_d = this.availabilityClassicTotal_ld.get(layer.getIndex ());
			final DoubleMatrix1D survTotal_d = this.availabilityWeightedTotal_ld.get(layer.getIndex ());
			final DoubleMatrix1D avTotal_md = this.availabilityClassicTotal_lmd.get(layer.getIndex ());
			final DoubleMatrix1D survTotal_md = this.availabilityWeightedTotal_lmd.get(layer.getIndex ());

			if (np.getNumberOfDemands(layer) != 0)
			{
				out.append("<table border='1'>");
				final List<String> headers = Arrays.asList("Demand" , "Origin node" , "Destination node" , 
						"Availability" , "Survivability" , 
						"WC latency (ms) / involved SRGs" , 
						"WC blocking / involved SRGs" , 
						"WC oversubscription / involved SRGs");
				final List<Function<Demand , String>> vals_d = Arrays.asList(
						d->d.getIndex () + "" ,
						d->d.getIngressNode().getIndex() + "(" + d.getIngressNode().getName() + ")",
						d->d.getEgressNode().getIndex() + "(" + d.getEgressNode().getName() + ")",
						d->printAvailability(avTotal_d.get(d.getIndex()) , pi_excess)) ,
						d->printAvailability(survTotal_d.get(d.getIndex()) , pi_excess)) ,
						
						);
				out.append("<tr>"); for (String h : headers) out.append("<th><b>" + h + "</b></th>"); out.append("</tr>"); 
				for (Demand d : np.getDemands (layer))
					out.append("<tr><td> " + d.getIndex () + "</td><td>" + d.getIngressNode ().getIndex () + " (" + d.getIngressNode ().getName () + ") </td><td> " + d.getEgressNode().getIndex () + " (" + d.getEgressNode().getName () + ")</td><td>" + 
				avTotal_d.get(d.getIndex()) +" ... " + (avTotal_d.get(d.getIndex()) + pi_excess) + "</td><td>" +  
							survTotal_d.get(d.getIndex()) + " ... " + (survTotal_d.get(d.getIndex()) + pi_excess) + "</td></tr>");
				out.append("</table>");
	
				out.append("<p></p><p></p>");
			}
			if (np.getNumberOfMulticastDemands(layer) != 0)
			{
				out.append("<table border='1'>");
				out.append("<tr><th><b>Multicast demand</b></th><th><b>Origin node</b></th><th><b>Destination nodes</b></th><th><b>Availability</b></th><th><b>Traffic survivability</b></th></tr>");
				for (MulticastDemand d : np.getMulticastDemands (layer))
				{
					String egressNodesInfo = ""; for (Node n : d.getEgressNodes()) egressNodesInfo += n.getIndex () + "(" + n.getName () + ") ";
					out.append("<tr><td> " + d.getIndex () + " id (" + d.getId () + ")" + "</td><td>" + d.getIngressNode ().getIndex () + " (" + d.getIngressNode ().getName () + ") </td><td> " + egressNodesInfo + "</td><td>" + avTotal_md.get(d.getIndex())  + " ... " + (avTotal_md.get(d.getIndex()) + pi_excess) + "</td><td>" +  survTotal_md.get(d.getIndex())  + " ... " + (survTotal_md.get(d.getIndex()) + pi_excess) + "</td></tr>");
				}
				out.append("</table>");
			}			
		}
		
		return out.toString();
	}


	private static String printAvailability (double val , double pi_ne)
	{
		return dfAv.format(val) +" ... " + dfAv.format(Math.min(val + pi_ne, 1.0));
	}
	

	private static class PerDemandInfo
	{
		private double fractionOkTrafficNoFailure = 0;
		private double av = 0, surv = 0, wcLatMs = 0.0, wcOversubsGbps = 0.0 , wcBlockingGbps = 0.0;
		private Set<Set<SharedRiskGroup>> failureStates_wcLat = new HashSet<> () , failureStates_wcOversubs  = new HashSet<> (), failureStates_wcBlocking  = new HashSet<> ();
		public void update (NetworkElement element ,  double prob , Set<SharedRiskGroup> srgs , SortedMap<Link,SortedMap<String,Pair<Double,Double>>> perLink_qos2occupationAndViolationMap)
		{
			if (!((element instanceof Demand) || (element instanceof MulticastDemand))) throw new RuntimeException ();
			final boolean isDemand = element instanceof Demand;
			final Demand d = isDemand? (Demand) element : null;
			final MulticastDemand md = isDemand? null : (MulticastDemand) element;
			final String qosType = isDemand? d.getQosType() : md.getQosType();
			final double latMs = isDemand? d.getWorstCasePropagationTimeInMs() : md.getWorseCasePropagationTimeInMs();
			final double oversubsGbps = (isDemand? d.getTraversedLinksAndCarriedTraffic(false).keySet() : md.getTraversedLinksAndCarriedTraffic(false).keySet()).stream().mapToDouble (e -> perLink_qos2occupationAndViolationMap.get(e).get(qosType).getFirst()).max().orElse(0.0);
			final double blockGbps = isDemand? d.getBlockedTraffic() : md.getBlockedTraffic();
			final boolean okLatency = isDemand? d.getMaximumAcceptableE2EWorstCaseLatencyInMs() + Configuration.precisionFactor > latMs : md.getMaximumAcceptableE2EWorstCaseLatencyInMs() + Configuration.precisionFactor > latMs; 
			final double trafFullyOkGbps = Math.max(0.0 ,  !okLatency? 0.0 : (isDemand? d.getCarriedTraffic() : md.getCarriedTraffic()) - oversubsGbps);
			final double fractionTrafficOk = (isDemand? d.getOfferedTraffic() : md.getOfferedTraffic()) < Configuration.precisionFactor? 0.0 : Math.max (1.0 , trafFullyOkGbps /  (isDemand? d.getOfferedTraffic() : md.getOfferedTraffic()));
			final boolean allTrafficOk = trafFullyOkGbps + Configuration.precisionFactor >= (isDemand? d.getOfferedTraffic(): md.getOfferedTraffic());
			if (allTrafficOk) av += prob;
			surv += prob * fractionTrafficOk; 
			if (latMs > wcLatMs) { wcLatMs = latMs; failureStates_wcLat = new HashSet<> (Arrays.asList(srgs)); }
			else if (latMs == wcLatMs) { wcLatMs = latMs; failureStates_wcLat.add(srgs); }
			if (oversubsGbps > wcOversubsGbps) { wcOversubsGbps = oversubsGbps; failureStates_wcOversubs = new HashSet<> (Arrays.asList(srgs)); }
			else if (oversubsGbps == wcOversubsGbps) { wcOversubsGbps = oversubsGbps; failureStates_wcOversubs.add(srgs); }
			if (blockGbps > wcBlockingGbps) { wcBlockingGbps = blockGbps; failureStates_wcBlocking = new HashSet<> (Arrays.asList(srgs)); }
			else if (blockGbps == wcBlockingGbps) { wcBlockingGbps = blockGbps; failureStates_wcBlocking.add(srgs); }
			if (srgs.isEmpty()) { fractionOkTrafficNoFailure = fractionTrafficOk; } 
		}
		public double getAvailability () { return av; }
		public double getSurvivability () { return surv; }
		public double getWcLatencyMs () { return wcLatMs; }
		public double getWcBlockingGbps () { return wcBlockingGbps; }
		public double getWcQoSViolationGbps () { return wcOversubsGbps; }
		public Set<Set<SharedRiskGroup>> getFailureStates_wcLat () { return failureStates_wcLat; }
		public Set<Set<SharedRiskGroup>> getFailureStates_wcQosViolation () { return failureStates_wcOversubs; }
		public Set<Set<SharedRiskGroup>> getFailureStates_wcBlocking () { return failureStates_wcBlocking; }
		public double getFractionOkTrafficNoFailure () { return fractionOkTrafficNoFailure; }
	}
	
}
