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

import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.simulation.SimEvent;
import com.net2plan.internal.IExternal;
import com.net2plan.utils.Triple;

import java.util.List;
import java.util.Map;

/**
 * Abstract class for any runnable code used into the online simulator, either 
 * for event generators and processors.
 * 
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.3.0
 */
public abstract class ISimExternal implements IExternal
{
	SimKernel simKernel;
	
	/**
	 * <p>Throws an 'end of simulation' exception, so that the kernel finishes 
	 * immediately the simulation, and it triggers the 
	 * {@link #finish(java.lang.StringBuilder, double) finish()} method.</p>
	 * 
	 * <p><b>Important</b>: Please, do not catch this exception inside your code.</p>
	 * 
	 * @since 0.3.0
	 */
	public void endSimulation()
	{
		throw new EndSimulationException();
	}
	
	/**
	 * <p>Indicates to the kernel that the transitory should be finished after 
	 * processing the current event, triggering the 
	 * {@link #finishTransitory(double) finishTransitory()} method of both event 
	 * generator and processor.</p>
	 * 
	 * <p>If the transitory finished previously, this will have no effect.</p>
	 * 
	 * @since 0.3.0
	 */
	public void endTransitory()
	{
		if (simKernel == null) throw new RuntimeException("Bad");
		simKernel.endTransitory();
	}

	/**
	 * Returns an algorithm-specific report.
	 *
	 * @param output Container for the report
	 * @param simTime Current simulation time
	 * @return Report title (return {@code null}, or an empty {@code output} to omit it)
	 * @since 0.3.0
	 */
	public String finish(StringBuilder output, double simTime)
	{
		return null;
	}

	/**
	 * Performs some transitory-finished action.
	 * 
	 * @param simTime Current time
	 * @since 0.3.0
	 */
	public void finishTransitory(double simTime) {}
	
	/**
	 * Returns the description.
	 *
	 * @return Description
	 * @since 0.3.0
	 */
	@Override
	public abstract String getDescription();

	/**
	 * Returns the list of required parameters, where the first item of each element is the parameter name, the second one is the parameter value, and the third one is the parameter description.
	 *
	 * @return List of specific parameters
	 * @since 0.3.0
	 */
	@Override
	public abstract List<Triple<String, String, String>> getParameters();
	
	/**
	 * Initializes the algorithm (i.e. reading input parameters).
	 *
	 * @param initialNetPlan Initial network plan
	 * @param algorithmParameters A key-value map with specific algorithm parameters.<br><br><b>Important</b>: The algorithm developer is responsible to convert values from String to their respective type, and to check that values
	 * @param simulationParameters A key-value map with simulation parameters
	 * @param net2planParameters A key-value map with {@code Net2Plan}-wide configuration options
	 * @since 0.3.0
	 */
	public abstract void initialize(NetPlan initialNetPlan, Map<String, String> algorithmParameters, Map<String, String> simulationParameters, Map<String, String> net2planParameters);
	
	/**
	 * Processes the next event in the future event list. It is called only for 
	 * the destination module of the event. In addition, new events can be 
	 * scheduled using the {@link #scheduleEvent(com.net2plan.interfaces.simulation.SimEvent) scheduleEvent} 
	 * method.
	 *
	 * @param currentNetPlan Current network plan
	 * @param event Current event to be processed
	 * @since 0.3.0
	 */
	public abstract void processEvent(NetPlan currentNetPlan, SimEvent event);
	
	/**
	 * <p>Adds a new event to the future event list.</p>
	 *
	 * @param event Event to be scheduled
	 * @since 0.3.0
	 */
	public void scheduleEvent(SimEvent event)
	{
		if (simKernel == null) throw new RuntimeException("Bad");
		simKernel.scheduleEvent(event);
	}
	
	void setKernel(SimKernel simKernel)
	{
		this.simKernel = simKernel;
	}
}
