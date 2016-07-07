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

import java.awt.Color;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.net2plan.gui.utils.FileChooserConfirmOverwrite;
import com.net2plan.gui.utils.topology.ITopologyCanvasPlugin;
import com.net2plan.interfaces.networkDesign.Configuration;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.internal.CommandLineParser;
import com.net2plan.utils.CollectionUtils;
import com.net2plan.utils.ImageUtils;
import com.net2plan.utils.Pair;
import com.net2plan.utils.Triple;

/**
 * Base class for topology canvas.
 * 
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.3
 */
public abstract class ITopologyCanvas implements Plugin
{
	private JFileChooser fc;
	
	@Override
	public final Map<String, String> getCurrentOptions()
	{
		return CommandLineParser.getParameters(getParameters(), Configuration.getOptions());
	}
	
	@Override
	public int getPriority()
	{
		return 0;
	}
	
	/**
	 * Adds a new unidirectional link between two nodes.
	 * 
	 * @param linkId Link identifier
	 * @param originNodeId Origin node identifier
	 * @param destinationNodeId Destinatin node identifier
	 * @since 0.3.0
	 */
	public abstract void addLink(Link npLink);

	/**
	 * Adds a new node.
	 * 
	 * @param nodeId Node identifier
	 * @param pos Node position
	 * @param label Node label (it can be null)
	 * @since 0.3.0
	 */
	public abstract void addNode(Node npNode);

	/**
	 * Adds a new plugin to the canvas.
	 * 
	 * @param plugin Plugin
	 * @since 0.3.0
	 */
	public abstract void addPlugin(ITopologyCanvasPlugin plugin);

	/**
	 * Returns the real coordinates in the topology for a given screen point.
	 * 
	 * @param screenPoint Screen location
	 * @return Coordinates in the topology system for the screen point
	 * @since 0.3.0
	 */
	public abstract Point2D convertViewCoordinatesToRealCoordinates(Point screenPoint);
	
	/**
	 * Decrease the font size.
	 * 
	 * @since 0.3.0
	 */
	public abstract void decreaseFontSize();
	
	/**
	 * Decrease the node size.
	 * 
	 * @since 0.3.0
	 */
	public abstract void decreaseNodeSize();
	
	/**
	 * Returns the top-level component of the canvas.
	 * 
	 * @return Top-level component of the canvas
	 * @since 0.3.0
	 */
	public abstract JComponent getComponent();
	
	/**
	 * Returns a reference to the internal component containing the canvas.
	 *
	 * @return Internal component containing the canvas
	 * @since 0.3.0
	 */
	public abstract JComponent getInternalComponent();
	
	/**
	 * Returns the identifier of a link associated to a mouse event, or -1 otherwise.
	 *
	 * @param e Mouse event
	 * @return Link identifier, or -1 if no link was clicked
	 * @since 0.3.1
	 */
	public abstract long getLink(MouseEvent e);
	
	/**
	 * Returns the identifier of a link associated to a mouse event, or -1 otherwise.
	 *
	 * @param e Mouse event
	 * @return Link identifier, or -1 if no link was clicked
	 * @since 0.3.1
	 */
	public abstract long getNode(MouseEvent e);
	
	/**
	 * Increase the font size.
	 * 
	 * @since 0.3.0
	 */
	public abstract void increaseFontSize();
	
	/**
	 * Increase the node size.
	 * 
	 * @since 0.3.0
	 */
	public abstract void increaseNodeSize();
	
	/**
	 * Indicates whether a link is visible or not.
	 * 
	 * @param linkId Link identifier
	 * @return {@code true} if the link is visible. Otherwise, {@code false}
	 * @since 0.3.0
	 */
	public abstract boolean isLinkVisible(Link associatedNpLink);

	/**
	 * Indicates whether a node is visible or not.
	 * 
	 * @param nodeId Node identifier
	 * @return {@code true} if the node is visible. Otherwise, {@code false}
	 * @since 0.3.0
	 */
	public abstract boolean isNodeVisible(Node associatedNpNode);
	
	/**
	 * Pans the graph to the .
	 *
	 * @param initialPoint Initial point where the mouse was pressed
	 * @param currentPoint Current point where the mouse is
	 * @since 0.3.1
	 */
	public abstract void panTo(Point initialPoint, Point currentPoint);

	/**
	 * Refreshes the canvas.
	 * 
	 * @since 0.3.0
	 */
	public abstract void refresh();

	
	/**
	 * Forces the canvas to update the position of a node
	 * 
	 * @param linkId Link identifier
	 * @since 0.3.0
	 */
	public abstract void updateNodeXYPosition(Node npNode);

	/**
	 * Removes a link from the canvas.
	 * 
	 * @param linkId Link identifier
	 * @since 0.3.0
	 */
	public abstract void removeLink(Link associatedNpLink);

	/**
	 * Removes a node from the canvas, and all its associated incoming/outgoing links.
	 * 
	 * @param nodeId Node identifier
	 * @since 0.3.0
	 */
	public abstract void removeNode(Node associatedNpNode);

	/**
	 * Removes a plugin from the canvas.
	 * 
	 * @param plugin Plugin
	 * @since 0.3.0
	 */
	public abstract void removePlugin(ITopologyCanvasPlugin plugin);

	/**
	 * Resets the graph.
	 * 
	 * @since 0.3.0
	 */
	public abstract void reset();

	/**
	 * Resets the emphasized elements.
	 * 
	 * @since 0.3.0
	 */
	public abstract void resetPickedAndUserDefinedColorState();

	/**
	 * Sets all links are visible/hidden.
	 * 
	 * @param visible Indicates whether all links are visible ({@code true}) or hidden ({@code false})
	 * @since 0.3.0
	 */
	public abstract void setAllLinksVisible(boolean visible);
	
	/**
	 * Sets all nodes are visible/hidden.
	 * 
	 * @param visible Indicates whether all nodes are visible ({@code true}) or hidden ({@code false})
	 * @since 0.3.0
	 */
	public abstract void setAllNodesVisible(boolean visible);
	
	/**
	 * Sets a link as visible/hidden.
	 * 
	 * @param linkId Link identifier
	 * @param visible Indicates whether the link is visible ({@code true}) or hidden ({@code false})
	 * @since 0.3.0
	 */
	public abstract void setLinkVisible(Link link, boolean visible);

	/**
	 * Sets some links as visible/hidden.
	 * 
	 * @param linkIds Link identifiers
	 * @param visible Indicates whether the links are visible ({@code true}) or hidden ({@code false})
	 * @since 0.3.0
	 */
	public abstract void setLinksVisible(Collection<Link> link , boolean visible);
	
	/**
	 * Sets a node as visible/hidden.
	 * 
	 * @param nodeId Node identifier
	 * @param visible Indicates whether the node is visible ({@code true}) or hidden ({@code false})
	 * @since 0.3.0
	 */
	public abstract void setNodeVisible(Node node, boolean visible);
	
	/**
	 * Sets some nodes as visible/hidden.
	 * 
	 * @param nodeIds Node identifiers
	 * @param visible Indicates whether the nodes are visible ({@code true}) or hidden ({@code false})
	 * @since 0.3.0
	 */
	public abstract void setNodesVisible(Collection<Node> nodeIds, boolean visible);
	
	/**
	 * Indicates whether or not non-connected nodes should be shown.
	 * 
	 * @param show Indicates whether or not non-connected nodes should be shown
	 * @since 0.3.0
	 */
	public abstract void showNonConnectedNodes(boolean show);

	/**
	 * Emphasizes a link.
	 * 
	 * @param linkId Link identifier
	 * @since 0.3.0
	 */
	public final void showLink(Link link , Color color , boolean dashed)
	{
		final Map<Link,Pair<Color,Boolean>> map = new HashMap<Link,Pair<Color,Boolean>> (); map.put (link , Pair.of(color,dashed));
		showAndPickNodesAndLinks(null, map);
	}

	/**
	 * Indicates whether or not link identifiers should be shown.
	 * 
	 * @param show Indicates whether or not link identifiers should be shown
	 * @since 0.3.0
	 */
	public abstract void showLinkLabels(boolean show);
	
	/**
	 * Emphasizes a node.
	 * 
	 * @param nodeId Node identifier
	 * @since 0.3.0
	 */
	public final void showNode(Node node , Color color)
	{
		final Map<Node,Color> map = new HashMap<Node,Color> (); map.put (node , color);
		showAndPickNodesAndLinks(map, null);
	}

	/**
	 * Indicates whether or not node names should be shown.
	 * 
	 * @param show Indicates whether or not node names should be shown
	 * @since 0.3.0
	 */
	public abstract void showNodeNames(boolean show);

	/**
	 * Emphasizes a set of nodes.
	 * 
	 * @param nodeIds Node identifiers
	 * @since 0.3.0
	 */
	public final void showNodes(Map<Node,Color> nodes)
	{
		showAndPickNodesAndLinks(nodes, null);
	}

//	/**
//	 * Emphasizes a set of nodes and/or links.
//	 * 
//	 * @param nodeIds Node identifiers (may be null)
//	 * @param linkIds Link identifiers (may be null)
//	 * @since 0.3.0
//	 */
//	public abstract void showNodesAndLinks(Collection<Pair<Node,Color>> nodes , Collection<Pair<Link,Color>> links);
//
//	/**
//	 * Emphasizes a set of links.
//	 * 
//	 * @param linkIds Links identifiers
//	 * @since 0.3.0
//	 */
//	public final void showRoute(Collection<Pair<Link,Color>> links)
//	{
//		showRoutes(links, null);
//	}

	/**
	 * Emphasizes two sets of links. The first one will show a heavy line, while
	 * the second one will show a heavy dashed line.
	 * 
	 * @param primaryRouteLinks Link identifiers for the first set of links
	 * @param secondaryRouteLinks Link identifiers for the second set of links
	 * @since 0.3.0
	 */
	public abstract void showAndPickNodesAndLinks(Map<Node,Color> npNodes, Map<Link,Pair<Color,Boolean>> npLinks);

	/**
	 * Takes a snapshot of the canvas.
	 * 
	 * @since 0.3.0
	 */
	public final void takeSnapshot()
	{
		if (fc == null)
		{
			fc = new FileChooserConfirmOverwrite();
			FileNameExtensionFilter pngFilter = new FileNameExtensionFilter("PNG files", "png");
			fc.setFileFilter(pngFilter);
		}

		takeSnapshot_preConfigure();
		JComponent component = getInternalComponent();
		BufferedImage bi = ImageUtils.trim(ImageUtils.takeSnapshot(component));
		takeSnapshot_postConfigure();
		
		int s = fc.showSaveDialog(null);
		if (s == JFileChooser.APPROVE_OPTION)
		{
			File f = fc.getSelectedFile();
			ImageUtils.writeImageToFile(f, bi, ImageUtils.ImageType.PNG);
		}
	}
	
	/**
	 * Configures the canvas before taking an snapshot (i.e. hiding elements).
	 * 
	 * @since 0.3.0
	 */
	public void takeSnapshot_preConfigure()
	{
	}

	/**
	 * Restores the canvas to its original state after taking an snapshot.
	 * 
	 * @since 0.3.0
	 */
	public void takeSnapshot_postConfigure()
	{
	}
	
	/**
	 * Refresh the canvas with the physical topology from the given network design.
	 * 
	 * @param netPlan Network design
	 * @since 0.3.0
	 */
	public final void updateTopology(NetPlan netPlan)
	{
		long layer = netPlan.getNetworkLayerDefault().getId ();
		updateTopology(netPlan, layer);
	}

	/**
	 * Refresh the canvas with the physical topology from the given network 
	 * design in the given layer.
	 * 
	 * @param netPlan Network design
	 * @param layerId Layer identifier
	 * @since 0.3.0
	 */
	public abstract void updateTopology(NetPlan netPlan, long layer);
//	{
//		Map<Long, Point2D> nodeXYPositionMap = new HashMap<Long, Point2D> (); //nodeXYPositionMap (); //netPlan.getNodeXYPositionMap();
//		Map<Long, String> nodeNameMap = new HashMap<Long, String> (); //netPlan.getNodeNameMap();
//		for (Node node : netPlan.getNodes ()) { nodeXYPositionMap.put (node.getId () , node.getXYPositionMap()); nodeNameMap.put (node.getId () , node.getName ()); }
//		Map<Long, Pair<Long, Long>> linkMap = netPlan.getLinkIdMap(netPlan.getLinks (netPlan.getNetworkLayerFromId (layer)));
//		
//		updateTopology(nodeXYPositionMap, nodeNameMap, linkMap);
//	}
	
	/**
	 * Refresh the canvas with the given physical topology.
	 * 
	 * @param nodeXYPositionMap Map of node XY position
	 * @param nodeNameMap Map of node names
	 * @param linkMap Map of links
	 * @since 0.3.0
	 */
//	public abstract void updateTopology(Map<Long, Point2D> nodeXYPositionMap, Map<Long, String> nodeNameMap, Map<Long, Pair<Long, Long>> linkMap);

	/**
	 * Makes zoom-all from the center of the view.
	 * 
	 * @since 0.3.0
	 */
	public abstract void zoomAll();

	/**
	 * Makes zoom-in from the center of the view.
	 * 
	 * @since 0.3.0
	 */
	public abstract void zoomIn();

	/**
	 * Makes zoom-out from the center of the view.
	 * 
	 * @since 0.3.0
	 */
	public abstract void zoomOut();
}
