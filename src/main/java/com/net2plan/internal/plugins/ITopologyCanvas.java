/*******************************************************************************
 * Copyright (c) 2015 Pablo Pavon Mariño.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * Contributors:
 *     Pablo Pavon Mariño - initial API and implementation
 ******************************************************************************/










package com.net2plan.internal.plugins;

import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.Map;

import javax.swing.JComponent;

import com.net2plan.gui.utils.topologyPane.GUILink;
import com.net2plan.gui.utils.topologyPane.GUINode;
import com.net2plan.gui.utils.topologyPane.ITopologyCanvasPlugin;
import com.net2plan.interfaces.networkDesign.Node;
import edu.uci.ics.jung.visualization.Layer;

/**
 * Base class for topology canvas.
 */
public interface ITopologyCanvas extends Plugin
{
	public Map<String, String> getCurrentOptions();

	public void setBackgroundOSMMapsActiveState (boolean activateMap);
	
	public boolean getBackgroundOSMMapsActiveState ();

    public void updateNodeXYPosition(Node node);

	
	/**
	 * Adds a new plugin to the canvas.
	 *
	 * @param plugin Plugin
	 * @since 0.3.0
	 */
	public void addPlugin(ITopologyCanvasPlugin plugin);

//    /**
//     * Returns the set of actions to be added to the popup menu for the network
//     * canvas, where no element (either node or link) is selected.
//     *
//     * @param pos Network coordinates where the popup action was triggered
//     * @return List of actions to be shown for the canvas
//     */
//    public List<JComponent> getCanvasActions(Point2D pos);

	public Point2D getNetPlanCoordinatesFromScreenPixelCoordinate(Point2D screenPoint, Layer layer);

	public Point2D getScreenPixelCoordinateFromNetPlanCoordinate(Point2D screenPoint, Layer layer);

	/**
	 * Returns a reference to the internal component containing the canvas.
	 *
	 * @return Internal component containing the canvas
	 * @since 0.3.0
	 */
	public JComponent getInternalVisualizationController();

	/**
	 * Returns the identifier of a link associated to a mouse event, or -1 otherwise.
	 *
	 * @param e Mouse event
	 * @return Link identifier, or -1 if no link was clicked
	 * @since 0.3.1
	 */
	public GUILink getLink(MouseEvent e);

	/**
	 * Returns the identifier of a link associated to a mouse event, or -1 otherwise.
	 *
	 * @param e Mouse event
	 * @return Link identifier, or -1 if no link was clicked
	 * @since 0.3.1
	 */
	public GUINode getNode(MouseEvent e);

	/**
	 * Pans the graph to the .
	 *
	 * @param initialPoint Initial point where the mouse was pressed
	 * @param currentPoint Current point where the mouse is
	 * @since 0.3.1
	 */
	public void panTo(Point2D initialPoint, Point2D currentPoint);

	/**
	 * Refreshes the canvas.
	 *
	 * @since 0.3.0
	 */
	public void refresh();


	/**
	 * Moves a node to the desired point.
	 * This method does not change the node's xy coordinates.
	 * Have in mind that by using this methos, the xy coordinates from the table do not equal the coordinates from the topology.
	 *
	 * @param npNode Node to move.
	 * @param point  Point to which the node will be moved.
	 */
	public void moveNodeToXYPosition(Node npNode, Point2D point);

	/**
	 * Removes a plugin from the canvas.
	 *
	 * @param plugin Plugin
	 * @since 0.3.0
	 */
	public void removePlugin(ITopologyCanvasPlugin plugin);

	/**
	 * Resets the emphasized elements.
	 *
	 * @since 0.3.0
	 */
	public void resetPickedStateAndRefresh();

	/**
	 * Takes a snapshot of the canvas.
	 *
	 * @since 0.3.0
	 */
	public void takeSnapshot();

	/**
	 * Refresh the canvas with the physical topology from the given network design.
	 *
	 * @param netPlan Network design
	 * @since 0.3.0
	 */
	public void rebuildTopologyAndRefresh();

	/**
	 * Makes zoom-all from the center of the view.
	 *
	 * @since 0.3.0
	 */
	public void zoomAll();

	/**
	 * Makes zoom-in from the center of the view.
	 *
	 * @since 0.3.0
	 */
	public void zoomIn();

	/**
	 * Makes zoom-out from the center of the view.
	 *
	 * @since 0.3.0
	 */
	public void zoomOut();

	public JComponent getComponent();
}
