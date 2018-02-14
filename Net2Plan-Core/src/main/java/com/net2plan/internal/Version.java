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





 




package com.net2plan.internal;

/**
 * Class enclosing the current VERSION of the kernel.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public class Version
{
	private final static String VERSION = "0.5.3";
	private final static String FILE_FORMAT_VERSION = "5";
	
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
