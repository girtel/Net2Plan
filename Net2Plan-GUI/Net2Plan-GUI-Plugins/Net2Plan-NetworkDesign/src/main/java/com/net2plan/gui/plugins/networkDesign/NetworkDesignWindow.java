package com.net2plan.gui.plugins.networkDesign;

/**
 * Created by Jorge San Emeterio on 7/03/17.
 */
public enum NetworkDesignWindow
{
    network(NetworkDesignWindow.networkWindowName),
    offline(NetworkDesignWindow.offlineWindowName),
    online(NetworkDesignWindow.onlineWindowName),
    whatif(NetworkDesignWindow.whatifWindowName),
    report(NetworkDesignWindow.reportWindowName);

    private final static String networkWindowName = "View/Edit network state";
    private final static String offlineWindowName = "Offline algorithms";
    private final static String onlineWindowName = "Online simulation";
    private final static String whatifWindowName = "What-if analysis";
    private final static String reportWindowName = "View reports";

    private final String text;

    NetworkDesignWindow(final String text)
    {
        this.text = text;
    }

    public static NetworkDesignWindow parseString(final String text)
    {
        switch (text)
        {
            case NetworkDesignWindow.networkWindowName:
                return network;
            case NetworkDesignWindow.offlineWindowName:
                return offline;
            case NetworkDesignWindow.onlineWindowName:
                return online;
            case NetworkDesignWindow.whatifWindowName:
                return whatif;
            case NetworkDesignWindow.reportWindowName:
                return report;
        }

        return null;
    }

    public static String getTabName(final NetworkDesignWindow tab)
    {
        switch (tab)
        {
            case network:
                return NetworkDesignWindow.networkWindowName;
            case offline:
                return NetworkDesignWindow.offlineWindowName;
            case online:
                return NetworkDesignWindow.onlineWindowName;
            case whatif:
                return NetworkDesignWindow.whatifWindowName;
            case report:
                return NetworkDesignWindow.reportWindowName;
        }

        return null;
    }
}
