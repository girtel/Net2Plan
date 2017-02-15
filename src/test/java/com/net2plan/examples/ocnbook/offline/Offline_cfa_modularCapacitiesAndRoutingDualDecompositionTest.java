package com.net2plan.examples.ocnbook.offline;

import static org.junit.Assert.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.net2plan.interfaces.networkDesign.IAlgorithm;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.utils.InputParameter;

public class Offline_cfa_modularCapacitiesAndRoutingDualDecompositionTest 
{
	private NetPlan np;
	private File temporalDirectoryTests;
	private double moduleCapacity;

	@Before
	public void setUp() throws Exception 
	{
		this.np = new NetPlan (new File ("src/main/resources/data/networkTopologies/abilene_N12_E30_withTrafficAndClusters3.n2p"));
		
		/* Create the temporal directory for storing the test files */
		this.temporalDirectoryTests = new File ("temporalDirectoryTests");
		temporalDirectoryTests.mkdirs();
		/* delete everything inside temporalDirectoryTests, including subfolders */
		Files.walk(Paths.get("temporalDirectoryTests")).filter(Files::isRegularFile).map(Path::toFile).forEach(File::delete);		
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
		final IAlgorithm algorithm = new Offline_cfa_modularCapacitiesAndRoutingDualDecomposition();
		Map<String,List<String>> testingParameters = new HashMap<> ();
		testingParameters.put("solverName" , Arrays.asList("cplex"));
		testingParameters.put("simulation_outFileNameRoot" , Arrays.asList("temporalDirectoryTests/modularCapacitiesAndRoutingDualDecomp"));
		testingParameters.put("simulation_numIterations" , Arrays.asList("5"));
		List<Map<String,String>> testsParam = InputParameter.getCartesianProductOfParameters (testingParameters);
		if (testsParam.isEmpty()) testsParam = Arrays.asList(InputParameter.getDefaultParameters(algorithm.getParameters()));
		for (Map<String,String> params : testsParam)
		{
			Map<String,String> paramsUsedToCall = InputParameter.getDefaultParameters(algorithm.getParameters());
			this.moduleCapacity = Double.parseDouble(paramsUsedToCall.get("moduleCapacity"));
			paramsUsedToCall.putAll(params); // so default parameters that are also in param, are replaced
			final NetPlan npInput = np.copy ();
			algorithm.executeAlgorithm(np , paramsUsedToCall , ImmutableMap.of("precisionFactor" , "0.0001"));
			checkValidity (npInput , np , paramsUsedToCall);
		}
	}

	private void checkValidity (NetPlan npInput , NetPlan npOutput , Map<String,String> params)
	{
		assertTrue (npOutput.getVectorLinkCapacity().zSum() > 1);
		assertEquals (Math.IEEEremainder(npOutput.getVectorLinkCapacity().zSum() , this.moduleCapacity) , 0 , 0.001);
		assertTrue (npOutput.getVectorDemandOfferedTraffic().zSum() > 1);
		assertEquals (npOutput.getVectorDemandBlockedTraffic().zSum() , 0 , 0.01);
		assertEquals (npOutput.getVectorLinkOversubscribedTraffic().zSum() , 0 , 0.01);
	}

}
