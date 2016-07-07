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


package com.net2plan.gui.tools;

import com.jom.JOMException;
import com.net2plan.gui.utils.*;
import com.net2plan.interfaces.networkDesign.Configuration;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.simulation.SimEvent;
import com.net2plan.internal.ErrorHandling;
import com.net2plan.internal.IExternal;
import com.net2plan.internal.SystemUtils;
import com.net2plan.internal.sim.EndSimulationException;
import com.net2plan.internal.sim.IGUISimulationListener;
import com.net2plan.internal.sim.SimCore;
import com.net2plan.internal.sim.SimCore.SimState;
import com.net2plan.internal.sim.SimKernel;
import com.net2plan.utils.ClassLoaderUtils;
import com.net2plan.utils.StringUtils;
import com.net2plan.utils.Triple;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import static com.net2plan.internal.sim.SimCore.SimState.NOT_STARTED;

/**
 * Targeted to evaluate network designs from the offline tool simulating the
 * network operation. Different aspects such as network resilience,
 * connection-admission-control and time-varying traffic resource allocation,
 * or even mix of them, can be analyzed using the online simulator.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.3.0
 */
public class GUIOnlineSimulation extends IGUINetworkViewer implements ActionListener, IGUISimulationListener {
    private final static String TITLE = "Online simulation";

    private JButton btn_run, btn_step, btn_pause, btn_stop;
    private JButton btn_viewEventList, btn_updateReport;
    private JPanel simReport;
    private JCheckBox chk_refresh;
    private JPanel pan_simulationController;
    private JTextArea simInfo;
    private JToolBar toolbar;
    private JSplitPane splitPaneConfiguration;
    private Thread simThread;
    private ParameterValueDescriptionPanel simulationConfigurationPanel;
    private RunnableSelector eventGeneratorPanel, eventProcessorPanel;
    private SimKernel simKernel;
    private int simReportTab;

    /**
     * Default constructor.
     *
     * @since 0.3.0
     */
    public GUIOnlineSimulation() {
        super(TITLE);
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        try {
            Object src = e.getSource();

            if (src == btn_run) {
                runSimulation(false);
            } else if (src == btn_step) {
                runSimulation(true);
            } else if (src == btn_pause) {
                simKernel.getSimCore().setSimulationState(simKernel.getSimCore().getSimulationState() == SimState.PAUSED ? SimState.RUNNING : SimState.PAUSED);
            } else if (src == btn_stop) {
                simKernel.getSimCore().setSimulationState(SimState.STOPPED);
            } else if (src == btn_viewEventList) {
                viewFutureEventList();
            } else if (src == btn_updateReport) {
                updateSimReport();
            } else {
                throw new Net2PlanException("Bad");
            }
        } catch (Net2PlanException ex) {
            if (ErrorHandling.isDebugEnabled()) ErrorHandling.addErrorOrException(ex, GUIOnlineSimulation.class);
            ErrorHandling.showErrorDialog(ex.getMessage(), "Error executing simulation");
        } catch (Throwable ex) {
            ErrorHandling.addErrorOrException(ex, GUIOnlineSimulation.class);
            ErrorHandling.showErrorDialog("An error happened");
        }
    }

    @Override
    public boolean inOnlineSimulationMode() {
        return true;
    }

    @Override
    public void configure(JPanel contentPane) {
        simKernel = new SimKernel();
        simKernel.setGUIListener(this);

        super.configure(contentPane);

        File ALGORITHMS_DIRECTORY = new File(CURRENT_DIR + SystemUtils.getDirectorySeparator() + "workspace");
        ALGORITHMS_DIRECTORY = ALGORITHMS_DIRECTORY.isDirectory() ? ALGORITHMS_DIRECTORY : CURRENT_DIR;

        eventGeneratorPanel = new RunnableSelector(SimKernel.getEventGeneratorLabel(), "File", simKernel.getEventGeneratorClass(), ALGORITHMS_DIRECTORY, new ParameterValueDescriptionPanel());
        eventProcessorPanel = new RunnableSelector(SimKernel.getEventProcessorLabel(), "File", simKernel.getEventProcessorClass(), ALGORITHMS_DIRECTORY, new ParameterValueDescriptionPanel());

        simulationConfigurationPanel = new ParameterValueDescriptionPanel();
        simulationConfigurationPanel.setParameters(simKernel.getSimulationParameters());

        JTabbedPane configPane = new JTabbedPane();
        configPane.addTab(SimKernel.getEventGeneratorLabel(), eventGeneratorPanel);
        configPane.addTab(SimKernel.getEventProcessorLabel(), eventProcessorPanel);

        JPanel topPane = new JPanel(new MigLayout("insets 0 0 0 0", "[][grow][]", "[][grow]"));
        topPane.add(new JLabel("Simulation parameters"), "spanx 3, wrap");
        topPane.add(simulationConfigurationPanel, "spanx 3, grow, wrap");

        splitPaneConfiguration = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPaneConfiguration.setTopComponent(topPane);
        splitPaneConfiguration.setBottomComponent(configPane);

        splitPaneConfiguration.setResizeWeight(0.5);
        splitPaneConfiguration.addPropertyChangeListener(new ProportionalResizeJSplitPaneListener());
        splitPaneConfiguration.setBorder(BorderFactory.createTitledBorder(new LineBorder(Color.BLACK), "Simulation execution"));

        JPanel pan_execution = new JPanel(new MigLayout("fill, insets 0 0 0 0"));
        pan_execution.add(splitPaneConfiguration, "grow");

        btn_updateReport = new JButton("Update");
        btn_updateReport.setToolTipText("Update the simulation report");
        btn_updateReport.addActionListener(this);

        simReport = new JPanel();
        simReport.setLayout(new BorderLayout());
        simReport.add(btn_updateReport, BorderLayout.NORTH);

        addTab("Execution controller", pan_execution, 1);
        simReportTab = addTab("Simulation report", simReport, 2);

        simKernel.reset();
    }

    @Override
    protected JPanel configureLeftBottomPanel() {
        pan_simulationController = new JPanel(new MigLayout("fill, insets 0 0 0 0"));
        pan_simulationController.setBorder(BorderFactory.createTitledBorder(new LineBorder(Color.BLACK), "Simulation controller"));

        simInfo = new JTextArea();
        simInfo.setFont(new JLabel().getFont());
        DefaultCaret caret = (DefaultCaret) simInfo.getCaret();
        caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);

        toolbar = new JToolBar();

        btn_run = new JButton("Run");
        btn_run.addActionListener(this);
        addKeyCombinationAction("Run simulation", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    if (btn_run.isEnabled()) runSimulation(false);
                } catch (Net2PlanException ex) {
                    if (ErrorHandling.isDebugEnabled())
                        ErrorHandling.addErrorOrException(ex, GUIOnlineSimulation.class);
                    ErrorHandling.showErrorDialog(ex.getMessage(), "Error executing simulation");
                } catch (Throwable ex) {
                    ErrorHandling.addErrorOrException(ex, GUIOnlineSimulation.class);
                    ErrorHandling.showErrorDialog("An error happened");
                }

            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_U, InputEvent.CTRL_DOWN_MASK));

        btn_run.setToolTipText("Execute the simulation [CTRL + U]");
        btn_step = new JButton("Step");
        btn_step.addActionListener(this);
        btn_step.setToolTipText("Execute the next scheduled event and pause");
        btn_pause = new JButton("Pause/Continue");
        btn_pause.addActionListener(this);
        btn_pause.setToolTipText("Pause the simulation (if active) or continue (if paused)");
        btn_stop = new JButton("Stop");
        btn_stop.addActionListener(this);
        btn_stop.setToolTipText("Stop the simulation (it cannot be resumed later)");
        chk_refresh = new JCheckBox("Refresh");
        chk_refresh.setSelected(true);

        btn_viewEventList = new JButton("View FEL");
        btn_viewEventList.setToolTipText("View future event list (FEL)");
        btn_viewEventList.addActionListener(this);

        toolbar.setFloatable(false);
        toolbar.setRollover(true);
        toolbar.add(btn_run);
        toolbar.add(btn_step);
        toolbar.add(btn_pause);
        toolbar.add(btn_stop);
        toolbar.addSeparator();
        toolbar.add(chk_refresh);
        toolbar.add(Box.createHorizontalGlue());
        toolbar.add(btn_viewEventList);

        pan_simulationController.add(toolbar, "dock north");
        pan_simulationController.add(new JScrollPane(simInfo), "grow");

        return pan_simulationController;
    }


    @Override
    public String getDescription() {
        return getName();
    }

    @Override
    public NetPlan getDesign() {
        return simKernel.getCurrentNetPlan();
    }

    @Override
    public NetPlan getInitialDesign() {
        return simKernel.getInitialNetPlan();
    }

    @Override
    public KeyStroke getKeyStroke() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_3, InputEvent.ALT_DOWN_MASK);
    }

    @Override
    public String getMenu() {
        return "Tools|" + TITLE;
    }

    @Override
    public String getName() {
        return "Online simulation (GUI)";
    }

    @Override
    public List<Triple<String, String, String>> getParameters() {
        return null;
    }

    @Override
    public int getPriority() {
        return Integer.MAX_VALUE - 2;
    }

    @Override
    public void refresh(boolean forceRefresh) {
        if (chk_refresh.isSelected() || forceRefresh)
            updateSimulationInfo();
    }

    @Override
    protected void reset_internal() {
        switch (simKernel.getSimCore().getSimulationState()) {
            case NOT_STARTED:
            case STOPPED:
                break;

            default:
                simKernel.getSimCore().setSimulationState(SimState.STOPPED);
                break;
        }

        simKernel.reset();
        loadDesign(simKernel.getCurrentNetPlan());
    }

    @Override
    protected void setNetPlan(NetPlan netPlan) {
        simKernel.setNetPlan(netPlan);
    }

    @Override
    public void simulationStateChanged(SimCore.SimState simulationState, Throwable reason) {
        simulationConfigurationPanel.setEnabled(false);
        eventGeneratorPanel.setEnabled(false);
        eventProcessorPanel.setEnabled(false);

        switch (simulationState) {
            case NOT_STARTED:
                btn_run.setEnabled(true);
                btn_step.setEnabled(true);
                btn_pause.setEnabled(false);
                btn_stop.setEnabled(false);

                simulationConfigurationPanel.setEnabled(true);
                eventGeneratorPanel.setEnabled(true);
                eventProcessorPanel.setEnabled(true);

                if (simThread != null) {
                    try {
                        simThread.stop();
                    } catch (Throwable e) {
                    }
                }

                break;

            case RUNNING:
            case STEP:
                btn_run.setEnabled(false);
                btn_step.setEnabled(false);
                btn_pause.setEnabled(true);
                btn_stop.setEnabled(true);
                break;

            case PAUSED:
                btn_run.setEnabled(false);
                btn_step.setEnabled(true);
                btn_pause.setEnabled(true);
                btn_stop.setEnabled(true);
                break;

            case STOPPED:
                btn_run.setEnabled(false);
                btn_step.setEnabled(false);
                btn_pause.setEnabled(false);
                btn_stop.setEnabled(false);
                break;

            default:
                throw new RuntimeException("Bad - Unknown simulation state");
        }

        if (simulationState == SimState.NOT_STARTED || simulationState == SimState.PAUSED || simulationState == SimState.STOPPED) {
            updateSimulationInfo();
            topologyPanel.updateLayerChooser();
            resetView();
        }

        if (reason == null) return;

        if (reason instanceof EndSimulationException) {
            ErrorHandling.showInformationDialog("Simulation finished", "Information");
            updateSimReport();
            showTab(simReportTab);
        } else if (reason instanceof Net2PlanException || reason instanceof JOMException) {
            if (ErrorHandling.isDebugEnabled()) ErrorHandling.addErrorOrException(reason, GUIOnlineSimulation.class);
            ErrorHandling.showErrorDialog(reason.getMessage(), "Error executing simulation");
        } else {
            ErrorHandling.addErrorOrException(reason, GUIOnlineSimulation.class);
            ErrorHandling.showErrorDialog("Fatal error");
        }
    }

    @Override
    protected void updateLog(String text) {
    }

    private void updateSimulationLog(String text) {
        simInfo.setText(null);
        simInfo.setText(text);
        simInfo.setCaretPosition(0);
    }

    private void runSimulation(final boolean stepByStep) {
        try {
            if (simKernel.getSimCore().getSimulationState() == NOT_STARTED) {
                Map<String, String> simulationParameters = simulationConfigurationPanel.getParameters();
                Map<String, String> net2planParameters = Configuration.getNet2PlanOptions();

                Triple<File, String, Class> aux;

                aux = eventGeneratorPanel.getRunnable();
                Map<String, String> eventGeneratorParameters = eventGeneratorPanel.getRunnableParameters();
                IExternal eventGenerator = ClassLoaderUtils.getInstance(aux.getFirst(), aux.getSecond(), simKernel.getEventGeneratorClass());

                aux = eventProcessorPanel.getRunnable();
                IExternal eventProcessor = ClassLoaderUtils.getInstance(aux.getFirst(), aux.getSecond(), simKernel.getEventProcessorClass());
                Map<String, String> eventProcessorParameters = eventProcessorPanel.getRunnableParameters();

                simKernel.configureSimulation(simulationParameters, net2planParameters, eventGenerator, eventGeneratorParameters, eventProcessor, eventProcessorParameters);
                simKernel.initialize();
                simKernel.getSimCore().setSimulationState(stepByStep ? SimCore.SimState.STEP : SimCore.SimState.RUNNING);

                updateSimulationLog("Simulation running...");

                simThread = new Thread(simKernel.getSimCore());
                simThread.start();
            } else {
                simKernel.getSimCore().setSimulationState(stepByStep ? SimCore.SimState.STEP : SimCore.SimState.RUNNING);
            }
        } catch (Net2PlanException ex) {
            simKernel.reset();
            if (ErrorHandling.isDebugEnabled()) ErrorHandling.addErrorOrException(ex, GUIOnlineSimulation.class);
            ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to execute simulation");
        } catch (Throwable ex) {
            simKernel.reset();
            ErrorHandling.addErrorOrException(ex, GUIOnlineSimulation.class);
            ErrorHandling.showErrorDialog("Error execution simulation");
        }
    }

    private void updateSimulationInfo() {
        if (SwingUtilities.isEventDispatchThread()) {
            updateSimulationLog(simKernel.getSimulationInfo());
        } else {
            try {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        updateSimulationLog(simKernel.getSimulationInfo());
                    }
                });
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void updateSimReport() {
        try {
            if (simKernel.getSimCore().getSimulationState() == SimCore.SimState.NOT_STARTED) {
                throw new Net2PlanException("Simulation not started yet");
            }

            JPanel pane = new ReportBrowser(simKernel.getSimulationReport());
            try {
                simReport.remove(((BorderLayout) simReport.getLayout()).getLayoutComponent(BorderLayout.CENTER));
            } catch (Throwable ex) {
            }

            simReport.add(pane, BorderLayout.CENTER);
            simReport.revalidate();
        } catch (Net2PlanException ex) {
            if (ErrorHandling.isDebugEnabled()) ErrorHandling.addErrorOrException(ex, GUIOnlineSimulation.class);
            ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to show simulation report");
        } catch (Throwable ex) {
            ErrorHandling.addErrorOrException(ex, GUIOnlineSimulation.class);
            ErrorHandling.showErrorDialog("Error updating simulation report");
        }
    }

    /**
     * Shows the future event list.
     *
     * @since 0.3.0
     */
    public void viewFutureEventList() {
        final JDialog dialog = new JDialog();
        dialog.setTitle("Future event list");
        SwingUtils.configureCloseDialogOnEscape(dialog);
        dialog.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(new Dimension(500, 300));
        dialog.setLocationRelativeTo(null);
        dialog.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        dialog.setLayout(new MigLayout("fill, insets 0 0 0 0"));

        final String[] tableHeader = StringUtils.arrayOf("Id", "Time", "Priority", "Type", "To module", "Custom object");
        Object[][] data = new Object[1][tableHeader.length];

        DefaultTableModel model = new ClassAwareTableModel();
        model.setDataVector(new Object[1][tableHeader.length], tableHeader);

        JTable table = new AdvancedJTable(model);
        RowSorter<TableModel> sorter = new TableRowSorter<TableModel>(model);
        table.setRowSorter(sorter);
        JScrollPane scrollPane = new JScrollPane(table);
        new FixedColumnDecorator(scrollPane, 1);
        dialog.add(scrollPane, "grow");

        PriorityQueue<SimEvent> futureEventList = simKernel.getSimCore().getFutureEventList().getPendingEvents();
        if (!futureEventList.isEmpty()) {
            int numEvents = futureEventList.size();
            SimEvent[] futureEventList_array = futureEventList.toArray(new SimEvent[numEvents]);
            Arrays.sort(futureEventList_array, futureEventList.comparator());
            data = new Object[numEvents][tableHeader.length];

            for (int eventId = 0; eventId < numEvents; eventId++) {
//				List<SimAction> actions = futureEventList_array[eventId].getEventActionList();
                Object customObject = futureEventList_array[eventId].getEventObject();
                data[eventId][0] = eventId;
                data[eventId][1] = StringUtils.secondsToYearsDaysHoursMinutesSeconds(futureEventList_array[eventId].getEventTime());
                data[eventId][2] = futureEventList_array[eventId].getEventPriority();
                data[eventId][3] = futureEventList_array[eventId].getEventType();
                data[eventId][4] = futureEventList_array[eventId].getEventDestinationModule().toString();
                data[eventId][5] = customObject == null ? "none" : customObject;
            }
        }

        model.setDataVector(data, tableHeader);
        table.getTableHeader().addMouseListener(new ColumnFitAdapter());
        table.setDefaultRenderer(Double.class, new CellRenderers.NumberCellRenderer());

        dialog.setVisible(true);
    }
}
