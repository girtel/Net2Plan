package com.net2plan.examples.niw.algorithms;

import com.net2plan.interfaces.networkDesign.IAlgorithm;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.utils.Triple;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/* FlexAlgo simulator */
import com.net2plan.niw.DefaultStatelessSimulator;

public class FlexAlgoTopologyOptimization implements IAlgorithm
{
    private long computeId(Link l, int N) { return l.getOriginNode().getId() + N * l.getDestinationNode().getId(); }

    @Override
    public String executeAlgorithm(NetPlan netPlan, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
    {
        DefaultStatelessSimulator simulator = new DefaultStatelessSimulator();
        NetPlan np = netPlan.copy();

        // Modify flex algos of np



//        simulator.executeAlgorithm();


        return null;
    }

    @Override
    public String getDescription()
    {
        return "Algorithm that finds a set of FlexAlgos that ";
    }

    @Override
    public List<Triple<String, String, String>> getParameters()
    {
        List<Triple<String, String, String>> simulationParameters = new ArrayList<>();

        simulationParameters.add(Triple.of("exec-time", "5", "Max execution time in minutes"));
        simulationParameters.add(Triple.of("seed", "-", "Seed used for the random number generator"));

        return simulationParameters;
    }
}
