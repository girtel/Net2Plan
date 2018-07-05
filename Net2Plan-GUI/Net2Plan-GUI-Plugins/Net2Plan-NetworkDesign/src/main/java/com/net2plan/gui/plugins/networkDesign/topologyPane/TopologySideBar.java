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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.ScrollPaneLayout;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;

import org.apache.commons.collections15.BidiMap;
import org.apache.commons.collections15.bidimap.DualHashBidiMap;

import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.gui.plugins.networkDesign.interfaces.ITopologyCanvas;
import com.net2plan.gui.plugins.networkDesign.visualizationControl.VisualizationConstants;
import com.net2plan.gui.plugins.networkDesign.visualizationControl.VisualizationState;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkLayer;

import net.miginfocom.swing.MigLayout;


public class TopologySideBar extends JPanel implements ActionListener
{
    private final GUINetworkDesign callback;
    private final TopologyPanel topologyPanel;
    private final ITopologyCanvas canvas;

    private final JToolBar layerToolBar;
    private final JButton btn_increaseInterLayerDistance, btn_decreaseInterLayerDistance, btn_moveUpLayer, btn_moveDownLayer;
    private final JButton btn_npChangeUndo, btn_npChangeRedo;
    private final JToggleButton btn_showLowerLayerInfo, btn_showUpperLayerInfo, btn_showThisLayerInfo;
    private final JPanel layersPanel;

    public TopologySideBar(GUINetworkDesign callback, TopologyPanel topologyPanel, ITopologyCanvas canvas)
    {
        super();

        this.callback = callback;
        this.topologyPanel = topologyPanel;
        this.canvas = canvas;

        this.setLayout(new BorderLayout());

        this.layerToolBar = new JToolBar();
        this.layerToolBar.setLayout(new MigLayout("insets 0 0 0 0, fillx, gap 0, wrap 1"));
        this.layerToolBar.setOrientation(JToolBar.VERTICAL);
        this.layerToolBar.setRollover(true);
        this.layerToolBar.setFloatable(false);
        this.layerToolBar.setOpaque(false);


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

        this.btn_increaseInterLayerDistance.setIcon(new ImageIcon(TopologyPanel.class.getResource("/resources/gui/increaseLayerDistance.png")));
        this.btn_decreaseInterLayerDistance.setIcon(new ImageIcon(TopologyPanel.class.getResource("/resources/gui/decreaseLayerDistance.png")));
        this.btn_showThisLayerInfo.setIcon(new ImageIcon(TopologyPanel.class.getResource("/resources/gui/showLayerPropagation.png")));
        this.btn_showUpperLayerInfo.setIcon(new ImageIcon(TopologyPanel.class.getResource("/resources/gui/showLayerUpperPropagation.png")));
        this.btn_showLowerLayerInfo.setIcon(new ImageIcon(TopologyPanel.class.getResource("/resources/gui/showLayerLowerPropagation.png")));
        this.btn_npChangeUndo.setIcon(new ImageIcon(TopologyPanel.class.getResource("/resources/gui/undoButton.png")));
        this.btn_npChangeRedo.setIcon(new ImageIcon(TopologyPanel.class.getResource("/resources/gui/redoButton.png")));

        this.btn_increaseInterLayerDistance.addActionListener(this);
        this.btn_decreaseInterLayerDistance.addActionListener(this);
        this.btn_showLowerLayerInfo.addActionListener(this);
        this.btn_showUpperLayerInfo.addActionListener(this);
        this.btn_showThisLayerInfo.addActionListener(this);
        this.btn_npChangeUndo.addActionListener(this);
        this.btn_npChangeRedo.addActionListener(this);
        
        /* Layers panel */
        layersPanel = new JPanel(new MigLayout("insets 0, gap 1, fillx, wrap 1"));

        final JScrollPane scPane = new JScrollPane(layersPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scPane.setLayout(new ScrollPaneLayout() {
            @Override
            public void layoutContainer(Container parent) {
              JScrollPane scrollPane = (JScrollPane)parent;

              Rectangle availR = scrollPane.getBounds();
              availR.x = availR.y = 0;

              Insets insets = parent.getInsets();
              availR.x = insets.left;
              availR.y = insets.top;
              availR.width  -= insets.left + insets.right;
              availR.height -= insets.top  + insets.bottom;

              Rectangle vsbR = new Rectangle();
              vsbR.width  = 5;
              vsbR.height = availR.height;
              vsbR.x = availR.x + availR.width - vsbR.width;
              vsbR.y = availR.y;

              if(viewport != null) {
                viewport.setBounds(availR);
              }
              if(vsb != null) {
                vsb.setVisible(true);
                vsb.setBounds(vsbR);
              }
            }
          });
        scPane.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
            private final Dimension d = new Dimension();
            @Override protected JButton createDecreaseButton(int orientation) {
              return new JButton() {
                @Override public Dimension getPreferredSize() {
                  return d;
                }
              };
            }
            @Override protected JButton createIncreaseButton(int orientation) {
              return new JButton() {
                @Override public Dimension getPreferredSize() {
                  return d;
                }
              };
            }
            @Override
            protected void paintTrack(Graphics g, JComponent c, Rectangle r) {}
            @Override
            protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
              Graphics2D g2 = (Graphics2D)g.create();
              g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                  RenderingHints.VALUE_ANTIALIAS_ON);
              Color color = null;
              JScrollBar sb = (JScrollBar)c;
              if(!sb.isEnabled() || r.width>r.height)
                return;
              else
                color = Color.LIGHT_GRAY;
              
              g2.setPaint(color);
              g2.fillRoundRect(r.x,r.y,r.width,r.height,10,10);
              g2.setPaint(Color.WHITE);
              g2.drawRoundRect(r.x,r.y,r.width,r.height,10,10);
              g2.dispose();
            }
            @Override
            protected void setThumbBounds(int x, int y, int width, int height) {
              super.setThumbBounds(x, y, width, height);
              scrollbar.repaint();
            }
          });
        
        scPane.setPreferredSize(new Dimension(0, 200));
        scPane.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.GRAY));
        
        final JPanel upDownButtonsPanel = new JPanel();
        final Border insetsBorder = BorderFactory.createEmptyBorder(10, 0, 0, 0);
        final Border lineBorder = BorderFactory.createMatteBorder(1, 0, 0, 0, Color.GRAY);
        upDownButtonsPanel.setBorder(new CompoundBorder(insetsBorder, lineBorder));
        
        /* Up down active layer buttons */
        btn_moveUpLayer = new JButton("\u25B2");
		btn_moveUpLayer.setOpaque(false);
		btn_moveUpLayer.setContentAreaFilled(false);
		btn_moveUpLayer.setBorder(null);
		btn_moveUpLayer.setBorderPainted(false);
		btn_moveUpLayer.setBorderPainted(false);
		btn_moveUpLayer.setFocusable(false);
		btn_moveUpLayer.setMargin(new Insets(0, 0, 0, 0));
		btn_moveUpLayer.setToolTipText("Move up the active layer");
        btn_moveUpLayer.addActionListener(this);

		
        btn_moveDownLayer = new JButton("\u25BC");
        btn_moveDownLayer.setOpaque(false);
        btn_moveDownLayer.setContentAreaFilled(false);
        btn_moveDownLayer.setBorder(null);
        btn_moveDownLayer.setBorderPainted(false);
        btn_moveDownLayer.setBorderPainted(false);
        btn_moveDownLayer.setFocusable(false);
        btn_moveDownLayer.setMargin(new Insets(0, 0, 0, 0));
        btn_moveDownLayer.setToolTipText("Move down the active layer");
        btn_moveDownLayer.addActionListener(this);

		
        upDownButtonsPanel.add(btn_moveUpLayer);
        upDownButtonsPanel.add(btn_moveDownLayer);
        
        updateLayersPanel();
        
        this.layerToolBar.add(btn_increaseInterLayerDistance, "growx");
        this.layerToolBar.add(btn_decreaseInterLayerDistance, "growx");
        this.layerToolBar.add(btn_showLowerLayerInfo, "growx");
        this.layerToolBar.add(btn_showUpperLayerInfo, "growx");
        this.layerToolBar.add(btn_showThisLayerInfo, "growx");
        //multiLayerToolbar.add(btn_npChangeUndo);
        //multiLayerToolbar.add(btn_npChangeRedo);
        this.layerToolBar.addSeparator();
        this.layerToolBar.add(upDownButtonsPanel, "growx");
        this.layerToolBar.add(scPane, "growx");
        this.layerToolBar.add(Box.createVerticalGlue());

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
        }
        else if (src == btn_moveUpLayer)
        {
            final NetworkLayer defaultLayer = callback.getDesign().getNetworkLayerDefault();

            final BidiMap<NetworkLayer, Integer> layerOrderMapConsideringNonVisible = new DualHashBidiMap<>(vs.getCanvasLayerOrderIndexMap(true));
            final int defaultLayerIndex = layerOrderMapConsideringNonVisible.get(defaultLayer);
            
            if (defaultLayerIndex == 0) return;
            
            final NetworkLayer neighbourLayer = layerOrderMapConsideringNonVisible.inverseBidiMap().get(defaultLayerIndex - 1);

            // Swap the selected layer with the one on top of it.
            this.swap(layerOrderMapConsideringNonVisible, defaultLayer, neighbourLayer);

            vs.setCanvasLayerVisibilityAndOrder(callback.getDesign(), layerOrderMapConsideringNonVisible, vs.getCanvasLayerVisibilityMap());

            callback.updateVisualizationAfterChanges();
            updateLayersPanel();
        }
        else if (src == btn_moveDownLayer)
        {
            final NetworkLayer defaultLayer = callback.getDesign().getNetworkLayerDefault();

            final BidiMap<NetworkLayer, Integer> layerOrderMapConsideringNonVisible = new DualHashBidiMap<>(vs.getCanvasLayerOrderIndexMap(true));
            final int defaultLayerIndex = layerOrderMapConsideringNonVisible.get(defaultLayer);
            
    		if (defaultLayerIndex == layerOrderMapConsideringNonVisible.size() - 1) return;
            
            final NetworkLayer neighbourLayer = layerOrderMapConsideringNonVisible.inverseBidiMap().get(defaultLayerIndex + 1);

            // Swap the selected layer with the one on top of it.
            this.swap(layerOrderMapConsideringNonVisible, defaultLayer, neighbourLayer);

            vs.setCanvasLayerVisibilityAndOrder(callback.getDesign(), layerOrderMapConsideringNonVisible, vs.getCanvasLayerVisibilityMap());

            callback.updateVisualizationAfterChanges();
            updateLayersPanel();
        }
    }

    public void updateLayersPanel()
    {
    	layersPanel.removeAll();
    	
    	final NetPlan netPlan = callback.getDesign();
    	final VisualizationState vs = callback.getVisualizationState();
    	
    	for (NetworkLayer layer : vs.getCanvasLayersInVisualizationOrder(true))
    	{
    		final String layerName = layer.getName().replaceAll(" ", "");
    		final String buttonText = layerName.isEmpty() || layerName.contains("Layer") ? "LAY" + layer.getIndex() : (layerName.length() <= 4 ? layerName : layerName.substring(0, 4));
			final JButton layerButton = new JButton();
			layerButton.setOpaque(false);
			layerButton.setContentAreaFilled(false);
			layerButton.setBorder(null);
			layerButton.setBorderPainted(false);
			layerButton.setBorderPainted(false);
			layerButton.setFocusable(false);
			layerButton.setMargin(new Insets(0, 0, 0, 0));
	        layerButton.setText(buttonText.toUpperCase());

	        final String layerState;
    		final Font buttonFont;
    		
    		if (layer.isDefaultLayer())
    		{
    			buttonFont = new Font("Segoe UI", Font.BOLD, 16);
    			layerButton.setFont(buttonFont.deriveFont(Font.BOLD));
    			layerState = "active layer";
    		}
    		else if (vs.isLayerVisibleInCanvas(layer))
    		{
    			buttonFont = new Font("Segoe UI", Font.PLAIN, 14);
    			layerState = "visible layer";
    		}
    		else
    		{
    			buttonFont = new Font("Segoe UI", Font.PLAIN, 14);
    			layerButton.setForeground(Color.GRAY);
    			layerState = "hidden layer";
    		}
    		
    		layerButton.setFont(buttonFont);
    		layerButton.setToolTipText(layer + " (" + layerState + ")");
    		
    		layerButton.addMouseListener(new MouseAdapter() {
    			@Override
    			public void mouseClicked(MouseEvent e)
    			{
    				if (!layer.isDefaultLayer())
    	            {
    	                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 1)
    	                {                                                
    	                    callback.getVisualizationState().setCanvasLayerVisibility(layer, !vs.isLayerVisibleInCanvas(layer));
    	                    callback.updateVisualizationAfterChanges();
    	                }
    	                else if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2)
    	                {
    	                    callback.getDesign().setNetworkLayerDefault(layer);
    	                    callback.getVisualizationState().setCanvasLayerVisibility(layer, true);
    	                    callback.updateVisualizationAfterChanges();
    	                }
    	                updateLayersPanel();
    	            }  
    			}
			});
    		
    		layersPanel.add(layerButton, "align center");
    		
    	}
    	
    	layersPanel.revalidate();
    	layersPanel.updateUI();
    	
    	/* Update up/down buttons */
    	final NetworkLayer defaultLayer = netPlan.getNetworkLayerDefault();
    	final Map<NetworkLayer, Integer> layerOrderMapConsideringNonVisible = vs.getCanvasLayerOrderIndexMap(true);
        final int defaultLayerIndex = layerOrderMapConsideringNonVisible.get(defaultLayer);
        
        if (defaultLayerIndex == 0) 
        {
        	btn_moveUpLayer.setEnabled(false);
        	btn_moveUpLayer.setForeground(Color.GRAY);
        }
		else
		{
			btn_moveUpLayer.setEnabled(true);
        	btn_moveUpLayer.setForeground(Color.BLACK);
		}
        
		if (defaultLayerIndex == layerOrderMapConsideringNonVisible.size() - 1)
		{
			btn_moveDownLayer.setEnabled(false);
			btn_moveDownLayer.setForeground(Color.GRAY);
		}
		else
		{
			btn_moveDownLayer.setEnabled(true);
			btn_moveDownLayer.setForeground(Color.BLACK);
		}
    }
    
    private <K, V> void swap(Map<K, V> map, K k1, K k2)
    {
        final V value1 = map.get(k1);
        final V value2 = map.get(k2);
        if ((value1 == null) || (value2 == null)) throw new RuntimeException();
        map.remove(k1);
        map.remove(k2);
        map.put(k1, value2);
        map.put(k2, value1);
    }
    
}
