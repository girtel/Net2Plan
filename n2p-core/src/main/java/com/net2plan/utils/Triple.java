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

import java.util.Objects;

/**
 * <p>A tuple consisting of three elements. There is no restriction on the type of the objects that may be stored.</p>
 *
 * <p>Example:</p>
 * {@code Map&lt;String, Object&gt; map = new LinkedHashMap&lt;&gt;();}
 * <br>
 * {@code map.put("parameter", "value");}
 * <br>
 * {@code Integer number = new Integer(3);}
 * <br>
 * {@code String string = "Test";}
 * <br>
 * <br>
 * {@code Triple&lt;Map&lt;String, Object&gt;, Integer, String&gt; data = new Triple&lt;&gt;(map, number, string);}
 * <br>
 * <br>
 * ...
 * <br>
 * <br>
 * {@code Map&lt;String, Object&gt; myMap = data.getFirst();}
 * <br>
 * {@code Integer myNumber = data.getSecond();}
 * <br>
 * {@code String myString = data.getThird();}
 *
 * @param <A> Class type for the first element
 * @param <B> Class type for the second element
 * @param <C> Class type for the third element
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * 
 * @see com.net2plan.utils.Pair
 * @see com.net2plan.utils.Quadruple
 * @see com.net2plan.utils.Quintuple
 */
public class Triple<A, B, C>
{
	private A a;
	private B b;
	private C c;
	private final boolean isModifiable;

	/**
	 * Default constructor.
	 *
	 * @param a The first element, may be {@code null}
	 * @param b The second element, may be {@code null}
	 * @param c The third element, may be {@code null}
	 * @param isModifiable Indicates whether or not the elements can be changed after initialization
	 * 
	 */
	public Triple(A a, B b, C c, boolean isModifiable)
	{
		this.a = a;
		this.b = b;
		this.c = c;
		this.isModifiable = isModifiable;
	}

	/**
	 * Indicates whether some other object is "equal to" this one.
	 *
	 * @param o Reference object with which to compare
	 * @return {@code true} if this object is the same as the {@code o} argument; {@code false} otherwise
	 * 
	 */
	@Override
	public boolean equals(Object o)
	{
		if (o == null) return false;
		if (o == this) return true;
		if (!(o instanceof Triple)) return false;

		Triple p = (Triple) o;
		if (a != null)
		{
			if (p.getFirst() == null) return false;
			if (!a.equals(p.getFirst())) return false;
		} else { if (p.getFirst() != null) return false; }
		if (b != null)
		{
			if (p.getSecond() == null) return false;
			if (!b.equals(p.getSecond())) return false;
		} else { if (p.getSecond() != null) return false; }
		if (c != null)
		{
			if (p.getThird() == null) return false;
			if (!c.equals(p.getThird())) return false;
		} else { if (p.getThird() != null) return false; }
		return true;
	}

	/**
	 * Returns a hash code value for the object. This method is supported for the benefit of hash tables such as those provided by {@code HashMap}.
	 *
	 * @return Hash code value for this object
	 * 
	 */
	@Override
	public int hashCode()
	{
		int hash = 3;
		hash = 59 * hash + Objects.hashCode(getFirst());
		hash = 59 * hash + Objects.hashCode(getSecond());
		hash = 59 * hash + Objects.hashCode(getThird());
		return hash;
	}

	/**
	 * Returns a {@code String} representation of this triple using the format (first, second, third).
	 *
	 * @return A {@code String} representation of this triple
	 * 
	 */
	@Override
	public String toString()
	{
		return "(" + a + ", " + b + ", " + c + ")";
	}

	private void checkIsModifiable()
	{
		if (!isModifiable()) throw new Net2PlanException("Pair is not modifiable");
	}

	/**
	 * This factory allows a triple to be created using inference to obtain the generic types.
	 *
	 * @param <A> Class type for the first element
	 * @param <B> Class type for the second element
	 * @param <C> Class type for the third element
	 * @param a The first element, may be {@code null}
	 * @param b The second element, may be {@code null}
	 * @param c The third element, may be {@code null}
	 * @return A triple formed from three parameters
	 * 
	 */
	public static <A, B, C> Triple<A, B, C> of(A a, B b, C c)
	{
		return new Triple<A, B, C>(a, b, c, true);
	}

	/**
	 * Returns the first element from this triple.
	 *
	 * @return The first element from this triple
	 * 
	 */
	public A getFirst()
	{
		return a;
	}

	/**
	 * Returns the second element from this triple.
	 *
	 * @return The second element from this triple
	 * 
	 */
	public B getSecond()
	{
		return b;
	}

	/**
	 * Returns the third element from this triple.
	 *
	 * @return The third element from this triple
	 * 
	 */
	public C getThird()
	{
		return c;
	}

	/**
	 * Indicates whether or not elements from the pair can be changed after initialization.
	 *
	 * @return {@code true} if the element can be changed. Otherwise, {@code false}
	 * 
	 */
	public boolean isModifiable()
	{
		return isModifiable;
	}
	
	/**
	 * Sets the first element from this triple.
	 *
	 * @param a The first element, may be {@code null}
	 * 
	 */
	public void setFirst(A a)
	{
		checkIsModifiable();
		this.a = a;
	}

	/**
	 * Sets the second element from this triple.
	 *
	 * @param b The second element, may be {@code null}
	 * 
	 */
	public void setSecond(B b)
	{
		checkIsModifiable();
		this.b = b;
	}

	/**
	 * Sets the third element from this triple.
	 *
	 * @param c The third element, may be {@code null}
	 * 
	 */
	public void setThird(C c)
	{
		checkIsModifiable();
		this.c = c;
	}

	/**
	 * This factory allows an unmodifiable triple to be created using inference to obtain the generic types.
	 *
	 * @param <A> Class type for the first element
	 * @param <B> Class type for the second element
	 * @param <C> Class type for the third element
	 * @param a The first element, may be {@code null}
	 * @param b The second element, may be {@code null}
	 * @param c The third element, may be {@code null}
	 * @return An unmodifiable triple formed from three parameters
	 * 
	 */
	public static <A, B, C> Triple<A, B, C> unmodifiableOf(A a, B b, C c)
	{
		return new Triple<A, B, C>(a, b, c, false);
	}
}
