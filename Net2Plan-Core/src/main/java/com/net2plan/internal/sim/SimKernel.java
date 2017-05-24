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



 




package com.net2plan.internal.sim;

import com.jom.JOMException;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.simulation.*;
import com.net2plan.internal.CommandLineParser;
import com.net2plan.internal.ErrorHandling;
import com.net2plan.internal.IExternal;
import com.net2plan.utils.Pair;
import com.net2plan.utils.StringUtils;
import com.net2plan.utils.Triple;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Core-class for simulators. Users are only responsible to implement their
 * simulation-specific methods (check input data, process events...).
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.2
 */
public class SimKernel implements IEventCallback
{
	private final static DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
	
	private NetPlan initialNetPlan, currentNetPlan; //, currentNetPlan_view;
	private SimStats stats;
	private Map<String, String> simulationParameters;
	private Map<String, String> net2planParameters;
	private Map<String, String> eventGeneratorParameters;
	private Map<String, String> eventProcessorParameters;
	private IExternal eventGenerator;
	private IExternal eventProcessor;
	private boolean disableStatistics;
	private SimEvent lastEvent;
	private IGUISimulationListener guiListener;
	private Throwable lastReason = null;
	private final SimCore simCore;
//	private NetPlan originalNetPlan;
	
	/**
	 * Default constructor.
	 * 
	 * @since 0.2.2
	 */
	public SimKernel()
	{
		simCore = new SimCore(this);
		setNetPlan(new NetPlan());
	}

	@Override
	public void endTransitory()
	{
		if (lastEvent == null) getSimCore().finishTransitory(0);
		else getSimCore().finishTransitory(lastEvent.getEventTime());
	}
	
	@Override
	public void finishTransitory(double currentSimTime)
	{
		((IEventGenerator) eventGenerator).finishTransitory(currentSimTime);
		((IEventProcessor) eventProcessor).finishTransitory(currentSimTime);

		if (!disableStatistics) stats.reset(currentSimTime);
	}

	@Override
	public final void processEvent(SimEvent event)
	{
		lastEvent = event;
		simulationLoop(event);
	}

	@Override
	public void refresh(boolean forceRefresh)
	{
		if (guiListener != null)
		{
			guiListener.refresh(forceRefresh);
		}
		else
		{
			System.out.println(getSimulationInfo());
		}
	}

	@Override
	public void scheduleEvent(SimEvent event)
	{
		simCore.getFutureEventList().addEvent(event);
	}

	@Override
	public void simulationStateChanged(SimCore.SimState simulationState, Throwable reason)
	{
		this.lastReason = reason;

		if (guiListener != null)
		{
			guiListener.simulationStateChanged(simulationState, reason);
		}
		else
		{
			if (reason == null || reason instanceof EndSimulationException) return;

			if (reason instanceof Net2PlanException || reason instanceof JOMException)
			{
				if (ErrorHandling.isDebugEnabled()) ErrorHandling.addErrorOrException(reason, SimKernel.class);
				
				System.out.println("Error executing simulation");
				System.out.println();
				System.out.println(reason.getMessage());
			}
			else
			{
				System.out.println("Fatal error");
				System.out.println();
				ErrorHandling.printStackTrace(reason);
			}
		}
	}
	
	/**
	 * Configures the simulation.
	 * 
	 * @param simulationParameters Simulation parameters
	 * @param net2planParameters Net2Plan-wide configuration parameters
	 * @param eventGenerator An instance of the event generator
	 * @param eventGeneratorParameters Parameter-value map for the event generator
	 * @param eventProcessor An instance of the event processor
	 * @param eventProcessorParameters Parameter-value map for the event processor
	 * @since 0.2.2
	 */
	public void configureSimulation(Map<String, String> simulationParameters, Map<String, String> net2planParameters, IExternal eventGenerator, Map<String, String> eventGeneratorParameters, IExternal eventProcessor, Map<String, String> eventProcessorParameters)
	{
		this.simulationParameters = new LinkedHashMap<String, String>(simulationParameters);

		if (!simulationParameters.containsKey("disableStatistics"))	throw new Net2PlanException("'disableStatistics' parameter is not configured");
		disableStatistics = Boolean.parseBoolean(simulationParameters.get("disableStatistics"));

		if (!simulationParameters.containsKey("refreshTime")) throw new Net2PlanException("'refreshTime' parameter is not configured");
		double refreshTimeInSeconds = Double.parseDouble(simulationParameters.get("refreshTime"));
		simCore.setRefreshTimeInSeconds(refreshTimeInSeconds);

		if (!simulationParameters.containsKey("simEvents")) throw new Net2PlanException("'simEvents' parameter is not configured");
		long simEvents = Long.parseLong(simulationParameters.get("simEvents"));
		simCore.setTotalSimulationEvents(simEvents);

		if (!simulationParameters.containsKey("transitoryEvents")) throw new Net2PlanException("'transitoryEvents' parameter is not configured");
		long transitoryEvents = Long.parseLong(simulationParameters.get("transitoryEvents"));
		simCore.setTotalTransitoryEvents(transitoryEvents);

		if (!simulationParameters.containsKey("transitoryTime")) throw new Net2PlanException("'transitoryTime' parameter is not configured");
		double transitoryTime = Double.parseDouble(simulationParameters.get("transitoryTime"));
		simCore.setTotalTransitoryTime(transitoryTime);

		if (!simulationParameters.containsKey("simTime")) throw new Net2PlanException("'simTime' parameter is not configured");
		double simTime = Double.parseDouble(simulationParameters.get("simTime"));
		simCore.setTotalSimulationTime(simTime);
		
		if (!getEventGeneratorClass().isAssignableFrom(eventGenerator.getClass())) throw new RuntimeException("Bad - Event generator is not an instance of " + getEventGeneratorClass().getName());
		if (!getEventProcessorClass().isAssignableFrom(eventProcessor.getClass())) throw new RuntimeException("Bad - Event processor is not an instance of " + getEventProcessorClass().getName());

		this.net2planParameters = new LinkedHashMap<String, String>(net2planParameters);
		this.eventGenerator = eventGenerator;
		this.eventGeneratorParameters = new LinkedHashMap<String, String>(eventGeneratorParameters);
		this.eventProcessor = eventProcessor;
		this.eventProcessorParameters = new LinkedHashMap<String, String>(eventProcessorParameters);
	}

	/**
	 * Returns the current network plan corresponding to the network state.
	 * 
	 * @return Current network plan corresponding to the network state
	 * @since 0.2.2
	 */
	public NetPlan getCurrentNetPlan()
	{
	//	if (currentNetPlan_view == null) currentNetPlan_view = currentNetPlan.unmodifiableView();
//		if (currentNetPlan_view == null) currentNetPlan_view = currentNetPlan;
//		return currentNetPlan_view;
		return currentNetPlan;
	}

	/**
	 * Returns the class of the event generator.
	 * 
	 * @return Class of the event generator
	 * @since 0.2.2
	 */
	public Class<? extends IExternal> getEventGeneratorClass()
	{
		return IEventGenerator.class;
	}
	
	/**
	 * Returns the label for the event generator.
	 * 
	 * @return Label for the event generator
	 * @since 0.2.2
	 */
	public static String getEventGeneratorLabel()
	{
		return "Event generator";
	}

	/**
	 * Returns the class of the event processor.
	 * 
	 * @return Class of the event processor
	 * @since 0.2.2
	 */
	public Class<? extends IExternal> getEventProcessorClass()
	{
		return IEventProcessor.class;
	}

	/**
	 * Returns the label for the event processor.
	 * 
	 * @return Label for the event processor
	 * @since 0.2.2
	 */
	public static String getEventProcessorLabel()
	{
		return "Provisioning algorithm";
	}
	
	/**
	 * Returns the initial network plan.
	 * 
	 * @return The initial network plan
	 * @since 0.2.2
	 */
	public NetPlan getInitialNetPlan()
	{
		return initialNetPlan;
	}

	/**
	 * Returns a reference to the simulation core.
	 * 
	 * @return Reference to the simulation core
	 * @since 0.2.0
	 */
	public SimCore getSimCore()
	{
		return simCore;
	}

	/**
	 * Returns a brief simulation information report (current simulation time,
	 * last event processed...).
	 * 
	 * @return Simulation information
	 * @since 0.2.2
	 */
	public String getSimulationInfo()
	{
		if (simCore.getSimulationState() == SimCore.SimState.NOT_STARTED)
		{
			return "Simulation not started yet";
		}

		String NEWLINE = StringUtils.getLineSeparator();

		double simTime = simCore.getFutureEventList().getCurrentSimulationTime();
		double cpuTime = simCore.getCPUTime();
		long processedEvents = simCore.getFutureEventList().getNumberOfProcessedEvents();
		int pendingEvents = simCore.getFutureEventList().getNumberOfPendingEvents();
		double evToSecRatio = cpuTime == 0 ? 0 : (double) processedEvents / cpuTime;
		double simToWallRatio = cpuTime == 0 ? 0 : (double) simTime / cpuTime;
		double evToSimSecRatio = simTime == 0 ? 0 : (double) processedEvents / simTime;
		
		StringBuilder info = new StringBuilder();
		info.append(String.format("Current date: %s", DATE_FORMAT.format(Calendar.getInstance().getTime())));
		info.append(NEWLINE);
		info.append(String.format("Current simulation time: %s", StringUtils.secondsToYearsDaysHoursMinutesSeconds(simTime)));
		info.append(NEWLINE);
		info.append(String.format("Current CPU time: %s (%.3g simsec/sec)", StringUtils.secondsToYearsDaysHoursMinutesSeconds(cpuTime), simToWallRatio));
		info.append(NEWLINE);
		info.append(String.format("Number of processed events: %d (%.3g ev/sec, %.3g ev/simsec)", processedEvents, evToSecRatio, evToSimSecRatio));
		info.append(NEWLINE);
		info.append(String.format("Number of pending events: %d", pendingEvents));
		info.append(NEWLINE);
		info.append(String.format("Last event processed: %s", lastEvent == null ? "None" : lastEvent.toString()));
		info.append(NEWLINE);

		return info.toString();
	}

	/**
	 * Returns the parameters for the simulation.
	 * 
	 * @return Parameters for the simulation
	 * @since 0.2.2
	 */
	public final List<Triple<String, String, String>> getSimulationParameters()
	{
		List<Triple<String, String, String>> parameters = new LinkedList<Triple<String, String, String>>();
		parameters.add(Triple.of("disableStatistics", "#boolean# false", "Disable compilation of simulation statistics (only simulation information, and optionally algorithm-specific information, is collected)"));
		parameters.add(Triple.of("refreshTime", "10", "Refresh time (in seconds)"));
		parameters.add(Triple.of("simEvents", "-1", "Total simulation events (including transitory period) (-1 means no limit). In case that 'simTime' and 'simEvents' are specified, the transitory period will finish when one of the previous values is reached"));
		parameters.add(Triple.of("transitoryEvents", "-1", "Number of events for transitory period (-1 means no transitory period). In case that 'transitoryTime' and 'transitoryEvents' are specified, the transitory period will finish when one of the previous values is reached"));
		parameters.add(Triple.of("simTime", "-1", "Total simulation time (in seconds, including transitory period) (-1 means no limit). In case that 'simTime' and 'simEvents' are specified, the transitory period will finish when one of the previous values is reached"));
		parameters.add(Triple.of("transitoryTime", "-1", "Transitory time (in seconds) (-1 means no transitory period). In case that 'transitoryTime' and 'transitoryEvents' are specified, the transitory period will finish when one of the previous values is reached"));

		return parameters;
	}

	/**
	 * Returns the simulation report.
	 * 
	 * @return Simulation report
	 * @since 0.2.2
	 */
	public String getSimulationReport()
	{
		double simTime = getSimCore().getFutureEventList().getCurrentSimulationTime();
		double cpuTime = getSimCore().getCPUTime();
		long processedEvents = getSimCore().getFutureEventList().getNumberOfProcessedEvents();
		int pendingEvents = getSimCore().getFutureEventList().getNumberOfPendingEvents();
		double evToSecRatio = cpuTime == 0 ? 0 : (double) processedEvents / cpuTime;
		double simToWallRatio = cpuTime == 0 ? 0 : (double) simTime / cpuTime;
		double evToSimSecRatio = simTime == 0 ? 0 : (double) processedEvents / simTime;
		
		StringBuilder info = new StringBuilder();
		info.append("<html><head><title>Simulation report</title></head>");
		info.append("<body>");
		info.append("<h1>Simulation information</h1>");
		info.append("<center><table border='1'><tr><th>Parameter</th><th>Value</th></tr>");
		info.append(String.format("<tr><td>Current simulation time</td><td>%s</td></tr>", StringUtils.secondsToYearsDaysHoursMinutesSeconds(simTime)));
		info.append(String.format("<tr><td>Current CPU time</td><td>%s (%.3g simsec/sec)</td></tr>", StringUtils.secondsToYearsDaysHoursMinutesSeconds(cpuTime), simToWallRatio));
		info.append(String.format("<tr><td>Number of processed events</td><td>%d (%.3g ev/sec, %.3g ev/simsec)</td></tr>", processedEvents, evToSecRatio, evToSimSecRatio));
		info.append(String.format("<tr><td>Number of pending events</td><td>%d</td></tr>", pendingEvents));
		info.append("</table></center>");

		info.append("<h1>General results</h1>");

		if (stats == null)
		{
			info.append("<p>No results available since 'disableStatistics' was set to 'true'</p>");
		}
		else
		{
			info.append(stats.getResults(getSimCore().getFutureEventList().getCurrentSimulationTime()));
		}

		info.append("<h1>Problem-specific results</h1>");
		
		StringBuilder eventGeneratorHtml = new StringBuilder();
		String eventGeneratorOut = ((IEventGenerator) eventGenerator).finish(eventGeneratorHtml, simTime);

		if (eventGeneratorOut != null && eventGeneratorHtml.length() > 0)
		{
			info.append(String.format("<h2>%s</h2>", eventGeneratorOut));
			info.append(eventGeneratorHtml);
		}
		else
		{
			info.append("<p>No results from the event generator");
		}
		
		StringBuilder eventProcessorHtml = new StringBuilder();
		String eventProcessorOut = ((IEventProcessor) eventProcessor).finish(eventProcessorHtml, simTime);

		if (eventProcessorOut != null && eventProcessorHtml.length() > 0)
		{
			info.append(String.format("<h2>%s</h2>", eventProcessorOut));
			info.append(eventProcessorHtml);
		}
		else
		{
			info.append("<p>No results from the event processor");
		}

		info.append("</body></html>");

		return info.toString();
	}
	
	/**
	 * Initialize the simulation (event generator, event processor...). In case 
	 * that new events are to be generated, they should be scheduled calling to 
	 * {@link #scheduleEvent(com.net2plan.interfaces.simulation.SimEvent) scheduleEvent}.
	 * 
	 * @since 0.3.0
	 */
	public void initialize()
	{
		((ISimExternal) eventGenerator).setKernel(this);
		if (eventGenerator instanceof ICombinedEventGenerator)
			for(IEventGenerator internalEventGenerator : ((ICombinedEventGenerator) eventGenerator).eventGenerators)
				((ISimExternal) internalEventGenerator).setKernel(this);
		
		((ISimExternal) eventProcessor).setKernel(this);
		if (eventProcessor instanceof ICombinedEventProcessor)
			for(IEventProcessor internalEventProcessor : ((ICombinedEventProcessor) eventProcessor).eventProcessors)
				((ISimExternal) internalEventProcessor).setKernel(this);
		
		((ISimExternal) eventGenerator).initialize(currentNetPlan, eventGeneratorParameters, simulationParameters, net2planParameters);
		((ISimExternal) eventProcessor).initialize(currentNetPlan, eventProcessorParameters, simulationParameters, net2planParameters);
		//if (!disableStatistics) stats = new SimStats(initialNetPlan, currentNetPlan.unmodifiableView(), simulationParameters, net2planParameters);
		if (!disableStatistics) stats = new SimStats(currentNetPlan, simulationParameters, net2planParameters);
	}

	/**
	 * Initializes the current network state from a initial network plan.
	 * 
	 * @since 0.2.2
	 */
	public void initializeNetState()
	{
		if (getSimCore().getSimulationState() != SimCore.SimState.NOT_STARTED)
			throw new Net2PlanException("Network state cannot be re-initialized once the simulation was started");

		currentNetPlan = initialNetPlan.copy();
//		currentNetPlan_view = null;
	}

	/**
	 * Resets the simulation.
	 * 
	 * @since 0.2.2
	 */
	public void reset()
	{
		simCore.reset();
		initializeNetState();
		lastReason = null;
		stats = null;
	}

	/**
	 * This method is used during development to check algorithms for the online simulator.
	 * 
	 * @param simKernel Simulation kernel
	 * @param netPlan Input network design
	 * @param eventGenerator Event generator
	 * @param customEventGeneratorParameters Custom event generator parameters (null means empty)
	 * @param eventProcessor Event processor
	 * @param customEventProcessorParameters Custom event processor parameters (null means empty)
	 * @param customSimulatorParameters Custom simulator parameters (null means empty)
	 * @param net2planParameters Net2Plan parameters
	 * @return Simulation report
	 * @since 0.3.0
	 */
	public static Pair<NetPlan, String> runSimulation(SimKernel simKernel, NetPlan netPlan, IExternal eventGenerator, Properties customEventGeneratorParameters, IExternal eventProcessor, Properties customEventProcessorParameters, Properties customSimulatorParameters, Map<String, String> net2planParameters)
	{
		simKernel.setNetPlan(netPlan);
		
		List<Triple<String, String, String>> defaultEventGeneratorParameters = null;
		try { defaultEventGeneratorParameters = eventGenerator.getParameters(); }
		catch(UnsupportedOperationException ex) { }
		
		List<Triple<String, String, String>> defaultEventProcessorParameters = null;
		try { defaultEventProcessorParameters = eventProcessor.getParameters(); }
		catch(UnsupportedOperationException ex) { }
		
		Map<String, String> eventGeneratorParameters = CommandLineParser.getParameters(defaultEventGeneratorParameters, customEventGeneratorParameters);
		Map<String, String> eventProcessorParameters = CommandLineParser.getParameters(defaultEventProcessorParameters, customEventProcessorParameters);
		Map<String, String> simulationParameters = CommandLineParser.getParameters(simKernel.getSimulationParameters(), customSimulatorParameters);

		System.out.println("Net2Plan parameters");
		System.out.println("-----------------------------");
		System.out.println(StringUtils.mapToString(net2planParameters, "=", String.format("%n")));
		System.out.println();
		System.out.println("Simulation parameters");
		System.out.println("-----------------------------");
		System.out.println(StringUtils.mapToString(simulationParameters, "=", String.format("%n")));
		System.out.println();
		System.out.println(getEventGeneratorLabel() + " parameters");
		System.out.println("-----------------------------");
		System.out.println(eventGeneratorParameters.isEmpty() ? "None" : StringUtils.mapToString(eventGeneratorParameters, "=", String.format("%n")));
		System.out.println();
		System.out.println(getEventProcessorLabel() + " parameters");
		System.out.println("-----------------------------");
		System.out.println(eventProcessorParameters.isEmpty() ? "None" : StringUtils.mapToString(eventProcessorParameters, "=", String.format("%n")));
		System.out.println();
		
		simKernel.configureSimulation(simulationParameters, net2planParameters, eventGenerator, eventGeneratorParameters, eventProcessor, eventProcessorParameters);
		simKernel.initialize();
		System.out.println("Simulation started...");
		System.out.println();

		long init = System.nanoTime();
		simKernel.getSimCore().setSimulationState(SimCore.SimState.RUNNING);
		simKernel.getSimCore().run();
		long end = System.nanoTime();
		if (simKernel.lastReason != null && !(simKernel.lastReason instanceof EndSimulationException)) throw new RuntimeException(simKernel.lastReason);
		
		double totalSimTimeInSeconds = (end - init) / 1.0e9;
		String totalSimTime = StringUtils.secondsToYearsDaysHoursMinutesSeconds(totalSimTimeInSeconds);
		System.out.println(String.format("%n%nSimulation finished successfully in %s", totalSimTime));
		
		return Pair.of(simKernel.getCurrentNetPlan(), simKernel.getSimulationReport());
	}
	
	/**
	 *
	 * @param stateListener State listener
	 * @since 0.2.2
	 */
	public void setGUIListener(IGUISimulationListener stateListener)
	{
		if (this.guiListener != null) throw new RuntimeException("A state listener was already installed");
		this.guiListener = stateListener;
	}

	/**
	 * <p>Sets the initial network plan.</p>
	 * 
	 * <p><b>Important</b>: Once the simulation is started, the initial network
	 * plan cannot be changed.</p>
	 * 
	 * @param netPlan Initial network plan
	 * @since 0.2.2
	 */
	public void setNetPlan(NetPlan netPlan)
	{
		if (simCore.getSimulationState() != SimCore.SimState.NOT_STARTED)
			throw new Net2PlanException("Network design cannot be changed once simulation was started");
	
		initialNetPlan = netPlan.copy ();  initialNetPlan.setModifiableState(false);
		currentNetPlan = netPlan;
		//initializeNetState();
	}

	/**
	 * Processes the event in the simulation. In case that new events are to 
	 * be generated, they should be scheduled calling to 
	 * {@link #scheduleEvent(com.net2plan.interfaces.simulation.SimEvent) scheduleEvent}.
	 * 
	 * @param event Current event
	 * @since 0.3.0
	 */
	public void simulationLoop(SimEvent event)
	{
//		currentNetPlan_view = null;
		
		switch(event.getEventDestinationModule())
		{
			case EVENT_GENERATOR:
				((ISimExternal) eventGenerator).processEvent(currentNetPlan, event);
				break;
				
			case EVENT_PROCESSOR:
				((ISimExternal) eventProcessor).processEvent(currentNetPlan, event);
				break;
				
			default:
				throw new RuntimeException("Bad");
		}

		if (!disableStatistics) stats.computeNextState(event.getEventTime());
	}
}
