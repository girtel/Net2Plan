
/* Problems: Islands are not linear (seldom they are). Many degree > 2 AMENs Some AMEN islands are huge (41 nodes) */

/*******************************************************************************
 * This program and the accompanying materials are made available under the terms of the MIT License available at
 * https://opensource.org/licenses/MIT
 *******************************************************************************/



// Sacar numeros para: fliterless sí/no, Topologia dense y small: fijar: CON tolerancia a fallos, trafico 2025, 
// Ver % cost y % consumo, como se reparte entre: 
// 1) Optica linea, optica transponder, IP, IT


/*
 * 
 */

package com.net2plan.examples.niw.algorithms.mhTechnoEconomicsAlgorithm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.net2plan.examples.niw.algorithms.mhTechnoEconomicsAlgorithm.TecnoEc2_costModel.LineCards;
import com.net2plan.examples.niw.algorithms.mhTechnoEconomicsAlgorithm.TecnoEc2_costModel.Pluggables;
import com.net2plan.examples.niw.algorithms.mhTechnoEconomicsAlgorithm.TecnoEc2_costModel.RouterChassis;
import com.net2plan.interfaces.networkDesign.Configuration;
import com.net2plan.interfaces.networkDesign.IAlgorithm;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.niw.OadmArchitecture_generic;
import com.net2plan.niw.OadmArchitecture_generic.Parameters;
import com.net2plan.niw.OpticalAmplifierInfo;
import com.net2plan.niw.WFiber;
import com.net2plan.niw.WLightpath;
import com.net2plan.niw.WNet;
import com.net2plan.niw.WNode;
import com.net2plan.niw.WServiceChain;
import com.net2plan.niw.WVnfInstance;
import com.net2plan.utils.InputParameter;
import com.net2plan.utils.Triple;



public class BillOfMaterialsOptical_v1 implements IAlgorithm
{
	private InputParameter trafficAcceptableByAServerGbps = new InputParameter("trafficAcceptableByAServerGbps", 40.0, "", 0, true, Double.MAX_VALUE, true);
	private InputParameter maximumUtilizationOfAccessPorts = new InputParameter("maximumUtilizationOfAccessPorts", 0.5, "", 0, true, 1.0, true);

	
	private enum LAYERTYPE { OPTICAL , IT, IP};
    private enum OPTICAL_IT_IP_ELEMENTS
    {
    	ITSERVER (LAYERTYPE.IT , 25000 , 1000),
    	TRANSPONDERBIDI_25G (LAYERTYPE.OPTICAL , 0.2 , 0),
    	TRANSPONDERBIDI_100G (LAYERTYPE.OPTICAL , 0.2 , 0),
    	SPLITTER1X2 (LAYERTYPE.OPTICAL , 0.2 , 0),
    	SPLITTER1X4 (LAYERTYPE.OPTICAL , 0.2 , 0),
    	SPLITTER1X8 (LAYERTYPE.OPTICAL , 0.2 , 0),
    	SPLITTER1XMORETHAN8 (LAYERTYPE.OPTICAL , 0.2 , 0),
    	EDFADOUBLEBOOSTERANDPREAMPL (LAYERTYPE.OPTICAL , 0.2 , 0),
    	WAVELENGTHBLOCKER (LAYERTYPE.OPTICAL , 0.2 , 0),
    	WSS_FIXED_1X4 (LAYERTYPE.OPTICAL , 0.2 , 0),
    	WSS_FIXED_1X9 (LAYERTYPE.OPTICAL , 0.2 , 0),
    	WSS_FIXED_1X20 (LAYERTYPE.OPTICAL , 0.2 , 0),
    	WSS_FLEXI_1X4 (LAYERTYPE.OPTICAL , 0.2 , 0),
    	WSS_FLEXI_1X9 (LAYERTYPE.OPTICAL , 0.2 , 0),
    	WSS_FLEXI_1X20 (LAYERTYPE.OPTICAL , 0.2 , 0),
    	MUXDEMUX80CHANNELS (LAYERTYPE.OPTICAL , 0.2 , 0);
    	private final double cost, consumption_W;
    	private final LAYERTYPE layer;
		private OPTICAL_IT_IP_ELEMENTS(LAYERTYPE layer , double cost, double consumption_W) 
		{
			this.layer = layer;
			this.cost = cost;
			this.consumption_W = consumption_W;
		}
		public double getCost() {
			return cost;
		}
		public double getConsumption_W() {
			return consumption_W;
		}
		public static OPTICAL_IT_IP_ELEMENTS getWssRequired (int numOutputsOrInputs)
		{
			if (numOutputsOrInputs <= 4) return WSS_FLEXI_1X4;
			else if (numOutputsOrInputs <= 9) return WSS_FLEXI_1X9;
			else if (numOutputsOrInputs <= 20) return OPTICAL_IT_IP_ELEMENTS.WSS_FLEXI_1X20;
			throw new Net2PlanException ("Too large WSS. Not possible");
		}
		public static OPTICAL_IT_IP_ELEMENTS getSplitterRequired (int numOutputsOrInputs)
		{
			if (numOutputsOrInputs <= 2) return SPLITTER1X2;
			else if (numOutputsOrInputs <= 4) return OPTICAL_IT_IP_ELEMENTS.SPLITTER1X4;
			else if (numOutputsOrInputs <= 8) return OPTICAL_IT_IP_ELEMENTS.SPLITTER1X8;
			return OPTICAL_IT_IP_ELEMENTS.SPLITTER1XMORETHAN8;
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
		
		final Map<WNode , Map<OPTICAL_IT_IP_ELEMENTS,Double>> bomOptical_n = new HashMap<> ();
		final Map<WFiber , Map<OPTICAL_IT_IP_ELEMENTS,Double>> bomOptical_e = new HashMap<> ();
		
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
			if (e.isExistingBoosterAmplifierAtOriginOadm()) increase (bomOptical_e.get(e) , OPTICAL_IT_IP_ELEMENTS.EDFADOUBLEBOOSTERANDPREAMPL , 0.5);
			if (e.isExistingPreamplifierAtDestinationOadm()) increase (bomOptical_e.get(e) , OPTICAL_IT_IP_ELEMENTS.EDFADOUBLEBOOSTERANDPREAMPL , 0.5);
			increase (bomOptical_e.get(e) , OPTICAL_IT_IP_ELEMENTS.EDFADOUBLEBOOSTERANDPREAMPL , (double) e.getOpticalLineAmplifiersInfo().size());
		}
		
		/* Directionless add/drop modules (the same if filterless or not): 1 splitter, 1 combiner, and an amplifier if more than 8 transponders 
		 * All are made with amplifier double and splitter/combiner
		 * 1) All have fixed mux/demux, even if transponders are of different grids */
		for (WNode n : wNet.getNodes())
		{
			final int numTranspondersInTheNode = Math.max(n.getIncomingLigtpaths().size() , n.getOutgoingLigtpaths().size());
			final OPTICAL_IT_IP_ELEMENTS splitterRequired =  OPTICAL_IT_IP_ELEMENTS.getSplitterRequired(numTranspondersInTheNode);
			increase (bomOptical_n.get(n) , splitterRequired , 2.0); // one splitter one combiner
			if (splitterRequired == OPTICAL_IT_IP_ELEMENTS.SPLITTER1XMORETHAN8)
				increase (bomOptical_n.get(n) , OPTICAL_IT_IP_ELEMENTS.EDFADOUBLEBOOSTERANDPREAMPL , 1.0); // extra amplification needed only if large splitter/combiner
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
					increase (bomOptical_n.get(n) , OPTICAL_IT_IP_ELEMENTS.getWssRequired(numOutputsWssOrSplitter) , 1.0); 
				else if (param.isFilterless())
					increase (bomOptical_n.get(n) , OPTICAL_IT_IP_ELEMENTS.getSplitterRequired(numOutputsWssOrSplitter) , 1.0); 
			}
			for (WFiber in : n.getIncomingFibers())
			{
				if (param.isRouteAndSelect())
					increase (bomOptical_n.get(n) , OPTICAL_IT_IP_ELEMENTS.getWssRequired(numOutputsWssOrSplitter) , 1.0); 
				else if (param.isBroadcastAndSelect() || param.isFilterless())
					increase (bomOptical_n.get(n) , OPTICAL_IT_IP_ELEMENTS.getSplitterRequired(numOutputsWssOrSplitter) , 1.0); 
			}
		}
		
		/* Add the transponders */
		for (WLightpath lp : wNet.getLightpaths())
		{
			if (lp.getLightpathRequest().getLineRateGbps() == 25.0)
				increase (bomOptical_n.get(lp.getA()) , OPTICAL_IT_IP_ELEMENTS.TRANSPONDERBIDI_25G , 1.0);
			else if (lp.getLightpathRequest().getLineRateGbps() == 100.0)
				increase (bomOptical_n.get(lp.getA()) , OPTICAL_IT_IP_ELEMENTS.TRANSPONDERBIDI_100G , 1.0);
			else throw new Net2PlanException ("Unkonwn transponder line rate");
		}
			
		
		/* IT BOM */
		if (wNet.getServiceChains().stream().anyMatch(sc->sc.getCurrentExpansionFactorApplied().stream().anyMatch(sf->sf != 1.0))) throw new Net2PlanException ("Some expansion factors are not 1.0");
		for (WNode n : wNet.getNodes())
		{
			double trafficToOrFromTheServersGbps = 0;
			for (WVnfInstance vnf : n.getVnfInstances())
			{
				for (WServiceChain sc : vnf.getTraversingServiceChains())
				{
					final double inTRafficThisSc = vnf.getOccupiedCapacityByTraversingRouteInGbps(sc);
					final double outTRafficThisSc = inTRafficThisSc;
					trafficToOrFromTheServersGbps += inTRafficThisSc;
				}
			}
			increase (bomOptical_n.get(n) , OPTICAL_IT_IP_ELEMENTS.ITSERVER , trafficToOrFromTheServersGbps / trafficAcceptableByAServerGbps.getDouble());
		}
		
		/* IP Scenario 1 */
		if (wNet.getLightpathRequests().stream().anyMatch(lpr->lpr.getLineRateGbps() > 100.0)) throw new Net2PlanException ("Muxponders are needed for transponders of more than 100G");
		for (WNode n : wNet.getNodes())
		{
			/* 10G ports coming from the access: OLTs, mobile, business, residential... */
			final double trafGeneratedInTheNodeGbps = n.getOutgoingServiceChainRequests().stream().mapToDouble(scr->scr.getCurrentOfferedTrafficInGbps()).sum ();
			final double trafFinishedInTheNodeGbps = n.getIncomingServiceChainRequests().stream().mapToDouble(scr->scr.getCurrentOfferedTrafficInGbps()).sum ();
			final double accessTrafficInTheNodesGbps = Math.max(trafGeneratedInTheNodeGbps, trafFinishedInTheNodeGbps);
			int num10GPortsAccessPlus10GLightpats = (int) Math.ceil(accessTrafficInTheNodesGbps / (10.0 * maximumUtilizationOfAccessPorts.getDouble()));
			
			/* 40G ports for the 25G transponders */
			num10GPortsAccessPlus10GLightpats += (int) n.getOutgoingLigtpaths().stream().
					filter(lp->lp.getLightpathRequest().getLineRateGbps() <= 10.0).count ();
			final int num40GPortsTransponders = (int) n.getOutgoingLigtpaths().stream().
					filter(lp->lp.getLightpathRequest().getLineRateGbps() > 10.0 && lp.getLightpathRequest().getLineRateGbps() <= 40.0).count ();
			final int num100GPortsTransponders = (int) n.getOutgoingLigtpaths().stream().
					filter(lp->lp.getLightpathRequest().getLineRateGbps() > 40.0 && lp.getLightpathRequest().getLineRateGbps() <= 100.0).count ();
			
			/* Compute the number of line cards */
			final LineCards lc10G = TecnoEc2_costModel.lineCardsAvailable.stream().filter(lc->lc.getPortRateGbps() == 10.0).sorted((e1,e2)->Double.compare(e1.getPriceDollars() , e2.getPriceDollars())).findFirst().get();
			final LineCards lc40G = TecnoEc2_costModel.lineCardsAvailable.stream().filter(lc->lc.getPortRateGbps() == 40.0).sorted((e1,e2)->Double.compare(e1.getPriceDollars() , e2.getPriceDollars())).findFirst().get();
			final LineCards lc100G = TecnoEc2_costModel.lineCardsAvailable.stream().filter(lc->lc.getPortRateGbps() == 100.0).sorted((e1,e2)->Double.compare(e1.getPriceDollars() , e2.getPriceDollars())).findFirst().get();
			final int numLc10G = (int) Math.ceil(num10GPortsAccessPlus10GLightpats / lc10G.getNumPorts());
			final int numLc40G = (int) Math.ceil(num40GPortsTransponders / lc40G.getNumPorts());
			final int numLc100G = (int) Math.ceil(num100GPortsTransponders / lc100G.getNumPorts());
			final int totalNumLc = numLc10G + numLc40G + numLc100G; 
			/* Choose the best architecture */
			RouterChassis bestRc = null;
			int bestNumChassis = -1;
			double bestCost = Double.MAX_VALUE;
			for (RouterChassis rc : TecnoEc2_costModel.routerChassisAvailable)
			{
				 int numChassis;
				if (totalNumLc <= rc.getNumLineCards())
					numChassis = 1; 
				else if (totalNumLc <= 2 * (rc.getNumLineCards() - 1))
					numChassis = 2;
				else continue;
				final int numExtraLcs = numChassis == 1? 0 : numChassis;
				final double totalCost = numChassis * rc.getPriceChassisRspFanPowerSupplyFabricCardsNotLineCardsDollars() + numExtraLcs * lc100G.getPriceDollars();
				if (totalCost < bestCost) { bestRc = rc; bestNumChassis = numChassis ; bestCost = totalCost; }
			}
			if (bestRc == null) throw new Net2PlanException ("No chassis options fits the requirements");
			
			/* */
			final Pluggables plug10G = TecnoEc2_costModel.pluggablesAvailable.stream().filter(lc->lc.getLineRateGbps() == 10.0).sorted((e1,e2)->Double.compare(e1.getPriceDollars() , e2.getPriceDollars())).findFirst().get();
			final Pluggables plug40G = TecnoEc2_costModel.pluggablesAvailable.stream().filter(lc->lc.getLineRateGbps() == 40.0).sorted((e1,e2)->Double.compare(e1.getPriceDollars() , e2.getPriceDollars())).findFirst().get();
			final Pluggables plug100G = TecnoEc2_costModel.pluggablesAvailable.stream().filter(lc->lc.getLineRateGbps() == 100.0).sorted((e1,e2)->Double.compare(e1.getPriceDollars() , e2.getPriceDollars())).findFirst().get();
			final int numPlugables10G = num10GPortsAccessPlus10GLightpats;
			final int numPluggables40G = num40GPortsTransponders;
			final int numPluggables100G = num100GPortsTransponders + (bestNumChassis == 1? 0 : bestNumChassis);
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
