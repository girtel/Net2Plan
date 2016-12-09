/**
 * 
 */
package com.net2plan.interfaces.networkDesign;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Pablo
 *
 */
public class ResourceTest 
{
	private NetPlan np = null;
	private Node hostNode , endDemandNode;
	private Link interLink;
	private Demand demandUpper ,demandBase;
	private Route serviceChainUpper, serviceChainBase;
	private Resource upperResource = null;
	private Resource baseResource = null;

	// upper resource: capacity 10, occupied 5 in base
	// base resource: capacity 10: occupied 1+5
	// serviceChainUpper (carries 100, occupies 200, occupies 1.0 in upper)
	// serviceChainBase (carries 100, occupies 300, occupies 1.0 in base)
	
	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception 
	{
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception 
	{
		this.np = new NetPlan ();
		this.hostNode = this.np.addNode(0 , 0 , "node0" , null);
		this.endDemandNode = np.addNode(0 , 0 , "node1" , null);
		this.interLink = np.addLink(hostNode,endDemandNode,100,100,1,null);
		this.demandUpper = np.addDemand(hostNode , endDemandNode , 3 , null);
		this.demandUpper.setServiceChainSequenceOfTraversedResourceTypes(Collections.singletonList("upperType"));
		this.demandBase = np.addDemand(hostNode , endDemandNode , 3 , null);
		this.demandBase.setServiceChainSequenceOfTraversedResourceTypes(Collections.singletonList("baseType"));
		
		/* create resources */
		this.baseResource = np.addResource("baseType" , "baseName" , hostNode , 10 , "Mbps" , null , 1 , null);
		assertTrue (baseResource.getIndex() == 0);
		assertTrue (np.getNumberOfResources() == 1);
		
		this.upperResource = np.addResource("upperType", "upperName" , hostNode , 10 , "Mbps", Collections.singletonMap(baseResource , 5.0) , 1 , null);
		assertTrue (upperResource.getIndex() == 1);
		assertTrue (np.getNumberOfResources() == 2);
		
		/* create service chain */
		List<NetworkElement> pathUpper = new LinkedList<NetworkElement> (); pathUpper.add(upperResource); pathUpper.add(interLink);
		this.serviceChainUpper = np.addServiceChain(demandUpper , 100 , 200 , pathUpper , Collections.singletonMap(upperResource , 1.0) , null);
		List<NetworkElement> pathBase = new LinkedList<NetworkElement> (); pathBase.add(baseResource); pathBase.add(interLink);
		this.serviceChainBase = np.addServiceChain(demandBase , 100 , 300 , pathBase , Collections.singletonMap(baseResource , 1.0) , null);
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception 
	{
	}


	@Test
	public void testCheckCaches() 
	{
		np.checkCachesConsistency();
	}

	@Test
	public void testGetBaseResources ()
	{
		assertTrue (upperResource.getBaseResources().size() == 1);
		assertTrue (upperResource.getBaseResources().iterator().next() == baseResource);
	}
	
	@Test
	public void testGetCapacity ()
	{
		assertTrue (upperResource.getCapacity() == 10);
		assertTrue (baseResource.getCapacity() == 10);
	}
	
	@Test
	public void testGetCapacityMeasurementUnits ()
	{
		assertTrue(baseResource.getCapacityMeasurementUnits().equals("Mbps"));
	}
	
	@Test
	public void testGetCapacityOccupiedByUpperResourceMap ()
	{
		assertTrue(baseResource.getCapacityOccupiedByUpperResource(upperResource) == 5.0);
	}
	
	@Test
	public void testGetCapacityOccupiedInBaseResource ()
	{
		assertTrue(upperResource.getCapacityOccupiedInBaseResource(baseResource) == 5.0);
	}

	@Test
	public void testGetHostNode ()
	{
		assertTrue(upperResource.getHostNode() == np.getNode(0));
	}

	@Test
	public void testGetOccupiedCapacity ()
	{
		assertEquals(upperResource.getOccupiedCapacity() , 1.0 , 0);
		assertEquals(baseResource.getOccupiedCapacity() , 6.0 , 0);
	}
	
	@Test
	public void testGetTraversingDemands ()
	{
		assertEquals(upperResource.getTraversingDemands().iterator().next() , demandUpper);
		assertEquals(baseResource.getTraversingDemands().iterator().next() , demandBase);
	}
	
	@Test
	public void testGetTraversingRouteOccupiedCapacity ()
	{
		assertEquals(upperResource.getTraversingRouteOccupiedCapacity(demandUpper.getRoutes().iterator().next()) , 1.0 , 0);
		assertEquals(baseResource.getTraversingRouteOccupiedCapacity(demandBase.getRoutes().iterator().next()) , 1.0 , 0);
		interLink.setFailureState(false);
		assertEquals(upperResource.getTraversingRouteOccupiedCapacity(demandUpper.getRoutes().iterator().next()) , 0.0 , 0);
		assertEquals(baseResource.getTraversingRouteOccupiedCapacity(demandBase.getRoutes().iterator().next()) , 0.0 , 0);
	}

	@Test
	public void testGetTraversingRoutes ()
	{
		assertEquals (baseResource.getTraversingRoutes() , Collections.singleton(serviceChainBase));
		assertEquals (upperResource.getTraversingRoutes() , Collections.singleton(serviceChainUpper));
	}

	@Test
	public void testGetUpperResources ()
	{
		assertEquals (baseResource.getUpperResources() , Collections.singleton(upperResource));
		assertEquals (upperResource.getUpperResources() , new HashSet<Resource> ());
	}
	
	@Test
	public void testIsOverSubscribed ()
	{
		assertTrue (!baseResource.isOversubscribed());
		assertTrue (!upperResource.isOversubscribed());
		serviceChainBase.setCarriedTrafficAndResourcesOccupationInformation(20,20,null);
		assertTrue (!baseResource.isOversubscribed());
		assertTrue (!upperResource.isOversubscribed());
		serviceChainBase.setCarriedTrafficAndResourcesOccupationInformation(20,20,Collections.singletonMap(baseResource , 100.0));
		assertTrue (baseResource.isOversubscribed());
		serviceChainUpper.setCarriedTrafficAndResourcesOccupationInformation(20,20,Collections.singletonMap(upperResource , 100.0));
		assertTrue (upperResource.isOversubscribed());
		np.checkCachesConsistency();
	}

	@Test
	public void testRemove1 ()
	{
		baseResource.remove();
		np.checkCachesConsistency();
		assertEquals(upperResource.getNetPlan() , null);
		assertEquals(baseResource.getNetPlan() , null);
		assertEquals(serviceChainBase.getNetPlan() , null);
		assertEquals(serviceChainUpper.getNetPlan() , null);
	}

	@Test
	public void testRemove2 ()
	{
		upperResource.remove();
		np.checkCachesConsistency();
		assertEquals(upperResource.getNetPlan() , null);
		assertEquals(serviceChainUpper.getNetPlan() , null);
		assertTrue(baseResource.getNetPlan() != null);
		assertTrue(serviceChainBase.getNetPlan() != null);
	}


	@Test
	public void testSetCapacity ()
	{
		assertEquals(upperResource.getOccupiedCapacity() , 1.0 , 0.0);
		assertEquals(baseResource.getOccupiedCapacity() , 6.0 , 0.0);
		upperResource.setCapacity(10 , Collections.singletonMap(baseResource , 3.0));
		np.checkCachesConsistency();
		assertEquals(upperResource.getCapacity() , 10.0 , 0.0);
		assertEquals(upperResource.getOccupiedCapacity() , 1.0 , 0.0);
		assertEquals(baseResource.getCapacity() , 10.0 , 0.0);
		assertEquals(baseResource.getOccupiedCapacity() , 4.0 , 0.0);
	}

	@Test
	public void testCopy ()
	{
		NetPlan np2 = np.copy();
		np2.checkCachesConsistency();
		assertEquals(np2.getResource(0).getId() , np.getResource(0).getId());
		assertEquals(np2.getResource(1).getId() , np.getResource(1).getId());
		assertEquals(np2.getRoute(0).getId() , np.getRoute(0).getId());
		assertEquals(np2.getRoute(1).getId() , np.getRoute(1).getId());
	}

	@Test
	public void testCopyFrom ()
	{
		NetPlan np2 = new NetPlan ();
		np2.copyFrom(np.copy());
		np2.checkCachesConsistency();
		assertEquals(np2.getResource(0).getId() , np.getResource(0).getId());
		assertEquals(np2.getResource(1).getId() , np.getResource(1).getId());
		assertEquals(np2.getRoute(0).getId() , np.getRoute(0).getId());
		assertEquals(np2.getRoute(1).getId() , np.getRoute(1).getId());
	}

	@Test
	public void testFailureRoute ()
	{
		assertEquals(upperResource.getOccupiedCapacity() , 1.0 , 0.0);
		assertEquals(baseResource.getOccupiedCapacity() , 6.0 , 0.0);
		interLink.setFailureState(false);
		assertEquals(serviceChainBase.getCarriedTraffic() , 0 , 0.0);
		assertEquals(serviceChainUpper.getCarriedTraffic() , 0 , 0.0);
		assertEquals(serviceChainBase.getOccupiedCapacity() , 0 , 0.0);
		assertEquals(serviceChainUpper.getOccupiedCapacity() , 0 , 0.0);
		assertEquals(serviceChainBase.getOccupiedCapacityInNoFailureState() , 300.0 , 0.0);
		assertEquals(serviceChainUpper.getOccupiedCapacityInNoFailureState() , 200.0 , 0.0);
		assertEquals(upperResource.getOccupiedCapacity() , 0.0 , 0.0);
		assertEquals(baseResource.getOccupiedCapacity() , 5.0 , 0.0);
	}
	
	@Test
	public void testReadSave ()
	{
		File file = null;
		try
		{
			file = File.createTempFile("testN2p" , "n2p");
		} catch (Exception e) { Assert.fail ("could not make the test: no temprary file creation possible"); }
		assertTrue (file != null);
		np.saveToFile(file);
		NetPlan np2 = new NetPlan (file);
		np2.checkCachesConsistency();
	}
	
}
