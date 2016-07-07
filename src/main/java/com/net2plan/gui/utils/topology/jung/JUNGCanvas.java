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


package com.net2plan.gui.utils.topology.jung;

import com.net2plan.gui.utils.topology.GUILink;
import com.net2plan.gui.utils.topology.GUINode;
import com.net2plan.gui.utils.topology.ITopologyCanvasPlugin;
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
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
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
public final class JUNGCanvas extends ITopologyCanvas {
    private NetPlan associatedNetPlan;
    private final static Color CANVAS_BGCOLOR = new Color(212, 208, 200);
    private final static float SCALE_IN = 1.1f;
    private final static float SCALE_OUT = 1 / SCALE_IN;
    private final static Transformer<GUINode, Point2D> FLIP_VERTICAL_COORDINATES;

//	private int FONT_SIZE = 11;
//	private double NODE_SIZE = 30;

    private final Graph<GUINode, GUILink> g;
    private final Layout<GUINode, GUILink> l;
    private final VisualizationViewer<GUINode, GUILink> vv;
    private final Map<Node, GUINode> nodeTable;
    private final Map<Link, GUILink> linkTable;
    private final PluggableGraphMouse gm;
    private final ScalingControl scalingControl;
    private final Transformer<Context<Graph<GUINode, GUILink>, GUILink>, Shape> originalEdgeShapeTransformer;
//	private final Set<GUILink> linksDown, hiddenLinks, backupPath;
//	private final Set<GUINode> nodesDown, hiddenNodes;

    private boolean showNodeNames, showLinkIds, showHideNonConnectedNodes;

    static {
        FLIP_VERTICAL_COORDINATES = new Transformer<GUINode, Point2D>() {
            public Point2D transform(GUINode vertex) {
                Point2D pos = vertex.getAssociatedNetPlanNode().getXYPositionMap();
                return new Point2D.Double(pos.getX(), -pos.getY());
            }
        };
    }

    /**
     * Default constructor.
     *
     * @since 0.2.3
     */
    public JUNGCanvas() {
//		backupPath = new LinkedHashSet<GUILink>();

        nodeTable = new LinkedHashMap<Node, GUINode>();
        linkTable = new LinkedHashMap<Link, GUILink>();

//		linksDown = new LinkedHashSet<GUILink>();
//		nodesDown = new LinkedHashSet<GUINode>();

        g = new DirectedOrderedSparseMultigraph<GUINode, GUILink>();
        l = new StaticLayout<GUINode, GUILink>(g, FLIP_VERTICAL_COORDINATES);
        vv = new VisualizationViewer<GUINode, GUILink>(l);
        originalEdgeShapeTransformer = new EdgeShape.QuadCurve<GUINode, GUILink>(); //vv.getRenderContext().getEdgeShapeTransformer();
        ((EdgeShape.QuadCurve<GUINode, GUILink>) originalEdgeShapeTransformer).setControlOffsetIncrement(10); // how much they separate from the direct line (default is 20)
        ((EdgeShape.QuadCurve<GUINode, GUILink>) originalEdgeShapeTransformer).setEdgeIndexFunction(DefaultParallelEdgeIndexFunction.<GUINode, GUILink>getInstance()); // how much they separate from the direct line (default is 20)

		/* Customize the graph */
        vv.getRenderContext().setVertexDrawPaintTransformer(new Transformer<GUINode, Paint>() {
            public Paint transform(GUINode n) {
                return n.getUserDefinedColorOverridesTheRest() == null ? n.getDrawPaint() : n.getUserDefinedColorOverridesTheRest();
            }
        });
        vv.getRenderContext().setVertexFillPaintTransformer(new Transformer<GUINode, Paint>() {
            public Paint transform(GUINode n) {
                return n.getUserDefinedColorOverridesTheRest() != null ? n.getUserDefinedColorOverridesTheRest() : vv.getPickedVertexState().isPicked(n) ? n.getFillPaintIfPicked() : n.getFillPaint();
            }
        });
        vv.getRenderContext().setVertexFontTransformer(new Transformer<GUINode, Font>() {
            public Font transform(GUINode n) {
                return n.getFont();
            }
        });

		
		/* If icons => comment this line */
        vv.getRenderContext().setVertexShapeTransformer(new Transformer<GUINode, Shape>() {
            public Shape transform(GUINode n) {
                return vv.getPickedVertexState().isPicked(n) ? n.getShapeIfPicked() : n.getShape();
            }
        });
		/* If shapes, comment this line */
        //vv.getRenderContext().setVertexIconTransformer(new Transformer<GUINode,Icon> () {} ... )

        vv.getRenderContext().setVertexIncludePredicate(new NodeDisplayPredicate<GUINode, GUILink>());
        vv.getRenderer().setVertexLabelRenderer(new NodeLabelRenderer());
        vv.setVertexToolTipTransformer(new Transformer<GUINode, String>() {
            public String transform(GUINode node) {
                return node.getToolTip();
            }
        });


        vv.getRenderContext().setEdgeIncludePredicate(new Predicate<Context<Graph<GUINode, GUILink>, GUILink>>() {
            public boolean evaluate(Context<Graph<GUINode, GUILink>, GUILink> context) {
                return context.element.isVisible();
            }
        });
        vv.getRenderContext().setEdgeArrowPredicate(new Predicate<Context<Graph<GUINode, GUILink>, GUILink>>() {
            public boolean evaluate(Context<Graph<GUINode, GUILink>, GUILink> context) {
                return context.element.isVisible() && context.element.getHasArrow();
            }
        });
        vv.getRenderContext().setEdgeArrowStrokeTransformer(new Transformer<GUILink, Stroke>() {
            public Stroke transform(GUILink i) {
                return vv.getPickedEdgeState().isPicked(i) ? i.getArrowStroke().getSecond() : i.getArrowStroke().getFirst();
            }
        });
        vv.getRenderContext().setEdgeArrowTransformer(new ConstantTransformer(ArrowFactory.getNotchedArrow(7, 10, 5)));
        vv.getRenderContext().setEdgeLabelClosenessTransformer(new ConstantDirectionalEdgeValueTransformer(.6, .6));
        vv.getRenderContext().setEdgeStrokeTransformer(new Transformer<GUILink, Stroke>() {
            public Stroke transform(GUILink i) {
                return i.getUserDefinedStrokeOverridesTheRest() != null ? i.getUserDefinedStrokeOverridesTheRest() : vv.getPickedEdgeState().isPicked(i) ? i.getEdgeStroke().getSecond() : i.getEdgeStroke().getFirst();
            }
        });

        vv.getRenderContext().setEdgeDrawPaintTransformer(new Transformer<GUILink, Paint>() {
            public Paint transform(GUILink e) {
                return e.getUserDefinedColorOverridesTheRest() != null ? e.getUserDefinedColorOverridesTheRest() : vv.getPickedEdgeState().isPicked(e) ? e.getEdgeDrawPaint().getSecond() : e.getEdgeDrawPaint().getFirst();
            }
        });
        vv.getRenderContext().setArrowDrawPaintTransformer(new Transformer<GUILink, Paint>() {
            public Paint transform(GUILink e) {
                return e.getUserDefinedColorOverridesTheRest() != null ? e.getUserDefinedColorOverridesTheRest() : vv.getPickedEdgeState().isPicked(e) ? e.getArrowDrawPaint().getSecond() : e.getArrowDrawPaint().getFirst();
            }
        });
        vv.getRenderContext().setArrowFillPaintTransformer(new Transformer<GUILink, Paint>() {
            public Paint transform(GUILink e) {
                return e.getUserDefinedColorOverridesTheRest() != null ? e.getUserDefinedColorOverridesTheRest() : vv.getPickedEdgeState().isPicked(e) ? e.getArrowFillPaint().getSecond() : e.getArrowFillPaint().getFirst();
            }
        });

        vv.getRenderContext().setEdgeLabelRenderer(new DefaultEdgeLabelRenderer(Color.BLUE));
//		vv.getRenderer().setEdgeLabelRenderer(new LinkIdRenderer());
        vv.getRenderer().setEdgeLabelRenderer(new BasicEdgeLabelRenderer<GUINode, GUILink>() {
            public void labelEdge(RenderContext<GUINode, GUILink> rc, Layout<GUINode, GUILink> layout, GUILink e, String label) {
                if (showLinkIds) super.labelEdge(rc, layout, e, e.getLabel());
            }
        });
        vv.setEdgeToolTipTransformer(new Transformer<GUILink, String>() {
            public String transform(GUILink link) {
                return link.getToolTip();
            }
        });
        vv.getRenderContext().setEdgeShapeTransformer(new Transformer<Context<Graph<GUINode, GUILink>, GUILink>, Shape>() {
                                                          public Shape transform(Context<Graph<GUINode, GUILink>, GUILink> c) {
                                                              final GUINode origin = c.element.getOriginNode();
                                                              final GUINode destination = c.element.getDestinationNode();
                                                              boolean separateTheLinks = vv.getPickedVertexState().isPicked(origin) || vv.getPickedVertexState().isPicked(destination);
                                                              if (!separateTheLinks) {
                                                                  Set<GUILink> linksNodePair = new HashSet<GUILink>(c.graph.getIncidentEdges(destination));
                                                                  linksNodePair.retainAll(c.graph.getIncidentEdges(origin));
                                                                  for (GUILink e : linksNodePair)
                                                                      if (vv.getPickedEdgeState().isPicked(e) || !e.getAssociatedNetPlanLink().isUp()) {
                                                                          separateTheLinks = true;
                                                                          break;
                                                                      }
                                                              }
                                                              return separateTheLinks ? originalEdgeShapeTransformer.transform(c) : new Line2D.Float(0.0f, 0.0f, 1.0f, 0.0f);
                                                          }
                                                      }
        );


        showNodeNames(false);
        showNonConnectedNodes(true);
        showLinkLabels(false);

        gm = new PluggableGraphMouse();
        vv.setGraphMouse(gm);

        scalingControl = new LayoutScalingControl();
        ITopologyCanvasPlugin scalingPlugin = new ScalingCanvasPlugin(scalingControl, MouseEvent.NOBUTTON);
        addPlugin(scalingPlugin);

        vv.setBackground(CANVAS_BGCOLOR);

//		hiddenNodes = new LinkedHashSet<GUINode>();
//		hiddenLinks = new LinkedHashSet<GUILink>();
//		
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
    public void addPlugin(ITopologyCanvasPlugin plugin) {
        plugin.setCanvas(this);
        gm.add(new GraphMousePluginAdapter(plugin));
    }

    @Override
    public Point2D convertViewCoordinatesToRealCoordinates(Point screenPoint) {
        Point2D layoutCoordinates = vv.getRenderContext().getMultiLayerTransformer().inverseTransform(Layer.LAYOUT, screenPoint);
        layoutCoordinates.setLocation(layoutCoordinates.getX(), -layoutCoordinates.getY());

        return layoutCoordinates;
    }

    @Override
    public void decreaseFontSize() {
        boolean changedSize = false;
        for (GUINode n : nodeTable.values()) changedSize |= n.decreaseFontSize();
        if (changedSize) refresh();
    }

    @Override
    public void decreaseNodeSize() {
        for (GUINode n : nodeTable.values()) n.setShapeSize(n.getShapeSize() * SCALE_OUT);
        refresh();
    }

    @Override
    public JComponent getComponent() {
        return vv;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public JComponent getInternalComponent() {
        return vv;
    }

    @Override
    public long getLink(MouseEvent e) {
        final VisualizationViewer<GUINode, GUILink> vv = (VisualizationViewer<GUINode, GUILink>) e.getSource();
        GraphElementAccessor<GUINode, GUILink> pickSupport = vv.getPickSupport();
        if (pickSupport != null) {
            final Point p = e.getPoint();
            final GUILink edge = pickSupport.getEdge(vv.getModel().getGraphLayout(), p.getX(), p.getY());
            if (edge != null) return edge.getAssociatedNetPlanLink().getId();
        }

        return -1;
    }

    @Override
    public String getName() {
        return "JUNG Canvas";
    }

    @Override
    public long getNode(MouseEvent e) {
        final VisualizationViewer<GUINode, GUILink> vv = (VisualizationViewer<GUINode, GUILink>) e.getSource();
        GraphElementAccessor<GUINode, GUILink> pickSupport = vv.getPickSupport();
        if (pickSupport != null) {
            final Point p = e.getPoint();
            final GUINode vertex = pickSupport.getVertex(vv.getModel().getGraphLayout(), p.getX(), p.getY());
            if (vertex != null) return vertex.getAssociatedNetPlanNode().getId();
        }

        return -1;
    }

    @Override
    public List<Triple<String, String, String>> getParameters() {
        return null;
    }

    @Override
    public void increaseFontSize() {
        for (GUINode n : nodeTable.values()) n.increaseFontSize();
        refresh();
    }

    @Override
    public void increaseNodeSize() {
        for (GUINode n : nodeTable.values()) n.setShapeSize(n.getShapeSize() * SCALE_IN);
        refresh();
    }

    @Override
    public boolean isLinkVisible(Link npLink) {
        GUILink e = linkTable.get(npLink);
        return e == null ? false : e.isVisible();
    }

    @Override
    public boolean isNodeVisible(Node npNode) {
        GUINode n = nodeTable.get(npNode);
        return n == null ? false : n.isVisible();
    }

    @Override
    public void panTo(Point initialPoint, Point currentPoint) {
        final MutableTransformer layoutTransformer = vv.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.LAYOUT);
        final Point2D q = layoutTransformer.inverseTransform(initialPoint);
        final Point2D lvc = layoutTransformer.inverseTransform(currentPoint);
        final double dx = (lvc.getX() - q.getX());
        final double dy = (lvc.getY() - q.getY());
        layoutTransformer.translate(dx, dy);
    }

    @Override
    public void refresh() {
        vv.repaint();
    }

    @Override
    public void removeLink(Link npLink) {
        removeLink(npLink, true);
    }

    @Override
    public void removeNode(Node npNode) {
        GUINode node = nodeTable.get(npNode);

        Iterator<GUILink> linkIt;
        Collection<GUILink> outLinks = g.getOutEdges(node);
        if (outLinks == null) outLinks = new LinkedHashSet<GUILink>();
        linkIt = outLinks.iterator();
        while (linkIt.hasNext()) removeLink(linkIt.next().getAssociatedNetPlanLink(), false);

        Collection<GUILink> inLinks = g.getInEdges(node);
        if (inLinks == null) inLinks = new LinkedHashSet<GUILink>();
        linkIt = inLinks.iterator();
        while (linkIt.hasNext()) removeLink(linkIt.next().getAssociatedNetPlanLink(), false);

        nodeTable.remove(npNode);
        g.removeVertex(node);
    }

    @Override
    public void removePlugin(ITopologyCanvasPlugin plugin) {
        if (plugin instanceof GraphMousePlugin) gm.remove((GraphMousePlugin) plugin);
    }

    @Override
    public void reset() {
        Iterator<GUILink> linkIt = linkTable.values().iterator();
        while (linkIt.hasNext()) g.removeEdge(linkIt.next());

        Iterator<GUINode> nodeIt = nodeTable.values().iterator();
        while (nodeIt.hasNext()) g.removeVertex(nodeIt.next());

        nodeTable.clear();
        linkTable.clear();

        refresh();
    }

    @Override
    public void resetPickedAndUserDefinedColorState() {
//		backupPath.clear();
        vv.getPickedVertexState().clear();
        vv.getPickedEdgeState().clear();
        for (GUINode n : nodeTable.values()) n.setUserDefinedColorOverridesTheRest(null);
        for (GUILink e : linkTable.values()) {
            e.setUserDefinedColorOverridesTheRest(null);
            e.setUserDefinedStrokeOverridesTheRest(null);
        }
        refresh();
    }

    @Override
    public void setAllLinksVisible(boolean visible) {
        for (GUILink e : linkTable.values()) e.setVisible(visible);
        refresh();
    }

    @Override
    public void setAllNodesVisible(boolean visible) {
        for (GUINode n : nodeTable.values()) n.setVisible(visible);
        refresh();
    }

    @Override
    public void setLinkVisible(Link link, boolean visible) {
        GUILink e = linkTable.get(link);
        if (e == null) throw new RuntimeException("Bad");
        e.setVisible(visible);
        refresh();
    }

    @Override
    public void setLinksVisible(Collection<Link> links, boolean visible) {
        for (Link link : links) {
            GUILink e = linkTable.get(link);
            if (e == null) throw new RuntimeException("Bad");
            e.setVisible(visible);
        }

        refresh();
    }

    @Override
    public void setNodeVisible(Node npNode, boolean visible) {
        GUINode node = nodeTable.get(npNode);
        if (node == null) throw new RuntimeException("Bad");
        node.setVisible(visible);
        refresh();
    }

    @Override
    public void setNodesVisible(Collection<Node> npNodes, boolean visible) {
        for (Node npNode : npNodes) {
            GUINode node = nodeTable.get(npNode);
            node.setVisible(visible);
        }

        refresh();
    }

    @Override
    public void showLinkLabels(boolean show) {
        if (showLinkIds != show) {
            showLinkIds = show;
            refresh();
        }
    }

    @Override
    public void showNodeNames(boolean show) {
        if (showNodeNames != show) {
            showNodeNames = show;
            refresh();
        }
    }

    @Override
    public void showAndPickNodesAndLinks(Map<Node, Color> npNodes, Map<Link, Pair<Color, Boolean>> npLinks) {
        resetPickedAndUserDefinedColorState();

//		System.out.println ("showNodesAndLinks : nodeIds; " + nodeIds + ", linkIds: " + linkIds);

        if (npNodes != null) {
            for (Entry<Node, Color> npNode : npNodes.entrySet()) {
                GUINode aux = nodeTable.get(npNode.getKey());
                aux.setUserDefinedColorOverridesTheRest(npNode.getValue());
                vv.getPickedVertexState().pick(aux, true);
            }
        }

        if (npLinks != null) {
            for (Entry<Link, Pair<Color, Boolean>> link : npLinks.entrySet()) {
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
    public void showNonConnectedNodes(boolean show) {
        if (showHideNonConnectedNodes != show) {
            showHideNonConnectedNodes = show;
            refresh();
        }
    }

//	@Override
//	public void showRoutes(Collection<Pair<Link,Color>> primaryRouteLinks, Collection<Pair<Link,Color>> secondaryRouteLinks)
//	{
//		resetPickedAndUserDefinedColorState();
//		
//		if (secondaryRouteLinks != null) 
//			for (Pair<Link,Color> linkId : secondaryRouteLinks)
//			{
//				GUILink e = linkTable.get (linkId.getFirst ()); 
//				if (e == null) {System.out.println ("showRoutes: link " + linkId.getFirst () + " not found"); continue; }
//				vv.getPickedEdgeState().pick(e, true);
//				e.setUserDefinedColorOverridesTheRest(linkId.getSecond ());
//				e.setUserDefinedStrokeOverridesTheRest(new BasicStroke(vv.getPickedEdgeState().isPicked(e) ? 2 : 1, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, new float[] { 10 }, 0.0f));
//			}
//		if (primaryRouteLinks != null) 
//			for (Pair<Link,Color> linkId : primaryRouteLinks)
//			{
//				GUILink e = linkTable.get (linkId.getFirst ()); 
//				if (e == null) {System.out.println ("showRoutes: link " + linkId.getFirst () + " not found"); continue; }
//				vv.getPickedEdgeState().pick(e, true);
//				e.setUserDefinedColorOverridesTheRest(linkId.getSecond ());
//				e.setUserDefinedStrokeOverridesTheRest(null);
//			}
//		refresh ();
//	}

    @Override
    public void takeSnapshot_preConfigure() {
        vv.setBackground(Color.WHITE);
    }

    @Override
    public void takeSnapshot_postConfigure() {
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
    public void zoomAll() {
        Set<GUINode> nodes = new LinkedHashSet<GUINode>();
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
        for (GUINode node : nodes) {
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
//		GUINode aux = new GUINode(-1, new Point2D.Double((aux_xmin + aux_xmax) / 2, (aux_ymin + aux_ymax) / 2));
        Point2D q = new Point2D.Double((auxTransf_xmin + auxTransf_xmax) / 2, (auxTransf_ymin + auxTransf_ymax) / 2);
        Point2D lvc = vv.getRenderContext().getMultiLayerTransformer().inverseTransform(vv.getCenter());
        double dx = (lvc.getX() - q.getX());
        double dy = (lvc.getY() - q.getY());
        vv.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.LAYOUT).translate(dx, dy);
    }

    @Override
    public void updateNodeXYPosition(Node npNode) {
        GUINode node = nodeTable.get(npNode);
        l.setLocation(node, FLIP_VERTICAL_COORDINATES.transform(node));
    }

    @Override
    public void zoomIn() {
        zoomIn(vv.getCenter());
    }

    @Override
    public void zoomOut() {
        zoomOut(vv.getCenter());
    }

    public void addLink(Link npLink) {
        if (linkTable.containsKey(npLink)) throw new RuntimeException("Bad - Link " + npLink + " already exists");
        final GUINode originNode = nodeTable.get(npLink.getOriginNode());
        final GUINode destNode = nodeTable.get(npLink.getDestinationNode());
        if ((originNode == null) || (destNode == null))
            throw new RuntimeException("Bad - Link " + npLink + ". End nodes are still not in the graph");
        GUILink link = new GUILink(npLink, originNode, destNode);
        linkTable.put(npLink, link);
        g.addEdge(link, link.getOriginNode(), link.getDestinationNode());
    }

    private void removeLink(Link npLink, boolean alsoFromGraph) {
        GUILink link = linkTable.get(npLink);

        linkTable.remove(npLink);

        if (alsoFromGraph) {
            g.removeEdge(link);
            refresh();
        }
    }

    private void zoomIn(Point2D point) {
        scalingControl.scale(vv, SCALE_IN, point);
    }

    private void zoomOut(Point2D point) {
        scalingControl.scale(vv, SCALE_OUT, point);
    }

    //    private final class LinkDisplayPredicate<Node, Link> implements Predicate<Context<Graph<Node, Link>, Link>>
//    {
//		@Override
//        public boolean evaluate(Context<Graph<Node, Link>, Link> context)
//        {
//        	com.net2plan.gui.utils.topology.GUILink e = (com.net2plan.gui.utils.topology.GUILink) context.element;
//			return !hiddenLinks.contains(e);
//        }
//    }
//	
    private class LinkIdRenderer extends BasicEdgeLabelRenderer<GUINode, GUILink> {
        @Override
        public void labelEdge(RenderContext<GUINode, GUILink> rc, Layout<GUINode, GUILink> layout, GUILink e, String label) {
            if (showLinkIds) super.labelEdge(rc, layout, e, e.getLabel());
        }
    }

    private final class NodeDisplayPredicate<Node, Link> implements Predicate<Context<Graph<Node, Link>, Node>> {
        @Override
        public boolean evaluate(Context<Graph<Node, Link>, Node> context) {
            com.net2plan.gui.utils.topology.GUINode v = (com.net2plan.gui.utils.topology.GUINode) context.element;
            if (!showHideNonConnectedNodes) {
                Collection<GUILink> incidentLinks = g.getIncidentEdges(v);
                if (incidentLinks == null) return false;
                if (incidentLinks.isEmpty()) return false;
            }

            return v.isVisible();
        }
    }


    private class NodeLabelRenderer extends BasicVertexLabelRenderer<GUINode, GUILink> {
        @Override
        public void labelVertex(RenderContext<GUINode, GUILink> rc, Layout<GUINode, GUILink> layout, GUINode v, String label) {
            Graph<GUINode, GUILink> graph = layout.getGraph();
            if (rc.getVertexIncludePredicate().evaluate(Context.<Graph<GUINode, GUILink>, GUINode>getInstance(graph, v)) == false) {
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
            if (gd instanceof TransformingGraphics) {
                BidirectionalTransformer transformer = ((TransformingGraphics) gd).getTransformer();
                if (transformer instanceof ShapeTransformer) {
                    ShapeTransformer shapeTransformer = (ShapeTransformer) transformer;
                    shape = shapeTransformer.transform(shape);
                }
            }

            Rectangle2D bounds = shape.getBounds2D();

            Point p = getAnchorPoint(bounds, d, Renderer.VertexLabel.Position.CNTR);
            g.draw(component, rc.getRendererPane(), p.x, p.y, d.width, d.height, true);

            if (showNodeNames) {
                component = prepareRenderer(rc, rc.getVertexLabelRenderer(), "<html><font color='black'>" + v.getLabel() + "</font></html>", rc.getPickedVertexState().isPicked(v), v);
                g = rc.getGraphicsContext();
                d = component.getPreferredSize();
                xform = AffineTransform.getTranslateInstance(x, y);

                shape = rc.getVertexShapeTransformer().transform(v);
                shape = xform.createTransformedShape(shape);
                if (rc.getGraphicsContext() instanceof TransformingGraphics) {
                    BidirectionalTransformer transformer = ((TransformingGraphics) rc.getGraphicsContext()).getTransformer();
                    if (transformer instanceof ShapeTransformer) {
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
        protected Point getAnchorPoint(Rectangle2D vertexBounds, Dimension labelSize, Renderer.VertexLabel.Position position) {
            double x;
            double y;
            int offset = 5;
            switch (position) {
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

    private static class ScalingCanvasPlugin extends ScalingGraphMousePlugin implements ITopologyCanvasPlugin {
        private ITopologyCanvas canvas;

        public ScalingCanvasPlugin(ScalingControl scaler, int modifiers) {
            super(scaler, modifiers, SCALE_OUT, SCALE_IN);
            setZoomAtMouse(false);
        }

        @Override
        public ITopologyCanvas getCanvas() {
            return canvas;
        }

        @Override
        public void setCanvas(ITopologyCanvas canvas) {
            this.canvas = canvas;
        }
    }

//	private class StateAwareTransformer<Object, Paint> implements Transformer<Object, Paint>
//	{
//		private final Transformer<Object, Paint> transformer;
//
//		public StateAwareTransformer(Transformer<Object, Paint> transformer)
//		{
//			this.transformer = transformer;
//		}
//
//		@Override
//		public Paint transform(Object o)
//		{
//			if ((o instanceof GUINode && nodesDown.contains((GUINode) o)) || (o instanceof GUILink && linksDown.contains((GUILink) o)))
//			{
//				return (Paint) Color.RED;
//			}
//
//			return transformer.transform(o);
//		}
//	}

//	private static class TransformerImpl implements Transformer<GUINode, Point2D>
//	{
//		@Override
//		public Point2D transform(GUINode vertex)
//		{
//			Point2D pos = vertex.getPosition();
//			return new Point2D.Double(pos.getX(), -pos.getY());
//		}
//	}
}
