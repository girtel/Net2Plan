package com.net2plan.interfaces;

import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.Node;

import javax.swing.*;
import java.awt.*;

/**
 * @author Jorge San Emeterio
 * @date 17-Feb-17
 */
public interface ITopologyCanvasVertex
{
    Node getAssociatedNode();
    NetworkLayer getLayer();

    Paint getBorderPaint();
    void setBorderPaint(Paint p);

    Paint getFillPaint();
    void setFillPaint(Paint p);

    Font getFont();
    void setFont(Font f);
    void increaseFontSize();
    boolean decreaseFontSize();

    String getToolTip();
    String getLabel();

    Shape getShape();
    Icon getIcon();
}
