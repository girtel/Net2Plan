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

import com.google.common.collect.ImmutableMap;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.simulation.IEventGenerator;
import com.net2plan.interfaces.simulation.IEventProcessor;
import com.net2plan.libraries.SRGUtils;
import com.net2plan.libraries.SRGUtils.SharedRiskModel;
import com.net2plan.libraries.WDMUtils;
import com.net2plan.utils.InputParameter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Online_evGen_ipOverWdmTest
{
	private NetPlan np;
	private int wdmLayerIndex, ipLayerIndex;
	private final static double TIMEPERSIMULATIONINSECONDS = 2;


	@Before
	public void setUp() throws Exception
	{
		this.np = new NetPlan (new File ("src/test/resources/data/networkTopologies/example7nodes_ipOverWDM.n2p"));
		this.wdmLayerIndex = 0;
		this.ipLayerIndex = 1;
		np.removeAllSRGs(); 
		SRGUtils.configureSRGs(np, 1, 1, SharedRiskModel.PER_BIDIRECTIONAL_LINK_BUNDLE, true, np.getNetworkLayer(wdmLayerIndex));
		WDMUtils.isWDMFormatCorrect(np , np.getNetworkLayer(wdmLayerIndex));
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
		final IEventGenerator generator = new Online_evGen_ipOverWdm ();
		final IEventProcessor processor = new Online_evProc_ipOverWdm ();

		final Map<String,String> simulationParameters = new HashMap<> ();
		simulationParameters.put("disableStatistics" , "false");
		simulationParameters.put("refreshTime" , "1");
		simulationParameters.put("simEvents" , "-1");
		simulationParameters.put("transitoryEvents" , "-1");
		simulationParameters.put("transitoryTime" , "" + (TIMEPERSIMULATIONINSECONDS/2));
		simulationParameters.put("simTime" , "" + TIMEPERSIMULATIONINSECONDS);
		final Map<String,String> net2planParameters = ImmutableMap.of("precisionFactor" , "0.001");
		
		final Map<String,List<String>> generatorParameters = new HashMap <>();
		generatorParameters.put("ipOverWdmFailureDefaultMTTFInHours" , Arrays.asList("0.1"));
		generatorParameters.put("ipOverWdmFailureDefaultMTTRInHours" , Arrays.asList("0.01"));
		generatorParameters.put("ipOverWDmFailureStatisticalPattern" , Arrays.asList("exponential-iid"));
		generatorParameters.put("ipTFFastFluctuationType" , Arrays.asList("random-truncated-gaussian")); //Arrays.asList("none" , "random-truncated-gaussian"));
		generatorParameters.put("ipTFSlowFluctuationType" , Arrays.asList("time-zone-based")); //Arrays.asList("none" , "time-zone-based"));
		final List<Map<String,String>> testsParamGenerator = InputParameter.getCartesianProductOfParameters (generatorParameters);
		for (Map<String,String> paramsGeneratorChangingThisTest : testsParamGenerator)
		{
			final Map<String,String> allParamsGeneratorThisTest = InputParameter.getDefaultParameters(generator.getParameters());
			allParamsGeneratorThisTest.putAll(paramsGeneratorChangingThisTest);
			//System.out.println(allParamsGeneratorThisTest);
			
			/* Create the processor parameters */
			final Map<String,List<String>> processorParameters = new HashMap <>();
			processorParameters.put("wdmNumFrequencySlotsPerFiber" , Arrays.asList("" + WDMUtils.getFiberNumFrequencySlots(np.getLink(0 , np.getNetworkLayer(wdmLayerIndex)))));
			processorParameters.put("wdmRwaType" , Arrays.asList("srg-disjointness-aware-route-first-fit" , "alternate-routing" , "least-congested-routing" , "load-sharing"));
			processorParameters.put("wdmTransponderTypesInfo" , Arrays.asList("10 1 1 9600 1 ; 20 1.5 2 9600 1"));
			processorParameters.put("wdmDefaultAndNewRouteRevoveryType" , Arrays.asList("none" , "restoration" , "1+1-link-disjoint"));
			final List<Map<String,String>> testsParamProcessor = InputParameter.getCartesianProductOfParameters (processorParameters);
			
			for (Map<String,String> paramsProcessorChangingThisTest : testsParamProcessor)
			{
				final Map<String,String> allParamsProcessorThisTest = InputParameter.getDefaultParameters(processor.getParameters());
				allParamsProcessorThisTest.putAll(paramsProcessorChangingThisTest);
				System.out.println(allParamsProcessorThisTest);

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
