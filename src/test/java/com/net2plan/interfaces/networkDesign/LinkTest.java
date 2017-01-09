package com.net2plan.interfaces.networkDesign;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class LinkTest 
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
		this.d123 = np.addMulticastDemand(n1 , endNodes , 100 , null , lowerLayer);
		this.t123 = np.addMulticastTree(d123 , 10,15,line123,null);
		this.tStar = np.addMulticastTree(d123 , 10,15,star,null);
		this.upperMdLink12 = np.addLink(n1,n2,10,100,1,null,upperLayer);
		this.upperMdLink13 = np.addLink(n1,n3,10,100,1,null,upperLayer);
		d123.couple(new HashSet<Link> (Arrays.asList(upperMdLink12 , upperMdLink13)));
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
	public void testGetOriginNode() 
	{
		assertEquals(upperLink12.getOriginNode(), n1);
		assertEquals(link12.getOriginNode(), n1);
		assertEquals(link23.getOriginNode(), n2);
		link12.setFailureState(false);
		assertEquals(upperLink12.getOriginNode(), n1);
		assertEquals(link12.getOriginNode(), n1);
		assertEquals(link23.getOriginNode(), n2);
	}

	@Test
	public void testGetDestinationNode() 
	{
		assertEquals(upperLink12.getDestinationNode(), n2);
		assertEquals(link12.getDestinationNode(), n2);
		assertEquals(link23.getDestinationNode(), n3);
		link12.setFailureState(false);
		assertEquals(upperLink12.getDestinationNode(), n2);
		assertEquals(link12.getDestinationNode(), n2);
		assertEquals(link23.getDestinationNode(), n3);
	}

	@Test
	public void testGetLayer() 
	{
		assertEquals(upperLink12.getLayer() , upperLayer);
		assertEquals(link12.getLayer() , lowerLayer);
		assertEquals(link23.getLayer() , lowerLayer);
		assertEquals(link13.getLayer() , lowerLayer);
	}

	@Test
	public void testGetCoupledDemand() 
	{
		assertEquals(upperLink12.getCoupledDemand() , d12);
		assertEquals(link12.getCoupledDemand() , null);
		assertEquals(link13.getCoupledDemand() , null);
	}

	@Test
	public void testGetCoupledMulticastDemand() 
	{
		assertEquals(upperMdLink12.getCoupledMulticastDemand(), d123);
		assertEquals(upperMdLink13.getCoupledMulticastDemand(), d123);
		assertEquals(upperLink12.getCoupledMulticastDemand(), null);
		assertEquals(link12.getCoupledMulticastDemand(), null);
		assertEquals(link13.getCoupledMulticastDemand(), null);
	}

	@Test
	public void testIsCoupled() 
	{
		assertTrue(upperMdLink12.isCoupled());
		assertTrue(upperMdLink13.isCoupled());
		assertTrue(upperLink12.isCoupled());
		assertTrue(!link12.isCoupled());
		assertTrue(!link13.isCoupled());
	}

	@Test
	public void testGetForwardingRules() 
	{
		fail("Not yet implemented");
	}

	@Test
	public void testGetCapacity() 
	{
		assertEquals(link12.getCapacity() , 100 , 0);
		assertEquals(link13.getCapacity() , 100 , 0);
		assertEquals(link23.getCapacity() , 100 , 0);
		assertEquals(upperLink12.getCapacity() , 1 , 0);
		assertEquals(upperMdLink12.getCapacity() , 20 , 0);
		assertEquals(upperMdLink12.getCapacity() , 20 , 0);
	}

	@Test
	public void testSetCapacity() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetCarriedTraffic() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetUtilization() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetOccupiedCapacity() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetOccupiedCapacityOnlyBackupRoutes() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetLengthInKm() {
		fail("Not yet implemented");
	}

	@Test
	public void testSetLengthInKm() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetPropagationSpeedInKmPerSecond() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetMulticastCarriedTraffic() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetMulticastOccupiedLinkCapacity() {
		fail("Not yet implemented");
	}

	@Test
	public void testSetPropagationSpeedInKmPerSecond() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetPropagationDelayInMs() {
		fail("Not yet implemented");
	}

	@Test
	public void testIsUp() {
		fail("Not yet implemented");
	}

	@Test
	public void testIsDown() {
		fail("Not yet implemented");
	}

	@Test
	public void testIsOversubscribed() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetSRGs() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetTraversingRoutes() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetTraversingTrees() {
		fail("Not yet implemented");
	}

	@Test
	public void testCoupleToLowerLayerDemand() {
		fail("Not yet implemented");
	}

	@Test
	public void testCoupleToNewDemandCreated() {
		fail("Not yet implemented");
	}

	@Test
	public void testRemoveAllForwardingRules() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetBidirectionalPair() {
		fail("Not yet implemented");
	}

	@Test
	public void testRemove() {
		fail("Not yet implemented");
	}

	@Test
	public void testSetFailureState() {
		fail("Not yet implemented");
	}

}
