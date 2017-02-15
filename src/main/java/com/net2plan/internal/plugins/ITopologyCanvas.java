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

import java.awt.*;
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
	public enum CanvasOperation { ZOOM_ALL, ZOOM_IN, ZOOM_OUT};

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

	double getCurrentCanvasScale();

	Point2D getCanvasCenter();

	Point2D getCanvasPointFromNetPlanPoint(Point2D screenPoint);

	Point2D getCanvasPointFromScreenPoint(Point2D netPlanPoint);

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

	Set<GUINode> getAllVertices();

	Set<GUILink> getAllEdges();

	Point2D getCanvasPointFromMovement(Point2D point);

	void panTo(Point2D initialPoint, Point2D destinationPoint);

	void addNode(Point2D position);

	void removeNode(Node node);

	void runOSMSupport();

	void stopOSMSupport();

	boolean isOSMRunning();

	void moveCanvasTo(Point2D destinationPoint);

	void updateAllVerticesXYPosition();

	/**
	 * Moves a GUI node to the desired point.
	 * Th#is method does not change the node's xy coordinates.
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
	 * Refreshes the canvas.
	 *
	 * @since 0.3.0
	 */
	void refresh();

	/**
	 * Refresh the canvas with the physical topology from the given network design.
	 *
	 * @since 0.3.0
	 */
	void rebuildCanvasGraphAndRefresh();

	/**
	 * Takes a snapshot of the canvas.
	 *
	 * @since 0.3.0
	 */
	void takeSnapshot();

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
	
	void updateInterLayerDistanceInNpCoordinates  (int interLayerDistanceInPixels);

	double getInterLayerDistanceInNpCoordinates();
	
}
