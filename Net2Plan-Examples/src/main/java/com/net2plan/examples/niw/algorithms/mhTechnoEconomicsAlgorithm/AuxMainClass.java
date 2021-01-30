package com.net2plan.examples.niw.algorithms.mhTechnoEconomicsAlgorithm;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AuxMainClass
{



    public static void main(String[] args) {

        Map<XrOpticsPostprocessing.OPTICAL_IT_IP_ELEMENTS, Integer> out = calculateBestTransponders(3525, true, true, false);

        out.forEach((k,v) -> System.out.println( k.name() + " : " + v.doubleValue()));

    }


    private static Map<XrOpticsPostprocessing.OPTICAL_IT_IP_ELEMENTS, Integer> calculateBestTransponders(double traffic, Boolean isTraffic, Boolean isXROptics, Boolean isDebug){

        Map<XrOpticsPostprocessing.OPTICAL_IT_IP_ELEMENTS, Integer> map = new HashMap<>();
        List<XrOpticsPostprocessing.OPTICAL_IT_IP_ELEMENTS> trasponderList = XrOpticsPostprocessing.OPTICAL_IT_IP_ELEMENTS.getListofTranspodersAvailable(isXROptics);

        double remainingTraffic = traffic;

        while (remainingTraffic > 0) {

            if (isTraffic) {
                for (XrOpticsPostprocessing.OPTICAL_IT_IP_ELEMENTS tp : trasponderList) {
                    double lr = tp.getLine_rate_Gbps();
                    double numberOfTpNeeded = Math.ceil(remainingTraffic / lr);

                     if (remainingTraffic == numberOfTpNeeded * lr) {
                        map.put(tp, (int) numberOfTpNeeded);
                        remainingTraffic = remainingTraffic - lr * numberOfTpNeeded;
                        break;
                     }
                    else if (remainingTraffic > lr) {
                        map.put(tp, (int) numberOfTpNeeded-1);
                        remainingTraffic = remainingTraffic - lr * (numberOfTpNeeded-1);
                    }
                    else
                        continue;
                }

            } else {

            }
        }

        return map;

    }

}
