package com.net2plan.gui.utils.topologyPane.components.mapPanel;

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.OSMTileFactoryInfo;
import org.jxmapviewer.viewer.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Created by Jorge San Emeterio on 13/10/2016.
 */
public class OSMMapPanel extends JXMapViewer
{
    private static final GeoPosition defaultPosition = new GeoPosition(47.20, 25.2);
    private static final int defaultZoom = 16;
    private static final int NUMBER_OF_THREADS = 8;

    public OSMMapPanel()
    {
        // Create a TileFactoryInfo for OpenStreetMap
        final TileFactoryInfo info = new OSMTileFactoryInfo();
        final DefaultTileFactory tileFactory = new DefaultTileFactory(info);

        this.setTileFactory(tileFactory);

        // Use 8 threads in parallel to load the tiles
        tileFactory.setThreadPoolSize(NUMBER_OF_THREADS);
    }

    public void moveToDefaultPosition()
    {
        // Default position
        this.setZoom(defaultZoom);
        this.setCenterPosition(defaultPosition);
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
