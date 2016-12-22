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


package com.net2plan.gui.utils.topologyPane.jung;

import com.net2plan.gui.utils.topologyPane.GUILink;
import com.net2plan.gui.utils.topologyPane.GUINode;
import com.net2plan.gui.utils.topologyPane.ITopologyCanvasPlugin;
import com.net2plan.gui.utils.topologyPane.mapControl.osm.state.OSMMapStateBuilder;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.internal.plugins.ITopologyCanvas;
import com.net2plan.utils.Pair;
import com.net2plan.utils.Triple;
import edu.uci.ics.jung.algorithms.layout.GraphElementAccessor;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.algorithms.layout.StaticLayout;
import edu.uci.ics.jung.graph.DirectedOrderedSparseMultigraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.Context;
import edu.uci.ics.jung.graph.util.DefaultParallelEdgeIndexFunction;
import edu.uci.ics.jung.visualization.Layer;
import edu.uci.ics.jung.visualization.RenderContext;
import edu.uci.ics.jung.visualization.VisualizationServer;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.*;
import edu.uci.ics.jung.visualization.decorators.ConstantDirectionalEdgeValueTransformer;
import edu.uci.ics.jung.visualization.decorators.EdgeShape;
import edu.uci.ics.jung.visualization.renderers.BasicEdgeLabelRenderer;
import edu.uci.ics.jung.visualization.renderers.BasicVertexLabelRenderer;
import edu.uci.ics.jung.visualization.renderers.DefaultEdgeLabelRenderer;
import edu.uci.ics.jung.visualization.renderers.Renderer;
import edu.uci.ics.jung.visualization.transform.BidirectionalTransformer;
import edu.uci.ics.jung.visualization.transform.MutableTransformer;
import edu.uci.ics.jung.visualization.transform.shape.GraphicsDecorator;
import edu.uci.ics.jung.visualization.transform.shape.ShapeTransformer;
import edu.uci.ics.jung.visualization.transform.shape.TransformingGraphics;
import edu.uci.ics.jung.visualization.util.ArrowFactory;
import org.apache.commons.collections15.Predicate;
import org.apache.commons.collections15.Transformer;
import org.apache.commons.collections15.functors.ConstantTransformer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.*;
import java.util.List;
import java.util.Map.Entry;

/**
 * Topology canvas using JUNG library [<a href='#jung'>JUNG</a>].
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @see <a name='jung'></a><a href='http://jung.sourceforge.net/'>Java Universal Network/Graph Framework (JUNG) website</a>
 * @since 0.2.3
 */
@SuppressWarnings("unchecked")
public final class JUNGCanvas extends ITopologyCanvas
{
    private final static Color CANVAS_BGCOLOR = new Color(212, 208, 200);
    private final static float SCALE_IN = 1.1f;
    private final static float SCALE_OUT = 1 / SCALE_IN;
    private final static Transformer<GUINode, Point2D> FLIP_VERTICAL_COORDINATES;

    private final Graph<GUINode, GUILink> g;
    private final Layout<GUINode, GUILink> l;
    private final VisualizationViewer<GUINode, GUILink> vv;
    private final Map<Node, GUINode> nodeTable;
    private final Map<Link, GUILink> linkTable;
    private final PluggableGraphMouse gm;
    private final ScalingControl scalingControl;
    private final Transformer<Context<Graph<GUINode, GUILink>, GUILink>, Shape> originalEdgeShapeTransformer;

    private VisualizationServer.Paintable paintableAssociatedToBackgroundImage;

    private boolean showNodeNames, showLinkIds, showHideNonConnectedNodes;

    static
    {
        FLIP_VERTICAL_COORDINATES = vertex ->
        {
            Point2D pos = vertex.getAssociatedNetPlanNode().getXYPositionMap();
            return new Point2D.Double(pos.getX(), -pos.getY());
        };
    }

    /**
     * Default constructor.
     *
     * @since 0.2.3
     */
    public JUNGCanvas()
    {
        nodeTable = new LinkedHashMap<>();
        linkTable = new LinkedHashMap<>();

        g = new DirectedOrderedSparseMultigraph<>();
        l = new StaticLayout<>(g, FLIP_VERTICAL_COORDINATES);
        vv = new VisualizationViewer<>(l);

        originalEdgeShapeTransformer = new EdgeShape.QuadCurve<>();
        ((EdgeShape.QuadCurve<GUINode, GUILink>) originalEdgeShapeTransformer).setControlOffsetIncrement(10); // how much they separate from the direct line (default is 20)
        ((EdgeShape.QuadCurve<GUINode, GUILink>) originalEdgeShapeTransformer).setEdgeIndexFunction(DefaultParallelEdgeIndexFunction.<GUINode, GUILink>getInstance()); // how much they separate from the direct line (default is 20)

		/* Customize the graph */
        vv.getRenderContext().setVertexDrawPaintTransformer(n -> n.getUserDefinedColorOverridesTheRest() == null ? n.getDrawPaint() : n.getUserDefinedColorOverridesTheRest());
        vv.getRenderContext().setVertexFillPaintTransformer(n -> n.getUserDefinedColorOverridesTheRest() != null ? n.getUserDefinedColorOverridesTheRest() : vv.getPickedVertexState().isPicked(n) ? n.getFillPaintIfPicked() : n.getFillPaint());
        vv.getRenderContext().setVertexFontTransformer(n -> n.getFont());

		
		/* If icons => comment this line */
        vv.getRenderContext().setVertexShapeTransformer(n -> vv.getPickedVertexState().isPicked(n) ? n.getShapeIfPicked() : n.getShape());
        /* If shapes, comment this line */
        //vv.getRenderContext().setVertexIconTransformer(new Transformer<GUINode,Icon> () {} ... )

        vv.getRenderContext().setVertexIncludePredicate(new NodeDisplayPredicate<>());
        vv.getRenderer().setVertexLabelRenderer(new NodeLabelRenderer());
        vv.setVertexToolTipTransformer(node -> node.getToolTip());


        vv.getRenderContext().setEdgeIncludePredicate(context -> context.element.isVisible());
        vv.getRenderContext().setEdgeArrowPredicate(context -> context.element.isVisible() && context.element.getHasArrow());
        vv.getRenderContext().setEdgeArrowStrokeTransformer(i -> vv.getPickedEdgeState().isPicked(i) ? i.getArrowStroke().getSecond() : i.getArrowStroke().getFirst());
        vv.getRenderContext().setEdgeArrowTransformer(new ConstantTransformer(ArrowFactory.getNotchedArrow(7, 10, 5)));
        vv.getRenderContext().setEdgeLabelClosenessTransformer(new ConstantDirectionalEdgeValueTransformer(.6, .6));
        vv.getRenderContext().setEdgeStrokeTransformer(i -> i.getUserDefinedStrokeOverridesTheRest() != null ? i.getUserDefinedStrokeOverridesTheRest() : vv.getPickedEdgeState().isPicked(i) ? i.getEdgeStroke().getSecond() : i.getEdgeStroke().getFirst());

        vv.getRenderContext().setEdgeDrawPaintTransformer(e -> e.getUserDefinedColorOverridesTheRest() != null ? e.getUserDefinedColorOverridesTheRest() : vv.getPickedEdgeState().isPicked(e) ? e.getEdgeDrawPaint().getSecond() : e.getEdgeDrawPaint().getFirst());
        vv.getRenderContext().setArrowDrawPaintTransformer(e -> e.getUserDefinedColorOverridesTheRest() != null ? e.getUserDefinedColorOverridesTheRest() : vv.getPickedEdgeState().isPicked(e) ? e.getArrowDrawPaint().getSecond() : e.getArrowDrawPaint().getFirst());
        vv.getRenderContext().setArrowFillPaintTransformer(e -> e.getUserDefinedColorOverridesTheRest() != null ? e.getUserDefinedColorOverridesTheRest() : vv.getPickedEdgeState().isPicked(e) ? e.getArrowFillPaint().getSecond() : e.getArrowFillPaint().getFirst());

        vv.getRenderContext().setEdgeLabelRenderer(new DefaultEdgeLabelRenderer(Color.BLUE));
        vv.getRenderer().setEdgeLabelRenderer(new BasicEdgeLabelRenderer<GUINode, GUILink>()
        {
            public void labelEdge(RenderContext<GUINode, GUILink> rc, Layout<GUINode, GUILink> layout, GUILink e, String label)
            {
                if (showLinkIds) super.labelEdge(rc, layout, e, e.getLabel());
            }
        });
        vv.setEdgeToolTipTransformer(link -> link.getToolTip());
        vv.getRenderContext().setEdgeShapeTransformer(c ->
                {
                    final GUINode origin = c.element.getOriginNode();
                    final GUINode destination = c.element.getDestinationNode();
                    boolean separateTheLinks = vv.getPickedVertexState().isPicked(origin) || vv.getPickedVertexState().isPicked(destination);
                    if (!separateTheLinks)
                    {
                        Set<GUILink> linksNodePair = new HashSet<>(c.graph.getIncidentEdges(destination));
                        linksNodePair.retainAll(c.graph.getIncidentEdges(origin));
                        for (GUILink e : linksNodePair)
                            if (vv.getPickedEdgeState().isPicked(e) || !e.getAssociatedNetPlanLink().isUp())
                            {
                                separateTheLinks = true;
                                break;
                            }
                    }
                    return separateTheLinks ? originalEdgeShapeTransformer.transform(c) : new Line2D.Float(0.0f, 0.0f, 1.0f, 0.0f);
                }
        );

        // Background controller
        this.paintableAssociatedToBackgroundImage = null;

        showNodeNames(false);
        showNonConnectedNodes(true);
        showLinkLabels(false);

        gm = new PluggableGraphMouse();
        vv.setGraphMouse(gm);

        scalingControl = new LayoutScalingControl();
        ITopologyCanvasPlugin scalingPlugin = new ScalingCanvasPlugin(scalingControl, MouseEvent.NOBUTTON);
        addPlugin(scalingPlugin);

        vv.setOpaque(false);
        vv.setBackground(new Color(0, 0, 0, 0));

        reset();
    }

    @Override
    public void addNode(Node npNode) //long nodeId, Point2D pos, String label)
    {
        if (nodeTable.containsKey(npNode)) throw new RuntimeException("Bad - Node " + npNode + " already exists");
        GUINode node = new GUINode(npNode);
        nodeTable.put(npNode, node);
        g.addVertex(node);
    }

    @Override
    public void addPlugin(ITopologyCanvasPlugin plugin)
    {
        plugin.setCanvas(this);
        gm.add(new GraphMousePluginAdapter(plugin));
    }

    /**
     * Converts a point from the SWING coordinates system into a point from the JUNG coordinates system.
     *
     * @param screenPoint (@code Point2D) on the SWING canvas.
     * @return (@code Point2D) on the JUNG canvas.
     */
    @Override
    public Point2D convertViewCoordinatesToRealCoordinates(Point2D screenPoint)
    {
        Point2D layoutCoordinates = vv.getRenderContext().getMultiLayerTransformer().inverseTransform(Layer.LAYOUT, screenPoint);
        layoutCoordinates.setLocation(layoutCoordinates.getX(), -layoutCoordinates.getY());

        return layoutCoordinates;
    }

    /**
     * Converts a point from the JUNG coordinates system to the SWING coordinates system.
     *
     * @param screenPoint (@code Point2D) on the JUNG canvas.
     * @return (@code Point2D) on the SWING canvas.
     */
    @Override
    public Point2D convertRealCoordinatesToViewCoordinates(Point2D screenPoint)
    {
        screenPoint.setLocation(screenPoint.getX(), -screenPoint.getY());
        return vv.getRenderContext().getMultiLayerTransformer().transform(Layer.LAYOUT, screenPoint);
    }

    @Override
    public void decreaseFontSize()
    {
        boolean changedSize = false;
        for (GUINode n : nodeTable.values()) changedSize |= n.decreaseFontSize();
        if (changedSize) refresh();
    }

    @Override
    public void decreaseNodeSize()
    {
        for (GUINode n : nodeTable.values()) n.setShapeSize(n.getShapeSize() * SCALE_OUT);
        refresh();
    }

    @Override
    public JComponent getComponent()
    {
        return vv;
    }

    @Override
    public String getDescription()
    {
        return null;
    }

    @Override
    public JComponent getInternalComponent()
    {
        return vv;
    }

    @Override
    public long getLink(MouseEvent e)
    {
        final VisualizationViewer<GUINode, GUILink> vv = (VisualizationViewer<GUINode, GUILink>) e.getSource();
        GraphElementAccessor<GUINode, GUILink> pickSupport = vv.getPickSupport();
        if (pickSupport != null)
        {
            final Point p = e.getPoint();
            final GUILink edge = pickSupport.getEdge(vv.getModel().getGraphLayout(), p.getX(), p.getY());
            if (edge != null) return edge.getAssociatedNetPlanLink().getId();
        }

        return -1;
    }

    @Override
    public String getName()
    {
        return "JUNG Canvas";
    }

    @Override
    public long getNode(MouseEvent e)
    {
        final VisualizationViewer<GUINode, GUILink> vv = (VisualizationViewer<GUINode, GUILink>) e.getSource();
        GraphElementAccessor<GUINode, GUILink> pickSupport = vv.getPickSupport();
        if (pickSupport != null)
        {
            final Point p = e.getPoint();
            final GUINode vertex = pickSupport.getVertex(vv.getModel().getGraphLayout(), p.getX(), p.getY());
            if (vertex != null) return vertex.getAssociatedNetPlanNode().getId();
        }

        return -1;
    }

    @Override
    public List<Triple<String, String, String>> getParameters()
    {
        return null;
    }

    @Override
    public void increaseFontSize()
    {
        for (GUINode n : nodeTable.values()) n.increaseFontSize();
        refresh();
    }

    @Override
    public void increaseNodeSize()
    {
        for (GUINode n : nodeTable.values()) n.setShapeSize(n.getShapeSize() * SCALE_IN);
        refresh();
    }

    @Override
    public boolean isLinkVisible(Link npLink)
    {
        GUILink e = linkTable.get(npLink);
        return e == null ? false : e.isVisible();
    }

    @Override
    public boolean isNodeVisible(Node npNode)
    {
        GUINode n = nodeTable.get(npNode);
        return n == null ? false : n.isVisible();
    }

    @Override
    public void panTo(Point2D initialPoint, Point2D currentPoint)
    {
        OSMMapStateBuilder.getSingleton().panTo(initialPoint, currentPoint);
    }

    @Override
    public void refresh()
    {
        vv.repaint();
    }

    @Override
    public void removeLink(Link npLink)
    {
        removeLink(npLink, true);
    }

    @Override
    public void removeNode(Node npNode)
    {
        GUINode node = nodeTable.get(npNode);

        Iterator<GUILink> linkIt;
        Collection<GUILink> outLinks = g.getOutEdges(node);
        if (outLinks == null) outLinks = new LinkedHashSet<>();
        linkIt = outLinks.iterator();
        while (linkIt.hasNext()) removeLink(linkIt.next().getAssociatedNetPlanLink(), false);

        Collection<GUILink> inLinks = g.getInEdges(node);
        if (inLinks == null) inLinks = new LinkedHashSet<>();
        linkIt = inLinks.iterator();
        while (linkIt.hasNext()) removeLink(linkIt.next().getAssociatedNetPlanLink(), false);

        nodeTable.remove(npNode);
        g.removeVertex(node);
    }

    @Override
    public void removePlugin(ITopologyCanvasPlugin plugin)
    {
        if (plugin instanceof GraphMousePlugin) gm.remove((GraphMousePlugin) plugin);
    }

    @Override
    public void reset()
    {
        Iterator<GUILink> linkIt = linkTable.values().iterator();
        while (linkIt.hasNext()) g.removeEdge(linkIt.next());

        Iterator<GUINode> nodeIt = nodeTable.values().iterator();
        while (nodeIt.hasNext()) g.removeVertex(nodeIt.next());

        nodeTable.clear();
        linkTable.clear();

        refresh();
    }

    @Override
    public void resetPickedAndUserDefinedColorState()
    {
        vv.getPickedVertexState().clear();
        vv.getPickedEdgeState().clear();
        for (GUINode n : nodeTable.values()) n.setUserDefinedColorOverridesTheRest(null);
        for (GUILink e : linkTable.values())
        {
            e.setUserDefinedColorOverridesTheRest(null);
            e.setUserDefinedStrokeOverridesTheRest(null);
        }
        refresh();
    }

    @Override
    public void setAllLinksVisible(boolean visible)
    {
        for (GUILink e : linkTable.values()) e.setVisible(visible);
        refresh();
    }

    @Override
    public void setAllNodesVisible(boolean visible)
    {
        for (GUINode n : nodeTable.values()) n.setVisible(visible);
        refresh();
    }

    @Override
    public void setLinkVisible(Link link, boolean visible)
    {
        GUILink e = linkTable.get(link);
        if (e == null) throw new RuntimeException("Bad");
        e.setVisible(visible);
        refresh();
    }

    @Override
    public void setLinksVisible(Collection<Link> links, boolean visible)
    {
        for (Link link : links)
        {
            GUILink e = linkTable.get(link);
            if (e == null) throw new RuntimeException("Bad");
            e.setVisible(visible);
        }

        refresh();
    }

    @Override
    public void setNodeVisible(Node npNode, boolean visible)
    {
        GUINode node = nodeTable.get(npNode);
        if (node == null) throw new RuntimeException("Bad");
        node.setVisible(visible);
        refresh();
    }

    @Override
    public void setNodesVisible(Collection<Node> npNodes, boolean visible)
    {
        for (Node npNode : npNodes)
        {
            GUINode node = nodeTable.get(npNode);
            node.setVisible(visible);
        }

        refresh();
    }

    @Override
    public void showLinkLabels(boolean show)
    {
        if (showLinkIds != show)
        {
            showLinkIds = show;
            refresh();
        }
    }

    @Override
    public void showNodeNames(boolean show)
    {
        if (showNodeNames != show)
        {
            showNodeNames = show;
            refresh();
        }
    }

    @Override
    public void showAndPickNodesAndLinks(Map<Node, Color> npNodes, Map<Link, Pair<Color, Boolean>> npLinks)
    {
        resetPickedAndUserDefinedColorState();

        if (npNodes != null)
        {
            for (Entry<Node, Color> npNode : npNodes.entrySet())
            {
                GUINode aux = nodeTable.get(npNode.getKey());
                aux.setUserDefinedColorOverridesTheRest(npNode.getValue());
                vv.getPickedVertexState().pick(aux, true);
            }
        }

        if (npLinks != null)
        {
            for (Entry<Link, Pair<Color, Boolean>> link : npLinks.entrySet())
            {
                GUILink aux = linkTable.get(link.getKey());
                aux.setUserDefinedColorOverridesTheRest(link.getValue().getFirst());
                vv.getPickedEdgeState().pick(aux, true);
                if (link.getValue().getSecond()) // if true, the edge is dashed
                    aux.setUserDefinedStrokeOverridesTheRest(new BasicStroke(vv.getPickedEdgeState().isPicked(aux) ? 2 : 1, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, new float[]{10}, 0.0f));
                else
                    aux.setUserDefinedStrokeOverridesTheRest(null);
            }
        }
        refresh();
    }

    @Override
    public void showNonConnectedNodes(boolean show)
    {
        if (showHideNonConnectedNodes != show)
        {
            showHideNonConnectedNodes = show;
            refresh();
        }
    }

    @Override
    public void takeSnapshot_preConfigure()
    {
        vv.setBackground(Color.WHITE);
    }

    @Override
    public void takeSnapshot_postConfigure()
    {
        vv.setBackground(CANVAS_BGCOLOR);
    }

    @Override
    public void updateTopology(NetPlan netPlan, long layerId)//Map<Long, Point2D> nodeXYPositionMap, Map<Long, String> nodeNameMap, Map<Long, Pair<Long, Long>> linkMap)
    {
        reset();
        final NetworkLayer layer = netPlan.getNetworkLayerFromId(layerId);
        if (netPlan.getNumberOfNodes() == 0) return;

        for (Node npNode : netPlan.getNodes())
            addNode(npNode);
        for (Link npLink : netPlan.getLinks(layer))
            addLink(npLink);

        refresh();
    }

    @Override
    public void zoomAll()
    {
        OSMMapStateBuilder.getSingleton().zoomAll();
    }

    public void frameTopology()
    {
        Set<GUINode> nodes = new LinkedHashSet<>();
        for (GUINode n : g.getVertices()) if (n.isVisible()) nodes.add(n);

        if (nodes.isEmpty()) return;

        double aux_xmax = Double.NEGATIVE_INFINITY;
        double aux_xmin = Double.POSITIVE_INFINITY;
        double aux_ymax = Double.NEGATIVE_INFINITY;
        double aux_ymin = Double.POSITIVE_INFINITY;
        double auxTransf_xmax = Double.NEGATIVE_INFINITY;
        double auxTransf_xmin = Double.POSITIVE_INFINITY;
        double auxTransf_ymax = Double.NEGATIVE_INFINITY;
        double auxTransf_ymin = Double.POSITIVE_INFINITY;
        for (GUINode node : nodes)
        {
            Point2D aux = node.getAssociatedNetPlanNode().getXYPositionMap();
            Point2D auxTransf = l.transform(node);
            if (aux_xmax < aux.getX()) aux_xmax = aux.getX();
            if (aux_xmin > aux.getX()) aux_xmin = aux.getX();
            if (aux_ymax < aux.getY()) aux_ymax = aux.getY();
            if (aux_ymin > aux.getY()) aux_ymin = aux.getY();
            if (auxTransf_xmax < auxTransf.getX()) auxTransf_xmax = auxTransf.getX();
            if (auxTransf_xmin > auxTransf.getX()) auxTransf_xmin = auxTransf.getX();
            if (auxTransf_ymax < auxTransf.getY()) auxTransf_ymax = auxTransf.getY();
            if (auxTransf_ymin > auxTransf.getY()) auxTransf_ymin = auxTransf.getY();
        }

        double PRECISION_FACTOR = 0.00001;

        Rectangle viewInLayoutUnits = vv.getRenderContext().getMultiLayerTransformer().inverseTransform(vv.getBounds()).getBounds();
        float ratio_h = Math.abs(aux_xmax - aux_xmin) < PRECISION_FACTOR ? 1 : (float) (viewInLayoutUnits.getWidth() / (aux_xmax - aux_xmin));
        float ratio_v = Math.abs(aux_ymax - aux_ymin) < PRECISION_FACTOR ? 1 : (float) (viewInLayoutUnits.getHeight() / (aux_ymax - aux_ymin));
        float ratio = (float) (0.8 * Math.min(ratio_h, ratio_v));
        scalingControl.scale(vv, ratio, vv.getCenter());

		/* Generate an auxiliary node at center of the graph */
        Point2D q = new Point2D.Double((auxTransf_xmin + auxTransf_xmax) / 2, (auxTransf_ymin + auxTransf_ymax) / 2);
        Point2D lvc = vv.getRenderContext().getMultiLayerTransformer().inverseTransform(vv.getCenter());
        double dx = (lvc.getX() - q.getX());
        double dy = (lvc.getY() - q.getY());

        vv.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.LAYOUT).translate(dx, dy);
    }

    @Override
    public void updateNodeXYPosition(Node npNode)
    {
        // Moves a node to its xy coordinates.
        GUINode node = nodeTable.get(npNode);
        l.setLocation(node, FLIP_VERTICAL_COORDINATES.transform(node));
    }

    @Override
    public void moveNodeToXYPosition(Node npNode, Point2D point)
    {
        GUINode node = nodeTable.get(npNode);
        l.setLocation(node, point);
    }

    public MutableTransformer getTransformer()
    {
        return vv.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.LAYOUT);
    }

    @Override
    public void zoomIn()
    {
        OSMMapStateBuilder.getSingleton().zoomIn();
    }

    @Override
    public void zoomOut()
    {
        OSMMapStateBuilder.getSingleton().zoomOut();
    }

    public void addLink(Link npLink)
    {
        if (linkTable.containsKey(npLink)) throw new RuntimeException("Bad - Link " + npLink + " already exists");
        final GUINode originNode = nodeTable.get(npLink.getOriginNode());
        final GUINode destNode = nodeTable.get(npLink.getDestinationNode());
        if ((originNode == null) || (destNode == null))
            throw new RuntimeException("Bad - Link " + npLink + ". End nodes are still not in the graph");
        GUILink link = new GUILink(npLink, originNode, destNode);
        linkTable.put(npLink, link);
        g.addEdge(link, link.getOriginNode(), link.getDestinationNode());
    }

    private void removeLink(Link npLink, boolean alsoFromGraph)
    {
        GUILink link = linkTable.get(npLink);

        linkTable.remove(npLink);

        if (alsoFromGraph)
        {
            g.removeEdge(link);
            refresh();
        }
    }

    public void zoom(float scale)
    {
        scalingControl.scale(vv, scale, vv.getCenter());
    }

    public void zoomIn(Point2D point)
    {
        scalingControl.scale(vv, SCALE_IN, point);
    }

    public void zoomOut(Point2D point)
    {
        scalingControl.scale(vv, SCALE_OUT, point);
    }

    public void setBackgroundImage(final File bgFile, final double x, final double y)
    {
        final Double x1 = x;
        final Double y1 = y;

        setBackgroundImage(bgFile, x1.intValue(), y1.intValue());
    }

    public void setBackgroundImage(final ImageIcon image, final double x, final double y)
    {
        final Double x1 = x;
        final Double y1 = y;

        setBackgroundImage(image, x1.intValue(), y1.intValue());
    }

    public void setBackgroundImage(final ImageIcon image, final int x, final int y)
    {
        updateBackgroundImage(image, x, y);
    }

    public void setBackgroundImage(final File bgFile, final int x, final int y)
    {
        final ImageIcon background = new ImageIcon(bgFile.getAbsolutePath());
        updateBackgroundImage(background, x, y);
    }

    public void updateBackgroundImage(final ImageIcon icon)
    {
        updateBackgroundImage(icon, 0, 0);
    }

    public void updateBackgroundImage(final ImageIcon icon, final int x, final int y)
    {
        if (paintableAssociatedToBackgroundImage != null)
            vv.removePreRenderPaintable(paintableAssociatedToBackgroundImage);
        paintableAssociatedToBackgroundImage = null;
        if (icon != null)
        {
            this.paintableAssociatedToBackgroundImage = new VisualizationViewer.Paintable()
            {
                public void paint(Graphics g)
                {
                    Graphics2D g2d = (Graphics2D) g;
                    AffineTransform oldXform = g2d.getTransform();
                    AffineTransform lat = vv.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.LAYOUT).getTransform();
                    AffineTransform vat = vv.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.VIEW).getTransform();
                    AffineTransform at = new AffineTransform();
                    at.concatenate(g2d.getTransform());
                    at.concatenate(vat);
                    at.concatenate(lat);
                    g2d.setTransform(at);
                    g.drawImage(icon.getImage(), x, y, icon.getIconWidth(), icon.getIconHeight(), vv);
                    g2d.setTransform(oldXform);
                }

                public boolean useTransform()
                {
                    return false;
                }
            };
            vv.addPreRenderPaintable(paintableAssociatedToBackgroundImage);
        }
    }

    private class LinkIdRenderer extends BasicEdgeLabelRenderer<GUINode, GUILink>
    {
        @Override
        public void labelEdge(RenderContext<GUINode, GUILink> rc, Layout<GUINode, GUILink> layout, GUILink e, String label)
        {
            if (showLinkIds) super.labelEdge(rc, layout, e, e.getLabel());
        }
    }

    private final class NodeDisplayPredicate<Node, Link> implements Predicate<Context<Graph<Node, Link>, Node>>
    {
        @Override
        public boolean evaluate(Context<Graph<Node, Link>, Node> context)
        {
            com.net2plan.gui.utils.topologyPane.GUINode v = (com.net2plan.gui.utils.topologyPane.GUINode) context.element;
            if (!showHideNonConnectedNodes)
            {
                Collection<GUILink> incidentLinks = g.getIncidentEdges(v);
                if (incidentLinks == null) return false;
                if (incidentLinks.isEmpty()) return false;
            }

            return v.isVisible();
        }
    }


    private class NodeLabelRenderer extends BasicVertexLabelRenderer<GUINode, GUILink>
    {
        @Override
        public void labelVertex(RenderContext<GUINode, GUILink> rc, Layout<GUINode, GUILink> layout, GUINode v, String label)
        {
            Graph<GUINode, GUILink> graph = layout.getGraph();
            if (rc.getVertexIncludePredicate().evaluate(Context.getInstance(graph, v)) == false)
            {
                return;
            }

            Point2D pt = layout.transform(v);
            pt = rc.getMultiLayerTransformer().transform(Layer.LAYOUT, pt);

            float x = (float) pt.getX();
            float y = (float) pt.getY();

            Component component = prepareRenderer(rc, rc.getVertexLabelRenderer(), "<html><font color='white'>" + v.getAssociatedNetPlanNode().getIndex() + "</font></html>", rc.getPickedVertexState().isPicked(v), v);
            GraphicsDecorator g = rc.getGraphicsContext();
            Dimension d = component.getPreferredSize();
            AffineTransform xform = AffineTransform.getTranslateInstance(x, y);

            Shape shape = rc.getVertexShapeTransformer().transform(v);
            shape = xform.createTransformedShape(shape);
            GraphicsDecorator gd = rc.getGraphicsContext();
            if (gd instanceof TransformingGraphics)
            {
                BidirectionalTransformer transformer = ((TransformingGraphics) gd).getTransformer();
                if (transformer instanceof ShapeTransformer)
                {
                    ShapeTransformer shapeTransformer = (ShapeTransformer) transformer;
                    shape = shapeTransformer.transform(shape);
                }
            }

            Rectangle2D bounds = shape.getBounds2D();

            Point p = getAnchorPoint(bounds, d, Renderer.VertexLabel.Position.CNTR);
            g.draw(component, rc.getRendererPane(), p.x, p.y, d.width, d.height, true);

            if (showNodeNames)
            {
                component = prepareRenderer(rc, rc.getVertexLabelRenderer(), "<html><font color='black'>" + v.getLabel() + "</font></html>", rc.getPickedVertexState().isPicked(v), v);
                g = rc.getGraphicsContext();
                d = component.getPreferredSize();
                xform = AffineTransform.getTranslateInstance(x, y);

                shape = rc.getVertexShapeTransformer().transform(v);
                shape = xform.createTransformedShape(shape);
                if (rc.getGraphicsContext() instanceof TransformingGraphics)
                {
                    BidirectionalTransformer transformer = ((TransformingGraphics) rc.getGraphicsContext()).getTransformer();
                    if (transformer instanceof ShapeTransformer)
                    {
                        ShapeTransformer shapeTransformer = (ShapeTransformer) transformer;
                        shape = shapeTransformer.transform(shape);
                    }
                }

                bounds = shape.getBounds2D();

                p = getAnchorPoint(bounds, d, Renderer.VertexLabel.Position.NE);
                g.draw(component, rc.getRendererPane(), p.x, p.y, d.width, d.height, true);
            }
        }

        @Override
        protected Point getAnchorPoint(Rectangle2D vertexBounds, Dimension labelSize, Renderer.VertexLabel.Position position)
        {
            double x;
            double y;
            int offset = 5;
            switch (position)
            {
                case NE:
                    x = vertexBounds.getMaxX() - offset;
                    y = vertexBounds.getMinY() + offset - labelSize.height;
                    return new Point((int) x, (int) y);
                case CNTR:
                    x = vertexBounds.getCenterX() - ((double) labelSize.width / 2);
                    y = vertexBounds.getCenterY() - ((double) labelSize.height / 2);
                    return new Point((int) x, (int) y);

                default:
                    return new Point();
            }

        }
    }

    private static class ScalingCanvasPlugin extends ScalingGraphMousePlugin implements ITopologyCanvasPlugin
    {
        private ITopologyCanvas canvas;

        public ScalingCanvasPlugin(ScalingControl scaler, int modifiers)
        {
            super(scaler, modifiers, SCALE_OUT, SCALE_IN);
            setZoomAtMouse(false);
        }

        @Override
        public ITopologyCanvas getCanvas()
        {
            return canvas;
        }

        @Override
        public void setCanvas(ITopologyCanvas canvas)
        {
            this.canvas = canvas;
        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent e)
        {
            boolean accepted = this.checkModifiers(e);
            if (accepted)
            {
                VisualizationViewer vv = (VisualizationViewer) e.getSource();
                int amount = e.getWheelRotation();
                if (this.zoomAtMouse)
                {
                    if (amount > 0)
                    {
                        OSMMapStateBuilder.getSingleton().zoomOut();
                    } else if (amount < 0)
                    {
                        OSMMapStateBuilder.getSingleton().zoomIn();
                    }
                } else if (amount > 0)
                {
                    OSMMapStateBuilder.getSingleton().zoomOut();
                } else if (amount < 0)
                {
                    OSMMapStateBuilder.getSingleton().zoomIn();
                }

                e.consume();
                vv.repaint();
            }

        }
    }
}
