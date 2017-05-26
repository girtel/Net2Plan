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
package com.net2plan.gui.plugins.networkDesign.topologyPane;

import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.gui.plugins.networkDesign.interfaces.ITopologyCanvas;
import com.net2plan.gui.plugins.networkDesign.visualizationControl.VisualizationConstants;
import com.net2plan.gui.plugins.networkDesign.visualizationControl.VisualizationState;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * @author Jorge San Emeterio
 * @date 25/04/17
 */
public class TopologySideBar extends JPanel implements ActionListener
{
    private final GUINetworkDesign callback;
    private final TopologyPanel topologyPanel;
    private final ITopologyCanvas canvas;

    private final JToggleButton btn_multilayer;
    private final JToolBar layerToolBar;
    private final JButton btn_increaseInterLayerDistance, btn_decreaseInterLayerDistance;
    private final JButton btn_npChangeUndo, btn_npChangeRedo;
    private final JToggleButton btn_showLowerLayerInfo, btn_showUpperLayerInfo, btn_showThisLayerInfo;

    private final MultiLayerControlPanel multilayerControlPanel;

    private final JDialog dialog;

    public TopologySideBar(GUINetworkDesign callback, TopologyPanel topologyPanel, ITopologyCanvas canvas)
    {
        super();

        this.callback = callback;
        this.topologyPanel = topologyPanel;
        this.canvas = canvas;

        this.setLayout(new BorderLayout());

        this.layerToolBar = new JToolBar();
        this.layerToolBar.setOrientation(JToolBar.VERTICAL);
        this.layerToolBar.setRollover(true);
        this.layerToolBar.setFloatable(false);
        this.layerToolBar.setOpaque(false);

        this.multilayerControlPanel = new MultiLayerControlPanel(callback);
        this.multilayerControlPanel.setBorder(BorderFactory.createTitledBorder("Layers"));

        this.dialog = new JDialog();
        this.dialog.setModal(false);
        this.dialog.setType(JFrame.Type.UTILITY);
        this.dialog.setLayout(new BorderLayout());
        this.dialog.add(multilayerControlPanel);
        this.dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        this.dialog.setResizable(false);
        this.dialog.setUndecorated(true);
        this.dialog.setAlwaysOnTop(true);
        this.dialog.addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentHidden(ComponentEvent componentEvent)
            {
                btn_multilayer.setSelected(false);
            }
        });
        this.dialog.addMouseMotionListener(new MouseMotionListener() {
            private int mx, my;

            @Override
            public void mouseMoved(MouseEvent e) {
                mx = e.getXOnScreen();
                my = e.getYOnScreen();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                Point p = dialog.getLocation();
                p.x += e.getXOnScreen() - mx;
                p.y += e.getYOnScreen() - my;
                mx = e.getXOnScreen();
                my = e.getYOnScreen();
                dialog.setLocation(p);
            }
        });
        this.dialog.addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosed(WindowEvent windowEvent)
            {
                btn_multilayer.setSelected(false);
            }
        });

        /* Multilayer buttons */
        this.btn_increaseInterLayerDistance = new JButton();
        this.btn_increaseInterLayerDistance.setToolTipText("Increase the distance between layers (when more than one layer is visible)");
        this.btn_decreaseInterLayerDistance = new JButton();
        this.btn_decreaseInterLayerDistance.setToolTipText("Decrease the distance between layers (when more than one layer is visible)");
        this.btn_showLowerLayerInfo = new JToggleButton();
        this.btn_showLowerLayerInfo.setToolTipText("Shows the links in lower layers that carry traffic of the picked element");
        this.btn_showLowerLayerInfo.setSelected(callback.getVisualizationState().isShowInCanvasLowerLayerPropagation());
        this.btn_showUpperLayerInfo = new JToggleButton();
        this.btn_showUpperLayerInfo.setToolTipText("Shows the links in upper layers that carry traffic that appears in the picked element");
        this.btn_showUpperLayerInfo.setSelected(callback.getVisualizationState().isShowInCanvasUpperLayerPropagation());
        this.btn_showThisLayerInfo = new JToggleButton();
        this.btn_showThisLayerInfo.setToolTipText("Shows the links in the same layer as the picked element, that carry traffic that appears in the picked element");
        this.btn_showThisLayerInfo.setSelected(callback.getVisualizationState().isShowInCanvasThisLayerPropagation());
        this.btn_npChangeUndo = new JButton();
        this.btn_npChangeUndo.setToolTipText("Navigate back to the previous state of the network (last time the network design was changed)");
        this.btn_npChangeRedo = new JButton();
        this.btn_npChangeRedo.setToolTipText("Navigate forward to the next state of the network (when network design was changed");
        this.btn_multilayer = new JToggleButton();
        this.btn_multilayer.setToolTipText("Show layer control table");

        this.btn_increaseInterLayerDistance.setIcon(new ImageIcon(TopologyPanel.class.getResource("/resources/gui/increaseLayerDistance.png")));
        this.btn_decreaseInterLayerDistance.setIcon(new ImageIcon(TopologyPanel.class.getResource("/resources/gui/decreaseLayerDistance.png")));
        this.btn_showThisLayerInfo.setIcon(new ImageIcon(TopologyPanel.class.getResource("/resources/gui/showLayerPropagation.png")));
        this.btn_showUpperLayerInfo.setIcon(new ImageIcon(TopologyPanel.class.getResource("/resources/gui/showLayerUpperPropagation.png")));
        this.btn_showLowerLayerInfo.setIcon(new ImageIcon(TopologyPanel.class.getResource("/resources/gui/showLayerLowerPropagation.png")));
        this.btn_npChangeUndo.setIcon(new ImageIcon(TopologyPanel.class.getResource("/resources/gui/undoButton.png")));
        this.btn_npChangeRedo.setIcon(new ImageIcon(TopologyPanel.class.getResource("/resources/gui/redoButton.png")));
        this.btn_multilayer.setIcon(new ImageIcon(TopologyPanel.class.getResource("/resources/gui/showLayerControl.png")));

        this.btn_increaseInterLayerDistance.addActionListener(this);
        this.btn_decreaseInterLayerDistance.addActionListener(this);
        this.btn_showLowerLayerInfo.addActionListener(this);
        this.btn_showUpperLayerInfo.addActionListener(this);
        this.btn_showThisLayerInfo.addActionListener(this);
        this.btn_npChangeUndo.addActionListener(this);
        this.btn_npChangeRedo.addActionListener(this);
        this.btn_multilayer.addActionListener(this);

        this.layerToolBar.add(btn_multilayer);
        this.layerToolBar.add(btn_increaseInterLayerDistance);
        this.layerToolBar.add(btn_decreaseInterLayerDistance);
        this.layerToolBar.add(btn_showLowerLayerInfo);
        this.layerToolBar.add(btn_showUpperLayerInfo);
        this.layerToolBar.add(btn_showThisLayerInfo);
        this.layerToolBar.add(Box.createVerticalGlue());
        //multiLayerToolbar.add(btn_npChangeUndo);
        //multiLayerToolbar.add(btn_npChangeRedo);

        this.add(layerToolBar, BorderLayout.WEST);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        Object src = e.getSource();
        final VisualizationState vs = callback.getVisualizationState();
        if (src == btn_increaseInterLayerDistance)
        {
            if (vs.getCanvasNumberOfVisibleLayers() == 1) return;

            final int currentInterLayerDistance = vs.getInterLayerSpaceInPixels();
            final int newInterLayerDistance = currentInterLayerDistance + (int) Math.ceil(currentInterLayerDistance * (VisualizationConstants.SCALE_IN - 1));

            vs.setInterLayerSpaceInPixels(newInterLayerDistance);
            canvas.updateInterLayerDistanceInNpCoordinates(newInterLayerDistance);
            canvas.updateAllVerticesXYPosition();
            canvas.refresh();
        } else if (src == btn_decreaseInterLayerDistance)
        {
            if (vs.getCanvasNumberOfVisibleLayers() == 1) return;

            final int currentInterLayerDistance = vs.getInterLayerSpaceInPixels();
            int newInterLayerDistance = currentInterLayerDistance - (int) Math.ceil(currentInterLayerDistance * (1 - VisualizationConstants.SCALE_OUT));

            if (newInterLayerDistance <= 0)
                newInterLayerDistance = 1;

            vs.setInterLayerSpaceInPixels(newInterLayerDistance);
            canvas.updateInterLayerDistanceInNpCoordinates(newInterLayerDistance);
            canvas.updateAllVerticesXYPosition();

            canvas.refresh();
        } else if (src == btn_showLowerLayerInfo)
        {
            vs.setShowInCanvasLowerLayerPropagation(btn_showLowerLayerInfo.isSelected());
            canvas.refresh();
        } else if (src == btn_showUpperLayerInfo)
        {
            vs.setShowInCanvasUpperLayerPropagation(btn_showUpperLayerInfo.isSelected());
            canvas.refresh();
        } else if (src == btn_showThisLayerInfo)
        {
            vs.setShowInCanvasThisLayerPropagation(btn_showThisLayerInfo.isSelected());
            canvas.refresh();
        } else if (src == btn_npChangeUndo)
        {
            callback.requestUndoAction();
        } else if (src == btn_npChangeRedo)
        {
            callback.requestRedoAction();
        } else if (src == btn_multilayer)
        {
            dialog.pack();
            dialog.setVisible(btn_multilayer.isSelected());

            dialog.validate();
            dialog.repaint();
        }
    }

    public void refresh()
    {
        multilayerControlPanel.refreshTable();

        dialog.pack();
        dialog.validate();
        dialog.repaint();
    }
}
