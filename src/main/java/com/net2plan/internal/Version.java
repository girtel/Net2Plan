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

/**
 * Class enclosing the current VERSION of the kernel.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public class Version
{
	private final static String VERSION = "0.4.0";
	private final static String FILE_FORMAT_VERSION = "4";
	
	/**
	 * Returns the current {@code .n2p} file format VERSION.
	 *
	 * @return {@code String} representation of the {@code .n2p} file format VERSION
	 * @since 0.3.0
	 */
	public static String getFileFormatVersion()
	{
		return FILE_FORMAT_VERSION;
	}

	/**
	 * Returns a {@code String} representation of the object.
	 *
	 * @return {@code String} representation of the object
	 * @since 0.2.3
	 */
	public static String getVersion()
	{
		return VERSION;
	}

	/**
	 * Returns a {@code String} representation of the object.
	 *
	 * @return {@code String} representation of the object
	 * @since 0.2.0
	 */
	@Override
	public String toString()
	{
		return getVersion();
	}
}
