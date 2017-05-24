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



 




package com.net2plan.internal.plugins;

import com.net2plan.interfaces.networkDesign.Configuration;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.internal.CommandLineParser;
import com.net2plan.internal.Constants.IOFeature;
import com.net2plan.io.IONet2Plan;

import javax.swing.filechooser.FileFilter;
import java.io.File;
import java.util.*;

/**
 * This class defines the template for input/output filters.
 * 
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.3.0
 */
@SuppressWarnings("unchecked")
public abstract class IOFilter extends FileFilter implements Plugin
{
	private final static UnsupportedOperationException UNSUPPORTED_IO_OPERATION = new UnsupportedOperationException();
	
	private final Set<String> acceptedExtensions;
	private final String description;
	private final EnumSet<IOFeature> features;
	
	/**
	 * Default constructor.
	 *
	 * @param name Name of the {@code IOFilter}
	 * @param features Set of provided features
	 * @param acceptedExtensions List of accepted extensions (empty means that all extensions are accepted)
	 * @since 0.3.0
	 */
	public IOFilter(String name, EnumSet<IOFeature> features, String... acceptedExtensions)
	{
		this.acceptedExtensions = new LinkedHashSet<String>();
		this.features = EnumSet.copyOf(features);

		if (acceptedExtensions != null && acceptedExtensions.length > 0)
		{
			this.acceptedExtensions.addAll(Arrays.asList(acceptedExtensions));
			
			StringBuilder extensions = new StringBuilder();
			extensions.append(String.format("*.%s", acceptedExtensions[0]));
			for(int i = 1; i < acceptedExtensions.length; i++)
				extensions.append(String.format(", *.%s", acceptedExtensions[i]));
			
			this.description = name + " (" + extensions.toString() + ")";
		}
		else
		{
			this.description = name + " (*.*)";
		}
	}
	
	@Override
	public final boolean accept(File f)
	{
		if (f == null) return false;
		if (f.isDirectory() || acceptAllExtensions()) return true;

		String fileName = f.getName();
		int i = fileName.lastIndexOf('.');
		if (i > 0 && i < fileName.length() - 1)
		{
			Set<String> extensions = getAcceptedExtensions();
			String desiredExtension = fileName.substring(i + 1).toLowerCase(Locale.ENGLISH);

			for (String extension : extensions)
				if (desiredExtension.equalsIgnoreCase(extension))
					return true;
		}

		return false;
	}
	
	@Override
	public boolean equals(Object o)
	{
		if (o == null) return false;
		if (o == this) return true;
		
		return getClass().getName().equals(o.getClass().getName());
	}

	@Override
	public final Map<String, String> getCurrentOptions()
	{
		return CommandLineParser.getParameters(getParameters(), Configuration.getOptions());
	}
	
	@Override
	public final String getDescription() { return description; }
	
	@Override
	public int getPriority()
	{
		return 0;
	}
	
	@Override
	public int hashCode()
	{
		int hash = 7;
		hash = 59 * hash + Objects.hashCode(this.acceptedExtensions);
		hash = 59 * hash + Objects.hashCode(this.description);
		hash = 59 * hash + Objects.hashCode(this.features);
		return hash;
	}
	
	/**
	 * Returns whether accepts all extensions.
	 *
	 * @return {@code true} if any extension is acceptable, and {@code false} otherwise.
	 * @since 0.3.0
	 */
	public boolean acceptAllExtensions() { return getAcceptedExtensions().isEmpty(); }
	
	/**
	 * Returns the set of accepted extensions by the {@code IOFilter}.
	 *
	 * @return Set of accepted extensions by the {@code IOFilter}.
	 * @since 0.3.0
	 */
	public Set<String> getAcceptedExtensions() { return Collections.unmodifiableSet(acceptedExtensions); }
	
	/**
	 * Returns the set of provided features by the {@code IOFilter}.
	 *
	 * @return Set of provided features by the {@code IOFilter}.
	 * @since 0.3.0
	 */
	public final EnumSet<IOFeature> getFeatures() { return EnumSet.copyOf(features); }
	
	/**
	 * Returns the set of {@code IOFilter} implementations supporting at least one of the 
	 * requested features.
	 *
	 * @param features Set of requested features
	 * @return Set of {@code IOFilter} implementations supporting at least one of the requested features
	 * @since 0.3.0
	 */
	public static Set<IOFilter> getIOFiltersByFeatures(EnumSet<IOFeature> features)
	{
		Set<IOFilter> ioFilters = new LinkedHashSet<IOFilter>();
		final Set<Class<? extends Plugin>> _classes = PluginSystem.getPlugins(IOFilter.class);
		List<Class<? extends Plugin>> _orderedClasses = new LinkedList<Class<? extends Plugin>> ();
		if (_classes.contains(IONet2Plan.class)) _orderedClasses.add (IONet2Plan.class);
		for (Class<? extends Plugin> filter : _classes) if (filter != IONet2Plan.class) _orderedClasses.add (filter);
		
		for(Class<? extends Plugin> _class : _orderedClasses)
		{
			try
			{
				IOFilter ioFilter = ((Class<? extends IOFilter>) _class).newInstance();
				Set<IOFeature> features_thisIOFilter = ioFilter.getFeatures();
				
				for (IOFeature feature : features)
				{
					if (features_thisIOFilter.contains(feature))
					{
						ioFilters.add(ioFilter);
						break;
					}
				}
			}
			catch (InstantiationException | IllegalAccessException ex)
			{
				throw new RuntimeException(ex);
			}
		}
		
		return ioFilters;
	}
	
	/**
	 * Loads a demand set from a given file.
	 *
	 * @param file Input file
	 * @return Network design
	 * @since 0.3.0
	 */
	public NetPlan readDemandSetFromFile(File file) { throw UNSUPPORTED_IO_OPERATION; }
	
	/**
	 * Loads a design from a given file.
	 *
	 * @param file Input file
	 * @return Network design
	 * @since 0.3.0
	 */
	public NetPlan readFromFile(File file) { throw UNSUPPORTED_IO_OPERATION; }

	/**
	 * Saves a demand set to the given file.
	 *
	 * @param netPlan Network design
	 * @param file Output file
	 * @since 0.3.0
	 */
	public void saveDemandSetToFile(NetPlan netPlan, File file) { throw UNSUPPORTED_IO_OPERATION; }

	/**
	 * Saves a network design to a given file.
	 *
	 * @param netPlan Network design
	 * @param file Output file
	 * @since 0.3.0
	 */
	public void saveToFile(NetPlan netPlan, File file) { throw UNSUPPORTED_IO_OPERATION; }
}
