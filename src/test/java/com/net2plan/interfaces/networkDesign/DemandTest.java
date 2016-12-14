package com.net2plan.interfaces.networkDesign;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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

import com.net2plan.utils.Constants.RoutingCycleType;
import com.net2plan.utils.Constants.RoutingType;
import com.net2plan.utils.Pair;

public class DemandTest 
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
	private NetworkLayer lowerLayer , upperLayer;
	private Link upperLink12;
	
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
		this.upperLayer = np.addLayer("upperLayer" , "description" , "upperTrafficUnits" , "upperLinkCapUnits" , null);
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
		this.pathSc123 = new LinkedList<NetworkElement> (); pathSc123.add(link12); pathSc123.add(res2); pathSc123.add(link23); 
		this.sc123 = np.addServiceChain(scd123 , 100 , 300 , pathSc123 , Collections.singletonMap(res2 , 1.0) , null); 
		this.segm13 = np.addProtectionSegment(Collections.singletonList(link13) , 50 , null);
		this.upperLink12 = np.addLink(n1,n2,10,100,1,null,upperLayer);
		this.d12.coupleToUpperLayerLink(upperLink12);
	}

	@After
	public void tearDown() throws Exception 
	{
		np.checkCachesConsistency();
	}

	@Test
	public void testGetRoutes() 
	{
		Set<Route> twoRoutes = new HashSet<Route> (); twoRoutes.add (r123a); twoRoutes.add (r123b);
		assertEquals (d13.getRoutes() , twoRoutes);
		assertEquals (d12.getRoutes() , Collections.singleton(r12));
		assertEquals (scd123.getRoutes() , Collections.singleton(sc123));
		r123b.remove ();
		assertEquals (d13.getRoutes() , Collections.singleton(r123a));
	}

	@Test
	public void testGetWorseCasePropagationTimeInMs() 
	{
		assertEquals (d13.getWorseCasePropagationTimeInMs() , 200 , 0.0);
		assertEquals (d12.getWorseCasePropagationTimeInMs() , 100 , 0.0);
		r12.remove();
		assertEquals (d12.getWorseCasePropagationTimeInMs() , 0 , 0.0);
	}

	@Test
	public void testIsTraversingOversubscribedLinks() 
	{
		assertTrue (!d12.isTraversingOversubscribedLinks());
		assertTrue (!d13.isTraversingOversubscribedLinks());
		link12.setCapacity(1);
		assertTrue (d12.isTraversingOversubscribedLinks());
		assertTrue (!d13.isTraversingOversubscribedLinks());
	}

	@Test
	public void testIsTraversingOversubscribedResources() 
	{
		assertTrue (!d12.isTraversingOversubscribedResources());
		assertTrue (!scd123.isTraversingOversubscribedResources());
		sc123.setCarriedTrafficAndResourcesOccupationInformation(10 , 10 , Collections.singletonMap(res2 , 1000.0));
		assertTrue (scd123.isTraversingOversubscribedResources());
	}

	@Test
	public void testGetServiceChainSequenceOfTraversedResourceTypes() 
	{
		assertEquals(d12.getServiceChainSequenceOfTraversedResourceTypes() , Collections.emptyList());
		assertEquals(scd123.getServiceChainSequenceOfTraversedResourceTypes() , Collections.singletonList("type"));
	}

	@Test
	public void testSetServiceChainSequenceOfTraversedResourceTypes() 
	{
		try { d12.setServiceChainSequenceOfTraversedResourceTypes(Collections.singletonList("rrr")); fail ("Should fail"); } catch (Exception e) {}
		r12.remove();
		d12.setServiceChainSequenceOfTraversedResourceTypes(Collections.singletonList("rrr")); 
		assertEquals(d12.getServiceChainSequenceOfTraversedResourceTypes() , Collections.singletonList("rrr"));
	}

	@Test
	public void testGetLayer() 
	{
		assertEquals(d12.getLayer() , np.getNetworkLayerDefault());
		assertEquals(scd123.getLayer() , np.getNetworkLayerDefault());
	}

	@Test
	public void testIsBifurcated() 
	{
		assertTrue (!scd123.isBifurcated());
		assertTrue (d13.isBifurcated());
		assertTrue (!d12.isBifurcated());
	}

	@Test
	public void testIsBlocked() 
	{
		assertTrue (d12.isBlocked());
		assertTrue (d13.isBlocked());
		assertTrue (!scd123.isBlocked());
	}

	@Test
	public void testIsServiceChainRequest() 
	{
		assertTrue (!d12.isServiceChainRequest());
		assertTrue (!d13.isServiceChainRequest());
		assertTrue (scd123.isServiceChainRequest());
	}

	@Test
	public void testIsCoupled() 
	{
		assertTrue (!scd123.isCoupled());
		assertTrue (d12.isCoupled());
		assertTrue (!d13.isCoupled());
	}

	@Test
	public void testGetOfferedTraffic() 
	{
		assertEquals(d12.getOfferedTraffic() , 3 , 0.0);
		assertEquals(scd123.getOfferedTraffic() , 3 , 0.0);
	}

	@Test
	public void testGetForwardingRules() 
	{
		np.setRoutingType(RoutingType.HOP_BY_HOP_ROUTING , lowerLayer);
		Map<Pair<Demand,Link>,Double> frs = new HashMap<Pair<Demand,Link>,Double> ();
		frs.put(Pair.of(d12 , link12) , 1.0);
		assertEquals (d12.getForwardingRules() , frs);
		frs = new HashMap<Pair<Demand,Link>,Double> ();
		frs.put(Pair.of(d13 , link12) , 1.0);
		assertEquals (d13.getForwardingRules() , frs);
		frs = new HashMap<Pair<Demand,Link>,Double> ();
		frs.put(Pair.of(scd123 , link12) , 1.0);
		assertEquals (scd123.getForwardingRules() , frs);
	}

	@Test
	public void testGetCarriedTraffic() 
	{
		assertEquals (d12.getCarriedTraffic() , 1 , 0.0);
		assertEquals (d13.getCarriedTraffic() , 2 , 0.0);
		assertEquals (scd123.getCarriedTraffic() , 100 , 0.0);
	}

	@Test
	public void testGetBlockedTraffic() 
	{
		assertEquals (d12.getBlockedTraffic() , 2 , 0.0);
		assertEquals (d13.getBlockedTraffic() , 1 , 0.0);
		assertEquals (scd123.getBlockedTraffic() , 0, 0.0);
	}

	@Test
	public void testGetRoutingCycleType() 
	{
		assertEquals (d12.getRoutingCycleType() , RoutingCycleType.LOOPLESS);
		assertEquals (d13.getRoutingCycleType() , RoutingCycleType.LOOPLESS);
		assertEquals (scd123.getRoutingCycleType() , RoutingCycleType.LOOPLESS);
		
	}

	@Test
	public void testGetIngressNode() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetEgressNode() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetBidirectionalPair() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetCoupledLink() {
		fail("Not yet implemented");
	}

	@Test
	public void testCoupleToUpperLayerLink() {
		fail("Not yet implemented");
	}

	@Test
	public void testCoupleToNewLinkCreated() {
		fail("Not yet implemented");
	}

	@Test
	public void testDecouple() {
		fail("Not yet implemented");
	}

	@Test
	public void testRemoveAllForwardingRules() {
		fail("Not yet implemented");
	}

	@Test
	public void testComputeShortestPathRoutes() {
		fail("Not yet implemented");
	}

	@Test
	public void testRemove() {
		fail("Not yet implemented");
	}

	@Test
	public void testSetOfferedTraffic() {
		fail("Not yet implemented");
	}

	@Test
	public void testComputeRoutingFundamentalMatrixDemand() {
		fail("Not yet implemented");
	}

}
