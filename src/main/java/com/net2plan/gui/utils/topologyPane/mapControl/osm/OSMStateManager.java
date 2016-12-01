package com.net2plan.gui.utils.topologyPane.mapControl.osm;

import com.net2plan.gui.utils.INetworkCallback;
import com.net2plan.gui.utils.topologyPane.TopologyPanel;
import com.net2plan.gui.utils.topologyPane.mapControl.osm.state.OSMRunningState;
import com.net2plan.gui.utils.topologyPane.mapControl.osm.state.OSMState;
import com.net2plan.gui.utils.topologyPane.mapControl.osm.state.OSMStoppedState;
import com.net2plan.internal.plugins.ITopologyCanvas;

import java.awt.geom.Point2D;

/**
 * @author Jorge San Emeterio
 * @date 01-Dec-16
 */
public final class OSMStateManager
{
    private static OSMState currentState;
    private static OSMRunningState runningState;
    private static OSMStoppedState stoppedState;

    private OSMStateManager()
    {
    }

    public static void setRunningState(final TopologyPanel topologyPanel, final ITopologyCanvas canvas, final INetworkCallback callback)
    {
        if (runningState == null)
        {
            runningState = new OSMRunningState();
            OSMMapController.startMap(topologyPanel, canvas, callback);
        }

        currentState = runningState;
    }

    public static void setStoppedState(final ITopologyCanvas canvas)
    {
        if (stoppedState == null)
        {
            stoppedState = new OSMStoppedState(canvas);
        }

        currentState = stoppedState;
    }

    public static void panTo(Point2D initialPoint, Point2D currentPoint)
    {
        currentState.panTo(initialPoint, currentPoint);
    }

    public static void zoomIn()
    {
        currentState.zoomIn();
    }

    public static void zoomOut()
    {
        currentState.zoomOut();
    }

    public static void zoomAll()
    {
        currentState.zoomAll();
    }
}
