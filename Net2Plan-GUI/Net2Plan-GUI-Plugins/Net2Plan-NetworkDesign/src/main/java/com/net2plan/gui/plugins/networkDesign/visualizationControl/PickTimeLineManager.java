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

import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.utils.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Jorge San Emeterio
 * @date 09-Feb-17
 */
class PickTimeLineManager
{
    private NetPlan netPlan;
    private List<Object> timeLine;
    private int timelineCursor;

    private final int timelineMaxSize;

    PickTimeLineManager()
    {
        this(10);
    }

    PickTimeLineManager(final int timelineMaxSize)
    {
        this.timelineMaxSize = timelineMaxSize;

        this.timeLine = new ArrayList<>(timelineMaxSize + 1);
        this.timelineCursor = -1;
    }

    private <T> void updateTimeline(final NetPlan currentNp, final T element)
    {
        if (this.timelineMaxSize <= 1) return;
        if (element == null) throw new RuntimeException("Cannot add a null element.");
        if (currentNp == null) throw new RuntimeException("Cannot update with a null NetPlan.");

        // Check pointer validity
        if (!timeLine.isEmpty())
        {
            if (!(timelineCursor >= 0 && timelineCursor < timeLine.size()))
                throw new RuntimeException("Timeline cursor has been misplaced.");
        } else
        {
            if (timelineCursor != -1)
                throw new RuntimeException("Timeline cursor has been misplaced.");
        }

        // Updating netPlan
        if (netPlan != currentNp)
        {
            this.netPlan = currentNp;

            this.timeLine.clear();
            this.timelineCursor = -1;
        } else
        {
            // Same topology

            // Sanity check
            cleanDuty();

            // Clean duty can leave the timeline empty.
            if (!(timeLine.isEmpty() && timelineCursor == -1))
            {
                // Do not add the same element that is currently be clicked upon.
                if (element.equals(timeLine.get(timelineCursor))) return;

                // If the new element if different from what is stored, remove all the elements that were stored
                if (timelineCursor != (timeLine.size() - 1))
                {
                    final int nextElementCursorIndex = timelineCursor + 1;

                    final Object nextTimelineElement = timeLine.get(nextElementCursorIndex);
                    final Object currentTimelineElement = timeLine.get(timelineCursor);

                    if (nextTimelineElement != currentTimelineElement)
                        timeLine.subList(nextElementCursorIndex, timeLine.size()).clear();
                }
            }
        }

        /* Add the elements at the end of the list */
        timeLine.add(element);
        timelineCursor++;

        // Remove the oldest pick if the list get too big.
        if (timeLine.size() > timelineMaxSize)
        {
            timeLine.remove(0);
            timelineCursor--;
        }
    }

    @SuppressWarnings("unchecked")
    private void cleanDuty()
    {
        if (timeLine.size() > timelineMaxSize) throw new RuntimeException("Timeline is over its capacity.");

        final List<Object> newTimeLine = new ArrayList<>(timeLine);
        for (int index = 0; index < timeLine.size(); index++)
        {
            final Object o = timeLine.get(index);
            final NetPlan np;

            if (o instanceof NetworkElement)
            {
                final NetworkElement networkElement = (NetworkElement) o;
                if (netPlan.getNetworkElement(networkElement.getId()) == null)
                {
                    newTimeLine.remove(networkElement);
                    timelineCursor--;
                    continue;
                }

                np = networkElement.getNetPlan();
            } else if (o instanceof Pair)
            {
                final Pair<Demand, Link> forwardingRule = (Pair<Demand, Link>) o;

                if (netPlan.getDemandFromId(forwardingRule.getFirst().getId()) == null || netPlan.getLinkFromId(forwardingRule.getSecond().getId()) == null)
                {
                    newTimeLine.remove(forwardingRule);
                    timelineCursor--;
                    continue;
                }

                np = forwardingRule.getFirst().getNetPlan();
            } else
            {
                throw new RuntimeException("Unknown object in the timeline.");
            }

            // This element does not belong to this NetPlan
            if (this.netPlan != np)
                throw new RuntimeException("The current timeline contains elements from other topologies.");

            // Do not have duplicate elements next to each other
            if (index != timeLine.size() - 1)
            {
                if (o == timeLine.get(index + 1))
                {
                    newTimeLine.remove(o);
                    timelineCursor--;
                }
            }
        }
        this.timeLine = new ArrayList<>(newTimeLine);
    }

    Object getPickNavigationBackElement()
    {
        if (timeLine.isEmpty() || this.timelineMaxSize <= 1) return null;
        if (timelineCursor == 0) return null; // End of the timeline, there is no more past.

        // Clean the timeline before giving anything
        cleanDuty();

        return timeLine.get(--timelineCursor);
    }

    Object getPickNavigationForwardElement()
    {
        if (timeLine.isEmpty() || this.timelineMaxSize <= 1) return null;
        if (timelineCursor == timeLine.size() - 1) return null;

        // Clean the timeline before giving anything
        cleanDuty();

        return timeLine.get(++timelineCursor);
    }

    void addElement(final NetPlan currentNp, final NetworkElement element)
    {
        updateTimeline(currentNp, element);
    }

    void addElement(final NetPlan currentNp, final Pair<Demand, Link> forwardingRule)
    {
        updateTimeline(currentNp, forwardingRule);
    }
}
