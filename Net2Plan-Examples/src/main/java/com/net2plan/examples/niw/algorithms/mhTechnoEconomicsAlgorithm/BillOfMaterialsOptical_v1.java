/* Problems: Islands are not linear (seldom they are). Many degree > 2 AMENs Some AMEN islands are huge (41 nodes) */

/*******************************************************************************
 * This program and the accompanying materials are made available under the terms of the MIT License available at
 * https://opensource.org/licenses/MIT
 *******************************************************************************/


package com.net2plan.examples.niw.algorithms.mhTechnoEconomicsAlgorithm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.net2plan.examples.niw.algorithms.mhTechnoEconomicsAlgorithm.TimExcel_NodeListSheet_forConfTimData.NODETYPE;
import com.net2plan.interfaces.networkDesign.Configuration;
import com.net2plan.interfaces.networkDesign.IAlgorithm;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.niw.OadmArchitecture_generic;
import com.net2plan.niw.OadmArchitecture_generic.Parameters;
import com.net2plan.niw.OpticalAmplifierInfo;
import com.net2plan.niw.OpticalSpectrumManager;
import com.net2plan.niw.WFiber;
import com.net2plan.niw.WLightpath;
import com.net2plan.niw.WNet;
import com.net2plan.niw.WNode;
import com.net2plan.utils.InputParameter;
import com.net2plan.utils.Pair;
import com.net2plan.utils.Triple;



public class BillOfMaterialsOptical_v1 implements IAlgorithm
{
    private enum OPTICALELEMENTS
    {
    	TRANSPONDERBIDI_25G (0.2 , 0),
    	TRANSPONDERBIDI_100G (0.2 , 0),
    	SPLITTER1X2 (0.2 , 0),
    	SPLITTER1X4 (0.2 , 0),
    	SPLITTER1X8 (0.2 , 0),
    	SPLITTER1XMORETHAN8 (0.2 , 0),
    	EDFADOUBLEBOOSTERANDPREAMPL (0.2 , 0),
    	WAVELENGTHBLOCKER (0.2 , 0),
    	WSS_FIXED_1X4 (0.2 , 0),
    	WSS_FIXED_1X9 (0.2 , 0),
    	WSS_FIXED_1X20 (0.2 , 0),
    	WSS_FLEXI_1X4 (0.2 , 0),
    	WSS_FLEXI_1X9 (0.2 , 0),
    	WSS_FLEXI_1X20 (0.2 , 0),
    	MUXDEMUX80CHANNELS (0.2 , 0);
    	private final double cost, consumption_W;
		private OPTICALELEMENTS(double cost, double consumption_W) 
		{
			this.cost = cost;
			this.consumption_W = consumption_W;
		}
		public double getCost() {
			return cost;
		}
		public double getConsumption_W() {
			return consumption_W;
		}
		public static OPTICALELEMENTS getWssRequired (int numOutputsOrInputs)
		{
			if (numOutputsOrInputs <= 4) return WSS_FLEXI_1X4;
			else if (numOutputsOrInputs <= 9) return WSS_FLEXI_1X9;
			else if (numOutputsOrInputs <= 20) return OPTICALELEMENTS.WSS_FLEXI_1X20;
			throw new Net2PlanException ("Too large WSS. Not possible");
		}
		public static OPTICALELEMENTS getSplitterRequired (int numOutputsOrInputs)
		{
			if (numOutputsOrInputs <= 2) return SPLITTER1X2;
			else if (numOutputsOrInputs <= 4) return OPTICALELEMENTS.SPLITTER1X4;
			else if (numOutputsOrInputs <= 8) return OPTICALELEMENTS.SPLITTER1X8;
			return OPTICALELEMENTS.SPLITTER1XMORETHAN8;
		}
    }
    
    
	@Override
	public String executeAlgorithm(NetPlan np, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
	{
		// First of all, initialize all parameters
		InputParameter.initializeAllInputParameterFieldsOfObject(this, algorithmParameters);

		final WNet wNet = new WNet(np);
		Configuration.setOption("precisionFactor", new Double(1e-9).toString());
		Configuration.precisionFactor = 1e-9;
		
		final Map<WNode , Map<OPTICALELEMENTS,Double>> bomOptical_n = new HashMap<> ();
		final Map<WFiber , Map<OPTICALELEMENTS,Double>> bomOptical_e = new HashMap<> ();
		
		/* */
		// 1. place preamplifiers and boosters: now simple: always booster and pre-amplifier, only line amplifier if 80 km
		for (WFiber e : wNet.getFibers())
		{
			e.setIsExistingBoosterAmplifierAtOriginOadm(true);
			e.setIsExistingPreamplifierAtDestinationOadm(true);
    		final double maxDistanceKm = 80.0;
			e.removeOpticalLineAmplifiers();
			final int numOlas = Math.floor(e.getLengthInKm() / maxDistanceKm) == e.getLengthInKm() / maxDistanceKm? (int) (e.getLengthInKm() / maxDistanceKm) - 1 : (int) Math.floor(e.getLengthInKm() / maxDistanceKm);
			final double interOlaDistanceKm = e.getLengthInKm() / (numOlas + 1);
    		final List<OpticalAmplifierInfo> olasInfo = new ArrayList<> ();
			for (int cont = 0; cont < numOlas ; cont ++) olasInfo.add(OpticalAmplifierInfo.getDefaultOla((cont+1) * interOlaDistanceKm));
			e.setOlaTraversedInfo(olasInfo);
		}

		/*****************************************************************************************************/
		
		/* AMPLIFIERS: All nodes have preampl & booster, and one line amplifier for 80 km */
		for (WFiber e : wNet.getFibers())
		{
			if (e.isExistingBoosterAmplifierAtOriginOadm()) increase (bomOptical_e.get(e) , OPTICALELEMENTS.EDFADOUBLEBOOSTERANDPREAMPL , 0.5);
			if (e.isExistingPreamplifierAtDestinationOadm()) increase (bomOptical_e.get(e) , OPTICALELEMENTS.EDFADOUBLEBOOSTERANDPREAMPL , 0.5);
			increase (bomOptical_e.get(e) , OPTICALELEMENTS.EDFADOUBLEBOOSTERANDPREAMPL , (double) e.getOpticalLineAmplifiersInfo().size());
		}
		
		/* Directionless add/drop modules (the same if filterless or not): 1 splitter, 1 combiner, and an amplifier if more than 8 transponders 
		 * All are made with amplifier double and splitter/combiner
		 * 1) All have fixed mux/demux, even if transponders are of different grids */
		for (WNode n : wNet.getNodes())
		{
			final int numTranspondersInTheNode = Math.max(n.getIncomingLigtpaths().size() , n.getOutgoingLigtpaths().size());
			final OPTICALELEMENTS splitterRequired =  OPTICALELEMENTS.getSplitterRequired(numTranspondersInTheNode);
			increase (bomOptical_n.get(n) , splitterRequired , 2.0); // one splitter one combiner
			if (splitterRequired == OPTICALELEMENTS.SPLITTER1XMORETHAN8)
				increase (bomOptical_n.get(n) , OPTICALELEMENTS.EDFADOUBLEBOOSTERANDPREAMPL , 1.0); // extra amplification needed only if large splitter/combiner
		}
		
		/* The OADM degrees, filterless or not */
		for (WNode n : wNet.getNodes())
		{
			final OadmArchitecture_generic oadm = n.getOpticalSwitchingArchitecture().getAsGenericArchitecture();
			final Parameters param = oadm.getParameters();
			final int numOutputsForEachDegree = n.getOutgoingFibers().size();
			final int numOutputsForDirectiolessModules = n.getOadmNumAddDropDirectionlessModules();
			final int numOutputsForDirectionfulModule = n.isOadmWithDirectedAddDropModulesInTheDegrees()? 1 : 0;
			final int numOutputsWssOrSplitter = numOutputsForEachDegree+ numOutputsForDirectiolessModules + numOutputsForDirectionfulModule; // the one is for the directionless modlue
			for (WFiber out : n.getOutgoingFibers())
			{
				if (param.isBroadcastAndSelect() || param.isRouteAndSelect())
					increase (bomOptical_n.get(n) , OPTICALELEMENTS.getWssRequired(numOutputsWssOrSplitter) , 1.0); 
				else if (param.isFilterless())
					increase (bomOptical_n.get(n) , OPTICALELEMENTS.getSplitterRequired(numOutputsWssOrSplitter) , 1.0); 
			}
			for (WFiber in : n.getIncomingFibers())
			{
				if (param.isRouteAndSelect())
					increase (bomOptical_n.get(n) , OPTICALELEMENTS.getWssRequired(numOutputsWssOrSplitter) , 1.0); 
				else if (param.isBroadcastAndSelect() || param.isFilterless())
					increase (bomOptical_n.get(n) , OPTICALELEMENTS.getSplitterRequired(numOutputsWssOrSplitter) , 1.0); 
			}
		}
		
		/* Add the transponders */
		for (WLightpath lp : wNet.getLightpaths())
		{
			if (lp.getLightpathRequest().getLineRateGbps() == 25.0)
				increase (bomOptical_n.get(lp.getA()) , OPTICALELEMENTS.TRANSPONDERBIDI_25G , 1.0);
			else if (lp.getLightpathRequest().getLineRateGbps() == 100.0)
				increase (bomOptical_n.get(lp.getA()) , OPTICALELEMENTS.TRANSPONDERBIDI_100G , 1.0);
			else throw new Net2PlanException ("Unkonwn transponder line rate");
		}
			
		
		/************************************************************************************************/
		// Separate per equipment in line = everything but transponders vs transponders
		// Separate cost between AMENs / MCENs / line amplifiers. Energy consumption differences?
		// ...
		
		
		return "Ok";
	}

	@Override
	public String getDescription()
	{
		return "This algorithm creates a BOM";
	}

	@Override
	public List<Triple<String, String, String>> getParameters()
	{
		/* Returns the parameter information for all the InputParameter objects defined in this object (uses Java reflection) */
		return InputParameter.getInformationAllInputParameterFieldsOfObject(this);
	}

    private static <E> Map<E , Double> increase (Map<E,Double> map , E key , Double amount)
    {
    	if (map.containsKey(key)) map.put(key, map.get(key) + amount); else map.put(key, amount);
    	return map;
    }
    
}
