package com.net2plan.gui.utils.topologyPane.jung.map.google;

import com.net2plan.gui.utils.INetworkCallback;
import com.net2plan.gui.utils.topologyPane.GUILink;
import com.net2plan.gui.utils.topologyPane.GUINode;
import com.net2plan.gui.utils.topologyPane.TopologyPanel;
import com.net2plan.gui.utils.topologyPane.jung.JUNGCanvas;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.internal.plugins.ITopologyCanvas;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import org.jxmapviewer.viewer.GeoPosition;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.List;

/**
 * Created by Jorge on 11/4/2016.
 */
public class GoogleMapController
{
    private static final int IMAGE_MAX_WIDTH = 640;
    private static final int IMAGE_MAX_HEIGHT = 640;

    private static final String API_KEY = "AIzaSyDXJnakqhm9N8yOisbD2FNj0_NObXi-5n4";

    private static TopologyPanel topologyPanel;
    private static ITopologyCanvas canvas;
    private static INetworkCallback callback;

    // Non-instanciable. Static class
    private GoogleMapController()
    {
    }

    public static void runMap(final TopologyPanel topologyPanel, final ITopologyCanvas canvas, final INetworkCallback callback)
    {
        // Topology elements. Complete Net2Plan information.
        GoogleMapController.topologyPanel = topologyPanel;
        GoogleMapController.canvas = canvas;
        GoogleMapController.callback = callback;

        // Proceed to load map
        loadMap();
    }

    private static void loadMap()
    {
        topologyPanel.zoomAll();

        final Point2D topologyCenter = getTopologyCenter();
        try
        {
            final File file = downloadImage(buildURL(topologyCenter.getY(), topologyCenter.getX(), 10));
            final BufferedImage image = ImageIO.read(file);

            // Positioning the image on the center of the screen
            final Point2D imageCenter = new Point2D.Double(image.getWidth()/2, image.getHeight()/2);

            final int screenWidth = canvas.getComponent().getWidth();
            final int screenHeight = canvas.getComponent().getHeight();

            ((JUNGCanvas) canvas).setBackgroundImage(file, (imageCenter.getX() - screenWidth)/2, imageCenter.getY() - screenHeight);
        } catch (MalformedURLException e)
        {
            e.printStackTrace();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private static File downloadImage(final URL url)
    {
        final File parentFile = new File("data/bg_maps");

        if (!parentFile.exists())
        {
            parentFile.mkdirs();
        }

        File file = new File(parentFile, "map.png");
        Image image = null;
        try
        {
            image = ImageIO.read(url);

            ImageIO.write((BufferedImage) image, "png", file);
        } catch (IOException e)
        {
            e.printStackTrace();
        }

        return file;
    }

    private static URL buildURL(final double latitude, final double longitude, final int zoom) throws MalformedURLException
    {
        final String header = "https://maps.googleapis.com/maps/api/staticmap?";

        final StringBuilder builder = new StringBuilder();
        builder.append(header);
        builder.append("center=" + latitude + "," + longitude);
        builder.append("&zoom=" + zoom);
        builder.append("&size=" + IMAGE_MAX_WIDTH + "x" + IMAGE_MAX_HEIGHT);
        builder.append("&key=" + API_KEY);

        final URL url = new URL(builder.toString());

        return url;
    }

    public static Point2D getTopologyCenter()
    {
        final List<Node> nodes = callback.getDesign().getNodes();

        double x = 0;
        double y = 0;

        for (Node node : nodes)
        {
            final Point2D positionMap = node.getXYPositionMap();

            x += positionMap.getX();
            y += positionMap.getY();
        }

        return new Point2D.Double(x/nodes.size(), y/nodes.size());
    }
}
