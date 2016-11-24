//1) Repasar esto
//2) Que todos los cambios impliquen update: anadir cambios de factores, completar todos
//3) las rutas no se pueden anadir aqui: aqui solo tenemos una cache, es en las rutas donde se fija esto.
//Ojo que al cambiar la secuencia de enlaces, hay que cambiar tambien los recursos ocupados. SI una demanda es de una
//service chain, todas sus rutas deben atravesar recursos en cualquier estado, y no pueden tener protection segments

// demand: setServiceChainSequence: le pasas List<String> con types. Solo se puede ejecutar cuando no tiene rutas.
// rutas: al hacer addroute, se chequea si hay service chain. En ese caso, hay que pasarle en constructor info de
//recursos ocupados (lista, y cuanto?? o solo lista???). La ruta comprueba que van en orden con demanda, y con lista enlaces
//// OJO: SI OCUPACION ESTA EN RUTA (AL HACER SETCARRIED Y AL CREAR) ENTONCES NO A LUGAR EL FIXED Y PROPORTIONAL!!!

/*******************************************************************************


 * 
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.net2plan.internal.AttributeMap;
import com.net2plan.utils.Pair;
import com.net2plan.utils.Triple;

/** <p>.</p> 
 * @author Pablo Pavon-Marino
 */
public class Resource extends NetworkElement
{
	/* this information never changes after creation */
	Node hostNode; // never changes after created, but with copyFrom
	String capacityMeasurementUnits; // never changes after created, but with copyFrom
	String type; // never changes after created, but with copyFrom
	double fixedOccupiedResourceCapacityPerTraversingRoute; // never changes after created, but with copyFrom
	double occupiedResourceCapacityPerTraversingTrafficUnits; // never changes after created, but with copyFrom
	Map<Resource , Pair<Double,Double>> baseResourcesConsumptionPolicy; // resource, fixed factor amount, proportional-to-this-resource-occupiedCapacity amount

	/* this information can change after creation */
	String name; // descriptive name of the resource. Can change.
	Map<Resource,Double> upperResourcesConsumptionInformation;
	double capacity;
	double totalOccupiedCapacity;
	List<Route> traversingRoutes;
	
	Resource (NetPlan netPlan , long id , int index , String type , String name , Node hostNode , 
			double capacity , String capacityMeasurementUnits,
			double fixedOccupiedResourceCapacityPerTraversingRoute,
			double occupiedResourceCapacityPerTraversingTrafficUnits,
			Map<Resource,Pair<Double,Double>> baseResourcesConsumptionPolicy , AttributeMap attributes)
	{
		super (netPlan , id , index , attributes);

		if (!netPlan.equals(hostNode.netPlan)) throw new Net2PlanException ("The Resource host node is in a different NetPlan object (or removed)"); 
		if (capacity < 0) throw new Net2PlanException ("The capacity of a resource cannot be negative"); 
		if (fixedOccupiedResourceCapacityPerTraversingRoute < 0) throw new Net2PlanException ("The occupation fixed factor of the resource cannot be negative");
		if (occupiedResourceCapacityPerTraversingTrafficUnits < 0) throw new Net2PlanException ("The occupation proportional factor of the resource cannot be negative");
		if (baseResourcesConsumptionPolicy == null) baseResourcesConsumptionPolicy = new HashMap<Resource,Pair<Double,Double>> (); 
		for (Entry<Resource,Pair<Double,Double>> resPolicyInfo : baseResourcesConsumptionPolicy.entrySet())
		{
			final Resource r = resPolicyInfo.getKey();
			if (!netPlan.equals(r.netPlan)) throw new Net2PlanException ("The consumed resources of a resource must be in the same NetPlan object");
			if (r.hostNode != hostNode) throw new Net2PlanException ("All the resources consumed by a resource must be in the same node resource");
			if (resPolicyInfo.getValue().getFirst() < 0) throw new Net2PlanException ("The fixed par of the consumed resources cannot be negative");
			if (resPolicyInfo.getValue().getSecond()  < 0) throw new Net2PlanException ("The proportional par of the consumed resources cannot be negative");
		}
		this.type = type;
		this.name = name;
		this.hostNode = hostNode;
		this.capacityMeasurementUnits = capacityMeasurementUnits;
		this.fixedOccupiedResourceCapacityPerTraversingRoute = fixedOccupiedResourceCapacityPerTraversingRoute;
		this.occupiedResourceCapacityPerTraversingTrafficUnits = occupiedResourceCapacityPerTraversingTrafficUnits;
		this.capacity = capacity;
		this.totalOccupiedCapacity = 0;
		this.upperResourcesConsumptionInformation = new HashMap<Resource,Double> ();
		this.baseResourcesConsumptionPolicy = new HashMap<Resource,Pair<Double,Double>> (baseResourcesConsumptionPolicy);
		this.traversingRoutes = new ArrayList<Route> ();
	}

	void copyFrom (Resource origin)
	{
		if ((this.id != origin.id) || (this.index != origin.index)) throw new RuntimeException ("Bad");
		if ((this.netPlan == null) || (origin.netPlan == null) || (this.netPlan == origin.netPlan)) throw new RuntimeException ("Bad");
		this.type = origin.type;
		this.name = origin.name;
		this.hostNode = this.netPlan.getNodeFromId (origin.hostNode.id);
		this.capacityMeasurementUnits = origin.capacityMeasurementUnits;
		this.fixedOccupiedResourceCapacityPerTraversingRoute = origin.fixedOccupiedResourceCapacityPerTraversingRoute;
		this.occupiedResourceCapacityPerTraversingTrafficUnits = origin.occupiedResourceCapacityPerTraversingTrafficUnits;
		this.capacity = origin.capacity;
		this.totalOccupiedCapacity = origin.totalOccupiedCapacity;
		this.upperResourcesConsumptionInformation = new HashMap<Resource,Double> ();
		for (Entry<Resource,Double> entry : origin.upperResourcesConsumptionInformation.entrySet())
		{
			final Resource resourceThisNp = this.netPlan.getResourceFromId(entry.getKey().id);
			if (resourceThisNp == null) throw new RuntimeException ("Bad");
			this.upperResourcesConsumptionInformation.put(resourceThisNp , entry.getValue());
		}
		this.baseResourcesConsumptionPolicy = new HashMap<Resource,Pair<Double,Double>> ();
		for (Entry<Resource,Pair<Double,Double>> entry : origin.baseResourcesConsumptionPolicy.entrySet())
		{
			final Resource resourceThisNp = this.netPlan.getResourceFromId(entry.getKey().id);
			if (resourceThisNp == null) throw new RuntimeException ("Bad");
			this.baseResourcesConsumptionPolicy.put(resourceThisNp , entry.getValue());
		}
		this.traversingRoutes = new ArrayList<Route> ();
		for (Route originRoute : origin.traversingRoutes)
		{
			final Route routeThisNp = this.netPlan.getRouteFromId(originRoute.id);
			if (routeThisNp == null) throw new RuntimeException ("Bad");
			this.traversingRoutes.add(this.netPlan.getRouteFromId(originRoute.id));
		}
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
	
	

	/** Returns the fixed amount of resource capacity occupied per each traversing route to this resource.
	 * The total capacity occupied in this resource is the fixed amount, plus the proportional factor multiplied by 
	 * the carried traffic of this resource traversing routes. 
	 * @return the fixed factor
	 */
	public double getFixedOccupiedResourceCapacityPerTraversingRoute() {
		return fixedOccupiedResourceCapacityPerTraversingRoute;
	}

	/** Returns the proportional factor of resource capacity occupied per each traversing route traffic units.
	 * The total capacity occupied in this resource is the fixed amount, plus the proportional factor multiplied by 
	 * the carried traffic of this resource traversing routes. 
	 * @return the proportional factor
	 */
	public double getOccupiedResourceCapacityPerTraversingTrafficUnits() {
		return occupiedResourceCapacityPerTraversingTrafficUnits;
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
		return totalOccupiedCapacity;
	}

	/** Sets the name of the resource
	 */
	public void setName(String name) 
	{
		this.name = name;
	}

	/**
	 * @param capacity the capacity to set
	 */
	public void setCapacity(double capacity) 
	{
		if (capacity < 0) throw new Net2PlanException ("The capacity of a resource cannot be negative"); 
		this.capacity = capacity;
	}

	
	/** Returns the set of base resources of this resource
	 * @return the set of base resources (an unmodificable set)
	 */
	public Set<Resource> getBaseResources ()
	{
		return Collections.unmodifiableSet(baseResourcesConsumptionPolicy.keySet());
	}

	/** Returns the set of resources that are above of this resource, so this resource is a base resource for them
	 * @return the set of upper resources (an unmodificable set)
	 */
	public Set<Resource> getUpperResources ()
	{
		return Collections.unmodifiableSet(upperResourcesConsumptionInformation.keySet());
	}

	/** Returns the fixed factor in the amount of capacity that this resource occupies in the base resource 
	 * provided. The total capacity occupied in the base resource is the fixed factor, plus the proportional factor multipled by 
	 * the occupied capacity in this resource. If baseResource is not a base resource of this resource, a zero is returned
	 * @param baseResource
	 * @return the fixed factor
	 */
	public double getBaseResourceFixedOccupationFactor (Resource baseResource)
	{
		Pair<Double,Double> info = baseResourcesConsumptionPolicy.get(baseResource);
		if (info == null) return 0; else return info.getFirst();
	}

	/** Returns the proportional factor in the amount of capacity that this resource occupies in the base resource 
	 * provided. The total capacity occupied in the base resource is the fixed factor, plus the proportional factor multipled by 
	 * the occupied capacity in this resource. If baseResource is not a base resource of this resource, a zero is returned
	 * @param baseResource
	 * @return the proportional factor
	 */
	public double getBaseResourceProportionalOccupationFactor (Resource baseResource)
	{
		Pair<Double,Double> info = baseResourcesConsumptionPolicy.get(baseResource);
		if (info == null) return 0; else return info.getSecond();
	}

	/** Returns the total amount of capacity occupied in the base resource, because of the existence of this resource. 
	 * The total capacity occupied in the base resource is the fixed factor, plus the proportional factor multiplied by 
	 * the occupied capacity in this resource. If baseResource is not a base resource of this resource, a zero is returned
	 * @param baseResource
	 * @return the total occupied capacity in such base resource
	 */
	public double getBaseResourceOccupiedCapacity (Resource baseResource)
	{
		Pair<Double,Double> info = baseResourcesConsumptionPolicy.get(baseResource);
		if (info == null) return 0; else return info.getFirst() + this.totalOccupiedCapacity * info.getSecond();
	}

	/** Returns the total amount of capacity occupied in this resource, caused by the given upper resource
	 * If upperResource is not an upper resource of this resource, a zero is returned
	 * @param upperResource the resource for which this resource is a base resource
	 * @return the occupied capacity in this resource, caused by the given upper reource 
	 */
	public double getUpperResourceOccupiedCapacity (Resource upperResource)
	{
		Double info = upperResourcesConsumptionInformation.get(upperResource);
		if (info == null) return 0; else return info;
	}

	/** Sets (or updates) the amount of capacity occupied in this resource caused by an upper resource.
	 * If the occupied capacity is zero, the resource is still listed as an upper resource
	 * @param upperResource the resource for which this resource is a base resource
	 * @param occupiedCapacity value of occupied capacity
	 */
	public void setUpperResourceOccupiedCapacity (Resource upperResource , double occupiedCapacity)
	{		
		checkAttachedToNetPlanObject();
		netPlan.checkIsModifiable();
		netPlan.checkInThisNetPlan(upperResource);
		if (occupiedCapacity < 0) throw new Net2PlanException ("The occupied capacity cannot be negative");
		upperResourcesConsumptionInformation.put(upperResource , occupiedCapacity);
		updateOccupationAndTrafficInfoHereAndInBaseResources();
	}
	
	
	/** Returns the map with the policy of how the occupation in this resource is propagated to the occupation 
	 * in its base resources. This is given in a map. The key is the base resource. The two values associated are 
	 * (i) the fixed factor of capacity in the base resource occupied, (ii) the proportional factor.
	 * The total occupied capacity in the base resource by this resource, is given by the fixed factor, 
	 * plus the current occupied capacity in this resource multiplied by the proportional factor.
	 * @return the baseResourcesConsumptionPolicy
	 */
	public Map<Resource, Pair<Double, Double>> getBaseResourcesConsumptionPolicy() 
	{
		return Collections.unmodifiableMap(baseResourcesConsumptionPolicy);
	}

	/** Returns the map with the information on the upper resources (the resources for which this resource is a base 
	 * resource), and the amount of occupied capacity they are incurring in me
	 * @return the map
	 */
	public Map<Resource, Double> getUpperResourcesOccupationInformation() 
	{
		return Collections.unmodifiableMap(upperResourcesConsumptionInformation);
	}

	/**
	 * @return the traversingRoutes
	 */
	public List<Route> getTraversingRoutes() 
	{
		return Collections.unmodifiableList(traversingRoutes);
	}


	public void addTraversingRoute (Route r)
	{
		r.checkAttachedToNetPlanObject();
		checkAttachedToNetPlanObject(r.netPlan);
		if (!r.getSeqNodesRealPath().contains(this.hostNode)) throw new Net2PlanException ("The route does not traverse the host node of this resource");
		this.traversingRoutes.add(r);
		updateOccupationAndTrafficInfoHereAndInBaseResources();
	}


	
	/* Reflects here any update in upper resources occupation, fixed and proportional occupation factors of this or any base resources, 
	 * and of traversing routes. Updates occupation of this resource and of all base resources */
	private void updateOccupationAndTrafficInfoHereAndInBaseResources ()
	{
		double currentTotalOccupiedCapacity = 0;
		for (Entry<Resource,Double> entryUpperResource : upperResourcesConsumptionInformation.entrySet())
			currentTotalOccupiedCapacity += entryUpperResource.getValue();
		for (Route travRoute : traversingRoutes)
			currentTotalOccupiedCapacity += fixedOccupiedResourceCapacityPerTraversingRoute + occupiedResourceCapacityPerTraversingTrafficUnits * travRoute.getCarriedTraffic();
		for (Entry<Resource , Pair<Double,Double>> baseInfo : baseResourcesConsumptionPolicy.entrySet())
		{
			final Resource baseResource = baseInfo.getKey();
			final double fixedFactor = baseInfo.getValue().getFirst();
			final double propFactor = baseInfo.getValue().getSecond();
			final double newOccupiedCapacityInBaseResource = fixedFactor + currentTotalOccupiedCapacity * propFactor;
			baseResource.setUpperResourceOccupiedCapacity(this , newOccupiedCapacityInBaseResource);
		}
	}
	
	
	/**
	 * <p>Returns a {@code String} representation of the Shared Risk Group.</p>
	 * @return {@code String} representation of the SRG
	 */
	public String toString () { return "resource " + index + " (id " + id + ")"; }

	void checkCachesConsistency ()
	{
//		for (Link link : links) if (!link.cache_srgs.contains(this)) throw new RuntimeException ("Bad");
//		for (Node node : nodes) if (!node.cache_nodeSRGs.contains(this)) throw new RuntimeException ("Bad");
	}


}
