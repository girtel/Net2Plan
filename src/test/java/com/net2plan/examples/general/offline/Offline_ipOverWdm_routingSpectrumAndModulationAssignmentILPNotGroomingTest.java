package com.net2plan.examples.general.offline;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.interfaces.networkDesign.Route;
import com.net2plan.libraries.SRGUtils;
import com.net2plan.libraries.SRGUtils.SharedRiskModel;
import com.net2plan.libraries.WDMUtils;
import com.net2plan.utils.CollectionUtils;
import com.net2plan.utils.Quadruple;

public class Offline_ipOverWdm_routingSpectrumAndModulationAssignmentILPNotGroomingTest
{
	private NetPlan np;
	private int wdmLayerIndex, ipLayerIndex;
	
	@Before
	public void setUp() throws Exception
	{
		this.np = new NetPlan (new File ("src/main/resources/data/networkTopologies/example7nodes_ipOverWDM.n2p"));
		this.wdmLayerIndex = 0;
		this.ipLayerIndex = 1;
		np.removeAllSRGs(); 
		SRGUtils.configureSRGs(np, 1, 1, SharedRiskModel.PER_BIDIRECTIONAL_LINK_BUNDLE, true, np.getNetworkLayer(wdmLayerIndex));
		WDMUtils.isWDMFormatCorrect(np , np.getNetworkLayer(wdmLayerIndex));
		
	}

	@After
	public void tearDown() throws Exception
	{
	}

	@Test
	public void testExecuteAlgorithm()
	{
		
		Map<String,List<String>> testingParameters = new HashMap<> ();
		testingParameters.put("solverName" , Arrays.asList("cplex"));
		testingParameters.put("solverLibraryName" , Arrays.asList("cplex.dll"));
		testingParameters.put("maxSolverTimeInSeconds" , Arrays.asList("10"));
		testingParameters.put("k" , Arrays.asList("5"));
		testingParameters.put("numFrequencySlotsPerFiber" , Arrays.asList("30"));
		testingParameters.put("transponderTypesInfo" , Arrays.asList("2000.0 1.0 1 9600.0 1.0 ; 4000.0 1.5 2 5000.0 1.5"));
		testingParameters.put("wdmLayerIndex" , Arrays.asList("0"));
		testingParameters.put("ipLayerIndex" , Arrays.asList("1"));
		testingParameters.put("networkRecoveryType" , Arrays.asList("not-fault-tolerant", "single-srg-tolerant-static-lp" , "1+1-srg-disjoint-lps"));
		testingParameters.put("optimizationTarget" , Arrays.asList("min-cost" , "maximin-fiber-number-idle-slots"));
		testingParameters.put("bidirectionalTransponders" , Arrays.asList("true" , "false"));
		testingParameters.put("maxPropagationDelayMs" , Arrays.asList("-1"));
		List<Map<String,String>> testsParam = CollectionUtils.getCartesianProductOfParameters (testingParameters);
		for (Map<String,String> params : testsParam)
		{
			System.out.println(params);
			final NetPlan npInput = np.copy ();
			new Offline_ipOverWdm_routingSpectrumAndModulationAssignmentILPNotGrooming ().executeAlgorithm(np , params , null);
			checkValidity (npInput , np , params);
		}
	}

	@Test
	public void testExecuteAlgorithmSingleLayerInputDesign()
	{
		this.np = new NetPlan (new File ("src/main/resources/data/networkTopologies/example7nodes_withTraffic.n2p"));
		testExecuteAlgorithm();
	}

	private void checkValidity (NetPlan npInput , NetPlan npOutput , Map<String,String> params)
	{
		final NetworkLayer ipOut = npOutput.getNetworkLayer(ipLayerIndex);
		final NetworkLayer wdmOut = npOutput.getNetworkLayer(wdmLayerIndex);
		final NetworkLayer wdmIn= npInput.getNetworkLayer(wdmLayerIndex);
		assertTrue (WDMUtils.isWDMFormatCorrect(npInput , wdmIn));
		assertTrue (WDMUtils.isWDMFormatCorrect(npOutput , wdmOut));
		assertTrue (npOutput.getVectorDemandOfferedTraffic(ipOut).zSum() > 1);
		assertTrue (npOutput.getNumberOfDemands(wdmOut) > 0);
		assertTrue (npOutput.getNumberOfRoutes(wdmOut) > 0);
		WDMUtils.checkResourceAllocationClashing(npOutput, false , true , wdmOut);
		assertEquals(npOutput.getVectorDemandBlockedTraffic(ipOut).zSum() , 0 , 0.01);
		if (params.get("networkRecoveryType").equals("1+1-srg-disjoint-lps"))
		{
			assertTrue (SRGUtils.isSingleSRGFailureTolerant (npOutput , ipOut));
			for (Route r : npOutput.getRoutes(wdmOut))
				if (!r.isBackupRoute())
				{
					assertEquals (r.getBackupRoutes().size() , 1);
					assertTrue (SRGUtils.isSRGDisjoint(r.getSeqLinks() , r.getBackupRoutes().get(0).getSeqLinks()));
				}
		}else if (params.get("networkRecoveryType").equals("single-srg-tolerant-static-lp"))
		{
			assertTrue (SRGUtils.isSingleSRGFailureTolerant (npOutput , ipOut));
		}
		if (params.get("bidirectionalTransponders").equals("true"))
		{
			Map<Quadruple<Node,Node,Double,Integer>,Set<Route>> mapLpsPerNodePairLineRateNumSlots = new HashMap<>();
			for (Route r : npOutput.getRoutes(wdmOut))
			{
				final WDMUtils.RSA rsa = new WDMUtils.RSA(r , false);
				final Quadruple<Node,Node,Double,Integer> mapKey =  Quadruple.of(r.getIngressNode() , r.getEgressNode() , r.getCarriedTrafficInNoFailureState() , rsa.getNumSlots());
				Set<Route> previousLps = mapLpsPerNodePairLineRateNumSlots.get(mapKey);
				if (previousLps == null)
				{
					previousLps = new HashSet<> ();mapLpsPerNodePairLineRateNumSlots.put(mapKey , previousLps);
				}
				previousLps.add(r);
			}
			for (Quadruple<Node,Node,Double,Integer> n1ton2Info : mapLpsPerNodePairLineRateNumSlots.keySet())
			{
				final Quadruple<Node,Node,Double,Integer> n2ton1Info = Quadruple.of(n1ton2Info.getSecond() , n1ton2Info.getFirst() , n1ton2Info.getThird () , n1ton2Info.getFourth() );
				assertEquals (mapLpsPerNodePairLineRateNumSlots.get(n1ton2Info).size() , mapLpsPerNodePairLineRateNumSlots.get(n2ton1Info).size());
			}
		}
		else if (params.get("bidirectionalTransponders").equals("false"))
		{
		} else fail ();
	}
}
