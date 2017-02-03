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
		pastShownInformation.add(Pair.of(null , callback.getVisualizationState().copy()));
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
        final NetPlan npCopy = callback.getDesign().copy();
        final VisualizationState vsCopyLinkedToNpCopy = callback.getVisualizationState().copyAndAdapt(npCopy);
        final Pair<NetPlan,VisualizationState> pair = Pair.of(npCopy, vsCopyLinkedToNpCopy);
		pastShownInformation.add(pair);
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
		final int newCursor = pastShownInformationCursor - 1;
		final VisualizationState backVS = pastShownInformation.get(newCursor).getSecond();
		final boolean notChangedNp = (pastShownInformation.get(newCursor).getFirst() == null) && (pastShownInformation.get(pastShownInformationCursor).getFirst() == null);
		if (notChangedNp)
			return Pair.of(null , backVS);
		NetPlan backNp = null;
		for (int index = newCursor; index >= 0 ; index --)
			if (pastShownInformation.get(index).getFirst() != null)
			{ 
				backNp = pastShownInformation.get(index).getFirst();
				break;
			}
		if (backNp == null) throw new RuntimeException ();
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
		return Pair.of(nextNp == null? callback.getDesign() : nextNp , nextVS);
    }

    
	private static Set<Link> getLinkSetFromIds (NetPlan np , Collection<Long> ids) 
	{
		Set<Link> res = new HashSet<Link> (); for (long id : ids) res.add(np.getLinkFromId(id)); return res; 
	}
	private static Set<Node> getNodeSetFromIds (NetPlan np , Collection<Long> ids) 
	{
		Set<Node> res = new HashSet<Node> (); for (long id : ids) res.add(np.getNodeFromId(id)); return res; 
	}
	private static List<Link> getLinkListFromIds (NetPlan np , Collection<Long> ids) 
	{
		List<Link> res = new LinkedList<Link> (); for (long id : ids) res.add(np.getLinkFromId(id)); return res; 
	}
	private static List<NetworkElement> getLinkAndResorceListFromIds (NetPlan np , Collection<Long> ids) 
	{
		List<NetworkElement> res = new LinkedList<NetworkElement> (); 
		for (long id : ids)
		{
			NetworkElement e = np.getLinkFromId(id);
			if (e == null) e = np.getResourceFromId(id);
			if (e == null) throw new Net2PlanException ("Unknown id in the list");
			res.add(e); 
		}
		return res;
	}
	private static Map<Resource,Double> getResourceOccupationMap (NetPlan np , List<Double> resAndOccList)
	{
		Map<Resource,Double> res = new HashMap<Resource,Double> ();
		Iterator<Double> it = resAndOccList.iterator();
		while (it.hasNext())
		{
			final long id = (long) (double) it.next();
			if (!it.hasNext()) throw new Net2PlanException ("Wrong array size");
			final double val = it.next();
			final Resource r = np.getResourceFromId(id); if (r == null) throw new Net2PlanException ("Unknown resource id");
			res.put(r, val);
		}
		return res;
	}

}
