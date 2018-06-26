package com.net2plan.interfaces.networkDesign.dynamicSrgs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;

import com.net2plan.interfaces.networkDesign.DynamicSrgImplementation;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.Node;
import com.sun.tools.javac.util.Pair;

public class DynamicSrgAllBidiLinksTwoNodes extends DynamicSrgImplementation
{
    private Node a,b;
    private NetworkLayer layer;
    private String initializationString;
    
    public SortedSet<Node> getEndNodes () { return new TreeSet<> (Arrays.asList(a,b)); }
    public Pair<Node,Node> getPair () { return Pair.of(a, b); }
    public Node getA () { return a; }
    public Node getB () { return b; }

    @Override
    public String getInitializationString () { return initializationString; } 
    
    @Override
    public void initialize (String initializationString , NetPlan np
            )
    {
        this.initializationString = initializationString;
        final String [] vals = initializationString.split(" ");
        if (vals.length != 3) throw new Net2PlanException ("Wrong format of the input string");
        try
        {
            this.a = np.getNodeFromId(Long.parseLong(vals [0]));
            this.b = np.getNodeFromId(Long.parseLong(vals [1]));
            this.layer = np.getNetworkLayer(Integer.parseInt(vals [2]));
        } catch (Exception e) { throw new Net2PlanException ("Wrong format of the input string"); }
    }
    
    @Override
    public SortedSet<Link> getLinksAllLayers ()
    {
        final NetPlan np = a.getNetPlan();
        if (np == null || b.getNetPlan() == null || layer.getNetPlan() == null) return new TreeSet<> ();
        assert np == b.getNetPlan();
        assert np == layer.getNetPlan();
        final SortedSet<Link> res_ab = np.getNodePairLinks(a, b, false, layer);
        for (Link e : new ArrayList<> (res_ab))
            if (e.isBidirectional())
                res_ab.add(e.getBidirectionalPair());
        return res_ab;
    }
    @Override
    public SortedSet<Node> getNodes () { return new TreeSet<> (); }
    @Override
    public void remove () {}
}
