package com.net2plan.interfaces.networkDesign;

import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class Offline_nfvPlacementTest 
{
	private NetPlan np;
	
	@Before
	public void setUp() throws Exception 
	{
		/* create a topology or load one, and use this for tests */
		this.np = new NetPlan ();
	}

	@After
	public void tearDown() throws Exception 
	{
	}

	@Test
	public void testExecuteAlgorithm() 
	{
	}

}
