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

import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.utils.StringUtils;
import com.net2plan.utils.Triple;

import java.util.*;

/**
 * <p>Abstract class providing an event generator that is able to combine 
 * existing event generators into a common one. All the event generators are 
 * called for each method corresponding to the {@link com.net2plan.interfaces.simulation.IEventGenerator IEventGenerator} class, 
 * i.e. initialize, finish..., in the order defined in the constructor. The only 
 * method to be implemented is {@link #processEvent(com.net2plan.interfaces.networkDesign.NetPlan, com.net2plan.interfaces.simulation.SimEvent) processEvent()}, 
 * which would send the event to the specific event generator.</p>
 *
 * <p><b>Important</b>: Classes for event generators must be in the classpath 
 * of {@code Net2Plan}. Otherwise, a {@code NoClassDefFoundError} exception would appear.</p>
 * 
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 */
public abstract class ICombinedEventGenerator extends IEventGenerator
{
	/**
	 * Reference to the set of event generators.
	 * 
	 */
	public final IEventGenerator[] eventGenerators;
	
	/**
	 * Default constructor.
	 * 
	 * @param eventGenerators Events generators to be combined
	 */
	public ICombinedEventGenerator(IEventGenerator... eventGenerators)
	{
		if (eventGenerators.length == 0) throw new Net2PlanException("Bad - At least an event generator is required");
		this.eventGenerators = Arrays.copyOf(eventGenerators, eventGenerators.length);
	}
	
	@Override
	public String finish(StringBuilder output, double simTime)
	{
		StringBuilder title = new StringBuilder();
		for(IEventGenerator eventGenerator : eventGenerators)
		{
			StringBuilder auxOutput = new StringBuilder();
			String auxTitle = eventGenerator.finish(auxOutput, simTime);
			if (auxTitle == null || auxOutput.length() == 0) continue;
			
			if (title.length() > 0) title.append(" / ");
			
			title.append(auxTitle);
			output.append(auxOutput);
		}
		
		return title.toString();
	}

	@Override
	public void finishTransitory(double simTime)
	{
		for(IEventGenerator eventGenerator : eventGenerators)
			eventGenerator.finishTransitory(simTime);
	}
	
	@Override
	public String getDescription()
	{
		String NEW_LINE = StringUtils.getLineSeparator();
		StringBuilder description = new StringBuilder("Combined event generator" + NEW_LINE);
		for(IEventGenerator eventGenerator : eventGenerators)
		{
			String auxDescription = eventGenerator.getDescription();
			if (auxDescription == null) continue;
			
			description.append(NEW_LINE).append(auxDescription);
		}
		
		return description.toString();
	}

	@Override
	public List<Triple<String, String, String>> getParameters()
	{
		List<Triple<String, String, String>> parameters = new LinkedList<Triple<String, String, String>>();
		for(IEventGenerator eventGenerator : eventGenerators)
		{
			List<Triple<String, String, String>> auxParameters = eventGenerator.getParameters();
			if (auxParameters == null) continue;
			
			parameters.addAll(auxParameters);
		}
		
		Set<String> existingParameters = new LinkedHashSet<String>();
		Iterator<Triple<String, String, String>> it = parameters.iterator();
		while(it.hasNext())
		{
			String parameterName = it.next().getFirst();
			if (existingParameters.contains(parameterName)) it.remove();
			else existingParameters.add(parameterName);
		}
		
		return parameters;
	}
	
	@Override
	public void initialize(NetPlan initialNetPlan, Map<String, String> algorithmParameters, Map<String, String> simulationParameters, Map<String, String> net2planParameters)
	{
		for(IEventGenerator eventGenerator : eventGenerators)
			eventGenerator.initialize(initialNetPlan, algorithmParameters, simulationParameters, net2planParameters);
	}
	
	@Override
	public abstract void processEvent(NetPlan currentNetPlan, SimEvent event);
}
