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
import com.net2plan.libraries.SRGUtils;
import com.net2plan.libraries.SRGUtils.SharedRiskModel;
import com.net2plan.utils.InputParameter;


public class Offline_tcfa_generalMultilayerTest 
{
	private NetPlan np;

	@Before
	public void setUp() throws Exception 
	{
		this.np = new NetPlan (new File ("src/main/resources/data/networkTopologies/example7nodes_withTraffic.n2p"));
		
	}

	@After
	public void tearDown() throws Exception 
	{
		np.checkCachesConsistency();
	}

	@Test
	public void test() 
	{
		final IAlgorithm algorithm = new Offline_tcfa_generalMultilayer();
		Map<String,List<String>> testingParameters = new HashMap<> ();
		testingParameters.put("solverName" , Arrays.asList("cplex"));
		testingParameters.put("maxSolverTimeInSeconds" , Arrays.asList("2"));
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
		final NetworkLayer lowerLayer = npOutput.getNetworkLayer(0);
		final NetworkLayer upperLayer = npOutput.getNetworkLayer(1);
		assertTrue(npOutput.getVectorDemandOfferedTraffic(upperLayer).zSum() > 1);
		assertEquals(npOutput.getVectorDemandBlockedTraffic(lowerLayer).zSum() , 0 , 0.01);
		assertEquals(npOutput.getVectorDemandBlockedTraffic(upperLayer).zSum() , 0 , 0.01);
		assertEquals(npOutput.getVectorLinkOversubscribedTraffic(lowerLayer).zSum() , 0 , 0.01);
		assertEquals(npOutput.getVectorLinkOversubscribedTraffic(upperLayer).zSum() , 0 , 0.01);
	}

}
