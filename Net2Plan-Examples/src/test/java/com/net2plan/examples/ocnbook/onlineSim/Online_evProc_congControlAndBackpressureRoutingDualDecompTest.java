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
package com.net2plan.examples.ocnbook.onlineSim;

import com.google.common.collect.ImmutableMap;
import com.net2plan.examples.TestConstants;
import com.net2plan.examples.general.onlineSim.OnlineTestUtils;
import com.net2plan.examples.general.onlineSim.Online_evGen_doNothing;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.simulation.IEventGenerator;
import com.net2plan.interfaces.simulation.IEventProcessor;
import com.net2plan.utils.InputParameter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class Online_evProc_congControlAndBackpressureRoutingDualDecompTest
{
	private NetPlan np;
	private File temporalDirectoryTests;
	private final static double TIMEPERSIMULATIONINSECONDS = -1;

	@Before
	public void setUp() throws Exception
	{
		this.np = new NetPlan (new File ("src/test/resources/data/networkTopologies/example4nodes.n2p"));
		np.setTrafficMatrix(new NetPlan (new File ("src/test/resources/data/trafficMatrices/tm4nodes.n2p")).getMatrixNode2NodeOfferedTraffic());

		/* Create the temporal directory for storing the test files */
		this.temporalDirectoryTests = new File (TestConstants.TEST_ALGORITHM_FILE_DIRECTORY);
		temporalDirectoryTests.mkdirs();
		/* delete everything inside temporalDirectoryTests, including subfolders */
		Files.walk(Paths.get(TestConstants.TEST_ALGORITHM_FILE_DIRECTORY)).filter(Files::isRegularFile).map(Path::toFile).forEach(File::delete);
	}

	@After
	public void tearDown() throws Exception
	{
		np.checkCachesConsistency();
//		Files.walk(Paths.get(TestConstants.TEST_ALGORITHM_FILE_DIRECTORY)).filter(Files::isRegularFile).map(Path::toFile).forEach(File::delete);
//		temporalDirectoryTests.delete();
	}

	@Test
	public void test()
	{
		final IEventGenerator generator = new Online_evGen_doNothing();
		final IEventProcessor processor = new Online_evProc_congControlAndBackpressureRoutingDualDecomp();

		final Map<String,String> simulationParameters = new HashMap<> ();
		simulationParameters.put("disableStatistics" , "false");
		simulationParameters.put("refreshTime" , "1");
		simulationParameters.put("simEvents" , "-1");
		simulationParameters.put("transitoryEvents" , "-1");
		simulationParameters.put("transitoryTime" , "" + (TIMEPERSIMULATIONINSECONDS == -1? -1 : TIMEPERSIMULATIONINSECONDS/2));
		simulationParameters.put("simTime" , "" + TIMEPERSIMULATIONINSECONDS);
		final Map<String,String> net2planParameters = ImmutableMap.of("precisionFactor" , "0.001");
		
		final Map<String,List<String>> generatorParameters = new HashMap <>();
		final List<Map<String,String>> testsParamGenerator = InputParameter.getCartesianProductOfParameters (generatorParameters);
		for (Map<String,String> paramsGeneratorChangingThisTest : testsParamGenerator)
		{
			final Map<String,String> allParamsGeneratorThisTest = InputParameter.getDefaultParameters(generator.getParameters());
			allParamsGeneratorThisTest.putAll(paramsGeneratorChangingThisTest);
			
			/* Create the processor parameters */
			final Map<String,List<String>> processorParameters = new HashMap <>();
			
			processorParameters.put("simulation_outFileNameRoot" , Arrays.asList(TestConstants.TEST_ALGORITHM_FILE_DIRECTORY + "/rootOutput"));
			final List<Map<String,String>> testsParamProcessor = InputParameter.getCartesianProductOfParameters (processorParameters);
			
			for (Map<String,String> paramsProcessorChangingThisTest : testsParamProcessor)
			{
				final Map<String,String> allParamsProcessorThisTest = InputParameter.getDefaultParameters(processor.getParameters());
				allParamsProcessorThisTest.putAll(paramsProcessorChangingThisTest);
				System.out.println(allParamsProcessorThisTest);

				final NetPlan npInput = np.copy ();
				final NetPlan npOutput = np.copy ();
				try
				{
					new OnlineTestUtils().runSimulation(npOutput , generator , processor , simulationParameters , net2planParameters ,
							allParamsGeneratorThisTest , allParamsProcessorThisTest , TIMEPERSIMULATIONINSECONDS);
				} catch (UnsatisfiedLinkError e)
				{
					System.err.println(this.getClass().getName() + ": " + TestConstants.IPOPT_NOT_FOUND_ERROR);
					return;
				}
				checkValidity (npInput , npOutput , allParamsGeneratorThisTest);
			}			
		}
	}

	private void checkValidity (NetPlan npInput , NetPlan npOutput , Map<String,String> params)
	{
		assertTrue (npOutput.getVectorDemandOfferedTraffic().zSum() > 1);
		assertTrue (npOutput.getVectorLinkCapacity().zSum() > 1);
		assertEquals (npOutput.getVectorDemandBlockedTraffic().zSum() , 0 , 2.5);
		assertEquals (npOutput.getVectorLinkOversubscribedTraffic().zSum() , 0 , 3);
		assertTrue (npOutput.getVectorDemandOfferedTraffic().zSum() > 1);
	}
}
