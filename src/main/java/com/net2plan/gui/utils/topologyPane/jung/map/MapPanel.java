package com.net2plan.gui.utils.topologyPane.jung.map;

import com.net2plan.utils.Pair;
import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.OSMTileFactoryInfo;
import org.jxmapviewer.viewer.DefaultTileFactory;
import org.jxmapviewer.viewer.GeoPosition;
import org.jxmapviewer.viewer.Tile;
import org.jxmapviewer.viewer.TileFactoryInfo;

import javax.swing.*;

/**
 * Created by Jorge San Emeterio on 13/10/2016.
 */
public class MapPanel
{
    private final JXMapViewer mapViewer;
    private final TileFactoryInfo info;
    private final DefaultTileFactory tileFactory;

    public MapPanel()
    {
        // Background image
        mapViewer = new JXMapViewer();

        // Create a TileFactoryInfo for OpenStreetMap
        info = new OSMTileFactoryInfo();
        tileFactory = new DefaultTileFactory(info);
        mapViewer.setTileFactory(tileFactory);

        // Use 8 threads in parallel to load the tiles
        tileFactory.setThreadPoolSize(8);

        // Set the focus
        final GeoPosition USA = new GeoPosition(39.50, -98.35);

        mapViewer.setZoom(15);
        mapViewer.setAddressLocation(USA);
   }

    public JComponent getMapController()
    {
        return mapViewer;
    }

    public Pair<Double, Double> getMapCoords()
    {
        final GeoPosition centerPosition = mapViewer.getCenterPosition();

        return Pair.of(centerPosition.getLatitude(), centerPosition.getLongitude());
    }

    public void setMapCoords(final double lat, final double lon)
    {
        final GeoPosition newLoc = new GeoPosition(lat, lon);

        mapViewer.setAddressLocation(newLoc);
    }

    public void setMapZoom(final int zoom)
    {
        mapViewer.setZoom(zoom);
    }
}
