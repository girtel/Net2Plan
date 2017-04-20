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
