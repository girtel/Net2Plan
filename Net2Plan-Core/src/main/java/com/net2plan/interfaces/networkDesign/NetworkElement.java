/*******************************************************************************
 * Copyright (c) 2016 Pablo Pavon-Marino.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * Contributors:
 *     Pablo Pavon-Marino - Jose-Luis Izquierdo-Zaragoza, up to version 0.3.1
 *     Pablo Pavon-Marino - from version 0.4.0 onwards
 ******************************************************************************/

package com.net2plan.interfaces.networkDesign;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Sets;
import com.net2plan.internal.AttributeMap;

/**
 * <p>Class defining a generic network element.</p>
 *
 * <p>This class represents a network element. It contains bare minimum fields and methods to be considered the skeleton for other network elements wich extends this class; such as nodes, links,
 * demands, etc.
 * </p>
 */
public abstract class NetworkElement 
{
	protected NetPlan netPlan;
	final protected long id;
	protected int index;
	protected final AttributeMap attributes;
	protected final Set<String> tags;
	protected final Set<String> planningDomains;
	
	NetworkElement (NetPlan netPlan , long id , int index , String planningDomain , AttributeMap attributes) 
	{ 
		this(netPlan , id , index , planningDomain == null? null : Sets.newHashSet(planningDomain) , attributes);
	}
	NetworkElement (NetPlan netPlan , long id , int index , Set<String> planningDomains , AttributeMap attributes) 
	{ 
		this.netPlan = netPlan; 
		this.id = id; 
		this.index = index; 
		this.attributes = new AttributeMap (attributes); 
		this.tags = new HashSet<> (); 
		this.planningDomains = new HashSet<> (); 
		if (planningDomains == null)
		{
			if (!(this instanceof NetPlan || this instanceof NetworkLayer))
				throw new RuntimeException ();
		}
		else
		{
			if (this instanceof NetPlan || this instanceof NetworkLayer)
				throw new RuntimeException ();
			this.planningDomains.addAll(planningDomains);
		}
	}

	
	/**
	 * <p>Checks whether this element (demand, node, route...) is attached to a netPlan object. When negative, an exception will be thrown.</p>
	 */
	public final void checkAttachedToNetPlanObject () { if (netPlan == null) throw new Net2PlanException ("The element " + this + " is not associated to any NetPlan object"); }

	/**
	 * <p>Checks whether this element (demand, node, route...) was not already removed from the {@code NetPlan} object. When negative, an exception will be thrown.</p>
	 * @param np NetPlan object
	 */
	public final void checkAttachedToNetPlanObject (NetPlan np) { np.checkInThisNetPlan(this); if (np != this.netPlan) throw new Net2PlanException ("The element " + this + " is not associated to the given NetPlan object"); }

	void checkHasPlanningDomain (String pd) { if (!this.planningDomains.contains(pd)) throw new Net2PlanException ("Wrong planning domain"); }

	void checkHasCommonPlanningDomain (NetworkElement e) { if (e == null) throw new Net2PlanException ("Element is null"); if (Sets.intersection(this.planningDomains, e.planningDomains).isEmpty()) throw new Net2PlanException ("Wrong planning domain"); }
	
	/**
	 * <p>Return true if the Object o is an IdetifiedElement, with the same identifier and attached to the same NetPlan object</p>
	 * @param o Object to compare to
	 * @return true if it was removed
	 * @since 0.4.0
	 */
	final public boolean equals(Object o) 
	{
			return (o == this); 
	}
	
	final boolean isDeepCopy (NetworkElement e2) 
	{
		if (this.id != e2.id) return false;
		if (this.index != e2.index) return false;
		if (!this.attributes.equals(e2.attributes)) return false;
		if (!this.tags.equals (e2.tags)) return false;
		if (!this.planningDomains.equals(e2.planningDomains)) return false;
		return true;
	}

	/**
	 * <p>Returns the value of a given attribute for this network element. If not defined, the attribute is searched in the netPlan object this element is attached to.
	 * Then, it returns null if the attribute is not found also there</p>
	 *
	 * @param key Attribute name
	 * @return Attribute value (or {@code null}, if not defined)
	 * @since 0.3.0
	 */
	public String getAttribute(String key)
	{
		checkAttachedToNetPlanObject();
		String value = attributes.get(key);
		if (this instanceof NetPlan) return value;
		return value == null ? netPlan.getAttribute (key) : value;
	}

	/** Adds a tag to this network element. If the element already has this tag, nothing happens
	 * @param tag the tag
	 */
	public void addTag (String tag)
	{
		this.tags.add (tag);
		Set<NetworkElement> setElements = netPlan.cache_taggedElements.get (tag);
		if (setElements == null) { setElements = new HashSet<> (); netPlan.cache_taggedElements.put (tag , setElements); }
		setElements.add (this);
	}
	
	/** Returns true if this network element has the given tag
	 * @param tag the tag
	 * @return see above
	 */
	public boolean hasTag (String tag)
	{
		return this.tags.contains (tag);
	}

	/** Removes this tag from the network element. If the element did not have the tag, nothing happens 
	 * @param tag the tag
	 * @return true if the element had the tag before, and so it was removed from it. False, if the element did not have the tag (and thus nothing happened)
	 */
	public boolean removeTag (String tag)
	{
		final boolean removed = this.tags.remove (tag);
		if (removed)
			netPlan.cache_taggedElements.get (tag).remove (this);
		return removed;
	}
	
	/** Returns the set of tags assigned to this network element
	 * @return the set (unmodifiable)
	 */
	public Set<String> getTags ()
	{
		return Collections.unmodifiableSet(this.tags);
	}
	
	
	/**
	 * <p>Returns the element attributes (a copy)</p>
	 * @return the attribute map
	 * @since 0.4.0
	 */
	final public Map<String,String> getAttributes () { return Collections.unmodifiableMap(attributes); }

	/**
	 * <p>Returns the unique identifier</p>
	 * @return The unique id
	 * @since 0.4.0
	 */
	final public long getId () { return id; }

	/**
	 * <p>Returns the index</p>
	 * @return The index
	 */
	public int getIndex () { return index; }

	/**
	 * <p>Returns the {@code NetPlan} object to which this element is attached</p>
	 * @return The NetPlan object
	 * @since 0.4.0
	 */
	final public NetPlan getNetPlan () { return netPlan; }
	
//	final public int hashCode() { return (int) id; }

	/**
	 * <p>Removes the attribute attached to this network element. If the attribute does not exist in this network element, no action is made</p>
	 * @since 0.4.0
	 */
	final public void removeAllAttributes ()
	{ 
		checkAttachedToNetPlanObject();
		netPlan.checkIsModifiable();
		attributes.clear();
	}

	/**
	 * Removes the attribute attached to this network element. If the attribute does not exist in this network element, no action is made
	 * @param key Attribute name
	 * @since 0.4.0
	 */
	final public void removeAttribute (String key)
	{ 
		checkAttachedToNetPlanObject();
		netPlan.checkIsModifiable();
		attributes.remove(key);
	}

	/**
	 * <p>Sets an attribute for this element. If it already exists, it will be overriden.</p>
	 *
	 * @param key Attribute name
	 * @param value Attribute value
	 * @since 0.3.0
	 */
	public void setAttribute (String key, String value)
	{
		checkAttachedToNetPlanObject();
		netPlan.checkIsModifiable();
		attributes.put (key,value);
	}

	/**
	 * <p>Sets the attributes for this network element. Any previous attributes will be removed.</p>
	 * @param map Attribute where the keys are the attribute names and the values the attribute values
	 */
	public void setAttributeMap (Map<String,String> map)
	{
		checkAttachedToNetPlanObject();
		netPlan.checkIsModifiable();
		attributes.clear(); 
		if (map != null) 
			for (Map.Entry<String,String> e : map.entrySet())
				attributes.put (e.getKey() , e.getValue());
	}

	/**
	 * <p>Returns a {@code String} representation of the network element.</p>
	 * @return {@code String} representation of the network element
	 */
	public String toString ()
	{
		if (this instanceof Link) return "Link id=" + id;
		if (this instanceof NetworkLayer) return "Network layer id=" + id;
		if (this instanceof Node) return "Node id=" + id;
		if (this instanceof Demand) return "Demand id=" + id;
		if (this instanceof MulticastDemand) return "Multicast demand id=" + id;
		if (this instanceof MulticastTree) return "Multicast tree id=" + id;
		if (this instanceof Route) return "Route id=" + id;
		if (this instanceof Resource) return "Resource id=" + id;
		if (this instanceof NetPlan) return "NetPlan id=" + id + ", hashcode: " + hashCode();
		throw new RuntimeException ("Bad");
	}
	
	/**
	 * Return true if the element was already removed from the NetPlan object, and thus cannot be acccessed
	 * @return true if it was removed
	 * @since 0.4.0
	 */
	final public boolean wasRemoved () { return (netPlan == null); }

	final protected void removeIdAndFromPlanningDomain () 
	{ 
		for (String pd : this.planningDomains)
			if (!netPlan.cache_planningDomain2networkElements.get(pd).remove(this)) throw new RuntimeException ();
		this.netPlan = null;
	} // called when the element is removed from the net2plan object


	void checkCachesConsistency ()
	{
		/* Check all the tags here are in the cache */
		for (String tag : tags) if (!netPlan.cache_taggedElements.get(tag).contains (this)) throw new RuntimeException ("tag: " + tag);
	}

	
//	/** Returns the planning domain this element belongs to, empty string if none, and an exception if the element belongs to more than one
//	 * @return see above
//	 */
//	public String getPlanningDomain () 
//	{
//		if (this instanceof NetPlan) throw new Net2PlanException ("The planning domains of NetPlan object are the ones defined this addGlobalPlanningDomain function");
//		if (this instanceof NetworkLayer) throw new Net2PlanException ("The planning domains of NetworkLayer objects are the ones of its constituent elements. Use layer.getPlanningDomainsInUse method");
//		if (this instanceof Node) throw new Net2PlanException ("Node objects can have more than one planning domain, which should be retrieved with method node.getPlanningDomains");
//		if (this.planningDomains.size () != 1) throw new Net2PlanException ("Element " + this + " has more than one planning domain");  
//		return this.planningDomains.iterator().next();
//	}
	
	
	
//	/** Sets the planning domain of this network element. 
//	 * @param pd
//	 */
//	public void setPlanningDomain (String pd)
//	{
//		if (!netPlan.cache_planningDomain2networkElements.containsKey(pd)) throw new Net2PlanException ("Planning domain " + pd + " was not defined");
//		if (this instanceof NetPlan) throw new Net2PlanException ("The planning domains of NetPlan object are the ones defined this addGlobalPlanningDomain function");
//		if (this instanceof NetworkLayer) throw new Net2PlanException ("The planning domains of NetworkLayer objects are the ones of its constituent elements. Cannot be set directly");
//		if (this instanceof Node) throw new Net2PlanException ("Node objects can have more than one planning domain, which should be modified with methods node.addPlanningDomain and node.removePlanningDomain");
//		if (this.planningDomains.size () != 1) throw new Net2PlanException ("Element " + this + " has more than one planning domain");  
//
//		/* */
//		if (this instanceof Link)
//		{
//			final Node originNode;
//			final Node destinationNode;
//
//		}
//		
//		
//		this.planningDomains.clear();
//		this.planningDomains.add(pd);
//	}


	/** Returns the planning domains this element is associated to. 
	 * @return see above
	 */
	public Set<String> getPlanningDomains () { return Collections.unmodifiableSet(this.planningDomains); }

	/** Adds a planning domain to this element, if the element is already assigned to it, nothing happens
	 * @param pd the planning domain to add
	 */
	public void addPlanningDomain (String pd)
	{
		if (!netPlan.cache_planningDomain2networkElements.containsKey(pd)) throw new Net2PlanException ("Planning domain " + pd + " was not defined");
		netPlan.cache_planningDomain2networkElements.get(pd).add(this);
		this.planningDomains.add(pd);
	}

	/** The set of network elements that must have a common planning domain, with the planning domain/s of this network element.
	 * @return see above
	 */
	abstract Set<NetworkElement> getNetworkElementsDirConnectedForcedToHaveCommonPlanningDomain ();
	
}
