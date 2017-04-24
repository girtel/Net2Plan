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


package com.net2plan.gui.plugins.networkDesign.topologyPane;

import com.google.common.collect.Sets;
import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.gui.plugins.networkDesign.FileChooserNetworkDesign;
import com.net2plan.gui.plugins.networkDesign.interfaces.ITopologyCanvas;
import com.net2plan.gui.plugins.networkDesign.interfaces.ITopologyCanvasPlugin;
import com.net2plan.gui.plugins.networkDesign.topologyPane.jung.AddLinkGraphPlugin;
import com.net2plan.gui.plugins.networkDesign.topologyPane.jung.JUNGCanvas;
import com.net2plan.gui.plugins.networkDesign.visualizationControl.VisualizationConstants;
import com.net2plan.gui.plugins.networkDesign.visualizationControl.VisualizationState;
import com.net2plan.gui.utils.FileDrop;
import com.net2plan.gui.utils.JPopUpButton;
import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.internal.Constants.DialogType;
import com.net2plan.internal.Constants.NetworkElementType;
import com.net2plan.internal.ErrorHandling;
import com.net2plan.internal.SystemUtils;
import com.net2plan.utils.Pair;
import com.net2plan.utils.SwingUtils;
import org.apache.commons.collections15.BidiMap;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.*;
import java.util.List;

@SuppressWarnings("unchecked")
public class TopologyPanel extends JPanel implements ActionListener//FrequentisBackgroundPanel implements ActionListener//JPanel implements ActionListener
{
    private final GUINetworkDesign callback;
    private final ITopologyCanvas canvas;

    private final JButton btn_increaseInterLayerDistance, btn_decreaseInterLayerDistance;
    private final JButton btn_npChangeUndo, btn_npChangeRedo;
    private final JToggleButton btn_showLowerLayerInfo, btn_showUpperLayerInfo, btn_showThisLayerInfo;
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
    public TopologyPanel(GUINetworkDesign callback, Class<? extends ITopologyCanvas> canvasType)
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
    public TopologyPanel(GUINetworkDesign callback, Class<? extends ITopologyCanvas> canvasType, List<ITopologyCanvasPlugin> plugins)
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
    public TopologyPanel(final GUINetworkDesign callback, File defaultDesignDirectory, File defaultDemandDirectory, Class<? extends ITopologyCanvas> canvasType, List<ITopologyCanvasPlugin> plugins)
    {
        File currentDir = SystemUtils.getCurrentDir();

        this.callback = callback;
        this.defaultDesignDirectory = defaultDesignDirectory == null ? new File(currentDir + SystemUtils.getDirectorySeparator() + "workspace" + SystemUtils.getDirectorySeparator() + "data" + SystemUtils.getDirectorySeparator() + "networkTopologies") : defaultDesignDirectory;
        this.defaultDemandDirectory = defaultDemandDirectory == null ? new File(currentDir + SystemUtils.getDirectorySeparator() + "workspace" + SystemUtils.getDirectorySeparator() + "data" + SystemUtils.getDirectorySeparator() + "trafficMatrices") : defaultDemandDirectory;
        this.multilayerControlPanel = new MultiLayerControlPanel(callback);

        try
        {
            canvas = canvasType.getDeclaredConstructor(GUINetworkDesign.class, TopologyPanel.class).newInstance(callback, this);
        } catch (Exception e)
        {
            throw new RuntimeException(e);
        }

        if (plugins != null)
            for (ITopologyCanvasPlugin plugin : plugins)
                addPlugin(plugin);

        setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new BorderLayout());

        TopologyToolBar toolbar = new TopologyToolBar(callback, this, canvas);
        topPanel.add(toolbar, BorderLayout.NORTH);

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

        /* Multilayer buttons */
        btn_increaseInterLayerDistance = new JButton();
        btn_increaseInterLayerDistance.setToolTipText("Increase the distance between layers (when more than one layer is visible)");
        btn_decreaseInterLayerDistance = new JButton();
        btn_decreaseInterLayerDistance.setToolTipText("Decrease the distance between layers (when more than one layer is visible)");
        btn_showLowerLayerInfo = new JToggleButton();
        btn_showLowerLayerInfo.setToolTipText("Shows the links in lower layers that carry traffic of the picked element");
        btn_showLowerLayerInfo.setSelected(callback.getVisualizationState().isShowInCanvasLowerLayerPropagation());
        btn_showUpperLayerInfo = new JToggleButton();
        btn_showUpperLayerInfo.setToolTipText("Shows the links in upper layers that carry traffic that appears in the picked element");
        btn_showUpperLayerInfo.setSelected(callback.getVisualizationState().isShowInCanvasUpperLayerPropagation());
        btn_showThisLayerInfo = new JToggleButton();
        btn_showThisLayerInfo.setToolTipText("Shows the links in the same layer as the picked element, that carry traffic that appears in the picked element");
        btn_showThisLayerInfo.setSelected(callback.getVisualizationState().isShowInCanvasThisLayerPropagation());
        btn_npChangeUndo = new JButton();
        btn_npChangeUndo.setToolTipText("Navigate back to the previous state of the network (last time the network design was changed)");
        btn_npChangeRedo = new JButton();
        btn_npChangeRedo.setToolTipText("Navigate forward to the next state of the network (when network design was changed");

        // MultiLayer control window
        JPopupMenu multiLayerPopUp = new JPopupMenu();
        multiLayerPopUp.add(multilayerControlPanel);
        JPopUpButton btn_multilayer = new JPopUpButton("", multiLayerPopUp);

        btn_increaseInterLayerDistance.setIcon(new ImageIcon(TopologyPanel.class.getResource("/resources/gui/increaseLayerDistance.png")));
        btn_decreaseInterLayerDistance.setIcon(new ImageIcon(TopologyPanel.class.getResource("/resources/gui/decreaseLayerDistance.png")));
        btn_multilayer.setIcon(new ImageIcon(TopologyPanel.class.getResource("/resources/gui/showLayerControl.png")));
        btn_showThisLayerInfo.setIcon(new ImageIcon(TopologyPanel.class.getResource("/resources/gui/showLayerPropagation.png")));
        btn_showUpperLayerInfo.setIcon(new ImageIcon(TopologyPanel.class.getResource("/resources/gui/showLayerUpperPropagation.png")));
        btn_showLowerLayerInfo.setIcon(new ImageIcon(TopologyPanel.class.getResource("/resources/gui/showLayerLowerPropagation.png")));
        btn_npChangeUndo.setIcon(new ImageIcon(TopologyPanel.class.getResource("/resources/gui/undoButton.png")));
        btn_npChangeRedo.setIcon(new ImageIcon(TopologyPanel.class.getResource("/resources/gui/redoButton.png")));

        btn_increaseInterLayerDistance.addActionListener(this);
        btn_decreaseInterLayerDistance.addActionListener(this);
        btn_showLowerLayerInfo.addActionListener(this);
        btn_showUpperLayerInfo.addActionListener(this);
        btn_showThisLayerInfo.addActionListener(this);
        btn_npChangeUndo.addActionListener(this);
        btn_npChangeRedo.addActionListener(this);

        multiLayerToolbar.add(new JToolBar.Separator());
        multiLayerToolbar.add(btn_multilayer);
        multiLayerToolbar.add(btn_increaseInterLayerDistance);
        multiLayerToolbar.add(btn_decreaseInterLayerDistance);
        multiLayerToolbar.add(btn_showLowerLayerInfo);
        multiLayerToolbar.add(btn_showUpperLayerInfo);
        multiLayerToolbar.add(btn_showThisLayerInfo);
        multiLayerToolbar.add(Box.createVerticalGlue());
        //multiLayerToolbar.add(btn_npChangeUndo);
        //multiLayerToolbar.add(btn_npChangeRedo);

        this.addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentResized(ComponentEvent e)
            {
                if (e.getComponent().getSize().getHeight() != 0 && e.getComponent().getSize().getWidth() != 0)
                {
                    canvas.zoomAll();
                }
            }
        });

        List<Component> children = SwingUtils.getAllComponents(this);
        for (Component component : children)
            if (component instanceof AbstractButton)
                component.setFocusable(false);

        if (ErrorHandling.isDebugEnabled())
        {
            canvas.getCanvasComponent().addMouseMotionListener(new MouseMotionAdapter()
            {
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

        final ITopologyCanvasPlugin popupPlugin = new PopupMenuPlugin(callback, this.canvas);
        addPlugin(new PanGraphPlugin(callback, canvas, MouseEvent.BUTTON1_MASK));
        if (callback.getVisualizationState().isNetPlanEditable() && getCanvas() instanceof JUNGCanvas)
            addPlugin(new AddLinkGraphPlugin(callback, canvas, MouseEvent.BUTTON1_MASK, MouseEvent.BUTTON1_MASK | MouseEvent.SHIFT_MASK));
        addPlugin(popupPlugin);
        if (callback.getVisualizationState().isNetPlanEditable())
            addPlugin(new MoveNodePlugin(callback, canvas, MouseEvent.BUTTON1_MASK | MouseEvent.CTRL_MASK));

        setBorder(BorderFactory.createTitledBorder(new LineBorder(Color.BLACK), "Network topology"));
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

    /**
     * Loads a network design from a {@code .n2p} file.
     *
     * @since 0.3.0
     */
    public void loadDesign()
    {
        if (callback.inOnlineSimulationMode()) return;

        try
        {
            checkNetPlanFileChooser();

            int rc = fc_netPlan.showOpenDialog(null);
            if (rc != JFileChooser.APPROVE_OPTION) return;

            // Disable OSM while loading the new topology
            boolean isOSMRunning = canvas.isOSMRunning();
            if (isOSMRunning) canvas.runDefaultView();

            NetPlan aux = fc_netPlan.readNetPlan();

            callback.setDesign(aux);
            final VisualizationState vs = callback.getVisualizationState();
            Pair<BidiMap<NetworkLayer, Integer>, Map<NetworkLayer, Boolean>> res =
                    vs.suggestCanvasUpdatedVisualizationLayerInfoForNewDesign(new HashSet<>(callback.getDesign().getNetworkLayers()));
            vs.setCanvasLayerVisibilityAndOrder(callback.getDesign(), res.getFirst(), res.getSecond());
            callback.updateVisualizationAfterNewTopology();
            callback.addNetPlanChange();

            // Reactivating the OSM Support
            if (isOSMRunning) canvas.runOSMSupport();
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

            callback.setDesign(netPlan);
            final VisualizationState vs = callback.getVisualizationState();
            Pair<BidiMap<NetworkLayer, Integer>, Map<NetworkLayer, Boolean>> res =
                    vs.suggestCanvasUpdatedVisualizationLayerInfoForNewDesign(new HashSet<>(callback.getDesign().getNetworkLayers()));
            vs.setCanvasLayerVisibilityAndOrder(callback.getDesign(), res.getFirst(), res.getSecond());
            callback.updateVisualizationAfterNewTopology();
            callback.addNetPlanChange();
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
                callback.addNetPlanChange();
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
}
