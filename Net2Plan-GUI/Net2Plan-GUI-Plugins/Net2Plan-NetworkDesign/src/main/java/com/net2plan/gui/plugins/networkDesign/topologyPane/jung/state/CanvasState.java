package com.net2plan.gui.plugins.networkDesign.topologyPane.jung.state;

/**
 * @author Jorge San Emeterio
 * @date 24/04/17
 */
public enum CanvasState
{
    ViewState, OSMState, SiteState;

    public static CanvasState getStateName(ICanvasState state)
    {
        if (state instanceof ViewState)
        {
            return ViewState;
        } else if (state instanceof OSMState)
        {
            return OSMState;
        } else if (state instanceof SiteState)
        {
            return SiteState;
        } else
        {
            throw new RuntimeException();
        }
    }
}
