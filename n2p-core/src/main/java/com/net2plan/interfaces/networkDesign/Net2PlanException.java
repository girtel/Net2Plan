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
