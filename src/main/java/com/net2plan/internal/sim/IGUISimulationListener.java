/*******************************************************************************
 * Copyright (c) 2015 Pablo Pavon Mariño.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 * Contributors:
 *     Pablo Pavon Mariño - initial API and implementation
 ******************************************************************************/


 




package com.net2plan.internal.sim;

/**
 * Contract that must be fulfilled by graphical user interfaces of simulators.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.2
 */
public interface IGUISimulationListener
{
	/**
	 * Refreshes the simulation state. It is called by the simulation core when
	 * the time-to-refresh timer expires.
	 * 
	 * @param forceRefresh Flag to indicate whether or not the callback must refresh the simulation information
	 * @since 0.2.2
	 */
	public void refresh(boolean forceRefresh);

	/**
	 * Callback used when simulation state changes.
	 * 
	 * @param simulationState Simulation state
	 * @param reason Reason why simulation state changed. Typically it is only use when state changes to stopped
	 * @since 0.2.2
	 */
	public void simulationStateChanged(SimCore.SimState simulationState, Throwable reason);
}
