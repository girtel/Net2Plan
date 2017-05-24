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

import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.simulation.SimEvent;
import com.net2plan.internal.Constants.UserInterface;
import com.net2plan.internal.SystemUtils;

/**
 * Core-class of the discrete event simulator.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public final class SimCore implements Runnable
{
	/**
	 * Possible simulation states.
	 * 
	 * @since 0.2.0
	 */
	public enum SimState
	{
		/**
		 * Simulation is not started.
		 * 
		 * @since 0.2.0
		 */
		NOT_STARTED,

		/**
		 * Simulation is running.
		 * 
		 * @since 0.2.0
		 */
		RUNNING,

		/**
		 * Simulation is processing the current head event in the future event 
		 * list, and then will be paused.
		 * 
		 * @since 0.2.0
		 */
		STEP,

		/**
		 * Simulation is paused.
		 * 
		 * @since 0.2.0
		 */
		PAUSED,

		/**
		 * Simulation is stopped.
		 * 
		 * @since 0.2.0
		 */
		STOPPED;
		
		@Override
		public String toString() 
		{
		    switch(this) 
		    {
		      case NOT_STARTED: return "NOT_STARTED";
		      case RUNNING: return "RUNNING";
		      case PAUSED: return "PAUSED";
		      case STEP: return "STEP";
		      case STOPPED: return "STOPPED";
		      default: throw new RuntimeException ();
		    }
		}
	};
	
	private final IEventCallback callback;
	private final FutureEventList futureEventList;
	private double cpuTime;
	private double refreshTimeInSeconds;
	private double timeSinceLastRefresh;
	private long totalSimEvents;
	private long totalTransitoryEvents;
	private double totalSimTime;
	private double totalTransitoryTime;
	private boolean isInTransitory;
	private SimState simulationState;
	private boolean processingEvent;

	/**
	 * Default constructor.
	 * 
	 * @param callback Reference to the callback that acts as simulation kernel
	 * @since 0.2.0
	 */
	public SimCore(IEventCallback callback)
	{
		this.callback = callback;
		futureEventList = new FutureEventList();

		reset();
	}

	@Override
	public void run()
	{
		if (simulationState == SimState.NOT_STARTED) throw new RuntimeException("Bad - Simulation not started yet");

		isInTransitory = true;
		if (totalTransitoryEvents == -1 && totalTransitoryTime == -1) isInTransitory = false;
		while (simulationState != SimState.STOPPED)
		{
			while (futureEventList.hasMoreEvents())
			{
				synchronized (callback)
				{
					double nextEventTime = futureEventList.getNextEventSimulationTime();
					if (nextEventTime == -1) throw new RuntimeException("Bad");

					if (isInTransitory)
					{
						if (totalTransitoryTime != -1 && nextEventTime >= totalTransitoryTime)
						{
							finishTransitory(totalTransitoryTime);
						}
						else if (totalTransitoryEvents != -1 && futureEventList.getNumberOfProcessedEvents() == totalTransitoryEvents)
						{
							finishTransitory(futureEventList.getCurrentSimulationTime());
						}
					}

					if (totalSimTime != -1 && nextEventTime >= totalSimTime)
					{
						setSimulationState(SimState.STOPPED, new EndSimulationException());
						return; // this kills the thread
					}
					else if (totalSimEvents != -1 && futureEventList.getNumberOfProcessedEvents() == totalSimEvents)
					{
						setSimulationState(SimState.STOPPED, new EndSimulationException());
						return;  // this kills the thread
					}

					/* Process next event in the future event list */
					long start = System.nanoTime();

					SimEvent event = futureEventList.getNextEvent();
					processingEvent = true;
					
					try
					{
						if (event == null) throw new RuntimeException("Event is a null object");
						callback.processEvent(event);
					}
					catch (Throwable e)
					{
						processingEvent = false;
						setSimulationState(SimCore.SimState.STOPPED, e);

						long end = System.nanoTime();
						cpuTime += ((double) (end - start)) / 1e9;
						callback.refresh(true);

						return;  // this kills the thread
					}

					processingEvent = false;
					long end = System.nanoTime();

					cpuTime += ((double) (end - start)) / 1e9;

					if (cpuTime - timeSinceLastRefresh >= refreshTimeInSeconds)
					{
						callback.refresh(false);
						timeSinceLastRefresh = cpuTime;
					}

					if (futureEventList.getNumberOfProcessedEvents() == Long.MAX_VALUE)
					{
						setSimulationState(SimState.STOPPED);
						return;  // this kills the thread
					}

					if (simulationState == SimState.STEP)
					{
						setSimulationState(SimState.PAUSED);
					}

					if (simulationState != SimState.RUNNING)
					{
						break;
					}
				}
			}

			callback.refresh(true);
			timeSinceLastRefresh = cpuTime;

			if (!futureEventList.hasMoreEvents())
			{
				setSimulationState(SimState.STOPPED, new EndSimulationException());
				return;  // this kills the thread
			}
			
			if (SystemUtils.getUserInterface() == UserInterface.CLI)
			{
				setSimulationState(SimState.STOPPED, new EndSimulationException());
			}

			simulationState = SimState.PAUSED;

			while (simulationState == SimState.PAUSED)
			{
				try
				{
					Thread.sleep(1);
				}
				catch (InterruptedException ex)
				{
					setSimulationState(SimState.STOPPED);
					break;
				}
			}
		}
	}
	
	private void checkSimulationNotStartedYet()
	{
		if (simulationState != SimState.NOT_STARTED)
		{
			throw new RuntimeException("Simulation was already started. No configuration changes allowed");
		}
	}

	/**
	 * Finishes the transitory of the simulation. If the transitory was already 
	 * finished, no further action is made.
	 * 
	 * @param simTime Current simulation time
	 * @since 0.2.0
	 */
	public void finishTransitory(double simTime)
	{
		if (isInTransitory)
		{
			callback.finishTransitory(simTime);
			isInTransitory = false;
		}
	}

	/**
	 * Returns the current CPU time spent in the simulation.
	 * 
	 * @return Total CPU time spent in simulation
	 * @since 0.2.0
	 */
	public double getCPUTime()
	{
		return cpuTime;
	}

	/**
	 * Returns a reference to the future event list.
	 *
	 * @return Reference to the future event list
	 * @since 0.2.0
	 */
	public FutureEventList getFutureEventList()
	{
		return futureEventList;
	}

	/**
	 * Returns the current simulation state.
	 * 
	 * @return Current simulation state
	 * @since 0.2.0
	 */
	public SimState getSimulationState()
	{
		return simulationState;
	}

	/**
	 * Resets the simulation.
	 *
	 * @since 0.2.2
	 */
	public void reset()
	{
		cpuTime = 0;
		futureEventList.reset();
		timeSinceLastRefresh = 0;

		refreshTimeInSeconds = 60;
		totalSimEvents = -1;
		totalTransitoryEvents = -1;
		totalSimTime = -1;
		totalTransitoryTime = -1;
		isInTransitory = true;

		processingEvent = false;
		setSimulationState(SimState.NOT_STARTED);
	}
	
	/**
	 * <p>Sets the time to refresh the simulation log.</p>
	 *
	 * <p><b>Important</b>: This method only can be executed before the simulation starts.</p>
	 * 
	 * @param refreshTimeInSeconds Refresh time (to avoid refreshing set it to an arbitrarely large value)
	 * @since 0.2.0
	 */
	public void setRefreshTimeInSeconds(double refreshTimeInSeconds)
	{
		checkSimulationNotStartedYet();

		if (refreshTimeInSeconds < 0)
		{
			throw new Net2PlanException("'refreshTimeInSeconds' must be in range [0, Double.MAX_VALUE]. To avoid refreshing set it to an arbitrarely large value");
		}

		this.refreshTimeInSeconds = refreshTimeInSeconds;
	}
	
	/**
	 * <p>Sets the total number of simulation events to finish the simulation.</p>
	 *
	 * <p><b>Important</b>: This method only can be executed before the simulation starts.</p>
	 * 
	 * @param totalSimEvents Total number of events (if -1, this constraint will not be applied)
	 * @since 0.2.0
	 */
	public void setTotalSimulationEvents(long totalSimEvents)
	{
		checkSimulationNotStartedYet();

		if (totalSimEvents <= 0 && totalSimEvents != -1)
		{
			throw new Net2PlanException("'totalSimEvents' must be in range [1, Long.MAX_VALUE], or -1 for no limit");
		}

		this.totalSimEvents = totalSimEvents;
	}

	/**
	 * <p>Sets the total simulation time before finishing the simulation.</p>
	 *
	 * <p><b>Important</b>: This method only can be executed before the simulation starts.</p>
	 * 
	 * @param totalSimTime Total simulation time (if -1, this constraint will not be applied)
	 * @since 0.2.0
	 */
	public void setTotalSimulationTime(double totalSimTime)
	{
		checkSimulationNotStartedYet();

		if (totalSimTime <= 0 && totalSimTime != -1)
		{
			throw new Net2PlanException("'totalSimTime' must be in range (0, Double.MAX_VALUE], or -1 for no limit");
		}

		this.totalSimTime = totalSimTime;
	}

	/**
	 * <p>Sets the total number of simulation events to finish the transitory of the simulation.</p>
	 *
	 * <p><b>Important</b>: This method only can be executed before the simulation starts.</p>
	 * 
	 * @param totalTransitoryEvents Total number of transitory events (if -1 or greater than {@code totalSimEvents}, this constraint will not be applied)
	 * @since 0.2.0
	 */
	public void setTotalTransitoryEvents(long totalTransitoryEvents)
	{
		checkSimulationNotStartedYet();

		if (totalTransitoryEvents <= 0 && totalTransitoryEvents != -1)
		{
			throw new Net2PlanException("'totalTransitoryEvents' must be in range [1, Long.MAX_VALUE], or -1 for no transitory");
		}

		this.totalTransitoryEvents = totalTransitoryEvents;
	}

	/**
	 * <p>Sets the total simulation time before finishing the transitory of the simulation.</p>
	 *
	 * <p><b>Important</b>: This method only can be executed before the simulation starts.</p>
	 * 
	 * @param totalTransitoryTime Total transitory time (if -1 or greater than {@code totalSimTime}, this constraint will not be applied)
	 * @since 0.2.0
	 */
	public void setTotalTransitoryTime(double totalTransitoryTime)
	{
		checkSimulationNotStartedYet();

		if (totalTransitoryTime <= 0 && totalTransitoryTime != -1)
		{
			throw new Net2PlanException("'totalTransitoryTime' must be in range (0, Double.MAX_VALUE], or -1 for no transitory");
		}

		this.totalTransitoryTime = totalTransitoryTime;
	}

	/**
	 * Sets the current simulation state.
	 * 
	 * @param simulationState Current simulation state
	 * @since 0.2.0
	 */
	public void setSimulationState(SimState simulationState)
	{
		setSimulationState(simulationState, null);
	}

	private void setSimulationState(SimState simulationState, Throwable reason)
	{
		this.simulationState = simulationState;
		while (processingEvent)
		{
			try { Thread.sleep(1); }
			catch (Throwable e) { }
		}

		callback.simulationStateChanged(simulationState, reason);
	}
}
