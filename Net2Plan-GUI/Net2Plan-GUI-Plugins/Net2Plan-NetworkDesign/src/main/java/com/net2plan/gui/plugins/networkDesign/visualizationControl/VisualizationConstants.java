package com.net2plan.gui.plugins.networkDesign.visualizationControl;

import com.google.common.collect.ImmutableMap;
import com.net2plan.gui.plugins.GUINetworkDesign;

import java.awt.*;
import java.net.URL;
import java.util.Map;

/**
 * @author Jorge San Emeterio
 * @date 17-Jan-17
 */
public final class VisualizationConstants
{
    private VisualizationConstants()
    {
    }

    public static Color TRANSPARENTCOLOR = new Color(0, 0, 0, 0);
    public final static int DEFAULT_ICONBORDERSIZEINPIXELS = 4;
    public final static Map<String, URL> DEFAULT_LAYERNAME2ICONURLMAP =
            ImmutableMap.of("OTN", GUINetworkDesign.class.getResource("/resources/gui/figs/OpticalTransport.png"),
                    "WDM", GUINetworkDesign.class.getResource("/resources/gui/figs/OADM.png"),
                    "IP", GUINetworkDesign.class.getResource("/resources/gui/figs/Router.png"),
                    "WIRELESS", GUINetworkDesign.class.getResource("/resources/gui/figs/WirelessRouter.png"));
    public final static Map<String, URL> DEFAULT_RESPOURCETYPE2ICONURLMAP = ImmutableMap.of(
            "CPU", GUINetworkDesign.class.getResource("/resources/gui/figs/CPU.png"),
            "RAM", GUINetworkDesign.class.getResource("/resources/gui/figs/RAM.png"),
            "HD", GUINetworkDesign.class.getResource("/resources/gui/figs/HD.png"));

    public final static float SCALE_IN = 1.1f;
    public final static float SCALE_OUT = 1 / SCALE_IN;

    public final static float INCREASENODESIZEFACTORACTIVE = 1.3f;
    public final static float INCREASENODESIZEFACTORPICKED = 1.1f;
    public final static float INCREASELINKSTROKEFACTORACTIVE = 3f;

    public final static Paint DEFAULT_GUINODE_COLOR_RESOURCE = java.awt.Color.DARK_GRAY;
    public final static Paint DEFAULT_GUINODE_COLOR_ORIGINFLOW = java.awt.Color.GREEN;
    public final static Paint DEFAULT_GUINODE_COLOR_FAILED = java.awt.Color.RED;
    public final static Paint DEFAULT_GUINODE_COLOR_ENDFLOW = java.awt.Color.CYAN;
    public final static Paint DEFAULT_GUINODE_COLOR_PICK = java.awt.Color.BLUE;
    public final static Paint DEFAULT_GUINODE_COLOR = java.awt.Color.BLACK;
    public final static Font DEFAULT_GUINODE_FONT = new Font("Helvetica", Font.BOLD, 11);
    public final static int DEFAULT_GUINODE_SHAPESIZE = 30;
    public final static int DEFAULT_GUINODE_SHAPESIZE_MORETHAN100NODES = 10;
    

    public final static boolean DEFAULT_REGGUILINK_HASARROW = false;

    public final static Stroke DEFAULT_REGGUILINK_EDGESTROKE = new BasicStroke(2);
    public final static Stroke DEFAULT_REGGUILINK_EDGESTROKE_ACTIVELAYER = new BasicStroke(2 * INCREASELINKSTROKEFACTORACTIVE);
    public final static Stroke DEFAULT_REGGUILINK_EDGESTROKE_PICKED = new BasicStroke(2 * INCREASELINKSTROKEFACTORACTIVE * INCREASELINKSTROKEFACTORACTIVE);
    public final static Stroke DEFAULT_REGGUILINK_EDGESTROKE_PICKED_COLATERALNONACTIVELAYER = new BasicStroke(2, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, new float[]{10}, 0.0f);
    public final static Stroke DEFAULT_REGGUILINK_EDGESTROKE_PICKED_COLATERALACTVELAYER = new BasicStroke(2 * INCREASELINKSTROKEFACTORACTIVE, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, new float[]{10}, 0.0f);
    public final static Stroke DEFAULT_REGGUILINK_EDGESTROKE_BACKUP = new BasicStroke(3, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, new float[]{5}, 0.0f);
    public final static Stroke DEFAULT_REGGUILINK_EDGESTROKE_BACKUP_ACTIVE = new BasicStroke(3 * INCREASELINKSTROKEFACTORACTIVE, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, new float[]{10}, 0.0f);
    public final static Stroke DEFAULT_REGGUILINK_EDGESTROKE_BACKUP_PICKED = new BasicStroke(3 * INCREASELINKSTROKEFACTORACTIVE * INCREASELINKSTROKEFACTORACTIVE, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, new float[]{10}, 0.0f);

    public final static Paint DEFAULT_REGGUILINK_EDGECOLOR = Color.BLACK;
    public final static Paint DEFAULT_REGGUILINK_EDGECOLOR_PICKED = Color.BLUE;
    public final static Paint DEFAULT_REGGUILINK_EDGECOLOR_BACKUP = Color.YELLOW;
    public final static Paint DEFAULT_REGGUILINK_EDGECOLOR_BACKUPANDPRIMARY = new Color(160f / 255, 82f / 255, 45f / 255); // brown
    public final static Paint DEFAULT_REGGUILINK_EDGECOLOR_AFFECTEDFAILURES = Color.ORANGE;
    public final static Paint DEFAULT_REGGUILINK_EDGECOLOR_FAILED = Color.RED;

    public final static boolean DEFAULT_INTRANODEGUILINK_HASARROW = false;
    public final static Stroke DEFAULT_INTRANODEGUILINK_EDGESTROKE = new BasicStroke(1, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, new float[]{10}, 0.0f);
    public final static Stroke DEFAULT_INTRANODEGUILINK_EDGESTROKE_PICKED = new BasicStroke(1 * INCREASELINKSTROKEFACTORACTIVE, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, new float[]{10}, 0.0f);
    public final static Paint DEFAULT_INTRANODEGUILINK_EDGEDRAWCOLOR = Color.BLACK;
    public final static Paint DEFAULT_INTRANODEGUILINK_EDGEDRAWCOLOR_PICKED = Color.BLUE;
}
