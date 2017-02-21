package com.net2plan.examples.ocnbook.offline;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.net2plan.interfaces.networkDesign.IAlgorithm;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.utils.InputParameter;

import static org.junit.Assert.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.net2plan.interfaces.networkDesign.Demand;
import com.net2plan.interfaces.networkDesign.IAlgorithm;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.interfaces.networkDesign.Route;
import com.net2plan.libraries.GraphUtils;
import com.net2plan.libraries.SRGUtils;
import com.net2plan.libraries.SRGUtils.SharedRiskModel;
import com.net2plan.utils.InputParameter;


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
			algorithm.executeAlgorithm(np , paramsUsedToCall , ImmutableMap.of("precisionFactor" , "0.0001"));
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
