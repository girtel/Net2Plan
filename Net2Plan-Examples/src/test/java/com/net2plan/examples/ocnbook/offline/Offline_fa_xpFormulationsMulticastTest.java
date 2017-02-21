package com.net2plan.examples.ocnbook.offline;

import static org.junit.Assert.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.net2plan.interfaces.networkDesign.Demand;
import com.net2plan.interfaces.networkDesign.IAlgorithm;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.MulticastDemand;
import com.net2plan.interfaces.networkDesign.MulticastTree;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.interfaces.networkDesign.Route;
import com.net2plan.libraries.SRGUtils;
import com.net2plan.libraries.SRGUtils.SharedRiskModel;
import com.net2plan.utils.InputParameter;

public class Offline_fa_xpFormulationsMulticastTest 
{
	private NetPlan np;

	@Before
	public void setUp() throws Exception 
	{
		final Random rng = new Random (0L);
		this.np = new NetPlan (new File ("src/test/resources/data/networkTopologies/example7nodes_withTraffic.n2p"));
		for (Node n : np.getNodes())
			np.addMulticastDemand(n , randomEgressNodes(rng,n) , rng.nextDouble() * 5 , null); 
		
	}
	private Set<Node> randomEgressNodes (Random rng , Node ingress)
	{
		HashSet<Node> res = new HashSet<> ();
		for (Node n : np.getNodes())
			if (n != ingress)
				if (rng.nextBoolean())
					res.add(n);
		return res;
	}

	@After
	public void tearDown() throws Exception 
	{
		np.checkCachesConsistency();
	}

	@Test
	public void test() 
	{
		final IAlgorithm algorithm = new Offline_fa_xpFormulationsMulticast();
		Map<String,List<String>> testingParameters = new HashMap<> ();
		testingParameters.put("solverName" , Arrays.asList("cplex"));
		testingParameters.put("nonBifurcatedRouting" , Arrays.asList("true" , "false"));
		testingParameters.put("optimizationTarget" , Arrays.asList("min-consumed-bandwidth" , "min-av-num-hops" , "minimax-link-utilization" , "maximin-link-idle-capacity" , "min-av-network-blocking"));
		List<Map<String,String>> testsParam = InputParameter.getCartesianProductOfParameters (testingParameters);
		if (testsParam.isEmpty()) testsParam = Arrays.asList(InputParameter.getDefaultParameters(algorithm.getParameters()));
		for (Map<String,String> params : testsParam)
		{
			if (Sets.newHashSet("min-av-network-delay" , "min-av-network-blocking").contains(params.get("optimizationTarget")))
			{
				if (params.get("nonBifurcatedRouting").equals("true")) continue;
				params.put("solverName" , "ipopt");
			}
			Map<String,String> paramsUsedToCall = InputParameter.getDefaultParameters(algorithm.getParameters());
			paramsUsedToCall.putAll(params); // so default parameters that are also in param, are replaced
			final NetPlan npInput = np.copy ();
			algorithm.executeAlgorithm(np , paramsUsedToCall , ImmutableMap.of("precisionFactor" , "0.0001"));
			checkValidity (npInput , np , paramsUsedToCall);
		}
	}

	private void checkValidity (NetPlan npInput , NetPlan npOutput , Map<String,String> params)
	{
		assertTrue (npOutput.getVectorLinkCapacity().zSum() > 1);
		assertTrue (npOutput.getVectorMulticastDemandOfferedTraffic().zSum() > 1);
		assertEquals (npOutput.getVectorMulticastDemandBlockedTraffic().zSum() , 0 , 0.01);
		assertEquals (npOutput.getVectorLinkOversubscribedTraffic().zSum() , 0 , 0.01);

		if (params.get("nonBifurcatedRouting").equals("true"))
			for (MulticastDemand d : npOutput.getMulticastDemands())
				assertEquals(d.getMulticastTrees().size() , 1);
	}

}
