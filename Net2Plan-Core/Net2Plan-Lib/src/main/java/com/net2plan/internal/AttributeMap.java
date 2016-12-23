/*******************************************************************************
 * Copyright (c) 2015 Pablo Pavon Mariño.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 * Contributors:
 *     Pablo Pavon Mariño - initial API and implementation
 ******************************************************************************/



 




package com.net2plan.internal;

import java.util.Map;
import java.util.TreeMap;

/**
 * Extends {@code TreeMap} to forbid 'null or empty' keys.
 * 
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.3.0
 */
public class AttributeMap extends TreeMap<String, String>
{
	private static final long serialVersionUID = 1L;
	
	/**
	 * Default constructor.
	 * 
	 * @since 0.3.0
	 */
	public AttributeMap()
	{
		super();
	}

	/**
	 * Constructor that copies the value set of the input map.
	 * 
	 * @param m Map to be copied (if null, it will be initialized as empty)
	 * @since 0.3.0
	 */
	public AttributeMap(Map<String, String> m)
	{
		this();

		if (m == null) return;
		for (Map.Entry<String, String> entry : m.entrySet()) put(new String (entry.getKey()), new String (entry.getValue()));
	}

	@Override
	public String put(String key, String value)
	{
		if (key == null || key.isEmpty()) throw new RuntimeException("Key cannot be null or empty");
		return super.put(key, value);
	}
	
	@Override
	public void putAll(Map<? extends String, ? extends String> m)
	{
		if (m == null) { clear(); return; }
		super.putAll(m);
	}
}
