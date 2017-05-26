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

package com.net2plan.utils;

import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;

import java.io.File;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

//public class TimeTrace <A>
@SuppressWarnings("unchecked")
public class TimeTrace
{
	private LinkedList<Pair<Double,Object>> list;
	
	public TimeTrace()
	{
		this.list = new LinkedList<Pair<Double,Object>> ();
	}

	public void add (double t , Object a)
	{
		this.list.addLast(Pair.of(t, a));
	}

	public int size () { return list.size(); }
	
	public String toString ()
	{
		final String NEWLINE = String.format("%n");
		StringBuilder pw = new StringBuilder ();
		final boolean entriesAreMap = list.getFirst().getSecond() instanceof Map<?,?>;
		Object [] keySet = (entriesAreMap)? ((Map<?,?>) list.getFirst().getSecond()).keySet().toArray() : null; // all keys in the same order always
		
		for (Pair<Double,Object> p : list)
		{
			final double t = p.getFirst();
			final Object a = p.getSecond();
			
			if (a == null) continue;
			
			pw.append("" + t);
			
			if ((a instanceof Double) || (a instanceof Integer))
			{
				pw.append(" " + a);
			}
			else if (a instanceof Map<?,?>)
			{
				Map<Long,Double> b = (Map<Long,Double>) a;
				for (Object k : keySet)
					pw.append (" " + b.get((Long) k));
			}
			else if (a instanceof double [])
			{
				for (double x : (double []) a)
					pw.append (" " + x);
			}
			else if (a instanceof DoubleMatrix1D)
			{
				for (double x : ((DoubleMatrix1D) a).toArray())
					pw.append (" " + x);
			}
			else if (a instanceof DoubleMatrix2D)
			{
				double [][] val = ((DoubleMatrix2D) a).toArray();
				for (double [] xArray : (double [][]) val) 
					for (double x : xArray) 
						pw.append (" " + x);
			}
			else if (a instanceof int [])
			{
				for (int x : (int []) a)
					pw.append (" " + x);
			}
			else if (a instanceof int [][])
			{
				for (int [] xArray : (int [][]) a) 
					for (int x : xArray) 
						pw.append (" " + x);
			}
			else if (a instanceof double [][])
			{
				for (double [] xArray : (double [][]) a) 
					for (double x : xArray) 
						pw.append (" " + x);
			}
			else throw new RuntimeException ("Unexpected type");
			
			pw.append (NEWLINE);
		}

		return pw.toString();
	}
	
	public void printToFile (File f)
	{
		try 
		{
			PrintWriter pw = new PrintWriter (f);
			pw.append(this.toString());
			pw.close ();
		} catch (Exception e) { e.printStackTrace(); throw new RuntimeException ("Not possible to write in File " + f); } 
	}

	public List<Pair<Double,Object>> getList () { return list; }
	
	public static void printToFile (File f , int [][] a)
	{
		try
		{
			PrintWriter pw = new PrintWriter (f);
			for (int i = 0 ; i < a.length ; i ++)
			{
				for (int j = 0 ; j < a[0].length ; j ++)
					pw.append(a [i][j] + " ");
				pw.println();
			}
			pw.close();
		} catch (Exception e) { e.printStackTrace(); throw new RuntimeException ("Bad"); }
	}
	public static void printToFile (File f , double [][] a)
	{
		try
		{
			PrintWriter pw = new PrintWriter (f);
			for (int i = 0 ; i < a.length ; i ++)
			{
				for (int j = 0 ; j < a[0].length ; j ++)
					pw.append(a [i][j] + " ");
				pw.println();
			}
			pw.close();
		} catch (Exception e) { e.printStackTrace(); throw new RuntimeException ("Bad"); }
	}

	public static void printToFile (File f , double [] a)
	{
		try
		{
			PrintWriter pw = new PrintWriter (f);
			for (int i = 0 ; i < a.length ; i ++)
				pw.print(a [i] + " ");
			pw.println ();
			pw.close();
		} catch (Exception e) { e.printStackTrace(); throw new RuntimeException ("Bad"); }
	}

	public static void printToFile (File f , DoubleMatrix1D a) { printToFile (f , a.toArray()); }

	public static void printToFile (File f , DoubleMatrix2D a) { printToFile (f , a.toArray()); }

	public static void printToFile (File f , double a)
	{
		try
		{
			PrintWriter pw = new PrintWriter (f);
			pw.println(a + " ");
			pw.close();
		} catch (Exception e) { e.printStackTrace(); throw new RuntimeException ("Bad"); }
	}
	public static void printToFile (File f , Map<Long,?> m)
	{
		try
		{
			PrintWriter pw = new PrintWriter (f);
			for (long k : m.keySet ())
				pw.append (" " + m.get(k));
			pw.close();
		} catch (Exception e) { e.printStackTrace(); throw new RuntimeException ("Bad"); }
	}
}
