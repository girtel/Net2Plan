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

import com.net2plan.internal.IExternal;
import com.net2plan.utils.Triple;

import java.util.List;
import java.util.Map;

/**
 * <p>Contract that must be fulfilled such that an algorithm can be run in {@code Net2Plan}.</p>
 * 
 * <p>New algorithms can be implemented as Java classes (.class/.jar files) with
 * a given signature. Integration of algorithms simply requires saving them in any
 * directory of the computer, although it is a good practice to store them in the
 * {@code \workspace} directory.</p>
 * 
 * <p><b>Important</b>: When algorithms are implemented as Java {@code .class} files,
 * the full path to the class must follow the package name of the class, in order
 * to successfully load the algorithm. For example, if we create an algorithm
 * named {@code testAlgorithm} in the package {@code test.myAlgorithms}, the full
 * path to the class must be like {@code ...{any}.../test/myAlgorithms/testAlgorithm.class}.
 * For {@code .jar} files there is not any restriction on the full path,
 * they can be stored at any folder.</p>
 * 
 * <p>In order to improve the user experience, kernel is able to catch any exception
 * thrown by algorithms. Those exceptions are introduced in a Java error console
 * (see 'Help file' for more information). For example, exceptions allow users to
 * check if their algorithms are well-programmed (i.e. no null-pointers), thus in
 * the error console they got a full trace of the error (files, line numbers...).
 * However, when input parameters are wrong or an error within the algorithm is
 * returned (i.e. an optimization problem is unable to find feasible solutions),
 * we recommend to throw exceptions using the
 * {@link com.net2plan.interfaces.networkDesign.Net2PlanException Net2PlanException} class,
 * since it allows users to see the error in a small dialog in which the only
 * information is the error message (i.e. "A feasible solution was not found")
 * instead of the full error trace in the error console (i.e.
 * {@code RuntimeException: A feasible solution was not found (MyAlgorithm.java:35), ...}).</p>
 * 
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public interface IAlgorithm extends IExternal
{
	/**
	 * Execute the algorithm.
	 *
	 * @param netPlan A network plan which serves as input and output
	 * @param algorithmParameters A key-value map with specific algorithm parameters.<p><b>Important</b>: The algorithm developer is responsible to convert values from String to their respective type, and to check that values</p>
	 * @param net2planParameters A key-value map with {@code Net2Plan}-wide configuration options
	 * @return An output {@code String}
	 * @since 0.2.0
	 */
	public String executeAlgorithm(NetPlan netPlan, Map<String, String> algorithmParameters, Map<String, String> net2planParameters);
	
	/**
	 * Returns the description.
	 *
	 * @return Description
	 * @since 0.2.0
	 */
	@Override
	public String getDescription();

	/**
	 * <p>Returns the list of required parameters, where the first item of each element 
	 * is the parameter name, the second one is the parameter value, and the third 
	 * one is the parameter description.</p>
	 * 
	 * <p>It is possible to define type-specific parameters if the default value is set 
	 * according to the following rules (but user is responsible of checking in its own code):</p>
	 * 
	 * <ul>
	 * <li>If the default value is #select#	and a set of space- or comma-separated values, 
	 * the GUI will show a combobox with all the values, where the first one will be the 
	 * selected one by default. For example: "#select# hops km" will allow choosing in the 
	 * GUI between hops and km, where hops will be the default value.</li>
	 * <li>If the default value is #boolean# and true or false, the GUI will show a checkbox, 
	 * where the default value will be true or false, depending on the value accompanying to 
	 * #boolean#. For example: "#boolean# true" will show a checkbox marked by default.</li>
	 * <li>If the default value is #algorithm#, the GUI will prepare a new interface to 
	 * select an algorithm as parameter. Three new fields will be added, including "_file", 
	 * "_classname" and "_parameters" suffixes to the indicated parameter name, refer to 
	 * the {@code .class} or {@code .jar} file where the code is located, the class name, 
	 * and a set of parameters (pair of key-values separated by commas, where individual 
	 * key and value are separated with an equal symbol. The same applies to reports (#report#), 
	 * event generators (#eventGenerator#) and event processors (#eventProcessor#).</li>
	 * </ul>
	 * 
	 * @return List of specific parameters
	 * @since 0.2.0
	 */
	@Override
	public List<Triple<String, String, String>> getParameters();
}
