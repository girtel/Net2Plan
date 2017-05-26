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



package com.net2plan.libraries;

import cern.colt.matrix.tdouble.DoubleFactory2D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.utils.Triple;

import java.util.*;

/**
 * Class to deal with auxiliary functions to handle wireless networks.
 *
 */
public class WirelessUtils
{

	/**
	 * Given a wireless network where a node cannot simultanously receive signals from two different nodes (or a collision would be produced and no signal is correctly received), this function computes the cases in which a node transmission interferes with a link reception, 
	 * and returns this information in several structures.
	 * 
	 * A link <i>e</i> is assumed to be interfered by a node <i>n</i> (different to the initial node of <i>e</i>), when the reception node of <i>e</i> (node <i>b(e)</i>) is reachable by <i>n</i> (that is, there is a link from <i>n</i> to <i>b(e)</i>). 
	 * Also, in those wireless networks for which a node cannot transmit and receive simultaneously, a link <i>e</i> is also interfered by node <i>b(e)</i>, since if <i>b(e)</i> transmits link <i>e</i> cannot receive.
	 * 	 
	 * The interference information is returned in three equivalent structures (they reflect the same information in different forms):
	 * <ul>
	 * <li>A {@code DoubleMatrix2D} object (a 2D matrix), with as many rows as nodes and as many columns as links. Position (n,e) equals to 1 is the transmission 
	 * in node n would impede any reception in link e. </li>
	 * <li>A map {@code Map<Link,Set<Node>>} which associates to each link, the set of nodes that interfere to it</li>
	 * <li>A map {@code Map<Node,Set<Link>>} which associates to each node, the set of links that the node interferes to</li>
	 * </ul>
	 * 
	 * @param nodes List of nodes of the network
	 * @param links List of wireless links
	 * @param simultaneousTxAndRxPossible Wheter or not simultaneous transmission and reception is possible in the network nodes
	 * @return the interference information in three equivalent structures
	 */
	public static Triple<DoubleMatrix2D, Map<Link,Set<Node>> , Map<Node,Set<Link>>> computeInterferenceMap (List<Node> nodes , List<Link> links , boolean simultaneousTxAndRxPossible)
	{
		final int N = nodes.size ();
		final int E = links.size ();
		DoubleMatrix2D nodeInterferesToLink_ne = DoubleFactory2D.sparse.make (N,E);
		Map<Node,Set<Link>> linksInterfFrom_n = new HashMap<Node,Set<Link>> (); for (Node n : nodes) linksInterfFrom_n.put (n , new HashSet<Link> ());
		Map<Link,Set<Node>> nodesInterfTo_e = new HashMap<Link,Set<Node>> (); for (Link e : links) nodesInterfTo_e.put (e , new HashSet<Node> ());
		for (Link interferedLink : links)
		{
			final Node a_e = interferedLink.getOriginNode();
			final Node b_e = interferedLink.getDestinationNode();
			for (Link interferingLink : b_e.getIncomingLinks())
			{ 
				final Node interferingNode = interferingLink.getOriginNode();
				if (interferingNode == a_e) continue;
				if (simultaneousTxAndRxPossible &&  (interferingNode == b_e)) continue;
				nodeInterferesToLink_ne.set (interferingNode.getIndex() , interferedLink.getIndex() , 1.0);
				linksInterfFrom_n.get (interferingNode).add (interferedLink);
				nodesInterfTo_e.get(interferedLink).add (interferingNode);
			}
			if (!simultaneousTxAndRxPossible) 
			{
				nodeInterferesToLink_ne.set (b_e.getIndex() , interferedLink.getIndex () , 1.0);
				linksInterfFrom_n.get (b_e).add (interferedLink);
				nodesInterfTo_e.get(interferedLink).add (b_e);
			}
		}
		return Triple.of (nodeInterferesToLink_ne , nodesInterfTo_e , linksInterfFrom_n);
	}

	/**
	 * Given a wireless network where a node cannot simultanously receive signals from two different nodes (or a collision would be produced and no signal is correctly received), 
	 * and cannot transmit and receive at the same time, this function computes the scheduling matrix, with one row per link, and one column for each VALID schedule: 
	 * in a valid schedule, all the links active (with a 1 in the associated row) could simultaneously transmit with no collision.
	 * 
	 * A collision occurs in a link <i>e</i> from node <i>a(e)</i> to node <i>b(e)</i>, when this link is active, and at the same time any link <i>e'</i> coming out 
	 * of a node <i>n</i> which can reach to <i>b(e)</i> (has a link to it) is active. Also, those schedules where a node transmits in two links simultanaously, 
	 * or a node must receive from two links simultaneouly, or a node has to transmit and receive simultaneously, are forbiden.
	 * @param links List of wireless links
	 * @return the scheduling matrix
	 */
	public static DoubleMatrix2D computeSchedulingMatrix (List<Link> links)
	{
		final int E = links.size (); if (E > 30) throw new RuntimeException ("Too many links");
		final int numMaxSched = (int) Math.pow(2, E);
		DoubleMatrix2D A_em = DoubleFactory2D.sparse.make(E, numMaxSched);
		int numValidSchedules = 0;
		
		for (int sched = 0 ; sched < numMaxSched ; sched ++)
		{
			boolean validSchedule = true;
			Set<Link> activeLinksSN = new HashSet<Link> ();
			int schedShifted = sched;
			for (int e = 0; e < E ; e ++)
			{
				final Link link = links.get(e);
				if ((schedShifted & 1) != 0)
				{
					/* link active, see if it conflicts */
					final Node a_e = link.getOriginNode();
					final Node b_e = link.getDestinationNode();
					for (Link previousE : activeLinksSN)
					{
						final Node prev_ae = previousE.getOriginNode();
						final Node prev_be = previousE.getDestinationNode();
						if (a_e == prev_ae) { validSchedule = false; break; } // 2 simult tx
						if (b_e == prev_be) { validSchedule = false; break; } // 2 simult rx
						if (b_e == prev_ae) { validSchedule = false; break; } // tx and rx simult
						if (prev_be == a_e) { validSchedule = false; break; } // tx and rx simult
					}
					if (!validSchedule) break;
					activeLinksSN.add(link);
				}
				schedShifted = schedShifted >> 1;
			}
			
			if (validSchedule)
			{
				for (Link e : activeLinksSN) A_em.set(e.getIndex() , numValidSchedules , 1.0);
				numValidSchedules ++;
			}
		}
		DoubleMatrix2D Aem_view = A_em.viewPart(0,0,E,numValidSchedules);
		return Aem_view;
	}

	/**
	 * Computes the Signal to Noise Ratio for the given wireless link.
	 * @param e link index
	 * @param transmissionPowerLogUnits_e The vector of transmission power of each link in logarithmic units
	 * @param Gnu_ee the interference map of the network: fraction of power in link e1 that reaches the end node of link e2 (and if e1 is not e2, becomes an interference to it)
	 * @param receptionThermalNoise_nu the thermal noise at the receptor node of link e
	 * @return the signal-to-noisse-and-interference ratio
	 */
	public static double computeSINRLink (int e , DoubleMatrix1D transmissionPowerLogUnits_e , DoubleMatrix2D Gnu_ee , double receptionThermalNoise_nu)
	{
		final int E = Gnu_ee.rows ();
		final double receivedPower_nu = Math.exp(transmissionPowerLogUnits_e.get(e)) * Gnu_ee.get(e , e);
		double interferencePower_nu = receptionThermalNoise_nu;
		for (int eInt = 0; eInt < E ; eInt ++)
			if (eInt != e) interferencePower_nu += Math.exp(transmissionPowerLogUnits_e.get(eInt)) * Gnu_ee.get(eInt,e);
		return receivedPower_nu / interferencePower_nu;
	}

	/* A matrix e1e2, with the gain in natural units of the signal from origin of e1 to destination of e2. Depends on the signal attenuation
	 *  (path loss and distance), and any additional attenuation by which receiver oe e2 can filter out the signal from links different to e2 */

	/**
	 * Computes the interference matrix in a network subject to soft interferences: a matrix with as many rows and columns as links, with the gain in natural units of the signal from origin of <i>e1</i> to destination of <i>e2</i>. 
	 * 
	 * The power transmitted from the origin of <i>e1</i> in its travel to the end node of <i>e2</i> suffers an attenuation given by <i>d^alpha</i>, where <i>d</i> is the distance traversed 
	 * by the signal and alpha is the path loss exponent characterizing the propagation model. In addition, when link <i>e1</i> is different to <i>e2</i>, then 
	 * the received power is actually an interference. In this case, we assume that the receptor of <i>e2</i> is able to further attenuate by a 
	 * factor {@code interferenceAttenuationFactor_nu} the received power.  
	 *  
	 * @param links List of links
	 * @param interferenceAttenuationFactor_nu Interference attenuation factor
	 * @param pathLossExponent Path loss exponent
	 * @return The interference matrix
	 */
	public static DoubleMatrix2D computeInterferenceMatrixNaturalUnits (List<Link> links , double interferenceAttenuationFactor_nu , double pathLossExponent)
	{
		if (links.isEmpty()) throw new Net2PlanException ("The network is empty");
		final int E = links.size ();
		final NetPlan netPlan = links.get(0).getNetPlan();
		/* Initialize the gains between links */
		DoubleMatrix2D Gnu_ee = DoubleFactory2D.dense.make (E,E);
		for (Link e : links)
			for (Link ep : links)
			{
				final Node a_e = e.getOriginNode();
				final Node b_ep = ep.getDestinationNode();
				final double distance_km = netPlan.getNodePairEuclideanDistance(a_e, b_ep);
				double attenuation_nu = ((e == ep)? 1 : interferenceAttenuationFactor_nu);
				if (a_e != b_ep) attenuation_nu *= Math.pow (distance_km , pathLossExponent); 
				if (attenuation_nu == 0) throw new Net2PlanException ("An interference map coordinate has infinite gain");
				Gnu_ee.set(e.getIndex() ,ep.getIndex (), 1 / attenuation_nu);
			}
		return Gnu_ee;
	}

	/* The maximum possible interference power in natural units a link can receive, if all the rest of the links transmit at max power */

	/**
	 * Returns the maximum possible interference power in natural (linear) units a link can receive, if the rest of the links transmit at its maximum power at the same time.
	 * @param e the link
	 * @param maxTransmissionPower_logu maximum transmission power of each other link in the network, in logarithmic units
	 * @param Gnu_ee The interference map for the network
	 * @return Maximum interference power in natural inuts
	 */
	public static double computeLinkReceivedInterferenceAtMaxPower_nu (int e , double maxTransmissionPower_logu  , DoubleMatrix2D Gnu_ee)
	{
		final int E = Gnu_ee.rows ();
		double interfPower_nu = 0;
		for (int eInt = 0; eInt < E ; eInt ++)
			if (e != eInt) interfPower_nu += Math.exp(maxTransmissionPower_logu) * Gnu_ee.get(eInt,e);
		return interfPower_nu;
	}

	/** Compute the highest received interference power that a node in the network receives, when all the links are simultaneously transmitting at its maximum power
	 * @param maxTransmissionPower_logu maximum transmission power of each link in the network, in logarithmic units
	 * @param Gnu_ee The interference map for the network
	 * @return the worst case (highest) interference power
	 */
	public static double computeWorseReceiverInterferencePower_nu (double maxTransmissionPower_logu  , DoubleMatrix2D Gnu_ee)
	{
		final int E = Gnu_ee.rows ();
		double worseInterferenceReceivedAtMaxPower_nu = 0;
		for (int e = 0; e < E ; e ++)
			worseInterferenceReceivedAtMaxPower_nu = Math.max(worseInterferenceReceivedAtMaxPower_nu , computeLinkReceivedInterferenceAtMaxPower_nu (e , maxTransmissionPower_logu  , Gnu_ee));
		return worseInterferenceReceivedAtMaxPower_nu;
	}
}
