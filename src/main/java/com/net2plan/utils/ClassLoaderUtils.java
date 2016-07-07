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

package com.net2plan.utils;

import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.internal.SystemUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * <p>Class to deal with dynamic Java class loading from .class/.jar files.</p>
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public class ClassLoaderUtils
{
	private final static FileFilter fileFilter = new FileNameExtensionFilter("Java files (*.class, *.jar)", "class", "jar");
	
	private ClassLoaderUtils() { }

	/**
	 * Returns a Java class from a .class file. It does the best to guess the classpath and fully qualified name of the class.
	 *
	 * @param classFile .class file
	 * @return Class defined in the .class file
	 * @since 0.2.0
	 */
	private static Class getClassFromClassFile(File classFile)
	{
		try
		{
			Pair<File, String> aux = getClasspathAndQualifiedNameFromClassFile(classFile);

			new URL("http://localhost/").openConnection().setDefaultUseCaches(false);
			URLClassLoader ucl = new URLClassLoader(new URL[] { aux.getFirst().toURI().toURL() });

			return ucl.loadClass(aux.getSecond());
		}
		catch (IOException | ClassNotFoundException e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * Returns a list of Java classes from a .class/.jar file.
	 *
	 * @param file .class/.jar file
	 * @return List of found Java classes
	 * @since 0.2.0
	 */
	private static List<Class> getClassesFromFile(File file)
	{
		List<Class> classes = new LinkedList<Class>();

		String extension = SystemUtils.getExtension(file).toLowerCase(Locale.getDefault());
		switch (extension)
		{
			case "jar":
				classes.addAll(getClassesFromJar(file));
				break;

			case "class":
				classes.add(getClassFromClassFile(file));
				break;

			default:
				throw new RuntimeException("'file' is not a valid Java file (.jar or .class)");
		}

		if (classes.isEmpty())
		{
			throw new RuntimeException("Java classes not found in '" + file + "'");
		}

		return classes;
	}

	/**
	 * Returns a list of Java classes from a .class/.jar file implementing/extending the specified class.
	 *
	 * @param <T> Class type
	 * @param file .class/.jar file
	 * @param _class Reference to the class
	 * @return List of found Java classes implementing/extending {@code _class}
	 */
	public static <T> List<Class<T>> getClassesFromFile(File file, Class<T> _class)
	{
		List<Class> allClasses = getClassesFromFile(file);
		List<Class<T>> classes = new LinkedList<Class<T>>();

//		System.out.println (allClasses);
		for (Class aux : allClasses)
		{
			if (_class.isAssignableFrom(aux))
			{
				classes.add(aux);
			}
//			else
//				System.out.println ("Class " + _class + " is not assignable from: " + aux);

		}

		return classes;
	}

	/**
	 * Returns a class from a .class/.jar file implementing/extending with the given fully qualified name.
	 *
	 * @param classFile .class/.jar file
	 * @param qualifiedName Fully qualified name of the class ({@code package.className})
	 * @return Java class
	 * @since 0.2.0
	 */
	private static Class getClassFromFile(File classFile, String qualifiedName)
	{
		if (!classFile.exists()) throw new Net2PlanException(classFile + " does not exist");
		if (!classFile.isFile()) throw new Net2PlanException(classFile + " is not a valid file (e.g. it is a directory)");

		String extension = SystemUtils.getExtension(classFile).toLowerCase(Locale.getDefault());
		switch (extension)
		{
			case "jar":
				try
				{
					new URL("http://localhost/").openConnection().setDefaultUseCaches(false);
					URLClassLoader ucl = new URLClassLoader(new URL[] { classFile.toURI().toURL() }, ClassLoader.getSystemClassLoader());
					return ucl.loadClass(qualifiedName);
				}
				catch (ClassNotFoundException e)
				{
					try
					{
						List<Class> classesInFile = getClassesFromFile(classFile);
						Iterator<Class> it = classesInFile.iterator();
						while (it.hasNext())
						{
							Class aux = it.next();
							if (aux.getName().endsWith(qualifiedName))
								return getClassFromFile(classFile, aux.getName());
						}

						throw new RuntimeException(e);
					}
					catch (Throwable e1)
					{
						throw new RuntimeException(e1);
					}
				}
				catch (IOException e)
				{
					throw new RuntimeException(e);
				}
			case "class":
				return getClassFromClassFile(classFile);

			default:
				throw new RuntimeException("'file' is not a valid Java file (.jar or .class)");
		}
	}

	/**
	 * Returns a Java class from a .class file implementing/extending the specified class.
	 *
	 * @param <T> a generic type
	 * @param file .class/.jar file
	 * @param className the class name
	 * @param _class Reference to the class
	 * @return Class defined in the .class file
	 * @since 0.2.0
	 */
	private static <T> Class<T> getClassFromFile(File file, String className, Class<T> _class)
	{
		Class aux = getClassFromFile(file, className);
		if (_class.isAssignableFrom(aux)) return (Class<T>) aux;

		throw new RuntimeException("File '" + file + "' doesn't contain any Java class named '" + className + "' of the required type (" + _class.getName() + ")");
	}

	/**
	 * Returns a list of Java classes from a .jar file
	 *
	 * @param jarFile .jar file
	 * @return List of found Java classes
	 * @since 0.2.0
	 */
	private static List<Class> getClassesFromJar(File jarFile)
	{
		try
		{
			new URL("http://localhost/").openConnection().setDefaultUseCaches(false);
			URLClassLoader cl = new URLClassLoader(new URL[] { jarFile.toURI().toURL() }, ClassLoader.getSystemClassLoader());

			List<Class> classes = new LinkedList<Class>();
			try (JarInputStream jar = new JarInputStream(new FileInputStream(jarFile)))
			{
				JarEntry jarEntry;

				while (true)
				{
					jarEntry = jar.getNextJarEntry();
					if (jarEntry == null) break;

					if (jarEntry.getName().endsWith(".class"))
					{
						String className = jarEntry.getName().replaceAll("/", "\\.").replaceAll(".class", "");
						if (className.contains("$")) continue;
						
						Class _class;
//						System.out.print("getClassesFromJar (" + jarFile + "): " + className);
						try { _class = cl.loadClass(className); /*System.out.println("... OK"); */ }
						catch(NoClassDefFoundError e) { /*System.out.println("... ERROR"); e.printStackTrace(); */ continue; }
						
						int modifier = _class.getModifiers();
						if (Modifier.isAbstract(modifier) || Modifier.isInterface(modifier))
							continue;

						classes.add(_class);
					}
				}
			}

			return classes;
		}
		catch (IOException | ClassNotFoundException e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * Tries to guess what are the classpath and fully qualified name of a .class Java file
	 *
	 * @param classFile .class file
	 * @return An object pair in which the first element is the classpath of the class, and the second one is the fully qualified name
	 * @since 0.2.0
	 */
	private static Pair<File, String> getClasspathAndQualifiedNameFromClassFile(File classFile)
	{
		String className = SystemUtils.getFilename(classFile);
		File fullPath = classFile.getAbsoluteFile().getParentFile();
		File currentPath = classFile.getAbsoluteFile().getParentFile();

		while (true)
		{
			try
			{
				URL url = currentPath.toURI().toURL();
				String packageIdentifier = "";
				
				if (!fullPath.equals(currentPath))
				{
					packageIdentifier = url.toURI().relativize(fullPath.toURI()).toString().replaceAll("[\\\\/]", ".");
					packageIdentifier += packageIdentifier.endsWith(".") ? "" : ":";
				}
				
				new URL("http://localhost/").openConnection().setDefaultUseCaches(false);
				URLClassLoader ucl = new URLClassLoader(new URL[] { url }, ClassLoader.getSystemClassLoader());
				ucl.loadClass(packageIdentifier + className);

				return Pair.of(currentPath, packageIdentifier + className);
			}
			catch (URISyntaxException | IOException | ClassNotFoundException | NoClassDefFoundError e)
			{
			}

			currentPath = currentPath.getAbsoluteFile().getParentFile();
			if (currentPath == null) break;
		}

		throw new RuntimeException("File '" + classFile + "' doesn't contain any Java class");
	}

	/**
	 * Returns the file filter for this class loader.
	 * 
	 * @return File filter (*.class, *.jar)
	 */
	public static FileFilter getFileFilter()
	{
		return fileFilter;
	}

	/**
	 * Returns a new instance for the desired class from a given file.
	 * 
	 * @param <T> Class type
	 * @param file .class/.jar file
	 * @param className the class name
	 * @param _class Reference to the class
	 * @return An instance of the given class from 
	 */
	public static <T> T getInstance(File file, String className, Class<T> _class)
	{
		try
		{
			return getClassFromFile(file, className, _class).newInstance();
		}
		catch (InstantiationException | IllegalAccessException e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * Given a fully qualified class name returns the package and class names.
	 * 
	 * @param fullyQualifiedClassName Fully qualified class name
	 * @return {@code String} array in which the first element is the package name (empty {@code String} means no package), and the second one is the class name
	 */
	public static Pair<String, String> getPackageAndClassName(String fullyQualifiedClassName)
	{
		String packageName = "";
		String className;

		int i = fullyQualifiedClassName.lastIndexOf('.');
		if (i == -1)
		{
			className = fullyQualifiedClassName;
		}
		else
		{
			packageName = fullyQualifiedClassName.substring(0, i);
			className = fullyQualifiedClassName.substring(i + 1);
		}

		return Pair.of(packageName, className);
	}
}
