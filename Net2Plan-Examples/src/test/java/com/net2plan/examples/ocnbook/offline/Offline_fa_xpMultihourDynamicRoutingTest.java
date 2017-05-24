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
package com.net2plan.examples.ocnbook.offline;

import com.google.common.collect.ImmutableMap;
import com.net2plan.examples.TestConstants;
import com.net2plan.interfaces.networkDesign.IAlgorithm;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.utils.InputParameter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class Offline_fa_xpMultihourDynamicRoutingTest 
{
	private NetPlan np;
	private File temporalDirectoryTests;

	@Before
	public void setUp() throws Exception 
	{
		/* Create the temporal directory for storing the test files */
		this.temporalDirectoryTests = new File (TestConstants.TEST_ALGORITHM_FILE_DIRECTORY);
		temporalDirectoryTests.mkdirs();
		/* delete everything inside temporalDirectoryTests, including subfolders */
		Files.walk(Paths.get(TestConstants.TEST_ALGORITHM_FILE_DIRECTORY)).filter(Files::isRegularFile).map(Path::toFile).forEach(File::delete);

		this.np = new NetPlan (new File ("src/test/resources/data/networkTopologies/example4nodes.n2p"));
		
		final Random rng = new Random (0L);
		NetPlan npTm0 = np.copy(); 
		NetPlan npTm1 = np.copy(); 
		npTm0.removeAllDemands(); 
		npTm1.removeAllDemands(); 
		for (Node n1 : npTm0.getNodes()) for (Node n2 : npTm0.getNodes()) if (n1 != n2) npTm0.addDemand(n1, n2, 5*rng.nextDouble() , null);
		for (Node n1 : npTm1.getNodes()) for (Node n2 : npTm1.getNodes()) if (n1 != n2) npTm1.addDemand(n1, n2, 5*rng.nextDouble() , null);
		npTm0.saveToFile(new File (TestConstants.TEST_ALGORITHM_FILE_DIRECTORY + "/rootInput_tm0.n2p"));
		npTm1.saveToFile(new File (TestConstants.TEST_ALGORITHM_FILE_DIRECTORY + "/rootInput_tm1.n2p"));
	}

	@After
	public void tearDown() throws Exception 
	{
		np.checkCachesConsistency();
		Files.walk(Paths.get(TestConstants.TEST_ALGORITHM_FILE_DIRECTORY)).filter(Files::isRegularFile).map(Path::toFile).forEach(File::delete);
		temporalDirectoryTests.delete();
	}

	@Test
	public void test() 
	{
		final IAlgorithm algorithm = new Offline_fa_xpMultihourDynamicRouting();
		Map<String,List<String>> testingParameters = new HashMap<> ();
		testingParameters.put("rootOfNameOfInputTrafficFiles" , Arrays.asList(TestConstants.TEST_ALGORITHM_FILE_DIRECTORY + "/rootInput"));
		testingParameters.put("rootOfNameOfOutputFiles" , Arrays.asList(TestConstants.TEST_ALGORITHM_FILE_DIRECTORY + "/rootOutput"));
		testingParameters.put("solverName" , Arrays.asList("ipopt"));
		testingParameters.put("optimizationTarget" , Arrays.asList("min-av-num-hops" , "minimax-link-utilization" , "maximin-link-idle-capacity"));
		List<Map<String,String>> testsParam = InputParameter.getCartesianProductOfParameters (testingParameters);
		if (testsParam.isEmpty()) testsParam = Arrays.asList(InputParameter.getDefaultParameters(algorithm.getParameters()));
		for (Map<String,String> params : testsParam)
		{
			Map<String,String> paramsUsedToCall = InputParameter.getDefaultParameters(algorithm.getParameters());
			paramsUsedToCall.putAll(params); // so default parameters that are also in param, are replaced
			final NetPlan npInput = np.copy ();
			try
			{
				algorithm.executeAlgorithm(np , paramsUsedToCall , ImmutableMap.of("precisionFactor" , "0.0001"));
			} catch (UnsatisfiedLinkError e)
			{
				System.err.println(this.getClass().getName() + ": " + TestConstants.IPOPT_NOT_FOUND_ERROR);
				return;
			}
			checkValidity (npInput , np , paramsUsedToCall);
		}
	}

	private void checkValidity (NetPlan npInput , NetPlan npOutput , Map<String,String> params)
	{
		assertTrue (npOutput.getVectorLinkCapacity().zSum() > 1);
		assertTrue (npOutput.getVectorDemandOfferedTraffic().zSum() > 1);
		assertEquals (npOutput.getVectorDemandBlockedTraffic().zSum() , 0 , 0.01);
		assertEquals (npOutput.getVectorLinkOversubscribedTraffic().zSum() , 0 , 0.01);
	}

}
