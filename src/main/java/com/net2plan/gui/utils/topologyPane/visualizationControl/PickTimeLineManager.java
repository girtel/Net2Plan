package com.net2plan.gui.utils.topologyPane.visualizationControl;

import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.internal.Constants;
import com.net2plan.utils.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Jorge San Emeterio
 * @date 09-Feb-17
 */
public class PickTimeLineManager
{
    private NetPlan netPlan;

    private List<Pair<NetworkElement, Pair<Demand, Link>>> pastPickedElements;
    private int pastPickedElementsCursor;
    private int pickListSize;

    private NetworkElement pickedNetworkElement;
    private Pair<Demand, Link> pickedForwardingRule;

    PickTimeLineManager(final int pickListSize)
    {
        this.pastPickedElements = new ArrayList<>(pickListSize + 1);
        this.pastPickedElementsCursor = -1;
        this.pickListSize = pickListSize;

        this.pickedNetworkElement = null;
        this.pickedForwardingRule = null;
    }

    void updatePickUndoList_newPickOrPickReset(final NetPlan currentNp)
    {
        // Updating netPlan
        if (netPlan == null) this.netPlan = currentNp;
        if (netPlan != currentNp)
        {
            this.netPlan = currentNp;

            this.pastPickedElements.clear();
            this.pastPickedElementsCursor = -1;
            updatePickUndoList_newPickOrPickReset(netPlan); // add a no pick, this is never removed
        }

        if (this.pickListSize <= 1) return; // nothing is stored since nothing will be retrieved
        if ((pickedForwardingRule == null) && (pickedNetworkElement == null)) return;

        /* Eliminate repeated continuous elements in the list, and everything after the cursor */
        final List<Pair<NetworkElement, Pair<Demand, Link>>> newList = new ArrayList<>();
        for (int index = 0; index <= pastPickedElementsCursor; index++)
        {
            final NetworkElement ne = pastPickedElements.get(index).getFirst();
            final Pair<Demand, Link> fr = pastPickedElements.get(index).getSecond();
            if ((index > 0) && (pastPickedElements.get(index).equals(pastPickedElements.get(index - 1)))) continue;
            if (ne != null) if (ne.getNetPlan() != netPlan) continue;
            if (fr != null)
                if ((fr.getFirst().getNetPlan() != netPlan) || (fr.getSecond().getNetPlan() != netPlan)) continue;
            newList.add(pastPickedElements.get(index));
        }
        this.pastPickedElements = newList;

        /* If the same element picked as the last one, do not add */
        if (!pastPickedElements.isEmpty())
            if (Pair.of(this.pickedNetworkElement, pickedForwardingRule).equals(pastPickedElements.get(pastPickedElements.size() - 1)))
                return;

        /* Add the elements at the end of the list */
        pastPickedElements.add(Pair.of(pickedNetworkElement, pickedForwardingRule));

        /* Check list size */
        while (pastPickedElements.size() > pickListSize)
            pastPickedElements.remove(0);
        pastPickedElementsCursor = pastPickedElements.size() - 1;
    }

    Pair<NetworkElement, Pair<Demand, Link>> getPickNavigationBackElement()
    {
        if (this.pickListSize <= 1) return null; // nothing is stored since nothing will be retrieved
        if (pastPickedElementsCursor == 0) return null;
        this.pastPickedElementsCursor--;
        return pastPickedElements.get(this.pastPickedElementsCursor);
    }

    Pair<NetworkElement, Pair<Demand, Link>> getPickNavigationForwardElement()
    {
        if (this.pickListSize <= 1) return null; // nothing is stored since nothing will be retrieved
        if (pastPickedElementsCursor >= pastPickedElements.size() - 1) return null;
        this.pastPickedElementsCursor++;
        return pastPickedElements.get(this.pastPickedElementsCursor);
    }

    void pickLayer(final NetPlan currentNp, final NetworkLayer pickedLayer)
    {
        this.pickedForwardingRule = null;
        this.pickedNetworkElement = pickedLayer;
        updatePickUndoList_newPickOrPickReset(currentNp);
    }

    void pickDemand(final NetPlan currentNp, final Demand pickedDemand)
    {
        this.pickedNetworkElement = pickedDemand;
        this.pickedForwardingRule = null;
        updatePickUndoList_newPickOrPickReset(currentNp);
    }

    void pickSRG(final NetPlan currentNp, final SharedRiskGroup pickedSRG)
    {
        this.pickedNetworkElement = pickedSRG;
        this.pickedForwardingRule = null;
        updatePickUndoList_newPickOrPickReset(currentNp);
    }

    void pickMulticastDemand(final NetPlan currentNp, final MulticastDemand pickedDemand)
    {
        this.pickedNetworkElement = pickedDemand;
        this.pickedForwardingRule = null;
        updatePickUndoList_newPickOrPickReset(currentNp);
    }

    void pickRoute(final NetPlan currentNp, final Route pickedRoute)
    {
        this.pickedNetworkElement = pickedRoute;
        this.pickedForwardingRule = null;
        updatePickUndoList_newPickOrPickReset(currentNp);
    }

    void pickMulticastTree(final NetPlan currentNp, final MulticastTree pickedTree)
    {
        this.pickedNetworkElement = pickedTree;
        this.pickedForwardingRule = null;
        updatePickUndoList_newPickOrPickReset(currentNp);
    }

    void pickLink(final NetPlan currentNp, final Link pickedLink)
    {
        this.pickedNetworkElement = pickedLink;
        this.pickedForwardingRule = null;
        updatePickUndoList_newPickOrPickReset(currentNp);
    }

    void pickNode(final NetPlan currentNp, final Node pickedNode)
    {
        this.pickedNetworkElement = pickedNode;
        this.pickedForwardingRule = null;
        updatePickUndoList_newPickOrPickReset(currentNp);
    }

    void pickResource(final NetPlan currentNp, final Resource pickedResource)
    {
        this.pickedNetworkElement = pickedResource;
        this.pickedForwardingRule = null;
        updatePickUndoList_newPickOrPickReset(currentNp);
    }

    void pickForwardingRule(final NetPlan currentNp, final Pair<Demand, Link> pickedFR)
    {
        this.pickedForwardingRule = pickedFR;
        this.pickedNetworkElement = null;
        updatePickUndoList_newPickOrPickReset(currentNp);
    }
}
