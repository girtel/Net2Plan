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

import java.awt.*;
import java.awt.event.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.border.LineBorder;

import com.net2plan.gui.utils.*;
import org.apache.commons.collections15.BidiMap;

import com.google.common.collect.Sets;
import com.net2plan.gui.utils.topologyPane.jung.AddLinkGraphPlugin;
import com.net2plan.gui.utils.topologyPane.jung.JUNGCanvas;
import com.net2plan.gui.utils.viewEditWindows.WindowController;
import com.net2plan.interfaces.networkDesign.Demand;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.MulticastDemand;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkElement;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.internal.Constants.DialogType;
import com.net2plan.internal.Constants.NetworkElementType;
import com.net2plan.internal.ErrorHandling;
import com.net2plan.internal.SystemUtils;
import com.net2plan.internal.plugins.ITopologyCanvas;
import com.net2plan.utils.Pair;

@SuppressWarnings("unchecked")
public class TopologyPanel extends JPanel implements ActionListener//FrequentisBackgroundPanel implements ActionListener//JPanel implements ActionListener
{
    private final IVisualizationCallback callback;
    private final ITopologyCanvas canvas;

    //    private final JPanel layerChooserPane;
//    private final JComboBox layerChooser;
    private final JButton btn_load, btn_loadDemand, btn_save, btn_zoomIn, btn_zoomOut, btn_zoomAll, btn_takeSnapshot, btn_reset;
    private final JButton btn_increaseInterLayerDistance, btn_decreaseInterLayerDistance;
    private final JButton btn_increaseNodeSize, btn_decreaseNodeSize, btn_increaseFontSize, btn_decreaseFontSize;
    private final JButton btn_npChangeUndo, btn_npChangeRedo;
    private final JToggleButton btn_showLowerLayerInfo, btn_showUpperLayerInfo, btn_showThisLayerInfo;
    private final JToggleButton btn_showNodeNames, btn_showLinkIds, btn_showNonConnectedNodes;
    private final JPopUpButton btn_multilayer;
    private final JPopupMenu multiLayerPopUp;
    private final JButton btn_tableControlWindow;
    private final JToggleButton btn_osmMap;
    private final JLabel position;
    private final JPanel canvasPanel;
    private final MultiLayerControlPanel multilayerControlPanel;

    private final File defaultDesignDirectory, defaultDemandDirectory;

    private FileChooserNetworkDesign fc_netPlan, fc_demands;

    /**
     * Simplified constructor that does not require to indicate default locations
     * for {@code .n2p} files.
     *
     * @param callback   Topology callback listening plugin events
     * @param canvasType Canvas type (i.e. JUNG)
     */
    public TopologyPanel(IVisualizationCallback callback, Class<? extends ITopologyCanvas> canvasType)
    {
        this(callback, canvasType, null);
    }

    /**
     * Simplified constructor that does not require to indicate default locations
     * for {@code .n2p} files.
     *
     * @param callback   Topology callback listening plugin events
     * @param canvasType Canvas type (i.e. JUNG)
     * @param plugins    List of plugins to be included (it may be null)
     */
    public TopologyPanel(IVisualizationCallback callback, Class<? extends ITopologyCanvas> canvasType, List<ITopologyCanvasPlugin> plugins)
    {
        this(callback, null, null, canvasType, plugins);
    }

    /**
     * Default constructor.
     *
     * @param callback               Topology callback listening plugin events
     * @param defaultDesignDirectory Default location for design {@code .n2p} files (it may be null, then default is equal to {@code net2planFolder/workspace/data/networkTopologies})
     * @param defaultDemandDirectory Default location for design {@code .n2p} files (it may be null, then default is equal to {@code net2planFolder/workspace/data/trafficMatrices})
     * @param canvasType             Canvas type (i.e. JUNG)
     * @param plugins                List of plugins to be included (it may be null)
     */
    public TopologyPanel(final IVisualizationCallback callback, File defaultDesignDirectory, File defaultDemandDirectory, Class<? extends ITopologyCanvas> canvasType, List<ITopologyCanvasPlugin> plugins)
    {
        File currentDir = SystemUtils.getCurrentDir();

        this.callback = callback;
        this.defaultDesignDirectory = defaultDesignDirectory == null ? new File(currentDir + SystemUtils.getDirectorySeparator() + "workspace" + SystemUtils.getDirectorySeparator() + "data" + SystemUtils.getDirectorySeparator() + "networkTopologies") : defaultDesignDirectory;
        this.defaultDemandDirectory = defaultDemandDirectory == null ? new File(currentDir + SystemUtils.getDirectorySeparator() + "workspace" + SystemUtils.getDirectorySeparator() + "data" + SystemUtils.getDirectorySeparator() + "trafficMatrices") : defaultDemandDirectory;
        this.multilayerControlPanel = new MultiLayerControlPanel(callback);

        try
        {
            canvas = canvasType.getDeclaredConstructor(IVisualizationCallback.class, TopologyPanel.class).newInstance(callback, this);
        } catch (Exception e)
        {
            throw new RuntimeException(e);
        }

        if (plugins != null)
            for (ITopologyCanvasPlugin plugin : plugins)
                addPlugin(plugin);

        setLayout(new BorderLayout());

        JToolBar toolbar = new JToolBar();
        toolbar.setRollover(true);
        toolbar.setFloatable(false);
        toolbar.setOpaque(false);
        toolbar.setBorderPainted(false);

//        layerChooser = new WiderJComboBox();
//        layerChooserPane = new JPanel(new BorderLayout());
//        layerChooserPane.add(new JLabel("Select layer: "), BorderLayout.WEST);
//        layerChooserPane.add(layerChooser, BorderLayout.CENTER);
//        layerChooser.addActionListener(new ActionListener()
//        {
//            @Override
//            public void actionPerformed(ActionEvent e)
//            {
//                Object selectedItem = layerChooser.getSelectedItem();
//                if (!(selectedItem instanceof StringLabeller))
//                    ErrorHandling.showErrorDialog("Bad object", "Error selecting layer");
//
//                final long newDefaultLayerId = (Long) ((StringLabeller) selectedItem).getObject();
//                final NetPlan currentState = callback.getDesign();
//                final NetworkLayer layer = currentState.getNetworkLayerFromId(newDefaultLayerId);
////				System.out.println ("Select layer: layerId " + layerId + ", layer: " + layer);
//                if (layer == null) throw new RuntimeException("Bad: " + newDefaultLayerId);
//                currentState.setNetworkLayerDefault(layer);
//
//                callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.LAYER));
//            }
//        });

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(toolbar, BorderLayout.NORTH);
//        topPanel.add(layerChooserPane, BorderLayout.SOUTH);

        add(topPanel, BorderLayout.NORTH);

        JComponent canvasComponent = canvas.getCanvasComponent();

        canvasPanel = new JPanel(new BorderLayout());
        canvasComponent.setBorder(LineBorder.createBlackLineBorder());

        JToolBar multiLayerToolbar = new JToolBar(JToolBar.VERTICAL);
        multiLayerToolbar.setRollover(true);
        multiLayerToolbar.setFloatable(false);
        multiLayerToolbar.setOpaque(false);

        canvasPanel.add(canvasComponent, BorderLayout.CENTER);
        canvasPanel.add(multiLayerToolbar, BorderLayout.WEST);
        add(canvasPanel, BorderLayout.CENTER);

        btn_load = new JButton();
        btn_load.setToolTipText("Load a network design");
        btn_loadDemand = new JButton();
        btn_loadDemand.setToolTipText("Load a traffic demand set");
        btn_save = new JButton();
        btn_save.setToolTipText("Save current state to a file");
        btn_zoomIn = new JButton();
        btn_zoomIn.setToolTipText("Zoom in");
        btn_zoomOut = new JButton();
        btn_zoomOut.setToolTipText("Zoom out");
        btn_zoomAll = new JButton();
        btn_zoomAll.setToolTipText("Zoom all");
        btn_takeSnapshot = new JButton();
        btn_takeSnapshot.setToolTipText("Take a snapshot of the canvas");
        btn_showNodeNames = new JToggleButton();
        btn_showNodeNames.setToolTipText("Show/hide node names");
        btn_showLinkIds = new JToggleButton();
        btn_showLinkIds.setToolTipText("Show/hide link utilization, measured as the ratio between the total traffic in the link (including that in protection segments) and total link capacity (including that reserved by protection segments)");
        btn_showNonConnectedNodes = new JToggleButton();
        btn_showNonConnectedNodes.setToolTipText("Show/hide non-connected nodes");
        btn_increaseNodeSize = new JButton();
        btn_increaseNodeSize.setToolTipText("Increase node size");
        btn_decreaseNodeSize = new JButton();
        btn_decreaseNodeSize.setToolTipText("Decrease node size");
        btn_increaseFontSize = new JButton();
        btn_increaseFontSize.setToolTipText("Increase font size");
        btn_decreaseFontSize = new JButton();
        btn_decreaseFontSize.setToolTipText("Decrease font size");
        /* Multilayer buttons */
        btn_increaseInterLayerDistance = new JButton();
        btn_increaseInterLayerDistance.setToolTipText("Increase the distance between layers (when more than one layer is visible)");
        btn_decreaseInterLayerDistance = new JButton();
        btn_decreaseInterLayerDistance.setToolTipText("Decrease the distance between layers (when more than one layer is visible)");
        btn_showLowerLayerInfo = new JToggleButton();
        btn_showLowerLayerInfo.setToolTipText("Shows the links in lower layers that carry traffic of the picked element");
        btn_showLowerLayerInfo.setSelected(getVisualizationState().isShowInCanvasLowerLayerPropagation());
        btn_showUpperLayerInfo = new JToggleButton();
        btn_showUpperLayerInfo.setToolTipText("Shows the links in upper layers that carry traffic that appears in the picked element");
        btn_showUpperLayerInfo.setSelected(getVisualizationState().isShowInCanvasUpperLayerPropagation());
        btn_showThisLayerInfo = new JToggleButton();
        btn_showThisLayerInfo.setToolTipText("Shows the links in the same layer as the picked element, that carry traffic that appears in the picked element");
        btn_showThisLayerInfo.setSelected(getVisualizationState().isShowInCanvasThisLayerPropagation());
        btn_npChangeUndo = new JButton ();
        btn_npChangeUndo.setToolTipText("Navigate back to the previous state of the network (last time the network design was changed)");
        btn_npChangeRedo = new JButton ();
        btn_npChangeRedo.setToolTipText("Navigate forward to the next state of the network (when network design was changed");


        btn_osmMap = new JToggleButton();
        btn_osmMap.setToolTipText("Toggle between on/off the OSM support. An internet connection is required in order for this to work.");
        btn_tableControlWindow = new JButton();
        btn_tableControlWindow.setToolTipText("Show the network topology control window.");

        // MultiLayer control window
        multiLayerPopUp = new JPopupMenu();
        multiLayerPopUp.add(multilayerControlPanel);
        btn_multilayer = new JPopUpButton("", multiLayerPopUp);

        btn_reset = new JButton("Reset");
        btn_reset.setToolTipText("Reset the user interface");
        btn_reset.setMnemonic(KeyEvent.VK_R);

        btn_load.setIcon(new ImageIcon(TopologyPanel.class.getResource("/resources/gui/loadDesign.png")));
        btn_loadDemand.setIcon(new ImageIcon(TopologyPanel.class.getResource("/resources/gui/loadDemand.png")));
        btn_save.setIcon(new ImageIcon(TopologyPanel.class.getResource("/resources/gui/saveDesign.png")));
        btn_showNodeNames.setIcon(new ImageIcon(TopologyPanel.class.getResource("/resources/gui/showNodeName.png")));
        btn_showLinkIds.setIcon(new ImageIcon(TopologyPanel.class.getResource("/resources/gui/showLinkUtilization.png")));
        btn_showNonConnectedNodes.setIcon(new ImageIcon(TopologyPanel.class.getResource("/resources/gui/showNonConnectedNodes.png")));
        btn_zoomIn.setIcon(new ImageIcon(TopologyPanel.class.getResource("/resources/gui/zoomIn.png")));
        btn_zoomOut.setIcon(new ImageIcon(TopologyPanel.class.getResource("/resources/gui/zoomOut.png")));
        btn_zoomAll.setIcon(new ImageIcon(TopologyPanel.class.getResource("/resources/gui/zoomAll.png")));
        btn_takeSnapshot.setIcon(new ImageIcon(TopologyPanel.class.getResource("/resources/gui/takeSnapshot.png")));
        btn_increaseNodeSize.setIcon(new ImageIcon(TopologyPanel.class.getResource("/resources/gui/increaseNode.png")));
        btn_decreaseNodeSize.setIcon(new ImageIcon(TopologyPanel.class.getResource("/resources/gui/decreaseNode.png")));
        btn_increaseFontSize.setIcon(new ImageIcon(TopologyPanel.class.getResource("/resources/gui/increaseFont.png")));
        btn_decreaseFontSize.setIcon(new ImageIcon(TopologyPanel.class.getResource("/resources/gui/decreaseFont.png")));
        btn_increaseInterLayerDistance.setIcon(new ImageIcon(TopologyPanel.class.getResource("/resources/gui/increaseLayerDistance.png")));
        btn_decreaseInterLayerDistance.setIcon(new ImageIcon(TopologyPanel.class.getResource("/resources/gui/decreaseLayerDistance.png")));
        btn_multilayer.setIcon(new ImageIcon(TopologyPanel.class.getResource("/resources/gui/showLayerControl.png")));
        btn_showThisLayerInfo.setIcon(new ImageIcon(TopologyPanel.class.getResource("/resources/gui/showLayerPropagation.png")));
        btn_showUpperLayerInfo.setIcon(new ImageIcon(TopologyPanel.class.getResource("/resources/gui/showLayerUpperPropagation.png")));
        btn_showLowerLayerInfo.setIcon(new ImageIcon(TopologyPanel.class.getResource("/resources/gui/showLayerLowerPropagation.png")));
        btn_tableControlWindow.setIcon(new ImageIcon(TopologyPanel.class.getResource("/resources/gui/showControl.png")));
        btn_osmMap.setIcon(new ImageIcon(TopologyPanel.class.getResource("/resources/gui/showOSM.png")));
        btn_npChangeUndo.setIcon(new ImageIcon(TopologyPanel.class.getResource("/resources/gui/undoButton.png")));
        btn_npChangeRedo.setIcon(new ImageIcon(TopologyPanel.class.getResource("/resources/gui/redoButton.png")));

        btn_load.addActionListener(this);
        btn_loadDemand.addActionListener(this);
        btn_save.addActionListener(this);
        btn_showNodeNames.addActionListener(this);
        btn_showLinkIds.addActionListener(this);
        btn_showNonConnectedNodes.addActionListener(this);
        btn_zoomIn.addActionListener(this);
        btn_zoomOut.addActionListener(this);
        btn_zoomAll.addActionListener(this);
        btn_takeSnapshot.addActionListener(this);
        btn_reset.addActionListener(this);
        btn_increaseInterLayerDistance.addActionListener(this);
        btn_decreaseInterLayerDistance.addActionListener(this);
        btn_showLowerLayerInfo.addActionListener(this);
        btn_showUpperLayerInfo.addActionListener(this);
        btn_showThisLayerInfo.addActionListener(this);
        btn_increaseNodeSize.addActionListener(this);
        btn_decreaseNodeSize.addActionListener(this);
        btn_increaseFontSize.addActionListener(this);
        btn_decreaseFontSize.addActionListener(this);
        btn_npChangeUndo.addActionListener(this);
        btn_npChangeRedo.addActionListener(this);

        toolbar.add(btn_load);
        toolbar.add(btn_loadDemand);
        toolbar.add(btn_save);
        toolbar.add(new JToolBar.Separator());
        toolbar.add(btn_zoomIn);
        toolbar.add(btn_zoomOut);
        toolbar.add(btn_zoomAll);
        toolbar.add(btn_takeSnapshot);
        toolbar.add(new JToolBar.Separator());
        toolbar.add(btn_showNodeNames);
        toolbar.add(btn_showLinkIds);
        toolbar.add(btn_showNonConnectedNodes);
        toolbar.add(new JToolBar.Separator());
        toolbar.add(btn_increaseNodeSize);
        toolbar.add(btn_decreaseNodeSize);
        toolbar.add(btn_increaseFontSize);
        toolbar.add(btn_decreaseFontSize);
        toolbar.add(new JToolBar.Separator());
        toolbar.add(Box.createHorizontalGlue());
        toolbar.add(btn_osmMap);
        toolbar.add(btn_tableControlWindow);
        toolbar.add(btn_reset);

        multiLayerToolbar.add(new JToolBar.Separator());
        multiLayerToolbar.add(btn_multilayer);
        multiLayerToolbar.add(btn_increaseInterLayerDistance);
        multiLayerToolbar.add(btn_decreaseInterLayerDistance);
        multiLayerToolbar.add(btn_showLowerLayerInfo);
        multiLayerToolbar.add(btn_showUpperLayerInfo);
        multiLayerToolbar.add(btn_showThisLayerInfo);
        multiLayerToolbar.add(Box.createVerticalGlue());
        multiLayerToolbar.add(btn_npChangeUndo);
        multiLayerToolbar.add(btn_npChangeRedo);

        this.addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentResized(ComponentEvent e)
            {
                canvas.zoomAll();
            }
        });

        List<Component> children = SwingUtils.getAllComponents(this);
        for (Component component : children)
            if (component instanceof AbstractButton)
                component.setFocusable(false);

        if (ErrorHandling.isDebugEnabled())
        {
            canvas.getCanvasComponent().addMouseMotionListener(new MouseMotionListener()
            {
                @Override
                public void mouseDragged(MouseEvent e)
                {
                }

                @Override
                public void mouseMoved(MouseEvent e)
                {
                    Point point = e.getPoint();
                    position.setText("view = " + point + ", NetPlan coord = " + canvas.getCanvasPointFromNetPlanPoint(point));
                }
            });

            position = new JLabel();
            add(position, BorderLayout.SOUTH);
        } else
        {
            position = null;
        }

        new FileDrop(canvasComponent, new LineBorder(Color.BLACK), new FileDrop.Listener()
        {
            @Override
            public void filesDropped(File[] files)
            {
                for (File file : files)
                {
                    try
                    {
                        if (!file.getName().toLowerCase(Locale.getDefault()).endsWith(".n2p")) return;
                        loadDesignFromFile(file);
                        break;
                    } catch (Throwable e)
                    {
                        break;
                    }
                }
            }
        });

        btn_showNodeNames.setSelected(getVisualizationState().isCanvasShowNodeNames());
        btn_showLinkIds.setSelected(getVisualizationState().isCanvasShowLinkLabels());
        btn_showNonConnectedNodes.setSelected(getVisualizationState().isCanvasShowNonConnectedNodes());

        final ITopologyCanvasPlugin popupPlugin = new PopupMenuPlugin(callback, this.canvas);
        addPlugin(new PanGraphPlugin(callback, canvas, MouseEvent.BUTTON1_MASK));
        if (callback.getVisualizationState().isNetPlanEditable() && getCanvas() instanceof JUNGCanvas)
            addPlugin(new AddLinkGraphPlugin(callback, canvas, MouseEvent.BUTTON1_MASK, MouseEvent.BUTTON1_MASK | MouseEvent.SHIFT_MASK));
        addPlugin(popupPlugin);
        if (callback.getVisualizationState().isNetPlanEditable())
            addPlugin(new MoveNodePlugin(callback, canvas, MouseEvent.BUTTON1_MASK | MouseEvent.CTRL_MASK));

        setBorder(BorderFactory.createTitledBorder(new LineBorder(Color.BLACK), "Network topology"));
//        setAllowLoadTrafficDemand(callback.allowLoadTrafficDemands());
    }

    public VisualizationState getVisualizationState()
    {
        return callback.getVisualizationState();
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        Object src = e.getSource();
        final VisualizationState vs = callback.getVisualizationState();
        if (src == btn_load)
        {
            loadDesign();
        } else if (src == btn_loadDemand)
        {
            loadTrafficDemands();
        } else if (src == btn_save)
        {
            saveDesign();
        } else if (src == btn_showNodeNames)
        {
            vs.setCanvasShowNodeNames(btn_showNodeNames.isSelected());
            canvas.refresh();
        } else if (src == btn_showLinkIds)
        {
            vs.setCanvasShowLinkLabels(btn_showLinkIds.isSelected());
            canvas.refresh();
        } else if (src == btn_showNonConnectedNodes)
        {
            vs.setCanvasShowNonConnectedNodes(btn_showNonConnectedNodes.isSelected());
            canvas.refresh();
        } else if (src == btn_takeSnapshot)
        {
            takeSnapshot();
        } else if (src == btn_zoomIn)
        {
            canvas.zoomIn();
        } else if (src == btn_zoomOut)
        {
            canvas.zoomOut();
        } else if (src == btn_zoomAll)
        {
            canvas.zoomAll();
        } else if (src == btn_reset)
        {
            callback.loadDesignDoNotUpdateVisualization(new NetPlan());
            Pair<BidiMap<NetworkLayer, Integer>, Map<NetworkLayer, Boolean>> res =
                    vs.suggestCanvasUpdatedVisualizationLayerInfoForNewDesign(new HashSet<>(callback.getDesign().getNetworkLayers()));
            vs.setCanvasLayerVisibilityAndOrder(callback.getDesign(), res.getFirst(), res.getSecond());
            callback.updateVisualizationAfterNewTopology();
            callback.getUndoRedoNavigationManager().updateNavigationInformation_newNetPlanChange();
        } else if (src == btn_increaseInterLayerDistance)
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
            final int newInterLayerDistance = currentInterLayerDistance - (int) Math.ceil(currentInterLayerDistance * (1 - VisualizationConstants.SCALE_OUT));

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
        	callback.undoRequested();
        } else if (src == btn_npChangeRedo)
        {
        	callback.redoRequested();
        } else if (src == btn_tableControlWindow)
        {
            WindowController.showTablesWindow(true);
        } else if (src == btn_osmMap)
        {
            if (btn_osmMap.isSelected())
            {
                switchOSMSupport(true);
            } else if (!btn_osmMap.isSelected())
            {
                switchOSMSupport(false);
            }
        } else if (src == btn_increaseNodeSize)
        {
            callback.getVisualizationState().increaseCanvasNodeSizeAll();
            canvas.refresh();
        } else if (src == btn_decreaseNodeSize)
        {
            callback.getVisualizationState().decreaseCanvasNodeSizeAll();
            canvas.refresh();
        } else if (src == btn_increaseFontSize)
        {
            callback.getVisualizationState().increaseCanvasFontSizeAll();
            canvas.refresh();
        } else if (src == btn_decreaseFontSize)
        {
            final boolean somethingChanged = callback.getVisualizationState().decreaseCanvasFontSizeAll();
            if (somethingChanged) canvas.refresh();
        }
    }

    /**
     * Adds a new plugin to the canvas.
     *
     * @param plugin Plugin to be added
     * @since 0.3.0
     */

    public void addPlugin(ITopologyCanvasPlugin plugin)
    {
        canvas.addPlugin(plugin);
    }

    private void checkNetPlanFileChooser()
    {
        if (fc_netPlan == null)
        {
            fc_netPlan = new FileChooserNetworkDesign(defaultDesignDirectory, DialogType.NETWORK_DESIGN);
        }
    }

    private void checkDemandFileChooser()
    {
        if (fc_demands == null)
        {
            fc_demands = new FileChooserNetworkDesign(defaultDemandDirectory, DialogType.DEMANDS);
        }
    }

    private String createLayerName(long layerId)
    {
        final NetworkLayer layer = callback.getDesign().getNetworkLayerFromId(layerId);
        return "Layer " + layer.getIndex() + (layer.getName().isEmpty() ? "" : ": " + layer.getName());
    }

    public JPanel getCanvasPanel()
    {
        return canvasPanel;
    }

    /**
     * Returns a reference to the topology canvas.
     *
     * @return Reference to the topology canvas
     * @since 0.2.3
     */
    public ITopologyCanvas getCanvas()
    {
        return canvas;
    }

//    private StringLabeller getLayerItem(long layerId)
//    {
//        int numLayers = layerChooser.getItemCount();
//        for (int l = 0; l < numLayers; l++)
//        {
//            StringLabeller item = (StringLabeller) layerChooser.getItemAt(l);
//            if (layerId == (Long) item.getObject()) return item;
//        }
//
//        throw new RuntimeException("Bad");
//    }

    /**
     * Loads a network design from a {@code .n2p} file.
     *
     * @since 0.3.0
     */
    public void loadDesign()
    {
        try
        {
            checkNetPlanFileChooser();

            int rc = fc_netPlan.showOpenDialog(null);
            if (rc != JFileChooser.APPROVE_OPTION) return;

            NetPlan aux = fc_netPlan.readNetPlan();

            aux.checkCachesConsistency();

            callback.loadDesignDoNotUpdateVisualization(aux);
            final VisualizationState vs = callback.getVisualizationState();
            Pair<BidiMap<NetworkLayer, Integer>, Map<NetworkLayer, Boolean>> res =
                    vs.suggestCanvasUpdatedVisualizationLayerInfoForNewDesign(new HashSet<>(callback.getDesign().getNetworkLayers()));
            vs.setCanvasLayerVisibilityAndOrder(callback.getDesign(), res.getFirst(), res.getSecond());
            callback.updateVisualizationAfterNewTopology();
            callback.getUndoRedoNavigationManager().updateNavigationInformation_newNetPlanChange();
        } catch (Net2PlanException ex)
        {
            if (ErrorHandling.isDebugEnabled()) ErrorHandling.addErrorOrException(ex, TopologyPanel.class);
            ErrorHandling.showErrorDialog(ex.getMessage(), "Error loading network design");
        } catch (Throwable ex)
        {
            ErrorHandling.addErrorOrException(ex, TopologyPanel.class);
            ErrorHandling.showErrorDialog("Error loading network design");
        }
    }

    private void loadDesignFromFile(File file)
    {
        try
        {
            NetPlan netPlan = new NetPlan(file);
            checkNetPlanFileChooser();
            fc_netPlan.setCurrentDirectory(file.getParentFile());

            callback.loadDesignDoNotUpdateVisualization(netPlan);
            final VisualizationState vs = callback.getVisualizationState();
            Pair<BidiMap<NetworkLayer, Integer>, Map<NetworkLayer, Boolean>> res =
                    vs.suggestCanvasUpdatedVisualizationLayerInfoForNewDesign(new HashSet<>(callback.getDesign().getNetworkLayers()));
            vs.setCanvasLayerVisibilityAndOrder(callback.getDesign(), res.getFirst(), res.getSecond());
            callback.updateVisualizationAfterNewTopology();
            callback.getUndoRedoNavigationManager().updateNavigationInformation_newNetPlanChange();
        } catch (Net2PlanException ex)
        {
            if (ErrorHandling.isDebugEnabled()) ErrorHandling.addErrorOrException(ex, TopologyPanel.class);
            ErrorHandling.showErrorDialog(ex.getMessage(), "Error loading network design");
        } catch (Throwable ex)
        {
            ErrorHandling.addErrorOrException(ex, TopologyPanel.class);
            ErrorHandling.showErrorDialog("Error loading network design");
        }
    }

    /**
     * Loads traffic demands from a {@code .n2p} file, overriding current demands.
     *
     * @since 0.3.0
     */
    public void loadTrafficDemands()
    {
        try
        {
            checkDemandFileChooser();

            int rc = fc_demands.showOpenDialog(null);
            if (rc != JFileChooser.APPROVE_OPTION) return;

            NetPlan demands = fc_demands.readDemands();

            if (!demands.hasDemands() && !demands.hasMulticastDemands())
                throw new Net2PlanException("Selected file doesn't contain a demand set");

            NetPlan netPlan = callback.getDesign();
            if (netPlan.hasDemands() || netPlan.hasMulticastDemands())
            {
                int result = JOptionPane.showConfirmDialog(null, "Current network structure contains a demand set. Overwrite?", "Loading demand set", JOptionPane.YES_NO_OPTION);
                if (result != JOptionPane.YES_OPTION) return;
            }

            NetPlan aux_netPlan = netPlan.copy();
            try
            {
                netPlan.removeAllDemands();
                for (Demand demand : demands.getDemands())
                    netPlan.addDemand(netPlan.getNode(demand.getIngressNode().getIndex()), netPlan.getNode(demand.getEgressNode().getIndex()), demand.getOfferedTraffic(), demand.getAttributes());

                netPlan.removeAllMulticastDemands();
                for (MulticastDemand demand : demands.getMulticastDemands())
                {
                    Set<Node> egressNodesThisNetPlan = new HashSet<Node>();
                    for (Node n : demand.getEgressNodes()) egressNodesThisNetPlan.add(netPlan.getNode(n.getIndex()));
                    netPlan.addMulticastDemand(netPlan.getNode(demand.getIngressNode().getIndex()), egressNodesThisNetPlan, demand.getOfferedTraffic(), demand.getAttributes());
                }
                callback.getVisualizationState().resetPickedState();
                callback.updateVisualizationAfterChanges(Sets.newHashSet(NetworkElementType.DEMAND, NetworkElementType.MULTICAST_DEMAND));
                callback.getUndoRedoNavigationManager().updateNavigationInformation_newNetPlanChange();
            } catch (Throwable ex)
            {
                callback.getDesign().assignFrom(aux_netPlan);
                throw new RuntimeException(ex);
            }
        } catch (Net2PlanException ex)
        {
            if (ErrorHandling.isDebugEnabled()) ErrorHandling.addErrorOrException(ex, TopologyPanel.class);
            ErrorHandling.showErrorDialog(ex.getMessage(), "Error loading traffic demands");
        } catch (Exception ex)
        {
            ErrorHandling.addErrorOrException(ex, TopologyPanel.class);
            ErrorHandling.showErrorDialog("Error loading traffic demands");
        }
    }

    /**
     * Refreshes the name of a layer.
     *
     * @param layerId Layer identifier
     * @since 0.3.1
     */
    public void refreshLayerName(long layerId)
    {
        multilayerControlPanel.refreshTable();
    }

    /**
     * Saves a network design to a {@code .n2p} file.
     *
     * @since 0.3.0
     */
    public void saveDesign()
    {
        try
        {
            checkNetPlanFileChooser();

            int rc = fc_netPlan.showSaveDialog(null);
            if (rc != JFileChooser.APPROVE_OPTION) return;

            NetPlan netPlan = callback.getDesign();
            if (netPlan.getNodes().isEmpty()) throw new Net2PlanException("Design is empty");

            fc_netPlan.saveNetPlan(netPlan);
            ErrorHandling.showInformationDialog("Design saved successfully", "Save design");
        } catch (Net2PlanException ex)
        {
            if (ErrorHandling.isDebugEnabled()) ErrorHandling.addErrorOrException(ex, TopologyPanel.class);
            ErrorHandling.showErrorDialog(ex.getMessage(), "Error saving network design");
        } catch (Throwable ex)
        {
            ErrorHandling.addErrorOrException(ex, TopologyPanel.class);
            ErrorHandling.showErrorDialog("Error saving network design");
        }
    }

//    /**
//     * Allows setting the current layer.
//     *
//     * @param layer Layer identifier
//     * @since 0.3.1
//     */
//    public void selectLayer(long layer)
//    {
//        long currentLayerId = (Long) ((StringLabeller) layerChooser.getSelectedItem()).getObject();
//        if (layer == currentLayerId) return;
//
//        layerChooser.setSelectedItem(getLayerItem(layer));
//    }
//
//    /**
//     * Configures the topology panel to allow (or not) loading of external traffic demand files.
//     *
//     * @param isAllowed Indicates whether or not it is allowed to load traffic demand files.
//     * @since 0.3.0
//     */
//    public void setAllowLoadTrafficDemand(boolean isAllowed) {
//        btn_loadDemand.setVisible(isAllowed);
//    }

    /**
     * Take a snapshot of the canvas.
     *
     * @since 0.3.0
     */
    public void takeSnapshot()
    {
        canvas.takeSnapshot();
    }

    public final void updateMultilayerVisibilityAndOrderPanel()
    {
        multilayerControlPanel.refreshTable();
    }

//    /**
//     * Updates the layer chooser.
//     *
//     * @since 0.3.1
//     */
//    public final void updateLayerChooser()
//    {
//        ActionListener[] al = layerChooser.getActionListeners();
//        for (ActionListener a : al) layerChooser.removeActionListener(a);
//
//        layerChooser.removeAllItems();
//
//        NetPlan currentState = callback.getDesign();
//
//        Collection<Long> layerIds = currentState.getNetworkLayerIds();
//
//        if (ErrorHandling.isDebugEnabled()) currentState.checkCachesConsistency();
//
//        for (long layerId : layerIds)
//            layerChooser.addItem(StringLabeller.of(layerId, createLayerName(layerId)));
//
//        for (ActionListener a : al) layerChooser.addActionListener(a);
//
//        layerChooser.setSelectedIndex(currentState.getNetworkLayerDefault().getIndex()); // PABLO: AQUI SE PIERDEN LOS LINKS!!!!
//
//        layerChooserPane.setVisible(layerChooser.getItemCount() > 1);
//
//        revalidate();
//    }

    private void switchOSMSupport(final boolean doSwitch)
    {
        if (doSwitch)
            canvas.runOSMSupport();
        else
            canvas.stopOSMSupport();
    }
}
