package com.net2plan.niw;


import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.NetPlan;



public class WFlexAlgo extends WNet
{
	  @JsonAutoDetect(fieldVisibility = Visibility.ANY, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE)
	  public static class FlexAlgoRepository
	  {
		  public FlexAlgoRepository () {}
		  public Map<Integer , FlexAlgoProperties> mapFlexAlgoId2FlexAlgoProperties = new LinkedHashMap<> ();
	  }
	
	  @JsonAutoDetect(fieldVisibility = Visibility.ANY, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE)
	  public static class FlexAlgoProperties
	  {
		  public FlexAlgoProperties () {}
		  public int calculation0MeansSpf1MeansHeuristic;
		  public int weightType_igp0_latency1_te2;
		  public int flexAlgoIndentifier;
		  public SortedSet<String>associatedSids = new TreeSet<> ();
		  public SortedSet<Long> linkIdsIncluded = new TreeSet<> ();
		  
		  public SortedSet<Link> getLinksIncluded (NetPlan np) { return linkIdsIncluded.stream().map(e->np.getLinkFromId(e)).filter(e->e!=null).collect(Collectors.toCollection(TreeSet::new)); }
		  public boolean isLinkIncluded (Link e) { return linkIdsIncluded.contains (e.getId()); }
		  public boolean isSidIncluded (String s) { return associatedSids.contains (s); }
		  public FlexAlgoProperties addLink (Link e) { linkIdsIncluded.add(e.getId()); return this; }
		  public FlexAlgoProperties removeLink (Link e) { linkIdsIncluded.remove(e.getId()); return this; }
		  public FlexAlgoProperties addSid (String sid) { associatedSids.add(sid); return this; }
		  public FlexAlgoProperties removeSid (String sid) { associatedSids.remove(sid); return this; }
 	  }
	
    /**
     * Creates a WNet object from a NetPlan object. Does not check its consistency as a valid NIW design
     *
     * @param np see above
     */
    public WFlexAlgo(NetPlan np, int k, String calculationType, String weightType)
    {
        super(np);
        getNe().setAttribute("k", "" + k);
        getNe().setAttribute("calculationType", calculationType);
        getNe().setAttribute("weightType", weightType);
    }


    public void setLinkList()
    {

    }


    public void setSidList()
    {

    }


    public void getLinkList()
    {

    }

    public void getSidList()
    {

    }


    public boolean isMetricWeightTe()
    {
        return getNe().getAttribute("weighType").equals("te");
    }
    public boolean isMetricWeightBgp()
    {
        return getNe().getAttribute("weighType").equals("bgp");
    }
    public boolean isMetricWeightDelay()
    {
        return getNe().getAttribute("weighType").equals("delay");
    }

    public boolean isCalculationTypeSpf()
    {
        return getNe().getAttribute("calculationType").equals("spf");
    }
    public boolean isCalculationTypeHeuristic()
    {
        return getNe().getAttribute("calculationType").equals("heuristic");
    }




}
