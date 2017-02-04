package com.net2plan.gui.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.net2plan.gui.utils.topologyPane.VisualizationState;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkElement;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.interfaces.networkDesign.Resource;
import com.net2plan.utils.Pair;

/** Manages the undo/redo information, tracking the current netPlan and the visualization state
 */
public class UndoRedoManager
{
	private List<Pair<NetPlan , VisualizationState>> pastShownInformation;
	private int pastShownInformationCursor;
	private int maxSizeUndoList;
	private final IVisualizationCallback callback;
	
	public UndoRedoManager(IVisualizationCallback callback , int maxSizeUndoList)
	{
		this.pastShownInformation = new ArrayList<> ();
		this.pastShownInformationCursor = -1;
		this.callback = callback;
		this.maxSizeUndoList = maxSizeUndoList;
	}

    /** Update the navigation with the new picked element, but no change in netPlan
     */
    public void updateNavigationInformation_onlyVisualizationChange ()
    {
        if (callback.inOnlineSimulationMode()) return;
        /* first remove everything after the cursor */
    	while (pastShownInformation.size() > pastShownInformationCursor+1) pastShownInformation.remove(pastShownInformationCursor+1);
        final VisualizationState vsCopy = callback.getVisualizationState().copy(null);
       	pastShownInformation.add(Pair.of(null , vsCopy));
		System.out.println("Introduced only visualization change index " + (pastShownInformation.size()-1) + ". Np: null (currentNp " + callback.getDesign().hashCode() + ") VS-np: " + vsCopy.getNetPlan().hashCode());
		VisualizationState.checkNpToVsConsistency(vsCopy , callback.getDesign());
		if (pastShownInformation.size() > maxSizeUndoList)
		{
			final NetPlan originNp = pastShownInformation.get(0).getFirst();
			if (originNp == null) throw new RuntimeException ();
			pastShownInformation.remove(0);
			if (pastShownInformation.get(0).getFirst() == null)
				pastShownInformation.get(0).setFirst(originNp);
		}
		pastShownInformationCursor = pastShownInformation.size()-1;
    }

    public void updateNavigationInformation_newNetPlanChange ()
    {
        if (callback.inOnlineSimulationMode()) return;
        /* first remove everything after the cursor */
    	while (pastShownInformation.size() > pastShownInformationCursor+1) pastShownInformation.remove(pastShownInformationCursor+1);
        final NetPlan npCopy = callback.getDesign().copy();
        final VisualizationState vsCopyLinkedToNpCopy = callback.getVisualizationState().copy(npCopy);
		VisualizationState.checkNpToVsConsistency(vsCopyLinkedToNpCopy , npCopy);
		pastShownInformation.add(Pair.of(npCopy, vsCopyLinkedToNpCopy));
		System.out.println("Introduced NP change index " + (pastShownInformation.size()-1) + ". Np: " + npCopy.hashCode() + ") VS-np: " + vsCopyLinkedToNpCopy.getNetPlan().hashCode());
		if (pastShownInformation.size() > maxSizeUndoList)
		{
			final NetPlan originNp = pastShownInformation.get(0).getFirst();
			if (originNp == null) throw new RuntimeException ();
			pastShownInformation.remove(0);
			if (pastShownInformation.get(0).getFirst() == null)
				pastShownInformation.get(0).setFirst(originNp);
		}
		pastShownInformationCursor = pastShownInformation.size()-1;
    }

    /** Returns the undo info in the navigation. Returns null if we are already in the first element. The NetPlan object returned is null if
     * there is no change respect to the current one
     * @return see above
     */
    public Pair<NetPlan,VisualizationState> getNavigationBackElement ()
    {
        if (callback.inOnlineSimulationMode()) return null;
    	if (pastShownInformationCursor == 0) return null;
		final int originalCursor = pastShownInformationCursor;
		final int newCursor = pastShownInformationCursor - 1;
		final VisualizationState backVS = pastShownInformation.get(newCursor).getSecond();
		final boolean notChangedNp = (pastShownInformation.get(originalCursor).getFirst() == null);
		if (notChangedNp)
		{
			this.pastShownInformationCursor = newCursor;
			return Pair.of(null , backVS);
		}
		NetPlan backNp = null;
		for (int index = newCursor; index >= 0 ; index --)
			if (pastShownInformation.get(index).getFirst() != null)
			{ 
				backNp = pastShownInformation.get(index).getFirst();
				break;
			}
		if (backNp == null) throw new RuntimeException ();
		this.pastShownInformationCursor = newCursor;
		return Pair.of(backNp , backVS);
    }
    
    /** Returns the forward info in the navigation. Returns null if we are already in the head. The NetPlan object returned is null if
     * there is no change respect to the current one
     * @return see above
     */
    public Pair<NetPlan,VisualizationState>  getNavigationForwardElement ()
    {
        if (callback.inOnlineSimulationMode()) return null;
		if (pastShownInformationCursor == pastShownInformation.size()-1) return null;
		final int newCursor = pastShownInformationCursor + 1;
		final NetPlan nextNp = pastShownInformation.get(newCursor).getFirst();
		final VisualizationState nextVS = pastShownInformation.get(newCursor).getSecond();
		this.pastShownInformationCursor = newCursor;
		return Pair.of(nextNp , nextVS);
    }

}
