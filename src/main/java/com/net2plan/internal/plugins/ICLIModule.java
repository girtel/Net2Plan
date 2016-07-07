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

import java.util.Map;

import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.net2plan.interfaces.networkDesign.Configuration;
import com.net2plan.internal.CommandLineParser;

/**
 * Interface for any module to be executed from the command-line user interface.
 * 
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.2
 */
public abstract class ICLIModule implements Plugin
{
	/**
	 * Executes the module according to the specified command-line call.
	 * 
	 * @param args Command line parameters
	 * @throws ParseException
	 * @since 0.2.2
	 */
	public abstract void executeFromCommandLine(String[] args) throws ParseException;

	/**
	 * Returns a human-readable help for this module.
	 * 
	 * @return Help string
	 * @since 0.2.2
	 */
	public abstract String getCommandLineHelp();

	/**
	 * Returns the set of options for this module.
	 * 
	 * @return Options for this module
	 * @since 0.2.2
	 */
	public abstract Options getCommandLineOptions();
	
	@Override
	public final Map<String, String> getCurrentOptions()
	{
		return CommandLineParser.getParameters(getParameters(), Configuration.getOptions());
	}
	
	@Override
	public int getPriority()
	{
		return 0;
	}
	
	/**
	 * Returns the name for the mode to be used for the {@code --mode} option of the CLI.
	 * 
	 * @return Mode name
	 * @since 0.3.0
	 */
	public abstract String getModeName();
}
