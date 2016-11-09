package com.net2plan.gui.utils.topologyPane.jung.map.osm;

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
    private static int imageID = 0;

    protected final TileFactoryInfo info;
    protected final DefaultTileFactory tileFactory;

    private final int NUMBER_OF_THREADS = 8;

    public OSMMapPanel()
    {
        // Create a TileFactoryInfo for OpenStreetMap
        info = new OSMTileFactoryInfo();
        tileFactory = new DefaultTileFactory(info);

        this.setTileFactory(tileFactory);

        // Use 8 threads in parallel to load the tiles
        tileFactory.setThreadPoolSize(NUMBER_OF_THREADS);
    }

    private void setDefaultPosition()
    {
        // Default position
        final GeoPosition europe = new GeoPosition(47.20, 25.2);

        this.setZoom(16);
        this.setCenterPosition(europe);
    }

    public File saveMap(final int width, final int height)
    {
        // Creating data folder
        final File parent = new File("data/bg_maps");

        if (!parent.exists())
        {
            parent.mkdirs();
        }

        // It is necessary to save each map on their own file.
        // If we only use one, the map is never updated.
        final File f = new File(parent, "background_map_" + (imageID++) + ".png");
        f.deleteOnExit();

        // Saving map
        try
        {
            final BufferedImage im = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

            this.paint(im.getGraphics());

            ImageIO.write(im, "PNG", f);
        } catch (IOException e)
        {
            e.printStackTrace();
        }

        return f;
    }
}
