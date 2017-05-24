/*******************************************************************************
 * Copyright (c) 2017 Pablo Pavon Marino and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the 2-clause BSD License 
 * which accompanies this distribution, and is available at
 * https://opensource.org/licenses/BSD-2-Clause
 *
 * Contributors:
 *     Pablo Pavon Marino and others - initial API and implementation
 *******************************************************************************/
package com.net2plan.gui.plugins.networkDesign.visualizationControl;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Paint;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.net2plan.gui.plugins.GUINetworkDesign;

/**
 * @author Jorge San Emeterio
 * @date 17-Jan-17
 */
public final class VisualizationConstants
{
    private VisualizationConstants()
    {
    }

    public final static List<Double> DEFAULT_LINKCOLORINGUTILIZATIONTHRESHOLDS = Arrays.asList (10.0, 20.0, 30.0, 40.0, 50.0, 60.0, 70.0 , 80.0, 90.0);
    public final static List<Double> DEFAULT_LINKCOLORINGRUNOUTTHRESHOLDS = Arrays.asList (2.0, 3.0, 6.0, 9.0, 12.0, 18.0, 24.0, 32.0, 38.0, 48.0);
    public final static List<Double> DEFAULT_LINKTHICKNESSTHRESHPOLDS = Arrays.asList (0.2, 0.5, 1.0, 2.5, 5.0, 10.0, 40.0, 100.0);
    public final static List<Color> DEFAULT_LINKCOLORSPERUTILIZATIONANDRUNOUT = Arrays.asList (
            new Color(25, 25, 25) , 
            new Color(0, 50, 0) , new Color(0, 75, 0) , 
            new Color(0, 100, 0) , new Color(50, 100, 0) , 
            new Color(100, 150, 0) , new Color(150, 150, 0) , 
            new Color(200, 150, 0) , new Color(200, 100, 0) , new Color(200, 50, 0) , new Color(200, 0, 0));
    public final static List<Double> DEFAULT_LINKRELATIVETHICKNESSVALUES = Arrays.asList (0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9);
    
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

    public final static BasicStroke DEFAULT_REGGUILINK_EDGESTROKE = new BasicStroke(2);
    public final static BasicStroke DEFAULT_REGGUILINK_EDGESTROKE_ACTIVELAYER = new BasicStroke(2 * INCREASELINKSTROKEFACTORACTIVE);
    public final static BasicStroke DEFAULT_REGGUILINK_EDGESTROKE_PICKED = new BasicStroke(2 * INCREASELINKSTROKEFACTORACTIVE * INCREASELINKSTROKEFACTORACTIVE);
    public final static BasicStroke DEFAULT_REGGUILINK_EDGESTROKE_PICKED_COLATERALNONACTIVELAYER = new BasicStroke(2, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, new float[]{10}, 0.0f);
    public final static BasicStroke DEFAULT_REGGUILINK_EDGESTROKE_PICKED_COLATERALACTVELAYER = new BasicStroke(2 * INCREASELINKSTROKEFACTORACTIVE, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, new float[]{10}, 0.0f);
    public final static BasicStroke DEFAULT_REGGUILINK_EDGESTROKE_BACKUP = new BasicStroke(3, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, new float[]{5}, 0.0f);
    public final static BasicStroke DEFAULT_REGGUILINK_EDGESTROKE_BACKUP_ACTIVE = new BasicStroke(3 * INCREASELINKSTROKEFACTORACTIVE, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, new float[]{10}, 0.0f);
    public final static BasicStroke DEFAULT_REGGUILINK_EDGESTROKE_BACKUP_PICKED = new BasicStroke(3 * INCREASELINKSTROKEFACTORACTIVE * INCREASELINKSTROKEFACTORACTIVE, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, new float[]{10}, 0.0f);

    public final static Paint DEFAULT_REGGUILINK_EDGECOLOR = Color.BLACK;
    public final static Paint DEFAULT_REGGUILINK_EDGECOLOR_PICKED = Color.BLUE;
    public final static Paint DEFAULT_REGGUILINK_EDGECOLOR_BACKUP = Color.YELLOW;
    public final static Paint DEFAULT_REGGUILINK_EDGECOLOR_BACKUPANDPRIMARY = new Color(160f / 255, 82f / 255, 45f / 255); // brown
    public final static Paint DEFAULT_REGGUILINK_EDGECOLOR_AFFECTEDFAILURES = Color.ORANGE;
    public final static Paint DEFAULT_REGGUILINK_EDGECOLOR_FAILED = Color.RED;

    public final static boolean DEFAULT_INTRANODEGUILINK_HASARROW = false;
    public final static BasicStroke DEFAULT_INTRANODEGUILINK_EDGESTROKE = new BasicStroke(1, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, new float[]{10}, 0.0f);
    public final static BasicStroke DEFAULT_INTRANODEGUILINK_EDGESTROKE_PICKED = new BasicStroke(1 * INCREASELINKSTROKEFACTORACTIVE, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, new float[]{10}, 0.0f);
    public final static Paint DEFAULT_INTRANODEGUILINK_EDGEDRAWCOLOR = Color.BLACK;
    public final static Paint DEFAULT_INTRANODEGUILINK_EDGEDRAWCOLOR_PICKED = Color.BLUE;
}
