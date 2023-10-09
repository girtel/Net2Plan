package com.net2plan.niw;


import java.util.*;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.utils.Pair;


public class WFlexAlgo 
{
    /**
     * Creates a WNet object from a NetPlan object. Does not check its consistency as a valid NIW design
     *
     * @param np see above
     */
    public WFlexAlgo(NetPlan np) {  }


    @JsonAutoDetect(fieldVisibility = Visibility.ANY, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE)
    public static class FlexAlgoRepository
    {
        public FlexAlgoRepository() { }
        public Map<Integer, FlexAlgoProperties> mapFlexAlgoId2FlexAlgoProperties = new LinkedHashMap<>();

        /* Checkers */
        public boolean containsKey(int key) { return mapFlexAlgoId2FlexAlgoProperties.containsKey(key); }

        /* Modifiers */
        public FlexAlgoProperties getFlexAlgoPropertiesFromID(int id)
        {
            assert containsKey(id);
            return mapFlexAlgoId2FlexAlgoProperties.get(id);
        }
        public void removeFlexAlgoPropertiesFromID(int id)
        {
            assert containsKey(id);
            mapFlexAlgoId2FlexAlgoProperties.remove(id);
        }
    }

    @JsonAutoDetect(fieldVisibility = Visibility.ANY, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE)
    public static class FlexAlgoProperties
    {
        /* Properties */
        public int calculationType;
        public int weightType;
        public int flexAlgoIndentifier;
        public SortedSet<String> associatedSids = new TreeSet<>();
        public SortedSet<Long> linkIdsIncluded = new TreeSet<>();


        /* Constants */
        public static final int calculation_spf = 0;
        public static final int calculation_heuristic = 1;
        public static final int weight_igp = 0;
        public static final int weight_te = 1;
        public static final int weight_latency = 2;


        /* Constructors */
        public FlexAlgoProperties() { }

        public FlexAlgoProperties(int flexAlgoIndentifier, int calculationType, int weightType)
        {
            assert flexAlgoIndentifier > 0;

            this.flexAlgoIndentifier = flexAlgoIndentifier;
            this.calculationType = calculationType;
            this.weightType = weightType;
        }

        /* Checkers */
        public boolean isLinkIncluded(Link e) { return linkIdsIncluded.contains(e.getId()); }
        public boolean isSidIncluded(String s) { return associatedSids.contains(s); }

        public boolean isIgpWeighted() { return weightType == weight_igp; }
        public boolean isLatencyWeighted() { return weightType == weight_latency; }
        public boolean isTeWeighted() { return weightType == weight_te; }


        /* Modify properties content */
        public FlexAlgoProperties addLink(Link e) { linkIdsIncluded.add(e.getId()); return this; }
        public FlexAlgoProperties removeLink(Link e) { linkIdsIncluded.remove(e.getId()); return this; }
        public FlexAlgoProperties addSid(String sid) { associatedSids.add(sid); return this; }
        public FlexAlgoProperties removeSid(String sid) { associatedSids.remove(sid); return this; }



        /* Get properties content */
        public SortedSet<Link> getLinksIncluded(NetPlan np) { return linkIdsIncluded.stream().map(np::getLinkFromId).filter(Objects::nonNull).collect(Collectors.toCollection(TreeSet::new)); }
        public SortedSet<Node> getNodesAssociated(NetPlan np) { return linkIdsIncluded.stream().map(np::getNodeFromId).filter(Objects::nonNull).collect(Collectors.toCollection(TreeSet::new)); }


        public int getK() { return this.flexAlgoIndentifier; }
        public int getCalculationType() { return this.calculationType; }
        public int getWeightType() { return this.weightType; }



        /* Content for tables */
        public String getCalculationString()
        {
            switch (calculationType)
            {
                case calculation_spf: return "SPF";
                case calculation_heuristic: return "Heuristic";
                default: return "Not recognized";
            }
        }
        public String getWeightTypeString()
        {
            switch (weightType)
            {
                case weight_igp: return "IGP";
                case weight_te: return "TE";
                case weight_latency: return "Latency";
                default: return "Not recognized";
            }
        }


        /* Content for right click options */
        public static List<Pair<String, Integer>> getCalculationOptions()
        {
            return Arrays.asList(Pair.of("SPF", calculation_spf), Pair.of("Heuristic", calculation_heuristic));
        }
        public static List<Pair<String, Integer>> getWeightOptions()
        {
            return Arrays.asList(Pair.of("IGP", weight_igp), Pair.of("TE", weight_te), Pair.of("Latency", weight_latency));
        }



    }


    @JsonAutoDetect(fieldVisibility = Visibility.ANY, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE)
    public enum WeightTypes
    {
        IGP, LATENCY, TE;

        boolean isIGP() { return this == IGP; }
        boolean isTE() { return this == TE; }
        boolean isLatency() { return this == LATENCY; }
    }

    @JsonAutoDetect(fieldVisibility = Visibility.ANY, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE)
    public enum CalculationTypes
    {
        SPF, HEURISTIC;

        boolean isSPF() { return this == SPF; }
        boolean isHeuristic() { return this == HEURISTIC; }
    }
}
