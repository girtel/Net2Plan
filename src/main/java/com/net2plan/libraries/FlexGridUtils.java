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

package com.net2plan.libraries;


import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.utils.CollectionUtils;
import com.net2plan.utils.Pair;

import java.util.*;

/**
 * Class implementing some static methods to assist in creating algorithms for 
 * flex-grid networks.
 * 
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 */
public class FlexGridUtils
{
	private FlexGridUtils() { }
	
	/**
	 * <p>Computes the list of spectral voids (list of available contiguous slots) 
	 * from a slot availability vector of a path.</p>
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
	 * Computes the maximum number of requests (each one measured in number of slots) which 
	 * can be allocated in a set of spectrum voids.
	 * 
	 * @param availableSpectrumVoids List of available spectrum voids (first item of each pair is the initial slot identifier, whereas the second one is the number of consecutive slots)
	 * @param numSlots Number of required slots for a reference connection
	 * @return Maximum number of requests which can be allocated in a set of spectrum voids
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
	 * Computes the number of frequency slots required for a certain amount of 
	 * bandwidth (measured in Gbps), including guard-bands.
	 * 
	 * @param bandwidthInGbps Requested bandwidth (in Gbps)
	 * @param slotGranularityInGHz Slot granularity (in GHz) 
	 * @param guardBandInGHz Guard-band size (in GHz)
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
	 * @return Slot occupancy
	 */
	public static TreeSet<Integer> computePathSlotOccupancy(List<Long> seqFibers, Map<Long, TreeSet<Integer>> slotOccupancyMap, int totalAvailableSlotsPerFiber)
	{
		TreeSet<Integer> out = new TreeSet<Integer>();
		for (long fiberId : seqFibers)
			out.addAll(slotOccupancyMap.get(fiberId));

		return out;
	}

	/**
	 * Returns the modulation format with the maximum spectral efficiency, whereas 
	 * the optical reach constraint is fulfilled, for the given path.
	 * 
	 * @param fiberLengthInKmMap Map indicating for each link its length (in kilometers)
	 * @param seqFibers (Loop-free) Sequence of traversed fibers (unchecked for conitinuity or cycles)
	 * @param availableModulationFormats Set of candidate modulation formats
	 * @return Best modulation format for the given path
	 */
	public static ModulationFormat computeModulationFormat(Map<Long, Double> fiberLengthInKmMap, List<Long> seqFibers, Set<ModulationFormat> availableModulationFormats)
	{
		if (availableModulationFormats == null) throw new Net2PlanException("Available modulation formats cannot be null");

		double pathLengthInKm = 0; for(Long e : seqFibers) pathLengthInKm += fiberLengthInKmMap.get(e);
		
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

//	/**
//	 * Returns the modulation format with the maximum spectral efficiency, while 
//	 * the optical reach constraint is fulfilled, for each path in a {@link com.net2plan.libraries.CandidatePathList CandidatePathList} 
//	 * object.
//	 * 
//	 * @param cpl Candidate path list
//	 * @param fiberLengthInKmMap Map indicating for each link its length (in kilometers)
//	 * @param availableModulationFormats Set of candidate modulation formats
//	 * @return Modulation format per path
//	 * @since 0.2.3
//	 */
//	public static Map<Long, ModulationFormat> computeModulationFormatPerPath(CandidatePathList cpl, Map<Long, Double> fiberLengthInKmMap, Set<ModulationFormat> availableModulationFormats)
//	{
//		Map<Long, ModulationFormat> out = new LinkedHashMap<Long, ModulationFormat>();
//		for (long pathId : cpl.getPathIds())
//		{
//			List<Long> seqFibers = cpl.getPathSequenceOfLinks(pathId);
//			out.put(pathId, computeModulationFormat(fiberLengthInKmMap, seqFibers, availableModulationFormats));
//		}
//
//		return out;
//	}

	/**
	 * Class to define modulation formats. Data for default formats were obtained 
	 * from [1].
	 * 
	 * @see <a href="http://ieeexplore.ieee.org/xpl/articleDetails.jsp?arnumber=6353490">[1] Z. Zhu, W. Lu, L. Zhang, and N. Ansari, "Dynamic Service Provisioning in Elastic Optical Networks with Hybrid Single-/Multi-Path Routing," <i>IEEE/OSA Journal of Lightwave Technology</i>, vol. 31, no. 1, pp. 15-22, January 2013</a>
	 */
	public static class ModulationFormat
	{
		/**
		 * BPSK format (optical reach = 9600 km, spectral efficiency = 1 bps/Hz).
		 */
		public final static ModulationFormat BPSK = ModulationFormat.of("BPSK", 9600.0, 1.0);

		/**
		 * QPSK format (optical reach = 4800 km, spectral efficiency = 2 bps/Hz).
		 */
		public final static ModulationFormat QPSK = ModulationFormat.of("QPSK", 4800.0, 2.0);

		/**
		 * 8-QAM format (optical reach = 2400 km, spectral efficiency = 3 bps/Hz).
		 */
		public final static ModulationFormat QAM_8 = ModulationFormat.of("8-QAM", 2400.0, 3.0);

		/**
		 * 16-QAM format (optical reach = 1200 km, spectral efficiency = 4 bps/Hz).
		 */

		public final static ModulationFormat QAM_16 = ModulationFormat.of("16-QAM", 1200.0, 4.0);
		
		/**
		 * Default set of available modulations (BPSK, QPSK, 8-QAM, 16-QAM).
		 */
		public final static Set<ModulationFormat> DefaultModulationSet = CollectionUtils.setOf(BPSK, QPSK, QAM_8, QAM_16);
		
		/**
		 * Modulation name.
		 */
		public final String name;
		
		/**
		 * Optical reach (in kilometers).
		 */
		public final double opticalReachInKm;
		
		/**
		 * Spectral efficiency (in bps per Hz).
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

		/**
		 * Factory method.
		 * 
		 * @param name Modulation name
		 * @param opticalReachInKm Optical reach (in kilometers)
		 * @param spectralEfficiencyInBpsPerHz Spectral efficiency (in bps per Hz)
		 * @return New modulation format with the given parameters
		 */
		public static ModulationFormat of(String name, double opticalReachInKm, double spectralEfficiencyInBpsPerHz)
		{
			return new ModulationFormat(name, opticalReachInKm, spectralEfficiencyInBpsPerHz);
		}

		@Override
		public String toString()
		{
			return name + ": optical reach = " + opticalReachInKm + " km, spectral efficiency = " + spectralEfficiencyInBpsPerHz + " bps/Hz";
		}
	}
}
