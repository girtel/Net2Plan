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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.net2plan.utils.Pair;


public class MulticastDemandTest 
{
	private NetPlan np = null;
	private Node n1, n2 , n3;
	private Link link12, link23 , link13;
	private Demand scd123;
	private MulticastDemand d123;
	private MulticastTree tStar, t123;
	private Set<Link> star, line123;
	private Set<Node> endNodes;
	private Route sc123;
	private List<Link> path13;
	private List<NetworkElement> pathSc123;
	private Resource res2;
	private Route segm13;
	private NetworkLayer lowerLayer , upperLayer;
	private Link upperLink12;
	private Link upperMdLink12 , upperMdLink13;
	private MulticastDemand upperMd123;
	private MulticastTree upperMt123;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
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
		this.path13 = new LinkedList<Link> (); path13.add(link12); path13.add(link23);
		this.res2 = np.addResource("type" , "name" , n2 , 100 , "Mbps" , null , 10 , null);
		this.scd123 = np.addDemand(n1 , n3 , 3 , null,lowerLayer);
		this.scd123.setServiceChainSequenceOfTraversedResourceTypes(Collections.singletonList("type"));
		this.pathSc123 = Arrays.asList(link12 ,res2 , link23); 
		this.sc123 = np.addServiceChain(scd123 , 100 , Arrays.asList(300.0 , 50.0 , 302.0) , pathSc123 , null); 
		this.upperLink12 = np.addLink(n1,n2,10,100,1,null,upperLayer);
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
	}

	@After
	public void tearDown() throws Exception 
	{
		np.checkCachesConsistency();
	}

	@Test
	public void testGetLayer() 
	{
		assertEquals(d123.getLayer() , lowerLayer);
		assertEquals(upperMd123.getLayer() , upperLayer);
	}

	@Test
	public void testGetWorseCasePropagationTimeInMs() 
	{
		assertEquals(d123.getWorseCasePropagationTimeInMs() , 200000 , 0);
		assertEquals(upperMd123.getWorseCasePropagationTimeInMs() , 200000 , 0);
	}

	@Test
	public void testIsTraversingOversubscribedLinks() 
	{
		assertTrue (d123.isTraversingOversubscribedLinks());
		sc123.setCarriedTraffic(0 , 1);
		assertTrue (!d123.isTraversingOversubscribedLinks());
		assertTrue (!upperMd123.isTraversingOversubscribedLinks());
		t123.setCarriedTraffic(1,1);
		assertTrue (upperMd123.isTraversingOversubscribedLinks());
	}

	@Test
	public void testGetIngressNode() 
	{
		assertEquals(d123.getIngressNode() , n1);
		assertEquals(upperMd123.getIngressNode() , n1);
		n1.setFailureState(false);
		assertEquals(d123.getIngressNode() , n1);
		assertEquals(upperMd123.getIngressNode() , n1);
	}

	@Test
	public void testGetEgressNodes() 
	{
		assertEquals(d123.getEgressNodes() , new HashSet<Node> (Arrays.asList(n2,n3)));
		assertEquals(upperMd123.getEgressNodes() , new HashSet<Node> (Arrays.asList(n2,n3)));
		n1.setFailureState(false);
		assertEquals(d123.getEgressNodes() , new HashSet<Node> (Arrays.asList(n2,n3)));
		assertEquals(upperMd123.getEgressNodes() , new HashSet<Node> (Arrays.asList(n2,n3)));
	}

	@Test
	public void testGetOfferedTraffic() 
	{
		assertEquals(d123.getOfferedTraffic() , 100.0 , 0);
		assertEquals(upperMd123.getOfferedTraffic() , 100.0 , 0);
		n1.setFailureState(false);
		assertEquals(d123.getOfferedTraffic() , 100.0 , 0);
		assertEquals(upperMd123.getOfferedTraffic() , 100.0 , 0);
	}

	@Test
	public void testGetCarriedTraffic() 
	{
		assertEquals(d123.getCarriedTraffic() , 20 , 0);
		assertEquals(upperMd123.getCarriedTraffic() , 10 , 0);
		n1.setFailureState(false);
		assertEquals(d123.getCarriedTraffic() , 0 , 0);
		assertEquals(upperMd123.getCarriedTraffic() , 0 , 0);
		n1.setFailureState(true);
		assertEquals(d123.getCarriedTraffic() , upperMdLink12.getCapacity() , 0);
		assertEquals(d123.getCarriedTraffic() , upperMdLink13.getCapacity() , 0);
	}

	@Test
	public void testGetBlockedTraffic() 
	{
		assertEquals(d123.getBlockedTraffic() , 100-20 , 0);
		assertEquals(upperMd123.getBlockedTraffic() , 100-10 , 0);
		n1.setFailureState(false);
		assertEquals(d123.getBlockedTraffic() , 100 , 0);
		assertEquals(upperMd123.getBlockedTraffic() , 100 , 0);
	}

	@Test
	public void testComputeMinimumCostMulticastTrees() 
	{
		Set<MulticastTree> twoTrees = new HashSet<MulticastTree> (Arrays.asList(tStar , t123));
		assertEquals (d123.computeMinimumCostMulticastTrees(null) , Pair.of(twoTrees , 2.0));
		assertEquals (d123.computeMinimumCostMulticastTrees(new double [] { 1.0,2.0,1.0}) , Pair.of(new HashSet<MulticastTree> (Arrays.asList(tStar)) , 2.0));
	}

	@Test
	public void testIsBifurcated() 
	{
		assertTrue(d123.isBifurcated());
		assertTrue(!upperMd123.isBifurcated());
	}

	@Test
	public void testIsBlocked() 
	{
		assertTrue(d123.isBlocked());
		assertTrue(upperMd123.isBlocked());
		t123.setCarriedTraffic(d123.getOfferedTraffic() , 10);
		assertTrue(!d123.isBlocked());
		assertTrue(upperMd123.isBlocked());
	}

	@Test
	public void testGetMulticastTrees() 
	{
		assertEquals(d123.getMulticastTrees() , new HashSet<MulticastTree> (Arrays.asList(t123 , tStar)));
		assertEquals(upperMd123.getMulticastTrees() , new HashSet<MulticastTree>(Arrays.asList(upperMt123)));
	}

	@Test
	public void testCouple() 
	{
		assertTrue (d123.isCoupled());
		assertEquals(d123.getCoupledLinks() , new HashSet<Link> (Arrays.asList(upperMdLink12 , upperMdLink13)));
	}

	@Test
	public void testCoupleToNewLinksCreated() 
	{
		NetworkLayer upperUpperLayer = np.addLayer("" , "" , "upperTrafficUnits" , "Mbps" , null , null);
		Set<Link> uuLinks = upperMd123.coupleToNewLinksCreated(upperUpperLayer);
		assertEquals(uuLinks.size() , 2);
		for (Link e : uuLinks)
		{
			assertEquals(e.isCoupled() , true);
			assertEquals(e.getCapacity() , upperMd123.getCarriedTraffic() , 0);
			assertEquals(e.getCoupledMulticastDemand() , upperMd123);
		}
	}

	@Test
	public void testGetCoupledLinks() 
	{
		assertEquals(d123.getCoupledLinks() , new HashSet<Link> (Arrays.asList(upperMdLink12 , upperMdLink13)));
		assertEquals(upperMd123.getCoupledLinks() , new HashSet<Link> ());
	}

	@Test
	public void testIsCoupled() 
	{
		assertTrue(d123.isCoupled());
		assertTrue(!upperMd123.isCoupled());
	}

	@Test
	public void testDecouple() 
	{
		d123.decouple();
		assertTrue(!d123.isCoupled());
		assertTrue(!upperMd123.isCoupled());
	}

	@Test
	public void testRemove() 
	{
		d123.remove();
		assertEquals(np.getMulticastDemands(lowerLayer) , Arrays.asList());
		assertEquals(np.getMulticastDemands(upperLayer) , Arrays.asList(upperMd123));
		upperMd123.remove();
		assertEquals(np.getMulticastDemands(lowerLayer) , Arrays.asList());
		assertEquals(np.getMulticastDemands(upperLayer) , Arrays.asList());
	}

	@Test
	public void testSetOfferedTraffic() 
	{
		d123.setOfferedTraffic(23);
		assertEquals(d123.getOfferedTraffic() , 23 , 0);
		upperMd123.setOfferedTraffic(22);
		assertEquals(upperMd123.getOfferedTraffic() , 22 , 0);
		n1.setFailureState(false);
		assertEquals(d123.getOfferedTraffic() , 23 , 0);
		assertEquals(upperMd123.getOfferedTraffic() , 22 , 0);
	}

}
