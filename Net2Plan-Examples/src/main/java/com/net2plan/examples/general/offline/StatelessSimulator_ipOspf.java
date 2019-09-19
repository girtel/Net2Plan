package com.net2plan.examples.general.offline;
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

import com.net2plan.interfaces.networkDesign.Demand;
import com.net2plan.interfaces.networkDesign.IAlgorithm;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.simulation.SimEvent;
import com.net2plan.libraries.IPUtils;
import com.net2plan.utils.InputParameter;
import com.net2plan.utils.Triple;

import cern.colt.function.tdouble.DoubleDoubleFunction;
import cern.colt.matrix.tdouble.DoubleMatrix1D;

/** 
 * Implements the reactions of an IP network governed by the OSPF/ECMP forwarding policies, for given link weigths
 * 
 * See the technology conventions used in Net2Plan built-in algorithms and libraries to represent IP/OSPF networks. 
 * @net2plan.keywords IP/OSPF, Network recovery: restoration
 * @net2plan.inputParameters 
 * @author Pablo Pavon-Marino
 */
public class StatelessSimulator_ipOspf implements IAlgorithm
{
	@Override
	public String getDescription()
	{
		return "Implements the reactions of an IP network governed by the OSPF/ECMP forwarding policies, for given link weigths";
	}

	@Override
	public List<Triple<String, String, String>> getParameters()
	{
		/* Returns the parameter information for all the InputParameter objects defined in this object (uses Java reflection) */
		return InputParameter.getInformationAllInputParameterFieldsOfObject(this);
	}

	
	@Override
	public String executeAlgorithm(NetPlan netPlan, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
	{
		/* Initialize all InputParameter objects defined in this object (this uses Java reflection) */
		InputParameter.initializeAllInputParameterFieldsOfObject(this, algorithmParameters);

		NetworkLayer ipLayer = netPlan.getNetworkLayer("IP"); 
		if (ipLayer == null) ipLayer = netPlan.getNetworkLayerDefault(); 
		
		DoubleMatrix1D linkIGPWeightSetting = IPUtils.getLinkWeightVector(netPlan , ipLayer);
		linkIGPWeightSetting.assign (netPlan.getVectorLinkUpState(ipLayer) , new DoubleDoubleFunction () { public double apply (double x , double y) { return y == 1? x : Double.MAX_VALUE; }  } );
		IPUtils.setECMPForwardingRulesFromLinkWeights(netPlan , linkIGPWeightSetting , ipLayer);
		return "";
	}

}
