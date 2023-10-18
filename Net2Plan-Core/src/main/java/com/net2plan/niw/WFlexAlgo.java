package com.net2plan.niw;


import java.util.*;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.utils.Pair;
import sun.nio.ch.Net;


public class WFlexAlgo
{
    /**
     * Creates a WNet object from a NetPlan object. Does not check its consistency as a valid NIW design
     *
     * @param np see above
     */
    public WFlexAlgo(NetPlan np) {}

    /* Constants */
    public static final int calculation_spf = 0;
    public static final int calculation_heuristic = 1;
    public static final int weight_igp = 0;
    public static final int weight_te = 1;
    public static final int weight_latency = 2;

    public static final String[] calculationNames = new String[] {"SPF", "Heuristic"};
    public static final String[] weightNames = new String[] {"IGP", "TE", "Latency"};

    /* Content for right click options */
    public static List<Pair<String, Integer>> getCalculationOptions()
    {
        return Arrays.asList(Pair.of("SPF", calculation_spf), Pair.of("Heuristic", calculation_heuristic));
    }

    public static List<Pair<String, Integer>> getWeightOptions()
    {
        return Arrays.asList(Pair.of("IGP", weight_igp), Pair.of("TE", weight_te), Pair.of("Latency", weight_latency));
    }


    @JsonAutoDetect(fieldVisibility = Visibility.ANY, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE)
    public static class FlexAlgoRepository
    {
        public Map<Integer, FlexAlgoProperties> mapFlexAlgoId2FlexAlgoProperties = new LinkedHashMap<>();

        public FlexAlgoRepository() {}


        /* Checkers */
        public boolean containsKey(int key) {return mapFlexAlgoId2FlexAlgoProperties.containsKey(key);}

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

    @JsonAutoDetect(fieldVisibility = Visibility.ANY, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, isGetterVisibility = Visibility.NONE)
    public static class FlexAlgoProperties implements Comparable
    {
        /* Properties */
        public int calculationType;
        public int weightType;
        public int flexAlgoIndentifier;
        public SortedSet<String> associatedSids = new TreeSet<>();
        public SortedSet<Long> nodeIdsIncluded = new TreeSet<>();
        public SortedSet<Long> linkIdsIncluded = new TreeSet<>();


        /* Constructors */
        public FlexAlgoProperties() {}

        public FlexAlgoProperties(int flexAlgoIndentifier, int calculationType, int weightType)
        {
            assert flexAlgoIndentifier > 0;

            this.flexAlgoIndentifier = flexAlgoIndentifier;
            this.calculationType = calculationType;
            this.weightType = weightType;
        }

        public FlexAlgoProperties(int flexAlgoIndentifier, int calculationType, int weightType, Optional<Set<Long>> ipLinkListIds, Optional<Set<Long>> nodeListIds, Optional<Set<String>> nodeSidList)
        {
            assert flexAlgoIndentifier > 0;

            this.flexAlgoIndentifier = flexAlgoIndentifier;
            this.calculationType = calculationType;
            this.weightType = weightType;
            ipLinkListIds.ifPresent(longs -> this.linkIdsIncluded = new TreeSet<>(longs));
            nodeListIds.ifPresent(longs -> this.nodeIdsIncluded = new TreeSet<>(longs));
            nodeSidList.ifPresent(strings -> this.associatedSids = new TreeSet<>(strings));
        }

        /* Checkers */
        public boolean isLinkIncluded(Link e) {return linkIdsIncluded.contains(e.getId());}

        public boolean isSidIncluded(String s) {return associatedSids.contains(s);}

        public boolean isNodeIncluded(Node n) {return nodeIdsIncluded.contains(n.getId());}

        public boolean isIgpWeighted() {return weightType == weight_igp;}

        public boolean isLatencyWeighted() {return weightType == weight_latency;}

        public boolean isTeWeighted() {return weightType == weight_te;}


        /* Modify properties content */
        public FlexAlgoProperties addLink(Link e)
        {
            linkIdsIncluded.add(e.getId());
            return this;
        }

        public FlexAlgoProperties removeLink(Link e)
        {
            linkIdsIncluded.remove(e.getId());
            return this;
        }

        public FlexAlgoProperties addSid(String sid)
        {
            associatedSids.add(sid);
            return this;
        }

        public FlexAlgoProperties removeSid(String sid)
        {
            associatedSids.remove(sid);
            return this;
        }

        public FlexAlgoProperties removeNodesAndLinks() { linkIdsIncluded.clear(); nodeIdsIncluded.clear(); return this; }
        public void setK(int k, FlexAlgoRepository repo) { if(k >= 128 && k <= 255 && !repo.containsKey(k)) this.flexAlgoIndentifier = k; }
        public void setCalculationType(int calculationType) {this.calculationType = calculationType;}
        public void setWeightType(int weightType) {this.weightType = weightType;}
        public void setLinkIdsIncluded(Set<Long> linkIdSet) { this.linkIdsIncluded = new TreeSet<>(linkIdSet); }
        public void setAssociatedSids(Set<String> sidSet) { this.associatedSids = new TreeSet<>(sidSet); }
        public void setNodeIdsIncluded(Set<Long> nodeIdSet) { this.nodeIdsIncluded = new TreeSet<>(nodeIdSet); }


        /* Get properties content */
        public SortedSet<Link> getLinksIncluded(NetPlan np) {return linkIdsIncluded.stream().map(np::getLinkFromId).filter(Objects::nonNull).collect(Collectors.toCollection(TreeSet::new));}
        public SortedSet<Node> getNodesIncluded(NetPlan np) {return nodeIdsIncluded.stream().map(np::getNodeFromId).filter(Objects::nonNull).collect(Collectors.toCollection(TreeSet::new));}
        public SortedSet<String> getAssociatedSids() { return new TreeSet<>(associatedSids); }
        public String getAssociatedSidsAsNiceLookingString()
        {
            StringBuilder sb = new StringBuilder();
            associatedSids.forEach(sid -> sb.append(sid).append(", "));
            return sb.toString();
        }
        public String getIncludedNodesAsNiceLookingString(NetPlan np)
        {
            StringBuilder sb = new StringBuilder();
            getNodesIncluded(np).forEach(n -> sb.append(n.getName()).append(" "));
            return sb.toString();
        }

        public int getK() {return this.flexAlgoIndentifier;}

        public int getCalculationType() {return this.calculationType;}

        public int getWeightType() {return this.weightType;}


        /* Content for tables */
        public String getCalculationString() { return calculationNames[calculationType]; }

        public String getWeightTypeString() { return weightNames[weightType]; }


        /* Content for right click options */
        public static List<Pair<String, Integer>> getCalculationOptions()
        {
            return Arrays.asList(Pair.of("SPF", calculation_spf), Pair.of("Heuristic", calculation_heuristic));
        }

        public static List<Pair<String, Integer>> getWeightOptions()
        {
            return Arrays.asList(Pair.of("IGP", weight_igp), Pair.of("TE", weight_te), Pair.of("Latency", weight_latency));
        }


        @Override
        public int compareTo(Object o)
        {
            FlexAlgoProperties flex = (FlexAlgoProperties) o;
            if (this.getK() == (flex.getK())) return 0;
            if (this.getWeightType() == flex.getWeightType()) return 0;
            if (this.getCalculationType() == flex.getCalculationType()) return 0;

            return -1;
        }
    }


}
