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

import com.net2plan.internal.SystemUtils;
import com.net2plan.utils.ClassLoaderUtils;

import javax.swing.filechooser.FileFilter;
import java.io.File;
import java.util.*;

/**
 * Class to handle plugin management.
 * 
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.3.0
 */
public class PluginSystem
{
	private final static Set<Class<? extends Plugin>> PLUGIN_TYPES;
	private final static Map<Class<? extends Plugin>, Set<Class<? extends Plugin>>> PLUGINS;
	
	static
	{
		PLUGIN_TYPES = new LinkedHashSet<Class<? extends Plugin>>();
		PLUGINS = new LinkedHashMap<Class<? extends Plugin>, Set<Class<? extends Plugin>>>();
	}
	
	/**
	 * Adds an external plugin to the pluggable handler.
	 * 
	 * @param pluginType Class of the type of plugin implemented (i.e. CLI/GUI module, IO filter...)
	 * @param plugin Class of the plugin
	 * @since 0.3.0
	 */
	public static void addPlugin(Class<? extends Plugin> pluginType, Class<? extends Plugin> plugin)
	{
		if (!PLUGINS.containsKey(pluginType)) throw new RuntimeException("Bad - Invalid plugin type " + pluginType.getName());
		
		if (pluginType.isAssignableFrom(plugin)) PLUGINS.get(pluginType).add(plugin);
		else throw new RuntimeException("Bad");
	}
	
	/**
	 * Returns the set of available plugin types (i.e. CLI/GUI modules...).
	 * 
	 * @return Set of available plugin types
	 * @since 0.3.0
	 */
	public static Set<Class<? extends Plugin>> getPluginTypes()
	{
		return Collections.unmodifiableSet(PLUGIN_TYPES);
	}
	
	/**
	 * Returns the set of available plugins for a certain plugin type.
	 * 
	 * @param pluginType Type of plugin
	 * @return Set of available plugins of a given type
	 * @since 0.3.0
	 */
	public static Set<Class<? extends Plugin>> getPlugins(Class<? extends Plugin> pluginType)
	{
		return Collections.unmodifiableSet(PLUGINS.get(pluginType));
	}

	public static void addExternalPlugin(Class<? extends Plugin> plugin)
	{
		if (PLUGIN_TYPES.contains(plugin))
			throw new RuntimeException("Plugin " + plugin.getName() + " already defined...");

		PLUGIN_TYPES.add(plugin);
		PLUGINS.put(plugin, new TreeSet<>(new PluginComparator()));
	}

	/**
	 * Load (or reload) external plugins from 'plugins' folder.
	 * 
	 * @since 0.3.1
	 */
	public static void loadExternalPlugins()
	{
		if (PLUGINS.isEmpty())
		{
			switch(SystemUtils.getUserInterface())
			{
				case CLI:
					addExternalPlugin(ICLIModule.class);
					addExternalPlugin(IOFilter.class);
					break;

				case GUI:
					addExternalPlugin(IGUIModule.class);
					addExternalPlugin(IOFilter.class);
					break;
			}
		}
		
		File pluginsFolder = new File(SystemUtils.getCurrentDir(), "plugins");
		
		if (pluginsFolder.exists() && pluginsFolder.isDirectory())
		{
			FileFilter fileFilter = ClassLoaderUtils.getFileFilter();
			for(File file : pluginsFolder.listFiles())
			{
				if (file.isFile() && fileFilter.accept(file))
				{
					for(Class<? extends Plugin> _class : PLUGINS.keySet())
					{
//						System.out.println("Plugin type: " + _class + ", IN FILE: " + file);
						for(Class<? extends Plugin> plugin : ClassLoaderUtils.getClassesFromFile(file, _class , null))
						{
//							System.out.print (" -- class found in the file: " + plugin);
							if (!PLUGINS.get(_class).contains(plugin))
								{ addPlugin(_class, plugin); /*System.out.println(".. added"); */}
//							else
//								System.out.println(".. NOT added"); 
						}
					}
				}
			}
		}
	}
	
	private static class PluginComparator implements Comparator<Class<? extends Plugin>>
	{
		@Override
		public int compare(Class<? extends Plugin> o1, Class<? extends Plugin> o2)
		{
			try
			{
				Plugin p1 = o1.newInstance();
				Plugin p2 = o2.newInstance();

				int priority = Integer.compare(p2.getPriority(), p1.getPriority());
				return priority == 0 ? p1.getName().compareTo(p2.getName()) : priority;
			}
			catch(InstantiationException | IllegalAccessException e)
			{
				return 1;
			}
		}
	}
}
