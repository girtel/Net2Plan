/*******************************************************************************
 * Copyright (c) 2015 Pablo Pavon Mari�o.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Contributors:
 * Pablo Pavon Mari�o - initial API and implementation
 ******************************************************************************/
//
//
//package com.net2plan.io;
//
//import com.net2plan.interfaces.networkDesign.Link;
//import com.net2plan.interfaces.networkDesign.NetPlan;
//import com.net2plan.interfaces.networkDesign.Node;
//import com.net2plan.internal.Constants;
//import com.net2plan.internal.plugins.IOFilter;
//import com.net2plan.utils.Pair;
//import com.net2plan.utils.StringUtils;
//import com.net2plan.utils.Triple;
//import edu.uci.ics.jung.graph.Hypergraph;
//import edu.uci.ics.jung.graph.UndirectedGraph;
//import edu.uci.ics.jung.graph.UndirectedSparseMultigraph;
//import edu.uci.ics.jung.graph.util.EdgeType;
//import edu.uci.ics.jung.io.GraphMLMetadata;
//import edu.uci.ics.jung.io.GraphMLReader;
//import org.apache.commons.collections15.Factory;
//import org.xml.sax.Attributes;
//import org.xml.sax.SAXException;
//import org.xml.sax.SAXNotSupportedException;
//
//import javax.xml.parsers.ParserConfigurationException;
//import java.io.*;
//import java.nio.charset.StandardCharsets;
//import java.util.*;
//import java.util.Map.Entry;
//
///**
// * Importer filter for GraphML topology format ({@code .graphml}, {@code .xml}).
// *
// * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
// * @since 0.3.1
// * @see <a href='http://graphml.graphdrawing.org/'>The GraphML File Format</a>
// */
//public class IOGraphML extends IOFilter {
//    private final static String TITLE = "GraphML";
//
//    /**
//     * Default constructor.
//     *
//     * @since 0.3.1
//     */
//    public IOGraphML() {
//        super(TITLE, EnumSet.of(Constants.IOFeature.LOAD_DESIGN), StringUtils.arrayOf("graphml", "xml"));
//    }
//
//    @Override
//    public String getName() {
//        return TITLE + " import filter";
//    }
//
//    @Override
//    public List<Triple<String, String, String>> getParameters() {
//        return null;
//    }
//
//    @Override
//    public NetPlan readFromFile(File file) {
//        try {
//            Factory<Long> vertexFactory = new ItemFactory();
//            Factory<Long> edgeFactory = new ItemFactory();
//
//            GraphMLReaderImproved<UndirectedGraph<Long, Long>, Long, Long> gmlr = new GraphMLReaderImproved<UndirectedGraph<Long, Long>, Long, Long>(vertexFactory, edgeFactory);
//            final UndirectedGraph<Long, Long> graph = new UndirectedSparseMultigraph<Long, Long>();
//            try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
//                gmlr.load(in, graph);
//            } catch (IOException ex) {
//                throw new RuntimeException(ex);
//            }
//
//            NetPlan netPlan = new NetPlan();
//
//            for (Entry<String, GraphMLMetadata<UndirectedGraph<Long, Long>>> entry : gmlr.getGraphMetadata().entrySet()) {
//                String key = gmlr.getKeyName(entry.getKey(), Constants.NetworkElementType.NETWORK);
//                String value = entry.getValue().transformer.transform(graph);
//                if (value == null) continue;
//
//                netPlan.setAttribute(key, value);
//            }
//
//            Map<String, GraphMLMetadata<Long>> vertexMetadata = gmlr.getVertexMetadata();
//            Map<String, GraphMLMetadata<Long>> edgeMetadata = gmlr.getEdgeMetadata();
//
//            Collection<Long> vertices = graph.getVertices();
//            Map<Long, Long> vertex2IdMapping = new LinkedHashMap<Long, Long>();
//            for (Long vertex : vertices) {
//                String vertexId = gmlr.getVertexIDs().get(vertex);
//                Node node = netPlan.addNode(0, 0, vertexId, null);
//                long nodeId = node.getId();
//
//                vertex2IdMapping.put(vertex, nodeId);
//
//                try {
//                    for (Entry<String, GraphMLMetadata<Long>> metadata : vertexMetadata.entrySet()) {
//                        String key = gmlr.getKeyName(metadata.getKey(), Constants.NetworkElementType.NODE);
//                        String value = metadata.getValue().transformer.transform(vertex);
//                        if (value == null) continue;
//
//                        node.setAttribute(key, value);
//                    }
//                } catch (Throwable e) {
//
//                }
//            }
//
//            Collection<Long> edges = graph.getEdges();
//
//            for (Long edge : edges) {
//                Set<Long> linkIds_thisEdge = new LinkedHashSet<Long>();
//                boolean isDirected = graph.getEdgeType(edge) == EdgeType.DIRECTED;
//                edu.uci.ics.jung.graph.util.Pair<Long> nodePair_thisLink = graph.getEndpoints(edge);
//
//                if (isDirected) {
//                    long linkId = netPlan.addLink(netPlan.getNodeFromId(vertex2IdMapping.get(nodePair_thisLink.getFirst())), netPlan.getNodeFromId(vertex2IdMapping.get(nodePair_thisLink.getSecond())), 0, 0, 200000, null).getId();
//                    linkIds_thisEdge.add(linkId);
//                } else {
//                    Pair<Link, Link> linkIds = netPlan.addLinkBidirectional(netPlan.getNodeFromId(vertex2IdMapping.get(nodePair_thisLink.getFirst())), netPlan.getNodeFromId(vertex2IdMapping.get(nodePair_thisLink.getSecond())), 0, 0, 200000, null);
//                    linkIds_thisEdge.add(linkIds.getFirst().getId());
//                    linkIds_thisEdge.add(linkIds.getSecond().getId());
//                }
//
//                for (Entry<String, GraphMLMetadata<Long>> metadata : edgeMetadata.entrySet()) {
//                    try {
//                        String key = gmlr.getKeyName(metadata.getKey(), Constants.NetworkElementType.LINK);
//                        String value = metadata.getValue().transformer.transform(edge);
//                        if (value == null) continue;
//
//                        for (long linkId : linkIds_thisEdge)
//                            netPlan.getLinkFromId(linkId).setAttribute(key, value);
//                    } catch (Throwable e) {
//
//                    }
//                }
//            }
//
//            return netPlan;
//        } catch (ParserConfigurationException | SAXException e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//    private static class GraphMLReaderImproved<G extends Hypergraph<V, E>, V, E> extends GraphMLReader<G, V, E> {
//        private final EnumMap<KeyType, Map<String, String>> keyMap;
//
//        /**
//         * Default constructor.
//         *
//         * @param vertex_factory the vertex factory to use to create vertex objects
//         * @param edge_factory the edge factory to use to create edge objects
//         * @throws ParserConfigurationException
//         * @throws SAXException
//         * @since 0.3.1
//         */
//        public GraphMLReaderImproved(Factory<V> vertex_factory, Factory<E> edge_factory) throws ParserConfigurationException, SAXException {
//            super(vertex_factory, edge_factory);
//
//            keyMap = new EnumMap<KeyType, Map<String, String>>(KeyType.class);
//            keyMap.put(KeyType.EDGE, new LinkedHashMap<String, String>());
//            keyMap.put(KeyType.GRAPH, new LinkedHashMap<String, String>());
//            keyMap.put(KeyType.VERTEX, new LinkedHashMap<String, String>());
//        }
//
//        @Override
//        protected void createKey(Attributes atts) throws SAXNotSupportedException {
//            super.createKey(atts);
//
//            Map<String, String> key_atts = getAttributeMap(atts);
//            String attrName = key_atts.get("attr.name");
//            if (attrName == null) attrName = current_key;
//
//            switch (key_type) {
//                case ALL:
//                    keyMap.get(KeyType.EDGE).put(current_key, attrName);
//                    keyMap.get(KeyType.GRAPH).put(current_key, attrName);
//                    keyMap.get(KeyType.VERTEX).put(current_key, attrName);
//                    break;
//                case EDGE:
//                    keyMap.get(KeyType.EDGE).put(current_key, attrName);
//                    break;
//                case GRAPH:
//                    keyMap.get(KeyType.GRAPH).put(current_key, attrName);
//                    break;
//                case VERTEX:
//                    keyMap.get(KeyType.VERTEX).put(current_key, attrName);
//                    break;
//                default:
//                    throw new RuntimeException("Bad");
//            }
//        }
//
//        /**
//         * Returns the actual name (if 'attr.name' is present) of a {@code <key>} element.
//         *
//         * @param key Key identifier ('id' attribute)
//         * @param networkElementType {@link com.net2plan.internal.Constants.NetworkElementType#NETWORK NETWORK}, {@link com.net2plan.internal.Constants.NetworkElementType#NODE NODE} or {@link com.net2plan.internal.Constants.NetworkElementType#LINK LINK}
//         * @return The actual name (if 'attr.name' is present), or the input key identifier
//         * @since 0.3.1
//         */
//        public String getKeyName(String key, Constants.NetworkElementType networkElementType) {
//            switch (networkElementType) {
//                case NETWORK:
//                    return keyMap.get(KeyType.GRAPH).get(key);
//
//                case NODE:
//                    return keyMap.get(KeyType.VERTEX).get(key);
//
//                case LINK:
//                    return keyMap.get(KeyType.EDGE).get(key);
//
//                default:
//                    throw new RuntimeException("Bad");
//            }
//        }
//    }
//
//    private static class ItemFactory implements Factory<Long> {
//        private long n = 0;
//
//        @Override
//        public Long create() {
//            return n++;
//        }
//    }
//}
