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
package com.net2plan.gui.plugins.networkDesign.focusPane;

import com.google.common.collect.Sets;
import com.net2plan.gui.plugins.networkDesign.visualizationControl.VisualizationState;
import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.internal.Constants.NetworkElementType;
import com.net2plan.libraries.SRGUtils;
import com.net2plan.utils.Constants.RoutingCycleType;
import com.net2plan.utils.DoubleUtils;
import com.net2plan.utils.Pair;
import com.net2plan.utils.Triple;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

public class FocusPane extends JPanel
{
	private final GUINetworkDesign callback;

	public FocusPane (GUINetworkDesign callback)
	{
		super ();

		this.callback = callback;

        this.setVisible(true);
        this.setLayout(new BorderLayout(0,0));
	}

	public void updateView ()
	{


		this.removeAll();
		this.repaint();
		final VisualizationState vs = callback.getVisualizationState();
		final NetworkElementType elementType = vs.getPickedElementType();

        final List<NetworkElement> pickedElement = vs.getPickedNetworkElements();
        final List<Pair<Demand, Link>> pickedForwardingRule = vs.getPickedForwardingRules();

        assert pickedElement != null;
        assert pickedForwardingRule != null;

		/* Return empty panel if zero or more than one element is picked */
		/* Check if remove everything */
		if (elementType == null) return; 
        if (pickedElement.size() > 1) return;
        if (pickedForwardingRule.size() > 1) return;

		/* Here if there is something new to show */
		if (elementType == NetworkElementType.ROUTE)
		{
			final Route r = (Route) pickedElement.get(0);
			final FigureLinkSequencePanel fig = new FigureLinkSequencePanel(callback , r.getPath() , r.getLayer() , r.getSeqOccupiedCapacitiesIfNotFailing(), r.getCarriedTraffic(), "Route " + r.getIndex() );
			this.add(fig , BorderLayout.WEST);
			this.add(createPanelInfo(getRouteInfoTables(r), r) , BorderLayout.CENTER);
		}
		else if (elementType == NetworkElementType.DEMAND)
		{
			final Demand d = (Demand) pickedElement.get(0);
			final FigureDemandSequencePanel fig = new FigureDemandSequencePanel(callback, d, "Demand " + d.getIndex());
			this.add(fig , BorderLayout.WEST);
			this.add(createPanelInfo(getDemandInfoTables(d), d) , BorderLayout.CENTER);
		}
		else if (elementType == NetworkElementType.FORWARDING_RULE)
		{
			final Pair<Demand,Link> fr = vs.getPickedForwardingRules().get(0);
			final FigureForwardingRuleSequencePanel fig = new FigureForwardingRuleSequencePanel(callback, fr, callback.getDesign().getNetworkLayerDefault(), "Forwarding rule");
			this.add(fig , BorderLayout.WEST);
			this.add(createPanelInfo(getForwardingRuleInfoTables(fr), null) , BorderLayout.CENTER);
		}
		else if (elementType == NetworkElementType.LAYER)
		{
			final NetworkLayer layer = (NetworkLayer) pickedElement.get(0);
//			final LinkSequencePanel fig = new LinkSequencePanel(r.getPath() , r.getLayer() , r.getSeqOccupiedCapacitiesIfNotFailing() , "Route " + r.getIndex() , r.getCarriedTraffic());
//			this.add(fig , BorderLayout.WEST);
			this.add(createPanelInfo(getLayerInfoTables(layer), layer) , BorderLayout.CENTER);
		}
		else if (elementType == NetworkElementType.LINK)
		{
			final Link e = (Link) pickedElement.get(0);
			final FigureLinkSequencePanel fig = new FigureLinkSequencePanel(callback , Arrays.asList(e) , e.getLayer() , Arrays.asList(e.getOccupiedCapacity()), e.getCarriedTraffic(), "Link " + e.getIndex());
			this.add(fig , BorderLayout.WEST);
			this.add(createPanelInfo(getLinkInfoTables(e), e) , BorderLayout.CENTER);
		}
		else if (elementType == NetworkElementType.MULTICAST_DEMAND)
		{
			final MulticastDemand md = (MulticastDemand) pickedElement.get(0);
			final FigureMulticastDemandSequencePanel fig = new FigureMulticastDemandSequencePanel(callback, md, "Multicast demand " + md.getIndex());
			this.add(fig , BorderLayout.WEST);
			this.add(createPanelInfo(getMulticastDemandInfoTables(md), md) , BorderLayout.CENTER);
		}
		else if (elementType == NetworkElementType.MULTICAST_TREE)
		{
			final MulticastTree t = (MulticastTree) pickedElement.get(0);
			final FigureMulticastTreePanel fig = new FigureMulticastTreePanel(callback , t , "Multicast tree " + t.getIndex() , t.getCarriedTraffic());
			this.add(fig , BorderLayout.WEST);
			this.add(createPanelInfo(getMulticastTreeInfoTables(t), t) , BorderLayout.CENTER);
		}
		else if (elementType == NetworkElementType.NODE)
		{
			final Node n = (Node) pickedElement.get(0);
			final FigureNodeSequencePanel fig = new FigureNodeSequencePanel(callback , n , n.getNetPlan().getNetworkLayerDefault() , "Node " + n.getIndex());
			this.add(fig , BorderLayout.WEST);
			this.add(createPanelInfo(getNodeInfoTables(n , n.getNetPlan().getNetworkLayerDefault()), n) , BorderLayout.CENTER);
		}
		else if (elementType == NetworkElementType.RESOURCE)
		{
			final Resource r = (Resource) pickedElement.get(0);
			final FigureResourcePanel fig = new FigureResourcePanel(callback , r , getResourceName(r));
			this.add(fig , BorderLayout.WEST);
			this.add(createPanelInfo(getResourceInfoTables(r), r) , BorderLayout.CENTER);
		}
		else if (elementType == NetworkElementType.SRG)
		{
			final SharedRiskGroup srg = (SharedRiskGroup) pickedElement.get(0);
			final FigureSRGSequencePanel fig = new FigureSRGSequencePanel(callback, srg, "Shared risk group " + srg.getIndex());
			this.add(fig , BorderLayout.WEST);
			this.add(createPanelInfo(getSRGInfoTables(srg), srg) , BorderLayout.CENTER);
		}

		this.revalidate();
		this.repaint ();
	}

	private List<Triple<String,String,String>> getLayerInfoTables (NetworkLayer layer)
	{
		final DecimalFormat df = new DecimalFormat("###.##");
		final NetPlan np = layer.getNetPlan();
		final List<Triple<String,String,String>> res = new ArrayList <> ();
		final String capUnits = np.getLinkCapacityUnitsName(layer);
		final String trafUnits = np.getDemandTrafficUnitsName(layer);
		res.add(Triple.of("Layer index/id" , "Layer " + layer.getIndex() + " (id " + layer.getId() + ")", "layer" + layer.getId()));
		res.add(Triple.of("Name" , layer.getName().equals("")? "No name" : layer.getName(), ""));
		res.add(Triple.of("Description" , layer.getDescription().equals("")? "No description" : layer.getDescription(), ""));
		res.add(Triple.of("Demand traffic units" , trafUnits.equals("")? "Not specified" : trafUnits, ""));
		res.add(Triple.of("Link capacity units" , capUnits.equals("")? "Not specified" : capUnits, ""));
		res.add(Triple.of("Routing type" , layer.isSourceRouting()? "Source routing" : "Hop-by-hop routing", ""));
		final double totalOccupiedCap = np.getLinks(layer).stream().mapToDouble(e->e.getOccupiedCapacity()).sum();
		final double totalCap = np.getLinks(layer).stream().mapToDouble(e->e.getCapacity()).sum();
		final double totalOfferedTrac = np.getDemands(layer).stream().mapToDouble(e->e.getOfferedTraffic()).sum();
		final double totalCarriedTrac = np.getDemands(layer).stream().mapToDouble(e->e.getCarriedTraffic()).sum();
		final double totalOffMultTraffic = np.getMulticastDemands(layer).stream().mapToDouble(e->e.getOfferedTraffic()).sum();
		final double totalCarMultTraffic = np.getMulticastDemands(layer).stream().mapToDouble(e->e.getCarriedTraffic()).sum();
		res.add(Triple.of("# links (total occupied / capacity)", "" + np.getNumberOfLinks(layer) + " (" + df.format(totalOccupiedCap) + " / " + df.format(totalCap) + ") " + capUnits + ")" , ""));
		res.add(Triple.of("# demands (total carried / offered)", "" + np.getNumberOfDemands(layer) + " (" + df.format(totalCarriedTrac) + " / " + df.format(totalOfferedTrac) + ") " + trafUnits, ""));
		if (layer.isSourceRouting())
			res.add(Triple.of("# routes", "" + np.getNumberOfRoutes(layer) , ""));
		else
			res.add(Triple.of("# forwarding rules", "" + np.getNumberOfForwardingRules(layer) , ""));
		res.add(Triple.of("# multicast demands (total carried / offered)", "" + np.getNumberOfMulticastDemands(layer) + " (" + df.format(totalCarMultTraffic) + " / " + df.format(totalOffMultTraffic) + ") " + trafUnits, ""));
		res.add(Triple.of("# multicast trees", "" + np.getNumberOfMulticastTrees(layer) , ""));
		return res;
	}
	private List<Triple<String,String,String>> getNodeInfoTables (Node n , NetworkLayer layer)
	{
		final DecimalFormat df = new DecimalFormat("###.##");
		final NetPlan np = n.getNetPlan();
		final List<Triple<String,String,String>> res = new ArrayList <> ();
		final String trafUnits = np.getDemandTrafficUnitsName(layer);
		res.add(Triple.of("Node index/id" , "Node " + n.getIndex() + " (id " + n.getId() + ")", "node" + n.getId()));
		res.add(Triple.of("Name" , n.getName().equals("")? "No name" : n.getName(), ""));
		res.add(Triple.of("Coordinates (x,y)" , "(" + n.getXYPositionMap().getX() + "," + n.getXYPositionMap().getY() + ")" , ""));
		res.add(Triple.of("Is up?", "" + n.isUp() , ""));
		res.add(Triple.of("# Resources" , n.getResources().isEmpty() ? "none" : ""+n.getResources().size()  , ""));
		for (Resource r : n.getResources())
			res.add(Triple.of("- " + getResourceName(r) , "" , "resource" + r.getId()));
		res.add(Triple.of("Information at layer" , getLayerName(layer) , ""));
		res.add(Triple.of("# output links", "" + n.getOutgoingLinks(layer).size() , ""));
		res.add(Triple.of("# input links" , "" + n.getIncomingLinks(layer).size() , ""));
		res.add(Triple.of("Output traffic" , "" + df.format(n.getOutgoingLinks(layer).stream().mapToDouble(e->e.getCarriedTraffic()).sum()) + " " + trafUnits , ""));
		res.add(Triple.of("Input traffic" , df.format(n.getIncomingLinks(layer).stream().mapToDouble(e->e.getCarriedTraffic()).sum())  + " " + trafUnits , ""));
		res.add(Triple.of("# SRGs" , n.getSRGs().isEmpty() ? "none" : ""+n.getSRGs().size()  , ""));
		for (SharedRiskGroup srg : n.getSRGs())
			res.add(Triple.of("- Sh. risk group index/id" , "" + srg.getIndex() + " (id " + srg.getId() + ")" , "srg" + srg.getId()));
		return res;
	}
	private List<Triple<String,String,String>> getResourceInfoTables (Resource r)
	{
		final DecimalFormat df = new DecimalFormat("###.##");
		final NetPlan np = r.getNetPlan();
		final List<Triple<String,String,String>> res = new ArrayList <> ();
		final String resCapUnits = r.getCapacityMeasurementUnits();
		res.add(Triple.of("Resource index/id" , "Resource " + r.getIndex() + " (id " + r.getId() + ")", "resource" + r.getId()));
		res.add(Triple.of("Name" , r.getName().equals("")? "No name" : r.getName(), ""));
		res.add(Triple.of("Type" , r.getType() , ""));
		res.add(Triple.of("Host node" , getNodeName(r.getHostNode()) , ""));
		res.add(Triple.of("Capacity occupied / total" , df.format(r.getOccupiedCapacity()) + " / " + df.format(r.getCapacity()) + " " + resCapUnits , ""));
		res.add(Triple.of("Processing time" , df.format(r.getProcessingTimeToTraversingTrafficInMs()) + " ms", ""));
		res.add(Triple.of("Is up?", "" + r.getHostNode().isUp() , ""));
		res.add(Triple.of("# base resources", "" + r.getBaseResources().size() , ""));
		for (Resource br : r.getBaseResources())
			res.add(Triple.of(getResourceName(br) + " (" + br.getType() + ")" , "Occup: " + df.format(r.getCapacityOccupiedInBaseResource(br)) + " " + br.getCapacityMeasurementUnits() , "resource" + br.getId()));
		res.add(Triple.of("# upper resources", "" + r.getUpperResources().size() , ""));
		for (Resource ur : r.getUpperResources())
			res.add(Triple.of(getResourceName(ur) + " (" + ur.getType() + ")" , "Occup: " + df.format(r.getCapacityOccupiedByUpperResource(ur)) + " " + resCapUnits , "resource" + ur.getId()));
		res.add(Triple.of("# Traversing routes", "" + r.getTraversingRoutes().size() , ""));
		for (Route route : r.getTraversingRoutes())
			res.add(Triple.of("- Route " + route.getIndex() + " (" + getLayerName(route.getLayer()) + ")", "Occup: " + df.format(r.getTraversingRouteOccupiedCapacity(route)) + " " + resCapUnits   + r.getTraversingRoutes().size() , "route" + route.getId()));
		return res;
	}
	private List<Triple<String,String,String>> getSRGInfoTables (SharedRiskGroup srg)
	{
		final DecimalFormat df = new DecimalFormat("###.##");
		final NetPlan np = srg.getNetPlan();
		final List<Triple<String,String,String>> res = new ArrayList <> ();
		res.add(Triple.of("SRG index/id" , "SRG " + srg.getIndex() + " (id " + srg.getId() + ")", "srg" + srg.getId()));
		res.add(Triple.of("Mean Time To Fail (MTTF)" , df.format(srg.getMeanTimeToFailInHours() / 24) + " days", ""));
		res.add(Triple.of("Mean Time To Repair (MTTR)" , df.format(srg.getMeanTimeToRepairInHours() / 24) + " days", ""));
		res.add(Triple.of("Availability" , String.format("%.6f" , srg.getAvailability()) + " days", ""));
		res.add(Triple.of("#Nodes" , "" + srg.getNodes() , ""));
		for (Node n : srg.getNodes())
			res.add(Triple.of(getNodeName(n) , ""  , "node" + n.getId()));
		res.add(Triple.of("#Links" , "" + srg.getNodes() , ""));
		for (NetworkLayer layer : np.getNetworkLayers())
		{
			final Set<Link> linksThisLayer = srg.getLinks(layer);
			if (linksThisLayer.isEmpty()) continue;
			for (Link link : linksThisLayer)
				res.add(Triple.of("Link " + getNodeName(link.getOriginNode()) + " -> " + getNodeName(link.getDestinationNode()) , "Link " + link.getIndex() +" (id " + link.getId() + ")" , "link" + link.getId()));
		}
		return res;
	}
	private List<Triple<String,String,String>> getLinkInfoTables (Link e)
	{
		final DecimalFormat df = new DecimalFormat("###.##");
		final NetPlan np = e.getNetPlan();
		final List<Triple<String,String,String>> res = new ArrayList <> ();
		final NetworkLayer layer = e.getLayer();
		final String trafUnits = np.getDemandTrafficUnitsName(layer);
		final String capUnits = np.getLinkCapacityUnitsName(layer);
		final double max_rho_e = np.getLinks(layer).stream().mapToDouble(ee->ee.getUtilization()).max().orElse(0);
		res.add(Triple.of("Link index/id" , "Link " + e.getIndex() + " (id " + e.getId() + ")", "link" + e.getId()));
		res.add(Triple.of("Is up?", "" + e.isUp() , ""));
		res.add(Triple.of("Is coupled to lower layer?", e.isCoupled() + "" , ""));
		if (e.isCoupled())
		{
			final NetworkLayer lowerLayer = e.getCoupledDemand() == null? e.getCoupledMulticastDemand().getLayer() : e.getCoupledDemand().getLayer();
			res.add(Triple.of("- Lower layer coupled to",  getLayerName(lowerLayer) , ""));
			if (e.getCoupledDemand() == null)
			{
				final MulticastDemand md = e.getCoupledMulticastDemand();
				res.add(Triple.of("- Coupled to",  "Multicast demand " + md.getIndex() + " (id " + md.getId() + ")" , "multicastDemand" + md.getId()));
			}
			else
			{
				final Demand d = e.getCoupledDemand();
				res.add(Triple.of("- Coupled to",  "Demand " + d.getIndex() + " (id " + d.getId() + ")" , "demand" + d.getId()));
			}
		}
		res.add(Triple.of("Carried traffic", "" + df.format(e.getCarriedTraffic()) + " " + trafUnits , ""));
		res.add(Triple.of("Capacity occupied / total", "" + df.format(e.getOccupiedCapacity()) + " / " + df.format(e.getCapacity()) + " " + capUnits , ""));
		res.add(Triple.of("Utilization", "" + df.format(e.getUtilization()) , ""));
		res.add(Triple.of("Is bottleneck?", "" + DoubleUtils.isEqualWithinRelativeTolerance(max_rho_e, e.getUtilization(), Configuration.precisionFactor) , ""));
		res.add(Triple.of("Length (km)", "" + df.format(e.getLengthInKm()) + " km" , ""));
		res.add(Triple.of("Length (ms)", "" + df.format(e.getPropagationDelayInMs()) + " ms" , ""));
		if (layer.isSourceRouting())
			res.add(Triple.of("# routes (total / backup)", "" + e.getTraversingRoutes().size() + " / " + e.getTraversingBackupRoutes().size(), ""));
		else
			res.add(Triple.of("# forw. rules", "" + e.getForwardingRules().size(), ""));
		res.add(Triple.of("# multicast trees", "" + e.getTraversingTrees().size() , ""));
		final Set<SharedRiskGroup> affectingSRGs = SRGUtils.getAffectingSRGs(Arrays.asList(e));
		res.add(Triple.of("# Affecting SRGs", "" + affectingSRGs.size() , ""));
		for (SharedRiskGroup srg : affectingSRGs)
			res.add(Triple.of("- Sh. risk group index/id" , "" + srg.getIndex() + " (id " + srg.getId() + ")" , "srg" + srg.getId()));
		return res;
	}
	private List<Triple<String,String,String>> getForwardingRuleInfoTables (Pair<Demand,Link> fr)
	{
		final DecimalFormat df = new DecimalFormat("###.##");
		final Demand d = fr.getFirst();
		final Link e = fr.getSecond();
		final NetworkLayer layer = d.getLayer();
		final NetPlan np = e.getNetPlan();
		final List<Triple<String,String,String>> res = new ArrayList <> ();
		final String trafUnits = np.getDemandTrafficUnitsName(layer);
		final String capUnits = np.getLinkCapacityUnitsName(layer);
		res.add(Triple.of("Demand index/id" , "Demand " + d.getIndex() + " (id " + d.getId() + ")", "demand" + d.getId()));
		res.add(Triple.of("Link index/id" , "Link " + e.getIndex() + " (id " + e.getId() + ")", "link" + e.getId()));
		res.add(Triple.of("Splitting factor", "" + df.format(np.getForwardingRuleSplittingFactor(d,e)) , ""));
		res.add(Triple.of("Carried traffic", "" + df.format(np.getForwardingRuleCarriedTraffic(d,e)) + " " + trafUnits , ""));
		return res;
	}
	private List<Triple<String,String,String>> getRouteInfoTables (Route r)
	{
		final DecimalFormat df = new DecimalFormat("###.##");
		final NetPlan np = r.getNetPlan();
		final List<Triple<String,String,String>> res = new ArrayList <> ();
		res.add(Triple.of("Route index/id" , "Route " + r.getIndex() + " (id " + r.getId() + ")", "route" + r.getId()));
		res.add(Triple.of("Layer" , "" + getLayerName(r.getLayer()) , "layer" + r.getLayer().getId()));
		res.add(Triple.of("Route demand index/id" , "" + r.getDemand().getIndex() + " (id " + r.getDemand().getId() + ")" , "demand" + r.getDemand().getId()));
		res.add(Triple.of("Demand offered traffic" , "" + df.format(r.getDemand().getOfferedTraffic()) + " " + np.getDemandTrafficUnitsName(r.getLayer()) , ""));
		res.add(Triple.of("Demand carried traffic" , "" + df.format(r.getDemand().getCarriedTraffic()) + " " + np.getDemandTrafficUnitsName(r.getLayer()) , ""));
		res.add(Triple.of("Route carried traffic" , "" + df.format(r.getCarriedTraffic()) + " " + np.getDemandTrafficUnitsName(r.getLayer()), ""));
		res.add(Triple.of("Is up?" , "" + np.isUp(r.getPath()), ""));
		res.add(Triple.of("Worst link utilization" , "" + df.format(r.getSeqLinks().stream().mapToDouble(e->e.getUtilization()).max().orElse(0)), ""));
		if (r.isServiceChain())
			res.add(Triple.of("Worst resource utilization" , "" + df.format(r.getSeqResourcesTraversed().stream().mapToDouble(e->e.getUtilization()).max().orElse(0)), ""));
		res.add(Triple.of("Is service chain?" , "" + r.getDemand().isServiceChainRequest(), ""));
		res.add(Triple.of("Route length (km)" , "" + df.format(r.getLengthInKm()) + " km", ""));
		res.add(Triple.of("Route length (ms)" , "" + df.format(r.getPropagationDelayInMiliseconds()) + " ms", ""));
		res.add(Triple.of("Is backup route?" , "" + r.isBackupRoute(), ""));
		for (Route pr : r.getRoutesIAmBackup())
			res.add(Triple.of("-- Primary route" , "Route " + pr.getIndex() , "route" + pr.getId()));
		res.add(Triple.of("Has backup routes?" , "" + r.hasBackupRoutes(), ""));
		for (Route br : r.getBackupRoutes())
			res.add(Triple.of("-- Backup route" , "Route " + br.getIndex() , "route" + br.getId()));
		return res;
	}
	private List<Triple<String,String,String>> getMulticastTreeInfoTables (MulticastTree t)
	{
		final DecimalFormat df = new DecimalFormat("###.##");
		final NetPlan np = t.getNetPlan();
		final List<Triple<String,String,String>> res = new ArrayList <> ();
		res.add(Triple.of("Tree index/id" , "Tree " + t.getIndex() + " (id " + t.getId() + ")", "multicastTre" + t.getId()));
		res.add(Triple.of("Layer" , "" + getLayerName(t.getLayer()) , "layer" + t.getLayer().getId()));
		res.add(Triple.of("Tree demand" , "Multicast demand " + t.getMulticastDemand().getIndex() + " (id " + t.getMulticastDemand().getId() + ")" , "multicastDemand" + t.getMulticastDemand().getId()));
		res.add(Triple.of("M. Demand offered traffic" , "" + df.format(t.getMulticastDemand().getOfferedTraffic()) + " " + np.getDemandTrafficUnitsName(t.getLayer()) , ""));
		res.add(Triple.of("M. Demand carried traffic" , "" + df.format(t.getMulticastDemand().getCarriedTraffic()) + " " + np.getDemandTrafficUnitsName(t.getLayer()) , ""));
		res.add(Triple.of("Tree carried traffic" , "" + df.format(t.getCarriedTraffic()) + " " + np.getDemandTrafficUnitsName(t.getLayer()), ""));
		res.add(Triple.of("Is up?" , "" + !t.isDown(), ""));
		res.add(Triple.of("Worst link utilization" , "" + df.format(t.getLinkSet().stream().mapToDouble(e->e.getUtilization()).max().orElse(0)), ""));
		res.add(Triple.of("E2E num. hops (av / max)" , "" + df.format(t.getTreeAveragePathLengthInHops()) + " / " + df.format(t.getTreeMaximumPathLengthInHops()) , ""));
		res.add(Triple.of("E2E length in km (av / max)" , "" + df.format(t.getTreeAveragePathLengthInHops()) + " / " + df.format(t.getTreeMaximumPathLengthInKm()) + " km", ""));
		res.add(Triple.of("E2E length in ms (av / max)" , "" + df.format(t.getTreeAveragePropagationDelayInMs()) + " / " + df.format(t.getTreeMaximumPropagationDelayInMs()) + " ms", ""));
		return res;
	}
	private List<Triple<String,String,String>> getDemandInfoTables (Demand d)
	{
		final DecimalFormat df = new DecimalFormat("###.##");
		final NetPlan np = d.getNetPlan();
		final NetworkLayer layer = d.getLayer();
		final List<Triple<String,String,String>> res = new ArrayList <> ();
		final RoutingCycleType cycleType = d.getRoutingCycleType();
		final boolean isLoopless = cycleType.equals(RoutingCycleType.LOOPLESS);
		res.add(Triple.of("Demand index/id" , "Demand " + d.getIndex() + " (id " + d.getId() + ")", "demand" + d.getId()));
		res.add(Triple.of("Layer" , "" + getLayerName(layer) , "layer" + layer.getId()));
		res.add(Triple.of("Offered traffic" , "" + df.format(d.getOfferedTraffic()) , ""));
		res.add(Triple.of("Carried traffic" , "" + df.format(d.getCarriedTraffic()) , ""));
		res.add(Triple.of("Lost traffic" , "" + df.format(d.getBlockedTraffic()) , ""));
		res.add(Triple.of("Is coupled?" , "" + d.isCoupled() , ""));
		if (d.isCoupled())
		{
			final Link coupledLink = d.getCoupledLink();
			res.add(Triple.of("- Upper layer coupled to",  getLayerName(coupledLink.getLayer()) , ""));
			res.add(Triple.of("- Coupled to",  "Link " + coupledLink.getIndex() + " (id " + coupledLink.getId() + ")" , "link" + coupledLink.getId()));
		}
		res.add(Triple.of("Is service chain?" , "" + d.isServiceChainRequest(), ""));
		if (d.isServiceChainRequest())
			res.add(Triple.of("- Seq. resource types" , StringUtils.join(d.getServiceChainSequenceOfTraversedResourceTypes(),","), ""));
		res.add(Triple.of("Has loops?" , isLoopless? "No" : cycleType.equals(RoutingCycleType.CLOSED_CYCLES)? "Yes (closed loops)" : "Yes (open loops)", ""));
		res.add(Triple.of("Routing type" , layer.isSourceRouting()? "Source routing" : "Hop by hop", ""));
		if (layer.isSourceRouting())
		{
			res.add(Triple.of("Num. routes (total/backup)" , "" + d.getRoutes().size() + "/" + d.getRoutesAreBackup().size(), ""));
			for (Route r : d.getRoutes())
				res.add(Triple.of("Route index/id" , "Route " + r.getIndex() + " (id " + r.getId() + ")" + (r.isBackupRoute()? " [backup]" : ""), "route" + r.getId()));
		}
		res.add(Triple.of("Worst case e2e latency" , df.format(d.getWorstCasePropagationTimeInMs()) + " ms", ""));
		return res;
	}
	private List<Triple<String,String,String>> getMulticastDemandInfoTables (MulticastDemand md)
	{
		final DecimalFormat df = new DecimalFormat("###.##");
		final NetPlan np = md.getNetPlan();
		final NetworkLayer layer = md.getLayer();
		final List<Triple<String,String,String>> res = new ArrayList <> ();
		res.add(Triple.of("Demand index/id" , "M. demand " + md.getIndex() + " (id " + md.getId() + ")", "multicastDemand" + md.getId()));
		res.add(Triple.of("Layer" , "" + getLayerName(layer) , "layer" + layer.getId()));
		res.add(Triple.of("Offered traffic" , "" + df.format(md.getOfferedTraffic()) , ""));
		res.add(Triple.of("Carried traffic" , "" + df.format(md.getCarriedTraffic()) , ""));
		res.add(Triple.of("Lost traffic" , "" + df.format(md.getBlockedTraffic()) , ""));
		if (md.isCoupled())
		{
			final Set<Link> coupledLinks = md.getCoupledLinks();
			final NetworkLayer upperLayer = coupledLinks.iterator().next().getLayer();
			res.add(Triple.of("- Upper layer coupled to",  getLayerName(upperLayer) , "layer" + upperLayer.getId()));
			for (Link c : coupledLinks)
				res.add(Triple.of("- Coupled to",  "Link " + c.getIndex() + " (id " + c.getId() + ")" , "link" + c.getId()));
		}
		res.add(Triple.of("Num. trees" , "" + md.getMulticastTrees().size() , ""));
//		for (MulticastTree t : md.getMulticastTrees())
//			res.add(Triple.of("Multicast tree index/id" , "Multicast tree " + t.getIndex() + " (id " + t.getId() + ")" , "multicastTree" + t.getId()));
		res.add(Triple.of("Worst case e2e latency" , df.format(md.getWorseCasePropagationTimeInMs()) + " ms", ""));
		return res;
	}

	private String getNodeName (Node n) { return "Node " + n.getIndex() + " (" + (n.getName().length() == 0? "No name" : n.getName()) + ")"; }
	private String getResourceName (Resource e) { return "Resource " + e.getIndex() + " (" + (e.getName().length() == 0? "No name" : e.getName()) + "). Type: " + e.getType(); }
	private String getLayerName (NetworkLayer e) { return "Layer " + e.getIndex() + " (" + (e.getName().length() == 0? "No name" : e.getName()) + ")"; }

	class LabelWithLink extends JLabel implements MouseListener
	{
		private final String internalLink;
		public LabelWithLink(Pair<String,String> t , boolean bold)
		{
			super (t.getFirst());
			if (bold) this.setFont(new Font(getFont().getName(), Font.BOLD, getFont().getSize()));
			this.internalLink = t.getSecond();
			if (!internalLink.equals("")) this.setForeground(Color.BLUE);
			this.addMouseListener(this);
		}
		@Override
		public void mouseClicked(MouseEvent arg0)
		{
			processMouseClickInternalLink(this.internalLink , callback);
		}
		@Override
		public void mouseEntered(MouseEvent arg0)
		{
		}
		@Override
		public void mouseExited(MouseEvent arg0)
		{
		}
		@Override
		public void mousePressed(MouseEvent arg0)
		{
		}
		@Override
		public void mouseReleased(MouseEvent arg0)
		{
		}
	}


	private JPanel createPanelInfo (List<Triple<String,String,String>> infoRows , NetworkElement e)
	{
		final JPanel tablePanel = new JPanel ();
		tablePanel.setLayout(new MigLayout("gap rel 0"));
		tablePanel.add(new LabelWithLink(Pair.of("Information table" ,  ""), true) , "newline, gaptop 0 , gapbottom 0");
		tablePanel.add(new JLabel ("────────────────") , "newline, gaptop 0, gapbottom 2");
		for (Triple<String,String,String> info : infoRows)
		{
			tablePanel.add(new LabelWithLink(Pair.of(info.getFirst() + ":" ,  info.getThird()), true) , "newline, gaptop 0, gapright 5");
			tablePanel.add(new LabelWithLink(Pair.of(info.getSecond() ,  info.getThird()), false) , "gaptop 0");
		}
		tablePanel.add(new LabelWithLink(Pair.of("User-defined attributes" ,  ""), true) , "newline, gaptop 15 , gapbottom 0");
		tablePanel.add(new JLabel ("────────────────────") , "newline, gaptop 0, gapbottom 2");

		/* If forwarding rule => no attributes */
		if (e == null) return tablePanel;

		if (e.getAttributes().isEmpty())
			tablePanel.add(new LabelWithLink(Pair.of("No attributes defined", "") , false) , "newline, gaptop 0, gapright 5");
		for (Entry<String,String> entry : e.getAttributes().entrySet())
		{
			tablePanel.add(new LabelWithLink(Pair.of(entry.getKey() + ":" ,  ""), true) , "newline, gaptop 0, gapright 5");
			tablePanel.add(new LabelWithLink(Pair.of(entry.getValue() ,  "") , false) , "gaptop 0");
		}
		return tablePanel;
	}

	/* whenever we have a click using an internal link, is processed here */
	static void processMouseClickInternalLink (String internalLink , GUINetworkDesign callback)
	{
		final NetPlan np = callback.getDesign();
		final VisualizationState vs = callback.getVisualizationState();
		if (internalLink.equals("")) return;
		if (internalLink.startsWith("demand"))
		{
			final long id = Long.parseLong(internalLink.substring((int) "demand".length()));
			final Demand e = np.getDemandFromId(id);
			vs.pickElement(e);
			callback.updateVisualizationAfterPick();

		} else if (internalLink.startsWith("route"))
		{
			final long id = Long.parseLong(internalLink.substring((int) "route".length()));
			final Route e = np.getRouteFromId(id);
			vs.pickElement(e);
			callback.updateVisualizationAfterPick();
		} else if (internalLink.startsWith("node"))
		{
			final long id = Long.parseLong(internalLink.substring((int) "node".length()));
			final Node e = np.getNodeFromId(id);
			vs.pickElement(e);
			callback.updateVisualizationAfterPick();
		} else if (internalLink.startsWith("multicastDemand"))
		{
			final long id = Long.parseLong(internalLink.substring((int) "multicastDemand".length()));
			final MulticastDemand e = np.getMulticastDemandFromId(id);
			vs.pickElement(e);
			callback.updateVisualizationAfterPick();
		} else if (internalLink.startsWith("multicastTree"))
		{
			final long id = Long.parseLong(internalLink.substring((int) "multicastTree".length()));
			final MulticastDemand e = np.getMulticastDemandFromId(id);
			vs.pickElement(e);
			callback.updateVisualizationAfterPick();
		} else if (internalLink.startsWith("link"))
		{
			final long id = Long.parseLong(internalLink.substring((int) "link".length()));
			final Link e = np.getLinkFromId(id);
			vs.pickElement(e);
			callback.updateVisualizationAfterPick();
		} else if (internalLink.startsWith("forwardingRule"))
		{
			final long demandId = Long.parseLong(internalLink.substring((int) "forwardingRule".length() , internalLink.indexOf(",")));
			final long linkId = Long.parseLong(internalLink.substring(internalLink.indexOf(",") + 1));
			final Pair<Demand,Link> e = Pair.of(np.getDemandFromId(demandId) , np.getLinkFromId(linkId));
			vs.pickForwardingRule(e);
			callback.updateVisualizationAfterPick();
		} else if (internalLink.startsWith("resource"))
		{
			final long id = Long.parseLong(internalLink.substring((int) "resource".length()));
			final Resource e = np.getResourceFromId(id);
			vs.pickElement(e);
			callback.updateVisualizationAfterPick();
		} else if (internalLink.startsWith("srg"))
		{
			final long id = Long.parseLong(internalLink.substring((int) "srg".length()));
			final SharedRiskGroup e = np.getSRGFromId(id);
			vs.pickElement(e);
			callback.updateVisualizationAfterPick();
		} else if (internalLink.startsWith("layer"))
		{
			final long id = Long.parseLong(internalLink.substring((int) "layer".length()));
			final NetworkLayer e = np.getNetworkLayerFromId(id);
			np.setNetworkLayerDefault(e);
			callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.LAYER));
			vs.pickElement(e);
			callback.updateVisualizationAfterPick();
            callback.addNetPlanChange();
		} else throw new RuntimeException ();
	}

}
