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

    public final static Paint DEFAULT_GUINODE_DRAWCOLOR = java.awt.Color.BLACK;
    public final static Paint DEFAULT_GUINODE_FILLCOLOR = java.awt.Color.BLACK;
    public final static Paint DEFAULT_GUINODE_FILLCOLOR_PICKED = java.awt.Color.BLACK;
    public final static Font DEFAULT_GUINODE_FONT = new Font("Helvetica", Font.BOLD, 11);
    public final static int DEFAULT_GUINODE_SHAPESIZE = 30;
    public final static Shape DEFAULT_GUINODE_SHAPE = new Ellipse2D.Double(-1 * DEFAULT_GUINODE_SHAPESIZE / 2, -1 * DEFAULT_GUINODE_SHAPESIZE / 2, 1 * DEFAULT_GUINODE_SHAPESIZE, 1 * DEFAULT_GUINODE_SHAPESIZE);
    public final static Shape DEFAULT_GUINODE_SHAPE_PICKED = new Ellipse2D.Double(-1.2 * DEFAULT_GUINODE_SHAPESIZE / 2, -1.2 * DEFAULT_GUINODE_SHAPESIZE / 2, 1.2 * DEFAULT_GUINODE_SHAPESIZE, 1.2 * DEFAULT_GUINODE_SHAPESIZE);

    public final static boolean DEFAULT_REGGUILINK_HASARROW = false;
    public final static Stroke DEFAULT_REGGUILINK_ARROWSTROKE = new BasicStroke(1);
    public final static Stroke DEFAULT_REGGUILINK_ARROWSTROKE_PICKED = new BasicStroke(2);
    public final static Stroke DEFAULT_REGGUILINK_EDGETROKE = new BasicStroke(3);
    public final static Stroke DEFAULT_REGGUILINK_EDGESTROKE_PICKED = new BasicStroke(5);
    public final static Paint DEFAULT_REGGUILINK_ARROWDRAWCOLOR = Color.BLACK;
    public final static Paint DEFAULT_REGGUILINK_ARROWDRAWCOLOR_PICKED = Color.BLUE;
    public final static Paint DEFAULT_REGGUILINK_ARROWFILLCOLOR = Color.BLACK;
    public final static Paint DEFAULT_REGGUILINK_EDGEDRAWCOLOR = Color.BLACK;
    public final static Paint DEFAULT_REGGUILINK_EDGEDRAWCOLOR_PICKED = Color.BLUE;
    public final static Stroke DEFAULT_REGGUILINK_EDGESTROKE_BACKUP = new BasicStroke(1, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, new float[] { 10 }, 0.0f);
    public final static Stroke DEFAULT_REGGUILINK_EDGESTROKE_BACKUP_PICKED = new BasicStroke(2, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, new float[] { 10 }, 0.0f);

    public final static boolean DEFAULT_INTRANODEGUILINK_HASARROW = false;
    public final static Stroke DEFAULT_INTRANODEGUILINK_ARROWSTROKE = new BasicStroke(0.5f);
    public final static Stroke DEFAULT_INTRANODEGUILINK_ARROWSTROKE_PICKED = new BasicStroke(2);
    public final static Stroke DEFAULT_INTRANODEGUILINK_EDGETROKE = new BasicStroke(1.5f);
    public final static Stroke DEFAULT_INTRANODEGUILINK_EDGESTROKE_PICKED = new BasicStroke(2.5f);
    public final static Paint DEFAULT_INTRANODEGUILINK_ARROWDRAWCOLOR = Color.BLACK;
    public final static Paint DEFAULT_INTRANODEGUILINK_ARROWDRAWCOLOR_PICKED = Color.BLUE;
    public final static Paint DEFAULT_INTRANODEGUILINK_ARROWFILLCOLOR = Color.BLACK;
    public final static Paint DEFAULT_INTRANODEGUILINK_EDGEDRAWCOLOR = Color.BLACK;
    public final static Paint DEFAULT_INTRANODEGUILINK_EDGEDRAWCOLOR_PICKED = Color.BLUE;
    public final static Stroke DEFAULT_REGGUILINK_INTRANODEGESTROKE_BACKUP = new BasicStroke(0.5f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, new float[] { 10 }, 0.0f);
    public final static Stroke DEFAULT_REGGUILINK_INTRANODEGESTROKE_BACKUP_PICKED = new BasicStroke(1, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, new float[] { 10 }, 0.0f);
}
