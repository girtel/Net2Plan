package com.net2plan.examples.general.offline

import com.net2plan.interfaces.networkDesign.IAlgorithm
import com.net2plan.interfaces.networkDesign.NetPlan
import com.net2plan.utils.Triple

/**
 * Created by Jorge San Emeterio on 03/12/2016.
 */
class Test_Algorithm implements IAlgorithm
{

    @Override
    String executeAlgorithm(NetPlan netPlan, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
    {
        netPlan.getNodes().each {node -> print(node.getName())}
        return "OK!"
    }

    @Override
    String getDescription()
    {
        return "Groovy test algorithm..."
    }

    @Override
    List<Triple<String, String, String>> getParameters()
    {
        return new ArrayList<Triple<String, String, String>>()
    }
}