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
package com.net2plan.libraries;

public class ProfileUtils 
{
	private static long lastTimeCall;
    public static long printTime (String m , long previousTime)
    {
    	lastTimeCall = System.nanoTime();
    	if (previousTime<0) 
    		System.out.println(m);
    	else
    		System.out.println(m + " - elapsed time " + (lastTimeCall - previousTime)*1e-6  + " ms");
    	return System.nanoTime();
    }
    public static long printTime (String m)
    {
    	final long newLastTimeCall = System.nanoTime();
   		System.out.println(m + " - elapsed time " + (newLastTimeCall - lastTimeCall)*1e-6  + " ms");
   		lastTimeCall = newLastTimeCall;
    	return newLastTimeCall;
    }
}
