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




package com.net2plan.interfaces.simulation;

import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.internal.sim.ISimExternal;
import com.net2plan.utils.Triple;

import java.util.List;
import java.util.Map;

/**
 * <p>Abstract class that must be inherited so that an event generator can be executed 
 * inside the {@code Online network simulation} tool included within {@code Net2Plan}.</p>
 * 
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 */
public abstract class IEventGenerator extends ISimExternal
{
	/**
	 * <p>Throws an 'end of simulation' exception, so that the kernel finishes 
	 * immediately the simulation, and it triggers the 
	 * {@link #finish(java.lang.StringBuilder, double) finish()} method.</p>
	 * 
	 * <p><b>Important</b>: Please, do not catch this exception inside your code.</p>
	 * 
	 */
	@Override
	public final void endSimulation()
	{
		super.endSimulation();
	}
	
	/**
	 * <p>Indicates to the kernel that the transitory should be finished after 
	 * processing the current event, triggering the 
	 * {@link #finishTransitory(double) finishTransitory()} method of both event 
	 * generator and processor.</p>
	 * 
	 * <p>If the transitory finished previously, this will have no effect.</p>
	 * 
	 */
	@Override
	public final void endTransitory()
	{
		super.endTransitory();
	}

	/**
	 * Returns an algorithm-specific report.
	 *
	 * @param output Container for the report
	 * @param simTime Current simulation time
	 * @return Report title (return {@code null}, or an empty {@code output} to omit it)
	 */
	@Override
	public String finish(StringBuilder output, double simTime) { return null; }

	/**
	 * Performs some transitory-finished action.
	 * 
	 * @param simTime Current time
	 */
	@Override
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
	 * <p>Returns the list of required parameters, where the first item of each element 
	 * is the parameter name, the second one is the parameter value, and the third 
	 * one is the parameter description.</p>
	 * 
	 * <p>It is possible to define type-specific parameters if the default value is set 
	 * according to the following rules (but user is responsible of checking in its own code):</p>
	 * 
	 * <ul>
	 * <li>If the default value is #select#	and a set of space- or comma-separated values, 
	 * the GUI will show a combobox with all the values, where the first one will be the 
	 * selected one by default. For example: "#select# hops km" will allow choosing in the 
	 * GUI between hops and km, where hops will be the default value.</li>
	 * <li>If the default value is #boolean# and true or false, the GUI will show a checkbox, 
	 * where the default value will be true or false, depending on the value accompanying to 
	 * #boolean#. For example: "#boolean# true" will show a checkbox marked by default.</li>
	 * <li>If the default value is #algorithm#, the GUI will prepare a new interface to 
	 * select an algorithm as parameter. Three new fields will be added, including "_file", 
	 * "_classname" and "_parameters" suffixes to the indicated parameter name, refer to 
	 * the {@code .class} or {@code .jar} file where the code is located, the class name, 
	 * and a set of parameters (pair of key-values separated by commas, where individual 
	 * key and value are separated with an equal symbol. The same applies to reports (#report#), 
	 * event generators (#eventGenerator#) and event processors (#eventProcessor#).</li>
	 * </ul>
	 *
	 * @return List of specific parameters
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
	 */
	@Override
	public abstract void initialize(NetPlan initialNetPlan, Map<String, String> algorithmParameters, Map<String, String> simulationParameters, Map<String, String> net2planParameters);
	
	/**
	 * Processes the next event in the future event list. It is called only for 
	 * the destination module of the event. In addition, new events can be 
	 * scheduled using the {@link #scheduleEvent(com.net2plan.interfaces.simulation.SimEvent) scheduleEvent} 
	 * method.
	 *
	 * @param currentNetPlan Current network plan
	 * @param event Current event to be processed
	 */
	@Override
	public abstract void processEvent(NetPlan currentNetPlan, SimEvent event);
	
	/**
	 * <p>Adds a new event to the future event list.</p>
	 *
	 * @param event Event to be scheduled
	 * @since 0.3.0
	 */
	@Override
	public final void scheduleEvent(SimEvent event)
	{
		super.scheduleEvent(event);
	}
}
