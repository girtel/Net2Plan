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

package com.net2plan.interfaces.networkDesign;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.*;

import com.google.common.collect.Sets;
import com.net2plan.libraries.GraphUtils.ClosedCycleRoutingException;
import com.net2plan.utils.Constants.RoutingCycleType;
import com.net2plan.utils.Constants.RoutingType;
import com.net2plan.utils.Pair;
import org.junit.rules.TemporaryFolder;

public class DemandTest
{
	private NetPlan np = null;
	private Node n1, n2 , n3;
	private Link link12, link23 , link13;
	private Demand d13, d12 , scd123;
	private Route r12, r123a, r123b , sc123;
	private List<Link> path13;
	private List<NetworkElement> pathSc123;
	private Resource res2 , res2backup;
	private Route segm13;
	private NetworkLayer lowerLayer , upperLayer;
	private Link upperLink12;

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception 
	{
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception 
	{
		this.np = new NetPlan ();
		this.lowerLayer = np.getNetworkLayerDefault();
		np.setDemandTrafficUnitsName("Mbps" , lowerLayer);
		this.upperLayer = np.addLayer("upperLayer" , "description" , "Mbps" , "upperTrafficUnits" , null , null);
		this.n1 = this.np.addNode(0 , 0 , "node1" , null);
		this.n2 = np.addNode(0 , 0 , "node2" , null);
		this.n3 = np.addNode(0 , 0 , "node3" , null);
		this.link12 = np.addLink(n1,n2,100,100,1,null,lowerLayer);
		this.link23 = np.addLink(n2,n3,100,100,1,null,lowerLayer);
		this.link13 = np.addLink(n1,n3,100,100,1,null,lowerLayer);
		this.d13 = np.addDemand(n1 , n3 , 3 , null,lowerLayer);
		d13.setIntendedRecoveryType(Demand.IntendedRecoveryType.NONE);
		this.d12 = np.addDemand(n1, n2, 3 , null,lowerLayer);
		this.r12 = np.addRoute(d12,1,1.5,Collections.singletonList(link12),null);
		this.path13 = new LinkedList<Link> (); path13.add(link12); path13.add(link23);
		this.r123a = np.addRoute(d13,1,1.5,path13,null);
		this.r123b = np.addRoute(d13,1,1.5,path13,null);
		this.res2 = np.addResource("type" , "name" , n2 , 100 , "Mbps" , null , 10 , null);
		this.res2backup = np.addResource("type" , "name" , n2 , 100 , "Mbps" , null , 10 , null);
		this.scd123 = np.addDemand(n1 , n3 , 3 , null,lowerLayer);
		this.scd123.setServiceChainSequenceOfTraversedResourceTypes(Collections.singletonList("type"));
		this.pathSc123 = Arrays.asList(link12 ,res2 , link23); 
		this.sc123 = np.addServiceChain(scd123 , 100 , Arrays.asList(300.0 , 50.0 , 302.0) , pathSc123 , null); 
		this.segm13 = np.addRoute(d13 , 0 , 50 , Collections.singletonList(link13) , null);
		this.r123a.addBackupRoute(segm13);
		this.upperLink12 = np.addLink(n1,n2,10,100,1,null,upperLayer);
		this.d12.coupleToUpperLayerLink(upperLink12);

		np.checkCachesConsistency();

		temporaryFolder.create();
	}


	@After
	public void tearDown() throws Exception 
	{
		np.checkCachesConsistency();

		temporaryFolder.delete();
	}

	@Test
	public void testBidirectional () throws IOException
	{
		Pair<Demand,Demand> pair = np.addDemandBidirectional(n1, n2, 1, null);
		assertTrue (pair.getFirst().isBidirectional());
		assertTrue (pair.getSecond().isBidirectional());
		assertEquals (pair.getFirst().getBidirectionalPair() , pair.getSecond());
		assertEquals (pair.getSecond().getBidirectionalPair() , pair.getFirst());
		pair.getFirst().remove();
		assertTrue (!pair.getSecond().isBidirectional());
		assertEquals (pair.getSecond().getBidirectionalPair() , null);
		
		Demand otherDemand = np.addDemand(n1, n3, 1, null);
		try { otherDemand.setBidirectionalPair(pair.getFirst()); fail (); } catch (Exception e) {}
		
		Demand otherDemand2 = np.addDemand(n1, n2, 1, null);
		pair.getSecond().setBidirectionalPair(otherDemand2);
		assertTrue (pair.getSecond().isBidirectional());
		assertEquals (otherDemand2.getBidirectionalPair() , pair.getSecond());
		assertEquals (pair.getSecond().getBidirectionalPair() , otherDemand2);

		File f = temporaryFolder.newFile("temp.n2p");
		this.np.saveToFile(f);
		NetPlan readNp = new NetPlan (f);
		assertTrue(readNp.isDeepCopy(np));
		assertTrue(np.isDeepCopy(readNp));
	}
	
	
	@Test
	public void testGetRoutes() 
	{
		assertEquals (d13.getRoutes() , new HashSet<Route> (Arrays.asList(r123a , r123b , segm13)));
		assertEquals (d12.getRoutes() , Collections.singleton(r12));
		assertEquals (scd123.getRoutes() , Collections.singleton(sc123));
		r123b.remove ();
		assertEquals (d13.getRoutes() , new HashSet<Route> (Arrays.asList(r123a , segm13)));
	}

	@Test
	public void testGetRecoveryType() 
	{
		assertEquals (d13.getIntendedRecoveryType() , Demand.IntendedRecoveryType.NONE);
		assertEquals (d12.getIntendedRecoveryType() , Demand.IntendedRecoveryType.NOTSPECIFIED);
		d13.setIntendedRecoveryType(Demand.IntendedRecoveryType.PROTECTION_NOREVERT);
		assertEquals (d13.getIntendedRecoveryType() , Demand.IntendedRecoveryType.PROTECTION_NOREVERT);
	}

	@Test
	public void testAddTag() 
	{
		d13.addTag ("t1"); d13.addTag ("t2"); d13.addTag ("t1"); 
		d12.addTag("t1");
		assertEquals (d13.getTags() , new HashSet<String> (Arrays.asList("t1" , "t2")));
		assertEquals (np.getTaggedDemands ("t1" , d13.getLayer()) , Sets.newHashSet(d13 , d12));
		assertEquals (np.getTaggedDemands ("xxx" , d13.getLayer()) , Sets.newHashSet());
	}

	@Test
	public void testRemoveTag() 
	{
		d13.addTag ("t1"); d13.addTag ("t2"); d13.addTag ("t1"); 
		d13.removeTag ("ssss");
		assertEquals (d13.getTags() , new HashSet<String> (Arrays.asList("t1" , "t2")));
		d13.removeTag ("t2");
		assertEquals (d13.getTags() , new HashSet<String> (Arrays.asList("t1")));
		d13.removeTag ("t1");
		assertEquals (d13.getTags() , new HashSet<String> (Arrays.asList()));
	}
	
	@Test
	public void testGetRoutesAreBackup() 
	{
		assertEquals (d13.getRoutesAreBackup() , new HashSet<Route> (Arrays.asList(segm13)));
		assertEquals (d12.getRoutesAreBackup() , Collections.emptySet());
		assertEquals (scd123.getRoutesAreBackup() , Collections.emptySet());
		segm13.remove ();
		assertEquals (d13.getRoutesAreBackup() , Collections.emptySet());
	}

	@Test
	public void testGetRoutesAreNotBackup() 
	{
		assertEquals (d13.getRoutesAreNotBackup() , new HashSet<Route> (Arrays.asList(r123a , r123b)));
		assertEquals (d12.getRoutesAreNotBackup() , new HashSet<Route> (Arrays.asList(r12)));
		assertEquals (scd123.getRoutesAreNotBackup() , new HashSet<Route> (Arrays.asList(sc123)));
		segm13.remove ();
		assertEquals (d13.getRoutesAreNotBackup() , new HashSet<Route> (Arrays.asList(r123a , r123b)));
	}

	@Test
	public void testGetWorseCasePropagationTimeInMs() 
	{
		assertEquals (d13.getWorstCasePropagationTimeInMs() , 200000 , 0.0);
		assertEquals (d12.getWorstCasePropagationTimeInMs() , 100000 , 0.0);
		r12.remove();
		assertEquals (d12.getWorstCasePropagationTimeInMs() , 0 , 0.0);
	}

	@Test
	public void testIsTraversingOversubscribedLinks() 
	{
		assertTrue (d12.isTraversingOversubscribedLinks());
		assertTrue (d13.isTraversingOversubscribedLinks());
		link12.setCapacity(500);
		assertTrue (!d12.isTraversingOversubscribedLinks());
		assertTrue (d13.isTraversingOversubscribedLinks());
		link23.setCapacity(500);
		assertTrue (!d12.isTraversingOversubscribedLinks());
		assertTrue (!d13.isTraversingOversubscribedLinks());
	}

	@Test
	public void testIsTraversingOversubscribedResources() 
	{
		assertTrue (!d12.isTraversingOversubscribedResources());
		assertTrue (!scd123.isTraversingOversubscribedResources());
		sc123.setCarriedTraffic(10 , Arrays.asList(10.0 , 1000.0 , 10.0));
		assertTrue (scd123.isTraversingOversubscribedResources());
	}

	@Test
	public void testGetServiceChainSequenceOfTraversedResourceTypes() 
	{
		assertEquals(d12.getServiceChainSequenceOfTraversedResourceTypes() , Collections.emptyList());
		assertEquals(scd123.getServiceChainSequenceOfTraversedResourceTypes() , Collections.singletonList("type"));
	}

	@Test
	public void testSetServiceChainSequenceOfTraversedResourceTypes() 
	{
		try { d12.setServiceChainSequenceOfTraversedResourceTypes(Collections.singletonList("rrr")); fail ("Should fail"); } catch (Exception e) {}
		r12.remove();
		d12.setServiceChainSequenceOfTraversedResourceTypes(Collections.singletonList("rrr")); 
		assertEquals(d12.getServiceChainSequenceOfTraversedResourceTypes() , Collections.singletonList("rrr"));
	}

	@Test
	public void testGetLayer() 
	{
		assertEquals(d12.getLayer() , np.getNetworkLayerDefault());
		assertEquals(scd123.getLayer() , np.getNetworkLayerDefault());
	}

	@Test
	public void testIsBifurcated() 
	{
		assertTrue (!scd123.isBifurcated());
		assertTrue (d13.isBifurcated());
		assertTrue (!d12.isBifurcated());
	}

	@Test
	public void testIsBlocked() 
	{
		assertTrue (d12.isBlocked());
		assertTrue (d13.isBlocked());
		assertTrue (!scd123.isBlocked());
	}

	@Test
	public void testIsServiceChainRequest() 
	{
		assertTrue (!d12.isServiceChainRequest());
		assertTrue (!d13.isServiceChainRequest());
		assertTrue (scd123.isServiceChainRequest());
	}

	@Test
	public void testIsCoupled() 
	{
		assertTrue (!scd123.isCoupled());
		assertTrue (d12.isCoupled());
		assertTrue (!d13.isCoupled());
	}

	@Test
	public void testGetOfferedTraffic() 
	{
		assertEquals(d12.getOfferedTraffic() , 3 , 0.0);
		assertEquals(scd123.getOfferedTraffic() , 3 , 0.0);
	}

	@Test
	public void testGetForwardingRules() 
	{
		scd123.remove();
		np.setRoutingType(RoutingType.HOP_BY_HOP_ROUTING , lowerLayer);
		Map<Pair<Demand,Link>,Double> frs = new HashMap<Pair<Demand,Link>,Double> ();
		frs.put(Pair.of(d12 , link12) , 1.0);
		assertEquals (d12.getForwardingRules() , frs);
		frs = new HashMap<Pair<Demand,Link>,Double> ();
		frs.put(Pair.of(d13 , link12) , 1.0);
		frs.put(Pair.of(d13 , link23) , 1.0);
		assertEquals (d13.getForwardingRules() , frs);
	}

	@Test
	public void testGetCarriedTraffic() 
	{
		assertEquals (d12.getCarriedTraffic() , 1 , 0.0);
		assertEquals (d13.getCarriedTraffic() , 2 , 0.0);
		assertEquals (scd123.getCarriedTraffic() , 100 , 0.0);
	}

	@Test
	public void testGetBlockedTraffic() 
	{
		assertEquals (d12.getBlockedTraffic() , 2 , 0.0);
		assertEquals (d13.getBlockedTraffic() , 1 , 0.0);
		assertEquals (scd123.getBlockedTraffic() , 0, 0.0);
	}

	@Test
	public void testGetRoutingCycleType() 
	{
		assertEquals (d12.getRoutingCycleType() , RoutingCycleType.LOOPLESS);
		assertEquals (d13.getRoutingCycleType() , RoutingCycleType.LOOPLESS);
		assertEquals (scd123.getRoutingCycleType() , RoutingCycleType.LOOPLESS);
		scd123.remove();
		np.setRoutingType(RoutingType.HOP_BY_HOP_ROUTING , lowerLayer);
		assertEquals (d12.getRoutingCycleType() , RoutingCycleType.LOOPLESS);
		assertEquals (d13.getRoutingCycleType() , RoutingCycleType.LOOPLESS);
		np.setRoutingType(RoutingType.SOURCE_ROUTING , lowerLayer);
		Link link21 = np.addLink(n2,n1,100,100,1,null,lowerLayer);
		List<Link> path1213 = new LinkedList<Link> (); path1213.add(link12); path1213.add(link21); path1213.add(link13); 
		Route r1213 = np.addRoute(d13,1,1.5,path1213,null);
		assertEquals (d13.getRoutingCycleType() , RoutingCycleType.OPEN_CYCLES);
		np.checkCachesConsistency();
		np.setRoutingType(RoutingType.HOP_BY_HOP_ROUTING , lowerLayer);
		np.checkCachesConsistency();
		assertEquals (d13.getRoutingCycleType() , RoutingCycleType.OPEN_CYCLES);
		np.checkCachesConsistency();
		d12.removeAllForwardingRules();
		np.checkCachesConsistency();
		np.setForwardingRule(d12, link12 , 1);
		np.checkCachesConsistency();
		try { np.setForwardingRule(d12, link21 , 1); fail ("An exception should be here"); } catch (ClosedCycleRoutingException e) {}
		np.checkCachesConsistency();
	}

	@Test
	public void testGetIngressNode() 
	{
		assertEquals (d12.getIngressNode() , n1);
		assertEquals (d13.getIngressNode() , n1);
		assertEquals (scd123.getIngressNode() , n1);
	}

	@Test
	public void testGetEgressNode() 
	{
		assertEquals (d12.getEgressNode() , n2);
		assertEquals (d13.getEgressNode() , n3);
		assertEquals (scd123.getEgressNode() , n3);
	}

	@Test
	public void testGetBidirectionalPair() 
	{
		assertEquals (d12.getBidirectionalPair() , null);
		Pair<Demand,Demand> pair = np.addDemandBidirectional(n1,n2,3,null,lowerLayer);
		assertEquals (pair.getFirst().getBidirectionalPair() , pair.getSecond());
		assertEquals (pair.getSecond().getBidirectionalPair() , pair.getFirst());
	}

	@Test
	public void testGetCoupledLink() 
	{
		assertEquals (d12.getCoupledLink() , upperLink12);
		upperLink12.remove();
		assertEquals (d12.getCoupledLink() , null);
		assertEquals (d13.getCoupledLink() , null);
	}

	@Test
	public void testCoupleToNewLinkCreated() 
	{
		try { d12.coupleToNewLinkCreated(upperLayer); fail ("Should not be here"); } catch (Exception e) {}
		try { d13.coupleToNewLinkCreated(lowerLayer); fail ("Should not be here"); } catch (Exception e) {}
		Link link13new = d13.coupleToNewLinkCreated(upperLayer);
		assertEquals (d13.getCoupledLink() , link13new);
		link13new.remove();
		assertEquals (d13.getCoupledLink() , null);
	}

	@Test
	public void testDecouple() 
	{
		d12.decouple();
		assertEquals (d12.getCoupledLink() , null);
		Link link13new = d13.coupleToNewLinkCreated(upperLayer);
		assertEquals (d13.getCoupledLink() , link13new);
		d13.decouple();
		assertEquals (d13.getCoupledLink() , null);
	}

	@Test
	public void testRemoveAllForwardingRules() 
	{
		try { d12.removeAllForwardingRules(); fail ("Bad"); } catch (Exception e) {}
		scd123.remove();
		np.setRoutingType(RoutingType.HOP_BY_HOP_ROUTING , lowerLayer);
		d12.removeAllForwardingRules();
		assertEquals (d12.getCarriedTraffic() , 0 , 0.0);
		assertEquals (d12.getForwardingRules() , Collections.emptyMap());
	}

	@Test
	public void testComputeShortestPathRoutes() 
	{
		Pair<Set<Route>,Double> sps = d12.computeShortestPathRoutes(null);
		assertEquals (sps.getFirst() , Collections.singleton(r12));
		assertEquals (sps.getSecond() , 1.0 , 0.0);
		sps = d13.computeShortestPathRoutes(null);
		assertEquals (sps.getFirst() , Collections.singleton(segm13));
		assertEquals (sps.getSecond() , 1 , 0.0);
		segm13.remove();
		sps = d13.computeShortestPathRoutes(null);
		assertEquals (sps.getFirst() , new HashSet<Route> (Arrays.asList(r123a , r123b)));
		assertEquals (sps.getSecond() , 2 , 0.0);
	}

	@Test
	public void testRemove() 
	{
		d12.remove();
		scd123.remove();
	}

	@Test
	public void testSetOfferedTraffic() 
	{
		d12.setOfferedTraffic(101);
		assertEquals (d12.getOfferedTraffic() , 101 , 0.0);
		scd123.setOfferedTraffic(101);
		assertEquals (scd123.getOfferedTraffic() , 101 , 0.0);
	}

	@Test
	public void testComputeRoutingFundamentalMatrixDemand() 
	{
		scd123.remove();
		np.setRoutingType(RoutingType.HOP_BY_HOP_ROUTING , lowerLayer);
		np.checkCachesConsistency();
		np.setRoutingType(RoutingType.SOURCE_ROUTING , lowerLayer);
		np.checkCachesConsistency();
		assertEquals(d12.getRoutes().size() , 1);
		assertEquals(d12.getRoutes().iterator().next().getSeqLinks() , Collections.singletonList(link12));
	}

}
