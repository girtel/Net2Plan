package com.net2plan.examples.ocnbook.offline;

import com.net2plan.interfaces.networkDesign.IAlgorithm;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.utils.Triple;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Jorge San Emeterio
 * @date 21-Nov-16
 */
public class Offline_dummy implements IAlgorithm
{
    @Override
    public String executeAlgorithm(NetPlan netPlan, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
    {
        return null;
    }

    @Override
    public String getDescription()
    {
        return "Ejemplo";
    }

    @Override
    public List<Triple<String, String, String>> getParameters()
    {
        List<Triple<String, String, String>> algorithm = new ArrayList<>();

        algorithm.add(Triple.of("Hola", "#file#", "mundo"));

        return algorithm;
    }
}
