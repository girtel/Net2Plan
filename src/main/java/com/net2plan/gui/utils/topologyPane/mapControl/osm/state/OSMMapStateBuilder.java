package com.net2plan.gui.utils.topologyPane.mapControl.osm.state;

import com.net2plan.gui.utils.INetworkCallback;
import com.net2plan.gui.utils.topologyPane.TopologyPanel;
import com.net2plan.gui.utils.topologyPane.mapControl.osm.OSMMapController;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.internal.plugins.ITopologyCanvas;

import java.awt.geom.Point2D;

/**
 * @author Jorge San Emeterio
 * @date 22-Dec-16
 */
public enum OSMMapStateBuilder
{
    INSTANCE;

    private OSMStateManager stateManager;

    private void build(final SingletonBuilder builder) throws Net2PlanException
    {
        final TopologyPanel topologyPanel = builder.topologyPanel;
        final INetworkCallback callback = builder.callback;

        if (topologyPanel == null || topologyPanel.getCanvas() == null || callback == null)
            throw new Net2PlanException("GUI component not correctly initialized before running state machine.");

        this.stateManager = new OSMStateManager(topologyPanel, topologyPanel.getCanvas(), callback);
    }

    public static OSMStateManager getSingleton()
    {
        return INSTANCE.stateManager;
    }

    public class OSMStateManager
    {
        private OSMState currentState;
        private final OSMRunningState runningState;
        private final OSMStoppedState stoppedState;

        private final TopologyPanel topologyPanel;
        private final ITopologyCanvas canvas;
        private final INetworkCallback callback;

        private final OSMMapController mapController;

        private OSMStateManager(final TopologyPanel topologyPanel, final ITopologyCanvas canvas, final INetworkCallback callback)
        {
            this.topologyPanel = topologyPanel;
            this.canvas = canvas;
            this.callback = callback;
            this.mapController = new OSMMapController();

            runningState = new OSMRunningState(mapController);
            stoppedState = new OSMStoppedState(canvas);
            currentState = stoppedState;
        }

        public void setRunningState()
        {
            currentState = runningState;
            mapController.startMap(topologyPanel, canvas, callback);
        }

        public void setStoppedState()
        {
            currentState = stoppedState;
            mapController.cleanMap();
        }

        public void panTo(final Point2D initialPoint, final Point2D currentPoint)
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

        public void addNode(final NetPlan netPlan, final String name, final Point2D pos)
        {
            currentState.addNode(topologyPanel, netPlan, name, pos);
        }

        public void moveNode(final Node node, final Point2D pos)
        {
            currentState.moveNode(callback, canvas, node, pos);
        }

        public OSMState getCurrentState()
        {
            return currentState;
        }

        public void takeSnapshot(final ITopologyCanvas canvas)
        {
            currentState.takeSnapshot(canvas);
        }
    }

    public static class SingletonBuilder
    {
        private final TopologyPanel topologyPanel;
        private final INetworkCallback callback;

        public SingletonBuilder(TopologyPanel topologyPanel, INetworkCallback callback)
        {
            this.topologyPanel = topologyPanel;
            this.callback = callback;
        }

        public void build()
        {
            OSMMapStateBuilder.INSTANCE.build(this);
        }
    }
}
