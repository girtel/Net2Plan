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


package com.net2plan.gui.utils.whatIfAnalysisPane;

import java.awt.BorderLayout;
import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JPanel;
import javax.swing.JTextArea;

import org.apache.commons.collections15.BidiMap;

import com.jom.JOMException;
import com.net2plan.gui.utils.IVisualizationCallback;
import com.net2plan.gui.utils.ParameterValueDescriptionPanel;
import com.net2plan.gui.utils.RunnableSelector;
import com.net2plan.gui.utils.topologyPane.visualizationControl.VisualizationState;
import com.net2plan.interfaces.networkDesign.Configuration;
import com.net2plan.interfaces.networkDesign.Demand;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.interfaces.simulation.IEventGenerator;
import com.net2plan.interfaces.simulation.SimEvent;
import com.net2plan.internal.ErrorHandling;
import com.net2plan.internal.IExternal;
import com.net2plan.internal.SystemUtils;
import com.net2plan.internal.plugins.IGUIModule;
import com.net2plan.internal.sim.EndSimulationException;
import com.net2plan.internal.sim.IGUISimulationListener;
import com.net2plan.internal.sim.SimCore;
import com.net2plan.internal.sim.SimCore.SimState;
import com.net2plan.internal.sim.SimKernel;
import com.net2plan.utils.ClassLoaderUtils;
import com.net2plan.utils.Pair;
import com.net2plan.utils.Triple;

/**
 * Targeted to evaluate network designs from the offline tool simulating the
 * network operation. Different aspects such as network resilience,
 * connection-admission-control and time-varying traffic resource allocation,
 * or even mix of them, can be analyzed using the online simulator.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.3.0
 */
public class WhatIfAnalysisPane extends JPanel implements IGUISimulationListener {

	private final IVisualizationCallback mainWindow;
    private JTextArea simInfo;
    private Thread simThread;
    private ParameterValueDescriptionPanel simulationConfigurationPanel;
    private RunnableSelector eventProcessorPanel;
    private SimKernel simKernel;
    
    public WhatIfAnalysisPane(IVisualizationCallback mainWindow)
    {
		super ();
		this.mainWindow = mainWindow;

		simKernel = new SimKernel();
        simKernel.setGUIListener(this);

        File ALGORITHMS_DIRECTORY = new File(IGUIModule.CURRENT_DIR + SystemUtils.getDirectorySeparator() + "workspace");
        ALGORITHMS_DIRECTORY = ALGORITHMS_DIRECTORY.isDirectory() ? ALGORITHMS_DIRECTORY : IGUIModule.CURRENT_DIR;

        eventProcessorPanel = new RunnableSelector(SimKernel.getEventProcessorLabel(), "File", simKernel.getEventProcessorClass(), ALGORITHMS_DIRECTORY, new ParameterValueDescriptionPanel());

        simulationConfigurationPanel = new ParameterValueDescriptionPanel();
        simulationConfigurationPanel.setParameters(simKernel.getSimulationParameters());

        this.setLayout(new BorderLayout());
        this.add(new JTextArea ("Message here"), BorderLayout.NORTH);
        this.add(eventProcessorPanel,  BorderLayout.CENTER);
	}
    
    public void whatIfDemandOfferedTrafficModified (Demand d)
    {
    	
    }
    public void whatIfLinkNodesFailureStateChanged (Collection<Link> linksToSetAsUp, Collection<Link> linksToSetAsDown, Collection<Node> nodesToSetAsUp, Collection<Node> nodesToSetAsDown)
    {
    	
    }
    
    
    @Override
    public void refresh(boolean forceRefresh) {    }

    /** Called by the SimKernel of the what-if analysis tool
     * @param simulationState
     * @param reason
     */
    @SuppressWarnings("deprecation")
    @Override
    public void simulationStateChanged(SimCore.SimState simulationState, Throwable reason) 
    {
        simulationConfigurationPanel.setEnabled(false);
        eventProcessorPanel.setEnabled(false);

        if (simulationState == SimState.NOT_STARTED || simulationState == SimState.PAUSED || simulationState == SimState.STEP || simulationState == SimState.STOPPED) 
        {
            mainWindow.setCurrentNetPlanDoNotUpdateVisualization(simKernel.getCurrentNetPlan());
            final VisualizationState vs = mainWindow.getVisualizationState();
    		Pair<BidiMap<NetworkLayer, Integer>, Map<NetworkLayer,Boolean>> res = 
    				vs.suggestCanvasUpdatedVisualizationLayerInfoForNewDesign(new HashSet<> (mainWindow.getDesign().getNetworkLayers()));
    		vs.setCanvasLayerVisibilityAndOrder(mainWindow.getDesign() , res.getFirst() , res.getSecond());
            mainWindow.updateVisualizationAfterNewTopology();
        }

        if (reason == null) return;

        if (reason instanceof EndSimulationException) 
        {
            ErrorHandling.showInformationDialog("Simulation finished", "Information");
            requestFocusInWindow();
        } else if (reason instanceof Net2PlanException || reason instanceof JOMException) {
            if (ErrorHandling.isDebugEnabled()) ErrorHandling.addErrorOrException(reason, WhatIfAnalysisPane.class);
            ErrorHandling.showErrorDialog(reason.getMessage(), "Error executing simulation");
        } else {
            ErrorHandling.addErrorOrException(reason, WhatIfAnalysisPane.class);
            ErrorHandling.showErrorDialog("Fatal error");
        }
    }

    
    /** Runs a short simulation to perform the what-if analysis. At the end, the resulting netplan is set 
     * @param eventToRun
     */
    private void runSimulation(SimEvent eventToRun) 
    {
        try 
        {
        	if (simKernel.getSimCore().getSimulationState() == SimState.RUNNING) throw new Net2PlanException ("Previous what-if analysis is in process");
            simKernel.getSimCore().setSimulationState(SimState.STOPPED);
            simKernel.setNetPlan(mainWindow.getDesign());

            final Map<String, String> net2planParameters = Configuration.getNet2PlanOptions();
            final Map<String, String> simulationParameters = new HashMap<> ();
            simulationParameters.put("disableStatistics", "true");
            simulationParameters.put("refreshTime", "-1");
            simulationParameters.put("simEvents", "-1");
            simulationParameters.put("transitoryEvents", "-1");
            simulationParameters.put("transitoryTime", "-1");
            simulationParameters.put("simTime", "-1");
            simulationParameters.put("disableStatistics", "true");
            simulationParameters.put("disableStatistics", "true");

            final IExternal eventGenerator = new IEventGenerator() 
            {
				@Override
				public void processEvent(NetPlan currentNetPlan, SimEvent event) {}
				
				@Override
				public void initialize(NetPlan initialNetPlan, Map<String, String> algorithmParameters,
						Map<String, String> simulationParameters, Map<String, String> net2planParameters)
				{
					scheduleEvent(eventToRun);
				}
				
				@Override
				public List<Triple<String, String, String>> getParameters() { return new LinkedList<> (); }
				
				@Override
				public String getDescription() { return ""; }
			};

			Triple<File, String, Class> aux = eventProcessorPanel.getRunnable();
            IExternal eventProcessor = ClassLoaderUtils.getInstance(aux.getFirst(), aux.getSecond(), simKernel.getEventProcessorClass());
            Map<String, String> eventProcessorParameters = eventProcessorPanel.getRunnableParameters();
            simKernel.configureSimulation(simulationParameters, net2planParameters, eventGenerator, new HashMap<String,String> (), eventProcessor, eventProcessorParameters);
            simKernel.initialize();
            simKernel.getSimCore().setSimulationState(SimCore.SimState.RUNNING);

            simThread = new Thread(simKernel.getSimCore());
            simThread.start();
        } catch (Net2PlanException ex) {
            if (ErrorHandling.isDebugEnabled()) ErrorHandling.addErrorOrException(ex, WhatIfAnalysisPane.class);
            ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to execute what-if analysis");
        } catch (Throwable ex) 
        {        	
            ErrorHandling.addErrorOrException(ex, WhatIfAnalysisPane.class);
            ErrorHandling.showErrorDialog("Error execution simulation");
        }
    }

}
