package com.net2plan.examples.niw.algorithms.mhTechnoEconomicsAlgorithm;

import com.jom.OptimizationProblem;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AuxMainClass
{
    public static void main(String[] args) {

        Map<XrOpticsPostprocessing.OPTICAL_IT_IP_ELEMENTS, Integer> out = calculateBestTransponders(575, false, true);

        out.forEach((k,v) -> System.out.println( k.name() + " : " + v.doubleValue()));

        double cost = calculateTotalCost(out, 1);
        System.out.println("Total cost: " + cost);

    }


    private static Map<XrOpticsPostprocessing.OPTICAL_IT_IP_ELEMENTS, Integer> calculateBestTransponders(double traffic, Boolean isTraffic, Boolean isXROptics){

        Map<XrOpticsPostprocessing.OPTICAL_IT_IP_ELEMENTS, Integer> map = new HashMap<>();
        List<XrOpticsPostprocessing.OPTICAL_IT_IP_ELEMENTS> trasponderList = XrOpticsPostprocessing.OPTICAL_IT_IP_ELEMENTS.getListofTranspodersAvailable(isXROptics);

        int T = trasponderList.size();

        double [] r_t = new double[T];
        double [] c_t = new double[T];
        int t = 0;

        for (XrOpticsPostprocessing.OPTICAL_IT_IP_ELEMENTS tp : trasponderList ){
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

        for (XrOpticsPostprocessing.OPTICAL_IT_IP_ELEMENTS tp : trasponderList){
            if (x_t[t] > 0)
                map.put(tp, (int) x_t[t]);
            t++;
        }

        return map;
    }

    private static double calculateTotalCost(Map<XrOpticsPostprocessing.OPTICAL_IT_IP_ELEMENTS, Integer> map, double alpha){

        double cost = 0;

        for (Map.Entry<XrOpticsPostprocessing.OPTICAL_IT_IP_ELEMENTS, Integer> entry : map.entrySet()){
            if(entry.getKey().getLayerType().equals(XrOpticsPostprocessing.OPTICALTYPE.XR))
                cost += entry.getKey().getCost() * alpha * entry.getValue();
            else
                cost += entry.getKey().getCost() * entry.getValue();
        }

        return cost;
    }

}
