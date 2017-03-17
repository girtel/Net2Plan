package com.net2plan.gui.launcher;

import com.net2plan.gui.GUINet2Plan;
import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.gui.plugins.GUITrafficDesign;
import com.net2plan.internal.plugins.IGUIModule;
import com.net2plan.internal.plugins.PluginSystem;
import org.apache.commons.cli.*;

import java.awt.*;
import java.awt.event.KeyEvent;

/**
 * Created by Jorge San Emeterio on 17/03/17.
 */
public class GUIPluginLauncher
{
    private enum Plugin
    {
        NetworkDesign,
        TrafficDesign,
        Unknown;

        public static Plugin parseText(String text)
        {
            switch (text.toLowerCase())
            {
                case "networkdesign":
                case "guinetworkdesign":
                    return NetworkDesign;
                case "trafficdesign":
                case "guitrafficdesign":
                    return NetworkDesign;
                default:
                    return Unknown;
            }
        }
    }

    public static void main(String[] args)
    {
        Options options = null;
        CommandLineParser parser = null;
        HelpFormatter formatter = null;
        try
        {
            // Parse input parameter
            options = new Options();

            final Option input = new Option("p", "plugin", true, "Plugin to be launched");
            input.setRequired(true);

            options.addOption(input);

            parser = new DefaultParser();
            formatter = new HelpFormatter();

            final CommandLine cmd = parser.parse(options, args);
            final String inputPlugin = cmd.getOptionValue("plugin");

            final Plugin plugin = Plugin.parseText(inputPlugin);

            if (plugin == Plugin.Unknown)
            {
                // TODO: Build message
                throw new ParseException("Unknown plugin: " + inputPlugin);
            }

            final Class<? extends IGUIModule> toLoadClass;
            switch (plugin)
            {
                case NetworkDesign:
                    toLoadClass = GUINetworkDesign.class;
                    break;
                case TrafficDesign:
                    toLoadClass = GUITrafficDesign.class;
                    break;
                default:
                case Unknown:
                    throw new RuntimeException();
            }

            GUINet2Plan.main(args);
            PluginSystem.addPlugin(IGUIModule.class, toLoadClass);
            GUINet2Plan.refreshMenu();

            runPlugin();
        } catch (ParseException e)
        {
            System.out.println(e.getMessage());
            formatter.printHelp("utility-name", options);

            System.exit(0);
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private static void runPlugin() throws AWTException
    {
        final Robot robot = new Robot();
        robot.setAutoDelay(40);
        robot.setAutoWaitForIdle(true);

        robot.keyPress(KeyEvent.VK_ALT);
        robot.keyPress(KeyEvent.VK_1);

        robot.delay(1000);

        robot.keyRelease(KeyEvent.VK_1);
        robot.keyRelease(KeyEvent.VK_ALT);

//        robot.keyPress(KeyEvent.VK_ALT);
//        robot.keyPress(KeyEvent.VK_F4);
//        robot.keyRelease(KeyEvent.VK_F4);
//        robot.keyRelease(KeyEvent.VK_ALT);
//
//        robot.keyPress(KeyEvent.VK_CONTROL);
//        robot.keyPress(KeyEvent.VK_O);
//
//        robot.keyRelease(KeyEvent.VK_O);
//        robot.keyRelease(KeyEvent.VK_CONTROL);
    }


}
