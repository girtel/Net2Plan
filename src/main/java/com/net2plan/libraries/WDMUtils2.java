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

// PABLO: HACER QUE RSA SOLO TENGA COPIAS DE DATOS, NO ORIGINALES POR SI LA COSA CAMBIA
// HACER UN CHECK DE RSA. EN EL CONSTRUCTOR SE HACE UN CHECK SUAVE. EL CHECK OTRO ES UN CHECK HARD (REGENERADORES SI O NO)

// ojo al release resources: si hace falta tambien libear de la ruta original 

package com.net2plan.libraries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import com.net2plan.interfaces.networkDesign.Demand;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.interfaces.networkDesign.ProtectionSegment;
import com.net2plan.interfaces.networkDesign.Route;
import com.net2plan.utils.Constants;
import com.net2plan.utils.IntUtils;
import com.net2plan.utils.Pair;
import com.net2plan.utils.StringUtils;

import cern.colt.matrix.tdouble.DoubleFactory1D;
import cern.colt.matrix.tdouble.DoubleFactory2D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import cern.colt.matrix.tint.IntFactory1D;
import cern.colt.matrix.tint.IntFactory2D;
import cern.colt.matrix.tint.IntMatrix1D;
import cern.colt.matrix.tint.IntMatrix2D;

/**
 * Class to deal with optical topologies including wavelength assignment and regenerator placement.
 *
 */
public class WDMUtils2
{
	private final static String ATTRIBUTE_ROWSEPARATOR = ";";
	private final static String ATTRIBUTE_COLUMNSEPARATOR = " ";
	
	/**
	 * This class represents a Routing and Spectrum Assignment, valid for a lightpath. This comprises a sequence of links, and for each link the set of frequency slots occupied.
	 * This number of slots must be the same in all the links.
	 */
	public static class RSA
	{
		public final Node ingressNode, egressNode;
		public final List<Link> seqLinks;
		public final IntMatrix2D seqFrequencySlots;
		public final int [] seqRegeneratorsOccupancy; // as many coordinates as links traversed, indicates with 1 if a regenerator is needed at origin node of the given link

		public RSA (Route r , boolean initializeWithTheInitialRoute)
		{
			this.ingressNode = r.getIngressNode();
			this.egressNode = r.getEgressNode();
			this.seqLinks = new ArrayList<Link> (r.getSeqLinksRealPath());
			try 
			{ 
				this.seqFrequencySlots = StringUtils.readIntMatrix(r.getAttribute(initializeWithTheInitialRoute? WDMUtils2.SEQUENCE_OF_FREQUENCYSLOTS_INITIAL_ROUTE_ATTRIBUTE_NAME : WDMUtils2.SEQUENCE_OF_FREQUENCYSLOTS_ATTRIBUTE_NAME));
				this.seqRegeneratorsOccupancy = r.getAttribute(initializeWithTheInitialRoute? WDMUtils2.SEQUENCE_OF_REGENERATORS_INITIAL_ROUTE_ATTRIBUTE_NAME : WDMUtils.SEQUENCE_OF_REGENERATORS_ATTRIBUTE_NAME) == null? new int [seqLinks.size()] : StringUtils.toIntArray(StringUtils.split(r.getAttribute(SEQUENCE_OF_REGENERATORS_ATTRIBUTE_NAME), " "));
			} catch (Exception e) { throw new WDMException("RSA not correctly defined in the attributes: " + WDMUtils2.SEQUENCE_OF_FREQUENCYSLOTS_ATTRIBUTE_NAME + " = " + r.getAttribute(SEQUENCE_OF_FREQUENCYSLOTS_ATTRIBUTE_NAME) + "; SEQUENCE_OF_REGENERATORS_ATTRIBUTE_NAME = " + r.getAttribute(SEQUENCE_OF_REGENERATORS_ATTRIBUTE_NAME)); }
			if (getNumSlots() != r.getOccupiedCapacityInNoFailureState()) throw new WDMException("The occupied link capacity is different to the number of slots");
			checkValidity(false);
		}
		
		public RSA (ProtectionSegment r)
		{
			this.ingressNode = r.getOriginNode();
			this.egressNode = r.getDestinationNode();
			this.seqLinks = new ArrayList<Link> (r.getSeqLinks());
			try 
			{ 
				this.seqFrequencySlots = StringUtils.readIntMatrix(r.getAttribute(WDMUtils2.SEQUENCE_OF_FREQUENCYSLOTS_ATTRIBUTE_NAME));
				this.seqRegeneratorsOccupancy = r.getAttribute(WDMUtils.SEQUENCE_OF_REGENERATORS_ATTRIBUTE_NAME) == null? new int [seqLinks.size()] : StringUtils.toIntArray(StringUtils.split(r.getAttribute(SEQUENCE_OF_REGENERATORS_ATTRIBUTE_NAME), " "));
			} catch (Exception e) { throw new WDMException("RSA not correctly defined in the attributes: " + WDMUtils2.SEQUENCE_OF_FREQUENCYSLOTS_ATTRIBUTE_NAME + " = " + r.getAttribute(SEQUENCE_OF_FREQUENCYSLOTS_ATTRIBUTE_NAME) + "; SEQUENCE_OF_REGENERATORS_ATTRIBUTE_NAME = " + r.getAttribute(SEQUENCE_OF_REGENERATORS_ATTRIBUTE_NAME)); }
			if (getNumSlots() != r.getOccupiedLinkCapacity()) throw new WDMException("The occupied link capacity is different to the number of slots");
			checkValidity(false);
		}

		public RSA (List<Link> seqLinks , IntMatrix2D seqFrequencySlots , int [] seqRegenerators)
		{ 
			this.seqLinks = new ArrayList<Link> (seqLinks); this.seqFrequencySlots = seqFrequencySlots;
			this.ingressNode = seqLinks.get(0).getOriginNode();
			this.egressNode = seqLinks.get(seqLinks.size()-1).getDestinationNode();
			this.seqRegeneratorsOccupancy = seqRegenerators == null? new int [seqLinks.size()] : Arrays.copyOf(seqRegenerators , seqRegenerators.length); 
			checkValidity (false);
		}

		public RSA (List<Link> seqLinks , IntMatrix2D seqFrequencySlots)
		{ 
			this.seqLinks = new ArrayList<Link> (seqLinks); this.seqFrequencySlots = seqFrequencySlots;
			this.ingressNode = seqLinks.get(0).getOriginNode();
			this.egressNode = seqLinks.get(seqLinks.size()-1).getDestinationNode();
			this.seqRegeneratorsOccupancy = new int [seqLinks.size()]; 
			
			checkValidity (false);
		}
		public RSA (List<Link> seqLinks , int initialSlot , int numSlots)
		{ 
			this.seqLinks = new ArrayList<Link>(seqLinks); 
			this.seqFrequencySlots = IntFactory2D.dense.make(numSlots , seqLinks.size());
			for (int e = 0; e < seqLinks.size() ; e ++)
				for (int s = 0 ; s < numSlots ; s ++) seqFrequencySlots.set(s,e,s+initialSlot);
			this.ingressNode = seqLinks.get(0).getOriginNode();
			this.egressNode = seqLinks.get(seqLinks.size()-1).getDestinationNode();
			this.seqRegeneratorsOccupancy = new int [seqLinks.size()]; 
			checkValidity (false);
		}
		public RSA (List<Link> seqLinks , int initialSlot)
		{ 
			this (seqLinks , initialSlot , 1);
		}
		
		public double getLengthInKm () { double accum = 0; for (Link e : seqLinks) accum += e.getLengthInKm(); return accum; }
		
		public int getNumSlots () { return seqFrequencySlots.rows(); }
		
		public void checkValidity (boolean checkRegenerators)
		{
			final int S = seqFrequencySlots.rows();
			final int E = seqFrequencySlots.columns();
			if (seqLinks == null) throw new WDMException ("The sequence of links is null");
			if (seqLinks.isEmpty()) throw new WDMException ("The sequence of links is empty");
			if (seqLinks.size() != E) throw new WDMException ("Wrong RSA");
			if (S == 0) throw new WDMException ("Wrong RSA");
			if (seqFrequencySlots.getMinLocation() [0] < 0) throw new WDMException ("Wrong slot identifier (cannot be negative)"); 
			if (checkRegenerators)
			{
				if (seqRegeneratorsOccupancy.length != seqLinks.size()) throw new WDMException ("Wrong regenerators occupancy in the RSA");
				seqRegeneratorsOccupancy [0] = 0;
				for (int counterLink = 1; counterLink < E ; counterLink ++)
				{
					if (seqRegeneratorsOccupancy [counterLink] == 0)
						if (!seqFrequencySlots.viewColumn(counterLink).equals(seqFrequencySlots.viewColumn(counterLink-1)))
							throw new WDMException ("Wrong regenerators occupancy in the RSA: a regenrator is not placed in a node where the lighptath slot wavelengths change");
				}
			}
		}
		
		
	}
	
	/**
	 * This class represents the request to add a new lightpath. It is used in online algorithms related to WDM networks, inside {@code SimEvent} objects.
	 */
	public static class LightpathAdd 
	{ 
		public Route lpAddedToFillByProcessor; 
		public final NetworkLayer layer; 
		public final Demand demand; 
		public final Node ingressNode, egressNode; 
		public final RSA primaryRSA , backupRSA;   
		public final double lineRateGbps;

		/**
		 * Constructor to generate a new {@code LightpathAdd} object.
		 * @param demand Demand
		 * @param lineRateGbps Line rate in Gbps
		 */
		public LightpathAdd(Demand demand, double lineRateGbps) { this.demand = demand; this.primaryRSA = null; this.lineRateGbps = lineRateGbps; this.backupRSA = null; this.ingressNode = demand.getIngressNode(); this.egressNode = demand.getEgressNode(); this.layer = demand.getLayer(); }

		/**
		 * Constructor to generate a new {@code LightpathAdd} object.
		 * @param demand Demand
		 * @param rsa The lightpath RSA 
		 * @param lineRateGbps Line rate in Gbps
		 */
		public LightpathAdd(Demand demand, RSA rsa , double lineRateGbps) { this.demand = demand; this.primaryRSA = rsa; this.lineRateGbps = lineRateGbps; this.backupRSA = null; this.ingressNode = demand.getIngressNode(); this.egressNode = demand.getEgressNode(); this.layer = demand.getLayer(); }

		/**
		 * Constructor to generate a new {@code LightpathAdd} object.
		 * @param demand Demand
		 * @param seqLinks_primary Primary sequence of fibers
		 * @param seqLinks_backup Backup sequence of fibers
		 * @param seqWavelengths_primary Primary sequence of wavelengths
		 * @param seqWavelengths_backup Backup sequence of wavelengths
		 * @param lineRateGbps Line rate in Gbps
		 */
		public LightpathAdd(Demand demand, RSA primaryRSA , RSA backupRSA , double lineRateGbps) { this.demand = demand; this.primaryRSA = primaryRSA; this.lineRateGbps = lineRateGbps; this.backupRSA = backupRSA ; this.ingressNode = demand.getIngressNode(); this.egressNode = demand.getEgressNode(); this.layer = demand.getLayer(); }

		/**
		 * Constructor to generate a new {@code LightpathAdd} object.
		 * @param ingressNode Ingress node
		 * @param egressNode Egress node
		 * @param layer Network layer
		 * @param lineRateGbps Line rate in Gbps
		 */
		public LightpathAdd(Node ingressNode , Node egressNode , NetworkLayer layer , double lineRateGbps) { this.demand = null; this.primaryRSA = null; this.backupRSA = null; this.lineRateGbps = lineRateGbps; this.ingressNode = ingressNode; this.egressNode = egressNode; this.layer = layer; }

		/**
		 * Constructor to generate a new {@code LightpathAdd} object.
		 * @param ingressNode Ingress node
		 * @param egressNode Egress node
		 * @param layer Network layer
		 * @param seqLinks Sequence of fibers
		 * @param seqWavelengths Sequence of wavelengths
		 * @param lineRateGbps Line rate in Gbps
		 */
		public LightpathAdd(NetworkLayer layer , RSA rsa , double lineRateGbps) { this.demand = null; this.primaryRSA = rsa; this.backupRSA = null; this.lineRateGbps = lineRateGbps; this.ingressNode = rsa.ingressNode; this.egressNode = rsa.egressNode; this.layer = layer; }

		/**
		 * Constructor to generate a new {@code LightpathAdd} object.
		 * @param ingressNode Ingress node
		 * @param egressNode Egress node
		 * @param layer Network layer
		 * @param seqLinks_primary Primary sequence of links
		 * @param seqLinks_backup Backup sequence of links
		 * @param seqWavelengths_primary Primary sequence of wavelengths
		 * @param seqWavelengths_backup Backup sequence of wavelengths
		 * @param lineRateGbps Line rate in Gbps
		 */
		public LightpathAdd(NetworkLayer layer , RSA primaryRSA, RSA backupRSA , double lineRateGbps) 
		{ 
			this.demand = null; this.primaryRSA = primaryRSA; this.backupRSA = backupRSA; this.lineRateGbps = lineRateGbps; this.ingressNode = primaryRSA.ingressNode; this.egressNode = primaryRSA.egressNode; this.layer = layer; 
			if (primaryRSA.ingressNode != backupRSA.ingressNode) throw new WDMException ("primary and backup RSAs must hava common end nodes"); 
			if (primaryRSA.egressNode != backupRSA.egressNode) throw new WDMException ("primary and backup RSAs must hava common end nodes"); 
		}
	}; 

	/**
	 * This class represents the request to remove an existing lightpath. It is used in online algorithms related to WDM networks, inside {@code SimEvent} objects.
	 */
	public static class LightpathRemove { public final Route lp;

		/**
		 * Constructor to generate a new {@code LightpathRemove} object.
		 * @param lp Route
		 */
		public LightpathRemove (Route lp) { this.lp = lp; } };

	/**
	 * This class represents the request to modify an existing lightpath. It is used in online algorithms related to WDM networks, inside {@code SimEvent} objects.
	 */
	public static class LightpathModify 
	{ 
		public final Route lp; 
		public final RSA rsa; 
		public final double carriedTraffic; 
//		public final double occupiedLinkCapacity;

		/**
		 * Constructor to generate a new {@code LightpathModify} object.
		 * @param lp Route to modify
		 * @param seqLinks New sequence of fibers
		 * @param carriedTraffic New carried traffic
		 * @param occupiedLinkCapacity New occupied link capacity
		 * @param seqWavelengths New sequence of wavelengths
		 */
		public LightpathModify (Route lp , RSA rsa , double carriedTraffic) { this.lp = lp; this.rsa = rsa; this.carriedTraffic = carriedTraffic; }  
	};
	
	/**
	 * Route/protection segment attribute name for sequence of regenerators.
	 * 
	 * @since 0.3.0
	 */
	public final static String SEQUENCE_OF_REGENERATORS_ATTRIBUTE_NAME = "seqRegenerators";
	
	/**
	 * Route/protection segment attribute name for sequence of wavelengths.
	 * 
	 * @since 0.3.0
	 */
	public final static String SEQUENCE_OF_FREQUENCYSLOTS_ATTRIBUTE_NAME = "seqFrequencySlots";
	
	/**
	 * Route/protection segment attribute name for sequence of wavelengths for the initial sequence of links (when the route was created)
	 * 
	 * @since 0.3.0
	 */
	public final static String SEQUENCE_OF_FREQUENCYSLOTS_INITIAL_ROUTE_ATTRIBUTE_NAME = "seqFrequencySlotsInitialRoute";
	
	/**
	 * Route/protection segment attribute name for sequence of regenerators occupied for the initial sequence of links (when the route was created)
	 * 
	 * @since 0.3.0
	 */
	public final static String SEQUENCE_OF_REGENERATORS_INITIAL_ROUTE_ATTRIBUTE_NAME = "seqRegeneratorsInitialRoute";

	private static class WDMException extends Net2PlanException
	{
		public WDMException(String message)
		{
			super("WDM: " + message);
		}
	}

	private WDMUtils2() { }

//	/**
//	 * Creates a new lightpath and updates the wavelength occupancy.
//	 * @param demand Demand
//	 * @param seqFibers Sequence of fibers
//	 * @param binaryRatePerChannel Binary rate per channel in Gbps
//	 * @param wavelengthId Wavelength identifier (the same for all traversed fibers)
//	 * @param wavelengthFiberOccupancy Occupied fibers in each wavelength
//	 * @return The newly create lightpath (as a route)
//	 */
//	public static Route addLightpathAndUpdateOccupancy (Demand demand , List<Link> seqFibers, double binaryRatePerChannel , int wavelengthId, DoubleMatrix2D wavelengthFiberOccupancy)
//	{
//		return addLightpathAndUpdateOccupancy (demand , seqFibers, binaryRatePerChannel , IntUtils.constantArray(seqFibers.size() , wavelengthId), null , wavelengthFiberOccupancy, null);
//	}
//
//	/**
//	 * Creates a new lightpath and updates the wavelength occupancy.
//	 * @param demand Demand
//	 * @param seqFibers Sequence of fibers
//	 * @param binaryRatePerChannel Binary rate per channel in Gbps
//	 * @param seqWavelengths Sequence of wavelengths
//	 * @param wavelengthFiberOccupancy Occupied fibers in each wavelength
//	 * @return The newly create lightpath (as a route)
//	 */
//	public static Route addLightpathAndUpdateOccupancy (Demand demand , List<Link> seqFibers, double binaryRatePerChannel , int[] seqWavelengths, DoubleMatrix2D wavelengthFiberOccupancy)
//	{
//		return addLightpathAndUpdateOccupancy (demand , seqFibers, binaryRatePerChannel , seqWavelengths, null , wavelengthFiberOccupancy, null);
//	}

	/**
	 * Creates a new lightpath and updates the wavelength occupancy.
	 * @param demand Demand
	 * @param seqFibers Sequence of fibers
	 * @param binaryRatePerChannel Binary rate per channel in Gbps
	 * @param seqFrequencySlots Sequence of wavelengths
	 * @param seqRegenerators Sequence of regenerators
	 * @param wavelengthFiberOccupancy Occupied fibers in each wavelength
	 * @param nodeRegeneratorOccupancy Number of regenerators installed per node
	 * @return The newly create lightpath (as a route)
	 */
	public static Route addLightpath (Demand demand , RSA rsa , double binaryRatePerChannel)
	{
		NetPlan np = demand.getNetPlan();
		Route lp = np.addRoute(demand , binaryRatePerChannel , rsa.getNumSlots() , rsa.seqLinks, null);
		setLightpathRSAAttributes (lp , rsa , false);
		setLightpathRSAAttributes (lp , rsa , true);
		return lp;
	}

	/**
	 * Creates a new protection lightpath and updates the wavelength occupancy.
	 * @param seqFibers Sequence of fibers
	 * @param seqFrequencySlots Sequence of wavelengths
	 * @param seqRegenerators Sequence of regeneratos
	 * @param wavelengthFiberOccupancy Occupied fibers in each wavelength
	 * @param nodeRegeneratorOccupancy Number of regenerators installed per node
	 * @return The newly created lightpath (as a protection segment)
	 */
	public static ProtectionSegment addLightpathAsProtectionSegment (RSA rsa)
	{
		NetPlan np = rsa.seqLinks.get(0).getNetPlan();
		ProtectionSegment lp = np.addProtectionSegment(rsa.seqLinks, rsa.getNumSlots() , null);
		setLightpathRSAAttributes (lp , rsa);
		return lp;
	}

	/**
	 * <p>Performs extra checks to those applicable to every network design, especially
	 * focused on WDM networks.</p>
	 *
	 * @param netPlan A {@link com.net2plan.interfaces.networkDesign.NetPlan} representing a WDM physical topology
	 * @param countDownLightpathResources Wheter or not include lightpaths that are down
	 * @param optionalLayerParameter Network layer (optional)
	 */
	public static void checkConsistency(NetPlan netPlan, boolean countDownLightpathResources , NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = netPlan.checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		
		for (Route lpRoute : netPlan.getRoutes(layer))
			if (!lpRoute.isDown())
				new RSA (lpRoute , false).checkValidity(true); // makes the check

		for (ProtectionSegment protectionLp : netPlan.getProtectionSegments(layer))
			if (!protectionLp.isDown())
				new RSA (protectionLp).checkValidity(true); // makes the check

		getNetworkSlotAndRegeneratorOcupancy(netPlan , countDownLightpathResources , layer); // serves as check
	}
	
	/**
	 * Returns the list of nodes within the lightpath route containing a regenerator,
	 * only following a distance criterium, assuming no wavelength conversion is required.
	 *
	 * @param seqFibers Sequence of traversed fibers
	 * @param maxRegeneratorDistanceInKm Maximum regeneration distance
	 * @param l_f Physical length in km per fiber
	 * @return A vector with as many elements as traversed links in the route/segment. Each element is a 1 if an optical regenerator is used at the origin node of the corresponding link, and a 0 if not. First element is always 0.
	 */
	public static int[] computeRegeneratorPositions(List<Link> seqFibers, DoubleMatrix1D l_f, double maxRegeneratorDistanceInKm)
	{
		int numHops = seqFibers.size();

		double accumDistance = 0;
		int[] seqRegenerators = new int[numHops];

		ListIterator<Link> it = seqFibers.listIterator();
		while (it.hasNext())
		{
			int hopId = it.nextIndex();
			Link fiber = it.next();
			double fiberLengthInKm = l_f.get(fiber.getIndex());

			if (fiberLengthInKm > maxRegeneratorDistanceInKm)
				throw new WDMException(String.format("Fiber %d is longer (%f km) than the maximum distance without regenerators (%f km)", fiber.getId(), fiberLengthInKm, maxRegeneratorDistanceInKm));

			accumDistance += fiberLengthInKm;

			if (accumDistance > maxRegeneratorDistanceInKm)
			{
				seqRegenerators[hopId] = 1;
				accumDistance = fiberLengthInKm;
			}
			else
			{
				seqRegenerators[hopId] = 0;
			}
		}

		return seqRegenerators;
	}

	/**
	 * Returns the list of nodes within the lightpath route containing a regenerator,
	 * only following a distance criterium, assuming wavelength conversion is required.
	 *
	 * @param seqFibers Sequence of traversed fibers
	 * @param seqFrequencySlots Sequence of frequency slots occupied (one row per slot, one column per traversed fiber)
	 * @param l_f Physical length in km per fiber
	 * @param maxRegeneratorDistanceInKm Maximum regeneration distance
	 * @return A vector with as many elements as traversed links in the route/segment. Each element is a 1 if an optical regenerator is used at the origin node of the corresponding link, and a 0 if not. First element is always 0.
	 */
	public static int[] computeRegeneratorPositions(List<Link> seqFibers, DoubleMatrix2D seqFrequencySlots, DoubleMatrix1D l_f, double maxRegeneratorDistanceInKm)
	{
		int numHops = seqFibers.size();

		double accumDistance = 0;
		int[] seqRegenerators = new int[numHops];

		ListIterator<Link> it = seqFibers.listIterator();
		while (it.hasNext())
		{
			int hopId = it.nextIndex();
			Link fiber = it.next();
			double fiberLengthInKm = l_f.get(fiber.getIndex ());

			if (fiberLengthInKm > maxRegeneratorDistanceInKm)
			{
				throw new WDMException(String.format("Fiber %d is longer (%f km) than the maximum distance without regenerators (%f km)", fiber.getId(), fiberLengthInKm, maxRegeneratorDistanceInKm));
			}

			accumDistance += fiberLengthInKm;

			if (accumDistance > maxRegeneratorDistanceInKm || (hopId > 0 && (!seqFrequencySlots.viewColumn(hopId - 1).equals(seqFrequencySlots.viewColumn(hopId) ))))
			{
				seqRegenerators[hopId] = 1;
				accumDistance = fiberLengthInKm;
			}
			else
			{
				seqRegenerators[hopId] = 0;
			}
		}

		return seqRegenerators;
	}
	
	/**
	 * Returns the number of frequency slots for the given fiber. It is equivalent to 
	 * the method {@link Link#getCapacity() getCapacity()}
	 * from the {@link com.net2plan.interfaces.networkDesign.Link Link} object,
	 * but converting capacity value from {@code double} to {@code int}.
	 * 
	 * @param fiber Link fiber
	 * @return Number of frequency slots
	 */
	public static int getFiberNumFrequencySlots(Link fiber)
	{
		double u_e = fiber.getCapacity();
		int w_f_thisFiber = (int) u_e;
		if (Math.abs(u_e - w_f_thisFiber) > 1e-6) throw new WDMException("Link capacity must be a non-negative integer representing the number of wavelengths of the fiber");
		return w_f_thisFiber;
	}


	/**
	 * Returns the total number of frequency slots in each fiber.
	 * 
	 * @param netPlan A {@link com.net2plan.interfaces.networkDesign.NetPlan} representing a WDM physical topology
	 * @param optionalLayerParameter Network layer (optional)
	 * @return Number of wavelengths per fiber
	 */
	public static DoubleMatrix1D getVectorFiberNumFrequencySlots (NetPlan netPlan, NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = netPlan.checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		Collection<Link> fibers = netPlan.getLinks(layer);
		DoubleMatrix1D w_f = DoubleFactory1D.dense.make (fibers.size ());
		for (Link fiber : fibers) w_f.set(fiber.getIndex () , getFiberNumFrequencySlots(fiber));
		return w_f;
	}

	/**
	 * Returns {@code true} if the given sequence of wavelengths has not been allocated in the given sequence of links, {@code false} otherwise.
	 * @param rsa the RSA to check
	 * @param wavelengthFiberOccupancy Occupied fibers in each wavelength
	 * @return See description above
	 */
	public static boolean isAllocatableRSASet (DoubleMatrix2D wavelengthFiberOccupancy , RSA ... rsas)
	{
		IntMatrix2D checkMatrix = IntFactory2D.sparse.make (wavelengthFiberOccupancy.rows () , wavelengthFiberOccupancy.columns());
		for (RSA rsa : rsas)
		{
			int orderTravLink = 0; 
			for (Link e : rsa.seqLinks)
			{
				for (int s = 0; s < rsa.seqFrequencySlots.rows() ; s ++)
				{
					final int slotIndex = rsa.seqFrequencySlots.get(orderTravLink,s);
					final int linkIndex = e.getIndex();
					if (wavelengthFiberOccupancy.get (slotIndex , linkIndex) != 0) return false;
					if (checkMatrix.get (slotIndex , linkIndex) != 0) return false;
					checkMatrix.set (slotIndex , linkIndex , 1);
				}
				orderTravLink ++;
			}
		}
		return true;
	}

	/**
	 * Returns the fiber occupied (columns) in each wavelength (rows).
	 * @param netPlan Current design
	 * @param countDownLightpathResources Include lightpaths that are down
	 * @param optionalLayerParameter Network layer (optional)
	 * @return Fibers occupied in each wavelength
	 */
	public static Pair<DoubleMatrix2D,DoubleMatrix1D> getNetworkSlotAndRegeneratorOcupancy(NetPlan netPlan, boolean countDownLightpathResources , NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = netPlan.checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		final int E = netPlan.getNumberOfLinks (layer);
		final int N = netPlan.getNumberOfNodes ();
		DoubleMatrix1D w_f = getVectorFiberNumFrequencySlots (netPlan, layer);
		final int W = w_f.size () == 0? 0 : (int) w_f.getMaxLocation() [0];
		DoubleMatrix2D wavelengthFiberOccupancy = DoubleFactory2D.dense.make (W,E);
		DoubleMatrix1D nodeRegeneratorOccupancy = DoubleFactory1D.dense.make (N);
	
		/* The wavelengths above the maximum number of wavelengths of a fiber, are set as occupied */
		for (int e = 0 ; e < E ; e ++) for (int w = (int) w_f.get(e) ; w < W ; w ++) wavelengthFiberOccupancy.set (e,w,1);
		
		/* Wavlengths occupied by the lightpaths as routes */
		for (Route lpRoute : netPlan.getRoutes(layer))
		{
			if (!countDownLightpathResources && lpRoute.isDown()) continue;
			if (lpRoute.getOccupiedCapacity() == 0) continue; // not been used now
			allocateResources(new RSA (lpRoute , false) , wavelengthFiberOccupancy , nodeRegeneratorOccupancy);
		}

		/* Wavlengths occupied by the lightpaths as protection segments */
		for (ProtectionSegment segment : netPlan.getProtectionSegments(layer))
		{
			if (!countDownLightpathResources && segment.isDown()) continue;
			if (segment.getReservedCapacityForProtection() == 0) continue; // not been used now
			allocateResources(new RSA (segment) , wavelengthFiberOccupancy , nodeRegeneratorOccupancy);
//			if (segment.getTraversingRoutes().size() == 1) continue; // its occupancy was already updated
//			if (segment.getTraversingRoutes().size() > 1) throw new RuntimeException ("Bad");
		}			

		return Pair.of(wavelengthFiberOccupancy,nodeRegeneratorOccupancy);
	}
	
	/**
	 * Updates {@code wavelengthFiberOccupancy} to consider that a lightpath is releasing
	 * used wavelengths.
	 * 
	 * @param seqFibers Sequence of traversed fibers
	 * @param seqWavelengths Sequence of wavelengths (as many as the number of links in the lightpath)
	 * @param wavelengthFiberOccupancy Occupied fibers in each wavelength
	 * @param seqRegenerators A 0-1 array indicating whether (1) or not (0) a regenerator/wavelength converter is required at the origin node of the corresponding fiber
	 * @param nodeRegeneratorOccupancy Number of regenerators installed per node
	 */
	public static void releaseResources(RSA rsa , DoubleMatrix2D wavelengthFiberOccupancy, DoubleMatrix1D nodeRegeneratorOccupancy)
	{
		final int S = rsa.getNumSlots();
		ListIterator<Link> fiberIt = rsa.seqLinks.listIterator();
		while(fiberIt.hasNext())
		{
			final int hopId = fiberIt.nextIndex();
			final Link fiber = fiberIt.next();
			for (int s = 0; s < S ; s ++)
			{
				final int slotId = (int) rsa.seqFrequencySlots.get(s,hopId);
				final boolean wasOccupied = wavelengthFiberOccupancy.get(slotId, fiber.getIndex ()) != 0;
				if (!wasOccupied) throw new WDMException("Wavelength " + slotId + " was unused in fiber " + fiber.getId ());
				wavelengthFiberOccupancy.set(slotId, fiber.getIndex () , 0.0);
			}
			if (rsa.seqRegeneratorsOccupancy != null) 
				if (rsa.seqRegeneratorsOccupancy[hopId] == 1)
				{
					Node node = fiber.getOriginNode();
					nodeRegeneratorOccupancy.set(node.getIndex (), nodeRegeneratorOccupancy.get(node.getIndex()) - 1);
				}
		}
	}
	
	/**
	 * Sets the number of wavelengths available on the given fiber.
	 *
	 * @param fiber Link fiber
	 * @param numWavelengths Number of wavelengths for the given fiber
	 */
	public static void setFiberNumFrequencySlots(Link fiber, int numWavelengths)
	{
		if (numWavelengths < 0) throw new WDMException("'numWavelengths' must be a non-negative integer");
		fiber.setCapacity(numWavelengths);
	}

	/**
	 * Sets the number of wavelengths available in each fiber to the same value.
	 * 
	 * @param netPlan A {@link com.net2plan.interfaces.networkDesign.NetPlan} representing a WDM physical topology
	 * @param numWavelengths Number of wavelengths for all fibers
	 * @param optionalLayerParameter Network layer (optional)
	 */
	public static void setFibersNumFrequencySlots(NetPlan netPlan, int numWavelengths , NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = netPlan.checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		if (numWavelengths < 0) throw new WDMException("'numWavelengths' must be a non-negative integer");
		for (Link fiber : netPlan.getLinks (layer)) fiber.setCapacity(numWavelengths);
	}

	/**
	 * Sets the sequence of wavelengths for the given lightpath.
	 * 
	 * @param lp Lightpath (as a route)
	 * @param seqFrequencySlots Sequence of wavelengths (as many as the number of links in the lightpath)
	 */
	public static void setLightpathRSAAttributes (Route lp , RSA rsa , boolean initializeTheInitialRoute)
	{
		lp.setAttribute(initializeTheInitialRoute? SEQUENCE_OF_FREQUENCYSLOTS_INITIAL_ROUTE_ATTRIBUTE_NAME : SEQUENCE_OF_FREQUENCYSLOTS_ATTRIBUTE_NAME, StringUtils.writeMatrix(rsa.seqFrequencySlots));
		lp.setAttribute(initializeTheInitialRoute? SEQUENCE_OF_REGENERATORS_INITIAL_ROUTE_ATTRIBUTE_NAME : SEQUENCE_OF_REGENERATORS_ATTRIBUTE_NAME, IntUtils.join(rsa.seqRegeneratorsOccupancy, " "));
	}

	/**
	 * Sets the current wavelength for the given lightpath, assuming no wavelength conversion.
	 *
	 * @param lp Lightpath (as a route)
	 * @param seqWavelengths Sequence of wavelengths
	 */
	public static void setLightpathRSAAttributes (ProtectionSegment lp , RSA rsa)
	{
		lp.setAttribute(SEQUENCE_OF_FREQUENCYSLOTS_ATTRIBUTE_NAME, StringUtils.writeMatrix(rsa.seqFrequencySlots));
		lp.setAttribute(SEQUENCE_OF_REGENERATORS_ATTRIBUTE_NAME, IntUtils.join(rsa.seqRegeneratorsOccupancy, " "));
	}

	/**
	 * <p>Wavelength assignment algorithm based on a first-fit fashion. Wavelengths
	 * are indexed from 0 to <i>W<sub>f</sub></i>-1, where <i>W<sub>f</sub></i>
	 * is the number of wavelengths supported by fiber <i>f</i>. Then, the wavelength
	 * assigned to each lightpath (along the whole physical route) is the minimum
	 * index among the common free-wavelength set for all traversed fibers.</p>
	 *
	 * <p>In case a lightpath cannot be allocated, the output will be an empty array.</p>
	 * 
	 * <p><b>Important</b>: {@code wavelengthFiberOccupancy} is not updated, so 
	 * subsequent usage of {@link #allocateResources(List, int[], DoubleMatrix2D, int[], DoubleMatrix1D) allocateResources} method is encouraged.</p>
	 *
	 * @param seqFibers Sequence of traversed fibers
	 * @param wavelengthFiberOccupancy Occupied fibers in each wavelength
	 * @return Sequence of wavelengths traversed by each lightpath
	 */
	public static int[] WA_firstFit(List<Link> seqFibers, DoubleMatrix2D wavelengthFiberOccupancy)
	{
		GraphUtils.checkRouteContinuity(seqFibers, Constants.CheckRoutingCycleType.NO_REPEAT_LINK);
		final int W = wavelengthFiberOccupancy.rows();
		for(int wavelengthId = 0; wavelengthId < W; wavelengthId++)
		{
			boolean freePaths = true;
			for (Link link : seqFibers) if (wavelengthFiberOccupancy.get(wavelengthId,link.getIndex()) != 0) {freePaths = false; break; } 
			if (!freePaths) continue;
			return IntFactory1D.dense.make (seqFibers.size(),wavelengthId).toArray ();
		}
		return new int[0];
	}

	/**
	 * <p>Wavelength assignment algorithm based on a first-fit fashion. Wavelengths
	 * are indexed from 0 to <i>W<sub>f</sub></i>-1, where <i>W<sub>f</sub></i>
	 * is the number of wavelengths supported by fiber <i>f</i>. Then, the wavelength
	 * assigned to each lightpath (along the whole physical route) is the minimum
	 * index among the common free-wavelength set for all traversed fibers.</p>
	 *
	 * <p>In case a lightpath cannot be allocated, the output will be an empty array.</p>
	 *
	 * <p><b>Important</b>: {@code wavelengthFiberOccupancy} is not updated, so
	 * subsequent usage of {@link #allocateResources(List, int[], DoubleMatrix2D, int[], DoubleMatrix1D) allocateResources} method is encouraged.</p>
	 *
	 * @param seqFibers_1 First sequence of traversed fibers
	 * @param seqFibers_2 Second sequence of traversed fibers
	 * @param wavelengthFiberOccupancy Occupied fibers in each wavelength
	 * @return Pair of sequences of wavelengths traversed by each lightpath
	 */
	public static Pair<int[],int[]> WA_firstFitTwoRoutes(List<Link> seqFibers_1, List<Link> seqFibers_2 , DoubleMatrix2D wavelengthFiberOccupancy)
	{
		GraphUtils.checkRouteContinuity(seqFibers_1, Constants.CheckRoutingCycleType.NO_REPEAT_LINK);
		GraphUtils.checkRouteContinuity(seqFibers_2, Constants.CheckRoutingCycleType.NO_REPEAT_LINK);
		final int W = wavelengthFiberOccupancy.rows();
		HashSet<Link> auxSet = new HashSet<Link> (seqFibers_1); auxSet.removeAll(seqFibers_2);
		final boolean haveLinksInCommon = !auxSet.isEmpty();
		for(int wavelengthId_1 = 0; wavelengthId_1 < W; wavelengthId_1 ++)
		{
			boolean freePath_1 = true;
			for (Link link : seqFibers_1) if (wavelengthFiberOccupancy.get(wavelengthId_1,link.getIndex()) != 0) {freePath_1 = false; break; } 
			if (!freePath_1) continue;
			for(int wavelengthId_2 = 0; wavelengthId_2 < W; wavelengthId_2 ++)
			{
				if (haveLinksInCommon && (wavelengthId_1 == wavelengthId_2)) continue;
				boolean freePath_2 = true;
				for (Link link : seqFibers_2) if (wavelengthFiberOccupancy.get(wavelengthId_2,link.getIndex()) != 0) { freePath_2 = false ; break; }
				if (!freePath_2) continue;
				return Pair.of(IntFactory1D.dense.make (seqFibers_1.size(),wavelengthId_1).toArray () , IntFactory1D.dense.make (seqFibers_2.size(),wavelengthId_2).toArray ());
			}			
		}
		return null;
	}

	/**
	 * <p>Wavelength assignment algorithm based on a first-fit fashion assuming
	 * full wavelength conversion and regeneration. Each node selects the first
	 * free wavelength for its output fiber, and next nodes in the lightpath try
	 * to maintain it. If not possible, or regeneration is needed, then include
	 * a regenerator (can act also as a full wavelength converter) and search
	 * for the first free wavelength, and so on.</p>
	 *
	 * <p>In case a lightpath cannot be allocated, the corresponding sequence of
	 * wavelengths ({@code seqWavelengths} parameter) will be an empty array.</p>
	 *
	 * @param seqFibers Sequence of traversed fibers
	 * @param wavelengthFiberOccupancy Occupied fibers in each wavelength
	 * @param l_f Physical length in km per fiber
	 * @param nodeRegeneratorOccupancy Number of regenerators installed per node
	 * @param maxRegeneratorDistanceInKm Maximum regeneration distance
	 * @return Sequence of wavelengths traversed by each lightpath, and a 0-1 array indicating whether (1) or not (0) a regenerator/wavelength converter is required at the origin node of the corresponding fiber
	 */
	public static Pair<int[], int[]> WA_RPP_firstFit(List<Link> seqFibers, DoubleMatrix2D wavelengthFiberOccupancy, DoubleMatrix1D l_f, DoubleMatrix1D nodeRegeneratorOccupancy, double maxRegeneratorDistanceInKm)
	{
		final int W = wavelengthFiberOccupancy.rows ();

		List<Integer> seqWavelengths = new LinkedList<Integer>();
		List<Integer> seqRegenerators = new LinkedList<Integer>();

		double control_accumDistance = 0;
		Set<Integer> control_occupied_w = new LinkedHashSet<Integer>();
		int control_firstFitValidWavelengthForSubpath = -1;
		List<Link> control_currentSubpathSeqLinks = new LinkedList<Link>();

		boolean lpAllocated = true;

		Map<Link, Set<Integer>> avoidLoopWavelengthClash = new LinkedHashMap<Link, Set<Integer>>();

		for (Link fiber : seqFibers)
		{
			double fiberLengthInKm = l_f.get(fiber.getIndex ());

			if (fiberLengthInKm > maxRegeneratorDistanceInKm)
			{
				throw new WDMException(String.format("Fiber %d is longer (%f km) than the maximum distance without regenerators (%f km)", fiber.getId (), fiberLengthInKm, maxRegeneratorDistanceInKm));
			}

			/* update the info as if this link was included in the subpath */
			final double plusLink_accumDistance = control_accumDistance + fiberLengthInKm;
			int plusLink_firstFitValidWavelengthForSubpath = -1;
			Set<Integer> plusLink_occupied_w = new LinkedHashSet<Integer>(control_occupied_w);

			if (avoidLoopWavelengthClash.containsKey(fiber))
			{
				plusLink_occupied_w.addAll(avoidLoopWavelengthClash.get(fiber));
			}

			for (int wavelengthId = W - 1; wavelengthId >= 0; wavelengthId--)
			{
				if (!plusLink_occupied_w.contains(wavelengthId) && (wavelengthFiberOccupancy.get(wavelengthId , fiber.getIndex()) == 0))
				{
					plusLink_firstFitValidWavelengthForSubpath = wavelengthId;
				}
				else
				{
					plusLink_occupied_w.add(wavelengthId);
				}
			}

			if (!control_currentSubpathSeqLinks.contains(fiber) && plusLink_accumDistance <= maxRegeneratorDistanceInKm && plusLink_firstFitValidWavelengthForSubpath != -1)
			{
				/* we do not have to put a regenerator in the origin node of e: the subpath is valid up to now */
				control_accumDistance = plusLink_accumDistance;
				control_occupied_w = plusLink_occupied_w;
				control_firstFitValidWavelengthForSubpath = plusLink_firstFitValidWavelengthForSubpath;
				control_currentSubpathSeqLinks.add(fiber);
				seqRegenerators.add(0);
				continue;
			}

			/* Here if we have to put a regenerator in initial node of this link, add a subpath */
			if (control_firstFitValidWavelengthForSubpath == -1)
			{
				lpAllocated = false;
				break;
			}

			seqRegenerators.add(1);
			int numFibersSubPath = control_currentSubpathSeqLinks.size();
			for (int cont = 0; cont < numFibersSubPath; cont++)
			{
				seqWavelengths.add(control_firstFitValidWavelengthForSubpath);

				Link aux_fiber = control_currentSubpathSeqLinks.get(cont);
				if (!avoidLoopWavelengthClash.containsKey(aux_fiber))
					avoidLoopWavelengthClash.put(aux_fiber, new LinkedHashSet<Integer>());

				avoidLoopWavelengthClash.get(aux_fiber).add(control_firstFitValidWavelengthForSubpath);
			}

			/* new span includes just this link */
			control_accumDistance = fiberLengthInKm;
			control_currentSubpathSeqLinks = new LinkedList<Link>();
			control_currentSubpathSeqLinks.add(fiber);
			control_occupied_w = new LinkedHashSet<Integer>();
			if (avoidLoopWavelengthClash.containsKey(fiber))
				control_occupied_w.addAll(avoidLoopWavelengthClash.get(fiber));

			control_firstFitValidWavelengthForSubpath = -1;
			for (int wavelengthId = 0; wavelengthId < W; wavelengthId++)
			{
				if (wavelengthFiberOccupancy.get(wavelengthId , fiber.getIndex()) != 0)
					control_occupied_w.add(wavelengthId);
				else if (control_firstFitValidWavelengthForSubpath == -1)
					control_firstFitValidWavelengthForSubpath = wavelengthId;
			}

			if (control_firstFitValidWavelengthForSubpath == -1)
			{
				lpAllocated = false;
				break;
			}
		}

		/* Add the last subpath */
		/* Here if we have to put a regenerator in initial node of this link, add a subpath */
		if (control_firstFitValidWavelengthForSubpath == -1) lpAllocated = false;

		if (!lpAllocated) return Pair.of(new int[0], new int[0]);
		
		int numFibersSubPath = control_currentSubpathSeqLinks.size();
		for (int cont = 0; cont < numFibersSubPath; cont++)
			seqWavelengths.add(control_firstFitValidWavelengthForSubpath);

		return Pair.of(IntUtils.toArray(seqWavelengths), IntUtils.toArray(seqRegenerators));
	}
	
	/**
	 * Updates {@code wavelengthFiberOccupancy} to consider that a new lightpath is occupying 
	 * a wavelength in each fiber.
	 * 
	 * @param seqFibers Sequence of traversed fibers
	 * @param seqFrequencyslots Sequence of wavelengths (as many as the number of links in the lightpath)
	 * @param wavelengthFiberOccupancy Set of occupied fibers in each wavelength
	 * @param seqRegenerators A 0-1 array indicating whether (1) or not (0) a regenerator/wavelength converter is required at the origin node of the corresponding fiber
	 * @param nodeRegeneratorOccupancy Number of regenerators installed per node
	 */
	public static void allocateResources(RSA rsa, DoubleMatrix2D wavelengthFiberOccupancy, DoubleMatrix1D nodeRegeneratorOccupancy)
	{
		final int W = wavelengthFiberOccupancy.rows();
		ListIterator<Link> fiberIt = rsa.seqLinks.listIterator();
		while(fiberIt.hasNext())
		{
			final int hopId = fiberIt.nextIndex();
			final Link fiber = fiberIt.next();
			IntMatrix1D slotIds = rsa.seqFrequencySlots.viewColumn(hopId);
			for (int cont = 0 ; cont < slotIds.size() ; cont ++)
			{
				final int slotId = (int) slotIds.get(cont);
				if (slotId >= W) throw new WDMException ("The slot id is higher than the number of slots available");
				if (wavelengthFiberOccupancy.get(slotId , fiber.getIndex ()) != 0) throw new WDMException ("Wavelength clashing: slot " + slotIds.get(cont) + ", fiber: " + fiber.getId ());
				wavelengthFiberOccupancy.set(slotId , fiber.getIndex () , 1.0);
			}

			if (rsa.seqRegeneratorsOccupancy != null)
				if (rsa.seqRegeneratorsOccupancy[hopId] == 1)
				{
					Node node = fiber.getOriginNode();
					nodeRegeneratorOccupancy.set (node.getIndex (), nodeRegeneratorOccupancy.get(node.getIndex ()) + 1);
				}
		}
	}
}
