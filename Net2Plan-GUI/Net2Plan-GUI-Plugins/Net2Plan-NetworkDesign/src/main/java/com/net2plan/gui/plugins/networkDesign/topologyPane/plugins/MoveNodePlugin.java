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


package com.net2plan.gui.plugins.networkDesign.topologyPane.plugins;

import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.gui.plugins.networkDesign.interfaces.ITopologyCanvas;
import com.net2plan.gui.plugins.networkDesign.interfaces.ITopologyCanvasPlugin;
import com.net2plan.gui.plugins.networkDesign.topologyPane.jung.GUINode;
import com.net2plan.gui.plugins.networkDesign.visualizationControl.VisualizationState;
import com.net2plan.interfaces.networkDesign.Node;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.List;

/**
 * Plugin that enables to move nodes.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.3.1
 */
public class MoveNodePlugin extends MouseAdapter implements ITopologyCanvasPlugin
{
    private GUINetworkDesign callback;
    private ITopologyCanvas canvas;
    private GUINode startVertex;
    private int modifiers;

    /**
     * Default constructor. Default modifier is "left button" click.
     *
     * @param callback Topology callback listening plugin events
     * @since 0.3.1
     */
    public MoveNodePlugin(GUINetworkDesign callback , ITopologyCanvas canvas) {
        this(callback, canvas , MouseEvent.BUTTON1_MASK);
    }

    /**
     * Constructor that allows to set the modifiers.
     *
     * @param callback  Topology callback listening plugin events
     * @param modifiers Mouse event modifiers to activate this functionality
     * @since 0.3.1
     */
    public MoveNodePlugin(GUINetworkDesign callback, ITopologyCanvas canvas , int modifiers) {
        setModifiers(modifiers);
        this.callback = callback;
        this.canvas = canvas;

        startVertex = null;
    }

    @Override
    public boolean checkModifiers(MouseEvent e) {
        return e.getModifiers() == getModifiers();
    }

    @Override
    public int getModifiers() {
        return modifiers;
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (startVertex != null) {
            final Point p = e.getPoint();

            this.moveNodeTo(startVertex, p);

            e.consume();
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (checkModifiers(e)) {
            GUINode node = canvas.getVertex(e);
            if (node != null) {
                callback.getVisualizationState().pickElement(node.getAssociatedNode());
                callback.updateVisualizationAfterPick();
                startVertex = node;
                e.consume();
            } else {
                callback.resetPickedStateAndUpdateView();
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        startVertex = null;
    }

    @Override
    public void setModifiers(int modifiers) {
        this.modifiers = modifiers;
    }

    private void moveNodeTo(final GUINode guiNode, final Point2D toPoint)
    {
        final VisualizationState vs = callback.getVisualizationState();
        if (!vs.isNetPlanEditable()) throw new UnsupportedOperationException("NetPlan is not editable");

        final Node node = guiNode.getAssociatedNode();

        final Point2D netPlanPoint = canvas.getCanvasPointFromMovement(toPoint);
        if (netPlanPoint == null) return;

        final Point2D jungPoint = canvas.getCanvasPointFromNetPlanPoint(toPoint);

        node.setXYPositionMap(netPlanPoint);

        callback.updateVisualizationJustTables();

        // Updating GUINodes position having in mind the selected layer.
        final List<GUINode> guiNodes = vs.getCanvasVerticallyStackedGUINodes(node);
        final int selectedLayerVisualizationOrder = vs.getCanvasVisualizationOrderRemovingNonVisible(guiNode.getLayer());

        for (GUINode stackedGUINode : guiNodes)
        {
            final int vlIndex = vs.getCanvasVisualizationOrderRemovingNonVisible(stackedGUINode.getLayer());
            final double interLayerDistanceInNpCoord = canvas.getInterLayerDistanceInNpCoordinates();

            if (vlIndex > selectedLayerVisualizationOrder)
            {
                final int layerDistance = vlIndex - selectedLayerVisualizationOrder;
                canvas.moveVertexToXYPosition(stackedGUINode, new Point2D.Double(jungPoint.getX(), -(jungPoint.getY() + (layerDistance * interLayerDistanceInNpCoord))));
            } else if (vlIndex == selectedLayerVisualizationOrder)
            {
                canvas.moveVertexToXYPosition(stackedGUINode, new Point2D.Double(jungPoint.getX(), -(jungPoint.getY())));
            } else
            {
                final int layerDistance = selectedLayerVisualizationOrder - vlIndex;
                canvas.moveVertexToXYPosition(stackedGUINode, new Point2D.Double(jungPoint.getX(), -(jungPoint.getY() - (layerDistance * interLayerDistanceInNpCoord))));
            }
        }

        canvas.refresh();
    }
}
