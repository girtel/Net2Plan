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

import java.awt.geom.Point2D;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.net2plan.utils.Constants.RoutingType;
import com.google.common.collect.Sets;
import com.net2plan.utils.Pair;

public class NodeTest 
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
		n1.setSiteName("s12");
		n2.setSiteName("s12");
		n3.setSiteName("s3");
		n1.setPopulation(100);
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
	}

	@After
	public void tearDown() throws Exception 
	{
		np.checkCachesConsistency();
	}

	@Test
	public void testGetName() 
	{
		assertEquals(n1.getName() , "node1");
		assertEquals(n2.getName() , "node2");
		assertEquals(n3.getName() , "node3");
	}

	@Test
	public void testGetSiteName() 
	{
		assertEquals(n1.getSiteName() , "s12");
		assertEquals(n2.getSiteName() , "s12");
		assertEquals(n3.getSiteName() , "s3");
	}
	
	@Test
	public void testSetSiteName() 
	{
		assertEquals (np.getSiteNames() , Sets.newHashSet("s12" , "s3"));
		n3.setSiteName("s4");
		assertEquals (np.getSiteNames() , Sets.newHashSet("s12" , "s4"));
		n3.setSiteName(null);
		assertEquals (np.getSiteNames() , Sets.newHashSet("s12"));
		assertEquals(n3.getSiteName() , null);
		n1.setSiteName("s1");
		assertEquals(n1.getSiteName() , "s1");
	}

	@Test
	public void testSetUrlNodeIcon() throws Exception
	{
		n1.setUrlNodeIcon(np.getNetworkLayerDefault() , new URL ("file:/icon"));
		assertEquals(n1.getUrlNodeIcon(np.getNetworkLayerDefault()) , new URL ("file:/icon"));
		n1.removeUrlNodeIcon(np.getNetworkLayerDefault());
		assertEquals(n1.getUrlNodeIcon(np.getNetworkLayerDefault()) , null);
	}

	
	@Test
	public void testSetName() 
	{
		n1.setName("xxx");
		assertEquals(n1.getName() , "xxx");
	}

	@Test
	public void testSetPopulation() 
	{
		assertEquals(n1.getPopulation(), 100 , 0);
		n1.setPopulation(200);
		assertEquals(n1.getPopulation(), 200 , 0);
	}

	
	@Test
	public void testGetEgressOfferedTraffic() 
	{
		assertEquals(n1.getEgressOfferedTraffic(lowerLayer) , 0 , 0);
		assertEquals(n2.getEgressOfferedTraffic(lowerLayer) , 3 , 0);
		assertEquals(n3.getEgressOfferedTraffic(lowerLayer) , 6 , 0);
		assertEquals(n1.getEgressOfferedTraffic(upperLayer) , 0 , 0);
		assertEquals(n2.getEgressOfferedTraffic(upperLayer) , 0 , 0);
		assertEquals(n3.getEgressOfferedTraffic(upperLayer) , 0 , 0);
	}

	@Test
	public void testGetIngressOfferedTraffic() 
	{
		assertEquals(n1.getIngressOfferedTraffic(lowerLayer) , 9 , 0);
		assertEquals(n2.getIngressOfferedTraffic(lowerLayer) , 0 , 0);
		assertEquals(n3.getIngressOfferedTraffic(lowerLayer) , 0 , 0);
		assertEquals(n1.getIngressOfferedTraffic(upperLayer) , 0 , 0);
		assertEquals(n2.getIngressOfferedTraffic(upperLayer) , 0 , 0);
		assertEquals(n3.getIngressOfferedTraffic(upperLayer) , 0 , 0);
	}

	@Test
	public void testGetEgressOfferedMulticastTraffic() 
	{
		assertEquals(n1.getEgressOfferedMulticastTraffic(lowerLayer) , 0 , 0);
		assertEquals(n2.getEgressOfferedMulticastTraffic(lowerLayer) , 100 , 0);
		assertEquals(n3.getEgressOfferedMulticastTraffic(lowerLayer) , 100 , 0);
		assertEquals(n1.getEgressOfferedMulticastTraffic(upperLayer) , 0 , 0);
		assertEquals(n2.getEgressOfferedMulticastTraffic(upperLayer) , 100 , 0);
		assertEquals(n3.getEgressOfferedMulticastTraffic(upperLayer) , 100 , 0);
	}

	@Test
	public void testGetIngressOfferedMulticastTraffic() 
	{
		assertEquals(n1.getIngressOfferedMulticastTraffic(lowerLayer) , 100 , 0);
		assertEquals(n2.getIngressOfferedMulticastTraffic(lowerLayer) , 0 , 0);
		assertEquals(n3.getIngressOfferedMulticastTraffic(lowerLayer) , 0 , 0);
		assertEquals(n1.getIngressOfferedMulticastTraffic(upperLayer) , 100 , 0);
		assertEquals(n2.getIngressOfferedMulticastTraffic(upperLayer) , 0 , 0);
		assertEquals(n3.getIngressOfferedMulticastTraffic(upperLayer) , 0 , 0);
	}

	@Test
	public void testGetEgressCarriedTraffic() 
	{
		assertEquals(n1.getEgressCarriedTraffic(lowerLayer) , 0 , 0);
		assertEquals(n2.getEgressCarriedTraffic(lowerLayer) , 1 , 0);
		assertEquals(n3.getEgressCarriedTraffic(lowerLayer) , 1+1+100+0 , 0);
		assertEquals(n1.getEgressCarriedTraffic(upperLayer) , 0 , 0);
		assertEquals(n2.getEgressCarriedTraffic(upperLayer) , 0 , 0);
		assertEquals(n3.getEgressCarriedTraffic(upperLayer) , 0 , 0);
	}

	@Test
	public void testGetIngressCarriedTraffic() 
	{
		assertEquals(n1.getIngressCarriedTraffic(lowerLayer) , 1+1+1+100+0 , 0);
		assertEquals(n2.getIngressCarriedTraffic(lowerLayer) , 0 , 0);
		assertEquals(n3.getIngressCarriedTraffic(lowerLayer) , 0 , 0);
		assertEquals(n1.getIngressCarriedTraffic(upperLayer) , 0 , 0);
		assertEquals(n2.getIngressCarriedTraffic(upperLayer) , 0 , 0);
		assertEquals(n3.getIngressCarriedTraffic(upperLayer) , 0 , 0);
	}

	@Test
	public void testGetEgressCarriedMulticastTraffic() 
	{
		assertEquals(n1.getEgressCarriedMulticastTraffic(lowerLayer) , 0 , 0);
		assertEquals(n2.getEgressCarriedMulticastTraffic(lowerLayer) , 20 , 0);
		assertEquals(n3.getEgressCarriedMulticastTraffic(lowerLayer) , 20 , 0);
		assertEquals(n1.getEgressCarriedMulticastTraffic(upperLayer) , 0 , 0);
		assertEquals(n2.getEgressCarriedMulticastTraffic(upperLayer) , 10 , 0);
		assertEquals(n3.getEgressCarriedMulticastTraffic(upperLayer) , 10 , 0);
	}

	@Test
	public void testGetIngressCarriedMulticastTraffic() 
	{
		assertEquals(n1.getIngressCarriedMulticastTraffic(lowerLayer) , 10+10 , 0);
		assertEquals(n2.getIngressCarriedMulticastTraffic(lowerLayer) , 0 , 0);
		assertEquals(n3.getIngressCarriedMulticastTraffic(lowerLayer) , 0 , 0);
		assertEquals(n1.getIngressCarriedMulticastTraffic(upperLayer) , 10 , 0);
		assertEquals(n2.getIngressCarriedMulticastTraffic(upperLayer) , 0 , 0);
		assertEquals(n3.getIngressCarriedMulticastTraffic(upperLayer) , 0 , 0);
	}

	@Test
	public void testGetInNeighbors() 
	{
		assertEquals(n1.getInNeighbors(lowerLayer) , new HashSet<Node> (Arrays.asList()));
		assertEquals(n2.getInNeighbors(lowerLayer) , new HashSet<Node> (Arrays.asList(n1)));
		assertEquals(n3.getInNeighbors(lowerLayer) , new HashSet<Node> (Arrays.asList(n1,n2)));
		assertEquals(n1.getInNeighbors(upperLayer) , new HashSet<Node> (Arrays.asList()));
		assertEquals(n2.getInNeighbors(upperLayer) , new HashSet<Node> (Arrays.asList(n1)));
		assertEquals(n3.getInNeighbors(upperLayer) , new HashSet<Node> (Arrays.asList(n1)));
	}

	@Test
	public void testGetOutNeighbors() 
	{
		assertEquals(n1.getOutNeighbors(lowerLayer) , new HashSet<Node> (Arrays.asList(n2,n3)));
		assertEquals(n2.getOutNeighbors(lowerLayer) , new HashSet<Node> (Arrays.asList(n3)));
		assertEquals(n3.getOutNeighbors(lowerLayer) , new HashSet<Node> (Arrays.asList()));
		assertEquals(n1.getOutNeighbors(upperLayer) , new HashSet<Node> (Arrays.asList(n2,n3)));
		assertEquals(n2.getOutNeighbors(upperLayer) , new HashSet<Node> (Arrays.asList()));
		assertEquals(n3.getOutNeighbors(upperLayer) , new HashSet<Node> (Arrays.asList()));
	}

	@Test
	public void testGetOutNeighborsAllLayers() 
	{
		assertEquals(n1.getOutNeighborsAllLayers () , new HashSet<Node> (Arrays.asList(n2,n3)));
		assertEquals(n2.getOutNeighborsAllLayers () , new HashSet<Node> (Arrays.asList(n3)));
		assertEquals(n3.getOutNeighborsAllLayers () , new HashSet<Node> (Arrays.asList()));
	}

	@Test
	public void testGetInNeighborsAllLayers() 
	{
		assertEquals(n1.getInNeighborsAllLayers () , new HashSet<Node> (Arrays.asList()));
		assertEquals(n2.getInNeighborsAllLayers () , new HashSet<Node> (Arrays.asList(n1)));
		assertEquals(n3.getInNeighborsAllLayers () , new HashSet<Node> (Arrays.asList(n1,n2)));
	}

	@Test
	public void testGetXYPositionMap() 
	{
		assertEquals(n1.getXYPositionMap() , new java.awt.geom.Point2D.Double(0,0));
	}

	@Test
	public void testSetXYPositionMap() 
	{
		n1.setXYPositionMap(new Point2D.Double(1,2));
		assertEquals(n1.getXYPositionMap() , new java.awt.geom.Point2D.Double(1,2));
	}

	@Test
	public void testIsUp() 
	{
		assertTrue (n1.isUp());
		n1.setFailureState(false);
		assertTrue (!n1.isUp());
		n1.setFailureState(true);
		assertTrue (n1.isUp());
	}

	@Test
	public void testIsDown() 
	{
		assertTrue (!n1.isDown());
		n1.setFailureState(false);
		assertTrue (n1.isDown());
		n1.setFailureState(true);
		assertTrue (!n1.isDown());
	}

	@Test
	public void testGetIncomingLinks() 
	{
		assertEquals(n1.getIncomingLinks(lowerLayer) , new HashSet<Link> (Arrays.asList()));
		assertEquals(n2.getIncomingLinks(lowerLayer) , new HashSet<Link> (Arrays.asList(link12)));
		assertEquals(n3.getIncomingLinks(lowerLayer) , new HashSet<Link> (Arrays.asList(link23, link13)));
		assertEquals(n1.getIncomingLinks(upperLayer) , new HashSet<Link> (Arrays.asList()));
		assertEquals(n2.getIncomingLinks(upperLayer) , new HashSet<Link> (Arrays.asList(upperLink12 , upperMdLink12)));
		assertEquals(n3.getIncomingLinks(upperLayer) , new HashSet<Link> (Arrays.asList(upperMdLink13)));
	}

	@Test
	public void testGetOutgoingLinks() 
	{
		assertEquals(n1.getOutgoingLinks(lowerLayer) , new HashSet<Link> (Arrays.asList(link12 , link13)));
		assertEquals(n2.getOutgoingLinks(lowerLayer) , new HashSet<Link> (Arrays.asList(link23)));
		assertEquals(n3.getOutgoingLinks(lowerLayer) , new HashSet<Link> (Arrays.asList()));
		assertEquals(n1.getOutgoingLinks(upperLayer) , new HashSet<Link> (Arrays.asList(upperLink12 , upperMdLink12 , upperMdLink13)));
		assertEquals(n2.getOutgoingLinks(upperLayer) , new HashSet<Link> (Arrays.asList()));
		assertEquals(n3.getOutgoingLinks(upperLayer) , new HashSet<Link> (Arrays.asList()));
	}

	@Test
	public void testGetOutgoingLinksAllLayers() 
	{
		assertEquals(n1.getOutgoingLinksAllLayers() , new HashSet<Link> (Arrays.asList(link12 , link13 , upperLink12 , upperMdLink12 , upperMdLink13)));
		assertEquals(n2.getOutgoingLinksAllLayers() , new HashSet<Link> (Arrays.asList(link23)));
		assertEquals(n3.getOutgoingLinksAllLayers() , new HashSet<Link> (Arrays.asList()));
	}

	@Test
	public void testGetIncomingLinksAllLayers() 
	{
		assertEquals(n1.getIncomingLinksAllLayers() , new HashSet<Link> (Arrays.asList()));
		assertEquals(n2.getIncomingLinksAllLayers() , new HashSet<Link> (Arrays.asList(link12 , upperLink12 , upperMdLink12 )));
		assertEquals(n3.getIncomingLinksAllLayers() , new HashSet<Link> (Arrays.asList(link23 , link13 , upperMdLink13)));
	}

	@Test
	public void testGetOutgoingDemandsAllLayers() 
	{
		assertEquals(n1.getOutgoingDemandsAllLayers() , new HashSet<Demand> (Arrays.asList(d12 , d13 , scd123)));
		assertEquals(n2.getOutgoingDemandsAllLayers() , new HashSet<Demand> (Arrays.asList()));
		assertEquals(n3.getOutgoingDemandsAllLayers() , new HashSet<Demand> (Arrays.asList()));
	}

	@Test
	public void testGetIncomingDemandsAllLayers() 
	{
		assertEquals(n1.getIncomingDemandsAllLayers() , new HashSet<Demand> (Arrays.asList()));
		assertEquals(n2.getIncomingDemandsAllLayers() , new HashSet<Demand> (Arrays.asList(d12 )));
		assertEquals(n3.getIncomingDemandsAllLayers() , new HashSet<Demand> (Arrays.asList(d13 , scd123)));
	}

	@Test
	public void testGetOutgoingMulticastDemandsAllLayers() 
	{
		assertEquals(n1.getOutgoingMulticastDemandsAllLayers() , new HashSet<MulticastDemand> (Arrays.asList(d123 , upperMd123)));
		assertEquals(n2.getOutgoingMulticastDemandsAllLayers() , new HashSet<MulticastDemand> (Arrays.asList()));
		assertEquals(n3.getOutgoingMulticastDemandsAllLayers() , new HashSet<MulticastDemand> (Arrays.asList()));
	}

	@Test
	public void testGetIncomingMulticastDemandsAllLayers() 
	{
		assertEquals(n1.getIncomingMulticastDemandsAllLayers() , new HashSet<MulticastDemand> (Arrays.asList()));
		assertEquals(n2.getIncomingMulticastDemandsAllLayers() , new HashSet<MulticastDemand> (Arrays.asList(d123 , upperMd123)));
		assertEquals(n3.getIncomingMulticastDemandsAllLayers() , new HashSet<MulticastDemand> (Arrays.asList(d123 , upperMd123)));
	}

	@Test
	public void testGetIncomingDemands() 
	{
		assertEquals(n1.getIncomingDemands(lowerLayer) , new HashSet<Demand> (Arrays.asList()));
		assertEquals(n2.getIncomingDemands(lowerLayer) , new HashSet<Demand> (Arrays.asList(d12 )));
		assertEquals(n3.getIncomingDemands(lowerLayer) , new HashSet<Demand> (Arrays.asList(d13 , scd123)));
		assertEquals(n1.getIncomingDemands(upperLayer) , new HashSet<Demand> (Arrays.asList()));
		assertEquals(n2.getIncomingDemands(upperLayer) , new HashSet<Demand> (Arrays.asList()));
		assertEquals(n3.getIncomingDemands(upperLayer) , new HashSet<Demand> (Arrays.asList()));
	}

	@Test
	public void testSetFailureState() 
	{
		assertTrue (n1.isUp());
		n1.setFailureState(false);
		assertTrue (!n1.isUp());
		n1.setFailureState(true);
		assertTrue (n1.isUp());
	}

	@Test
	public void testGetIncomingRoutes() 
	{
		assertEquals(n1.getIncomingRoutes(lowerLayer) , new HashSet<Route> (Arrays.asList()));
		assertEquals(n2.getIncomingRoutes(lowerLayer) , new HashSet<Route> (Arrays.asList(r12)));
		assertEquals(n3.getIncomingRoutes(lowerLayer) , new HashSet<Route> (Arrays.asList(r123a , r123b , sc123 , segm13)));
		assertEquals(n1.getIncomingRoutes(upperLayer) , new HashSet<Route> (Arrays.asList()));
		assertEquals(n2.getIncomingRoutes(upperLayer) , new HashSet<Route> (Arrays.asList()));
		assertEquals(n3.getIncomingRoutes(upperLayer) , new HashSet<Route> (Arrays.asList()));
	}

	@Test
	public void testGetIncomingMulticastTrees() 
	{
		assertEquals(n1.getIncomingMulticastTrees(lowerLayer) , new HashSet<MulticastTree> (Arrays.asList()));
		assertEquals(n2.getIncomingMulticastTrees(lowerLayer) , new HashSet<MulticastTree> (Arrays.asList(tStar , t123)));
		assertEquals(n3.getIncomingMulticastTrees(lowerLayer) , new HashSet<MulticastTree> (Arrays.asList(tStar , t123)));
		assertEquals(n1.getIncomingMulticastTrees(upperLayer) , new HashSet<MulticastTree> (Arrays.asList()));
		assertEquals(n2.getIncomingMulticastTrees(upperLayer) , new HashSet<MulticastTree> (Arrays.asList(upperMt123)));
		assertEquals(n3.getIncomingMulticastTrees(upperLayer) , new HashSet<MulticastTree> (Arrays.asList(upperMt123)));
	}

	@Test
	public void testGetOutgoingRoutes() 
	{
		assertEquals(n1.getOutgoingRoutes(lowerLayer) , new HashSet<Route> (Arrays.asList(r12 , r123a , r123b , sc123 , segm13)));
		assertEquals(n2.getOutgoingRoutes(lowerLayer) , new HashSet<Route> (Arrays.asList()));
		assertEquals(n3.getOutgoingRoutes(lowerLayer) , new HashSet<Route> (Arrays.asList()));
		assertEquals(n1.getOutgoingRoutes(upperLayer) , new HashSet<Route> (Arrays.asList()));
		assertEquals(n2.getOutgoingRoutes(upperLayer) , new HashSet<Route> (Arrays.asList()));
		assertEquals(n3.getOutgoingRoutes(upperLayer) , new HashSet<Route> (Arrays.asList()));
	}

	@Test
	public void testGetOutgoingMulticastTrees() 
	{
		assertEquals(n1.getOutgoingMulticastTrees(lowerLayer) , new HashSet<MulticastTree> (Arrays.asList(tStar , t123)));
		assertEquals(n2.getOutgoingMulticastTrees(lowerLayer) , new HashSet<MulticastTree> (Arrays.asList()));
		assertEquals(n3.getOutgoingMulticastTrees(lowerLayer) , new HashSet<MulticastTree> (Arrays.asList()));
		assertEquals(n1.getOutgoingMulticastTrees(upperLayer) , new HashSet<MulticastTree> (Arrays.asList(upperMt123)));
		assertEquals(n2.getOutgoingMulticastTrees(upperLayer) , new HashSet<MulticastTree> (Arrays.asList()));
		assertEquals(n3.getOutgoingMulticastTrees(upperLayer) , new HashSet<MulticastTree> (Arrays.asList()));
	}

	@Test
	public void testGetOutgoingDemands() 
	{
		assertEquals(n1.getOutgoingDemands(lowerLayer) , new HashSet<Demand> (Arrays.asList(d12, d13,scd123)));
		assertEquals(n2.getOutgoingDemands(lowerLayer) , new HashSet<Demand> (Arrays.asList()));
		assertEquals(n3.getOutgoingDemands(lowerLayer) , new HashSet<Demand> (Arrays.asList()));
		assertEquals(n1.getOutgoingDemands(upperLayer) , new HashSet<Demand> (Arrays.asList()));
		assertEquals(n2.getOutgoingDemands(upperLayer) , new HashSet<Demand> (Arrays.asList()));
		assertEquals(n3.getOutgoingDemands(upperLayer) , new HashSet<Demand> (Arrays.asList()));
	}

	@Test
	public void testGetIncomingMulticastDemands() 
	{
		assertEquals(n1.getIncomingMulticastDemands(lowerLayer) , new HashSet<MulticastDemand> (Arrays.asList()));
		assertEquals(n2.getIncomingMulticastDemands(lowerLayer) , new HashSet<MulticastDemand> (Arrays.asList(d123)));
		assertEquals(n3.getIncomingMulticastDemands(lowerLayer) , new HashSet<MulticastDemand> (Arrays.asList(d123)));
		assertEquals(n1.getIncomingMulticastDemands(upperLayer) , new HashSet<MulticastDemand> (Arrays.asList()));
		assertEquals(n2.getIncomingMulticastDemands(upperLayer) , new HashSet<MulticastDemand> (Arrays.asList(upperMd123)));
		assertEquals(n3.getIncomingMulticastDemands(upperLayer) , new HashSet<MulticastDemand> (Arrays.asList(upperMd123)));
	}

	@Test
	public void testGetOutgoingMulticastDemands() 
	{
		assertEquals(n1.getOutgoingMulticastDemands(lowerLayer) , new HashSet<MulticastDemand> (Arrays.asList(d123)));
		assertEquals(n2.getOutgoingMulticastDemands(lowerLayer) , new HashSet<MulticastDemand> (Arrays.asList()));
		assertEquals(n3.getOutgoingMulticastDemands(lowerLayer) , new HashSet<MulticastDemand> (Arrays.asList()));
		assertEquals(n1.getOutgoingMulticastDemands(upperLayer) , new HashSet<MulticastDemand> (Arrays.asList(upperMd123)));
		assertEquals(n2.getOutgoingMulticastDemands(upperLayer) , new HashSet<MulticastDemand> (Arrays.asList()));
		assertEquals(n3.getOutgoingMulticastDemands(upperLayer) , new HashSet<MulticastDemand> (Arrays.asList()));
	}

	@Test
	public void testGetAssociatedRoutes() 
	{
		assertEquals(n1.getAssociatedRoutes(lowerLayer) , new HashSet<Route> (Arrays.asList(r12 , r123a , r123b , sc123 , segm13)));
		assertEquals(n2.getAssociatedRoutes(lowerLayer) , new HashSet<Route> (Arrays.asList(r12 , r123a , r123b , sc123)));
		assertEquals(n3.getAssociatedRoutes(lowerLayer) , new HashSet<Route> (Arrays.asList(r123a , r123b , sc123 , segm13)));
		assertEquals(n1.getAssociatedRoutes(upperLayer) , new HashSet<Route> (Arrays.asList()));
		assertEquals(n2.getAssociatedRoutes(upperLayer) , new HashSet<Route> (Arrays.asList()));
		assertEquals(n3.getAssociatedRoutes(upperLayer) , new HashSet<Route> (Arrays.asList()));
	}

	@Test
	public void testGetAssociatedMulticastTrees() 
	{
		assertEquals(n1.getAssociatedMulticastTrees(lowerLayer) , new HashSet<MulticastTree> (Arrays.asList(tStar , t123)));
		assertEquals(n2.getAssociatedMulticastTrees(lowerLayer) , new HashSet<MulticastTree> (Arrays.asList(tStar , t123)));
		assertEquals(n3.getAssociatedMulticastTrees(lowerLayer) , new HashSet<MulticastTree> (Arrays.asList(tStar , t123)));
		assertEquals(n1.getAssociatedMulticastTrees(upperLayer) , new HashSet<MulticastTree> (Arrays.asList(upperMt123)));
		assertEquals(n2.getAssociatedMulticastTrees(upperLayer) , new HashSet<MulticastTree> (Arrays.asList(upperMt123)));
		assertEquals(n3.getAssociatedMulticastTrees(upperLayer) , new HashSet<MulticastTree> (Arrays.asList(upperMt123)));
	}

	@Test
	public void testGetSRGs() 
	{
		assertEquals(n1.getSRGs() , new HashSet<SharedRiskGroup> (Arrays.asList()));
		assertEquals(n2.getSRGs() , new HashSet<SharedRiskGroup> (Arrays.asList()));
		assertEquals(n3.getSRGs() , new HashSet<SharedRiskGroup> (Arrays.asList()));
		SharedRiskGroup srg = np.addSRG(1,1,null);
		srg.addNode(n1);
		assertEquals(n1.getSRGs() , new HashSet<SharedRiskGroup> (Arrays.asList(srg)));
		assertEquals(n2.getSRGs() , new HashSet<SharedRiskGroup> (Arrays.asList()));
		assertEquals(n3.getSRGs() , new HashSet<SharedRiskGroup> (Arrays.asList()));
		srg.addNode(n3);
		assertEquals(n1.getSRGs() , new HashSet<SharedRiskGroup> (Arrays.asList(srg)));
		assertEquals(n2.getSRGs() , new HashSet<SharedRiskGroup> (Arrays.asList()));
		assertEquals(n3.getSRGs() , new HashSet<SharedRiskGroup> (Arrays.asList(srg)));
	}

	@Test
	public void testGetResources() 
	{
		assertEquals(n1.getResources("notType") , new HashSet<Resource> (Arrays.asList()));
		assertEquals(n2.getResources("notType") , new HashSet<Resource> (Arrays.asList()));
		assertEquals(n3.getResources("notType") , new HashSet<Resource> (Arrays.asList()));
		assertEquals(n1.getResources("type") , new HashSet<Resource> (Arrays.asList()));
		assertEquals(n2.getResources("type") , new HashSet<Resource> (Arrays.asList(res2 , res2backup)));
		assertEquals(n3.getResources("type") , new HashSet<Resource> (Arrays.asList()));
		assertEquals(n1.getResources() , new HashSet<Resource> (Arrays.asList()));
		assertEquals(n2.getResources() , new HashSet<Resource> (Arrays.asList(res2 , res2backup)));
		assertEquals(n3.getResources() , new HashSet<Resource> (Arrays.asList()));
	}

	@Test
	public void testRemove() 
	{
		n1.remove();
		assertEquals(np.getNodes() , Arrays.asList(n2,n3));
		n3.remove();
		assertEquals(np.getNodes() , Arrays.asList(n2));
		n2.remove();
		assertEquals(np.getNodes() , Arrays.asList());
	}

	@Test
	public void testRemoveAllForwardingRules() 
	{
		scd123.remove();
		np.setRoutingType(RoutingType.HOP_BY_HOP_ROUTING , lowerLayer); 
		n1.removeAllForwardingRules(lowerLayer);
		assertEquals (n1.getForwardingRules(lowerLayer) , Collections.emptyMap());
	}
	@Test
	public void testGetForwardingRulesNetworkLayerArray() 
	{
		try { np.setRoutingType(RoutingType.HOP_BY_HOP_ROUTING , lowerLayer); fail ("Not in service chains"); } catch (Exception e) {}
		scd123.remove();
		np.setRoutingType(RoutingType.HOP_BY_HOP_ROUTING , lowerLayer); 
		n1.getForwardingRules(lowerLayer);
		
		Map<Pair<Demand,Link>,Double> fr = new HashMap<Pair<Demand,Link>,Double> ();
		fr.put(Pair.of (d12,link12) , 1.0);
		fr.put(Pair.of (d13,link12) , 1.0);
		assertEquals (n1.getForwardingRules(lowerLayer) , fr);

		fr = new HashMap<Pair<Demand,Link>,Double> ();
		fr.put(Pair.of (d13,link23) , 1.0);
		assertEquals (n2.getForwardingRules(lowerLayer) , fr);

		assertEquals (n3.getForwardingRules(lowerLayer) , Collections.emptyMap());
	}

	@Test
	public void testGetForwardingRulesDemand() 
	{
		try { np.setRoutingType(RoutingType.HOP_BY_HOP_ROUTING , lowerLayer); fail ("Not in service chains"); } catch (Exception e) {}
		scd123.remove();
		np.setRoutingType(RoutingType.HOP_BY_HOP_ROUTING , lowerLayer); 
		n1.getForwardingRules(lowerLayer);
		
		Map<Pair<Demand,Link>,Double> fr = new HashMap<Pair<Demand,Link>,Double> ();
		fr.put(Pair.of (d12,link12) , 1.0);
		assertEquals (n1.getForwardingRules(d12) , fr);

		fr = new HashMap<Pair<Demand,Link>,Double> ();
		fr.put(Pair.of (d13,link12) , 1.0);
		assertEquals (n1.getForwardingRules(d13) , fr);

		fr = new HashMap<Pair<Demand,Link>,Double> ();
		fr.put(Pair.of (d13,link23) , 1.0);
		assertEquals (n2.getForwardingRules(d13) , fr);

		assertEquals (n3.getForwardingRules(d12) , Collections.emptyMap());
		assertEquals (n3.getForwardingRules(d13) , Collections.emptyMap());
	}

}
