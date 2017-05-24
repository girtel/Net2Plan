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
 * <p>Abstract class providing an event processor that is able to combine 
 * existing event processors into a common one. All the event generators are 
 * called for each method corresponding to the {@link com.net2plan.interfaces.simulation.IEventProcessor IEventProcessor} class, 
 * i.e. initialize, finish..., in the order defined in the constructor. The only 
 * method to be implemented is {@link #processEvent(com.net2plan.interfaces.networkDesign.NetPlan, com.net2plan.interfaces.simulation.SimEvent) processEvent()}, 
 * which would send the event to the specific event processor.</p>
 *
 * <p><b>Important</b>: Classes for event processors must be in the classpath 
 * of {@code Net2Plan}. Otherwise, a {@code NoClassDefFoundError} exception should appear.</p>
 * 
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 */
public abstract class ICombinedEventProcessor extends IEventProcessor
{
	/**
	 * Reference to the set of event processors.
	 * 
	 */
	public final IEventProcessor[] eventProcessors;
	
	/**
	 * Default constructor.
	 * 
	 * @param eventProcessors Events processors to be combined
	 */
	public ICombinedEventProcessor(IEventProcessor... eventProcessors)
	{
		if (eventProcessors.length == 0) throw new Net2PlanException("Bad - At least an event processor is required");
		this.eventProcessors = Arrays.copyOf(eventProcessors, eventProcessors.length);
	}
	
	@Override
	public String finish(StringBuilder output, double simTime)
	{
		StringBuilder title = new StringBuilder();
		for(IEventProcessor eventProcessor : eventProcessors)
		{
			StringBuilder auxOutput = new StringBuilder();
			String auxTitle = eventProcessor.finish(auxOutput, simTime);
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
		for(IEventProcessor eventGenerator : eventProcessors)
			eventGenerator.finishTransitory(simTime);
	}
	
	@Override
	public String getDescription()
	{
		String NEW_LINE = StringUtils.getLineSeparator();
		StringBuilder description = new StringBuilder("Combined event processor" + NEW_LINE);
		for(IEventProcessor eventProcessor : eventProcessors)
		{
			String auxDescription = eventProcessor.getDescription();
			if (auxDescription == null) continue;
			
			description.append(NEW_LINE).append(auxDescription);
		}
		
		return description.toString();
	}

	@Override
	public List<Triple<String, String, String>> getParameters()
	{
		List<Triple<String, String, String>> parameters = new LinkedList<Triple<String, String, String>>();
		for(IEventProcessor eventProcessor : eventProcessors)
		{
			List<Triple<String, String, String>> auxParameters = eventProcessor.getParameters();
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
		for(IEventProcessor eventProcessor : eventProcessors)
			eventProcessor.initialize(initialNetPlan, algorithmParameters, simulationParameters, net2planParameters);
	}
	
	@Override
	public abstract void processEvent(NetPlan currentNetPlan, SimEvent event);
}
