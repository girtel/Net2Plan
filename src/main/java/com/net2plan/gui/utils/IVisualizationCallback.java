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
import java.util.Set;

import com.net2plan.gui.utils.topologyPane.GUINode;
import com.net2plan.internal.plugins.ITopologyCanvas;

import com.net2plan.gui.utils.topologyPane.visualizationControl.VisualizationState;
import com.net2plan.gui.utils.whatIfAnalysisPane.WhatIfAnalysisPane;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkElement;
import com.net2plan.internal.Constants.NetworkElementType;

/**
 * Interface to be implemented by any class dealing with network designs.
 */
public interface IVisualizationCallback
{
	VisualizationState getVisualizationState ();
	
	void resetPickedStateAndUpdateView ();

    void putTransientColorInElementTopologyCanvas (Collection<? extends NetworkElement> linksAndNodes , Color color);

	void updateVisualizationJustTables ();

	void updateVisualizationJustCanvasLinkNodeVisibilityOrColor ();

	void updateVisualizationAfterNewTopology ();

	NetPlan getDesign();

    NetPlan getInitialDesign();

	void updateVisualizationAfterChanges (Set<NetworkElementType> modificationsMade);

    boolean inOnlineSimulationMode();

	void setCurrentNetPlanDoNotUpdateVisualization(NetPlan netPlan);

	void updateVisualizationAfterPick();

	void moveNodeTo(GUINode guiNode, Point2D toPoint);

	void runCanvasOperation(ITopologyCanvas.CanvasOperation... canvasOperation);

	UndoRedoManager getUndoRedoNavigationManager();

	void requestUndoAction();

	void requestRedoAction();

	WhatIfAnalysisPane getWhatIfAnalysisPane();
}
