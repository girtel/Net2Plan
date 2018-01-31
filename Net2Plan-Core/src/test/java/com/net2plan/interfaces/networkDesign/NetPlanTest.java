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
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.junit.*;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.net2plan.utils.Constants.RoutingType;
import com.net2plan.utils.Pair;

import cern.colt.matrix.tdouble.DoubleFactory2D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import org.junit.rules.TemporaryFolder;

public class NetPlanTest
{
	private NetPlan netTriangle = null;
	private Node netTriangle_n1,  netTriangle_n2, netTriangle_n3;
	private Resource netTriangle_r1,  netTriangle_r2, netTriangle_r3;
	private Link netTriangle_e12,  netTriangle_e21, netTriangle_e13 , netTriangle_e31 , netTriangle_e23 , netTriangle_e32;
	private Demand netTriangle_d12,  netTriangle_d21, netTriangle_d13 , netTriangle_d31 , netTriangle_d23 , netTriangle_d32;

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
		this.lowerLayer = np.getNetworkLayerDefault();
		lowerLayer.addTag("t1");
		np.setDemandTrafficUnitsName("Mbps" , lowerLayer);
		this.upperLayer = np.addLayer("upperLayer" , "description" , "Mbps" , "upperTrafficUnits" , new URL ("file:/upperIcon") , null);
		upperLayer.addTag("t1");
		this.n1 = this.np.addNode(0 , 0 , "node1" , null);
		n1.addTag("t1");
		this.n2 = np.addNode(0 , 0 , "node2" , null);
		n1.setPopulation(200);
		this.n3 = np.addNode(0 , 0 , "node3" , null);
		n1.setPopulation(100);
		n1.setAttribute("att" , "1");
		n1.setSiteName("s12");
		n2.setSiteName("s12");
		n3.setSiteName("s3");
		this.n1.setUrlNodeIcon(lowerLayer , new URL ("file:/lowerIcon"));
		this.link12 = np.addLink(n1,n2,100,100,1,null,lowerLayer);
		this.link23 = np.addLink(n2,n3,100,100,1,null,lowerLayer);
		this.link13 = np.addLink(n1,n3,100,100,1,null,lowerLayer);
		link12.addTag("t1");
		this.d13 = np.addDemand(n1 , n3 , 3 , null,lowerLayer);
		d13.addTag("t1"); d13.addTag("t2");
		d13.setIntendedRecoveryType(Demand.IntendedRecoveryType.NONE);
		this.d12 = np.addDemand(n1, n2, 3 , null,lowerLayer);
		d12.setIntendedRecoveryType(Demand.IntendedRecoveryType.PROTECTION_NOREVERT);
		this.r12 = np.addRoute(d12,1,1.5,Collections.singletonList(link12),null);
		r12.addTag("t1"); r12.addTag("t3");
		np.addTag("t1");
		this.path13 = new LinkedList<Link> (); path13.add(link12); path13.add(link23);
		this.r123a = np.addRoute(d13,1,1.5,path13,null);
		this.r123b = np.addRoute(d13,1,1.5,path13,null);
		this.res2 = np.addResource("type" , "name" , n2 , 100 , "Mbps" , null , 10 , null);
		res2.addTag("t1");
		this.res2backup = np.addResource("type" , "name" , n2 , 100 , "Mbps" , null , 10 , null);
		this.scd123 = np.addDemand(n1 , n3 , 3 , null,lowerLayer);
		this.scd123.setServiceChainSequenceOfTraversedResourceTypes(Collections.singletonList("type"));
		this.pathSc123 = Arrays.asList(link12 ,res2 , link23);
		this.sc123 = np.addServiceChain(scd123 , 100 , Arrays.asList(300.0 , 50.0 , 302.0) , pathSc123 , null);
		sc123.addTag("t1");
		this.segm13 = np.addRoute(d13 , 0 , 50 , Collections.singletonList(link13) , null);
		this.r123a.addBackupRoute(segm13);
		this.upperLink12 = np.addLink(n1,n2,10,100,1,null,upperLayer);
		this.d12.coupleToUpperLayerLink(upperLink12);
		this.line123 = new HashSet<Link> (Arrays.asList(link12, link23));
		this.star = new HashSet<Link> (Arrays.asList(link12, link13));
		this.endNodes = new HashSet<Node> (Arrays.asList(n2,n3));
		this.d123 = np.addMulticastDemand(n1 , endNodes , 100 , null , lowerLayer);
		d123.addTag("t1");
		this.t123 = np.addMulticastTree(d123 , 10,15,line123,null);
		t123.addTag("t1");
		this.tStar = np.addMulticastTree(d123 , 10,15,star,null);
		this.upperMdLink12 = np.addLink(n1,n2,10,100,1,null,upperLayer);
		this.upperMdLink13 = np.addLink(n1,n3,10,100,1,null,upperLayer);
		this.upperMd123 = np.addMulticastDemand (n1 , endNodes , 100 , null , upperLayer);
		this.upperMt123 = np.addMulticastTree (upperMd123 , 10 , 15 , new HashSet<Link> (Arrays.asList(upperMdLink12 , upperMdLink13)) , null);
		d123.couple(new HashSet<Link> (Arrays.asList(upperMdLink12 , upperMdLink13)));
		np.addGlobalPlanningDomain("pd1");
		np.addGlobalPlanningDomain("pd2");
		n1.addToPlanningDomain("pd1");
		
		/* Triangle link cap 100, length 1, demands offered 1 */
		this.netTriangle = new NetPlan ();
		this.netTriangle.addGlobalPlanningDomain("new");
		this.netTriangle_n1 = this.netTriangle.addNode(0 , 0 , "node1" , null);
		this.netTriangle_n2 = this.netTriangle.addNode(0 , 0 , "node2" , null);
		this.netTriangle_n3 = this.netTriangle.addNode(0 , 0 , "node3" , null);
		this.netTriangle_e12 = this.netTriangle.addLink(netTriangle_n1,netTriangle_n2,100,1,1,null);
		this.netTriangle_e21 = this.netTriangle.addLink(netTriangle_n2,netTriangle_n1,100,1,1,null);
		this.netTriangle_e13 = this.netTriangle.addLink(netTriangle_n1,netTriangle_n3,100,1,1,null);
		this.netTriangle_e31 = this.netTriangle.addLink(netTriangle_n3,netTriangle_n1,100,1,1,null);
		this.netTriangle_e23 = this.netTriangle.addLink(netTriangle_n2,netTriangle_n3,100,1,1,null);
		this.netTriangle_e32 = this.netTriangle.addLink(netTriangle_n3,netTriangle_n2,100,1,1,null);
		this.netTriangle_d12 = this.netTriangle.addDemand(netTriangle_n1,netTriangle_n2,1,null);
		this.netTriangle_d21 = this.netTriangle.addDemand(netTriangle_n2,netTriangle_n1,1,null);
		this.netTriangle_d13 = this.netTriangle.addDemand(netTriangle_n1,netTriangle_n3,1,null);
		this.netTriangle_d31 = this.netTriangle.addDemand(netTriangle_n3,netTriangle_n1,1,null);
		this.netTriangle_d23 = this.netTriangle.addDemand(netTriangle_n2,netTriangle_n3,1,null);
		this.netTriangle_d32 = this.netTriangle.addDemand(netTriangle_n3,netTriangle_n2,1,null);
		this.netTriangle_r1 = netTriangle.addResource("type1" , "name" , netTriangle_n1 , 100.0 , "units" , null , 1.0 , null);
		this.netTriangle_r2 = netTriangle.addResource("type2" , "name" , netTriangle_n2 , 100.0 , "units" , null , 1.0 , null);
		this.netTriangle_r3 = netTriangle.addResource("type3" , "name" , netTriangle_n3 , 100.0 , "units" , null , 1.0 , null);

		temporaryFolder.create();

		File resourcesDir = temporaryFolder.getRoot();
		if (!resourcesDir.exists()) resourcesDir.mkdirs();
	}

	@After
	public void tearDown() throws Exception
	{
		np.checkCachesConsistency();

		temporaryFolder.delete();
	}

	@Test
	public void testNetPlan()
	{
		this.np = new NetPlan ();
	}

	@Test
	public void testGetSiteNames ()
	{
		assertEquals(np.getSiteNames() ,  Sets.newHashSet("s12", "s3"));
		n3.setSiteName(null);
		assertEquals(np.getSiteNames() ,  Sets.newHashSet("s12"));
		n2.setSiteName(null);
		assertEquals(np.getSiteNames() ,  Sets.newHashSet("s12"));
	}

	@Test
	public void testGetSiteNodes ()
	{
		assertEquals(np.getSiteNames() ,  Sets.newHashSet("s12" , "s3"));
		assertEquals(np.getSiteNodes("s12") ,  Sets.newHashSet(n1 , n2));
		assertEquals(np.getSiteNodes("s3") ,  Sets.newHashSet(n3));
		assertEquals(np.getSiteNodes("xxx") ,  Sets.newHashSet());
		n3.setSiteName(null);
		assertEquals(np.getSiteNames() ,  Sets.newHashSet("s12"));
		assertEquals(np.getSiteNodes("s3") ,  Sets.newHashSet());
		n2.setSiteName(null);
		assertEquals(np.getSiteNames() ,  Sets.newHashSet("s12"));
		assertEquals(np.getSiteNodes("s12") ,  Sets.newHashSet(n1));
		n1.setSiteName(null);
		assertEquals(np.getSiteNodes("s12") ,  Sets.newHashSet());
		assertEquals(np.getSiteNames() ,  Sets.newHashSet());
	}

	@Test
	public void testNetPlanFile() throws IOException
	{
		File f = temporaryFolder.newFile("temp.n2p");
		this.np.saveToFile(f);
		NetPlan readNp = new NetPlan (f);
		assertTrue(readNp.isDeepCopy(np));
		assertTrue(np.isDeepCopy(readNp));

		NetPlan np1 = new NetPlan (new File ("src/main/resources/data/networkTopologies/example7nodes_ipOverWDM.n2p"));
		np1.checkCachesConsistency();
		np1.saveToFile(f);
		NetPlan np2 = new NetPlan (f);
		np2.checkCachesConsistency();
		assertTrue (np1.isDeepCopy(np2));
		assertTrue (np2.isDeepCopy(np1));
	}

	@Test
	public void testGetIds()
	{
		assertEquals (NetPlan.getIds(Arrays.asList(link12 , n1)) , Arrays.asList(link12.getId() , n1.getId()));
	}

	@Test
	public void testGetIndexes()
	{
		assertEquals (NetPlan.getIndexes(Arrays.asList(link12 , n1)) , Arrays.asList(link12.getIndex() , n1.getIndex()));
	}

	@Test
	public void testAddDemand()
	{
		assertEquals(np.getDemands(lowerLayer) , Arrays.asList(d13, d12, scd123));
	}

	@Test
	public void testAddDemandBidirectional()
	{
		Pair<Demand,Demand> pair = np.addDemandBidirectional(n2,n3,10,null,upperLayer);
		assertEquals(pair.getFirst().getIngressNode() , n2);
		assertEquals(pair.getSecond().getIngressNode() , n3);
		assertEquals(pair.getFirst().getEgressNode() , n3);
		assertEquals(pair.getSecond().getEgressNode() , n2);
		assertEquals(np.getDemands(upperLayer) , Arrays.asList(pair.getFirst() , pair.getSecond()));
	}

	@Test
	public void testAddLayerStringStringStringStringMapOfStringString()
	{
		NetworkLayer layer = np.addLayer("name" , "description" , "linkCapUnits" , "demandCapUnits",null , null);
		assertEquals(np.getNumberOfLayers() , 3);
		assertEquals(np.getNetworkLayer("name") , layer);
	}

	@Test
	public void testAddLayerFrom()
	{
		np.addLayerFrom(upperLayer);
		assertEquals(np.getNumberOfLayers() , 3);
	}

	@Test
	public void testGetNodePairDemands()
	{
		assertEquals(np.getNodePairDemands (n1,n3,false,lowerLayer) , Sets.newHashSet(d13,scd123));
		assertEquals(np.getNodePairDemands (n1,n3,true,lowerLayer) , Sets.newHashSet(d13,scd123));
		d13.remove();
		assertEquals(np.getNodePairDemands (n1,n3,true,lowerLayer) , Sets.newHashSet(scd123));
	}

	@Test
	public void testGetNodePairRoutes()
	{
		assertEquals(np.getNodePairRoutes (n1,n3,false,lowerLayer) , Sets.newHashSet(r123a, r123b , segm13 , sc123));
		assertEquals(np.getNodePairRoutes (n1,n3,true,lowerLayer) , Sets.newHashSet(r123a, r123b , segm13 , sc123));
		r123a.remove();
		assertEquals(np.getNodePairRoutes (n1,n3,true,lowerLayer) , Sets.newHashSet(r123b , segm13 , sc123));
	}

	@Test
	public void testGetNodePairLinks()
	{
		assertEquals(np.getNodePairLinks (n1,n2,false,lowerLayer) , Sets.newHashSet(link12));
		assertEquals(np.getNodePairLinks (n1,n2,true,lowerLayer) , Sets.newHashSet(link12));
		link12.remove();
		assertEquals(np.getNodePairLinks (n1,n2,false,lowerLayer) , Sets.newHashSet());
	}

	@Test
	public void testAddLink()
	{
		Link newLinkUpper = np.addLink(n2,n3,10,11,12,null,upperLayer);
		assertEquals(newLinkUpper.getOriginNode() , n2);
		assertEquals(newLinkUpper.getDestinationNode() , n3);
		assertEquals(newLinkUpper.getCapacity() , 10.0 , 0);
		assertEquals(newLinkUpper.getLengthInKm() , 11.0 , 0);
		assertEquals(newLinkUpper.getPropagationSpeedInKmPerSecond() , 12.0 , 0);
		assertEquals(newLinkUpper.getLayer() , upperLayer);
	}

	@Test
	public void testAddLinkBidirectional()
	{
		Pair<Link,Link> pair = np.addLinkBidirectional(n2,n3,10,11,12,null,upperLayer);
		assertEquals(pair.getFirst().getOriginNode() , n2);
		assertEquals(pair.getSecond().getOriginNode() , n3);
		assertEquals(pair.getFirst().getDestinationNode() , n3);
		assertEquals(pair.getSecond().getDestinationNode() , n2);
	}

	@Test
	public void testAddMulticastDemand()
	{
		assertEquals(upperMd123.getIngressNode() , n1);
		assertEquals(upperMd123.getEgressNodes() , endNodes);
		assertEquals(upperMd123.getOfferedTraffic() , 100 , 0);
		assertEquals(upperMd123.getLayer() , upperLayer);
	}

	@Test
	public void testAddMulticastTree()
	{
		this.tStar = np.addMulticastTree(d123 , 10,15,star,null);
		assertEquals(tStar.getIngressNode() , n1);
		assertEquals(tStar.getEgressNodes() , endNodes);
		assertEquals(tStar.getCarriedTrafficInNoFailureState() , 10, 0);
		assertEquals(tStar.getOccupiedLinkCapacityInNoFailureState() , 15, 0);
		assertEquals(tStar.getLayer() , lowerLayer);
		assertEquals(tStar.getMulticastDemand() , d123);
	}

	@Test
	public void testAddNode()
	{
		this.n1 = this.np.addNode(0 , 0 , "node1" , null);
		assertEquals(n1.getXYPositionMap() , new Point2D.Double(0,0));
		assertEquals(n1.getName () , "node1");
	}

	@Test
	public void testAddResource()
	{
		this.res2 = np.addResource("type" , "name" , n2 , 100 , "Mbps" , null , 10 , null);
		assertEquals(res2.getType() , "type");
		assertEquals(res2.getName() , "name");
		assertEquals(res2.getHostNode() , n2);
		assertEquals(res2.getCapacity() , 100 , 0);
		assertEquals(res2.getCapacityMeasurementUnits() , "Mbps");
		assertEquals(res2.getCapacityOccupiedInBaseResourcesMap() , Collections.emptyMap ());
		assertEquals(res2.getProcessingTimeToTraversingTrafficInMs() , 10 , 0);
	}

	@Test
	public void testAddRoute()
	{
		assertEquals(r123a.getDemand() , d13);
		assertEquals(r123a.getCarriedTrafficInNoFailureState() , 1 , 0);
		assertEquals(r123a.getOccupiedCapacityInNoFailureState(link12) , 1.5 , 0);
		assertEquals(r123a.getOccupiedCapacityInNoFailureState(link23) , 1.5 , 0);
	}

	@Test
	public void testAddServiceChain()
	{
		assertEquals(sc123.getDemand() , scd123);
		assertEquals(sc123.getCarriedTrafficInNoFailureState() , 100 , 0);
		assertEquals(sc123.getOccupiedCapacityInNoFailureState(link12) , 300 , 0);
		assertEquals(sc123.getOccupiedCapacityInNoFailureState(res2) , 50 , 0);
		assertEquals(sc123.getOccupiedCapacityInNoFailureState(link23) , 302 , 0);
	}

	@Test
	public void testComputeUnicastCandidatePathList()
	{
		int K = 1;
		double maxLengthInKm = -1;
		int maxNumHops = -1;
		double maxPropDelayInMs = -1;
		double maxRouteCost = -1;
		double maxRouteCostFactorRespectToShortestPath = -1;
		double maxRouteCostRespectToShortestPath = -1;
		Map<Pair<Node,Node>,List<List<Link>>> cpl = netTriangle.computeUnicastCandidatePathList(null ,K, maxLengthInKm, maxNumHops, maxPropDelayInMs, maxRouteCost,maxRouteCostFactorRespectToShortestPath, maxRouteCostRespectToShortestPath , null);
		for (Demand d : netTriangle.getDemands())
			assertEquals(cpl.get(Pair.of(d.getIngressNode(),d.getEgressNode())) , Arrays.asList(Arrays.asList(netTriangle.getNodePairLinks(d.getIngressNode() , d.getEgressNode() , false).iterator().next())));

		K=2; cpl = netTriangle.computeUnicastCandidatePathList(null ,K, maxLengthInKm, maxNumHops, maxPropDelayInMs, maxRouteCost,maxRouteCostFactorRespectToShortestPath, maxRouteCostRespectToShortestPath , null);
		for (Demand d : netTriangle.getDemands())
		{
			final Set<Node> allNodes = new HashSet<Node> (netTriangle.getNodes());
			allNodes.removeAll(Arrays.asList(d.getIngressNode() , d.getEgressNode()));
			final Node intermNode = allNodes.iterator().next();
			final Link directLink = netTriangle.getNodePairLinks(d.getIngressNode(),d.getEgressNode(),false).iterator().next();
			final Link firstLink = netTriangle.getNodePairLinks(d.getIngressNode(),intermNode,false).iterator().next();
			final Link secondLink = netTriangle.getNodePairLinks(intermNode , d.getEgressNode(),false).iterator().next();
			assertEquals(cpl.get(Pair.of(d.getIngressNode(),d.getEgressNode())) , Arrays.asList(Arrays.asList(directLink) , Arrays.asList(firstLink,secondLink)));
		}
		K=3; cpl = netTriangle.computeUnicastCandidatePathList(null ,K, maxLengthInKm, maxNumHops, maxPropDelayInMs, maxRouteCost,maxRouteCostFactorRespectToShortestPath, maxRouteCostRespectToShortestPath , null);
		for (Demand d : netTriangle.getDemands())
		{
			final Set<Node> allNodes = new HashSet<Node> (netTriangle.getNodes());
			allNodes.removeAll(Arrays.asList(d.getIngressNode() , d.getEgressNode()));
			final Node intermNode = allNodes.iterator().next();
			final Link directLink = netTriangle.getNodePairLinks(d.getIngressNode(),d.getEgressNode(),false).iterator().next();
			final Link firstLink = netTriangle.getNodePairLinks(d.getIngressNode(),intermNode,false).iterator().next();
			final Link secondLink = netTriangle.getNodePairLinks(intermNode , d.getEgressNode(),false).iterator().next();
			assertEquals(cpl.get(Pair.of(d.getIngressNode(),d.getEgressNode())) , Arrays.asList(Arrays.asList(directLink) , Arrays.asList(firstLink,secondLink)));
		}

		K=3; maxLengthInKm = 1; cpl = netTriangle.computeUnicastCandidatePathList(null ,K, maxLengthInKm, maxNumHops, maxPropDelayInMs, maxRouteCost,maxRouteCostFactorRespectToShortestPath, maxRouteCostRespectToShortestPath , null);
		maxLengthInKm = -1;
		for (Demand d : netTriangle.getDemands())
			assertEquals(cpl.get(Pair.of(d.getIngressNode(),d.getEgressNode())) , Arrays.asList(Arrays.asList(netTriangle.getNodePairLinks(d.getIngressNode() , d.getEgressNode() , false).iterator().next())));
		K=3; maxLengthInKm = 0.5; cpl = netTriangle.computeUnicastCandidatePathList(null ,K, maxLengthInKm, maxNumHops, maxPropDelayInMs, maxRouteCost,maxRouteCostFactorRespectToShortestPath, maxRouteCostRespectToShortestPath , null);
		maxLengthInKm = -1;
		for (Demand d : netTriangle.getDemands())
			assertEquals(cpl.get(Pair.of(d.getIngressNode(),d.getEgressNode())) , Arrays.asList());

		K=3; maxNumHops = 1; cpl = netTriangle.computeUnicastCandidatePathList(null ,K, maxLengthInKm, maxNumHops, maxPropDelayInMs, maxRouteCost,maxRouteCostFactorRespectToShortestPath, maxRouteCostRespectToShortestPath , null);
		maxNumHops = -1;
		for (Demand d : netTriangle.getDemands())
			assertEquals(cpl.get(Pair.of(d.getIngressNode(),d.getEgressNode())) , Arrays.asList(Arrays.asList(netTriangle.getNodePairLinks(d.getIngressNode() , d.getEgressNode() , false).iterator().next())));

		K=3; maxPropDelayInMs = 1000; cpl = netTriangle.computeUnicastCandidatePathList(null ,K, maxLengthInKm, maxNumHops, maxPropDelayInMs, maxRouteCost,maxRouteCostFactorRespectToShortestPath, maxRouteCostRespectToShortestPath , null);
		maxPropDelayInMs = -1;
		for (Demand d : netTriangle.getDemands())
			assertEquals(cpl.get(Pair.of(d.getIngressNode(),d.getEgressNode())) , Arrays.asList(Arrays.asList(netTriangle.getNodePairLinks(d.getIngressNode() , d.getEgressNode() , false).iterator().next())));

		K=3; maxRouteCost = 1; cpl = netTriangle.computeUnicastCandidatePathList(null ,K, maxLengthInKm, maxNumHops, maxPropDelayInMs, maxRouteCost,maxRouteCostFactorRespectToShortestPath, maxRouteCostRespectToShortestPath , null);
		maxRouteCost = -1;
		for (Demand d : netTriangle.getDemands())
			assertEquals(cpl.get(Pair.of(d.getIngressNode(),d.getEgressNode())) , Arrays.asList(Arrays.asList(netTriangle.getNodePairLinks(d.getIngressNode() , d.getEgressNode() , false).iterator().next())));

		K=3; maxRouteCostFactorRespectToShortestPath = 0.1; cpl = netTriangle.computeUnicastCandidatePathList(null ,K, maxLengthInKm, maxNumHops, maxPropDelayInMs, maxRouteCost,maxRouteCostFactorRespectToShortestPath, maxRouteCostRespectToShortestPath , null);
		maxRouteCostFactorRespectToShortestPath = -1;
		for (Demand d : netTriangle.getDemands())
			assertEquals(cpl.get(Pair.of(d.getIngressNode(),d.getEgressNode())) , Arrays.asList(Arrays.asList(netTriangle.getNodePairLinks(d.getIngressNode() , d.getEgressNode() , false).iterator().next())));
		K=3; maxRouteCostFactorRespectToShortestPath = 2; cpl = netTriangle.computeUnicastCandidatePathList(null ,K, maxLengthInKm, maxNumHops, maxPropDelayInMs, maxRouteCost,maxRouteCostFactorRespectToShortestPath, maxRouteCostRespectToShortestPath , null);
		maxRouteCostFactorRespectToShortestPath = -1;
		for (Demand d : netTriangle.getDemands())
		{
			final Set<Node> allNodes = new HashSet<Node> (netTriangle.getNodes());
			allNodes.removeAll(Arrays.asList(d.getIngressNode() , d.getEgressNode()));
			final Node intermNode = allNodes.iterator().next();
			final Link directLink = netTriangle.getNodePairLinks(d.getIngressNode(),d.getEgressNode(),false).iterator().next();
			final Link firstLink = netTriangle.getNodePairLinks(d.getIngressNode(),intermNode,false).iterator().next();
			final Link secondLink = netTriangle.getNodePairLinks(intermNode , d.getEgressNode(),false).iterator().next();
			assertEquals(cpl.get(Pair.of(d.getIngressNode(),d.getEgressNode())) , Arrays.asList(Arrays.asList(directLink) , Arrays.asList(firstLink,secondLink)));
		}
		K=3; maxRouteCostRespectToShortestPath = 0.1; cpl = netTriangle.computeUnicastCandidatePathList(null ,K, maxLengthInKm, maxNumHops, maxPropDelayInMs, maxRouteCost,maxRouteCostFactorRespectToShortestPath, maxRouteCostRespectToShortestPath , null);
		maxRouteCostRespectToShortestPath = -1;
		for (Demand d : netTriangle.getDemands())
			assertEquals(cpl.get(Pair.of(d.getIngressNode(),d.getEgressNode())) , Arrays.asList(Arrays.asList(netTriangle.getNodePairLinks(d.getIngressNode() , d.getEgressNode() , false).iterator().next())));
	}

	@Test
	public void testComputeUnicastCandidate11PathList()
	{
		int K = 2;
		double maxLengthInKm = -1;
		int maxNumHops = -1;
		double maxPropDelayInMs = -1;
		double maxRouteCost = -1;
		double maxRouteCostFactorRespectToShortestPath = -1;
		double maxRouteCostRespectToShortestPath = -1;
		Map<Pair<Node,Node>,List<List<Link>>> cpl = netTriangle.computeUnicastCandidatePathList(null ,K, maxLengthInKm, maxNumHops, maxPropDelayInMs, maxRouteCost,maxRouteCostFactorRespectToShortestPath, maxRouteCostRespectToShortestPath , null);
		Map<Pair<Node,Node>,List<Pair<List<Link>,List<Link>>>> cpl11 = NetPlan.computeUnicastCandidate11PathList(cpl , 1);
		for (Demand d : netTriangle.getDemands())
		{
			final Set<Node> allNodes = new HashSet<Node> (netTriangle.getNodes());
			allNodes.removeAll(Arrays.asList(d.getIngressNode() , d.getEgressNode()));
			final Node intermNode = allNodes.iterator().next();
			final Link directLink = netTriangle.getNodePairLinks(d.getIngressNode(),d.getEgressNode(),false).iterator().next();
			final Link firstLink = netTriangle.getNodePairLinks(d.getIngressNode(),intermNode,false).iterator().next();
			final Link secondLink = netTriangle.getNodePairLinks(intermNode , d.getEgressNode(),false).iterator().next();
			assertEquals(cpl11.get(Pair.of(d.getIngressNode(),d.getEgressNode())) , Arrays.asList(Pair.of(Arrays.asList(directLink) , Arrays.asList(firstLink,secondLink))));
		}
		cpl11 = NetPlan.computeUnicastCandidate11PathList(cpl , 2);
		for (Demand d : netTriangle.getDemands())
		{
			final Set<Node> allNodes = new HashSet<Node> (netTriangle.getNodes());
			allNodes.removeAll(Arrays.asList(d.getIngressNode() , d.getEgressNode()));
			final Node intermNode = allNodes.iterator().next();
			final Link directLink = netTriangle.getNodePairLinks(d.getIngressNode(),d.getEgressNode(),false).iterator().next();
			final Link firstLink = netTriangle.getNodePairLinks(d.getIngressNode(),intermNode,false).iterator().next();
			final Link secondLink = netTriangle.getNodePairLinks(intermNode , d.getEgressNode(),false).iterator().next();
			assertEquals(cpl11.get(Pair.of(d.getIngressNode(),d.getEgressNode())) , Arrays.asList(Pair.of(Arrays.asList(directLink) , Arrays.asList(firstLink,secondLink))));
		}
		K = 1; cpl = netTriangle.computeUnicastCandidatePathList(null ,K, maxLengthInKm, maxNumHops, maxPropDelayInMs, maxRouteCost,maxRouteCostFactorRespectToShortestPath, maxRouteCostRespectToShortestPath , null);
		cpl11 = NetPlan.computeUnicastCandidate11PathList(cpl , 2);
		for (Demand d : netTriangle.getDemands())
			assertEquals(cpl11.get(Pair.of(d.getIngressNode(),d.getEgressNode())) , Arrays.asList());
	}

	@Test
	public void testComputeUnicastCandidate11ServiceChainList()
	{
		netTriangle_d12.setServiceChainSequenceOfTraversedResourceTypes(Arrays.asList("type2" , "type1"));
		Map<Demand,List<List<NetworkElement>>> cpl = netTriangle.computeUnicastCandidateServiceChainList (null , null , 3, -1 , -1, -1, -1);
		Map<Demand,List<Pair<List<NetworkElement>,List<NetworkElement>>>> cpl11 = NetPlan.computeUnicastCandidate11ServiceChainList(cpl , 1);
		assertEquals(cpl11.get(netTriangle_d12) , Arrays.asList());
	}

	@Test
	public void testComputeUnicastCandidateServiceChainList()
	{
		Map<Demand,List<List<NetworkElement>>> cpl = netTriangle.computeUnicastCandidateServiceChainList (null , null ,
				1, -1 , -1, -1, -1);
		for (Demand d : netTriangle.getDemands())
			assertEquals(cpl.get(d) , Arrays.asList(Arrays.asList(netTriangle.getNodePairLinks(d.getIngressNode() , d.getEgressNode() , false).iterator().next())));
		cpl = netTriangle.computeUnicastCandidateServiceChainList (null , null , 2, -1 , -1, -1, -1);
		for (Demand d : netTriangle.getDemands())
		{
			final Set<Node> allNodes = new HashSet<Node> (netTriangle.getNodes());
			allNodes.removeAll(Arrays.asList(d.getIngressNode() , d.getEgressNode()));
			final Node intermNode = allNodes.iterator().next();
			final Link directLink = netTriangle.getNodePairLinks(d.getIngressNode(),d.getEgressNode(),false).iterator().next();
			final Link firstLink = netTriangle.getNodePairLinks(d.getIngressNode(),intermNode,false).iterator().next();
			final Link secondLink = netTriangle.getNodePairLinks(intermNode , d.getEgressNode(),false).iterator().next();
			assertEquals(cpl.get(d) , Arrays.asList(Arrays.asList(directLink) , Arrays.asList(firstLink,secondLink)));
		}

		netTriangle_d12.setServiceChainSequenceOfTraversedResourceTypes(Arrays.asList("type1"));
		cpl = netTriangle.computeUnicastCandidateServiceChainList (null , null , 3, -1 , -1, -1, -1);
		assertEquals(cpl.get(netTriangle_d12) , Arrays.asList(Arrays.asList(netTriangle_r1 , netTriangle_e12)  ,  Arrays.asList(netTriangle_r1 , netTriangle_e13 , netTriangle_e32)));

		netTriangle_d12.setServiceChainSequenceOfTraversedResourceTypes(Arrays.asList("type1" , "type2"));
		cpl = netTriangle.computeUnicastCandidateServiceChainList (null , null , 3, -1 , -1, -1, -1);
		assertEquals(cpl.get(netTriangle_d12) , Arrays.asList(Arrays.asList(netTriangle_r1 , netTriangle_e12 , netTriangle_r2)  ,  Arrays.asList(netTriangle_r1 , netTriangle_e13 , netTriangle_e32 , netTriangle_r2)));

		netTriangle_d12.setServiceChainSequenceOfTraversedResourceTypes(Arrays.asList("type2" , "type1"));
		cpl = netTriangle.computeUnicastCandidateServiceChainList (null , null , 3, -1 , -1, -1, -1);

		assertEquals(cpl.get(netTriangle_d12) , Arrays.asList(
				Arrays.asList(netTriangle_e12 , netTriangle_r2 , netTriangle_e21 , netTriangle_r1 , netTriangle_e12)  ,
				Arrays.asList(netTriangle_e12 , netTriangle_r2 , netTriangle_e21 , netTriangle_r1 , netTriangle_e13 , netTriangle_e32) ,
				Arrays.asList(netTriangle_e12 , netTriangle_r2 , netTriangle_e23 , netTriangle_e31 , netTriangle_r1 , netTriangle_e12)));
	}

	@Test
	public void testComputeMulticastCandidatePathList()
	{
//	 * 		<li>{@code K}: Number of desired multicast trees per demand (default: 3). If <i>K'&lt;</i>{@code K} different trees are found for the multicast demand, then only <i>K'</i> are
//	 * 		included in the candidate list</li>
//	 * 		<li>{@code maxCopyCapability}: the maximum number of copies of an input traffic a node can make. Then, a node can have at most this number of ouput links carrying traffic of a multicast tree (default: Double.MAX_VALUE)</li>
//	 * 		<li>{@code maxE2ELengthInKm}: Maximum path length measured in kilometers allowed for any tree, from the origin node, to any destination node (default: Double.MAX_VALUE)</li>
//	 * 		<li>{@code maxE2ENumHops}: Maximum number of hops allowed for any tree, from the origin node, to any destination node (default: Integer.MAX_VALUE)</li>
//	 * 		<li>{@code maxE2EPropDelayInMs}: Maximum propagation delay in miliseconds allowed in a path, for any tree, from the origin node, to any destination node  (default: Double.MAX_VALUE)</li>
//	 * 		<li>{@code maxTreeCost}: Maximum tree weight allowed, summing the weights of the links (default: Double.MAX_VALUE)</li>
//	 * 		<li>{@code maxTreeCostFactorRespectToMinimumCostTree}: Trees with higher weight (cost) than the cost of the minimum cost tree, multiplied by this factor, are not returned (default: Double.MAX_VALUE)</li>
//	 *		<li>{@code maxTreeCostRespectToMinimumCostTree}: Trees with higher weight (cost) than the cost of the minimum cost tree, plus this factor, are not returned (default: Double.MAX_VALUE). While the previous one is a multiplicative factor, this one is an additive factor</li>

		try
		{
		    Map<MulticastDemand, List<Set<Link>>> cpl;
			MulticastDemand d123 = netTriangle.addMulticastDemand(netTriangle_n1,
					new HashSet<Node>(Arrays.asList(netTriangle_n2, netTriangle_n3)), 0, null);
			cpl = netTriangle.computeMulticastCandidatePathList(null, "cplex", "cplex.dll", -1, "K", "100");
			assertEquals(cpl.get(d123), Arrays.asList(
					new HashSet<Link>(Arrays.asList(netTriangle_e12, netTriangle_e13)),
					new HashSet<Link>(Arrays.asList(netTriangle_e12, netTriangle_e23)),
					new HashSet<Link>(Arrays.asList(netTriangle_e13, netTriangle_e32))));
			cpl = netTriangle.computeMulticastCandidatePathList(null, "cplex", "cplex.dll", -1, "K", "100", "maxCopyCapability", "1");
			assertEquals(cpl.get(d123), Arrays.asList(
					new HashSet<Link>(Arrays.asList(netTriangle_e12, netTriangle_e23)),
					new HashSet<Link>(Arrays.asList(netTriangle_e13, netTriangle_e32))));
			cpl = netTriangle.computeMulticastCandidatePathList(null, "cplex", "cplex.dll", -1, "K", "100", "maxE2ELengthInKm", "1");
			assertEquals(cpl.get(d123), Arrays.asList(
					new HashSet<Link>(Arrays.asList(netTriangle_e12, netTriangle_e13))));
			cpl = netTriangle.computeMulticastCandidatePathList(null, "cplex", "cplex.dll", -1, "K", "100", "maxE2ENumHops", "1");
			assertEquals(cpl.get(d123), Arrays.asList(
					new HashSet<Link>(Arrays.asList(netTriangle_e12, netTriangle_e13))));
			cpl = netTriangle.computeMulticastCandidatePathList(null, "cplex", "cplex.dll", -1, "K", "100", "maxE2EPropDelayInMs", "1000");
			assertEquals(cpl.get(d123), Arrays.asList(
					new HashSet<Link>(Arrays.asList(netTriangle_e12, netTriangle_e13))));
			cpl = netTriangle.computeMulticastCandidatePathList(null, "cplex", "cplex.dll", -1, "K", "100", "maxTreeCost", "1");
			assertEquals(cpl.get(d123), Arrays.asList());
			cpl = netTriangle.computeMulticastCandidatePathList(null, "cplex", "cplex.dll", -1, "K", "100", "maxTreeCostFactorRespectToMinimumCostTree", "1");
			assertEquals(cpl.get(d123), Arrays.asList(
					new HashSet<Link>(Arrays.asList(netTriangle_e12, netTriangle_e13)),
					new HashSet<Link>(Arrays.asList(netTriangle_e12, netTriangle_e23)),
					new HashSet<Link>(Arrays.asList(netTriangle_e13, netTriangle_e32))));
			cpl = netTriangle.computeMulticastCandidatePathList(null, "cplex", "cplex.dll", -1, "K", "100", "maxTreeCostRespectToMinimumCostTree", "0");
			assertEquals(cpl.get(d123), Arrays.asList(
					new HashSet<Link>(Arrays.asList(netTriangle_e12, netTriangle_e13)),
					new HashSet<Link>(Arrays.asList(netTriangle_e12, netTriangle_e23)),
					new HashSet<Link>(Arrays.asList(netTriangle_e13, netTriangle_e32))));
			netTriangle_e12.remove();
			cpl = netTriangle.computeMulticastCandidatePathList(null, "cplex", "cplex.dll", -1, "K", "100");
			assertEquals(cpl.get(d123), Arrays.asList(
					new HashSet<Link>(Arrays.asList(netTriangle_e13, netTriangle_e32))));
		} catch (UnsatisfiedLinkError e)
        {
            System.err.println(this.getClass().getName() + ": Could not find CPLEX solver, related tests will be ignored...");
        }
	}


	@Test
	public void testAddSRG()
	{
		SharedRiskGroup srg = np.addSRG(1,2,null);
		assertEquals(srg.getMeanTimeToFailInHours() , 1 , 0);
		assertEquals(srg.getMeanTimeToRepairInHours() , 2 , 0);
	}


	@Test
	public void testAssignFrom()
	{
		NetPlan np2 = np.copy();
		np.checkCachesConsistency();
		np2.checkCachesConsistency();
		assertTrue (np.isDeepCopy(np2));
		assertTrue (np2.isDeepCopy(np));
		np.assignFrom(np2);
		np.checkCachesConsistency();
		assertTrue (np.isDeepCopy(np2));
		assertTrue (np2.isDeepCopy(np));
	}

	@Test
	public void testCopy()
	{
		NetPlan np2 = np.copy();
		assertTrue (np.isDeepCopy(np2));
		assertTrue (np2.isDeepCopy(np));
	}

	@Test
	public void testCopyFrom()
	{
		NetPlan np2 = np.copy();
		np.copyFrom(np2);
		assertTrue (np.isDeepCopy(np2));
		assertTrue (np2.isDeepCopy(np));
	}

	@Test
	public void testGetAttributesCollectionOfQextendsNetworkElementString()
	{
		n1.setAttribute("att" , "1");
		n2.setAttribute("att" , "2");
		assertEquals (NetPlan.getAttributes(Arrays.asList(n1,n2) , "att") , ImmutableMap.of(n1,"1",n2,"2"));
	}

	@Test
	public void testGetAttributeValues()
	{
		n1.setAttribute("att" , "1");
		n2.setAttribute("att" , "2");
		assertTrue (Arrays.equals(NetPlan.getAttributeValues(Arrays.asList(n1,n2,n3) , "att" , 7).toArray() , new double [] {1.0 , 2.0 , 7.0}));
	}

	@Test
	public void testGetDemandTrafficUnitsName()
	{
		assertEquals(np.getDemandTrafficUnitsName(lowerLayer) , "Mbps");
	}

	@Test
	public void testGetLinkCapacityUnitsName()
	{
		np.setLinkCapacityUnitsName("name" , lowerLayer);
		assertEquals(np.getLinkCapacityUnitsName(lowerLayer) , "name");
	}

	@Test
	public void testGetNetworkDescription()
	{
		np.setNetworkDescription("bla");
		assertEquals(np.getNetworkDescription() , "bla");
	}

	@Test
	public void testGetNetworkElementByAttribute()
	{
		n1.setAttribute("att" , "1");
		n2.setAttribute("att" , "2");
		assertEquals (NetPlan.getNetworkElementByAttribute(Arrays.asList(n1,n2,n3) , "att" , "2") , n2);
	}

	@Test
	public void testGetNetworkElementsByAttribute()
	{
		n1.setAttribute("att" , "1");
		n2.setAttribute("att" , "2");
		n3.setAttribute("att" , "2");
		assertEquals (NetPlan.getNetworkElementsByAttribute(Arrays.asList(n1,n2,n3) , "att" , "2") , Arrays.asList(n2,n3));
	}

	@Test
	public void testGetNetworkName()
	{
		np.setNetworkName("bla");
		assertEquals (np.getNetworkName() , "bla");
	}

	@Test
	public void testGetNodeByName()
	{
		n1.setName("1");
		n2.setName("1");
		n3.setName("3");
		assertEquals (np.getNodeByName("1") , n1);
	}

	@Test
	public void testGetNumberOfNodePairs()
	{
		assertEquals (np.getNumberOfNodePairs() , 2*3);
	}

	@Test
	public void testComputeRouteCostVector()
	{
		assertTrue (Arrays.equals(np.computeRouteCostVector(null).toArray(), new double [] {1,2,2,2,1}));
	}

	@Test
	public void testComputeMulticastTreeCostVector()
	{
		assertTrue (Arrays.equals(np.computeMulticastTreeCostVector(null).toArray(), new double [] {2,2}));
	}

	@Test
	public void testHasUnicastRoutingLoops()
	{
		assertTrue(!np.hasUnicastRoutingLoops());
		Link link21 = np.addLink(n2,n1,0,0,1,null);
		np.addRoute(d13 , 0 , 0 , Arrays.asList(link12, link21, link13) , null);
		assertTrue(np.hasUnicastRoutingLoops());
	}

	@Test
	public void testIsUp()
	{
		assertTrue(np.isUp(Arrays.asList(n1,n2,link12)));
		n1.setFailureState(false);
		assertTrue(!np.isUp(Arrays.asList(n2,link12)));
	}

	@Test
	public void testRemoveNetworkLayer()
	{
		np.removeNetworkLayer(lowerLayer);
		assertEquals(np.getNumberOfLayers() , 1);
		try { np.removeNetworkLayer(upperLayer); fail (); } catch (Exception e) {}
	}

	@Test
	public void testRemoveAllDemands()
	{
		np.removeAllDemands(lowerLayer);
		assertEquals(np.getNumberOfDemands(lowerLayer) , 0);
	}

	@Test
	public void testRemoveAllForwardingRules()
	{
		try { np.removeAllForwardingRules(upperLayer); fail (); } catch (Exception e) {}
		np.setRoutingType(RoutingType.HOP_BY_HOP_ROUTING , upperLayer);
		np.removeAllForwardingRules(upperLayer);
		assertEquals(np.getNumberOfForwardingRules(upperLayer) , 0);
	}

	@Test
	public void testRemoveAllLinks()
	{
		np.removeAllLinks(lowerLayer);
		assertEquals(np.getNumberOfLinks(lowerLayer) , 0);
	}

	@Test
	public void testRemoveAllMulticastDemands()
	{
		np.removeAllMulticastDemands(lowerLayer);
		assertEquals(np.getNumberOfMulticastDemands(lowerLayer) , 0);
	}

	@Test
	public void testRemoveAllMulticastTrees()
	{
		np.removeAllMulticastTrees(lowerLayer);
		assertEquals(np.getNumberOfMulticastTrees(lowerLayer) , 0);
	}

	@Test
	public void testRemoveAllMulticastTreesUnused()
	{
		np.removeAllMulticastTreesUnused(0.1 , lowerLayer);
		assertEquals(np.getNumberOfMulticastTrees(lowerLayer) , 2);
	}

	@Test
	public void testRemoveAllNetworkLayers()
	{
		np.removeAllNetworkLayers();
		assertEquals(np.getNumberOfLayers() , 1);
	}

	@Test
	public void testRemoveAllNodes()
	{
		np.removeAllNodes();
		assertEquals(np.getNumberOfNodes() , 0);
	}

	@Test
	public void testRemoveAllRoutes()
	{
		np.removeAllRoutes();
		assertEquals(np.getNumberOfRoutes() , 0);
	}

	@Test
	public void testRemoveAllRoutesUnused()
	{
		np.removeAllRoutesUnused(0.1 , lowerLayer);
		assertEquals(np.getNumberOfRoutes() , 5);
	}

	@Test
	public void testRemoveAllLinksUnused()
	{
		np.removeAllLinksUnused(0.1 , lowerLayer);
		assertEquals(np.getNumberOfLinks () , 3);
		link12.setCapacity(0);
		np.removeAllLinksUnused(0.1 , lowerLayer);
		assertEquals(np.getNumberOfLinks () , 2);
	}

	@Test
	public void testRemoveAllUnicastRoutingInformation()
	{
		np.removeAllUnicastRoutingInformation(lowerLayer);
		assertEquals(np.getNumberOfLinks (lowerLayer) , 3);
		assertEquals(np.getNumberOfRoutes (lowerLayer) , 0);
		assertEquals(np.getNumberOfMulticastTrees(lowerLayer) , 2);
	}

	@Test
	public void testRemoveAllSRGs()
	{
		np.addSRG(1,2,null);
		assertEquals(np.getNumberOfSRGs() , 1);
		np.removeAllSRGs();
		assertEquals(np.getNumberOfSRGs() , 0);
	}

	@Test
	public void testRemoveAllResources()
	{
		assertEquals(np.getNumberOfResources() , 2);
		np.removeAllResources();
		assertEquals(np.getNumberOfResources() , 0);
	}

	@Test
	public void testReset()
	{
		np.reset();
		NetPlan np2 = new NetPlan ();
		assertTrue (np2.isDeepCopy(np));
		assertTrue (np.isDeepCopy(np2));
	}

//	/* already in the NetPlan constructor test*/
//	@Test
//	public void testSaveToFile()
//	{
//	}

	@Test
	public void testSetAllLinksFailureState()
	{
		np.setAllLinksFailureState(false, lowerLayer);
		for (Link e : np.getLinks(lowerLayer))
			assertTrue (e.isDown());
		np.setAllLinksFailureState(true, lowerLayer);
		for (Link e : np.getLinks(lowerLayer))
			assertTrue (e.isUp());
	}

	@Test
	public void testSetAllNodesFailureState()
	{
		np.setAllNodesFailureState(false);
		for (Node n : np.getNodes())
			assertTrue (n.isDown());
		np.setAllNodesFailureState(true);
		for (Node n : np.getNodes())
			assertTrue (n.isUp());
	}

	@Test
	public void testSetLinksAndNodesFailureState()
	{
		try { np.setLinksAndNodesFailureState(Arrays.asList(link12) , Arrays.asList(link12 , link13 , upperLink12), null, Arrays.asList(n1,n2)); fail (); } catch (Exception e) {}
		np.setLinksAndNodesFailureState(null , Arrays.asList(link12 , link13 , upperLink12), null, Arrays.asList(n1,n2));
		assertTrue (!n1.isUp());
		assertTrue (!n2.isUp());
		assertTrue (n3.isUp());
		assertTrue (!link12.isUp());
		assertTrue (!link13.isUp());
		assertTrue (!upperLink12.isUp());
		np.setLinksAndNodesFailureState(Arrays.asList(link12 , link13 , upperLink12), null , Arrays.asList(n1,n2) , null);
		assertTrue (n1.isUp());
		assertTrue (n2.isUp());
		assertTrue (n3.isUp());
		assertTrue (link12.isUp());
		assertTrue (link13.isUp());
		assertTrue (upperLink12.isUp());
	}

	@Test
	public void testSetDemandTrafficUnitsName()
	{
		try { np.setDemandTrafficUnitsName("bla", lowerLayer); fail (); } catch (Exception e) {}
		np.setDemandTrafficUnitsName("bla", upperLayer);
		assertEquals(np.getDemandTrafficUnitsName(upperLayer) , "bla");
	}

	@Test
	public void testSetForwardingRule()
	{
		sc123.remove();
		scd123.setServiceChainSequenceOfTraversedResourceTypes(null);
		np.setRoutingType(RoutingType.HOP_BY_HOP_ROUTING , lowerLayer);
		np.checkCachesConsistency();
		np.setForwardingRule(d12, link12 , 0.7);
		np.checkCachesConsistency();
		assertEquals(np.getForwardingRuleSplittingFactor(d12,   link12) , 0.7 , 0);
		try { np.setForwardingRule(d12, link13 , 0.7); fail (); } catch (Exception e) {}
	}

	@Test
	public void testSetForwardingRulesCollectionOfDemandCollectionOfLinkCollectionOfDoubleBoolean()
	{
		sc123.remove();
		scd123.setServiceChainSequenceOfTraversedResourceTypes(null);
		np.setRoutingType(RoutingType.HOP_BY_HOP_ROUTING , lowerLayer);
		np.setForwardingRules(Arrays.asList(d12 , d12), Arrays.asList(link12 , link13), Arrays.asList(0.7 , 0.1), true);
		assertEquals(np.getForwardingRuleSplittingFactor(d12,   link12) , 0.7 , 0);
		assertEquals(np.getForwardingRuleSplittingFactor(d12,   link13) , 0.1 , 0);
	}

	@Test
	public void testSetForwardingRulesDoubleMatrix2DNetworkLayerArray()
	{
		sc123.remove();
		scd123.setServiceChainSequenceOfTraversedResourceTypes(null);
		np.setRoutingType(RoutingType.HOP_BY_HOP_ROUTING , lowerLayer);
		DoubleMatrix2D f_de = np.getMatrixDemandBasedForwardingRules(lowerLayer);
		f_de.set(d12.getIndex(), link12.getIndex(), 0.7);
		np.setForwardingRules(f_de , lowerLayer);
		assertEquals(np.getForwardingRuleSplittingFactor(d12,   link12) , 0.7 , 0);
	}

	@Test
	public void testSetLinkCapacityUnitsName()
	{
		try { np.setLinkCapacityUnitsName("bla", upperLayer); fail (); } catch (Exception e) {}
		np.setLinkCapacityUnitsName("bla", lowerLayer);
		assertEquals(np.getLinkCapacityUnitsName(lowerLayer) , "bla");
	}

	@Test
	public void testSetNetworkDescription()
	{
		np.setNetworkDescription("bla");
		assertEquals(np.getNetworkDescription() , "bla");
	}

	@Test
	public void testSetAttributeAsXXX ()
	{
		n1.setAttribute("a", 1.5);
		assertEquals(n1.getAttributeAsDouble("a", null) , 1.5 , 0);
		assertEquals(n1.getAttributeAsDouble("b", null) , null);
		assertEquals(n1.getAttributeAsDouble("b", 7.0) , 7 , 0);
		n1.setAttribute("a", 2);
		assertEquals(n1.getAttributeAsDouble("a", null) , 2 , 0);
		
		n1.setAttributeAsNumberList("a", Arrays.asList(1.2 , 1.3));
		assertEquals(n1.getAttributeAsDoubleList("a", null) , Arrays.asList(1.2 , 1.3));
		n1.setAttributeAsNumberList("a", Arrays.asList());
		assertEquals(n1.getAttributeAsDoubleList("a", null) , Arrays.asList());
		n1.setAttributeAsNumberList("a", Arrays.asList(1.2));
		assertEquals(n1.getAttributeAsDoubleList("a", null) , Arrays.asList(1.2));
		
		double vals [][] = new double [] [] { {1 ,2 ,3.5 } , { 4 ,5 ,6.2} };
		DoubleMatrix2D valsMatrix = DoubleFactory2D.dense.make(vals);
		n1.setAttributeAsNumberMatrix("a", valsMatrix);
		assertEquals(n1.getAttributeAsDoubleMatrix("a", null) , valsMatrix);
		valsMatrix = DoubleFactory2D.dense.make(0,0);
		n1.setAttributeAsNumberMatrix("a", valsMatrix);
		assertEquals(n1.getAttributeAsDoubleMatrix("a", null) , valsMatrix);
		valsMatrix = DoubleFactory2D.dense.make(1,2);
		n1.setAttributeAsNumberMatrix("a", valsMatrix);
		assertEquals(n1.getAttributeAsDoubleMatrix("a", null) , valsMatrix);
		valsMatrix = DoubleFactory2D.dense.make(2,1);
		n1.setAttributeAsNumberMatrix("a", valsMatrix);
		assertEquals(n1.getAttributeAsDoubleMatrix("a", null) , valsMatrix);
		
		String original = "1";
		n1.setAttributeAsStringList("a", Arrays.asList(original , original , original));
		assertEquals(n1.getAttributeAsStringList("a", null) , Arrays.asList(original , original , original));
		n1.setAttributeAsStringMatrix("a", Arrays.asList(Arrays.asList(original , original) , Arrays.asList(original , original)));
		assertEquals(n1.getAttributeAsStringMatrix("a", null) , Arrays.asList(Arrays.asList(original , original) , Arrays.asList(original , original)));
		n1.setAttributeAsStringMatrix("a", Arrays.asList(Arrays.asList(original , original)));
		assertEquals(n1.getAttributeAsStringMatrix("a", null) , Arrays.asList(Arrays.asList(original , original)));
		n1.setAttributeAsStringMatrix("a", Arrays.asList(Arrays.asList(original) , Arrays.asList(original)));
		assertEquals(n1.getAttributeAsStringMatrix("a", null) , Arrays.asList(Arrays.asList(original) , Arrays.asList(original)));

		original = ">A>>B 3; >E>>aaA>Aas>>2: ; ; ; 223>s>; > ;";
		n1.setAttributeAsStringList("a", Arrays.asList(original , original , original));
		assertEquals(n1.getAttributeAsStringList("a", null) , Arrays.asList(original , original , original));
		n1.setAttributeAsStringMatrix("a", Arrays.asList(Arrays.asList(original , original) , Arrays.asList(original , original)));
		assertEquals(n1.getAttributeAsStringMatrix("a", null) , Arrays.asList(Arrays.asList(original , original) , Arrays.asList(original , original)));
		n1.setAttributeAsStringMatrix("a", Arrays.asList(Arrays.asList(original , original)));
		assertEquals(n1.getAttributeAsStringMatrix("a", null) , Arrays.asList(Arrays.asList(original , original)));
		n1.setAttributeAsStringMatrix("a", Arrays.asList(Arrays.asList(original) , Arrays.asList(original)));
		assertEquals(n1.getAttributeAsStringMatrix("a", null) , Arrays.asList(Arrays.asList(original) , Arrays.asList(original)));

		original = "";
		n1.setAttributeAsStringList("a", Arrays.asList(original , original , original));
		assertEquals(n1.getAttributeAsStringList("a", null) , Arrays.asList(original , original , original));
		n1.setAttributeAsStringMatrix("a", Arrays.asList(Arrays.asList(original , original) , Arrays.asList(original , original)));
		assertEquals(n1.getAttributeAsStringMatrix("a", null) , Arrays.asList(Arrays.asList(original , original) , Arrays.asList(original , original)));
		n1.setAttributeAsStringMatrix("a", Arrays.asList(Arrays.asList(original , original)));
		assertEquals(n1.getAttributeAsStringMatrix("a", null) , Arrays.asList(Arrays.asList(original , original)));
		n1.setAttributeAsStringMatrix("a", Arrays.asList(Arrays.asList(original) , Arrays.asList(original)));
		assertEquals(n1.getAttributeAsStringMatrix("a", null) , Arrays.asList(Arrays.asList(original) , Arrays.asList(original)));
}

	
	@Test
	public void testSetNetworkLayerDefault()
	{
		np.setNetworkLayerDefault(upperLayer);
		assertEquals(np.getNetworkLayerDefault() , upperLayer);
	}

	@Test
	public void testSetNetworkName()
	{
		np.setNetworkName("bla");
		assertEquals(np.getNetworkName() , "bla");
	}

	@Test
	public void testSetRoutingType()
	{
		sc123.remove();
		scd123.setServiceChainSequenceOfTraversedResourceTypes(null);
		r12.setCarriedTraffic(1,1);
		r123a.setCarriedTraffic(2,2);
		r123b.setCarriedTraffic(3,3);
		d13.setOfferedTraffic(5);
		d12.setOfferedTraffic(1);
		NetPlan npSR = np.copy();
		np.setRoutingType(RoutingType.HOP_BY_HOP_ROUTING , lowerLayer);
		NetPlan npHR = np.copy ();
		assertEquals (np.getRoutingType(lowerLayer) , RoutingType.HOP_BY_HOP_ROUTING);
		np.setRoutingType(RoutingType.SOURCE_ROUTING , lowerLayer);
		assertEquals (np.getRoutingType(lowerLayer) , RoutingType.SOURCE_ROUTING);
		assertTrue (npSR.getVectorLinkCarriedTraffic(npSR.getNetworkLayer(lowerLayer.getIndex())).equals(np.getVectorLinkCarriedTraffic(lowerLayer)));
		assertTrue (npSR.getVectorLinkCarriedTraffic(npSR.getNetworkLayer(upperLayer.getIndex())).equals(np.getVectorLinkCarriedTraffic(upperLayer)));
		assertTrue (npHR.getVectorLinkCarriedTraffic(npHR.getNetworkLayer(lowerLayer.getIndex())).equals(np.getVectorLinkCarriedTraffic(lowerLayer)));
		assertTrue (npHR.getVectorLinkCarriedTraffic(npHR.getNetworkLayer(upperLayer.getIndex())).equals(np.getVectorLinkCarriedTraffic(upperLayer)));
	}

	@Test
	public void testSetTrafficMatrix()
	{
		DoubleMatrix2D m = DoubleFactory2D.dense.make(np.getNumberOfNodes() , np.getNumberOfNodes() , 3.0);
		np.setTrafficMatrix(m, lowerLayer);
		for (Demand d : np.getDemands (lowerLayer))
			assertEquals (d.getOfferedTraffic() , 3.0 , 0);
	}

	@Test
	public void testAddPlanningDomain ()
	{
		assertEquals (np.getGlobalPlanningDomains() , Sets.newHashSet("pd1","pd2"));
		np.addGlobalPlanningDomain("new1");
		np.addGlobalPlanningDomain("new2");
		assertEquals (np.getGlobalPlanningDomains() , Sets.newHashSet("pd1","pd2","new1" , "new2"));

		try { n1.addToPlanningDomain("xx"); fail (); } catch (Exception e) {}
		n1.addToPlanningDomain("new1");
		n2.addToPlanningDomain("new1");
		assertEquals(np.getGlobalPlanningDomainNodes("new1"), Sets.newHashSet(n1,n2));
		assertEquals(np.getGlobalPlanningDomainNodes("new2"), Sets.newHashSet());
		assertEquals(np.getGlobalPlanningDomainNodes("aaa"), null);
		try { np.removeGlobalPlanningDomain("new1"); fail (); } catch (Exception e) {}
		np.removeGlobalPlanningDomain("new2"); 
		assertEquals(np.getGlobalPlanningDomainNodes("new1"), Sets.newHashSet(n1,n2));
		assertEquals (np.getGlobalPlanningDomains() , Sets.newHashSet("pd1","pd2","new1"));
	}
		
	@Test
	public void testMerge ()
	{
		/* Merge with a copy of itself */
		NetPlan restricted = np.copy();
		NetPlan merged = np.copy().mergeIntoThisDesign(restricted);
		checkEqual(np, merged);
		
		restricted = np.copy();
		restricted.restrictDesign(Sets.newHashSet(restricted.getNodeFromId(n1.getId())));
		merged = np.copy().mergeIntoThisDesign(restricted);
		checkEqual(np, merged);

		restricted = np.copy();
		restricted.restrictDesign(Sets.newHashSet(restricted.getNodeFromId(n1.getId()),restricted.getNodeFromId(n2.getId())));
		merged = np.copy().mergeIntoThisDesign(restricted);
		checkEqual(np, merged);

		restricted = np.copy();
		restricted.restrictDesign(Sets.newHashSet(restricted.getNodeFromId(n1.getId()),restricted.getNodeFromId(n3.getId())));
		merged = np.copy().mergeIntoThisDesign(restricted);
		checkEqual(np, merged);

		restricted = np.copy();
		restricted.restrictDesign(Sets.newHashSet(restricted.getNodeFromId(n2.getId()),restricted.getNodeFromId(n2.getId())));
		merged = np.copy().mergeIntoThisDesign(restricted);
		checkEqual(np, merged);


		restricted = np.copy();
		restricted.restrictDesign(Sets.newHashSet(restricted.getNodeFromId(n1.getId()),restricted.getNodeFromId(n2.getId()) , restricted.getNodeFromId(n3.getId())));
		merged = np.copy().mergeIntoThisDesign(restricted);
		checkEqual(np, merged);
		
	}
	
	@Test
	public void testRestrictCopy ()
	{
		NetPlan np = new NetPlan ();
		final NetworkLayer lowerLayer = np.getNetworkLayerDefault();
		final NetworkLayer upperLayer = np.addLayer("", "", "", "", null, null);
		final Node n1 = np.addNode(0, 0, "", null); final long idn1 = n1.getId();
		final Node n2 = np.addNode(0, 0, "", null); final long idn2 = n2.getId();
		final Node n3 = np.addNode(0, 0, "", null); final long idn3 = n3.getId();
		final Node n4 = np.addNode(0, 0, "", null); final long idn4 = n4.getId();
		final Link low12 = np.addLink(n1, n2, 0, 0, 200000, null , lowerLayer);
		final Link low23 = np.addLink(n2, n3, 0, 0, 200000, null , lowerLayer);
		final Demand dlow12 = np.addDemand(n1, n2, 0, null, lowerLayer);
		
		NetPlan np2 = np.copy();
		np2.restrictDesign(Sets.newHashSet(np2.getNodeFromId(idn1),np2.getNodeFromId(idn2),np2.getNodeFromId(idn3),np2.getNodeFromId(idn4)));
		assertEquals(np2.getAllIds() , np.getAllIds());

		np2 = np.copy();
		np2.restrictDesign(Sets.newHashSet());
		assertEquals(np2.getAllIds() , NetPlan.getIds(Sets.newHashSet(np , lowerLayer , upperLayer)));
		
		np2 = np.copy();
		np2.restrictDesign(Sets.newHashSet(np2.getNodeFromId(idn1),np2.getNodeFromId(idn2)));
		assertEquals(np2.getAllIds() , NetPlan.getIds(Sets.newHashSet(np , lowerLayer , upperLayer , n1,n2,low12,dlow12)));

		np2 = np.copy();
		np2.restrictDesign(Sets.newHashSet(np2.getNodeFromId(idn1),np2.getNodeFromId(idn2),np2.getNodeFromId(idn3)));
		assertEquals(np2.getAllIds() , NetPlan.getIds(Sets.newHashSet(np,lowerLayer,upperLayer,n1,n2,n3,low12,low23,dlow12)));

		final Demand dlow13 = np.addDemand(n1, n3, 0, null, lowerLayer);
		final Route rlow13 = np.addRoute(dlow13, 0, 0, Arrays.asList(low12,low23), null);
		np2 = np.copy();
		np2.restrictDesign(Sets.newHashSet(np2.getNodeFromId(idn1),np2.getNodeFromId(idn3)));
		assertEquals(np2.getAllIds() , NetPlan.getIds(Sets.newHashSet(np,lowerLayer,upperLayer,
				n1,n2,n3,low12,low23,dlow12,dlow13,rlow13)));

		final Link upperLink13 = dlow13.coupleToNewLinkCreated(upperLayer);
		np2 = np.copy();
		np2.restrictDesign(Sets.newHashSet(np2.getNodeFromId(idn1),np2.getNodeFromId(idn3)));
		assertEquals(np2.getAllIds() , NetPlan.getIds(Sets.newHashSet(np,lowerLayer,upperLayer,
				n1,n2,n3,low12,low23,dlow12,dlow13,rlow13,upperLink13)));

	}		
//		allElements = new HashSet<> ();
//		allElements.addAll(netTriangle.getNodes());
//		allElements.addAll(netTriangle.getResources());
//		allElements.addAll(netTriangle.getSRGs());
//		for (NetworkLayer layer : netTriangle.getNetworkLayers())
//		{
//			allElements.addAll(netTriangle.getLinks (layer));
//			allElements.addAll(netTriangle.getDemands (layer));
//			allElements.addAll(netTriangle.getRoutes (layer));
//			allElements.addAll(netTriangle.getMulticastDemands (layer));
//			allElements.addAll(netTriangle.getMulticastTrees (layer));
//		}
//		assertEquals (netTriangle.getGlobalPlanningDomainElements("new") , allElements);
//		assertEquals (netTriangle.getGlobalPlanningDomainElements("") , Sets.newHashSet());
//		assertEquals (netTriangle.getGlobalPlanningDomainElements("other") , null);
//
//	}

//	
//	@Test
//	public void testRestrictPlanning ()
//	{
//		NetPlan np = new NetPlan ();
//		final NetworkLayer lowerLayer = np.getNetworkLayerDefault();
//		final NetworkLayer upperLayer = np.addLayer("", "", "", "", null, null);
//		final Node n1 = np.addNode(0, 0, "", null);
//		final Node n2 = np.addNode(0, 0, "", null);
//		final Node n3 = np.addNode(0, 0, "", null);
//		final Link low12 = np.addLink(n1, n2, 0, 0, 200000, null , lowerLayer);
//		final Link low13 = np.addLink(n1, n3, 0, 0, 200000, null , lowerLayer);
//		final Link low23 = np.addLink(n2, n3, 0, 0, 200000, null , lowerLayer);
//		final Demand dlow13 = np.addDemand(n1, n3, 0, null, lowerLayer);
//		final long idn1 = n1.getId ();
//		final long idn2 = n2.getId ();
//		final long idn3 = n3.getId ();
//		NetworkLayer copyLowerLayer = null;
//		NetworkLayer copyUpperLayer = null;
//		
//		NetPlan npCopy = np.copy(); copyLowerLayer = npCopy.getNetworkLayer(0); copyUpperLayer = npCopy.getNetworkLayer(1);
//		npCopy.restrictToPlanningDomain(Sets.newHashSet(npCopy.getNodeFromId(idn1),npCopy.getNodeFromId(idn3)), copyLowerLayer, false);
//		assertEquals(npCopy.getNodeIds() , Arrays.asList(n1.getId() , n3.getId()));
//		
//		npCopy = np.copy(); copyLowerLayer = npCopy.getNetworkLayer(0); copyUpperLayer = npCopy.getNetworkLayer(1);
//		npCopy.restrictToPlanningDomain(Sets.newHashSet(npCopy.getNodeFromId(idn1),npCopy.getNodeFromId(idn3)), copyLowerLayer, true);
//		assertEquals(npCopy.getNodeIds() , Arrays.asList(idn1 , idn3));
//		
//		npCopy = np.copy(); copyLowerLayer = npCopy.getNetworkLayer(0); copyUpperLayer = npCopy.getNetworkLayer(1);
//		npCopy.restrictToPlanningDomain(Sets.newHashSet(npCopy.getNodeFromId(idn1),npCopy.getNodeFromId(idn3)), copyUpperLayer, true);
//		assertEquals(npCopy.getNodeIds() , Arrays.asList(idn1 , idn3));
//
//		final Link up13 = dlow13.coupleToNewLinkCreated(upperLayer);
//		npCopy = np.copy(); copyLowerLayer = npCopy.getNetworkLayer(0); copyUpperLayer = npCopy.getNetworkLayer(1);
//		npCopy.restrictToPlanningDomain(Sets.newHashSet(npCopy.getNodeFromId(idn1),npCopy.getNodeFromId(idn3)), copyLowerLayer, false);
//		assertEquals(npCopy.getNodeIds() , Arrays.asList(n1.getId() , n3.getId()));
//		
//		npCopy = np.copy(); copyLowerLayer = npCopy.getNetworkLayer(0); copyUpperLayer = npCopy.getNetworkLayer(1);
//		npCopy.restrictToPlanningDomain(Sets.newHashSet(npCopy.getNodeFromId(idn1),npCopy.getNodeFromId(idn3)), copyLowerLayer, true);
//		assertEquals(npCopy.getNodeIds() , Arrays.asList(n1.getId() , n3.getId()));
//		
//		npCopy = np.copy(); copyLowerLayer = npCopy.getNetworkLayer(0); copyUpperLayer = npCopy.getNetworkLayer(1);
//		npCopy.restrictToPlanningDomain(Sets.newHashSet(npCopy.getNodeFromId(idn1),npCopy.getNodeFromId(idn3)), copyUpperLayer, false);
//		assertEquals(npCopy.getNodeIds() , Arrays.asList(n1.getId() , n3.getId()));
//		
//		npCopy = np.copy(); copyLowerLayer = npCopy.getNetworkLayer(0); copyUpperLayer = npCopy.getNetworkLayer(1);
//		npCopy.restrictToPlanningDomain(Sets.newHashSet(npCopy.getNodeFromId(idn1),npCopy.getNodeFromId(idn3)), copyUpperLayer, true);
//		assertEquals(npCopy.getNodeIds() , Arrays.asList(n1.getId() , n3.getId()));
//
//		final Route r123 = np.addRoute(dlow13, 1, 1, Arrays.asList(low12 , low23), null);
//		npCopy = np.copy(); copyLowerLayer = npCopy.getNetworkLayer(0); copyUpperLayer = npCopy.getNetworkLayer(1);
//		npCopy.restrictToPlanningDomain(Sets.newHashSet(npCopy.getNodeFromId(idn1),npCopy.getNodeFromId(idn3)), copyLowerLayer, false);
//		assertEquals(npCopy.getNodeIds() , Arrays.asList(n1.getId() , n2.getId() , n3.getId()));
//		
//		npCopy = np.copy(); copyLowerLayer = npCopy.getNetworkLayer(0); copyUpperLayer = npCopy.getNetworkLayer(1);
//		npCopy.restrictToPlanningDomain(Sets.newHashSet(npCopy.getNodeFromId(idn1),npCopy.getNodeFromId(idn3)), copyLowerLayer, true);
//		assertEquals(npCopy.getNodeIds() , Arrays.asList(n1.getId() , n2.getId() , n3.getId()));
//		
//		npCopy = np.copy(); copyLowerLayer = npCopy.getNetworkLayer(0); copyUpperLayer = npCopy.getNetworkLayer(1);
//		npCopy.restrictToPlanningDomain(Sets.newHashSet(npCopy.getNodeFromId(idn1),npCopy.getNodeFromId(idn3)), copyUpperLayer, false);
//		assertEquals(npCopy.getNodeIds() , Arrays.asList(n1.getId() , n2.getId() , n3.getId()));
//
//		npCopy = np.copy(); copyLowerLayer = npCopy.getNetworkLayer(0); copyUpperLayer = npCopy.getNetworkLayer(1);
//		npCopy.restrictToPlanningDomain(Sets.newHashSet(npCopy.getNodeFromId(idn1),npCopy.getNodeFromId(idn3)), copyUpperLayer, true);
//		assertEquals(npCopy.getNodeIds() , Arrays.asList(n1.getId() , n2.getId() , n3.getId()));
//	}
//

	private void checkEqual (NetPlan np1 , NetPlan np2)
	{
		assertEquals(np1.getNetworkLayers().stream().map(e->e.getName()).collect(Collectors.toSet()) , np2.getNetworkLayers().stream().map(e->e.getName()).collect(Collectors.toSet()));
		if (np1.getNumberOfNodes() != np2.getNumberOfNodes())throw new RuntimeException();
		if (np1.getNumberOfResources() != np2.getNumberOfResources())throw new RuntimeException();
		if (np1.getNumberOfSRGs() != np2.getNumberOfSRGs())throw new RuntimeException();
		for (int index = 0; index < np1.getNumberOfLayers() ; index ++)
		{
			final NetworkLayer l1 = np1.getNetworkLayer(index);
			final NetworkLayer l2 = np2.getNetworkLayer(index);
			if (np1.getNumberOfLinks(l1) != np2.getNumberOfLinks(l2)) throw new RuntimeException();
			if (np1.getNumberOfDemands(l1) != np2.getNumberOfDemands(l2)) throw new RuntimeException();
			if (np1.getNumberOfMulticastDemands(l1) != np2.getNumberOfMulticastDemands(l2)) throw new RuntimeException();
			if (np1.getNumberOfMulticastTrees(l1) != np2.getNumberOfMulticastTrees(l2)) throw new RuntimeException();
			if (l1.isSourceRouting())
			{
				if (np1.getNumberOfRoutes(l1) != np2.getNumberOfRoutes(l2)) throw new RuntimeException();
			}
			else
			{
				if (np1.getNumberOfForwardingRules(l1) != np2.getNumberOfForwardingRules(l2)) throw new RuntimeException();
			}
			assertEquals(np1.getVectorDemandOfferedTraffic(l1).zSum() , np2.getVectorDemandOfferedTraffic(l2).zSum() , 0.0001);
			assertEquals(np1.getVectorDemandCarriedTraffic(l1).zSum() , np2.getVectorDemandCarriedTraffic(l2).zSum() , 0.0001);
			assertEquals(np1.getVectorMulticastDemandOfferedTraffic(l1).zSum() , np2.getVectorMulticastDemandOfferedTraffic(l2).zSum() , 0.0001);
			assertEquals(np1.getVectorMulticastDemandCarriedTraffic(l1).zSum() , np2.getVectorMulticastDemandCarriedTraffic(l2).zSum() , 0.0001);
			assertEquals(np1.getVectorLinkCarriedTraffic(l1).zSum() , np2.getVectorLinkCarriedTraffic(l2).zSum() , 0.0001);
			assertEquals(np1.getVectorLinkOccupiedCapacity(l1).zSum() , np2.getVectorLinkOccupiedCapacity(l2).zSum() , 0.0001);
			assertEquals(np1.getVectorMulticastTreeCarriedTraffic(l1).zSum() , np2.getVectorMulticastTreeCarriedTraffic(l2).zSum() , 0.0001);
			if (l1.isSourceRouting())
				assertEquals(np1.getVectorRouteCarriedTraffic(l1).zSum() , np2.getVectorRouteCarriedTraffic(l2).zSum() , 0.0001);
			final long numCoupledLinks_1 = np1.getLinks(l1).stream().filter(e->e.isCoupled()).count();
			final long numCoupledLinks_2 = np2.getLinks(l2).stream().filter(e->e.isCoupled()).count();
			final long numCoupledDemands_1 = np1.getDemands(l1).stream().filter(e->e.isCoupled()).count();
			final long numCoupledDemands_2 = np2.getDemands(l2).stream().filter(e->e.isCoupled()).count();
			final long numCoupledMDemands_1 = np1.getMulticastDemands(l1).stream().filter(e->e.isCoupled()).count();
			final long numCoupledMDemands_2 = np2.getMulticastDemands(l2).stream().filter(e->e.isCoupled()).count();
			assertEquals (numCoupledLinks_1 , numCoupledLinks_2);
			assertEquals (numCoupledDemands_1 , numCoupledDemands_2);
			assertEquals (numCoupledMDemands_1 , numCoupledMDemands_2);
		}
	}

	
	
}
