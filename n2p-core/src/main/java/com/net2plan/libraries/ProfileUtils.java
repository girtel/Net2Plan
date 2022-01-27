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

import com.net2plan.internal.ErrorHandling;

public class ProfileUtils
{
	private static long lastTimeCall;
    public static void printTime (String m , long previousTime)
    {
    	if (!ErrorHandling.isProfileEnabled()) return;

    	lastTimeCall = System.nanoTime();
    	if (previousTime<0) 
    		System.out.println("PROFILE: " +m);
    	else
    		System.out.println("PROFILE: " +m + " - elapsed time " + (lastTimeCall - previousTime)*1e-6  + " ms");
    }
    public static void printTime (String m)
    {
		if (!ErrorHandling.isProfileEnabled()) return;

    	final long newLastTimeCall = System.nanoTime();
    	System.out.println("PROFILE: " + m + " - elapsed time " + (newLastTimeCall - lastTimeCall)*1e-6  + " ms");
   		lastTimeCall = newLastTimeCall;
    }
    public static void resetTimer ()
    {
		if (!ErrorHandling.isProfileEnabled()) return;
   		lastTimeCall = System.nanoTime();
    }
}
