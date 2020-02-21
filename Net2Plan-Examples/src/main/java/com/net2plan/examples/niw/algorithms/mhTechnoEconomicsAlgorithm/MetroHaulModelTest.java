package com.net2plan.examples.niw.algorithms.mhTechnoEconomicsAlgorithm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.xmlbeans.impl.tool.XSTCTester.TestCase;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.niw.OadmArchitecture_generic;
import com.net2plan.niw.OadmArchitecture_generic.Parameters;
import com.net2plan.niw.OpticalSpectrumManager;
import com.net2plan.niw.OsmLightpathOccupationInfo;
import com.net2plan.niw.WFiber;
import com.net2plan.niw.WIpLink;
import com.net2plan.niw.WLightpath;
import com.net2plan.niw.WLightpathRequest;
import com.net2plan.niw.WNet;
import com.net2plan.niw.WNode;
import com.net2plan.niw.WServiceChain;
import com.net2plan.niw.WServiceChainRequest;
import com.net2plan.niw.WUserService;
import com.net2plan.niw.WVnfType;
import com.net2plan.utils.InputParameter;
import com.net2plan.utils.Pair;

// import org.junit.jupiter.api.Test;

public class MetroHaulModelTest extends TestCase
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
		System.out.println("setUp");

		this.net = WNet.createEmptyDesign(true, true);
		this.n1 = net.addNode(0, 0, "n1", "type1");
		this.n2 = net.addNode(0, 0, "n2", "type1");
		this.n3 = net.addNode(0, 0, "n3", "type1");
		this.n4 = net.addNode(0, 0, "n4", "type1");
		this.n5 = net.addNode(0, 0, "n5", "type2");
		n3.setIsConnectedToNetworkCore(true);
		assertEquals(net.getNodes(), Arrays.asList(n1, n2, n3, n4, n5));
		assertTrue(n3.isConnectedToNetworkCore());
		assertTrue(!n1.isConnectedToNetworkCore());
		this.f12 = net.addFiber(n1, n2, Arrays.asList(Pair.of (0, 300)), -1, true);
		this.f23 = net.addFiber(n2, n3, Arrays.asList(Pair.of (0, 300)), -1, true);
		this.f34 = net.addFiber(n3, n4, Arrays.asList(Pair.of (0, 300)), -1, true);
		this.f41 = net.addFiber(n4, n1, Arrays.asList(Pair.of (0, 300)), -1, true);
		this.f45 = net.addFiber(n4, n5, Arrays.asList(Pair.of (0, 300)), -1, true);
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
	    final OsmLightpathOccupationInfo lpOccupation = new OsmLightpathOccupationInfo(fiberPath, Optional.empty (), Optional.empty (), Optional.empty ());
		this.l12 = lr12.addLightpathUnregenerated(fiberPath, osm.spectrumAssignment_firstFit(lpOccupation, 5, Optional.empty() , new TreeSet<> ()).get(), false);
		this.osm = OpticalSpectrumManager.createFromRegularLps(net);
		fiberPath = net.getKShortestWdmPath(1, n2, n1, null).get(0);
		this.l21 = lr21.addLightpathUnregenerated(fiberPath, osm.spectrumAssignment_firstFit(lpOccupation, 5, Optional.empty() , new TreeSet<> ()).get(), false);
		this.osm = OpticalSpectrumManager.createFromRegularLps(net);
		fiberPath = net.getKShortestWdmPath(1, n1, n3, null).get(0);
		this.l13 = lr13.addLightpathUnregenerated(fiberPath, osm.spectrumAssignment_firstFit(lpOccupation, 5, Optional.empty() , new TreeSet<> ()).get(), false);
		this.osm = OpticalSpectrumManager.createFromRegularLps(net);
		fiberPath = net.getKShortestWdmPath(1, n3, n1, null).get(0);
		this.l31 = lr31.addLightpathUnregenerated(fiberPath, osm.spectrumAssignment_firstFit(lpOccupation, 5, Optional.empty() , new TreeSet<> ()).get(), false);
		this.osm = OpticalSpectrumManager.createFromRegularLps(net);
		fiberPath = net.getKShortestWdmPath(1, n1, n4, null).get(0);
		this.l14 = lr14.addLightpathUnregenerated(fiberPath, osm.spectrumAssignment_firstFit(lpOccupation, 5, Optional.empty() , new TreeSet<> ()).get(), false);
		this.osm = OpticalSpectrumManager.createFromRegularLps(net);
		fiberPath = net.getKShortestWdmPath(1, n4, n1, null).get(0);
		this.l41 = lr41.addLightpathUnregenerated(fiberPath, osm.spectrumAssignment_firstFit(lpOccupation, 5, Optional.empty() , new TreeSet<> ()).get(), false);
        this.osm = OpticalSpectrumManager.createFromRegularLps(net);
		assertEquals(net.getLightpaths(), Arrays.asList(l12, l21, l13, l31, l14, l41));
		assertEquals(n1.getIncomingLigtpaths(), new TreeSet<>(Arrays.asList(l21, l31, l41)));
		assertEquals(n1.getOutgoingLigtpaths(), new TreeSet<>(Arrays.asList(l12, l13, l14)));

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

		this.scr13 = net.addServiceChainRequest(n1, true, userService);
		this.scr31 = net.addServiceChainRequest(n1, false, userService);
		assertEquals(net.getServiceChainRequests(), Arrays.asList(scr13, scr31));
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

		for (WNode n : new WNode [] { n1,n2,n3,n4,n5})
		{
			final OadmArchitecture_generic oadm = n.getOpticalSwitchingArchitecture().getAsGenericArchitecture();
			final Parameters p = oadm.getParameters();
			p.setArchitectureTypeAsBroadcastAndSelect();
			oadm.updateParameters(p);
		}

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

	@BeforeClass
	public static void prepareTest()
	{
		System.out.println("prepareTest");
	}

//	@Test
	public void testImport()
	{
		final NetPlan np = new NetPlan();
		final Map<String, String> algorithmParameters = InputParameter.getDefaultParameters(new ImporterFromTimBulkFiles_forConfTimData().getParameters());

        algorithmParameters.put("excelFilePath", "resources/excelFiles/Traffic_DenseUrbanMetro_M-H_D2.3.xlsx");
        new ImporterFromTimBulkFiles_forConfTimData().executeAlgorithm(np, algorithmParameters, new HashMap<>());
        new WNet(np).checkConsistency();

        algorithmParameters.put("excelFilePath", "resources/excelFiles/Traffic_Small_M-H_D2.3.xlsx");
        new ImporterFromTimBulkFiles_forConfTimData().executeAlgorithm(np, algorithmParameters, new HashMap<>());
        new WNet(np).checkConsistency();

//        algorithmParameters.put("excelFilePath", "resources/excelFiles/Traffic_Medium_M-H_D2.3.xlsx");
//        new ImporterFromTimBulkFiles_forConfTimData().executeAlgorithm(np, algorithmParameters, new HashMap<>());
//        new WNet(np).checkConsistency();
//
//        algorithmParameters.put("excelFilePath", "resources/excelFiles/Traffic_Large_M-H_D2.3.xlsx");
//		new ImporterFromTimBulkFiles_forConfTimData().executeAlgorithm(np, algorithmParameters, new HashMap<>());
//		new WNet(np).checkConsistency();

	}

//    @Test
	public void testNodeDegree()
    {

        System.out.println("------------ Node degree test -------------");

        final NetPlan np = new NetPlan();
        final Map<String, String> algorithmParameters = InputParameter.getDefaultParameters(new ImporterFromTimBulkFiles_forConfTimData().getParameters());

        algorithmParameters.put("excelFilePath", "resources/excelFiles/Traffic_Small_M-H_D2.3.xlsx");
        algorithmParameters.put("opticalSwiInputParameter","option1");
        algorithmParameters.put("yearDataForAllTrafficTypes","2025");

        new ImporterFromTimBulkFiles_forConfTimData().executeAlgorithm(np, algorithmParameters, new HashMap<>());

        WNet wNet = new WNet(np);
        int d1 = 0;
        int d1DW = 0;
        int d2 = 0;
        int d2DW = 0;
        int d3 = 0;
        int d4 = 0;
        int d5 = 0;
        int d6 = 0;
        int d7 = 0;
        int d8 = 0;
        int d9 = 0;

        System.out.println("Nodes: " + wNet.getNodes().size());

        for (WNode node : wNet.getNodes()) {
            int nodeDegree = node.getIncomingFibers().size();

            node.getOpticalSwitchingArchitecture();

            if (nodeDegree == 1){
                d1++;
                if (node.getOpticalSwitchingArchitecture().isPotentiallyWastingSpectrum())
                    d1DW++;
            }
            else if (nodeDegree == 2) {
                d2++;
                if (node.getOpticalSwitchingArchitecture().isPotentiallyWastingSpectrum())
                    d2DW++;
            }
            else if (nodeDegree == 3)
                d3++;
            else if (nodeDegree == 4)
                d4++;
            else if (nodeDegree == 5)
                d5++;
            else if (nodeDegree == 6)
                d6++;
            else if (nodeDegree == 7)
                d7++;
            else if (nodeDegree == 8)
                d8++;
            else
                d9++;
        }

        System.out.println("Degree 1: " + d1);
        System.out.println("Degree 1 D&W: " + d1DW);
        System.out.println("Degree 2: " + d2);
        System.out.println("Degree 2 D&W: " + d2DW);
        System.out.println("Degree 3: " + d3);
        System.out.println("Degree 4: " + d4);
        System.out.println("Degree 5: " + d5);
        System.out.println("Degree 6: " + d6);
        System.out.println("Degree 7: " + d7);
        System.out.println("Degree 8: " + d8);
        System.out.println("Degree 9: " + d9);
    }

//	    @Test
	    public void testLasingLoopsInTopologies()
	    {
            System.out.println("------------ Lasing loops test -------------");

	        final NetPlan np = new NetPlan();
	        final Map<String, String> algorithmParameters = InputParameter.getDefaultParameters(new ImporterFromTimBulkFiles_forConfTimData().getParameters());

            System.out.println("************** Option 1 ****************");

            algorithmParameters.put("excelFilePath", "resources/excelFiles/Traffic_DenseUrbanMetro_M-H_D2.3.xlsx");
            algorithmParameters.put("opticalSwiInputParameter","option1");
            new ImporterFromTimBulkFiles_forConfTimData().executeAlgorithm(np, algorithmParameters, new HashMap<>());
            this.osm = OpticalSpectrumManager.createFromRegularLps(new WNet(np));
            this.unavoidableLasingLoops = this.osm.getUnavoidableLasingLoops ();
            System.out.println("unavoidableLasingLoops: " + unavoidableLasingLoops);
            assertEquals (unavoidableLasingLoops , new ArrayList<> ());
            new WNet(np).checkConsistency();

            algorithmParameters.put("excelFilePath", "resources/excelFiles/Traffic_Small_M-H_D2.3.xlsx");
            algorithmParameters.put("opticalSwiInputParameter","option1");
            new ImporterFromTimBulkFiles_forConfTimData().executeAlgorithm(np, algorithmParameters, new HashMap<>());
            this.osm = OpticalSpectrumManager.createFromRegularLps(new WNet(np));
            this.unavoidableLasingLoops = this.osm.getUnavoidableLasingLoops ();
            System.out.println("unavoidableLasingLoops: " + unavoidableLasingLoops);
            assertEquals (unavoidableLasingLoops , new ArrayList<> ());
            new WNet(np).checkConsistency();

//            algorithmParameters.put("excelFilePath", "resources/excelFiles/Traffic_Medium_M-H_D2.3.xlsx");
//            algorithmParameters.put("opticalSwiInputParameter","option1");
//            new ImporterFromTimBulkFiles_forConfTimData().executeAlgorithm(np, algorithmParameters, new HashMap<>());
//            this.osm = OpticalSpectrumManager.createFromRegularLps(new WNet(np));
//            this.unavoidableLasingLoops = this.osm.getUnavoidableLasingLoops ();
//            System.out.println("unavoidableLasingLoops: " + unavoidableLasingLoops);
//            assertEquals (unavoidableLasingLoops , new ArrayList<> ());
//            new WNet(np).checkConsistency();
//
//            algorithmParameters.put("excelFilePath", "resources/excelFiles/Traffic_Large_M-H_D2.3.xlsx");
//            algorithmParameters.put("opticalSwiInputParameter","option1");
//            new ImporterFromTimBulkFiles_forConfTimData().executeAlgorithm(np, algorithmParameters, new HashMap<>());
//            this.osm = OpticalSpectrumManager.createFromRegularLps(new WNet(np));
//            this.unavoidableLasingLoops = this.osm.getUnavoidableLasingLoops ();
//            System.out.println("unavoidableLasingLoops: " + unavoidableLasingLoops);
//            assertEquals (unavoidableLasingLoops , new ArrayList<> ());
//            new WNet(np).checkConsistency();


            System.out.println("************** Option 2 ****************");

            algorithmParameters.put("excelFilePath", "resources/excelFiles/Traffic_DenseUrbanMetro_M-H_D2.3.xlsx");
            algorithmParameters.put("opticalSwiInputParameter","option2");
            new ImporterFromTimBulkFiles_forConfTimData().executeAlgorithm(np, algorithmParameters, new HashMap<>());
            this.osm = OpticalSpectrumManager.createFromRegularLps(new WNet(np));
            this.unavoidableLasingLoops = this.osm.getUnavoidableLasingLoops ();
            System.out.println("unavoidableLasingLoops: " + unavoidableLasingLoops);
            assertEquals (unavoidableLasingLoops , new ArrayList<> ());
            new WNet(np).checkConsistency();

            algorithmParameters.put("excelFilePath", "resources/excelFiles/Traffic_Small_M-H_D2.3.xlsx");
            algorithmParameters.put("opticalSwiInputParameter","option2");
            new ImporterFromTimBulkFiles_forConfTimData().executeAlgorithm(np, algorithmParameters, new HashMap<>());
            this.osm = OpticalSpectrumManager.createFromRegularLps(new WNet(np));
            this.unavoidableLasingLoops = this.osm.getUnavoidableLasingLoops ();
            System.out.println("unavoidableLasingLoops: " + unavoidableLasingLoops);
            assertEquals (unavoidableLasingLoops , new ArrayList<> ());
            new WNet(np).checkConsistency();

//            algorithmParameters.put("excelFilePath", "resources/excelFiles/Traffic_Medium_M-H_D2.3.xlsx");
//            algorithmParameters.put("opticalSwiInputParameter","option2");
//            new ImporterFromTimBulkFiles_forConfTimData().executeAlgorithm(np, algorithmParameters, new HashMap<>());
//            this.osm = OpticalSpectrumManager.createFromRegularLps(new WNet(np));
//            this.unavoidableLasingLoops = this.osm.getUnavoidableLasingLoops ();
//            System.out.println("unavoidableLasingLoops: " + unavoidableLasingLoops);
//            assertEquals (unavoidableLasingLoops , new ArrayList<> ());
//            new WNet(np).checkConsistency();
//
//            algorithmParameters.put("excelFilePath", "resources/excelFiles/Traffic_Large_M-H_D2.3.xlsx");
//            algorithmParameters.put("opticalSwiInputParameter","option2");
//            new ImporterFromTimBulkFiles_forConfTimData().executeAlgorithm(np, algorithmParameters, new HashMap<>());
//            this.osm = OpticalSpectrumManager.createFromRegularLps(new WNet(np));
//            this.unavoidableLasingLoops = this.osm.getUnavoidableLasingLoops ();
//            System.out.println("unavoidableLasingLoops: " + unavoidableLasingLoops);
//            assertEquals (unavoidableLasingLoops , new ArrayList<> ());
//            new WNet(np).checkConsistency();

	    }

//	@Test
	public void testWithCapPlanning()
	{
        System.out.println("------------ Capacity planning test -------------");

		final NetPlan np = new NetPlan();
		final Map<String, String> algorithmParameters = InputParameter.getDefaultParameters(new ImporterFromTimBulkFiles_forConfTimData().getParameters());

		algorithmParameters.put("excelFilePath", "resources/excelFiles/Traffic_DenseUrbanMetro_M-H_D2.3.xlsx");
		new ImporterFromTimBulkFiles_forConfTimData().executeAlgorithm(np, algorithmParameters, new HashMap<>());
		new WNet(np).checkConsistency();
		new CapacityPlanningAlgorithm_v1().executeAlgorithm(np, InputParameter.getDefaultParameters(new CapacityPlanningAlgorithm_v1().getParameters()), new HashMap<>());
		new WNet(np).checkConsistency();

		algorithmParameters.put("excelFilePath", "resources/excelFiles/Traffic_Small_M-H_D2.3.xlsx");
		new ImporterFromTimBulkFiles_forConfTimData().executeAlgorithm(np, algorithmParameters, new HashMap<>());
		new WNet(np).checkConsistency();
		new CapacityPlanningAlgorithm_v1().executeAlgorithm(np, InputParameter.getDefaultParameters(new CapacityPlanningAlgorithm_v1().getParameters()), new HashMap<>());
		new WNet(np).checkConsistency();

//		algorithmParameters.put("excelFilePath", "resources/excelFiles/Traffic_Medium_M-H_D2.3.xlsx");
//		new ImporterFromTimBulkFiles_forConfTimData().executeAlgorithm(np, algorithmParameters, new HashMap<>());
//		new WNet(np).checkConsistency();
//		new CapacityPlanningAlgorithm_v1().executeAlgorithm(np, InputParameter.getDefaultParameters(new CapacityPlanningAlgorithm_v1().getParameters()), new HashMap<>());
//		new WNet(np).checkConsistency();
//
//		algorithmParameters.put("excelFilePath", "resources/excelFiles/Traffic_Large_M-H_D2.3.xlsx");
//		new ImporterFromTimBulkFiles_forConfTimData().executeAlgorithm(np, algorithmParameters, new HashMap<>());
//		new WNet(np).checkConsistency();
//		new CapacityPlanningAlgorithm_v1().executeAlgorithm(np, InputParameter.getDefaultParameters(new CapacityPlanningAlgorithm_v1().getParameters()), new HashMap<>());
//		new WNet(np).checkConsistency();
	}

//    @Test
    public void testStatsClass()
    {

        System.out.println("------------ Stats test -------------");

        final NetPlan np = new NetPlan();
        final Map<String, String> algorithmParameters = InputParameter.getDefaultParameters(new ImporterFromTimBulkFiles_forConfTimData().getParameters());

        algorithmParameters.put("excelFilePath", "resources/excelFiles/Traffic_DenseUrbanMetro_M-H_D2.3.xlsx");
//        algorithmParameters.put("excelFilePath", "resources/excelFiles/Traffic_Small_M-H_D2.3.xlsx");
//        algorithmParameters.put("excelFilePath", "resources/excelFiles/Traffic_Medium_M-H_D2.3.xlsx");
//        algorithmParameters.put("excelFilePath", "resources/excelFiles/Traffic_Large_M-H_D2.3.xlsx");

        algorithmParameters.put("opticalSwiInputParameter","option1");   // All ROADMs
//        algorithmParameters.put("opticalSwiInputParameter","option2"); // With D&W Filterless

        algorithmParameters.put("initialIndex","-1"); // Static planning
//        algorithmParameters.put("initialIndex","-1"); // Dynamic planning

        algorithmParameters.put("yearDataForAllTrafficTypes","2019");
//        algorithmParameters.put("yearDataForAllTrafficTypes","2022");
//        algorithmParameters.put("yearDataForAllTrafficTypes","2025");

        algorithmParameters.put("addFaultToleranceToMcenBBFailureAndWdmDuctFailureIfPossible","false"); // Without failure tolerance
//        algorithmParameters.put("addFaultToleranceToMcenBBFailureAndWdmDuctFailureIfPossible","true"); // With failure tolerance

        new ImporterFromTimBulkFiles_forConfTimData().executeAlgorithm(np, algorithmParameters, new HashMap<>());
        new AssessmentBenefitsOfDynamicity().executeAlgorithm(np, algorithmParameters, new HashMap<>());
        new WNet(np).checkConsistency();

    }

    @Test
    public void testBillOfMaterials()
    {
        final NetPlan np = new NetPlan();
        final Map<String, String> algorithmParameters = InputParameter.getDefaultParameters(new ImporterFromTimBulkFiles_forConfTimData().getParameters());

        algorithmParameters.put("excelFilePath", "resources/excelFiles/Traffic_DenseUrbanMetro_M-H_D2.3.xlsx");
//        algorithmParameters.put("excelFilePath", "resources/excelFiles/Traffic_Small_M-H_D2.3.xlsx");
        algorithmParameters.put("opticalSwiInputParameter","option2");   // Option 1: All Roadm, Option2: with D&W Filterless
        algorithmParameters.put("initialIndex","-1"); // -1: Static planning, 0: Dynamic planning
        algorithmParameters.put("yearDataForAllTrafficTypes","2025"); // 2019 2022 2025
        algorithmParameters.put("addFaultToleranceToMcenBBFailureAndWdmDuctFailureIfPossible","true"); // false: Without failure tolerance, true: with faultTolerance


        new ImporterFromTimBulkFiles_forConfTimData().executeAlgorithm(np, algorithmParameters, new HashMap<>());
        new AssessmentBenefitsOfDynamicity().executeAlgorithm(np, algorithmParameters, new HashMap<>());

        new BillOfMaterialsOptical_v1().executeAlgorithm(np, algorithmParameters, new HashMap<>());

    }


}