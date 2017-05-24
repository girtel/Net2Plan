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

import java.awt.geom.Point2D;

/**
 * Extended version of {@code Point2D.Double} that forbids position changes.
 * 
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.3.1
 */
public class UnmodifiablePoint2D extends Point2D.Double
{
	/**
	 * Default constructor.
	 *
	 * @param x X coordinate
	 * @param y Y coordinate
	 * @since 0.3.1
	 */
	public UnmodifiablePoint2D(double x, double y)
	{
		super(x, y);
	}
	
	@Override
	public void setLocation(double x, double y)
	{
		throw new UnsupportedOperationException("Unmodifiable Point2D");
	}
}
