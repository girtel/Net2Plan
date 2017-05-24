
//1) Repasar esto
//2) Que todos los cambios impliquen update: anadir cambios de factores, completar todos
//3) las rutas no se pueden anadir aqui: aqui solo tenemos una cache, es en las rutas donde se fija esto.
//Ojo que al cambiar la secuencia de enlaces, hay que cambiar tambien los recursos ocupados. SI una demanda es de una
//service chain, todas sus rutas deben atravesar recursos en cualquier estado, y no pueden tener protection segments

// rutas: al hacer addroute, se chequea si hay service chain. En ese caso, hay que pasarle en constructor info de
//recursos ocupados (lista pares recurso-ocupacion). La ruta comprueba que van en orden con demanda, y con lista enlaces

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

import com.net2plan.internal.AttributeMap;
import com.net2plan.internal.ErrorHandling;

import java.net.URL;
import java.util.*;
import java.util.Map.Entry;

/** <p>.</p> 
 * @author Pablo Pavon-Marino
 */
public class Resource extends NetworkElement
{
	/* this information never changes after creation */
	Node hostNode; // never changes after created, but with copyFrom
	String capacityMeasurementUnits; // never changes after created, but with copyFrom
	String type; // never changes after created, but with copyFrom
	double processingTimeToTraversingTrafficInMs;
	URL urlIcon;
	
	/* this information can change after creation */
	String name; // descriptive name of the resource. Can change.
	Map<Resource,Double> capacityUpperResourcesOccupyInMe;
	Map<Resource , Double> capacityIOccupyInBaseResource; // capacity can change, but no new resources can be put (if not, there is danger of loops!!) 
	double capacity;
	double cache_totalOccupiedCapacity;
	Map<Route,Double> cache_traversingRoutesAndOccupiedCapacitiesIfNotFailingRoute;
	
	Resource (NetPlan netPlan , long id , int index , String type , String name , Node hostNode , 
			double capacity , String capacityMeasurementUnits,
			Map<Resource,Double> capacityIOccupyInBaseResource , double processingTimeToTraversingTraffic , 
			AttributeMap attributes)
	{
		super (netPlan , id , index , attributes);

		if (!netPlan.equals(hostNode.netPlan)) throw new Net2PlanException ("The Resource host node is in a different NetPlan object (or removed)"); 
		if (capacity < 0) throw new Net2PlanException ("The capacity of a resource cannot be negative");
		if (processingTimeToTraversingTraffic < 0)throw new Net2PlanException ("The processing time for the traversing traffic cannot be negative");
		if (capacityIOccupyInBaseResource == null) capacityIOccupyInBaseResource = new HashMap<Resource,Double> (); 
		for (Entry<Resource,Double> resPolicyInfo : capacityIOccupyInBaseResource.entrySet())
		{
			final Resource r = resPolicyInfo.getKey();
			if (!netPlan.equals(r.netPlan)) throw new Net2PlanException ("The consumed resources of a resource must be in the same NetPlan object");
			if (r.hostNode != hostNode) throw new Net2PlanException ("All the resources consumed by a resource must be in the same node resource");
			if (resPolicyInfo.getValue() < 0) throw new Net2PlanException ("The consumed capacity in base resource cannot be negative");
		}
		this.type = type;
		this.name = name;
		this.hostNode = hostNode;
		this.capacityMeasurementUnits = capacityMeasurementUnits;
		this.capacity = capacity;
		this.cache_totalOccupiedCapacity = 0;
		this.processingTimeToTraversingTrafficInMs = processingTimeToTraversingTraffic;
		this.capacityUpperResourcesOccupyInMe = new HashMap<Resource,Double> ();
		this.capacityIOccupyInBaseResource = new HashMap<Resource,Double> (capacityIOccupyInBaseResource);
		this.urlIcon = null;
		for (Entry<Resource,Double> entry : this.capacityIOccupyInBaseResource.entrySet())
		{		
			entry.getKey().capacityUpperResourcesOccupyInMe.put(this , entry.getValue());
			entry.getKey().updateTotalOccupiedCapacity();
		}
		this.cache_traversingRoutesAndOccupiedCapacitiesIfNotFailingRoute = new HashMap<Route,Double> ();
	}

	void copyFrom (Resource origin)
	{
		if ((this.id != origin.id) || (this.index != origin.index)) throw new RuntimeException ("Bad");
		if ((this.netPlan == null) || (origin.netPlan == null) || (this.netPlan == origin.netPlan)) throw new RuntimeException ("Bad");
		this.type = origin.type;
		this.name = origin.name;
		this.hostNode = this.netPlan.getNodeFromId (origin.hostNode.id);
		this.capacityMeasurementUnits = origin.capacityMeasurementUnits;
		this.capacity = origin.capacity;
		this.cache_totalOccupiedCapacity = origin.cache_totalOccupiedCapacity;
		this.processingTimeToTraversingTrafficInMs = origin.processingTimeToTraversingTrafficInMs;
		this.urlIcon = origin.urlIcon;
		this.capacityUpperResourcesOccupyInMe = new HashMap<Resource,Double> ();
		for (Entry<Resource,Double> entry : origin.capacityUpperResourcesOccupyInMe.entrySet())
		{
			final Resource resourceThisNp = this.netPlan.getResourceFromId(entry.getKey().id);
			if (resourceThisNp == null) throw new RuntimeException ("Bad");
			this.capacityUpperResourcesOccupyInMe.put(resourceThisNp , entry.getValue());
		}
		this.capacityIOccupyInBaseResource = new HashMap<Resource,Double> ();
		for (Entry<Resource,Double> entry : origin.capacityIOccupyInBaseResource.entrySet())
		{
			final Resource resourceThisNp = this.netPlan.getResourceFromId(entry.getKey().id);
			if (resourceThisNp == null) throw new RuntimeException ("Bad");
			this.capacityIOccupyInBaseResource.put(resourceThisNp , entry.getValue());
		}
		this.cache_traversingRoutesAndOccupiedCapacitiesIfNotFailingRoute = new HashMap<Route,Double> ();
		for (Entry<Route,Double> originRoute : origin.cache_traversingRoutesAndOccupiedCapacitiesIfNotFailingRoute.entrySet())
		{
			final Route routeThisNp = this.netPlan.getRouteFromId(originRoute.getKey().id);
			if (routeThisNp == null) throw new RuntimeException ("Bad");
			this.cache_traversingRoutesAndOccupiedCapacitiesIfNotFailingRoute.put(this.netPlan.getRouteFromId(originRoute.getKey().id) , originRoute.getValue());
		}
	}

	boolean isDeepCopy (Resource r2)
	{
		if (!super.isDeepCopy(r2)) return false;
		if (this.hostNode.id != r2.hostNode.id) return false;
		if (!this.capacityMeasurementUnits.equals(r2.capacityMeasurementUnits)) return false;
		if (!this.type.equals(r2.type)) return false;
		if (!this.name.equals(r2.name)) return false;
		if (this.processingTimeToTraversingTrafficInMs != r2.processingTimeToTraversingTrafficInMs) return false;
		if (this.capacity != r2.capacity) return false;
		if  ((this.urlIcon == null) != (r2.urlIcon == null)) return false;
		if (this.urlIcon != null) if (!this.urlIcon.equals(r2.urlIcon)) return false;
		if (!NetPlan.isDeepCopy(this.capacityIOccupyInBaseResource , r2.capacityIOccupyInBaseResource)) return false;
		if (!NetPlan.isDeepCopy(this.capacityUpperResourcesOccupyInMe , r2.capacityUpperResourcesOccupyInMe)) return false;
		if (!NetPlan.isDeepCopy(this.cache_traversingRoutesAndOccupiedCapacitiesIfNotFailingRoute , r2.cache_traversingRoutesAndOccupiedCapacitiesIfNotFailingRoute)) return false;
		return true;
	}
	
	/**
	 * <p>Returns resource link utilization, measured as the ratio between the total occupied capacity in the total capacity.</p>
	 * @return The utilization as described above. If the resource has zero capacity and strictly positive occupied capacity, Double.POSITIVE_INFINITY is returned 
	 * */
	public double getUtilization()
	{
		if ((capacity == 0) && (cache_totalOccupiedCapacity > 0)) return Double.POSITIVE_INFINITY;
		return capacity == 0? 0 : cache_totalOccupiedCapacity / capacity;
	}

	/** Returns true if the occupied capacity of the resource exceeds its capacity
	 * @return See above
	 */
	public boolean isOversubscribed ()
	{
		final double PRECISION_FACTOR = Double.parseDouble(Configuration.getOption("precisionFactor"));
		return capacity + PRECISION_FACTOR < cache_totalOccupiedCapacity;
	}
	
	/** Gets the processing time added to every traversing traffic
	 * @return the time
	 */
	public double getProcessingTimeToTraversingTrafficInMs ()
	{
		return processingTimeToTraversingTrafficInMs;
	}

	/** Sets the processing time added to every traversing traffic
	 * @param time the new processing time (cannot be negative)
	 */
	public void setProcessingTimeToTraversingTrafficInMs (double time)
	{
		if (time < 0) throw new Net2PlanException ("The processing time cannot be negative");
		this.processingTimeToTraversingTrafficInMs = time;
	}

	/** Returns the url of the icon specified by the user to represent this resource, or null if none
	 * @return the url
	 */
	public URL getUrlIcon ()
	{
		return urlIcon;
	}
	
	/** Sets the url of the icon specified by the user to represent this resource. A null value removes current URL
	 * @param url the url
	 */
	public void setUrlIcon (URL url)
	{
		this.urlIcon = url;
	}

	/** Returns the String describing the type of the node
	 * @return the type
	 */
	public String getType() 
	{
		return type;
	}

	/** Returns the name of the resource
	 * @return the name
	 */
	public String getName() 
	{
		return name;
	}

	/** Returns the host node of this resource
	 * @return the hostNode
	 */
	public Node getHostNode() 
	{
		return hostNode;
	}

	/** Gets the units in which the capacity of this resource is measured
	 * @return the capacityMeasurementUnits
	 */
	public String getCapacityMeasurementUnits() 
	{
		return capacityMeasurementUnits;
	}
	
	/** Sets the units in which the capacity of this resource is measured
	 * @param units see above
	 */
	public void setCapacityMeasurementUnits(String units) 
	{
		this.capacityMeasurementUnits = units;
	}
	
	/** Gets the capacity in resource units of this resource
	 * @return the capacity
	 */
	public double getCapacity() 
	{
		return capacity;
	}

	/** Gets the occupied capacity in resource units of this resource
	 * @return the capacity
	 */
	public double getOccupiedCapacity() 
	{
		return cache_totalOccupiedCapacity;
	}

	/** Sets the name of the resource
	 * @param name The name
	 */
	public void setName(String name) 
	{
		this.name = name;
	}

	/** Returns the set of base resources of this resource
	 * @return the set of base resources (an unmodificable set)
	 */
	public Set<Resource> getBaseResources ()
	{
		return Collections.unmodifiableSet(capacityIOccupyInBaseResource.keySet());
	}

	/** Returns the set of resources that are above of this resource, so this resource is a base resource for them
	 * @return the set of upper resources (an unmodificable set)
	 */
	public Set<Resource> getUpperResources ()
	{
		return Collections.unmodifiableSet(capacityUpperResourcesOccupyInMe.keySet());
	}


	/** Returns the capacity that this resource is occupying in the base resource. If bsaeResource is not a base resource for this resource,
	 *  the method returns zero
	 * @param baseResource The base resource
	 * @return the occupied capacity
	 */
	public double getCapacityOccupiedInBaseResource (Resource baseResource)
	{
		Double info = capacityIOccupyInBaseResource.get(baseResource);
		if (info == null) return 0; else return info;
	}

	/** Returns the capacity that upperResource occupied in this resource. If thi resource is not a base for upperResource, 0 is returned
	 * @param upperResource the resource for which this resource is a base resource
	 * @return the occupied capacity in this resource by the given upper resource 
	 */
	public double getCapacityOccupiedByUpperResource (Resource upperResource)
	{
		Double info = capacityUpperResourcesOccupyInMe.get(upperResource);
		if (info == null) return 0; else return info;
	}

	/** Returns the map with an element for each base resource, and the key the amount of capacity occupied in it
	 * @return the map
	 */
	public Map<Resource, Double> getCapacityOccupiedInBaseResourcesMap() 
	{
		return Collections.unmodifiableMap(capacityIOccupyInBaseResource);
	}

	/** Returns the map with the information on the upper resources (the resources for which this resource is a base 
	 * resource), and the amount of occupied capacity they are incurring in me
	 * @return the map
	 */
	public Map<Resource, Double> getCapacityOccupiedByUpperResourcesMap() 
	{
		return Collections.unmodifiableMap(capacityUpperResourcesOccupyInMe);
	}

	/** Returns a set with the demands that have at least one route traversing this resource
	 * @return the traversing demands
	 */
	public Set<Demand> getTraversingDemands() 
	{
		Set<Demand> res = new HashSet<Demand> (); for (Route r : cache_traversingRoutesAndOccupiedCapacitiesIfNotFailingRoute.keySet()) res.add(r.demand);
		return res;
	}

	/** Returns a set with the routes that are traversing this resource
	 * @return the traversingRoutes
	 */
	public Set<Route> getTraversingRoutes() 
	{
		return cache_traversingRoutesAndOccupiedCapacitiesIfNotFailingRoute.keySet();
	}

	/** Returns the capacity that is occupied in this resource, because of a traversing route. If the route is not traversing 
	 * the resource, zero is returned. If the route is down, the occupied capacity is zero
	 * @param route the route
	 * @return the occupied capacity
	 */
	public double getTraversingRouteOccupiedCapacity(Route route) 
	{
		Double info = cache_traversingRoutesAndOccupiedCapacitiesIfNotFailingRoute.get(route);
		return (info == null) || (route.isDown())? 0.0 : info;
	}

	/** Returns a map with one key per each traversing route, and associated to it the amount of capacity occupied in this resource 
	 * because of it. Recall that if a route is down, its occupied capacity in the traversed resources drops to zero, and 
	 * come backs to its previous value when the route becomes up again
	 * @return the map
	 */
	public Map<Route,Double> getTraversingRouteOccupiedCapacityMap() 
	{
		Map<Route,Double> res = new HashMap<Route,Double> (cache_traversingRoutesAndOccupiedCapacitiesIfNotFailingRoute);
		for (Route r : res.keySet()) if (r.isDown()) res.put(r , 0.0);
		return res;
	}
	
	/** Resets the capacity of a resource, as well as the occupied capacity in the base resources. The set of base resources cannot have 
	 * new elements respect to the ones already established in the constructor. This limit is set to avoid loops in resources occupations.
	 *  Any base resource that is in the current map, that is not in newCapacityIOccupyInBaseResourcesMap, is assumed to maintain the same occupation.
	 * @param newCapacity the new capacity value to set in this resource
	 * @param newCapacityIOccupyInBaseResourcesMap a map with the update of the capacities to occupy in the base resources. If null, an empty map is assumed.
	 */
	public void setCapacity(double newCapacity , Map<Resource,Double> newCapacityIOccupyInBaseResourcesMap) 
	{
		checkAttachedToNetPlanObject();
		netPlan.checkIsModifiable();
		if (newCapacityIOccupyInBaseResourcesMap == null) newCapacityIOccupyInBaseResourcesMap = new HashMap<Resource,Double> ();
		for (Entry<Resource,Double> entry : newCapacityIOccupyInBaseResourcesMap.entrySet())
		{
			netPlan.checkInThisNetPlan(entry.getKey());
			if (!this.capacityIOccupyInBaseResource.keySet().contains(entry.getKey())) throw new Net2PlanException ("When resizing a resource capacity, we cannot increase its set of base resources");
			if (entry.getValue() < 0) throw new Net2PlanException ("The capacity occupied in a base resource cannot be negative");
		}
		if (newCapacity < 0) throw new Net2PlanException ("The capacity of a resource cannot be negative");
		this.capacity = newCapacity;
		this.capacityIOccupyInBaseResource.putAll (newCapacityIOccupyInBaseResourcesMap);
		for (Entry<Resource,Double> entry : newCapacityIOccupyInBaseResourcesMap.entrySet())
			entry.getKey().setUpperResourceOccupiedCapacity(this , entry.getValue());
		updateTotalOccupiedCapacity();
	}

	/** Sets (or updates) the amount of capacity occupied in this resource caused by an upper resource.
	 * If the occupied capacity is zero, the resource is still listed as an upper resource (not removed from the map)
	 * @param upperResource the resource for which this resource is a base resource
	 * @param occupiedCapacity value of occupied capacity
	 */
	void setUpperResourceOccupiedCapacity (Resource upperResource , double occupiedCapacity)
	{		
		checkAttachedToNetPlanObject();
		netPlan.checkIsModifiable();
		netPlan.checkInThisNetPlan(upperResource);
		if (upperResource.hostNode != this.hostNode) throw new Net2PlanException ("Upper resource must be in the same node as this resource");
		if (occupiedCapacity < 0) throw new Net2PlanException ("The occupied capacity cannot be negative");
		capacityUpperResourcesOccupyInMe.put(upperResource , occupiedCapacity);
		updateTotalOccupiedCapacity();
	}

	/** Removes the occupation of this resource (releasing any capacity allocated to it) in this resource.
	 * @param upperResource the resource for which this resource is a base resource
	 */
	void removeUpperResourceOccupation (Resource upperResource)
	{		
		setUpperResourceOccupiedCapacity (upperResource , 0);
		capacityUpperResourcesOccupyInMe.remove(upperResource);
	}

	void addTraversingRoute (Route r , double resourceOccupiedCapacityByThisRouteIfNotFailing)
	{
		if (!r.getSeqNodes().contains(this.hostNode)) throw new Net2PlanException ("The route does not traverse the host node of this resource");
		this.cache_traversingRoutesAndOccupiedCapacitiesIfNotFailingRoute.put(r , resourceOccupiedCapacityByThisRouteIfNotFailing);
		updateTotalOccupiedCapacity();
	}

	void removeTraversingRoute (Route r)
	{
		this.cache_traversingRoutesAndOccupiedCapacitiesIfNotFailingRoute.remove(r);
		updateTotalOccupiedCapacity();
	}

	
	/* Updates the value of total occupied capacity, summing the occupation by 1) upper resources, 2) traversing routes
	 */
	void updateTotalOccupiedCapacity ()
	{
		this.cache_totalOccupiedCapacity = 0;
		for (Entry<Resource,Double> entryUpperResource : capacityUpperResourcesOccupyInMe.entrySet())
			cache_totalOccupiedCapacity += entryUpperResource.getValue();
		for (Entry<Route,Double> entry : cache_traversingRoutesAndOccupiedCapacitiesIfNotFailingRoute.entrySet())
			if (!entry.getKey().isDown())
				cache_totalOccupiedCapacity += entry.getValue();
	}
	
	/**
	 * <p>Removes this Resource. Before that, it removes all the traversing routes (if any), and removes all the resources that are based on this (if any).</p>
	 */
	public void remove()
	{
		checkAttachedToNetPlanObject();
		netPlan.checkIsModifiable();
		for (Route r : new ArrayList<> (cache_traversingRoutesAndOccupiedCapacitiesIfNotFailingRoute.keySet())) r.remove();
		for (Resource upperResource : new ArrayList<> (capacityUpperResourcesOccupyInMe.keySet())) upperResource.remove();
		for (Resource baseResource : new ArrayList<> (capacityIOccupyInBaseResource.keySet())) baseResource.removeUpperResourceOccupation(this);
		netPlan.cache_id2ResourceMap.remove (id);
		Set<Resource> resourcesThisType = netPlan.cache_type2Resources.get(type);
		if (!resourcesThisType.contains(this)) throw new RuntimeException ("Bad");
		if (resourcesThisType.size() == 1) netPlan.cache_type2Resources.remove (type); else resourcesThisType.remove(this);
		hostNode.cache_nodeResources.remove(this);
        for (String tag : tags) netPlan.cache_taggedElements.get(tag).remove(this);
		NetPlan.removeNetworkElementAndShiftIndexes(netPlan.resources , index);
		if (ErrorHandling.isDebugEnabled()) netPlan.checkCachesConsistency();
		removeId ();
	}

	
	
	/**
	 * <p>Returns a {@code String} representation of the Shared Risk Group.</p>
	 * @return {@code String} representation of the SRG
	 */
	public String toString () { return "resource " + index + " (id " + id + ")"; }

	void checkCachesConsistency ()
	{
		super.checkCachesConsistency ();

		if (!netPlan.cache_type2Resources.get(this.type).contains(this)) throw new RuntimeException ("Bad");

		double accumOccupCap = 0;
		for (Entry<Resource,Double> upperEntry : capacityUpperResourcesOccupyInMe.entrySet())
		{
			final Resource upperResource = upperEntry.getKey();
			final double val = upperEntry.getValue();
			if (upperResource.capacityIOccupyInBaseResource.get(this) != val) throw new RuntimeException ("Bad");
			accumOccupCap += val;
		}
		for (Entry<Resource,Double> lowerEntry : capacityIOccupyInBaseResource.entrySet())
		{
			final Resource lowerResource = lowerEntry.getKey();
			final double val = lowerEntry.getValue();
			if (lowerResource.capacityUpperResourcesOccupyInMe.get(this) != val) throw new RuntimeException ("Bad");
		}
		for (Entry<Route,Double> travRoute : cache_traversingRoutesAndOccupiedCapacitiesIfNotFailingRoute.entrySet())
		{
			final Route r = travRoute.getKey();
			final double val = travRoute.getValue();
			if (r.cache_linkAndResourcesTraversedOccupiedCapIfnotFailMap.get(this) != val) throw new RuntimeException ("Bad");
			if (!r.isDown()) accumOccupCap += val;
		}

		if (accumOccupCap != cache_totalOccupiedCapacity)
		{
			if (cache_totalOccupiedCapacity != 0)
			{
				if ((accumOccupCap / cache_totalOccupiedCapacity) > 0.001) throw new RuntimeException();
			} else
			{
				if (accumOccupCap > 0.001 || accumOccupCap < -0.001) throw new RuntimeException();
			}
		}
	}


}
