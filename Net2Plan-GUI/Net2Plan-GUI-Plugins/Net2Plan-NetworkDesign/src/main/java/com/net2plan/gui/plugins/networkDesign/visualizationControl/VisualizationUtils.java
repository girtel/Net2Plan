package com.net2plan.gui.plugins.networkDesign.visualizationControl;

import java.awt.*;

/**
 * @author Jorge San Emeterio
 * @date 28/04/17
 */
public final class VisualizationUtils
{
    private VisualizationUtils() {}

    static BasicStroke resizedBasicStroke(BasicStroke a, float multFactorSize)
    {
        if (multFactorSize == 1) return a;
        return new BasicStroke(a.getLineWidth() * multFactorSize, a.getEndCap(), a.getLineJoin(), a.getMiterLimit(), a.getDashArray(), a.getDashPhase());
    }
}
