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


package com.net2plan.gui.utils;

import javax.swing.*;
import java.awt.*;

/**
 * <p>{@code FlowLayout} subclass that fully supports wrapping of components.</p>
 * <p>Credits to Rob Camick for his {@code WrapLayout} (<a href='http://tips4java.wordpress.com/2008/11/06/wrap-layout/'>Wrap Layout</a>)</p>
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @see <a href="http://tips4java.wordpress.com/2008/11/06/wrap-layout/"></a>
 * @since 0.2.0
 */
public class WrapLayout extends FlowLayout {
    /**
     * Constructs a new {@code WrapLayout} with a left
     * alignment and a default 5-unit horizontal and vertical gap.
     *
     * @since 0.2.0
     */
    public WrapLayout() {
        super();
    }

    /**
     * Constructs a new {@code WrapLayout} with the specified
     * alignment and a default 5-unit horizontal and vertical gap. Allowed
     * values for the {@code align} are defined in {@code FlowLayout}.
     *
     * @param align the alignment value
     * @since 0.2.0
     */
    public WrapLayout(int align) {
        super(align);
    }

    /**
     * Creates a new flow layout manager with the indicated alignment
     * and the indicated horizontal and vertical gaps. Allowed
     * values for the {@code align} are defined in {@code FlowLayout}.
     *
     * @param align the alignment value
     * @param hgap  the horizontal gap between components
     * @param vgap  the vertical gap between components
     * @since 0.2.0
     */
    public WrapLayout(int align, int hgap, int vgap) {
        super(align, hgap, vgap);
    }

    @Override
    public Dimension minimumLayoutSize(Container target) {
        Dimension minimum = layoutSize(target, false);
        minimum.width -= (getHgap() + 1);
        return minimum;
    }

    @Override
    public Dimension preferredLayoutSize(Container target) {
        return layoutSize(target, true);
    }

    /*
     * A new row has been completed. Use the dimensions of this row
     * to update the preferred size for the container.
     *
     * @param dim update the width and height when appropriate
     * @param rowWidth the width of the row to add
     * @param rowHeight the height of the row to add
     * @since 0.2.0
     */
    private void addRow(Dimension dim, int rowWidth, int rowHeight) {
        dim.width = Math.max(dim.width, rowWidth);

        if (dim.height > 0) dim.height += getVgap();
        dim.height += rowHeight;
    }

    /**
     * Returns the minimum or preferred dimension needed to layout the target
     * container.
     *
     * @param target    target to get layout size for
     * @param preferred should preferred size be calculated
     * @return the dimension to layout the target container
     * @since 0.2.0
     */
    private Dimension layoutSize(Container target, boolean preferred) {
        synchronized (target.getTreeLock()) {
            /*  Each row must fit with the width allocated to the containter */
            /*  When the container width = 0, the preferred width of the container has not yet been calculated so lets ask for the maximum */
            int targetWidth = target.getSize().width;
            if (targetWidth == 0) targetWidth = Integer.MAX_VALUE;

            int hgap = getHgap();
            int vgap = getVgap();
            Insets insets = target.getInsets();
            int horizontalInsetsAndGap = insets.left + insets.right + (hgap * 2);
            int maxWidth = targetWidth - horizontalInsetsAndGap;

			/* Fit components into the allowed width */
            Dimension dim = new Dimension(0, 0);
            int rowWidth = 0;
            int rowHeight = 0;

            int nmembers = target.getComponentCount();

            for (int i = 0; i < nmembers; i++) {
                Component m = target.getComponent(i);

                if (m.isVisible()) {
                    Dimension d = preferred ? m.getPreferredSize() : m.getMinimumSize();

					/* Can't add the component to current row. Start a new row */
                    if (rowWidth + d.width > maxWidth) {
                        addRow(dim, rowWidth, rowHeight);
                        rowWidth = 0;
                        rowHeight = 0;
                    }

					/* Add a horizontal gap for all components after the first */
                    if (rowWidth != 0) rowWidth += hgap;

                    rowWidth += d.width;
                    rowHeight = Math.max(rowHeight, d.height);
                }
            }

            addRow(dim, rowWidth, rowHeight);

            dim.width += horizontalInsetsAndGap;
            dim.height += insets.top + insets.bottom + vgap * 2;

			/*
			 * When using a scroll pane or the DecoratedLookAndFeel we need to
			 * make sure the preferred size is less than the size of the
			 * target containter so shrinking the container size works
			 * correctly. Removing the horizontal gap is an easy way to do this
			 */
            Container scrollPane = SwingUtilities.getAncestorOfClass(JScrollPane.class, target);
            if (scrollPane != null) dim.width -= (hgap + 1);

            return dim;
        }
    }
}
