package com.net2plan.gui.launcher;

import com.net2plan.gui.GUINet2Plan;
import com.net2plan.internal.plugins.PluginSystem;

public class GUILauncher
{
	public static void main(String[] args)
	{
		GUINet2Plan.main(args);
		//PluginSystem.addPlugin(IGUIModule.class, GUINetworkDesign.class);
		//PluginSystem.addPlugin(IGUIModule.class, GUITrafficDesign.class);
		PluginSystem.loadExternalPlugins();
		GUINet2Plan.refreshMenu();
	}
}

