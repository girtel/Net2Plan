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


package com.net2plan.gui.plugins.networkDesign.topologyPane.jung;

import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.gui.plugins.networkDesign.interfaces.ITopologyCanvas;
import com.net2plan.gui.plugins.networkDesign.interfaces.ITopologyCanvasPlugin;
import com.net2plan.gui.plugins.networkDesign.topologyPane.TopologyPanel;
import com.net2plan.gui.plugins.networkDesign.topologyPane.jung.plugins.GraphMousePluginAdapter;
import com.net2plan.gui.plugins.networkDesign.topologyPane.jung.state.CanvasOption;
import com.net2plan.gui.plugins.networkDesign.topologyPane.jung.state.CanvasStateController;
import com.net2plan.gui.plugins.networkDesign.visualizationControl.VisualizationConstants;
import com.net2plan.interfaces.networkDesign.Configuration;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.internal.CommandLineParser;
import com.net2plan.utils.Triple;
import edu.uci.ics.jung.algorithms.layout.GraphElementAccessor;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.algorithms.layout.StaticLayout;
import edu.uci.ics.jung.graph.DirectedOrderedSparseMultigraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.Context;
import edu.uci.ics.jung.graph.util.EdgeIndexFunction;
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
import edu.uci.ics.jung.visualization.transform.MutableTransformer;
import edu.uci.ics.jung.visualization.transform.shape.GraphicsDecorator;
import edu.uci.ics.jung.visualization.util.ArrowFactory;
import org.apache.commons.collections15.Transformer;
import org.apache.commons.collections15.functors.ConstantTransformer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.List;

/**
 * Topology canvas using JUNG library [<a href='#jung'>JUNG</a>].
 *
 * @see <a name='jung'></a><a href='http://jung.sourceforge.net/'>Java Universal Network/Graph Framework (JUNG) website</a>
 */
@SuppressWarnings("unchecked")
public final class JUNGCanvas implements ITopologyCanvas
{
    private final GUINetworkDesign callback;

    private double currentInterLayerDistanceInNpCoordinates;

    private final Graph<GUINode, GUILink> g;
    private final Layout<GUINode, GUILink> l;
    private final VisualizationViewer<GUINode, GUILink> vv;
    private final PluggableGraphMouse gm;
    private final ScalingControl scalingControl;
    private final Transformer<GUINode, Point2D> transformNetPlanCoordinatesToJungCoordinates;
    private final Transformer<Context<Graph<GUINode, GUILink>, GUILink>, Shape> originalEdgeShapeTransformer;

    private final CanvasStateController stateController;

    /**
     * Default constructor.
     *
     * @since 0.2.3
     */
    public JUNGCanvas(GUINetworkDesign callback, TopologyPanel topologyPanel)
    {
        this.callback = callback;

    	transformNetPlanCoordinatesToJungCoordinates = vertex ->
        {
            final int vlIndex = this.callback.getVisualizationState().getCanvasVisualizationOrderRemovingNonVisible(vertex.getLayer());
            final double interLayerDistanceInNpCoord = currentInterLayerDistanceInNpCoordinates;
            final Point2D basePositionInNetPlanCoord = vertex.getAssociatedNode().getXYPositionMap();
            return new Point2D.Double(basePositionInNetPlanCoord.getX(), -(basePositionInNetPlanCoord.getY() - (vlIndex * interLayerDistanceInNpCoord)));
        };

        g = new DirectedOrderedSparseMultigraph<>();
        l = new StaticLayout<>(g, transformNetPlanCoordinatesToJungCoordinates); 
        vv = new VisualizationViewer<>(l);

        stateController = new CanvasStateController(callback, topologyPanel, this);

        originalEdgeShapeTransformer = new EdgeShape.QuadCurve<>();
        ((EdgeShape.QuadCurve<GUINode, GUILink>) originalEdgeShapeTransformer).setControlOffsetIncrement(10); // how much they separate from the direct line (default is 20)
        //((EdgeShape.QuadCurve<GUINode, GUILink>) originalEdgeShapeTransformer).setEdgeIndexFunction(DefaultParallelEdgeIndexFunction.<GUINode, GUILink>getInstance()); // how much they separate from the direct line (default is 20)
        /* This functions gives an index to the links to show separate (curved): the order among the parallel links (BUT NOW only among the separated ones among them) */
        ((EdgeShape.QuadCurve<GUINode, GUILink>) originalEdgeShapeTransformer).setEdgeIndexFunction(new EdgeIndexFunction<GUINode,GUILink>()
		{
        	public void reset(Graph<GUINode,GUILink> graph, GUILink e) {}
        	public void reset() {}
        	public int getIndex(Graph<GUINode,GUILink> graph, GUILink e) 
        	{
        		final GUINode u = e.getOriginNode();
        		final GUINode v = e.getDestinationNode();
            	final HashSet<GUILink> commonEdgeSet = new HashSet<>(graph.getInEdges(v));
            	commonEdgeSet.retainAll(graph.getOutEdges(u));
            	commonEdgeSet.removeIf(ee->!ee.isShownSeparated());
            	int count=0;
            	for(GUILink other : commonEdgeSet) 
            		if (other == e)
            			return count;
            		else
            			count ++;
            	throw new RuntimeException();
        	}
		});
		/* Customize the graph */
        vv.getRenderContext().setVertexDrawPaintTransformer(n -> n.getBorderPaint());
        vv.getRenderContext().setVertexFillPaintTransformer(n -> n.getFillPaint());
        vv.getRenderContext().setVertexFontTransformer(n -> n.getFont());

        vv.getRenderContext().setVertexIconTransformer(gn -> gn.getIcon()); 

        vv.getRenderContext().setVertexIncludePredicate(guiNodeContext -> callback.getVisualizationState().isVisibleInCanvas(guiNodeContext.element));
        vv.getRenderer().setVertexLabelRenderer(new NodeLabelRenderer());
        vv.setVertexToolTipTransformer(node -> node.getToolTip());


        vv.getRenderContext().setEdgeIncludePredicate(context -> callback.getVisualizationState().isVisibleInCanvas(context.element));
        vv.getRenderContext().setEdgeArrowPredicate(context -> callback.getVisualizationState().isVisibleInCanvas(context.element) && context.element.getHasArrow());
        vv.getRenderContext().setEdgeArrowStrokeTransformer(i -> i.getStroke());
        vv.getRenderContext().setEdgeArrowTransformer(new ConstantTransformer(ArrowFactory.getNotchedArrow(7, 10, 5)));
        vv.getRenderContext().setEdgeLabelClosenessTransformer(new ConstantDirectionalEdgeValueTransformer(.6, .6));
        vv.getRenderContext().setEdgeStrokeTransformer(i -> i.getEdgeStroke());

        vv.getRenderContext().setEdgeDrawPaintTransformer(e -> e.getEdgeDrawPaint());
        vv.getRenderContext().setArrowDrawPaintTransformer(e -> e.getEdgeDrawPaint());
        vv.getRenderContext().setArrowFillPaintTransformer(e -> e.getEdgeDrawPaint());

        vv.getRenderContext().setEdgeLabelRenderer(new DefaultEdgeLabelRenderer(Color.BLUE));
        vv.getRenderer().setEdgeLabelRenderer(new BasicEdgeLabelRenderer<GUINode, GUILink>()
        {
            public void labelEdge(RenderContext<GUINode, GUILink> rc, Layout<GUINode, GUILink> layout, GUILink e, String label)
            {
                if (callback.getVisualizationState().isCanvasShowLinkLabels()) super.labelEdge(rc, layout, e, e.getLabel());
            }
        });
        vv.setEdgeToolTipTransformer(link -> link.getToolTip());
        vv.getRenderContext().setEdgeShapeTransformer(c -> c.element.isShownSeparated() ? originalEdgeShapeTransformer.transform(c) : new Line2D.Float(0.0f, 0.0f, 1.0f, 0.0f));

        gm = new PluggableGraphMouse();
        vv.setGraphMouse(gm);

        scalingControl = new LayoutScalingControl();
        ITopologyCanvasPlugin scalingPlugin = new ScalingCanvasPlugin(scalingControl, MouseEvent.NOBUTTON);
        addPlugin(scalingPlugin);

        vv.setOpaque(false);
        vv.setBackground(new Color(0, 0, 0, 0));

        this.updateInterLayerDistanceInNpCoordinates(callback.getVisualizationState().getInterLayerSpaceInPixels());

//        reset();
    }

    @Override
    public void addPlugin(ITopologyCanvasPlugin plugin)
    {
        gm.add(new GraphMousePluginAdapter(plugin));
    }

    @Override
    public void removePlugin(ITopologyCanvasPlugin plugin)
    {
        if (plugin instanceof GraphMousePlugin) gm.remove((GraphMousePlugin) plugin);
    }

    @Override
    public Point2D getCanvasPointFromNetPlanPoint(Point2D npCoord)
    {
        Point2D layoutOrViewCoordinates = vv.getRenderContext().getMultiLayerTransformer().inverseTransform(Layer.LAYOUT, npCoord);
        layoutOrViewCoordinates.setLocation(layoutOrViewCoordinates.getX(), -layoutOrViewCoordinates.getY());

        return layoutOrViewCoordinates;
    }

    @Override
    public Point2D getCanvasPointFromScreenPoint(Point2D screenPoint)
    {
        return vv.getRenderContext().getMultiLayerTransformer().inverseTransform(Layer.LAYOUT, screenPoint);
    }

    @Override
    public Point2D getCanvasPointFromMovement(Point2D diffPoint)
    {
        return stateController.getCanvasCoordinateFromScreenPoint(diffPoint);
    }

    public Rectangle getCurrentCanvasViewWindow()
    {
        return vv.getRenderContext().getMultiLayerTransformer().inverseTransform(vv.getBounds()).getBounds();
    }

    public void resetTransformer()
    {
        vv.getRenderContext().getMultiLayerTransformer().setToIdentity();
    }

    @Override
    public String getDescription()
    {
        return "";
    }

    @Override
    public JComponent getCanvasComponent()
    {
        return vv;
    }


    @Override
    public String getName()
    {
        return "JUNG Canvas";
    }

    @Override
    public GUILink getEdge(MouseEvent e)
    {
        final VisualizationViewer<GUINode, GUILink> vv = (VisualizationViewer<GUINode, GUILink>) e.getSource();
        GraphElementAccessor<GUINode, GUILink> pickSupport = vv.getPickSupport();
        if (pickSupport != null)
        {
            final Point p = e.getPoint();
            return pickSupport.getEdge(vv.getModel().getGraphLayout(), p.getX(), p.getY());
        }

        return null;
    }

    public GUINode getVertex(MouseEvent e)
    {
        final VisualizationViewer<GUINode, GUILink> vv = (VisualizationViewer<GUINode, GUILink>) e.getSource();
        GraphElementAccessor<GUINode, GUILink> pickSupport = vv.getPickSupport();
        if (pickSupport != null)
        {
            final Point p = e.getPoint();
            final GUINode vertex = pickSupport.getVertex(vv.getModel().getGraphLayout(), p.getX(), p.getY());
            if (vertex != null) return vertex;
        }

        return null;
    }

    @Override
    public Set<GUINode> getAllVertices()
    {
        return Collections.unmodifiableSet(new HashSet<>(g.getVertices()));
    }

    @Override
    public Set<GUILink> getAllEdges()
    {
        return Collections.unmodifiableSet(new HashSet<>(g.getEdges()));
    }

    public Transformer<GUINode, Point2D> getTransformer()
    {
        return transformNetPlanCoordinatesToJungCoordinates;
    }

    public Layout<GUINode, GUILink> getLayout()
    {
        return l;
    }

    @Override
    public List<Triple<String, String, String>> getParameters()
    {
        return null;
    }

    @Override
    public void refresh()
    {
        vv.repaint();
    }

    @Override
    public void cleanSelection()
    {
        vv.getPickedVertexState().clear();
        vv.getPickedEdgeState().clear();
        refresh();
    }

    @Override
    public void rebuildGraph()
    {
        for (GUILink gl : new ArrayList<>(g.getEdges()))
            g.removeEdge(gl);
        for (GUINode gn : new ArrayList<>(g.getVertices()))
            g.removeVertex(gn);
        for (GUINode gn : callback.getVisualizationState().getCanvasAllGUINodes()) g.addVertex(gn);
        for (GUILink gl : callback.getVisualizationState().getCanvasAllGUILinks(true, true))
            g.addEdge(gl, gl.getOriginNode(), gl.getDestinationNode());

        updateAllVerticesXYPosition();
        refresh();
    }

    @Override
    public void zoomAll()
    {
        stateController.zoomAll();
    }

    @Override
    public void updateAllVerticesXYPosition()
    {
        stateController.updateNodesXYPosition();
    }

    @Override
    public void moveVertexToXYPosition(GUINode npNode, Point2D point)
    {
        l.setLocation(npNode, point);
    }

    @Override
    public void panTo(Point2D initialPoint, Point2D destinationPoint)
    {
        stateController.panTo(initialPoint, destinationPoint);
    }

    @Override
    public void addNode(Point2D position)
    {
        stateController.addNode(position);
    }

    @Override
    public void removeNode(Node node)
    {
        stateController.removeNode(node);
    }

    @Override
    public void moveCanvasTo(Point2D destinationPoint)
    {
        final MutableTransformer layoutTransformer = vv.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.LAYOUT);
        layoutTransformer.translate(destinationPoint.getX(), destinationPoint.getY());
    }

    @Override
    public void zoomIn()
    {
        stateController.zoomIn();
    }

    @Override
    public void zoomOut()
    {
        stateController.zoomOut();
    }

    @Override
    public void zoom(Point2D centerPoint, float scale)
    {
        scalingControl.scale(vv, scale, centerPoint);
    }

    @Override
    public double getCurrentCanvasScale()
    {
        return vv.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.LAYOUT).getScale();
    }

    @Override
    public Point2D getCanvasCenter()
    {
        return vv.getCenter();
    }

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

    @Override
    public void takeSnapshot()
    {
        stateController.takeSnapshot();
    }

    /** STATE CONTROL **/

    @Override
    public void setState(CanvasOption state, Object... stateParams)
    {
        stateController.setState(state, stateParams);
    }

    @Override
    public CanvasOption getState()
    {
        return stateController.getState();
    }

    @Override
    public void returnToPreviousState()
    {
        stateController.returnToPreviousState();
    }

    /** ------ **/

    private class NodeLabelRenderer extends BasicVertexLabelRenderer<GUINode, GUILink>
    {
        @Override
        public void labelVertex(RenderContext<GUINode, GUILink> rc, Layout<GUINode, GUILink> layout, GUINode v, String label)
        {
            if (!callback.getVisualizationState().isVisibleInCanvas(v)) return;
            if (callback.getVisualizationState().isCanvasShowNodeNames() && v.getLayer().isDefaultLayer())
            {
                Point2D vertexPositionInPixels = layout.transform(v);
                vertexPositionInPixels = rc.getMultiLayerTransformer().transform(Layer.LAYOUT, vertexPositionInPixels);
                final Component component = prepareRenderer(rc, rc.getVertexLabelRenderer(), "<html><font color='black'>" + v.getLabel() + "</font></html>", rc.getPickedVertexState().isPicked(v), v);
                final GraphicsDecorator g = rc.getGraphicsContext();
                final Dimension dimensionMessage = component.getPreferredSize();
                final Icon vertexIcon = v.getIcon();
                final Rectangle2D boundsVertex = new Rectangle2D.Double(vertexPositionInPixels.getX() - vertexIcon.getIconWidth() / 2, vertexPositionInPixels.getY() - vertexIcon.getIconHeight() / 2, vertexIcon.getIconWidth(), vertexIcon.getIconHeight());
                final Point anchorPointInPixels = getAnchorPoint(boundsVertex, dimensionMessage, Renderer.VertexLabel.Position.NE);
                g.draw(component, rc.getRendererPane(), anchorPointInPixels.x, anchorPointInPixels.y, dimensionMessage.width, dimensionMessage.height, true);
            }
        }

        @Override
        protected Point getAnchorPoint(Rectangle2D vertexBounds, Dimension labelSize, Renderer.VertexLabel.Position position)
        {
            double x;
            double y;
            int offset = 0;
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

    private class ScalingCanvasPlugin extends ScalingGraphMousePlugin implements ITopologyCanvasPlugin
    {
        public ScalingCanvasPlugin(ScalingControl scaler, int modifiers)
        {
            super(scaler, modifiers, VisualizationConstants.SCALE_OUT, VisualizationConstants.SCALE_IN);
            setZoomAtMouse(false);
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
                        stateController.zoomOut();
                    } else if (amount < 0)
                    {
                        stateController.zoomIn();
                    }
                } else if (amount > 0)
                {
                    stateController.zoomOut();
                } else if (amount < 0)
                {
                    stateController.zoomIn();
                }

                e.consume();
                vv.repaint();
            }

        }
    }

    @Override
    public void updateInterLayerDistanceInNpCoordinates(int interLayerDistanceInPixels)
    {
        this.currentInterLayerDistanceInNpCoordinates = stateController.getCanvasInterlayerDistance(interLayerDistanceInPixels);
    }

    @Override
    public double getInterLayerDistanceInNpCoordinates()
    {
        return currentInterLayerDistanceInNpCoordinates;
    }
}
