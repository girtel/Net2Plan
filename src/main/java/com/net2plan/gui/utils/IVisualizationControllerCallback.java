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

    public void pickNodeAndUpdateView (Node node);

    public void pickLinkAndUpdateView (Link link);

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

    public NetPlan getDesign();

    public NetPlan getInitialDesign();

//    public void moveNodeXYPosition (Node node, Point2D pos , boolean updateView);

    public boolean inOnlineSimulationMode();

	public void loadDesignDoNotUpdateVisualization(NetPlan netPlan);

}
