/*
 * ******************************************************************************
 *  * Copyright (c) 2017 Pablo Pavon-Marino.
 *  * All rights reserved. This program and the accompanying materials
 *  * are made available under the terms of the GNU Lesser Public License v3.0
 *  * which accompanies this distribution, and is available at
 *  * http://www.gnu.org/licenses/lgpl.html
 *  *
 *  * Contributors:
 *  *     Pablo Pavon-Marino - Jose-Luis Izquierdo-Zaragoza, up to version 0.3.1
 *  *     Pablo Pavon-Marino - from version 0.4.0 onwards
 *  *     Pablo Pavon Marino - Jorge San Emeterio Villalain, from version 0.4.1 onwards
 *  *****************************************************************************
 */

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

    private List<Pair<NetworkElement, Pair<Demand, Link>>> timeLine;
    private int currentElementInTimelineCursor;
    private int timelineMaxSize;

    private NetworkElement pickedNetworkElement;
    private Pair<Demand, Link> pickedForwardingRule;

    PickTimeLineManager()
    {
        this.timeLine = new ArrayList<>(timelineMaxSize + 1);
        this.currentElementInTimelineCursor = -1;
        this.timelineMaxSize = 10;

        this.pickedNetworkElement = null;
        this.pickedForwardingRule = null;
    }

    /**
     * Update timeline after new pick or reset
     *
     * @param currentNp Current NetPlan
     */
    private void updateTimeline(final NetPlan currentNp)
    {
        // Updating netPlan
        if (netPlan == null) this.netPlan = currentNp;
        if (netPlan != currentNp)
        {
            this.netPlan = currentNp;

            this.timeLine.clear();
            this.currentElementInTimelineCursor = -1;
        }
        
        // Synchronizing timeline and netPlan
        List<Pair<NetworkElement, Pair<Demand, Link>>> cleanedUpTimeline = new ArrayList<>(timeLine);
        for (Pair<NetworkElement, Pair<Demand, Link>> pair : timeLine)
        {
            final NetworkElement element = pair.getFirst();
            final Pair<Demand, Link> FR = pair.getSecond();

            if (pair.getFirst() != null)
            {
                if (netPlan.getNetworkElement(element.getId()) == null)
                {
                    cleanedUpTimeline.remove(pair);
                    currentElementInTimelineCursor--;
                }
            } else if (pair.getSecond() != null)
            {
                if (netPlan.getDemandFromId(FR.getFirst().getId()) == null || netPlan.getLinkFromId(FR.getSecond().getId()) == null)
                {
                    cleanedUpTimeline.remove(pair);
                    currentElementInTimelineCursor--;
                }
            } else
            {
                throw new RuntimeException();
            }
        }
        timeLine = cleanedUpTimeline;

        if (this.timelineMaxSize <= 1) return; // nothing is stored since nothing will be retrieved
        if ((pickedForwardingRule == null) && (pickedNetworkElement == null)) return;

        if (!timeLine.isEmpty())
        {
            // Do not add the same element that is currently be clicked upon.
            if (Pair.unmodifiableOf(this.pickedNetworkElement, pickedForwardingRule).equals(timeLine.get(currentElementInTimelineCursor)))
            {
                return;
            }

            // If the new element if different from what is stored, remove all the elements that were stored
            if (currentElementInTimelineCursor != timeLine.size() - 1)
            {
                final int nextElementCursorIndex = currentElementInTimelineCursor + 1;
                final Pair<NetworkElement, Pair<Demand, Link>> nextTimelineElement = timeLine.get(nextElementCursorIndex);
                if (nextTimelineElement.getFirst() != pickedNetworkElement || nextTimelineElement.getSecond() != pickedForwardingRule)
                {
                    timeLine.subList(nextElementCursorIndex, timeLine.size()).clear();
                }
            }
        }

        // Cleaning duty
        final List<Pair<NetworkElement, Pair<Demand, Link>>> newTimeLine = new ArrayList<>();
        for (int index = 0; index < timeLine.size(); index++)
        {
            final NetworkElement ne = timeLine.get(index).getFirst();
            final Pair<Demand, Link> fr = timeLine.get(index).getSecond();

            // Do not add this pick if the last if the same as this one.
            if ((index > 0) && (timeLine.get(index).equals(timeLine.get(index - 1)))) continue;

            // This element does not belong to this NetPlan
            if (ne != null && ne.getNetPlan() != netPlan) continue;
            if (fr != null && ((fr.getFirst().getNetPlan() != netPlan) || (fr.getSecond().getNetPlan() != netPlan)))
                continue;
            newTimeLine.add(timeLine.get(index));
        }
        this.timeLine = new ArrayList<>(newTimeLine);

        /* Add the elements at the end of the list */
        timeLine.add(Pair.of(pickedNetworkElement, pickedForwardingRule));

        // Remove the oldest pick if the list get too big.
        while (timeLine.size() > timelineMaxSize)
        {
            timeLine.remove(0);
            currentElementInTimelineCursor = timeLine.size() - 1;
        }

        // NOTE: The cursor does not depend on the timeline, which may cause them to desynchronize.
        currentElementInTimelineCursor++;
    }

    Pair<NetworkElement, Pair<Demand, Link>> getPickNavigationBackElement()
    {
        if (timeLine.isEmpty()) return null;
        if (this.timelineMaxSize <= 1) return null; // Empty timeline, nothing can be returned
        if (currentElementInTimelineCursor == 0) return null; // End of the timeline, there is no more past.
        this.currentElementInTimelineCursor--; // Retrieving prior element
        return timeLine.get(this.currentElementInTimelineCursor);
    }

    Pair<NetworkElement, Pair<Demand, Link>> getPickNavigationForwardElement()
    {
        if (timeLine.isEmpty()) return null;
        if (this.timelineMaxSize <= 1) return null; // nothing is stored since nothing will be retrieved
        if (currentElementInTimelineCursor >= timeLine.size() - 1) return null;
        this.currentElementInTimelineCursor++;
        return timeLine.get(this.currentElementInTimelineCursor);
    }

    void addElement(final NetPlan currentNp, final NetworkElement element)
    {
        this.pickedForwardingRule = null;
        this.pickedNetworkElement = element;
        updateTimeline(currentNp);
    }

    void addElement(final NetPlan currentNp, final Pair<Demand, Link> forwardingRule)
    {
        this.pickedForwardingRule = forwardingRule;
        this.pickedNetworkElement = null;
        updateTimeline(currentNp);
    }
}
