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


package com.net2plan.gui.plugins.networkDesign.whatIfAnalysisPane;

import com.net2plan.gui.utils.ParameterValueDescriptionPanel;
import com.net2plan.gui.utils.RunnableSelector;
import com.net2plan.gui.plugins.networkDesign.visualizationControl.VisualizationState;
import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.interfaces.simulation.IEventGenerator;
import com.net2plan.interfaces.simulation.SimEvent;
import com.net2plan.internal.IExternal;
import com.net2plan.internal.SystemUtils;
import com.net2plan.internal.plugins.IGUIModule;
import com.net2plan.internal.sim.EndSimulationException;
import com.net2plan.internal.sim.IGUISimulationListener;
import com.net2plan.internal.sim.SimCore;
import com.net2plan.internal.sim.SimCore.SimState;
import com.net2plan.internal.sim.SimKernel;
import com.net2plan.utils.ClassLoaderUtils;
import com.net2plan.utils.Triple;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.*;
import java.util.List;

/**
 * Targeted to evaluate network designs from the offline tool simulating the
 * network operation. Different aspects such as network resilience,
 * connection-admission-control and time-varying traffic resource allocation,
 * or even mix of them, can be analyzed using the online simulator.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.3.0
 */
public class WhatIfAnalysisPane extends JPanel implements IGUISimulationListener, ActionListener
{
    private final GUINetworkDesign callback;
    private Thread simThread;
    private ParameterValueDescriptionPanel simulationConfigurationPanel;
    private RunnableSelector eventProcessorPanel;
    private SimKernel simKernel;
    private final JToggleButton btn_whatIfActivated;
    private Throwable lastWhatIfExecutionException;

    public WhatIfAnalysisPane(GUINetworkDesign callback)
    {
        super();
        this.callback = callback;

        simKernel = new SimKernel();
        simKernel.setGUIListener(this);

        File ALGORITHMS_DIRECTORY = new File(IGUIModule.CURRENT_DIR + SystemUtils.getDirectorySeparator() + "workspace");
        ALGORITHMS_DIRECTORY = ALGORITHMS_DIRECTORY.isDirectory() ? ALGORITHMS_DIRECTORY : IGUIModule.CURRENT_DIR;

        eventProcessorPanel = new RunnableSelector(SimKernel.getEventProcessorLabel(), "File", simKernel.getEventProcessorClass(), ALGORITHMS_DIRECTORY, new ParameterValueDescriptionPanel());

        simulationConfigurationPanel = new ParameterValueDescriptionPanel();
        simulationConfigurationPanel.setParameters(simKernel.getSimulationParameters());

        final JPanel upperButtonPlusLabelPanel = new JPanel();
        btn_whatIfActivated = new JToggleButton("Toggle What-If Mode");
        btn_whatIfActivated.setToolTipText("Activate/Deactivate What-if analysis tool");
        btn_whatIfActivated.addActionListener(this);
        btn_whatIfActivated.setFocusable(false);
        btn_whatIfActivated.setSelected(!callback.getVisualizationState().isWhatIfAnalysisActive());
        // Negate the last selection and run the listener.
        btn_whatIfActivated.doClick();
        upperButtonPlusLabelPanel.setLayout(new BorderLayout());
        upperButtonPlusLabelPanel.add(btn_whatIfActivated, BorderLayout.CENTER);

        final JTextArea upperText = new JTextArea();
        upperText.setFont(new JLabel().getFont());
        upperText.setBackground(new JLabel().getBackground());
        upperText.setLineWrap(true);
        upperText.setEditable(false);
        upperText.setWrapStyleWord(true);
        final String NEWLINE = String.format("%n");
        upperText.setText("The what-if analysis tool permits visualizing the changes produced in the network under "
                + "some events triggered in the user interface." + NEWLINE + NEWLINE
                + "The events that can be tested are:" + NEWLINE + NEWLINE
                + "- Setting failures/repairs in nodes and links. This can be done in the Nodes table, Links table "
                + "and Shared-risk group tables. " + NEWLINE + NEWLINE
                + "- Modifying the offered traffic of demands (only those not coupled to any upper layer link)." + NEWLINE + NEWLINE
                + "In the what-if analysis, the user modifications in the previous tables will trigger appropriate "
                + "events sent to the (built-in or user-developed) online reaction algorithm selected in the "
                + "table below. This algorithm is expected to receive the event, and generate the next network "
                + "state considering the network reaction for such event." + NEWLINE + NEWLINE
                + "Note: If the online reaction algorithm does not process correctly the input event, the design is unchanged."
        );
        this.setLayout(new BorderLayout());
        this.add(upperButtonPlusLabelPanel, BorderLayout.NORTH);

        final JSplitPane aux_Panel = new JSplitPane();
        aux_Panel.setLeftComponent(new JScrollPane(upperText));
        aux_Panel.setRightComponent(eventProcessorPanel);

        aux_Panel.setOrientation(JSplitPane.VERTICAL_SPLIT);
        aux_Panel.setResizeWeight(0.3);
        aux_Panel.setEnabled(false);
        aux_Panel.setDividerLocation(0.3);
        aux_Panel.setDividerSize(0      );

        this.add(aux_Panel, BorderLayout.CENTER);
    }

    public Throwable getLastWhatIfExecutionException()
    {
        return lastWhatIfExecutionException;
    }

    public void whatIfDemandOfferedTrafficModified(Demand demand, double newOfferedTraffic)
    {
    	if (demand.isCoupled()) throw new Net2PlanException ("What-if analysis changing the offered traffic in coupled demands is not accepted");
        this.lastWhatIfExecutionException = null;
        SimEvent.DemandModify modifyEvent = new SimEvent.DemandModify(demand, newOfferedTraffic, false);
        SimEvent event = new SimEvent(0, SimEvent.DestinationModule.EVENT_PROCESSOR, -1, modifyEvent);
        runSimulation(event);
    }

    public void whatIfDemandOfferedTrafficModified(MulticastDemand demand, double newOfferedTraffic)
    {
    	if (demand.isCoupled()) throw new Net2PlanException ("What-if analysis changing the offered traffic in coupled demands is not accepted");
        this.lastWhatIfExecutionException = null;
        SimEvent.MulticastDemandModify modifyEvent = new SimEvent.MulticastDemandModify(demand, newOfferedTraffic, false);
        SimEvent event = new SimEvent(0, SimEvent.DestinationModule.EVENT_PROCESSOR, -1, modifyEvent);
        runSimulation(event);
    }

    public void whatIfLinkNodesFailureStateChanged(Collection<Node> nodesToSetAsUp, Collection<Node> nodesToSetAsDown, Collection<Link> linksToSetAsUp, Collection<Link> linksToSetAsDown)
    {
        this.lastWhatIfExecutionException = null;
        SimEvent.NodesAndLinksChangeFailureState eventInfo = new SimEvent.NodesAndLinksChangeFailureState(nodesToSetAsUp, nodesToSetAsDown, linksToSetAsUp, linksToSetAsDown);
        SimEvent event = new SimEvent(0, SimEvent.DestinationModule.EVENT_PROCESSOR, -1, eventInfo);
        runSimulation(event);
    }


    @Override
    public void refresh(boolean forceRefresh)
    {
    }

    /**
     * Called by the SimKernel of the what-if analysis tool
     *
     * @param simulationState
     * @param reason
     */
    @SuppressWarnings("deprecation")
    @Override
    public void simulationStateChanged(SimCore.SimState simulationState, Throwable reason)
    {
        simulationConfigurationPanel.setEnabled(false);
        eventProcessorPanel.setEnabled(false);

        if (reason == null)
        {
            return;
        }

        if (reason instanceof EndSimulationException)
        {
            callback.setCurrentNetPlanDoNotUpdateVisualization(simKernel.getCurrentNetPlan());
            synchronized (this)
            {
                this.lastWhatIfExecutionException = null;
                eventProcessorPanel.setEnabled(true);
                this.notify(); // make the thread waiting for the simulation to finish to continue
            }
        } else
        {
            synchronized (this)
            {
                lastWhatIfExecutionException = reason;
                eventProcessorPanel.setEnabled(true);
                this.notify(); // make the thread waiting for the simulation to finish to continue
            }
        }
    }


    /**
     * Runs a short simulation to perform the what-if analysis. At the end, the resulting netplan is set
     *
     * @param eventToRun
     */
    private void runSimulation(SimEvent eventToRun)
    {
        try
        {
            if (simKernel.getSimCore().getSimulationState() == SimState.RUNNING)
                throw new Net2PlanException("Previous what-if analysis is in process");
            simKernel.getSimCore().setSimulationState(SimState.NOT_STARTED);
            simKernel.setNetPlan(callback.getDesign());
//            simKernel.getSimCore().setSimulationState(SimState.STOPPED);

            final Map<String, String> net2planParameters = Configuration.getNet2PlanOptions();
            final Map<String, String> simulationParameters = new HashMap<>();
            simulationParameters.put("disableStatistics", "true");
            simulationParameters.put("refreshTime", "100000");
            simulationParameters.put("simEvents", "-1");
            simulationParameters.put("transitoryEvents", "-1");
            simulationParameters.put("transitoryTime", "-1");
            simulationParameters.put("simTime", "-1");
            simulationParameters.put("disableStatistics", "true");
            simulationParameters.put("disableStatistics", "true");

            final IExternal eventGenerator = new IEventGenerator()
            {
                @Override
                public void processEvent(NetPlan currentNetPlan, SimEvent event)
                {
                }

                @Override
                public void initialize(NetPlan initialNetPlan, Map<String, String> algorithmParameters,
                                       Map<String, String> simulationParameters, Map<String, String> net2planParameters)
                {
                    scheduleEvent(eventToRun);
                }

                @Override
                public List<Triple<String, String, String>> getParameters()
                {
                    return new LinkedList<>();
                }

                @Override
                public String getDescription()
                {
                    return "";
                }
            };

            Triple<File, String, Class> aux = eventProcessorPanel.getRunnable();
            IExternal eventProcessor = ClassLoaderUtils.getInstance(aux.getFirst(), aux.getSecond(), simKernel.getEventProcessorClass() , null);
            Map<String, String> eventProcessorParameters = eventProcessorPanel.getRunnableParameters();
//			IExternal eventProcessor = new Online_evProc_ipOverWdm();
//			Map<String, String> eventProcessorParameters = InputParameter.getDefaultParameters(eventProcessor.getParameters());
            simKernel.configureSimulation(simulationParameters, net2planParameters, eventGenerator, new HashMap<String, String>(), eventProcessor, eventProcessorParameters);
            simKernel.initialize();
            simKernel.getSimCore().setSimulationState(SimCore.SimState.RUNNING);

            simThread = new Thread(simKernel.getSimCore());
            simThread.start();
        } catch (Throwable ex)
        {
            synchronized (this)
            {
                lastWhatIfExecutionException = ex;
                eventProcessorPanel.setEnabled(true);
                this.notify(); // make the thread waiting for the simulation to finish to continue
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        final Object src = e.getSource();
        if (src == btn_whatIfActivated)
        {
            final VisualizationState vs = callback.getVisualizationState();
            if (callback.inOnlineSimulationMode()) btn_whatIfActivated.setSelected(false);
            vs.setWhatIfAnalysisActive(btn_whatIfActivated.isSelected());
            eventProcessorPanel.setEnabled(btn_whatIfActivated.isSelected());
        }
    }

}
