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

import java.util.*;
import java.util.Map.Entry;

/**
 * <p>Provides extra functionality for {@code boolean} data.</p>
 * 
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public class BooleanUtils
{
	private BooleanUtils() { }

	/**
	 * Generates a {@code boolean[]} from comma-separated values.
	 *
	 * @param values Comma-separated {@code boolean} values
	 * @return {@code boolean[]}
	 */
	public static boolean[] arrayOf(boolean... values)
	{
		return values;
	}
	
	/**
	 * Converts from {@code boolean[]} to {@code Boolean[]}.
	 *
	 * @param array {@code boolean[]}
	 * @return Equivalent {@code Boolean[]}
	 */
	public static Boolean[] asObjectArray(boolean[] array)
	{
		Boolean[] objectArray = new Boolean[array.length];
		for (int i = 0; i < array.length; i++) objectArray[i] = array[i];

		return objectArray;
	}

	/**
	 * Converts from {@code Boolean[]} to {@code boolean[]}.
	 *
	 * @param array {@code Boolean[]}
	 * @return Equivalent {@code boolean[]}
	 */
	public static boolean[] asPrimitiveArray(Boolean[] array)
	{
		boolean[] primitiveArray = new boolean[array.length];
		for (int i = 0; i < array.length; i++) primitiveArray[i] = array[i];

		return primitiveArray;
	}

	/**
	 * Returns a map filled with a given value.
	 *
	 * @param <A> Key type
	 * @param identifiers Set of map keys
	 * @param value Value for all elements
	 * @return A map with the given value in each entry
	 */
	public static <A> Map<A, Boolean> constantMap(Set<A> identifiers, boolean value)
	{
		Map<A, Boolean> out = new LinkedHashMap<A, Boolean>();
		for(A identifier : identifiers)
			out.put(identifier, value);
		
		return out;
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
	public static <A extends Number> Map<A, Boolean> convertArray2Map(Set<A> identifiers, boolean[] array)
	{
		Map<A, Integer> id2LinearIndex = CollectionUtils.convertId2LinearIndexMap(identifiers);
		Map<A, Boolean> out = new LinkedHashMap<A, Boolean>();
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
	public static <A extends Number, B extends Number> Map<Pair<A, B>, Boolean> convertTable2Map(Set<A> rowIdentifiers, Set<B> columnIdentifiers, boolean[][] table)
	{
		Map<A, Integer> rowId2LinearIndex = CollectionUtils.convertId2LinearIndexMap(rowIdentifiers);
		Map<B, Integer> columnId2LinearIndex = CollectionUtils.convertId2LinearIndexMap(columnIdentifiers);
		Map<Pair<A, B>, Boolean> out = new LinkedHashMap<Pair<A, B>, Boolean>();
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
	 * Returns the position(s) where a given value can be found into an array.
	 *
	 * @param array Input array
	 * @param value Value to be searched for
	 * @param searchType Indicates whether the first, the last, or all occurrences are returned
	 * @return Position(s) in which the given value can be found
	 */
	public static int[] find(boolean[] array, boolean value, Constants.SearchType searchType)
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
	 * Returns a set of selected elements from an input array. It is not backed
	 * in the input array, thus changes in this array are not reflected in the
	 * input array.
	 *
	 * @param array Input array
	 * @param indexes Position of elements to be selected.
	 * @return Set of elements in the order given by {@code indexes}
	 */
	public static boolean[] select(boolean[] array, int[] indexes)
	{
		boolean[] out = new boolean[indexes.length];
		for (int i = 0; i < indexes.length; i++) out[i] = array[indexes[i]];

		return out;
	}

	/**
	 * Converts a collection ({@code List}, {@code Set}...) of {@code Boolean} objects to a {@code boolean[]}.
	 *
	 * @param list Input list
	 * @return {@code boolean[]}
	 */
	public static boolean[] toArray(Collection<Boolean> list)
	{
		return asPrimitiveArray(list.toArray(new Boolean[list.size()]));
	}

	/**
	 * Converts from a {@code boolean[]} to a list.
	 *
	 * @param array Input array
	 * @return A list of {@code Boolean} objects
	 */
	public static List<Boolean> toList(boolean[] array)
	{
		List<Boolean> list = new ArrayList<Boolean>();
		for (int i = 0; i < array.length; i++) list.add(array[i]);

		return list;
	}
}
