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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.net2plan.utils.Pair;
import com.net2plan.utils.Triple;
import com.net2plan.utils.Constants.RoutingType;
import org.junit.rules.TemporaryFolder;

public class LinkTest 
{
	private NetPlan np = null;
	private Node n1, n2 , n3;
	private Link link12, link23 , link13;
	private Demand d13, d12 , scd123;
	private MulticastDemand d123;
	private MulticastTree tStar, t123;
	private Set<Link> star, line123;
	private Set<Node> endNodes;
	private Route r12, r123a, r123b , sc123;
	private List<Link> path13;
	private List<NetworkElement> pathSc123;
	private Resource res2 , res2backup;
	private Route segm13;
	private NetworkLayer lowerLayer , upperLayer;
	private Link upperLink12;
	private Link upperMdLink12 , upperMdLink13;
	private MulticastDemand upperMd123;
	private MulticastTree upperMt123;

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();
	

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
		this.line123 = new HashSet<Link> (Arrays.asList(link12, link23)); 
		this.star = new HashSet<Link> (Arrays.asList(link12, link13));
		this.endNodes = new HashSet<Node> (Arrays.asList(n2,n3));
		this.d123 = np.addMulticastDemand(n1 , endNodes , 100 , null , lowerLayer);
		this.t123 = np.addMulticastTree(d123 , 10,15,line123,null);
		this.tStar = np.addMulticastTree(d123 , 10,15,star,null);
		this.upperMdLink12 = np.addLink(n1,n2,10,100,1,null,upperLayer);
		this.upperMdLink13 = np.addLink(n1,n3,10,100,1,null,upperLayer);
		this.upperMd123 = np.addMulticastDemand (n1 , endNodes , 100 , null , upperLayer);
		this.upperMt123 = np.addMulticastTree (upperMd123 , 10 , 15 , new HashSet<Link> (Arrays.asList(upperMdLink12 , upperMdLink13)) , null);
		d123.couple(new HashSet<Link> (Arrays.asList(upperMdLink12 , upperMdLink13)));

		temporaryFolder.create();
	}

	@After
	public void tearDown() throws Exception 
	{
		np.checkCachesConsistency();

		temporaryFolder.delete();
	}

	@Test
	public void testCheckCaches() 
	{
		np.checkCachesConsistency();
	}

	@Test
	public void testBidirectional () throws IOException
	{
		Pair<Link,Link> pair = np.addLinkBidirectional(n1, n2, 1, 1, 1, null);
		assertTrue (pair.getFirst().isBidirectional());
		assertTrue (pair.getSecond().isBidirectional());
		assertEquals (pair.getFirst().getBidirectionalPair() , pair.getSecond());
		assertEquals (pair.getSecond().getBidirectionalPair() , pair.getFirst());
		pair.getFirst().remove();
		assertTrue (!pair.getSecond().isBidirectional());
		assertEquals (pair.getSecond().getBidirectionalPair() , null);
		
		Link other = np.addLink(n1, n3, 1, 1 , 1 , null);
		try { other.setBidirectionalPair(pair.getFirst()); fail (); } catch (Exception e) {}
		
		Link other2 = np.addLink(n1, n2, 1, 1,1,null);
		pair.getSecond().setBidirectionalPair(other2);
		assertTrue (pair.getSecond().isBidirectional());
		assertEquals (other2.getBidirectionalPair() , pair.getSecond());
		assertEquals (pair.getSecond().getBidirectionalPair() , other2);
		
		File f = temporaryFolder.newFile("temp.n2p");
		this.np.saveToFile(f);
		NetPlan readNp = new NetPlan (f);
		assertTrue(readNp.isDeepCopy(np));
		assertTrue(np.isDeepCopy(readNp));
	}

	@Test
	public void testGetOriginNode() 
	{
		assertEquals(upperLink12.getOriginNode(), n1);
		assertEquals(link12.getOriginNode(), n1);
		assertEquals(link23.getOriginNode(), n2);
		link12.setFailureState(false);
		assertEquals(upperLink12.getOriginNode(), n1);
		assertEquals(link12.getOriginNode(), n1);
		assertEquals(link23.getOriginNode(), n2);
	}

	@Test
	public void testGetDestinationNode() 
	{
		assertEquals(upperLink12.getDestinationNode(), n2);
		assertEquals(link12.getDestinationNode(), n2);
		assertEquals(link23.getDestinationNode(), n3);
		link12.setFailureState(false);
		assertEquals(upperLink12.getDestinationNode(), n2);
		assertEquals(link12.getDestinationNode(), n2);
		assertEquals(link23.getDestinationNode(), n3);
	}

	@Test
	public void testGetLayer() 
	{
		assertEquals(upperLink12.getLayer() , upperLayer);
		assertEquals(link12.getLayer() , lowerLayer);
		assertEquals(link23.getLayer() , lowerLayer);
		assertEquals(link13.getLayer() , lowerLayer);
	}

	@Test
	public void testGetCoupledDemand() 
	{
		assertEquals(upperLink12.getCoupledDemand() , d12);
		assertEquals(link12.getCoupledDemand() , null);
		assertEquals(link13.getCoupledDemand() , null);
	}

	@Test
	public void testGetCoupledMulticastDemand() 
	{
		assertEquals(upperMdLink12.getCoupledMulticastDemand(), d123);
		assertEquals(upperMdLink13.getCoupledMulticastDemand(), d123);
		assertEquals(upperLink12.getCoupledMulticastDemand(), null);
		assertEquals(link12.getCoupledMulticastDemand(), null);
		assertEquals(link13.getCoupledMulticastDemand(), null);
	}

	@Test
	public void testIsCoupled() 
	{
		assertTrue(upperMdLink12.isCoupled());
		assertTrue(upperMdLink13.isCoupled());
		assertTrue(upperLink12.isCoupled());
		assertTrue(!link12.isCoupled());
		assertTrue(!link13.isCoupled());
	}

	@Test
	public void testGetForwardingRules() 
	{
		try { np.setRoutingType(RoutingType.HOP_BY_HOP_ROUTING , lowerLayer); fail ("Not in service chains"); } catch (Exception e) {}
		scd123.remove();
		np.setRoutingType(RoutingType.HOP_BY_HOP_ROUTING , lowerLayer); 
		Map<Pair<Demand,Link>,Double> frLink12 = link12.getForwardingRules();
		assertEquals (frLink12.get(Pair.of(d12,link12)) , 1.0 , 0);
		assertEquals (frLink12.get(Pair.of(d12,link23)) , null);
		assertEquals (frLink12.get(Pair.of(d13,link12)) , 1.0 , 0);
		assertEquals (link23.getForwardingRules().get(Pair.of(d13,link23)) , 1.0 , 0);
		assertEquals (frLink12.get(Pair.of(d13,link13)) , null);
	}

	@Test
	public void testGetCapacity() 
	{
		assertEquals(link12.getCapacity() , 100 , 0);
		assertEquals(link13.getCapacity() , 100 , 0);
		assertEquals(link23.getCapacity() , 100 , 0);
		assertEquals(upperLink12.getCapacity() , 1 , 0);
		assertEquals(upperMdLink12.getCapacity() , 20 , 0);
		assertEquals(upperMdLink12.getCapacity() , 20 , 0);
	}

	@Test
	public void testSetCapacity() 
	{
		assertEquals(link12.getCapacity() , 100 , 0);
		link12.setCapacity(150);
		assertEquals(link12.getCapacity() , 150 , 0);
		try { upperLink12.setCapacity(5); fail (); } catch (Exception e) {}
		try { upperMdLink12.setCapacity(5); fail (); } catch (Exception e) {}
		try { upperMdLink13.setCapacity(5); fail (); } catch (Exception e) {}
		link13.setCapacity(0);
		assertEquals(link13.getCapacity() , 0 , 0);
		try { link12.setCapacity(-1); fail (); } catch (Exception e) {}
	}

	@Test
	public void testGetCarriedTraffic() 
	{
		assertEquals(link12.getCarriedTraffic() , 123 , 0);
		assertEquals(link13.getCarriedTraffic() , 10 , 0); 
		assertEquals(upperLink12.getCarriedTraffic() , 0 , 0); 
		assertEquals(upperMdLink12.getCarriedTraffic() , 10 , 0); 
		assertEquals(upperMdLink13.getCarriedTraffic() , 10 , 0); 
	}

	@Test
	public void testGetUtilization() 
	{
		assertEquals(link12.getUtilization() , (1.5+1.5+1.5+300+15+15) / 100 , 0.0001);
		assertEquals(link13.getUtilization() , (50.0+15.0) / 100.0 , 0.0001);
	}

	@Test
	public void testGetOccupiedCapacity() 
	{
		assertEquals(link12.getOccupiedCapacity() , (1.5+1.5+1.5+300+15+15) , 0.0001);
		assertEquals(link13.getOccupiedCapacity() , (50+15) , 0.0001);
		assertEquals(segm13.getOccupiedCapacity() , (50) , 0.0001);
		assertEquals(upperLink12.getOccupiedCapacity() , 0 , 0.0001);
		assertEquals(upperMdLink12.getOccupiedCapacity() , 15 , 0.0001);
		assertEquals(upperMdLink13.getOccupiedCapacity() , 15 , 0.0001);
		link12.setFailureState(false);
		assertEquals(link12.getOccupiedCapacity() , 0 , 0.0001);
		assertEquals(link23.getOccupiedCapacity() , 0 , 0.0001);
	}

	@Test
	public void testGetOccupiedCapacityOnlyBackupRoutes() 
	{
		assertEquals(link12.getOccupiedCapacityOnlyBackupRoutes() , 0 , 0.0001);
		assertEquals(link13.getOccupiedCapacityOnlyBackupRoutes() , 50 , 0.0001);
	}

	@Test
	public void testGetLengthInKm() 
	{
		assertEquals(link13.getLengthInKm() , 100 , 0);
		assertEquals(upperLink12.getLengthInKm() , d12.getWorstCaseLengthInKm() , 0);
		assertEquals(upperMdLink12.getLengthInKm() , d123.getWorstCaseLengthInKm() , 0);
	}

	@Test
	public void testSetLengthInKm() 
	{
		link13.setLengthInKm(200);
		try { upperLink12.setLengthInKm(200); fail (); } catch (Exception e) {}
		assertEquals(link13.getLengthInKm() , 200 , 0);
	}

	@Test
	public void testGetPropagationSpeedInKmPerSecond() 
	{
		assertEquals(link13.getPropagationSpeedInKmPerSecond() , 1 , 0);
		assertEquals(upperLink12.getPropagationSpeedInKmPerSecond() , 1 , 0);
		assertEquals(upperMdLink12.getPropagationSpeedInKmPerSecond() , 1 , 0);
	}

	@Test
	public void testGetMulticastCarriedTraffic() 
	{
		assertEquals(link12.getMulticastCarriedTraffic() , 20 , 0);
		assertEquals(link23.getMulticastCarriedTraffic() , 10 , 0);
		assertEquals(link13.getMulticastCarriedTraffic() , 10 , 0);
		assertEquals(upperMdLink12.getMulticastCarriedTraffic() , 10 , 0);
		assertEquals(upperMdLink13.getMulticastCarriedTraffic() , 10 , 0);
	}

	@Test
	public void testGetMulticastOccupiedLinkCapacity() 
	{
		assertEquals(link12.getMulticastOccupiedLinkCapacity() , 30 , 0);
		assertEquals(link23.getMulticastOccupiedLinkCapacity() , 15 , 0);
		assertEquals(link13.getMulticastOccupiedLinkCapacity() , 15 , 0);
		assertEquals(upperMdLink12.getMulticastOccupiedLinkCapacity() , 15 , 0);
		assertEquals(upperMdLink13.getMulticastOccupiedLinkCapacity() , 15 , 0);
	}

	@Test
	public void testSetPropagationSpeedInKmPerSecond() 
	{
		assertEquals(link12.getPropagationSpeedInKmPerSecond() , 1 , 0);
		assertEquals(link23.getPropagationSpeedInKmPerSecond() , 1 , 0);
		assertEquals(link13.getPropagationSpeedInKmPerSecond() , 1 , 0);
		assertEquals(upperLink12.getPropagationSpeedInKmPerSecond() , 1 , 0);
		assertEquals(upperMdLink12.getPropagationSpeedInKmPerSecond() , 1 , 0);
		assertEquals(upperMdLink13.getPropagationSpeedInKmPerSecond() , 1 , 0);
	}

	@Test
	public void testGetPropagationDelayInMs() 
	{
		assertEquals(link12.getPropagationDelayInMs() , 100000 , 0);
		assertEquals(link23.getPropagationDelayInMs() , 100000 , 0);
		assertEquals(link13.getPropagationDelayInMs() , 100000 , 0);
		assertEquals(upperLink12.getPropagationDelayInMs() , 100000 , 0);
		assertEquals(upperMdLink12.getPropagationDelayInMs() , d123.getWorseCasePropagationTimeInMs() , 0);
		assertEquals(upperMdLink13.getPropagationDelayInMs() , d123.getWorseCasePropagationTimeInMs() , 0);
	}

	@Test
	public void testIsUp() 
	{
		assertTrue(link12.isUp());
		assertTrue(!link12.isDown());
		link12.setFailureState(false);
		assertTrue(!link12.isUp());
		assertTrue(link12.isDown());
	
		assertTrue(upperLink12.isUp());
		assertTrue(!upperLink12.isDown());
		upperLink12.setFailureState(false);
		assertTrue(!upperLink12.isUp());
		assertTrue(upperLink12.isDown());
	}

	@Test
	public void testIsOversubscribed() 
	{
		assertTrue(link12.isOversubscribed());
		link12.setCapacity(100000);
		assertTrue(!link12.isOversubscribed());
		link12.setFailureState(false);
		link12.setCapacity(1);
		assertTrue(!link12.isOversubscribed());
		link12.setCapacity(0);
		assertTrue(!link12.isOversubscribed());
	}

	@Test
	public void testGetSRGs() 
	{
		SharedRiskGroup srg = np.addSRG(1,1,null);
		srg.addNode(n2);
		assertEquals(link12.getSRGs() , Collections.emptySet());
		srg.addLink(link12);
		assertEquals(link12.getSRGs() , Collections.singleton(srg));
	}

	@Test
	public void testGetTraversingRoutes() 
	{
		assertEquals(link12.getTraversingRoutes() , new HashSet<Route> (Arrays.asList(r12,r123a,r123b,sc123)));
		assertEquals(link23.getTraversingRoutes() , new HashSet<Route> (Arrays.asList(r123a,r123b,sc123)));
		assertEquals(link13.getTraversingRoutes() , new HashSet<Route> (Arrays.asList(segm13)));
		assertEquals(upperLink12.getTraversingRoutes() , new HashSet<Route> (Arrays.asList()));
		assertEquals(upperMdLink12.getTraversingRoutes() , new HashSet<Route> (Arrays.asList()));
		assertEquals(upperMdLink13.getTraversingRoutes() , new HashSet<Route> (Arrays.asList()));
	}

	@Test
	public void testGetTraversingTrees() 
	{
		assertEquals(link12.getTraversingTrees() , new HashSet<MulticastTree> (Arrays.asList(t123 , tStar)));
		assertEquals(link23.getTraversingTrees() , new HashSet<MulticastTree> (Arrays.asList(t123)));
		assertEquals(link13.getTraversingTrees() , new HashSet<MulticastTree> (Arrays.asList(tStar)));
		assertEquals(upperLink12.getTraversingTrees() , new HashSet<MulticastTree> (Arrays.asList()));
		assertEquals(upperMdLink12.getTraversingTrees() , new HashSet<MulticastTree> (Arrays.asList(upperMt123)));
		assertEquals(upperMdLink13.getTraversingTrees() , new HashSet<MulticastTree> (Arrays.asList(upperMt123)));
	}

	@Test
	public void testCoupleToLowerLayerDemand() 
	{
		final Demand dd13 = np.addDemand(n1,n3,10,null,lowerLayer);
		final Link ll13 = np.addLink(n1,n3,100,100,1,null,upperLayer);
		ll13.coupleToLowerLayerDemand(dd13);
		assertEquals(ll13.getCapacity() , 0 , 0);
		assertTrue(ll13.isCoupled());
		assertEquals(ll13.getCoupledDemand() , dd13);
		try { ll13.coupleToLowerLayerDemand(dd13); fail (); } catch (Exception e) {}
	}

	@Test
	public void testCoupleToNewDemandCreated() 
	{
		final Link ll13 = np.addLink(n1,n3,100,100,1,null,upperLayer);
		final Demand dd13 = ll13.coupleToNewDemandCreated(lowerLayer);
		assertEquals(ll13.getCapacity() , 0 , 0);
		assertTrue(ll13.isCoupled());
		assertEquals(ll13.getCoupledDemand() , dd13);
		try { ll13.coupleToLowerLayerDemand(dd13); fail (); } catch (Exception e) {}
		try { ll13.coupleToNewDemandCreated(lowerLayer); fail (); } catch (Exception e) {}
	}

	@Test
	public void testRemoveAllForwardingRules() 
	{
		try { np.setRoutingType(RoutingType.HOP_BY_HOP_ROUTING , lowerLayer); fail ("Not in service chains"); } catch (Exception e) {}
		scd123.remove();
		np.setRoutingType(RoutingType.HOP_BY_HOP_ROUTING , lowerLayer); 
		link12.removeAllForwardingRules();
		assertEquals (link12.getForwardingRules() , Collections.emptyMap());
	}

	@Test
	public void testGetBidirectionalPair() 
	{
		assertEquals(link12.getBidirectionalPair(),null);
		Pair<Link,Link> pair = np.addLinkBidirectional(n1,n2,100,100,1,null,upperLayer);
		assertEquals(pair.getFirst().getBidirectionalPair() , pair.getSecond());
		assertEquals(pair.getSecond().getBidirectionalPair() , pair.getFirst());
	}

	@Test
	public void testRemove() 
	{
		List<Link> lower = new ArrayList<Link> (np.getLinks(lowerLayer));
		List<Link> upper = new ArrayList<Link> (np.getLinks(upperLayer));

		link12.remove();
		lower.remove(link12);
		assertEquals (lower , np.getLinks(lowerLayer));
		
		upperLink12.remove();
		upper.remove(upperLink12);
		assertEquals (upper , np.getLinks(upperLayer));

		upperMdLink12.remove();
		upper.remove(upperMdLink12);
		assertEquals (upper , np.getLinks(upperLayer));
	}

	@Test
	public void testSetFailureState() 
	{
		link12.setFailureState(false);
		assertTrue (link12.isDown());
		assertTrue (!upperLink12.isDown());
		link12.setFailureState(true);
		assertTrue (!link12.isDown());
		upperLink12.setFailureState(false);
		assertTrue (upperLink12.isDown());
		upperLink12.setFailureState(true);
		assertTrue (!upperLink12.isDown());
	}
	
	@Test
	public void testGetLinksThisLayerPotentiallyCarryingTrafficTraversingThisLink  ()
	{
		Triple<Map<Demand,Set<Link>>,Map<Demand,Set<Link>>,Map<Pair<MulticastDemand,Node>,Set<Link>>> triple;
		triple = link12.getLinksThisLayerPotentiallyCarryingTrafficTraversingThisLink  ();
		assertEquals (triple.getFirst() , ImmutableMap.of(d12 , Sets.newHashSet(link12) , d13 , Sets.newHashSet(link12 , link23)
				, scd123 , Sets.newHashSet(link12 , link23)));
		assertEquals (triple.getSecond() , ImmutableMap.of());
		assertEquals (triple.getThird() , ImmutableMap.of(Pair.of(d123,n3) , Sets.newHashSet(link12 , link23) , Pair.of(d123,n2) , Sets.newHashSet(link12)  ));
		
		link23.setFailureState(false);
		triple = link12.getLinksThisLayerPotentiallyCarryingTrafficTraversingThisLink  ();
		assertEquals (triple.getFirst() , ImmutableMap.of(d12 , Sets.newHashSet(link12)));
		assertEquals (triple.getSecond() , ImmutableMap.of());
		assertEquals (triple.getThird() , ImmutableMap.of(Pair.of(d123,n2) , Sets.newHashSet(link12)  ));
		
		link23.setFailureState(true);
		triple = link13.getLinksThisLayerPotentiallyCarryingTrafficTraversingThisLink  ();
		assertEquals (triple.getFirst() , ImmutableMap.of());
		assertEquals (triple.getSecond() , ImmutableMap.of(d13 , Sets.newHashSet(link13)));
		assertEquals (triple.getThird() , ImmutableMap.of(Pair.of(d123,n3) , Sets.newHashSet(link13)));
		link23.setFailureState(false);
		triple = link13.getLinksThisLayerPotentiallyCarryingTrafficTraversingThisLink  ();
		assertEquals (triple.getFirst() , ImmutableMap.of());
		assertEquals (triple.getSecond() , ImmutableMap.of(d13 , Sets.newHashSet(link13)));
		assertEquals (triple.getThird() , ImmutableMap.of(Pair.of(d123,n3) , Sets.newHashSet(link13)));
		
	}
	

	
}
