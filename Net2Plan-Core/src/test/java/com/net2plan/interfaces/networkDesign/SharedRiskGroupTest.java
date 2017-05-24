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

public class SharedRiskGroupTest 
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
	private Resource res2;
	private Route segm13;
	private NetworkLayer lowerLayer , upperLayer;
	private Link upperLink12;
	private Link upperMdLink12 , upperMdLink13;
	private MulticastDemand upperMd123;
	private MulticastTree upperMt123;
	private SharedRiskGroup srgN1L23 , srgL13;

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
		this.d13 = np.addDemand(n1 , n3 , 3 , null,lowerLayer);
		this.d12 = np.addDemand(n1, n2, 3 , null,lowerLayer);
		this.r12 = np.addRoute(d12,1,1.5,Collections.singletonList(link12),null);
		this.path13 = new LinkedList<Link> (); path13.add(link12); path13.add(link23);
		this.r123a = np.addRoute(d13,1,1.5,path13,null);
		this.r123b = np.addRoute(d13,1,1.5,path13,null);
		this.res2 = np.addResource("type" , "name" , n2 , 100 , "Mbps" , null , 10 , null);
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
		
		this.srgL13 = np.addSRG(1,2,null); srgL13.addLink(link13);
		this.srgN1L23 = np.addSRG(1,2,null); srgN1L23.addLink(link23); srgN1L23.addNode(n1);
	}

	@After
	public void tearDown() throws Exception 
	{
		np.checkCachesConsistency();
	}

	@Test
	public void testGetNodes() 
	{
		assertEquals(srgL13.getNodes() , new HashSet<Node> (Arrays.asList()));
		assertEquals(srgN1L23.getNodes() , new HashSet<Node> (Arrays.asList(n1)));
		srgN1L23.addNode(n1);
		assertEquals(srgN1L23.getNodes() , new HashSet<Node> (Arrays.asList(n1)));
		srgN1L23.addNode(n2);
		assertEquals(srgN1L23.getNodes() , new HashSet<Node> (Arrays.asList(n1,n2)));
	}

	@Test
	public void testGetAffectedLinks() 
	{
		assertEquals(srgL13.getAffectedLinksAllLayers() , new HashSet<Link> (Arrays.asList(link13)));
		assertEquals(srgN1L23.getAffectedLinksAllLayers() , new HashSet<Link> (Arrays.asList(link12 , link13 , link23 , upperLink12 , upperMdLink12 , upperMdLink13)));
	}

	@Test
	public void testGetAffectedLinksNetworkLayer() 
	{
		assertEquals(srgL13.getAffectedLinks(lowerLayer) , new HashSet<Link> (Arrays.asList(link13)));
		assertEquals(srgN1L23.getAffectedLinks(lowerLayer) , new HashSet<Link> (Arrays.asList(link12 , link13 , link23)));
		assertEquals(srgL13.getAffectedLinks(upperLayer) , new HashSet<Link> (Arrays.asList()));
		assertEquals(srgN1L23.getAffectedLinks(upperLayer) , new HashSet<Link> (Arrays.asList(upperLink12 , upperMdLink12 , upperMdLink13)));
	}

	@Test
	public void testGetAffectedRoutesAllLayers() 
	{
		assertEquals(srgL13.getAffectedRoutesAllLayers() , new HashSet<Route> (Arrays.asList(segm13)));
		assertEquals(srgN1L23.getAffectedRoutesAllLayers() , new HashSet<Route> (Arrays.asList(r12 , r123a , r123b , segm13 , sc123)));
	}

	@Test
	public void testGetAffectedRoutes() 
	{
		assertEquals(srgL13.getAffectedRoutes(lowerLayer) , new HashSet<Route> (Arrays.asList(segm13)));
		assertEquals(srgN1L23.getAffectedRoutes(lowerLayer) , new HashSet<Route> (Arrays.asList(r12 , r123a, r123b, sc123 , segm13)));
		assertEquals(srgL13.getAffectedRoutes(upperLayer) , new HashSet<Route> (Arrays.asList()));
		assertEquals(srgN1L23.getAffectedRoutes(upperLayer) , new HashSet<Route> (Arrays.asList()));
	}

	@Test
	public void testGetAffectedMulticastTreesAllLayers() 
	{
		assertEquals(srgL13.getAffectedMulticastTreesAllLayers() , new HashSet<MulticastTree> (Arrays.asList(tStar)));
		assertEquals(srgN1L23.getAffectedMulticastTreesAllLayers() , new HashSet<MulticastTree> (Arrays.asList(tStar , t123 , upperMt123)));
	}

	@Test
	public void testGetAffectedMulticastTrees() 
	{
		assertEquals(srgL13.getAffectedMulticastTrees(lowerLayer) , new HashSet<MulticastTree> (Arrays.asList(tStar)));
		assertEquals(srgN1L23.getAffectedMulticastTrees(lowerLayer) , new HashSet<MulticastTree> (Arrays.asList(tStar , t123)));
		assertEquals(srgL13.getAffectedMulticastTrees(upperLayer) , new HashSet<MulticastTree> (Arrays.asList()));
		assertEquals(srgN1L23.getAffectedMulticastTrees(upperLayer) , new HashSet<MulticastTree> (Arrays.asList(upperMt123)));
	}

	@Test
	public void testAffectsAnyOf() 
	{
		assertEquals(srgL13.affectsAnyOf(Arrays.asList(n1,link23)) , false);
		assertEquals(srgN1L23.affectsAnyOf(Arrays.asList(n2,link23)) , true);
		assertEquals(srgN1L23.affectsAnyOf(Arrays.asList(n2,link13)) , true);
		assertEquals(srgN1L23.affectsAnyOf(Arrays.asList(n2)) , false);
	}

	@Test
	public void testGetLinksAllLayers() 
	{
		assertEquals(srgL13.getLinksAllLayers() , new HashSet<Link> (Arrays.asList(link13)));
		assertEquals(srgN1L23.getLinksAllLayers() , new HashSet<Link> (Arrays.asList(link23)));
		srgL13.addLink(upperLink12);
		assertEquals(srgL13.getLinksAllLayers() , new HashSet<Link> (Arrays.asList(link13 , upperLink12)));
	}

	@Test
	public void testGetLinksNetworkLayer() 
	{
		assertEquals(srgL13.getLinks(lowerLayer) , new HashSet<Link> (Arrays.asList(link13)));
		assertEquals(srgN1L23.getLinks(lowerLayer) , new HashSet<Link> (Arrays.asList(link23)));
		assertEquals(srgL13.getLinks(upperLayer) , new HashSet<Link> (Arrays.asList()));
		assertEquals(srgN1L23.getLinks(upperLayer) , new HashSet<Link> (Arrays.asList()));
		srgL13.addLink(upperLink12);
		assertEquals(srgL13.getLinks(lowerLayer) , new HashSet<Link> (Arrays.asList(link13)));
		assertEquals(srgN1L23.getLinks(lowerLayer) , new HashSet<Link> (Arrays.asList(link23)));
		assertEquals(srgL13.getLinks(upperLayer) , new HashSet<Link> (Arrays.asList(upperLink12)));
		assertEquals(srgN1L23.getLinks(upperLayer) , new HashSet<Link> (Arrays.asList()));
	}

	@Test
	public void testGetMeanTimeToFailInHours() 
	{
		assertEquals(srgL13.getMeanTimeToFailInHours() , 1 , 0);
		assertEquals(srgN1L23.getMeanTimeToFailInHours() , 1 , 0);
	}

	@Test
	public void testSetMeanTimeToFailInHours() 
	{
		srgL13.setMeanTimeToFailInHours(100);
		assertEquals(srgL13.getMeanTimeToFailInHours() , 100 , 0);
	}

	@Test
	public void testGetMeanTimeToRepairInHours() 
	{
		assertEquals(srgL13.getMeanTimeToRepairInHours() , 2 , 0);
		assertEquals(srgN1L23.getMeanTimeToRepairInHours() , 2 , 0);
	}

	@Test
	public void testSetMeanTimeToRepairInHours() 
	{
		srgL13.setMeanTimeToRepairInHours(100);
		assertEquals(srgL13.getMeanTimeToRepairInHours() , 100 , 0);
	}

	@Test
	public void testGetAvailability() 
	{
		assertEquals(srgL13.getAvailability() , 1.0/3.0 , 0.0001);
	}

	@Test
	public void testSetAsDown() 
	{
		srgL13.setAsDown();
		assertTrue(link13.isDown());
		assertTrue(!link12.isDown());
		assertTrue(!link23.isDown());
		assertTrue(!n1.isDown());
		assertTrue(!n2.isDown());
		assertTrue(!n3.isDown());
		link13.setFailureState(true);
		srgN1L23.setAsDown();
		assertTrue(!link13.isDown());
		assertTrue(!link12.isDown());
		assertTrue(link23.isDown());
		assertTrue(n1.isDown());
		assertTrue(!n2.isDown());
		assertTrue(!n3.isDown());
	}

	@Test
	public void testSetAsUp() 
	{
		link13.setFailureState(true);
		srgL13.setAsUp();
		assertTrue(!link13.isDown());
	}

	@Test
	public void testRemoveLink() 
	{
		srgL13.removeLink(link12);
		assertEquals(srgL13.getLinksAllLayers() , new HashSet<Link> (Arrays.asList(link13)));
		srgL13.removeLink(link13);
		assertEquals(srgL13.getLinksAllLayers() , new HashSet<Link> (Arrays.asList()));
	}

	@Test
	public void testRemoveNode() 
	{
		srgL13.removeNode(n1);
		assertEquals(srgL13.getNodes() , new HashSet<Node> (Arrays.asList()));
		assertEquals(srgN1L23.getNodes() , new HashSet<Node> (Arrays.asList(n1)));
		srgN1L23.removeNode(n1);
		assertEquals(srgN1L23.getNodes() , new HashSet<Node> (Arrays.asList()));
	}

	@Test
	public void testRemove() 
	{
		assertEquals(np.getSRGs() , Arrays.asList(srgL13 , srgN1L23));
		srgL13.remove();
		assertEquals(np.getSRGs() , Arrays.asList(srgN1L23));
	}

	@Test
	public void testAddLink() 
	{
		srgL13.addLink(link13);
		assertEquals(srgL13.getLinksAllLayers() , new HashSet<Link> (Arrays.asList(link13)));
		srgL13.addLink(link23);
		assertEquals(srgL13.getLinksAllLayers() , new HashSet<Link> (Arrays.asList(link13 , link23)));
	}

	@Test
	public void testAddNode() 
	{
		assertEquals(srgN1L23.getNodes() , new HashSet<Node> (Arrays.asList(n1)));
		srgN1L23.addNode(n1);
		assertEquals(srgN1L23.getNodes() , new HashSet<Node> (Arrays.asList(n1)));
		srgN1L23.addNode(n2);
		assertEquals(srgN1L23.getNodes() , new HashSet<Node> (Arrays.asList(n1 , n2)));
	}

}
