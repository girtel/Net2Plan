package com.net2plan.gui.utils.topologyPane;

import java.awt.*;
import java.awt.geom.Ellipse2D;

/**
 * @author Jorge San Emeterio
 * @date 17-Jan-17
 */
public final class VisualizationConstants
{
    private VisualizationConstants() {}

    public final static float SCALE_IN = 1.1f;
    public final static float SCALE_OUT = 1 / SCALE_IN;

    public final static float INCREASENODESIZEFACTORACTIVE = 1.3f;
    public final static float INCREASENODESIZEFACTORPICKED = 1.1f;
    public final static float INCREASELINKSTROKEFACTORACTIVE = 3f;

    public final static Paint DEFAULT_GUINODE_COLOR_ORIGINFLOW = java.awt.Color.GREEN;
    public final static Paint DEFAULT_GUINODE_COLOR_ENDFLOW = java.awt.Color.CYAN;
    public final static Paint DEFAULT_GUINODE_COLOR_PICK = java.awt.Color.BLUE;
    public final static Paint DEFAULT_GUINODE_COLOR = java.awt.Color.BLACK;
    public final static Font DEFAULT_GUINODE_FONT = new Font("Helvetica", Font.BOLD, 11);
    public final static int DEFAULT_GUINODE_SHAPESIZE = 30;
    public final static Shape DEFAULT_GUINODE_SHAPE = new Ellipse2D.Double(-1 * DEFAULT_GUINODE_SHAPESIZE / 2, -1 * DEFAULT_GUINODE_SHAPESIZE / 2, 1 * DEFAULT_GUINODE_SHAPESIZE, 1 * DEFAULT_GUINODE_SHAPESIZE);

    public final static boolean DEFAULT_REGGUILINK_HASARROW = false;
//    public final static Stroke DEFAULT_REGGUILINK_ARROWSTROKE = new BasicStroke(1);
//    public final static Stroke DEFAULT_REGGUILINK_ARROWSTROKE_ACTIVELAYER = new BasicStroke(1 * INCREASELINKSTROKEFACTORACTIVE);
//    public final static Stroke DEFAULT_REGGUILINK_ARROWSTROKE_PICKED = new BasicStroke(1 * INCREASELINKSTROKEFACTORACTIVE * INCREASELINKSTROKEFACTORACTIVE);
//    public final static Paint DEFAULT_REGGUILINK_ARROWDRAWCOLOR = Color.BLACK;
//    public final static Paint DEFAULT_REGGUILINK_ARROWDRAWCOLOR_PICKED = Color.BLUE;
//    public final static Paint DEFAULT_REGGUILINK_ARROWFILLCOLOR = Color.BLACK;

    public final static Stroke DEFAULT_REGGUILINK_EDGESTROKE = new BasicStroke(2);
    public final static Stroke DEFAULT_REGGUILINK_EDGESTROKE_ACTIVELAYER = new BasicStroke(2 * INCREASELINKSTROKEFACTORACTIVE);
    public final static Stroke DEFAULT_REGGUILINK_EDGESTROKE_PICKED = new BasicStroke(2 * INCREASELINKSTROKEFACTORACTIVE * INCREASELINKSTROKEFACTORACTIVE);
    public final static Stroke DEFAULT_REGGUILINK_EDGESTROKE_PICKED_COLATERALNONACTIVELAYER = new BasicStroke(2, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, new float[] { 10 }, 0.0f);
    public final static Stroke DEFAULT_REGGUILINK_EDGESTROKE_PICKED_COLATERALACTVELAYER = new BasicStroke(2 * INCREASELINKSTROKEFACTORACTIVE, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, new float[] { 10 }, 0.0f);
    public final static Stroke DEFAULT_REGGUILINK_EDGESTROKE_BACKUP = new BasicStroke(3, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, new float[] { 5 }, 0.0f);
    public final static Stroke DEFAULT_REGGUILINK_EDGESTROKE_BACKUP_ACTIVE = new BasicStroke(3 * INCREASELINKSTROKEFACTORACTIVE, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, new float[] { 10 }, 0.0f);
    public final static Stroke DEFAULT_REGGUILINK_EDGESTROKE_BACKUP_PICKED = new BasicStroke(3 * INCREASELINKSTROKEFACTORACTIVE * INCREASELINKSTROKEFACTORACTIVE, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, new float[] { 10 }, 0.0f);

    public final static Paint DEFAULT_REGGUILINK_EDGECOLOR = Color.BLACK;
    public final static Paint DEFAULT_REGGUILINK_EDGECOLOR_PICKED = Color.BLUE;
    public final static Paint DEFAULT_REGGUILINK_EDGECOLOR_BACKUP = Color.YELLOW;
    public final static Paint DEFAULT_REGGUILINK_EDGECOLOR_BACKUPANDPRIMARY = new Color (160f/255,82f/255,45f/255); // brown
    public final static Paint DEFAULT_REGGUILINK_EDGECOLOR_AFFECTEDFAILURES = Color.ORANGE;
    public final static Paint DEFAULT_REGGUILINK_EDGECOLOR_FAILED = Color.RED;

    public final static boolean DEFAULT_INTRANODEGUILINK_HASARROW = false;
//    public final static Stroke DEFAULT_INTRANODEGUILINK_ARROWSTROKE = new BasicStroke(0.5f);
//    public final static Stroke DEFAULT_INTRANODEGUILINK_ARROWSTROKE_ACTIVE = new BasicStroke(0.5f * INCREASELINKSTROKEFACTORACTIVE);
//    public final static Stroke DEFAULT_INTRANODEGUILINK_ARROWSTROKE_PICKED = new BasicStroke(0.5f * INCREASELINKSTROKEFACTORACTIVE * INCREASELINKSTROKEFACTORACTIVE);
    public final static Stroke DEFAULT_INTRANODEGUILINK_EDGESTROKE = new BasicStroke(1, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, new float[] { 10 }, 0.0f);
    public final static Stroke DEFAULT_INTRANODEGUILINK_EDGESTROKE_ACTIVE = new BasicStroke(1 * INCREASELINKSTROKEFACTORACTIVE, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, new float[] { 10 }, 0.0f);
    public final static Stroke DEFAULT_INTRANODEGUILINK_EDGESTROKE_PICKED = new BasicStroke(1 * INCREASELINKSTROKEFACTORACTIVE * INCREASELINKSTROKEFACTORACTIVE, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, new float[] { 10 }, 0.0f);
//    public final static Stroke DEFAULT_REGGUILINK_INTRANODEGESTROKE_BACKUP = new BasicStroke(0.5f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, new float[] { 10 }, 0.0f);
//    public final static Stroke DEFAULT_REGGUILINK_INTRANODEGESTROKE_BACKUP_PICKED = new BasicStroke(1, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, new float[] { 10 }, 0.0f);
//    public final static Paint DEFAULT_INTRANODEGUILINK_ARROWDRAWCOLOR = Color.BLACK;
//    public final static Paint DEFAULT_INTRANODEGUILINK_ARROWDRAWCOLOR_PICKED = Color.BLUE;
//    public final static Paint DEFAULT_INTRANODEGUILINK_ARROWFILLCOLOR = Color.BLACK;
    public final static Paint DEFAULT_INTRANODEGUILINK_EDGEDRAWCOLOR = Color.BLACK;
    public final static Paint DEFAULT_INTRANODEGUILINK_EDGEDRAWCOLOR_PICKED = Color.BLUE;
}
