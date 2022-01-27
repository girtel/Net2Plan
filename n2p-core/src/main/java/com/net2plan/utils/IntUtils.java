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

import com.net2plan.interfaces.networkDesign.Net2PlanException;

import java.io.Serializable;
import java.util.*;
import java.util.Map.Entry;

/**
 * <p>Provides extra functionality for {@code int} primitives.</p>
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 */
public class IntUtils
{
	private IntUtils() { }

	/**
	 * Generates an {@code int[]} from comma-separated values.
	 *
	 * @param values Comma-separated {@code int} values
	 * @return {@code int[]}
	 */
	public static int[] arrayOf(int... values)
	{
		return values;
	}
	
	/**
	 * Converts from an {@code int} array to an {@code Integer} array.
	 *
	 * @param array {@code int} array
	 * @return Equivalent {@code Integer} array
	 */
	public static Integer[] asObjectArray(int[] array)
	{
		Integer[] objectArray = new Integer[array.length];

		for (int i = 0; i < array.length; i++)
			objectArray[i] = array[i];

		return objectArray;
	}

	/**
	 * Converts from an {@code Integer} array to an {@code int} array.
	 *
	 * @param array {@code Integer} array
	 * @return Equivalent {@code int} array
	 */
	public static int[] asPrimitiveArray(Integer[] array)
	{
		int[] primitiveArray = new int[array.length];

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
	public static double average(int[] array)
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
	public static <A> double average(Map<A, Integer> map)
	{
		return average(map.values());
	}

	/**
	 * Returns the average value of a collection.
	 *
	 * @param collection Input collection
	 * @return Average value (or zero, if {@code collection} is empty)
	 */
	public static double average(Collection<Integer> collection)
	{
		return collection.isEmpty() ? 0 : sum(collection) / collection.size();
	}
	
	/**
	 * Returns the average value only among non-zero values
	 *
	 * @param array Input array
	 * @return Average value among non-zero values
	 */
	public static double averageNonZeros(int[] array)
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
	 * Concatenates a series of arrays.
	 *
	 * @param arrays List of arrays
	 * @return Concatenated array
	 */
	public static int[] concatenate(int[]... arrays)
	{
		List<Integer> list = new LinkedList<Integer>();

		for (int[] array : arrays)
			list.addAll(toList(array));

		return toArray(list);
	}

	/**
	 * Returns an array filled with a given value.
	 *
	 * @param N Number of elements
	 * @param value Value for all elements
	 * @return An array of length {@code N} with the given value
	 */
	public static int[] constantArray(int N, int value)
	{
		int[] array = new int[N];
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
	public static <A> Map<A, Integer> constantMap(Set<A> identifiers, int value)
	{
		Map<A, Integer> out = new LinkedHashMap<A, Integer>();
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
	public static boolean contains(int[] array, int value)
	{
		for (int i = 0; i < array.length; i++)
			if (array[i] == value)
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
	public static boolean containsAll(int[] container, int[] array)
	{
		if (container.length == 0) return false;

		Set<Integer> set = new LinkedHashSet<Integer>(toList(container));
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
	public static boolean containsAny(int[] container, int[] array)
	{
		if (container.length == 0) return false;

		for (int i = 0; i < array.length; i++)
			for (int j = 0; j < container.length; j++)
				if (array[i] == container[j])
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
	public static <A extends Number> Map<A, Integer> convertArray2Map(Set<A> identifiers, int[] array)
	{
		Map<A, Integer> id2LinearIndex = CollectionUtils.convertId2LinearIndexMap(identifiers);
		Map<A, Integer> out = new LinkedHashMap<A, Integer>();
		for(Map.Entry<A, Integer> entry : id2LinearIndex.entrySet())
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
	 * @param table Input table
	 * @return Map
	 */
	public static <A extends Number, B extends Number> Map<Pair<A, B>, Integer> convertTable2Map(Set<A> rowIdentifiers, Set<B> columnIdentifiers, int[][] table)
	{
		Map<A, Integer> rowId2LinearIndex = CollectionUtils.convertId2LinearIndexMap(rowIdentifiers);
		Map<B, Integer> columnId2LinearIndex = CollectionUtils.convertId2LinearIndexMap(columnIdentifiers);
		Map<Pair<A, B>, Integer> out = new LinkedHashMap<Pair<A, B>, Integer>();
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
	public static int[] copy(int[] array)
	{
		return Arrays.copyOf(array, array.length);
	}

	/**
	 * Returns a deep copy of the input {@code array}.
	 *
	 * @param array Input array
	 * @return Deep copy of {@code array}
	 */
	public static int[][] copy(int[][] array)
	{
		int[][] out = new int[array.length][];
		for (int rowId = 0; rowId < array.length; rowId++)
			out[rowId] = copy(array[rowId]);

		return out;
	}

	/**
	 * Divides two arrays element-to-element.
	 *
	 * @param array1 Numerator
	 * @param array2 Denominator
	 * @return The element-wise division of the input arrays
	 */
	public static double[] divide(int[] array1, int[] array2)
	{
		double[] out = new double[array1.length];
		for (int i = 0; i < out.length; i++)
			out[i] = (double) array1[i] / array2[i];

		return out;
	}

	/**
	 * Divides all elements in an array by a scalar.
	 *
	 * @param array Input array
	 * @param value Scalar
	 * @return A new array containing the input elements divided by the scalar
	 */
	public static double[] divide(int[] array, double value)
	{
		double[] out = new double[array.length];
		for (int i = 0; i < out.length; i++)
			out[i] = array[i] / value;

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
	public static <A> Map<A, Double> divide(Map<A, Integer> map, double value)
	{
		Map<A, Double> out = new LinkedHashMap<A, Double>();

		for (Map.Entry<A, Integer> entry : map.entrySet())
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
	public static <A> Map<A, Double> divide(Map<A, Integer> map1, Map<A, Integer> map2)
	{
		Map<A, Double> out = new LinkedHashMap<A, Double>();

		for (Map.Entry<A, Integer> entry : map1.entrySet())
		{
			A key = entry.getKey();
			out.put(key, (double) entry.getValue() / map2.get(key));
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
	public static double[] divideNonSingular(int[] array1, int[] array2)
	{
		double[] out = new double[array1.length];
		for (int i = 0; i < out.length; i++)
			out[i] = array1[i] == 0 && array2[i] == 0 ? 0 : (double) array1[i] / array2[i];

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
	public static int[] find(int[] array, int value, Constants.SearchType searchType)
	{
		switch(searchType)
		{
			case ALL:
			case FIRST:
				List<Integer> candidateIndexes = new LinkedList<Integer>();
				for(int i = 0; i < array.length; i++)
				{
					if(array[i] == value)
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
	 * Computes the greatest absolute common divisor of an integer array.
	 *
	 * @param array Input array
	 * @return The greatest absolute common divisor. By convention, {@code gcd(0, 0)} is equal to zero and {@code gcd([])} is equal to one
	 */
	public static int gcd(int[] array)
	{
		if (array.length == 0)
		{
			return 1;
		}

		int[] maxMinValue = maxMinValues(array);
		int minAbsValue = Math.min(Math.abs(maxMinValue[0]), Math.abs(maxMinValue[1]));

		for (int i = minAbsValue; i >= 1; i--)
		{
			int j;

			for (j = 0; j < array.length; ++j)
			{
				if (Math.abs(array[j]) % i != 0)
				{
					break;
				}
			}

			if (j == array.length)
			{
				return i;
			}
		}

		return 1;
	}

	/**
	 * Computes the greatest common divisor of an input collection.
	 *
	 * @param collection Input collection
	 * @return The greatest common divisor. By convention, {@code gcd(0, 0)} is equal to zero and {@code gcd([])} is equal to one
	 */
	public static int gcd(Collection<Integer> collection)
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
	public static int[] intersect(int[]... arrays)
	{
		if (arrays.length == 0) return new int[0];

		Set<Integer> intersectionSet = new LinkedHashSet<Integer>();
		intersectionSet.addAll(toList(arrays[0]));

		for (int i = 1; i < arrays.length; i++)
			intersectionSet.retainAll(toList(arrays[i]));

		return toArray(intersectionSet);
	}

	/**
	 * Joins the elements in an input array using a given separator. It is an improved version of {@code Arrays.toString()}.
	 *
	 * @param array Input array
	 * @param separator Separator
	 * @return {@code String} representation of the input {@code array}
	 */
	public static String join(int[] array, String separator)
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
	public static String toString (int[] array)
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
	 * Converts an array of {@code String} with the values, into a {@code int} array. Uses {@code Integer.parseInt} to parse the number, an can raise the exceptions 
	 * that this method raises
	 *
	 * @param list Input list
	 * @return output array
	 */
	public static int [] toArray(String [] list)
	{
		int [] res = new int [list.length];
		int counter = 0;
		for (String s : list)
			res [counter ++] = Integer.parseInt (s);
		return res;
	}

	/**
	 * Returns a list of the comma-separated input values.
	 *
	 * @param values Comma-separated input values
	 * @return List of values (iteration order equal to insertion order)
	 */
	public static List<Integer> listOf(int... values)
	{
		return toList(values);
	}

	/**
	 * Returns the position(s) in which the maximum value is found.
	 *
	 * @param array Input array
	 * @param searchType Indicates whether the first, the last, or all maximum positions are returned
	 * @return Position(s) in which the maximum value is found
	 */
	public static int[] maxIndexes(int[] array, Constants.SearchType searchType)
	{
		if (array.length == 0) return new int[0];

		List<Integer> candidateIndexes = new ArrayList<Integer>();
		candidateIndexes.add(0);

		int maxValue = array[0];
		for (int i = 1; i < array.length; i++)
		{
			if (array[i] > maxValue)
			{
				maxValue = array[i];
				candidateIndexes.clear();
				candidateIndexes.add(i);
			}
			else if (array[i] == maxValue)
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

		return toArray(candidateIndexes);
	}

	/**
	 * Returns the position(s) in which the maximum value is found.
	 *
	 * @param <A> Key type
	 * @param map Input map
	 * @param searchType Indicates whether the first, the last, or all maximum positions are returned
	 * @return Position(s) in which the maximum value is found
	 */
	public static <A> Set<A> maxIndexes(Map<A, Integer> map, Constants.SearchType searchType)
	{
		if (map.isEmpty()) return new LinkedHashSet<A>();

		Set<A> candidateIndexes = new TreeSet<A>();
		int maxValue = Integer.MIN_VALUE;
		for (Map.Entry<A, Integer> entry : map.entrySet())
		{
			A index = entry.getKey();
			int value = entry.getValue();
			if (value > maxValue)
			{
				maxValue = value;
				candidateIndexes.clear();
				candidateIndexes.add(index);
			}
			else if (value == maxValue)
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
	 * Returns the maximum value in the input array.
	 *
	 * @param array Input array
	 * @return Maximum value
	 */
	public static int maxValue(int[] array)
	{
		if (array.length == 0) throw new NoSuchElementException("Empty array");

		int maxValue = array[0];
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
	public static <A> int maxValue(Map<A, Integer> map)
	{
		return maxValue(map.values());
	}

	/**
	 * Returns the maximum value in the input collection.
	 *
	 * @param collection Input collection
	 * @return Maximum value
	 */
	public static int maxValue(Collection<Integer> collection)
	{
		if (collection.isEmpty()) throw new NoSuchElementException("Empty collection");

		int maxValue = Integer.MIN_VALUE;
		for (int value : collection)
			if (value > maxValue)
				maxValue = value;
		
		return maxValue;
	}
	
	/**
	 * Returns the position(s) in which the minimum value is found.
	 *
	 * @param <A> Key type
	 * @param map Input map
	 * @param searchType Indicates whether the first, the last, or all minimum positions are returned
	 * @return Position(s) in which the minimum value is found
	 */
	public static <A> Set<A> minIndexes(Map<A, Integer> map, Constants.SearchType searchType)
	{
		if (map.isEmpty()) return new LinkedHashSet<A>();

		Set<A> candidateIndexes = new TreeSet<A>();
		int minValue = Integer.MAX_VALUE;
		for (Map.Entry<A, Integer> entry : map.entrySet())
		{
			A index = entry.getKey();
			int value = entry.getValue();
			if (value < minValue)
			{
				minValue = value;
				candidateIndexes.clear();
				candidateIndexes.add(index);
			}
			else if (value == minValue)
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
	 * Returns the maximum/minimum values of an input collection.
	 *
	 * @param collection Input collection
	 * @return Maximum/minimum values
	 */
	public static int[] maxMinValues(Collection<Integer> collection)
	{
		if (collection.isEmpty()) throw new NoSuchElementException("Empty collection");

		int maxValue = Integer.MIN_VALUE;
		int minValue = Integer.MAX_VALUE;
		for (int value : collection)
		{
			if (value > maxValue) maxValue = value;
			if (value < minValue) minValue = value;
		}

		return arrayOf(maxValue, minValue);
	}
	
	/**
	 * Returns the maximum/minimum values of an input array.
	 *
	 * @param <A> the map key type
	 * @param map Input map
	 * @return Maximum/minimum values
	 */
	public static <A> int[] maxMinValues(Map<A, Integer> map)
	{
		return maxMinValues(map.values());
	}
	
	/**
	 * Returns the maximum/minimum values of an input array.
	 *
	 * @param array Input array
	 * @return Maximum/minimum values
	 */
	public static int[] maxMinValues(int[] array)
	{
		if (array.length == 0)
		{
			throw new NoSuchElementException("Empty array");
		}

		int maxValue = array[0];
		int minValue = array[0];
		for (int i = 1; i < array.length; i++)
		{
			if (array[i] > maxValue)
				maxValue = array[i];

			if (array[i] < minValue)
				minValue = array[i];
		}

		return arrayOf(maxValue, minValue);
	}

	/**
	 * Returns the maximum value in the input array.
	 *
	 * @param array Input array
	 * @return Maximum value
	 */
	public static int maxValue(int[][] array)
	{
		if (array.length == 0) throw new NoSuchElementException("Empty array");

		int maxValue = Integer.MIN_VALUE;

		for (int[] array1 : array)
			for (int j = 0; j < array1.length; j++)
				if (array1[j] > maxValue)
					maxValue = array1[j];

		return maxValue;
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
	public static int[][] maxMinIndexes(int[] array, Constants.SearchType searchType)
	{
		if (array.length == 0)
		{
			return new int[2][0];
		}

		List<Integer> candidateMaxIndexes = new ArrayList<Integer>();
		candidateMaxIndexes.add(0);
		List<Integer> candidateMinIndexes = new ArrayList<Integer>();
		candidateMinIndexes.add(0);

		int maxValue = array[0];
		int minValue = array[0];
		for (int i = 1; i < array.length; i++)
		{
			if (array[i] > maxValue)
			{
				maxValue = array[i];
				candidateMaxIndexes.clear();
				candidateMaxIndexes.add(i);
			}
			else if (array[i] == maxValue)
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

			if (array[i] < minValue)
			{
				minValue = array[i];
				candidateMaxIndexes.clear();
				candidateMaxIndexes.add(i);
			}
			else if (array[i] == minValue)
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
			toArray(candidateMaxIndexes), toArray(candidateMinIndexes)
		};
	}

	/**
	 * Returns the minimum value in the input array.
	 *
	 * @param array Input array
	 * @return Minimum value
	 */
	public static int minValue(int[] array)
	{
		if (array.length == 0) throw new NoSuchElementException("Empty array");

		int minValue = array[0];
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
	public static double minValue(int[][] matrix)
	{
		if (matrix.length == 0) throw new NoSuchElementException("Empty array");

		int minValue = matrix[0][0];
		for (int[] matrix1 : matrix)
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
	 *  
	 */
	public static int minValue(Collection<Integer> collection)
	{
		if (collection.isEmpty()) throw new NoSuchElementException("Empty collection");
		
		int minValue = Integer.MAX_VALUE;
		for (int value : collection)
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
	 *  
	 */
	public static <A> int minValue(Map<A, Integer> map)
	{
		return minValue(map.values());
	}
	
	/**
	 * Returns the position(s) in which the minimum value is found.
	 *
	 * @param array Input array
	 * @param searchType Indicates whether the first, the last, or all minimum positions are returned
	 * @return Position(s) in which the minimum value is found
	 *  
	 */
	public static int[] minIndexes(int[] array, Constants.SearchType searchType)
	{
		if (array.length == 0) return new int[0];

		List<Integer> candidateIndexes = new ArrayList<Integer>();
		candidateIndexes.add(0);

		int minValue = array[0];
		for (int i = 1; i < array.length; i++)
		{
			if (array[i] < minValue)
			{
				minValue = array[i];
				candidateIndexes.clear();
				candidateIndexes.add(i);
			}
			else if (array[i] == minValue)
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

		return toArray(candidateIndexes);
	}

	/**
	 * Returns the element-wise sum of two maps.
	 *
	 * @param <A> Key type
	 * @param map1 Input map 1
	 * @param map2 Input map 2
	 * @return A new map with the element-wise sum
	 *  
	 */
	public static <A> Map<A, Double> sum(Map<A, Integer> map1, Map<A, Integer> map2)
	{
		Map<A, Double> out = new LinkedHashMap<A, Double>();

		for (Map.Entry<A, Integer> entry : map1.entrySet())
		{
			A key = entry.getKey();
			out.put(key, (double) entry.getValue() + map2.get(key));
		}

		return out;
	}
	
	/**
	 * Multiplies all elements in an array by a scalar.
	 *
	 * @param array Input array
	 * @param value Scalar
	 * @return A new array containing the input elements multiplied by the scalar
	 *  
	 */
	public static double[] mult(int[] array, double value)
	{
		double[] out = new double[array.length];
		for (int i = 0; i < out.length; i++)
			out[i] = array[i] * value;

		return out;
	}

	/**
	 * Multiplies all elements in an array by a scalar.
	 *
	 * @param <A> Key type
	 * @param map Input map
	 * @param value Scalar
	 * @return A new map containing the input elements multiplied by the scalar
	 *  
	 */
	public static <A> Map<A, Double> mult(Map<A, Integer> map, double value)
	{
		Map<A, Double> out = new LinkedHashMap<A, Double>();
		for(Map.Entry<A, Integer> entry : map.entrySet())
			out.put(entry.getKey(), entry.getValue() * value);
		
		return out;
	}
	
	/**
	 * Multiplies all elements in a matrix by a scalar.
	 *
	 * @param matrix Input array
	 * @param value Scalar
	 * @return A new matrix containing the input elements multiplied by the scalar
	 *  
	 */
	public static double[][] mult(int[][] matrix, double value)
	{
		double[][] out = new double[matrix.length][];
		for (int i = 0; i < out.length; i++)
			out[i] = IntUtils.mult(matrix[i], value);

		return out;
	}

	/**
	 * Multiplies two arrays element-to-element.
	 *
	 * @param array1 Input array 1
	 * @param array2 Input array 2
	 * @return The element-wise multiplication of the input arrays
	 *  
	 */
	public static double[] mult(int[] array1, int[] array2)
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
	 *  
	 */
	public static <A> Map<A, Double> mult(Map<A, Integer> map1, Map<A, Integer> map2)
	{
		Map<A, Double> out = new LinkedHashMap<A, Double>();

		for (Map.Entry<A, Integer> entry : map1.entrySet())
		{
			A key = entry.getKey();
			out.put(key, (double) entry.getValue() * map2.get(key));
		}

		return out;
	}
	
	/**
	 * Returns an array filled with ones.
	 *
	 * @param N Number of elements
	 * @return An all-one array of length {@code N}
	 *  
	 */
	public static int[] ones(int N)
	{
		return constantArray(N, 1);
	}

	/**
	 * Returns a map filled with ones.
	 *
	 * @param <A> Key type
	 * @param identifiers Set of map keys
	 * @return An all-one map
	 *  
	 */
	public static <A> Map<A, Integer> ones(Set<A> identifiers)
	{
		return constantMap(identifiers, 1);
	}
	
	/**
	 * Reverses the order of the elements of the input array (it will be overriden).
	 *
	 * @param array Input array
	 *  
	 */
	public static void reverse(int[] array)
	{
		for (int i = 0; i < array.length / 2; ++i)
		{
			int temp = array[i];
			array[i] = array[array.length - i - 1];
			array[array.length - i - 1] = temp;
		}
	}

	/**
	 * Scalar product of two vectors.
	 *
	 * @param array1 Input array 1
	 * @param array2 Input array 2
	 * @return The scalar product of the input arrays
	 *  
	 */
	public static double scalarProduct(int[] array1, int[] array2)
	{
		return DoubleUtils.sum(mult(array1, array2));
	}

	/**
	 * Scalar product of two maps.
	 *
	 * @param <A> Key type
	 * @param map1 Input map 1
	 * @param map2 Input map 2
	 * @return The scalar product of the input maps
	 *  
	 */
	public static <A> double scalarProduct(Map<A, Integer> map1, Map<A, Integer> map2)
	{
		double out = 0;
		for(Map.Entry<A, Integer> entry : map1.entrySet())
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
	 *  
	 */
	public static int[] select(int[] array, int[] indexes)
	{
		int[] out = new int[indexes.length];

		for (int i = 0; i < indexes.length; i++)
			out[i] = array[indexes[i]];

		return out;
	}
	
	/**
	 * Returns a column of a bidimensional input array.
	 *
	 * @param array Input array
	 * @param column Column
	 * @return Column of an array
	 *  
	 */
	public static int[] selectColumn(int[][] array, int column)
	{
		int[] output = new int[array.length];
		for (int i = 0; i < output.length; i++)
			output[i] = array[i][column];

		return output;
	}

	/**
	 * Returns a row of a bidimensional input array.
	 *
	 * @param array Input array
	 * @param row Row
	 * @return Row of an array
	 *  
	 */
	public static int[] selectRow(int[][] array, int row)
	{
		return array[row];
	}

	/**
	 * Returns a list of the comma-separated input values.
	 *
	 * @param values Comma-separated input values
	 * @return List of values (iteration order equal to insertion order)
	 *  
	 */
	public static Set<Integer> setOf(int... values)
	{
		return new LinkedHashSet<Integer>(toList(values));
	}

	/**
	 * Returns the elements contained in the first array, but not any of the others.
	 *
	 * @param arrays Input arrays
	 * @return Difference set
	 *  
	 */
	public static int[] setdiff(int[]... arrays)
	{
		Set<Integer> differenceSet = new LinkedHashSet<Integer>();
		for (int i = 0; i < arrays[0].length; i++)
			differenceSet.add(arrays[0][i]);

		for (int i = 1; i < arrays.length; i++)
		{
			List<Integer> aux = toList(arrays[i]);
			differenceSet.removeAll(aux);
		}

		int[] differenceArray = toArray(differenceSet);

		return differenceArray;
	}

	/**
	 * Sorts the input array (it will be overriden).
	 *
	 * @param array Input array
	 * @param orderingType Ascending or descending
	 *  
	 */
	public static void sort(int[] array, Constants.OrderingType orderingType)
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
	 *  
	 */
	public static int[] sortIndexes(int[] array, Constants.OrderingType orderingType)
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
	 *  
	 */
	public static double std(int[] array)
	{
		if (array.length == 0) return 0;
		
		double M = 0.0;
		double S = 0.0;
		int k = 1;
		for (int value : array) 
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
	 *  
	 */
	public static double std(Collection<Integer> collection)
	{
		if (collection.isEmpty()) return 0;
		
		double M = 0.0;
		double S = 0.0;
		int k = 1;
		for (int value : collection) 
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
	 *  
	 */
	public static <A> double std(Map<A, Integer> map)
	{
		return std(map.values());
	}
	
	/**
	 * Returns the element-wise substraction of two arrays.
	 *
	 * @param array1 Input array 1
	 * @param array2 Input array 2
	 * @return A new array with the element-wise subtraction
	 *  
	 */
	public static int[] substract(int[] array1, int[] array2)
	{
		int[] out = new int[array1.length];
		for (int i = 0; i < out.length; i++)
			out[i] = array1[i] - array2[i];

		return out;
	}
	/**
	 * Returns the sum of all elements in the input array.
	 *
	 * @param array Input array
	 * @return Sum of all array elements
	 *  
	 */
	public static double sum(int[] array)
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
	 *  
	 */
	public static double sum(int[][] matrix)
	{
		double out = 0;
		for (int[] matrix1 : matrix)
			out += sum(matrix1);

		return out;
	}
	
	/**
	 * Returns the sum of all elements in the input collection.
	 *
	 * @param collection Input collection
	 * @return Sum of all collection values
	 *  
	 */
	public static double sum(Collection<Integer> collection)
	{
		double out = 0;
		for(int value : collection)
			out += value;

		return out;
	}
	
	/**
	 * Returns the sum of all elements in the input collection.
	 *
	 * @param <A> Key type
	 * @param map Input map
	 * @return Sum of all map values
	 *  
	 */
	public static <A> double sum(Map<A, Integer> map)
	{
		return sum(map.values());
	}

	/**
	 * Returns the element-wise sum of two arrays.
	 *
	 * @param array1 Input array 1
	 * @param array2 Input array 2
	 * @return A new array with the element-wise sum
	 *  
	 */
	public static double[] sum(int[] array1, int[] array2)
	{
		double[] out = new double[array1.length];
		for (int i = 0; i < out.length; i++)
			out[i] = array1[i] + array2[i];

		return out;
	}
	
	/**
	 * Converts a collection ({@code List}, {@code Set}...) of {@code Integer} objects to an {@code int} array.
	 *
	 * @param list Input list
	 * @return {@code int} array
	 *  
	 */
	public static int[] toArray(Collection<Integer> list)
	{
		return asPrimitiveArray(list.toArray(new Integer[list.size()]));
	}

	/**
	 * Converts an {@code int} array to a {@code double} array
	 *
	 * @param array Input array
	 * @return {@code double} array
	 *  
	 */
	public static double[] toDoubleArray(int[] array)
	{
		double[] out = new double[array.length];
		for (int i = 0; i < out.length; i++)
			out[i] = array[i];

		return out;
	}

	/**
	 * Converts from a {@code int} array to a list.
	 *
	 * @param array Input array
	 * @return A list of {@code Integer} objects
	 *  
	 */
	public static List<Integer> toList(int[] array)
	{
		List<Integer> list = new ArrayList<Integer>();
		for (int i = 0; i < array.length; i++)
			list.add(array[i]);

		return list;
	}

	/**
	 * Converts an {@code int} array to a {@code long} array
	 *
	 * @param array Input array
	 * @return {@code long} array
	 *  
	 */
	public static long[] toLongArray(int[] array)
	{
		long[] out = new long[array.length];
		for (int i = 0; i < out.length; i++)
			out[i] = array[i];

		return out;
	}

	/**
	 * Returns an array with all elements in input arrays (no repetitions). There is no order guarantee.
	 *
	 * @param arrays Input arrays
	 * @return A new array with all elements in input arrays
	 *  
	 */
	public static int[] union(int[]... arrays)
	{
		Set<Integer> unionSet = new LinkedHashSet<Integer>();
		for (int[] array : arrays)
			for (int j = 0; j < array.length; j++)
				unionSet.add(array[j]);

		return toArray(unionSet);
	}
	
	/**
	 * Returns the same values of the input {@code array} but with no repetitions. There is no order guarantee.
	 *
	 * @param array Input array
	 * @return Same values as in {@code array} but with no repetitions
	 *  
	 */
	public static int[] unique(int[] array)
	{
		Set<Integer> uniqueSet = new LinkedHashSet<Integer>();
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
	 *  
	 */
	public static <A> Map<A, Integer> zeros(Set<A> identifiers)
	{
		return constantMap(identifiers, 0);
	}

	/**
	 * Returns an array filled with zeros.
	 *
	 * @param N Number of elements
	 * @return An all-zero array of length {@code N}
	 *  
	 */
	public static int[] zeros(int N)
	{
		return constantArray(N, 0);
	}

	private static class ArrayIndexComparator implements Comparator<Integer>, Serializable
	{
		private static final long serialVersionUID = 1L;
		
		private final Constants.OrderingType orderingType;
		private final int[] array;

		public ArrayIndexComparator(int[] array, Constants.OrderingType orderingType)
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
					return Integer.compare(array[index1], array[index2]);
					
				case DESCENDING:
					return Integer.compare(array[index2], array[index1]);
					
				default:
					throw new RuntimeException("Bad");
			}
		}
	}
}
