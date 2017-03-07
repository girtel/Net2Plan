package com.net2plan.gui.plugins.networkDesign.visualizationControl;

import com.net2plan.interfaces.networkDesign.Demand;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkElement;
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
     * @param currentNp
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

        if (this.timelineMaxSize <= 1) return; // nothing is stored since nothing will be retrieved
        if ((pickedForwardingRule == null) && (pickedNetworkElement == null)) return;

        if (!timeLine.isEmpty()) // TODO
        {
            if (timeLine.size() == 1)
            {
                System.out.println();
            }

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
            currentElementInTimelineCursor--;
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
