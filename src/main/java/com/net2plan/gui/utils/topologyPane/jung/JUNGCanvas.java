/*******************************************************************************
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Contributors:
 * Pablo Pavon Mariño - initial API and implementation
 ******************************************************************************/


package com.net2plan.gui.utils.topologyPane.jung;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.*;

import javax.swing.ImageIcon;
import javax.swing.JComponent;

import com.net2plan.gui.utils.IVisualizationCallback;
import com.net2plan.gui.utils.topologyPane.*;
import com.net2plan.gui.utils.topologyPane.jung.osmSupport.state.OSMStateManager;
import com.net2plan.interfaces.networkDesign.Node;
import org.apache.commons.collections15.Transformer;
import org.apache.commons.collections15.functors.ConstantTransformer;

import com.net2plan.interfaces.networkDesign.Configuration;
import com.net2plan.internal.CommandLineParser;
import com.net2plan.internal.plugins.ITopologyCanvas;
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
import edu.uci.ics.jung.visualization.control.GraphMousePlugin;
import edu.uci.ics.jung.visualization.control.LayoutScalingControl;
import edu.uci.ics.jung.visualization.control.PluggableGraphMouse;
import edu.uci.ics.jung.visualization.control.ScalingControl;
import edu.uci.ics.jung.visualization.control.ScalingGraphMousePlugin;
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

/**
 * Topology canvas using JUNG library [<a href='#jung'>JUNG</a>].
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @see <a name='jung'></a><a href='http://jung.sourceforge.net/'>Java Universal Network/Graph Framework (JUNG) website</a>
 * @since 0.2.3
 */
@SuppressWarnings("unchecked")
public final class JUNGCanvas implements ITopologyCanvas
{
    private double currentInterLayerDistanceInNpCoordinates;
    private final VisualizationState vs;
    private final Transformer<GUINode, Point2D> transformNetPlanCoordinatesToJungCoordinates;

    private final Graph<GUINode, GUILink> g;
    private final Layout<GUINode, GUILink> l;
    private final VisualizationViewer<GUINode, GUILink> vv;
    private final PluggableGraphMouse gm;
    private final ScalingControl scalingControl;
    private final Transformer<Context<Graph<GUINode, GUILink>, GUILink>, Shape> originalEdgeShapeTransformer;
    private VisualizationServer.Paintable paintableAssociatedToBackgroundImage;

    private final OSMStateManager osmStateManager;

    /**
     * Default constructor.
     *
     * @since 0.2.3
     */
    public JUNGCanvas(IVisualizationCallback callback, TopologyPanel topologyPanel)
    {
        transformNetPlanCoordinatesToJungCoordinates = vertex ->
        {
            final int vlIndex = vertex.getVisualizationOrderRemovingNonVisibleLayers();
            //final double interLayerDistanceInNpCoord = vertex.getVisualizationState().getInterLayerSpaceInNetPlanCoordinates();
            final double interLayerDistanceInNpCoord = currentInterLayerDistanceInNpCoordinates;
            final Point2D basePositionInNetPlanCoord = vertex.getAssociatedNetPlanNode().getXYPositionMap();
            return new Point2D.Double(basePositionInNetPlanCoord.getX(), -(basePositionInNetPlanCoord.getY() + (vlIndex * interLayerDistanceInNpCoord)));
        };

        g = new DirectedOrderedSparseMultigraph<>();
        l = new StaticLayout<>(g, transformNetPlanCoordinatesToJungCoordinates);
        vv = new VisualizationViewer<>(l);

        this.vs = callback.getVisualizationState();

        osmStateManager = new OSMStateManager(callback, topologyPanel, this);

        originalEdgeShapeTransformer = new EdgeShape.QuadCurve<>();
        ((EdgeShape.QuadCurve<GUINode, GUILink>) originalEdgeShapeTransformer).setControlOffsetIncrement(10); // how much they separate from the direct line (default is 20)
        ((EdgeShape.QuadCurve<GUINode, GUILink>) originalEdgeShapeTransformer).setEdgeIndexFunction(DefaultParallelEdgeIndexFunction.<GUINode, GUILink>getInstance()); // how much they separate from the direct line (default is 20)

		/* Customize the graph */
        vv.getRenderContext().setVertexDrawPaintTransformer(n -> n.getDrawPaint());
        vv.getRenderContext().setVertexFillPaintTransformer(n -> n.getFillPaint());
        vv.getRenderContext().setVertexFontTransformer(n -> n.getFont());

		
		/* If icons => comment this line */
        vv.getRenderContext().setVertexShapeTransformer(n -> n.getShape());
        /* If shapes, comment this line */
        //vv.getRenderContext().setVertexIconTransformer(new Transformer<GUINode,Icon> () {} ... )

        /* Convert shape to Icon: http://stackoverflow.com/questions/5449633/convert-shape-object-to-image-object-in-java */
        /* then use only icons. modify the icon to have one with two circles around when selected? the circles are red if the node is failed */

// https://java.net/projects/snat/sources/reposit/content/test/UnicodeLabelDemo.java        
//        ojo!!
//        VertexIconShapeTransformer<Integer> vertexIconShapeFunction =
//        079.
//        new VertexIconShapeTransformer<Integer>(new EllipseVertexShapeTransformer<Integer>());
//        080.
//        DefaultVertexIconTransformer<Integer> vertexIconFunction = new DefaultVertexIconTransformer<Integer>();
//        081.
//        vv.getRenderContext().setVertexShapeTransformer(vertexIconShapeFunction);
//        082.
//        vv.getRenderContext().setVertexIconTransformer(vertexIconFunction);
        
        //http://jung.sourceforge.net/site/jung-samples/xref/edu/uci/ics/jung/samples/DrawnIconVertexDemo.html
        //        vv.getRenderContext().setVertexIconTransformer(new Transformer<Integer,Icon>() {
//        	77  
//        	78          	/*
//        	79          	 * Implements the Icon interface to draw an Icon with background color and
//        	80          	 * a text label
//        	81          	 */
//        	82  			public Icon transform(final Integer v) {
//        	83  				return new Icon() {
//        	84  
//        	85  					public int getIconHeight() {
//        	86  						return 20;
//        	87  					}
//        	88  
//        	89  					public int getIconWidth() {
//        	90  						return 20;
//        	91  					}
//        	92  
//        	93  					public void paintIcon(Component c, Graphics g,
//        	94  							int x, int y) {
//        	95  						if(vv.getPickedVertexState().isPicked(v)) {
//        	96  							g.setColor(Color.yellow);
//        	97  						} else {
//        	98  							g.setColor(Color.red);
//        	99  						}
//        	100 						g.fillOval(x, y, 20, 20);
//        	101 						if(vv.getPickedVertexState().isPicked(v)) {
//        	102 							g.setColor(Color.black);
//        	103 						} else {
//        	104 							g.setColor(Color.white);
//        	105 						}
//        	106 						g.drawString(""+v, x+6, y+15);
//        	107 						
//        	108 					}};
//        	109 			}});
        vv.getRenderContext().setVertexIncludePredicate(guiNodeContext -> vs.isVisible(guiNodeContext.element));
        vv.getRenderer().setVertexLabelRenderer(new NodeLabelRenderer());
        vv.setVertexToolTipTransformer(node -> node.getToolTip());


        vv.getRenderContext().setEdgeIncludePredicate(context -> vs.isVisible(context.element));
        vv.getRenderContext().setEdgeArrowPredicate(context -> vs.isVisible(context.element) && context.element.getHasArrow());
        vv.getRenderContext().setEdgeArrowStrokeTransformer(i -> i.getArrowStroke());
        vv.getRenderContext().setEdgeArrowTransformer(new ConstantTransformer(ArrowFactory.getNotchedArrow(7, 10, 5)));
        vv.getRenderContext().setEdgeLabelClosenessTransformer(new ConstantDirectionalEdgeValueTransformer(.6, .6));
        vv.getRenderContext().setEdgeStrokeTransformer(i -> i.getEdgeStroke());

        vv.getRenderContext().setEdgeDrawPaintTransformer(e -> e.getEdgeDrawPaint());
        vv.getRenderContext().setArrowDrawPaintTransformer(e -> e.getArrowDrawPaint());
        vv.getRenderContext().setArrowFillPaintTransformer(e -> e.getArrowFillPaint());

        vv.getRenderContext().setEdgeLabelRenderer(new DefaultEdgeLabelRenderer(Color.BLUE));
        vv.getRenderer().setEdgeLabelRenderer(new BasicEdgeLabelRenderer<GUINode, GUILink>()
        {
            public void labelEdge(RenderContext<GUINode, GUILink> rc, Layout<GUINode, GUILink> layout, GUILink e, String label)
            {
                if (vs.isShowLinkLabels()) super.labelEdge(rc, layout, e, e.getLabel());
            }
        });
        vv.setEdgeToolTipTransformer(link -> link.getToolTip());
//        vv.getRenderContext().setEdgeShapeTransformer(c ->
//                {
//                    final GUINode origin = c.element.getOriginNode();
//                    final GUINode destination = c.element.getDestinationNode();
//                    boolean separateTheLinks = vv.getPickedVertexState().isPicked(origin) || vv.getPickedVertexState().isPicked(destination);
//                    if (!separateTheLinks)
//                    {
//                        Set<GUILink> linksNodePair = new HashSet<>(c.graph.getIncidentEdges(destination));
//                        linksNodePair.retainAll(c.graph.getIncidentEdges(origin));
//                        for (GUILink e : linksNodePair)
//                            if (vv.getPickedEdgeState().isPicked(e) || !e.getAssociatedNetPlanLink().isUp())
//                            {
//                                separateTheLinks = true;
//                                break;
//                            }
//                    }
//                    return separateTheLinks ? originalEdgeShapeTransformer.transform(c) : new Line2D.Float(0.0f, 0.0f, 1.0f, 0.0f);
//                }
//        );

        vv.getRenderContext().setEdgeShapeTransformer(c -> c.element.isShownSeparated() ? originalEdgeShapeTransformer.transform(c) : new Line2D.Float(0.0f, 0.0f, 1.0f, 0.0f));

        // Background controller
        this.paintableAssociatedToBackgroundImage = null;

        gm = new PluggableGraphMouse();
        vv.setGraphMouse(gm);

        scalingControl = new LayoutScalingControl();
        ITopologyCanvasPlugin scalingPlugin = new ScalingCanvasPlugin(scalingControl, MouseEvent.NOBUTTON);
        addPlugin(scalingPlugin);

        vv.setOpaque(false);
        vv.setBackground(new Color(0, 0, 0, 0));

        this.updateInterLayerDistanceInNpCoordinates(vs.getInterLayerSpaceInPixels());

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

    /**
     * Converts a point from the SWING coordinates system into a point from the JUNG coordinates system.
     *
     * @param jungLayoutCoord (@code Point2D) on the SWING canvas.
     * @return (@code Point2D) on the JUNG canvas.
     */
    @Override
    public Point2D getCanvasPointFromNetPlanPoint(Point2D screenPoint)
    {
        Point2D layoutOrViewCoordinates = vv.getRenderContext().getMultiLayerTransformer().inverseTransform(Layer.LAYOUT, screenPoint);
        layoutOrViewCoordinates.setLocation(layoutOrViewCoordinates.getX(), -layoutOrViewCoordinates.getY());

        return layoutOrViewCoordinates;
    }

    public void resetTransformer()
    {
        vv.getRenderContext().getMultiLayerTransformer().setToIdentity();
    }

    @Override
    public Point2D getCanvasPointFromScreenPoint(Point2D netPlanPoint)
    {
        return vv.getRenderContext().getMultiLayerTransformer().inverseTransform(Layer.LAYOUT, netPlanPoint);
    }

    public Rectangle getCurrentCanvasViewWindow()
    {
        return vv.getRenderContext().getMultiLayerTransformer().inverseTransform(vv.getBounds()).getBounds();
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

    @Override
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
    public void resetPickedStateAndRefresh()
    {
        vv.getPickedVertexState().clear();
        vv.getPickedEdgeState().clear();
        refresh();
    }

    @Override
    public void rebuildCanvasGraphAndRefresh()
    {
        for (GUILink gl : new ArrayList<>(g.getEdges()))
            g.removeEdge(gl);
        for (GUINode gn : new ArrayList<>(g.getVertices()))
            g.removeVertex(gn);
        for (GUINode gn : vs.getAllGUINodes()) g.addVertex(gn);
        for (GUILink gl : vs.getAllGUILinks(true, true))
            g.addEdge(gl, gl.getOriginNode(), gl.getDestinationNode());
        refresh();
    }

    @Override
    public void zoomAll()
    {
        osmStateManager.zoomAll();
    }

    @Override
    public void updateAllVerticesXYPosition()
    {
        osmStateManager.updateNodesXYPosition();
    }

    @Override
    public void moveVertexToXYPosition(GUINode npNode, Point2D point)
    {
        l.setLocation(npNode, point);
    }

    @Override
    public void panTo(Point2D initialPoint, Point2D destinationPoint)
    {
        osmStateManager.panTo(initialPoint, destinationPoint);
    }

    @Override
    public void addNode(Point2D position)
    {
        osmStateManager.addNode(position);
    }

    @Override
    public void removeNode(Node node)
    {
        osmStateManager.removeNode(node);
    }

    @Override
    public void runOSMSupport()
    {
        osmStateManager.setRunningState();
    }

    @Override
    public void stopOSMSupport()
    {
        osmStateManager.setStoppedState();
    }

    @Override
    public boolean isOSMRunning()
    {
        return osmStateManager.isMapActivated();
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
        osmStateManager.zoomIn();
    }

    @Override
    public void zoomOut()
    {
        osmStateManager.zoomOut();
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

    public void setBackgroundImage(final File bgFile, final double x, final double y)
    {
        final Double x1 = x;
        final Double y1 = y;

        setBackgroundImage(bgFile, x1.intValue(), y1.intValue());
    }

    public void setBackgroundImage(final File bgFile, final int x, final int y)
    {
        final ImageIcon background = new ImageIcon(bgFile.getAbsolutePath());
        updateBackgroundImage(background, x, y);
    }

    public void setBackgroundImage(final ImageIcon image, final int x, final int y)
    {
        updateBackgroundImage(image, x, y);
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
        osmStateManager.takeSnapshot();
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

            if (vs.isShowNodeNames())
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
                        osmStateManager.zoomOut();
                    } else if (amount < 0)
                    {
                        osmStateManager.zoomIn();
                    }
                } else if (amount > 0)
                {
                    osmStateManager.zoomOut();
                } else if (amount < 0)
                {
                    osmStateManager.zoomIn();
                }

                e.consume();
                vv.repaint();
            }

        }
    }

    @Override
    public void updateInterLayerDistanceInNpCoordinates(int interLayerDistanceInPixels)
    {
        this.currentInterLayerDistanceInNpCoordinates = osmStateManager.getCanvasInterlayerDistance(interLayerDistanceInPixels);
    }

    @Override
    public double getInterLayerDistanceInNpCoordinates()
    {
        return currentInterLayerDistanceInNpCoordinates;
    }
}
