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

import com.google.common.collect.Sets;
import com.net2plan.libraries.GraphUtils;
import com.net2plan.utils.Pair;
import org.junit.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

@SuppressWarnings("unchecked")
public class InterLayerPropagationGraphTest
{
	/* Layer 0: link 1->2->3, and 1->3 */
	/* Layer 0: cdemand 1->2 [C-L1], cDemand 1->3 [C-L1] cdemand 2-3 [C-L2]*/
	/* Layer 1: clink c1->2 [C-L0], c1->3 [C-L0] , 2->3 */
	/* Layer 1: cdemand 1->3 [C-L2] , mdemand 1->[2,3][C-L2] */
	/* Layer 2: clink 1->3 [C-L1], clinkMd[1->2, 1->3] [C-L1] , clink2->3 [C-L0] */
	/* Layer 2: demand 1->3 , mDemand 1->[2,3], demand 2->3 , demand 1->2 */
	
	private NetPlan np = null;
	private Node n1, n2 , n3;
	private Link link12_L0, link23_L0, link13_L0;
	private Link link12_L1_cL0, link13_L1_cL0, link23_L1;
	private Link link13_L2_cL1, link12_L2_cML1, link13_L2_cML1 , link23_L2_cL0;
	private Demand demand12_L0_CL1 , demand13_L0_CL1 , demand23_L0_CL2;
	private Demand demand13_L1_cL2; private MulticastDemand mDemand123_L1_CL2;
	private Demand demand23_L2, demand12_L2; private MulticastDemand mDemand123_L2;
	private NetworkLayer layer0 , layer1 , layer2;
	private InterLayerPropagationGraph g_demand23_L2_up , g_demand23_L2_down , g_mDemand123_L2_N1_up , g_mDemand123_L2_N1_down; 
	private InterLayerPropagationGraph g_link12_L0_up , g_link12_L0_down , g_link13_L0_up , g_link13_L0_down;

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
		this.layer0 = np.getNetworkLayerDefault(); np.setDemandTrafficUnitsName("Mbps" , layer0);
		this.layer1 = np.addLayer("l1" , "description" , "Mbps" , "Mbps" , null , null);
		this.layer2 = np.addLayer("l2" , "description" , "Mbps" , "Mbps" , null , null);
		this.n1 = this.np.addNode(0 , 0 , "node1" , null);
		this.n2 = np.addNode(0 , 0 , "node2" , null);
		this.n3 = np.addNode(0 , 0 , "node3" , null);
		this.link12_L0 = np.addLink(n1,n2,100,100,1,null,layer0);
		this.link23_L0 = np.addLink(n2,n3,100,100,1,null,layer0);
		this.link13_L0 = np.addLink(n1,n3,100,100,1,null,layer0);
		this.link12_L1_cL0 = np.addLink(n1,n2,100,100,1,null,layer1);
		this.link13_L1_cL0 = np.addLink(n1,n3,100,100,1,null,layer1);
		this.link23_L1 = np.addLink(n2,n3,100,100,1,null,layer1);
		this.link13_L2_cL1 = np.addLink(n1,n3,100,100,1,null,layer2);
		this.link12_L2_cML1 = np.addLink(n1,n2,100,100,1,null,layer2);
		this.link13_L2_cML1 = np.addLink(n1,n3,100,100,1,null,layer2);
		this.link23_L2_cL0 = np.addLink(n2,n3,100,100,1,null,layer2);
		this.demand12_L0_CL1 = np.addDemand(n1 , n2 , 0 , null,layer0); 
		this.demand13_L0_CL1 = np.addDemand(n1 , n3 , 0 , null,layer0);
		this.demand23_L0_CL2 = np.addDemand(n2 , n3 , 0 , null,layer0);
		this.demand13_L1_cL2 = np.addDemand(n1 , n3 , 0 , null,layer1);
		this.demand23_L2 = np.addDemand(n2 , n3 , 0 , null,layer2);
		this.demand12_L2 = np.addDemand(n1 , n2 , 0 , null,layer2);
		this.mDemand123_L1_CL2 = np.addMulticastDemand(n1 , Sets.newHashSet(n2,n3) , 100 , null , layer1);
		this.mDemand123_L2 = np.addMulticastDemand(n1 , Sets.newHashSet(n2,n3) , 100 , null , layer2);

		for (Demand d : Arrays.asList(demand12_L0_CL1 , demand13_L0_CL1 , demand23_L0_CL2 , demand13_L1_cL2 , demand23_L2 , demand12_L2))
			np.addRoute(d , 0 , 0 , GraphUtils.getShortestPath(np.getNodes() , np.getLinks(d.getLayer()) , d.getIngressNode() , d.getEgressNode(),  null) , null);
		for (MulticastDemand md : Arrays.asList(mDemand123_L1_CL2 , mDemand123_L2))
		{
			Set<Link> tree = new HashSet<> ();
			for (Node n : md.getEgressNodes())
				tree.addAll(GraphUtils.getShortestPath(np.getNodes() , np.getLinks(md.getLayer()) , md.getIngressNode() , n,  null));
			np.addMulticastTree(md , 0 , 0 , tree , null);
		}
		
		/* Layer 0: link 1->2->3, and 1->3 */
		/* Layer 0: cdemand 1->2 [C-L1], cDemand 1->3 [C-L1] cdemand 2-3 [C-L2]*/
		/* Layer 1: clink c1->2 [C-L0], c1->3 [C-L0] , 2->3 */
		/* Layer 1: cdemand 1->3 [C-L2] , mdemand 1->[2,3][C-L2] */
		/* Layer 2: clink 1->3 [C-L1], clinkMd[1->2, 1->3] [C-L1] , clink2->3 [C-L0] */
		/* Layer 2: demand 1->3 , mDemand 1->[2,3], demand 2->3  */

		this.mDemand123_L1_CL2.couple(Sets.newHashSet(link12_L2_cML1 , link13_L2_cML1));
		this.demand13_L1_cL2.coupleToUpperLayerLink(link13_L2_cL1);
		this.demand12_L0_CL1.coupleToUpperLayerLink(link12_L1_cL0);
		this.demand13_L0_CL1.coupleToUpperLayerLink(link13_L1_cL0);
		this.demand23_L0_CL2.coupleToUpperLayerLink(link23_L2_cL0);
		try { demand12_L2.coupleToUpperLayerLink(link12_L0); fail (); } catch (Exception e) {} 
		
		this.g_demand23_L2_up = new InterLayerPropagationGraph(Sets.newHashSet(demand23_L2) , null , null , true);
		this.g_demand23_L2_down = new InterLayerPropagationGraph(Sets.newHashSet(demand23_L2) , null , null , false);
		this.g_mDemand123_L2_N1_up = new InterLayerPropagationGraph(null ,null , Sets.newHashSet(Pair.of(mDemand123_L2 , n2)) , true);
		this.g_mDemand123_L2_N1_down = new InterLayerPropagationGraph(null ,null , Sets.newHashSet(Pair.of(mDemand123_L2 , n2)) , false);
		this.g_link12_L0_up = new InterLayerPropagationGraph(null , Sets.newHashSet(link12_L0) , null , true);
		this.g_link12_L0_down = new InterLayerPropagationGraph(null , Sets.newHashSet(link12_L0) , null , false);
		this.g_link13_L0_up = new InterLayerPropagationGraph(null , Sets.newHashSet(link13_L0) , null , true);
		this.g_link13_L0_down = new InterLayerPropagationGraph(null , Sets.newHashSet(link13_L0) , null , false);
	}

	@After
	public void tearDown() throws Exception
	{
	}

	@Test
	public void testGetInitialIPGVertices()
	{
		assertEquals (g_demand23_L2_up.getInitialIPGVertices().iterator().next().getDemand() , demand23_L2);
		assertEquals (g_mDemand123_L2_N1_down.getInitialIPGVertices().iterator().next().getMulticastDemandAndNode() , Pair.of(mDemand123_L2 , n2));
		assertEquals (g_link13_L0_down.getInitialIPGVertices().iterator().next().getLink() , link13_L0);
	}

	@Test
	public void testIsUpWardsTrueDownwardsFalse()
	{
		assertTrue (g_demand23_L2_up.isUpWardsTrueDownwardsFalse());
		assertTrue (!g_demand23_L2_down.isUpWardsTrueDownwardsFalse());
	}

	@Test
	public void testGetLinksInGraph()
	{
		assertEquals(g_demand23_L2_up.getLinksInGraph() , Sets.newHashSet());
		assertEquals(g_demand23_L2_down.getLinksInGraph() , Sets.newHashSet(link23_L2_cL0 , link23_L0));
		assertEquals(g_mDemand123_L2_N1_up.getLinksInGraph() , Sets.newHashSet());
		assertEquals(g_mDemand123_L2_N1_down.getLinksInGraph() , Sets.newHashSet(link12_L2_cML1 , link12_L1_cL0 , link12_L0));
		assertEquals(g_link12_L0_up.getLinksInGraph() , Sets.newHashSet(link12_L0 , link12_L1_cL0, link12_L2_cML1));
		assertEquals(g_link12_L0_down.getLinksInGraph() , Sets.newHashSet(link12_L0));
		assertEquals(g_link13_L0_down.getLinksInGraph() , Sets.newHashSet(link13_L0));
		assertEquals(g_link13_L0_up.getLinksInGraph() , Sets.newHashSet(link13_L0 , link13_L1_cL0, link13_L2_cL1 , link13_L2_cML1));
	}

	@Test
	public void testGetDemandsInGraph()
	{
		assertEquals(g_demand23_L2_up.getDemandsInGraph() , Sets.newHashSet(demand23_L2));
		assertEquals(g_demand23_L2_down.getDemandsInGraph() , Sets.newHashSet(demand23_L2 , demand23_L0_CL2));
		assertEquals(g_mDemand123_L2_N1_up.getDemandsInGraph() , Sets.newHashSet());
		assertEquals(g_mDemand123_L2_N1_down.getDemandsInGraph() , Sets.newHashSet(demand12_L0_CL1));
		assertEquals(g_link12_L0_up.getDemandsInGraph() , Sets.newHashSet(demand12_L0_CL1 , demand12_L2));
		assertEquals(g_link12_L0_down.getDemandsInGraph() , Sets.newHashSet());
		assertEquals(g_link13_L0_down.getDemandsInGraph() , Sets.newHashSet());
		assertEquals(g_link13_L0_up.getDemandsInGraph() , Sets.newHashSet(demand13_L0_CL1 , demand13_L1_cL2));
	}

	@Test
	public void testGetMulticastDemandFlowsInGraph()
	{
		assertEquals(g_demand23_L2_up.getMulticastDemandFlowsInGraph() , Sets.newHashSet());
		assertEquals(g_demand23_L2_down.getMulticastDemandFlowsInGraph() , Sets.newHashSet());
		assertEquals(g_mDemand123_L2_N1_up.getMulticastDemandFlowsInGraph() , Sets.newHashSet(Pair.of(mDemand123_L2,n2)));
		assertEquals(g_mDemand123_L2_N1_down.getMulticastDemandFlowsInGraph() , Sets.newHashSet(Pair.of(mDemand123_L2,n2) , Pair.of(mDemand123_L1_CL2,n2)));
		assertEquals(g_link12_L0_up.getMulticastDemandFlowsInGraph() , Sets.newHashSet(Pair.of(mDemand123_L2,n2) , Pair.of(mDemand123_L1_CL2,n2)));
		assertEquals(g_link12_L0_down.getMulticastDemandFlowsInGraph() , Sets.newHashSet());
		assertEquals(g_link13_L0_down.getMulticastDemandFlowsInGraph() , Sets.newHashSet());
		assertEquals(g_link13_L0_up.getMulticastDemandFlowsInGraph() , Sets.newHashSet(Pair.of(mDemand123_L2,n3) , Pair.of(mDemand123_L1_CL2,n3)));
	}

}
