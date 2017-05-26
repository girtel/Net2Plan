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
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import org.apache.commons.collections15.Transformer;

import java.util.*;
import java.util.Map.Entry;

/**
 * <p>Provides extra functionality for the Java Collections Framework. For Java primitives check 
 * the corresponding {@code xxxUtils} class.</p>
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.3.0
 */
@SuppressWarnings("unchecked")
public class CollectionUtils
{
	private CollectionUtils() { }

	/**
	 * Checks if an input collection contains a given value.
	 *
	 * @param <A> Key type
	 * @param collection Input collection
	 * @param value Value to search
	 * @return {@code true} if {@code value} is present in {@code collection}, and {@code false} otherwise. If {@code collection} is empty, it will return {@code false}
	 */
	public static <A> boolean contains(Collection<A> collection, A value)
	{
		if (collection.isEmpty()) return false;

		for (A item : collection)
			if (item == value)
				return true;

		return false;
	}
	
	/**
	 * Checks whether all elements of a collection are present in another one.
	 *
	 * @param <A> Key type
	 * @param container Container collection
	 * @param collection Collection with elements to be checked
	 * @return {@code true} if all elements in {@code collection} are present in {@code container}, and {@code false} otherwise. If {@code container} is empty, it will return {@code false}
	 */
	public static <A> boolean containsAll(Collection<A> container, Collection<A> collection)
	{
		if (container.isEmpty()) return false;

		Set<A> set = new LinkedHashSet<A>(container);
		set.removeAll(collection);

		return set.isEmpty();
	}
	
	/**
	 * Checks whether any element of a collection is present in another one.
	 *
	 * @param <A> Key type
	 * @param collection1 Container collection
	 * @param collection2 Collection with elements to be checked
	 * @return {@code true} if any element in {@code collection} is present in {@code container}, and {@code false} otherwise. If {@code container} is empty, it will return {@code false}
	 */
	public static <A> boolean containsAny(Collection<A> collection1, Collection<A> collection2)
	{
		return !Collections.disjoint(collection1, collection2);
	}
	
	/**
	 * <p>Returns a map containing the association of each element with its corresponding
	 * position within the set (in linear order). First element will be associate to index 0,
	 * second element to index 1, and so on.</p>
	 * 
	 * <p><b>Important</b>: Since this method is widely used within the tool to deal with 
	 * conversion between node/link/whatever identifier to its corresponding linear index 
	 * (first element will be item 0, second element will be item 1, and so on), it is 
	 * required that in iterator order the elements follow an asceding order.</p>
	 *
	 * @param <A> Key type
	 * @param elements Set of elements in ascending iterator order (null values are not allowed)
	 * @return Element to linear index mapping
	 */
	public static <A extends Number> Map<A, Integer> convertId2LinearIndexMap(Set<A> elements)
	{
		Map<A, Integer> out = new LinkedHashMap<A, Integer>();
		int i = 0;
		Double previousValue = null;
		for (A value : elements)
		{
			if (value == null) throw new Net2PlanException("Bad - Null values not allowed");
			
			double currentValue = value.doubleValue();
			if (previousValue != null && currentValue < previousValue) throw new Net2PlanException("Bad - Values must be given in ascending order");

			out.put(value, i++);
			previousValue = currentValue;
		}
		
		return out;
	}
	
	/**
	 * Returns a map where each entry represents the number of times that the
	 * corresponding key appears in the input collection.
	 *
	 * @param <A> Key type
	 * @param collection Input collection
	 * @return Map where the key is an item appearing in the input collection, and the value is the number of times that appears
	 */
	public static <A> Map<A, Integer> count(Collection<A> collection)
	{
		Map<A, Integer> out = new LinkedHashMap<A, Integer>();
		for (A value : collection)
			out.put(value, out.containsKey(value) ? out.get(value) + 1 : 1);

		return out;
	}
	
	/**
	 * Returns the key(s) where a given value can be found into a map.
	 * 
	 * @param <A> Key type
	 * @param <B> Value type
	 * @param map Input map
	 * @param value Value to be searched for
	 * @param searchType Indicates whether the first, the last, or all occurrences are returned
	 * @return Key(s) in which the given value can be found (in iteration order)
	 */
	public static <A, B> List<A> find(Map<A, B> map, B value, Constants.SearchType searchType)
	{
		List<A> out = new LinkedList<A>();
		for(Entry<A, B> entry : map.entrySet())
		{
			if ((value == null && entry.getValue() == null) || (value != null && value.equals(entry.getValue())))
			{
				switch(searchType)
				{
					case FIRST:
						out.add(entry.getKey());
						return out;
						
					case ALL:
						out.add(entry.getKey());
						break;
						
					case LAST:
						out.clear();
						out.add(entry.getKey());
						break;
						
					default:
						throw new RuntimeException("Bad");
				}
			}
		}
		
		return out;
	}

	/**
	 * Obtains the equivalent {@code Transformer} of a {@code Map}.
	 *
	 * @param <A> Key type
	 * @param <B> Value type
	 * @param map Input map
	 * @return {@code Transformer}
	 */
	public static <A, B> Transformer<A, B> getMapTransformer(final Map<A, B> map)
	{
		return new Transformer<A, B>()
		{
			@Override
			public B transform(A key)
			{
				return map.get(key);
			}
		};
	}

	/**
	 * Returns the intersection set of a series of input collections. There is no order guarantee.
	 *
	 * @param <A> Key type
	 * @param collections Series of input collections
	 * @return Intersection set of input collections
	 */
	public static <A> Set<A> intersect(Collection<A>... collections)
	{
		if (collections.length == 0) return new LinkedHashSet<A>();

		Set<A> intersectionSet = new LinkedHashSet<A>();
		intersectionSet.addAll(collections[0]);

		for (int i = 1; i < collections.length; i++)
		{
			intersectionSet.retainAll(collections[i]);
		}

		return intersectionSet;
	}
	
	/**
	 * Joins the elements in an input collection using a given separator.
	 *
	 * @param <A> Key type
	 * @param collection Input collection
	 * @param separator Separator
	 * @return {@code String} representation of the input {@code collection}
	 */
	public static <A> String join(Collection<A> collection, String separator)
	{
		if (collection.isEmpty()) return "";

		Iterator<A> it = collection.iterator();
		StringBuilder out = new StringBuilder();
		out.append(it.next());
		
		while(it.hasNext())
			out.append(separator).append(it.next());

		return out.toString();
	}
	
	/**
	 * Returns a list of the comma-separated input values.
	 *
	 * @param <A> Value type
	 * @param values Comma-separated input values
	 * @return List of values (iteration order equal to insertion order)
	 */
	public static <A> List<A> listOf(A... values)
	{
		return new LinkedList<A>(Arrays.asList(values));
	}

	/**
	 * Returns a list of the values of the selected elements from an input map. 
	 * It is not backed in the input map, thus changes in this list are not 
	 * reflected in the input map.
	 *
	 * @param <A> Key type
	 * @param <B> Value type
	 * @param map Input map
	 * @param keys Keys of the elements to be selected
	 * @return List of the values associated to the input indexes (in iteration order)
	 */
	public static <A, B> List<B> select(Map<A, B> map, Collection<A> keys)
	{
		List<B> out = new LinkedList<B>();
		for (A key : keys)
			if (map.containsKey(key))
				out.add(map.get(key));

		return out;
	}

	/**
	 * Returns a map of selected elements from the input map. It is not backed
	 * in the input map, thus changes in the returned one are not reflected in the
	 * input map.
	 *
	 * @param <A> Key type
	 * @param <B> Value type
	 * @param map Input map
	 * @param keys Keys of the elements to be selected.
	 * @return Map containing the entries corresponding to the keys in the input set (in iteration order)
	 */
	public static <A, B> Map<A, B> selectEntries(Map<A, B> map, Set<A> keys)
	{
		Map<A, B> out = new LinkedHashMap<A, B>();
		for (A key : keys)
			if (map.containsKey(key))
				out.put(key, map.get(key));

		return out;
	}
	
	/**
	 * Returns a set of the comma-separated input values.
	 *
	 * @param <A> Value type
	 * @param values Comma-separated input values
	 * @return Set of values (iteration order equal to insertion order)
	 */
	public static <A> Set<A> setOf(A... values)
	{
		return new LinkedHashSet<A>(Arrays.asList(values));
	}

	/**
	 * Returns a {@code SortedMap} copy of the input map according to the values.
	 *
	 * @param <A> Key type
	 * @param <B> Value type
	 * @param map Input map
	 * @param orderingType Ascending or descending
	 * @return {@code SortedMap}. For keys sharing the same value, entries will be ordered according to ascending order of keys.
	 */
	public static <A extends Comparable<A>, B extends Comparable<B>> SortedMap<A, B> sort(final Map<A, B> map, final Constants.OrderingType orderingType)
	{
		SortedMap<A, B> sortedMap = new TreeMap<A, B>(new Comparator<A>()
		{
			@Override
			public int compare(A key1, A key2)
			{
				B value1 = map.get(key1);
				B value2 = map.get(key2);
				int value = orderingType == Constants.OrderingType.ASCENDING ? value1.compareTo(value2) : value2.compareTo(value1);
				
				if (value == 0) return key1.compareTo(key2); /* Return 0 would merge entries, so order keys according to ascending order */
				else return value;
			}
		});
		
		sortedMap.putAll(map);
		return sortedMap;
	}

	/**
	 * Given a collection of keys and an array of values, returns a map.
	 * @param keys Input keys
	 * @param vals Input
	 * @param <K> Key type
	 * @return Map containing the entries corresponding to the input keys and values
	 */
	public static <K> Map<K,Double> toMap (Collection<K> keys , DoubleMatrix1D vals)
	{
		if (vals.size () != keys.size ()) throw new Net2PlanException ("Bad number of elements");
		Map<K,Double> res = new HashMap<K,Double> ();
		int counter = 0; for (K key : keys) res.put(key, vals.get(counter++)); 
		return res;

	}


}
