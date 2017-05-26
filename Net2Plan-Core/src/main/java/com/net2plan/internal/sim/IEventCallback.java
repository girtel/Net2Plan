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

import com.net2plan.interfaces.simulation.SimEvent;

/**
 * Contract that must be fulfilled by classes (i.e. simulation module) which calls the simulation core.
 * 
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public interface IEventCallback extends IGUISimulationListener
{
	/**
	 * <p>Indicates to the {@link com.net2plan.internal.sim.SimCore SimCore} that 
	 * the transitory should be finished right now, triggering the 
	 * {@link #finishTransitory(double) finishTransitory()} method of the event callback.</p>
	 * 
	 * <p>If the transitory finished previously, this will have no effect.</p>
	 * 
	 * @since 0.3.0
	 */
	public void endTransitory();

	/**
	 * Performs some transitory-finished action.
	 * 
	 * @param currentSimTime Current simulation time
	 * @since 0.2.0
	 */
	public void finishTransitory(double currentSimTime);
	
	/**
	 * Processes a single event.
	 * 
	 * @param event Simulation event
	 * @since 0.3.0
	 */
	public void processEvent(SimEvent event);
	
	/**
	 * Adds a new event to the future event list.
	 * 
	 * @param event Simulation event
	 * @since 0.3.0
	 */
	public abstract void scheduleEvent(SimEvent event);
}
