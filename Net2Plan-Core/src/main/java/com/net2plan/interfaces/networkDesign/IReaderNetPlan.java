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

import org.codehaus.stax2.XMLStreamReader2;

import javax.xml.stream.XMLStreamException;

interface IReaderNetPlan
{
	public void create(NetPlan netPlan, XMLStreamReader2 xmlStreamReader) throws XMLStreamException;
}



