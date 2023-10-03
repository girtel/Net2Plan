package com.net2plan.niw;


import java.util.*;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.NetPlan;


public class WFlexAlgo extends WNet
{
    /**
     * Creates a WNet object from a NetPlan object. Does not check its consistency as a valid NIW design
     *
     * @param np see above
     */
    public WFlexAlgo(NetPlan np) { super(np); }


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
        public CalculationTypes calculationType;
        public WeightTypes weightType;
        public int flexAlgoIndentifier;
        public SortedSet<String> associatedSids = new TreeSet<>();
        public SortedSet<Long> linkIdsIncluded = new TreeSet<>();



        /* Constructors */
        public FlexAlgoProperties() { }

        public FlexAlgoProperties(int flexAlgoIndentifier, CalculationTypes calculationType, WeightTypes weightType)
        {
            assert flexAlgoIndentifier > 0;

            this.flexAlgoIndentifier = flexAlgoIndentifier;
            this.calculationType = calculationType;
            this.weightType = weightType;
        }






        /* Checkers */
        public boolean isLinkIncluded(Link e) { return linkIdsIncluded.contains(e.getId()); }
        public boolean isSidIncluded(String s) { return associatedSids.contains(s); }

        public boolean isIgpWeighted() { return weightType.isIGP(); }
        public boolean isLatencyWeighted() { return weightType.isLatency(); }
        public boolean isTeWeighted() { return weightType.isTE(); }


        /* Modify properties content */
        public FlexAlgoProperties addLink(Link e) { linkIdsIncluded.add(e.getId()); return this; }
        public FlexAlgoProperties removeLink(Link e) { linkIdsIncluded.remove(e.getId()); return this; }
        public FlexAlgoProperties addSid(String sid) { associatedSids.add(sid); return this; }
        public FlexAlgoProperties removeSid(String sid) { associatedSids.remove(sid); return this; }



        /* Get properties content */
        public SortedSet<Link> getLinksIncluded(NetPlan np) { return linkIdsIncluded.stream().map(np::getLinkFromId).filter(Objects::nonNull).collect(Collectors.toCollection(TreeSet::new)); }


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
