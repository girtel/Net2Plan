import org.junit.*;

import java.util.*;

import static org.junit.Assert.assertTrue;

public class MulticastTreeTest 
{
	private NetPlan np = null;
	private Node n1, n2 , n3;
	private Link link12, link23 , link13;
	private MulticastDemand d123;
	private MulticastTree tStar, t123;
	private Set<Link> star, line123;
	private Set<Node> endNodes;
	

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
		this.n1 = this.np.addNode(0 , 0 , "node1" , null);
		this.n2 = np.addNode(0 , 0 , "node2" , null);
		this.n3 = np.addNode(0 , 0 , "node3" , null);
		this.link12 = np.addLink(n1,n2,100,100,1,null);
		this.link23 = np.addLink(n2,n3,100,100,1,null);
		this.link13 = np.addLink(n1,n3,100,100,1,null);
		this.endNodes = new HashSet<Node> (); endNodes.add(n2); endNodes.add(n3);
		this.line123 = new HashSet<Link> (); line123.add(link12); line123.add(link23);
		this.star = new HashSet<Link> (); star.add(link12); star.add(link13);
		this.d123 = np.addMulticastDemand(n1 , endNodes , 100 , null);
		this.t123 = np.addMulticastTree(d123 , 10,15,line123,null);
		this.tStar = np.addMulticastTree(d123 , 10,15,star,null);
	}

	@After
	public void tearDown() throws Exception 
	{
		np.checkCachesConsistency();
	}

	@Test
	public void testIsDeepCopyMulticastTree() 
	{
		NetPlan np2 = np.copy();
		np2.checkCachesConsistency();
		assertTrue (np.isDeepCopy(np));
		assertTrue (np.isDeepCopy(np2));
		assertTrue (np2.isDeepCopy(np));
	}

	@Test
	public void testGetInitialLinkSet()
	{
		Assert.assertEquals (t123.getInitialLinkSet() , line123);
		Assert.assertEquals (tStar.getInitialLinkSet() , star);
		link12.setFailureState(false);
		Assert.assertEquals (t123.getInitialLinkSet() , line123);
		Assert.assertEquals (tStar.getInitialLinkSet() , star);
	}

	@Test
	public void testRevertToInitialSetOfLinks() 
	{
		t123.setLinks(star);
		Assert.assertEquals (t123.getLinkSet() , star);
		Assert.assertEquals (t123.getInitialLinkSet() , line123);
		t123.revertToInitialSetOfLinks();
		Assert.assertEquals (t123.getLinkSet() , line123);
		Assert.assertEquals (t123.getInitialLinkSet() , line123);
	}

	@Test
	public void testIsUpTheInitialLinkSet() 
	{
		t123.setLinks(star);
		link23.setFailureState(false);
		assertTrue(!t123.isUpTheInitialLinkSet());
		link23.setFailureState(true);
		assertTrue(t123.isUpTheInitialLinkSet());
	}

	@Test
	public void testGetLayer() 
	{
		Assert.assertEquals(tStar.getLayer() , np.getNetworkLayerDefault());
	}

	@Test
	public void testGetSRGs() 
	{
		Assert.assertEquals (t123.getSRGs() , Collections.emptySet());
		SharedRiskGroup srg = np.addSRG(1,1,null); 
		srg.addLink(link13);
		Assert.assertEquals (t123.getSRGs() , Collections.emptySet());
		Assert.assertEquals (tStar.getSRGs() , Collections.singleton(srg));
		srg.addLink(link12);
		Assert.assertEquals (t123.getSRGs() , Collections.singleton(srg));
		Assert.assertEquals (tStar.getSRGs() , Collections.singleton(srg));
		srg.removeLink(link13);
		srg.removeLink(link12);
		Assert.assertEquals (t123.getSRGs() , Collections.emptySet());
		Assert.assertEquals (tStar.getSRGs() , Collections.emptySet());
		srg.addNode(n2);
		Assert.assertEquals (t123.getSRGs() , Collections.singleton(srg));
		Assert.assertEquals (tStar.getSRGs() , Collections.singleton(srg));
	}

	@Test
	public void testGetMulticastDemand() 
	{
		Assert.assertEquals(t123.getMulticastDemand() , d123);
		Assert.assertEquals(tStar.getMulticastDemand() , d123);
	}

	@Test
	public void testGetIngressNode() 
	{
		Assert.assertEquals(t123.getIngressNode() , n1);
		Assert.assertEquals(tStar.getIngressNode() , n1);
	}

	@Test
	public void testGetEgressNodes() 
	{
		Assert.assertEquals(t123.getEgressNodes() , endNodes);
		Assert.assertEquals(tStar.getEgressNodes() , endNodes);
	}

	@Test
	public void testGetSeqLinksToEgressNode() 
	{
		Assert.assertEquals(t123.getSeqLinksToEgressNode(n1) , null);
		Assert.assertEquals(t123.getSeqLinksToEgressNode(n2) , Collections.singletonList(link12));
		List<Link> path123 = new LinkedList<Link> (); path123.add(link12); path123.add(link23);
		Assert.assertEquals(t123.getSeqLinksToEgressNode(n3) , path123);
		Assert.assertEquals(tStar.getSeqLinksToEgressNode(n1) , null);
		Assert.assertEquals(tStar.getSeqLinksToEgressNode(n2) , Collections.singletonList(link12));
		Assert.assertEquals(tStar.getSeqLinksToEgressNode(n3) , Collections.singletonList(link13));
	}

	@Test
	public void testSetLinks() 
	{
		t123.setLinks(star);
		Assert.assertEquals(t123.getLinkSet() , star);
		Assert.assertEquals(t123.getInitialLinkSet() , line123);
	}

	@Test
	public void testIsDown() 
	{
		assertTrue (!t123.isDown());
		assertTrue (!tStar.isDown());
		link12.setFailureState(false);
		assertTrue (t123.isDown());
		assertTrue (tStar.isDown());
		link12.setFailureState(true);
		assertTrue (!t123.isDown());
		assertTrue (!tStar.isDown());
		link23.setFailureState(false);
		assertTrue (t123.isDown());
		assertTrue (!tStar.isDown());
		link23.setFailureState(true);
		assertTrue (!t123.isDown());
		assertTrue (!tStar.isDown());
	}

	@Test
	public void testGetNodeSet() 
	{
		Set<Node> allNodes = new HashSet<Node> (); allNodes.add(n1); allNodes.add(n2); allNodes.add(n3);   
		Assert.assertEquals(t123.getNodeSet() , allNodes);
		Assert.assertEquals(tStar.getNodeSet() , allNodes);
	}

	@Test
	public void testGetIngressLinkOfNode() 
	{
		Assert.assertEquals(t123.getIngressLinkOfNode(n1) , null);
		Assert.assertEquals(t123.getIngressLinkOfNode(n2) , link12);
		Assert.assertEquals(t123.getIngressLinkOfNode(n3) , link23);
		Assert.assertEquals(tStar.getIngressLinkOfNode(n1) , null);
		Assert.assertEquals(tStar.getIngressLinkOfNode(n2) , link12);
		Assert.assertEquals(tStar.getIngressLinkOfNode(n3) , link13);
	}

	@Test
	public void testGetOutputLinkOfNode() 
	{
		Assert.assertEquals(t123.getOutputLinkOfNode(n1) , Collections.singleton(link12));
		Assert.assertEquals(t123.getOutputLinkOfNode(n2) , Collections.singleton(link23));
		Assert.assertEquals(t123.getOutputLinkOfNode(n3) , Collections.emptySet());
		Assert.assertEquals(tStar.getOutputLinkOfNode(n1) , star);
		Assert.assertEquals(tStar.getOutputLinkOfNode(n2) , Collections.emptySet());
		Assert.assertEquals(tStar.getOutputLinkOfNode(n3) , Collections.emptySet());
	}

	@Test
	public void testGetTreeTotalLengthInKm() 
	{
		Assert.assertEquals(t123.getTreeTotalLengthInKm() , 200 , 0.0);
		Assert.assertEquals(tStar.getTreeTotalLengthInKm() , 200 , 0.0);
	}

	@Test
	public void testGetTreeMaximumPathLengthInKm() 
	{
		Assert.assertEquals(t123.getTreeMaximumPathLengthInKm() , 200 , 0.0);
		Assert.assertEquals(tStar.getTreeMaximumPathLengthInKm() , 100 , 0.0);
	}

	@Test
	public void testGetTreeAveragePathLengthInKm() 
	{
		Assert.assertEquals(t123.getTreeAveragePathLengthInKm() , 150 , 0.0);
		Assert.assertEquals(tStar.getTreeAveragePathLengthInKm() , 100 , 0.0);
	}

	@Test
	public void testGetTreeMaximumPropagationDelayInMs() 
	{
		Assert.assertEquals(t123.getTreeMaximumPropagationDelayInMs() , 200000 , 0.0);
		Assert.assertEquals(tStar.getTreeMaximumPropagationDelayInMs() , 100000 , 0.0);
	}

	@Test
	public void testGetTreeAveragePropagationDelayInMs() 
	{
		Assert.assertEquals(t123.getTreeAveragePropagationDelayInMs() , 150000 , 0.0);
		Assert.assertEquals(tStar.getTreeAveragePropagationDelayInMs() , 100000 , 0.0);
	}

	@Test
	public void testGetTreeMaximumPathLengthInHops() 
	{
		Assert.assertEquals(t123.getTreeMaximumPathLengthInHops() , 2);
		Assert.assertEquals(tStar.getTreeMaximumPathLengthInHops() , 1);
	}

	@Test
	public void testGetTreeAveragePathLengthInHops() 
	{
		Assert.assertEquals(t123.getTreeAveragePathLengthInHops() , 1.5 , 0.0);
		Assert.assertEquals(tStar.getTreeAveragePathLengthInHops() , 1 , 0.0);
	}

	@Test
	public void testGetTreeNumberOfLinks() 
	{
		Assert.assertEquals(t123.getTreeNumberOfLinks() , 2);
		Assert.assertEquals(tStar.getTreeNumberOfLinks() , 2);
	}

	@Test
	public void testGetCarriedTraffic() 
	{
		Assert.assertEquals(t123.getCarriedTraffic() , 10 , 0.0);
		Assert.assertEquals(tStar.getCarriedTraffic() , 10 , 0.0);
		Assert.assertEquals(t123.getOccupiedLinkCapacity() , 15 , 0.0);
		Assert.assertEquals(tStar.getOccupiedLinkCapacity() , 15 , 0.0);
		Assert.assertEquals(t123.getCarriedTrafficInNoFailureState() , 10 , 0.0);
		Assert.assertEquals(tStar.getCarriedTrafficInNoFailureState() , 10 , 0.0);
		Assert.assertEquals(t123.getOccupiedLinkCapacityInNoFailureState() , 15 , 0.0);
		Assert.assertEquals(tStar.getOccupiedLinkCapacityInNoFailureState() , 15 , 0.0);
		link12.setFailureState(false);
		Assert.assertEquals(t123.getCarriedTraffic() , 0 , 0.0);
		Assert.assertEquals(tStar.getCarriedTraffic() , 0 , 0.0);
		Assert.assertEquals(t123.getOccupiedLinkCapacity() , 0 , 0.0);
		Assert.assertEquals(tStar.getOccupiedLinkCapacity() , 0 , 0.0);
		Assert.assertEquals(t123.getCarriedTrafficInNoFailureState() , 10 , 0.0);
		Assert.assertEquals(tStar.getCarriedTrafficInNoFailureState() , 10 , 0.0);
		Assert.assertEquals(t123.getOccupiedLinkCapacityInNoFailureState() , 15 , 0.0);
		Assert.assertEquals(tStar.getOccupiedLinkCapacityInNoFailureState() , 15 , 0.0);
		link12.setFailureState(true);
		Assert.assertEquals(t123.getCarriedTraffic() , 10 , 0.0);
		Assert.assertEquals(tStar.getCarriedTraffic() , 10 , 0.0);
		Assert.assertEquals(t123.getOccupiedLinkCapacity() , 15 , 0.0);
		Assert.assertEquals(tStar.getOccupiedLinkCapacity() , 15 , 0.0);
		Assert.assertEquals(t123.getCarriedTrafficInNoFailureState() , 10 , 0.0);
		Assert.assertEquals(tStar.getCarriedTrafficInNoFailureState() , 10 , 0.0);
		Assert.assertEquals(t123.getOccupiedLinkCapacityInNoFailureState() , 15 , 0.0);
		Assert.assertEquals(tStar.getOccupiedLinkCapacityInNoFailureState() , 15 , 0.0);
		link23.setFailureState(false);
		Assert.assertEquals(t123.getCarriedTraffic() , 0 , 0.0);
		Assert.assertEquals(tStar.getCarriedTraffic() , 10 , 0.0);
		Assert.assertEquals(t123.getOccupiedLinkCapacity() , 0 , 0.0);
		Assert.assertEquals(tStar.getOccupiedLinkCapacity() , 15 , 0.0);
		Assert.assertEquals(t123.getCarriedTrafficInNoFailureState() , 10 , 0.0);
		Assert.assertEquals(tStar.getCarriedTrafficInNoFailureState() , 10 , 0.0);
		Assert.assertEquals(t123.getOccupiedLinkCapacityInNoFailureState() , 15 , 0.0);
		Assert.assertEquals(tStar.getOccupiedLinkCapacityInNoFailureState() , 15 , 0.0);
		link23.setFailureState(true);
		Assert.assertEquals(t123.getCarriedTraffic() , 10 , 0.0);
		Assert.assertEquals(tStar.getCarriedTraffic() , 10 , 0.0);
		Assert.assertEquals(t123.getOccupiedLinkCapacity() , 15 , 0.0);
		Assert.assertEquals(tStar.getOccupiedLinkCapacity() , 15 , 0.0);
		Assert.assertEquals(t123.getCarriedTrafficInNoFailureState() , 10 , 0.0);
		Assert.assertEquals(tStar.getCarriedTrafficInNoFailureState() , 10 , 0.0);
		Assert.assertEquals(t123.getOccupiedLinkCapacityInNoFailureState() , 15 , 0.0);
		Assert.assertEquals(tStar.getOccupiedLinkCapacityInNoFailureState() , 15 , 0.0);
	}

	@Test
	public void testSetCarriedTraffic() 
	{
		Assert.assertEquals(t123.getCarriedTraffic() , 10 , 0.0);
		Assert.assertEquals(tStar.getCarriedTraffic() , 10 , 0.0);
		Assert.assertEquals(t123.getOccupiedLinkCapacity() , 15 , 0.0);
		Assert.assertEquals(tStar.getOccupiedLinkCapacity() , 15 , 0.0);
		t123.setCarriedTraffic(20 , 30);
		tStar.setCarriedTraffic(20 , 30);
		Assert.assertEquals(t123.getCarriedTraffic() , 20 , 0.0);
		Assert.assertEquals(tStar.getCarriedTraffic() , 20 , 0.0);
		Assert.assertEquals(t123.getOccupiedLinkCapacity() , 30 , 0.0);
		Assert.assertEquals(tStar.getOccupiedLinkCapacity() , 30 , 0.0);
		link23.setFailureState(false);
		t123.setCarriedTraffic(40 , 60);
		tStar.setCarriedTraffic(40 , 60);
		Assert.assertEquals(t123.getCarriedTraffic() , 0 , 0.0);
		Assert.assertEquals(tStar.getCarriedTraffic() , 40 , 0.0);
		Assert.assertEquals(t123.getOccupiedLinkCapacity() , 0 , 0.0);
		Assert.assertEquals(tStar.getOccupiedLinkCapacity() , 60 , 0.0);
		Assert.assertEquals(t123.getCarriedTrafficInNoFailureState() , 40 , 0.0);
		Assert.assertEquals(tStar.getCarriedTrafficInNoFailureState() , 40 , 0.0);
		Assert.assertEquals(t123.getOccupiedLinkCapacityInNoFailureState() , 60 , 0.0);
		Assert.assertEquals(tStar.getOccupiedLinkCapacityInNoFailureState() , 60 , 0.0);
	}

	@Test
	public void testRemove() 
	{
		t123.remove();
		Assert.assertEquals(d123.getCarriedTraffic() , 10 , 0.0);
		Assert.assertEquals(d123.getMulticastTrees() , Collections.singleton(tStar));
		tStar.remove();
		Assert.assertEquals(d123.getCarriedTraffic() , 0 , 0.0);
		Assert.assertEquals(d123.getMulticastTrees() , Collections.emptySet());
	}


}
