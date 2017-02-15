package com.net2plan.interfaces.networkDesign;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.awt.geom.Point2D;
import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
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

import com.google.common.collect.ImmutableMap;
import com.net2plan.utils.Constants.RoutingType;
import com.net2plan.utils.Pair;

import cern.colt.matrix.tdouble.DoubleFactory2D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;

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
		np.setDemandTrafficUnitsName("Mbps" , lowerLayer);
		this.upperLayer = np.addLayer("upperLayer" , "description" , "Mbps" , "upperTrafficUnits" , new URL ("file:/upperIcon") , null);
		this.n1 = this.np.addNode(0 , 0 , "node1" , null);
		this.n2 = np.addNode(0 , 0 , "node2" , null);
		this.n3 = np.addNode(0 , 0 , "node3" , null);
		this.n1.setUrlNodeIcon(lowerLayer , new URL ("file:/lowerIcon"));
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

		/* Triangle link cap 100, length 1, demands offered 1 */
		this.netTriangle = new NetPlan ();
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
	}

	@After
	public void tearDown() throws Exception
	{
		np.checkCachesConsistency();
	}

	@Test
	public void testNetPlan()
	{
		this.np = new NetPlan ();
	}

	@Test
	public void testNetPlanFile()
	{
		File f = new File ("test.n2p");
		this.np.saveToFile(f);
		NetPlan readNp = new NetPlan (f);
		assertTrue(readNp.isDeepCopy(np));
		assertTrue(np.isDeepCopy(readNp));
		
		NetPlan np1 = new NetPlan (new File ("src/main/resources/data/networkTopologies/example7nodes_ipOverWDM.n2p"));
		np1.checkCachesConsistency();
		np1.saveToFile(new File ("test.n2p"));
		NetPlan np2 = new NetPlan (new File ("test.n2p"));
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

		K=3; maxRouteCostFactorRespectToShortestPath = 0.1; cpl = cpl = netTriangle.computeUnicastCandidatePathList(null ,K, maxLengthInKm, maxNumHops, maxPropDelayInMs, maxRouteCost,maxRouteCostFactorRespectToShortestPath, maxRouteCostRespectToShortestPath , null);
		maxRouteCostFactorRespectToShortestPath = -1;
		for (Demand d : netTriangle.getDemands())
			assertEquals(cpl.get(Pair.of(d.getIngressNode(),d.getEgressNode())) , Arrays.asList(Arrays.asList(netTriangle.getNodePairLinks(d.getIngressNode() , d.getEgressNode() , false).iterator().next())));
		K=3; maxRouteCostFactorRespectToShortestPath = 2; cpl = cpl = netTriangle.computeUnicastCandidatePathList(null ,K, maxLengthInKm, maxNumHops, maxPropDelayInMs, maxRouteCost,maxRouteCostFactorRespectToShortestPath, maxRouteCostRespectToShortestPath , null);
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
		K=3; maxRouteCostRespectToShortestPath = 0.1; cpl = cpl = netTriangle.computeUnicastCandidatePathList(null ,K, maxLengthInKm, maxNumHops, maxPropDelayInMs, maxRouteCost,maxRouteCostFactorRespectToShortestPath, maxRouteCostRespectToShortestPath , null);
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
		Map<MulticastDemand,List<Set<Link>>> cpl;
		MulticastDemand d123 = netTriangle.addMulticastDemand(netTriangle_n1 , 
				new HashSet<Node> (Arrays.asList(netTriangle_n2 , netTriangle_n3)) , 0 , null); 
		cpl = netTriangle.computeMulticastCandidatePathList(null , "cplex" , "cplex.dll" , -1 , "K" , "100");
		assertEquals(cpl.get(d123) , Arrays.asList(
				new HashSet<Link> (Arrays.asList(netTriangle_e12 , netTriangle_e13))  ,
				new HashSet<Link> (Arrays.asList(netTriangle_e12 , netTriangle_e23)) , 
				new HashSet<Link> (Arrays.asList(netTriangle_e13 , netTriangle_e32)) ));
		cpl = netTriangle.computeMulticastCandidatePathList(null , "cplex" , "cplex.dll" , -1 , "K" , "100" , "maxCopyCapability" , "1");
		assertEquals(cpl.get(d123) , Arrays.asList(
				new HashSet<Link> (Arrays.asList(netTriangle_e12 , netTriangle_e23)) , 
				new HashSet<Link> (Arrays.asList(netTriangle_e13 , netTriangle_e32)) ));
		cpl = netTriangle.computeMulticastCandidatePathList(null , "cplex" , "cplex.dll" , -1 , "K" , "100" , "maxE2ELengthInKm" , "1");
		assertEquals(cpl.get(d123) , Arrays.asList(
				new HashSet<Link> (Arrays.asList(netTriangle_e12 , netTriangle_e13))));
		cpl = netTriangle.computeMulticastCandidatePathList(null , "cplex" , "cplex.dll" , -1 , "K" , "100" , "maxE2ENumHops" , "1");
		assertEquals(cpl.get(d123) , Arrays.asList(
				new HashSet<Link> (Arrays.asList(netTriangle_e12 , netTriangle_e13))));
		cpl = netTriangle.computeMulticastCandidatePathList(null , "cplex" , "cplex.dll" , -1 , "K" , "100" , "maxE2EPropDelayInMs" , "1000");
		assertEquals(cpl.get(d123) , Arrays.asList(
				new HashSet<Link> (Arrays.asList(netTriangle_e12 , netTriangle_e13))));
		cpl = netTriangle.computeMulticastCandidatePathList(null , "cplex" , "cplex.dll" , -1 , "K" , "100" , "maxTreeCost" , "1");
		assertEquals(cpl.get(d123) , Arrays.asList());
		cpl = netTriangle.computeMulticastCandidatePathList(null , "cplex" , "cplex.dll" , -1 , "K" , "100" , "maxTreeCostFactorRespectToMinimumCostTree" , "1");
		assertEquals(cpl.get(d123) , Arrays.asList(
				new HashSet<Link> (Arrays.asList(netTriangle_e12 , netTriangle_e13))  ,
				new HashSet<Link> (Arrays.asList(netTriangle_e12 , netTriangle_e23)) , 
				new HashSet<Link> (Arrays.asList(netTriangle_e13 , netTriangle_e32)) ));
		cpl = netTriangle.computeMulticastCandidatePathList(null , "cplex" , "cplex.dll" , -1 , "K" , "100" , "maxTreeCostRespectToMinimumCostTree" , "0");
		assertEquals(cpl.get(d123) , Arrays.asList(
				new HashSet<Link> (Arrays.asList(netTriangle_e12 , netTriangle_e13))  ,
				new HashSet<Link> (Arrays.asList(netTriangle_e12 , netTriangle_e23)) , 
				new HashSet<Link> (Arrays.asList(netTriangle_e13 , netTriangle_e32)) ));
		netTriangle_e12.remove();
		cpl = netTriangle.computeMulticastCandidatePathList(null , "cplex" , "cplex.dll" , -1 , "K" , "100");
		assertEquals(cpl.get(d123) , Arrays.asList(
				new HashSet<Link> (Arrays.asList(netTriangle_e13 , netTriangle_e32)) ));
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
		np.assignFrom(np2);
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
		np.setForwardingRule(d12, link12 , 0.7); 
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

}
