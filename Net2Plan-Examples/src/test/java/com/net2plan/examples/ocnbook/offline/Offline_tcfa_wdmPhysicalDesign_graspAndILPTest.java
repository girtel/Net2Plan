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


public class Offline_tcfa_wdmPhysicalDesign_graspAndILPTest 
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
		final IAlgorithm algorithm = new Offline_tcfa_wdmPhysicalDesign_graspAndILP();
		Map<String,List<String>> testingParameters = new HashMap<> ();
		testingParameters.put("algorithm_solverName" , Arrays.asList("cplex"));
		testingParameters.put("algorithm_maxExecutionTimeInSeconds" , Arrays.asList("4"));
		testingParameters.put("algorithm_outputFileNameRoot" , Arrays.asList(TestConstants.TEST_ALGORITHM_FILE_DIRECTORY + "/rootOutput"));
		testingParameters.put("tcfa_srgType" , Arrays.asList("perBidirectionalLinkBundle" , "noFailure"));
		testingParameters.put("tcfa_recoveryType" , Arrays.asList("1+1" , "shared" , "restoration"));
		testingParameters.put("tcfa_bidirectionalLinks" , Arrays.asList("true" , "false"));
		
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
				System.err.println(this.getClass().getName() + ": " + TestConstants.GLPK_NOT_FOUND_ERROR);
				return;
			}
			checkValidity (npInput , np , paramsUsedToCall);
		}
	}

	private void checkValidity (NetPlan npInput , NetPlan npOutput , Map<String,String> params)
	{
		/* already tested inside the algorithm */
	}

}
