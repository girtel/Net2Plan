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
