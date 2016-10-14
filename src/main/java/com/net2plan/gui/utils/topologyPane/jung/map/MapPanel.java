package com.net2plan.gui.utils.topologyPane.jung.map;

import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.Node;
import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.OSMTileFactoryInfo;
import org.jxmapviewer.input.CenterMapListener;
import org.jxmapviewer.input.PanKeyListener;
import org.jxmapviewer.input.PanMouseInputListener;
import org.jxmapviewer.input.ZoomMouseWheelListenerCursor;
import org.jxmapviewer.viewer.DefaultTileFactory;
import org.jxmapviewer.viewer.GeoPosition;
import org.jxmapviewer.viewer.TileFactoryInfo;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.MouseInputListener;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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

        // Add interactions
        MouseInputListener mia = new PanMouseInputListener(mapViewer);
        mapViewer.addMouseListener(mia);
        mapViewer.addMouseMotionListener(mia);

        mapViewer.addMouseListener(new CenterMapListener(mapViewer));

        mapViewer.addMouseWheelListener(new ZoomMouseWheelListenerCursor(mapViewer));

        mapViewer.addKeyListener(new PanKeyListener(mapViewer));

        // Use 8 threads in parallel to load the tiles
        tileFactory.setThreadPoolSize(8);
        // Set the focus
        final GeoPosition europe = new GeoPosition(47.20, 25.2);

        mapViewer.setZoom(15);
        mapViewer.setAddressLocation(europe);
    }

    public JComponent getMapComponent()
    {
        return mapViewer;
    }

    public Point2D getMapCoords()
    {
        return mapViewer.getCenter();
    }

    public void setMapZoom(final int zoom)
    {
        mapViewer.setZoom(zoom);
    }

    public void centerMap(final NetPlan netPlan)
    {
        centerMap(netPlan.getNodes());
    }

    public void centerMap(final List<Node> nodes)
    {
        final Point2D topologyCenter = getTopologyCenter(nodes);

        mapViewer.setCenter(topologyCenter);
        mapViewer.repaint();
    }

    public Point2D getTopologyCenter(final List<Node> nodes)
    {
        final List<Point2D> knots = nodes.stream().map(node -> node.getXYPositionMap()).collect(Collectors.toList());

        double centroidX = 0, centroidY = 0;

        for (Point2D knot : knots)
        {
            centroidX += knot.getX();
            centroidY += knot.getY();
        }
        return new Point2D.Double(centroidX / knots.size(), centroidY / knots.size());
    }

    public File saveMap()
    {
        File f = new File("background_map.png");
        f.deleteOnExit();

        try
        {
            BufferedImage im = new BufferedImage(mapViewer.getWidth(), mapViewer.getHeight(), BufferedImage.TYPE_INT_ARGB);
            mapViewer.paint(im.getGraphics());
            ImageIO.write(im, "PNG", f);
        } catch (IOException e)
        {
            e.printStackTrace();
        }

        // Showing them up again
        Arrays.stream(mapViewer.getComponents()).forEach(component -> component.setVisible(true));

        return f;
    }
}
