package com.net2plan.gui.utils.topologyPane.mapControl.osm.state;

import com.net2plan.gui.utils.IVisualizationCallback;
import com.net2plan.gui.utils.topologyPane.TopologyPanel;
import com.net2plan.interfaces.networkDesign.Net2PlanException;

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
        final IVisualizationCallback callback = builder.callback;

        if (topologyPanel == null || topologyPanel.getCanvas() == null || callback == null)
            throw new Net2PlanException("GUI component not correctly initialized before running state machine.");

        this.stateManager = new OSMStateManager(callback, topologyPanel, topologyPanel.getCanvas());
    }

    public static OSMStateManager getSingleton()
    {
        return INSTANCE.stateManager;
    }

    public static class SingletonBuilder
    {
        private final TopologyPanel topologyPanel;
        private final IVisualizationCallback callback;

        public SingletonBuilder(TopologyPanel topologyPanel, IVisualizationCallback callback)
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
