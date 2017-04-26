package com.net2plan.gui.plugins.networkDesign.topologyPane.jung.state;

/**
 * @author Jorge San Emeterio
 * @date 24/04/17
 */
public enum CanvasState
{
    ViewState(1),
    OSMState(2),
    SiteState(3);

    private final int code;

    CanvasState(int code)
    {
        this.code = code;
    }

    public static int getStateCode(CanvasState state)
    {
        switch (state)
        {
            case ViewState:
                return 1;
            case OSMState:
                return 2;
            case SiteState:
                return 3;
            default:
                return -1;
        }
    }

    public static CanvasState getStateName(int stateCode)
    {
        switch (stateCode)
        {
            case 1:
                return ViewState;
            case 2:
                return OSMState;
            case 3:
                return SiteState;
            default:
                return null;
        }
    }
}
