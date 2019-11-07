package com.net2plan.examples.niw;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.xmlbeans.impl.tool.XSTCTester.TestCase;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.net2plan.examples.niw.algorithms.SimpleCapacityPlanningAlgorithm_v2;
import com.net2plan.examples.niw.algorithms.TopologyGenerator_example7nodesWithTraffic;
import com.net2plan.interfaces.networkDesign.Configuration;
import com.net2plan.niw.OpticalSpectrumManager;
import com.net2plan.niw.WFiber;
import com.net2plan.niw.WIpLink;
import com.net2plan.niw.WIpUnicastDemand;
import com.net2plan.niw.WLightpath;
import com.net2plan.niw.WLightpathRequest;
import com.net2plan.niw.WNet;
import com.net2plan.niw.WNetConstants;
import com.net2plan.niw.WNode;
import com.net2plan.niw.WServiceChain;
import com.net2plan.niw.WServiceChainRequest;
import com.net2plan.niw.WUserService;
import com.net2plan.niw.WVnfType;
import com.net2plan.utils.InputParameter;
import com.net2plan.utils.Pair;

// import org.junit.jupiter.api.Test;

public class NiwModelTest extends TestCase
{
	private WNet net;
	private WNode n1, n2, n3, n4, n5;
	private Pair<WFiber, WFiber> f12, f23, f34, f41,f45;
	private Pair<WIpLink, WIpLink> i12, i13, i14;
	private WLightpathRequest lr12, lr21, lr13, lr31, lr14, lr41;
	private WLightpath l12, l21, l13, l31, l14, l41;
	private OpticalSpectrumManager osm;
	private WUserService userService;
	private WServiceChainRequest scr13, scr31;
	private WServiceChain sc13, sc31;
	private WVnfType vnfType1, vnfType2;
    private List<List<WFiber>> unavoidableLasingLoops;

    @Before
	public void setUp() throws Exception
	{
		this.net = WNet.createEmptyDesign(true , true);
		this.n1 = net.addNode(0, 0, "n1", "type1");
		this.n2 = net.addNode(0, 0, "n2", "type1");
		this.n3 = net.addNode(0, 0, "n3", "type1");
		this.n4 = net.addNode(0, 0, "n4", "type1");
		this.n5 = net.addNode(0, 0, "n5", "type2");
		n3.setIsConnectedToNetworkCore(true);
		
		assertEquals(net.getNodes(), Arrays.asList(n1, n2, n3, n4, n5));
		assertTrue(n3.isConnectedToNetworkCore());
		assertTrue(!n1.isConnectedToNetworkCore());
		this.f12 = net.addFiber(n1, n2, Arrays.asList(Pair.of(0, 300)), -1, true);
		this.f23 = net.addFiber(n2, n3, Arrays.asList(Pair.of(0, 300)), -1, true);
		this.f34 = net.addFiber(n3, n4, Arrays.asList(Pair.of(0, 300)), -1, true);
		this.f41 = net.addFiber(n4, n1, Arrays.asList(Pair.of(0, 300)), -1, true);
		this.f45 = net.addFiber(n4, n5, Arrays.asList(Pair.of(0, 300)), -1, true);
		assertEquals(net.getFibers(), Arrays.asList(f12.getFirst(), f12.getSecond(), f23.getFirst(), f23.getSecond(), f34.getFirst(), f34.getSecond(), f41.getFirst(), f41.getSecond(),f45.getFirst(),f45.getSecond()));
		assertEquals(n1.getIncomingFibers(), new TreeSet<>(Arrays.asList(f12.getSecond(), f41.getFirst())));
		assertEquals(n1.getOutgoingFibers(), new TreeSet<>(Arrays.asList(f12.getFirst(), f41.getSecond())));

		this.i12 = net.addIpLinkBidirectional (n1, n2, 10.0);
		this.i13 = net.addIpLinkBidirectional(n1, n3, 10);
		this.i14 = net.addIpLinkBidirectional(n1, n4, 10);
		assertEquals(net.getIpLinks(), Arrays.asList(i12.getFirst(), i12.getSecond(), i13.getFirst(), i13.getSecond(), i14.getFirst(), i14.getSecond()));
		assertEquals(n1.getIncomingIpLinks(), new TreeSet<>(Arrays.asList(i12.getSecond(), i13.getSecond(), i14.getSecond())));
		assertEquals(n1.getOutgoingIpLinks(), new TreeSet<>(Arrays.asList(i12.getFirst(), i13.getFirst(), i14.getFirst())));

		this.lr12 = net.addLightpathRequest(n1, n2, 10.0, false);
		this.lr21 = net.addLightpathRequest(n2, n1, 10.0, false);
		this.lr13 = net.addLightpathRequest(n1, n3, 10.0, false);
		this.lr31 = net.addLightpathRequest(n3, n1, 10.0, false);
		this.lr14 = net.addLightpathRequest(n1, n4, 10.0, false);
		this.lr41 = net.addLightpathRequest(n4, n1, 10.0, false);
		assertEquals(net.getLightpathRequests(), Arrays.asList(lr12, lr21, lr13, lr31, lr14, lr41));
		assertEquals(n1.getIncomingLigtpathRequests(), new TreeSet<>(Arrays.asList(lr21, lr31, lr41)));
		assertEquals(n1.getOutgoingLigtpathRequests(), new TreeSet<>(Arrays.asList(lr12, lr13, lr14)));

		this.osm = OpticalSpectrumManager.createFromRegularLps(net);
		List<WFiber> fiberPath;
		fiberPath = net.getKShortestWdmPath(1, n1, n2, null).get(0);
		this.l12 = lr12.addLightpathUnregenerated(fiberPath, osm.spectrumAssignment_firstFit(fiberPath, 5, Optional.empty()).get(), false);
		this.osm = OpticalSpectrumManager.createFromRegularLps(net);
		fiberPath = net.getKShortestWdmPath(1, n2, n1, null).get(0);
		this.l21 = lr21.addLightpathUnregenerated(fiberPath, osm.spectrumAssignment_firstFit(fiberPath, 5, Optional.empty()).get(), false);
		this.osm = OpticalSpectrumManager.createFromRegularLps(net);
		fiberPath = net.getKShortestWdmPath(1, n1, n3, null).get(0);
		this.l13 = lr13.addLightpathUnregenerated(fiberPath, osm.spectrumAssignment_firstFit(fiberPath, 5, Optional.empty()).get(), false);
		this.osm = OpticalSpectrumManager.createFromRegularLps(net);
		fiberPath = net.getKShortestWdmPath(1, n3, n1, null).get(0);
		this.l31 = lr31.addLightpathUnregenerated(fiberPath, osm.spectrumAssignment_firstFit(fiberPath, 5, Optional.empty()).get(), false);
		this.osm = OpticalSpectrumManager.createFromRegularLps(net);
		fiberPath = net.getKShortestWdmPath(1, n1, n4, null).get(0);
		this.l14 = lr14.addLightpathUnregenerated(fiberPath, osm.spectrumAssignment_firstFit(fiberPath, 5, Optional.empty()).get(), false);
		this.osm = OpticalSpectrumManager.createFromRegularLps(net);
		fiberPath = net.getKShortestWdmPath(1, n4, n1, null).get(0);
		this.l41 = lr41.addLightpathUnregenerated(fiberPath, osm.spectrumAssignment_firstFit(fiberPath, 5, Optional.empty()).get(), false);
        this.osm = OpticalSpectrumManager.createFromRegularLps(net);
		assertEquals(net.getLightpaths(), Arrays.asList(l12, l21, l13, l31, l14, l41));
		assertEquals(n1.getDroppedLigtpaths(), new TreeSet<>(Arrays.asList(l21, l31, l41)));
		assertEquals(n1.getAddedLigtpaths(), new TreeSet<>(Arrays.asList(l12, l13, l14)));

		assertTrue(!lr12.isCoupledToIpLink());
		assertTrue(!i12.getFirst().isCoupledtoLpRequest());

		this.lr12.coupleToIpLink(i12.getFirst());
		this.lr21.coupleToIpLink(i12.getSecond());
		this.lr13.coupleToIpLink(i13.getFirst());
		this.lr31.coupleToIpLink(i13.getSecond());
		this.lr14.coupleToIpLink(i14.getFirst());
		this.lr41.coupleToIpLink(i14.getSecond());

		assertTrue(lr12.isCoupledToIpLink());
		assertTrue(i12.getFirst().isCoupledtoLpRequest());

		this.userService = new WUserService("service1", Arrays.asList("vnftype1", "vnftype2"), Arrays.asList("vnftype2", "vnftype1"), Arrays.asList(2.0, 3.0), Arrays.asList(2.0, 3.0), Arrays.asList(2.0, 3.0, 5.0), Arrays.asList(2.0, 3.0, 6.0), 2.0,
				true, "");
		net.addOrUpdateUserService(userService);
		assertEquals(net.getUserServiceNames(), new TreeSet<>(Arrays.asList(userService.getUserServiceUniqueId())));

		final WIpUnicastDemand unidiDemand12 = net.addIpUnicastDemand(n1, n2, true, true);
		
		this.scr13 = net.addServiceChainRequest(n1, true, userService);
		this.scr31 = net.addServiceChainRequest(n1, false, userService);
		assertEquals(net.getServiceChainRequests(), Arrays.asList(scr13, scr31));
		
		final WServiceChainRequest scr = net.addServiceChainRequest (n1 , true , userService);
		assertEquals(net.getServiceChainRequests(), Arrays.asList(scr13, scr31 , scr));
		scr.remove();
		assertEquals(net.getServiceChainRequests(), Arrays.asList(scr13, scr31));
		
		
		assertEquals(net.getIpUnicastDemands(), Arrays.asList(unidiDemand12));
		assertEquals(n1.getIncomingServiceChainRequests(), new TreeSet<>(Arrays.asList(scr31)));
		assertEquals(n1.getOutgoingServiceChainRequests(), new TreeSet<>(Arrays.asList(scr13)));
		assertEquals(n2.getOutgoingServiceChainRequests(), new TreeSet<>(Arrays.asList()));
		assertEquals(scr13.getPotentiallyValidOrigins(), new HashSet<>(Arrays.asList(n1)));
		assertEquals(scr31.getPotentiallyValidDestinations(), new HashSet<>(Arrays.asList(n1)));

		this.vnfType1 = new WVnfType("vnftype1", 1.0, 1.1, 1.2, 1.3, 1.4, Optional.empty(), "");
		this.vnfType2 = new WVnfType("vnftype2", 2.0, 2.1, 2.2, 2.3, 2.4, Optional.empty(), "");
		net.addOrUpdateVnfType(vnfType1);
		net.addOrUpdateVnfType(vnfType2);

		for (WNode n : net.getNodes())
			net.addVnfInstance(n, "name1", vnfType1);
		for (WNode n : net.getNodes())
			net.addVnfInstance(n, "name2", vnfType2);
		assertEquals(net.getVnfInstances(vnfType1.getVnfTypeName()).size(), net.getNodes().size());
		assertEquals(net.getVnfInstances(vnfType2.getVnfTypeName()).size(), net.getNodes().size());
		assertEquals(net.getVnfInstances().size(), 2 * net.getNodes().size());

		this.sc13 = scr13.addServiceChain(Arrays.asList(i12.getFirst(), n2.getVnfInstances(vnfType1.getVnfTypeName()).first(), i12.getSecond(), i13.getFirst(), n3.getVnfInstances(vnfType2.getVnfTypeName()).first()), 1.7);
		this.sc31 = scr31.addServiceChain(Arrays.asList(i13.getSecond(), n1.getVnfInstances(this.vnfType2.getVnfTypeName()).first(), n1.getVnfInstances(this.vnfType1.getVnfTypeName()).first()), 1.8);

		assertEquals(sc13.getServiceChainRequest().getDefaultSequenceOfExpansionFactorsRespectToInjection(), sc13.getCurrentExpansionFactorApplied());
		assertEquals(sc31.getServiceChainRequest().getDefaultSequenceOfExpansionFactorsRespectToInjection(), sc31.getCurrentExpansionFactorApplied());

		assertEquals(net.getServiceChains(), Arrays.asList(sc13, sc31));

		assertEquals(n1.getIncomingServiceChains(), new TreeSet<>(Arrays.asList(sc31)));
		assertEquals(n1.getOutgoingServiceChains(), new TreeSet<>(Arrays.asList(sc13)));
		assertEquals(sc13.getInitiallyInjectedTrafficGbps(), 1.7, 0.01);
		assertEquals(sc31.getInitiallyInjectedTrafficGbps(), 1.8, 0.01);
		assertEquals(n1.getVnfInstances(vnfType1.getVnfTypeName()).first().getTraversingServiceChains(), new TreeSet<>(Arrays.asList(sc31)));
		assertEquals(n1.getVnfInstances(vnfType2.getVnfTypeName()).first().getTraversingServiceChains(), new TreeSet<>(Arrays.asList(sc31)));
		assertEquals(n2.getVnfInstances(vnfType1.getVnfTypeName()).first().getTraversingServiceChains(), new TreeSet<>(Arrays.asList(sc13)));
		assertEquals(n2.getVnfInstances(vnfType2.getVnfTypeName()).first().getTraversingServiceChains(), new TreeSet<>(Arrays.asList()));
		assertEquals(n3.getVnfInstances(vnfType1.getVnfTypeName()).first().getTraversingServiceChains(), new TreeSet<>(Arrays.asList()));
		assertEquals(n3.getVnfInstances(vnfType2.getVnfTypeName()).first().getTraversingServiceChains(), new TreeSet<>(Arrays.asList(sc13)));
		net.checkConsistency();

		n1.setOpticalSwitchType(WNode.OPTICALSWITCHTYPE.FILTERLESS_DROPANDWASTENOTDIRECTIONLESS);
		n2.setOpticalSwitchType(WNode.OPTICALSWITCHTYPE.FILTERLESS_DROPANDWASTENOTDIRECTIONLESS);
        n3.setOpticalSwitchType(WNode.OPTICALSWITCHTYPE.FILTERLESS_DROPANDWASTENOTDIRECTIONLESS);
        n4.setOpticalSwitchType(WNode.OPTICALSWITCHTYPE.FILTERLESS_DROPANDWASTENOTDIRECTIONLESS);
		n5.setOpticalSwitchType(WNode.OPTICALSWITCHTYPE.FILTERLESS_DROPANDWASTENOTDIRECTIONLESS);

        this.osm = OpticalSpectrumManager.createFromRegularLps(net);
        List<List<WFiber>> res = new ArrayList<>();
        List<WFiber> res1 = new ArrayList<>();
        res1.add(this.f12.getFirst());
        res1.add(this.f23.getFirst());
        res1.add(this.f34.getFirst());
        res1.add(this.f41.getFirst());
        res.add(res1);
        List<WFiber> res2 = new ArrayList<>();
        res2.add(this.f12.getSecond());
        res2.add(this.f41.getSecond());
        res2.add(this.f34.getSecond());
        res2.add(this.f23.getSecond());
        res.add(res2);

        
        //assertEquals(this.osm.getUnavoidableLasingLoops(), res);
        net.checkConsistency();

	}

   @Test
   public void testAlgorithm ()
   {
   	final WNet net = WNet.createEmptyDesign(true , true);
   	new TopologyGenerator_example7nodesWithTraffic().executeAlgorithm(net.getNe(), new TreeMap<> (), new TreeMap<> ());
   	net.checkConsistency();
   	final Map<String,String> params = InputParameter.getDefaultParameters(new SimpleCapacityPlanningAlgorithm_v2 ().getParameters());
   	new SimpleCapacityPlanningAlgorithm_v2 ().executeAlgorithm(net.getNe(), params , new TreeMap<> ());
	
   	assert net.getIpUnicastDemands().stream().allMatch(d->d.getCurrentBlockedTraffic() < Configuration.precisionFactor);
	net.checkConsistency();


   }
    
 	@Test
 	public void testBase()
 	{
 		net.checkConsistency();
 	}
    
	@BeforeClass
	public static void prepareTest()
	{
	}

	private WNet createBasicTopology ()
	{
		final WNet wNet = WNet.createEmptyDesign(true , true);

		final WNode madrid = wNet.addNode (-3.6919444, 40.4188889 , "Madrid" , ""); madrid.setPoputlation(3265038.0);
		final WNode barcelona = wNet.addNode (2.1769444 , 41.3825 , "Barcelona" , ""); barcelona.setPoputlation(1615448.0);
		final WNode valencia = wNet.addNode (-0.375 , 39.4666667 , "Valencia" , ""); valencia.setPoputlation(798033.0);
		final WNode sevilla = wNet.addNode(-5.9833333 , 37.3833333 , "Sevilla" , ""); sevilla.setPoputlation(703021.0);
		final WNode zaragoza = wNet.addNode(-0.8833333 , 41.65 , "Zaragoza" , ""); zaragoza.setPoputlation(674725.0);
		final WNode malaga = wNet.addNode(-4.4166667 , 36.7166667 , "Malaga" , ""); malaga.setPoputlation(568030.0);
		final WNode murcia = wNet.addNode(-1.1302778 , 37.9861111 , "Murcia" , ""); murcia.setPoputlation(442203.0);
		
		wNet.addFiber(sevilla, malaga, Arrays.asList (Pair.of (1,320)) , -1, true);
		wNet.addFiber(malaga, murcia , Arrays.asList (Pair.of (1,320)) , -1, true);
		wNet.addFiber(murcia , valencia , Arrays.asList (Pair.of (1,320)) , -1, true);
		wNet.addFiber(valencia , barcelona , Arrays.asList (Pair.of (1,320)) , -1, true);
		wNet.addFiber(barcelona , zaragoza , Arrays.asList (Pair.of (1,320)) , -1, true);
		wNet.addFiber(zaragoza , madrid , Arrays.asList (Pair.of (1,320)) , -1, true);
		wNet.addFiber(madrid , sevilla, Arrays.asList (Pair.of (1,320)) , -1, true);
		wNet.addFiber(madrid , valencia, Arrays.asList (Pair.of (1,320)) , -1, true);
		
		final Random rng = new Random (1);
		for (WNode n1 : wNet.getNodes())
			for (WNode n2 : wNet.getNodes())
				if (n1.getId() < n2.getId())
				{
					final WIpUnicastDemand d12 = wNet.addIpUnicastDemand(n1, n2, true, true);
					final WIpUnicastDemand d21 = wNet.addIpUnicastDemand(n2, n1, false, true);
					d12.setBidirectionalPair(d21);
					d12.setCurrentOfferedTrafficInGbps(rng.nextDouble() * n1.getPopulation() * n2.getPopulation());
					d21.setCurrentOfferedTrafficInGbps(rng.nextDouble() * n1.getPopulation() * n2.getPopulation());
				}
		final double totalTrafficGbps = wNet.getIpUnicastDemands().stream().mapToDouble(e->e.getCurrentOfferedTrafficInGbps()).sum();
		for (WIpUnicastDemand e : wNet.getIpUnicastDemands()) e.setCurrentOfferedTrafficInGbps(e.getCurrentCarriedTrafficGbps() * 100 / totalTrafficGbps);
				
		return wNet;
	}
	
}