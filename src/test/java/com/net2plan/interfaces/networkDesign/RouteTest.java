package com.net2plan.interfaces.networkDesign;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class RouteTest 
{
	private NetPlan np = null;
	private Node n1, n2 , n3;
	private Link link12, link23 , link13;
	private Demand d13, d12 , scd123;
	private Route r12, r123a, r123b , sc123;
	private List<Link> path13;
	private List<NetworkElement> pathSc123;
	private Resource res2 , res2backup;
	private ProtectionSegment segm13;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception 
	{
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
		this.d13 = np.addDemand(n1 , n3 , 3 , null);
		this.d12 = np.addDemand(n1, n2, 3 , null);
		this.r12 = np.addRoute(d12,1,1.5,Collections.singletonList(link12),null);
		this.path13 = new LinkedList<Link> (); path13.add(link12); path13.add(link23);
		this.r123a = np.addRoute(d13,1,1.5,path13,null);
		this.r123b = np.addRoute(d13,1,1.5,path13,null);
		this.res2 = np.addResource("type" , "name" , n2 , 100 , "Mbps" , null , 10 , null);
		this.res2backup = np.addResource("type" , "name" , n2 , 100 , "Mbps" , null , 10 , null);
		this.scd123 = np.addDemand(n1 , n3 , 3 , null);
		this.scd123.setServiceChainSequenceOfTraversedResourceTypes(Collections.singletonList("type"));
		this.pathSc123 = new LinkedList<NetworkElement> (); pathSc123.add(link12); pathSc123.add(res2); pathSc123.add(link23); 
		this.sc123 = np.addServiceChain(scd123 , 100 , 300 , pathSc123 , Collections.singletonMap(res2 , 1.0) , null); 
		this.segm13 = np.addProtectionSegment(Collections.singletonList(link13) , 50 , null);
	}

	@After
	public void tearDown() throws Exception 
	{
		np.checkCachesConsistency();
	}

	@Test
	public void testCheckCaches() 
	{
		np.checkCachesConsistency();
	}

	@Test
	public void testCopyFromAndEquals() 
	{
		NetPlan np2 = np.copy();
		np2.checkCachesConsistency();
		assertTrue (np.isDeepCopy(np));
		assertTrue (np.isDeepCopy(np2));
		assertTrue (np2.isDeepCopy(np));
	}

	@Test
	public void testGetInitialSequenceOfLinks() 
	{
		assertEquals (r12.getInitialSequenceOfLinks() , Collections.singletonList(link12));
		assertEquals (r123a.getInitialSequenceOfLinks() , path13);
		assertEquals (r123b.getInitialSequenceOfLinks() , path13);
		assertEquals (sc123.getInitialSequenceOfLinks() , path13);
		link12.setFailureState(false);
		assertEquals (r12.getInitialSequenceOfLinks() , Collections.singletonList(link12));
		assertEquals (r123a.getInitialSequenceOfLinks() , path13);
		assertEquals (r123b.getInitialSequenceOfLinks() , path13);
		assertEquals (sc123.getInitialSequenceOfLinks() , path13);
	}

	@Test
	public void testGetInitialSeqLinksAndResourcesTraversed() 
	{
		assertEquals (r12.getInitialSeqLinksAndResourcesTraversed() , Collections.singletonList(link12));
		assertEquals (r123a.getInitialSeqLinksAndResourcesTraversed() , path13);
		assertEquals (r123b.getInitialSeqLinksAndResourcesTraversed() , path13);
		assertEquals (sc123.getInitialSeqLinksAndResourcesTraversed() , pathSc123);
		link12.setFailureState(false);
		assertEquals (r12.getInitialSeqLinksAndResourcesTraversed() , Collections.singletonList(link12));
		assertEquals (r123a.getInitialSeqLinksAndResourcesTraversed() , path13);
		assertEquals (r123b.getInitialSeqLinksAndResourcesTraversed() , path13);
		assertEquals (sc123.getInitialSeqLinksAndResourcesTraversed() , pathSc123);
	}

	@Test
	public void testGetCurrentSeqLinksAndResourcesTraversed() 
	{
		assertEquals (r12.getCurrentSeqLinksAndResourcesTraversed() , Collections.singletonList(link12));
		assertEquals (r123a.getCurrentSeqLinksAndResourcesTraversed() , path13);
		assertEquals (r123b.getCurrentSeqLinksAndResourcesTraversed() , path13);
		assertEquals (sc123.getCurrentSeqLinksAndResourcesTraversed() , pathSc123);
		link12.setFailureState(false);
		assertEquals (r12.getCurrentSeqLinksAndResourcesTraversed() , Collections.singletonList(link12));
		assertEquals (r123a.getCurrentSeqLinksAndResourcesTraversed() , path13);
		assertEquals (r123b.getCurrentSeqLinksAndResourcesTraversed() , path13);
		assertEquals (sc123.getCurrentSeqLinksAndResourcesTraversed() , pathSc123);
	}

	@Test
	public void testGetCurrentResourcesTraversed() 
	{
		assertEquals (r12.getCurrentResourcesTraversed() , Collections.emptySet());
		assertEquals (r123a.getCurrentResourcesTraversed() , Collections.emptySet());
		assertEquals (r123b.getCurrentResourcesTraversed() , Collections.emptySet());
		assertEquals (sc123.getCurrentResourcesTraversed() , Collections.singleton(res2));
		link12.setFailureState(false);
		assertEquals (r12.getCurrentResourcesTraversed() , Collections.emptySet());
		assertEquals (r123a.getCurrentResourcesTraversed() , Collections.emptySet());
		assertEquals (r123b.getCurrentResourcesTraversed() , Collections.emptySet());
		assertEquals (sc123.getCurrentResourcesTraversed() , Collections.singleton(res2));
	}

	@Test
	public void testGetInitialResourcesTraversed() 
	{
		assertEquals (r12.getInitialSeqLinksAndResourcesTraversed() , Collections.singletonList(link12));
		assertEquals (r123a.getInitialSeqLinksAndResourcesTraversed() , path13);
		assertEquals (r123b.getInitialSeqLinksAndResourcesTraversed() , path13);
		assertEquals (sc123.getInitialSeqLinksAndResourcesTraversed() , pathSc123);
		link12.setFailureState(false);
		assertEquals (r12.getInitialSeqLinksAndResourcesTraversed() , Collections.singletonList(link12));
		assertEquals (r123a.getInitialSeqLinksAndResourcesTraversed() , path13);
		assertEquals (r123b.getInitialSeqLinksAndResourcesTraversed() , path13);
		assertEquals (sc123.getInitialSeqLinksAndResourcesTraversed() , pathSc123);
	}

	@Test
	public void testGetResourceCurrentTotalOccupation() 
	{
		assertEquals (sc123.getResourceCurrentTotalOccupation(res2) , 1.0 , 0.0);
		link12.setFailureState(false);
		assertEquals (sc123.getResourceCurrentTotalOccupation(res2) , 0.0 , 0.0);
	}

	@Test
	public void testAddProtectionSegment() 
	{
		r123a.addProtectionSegment(segm13);
		assertEquals (r123a.getPotentialBackupProtectionSegments() , Collections.singleton(segm13));
	}

	@Test
	public void testGetCarriedTrafficAndOccupiedCapacities() 
	{
		/* Carried traffic and occupied capacities in no failure */
		assertEquals (d12.getCarriedTraffic() , 1 , 0.0);
		assertEquals (d13.getCarriedTraffic() , 2 , 0.0);
		assertEquals (scd123.getCarriedTraffic() , 100 , 0.0);
		assertEquals (r12.getCarriedTraffic() , 1 , 0.0);
		assertEquals (r12.getCarriedTrafficInNoFailureState() , 1 , 0.0);
		assertEquals (r12.getOccupiedCapacity() , 1.5 , 0.0);
		assertEquals (r12.getOccupiedCapacityInNoFailureState(), 1.5 , 0.0);
		assertEquals (r123a.getCarriedTraffic() , 1 , 0.0);
		assertEquals (r123a.getCarriedTrafficInNoFailureState() , 1 , 0.0);
		assertEquals (r123a.getOccupiedCapacity() , 1.5 , 0.0);
		assertEquals (r123a.getOccupiedCapacityInNoFailureState(), 1.5 , 0.0);
		assertEquals (r123b.getCarriedTraffic() , 1 , 0.0);
		assertEquals (r123b.getCarriedTrafficInNoFailureState() , 1 , 0.0);
		assertEquals (r123b.getOccupiedCapacity() , 1.5 , 0.0);
		assertEquals (r123b.getOccupiedCapacityInNoFailureState(), 1.5 , 0.0);
		assertEquals (sc123.getCarriedTraffic() , 100 , 0.0);
		assertEquals (sc123.getCarriedTrafficInNoFailureState() , 100 , 0.0);
		assertEquals (sc123.getOccupiedCapacity() , 300 , 0.0);
		assertEquals (sc123.getOccupiedCapacityInNoFailureState(), 300 , 0.0);
		
		/* Carried traffic and occupied capacities in failure */
		link12.setFailureState(false);
		assertEquals (d12.getCarriedTraffic() , 0 , 0.0);
		assertEquals (d13.getCarriedTraffic() , 0 , 0.0);
		assertEquals (scd123.getCarriedTraffic() , 0 , 0.0);
		assertEquals (r12.getCarriedTraffic() , 0 , 0.0);
		assertEquals (r12.getCarriedTrafficInNoFailureState() , 1 , 0.0);
		assertEquals (r12.getOccupiedCapacity() , 0 , 0.0);
		assertEquals (r12.getOccupiedCapacityInNoFailureState(), 1.5 , 0.0);
		assertEquals (r123a.getCarriedTraffic() , 0 , 0.0);
		assertEquals (r123a.getCarriedTrafficInNoFailureState() , 1 , 0.0);
		assertEquals (r123a.getOccupiedCapacity() , 0 , 0.0);
		assertEquals (r123a.getOccupiedCapacityInNoFailureState(), 1.5 , 0.0);
		assertEquals (r123b.getCarriedTraffic() , 0 , 0.0);
		assertEquals (r123b.getCarriedTrafficInNoFailureState() , 1 , 0.0);
		assertEquals (r123b.getOccupiedCapacity() , 0 , 0.0);
		assertEquals (r123b.getOccupiedCapacityInNoFailureState(), 1.5 , 0.0);
		assertEquals (sc123.getCarriedTraffic() , 0.0 , 0.0);
		assertEquals (sc123.getCarriedTrafficInNoFailureState() , 100 , 0.0);
		assertEquals (sc123.getOccupiedCapacity() , 0.0 , 0.0);
		assertEquals (sc123.getOccupiedCapacityInNoFailureState(), 300 , 0.0);
	}

	@Test
	public void testGetCurrentlyTraversedProtectionSegments() 
	{
		np.checkCachesConsistency();
		r123a.addProtectionSegment(segm13);
		r123a.setSeqLinksAndProtectionSegments(Collections.singletonList(segm13));
		assertEquals (r123a.getCurrentlyTraversedProtectionSegments(), Collections.singletonList(segm13));
		link12.setFailureState(false);
		assertEquals (r123a.getCurrentlyTraversedProtectionSegments(), Collections.singletonList(segm13));
	}

	@Test
	public void testRevertToInitialSequenceOfLinks() 
	{
		r123a.setSeqLinksAndProtectionSegments(Collections.singletonList(link13));
		r123a.revertToInitialSequenceOfLinks();
		assertEquals (r123a.getSeqLinksAndProtectionSegments(), path13);
	}

	@Test
	public void testIsUpTheInitialSequenceOfLinks() 
	{
		r123a.setSeqLinksAndProtectionSegments(Collections.singletonList(link13));
		assertTrue (r123a.isUpTheInitialSequenceOfLinks());
		link12.setFailureState(false);
		assertTrue (!r123a.isUpTheInitialSequenceOfLinks());
	}

	@Test
	public void testGetDemand() 
	{
		assertEquals (r12.getDemand() , d12);
		assertEquals (r123a.getDemand() , d13);
	}

	@Test
	public void testGetEgressNode() 
	{
		assertEquals (r12.getEgressNode() , n2);
		assertEquals (r123a.getEgressNode() , n3);
	}

	@Test
	public void testGetFirstAvailableNodeAfterFailures() 
	{
		/* last node fails */
		assertEquals (r123a.getFirstAvailableNodeAfterFailures() , n3);
		n3.setFailureState(false);
		assertEquals (r123a.getFirstAvailableNodeAfterFailures() , null);
		n3.setFailureState(true);
		assertEquals (r123a.getFirstAvailableNodeAfterFailures() , n3);
		
		/* links fail */
		link12.setFailureState(false);
		assertEquals (r123a.getFirstAvailableNodeAfterFailures() , n2);
		link23.setFailureState(false);
		assertEquals (r123a.getFirstAvailableNodeAfterFailures() , n3);
		n3.setFailureState(false);
		assertEquals (r123a.getFirstAvailableNodeAfterFailures() , null);
		link12.setFailureState(true);
		link23.setFailureState(true);
		n3.setFailureState(true);
		assertEquals (r123a.getFirstAvailableNodeAfterFailures() , n3);
		link23.setFailureState(false);
		assertEquals (r123a.getFirstAvailableNodeAfterFailures() , n3);
	}

	@Test
	public void testGetFirstAvailableNodeBeforeFailures() 
	{
		/* last node fails */
		assertEquals (r123a.getFirstAvailableNodeBeforeFailures() , n3);
		n3.setFailureState(false);
		assertEquals (r123a.getFirstAvailableNodeBeforeFailures() , n2);
		n3.setFailureState(true);
		assertEquals (r123a.getFirstAvailableNodeBeforeFailures() , n3);
		
		/* first node fails */
		n1.setFailureState(false);
		assertEquals (r123a.getFirstAvailableNodeBeforeFailures() , null);
		n1.setFailureState(true);
		assertEquals (r123a.getFirstAvailableNodeBeforeFailures() , n3);

		/* links fail */
		link12.setFailureState(false);
		assertEquals (r123a.getFirstAvailableNodeBeforeFailures() , n1);
		link23.setFailureState(false);
		assertEquals (r123a.getFirstAvailableNodeBeforeFailures() , n1);
		n3.setFailureState(false);
		assertEquals (r123a.getFirstAvailableNodeBeforeFailures() , n1);
		link12.setFailureState(true);
		link23.setFailureState(true);
		n3.setFailureState(true);
		assertEquals (r123a.getFirstAvailableNodeBeforeFailures() , n3);
		link23.setFailureState(false);
		assertEquals (r123a.getFirstAvailableNodeBeforeFailures() , n2);
	}

	@Test
	public void testGetIngressNode() 
	{
		assertEquals(r123a.getIngressNode() , n1);
	}

	@Test
	public void testGetLayer() {
		assertEquals(r123a.getLayer() , np.getNetworkLayerDefault());
	}

	@Test
	public void testGetLengthInKm() 
	{
		assertEquals(r123a.getLengthInKm() , 200 , 0.0);
	}

	@Test
	public void testGetNumberOfHops() 
	{
		assertEquals(sc123.getNumberOfHops() , 2);
		assertEquals(r123a.getNumberOfHops() , 2);
	}

	@Test
	public void testGetPropagationDelayInMiliseconds() 
	{
		assertEquals(sc123.getPropagationDelayInMiliseconds() , 10 + 200*1000  , 0.0);
		assertEquals(r123a.getPropagationDelayInMiliseconds() , 200*1000 , 0.0);
	}

	@Test
	public void testGetPropagationSpeedInKmPerSecond() 
	{
		assertEquals(sc123.getPropagationSpeedInKmPerSecond() , 1 , 0.0);
		assertEquals(r123a.getPropagationSpeedInKmPerSecond() , 1 , 0.0);
	}

	@Test
	public void testGetSeqLinksAndProtectionSegments() 
	{
		assertEquals (r123a.getSeqLinksAndProtectionSegments(), path13);
		assertEquals (sc123.getSeqLinksAndProtectionSegments(), path13);
		r123a.addProtectionSegment(segm13);
		r123a.setSeqLinksAndProtectionSegments(Collections.singletonList(segm13));
		assertEquals (r123a.getSeqLinksAndProtectionSegments(), Collections.singletonList(segm13));
	}

	@Test
	public void testGetSeqResourcesTraversed() 
	{
		assertEquals (r123a.getSeqResourcesTraversed(), Collections.emptyList());
		assertEquals (sc123.getSeqResourcesTraversed(), Collections.singletonList(res2));
	}

	@Test
	public void testGetInitialSeqResourcesTraversed() 
	{
		assertEquals (r123a.getInitialSeqResourcesTraversed(), Collections.emptyList());
		assertEquals (sc123.getInitialSeqResourcesTraversed(), Collections.singletonList(res2));
		List<NetworkElement> newSc = new LinkedList<NetworkElement> (); newSc.add (link12); newSc.add(res2backup); newSc.add(link23);
		sc123.setSeqLinksSegmentsAndResourcesOccupation(newSc , Collections.singletonMap(res2backup , 1.0));
		assertEquals (sc123.getInitialSeqResourcesTraversed(), Collections.singletonList(res2));
		assertEquals (sc123.getSeqResourcesTraversed(), Collections.singletonList(res2backup));
		assertEquals (sc123.getCurrentResourcesTraversed(), Collections.singleton(res2backup));
	}

	@Test
	public void testGetSeqLinksRealPath() 
	{
		assertEquals (r123a.getSeqLinksRealPath(), path13);
		assertEquals (sc123.getSeqLinksRealPath(), path13);
		List<NetworkElement> newSc = new LinkedList<NetworkElement> (); newSc.add (link12); newSc.add(res2backup); newSc.add(link23);
		sc123.setSeqLinksSegmentsAndResourcesOccupation(newSc , Collections.singletonMap(res2backup , 1.0));
		assertEquals (sc123.getSeqLinksRealPath(), path13);
		r123a.setSeqLinksAndProtectionSegments(Collections.singletonList(link13));
		assertEquals (r123a.getSeqLinksRealPath(), Collections.singletonList(link13));
	}

	@Test
	public void testGetSeqLinksRealPathAndResources() 
	{
		List<NetworkElement> currentSc = new LinkedList<NetworkElement> (); currentSc.add (link12); currentSc.add(res2); currentSc.add(link23);
		assertEquals (r123a.getSeqLinksRealPathAndResources(), path13);
		assertEquals (sc123.getSeqLinksRealPathAndResources(), currentSc);
		List<NetworkElement> newSc = new LinkedList<NetworkElement> (); newSc.add (link12); newSc.add(res2backup); newSc.add(link23);
		sc123.setSeqLinksSegmentsAndResourcesOccupation(newSc , Collections.singletonMap(res2backup , 1.0));
		assertEquals (sc123.getSeqLinksRealPathAndResources(), newSc);
		r123a.addProtectionSegment(segm13);
		r123a.setSeqLinksAndProtectionSegments(Collections.singletonList(segm13));
		assertEquals (r123a.getSeqLinksRealPathAndResources(), Collections.singletonList(link13));
	}

	@Test
	public void testGetSeqNodesRealPath() 
	{
		List<Node> n123 = new LinkedList<Node> (); n123.add(n1); n123.add(n2); n123.add(n3);
		List<Node> n13 = new LinkedList<Node> (); n13.add(n1); n13.add(n3);
		assertEquals (r123a.getSeqNodesRealPath(), n123);
		assertEquals (sc123.getSeqNodesRealPath(), n123);
		List<NetworkElement> newSc = new LinkedList<NetworkElement> (); newSc.add (link12); newSc.add(res2backup); newSc.add(link23);
		sc123.setSeqLinksSegmentsAndResourcesOccupation(newSc , Collections.singletonMap(res2backup , 1.0));
		assertEquals (sc123.getSeqNodesRealPath(), n123);
		r123a.addProtectionSegment(segm13);
		r123a.setSeqLinksAndProtectionSegments(Collections.singletonList(segm13));
		assertEquals (r123a.getSeqNodesRealPath(), n13);
		r123a.setSeqLinksAndProtectionSegments(Collections.singletonList(link13));
		assertEquals (r123a.getSeqNodesRealPath(), n13);
	}

	@Test
	public void testGetNumberOfTimesLinkIsTraversed() 
	{
		assertEquals (r123a.getNumberOfTimesLinkIsTraversed(link12) , 1);
		assertEquals (r123a.getNumberOfTimesLinkIsTraversed(link13) , 0);
	}

	@Test
	public void testGetNumberOfTimesResourceIsTraversed() 
	{
		assertEquals (sc123.getNumberOfTimesResourceIsTraversed(res2) , 1);
		assertEquals (sc123.getNumberOfTimesResourceIsTraversed(res2backup) , 0);
		List<NetworkElement> newSc = new LinkedList<NetworkElement> (); newSc.add (link12); newSc.add(res2backup); newSc.add(link23);
		sc123.setSeqLinksSegmentsAndResourcesOccupation(newSc , Collections.singletonMap(res2backup , 1.0));
		assertEquals (sc123.getNumberOfTimesResourceIsTraversed(res2) , 0);
		assertEquals (sc123.getNumberOfTimesResourceIsTraversed(res2backup) , 1);
	}

	@Test
	public void testGetSRGs() 
	{
		assertEquals (sc123.getSRGs() , Collections.emptySet());
		SharedRiskGroup srg = np.addSRG(1,1,null); 
		srg.addLink(link13);
		assertEquals (r123a.getSRGs() , Collections.emptySet());
		srg.addLink(link12);
		assertEquals (r123a.getSRGs() , Collections.singleton(srg));
		srg.addLink(link23);
		assertEquals (r123a.getSRGs() , Collections.singleton(srg));
		srg.removeLink(link12);
		srg.removeLink(link23);
		assertEquals (r123a.getSRGs() , Collections.emptySet());
		srg.addNode(n2);
		assertEquals (r123a.getSRGs() , Collections.singleton(srg));
		srg.remove ();
		srg = np.addSRG(1,1,null);
		srg.addLink(link12);
		assertEquals (r123a.getSRGs() , Collections.singleton(srg));
		r123a.setSeqLinksAndProtectionSegments(Collections.singletonList(link13));
		assertEquals (r123a.getSRGs() , Collections.emptySet());
	}

	@Test
	public void testHasLoops() 
	{
		assertTrue (!r123a.hasLoops());
		assertTrue (!sc123.hasLoops());
	}

	@Test
	public void testIsDown() 
	{
		assertTrue (!r123a.isDown());
		link12.setFailureState(false);
		assertTrue (r123a.isDown());
		link12.setFailureState(true);
		assertTrue (!r123a.isDown());
		r123a.setSeqLinksAndProtectionSegments(Collections.singletonList(link13));
		link12.setFailureState(false);
		assertTrue (!r123a.isDown());
	}

	@Test
	public void testRemove() 
	{
		r123a.remove();
		assertEquals (d13.getRoutes() , Collections.singleton(r123b));
		assertEquals (scd123.getRoutes() , Collections.singleton(sc123));
		sc123.remove();
		assertEquals (scd123.getRoutes() , Collections.emptySet());
	}

	@Test
	public void testRemoveProtectionSegmentFromBackupSegmentList() 
	{
		r123a.addProtectionSegment(segm13);
		assertEquals (r123a.getPotentialBackupProtectionSegments() , Collections.singleton(segm13));
		r123a.removeProtectionSegmentFromBackupSegmentList(segm13);
		assertEquals (r123a.getPotentialBackupProtectionSegments() , Collections.emptySet());
		r123a.addProtectionSegment(segm13);
		r123a.setSeqLinksAndProtectionSegments(Collections.singletonList(segm13));
		assertEquals (r123a.getPotentialBackupProtectionSegments() , Collections.singleton(segm13));
		try { r123a.removeProtectionSegmentFromBackupSegmentList(segm13); fail ("An exceptrion should have been thrown"); } catch (Exception e) { }
	}

	@Test
	public void testSetCarriedTraffic() 
	{
		try { sc123.setCarriedTraffic(1,1); fail ("An exception should be raised here"); } catch (Exception e) { }
		r123a.setCarriedTraffic(10,15);
		assertEquals(r123a.getCarriedTraffic() , 10 , 0.0);
		assertEquals(r123a.getOccupiedCapacity() , 15 , 0.0);
		link23.setFailureState(false);
		assertEquals(r123a.getCarriedTraffic() , 0 , 0.0);
		assertEquals(r123a.getOccupiedCapacity() , 0 , 0.0);
	}

	@Test
	public void testSetCarriedTrafficAndResourcesOccupationInformation() 
	{
		assertEquals(res2.getTraversingRouteOccupiedCapacity(sc123) , 1 , 0.0);
		sc123.setCarriedTrafficAndResourcesOccupationInformation(50,75,null);
		assertEquals(sc123.getCarriedTraffic() , 50 , 0.0);
		assertEquals(sc123.getOccupiedCapacity() , 75 , 0.0);
		assertEquals(res2.getTraversingRouteOccupiedCapacity(sc123) , 0 , 0.0);
		sc123.setCarriedTrafficAndResourcesOccupationInformation(10,15,Collections.singletonMap(res2 , 10.0));
		assertEquals(res2.getTraversingRouteOccupiedCapacity(sc123) , 10 , 0.0);
	}

}
