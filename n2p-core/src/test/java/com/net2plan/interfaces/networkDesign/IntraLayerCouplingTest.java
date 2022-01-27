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
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.net2plan.libraries.GraphUtils;
import com.net2plan.utils.Constants.RoutingType;

public class IntraLayerCouplingTest
{
	private NetPlan np = null;
	private Node n1, n2 , n3;
	private Link link12_L0, link23_L0, link13_L0 , link31_L0;
	private Link link12_L01, link13_L01, link23_L01;
	private Demand demand12_L01, demand13_L01, demand23_L01 , demand31_L01;
	private Demand demand12_L1, demand13_L1, demand23_L1;
	private Link link12_L1, link23_L1, link31_L1;
	private Demand demand12_cL1, demand23_cL1, demand31_cL1;

	private NetworkLayer layer0 , layer1;
//	private InterLayerPropagationGraph g_demand23_L2_up , g_demand23_L2_down , g_mDemand123_L2_N1_up , g_mDemand123_L2_N1_down; 
//	private InterLayerPropagationGraph g_link12_L0_up , g_link12_L0_down , g_link13_L0_up , g_link13_L0_down;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception
	{
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception
	{
	}

	@Before
	public void setUp() throws Exception
	{
		this.np = new NetPlan ();
		this.layer0 = np.getNetworkLayerDefault(); np.setDemandTrafficUnitsName("Mbps" , layer0); np.setLinkCapacityUnitsName("Mbps" , layer0);
		this.layer1 = np.addLayer("l1" , "description" , "Mbps" , "Mbps" , null , null);
		this.n1 = this.np.addNode(0 , 0 , "node1" , null);
		this.n2 = np.addNode(0 , 0 , "node2" , null);
		this.n3 = np.addNode(0 , 0 , "node3" , null);
		this.link12_L0 = np.addLink(n1,n2,100,100,1,null,layer0);
		this.link23_L0 = np.addLink(n2,n3,100,100,1,null,layer0);
		this.link13_L0 = np.addLink(n1,n3,100,100,1,null,layer0);
		this.link31_L0 = np.addLink(n3,n1,100,100,1,null,layer0);
		this.demand12_L01 = np.addDemand(n1 , n2 , 1  , RoutingType.SOURCE_ROUTING, null,layer0); 
		this.demand13_L01 = np.addDemand(n1 , n3 , 1  , RoutingType.SOURCE_ROUTING, null,layer0);
		this.demand23_L01 = np.addDemand(n2 , n3 , 1  , RoutingType.SOURCE_ROUTING, null,layer0);
		this.demand31_L01 = np.addDemand(n3 , n1 , 1  , RoutingType.SOURCE_ROUTING, null,layer0);
		this.link12_L01 = demand12_L01.coupleToNewLinkCreated(layer0);
		this.link13_L01 = demand13_L01.coupleToNewLinkCreated(layer0);
		this.link23_L01 = demand23_L01.coupleToNewLinkCreated(layer0);

		this.link12_L1 = np.addLink(n1,n2,100,100,1,null,layer1);
		this.link23_L1 = np.addLink(n2,n3,100,100,1,null,layer1);
		this.link31_L1 = np.addLink(n3,n1,100,100,1,null,layer1);
		this.demand12_cL1 = link12_L1.coupleToNewDemandCreated(layer0 , RoutingType.SOURCE_ROUTING); 
		this.demand23_cL1 = link23_L1.coupleToNewDemandCreated(layer0 , RoutingType.SOURCE_ROUTING); 
		this.demand31_cL1 = link31_L1.coupleToNewDemandCreated(layer0 , RoutingType.SOURCE_ROUTING); 
		
		this.demand12_L1 = np.addDemand(n1 , n2 , 1  , RoutingType.SOURCE_ROUTING, null,layer1); 
		this.demand13_L1 = np.addDemand(n1 , n3 , 1  , RoutingType.SOURCE_ROUTING, null,layer1); 
		this.demand23_L1 = np.addDemand(n2 , n3 , 1  , RoutingType.SOURCE_ROUTING, null,layer1); 
		

		System.out.println("------ START ----- ");
		for (Demand d : Arrays.asList(demand12_L01, demand13_L01, demand23_L01 , 
				demand31_L01 , demand12_cL1, demand23_cL1, 
				demand31_cL1 , demand12_L1, demand13_L1, 
				demand23_L1))
		{
			final List<Link> sp = GraphUtils.getShortestPath(np.getNodes() , np.getLinks(d.getLayer()) , d.getIngressNode() , d.getEgressNode(),  null);
			System.out.println("Demand " + d + ", SP: " + sp);
			np.addRoute(d , 0.5 , 0.5 , sp , null);
		}
		System.out.println("------ END ----- ");
		
//		this.g_demand23_L2_up = new InterLayerPropagationGraph(new TreeSet<> (Arrays.asList (demand23_L2) , null , null , true);
//		this.g_demand23_L2_down = new InterLayerPropagationGraph(new TreeSet<> (Arrays.asList (demand23_L2) , null , null , false);
//		this.g_mDemand123_L2_N1_up = new InterLayerPropagationGraph(null ,null , new TreeSet<> (Arrays.asList (Pair.of(mDemand123_L2 , n2)) , true);
//		this.g_mDemand123_L2_N1_down = new InterLayerPropagationGraph(null ,null , new TreeSet<> (Arrays.asList (Pair.of(mDemand123_L2 , n2)) , false);
//		this.g_link12_L0_up = new InterLayerPropagationGraph(null , new TreeSet<> (Arrays.asList (link12_L0) , null , true);
//		this.g_link12_L0_down = new InterLayerPropagationGraph(null , new TreeSet<> (Arrays.asList (link12_L0) , null , false);
//		this.g_link13_L0_up = new InterLayerPropagationGraph(null , new TreeSet<> (Arrays.asList (link13_L0) , null , true);
//		this.g_link13_L0_down = new InterLayerPropagationGraph(null , new TreeSet<> (Arrays.asList (link13_L0) , null , false);
	}

	@After
	public void tearDown() throws Exception
	{
	}

	@Test
	public void testFailures()
	{
		for (NetworkLayer layer : np.getNetworkLayers())
			for (Link e : np.getLinks(layer))
			{
				np.setAllLinksFailureState(true, layer0);
				np.setAllLinksFailureState(true, layer1);
				np.setLinksAndNodesFailureState(null, Arrays.asList(e), null, null);
			}
		
		for (Node n : np.getNodes())
		{
			np.setAllNodesFailureState(true);
			np.setLinksAndNodesFailureState(null, null , null , Arrays.asList(n));
		}
	}

	@Test
	public void testNoInternalLoop()
	{
		final Demand dL0 = np.addDemand(n1, n2, 10 , RoutingType.SOURCE_ROUTING, null, layer0);
		final Link linkL0 = dL0.coupleToNewLinkCreated(layer0);
		np.addRoute(dL0, 1, 1, Arrays.asList(link12_L0), null);
		np.addRoute(dL0, 1, 1, Arrays.asList(link12_L01), null); 

		dL0.decouple();
		final Link linkL1 = dL0.coupleToNewLinkCreated(layer1);
		np.addRoute(dL0, 1, 1, Arrays.asList(link12_L0), null);
		np.addRoute(dL0, 1, 1, Arrays.asList(link12_L01), null); 
	}

	@Test
	public void testNoInternalLoop_1()
	{
		link12_L0.coupleToNewDemandCreated(layer0 , RoutingType.SOURCE_ROUTING); 
	}

	@Test
	public void testNoInternalLoop_2()
	{
		final Demand dL0 = np.addDemand(n1, n2, 10 , RoutingType.SOURCE_ROUTING, null, layer0);
		final Link linkL0 = dL0.coupleToNewLinkCreated(layer0);
		np.addRoute(dL0, 1, 1, Arrays.asList(link12_L0), null);
		np.addRoute(dL0, 1, 1, Arrays.asList(link12_L01), null); 

		final Demand dLink12_L0 = link12_L0.coupleToNewDemandCreated(layer0 , RoutingType.SOURCE_ROUTING);
		try { np.addRoute(dLink12_L0, 1, 1, Arrays.asList(linkL0), null); fail (); } catch (Exception e) {}
		try { np.addRoute(dLink12_L0, 1, 1, Arrays.asList(link12_L0), null); fail (); } catch (Exception e) {}
		try { np.addRoute(dLink12_L0, 1, 1, Arrays.asList(link12_L01), null); fail (); } catch (Exception e) {}
		final Demand upperTodL0 = np.addDemand(n1, n2, 10 , RoutingType.SOURCE_ROUTING, null, layer0);
		np.addRoute(upperTodL0, 1, 1, Arrays.asList(link12_L01), null); 
		np.addRoute(upperTodL0, 1, 1, Arrays.asList(link12_L0), null); 
		np.addRoute(upperTodL0, 1, 1, Arrays.asList(linkL0), null); 
	}

	@Test
	public void testIPG_hbh()
	{
		np.setRoutingTypeAllDemands(RoutingType.HOP_BY_HOP_ROUTING, layer0);
		testIPG ();
		testFailures();
	}

	@Test
	public void testIPG_hbhPartial()
	{
		demand12_L01.setRoutingType(RoutingType.HOP_BY_HOP_ROUTING);
		demand23_L01.setRoutingType(RoutingType.HOP_BY_HOP_ROUTING);
		demand12_L1.setRoutingType(RoutingType.HOP_BY_HOP_ROUTING);
		demand23_L1.setRoutingType(RoutingType.HOP_BY_HOP_ROUTING);
		demand12_cL1.setRoutingType(RoutingType.HOP_BY_HOP_ROUTING);
		demand31_cL1.setRoutingType(RoutingType.HOP_BY_HOP_ROUTING);
		testIPG ();
		testFailures();
	}

	@Test
	public void testIPG()
	{
		for (boolean upDown : new boolean [] {true , false})
		{
			new InterLayerPropagationGraph(new TreeSet<> (Arrays.asList (demand12_L1)) , null , null , upDown);
			new InterLayerPropagationGraph(new TreeSet<> (Arrays.asList (demand13_L1)) , null , null , upDown);
			new InterLayerPropagationGraph(new TreeSet<> (Arrays.asList (demand23_L1)) , null , null , upDown);
			new InterLayerPropagationGraph(new TreeSet<> (Arrays.asList (demand12_cL1)) , null , null , upDown);
			new InterLayerPropagationGraph(new TreeSet<> (Arrays.asList (demand23_cL1)) , null , null , upDown);
			new InterLayerPropagationGraph(new TreeSet<> (Arrays.asList (demand31_cL1)) , null , null , upDown);
			new InterLayerPropagationGraph(new TreeSet<> (Arrays.asList (demand12_L01)) , null , null , upDown);
			new InterLayerPropagationGraph(new TreeSet<> (Arrays.asList (demand13_L01)) , null , null , upDown);
			new InterLayerPropagationGraph(new TreeSet<> (Arrays.asList (demand23_L01)) , null , null , upDown);
			new InterLayerPropagationGraph(new TreeSet<> (Arrays.asList (demand31_L01)) , null , null , upDown);

			new InterLayerPropagationGraph(null , new TreeSet<> (Arrays.asList (link12_L0)) , null , upDown);
			new InterLayerPropagationGraph(null , new TreeSet<> (Arrays.asList (link23_L0)) , null , upDown);
			new InterLayerPropagationGraph(null , new TreeSet<> (Arrays.asList (link13_L0)) , null , upDown);
			new InterLayerPropagationGraph(null , new TreeSet<> (Arrays.asList (link31_L0)) , null , upDown);
			new InterLayerPropagationGraph(null , new TreeSet<> (Arrays.asList (link12_L01)) , null , upDown);
			new InterLayerPropagationGraph(null , new TreeSet<> (Arrays.asList (link13_L01)) , null , upDown);
			new InterLayerPropagationGraph(null , new TreeSet<> (Arrays.asList (link23_L01)) , null , upDown);
			new InterLayerPropagationGraph(null , new TreeSet<> (Arrays.asList (link12_L1)) , null , upDown);
			new InterLayerPropagationGraph(null , new TreeSet<> (Arrays.asList (link23_L1)) , null , upDown);
			new InterLayerPropagationGraph(null , new TreeSet<> (Arrays.asList (link31_L1)) , null , upDown);
		}

		assertEquals (new InterLayerPropagationGraph(new TreeSet<> (Arrays.asList (demand12_L1)) , null , null , true).getLinksInGraph() , new TreeSet<> (Arrays.asList ()));
		assertEquals (new InterLayerPropagationGraph(new TreeSet<> (Arrays.asList (demand12_L1)) , null , null , true).getDemandsInGraph() , new TreeSet<> (Arrays.asList (demand12_L1)));
		
		assertEquals (new InterLayerPropagationGraph(new TreeSet<> (Arrays.asList (demand12_L1)) , null , null , false).getDemandsInGraph() , new TreeSet<> (Arrays.asList (demand12_L1 , demand12_cL1)));
		assertEquals (new InterLayerPropagationGraph(new TreeSet<> (Arrays.asList (demand12_L1)) , null , null , false).getLinksInGraph() , new TreeSet<> (Arrays.asList (link12_L1 , link12_L0)));
	}

	
	
}
