package com.net2plan.gui.launcher.wrapper;

import java.util.Map;

/**
 * Created by Jorge San Emeterio on 19/03/2017.
 */
public class GUITrafficDesignWrapper implements IGUIPluginWrapper
{
    private Map<String, String> parameters;

    @Override
    public void launchMode(int mode, Map<String, String> parameters)
    {
        this.parameters = parameters;

        switch (mode)
        {
            default:
                System.err.println("Unknown mode: " + mode + " in wrapper: " + this.getClass().getName());
            case 1:
                return;
        }
    }
}
