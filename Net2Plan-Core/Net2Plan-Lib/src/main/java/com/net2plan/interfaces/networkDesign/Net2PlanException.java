/*******************************************************************************
 * Copyright (c) 2016 Pablo Pavon-Marino.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * Contributors:
 *     Pablo Pavon-Marino - Jose-Luis Izquierdo-Zaragoza, up to version 0.3.1
 *     Pablo Pavon-Marino - from version 0.4.0 onwards
 ******************************************************************************/

package com.net2plan.interfaces.networkDesign;

/**
 * {@code Net2PlanException} is the superclass of those exceptions that can be
 * thrown during the normal operation of algorithms or reports.
 *
 * It is devoted to situations like wrong input parameters. Contrary to other
 * exceptions, it has a special treatment by Net2Plan kernel: a popup with the
 * message will be thrown, instead of redirecting the message and stack trace
 * to the error console.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 * @see java.lang.RuntimeException
 */
public class Net2PlanException extends RuntimeException
{
	private static final long serialVersionUID = 1L;

	/**
	 * Default constructor.
	 *
	 * @since 0.3.0
	 */
	public Net2PlanException()
	{
		super();
	}
	
	/**
	 * Constructs a new {@code Net2PlanException} exception with the specified detail message.
	 *
	 * @param message Message to be retrieved by the {@link #getMessage()} method.
	 * @since 0.2.0
	 */
	public Net2PlanException(String message)
	{
		super(message);
	}
}
