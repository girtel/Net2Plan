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




package com.net2plan.examples.general.onlineSim;

import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.simulation.IEventGenerator;
import com.net2plan.interfaces.simulation.SimEvent;
import com.net2plan.utils.Triple;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/** 
 * This event generator does not produce any event. It is only needed with those event processors that work alone, generating and consuming their own events, 
 * and that cannot receive any event coming from the event generator module.
 * @net2plan.inputParameters 
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 */
public class Online_evGen_doNothing extends IEventGenerator
{
	@Override
	public String getDescription()
	{
		return "This event generator does not generate any event. In general, it is only for testing purposes";
	}

	@Override
	public List<Triple<String, String, String>> getParameters()
	{
		return new LinkedList<Triple<String, String, String>>();
	}

	@Override
	public void initialize(NetPlan initialNetPlan, Map<String, String> algorithmParameters, Map<String, String> simulationParameters, Map<String, String> net2planParameters)
	{
	}

	@Override
	public void processEvent(NetPlan currentNetPlan, SimEvent event)
	{
	}
}
