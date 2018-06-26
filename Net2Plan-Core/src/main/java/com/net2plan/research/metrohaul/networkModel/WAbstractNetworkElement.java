/*******************************************************************************
 * Copyright (c) 2017 Pablo Pavon Marino and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the 2-clause BSD License 
 * which accompanies this distribution, and is available at
 * https://opensource.org/licenses/BSD-2-Clause
 *
 * Contributors:
 *     Pablo Pavon Marino and others - initial API and implementation
 *******************************************************************************/

package com.net2plan.research.metrohaul.networkModel;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkElement;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.Resource;

/** This base class is inherited by all the classes in the library representing relevant network elements. 
 * Provides some base implementations for common methods. It wraps an underlying Net2Plan object, providing the 
 * baseline support.
 */
public abstract class WAbstractNetworkElement implements Comparable<WAbstractNetworkElement>  
{
	/** The Net2Plan object associated to this library object
	 */
	protected final NetworkElement e;
	
	protected WAbstractNetworkElement (NetworkElement e) 
	{
		if (e == null) throw new Net2PlanException ("Null element");
		this.e = e; 
	}

	/** Returns the underlying Net2Plan object. Users should NOT modify or even read this object, since all manipulations 
	 * should be made through the provided methods in he library. This method is just provided for debugging for those users 
	 * who really know what they are doing
	 * @return see above
	 */
	public abstract NetworkElement getNe ();
	
	/** Returns the value of a user-defined attributes
	 * @param name the name of the attribute
	 * @param defaultValue if no attribute with this name exists, returns this default value
	 * @return see above
	 */
	protected String getAttributeOrDefault (String name , String defaultValue) { return e.getAttribute(name, defaultValue);  }    

	/** Returns the value associated to a user-defined attribute, as a boolean, or a default is the attribute does not exist, 
	 * or cannot be parsed to a boolen 
	 * @param name the name of the attribute
	 * @param defaultValue see above
	 * @return see above
	 */
	protected boolean getAttributeAsBooleanOrDefault (String name , Boolean defaultValue) { final String s = e.getAttribute(name); if (s == null) return defaultValue; try { return Boolean.parseBoolean (s); } catch (Exception ee) { return defaultValue; }  }    
	
	protected void setAttributeAsBoolean (String name , Boolean value) { e.setAttribute(name, value.toString()); }    
	
	protected double getAttributeAsDoubleOrDefault (String name , Double defaultValue) { return e.getAttributeAsDouble(name, defaultValue); }    

	protected int getAttributeAsIntegerOrDefault (String name , Integer defaultValue) { final String s = e.getAttribute(name); if (s == null) return defaultValue; try { return Integer.parseInt(s); } catch (Exception ee) { return defaultValue; }  }    
	
	protected List<Double> getAttributeAsListDoubleOrDefault (String name , List<Double> defaultValue) { return e.getAttributeAsDoubleList(name, defaultValue);}    
	
	protected SortedSet<Integer> getAttributeAsSortedSetIntegerOrDefault (String name , SortedSet<Integer> defaultValue) { final List<Double> res = e.getAttributeAsDoubleList(name , null); if (res == null) return defaultValue; return res.stream().map(nn->new Integer(nn.intValue())).collect(Collectors.toCollection(TreeSet::new));  }    

	NetworkLayer getIpNpLayer () { return e.getNetPlan().getNetworkLayer(1); }
	NetworkLayer getWdmNpLayer () { return e.getNetPlan().getNetworkLayer(0); }

	/** Returns the underlying net2plan object that is giving support for this network. 
	 * Regular users should NOT access the underlying NetPlan objtec in any form. This method is just provided for debugging 
	 * for those users who really know what they are doing 
	 * @return see above
	 */
	public NetPlan getNetPlan () { return e.getNetPlan(); }

	/** Returns true if this element was removed from the network
	 * @return see above
	 */
	public boolean wasRemoved () { return e.wasRemoved(); }

	/** Returns an internally generated id associated with this object, that is unique among all network elements of 
	 * all types in this network
	 * @return see above
	 */
	public long getId () { return e.getId(); }

	/** Returns the network object that this element belongs to
	 * @return see above
	 */
	public WNet getNet () { return new WNet (getNetPlan()); }

	/** Returns true if this element is an VNF instance
	 * @return see above
	 */
	public boolean isVnfInstance () { return this instanceof WVnfInstance; }

	/** Returns true if this element is a WDM fiber
	 * @return see above
	 */
	public boolean isFiber () { return this instanceof WFiber; }
	
	/** Returns true if this element is an IP link
	 * @return see above
	 */
	public boolean isWIpLink () { return this instanceof WIpLink; }

	/** Returns true if this element is an IP layer 
	 * @return see above
	 */
	public boolean isLayerIp () { return this instanceof WLayerIp; }
	
	/** Returns true if this element is a WDM layer element
	 * @return see above
	 */
	public boolean isLayerWdm () { return this instanceof WLayerWdm; }

	/** Returns true if this element is a lightpath request element
	 * @return see above
	 */
	public boolean isLightpathRequest () { return this instanceof WLightpathRequest; }

	/** Returns true if this element is a lightpath unregenerated (without intermediate regenerations)
	 * @return see above
	 */
	public boolean isLightpathUnregenerated () { return this instanceof WLightpathUnregenerated; }

	/** Returns true if this element is a network node 
	 * @return see above
	 */
	public boolean isNode () { return this instanceof WNode; }

	/** Returns true if this element is a service chain request
	 * @return see above
	 */
	public boolean isServiceChainRequest () { return this instanceof WServiceChainRequest; }

	/** Returns true if this element is a service chain 
	 * @return see above
	 */
	public boolean isServiceChain () { return this instanceof WServiceChain; }

	/** Casts this element to a WFiber object (fails if it is not an object of such type)
	 * @return see above
	 */
	public WFiber getAsFiber () { return (WFiber) this; }
	
	/** Casts this element to a WIpLink object (fails if it is not an object of such type)
	 * @return see above
	 */
	public WIpLink getAsIpLink () { return (WIpLink) this; }
	
	/** Casts this element to a WLayerIp object (fails if it is not an object of such type)
	 * @return see above
	 */
	public WLayerIp getAsIpLayer () { return (WLayerIp) this; }
	
	/** Casts this element to a WLayerWdm object (fails if it is not an object of such type)
	 * @return see above
	 */
	public WLayerWdm getAsWdmLayer () { return (WLayerWdm) this; }

	/** Casts this element to a WLightpathRequest object (fails if it is not an object of such type)
	 * @return see above
	 */
	public WLightpathRequest getAsLightpathRequest () { return (WLightpathRequest) this; }

	/** Casts this element to a WLightpathUnregenerated object (fails if it is not an object of such type)
	 * @return see above
	 */
	public WLightpathUnregenerated getAsLightpathUnregenerated () { return (WLightpathUnregenerated) this; }

	/** Casts this element to a WNode object (fails if it is not an object of such type)
	 * @return see above
	 */
	public WNode getAsNode () { return (WNode) this; }
	
	/** Casts this element to a WServiceChain object (fails if it is not an object of such type)
	 * @return see above
	 */
	public WServiceChain getAsServiceChain () { return (WServiceChain) this; }

	/** Casts this element to a WServiceChainRequest object (fails if it is not an object of such type)
	 * @return see above
	 */
	public WServiceChainRequest getAsServiceChainRequest () { return (WServiceChainRequest) this; }

	/** Casts this element to a WVnfInstance object (fails if it is not an object of such type)
	 * @return see above
	 */
	public WVnfInstance getAsVnfInstance () { return (WVnfInstance) this; }

	@Override
    public final boolean equals(Object obj)
    {
        if (obj == null) return false;
        if (this == obj) return true;
        if (getClass() != obj.getClass()) return false;
        WAbstractNetworkElement other = (WAbstractNetworkElement) obj;
        if (e == null)
        {
            if (other.e != null) return false;
        } else if (!e.equals(other.e)) return false;
        return true;
    }

    @Override
    public final int compareTo(WAbstractNetworkElement o)
	{
	    if (this.equals(o)) return 0;
	    if (o == null) throw new NullPointerException ();
	    if (this.getNetPlan() != o.getNetPlan()) throw new Net2PlanException ("Different netplan object!");
	    return Long.compare(this.getId(), o.getId());
	}
}
