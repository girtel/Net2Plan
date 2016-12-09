/**
 * 
 */
package com.net2plan.interfaces.networkDesign;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
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
	private Route route;
	private Resource upperResource = null;
	private Resource baseResource = null;

	
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
		
		Map<Resource,Double> occupationInBaseResource = new HashMap<Resource,Double> ();
		occupationInBaseResource.put(baseResource , 5.0);
		this.upperResource = np.addResource("upperType", "upperName" , hostNode , 10 , "Mbps", occupationInBaseResource , 1 , null);
		assertTrue (upperResource.getIndex() == 1);
		assertTrue (np.getNumberOfResources() == 2);
		
		/* create service chain */
		List<NetworkElement> pathUpper = new LinkedList<NetworkElement> (); pathUpper.add(upperResource); pathUpper.add(interLink);
		np.addServiceChain(demandUpper , 100 , 200 , pathUpper , Collections.singletonMap(upperResource , 1.0) , null);
		List<NetworkElement> pathBase = new LinkedList<NetworkElement> (); pathBase.add(baseResource); pathBase.add(interLink);
		np.addServiceChain(demandBase , 100 , 300 , pathBase , Collections.singletonMap(baseResource , 1.0) , null);
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
	}

	@Test
	public void testGetTraversingRoutes ()
	{
	}

	
	//	/**
//	 * Test method for {@link com.net2plan.interfaces.networkDesign.Resource#setUpperResourceOccupiedCapacity(com.net2plan.interfaces.networkDesign.Resource, double)}.
//	 */
//	@Test
//	public void testSetUpperResourceOccupiedCapacity() 
//	{
//		
//		fail("Not yet implemented");
//	}
//
//	/**
//	 * Test method for {@link com.net2plan.interfaces.networkDesign.Resource#removeUpperResourceOccupation(com.net2plan.interfaces.networkDesign.Resource)}.
//	 */
//	@Test
//	public void testRemoveUpperResourceOccupation() {
//		fail("Not yet implemented");
//	}
//
//	/**
//	 * Test method for {@link com.net2plan.interfaces.networkDesign.Resource#addTraversingRoute(com.net2plan.interfaces.networkDesign.Route, double)}.
//	 */
//	@Test
//	public void testAddTraversingRoute() {
//		fail("Not yet implemented");
//	}
//
//	/**
//	 * Test method for {@link com.net2plan.interfaces.networkDesign.Resource#removeTraversingRoute(com.net2plan.interfaces.networkDesign.Route)}.
//	 */
//	@Test
//	public void testRemoveTraversingRoute() {
//		fail("Not yet implemented");
//	}
//
//	/**
//	 * Test method for {@link com.net2plan.interfaces.networkDesign.Resource#remove()}.
//	 */
//	@Test
//	public void testRemove() {
//		fail("Not yet implemented");
//	}

}
