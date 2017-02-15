package com.net2plan.examples.ocnbook.offline;

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

public class Offline_fa_xp11PathProtectionTest 
{
	private NetPlan np;

	@Before
	public void setUp() throws Exception 
	{
		this.np = new NetPlan (new File ("src/main/resources/data/networkTopologies/example7nodes_withTraffic.n2p"));
		SRGUtils.configureSRGs(np , 1 , 1 , SharedRiskModel.PER_BIDIRECTIONAL_LINK_BUNDLE , true);
	}

	@After
	public void tearDown() throws Exception 
	{
		np.checkCachesConsistency();
	}

	@Test
	public void test() 
	{
		final IAlgorithm algorithm = new Offline_fa_xp11PathProtection();
		Map<String,List<String>> testingParameters = new HashMap<> ();
		testingParameters.put("solverName" , Arrays.asList("cplex"));
		testingParameters.put("optimizationTarget" , Arrays.asList("min-av-num-hops" , "minimax-link-utilization" , "maximin-link-idle-capacity"));
		testingParameters.put("type11" , Arrays.asList("linkDisjoint" , "srgDisjoint"));
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
		assertTrue (npOutput.getVectorLinkCapacity().zSum() > 1);
		assertTrue (npOutput.getVectorDemandOfferedTraffic().zSum() > 1);
		assertEquals (npOutput.getVectorDemandBlockedTraffic().zSum() , 0 , 0.01);
		assertEquals (npOutput.getVectorLinkOversubscribedTraffic().zSum() , 0 , 0.01);

		for (Route r : npOutput.getRoutes())
			if (!r.isBackupRoute())
			{
				assertEquals (r.getBackupRoutes().size() , 1);
				if (params.get("type11").equals("linkDisjoint"))
				{
					assertTrue (Collections.disjoint(r.getSeqLinks() , r.getBackupRoutes().get(0).getSeqLinks()));
				}
				else if (params.get("type11").equals("srgDisjoint"))
				{
					assertTrue (SRGUtils.isSRGDisjoint(r.getSeqLinks() , r.getBackupRoutes().get(0).getSeqLinks()));
				} else fail ();
			}


	}

}
