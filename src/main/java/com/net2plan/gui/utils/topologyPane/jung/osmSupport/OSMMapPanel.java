package com.net2plan.gui.utils.topologyPane.jung.osmSupport;

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.OSMTileFactoryInfo;
import org.jxmapviewer.viewer.*;

/**
 * Created by Jorge San Emeterio on 13/10/2016.
 */
public class OSMMapPanel extends JXMapViewer
{
    private static final int NUMBER_OF_THREADS = 8;
    private static final GeoPosition europe = new GeoPosition(47.20, 25.2);

    private final GeoPosition defaultPosition;
    private final int defaultZoom;

    public OSMMapPanel()
    {
        // Create a TileFactoryInfo for OpenStreetMap
        final TileFactoryInfo info = new OSMTileFactoryInfo();
        final DefaultTileFactory tileFactory = new DefaultTileFactory(info);

        this.setTileFactory(tileFactory);

        // Use 8 threads in parallel to load the tiles
        tileFactory.setThreadPoolSize(NUMBER_OF_THREADS);

        this.defaultPosition = europe;
        this.defaultZoom = 16;
    }

    public void moveToDefaultPosition()
    {
        this.setCenterPosition(defaultPosition);
    }

    public void returnToDefaultZoom()
    {
        this.setZoom(defaultZoom);
    }

    public GeoPosition getDefaultPosition()
    {
        return defaultPosition;
    }

    public int getDefaultZoom()
    {
        return defaultZoom;
    }
}
