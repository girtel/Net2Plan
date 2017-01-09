package com.net2plan.interfaces.networkDesign;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class MulticastDemandTest 
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
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testGetLayer() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetWorseCasePropagationTimeInMs() {
		fail("Not yet implemented");
	}

	@Test
	public void testIsTraversingOversubscribedLinks() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetIngressNode() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetEgressNodes() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetOfferedTraffic() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetCarriedTraffic() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetBlockedTraffic() {
		fail("Not yet implemented");
	}

	@Test
	public void testComputeMinimumCostMulticastTrees() {
		fail("Not yet implemented");
	}

	@Test
	public void testIsBifurcated() {
		fail("Not yet implemented");
	}

	@Test
	public void testIsBlocked() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetMulticastTrees() {
		fail("Not yet implemented");
	}

	@Test
	public void testCouple() {
		fail("Not yet implemented");
	}

	@Test
	public void testCoupleToNewLinksCreated() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetCoupledLinks() {
		fail("Not yet implemented");
	}

	@Test
	public void testIsCoupled() {
		fail("Not yet implemented");
	}

	@Test
	public void testDecouple() {
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

}
