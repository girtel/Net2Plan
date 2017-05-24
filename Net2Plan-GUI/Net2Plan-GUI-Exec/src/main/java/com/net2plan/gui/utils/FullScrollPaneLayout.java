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
import javax.swing.border.Border;
import java.awt.*;

/**
 * <p>This class extends the default layout for {@code JScrollPane} to support
 * more than headers and corners.</p>
 * <p>redits to Santhosh Kumar for his <a href='http://www.jroller.com/santhosh/entry/enhancing_jscrollpane'>Enhancing JScrollPane</a>.</p>
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public class FullScrollPaneLayout extends ScrollPaneLayout {
    private static final String HORIZONTAL_LEFT = "HorizontalLeft";
    private static final String HORIZONTAL_RIGHT = "HorizontalRight";
    private static final String VERTICAL_TOP = "VerticalTop";
    private static final String VERTICAL_BOTTOM = "VerticalBottom";
    private Component hleft, hright, vtop, vbottom = null;

    @Override
    public void addLayoutComponent(String s, Component c) {
        switch (s) {
            case HORIZONTAL_LEFT:
                hleft = c;
                break;
            case HORIZONTAL_RIGHT:
                hright = c;
                break;
            case VERTICAL_TOP:
                vtop = c;
                break;
            case VERTICAL_BOTTOM:
                vbottom = c;
                break;
            default:
                super.addLayoutComponent(s, c);
                break;
        }
    }

    @Override
    public void layoutContainer(Container parent) {
        if (!(parent instanceof JScrollPane)) {
            throw new RuntimeException("Bad - Not a JScrollPane instance");
        }

        JScrollPane scrollPane = (JScrollPane) parent;
        vsbPolicy = scrollPane.getVerticalScrollBarPolicy();
        hsbPolicy = scrollPane.getHorizontalScrollBarPolicy();

        Rectangle availR = scrollPane.getBounds();
        availR.x = availR.y = 0;

        Insets insets = parent.getInsets();
        availR.x = insets.left;
        availR.y = insets.top;
        availR.width -= insets.left + insets.right;
        availR.height -= insets.top + insets.bottom;

        boolean leftToRight = isLeftToRight(scrollPane);

		/* If there's a visible column header remove the space it
         * needs from the top of availR.  The column header is treated
		 * as if it were fixed height, arbitrary width.
		 */
        Rectangle colHeadR = new Rectangle(0, availR.y, 0, 0);

        if ((colHead != null) && (colHead.isVisible())) {
            int colHeadHeight = Math.min(availR.height,
                    colHead.getPreferredSize().height);
            colHeadR.height = colHeadHeight;
            availR.y += colHeadHeight;
            availR.height -= colHeadHeight;
        }

		/* If there's a visible row header remove the space it needs
         * from the left or right of availR.  The row header is treated
		 * as if it were fixed width, arbitrary height.
		 */

        Rectangle rowHeadR = new Rectangle(0, 0, 0, 0);

        if ((rowHead != null) && (rowHead.isVisible())) {
            int rowHeadWidth = Math.min(availR.width,
                    rowHead.getPreferredSize().width);
            rowHeadR.width = rowHeadWidth;
            availR.width -= rowHeadWidth;
            if (leftToRight) {
                rowHeadR.x = availR.x;
                availR.x += rowHeadWidth;
            } else {
                rowHeadR.x = availR.x + availR.width;
            }
        }

		/* If there's a JScrollPane.viewportBorder, remove the
		 * space it occupies for availR.
		 */
        Border viewportBorder = scrollPane.getViewportBorder();
        Insets vpbInsets;
        if (viewportBorder != null) {
            vpbInsets = viewportBorder.getBorderInsets(parent);
            availR.x += vpbInsets.left;
            availR.y += vpbInsets.top;
            availR.width -= vpbInsets.left + vpbInsets.right;
            availR.height -= vpbInsets.top + vpbInsets.bottom;
        } else {
            vpbInsets = new Insets(0, 0, 0, 0);
        }

        Component view = (viewport != null) ? viewport.getView() : null;
        Dimension viewPrefSize = (view != null) ? view.getPreferredSize() : new Dimension(0, 0);
        Dimension extentSize = (viewport != null) ? viewport.toViewCoordinates(availR.getSize()) : new Dimension(0, 0);

        boolean viewTracksViewportWidth = false;
        boolean viewTracksViewportHeight = false;
        boolean isEmpty = (availR.width < 0 || availR.height < 0);

        Scrollable sv;
        if (!isEmpty && view instanceof Scrollable) {
            sv = (Scrollable) view;
            viewTracksViewportWidth = sv.getScrollableTracksViewportWidth();
            viewTracksViewportHeight = sv.getScrollableTracksViewportHeight();
        } else {
            sv = null;
        }

        Rectangle vsbR = new Rectangle(0, availR.y - vpbInsets.top, 0, 0);
        boolean vsbNeeded;

        if (isEmpty) {
            vsbNeeded = false;
        } else if (vsbPolicy == VERTICAL_SCROLLBAR_ALWAYS) {
            vsbNeeded = true;
        } else if (vsbPolicy == VERTICAL_SCROLLBAR_NEVER) {
            vsbNeeded = false;
        } else {
            vsbNeeded = !viewTracksViewportHeight && (viewPrefSize.height > extentSize.height);
        }

        if ((vsb != null) && vsbNeeded) {
            adjustForVSB(true, availR, vsbR, vpbInsets, leftToRight);
            extentSize = viewport.toViewCoordinates(availR.getSize());
        }

        Rectangle hsbR = new Rectangle(availR.x - vpbInsets.left, 0, 0, 0);
        boolean hsbNeeded;
        if (isEmpty) {
            hsbNeeded = false;
        } else if (hsbPolicy == HORIZONTAL_SCROLLBAR_ALWAYS) {
            hsbNeeded = true;
        } else if (hsbPolicy == HORIZONTAL_SCROLLBAR_NEVER) {
            hsbNeeded = false;
        } else {
            hsbNeeded = !viewTracksViewportWidth && (viewPrefSize.width > extentSize.width);
        }

        if ((hsb != null) && hsbNeeded) {
            adjustForHSB(true, availR, hsbR, vpbInsets);
            if ((vsb != null) && !vsbNeeded && (vsbPolicy != VERTICAL_SCROLLBAR_NEVER)) {
                extentSize = viewport.toViewCoordinates(availR.getSize());
                vsbNeeded = viewPrefSize.height > extentSize.height;

                if (vsbNeeded) {
                    adjustForVSB(true, availR, vsbR, vpbInsets, leftToRight);
                }
            }
        }

        if (viewport != null) {
            viewport.setBounds(availR);

            if (sv != null) {
                extentSize = viewport.toViewCoordinates(availR.getSize());

                boolean oldHSBNeeded = hsbNeeded;
                boolean oldVSBNeeded = vsbNeeded;
                viewTracksViewportWidth = sv.getScrollableTracksViewportWidth();
                viewTracksViewportHeight = sv.getScrollableTracksViewportHeight();
                if (vsb != null && vsbPolicy == VERTICAL_SCROLLBAR_AS_NEEDED) {
                    boolean newVSBNeeded = !viewTracksViewportHeight && (viewPrefSize.height > extentSize.height);
                    if (newVSBNeeded != vsbNeeded) {
                        vsbNeeded = newVSBNeeded;
                        adjustForVSB(vsbNeeded, availR, vsbR, vpbInsets, leftToRight);
                        extentSize = viewport.toViewCoordinates(availR.getSize());
                    }
                }

                if (hsb != null && hsbPolicy == HORIZONTAL_SCROLLBAR_AS_NEEDED) {
                    boolean newHSBbNeeded = !viewTracksViewportWidth && (viewPrefSize.width > extentSize.width);
                    if (newHSBbNeeded != hsbNeeded) {
                        hsbNeeded = newHSBbNeeded;
                        adjustForHSB(hsbNeeded, availR, hsbR, vpbInsets);
                        if ((vsb != null) && !vsbNeeded && (vsbPolicy != VERTICAL_SCROLLBAR_NEVER)) {
                            extentSize = viewport.toViewCoordinates(availR.getSize());
                            vsbNeeded = viewPrefSize.height > extentSize.height;

                            if (vsbNeeded) {
                                adjustForVSB(true, availR, vsbR, vpbInsets, leftToRight);
                            }
                        }
                    }
                }

                if (oldHSBNeeded != hsbNeeded || oldVSBNeeded != vsbNeeded) {
                    viewport.setBounds(availR);
                }
            }
        }

        vsbR.height = availR.height + vpbInsets.top + vpbInsets.bottom;
        hsbR.width = availR.width + vpbInsets.left + vpbInsets.right;
        rowHeadR.height = availR.height + vpbInsets.top + vpbInsets.bottom;
        rowHeadR.y = availR.y - vpbInsets.top;
        colHeadR.width = availR.width + vpbInsets.left + vpbInsets.right;
        colHeadR.x = availR.x - vpbInsets.left;

        if (rowHead != null) {
            rowHead.setBounds(rowHeadR);
        }
        if (colHead != null) {
            colHead.setBounds(colHeadR);
        }

        if (vsb != null) {
            if (vsbNeeded) {
                vsb.setVisible(true);
                if (vtop == null && vbottom == null) {
                    vsb.setBounds(vsbR);
                } else {
                    Rectangle rect = new Rectangle(vsbR);
                    if (vtop != null) {
                        Dimension dim = vtop.getPreferredSize();
                        rect.y += dim.height;
                        rect.height -= dim.height;
                        vtop.setVisible(true);
                        vtop.setBounds(vsbR.x, vsbR.y, vsbR.width, dim.height);
                    }
                    if (vbottom != null) {
                        Dimension dim = vbottom.getPreferredSize();
                        rect.height -= dim.height;
                        vbottom.setVisible(true);
                        vbottom.setBounds(vsbR.x, vsbR.y + vsbR.height - dim.height, vsbR.width, dim.height);
                    }
                    vsb.setBounds(rect);
                }
            } else {
                vsb.setVisible(false);
                if (vtop != null) {
                    vtop.setVisible(false);
                }
                if (vbottom != null) {
                    vbottom.setVisible(false);
                }
            }
        }

        if (hsb != null) {
            if (hsbNeeded) {
                hsb.setVisible(true);
                if (hleft == null && hright == null) {
                    hsb.setBounds(hsbR);
                } else {
                    Rectangle rect = new Rectangle(hsbR);
                    if (hleft != null) {
                        Dimension dim = hleft.getPreferredSize();
                        rect.x += dim.width;
                        rect.width -= dim.width;
                        hleft.setVisible(true);
                        hleft.setBounds(hsbR.x, hsbR.y, dim.width, hsbR.height);
                        hleft.doLayout();
                    }
                    if (hright != null) {
                        Dimension dim = hright.getPreferredSize();
                        rect.width -= dim.width;
                        hright.setVisible(true);
                        hright.setBounds(hsbR.x + hsbR.width - dim.width, hsbR.y, dim.width, hsbR.height);
                    }
                    hsb.setBounds(rect);
                }
            } else {
                hsb.setVisible(false);
                if (hleft != null) {
                    hleft.setVisible(false);
                }
                if (hright != null) {
                    hright.setVisible(false);
                }
            }
        }

        if (lowerLeft != null) {
            lowerLeft.setBounds(leftToRight ? rowHeadR.x : vsbR.x, hsbR.y, leftToRight ? rowHeadR.width : vsbR.width, hsbR.height);
        }
        if (lowerRight != null) {
            lowerRight.setBounds(leftToRight ? vsbR.x : rowHeadR.x, hsbR.y, leftToRight ? vsbR.width : rowHeadR.width, hsbR.height);
        }
        if (upperLeft != null) {
            upperLeft.setBounds(leftToRight ? rowHeadR.x : vsbR.x, colHeadR.y, leftToRight ? rowHeadR.width : vsbR.width, colHeadR.height);
        }
        if (upperRight != null) {
            upperRight.setBounds(leftToRight ? vsbR.x : rowHeadR.x, colHeadR.y, leftToRight ? vsbR.width : rowHeadR.width, colHeadR.height);
        }
    }

    @Override
    public void removeLayoutComponent(Component c) {
        if (c == hleft) {
            hleft = null;
        } else if (c == hright) {
            hright = null;
        } else if (c == vtop) {
            vtop = null;
        } else if (c == vbottom) {
            vbottom = null;
        } else {
            super.removeLayoutComponent(c);
        }
    }

    private void adjustForHSB(boolean wantsHSB, Rectangle available, Rectangle hsbR, Insets vpbInsets) {
        int oldHeight = hsbR.height;
        if (wantsHSB) {
            int hsbHeight = Math.max(0, Math.min(available.height, hsb.getPreferredSize().height));

            available.height -= hsbHeight;
            hsbR.y = available.y + available.height + vpbInsets.bottom;
            hsbR.height = hsbHeight;
        } else {
            available.height += oldHeight;
        }
    }

    private void adjustForVSB(boolean wantsVSB, Rectangle available, Rectangle vsbR, Insets vpbInsets, boolean leftToRight) {
        int oldWidth = vsbR.width;
        if (wantsVSB) {
            int vsbWidth = Math.max(0, Math.min(vsb.getPreferredSize().width, available.width));

            available.width -= vsbWidth;
            vsbR.width = vsbWidth;

            if (leftToRight) {
                vsbR.x = available.x + available.width + vpbInsets.right;
            } else {
                vsbR.x = available.x - vpbInsets.left;
                available.x += vsbWidth;
            }
        } else {
            available.width += oldWidth;
        }
    }

    private static boolean isLeftToRight(Component c) {
        return c.getComponentOrientation().isLeftToRight();
    }
}
