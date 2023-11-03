package com.net2plan.examples.niw.algorithms;

import com.net2plan.interfaces.networkDesign.IAlgorithm;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.niw.*;
import com.net2plan.utils.Triple;

import java.util.*;
import java.util.stream.Collectors;

/* FlexAlgo simulator */

public class FlexAlgoTopologyOptimization implements IAlgorithm
{
    private long computeId(Link l, int N) { return l.getOriginNode().getId() + N * l.getDestinationNode().getId(); }

    @Override
    public String executeAlgorithm(NetPlan netPlan, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
    {
        /* Get simulation parameters */
        final double maxExecutionTimeInSeconds = Double.parseDouble(algorithmParameters.get("exec-time"));
        final long algorithmStartTime = System.nanoTime();
        final long mainLoopEndTime = algorithmStartTime + (long) (0.95 * maxExecutionTimeInSeconds * 1e9);
        final long algorithmEndTime = algorithmStartTime + (long) (maxExecutionTimeInSeconds * 1e9);
        final double penalization = Double.parseDouble(algorithmParameters.get("penalization"));
        final double costPerCg = Double.parseDouble(algorithmParameters.get("costPerCg"));
        final double costPerMs = Double.parseDouble(algorithmParameters.get("costPerMs"));





        final WNet net = new WNet (netPlan);














        TopologyEvaluation evaluation = new TopologyEvaluation(net, penalization, costPerCg, costPerMs);

        return "Current cost: " + evaluation.getCost() + ". Current penalized cost: " + evaluation.getPenalizedCost();
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

        simulationParameters.add(Triple.of("exec-time", "60", "Max execution time in seconds"));
        simulationParameters.add(Triple.of("seed", "-", "Seed used for the random number generator"));
        simulationParameters.add(Triple.of("penalization", "1000", "Factor used to penalize restrictions"));
        simulationParameters.add(Triple.of("costPerCg", "3", "Factor to transform latency into cost"));
        simulationParameters.add(Triple.of("costPerMs", "3", "Factor to transform congestion into cost"));

        return simulationParameters;
    }





    public static class TopologyEvaluation
    {
        // TODO evaluate a penalization of not using SR

        /* Cost variables */
        private double cost = 0;
        private double penalizationCost = 0;


        /**
         *
         * @param net {@link WNet Topology} to evaluate its cost
         * @param penalization Penalization factor
         * @param costPerCg Cost for congestion measures
         * @param costPerMs Cost for latency measures
         */
        public TopologyEvaluation(WNet net, double penalization, double costPerCg, double costPerMs)
        {
            // TODO find good parameters

            DefaultStatelessSimulator simulator = new DefaultStatelessSimulator();
            Map<String, String> algPar = new HashMap<>();
            algPar.put("mplsTeTunnelType", "cspf-dynamic");
            simulator.executeAlgorithm(net.getNe(), algPar, new HashMap<>()); // Route all demands with the default simulator of niw.
            // This will update the np object with all that routing info.


            /* Obtain some previous info about the topology */
            final List<WIpLink> links = net.getIpLinks();
            final List<WIpUnicastDemand> demands = net.getIpUnicastDemands();
            final List<WIpUnicastDemand> demandsOfSrWithTmax = net.getIpUnicastDemands().stream().filter(WIpUnicastDemand::isSegmentRoutingActive).filter(d -> d.getMaximumAcceptableE2EWorstCaseLatencyInMs() != Double.MAX_VALUE).collect(Collectors.toList());


            /* Calculate the main objective function cost */
            for(WIpUnicastDemand d: demandsOfSrWithTmax) cost += d.getWorstCaseEndtoEndLatencyMs()*costPerMs;
            double maxCg = Double.MAX_VALUE;
            for(WIpLink l: links) maxCg = Math.min(maxCg, l.getCurrentUtilization());
            cost += maxCg*costPerCg;


            /* Calculate the penalization costs */
            // Demands whose latency does not accomplish maximum latency allowed
            for(WIpUnicastDemand d: demandsOfSrWithTmax)
                penalizationCost += Math.max(d.getWorstCaseEndtoEndLatencyMs() - d.getMaximumAcceptableE2EWorstCaseLatencyInMs(), 0)*costPerMs * penalization;

            // Demands with traffic loss
            for(WIpUnicastDemand d: demands)
                penalizationCost += (1 - d.getCurrentCarriedTrafficGbps()/d.getCurrentOfferedTrafficInGbps())*100*costPerCg * penalization;

            // Links with over subscription
            for(WIpLink l: links)
                penalizationCost += Math.max(l.getCarriedTrafficGbps() - l.getCurrentCapacityGbps(), 0)*costPerCg * penalization;

        }

        public double getCost() { return cost; }
        public double getPenalizedCost() { return cost + penalizationCost; }

    }



}
