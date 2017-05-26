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
 * <p>A tuple consisting of four elements. There is no restriction on the type of the objects that may be stored.</p>
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
 * {@code List list = new ArrayList();}
 * <br>
 * <br>
 * {@code Quadruple&lt;Map&lt;String, Object&gt;, Integer, String, List&gt; data = new Quadruple&lt;&gt;(map, number, string, list);}
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
 * <br>
 * {@code List myList = data.getFourth();}
 *
 * @param <A> Class type for the first element
 * @param <B> Class type for the second element
 * @param <C> Class type for the third element
 * @param <D> Class type for the fourth element
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * 
 * @see com.net2plan.utils.Pair
 * @see com.net2plan.utils.Triple
 * @see com.net2plan.utils.Quintuple
 */
public class Quadruple<A, B, C, D>
{
	private A a;
	private B b;
	private C c;
	private D d;
	private final boolean isModifiable;

	/**
	 * Default constructor.
	 *
	 * @param a The first element, may be {@code null}
	 * @param b The second element, may be {@code null}
	 * @param c The third element, may be {@code null}
	 * @param d The fourth element, may be {@code null}
	 * @param isModifiable Indicates whether or not the elements can be changed after initialization
	 * 
	 */
	public Quadruple(A a, B b, C c, D d, boolean isModifiable)
	{
		this.a = a;
		this.b = b;
		this.c = c;
		this.d = d;
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
		if (o == null)
		{
			return false;
		}
		if (o == this)
		{
			return true;
		}
		if (!(o instanceof Quadruple))
		{
			return false;
		}

		Quadruple p = (Quadruple) o;
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
		if (d != null)
		{
			if (p.getFourth() == null) return false;
			if (!d.equals(p.getFourth())) return false;
		} else { if (p.getFourth() != null) return false; }
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
		hash = 97 * hash + Objects.hashCode(getFirst());
		hash = 97 * hash + Objects.hashCode(getSecond());
		hash = 97 * hash + Objects.hashCode(getThird());
		hash = 97 * hash + Objects.hashCode(getFourth());
		return hash;
	}

	/**
	 * Returns a {@code String} representation of this quadruple using the format (first, second, third, fourth).
	 *
	 * @return A {@code String} representation of this quadruple
	 * 
	 */
	@Override
	public String toString()
	{
		return "(" + a + ", " + b + ", " + c + ", " + d + ")";
	}

	private void checkIsModifiable()
	{
		if (!isModifiable()) throw new Net2PlanException("Pair is not modifiable");
	}

	/**
	 * This factory allows a quadruple to be created using inference to obtain the generic types.
	 *
	 * @param <A> Class type for the first element
	 * @param <B> Class type for the second element
	 * @param <C> Class type for the third element
	 * @param <D> Class type for the fourth element
	 * @param a The first element, may be {@code null}
	 * @param b The second element, may be {@code null}
	 * @param c The third element, may be {@code null}
	 * @param d The fourth element, may be {@code null}
	 * @return A quadruple formed from four parameters
	 * 
	 */
	public static <A, B, C, D> Quadruple<A, B, C, D> of(A a, B b, C c, D d)
	{
		return new Quadruple<A, B, C, D>(a, b, c, d, true);
	}
	
	/**
	 * Returns the first element from this quadruple.
	 *
	 * @return The first element from this quadruple
	 * 
	 */
	public A getFirst()
	{
		return a;
	}

	/**
	 * Returns the fourth element from this quadruple.
	 *
	 * @return The fourth element from this quadruple
	 * 
	 */
	public D getFourth()
	{
		return d;
	}

	/**
	 * Returns the second element from this quadruple.
	 *
	 * @return The second element from this quadruple
	 * 
	 */
	public B getSecond()
	{
		return b;
	}

	/**
	 * Returns the third element from this quadruple.
	 *
	 * @return The third element from this quadruple
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
	 * Sets the first element from this quadruple.
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
	 * Sets the fourth element from this quadruple.
	 *
	 * @param d The fourth element, may be {@code null}
	 * 
	 */
	public void setFourth(D d)
	{
		checkIsModifiable();
		this.d = d;
	}
	
	/**
	 * Sets the second element from this quadruple.
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
	 * Sets the third element from this quadruple.
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
	 * This factory allows an unmodifiable pair to be created using inference to obtain the generic types.
	 *
	 * @param <A> Class type for the first element
	 * @param <B> Class type for the second element
	 * @param <C> Class type for the third element
	 * @param <D> Class type for the fourth element
	 * @param a The first element, may be {@code null}
	 * @param b The second element, may be {@code null}
	 * @param c The third element, may be {@code null}
	 * @param d The fourth element, may be {@code null}
	 * @return An unmodifiable quadruple formed from two parameters
	 * 
	 */
	public static <A, B, C, D> Quadruple<A, B, C, D> unmodifiableOf(A a, B b, C c, D d)
	{
		return new Quadruple<A, B, C, D>(a, b, c, d, false);
	}
	
}
