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

import cern.colt.function.tdouble.DoubleDoubleFunction;
import cern.colt.function.tdouble.DoubleFunction;
import cern.colt.list.tdouble.DoubleArrayList;
import cern.colt.list.tint.IntArrayList;
import cern.colt.matrix.tdouble.DoubleFactory1D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.interfaces.simulation.IEventProcessor;
import com.net2plan.interfaces.simulation.SimEvent;
import com.net2plan.libraries.SRGUtils;
import com.net2plan.utils.ClassLoaderUtils;
import com.net2plan.utils.InputParameter;
import com.net2plan.utils.StringUtils;
import com.net2plan.utils.Triple;

import java.io.Closeable;
import java.io.File;
import java.text.DecimalFormat;
import java.util.*;

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
	private InputParameter considerTrafficInOversubscribedLinksAsLost = new InputParameter ("considerTrafficInOversubscribedLinksAsLost" , true , "If true, all the demands whose traffic (even only a fraction of it) traverses an oversubscribed link, are considered that all its treaffic is blocked, as they are supposed to fail to satisfy QoS agreements");
	private InputParameter maximumE2ELatencyMs = new InputParameter ("maximumE2ELatencyMs", (double) -1 , "Maximum end-to-end latency of the traffic of any demand (a non-positive value means no limit). All the traffic of demands where a fraction of its traffic can exceed this value, are considered as lost, as they are supposed to fail to satisfy QoS agreements");
	
	private ArrayList<DoubleMatrix1D> availabilityClassicNoFailure_ld, availabilityWeightedNoFailure_ld, availabilityClassicNoFailure_lmd, availabilityWeightedNoFailure_lmd;
	private ArrayList<DoubleMatrix1D> availabilityClassicTotal_ld, availabilityWeightedTotal_ld, availabilityClassicTotal_lmd, availabilityWeightedTotal_lmd;
	private double pi_excess;

	private IEventProcessor algorithm;
	
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
		availabilityClassicNoFailure_ld = new ArrayList<DoubleMatrix1D> ();
		availabilityWeightedNoFailure_ld = new ArrayList<DoubleMatrix1D> ();
		availabilityClassicNoFailure_lmd = new ArrayList<DoubleMatrix1D> ();
		availabilityWeightedNoFailure_lmd = new ArrayList<DoubleMatrix1D> ();
		availabilityClassicTotal_ld = new ArrayList<DoubleMatrix1D> (); 
		availabilityWeightedTotal_ld = new ArrayList<DoubleMatrix1D> ();
		availabilityClassicTotal_lmd = new ArrayList<DoubleMatrix1D> ();
		availabilityWeightedTotal_lmd = new ArrayList<DoubleMatrix1D> ();
		for(int indexLayer = 0 ; indexLayer < netPlan.getNumberOfLayers() ; indexLayer ++)
		{
			final NetworkLayer layer = netPlan.getNetworkLayer (indexLayer);
			final int D = netPlan.getNumberOfDemands(layer);
			final int MD = netPlan.getNumberOfMulticastDemands(layer);
			availabilityClassicTotal_ld.add (DoubleFactory1D.dense.make (D,0.0));
			availabilityWeightedTotal_ld.add (DoubleFactory1D.dense.make (D,0.0));
			availabilityClassicTotal_lmd.add (DoubleFactory1D.dense.make (MD,0.0));
			availabilityWeightedTotal_lmd.add (DoubleFactory1D.dense.make (MD,0.0));
		}

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

			final DoubleMatrix1D h_d = netPlan.getVectorDemandOfferedTraffic(layer);
			final DoubleMatrix1D blocked_d = netPlan.getVectorDemandBlockedTraffic(layer);
			if (considerTrafficInOversubscribedLinksAsLost.getBoolean()) for (Demand d : netPlan.getDemands (layer)) if (d.isTraversingOversubscribedLinks()) blocked_d.set (d.getIndex () , d.getOfferedTraffic());
			if (maximumE2ELatencyMs.getDouble () > 0) for (Demand d : netPlan.getDemands (layer)) if (d.getWorstCasePropagationTimeInMs() > maximumE2ELatencyMs.getDouble ()) blocked_d.set (d.getIndex () , d.getOfferedTraffic());
			
			final DoubleMatrix1D h_md = netPlan.getVectorMulticastDemandOfferedTraffic(layer);
			final DoubleMatrix1D blocked_md = netPlan.getVectorMulticastDemandBlockedTraffic(layer);
			if (considerTrafficInOversubscribedLinksAsLost.getBoolean()) for (MulticastDemand d : netPlan.getMulticastDemands (layer)) if (d.isTraversingOversubscribedLinks()) blocked_md.set (d.getIndex () , d.getOfferedTraffic());
			if (maximumE2ELatencyMs.getDouble () > 0) for (MulticastDemand d : netPlan.getMulticastDemands(layer)) if (d.getWorseCasePropagationTimeInMs() > maximumE2ELatencyMs.getDouble ()) blocked_md.set (d.getIndex () , d.getOfferedTraffic());

			//			System.out.println ("****** No failure Layer  " + layer + ", blocked_d: " + blocked_d);
			final DoubleMatrix1D availabilityClassic_d = blocked_d.copy ().assign (new DoubleFunction () { public double apply (double x) { return x > PRECISION_FACTOR_blocking? 0 : 1;  }  } );
			final DoubleMatrix1D availabilityWeighted_d = blocked_d.copy ().assign (h_d , new DoubleDoubleFunction () { public double apply (double x , double y) { return y < PRECISION_FACTOR_hd? 1 : 1 - x/y; }  } );
			final DoubleMatrix1D availabilityClassic_md = blocked_md.copy ().assign (new DoubleFunction () { public double apply (double x) { return x > PRECISION_FACTOR_blocking ? 0 : 1;  }  } );
			final DoubleMatrix1D availabilityWeighted_md = blocked_md.copy ().assign (h_md , new DoubleDoubleFunction () { public double apply (double x , double y) { return y < PRECISION_FACTOR_hd ? 1 : 1 - x/y; }  } );

			availabilityClassicTotal_ld.get(layer.getIndex ()).assign (availabilityClassic_d , new DoubleDoubleFunction () { public double apply (double x , double y) { return x + pi_s0 * y; } } );
			availabilityWeightedTotal_ld.get(layer.getIndex ()).assign (availabilityWeighted_d , new DoubleDoubleFunction () { public double apply (double x , double y) { return x + pi_s0 * y; } } );
			availabilityClassicTotal_lmd.get(layer.getIndex ()).assign (availabilityClassic_md , new DoubleDoubleFunction () { public double apply (double x , double y) { return x + pi_s0 * y; } } );
			availabilityWeightedTotal_lmd.get(layer.getIndex ()).assign (availabilityWeighted_md , new DoubleDoubleFunction () { public double apply (double x , double y) { return x + pi_s0 * y; } } );

			availabilityClassicNoFailure_ld.add(availabilityClassic_d);
			availabilityWeightedNoFailure_ld.add(availabilityWeighted_d);
			availabilityClassicNoFailure_lmd.add(availabilityClassic_md);
			availabilityWeightedNoFailure_lmd.add(availabilityWeighted_md);
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
			
			for(NetworkLayer layer : auxNetPlan.getNetworkLayers ())
			{
				final DoubleMatrix1D h_d = auxNetPlan.getVectorDemandOfferedTraffic(layer);
				final DoubleMatrix1D blocked_d = auxNetPlan.getVectorDemandBlockedTraffic(layer);
				if (considerTrafficInOversubscribedLinksAsLost.getBoolean()) for (Demand d : auxNetPlan.getDemands (layer)) if (d.isTraversingOversubscribedLinks()) blocked_d.set (d.getIndex () , d.getOfferedTraffic());

				if (maximumE2ELatencyMs.getDouble () > 0) for (Demand d : auxNetPlan.getDemands (layer)) if (d.getWorstCasePropagationTimeInMs() > maximumE2ELatencyMs.getDouble ()) blocked_d.set (d.getIndex () , d.getOfferedTraffic());
				final DoubleMatrix1D h_md = auxNetPlan.getVectorMulticastDemandOfferedTraffic(layer);
				final DoubleMatrix1D blocked_md = auxNetPlan.getVectorMulticastDemandBlockedTraffic(layer);
				if (considerTrafficInOversubscribedLinksAsLost.getBoolean()) for (MulticastDemand d : auxNetPlan.getMulticastDemands (layer)) if (d.isTraversingOversubscribedLinks()) blocked_md.set (d.getIndex () , d.getOfferedTraffic());

				if (maximumE2ELatencyMs.getDouble () > 0) for (MulticastDemand d : auxNetPlan.getMulticastDemands(layer)) if (d.getWorseCasePropagationTimeInMs() > maximumE2ELatencyMs.getDouble ()) blocked_md.set (d.getIndex () , d.getOfferedTraffic());

				final DoubleMatrix1D availabilityClassic_d = blocked_d.copy ().assign (new DoubleFunction () { public double apply (double x) { return x > PRECISION_FACTOR_blocking? 0 : 1;  }  } );
				final DoubleMatrix1D availabilityWeighted_d = blocked_d.copy ().assign (h_d , new DoubleDoubleFunction () { public double apply (double x , double y) { return y < PRECISION_FACTOR_hd? 1 : 1 - x/y; }  } );
				final DoubleMatrix1D availabilityClassic_md = blocked_md.copy ().assign (new DoubleFunction () { public double apply (double x) { return x > PRECISION_FACTOR_blocking? 0 : 1;  }  } );
				final DoubleMatrix1D availabilityWeighted_md = blocked_md.copy ().assign (h_md , new DoubleDoubleFunction () { public double apply (double x , double y) { return y < PRECISION_FACTOR_hd? 1 : 1 - x/y; }  } );

				availabilityClassicTotal_ld.get(layer.getIndex ()).assign (availabilityClassic_d , new DoubleDoubleFunction () { public double apply (double x , double y) { return x + pi_s_thisState * y; } } );
				availabilityWeightedTotal_ld.get(layer.getIndex ()).assign (availabilityWeighted_d , new DoubleDoubleFunction () { public double apply (double x , double y) { return x + pi_s_thisState * y; } } );
				availabilityClassicTotal_lmd.get(layer.getIndex ()).assign (availabilityClassic_md , new DoubleDoubleFunction () { public double apply (double x , double y) { return x + pi_s_thisState * y; } } );
				availabilityWeightedTotal_lmd.get(layer.getIndex ()).assign (availabilityWeighted_md , new DoubleDoubleFunction () { public double apply (double x , double y) { return x + pi_s_thisState * y; } } );
				
//				System.out.println ("Failure " + failureState + ", availabilityClassicTotal_ld: " + availabilityClassicTotal_ld);
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
		out.append("<p>P. Pav�n Mariño, \"Optimization of computer networks. Modeling and algorithms. A hands-on approach\", Wiley 2016</p>");
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
			double val;

			out.append("<h2>Layer " + layer.getName () + ", index = " + layer.getIndex () + ", id = " + layer.getId () + "</h2>");
			final DoubleMatrix1D h_d = np.getVectorDemandOfferedTraffic(layer);
			final DoubleMatrix1D h_md = np.getVectorMulticastDemandOfferedTraffic(layer);
			final DoubleMatrix1D avNoFailure_d = this.availabilityClassicNoFailure_ld.get(layer.getIndex ());
			final DoubleMatrix1D survNoFailure_d = this.availabilityWeightedNoFailure_ld.get(layer.getIndex ());
			final DoubleMatrix1D avNoFailure_md = this.availabilityClassicNoFailure_lmd.get(layer.getIndex ());
			final DoubleMatrix1D survNoFailure_md = this.availabilityWeightedNoFailure_lmd.get(layer.getIndex ());
			final DoubleMatrix1D avTotal_d = this.availabilityClassicTotal_ld.get(layer.getIndex ());
			final DoubleMatrix1D survTotal_d = this.availabilityWeightedTotal_ld.get(layer.getIndex ());
			final DoubleMatrix1D avTotal_md = this.availabilityClassicTotal_lmd.get(layer.getIndex ());
			final DoubleMatrix1D survTotal_md = this.availabilityWeightedTotal_lmd.get(layer.getIndex ());

			out.append ("<ul>");
			final double H = h_d.zSum();
			val = h_d.size () == 0? 0 : H; out.append ("<li>UNICAST TRAFFIC: (Deterministic) Total offered traffic: " + df_6.format (val) + "</li>");
			if (np.getNumberOfDemands (layer) != 0)
			{
				val = survNoFailure_d.size () == 0? 0 : 1 - (survNoFailure_d.zDotProduct(h_d) / H); out.append ("<li>UNICAST TRAFFIC: (Deterministic) Blocked traffic when no failure occurs: " + df_6.format (val) + "</li>");
				val = avTotal_d.size () == 0? 0 : avTotal_d.zDotProduct(h_d) / H; out.append ("<li>UNICAST TRAFFIC: (Estimated) Network availability: " + df_6.format(val) + " - " + df_6.format(val + pi_excess)  + "</li>");
				val = survTotal_d.size () == 0? 0 : survTotal_d.zDotProduct(h_d) / H; out.append ("<li>UNICAST TRAFFIC: (Estimated) Network traffic survivability: " + df_6.format(val) + " - " + df_6.format(val + pi_excess) + "</li>");
				val = avTotal_d.size () == 0? 0 : avTotal_d.getMinLocation() [0]; out.append ("<li>UNICAST TRAFFIC: (Estimated) Worse availability among demands: " + df_6.format(val) + " - " + df_6.format(val + pi_excess)  + "</li>");
				val = survTotal_d.size () == 0? 0 : survTotal_d.getMinLocation() [0]; out.append ("<li>UNICAST TRAFFIC: (Estimated) Worse traffic survivability among demands: " + df_6.format(val) + " - " + df_6.format(val + pi_excess)  + "</li>");
			}			
			final double MH = h_md.zSum();
			val = h_md.size () == 0? 0 : MH; out.append ("<li>MULTICAST TRAFFIC: (Deterministic) Total offered traffic: " + df_6.format (val) + "</li>");
			if (np.getNumberOfMulticastDemands(layer) != 0)
			{
				val = survNoFailure_md.size () == 0? 0 : 1-(survNoFailure_md.zDotProduct(h_md) / MH); out.append ("<li>MULTICAST TRAFFIC: (Deterministic) Blocked traffic when no failure occurs: " + df_6.format (val) + "</li>");
				val = avTotal_md.size () == 0? 0 : avTotal_md.zDotProduct(h_md) / MH; out.append ("<li>MULTICAST TRAFFIC: (Estimated) Network availability: " + df_6.format(val) + " - " + df_6.format(val + pi_excess)  + "</li>");
				val = survTotal_md.size () == 0? 0 : survTotal_md.zDotProduct(h_md) /MH; out.append ("<li>MULTICAST TRAFFIC: (Estimated) Network traffic survivability: " + df_6.format(val) + " - " + df_6.format(val + pi_excess) + "</li>");
				val = avTotal_md.size () == 0? 0 : avTotal_md.getMinLocation() [0]; out.append ("<li>MULTICAST TRAFFIC: (Estimated) Worse availability among demands: " + df_6.format(val) + " - " + df_6.format(val + pi_excess)  + "</li>");
				val = survTotal_md.size () == 0? 0 : survTotal_md.getMinLocation() [0]; out.append ("<li>MULTICAST TRAFFIC: (Estimated) Worse traffic survivability among demands: " + df_6.format(val) + " - " + df_6.format(val + pi_excess)  + "</li>");
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
				out.append("<tr><th><b>Demand</b></th><th><b>Origin node</b></th><th><b>Destination node</b></th><th><b>Availability</b></th><th><b>Traffic survivability</b></th></tr>");
				for (Demand d : np.getDemands (layer))
					out.append("<tr><td> " + d.getIndex () + " id (" + d.getId () + ")" + "</td><td>" + d.getIngressNode ().getIndex () + " (" + d.getIngressNode ().getName () + ") </td><td> " + d.getEgressNode().getIndex () + " (" + d.getEgressNode().getName () + ")</td><td>" + avTotal_d.get(d.getIndex()) +" ... " + (avTotal_d.get(d.getIndex()) + pi_excess) + "</td><td>" +  survTotal_d.get(d.getIndex()) + " ... " + (survTotal_d.get(d.getIndex()) + pi_excess) + "</td></tr>");
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

	
	
}
