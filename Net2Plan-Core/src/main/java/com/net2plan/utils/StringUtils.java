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

import cern.colt.matrix.tdouble.DoubleFactory2D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import cern.colt.matrix.tint.IntFactory2D;
import cern.colt.matrix.tint.IntMatrix1D;
import cern.colt.matrix.tint.IntMatrix2D;
import com.net2plan.interfaces.networkDesign.Net2PlanException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

/**
 * <p>Provides extra functionality for String objects.</p>
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * 
 */
public class StringUtils
{
	private final static String lineSeparator;

	static { lineSeparator = String.format("%n"); }
	
	private StringUtils() { }

	
	/**
	 * Generates an {@code String} from a matrix, where rows are separated by ";" and columns by spaces
	 *
	 * @param mat a matrix 
	 * @return the {@code String}
	 * 
	 */
	public static String writeMatrix (DoubleMatrix2D mat)
	{
		final int R = mat.rows();
		final int C = mat.columns();
		StringBuffer out = new StringBuffer ();
		for (int r = 0; r < R ; r ++)
		{
			if (r != 0) out.append (";");
			for (int c = 0 ; c < C ; c ++) out.append (" " + mat.get(r,c));
		}
		return out.toString();
	}

	/**
	 * Generates an {@code String} from a matrix, where rows are separated by ";" and columns by spaces
	 *
	 * @param mat a matrix 
	 * @return the {@code String}
	 * 
	 */
	public static String writeMatrix (IntMatrix2D mat)
	{
		final int R = mat.rows();
		final int C = mat.columns();
		StringBuffer out = new StringBuffer ();
		for (int r = 0; r < R ; r ++)
		{
			if (r != 0) out.append (";");
			for (int c = 0 ; c < C ; c ++) out.append (" " + mat.get(r,c));
		}
		return out.toString();
	}

	/**
	 * Generates an {@code String} from a vector of {@code DoubleMatrix1D} where elements are separated by spaces
	 *
	 * @param mat a vector
	 * @return the {@code String}
	 * 
	 */
	public static String writeMatrix (DoubleMatrix1D mat)
	{
		final int C = (int) mat.size();
		StringBuffer out = new StringBuffer ();
		for (int c = 0 ; c < C ; c ++) { if (c != 0) out.append(" "); out.append (mat.get(c)); }
		return out.toString();
	}
	
	/**
	 * Generates an {@code String} from a vector of {@code IntMatrix1D} where elements are separated by spaces
	 *
	 * @param mat a vector
	 * @return the {@code String}
	 * 
	 */
	public static String writeMatrix (IntMatrix1D mat)
	{
		final int C = (int) mat.size();
		StringBuffer out = new StringBuffer ();
		for (int c = 0 ; c < C ; c ++) { if (c != 0) out.append(" "); out.append (mat.get(c)); }
		return out.toString();
	}

	/**
	 * Reads a matrix {@code DoubleMatrix2D} from a {@code String}, where rows are separated by ";" and columns by spaces
	 *
	 * @param s {@code String} 
	 * @return the matrix 
	 */
	public static DoubleMatrix2D  readMatrix (String s)
	{
		String [] rows = split(s , ";");
		if (rows.length == 0) return DoubleFactory2D.dense.make(0,0);
		DoubleMatrix2D res = null;
		for (int row = 0 ; row < rows.length ; row ++)
		{
			double [] thisRow = StringUtils.toDoubleArray(StringUtils.split(rows [row] , " "));
			if (res == null) res = DoubleFactory2D.dense.make(rows.length , thisRow.length); else if (res.columns() != thisRow.length) throw new Net2PlanException ("Error reading the matrix. The number of columns cannot change from row to row");
			for (int c = 0 ; c < thisRow.length ; c ++) res.set(row,c,thisRow [c]);
		}
		return res;
	}

	/**
	 * Reads a matrix {@code IntMatrix2D} from a {@code String}, where rows are separated by ";" and columns by spaces
	 *
	 * @param s {@code String} 
	 * @return the matrix 
	 */
	public static IntMatrix2D  readIntMatrix (String s)
	{
		String [] rows = split(s , ";");
		if (rows.length == 0) return IntFactory2D.dense.make(0,0);
		IntMatrix2D res = null;
		for (int row = 0 ; row < rows.length ; row ++)
		{
			int [] thisRow = StringUtils.toIntArray(StringUtils.split(rows [row] , " "));
			if (res == null) res = IntFactory2D.dense.make(rows.length , thisRow.length); else if (res.columns() != thisRow.length) throw new Net2PlanException ("Error reading the matrix. The number of columns cannot change from row to row");
			for (int c = 0 ; c < thisRow.length ; c ++) res.set(row,c,thisRow [c]);
		}
		return res;
	}
	
	/**
	 * Generates an {@code String[]} from comma-separated values.
	 *
	 * @param values Comma-separated {@code String} values
	 * @return {@code String[]}
	 * 
	 */
	public static String[] arrayOf(String... values)
	{
		return values;
	}
	
	/**
	 * Returns the position(s) where a given value can be found into an array.
	 *
	 * @param array Input array
	 * @param value Value to be searched for
	 * @param searchType Indicates whether the first, the last, or all occurrences are returned
	 * @return Position(s) in which the given value can be found
	 * 
	 */
	public static int[] find(String[] array, String value, Constants.SearchType searchType)
	{
		switch(searchType)
		{
			case ALL:
			case FIRST:
				List<Integer> candidateIndexes = new LinkedList<Integer>();
				for(int i = 0; i < array.length; i++)
				{
					if(array[i].equals(value))
					{
						candidateIndexes.add(i);
						if (searchType == Constants.SearchType.FIRST) break;
					}
				}
				return IntUtils.toArray(candidateIndexes);
                       
			case LAST:
				for(int i = array.length - 1; i >= 0; i--) if(array[i].equals(value)) return new int[]{i};
				return new int[0];
                               
			default:
				throw new Net2PlanException("Invalid search type argument");
		}
	}
	
	/**
	 * Returns the line separator. Each operating system uses a different one (e.g. \r\n on Microsoft Windows, \n for Unix systems...).
	 *
	 * @return The line separator
	 * 
	 */
	public static String getLineSeparator()
	{
		return lineSeparator;
	}

	/**
	 * Joins elements from a given array into a {@code String}.
	 *
	 * @param array Input array
	 * @param separator Entry separator
	 * @return A String
	 * 
	 */
	public static String join(String[] array, String separator)
	{
		if (array.length == 0) return "";
		
		StringBuilder out = new StringBuilder();
		out.append(array[0]);

		for (int i = 1; i < array.length; i++) out.append(separator).append(array[i]);

		return out.toString();
	}

	/**
	 * Joins elements from a given collection into a {@code String}.
	 *
	 * @param collection Input collection
	 * @param separator Entry separator
	 * @return A String
	 * 
	 */
	public static String join(Collection<String> collection, String separator)
	{
		if (collection.isEmpty()) return "";
		
		StringBuilder out = new StringBuilder();
		Iterator<String> it = collection.iterator();
		out.append(it.next());

		while(it.hasNext()) out.append(separator).append(it.next());

		return out.toString();
	}
	
	/**
	 * Outputs entries from a {@code Map} to a {@code String}. Default 
	 * separator between each key and value is '=', and separator between key-value pair
	 * is ', '.
	 *
	 * @param map Input map
	 * @return {@code String} representing the map
	 * 
	 */
	public static String mapToString(Map map)
	{
		return mapToString(map, "=", ", ");
	}
	
	/**
	 * Outputs entries from a {@code Map} to a {@code String}.
	 *
	 * @param map Input map
	 * @param keyValueSeparator Separator between keys and values
	 * @param entrySeparator Separator between key-value pairs
	 * @return {@code String} representing the map
	 * 
	 */
	public static String mapToString(Map map, String keyValueSeparator, String entrySeparator)
	{
		StringBuilder string = new StringBuilder();

		Iterator it = map.entrySet().iterator();
		while (it.hasNext())
		{
			Entry entry = (Entry) it.next();
			if (string.length() > 0) string.append(entrySeparator);

			string.append(entry.getKey());
			string.append(keyValueSeparator);
			string.append(entry.getValue());
		}

		return string.toString();
	}

	/**
	 * Outputs entries from a {@code Map} to a {@code String}. Default
	 * separator between list entries is ', '.
	 * @param list Input list
	 * @return {@code String} representing the list.
	 */
	public static String listToString(List list)
	{
		return listToString(list, ", ");
	}

	/**
	 * Outputs entries from a {@code List} to a {@code String}.
	 * @param list Input list
	 * @param entrySeparator Separator between list entries.
	 * @return {@code String} representing the list.
	 */
	public static String listToString(List list, String entrySeparator)
	{
		StringBuilder string = new StringBuilder();

		for (Object entry : list)
		{
			if (string.length() > 0) string.append(entrySeparator);
			string.append(entry);
		}

		return string.toString();
	}
	
	/**
	 * Outputs an {@code String} to a file.
	 *
	 * @param text {@code String} to be saved
	 * @param file {@code File} where save the contents
	 * 
	 */
	public static void saveToFile(String text, File file)
	{
		saveToFile(text, file, false);
	}

	/**
	 * Outputs an {@code String} to a file.
	 *
	 * @param text {@code String} to be saved
	 * @param file {@code File} where save the contents
	 * @param append Indicates whether {@code file} must be overriden if already exists (if {@code true}), or {@code text} is append at the end (if {@code false})
	 * 
	 */
	public static void saveToFile(String text, File file, boolean append)
	{
		try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, append), StandardCharsets.UTF_8))) { out.write(text); out.write(getLineSeparator()); }
		catch (IOException ex) { throw new RuntimeException(ex); }
	}

	/**
	 * Converts a timestamp in seconds into its equivalent representation in days,
	 * hours, minutes and seconds.
	 *
	 * @param seconds Timestamp
	 * @return String representation in days, hours, minutes and seconds
	 * 
	 */
	public static String secondsToYearsDaysHoursMinutesSeconds(double seconds)
	{
		if (seconds == Double.MAX_VALUE) return "infinity";

		long aux_seconds = (long) seconds;
		long days = TimeUnit.SECONDS.toDays(aux_seconds);
		long months = (long) Math.floor((double) days / 30);
		long years = (long) Math.floor((double) months / 12);
		months %= 12;
		days %= 30;
		long hours = TimeUnit.SECONDS.toHours(aux_seconds) % 24;
		long minutes = TimeUnit.SECONDS.toMinutes(aux_seconds) % 60;
		seconds = seconds % 60;
		StringBuilder out = new StringBuilder();
		if (years > 0)
		{
			if (years == 1) out.append(String.format("%d year", years));
			else out.append(String.format("%d years", years));
		}

		if (months > 0)
		{
			if (out.length() > 0) out.append(", ");
			if (months == 1) out.append(String.format("%d month", months));
			else out.append(String.format("%d months", months));
		}
		
		if (days > 0)
		{
			if (out.length() > 0) out.append(", ");
			if (days == 1) out.append(String.format("%d day", days));
			else out.append(String.format("%d days", days));
		}
		
		if (hours > 0)
		{
			if (out.length() > 0) out.append(", ");
			out.append(String.format("%d h", hours));
		}

		if (minutes > 0)
		{
			if (out.length() > 0) out.append(", ");
			out.append(String.format("%d m", minutes));
		}

		if (seconds > 0)
		{
			if (out.length() > 0) out.append(", ");
			out.append(String.format("%.3f s", seconds));
		}

		if (out.length() == 0) out.append("0 s");
		return out.toString();
	}

	/**
	 * Returns a set of selected elements from an input array. It is not backed
	 * in the input array, thus changes in this array are not reflected in the
	 * input array.
	 *
	 * @param array Input array
	 * @param indexes Position of elements to be selected.
	 * @return Set of elements in the order given by {@code indexes}
	 * 
	 */
	public static String[] select(String[] array, int[] indexes)
	{
		String[] out = new String[indexes.length];
		for (int i = 0; i < indexes.length; i++) out[i] = array[indexes[i]];

		return out;
	}

	/**
	 * Splits a String into an array asumming items are separated by spaces.
	 *
	 * @param string Input string
	 * @return String array
	 * 
	 */
	public static String[] split(String string)
	{
		if (string == null)
		{
			return new String[0];
		}

		List<String> list = new LinkedList<String>();
		StringTokenizer tokens = new StringTokenizer(string, " ");
		while (tokens.hasMoreTokens())
		{
			list.add(tokens.nextToken());
		}

		return toArray(list);
	}

	/**
	 * Splits a String into an array according to a set of separators (i.e. ", " 
	 * means that the separator can be either a comma or a space).
	 *
	 * @param string Input string
	 * @param separators Set of separators
	 * @return {@code String} array (empty array if {@code string} = {@code null})
	 * 
	 */
	public static String[] split(String string, String separators)
	{
		if (string == null) return new String[0];

		List<String> list = new LinkedList<String>();
		StringTokenizer tokens = new StringTokenizer(string, separators);
		while (tokens.hasMoreTokens()) list.add(tokens.nextToken());

		return toArray(list);
	}

	/**
	 * Converts an {@code String} to a map. Default separator between each key 
	 * and value is '=', and separator between key-value pair is ', '.
	 *
	 * @param string Input {@code String}
	 * @return {@code String} representing the map
	 * 
	 */
	public static Map<String, String> stringToMap(String string)
	{
		return stringToMap(string, "=", ",");
	}
	
	/**
	 * Converts an {@code String} to a map.
	 *
	 * @param string Input {@code String}
	 * @param keyValueSeparator Separator between keys and values
	 * @param entrySeparator Separator between key-value pairs
	 * @return {@code Map}
	 * 
	 */
	public static Map<String, String> stringToMap(String string, String keyValueSeparator, String entrySeparator)
	{
		Map<String, String> map = new LinkedHashMap<String, String>();
		if (string == null || string.isEmpty()) return map;
		
		String[] parameters = StringUtils.split(string, entrySeparator);
		for (String parameter : parameters)
		{
			String[] paramValue = StringUtils.split(parameter, keyValueSeparator);
			if (paramValue.length < 2) throw new Net2PlanException("Bad - Parameter " + paramValue[0] + " has no associated value");
			if (paramValue.length > 2) throw new Net2PlanException("Bad - Parameter " + paramValue[0] + " has more than one associated value");
			map.put(paramValue[0].trim(), paramValue[1].trim());
		}
		
		return map;
	}

	/**
	 * Converts a collection ({@code List}, {@code Set}...) of objects to a {@code String} array. If objects are not instances of {@code String}, {@code toString()} will be used.
	 *
	 * @param list Input list
	 * @return {@code String} array (empty array if {@code list} = {@code null})
	 * 
	 */
	public static String[] toArray(Collection list)
	{
		if (list == null) return new String[0];

		String[] out = new String[list.size()];
		int i = 0;
		Iterator it = list.iterator();
		while (it.hasNext()) out[i++] = it.next().toString();

		return out;
	}

	/**
	 * Converts a collection of {@code String} objects to a {@code boolean} array.
	 *
	 * @param collection Input collection
	 * @return New {@code boolean} array
	 * 
	 */
	public static boolean[] toBooleanArray(Collection<String> collection)
	{
		int i = 0;
		int N = collection.size();
		boolean[] newArray = new boolean[N];
		for(String item : collection)
		{
			newArray[i] = Boolean.parseBoolean(item);
			i++;
		}
		
		return newArray;
	}

	/**
	 * Converts a collection of {@code String} objects to a {@code boolean} array.
	 *
	 * @param collection Input collection
	 * @param valueForNull Value for {@code null} positions
	 * @return New {@code boolean} array
	 * 
	 */
	public static boolean[] toBooleanArray(Collection<String> collection, boolean valueForNull)
	{
		int i = 0;
		int N = collection.size();
		boolean[] newArray = new boolean[N];
		for(String item : collection)
		{
			newArray[i] = item == null ? valueForNull : Boolean.parseBoolean(item);
			i++;
		}
		
		return newArray;
	}

	/**
	 * Converts a {@code String} array to a {@code boolean} array.
	 *
	 * @param array Input array
	 * @return New {@code boolean} array
	 * 
	 */
	public static boolean[] toBooleanArray(String[] array)
	{
		boolean[] newArray = new boolean[array.length];
		for (int i = 0; i < array.length; i++)
		{
			if (array[i] == null) throw new RuntimeException("Null value in position " + i);
			newArray[i] = Boolean.parseBoolean(array[i]);
		}
		return newArray;
	}
	
	/**
	 * Converts a {@code String} array to a {@code boolean} array.
	 *
	 * @param array Input array
	 * @param valueForNull Value for {@code null} positions
	 * @return New {@code boolean} array
	 * 
	 */
	public static boolean[] toBooleanArray(String[] array, boolean valueForNull)
	{
		boolean[] newArray = new boolean[array.length];
		for (int i = 0; i < array.length; i++) newArray[i] = array[i] == null ? valueForNull : Boolean.parseBoolean(array[i]);

		return newArray;
	}

	/**
	 * Converts a {@code String} array to a {@code boolean} array.
	 *
	 * @param array Input array
	 * @return New {@code boolean} array
	 * 
	 */
	public static List<Boolean> toBooleanList(String[] array)
	{
		List<Boolean> out = new ArrayList<Boolean>(array.length);
		for (int i = 0; i < array.length; i++)
		{
			if (array[i] == null) throw new RuntimeException("Null value in position " + i);
			out.add(Boolean.parseBoolean(array[i]));
		}
		return out;
	}
	
	/**
	 * Converts a {@code String} array to a {@code Boolean} list.
	 *
	 * @param array Input array
	 * @param valueForNull Value for {@code null} positions
	 * @return New {@code boolean} array
	 * 
	 */
	public static List<Boolean> toBooleanList(String[] array, boolean valueForNull)
	{
		List<Boolean> out = new ArrayList<Boolean>(array.length);
		for (String array1 : array)
			out.add(array1 == null ? valueForNull : Boolean.parseBoolean(array1));

		return out;
	}

	/**
	 * Converts a map whose values are {@code String} objects to another 
	 * where values are {@code Boolean} objects.
	 *
	 * @param <A> Key type
	 * @param map Input map
	 * @return New map where values are {@code Boolean} objects
	 * 
	 */
	public static <A> Map<A, Boolean> toBooleanMap(Map<A, String> map)
	{
		Map<A, Boolean> out = new LinkedHashMap<A, Boolean>();
		for(Entry<A, String> entry : map.entrySet())
		{
			if (entry.getValue() == null) throw new RuntimeException("Null value in entry for key " + entry.getKey());
			out.put(entry.getKey(), Boolean.parseBoolean(entry.getValue()));
		}
		
		return out;
	}

	/**
	 * Converts a map whose values are {@code String} objects to another 
	 * where values are {@code Boolean} objects.
	 *
	 * @param <A> Key type
	 * @param map Input map
	 * @param valueForNull Value for {@code null} entries
	 * @return New map where values are {@code Boolean} objects
	 * 
	 */
	public static <A> Map<A, Boolean> toBooleanMap(Map<A, String> map, boolean valueForNull)
	{
		Map<A, Boolean> out = new LinkedHashMap<A, Boolean>();
		for(Entry<A, String> entry : map.entrySet())
		{
			if (entry.getValue() == null) throw new RuntimeException("Null value in entry for key " + entry.getKey());
			out.put(entry.getKey(), entry.getValue() == null ? valueForNull : Boolean.parseBoolean(entry.getValue()));
		}
		
		return out;
	}

	/**
	 * Converts a collection of {@code String} objects to a {@code double} array.
	 *
	 * @param collection Input collection
	 * @return New {@code double} array
	 * 
	 */
	public static double[] toDoubleArray(Collection<String> collection)
	{
		int i = 0;
		int N = collection.size();
		double[] newArray = new double[N];
		for(String item : collection)
		{
			newArray[i] = Double.parseDouble(item);
			i++;
		}
		
		return newArray;
	}

	/**
	 * Converts a collection of {@code String} objects to a {@code double} array.
	 *
	 * @param collection Input collection
	 * @param valueForNull Value for {@code null} positions
	 * @return New {@code double} array
	 * 
	 */
	public static double[] toDoubleArray(Collection<String> collection, double valueForNull)
	{
		int i = 0;
		int N = collection.size();
		double[] newArray = new double[N];
		for(String item : collection)
		{
			newArray[i] = item == null ? valueForNull : Double.parseDouble(item);
			i++;
		}
		
		return newArray;
	}

	/**
	 * Converts a {@code String} array to a {@code double} array.
	 *
	 * @param array Input array
	 * @return New {@code double} array
	 * 
	 */
	public static double[] toDoubleArray(String[] array)
	{
		double[] newArray = new double[array.length];
		for (int i = 0; i < array.length; i++)
		{
			if (array[i] == null) throw new RuntimeException("Null value in position " + i);
			newArray[i] = Double.parseDouble(array[i]);
		}
		return newArray;
	}

	/**
	 * Converts a {@code String} array to a {@code double} array.
	 *
	 * @param array Input array
	 * @param valueForNull Value for {@code null} positions
	 * @return New {@code double} array
	 * 
	 */
	public static double[] toDoubleArray(String[] array, double valueForNull)
	{
		double[] newArray = new double[array.length];
		for (int i = 0; i < array.length; i++)
		{
			newArray[i] = array[i] == null ? valueForNull : Double.parseDouble(array[i]);
		}
		return newArray;
	}

	/**
	 * Converts a {@code String} array to a {@code double} array.
	 *
	 * @param array Input array
	 * @return New {@code double} array
	 * 
	 */
	public static List<Double> toDoubleList(String[] array)
	{
		List<Double> out = new ArrayList<Double>(array.length);
		for (int i = 0; i < array.length; i++)
		{
			if (array[i] == null) throw new RuntimeException("Null value in position " + i);
			out.add(Double.parseDouble(array[i]));
		}
		return out;
	}

	/**
	 * Converts a {@code String} array to a {@code double} array.
	 *
	 * @param array Input array
	 * @param valueForNull Value for {@code null} positions
	 * @return New {@code double} array
	 * 
	 */
	public static List<Double> toDoubleList(String[] array, double valueForNull)
	{
		List<Double> out = new ArrayList<Double>(array.length);
		for (String array1 : array)
		{
			out.add(array1 == null ? valueForNull : Double.parseDouble(array1));
		}

		return out;
	}
	
	/**
	 * Converts a map whose values are {@code String} objects to another 
	 * where values are {@code Double} objects.
	 *
	 * @param <A> Key type
	 * @param map Input map
	 * @return New map where values are {@code Double} objects
	 * 
	 */
	public static <A> Map<A, Double> toDoubleMap(Map<A, String> map)
	{
		Map<A, Double> out = new LinkedHashMap<A, Double>();
		for(Entry<A, String> entry : map.entrySet())
		{
			if (entry.getValue() == null) throw new RuntimeException("Null value in entry for key " + entry.getKey());
			out.put(entry.getKey(), Double.parseDouble(entry.getValue()));
		}
		
		return out;
	}

	/**
	 * Converts a map whose values are {@code String} objects to another 
	 * where values are {@code Double} objects.
	 *
	 * @param <A> Key type
	 * @param map Input map
	 * @param valueForNull Value for {@code null} positions
	 * @return New map where values are {@code Double} objects
	 * 
	 */
	public static <A> Map<A, Double> toDoubleMap(Map<A, String> map, double valueForNull)
	{
		Map<A, Double> out = new LinkedHashMap<A, Double>();
		for(Entry<A, String> entry : map.entrySet()) out.put(entry.getKey(), entry.getValue() == null ? valueForNull : Double.parseDouble(entry.getValue()));
		
		return out;
	}

	/**
	 * Converts a collection of {@code String} objects to an {@code int} array.
	 *
	 * @param collection Input collection
	 * @return New {@code int} array
	 * 
	 */
	public static int[] toIntArray(Collection<String> collection)
	{
		int i = 0;
		int N = collection.size();
		int[] newArray = new int[N];
		for(String item : collection)
		{
			newArray[i] = Integer.parseInt(item);
			i++;
		}
		
		return newArray;
	}

	/**
	 * Converts a collection of {@code String} objects to an {@code int} array.
	 *
	 * @param collection Input collection
	 * @param valueForNull Value for {@code null} positions
	 * @return New {@code int} array
	 * 
	 */
	public static int[] toIntArray(Collection<String> collection, int valueForNull)
	{
		int i = 0;
		int N = collection.size();
		int[] newArray = new int[N];
		for(String item : collection)
		{
			newArray[i] = item == null ? valueForNull : Integer.parseInt(item);
			i++;
		}
		
		return newArray;
	}

	/**
	 * Converts a {@code String} array to an {@code int} array.
	 *
	 * @param array Input array
	 * @return New {@code int} array
	 * 
	 */
	public static int[] toIntArray(String[] array)
	{
		int[] newArray = new int[array.length];
		for (int i = 0; i < array.length; i++)
		{
			if (array[i] == null) throw new RuntimeException("Null value in position " + i);
			newArray[i] = Integer.parseInt(array[i]);
		}
		
		return newArray;
	}

	/**
	 * Converts a {@code String} array to an {@code int} array.
	 *
	 * @param array Input array
	 * @param valueForNull Value for {@code null} positions
	 * @return New {@code int} array
	 * 
	 */
	public static int[] toIntArray(String[] array, int valueForNull)
	{
		int[] newArray = new int[array.length];
		for (int i = 0; i < array.length; i++)
			newArray[i] = array[i] == null ? valueForNull : Integer.parseInt(array[i]);

		return newArray;
	}

	/**
	 * Converts a map whose values are {@code String} objects to another 
	 * where values are {@code Integer} objects.
	 *
	 * @param <A> Key type
	 * @param map Input map
	 * @return New map where values are {@code Integer} objects
	 * 
	 */
	public static <A> Map<A, Integer> toIntegerMap(Map<A, String> map)
	{
		Map<A, Integer> out = new LinkedHashMap<A, Integer>();
		for(Entry<A, String> entry : map.entrySet())
		{
			if (entry.getValue() == null) throw new RuntimeException("Null value in entry for key " + entry.getKey());
			out.put(entry.getKey(), Integer.parseInt(entry.getValue()));
		}
		
		return out;
	}

	/**
	 * Converts a map whose values are {@code String} objects to another 
	 * where values are {@code Integer} objects.
	 *
	 * @param <A> Key type
	 * @param map Input map
	 * @param valueForNull Value for {@code null} positions
	 * @return New map where values are {@code Integer} objects
	 * 
	 */
	public static <A> Map<A, Integer> toIntegerMap(Map<A, String> map, int valueForNull)
	{
		Map<A, Integer> out = new LinkedHashMap<A, Integer>();
		for(Entry<A, String> entry : map.entrySet()) out.put(entry.getKey(), entry.getValue() == null ? valueForNull : Integer.parseInt(entry.getValue()));
		
		return out;
	}

	/**
	 * Converts from a {@code String} array to a list.
	 *
	 * @param array Input array
	 * @return A list of {@code String} objects
	 * 
	 */
	public static List<String> toList(String[] array)
	{
		return new LinkedList<String>(Arrays.asList(array));
	}

	/**
	 * Converts a collection of {@code String} objects to a {@code long} array.
	 *
	 * @param collection Input collection
	 * @return New {@code long} array
	 * 
	 */
	public static long[] toLongArray(Collection<String> collection)
	{
		int i = 0;
		int N = collection.size();
		long[] newArray = new long[N];
		for(String item : collection)
		{
			newArray[i] = Long.parseLong(item);
			i++;
		}
		
		return newArray;
	}

	/**
	 * Converts a collection of {@code String} objects to a {@code long} array.
	 *
	 * @param collection Input collection
	 * @param valueForNull Value for {@code null} positions
	 * @return New {@code long} array
	 * 
	 */
	public static long[] toLongArray(Collection<String> collection, long valueForNull)
	{
		int i = 0;
		int N = collection.size();
		long[] newArray = new long[N];
		for(String item : collection)
		{
			newArray[i] = item == null ? valueForNull : Long.parseLong(item);
			i++;
		}
		
		return newArray;
	}

	/**
	 * Converts a {@code String} array to a {@code long} array.
	 *
	 * @param array Input array
	 * @return New {@code long} array
	 * 
	 */
	public static long[] toLongArray(String[] array)
	{
		long[] newArray = new long[array.length];
		for (int i = 0; i < array.length; i++)
		{
			if (array[i] == null)
			{
				throw new RuntimeException("Null value in position " + i);
			}
			newArray[i] = Long.parseLong(array[i]);
		}
		return newArray;
	}

	/**
	 * Converts a {@code String} array to a {@code long} array.
	 *
	 * @param array Input array
	 * @param valueForNull Value for {@code null} positions
	 * @return New {@code long} array
	 * 
	 */
	public static long[] toLongArray(String[] array, long valueForNull)
	{
		long[] newArray = new long[array.length];
		for (int i = 0; i < array.length; i++)
		{
			newArray[i] = array[i] == null ? valueForNull : Long.parseLong(array[i]);
		}
		return newArray;
	}
	
	/**
	 * Converts a {@code String} array to a {@code long} array.
	 *
	 * @param array Input array
	 * @return New {@code long} array
	 * 
	 */
	public static List<Long> toLongList(String[] array)
	{
		List<Long> out = new ArrayList<Long>(array.length);
		for (int i = 0; i < array.length; i++)
		{
			if (array[i] == null) throw new RuntimeException("Null value in position " + i);
			out.add(Long.parseLong(array[i]));
		}
		return out;
	}

	/**
	 * Converts a {@code String} array to a {@code long} array.
	 *
	 * @param array Input array
	 * @param valueForNull Value for {@code null} positions
	 * @return New {@code long} array
	 * 
	 */
	public static List<Long> toLongList(String[] array, long valueForNull)
	{
		List<Long> out = new ArrayList<Long>(array.length);
		for (String array1 : array)  out.add(array1 == null ? valueForNull : Long.parseLong(array1));

		return out;
	}

	/**
	 * Converts a map whose values are {@code String} objects to another 
	 * where values are {@code Long} objects.
	 *
	 * @param <A> Key type
	 * @param map Input map
	 * @return New map where values are {@code Long} objects
	 * 
	 */
	public static <A> Map<A, Long> toLongMap(Map<A, String> map)
	{
		Map<A, Long> out = new LinkedHashMap<A, Long>();
		for(Entry<A, String> entry : map.entrySet())
		{
			if (entry.getValue() == null) throw new RuntimeException("Null value in entry for key " + entry.getKey());
			out.put(entry.getKey(), Long.parseLong(entry.getValue()));
		}
		
		return out;
	}

	/**
	 * Converts a map whose values are {@code String} objects to another 
	 * where values are {@code Long} objects.
	 *
	 * @param <A> Key type
	 * @param map Input map
	 * @param valueForNull Value for {@code null} positions
	 * @return New map where values are {@code Long} objects
	 * 
	 */
	public static <A> Map<A, Long> toLongMap(Map<A, String> map, long valueForNull)
	{
		Map<A, Long> out = new LinkedHashMap<A, Long>();
		for(Entry<A, String> entry : map.entrySet()) out.put(entry.getKey(), entry.getValue() == null ? valueForNull : Long.parseLong(entry.getValue()));
		
		return out;
	}
	
	/**
	 * Converts to a {@code Map} a codified {@code String}.
	 *
	 * @param string Input {@code String}
	 * @param keyValueSeparator Separator between keys and values
	 * @param entrySeparator Separator between key-value pairs
	 * @return {@code Map} represented by the {@code String}
	 * 
	 */
	public static Map<String, String> toMap(String string, String keyValueSeparator, String entrySeparator)
	{
		Map<String, String> out = new LinkedHashMap<String, String>();
		String[] pairs = string.split(entrySeparator);
		for (String pair : pairs)
		{
			String[] keyValue = pair.split(keyValueSeparator);
			out.put(keyValue[0], keyValue[1]);
		}		
		
		return out;
	}

}
