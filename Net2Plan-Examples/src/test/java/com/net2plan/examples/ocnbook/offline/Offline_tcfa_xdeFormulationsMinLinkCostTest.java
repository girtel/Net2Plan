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
import com.net2plan.libraries.GraphUtils;
import com.net2plan.utils.InputParameter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;


public class Offline_tcfa_xdeFormulationsMinLinkCostTest 
{
	private NetPlan np;

	@Before
	public void setUp() throws Exception 
	{
		this.np = new NetPlan (new File ("src/test/resources/data/networkTopologies/example7nodes_withTraffic.n2p"));
		
	}

	@After
	public void tearDown() throws Exception 
	{
		np.checkCachesConsistency();
	}

	@Test
	public void test() 
	{
		final IAlgorithm algorithm = new Offline_tcfa_xdeFormulationsMinLinkCost();
		Map<String,List<String>> testingParameters = new HashMap<> ();
		testingParameters.put("solverName" , Arrays.asList("cplex"));
		testingParameters.put("maxSolverTimeInSeconds" , Arrays.asList("2"));
		testingParameters.put("topologyType" , Arrays.asList("arbitrary-mesh" , "bidirectional-mesh" , "unidirectional-ring" , "bidirectional-ring" , "bidirectional-tree"));
		
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
				System.err.println(this.getClass().getName() + ": " + TestConstants.CPLEX_NOT_FOUND_ERROR);
				return;
			}
			checkValidity (npInput , np , paramsUsedToCall);
		}
	}

	private void checkValidity (NetPlan npInput , NetPlan npOutput , Map<String,String> params)
	{
		final int N = npOutput.getNumberOfNodes();
		final int E = npOutput.getNumberOfLinks();
		assertTrue (GraphUtils.isConnected(npOutput.getNodes() , npOutput.getLinks()));
		if (params.get("topologyType").equals("bidirectional-mesh"))
		{
			assertTrue (GraphUtils.isBidirectional(npOutput.getNodes() , npOutput.getLinks()));
		}
		else if (params.get("topologyType").equals("unidirectional-ring"))
		{
			assertEquals(E , N);
		}
		else if (params.get("topologyType").equals("bidirectional-ring"))
		{
			assertTrue (GraphUtils.isBidirectional(npOutput.getNodes() , npOutput.getLinks()));
			assertEquals(E , 2*N);
		}
		else if (params.get("topologyType").equals("bidirectional-tree"))
		{
			assertTrue (GraphUtils.isBidirectional(npOutput.getNodes() , npOutput.getLinks()));
			assertEquals(E , 2*(N-1));
		}
		else if (params.get("topologyType").equals("arbitrary-mesh"))
		{
		}
		else fail ();
	}

}
