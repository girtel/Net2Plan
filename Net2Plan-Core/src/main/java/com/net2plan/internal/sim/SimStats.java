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



 




package com.net2plan.internal.sim;

import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.utils.HTMLUtils;
import com.net2plan.utils.StringUtils;
import org.apache.commons.lang3.mutable.MutableDouble;
import org.codehaus.stax2.XMLOutputFactory2;
import org.codehaus.stax2.XMLStreamWriter2;

import javax.xml.stream.XMLOutputFactory;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Abstract class defining a template for statistics classes for simulations.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.3
 */
public class SimStats
{
	/* Input and Net2Plan-wide parameters */
	private final double precisionFactor;
	
	private final NetPlan netState;

	private double lastEventTime, transitoryTime;
	
	private Set<Long> previousState_layerIds;
	private Set<Long> previousState_nodeIds, previousState_nodeDownIds;
	private Map<Long, Set<Long>> previousState_linkIds, previousState_linkDownIds, previousState_demandIds;
	
	/* Network information */
	private double accum_avgNumLayers, accum_avgNumNodes;
	private int maxNumLayers, maxNumNodes, minNumLayers, minNumNodes;
	
	/* Layer information */
	private Map<Long, MutableDouble> accum_layerTotalTime, accum_avgNumLinks, accum_avgNumDemands, accum_avgTotalOfferedTraffic, maxTotalOfferedTraffic, minTotalOfferedTraffic, accum_avgTotalCarriedTraffic, maxTotalCarriedTraffic, minTotalCarriedTraffic, accum_avgTotalCapacity, maxTotalCapacity, minTotalCapacity, accum_avgCongestion, maxCongestion, minCongestion, accum_availabilityClassic, accum_availabilityWeighted, worstDemandAvailabilityClassic, worstDemandAvailabilityWeighted;
	private Map<Long, Integer> maxNumLinks, minNumLinks, maxNumDemands, minNumDemands;
	
	/* Node information */
	private Map<Long, MutableDouble> accum_nodeUpTime, accum_nodeTotalTime;
	private Map<Long, Map<Long, MutableDouble>> accum_avgNodeInDegree, accum_avgNodeOutDegree, accum_avgNodeIngressTraffic, accum_avgNodeEgressTraffic;
	private Map<Long, Map<Long, Double>> previousState_nodeIngressTraffic, previousState_nodeEgressTraffic, maxNodeIngressTraffic, minNodeIngressTraffic, maxNodeEgressTraffic, minNodeEgressTraffic;
	private Map<Long, Map<Long, Integer>> previousState_nodeInDegree, previousState_nodeOutDegree, maxNodeInDegree, maxNodeOutDegree, minNodeInDegree, minNodeOutDegree;
	
	/* Link information */
	private Map<Long, Map<Long, Double>> previousState_linkLengthInKm, previousState_linkCapacity, previousState_linkOccupiedCapacity;
	private Map<Long, Map<Long, MutableDouble>> accum_avgLinkLengthInKm, minLinkLengthInKm, maxLinkLengthInKm;
	private Map<Long, Map<Long, MutableDouble>> accum_avgCapacity, minCapacity, maxCapacity;
	private Map<Long, Map<Long, MutableDouble>> accum_avgLinkOccupiedCapacity, minLinkOccupiedCapacity, maxLinkOccupiedCapacity;
	private Map<Long, Map<Long, MutableDouble>> accum_avgUtilization, minUtilization, maxUtilization;
	private Map<Long, Map<Long, MutableDouble>> accum_avgOversubscribedCapacity, minOversubscribedCapacity, maxOversubscribedCapacity;
	private Map<Long, Map<Long, MutableDouble>> accum_linkOversubscribedTime, accum_linkUpTime, accum_linkTotalTime;
	
	/* Demand information */
	private Map<Long, Map<Long, Double>> previousState_demandOfferedTraffic, previousState_demandCarriedTraffic;
	private Map<Long, Map<Long, MutableDouble>> accum_avgDemandOfferedTraffic, minDemandOfferedTraffic, maxDemandOfferedTraffic;
	private Map<Long, Map<Long, MutableDouble>> accum_avgDemandCarriedTraffic, minDemandCarriedTraffic, maxDemandCarriedTraffic;
	private Map<Long, Map<Long, MutableDouble>> accum_avgDemandBlockedTraffic, minDemandBlockedTraffic, maxDemandBlockedTraffic;
	private Map<Long, Map<Long, MutableDouble>> accum_avgExcessCarriedTraffic, minDemandExcessCarriedTraffic, maxDemandExcessCarriedTraffic;
	private Map<Long, Map<Long, MutableDouble>> accum_demandAvailabilityClassic, accum_demandAvailabilityWeighted;
	private Map<Long, Map<Long, MutableDouble>> excessDemandCarriedTrafficTime, demandTotalTime;
	
	/**
	 * Default constructor.
	 * 
	 * @param netState Reference to the current network state
	 * @param simulationParameters A key-value map with simulation options
	 * @param net2planParameters A key-value map with {@code Net2Plan}-wide configuration options
	 * @since 0.3.0
	 */
	public SimStats(NetPlan netState, Map<String, String> simulationParameters, Map<String, String> net2planParameters)
	{
		this.netState = netState;
		precisionFactor = Double.parseDouble(net2planParameters.get("precisionFactor"));
		
		reset(0);
	}
	
	private void checkAndCreateDemand(long layerId, long demandId)
	{
		if (!accum_avgDemandOfferedTraffic.get(layerId).containsKey(demandId))
		{
			/* Initialize demand information */
			accum_avgDemandOfferedTraffic.get(layerId).put(demandId, new MutableDouble());
			minDemandOfferedTraffic.get(layerId).put(demandId, new MutableDouble(Double.MAX_VALUE));
			maxDemandOfferedTraffic.get(layerId).put(demandId, new MutableDouble());
			accum_avgDemandCarriedTraffic.get(layerId).put(demandId, new MutableDouble());
			minDemandCarriedTraffic.get(layerId).put(demandId, new MutableDouble(Double.MAX_VALUE));
			maxDemandCarriedTraffic.get(layerId).put(demandId, new MutableDouble());
			accum_avgDemandBlockedTraffic.get(layerId).put(demandId, new MutableDouble());
			minDemandBlockedTraffic.get(layerId).put(demandId, new MutableDouble(Double.MAX_VALUE));
			maxDemandBlockedTraffic.get(layerId).put(demandId, new MutableDouble());
			accum_avgExcessCarriedTraffic.get(layerId).put(demandId, new MutableDouble());
			minDemandExcessCarriedTraffic.get(layerId).put(demandId, new MutableDouble(Double.MAX_VALUE));
			maxDemandExcessCarriedTraffic.get(layerId).put(demandId, new MutableDouble());
			accum_demandAvailabilityClassic.get(layerId).put(demandId, new MutableDouble());
			accum_demandAvailabilityWeighted.get(layerId).put(demandId, new MutableDouble());
			excessDemandCarriedTrafficTime.get(layerId).put(demandId, new MutableDouble());
			demandTotalTime.get(layerId).put(demandId, new MutableDouble());
		}
	}

	private void checkAndCreateLayer(long layerId)
	{
		if (!accum_avgLinkLengthInKm.containsKey(layerId))
		{
			/* Initialize layer information */
			accum_layerTotalTime.put(layerId, new MutableDouble());
			accum_avgNumLinks.put(layerId, new MutableDouble());
			accum_avgNumDemands.put(layerId, new MutableDouble());
			accum_avgTotalOfferedTraffic.put(layerId, new MutableDouble());
			maxTotalOfferedTraffic.put(layerId, new MutableDouble());
			minTotalOfferedTraffic.put(layerId, new MutableDouble(Double.MAX_VALUE));
			accum_avgTotalCarriedTraffic.put(layerId, new MutableDouble());
			maxTotalCarriedTraffic.put(layerId, new MutableDouble());
			minTotalCarriedTraffic.put(layerId, new MutableDouble(Double.MAX_VALUE));
			accum_avgTotalCapacity.put(layerId, new MutableDouble());
			maxTotalCapacity.put(layerId, new MutableDouble());
			minTotalCapacity.put(layerId, new MutableDouble(Double.MAX_VALUE));
			accum_avgCongestion.put(layerId, new MutableDouble());
			maxCongestion.put(layerId, new MutableDouble());
			minCongestion.put(layerId, new MutableDouble(Double.MAX_VALUE));
			accum_availabilityClassic.put(layerId, new MutableDouble());
			accum_availabilityWeighted.put(layerId, new MutableDouble());
			worstDemandAvailabilityClassic.put(layerId, new MutableDouble(1));
			worstDemandAvailabilityWeighted.put(layerId, new MutableDouble(1));
			maxNumLinks.put(layerId, 0);
			minNumLinks.put(layerId, Integer.MAX_VALUE);
			maxNumDemands.put(layerId, 0);
			minNumDemands.put(layerId, Integer.MAX_VALUE);

			/* Initialize node information in this layer */
			accum_avgNodeInDegree.put(layerId, new LinkedHashMap<Long, MutableDouble>());
			minNodeInDegree.put(layerId, new LinkedHashMap<Long, Integer>());
			maxNodeInDegree.put(layerId, new LinkedHashMap<Long, Integer>());
			accum_avgNodeOutDegree.put(layerId, new LinkedHashMap<Long, MutableDouble>());
			minNodeOutDegree.put(layerId, new LinkedHashMap<Long, Integer>());
			maxNodeOutDegree.put(layerId, new LinkedHashMap<Long, Integer>());
			accum_avgNodeIngressTraffic.put(layerId, new LinkedHashMap<Long, MutableDouble>());
			maxNodeIngressTraffic.put(layerId, new LinkedHashMap<Long, Double>());
			minNodeIngressTraffic.put(layerId, new LinkedHashMap<Long, Double>());
			accum_avgNodeEgressTraffic.put(layerId, new LinkedHashMap<Long, MutableDouble>());
			maxNodeEgressTraffic.put(layerId, new LinkedHashMap<Long, Double>());
			minNodeEgressTraffic.put(layerId, new LinkedHashMap<Long, Double>());

			/* Initialize link information in this layer */
			accum_avgLinkLengthInKm.put(layerId, new LinkedHashMap<Long, MutableDouble>());
			minLinkLengthInKm.put(layerId, new LinkedHashMap<Long, MutableDouble>());
			maxLinkLengthInKm.put(layerId, new LinkedHashMap<Long, MutableDouble>());
			accum_avgCapacity.put(layerId, new LinkedHashMap<Long, MutableDouble>());
			minCapacity.put(layerId, new LinkedHashMap<Long, MutableDouble>());
			maxCapacity.put(layerId, new LinkedHashMap<Long, MutableDouble>());
			accum_avgLinkOccupiedCapacity.put(layerId, new LinkedHashMap<Long, MutableDouble>());
			minLinkOccupiedCapacity.put(layerId, new LinkedHashMap<Long, MutableDouble>());
			maxLinkOccupiedCapacity.put(layerId, new LinkedHashMap<Long, MutableDouble>());
			accum_avgUtilization.put(layerId, new LinkedHashMap<Long, MutableDouble>());
			minUtilization.put(layerId, new LinkedHashMap<Long, MutableDouble>());
			maxUtilization.put(layerId, new LinkedHashMap<Long, MutableDouble>());
			accum_avgOversubscribedCapacity.put(layerId, new LinkedHashMap<Long, MutableDouble>());
			minOversubscribedCapacity.put(layerId, new LinkedHashMap<Long, MutableDouble>());
			maxOversubscribedCapacity.put(layerId, new LinkedHashMap<Long, MutableDouble>());
			accum_linkOversubscribedTime.put(layerId, new LinkedHashMap<Long, MutableDouble>());
			accum_linkUpTime.put(layerId, new LinkedHashMap<Long, MutableDouble>());
			accum_linkTotalTime.put(layerId, new LinkedHashMap<Long, MutableDouble>());

			/* Initialize demand information in this layer */
			accum_avgDemandOfferedTraffic.put(layerId, new LinkedHashMap<Long, MutableDouble>());
			minDemandOfferedTraffic.put(layerId, new LinkedHashMap<Long, MutableDouble>());
			maxDemandOfferedTraffic.put(layerId, new LinkedHashMap<Long, MutableDouble>());
			accum_avgDemandCarriedTraffic.put(layerId, new LinkedHashMap<Long, MutableDouble>());
			minDemandCarriedTraffic.put(layerId, new LinkedHashMap<Long, MutableDouble>());
			maxDemandCarriedTraffic.put(layerId, new LinkedHashMap<Long, MutableDouble>());
			accum_avgDemandBlockedTraffic.put(layerId, new LinkedHashMap<Long, MutableDouble>());
			minDemandBlockedTraffic.put(layerId, new LinkedHashMap<Long, MutableDouble>());
			maxDemandBlockedTraffic.put(layerId, new LinkedHashMap<Long, MutableDouble>());
			accum_avgExcessCarriedTraffic.put(layerId, new LinkedHashMap<Long, MutableDouble>());
			minDemandExcessCarriedTraffic.put(layerId, new LinkedHashMap<Long, MutableDouble>());
			maxDemandExcessCarriedTraffic.put(layerId, new LinkedHashMap<Long, MutableDouble>());
			accum_demandAvailabilityClassic.put(layerId, new LinkedHashMap<Long, MutableDouble>());
			accum_demandAvailabilityWeighted.put(layerId, new LinkedHashMap<Long, MutableDouble>());
			excessDemandCarriedTrafficTime.put(layerId, new LinkedHashMap<Long, MutableDouble>());
			demandTotalTime.put(layerId, new LinkedHashMap<Long, MutableDouble>());
		}
	}

	private void checkAndCreateLink(long layerId, long linkId)
	{
		if (!accum_avgLinkLengthInKm.get(layerId).containsKey(linkId))
		{
			/* Initialize link information */
			accum_avgLinkLengthInKm.get(layerId).put(linkId, new MutableDouble());
			minLinkLengthInKm.get(layerId).put(linkId, new MutableDouble(Double.MAX_VALUE));
			maxLinkLengthInKm.get(layerId).put(linkId, new MutableDouble());
			accum_avgCapacity.get(layerId).put(linkId, new MutableDouble());
			minCapacity.get(layerId).put(linkId, new MutableDouble(Double.MAX_VALUE));
			maxCapacity.get(layerId).put(linkId, new MutableDouble());
			accum_avgLinkOccupiedCapacity.get(layerId).put(linkId, new MutableDouble());
			minLinkOccupiedCapacity.get(layerId).put(linkId, new MutableDouble(Double.MAX_VALUE));
			maxLinkOccupiedCapacity.get(layerId).put(linkId, new MutableDouble());
			accum_avgUtilization.get(layerId).put(linkId, new MutableDouble());
			minUtilization.get(layerId).put(linkId, new MutableDouble(Double.MAX_VALUE));
			maxUtilization.get(layerId).put(linkId, new MutableDouble());
			accum_avgOversubscribedCapacity.get(layerId).put(linkId, new MutableDouble());
			minOversubscribedCapacity.get(layerId).put(linkId, new MutableDouble(Double.MAX_VALUE));
			maxOversubscribedCapacity.get(layerId).put(linkId, new MutableDouble());
			accum_linkOversubscribedTime.get(layerId).put(linkId, new MutableDouble());
			accum_linkUpTime.get(layerId).put(linkId, new MutableDouble());
			accum_linkTotalTime.get(layerId).put(linkId, new MutableDouble());
		}
	}
	
	private void checkAndCreateNode(long nodeId)
	{
		if (!accum_nodeUpTime.containsKey(nodeId))
		{
			/* Initialize node information */
			accum_nodeUpTime.put(nodeId, new MutableDouble());
			accum_nodeTotalTime.put(nodeId, new MutableDouble());
		}
	}
	
	private void checkAndCreateNode(long layerId, long nodeId)
	{
		if (!accum_avgNodeInDegree.get(layerId).containsKey(nodeId))
		{
			accum_avgNodeInDegree.get(layerId).put(nodeId, new MutableDouble());
			minNodeInDegree.get(layerId).put(nodeId, Integer.MAX_VALUE);
			maxNodeInDegree.get(layerId).put(nodeId, 0);
			accum_avgNodeOutDegree.get(layerId).put(nodeId, new MutableDouble());
			minNodeOutDegree.get(layerId).put(nodeId, Integer.MAX_VALUE);
			maxNodeOutDegree.get(layerId).put(nodeId, 0);
			accum_avgNodeIngressTraffic.get(layerId).put(nodeId, new MutableDouble());
			maxNodeIngressTraffic.get(layerId).put(nodeId, 0.0);
			minNodeIngressTraffic.get(layerId).put(nodeId, Double.MAX_VALUE);
			accum_avgNodeEgressTraffic.get(layerId).put(nodeId, new MutableDouble());
			maxNodeEgressTraffic.get(layerId).put(nodeId, 0.0);
			minNodeEgressTraffic.get(layerId).put(nodeId, Double.MAX_VALUE);
		}
	}
	
	/**
	 * Computes statistics for the current simulation time.
	 *
	 * @param simTime Current simulation time
	 * @since 0.3.0
	 */
	public void computeNextState(double simTime)
	{
		/* Do not update metrics for events in the same simulation time */
		if (simTime > lastEventTime)
		{
			/*
			 * Update metrics:
			 * - Cumulative metrics: accum += previous * timeInterval
			 * - Max/min metrics: metric = max/min(metric, previous)
			 */
			double timeInterval = simTime - lastEventTime;

			/* Network metrics */
			int numLayers = previousState_layerIds.size();
			accum_avgNumLayers += numLayers * timeInterval;
			minNumLayers = Math.min(numLayers, minNumLayers);
			maxNumLayers = Math.max(numLayers, maxNumLayers);

			int numNodes = previousState_nodeIds.size();
			accum_avgNumNodes += numNodes * timeInterval;
			minNumNodes = Math.min(numNodes, minNumNodes);
			maxNumNodes = Math.max(numNodes, maxNumNodes);
			
			for(long nodeId : previousState_nodeIds)
			{
				checkAndCreateNode(nodeId);

				if (netState.getNodeFromId (nodeId) != null)
				{
					if (!previousState_nodeDownIds.contains(nodeId)) accum_nodeUpTime.get(nodeId).add(timeInterval);
					accum_nodeTotalTime.get(nodeId).add(timeInterval);
				}
				else
				{
					accum_nodeUpTime.remove(nodeId);
					accum_nodeTotalTime.remove(nodeId);
				}
			}

			for(long layerId : previousState_layerIds)
			{
				checkAndCreateLayer(layerId);

				if (netState.getNetworkLayerFromId (layerId) != null)
				{
					accum_layerTotalTime.get(layerId).add(timeInterval);

					for(long nodeId : previousState_nodeIds)
					{
						checkAndCreateNode(layerId, nodeId);

						if (netState.getNodeFromId (nodeId) != null)
						{
							int nodeInDegree = previousState_nodeInDegree.get(layerId).get(nodeId);
							accum_avgNodeInDegree.get(layerId).get(nodeId).add(nodeInDegree * timeInterval);
							minNodeInDegree.get(layerId).put(nodeId, Math.min(nodeInDegree, minNodeInDegree.get(layerId).get(nodeId)));
							maxNodeInDegree.get(layerId).put(nodeId, Math.max(nodeInDegree, maxNodeInDegree.get(layerId).get(nodeId)));

							int nodeOutDegree = previousState_nodeOutDegree.get(layerId).get(nodeId);
							accum_avgNodeOutDegree.get(layerId).get(nodeId).add(nodeOutDegree * timeInterval);
							minNodeOutDegree.get(layerId).put(nodeId, Math.min(nodeInDegree, minNodeOutDegree.get(layerId).get(nodeId)));
							maxNodeOutDegree.get(layerId).put(nodeId, Math.max(nodeInDegree, maxNodeOutDegree.get(layerId).get(nodeId)));

							double nodeIngressTraffic = previousState_nodeIngressTraffic.get(layerId).get(nodeId);
							accum_avgNodeIngressTraffic.get(layerId).get(nodeId).add(nodeIngressTraffic * timeInterval);
							minNodeIngressTraffic.get(layerId).put(nodeId, Math.min(nodeIngressTraffic, minNodeIngressTraffic.get(layerId).get(nodeId)));
							maxNodeIngressTraffic.get(layerId).put(nodeId, Math.max(nodeIngressTraffic, maxNodeIngressTraffic.get(layerId).get(nodeId)));

							double nodeEgressTraffic = previousState_nodeEgressTraffic.get(layerId).get(nodeId);
							accum_avgNodeEgressTraffic.get(layerId).get(nodeId).add(nodeEgressTraffic * timeInterval);
							minNodeEgressTraffic.get(layerId).put(nodeId, Math.min(nodeEgressTraffic, minNodeEgressTraffic.get(layerId).get(nodeId)));
							maxNodeEgressTraffic.get(layerId).put(nodeId, Math.max(nodeEgressTraffic, maxNodeEgressTraffic.get(layerId).get(nodeId)));

						}
						else
						{
							accum_avgNodeInDegree.get(layerId).remove(nodeId);
							minNodeInDegree.get(layerId).remove(nodeId);
							maxNodeInDegree.get(layerId).remove(nodeId);
							accum_avgNodeOutDegree.get(layerId).remove(nodeId);
							minNodeOutDegree.get(layerId).remove(nodeId);
							maxNodeOutDegree.get(layerId).remove(nodeId);
							accum_avgNodeIngressTraffic.get(layerId).remove(nodeId);
							maxNodeIngressTraffic.get(layerId).remove(nodeId);
							minNodeIngressTraffic.get(layerId).remove(nodeId);
							accum_avgNodeEgressTraffic.get(layerId).remove(nodeId);
							maxNodeEgressTraffic.get(layerId).remove(nodeId);
							minNodeEgressTraffic.get(layerId).remove(nodeId);
						}
					}
					
					Set<Long> previousState_linkIds_thisLayer = previousState_linkIds.get(layerId);
					int numLinks = previousState_linkIds_thisLayer.size();
					accum_avgNumLinks.get(layerId).add(numLinks * timeInterval);
					maxNumLinks.put(layerId, Math.max(numLinks, maxNumLinks.get(layerId)));
					minNumLinks.put(layerId, Math.min(numLinks, minNumLinks.get(layerId)));
					
					Set<Long> previousState_demandIds_thisLayer = previousState_demandIds.get(layerId);
					int numDemands = previousState_demandIds_thisLayer.size();
					accum_avgNumDemands.get(layerId).add(numDemands * timeInterval);
					maxNumDemands.put(layerId, Math.max(numDemands, maxNumDemands.get(layerId)));
					minNumDemands.put(layerId, Math.min(numDemands, minNumDemands.get(layerId)));

					double totalCapacityInstalled = 0;
					double congestion = 0;
					
					Set<Long> previousState_linkDownIds_thisLayer = previousState_linkDownIds.get(layerId);
					Map<Long, Double> previousState_linkCapacity_thisLayer = previousState_linkCapacity.get(layerId);
					Map<Long, Double> previousState_linkOccupiedCapacity_thisLayer = previousState_linkOccupiedCapacity.get(layerId);
					Map<Long, Double> previousState_linkLengthInKm_thisLayer = previousState_linkLengthInKm.get(layerId);
					for(long linkId : previousState_linkIds_thisLayer)
					{
						double u_e = previousState_linkCapacity_thisLayer.get(linkId);
						double y_e = previousState_linkOccupiedCapacity_thisLayer.get(linkId);
						double rho_e = y_e == 0 ? 0 : Math.max(y_e / u_e, 0);
						
						totalCapacityInstalled += u_e;
						congestion = Math.max(congestion, rho_e);
						
						checkAndCreateLink(layerId, linkId);

						if (netState.getLinkFromId (linkId) != null)
						{
							double l_e = previousState_linkLengthInKm_thisLayer.get(linkId);
							double oversubscribedCapacity = y_e - u_e; if (oversubscribedCapacity < precisionFactor) oversubscribedCapacity = 0;

							accum_avgLinkLengthInKm.get(layerId).get(linkId).add(l_e * timeInterval);
							minLinkLengthInKm.get(layerId).get(linkId).setValue(Math.min(minLinkLengthInKm.get(layerId).get(linkId).doubleValue(), l_e));
							maxLinkLengthInKm.get(layerId).get(linkId).setValue(Math.max(maxLinkLengthInKm.get(layerId).get(linkId).doubleValue(), l_e));
							accum_avgCapacity.get(layerId).get(linkId).add(u_e * timeInterval);
							minCapacity.get(layerId).get(linkId).setValue(Math.min(minCapacity.get(layerId).get(linkId).doubleValue(), u_e));
							maxCapacity.get(layerId).get(linkId).setValue(Math.max(maxCapacity.get(layerId).get(linkId).doubleValue(), u_e));
							accum_avgLinkOccupiedCapacity.get(layerId).get(linkId).add(y_e * timeInterval);
							minLinkOccupiedCapacity.get(layerId).get(linkId).setValue(Math.min(minLinkOccupiedCapacity.get(layerId).get(linkId).doubleValue(), y_e));
							maxLinkOccupiedCapacity.get(layerId).get(linkId).setValue(Math.max(maxLinkOccupiedCapacity.get(layerId).get(linkId).doubleValue(), y_e));
							accum_avgUtilization.get(layerId).get(linkId).add(rho_e * timeInterval);
							minUtilization.get(layerId).get(linkId).setValue(Math.min(minUtilization.get(layerId).get(linkId).doubleValue(), rho_e));
							maxUtilization.get(layerId).get(linkId).setValue(Math.max(maxUtilization.get(layerId).get(linkId).doubleValue(), rho_e));
							accum_avgOversubscribedCapacity.get(layerId).get(linkId).add(oversubscribedCapacity * timeInterval);
							minOversubscribedCapacity.get(layerId).get(linkId).setValue(Math.min(minOversubscribedCapacity.get(layerId).get(linkId).doubleValue(), oversubscribedCapacity));
							maxOversubscribedCapacity.get(layerId).get(linkId).setValue(Math.max(maxOversubscribedCapacity.get(layerId).get(linkId).doubleValue(), oversubscribedCapacity));
							if (oversubscribedCapacity > 0) accum_linkOversubscribedTime.get(layerId).get(linkId).add(timeInterval);
							if (!previousState_linkDownIds_thisLayer.contains(linkId)) accum_linkUpTime.get(layerId).get(linkId).add(timeInterval);
							accum_linkTotalTime.get(layerId).get(linkId).add(timeInterval);
						}
						else
						{
							accum_avgLinkLengthInKm.get(layerId).remove(linkId);
							minLinkLengthInKm.get(layerId).remove(linkId);
							maxLinkLengthInKm.get(layerId).remove(linkId);
							accum_avgCapacity.get(layerId).remove(linkId);
							minCapacity.get(layerId).remove(linkId);
							maxCapacity.get(layerId).remove(linkId);
							accum_avgLinkOccupiedCapacity.get(layerId).remove(linkId);
							minLinkOccupiedCapacity.get(layerId).remove(linkId);
							maxLinkOccupiedCapacity.get(layerId).remove(linkId);
							accum_avgUtilization.get(layerId).remove(linkId);
							minUtilization.get(layerId).remove(linkId);
							maxUtilization.get(layerId).remove(linkId);
							accum_avgOversubscribedCapacity.get(layerId).remove(linkId);
							minOversubscribedCapacity.get(layerId).remove(linkId);
							maxOversubscribedCapacity.get(layerId).remove(linkId);
							accum_linkOversubscribedTime.get(layerId).remove(linkId);
							accum_linkUpTime.get(layerId).remove(linkId);
							accum_linkTotalTime.get(layerId).remove(linkId);
						}
					}
					
					accum_avgTotalCapacity.get(layerId).add(totalCapacityInstalled * timeInterval);
					maxTotalCapacity.get(layerId).setValue(Math.max(totalCapacityInstalled, maxTotalCapacity.get(layerId).doubleValue()));
					minTotalCapacity.get(layerId).setValue(Math.min(totalCapacityInstalled, minTotalCapacity.get(layerId).doubleValue()));
					accum_avgCongestion.get(layerId).add(congestion * timeInterval);
					maxCongestion.get(layerId).setValue(Math.max(congestion, maxCongestion.get(layerId).doubleValue()));
					minCongestion.get(layerId).setValue(Math.min(congestion, minCongestion.get(layerId).doubleValue()));
					
					double totalOfferedTraffic = 0;
					double totalCarriedTraffic = 0;
					double totalBlockedTraffic = 0;

					double worstDemandAvailabilityClassic_thisLayer = 1;
					double worstDemandAvailabilityWeighted_thisLayer = 1;

					Map<Long, Double> previousState_demandOfferedTraffic_thislayer = previousState_demandOfferedTraffic.get(layerId);
					Map<Long, Double> previousState_demandCarriedTraffic_thisLayer = previousState_demandCarriedTraffic.get(layerId);
					for(long demandId : previousState_demandIds_thisLayer)
					{
						double h_d = previousState_demandOfferedTraffic_thislayer.get(demandId);
						double r_d = previousState_demandCarriedTraffic_thisLayer.get(demandId);
						double blockedTraffic_d = h_d - r_d; if (blockedTraffic_d < precisionFactor) blockedTraffic_d = 0;
						totalOfferedTraffic += h_d;
						totalCarriedTraffic += r_d;
						totalBlockedTraffic += blockedTraffic_d;
						
						checkAndCreateDemand(layerId, demandId);
							
						if (netState.getDemandFromId (demandId) != null)
						{
							double excessCarriedTraffic_d = r_d - h_d; if (excessCarriedTraffic_d < precisionFactor) excessCarriedTraffic_d = 0;

							accum_avgDemandOfferedTraffic.get(layerId).get(demandId).add(h_d * timeInterval);
							minDemandOfferedTraffic.get(layerId).get(demandId).setValue(Math.min(minDemandOfferedTraffic.get(layerId).get(demandId).doubleValue(), h_d));
							maxDemandOfferedTraffic.get(layerId).get(demandId).setValue(Math.max(maxDemandOfferedTraffic.get(layerId).get(demandId).doubleValue(), h_d));
							accum_avgDemandCarriedTraffic.get(layerId).get(demandId).add(r_d * timeInterval);
							minDemandCarriedTraffic.get(layerId).get(demandId).setValue(Math.min(minDemandCarriedTraffic.get(layerId).get(demandId).doubleValue(), r_d));
							maxDemandCarriedTraffic.get(layerId).get(demandId).setValue(Math.max(maxDemandCarriedTraffic.get(layerId).get(demandId).doubleValue(), r_d));
							accum_avgDemandBlockedTraffic.get(layerId).get(demandId).add(blockedTraffic_d * timeInterval);
							minDemandBlockedTraffic.get(layerId).get(demandId).setValue(Math.min(minDemandBlockedTraffic.get(layerId).get(demandId).doubleValue(), blockedTraffic_d));
							maxDemandBlockedTraffic.get(layerId).get(demandId).setValue(Math.max(maxDemandBlockedTraffic.get(layerId).get(demandId).doubleValue(), blockedTraffic_d));
							if (blockedTraffic_d == 0) accum_demandAvailabilityClassic.get(layerId).get(demandId).add(timeInterval);
							accum_demandAvailabilityWeighted.get(layerId).get(demandId).add(h_d > 0 ? (1 - blockedTraffic_d / h_d) * timeInterval : timeInterval);
							accum_avgExcessCarriedTraffic.get(layerId).get(demandId).add(excessCarriedTraffic_d * timeInterval);
							minDemandExcessCarriedTraffic.get(layerId).get(demandId).setValue(Math.min(minDemandExcessCarriedTraffic.get(layerId).get(demandId).doubleValue(), excessCarriedTraffic_d));
							maxDemandExcessCarriedTraffic.get(layerId).get(demandId).setValue(Math.max(maxDemandExcessCarriedTraffic.get(layerId).get(demandId).doubleValue(), excessCarriedTraffic_d));
							if (excessCarriedTraffic_d > 0) excessDemandCarriedTrafficTime.get(layerId).get(demandId).add(timeInterval);
							demandTotalTime.get(layerId).get(demandId).add(timeInterval);
						}
						else
						{
							double totalTime_thisDemand = demandTotalTime.get(layerId).get(demandId).doubleValue();
							if (totalTime_thisDemand > 0)
							{
								worstDemandAvailabilityClassic_thisLayer = Math.min(worstDemandAvailabilityClassic_thisLayer, accum_demandAvailabilityClassic.get(layerId).get(demandId).doubleValue() / totalTime_thisDemand);
								worstDemandAvailabilityWeighted_thisLayer = Math.min(worstDemandAvailabilityWeighted_thisLayer, accum_demandAvailabilityWeighted.get(layerId).get(demandId).doubleValue() / totalTime_thisDemand);
							}
							
							accum_avgDemandOfferedTraffic.get(layerId).remove(demandId);
							minDemandOfferedTraffic.get(layerId).remove(demandId);
							maxDemandOfferedTraffic.get(layerId).remove(demandId);
							accum_avgDemandCarriedTraffic.get(layerId).remove(demandId);
							minDemandCarriedTraffic.get(layerId).remove(demandId);
							maxDemandCarriedTraffic.get(layerId).remove(demandId);
							accum_avgDemandBlockedTraffic.get(layerId).remove(demandId);
							minDemandBlockedTraffic.get(layerId).remove(demandId);
							maxDemandBlockedTraffic.get(layerId).remove(demandId);
							accum_demandAvailabilityClassic.get(layerId).remove(demandId);
							accum_demandAvailabilityWeighted.get(layerId).remove(demandId);
							accum_avgExcessCarriedTraffic.get(layerId).remove(demandId);
							minDemandExcessCarriedTraffic.get(layerId).remove(demandId);
							maxDemandExcessCarriedTraffic.get(layerId).remove(demandId);
							excessDemandCarriedTrafficTime.get(layerId).remove(demandId);
							demandTotalTime.get(layerId).remove(demandId);
						}
					}
					
					accum_avgTotalOfferedTraffic.get(layerId).add(totalOfferedTraffic * timeInterval);
					maxTotalOfferedTraffic.get(layerId).setValue(Math.max(totalOfferedTraffic, maxTotalOfferedTraffic.get(layerId).doubleValue()));
					minTotalOfferedTraffic.get(layerId).setValue(Math.min(totalOfferedTraffic, minTotalOfferedTraffic.get(layerId).doubleValue()));
					accum_avgTotalCarriedTraffic.get(layerId).add(totalCarriedTraffic * timeInterval);
					maxTotalCarriedTraffic.get(layerId).setValue(Math.max(totalCarriedTraffic, maxTotalCarriedTraffic.get(layerId).doubleValue()));
					minTotalCarriedTraffic.get(layerId).setValue(Math.min(totalCarriedTraffic, minTotalCarriedTraffic.get(layerId).doubleValue()));
					if (totalBlockedTraffic < precisionFactor) accum_availabilityClassic.get(layerId).add(timeInterval);
					accum_availabilityWeighted.get(layerId).add(totalOfferedTraffic > 0 ? Math.min(1, 1 - totalBlockedTraffic / totalOfferedTraffic) * timeInterval : timeInterval);
					worstDemandAvailabilityClassic.get(layerId).setValue(Math.min(worstDemandAvailabilityClassic_thisLayer, worstDemandAvailabilityClassic.get(layerId).doubleValue()));
					worstDemandAvailabilityWeighted.get(layerId).setValue(Math.min(worstDemandAvailabilityWeighted_thisLayer, worstDemandAvailabilityWeighted.get(layerId).doubleValue()));
				}
				else
				{
					/* Remove layer information */
					accum_layerTotalTime.remove(layerId);
					accum_avgNumLinks.remove(layerId);
					accum_avgNumDemands.remove(layerId);
					accum_avgTotalOfferedTraffic.remove(layerId);
					maxTotalOfferedTraffic.remove(layerId);
					minTotalOfferedTraffic.remove(layerId);
					accum_avgTotalCarriedTraffic.remove(layerId);
					maxTotalCarriedTraffic.remove(layerId);
					minTotalCarriedTraffic.remove(layerId);
					accum_avgTotalCapacity.remove(layerId);
					maxTotalCapacity.remove(layerId);
					minTotalCapacity.remove(layerId);
					accum_avgCongestion.remove(layerId);
					maxCongestion.remove(layerId);
					minCongestion.remove(layerId);
					accum_availabilityClassic.remove(layerId);
					accum_availabilityWeighted.remove(layerId);
					worstDemandAvailabilityClassic.remove(layerId);
					worstDemandAvailabilityWeighted.remove(layerId);
					maxNumLinks.remove(layerId);
					minNumLinks.remove(layerId);
					maxNumDemands.remove(layerId);
					minNumDemands.remove(layerId);

					/* Remove node information in this layer */
					accum_avgNodeInDegree.remove(layerId);
					minNodeInDegree.remove(layerId);
					maxNodeInDegree.remove(layerId);
					accum_avgNodeOutDegree.remove(layerId);
					minNodeOutDegree.remove(layerId);
					maxNodeOutDegree.remove(layerId);
					accum_avgNodeIngressTraffic.remove(layerId);
					maxNodeIngressTraffic.remove(layerId);
					minNodeIngressTraffic.remove(layerId);
					accum_avgNodeEgressTraffic.remove(layerId);
					maxNodeEgressTraffic.remove(layerId);
					minNodeEgressTraffic.remove(layerId);

					/* Remove link information in this layer */
					accum_avgLinkLengthInKm.remove(layerId);
					minLinkLengthInKm.remove(layerId);
					maxLinkLengthInKm.remove(layerId);
					accum_avgCapacity.remove(layerId);
					minCapacity.remove(layerId);
					maxCapacity.remove(layerId);
					accum_avgLinkOccupiedCapacity.remove(layerId);
					minLinkOccupiedCapacity.remove(layerId);
					maxLinkOccupiedCapacity.remove(layerId);
					accum_avgUtilization.remove(layerId);
					minUtilization.remove(layerId);
					maxUtilization.remove(layerId);
					accum_avgOversubscribedCapacity.remove(layerId);
					minOversubscribedCapacity.remove(layerId);
					maxOversubscribedCapacity.remove(layerId);
					accum_linkOversubscribedTime.remove(layerId);
					accum_linkUpTime.remove(layerId);
					accum_linkTotalTime.remove(layerId);

					/* Remove demand information in this layer */
					accum_avgDemandOfferedTraffic.remove(layerId);
					minDemandOfferedTraffic.remove(layerId);
					maxDemandOfferedTraffic.remove(layerId);
					accum_avgDemandCarriedTraffic.remove(layerId);
					minDemandCarriedTraffic.remove(layerId);
					maxDemandCarriedTraffic.remove(layerId);
					accum_avgDemandBlockedTraffic.remove(layerId);
					minDemandBlockedTraffic.remove(layerId);
					maxDemandBlockedTraffic.remove(layerId);
					accum_avgExcessCarriedTraffic.remove(layerId);
					minDemandExcessCarriedTraffic.remove(layerId);
					maxDemandExcessCarriedTraffic.remove(layerId);
					accum_demandAvailabilityClassic.remove(layerId);
					accum_demandAvailabilityWeighted.remove(layerId);
					excessDemandCarriedTrafficTime.remove(layerId);
					demandTotalTime.remove(layerId);
				}
			}
		}
		
		/* Update previous state (previous = current) */
		previousState_layerIds = new LinkedHashSet<Long>(netState.getNetworkLayerIds());
		previousState_nodeIds = new LinkedHashSet<Long>(netState.getNodeIds());
		previousState_nodeDownIds = new LinkedHashSet<Long>(NetPlan.getIds (netState.getNodesDown()));
		previousState_nodeInDegree = new LinkedHashMap<Long, Map<Long, Integer>>();
		previousState_nodeOutDegree = new LinkedHashMap<Long, Map<Long, Integer>>();
		previousState_nodeIngressTraffic = new LinkedHashMap<Long, Map<Long, Double>>();
		previousState_nodeEgressTraffic = new LinkedHashMap<Long, Map<Long, Double>>();
		previousState_linkIds = new LinkedHashMap<Long, Set<Long>>();
		previousState_linkDownIds = new LinkedHashMap<Long, Set<Long>>();
		previousState_linkLengthInKm = new LinkedHashMap<Long, Map<Long, Double>>();
		previousState_linkCapacity = new LinkedHashMap<Long, Map<Long, Double>>();
		previousState_linkOccupiedCapacity = new LinkedHashMap<Long, Map<Long, Double>>();
		previousState_demandIds = new LinkedHashMap<Long, Set<Long>>();
		previousState_demandOfferedTraffic = new LinkedHashMap<Long, Map<Long, Double>>();
		previousState_demandCarriedTraffic = new LinkedHashMap<Long, Map<Long, Double>>();
		
		/* Compute node, link metrics */
		for(long layerId : previousState_layerIds)
		{
			NetworkLayer netStateLayer = netState.getNetworkLayerFromId (layerId);
			previousState_nodeInDegree.put(layerId, new LinkedHashMap<Long, Integer>());
			previousState_nodeOutDegree.put(layerId, new LinkedHashMap<Long, Integer>());
			previousState_nodeIngressTraffic.put(layerId, new LinkedHashMap<Long, Double>());
			previousState_nodeEgressTraffic.put(layerId, new LinkedHashMap<Long, Double>());
			for(long nodeId : previousState_nodeIds)
			{
				Node netStateNode = netState.getNodeFromId (nodeId);
				previousState_nodeInDegree.get(layerId).put(nodeId, netStateNode.getIncomingLinks(netStateLayer).size());
				previousState_nodeOutDegree.get(layerId).put(nodeId, netStateNode.getOutgoingLinks(netStateLayer).size());
				previousState_nodeIngressTraffic.get(layerId).put(nodeId, netStateNode.getIngressCarriedTraffic(netStateLayer));
				previousState_nodeEgressTraffic.get(layerId).put(nodeId, netStateNode.getEgressCarriedTraffic(netStateLayer));
			}

			previousState_linkIds.put(layerId, new LinkedHashSet<Long>(netState.getLinkIds(netStateLayer)));
			previousState_linkDownIds.put(layerId, new LinkedHashSet<Long>(NetPlan.getIds (netState.getLinksDown(netStateLayer))));
			previousState_linkCapacity.put(layerId, new LinkedHashMap<Long, Double>());
			previousState_linkOccupiedCapacity.put(layerId, new LinkedHashMap<Long, Double>());
			previousState_linkLengthInKm.put(layerId, new LinkedHashMap<Long, Double>());
			for(long linkId : previousState_linkIds.get(layerId))
			{
				Link netStateLink = netState.getLinkFromId (linkId);
				previousState_linkCapacity.get(layerId).put(linkId, netStateLink.getCapacity());
				previousState_linkOccupiedCapacity.get(layerId).put(linkId, netStateLink.getOccupiedCapacity());
				previousState_linkLengthInKm.get(layerId).put(linkId, netStateLink.getLengthInKm());
			}
		}
		
		/* Compute demand metrics */
		for(long layerId : previousState_layerIds)
		{
			NetworkLayer netStateLayer = netState.getNetworkLayerFromId (layerId);
			previousState_demandIds.put(layerId, new LinkedHashSet<Long>(netState.getDemandIds(netStateLayer)));
			previousState_demandOfferedTraffic.put(layerId, new LinkedHashMap<Long, Double>());
			previousState_demandCarriedTraffic.put(layerId, new LinkedHashMap<Long, Double>());
			for(long demandId : previousState_demandIds.get(layerId))
			{
				Demand netStateDemand = netState.getDemandFromId (demandId);
				previousState_demandOfferedTraffic.get(layerId).put(demandId, netStateDemand.getOfferedTraffic());
				previousState_demandCarriedTraffic.get(layerId).put(demandId, netStateDemand.getCarriedTraffic());
			}
		}
		
		lastEventTime = simTime;
	}
	
	/**
	 * Resets the statistics.
	 * 
	 * @param simTime Current simulation time
	 * @since 0.2.3
	 */
	public void reset(double simTime)
	{
		lastEventTime = simTime;
		
		/* Network information */
		accum_avgNumLayers = 0;
		accum_avgNumNodes = 0;
		maxNumLayers = 0;
		maxNumNodes = 0;
		minNumLayers = Integer.MAX_VALUE;
		minNumNodes = Integer.MAX_VALUE;

		/* Layer information */
		accum_layerTotalTime = new LinkedHashMap<Long, MutableDouble>();
		accum_avgNumLinks = new LinkedHashMap<Long, MutableDouble>();
		accum_avgNumDemands = new LinkedHashMap<Long, MutableDouble>();
		accum_avgTotalOfferedTraffic = new LinkedHashMap<Long, MutableDouble>();
		maxTotalOfferedTraffic = new LinkedHashMap<Long, MutableDouble>();
		minTotalOfferedTraffic = new LinkedHashMap<Long, MutableDouble>();
		accum_avgTotalCarriedTraffic = new LinkedHashMap<Long, MutableDouble>();
		maxTotalCarriedTraffic = new LinkedHashMap<Long, MutableDouble>();
		minTotalCarriedTraffic = new LinkedHashMap<Long, MutableDouble>();
		accum_avgTotalCapacity = new LinkedHashMap<Long, MutableDouble>();
		maxTotalCapacity = new LinkedHashMap<Long, MutableDouble>();
		minTotalCapacity = new LinkedHashMap<Long, MutableDouble>();
		accum_avgCongestion = new LinkedHashMap<Long, MutableDouble>();
		maxCongestion = new LinkedHashMap<Long, MutableDouble>();
		minCongestion = new LinkedHashMap<Long, MutableDouble>();
		accum_availabilityClassic = new LinkedHashMap<Long, MutableDouble>();
		accum_availabilityWeighted = new LinkedHashMap<Long, MutableDouble>();
		worstDemandAvailabilityClassic = new LinkedHashMap<Long, MutableDouble>();
		worstDemandAvailabilityWeighted = new LinkedHashMap<Long, MutableDouble>();
		maxNumLinks = new LinkedHashMap<Long, Integer>();
		minNumLinks = new LinkedHashMap<Long, Integer>();
		maxNumDemands = new LinkedHashMap<Long, Integer>();
		minNumDemands = new LinkedHashMap<Long, Integer>();
	
		/* Node information */
		accum_nodeUpTime = new LinkedHashMap<Long, MutableDouble>();
		accum_nodeTotalTime = new LinkedHashMap<Long, MutableDouble>();
		accum_avgNodeInDegree = new LinkedHashMap<Long, Map<Long, MutableDouble>>();
		maxNodeInDegree = new LinkedHashMap<Long, Map<Long, Integer>>();
		minNodeInDegree = new LinkedHashMap<Long, Map<Long, Integer>>();
		accum_avgNodeOutDegree = new LinkedHashMap<Long, Map<Long, MutableDouble>>();
		maxNodeOutDegree = new LinkedHashMap<Long, Map<Long, Integer>>();
		minNodeOutDegree = new LinkedHashMap<Long, Map<Long, Integer>>();
		accum_avgNodeIngressTraffic = new LinkedHashMap<Long, Map<Long, MutableDouble>>();
		maxNodeIngressTraffic = new LinkedHashMap<Long, Map<Long, Double>>();
		minNodeIngressTraffic = new LinkedHashMap<Long, Map<Long, Double>>();
		accum_avgNodeEgressTraffic = new LinkedHashMap<Long, Map<Long, MutableDouble>>();
		maxNodeEgressTraffic = new LinkedHashMap<Long, Map<Long, Double>>();
		minNodeEgressTraffic = new LinkedHashMap<Long, Map<Long, Double>>();
		
		/* Link information */
		accum_avgLinkLengthInKm = new LinkedHashMap<Long, Map<Long, MutableDouble>>();
		minLinkLengthInKm = new LinkedHashMap<Long, Map<Long, MutableDouble>>();
		maxLinkLengthInKm = new LinkedHashMap<Long, Map<Long, MutableDouble>>();
		accum_avgCapacity = new LinkedHashMap<Long, Map<Long, MutableDouble>>();
		minCapacity = new LinkedHashMap<Long, Map<Long, MutableDouble>>();
		maxCapacity = new LinkedHashMap<Long, Map<Long, MutableDouble>>();
		accum_avgLinkOccupiedCapacity = new LinkedHashMap<Long, Map<Long, MutableDouble>>();
		minLinkOccupiedCapacity = new LinkedHashMap<Long, Map<Long, MutableDouble>>();
		maxLinkOccupiedCapacity = new LinkedHashMap<Long, Map<Long, MutableDouble>>();
		accum_avgUtilization = new LinkedHashMap<Long, Map<Long, MutableDouble>>();
		minUtilization = new LinkedHashMap<Long, Map<Long, MutableDouble>>();
		maxUtilization = new LinkedHashMap<Long, Map<Long, MutableDouble>>();
		accum_avgOversubscribedCapacity = new LinkedHashMap<Long, Map<Long, MutableDouble>>();
		minOversubscribedCapacity = new LinkedHashMap<Long, Map<Long, MutableDouble>>();
		maxOversubscribedCapacity = new LinkedHashMap<Long, Map<Long, MutableDouble>>();
		accum_linkOversubscribedTime = new LinkedHashMap<Long, Map<Long, MutableDouble>>();
		accum_linkUpTime = new LinkedHashMap<Long, Map<Long, MutableDouble>>();
		accum_linkTotalTime = new LinkedHashMap<Long, Map<Long, MutableDouble>>();

		/* Demand information */
		accum_avgDemandOfferedTraffic = new LinkedHashMap<Long, Map<Long, MutableDouble>>();
		minDemandOfferedTraffic = new LinkedHashMap<Long, Map<Long, MutableDouble>>();
		maxDemandOfferedTraffic = new LinkedHashMap<Long, Map<Long, MutableDouble>>();
		accum_avgDemandCarriedTraffic = new LinkedHashMap<Long, Map<Long, MutableDouble>>();
		minDemandCarriedTraffic = new LinkedHashMap<Long, Map<Long, MutableDouble>>();
		maxDemandCarriedTraffic = new LinkedHashMap<Long, Map<Long, MutableDouble>>();
		accum_avgDemandBlockedTraffic = new LinkedHashMap<Long, Map<Long, MutableDouble>>();
		minDemandBlockedTraffic = new LinkedHashMap<Long, Map<Long, MutableDouble>>();
		maxDemandBlockedTraffic = new LinkedHashMap<Long, Map<Long, MutableDouble>>();
		accum_avgExcessCarriedTraffic = new LinkedHashMap<Long, Map<Long, MutableDouble>>();
		minDemandExcessCarriedTraffic = new LinkedHashMap<Long, Map<Long, MutableDouble>>();
		maxDemandExcessCarriedTraffic = new LinkedHashMap<Long, Map<Long, MutableDouble>>();
		accum_demandAvailabilityClassic = new LinkedHashMap<Long, Map<Long, MutableDouble>>();
		accum_demandAvailabilityWeighted = new LinkedHashMap<Long, Map<Long, MutableDouble>>();
		excessDemandCarriedTrafficTime = new LinkedHashMap<Long, Map<Long, MutableDouble>>();
		demandTotalTime = new LinkedHashMap<Long, Map<Long, MutableDouble>>();
		
		computeNextState(simTime);
		transitoryTime = simTime;
	};

	/**
	 * Returns a HTML {@code String} with statistics.
	 * 
	 * @param simTime Current simulation time
	 * @return Statistics in HTML format
	 * @since 0.2.3
	 */
	public String getResults(double simTime)
	{
		if (lastEventTime == 0) return "<p>No event was processed</p>";
		
		double totalSimulationTime = simTime - transitoryTime;
		if (totalSimulationTime == 0) return "<p>Simulation time equal to zero. No results</p>";
		
//		computeNextState(simTime+0.0000000000001);
		try (ByteArrayOutputStream os = new ByteArrayOutputStream())
		{
			XMLOutputFactory2 output = (XMLOutputFactory2) XMLOutputFactory.newFactory();
			XMLStreamWriter2 writer = (XMLStreamWriter2) output.createXMLStreamWriter(os);
			
			writer.writeStartDocument("UTF-8", "1.0");
			
			/* Write network information */
			writer.writeStartElement("network");
			writer.writeAttribute("avgNumLayers", String.format("%.3f", totalSimulationTime > 0 ? accum_avgNumLayers / totalSimulationTime : 0));
			
			int minNumLayers_thisNetwork = minNumLayers;
			if (minNumLayers_thisNetwork == Integer.MAX_VALUE) minNumLayers_thisNetwork = 0;
			writer.writeAttribute("minNumLayers", Integer.toString(minNumLayers_thisNetwork));
			writer.writeAttribute("maxNumLayers", Integer.toString(maxNumLayers));
			writer.writeAttribute("avgNumNodes", String.format("%.3f", totalSimulationTime > 0 ? accum_avgNumNodes / totalSimulationTime : 0));
			
			int minNumNodes_thisNetwork = minNumNodes;
			if (minNumNodes_thisNetwork == Integer.MAX_VALUE) minNumNodes_thisNetwork = 0;
			writer.writeAttribute("minNumNodes", Integer.toString(minNumNodes_thisNetwork));
			writer.writeAttribute("maxNumNodes", Integer.toString(maxNumNodes));
			
			/* Write node information */
			Collection<Long> nodeIds = netState.getNodeIds();
			for(long nodeId : nodeIds)
			{
				checkAndCreateNode(nodeId);
				
				double upTime_thisNode = accum_nodeUpTime.get(nodeId).doubleValue();
				double totalTime_thisNode = accum_nodeTotalTime.get(nodeId).doubleValue();
				double upTimePercentage_thisNode = totalTime_thisNode > 0 ? 100 * upTime_thisNode / totalTime_thisNode : 0;
				
				writer.writeStartElement("node");
				writer.writeAttribute("id", Long.toString(nodeId));
				writer.writeAttribute("name", netState.getNodeFromId(nodeId).getName ());
				writer.writeAttribute("upTime", StringUtils.secondsToYearsDaysHoursMinutesSeconds(upTime_thisNode));
				writer.writeAttribute("upTimePercentage", String.format("%.3f", upTimePercentage_thisNode));
				writer.writeAttribute("totalTime", StringUtils.secondsToYearsDaysHoursMinutesSeconds(totalTime_thisNode));
				writer.writeEndElement();	
			}			
			
			/* Write layer information */
			for(long layerId : netState.getNetworkLayerIds ())
			{
				checkAndCreateLayer(layerId);
				
				double totalTime_thisLayer = accum_layerTotalTime.get(layerId).doubleValue();
				
				NetworkLayer netStateLayer = netState.getNetworkLayerFromId (layerId);
				
				String trafficUnitsName = netState.getDemandTrafficUnitsName(netStateLayer);
				if (trafficUnitsName.isEmpty()) trafficUnitsName = "none";
				String capacityUnitsName = netState.getLinkCapacityUnitsName(netStateLayer);
				if (capacityUnitsName.isEmpty()) capacityUnitsName = "none";
				
				writer.writeStartElement("layer");
				writer.writeAttribute("id", Long.toString(layerId));
				writer.writeAttribute("name", netStateLayer.getName ());
				writer.writeAttribute("avgNumLinks", String.format("%.3f", totalTime_thisLayer > 0 ? accum_avgNumLinks.get(layerId).doubleValue() / totalTime_thisLayer : 0));

				int minNumLinks_thisLayer = minNumLinks.get(layerId);
				if (minNumLinks_thisLayer == Integer.MAX_VALUE) minNumLinks_thisLayer = 0;
				writer.writeAttribute("minNumLinks", Integer.toString(minNumLinks_thisLayer));
				writer.writeAttribute("maxNumLinks", Integer.toString(maxNumLinks.get(layerId)));
				writer.writeAttribute("avgNumDemands", String.format("%.3f", totalTime_thisLayer > 0 ? accum_avgNumDemands.get(layerId).doubleValue() / totalTime_thisLayer : 0));

				int minNumDemands_thisLayer = minNumDemands.get(layerId);
				if (minNumDemands_thisLayer == Integer.MAX_VALUE) minNumDemands_thisLayer = 0;
				writer.writeAttribute("minNumDemands", Integer.toString(minNumDemands_thisLayer));
				writer.writeAttribute("maxNumDemands", Integer.toString(maxNumDemands.get(layerId)));
				writer.writeAttribute("totalTime", StringUtils.secondsToYearsDaysHoursMinutesSeconds(totalTime_thisLayer));
				writer.writeAttribute("trafficUnitsName", trafficUnitsName);
				writer.writeAttribute("avgOfferedTraffic", String.format("%.3f", totalTime_thisLayer > 0 ? accum_avgTotalOfferedTraffic.get(layerId).doubleValue() / totalTime_thisLayer : 0));
				
				double minTotalOfferedTraffic_thisLayer = minTotalOfferedTraffic.get(layerId).doubleValue();
				if (minTotalOfferedTraffic_thisLayer == Double.MAX_VALUE) minTotalOfferedTraffic_thisLayer = 0;
				writer.writeAttribute("minOfferedTraffic", String.format("%.3f", minTotalOfferedTraffic_thisLayer));
				writer.writeAttribute("maxOfferedTraffic", String.format("%.3f", maxTotalOfferedTraffic.get(layerId).doubleValue()));
				writer.writeAttribute("avgCarriedTraffic", String.format("%.3f", totalTime_thisLayer > 0 ? accum_avgTotalCarriedTraffic.get(layerId).doubleValue() / totalTime_thisLayer : 0));
				
				double minTotalCarriedTraffic_thisLayer = minTotalCarriedTraffic.get(layerId).doubleValue();
				if (minTotalCarriedTraffic_thisLayer == Double.MAX_VALUE) minTotalCarriedTraffic_thisLayer = 0;
				writer.writeAttribute("minCarriedTraffic", String.format("%.3f", minTotalCarriedTraffic_thisLayer));
				writer.writeAttribute("maxCarriedTraffic", String.format("%.3f", maxTotalCarriedTraffic.get(layerId).doubleValue()));
				writer.writeAttribute("capacityUnitsName", capacityUnitsName);
				writer.writeAttribute("avgTotalCapacity", String.format("%.3f", totalTime_thisLayer > 0 ? accum_avgTotalCapacity.get(layerId).doubleValue() / totalTime_thisLayer : 0));
				
				double minTotalCapacity_thisLayer = minTotalCapacity.get(layerId).doubleValue();
				if (minTotalCapacity_thisLayer == Double.MAX_VALUE) minTotalCapacity_thisLayer = 0;
				writer.writeAttribute("minTotalCapacity", String.format("%.3f", minTotalCapacity_thisLayer));
				writer.writeAttribute("maxTotalCapacity", String.format("%.3f", maxTotalCapacity.get(layerId).doubleValue()));
				writer.writeAttribute("avgCongestion", String.format("%.3f", totalTime_thisLayer > 0 ? accum_avgCongestion.get(layerId).doubleValue() / totalTime_thisLayer : 0));
				
				double minCongestion_thisLayer = minCongestion.get(layerId).doubleValue();
				if (minCongestion_thisLayer == Double.MAX_VALUE) minCongestion_thisLayer = 0;
				writer.writeAttribute("minCongestion", String.format("%.3f", minCongestion_thisLayer));
				writer.writeAttribute("maxCongestion", String.format("%.3f", maxCongestion.get(layerId).doubleValue()));
				writer.writeAttribute("availabilityClassic", String.format("%.6f", totalTime_thisLayer > 0 ? accum_availabilityClassic.get(layerId).doubleValue() / totalTime_thisLayer : 0));
				writer.writeAttribute("availabilityWeighted", String.format("%.6f", totalTime_thisLayer > 0 ? accum_availabilityWeighted.get(layerId).doubleValue() / totalTime_thisLayer : 0));
				
				double worstDemandAvailabilityClassic_thisLayer = worstDemandAvailabilityClassic.get(layerId).doubleValue();
				double worstDemandAvailabilityWeighted_thisLayer = worstDemandAvailabilityWeighted.get(layerId).doubleValue();

				/* Write demand information */
				Collection<Long> demandIds_thisLayer = netState.getDemandIds(netStateLayer);
				for(long demandId : netState.getDemandIds (netStateLayer))
				{
					checkAndCreateDemand(layerId, demandId);
					
					double totalTime_thisDemand = demandTotalTime.get(layerId).get(demandId).doubleValue();
					worstDemandAvailabilityClassic_thisLayer = Math.min(worstDemandAvailabilityClassic_thisLayer, totalTime_thisDemand > 0 ? accum_demandAvailabilityClassic.get(layerId).get(demandId).doubleValue() / totalTime_thisDemand : 0);
					worstDemandAvailabilityWeighted_thisLayer = Math.min(worstDemandAvailabilityWeighted_thisLayer, totalTime_thisDemand > 0 ? accum_demandAvailabilityWeighted.get(layerId).get(demandId).doubleValue() / totalTime_thisDemand : 0);
				}

				writer.writeAttribute("worstDemandAvailabilityClassic", String.format("%.6f", worstDemandAvailabilityClassic_thisLayer));
				writer.writeAttribute("worstDemandAvailabilityWeighted", String.format("%.6f", worstDemandAvailabilityWeighted_thisLayer));
				
				/* Write node information */
				for(long nodeId : nodeIds)
				{
					checkAndCreateNode(layerId, nodeId);
					
					double totalTime_thisNode_thisLayer = Math.min(accum_nodeTotalTime.get(nodeId).doubleValue(), totalTime_thisLayer);
					writer.writeStartElement("node");
					writer.writeAttribute("id", Long.toString(nodeId));
					writer.writeAttribute("name", netState.getNodeFromId(nodeId).getName ());
					writer.writeAttribute("avgInDegree", String.format("%.3f", totalTime_thisNode_thisLayer > 0 ? accum_avgNodeInDegree.get(layerId).get(nodeId).doubleValue() / totalTime_thisNode_thisLayer : 0));
					
					int minNodeInDegree_thisNode_thisLayer = minNodeInDegree.get(layerId).get(nodeId);
					if (minNodeInDegree_thisNode_thisLayer == Integer.MAX_VALUE) minNodeInDegree_thisNode_thisLayer = 0;
					writer.writeAttribute("minInDegree", Integer.toString(minNodeInDegree_thisNode_thisLayer));
					writer.writeAttribute("maxInDegree", Integer.toString(maxNodeInDegree.get(layerId).get(nodeId)));
					writer.writeAttribute("avgOutDegree", String.format("%.3f", totalTime_thisNode_thisLayer > 0 ? accum_avgNodeOutDegree.get(layerId).get(nodeId).doubleValue() / totalTime_thisNode_thisLayer : 0));
					
					int minNodeOutDegree_thisNode_thisLayer = minNodeOutDegree.get(layerId).get(nodeId);
					if (minNodeOutDegree_thisNode_thisLayer == Integer.MAX_VALUE) minNodeOutDegree_thisNode_thisLayer = 0;
					writer.writeAttribute("minOutDegree", Integer.toString(minNodeOutDegree_thisNode_thisLayer));
					writer.writeAttribute("maxOutDegree", Integer.toString(maxNodeOutDegree.get(layerId).get(nodeId)));
					writer.writeAttribute("avgIngressTraffic", String.format("%.3f", totalTime_thisNode_thisLayer > 0 ? accum_avgNodeIngressTraffic.get(layerId).get(nodeId).doubleValue() / totalTime_thisNode_thisLayer : 0));
					
					double minNodeIngressTraffic_thisNode_thisLayer = minNodeIngressTraffic.get(layerId).get(nodeId);
					if (minNodeIngressTraffic_thisNode_thisLayer == Double.MAX_VALUE) minNodeIngressTraffic_thisNode_thisLayer = 0;
					writer.writeAttribute("minIngressTraffic", String.format("%.3f", minNodeIngressTraffic.get(layerId).get(nodeId)));
					writer.writeAttribute("maxIngressTraffic", String.format("%.3f", maxNodeIngressTraffic.get(layerId).get(nodeId)));
					writer.writeAttribute("avgEgressTraffic", String.format("%.3f", totalTime_thisNode_thisLayer > 0 ? accum_avgNodeEgressTraffic.get(layerId).get(nodeId).doubleValue() / totalTime_thisNode_thisLayer : 0));
					
					double minNodeEgressTraffic_thisNode_thisLayer = minNodeEgressTraffic.get(layerId).get(nodeId);
					if (minNodeEgressTraffic_thisNode_thisLayer == Double.MAX_VALUE) minNodeEgressTraffic_thisNode_thisLayer = 0;
					writer.writeAttribute("minEgressTraffic", String.format("%.3f", minNodeEgressTraffic_thisNode_thisLayer));
					writer.writeAttribute("maxEgressTraffic", String.format("%.3f", maxNodeEgressTraffic.get(layerId).get(nodeId)));
					
					writer.writeEndElement();	
				}			
				
				/* Write link information */
				Collection<Long> linkIds_thisLayer = netState.getLinkIds(netState.getNetworkLayerFromId (layerId));
				for(long linkId : linkIds_thisLayer)
				{
					checkAndCreateLink(layerId, linkId);
					Link netStateLink = netState.getLinkFromId (linkId);
					long originNodeId_thisLink = netStateLink.getOriginNode().getId ();
					long destinationNodeId_thisLink = netStateLink.getDestinationNode().getId ();
					String originNodeName = netStateLink.getOriginNode().getName ();
					String destinationNodeName = netStateLink.getDestinationNode().getName ();
					double upTime_thisLink = accum_linkUpTime.get(layerId).get(linkId).doubleValue();
					double totalTime_thisLink = accum_linkTotalTime.get(layerId).get(linkId).doubleValue();
					double upTimePercentage_thisLink = totalTime_thisLink > 0 ? 100 * upTime_thisLink / totalTime_thisLink : 0;
					double oversubscribedTime_thisLink = accum_linkOversubscribedTime.get(layerId).get(linkId).doubleValue();
					double oversubscribedTimePercentage_thisLink = totalTime_thisLink > 0 ? 100 * oversubscribedTime_thisLink / totalTime_thisLink : 0;
					
					writer.writeStartElement("link");
					writer.writeAttribute("id", Long.toString(linkId));
					writer.writeAttribute("originNode", originNodeName.isEmpty() ? Long.toString(originNodeId_thisLink) : String.format("%d (%s)", originNodeId_thisLink, originNodeName));
					writer.writeAttribute("destinationNode", destinationNodeName.isEmpty() ? Long.toString(destinationNodeId_thisLink) : String.format("%d (%s)", destinationNodeId_thisLink, destinationNodeName));
					writer.writeAttribute("avgLengthInKm", String.format("%.3f", totalTime_thisLink > 0 ? accum_avgLinkLengthInKm.get(layerId).get(linkId).doubleValue() / totalTime_thisLink : 0));
					
					double minLinkLengthInKm_thisLink = minLinkLengthInKm.get(layerId).get(linkId).doubleValue();
					if (minLinkLengthInKm_thisLink == Double.MAX_VALUE) minLinkLengthInKm_thisLink = 0;
					writer.writeAttribute("minLengthInKm", String.format("%.3f", minLinkLengthInKm_thisLink));
					writer.writeAttribute("maxLengthInKm", String.format("%.3f", maxLinkLengthInKm.get(layerId).get(linkId).doubleValue()));
					writer.writeAttribute("avgCapacity", String.format("%.3f", totalTime_thisLink > 0 ? accum_avgCapacity.get(layerId).get(linkId).doubleValue() / totalTime_thisLink : 0));
					
					double minCapacity_thisLayer = minCapacity.get(layerId).get(linkId).doubleValue();
					if (minCapacity_thisLayer == Double.MAX_VALUE) minCapacity_thisLayer = 0;
					writer.writeAttribute("minCapacity", String.format("%.3f", minCapacity_thisLayer));
					writer.writeAttribute("maxCapacity", String.format("%.3f", maxCapacity.get(layerId).get(linkId).doubleValue()));
					writer.writeAttribute("avgOccupiedCapacity", String.format("%.3f", totalTime_thisLink > 0 ? accum_avgLinkOccupiedCapacity.get(layerId).get(linkId).doubleValue() / totalTime_thisLink : 0));
					
					double minCarriedTraffic_thisLink = minLinkOccupiedCapacity.get(layerId).get(linkId).doubleValue();
					if (minCarriedTraffic_thisLink == Double.MAX_VALUE) minCarriedTraffic_thisLink = 0;
					writer.writeAttribute("minOccupiedCapacity", String.format("%.3f", minCarriedTraffic_thisLink));
					writer.writeAttribute("maxOccupiedCapacity", String.format("%.3f", maxLinkOccupiedCapacity.get(layerId).get(linkId).doubleValue()));
					
					writer.writeAttribute("avgUtilization", String.format("%.3f", totalTime_thisLink > 0 ? accum_avgUtilization.get(layerId).get(linkId).doubleValue() / totalTime_thisLink : 0));
					
					double minUtilization_thisLink = minUtilization.get(layerId).get(linkId).doubleValue();
					if (minUtilization_thisLink == Double.MAX_VALUE) minUtilization_thisLink = 0;
					writer.writeAttribute("minUtilization", String.format("%.3f", minUtilization_thisLink));
					writer.writeAttribute("maxUtilization", String.format("%.3f", maxUtilization.get(layerId).get(linkId).doubleValue()));
					
					writer.writeAttribute("avgOversubscribedCapacity", String.format("%.3f", totalTime_thisLink > 0 ? accum_avgOversubscribedCapacity.get(layerId).get(linkId).doubleValue() / totalTime_thisLink : 0));
					
					double minOversubscribedCapacity_thisLink = minOversubscribedCapacity.get(layerId).get(linkId).doubleValue();
					if (minOversubscribedCapacity_thisLink == Double.MAX_VALUE) minOversubscribedCapacity_thisLink = 0;
					writer.writeAttribute("minOversubscribedCapacity", String.format("%.3f", minOversubscribedCapacity_thisLink));
					writer.writeAttribute("maxOversubscribedCapacity", String.format("%.3f", maxOversubscribedCapacity.get(layerId).get(linkId).doubleValue()));
					writer.writeAttribute("oversubscribedTime", StringUtils.secondsToYearsDaysHoursMinutesSeconds(oversubscribedTime_thisLink));
					writer.writeAttribute("oversubscribedTimePercentage", String.format("%.3f", oversubscribedTimePercentage_thisLink));
					writer.writeAttribute("upTime", StringUtils.secondsToYearsDaysHoursMinutesSeconds(upTime_thisLink));
					writer.writeAttribute("upTimePercentage", String.format("%.3f", upTimePercentage_thisLink));
					writer.writeAttribute("totalTime", StringUtils.secondsToYearsDaysHoursMinutesSeconds(totalTime_thisLink));
					writer.writeEndElement();	
				}
				
				/* Write demand information */
				for(long demandId : demandIds_thisLayer)
				{
					Demand netStateDemand = netState.getDemandFromId (demandId);
					long ingressNodeId_thisDemand = netStateDemand.getIngressNode().getId ();
					long egressNodeId_thisDemand = netStateDemand.getEgressNode().getId ();
					String ingressNodeName = netStateDemand.getIngressNode().getName ();
					String egressNodeName = netStateDemand.getEgressNode().getName ();
					double totalTime_thisDemand = demandTotalTime.get(layerId).get(demandId).doubleValue();
					double excessCarriedTrafficTime_thisDemand = excessDemandCarriedTrafficTime.get(layerId).get(demandId).doubleValue();
					double excessCarriedTrafficTimePercentage_thisDemand = totalTime_thisDemand > 0 ? 100 * excessCarriedTrafficTime_thisDemand / totalTime_thisDemand : 0;

					writer.writeStartElement("demand");
					writer.writeAttribute("id", Long.toString(demandId));
					writer.writeAttribute("ingressNode", ingressNodeName.isEmpty() ? Long.toString(ingressNodeId_thisDemand) : String.format("%d (%s)", ingressNodeId_thisDemand, ingressNodeName));
					writer.writeAttribute("egressNode", egressNodeName.isEmpty() ? Long.toString(egressNodeId_thisDemand) : String.format("%d (%s)", egressNodeId_thisDemand, egressNodeName));
					writer.writeAttribute("avgOfferedTraffic", String.format("%.3f", totalTime_thisDemand > 0 ? accum_avgDemandOfferedTraffic.get(layerId).get(demandId).doubleValue() / totalTime_thisDemand : 0));
					
					double minOfferedTraffic_thisDemand = minDemandOfferedTraffic.get(layerId).get(demandId).doubleValue();
					if (minOfferedTraffic_thisDemand == Double.MAX_VALUE) minOfferedTraffic_thisDemand = 0;
					writer.writeAttribute("minOfferedTraffic", String.format("%.3f", minOfferedTraffic_thisDemand));
					writer.writeAttribute("maxOfferedTraffic", String.format("%.3f", maxDemandOfferedTraffic.get(layerId).get(demandId).doubleValue()));
					writer.writeAttribute("avgCarriedTraffic", String.format("%.3f", totalTime_thisDemand > 0 ? accum_avgDemandCarriedTraffic.get(layerId).get(demandId).doubleValue() / totalTime_thisDemand : 0));
					
					double minCarriedTraffic_thisDemand = minDemandCarriedTraffic.get(layerId).get(demandId).doubleValue();
					if (minCarriedTraffic_thisDemand == Double.MAX_VALUE) minCarriedTraffic_thisDemand = 0;
					writer.writeAttribute("minCarriedTraffic", String.format("%.3f", minCarriedTraffic_thisDemand));
					writer.writeAttribute("maxCarriedTraffic", String.format("%.3f", maxDemandCarriedTraffic.get(layerId).get(demandId).doubleValue()));
					writer.writeAttribute("avgBlockedTraffic", String.format("%.3f", totalTime_thisDemand > 0 ? accum_avgDemandBlockedTraffic.get(layerId).get(demandId).doubleValue() / totalTime_thisDemand : 0));
					
					double minBlockedTraffic_thisDemand = minDemandBlockedTraffic.get(layerId).get(demandId).doubleValue();
					if (minBlockedTraffic_thisDemand == Double.MAX_VALUE) minBlockedTraffic_thisDemand = 0;
					writer.writeAttribute("minBlockedTraffic", String.format("%.3f", minBlockedTraffic_thisDemand));
					writer.writeAttribute("maxBlockedTraffic", String.format("%.3f", maxDemandBlockedTraffic.get(layerId).get(demandId).doubleValue()));
					writer.writeAttribute("availabilityClassic", String.format("%.6f", totalTime_thisDemand > 0 ? accum_demandAvailabilityClassic.get(layerId).get(demandId).doubleValue() / totalTime_thisDemand : 0));
					writer.writeAttribute("availabilityWeighted", String.format("%.6f", totalTime_thisDemand > 0 ? accum_demandAvailabilityWeighted.get(layerId).get(demandId).doubleValue() / totalTime_thisDemand : 0));
					writer.writeAttribute("avgExcessCarriedTraffic", String.format("%.3f", totalTime_thisDemand > 0 ? accum_avgExcessCarriedTraffic.get(layerId).get(demandId).doubleValue() / totalTime_thisDemand : 0));
					
					double minExcessCarriedTraffic_thisDemand = minDemandExcessCarriedTraffic.get(layerId).get(demandId).doubleValue();
					if (minExcessCarriedTraffic_thisDemand == Double.MAX_VALUE) minExcessCarriedTraffic_thisDemand = 0;
					writer.writeAttribute("minExcessCarriedTraffic", String.format("%.3f", minExcessCarriedTraffic_thisDemand));
					writer.writeAttribute("maxExcessCarriedTraffic", String.format("%.3f", maxDemandExcessCarriedTraffic.get(layerId).get(demandId).doubleValue()));
					writer.writeAttribute("excessCarriedTrafficTime", StringUtils.secondsToYearsDaysHoursMinutesSeconds(excessCarriedTrafficTime_thisDemand));
					writer.writeAttribute("excessCarriedTrafficTimePercentage", String.format("%.3f", excessCarriedTrafficTimePercentage_thisDemand));
					writer.writeAttribute("totalTime", StringUtils.secondsToYearsDaysHoursMinutesSeconds(totalTime_thisDemand));
					writer.writeEndElement();	
				}
				
				writer.writeEndElement();	
			}			

			writer.writeEndElement();
			writer.writeEndDocument();
			writer.flush();
			writer.close();

			String xml = os.toString(StandardCharsets.UTF_8.name());
			return HTMLUtils.getHTMLFromXML(xml, SimStats.class.getResource("/sim/SimStats.xsl").toURI().toURL());
		}
		catch(Throwable e)
		{
			throw new RuntimeException(e);
		}
	}
}
