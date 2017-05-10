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
import com.net2plan.gui.plugins.networkDesign.topologyPane.jung.plugins.AddLinkGraphPlugin;
import com.net2plan.gui.plugins.networkDesign.topologyPane.jung.JUNGCanvas;
import com.net2plan.gui.plugins.networkDesign.topologyPane.jung.state.CanvasOption;
import com.net2plan.gui.plugins.networkDesign.topologyPane.plugins.MoveNodePlugin;
import com.net2plan.gui.plugins.networkDesign.topologyPane.plugins.PanGraphPlugin;
import com.net2plan.gui.plugins.networkDesign.topologyPane.plugins.PopupMenuPlugin;
import com.net2plan.gui.plugins.networkDesign.visualizationControl.VisualizationState;
import com.net2plan.gui.utils.FileDrop;
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
public class TopologyPanel extends JPanel
{
    private final GUINetworkDesign callback;
    private final ITopologyCanvas canvas;

    private final JPanel canvasPanel;

    private final TopologyTopBar topBar;
    private final TopologySideBar sideBar;

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
        this.callback = callback;

        try
        {
            // File chooser default directory.
            final File currentDir = SystemUtils.getCurrentDir();

            File defaultDesignDirectoryFilter = defaultDesignDirectory == null ? new File(currentDir + SystemUtils.getDirectorySeparator() + "workspace" + SystemUtils.getDirectorySeparator() + "data" + SystemUtils.getDirectorySeparator() + "networkTopologies") : defaultDesignDirectory;
            File defaultDemandDirectoryFilter = defaultDemandDirectory == null ? new File(currentDir + SystemUtils.getDirectorySeparator() + "workspace" + SystemUtils.getDirectorySeparator() + "data" + SystemUtils.getDirectorySeparator() + "trafficMatrices") : defaultDemandDirectory;

            // File chooser
            this.fc_netPlan = new FileChooserNetworkDesign(defaultDesignDirectoryFilter, DialogType.NETWORK_DESIGN);
            this.fc_demands = new FileChooserNetworkDesign(defaultDemandDirectoryFilter, DialogType.DEMANDS);

            // Declare canvas : Reflections
            this.canvas = canvasType.getDeclaredConstructor(GUINetworkDesign.class, TopologyPanel.class).newInstance(callback, this);

            // Add given plugins
            if (plugins != null)
                for (ITopologyCanvasPlugin plugin : plugins)
                    addPlugin(plugin);

            this.setLayout(new BorderLayout());

            // Top bar menu
            this.topBar = new TopologyTopBar(callback, this, canvas);
            this.add(topBar, BorderLayout.NORTH);

            // Canvas panel
            final JComponent canvasComponent = canvas.getCanvasComponent();
            canvasComponent.setBorder(LineBorder.createBlackLineBorder());

            this.canvasPanel = new JPanel(new BorderLayout());

            this.sideBar = new TopologySideBar(callback, canvas);

            this.canvasPanel.add(canvasComponent, BorderLayout.CENTER);
            this.canvasPanel.add(sideBar, BorderLayout.WEST);

            this.add(canvasPanel, BorderLayout.CENTER);

            // Buttons cannot be focusable
            List<Component> children = SwingUtils.getAllComponents(this);
            for (Component component : children)
                if (component instanceof AbstractButton)
                    component.setFocusable(false);

            // Action controllers
            this.addPlugin(new PanGraphPlugin(callback, canvas, MouseEvent.BUTTON1_MASK)); // Panning
            this.addPlugin(new PopupMenuPlugin(callback, this.canvas)); // Right button pop-up

            // Create links
            if (callback.getVisualizationState().isNetPlanEditable() && this.getCanvas() instanceof JUNGCanvas)
                addPlugin(new AddLinkGraphPlugin(callback, canvas, MouseEvent.BUTTON1_MASK, MouseEvent.BUTTON1_MASK | MouseEvent.SHIFT_MASK));

            // Move nodes
            if (callback.getVisualizationState().isNetPlanEditable())
                addPlugin(new MoveNodePlugin(callback, canvas, MouseEvent.BUTTON1_MASK | MouseEvent.CTRL_MASK));

            this.setBorder(BorderFactory.createTitledBorder(new LineBorder(Color.BLACK), "Network topology"));

            // Zoom all on resize
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

            // Key actions
            this.addKeyCombinationActions();

            // File drop listener
            new FileDrop(canvasComponent, new LineBorder(Color.BLACK), files ->
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
            });

            // DEBUG
            if (ErrorHandling.isDebugEnabled())
            {
                final JLabel position = new JLabel();

                canvas.getCanvasComponent().addMouseMotionListener(new MouseMotionAdapter()
                {
                    @Override
                    public void mouseMoved(MouseEvent e)
                    {
                        Point point = e.getPoint();
                        position.setText("view = " + point + ", NetPlan coord = " + canvas.getCanvasPointFromNetPlanPoint(point));
                    }
                });

                add(position, BorderLayout.SOUTH);
            }
        } catch (Exception e)
        {
            throw new RuntimeException(e);
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

    private void addKeyCombinationActions()
    {
        final TopologyPanel topologyPanel = TopologyPanel.this;

        callback.addKeyCombinationAction("Load design", new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                topologyPanel.loadDesign();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));

        callback.addKeyCombinationAction("Save design", new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                topologyPanel.saveDesign();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));

        callback.addKeyCombinationAction("Zoom in", new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                if (topologyPanel.getSize().getWidth() != 0 && topologyPanel.getSize().getHeight() != 0)
                    topologyPanel.getCanvas().zoomIn();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ADD, InputEvent.CTRL_DOWN_MASK), KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, InputEvent.CTRL_DOWN_MASK));

        callback.addKeyCombinationAction("Zoom out", new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                if (topologyPanel.getSize().getWidth() != 0 && topologyPanel.getSize().getHeight() != 0)
                    topologyPanel.getCanvas().zoomOut();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, InputEvent.CTRL_DOWN_MASK), KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, InputEvent.CTRL_DOWN_MASK));

        callback.addKeyCombinationAction("Zoom all", new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                if (topologyPanel.getSize().getWidth() != 0 && topologyPanel.getSize().getHeight() != 0)
                    topologyPanel.getCanvas().zoomAll();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_MULTIPLY, InputEvent.CTRL_DOWN_MASK));

        callback.addKeyCombinationAction("Take snapshot", new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                canvas.takeSnapshot();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_F12, InputEvent.CTRL_DOWN_MASK));

        callback.addKeyCombinationAction("Load traffic demands", new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                topologyPanel.loadTrafficDemands();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK));
    }

    public final void updateTopToolbar()
    {
        topBar.update();
    }

    public final void updateMultilayerPanel()
    {
        sideBar.getMultilayerControlPanel().refreshTable();
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
            assert fc_netPlan != null;

            int rc = fc_netPlan.showOpenDialog(null);
            if (rc != JFileChooser.APPROVE_OPTION) return;

            // Disable OSM while loading the new topology
            boolean isOSMRunning = canvas.getState() == CanvasOption.OSMState;
            if (isOSMRunning) canvas.setState(CanvasOption.ViewState);

            NetPlan aux = fc_netPlan.readNetPlan();

            callback.setDesign(aux);
            final VisualizationState vs = callback.getVisualizationState();
            Pair<BidiMap<NetworkLayer, Integer>, Map<NetworkLayer, Boolean>> res =
                    vs.suggestCanvasUpdatedVisualizationLayerInfoForNewDesign(new HashSet<>(callback.getDesign().getNetworkLayers()));
            vs.setCanvasLayerVisibilityAndOrder(callback.getDesign(), res.getFirst(), res.getSecond());
            callback.updateVisualizationAfterNewTopology();
            callback.addNetPlanChange();

            // Reactivating the OSM Support
            if (isOSMRunning) canvas.setState(CanvasOption.OSMState);
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
            assert fc_netPlan != null;

            NetPlan netPlan = new NetPlan(file);
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
            assert fc_demands != null;

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
     * Saves a network design to a {@code .n2p} file.
     *
     * @since 0.3.0
     */
    public void saveDesign()
    {
        try
        {
            assert fc_netPlan != null;

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
}
