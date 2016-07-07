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


package com.net2plan.gui.utils.topology;

import com.net2plan.gui.utils.*;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.internal.Constants.DialogType;
import com.net2plan.internal.ErrorHandling;
import com.net2plan.internal.SystemUtils;
import com.net2plan.internal.plugins.ITopologyCanvas;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * <p>Wrapper class for the graph canvas.</p>
 * <p>
 * <p>Icons were taken from http://www.iconarchive.com/</p>
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public class TopologyPanel extends JPanel implements ActionListener//FrequentisBackgroundPanel implements ActionListener//JPanel implements ActionListener
{
    private final INetworkCallback callback;
    private ITopologyCanvas canvas;
    private JButton btn_load, btn_loadDemand, btn_save, btn_zoomIn, btn_zoomOut, btn_zoomAll, btn_takeSnapshot, btn_reset;
    private JToggleButton btn_showNodeNames, btn_showLinkIds, btn_showNonConnectedNodes;
    private FileChooserNetworkDesign fc_netPlan, fc_demands;
    private final File defaultDesignDirectory, defaultDemandDirectory;
    private JComboBox layerChooser;
    private JPanel layerChooserPane;
    private final JLabel position;

    /**
     * Simplified constructor that does not require to indicate default locations
     * for {@code .n2p} files.
     *
     * @param callback   Topology callback listening plugin events
     * @param canvasType Canvas type (i.e. JUNG)
     * @since 0.2.3
     */
    public TopologyPanel(INetworkCallback callback, Class<? extends ITopologyCanvas> canvasType) {
        this(callback, canvasType, null);
    }

    /**
     * Simplified constructor that does not require to indicate default locations
     * for {@code .n2p} files.
     *
     * @param callback   Topology callback listening plugin events
     * @param canvasType Canvas type (i.e. JUNG)
     * @param plugins    List of plugins to be included (it may be null)
     * @since 0.2.3
     */
    public TopologyPanel(INetworkCallback callback, Class<? extends ITopologyCanvas> canvasType, List<ITopologyCanvasPlugin> plugins) {
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
     * @since 0.2.0
     */
    public TopologyPanel(final INetworkCallback callback, File defaultDesignDirectory, File defaultDemandDirectory, Class<? extends ITopologyCanvas> canvasType, List<ITopologyCanvasPlugin> plugins) {
//		super (null, FrequentisBackgroundPanel.ACTUAL, 1.0f, 0.5f);

        File currentDir = SystemUtils.getCurrentDir();

        this.callback = callback;
        this.defaultDesignDirectory = defaultDesignDirectory == null ? new File(currentDir + SystemUtils.getDirectorySeparator() + "workspace" + SystemUtils.getDirectorySeparator() + "data" + SystemUtils.getDirectorySeparator() + "networkTopologies") : defaultDesignDirectory;
        this.defaultDemandDirectory = defaultDemandDirectory == null ? new File(currentDir + SystemUtils.getDirectorySeparator() + "workspace" + SystemUtils.getDirectorySeparator() + "data" + SystemUtils.getDirectorySeparator() + "trafficMatrices") : defaultDemandDirectory;

        try {
            canvas = canvasType.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
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

        layerChooser = new WiderJComboBox();
        layerChooserPane = new JPanel(new BorderLayout());
        layerChooserPane.add(new JLabel("Select layer: "), BorderLayout.WEST);
        layerChooserPane.add(layerChooser, BorderLayout.CENTER);
        layerChooser.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Object selectedItem = layerChooser.getSelectedItem();
                if (!(selectedItem instanceof StringLabeller))
                    ErrorHandling.showErrorDialog("Bad object", "Error selecting layer");

                long layerId = (Long) ((StringLabeller) selectedItem).getObject();
                NetPlan currentState = callback.getDesign();
                NetworkLayer layer = currentState.getNetworkLayerFromId(layerId);
//				System.out.println ("Select layer: layerId " + layerId + ", layer: " + layer);
                if (layer == null) throw new RuntimeException("Bad: " + layerId);
                currentState.setNetworkLayerDefault(layer);
                getCanvas().updateTopology(currentState);
                callback.updateNetPlanView();
                callback.layerChanged(layerId);
            }
        });

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(toolbar, BorderLayout.NORTH);
        topPanel.add(layerChooserPane, BorderLayout.SOUTH);

        add(topPanel, BorderLayout.NORTH);

        JComponent canvasComponent = canvas.getComponent();
        canvasComponent.setBorder(LineBorder.createBlackLineBorder());
        add(canvasComponent, BorderLayout.CENTER);

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
        btn_showNodeNames = new JToggleButton("Node names");
        btn_showNodeNames.setToolTipText("Show/hide node names");
        btn_showLinkIds = new JToggleButton("Link utilizations");
        btn_showLinkIds.setToolTipText("Show/hide link utilization, measured as the ratio between the total traffic in the link (including that in protection segments) and total link capacity (including that reserved by protection segments)");
        btn_showNonConnectedNodes = new JToggleButton("Non-connected nodes");
        btn_showNonConnectedNodes.setToolTipText("Show/hide non-connected nodes");
        JButton increaseNodeSize = new JButton();
        increaseNodeSize.setToolTipText("Increase node size");
        JButton decreaseNodeSize = new JButton();
        decreaseNodeSize.setToolTipText("Decrease node size");
        JButton increaseFontSize = new JButton();
        increaseFontSize.setToolTipText("Increase font size");
        JButton decreaseFontSize = new JButton();
        decreaseFontSize.setToolTipText("Decrease font size");

        btn_reset = new JButton("Reset");
        btn_reset.setToolTipText("Reset the user interface");

        btn_load.setIcon(new ImageIcon(TopologyPanel.class.getResource("/resources/gui/loadDesign.png")));
        btn_loadDemand.setIcon(new ImageIcon(TopologyPanel.class.getResource("/resources/gui/loadDemand.png")));
        btn_save.setIcon(new ImageIcon(TopologyPanel.class.getResource("/resources/gui/saveDesign.png")));
        btn_showNodeNames.setBorderPainted(true);
        btn_showLinkIds.setBorderPainted(true);
        btn_showNonConnectedNodes.setBorderPainted(true);
        btn_zoomIn.setIcon(new ImageIcon(TopologyPanel.class.getResource("/resources/gui/zoomIn.png")));
        btn_zoomOut.setIcon(new ImageIcon(TopologyPanel.class.getResource("/resources/gui/zoomOut.png")));
        btn_zoomAll.setIcon(new ImageIcon(TopologyPanel.class.getResource("/resources/gui/zoomAll.png")));
        btn_takeSnapshot.setIcon(new ImageIcon(TopologyPanel.class.getResource("/resources/gui/takeSnapshot.png")));
        increaseNodeSize.setIcon(new ImageIcon(TopologyPanel.class.getResource("/resources/gui/increaseNode.png")));
        decreaseNodeSize.setIcon(new ImageIcon(TopologyPanel.class.getResource("/resources/gui/decreaseNode.png")));
        increaseFontSize.setIcon(new ImageIcon(TopologyPanel.class.getResource("/resources/gui/increaseFont.png")));
        decreaseFontSize.setIcon(new ImageIcon(TopologyPanel.class.getResource("/resources/gui/decreaseFont.png")));

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
        toolbar.add(increaseNodeSize);
        toolbar.add(decreaseNodeSize);
        toolbar.add(increaseFontSize);
        toolbar.add(decreaseFontSize);
        toolbar.add(Box.createHorizontalGlue());
        toolbar.add(btn_reset);

        increaseNodeSize.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                getCanvas().increaseNodeSize();
            }
        });

        decreaseNodeSize.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                getCanvas().decreaseNodeSize();
            }
        });

        increaseFontSize.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                getCanvas().increaseFontSize();
            }
        });

        decreaseFontSize.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                getCanvas().decreaseFontSize();
            }
        });

        List<Component> children = SwingUtils.getAllComponents(this);
        for (Component component : children)
            if (component instanceof AbstractButton)
                component.setFocusable(false);

        if (ErrorHandling.isDebugEnabled()) {
            canvas.getInternalComponent().addMouseMotionListener(new MouseMotionListener() {
                @Override
                public void mouseDragged(MouseEvent e) {
                }

                @Override
                public void mouseMoved(MouseEvent e) {
                    Point point = e.getPoint();
                    position.setText("view = " + point + ", layout = " + canvas.convertViewCoordinatesToRealCoordinates(point));
                }
            });

            position = new JLabel();
            add(position, BorderLayout.SOUTH);
        } else {
            position = null;
        }

        new FileDrop(canvasComponent, new LineBorder(Color.BLACK), new FileDrop.Listener() {
            @Override
            public void filesDropped(File[] files) {
                for (File file : files) {
                    try {
                        if (!file.getName().toLowerCase(Locale.getDefault()).endsWith(".n2p")) return;
                        loadDesignFromFile(file);
                        break;
                    } catch (Throwable e) {
                        break;
                    }
                }
            }
        });

        btn_showNodeNames.setSelected(false);
        btn_showLinkIds.setSelected(false);
        btn_showNonConnectedNodes.setSelected(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();

        if (src == btn_load) {
            loadDesign();
        } else if (src == btn_loadDemand) {
            loadTrafficDemands();
        } else if (src == btn_save) {
            saveDesign();
        } else if (src == btn_showNodeNames) {
            canvas.showNodeNames(btn_showNodeNames.isSelected());
        } else if (src == btn_showLinkIds) {
            canvas.showLinkLabels(btn_showLinkIds.isSelected());
        } else if (src == btn_showNonConnectedNodes) {
            canvas.showNonConnectedNodes(btn_showNonConnectedNodes.isSelected());
        } else if (src == btn_takeSnapshot) {
            takeSnapshot();
        } else if (src == btn_zoomIn) {
            zoomIn();
        } else if (src == btn_zoomOut) {
            zoomOut();
        } else if (src == btn_zoomAll) {
            zoomAll();
        } else if (src == btn_reset) {
            callback.reset();
        }
    }

    /**
     * Adds a new plugin to the canvas.
     *
     * @param plugin Plugin to be added
     * @since 0.3.0
     */
    public void addPlugin(ITopologyCanvasPlugin plugin) {
        canvas.addPlugin(plugin);
    }

    private void checkNetPlanFileChooser() {
        if (fc_netPlan == null) {
            fc_netPlan = new FileChooserNetworkDesign(defaultDesignDirectory, DialogType.NETWORK_DESIGN);
        }
    }

    private void checkDemandFileChooser() {
        if (fc_demands == null) {
            fc_demands = new FileChooserNetworkDesign(defaultDemandDirectory, DialogType.DEMANDS);
        }
    }

    private String createLayerName(long layerId) {
        final NetworkLayer layer = callback.getDesign().getNetworkLayerFromId(layerId);
        return "Layer " + layer.getIndex() + (layer.getName().isEmpty() ? "" : ": " + layer.getName());
    }

    /**
     * Returns a reference to the topology canvas.
     *
     * @return Reference to the topology canvas
     * @since 0.2.3
     */
    public ITopologyCanvas getCanvas() {
        return canvas;
    }

    private StringLabeller getLayerItem(long layerId) {
        int numLayers = layerChooser.getItemCount();
        for (int l = 0; l < numLayers; l++) {
            StringLabeller item = (StringLabeller) layerChooser.getItemAt(l);
            if (layerId == (Long) item.getObject()) return item;
        }

        throw new RuntimeException("Bad");
    }

    /**
     * Loads a network design from a {@code .n2p} file.
     *
     * @since 0.3.0
     */
    public void loadDesign() {
        try {
            checkNetPlanFileChooser();

            int rc = fc_netPlan.showOpenDialog(null);
            if (rc != JFileChooser.APPROVE_OPTION) return;

            NetPlan aux = fc_netPlan.readNetPlan();

            aux.checkCachesConsistency();


            callback.loadDesign(aux);


        } catch (Net2PlanException ex) {
            if (ErrorHandling.isDebugEnabled()) ErrorHandling.addErrorOrException(ex, TopologyPanel.class);
            ErrorHandling.showErrorDialog(ex.getMessage(), "Error loading network design");
        } catch (Throwable ex) {
            ErrorHandling.addErrorOrException(ex, TopologyPanel.class);
            ErrorHandling.showErrorDialog("Error loading network design");
        }
    }

    private void loadDesignFromFile(File file) {
        try {
            NetPlan netPlan = new NetPlan(file);
            checkNetPlanFileChooser();
            fc_netPlan.setCurrentDirectory(file.getParentFile());

            callback.loadDesign(netPlan);


        } catch (Net2PlanException ex) {
            if (ErrorHandling.isDebugEnabled()) ErrorHandling.addErrorOrException(ex, TopologyPanel.class);
            ErrorHandling.showErrorDialog(ex.getMessage(), "Error loading network design");
        } catch (Throwable ex) {
            ErrorHandling.addErrorOrException(ex, TopologyPanel.class);
            ErrorHandling.showErrorDialog("Error loading network design");
        }
    }

    /**
     * Loads traffic demands from a {@code .n2p} file, overriding current demands.
     *
     * @since 0.3.0
     */
    public void loadTrafficDemands() {
        try {
            checkDemandFileChooser();

            int rc = fc_demands.showOpenDialog(null);
            if (rc != JFileChooser.APPROVE_OPTION) return;

            NetPlan aux = fc_demands.readDemands();
            callback.loadTrafficDemands(aux);
        } catch (Net2PlanException ex) {
            if (ErrorHandling.isDebugEnabled()) ErrorHandling.addErrorOrException(ex, TopologyPanel.class);
            ErrorHandling.showErrorDialog(ex.getMessage(), "Error loading traffic demands");
        } catch (Exception ex) {
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
    public void refreshLayerName(long layerId) {
        StringLabeller item = getLayerItem(layerId);
        item.setLabel(createLayerName(layerId));

        revalidate();
        repaint();
    }

    /**
     * Saves a network design to a {@code .n2p} file.
     *
     * @since 0.3.0
     */
    public void saveDesign() {
        try {
            checkNetPlanFileChooser();

            int rc = fc_netPlan.showSaveDialog(null);
            if (rc != JFileChooser.APPROVE_OPTION) return;

            NetPlan netPlan = callback.getDesign();
            if (netPlan.getNodes().isEmpty()) throw new Net2PlanException("Design is empty");

            fc_netPlan.saveNetPlan(netPlan);
            ErrorHandling.showInformationDialog("Design saved successfully", "Save design");
        } catch (Net2PlanException ex) {
            if (ErrorHandling.isDebugEnabled()) ErrorHandling.addErrorOrException(ex, TopologyPanel.class);
            ErrorHandling.showErrorDialog(ex.getMessage(), "Error saving network design");
        } catch (Throwable ex) {
            ErrorHandling.addErrorOrException(ex, TopologyPanel.class);
            ErrorHandling.showErrorDialog("Error saving network design");
        }
    }

    /**
     * Allows setting the current layer.
     *
     * @param layerId Layer identifier
     * @since 0.3.1
     */
    public void selectLayer(long layer) {
        long currentLayerId = (Long) ((StringLabeller) layerChooser.getSelectedItem()).getObject();
        if (layer == currentLayerId) return;

        layerChooser.setSelectedItem(getLayerItem(layer));
    }

    /**
     * Configures the topology panel to allow (or not) loading of external traffic demand files.
     *
     * @param isAllowed Indicates whether or not it is allowed to load traffic demand files.
     * @since 0.3.0
     */
    public void setAllowLoadTrafficDemand(boolean isAllowed) {
        btn_loadDemand.setVisible(isAllowed);
    }

    /**
     * Take a snapshot of the canvas.
     *
     * @since 0.3.0
     */
    public void takeSnapshot() {
        canvas.takeSnapshot();
    }

    /**
     * Updates the layer chooser.
     *
     * @since 0.3.1
     */
    public final void updateLayerChooser() {
        ActionListener[] al = layerChooser.getActionListeners();
        for (ActionListener a : al) layerChooser.removeActionListener(a);

        layerChooser.removeAllItems();

        NetPlan currentState = callback.getDesign();

        Collection<Long> layerIds = currentState.getNetworkLayerIds();

        if (ErrorHandling.isDebugEnabled()) currentState.checkCachesConsistency();

        for (long layerId : layerIds)
            layerChooser.addItem(StringLabeller.of(layerId, createLayerName(layerId)));

        for (ActionListener a : al) layerChooser.addActionListener(a);

        layerChooser.setSelectedIndex(currentState.getNetworkLayerDefault().getIndex()); // PABLO: AQUI SE PIERDEN LOS LINKS!!!!

        layerChooserPane.setVisible(layerChooser.getItemCount() > 1);

        revalidate();
    }

    /**
     * Makes zoom-all from the center of the view.
     *
     * @since 0.3.0
     */
    public void zoomAll() {
        canvas.zoomAll();
    }

    /**
     * Makes zoom-in from the center of the view.
     *
     * @since 0.3.0
     */
    public void zoomIn() {
        canvas.zoomIn();
    }

    /**
     * Makes zoom-out from the center of the view.
     *
     * @since 0.3.0
     */
    public void zoomOut() {
        canvas.zoomOut();
    }
}
