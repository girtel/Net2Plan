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



 




package com.net2plan.internal.plugins;

import com.net2plan.internal.IExternal;
import java.util.Map;

/**
 * Interface to be implemented by any external plugin.
 * 
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.3.0
 */
public interface Plugin extends IExternal
{
	/**
	 * Returns the current value for the plugin options.
	 * 
	 * @return Current options setting
	 * @since 0.3.1
	 */
	public Map<String, String> getCurrentOptions();
	
	/**
	 * Returns the plugin name.
	 * 
	 * @return Plugin name
	 * @since 0.3.0
	 */
	public String getName();
	
	/**
	 * Returns the plugin priority (the higher, the first). Ties are broken using 
	 * natural ordering by {@link #getName() getName()} method.
	 * 
	 * @return Plugin priority
	 * @since 0.3.1
	 */
	public int getPriority();
}
