package com.net2plan.examples.niw.algorithms;
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


import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import com.net2plan.interfaces.networkDesign.IAlgorithm;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.niw.networkModel.WIpUnicastDemand;
import com.net2plan.niw.networkModel.WNet;
import com.net2plan.niw.networkModel.WNetConstants;
import com.net2plan.niw.networkModel.WNode;
import com.net2plan.utils.InputParameter;
import com.net2plan.utils.Triple;

/** 
 * Creates an example topology with traffic, but no lightpaths
 * @author Pablo Pavon-Marino
 */
public class TopologyGenerator_example7nodesWithTraffic implements IAlgorithm
{
	@Override
	public String getDescription()
	{
		return "";
	}

	@Override
	public List<Triple<String, String, String>> getParameters()
	{
		/* Returns the parameter information for all the InputParameter objects defined in this object (uses Java reflection) */
		return InputParameter.getInformationAllInputParameterFieldsOfObject(this);
	}

	
	@Override
	public String executeAlgorithm(NetPlan np, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
	{
		/* Initialize all InputParameter objects defined in this object (this uses Java reflection) */
		InputParameter.initializeAllInputParameterFieldsOfObject(this, algorithmParameters);

		final WNet wNet = WNet.createEmptyDesign();

		final WNode madrid = wNet.addNode (-3.6919444, 40.4188889 , "Madrid" , ""); madrid.setPoputlation(3265038.0);
		final WNode barcelona = wNet.addNode (2.1769444 , 41.3825 , "Barcelona" , ""); barcelona.setPoputlation(1615448.0);
		final WNode valencia = wNet.addNode (-0.375 , 39.4666667 , "Valencia" , ""); valencia.setPoputlation(798033.0);
		final WNode sevilla = wNet.addNode(-5.9833333 , 37.3833333 , "Sevilla" , ""); sevilla.setPoputlation(703021.0);
		final WNode zaragoza = wNet.addNode(-0.8833333 , 41.65 , "Zaragoza" , ""); zaragoza.setPoputlation(674725.0);
		final WNode malaga = wNet.addNode(-4.4166667 , 36.7166667 , "Malaga" , ""); malaga.setPoputlation(568030.0);
		final WNode murcia = wNet.addNode(-1.1302778 , 37.9861111 , "Murcia" , ""); murcia.setPoputlation(442203.0);
		
		wNet.addFiber(sevilla, malaga, WNetConstants.WFIBER_DEFAULT_VALIDOPTICALSLOTRANGES.stream().map(e->e.intValue()).collect(Collectors.toList()) , -1, true);
		wNet.addFiber(malaga, murcia , WNetConstants.WFIBER_DEFAULT_VALIDOPTICALSLOTRANGES.stream().map(e->e.intValue()).collect(Collectors.toList()) , -1, true);
		wNet.addFiber(murcia , valencia , WNetConstants.WFIBER_DEFAULT_VALIDOPTICALSLOTRANGES.stream().map(e->e.intValue()).collect(Collectors.toList()) , -1, true);
		wNet.addFiber(valencia , barcelona , WNetConstants.WFIBER_DEFAULT_VALIDOPTICALSLOTRANGES.stream().map(e->e.intValue()).collect(Collectors.toList()) , -1, true);
		wNet.addFiber(barcelona , zaragoza , WNetConstants.WFIBER_DEFAULT_VALIDOPTICALSLOTRANGES.stream().map(e->e.intValue()).collect(Collectors.toList()) , -1, true);
		wNet.addFiber(zaragoza , madrid , WNetConstants.WFIBER_DEFAULT_VALIDOPTICALSLOTRANGES.stream().map(e->e.intValue()).collect(Collectors.toList()) , -1, true);
		wNet.addFiber(madrid , sevilla, WNetConstants.WFIBER_DEFAULT_VALIDOPTICALSLOTRANGES.stream().map(e->e.intValue()).collect(Collectors.toList()) , -1, true);
		wNet.addFiber(madrid , valencia, WNetConstants.WFIBER_DEFAULT_VALIDOPTICALSLOTRANGES.stream().map(e->e.intValue()).collect(Collectors.toList()) , -1, true);
		
		final Random rng = new Random (1);
		for (WNode n1 : wNet.getNodes())
			for (WNode n2 : wNet.getNodes())
				if (n1.getId() < n2.getId())
				{
					final WIpUnicastDemand d12 = wNet.addIpUnicastDemand(n1, n2, true, true);
					final WIpUnicastDemand d21 = wNet.addIpUnicastDemand(n2, n1, false, true);
					d12.setBidirectionalPair(d21);
					d12.setCurrentOfferedTrafficInGbps(rng.nextDouble() * n1.getPopulation() * n2.getPopulation());
					d21.setCurrentOfferedTrafficInGbps(rng.nextDouble() * n1.getPopulation() * n2.getPopulation());
				}
		final double totalTrafficGbps = wNet.getIpUnicastDemands().stream().mapToDouble(e->e.getCurrentOfferedTrafficInGbps()).sum();
		for (WIpUnicastDemand e : wNet.getIpUnicastDemands()) e.setCurrentOfferedTrafficInGbps(e.getCurrentCarriedTrafficGbps() * 100 / totalTrafficGbps);
				
		np.assignFrom(wNet.getNe());
				
		return "";
	}
	
}
