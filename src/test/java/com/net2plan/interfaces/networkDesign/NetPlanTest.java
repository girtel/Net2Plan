package com.net2plan.interfaces.networkDesign;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.geom.Point2D;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.net2plan.utils.Pair;

import cern.colt.matrix.tdouble.DoubleMatrix1D;

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
		this.upperLayer = np.addLayer("upperLayer" , "description" , "Mbps" , "upperTrafficUnits" , null);
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
		NetworkLayer layer = np.addLayer("name" , "description" , "linkCapUnits" , "demandCapUnits",null);
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


//	@Test
//	public void testAssignFrom()
//	{
//		NetPlan np2 = new NetPlan ();
//		np2.assignFrom(np);
//		assertTrue (np.isDeepCopy(np2));
//		assertTrue (np2.isDeepCopy(np));
//	}
//
//	@Test
//	public void testCopy()
//	{
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testCopyFrom()
//	{
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testGetAttributesCollectionOfQextendsNetworkElementString()
//	{
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testGetAttributeValues()
//	{
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testGetDemandTrafficUnitsName()
//	{
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testGetLinkCapacityUnitsName()
//	{
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testGetNetworkDescription()
//	{
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testGetNetworkElementByAttribute()
//	{
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testGetNetworkName()
//	{
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testGetNodeByName()
//	{
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testGetNumberOfNodePairs()
//	{
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testComputeRouteCostVector()
//	{
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testComputeMulticastTreeCostVector()
//	{
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testHasUnicastRoutingLoops()
//	{
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testIsUp()
//	{
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testRemoveNetworkLayer()
//	{
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testRemoveAllDemands()
//	{
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testRemoveAllForwardingRules()
//	{
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testRemoveAllLinks()
//	{
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testRemoveAllMulticastDemands()
//	{
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testRemoveAllMulticastTrees()
//	{
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testRemoveAllMulticastTreesUnused()
//	{
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testRemoveAllNetworkLayers()
//	{
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testRemoveAllNodes()
//	{
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testRemoveAllRoutes()
//	{
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testRemoveAllRoutesUnused()
//	{
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testRemoveAllLinksUnused()
//	{
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testRemoveAllUnicastRoutingInformation()
//	{
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testRemoveAllSRGs()
//	{
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testRemoveAllResources()
//	{
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testReset()
//	{
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testSaveToFile()
//	{
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testSaveToOutputStream()
//	{
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testSetAllLinksFailureState()
//	{
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testSetAllNodesFailureState()
//	{
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testSetLinksAndNodesFailureState()
//	{
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testSetDemandTrafficUnitsName()
//	{
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testSetDescription()
//	{
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testSetForwardingRule()
//	{
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testSetForwardingRulesCollectionOfDemandCollectionOfLinkCollectionOfDoubleBoolean()
//	{
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testSetForwardingRulesDoubleMatrix2DNetworkLayerArray()
//	{
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testSetLinkCapacityUnitsName()
//	{
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testSetNetworkDescription()
//	{
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testSetNetworkLayerDefault()
//	{
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testSetNetworkName()
//	{
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testSetRoutingType()
//	{
//		fail("Not yet implemented");
//	}
//
//	@Test
//	public void testSetTrafficMatrix()
//	{
//		fail("Not yet implemented");
//	}
//
}
