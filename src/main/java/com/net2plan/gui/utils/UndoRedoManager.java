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
import java.util.Map.Entry;

import org.apache.commons.collections15.BidiMap;
import org.apache.commons.collections15.bidimap.DualHashBidiMap;

import com.net2plan.gui.utils.topologyPane.VisualizationState;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkElement;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.interfaces.networkDesign.Resource;
import com.net2plan.utils.Pair;

/** Manages the undo/redo information, tracking the current netPlan and the visualization state
 */
public class UndoRedoManager
{
	private List<Pair<VisualizationState , Boolean>> pastInfoVsNewNp;
	private int pastInfoVsNewNpCursor;
	private int maxSizeUndoList;
	private final IVisualizationCallback callback;
	
	public UndoRedoManager(IVisualizationCallback callback , int maxSizeUndoList)
	{
		this.pastInfoVsNewNp = new ArrayList<> ();
		this.pastInfoVsNewNpCursor = -1;
		this.callback = callback;
		this.maxSizeUndoList = maxSizeUndoList;
	}

    /** Update the navigation with the new picked element, but no change in netPlan
     */
    public void updateNavigationInformation_onlyVisualizationChange ()
    {
    	return;
//    	if (this.maxSizeUndoList <= 1) return; // nothing is stored since nothing will be retrieved
//        if (callback.inOnlineSimulationMode()) return;
//        /* first remove everything after the cursor */
//        if (pastInfoVsNewNpCursor != pastInfoVsNewNp.size() - 1)
//        	this.pastInfoVsNewNp = this.pastInfoVsNewNp.subList(0 , pastInfoVsNewNpCursor+1);
//        final VisualizationState vsCopy = callback.getVisualizationState().copy(null);
//       	pastInfoVsNewNp.add(Pair.of(vsCopy , false));
//		VisualizationState.checkNpToVsConsistency(vsCopy , callback.getDesign());
//		while (pastInfoVsNewNp.size() > maxSizeUndoList)
//			pastInfoVsNewNp.remove(0);
//		pastInfoVsNewNpCursor = pastInfoVsNewNp.size()-1;
//		System.out.println("Introduced only visualization change index " + (pastInfoVsNewNp.size()-1) + ". Callback currentNp: " + callback.getDesign().hashCode() + ") VS-np: " + vsCopy.getNetPlan().hashCode());
    }

    public void updateNavigationInformation_newNetPlanChange ()
    {
		printUndoList("BEFORE NEWCHANGE");
        if (this.maxSizeUndoList <= 1) return; // nothing is stored since nothing will be retrieved
        if (callback.inOnlineSimulationMode()) return;
        /* first remove everything after the cursor */
        while (this.pastInfoVsNewNp.size() > pastInfoVsNewNpCursor + 1) this.pastInfoVsNewNp.remove(pastInfoVsNewNp.size()-1);
//        if (pastInfoVsNewNpCursor != pastInfoVsNewNp.size() - 1)
//        	this.pastInfoVsNewNp = this.pastInfoVsNewNp.subList(0 , pastInfoVsNewNpCursor+1);
        final NetPlan npCopy = callback.getDesign().copy();
//        final VisualizationState vsCopyLinkedToNpCopy = callback.getVisualizationState().copy(npCopy);
        
        final BidiMap<NetworkLayer, Integer> cp_mapLayer2VisualizationOrder = new DualHashBidiMap<>();
		for (NetworkLayer cpLayer : npCopy.getNetworkLayers()) 
			cp_mapLayer2VisualizationOrder.put(cpLayer, callback.getVisualizationState().getCanvasVisualizationOrderNotRemovingNonVisible (callback.getDesign().getNetworkLayer(cpLayer.getIndex())));
        final Map<NetworkLayer, Boolean> cp_layerVisibilityMap = new HashMap<>();
		for (NetworkLayer cpLayer : npCopy.getNetworkLayers()) 
			cp_layerVisibilityMap.put(cpLayer, callback.getVisualizationState().isLayerVisibleInCanvas(callback.getDesign().getNetworkLayer(cpLayer.getIndex())));
        final VisualizationState vsCopyLinkedToNpCopy = new VisualizationState(npCopy, cp_mapLayer2VisualizationOrder, cp_layerVisibilityMap);

        System.out.println("*** The VS copied in undo list has pickedEleent: " + vsCopyLinkedToNpCopy.getPickedNetworkElement());
        
        VisualizationState.checkNpToVsConsistency(vsCopyLinkedToNpCopy , npCopy);
		pastInfoVsNewNp.add(Pair.of(vsCopyLinkedToNpCopy , true));
		while (pastInfoVsNewNp.size() > maxSizeUndoList)
			pastInfoVsNewNp.remove(0);
		pastInfoVsNewNpCursor = pastInfoVsNewNp.size()-1;
		System.out.println("Introduced NP change index " + (pastInfoVsNewNp.size()-1) + ". Callback currentNp: " + callback.getDesign().hashCode() + ") VS-np: " + vsCopyLinkedToNpCopy.getNetPlan().hashCode());
		printUndoList("AFTER NEWCHANGE");
    }

    /** Returns the undo info in the navigation. Returns null if we are already in the first element. The NetPlan object returned is null if
     * there is no change respect to the current one
     * @return see above
     */
    public Pair<VisualizationState,Boolean> getNavigationBackElement ()
    {
		printUndoList("BEFORE BACK");
        if (this.maxSizeUndoList <= 1) return null; // nothing is stored since nothing will be retrieved
        if (callback.inOnlineSimulationMode()) return null;
    	if (pastInfoVsNewNpCursor == 0) return null;
		final int originalCursor = pastInfoVsNewNpCursor;
		final int newCursor = pastInfoVsNewNpCursor - 1;
		final VisualizationState backVS = pastInfoVsNewNp.get(newCursor).getFirst();
		final boolean changedNp = (pastInfoVsNewNp.get(originalCursor).getSecond());
		this.pastInfoVsNewNpCursor = newCursor;
		printUndoList("AFTER BACK");
		return Pair.of (backVS , changedNp);
    }
    
    /** Returns the forward info in the navigation. Returns null if we are already in the head. The NetPlan object returned is null if
     * there is no change respect to the current one
     * @return see above
     */
    public Pair<VisualizationState,Boolean>  getNavigationForwardElement ()
    {
		printUndoList("BEFORE FORWARD");

        if (this.maxSizeUndoList <= 1) return null; // nothing is stored since nothing will be retrieved
        if (callback.inOnlineSimulationMode()) return null;
		if (pastInfoVsNewNpCursor == pastInfoVsNewNp.size()-1) return null;
		final int newCursor = pastInfoVsNewNpCursor + 1;
		final VisualizationState nextVS = pastInfoVsNewNp.get(newCursor).getFirst();
		final boolean changedNp = (pastInfoVsNewNp.get(newCursor).getSecond());
		this.pastInfoVsNewNpCursor = newCursor;
		printUndoList("AFTER FORWARD");
		return Pair.of(nextVS , changedNp);
    }

    private void printUndoList (String m)
    {
    	System.out.println("Unod list: (" + pastInfoVsNewNp.size() + " elements, cursor: " + pastInfoVsNewNpCursor+  ") ---" + m);
    	for (Pair<VisualizationState,Boolean> el : pastInfoVsNewNp)
    	{
    		final NetPlan np = el.getFirst().getNetPlan();
    		System.out.println("Np " + np.hashCode() + ", nodes: " + np.getNumberOfNodes () + ", num layers: " + np.getNumberOfLayers() + ", new topology: " + el.getSecond());
    	}
    	
    }
}
