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

import com.net2plan.utils.StringUtils;
import org.codehaus.stax2.XMLStreamWriter2;

import javax.xml.stream.XMLStreamException;

/**
 * Class with utilities for XML files.
 * 
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.3.0
 */
public class XMLUtils
{
	private final static String TAB = String.format("\t");
	private final static String NEW_LINE = StringUtils.getLineSeparator();
	
	/**
	 * Applies some indentation before the next output to the XML writer.
	 *
	 * @param writer Reference to the XML writer
	 * @param level Indentation level
	 * @throws XMLStreamException Exception for unexpected XML processing errors.
	 * @since 0.3.0
	 */
	public static void indent(XMLStreamWriter2 writer, int level) throws XMLStreamException
	{
		writer.writeRaw(NEW_LINE);
		for(int i = 0; i < level; i++)
			writer.writeRaw(TAB);
	}
}
