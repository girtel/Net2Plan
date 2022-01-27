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
import com.net2plan.utils.StringUtils;

import java.util.Collection;
import java.util.Iterator;
import java.util.PriorityQueue;

/**
 * <p>Class in charge of dealing with the future event list (FEL) of the
 * discrete event simulator.</p>
 *
 * <p>Event order is determined by the following criteria:</p>
 * <ul>
 * <li>1. Event time (the lower value, the first event), if equal</li>
 * <li>2. Event priority (the higher value, the higher priority), if equal</li>
 * <li>3. Insertion order (first in, first out)</li>
 * </ul>
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public final class FutureEventList
{
	private final PriorityQueue<SimEvent> futureEventList;
	private double currentTime;
	private long eventsProcessed;

	/**
	 * Default constructor.
	 *
	 * @since 0.2.0
	 */
	public FutureEventList()
	{
		futureEventList = new PriorityQueue<SimEvent>();
		reset();
	}
	
	@Override
	public String toString()
	{
		StringBuilder out = new StringBuilder();
		
		Iterator<SimEvent> it = futureEventList.iterator();
		while(it.hasNext()) out.append(it.next());
		
		return out.toString();
	}

	/**
	 * <p>Adds an event to the future event list.</p>
	 *
	 * <p>Some sanity checks are performed:</p>
	 * <ul>
	 * <li>Event time must be positive, and greater (or equal) than the current
	 * simulation time</li>
	 * <li>Event object must not be currently in the future event list</li>
	 * </ul>
	 *
	 * @param event Event to be added
	 * @since 0.2.0
	 */
	public void addEvent(SimEvent event)
	{
		if (event.getEventTime() < 0) throw new Net2PlanException("Event time must be greater or equal than zero");

		if (event.getEventTime() < currentTime)
		{
			throw new Net2PlanException(String.format("Event cannot be scheduled before the current simulation time (sim. time = %s, event time = %s)", StringUtils.secondsToYearsDaysHoursMinutesSeconds(currentTime), StringUtils.secondsToYearsDaysHoursMinutesSeconds(event.getEventTime())));
		}

		futureEventList.add(event);
	}

	/**
	 * Adds a collection of events to the future event list. Events will be added
	 * according to the default iteration order.
	 *
	 * @param events Collection of events to be added
	 * @since 0.2.0
	 */
	public void addEvents(Collection<? extends SimEvent> events)
	{
		for (SimEvent event : events)
		{
			addEvent(event);
		}
	}

	/**
	 * Clears the future event list.
	 *
	 * @since 0.2.0
	 */
	public void clear()
	{
		futureEventList.clear();
	}

	/**
	 * Returns the current simulation time (time of the last processed event).
	 *
	 * @return Current simulation time
	 * @since 0.2.0
	 */
	public double getCurrentSimulationTime()
	{
		return currentTime;
	}

	/**
	 * Returns the simulation time of the next event in the future event list.
	 *
	 * @return Next simulation time (or -1 if no more events)
	 * @since 0.2.0
	 */
	public double getNextEventSimulationTime()
	{
		if (!hasMoreEvents())
		{
			return -1;
		}

		return futureEventList.peek().getEventTime();
	}

	/**
	 * Returns the next event in the future event list. Simulation time is advanced
	 * to the time of this event.
	 *
	 * @return Next event in the future event list (or null, if no more events)
	 * @since 0.2.0
	 */
	public SimEvent getNextEvent()
	{
		if (futureEventList.isEmpty()) return null;

		SimEvent nextEvent = futureEventList.poll();
		currentTime = nextEvent.getEventTime();
		eventsProcessed++;

		return nextEvent;
	}

	/**
	 * Returns the number of pending events in the future event list.
	 *
	 * @return Number of pending events
	 * @since 0.2.0
	 */
	public int getNumberOfPendingEvents()
	{
		return futureEventList.size();
	}

	/**
	 * Returns the number of processed events.
	 *
	 * @return Number of processed events
	 * @since 0.2.0
	 */
	public long getNumberOfProcessedEvents()
	{
		return eventsProcessed;
	}

	/**
	 * <p>Returns the whole future event list.</p>
	 *
	 * <p><b>Important</b>: It is the original future event list, changes by user
	 * are not checked, so it is discouraged at all.</p>
	 *
	 * @return Future event list
	 * @since 0.2.0
	 */
	public PriorityQueue<SimEvent> getPendingEvents()
	{
		return futureEventList;
	}

	/**
	 * Returns {@code true} if the future event list has more events scheduled.
	 *
	 * @return {@code true} if there are more events in the future event list, {@code false} otherwise
	 * @since 0.2.0
	 */
	public boolean hasMoreEvents()
	{
		return !futureEventList.isEmpty();
	}

	/**
	 * <p>Removes an event from the future event list.</p>
	 *
	 * @param event Event to be removed
	 * @since 0.2.0
	 */
	public void remove(SimEvent event)
	{
		futureEventList.remove(event);
	}

	/**
	 * Resets the future event list.
	 *
	 * @since 0.2.0
	 */
	public void reset()
	{
		currentTime = 0;
		eventsProcessed = 0;
		clear();
	}
}
