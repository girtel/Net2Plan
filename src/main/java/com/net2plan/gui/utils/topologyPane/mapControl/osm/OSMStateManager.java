package com.net2plan.gui.utils.topologyPane.mapControl.osm;

import com.net2plan.gui.utils.topologyPane.mapControl.osm.state.OSMRunningState;
import com.net2plan.gui.utils.topologyPane.mapControl.osm.state.OSMState;
import com.net2plan.gui.utils.topologyPane.mapControl.osm.state.OSMStoppedState;

import java.awt.geom.Point2D;

/**
 * @author Jorge San Emeterio
 * @date 01-Dec-16
 */
public class OSMStateManager
{
    private OSMState currentState;
    private OSMRunningState runningState;
    private OSMStoppedState stoppedState;

    public OSMStateManager()
    {
        runningState = new OSMRunningState();
        stoppedState = new OSMStoppedState();
        currentState = stoppedState;
    }

    public void setRunningState()
    {
        currentState = runningState;
    }

    public void setStoppedState()
    {
        currentState = stoppedState;
    }

    public void panTo(Point2D initialPoint, Point2D currentPoint)
    {
        currentState.panTo(initialPoint, currentPoint);
    }

    public void zoomIn()
    {
        currentState.zoomIn();
    }

    public void zoomOut()
    {
        currentState.zoomOut();
    }

    public void zoomAll()
    {
        currentState.zoomAll();
    }
}
