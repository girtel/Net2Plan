package com.net2plan.niw;


import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
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
    public WFlexAlgo(NetPlan np) {}

    /* Constants */
    public static final int CALCULATION_SPF = 0;
    public static final int CALCULATION_SSP = 1;
    public static final int WEIGHT_IGP = 0;
    public static final int WEIGHT_SSP = 1;
    public static final int WEIGHT_LATENCY = 2;

    public static final String[] calculationNames = new String[] {"SPF", "SSP"};
    public static final String[] weightNames = new String[] {"IGP", "TE", "Latency"};

    /* Content for right click options */
    public static List<Pair<String, Integer>> getCalculationOptions() { return Arrays.asList(Pair.of("SPF", CALCULATION_SPF), Pair.of("SSP", CALCULATION_SSP)); }
    public static List<Pair<String, Integer>> getWeightOptions() { return Arrays.asList(Pair.of("IGP", WEIGHT_IGP), Pair.of("TE", WEIGHT_SSP), Pair.of("Latency", WEIGHT_LATENCY)); }







    @JsonAutoDetect(fieldVisibility = Visibility.ANY, getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, isGetterVisibility = Visibility.NONE)
    public static class FlexAlgoRepository
    {
        public Map<Integer, FlexAlgoProperties> mapFlexAlgoId2FlexAlgoProperties = new LinkedHashMap<>();
        public FlexAlgoRepository() {}


        /* Checkers */
        public boolean containsKey(int key) { return mapFlexAlgoId2FlexAlgoProperties.containsKey(key); }
        public boolean isEmpty() { return mapFlexAlgoId2FlexAlgoProperties.isEmpty(); }
        public boolean isGoodK(int k) { return k >= 128 && k <= 255 && !containsKey(k); }

        /* Get properties */
        public FlexAlgoProperties getFlexAlgoPropertiesFromID(int id) { assert containsKey(id); return mapFlexAlgoId2FlexAlgoProperties.get(id); }
        public List<FlexAlgoProperties> getAll() { return isEmpty() ? new ArrayList<>() : new ArrayList<>(mapFlexAlgoId2FlexAlgoProperties.values()); }

        /* Remove properties */
        public void removeFlexAlgoPropertiesFromID(int id) { assert containsKey(id); mapFlexAlgoId2FlexAlgoProperties.remove(id); }
        public void clear() { mapFlexAlgoId2FlexAlgoProperties.clear(); }

        /* New properties */
        public void addFlexAlgo(int flexAlgoId, Optional<FlexAlgoProperties> flexAlgo)
        {
            assert flexAlgo.isPresent(); assert !containsKey(flexAlgoId); assert flexAlgoId == flexAlgo.get().getK();
            mapFlexAlgoId2FlexAlgoProperties.put(flexAlgoId, flexAlgo.get());
        }


        /* Change properties */
        public void changeFlexAlgoK(int oldK, int newK)
        {
            assert containsKey(oldK); assert !containsKey(newK);
            FlexAlgoProperties flex = mapFlexAlgoId2FlexAlgoProperties.get(oldK);
            flex.flexAlgoIndentifier = newK;
            addFlexAlgo(newK, Optional.of(flex));
            removeFlexAlgoPropertiesFromID(oldK);
        }










        /* Generic */
        public void performOperation(int flexAlgoId, Consumer<FlexAlgoProperties> operation)
        {
            assert containsKey(flexAlgoId);
            operation.accept( mapFlexAlgoId2FlexAlgoProperties.get(flexAlgoId) );
        }

        public <T> T performOperationAndReturn(int flexAlgoId, Function<FlexAlgoProperties, T> operation)
        {
            assert containsKey(flexAlgoId);
            return operation.apply( mapFlexAlgoId2FlexAlgoProperties.get(flexAlgoId) );
        }

        public void performBatchOperation(List<Integer> idList, Consumer<FlexAlgoProperties> operation)
        {
            for(int id : idList)
            {
                assert containsKey(id);
                operation.accept(getFlexAlgoPropertiesFromID(id));
            }
        }
        public <T> List<T> performBatchOperationAndReturn(List<Integer> idLIst, Function<FlexAlgoProperties, T> operation)
        {
            List<T> results = new ArrayList<>();
            for(int id : idLIst)
            {
                assert containsKey(id);
                results.add( operation.apply(getFlexAlgoPropertiesFromID(id)) );
            }
            return results;
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
        public boolean isIgpWeighted() {return weightType == WEIGHT_IGP;}
        public boolean isLatencyWeighted() {return weightType == WEIGHT_LATENCY;}
        public boolean isTeWeighted() {return weightType == WEIGHT_SSP;}


        /* Modify properties content */
        /* Add */
        public FlexAlgoProperties addLink(Link e) { linkIdsIncluded.add(e.getId()); return this; }
        public FlexAlgoProperties addSid(String sid) { associatedSids.add(sid); return this; }

        /* Remove */
        public FlexAlgoProperties removeLink(Link e) { linkIdsIncluded.remove(e.getId()); return this; }
        public FlexAlgoProperties removeSid(String sid) { associatedSids.remove(sid); return this; }
        public FlexAlgoProperties removeNodesAndLinks() { linkIdsIncluded.clear(); nodeIdsIncluded.clear(); return this; }

        /* Set properties content */
        public FlexAlgoProperties setK(int k, FlexAlgoRepository repo) { if(k >= 128 && k <= 255 && !repo.containsKey(k)) this.flexAlgoIndentifier = k; return this; }
        public FlexAlgoProperties setCalculationType(int calculationType) {this.calculationType = calculationType; return this; }
        public FlexAlgoProperties setWeightType(int weightType) {this.weightType = weightType; return this; }
        public FlexAlgoProperties setLinkIdsIncluded(Set<Long> linkIdSet) { this.linkIdsIncluded = new TreeSet<>(linkIdSet); return this; }
        public FlexAlgoProperties setAssociatedSids(Set<String> sidSet) { this.associatedSids = new TreeSet<>(sidSet); return this; }
        public FlexAlgoProperties setNodeIdsIncluded(Set<Long> nodeIdSet) { this.nodeIdsIncluded = new TreeSet<>(nodeIdSet); return this; }


        /* Get properties content */
        public SortedSet<Link> getLinksIncluded(NetPlan np) {return linkIdsIncluded.stream().map(np::getLinkFromId).filter(Objects::nonNull).collect(Collectors.toCollection(TreeSet::new));}
        public SortedSet<Node> getNodesIncluded(NetPlan np) {return nodeIdsIncluded.stream().map(np::getNodeFromId).filter(Objects::nonNull).collect(Collectors.toCollection(TreeSet::new));}
        public SortedSet<String> getAssociatedSids() { return new TreeSet<>(associatedSids); }
        public int getK() {return this.flexAlgoIndentifier;}
        public int getCalculationType() {return this.calculationType;}
        public int getWeightType() {return this.weightType;}



        /* Visual content */
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
        public String getCalculationString() { return calculationNames[calculationType]; }
        public String getWeightTypeString() { return weightNames[weightType]; }


        /* Misc methods */
        @Override
        public int compareTo(Object o)
        {
            FlexAlgoProperties a = (FlexAlgoProperties) o;
            FlexAlgoProperties b = this;
            return a.getK() == b.getK() && a.getWeightType() == b.getWeightType() && a.getCalculationType() == b.getCalculationType()
            && a.getAssociatedSids().equals(b.getAssociatedSids()) ? 0 : 1;

        }
    }


}
