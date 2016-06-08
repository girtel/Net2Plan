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

package com.net2plan.interfaces.simulation;

import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.utils.StringUtils;

import java.util.*;

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
	 * This class represents the request to add a new Protection Segment.
	 */
	public static class SegmentAdd
	{ 
		public ProtectionSegment segmentAddedToFillByProcessor; public final Set<Route> associatedRoutes; public final List<Link> seqLinks; public final double reservedLinkCapacity;

		/**
		 * Default constructor.
		 * @param associatedRoutes Associated routes
		 * @param seqLinks Sequence of links
		 * @param reservedLinkCapacity Reserved link capacity
		 */
		public SegmentAdd(Set<Route> associatedRoutes , List<Link> seqLinks, double reservedLinkCapacity) { this.associatedRoutes = new HashSet<Route> (associatedRoutes); this.seqLinks = new
			LinkedList<Link> (seqLinks); this.reservedLinkCapacity = reservedLinkCapacity; }
		@Override
		public String toString()
		{
			return this.getClass ().getSimpleName() + " [associatedRoutes=" + associatedRoutes + ", seqLinks=" + seqLinks + ", reservedLinkCapacity=" + reservedLinkCapacity + "]";
		}  
	};

	/**
	 * This class represents the request to remove an existing Protection Segment.
	 */
	public static class SegmentRemove
	{ 
		public final ProtectionSegment segment;

		/**
		 * Default constructor.
		 * @param segment Protection Segment to be removed
		 */
		public SegmentRemove (ProtectionSegment segment) { this.segment = segment; }
		@Override
		public String toString()
		{
			return this.getClass ().getSimpleName() + " [segment=" + segment + "]";
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

	
	
	
//	/** This class contains constants for the types of events used in built-in algorithms. 
//	 * When developing other algorithms, the user is free to follow these constants in the type field or not. */
//	public static final class EventType
//	{
//		/** The event generator requests to establish a set of nodes as up (all at the same time). Parameters are the nodes (Set<Node>) */
//		public final static int NODES_UP = 100; 
//		/** The event generator requests to establish a set of nodes as down (all at the same time). Parameters are the nodes (Set<Node>) */
//		public final static int NODES_DOWN = 101; 
//		/** The event generator requests to establish a set of links as up (all at the same time). Parameters are the links (Set<Link>) */
//		public final static int LINKS_UP = 200; 
//		/** The event generator requests to establish a set of links as down (all at the same time). Parameters are the links (Set<Link>) */
//		public final static int LINKS_DOWN = 201; 
//		/** The event generator requests to add a set of demands (all at the same time). Parameters are a Map<Long,Quadruple<Node,Node,NetworkLayer,Double>>, where the map key is the demand internal identifier assigned by the generator module (THIS IS NOT THE DEMAND IDENTIFIER IN THE NETPLAN OBJECT, WHICH IS NOT KNOWN UNTIL THE DEMAND OBJECT IS CREATED BY THE EVENT PROCESSOR). The Quadruple is composed of composed of the demand input node (Node), demand output node (Node), the network layer (NetworkLayer) and demand offered traffic (Double). Created demands by the event processor should have an attribute of key "EventGeneratorId" and value the internal identifier set  */
//		public final static int DEMANDS_ADD = 300; 
//		/** The event generator requests to remove a set of demands (all at the same time). Parameters are a Set<Long>, the demand internal identifiers passed when the event for creating a demand was produced (Long).  */
//		public final static int DEMANDS_REMOVE = 301;
//		/** The event generator requests to modify the offered traffic of a set of demands (all at the same time). Parameters are a Map<Long,Double>, where each entry has as key the internal identifier of the demand assigned by the generator, and the value, the new offered traffic (Double) for that demand */
//		public final static int DEMANDS_MODIFY = 302;
//		/** The event generator requests to add a set of multicast demands (all at the same time). Parameters are a Map<Long,Quadruple<Node,Node,NetworkLayer,Double>>, where the map key is the demand internal identifier assigned by the generator module (THIS IS NOT THE DEMAND IDENTIFIER IN THE NETPLAN OBJECT, WHICH IS NOT KNOWN UNTIL THE DEMAND OBJECT IS CREATED BY THE EVENT PROCESSOR). The Quadruple is composed of composed of the demand input node (Node), demand output node (Node), the network layer (NetworkLayer) and demand offered traffic (Double). Created demands by the event processor should have an attribute of key "EventGeneratorId" and value the internal identifier set  */
//		public final static int MULTICASTDEMANDS_ADD = 400; 
//		/** The event generator requests to remove a set of multicast demands (all at the same time). Parameters are a Set<Long>, the demand internal identifiers passed when the event for creating a demand was produced (Long).  */
//		public final static int MULTICASTDEMANDS_REMOVE = 401;
//		/** The event generator requests to modify the offered traffic of a set of multicast demands (all at the same time). Parameters are a Map<Long,Double>, where each entry has as key the internal identifier of the demand assigned by the generator, and the value, the new offered traffic (Double) for that demand */
//		public final static int MULTICASTDEMANDS_MODIFY = 402;
//		/** The event generator requests to add a set of routes (all at the same time). Parameters are a Map<Long,Quadruple<Demand,List<Link>,Double,Double>>, where the map key is the route internal identifier assigned by the generator module (THIS IS NOT THE ROUTE IDENTIFIER IN THE NETPLAN OBJECT, WHICH IS NOT KNOWN UNTIL THE DEMAND OBJECT IS CREATED BY THE EVENT PROCESSOR). The Quadruple is composed of composed of the route demand (Demand), the sequence of links (List<Link>), the carried traffic (Double) and the route occupied capacity in the links. Created routes by the event processor should have an attribute of key "EventGeneratorId" and value the internal identifier set  */
//		public final static int ROUTES_ADD = 500; 
//		/** The event generator requests to remove a set of routes (all at the same time). Parameters are a Set<Long>, the route internal identifiers passed when the event for creating a route was produced (Long).  */
//		public final static int ROUTES_REMOVE = 501;
//		/** The event generator requests to modify the carried traffic, occupied capacity of a set of routes (all at the same time). Parameters are a Map<Long,Triple<List<Link>,Double,Double>>, where each entry has as key the internal identifier of the route assigned by the generator, and the values of new sequence of links, carried traffic and and occupied capacity in the traversed links */
//		public final static int ROUTES_MODIFY = 502;
//	}
//	
	
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
	

//	/**
//	 * Constructor that allows to generate an event with an action notification.
//	 *
//	 * @param eventTime Event time
//	 * @param action Action to be notified
//	 * @param destinationModule Module that will receive the event
//	 * @since 0.3.0
//	 */
//	public SimEvent(double eventTime, final SimAction action, DestinationModule destinationModule)
//	{
//		this(eventTime, new LinkedList<SimAction>() {{ add(action); }}, null, destinationModule);
//	}
//	
//	/**
//	 * Constructor that allows to generate an event with a list of action notifications.
//	 *
//	 * @param eventTime Event time
//	 * @param actions List of actions to be notified
//	 * @param destinationModule Module that will receive the event
//	 * @since 0.3.0
//	 */
//	public SimEvent(double eventTime, List<SimAction> actions, DestinationModule destinationModule)
//	{
//		this(eventTime, actions, null, destinationModule);
//	}
//	
//	/**
//	 * Constructor that allows to generate an event with a custom object and a list of action notifications.
//	 * 
//	 * @param eventTime Event time
//	 * @param actions List of actions to be notified
//	 * @param object Custom object
//	 * @param destinationModule Module that will receive the event
//	 * @since 0.3.0
//	 */
//	public SimEvent(double eventTime, List<SimAction> actions, Object object, DestinationModule destinationModule)
//	{
//		this.eventTime = eventTime;
////		this.actions = new LinkedList<SimAction>();
////		this.actionTypes = EnumSet.noneOf(SimAction.ActionType.class);
//		this.object = object;
//		this.destinationModule = destinationModule;
//		
//		if (actions != null)
//		{
//			for(SimAction action : actions)
//			{
//				this.actions.add(action);
//				this.actionTypes.add(action.getActionType());
//			}
//		}
//		
//		setEventPriority(DEFAULT_PRIORITY);
//		setEventType(DEFAULT_TYPE);
//	}

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
	
//	/**
//	 * Returns {@code true} if the event contains an action notification of the
//	 * given type.
//	 *
//	 * @param actionType Type of action notification
//	 * @return {@code true} if the event contains an action notification of the given type. Otherwise, {@code false}
//	 * @since 0.3.0
//	 */
//	public boolean contains(SimAction.ActionType actionType)
//	{
//		return containsAny(EnumSet.of(actionType));
//	}
//	
//	/**
//	 * Returns {@code true} if the event contains an action notification of 
//	 * among one of the given types.
//	 *
//	 * @param actionTypes Types of action notification
//	 * @return {@code true} if the event contains an action notification of any of the given type. Otherwise, {@code false}
//	 * @since 0.3.0
//	 */
//	public boolean containsAny(EnumSet<SimAction.ActionType> actionTypes)
//	{
//		for(SimAction.ActionType actionType : actionTypes)
//			if (this.actionTypes.contains(actionType))
//				return true;
//		
//		return false;
//	}
//	
//	/**
//	 * <p>Returns the list of action notifications.</p>
//	 *
//	 * <p><b>Important</b>: The returned list is unmodifiable</p>
//	 *
//	 * @return List of action notifications
//	 * @since 0.3.0
//	 */
//	public final List<SimAction> getEventActionList()
//	{
//		return Collections.unmodifiableList(actions);
//	}
//	
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
	
//	/**
//	 * Sets scheduling priority of the message. The priority value is used when 
//	 * the simulator inserts messages into the future event list (FEL), to order 
//	 * messages with identical arrival time values.
//	 * 
//	 * @param priority Event priority (the higher, the more priority)
//	 * @return Reference to the event
//	 * @since 0.3.0
//	 */
//	public final SimEvent setEventPriority(int priority)
//	{
//		this.priority = priority;
//		return this;
//	}
//
//	/**
//	 * Sets the message kind. This field can be freely used by the user. No 
//	 * processing is made by the simulation kernel.
//	 * 
//	 * @param type Event type
//	 * @return Reference to the event
//	 * @since 0.3.0
//	 */
//	public final SimEvent setEventType(int type)
//	{
//		this.type = type;
//		return this;
//	}
}
