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


package com.net2plan.gui.plugins.networkDesign.topologyPane.jung.plugins;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.gui.plugins.networkDesign.interfaces.ITopologyCanvas;
import com.net2plan.gui.plugins.networkDesign.interfaces.ITopologyCanvasPlugin;
import com.net2plan.gui.plugins.networkDesign.topologyPane.jung.GUILink;
import com.net2plan.gui.plugins.networkDesign.topologyPane.jung.GUINode;
import com.net2plan.gui.plugins.networkDesign.visualizationControl.PickManager;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.niw.WAbstractNetworkElement;
import com.net2plan.niw.WNet;
import com.net2plan.niw.WNetConstants;
import com.net2plan.niw.WNode;
import com.net2plan.utils.Pair;

import edu.uci.ics.jung.visualization.VisualizationServer.Paintable;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.util.ArrowFactory;

/**
 * Plugin that allows to add new links graphically over the canvas.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
@SuppressWarnings("unchecked")
public class AddLinkGraphPlugin extends MouseAdapter implements ITopologyCanvasPlugin
{
    private GUINetworkDesign callback;
    private GUINode startVertex;
    private Paintable edgePaintable;
    private Paintable arrowPaintable;
    private CubicCurve2D rawEdge;
    private Shape edgeShape;
    private Shape rawArrowShape;
    private Shape arrowShape;
    private int modifiers, modifiersBidirectional;
    private Point down;
    private ITopologyCanvas canvas;

    /**
     * Default constructor. By default the modifier to add unidirectional links
     * is the left button of the mouse, while to add bidirectional links required
     * to hold the SHIFT key also.
     *
     * @param callback Topology callback
     * @since 0.2.0
     */
    public AddLinkGraphPlugin(GUINetworkDesign callback , ITopologyCanvas canvas) {
        this(callback, canvas , MouseEvent.BUTTON1_MASK, MouseEvent.BUTTON1_MASK | MouseEvent.SHIFT_MASK);
    }

    /**
     * Constructor that allows to set the modifiers to activate both
     * 'add unidirectional link' and 'add bidirectional link' modes.
     *
     * @param callback               Topology callback
     * @param modifiers              Modifier to activate the plugin to add unidirectional links
     * @param modifiersBidirectional Modifier to activate the plugin to add bidirectional links
     * @since 0.2.0
     */
    public AddLinkGraphPlugin(GUINetworkDesign callback, ITopologyCanvas canvas , int modifiers, int modifiersBidirectional) {
        setModifiers(modifiers);
        setModifiersBidirectional(modifiersBidirectional);

        this.callback = callback;
        down = null;
        startVertex = null;
        rawEdge = new CubicCurve2D.Float();
        rawEdge.setCurve(0.0f, 0.0f, 0.33f, 100, .66f, -50, 1.0f, 0.0f);
        rawArrowShape = ArrowFactory.getNotchedArrow(20, 16, 8);
        edgePaintable = new EdgePaintable();
        arrowPaintable = new ArrowPaintable();
        this.canvas = canvas;
    }

    @Override
    public boolean checkModifiers(MouseEvent e) {
        return (e.getModifiers() == modifiers) || e.getModifiers() == modifiersBidirectional;
    }

    @Override
    public int getModifiers() {
        return modifiers;
    }

    @Override
    public void mouseClicked(MouseEvent e) 
    {
    	/*final PickManager pickManager = callback.getPickManager();
        if (checkModifiers(e)) {
            GUINode guiNode = canvas.getVertex(e);
            Node node = guiNode == null? null : guiNode.getAssociatedNode();
            if (node != null) {
                pickManager.pickElements(pickManager.new PickStateInfo (node , Optional.empty()));
                e.consume();
            } else {
                GUILink link = canvas.getEdge(e);
                if (link != null) 
                {
                	if (!link.isIntraNodeLink())
                	{ 
                        pickManager.pickElements(pickManager.new PickStateInfo (link.getAssociatedNetPlanLink() , Optional.empty()));
                	}
                    e.consume();
                }
            }
            callback.updateVisualizationAfterPick();
        }*/
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (startVertex != null) {
            VisualizationViewer<GUINode, GUILink> vv = (VisualizationViewer<GUINode, GUILink>) e.getSource();
            transformArrowShape(down, e.getPoint());
            transformEdgeShape(down, e.getPoint());
            e.consume();
            vv.repaint();
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (checkModifiers(e)) {
            startVertex = null;
            down = null;

            GUINode guiNode = canvas.getVertex(e);
            Node node = guiNode == null? null : guiNode.getAssociatedNode();
            if (node != null) {
                final VisualizationViewer<GUINode, GUILink> vv = (VisualizationViewer<GUINode, GUILink>) e.getSource();

                startVertex = guiNode;
                down = e.getPoint();
                transformEdgeShape(down, down);
                vv.addPostRenderPaintable(edgePaintable);
                transformArrowShape(down, e.getPoint());
                vv.addPostRenderPaintable(arrowPaintable);
                e.consume();
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) 
    {
    	if (startVertex != null) 
        {
            final VisualizationViewer<GUINode, GUILink> vv = (VisualizationViewer<GUINode, GUILink>) e.getSource();
            vv.removePostRenderPaintable(edgePaintable);
            vv.removePostRenderPaintable(arrowPaintable);

            final GUINode guiNode = canvas.getVertex(e);
            final Node node = guiNode == null? null : guiNode.getAssociatedNode();
            if (node != null && startVertex.getAssociatedNode() != node)
            {
            	if (guiNode.getLayer() == startVertex.getLayer ())
    			{
        			final boolean isBidirectional = (e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) == MouseEvent.SHIFT_DOWN_MASK;
                    final boolean isNiwOk = callback.getVisualizationState().isNiwDesignButtonActive() && callback.isNiwValidCurrentDesign();
                    final NetworkLayer layer = callback.getDesign().getNetworkLayerDefault();
        			final Link linkToPick;
                    if (isNiwOk)
                    {
                    	final WNet wNet = callback.getNiwInfo().getSecond();
                    	final Function<NetworkLayer,Boolean> isIp = d -> { final WAbstractNetworkElement ee = wNet.getWElement(d).orElse(null); return ee == null? false : ee.isLayerIp(); };
                    	final Function<NetworkLayer,Boolean> isWdm = d -> { final WAbstractNetworkElement ee = wNet.getWElement(d).orElse(null); return ee == null? false : ee.isLayerWdm(); };
                    	if (isIp.apply(layer))
                    		linkToPick = wNet.addIpLinkBidirectional(new WNode(startVertex.getAssociatedNode()), new WNode (node), 100.0).getFirst().getNe();
                    	else if (isWdm.apply(layer))
                    		linkToPick = wNet.addFiber(new WNode(startVertex.getAssociatedNode()), new WNode (node), WNetConstants.WFIBER_DEFAULT_VALIDOPTICALSLOTRANGES, -1.0, isBidirectional).getFirst().getNe();
                    	else linkToPick = null;
                    }
                    else
                    {
                        if (isBidirectional) 
                        	linkToPick = node.getNetPlan().addLinkBidirectional(startVertex.getAssociatedNode(), node,0,0,200000,null).getFirst(); 
                        else
                        	linkToPick = node.getNetPlan().addLink(startVertex.getAssociatedNode(), node,0,0,200000,null);
                    }

                    callback.setDesignAndCallWhatIfSomethingModified(callback.getDesign());
                    callback.getVisualizationState().recomputeCanvasTopologyBecauseOfLinkOrNodeAdditionsOrRemovals(); // implies a reset picked
                    callback.updateVisualizationAfterChanges();
                    callback.addNetPlanChange();
                    if (linkToPick != null) callback.getPickManager().pickElements(linkToPick);
                    callback.updateVisualizationAfterPick();
    			}
                //if (node == startVertex.getAssociatedNode()) callback.resetPickedStateAndUpdateView();
            }

            startVertex = null;
            down = null;
            //callback.resetPickedStateAndUpdateView();
            vv.repaint();
        }
    }

    @Override
    public void setModifiers(int modifiers) {
        this.modifiers = modifiers;
    }

    /**
     * Returns the modifier to add bidirectional links.
     *
     * @return Modifier for bidirectional links
     * @since 0.3.1
     */
    public int getModifiersBidirectional() {
        return modifiersBidirectional;
    }

    /**
     * Sets the modifier to add bidirectional links.
     *
     * @param modifiersBidirectional Modifier for bidirectional links
     * @since 0.3.1
     */
    public void setModifiersBidirectional(int modifiersBidirectional) {
        this.modifiersBidirectional = modifiersBidirectional;
    }

    private void transformArrowShape(Point2D down, Point2D out) {
        float x1 = (float) down.getX();
        float y1 = (float) down.getY();
        float x2 = (float) out.getX();
        float y2 = (float) out.getY();

        AffineTransform xform = AffineTransform.getTranslateInstance(x2, y2);

        float dx = x2 - x1;
        float dy = y2 - y1;
        float thetaRadians = (float) Math.atan2(dy, dx);
        xform.rotate(thetaRadians);
        arrowShape = xform.createTransformedShape(rawArrowShape);
    }

    private void transformEdgeShape(Point2D down, Point2D out) {
        float x1 = (float) down.getX();
        float y1 = (float) down.getY();
        float x2 = (float) out.getX();
        float y2 = (float) out.getY();

        AffineTransform xform = AffineTransform.getTranslateInstance(x1, y1);

        float dx = x2 - x1;
        float dy = y2 - y1;
        float thetaRadians = (float) Math.atan2(dy, dx);
        xform.rotate(thetaRadians);
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        xform.scale(dist / rawEdge.getBounds().getWidth(), 1.0);
        edgeShape = xform.createTransformedShape(rawEdge);
    }

    private class ArrowPaintable implements Paintable {
        @Override
        public void paint(Graphics g) {
            if (!(g instanceof Graphics2D)) {
                throw new IllegalArgumentException("A Graphics2D object is required");
            }

            if (arrowShape != null) {
                Color oldColor = g.getColor();
                g.setColor(Color.black);
                ((Graphics2D) g).fill(arrowShape);
                g.setColor(oldColor);
            }
        }

        @Override
        public boolean useTransform() {
            return false;
        }
    }

    private class EdgePaintable implements Paintable {
        @Override
        public void paint(Graphics g) {
            if (!(g instanceof Graphics2D)) {
                throw new IllegalArgumentException("A Graphics2D object is required");
            }

            if (edgeShape != null) {
                Color oldColor = g.getColor();
                g.setColor(Color.black);
                ((Graphics2D) g).draw(edgeShape);
                g.setColor(oldColor);
            }
        }

        @Override
        public boolean useTransform() {
            return false;
        }
    }
}
