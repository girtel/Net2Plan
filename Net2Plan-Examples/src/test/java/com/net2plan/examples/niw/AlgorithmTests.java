package com.net2plan.examples.niw;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.xmlbeans.impl.tool.XSTCTester.TestCase;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.net2plan.examples.niw.algorithms.Offline_nfvPlacementILP_v1;
import com.net2plan.examples.niw.algorithms.SimpleCapacityPlanningAlgorithm_v2;
import com.net2plan.examples.niw.algorithms.TopologyGenerator_example7nodesWithTraffic;
import com.net2plan.interfaces.networkDesign.Configuration;
import com.net2plan.interfaces.networkDesign.IAlgorithm;
import com.net2plan.niw.WIpLink;
import com.net2plan.niw.WIpUnicastDemand;
import com.net2plan.niw.WNet;
import com.net2plan.niw.WNode;
import com.net2plan.niw.WServiceChainRequest;
import com.net2plan.utils.InputParameter;
import com.net2plan.utils.Pair;

// import org.junit.jupiter.api.Test;

public class AlgorithmTests extends TestCase
{
//	private WNet net;
//	private WNode n1, n2, n3, n4, n5;
//	private Pair<WFiber, WFiber> f12, f23, f34, f41,f45;
//	private Pair<WIpLink, WIpLink> i12, i13, i14;
//	private WLightpathRequest lr12, lr21, lr13, lr31, lr14, lr41;
//	private WLightpath l12, l21, l13, l31, l14, l41;
//	private OpticalSpectrumManager osm;
//	private WUserService userService;
//	private WServiceChainRequest scr13, scr31;
//	private WServiceChain sc13, sc31;
//	private WVnfType vnfType1, vnfType2;
//    private List<List<WFiber>> unavoidableLasingLoops;

    @Before
	public void setUp() throws Exception
	{
	}
    
    @Test
    public void testOfflineNfvPlacementIlp ()
    {
		final WNet net = WNet.createEmptyDesign(true , true);
		final WNode n1 = net.addNode(0, 0, "n1", "type1");
		final WNode n2 = net.addNode(0, 0, "n2", "type1");
		final WNode n3 = net.addNode(0, 0, "n3", "type1");
		final WNode n4 = net.addNode(0, 0, "n4", "type1");
		final WNode n5 = net.addNode(0, 0, "n5", "type2");
		net.removeWdmLayer();
		final WIpLink e12 = net.addIpLinkBidirectional(n1, n2, 10.0).getFirst();
		final WIpLink e23 = net.addIpLinkBidirectional(n2, n3, 10.0).getFirst();
		final WIpLink e34 = net.addIpLinkBidirectional(n3, n2, 10.0).getFirst();
		final WIpLink e45 = net.addIpLinkBidirectional(n4, n5, 10.0).getFirst();
		final WIpLink e51 = net.addIpLinkBidirectional(n5, n1, 10.0).getFirst();

		final WServiceChainRequest scr12_34 = net.addServiceChainRequest(new TreeSet<> (Arrays.asList(n1 , n2)), 
				new TreeSet<> (Arrays.asList(n3 , n4)), 
				Arrays.asList("NAT" , "FW"), false, 
				Optional.of(Arrays.asList(1.0 , 2.0)), 
				Optional.of(Arrays.asList(10.0 , 11.0 , 12.0)));

		
		final WServiceChainRequest scr12_45 = net.addServiceChainRequest(new TreeSet<> (Arrays.asList(n1 , n2)), 
				new TreeSet<> (Arrays.asList(n3 , n4)), 
				Arrays.asList("FW" , "NAT"), false, 
				Optional.of(Arrays.asList(1.0 , 2.0)), 
				Optional.of(Arrays.asList(10.0 , 11.0 , 12.0)));

		net.getServiceChainRequests().forEach(e->e.setCurrentOfferedTrafficInGbps(10.0));
		net.getIpLinks().forEach(e->e.setLengthIfNotCoupledInKm(400.0)); // 2 ms for each IP link
		net.getNodes().forEach(e->e.setTotalNumCpus(100.0));
		net.getNodes().forEach(e->e.setTotalRamGB(100.0));
		net.getNodes().forEach(e->e.setTotalHdGB(100.0));
		
		final IAlgorithm alg = new Offline_nfvPlacementILP_v1(); 
		alg.executeAlgorithm(net.getNe(), InputParameter.getDefaultParameters(alg.getParameters()), new HashMap<> ());

		
		net.checkConsistency();

	}

	
}