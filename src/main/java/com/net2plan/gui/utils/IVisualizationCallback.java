/*******************************************************************************
 * Copyright (c) 2015 Pablo Pavon Mariño.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser License v2.1
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
import java.util.List;
import java.util.Set;

import org.apache.commons.collections15.BidiMap;

import com.net2plan.gui.utils.topologyPane.VisualizationState;
import com.net2plan.interfaces.networkDesign.Demand;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.MulticastDemand;
import com.net2plan.interfaces.networkDesign.MulticastTree;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkElement;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.interfaces.networkDesign.Resource;
import com.net2plan.interfaces.networkDesign.Route;
import com.net2plan.interfaces.networkDesign.SharedRiskGroup;
import com.net2plan.internal.Constants.NetworkElementType;
import com.net2plan.utils.Pair;

/**
 * Interface to be implemented by any class dealing with network designs.
 */
public interface IVisualizationCallback
{
	VisualizationState getVisualizationState ();
	
	void resetPickedStateAndUpdateView ();

//    void pickNodeAndUpdateView (Node node);
//
//    void pickMulticastDemandAndUpdateView(MulticastDemand demand);
//    
//    void pickForwardingRuleAndUpdateView(Pair<Demand, Link> demandLink);
//    
//    void pickLinkAndUpdateView (Link link);
//
//    void pickDemandAndUpdateView (Demand demand);
//
//    void pickRouteAndUpdateView (Route route);
//
//    void pickMulticastTreeAndUpdateView (MulticastTree tree);
//
//    void pickSRGAndUpdateView (NetworkLayer layer , SharedRiskGroup srg);
//
//    void pickLayerAndUpdateView (NetworkLayer layer);

    void putTransientColorInElementTopologyCanvas (Collection<? extends NetworkElement> linksAndNodes , Color color);

	void updateVisualizationJustTables ();

	void updateVisualizationJustCanvasLinkNodeVisibilityOrColor ();

	void updateVisualizationAfterNewTopology ();

	NetPlan getDesign();

    NetPlan getInitialDesign();

	void updateVisualizationAfterChanges (Set<NetworkElementType> modificationsMade);

//    void moveNodeXYPosition (Node node, Point2D pos , boolean updateView);

    boolean inOnlineSimulationMode();

	void loadDesignDoNotUpdateVisualization(NetPlan netPlan);

	void updateVisualizationAfterPick();
}
