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


package com.net2plan.gui.utils.topologyPane;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.LinkedList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import com.google.common.collect.Sets;
import com.net2plan.gui.utils.IVisualizationCallback;
import com.net2plan.gui.utils.topologyPane.visualizationControl.VisualizationState;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.internal.Constants.NetworkElementType;
import com.net2plan.internal.plugins.ITopologyCanvas;

/**
 * Plugin for the popup menu of the canvas.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.3.1
 */
public class PopupMenuPlugin extends MouseAdapter implements ITopologyCanvasPlugin 
{
    private final IVisualizationCallback callback;
    private final ITopologyCanvas canvas;

    /**
     * Default constructor.
     *
     * @param callback Reference to the class handling change events.
     * @since 0.3.1
     */
    public PopupMenuPlugin(IVisualizationCallback callback , ITopologyCanvas canvas)
    {
        this.callback = callback;
        this.canvas = canvas;
    }

    @Override
    public boolean checkModifiers(MouseEvent e) {
        return e.isPopupTrigger();
    }

    @Override
    public int getModifiers() {
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    public void mouseReleased(MouseEvent e) 
    {
        if (checkModifiers(e)) 
        {
            final Point p = e.getPoint();
            final Point2D positionInNetPlanCoordinates = canvas.getCanvasPointFromNetPlanPoint(p);
            final GUINode gn = canvas.getVertex(e);
            final Node node = gn == null ? null : gn.getAssociatedNetPlanNode();
            final GUILink gl = canvas.getEdge(e);
            final Link link = gl == null ? null : gl.isIntraNodeLink()? null : gl.getAssociatedNetPlanLink();

            List<JComponent> actions;
            if (node != null) {
                actions = getNodeActions(node, positionInNetPlanCoordinates);
            } else if (link != null) {
                actions = getLinkActions(link, positionInNetPlanCoordinates);
            } else {
            	callback.resetPickedStateAndUpdateView();
                actions = getCanvasActionsMouseInNoNodeNorLinkPoint(positionInNetPlanCoordinates);
            }

            if (actions == null || actions.isEmpty()) return;

            final JPopupMenu popup = new JPopupMenu();
            for (JComponent action : actions)
                popup.add(action);

            popup.show(canvas.getCanvasComponent(), e.getX(), e.getY());
            e.consume();
        }
    }

    @Override
    public void setModifiers(int modifiers) 
    {
        throw new UnsupportedOperationException("Not supported yet");
    }

    private List<JComponent> getNodeActions(Node node , Point2D pos)
    {
        final List<JComponent> actions = new LinkedList<JComponent>();
        final VisualizationState vs = callback.getVisualizationState();
        if (!vs.isNetPlanEditable()) return actions;
        if (vs.isWhatIfAnalysisActive()) return actions;
        if (callback.inOnlineSimulationMode()) return actions;
        
    	final NetPlan netPlan = callback.getDesign();
        actions.add(new JMenuItem(new RemoveNodeAction("Remove node", node)));

        if (netPlan.getNumberOfNodes() > 1)
        {
            actions.add(new JPopupMenu.Separator());
            JMenu unidirectionalMenu = new JMenu("Create unidirectional link");
            JMenu bidirectionalMenu = new JMenu("Create bidirectional link");

            String nodeName = node.getName() == null ? "" : node.getName();
            String nodeString = Long.toString(node.getId()) + (nodeName.isEmpty() ? "" : " (" + nodeName + ")");

            final NetworkLayer layer = netPlan.getNetworkLayerDefault();
            for (Node auxNode : netPlan.getNodes())
            {
                if (auxNode == node) continue;

                String auxNodeName = auxNode.getName() == null ? "" : auxNode.getName();
                String auxNodeString = Long.toString(auxNode.getId()) + (auxNodeName.isEmpty() ? "" : " (" + auxNodeName + ")");

                AbstractAction unidirectionalAction = new AddLinkAction(nodeString + " => " + auxNodeString, layer, node, auxNode);
                unidirectionalMenu.add(unidirectionalAction);

                AbstractAction bidirectionalAction = new AddLinkBidirectionalAction(nodeString + " <=> " + auxNodeString, layer, node, auxNode);
                bidirectionalMenu.add(bidirectionalAction);
            }

            actions.add(unidirectionalMenu);
            actions.add(bidirectionalMenu);
        }
        return actions;
    }

    private List<JComponent> getLinkActions(Link link, Point2D pos)
    {
        List<JComponent> actions = new LinkedList<JComponent>();
        final VisualizationState vs = callback.getVisualizationState();
        if (!vs.isNetPlanEditable()) return actions;
        if (vs.isWhatIfAnalysisActive()) return actions;
        if (callback.inOnlineSimulationMode()) return actions;

        actions.add(new JMenuItem(new RemoveLinkAction("Remove link", link)));

        return actions;
    }

    private class AddLinkAction extends AbstractAction
    {
        private final NetworkLayer layer;
        private final Node originNode;
        private final Node destinationNode;

        public AddLinkAction(String name, NetworkLayer layer, Node originNode, Node destinationNode)
        {
            super(name);
            this.layer = layer;
            this.originNode = originNode;
            this.destinationNode = destinationNode;
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
        	originNode.getNetPlan().addLink(originNode , destinationNode , 0 , 0 , 200000 , null , layer);
        	callback.getVisualizationState().recomputeCanvasTopologyBecauseOfLinkOrNodeAdditionsOrRemovals();
            callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.LINK));
            callback.getUndoRedoNavigationManager().addNetPlanChange();
        }
    }

    private class AddLinkBidirectionalAction extends AbstractAction
    {
        private final NetworkLayer layer;
        private final Node originNode;
        private final Node destinationNode;

        public AddLinkBidirectionalAction(String name, NetworkLayer layer, Node originNode, Node destinationNode)
        {
            super(name);
            this.layer = layer;
            this.originNode = originNode;
            this.destinationNode = destinationNode;
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
        	originNode.getNetPlan().addLinkBidirectional(originNode , destinationNode , 0 , 0 , 200000 , null , layer);
        	callback.getVisualizationState().recomputeCanvasTopologyBecauseOfLinkOrNodeAdditionsOrRemovals();
            callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.NODE));
            callback.getUndoRedoNavigationManager().addNetPlanChange();
        }

    }

    public List<JComponent> getCanvasActionsMouseInNoNodeNorLinkPoint(Point2D positionInNetPlanCoordinates)
    {
        final List<JComponent> actions = new LinkedList<>();
        final VisualizationState vs = callback.getVisualizationState();
        if (!vs.isNetPlanEditable()) return actions;
        if (vs.isWhatIfAnalysisActive()) return actions;
        if (callback.inOnlineSimulationMode()) return actions;
        
        
        JMenuItem addNode = new JMenuItem(new AddNodeAction("Add node here", positionInNetPlanCoordinates));
        actions.add(addNode);

        actions.add(new JPopupMenu.Separator());

        JMenu topologySettingMenu = new JMenu("Change topology layout");

        JMenuItem circularSetting = new JMenuItem("Circular");
        circularSetting.addActionListener(e -> 
        {
        	final List<Node> nodes = callback.getDesign().getNodes();
        	final double angStep = 360.0 / nodes.size(); 
        	final double radius = 10; // PABLO: THIS SHOUD BE SET IN OTHER COORDINATES?
            for (int i = 0; i < nodes.size(); i++)
            	nodes.get(i).setXYPositionMap(new Point2D.Double(positionInNetPlanCoordinates.getX() + radius * Math.cos(Math.toRadians(angStep*i)) , positionInNetPlanCoordinates.getY() + radius * Math.sin(Math.toRadians(angStep*i))));
        	callback.getVisualizationState().recomputeCanvasTopologyBecauseOfLinkOrNodeAdditionsOrRemovals();
            callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.NODE));
            callback.runCanvasOperation(ITopologyCanvas.CanvasOperation.ZOOM_ALL);
            callback.getUndoRedoNavigationManager().addNetPlanChange();
         });

        topologySettingMenu.add(circularSetting);

        actions.add(topologySettingMenu);

        return actions;
    }

    private class RemoveNodeAction extends AbstractAction
    {
        private final Node node;

        public RemoveNodeAction(String name, Node node)
        {
            super(name);
            this.node = node;
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            canvas.removeNode(node);
        }
    }

    private class AddNodeAction extends AbstractAction
    {
        private final Point2D positionInNetPlanCoordinates;

        public AddNodeAction(String name, Point2D positionInNetPlanCoordinates)
        {
            super(name);
            this.positionInNetPlanCoordinates = positionInNetPlanCoordinates;
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            canvas.addNode(positionInNetPlanCoordinates);
        }
    }

    private class RemoveLinkAction extends AbstractAction
    {
        private final Link link;

        public RemoveLinkAction(String name, Link link)
        {
            super(name);
            this.link = link;
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            link.remove();
        	callback.getVisualizationState().recomputeCanvasTopologyBecauseOfLinkOrNodeAdditionsOrRemovals();
        	callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.LINK));
        	callback.getUndoRedoNavigationManager().addNetPlanChange();
        }
    }

}
