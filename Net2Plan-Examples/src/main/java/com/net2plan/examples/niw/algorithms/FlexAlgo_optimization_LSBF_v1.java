package com.net2plan.examples.niw.algorithms;

import com.net2plan.interfaces.networkDesign.IAlgorithm;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.niw.*;
import com.net2plan.utils.Triple;

import java.util.*;
import java.util.stream.Collectors;

/* FlexAlgo simulator */

public class FlexAlgo_optimization_LSBF_v1 implements IAlgorithm
{
    public static final String CONTENT_SPLITTER = "----<  >--------<  >--------<  >--------<  >--------<  >--------<  >--------<  >----";
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
        simulationParameters.add(Triple.of("multi", "true", "Obtain multiple links that can be computed as one"));
        simulationParameters.add(Triple.of("strict", "true", "If true, Flex-Algo do the routing strictly following the flex-algo. If false, Flex-Algo can all the links that are not assigned in other flex-algo"));
        return simulationParameters;
    }

    /* --------------------------------------------------------------------------- */
    /* ########################################################################### */
    /* #                                                                         # */
    /* #                                 ALGORITHM                               # */
    /* #                                                                         # */
    /* ########################################################################### */
    /* --------------------------------------------------------------------------- */
    @Override
    public String executeAlgorithm(NetPlan netPlan, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
    {
        /* Get simulation parameters */
        final double maxExecutionTimeInSeconds = Double.parseDouble(algorithmParameters.get("exec-time"));
        final long algorithmStartTime = System.nanoTime();
        final long mainLoopEndTime = algorithmStartTime + (long) (0.95 * maxExecutionTimeInSeconds * 1e9);
        final long algorithmEndTime = algorithmStartTime + (long) (maxExecutionTimeInSeconds * 1e9);
        final double penalization = Double.parseDouble(algorithmParameters.get("penalization"));
        final boolean multiLinks = Boolean.parseBoolean(algorithmParameters.get("multi"));
        final boolean strictFlexAlgo = Boolean.parseBoolean(algorithmParameters.get("strict"));

        /* Initial tasks. Store data from topology */
        final NetPlan np = netPlan.copy();
        final WNet net = new WNet (np);
        if(np.getFlexAlgoProperties().isEmpty()) net.initializeFlexAlgoAttributes();
        WNet.performOperationOnRepository(net.getNe(), repo -> {
            repo.clear();
            repo.addFlexAlgo(128, Optional.of(new WFlexAlgo.FlexAlgoProperties(128, WFlexAlgo.CALCULATION_SPF, WFlexAlgo.WEIGHT_IGP)));
            repo.addFlexAlgo(129, Optional.of(new WFlexAlgo.FlexAlgoProperties(129, WFlexAlgo.CALCULATION_SPF, WFlexAlgo.WEIGHT_TE)));
            repo.addFlexAlgo(130, Optional.of(new WFlexAlgo.FlexAlgoProperties(130, WFlexAlgo.CALCULATION_SPF, WFlexAlgo.WEIGHT_LATENCY)));
        });
        for(WIpUnicastDemand d: net.getIpUnicastDemands()) // TODO this can be changed to maybe only evaluate for demands that are SR enabled from the topology
            d.setSegmentRoutingEnabled(true);
        TopologyEvaluation.initialize(net, penalization, strictFlexAlgo);


        /* Create a cache for fastly store common data and easily manipulate it */
        final DataCache cache = new DataCache(net);

        /* Statistics variables to evaluate the performance of multiple operations. With this we can check if algorithm is behaving correctly */
        ArrayList<Double> findingInTabooList = new ArrayList<>();
        ArrayList<Double> calculatingCost = new ArrayList<>();
        ArrayList<Double> iterationTimes = new ArrayList<>();
        int totalIterations = 0;
        int randomizedIterations = 0;
        int numberOfTimesPermutedFlexAlgoWereBetter = 0;


        /* Main loop. Find a random solution and make a best fit iteration until reach time */
        Solution bestSolutionFound = new Solution(net, cache.flexAlgos());
        double bestCostFound = (new TopologyEvaluation(bestSolutionFound)).getPenalizedCost();
        boolean improvedFromPreviousSolution = true;

        while (System.nanoTime() < mainLoopEndTime)
        {
            /* Select a random solution to begin each BF iteration (while stuck at best neighbor) */
            for(WFlexAlgo.FlexAlgoProperties flex: cache.flexAlgos())
            {
                int maxAdjacencies = cache.rng().nextInt(cache.getLinkAdjacencies().size());
                while (--maxAdjacencies >= 0)
                    cache.swapLinksInsideFlexAlgo(flex, cache.getRandomLinkAdjacency());
            }
            for(WIpUnicastDemand demand: cache.demands())
                demand.setFlexAlgoId(Optional.of(String.valueOf(cache.getRandomFlexAlgo().getK())));
            Solution initialSolution = new Solution(net, cache.flexAlgos());
            TopologyEvaluation initialEvaluation = new TopologyEvaluation(initialSolution);
            cache.addSolutionToIterated(initialSolution, initialEvaluation.getPenalizedCost());


            /* Second loop. Iterate BestFit for each random solution */
            while (System.nanoTime() < mainLoopEndTime)
            {
                long t0 = System.nanoTime();
                randomizedIterations++;

                double costBeforeIteration = bestCostFound;

                // Best fit behavior
                for(WFlexAlgo.FlexAlgoProperties flex: cache.flexAlgos())
                {
                    // Swap demands
                    for(WIpUnicastDemand demand: cache.demands())
                    {
                        totalIterations++;

                        String previousFlexAlgo = demand.getSrFlexAlgoId().orElse("0");
                        demand.setFlexAlgoId(Optional.of(String.valueOf(flex.getK())));
                        Solution neighbor = new Solution(net, cache.flexAlgos());

                        // Check whether this combination of links, demands and flex algo have already been evaluated, to save time evaluating a solution that has already been evaluated
                        long t2 = System.nanoTime();
                        final boolean alreadyIterated = cache.isSolutionAlreadyIterated(neighbor);
                        findingInTabooList.add( (System.nanoTime() - t2)/1e6 );

                        // If it has not been already evaluated, save the cost to the neighborhood map and go back to previous solution
                        if(!alreadyIterated)
                        {
                            long n3 = System.nanoTime();
                            TopologyEvaluation neighborEvaluation = new TopologyEvaluation(neighbor);
                            calculatingCost.add( (System.nanoTime() - n3)/1e6 );
                            cache.addSolutionToIterated(neighbor, neighborEvaluation.getPenalizedCost());

                            final boolean costImproved = neighborEvaluation.getPenalizedCost() < bestCostFound;
                            if(costImproved)
                            {
                                bestSolutionFound = neighbor;
                                bestCostFound = neighborEvaluation.getPenalizedCost();
                            }
                        }

                        // Go back to previous solution
                        demand.setFlexAlgoId(Optional.of(previousFlexAlgo));
                    } /* End of iterating demands */

                    // Swap links
                    for (Set<WIpLink> linkAdjacencies: cache.getLinkAdjacencies())
                    {
                        cache.swapLinksInsideFlexAlgo(flex, linkAdjacencies);
                        Solution neighbor = new Solution(net, cache.flexAlgos());

                        // Check whether this combination of links, demands and flex algo have already been evaluated, to save time evaluating a solution that has already been evaluated
                        long t2 = System.nanoTime();
                        final boolean alreadyIterated = cache.isSolutionAlreadyIterated(neighbor);
                        findingInTabooList.add( (System.nanoTime() - t2)/1e6 );

                        // If it has not been already evaluated, save the cost to the neighborhood map and go back to previous solution
                        if(!alreadyIterated)
                        {
                            long n3 = System.nanoTime();
                            TopologyEvaluation neighborEvaluation = new TopologyEvaluation(neighbor);
                            calculatingCost.add( (System.nanoTime() - n3)/1e6 );
                            cache.addSolutionToIterated(neighbor, neighborEvaluation.getPenalizedCost());

                            final boolean costImproved = neighborEvaluation.getPenalizedCost() < bestCostFound;
                            if(costImproved)
                            {
                                bestSolutionFound = neighbor;
                                bestCostFound = neighborEvaluation.getPenalizedCost();
                            }
                        }

                        // Go back to previous solution
                        cache.swapLinksInsideFlexAlgo(flex, linkAdjacencies);
                    } /* End of iterating links */
                } /* End of iterating Flex-Algo */


                iterationTimes.add( (System.nanoTime() - t0)/1e6 );


                /* If cost did not improve from all neighbor, optimum local found */
                final boolean costImproved = bestCostFound < costBeforeIteration;
                if(!costImproved) break; // break and repeat random solution until there is time (with the hope that there will be a better solution iterating randomly)
            } /* End of BF */

        } /* End of main loop */




        /* Statistics calculation */
        double meanFindingInTabooList = 0; for(Double d: findingInTabooList) meanFindingInTabooList += d; meanFindingInTabooList /= findingInTabooList.size();
        double meanCalculatingCost = 0; for(Double d: calculatingCost) meanCalculatingCost += d; meanCalculatingCost /= calculatingCost.size();
        double meanIterationTime = 0; for(Double d: iterationTimes) meanIterationTime += d; meanIterationTime /= iterationTimes.size();


        System.out.println(CONTENT_SPLITTER);
        System.out.println("Mean time finding in taboo list:\t" + meanFindingInTabooList + " ms");
        System.out.println("Mean time calculating cost:\t\t" + meanCalculatingCost + " ms");
        System.out.println("Mean time per iteration:\t\t" + meanIterationTime + " ms");
        System.out.println("Total iterations: " + totalIterations);
        System.out.println("Total iterations of randomized begin: " + randomizedIterations);
        System.out.println("Total times were permuting Flex-Algo improved: " + numberOfTimesPermutedFlexAlgoWereBetter);
        System.out.println("Amount of different solutions: " + cache.iterations().size());



        /* Create a new evaluation from the best solution, to show extra data */
        bestSolutionFound.applyComponentToNet(net);
        TopologyEvaluation bestSolutionEvaluation = new TopologyEvaluation(bestSolutionFound);
        System.out.println(CONTENT_SPLITTER);
        bestSolutionEvaluation.printEvaluationData();
        System.out.println(CONTENT_SPLITTER);

        /* Set the NetPlan from the Net2Plan GUI to the form of the best solution found */
        netPlan.assignFrom(net.getNe());

        return "Best cost found: " + bestSolutionEvaluation.getCost() + ". Best cost found + penalization: " + bestSolutionEvaluation.getPenalizedCost();
    } /* End of algorithm */


    /* --------------------------------------------------------------------------- */
    /* ########################################################################### */
    /* #                                                                         # */
    /* #                           TOPOLOGY EVALUATION                           # */
    /* #                                                                         # */
    /* ########################################################################### */
    /* --------------------------------------------------------------------------- */
    public static class TopologyEvaluation
    {
        /* Cost variables */
        private double sum_targetLatency = 0, sum_realLatency = 0;
        private double percentageOfDemandsNotFollowingLatencyRestriction = 0, percentageOfLinksWithOverSubscription = 0;

        /* Help data */
        private static int glob_id = 0;
        private final int id;

        public TopologyEvaluation(Solution solution)
        {
            this.id = ++glob_id;

            /* Execute the algorithm to obtain routing info */
            // Add the flex algo information (local one) to the WNet topology, in order to simulate the net with the given algorithms
            solution.applyComponentToNet(net);
            simulator.executeAlgorithm(net.getNe(), algPar, new HashMap<>());

            /* The objective function is:
             *     min( sum(latency of all demands) + penalizationFactor * ( #Oversubscripted_links + #Demands_not_following_max_latency_restriction ) )
             */


            int numberofDemandsWithLatencyRestriction = 0;
            int numberOfDemandsNotFollowingLatencyRestriction = 0;
            int numberOfLinksWithOverSubscription = 0;

            for(WIpUnicastDemand demand: net.getIpUnicastDemands())
            {
                sum_targetLatency += demand.getWorstCaseEndtoEndLatencyMs();
                sum_realLatency += demand.getWorstCaseEndtoEndLatencyMs();
                if(demand.getMaximumAcceptableE2EWorstCaseLatencyInMs() != Double.MAX_VALUE)
                {
                    // Sum the latency of the demands with restrictions, so those demands will be routed with the shortest paths (as it will sum twice)
                    sum_targetLatency += demand.getWorstCaseEndtoEndLatencyMs();
                    numberofDemandsWithLatencyRestriction++;
                    if(demand.getWorstCaseEndtoEndLatencyMs() - demand.getMaximumAcceptableE2EWorstCaseLatencyInMs() > 0)
                        numberOfDemandsNotFollowingLatencyRestriction++;
                }

            }

            for(WIpLink link: net.getIpLinks())
                if(link.getCurrentUtilization() > 1) numberOfLinksWithOverSubscription++;


            /* Save statistics founded */
            this.percentageOfDemandsNotFollowingLatencyRestriction = (double) numberOfDemandsNotFollowingLatencyRestriction / numberofDemandsWithLatencyRestriction;
            this.percentageOfLinksWithOverSubscription = (double) numberOfLinksWithOverSubscription / net.getIpLinks().size();
        }

        public double getCost() { return sum_targetLatency; }
        public double getPenalizedCost() { return getCost() +  penalization*(percentageOfDemandsNotFollowingLatencyRestriction + percentageOfLinksWithOverSubscription); }
        public void printEvaluationData()
        {
            System.out.println("Topology evaluation #" + this.id +
                    "\n    #_sum_targetLatency_# " + sum_targetLatency +
                    "\n    #_sum_realLatency_# " + sum_realLatency +
                    "\n    #_percentageOfDemandsNotFollowingLatencyRestriction_# " + percentageOfDemandsNotFollowingLatencyRestriction +
                    "\n    #_percentageOfLinksWithOverSubscription_# " + percentageOfLinksWithOverSubscription +
                    "\n    $ penalization multiplier $ " + penalization +
                    "\n    cost = targetLatency = " + getCost() +
                    "\n    penalizedCost = " + getPenalizedCost()
            );
        }

        /* Common parts to all topology evaluations */
        private static DefaultStatelessSimulator simulator;
        private static final Map<String, String> algPar = new TreeMap<>(); // Auxiliary data
        private static double penalization;
        private static WNet net;
        public static void initialize(WNet net, double penalization, boolean strictFlexAlgo)
        {
            algPar.put("mplsTeTunnelType", "cspf-dynamic");

            if(strictFlexAlgo) algPar.put("flexAlgoRoutingFlexibility", "strict");
            else algPar.put("flexAlgoRoutingFlexibility", "wide");

            simulator = new DefaultStatelessSimulator();

            double totalSumPropagationInNetwork = 0;
            for(WIpLink l: net.getIpLinks())
                totalSumPropagationInNetwork += l.getWorstCasePropagationDelayInMs();

            TopologyEvaluation.penalization = penalization * net.getIpUnicastDemands().size() * (totalSumPropagationInNetwork / 2); // counting bidirectional links as one
            TopologyEvaluation.net = net;
        }
    } /* End of TopologyEvaluation */


    /* --------------------------------------------------------------------------- */
    /* ########################################################################### */
    /* #                                                                         # */
    /* #                           SOLUTION ABSTRACTION                          # */
    /* #                                                                         # */
    /* ########################################################################### */
    /* --------------------------------------------------------------------------- */

    public static class Solution implements Comparable<Solution>
    {
        static final int FLEX_OFFSET = 128;
        private final List<SortedSet<WIpLink>> solution_perFlexAlgoLinks = new ArrayList<> ();
        private final List<Integer> solution_perDemandFlexAlgo = new ArrayList<> ();
        private final SortedMap<WIpLink , Set<WFlexAlgo.FlexAlgoProperties>> cacheSolution_mapLink2AssignedFlexAlgos = new TreeMap<>();
        private final List<SortedSet<WIpUnicastDemand>> cacheSolution_perFlexAlgoAssignedDemands = new ArrayList<> ();
        public Solution(WNet net, List<WFlexAlgo.FlexAlgoProperties> flexAlgoProperties)
        {
            /* Import the desired routing information to the component, that is, the links of each flex-algo and the flex-algo of each demand */
            flexAlgoProperties.stream().sorted(Comparator.comparing(WFlexAlgo.FlexAlgoProperties::getK)).forEach(f -> {
                SortedSet<WIpLink> links = f.getLinksIncluded(net.getNe()).stream().map(link -> (WIpLink) net.getWElement(link).orElse(null)).filter(Objects::nonNull).collect(Collectors.toCollection(TreeSet::new));
                solution_perFlexAlgoLinks.add(links);
                cacheSolution_perFlexAlgoAssignedDemands.add(new TreeSet<>());
            });

            net.getIpUnicastDemands().stream().sorted(Comparator.comparing(WIpUnicastDemand::getId)).forEach(d -> {
                solution_perDemandFlexAlgo.add(Integer.parseInt(d.getSrFlexAlgoId().orElse("0")));
                int flexId = Math.max(Integer.parseInt(d.getSrFlexAlgoId().orElse("0")) - FLEX_OFFSET , 0);
                cacheSolution_perFlexAlgoAssignedDemands.get(flexId).add(d);
            } );
        }
        @Override
        public int hashCode() { return Objects.hash(solution_perDemandFlexAlgo, solution_perFlexAlgoLinks); }
        @Override
        public boolean equals(Object obj)
        {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            Solution other = (Solution) obj;
            return Objects.equals(solution_perDemandFlexAlgo, other.solution_perDemandFlexAlgo)
                    && Objects.equals(solution_perFlexAlgoLinks, other.solution_perFlexAlgoLinks);
        }
        @Override
        public int compareTo(Solution o) { return Integer.compare(this.hashCode(), o.hashCode()); }
        public void printComponent()
        {
            System.out.println("Solution hash=" + this.hashCode() + ": ");
            for (int i = 0; i < solution_perFlexAlgoLinks.size(); i++)
            {
                System.out.print("    Links of Flex-Algo " + (i + FLEX_OFFSET) + ": ");
                solution_perFlexAlgoLinks.get(i).forEach(l -> System.out.print(l.getId() + " "));
                System.out.println();
            }
            for (int i = 0; i < solution_perDemandFlexAlgo.size(); i++)
                System.out.println("    Flex-Algo of demand " + i + " : " + solution_perDemandFlexAlgo.get(i));
        }

        public void applyComponentToNet(WNet netToApply)
        {
            ArrayList<Integer> indexes = new ArrayList<>();
            for (int i = 0; i < solution_perFlexAlgoLinks.size(); i++) indexes.add(i + FLEX_OFFSET);

            WNet.performOperationOnFlexAlgo(netToApply.getNe(), indexes, flexAlgo -> {
                if (!flexAlgo.linkIdsIncluded.isEmpty()) flexAlgo.removeAllLinks();
                flexAlgo.setLinkIdsIncluded(solution_perFlexAlgoLinks.get(flexAlgo.getK() - FLEX_OFFSET).stream().map(WIpLink::getId).collect(Collectors.toSet()));
            });

            for (int i = 0; i < cacheSolution_perFlexAlgoAssignedDemands.size(); i++)
                for (WIpUnicastDemand d : cacheSolution_perFlexAlgoAssignedDemands.get(i))
                    d.setFlexAlgoId(Optional.of(String.valueOf(i + FLEX_OFFSET)));
        }
    } /* End of Solution */


    /* --------------------------------------------------------------------------- */
    /* ########################################################################### */
    /* #                                                                         # */
    /* #                               DATA CACHE                                # */
    /* #                                                                         # */
    /* ########################################################################### */
    /* --------------------------------------------------------------------------- */
    public static class DataCache
    {
        final WNet net;
        final Random rng;
        final int N, L, D, F;

        final List<WIpLink> bidiLinks;
        final List<WFlexAlgo.FlexAlgoProperties> flexAlgos;
        final Map<WIpUnicastDemand, Integer> flexAlgosOfDemands = new HashMap<>();
        final Set<WIpLink> tabooLinks = new HashSet<>();
        final Map<Solution, Double> evaluationOfSolutions = new TreeMap<>();
        final Set<WIpUnicastDemand> cache_demandsWithLatencyRestriction = new HashSet<>();
        final Set<WIpUnicastDemand> cache_demandsWithoutLatencyRestriction = new HashSet<>();
        final List<Set<WIpLink>> linkAdjacencies = new ArrayList<>();


        public DataCache(WNet net)
        {
            this.net = net;
            this.rng = new Random();

            /* Step for better optimization */ //  Precalculate bidirectional links and use only one direction to obtain random nodes. Avoids performing the same operation twice
            Set<Long> bidiLinksId = new HashSet<>();
            for(WIpLink l: net.getIpLinks())
                bidiLinksId.add(computeId(l, net.getNumberOfNodes()));
            bidiLinks = net.getIpLinks().stream().filter(ipLink -> bidiLinksId.contains(  computeId(ipLink, net.getNumberOfNodes())  )).collect(Collectors.toList());

            /* Obtain the filtered lists previously to improve performance */
            net.getIpUnicastDemands().forEach(d -> {
                if(d.getMaximumAcceptableE2EWorstCaseLatencyInMs() != Double.MAX_VALUE) cache_demandsWithLatencyRestriction.add(d);
                else cache_demandsWithoutLatencyRestriction.add(d);
            });
            net.getIpUnicastDemands().forEach(d -> {
                flexAlgosOfDemands.put(d, Integer.parseInt(d.getSrFlexAlgoId().orElse("0")));
            });

            /* Find taboo links */ // This help improving performance. We do not want to swap links that are the only outgoing link of a node because it will break the routing of the demands of other flex algo
            for(WNode n: net.getNodes())
                if(n.getOutgoingIpLinks().size() == 1)
                {
                    final Set<WIpLink> outLinks = n.getOutgoingIpLinks();
                    outLinks.forEach(l -> {
                        tabooLinks.add(l);
                        tabooLinks.add(l.getBidirectionalPair());
                    });
                }
            bidiLinks.removeAll(tabooLinks); // This will set bidilinks as links that are eligible to be swapped

            /* Precalculate set of links that can behave like a single one, that is, links that go through a router without in/out traffic (demands whose origin/destination is not that router)  */
            for(WIpLink l: bidiLinks)
                linkAdjacencies.add(findShortCommonPath(l));

            /* Save stats */
            this.N = net.getNumberOfNodes();
            this.L = bidiLinks.size();
            this.D = net.getIpUnicastDemands().size();
            this.flexAlgos = net.getNe().getFlexAlgoProperties();
            this.F = this.flexAlgos.size();

            /* Add all simple links to all the Flex-Algo */
            for(WFlexAlgo.FlexAlgoProperties flexAlgo: flexAlgos)
                for(WIpLink simpleLink: tabooLinks)
                    flexAlgo.addLink(simpleLink.getNe());
        }


        /* Obtain raw data */
        public Random rng() { return rng; }
        public Map<Solution, Double> iterations() { return evaluationOfSolutions; }
        public List<WIpUnicastDemand> demands() { return new ArrayList<>(this.flexAlgosOfDemands.keySet()); }
        public List<WIpLink> links() { return this.bidiLinks; }
        public List<WFlexAlgo.FlexAlgoProperties> flexAlgos() { return this.flexAlgos; }
        public List<WNode> nodes() { return net.getNodes(); }

        /* Modify parameters */
        public boolean isSolutionAlreadyIterated(Solution sol) { return evaluationOfSolutions.containsKey(sol); }
        public void addSolutionToIterated(Solution sol, double cost) { evaluationOfSolutions.put(sol, cost); }

        /* Random */
        public WIpLink getRandomLink() { return bidiLinks.get(rng.nextInt(L)); }
        public WFlexAlgo.FlexAlgoProperties getRandomFlexAlgo() { return flexAlgos.get(rng.nextInt(F)); }
        public WNode getRandomNode() { return net.getNodes().get(rng.nextInt(N)); }
        public WIpUnicastDemand getRandomDemand() { return net.getIpUnicastDemands().get(rng.nextInt(D)); }
        public int randomNumberOfLinks() { return rng.nextInt(L); }
        public Set<WIpLink> getRandomSetOfLinks() { return linkAdjacencies.get(rng.nextInt(linkAdjacencies.size())); }
        public WFlexAlgo.FlexAlgoProperties getRandomFlexAlgoFromDemandLatency(double latency) { return latency == Double.MAX_VALUE ? getRandomFlexAlgo() : flexAlgos.stream().filter(f -> f.getWeightType() == WFlexAlgo.WEIGHT_LATENCY).findAny().orElse(null); }
        public Set<WIpLink> getRandomLinkAdjacency() { return linkAdjacencies.get(rng.nextInt(linkAdjacencies.size())); }

        /* Return lists */
        public WFlexAlgo.FlexAlgoProperties getFlexAlgoOfType(int type) { return flexAlgos.stream().filter(f -> f.getWeightType() == type).findAny().orElse(null); }
        public Set<WIpUnicastDemand> getDemandsWithLatencyRestriction() { return this.cache_demandsWithLatencyRestriction; }
        public Set<WIpUnicastDemand> getDemandsWithoutLatencyRestriction() { return this.cache_demandsWithoutLatencyRestriction; }
        public List<Set<WIpLink>> getLinkAdjacencies() { return this.linkAdjacencies; }



        /* Miscellaneous functions */
        public double getCostOfSolution(Solution sol) { return evaluationOfSolutions.get(sol); }
        public long computeId(WIpLink l, int N) {
            long optionA = l.getA().getId() + N * l.getB().getId();
            long optionB = l.getB().getId() + N * l.getA().getId();
            return Math.min(optionA, optionB); // so we always get the same link when computing both directions of the link
        }

        /** This tries to find a set of links that behave like one link, that is, it finds intermediate node that only have 2 outgoing links. It tries to find if some of the endpoints of the links is an intermediate node */
        public Set<WIpLink> findShortCommonPath(WIpLink link)
        {
            Set<WIpLink> linksToAdd = new HashSet<>();
            linksToAdd.add(link);
            linksToAdd.add(link.getBidirectionalPair());

            WNode na = link.getA(), nb = link.getB();
            recursivelyFindPath(linksToAdd, new HashSet<>(), na);
            recursivelyFindPath(linksToAdd, new HashSet<>(), nb);

            return linksToAdd;
        }
        public void recursivelyFindPath(Set<WIpLink> linksToAdd, Set<WNode> nodesIterated, WNode iterationNode)
        {
            if(nodesIterated.contains(iterationNode) || !iterationNode.getOutgoingIpUnicastDemands().isEmpty()) return;
            nodesIterated.add(iterationNode);

            if(iterationNode.getOutgoingIpLinks().size() == 2)
            {
                final Set<WIpLink> outLinks = iterationNode.getOutgoingIpLinks();
                outLinks.removeAll(linksToAdd); // This set "outLinks" as links that have not been iterated yet
                outLinks.forEach(l -> {
                    linksToAdd.add(l);
                    linksToAdd.add(l.getBidirectionalPair());
                });

                // Remaining links that have not been iterated yet
                outLinks.forEach(l -> {
                    recursivelyFindPath(linksToAdd, nodesIterated, l.getB());
                    recursivelyFindPath(linksToAdd, nodesIterated, l.getA());
                });
            }
        }

        /** This method swaps a link from a flex algo, but also swaps its bidirectional pair */
        public void swapLinkAndBidirectionalPairInsideFlexAlgo(WFlexAlgo.FlexAlgoProperties flexAlgo, WIpLink linkToSwap)
        {
            if(flexAlgo.isLinkIncluded(linkToSwap.getNe()))
            {
                flexAlgo.removeLink(linkToSwap.getNe());
                flexAlgo.removeLink(linkToSwap.getBidirectionalPair().getNe());
            }
            else
            {
                flexAlgo.addLink(linkToSwap.getNe());
                flexAlgo.addLink(linkToSwap.getBidirectionalPair().getNe());
            }
        }

        /** This method swaps a set of links from a flex algo, but it <b><i><strong>DOES NOT SWAP</strong></i></b> its bidirectional pair. It is needed to add the bidirectional pair to the list of links*/
        public void swapLinksInsideFlexAlgo(WFlexAlgo.FlexAlgoProperties flexAlgo, Set<WIpLink> linksToSwap)
        {
            for(WIpLink l: linksToSwap)
            {
                if(flexAlgo.isLinkIncluded(l.getNe())) flexAlgo.removeLink(l.getNe());
                else flexAlgo.addLink(l.getNe());
            }
        }
    } /* End of DataCache */

} /* End of FlexAlgoTopologyOptimization */
