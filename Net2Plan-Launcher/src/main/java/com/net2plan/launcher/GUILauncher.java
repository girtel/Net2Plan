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

package com.net2plan.launcher;

import com.google.common.base.Splitter;
import com.net2plan.gui.GUINet2Plan;
import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.gui.plugins.GUITrafficDesign;
import com.net2plan.gui.utils.Robot;
import com.net2plan.interfaces.IGUIModeWrapper;
import com.net2plan.internal.ErrorHandling;
import com.net2plan.internal.plugins.IGUIModule;
import com.net2plan.internal.plugins.PluginSystem;
import com.net2plan.utils.Pair;
import org.apache.commons.cli.*;
import org.apache.commons.lang.ArrayUtils;
import org.reflections.Reflections;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by Jorge San Emeterio on 17/03/17.
 */
public class GUILauncher
{
    private static IGUIModule currentPlugin;

    private final static Options OPTIONS;
    private final static OptionGroup LAUNCH_TYPE;

    static
    {
        // Parse input parameter
        OPTIONS = new Options();

        LAUNCH_TYPE = new OptionGroup();
        LAUNCH_TYPE.setRequired(false);

        final Option vanilla = new Option("v", null, false, "Launch GUI as if it was executed from outside.");
        LAUNCH_TYPE.addOption(vanilla);

        final Option robot = new Option("r", null, false, "Launch GUI under an automated tool.");
        LAUNCH_TYPE.addOption(robot);

        final Option debug = new Option("d", null, false, "(Optional) Launch GUI in debug mode.");
        debug.setRequired(false);

        OPTIONS.addOptionGroup(LAUNCH_TYPE);
        OPTIONS.addOption(debug);
    }

    public static void main(String[] args)
    {
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        try
        {
            final CommandLine cl = parser.parse(OPTIONS, args, true);

            boolean isDebug = cl.hasOption('d');
            if (isDebug) args = (String[]) ArrayUtils.removeElement(args, "-d");

            if (LAUNCH_TYPE.getSelected() == null) LAUNCH_TYPE.setSelected(OPTIONS.getOption("v"));

            if (LAUNCH_TYPE.getSelected().equals("v"))
            {
                // Vanilla launcher
                GUINet2Plan.main(args);
                PluginSystem.addPlugin(IGUIModule.class, GUINetworkDesign.class);
                PluginSystem.addPlugin(IGUIModule.class, GUITrafficDesign.class);
                PluginSystem.loadExternalPlugins();
                GUINet2Plan.refreshMenu();

                ErrorHandling.setDebug(isDebug);
            } else if (LAUNCH_TYPE.getSelected().equals("r"))
            {
                // Robot launcher
                final String[] robot_args = (String[]) ArrayUtils.removeElement(args, "-r");

                //New options
                final Options R_OPTIONS = new Options();

                final Option plugin = new Option(null, "plugin", true, "Class name of the tool/plugin");
                plugin.setRequired(true);
                plugin.setType(PatternOptionBuilder.CLASS_VALUE);
                plugin.setArgName("Tool/Plugin");
                R_OPTIONS.addOption(plugin);

                final Option routine = new Option(null, "routine", true, "(Optional) Tool/Plugin launch mode. By default launches mode 1.");
                routine.setRequired(false);
                routine.setType(PatternOptionBuilder.NUMBER_VALUE);
                routine.setArgName("Launch mode");
                R_OPTIONS.addOption(routine);

                final Option packageURL = new Option(null, "package-name", true, "(Optional) Name of the package containing the tool");
                packageURL.setRequired(false);
                packageURL.setArgName("Package name");
                packageURL.setType(PatternOptionBuilder.STRING_VALUE);
                R_OPTIONS.addOption(packageURL);

                final Option param = new Option(null, "tool-param", true, "(Optional) Tool/Plugin launch mode parameters. Each pair key-value separated by spaces.");
                param.setRequired(false);
                param.setArgName("property=value");
                param.setValueSeparator('=');
                R_OPTIONS.addOption(param);

                final CommandLine cmd = parser.parse(R_OPTIONS, robot_args, true);

                // Scan for the given plugin
                final String inputPlugin = cmd.getOptionValue("plugin");

                // Default package
                String packageName = "com.net2plan.gui.plugins";
                if (cmd.hasOption("package-name")) packageName = cmd.getOptionValue("package-name");

                final Pair<IGUIModule, IGUIModeWrapper> pluginPair = findPlugin(inputPlugin, packageName);
                currentPlugin = pluginPair.getFirst();

                // Plugin not found
                if (currentPlugin == null)
                {
                    formatter.printHelp("GUILauncher", R_OPTIONS);
                    throw new ParseException("Plugin: " + inputPlugin + " could not be found at package: " + packageName);
                }

                // Exceptions are grabbed by N2P from now on.

                // Run Net2Plan
                GUINet2Plan.main(new String[0]);
                PluginSystem.addPlugin(IGUIModule.class, currentPlugin.getClass());
                GUINet2Plan.refreshMenu();

                ErrorHandling.setDebug(isDebug);

                runPlugin();

                // Parse mode and params

                // Looking for plugin wrapper
                final IGUIModeWrapper wrapper = pluginPair.getSecond();

                if (wrapper != null)
                {
                    int mode = 1;

                    Map<String, String> parameters = new HashMap<>();
                    if (cmd.hasOption("routine"))
                    {
                        mode = Integer.parseInt(cmd.getOptionValue("routine"));
                        if (cmd.hasOption("tool-param"))
                            parameters = parseParameters(cmd.getOptionValue("tool-param"), R_OPTIONS.getOption("tool-param").getValueSeparator());
                    }

                    wrapper.launchRoutine(mode, parameters);
                } else
                {
                    ErrorHandling.showErrorDialog("Debug wrapper not found for class: " + inputPlugin);
                }
            } else
            {
                throw new ParseException("Unknown launch option: " + LAUNCH_TYPE.getSelected());
            }
        } catch (ParseException e)
        {
            System.err.println(e.getMessage());
            formatter.printHelp("GUILauncher", OPTIONS);
        } catch (Exception e)
        {
            ErrorHandling.showErrorDialog("An error happened while running launcher.\nCheck console for more details.");
            e.printStackTrace();
        }
    }

    private static void runPlugin() throws AWTException
    {
        final Robot robot = new Robot();

        // Showing the tool
        final KeyStroke pluginKeyStroke = currentPlugin.getKeyStroke();
        final int keyModifier = getKeyModifier(pluginKeyStroke);

        if (keyModifier == -1) // No modifier
            robot.type(pluginKeyStroke.getKeyCode());
        else
            robot.type(pluginKeyStroke.getKeyCode(), keyModifier);
    }

    private static Map<String, String> parseParameters(final String parameter, final char separator)
    {
        return Splitter.on(" ").withKeyValueSeparator(separator).split(parameter);
    }

    private static int getKeyModifier(final KeyStroke keyStroke)
    {
        final String modifierString = keyStroke.toString().split("pressed")[0].trim();

        switch (modifierString)
        {
            case "alt":
                return KeyEvent.VK_ALT;
            case "shift":
                return KeyEvent.VK_SHIFT;
            case "ctrl":
                return KeyEvent.VK_CONTROL;
            default:
                return -1;
        }
    }

    private static Pair<IGUIModule, IGUIModeWrapper> findPlugin(final String pluginName, final String packageName)
    {
        Reflections reflections = new Reflections(packageName);
        Set<Class<? extends IGUIModule>> modules = reflections.getSubTypesOf(IGUIModule.class);
        Set<Class<? extends IGUIModeWrapper>> wrappers = reflections.getSubTypesOf(IGUIModeWrapper.class);

        IGUIModule module = null;
        IGUIModeWrapper moduleWrapper = null;
        try
        {
            for (Class<? extends IGUIModule> moduleClass : modules)
            {
                if (moduleClass.getSimpleName().equals(pluginName))
                {
                    final Class<?> classDefinition = Class.forName(moduleClass.getName());
                    final Constructor<?> constructor = classDefinition.getConstructor();

                    module = (IGUIModule) constructor.newInstance();
                    break;
                }
            }

            if (module == null) return Pair.unmodifiableOf(null, null);

            for (Class<? extends IGUIModeWrapper> wrap : wrappers)
            {
                if (wrap.getSimpleName().equals(pluginName + "ModeWrapper"))
                {
                    final Class<?> classDefinition = Class.forName(wrap.getName());
                    final Constructor<?> constructor = classDefinition.getConstructor();

                    moduleWrapper = (IGUIModeWrapper) constructor.newInstance();
                    break;
                }
            }

            return Pair.unmodifiableOf(module, moduleWrapper);
        } catch (Exception e)
        {
            throw new RuntimeException(e.getMessage());
        }
    }
}
