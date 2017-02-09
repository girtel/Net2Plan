package com.net2plan.gui.utils.topologyPane.jung.osmSupport.state;

import com.net2plan.internal.ErrorHandling;

/**
 * @author Jorge San Emeterio
 * @date 09-Feb-17
 */
public class OSMException extends RuntimeException
{
    public OSMException(final String message)
    {
        ErrorHandling.showErrorDialog(message, "Could not display OSM");
    }

    public OSMException(final String message, final String title)
    {
        ErrorHandling.showErrorDialog(message, title);
    }
}
