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

package com.net2plan.interfaces.simulation;

import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.utils.StringUtils;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

/**
 * <p>Class representing a simulation event.</p>
 *
 * <p>Regardless specific event details, every event is defined by the event arrival time and a priority value. The highest priority event is called first.</p>
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @see <a href="http://en.wikipedia.org/wiki/Discrete_event_simulation">Wikipedia, "Discrete event simulation," <i>Wikipedia</i>, <i>The Free Encyclopedia</i></a>
 */
public class SimEvent implements Comparable<SimEvent>
{
	/**
	 * Module that will receive the associated event.
	 * 
	 */
	public enum DestinationModule
	{
		/**
		 * Event generator.
		 *
		 */
		EVENT_GENERATOR("event generator"),

		/**
		 * Event processor.
		 *
		 */
		EVENT_PROCESSOR("event processor");
	
		private final String label;
		private DestinationModule(String label) { this.label = label; }
		
		@Override
		public String toString() { return label; }
	};

	/**
	 * This class represents the request to add a new Demand.
	 */
	public static class DemandAdd
	{
		public Demand demandAddedToFillByProcessor; public final Node ingressNode; public final Node egressNode; public final NetworkLayer layer; public double offeredTraffic;

		/**
		 * Default constructor.
		 * @param ingressNode Ingress Node
		 * @param egressNode Egress Node
		 * @param layer Network Layer
		 * @param offeredTraffic Offered traffic
		 */
		public DemandAdd
			(Node ingressNode, Node egressNode, NetworkLayer layer, double offeredTraffic) { this.ingressNode = ingressNode; this.egressNode = egressNode; this.layer = layer; this.offeredTraffic = offeredTraffic;	}
		@Override
		public String toString()
		{
			return this.getClass ().getSimpleName() + " [ingressNode=" + ingressNode + ", egressNode=" + egressNode + ", layer=" + layer + ", offeredTraffic=" + offeredTraffic + "]";
		}
	};

	/**
	 * This class represents the request to remove an existing Demand.
	 */
	public static class DemandRemove
	{ 
		public final Demand demand;

		/**
		 * Default constructor.
		 * @param demand Demand to be removed
		 */
		public DemandRemove (Demand demand) { this.demand = demand; }

		@Override
		public String toString()
		{
			return this.getClass ().getSimpleName() + " [demand=" + demand + "]";
		} 
	};

	/**
	 * This class represents the request to modify an existing Demand.
	 */
	public static class DemandModify
	{
		public final Demand demand; public final double offeredTraffic; public final boolean modificationIsRelativeToCurrentOfferedTraffic;

		/**
		 * Default constructor.
		 * @param demand Demand to be modified
		 * @param offeredTraffic New offered traffic
		 * @param modificationIsRelativeToCurrentOfferedTraffic Wheter or not the modification is relative to the current offered traffic
		 */
		public DemandModify (Demand demand , double offeredTraffic , boolean modificationIsRelativeToCurrentOfferedTraffic) { this.demand = demand; this.offeredTraffic = offeredTraffic; this
			.modificationIsRelativeToCurrentOfferedTraffic = modificationIsRelativeToCurrentOfferedTraffic; }

		@Override
		public String toString()
		{
			return this.getClass ().getSimpleName() + " [demand=" + demand + ", offeredTraffic=" + offeredTraffic + ", modificationIsRelativeToCurrentOfferedTraffic=" + modificationIsRelativeToCurrentOfferedTraffic + "]";
		} 
		
	};

	/**
	 * This class represents the request to modify an existing Demand.
	 */
	public static class MulticastDemandModify
	{
		public final MulticastDemand demand; public final double offeredTraffic; public final boolean modificationIsRelativeToCurrentOfferedTraffic;

		/**
		 * Default constructor.
		 * @param demand Demand to be modified
		 * @param offeredTraffic New offered traffic
		 * @param modificationIsRelativeToCurrentOfferedTraffic Wheter or not the modification is relative to the current offered traffic
		 */
		public MulticastDemandModify (MulticastDemand demand , double offeredTraffic , boolean modificationIsRelativeToCurrentOfferedTraffic) { this.demand = demand; this.offeredTraffic = offeredTraffic; this
			.modificationIsRelativeToCurrentOfferedTraffic = modificationIsRelativeToCurrentOfferedTraffic; }

		@Override
		public String toString()
		{
			return this.getClass ().getSimpleName() + " [Multicast demand=" + demand + ", offeredTraffic=" + offeredTraffic + ", modificationIsRelativeToCurrentOfferedTraffic=" + modificationIsRelativeToCurrentOfferedTraffic + "]";
		} 
		
	};

	/**
	 * This class represents the request to add a new Route.
	 */
	public static class RouteAdd
	{ 
		public Route routeAddedToFillByProcessor; public final Demand demand; public final List<Link> seqLinks; public final double carriedTraffic; public final double occupiedLinkCapacity;

		/**
		 * Default constructor.
		 * @param demand Demand associated to the Route
		 * @param seqLinks Sequence of links
		 * @param carriedTraffic Carried traffic
		 * @param occupiedLinkCapacity Occupied link capacity
		 */
		public RouteAdd(Demand demand, List<Link> seqLinks, double carriedTraffic, double occupiedLinkCapacity) { this.demand = demand; this.seqLinks = seqLinks == null? new LinkedList<Link>() : new LinkedList<Link>(seqLinks); this
			.carriedTraffic = carriedTraffic; this.occupiedLinkCapacity = occupiedLinkCapacity; }

		@Override
		public String toString()
		{
			return this.getClass ().getSimpleName() + " [demand=" + demand + ", seqLinks=" + seqLinks + ", carriedTraffic=" + carriedTraffic + ", occupiedLinkCapacity=" + occupiedLinkCapacity + "]";
		}  
		
	};

	/**
	 * This class represents the request to remove and existing Route.
	 */
	public static class RouteRemove
	{ 
		public final Route route;

		/**
		 * Default constructor.
		 * @param route Route to be removed
		 */
		public RouteRemove (Route route) { this.route = route; }
		@Override
		public String toString()
		{
			return this.getClass ().getSimpleName() + " [route=" + route + "]";
		} 
	};

	/**
	 * This class represents the request to modify an existing Route.
	 */
	public static class RouteModify
	{ 
		public final Route route; public final List<Link> seqLinks; public final double carriedTraffic; public final double occupiedLinkCapacity;

		/**
		 * Default constructor.
		 * @param route Route to be modified
		 * @param seqLinks New sequence of links
		 * @param carriedTraffic New carried traffic
		 * @param occupiedLinkCapacity New occupied capacity
		 */
		public RouteModify(Route route , List<Link> seqLinks, double carriedTraffic, double occupiedLinkCapacity) { this.route = route; this.seqLinks = new LinkedList<Link> (seqLinks); this
			.carriedTraffic = carriedTraffic; this.occupiedLinkCapacity = occupiedLinkCapacity; }
		@Override
		public String toString()
		{
			return this.getClass ().getSimpleName() + " [route=" + route + ", seqLinks=" + seqLinks + ", carriedTraffic=" + carriedTraffic + ", occupiedLinkCapacity=" + occupiedLinkCapacity + "]";
		}  
	};

	/**
	 * This class represents the request to modify the failure state of multiple links and nodes.
	 */
	public static class NodesAndLinksChangeFailureState
	{
		public final Collection<Node> nodesToUp , nodesToDown; public final Collection<Link> linksToUp , linksToDown;

		/**
		 * Default constructor.
		 * @param nodesToUp Nodes to up state
		 * @param nodesToDown Nodes to down state
		 * @param linksToUp Links to up state
		 * @param linksToDown Links to down state
		 */
		public NodesAndLinksChangeFailureState (Collection<Node> nodesToUp , Collection<Node> nodesToDown , Collection<Link> linksToUp , Collection<Link> linksToDown) { this.nodesToUp = nodesToUp
			== null? null : new HashSet<Node>(nodesToUp); this.nodesToDown = nodesToDown == null? null : new HashSet<Node> (nodesToDown); this.linksToUp = linksToUp == null? null : new HashSet<Link> (linksToUp); this.linksToDown = linksToDown == null? null : new HashSet<Link> (linksToDown); }
		@Override
		public String toString()
		{
			return this.getClass ().getSimpleName() + " [nodesToUp=" + nodesToUp + ", nodesToDown=" + nodesToDown + ", linksToUp=" + linksToUp + ", linksToDown=" + linksToDown + "]";
		} 
	};

	/**
	 * This class represents the request to add a new Link.
	 */
	public static class LinkAdd
	{ 
		public Link linkAddedToFillByProcessor; public final Node originNode; public final Node destinationNode; public final NetworkLayer layer; public double capacity; public double lengthInKm; public double propagationSpeedInKmPerSecond;

		/**
		 * Default constructor.
		 * @param originNode Origin Node
		 * @param destinationNode Destination Node
		 * @param layer Network Layer
		 * @param capacity Link capacity (in traffic units)
		 * @param lengthInKm Link length in km
		 * @param propagationSpeedInKmPerSecond Propagation speed in km/s
		 */
		public LinkAdd(Node originNode, Node destinationNode, NetworkLayer layer, double capacity, double lengthInKm, double propagationSpeedInKmPerSecond) {	this.originNode = originNode; this
			.destinationNode = destinationNode; this.layer = layer; this.capacity = capacity; this.lengthInKm = lengthInKm; this.propagationSpeedInKmPerSecond = propagationSpeedInKmPerSecond; }
		@Override
		public String toString()
		{
			return this.getClass ().getSimpleName() + " [originNode=" + originNode + ", destinationNode=" + destinationNode + ", layer=" + layer + ", capacity=" + capacity + ", lengthInKm=" + lengthInKm + ", propagationSpeedInKmPerSecond=" + propagationSpeedInKmPerSecond + "]";
		}  
	};

	/**
	 * This class represents the request to remove an existing Link.
	 */
	public static class LinkRemove
	{ 
		public final Link link;

		/**
		 * Default constructor
		 * @param link Link to be removed.
		 */
		public LinkRemove (Link link) { this.link = link; }
		@Override
		public String toString()
		{
			return this.getClass ().getSimpleName() + " [link=" + link + "]";
		} 
	};

	/**
	 * This class represents the request to modify an existing Link
	 */
	public static class LinkModify
	{ 
		public final Link link; public final double newCapacity;

		/**
		 * Default constructor
		 * @param link Link to be modified.
		 * @param newCapacity New link capacity
		 */
		public LinkModify (Link link , double newCapacity) { this.link = link; this.newCapacity = newCapacity; }
		@Override
		public String toString()
		{
			return this.getClass ().getSimpleName() + " [link=" + link + ", newCapacity=" + newCapacity + "]";
		} 
	};

	
	/**
	 * Default priority for events (higher values give higher priority).
	 */
	public final static int DEFAULT_PRIORITY = 0;
	
//	/**
//	 * Default type for events.
//	 * 
//	 * @since 0.3.0
//	 */
//	public final static int DEFAULT_TYPE = 0;
//	
	private final double eventTime;
	private final int type;
	private final int priority;
	private final Object object;
	private final DestinationModule destinationModule;

	//	private final List<SimAction> actions;
	//	private final EnumSet<SimAction.ActionType> actionTypes;

	/**
	 * Constructor that allows to generate an event with a custom object.
	 * @param eventTime Event time
	 * @param destinationModule Module that will receive the event
	 * @param type Event type
	 * @param priority Event priority
	 * @param object Custom object
	 */
	public SimEvent(double eventTime, DestinationModule destinationModule , int type , int priority , Object object)
	{
		this.eventTime = eventTime;
		this.type = type;
		this.priority = priority;
		this.object = object;
		this.destinationModule = destinationModule;
	}
	
	/**
	 * Constructor that allows to generate an event with a custom object.
	 *
	 * @param eventTime Event time
	 * @param type Event type
	 * @param object Custom object
	 * @param destinationModule Module that will receive the event
	 */
	public SimEvent(double eventTime, DestinationModule destinationModule , int type , Object object)
	{
		this.eventTime = eventTime;
		this.type = type;
		this.priority = SimEvent.DEFAULT_PRIORITY;
		this.object = object;
		this.destinationModule = destinationModule;
	}
	

	@Override
	public final int compareTo(SimEvent e)
	{
		/* Compare event time */
		if (getEventTime() < e.getEventTime())
		{
			return -1;
		}
		else
		{
			if (getEventTime() > e.getEventTime())
			{
				return 1;
			}
			else
			{
				/* If equal, sort by priority */
				if (getEventPriority() < e.getEventPriority()) return 1;
				else return getEventPriority() > e.getEventPriority() ? -1 : 0;
			}
		}
	}

	@Override
	public String toString()
	{
		String NEW_LINE = StringUtils.getLineSeparator();
		StringBuilder out = new StringBuilder();
		out.append(String.format("New event for %s at %s (priority = %d, type = %d)", destinationModule.label, StringUtils.secondsToYearsDaysHoursMinutesSeconds(eventTime), priority, type));
		out.append(NEW_LINE).append(NEW_LINE);
		
//		if (actions.isEmpty())
//		{
//			out.append("No action specified");
//			out.append(NEW_LINE);
//		}
//		else
//		{
//			out.append("List of notified actions:");
//			out.append(NEW_LINE);
//			
//			for(SimAction action : actions)
//			{
//				out.append(action.toString());
//				out.append(NEW_LINE);
//			}
//		}
//		
		if (object == null)
		{
			out.append("No custom object specified");
			out.append(NEW_LINE);
		}
		else
		{
			out.append("Custom object: ").append(object.toString());
			out.append(NEW_LINE);
		}
		
		return out.toString();
	}
	
	/**
	 * <p>Returns the destination module of the event.</p>
	 *
	 * @return List of action notifications
	 */
	public final DestinationModule getEventDestinationModule()
	{
		return destinationModule;
	}
	
	/**
	 * Returns the custom object of the event (may be null).
	 * 
	 * @return Custom object
	 */
	public final Object getEventObject()
	{
		return object;
	}
	
	/**
	 * Returns the event priority.
	 * 
	 * @return Event priority
	 */
	public final int getEventPriority()
	{
		return priority;
	}
	
	/**
	 * Returns the event time.
	 * 
	 * @return Event time
	 */
	public final double getEventTime()
	{
		return eventTime;
	}
	
	/**
	 * Returns the custom event type.
	 * 
	 * @return Event type
	 */
	public final int getEventType()
	{
		return type;
	}
	
}
