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

import com.net2plan.internal.Constants.RunnableCodeType;
import com.net2plan.utils.StringUtils;
import com.net2plan.utils.Triple;
import org.apache.commons.cli.*;

import java.util.*;
import java.util.Map.Entry;

/**
 * Extends the {@code PosixParser} to modify the {@code processOption}
 * method.
 * 
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.2
 */
@SuppressWarnings({"deprecation", "unchecked"})
public class CommandLineParser extends PosixParser
{
	/**
	 * Modifies the original method to bypass (for convenience) options not
	 * defined in the {@code Options} object.
	 *
	 * @param arg The {@code String} value representing an {@code Option}
	 * @param iter The iterator over the flattened command line arguments
	 * @since 0.2.2
	 */

	protected void processOption(String arg, ListIterator iter)
	{
		boolean hasOption = this.getOptions().hasOption(arg);
		if (!hasOption) return;

		try { super.processOption(arg, iter); }
		catch(Throwable e) { throw new RuntimeException(e); }
	}

	/**
	 * Gets the current parameters from the user-specified ones, taking default
	 * values for unspecified parameters.
	 * 
	 * @param defaultParameters Default parameters (key, value, and description)
	 * @param inputParameters User parameters (key, value)
	 * @return Current parameters (key, value)
	 * @since 0.3.0
	 */
	public static Map<String, String> getParameters(List<Triple<String, String, String>> defaultParameters, Map inputParameters)
	{
		return getParameters(defaultParameters, inputParameters == null ? null : inputParameters.entrySet());
	}

	/**
	 * Gets the current parameters from the user-specified ones, taking default
	 * values for unspecified parameters.
	 * 
	 * @param defaultParameters Default parameters (key, value, and description)
	 * @param inputParameters User parameters (key, value)
	 * @return Current parameters (key, value)
	 * @since 0.2.2
	 */
	public static Map<String, String> getParameters(List<Triple<String, String, String>> defaultParameters, Properties inputParameters)
	{
		return getParameters(defaultParameters, inputParameters == null ? null : inputParameters.entrySet());
	}

	private static Map<String, String> getParameters(List<Triple<String, String, String>> defaultParameters, Set<Entry<Object, Object>> inputParameters)
	{
		Map<String, String> parameters = new LinkedHashMap<String, String>();

		if (defaultParameters != null)
		{
			for (Triple<String, String, String> param : defaultParameters)
			{
				if (RunnableCodeType.find(param.getSecond()) == null)
				{
					//String defaultValue = param.getSecond().toLowerCase(Locale.getDefault());
					String defaultValue = param.getSecond();
					if (defaultValue.indexOf ("#select#") != -1)
					{
						//String auxOptions = param.getSecond().replaceFirst("#select#", "").trim();
						String auxOptions = defaultValue.substring(defaultValue.indexOf ("#select#") + "#select#".length()).trim();
						String[] options = StringUtils.split(auxOptions, ", ");
						if (options.length > 0)
						{
							parameters.put(param.getFirst(), options[0]);
							continue;
						}
					}
					else if (defaultValue.indexOf("#boolean#") != -1)
					{
						boolean isSelected = Boolean.parseBoolean(defaultValue.substring(defaultValue.indexOf ("#boolean#") + "#boolean#".length()).trim());
						parameters.put(param.getFirst(), Boolean.toString(isSelected));
						continue;
					}
					
					parameters.put(param.getFirst(), param.getSecond());
				}
				else
				{
					parameters.put(param.getFirst() + "_file", "");
					parameters.put(param.getFirst() + "_classname", "");
					parameters.put(param.getFirst() + "_parameters", "");
				}
			}
		}

		if (inputParameters != null)
			for (Entry param : inputParameters)
				if (parameters.containsKey(param.getKey().toString()))
					parameters.put(param.getKey().toString(), param.getValue().toString());

		return parameters;
	}
}
