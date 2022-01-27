package com.net2plan.interfaces.networkDesign;

import java.util.SortedSet;

public abstract class DynamicSrgImplementation
{
    public abstract String getInitializationString ();
    public abstract void initialize (String initializationString , NetPlan np);
    public abstract SortedSet<Link> getLinksAllLayers (); // returns the links
    public abstract SortedSet<Node> getNodes (); // returns the nodes
    public abstract void remove (); // called when the Srg is removed for any clean up
}
