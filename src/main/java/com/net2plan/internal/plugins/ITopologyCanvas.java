/*******************************************************************************
 * Copyright (c) 2015 Pablo Pavon Mariño.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * Contributors:
 *     Pablo Pavon Mariño - initial API and implementation
 ******************************************************************************/










package com.net2plan.internal.plugins;

import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.AffineTransformOp;
import java.util.Map;
import java.util.Set;

import javax.swing.JComponent;

import com.net2plan.gui.utils.topologyPane.GUILink;
import com.net2plan.gui.utils.topologyPane.GUINode;
import com.net2plan.gui.utils.topologyPane.ITopologyCanvasPlugin;
import com.net2plan.interfaces.networkDesign.Node;
import edu.uci.ics.jung.visualization.Layer;
import edu.uci.ics.jung.visualization.transform.AffineTransformer;
import edu.uci.ics.jung.visualization.transform.MutableTransformer;

/**
 * Base class for topology canvas.
 */
public interface ITopologyCanvas extends Plugin
{
	Map<String, String> getCurrentOptions();

	/**
	 * Adds a new plugin to the canvas.
	 *
	 * @param plugin Plugin
	 * @since 0.3.0
	 */
	void addPlugin(ITopologyCanvasPlugin plugin);

	/**
	 * Removes a plugin from the canvas.
	 *
	 * @param plugin Plugin
	 * @since 0.3.0
	 */
	void removePlugin(ITopologyCanvasPlugin plugin);

	Point2D getNetPlanCoordinatesFromScreenPixelCoordinate(Point2D screenPoint, Layer layer);

	/**
	 * Returns a reference to the internal component containing the canvas.
	 *
	 * @return Internal component containing the canvas
	 * @since 0.3.0
	 */
	JComponent getCanvasComponent();

	/**
	 * Returns the identifier of a link associated to a mouse event, or -1 otherwise.
	 *
	 * @param e Mouse event
	 * @return Link identifier, or -1 if no link was clicked
	 * @since 0.3.1
	 */
	GUILink getEdge(MouseEvent e);

	/**
	 * Returns the identifier of a link associated to a mouse event, or -1 otherwise.
	 *
	 * @param e Mouse event
	 * @return Link identifier, or -1 if no link was clicked
	 * @since 0.3.1
	 */
	GUINode getVertex(MouseEvent e);

	/**
	 * Pans the graph to the .
	 *
	 * @param initialPoint Initial point where the mouse was pressed
	 * @param currentPoint Current point where the mouse is
	 * @since 0.3.1
	 */
	void panTo(Point2D initialPoint, Point2D currentPoint);

	void moveCanvasTo(Point2D destinationPoint);

	/**
	 * Refreshes the canvas.
	 *
	 * @since 0.3.0
	 */
	void refresh();

	/**
	 * Updates the position of a GUI Node based on its associted node.
	 * @param node
	 */
	void updateVertexXYPosition(GUINode node);

	/**
	 * Moves a GUI node to the desired point.
	 * This method does not change the node's xy coordinates.
	 * Have in mind that by using this method, the xy coordinates from the table do not equal the coordinates from the topology.
	 *
	 * @param npNode Node to move.
	 * @param point  Point to which the node will be moved.
	 */
	void moveVertexToXYPosition(GUINode npNode, Point2D point);

	/**
	 * Resets the emphasized elements.
	 *
	 * @since 0.3.0
	 */
	void resetPickedStateAndRefresh();

	/**
	 * Takes a snapshot of the canvas.
	 *
	 * @since 0.3.0
	 */
	void takeSnapshot();

	/**
	 * Refresh the canvas with the physical topology from the given network design.
	 *
	 * @since 0.3.0
	 */
	void rebuildTopologyAndRefresh();

	/**
	 * Makes zoom-all from the center of the view.
	 *
	 * @since 0.3.0
	 */
	void zoomAll();

	/**
	 * Makes zoom-in from the center of the view.
	 *
	 * @since 0.3.0
	 */
	void zoomIn();

	/**
	 * Makes zoom-out from the center of the view.
	 *
	 * @since 0.3.0
	 */
	void zoomOut();

	void zoom(Point2D centerPoint, float scale);

	double getCurrentCanvasScale();

	Point2D getCanvasCenter();

 	Set<GUINode> getGraphVertices();

	Set<GUILink> getGraphEdges();
}
