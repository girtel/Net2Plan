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


public class Offline_tcfa_wdmPhysicalDesign_graspAndILPTest 
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

		this.np = new NetPlan (new File ("src/test/resources/data/networkTopologies/example4nodes.n2p"));
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
		final IAlgorithm algorithm = new Offline_tcfa_wdmPhysicalDesign_graspAndILP();
		Map<String,List<String>> testingParameters = new HashMap<> ();
		testingParameters.put("algorithm_solverName" , Arrays.asList("cplex"));
		testingParameters.put("algorithm_maxExecutionTimeInSeconds" , Arrays.asList("4"));
		testingParameters.put("algorithm_outputFileNameRoot" , Arrays.asList("temporalDirectoryTests/rootOutput"));
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
			algorithm.executeAlgorithm(np , paramsUsedToCall , ImmutableMap.of("precisionFactor" , "0.0001"));
			checkValidity (npInput , np , paramsUsedToCall);
		}
	}

	private void checkValidity (NetPlan npInput , NetPlan npOutput , Map<String,String> params)
	{
		/* already tested inside the algorithm */
	}

}
