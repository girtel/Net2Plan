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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.net2plan.interfaces.networkDesign.Demand;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.interfaces.networkDesign.ProtectionSegment;
import com.net2plan.interfaces.networkDesign.Route;
import com.net2plan.utils.CollectionUtils;
import com.net2plan.utils.Constants;
import com.net2plan.utils.IntUtils;
import com.net2plan.utils.Pair;
import com.net2plan.utils.StringUtils;

import cern.colt.matrix.tdouble.DoubleFactory1D;
import cern.colt.matrix.tdouble.DoubleFactory2D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import cern.colt.matrix.tint.IntFactory2D;
import cern.colt.matrix.tint.IntMatrix1D;
import cern.colt.matrix.tint.IntMatrix2D;

/**
 * Class to deal with WDM optical topologies in fixed-grid and flexi-grid networks, including wavelength assignment and regenerator placement.
 *
 * <p>Optical networks have been established as the enabling technology for today’s high-speed communication networks. Wavelength Division Multiplexing (WDM) enables the efficient utilization of optical fibers by dividing its tremendous bandwidth into a set of disjoint channels.</p>
 * <p>The so-called <em>wavelength grid</em> determines the wavelengths or wavebands used by a channel:</p>
 * <ul>
 * <li>In <em>fixed-grid</em> networks, all the channels have the same width, typically of 100 GHz or 50 GHz. For instance, according to the 100 GHz ITU-T grid, first channel in the C-band has a central frequency of 196.1 THz, next 196.0 THz, next 195.9 THz... (until 191.2 THz, the last channel in the C-band). In this case each channel is typically called a <em>wavelength</em>.</li>
 * <li><em>Flexi-grid</em> networks are a generalization of fixed-grid networks, where the width of a channel is an integer multiple of the width of a so-called frequency slot. Frequency slots are typically of 12.5 GHz. A channel can typically aggregate up to tens of slots. In general (but not always), these slots are constrained to be contiguous.</li>
 * </ul>
 * 
 * <p>Flexi-grid networks where all the channels occupy exactly one frequency slot, and the width of the slot is e.g. 100 GHz, are equivalent to a fixed-grid network with a 100 GHz width. For this reason, after Net2Plan 0.4.1 we have put together inside <em>WDMUtils</em> the functionalities of both fixed and flexi grid WDM networks (formerly, flexi-grid networks were dealt with in <em>FlexiGridUtils</em> library).</p>
 * 
 * <h2>Wavelength-routed networks</h2>
 * <p>In wavelength-routed WDM networks, all-optical channels traversing several fibers, called <b>lightpaths</b>, 
 * can be established between pairs of nodes. A lightpath occupies one channel (a wavelength in fixed-grid networks, and a set of frequency slots in flexi-grid networks) 
 * in each traversed link, and two lightpaths routed over the same physical link cannot use the same frequency slot in that link. 
 * This is called the <b>wavelength or frequency slot clash constraint</b>. The <em>Routing and Spectrum Assignment</em> (RSA) problem and the <em>Routing and Wavelength Assignment</em> (RWA) problem in flexi and fixed grid WDM 
 * networks respectively are the ones deciding for each lightpath the route and frequency slots (wavelengths) to occupy in each traversed fiber. 
 * Typically, the wavelength/slots of a lightpath cannot change along the traversed fibers (since wavelength converters are not available). In this case we say that the problem has the <em>wavelength continuity constraint</em>.  
 * 
 * <p>Higher layers in the network see a lightpath as a pipe to transmit traffic, and they are not necessarily aware of the actual sequence of fiber links of the lightpath.</p>
 * <p>The <em>line rate</em> of the lightpath is its capacity e.g. in Gbps. Typical line rates are 10, 40 and 100 Gbps. In flexi-grid networks, it is possible to have lightpaths of different line rates, that occupy the same number of frequency slots. This is because, the transponders (the ones transmitting and receiving the optical signals) can be based on different optical modulations with different <em>spectral efficiencies</em>. For instance, a transponder using BPSK modulation has a spectral efficiency of 1 bps/Hz, while a more sophisticated transponder using 16-QAM has 4 bps/Hz.</p>
 * <p>Nodes in wavelength-routed networks are called Optical Add/Drop Multiplexers (OADMs). They are able to add new lightpaths initiated in the node, drop lightpaths terminating in the node, and optically switch (bypass) lightpaths that traverse the node. Usually, a lightpath occupies the same wavelength in all the traversed links. This is called the <b>wavelength continuity constraint</b>. However, it is possible (so far only in fixed-grid networks) to allocate optical wavelength converters at intermediate nodes of the lightpath that are able to change its wavelength. Nowadays, wavelength converters are composed of an opto-electronic receiver attached to an electro-optic transmitter. Then, the optical signal is regenerated, while its wavelength can be modified. More often, optical regenerators are used not for changing the wavelength in an intermediate node of a lightpath, but to regenerate the optical signal recovering it from its normal degradation caused by channel noise and other impairments.</p>
 * <p>In Net2Plan, the <em>WDMUtils</em> library is provided to ease the handling of WDM networks. <em>WDMUtils</em> assumptions are:
 * <ul>
 * <li><em>Network layer</em>: A WDM network is represented by a network layer. Typically, the layer name is set to "WDM" (although this is not mandatory). The measure units for the traffic in this layer is assumed to be Gbps. In its turn, the capacity of the links is assumed to be measured in number of frequency slots.</li>
 * <li><em>Links</em>: Each link in the WDM network layer is an optical fibre. The capacity of the link is measured as the (integer) number of frequency slots available in that fiber. For instance, a typical WDM network using the C-band in 50 GHz channels, has 80 available waveleneghts, and thus the link capacity is 80.  </li>
 * <li><em>Nodes</em>: Each node in the network with input/output links in the WDM layer, represents an Optical Add/Drop Multiplexer (OADMs) capable of routing lightpaths.</li>
 * <li><em>Demands</em>: A demand is here an intention to carry traffic between two OADMS. The offered traffic is assumed to be measured in Gbps (e.g. typically 10 Gbps, 40 Gbps, 100 Gbps).</li>
 * <li><em>Routes</em>: A route represents a lightpath. The carried traffic of the lightpath corresponds to the lighptath line rate. The occupied link capacity of the lightpath is the total number of slots occupied (the same total number in all the traversed links). For specifying the particular frequency slots occupied by a lightpath in a link, in Net2Plan we assume that frequency slots in any link are numbered with consecutive numbers starting from zero 0,1,2,... The lowest number typically corresponds to the lower wavelength. The set of frequency slots occupied by a lightpath in each traversed link is stored by <em>WDMUtils</em> in a route attribute, in an internal format. The user using the <em>WDMUtils</em> library does not need to bother about the details of this format. Additionally, it is possible to define the set of nodes where the optical signal goes through a regenerator of the optical signal (with or without wavelength conversion). This information is also stored as Route attributes.</li>
 * <li><em>Protection segments</em>: Zero, one or more protection segments can be associated to the route, representing (partial) backup lightpaths that protect the primary route. The occupied link capacity of the protection segment reflects the total number of frequency slots reserved in the traversed links. The actual set of slots reserved in each traversed, and the places where the signal is regenerated (if any) are also stored by <em>WDMUtils</em> in segment attributes</li>
 * </ul>
 * Network design algorithms and other resources for designing WDM networks can be found in the Net2Plan code repository under keyword <em>WDM</em>.</p>
 */
public class WDMUtils
{
	
	/**
	 * This class represents a Routing and Spectrum Assignment, valid for a lightpath in both fixed and flexi-grid WDM networks. 
	 * This comprises a sequence of links, and for each link the set of frequency slots occupied.
	 * This number of slots must be the same in all the links.
	 */
	public static class RSA
	{
		
		/**
		 * The initial node of the lightpath 
		 */
		public final Node ingressNode;
		/**
		 * The end node of the lightpath 
		 */
		public final Node egressNode;
		/**
		 * The sequence of traversed fibers 
		 */
		public final List<Link> seqLinks;
		/**
		 * A 2D integer matrix with as many columns as traversed links (in the same order), and rows as the number of frequency slots occupied (the same in all the links).
		 * Position (i,j) of the array is the identifier of the i-th slot used in the fiber traversed in the j-th place. Recall that an slot identifier is an integer number between zero and the number of slots in the grid minus one. 
		 */
		public final IntMatrix2D seqFrequencySlots;
		/**
		 * An integer vector with one coordinate per traversed fiber. A 1 is set in the i-th position if a regenerator is needed at origin node of the fiber traversed in the i-th place, and a 0 otherwise.
		 */
		public final int [] seqRegeneratorsOccupancy; // as many coordinates as links traversed

		/** Creates a RSA object reading the information from the existing Route object (and its WDM-related attributes). 
		 * @param r the route object
		 * @param initializeWithTheInitialRoute if {@code true}, the RSA object created gets the information from the route initial sequence of links, and if not, from the route current sequence of traversed links.
		 */
		public RSA (Route r , boolean initializeWithTheInitialRoute)
		{
			this.ingressNode = r.getIngressNode();
			this.egressNode = r.getEgressNode();
			this.seqLinks = initializeWithTheInitialRoute? r.getInitialSequenceOfLinks() : new ArrayList<Link> (r.getSeqLinksRealPath());
			try 
			{ 
				final IntMatrix2D candidateSeqFreqSlots = StringUtils.readIntMatrix(r.getAttribute(initializeWithTheInitialRoute? WDMUtils.SEQUENCE_OF_FREQUENCYSLOTS_INITIAL_ROUTE_ATTRIBUTE_NAME : WDMUtils.SEQUENCE_OF_FREQUENCYSLOTS_ATTRIBUTE_NAME));	
				this.seqFrequencySlots = candidateSeqFreqSlots.rows() > 0? candidateSeqFreqSlots : IntFactory2D.dense.make(0,seqLinks.size());
				this.seqRegeneratorsOccupancy = r.getAttribute(initializeWithTheInitialRoute? WDMUtils.SEQUENCE_OF_REGENERATORS_INITIAL_ROUTE_ATTRIBUTE_NAME : WDMUtils.SEQUENCE_OF_REGENERATORS_ATTRIBUTE_NAME) == null? new int [seqLinks.size()] : StringUtils.toIntArray(StringUtils.split(r.getAttribute(SEQUENCE_OF_REGENERATORS_ATTRIBUTE_NAME), " "));
			} catch (Exception e) { throw new WDMException("RSA not correctly defined in the attributes: " + WDMUtils.SEQUENCE_OF_FREQUENCYSLOTS_ATTRIBUTE_NAME + " = " + r.getAttribute(SEQUENCE_OF_FREQUENCYSLOTS_ATTRIBUTE_NAME) + "; SEQUENCE_OF_REGENERATORS_ATTRIBUTE_NAME = " + r.getAttribute(SEQUENCE_OF_REGENERATORS_ATTRIBUTE_NAME)); }
			if (getNumSlots() != r.getOccupiedCapacityInNoFailureState()) throw new WDMException("The occupied link capacity is different to the number of slots");
			checkValidity();
		}
		
		/** Creates a RSA object reading the information from the existing ProtectionSegment object (and its WDM-related attributes). 
		 * @param r the segment object
		 */
		public RSA (ProtectionSegment r)
		{
			this.ingressNode = r.getOriginNode();
			this.egressNode = r.getDestinationNode();
			this.seqLinks = new ArrayList<Link> (r.getSeqLinks());
			try 
			{ 
				this.seqFrequencySlots = StringUtils.readIntMatrix(r.getAttribute(WDMUtils.SEQUENCE_OF_FREQUENCYSLOTS_ATTRIBUTE_NAME));
				this.seqRegeneratorsOccupancy = r.getAttribute(WDMUtils.SEQUENCE_OF_REGENERATORS_ATTRIBUTE_NAME) == null? new int [seqLinks.size()] : StringUtils.toIntArray(StringUtils.split(r.getAttribute(SEQUENCE_OF_REGENERATORS_ATTRIBUTE_NAME), " "));
			} catch (Exception e) { throw new WDMException("RSA not correctly defined in the attributes: " + WDMUtils.SEQUENCE_OF_FREQUENCYSLOTS_ATTRIBUTE_NAME + " = " + r.getAttribute(SEQUENCE_OF_FREQUENCYSLOTS_ATTRIBUTE_NAME) + "; SEQUENCE_OF_REGENERATORS_ATTRIBUTE_NAME = " + r.getAttribute(SEQUENCE_OF_REGENERATORS_ATTRIBUTE_NAME)); }
			if (getNumSlots() != r.getOccupiedLinkCapacity()) throw new WDMException("The occupied link capacity is different to the number of slots");
			checkValidity();
		}

		/** Creates a RSA object from the provided information
		 * @param seqLinks sequence of traversed fibers (a copy is made internally)
		 * @param seqFrequencySlots 2D array with the sequence of frequency slots information. 
		 * @param seqRegenerators vector with the sequence of traversed regenerators information (if {@code null}, no regenerators are placed)
		 */
		public RSA (List<Link> seqLinks , IntMatrix2D seqFrequencySlots , int [] seqRegenerators)
		{ 
			this.seqLinks = new ArrayList<Link> (seqLinks); 
			this.seqFrequencySlots = seqFrequencySlots;
			this.ingressNode = seqLinks.get(0).getOriginNode();
			this.egressNode = seqLinks.get(seqLinks.size()-1).getDestinationNode();
			this.seqRegeneratorsOccupancy = seqRegenerators == null? new int [seqLinks.size()] : Arrays.copyOf(seqRegenerators , seqRegenerators.length); 
			checkValidity ();
		}

		/** Equivalent to {@code RSA (seqLinks , seqFrequencySlot, null)
		 * @param seqLinks sequence of traversed fibers (a copy is made internally)
		 * @param seqFrequencySlots 2D array with the sequence of frequency slots information. 
		 */
		public RSA (List<Link> seqLinks , IntMatrix2D seqFrequencySlots)
		{ 
			this(seqLinks , seqFrequencySlots , null);
		}

		/** Creates a RSA with the same set of contigous slots occupied in all the traversed fibers
		 * @param seqLinks Sequence of traversed fiber
		 * @param initialSlot id of the initial frequency slot occupied
		 * @param numSlots number of contiguous slots of the RSA
		 */
		public RSA (List<Link> seqLinks , int initialSlot , int numSlots)
		{ 
			this.seqLinks = new ArrayList<Link>(seqLinks); 
			this.seqFrequencySlots = IntFactory2D.dense.make(numSlots , seqLinks.size());
			for (int e = 0; e < seqLinks.size() ; e ++)
				for (int s = 0 ; s < numSlots ; s ++) seqFrequencySlots.set(s,e,s+initialSlot);
			this.ingressNode = seqLinks.get(0).getOriginNode();
			this.egressNode = seqLinks.get(seqLinks.size()-1).getDestinationNode();
			this.seqRegeneratorsOccupancy = new int [seqLinks.size()]; 
			checkValidity ();
		}

		/** Creates a RSA where the same one single slot is occupied in all the traversed links. 
		 * Typically used in fixed-grid networks without wavelength conversion. Equivalent to {@code RSA (seqLinks , occupiedSlotId , 1)}
		 * @param seqLinks sequence of traversed fibers
		 * @param occupiedSlotId the slot occupied
		 */
		public RSA (List<Link> seqLinks , int occupiedSlotId)
		{ 
			this (seqLinks , occupiedSlotId , 1);
		}
		
		/** Gets the list of nodes (in the same order as they are traversed) where frequency slot changes in the RSA. These are 
		 * nodes where regenerators (which are assumed to make also wavelength conversion) would be needed.
		 * @return a list of nodes where frequency slot converters are needed
		 */
		public List<Node> getNodesWithFrequencySlotConversion ()
		{
			List<Node> res = new ArrayList<Node> ();
			final int E = seqLinks.size();
			for (int counterLink = 1; counterLink < E ; counterLink ++)
			{
				if (seqRegeneratorsOccupancy [counterLink] == 0)
					if (!seqFrequencySlots.viewColumn(counterLink).equals(seqFrequencySlots.viewColumn(counterLink-1)))
						res.add(seqLinks.get(counterLink).getOriginNode());
			}
			return res;
		}
		
		/** Returns whether or not frequency slot conversions occur in this RSA (and thus converters would be needed)
		 * @return {@code true} if frequency slot conversion occurs, {@code false} otherwise
		 */
		public boolean hasFrequencySlotConversions () { return getNodesWithFrequencySlotConversion().size() > 0; }

		/** Gets the length of the RSA in km, summing the length of the traversed fibers
		 * @return the length in km
		 */
		public double getLengthInKm () { double accum = 0; for (Link e : seqLinks) accum += e.getLengthInKm(); return accum; }
		
		/** Gets the number of slots occupied in this RSA (the same in all the traversed fibers)
		 * @return the number of slots
		 */
		public int getNumSlots () { return seqFrequencySlots.rows(); }
		
		/** Checks that in every node where the frequency slots of the RSA change, a regenerator is placed (if not, an exception is raised). 
		 * This is assuming that regenerators are the equipment needed to perform wavelength/frequency slot conversion  
		 */
		public void checkFrequencySlotConversionOccupiesARegenerator ()
		{
			final int E = seqFrequencySlots.columns();
			for (int counterLink = 1; counterLink < E ; counterLink ++)
			{
				if (seqRegeneratorsOccupancy [counterLink] == 0)
					if (!seqFrequencySlots.viewColumn(counterLink).equals(seqFrequencySlots.viewColumn(counterLink-1)))
						throw new WDMException ("Wrong regenerators occupancy in the RSA: a regenrator is not placed in a node where the lighptath slot wavelengths change");
			}
		}

		private void checkValidity ()
		{
			final int S = seqFrequencySlots.rows();
			final int E = seqFrequencySlots.columns();
			if (seqLinks == null) throw new WDMException ("The sequence of links is null");
			if (seqLinks.isEmpty()) throw new WDMException ("The sequence of links is empty");
			if (seqLinks.size() != E) throw new WDMException ("Wrong RSA");
			if (S == 0) throw new WDMException ("Wrong RSA");
			if (seqFrequencySlots.getMinLocation() [0] < 0) throw new WDMException ("Wrong slot identifier (cannot be negative)"); 
			if (seqRegeneratorsOccupancy.length != seqLinks.size()) throw new WDMException ("Wrong regenerators occupancy in the RSA");
			seqRegeneratorsOccupancy [0] = 0;
		}

	}
	
	/**
	 * This class represents the request to add a new lightpath. It is used in online algorithms related to WDM networks, inside {@code SimEvent} objects.
	 */
	public static class LightpathAdd 
	{ 
		/**
		 * The event generator puts a null here. The processor fills it with the Route object created, if the lightpath add request is accepted 
		 */
		public Route lpAddedToFillByProcessor; 
		/**
		 * The id of the network WDM layer 
		 */
		public final NetworkLayer layer; 
		/**
		 * The WDM demand the lightpath belongs to 
		 */
		public final Demand demand; 
		/**
		 * The ingress node of the lightpath
		 */
		public final Node ingressNode;
		/**
		 * The egress node of the lightpath
		 */
		public final Node egressNode; 
		/**
		 * The RSA of the primary route of the lightpath to add
		 */
		public final RSA primaryRSA;
		/**
		 * The RSA of the backup lightpath to add as a protection segment (or null if none) 
		 */
		public final RSA backupRSA;   
		/**
		 * The line rate of the lightpath
		 */
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
		 * @param rsa The lightpath RSA of the primary (no backup)
		 * @param lineRateGbps Line rate in Gbps
		 */
		public LightpathAdd(Demand demand, RSA rsa , double lineRateGbps) { this.demand = demand; this.primaryRSA = rsa; this.lineRateGbps = lineRateGbps; this.backupRSA = null; this.ingressNode = demand.getIngressNode(); this.egressNode = demand.getEgressNode(); this.layer = demand.getLayer(); }

		/**
		 * Constructor to generate a new {@code LightpathAdd} object.
		 * @param demand Demand the lighptath belongs to
		 * @param primaryRSA the RSA of the primary (create as a Route)
		 * @param backupRSA the RSA of the backup (create as a protection segment)
		 * @param lineRateGbps line rate of the lightpath
		 */
		public LightpathAdd(Demand demand, RSA primaryRSA , RSA backupRSA , double lineRateGbps) { this.demand = demand; this.primaryRSA = primaryRSA; this.lineRateGbps = lineRateGbps; this.backupRSA = backupRSA ; this.ingressNode = demand.getIngressNode(); this.egressNode = demand.getEgressNode(); this.layer = demand.getLayer(); }

		/**
		 * Constructor to generate a new {@code LightpathAdd} object.
		 * @param ingressNode Ingress node of the lightpath to create
		 * @param egressNode Egress node of the lightpath to create
		 * @param layer The WDM network layer
		 * @param lineRateGbps Line rate in Gbps
		 */
		public LightpathAdd(Node ingressNode , Node egressNode , NetworkLayer layer , double lineRateGbps) { this.demand = null; this.primaryRSA = null; this.backupRSA = null; this.lineRateGbps = lineRateGbps; this.ingressNode = ingressNode; this.egressNode = egressNode; this.layer = layer; }

		/**
		 * Constructor to generate a new {@code LightpathAdd} object.
		 * @param layer The WDM network layer
		 * @param rsa The RSA of the lightpath to create
		 * @param lineRateGbps the line rate of the lightpath
		 */
		public LightpathAdd(NetworkLayer layer , RSA rsa , double lineRateGbps) { this.demand = null; this.primaryRSA = rsa; this.backupRSA = null; this.lineRateGbps = lineRateGbps; this.ingressNode = rsa.ingressNode; this.egressNode = rsa.egressNode; this.layer = layer; }

		/**
		 * Constructor to generate a new {@code LightpathAdd} object.
		 * @param layer The WDM network layer
		 * @param primaryRSA The RSA of the primary 
		 * @param backupRSA The RSA of the backup (to create as a protection segment)
		 * @param lineRateGbps the line rate of the lightpath
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
	public static class LightpathRemove 
	{ 
		/**
		 * The Route object representing the lightpath to remove (in general, protection segments associated to this route will be also removed)
		 */
		public final Route lp;

		/**
		 * Constructor to generate a new {@code LightpathRemove} object.
		 * @param lp Route
		 */
		public LightpathRemove (Route lp) { this.lp = lp; } 
	};

	/**
	 * This class represents the request to modify an existing lightpath. It is used in online algorithms related to WDM networks, inside {@code SimEvent} objects.
	 */
	public static class LightpathModify 
	{ 
		/**
		 * The route object of the lightpath to modify
		 */
		public final Route lp; 
		/**
		 * The new RSA of the lightpath
		 */
		public final RSA rsa; 
		/**
		 * The new carried traffic of the lightpath 
		 */
		public final double carriedTraffic; 

		/**
		 * Constructor to generate a new {@code LightpathModify} object.
		 * @param lp Route to modify
		 * @param rsa the new RSA
		 * @param carriedTraffic new carried traffic
		 */
		public LightpathModify (Route lp , RSA rsa , double carriedTraffic) { this.lp = lp; this.rsa = rsa; this.carriedTraffic = carriedTraffic; }  
	};
	
	/**
	 * Route/protection segment attribute name for sequence of regenerators.
	 */
	private final static String SEQUENCE_OF_REGENERATORS_ATTRIBUTE_NAME = "seqRegenerators";
	
	/**
	 * Route/protection segment attribute name for sequence of wavelengths.
	 */
	private final static String SEQUENCE_OF_FREQUENCYSLOTS_ATTRIBUTE_NAME = "seqFrequencySlots";
	
	/**
	 * Route/protection segment attribute name for sequence of wavelengths for the initial sequence of links (when the route was created)
	 */
	private final static String SEQUENCE_OF_FREQUENCYSLOTS_INITIAL_ROUTE_ATTRIBUTE_NAME = "seqFrequencySlotsInitialRoute";
	
	/**
	 * Route/protection segment attribute name for sequence of regenerators occupied for the initial sequence of links (when the route was created)
	 */
	private final static String SEQUENCE_OF_REGENERATORS_INITIAL_ROUTE_ATTRIBUTE_NAME = "seqRegeneratorsInitialRoute";

	private static class WDMException extends Net2PlanException
	{
		public WDMException(String message)
		{
			super("WDM: " + message);
		}
	}

	private WDMUtils() { }

	/** Creates a new lightpath with the given RSA, as a {@code Route} object. The attributes of the lightpath to store the WDM information are initialized appropriately
	 * @param demand The demand the lightpath belongs to 
	 * @param rsa the RSA 
	 * @param lineRateGbps the line rate of the lightpath in Gbps
	 * @return The newly created lightpath 
	 */
	public static Route addLightpath (Demand demand , RSA rsa , double lineRateGbps)
	{
		NetPlan np = demand.getNetPlan();
		Route lp = np.addRoute(demand , lineRateGbps , rsa.getNumSlots() , rsa.seqLinks, null);
		setLightpathRSAAttributes (lp , rsa , false);
		setLightpathRSAAttributes (lp , rsa , true);
		return lp;
	}

	/** Creates a new lightpath with the given RSA, as a {@code ProtectionSegment} object. The attributes of the lightpath to store the WDM information are initialized appropriately
	 * @param rsa the RSA (traversed fibers, and slots to reserve in them)
	 * @return The newly created lightpath 
	 */
	public static ProtectionSegment addLightpathAsProtectionSegment (RSA rsa)
	{
		NetPlan np = rsa.seqLinks.get(0).getNetPlan();
		ProtectionSegment lp = np.addProtectionSegment(rsa.seqLinks, rsa.getNumSlots() , null);
		setLightpathRSAAttributes (lp , rsa);
		return lp;
	}

	/** Performs some checks in a WDM network. Checks that all the route and protection segment objects of the WDM layer have valid 
	 * values in the attributes storing its RSA and regenerator occupancy. 
	 * In addition, it checks resource clashing: no frequency slot in the same fiber can be occupied by more than one lightpath 
	 * @param netPlan The object representing the network
	 * @param countDownLightpathResources Whether or not include lightpaths that are down
	 * @param checkRegeneratorsAsWavelengthConverters Whether or not to check that a lighpath occupies one regenerator in every node where the RSA checnges the frequency slots (that is, where a wavelength conversion occurs)
	 * @param optionalLayerParameter WDM network layer. If not present, the default layer is assumed
	 */
	public static void checkConsistency(NetPlan netPlan, boolean countDownLightpathResources , boolean checkRegeneratorsAsWavelengthConverters , NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = netPlan.checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		
		for (Route lpRoute : netPlan.getRoutes(layer))
			if (!lpRoute.isDown())
			{
				final RSA rsa = new RSA (lpRoute , false); // makes the check
				if (checkRegeneratorsAsWavelengthConverters) rsa.checkFrequencySlotConversionOccupiesARegenerator();
			}
		for (ProtectionSegment protectionLp : netPlan.getProtectionSegments(layer))
			if (!protectionLp.isDown())
			{
				final RSA rsa = new RSA (protectionLp); // makes the check
				if (checkRegeneratorsAsWavelengthConverters) rsa.checkFrequencySlotConversionOccupiesARegenerator();
			}
		getNetworkSlotAndRegeneratorOcupancy(netPlan , countDownLightpathResources , layer); // serves as check
	}
	
	/**
	 * Returns the list of nodes within the lightpath route containing a regenerator,
	 * only following a distance criterium, assuming no wavelength conversion is required.
	 * @param seqFibers Sequence of traversed fibers
	 * @param maxRegeneratorDistanceInKm Maximum regeneration distance: after this distance from the initial node or from the last regenerator, the signal is considered not recuperable 
	 * @return A vector with as many elements as traversed links in the route/segment. Each element is a 1 if an optical regenerator is used at the origin node of the corresponding link, and a 0 if not. First element is always 0.
	 */
	public static int[] computeRegeneratorPositions(List<Link> seqFibers, double maxRegeneratorDistanceInKm)
	{
		int numHops = seqFibers.size();

		double accumDistance = 0;
		int[] seqRegenerators = new int[numHops];

		ListIterator<Link> it = seqFibers.listIterator();
		while (it.hasNext())
		{
			int hopId = it.nextIndex();
			Link fiber = it.next();
			double fiberLengthInKm = fiber.getLengthInKm();

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
	 * only following a distance criterium. If the frequency slots occupied changes in a link to the next, we assume that such wavelength conversion needs a regenerator, and thus one regenerator is placed in the intermediate node.
	 * @param seqFibers Sequence of traversed fibers
	 * @param seqFrequencySlots Sequence of frequency slots occupied (one row per slot, one column per traversed fiber)
	 * @param maxRegeneratorDistanceInKm Maximum regeneration distance: after this distance from the initial node or from the last regenerator, the signal is considered not recuperable 
	 * @return A vector with as many elements as traversed links in the route/segment. Each element is a 1 if an optical regenerator is used at the origin node of the corresponding link, and a 0 if not. First element is always 0.
	 */
	public static int[] computeRegeneratorPositions(List<Link> seqFibers, DoubleMatrix2D seqFrequencySlots, double maxRegeneratorDistanceInKm)
	{
		int numHops = seqFibers.size();

		double accumDistance = 0;
		int[] seqRegenerators = new int[numHops];

		ListIterator<Link> it = seqFibers.listIterator();
		while (it.hasNext())
		{
			int hopId = it.nextIndex();
			Link fiber = it.next();
			double fiberLengthInKm = fiber.getLengthInKm();

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
	 * @param optionalLayerParameter WDM network layer. If not present, the default layer is assumed
	 * @return Number of wavelengths per fiber
	 */
	public static DoubleMatrix1D getVectorFiberNumFrequencySlots (NetPlan netPlan, NetworkLayer ... optionalLayerParameter)
	{
		return netPlan.getVectorLinkCapacity(optionalLayerParameter);
	}

	/**
	 * Returns {@code true} if all the RSAs are allocatable (the needed frequency slots are free in the given sequence of links), {@code false} otherwise. 
	 * @param frequencySlot2FiberOccupancy_se Current slot-fiber occupancy 
	 * @param rsas one or more RSAs to check. We start allocating them in order (never releasing the resources of the previous ones). Then, {@code true} is returned if it is possible to allocate all of them simultaneously. In other words, if two RSAs in {@code rsas} require the same frequency slot in the same link, they are not allocatable. 
	 * @return See description above
	 */
	public static boolean isAllocatableRSASet (DoubleMatrix2D frequencySlot2FiberOccupancy_se , RSA ... rsas)
	{
		IntMatrix2D checkMatrix = IntFactory2D.sparse.make (frequencySlot2FiberOccupancy_se.rows () , frequencySlot2FiberOccupancy_se.columns());
		for (RSA rsa : rsas)
		{
			int orderTravLink = 0; 
			for (Link e : rsa.seqLinks)
			{
				for (int s = 0; s < rsa.seqFrequencySlots.rows() ; s ++)
				{
					final int slotIndex = rsa.seqFrequencySlots.get(orderTravLink,s);
					final int linkIndex = e.getIndex();
					if (frequencySlot2FiberOccupancy_se.get (slotIndex , linkIndex) != 0) return false;
					if (checkMatrix.get (slotIndex , linkIndex) != 0) return false;
					checkMatrix.set (slotIndex , linkIndex , 1);
				}
				orderTravLink ++;
			}
		}
		return true;
	}

	/**
	 * Returns the fiber occupied (columns) in each wavelength (rows), and an array with the number of occupied regenerators in each node.
	 * @param netPlan Current design
	 * @param countDownLightpathResources Include lightpaths that are down
	 * @param optionalLayerParameter WDM network layer. If not present, the default layer is assumed
	 * @return Frequency slot - links occupation matrix, and per node regenerator occupation vector
	 */
	public static Pair<DoubleMatrix2D,DoubleMatrix1D> getNetworkSlotAndRegeneratorOcupancy(NetPlan netPlan, boolean countDownLightpathResources , NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = netPlan.checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		final int E = netPlan.getNumberOfLinks (layer);
		final int N = netPlan.getNumberOfNodes ();
		DoubleMatrix1D w_f = getVectorFiberNumFrequencySlots (netPlan, layer);
		final int W = w_f.size () == 0? 0 : (int) w_f.getMaxLocation() [0];
		DoubleMatrix2D frequencySlot2FiberOccupancy_se = DoubleFactory2D.dense.make (W,E);
		DoubleMatrix1D nodeRegeneratorOccupancy = DoubleFactory1D.dense.make (N);
	
		/* The wavelengths above the maximum number of wavelengths of a fiber, are set as occupied */
		for (int e = 0 ; e < E ; e ++) for (int w = (int) w_f.get(e) ; w < W ; w ++) frequencySlot2FiberOccupancy_se.set (e,w,1);
		
		/* Wavlengths occupied by the lightpaths as routes */
		for (Route lpRoute : netPlan.getRoutes(layer))
		{
			if (!countDownLightpathResources && lpRoute.isDown()) continue;
			if (lpRoute.getOccupiedCapacity() == 0) continue; // not been used now
			allocateResources(new RSA (lpRoute , false) , frequencySlot2FiberOccupancy_se , nodeRegeneratorOccupancy);
		}

		/* Wavlengths occupied by the lightpaths as protection segments */
		for (ProtectionSegment segment : netPlan.getProtectionSegments(layer))
		{
			if (!countDownLightpathResources && segment.isDown()) continue;
			if (segment.getReservedCapacityForProtection() == 0) continue; // not been used now
			allocateResources(new RSA (segment) , frequencySlot2FiberOccupancy_se , nodeRegeneratorOccupancy);
//			if (segment.getTraversingRoutes().size() == 1) continue; // its occupancy was already updated
//			if (segment.getTraversingRoutes().size() > 1) throw new RuntimeException ("Bad");
		}			

		return Pair.of(frequencySlot2FiberOccupancy_se,nodeRegeneratorOccupancy);
	}
	
	/**
	 * Updates {@code frequencySlot2FiberOccupancy_se} to consider that a lightpath is releasing
	 * used frequency slots, and {@code nodeRegeneratorOccupancy} to consider that the lightpath releases the occupied regenerators
	 * @param rsa The RSA to release
	 * @param frequencySlot2FiberOccupancy_se Current slot-fiber occupancy (updated inside the method)
	 * @param nodeRegeneratorOccupancy Current node regenerator occupancy (updated inside the method). If {@code null} regenerator information is not updated
	 */
	public static void releaseResources(RSA rsa , DoubleMatrix2D frequencySlot2FiberOccupancy_se, DoubleMatrix1D nodeRegeneratorOccupancy)
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
				final boolean wasOccupied = frequencySlot2FiberOccupancy_se.get(slotId, fiber.getIndex ()) != 0;
				if (!wasOccupied) throw new WDMException("Wavelength " + slotId + " was unused in fiber " + fiber.getId ());
				frequencySlot2FiberOccupancy_se.set(slotId, fiber.getIndex () , 0.0);
			}
			if ((nodeRegeneratorOccupancy != null) && (rsa.seqRegeneratorsOccupancy != null)) 
				if (rsa.seqRegeneratorsOccupancy[hopId] == 1)
				{
					Node node = fiber.getOriginNode();
					nodeRegeneratorOccupancy.set(node.getIndex (), nodeRegeneratorOccupancy.get(node.getIndex()) - 1);
				}
		}
	}
	
	/**
	 * Sets the number of frequency slots available on the given fiber.
	 *
	 * @param fiber Link fiber
	 * @param numFrequencySlots Number of of frequency slots for the given fiber
	 */
	public static void setFiberNumFrequencySlots(Link fiber, int numFrequencySlots)
	{
		if (numFrequencySlots < 0) throw new WDMException("'numWavelengths' must be a non-negative integer");
		fiber.setCapacity(numFrequencySlots);
	}


	/** The full RSA of the lightpath (travsersed fibers, occupied slots and regenerators) is reverted to its initial state.
	 * This means that the current sequence of fibers is changed, and the attributes storing the information of the current RSA are
	 * updated with the information of the initial RSA (also already stored in attributes of the route by previous WDMUtils methods)  
	 * @param r The route of the lightpath to revert
	 */
	public static void revertToOriginalRSA (Route r)
	{
		r.revertToInitialSequenceOfLinks();
		r.setAttribute(SEQUENCE_OF_REGENERATORS_ATTRIBUTE_NAME , r.getAttribute(SEQUENCE_OF_REGENERATORS_INITIAL_ROUTE_ATTRIBUTE_NAME));
		r.setAttribute(SEQUENCE_OF_FREQUENCYSLOTS_ATTRIBUTE_NAME , r.getAttribute(SEQUENCE_OF_FREQUENCYSLOTS_INITIAL_ROUTE_ATTRIBUTE_NAME));
	}
	
	/**
	 * Sets the number of frequency slots available in each fiber to the same value.
	 * 
	 * @param netPlan A {@link com.net2plan.interfaces.networkDesign.NetPlan} representing a WDM physical topology
	 * @param numFrequencySlots Number of wavelengths for all fibers
	 * @param optionalLayerParameter WDM network layer. If not present, the default layer is assumed
	 */
	public static void setFibersNumFrequencySlots(NetPlan netPlan, int numFrequencySlots , NetworkLayer ... optionalLayerParameter)
	{
		NetworkLayer layer = netPlan.checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
		if (numFrequencySlots < 0) throw new WDMException("'numWavelengths' must be a non-negative integer");
		for (Link fiber : netPlan.getLinks (layer)) fiber.setCapacity(numFrequencySlots);
	}

	/**
	 * Sets the attributes of the given {@code Route} object to reflect the RSA occupation (in the initial route or in the current route, depending on {@code initializeTheInitialRoute}). 
	 * @param lp Lightpath (as a route)
	 * @param rsa The RSA to set in the attributes
	 * @param initializeTheInitialRoute If {@code true}, we assume that the RSA corresponds to the initial path in the {@code Route} object, if not, to the current one.
	 */
	public static void setLightpathRSAAttributes (Route lp , RSA rsa , boolean initializeTheInitialRoute)
	{
		lp.setAttribute(initializeTheInitialRoute? SEQUENCE_OF_FREQUENCYSLOTS_INITIAL_ROUTE_ATTRIBUTE_NAME : SEQUENCE_OF_FREQUENCYSLOTS_ATTRIBUTE_NAME, StringUtils.writeMatrix(rsa.seqFrequencySlots));
		lp.setAttribute(initializeTheInitialRoute? SEQUENCE_OF_REGENERATORS_INITIAL_ROUTE_ATTRIBUTE_NAME : SEQUENCE_OF_REGENERATORS_ATTRIBUTE_NAME, IntUtils.join(rsa.seqRegeneratorsOccupancy, " "));
	}

	/**
	 * Sets the attributes of the given {@code ProtectionSegment} object to reflect the RSA occupation 
	 *
	 * @param lp Lightpath 
	 * @param rsa The RSA to store
	 */
	public static void setLightpathRSAAttributes (ProtectionSegment lp , RSA rsa)
	{
		lp.setAttribute(SEQUENCE_OF_FREQUENCYSLOTS_ATTRIBUTE_NAME, StringUtils.writeMatrix(rsa.seqFrequencySlots));
		lp.setAttribute(SEQUENCE_OF_REGENERATORS_ATTRIBUTE_NAME, IntUtils.join(rsa.seqRegeneratorsOccupancy, " "));
	}

	/**
	 * <p>Frequency slot assignment algorithm based on a first-fit fashion. It tries to find a set of contiguous slots that are available 
	 * in all the traversed links, and gets the one which starts in the lowest slot id (the initial slot id of the block is returned)
	 * <p><b>Important</b>: {@code frequencySlot2FiberOccupancy_se} is not updated by this method
	 *
	 * @param seqFibers Sequence of traversed fibers
	 * @param frequencySlot2FiberOccupancy_se Current slot-fiber occupancy 
	 * @param numContiguousSlotsRequired Number of slots of the block (in fixed-grid WDM, this is 1)
	 * @return The id of the initial slot of the contiguous block, or -1 if there is no such block of contigous slots with free resources in all the links
	 */
	public static int spectrumAssignment_firstFit(List<Link> seqFibers, DoubleMatrix2D frequencySlot2FiberOccupancy_se , int numContiguousSlotsRequired)
	{
		GraphUtils.checkRouteContinuity(seqFibers, Constants.CheckRoutingCycleType.NO_REPEAT_LINK);
		final int W = frequencySlot2FiberOccupancy_se.rows();
		for(int initialSlotId = 0; initialSlotId < W - numContiguousSlotsRequired + 1; initialSlotId++)
		{
			boolean freePaths = true;
			for (Link link : seqFibers)
			{
				for (int cont = 0; cont < numContiguousSlotsRequired ; cont ++)
					if (frequencySlot2FiberOccupancy_se.get(initialSlotId+cont,link.getIndex()) != 0) {freePaths = false; break; }
				if (!freePaths) break;
			}
			if (!freePaths) continue;
			return initialSlotId;
		}
		return -1;
	}

	/**
	 * <p>Frequency slot assignment algorithm based on a first-fit fashion for two different paths. 
	 * It tries to find the lowest {@code (s1,s2)} pair, so that a contiguous block of the needed slots, starting in s1, are free in the first path,
	 * and starting in {@code s2} are free in the second path (assuming the occupied slots in the first path are not available now). 
	 * Among all the feasible {@code (s1,s2)} pairs, the returned is the one with lowest {@code s1}, and if more than one, with the lowest {@code s2}. 
	 * If no {@code (s1,s2)} pair exists with the required idle frequency slots, the method returns {@code null} </p>
	 *
	 * <p><b>Important</b>: {@code frequencySlot2FiberOccupancy_se} is not updated by this method
	 *
	 * @param seqFibers_1 First sequence of traversed fibers
	 * @param seqFibers_2 Second sequence of traversed fibers
	 * @param frequencySlot2FiberOccupancy_se Current slot-fiber occupancy 
	 * @param numContiguousSlotsRequired Number of slots of the block (in fixed-grid WDM, this is 1)
	 * @return Pair of sequences of wavelengths traversed by each lightpath
	 */
	public static Pair<Integer,Integer> spectrumAssignment_firstFitTwoRoutes(List<Link> seqFibers_1, List<Link> seqFibers_2 , DoubleMatrix2D frequencySlot2FiberOccupancy_se , int numContiguousSlotsRequired)
	{
		GraphUtils.checkRouteContinuity(seqFibers_1, Constants.CheckRoutingCycleType.NO_REPEAT_LINK);
		GraphUtils.checkRouteContinuity(seqFibers_2, Constants.CheckRoutingCycleType.NO_REPEAT_LINK);
		final int W = frequencySlot2FiberOccupancy_se.rows();
		HashSet<Link> auxSet = new HashSet<Link> (seqFibers_1); auxSet.removeAll(seqFibers_2);
		final boolean haveLinksInCommon = !auxSet.isEmpty();
		for(int initialSlot_1 = 0; initialSlot_1 < W - numContiguousSlotsRequired + 1; initialSlot_1 ++)
		{
			boolean freePath_1 = true;
			for (Link link : seqFibers_1) 
			{
				for (int cont = 0 ; cont < numContiguousSlotsRequired ; cont ++)
					if (frequencySlot2FiberOccupancy_se.get(initialSlot_1 + cont,link.getIndex()) != 0) {freePath_1 = false; break; }
				if (!freePath_1) break;
			}
			if (!freePath_1) continue;
			for(int initialSlot_2 = 0; initialSlot_2 < W - numContiguousSlotsRequired + 1; initialSlot_2 ++)
			{
				if (haveLinksInCommon && (Math.abs(initialSlot_1 - initialSlot_2) < numContiguousSlotsRequired)) continue;
				boolean freePath_2 = true;
				for (Link link : seqFibers_2)
				{ 
					for (int cont = 0 ; cont < numContiguousSlotsRequired ; cont ++)
						if (frequencySlot2FiberOccupancy_se.get(initialSlot_2,link.getIndex()) != 0) { freePath_2 = false ; break; }
					if (!freePath_2) break;
				}
				if (!freePath_2) continue;
				return Pair.of(initialSlot_1, initialSlot_2);
			}			
		}
		return null;
	}

	/**
	 * <p>Wavelength assignment algorithm based on a first-fit fashion assuming
	 * full wavelength conversion and regeneration capabilities. This algorithm is targeted for fixed-frid WDM networks, where all 
	 * lightpaths occupy just one frequency slot (we call it, the lightpath wavelength). 
	 * In the algorithm, each node selects the first
	 * free block for its output fiber, and next nodes in the lightpath try
	 * to maintain it. If not possible, or regeneration is needed, then include
	 * a regenerator (can act also as a full wavelength converter) and search
	 * for the first free wavelength, and so on.</p>
	 *
	 * <p>In case a lightpath cannot be allocated, the corresponding sequence of
	 * wavelengths ({@code seqWavelengths} parameter) will be an empty array.</p>
	 *
	 * @param seqFibers Sequence of traversed fibers
	 * @param frequencySlot2FiberOccupancy_se Current slot-fiber occupancy 
	 * @param nodeRegeneratorOccupancy Number of regenerators installed per node
	 * @param maxRegeneratorDistanceInKm Maximum regeneration distance
	 * @return Sequence of wavelengths traversed by each lightpath, and a 0-1 array indicating whether (1) or not (0) a regenerator/wavelength converter is required at the origin node of the corresponding fiber
	 */
	public static Pair<int[], int[]> wavelengthAssignment_RPP_firstFit(List<Link> seqFibers, DoubleMatrix2D frequencySlot2FiberOccupancy_se, DoubleMatrix1D nodeRegeneratorOccupancy, double maxRegeneratorDistanceInKm)
	{
		final int W = frequencySlot2FiberOccupancy_se.rows ();

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
			double fiberLengthInKm = fiber.getLengthInKm();

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
				if (!plusLink_occupied_w.contains(wavelengthId) && (frequencySlot2FiberOccupancy_se.get(wavelengthId , fiber.getIndex()) == 0))
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
				if (frequencySlot2FiberOccupancy_se.get(wavelengthId , fiber.getIndex()) != 0)
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
	 * Updates {@code frequencySlot2FiberOccupancy_se} and {@code nodeRegeneratorOccupancy} to consider that a new lightpath is occupying 
	 * the resources given by {@code rsa}.
	 * 
	 * @param rsa The rsa
	 * @param frequencySlot2FiberOccupancy_se Current slot-fiber occupancy (updated inside the method)
	 * @param nodeRegeneratorOccupancy Current number of regenerators occupied per node
	 */
	public static void allocateResources(RSA rsa, DoubleMatrix2D frequencySlot2FiberOccupancy_se, DoubleMatrix1D nodeRegeneratorOccupancy)
	{
		final int W = frequencySlot2FiberOccupancy_se.rows();
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
				if (frequencySlot2FiberOccupancy_se.get(slotId , fiber.getIndex ()) != 0) throw new WDMException ("Wavelength clashing: slot " + slotIds.get(cont) + ", fiber: " + fiber.getId ());
				frequencySlot2FiberOccupancy_se.set(slotId , fiber.getIndex () , 1.0);
			}

			if (rsa.seqRegeneratorsOccupancy != null)
				if (rsa.seqRegeneratorsOccupancy[hopId] == 1)
				{
					Node node = fiber.getOriginNode();
					nodeRegeneratorOccupancy.set (node.getIndex (), nodeRegeneratorOccupancy.get(node.getIndex ()) + 1);
				}
		}
	}

	/**
	 * <p>Computes the list of spectral voids (list of available contiguous slots) 
	 * from a slot availability vector (of a fiber or of a path).</p>
	 * 
	 * @param slotOccupancy Set of slots that are already occupied
	 * @param totalAvailableSlotsPerFiber Number of slots per fiber
	 * @return List of spectrum voids, each one with a pair indicating both the initial slot id and the number of consecutive slots within the void. If no spectrum void is found, it returns an empty list
	 */
	public static List<Pair<Integer, Integer>> computeAvailableSpectrumVoids(TreeSet<Integer> slotOccupancy, int totalAvailableSlotsPerFiber)
	{
		List<Pair<Integer, Integer>> out = new LinkedList<Pair<Integer, Integer>>();
		
		int firstAvailableSlot = 0;
		Iterator<Integer> it = slotOccupancy.iterator();
		while(it.hasNext())
		{
			int firstNotAvailableSlot = it.next();
			if (firstAvailableSlot < firstNotAvailableSlot)
			{
				int numSlots_thisVoid = firstNotAvailableSlot - firstAvailableSlot;
				out.add(Pair.of(firstAvailableSlot, numSlots_thisVoid));
			}

			firstAvailableSlot = firstNotAvailableSlot + 1;
		}
		
		if (firstAvailableSlot < totalAvailableSlotsPerFiber)
			out.add(Pair.of(firstAvailableSlot, totalAvailableSlotsPerFiber - firstAvailableSlot));
		
		return out;
	}
	
	/**
	 * Computes the maximum number of requests (each one of the same given number of frequency slots) which 
	 * can be simultaneously allocated in the given set of spectrum voids.
	 * 
	 * @param availableSpectrumVoids List of available spectrum voids (first item of each pair is the initial slot identifier, the second one is the number of consecutive slots)
	 * @param numSlots Number of required slots of all the connections
	 * @return Maximum number of requests of the given number of slots which can be allocated in the given set of spectrum voids
	 */
	public static int computeMaximumRequests(List<Pair<Integer, Integer>> availableSpectrumVoids, int numSlots)
	{
		int numRequests = 0;

		for (Pair<Integer, Integer> spectrumVoid : availableSpectrumVoids)
		{
			int numSlotsThisVoid = spectrumVoid.getSecond();
			if (numSlotsThisVoid < numSlots) continue;

			numRequests += (int) Math.floor((double) numSlotsThisVoid / numSlots);
		}

		return numRequests;
	}

	/**
	 * Returns the modulation format among the ones given, (i) with the maximum spectral efficiency, (ii) but with enough optical reach for the given path length.
	 * 
	 * @param pathLengthInKm Path length (in kilometers): the minimum reach of the returned modulation format
	 * @param availableModulationFormats Set of candidate modulation formats
	 * @return Best modulation format for the given path length. An exception is raised if no moculation format is applicable
	 */
	public static ModulationFormat computeModulationFormat(double pathLengthInKm, Set<ModulationFormat> availableModulationFormats)
	{
		if (availableModulationFormats == null) throw new Net2PlanException("Available modulation formats cannot be null");

		ModulationFormat candidateModulationFormat = null;
		for (ModulationFormat modulationFormat : availableModulationFormats)
		{
			if (pathLengthInKm > modulationFormat.opticalReachInKm) continue;

			if (candidateModulationFormat == null || modulationFormat.spectralEfficiencyInBpsPerHz > candidateModulationFormat.spectralEfficiencyInBpsPerHz)
				candidateModulationFormat = modulationFormat;
		}

		if (candidateModulationFormat == null) throw new Net2PlanException("No modulation format is applicable");

		return candidateModulationFormat;
	}

	/** The same as {@code computeModulationFormat(lengthKm, availableModulationFormats)}, where {@code lengthKm} is the length in km of the input path. 
	 * @param seqFibers Sequence of fibers of the path
	 * @param availableModulationFormats Set of candidate modulation formats
	 * @return Best modulation format for the given path length. An exception is raised if no moculation format is applicable
	 */
	public static ModulationFormat computeModulationFormat(List<Link> seqFibers, Set<ModulationFormat> availableModulationFormats)
	{
		double pathLengthInKm = 0; for (Link e : seqFibers) pathLengthInKm += e.getLengthInKm(); 
		return computeModulationFormat(pathLengthInKm, availableModulationFormats);
	}

	/**
	 * Returns the modulation format with the maximum spectral efficiency, while 
	 * the optical reach constraint is fulfilled, for each path in the list of candidate paths ({@code cpl})
	 * @param cpl The list of paths (the key of the map is the pair of end nodes, the value is the list of the paths between these nodes)
	 * @param availableModulationFormats Set of candidate modulation formats
	 * @return Modulation format per path
	 */
	public static Map<List<Link>, ModulationFormat> computeModulationFormatPerPath(
			Map<Pair<Node, Node>, List<List<Link>>> cpl, 
			Set<ModulationFormat> availableModulationFormats) {
		Map<List<Link>, ModulationFormat> out = new LinkedHashMap<List<Link>, ModulationFormat>();
		for (Collection<List<Link>> paths : cpl.values())
			for (List<Link> seqFibers : paths) 
				out.put(seqFibers, computeModulationFormat(seqFibers, availableModulationFormats));
		return out;
	}

	/**
	 * Computes the number of frequency slots required for a lightpath that needs a given amount of Gbps, using a certain modulation (defining the Gbps/GHz spectral efficiency), 
	 * and that requires a given minimum guard band in GHz (this guard band is assumed to be the sum of the two guard bands at upper and lower side of the band) 
	 * 
	 * @param bandwidthInGbps Requested bandwidth (in Gbps)
	 * @param slotGranularityInGHz Slot granularity (in GHz) 
	 * @param guardBandInGHz Guard-band size (in GHz) (summing the one with upper and lower wavelengths)
	 * @param modulationFormat Modulation format
	 * @return Number of slots required to allocate the bandwidth demand
	 */
	public static int computeNumberOfSlots(double bandwidthInGbps, double slotGranularityInGHz, double guardBandInGHz, ModulationFormat modulationFormat)
	{
		if (bandwidthInGbps < 0) throw new Net2PlanException("'bandwidthInGbps' must be greater or equal than zero");
		if (slotGranularityInGHz <= 0) throw new Net2PlanException("'slotGranularityInGHz' must be greater than zero");
		if (guardBandInGHz < 0) throw new Net2PlanException("'guardBandInGHz' must be greater or equal than zero");

		double requestedBandwidthInGHz = bandwidthInGbps / modulationFormat.spectralEfficiencyInBpsPerHz;
		double requiredBandwidthInGHz = requestedBandwidthInGHz + guardBandInGHz;
		int numSlots = (int) Math.ceil(requiredBandwidthInGHz / slotGranularityInGHz);

		return numSlots;
	}

	/**
	 * <p>Computes the slot availability vector of a path, represented by a sequence 
	 * of fibers, where each position indicates whether or not its corresponding 
	 * frequency slot is available along the path.</p>
	 * 
	 * <p><b>Important</b>: Loop-free paths should be employed, but it is not 
	 * checked by the method.</p>
	 * 
	 * @param slotOccupancyMap Indicates per each fiber its slot occupancy, where already-occupied slots appear
	 * @param seqFibers (Loop-free) Sequence of traversed fibers (unchecked for conitinuity or cycles)
	 * @param totalAvailableSlotsPerFiber Number of slots per fiber
	 * @return Slot occupancy (vector with one coordinate per slot, 1 if occupied, 0 if not)
	 */
	public static TreeSet<Integer> computePathSlotOccupancy(List<Link> seqFibers, DoubleMatrix2D frequencySlot2FiberOccupancy_se)
	{
		final int S = frequencySlot2FiberOccupancy_se.rows();
		TreeSet<Integer> out = new TreeSet<Integer>();
		for (int s = 0; s < S ; s ++)
		{
			boolean free = true;
			for (Link fiber : seqFibers) if (frequencySlot2FiberOccupancy_se.get (s , fiber.getIndex ()) == 1) { free = false; break;  }
			if (!free) out.add (s);
		}
		return out;
	}

	/**
	 * Class to define typical modulation formats. Data for default formats were obtained 
	 * from [1].
	 * 
	 * @since 0.2.3
	 * @see <a href="http://ieeexplore.ieee.org/xpl/articleDetails.jsp?arnumber=6353490">[1] Z. Zhu, W. Lu, L. Zhang, and N. Ansari, "Dynamic Service Provisioning in Elastic Optical Networks with Hybrid Single-/Multi-Path Routing," <i>IEEE/OSA Journal of Lightwave Technology</i>, vol. 31, no. 1, pp. 15-22, January 2013</a>
	 */
	public static class ModulationFormat
	{
		/**
		 * BPSK format (optical reach = 9600 km, spectral efficiency = 1 bps/Hz).
		 * 
		 * @since 0.2.3
		 */
		public final static ModulationFormat BPSK = new ModulationFormat ("BPSK", 9600.0, 1.0);

		/**
		 * QPSK format (optical reach = 4800 km, spectral efficiency = 2 bps/Hz).
		 * 
		 * @since 0.2.3
		 */
		public final static ModulationFormat QPSK = new ModulationFormat ("QPSK", 4800.0, 2.0);

		/**
		 * 8-QAM format (optical reach = 2400 km, spectral efficiency = 3 bps/Hz).
		 * 
		 * @since 0.2.3
		 */
		public final static ModulationFormat QAM_8 = new ModulationFormat ("8-QAM", 2400.0, 3.0);

		/**
		 * 16-QAM format (optical reach = 1200 km, spectral efficiency = 4 bps/Hz).
		 * 
		 * @since 0.2.3
		 */

		public final static ModulationFormat QAM_16 = new ModulationFormat ("16-QAM", 1200.0, 4.0);
		
		/**
		 * Default set of available modulations (BPSK, QPSK, 8-QAM, 16-QAM).
		 * 
		 * @since 0.2.3
		 */
		public final static Set<ModulationFormat> DEFAULT_MODULATION_SET = CollectionUtils.setOf(BPSK, QPSK, QAM_8, QAM_16);
		
		/**
		 * Modulation name.
		 * 
		 * @since 0.2.3
		 */
		public final String name;
		
		/**
		 * Optical reach (in kilometers).
		 * 
		 * @since 0.2.3
		 */
		public final double opticalReachInKm;
		
		/**
		 * Spectral efficiency (in bps per Hz).
		 * 
		 * @since 0.2.3
		 */
		public final double spectralEfficiencyInBpsPerHz;

		/**
		 * Default constructor.
		 * 
		 * @param name Modulation name
		 * @param opticalReachInKm Optical reach (in kilometers)
		 * @param spectralEfficiencyInBpsPerHz Spectral efficiency (in bps per Hz)
		 * @since 0.2.3
		 */
		public ModulationFormat(String name, double opticalReachInKm, double spectralEfficiencyInBpsPerHz)
		{
			if (name == null || name.isEmpty()) throw new Net2PlanException("Modulation name cannot be null");
			if (opticalReachInKm <= 0) throw new Net2PlanException("Optical reach must be greater than zero");
			if (spectralEfficiencyInBpsPerHz <= 0) throw new Net2PlanException("Spectral efficiency must be greater than zero");

			this.name = name;
			this.opticalReachInKm = opticalReachInKm;
			this.spectralEfficiencyInBpsPerHz = spectralEfficiencyInBpsPerHz;
		}

//		public ModulationFormat() {
//			this.name = null;
//			this.opticalReachInKm = 0;
//			this.spectralEfficiencyInBpsPerHz = 0;
//		}
//		
//		public void setModulationFormat(String name, double opticalReachInKm, double spectralEfficiencyInBpsPerHz) {
//			if (name == null || name.isEmpty()) throw new Net2PlanException("Modulation name cannot be null");
//			if (opticalReachInKm <= 0) throw new Net2PlanException("Optical reach must be greater than zero");
//			if (spectralEfficiencyInBpsPerHz <= 0) throw new Net2PlanException("Spectral efficiency must be greater than zero");
//
//			this.name = name;
//			this.opticalReachInKm = opticalReachInKm;
//			this.spectralEfficiencyInBpsPerHz = spectralEfficiencyInBpsPerHz;
//		}
//		
//
//		/**
//		 * Factory method.
//		 * 
//		 * @param name Modulation name
//		 * @param opticalReachInKm Optical reach (in kilometers)
//		 * @param spectralEfficiencyInBpsPerHz Spectral efficiency (in bps per Hz)
//		 * @return New modulation format with the given parameters
//		 * @since 0.2.3
//		 */
//		public static ModulationFormat of(String name, double opticalReachInKm, double spectralEfficiencyInBpsPerHz)
//		{
//			return new ModulationFormat(name, opticalReachInKm, spectralEfficiencyInBpsPerHz);
//		}

		@Override
		public String toString()
		{
			return name + ": optical reach = " + opticalReachInKm + " km, spectral efficiency = " + spectralEfficiencyInBpsPerHz + " bps/Hz";
		}
	}

}
