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

package com.net2plan.interfaces.networkDesign;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;

import com.net2plan.internal.AttributeMap;
import com.net2plan.internal.Constants.NetworkElementType;
import com.net2plan.utils.StringUtils;

import cern.colt.matrix.tdouble.DoubleFactory2D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;

/**
 * <p>Class defining a generic network element.</p>
 *
 * <p>This class represents a network element. It contains bare minimum fields and methods to be considered the skeleton for other network elements wich extends this class; such as nodes, links,
 * demands, etc.
 * </p>
 */
public abstract class NetworkElement implements Comparable<NetworkElement>
{
	private final static String MATRIX_COLSEPARATOR = " ";
	private final static String MATRIX_ROWSEPARATOR = ";";

	protected NetPlan netPlan;
	final protected long id;
	protected int index;
	protected final AttributeMap attributes;
	protected final SortedSet<String> tags;
	protected String description;
    protected String name;
    private final NetworkElementType elementType;
    
	NetworkElement (NetPlan netPlan , long id , int index , AttributeMap attributes) 
	{ 
		this.netPlan = netPlan; 
		this.id = id; 
		this.index = index; 
		this.attributes = new AttributeMap (attributes);
        this.name = "";
		this.description = "";
		this.tags = new TreeSet<> (); 
		this.elementType = NetworkElement.getNetworkElementType(this);
	}

	public NetworkElementType getNeType () { return this.elementType; }
	
	/** Sets this element name
	 * @param name  see above
	 */
	public final void setName (String name) { this.name = name == null? "" : name; }

	/** Returns current element name String
	 * @return see above
	 */
	public final String getName () { return this.name; }
	
    /** Sets this element description
     * @param description  see above
     */
    public final void setDescription (String description) { this.description = description == null? "" : description; }

    /** Returns current element description String
     * @return see above
     */
    public final String getDescription () { return this.description; }

    /**
	 * <p>Checks whether this element (demand, node, route...) is attached to a netPlan object. When negative, an exception will be thrown.</p>
	 */
	public final void checkAttachedToNetPlanObject () { if (netPlan == null) throw new Net2PlanException ("The element " + this + " is not associated to any NetPlan object"); }

	/**
	 * <p>Checks whether this element (demand, node, route...) was not already removed from the {@code NetPlan} object. When negative, an exception will be thrown.</p>
	 * @param np NetPlan object
	 */
	public final void checkAttachedToNetPlanObject (NetPlan np) { np.checkInThisNetPlan(this); if (np != this.netPlan) throw new Net2PlanException ("The element " + this + " is not associated to the given NetPlan object"); }

//	void checkHasPlanningDomain (String pd) { if (!this.planningDomains.contains(pd)) throw new Net2PlanException ("Wrong planning domain"); }
//
//	void checkHasCommonPlanningDomain (NetworkElement e) { if (e == null) throw new Net2PlanException ("Element is null"); if (Sets.intersection(this.planningDomains, e.planningDomains).isEmpty()) throw new Net2PlanException ("Wrong planning domain"); }
	
	/**
	 * <p>Return true if the Object o is an IdetifiedElement, with the same identifier and attached to the same NetPlan object</p>
	 * @param o Object to compare to
	 * @return true if it was removed
	 * @since 0.4.0
	 */
	@Override
    final public boolean equals(Object o) 
	{
			return (o == this); 
	}
	
	final public int compareTo(NetworkElement o)
	{
		if (this.equals(o)) return 0;
		if (o == null) throw new NullPointerException();
//		if (this.netPlan != o.getNetPlan())
//			throw new Net2PlanException ("Different N2p object");
		return Long.compare(this.id , o.id);
	}
	
	final boolean isDeepCopy (NetworkElement e2) 
	{
		if (this.id != e2.id) return false;
		if (this.index != e2.index) return false;
		if (!this.attributes.equals(e2.attributes)) return false;
		if (!this.tags.equals (e2.tags)) return false;
		if (!this.name.equals(e2.name)) return false;
        if (!this.description.equals(e2.description)) return false;
//		if (!this.planningDomains.equals(e2.planningDomains)) return false;
		return true;
	}

	/**
	 * Returns the value of a given attribute for this network element, or null if not found. 
	 * @param key Attribute name
	 * @return Attribute value (or {@code null}, if not defined)
	 */
	public String getAttribute(String key)
	{
		checkAttachedToNetPlanObject();
		return attributes.get(key);
	}

	/**
	 * Returns the value of a given attribute for this network element, or the default value provided if not found 
	 * @param key Attribute name
	 * @param defaultValue default value to return if not found
	 * @return Attribute value (or {@code null}, if not defined)
	 */
	public String getAttribute(String key , String defaultValue)
	{
		final String val = attributes.get(key);
		return val == null? defaultValue : val;
	}
	
	/**
	 * Returns the value of a given attribute for this network element 
	 * @param key Attribute name
	 * @param defaultValue default value to return if not found, or could not be parsed
	 * @return see above
	 */
	public Double getAttributeAsDouble (String key , Double defaultValue)
	{
		final String val = attributes.get(key);
		if (val == null) return defaultValue;
		try 
		{
			return Double.parseDouble(val);
		} catch (Exception ee) { return defaultValue; }
	}
	
	/**
	 * Returns the value of a given attribute for this network element, in form of a matrix, as stored using the setAttributeAsDoubleMatrix method
	 * @param key Attribute name
	 * @param defaultValue default value to return if not found, or could not be parsed
	 * @return see above
	 */
	public DoubleMatrix2D getAttributeAsDoubleMatrix (String key , DoubleMatrix2D defaultValue)
	{
		checkAttachedToNetPlanObject();
		final String val = attributes.get(key);
		if (val == null) return defaultValue;  
		try 
		{
			final String [] rows = val.split(MATRIX_ROWSEPARATOR,-1);
			final List<List<Double>> res = new ArrayList<> (rows.length);
			int numCols = 0;
			for (String row : rows)
			{
				if (row.equals("")) continue;
				final List<Double> rowVals = new LinkedList<> ();
				res.add(rowVals);
    			for (String cell : row.split(MATRIX_COLSEPARATOR,-1))
    			{
    				if (cell.equals("")) continue;
    				rowVals.add(Double.parseDouble(cell));
    			}
    			numCols = Math.max(numCols, rowVals.size());
			}
			final DoubleMatrix2D resMatrix = DoubleFactory2D.dense.make(res.size() , numCols);
			for (int row = 0; row < res.size() ; row ++)
				for (int col = 0 ; col < res.get(row).size() ; col ++)
					resMatrix.set(row, col, res.get(row).get(col));
			return resMatrix;
		} catch (Exception ee) { return defaultValue; }
	}

	   /**
     * Returns the value of a given attribute for this network element, in form of a matrix, as stored using the setAttributeAsDoubleMatrix method
     * @param key Attribute name
     * @param defaultValue default value to return if not found, or could not be parsed
     * @return see above
     */
    public List<List<Double>> getAttributeAsDoubleMatrix (String key , List<List<Double>> defaultValue)
    {
        checkAttachedToNetPlanObject();
        final String val = attributes.get(key);
        if (val == null) return defaultValue;  
        try 
        {
            final String [] rows = val.split(MATRIX_ROWSEPARATOR,-1);
            final List<List<Double>> res = new ArrayList<> (rows.length);
            for (String row : rows)
            {
                if (row.equals("")) continue;
                final List<Double> rowVals = new LinkedList<> ();
                res.add(rowVals);
                for (String cell : row.split(MATRIX_COLSEPARATOR,-1))
                {
                    if (cell.equals("")) continue;
                    rowVals.add(Double.parseDouble(cell));
                }
            }
            return res;
        } catch (Exception ee) { return defaultValue; }
    }

	
	/**
	 * Returns the value of a given attribute for this network element, in form of a list of doubles, as stored using the setAttributeAsDoubleList method
	 * @param key Attribute name
	 * @param defaultValue default value to return if not found, or could not be parsed
	 * @return see above
	 */
	public List<Double> getAttributeAsDoubleList (String key , List<Double> defaultValue)
	{
		checkAttachedToNetPlanObject();
		final String val = attributes.get(key);
		if (val == null) return defaultValue;  
		try 
		{
			final String [] parts = val.split(MATRIX_COLSEPARATOR,-1);
			final List<Double> res = new ArrayList<> (parts.length);
			for (String part : parts)
			{
				if (part.equals("")) continue;
				res.add(Double.parseDouble(part));
			}
			return res;
		} catch (Exception ee) { ee.printStackTrace();return defaultValue; }
	}

	/**
	 * Returns the value of a given attribute for this network element, in form of a list of strings, as stored using the setAttributeAsStringList method
	 * @param key Attribute name
	 * @param defaultValue default value to return if not found, or could not be parsed
	 * @return see above
	 */
	public List<String> getAttributeAsStringList (String key , List<String> defaultValue)
	{
		checkAttachedToNetPlanObject();
		final String val = attributes.get(key);
		if (val == null) return defaultValue;  
		return StringUtils.readEscapedString_asStringList (val , defaultValue);
	}
	
	/**
	 * Returns the value of a given attribute for this network element, in form of a list of list of strings, as stored using the setAttributeAsStringMatrix method
	 * @param key Attribute name
	 * @param defaultValue default value to return if not found, or could not be parsed
	 * @return see above
	 */
	public List<List<String>> getAttributeAsStringMatrix (String key , List<List<String>> defaultValue)
	{
		checkAttachedToNetPlanObject();
		final String val = attributes.get(key);
		if (val == null) return defaultValue;  
		if (val.equals("")) return new ArrayList<> ();
		try 
		{
			final String [] rows = val.split(MATRIX_ROWSEPARATOR,-1);
			final List<List<String>> res = new ArrayList<> (rows.length);
			int numCols = 0;
			for (String row : rows)
			{
				//if (row.equals("")) continue;
				final List<String> rowVals = new LinkedList<> ();
				res.add(rowVals);
    			for (String cell : row.split(MATRIX_COLSEPARATOR,-1))
    			{
    				//if (cell.equals("")) continue;
    				rowVals.add(StringUtils.unescapedStringRead(cell));
    			}
    			numCols = Math.max(numCols, rowVals.size());
			}
			return res;
		} catch (Exception ee) { return defaultValue; }
	}

	/** Adds a tag to this network element. If the element already has this tag, nothing happens
	 * @param tag the tag
	 */
	public void addTag (String tag)
	{
		this.tags.add (tag);
		SortedSet<NetworkElement> setElements = netPlan.cache_taggedElements.get (tag);
		if (setElements == null) { setElements = new TreeSet<> (); netPlan.cache_taggedElements.put (tag , setElements); }
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
	public SortedSet<String> getTags ()
	{
		return Collections.unmodifiableSortedSet(this.tags);
	}
	
	
	/**
	 * <p>Returns the element attributes (a copy)</p>
	 * @return the attribute map
	 * @since 0.4.0
	 */
	final public SortedMap<String,String> getAttributes () { return Collections.unmodifiableSortedMap(attributes); }

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
	 * <p>Sets an attribute for this element, so it can be read with getAttributeAsNumber method. If it already exists, previous value is lost.</p>
	 *
	 * @param key Attribute name
	 * @param value Attribute value
	 */
	public void setAttribute (String key, Number value)
	{
		checkAttachedToNetPlanObject();
		netPlan.checkIsModifiable();
		attributes.put (key,value.toString());
	}

	/**
	 * Sets an attribute for this element, storing the values of the given value list, so it can be read with getAttributeAsNumberList method.
	 * If it already exists, previous value is lost.  
	 * @param key Attribute name
	 * @param valueList Attribute value
	 */
	public void setAttributeAsNumberList (String key, List<? extends Number> valueList)
	{
		checkAttachedToNetPlanObject();
		netPlan.checkIsModifiable();
		final StringBuffer st = new StringBuffer ();
		boolean firstTime = true;
		for (Number val : valueList)
		{
			if (firstTime) { firstTime = false; } else { st.append(MATRIX_COLSEPARATOR); }
			st.append(val.toString()); 
		}
		attributes.put (key,st.toString());
	}

	/**
	 * Sets an attribute for this element, storing the values of the given value list, so it can be read with getAttributeAsStringList method. 
	 * If it already exists, previous value is lost.
	 *
	 * @param key Attribute name
	 * @param vals Attribute vals
	 */
	public void setAttributeAsStringList (String key, List<String> vals)
	{
		checkAttachedToNetPlanObject();
		netPlan.checkIsModifiable();
		attributes.put (key, StringUtils.createEscapedString_asStringList (vals));
	}

	/**
	 * Sets an attribute for this element, storing the values of the given value list, 
	 * so it can be read with getAttributeAsStringMatrix method. 
	 * If it already exists, previous value is lost.  
	 * @param key Attribute name
	 * @param vals Attribute vals
	 */
	public void setAttributeAsStringMatrix (String key, List<List<String>> vals)
	{
		if (vals.isEmpty()) { attributes.put (key,""); return; }
		for (List<String> row : vals) if (row.isEmpty()) throw new Net2PlanException ("One of the rows of the matrix is empty");
		checkAttachedToNetPlanObject();
		netPlan.checkIsModifiable();
		final StringBuffer st = new StringBuffer ();
		boolean firstRow = true;
		for (List<String> row : vals)
		{
			if (firstRow) { firstRow = false; } else { st.append(MATRIX_ROWSEPARATOR); }
			boolean firstColumn = true;
			for (String cell : row)
			{
				if (firstColumn) { firstColumn = false; } else { st.append(MATRIX_COLSEPARATOR); }
				st.append(StringUtils.escapedStringToWrite(cell));
			}
		}
		attributes.put (key,st.toString());
	}

	/**
	 * Sets an attribute for this element, storing the values of the given matrix, so it can be read with setAttributeAsNumberMatrix method.
	 * If it already exists, previous value is lost. 
	 * @param key Attribute name
	 * @param vals Attribute vals
	 */
	public void setAttributeAsNumberMatrix (String key, DoubleMatrix2D vals)
	{
		checkAttachedToNetPlanObject();
		netPlan.checkIsModifiable();
		final StringBuffer st = new StringBuffer ();
		for (int row = 0; row < vals.rows() ; row ++)
		{
    		for (int col = 0; col < vals.columns() ; col ++)
    		{
    			st.append(vals.get(row, col));
    			if (col != vals.columns()-1) st.append(MATRIX_COLSEPARATOR);
    		}
			if (row != vals.rows()-1) st.append(MATRIX_ROWSEPARATOR);
		}
		attributes.put (key,st.toString());
	}
	
    /**
     * Sets an attribute for this element, storing the values of the given matrix, so it can be read with setAttributeAsNumberMatrix method.
     * If it already exists, previous value is lost. 
     * @param key Attribute name
     * @param vals Attribute vals
     */
    public void setAttributeAsNumberMatrix (String key, List<List<Number>> vals)
    {
        checkAttachedToNetPlanObject();
        netPlan.checkIsModifiable();
        final StringBuffer st = new StringBuffer ();
        for (int row = 0; row < vals.size() ; row ++)
        {
            final List<Number> rowVals = vals.get(row);
            for (int col = 0; col < rowVals.size() ; col ++)
            {
                st.append(rowVals.get(col));
                if (col != rowVals.size()-1) st.append(MATRIX_COLSEPARATOR);
            }
            if (row != vals.size()-1) st.append(MATRIX_ROWSEPARATOR);
        }
        attributes.put (key,st.toString());
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
			for (SortedMap.Entry<String,String> e : map.entrySet())
				attributes.put (e.getKey() , e.getValue());
	}

	/**
	 * <p>Returns a {@code String} representation of the network element.</p>
	 * @return {@code String} representation of the network element
	 */
	@Override
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

	final protected void removeId () 
	{ 
		this.netPlan = null;
	} // called when the element is removed from the net2plan object


	void checkCachesConsistency ()
	{
		/* Check all the tags here are in the cache */
		for (String tag : tags) if (!netPlan.cache_taggedElements.get(tag).contains (this)) throw new RuntimeException ("tag: " + tag);
	}

    private static NetworkElementType getNetworkElementType(NetworkElement e)
    {
        if (e instanceof Node) return NetworkElementType.NODE;
        if (e instanceof Link) return NetworkElementType.LINK;
        if (e instanceof Demand) return NetworkElementType.DEMAND;
        if (e instanceof MulticastDemand) return NetworkElementType.MULTICAST_DEMAND;
        if (e instanceof MulticastTree) return NetworkElementType.MULTICAST_TREE;
        if (e instanceof Route) return NetworkElementType.ROUTE;
        if (e instanceof SharedRiskGroup) return NetworkElementType.SRG;
        if (e instanceof NetworkLayer) return NetworkElementType.LAYER;
        if (e instanceof Resource) return NetworkElementType.RESOURCE;
        if (e instanceof NetPlan) return NetworkElementType.NETWORK;
        throw new RuntimeException ();
    }


	
}
