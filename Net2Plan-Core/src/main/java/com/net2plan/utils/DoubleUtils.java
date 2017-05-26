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

import cern.colt.list.tdouble.DoubleArrayList;
import cern.colt.list.tint.IntArrayList;
import cern.colt.matrix.tdouble.DoubleFactory2D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import com.net2plan.interfaces.networkDesign.Net2PlanException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Map.Entry;

/**
 * <p>Provides extra functionality for {@code double} primitives.</p>
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public class DoubleUtils
{
	private DoubleUtils() { }

	/**
	 * Generates a {@code double[]} from comma-separated values.
	 *
	 * @param values Comma-separated {@code double} values
	 * @return {@code double[]}
	 */
	public static double[] arrayOf(double... values)
	{
		return values;
	}
	
	/**
	 * Converts from a {@code double} array to a {@code Double} array.
	 *
	 * @param array {@code double} array
	 * @return Equivalent {@code Double} array
	 */
	public static Double[] asObjectArray(double[] array)
	{
		Double[] objectArray = new Double[array.length];

		for (int i = 0; i < array.length; i++)
			objectArray[i] = array[i];

		return objectArray;
	}

	/**
	 * Converts from a {@code Double} array to a {@code double} array.
	 *
	 * @param array {@code Double} array
	 * @return Equivalent {@code double} array
	 */
	public static double[] asPrimitiveArray(Double[] array)
	{
		double[] primitiveArray = new double[array.length];

		for (int i = 0; i < array.length; i++)
			primitiveArray[i] = array[i];

		return primitiveArray;
	}

	/**
	 * Returns the average value of an array.
	 *
	 * @param array Input array
	 * @return Average value (or zero, if {@code array} is empty)
	 */
	public static double average(double[] array)
	{
		return array.length == 0 ? 0 : sum(array) / array.length;
	}

	/**
	 * Returns the average of the map values.
	 *
	 * @param <A> Key type
	 * @param map Input map
	 * @return Average value (or zero, if {@code map} is empty)
	 */
	public static <A> double average(Map<A, Double> map)
	{
		return average(map.values());
	}

	/**
	 * Returns the average value of a collection.
	 *
	 * @param collection Input collection
	 * @return Average value (or zero, if {@code collection} is empty)
	 */
	public static double average(Collection<Double> collection)
	{
		return collection.isEmpty() ? 0 : sum(collection) / collection.size();
	}
	
	/**
	 * Returns the average value only among non-zero values
	 *
	 * @param array Input array
	 * @return Average value among non-zero values
	 */
	public static double averageNonZeros(double[] array)
	{
		if (array.length == 0) return 0;

		int numNonZeros = 0;
		double sum = 0;
		for (int i = 0; i < array.length; i++)
		{
			if (array[i] != 0)
			{
				sum += array[i];
				numNonZeros++;
			}
		}

		return numNonZeros == 0 ? 0 : sum / numNonZeros;
	}


	
	
	/**
	 * Rounds up each element of a given {@code double} array.
	 *
	 * @param array Array to be rounded up
	 * @return Rounded up array
	 */
	public static double[] ceil(double[] array)
	{
		double[] out = new double[array.length];
		for (int i = 0; i < array.length; i++)
			out[i] = Math.ceil(array[i]);

		return out;
	}

	/**
	 * Concatenates a series of arrays.
	 *
	 * @param arrays List of arrays
	 * @return Concatenated array
	 */
	public static double[] concatenate(double[]... arrays)
	{
		List<Double> list = new LinkedList<Double>();

		for (double[] array : arrays)
			list.addAll(toList(array));

		double[] concatArray = toArray(list);

		return concatArray;
	}

	/**
	 * Returns an array filled with a given value.
	 *
	 * @param N Number of elements
	 * @param value Value for all elements
	 * @return An array of length {@code N} with the given value
	 */
	public static double[] constantArray(int N, double value)
	{
		double[] array = new double[N];
		Arrays.fill(array, value);

		return array;
	}

	/**
	 * Returns a map filled with a given value.
	 *
	 * @param <A> Key type
	 * @param identifiers Set of map keys
	 * @param value Value for all elements
	 * @return A map with the given value in each entry
	 */
	public static <A> Map<A, Double> constantMap(Set<A> identifiers, double value)
	{
		Map<A, Double> out = new LinkedHashMap<A, Double>();
		for(A identifier : identifiers)
			out.put(identifier, value);
		
		return out;
	}

	/**
	 * Checks if an input array contains a given value
	 *
	 * @param array Input array
	 * @param value Value to search
	 * @return {@code true} if {@code value} is present in {@code array}, and {@code false} otherwise. If {@code array} is empty, it will return {@code false}
	 */
	public static boolean contains(double[] array, double value)
	{
		for (int i = 0; i < array.length; i++)
			if (Math.abs(array[i] - value) < 1E-10)
				return true;

		return false;
	}

	/**
	 * Checks if an array contains all numbers from another one.
	 *
	 * @param container Container array
	 * @param array Array with elements to be checked
	 * @return {@code true} if all elements in {@code array} are present in {@code container}, and {@code false} otherwise. If {@code container} is empty, it will return {@code false}
	 */
	public static boolean containsAll(double[] container, double[] array)
	{
		if (container.length == 0) return false;

		Set<Double> set = new LinkedHashSet<Double>(toList(container));
		set.removeAll(toList(array));

		return set.isEmpty();
	}

	/**
	 * Checks whether any element of an array is present in another one. It is equivalent to assert if {@code intersection(array1, array2).length == 0}.
	 *
	 * @param container Container array
	 * @param array Array with elements to be checked
	 * @return {@code true} if any element in {@code array} is present in {@code container}, and {@code false} otherwise. If {@code container} is empty, it will return {@code false}
	 */
	public static boolean containsAny(double[] container, double[] array)
	{
		if (container.length == 0) return false;

		for (int i = 0; i < array.length; i++)
			for (int j = 0; j < container.length; j++)
				if (Math.abs(array[i] - container[i]) < 1E-10)
					return true;

		return false;
	}

	/**
	 * Returns a map containing the association of each identifier with its corresponding
	 * value within the array (in linear order). First element in the array will be
	 * associated to the first identifier in the set (in iteration order), 
	 * second element in the array to the second element in the set, and so on.
	 *
	 * @param <A> Key type
	 * @param identifiers Set of identifier in ascending iterator order (duplicated and null values are not allowed)
	 * @param array Input array
	 * @return Map
	 */
	public static <A extends Number> Map<A, Double> convertArray2Map(Set<A> identifiers, double[] array)
	{
		Map<A, Integer> id2LinearIndex = CollectionUtils.convertId2LinearIndexMap(identifiers);
		Map<A, Double> out = new LinkedHashMap<A, Double>();
		for(Entry<A, Integer> entry : id2LinearIndex.entrySet())
		{
			A id = entry.getKey();
			int index = entry.getValue();
			out.put(id, array[index]);
		}
		
		return out;
	}
	
	/**
	 * Returns a map containing the association of each identifier pair with its corresponding
	 * value within the table (in linear order). First element in the table will be
	 * associated to the first identifier pair (in iteration order), 
	 * second element in the table to the second identifier pair, and so on.
	 *
	 * @param <A> Row key type
	 * @param <B> Column key type
	 * @param rowIdentifiers Set of row identifiers in ascending iterator order (null values are not allowed)
	 * @param columnIdentifiers Set of column identifiers in ascending iterator order (duplicated and null values are not allowed)
	 * @param matrix Input matrix
	 * @return Map
	 */
	public static <A extends Number, B extends Number> Map<Pair<A, B>, Double> convertMatrix2Map(Set<A> rowIdentifiers, Set<B> columnIdentifiers, DoubleMatrix2D matrix)
	{
		List<A> rowIdentifiersList = new LinkedList<A>(rowIdentifiers);
		List<B> columnIdentifiersList = new LinkedList<B>(columnIdentifiers);
		
		IntArrayList rowIndices = new IntArrayList();
		IntArrayList colIndices = new IntArrayList();
		DoubleArrayList values = new DoubleArrayList();
		matrix.getNonZeros(rowIndices, colIndices, values);
		
		int elements = values.size();
		Map<Pair<A, B>, Double> out = new LinkedHashMap<Pair<A, B>, Double>();
		for(int i = 0; i < elements; i++)
		{
			int row = rowIndices.get(i);
			int column = colIndices.get(i);
			double value = values.get(i);
			
			A rowId = rowIdentifiersList.get(row);
			B columnId = columnIdentifiersList.get(column);
			out.put(Pair.of(rowId, columnId), value);
		}

		return out;
	}

	/**
	 * Returns a map containing the association of each identifier pair with its corresponding
	 * value within the table (in linear order). First element in the table will be
	 * associated to the first identifier pair (in iteration order), 
	 * second element in the table to the second identifier pair, and so on.
	 *
	 * @param <A> Row key type
	 * @param <B> Column key type
	 * @param rowIdentifiers Set of row identifiers in ascending iterator order (null values are not allowed)
	 * @param columnIdentifiers Set of column identifiers in ascending iterator order (duplicated and null values are not allowed)
	 * @param table Input table
	 * @return Map
	 */
	public static <A extends Number, B extends Number> Map<Pair<A, B>, Double> convertTable2Map(Set<A> rowIdentifiers, Set<B> columnIdentifiers, double[][] table)
	{
		Map<A, Integer> rowId2LinearIndex = CollectionUtils.convertId2LinearIndexMap(rowIdentifiers);
		Map<B, Integer> columnId2LinearIndex = CollectionUtils.convertId2LinearIndexMap(columnIdentifiers);
		Map<Pair<A, B>, Double> out = new LinkedHashMap<Pair<A, B>, Double>();
		for(Entry<A, Integer> rowEntry : rowId2LinearIndex.entrySet())
		{
			A rowId = rowEntry.getKey();
			int row = rowEntry.getValue();
			
			for(Entry<B, Integer> columnEntry : columnId2LinearIndex.entrySet())
			{
				B columnId = columnEntry.getKey();
				int column = columnEntry.getValue();

				out.put(Pair.of(rowId, columnId), table[row][column]);
			}
		}

		return out;
	}

	/**
	 * Returns a deep copy of the input {@code array}.
	 *
	 * @param array Input array
	 * @return Deep copy of {@code array}
	 */
	public static double[] copy(double[] array)
	{
		return Arrays.copyOf(array, array.length);
	}

	/**
	 * Returns a deep copy of the input {@code array}.
	 *
	 * @param array Input array
	 * @return Deep copy of {@code array}
	 */
	public static double[][] copy(double[][] array)
	{
		double[][] out = new double[array.length][];
		for (int rowId = 0; rowId < array.length; rowId++)
		{
			out[rowId] = copy(array[rowId]);
		}

		return out;
	}

	/**
	 * Divides two arrays element-to-element
	 *
	 * @param array1 Numerator
	 * @param array2 Denominator
	 * @return The element-wise division of the input arrays
	 */
	public static double[] divide(double[] array1, double[] array2)
	{
		double[] out = new double[array1.length];

		for (int i = 0; i < out.length; i++)
		{
			out[i] = array1[i] / array2[i];
		}

		return out;
	}

	/**
	 * Divides all elements in an array by a scalar.
	 *
	 * @param array Input array
	 * @param value Scalar
	 * @return A new array containing the input elements divided by the scalar
	 */
	public static double[] divide(double[] array, double value)
	{
		double[] out = new double[array.length];

		for (int i = 0; i < out.length; i++)
		{
			out[i] = array[i] / value;
		}

		return out;
	}

	/**
	 * Divides all elements in a map by a scalar.
	 *
	 * @param <A> Key type
	 * @param map Input map
	 * @param value Scalar
	 * @return A new map containing the input elements divided by the scalar
	 */
	public static <A> Map<A, Double> divide(Map<A, Double> map, double value)
	{
		Map<A, Double> out = new LinkedHashMap<A, Double>();

		for (Entry<A, Double> entry : map.entrySet())
		{
			out.put(entry.getKey(), entry.getValue() / value);
		}

		return out;
	}

	/**
	 * Returns the element-wise quotient of two maps.
	 *
	 * @param <A> Key type
	 * @param map1 Input map 1
	 * @param map2 Input map 2
	 * @return A new map with the element-wise quotient
	 */
	public static <A> Map<A, Double> divide(Map<A, Double> map1, Map<A, Double> map2)
	{
		Map<A, Double> out = new LinkedHashMap<A, Double>();

		for (Entry<A, Double> entry : map1.entrySet())
		{
			A key = entry.getKey();
			out.put(key, entry.getValue() / map2.get(key));
		}

		return out;
	}
	
	/**
	 * Divides two arrays element-to-element, but when numerator and denominator = 0, returns 0 instead of a singularity (NaN)
	 *
	 * @param array1 Numerator
	 * @param array2 Denominator
	 * @return The element-wise division of the input arrays
	 */
	public static double[] divideNonSingular(double[] array1, double[] array2)
	{
		double[] out = new double[array1.length];

		for (int i = 0; i < out.length; i++)
		{
			out[i] = array1[i] == 0 && array2[i] == 0 ? 0 : array1[i] / array2[i];
		}

		return out;
	}

	/**
	 * Returns the position(s) where a given value can be found into an array.
	 *
	 * @param array Input array
	 * @param value Value to be searched for
	 * @param searchType Indicates whether the first, the last, or all occurrences are returned
	 * @return Position(s) in which the given value can be found
	 */
	public static int[] find(double[] array, double value, Constants.SearchType searchType)
	{
		switch(searchType)
		{
			case ALL:
			case FIRST:
				List<Integer> candidateIndexes = new LinkedList<Integer>();
				for(int i = 0; i < array.length; i++)
				{
					if (Math.abs(array[i] - value) < 1E-10)
					{
						candidateIndexes.add(i);
						if (searchType == Constants.SearchType.FIRST) break;
					}
				}
				return IntUtils.toArray(candidateIndexes);
                       
			case LAST:
				for(int i = array.length - 1; i >= 0; i--) if(array[i] == value) return new int[]{i};
				return new int[0];
                               
			default:
				throw new Net2PlanException("Invalid search type argument");
		}
	}

	/**
	 * Rounds down each element of a given {@code double} array.
	 *
	 * @param array Array to be rounded down
	 * @return Rounded down array
	 */
	public static double[] floor(double[] array)
	{
		double[] out = new double[array.length];
		for (int i = 0; i < array.length; i++)
			out[i] = Math.floor(array[i]);

		return out;
	}

	/**
	 * Converts a {@code double} to {@code String} (using '%f' modifier) and remove trailing zeros.
	 * 
	 * @param value Input value
	 * @param numDecimals Maximum number of decimal digits
	 * @return Formatted value wihout trailiing zeros
	 */
	public static String formatAndRemoveTrailingZeros(double value, int numDecimals)
	{
		String s = String.format("%." + numDecimals + "f", value);
		if (numDecimals > 0) s = !s.contains(".") ? s : s.replaceAll("0*$", "").replaceAll("\\.$", "");
		
		return s;
	}
	
	/**
	 * Computes the greatest common divisor of a double array.
	 *
	 * @param array Input array
	 * @return The greatest common divisor. By convention, {@code gcd(0, 0)} is equal to zero and {@code gcd([])} is equal to one
	 */
	public static double gcd(double[] array)
	{
		if (array.length == 0) return 1;

		int[] intArray = new int[array.length];
		for (int i = 0; i < intArray.length; i++)
			intArray[i] = (int) (1000 * array[i]);

		return (double) IntUtils.gcd(intArray) / 1000;
	}

	/**
	 * Computes the greatest common divisor of an input collection.
	 *
	 * @param collection Input collection
	 * @return The greatest common divisor. By convention, {@code gcd(0, 0)} is equal to zero and {@code gcd([])} is equal to one
	 */
	public static double gcd(Collection<Double> collection)
	{
		if (collection.isEmpty()) throw new NoSuchElementException("Empty collection");
		
		return gcd(toArray(collection));
	}
	
	/**
	 * Returns the intersection vector of a series of input arrays. There is no order guarantee.
	 *
	 * @param arrays Vector of input arrays
	 * @return Intersection vector of input arrays
	 */
	public static double[] intersect(double[]... arrays)
	{
		if (arrays.length == 0) return new double[0];

		Set<Double> intersectionSet = new LinkedHashSet<Double>();
		intersectionSet.addAll(toList(arrays[0]));

		for (int i = 1; i < arrays.length; i++)
			intersectionSet.retainAll(toList(arrays[i]));

		return toArray(intersectionSet);
	}

	/**
	 * Checks if a given value is within a tolerance margin compared to a given pattern.
	 *
	 * @param pattern Pattern value
	 * @param value Value to compare with pattern
	 * @param tolerance Tolerance margin
	 * @return {@code true} if {@code value} is within the tolerance, and false otherwise
	 */
	public static boolean isEqualWithinAbsoluteTolerance(double pattern, double value, double tolerance)
	{
		return (Math.abs(pattern - value) <= tolerance);
	}

	/**
	 * Checks if the relative difference between a given value and a pattern is within a tolerance margin.
	 *
	 * @param pattern Pattern value
	 * @param value Value to compare with pattern
	 * @param tolerance Tolerance margin
	 * @return {@code true} if {@code value} is within the tolerance, and false otherwise
	 */
	public static boolean isEqualWithinRelativeTolerance(double pattern, double value, double tolerance)
	{
		if (pattern == Double.POSITIVE_INFINITY && value == Double.POSITIVE_INFINITY) return true;
		return (pattern == 0) ? isEqualWithinAbsoluteTolerance(value, pattern, tolerance) : ((Math.abs((pattern - value) / pattern) <= tolerance));
	}

	/**
	 * Joins the elements in an input array using a given separator. It is an improved version of {@code Arrays.toString()}.
	 *
	 * @param array Input array
	 * @param separator Separator
	 * @return {@code String} representation of the input {@code array}
	 */
	public static String join(double[] array, String separator)
	{
		if (array.length == 0) return "";

		StringBuilder out = new StringBuilder();
		out.append(array[0]);

		for (int i = 1; i < array.length; i++)
			out.append(separator).append(array[i]);

		return out.toString();
	}

	/**
	 * Converts the array into an String of space separated values. If the array is empty, "" is returned
	 *
	 * @param array Input array
	 * @return The string representation
	 */
	public static String toString (double[] array)
	{
		String s = ""; 
		for (int cont = 0; cont < array.length ; cont ++) 
		{
			if (cont > 0) s += " ";
			s += array [cont];
		}
		return s;
	}


	/**
	 * Returns a list of the comma-separated input values.
	 *
	 * @param values Comma-separated input values
	 * @return List of values (iteration order equal to insertion order)
	 */
	public static List<Double> listOf(double... values)
	{
		return toList(values);
	}

	/**
	 * Returns the maximum value in the input array.
	 *
	 * @param array Input array
	 * @return Maximum value
	 */
	public static double maxValue(double[] array)
	{
		if (array.length == 0) throw new NoSuchElementException("Empty array");

		double maxValue = array[0];
		for (int i = 1; i < array.length; i++)
			if (array[i] > maxValue)
				maxValue = array[i];

		return maxValue;
	}

	/**
	 * Returns the maximum value in the input array.
	 *
	 * @param <A> Key type
	 * @param map Input map
	 * @return Maximum value
	 */
	public static <A> double maxValue(Map<A, Double> map)
	{
		return maxValue(map.values());
	}

	/**
	 * Returns the maximum value in the input collection.
	 *
	 * @param collection Input collection
	 * @return Maximum value
	 */
	public static double maxValue(Collection<Double> collection)
	{
		if (collection.isEmpty()) throw new NoSuchElementException("Empty collection");

		double maxValue = -Double.MAX_VALUE;
		for (double value : collection)
			if (value > maxValue)
				maxValue = value;
		
		return maxValue;
	}
	
	/**
	 * Returns the position(s) in which the maximum value is found.
	 *
	 * @param array Input array
	 * @param searchType Indicates whether the first, the last, or all maximum positions are returned
	 * @param precisionFactor Tolerance factor (values with absolute difference lower or equal than {@code precisionFactor} will be considered equal)
	 * @return Position(s) in which the maximum value is found
	 */
	public static int[] maxIndexes(double[] array, Constants.SearchType searchType, double precisionFactor)
	{
		if (array.length == 0)
		{
			return new int[0];
		}
		
		List<Integer> candidateIndexes = new ArrayList<Integer>();
		candidateIndexes.add(0);

		double maxValue = array[0];
		for (int i = 1; i < array.length; i++)
		{
			if (array[i] > maxValue + precisionFactor)
			{
				maxValue = array[i];
				candidateIndexes.clear();
				candidateIndexes.add(i);
			}
			else if (Math.abs(array[i] - maxValue) <= precisionFactor)
			{
				switch (searchType)
				{
					case ALL:
						candidateIndexes.add(i);
						break;

					case FIRST:
						continue;

					case LAST:
						candidateIndexes.clear();
						candidateIndexes.add(i);
						break;

					default:
						throw new Net2PlanException("Invalid search type argument");
				}
			}
		}

		return IntUtils.toArray(candidateIndexes);
	}

	/**
	 * Returns the position(s) in which the maximum value is found.
	 *
	 * @param <A> Key type
	 * @param map Input map
	 * @param searchType Indicates whether the first, the last, or all maximum positions are returned
	 * @return Position(s) in which the maximum value is found
	 */
	public static <A> Set<A> maxIndexes(Map<A, Double> map, Constants.SearchType searchType)
	{
		if (map.isEmpty()) return new LinkedHashSet<A>();

		Set<A> candidateIndexes = new TreeSet<A>();
		double maxValue = -Double.MAX_VALUE;
		for (Entry<A, Double> entry : map.entrySet())
		{
			A index = entry.getKey();
			double value = entry.getValue();
			if (value > maxValue)
			{
				maxValue = value;
				candidateIndexes.clear();
				candidateIndexes.add(index);
			}
			else if (Math.abs(value - maxValue) < 1E-10)
			{
				switch (searchType)
				{
					case ALL:
						candidateIndexes.add(index);
						break;

					case FIRST:
						continue;

					case LAST:
						candidateIndexes.clear();
						candidateIndexes.add(index);
						break;

					default:
						throw new Net2PlanException("Invalid search type argument");
				}
			}
		}

		return candidateIndexes;
	}
	
	/**
	 * Returns the position(s) in which the minimum value is found.
	 *
	 * @param <A> Key type
	 * @param map Input map
	 * @param searchType Indicates whether the first, the last, or all minimum positions are returned
	 * @return Position(s) in which the minimum value is found
	 */
	public static <A> Set<A> minIndexes(Map<A, Double> map, Constants.SearchType searchType)
	{
		if (map.isEmpty()) return new LinkedHashSet<A>();

		Set<A> candidateIndexes = new TreeSet<A>();
		double minValue = Double.MAX_VALUE;
		for (Entry<A, Double> entry : map.entrySet())
		{
			A index = entry.getKey();
			double value = entry.getValue();
			if (value < minValue)
			{
				minValue = value;
				candidateIndexes.clear();
				candidateIndexes.add(index);
			}
			else if (Math.abs(value - minValue) < 1E-10)
			{
				switch (searchType)
				{
					case ALL:
						candidateIndexes.add(index);
						break;

					case FIRST:
						continue;

					case LAST:
						candidateIndexes.clear();
						candidateIndexes.add(index);
						break;

					default:
						throw new Net2PlanException("Invalid search type argument");
				}
			}
		}

		return candidateIndexes;
	}

	/**
	 * Returns the position(s) in which the maximum value is found.
	 *
	 * @param array Input array
	 * @param searchType Indicates whether the first, the last, or all maximum positions are returned
	 * @return Position(s) in which the maximum value is found
	 */
	public static int[] maxIndexes(double[] array, Constants.SearchType searchType)
	{
		return maxIndexes(array, searchType, 0);
	}

	/**
	 * <p>Returns the position(s) in which the maximum/minimum values are found.</p>
	 *
	 * <p>{@code out[0]} are the maximum positions, while {@code out[1]} are the minimum positions</p>
	 *
	 * @param array Input array
	 * @param searchType Indicates whether the first, the last, or all maximum positions are returned
	 * @param precisionFactor Tolerance factor (values with absolute difference lower or equal than {@code precisionFactor} will be considered equal)
	 * @return Position(s) in which the maximum/minimum value are found.
	 */
	public static int[][] maxMinIndexes(double[] array, Constants.SearchType searchType, double precisionFactor)
	{
		if (array.length == 0) return new int[2][0];

		List<Integer> candidateMaxIndexes = new ArrayList<Integer>();
		candidateMaxIndexes.add(0);
		List<Integer> candidateMinIndexes = new ArrayList<Integer>();
		candidateMinIndexes.add(0);

		double maxValue = array[0];
		double minValue = array[0];
		for (int i = 1; i < array.length; i++)
		{
			if (array[i] > maxValue + precisionFactor)
			{
				maxValue = array[i];
				candidateMaxIndexes.clear();
				candidateMaxIndexes.add(i);
			}
			else if (Math.abs(array[i] - maxValue) <= precisionFactor)
			{
				switch (searchType)
				{
					case ALL:
						candidateMaxIndexes.add(i);
						break;

					case FIRST:
						continue;

					case LAST:
						candidateMaxIndexes.clear();
						candidateMaxIndexes.add(i);
						break;

					default:
						throw new Net2PlanException("Invalid search type argument");
				}
			}

			if (array[i] < minValue - precisionFactor)
			{
				minValue = array[i];
				candidateMaxIndexes.clear();
				candidateMaxIndexes.add(i);
			}
			else if (Math.abs(array[i] - minValue) <= precisionFactor)
			{
				switch (searchType)
				{
					case ALL:
						candidateMaxIndexes.add(i);
						break;

					case FIRST:
						continue;

					case LAST:
						candidateMaxIndexes.clear();
						candidateMaxIndexes.add(i);
						break;

					default:
						throw new Net2PlanException("Invalid search type argument");
				}
			}
		}

		return new int[][]
		{
			IntUtils.toArray(candidateMaxIndexes), IntUtils.toArray(candidateMinIndexes)
		};
	}

	/**
	 * <p>Returns the position(s) in which the maximum/minimum values are found.</p>
	 *
	 * <p>{@code out[0]} are the maximum positions, while {@code out[1]} are the minimum positions</p>
	 *
	 * @param array Input array
	 * @param searchType Indicates whether the first, the last, or all maximum positions are returned
	 * @return Position(s) in which the maximum/minimum value are found.
	 */
	public static int[][] maxMinIndexes(double[] array, Constants.SearchType searchType)
	{
		return maxMinIndexes(array, searchType, 0);
	}

	/**
	 * Returns the maximum/minimum values of an input collection.
	 *
	 * @param collection Input collection
	 * @return Maximum/minimum values
	 */
	public static double[] maxMinValues(Collection<Double> collection)
	{
		if (collection.isEmpty()) throw new NoSuchElementException("Empty collection");

		double maxValue = -Double.MAX_VALUE;
		double minValue = Double.MAX_VALUE;
		for (double value : collection)
		{
			if (value > maxValue) maxValue = value;
			if (value < minValue) minValue = value;
		}

		return new double[] { maxValue, minValue };
	}
	
	/**
	 * Returns the maximum/minimum values of an input array.
	 *
	 * @param <A> map key
	 * @param map Input map
	 * @return Maximum/minimum values
	 */
	public static <A> double[] maxMinValues(Map<A, Double> map)
	{
		return maxMinValues(map.values());
	}
	
	/**
	 * Returns the maximum/minimum values of an input array.
	 *
	 * @param array Input array
	 * @return Maximum/minimum values
	 */
	public static double[] maxMinValues(double[] array)
	{
		if (array.length == 0)
		{
			throw new NoSuchElementException("Empty array");
		}

		double maxValue = array[0];
		double minValue = array[0];
		for (int i = 1; i < array.length; i++)
		{
			if (array[i] > maxValue) maxValue = array[i];
			if (array[i] < minValue) minValue = array[i];
		}

		return arrayOf(maxValue, minValue);
	}

	/**
	 * Returns the maximum value in the input array.
	 *
	 * @param array Input array
	 * @return Maximum value
	 */
	public static double maxValue(double[][] array)
	{
		if (array.length == 0)
		{
			throw new NoSuchElementException("Empty array");
		}

		double maxValue = -Double.MAX_VALUE;

		for (double[] array1 : array)
			for (int j = 0; j < array1.length; j++)
				if (array1[j] > maxValue)
					maxValue = array1[j];

		return maxValue;
	}

	/**
	 * Returns the minimum value in the input array.
	 *
	 * @param array Input array
	 * @return Minimum value
	 */
	public static double minValue(double[] array)
	{
		if (array.length == 0) throw new NoSuchElementException("Empty array");

		double minValue = array[0];
		for (int i = 1; i < array.length; i++)
			if (array[i] < minValue)
				minValue = array[i];

		return minValue;
	}

	/**
	 * Returns the minimum value in the input matrix.
	 *
	 * @param matrix Input matrix
	 * @return Minimum value
	 */
	public static double minValue(double[][] matrix)
	{
		if (matrix.length == 0) throw new NoSuchElementException("Empty array");

		double minValue = matrix[0][0];
		for (double[] matrix1 : matrix)
			for (int j = 0; j < matrix1.length; j++)
				if (matrix1[j] < minValue)
					minValue = matrix1[j];

		return minValue;
	}
	
	/**
	 * Returns the minimum value in the input collection.
	 *
	 * @param collection Input collection
	 * @return Minimum value
	 */
	public static double minValue(Collection<Double> collection)
	{
		if (collection.isEmpty()) throw new NoSuchElementException("Empty collection");
		
		double minValue = Double.MAX_VALUE;
		for (double value : collection)
			if (value < minValue)
				minValue = value;

		return minValue;
	}

	/**
	 * Returns the minimum value in the input map.
	 *
	 * @param <A> Key type
	 * @param map Input map
	 * @return Minimum value
	 */
	public static <A> double minValue(Map<A, Double> map)
	{
		return minValue(map.values());
	}
	
	/**
	 * Returns the position(s) in which the minimum value is found.
	 *
	 * @param array Input array
	 * @param searchType Indicates whether the first, the last, or all minimum positions are returned
	 * @return Position(s) in which the minimum value is found
	 */
	public static int[] minIndexes(double[] array, Constants.SearchType searchType)
	{
		if (array.length == 0)
		{
			return new int[0];
		}

		List<Integer> candidateIndexes = new ArrayList<Integer>();
		candidateIndexes.add(0);

		double minValue = array[0];
		for (int i = 1; i < array.length; i++)
		{
			if (array[i] < minValue)
			{
				minValue = array[i];
				candidateIndexes.clear();
				candidateIndexes.add(i);
			}
			else if (Math.abs(array[i] - minValue) < 1E-10)
			{
				switch (searchType)
				{
					case ALL:
						candidateIndexes.add(i);
						break;

					case FIRST:
						continue;

					case LAST:
						candidateIndexes.clear();
						candidateIndexes.add(i);
						break;

					default:
						throw new Net2PlanException("Invalid search type argument");
				}
			}
		}

		return IntUtils.toArray(candidateIndexes);
	}

	/**
	 * Returns the element-wise sum of two maps.
	 *
	 * @param <A> Key type
	 * @param map1 Input map 1
	 * @param map2 Input map 2
	 * @return A new map with the element-wise sum
	 */
	public static <A> Map<A, Double> sum(Map<A, Double> map1, Map<A, Double> map2)
	{
		Map<A, Double> out = new LinkedHashMap<A, Double>();

		for (Entry<A, Double> entry : map1.entrySet())
		{
			A key = entry.getKey();
			out.put(key, entry.getValue() + map2.get(key));
		}

		return out;
	}
	
	/**
	 * Multiplies all elements in an array by a scalar.
	 *
	 * @param array Input array
	 * @param value Scalar
	 * @return A new array containing the input elements multiplied by the scalar
	 */
	public static double[] mult(double[] array, double value)
	{
		double[] out = new double[array.length];

		for (int i = 0; i < out.length; i++)
		{
			out[i] = array[i] * value;
		}

		return out;
	}

	/**
	 * Multiplies all elements in an array by a scalar.
	 *
	 * @param <A> Key type
	 * @param map Input map
	 * @param value Scalar
	 * @return A new map containing the input elements multiplied by the scalar
	 */
	public static <A> Map<A, Double> mult(Map<A, Double> map, double value)
	{
		Map<A, Double> out = new LinkedHashMap<A, Double>();
		for(Entry<A, Double> entry : map.entrySet())
			out.put(entry.getKey(), entry.getValue() * value);
		
		return out;
	}
	
	/**
	 * Multiplies all elements in a matrix by a scalar.
	 *
	 * @param matrix Input array
	 * @param value Scalar
	 * @return A new matrix containing the input elements multiplied by the scalar
	 */
	public static double[][] mult(double[][] matrix, double value)
	{
		double[][] out = new double[matrix.length][];

		for (int i = 0; i < out.length; i++)
			out[i] = DoubleUtils.mult(matrix[i], value);

		return out;
	}

	/**
	 * Multiplies two arrays element-to-element.
	 *
	 * @param array1 Input array 1
	 * @param array2 Input array 2
	 * @return The element-wise multiplication of the input arrays
	 */
	public static double[] mult(double[] array1, double[] array2)
	{
		double[] out = new double[array1.length];

		for (int i = 0; i < out.length; i++)
			out[i] = array1[i] * array2[i];

		return out;
	}

	/**
	 * Returns the element-wise product of two maps.
	 *
	 * @param <A> Key type
	 * @param map1 Input map 1
	 * @param map2 Input map 2
	 * @return A new map with the element-wise product
	 */
	public static <A> Map<A, Double> mult(Map<A, Double> map1, Map<A, Double> map2)
	{
		Map<A, Double> out = new LinkedHashMap<A, Double>();

		for (Entry<A, Double> entry : map1.entrySet())
		{
			A key = entry.getKey();
			out.put(key, entry.getValue() * map2.get(key));
		}

		return out;
	}
	
	/**
	 * Returns an array filled with ones.
	 *
	 * @param N Number of elements
	 * @return An all-one array of length {@code N}
	 */
	public static double[] ones(int N)
	{
		double[] array = new double[N];
		Arrays.fill(array, 1);

		return array;
	}

	/**
	 * Returns a map filled with ones.
	 *
	 * @param <A> Key type
	 * @param identifiers Set of map keys
	 * @return An all-one map
	 */
	public static <A> Map<A, Double> ones(Set<A> identifiers)
	{
		return constantMap(identifiers, 1);
	}
	
	/**
	 * Reads a 2D matrix from a file. Each line in the file is a row of the matrix.
	 * Items can be separated by spaces or tabs. Lines starting with '#' are skipped.
	 * 
	 * @param file Input file
	 * @return A 2D matrix
	 */
	public static DoubleMatrix2D read2DMatrixFromFile(File file)
	{
		try
		{
			List<List<Double>> list;
			try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)))
			{
				list = new LinkedList<List<Double>>();
				String line;
				while ((line = in.readLine()) != null)
				{
					if (line.startsWith("#")) continue;

					List<Double> aux = new LinkedList<Double>();
					
					StringTokenizer tokenizer = new StringTokenizer(line, " \t");
					while (tokenizer.hasMoreTokens()) aux.add(Double.parseDouble(tokenizer.nextToken()));

					list.add(aux);
				}
			}

			int numRows = list.size();
			if (numRows == 0) throw new Net2PlanException("Empty matrix");

			double[][] matrix = new double[numRows][];
			ListIterator<List<Double>> it = list.listIterator();
			while (it.hasNext())
				matrix[it.nextIndex()] = toArray(it.next());

			int columns = matrix[0].length;
			for (int rowId = 1; rowId < matrix.length; rowId++)
				if (matrix[rowId].length != columns)
					throw new Net2PlanException("All rows don't have the same number of columns");

			return DoubleFactory2D.dense.make(matrix);
		}
		catch (IOException | NumberFormatException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Reverses the order of the elements of the input array (it will be overriden).
	 *
	 * @param array Input array
	 */
	public static void reverse(double[] array)
	{
		for (int i = 0; i < array.length / 2; ++i)
		{
			double temp = array[i];
			array[i] = array[array.length - i - 1];
			array[array.length - i - 1] = temp;
		}
	}

	/**
	 * Rounds a number to the closest {@code double} given the number of
	 * required decimal places.
	 *
	 * @param number Number to be rounded
	 * @param decimals Decimal places
	 * @return Rounded number
	 * @since 0.2.2
	 */
	public static double round(double number, int decimals)
	{
		double out = number * Math.pow(10, decimals);
		out = Math.round(out) / Math.pow(10, decimals);

		return out;
	}

	/**
	 * Rounds a number to the nearest {@code double} given the number of
	 * required decimal places.
	 *
	 * @param array Array to be rounded
	 * @param decimals Decimal places
	 * @return Rounded number
	 */
	public static double[] round(double[] array, int decimals)
	{
		double[] out = new double[array.length];
		for (int i = 0; i < array.length; i++)
		{
			out[i] = round(array[i], decimals);
		}

		return out;
	}

	/**
	 * Scalar product of two vectors.
	 *
	 * @param array1 Input array 1
	 * @param array2 Input array 2
	 * @return The scalar product of the input arrays
	 */
	public static double scalarProduct(double[] array1, double[] array2)
	{
		return sum(mult(array1, array2));
	}

	/**
	 * Scalar product of two maps.
	 *
	 * @param <A> Key type
	 * @param map1 Input map 1
	 * @param map2 Input map 2
	 * @return The scalar product of the input maps
	 */
	public static <A> double scalarProduct(Map<A, Double> map1, Map<A, Double> map2)
	{
		double out = 0;
		for(Entry<A, Double> entry : map1.entrySet())
		{
			A key = entry.getKey();
			out += map1.get(key) * map2.get(key);
		}
		
		return out;
	}
	
	/**
	 * Returns a set of selected elements from an input array. It is not backed
	 * in the input array, thus changes in this array are not reflected in the
	 * input array.
	 *
	 * @param array Input array
	 * @param indexes Position of elements to be selected.
	 * @return Set of elements in the order given by {@code indexes}
	 */
	public static double[] select(double[] array, int[] indexes)
	{
		double[] out = new double[indexes.length];

		for (int i = 0; i < indexes.length; i++)
		{
			out[i] = array[indexes[i]];
		}

		return out;
	}
	
	/**
	 * Returns a column of a bidimensional input array.
	 *
	 * @param array Input array
	 * @param column Column
	 * @return Column of an array
	 */
	public static double[] selectColumn(double[][] array, int column)
	{
		double[] output = new double[array.length];

		for (int i = 0; i < output.length; i++)
		{
			output[i] = array[i][column];
		}

		return output;
	}

	/**
	 * Returns a row of a bidimensional input array.
	 *
	 * @param array Input array
	 * @param row Row
	 * @return Row of an array
	 */
	public static double[] selectRow(double[][] array, int row)
	{
		return array[row];
	}

	/**
	 * Returns a list of the comma-separated input values.
	 *
	 * @param values Comma-separated input values
	 * @return List of values (iteration order equal to insertion order)
	 */
	public static Set<Double> setOf(double... values)
	{
		return new LinkedHashSet<Double>(toList(values));
	}

	/**
	 * Returns the elements contained in the first array, but not any of the others.
	 *
	 * @param arrays Input arrays
	 * @return Difference set
	 */
	public static double[] setdiff(double[]... arrays)
	{
		Set<Double> differenceSet = new LinkedHashSet<Double>();
		for (int i = 0; i < arrays[0].length; i++)
		{
			differenceSet.add(arrays[0][i]);
		}

		for (int i = 1; i < arrays.length; i++)
		{
			List<Double> aux = toList(arrays[i]);
			differenceSet.removeAll(aux);
		}

		double[] differenceArray = toArray(differenceSet);

		return differenceArray;
	}

	/**
	 * Sorts the input array (it will be overriden).
	 *
	 * @param array Input array
	 * @param orderingType Ascending or descending
	 */
	public static void sort(double[] array, Constants.OrderingType orderingType)
	{
		Arrays.sort(array);

		switch (orderingType)
		{
			case ASCENDING:
				break;

			case DESCENDING:
				reverse(array);
				break;

			default:
				throw new Net2PlanException("Invalid ordering type argument");
		}
	}

	/**
	 * Sorts indexes of the {@code array} into ascending/descending order in a stable way. Stable means that index order doesn't change if values are the same.
	 *
	 * @param array Array to be sorted
	 * @param orderingType Ascending or descending
	 * @return Sorted indexes
	 */
	public static int[] sortIndexes(double[] array, Constants.OrderingType orderingType)
	{
		ArrayIndexComparator comparator = new ArrayIndexComparator(array, orderingType);
		Integer[] indexes = comparator.createIndexArray();
		Arrays.sort(indexes, comparator);
		
		return IntUtils.asPrimitiveArray(indexes);
	}

	/**
	 * Returns the standard deviation of an array using the Welford's method.
	 *
	 * @param array Input array
	 * @return Standard deviation
	 */
	public static double std(double[] array)
	{
		if (array.length == 0) return 0;
		
		double M = 0.0;
		double S = 0.0;
		int k = 1;
		for (double value : array) 
		{
			double tmpM = M;
			M += (value - tmpM) / k;
			S += (value - tmpM) * (value - M);
			k++;
		}
		
		return Math.sqrt(S / (k-1));
	}		

	/**
	 * Returns the standard deviation of a collection using the Welford's method.
	 *
	 * @param collection Collection of numbers
	 * @return Standard deviation (or zero, if {@code collection} is empty
	 */
	public static double std(Collection<Double> collection)
	{
		if (collection.isEmpty()) return 0;
		
		double M = 0.0;
		double S = 0.0;
		int k = 1;
		for (double value : collection) 
		{
			double tmpM = M;
			M += (value - tmpM) / k;
			S += (value - tmpM) * (value - M);
			k++;
		}
		
		return Math.sqrt(S / (k-1));
	}		

	/**
	 * Returns the standard deviation of values of a map using the Welford's method.
	 *
	 * @param <A> Key type
	 * @param map Input map
	 * @return Standard deviation
	 */
	public static <A> double std(Map<A, Double> map)
	{
		return std(map.values());
	}
	
	/**
	 * Returns the element-wise substraction of two arrays.
	 *
	 * @param array1 Input array 1
	 * @param array2 Input array 2
	 * @return A new array with the element-wise subtraction
	 */
	public static double[] substract(double[] array1, double[] array2)
	{
		double[] out = new double[array1.length];

		for (int i = 0; i < out.length; i++)
			out[i] = array1[i] - array2[i];

		return out;
	}

	/**
	 * Returns the sum of all elements in the input array.
	 *
	 * @param array Input array
	 * @return Sum of all array elements
	 */
	public static double sum(double[] array)
	{
		double out = 0;
		for (int i = 0; i < array.length; i++)
			out += array[i];

		return out;
	}

	/**
	 * Returns the sum of all elements in the input matrix.
	 *
	 * @param matrix Input matrix
	 * @return Sum of all matrix elements
	 */
	public static double sum(double[][] matrix)
	{
		double out = 0;
		for (double[] matrix1 : matrix)
			out += sum(matrix1);

		return out;
	}
	
	/**
	 * Returns the sum of all elements in the input collection.
	 *
	 * @param collection Input collection
	 * @return Sum of all collection values
	 */
	public static double sum(Collection<Double> collection)
	{
		double out = 0;
		for(double value : collection)
			out += value;

		return out;
	}
	
	/**
	 * Returns the sum of all elements in the input collection.
	 *
	 * @param <A> Key type
	 * @param map Input map
	 * @return Sum of all map values
	 */
	public static <A> double sum(Map<A, Double> map)
	{
		return sum(map.values());
	}

	/**
	 * Returns the element-wise sum of two arrays.
	 *
	 * @param array1 Input array 1
	 * @param array2 Input array 2
	 * @return A new array with the element-wise sum
	 */
	public static double[] sum(double[] array1, double[] array2)
	{
		double[] out = new double[array1.length];

		for (int i = 0; i < out.length; i++)
			out[i] = array1[i] + array2[i];

		return out;
	}
	
	/**
	 * Converts a collection ({@code List}, {@code Set}...) of {@code Double} objects to a {@code double} array.
	 *
	 * @param list Input list
	 * @return {@code int} array
	 */
	public static double[] toArray(Collection<Double> list)
	{
		return asPrimitiveArray(list.toArray(new Double[list.size()]));
	}

	/**
	 * Converts an array of {@code String} with the values, into a {@code double} array. Uses {@code Double.parseDouble} to parse the number, an can raise the exceptions 
	 * that this method raises
	 *
	 * @param list Input list
	 * @return output array
	 */
	public static double [] toArray(String [] list)
	{
		double [] res = new double [list.length];
		int counter = 0;
		for (String s : list)
			res [counter ++] = Double.parseDouble (s);
		return res;
	}

	
	/**
	 * Converts a {@code double} array to an {@code int} array
	 *
	 * @param array Input array
	 * @return {@code int} array
	 */
	public static int[] toIntArray(double[] array)
	{
		int[] out = new int[array.length];
		for (int i = 0; i < out.length; i++)
			out[i] = (int) Math.round(array[i]);

		return out;
	}

	/**
	 * Converts from a {@code double} array to a list.
	 *
	 * @param array Input array
	 * @return A list of {@code Double} objects
	 */
	public static List<Double> toList(double[] array)
	{
		List<Double> list = new ArrayList<Double>();
		for (int i = 0; i < array.length; i++)
			list.add(array[i]);

		return list;
	}

	/**
	 * Returns an array with all elements in input arrays (no repetitions). There is no order guarantee.
	 *
	 * @param arrays Input arrays
	 * @return A new array with all elements in input arrays
	 */
	public static double[] union(double[]... arrays)
	{
		Set<Double> unionSet = new LinkedHashSet<Double>();
		for (double[] array : arrays)
			for (int j = 0; j < array.length; j++)
				unionSet.add(array[j]);

		double[] unionArray = toArray(unionSet);

		return unionArray;
	}

	/**
	 * Returns the same values of the input {@code array} but with no repetitions. There is no order guarantee.
	 *
	 * @param array Input array
	 * @return Same values as in {@code array} but with no repetitions
	 */
	public static double[] unique(double[] array)
	{
		Set<Double> uniqueSet = new LinkedHashSet<Double>();
		for (int i = 0; i < array.length; i++)
			uniqueSet.add(array[i]);

		return toArray(uniqueSet);
	}

	/**
	 * Returns a map filled with zeros.
	 *
	 * @param <A> Key type
	 * @param identifiers Set of unique identifiers keys
	 * @return An all-zero map
	 */
	public static <A> Map<A, Double> zeros(Set<A> identifiers)
	{
		return constantMap(identifiers, 0.0);
	}

	/**
	 * Returns an array filled with zeros.
	 *
	 * @param N Number of elements
	 * @return An all-zero array of length {@code N}
	 */
	public static double[] zeros(int N)
	{
		double[] array = new double[N];
		Arrays.fill(array, 0);

		return array;
	}

	private static class ArrayIndexComparator implements Comparator<Integer>, Serializable
	{
		private static final long serialVersionUID = 1L;
		
		private final Constants.OrderingType orderingType;
		private final double[] array;

		public ArrayIndexComparator(double[] array, Constants.OrderingType orderingType)
		{
			this.array = array;
			this.orderingType = orderingType;
		}

		public Integer[] createIndexArray()
		{
			Integer[] indexes = new Integer[array.length];
			for (int i = 0; i < array.length; i++)
				indexes[i] = i;

			return indexes;
		}

		@Override
		public int compare(Integer index1, Integer index2)
		{
			switch(orderingType)
			{
				case ASCENDING:
					return Double.compare(array[index1], array[index2]);
					
				case DESCENDING:
					return Double.compare(array[index2], array[index1]);
					
				default:
					throw new RuntimeException("Bad");
			}
		}
	}	
}
