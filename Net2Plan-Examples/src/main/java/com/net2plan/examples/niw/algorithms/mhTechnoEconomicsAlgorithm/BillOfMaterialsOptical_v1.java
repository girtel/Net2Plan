
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

        // Complete the list with the real values of each element
    	// IT
        ITSERVER (LAYERTYPE.IT , 5.6 , 2400),
        NASLARGE (LAYERTYPE.IT , 6.6 ,  200),

        // IP
        TELCOSWITCH_NCS_5001(LAYERTYPE.IP , 8.4 , 200),
        TELCOSWITCH_NCS_5002(LAYERTYPE.IP , 12 , 240),
        TELCOSWITCH_NCS_5011(LAYERTYPE.IP , 16 , 240),
        TELCOSWITCH_NCS_5502(LAYERTYPE.IP , 28 , 240),
        TELCOSWITCH_NCS_5508(LAYERTYPE.IP , 54 , 300),
        TELCOSWITCH_NCS_5516(LAYERTYPE.IP , 90 , 300),

        l2SWITCH_NEXUS_93360(LAYERTYPE.IP , 6.5 , 296),
        l2SWITCH_NEXUS_93364(LAYERTYPE.IP , 10 , 429),

        PLUGGABLE_10G_COPPER(LAYERTYPE.IP , 0.01 , 0 ),
        PLUGGABLE_40G_COPPER(LAYERTYPE.IP , 0.02 , 0 ),
        PLUGGABLE_100G_COPPER(LAYERTYPE.IP , 0.04 , 0 ),
        PLUGGABLE_10G_GREY(LAYERTYPE.IP , 0.016 , 0 ),
        PLUGGABLE_40G_GREY(LAYERTYPE.IP , 0.05 , 0 ),
        PLUGGABLE_100G_GREY(LAYERTYPE.IP , 0.1 , 0 ),

        ROUTER_ASR_9906(LAYERTYPE.IP , 7 , 3000 ),
        ROUTER_ASR_9912(LAYERTYPE.IP , 11 , 3000 ),
        ROUTER_ASR_9922(LAYERTYPE.IP , 18.4 , 3000 ),

        LINE_CARD_48x10G(LAYERTYPE.IP , 64 , 810),
        LINE_CARD_8X100G(LAYERTYPE.IP , 98 , 1100),

        // OPTICAL
    	TRANSPONDERBIDI_25G (LAYERTYPE.OPTICAL , 2.4 , 120),
    	TRANSPONDERBIDI_100G (LAYERTYPE.OPTICAL , 5 , 160),
    	SPLITTER1X2 (LAYERTYPE.OPTICAL , 0.004 , 0),
    	SPLITTER1X4 (LAYERTYPE.OPTICAL , 0.01 , 0),
    	SPLITTER1X8 (LAYERTYPE.OPTICAL , 0.02 , 0),
    	SPLITTER1XMORETHAN8 (LAYERTYPE.OPTICAL , 0.4 , 0),
    	EDFADOUBLEBOOSTERANDPREAMPL (LAYERTYPE.OPTICAL , 0.6 , 27),
    	WAVELENGTHBLOCKER (LAYERTYPE.OPTICAL , 0.2 , 7.2),
    	WSS_FIXED_1X4 (LAYERTYPE.OPTICAL , 1.1 , 30),
    	WSS_FIXED_1X9 (LAYERTYPE.OPTICAL , 2.2 , 40),
    	WSS_FIXED_1X20 (LAYERTYPE.OPTICAL , 3 , 75),
    	WSS_FLEXI_1X4 (LAYERTYPE.OPTICAL , 1.1 , 30),
    	WSS_FLEXI_1X9 (LAYERTYPE.OPTICAL , 2.2 , 40),
    	WSS_FLEXI_1X20 (LAYERTYPE.OPTICAL , 3 , 75),
    	MUXDEMUX80CHANNELS (LAYERTYPE.OPTICAL , 0.8 , 0);

    	// Complete the enum list with the components of IT and IP layers

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
//			final LineCards lc40G = TecnoEc2_costModel.lineCardsAvailable.stream().filter(lc->lc.getPortRateGbps() == 40.0).sorted((e1,e2)->Double.compare(e1.getPriceDollars() , e2.getPriceDollars())).findFirst().get();
			final LineCards lc100G = TecnoEc2_costModel.lineCardsAvailable.stream().filter(lc->lc.getPortRateGbps() == 100.0).sorted((e1,e2)->Double.compare(e1.getPriceDollars() , e2.getPriceDollars())).findFirst().get();
			final int numLc10G = (int) Math.ceil(num10GPortsAccessPlus10GLightpats / lc10G.getNumPorts());
//			final int numLc40G = (int) Math.ceil(num40GPortsTransponders / lc40G.getNumPorts());
			final int numLc100G = (int) Math.ceil(num100GPortsTransponders / lc100G.getNumPorts());
//			final int totalNumLc = numLc10G + numLc40G + numLc100G;
            final int totalNumLc = numLc10G + numLc100G;

			// Increase the number of Line cards (10G, 40G and 100G) needed in this node
            increase(bomOptical_n.get(n), OPTICAL_IT_IP_ELEMENTS.LINE_CARD_48x10G, (double) numLc10G);
            increase(bomOptical_n.get(n), OPTICAL_IT_IP_ELEMENTS.LINE_CARD_8X100G, (double) numLc100G);

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

            // Increase the number of chasis needed in this node
            OPTICAL_IT_IP_ELEMENTS routerEum = null;

            switch (bestRc.getName()){
                case "ASR 9906":
                    routerEum = OPTICAL_IT_IP_ELEMENTS.ROUTER_ASR_9906;
                    break;
                case "ASR 9912":
                    routerEum = OPTICAL_IT_IP_ELEMENTS.ROUTER_ASR_9912;
                    break;
                case "ASR 9922":
                    routerEum = OPTICAL_IT_IP_ELEMENTS.ROUTER_ASR_9922;
                    break;
            }

            increase(bomOptical_n.get(n) , routerEum , (double) bestNumChassis);

            /* Calculate the number of pluggable devices in this node */
			final Pluggables plug10G = TecnoEc2_costModel.pluggablesAvailable.stream().filter(lc->lc.getLineRateGbps() == 10.0).sorted((e1,e2)->Double.compare(e1.getPriceDollars() , e2.getPriceDollars())).findFirst().get();
			final Pluggables plug40G = TecnoEc2_costModel.pluggablesAvailable.stream().filter(lc->lc.getLineRateGbps() == 40.0).sorted((e1,e2)->Double.compare(e1.getPriceDollars() , e2.getPriceDollars())).findFirst().get();
			final Pluggables plug100G = TecnoEc2_costModel.pluggablesAvailable.stream().filter(lc->lc.getLineRateGbps() == 100.0).sorted((e1,e2)->Double.compare(e1.getPriceDollars() , e2.getPriceDollars())).findFirst().get();
			final int numPlugables10G = num10GPortsAccessPlus10GLightpats;
			final int numPluggables40G = num40GPortsTransponders;
			final int numPluggables100G = num100GPortsTransponders + (bestNumChassis == 1? 0 : bestNumChassis);

			// Increase the number of pluggables (10G, 40G, 100G) in this node

            OPTICAL_IT_IP_ELEMENTS plug10G_enum = null;
            OPTICAL_IT_IP_ELEMENTS plug40G_enum = null;
            OPTICAL_IT_IP_ELEMENTS plug100G_enum = null;

            if (plug10G.getName().equalsIgnoreCase("10G SFP+ copper"))
                plug10G_enum = OPTICAL_IT_IP_ELEMENTS.PLUGGABLE_10G_COPPER;
            else if(plug10G.getName().equalsIgnoreCase("10G SFP+ optical"))
                plug10G_enum = OPTICAL_IT_IP_ELEMENTS.PLUGGABLE_10G_GREY;
            else throw new Net2PlanException("Wrong pluggable");

            increase(bomOptical_n.get(n), plug10G_enum, (double) numPlugables10G);

            if (plug40G.getName().equalsIgnoreCase("40G QSFP copper"))
                plug40G_enum = OPTICAL_IT_IP_ELEMENTS.PLUGGABLE_10G_COPPER;
            else if(plug40G.getName().equalsIgnoreCase("40G QSFP optical"))
                plug40G_enum = OPTICAL_IT_IP_ELEMENTS.PLUGGABLE_10G_GREY;
            else throw new Net2PlanException("Wrong pluggable");

            increase(bomOptical_n.get(n), plug40G_enum, (double) numPluggables40G);

            if (plug100G.getName().equalsIgnoreCase("100G QSFP copper"))
                plug100G_enum = OPTICAL_IT_IP_ELEMENTS.PLUGGABLE_10G_COPPER;
            else if(plug100G.getName().equalsIgnoreCase("100G QSFP optica"))
                plug100G_enum = OPTICAL_IT_IP_ELEMENTS.PLUGGABLE_10G_GREY;
            else throw new Net2PlanException("Wrong pluggable");

            increase(bomOptical_n.get(n), plug100G_enum, (double) numPluggables100G);

		}


		/************************************************************************************************/
		// Separate per equipment in line = everything but transponders vs transponders
		// Separate cost between AMENs / MCENs / line amplifiers. Energy consumption differences?
		// ...

        // Finally, see the best way to treat the data in order to provide the correct metrics. Three options:
        //  1. Using only one method in this class in case the length code is not so long (maybe this one is more appropiate)
        //  2. Create a new class to export the corresponding metrics

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

    private Map<WNode, Triple<Double, Double, Double>> computeMetrics (List<WNode> nodes, List<WFiber> fibers, Map<WNode , Map<OPTICAL_IT_IP_ELEMENTS,Double>> bomOptical_n, Map<WFiber , Map<OPTICAL_IT_IP_ELEMENTS,Double>> bomOptical_e ,LAYERTYPE layer )
    {
        Map<WNode, Triple<Double, Double, Double>> itMetrics = new HashMap<>();
        Map<WNode, Triple<Double, Double, Double>> ipMetrics = new HashMap<>();
        Map<WNode, Triple<Double, Double, Double>> opticalMetrics = new HashMap<>();

        for (WNode n : nodes)
        {
            double trafficInThisNode = 0;

            Map<OPTICAL_IT_IP_ELEMENTS, Double> elementsInthisNode = bomOptical_n.get(n);

            double totalConsumptionInThisNode = 0;
            double totalCostInThisInThisNode = 0;

            for (Map.Entry<OPTICAL_IT_IP_ELEMENTS, Double> entry : elementsInthisNode.entrySet()) {

                if(entry.getKey().layer == layer ) {
                    totalConsumptionInThisNode += entry.getKey().consumption_W * entry.getValue();
                    totalCostInThisInThisNode += entry.getKey().cost * entry.getValue();
                }

                if (entry.getKey().layer == LAYERTYPE.IT) trafficInThisNode = n.getVnfInstances().stream().mapToDouble(value -> value.getCurrentCapacityInGbps()).sum();
                else if (entry.getKey().layer == LAYERTYPE.IT) n.getInOutOrTraversingServiceChains().stream().mapToDouble(value -> value.getCurrentCarriedTrafficGbps()).sum();
                else if (entry.getKey().layer == LAYERTYPE.OPTICAL) {
                    n.getIncomingLigtpaths().stream().mapToDouble(value -> value.getLightpathRequest().getLineRateGbps()).sum();
                }
                else throw new Net2PlanException("Element is not attached to any layer");
            }

            itMetrics.put(n, Triple.of(trafficInThisNode,totalConsumptionInThisNode,totalCostInThisInThisNode));
        }

//        if (layer == LAYERTYPE.OPTICAL)
//        {
//            for (WFiber fiber : fibers)
//            {
//                for (Map.Entry<OPTICAL_IT_IP_ELEMENTS,Double> entry : bomOptical_e.get(fiber).entrySet())
//                {
//                    entry.getKey()
//                }
//            }
//        }

        return  itMetrics;
    }

    public void showMetrics(List<WNode> nodes, List<WFiber> fibers, Map<WNode , Map<OPTICAL_IT_IP_ELEMENTS,Double>> bomOptical_n,  Map<WFiber , Map<OPTICAL_IT_IP_ELEMENTS,Double>> bomOptical_e)
    {
        Map<WNode, Triple<Double, Double, Double>> itMetrics = computeMetrics(nodes,fibers,bomOptical_n,bomOptical_e,LAYERTYPE.IT);
        Map<WNode, Triple<Double, Double, Double>> ipMetrics = computeMetrics(nodes,fibers,bomOptical_n,bomOptical_e,LAYERTYPE.IT);
        Map<WNode, Triple<Double, Double, Double>> opticalMetrics = computeMetrics(nodes,fibers,bomOptical_n,bomOptical_e,LAYERTYPE.IT);

        System.out.println("*******  IT Metrics *********");

        double it_totalTraffic = itMetrics.entrySet().stream().mapToDouble(entry -> entry.getValue().getFirst()).sum();
        double it_totalConsumption = itMetrics.entrySet().stream().mapToDouble(entry -> entry.getValue().getSecond()).sum();
        double it_totalCost = itMetrics.entrySet().stream().mapToDouble(entry -> entry.getValue().getThird()).sum();

        System.out.println("IT - Total VNF Traffic: " + it_totalTraffic);
        System.out.println("IT - Total Consumption (W): " + it_totalConsumption);
        System.out.println("IT - Total cost: " + it_totalCost);

        System.out.println("*******  IP Metrics *********");

        double ip_totalTraffic = ipMetrics.entrySet().stream().mapToDouble(entry -> entry.getValue().getFirst()).sum();
        double ip_totalConsumption = ipMetrics.entrySet().stream().mapToDouble(entry -> entry.getValue().getSecond()).sum();
        double ip_totalCost = ipMetrics.entrySet().stream().mapToDouble(entry -> entry.getValue().getThird()).sum();

        System.out.println("IP - Total VNF Traffic: " + ip_totalTraffic);
        System.out.println("IP - Total Consumption (W): " + ip_totalConsumption);
        System.out.println("IP - Total cost: " + ip_totalCost);

        System.out.println("*******  Optical Metrics *********");

        double optical_totalTraffic = opticalMetrics.entrySet().stream().mapToDouble(entry -> entry.getValue().getFirst()).sum();
        double optical_totalConsumption = opticalMetrics.entrySet().stream().mapToDouble(entry -> entry.getValue().getSecond()).sum();
        double optical_totalCost = opticalMetrics.entrySet().stream().mapToDouble(entry -> entry.getValue().getThird()).sum();

        System.out.println("Optical - Total VNF Traffic: " + optical_totalTraffic);
        System.out.println("Optical - Total Consumption (W): " + optical_totalConsumption);
        System.out.println("Optical - Total cost: " + optical_totalCost);

    }



}
