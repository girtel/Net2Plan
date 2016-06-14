/*******************************************************************************
 * Copyright (c) 2016 Pablo Pavon-Marino.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Contributors:
 * Pablo Pavon-Marino - Jose-Luis Izquierdo-Zaragoza, up to version 0.3.1
 * Pablo Pavon-Marino - from version 0.4.0 onwards
 ******************************************************************************/


package com.net2plan.interfaces.networkDesign;

import com.net2plan.internal.CommandLineParser;
import com.net2plan.internal.SystemUtils;
import com.net2plan.utils.Triple;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

/**
 * <p>Class containing current Net2Plan-wide options, and methods to work with them.</p>
 * <p>
 * <p>In the current version the available options are:</p>
 * <p>
 * <ul>
 * <li><tt>classpath</tt> (String): Set of external libraries loaded at runtime (separated by semi-colon). Default: None</li>
 * <li><tt>defaultRunnableCodePath</tt> (String): Default path (either .jar file or folder) for external code (i.e. algorithms). Default: {@code workspace\BuiltInExamples.jar} file</li>
 * <li><tt>topologyViewer</tt> (String): Type of topology viewer. Default: JUNGCanvas</li>
 * <li><tt>precisionFactor</tt> (double): Precision factor for checks to overcome numeric errors. Default: 1e-3</li>
 * </ul>
 * <p>
 * <p>In addition, due to the close relation to JOM library, some JOM-specific options can be configured:</p>
 * <p>
 * <ul>
 * <li><tt>defaultILPSolver</tt> (String): Default solver for LP/ILP models. Default: glpk</li>
 * <li><tt>defaultNLPSolver</tt> (String): Default solver for NLP models. Default: ipopt</li>
 * <li><tt>cplexSolverLibraryName</tt> (String): Default path for cplex library (.dll/.so/.dylib file). Default: None</li>
 * <li><tt>glpkSolverLibraryName</tt> (String): Default path for glpk library (.dll/.so/.dylib file). Default: None</li>
 * <li><tt>ipoptSolverLibraryName</tt> (String): Default path for ipopt library (.dll/.so/.dylib file). Default: None</li>
 * </ul>
 * <p>
 * <p><b>Important</b>: Values are stored in {@code String} format. Users are
 * responsible to make conversions to the appropiate type (i.e. {@code double}).</p>
 * <p>
 * <p><b>Important</b>: Users should not access this class directly. All interfaces
 * for implementing user-made code (i.e. algorithms) include a map so-called
 * {@code net2planParameters} as input parameter, where users can find the
 * current configuration of the tool (e.g. {@link com.net2plan.interfaces.networkDesign.IAlgorithm#executeAlgorithm(NetPlan, Map, Map) IAlgorithm.executeAlgorithm()}) .</p>
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 */
public class Configuration {
    private final static File optionsFile;
    private static File currentOptionsFile;
    private final static List<Triple<String, String, String>> defaultOptions;
    private final static Map<String, String> options;

    public static double precisionFactor = 0.001;

    static {
//		Set<String> canvasTypes = new LinkedHashSet<String>();
//		for(Class<? extends Plugin> plugin : PluginSystem.getPlugins(ITopologyCanvas.class))
//			canvasTypes.add(plugin.getName());

        defaultOptions = new LinkedList<Triple<String, String, String>>();
        defaultOptions.add(Triple.unmodifiableOf("classpath", "", "Set of external libraries loaded at runtime (separated by semi-colon)"));
        defaultOptions.add(Triple.unmodifiableOf("defaultRunnableCodePath", SystemUtils.getCurrentDir() + SystemUtils.getDirectorySeparator() + "workspace" + SystemUtils.getDirectorySeparator() + "BuiltInExamples.jar", "Default path (either .jar file or folder) for external code (i.e. algorithms)"));
        defaultOptions.add(Triple.unmodifiableOf("precisionFactor", "1e-3", "Precision factor for checks to overcome numeric errors"));
//		defaultOptions.add(Triple.unmodifiableOf("topologyViewer", "#select# " + StringUtils.join(canvasTypes, " "), "Type of topology viewer (it requires reloading active tool)"));
        defaultOptions.add(Triple.unmodifiableOf("cplexSolverLibraryName", "", "Default path for cplex library (.dll/.so/.dylib file)"));
        defaultOptions.add(Triple.unmodifiableOf("glpkSolverLibraryName", "", "Default path for glpk library (.dll/.so/.dylib file)"));
        defaultOptions.add(Triple.unmodifiableOf("ipoptSolverLibraryName", "", "Default path for ipopt library (.dll/.so/.dylib file)"));
        defaultOptions.add(Triple.unmodifiableOf("defaultILPSolver", "glpk", "Default solver for LP/ILP models"));
        defaultOptions.add(Triple.unmodifiableOf("defaultNLPSolver", "ipopt", "Default solver for NLP models"));
        options = CommandLineParser.getParameters(defaultOptions, null);

        optionsFile = new File(SystemUtils.getCurrentDir() + SystemUtils.getDirectorySeparator() + "options.ini");
        currentOptionsFile = optionsFile;
        precisionFactor = Double.parseDouble(getOption("precisionFactor"));
    }

    ;

    /**
     * Checks value of current options.
     *
     * @since 0.2.0
     */
    private static void check() {
        check(options);
    }

    /**
     * Checks the given options for validity.
     *
     * @param net2planParameters A key-value map with {@code Net2Plan}-wide configuration options
     * @since 0.2.2
     */
    public static void check(Map<String, String> net2planParameters) {
        try {
            Double precisionFactor = Double.parseDouble((String) net2planParameters.get("precisionFactor"));
            if (precisionFactor <= 0) throw new Exception("");
        } catch (Exception ex) {
            throw new Net2PlanException("'precisionFactor' option must be greater than zero");
        }
    }

    /**
     * Returns the current map of options (inlcuding ones those from plugins).
     *
     * @return Map of current options
     * @since 0.2.0
     */
    public static Map<String, String> getOptions() {
        return new LinkedHashMap<String, String>(options);
    }


    /**
     * Returns the current map of Net2Plan-wide options.
     *
     * @return Map of current options
     * @since 0.3.0
     */
    public static Map<String, String> getNet2PlanOptions() {
        Map<String, String> map = new LinkedHashMap<String, String>();
        for (Triple<String, String, String> parameter : defaultOptions)
            map.put(parameter.getFirst(), getOption(parameter.getFirst()));

        return map;
    }

    /**
     * <p>Returns the list of Net2Plan-wide parameters, where the first item of each element
     * is the parameter name, the second one is the parameter value, and the third
     * one is the parameter description.</p>
     * <p>
     * <p>It is possible to define type-specific parameters if the default value is set
     * according to the following rules (but user is responsible of checking in its own code):</p>
     * <p>
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
     * @return List of specific Net2Plan-wide parameters
     * @since 0.3.0
     */
    public static List<Triple<String, String, String>> getNet2PlanParameters() {
        return Collections.unmodifiableList(defaultOptions);
    }

    /**
     * Returns the value of an option.
     *
     * @param option Option name
     * @return Option value
     * @since 0.2.0
     */
    public static String getOption(String option) {
        if (!options.containsKey(option)) throw new RuntimeException("Unknown option '" + option + "'");

        String value = options.get(option);
        return value == null ? "" : value;
    }

    /**
     * Reads options from the default file.
     *
     * @throws IOException If the specified file cannot be loaded
     * @since 0.2.0
     */
    public static void readFromOptionsDefaultFile() throws IOException {
        if (optionsFile.exists()) readFromOptionsFile(optionsFile);
    }

    /**
     * Reads options from a given file.
     *
     * @param f Options file
     * @throws IOException If the specified file cannot be loaded
     * @since 0.2.0
     */
    public static void readFromOptionsFile(File f) throws IOException {
        Map<String, String> oldOptions = new LinkedHashMap<String, String>(options);
        Properties p = new Properties();

        try {
            try (InputStream in = new FileInputStream(f)) {
                p.load(in);
            }

            for (Entry<Object, Object> entry : p.entrySet())
                options.put(entry.getKey().toString(), entry.getValue().toString());

            check(options);

            currentOptionsFile = f.getAbsoluteFile();

            if (options.containsKey("classpath")) {
                String classpath = options.get("classpath");
                StringTokenizer tokens = new StringTokenizer(classpath, ";");
                while (tokens.hasMoreTokens()) {
                    String token = tokens.nextToken();
                    if (!token.isEmpty()) SystemUtils.addToClasspath(new File(token));
                }
            }
            precisionFactor = Double.parseDouble(getOption("precisionFactor"));
        } catch (FileNotFoundException ex) {
            throw new IOException("Options file not found (" + f + "), default options were loaded");
        } catch (Throwable ex1) {
            options.clear();
            options.putAll(oldOptions);
            throw new IOException(String.format("%s%n%s", ex1.getMessage(), "Default options were loaded"));
        }
    }


    /**
     * Saves current options to the file system.
     *
     * @since 0.2.0
     */
    public static void saveOptions() {
        check();

        Properties p = new Properties();

        for (Entry<String, String> entry : options.entrySet())
            p.setProperty(entry.getKey(), entry.getValue());

        try (OutputStream out = new FileOutputStream(currentOptionsFile)) {
            p.store(out, "Options file from Net2Plan");
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Puts the value for an option. If an option already exists, its value will be overriden.
     *
     * @param option Option name
     * @param value  Option value
     * @since 0.2.0
     */
    public static void setOption(String option, String value) {
        boolean isPresent = options.containsKey(option);
        String oldValue = null;

        try {
            if (isPresent) oldValue = options.get(option);

            options.put(option, value);
            precisionFactor = Double.parseDouble(getOption("precisionFactor"));
            check(options);
        } catch (Throwable e) {
            if (isPresent) {
                options.put(option, oldValue);
            }
        }
    }

    /**
     * Puts the value a set of options. If an option already exists, its value will be overriden.
     *
     * @param options Option name and value pairs
     * @since 0.3.0
     */
    public static void setOptions(Map<String, String> options) {
        Map<String, String> oldOptions = new LinkedHashMap<String, String>(options);
        Configuration.options.putAll(options);

        try {
            check(Configuration.options);
            precisionFactor = Double.parseDouble(getOption("precisionFactor"));
        } catch (Throwable e) {
            Configuration.options.clear();
            Configuration.options.putAll(oldOptions);
            throw new RuntimeException(e);
        }
    }
}
