package com.net2plan.examples.ocnbook.offline;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.net2plan.interfaces.networkDesign.IAlgorithm;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.utils.InputParameter;

public class Offline_fa_ospfWeightOptimization_greedyTest 
{
	private NetPlan np;
	private File temporalDirectoryTests;

	@Before
	public void setUp() throws Exception 
	{
		/* Create the temporal directory for storing the test files */
		this.temporalDirectoryTests = new File ("temporalDirectoryTests");
		temporalDirectoryTests.mkdirs();
		/* delete everything inside temporalDirectoryTests, including subfolders */
		Files.walk(Paths.get("temporalDirectoryTests")).filter(Files::isRegularFile).map(Path::toFile).forEach(File::delete);		

		this.np = new NetPlan (new File ("src/main/resources/data/networkTopologies/example7nodes_withTraffic.n2p"));
	}

	@After
	public void tearDown() throws Exception 
	{
		np.checkCachesConsistency();
		Files.walk(Paths.get("temporalDirectoryTests")).filter(Files::isRegularFile).map(Path::toFile).forEach(File::delete);
		temporalDirectoryTests.delete();
	}

	@Test
	public void test() 
	{
		final IAlgorithm algorithm = new Offline_fa_ospfWeightOptimization_greedy();
		Map<String,List<String>> testingParameters = new HashMap<> ();
		testingParameters.put("algorithm_outputFileNameRoot" , Arrays.asList("temporalDirectoryTests/rootOutput"));
		testingParameters.put("algorithm_numSamples" , Arrays.asList("5"));
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

	private static void checkValidity (NetPlan npInput , NetPlan npOutput , Map<String,String> params)
	{
		/* No modification is made by the algorithm in the NetPlan object */
//		assertTrue (npOutput.getVectorDemandOfferedTraffic().zSum() > 1);
//		assertEquals (npOutput.getVectorDemandBlockedTraffic().zSum() , 0 , 0.01);
//		assertEquals (npOutput.getVectorLinkOversubscribedTraffic().zSum() , 0 , 0.01);
	}

}
