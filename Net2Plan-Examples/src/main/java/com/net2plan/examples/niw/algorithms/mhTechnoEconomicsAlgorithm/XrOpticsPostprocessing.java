
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

import com.net2plan.interfaces.networkDesign.Configuration;
import com.net2plan.interfaces.networkDesign.IAlgorithm;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.niw.*;
import com.net2plan.utils.InputParameter;
import com.net2plan.utils.Pair;
import com.net2plan.utils.Triple;

import java.util.*;
import java.util.stream.Collectors;


public class XrOpticsPostprocessing implements IAlgorithm
{
	private InputParameter alpha = new InputParameter("alpha", (double) 1.5, "Description", 0, true, Double.MAX_VALUE, true);
	private InputParameter isXrOpticsType = new InputParameter("opticsType", false, "True if XR, false otherwise");
    private InputParameter isTrafficAware = new InputParameter("isTrafficAware", false, "True if isTrafficAware, false if cost");
    private InputParameter isDebug = new InputParameter("isDebug", true, "True if debug, false otherwise");

	public enum LAYERTYPE { NONXR , XR };
    public enum OPTICAL_IT_IP_ELEMENTS
    {
        // Complete the list with the real values of each element

        // OPTICAL
    	TRANSPONDERBIDI_25G (LAYERTYPE.NONXR , 3.5 , 120, 25),
    	TRANSPONDERBIDI_100G (LAYERTYPE.NONXR , 5 , 160, 100),
        TRANSPONDERBIDI_400G (LAYERTYPE.NONXR , 12 , 160, 400),

        TRANSPONDERBIDIXR_100G (LAYERTYPE.XR , 5 , 160, 100),
        TRANSPONDERBIDIXR_400G (LAYERTYPE.XR , 12 , 160, 400);

    	// Complete the enum list with the components of IT and IP layers

    	private final double cost, consumption_W, line_rate_Gbps;
    	private final LAYERTYPE layer;
		private OPTICAL_IT_IP_ELEMENTS(LAYERTYPE layer, double cost, double consumption_W, double line_rate_Gbps)
		{
			this.layer = layer;
			this.cost = cost;
			this.consumption_W = consumption_W;
			this.line_rate_Gbps = line_rate_Gbps;
		}
		public double getCost() {
			return cost;
		}
		public double getConsumption_W() {
			return consumption_W;
		}
		public double getLine_rate_Gbps() {return line_rate_Gbps; }

		public static List<OPTICAL_IT_IP_ELEMENTS> getListofTranspodersAvailable (Boolean isXROptics){

		    List<OPTICAL_IT_IP_ELEMENTS> transponderList =  new ArrayList<> ();

		    if (isXROptics)
            {
                transponderList.add(TRANSPONDERBIDIXR_400G);
                transponderList.add(TRANSPONDERBIDIXR_100G);
                transponderList.add(TRANSPONDERBIDI_25G);
                transponderList.add(TRANSPONDERBIDI_400G);
                transponderList.add(TRANSPONDERBIDI_100G);
            }else {
                transponderList.add(TRANSPONDERBIDI_400G);
                transponderList.add(TRANSPONDERBIDI_100G);
                transponderList.add(TRANSPONDERBIDI_25G);


            }

		    return transponderList;
        }

    }
    
    
	@Override
	public String executeAlgorithm(NetPlan np, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
	{
		// First of all, initialize all parameters
        Configuration.setOption("precisionFactor", new Double(1e-9).toString());
        Configuration.precisionFactor = 1e-9;
		InputParameter.initializeAllInputParameterFieldsOfObject(this, algorithmParameters);

		final WNet wNet = new WNet(np);
		
		final Map<WNode , Map<OPTICAL_IT_IP_ELEMENTS,Double>> bomOptical_n = new HashMap<> ();
		final Map<WFiber , Map<OPTICAL_IT_IP_ELEMENTS,Double>> bomOptical_e = new HashMap<> ();

		for (WNode n : wNet.getNodes())
		    bomOptical_n.put(n, new HashMap<>());

        for (WFiber e : wNet.getFibers())
            bomOptical_e.put(e, new HashMap<>());

		/* */
		// 1. place preamplifiers and boosters: now simple: always booster and pre-amplifier, only line amplifier if 80 km

		/* Directionless add/drop modules (the same if filterless or not): 1 splitter, 1 combiner, and an amplifier if more than 8 transponders 
		 * All are made with amplifier double and splitter/combiner
		 * 1) All have fixed mux/demux, even if transponders are of different grids */

		/* The OADM degrees, filterless or not */

		/* Add the transponders */
        int numTransponders25G = 0;
        int numTransponders100G = 0;
        int numTransponders400G = 0;
//
//        for (WLightpath lp : wNet.getLightpaths()) lp.remove();
//        for (WLightpathRequest lpr : wNet.getLightpathRequests()) lpr.remove();

        for (WIpLink ipl : wNet.getIpLinks()){
            double ipTraffic = ipl.getCarriedTrafficGbps();
            String origin = ipl.getA().getName();
            String destination = ipl.getB().getName();

//            if (isDebug.getBoolean()) System.out.println("Ip Link: " + origin + " - " + destination + " " + ipTraffic + " Gbps" );

        }

        if (isXrOpticsType.getBoolean()){

        }
        else{
            for (WNode origin : wNet.getNodes())
            {
                Map<WNode,Map<OPTICAL_IT_IP_ELEMENTS, Integer>> tranponderMapPerNode = postProcessingTranponderPerNode(wNet, origin, isTrafficAware.getBoolean(), isXrOpticsType.getBoolean(), isDebug.getBoolean());

                System.out.println("Node: " + origin.getName() + " Total traffic in lightpaths: " + origin.getOutgoingLigtpaths().stream().mapToDouble(value -> value.getLightpathRequest().getLineRateGbps()).sum());
                System.out.println("---------------------------------------");
            }
        }


		for (WLightpath lp : wNet.getLightpaths())
		{
			if (lp.getLightpathRequest().getLineRateGbps() == 25.0) {
                increase(bomOptical_n.get(lp.getA()), OPTICAL_IT_IP_ELEMENTS.TRANSPONDERBIDI_25G, 1.0);
                numTransponders25G++;
            }
			else if (lp.getLightpathRequest().getLineRateGbps() == 100.0){
                increase (bomOptical_n.get(lp.getA()) , OPTICAL_IT_IP_ELEMENTS.TRANSPONDERBIDI_100G , 1.0);
                numTransponders100G++;
            }
			else throw new Net2PlanException ("Unkonwn transponder line rate");
		}

		/* IP Scenario 1 */
        int numTotalPorts25G = 0;
        int numTotalPorts100G = 0;





//        Assert.assertEquals(numTotalPorts25G, numTransponders25G);
//		Assert.assertEquals(numTotalPorts100G,numTransponders100G);

		/************************************************************************************************/
		// Separate per equipment in line = everything but transponders vs transponders
		// Separate cost between AMENs / MCENs / line amplifiers. Energy consumption differences?
		// ...

        // Finally, see the best way to treat the data in order to provide the correct metrics. Three options:
        //  1. Using only one method in this class in case the length code is not so long (maybe this one is more appropiate)
        //  2. Create a new class to export the corresponding metrics

//        System.out.println("------------ Traffic Metrics --------------");
//
//        System.out.println("WDM - Total carried traffic : " +  wNet.getNe().getVectorRouteCarriedTraffic(wNet.getNe().getNetworkLayer(0)).zSum());
//        System.out.println("IP - Total carried traffic : " +  wNet.getNe().getVectorRouteCarriedTraffic(wNet.getNe().getNetworkLayer(1)).zSum());
//
//        final List<WNode> mcenBbs = wNet.getNodes().stream().filter(n-> TimExcel_NodeListSheet_forConfTimData.NODETYPE.isMcenBb(n)).collect(Collectors.toList());
//
//        double mcenBBTraffic = 0;
//
//        for (WNode node : mcenBbs)
//            mcenBBTraffic += node.getNe().getIngressCarriedTraffic();
//
//        System.out.println("MCENBB - Total carried traffic : " + mcenBBTraffic);
//        System.out.println("MCENBB - Average carried traffic per node : "+ mcenBBTraffic/(double)mcenBbs.size() );
//
//        final List<WNode> mcenNoBBs = wNet.getNodes().stream().filter(n-> TimExcel_NodeListSheet_forConfTimData.NODETYPE.isMcenNotBb(n)).collect(Collectors.toList());
//
//        double mcenNoBBTraffic = 0;
//
//        for (WNode node : mcenNoBBs)
//            mcenNoBBTraffic += node.getNe().getIngressCarriedTraffic(wNet.getNe().getNetworkLayer(1));
//
//        System.out.println("MCEN No BB - Total carried traffic : " + mcenNoBBTraffic);
//        System.out.println("MCEN No BB - Average carried traffic per node : "+ mcenNoBBTraffic/(double)mcenNoBBs.size() );
//
//
//        final List<WNode> amens = wNet.getNodes().stream().filter(n-> TimExcel_NodeListSheet_forConfTimData.NODETYPE.isAmen(n)).collect(Collectors.toList());
//
//        double amenTraffic = 0;
//
//        for (WNode node : amens)
//            amenTraffic += node.getNe().getIngressCarriedTraffic(wNet.getNe().getNetworkLayer(1));
//
//        System.out.println("AMEN - Total carried traffic : " + amenTraffic);
//        System.out.println("AMEN - Average carried traffic per node : "+ amenTraffic/(double)amens.size() );
//
//
//
//        showMetrics(wNet.getNodes(),wNet.getFibers(),bomOptical_n,bomOptical_e);
//
//        System.out.println(" ");
//        System.out.println(" ");
//
////        showBOMstatus(wNet.getNodes(),wNet.getFibers(),bomOptical_n,bomOptical_e);
//        showBOMElementByElement(wNet.getNodes(),wNet.getFibers(), bomOptical_n,bomOptical_e);

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
    	if (map.containsKey(key)) map.put(key, map.get(key) + amount);
    	else map.put(key, amount);
    	return map;
    }

    private static Map<WNode,Map<OPTICAL_IT_IP_ELEMENTS, Integer>> postProcessingTranponderPerNode (WNet wNet, WNode origin, Boolean isTraffic, Boolean isXROptics, Boolean isDebug){

        Map<WNode,Map<OPTICAL_IT_IP_ELEMENTS, Integer>> transponderMap = new HashMap<>();

        SortedSet<WLightpath> outgoingLigtpaths = origin.getOutgoingLigtpaths();
        final double totalTraffic = outgoingLigtpaths.stream().mapToDouble(v->v.getLightpathRequest().getLineRateGbps()).sum();

        if (isXROptics)
        {
            Map<OPTICAL_IT_IP_ELEMENTS, Integer> map = calculateBestTransponders(totalTraffic,isTraffic,isXROptics,isDebug);

        }else{
            for (WNode destination : wNet.getNodes())
            {
                List<WLightpath> lpsThisNode = outgoingLigtpaths.stream().filter(lp -> lp.getB().equals(destination)).collect(Collectors.toList());
                double trafficThisDestination = lpsThisNode.stream().mapToDouble(v->v.getLightpathRequest().getLineRateGbps()).sum();
                if (trafficThisDestination > 0 )
                {
                    System.out.println("Destination node " + destination.getName() + " :" + trafficThisDestination );
                    Map<OPTICAL_IT_IP_ELEMENTS, Integer> newTps = calculateBestTransponders(trafficThisDestination, isTraffic, isXROptics, isDebug);
                    transponderMap.put(destination,newTps);
                }
            }
        }





        return transponderMap;

    }

    private static Map<OPTICAL_IT_IP_ELEMENTS, Integer> calculateBestTransponders(double traffic, Boolean isTraffic, Boolean isXROptics, Boolean isDebug){

        Map<OPTICAL_IT_IP_ELEMENTS, Integer> finalMap = new HashMap<>();

        List<OPTICAL_IT_IP_ELEMENTS> trasponderList = OPTICAL_IT_IP_ELEMENTS.getListofTranspodersAvailable(isXROptics);
        double remainingTraffic = traffic;
        double bestTraffic = 0;

        if(isTraffic)
        {
            Map<OPTICAL_IT_IP_ELEMENTS, Integer> auxMap = new HashMap<>();
            while (remainingTraffic > 0){
                 for (OPTICAL_IT_IP_ELEMENTS tp : trasponderList) {
                     double lr = tp.line_rate_Gbps;
                     double numberOfTpNeeded = Math.ceil(traffic/lr);
                     if (lr >= traffic)
                     {
                         bestTraffic = traffic - lr*numberOfTpNeeded;
                         auxMap.put(tp,(int) numberOfTpNeeded);
                         remainingTraffic = traffic - lr*numberOfTpNeeded;
                         break;
                     }
                     else{

                     }
                     auxMap.put(tp,(int) numberOfTpNeeded);
                     remainingTraffic = traffic - lr*numberOfTpNeeded;
                 }
            }
        }else
        {
            while (remainingTraffic > 0) {


            }
        }

        return finalMap;

    }


    private Map<WNode, Pair<Double, Double>> computeNodeMetrics (List<WNode> nodes, Map<WNode , Map<OPTICAL_IT_IP_ELEMENTS,Double>> bomOptical_n, LAYERTYPE layer )
    {
        Map<WNode, Pair<Double, Double>> metrics = new HashMap<>();

        for (WNode n : nodes)
        {
            Map<OPTICAL_IT_IP_ELEMENTS, Double> elementsInthisNode = bomOptical_n.get(n);

            double totalConsumptionInThisNode = 0;
            double totalCostInThisInThisNode = 0;

            Iterator<Map.Entry<OPTICAL_IT_IP_ELEMENTS, Double>> iterator = elementsInthisNode.entrySet().stream().filter(v -> v.getKey().layer.equals(layer)).iterator();

            while (iterator.hasNext()) {

                Map.Entry<OPTICAL_IT_IP_ELEMENTS, Double> entry = iterator.next();

                totalConsumptionInThisNode += entry.getKey().consumption_W * entry.getValue();
                totalCostInThisInThisNode += entry.getKey().cost * entry.getValue();
            }

            metrics.put(n, Pair.of(totalConsumptionInThisNode,totalCostInThisInThisNode));
        }

        return  metrics;
    }


    public void showMetrics(List<WNode> nodes, List<WFiber> fibers, Map<WNode , Map<OPTICAL_IT_IP_ELEMENTS,Double>> bomOptical_n,  Map<WFiber , Map<OPTICAL_IT_IP_ELEMENTS,Double>> bomOptical_e)
    {

        Map<WNode, Pair<Double, Double>> opticalMetrics_transponderSide_nonXR = computeNodeMetrics(nodes,bomOptical_n, LAYERTYPE.NONXR);


        System.out.println("*******  Optical Metrics - TRANSPONDER SIDE *********");

        System.out.println("Optical - Total Consumption (W): " + opticalMetrics_transponderSide_nonXR.entrySet().stream().mapToDouble(entry -> entry.getValue().getFirst()).sum());
        System.out.println("Optical - Total cost: " + opticalMetrics_transponderSide_nonXR.entrySet().stream().mapToDouble(entry -> entry.getValue().getSecond()).sum());

    }

    public void showBOMstatus(List<WNode> nodes, List<WFiber> fibers, Map<WNode,Map<OPTICAL_IT_IP_ELEMENTS,Double>> bomOptical_n,  Map<WFiber , Map<OPTICAL_IT_IP_ELEMENTS,Double>> bomOptical_e){

        System.out.println("----- Print BOM_n -----");

        Map<OPTICAL_IT_IP_ELEMENTS,Double> bomByElement_n = new HashMap();
        Map<OPTICAL_IT_IP_ELEMENTS,Double> bomByElement_e = new HashMap();


        for (WNode node : nodes)
        {
            Set<Map.Entry<OPTICAL_IT_IP_ELEMENTS, Double>> thisNode = bomOptical_n.get(node).entrySet();

            System.out.println(" ");
            System.out.println("* -------------> Node ID: " + node.getNe().getIndex() + " Type: " + node.getType());


            for (Map.Entry<OPTICAL_IT_IP_ELEMENTS,Double> element : thisNode)
            {
                System.out.println("Element name: " + element.getKey().name());
                System.out.println("Number of elements: " + element.getValue());

                if (bomByElement_n.containsKey(element.getKey())) bomByElement_n.put(element.getKey(), bomByElement_n.get(element.getKey())+element.getValue());
                else bomByElement_n.put(element.getKey(), element.getValue());
            }
        }

        for (WFiber fiber : fibers)
        {
            Set<Map.Entry<OPTICAL_IT_IP_ELEMENTS, Double>> thisFiber = bomOptical_e.get(fiber).entrySet();

            System.out.println("- Fiber ID: " + fiber.getNe().getIndex());

            for (Map.Entry<OPTICAL_IT_IP_ELEMENTS,Double> element : thisFiber)
            {
                System.out.println("Element name: " + element.getKey().name());
                System.out.println("Number of elements: " + element.getValue());

                if (bomByElement_e.containsKey(element.getKey())) bomByElement_e.put(element.getKey(), bomByElement_e.get(element.getKey())+element.getValue());
                else bomByElement_e.put(element.getKey(), element.getValue());
            }

        }

    }

    public void showBOMElementByElement(List<WNode> nodes, List<WFiber> fibers, Map<WNode,Map<OPTICAL_IT_IP_ELEMENTS,Double>> bomOptical_n,  Map<WFiber , Map<OPTICAL_IT_IP_ELEMENTS,Double>> bomOptical_e)
    {

        System.out.println("----- Print BOM Element by Element -----");

        Map<OPTICAL_IT_IP_ELEMENTS,Double> bomByElement_n = new HashMap();

        for (WNode node : nodes)
        {
            Set<Map.Entry<OPTICAL_IT_IP_ELEMENTS, Double>> thisNode = bomOptical_n.get(node).entrySet();

            for (Map.Entry<OPTICAL_IT_IP_ELEMENTS,Double> element : thisNode)
            {
                if (bomByElement_n.containsKey(element.getKey())) bomByElement_n.put(element.getKey(), bomByElement_n.get(element.getKey())+element.getValue());
                else bomByElement_n.put(element.getKey(), element.getValue());
            }
        }

        for (WFiber fiber : fibers)
        {
            Set<Map.Entry<OPTICAL_IT_IP_ELEMENTS, Double>> thisFiber = bomOptical_e.get(fiber).entrySet();

            for (Map.Entry<OPTICAL_IT_IP_ELEMENTS,Double> element : thisFiber)
            {
                if (bomByElement_n.containsKey(element.getKey())) bomByElement_n.put(element.getKey(), bomByElement_n.get(element.getKey())+element.getValue());
                else bomByElement_n.put(element.getKey(), element.getValue());
            }
        }

        for (Map.Entry<OPTICAL_IT_IP_ELEMENTS,Double> element : bomByElement_n.entrySet())
            System.out.println(element.getKey().name() + ": " + element.getValue());


    }

}
