package com.net2plan.niw.networkModel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.TreeSet;

import org.apache.xmlbeans.impl.tool.XSTCTester.TestCase;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.net2plan.niw.OadmArchitecture_generic;
import com.net2plan.niw.OpticalAmplifierInfo;
import com.net2plan.niw.OpticalSimulationModule;
import com.net2plan.niw.OpticalSpectrumManager;
import com.net2plan.niw.OsmLightpathOccupationInfo;
import com.net2plan.niw.WFiber;
import com.net2plan.niw.WIpLink;
import com.net2plan.niw.WIpUnicastDemand;
import com.net2plan.niw.WLightpath;
import com.net2plan.niw.WLightpathRequest;
import com.net2plan.niw.WNet;
import com.net2plan.niw.WNode;
import com.net2plan.niw.WServiceChain;
import com.net2plan.niw.WServiceChainRequest;
import com.net2plan.niw.WUserService;
import com.net2plan.niw.WVnfType;
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

		this.i12 = net.addIpLinkBidirectional(n1, n2, 10.0);
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
		this.l12 = lr12.addLightpathUnregenerated(fiberPath, osm.spectrumAssignment_firstFit(new OsmLightpathOccupationInfo(fiberPath, Optional.empty(), Optional.empty(), Optional.empty()), 5, Optional.empty() , new TreeSet<> ()).get(), false);
		this.osm = OpticalSpectrumManager.createFromRegularLps(net);
		fiberPath = net.getKShortestWdmPath(1, n2, n1, null).get(0);
		this.l21 = lr21.addLightpathUnregenerated(fiberPath, osm.spectrumAssignment_firstFit(new OsmLightpathOccupationInfo(fiberPath, Optional.empty(), Optional.empty(), Optional.empty()), 5, Optional.empty() , new TreeSet<> ()).get(), false);
		this.osm = OpticalSpectrumManager.createFromRegularLps(net);
		fiberPath = net.getKShortestWdmPath(1, n1, n3, null).get(0);
		this.l13 = lr13.addLightpathUnregenerated(fiberPath, osm.spectrumAssignment_firstFit(new OsmLightpathOccupationInfo(fiberPath, Optional.empty(), Optional.empty(), Optional.empty()), 5, Optional.empty() , new TreeSet<> ()).get(), false);
		this.osm = OpticalSpectrumManager.createFromRegularLps(net);
		fiberPath = net.getKShortestWdmPath(1, n3, n1, null).get(0);
		this.l31 = lr31.addLightpathUnregenerated(fiberPath, osm.spectrumAssignment_firstFit(new OsmLightpathOccupationInfo(fiberPath, Optional.empty(), Optional.empty(), Optional.empty()), 5, Optional.empty() , new TreeSet<> ()).get(), false);
		this.osm = OpticalSpectrumManager.createFromRegularLps(net);
		fiberPath = net.getKShortestWdmPath(1, n1, n4, null).get(0);
		this.l14 = lr14.addLightpathUnregenerated(fiberPath, osm.spectrumAssignment_firstFit(new OsmLightpathOccupationInfo(fiberPath, Optional.empty(), Optional.empty(), Optional.empty()), 5, Optional.empty() , new TreeSet<> ()).get(), false);
		this.osm = OpticalSpectrumManager.createFromRegularLps(net);
		fiberPath = net.getKShortestWdmPath(1, n4, n1, null).get(0);
		this.l41 = lr41.addLightpathUnregenerated(fiberPath, osm.spectrumAssignment_firstFit(new OsmLightpathOccupationInfo(fiberPath, Optional.empty(), Optional.empty(), Optional.empty()), 5, Optional.empty() , new TreeSet<> ()).get(), false);
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

		n1.setOpticalSwitchArchitecture(OadmArchitecture_generic.class);
		n2.setOpticalSwitchArchitecture(OadmArchitecture_generic.class);
        n3.setOpticalSwitchArchitecture(OadmArchitecture_generic.class);
        n4.setOpticalSwitchArchitecture(OadmArchitecture_generic.class);
		n5.setOpticalSwitchArchitecture(OadmArchitecture_generic.class);

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
   	final WNet netIp = createBasicTopology_ex7nodesWithTraff (true , false);
   	final WNet netIpOverWdm = createBasicTopology_ex7nodesWithTraff (true , true);
   	netIp.checkConsistency();
   	netIpOverWdm.checkConsistency();
   	netIp.updateNetPlanObjectInternalState();
   	netIpOverWdm.updateNetPlanObjectInternalState();
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

	private WNet createBasicTopology_ex7nodesWithTraff (boolean withIpLayer , boolean withWdmLayer)
	{
		final WNet wNet = WNet.createEmptyDesign(withIpLayer , withWdmLayer);

		final WNode madrid = wNet.addNode (-3.6919444, 40.4188889 , "Madrid" , ""); madrid.setPoputlation(3265038.0);
		final WNode barcelona = wNet.addNode (2.1769444 , 41.3825 , "Barcelona" , ""); barcelona.setPoputlation(1615448.0);
		final WNode valencia = wNet.addNode (-0.375 , 39.4666667 , "Valencia" , ""); valencia.setPoputlation(798033.0);
		final WNode sevilla = wNet.addNode(-5.9833333 , 37.3833333 , "Sevilla" , ""); sevilla.setPoputlation(703021.0);
		final WNode zaragoza = wNet.addNode(-0.8833333 , 41.65 , "Zaragoza" , ""); zaragoza.setPoputlation(674725.0);
		final WNode malaga = wNet.addNode(-4.4166667 , 36.7166667 , "Malaga" , ""); malaga.setPoputlation(568030.0);
		final WNode murcia = wNet.addNode(-1.1302778 , 37.9861111 , "Murcia" , ""); murcia.setPoputlation(442203.0);
		
		if (!withWdmLayer)
		{
			wNet.addIpLinkBidirectional(sevilla, malaga, 50.0);
			wNet.addIpLinkBidirectional(malaga, murcia, 50.0);
			wNet.addIpLinkBidirectional(murcia , valencia, 50.0);
			wNet.addIpLinkBidirectional(valencia , barcelona, 50.0);
			wNet.addIpLinkBidirectional(barcelona , zaragoza, 50.0);
			wNet.addIpLinkBidirectional(zaragoza , madrid, 50.0);
			wNet.addIpLinkBidirectional(madrid , sevilla, 50.0);
			wNet.addIpLinkBidirectional(madrid , valencia, 50.0);
		} else
		{
			wNet.addFiber(sevilla, malaga, Arrays.asList(Pair.of(1,320)) , -1, true);
			wNet.addFiber(malaga, murcia , Arrays.asList(Pair.of(1,320)) , -1, true);
			wNet.addFiber(murcia , valencia , Arrays.asList(Pair.of(1,320)) , -1, true);
			wNet.addFiber(valencia , barcelona , Arrays.asList(Pair.of(1,320)) , -1, true);
			wNet.addFiber(barcelona , zaragoza , Arrays.asList(Pair.of(1,320)) , -1, true);
			wNet.addFiber(zaragoza , madrid , Arrays.asList(Pair.of(1,320)) , -1, true);
			wNet.addFiber(madrid , sevilla, Arrays.asList(Pair.of(1,320)) , -1, true);
			wNet.addFiber(madrid , valencia, Arrays.asList(Pair.of(1,320)) , -1, true);
		}

		if (withIpLayer)
		{
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
			for (WIpUnicastDemand e : wNet.getIpUnicastDemands()) e.setCurrentOfferedTrafficInGbps(e.getCurrentOfferedTrafficInGbps() * 100 / totalTrafficGbps);
		}
		
		return wNet;
	}

	
	@Test
	public void opticalSignalTests () 
	{
		final WNet net = WNet.createEmptyDesign(false, true);
		final WNode a = net.addNode(0, 0, "A", "");
		final WNode b = net.addNode(0, 0, "B", "");
		final WNode c = net.addNode(0, 0, "C", "");
		final WFiber ab = net.addFiber(a, b, null, 160.0, false).getFirst();
		final WFiber bc = net.addFiber(b, c, null, 160.0, false).getFirst();
		final List<WFiber> fibers = Arrays.asList(ab , bc);
		final List<WNode> nodes = Arrays.asList(a , b , c);
		for (WNode n : nodes)
		{
			final OadmArchitecture_generic arq = (OadmArchitecture_generic) n.getOpticalSwitchingArchitecture();
			final OadmArchitecture_generic.Parameters param = arq.getParameters();
			param.setArchitectureTypeAsFilterless();
			param.setAddDropModuleTypeAsMuxBased();
			param.setMuxDemuxLoss_dB(6.0);
			param.setMuxDemuxPmd_ps(0.5);
			param.setDegreeSplitterCombinerLoss_dB(Optional.of(3.0));
			param.setDirlessAddDropSplitterCombinerLoss_dB(Optional.empty());
			n.setIsOadmWithDirectedAddDropModulesInTheDegrees(true);
			n.setOadmNumAddDropDirectionlessModules(1);
			arq.updateParameters(param);
		}
		fibers.forEach(e->e.setAttenuationCoefficient_dbPerKm(0.25));
		fibers.forEach(e->e.setChromaticDispersionCoeff_psPerNmKm(15.0));
		fibers.forEach(e->e.setPmdLinkDesignValueCoeff_psPerSqrtKm(0.5));
		fibers.forEach(e->e.setIsExistingBoosterAmplifierAtOriginOadm(true));
		fibers.forEach(e->e.setIsExistingPreamplifierAtDestinationOadm(true));
		fibers.forEach(e->e.setOriginOadmSpectrumEqualizationTargetBeforeBooster_mwPerGhz(Optional.empty()));
		final OpticalAmplifierInfo booster = OpticalAmplifierInfo.getDefaultBooster();
		booster.setGainDb(6.0);
		booster.setNoiseFigureDb(6.0);
		booster.setCdCompensationPsPerNm(-10.0);
		booster.setPmdPs(0.5);
		fibers.forEach(e->e.setOriginBoosterAmplifierInfo(booster));
		final OpticalAmplifierInfo pream = OpticalAmplifierInfo.getDefaultPreamplifier();
		pream.setGainDb(20.0);
		pream.setNoiseFigureDb(6.0);
		pream.setCdCompensationPsPerNm(-10.0);
		pream.setPmdPs(0.5);
		fibers.forEach(e->e.setDestinationPreAmplifierInfo(pream));
		final double olasPmd_ps = 0.5;
		final double olasCdCompensation_psPerNm = -100.0;
		final OpticalAmplifierInfo ola = OpticalAmplifierInfo.getDefaultOla(80.0);
		ola.setGainDb(20.0);
		ola.setNoiseFigureDb(6.0);
		ola.setCdCompensationPsPerNm(olasCdCompensation_psPerNm);
		ola.setPmdPs(olasPmd_ps);
		fibers.forEach(e->e.setOlaTraversedInfo(Arrays.asList (ola)));
		for (int cont = 0 ; cont < 10 ; cont ++)
		{
			final WLightpathRequest lpr = net.addLightpathRequest(a, c, 100.0, false);
			final int s0 = cont * 4;
			lpr.addLightpathUnregenerated(Arrays.asList(ab , bc), new TreeSet<> (Arrays.asList(s0 , s0+1 , s0+2 , s0+3)), false);
		}
		final List<WLightpath> lps = net.getLightpaths();
		lps.forEach(e->e.setAddTransponderInjectionPower_dBm(0.0));
		final OpticalSimulationModule osm = new OpticalSimulationModule (net).updateAllPerformanceInfo();
		for (WLightpath lp : lps)
		{
			assertEquals (lp.getAddTransponderInjectionPower_dBm() ,  0.0 , 1e-3);
			// -10log(2) from output degree combiner 
			assertEquals (osm.getOpticalPerformanceOfLightpathAtFiberEndsAfterBoosterBeforePreamplifier(ab, lp).getFirst().getPower_dbm() ,  -3.0 , 1e-1);
			assertEquals (osm.getOpticalPerformanceOfLightpathAtFiberEndsAfterBoosterBeforePreamplifier(ab, lp).getSecond().getPower_dbm() ,  -23.0 , 1e-1);
			assertEquals (osm.getOpticalPerformanceOfLightpathAtFiberEndsAfterBoosterBeforePreamplifier(bc, lp).getFirst().getPower_dbm() ,  -3.0 , 1e-1);
			assertEquals (osm.getOpticalPerformanceOfLightpathAtFiberEndsAfterBoosterBeforePreamplifier(bc, lp).getSecond().getPower_dbm() ,  -23.0 , 1e-1);
			assertEquals (osm.getOpticalPerformanceAtTransponderReceiverEnd(lp).getPower_dbm() ,  -23.0 + 20 - 3  - 6.0, 1e-1);
			assertEquals (osm.getOpticalPerformanceOfLightpathAtLineAmplifierInputAndOutput(lp, ab, 0).getFirst().getPower_dbm() ,  -23.0 , 1e-1);
			assertEquals (osm.getOpticalPerformanceOfLightpathAtLineAmplifierInputAndOutput(lp, ab, 0).getSecond().getPower_dbm() ,  -3.0 , 1e-1);
			assertEquals (osm.getOpticalPerformanceOfLightpathAtLineAmplifierInputAndOutput(lp, bc, 0).getFirst().getPower_dbm() ,  -23.0  , 1e-1);
			assertEquals (osm.getOpticalPerformanceOfLightpathAtLineAmplifierInputAndOutput(lp, bc, 0).getSecond().getPower_dbm() ,  -3.0 , 1e-1);
		}
		for (WFiber e : net.getFibers())
		{
			assertEquals (osm.getTotalPowerAtLineAmplifierInput_dBm(e, 0) , linear2dB(lps.stream().mapToDouble(lp->dB2linear(osm.getOpticalPerformanceOfLightpathAtLineAmplifierInputAndOutput(lp, e, 0).getFirst().getPower_dbm())).sum ()) , 1e-1);
			assertEquals (osm.getTotalPowerAtLineAmplifierOutput_dBm(e, 0) , linear2dB(lps.stream().mapToDouble(lp->dB2linear(osm.getOpticalPerformanceOfLightpathAtLineAmplifierInputAndOutput(lp, e, 0).getSecond().getPower_dbm())).sum ()) , 1e-1);
			assertEquals (osm.getTotalPowerAtFiberEndsAfterBoosterBeforePreamplifier_dBm(e).getFirst() , linear2dB(lps.stream().mapToDouble(lp->dB2linear(osm.getOpticalPerformanceOfLightpathAtFiberEndsAfterBoosterBeforePreamplifier(e , lp).getFirst().getPower_dbm())).sum ()) , 1e-1);
			assertEquals (osm.getTotalPowerAtFiberEndsAfterBoosterBeforePreamplifier_dBm(e).getSecond() , linear2dB(lps.stream().mapToDouble(lp->dB2linear(osm.getOpticalPerformanceOfLightpathAtFiberEndsAfterBoosterBeforePreamplifier(e , lp).getSecond().getPower_dbm())).sum ()) , 1e-1);
		}
		
		for (WLightpath lp : net.getLightpaths())
		{
			final double noisePartAll_linear = OpticalSimulationModule.dB2linear(6.0) * OpticalSimulationModule.constant_h * lp.getCentralFrequencyThz() * 1e12 * 12.5e9;
			final double powerPartBoosters_linear = OpticalSimulationModule.dB2linear(-3.0) / 1000.0;
			final double powerPartOlasAndPreampl_linear = OpticalSimulationModule.dB2linear(-23.0) / 1000.0;
			final double osnrTotal_linear = 1.0 / (2*(noisePartAll_linear / powerPartBoosters_linear) + 3*(noisePartAll_linear / powerPartOlasAndPreampl_linear));
			final double osnrTotal_dB = OpticalSimulationModule.linear2dB(osnrTotal_linear);
			assertEquals (osm.getOpticalPerformanceAtTransponderReceiverEnd(lp).getOsnrAt12_5GhzRefBw() , osnrTotal_dB , 1.5);
			
			final double totalLengthKm = ab.getLengthInKm() + bc.getLengthInKm();
			final double cdEnd = ab.getChromaticDispersionCoeff_psPerNmKm() * totalLengthKm + // fiber 
					2 * olasCdCompensation_psPerNm + // OLAs 
					2 * (-10.0) + // booster
					2 * (-10.0); // pre-amplifiers
			assertEquals (cdEnd , osm.getOpticalPerformanceAtTransponderReceiverEnd(lp).getCd_psPerNm() , 1e-3);
			final double pmdSquare = Math.pow(0.5, 2) * totalLengthKm + // fiber 
					2 * Math.pow(0.5, 2) + // MUX / DEMUX
					2 * Math.pow(0.5, 2) + // OLAs 
					2 * Math.pow(0.5, 2) + // booster
					2 * Math.pow(0.5, 2); // pre-amplifiers
			assertEquals (pmdSquare , osm.getOpticalPerformanceAtTransponderReceiverEnd(lp).getPmdSquared_ps2() , 1e-3);

			
		}

	}

    public static double dB2linear(double dB)
    {
        return Math.pow(10, dB / 10);
    }
    public static double linear2dB(double num)
    {
        return num == 0? -Double.MAX_VALUE : 10 * Math.log10(num);
    }

		
}