/*******************************************************************************
 * Copyright (c) 2015 Pablo Pavon Mariño.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Contributors:
 * Pablo Pavon Mariño - initial API and implementation
 ******************************************************************************/


package com.net2plan.gui.utils;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.Set;

import com.net2plan.gui.utils.topologyPane.VisualizationState;
import com.net2plan.interfaces.networkDesign.Demand;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.MulticastDemand;
import com.net2plan.interfaces.networkDesign.MulticastTree;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkElement;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.interfaces.networkDesign.Route;
import com.net2plan.interfaces.networkDesign.SharedRiskGroup;
import com.net2plan.internal.Constants.NetworkElementType;
import com.net2plan.utils.Pair;

/**
 * Interface to be implemented by any class dealing with network designs.
 */
public interface IVisualizationControllerCallback
{
	public VisualizationState getVisualizationState ();
	
	public void updateVisualizationAfterChanges (Set<NetworkElementType> changes);

	public void updateVisualizationAfterLinkNodeColorChanges ();

	public void updateVisualizationJustTables ();

	public void resetPickedStateAndUpdateView ();

    public void pickLinkAndUpdateView (Link link);

    public void pickNodeAndUpdateView (Node node);

    public void pickDemandAndUpdateView (Demand demand);

    public void pickMulticastDemandAndUpdateView (MulticastDemand demand);

    public void pickForwardingRuleAndUpdateView (Pair<Demand, Link> demandLink);

    public void pickRouteAndUpdateView (Route route);

    public void pickMulticastTreeAndUpdateView (MulticastTree tree);

    public void pickSRGAndUpdateView (NetworkLayer layer , SharedRiskGroup srg);

    public void putColorInElementTopologyCanvas (Collection<? extends NetworkElement> linksAndNodes , Color color);

	public void updateVisualizationAfterNewTopology ();

	public void justApplyZoomAll ();

	public void updateVisualizationJustTopologyCanvas ();

	
//    /**
//     * Adds a new link.
//     *
//     * @param layer           Layer identifier
//     * @param originNode      Origin node identifier
//     * @param destinationNode Destination node identifier
//     * @return Link identifier
//     */
//    public Link addLink (NetworkLayer layer , Node originNode, Node destinationNode , boolean updateView);
//
//    /**
//     * Adds a new bidirectional link (one on each direction).
//     *
//     * @param layer           Layer identifier
//     * @param originNode      Origin node identifier
//     * @param destinationNode Destination node identifier
//     * @return Link identifiers
//     */
//    public Pair<Link, Link> addLinkBidirectional (NetworkLayer layer , Node originNode, Node destinationNode , boolean updateView);
//    
//    /**
//     * Adds a new bidirectional link (one on each direction).
//     *
//     * @param layer           Layer identifier
//     * @param originNode      Origin node identifier
//     * @param destinationNode Destination node identifier
//     * @return Link identifiers
//     */
//    public void applyTopologyRearrangementAndUpdateView (ITopologyDistribution distribution);
//
//    
//    public void setNodeVisibilityStateAndUpdateView (Node node, boolean setAsVisible);
//    
//    public void setNodeNameAndUpdateView (Node node, String name);
//
//    public void setNodeFailureState (Node node, boolean isUp , boolean updateView);
//
//    public void setLinkFailureState (Link link, boolean isUp , boolean updateView);
/**
//     * Adds a node at the given coordinates.
//     *
//     * @param pos 2D position
//     */
//    public Node addNode (Point2D pos , boolean updateView);

    /**
     * Returns the current network design.
     *
     * @return Current {@code NetPlan}
     */
    public NetPlan getDesign();

    /**
     * Returns the current network plan.
     *
     * @return First item is the network plan, and the second one is the active layer
     */
    public NetPlan getInitialDesign();

//    /**
//     * Allows to execute some action whenever a layer is selected in the GUI.
//     *
//     * @param layer Layer identifier
//     * @since 0.3.1
//     */
//    public void layerChanged(long layer);

//    /**
//     * It is called when a new network design is loaded.
//     *
//     * @param netPlan Network design
//     * @since 0.3.1
//     */
//    public void loadDesignAndUpdateView (File npFile);
//
//    /**
//     * Loads a set of traffic demands from the given {@code NetPlan}.
//     *
//     * @param netPlan Network design containing a demand set
//     * @since 0.3.1
//     */
//    public void loadTrafficDemandsAndUpdateView (File npFile);

    /**
     * Moves the node to the given position.
     *
     * @param node Node identifier
     * @param pos  2D position
     * @since 0.3.1
     */
    public void moveNodeXYPosition (Node node, Point2D pos , boolean updateView);

//    /**
//     * Removes the given link.
//     *
//     * @param link Link identifier
//     * @since 0.3.1
//     */
//    public void removeNetworkElementAndUpdateView (NetworkElement e);


//    /**
//     * Resets the current topology (i.e. remove any node/link).
//     *
//     * @since 0.3.1
//     */
//    public void resetNetPlanAndUpdateView ();
//

//    /**
//     * Updates the {@code NetPlan} view (i.e. node info, link info, and so on).
//     *
//     * @since 0.2.3
//     */
//    public void updateWarningsAndTables();
//
//    /**
//     * Updates the {@code NetPlan} warnings (over-subscribed links, blocked demands, and so on).
//     *
//     * @since 0.3.0
//     */
//    public void updateWarnings();
//
    // added by Pablo
//    public boolean allowDocumentUpdate ();
    // added by Pablo
//    public TopologyPanel getTopologyPanel();

    // added by Pablo
    public boolean inOnlineSimulationMode();

    // added by Pablo
    //public boolean allowLoadTrafficDemands();

	public void loadDesignDoNotUpdateVisualization(NetPlan netPlan);


//	void setDemandOfferedTraffic(Demand d, double traffic , boolean updateView);
//
//
//	void removeAllNetworkElementsOfaTypeAndUpdateView (NetworkElementType type, NetworkLayer layer);
//
//
//	Demand addDemand(NetworkLayer layer, Node originNode, Node destinationNode , boolean updateView);
//
//	Pair<Demand,Demand> addDemandBidirectional (NetworkLayer layer, Node originNode, Node destinationNode , boolean updateView);
	
	


    // added by Jorge
//    public Map<Constants.NetworkElementType, AdvancedJTableNetworkElement> getTables();

}
