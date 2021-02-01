
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

import com.jom.OptimizationProblem;
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
	private InputParameter alpha = new InputParameter("alpha", (double) 1.5, "Alpha");
	private InputParameter isXrOpticsType = new InputParameter("isXRopticsType", false, "True if XR, false otherwise");
    private InputParameter isTrafficAware = new InputParameter("isTrafficAware", false, "True if isTrafficAware, false if cost");
    private InputParameter isDebug = new InputParameter("isDebug", true, "True if debug, false otherwise");

	public enum OPTICALTYPE { NONXR , XR };
    public enum OPTICAL_IT_IP_ELEMENTS
    {
        // Complete the list with the real values of each element

        // OPTICAL
    	TRANSPONDERBIDI_25G (OPTICALTYPE.NONXR , 3.5 , 120, 25),
    	TRANSPONDERBIDI_100G (OPTICALTYPE.NONXR , 5 , 160, 100),
        TRANSPONDERBIDI_400G (OPTICALTYPE.NONXR , 12 , 160, 400),

        TRANSPONDERBIDIXR_100G (OPTICALTYPE.XR , 5 , 160, 100),
        TRANSPONDERBIDIXR_400G (OPTICALTYPE.XR , 12 , 160, 400);

    	// Complete the enum list with the components of IT and IP layers

    	private final double cost, consumption_W, line_rate_Gbps;
    	private final OPTICALTYPE layer;
		private OPTICAL_IT_IP_ELEMENTS(OPTICALTYPE layer, double cost, double consumption_W, double line_rate_Gbps)
		{
			this.layer = layer;
			this.cost = cost;
			this.consumption_W = consumption_W;
			this.line_rate_Gbps = line_rate_Gbps;
		}
		public double getCost() { return cost;	}
		public double getConsumption_W() {
			return consumption_W;
		}
		public double getLine_rate_Gbps() {return line_rate_Gbps; }
		public OPTICALTYPE getLayerType() {return layer;}

		public static List<OPTICAL_IT_IP_ELEMENTS> getListofTranspodersAvailable (Boolean isXROptics){

		    List<OPTICAL_IT_IP_ELEMENTS> transponderList =  new ArrayList<> ();

		    if (isXROptics)
            {
                transponderList.add(TRANSPONDERBIDIXR_400G);
                transponderList.add(TRANSPONDERBIDIXR_100G);
                transponderList.add(TRANSPONDERBIDI_25G);
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


        System.out.println("Alpha: " + alpha.getDouble());
        System.out.println("IsXROptics: " + isXrOpticsType.getBoolean());
        System.out.println("IsTraffic: " + isTrafficAware.getBoolean());

		final WNet wNet = new WNet(np);
		
		final Map<WNode , Map<OPTICAL_IT_IP_ELEMENTS,Double>> bomOptical_n = new HashMap<> ();

		for (WNode n : wNet.getNodes())
		    bomOptical_n.put(n, new HashMap<>());

        for (WNode origin : wNet.getNodes())
        {
            Map<OPTICAL_IT_IP_ELEMENTS, Double> tranponderMapPerNode = postProcessingTranponderPerNode(wNet, origin, isTrafficAware.getBoolean(), isXrOpticsType.getBoolean(), alpha.getDouble());
            bomOptical_n.put(origin,tranponderMapPerNode);
        }

//        showMetrics(wNet.getNodes(),bomOptical_n);
//        showBOMstatus(wNet.getNodes(),bomOptical_n);
        showBOMElementByElement(wNet.getNodes(),bomOptical_n, alpha.getDouble());

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

    private static Map<OPTICAL_IT_IP_ELEMENTS, Double> postProcessingTranponderPerNode (WNet wNet, WNode origin, Boolean isTraffic, Boolean isXROptics, double alpha){

        Map<OPTICAL_IT_IP_ELEMENTS, Double> transponderMap = new HashMap<>();

        SortedSet<WLightpath> outgoingLigtpaths = origin.getOutgoingLigtpaths();
        final double totalTraffic = outgoingLigtpaths.stream().mapToDouble(v->v.getLightpathRequest().getLineRateGbps()).sum();

        if (isXROptics){

            Map<OPTICAL_IT_IP_ELEMENTS, Double> xRmap = calculateBestTransponders(totalTraffic,isTraffic,isXROptics);
            double costXR = calculateTotalCost(xRmap, alpha);

            Map<OPTICAL_IT_IP_ELEMENTS, Double> noXRmap = new HashMap<>();

            for (WNode destination : wNet.getNodes())
            {
                List<WLightpath> lpsThisNode = outgoingLigtpaths.stream().filter(lp -> lp.getB().equals(destination)).collect(Collectors.toList());
                double trafficThisDestination = lpsThisNode.stream().mapToDouble(v->v.getLightpathRequest().getLineRateGbps()).sum();
                if (trafficThisDestination > 0 )
                {
                    Map<OPTICAL_IT_IP_ELEMENTS, Double> newTps = calculateBestTransponders(trafficThisDestination, isTraffic, !isXROptics);

                    for (Map.Entry<OPTICAL_IT_IP_ELEMENTS, Double> entry : newTps.entrySet()){
                        if (noXRmap.containsKey(entry.getKey())) noXRmap.put(entry.getKey(),entry.getValue()+noXRmap.get(entry.getKey()));
                        else noXRmap.put(entry.getKey(),entry.getValue());
                    }
                }
            }
            double costNoXR = calculateTotalCost(noXRmap,0);

            if (costXR < costNoXR)
                transponderMap = xRmap;
            else
                transponderMap = noXRmap;

        }else{

            Map<OPTICAL_IT_IP_ELEMENTS, Double> noXRmap = new HashMap<>();

            for (WNode destination : wNet.getNodes())
            {
                List<WLightpath> lpsThisNode = outgoingLigtpaths.stream().filter(lp -> lp.getB().equals(destination)).collect(Collectors.toList());
                double trafficThisDestination = lpsThisNode.stream().mapToDouble(v->v.getLightpathRequest().getLineRateGbps()).sum();
                if (trafficThisDestination > 0 )
                {
                    Map<OPTICAL_IT_IP_ELEMENTS, Double> newTps = calculateBestTransponders(trafficThisDestination, isTraffic, isXROptics);

                    for (Map.Entry<OPTICAL_IT_IP_ELEMENTS, Double> entry : newTps.entrySet()){
                        if (noXRmap.containsKey(entry.getKey())) noXRmap.put(entry.getKey(),entry.getValue()+noXRmap.get(entry.getKey()));
                        else noXRmap.put(entry.getKey(),entry.getValue());
                    }
                }
            }

            transponderMap = noXRmap;
        }

        return transponderMap;
    }

    private static Map<OPTICAL_IT_IP_ELEMENTS, Double> calculateBestTransponders(double traffic, Boolean isTraffic, Boolean isXROptics){

        Map<OPTICAL_IT_IP_ELEMENTS, Double> map = new HashMap<>();
        List<OPTICAL_IT_IP_ELEMENTS> trasponderList = OPTICAL_IT_IP_ELEMENTS.getListofTranspodersAvailable(isXROptics);

        int T = trasponderList.size();

        double [] r_t = new double[T];
        double [] c_t = new double[T];
        int t = 0;

        for (OPTICAL_IT_IP_ELEMENTS tp : trasponderList ){
            r_t[t] = tp.getLine_rate_Gbps();
            c_t[t] = tp.getCost();
            t++;
        }

        double remainingTraffic = traffic;

        OptimizationProblem op = new OptimizationProblem();

        op.addDecisionVariable("x_t", true, new int [] {1,T} , 0 , Double.MAX_VALUE);

        op.setInputParameter("r_t", r_t, "row");
        op.setInputParameter("c_t", c_t, "row");
        op.setInputParameter("h_d", traffic);

        if (isTraffic) {
            op.setObjectiveFunction("minimize", "x_t * c_t'");
            op.addConstraint("x_t * r_t' == h_d");

        } else {
            op.setObjectiveFunction("minimize", "x_t * c_t'");
            op.addConstraint("x_t * r_t' >= h_d");
        }

        op.solve("cplex");

        double [] x_t = op.getPrimalSolution("x_t").to1DArray();

        t = 0;

        for (OPTICAL_IT_IP_ELEMENTS tp : trasponderList){
            if (x_t[t] > 0)
                map.put(tp, x_t[t]);
            t++;
        }

        return map;
    }

    public static double calculateTotalCost(Map<OPTICAL_IT_IP_ELEMENTS, Double> map, double alpha){

        double cost = 0;

        for (Map.Entry<OPTICAL_IT_IP_ELEMENTS, Double> entry : map.entrySet()){
            if(entry.getKey().getLayerType().equals(OPTICALTYPE.XR))
                cost += entry.getKey().getCost() * alpha * entry.getValue();
            else
                cost += entry.getKey().getCost() * entry.getValue();
        }

        return cost;
    }


    private Map<WNode, Pair<Double, Double>> computeNodeMetrics (List<WNode> nodes, Map<WNode , Map<OPTICAL_IT_IP_ELEMENTS,Double>> bomOptical_n, OPTICALTYPE layer )
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


    public void showMetrics(List<WNode> nodes, Map<WNode , Map<OPTICAL_IT_IP_ELEMENTS,Double>> bomOptical_n)
    {

        Map<WNode, Pair<Double, Double>> opticalMetrics_transponderSide_nonXR = computeNodeMetrics(nodes,bomOptical_n, OPTICALTYPE.NONXR);


        System.out.println("*******  Optical Metrics - TRANSPONDER SIDE *********");

        System.out.println("Optical - Total Consumption (W): " + opticalMetrics_transponderSide_nonXR.entrySet().stream().mapToDouble(entry -> entry.getValue().getFirst()).sum());
        System.out.println("Optical - Total cost: " + opticalMetrics_transponderSide_nonXR.entrySet().stream().mapToDouble(entry -> entry.getValue().getSecond()).sum());

    }

    public void showBOMstatus(List<WNode> nodes, Map<WNode,Map<OPTICAL_IT_IP_ELEMENTS,Double>> bomOptical_n){

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

    }

    public void showBOMElementByElement(List<WNode> nodes, Map<WNode,Map<OPTICAL_IT_IP_ELEMENTS,Double>> bomOptical_n, double alpha)
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

        double totalCost = 0;

        for (Map.Entry<OPTICAL_IT_IP_ELEMENTS,Double> element : bomByElement_n.entrySet()) {
            System.out.println(element.getKey().name() + ": " + element.getValue());
            if (element.getKey().getLayerType().equals(OPTICALTYPE.XR))
                totalCost += element.getKey().getCost() * alpha * element.getValue();
            else
                totalCost += element.getKey().getCost() * element.getValue();
        }

        System.out.println("Total cost: " + totalCost);


    }

}
