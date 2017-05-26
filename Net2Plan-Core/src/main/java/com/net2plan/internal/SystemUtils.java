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

import com.google.common.io.Files;
import com.net2plan.interfaces.networkDesign.Configuration;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.internal.Constants.UserInterface;
import com.net2plan.internal.plugins.PluginSystem;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;

/**
 * Class with system utilities depending on the operating system and locale configuration.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public class SystemUtils
{
	private static Class mainClass;
	private static UserInterface ui = null;
	private static Set<URL> defaultClasspath;

	/**
	 * Adds a given file to the classpath.
	 * 
	 * @param f File to be added
	 * @since 0.3.1
	 */
	public static void addToClasspath(File f)
	{
		try
		{
			Method addURL = URLClassLoader.class.getDeclaredMethod("addURL", new Class[] { URL.class });
			addURL.setAccessible(true);
			
			URL url = f.toURI().toURL();
			ClassLoader cl = ClassLoader.getSystemClassLoader();
			addURL.invoke(cl, new Object[] { url });
		}
		catch (NoSuchMethodException | SecurityException | MalformedURLException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Configures the environment for Net2Plan (locale, number format, look and feel...).
	 * 
	 * @param mainClass Main class of the running program
	 * @param ui User interface (GUI or CLI)
	 * @since 0.2.3
	 */
	public static void configureEnvironment(Class mainClass, UserInterface ui)
	{
		SystemUtils.mainClass = mainClass;

		try { Locale.setDefault(Locale.US); }
		catch (Throwable e) { }

		try { NumberFormat.getInstance().setGroupingUsed(false); }
		catch (Throwable e) { }

		if (SystemUtils.ui != null) throw new RuntimeException("Environment was already configured");
		SystemUtils.ui = ui;
		
		if (ui == UserInterface.GUI)
		{
			try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
			catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) { }

			Color bgColor = new Color(200, 200, 200);
			Color fgColor = new Color(0, 0, 0);

			/* More information on http://www.java2s.com/Tutorial/Java/0240__Swing/Catalog0240__Swing.htm */
			/* Also googling "Customizing XXX Look and Feel", where XXX is the component name */
			UIManager.put("Button.background", bgColor);
			UIManager.put("CheckBox.background", bgColor);
			UIManager.put("CheckBox.interiorBackground", Color.WHITE);
			UIManager.put("ComboBox.background", Color.WHITE);
			UIManager.put("Label.background", bgColor);
			UIManager.put("Menu.background", bgColor);
			UIManager.put("MenuBar.background", bgColor);
			UIManager.put("MenuItem.background", bgColor);
			UIManager.put("OptionPane.background", bgColor);
			UIManager.put("Panel.background", bgColor);
			UIManager.put("RadioButton.background", bgColor);
			UIManager.put("ScrollPane.background", bgColor);
			UIManager.put("Separator.background", bgColor);
			UIManager.put("SplitPane.background", bgColor);
			UIManager.put("TabbedPane.background", bgColor);
			UIManager.put("Table.background", Color.WHITE);
			UIManager.put("TableHeader.background", bgColor);
			UIManager.put("TextArea.background", Color.WHITE);
			UIManager.put("TextField.background", Color.WHITE);
			UIManager.put("TextField.disabledBackground", bgColor);
			UIManager.put("TextField.inactiveBackground", bgColor);
			UIManager.put("ToolBar.background", bgColor);
			UIManager.put("Viewport.background", bgColor);
			UIManager.put("Button.foreground", fgColor);
			UIManager.put("CheckBox.foreground", fgColor);
			UIManager.put("ComboBox.foreground", fgColor);
			UIManager.put("Label.foreground", fgColor);
			UIManager.put("Label.disabledForeground", fgColor);
			UIManager.put("Menu.foreground", fgColor);
			UIManager.put("MenuBar.foreground", fgColor);
			UIManager.put("MenuItem.foreground", fgColor);
			UIManager.put("MenuItem.acceleratorForeground", fgColor);
			UIManager.put("OptionPane.foreground", fgColor);
			UIManager.put("RadioButton.foreground", fgColor);
			UIManager.put("Separator.foreground", fgColor);
			UIManager.put("TabbedPane.foreground", fgColor);
			UIManager.put("Table.foreground", fgColor);
			UIManager.put("TableHeader.foreground", fgColor);
			UIManager.put("TextArea.foreground", fgColor);
			UIManager.put("TextField.foreground", fgColor);
			UIManager.put("TextField.inactiveForeground", fgColor);
			UIManager.put("Separator.shadow", bgColor);
			UIManager.put("TitledBorder.titleColor", fgColor);
		}
		
		defaultClasspath = getClasspath();
		
		/* Load options */
		try { Configuration.readFromOptionsDefaultFile();  }
		catch (IOException ex) { throw new Net2PlanException("Error loading options: " + ex.getMessage()); }
		catch (Throwable ex) { ex.printStackTrace(); throw new RuntimeException(ex); }

		PluginSystem.loadExternalPlugins();
	}

	/**
	 * Returns the system classpath.
	 *
	 * @return System classpath
	 * @since 0.3.1
	 */
	public static Set<URL> getClasspath()
	{
		ClassLoader cl = ClassLoader.getSystemClassLoader();
		Set<URL> classpath = new TreeSet<URL>(new URLComparator());
		classpath.addAll(Arrays.asList(((URLClassLoader) cl).getURLs()));
		
		return Collections.unmodifiableSet(classpath);
	}
	
	/**
	 * Returns the current directory where the application is executing in.
	 *
	 * @return Current directory where the application is executing in
	 * @since 0.2.0
	 */
	public static File getCurrentDir()
	{
		try
		{
			if (mainClass == null) throw new RuntimeException("Bad");

			File curDir = new File(mainClass.getProtectionDomain().getCodeSource().getLocation().toURI());
			if (!curDir.isDirectory()) curDir = curDir.getAbsoluteFile().getParentFile();
			
			return curDir;
		}
		catch (RuntimeException | URISyntaxException e)
		{
			try
			{
				return new File("test").getCanonicalFile().getParentFile();
			}
			catch(Throwable e1)
			{
				throw new RuntimeException(e1);
			}
		}
	}

	/**
	 * Returns the system-dependent default name-separator character.
	 *
	 * @return System-dependent default name-separator character
	 * @since 0.2.2
	 */
	public static String getDirectorySeparator()
	{
		return File.separator;
	}

	/**
	 * Returns the decimal separator
	 *
	 * @return The decimal separator
	 * @since 0.2.0
	 */
	public static char getDecimalSeparator()
	{
		return ((DecimalFormat) DecimalFormat.getInstance()).getDecimalFormatSymbols().getDecimalSeparator();
	}

	/**
	 * Returns the extension of the given file.
	 *
	 * @param file File to analyze
	 * @return File extension, an empty {@code String} ("") if it doesn't have extension
	 * @throws IllegalArgumentException If {@code file} is not an existing file or it is a directory
	 * @since 0.2.0
	 */
	public static String getExtension(File file)
	{
		return Files.getFileExtension(file.getAbsolutePath());
	}

	/**
	 * Returns the name (without extension or path) of the given file.
	 *
	 * @param file File to analyze
	 * @return File name
	 * @since 0.2.0
	 */
	public static String getFilename(File file)
	{
		return Files.getNameWithoutExtension(file.getAbsolutePath());
	}
	
	/**
	 * Returns the path of a file (without file name or extension).
	 *
	 * @param file File to analyze
	 * @return Absolute path of {@code file}
	 * @since 0.3.1
	 */
	public static String getPath(File file)
	{
		String absolutePath = file.getAbsolutePath();
		return absolutePath.substring(0, absolutePath.lastIndexOf(getDirectorySeparator()));
	}
	
	/**
	 * Returns the user classpath ({@code .class/.jar} files not added by default).
	 *
	 * @return User classpath
	 * @since 0.3.1
	 */
	public static Set<URL> getUserClasspath()
	{
		Set<URL> classpath = new TreeSet<URL>(new URLComparator());
		classpath.addAll(getClasspath());
		classpath.removeAll(defaultClasspath);
		
		return Collections.unmodifiableSet(classpath);
	}

	/**
	 * Returns the active user interface (GUI or CLI).
	 *
	 * @return Constant indicating the user interface that is active
	 * @since 0.3.0
	 */
	public static UserInterface getUserInterface()
	{
		return ui;
	}
	
	private static class URLComparator implements Comparator<URL>
	{
		@Override
		public int compare(URL o1, URL o2)
		{
			if (o1.sameFile(o2)) return 0;
			else return o1.toString().compareTo(o2.toString());
		}
	}
}
