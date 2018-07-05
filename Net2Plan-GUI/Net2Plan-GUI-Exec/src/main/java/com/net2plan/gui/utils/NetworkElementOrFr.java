package com.net2plan.gui.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.net2plan.interfaces.networkDesign.Demand;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.MulticastDemand;
import com.net2plan.interfaces.networkDesign.MulticastTree;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkElement;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.Route;
import com.net2plan.internal.Constants.NetworkElementType;
import com.net2plan.utils.Pair;

public class NetworkElementOrFr 
{
	private final NetworkElement ne;
	private final Pair<Demand,Link> fr;
	final boolean isNe;
	public NetworkElementOrFr (NetworkElement ne) { this.ne = ne; this.fr = null; isNe = true; }
	public NetworkElementOrFr (Pair<Demand,Link> fr) { this.ne = null; this.fr = fr; isNe = false; }
	public NetworkElementOrFr (Demand d , Link e) { this.ne = null; this.fr = Pair.of(d, e); isNe = false; }
	public boolean isNe () { return isNe; }
	public boolean isFr () { return !isNe; }
	public NetworkElement getNe () { if (!isNe) throw new Net2PlanException ("Not a network element"); return ne; }
	public Pair<Demand,Link> getFr () { if (isNe) throw new Net2PlanException ("Not a forwarding rule"); return fr; }

	public Optional<NetworkElementOrFr> getTranslationToOtherNp (NetPlan newNp)
	{
		final NetworkElement newNe = ne != null? newNp.getNetworkElement(ne.getId())  : null;
		final Pair<Demand,Link> newFr = fr != null? Pair.of(newNp.getDemandFromId(fr.getFirst().getId()) , newNp.getLinkFromId (fr.getSecond().getId()))  : null; 
		if (ne == null && fr == null) return Optional.empty();
		return ne != null? Optional.of(new NetworkElementOrFr(ne)) : Optional.of(new NetworkElementOrFr(fr)); 
	}
	
	public static List<NetworkElementOrFr> createListNe (List<? extends NetworkElement> nes)
	{
		return nes.stream().filter(e->e!= null).map(e->new NetworkElementOrFr(e)).collect(Collectors.toList());
	}
//	public static List<Pair<NetworkElementOrFr,NetworkLayer>> createListNeAndEstimLayers (List<? extends NetworkElement> nes)
//	{
//		return nes.stream().map(e->{ NetworkElementOrFr nefr = new NetworkElementOrFr(e); return Pair.of(nefr, nefr.getOrEstimateLayer()); }).collect(Collectors.toList());
//	}
	public static List<NetworkElementOrFr> createListFr (List<Pair<Demand,Link>> frs)
	{
		return frs.stream().map(e->new NetworkElementOrFr(e)).collect(Collectors.toList());
	}
	public static List<NetworkElementOrFr> createList (Collection<?> es)
	{
		final List<NetworkElementOrFr> res = new ArrayList<> ();
		for (Object e : es)
		{
			if (e == null) continue;
			final boolean isPair = e instanceof Pair;
			final boolean isNe = e instanceof NetworkElement;
			if (!isPair && !isNe) continue;
			if (isPair) 
			{
				final Pair ee = (Pair) e;
				if (ee.getFirst() == null) continue;
				if (ee.getSecond() == null) continue;
				if (!(ee.getFirst() instanceof Demand)) continue;
				if (!(ee.getSecond() instanceof Link)) continue;
				res.add(new NetworkElementOrFr(Pair.of((Demand) ee.getFirst(), (Link) ee.getSecond())));
			}
			else
			{
				res.add(new NetworkElementOrFr((NetworkElement) e));
			}
		}
		return res;
	}
    public NetworkElementType getElementType()
    {
    	if (ne == null && fr == null) return null;
    	if (isFr ()) return NetworkElementType.FORWARDING_RULE;
    	return ne.getNeType();
    }

    public Object getObject () { return isFr()? fr : ne; }

    public NetworkLayer getOrEstimateLayer ()
    {
    	if (ne == null && fr == null) return null;
    	if (isFr()) return fr.getFirst().getLayer(); 
    	switch (ne.getNeType())
    	{
		case DEMAND:
			return ((Demand) ne).getLayer();
		case LAYER: return (NetworkLayer) ne;
		case LINK: 
			return ((Link) ne).getLayer();
		case MULTICAST_DEMAND:
			return ((MulticastDemand) ne).getLayer();
		case MULTICAST_TREE:
			return ((MulticastTree) ne).getLayer();
		case NETWORK:
			return ((NetPlan) ne).getNetworkLayerDefault();
		case NODE:
			return ne.getNetPlan().getNetworkLayerDefault();
		case RESOURCE:
			return ne.getNetPlan().getNetworkLayerDefault();
		case ROUTE:
			return ((Route) ne).getLayer();
		case SRG:
			return ne.getNetPlan().getNetworkLayerDefault();
		default:
			throw new Net2PlanException ();
    	}
    }
}
