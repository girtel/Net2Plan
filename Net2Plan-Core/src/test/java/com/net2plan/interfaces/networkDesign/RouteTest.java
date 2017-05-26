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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.net2plan.utils.Triple;

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
	private Route segm13;
	
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
		this.path13 = Arrays.asList(link12 , link23);
		this.r123a = np.addRoute(d13,1,1.5,path13,null);
		this.segm13 = np.addServiceChain(d13 , 0.0 , Arrays.asList(25.0) , Arrays.asList(link13) , null);
		this.r123a.addBackupRoute(segm13);
		this.r123b = np.addRoute(d13,1,1.5,path13,null);
		this.res2 = np.addResource("type" , "name" , n2 , 100 , "Mbps" , null , 10 , null);
		this.res2backup = np.addResource("type" , "name" , n2 , 100 , "Mbps" , null , 10 , null);
		this.scd123 = np.addDemand(n1 , n3 , 3 , null);
		this.scd123.setServiceChainSequenceOfTraversedResourceTypes(Collections.singletonList("type"));
		this.pathSc123 = new LinkedList<NetworkElement> (); pathSc123.add(link12); pathSc123.add(res2); pathSc123.add(link23); 
		this.sc123 = np.addServiceChain(scd123 , 100 , Arrays.asList(100.0 , 1.0 , 200.0) , Arrays.asList(link12 , res2 , link23) , null);
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
	public void testGetRoutesIAmBackup() 
	{
		assertEquals (r12.getRoutesIAmBackup() , Collections.emptySet());
		assertEquals (r123a.getRoutesIAmBackup() , Collections.emptySet());
		assertEquals (segm13.getRoutesIAmBackup() , Collections.singleton(r123a));
	}

	@Test
	public void testGetBackupRoutes() 
	{
		assertEquals (r12.getBackupRoutes() , Collections.emptyList());
		assertEquals (r123a.getBackupRoutes() , Collections.singletonList(segm13));
		assertEquals (segm13.getBackupRoutes() , Collections.emptyList());
	}

	@Test
	public void testGetInitialState() 
	{
		Triple<Double, List<NetworkElement>, List<Double>> res;
		res = sc123.getInitialState();
		assertEquals (res.getFirst() , 100 , 0);
		assertEquals (res.getSecond() , Arrays.asList(link12 , res2 , link23));
		assertEquals (res.getThird() , Arrays.asList(100.0, 1.0 , 200.0));
		sc123.setPath(1, Arrays.asList(link12 , res2backup , link23), Arrays.asList(1.0 , 2.0 , 3.0));
		assertEquals (res.getFirst() , 100 , 0);
		assertEquals (res.getSecond() , Arrays.asList(link12 , res2 , link23));
		assertEquals (res.getThird() , Arrays.asList(100.0, 1.0 , 200.0));
	}

	@Test
	public void testGetPath() 
	{
		assertEquals (r12.getPath() , Arrays.asList(link12));
		assertEquals (r123a.getPath() , Arrays.asList(link12 , link23));
		assertEquals (r123b.getPath() , Arrays.asList(link12 , link23));
		assertEquals (sc123.getPath() , Arrays.asList(link12 , res2 , link23));
		sc123.setPath(1, Arrays.asList(link12 , res2backup , link23), Arrays.asList(1.0 , 2.0 , 3.0));
		assertEquals (sc123.getPath() , Arrays.asList(link12 , res2backup , link23));
	}

	@Test
	public void testIsBackupRoute() 
	{
		assertTrue (segm13.isBackupRoute());
		assertTrue (!r12.isBackupRoute());
		assertTrue (!r123a.isBackupRoute());
		assertTrue (!r123b.isBackupRoute());
		assertTrue (!sc123.isBackupRoute());
	}

	@Test
	public void testHasBackupRoutes() 
	{
		assertTrue (!segm13.hasBackupRoutes());
		assertTrue (!r12.hasBackupRoutes());
		assertTrue (r123a.hasBackupRoutes());
		assertTrue (!r123b.hasBackupRoutes());
		assertTrue (!sc123.hasBackupRoutes());
	}

	@Test
	public void testAddBackupRoute() 
	{
		final Route newSegmScd = np.addServiceChain(scd123, 0, Arrays.asList(1.0 , 2.0 , 3.0), Arrays.asList(link12, res2backup , link23), null);
		sc123.addBackupRoute(newSegmScd);
		assertEquals(sc123.getBackupRoutes(), Arrays.asList(newSegmScd));
		try { sc123.addBackupRoute(r123a); fail ("Should not accept a backup of different demand"); } catch (Net2PlanException e) { }
		try { sc123.addBackupRoute(newSegmScd); fail ("Should not accept the same backup twice"); } catch (Net2PlanException e) { }
		final Route newSegm123 = np.addServiceChain(d13, 0, Arrays.asList(1.0 , 2.0), Arrays.asList(link12, link23), null);
		assertEquals(r123a.getBackupRoutes(), Arrays.asList(segm13));
		r123a.addBackupRoute(newSegm123);
		assertEquals(r123a.getBackupRoutes(), Arrays.asList(segm13 , newSegm123));
	}

	@Test
	public void testRemoveBackupRoute() 
	{
		final Route newSegmScd = np.addServiceChain(scd123, 0, Arrays.asList(1.0 , 2.0 , 3.0), Arrays.asList(link12, res2backup , link23), null);
		sc123.addBackupRoute(newSegmScd);
		assertEquals(sc123.getBackupRoutes(), Arrays.asList(newSegmScd));
		try { sc123.removeBackupRoute(null); fail ("Should not accept this"); } catch (Net2PlanException e) { }
		try { sc123.removeBackupRoute(r12);  fail (""); } catch (Net2PlanException e) { }
		sc123.removeBackupRoute(newSegmScd);
		assertEquals(sc123.getBackupRoutes(), Arrays.asList());
		final Route newSegm123 = np.addServiceChain(d13, 0, Arrays.asList(1.0 , 2.0), Arrays.asList(link12, link23), null);
		r123a.addBackupRoute(newSegm123);
		assertEquals(r123a.getBackupRoutes(), Arrays.asList(segm13 , newSegm123));
		r123a.removeBackupRoute(segm13);
		assertEquals(r123a.getBackupRoutes(), Arrays.asList(newSegm123));
		r123a.removeBackupRoute(newSegm123);
		assertEquals(r123a.getBackupRoutes(), Arrays.asList());
	}

	@Test
	public void testGetCarriedTraffic() 
	{
		assertEquals(sc123.getCarriedTraffic() ,  100.0 , 0.0);
		sc123.setCarriedTraffic(0.0, null);
		assertEquals(sc123.getCarriedTraffic() ,  0.0 , 0.0);
		sc123.setCarriedTraffic(1.0, null);
		assertEquals(sc123.getCarriedTraffic() ,  1.0 , 0.0);
		link12.setFailureState(false);
		assertEquals(sc123.getCarriedTraffic() ,  0.0 , 0.0);
	}

	@Test
	public void testGetOccupiedCapacity() 
	{
		this.sc123 = np.addServiceChain(scd123 , 100 , Arrays.asList(100.0 , 1.0 , 200.0) , Arrays.asList(link12 , res2 , link23) , null);
		assertEquals(sc123.getOccupiedCapacity(), 100.0 , 0.0);
		assertEquals(sc123.getOccupiedCapacity(link12), 100.0 , 0.0);
		assertEquals(sc123.getOccupiedCapacity(res2), 1.0 , 0.0);
		assertEquals(sc123.getOccupiedCapacity(link23), 200.0 , 0.0);
		assertEquals(sc123.getOccupiedCapacity(link13), 0.0 , 0.0);
		link12.setFailureState(false);
		assertEquals(sc123.getOccupiedCapacity(), 0.0 , 0.0);
		assertEquals(sc123.getOccupiedCapacity(link12), 0.0 , 0.0);
		assertEquals(sc123.getOccupiedCapacity(res2), 0.0 , 0.0);
		assertEquals(sc123.getOccupiedCapacity(link23), 0.0 , 0.0);
		assertEquals(sc123.getOccupiedCapacity(link13), 0.0 , 0.0);
	}

	@Test
	public void testGetCarriedTrafficInNoFailureState() 
	{
		assertEquals(sc123.getCarriedTrafficInNoFailureState() ,  100.0 , 0.0);
		sc123.setCarriedTraffic(0.0, null);
		assertEquals(sc123.getCarriedTrafficInNoFailureState() ,  0.0 , 0.0);
		sc123.setCarriedTraffic(1.0, null);
		assertEquals(sc123.getCarriedTrafficInNoFailureState() ,  1.0 , 0.0);
		link12.setFailureState(false);
		assertEquals(sc123.getCarriedTrafficInNoFailureState() ,  1.0 , 0.0);
	}

	@Test
	public void testGetOccupiedCapacityInNoFailureState() 
	{
		this.sc123 = np.addServiceChain(scd123 , 100 , Arrays.asList(100.0 , 1.0 , 200.0) , Arrays.asList(link12 , res2 , link23) , null);
		assertEquals(sc123.getOccupiedCapacityInNoFailureState(), 100.0 , 0.0);
		assertEquals(sc123.getOccupiedCapacityInNoFailureState(link12), 100.0 , 0.0);
		assertEquals(sc123.getOccupiedCapacityInNoFailureState(res2), 1.0 , 0.0);
		assertEquals(sc123.getOccupiedCapacityInNoFailureState(link23), 200.0 , 0.0);
		assertEquals(sc123.getOccupiedCapacityInNoFailureState(link13), 0.0 , 0.0);
		link12.setFailureState(false);
		assertEquals(sc123.getOccupiedCapacityInNoFailureState(), 100.0 , 0.0);
		assertEquals(sc123.getOccupiedCapacityInNoFailureState(link12), 100.0 , 0.0);
		assertEquals(sc123.getOccupiedCapacityInNoFailureState(res2), 1.0 , 0.0);
		assertEquals(sc123.getOccupiedCapacityInNoFailureState(link23), 200.0 , 0.0);
		assertEquals(sc123.getOccupiedCapacityInNoFailureState(link13), 0.0 , 0.0);
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
	public void testGetLayer() 
	{
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
	public void testGetSeqOccupiedCapacitiesIfNotFailing() 
	{
		assertEquals (sc123.getSeqOccupiedCapacitiesIfNotFailing(), Arrays.asList(100.0 , 1.0 , 200.0));
		List<NetworkElement> newSc = new LinkedList<NetworkElement> (); newSc.add (link12); newSc.add(res2backup); newSc.add(link23);
		sc123.setPath(sc123.getCarriedTraffic(), newSc , Arrays.asList(1.0 , 2.0 , 3.0));
		assertEquals (sc123.getSeqOccupiedCapacitiesIfNotFailing(), Arrays.asList(1.0 , 2.0 , 3.0));
	}

	@Test
	public void testGetSeqResourcesTraversed() 
	{
		assertEquals (r123a.getSeqResourcesTraversed(), Collections.emptyList());
		assertEquals (sc123.getSeqResourcesTraversed(), Collections.singletonList(res2));
	}

	@Test
	public void testGetSeqLinks()
	{
		assertEquals (r123a.getSeqLinks(), path13);
		assertEquals (sc123.getSeqLinks(), path13);
		List<NetworkElement> newSc = new LinkedList<NetworkElement> (); newSc.add (link12); newSc.add(res2backup); newSc.add(link23);
		sc123.setPath(sc123.getCarriedTraffic(), newSc , Arrays.asList(1.0 , 2.0 , 3.0));
		assertEquals (sc123.getSeqLinks(), path13);
		r123a.setSeqLinks(Collections.singletonList(link13));
		assertEquals (r123a.getSeqLinks(), Collections.singletonList(link13));
	}

	@Test
	public void testGetSeqNodes() 
	{
		assertEquals (r123a.getSeqNodes(), Arrays.asList(n1,n2,n3));
		assertEquals (sc123.getSeqNodes(), Arrays.asList(n1,n2,n3));
		sc123.setPath(sc123.getCarriedTraffic(), Arrays.asList(link12 , res2backup , link23) , Arrays.asList(1.0 , 2.0 , 3.0));
		assertEquals (sc123.getSeqNodes(), Arrays.asList(n1 , n2 , n3));
		r123a.setSeqLinks(Collections.singletonList(link13));
		assertEquals (r123a.getSeqNodes(), Arrays.asList(n1 , n3));
	}

	@Test
	public void testGetNumberOfTimesIsTraversed() 
	{
		assertEquals (r123a.getNumberOfTimesIsTraversed(link12) , 1);
		assertEquals (r123a.getNumberOfTimesIsTraversed(link13) , 0);
		assertEquals (sc123.getNumberOfTimesIsTraversed(res2) , 1);
		assertEquals (sc123.getNumberOfTimesIsTraversed(res2backup) , 0);
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
		r123a.setSeqLinks(Collections.singletonList(link13));
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
		r123a.setSeqLinks(Collections.singletonList(link13));
		link12.setFailureState(false);
		assertTrue (!r123a.isDown());
	}

	@Test
	public void testRemove() 
	{
		r123a.remove();
		assertEquals (d13.getRoutes() , new HashSet<Route> (Arrays.asList(r123b , segm13)));
		assertEquals (scd123.getRoutes() , Collections.singleton(sc123));
		sc123.remove();
		assertEquals (scd123.getRoutes() , Collections.emptySet());
	}

	@Test
	public void testSetCarriedTrafficDoubleDouble() 
	{
		sc123.setCarriedTraffic(4 ,  5);
		assertEquals(sc123.getCarriedTraffic() , 4 , 0.0);
		assertEquals(sc123.getOccupiedCapacity(link12) , 5 , 0.0);
		assertEquals(sc123.getOccupiedCapacity(res2) , 5 , 0.0);
		assertEquals(sc123.getOccupiedCapacity(res2backup) , 0 , 0.0);
		assertEquals(sc123.getOccupiedCapacity(link23) , 5 , 0.0);
	}

	@Test
	public void testSetCarriedTrafficDoubleListOfDouble() 
	{
		sc123.setCarriedTraffic(5 ,  Arrays.asList(1.0,2.0,3.0));
		assertEquals(sc123.getCarriedTraffic() , 5 , 0.0);
		assertEquals(sc123.getOccupiedCapacity(link12) , 1 , 0.0);
		assertEquals(sc123.getOccupiedCapacity(res2) , 2 , 0.0);
		assertEquals(sc123.getOccupiedCapacity(link23) , 3 , 0.0);
	}

	@Test
	public void testSetPath() 
	{
		assertEquals (r123a.getCarriedTraffic() , 1.0 , 0.0);
		assertEquals (r123a.getSeqOccupiedCapacitiesIfNotFailing() , Arrays.asList(1.5, 1.5));
		assertEquals (r123a.getSeqLinks() , Arrays.asList(link12, link23));
		assertEquals (r123a.getSeqResourcesTraversed() , Arrays.asList());
		assertEquals (r123a.getPath() , Arrays.asList(link12 , link23));
		r123a.setPath(5 , Arrays.asList(link12 , link23) , Arrays.asList(1.0 , 3.0));
		assertEquals (r123a.getCarriedTraffic() , 5.0 , 0.0);
		assertEquals (r123a.getSeqOccupiedCapacitiesIfNotFailing() , Arrays.asList(1.0, 3.0));
		assertEquals (r123a.getSeqLinks() , Arrays.asList(link12, link23));
		assertEquals (r123a.getSeqResourcesTraversed() , Arrays.asList());
		assertEquals (r123a.getPath() , Arrays.asList(link12 , link23));

		this.sc123 = np.addServiceChain(scd123 , 100 , Arrays.asList(100.0 , 1.0 , 200.0) , Arrays.asList(link12 , res2 , link23) , null);

		assertEquals (sc123.getCarriedTraffic() , 100.0 , 0.0);
		assertEquals (sc123.getSeqOccupiedCapacitiesIfNotFailing() , Arrays.asList(100.0 , 1.0 , 200.0));
		assertEquals (sc123.getSeqLinks() , Arrays.asList(link12, link23));
		assertEquals (sc123.getSeqResourcesTraversed() , Arrays.asList(res2));
		assertEquals (sc123.getPath() , Arrays.asList(link12 , res2 , link23));
		sc123.setPath(5 , Arrays.asList(link12 , res2backup , link23) , Arrays.asList(1.0 , 2.0 , 3.0));
		assertEquals (sc123.getCarriedTraffic() , 5.0 , 0.0);
		assertEquals (sc123.getSeqOccupiedCapacitiesIfNotFailing() , Arrays.asList(1.0, 2.0 , 3.0));
		assertEquals (sc123.getSeqLinks() , Arrays.asList(link12, link23));
		assertEquals (sc123.getSeqResourcesTraversed() , Arrays.asList(res2backup));
		assertEquals (sc123.getPath() , Arrays.asList(link12 , res2backup , link23));
}

	@Test
	public void testSetSeqLinks() 
	{
		r12.setSeqLinks(Arrays.asList(link12));
		assertEquals (r12.getSeqLinks() , Arrays.asList(link12));
		r123a.setSeqLinks(Arrays.asList(link13));
		assertEquals (r123a.getSeqLinks() , Arrays.asList(link13));
		assertEquals (r123a.getCarriedTraffic() , 1.0 , 0.0);
		assertEquals (r123a.getOccupiedCapacity(link13) , 1.5 , 0.0);
		r123a.setPath(5 , Arrays.asList(link12 , link23) , Arrays.asList(1.0 , 3.0));
		try { r123a.setSeqLinks(Arrays.asList(link13)); fail ("Cannot accep this with different occupations per link"); } catch (Exception e) { }
	}

	@Test
	public void testGetSeqResources() 
	{
		assertEquals (r12.getSeqResourcesTraversed() , Arrays.asList());
		assertEquals (r123a.getSeqResourcesTraversed() , Arrays.asList());
		assertEquals (r123b.getSeqResourcesTraversed() , Arrays.asList());
		assertEquals (sc123.getSeqResourcesTraversed() , Arrays.asList(res2));
		link12.setFailureState(false);
		assertEquals (sc123.getSeqResourcesTraversed() , Arrays.asList(res2));
		n2.setFailureState(false);
		assertEquals (sc123.getSeqResourcesTraversed() , Arrays.asList(res2));
		sc123.setPath(sc123.getCarriedTraffic(), Arrays.asList(link12 , res2backup , link23) , Arrays.asList(1.0 , 2.0 , 3.0));
		assertEquals (sc123.getSeqResourcesTraversed() , Arrays.asList(res2backup));
	}

	
	/////////////////////
	

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
		assertEquals (sc123.getOccupiedCapacity() , 100 , 0.0);
		assertEquals (sc123.getOccupiedCapacityInNoFailureState(), 100 , 0.0);
		assertEquals (sc123.getOccupiedCapacity(link12) , 100 , 0.0);
		assertEquals (sc123.getOccupiedCapacity(res2) , 1.0 , 0.0);
		assertEquals (sc123.getOccupiedCapacity(link23) , 200 , 0.0);
		assertEquals (sc123.getOccupiedCapacity(link13) , 0 , 0.0);
		
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
		assertEquals (sc123.getOccupiedCapacityInNoFailureState(), 100 , 0.0);
		assertEquals (sc123.getOccupiedCapacity(link12) , 0 , 0.0);
		assertEquals (sc123.getOccupiedCapacity(link23) , 0 , 0.0);
		assertEquals (sc123.getOccupiedCapacity(link13) , 0 , 0.0);
	}


	@Test
	public void testSetCarriedTraffic() 
	{
		sc123.setCarriedTraffic(1,2.0); 
		assertEquals(sc123.getCarriedTraffic() , 1 , 0.0);
		assertEquals(sc123.getOccupiedCapacity() , 2.0 , 0.0);
		assertEquals(sc123.getSeqOccupiedCapacitiesIfNotFailing() , Arrays.asList(2.0,2.0,2.0));
		r123a.setCarriedTraffic(10,15);
		assertEquals(r123a.getCarriedTraffic() , 10 , 0.0);
		assertEquals(r123a.getOccupiedCapacity() , 15 , 0.0);
		link23.setFailureState(false);
		assertEquals(r123a.getCarriedTraffic() , 0 , 0.0);
		assertEquals(r123a.getOccupiedCapacity() , 0 , 0.0);
	}

	@Test
	public void testSetCarriedTrafficAndPath() 
	{
		sc123.setPath(5 ,  Arrays.asList(link12,res2backup,link23) , Arrays.asList(1.0,2.0,3.0));
		assertEquals(sc123.getCarriedTraffic() , 5 , 0.0);
		assertEquals(sc123.getOccupiedCapacity(link12) , 1 , 0.0);
		assertEquals(sc123.getOccupiedCapacity(res2) , 0 , 0.0);
		assertEquals(sc123.getOccupiedCapacity(res2backup) , 2 , 0.0);
		assertEquals(sc123.getOccupiedCapacity(link23) , 3 , 0.0);
		r123a.setPath(1, Arrays.asList(link13), Arrays.asList(11.0));
		assertEquals(r123a.getCarriedTraffic() , 1 , 0.0);
		assertEquals(r123a.getOccupiedCapacity(link13) , 11 , 0.0);
		assertEquals(r123a.getOccupiedCapacity(link12) , 0 , 0.0);
	}

}
