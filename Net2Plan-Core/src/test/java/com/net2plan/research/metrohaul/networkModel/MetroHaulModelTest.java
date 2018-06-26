/*******************************************************************************
 * Copyright (c) 2017 Pablo Pavon Marino and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the 2-clause BSD License 
 * which accompanies this distribution, and is available at
 * https://opensource.org/licenses/BSD-2-Clause
 *
 * Contributors:
 *     Pablo Pavon Marino and others - initial API and implementation
 *******************************************************************************/

package com.net2plan.research.metrohaul.networkModel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.net2plan.utils.Pair;

public class MetroHaulModelTest
{
	private WNet net;
	private WNode n1,n2,n3,n4,n5;
	private Pair<WFiber,WFiber> f12, f23, f34, f41;
	private Pair<WIpLink,WIpLink> i12, i13, i14;
	private WLightpathRequest lr12, lr21, lr13, lr31, lr14, lr41;
	private WLightpathUnregenerated l12, l21, l13, l31, l14, l41;
	private OpticalSpectrumManager osm;
	private WUserService userService;
	private WServiceChainRequest scr13 , scr31;
	private WServiceChain sc13 , sc31;
	private WVnfType vnfType1 , vnfType2;
	
	@Before
	public void setUp() throws Exception 
	{
		this.net = WNet.createEmptyDesign();
		this.n1 = net.addNode(0, 0, "n1", "type1");
		this.n2 = net.addNode(0, 0, "n2", "type1");
		this.n3 = net.addNode(0, 0, "n3", "type1");
		this.n4 = net.addNode(0, 0, "n4", "type1");
		this.n5 = net.addNode(0, 0, "n5", "type2");
		n3.setIsConnectedToNetworkCore(true);
		assertEquals (net.getNodes() , Arrays.asList(n1,n2,n3,n4,n5));
		assertTrue (n3.isConnectedToNetworkCore());
		assertTrue (!n1.isConnectedToNetworkCore());
		this.f12 = net.addFiber(n1, n2, Arrays.asList(0, 300), -1, true);
		this.f23 = net.addFiber(n2, n3, Arrays.asList(0, 300), -1, true);
		this.f34 = net.addFiber(n3, n4, Arrays.asList(0, 300), -1, true);
		this.f41 = net.addFiber(n4, n1, Arrays.asList(0, 300), -1, true);
		assertEquals (net.getFibers() , Arrays.asList(f12.getFirst() , f12.getSecond() , f23.getFirst() , f23.getSecond() , f34.getFirst() , f34.getSecond() , f41.getFirst() , f41.getSecond()));
		assertEquals (n1.getIncomingFibers() , new TreeSet<> (Arrays.asList(f12.getSecond() , f41.getFirst())));
		assertEquals (n1.getOutgoingFibers() , new TreeSet<> (Arrays.asList(f12.getFirst() , f41.getSecond())));
		
		this.i12 = net.addIpLink(n1, n2, 10.0, true);
		this.i13 = net.addIpLink(n1, n3, 10, true);
		this.i14 = net.addIpLink(n1, n4, 10, true);
		assertEquals (net.getIpLinks() , Arrays.asList(i12.getFirst() , i12.getSecond() , i13.getFirst() , i13.getSecond() , i14.getFirst() , i14.getSecond()));
		assertEquals (n1.getIncomingIpLinks() , new TreeSet<> (Arrays.asList(i12.getSecond() , i13.getSecond() , i14.getSecond())));
		assertEquals (n1.getOutgoingIpLinks() , new TreeSet<> (Arrays.asList(i12.getFirst() , i13.getFirst() , i14.getFirst())));
		
		this.lr12 = net.addLightpathRequest(n1, n2, 10.0, false);
		this.lr21 = net.addLightpathRequest(n2, n1, 10.0, false);
		this.lr13 = net.addLightpathRequest(n1, n3, 10.0, false);
		this.lr31 = net.addLightpathRequest(n3, n1, 10.0, false);
		this.lr14 = net.addLightpathRequest(n1, n4, 10.0, false);
		this.lr41 = net.addLightpathRequest(n4, n1, 10.0, false);
		assertEquals (net.getLightpathRequests() , Arrays.asList(lr12, lr21, lr13, lr31 , lr14 , lr41));
		assertEquals (n1.getIncomingLigtpathRequests() , new TreeSet<> (Arrays.asList(lr21, lr31, lr41)));
		assertEquals (n1.getOutgoingLigtpathRequests() , new TreeSet<> (Arrays.asList(lr12, lr13, lr14)));
		
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
		assertEquals (net.getLightpaths() , Arrays.asList(l12, l21, l13, l31 , l14 , l41));
		assertEquals (n1.getIncomingLigtpaths() , new TreeSet<> (Arrays.asList(l21, l31, l41)));
		assertEquals (n1.getOutgoingLigtpaths() , new TreeSet<> (Arrays.asList(l12, l13, l14)));

		assertTrue (!lr12.isCoupledToIpLink());
		assertTrue (!i12.getFirst().isCoupledtoLpRequest());
		
		this.lr12.coupleToIpLink(i12.getFirst());
		this.lr21.coupleToIpLink(i12.getSecond());
		this.lr13.coupleToIpLink(i13.getFirst());
		this.lr31.coupleToIpLink(i13.getSecond());
		this.lr14.coupleToIpLink(i14.getFirst());
		this.lr41.coupleToIpLink(i14.getSecond());

		assertTrue (lr12.isCoupledToIpLink());
		assertTrue (i12.getFirst().isCoupledtoLpRequest());

		this.userService = new WUserService("service1", Arrays.asList("vnftype1" , "vnftype2") , Arrays.asList("vnftype2" , "vnftype1"), Arrays.asList(2.0 , 3.0), Arrays.asList(2.0 , 3.0), Arrays.asList(2.0 , 3.0 , 5.0), Arrays.asList(2.0 , 3.0 , 6.0), 2.0, true, "");
		net.addOrUpdateUserService(userService);
		assertEquals (net.getUserServiceNames() , new TreeSet<> (Arrays.asList(userService.getUserServiceUniqueId())));
		
		this.scr13 = net.addServiceChainRequest(n1, true, userService);
		this.scr31 = net.addServiceChainRequest(n1, false, userService);
		assertEquals (net.getServiceChainRequests() , Arrays.asList(scr13, scr31));
		assertEquals (n1.getIncomingServiceChainRequests() , new TreeSet<> (Arrays.asList(scr31)));
		assertEquals (n1.getOutgoingServiceChainRequests() , new TreeSet<> (Arrays.asList(scr13)));
		assertEquals (n2.getOutgoingServiceChainRequests() , new TreeSet<> (Arrays.asList()));
		assertEquals (scr13.getPotentiallyValidOrigins() , new HashSet<> (Arrays.asList(n1)));
		assertEquals (scr31.getPotentiallyValidDestinations() , new HashSet<> (Arrays.asList(n1)));
		
		this.vnfType1 = new WVnfType("vnftype1", 1.0, 1.1 , 1.2, 1.3, 1.4 , false, null, "");
		this.vnfType2 = new WVnfType("vnftype2", 2.0, 2.1 , 2.2, 2.3, 2.4 , false, null, "");
		net.addOrUpdateVnfType(vnfType1);
		net.addOrUpdateVnfType(vnfType2);
		
		for (WNode n : net.getNodes()) net.addVnfInstance(n, "name1", vnfType1);
		for (WNode n : net.getNodes()) net.addVnfInstance(n, "name2", vnfType2);
		assertEquals (net.getVnfInstances(vnfType1.getVnfTypeName()).size() , net.getNodes().size());
		assertEquals (net.getVnfInstances(vnfType2.getVnfTypeName()).size() , net.getNodes().size());
		assertEquals (net.getVnfInstances().size() , 2*net.getNodes().size());
		
		this.sc13 = scr13.addServiceChain(Arrays.asList(i12.getFirst() , n2.getVnfInstances(vnfType1.getVnfTypeName()).first() , i12.getSecond() , i13.getFirst() , n3.getVnfInstances(vnfType2.getVnfTypeName()).first()), 1.7);
		this.sc31 = scr31.addServiceChain(Arrays.asList(i13.getSecond() , n1.getVnfInstances(this.vnfType2.getVnfTypeName()).first() , n1.getVnfInstances(this.vnfType1.getVnfTypeName()).first()  ), 1.8);
		assertEquals (net.getServiceChains() , Arrays.asList(sc13 , sc31));
		assertEquals (n1.getIncomingServiceChains() , new TreeSet<> (Arrays.asList(sc31)));
		assertEquals (n1.getOutgoingServiceChains() , new TreeSet<> (Arrays.asList(sc13)));
		assertEquals (sc13.getInitiallyInjectedTrafficGbps() , 1.7 , 0.01);
		assertEquals (sc31.getInitiallyInjectedTrafficGbps() , 1.8 , 0.01);
		assertEquals (n1.getVnfInstances(vnfType1.getVnfTypeName()).first().getTraversingServiceChains() , new TreeSet<> (Arrays.asList(sc31)));
		assertEquals (n1.getVnfInstances(vnfType2.getVnfTypeName()).first().getTraversingServiceChains() , new TreeSet<> (Arrays.asList(sc31)));
		assertEquals (n2.getVnfInstances(vnfType1.getVnfTypeName()).first().getTraversingServiceChains() , new TreeSet<> (Arrays.asList(sc13)));
		assertEquals (n2.getVnfInstances(vnfType2.getVnfTypeName()).first().getTraversingServiceChains() , new TreeSet<> (Arrays.asList()));
		assertEquals (n3.getVnfInstances(vnfType1.getVnfTypeName()).first().getTraversingServiceChains() , new TreeSet<> (Arrays.asList()));
		assertEquals (n3.getVnfInstances(vnfType2.getVnfTypeName()).first().getTraversingServiceChains() , new TreeSet<> (Arrays.asList(sc13)));
	}
	
	
    @BeforeClass
    public static void prepareTest()
    {
    }

    @Test
    public void testListToStringDefaultSeparator()
    {
    }

    @Test
    public void testListToStringSpecificSeparator()
    {
    }
}
