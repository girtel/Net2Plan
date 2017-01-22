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

public class Offline_cfa_xpMultiperiodModularCapacitiesTest 
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

		this.np = new NetPlan (new File ("src/main/resources/data/networkTopologies/example4nodes.n2p"));
		
		final Random rng = new Random (0L);
		NetPlan npTm0 = np.copy(); 
		NetPlan npTm1 = np.copy(); 
		npTm0.removeAllDemands(); 
		npTm1.removeAllDemands(); 
		for (Node n1 : npTm0.getNodes()) for (Node n2 : npTm0.getNodes()) if (n1 != n2) npTm0.addDemand(n1, n2, 100*rng.nextDouble() , null);
		for (Node n1 : npTm1.getNodes()) for (Node n2 : npTm1.getNodes()) if (n1 != n2) npTm1.addDemand(n1, n2, 100*rng.nextDouble() , null);
		npTm0.saveToFile(new File ("temporalDirectoryTests/rootInput_tm0.n2p"));
		npTm1.saveToFile(new File ("temporalDirectoryTests/rootInput_tm1.n2p"));
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
		final IAlgorithm algorithm = new Offline_cfa_xpMultiperiodModularCapacities();
		Map<String,List<String>> testingParameters = new HashMap<> ();
		testingParameters.put("rootOfNameOfInputTrafficFiles" , Arrays.asList("temporalDirectoryTests/rootInput"));
		testingParameters.put("rootOfNameOfOutputFiles" , Arrays.asList("temporalDirectoryTests/rootOutput"));
		testingParameters.put("nonBifurcatedRouting" , Arrays.asList("true" , "false"));
		testingParameters.put("solverName" , Arrays.asList("cplex"));
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
		assertTrue (npOutput.getVectorLinkCapacity().zSum() > 1);
		assertEquals (Math.IEEEremainder(npOutput.getVectorLinkCapacity().zSum() , 10) , 0 , 0.001);
		assertTrue (npOutput.getVectorDemandOfferedTraffic().zSum() > 1);
		assertEquals (npOutput.getVectorDemandBlockedTraffic().zSum() , 0 , 0.01);
		assertEquals (npOutput.getVectorLinkOversubscribedTraffic().zSum() , 0 , 0.01);
	}

}
