/*******************************************************************************
 * Copyright (c) 2016 Pablo Pavon-Marino. All rights reserved. This program and the accompanying materials are made available under the terms of the GNU Lesser Public License v2.1 which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Contributors: Pablo Pavon-Marino - Jose-Luis Izquierdo-Zaragoza, up to version 0.3.1 Pablo Pavon-Marino - from version 0.4.0 onwards
 ******************************************************************************/


package com.net2plan.libraries;

import cern.colt.list.tdouble.DoubleArrayList;
import cern.colt.list.tint.IntArrayList;
import cern.colt.matrix.tdouble.DoubleFactory1D;
import cern.colt.matrix.tdouble.DoubleFactory2D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import cern.colt.matrix.tdouble.algo.DenseDoubleAlgebra;
import cern.jet.math.tdouble.DoubleFunctions;
import cern.jet.math.tdouble.DoublePlusMultFirst;
import com.jom.OptimizationProblem;
import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.utils.*;
import com.net2plan.utils.Constants.CheckRoutingCycleType;
import com.net2plan.utils.Constants.RoutingCycleType;
import edu.uci.ics.jung.algorithms.filters.EdgePredicateFilter;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.algorithms.layout.StaticLayout;
import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;
import edu.uci.ics.jung.graph.DirectedOrderedSparseMultigraph;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.Context;
import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.visualization.Layer;
import edu.uci.ics.jung.visualization.VisualizationImageServer;
import edu.uci.ics.jung.visualization.control.LayoutScalingControl;
import edu.uci.ics.jung.visualization.decorators.EdgeShape;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
import edu.uci.ics.jung.visualization.renderers.DefaultVertexLabelRenderer;
import edu.uci.ics.jung.visualization.renderers.Renderer;
import org.apache.commons.collections15.ListUtils;
import org.apache.commons.collections15.Predicate;
import org.apache.commons.collections15.Transformer;
import org.apache.commons.collections15.functors.ConstantTransformer;
import org.apache.commons.collections15.functors.MapTransformer;
import org.jgrapht.DirectedGraph;
import org.jgrapht.UndirectedGraph;
import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.alg.StrongConnectivityInspector;
import org.jgrapht.graph.*;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

/**
 * <p>Auxiliary static methods to work with graphs.</p>
 * <p>
 * <p>These methods make intensive use of several Java libraries (i.e. <a href='#jom'>JOM</a>, <a href='#jgrapht'>JGraphT</a> or <a href='#jung'>JUNG</a>) hiding low-level details to users.</p>
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @see <a name='jom'></a><a href='http://www.net2plan.com/jom'>Java Optimization Modeler (JOM) website</a>
 * @see <a name='jgrapht'></a><a href='http://jgrapht.org/'>JGraphT website</a>
 * @see <a name='jung'></a><a href='http://jung.sourceforge.net/'>Java Universal Network/Graph Framework (JUNG) website</a>
 * @since 0.2.0
 */
public class GraphUtils {
    private GraphUtils() {
    }

    /**
     * <p>Checks for validity of a given path (continuity and, optionally, no loops).</p>
     *
     * @param seqLinks    Sequence of traversed links
     * @param checkCycles Indicates whether (and how) or not to check if there are cycles
     */
    public static void checkRouteContinuity(List<Link> seqLinks, CheckRoutingCycleType checkCycles) {
        if (seqLinks.isEmpty()) throw new Net2PlanException("No path");

        if (checkCycles == CheckRoutingCycleType.NO_REPEAT_LINK && (new HashSet<Link>(seqLinks)).size() != seqLinks.size())
            throw new Net2PlanException("There is a loop, seq. links = " + CollectionUtils.join(seqLinks, " => "));

        List<Node> seqNodes = convertSequenceOfLinksToSequenceOfNodes(seqLinks);
        if (checkCycles == CheckRoutingCycleType.NO_REPEAT_NODE && (new HashSet<Node>(seqNodes)).size() != seqNodes.size())
            throw new Net2PlanException("There is a loop, seq. links = " + CollectionUtils.join(seqLinks, " => ") + ", seq. nodes = " + CollectionUtils.join(seqNodes, " => "));
    }

    /**
     * Computes the Euclidean distance between two points.
     *
     * @param point1 Point 1
     * @param point2 Point 2
     * @return Euclidean distance between two points
     */
    public static double computeEuclideanDistance(Point2D point1, Point2D point2) {
        return point1.distance(point2);
    }

    /**
     * Computes the Haversine distance between two points, that is, the shortest distance over the Earth's surface.
     * <p>
     * <p><b>Important</b>: It is assumed an Earth's radius equal to {@link com.net2plan.utils.Constants#EARTH_RADIUS_IN_KM Constants.EARTH_RADIUS_IN_KM }</p>
     * <p>
     * <p><b>Important</b>: Coordinates are assumed to be given in degress, conversions to radians are made internally.</p>
     *
     * @param point1 Point 1 (x-coord is equal to longitude, y-coord is equal to latitude)
     * @param point2 Point 2 (x-coord is equal to longitude, y-coord is equal to latitude)
     * @return Haversine distance between two points
     * @see <a href="http://www.movable-type.co.uk/scripts/latlong.html">Calculate distance, bearing and more between Latitude/Longitude points</a>
     */
    public static double computeHaversineDistanceInKm(Point2D point1, Point2D point2) {
        double lon1 = point1.getX();
        double lat1 = point1.getY();
        double lon2 = point2.getX();
        double lat2 = point2.getY();

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double sindLat = Math.sin(dLat / 2);
        double sindLon = Math.sin(dLon / 2);
        double a = Math.pow(sindLat, 2) + Math.pow(sindLon, 2) * Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2));
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return Constants.EARTH_RADIUS_IN_KM * c;
    }

    /**
     * Computes the fundamental matrix of the absorbing Markov chain represented by a given demand-link routing for a given demand.
     * If the routing has closed loops, this is referred to in the RoutingCycleType, and the fundamental matrix is null
     *
     * @param demand Demand
     * @param nodes  List of nodes
     * @param links  List of links
     * @param f_de   For each demand <i>d</i> (<i>d = 0</i> refers to the first demand in {@code demandIds}, <i>d = 1</i> refers to the second one, and so on), and each link <i>e</i> (<i>e = 0</i> refers to the first link in {@code linkMap.keySet()}, <i>e = 1</i> refers to the second one, and so on), {@code f_de[d][e]} sets the fraction of the traffic from demand <i>d</i> that arrives (or is generated in) node <i>a(e)</i> (the origin node of link <i>e</i>), that is forwarded through link <i>e</i>. It must hold that for every node <i>n</i> different of <i>b(d)</i>, the sum of the fractions <i>f<sub>te</sub></i> along its outgoing links must be lower or equal than 1 (unchecked)
     * @return <i>N</i>x<i>N</i> fundamental matrix and {@link com.net2plan.utils.Constants.RoutingCycleType RoutingCycleType}
     */
    private static Pair<DoubleMatrix2D, RoutingCycleType> computeM_fde(Demand demand, List<Node> nodes, List<Link> links, DoubleMatrix2D f_de) {
        int N = nodes.size();
        DenseDoubleAlgebra algebra = new DenseDoubleAlgebra();

		/* Compute the I-Q matrix */
        DoubleMatrix2D IminusQ = DoubleFactory2D.dense.identity(N);
        IntArrayList e_thisDemand = new IntArrayList();
        DoubleArrayList f_de_thisDemand = new DoubleArrayList();
        f_de.viewRow(demand.getIndex()).getNonZeros(e_thisDemand, f_de_thisDemand);

        int elements = e_thisDemand.size();
        for (int i = 0; i < elements; i++) {
            int e = e_thisDemand.get(i);
            int a_e = links.get(e).getOriginNode().getIndex();
            int b_e = links.get(e).getDestinationNode().getIndex();
            IminusQ.set(a_e, b_e, IminusQ.get(a_e, b_e) - f_de_thisDemand.getQuick(i));
        }

		/* Compute the chain fundamental matrix M (IllegalArgumentException is catched if det(IminusQ) == 0 */
        DoubleMatrix2D M;
        try {
            M = algebra.inverse(IminusQ);
        } catch (IllegalArgumentException e) {
//			System.out.println("** Closed loop for demand: " + demand + " (" + demand.getIngressNode() + " to " + demand.getEgressNode());
//			System.out.println("**   f_de: " + f_de.viewRow(demand.getIndex()));
            return Pair.of(null, RoutingCycleType.CLOSED_CYCLES);
//			throw new ClosedCycleRoutingException("Closed routing cycle for demand " + demand);
        }

        for (int contN = 0; contN < N; contN++)
            if (Math.abs(M.get(contN, contN) - 1) > 1e-5) return Pair.of(M, RoutingCycleType.OPEN_CYCLES);

        return Pair.of(M, RoutingCycleType.LOOPLESS);
    }

    /**
     * Applies a correction to a given airline distance (i.e. Euclidean distance, Haversine distance...) considering cable (i.e. optical fiber) deployment issues to predict a more realistic cable length. For undersea cables, the airline distance is considered as valid.
     *
     * @param airlineDistanceInKm Airline distance in km (i.e. Euclidean distance, Haversine distance...)
     * @return Cable length in km
     * @see <a href="http://www.etsi.org/deliver/etsi_en/300400_300499/300416/01.02.01_60/en_300416v010201p.pdf">ETSI, "Network Aspects (NA): Availability performance of path elements of international digital paths," European Standard EN 300 416 V1.2.1, August 1998.</a>
     */
    public static double computeRoadDistanceInKm(double airlineDistanceInKm) {
        if (airlineDistanceInKm < 0)
            throw new Net2PlanException("Bad - Airline distance must be greater or equal than zero");
        else if (airlineDistanceInKm < 1000)
            return 1.5 * airlineDistanceInKm;
        else if (airlineDistanceInKm < 1200)
            return 1500;
        else
            return 1.25 * airlineDistanceInKm;
    }

    /**
     * Returns the total cost for a given path, which is equal to the sum of the cost of each traversed link.
     *
     * @param seqLinks    Sequence of traversed links
     * @param linkCostMap Cost per link array
     * @return Cost of the path
     */
    public static double convertPath2PathCost(List<Link> seqLinks, DoubleMatrix1D linkCostMap) {
        if (linkCostMap == null) return seqLinks.size();
        double pathCost = 0;
        for (Link e : seqLinks)
            pathCost += linkCostMap.get(e.getIndex());
        return pathCost;
    }

    /**
     * Returns the total cost for a given a list of paths.
     *
     * @param pathList    List of paths, where each path is represented by its sequence of traversed links
     * @param linkCostMap Cost per link array
     * @return Cost per path
     */
    public static List<Double> convertPathList2PathCost(List<List<Link>> pathList, DoubleMatrix1D linkCostMap) {
        List<Double> pathCost = new LinkedList<Double>();

        ListIterator<List<Link>> it = pathList.listIterator();
        while (it.hasNext()) {
            List<Link> path = it.next();
            pathCost.add(convertPath2PathCost(path, linkCostMap));
        }

        return pathCost;
    }

    /**
     * Converts a given sequence of links to the corresponding sequence of nodes.
     *
     * @param seqLinks Sequence of traversed links
     * @return Sequence of nodes
     */
    public static List<Node> convertSequenceOfLinksToSequenceOfNodes(List<Link> seqLinks) {
        if (seqLinks.isEmpty()) throw new Net2PlanException("No path");
        List<Node> sequenceOfNodes = new LinkedList<Node>();
        Node lastVisitedNode = seqLinks.iterator().next().getOriginNode();
        sequenceOfNodes.add(lastVisitedNode);
        for (Link e : seqLinks) {
            if (!e.getOriginNode().equals(lastVisitedNode)) throw new Net2PlanException("Invalid sequence of links");
            sequenceOfNodes.add(e.getDestinationNode());
            lastVisitedNode = e.getDestinationNode();
        }
        return sequenceOfNodes;
    }

    /**
     * Given a forwarding rule mapping (fractions of traffic entering a node from demand 'd', leaving that node through link 'e'), and an offered traffic to the network,
     * it generates the resulting demand-link routing in the form x_de (amount of traffic from demand d, transmitted through link e).
     * When a demand has a closed routing loop, the traffic in the link would be infinite. Then, a {@code ClosedCycleRoutingException} is thrown
     *
     * @param nodes   List of nodes
     * @param links   List of links
     * @param demands List of demands
     * @param h_d     The amount of traffic offered for each demand
     * @param f_de    For each demand <i>d</i> (<i>d = 0</i> refers to the first demand index, <i>d = 1</i> refers to the second one, and so on), and each link <i>e</i> (<i>e = 0</i> refers to the first link index, <i>e = 1</i> refers to the second one, and so on), {@code f_de[d][e]} sets the fraction of the traffic from demand <i>d</i> that arrives (or is generated in) node <i>a(e)</i> (the origin node of link <i>e</i>), that is forwarded through link <i>e</i>. It must hold that for every node <i>n</i> different of <i>b(d)</i>, the sum of the fractions <i>f<sub>de</sub></i> along its outgoing links must be lower or equal than 1 (unchecked)
     * @return Demand-link routing in the form x_de (amount of traffic from demand d, transmitted through link e), total carried traffic per demand, carried traffic per link, and routing cycle type (loopless, with open cycles...) per demand
     */
    public static Quadruple<DoubleMatrix2D, DoubleMatrix1D, DoubleMatrix1D, List<RoutingCycleType>> convert_fde2xde(List<Node> nodes, List<Link> links, List<Demand> demands, DoubleMatrix1D h_d, DoubleMatrix2D f_de) {
        int E = links.size();
        int D = demands.size();

        DoubleMatrix2D x_de = DoubleFactory2D.sparse.make(D, E);
        DoubleMatrix1D r_d = DoubleFactory1D.dense.make(D);
        DoubleMatrix1D y_e = DoubleFactory1D.dense.make(E);
        List<RoutingCycleType> routingCycleType = new ArrayList<RoutingCycleType>(D);
        double PRECISION_FACTOR = Double.parseDouble(Configuration.getOption("precisionFactor"));

		/* For each demand 'd', detect if the routing is cycleless and well defined */
        for (Demand demand : demands) {
            Pair<DoubleMatrix2D, RoutingCycleType> out = computeM_fde(demand, nodes, links, f_de);
            DoubleMatrix2D M = out.getFirst();
            routingCycleType.add(out.getSecond());
            if (M == null) throw new ClosedCycleRoutingException("Closed routing cycle for demand " + demand);


            final double h_d_thisDemand = h_d.get(demand.getIndex());
            final int a_d = demand.getIngressNode().getIndex();
            final int b_d = demand.getEgressNode().getIndex();
            final int d = demand.getIndex();
            for (Link link : links) {
                int e = link.getIndex();
                int a_e = link.getOriginNode().getIndex();
                x_de.setQuick(d, link.getIndex(), M == null ? 0 : h_d_thisDemand * M.getQuick(a_d, a_e) * f_de.getQuick(d, e));
                if (x_de.getQuick(d, e) < PRECISION_FACTOR) x_de.setQuick(d, e, 0);
                y_e.set(e, y_e.get(e) + x_de.getQuick(d, e));
            }

            r_d.set(d, M == null ? 0 : h_d_thisDemand * M.getQuick(a_d, b_d));
            if (r_d.get(d) < PRECISION_FACTOR) r_d.set(d, 0);
        }

        return Quadruple.of(x_de, r_d, y_e, routingCycleType);
    }

    /**
     * Given a destination-based routing in the form f_te (fractions of traffic in a node, that is forwarded through each of its output links), it generates the resulting demand-link routing in the form f_de (fractions of traffic in a node from demand d, transmitted through link e).
     *
     * @param demands List of demands
     * @param f_te    For each destination node <i>t</i> (<i>t = 0</i> refers to the first node index, <i>t = 1</i> refers to the second one, and so on), and each link <i>e</i> (<i>e = 0</i> refers to the first link index, <i>e = 1</i> refers to the second one, and so on), {@code f_te[t][e]} sets the fraction of the traffic targeted to node <i>t</i> that arrives (or is generated in) node <i>a(e)</i> (the origin node of link <i>e</i>), that is forwarded through link <i>e</i>. It must hold that for every node <i>n</i> different of <i>t</i>, the sum of the fractions <i>f<sub>te</sub></i> along its outgoing links must be lower or equal than 1 (unchecked)
     * @return Demand-link routing in the form f_de (fractions of traffic in a node from demand d, transmitted through link e)
     */
    public static DoubleMatrix2D convert_fte2fde(List<Demand> demands, DoubleMatrix2D f_te) {
        int E = f_te.columns();
        int D = demands.size();
        DoubleMatrix2D f_de = DoubleFactory2D.sparse.make(D, E);
        for (Demand d : demands)
            f_de.viewRow(d.getIndex()).assign(f_te.viewRow(d.getEgressNode().getIndex()));
        return f_de;
    }

    /**
     * Given a destination-based routing in the form f_te (fractions of traffic in a node, that is forwarded through each of its output links), and an offered traffic to the network, it generates the resulting demand-link routing in the form x_de (amount of traffic from demand d, transmitted through link e).
     * If the routing of a demand has closed loops a {@code ClosedCycleRoutingException} is thrown
     *
     * @param nodes   List of nodes
     * @param links   List of links
     * @param demands List of demands
     * @param f_te    For each destination node <i>t</i> and each link <i>e</i>, {@code f_te[t][e]} sets the fraction of the traffic targeted to node <i>t</i> that arrives (or is generated in) node <i>a(e)</i> (the origin node of link <i>e</i>), that is forwarded through link <i>e</i>. It must hold that for every node <i>n</i> different of <i>t</i>, the sum of the fractions <i>f<sub>te</sub></i> along its outgoing links must be lower or equal than 1 (unchecked)
     * @return Demand-link routing in the form x_de (amount of traffic from demand d, transmitted through link e), total carried traffic per demand, carried traffic per link, and routing cycle type (loopless, with open cycles...) per demand
     */
    public static Quadruple<DoubleMatrix2D, DoubleMatrix1D, DoubleMatrix1D, List<RoutingCycleType>> convert_fte2xde(List<Node> nodes, List<Link> links, List<Demand> demands, DoubleMatrix2D f_te) {
        final DoubleMatrix2D f_de = convert_fte2fde(demands, f_te);
        final int D = demands.size();
        DoubleMatrix1D h_d = DoubleFactory1D.dense.make(D);
        for (int d = 0; d < D; d++)
            h_d.set(d, demands.get(d).getOfferedTraffic());
        return convert_fde2xde(nodes, links, demands, h_d, f_de);
    }

    /**
     * Given a destination-based routing in the form f_te (fractions of traffic in a node, that is forwarded through each of its
     * output links), and an offered traffic to the network, it generates the resulting set of paths that are produced.
     * In the routing has open cycles, they are removed from the routing so the traffic of that demand uses the same or less
     * capacity in the traversed links. If the routing has closed cycles, a {@code ClosedCycleRoutingException} is thrown. If the
     * routing has open cycles, they are removed from the routes, so that for every demand, the new routing uses the same or less
     * bandwidth in the traversed links.
     *
     * @param nodes    List of nodes {@link com.net2plan.interfaces.networkDesign.NetPlan Net2Plan} object
     * @param links    List of links
     * @param demands  List of demands
     * @param h_d      The amount of traffic offered for each demand
     * @param f_te     For each destination node <i>t</i> (<i>t = 0</i> refers to the first node in {@code nodeIds}, <i>t = 1</i> refers to the second one, and so on), and each link <i>e</i> (<i>e = 0</i> refers to the first link in {@code linkMap.keySet()}, <i>e = 1</i> refers to the second one, and so on), {@code f_te[t][e]} sets the fraction of the traffic targeted to node <i>t</i> that arrives (or is generated in) node <i>a(e)</i> (the origin node of link <i>e</i>), that is forwarded through link <i>e</i>. It must hold that for every node <i>n</i> different of <i>t</i>, the sum of the fractions <i>f<sub>te</sub></i> along its outgoing links must be lower or equal than 1 (unchecked)
     * @param d_p      (Output parameter) Demand corresponding to each path. User should pass a initialized {@code List} object (i.e. {@code LinkedList} or {@code ArrayList})
     * @param x_p      (Output parameter) Carried traffic per path. User should pass a initialized {@code List} object (i.e. {@code LinkedList} or {@code ArrayList})
     * @param pathList (Output parameter) List of paths, where each path is represented by its sequence of traversed links. User should pass a initialized {@code List} object (i.e. {@code LinkedList} or {@code ArrayList})
     * @since 0.3.1
     */
    public static void convert_fte2xp(List<Node> nodes, List<Link> links, List<Demand> demands, DoubleMatrix1D h_d, DoubleMatrix2D f_te, List<Demand> d_p, List<Double> x_p, List<List<Link>> pathList) {
        Quadruple<DoubleMatrix2D, DoubleMatrix1D, DoubleMatrix1D, List<RoutingCycleType>> res = GraphUtils.convert_fte2xde(nodes, links, demands, f_te);
//		for (RoutingCycleType routingCycleType : res.getFourth())
//		{
//			//if (routingCycleType == RoutingCycleType.OPEN_CYCLES) throw new Net2PlanException("There are some open cycles");
//			//if (routingCycleType == RoutingCycleType.CLOSED_CYCLES) throw new Net2PlanException("There are some closed cycles");
//		}
        GraphUtils.convert_xde2xp(nodes, links, demands, res.getFirst(), d_p, x_p, pathList);
    }

    /**
     * Given a destination-based routing in the form f_te (fractions of traffic in a node, that is forwarded through each of its output links), and an offered traffic to the network, it generates the resulting destination-based routing in the form x_te (amount of traffic targeted to node t, transmitted through link e).
     * If the routing has closed loops a {@code ClosedCycleRoutingException} is thrown
     *
     * @param nodes   List of nodes
     * @param links   List of links
     * @param demands List of demands
     * @param h_d     The amount of traffic offered for each demand
     * @param f_te    For each destination node <i>t</i> (<i>t = 0</i> refers to the first node index, <i>t = 1</i> refers to the second one, and so on), and each link <i>e</i> (<i>e = 0</i> refers to the first link index, <i>e = 1</i> refers to the second one, and so on), {@code f_te[t][e]} sets the fraction of the traffic targeted to node <i>t</i> that arrives (or is generated in) node <i>a(e)</i> (the origin node of link <i>e</i>), that is forwarded through link <i>e</i>. It must hold that for every node <i>n</i> different of <i>t</i>, the sum of the fractions <i>f<sub>te</sub></i> along its outgoing links must be lower or equal than 1 (unchecked)
     * @return Destination-based routing in the form <i>f<sub>te</sub></i> (fractions of traffic in a node, that is forwarded through each of its output links)
     */
    public static DoubleMatrix2D convert_fte2xte(List<Node> nodes, List<Link> links, List<Demand> demands, DoubleMatrix2D f_te, DoubleMatrix1D h_d) {
        final DoubleMatrix2D f_de = convert_fte2fde(demands, f_te);
        final Quadruple<DoubleMatrix2D, DoubleMatrix1D, DoubleMatrix1D, List<RoutingCycleType>> res = convert_fde2xde(nodes, links, demands, h_d, f_de);
        return convert_xde2xte(nodes, links, demands, res.getFirst());
    }

    /**
     * Given a demand-link routing in the form x_de (amount of traffic from demand d, transmitted through link e), returns the equivalent forwarding rule mapping (fractions of traffic entering a node from demand 'd', leaving that node through link 'e').
     *
     * @param nodes   List of nodes
     * @param links   List of links
     * @param demands List of demands
     * @param x_de    Demand-link routing in the form x_de (amount of traffic from demand d, transmitted through link e)
     * @return Forwarding rule mapping (fractions of traffic entering a node from demand 'd', leaving that node through link 'e')
     */
    public static DoubleMatrix2D convert_xde2fde(List<Node> nodes, List<Link> links, List<Demand> demands, DoubleMatrix2D x_de) {
        final double PRECISION_FACTOR = Double.parseDouble(Configuration.getOption("precisionFactor"));
        final int D = demands.size();
        final int E = links.size();
        Graph<Node, Link> graph = JUNGUtils.getGraphFromLinkMap(nodes, links);
        DoubleMatrix2D f_de = DoubleFactory2D.sparse.make(D, E);

		/* For each node 'n', where we are filling the routing table */
        for (Node node : nodes) {
            /* Compute the outgoing links from node n */
            Collection<Link> outLinks = graph.getOutEdges(node);
            if (outLinks == null) outLinks = new LinkedHashSet<Link>();

			/* For each demand 'd', fill the f_de fractions */
            for (Demand demand : demands) {
                int d = demand.getIndex();
                double outTraffic = 0;
                for (Link link : outLinks)
                    outTraffic += x_de.get(demand.getIndex(), link.getIndex());

                if (outTraffic > PRECISION_FACTOR) /* there is traffic leaving the node */ {
                    for (Link link : outLinks)
                        f_de.set(d, link.getIndex(), x_de.get(d, link.getIndex()) / outTraffic);
                }
            }
        }

        return f_de;
    }

    /**
     * Given a demand-link routing in the form x_de (amount of traffic from demand d, transmitted through link e), returns the equivalent forwarding rule mapping (fractions of traffic entering a node from demand 'd', leaving that node through link 'e').
     *
     * @param nodes   List of nodes
     * @param links   List of links
     * @param demands List of demands
     * @param x_de    Demand-link routing in the form x_de (amount of traffic from demand d, transmitted through link e)
     * @return Forwarding rule matrix (a <i>N</i>x<i>E</i> matrix where each element <i>&delta;<sub>ne</sub></i> equals the fraction of traffic entering a node 'n' from demand 'd', leaving that node through link 'e')
     */
    public static DoubleMatrix2D convert_xde2xte(List<Node> nodes, List<Link> links, List<Demand> demands, DoubleMatrix2D x_de) {
        final int N = nodes.size();
        final int D = demands.size();
        final int E = links.size();

        DoubleMatrix2D x_te = DoubleFactory2D.sparse.make(N, E);
        for (Demand demand : demands) {
            final int t = demand.getEgressNode().getIndex();
            final int d = demand.getIndex();
            DoubleMatrix1D x_e_thisDemand = x_de.viewRow(d);
            x_te.viewRow(t).assign(x_e_thisDemand, DoublePlusMultFirst.plusMult(1));
        }
        return x_te;
    }


    /**
     * Removes open or closed cycles from the x_de routing matrix, for the given set of demands. The x_de matrix can be in the form
     * of traffic in each link, or fraction of traffic respect to demand offered traffic, carried in each link. The cycles are removed
     * guaranteeing that the new routing has the same or less traffic of each demand in each link
     * This is specified in the xdeAsFractionRespecttoDemandOfferedTraffic parameter.
     *
     * @param nodes                                      List of nodes
     * @param links                                      List of links
     * @param demands                                    List of demands to which I want to remove the loops
     * @param x_de                                       Demand-link routing matrix, with one row per demnad, and one column per link. Contains the traffic of demand d in link e, or the fraction of the traffic respect to the demand offered traffic
     * @param xdeAsFractionRespecttoDemandOfferedTraffic true is the matrix is in the fractional form, false otherwise
     * @param solverName                                 the name of the solver to call for the internal formulation of the algorithm
     * @param solverLibraryName                          the solver library name
     * @param maxSolverTimeInSecondsPerDemand            the maximum time the solver is allowed for each of the internal formulations (one for each demand).
     * @return The new x_de matrix )
     */
    public static DoubleMatrix2D removeCyclesFrom_xde(List<Node> nodes, List<Link> links, List<Demand> demands, DoubleMatrix2D x_de, boolean xdeAsFractionRespecttoDemandOfferedTraffic, String solverName, String solverLibraryName, double maxSolverTimeInSecondsPerDemand) {
        DoubleMatrix2D newXde = x_de.copy();
        final int E = x_de.columns();
        final int N = nodes.size();
        DoubleMatrix2D A_ne = DoubleFactory2D.sparse.make(N, E);
        for (Link e : links) {
            A_ne.set(e.getOriginNode().getIndex(), e.getIndex(), 1);
            A_ne.set(e.getDestinationNode().getIndex(), e.getIndex(), -1);
        }
        for (Demand d : demands) {
            DoubleMatrix1D div = DoubleFactory1D.dense.make(N);
            div.set(d.getIngressNode().getIndex(), xdeAsFractionRespecttoDemandOfferedTraffic ? 1.0 : d.getOfferedTraffic());
            div.set(d.getEgressNode().getIndex(), xdeAsFractionRespecttoDemandOfferedTraffic ? -1.0 : -d.getOfferedTraffic());
            OptimizationProblem op = new OptimizationProblem();
            op.setInputParameter("A_ne", A_ne);
            op.setInputParameter("div", div, "column");
            op.addDecisionVariable("x_e", false, new int[]{E, 1}, new double[E], x_de.viewRow(d.getIndex()).copy().assign(DoubleFunctions.max(0)).toArray());
            op.setObjectiveFunction("minimize", "sum(x_e)");
            op.addConstraint("A_ne * x_e == div");
            if (solverLibraryName == null)
                op.solve(solverName, "maxSolverTimeInSeconds", (Double) maxSolverTimeInSecondsPerDemand);
            else
                op.solve(solverName, "solverLibraryName", solverLibraryName, "maxSolverTimeInSeconds", (Double) maxSolverTimeInSecondsPerDemand);
            if (!op.solutionIsFeasible()) throw new Net2PlanException("A feasible solution was not found");
            newXde.viewRow(d.getIndex()).assign(op.getPrimalSolution("x_e").view1D());
        }
        return newXde;
    }

    /**
     * Removes open or closed cycles from the x_de routing matrix, for the given set of demands. The x_de matrix can be in the form
     * of traffic in each link, or fraction of traffic respect to demand offered traffic, carried in each link. The cycles are removed
     * guaranteeing that the new routing has the same or less traffic of each demand in each link
     * This is specified in the xdeAsFractionRespecttoDemandOfferedTraffic parameter.
     *
     * @param nodes                                      List of nodes
     * @param links                                      List of links
     * @param demands                                    List of demands to which I want to remove the loops
     * @param x_de                                       Demand-link routing matrix, with one row per demnad, and one column per link. Contains the traffic of demand d in link e, or the fraction of the traffic respect to the demand offered traffic
     * @param xdeAsFractionRespecttoDemandOfferedTraffic true is the matrix is in the fractional form, false otherwise
     * @param solverName                                 the name of the solver to call for the internal formulation of the algorithm
     * @param solverLibraryName                          the solver library name
     * @param maxSolverTimeInSecondsPerDemand            the maximum time the solver is allowed for each of the internal formulations (one for each demand).
     * @return The new x_de matrix )
     */
    public static DoubleMatrix2D removeCyclesFrom_xte(List<Node> nodes, List<Link> links, DoubleMatrix2D trafficMatrix, DoubleMatrix2D x_te, String solverName, String solverLibraryName, double maxSolverTimeInSecondsPerDestination) {
        DoubleMatrix2D newXte = x_te.copy();
        final int E = x_te.columns();
        final int N = nodes.size();
        DoubleMatrix2D A_ne = DoubleFactory2D.sparse.make(N, E);
        for (Link e : links) {
            A_ne.set(e.getOriginNode().getIndex(), e.getIndex(), 1);
            A_ne.set(e.getDestinationNode().getIndex(), e.getIndex(), -1);
        }
        for (Node t : nodes) {
            DoubleMatrix1D div = trafficMatrix.viewColumn(t.getIndex()).copy();
            div.set(t.getIndex(), -div.zSum());
            OptimizationProblem op = new OptimizationProblem();
            op.setInputParameter("A_ne", A_ne);
            op.setInputParameter("div", div, "column");
            op.addDecisionVariable("x_e", false, new int[]{E, 1}, new double[E], x_te.viewRow(t.getIndex()).copy().assign(DoubleFunctions.max(0)).toArray());
            op.setObjectiveFunction("minimize", "sum(x_e)");
            op.addConstraint("A_ne * x_e == div");
            if (solverLibraryName == null)
                op.solve(solverName, "maxSolverTimeInSeconds", (Double) maxSolverTimeInSecondsPerDestination);
            else
                op.solve(solverName, "solverLibraryName", solverLibraryName, "maxSolverTimeInSeconds", (Double) maxSolverTimeInSecondsPerDestination);
            if (!op.solutionIsFeasible()) throw new Net2PlanException("A feasible solution was not found");
            newXte.viewRow(t.getIndex()).assign(op.getPrimalSolution("x_e").view1D());
        }
        return newXte;
    }


    /**
     * Converts the routing in the form x_de into a set of loopless routes. If the x_de matrix has open or closed loops, they are removed
     * from the routing. The resulting routing (after removing the cycles if any) of each demand uses the same of less bandwidth than the original routing.
     *
     * @param nodes    List of nodes
     * @param links    List of links
     * @param demands  List of demands
     * @param x_de     Demand-link routing in the form x_de (amount of traffic from demand d, transmitted through link e)
     * @param d_p      (Output parameter) Demand corresponding to each path. User should pass a initialized {@code List} object (i.e. {@code LinkedList} or {@code ArrayList})
     * @param x_p      (Output parameter) Carried traffic per path. User should pass a initialized {@code List} object (i.e. {@code LinkedList} or {@code ArrayList})
     * @param pathList (Output parameter) List of paths, where each path is represented by its sequence of traversed links. User should pass a initialized {@code List} object (i.e. {@code LinkedList} or {@code ArrayList})
     * @return Number of new paths
     */
    public static int convert_xde2xp(List<Node> nodes, List<Link> links, List<Demand> demands, DoubleMatrix2D x_de, List<Demand> d_p, List<Double> x_p, List<List<Link>> pathList) {
        double PRECISION_FACTOR = Double.parseDouble(Configuration.getOption("precisionFactor"));
        final int E = links.size();

        int numPaths = 0;
        Graph<Node, Link> graph = JUNGUtils.getGraphFromLinkMap(nodes, links);
        Transformer<Link, Double> nev = JUNGUtils.getEdgeWeightTransformer(null);
        for (Demand demand : demands) {
            Node ingressNode = demand.getIngressNode();
            Node egressNode = demand.getEgressNode();
            DoubleMatrix1D x_e = x_de.viewRow(demand.getIndex()).copy();

            Collection<Link> incomingLinksToIngressNode = graph.getInEdges(ingressNode);
            Collection<Link> outgoingLinksFromIngressNode = graph.getOutEdges(ingressNode);
            if (incomingLinksToIngressNode == null) incomingLinksToIngressNode = new LinkedHashSet<Link>();
            if (outgoingLinksFromIngressNode == null) outgoingLinksFromIngressNode = new LinkedHashSet<Link>();

            double divAtIngressNode = 0;
            for (Link link : outgoingLinksFromIngressNode)
                divAtIngressNode += x_e.get(link.getIndex());

            for (Link link : incomingLinksToIngressNode)
                divAtIngressNode -= x_e.get(link.getIndex());

            while (divAtIngressNode > PRECISION_FACTOR) {
                List<Link> candidateLinks = new LinkedList<Link>();
                for (int e = 0; e < E; e++)
                    if (x_e.get(e) > 0) candidateLinks.add(links.get(e));
                if (candidateLinks.isEmpty()) break;

                Graph<Node, Link> auxGraph = JUNGUtils.filterGraph(graph, null, null, candidateLinks, null);
                List<Link> seqLinks = JUNGUtils.getShortestPath(auxGraph, nev, ingressNode, egressNode);
                if (seqLinks.isEmpty()) break;

                double trafficInPath = Double.MAX_VALUE;
                for (Link link : seqLinks)
                    trafficInPath = Math.min(trafficInPath, x_e.get(link.getIndex()));

                trafficInPath = Math.min(trafficInPath, divAtIngressNode);
                divAtIngressNode -= trafficInPath;

                x_p.add(trafficInPath);
                d_p.add(demand);
                pathList.add(seqLinks);
                for (Link link : seqLinks) {
                    double newTrafficValue_thisLink = x_e.get(link.getIndex()) - trafficInPath;
                    if (newTrafficValue_thisLink < PRECISION_FACTOR)
                        x_e.set(link.getIndex(), 0);
                    else
                        x_e.set(link.getIndex(), newTrafficValue_thisLink);
                }

                numPaths++;

                if (divAtIngressNode <= PRECISION_FACTOR) break;
            }
        }

        return numPaths;
    }

    /**
     * Returns the carried traffic per link.
     *
     * @param x_de Demand-link routing in the form x_de (amount of traffic from demand d, transmitted through link e)
     * @return Carried traffic per link
     */
    public static DoubleMatrix1D convert_xde2ye(DoubleMatrix2D x_de) {
        return x_de.zMult(DoubleFactory1D.dense.make(x_de.columns(), 1), null);
    }

    /**
     * Given a set of traffic routes and their carried traffic returns a destination-based routing in the form <i>f<sub>te</sub></i> (fractions of traffic in a node, that is forwarded through each of its output links).
     *
     * @param nodes   List of nodes
     * @param links   List of links
     * @param demands List of demands
     * @param routes  List of routes
     * @return Destination-based routing in the form <i>f<sub>te</sub></i> (fractions of traffic in a node, that is forwarded through each of its output links)
     */
    public static DoubleMatrix2D convert_xp2fte(List<Node> nodes, List<Link> links, List<Demand> demands, List<Route> routes) {
        DoubleMatrix2D x_te = convert_xp2xte(nodes, links, demands, routes);
        return convert_xte2fte(nodes, links, x_te);
    }

    /**
     * Given a set of traffic routes and their carried traffic returns a destination-based routing in the form x_te (amount of traffic targeted to node t, transmitted through link e).
     *
     * @param nodes   List of nodes
     * @param links   List of links
     * @param demands List of demands
     * @param routes  List of routes
     * @return Destination-based routing in the form x_te (amount of traffic targeted to node t, transmitted through link e)
     */
    public static DoubleMatrix2D convert_xp2xte(List<Node> nodes, List<Link> links, List<Demand> demands, List<Route> routes) {
        int E = links.size();
        int N = nodes.size();
        DoubleMatrix2D x_te = DoubleFactory2D.sparse.make(N, E, 0);

        for (Route route : routes) {
            final int t = route.getEgressNode().getIndex();
            for (Link link : route.getSeqLinksRealPath()) {
                final int e = link.getIndex();
                x_te.setQuick(t, e, x_te.getQuick(t, e) + route.getCarriedTraffic());
            }
        }
        return x_te;
    }

    /**
     * Returns the carried traffic per link.
     *
     * @param links  List of links
     * @param routes List of routes
     * @return Carried traffic per link
     */
    public static DoubleMatrix1D convert_xp2ye(List<Link> links, List<Route> routes) {
        final int E = links.size();
        DoubleMatrix1D y_e = DoubleFactory1D.dense.make(E);
        for (Route r : routes)
            for (Link e : r.getSeqLinksRealPath())
                y_e.set(e.getIndex(), y_e.get(e.getIndex()) + r.getCarriedTraffic());
        return y_e;
    }

    /**
     * Given a destination-based routing in the form of an array x_te (amount of traffic targeted to node t, transmitted through link e), it returns the associated destination-based routing in the form of fractions f_te (fraction of the traffic targeted to node t that arrives (or is generated in) node a(e) (the initial node of link e), that is forwarded through link e). If a node n does not forward any traffic to a destination t, it is not possible to determine the f_te fractions for the output links of the node. Then, the function arbitrarily assumes that the 100% of the traffic to node t, would be forwarded through the shortest path (in number of hops) from n to t. Finally note that for every destination t, f_te = 0 for all the links e outgoing of node t.
     *
     * @param nodes List of nodes
     * @param links List of links
     * @param x_te  For each destination node <i>t</i> (<i>t = 0</i> refers to the first node in {@code nodeIds}, <i>t = 1</i> refers to the second one, and so on), and each link <i>e</i> (<i>e = 0</i> refers to the first link in {@code linkMap.keySet()}, <i>e = 1</i> refers to the second one, and so on), {@code f_te[t][e]} represents the traffic targeted to node <i>t</i> that arrives (or is generated in) node <i>a(e)</i> (the origin node of link <i>e</i>), that is forwarded through link <i>e</i>
     * @return The f_te matrix created
     */
    public static DoubleMatrix2D convert_xte2fte(List<Node> nodes, List<Link> links, DoubleMatrix2D x_te) {
        final int N = nodes.size();
        final int E = links.size();
        DoubleMatrix2D A_tn = x_te.zMult(GraphUtils.getOutgoingIncidenceMatrix(nodes, links), null, 1, 0, false, true); // traffic of demand d that leaves node n
        DoubleMatrix2D f_te = DoubleFactory2D.sparse.make(N, E);
        IntArrayList ts = new IntArrayList();
        IntArrayList es = new IntArrayList();
        DoubleArrayList trafs = new DoubleArrayList();
        x_te.getNonZeros(ts, es, trafs);
        for (int cont = 0; cont < ts.size(); cont++) {
            final int t = ts.get(cont);
            final int e = es.get(cont);
            double outTraf = A_tn.get(t, links.get(e).getOriginNode().getIndex());
            f_te.set(t, e, outTraf == 0 ? 0 : trafs.get(cont) / outTraf);
        }
        return f_te;
    }

    /**
     * Given a path-based routing, returns the amount of traffic for each demand d traversing each link e.
     *
     * @param links   List of links
     * @param demands List of demands
     * @param routes  List of rutes
     * @return Demand-link routing in the form x_de (amount of traffic from demand d, transmitted through link e)
     */
    public static DoubleMatrix2D convert_xp2xde(List<Link> links, List<Demand> demands, List<Route> routes) {
        int E = links.size();
        int D = demands.size();
        DoubleMatrix2D x_de = DoubleFactory2D.sparse.make(D, E, 0);

        for (Route route : routes) {
            final int d = route.getDemand().getIndex();
            for (Link link : route.getSeqLinksRealPath()) {
                final int e = link.getIndex();
                x_de.setQuick(d, e, x_de.getQuick(d, e) + route.getCarriedTraffic());
            }
        }
        return x_de;
    }

    /**
     * Returns the carried traffic per link.
     *
     * @param x_te For each destination node <i>t</i> (<i>t = 0</i> refers to the first node in {@code nodeIds}, <i>t = 1</i> refers to the second one, and so on), and each link <i>e</i> (<i>e = 0</i> refers to the first link in {@code linkMap.keySet()}, <i>e = 1</i> refers to the second one, and so on), {@code f_te[t][e]} represents the traffic targeted to node <i>t</i> that arrives (or is generated in) node <i>a(e)</i> (the origin node of link <i>e</i>), that is forwarded through link <i>e</i>
     * @return Carried traffic per link
     */
    public static DoubleMatrix1D convert_xte2ye(DoubleMatrix2D x_te) {
        return convert_xde2ye(x_te);
    }

    /**
     * Returns all the loopless shortest paths between two nodes. All these paths have the same total cost.
     *
     * @param nodes           List of nodes
     * @param links           List of links
     * @param originNode      Origin node
     * @param destinationNode Destination node
     * @param linkCostMap     Cost per link, where the key is the link identifier and the value is the cost of traversing the link. No special iteration-order (i.e. ascending) is required
     * @return All loopless shortest paths
     */
    public static List<List<Link>> getAllLooplessShortestPaths(List<Node> nodes, List<Link> links, Node originNode, Node destinationNode, Map<Link, Double> linkCostMap) {
        Graph<Node, Link> g = JUNGUtils.getGraphFromLinkMap(nodes, links);
        Transformer<Link, Double> nev = JUNGUtils.getEdgeWeightTransformer(linkCostMap);
        g = JUNGUtils.filterGraph(g, nev);

        YenAlgorithm<Node, Link> paths = new YenAlgorithm<Node, Link>(g, nev) {
            @Override
            public boolean compareCandidateToShortestPath(GraphPath<Link> candidate, GraphPath<Link> shortestPath) {
                return Math.abs(candidate.getPathWeight() - shortestPath.getPathWeight()) < 1E-10;
            }
        };

        List<List<Link>> pathList = paths.getPaths(originNode, destinationNode, Integer.MAX_VALUE);
        return pathList;
    }

    /**
     * @param nodes                List of nodes
     * @param links                List of links
     * @param originNode           Origin node
     * @param destinationNode      Destination node
     * @param linkCostMap          Cost per link, where the key is the link identifier and the value is the cost of traversing the link. No special iteration-order (i.e. ascending) is required. If <code>null</code> all the links have a weight of one.
     * @param linkSpareCapacityMap Current available capacity per link, where the key is the link identifier and the value is the cost of traversing the link. No special iteration-order (i.e. ascending) is required. If <code>null</code> the spare capacity of each link is its capacity minus its carried traffic (summing the regular traffic and the ones in the protection segments), truncated to zero (negative values are not admitted).
     * @param capacityGoal         Minimum capacity required
     * @return Shortest path fulfilling a minimum capacity requirement
     */
    public static List<Link> getCapacitatedShortestPath(Collection<Node> nodes, Collection<Link> links, Node originNode, Node destinationNode, final Map<Link, Double> linkCostMap, final Map<Link, Double> linkSpareCapacityMap, final double capacityGoal) {
        Graph<Node, Link> graph = JUNGUtils.getGraphFromLinkMap(nodes, links);
        if (!graph.containsVertex(originNode)) return new LinkedList<Link>();
        if (!graph.containsVertex(destinationNode)) return new LinkedList<Link>();
        Transformer<Link, Double> nev = JUNGUtils.getEdgeWeightTransformer(linkCostMap);
        Transformer<Link, Double> capacityTransformer;
        if (linkSpareCapacityMap == null) {
            final Map<Link, Double> linkSpareCapacityMapToUse = new HashMap<Link, Double>();
            for (Link e : links)
                linkSpareCapacityMapToUse.put(e, Math.max(0, e.getCapacity() - e.getCarriedTrafficIncludingProtectionSegments()));
            capacityTransformer = JUNGUtils.getEdgeWeightTransformer(linkSpareCapacityMapToUse);
        } else
            capacityTransformer = JUNGUtils.getEdgeWeightTransformer(linkSpareCapacityMap);
        return JUNGUtils.getCapacitatedShortestPath(graph, nev, originNode, destinationNode, capacityTransformer, capacityGoal);
    }

    /** Returns the shortest path that fulfills a given minimum capacity requirement along its traversed links. In case no path can be found, an empty list will be returned.
     * @param nodes Collection of nodes
     * @param linkMap Map of links, where the key is the unique link identifier and the value is a {@link com.net2plan.utils.Pair Pair} representing the origin node and the destination node of the link, respectively
     * @param originNodeId Origin node
     * @param destinationNodeId Destination node
     * @param linkCostMap Cost per link, where the key is the link identifier and the value is the cost of traversing the link. No special iteration-order (i.e. ascending) is required
     * @param linkSpareCapacityMap Current available capacity per link, where the key is the link identifier and the value is the cost of traversing the link. No special iteration-order (i.e. ascending) is required
     * @param capacityGoal Minimum capacity required
     * @return Shortest path fulfilling a minimum capacity requirement */
    //	public static List<Long> getCapacitatedShortestPath(Collection <Long> nodes , Map<Long, Pair<Long, Long>> linkMap, long originNodeId, long destinationNodeId, final Map<Long, Double> linkCostMap, final Map<Long, Double> linkSpareCapacityMap, final double capacityGoal)
    //	{
    //		Graph<Long, Long> graph = JUNGUtils.getGraphFromLinkMap(nodes , linkMap);
    //		if (!graph.containsVertex(originNodeId)) return new LinkedList<Long>();
    //		if (!graph.containsVertex(destinationNodeId)) return new LinkedList<Long>();
    //
    //		Transformer<Long, Double> nev = JUNGUtils.getEdgeWeightTransformer(linkCostMap);
    //		Transformer<Long, Double> capacityTransformer = JUNGUtils.getEdgeWeightTransformer(linkSpareCapacityMap);
    //		return JUNGUtils.getCapacitatedShortestPath(graph, nev, originNodeId, destinationNodeId, capacityTransformer, capacityGoal);
    //	}

    /** Obtains the sequence of links representing the (unidirectional) shortest path between two nodes.
     *
     * @param nodes Collection of nodes
     * @param linkMap Map of links, where the key is the unique link identifier and the value is a {@link com.net2plan.utils.Pair Pair} representing the origin node and the destination node of the link, respectively
     * @param originNodeId Origin node identifier
     * @param destinationNodeId Destination node identifier
     * @param linkCostMap Cost per link, where the key is the link identifier and the value is the cost of traversing the link. No special iteration-order (i.e. ascending) is required
     * @return Sequence of links in the shortest path (empty, if destination not reachable from origin) */
    //	public static List<Long> getShortestPath(Collection<Long> nodes , Map<Long, Pair<Long, Long>> linkMap, long originNodeId, long destinationNodeId, Map<Long, Double> linkCostMap)
    //	{
    //		Graph<Long, Long> graph = JUNGUtils.getGraphFromLinkMap(nodes , linkMap);
    //		if (!graph.containsVertex(originNodeId)) return new LinkedList<Long>();
    //		if (!graph.containsVertex(destinationNodeId)) return new LinkedList<Long>();
    //
    //		Transformer<Long, Double> nev = JUNGUtils.getEdgeWeightTransformer(linkCostMap);
    //		return JUNGUtils.getShortestPath(graph, nev, originNodeId, destinationNodeId);
    //	}

    /**
     * Returns the K-loopless shortest paths between two nodes, satisfying some user-defined constraints. If only <i>n</i> shortest path are found (n&lt;K), those are returned.
     *
     * @param nodes                                   List of nodes
     * @param links                                   List of links
     * @param originNode                              Origin node
     * @param destinationNode                         Destination node
     * @param linkCostMap                             Cost per link, where the key is the link identifier and the value is the cost of traversing the link. No special iteration-order (i.e. ascending) is required. If {@code null}, all links have weight one
     * @param K                                       Desired nummber of paths (a lower number of paths may be returned if there are less than {@code K} loop-less paths admissible)
     * @param maxLengthInKm                           Maximum length of the path. If non-positive, no maximum limit is assumed
     * @param maxNumHops                              Maximum number of hops. If non-positive, no maximum limit is assumed
     * @param maxPropDelayInMs                        Maximum propagation delay of the path. If non-positive, no maximum limit is assumed
     * @param maxRouteCost                            Maximum route cost. If non-positive, no maximum limit is assumed
     * @param maxRouteCostFactorRespectToShortestPath Maximum route cost factor respect to the shortest path. If non-positive, no maximum limit is assumed
     * @param maxRouteCostRespectToShortestPath       Maximum route cost respect to the shortest path. If non-positive, no maximum limit is assumed
     * @return K-shortest paths
     */
    public static List<List<Link>> getKLooplessShortestPaths(List<Node> nodes, List<Link> links, Node originNode, Node destinationNode, Map<Link, Double> linkCostMap, int K, double maxLengthInKm, int maxNumHops, double maxPropDelayInMs, double maxRouteCost, double maxRouteCostFactorRespectToShortestPath, double maxRouteCostRespectToShortestPath) {
        if (maxLengthInKm <= 0) maxLengthInKm = Double.MAX_VALUE;
        if (maxNumHops <= 0) maxNumHops = Integer.MAX_VALUE;
        if (maxPropDelayInMs <= 0) maxPropDelayInMs = Double.MAX_VALUE;
        if (maxRouteCost <= 0) maxRouteCost = Double.MAX_VALUE;
        if (maxRouteCostFactorRespectToShortestPath <= 0) maxRouteCostFactorRespectToShortestPath = Double.MAX_VALUE;
        if (maxRouteCostRespectToShortestPath <= 0) maxRouteCostRespectToShortestPath = Double.MAX_VALUE;

        if (linkCostMap == null) {
            linkCostMap = new HashMap<Link, Double>();
            for (Link e : links)
                linkCostMap.put(e, 1.0);
        }

        Graph<Node, Link> g = JUNGUtils.getGraphFromLinkMap(nodes, links);

        YenAlgorithm<Node, Link> paths = new YenAlgorithm<Node, Link>(g, (Transformer<Link, Double>) JUNGUtils.getEdgeWeightTransformer(linkCostMap), K, maxNumHops, maxLengthInKm, maxPropDelayInMs, maxRouteCost, maxRouteCostFactorRespectToShortestPath, maxRouteCostRespectToShortestPath) {
            @Override
            public boolean acceptPath(GraphPath<Link> candidate) {
                if (maxNumHops != Integer.MAX_VALUE && candidate.getPathLength() > maxNumHops) return false;

                if (maxLengthInKm == Double.MAX_VALUE && maxPropDelayInMs == Double.MAX_VALUE) return true;

                double pathLengthInKm = 0;
                double pathPropDelayInMs = 0;
                for (Link link : candidate.getPath()) {
                    pathLengthInKm += link.getLengthInKm();
                    pathPropDelayInMs += link.getPropagationDelayInMs();
                }

                return pathLengthInKm <= maxLengthInKm && pathPropDelayInMs <= maxPropDelayInMs;
            }

            @Override
            public boolean compareCandidateToShortestPath(GraphPath<Link> candidate, GraphPath<Link> shortestPath) {
                if (maxRouteCost != Double.MAX_VALUE && candidate.getPathWeight() > maxRouteCost) {
                    return false;
                }

                if (maxRouteCostFactorRespectToShortestPath != Double.MAX_VALUE && candidate.getPathWeight() > shortestPath.getPathWeight() * maxRouteCostFactorRespectToShortestPath) {
                    return false;
                }

                return !(maxRouteCostRespectToShortestPath != Double.MAX_VALUE && candidate.getPathWeight() > shortestPath.getPathWeight() + maxRouteCostRespectToShortestPath);
            }
        };
        return paths.getPaths(originNode, destinationNode, K);
        //		////////////////////////////////////
        //		Graph<Node, Link> graph = JUNGUtils.getGraphFromLinkMap(nodes , linkMap);
        //		final Transformer<Link, Double> nev = JUNGUtils.getEdgeWeightTransformer(linkCostMap);
        //		return JUNGUtils.getKLooplessShortestPaths(graph, nev, originNode, destinationNode , K);
    }

    /**
     * Returns the shortest pair of link-disjoint paths, where each item represents a path. The number of returned items will be equal to the number of paths found: when empty, no path was found; when {@code size()} = 1, only one path was found; and when {@code size()} = 2, the link-disjoint paths were found. Internally it uses the Suurballe-Tarjan algorithm.
     *
     * @param nodes           Collection of nodes
     * @param links           Collection of links
     * @param originNode      Origin node
     * @param destinationNode Destination node
     * @param linkCostMap     Cost per link, where the key is the link identifier and the value is the cost of traversing the link. No special iteration-order (i.e. ascending) is required
     * @return Shortest pair of link-disjoint paths
     */
    public static List<List<Link>> getTwoLinkDisjointPaths(Collection<Node> nodes, Collection<Link> links, Node originNode, Node destinationNode, Map<Link, Double> linkCostMap) {
        Graph<Node, Link> graph = JUNGUtils.getGraphFromLinkMap(nodes, links);
        Transformer<Link, Double> nev = JUNGUtils.getEdgeWeightTransformer(linkCostMap);
        List<List<Link>> linkDisjointSPs = JUNGUtils.getTwoLinkDisjointPaths(graph, nev, originNode, destinationNode);
        return linkDisjointSPs;
    }

    /**
     * Returns the shortest pair of node-disjoint paths, where each item represents a path. The number of returned items will be equal to the number of paths found: when empty, no path was found; when {@code size()} = 1, only one path was found; and when {@code size()} = 2, the node-disjoint paths were found. Internally it uses the Suurballe-Tarjan algorithm.
     *
     * @param nodes           Collection of nodes
     * @param links           Collection of links
     * @param originNode      Origin node
     * @param destinationNode Destination node
     * @param linkCostMap     Cost per link, where the key is the link identifier and the value is the cost of traversing the link. No special iteration-order (i.e. ascending) is required
     * @return Shortest pair of node-disjoint paths
     */
    public static List<List<Link>> getTwoNodeDisjointPaths(Collection<Node> nodes, Collection<Link> links, Node originNode, Node destinationNode, Map<Link, Double> linkCostMap) {
        Graph<Node, Link> graph = JUNGUtils.getGraphFromLinkMap(nodes, links);
        Transformer<Link, Double> nev = JUNGUtils.getEdgeWeightTransformer(linkCostMap);
        List<List<Link>> nodeDisjointSPs = JUNGUtils.getTwoNodeDisjointPaths(graph, nev, originNode, destinationNode);
        return nodeDisjointSPs;
    }

    /**
     * <p>Given a map of links, it computes the adjacency matrix. This is a matrix with as many rows and columns as nodes. Position (<i>i</i>,<i>j</i>) accounts for the number of links/demands/paths from node <i>i</i> (<i>i = 0</i> refers to the first node in {@code nodeIds}, <i>i = 1</i> refers to the second one, and so on) to node <i>j</i> (<i>j = 0</i> refers to the first node in {@code nodeIds}, <i>j = 1</i> refers to the second one, and so on).</p>
     * <p>
     * <p>The output is in the sparse {@code DoubleMatrix2D} format, so that could be directly used along with the <a href='#jom'>JOM</a> library in order to solve optimization problems.</p>
     * <p>
     * <p>For users not interested in this format, a classical dense {@code double[][]} matrix could be obtained via the command:</p>
     * <p>
     * <p>{@code double[][] matrix = getAdjacencyMatrix(nodeIds, linkMap).toArray();}</p>
     *
     * @param nodes   List of nodes
     * @param linkMap Map of links, where the key is the unique link identifier and the value is a {@link com.net2plan.utils.Pair Pair} representing the origin node and the destination node of the link, respectively
     * @return Adjacency matrix
     * @see <a href='http://www.net2plan.com/jom/'>Java Optimization Modeler (JOM) website</a>
     * @since 0.3.1
     */
    public static DoubleMatrix2D getAdjacencyMatrix(List<Node> nodes, List<? extends NetworkElement> linkMap) {
        int N = nodes.size();
        DoubleMatrix2D delta_nn = DoubleFactory2D.sparse.make(N, N);
        for (NetworkElement o : linkMap)
            if (o instanceof Link) {
                Link e = (Link) o;
                delta_nn.set(e.getOriginNode().getIndex(), e.getDestinationNode().getIndex(), delta_nn.get(e.getOriginNode().getIndex(), e.getDestinationNode().getIndex()) + 1);
            } else if (o instanceof Demand) {
                Demand e = (Demand) o;
                delta_nn.set(e.getIngressNode().getIndex(), e.getEgressNode().getIndex(), delta_nn.get(e.getIngressNode().getIndex(), e.getEgressNode().getIndex()) + 1);
            } else if (o instanceof Route) {
                Route e = (Route) o;
                delta_nn.set(e.getIngressNode().getIndex(), e.getEgressNode().getIndex(), delta_nn.get(e.getIngressNode().getIndex(), e.getEgressNode().getIndex()) + 1);
            } else
                throw new Net2PlanException("Error making the matrix");
        return delta_nn;
    }

    /**
     * <p>Given a map of links representing a bidirectional topology, with the same number of links on each direction for each node pair, it bundles into opposite link pairs and computes the bidirectional matrix. This is a matrix with as many rows as opposite link pairs (or bidirectional links) and columns as links. For each bidirectional link <i>i</i>, position (<i>i</i>,<i>j</i>) is equal to 1 for link <i>j</i> (<i>e = 0</i> refers to the first link in {@code linkMap.keySet()}, <i>e = 1</i> refers to the second one, and so on) in one direction, -1 for the link <i>j</i> in the opposite direction, and zero otherwise. By convention, the selection order of links for each bidirectional link is given by the following scheme: first, for each node <i>i</i> (in ascending order), it iterates over all node <i>j</i> (in ascending order); then, for each node pair (<i>i</i>, <i>j</i>) it retrieves the identifiers of links from <i>i</i> to <i>j</i>, and viceversa. Finally, it adds a new row to the matrix for each bidirectional link consisting of a link from <i>i</i> to <i>j</i> and another one in the opposite direction (also, in ascending order).</p>
     * <p>
     * <p>The output is in the sparse {@code DoubleMatrix2D} format, so that could be directly used along with the <a href='#jom'>JOM</a> library in order to solve optimization problems.</p>
     * <p>
     * <p>For users not interested in this format, a classical dense {@code double[][]} matrix could be obtained via the command:</p>
     * <p>
     * <p>{@code double[][] matrix = getBidirectionalMatrix(nodeIds, linkMap).toArray();}</p>
     *
     * @param nodes   List of nodes
     * @param linkMap Map of links, where the key is the unique link identifier and the value is a {@link com.net2plan.utils.Pair Pair} representing the origin node and the destination node of the link, respectively. It is mandatory that can be iterated in ascending order of link identifier (i.e. using {@code TreeMap} or those {@code Map} objects returned from {@link com.net2plan.interfaces.networkDesign.NetPlan Net2Plan} object
     * @return Bidirectional matrix
     * @see <a href='http://www.net2plan.com/jom/'>Java Optimization Modeler (JOM) website</a>
     * @since 0.3.1
     */
    public static DoubleMatrix2D getBidirectionalMatrix(List<Node> nodes, List<Link> linkMap) {
        int E = linkMap.size();
        Graph<Node, Link> graph = JUNGUtils.getGraphFromLinkMap(nodes, linkMap);

        DoubleMatrix2D M_ij = DoubleFactory2D.sparse.make(E / 2, E);
        int i = 0;
        for (Node nodeId_1 : nodes) {
            for (Node nodeId_2 : nodes) {
                if (nodeId_1.getIndex() >= nodeId_2.getIndex()) continue;

                Collection<Link> downstreamLinks = graph.findEdgeSet(nodeId_1, nodeId_2);
                Collection<Link> upstreamLinks = graph.findEdgeSet(nodeId_2, nodeId_1);
                if (downstreamLinks.size() != upstreamLinks.size())
                    throw new Net2PlanException("Link map must be bidirectional (same number of links on each direction)");

                Iterator<Link> downstreamLinks_it = downstreamLinks.iterator();
                Iterator<Link> upstreamLinks_it = upstreamLinks.iterator();
                while (downstreamLinks_it.hasNext()) {
                    int e_1 = downstreamLinks_it.next().getIndex();
                    int e_2 = upstreamLinks_it.next().getIndex();

                    M_ij.setQuick(i, e_1, 1);
                    M_ij.setQuick(i, e_2, -1);
                    i++;
                }
            }
        }

        return M_ij;
    }

    /**
     * <p>Given a list of Network Elements, it computes the node-network element incidence matrix. This is a matrix with as many rows as nodes, and as many columns as elements. Position (<i>n</i>, <i>e</i>) has a 1 if element <i>e</i> (<i>e = 0</i> refers to the first element}, <i>e = 1</i> refers to the second one, and so on) is initiated in node <i>n</i> (<i>n = 0</i> refers to the first node , <i>n = 1</i> refers to the second one, and so on), -1 if it ends in node n, and 0 otherwise.</p>
     *
     * @param nodes    List of nodes
     * @param elements List of elements
     * @return Incidence matrix
     * @see com.net2plan.interfaces.networkDesign.NetworkLayer
     */
    public static DoubleMatrix2D getIncidenceMatrix(List<Node> nodes, List<? extends NetworkElement> elements) {
        int N = nodes.size();
        int E = elements.size();
        DoubleMatrix2D A_ne = DoubleFactory2D.sparse.make(N, E);
        for (NetworkElement o : elements)
            if (o instanceof Link) {
                Link e = (Link) o;
                A_ne.set(e.getOriginNode().getIndex(), e.getIndex(), 1);
                A_ne.set(e.getDestinationNode().getIndex(), e.getIndex(), -1);
            } else if (o instanceof Demand) {
                Demand e = (Demand) o;
                A_ne.set(e.getIngressNode().getIndex(), e.getIndex(), 1);
                A_ne.set(e.getEgressNode().getIndex(), e.getIndex(), -1);
            } else if (o instanceof Route) {
                Route e = (Route) o;
                A_ne.set(e.getIngressNode().getIndex(), e.getIndex(), 1);
                A_ne.set(e.getEgressNode().getIndex(), e.getIndex(), -1);
            } else
                throw new Net2PlanException("Error making the matrix");
        return A_ne;
    }

    /**
     * <p>Given a list of Network Element, it computes the node-network element incoming incidence matrix. This is a matrix with as many rows as nodes, and as many columns as network elements. Position (<i>n</i>, <i>e</i>) has a 1 if element <i>e</i> (<i>e = 0</i> refers to the first element n {@code elements}, <i>e = 1</i> refers to the second one, and so on) is terminated in node <i>n</i> (<i>n = 0</i> refers to the first node in {@code nodes}, <i>n = 1</i> refers to the second one, and so on), and 0 otherwise.</p>
     *
     * @param nodes    List of nodes
     * @param elements List of Network Elements
     * @return Node-link incoming incidence matrix
     * @see com.net2plan.interfaces.networkDesign.NetworkLayer
     */
    public static DoubleMatrix2D getIncomingIncidenceMatrix(List<Node> nodes, List<? extends NetworkElement> elements) {
        int N = nodes.size();
        int E = elements.size();
        DoubleMatrix2D A_ne = DoubleFactory2D.sparse.make(N, E);
        for (NetworkElement o : elements)
            if (o instanceof Link) {
                Link e = (Link) o;
                A_ne.set(e.getDestinationNode().getIndex(), e.getIndex(), 1);
            } else if (o instanceof Demand) {
                Demand e = (Demand) o;
                A_ne.set(e.getEgressNode().getIndex(), e.getIndex(), 1);
            } else if (o instanceof Route) {
                Route e = (Route) o;
                A_ne.set(e.getEgressNode().getIndex(), e.getIndex(), 1);
            } else
                throw new Net2PlanException("Error making the matrix");
        return A_ne;
    }

    /**
     * <p>Given a list of Network elements, it computes the node-network element outgoing incidence matrix. This is a matrix with as many rows as nodes, and as many columns as elements. Position (<i>n</i>, <i>e</i>) has a 1 if element <i>e</i> (<i>e = 0</i> refers to the first element, <i>e = 1</i> refers to the second one, and so on) is initiated in node <i>n</i> (<i>n = 0</i> refers to the first node , <i>n = 1</i> refers to the second one, and so on), and 0 otherwise.</p>
     *
     * @param nodes    List of nodes
     * @param elements List of elements
     * @return Node-link outgoing incidence matrix
     * @see com.net2plan.interfaces.networkDesign.NetworkElement
     */
    public static DoubleMatrix2D getOutgoingIncidenceMatrix(List<Node> nodes, List<? extends NetworkElement> elements) {
        int N = nodes.size();
        int E = elements.size();
        DoubleMatrix2D A_ne = DoubleFactory2D.sparse.make(N, E);
        for (NetworkElement o : elements)
            if (o instanceof Link) {
                Link e = (Link) o;
                A_ne.set(e.getOriginNode().getIndex(), e.getIndex(), 1);
            } else if (o instanceof Demand) {
                Demand e = (Demand) o;
                A_ne.set(e.getIngressNode().getIndex(), e.getIndex(), 1);
            } else if (o instanceof Route) {
                Route e = (Route) o;
                A_ne.set(e.getIngressNode().getIndex(), e.getIndex(), 1);
            } else
                throw new Net2PlanException("Error making the matrix");
        return A_ne;
    }

    /**
     * Obtains the sequence of links representing the (unidirectional) shortest path between two nodes.
     *
     * @param nodes           Collection of nodes
     * @param links           Collection of links
     * @param originNode      Origin node
     * @param destinationNode Destination node
     * @param linkCostMap     Cost per link, where the key is the link identifier and the value is the cost of traversing the link. No special iteration-order (i.e. ascending) is required. If <code>null</code>, the shortest path in number of traversed links is returned,
     * @return Sequence of links in the shortest path (empty, if destination not reachable from origin)
     */
    public static List<Link> getShortestPath(Collection<Node> nodes, Collection<Link> links, Node originNode, Node destinationNode, Map<Link, Double> linkCostMap) {
        Graph<Node, Link> graph = JUNGUtils.getGraphFromLinkMap(nodes, links);
        if (!graph.containsVertex(originNode)) return new LinkedList<Link>();
        if (!graph.containsVertex(destinationNode)) return new LinkedList<Link>();

        Transformer<Link, Double> nev = JUNGUtils.getEdgeWeightTransformer(linkCostMap);
        return JUNGUtils.getShortestPath(graph, nev, originNode, destinationNode);
    }


    /**
     * Returns the K-minimum cost multicast trees starting in the originNode and ending in the set destinationNodes, satisfying some user-defined constraints.
     * If only <i>n</i> multicast trees are found (n&lt;K), those are returned.
     *
     * @param links                                     the network links
     * @param originNode                                the origin node of all the multicast trees
     * @param destinationNodes                          the set of destination nodes of all the multicast trees
     * @param Aout_ne                                   the outgoing incidence matrix: a matrix with one row per node, and one column per link. Coordinate (n,e) is 1 if link e is an outgoing link of node n, and 0 otherwise.
     * @param Ain_ne                                    the incoming incidence matrix: a matrix with one row per node, and one column per link. Coordinate (n,e) is 1 if link e is an incoming link of node n, and 0 otherwise.
     * @param linkCost                                  the cost to be associated to each link
     * @param solverName                                the name of the solver to call for the internal formulation of the algorithm
     * @param solverLibraryName                         the solver library name
     * @param maxSolverTimeInSecondsPerTree             the maximum time the solver is allowed for each of the internal formulations (one for each new tree). The best solution found so far is returned. If non-positive, no time limit is set
     * @param K                                         Desired nummber of trees (a lower number of trees may be returned if there are less than {@code K} multicast trees admissible)
     * @param maxCopyCapability                         the maximum number of copies of an input traffic a node can make. Then, a node can have at most this number of ouput links carrying traffic of a multicast tree
     * @param maxE2ELengthInKm                          Maximum path length measured in kilometers allowed for any tree, from the origin node, to any destination node
     * @param maxE2ENumHops                             Maximum number of hops allowed for any tree, from the origin node, to any destination node
     * @param maxE2EPropDelayInMs                       Maximum propagation delay in miliseconds allowed in a path, for any tree, from the origin node, to any destination node
     * @param maxTreeCost                               Maximum tree weight allowed, summing the weights of the links
     * @param maxTreeCostFactorRespectToMinimumCostTree Trees with higher weight (cost) than the cost of the minimum cost tree, multiplied by this factor, are not returned
     * @param maxTreeCostRespectToMinimumCostTree       Trees with higher weight (cost) than the cost of the minimum cost tree, plus this factor, are not returned. While the previous one is a multiplicative factor, this one is an additive factor
     * @return the list of k-minimum cost multicast trees constrained according to the method inputs
     */
    public static List<Set<Link>> getKMinimumCostMulticastTrees(List<Link> links, Node originNode, Set<Node> destinationNodes, DoubleMatrix2D Aout_ne, DoubleMatrix2D Ain_ne, DoubleMatrix1D linkCost, String solverName, String solverLibraryName, double maxSolverTimeInSecondsPerTree, int K, int maxCopyCapability, double maxE2ELengthInKm, int maxE2ENumHops, double maxE2EPropDelayInMs, double maxTreeCost, double maxTreeCostFactorRespectToMinimumCostTree, double maxTreeCostRespectToMinimumCostTree) {
        if (K <= 0) throw new Net2PlanException("'K' parameter must be greater than zero");
        if (maxCopyCapability <= 0) maxCopyCapability = Integer.MAX_VALUE;
        if (maxE2ELengthInKm <= 0) maxE2ELengthInKm = Double.MAX_VALUE;
        if (maxE2ENumHops <= 0) maxE2ENumHops = Integer.MAX_VALUE;
        if (maxE2EPropDelayInMs <= 0) maxE2EPropDelayInMs = Double.MAX_VALUE;
        if (maxTreeCost <= 0) maxTreeCost = Double.MAX_VALUE;
        if (maxTreeCostFactorRespectToMinimumCostTree <= 0)
            maxTreeCostFactorRespectToMinimumCostTree = Double.MAX_VALUE;
        if (maxTreeCostRespectToMinimumCostTree <= 0) maxTreeCostRespectToMinimumCostTree = Double.MAX_VALUE;

        List<Set<Link>> result = new LinkedList<Set<Link>>();
        final int E = links.size();
        final int N = Aout_ne.rows();
        final int T = destinationNodes.size();
        final int[] targetIndexes = new int[T];
        int counter = 0;
        for (Node n : destinationNodes)
            targetIndexes[counter++] = n.getIndex();
        List<DoubleMatrix1D> previousTrees = new ArrayList<DoubleMatrix1D>();
        DoubleMatrix1D delta_ad = DoubleFactory1D.sparse.make(N);
        delta_ad.set(originNode.getIndex(), 1);
        DoubleMatrix1D delta_bd = DoubleFactory1D.sparse.make(N);
        for (Node n : destinationNodes)
            delta_bd.set(n.getIndex(), 1);
        double firstTreeCost = -1;
        Set<Link> firstTree = null;
        for (int k = 0; k < K; k++) {
            OptimizationProblem op = new OptimizationProblem();
            op.setInputParameter("E", E);
            op.setInputParameter("c_e", linkCost == null ? DoubleFactory1D.dense.make(E, 1) : linkCost, "row");
            op.setInputParameter("K", maxCopyCapability <= 0 ? E : maxCopyCapability);
            op.setInputParameter("Aout_ne", Aout_ne);
            op.setInputParameter("Ain_ne", Ain_ne);
            DoubleMatrix2D A_nt = DoubleFactory2D.sparse.make(N, T);
            for (int t = 0; t < T; t++) {
                A_nt.set(originNode.getIndex(), t, 1.0);
                A_nt.set(targetIndexes[t], t, -1.0);
            }
            op.setInputParameter("A_nt", A_nt); // 1 if node n is ingress node of demand, -1 if node n is the t-th egress node of demand
            op.setInputParameter("T", T);
            op.setInputParameter("delta_ad", delta_ad, "row");
            op.setInputParameter("delta_bd", delta_bd, "row");
            op.setInputParameter("onesT", DoubleFactory1D.dense.make(T, 1.0), "row");
            op.addDecisionVariable("x_e", true, new int[]{1, E}, 0, 1);
            op.addDecisionVariable("x_et", true, new int[]{E, T}, 0, 1); // 1 if link e is in the path of the tree from the demand ingress node to the t-th demand target node
            op.setObjectiveFunction("minimize", "c_e * x_e'");
            op.addConstraint("x_et <= x_e' * onesT"); // a link belongs to a path only if it is in the tree
            op.addConstraint("(Aout_ne - Ain_ne) * x_et == A_nt"); // flow conservation constraint for each path in the tree
            op.addConstraint("Ain_ne * x_e' >= delta_bd'"); // a destination node receives at least one input link
            op.addConstraint("Ain_ne * x_e' <= 1 - delta_ad'"); // source nodes receive 0 links, destination nodes at most one (then just one)
            op.addConstraint("Aout_ne * x_e' <= K * (delta_ad' + Ain_ne * x_e')"); // at most K out links from ingress node and from intermediate nodes if they have one input link
            double maximumAllowedTreeCost = maxTreeCost;
            if (!previousTrees.isEmpty()) {
                maximumAllowedTreeCost = Math.min(maximumAllowedTreeCost, Math.min(firstTreeCost * maxTreeCostFactorRespectToMinimumCostTree, firstTreeCost + maxTreeCostRespectToMinimumCostTree));
                if (maximumAllowedTreeCost != Double.MAX_VALUE)
                    op.addConstraint("c_e * x_e' <= " + maximumAllowedTreeCost);
            }
            if ((maxE2ELengthInKm > 0) && (maxE2ELengthInKm < Double.MAX_VALUE)) {
                DoubleMatrix1D d_e = DoubleFactory1D.dense.make(E);
                for (Link e : links)
                    d_e.set(e.getIndex(), e.getLengthInKm());
                op.setInputParameter("d_e", d_e, "row");
                op.addConstraint("d_e * x_et <= " + maxE2ELengthInKm);
            }
            if ((maxE2ENumHops > 0) && (maxE2ENumHops < Integer.MAX_VALUE)) {
                op.setInputParameter("onesE", DoubleFactory1D.dense.make(E, 1.0), "row");
                op.addConstraint("onesE * x_et <= " + maxE2ENumHops);
            }
            if (maxE2EPropDelayInMs < Double.MAX_VALUE) {
                DoubleMatrix1D p_e = DoubleFactory1D.dense.make(E);
                for (Link e : links)
                    p_e.set(e.getIndex(), e.getPropagationDelayInMs());
                op.setInputParameter("p_e", p_e, "row");
                op.addConstraint("p_e * x_et <= " + maxE2EPropDelayInMs);
            }
            /* The constraint for not repeating previous solutions */
            for (int cont = 0; cont < previousTrees.size(); cont++) {
                op.setInputParameter("previousSolution", previousTrees.get(cont), "row");
                op.addConstraint("previousSolution * x_e' <= " + (previousTrees.get(cont).zSum() - 1));
            }

            if (solverLibraryName == null)
                op.solve(solverName, "maxSolverTimeInSeconds", (Double) maxSolverTimeInSecondsPerTree);
            else
                op.solve(solverName, "solverLibraryName", solverLibraryName, "maxSolverTimeInSeconds", (Double) maxSolverTimeInSecondsPerTree);

			/* If the problem is infeqasible, there are no more trees for this demand */
            if (op.feasibleSolutionDoesNotExist()) {
                System.out.println("*** K minimum cost multicast tree + BREAK when k = " + k);
                break;
            }
            if (!op.solutionIsFeasible())
                throw new Net2PlanException("The multicast tree ILP in the candidate tree list ended without producing a feasible solution nor guaranteeing unfeasibility: increase the solver time?");
			/* Add the tree */
            final double[] x_e = op.getPrimalSolution("x_e").to1DArray();
            Set<Link> res = new HashSet<Link>();
            double treeCost = 0;
            for (int e = 0; e < E; e++)
                if (Math.abs(x_e[e] - 1) < 1E-3) {
                    res.add(links.get(e));
                    treeCost += linkCost.get(e);
                }
            previousTrees.add(DoubleFactory1D.sparse.make(x_e));
            if (firstTree == null) {
                firstTree = res;
                firstTreeCost = treeCost;
            }
            result.add(res);
        }
        return result;
    }

    public static Set<Link> getMinimumCostMulticastTree(List<Link> links, DoubleMatrix2D Aout_ne, DoubleMatrix2D Ain_ne, DoubleMatrix1D linkCost, Node originNode, Set<Node> destinationNodes, int maxCopyCapability, int maxE2ENumHops, double maxE2ELengthInKm, double maxE2EPropDelayInMs, String solverName, String solverLibraryName, double maxSolverTimeInSeconds, String... solverParam) {
        final int E = Aout_ne.columns();
        final int N = Aout_ne.rows();
        final int T = destinationNodes.size();
        final int[] targetIndexes = new int[T];
        int counter = 0;
        for (Node n : destinationNodes)
            targetIndexes[counter++] = n.getIndex();
        if ((Ain_ne.rows() != N) || (Ain_ne.columns() != E) || (linkCost.size() != E))
            throw new Net2PlanException("Wrong array size");
        final DoubleMatrix1D delta_ad = DoubleFactory1D.sparse.make(N);
        delta_ad.set(originNode.getIndex(), 1);
        final DoubleMatrix1D delta_bd = DoubleFactory1D.sparse.make(N);
        for (Node n : destinationNodes)
            delta_bd.set(n.getIndex(), 1);

        OptimizationProblem op = new OptimizationProblem();
        op.setInputParameter("E", E);
        op.setInputParameter("c_e", linkCost, "row");
        op.setInputParameter("K", maxCopyCapability <= 0 ? E : maxCopyCapability);
        op.setInputParameter("Aout_ne", Aout_ne);
        op.setInputParameter("Ain_ne", Ain_ne);
        DoubleMatrix2D A_nt = DoubleFactory2D.sparse.make(N, T);
        for (int t = 0; t < T; t++) {
            A_nt.set(originNode.getIndex(), t, 1.0);
            A_nt.set(targetIndexes[t], t, -1.0);
        }
        op.setInputParameter("A_nt", A_nt); // 1 if node n is ingress node of demand, -1 if node n is the t-th egress node of demand
        op.setInputParameter("T", T);
        op.setInputParameter("delta_ad", delta_ad, "row");
        op.setInputParameter("delta_bd", delta_bd, "row");
        op.setInputParameter("onesT", DoubleFactory1D.dense.make(T, 1.0), "row");
        op.addDecisionVariable("x_e", true, new int[]{1, E}, 0, 1);
        op.addDecisionVariable("x_et", true, new int[]{E, T}, 0, 1); // 1 if link e is in the path of the tree from the demand ingress node to the t-th demand target node
        op.setObjectiveFunction("minimize", "c_e * x_e'");
        op.addConstraint("x_et <= x_e' * onesT"); // a link belongs to a path only if it is in the tree
        op.addConstraint("(Aout_ne - Ain_ne) * x_et == A_nt"); // flow conservation constraint for each path in the tree
        op.addConstraint("Ain_ne * x_e' >= delta_bd'"); // a destination node receives at least one input link
        op.addConstraint("Ain_ne * x_e' <= 1 - delta_ad'"); // source nodes receive 0 links, destination nodes at most one (then just one)
        op.addConstraint("Aout_ne * x_e' <= K * (delta_ad' + Ain_ne * x_e')"); // at most K out links from ingress node and from intermediate nodes if they have one input link
        if ((maxE2ELengthInKm > 0) && (maxE2ELengthInKm < Double.MAX_VALUE)) {
            DoubleMatrix1D linkLength = DoubleFactory1D.dense.make(E);
            for (Link e : links)
                linkLength.set(e.getIndex(), e.getLengthInKm());
            op.setInputParameter("d_e", linkLength, "row");
            op.addConstraint("d_e * x_et <= " + maxE2ELengthInKm);
        }
        if ((maxE2ENumHops > 0) && (maxE2ENumHops < Integer.MAX_VALUE)) {
            op.setInputParameter("onesE", DoubleFactory1D.dense.make(E, 1.0), "row");
            op.addConstraint("onesE * x_et <= " + maxE2ENumHops);
        }
        if ((maxE2EPropDelayInMs > 0) && (maxE2EPropDelayInMs < Double.MAX_VALUE)) {
            DoubleMatrix1D prop_e = DoubleFactory1D.dense.make(E);
            for (Link e : links)
                prop_e.set(e.getIndex(), e.getPropagationDelayInMs());
            op.setInputParameter("p_e", prop_e, "row");
            op.addConstraint("p_e * x_et <= " + maxE2EPropDelayInMs);
        }

        if (solverLibraryName == null)
            op.solve(solverName, "maxSolverTimeInSeconds", maxSolverTimeInSeconds);
        else
            op.solve(solverName, "solverLibraryName", solverLibraryName, "maxSolverTimeInSeconds", maxSolverTimeInSeconds);

		/* If the problem is infeqasible, there are no more trees for this demand */
        if (op.feasibleSolutionDoesNotExist()) return new HashSet<Link>();
        if (!op.solutionIsFeasible())
            throw new Net2PlanException("The multicast tree ILP ended without producing a feasible solution nor guaranteeing unfeasibility: increase the solver time?");
		/* Add the tree */
        final double[] x_e = op.getPrimalSolution("x_e").to1DArray();
        Set<Link> res = new HashSet<Link>();
        for (int e = 0; e < E; e++)
            if (Math.abs(x_e[e] - 1) < 1e-3) res.add(links.get(e));
        return res;
    }

    /**
     * Checks whether the physical topology has the same number of links between each node pair in both directions (assuming multi-digraphs).
     *
     * @param links List of links
     * @param nodes List of nodes
     * @return {@code true} if the physical topology is bidirectional, and false otherwise
     */
    public static boolean isBidirectional(List<Node> nodes, List<Link> links) {
        org.jgrapht.Graph<Node, Link> graph = JGraphTUtils.getGraphFromLinkMap(nodes, links);
        return JGraphTUtils.isWeightedBidirectional(graph);
    }

    /**
     * Check whether the physical topology is connected, that is, if it is possible to connect every node to each other, but only in a subset of nodes (subgraph).
     *
     * @param nodes List of nodes
     * @param links List of links
     * @return {@code true} if the subgraph is connected, and false otherwise
     */
    public static boolean isConnected(List<Node> nodes, List<Link> links) {
        org.jgrapht.Graph<Node, Link> graph = JGraphTUtils.getGraphFromLinkMap(nodes, links);
        return JGraphTUtils.isConnected(graph, new HashSet<Node>(nodes));
    }

    /**
     * Check whether the physical topology is simple, that is, if it has at most one unidirectional link from a node to each other.
     *
     * @param links List of links
     * @param nodes List of nodes
     * @return {@code true} if the physical topology is simple, and false otherwise
     */
    public static boolean isSimple(List<Node> nodes, List<Link> links) {
        org.jgrapht.Graph<Node, Link> graph = JGraphTUtils.getGraphFromLinkMap(nodes, links);
        return JGraphTUtils.isSimple(graph);
    }

    /**
     * Checks whether the physical topology has the same number of links/demands between each node pair in both directions (assuming multi-digraphs) and same weights per direction.
     *
     * @param nodes       List of nodes
     * @param elements    List of network elements (must be type Link or Demand)
     * @param linkCostMap Cost per link, where the key is the link identifier and the value is the cost of traversing the link. No special iteration-order (i.e. ascending) is required (only used when {@code elements} are links)
     * @return {@code true} if the physical topology is bidirectional and symmetric, and false otherwise
     * @since 0.3.0
     */
    public static boolean isWeightedBidirectional(List<Node> nodes, List<? extends NetworkElement> elements, DoubleMatrix1D linkCostMap) {
        if (elements.isEmpty()) return true;
        if (elements.get(0) instanceof Link) {
            org.jgrapht.Graph<Node, Link> auxGraph = JGraphTUtils.getGraphFromLinkMap(nodes, (List<Link>) elements);
            Map<Link, Double> linkCostMapMap = CollectionUtils.toMap((List<Link>) elements, linkCostMap);
            org.jgrapht.Graph<Node, Link> graph = JGraphTUtils.getAsWeightedGraph(auxGraph, linkCostMapMap);
            return JGraphTUtils.isWeightedBidirectional(graph);
        } else if (elements.get(0) instanceof Demand) {
            org.jgrapht.Graph<Node, Demand> auxGraph = JGraphTUtils.getGraphFromDemandMap((List<Demand>) elements);
            Map<Demand, Double> linkCostMapMap = CollectionUtils.toMap((List<Demand>) elements, linkCostMap);
            org.jgrapht.Graph<Node, Demand> graph = JGraphTUtils.getAsWeightedGraph(auxGraph, linkCostMapMap);
            return JGraphTUtils.isWeightedBidirectional(graph);
        } else
            throw new Net2PlanException("Unexpected network element type");
    }

    /**
     * Given a list of links that may contain multiple links between some node pairs, returns a matrix where appears, for each node pair, the link having the lowest weight (links whose weight is equal to {@code Double.MAX_VALUE} are included). Ties are broken arbitrarely.
     *
     * @param links       List of links
     * @param linkCostMap Cost per link, where the key is the link identifier and the value is the cost of traversing the link. No special iteration-order (i.e. ascending) is required
     * @return Matrix with the lower links weight between node pairs
     */
    public static DoubleMatrix1D simplifyLinkMap(List<Link> links, DoubleMatrix1D linkCostMap) {
        Map<Pair<Node, Node>, Link> originDestinationNodePairLinkIdMapping = new LinkedHashMap<Pair<Node, Node>, Link>();
        for (Link link : links) {
            Pair<Node, Node> originDestinationNodePair = Pair.of(link.getOriginNode(), link.getDestinationNode());
            double cost_thisLink = linkCostMap == null ? 1 : linkCostMap.get(link.getIndex());
            if (cost_thisLink == Double.MAX_VALUE) continue;
            if (originDestinationNodePairLinkIdMapping.containsKey(originDestinationNodePair) && linkCostMap != null) {
                Link bestLink = originDestinationNodePairLinkIdMapping.get(originDestinationNodePair);
                double bestCostId = linkCostMap.get(bestLink.getIndex());
                if (cost_thisLink < bestCostId)
                    originDestinationNodePairLinkIdMapping.put(originDestinationNodePair, link);
            } else {
                originDestinationNodePairLinkIdMapping.put(originDestinationNodePair, link);
            }
        }
        DoubleMatrix1D res = DoubleFactory1D.dense.make(links.size(), Double.MAX_VALUE);
        for (Link linkMantain : originDestinationNodePairLinkIdMapping.values())
            res.set(linkMantain.getIndex(), linkCostMap.get(linkMantain.getIndex()));
        return res;
    }

    /**
     * <p>Class to represent a path in a Graph. Note that a path is defined in terms of edges (rather than vertices) so that multiple edges between the same pair of vertices can be discriminated.</p>
     * <p>
     * <p>It implements the {@code {@link java.lang.Comparable Comparable} interface to impose order between different paths. First, try to order using the path weight, and if equals, using the number of hops.</p>
     *
     * @param <E> Edge type
     * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
     */
    private static class GraphPath<E> implements Comparable<GraphPath> {
        private final List<E> path;
        private final double pathWeight;

        /**
         * Default constructor.
         *
         * @param path       Sequence of links
         * @param pathWeight Path weight
         */
        public GraphPath(List<E> path, double pathWeight) {
            this.path = path;
            this.pathWeight = pathWeight;
        }

        /**
         * Returns the edges making up the path.
         *
         * @return An unmodifiable list with the sequence of edges followed by the path
         */
        public List<E> getPath() {
            return Collections.unmodifiableList(path);
        }

        /**
         * Returns the path length measured in number of hops or edges. It is equivalent to {@code {@link #getPath getPath()}.size()}.
         *
         * @return The path length
         */
        public int getPathLength() {
            return getPath().size();
        }

        /**
         * Returns the weight assigned to the path.
         *
         * @return The weight assigned to the path
         */
        public double getPathWeight() {
            return pathWeight;
        }

        /**
         * Indicates whether some other object is "equal to" this one.
         *
         * @param o Reference object with which to compare
         * @return {@code true} if this object is the same as the {@code o} argument; {@code false} otherwise
         */
        @Override
        public boolean equals(Object o) {
            if (o == null) return false;
            if (o == this) return true;
            if (!(o instanceof GraphPath)) return false;

            GraphPath p = (GraphPath) o;
            return getPath().equals(p.getPath()) && Math.abs(getPathWeight() - p.getPathWeight()) < 1e-10;
        }

        /**
         * Returns a hash code value for the object. This method is supported for the benefit of hash tables such as those provided by {@code HashMap}.
         *
         * @return Hash code value for this object
         * @since 0.2.0
         */
        @Override
        public int hashCode() {
            int hash = 7;
            hash = 37 * hash + (this.path != null ? this.path.hashCode() : 0);
            hash = 37 * hash + (int) (Double.doubleToLongBits(this.pathWeight) ^ (Double.doubleToLongBits(this.pathWeight) >>> 32));
            return hash;
        }

        /**
         * Compares this object with the specified object for order.
         *
         * @param o The object to be compared
         * @return A negative integer, zero, or a positive integer as this object is less than, equal to, or greater than the specified object
         */
        @Override
        public int compareTo(GraphPath o) {
            if (pathWeight < o.pathWeight) return -1;
            if (pathWeight > o.pathWeight) return 1;

            return path.size() == o.path.size() ? 0 : (path.size() > o.path.size() ? -1 : 1);
        }
    }

    /**
     * <p>Auxiliary class to work with the graph library <a href='GraphUtils.html#jgrapht'>JGraphT</a>.</p>
     *
     * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
     * @since 0.2.0
     */
    static class JGraphTUtils {
        private static class DummyNode extends Node {
            DummyNode(long id, int index) {
                super(null, id, index, 0, 0, "", null);
            }
        }

        private static class DummyLink extends Link {
            DummyLink(long id, int index, Node originNode, Node destinationNode) {
                super(null, id, index, null, originNode, destinationNode, 0, 0, 1, null);
            }

            DummyLink(long id, int index) {
                super(null, id, index, null, null, null, 0, 0, 0, null);
            }
        }

        /**
         * Builds an auxiliary graph for the application of edge-disjoint shortest-path pair algorithms to node-disjoint problems.
         *
         * @param graph           Graph representing the network
         * @param originNode      Origin node
         * @param destinationNode Destination node
         * @return Auxiliary graph
         */
        public static org.jgrapht.Graph<Node, Link> buildAuxiliaryNodeDisjointGraph(final org.jgrapht.Graph<Node, Link> graph, Node originNode, Node destinationNode) {
            org.jgrapht.Graph<Node, Link> auxGraph = new DirectedWeightedMultigraph<Node, Link>(Link.class);
            Map<Node, Node> originalNodeId2AuxIdMapping = new LinkedHashMap<Node, Node>();

            for (Node vertex : graph.vertexSet()) {
                auxGraph.addVertex(vertex);

                if (vertex.equals(originNode) || vertex.equals(destinationNode)) {
                    originalNodeId2AuxIdMapping.put(vertex, vertex);
                } else {
                    DummyNode auxVertex = new DummyNode(-10000 + vertex.getIndex(), -10000 + vertex.getIndex());
                    originalNodeId2AuxIdMapping.put(vertex, auxVertex);
                    DummyLink auxEdge = new DummyLink(-10000 + vertex.getIndex(), -10000 + vertex.getIndex());
                    auxGraph.addVertex(auxVertex);
                    auxGraph.addEdge(vertex, auxVertex, auxEdge);
                }
            }

            for (Link edge : graph.edgeSet()) {
                if (edge.getId() < 0)
                    throw new Net2PlanException("Bad - Edge indexes must be greater or equal than zero");

                Node originNode_thisLink = originalNodeId2AuxIdMapping.get(graph.getEdgeSource(edge));
                Node destinationNode_thisLink = graph.getEdgeTarget(edge);
                auxGraph.addEdge(originNode_thisLink, destinationNode_thisLink, edge);
            }

            return auxGraph;
        }

        /**
         * <p>It generates a weighted view of the backing graph specified in the constructor. This graph allows modules to apply algorithms designed for weighted graphs to an unweighted graph by providing an explicit edge weight mapping.</p>
         * <p>
         * <p>Query operations on this graph "read through" to the backing graph. Vertex addition/removal and edge addition/removal are supported.</p>
         *
         * @param <V>           Vertex type
         * @param <E>           Edge type
         * @param graph         The backing graph over which a weighted view is to be created
         * @param edgeWeightMap A mapping of edges to weights (null means all to one)
         * @return Returns a weighted view of the backing graph specified in the constructor
         */
        public static <V, E> org.jgrapht.WeightedGraph<V, E> getAsWeightedGraph(org.jgrapht.Graph<V, E> graph, Map<E, Double> edgeWeightMap) {
            if (edgeWeightMap == null) {
                edgeWeightMap = new LinkedHashMap<E, Double>();
                for (E edge : graph.edgeSet())
                    edgeWeightMap.put(edge, 1.0);
            }

            return new AsWeightedGraph<V, E>(graph, edgeWeightMap);
        }

        /**
         * <p>Obtains a {@code JGraphT} graph from a given link map.</p>
         *
         * @param nodes Collection of nodes
         * @param links List of links
         * @return {@code JGraphT} graph
         */
        public static org.jgrapht.Graph<Node, Link> getGraphFromLinkMap(Collection<Node> nodes, Collection<Link> links) {
            org.jgrapht.Graph<Node, Link> graph = new DirectedWeightedMultigraph<Node, Link>(Link.class);

            for (Node node : nodes)
                graph.addVertex(node);

            if (links != null) {
                for (Link link : links) {
                    Node originNode = link.getOriginNode();
                    Node destinationNode = link.getDestinationNode();

                    if (!graph.containsVertex(originNode))
                        throw new RuntimeException("Bad"); //graph.addVertex(originNode);
                    if (!graph.containsVertex(destinationNode))
                        throw new RuntimeException("Bad"); //graph.addVertex(destinationNode);

                    graph.addEdge(originNode, destinationNode, link);
                }
            }

            return graph;
        }

        /**
         * <p>Obtains a {@code JGraphT} graph from a given link map.</p>
         *
         * @param demands List of demands
         * @return {@code JGraphT} graph
         */
        public static org.jgrapht.Graph<Node, Demand> getGraphFromDemandMap(List<Demand> demands) {
            org.jgrapht.Graph<Node, Demand> graph = new DirectedWeightedMultigraph<Node, Demand>(Demand.class);

            if (demands != null) {
                for (Demand demand : demands) {
                    Node originNode = demand.getIngressNode();
                    Node destinationNode = demand.getEgressNode();

                    if (!graph.containsVertex(originNode)) graph.addVertex(originNode);
                    if (!graph.containsVertex(destinationNode)) graph.addVertex(destinationNode);

                    graph.addEdge(originNode, destinationNode, demand);
                }
            }

            return graph;
        }

        /**
         * Check whether the topology has the same number of links between each node pair in both directions (assuming multi-digraphs).
         *
         * @param graph The graph to analyze
         * @return {@code true} if the graph is bidirectional, and false otherwise
         */
        public static boolean isBidirectional(org.jgrapht.Graph graph) {
            Object[] vertices = graph.vertexSet().toArray();
            for (int v1 = 0; v1 < vertices.length; v1++)
                for (int v2 = v1 + 1; v2 < vertices.length; v2++)
                    if (graph.getAllEdges(vertices[v1], vertices[v2]).size() != graph.getAllEdges(vertices[v2], vertices[v1]).size())
                        return false;

            return true;
        }

        /**
         * Check whether the graph is connected, that is, if it is possible to connect every node to each other.
         *
         * @param graph The graph to analyze
         * @return {@code true} if the graph is connected, and false otherwise
         */
        public static boolean isConnected(org.jgrapht.Graph graph) {
            if (graph instanceof DirectedGraph) {
                StrongConnectivityInspector ci = new StrongConnectivityInspector((DirectedGraph) graph);
                return ci.isStronglyConnected();
            } else if (graph instanceof UndirectedGraph) {
                ConnectivityInspector ci = new ConnectivityInspector((UndirectedGraph) graph);
                return ci.isGraphConnected();
            }

            throw new RuntimeException("Bad");
        }

        /**
         * Check whether the graph is connected, that is, if it is possible to connect every node to each other, but only in a subset of vertices (subgraph).
         *
         * @param graph    The graph to analyze
         * @param vertices Subset of vertices
         * @return {@code true} if the subgraph is connected, and false otherwise
         */
        public static boolean isConnected(org.jgrapht.Graph graph, Set vertices) {
            Subgraph subgraph;
            if (graph instanceof DirectedGraph)
                subgraph = new DirectedSubgraph((DirectedGraph) graph, vertices, null);
            else if (graph instanceof UndirectedGraph)
                subgraph = new UndirectedSubgraph((UndirectedGraph) graph, vertices, null);
            else
                throw new RuntimeException("Bad");

            return isConnected(subgraph);
        }

        /**
         * Check whether the graph is simple, that is, if it has at most one link between each node pair (one per direction under directed graphs, one under undirected graphs).
         *
         * @param graph The graph to analyze
         * @return {@code true} if the graph is simple, and false otherwise
         */
        public static boolean isSimple(org.jgrapht.Graph graph) {
            Object[] vertices = graph.vertexSet().toArray();
            for (int v1 = 0; v1 < vertices.length; v1++) {
                for (int v2 = v1 + 1; v2 < vertices.length; v2++) {
                    if (graph.getAllEdges(vertices[v1], vertices[v2]).size() > 1) return false;
                    if (graph.getAllEdges(vertices[v2], vertices[v1]).size() > 1) return false;
                }
            }

            return true;
        }

        /**
         * Checks whether the graph has the same number of links between each node pair in both directions (assuming multi-digraphs) and same individual weights per direction.
         *
         * @param graph The graph to analyze
         * @return {@code true} if the graph is weighted-bidirectional, and false otherwise. By convention returns {@code false} if network is empty
         */
        public static boolean isWeightedBidirectional(org.jgrapht.Graph graph) {
            Object[] vertices = graph.vertexSet().toArray();
            if (vertices.length == 0) {
                return false;
            }

            for (int vertexId_1 = 0; vertexId_1 < vertices.length; vertexId_1++) {
                for (int vertexId_2 = vertexId_1 + 1; vertexId_2 < vertices.length; vertexId_2++) {
                    Set links_12 = graph.getAllEdges(vertices[vertexId_1], vertices[vertexId_2]);
                    Set links_21 = graph.getAllEdges(vertices[vertexId_2], vertices[vertexId_1]);

                    if (links_12.size() != links_21.size()) {
                        return false;
                    }

                    Iterator it_12 = links_12.iterator();
                    while (it_12.hasNext()) {
                        Object aux_12 = it_12.next();

                        Iterator it_21 = links_21.iterator();
                        while (it_21.hasNext()) {
                            Object aux_21 = it_21.next();

                            if (Math.abs(graph.getEdgeWeight(aux_12) - graph.getEdgeWeight(aux_21)) < 1E-10) {
                                it_12.remove();
                                it_21.remove();
                                break;
                            }
                        }
                    }

                    if (!links_12.isEmpty() || !links_12.isEmpty()) {
                        return false;
                    }
                }
            }

            return true;
        }
    }

    /**
     * <p>Auxiliary class to work with the graph library <a href='GraphUtils.html#jung'>JUNG</a>.</p>
     *
     * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
     * @since 0.2.0
     */
    static class JUNGUtils {
        private static class DummyNode extends Node {
            DummyNode(long id, int index) {
                super(null, id, index, 0, 0, "", null);
            }
        }

        private static class DummyLink extends Link {
            DummyLink(long id, int index, Node originNode, Node destinationNode) {
                super(null, id, index, null, originNode, destinationNode, 0, 0, 1, null);
            }

            DummyLink(long id, int index) {
                super(null, id, index, null, null, null, 0, 0, 0, null);
            }
        }

        /**
         * Builds an auxiliary graph for the application of edge-disjoint shortest-path pair algorithms to node-disjoint problems.
         *
         * @param graph           Graph representing the network
         * @param originNode      Origin node
         * @param destinationNode Destination node
         * @return Auxiliary graph
         */
        public static Graph<Node, Link> buildAuxiliaryNodeDisjointGraph(final Graph<Node, Link> graph, Node originNode, Node destinationNode) {
            Graph<Node, Link> auxGraph = new DirectedSparseMultigraph<Node, Link>();
            Map<Node, Node> originalNodeId2AuxIdMapping = new LinkedHashMap<Node, Node>();

            for (Node vertex : graph.getVertices()) {
                auxGraph.addVertex(vertex);

                if (vertex.equals(originNode) || vertex.equals(destinationNode)) {
                    originalNodeId2AuxIdMapping.put(vertex, vertex);
                } else {
                    DummyNode auxVertex = new DummyNode(-10000 + vertex.getIndex(), -10000 + vertex.getIndex());
                    originalNodeId2AuxIdMapping.put(vertex, auxVertex);
                    DummyLink auxEdge = new DummyLink(-10000 + vertex.getIndex(), -10000 + vertex.getIndex());
                    auxGraph.addVertex(auxVertex);
                    auxGraph.addEdge(auxEdge, vertex, auxVertex);
                }
            }

            for (Link edge : graph.getEdges()) {
                if (edge.getId() < 0)
                    throw new Net2PlanException("Bad - Edge indexes must be greater or equal than zero");

                switch (graph.getEdgeType(edge)) {
                    case DIRECTED:
                        Node originNode_thisLink = originalNodeId2AuxIdMapping.get(graph.getSource(edge));
                        Node destinationNode_thisLink = graph.getDest(edge);
                        auxGraph.addEdge(edge, originNode_thisLink, destinationNode_thisLink);
                        break;

                    default:
                        throw new Net2PlanException("This method is only for directed graphs");
                }
            }

            return auxGraph;
        }

        /**
         * Builds an auxiliary graph for the application of edge-disjoint shortest-path pair algorithms to node-disjoint problems.
         *
         * @param graph           Graph representing the network
         * @param nev             Object responsible for returning weights for edges
         * @param originNode      Origin node
         * @param destinationNode Destination node
         * @return Auxiliary graph, and its corresponding object returning edge weights
         */
        public static Pair<Graph<Node, Link>, Transformer<Link, Double>> buildAuxiliaryNodeDisjointGraph(final Graph<Node, Link> graph, final Transformer<Link, Double> nev, Node originNode, Node destinationNode) {
            final Graph<Node, Link> auxGraph = buildAuxiliaryNodeDisjointGraph(graph, originNode, destinationNode);

            Transformer<Link, Double> auxNev = new Transformer<Link, Double>() {
                @Override
                public Double transform(Link edge) {
                    if (graph.containsEdge(edge)) {
                        if (edge.getId() < 0) throw new RuntimeException("Bad");
                        return nev.transform(edge);
                    } else if (auxGraph.containsEdge(edge)) {
                        return 1.0;
                    }

                    throw new RuntimeException("Bad");
                }
            };

            return Pair.of(auxGraph, auxNev);
        }

        /**
         * Creates an image of the network topology. The returned image will be trimmed to remove white borders.
         *
         * @param <V>                       Vertex type
         * @param <E>                       Edge type
         * @param graph                     Graph representing the network
         * @param vertexPositionTransformer Object responsible for returning position for vertices
         * @param width                     Expected maximum width (it may be lower due to white borders removal)
         * @param height                    Expected maximum height (it may be lower due to white borders removal)
         * @param fontSize                  Font size for vertex label (in points)
         * @param nodeSize                  Node size
         * @param linkThickness             Link thickness
         * @return Image
         */
        public static <V, E> BufferedImage createImage(Graph<V, E> graph, final Transformer<V, Point2D> vertexPositionTransformer, int width, int height, int fontSize, final float nodeSize, final float linkThickness) {
            Transformer<V, Point2D> actualVertexPositionTransformer = new Transformer<V, Point2D>() {
                @Override
                public Point2D transform(V input) {
                    Point2D p = new Point2D.Double();
                    p.setLocation(vertexPositionTransformer.transform(input));
                    p.setLocation(p.getX(), -p.getY());

                    return p;
                }
            };

            Layout<V, E> layout = new StaticLayout<V, E>(graph, actualVertexPositionTransformer, new Dimension(width, height));
            VisualizationImageServer<V, E> vis = new VisualizationImageServer<V, E>(layout, layout.getSize());
            vis.setBackground(Color.WHITE);
            vis.getRenderContext().setEdgeArrowPredicate(new NoArrowPredicate<V, E>());
            vis.getRenderContext().setEdgeShapeTransformer(new EdgeShape.Line<V, E>());
            vis.getRenderContext().setEdgeStrokeTransformer(new ConstantTransformer(new BasicStroke(linkThickness)));
            vis.getRenderContext().setVertexDrawPaintTransformer(new ConstantTransformer(Color.BLACK));
            vis.getRenderContext().setVertexFillPaintTransformer(new ConstantTransformer(Color.BLACK));
            vis.getRenderContext().setVertexFontTransformer(new ConstantTransformer(new Font("Helvetica", Font.BOLD, fontSize)));
            vis.getRenderContext().setVertexLabelRenderer(new MyDefaultVertexLabelRenderer<V>(Color.WHITE));
            vis.getRenderContext().setVertexLabelTransformer(new ToStringLabeller<V>());
            vis.getRenderContext().setVertexShapeTransformer(new Transformer<V, Shape>() {
                @Override
                public Shape transform(V vertex) {
                    return new Ellipse2D.Double(-nodeSize / 2, -nodeSize / 2, nodeSize, nodeSize);
                }
            });

            vis.getRenderer().getVertexLabelRenderer().setPosition(Renderer.VertexLabel.Position.CNTR);

            double aux_xmax = Double.NEGATIVE_INFINITY;
            double aux_xmin = Double.POSITIVE_INFINITY;
            double aux_ymax = Double.NEGATIVE_INFINITY;
            double aux_ymin = Double.POSITIVE_INFINITY;

            for (V vertex : graph.getVertices()) {
                Point2D aux = layout.transform(vertex);
                if (aux_xmax < aux.getX()) aux_xmax = aux.getX();
                if (aux_xmin > aux.getX()) aux_xmin = aux.getX();
                if (aux_ymax < aux.getY()) aux_ymax = aux.getY();
                if (aux_ymin > aux.getY()) aux_ymin = aux.getY();
            }

            final double PRECISION_FACTOR = 0.00001;
            Rectangle viewInLayoutUnits = vis.getRenderContext().getMultiLayerTransformer().inverseTransform(vis.getBounds()).getBounds();
            float ratio_h = Math.abs(aux_xmax - aux_xmin) < PRECISION_FACTOR ? 1 : (float) (viewInLayoutUnits.getWidth() / (aux_xmax - aux_xmin));
            float ratio_v = Math.abs(aux_ymax - aux_ymin) < PRECISION_FACTOR ? 1 : (float) (viewInLayoutUnits.getHeight() / (aux_ymax - aux_ymin));
            float ratio = (float) (0.8 * Math.min(ratio_h, ratio_v));
            LayoutScalingControl scalingControl = new LayoutScalingControl();
            scalingControl.scale(vis, ratio, vis.getCenter());

            Point2D q = new Point2D.Double((aux_xmin + aux_xmax) / 2, (aux_ymin + aux_ymax) / 2);
            Point2D lvc = vis.getCenter();
            double dx = (lvc.getX() - q.getX());
            double dy = (lvc.getY() - q.getY());
            vis.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.LAYOUT).translate(dx, dy);

            BufferedImage bi = (BufferedImage) vis.getImage(new Point2D.Double(layout.getSize().getWidth() / 2, layout.getSize().getHeight() / 2), new Dimension(layout.getSize()));
            return ImageUtils.trim(bi);
        }

        /**
         * <p>Filters a graph maintaining/removing some vertices/edges. In the same invocation, user must choose between 'accept' or 'block' some elements (vertices and/or edges).</p>
         * <p>
         * <p><b>Important</b>: Returned graph is not backed in the input one, so changes will not be reflected on it.
         *
         * @param <V>              Class type for vertices
         * @param <E>              Class type for edges
         * @param graph            Graph representing the network
         * @param acceptedVertices Collection of accepted vertices (null means empty)
         * @param blockedVertices  Collection of blocked vertices (null means empty)
         * @param acceptedEdges    Collection of accepted edges (null means empty)
         * @param blockedEdges     Collection of blocked edges (null means empty)
         * @return Filtered graph
         */
        public static <V, E> Graph<V, E> filterGraph(Graph<V, E> graph, final Collection<V> acceptedVertices, final Collection<V> blockedVertices, final Collection<E> acceptedEdges, final Collection<E> blockedEdges) {
            if (acceptedVertices != null && blockedVertices != null)
                throw new Net2PlanException("Over-specified filter: only accepted or blocked vertices can be specified at the same time");
            if (acceptedEdges != null && blockedEdges != null)
                throw new Net2PlanException("Over-specified filter: only accepted or blocked edges can be specified at the same time");

            Graph<V, E> filteredGraph;
            try {
                filteredGraph = graph.getClass().newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException("Unable to create a copy of existing graph: ", e);
            }

            if (acceptedVertices != null) {
                for (V vertex : graph.getVertices())
                    if (acceptedVertices.contains(vertex)) filteredGraph.addVertex(vertex);
            } else if (blockedVertices != null && !blockedVertices.isEmpty()) {
                for (V vertex : graph.getVertices())
                    if (!blockedVertices.contains(vertex)) filteredGraph.addVertex(vertex);
            } else {
                for (V vertex : graph.getVertices())
                    filteredGraph.addVertex(vertex);
            }

            if (acceptedEdges != null) {
                for (E edge : graph.getEdges()) {
                    if (!acceptedEdges.contains(edge)) continue;
                    Collection<V> incident = graph.getIncidentVertices(edge);

                    boolean allIncidents = true;
                    for (V vertex : incident) {
                        if (!filteredGraph.containsVertex(vertex)) {
                            allIncidents = false;
                            break;
                        }
                    }

                    if (allIncidents) filteredGraph.addEdge(edge, incident);
                }
            } else if (blockedEdges != null && !blockedEdges.isEmpty()) {
                for (E edge : graph.getEdges()) {
                    if (blockedEdges.contains(edge)) continue;

                    Collection<V> incident = graph.getIncidentVertices(edge);

                    boolean allIncidents = true;
                    for (V vertex : incident) {
                        if (!filteredGraph.containsVertex(vertex)) {
                            allIncidents = false;
                            break;
                        }
                    }

                    if (allIncidents) filteredGraph.addEdge(edge, incident);
                }
            } else {
                for (E edge : graph.getEdges()) {
                    Collection<V> incident = graph.getIncidentVertices(edge);

                    boolean allIncidents = true;
                    for (V vertex : incident) {
                        if (!filteredGraph.containsVertex(vertex)) {
                            allIncidents = false;
                            break;
                        }
                    }

                    if (allIncidents) filteredGraph.addEdge(edge, incident);
                }
            }

            return filteredGraph;
        }

        /**
         * <p>Filters a graph removing those edges whose weight is equal to {@code Double.MAX_VALUE}.</p>
         * <p>
         * <p><b>Important</b>: Returned graph is not backed in the input one, so changes will not be reflected on it.
         *
         * @param <V>   Class type for vertices
         * @param <E>   Class type for edges
         * @param graph Graph representing the network
         * @param nev   Object responsible for returning weights for edges
         * @return Filtered graph
         */
        public static <V, E> Graph<V, E> filterGraph(Graph<V, E> graph, final Transformer<E, Double> nev) {
            return filterGraph(graph, nev, null, 0);
        }

        /**
         * <p>Filters a graph removing those edges whose weight is equal to {@code Double.MAX_VALUE}, or whose capacity is below a certain threshold.</p>
         * <p>
         * <p><b>Important</b>: Returned graph is not backed in the input one, so changes will not be reflected on it.
         *
         * @param <V>                          Class type for vertices
         * @param <E>                          Class type for edges
         * @param graph                        Graph representing the network
         * @param nev                          Object responsible for returning weights for edges
         * @param edgeSpareCapacityTransformer Object responsible for returning capacity for edges (if null, it will not applied)
         * @param requiredCapacity             Capacity threshold. Edges whose capacity is below that value it will be removed
         * @return Filtered graph
         */
        public static <V, E> Graph<V, E> filterGraph(Graph<V, E> graph, final Transformer<E, Double> nev, final Transformer<E, Double> edgeSpareCapacityTransformer, final double requiredCapacity) {
            EdgePredicateFilter<V, E> linkFilter = new EdgePredicateFilter<V, E>(new Predicate<E>() {
                @Override
                public boolean evaluate(E edge) {
                    if (nev != null && nev.transform(edge) == Double.MAX_VALUE)
                        return false;
                    else if (edgeSpareCapacityTransformer != null && edgeSpareCapacityTransformer.transform(edge) < requiredCapacity)
                        return false;

                    return true;
                }
            });

            return linkFilter.transform(graph);
        }

        /**
         * Returns the shortest path that fulfills a given minimum capacity requirement along its traversed edges. In case no path can be found, an empty list will be returned.
         *
         * @param <V>                          Class type for vertices
         * @param <E>                          Class type for edges
         * @param graph                        Graph representing the network
         * @param nev                          Object responsible for returning weights for edges
         * @param originNodeId                 Origin node
         * @param destinationNodeId            Destination node
         * @param edgeSpareCapacityTransformer Object responsible for returning capacity for edges (if null, it will not applied)
         * @param requiredCapacity             Capacity threshold. Edges whose capacity is below that value it will be removed
         * @return Shortest path fulfilling a minimum capacity requirement
         */
        public static <V, E> List<E> getCapacitatedShortestPath(Graph<V, E> graph, Transformer<E, Double> nev, V originNodeId, V destinationNodeId, Transformer<E, Double> edgeSpareCapacityTransformer, double requiredCapacity) {
            if (!graph.containsVertex(originNodeId)) return new LinkedList<E>();
            if (!graph.containsVertex(destinationNodeId)) return new LinkedList<E>();

            if (nev == null) nev = getEdgeWeightTransformer(null);

            Graph<V, E> filteredGraph = filterGraph(graph, nev, edgeSpareCapacityTransformer, requiredCapacity);
            DijkstraShortestPath<V, E> dsp = new DijkstraShortestPath<V, E>(filteredGraph, nev);
            List<E> path = dsp.getPath(originNodeId, destinationNodeId);
            return path;
        }

        /**
         * Obtains a transformer for returning link weight from link identifier.
         *
         * @param <E>       Edge type
         * @param linkCosts A mapping of edges to weights (null means 'all-one' map)
         * @return A transformer for returning weights for edges
         */
        public static <E> Transformer<E, Double> getEdgeWeightTransformer(final Map<E, Double> linkCosts) {
            if (linkCosts == null) {
                return new Transformer<E, Double>() {
                    @Override
                    public Double transform(E edge) {
                        return 1.0;
                    }
                };
            } else {
                return new Transformer<E, Double>() {
                    @Override
                    public Double transform(E edge) {
                        Double value = linkCosts.get(edge);
                        if (value == null) throw new Net2PlanException("Bad - No weight for link " + edge);
                        return value;
                    }
                };
            }
        }

        /**
         * <p>Obtains a {@code JUNG} graph from a given set of links.</p>
         *
         * @param nodes Collection of nodes
         * @param links Collection of links
         * @return {@code JUNG} graph
         */
        public static Graph<Node, Link> getGraphFromLinkMap(Collection<Node> nodes, Collection<Link> links)//Map<Long, Pair<Long, Long>> linkMap)
        {
            Graph<Node, Link> graph = new DirectedOrderedSparseMultigraph<Node, Link>();

            for (Node node : nodes)
                graph.addVertex(node);

            if (links != null) {
                for (Link e : links) {
                    if (!graph.containsVertex(e.getOriginNode()))
                        throw new RuntimeException("Bad"); //graph.addVertex(e.getOriginNode());
                    if (!graph.containsVertex(e.getDestinationNode()))
                        throw new RuntimeException("Bad"); //graph.addVertex(e.getDestinationNode());
                    graph.addEdge(e, e.getOriginNode(), e.getDestinationNode());
                }
            }

            return graph;
        }

        /** <p>Obtains a <code>JUNG</code> graph from a given link map.</p>
         *
         * @param nodes Collection of nodes
         * @param linkMap Map of links, where the key is the unique link identifier and the value is a {@link com.net2plan.utils.Pair Pair} representing the origin node and the destination node of the link, respectively. Origin/destination nodes will be added as needed
         * @return <code>JUNG</code> graph */
        //		public static Graph<Long, Long> getGraphFromLinkMap(Collection<Long> nodes , Map<Long, Pair<Long, Long>> linkMap)
        //		{
        //			Graph<Long, Long> graph = new DirectedOrderedSparseMultigraph<Long, Long>();
        //
        //			for (long nodeId : nodes) graph.addVertex(nodeId);
        //
        //			if (linkMap != null)
        //			{
        //				for(Entry<Long, Pair<Long, Long>> entry : linkMap.entrySet())
        //				{
        //					long linkId = entry.getKey();
        //					long originNodeId = entry.getValue().getFirst();
        //					long destinationNodeId = entry.getValue().getSecond();
        //
        //					if (!graph.containsVertex(originNodeId)) throw new RuntimeException ("Bad"); //graph.addVertex(originNodeId);
        //					if (!graph.containsVertex(destinationNodeId)) throw new RuntimeException ("Bad"); // graph.addVertex(destinationNodeId);
        //
        //					graph.addEdge(linkId, originNodeId, destinationNodeId);
        //				}
        //			}
        //
        //			return graph;
        //		}

        /**
         * Returns the K-loopless shortest paths between two nodes. If <i>n</i> shortest paths are found (n&lt;K), those are returned.
         *
         * @param <V>               Class type for vertices
         * @param <E>               Class type for edges
         * @param graph             Graph representing the network
         * @param nev               Object responsible for returning weights for edges
         * @param originNodeId      Origin node
         * @param destinationNodeId Destination node
         * @param K                 Number of different paths
         * @return K-shortest paths
         */
        public static <V, E> List<List<E>> getKLooplessShortestPaths(Graph<V, E> graph, Transformer<E, Double> nev, V originNodeId, V destinationNodeId, int K) {
            if (!graph.containsVertex(originNodeId)) return new LinkedList<List<E>>();
            if (!graph.containsVertex(destinationNodeId)) return new LinkedList<List<E>>();

            if (nev == null) nev = getEdgeWeightTransformer(null);

            Graph<V, E> filteredGraph = filterGraph(graph, nev);
            YenAlgorithm<V, E> paths = new YenAlgorithm<V, E>(filteredGraph, nev);
            List<List<E>> pathList = paths.getPaths(originNodeId, destinationNodeId, K);
            return pathList;
        }

        /**
         * Returns the weight of a path given the sequence of edges.
         *
         * @param <E>  Class type for edges
         * @param path Sequence of edges
         * @param nev  Object responsible for returning weights for edges
         * @return Path weight
         */
        public static <E> double getPathWeight(List<E> path, Transformer<E, Double> nev) {
            double pathWeight = 0;
            if (nev == null) nev = getEdgeWeightTransformer(null);
            for (E edge : path)
                pathWeight += nev.transform(edge);

            return pathWeight;
        }

        /**
         * Returns the shortest path between two nodes using Dijkstra's algorithm.
         *
         * @param <V>               Vertex type
         * @param <E>               Edge type
         * @param graph             Graph representing the network
         * @param nev               Object responsible for returning weights for edges
         * @param originNodeId      Origin node
         * @param destinationNodeId Destination node
         * @return Shortest path
         */
        public static <V, E> List<E> getShortestPath(Graph<V, E> graph, Transformer<E, Double> nev, V originNodeId, V destinationNodeId) {
            return getCapacitatedShortestPath(graph, nev, originNodeId, destinationNodeId, null, 0);
        }

        /**
         * Returns the shortest pair of link-disjoint paths, where each item represents a path. The number of returned items will be equal to the number of paths found: when empty, no path was found; when {@code size()} = 1, only one path was found; and when {@code size()} = 2, the link-disjoint paths were found. Internally it uses the Suurballe-Tarjan algorithm.
         *
         * @param <V>               Vertex type
         * @param <E>               Edge type
         * @param graph             Graph representing the network
         * @param nev               Object responsible for returning weights for edges
         * @param originNodeId      Origin node
         * @param destinationNodeId Destination node
         * @return Shortest pair of link-disjoint paths
         */
        public static <V, E> List<List<E>> getTwoLinkDisjointPaths(Graph<V, E> graph, Transformer<E, Double> nev, V originNodeId, V destinationNodeId) {
            SuurballeTarjanAlgorithm<V, E> suurballeTarjanAlgorithm = new SuurballeTarjanAlgorithm<V, E>(graph, nev);
            return suurballeTarjanAlgorithm.getDisjointPaths(originNodeId, destinationNodeId);
        }

        /**
         * Returns the shortest pair of node-disjoint paths, where each item represents a path. The number of returned items will be equal to the number of paths found: when empty, no path was found; when {@code size()} = 1, only one path was found; and when {@code size()} = 2, the node-disjoint paths were found. Internally it uses the Suurballe-Tarjan algorithm.
         *
         * @param graph           Graph representing the network
         * @param nev             Object responsible for returning weights for edges
         * @param originNode      Origin node
         * @param destinationNode Origin node
         * @return Shortest pair of node-disjoint paths
         */
        public static List<List<Link>> getTwoNodeDisjointPaths(final Graph<Node, Link> graph, final Transformer<Link, Double> nev, Node originNode, Node destinationNode) {
            List<List<Link>> nodeDisjointSPs = new LinkedList<List<Link>>();
            if (graph.getVertexCount() < 2 || !graph.containsVertex(originNode) || !graph.containsVertex(destinationNode))
                return nodeDisjointSPs;

            Pair<Graph<Node, Link>, Transformer<Link, Double>> aux = buildAuxiliaryNodeDisjointGraph(graph, nev, originNode, destinationNode);
            Graph<Node, Link> auxGraph = aux.getFirst();
            Transformer<Link, Double> auxNev = aux.getSecond();

            nodeDisjointSPs = getTwoLinkDisjointPaths(auxGraph, auxNev, originNode, destinationNode);
            for (List<Link> auxSP : nodeDisjointSPs) {
                Iterator<Link> it = auxSP.iterator();
                while (it.hasNext()) {
                    Link edge = it.next();
                    if (!graph.containsEdge(edge)) it.remove();
                }
            }

            return nodeDisjointSPs;
        }

        /**
         * Check whether the topology has the same number of links between each node pair in both directions (assuming multi-digraphs).
         *
         * @param graph The graph to analyze
         * @return {@code true} if the graph is bidirectional, and false otherwise
         */
        public static boolean isBidirectional(Graph graph) {
            Object[] vertices = graph.getVertices().toArray();
            for (int v1 = 0; v1 < vertices.length; v1++) {
                for (int v2 = v1 + 1; v2 < vertices.length; v2++) {
                    if (graph.findEdgeSet(vertices[v1], vertices[v2]).size() != graph.findEdgeSet(vertices[v2], vertices[v1]).size())
                        return false;
                }
            }

            return true;
        }

        /**
         * Check whether the graph is simple, that is, if it has at most one link between each node pair (one per direction under directed graphs, one under undirected graphs).
         *
         * @param graph The graph to analyze
         * @return {@code true} if the graph is simple, and false otherwise
         */
        public static boolean isSimple(Graph graph) {
            Object[] vertices = graph.getVertices().toArray();
            for (int v1 = 0; v1 < vertices.length; v1++) {
                for (int v2 = v1 + 1; v2 < vertices.length; v2++) {
                    if (graph.findEdgeSet(vertices[v1], vertices[v2]).size() > 1) return false;
                    if (graph.findEdgeSet(vertices[v2], vertices[v1]).size() > 1) return false;
                }
            }

            return true;
        }

        /**
         * Checks whether the graph has the same number of links between each node pair in both directions (assuming multi-digraphs) and same individual weights per direction.
         *
         * @param <V>   Vertex type
         * @param <E>   Edge type
         * @param graph The graph to analyze
         * @param nev   Object responsible for returning weights for edges
         * @return {@code true} if the graph is weighted-bidirectional, and false otherwise. By convention returns {@code false} if network is empty
         */
        public static <V, E> boolean isWeightedBidirectional(Graph<V, E> graph, Transformer<E, Double> nev) {
            Object[] vertices = graph.getVertices().toArray();
            if (vertices.length == 0) return false;

            for (int v1 = 0; v1 < vertices.length; v1++) {
                for (int v2 = v1 + 1; v2 < vertices.length; v2++) {
                    Collection<E> links_12 = graph.findEdgeSet((V) vertices[v1], (V) vertices[v2]);
                    Collection<E> links_21 = graph.findEdgeSet((V) vertices[v2], (V) vertices[v1]);

                    if (links_12.size() != links_21.size()) return false;

                    Iterator<E> it_12 = links_12.iterator();
                    while (it_12.hasNext()) {
                        E aux_12 = it_12.next();

                        Iterator<E> it_21 = links_21.iterator();
                        while (it_21.hasNext()) {
                            E aux_21 = it_21.next();

                            if (Math.abs(nev.transform(aux_12) - nev.transform(aux_21)) < 1e-10) {
                                it_12.remove();
                                it_21.remove();
                                break;
                            }
                        }
                    }

                    if (!links_12.isEmpty() || !links_12.isEmpty()) return false;
                }
            }

            return true;
        }

        /**
         * Given an input graph that may contain multiple edges between some vertex pairs, returns a new graph where only appears, for each vertex pair, the edge having the lowest weight (edges whose weight is equal to {@code Double.MAX_VALUE} are excluded). Ties are broken arbitrarely.
         * <p>
         * <p><b>Important</b>: Returned graph is not backed in the input one, so changes will not be reflected on it.
         *
         * @param <V>   Vertex type
         * @param <E>   Edge type
         * @param graph Graph representing the network
         * @param nev   Object responsible for returning weights for edges
         * @return Copy of the input graph with at most an edge between each vertex pair
         */
        public static <V, E> Graph<V, E> simplifyGraph(Graph<V, E> graph, Transformer<E, Double> nev) {
            Collection<V> vertices = graph.getVertices();
            Set<E> edgesToMaintain = new LinkedHashSet<E>();
            for (V originVertex : vertices) {
                for (V destinationVertex : vertices) {
                    if (originVertex == destinationVertex) continue;

                    Collection<E> edges = graph.findEdgeSet(originVertex, destinationVertex);
                    if (edges.isEmpty()) continue;

                    E bestEdge = null;
                    double bestWeight = Double.MAX_VALUE;

                    for (E edge : edges) {
                        double weight_thisEdge = nev.transform(edge);
                        if (weight_thisEdge < bestWeight) {
                            bestWeight = weight_thisEdge;
                            bestEdge = edge;
                        }
                    }

                    if (bestEdge != null) edgesToMaintain.add(bestEdge);
                }
            }

            return JUNGUtils.filterGraph(graph, null, null, edgesToMaintain, null);
        }

        private static class MyDefaultVertexLabelRenderer<V> extends DefaultVertexLabelRenderer {
            private final Color color;

            public MyDefaultVertexLabelRenderer(Color color) {
                super(color);
                this.color = color;
            }

            @Override
            public <V> Component getVertexLabelRendererComponent(JComponent vv, Object value, Font font, boolean isSelected, V vertex) {
                Component c = super.getVertexLabelRendererComponent(vv, value, font, isSelected, vertex);
                c.setForeground(color);

                return c;
            }
        }

        private static class NoArrowPredicate<V, E> implements Predicate<Context<Graph<V, E>, E>> {
            @Override
            public boolean evaluate(Context<Graph<V, E>, E> context) {
                return false;
            }
        }
    }

    /**
     * <p>Class to calculate the shortest link-disjoint path pair between two nodes using Suurballe-Tarjan's algorithm.</p>
     * <p>
     * <p>Reference: {@code J.W. Suurballe, R.E. Tarjan, "A Quick Method for Finding Shortest Pairs of Disjoint Paths," <i>Networks</i>, vol. 14, no. 2, pp. 325-335, 1984}</p>
     *
     * @param <V> Vertex type
     * @param <E> Edge type
     * @author Pablo Pavon-Marino, Jose-Luis Izquierdo Zaragoza
     */
    private static class SuurballeTarjanAlgorithm<V, E> {
        private final Graph<V, E> graph;
        private final Transformer<E, Double> nev;
        private final DijkstraShortestPath<V, E> dijkstra;
        private final boolean cached;

        /**
         * Default constructor. Previous results from the shortest-path algorithm are cached.
         *
         * @param graph Graph on which shortest paths are searched
         * @param nev   The class responsible for returning weights for edges
         */
        private SuurballeTarjanAlgorithm(Graph<V, E> graph, Transformer<E, Double> nev) {
            this(graph, nev, true);
        }

        /**
         * This constructor allows to configure if the shortest-path algorithm should cached previous computations.
         *
         * @param graph  Graph on which shortest paths are searched
         * @param nev    The class responsible for returning weights for edges
         * @param cached Indicates whether previous computations from the shortest-path algorithm should be cached
         */
        private SuurballeTarjanAlgorithm(Graph<V, E> graph, Transformer<E, Double> nev, boolean cached) {
            this.graph = graph;
            this.nev = nev;
            this.cached = cached;

            dijkstra = new DijkstraShortestPath<V, E>(graph, nev, cached);
        }

        /**
         * <p>Returns the shortest link-disjoint path pair (in increasing order of weight).</p> <p><b>Important</b>: If only one path can be found, only such a path will be returned.</p>
         *
         * @param startVertex Start vertex of the calculated paths
         * @param endVertex   Target vertex of the calculated paths
         * @return List of paths in increasing order of weight
         */
        private List<List<E>> getDisjointPaths(V startVertex, V endVertex) {
            List<List<E>> linkDisjointSPs = new LinkedList<List<E>>();

            if (!graph.containsVertex(startVertex) || !graph.containsVertex(endVertex) || startVertex.equals(endVertex))
                return linkDisjointSPs;

			/* If target is not reachable, return */
            if (dijkstra.getDistance(startVertex, endVertex) == null) return linkDisjointSPs;

            List<E> sp = dijkstra.getPath(startVertex, endVertex);

			/* Determine length of shortest path from "source" to any other node */
            Map<V, Number> lengthMap = dijkstra.getDistanceMap(startVertex);

			/* Length transformation */
            Transformer<E, Double> lengthTrans = lengthTransformation(graph, MapTransformer.getInstance(lengthMap));

			/* Get shortest path in g with reversed shortest path... */
            Graph<V, E> revG = reverseEdges(graph, sp);
            DijkstraShortestPath<V, E> revDijkstra = new DijkstraShortestPath<V, E>(revG, lengthTrans, cached);

            Number revDistance = revDijkstra.getDistance(startVertex, endVertex);
            if (revDistance == null || revDistance.doubleValue() == Double.MAX_VALUE) {
				/* no alternate path, return */
                linkDisjointSPs.add(sp);
                return linkDisjointSPs;
            }

            List<E> revSp = revDijkstra.getPath(startVertex, endVertex);

            validatePath(graph, startVertex, endVertex, sp);
            validatePath(revG, startVertex, endVertex, revSp);

            List<E> spCopy = new LinkedList<E>(sp);
            List<List<E>> paths = findDisjointPaths(sp, revSp);

            if (paths == null) {
				/* no disjoint solution found, just return shortest path */
                linkDisjointSPs.add(spCopy);
                return linkDisjointSPs;
            }

			/* Check path validity */
            for (List<E> path : paths)
                validatePath(graph, startVertex, endVertex, path);

            return paths;
        }

        private static <V, E> void validatePath(Graph<V, E> graph, V source, V target, List<E> path) {
            if (!graph.isSource(source, path.get(0)))
                throw new RuntimeException("Bad - Source node is not the first node in the path");

            Iterator<E> it = path.iterator();
            E originVertex = it.next();

            while (it.hasNext()) {
                E destinationVertex = it.next();
                if (!graph.isSource(graph.getDest(originVertex), destinationVertex))
                    throw new RuntimeException("Bad - Path is not contiguous");

                originVertex = destinationVertex;
            }

            if (!graph.isDest(target, path.get(path.size() - 1))) throw new RuntimeException("Bad - ");
        }

        /**
         * Combines two disjoint paths from two SuurballeTarjan input paths.
         *
         * @param path1 Dijkstra shortest path
         * @param path2 Dijkstra shortest path in partly reverted graph
         * @return the two disjoint paths
         * @since 0.3.0
         */
        private List<List<E>> findDisjointPaths(List<E> path1, List<E> path2) {
            final V source = graph.getSource(path1.get(0));
            final V target = graph.getDest(path1.get(path1.size() - 1));

			/* First, remove common links */
            Iterator<E> path1_it = path1.iterator();
            while (path1_it.hasNext()) {
                E e1 = path1_it.next();

                Iterator<E> path2_it = path2.iterator();
                while (path2_it.hasNext()) {
                    E e2 = path2_it.next();

                    if (e1.equals(e2)) {
                        if (graph.isSource(source, e1) || graph.isSource(source, e2) || graph.isDest(target, e1) || graph.isDest(target, e2))
                            return null;

                        path1_it.remove();
                        path2_it.remove();
                        break;
                    }
                }
            }

			/* no disjoint solution found */
            if (path1.isEmpty() || path2.isEmpty()) return null;

			/* Now recombine the two paths */
            List<E> union = ListUtils.union(path1, path2); /* concatenate */

            List<E> p1 = recombinePaths(path1, target, union);
            if (p1 == null) return null;

            List<E> p2 = recombinePaths(path2, target, union);
            if (p2 == null) return null;

            //			if (!union.isEmpty()) throw new RuntimeException("Bad"); /* ToDo: It is an error? */

            List<List<E>> solution = new LinkedList<List<E>>();

            double path1_cost = 0;
            for (E edge : p1)
                path1_cost += nev.transform(edge);

            double path2_cost = 0;
            for (E edge : p2)
                path2_cost += nev.transform(edge);

            if (path1_cost <= path2_cost) {
                solution.add(p1);
                solution.add(p2);
            } else {
                solution.add(p2);
                solution.add(p1);
            }

            return solution;
        }

        private List<E> recombinePaths(List<E> path, V target, List<E> union) {
            LinkedList<E> p = new LinkedList<E>(); /* provides getLast */
            p.add(path.get(0));
            union.remove(path.get(0));

            V curDest;
            while (!(curDest = graph.getDest(p.getLast())).equals(target)) {
                boolean progress = false;
                for (E e : union) {
                    if (graph.isSource(curDest, e)) {
                        p.add(e);
                        progress = true;
                        union.remove(e);
                        break;
                    }
                }

                if (!progress) return null;

                if (union.isEmpty()) {
                    if (!graph.isDest(target, p.getLast()))
                        throw new RuntimeException("Bad");
                    else
                        break;
                }
            }
            return p;
        }

        /**
         * This method reverse the path "path" in the graph "graph" and returns it.
         *
         * @param graph the input graph which will not be changed.
         * @param path  the path to reverse
         * @return a new graph with the reversed path
         * @since 0.3.0
         */
        private static <V, E> Graph<V, E> reverseEdges(Graph<V, E> graph, List<E> path) {
            if (graph == null || path == null) throw new IllegalArgumentException();
            Graph<V, E> clone = new DirectedOrderedSparseMultigraph<V, E>();

            for (V v : graph.getVertices())
                clone.addVertex(v);
            for (E e : graph.getEdges())
                clone.addEdge(e, graph.getEndpoints(e));

            for (E link : path) {
                V src = clone.getSource(link);
                V dst = clone.getDest(link);
                clone.removeEdge(link);
                clone.addEdge(link, dst, src, EdgeType.DIRECTED);
            }

            return clone;
        }

        /**
         * This method does the following length transformation:
         * <p>
         * <pre> c'(v,w) = c(v,w) - d (s,w) + d (s,v) </pre>
         *
         * @param graph1  the graph
         * @param slTrans The shortest length transformer
         * @return the transformed graph
         * @since 0.3.0
         */
        private Transformer<E, Double> lengthTransformation(Graph<V, E> graph1, Transformer<V, Number> slTrans) {
            Map<E, Double> map = new LinkedHashMap<E, Double>();

            for (E link : graph1.getEdges()) {
                double newWeight;

                if (slTrans.transform(graph1.getSource(link)) == null) {
                    newWeight = Double.MAX_VALUE;
                } else {
                    newWeight = nev.transform(link) - slTrans.transform(graph1.getDest(link)).doubleValue() + slTrans.transform(graph1.getSource(link)).doubleValue();
                    if (newWeight < 0 || newWeight > -1e-6) newWeight = 0; /* Numerical errors */
                }

                map.put(link, newWeight);
            }

            return MapTransformer.getInstance(map);
        }
    }

    /**
     * <p>Class to calculate the (loopless) <i>k</i>-shortest paths between a node pair using Yen's algorithm.</p>
     * <p>
     * <p>Reference: {@code J.Y. Yen, "Finding the K Shortest Loopless Paths in a Network," <i>Management Science</i>, vol. 17, no. 11, pp. 712-716, Jul. 1971}</p>
     *
     * @param <V> Vertex type
     * @param <E> Edge type
     * @author Pablo Pavon-Marino, Jose-Luis Izquierdo Zaragoza
     */
    static class YenAlgorithm<V, E> {
        private final Graph<V, E> graph;
        private final Transformer<E, Double> nev;
        private final DijkstraShortestPath<V, E> dijkstra;

        protected int K;
        protected double maxLengthInKm;
        protected int maxNumHops;
        protected double maxPropDelayInMs;
        protected double maxRouteCost;
        protected double maxRouteCostFactorRespectToShortestPath;
        protected double maxRouteCostRespectToShortestPath;

        /**
         * Default constructor.
         *
         * @param graph Graph on which shortest paths are searched
         * @param nev   The class responsible for returning weights for edges
         */
        private YenAlgorithm(Graph<V, E> graph, Transformer<E, Double> nev, int K, int maxNumHops, double maxLengthInKm, double maxPropDelayInMs, double maxRouteCost, double maxRouteCostFactorRespectToShortestPath, double maxRouteCostRespectToShortestPath) {
            this.K = K;
            this.maxLengthInKm = maxLengthInKm;
            this.maxNumHops = maxNumHops;
            this.maxPropDelayInMs = maxPropDelayInMs;
            this.maxRouteCost = maxRouteCost;
            this.maxRouteCostFactorRespectToShortestPath = maxRouteCostFactorRespectToShortestPath;
            this.maxRouteCostRespectToShortestPath = maxRouteCostRespectToShortestPath;
            if (nev == null) {
                this.graph = graph;
                this.nev = JUNGUtils.getEdgeWeightTransformer(null);
            } else {
                this.nev = nev;
                this.graph = JUNGUtils.filterGraph(graph, nev);
            }

            dijkstra = new DijkstraShortestPath<V, E>(graph, nev);
        }

        /**
         * Default constructor.
         *
         * @param graph Graph on which shortest paths are searched
         * @param nev   The class responsible for returning weights for edges
         */
        YenAlgorithm(Graph<V, E> graph, Transformer<E, Double> nev) {
            this.K = -1;
            this.maxLengthInKm = -1;
            this.maxNumHops = -1;
            this.maxPropDelayInMs = -1;
            this.maxRouteCost = -1;
            this.maxRouteCostFactorRespectToShortestPath = -1;
            this.maxRouteCostRespectToShortestPath = -1;
            if (nev == null) {
                this.graph = graph;
                this.nev = JUNGUtils.getEdgeWeightTransformer(null);
            } else {
                this.nev = nev;
                this.graph = JUNGUtils.filterGraph(graph, nev);
            }

            dijkstra = new DijkstraShortestPath<V, E>(graph, nev);
        }

        /**
         * <p>Returns the (loopless) <i>k</i>-shortest simple paths in increasing order of weight.</p> <p><b>Important</b>: If only <i>n</i> < <i>k</i> paths can be found, only such <i>n</i> paths will be returned.</p>
         *
         * @param startVertex Start vertex of the calculated paths
         * @param endVertex   Target vertex of the calculated paths
         * @param k           Number of paths to be computed
         * @return List of paths in increasing order of weight
         */
        private List<List<E>> getPaths(V startVertex, V endVertex, int k) {

            LinkedList<List<E>> paths = new LinkedList<List<E>>();

            if (!graph.containsVertex(startVertex) || !graph.containsVertex(endVertex) || startVertex.equals(endVertex))
                return paths;
            if (dijkstra.getDistance(startVertex, endVertex) == null) return paths;

            PriorityQueue<GraphPath> priorityQueue = new PriorityQueue<GraphPath>();
            List<E> aux = dijkstra.getPath(startVertex, endVertex);
            double cost = GraphUtils.JUNGUtils.getPathWeight(aux, nev);
            GraphPath<E> shortestPath = new GraphPath<E>(aux, cost);

            if (!acceptPath(shortestPath)) return paths;
            paths.add(aux);

            DijkstraShortestPath<V, E> blockedDijkstra;

            while (paths.size() < k) {
                List<E> curShortestPath = paths.getLast();

                int currentPathLength = curShortestPath.size();

				/* Split path into Head and NextEdge */
                for (int deviationId = 0; deviationId < currentPathLength; deviationId++) {
                    List<E> head = curShortestPath.subList(0, deviationId);
                    V deviationVertex = head.isEmpty() ? startVertex : graph.getDest(head.get(deviationId - 1));

					/* 1. Block edges */
                    Graph<V, E> blocked = blockFilter(head, deviationVertex, paths);

					/* 2. Get shortest path in graph with blocked edges */
                    blockedDijkstra = new DijkstraShortestPath<V, E>(blocked, nev);

                    Number dist = blockedDijkstra.getDistance(deviationVertex, endVertex);
                    if (dist == null) continue;

                    List<E> tail = blockedDijkstra.getPath(deviationVertex, endVertex);

					/* 3. Combine head and tail into new path */
                    List<E> candidatePath = new ArrayList<E>(deviationId + tail.size());
                    candidatePath.addAll(head);
                    candidatePath.addAll(tail);

                    GraphPath<E> candidate = new GraphPath<E>(candidatePath, JUNGUtils.getPathWeight(candidatePath, nev));

					/* Check if we already found this solution */
                    if (priorityQueue.contains(candidate)) continue;

                    if (!acceptPath(candidate) || !compareCandidateToShortestPath(candidate, shortestPath)) continue;

                    priorityQueue.add(candidate);
                }

                if (priorityQueue.isEmpty()) break; /* No more candidate paths */
                paths.add(priorityQueue.poll().getPath());
            }

            return paths;
        }

        /**
         * Blocks all incident edges of the vertices in head as well as the edge connecting head to the next node by creating a new filtered graph.
         *
         * @param head       The current head, from source to deviation node
         * @param deviation  The edge to the next node
         * @param foundPaths The solutions already found and to check against
         * @return The filtered graph without the blocked edges
         * @since 0.3.0
         */
        private Graph<V, E> blockFilter(List<E> head, V deviation, List<List<E>> foundPaths) {
            final Set<E> blocked = new LinkedHashSet<E>();

			/* Block incident edges to make all vertices in head unreachable. */
            for (E e : head) {
                for (E e2 : graph.getIncidentEdges(graph.getSource(e))) {
                    blocked.add(e2);
                }
            }

			/* Block all outgoing edges that have been used at deviation vertex */
            for (List<E> path : foundPaths) {
                if (path.size() > head.size() && ListUtils.isEqualList(path.subList(0, head.size()), head)) {
                    for (E e : path) {
                        if (graph.isSource(deviation, e)) {
                            blocked.add(e);
                            break; /* Continue with next path */
                        }
                    }
                }
            }

            return JUNGUtils.filterGraph(graph, null, null, null, blocked);
        }

        /**
         * Indicates whether a path is valid. It can be override to implement checks like maximum path length. In contrast, {@link #compareCandidateToShortestPath(GraphUtils.GraphPath, GraphUtils.GraphPath) compareCandidateToShortestPath()} method. method can be used to compare current candidate path with the shortest path.
         *
         * @param candidate Candidate path
         * @return {@code true} must be accepted. Otherwise, {@code false}
         */
        public boolean acceptPath(GraphPath<E> candidate) {
            return true;
        }

        /**
         * Indicates whether a path is valid when compared to the shortest path. It can be override to implement checks like maximum distance from the shortest path.
         *
         * @param candidate    Candidate path
         * @param shortestPath
         * @return {@code true} must be accepted. Otherwise, {@code false}
         */
        public boolean compareCandidateToShortestPath(GraphPath<E> candidate, GraphPath<E> shortestPath) {
            return true;
        }
    }

    /**
     * Exception thrown when hop-by-hop routing includes closed cycles.
     *
     * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
     */
    public static class ClosedCycleRoutingException extends Net2PlanException {
        private static final long serialVersionUID = 1L;

        /**
         * Default constructor.
         */
        public ClosedCycleRoutingException() {
            super();
        }

        /**
         * Constructs a new {@code Net2PlanException} exception with the specified detail message.
         *
         * @param message Message to be retrieved by the {@link #getMessage()} method.
         */
        public ClosedCycleRoutingException(String message) {
            super(message);
        }
    }
}
