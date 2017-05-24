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

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.net2plan.examples.ocnbook.onlineSim.Online_evGen_generalGenerator;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.simulation.IEventGenerator;
import com.net2plan.interfaces.simulation.IEventProcessor;
import com.net2plan.libraries.IPUtils;
import com.net2plan.libraries.SRGUtils;
import com.net2plan.libraries.SRGUtils.SharedRiskModel;
import com.net2plan.utils.Constants.RoutingType;
import com.net2plan.utils.InputParameter;

public class Online_evGen_ipOspfTest
{
	private NetPlan np;
	private final static double TIMEPERSIMULATIONINSECONDS = 2;


	@Before
	public void setUp() throws Exception
	{
		this.np = new NetPlan (new File ("src/test/resources/data/networkTopologies/example7nodes_withTraffic.n2p"));
		np.getNetworkLayerDefault().setName("IP");
		np.setRoutingType(RoutingType.HOP_BY_HOP_ROUTING);
		IPUtils.setLinkWeights(np , 1);
		np.removeAllSRGs(); 
		SRGUtils.configureSRGs(np, 1, 1, SharedRiskModel.PER_BIDIRECTIONAL_LINK_BUNDLE, true);
		np.checkCachesConsistency();
	}

	@After
	public void tearDown() throws Exception
	{
		np.checkCachesConsistency();
	}

	@Test
	public void test()
	{
		final IEventGenerator generator = new Online_evGen_generalGenerator();
		final IEventProcessor processor = new Online_evProc_ipOspf();

		final Map<String,String> simulationParameters = new HashMap<> ();
		simulationParameters.put("disableStatistics" , "false");
		simulationParameters.put("refreshTime" , "1");
		simulationParameters.put("simEvents" , "-1");
		simulationParameters.put("transitoryEvents" , "-1");
		simulationParameters.put("transitoryTime" , "" + (TIMEPERSIMULATIONINSECONDS/2));
		simulationParameters.put("simTime" , "" + TIMEPERSIMULATIONINSECONDS);
		final Map<String,String> net2planParameters = ImmutableMap.of("precisionFactor" , "0.001");
		
		final Map<String,List<String>> generatorParameters = new HashMap <>();
		generatorParameters.put("_fail_failureModel" , Arrays.asList("perBidirectionalLinkBundle"));
		generatorParameters.put("_tfFast_fluctuationType" , Arrays.asList("random-truncated-gaussian"));
		generatorParameters.put("_trafficType" , Arrays.asList("non-connection-based" , "connection-based-longrun" , "connection-based-incremental"));
		generatorParameters.put("_tfSlow_fluctuationType" , Arrays.asList("time-zone-based"));
		generatorParameters.put("cac_arrivalsPattern" , Arrays.asList("random-exponential-arrivals-and-duration"));
		generatorParameters.put("fail_statisticalPattern" , Arrays.asList("exponential-iid"));
		final List<Map<String,String>> testsParamGenerator = InputParameter.getCartesianProductOfParameters (generatorParameters);
		for (Map<String,String> paramsGeneratorChangingThisTest : testsParamGenerator)
		{
			final Map<String,String> allParamsGeneratorThisTest = InputParameter.getDefaultParameters(generator.getParameters());
			allParamsGeneratorThisTest.putAll(paramsGeneratorChangingThisTest);
			
			/* Create the processor parameters */
			final Map<String,List<String>> processorParameters = new HashMap <>();
			processorParameters.put("ipMaximumE2ELatencyMs" , Arrays.asList("5"));
			final List<Map<String,String>> testsParamProcessor = InputParameter.getCartesianProductOfParameters (processorParameters);
			
			for (Map<String,String> paramsProcessorChangingThisTest : testsParamProcessor)
			{
				final Map<String,String> allParamsProcessorThisTest = InputParameter.getDefaultParameters(processor.getParameters());
				allParamsProcessorThisTest.putAll(paramsProcessorChangingThisTest);
				System.out.println(allParamsProcessorThisTest);

//				if (allParamsProcessorThisTest.get("ipOverWdmNetworkRecoveryType").equals("1+1-lps-OSPF-rerouting") && 
//						allParamsProcessorThisTest.get("wdmProtectionTypeToNewRoutes").equals ("none")) continue;
				
				
				final NetPlan npInput = np.copy ();
				final NetPlan npOutput = np.copy ();
				new OnlineTestUtils().runSimulation(npOutput , generator , processor , simulationParameters , net2planParameters , 
						allParamsGeneratorThisTest , allParamsProcessorThisTest , TIMEPERSIMULATIONINSECONDS);
				checkValidity (npInput , npOutput , allParamsGeneratorThisTest);
			}			
			
			
		}
	}

	private void checkValidity (NetPlan npInput , NetPlan npOutput , Map<String,String> params)
	{
	}
}
