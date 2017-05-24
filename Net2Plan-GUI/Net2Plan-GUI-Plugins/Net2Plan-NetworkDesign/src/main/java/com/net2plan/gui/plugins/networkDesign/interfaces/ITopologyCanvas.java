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










package com.net2plan.gui.plugins.networkDesign.interfaces;

import com.net2plan.gui.plugins.networkDesign.topologyPane.jung.GUILink;
import com.net2plan.gui.plugins.networkDesign.topologyPane.jung.GUINode;
import com.net2plan.gui.plugins.networkDesign.topologyPane.jung.state.CanvasOption;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.internal.plugins.Plugin;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.Map;
import java.util.Set;

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

	double getCurrentCanvasScale();

	/**
	 * Gets the untransformed center of the canvas.
	 * Have in mind that this point does not belong to any of the other coordinate systems and must be converted to the desired one.
	 * Alas, it can be used for tasks such as zoom as it does not change with movement.
	 * @return Center of the canvas
	 */
	Point2D getCanvasCenter();

	/**
	 * Converts a point from the Net2Plan coordinate system to the JUNG coordinate system
	 */
	Point2D getCanvasPointFromNetPlanPoint(Point2D screenPoint);

	/**
	 * Converts a point from the SWING coordinate system to the JUNG coordinate system
	 */
	Point2D getCanvasPointFromScreenPoint(Point2D netPlanPoint);

	/**
	 * Converts a point from the JUNG coordinate system to a translation difference.
	 */
	Point2D getCanvasPointFromMovement(Point2D point);

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

	void panTo(Point2D initialPoint, Point2D destinationPoint);

	void addNode(Point2D position);

	void removeNode(Node node);

	void setState(CanvasOption state, Object... stateParams);

	CanvasOption getState();

	void returnToPreviousState();

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
	void cleanSelection();

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
	void rebuildGraph();

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
