/*******************************************************************************
 * Copyright (c) 2017 Pablo Pavon Marino and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the 2-clause BSD License 
 * which accompanies this distribution, and is available at
 * https://opensource.org/licenses/BSD-2-Clause
 *
 * Contributors:
 *     Pablo Pavon Marino and others - initial API and implementation
 *******************************************************************************/

package com.net2plan.libraries;

import cern.colt.matrix.tdouble.DoubleFactory1D;
import cern.colt.matrix.tdouble.DoubleFactory2D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import cern.colt.matrix.tdouble.algo.DenseDoubleAlgebra;
import cern.colt.matrix.tdouble.algo.decomposition.DenseDoubleEigenvalueDecomposition;
import cern.jet.math.tdouble.DoubleFunctions;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.libraries.GraphUtils.JGraphTUtils;
import com.net2plan.libraries.GraphUtils.JUNGUtils;
import com.net2plan.utils.CollectionUtils;
import com.net2plan.utils.Constants;
import com.net2plan.utils.DoubleUtils;
import edu.uci.ics.jung.algorithms.scoring.BetweennessCentrality;
import edu.uci.ics.jung.graph.Graph;
import org.apache.commons.collections15.Transformer;
import org.jgrapht.DirectedGraph;
import org.jgrapht.alg.EdmondsKarpMaximumFlow;
import org.jgrapht.alg.StrongConnectivityInspector;

import java.util.*;

/**
 * <p>Class to deal with graph-theory metrics computation.</p>
 *
 * <p><b>Important</b>: Internal computations (like shortest-paths) are cached in order to improve efficiency.</p>
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 */
@SuppressWarnings("unchecked")
public class GraphTheoryMetrics
{
	private final List<Node> nodes;
	private final List<Link> linkMap;
	private Map<Link, Double> costMap;
	private final int N;
	private final int E;
	private DoubleMatrix2D adjacencyMatrix;
	private double[] adjacencyMatrixEigenvalues;
	private double averageSPLength, diameter, heterogeneity;
	private DirectedGraph<Node, Link> graph_jgrapht;
	private Graph<Node, Link> graph_jung;
	private DoubleMatrix2D incidenceMatrix;
	private DoubleMatrix2D laplacianMatrix;
	private double[] laplacianMatrixEigenvalues;
	private Transformer<Link, Double> nev;
	
	private DoubleMatrix1D linkBetweenessCentrality;
	private DoubleMatrix1D nodeBetweenessCentrality;
	private DoubleMatrix1D outNodeDegree;

	/**
	 * Default constructor
	 * @param nodes List of odes
	 * @param links List of links
	 * @param linkCostMap Cost per link, where the key is the link identifier and the value is the cost of traversing the link. No special iteration-order (i.e. ascending) is required
	 */
	public GraphTheoryMetrics(List<Node> nodes, List<Link> links, Map<Link, Double> linkCostMap)
	{
		this.nodes = nodes;
		this.linkMap = links;
		this.N = nodes.size();
		this.E = links.size();

		adjacencyMatrix = null;
		adjacencyMatrixEigenvalues = null;
		graph_jgrapht = null;
		graph_jung = null;
		incidenceMatrix = null;
		laplacianMatrix = null;
		laplacianMatrixEigenvalues = null;
		linkBetweenessCentrality = null;
		nev = null;
		nodeBetweenessCentrality = null;
		outNodeDegree = null;

		configureLinkCostMap(linkCostMap);
	}

	private void computeBetweenessCentrality()
	{
		Graph<Node, Link> aux_graph = getGraph_JUNG();
		BetweennessCentrality bc = new BetweennessCentrality(aux_graph, getCostTransformer());			

		nodeBetweenessCentrality = DoubleFactory1D.dense.make (N);
		for (Node node : this.nodes) nodeBetweenessCentrality.set(node.getIndex () , bc.getVertexScore(node));

		linkBetweenessCentrality = DoubleFactory1D.dense.make (E);
		for (Link link : linkMap) linkBetweenessCentrality.set(link.getIndex (), bc.getEdgeScore(link));
	}
	
	private void computeSPDistanceMetrics()
	{
		diameter = 0;
		averageSPLength = 0;
		heterogeneity = 0;
		
		Graph<Node, Link> aux_graph = getGraph_JUNG();
		Transformer<Link, Double> aux_nev = getCostTransformer();

		/* Compute network diameter using naïve Floyd-Warshall algorithm */
		double[][] costMatrix = new double[N][N];
		for(int n = 0; n < N; n++)
		{
			Arrays.fill(costMatrix[n], Double.MAX_VALUE);
			costMatrix[n][n] = 0;
		}
		
		for(Link edge : aux_graph.getEdges())
		{
			int a_e = edge.getOriginNode().getIndex ();
			int b_e = edge.getDestinationNode().getIndex ();
			double newCost = aux_nev.transform(edge);
			if (newCost < costMatrix[a_e][b_e]) costMatrix[a_e][b_e] = newCost;
		}
		
		for(int k = 0; k < N; k++)
		{
			for(int i = 0; i < N; i++)
			{
				if (i == k) continue;
				
				for(int j = 0; j < N; j++)
				{
					if (j == k || j == i) continue;
					
					double newValue = costMatrix[i][k] + costMatrix[k][j];
					if (newValue < costMatrix[i][j]) costMatrix[i][j] = newValue;
				}
			}
		}
		
		int numPaths = 0;
		double sum = 0;
		double M = 0.0;
		double S = 0.0;

		for(int i = 0; i < N; i++)
		{
			for(int j = i + 1; j < N; j++)
			{
				double dist_ij = costMatrix[i][j];
				if (dist_ij < Double.MAX_VALUE)
				{
					sum += dist_ij;
					numPaths++;

					double tmpM = M;
					M += (dist_ij - tmpM) / numPaths;
					S += (dist_ij - tmpM) * (dist_ij - M);

					if (dist_ij > diameter) diameter = dist_ij;
				}
				
				double dist_ji = costMatrix[j][i];
				if (dist_ji < Double.MAX_VALUE)
				{
					sum += dist_ji;
					numPaths++;

					double tmpM = M;
					M += (dist_ji - tmpM) / numPaths;
					S += (dist_ji - tmpM) * (dist_ji - M);
					
					if (dist_ji > diameter) diameter = dist_ji;
				}
			}
		}
		
		if (numPaths == 0) return;
		
		averageSPLength = numPaths == 0 ? 0 : sum / numPaths;
		heterogeneity = averageSPLength == 0 ? 0 : Math.sqrt(S / numPaths) / averageSPLength;
	}

	/**
	 * Re-configures link cost setting. Related information, such as shortest paths, is cleared.
	 * 
	 * @param linkCostMap Cost per link, where the key is the link identifier and the value is the cost of traversing the link. No special iteration-order (i.e. ascending) is required
	 */
	public void configureLinkCostMap(Map<Link, Double> linkCostMap)
	{
		if (linkCostMap == null) this.costMap = null;
		else this.costMap = new LinkedHashMap<Link, Double>(linkCostMap);

		averageSPLength = -1;
		diameter = -1;
		heterogeneity = -1;
		nev = null;
	}

	/**
	 * Returns the adjacency matrix of the network. The adjacency matrix is a
	 * <i>NxN</i> matrix (where <i>N</i> is the number of nodes in the network),
	 * where each position (<i>i</i>,<i>j</i>) represents the number of directed
	 * links from <i>i</i> to <i>j</i>.
	 *
	 * @return Adjacency matrix
	 */
	private DoubleMatrix2D getAdjacencyMatrix()
	{
		if (adjacencyMatrix == null)
		{
			adjacencyMatrix = DoubleFactory2D.sparse.make(N, N);

			for (Link link  : linkMap)
			{
				int a_e = link.getOriginNode().getIndex ();
				int b_e = link.getDestinationNode().getIndex();
				adjacencyMatrix.setQuick(a_e, b_e, adjacencyMatrix.get(a_e, b_e) + 1);
			}
		}

		return adjacencyMatrix;
	}

	/**
	 * Returns the eigenvalues of the adjacency matrix.
	 *
	 * @return Eigenvalues of the adjacency matrix
	 */
	private double[] getAdjacencyMatrixEigenvalues()
	{
		if (adjacencyMatrixEigenvalues == null)
		{
			DoubleMatrix2D A_nn = getAdjacencyMatrix().copy();
			A_nn.assign(A_nn.viewDice(), DoubleFunctions.max);

			DenseDoubleAlgebra alg = new DenseDoubleAlgebra();
			DenseDoubleEigenvalueDecomposition eig = alg.eig(A_nn);

			adjacencyMatrixEigenvalues = eig.getRealEigenvalues().toArray();
			DoubleUtils.sort(adjacencyMatrixEigenvalues, Constants.OrderingType.ASCENDING);
		}

		return adjacencyMatrixEigenvalues;
	}

	/**
	 * <p>Returns the algebraic connectivity of the network. The algebraic connectivity
	 * is equal to the second smallest eigenvalue of the laplacian matrix.</p>
	 *
	 * <p>For symmetric (or undirected) networks, if the algebraic connectivity
	 * is different from zero, it is ensured that the network is connected, that is,
	 * it is possible to find a path between each node pair.
	 *
	 * @return Algebraic connectivity
	 */
	public double getAlgebraicConnectivity()
	{
		double[] eig = getLaplacianMatrixEigenvalues();
		return eig[1];
	}

	/**
	 * Returns the assortativity of the network.
	 *
	 * @return Assortativity
	 */
	public double getAssortativity()
	{
		if (E == 0) return 0;

		DoubleMatrix1D aux_outNodeDegree = getOutNodeDegree();

		double a = 0;
		double b = 0;
		double y = 0;

		for (Link link : linkMap)
		{
			Node originNode = link.getOriginNode();
			Node destinationNode = link.getDestinationNode();

			int j_e = (int) aux_outNodeDegree.get(originNode.getIndex ());
			int k_e = (int) aux_outNodeDegree.get(destinationNode.getIndex ());

			y += j_e + k_e;
			a += j_e * k_e;
			b += j_e * j_e + k_e * k_e;
		}

		y /= 2.0D * E;
		y *= y;
		a /= E;
		b /= 2.0D * E;

		return (a - y) / (b - y);
	}

	/**
	 * Returns the average neighbor connectivity.
	 *
	 * @return Average neighbor connectivity
	 */
	public double getAverageNeighborConnectivity()
	{
		if (E == 0) return 0;

		DoubleMatrix1D aux_outNodeDegree = getOutNodeDegree();

		int maxNodeDegree = aux_outNodeDegree.size () == 0? 0 : (int) aux_outNodeDegree.getMaxLocation() [0];

		double[] knn = new double[maxNodeDegree + 1];
		DoubleMatrix2D m = DoubleFactory2D.sparse.make(maxNodeDegree + 1, maxNodeDegree + 1);

		for (Link link : linkMap)
		{
			Node originNode = link.getOriginNode();
			Node destinationNode = link.getDestinationNode();

			int degree_k1 = (int) aux_outNodeDegree.get(originNode.getIndex ());
			int degree_k2 = (int) aux_outNodeDegree.get(destinationNode.getIndex ());

			m.set(degree_k1, degree_k2, m.get(degree_k1, degree_k2) + 1);
		}

		for (int k_1 = 1; k_1 <= maxNodeDegree; k_1++)
		{
			knn[k_1] = k_1 * m.viewRow(k_1).zSum();
		}

		return DoubleUtils.averageNonZeros(knn) / (E - 1);
	}

	/**
	 * Returns the average number of outgoing links per node.
	 *
	 * @return Average number of outgoing links per node
	 */
	public double getAverageOutNodeDegree()
	{
		return getOutNodeDegree().size() == 0? 0 : getOutNodeDegree().zSum() / getOutNodeDegree().size();
	}

	/**
	 * Returns the average shortest path distance among all node-pair shortest paths.
	 *
	 * @return Average shortest path distance
	 */
	public double getAverageShortestPathDistance()
	{
		if (averageSPLength == -1) computeSPDistanceMetrics();
		return averageSPLength;
	}

	/**
	 * Returns the average two-term reliability (A2TR) of the network. A2TR is computed
	 * as the ratio between the number of node-pair for which a path can be found
	 * and the same number when the network is connected (<i>Nx(N-1)</i>, where
	 * <i>N</i> is the number of nodes in the network). The value is in range [0, 1].
	 *
	 * @return Average two-term reliability
	 */
	public double getAverageTwoTermReliability()
	{
		if (E == 0) return 0;

		DirectedGraph<Node, Link> graph = getGraph_JGraphT();
		StrongConnectivityInspector<Node, Link> ci = new StrongConnectivityInspector<Node, Link>(graph);
		List<Set<Node>> connectedComponents = ci.stronglyConnectedSets();

		double sum = 0;
		Iterator<Set<Node>> it = connectedComponents.iterator();
		while (it.hasNext())
		{
			int componentSize = it.next().size();
			sum += componentSize * (componentSize - 1);
		}

		return sum / (N * (N - 1));
	}
	
	/**
	 * Returns the clustering coefficient of the network.
	 *
	 * @return Clustering coefficient
	 */
	public double getClusteringCoefficient()
	{
		if (E == 0) return 0;

		DoubleMatrix1D aux_outNodeDegree = getOutNodeDegree();

		Map<Node, Double> clusteringCoefficient = new LinkedHashMap<Node, Double>();
		for (Node node : nodes)
		{
			switch ((int) aux_outNodeDegree.get(node.getIndex ()))
			{
				case 0:
					break;

				case 1:
					clusteringCoefficient.put(node, 1.0);
					break;

				default:
					Collection<Node> neighbors = getNeighbors(node);
					int aux = 0;
					for (Node i : neighbors)
					{
						Collection<Node> aux_neighbors = getNeighbors(i);
						for (Node j : neighbors)
						{
							if (i.equals(j)) continue;

							if (CollectionUtils.contains(aux_neighbors, j)) aux++;
						}
					}

					clusteringCoefficient.put(node, (double) aux / neighbors.size());
					break;
			}
		}

		return DoubleUtils.average(clusteringCoefficient);
	}

	private Transformer<Link, Double> getCostTransformer()
	{
		if (nev == null) nev = JUNGUtils.getEdgeWeightTransformer(costMap);

		return nev;
	}

	/**
	 * Returns the density of the network. The density represents the ratio
	 * between the number of links in the network and the number of links needed
	 * to build a full-mesh network (<i>Nx(N-1)</i>, where <i>N</i> is the number of
	 * nodes in the network).
	 *
	 * @return Density
	 */
	public double getDensity()
	{
		if (N == 0) return 0;

		return (double) E / (N * (N - 1));
	}

	/**
	 * Returns the diameter of the network. The diameter is the longest path distance
	 * among all node-pair shortest paths.
	 *
	 * @return Network diameter
	 */
	public double getDiameter()
	{
		if (diameter == -1) computeSPDistanceMetrics();
		return diameter;
	}

	private DirectedGraph<Node, Link> getGraph_JGraphT()
	{
		if (graph_jgrapht == null) graph_jgrapht = (DirectedGraph<Node, Link>) JGraphTUtils.getGraphFromLinkMap(nodes , linkMap);
		return graph_jgrapht;
	}

	private Graph<Node, Link> getGraph_JUNG()
	{
		if (graph_jung == null) graph_jung = JUNGUtils.getGraphFromLinkMap(nodes , linkMap);

		return graph_jung;
	}

	/**
	 * Returns the heterogeneity of the network. The heterogeneity is equal to the
	 * standard deviation of all node-pair shortest paths divided by the average
	 * shortest path distance.
	 *
	 * @return Heterogeneity
	 */
	public double getHeterogeneity()
	{
		if (heterogeneity == -1) computeSPDistanceMetrics();
		return heterogeneity;
	}

	/**
	 * Returns the incidence matrix of the network. The incidence matrix is a
	 * <i>NxE</i> matrix (where <i>N</i> and <i>E</i> are the number of nodes
	 * and links in the network, respectively), where each position (<i>i</i>,
	 * <i>j</i>) is equal to: '1', if node <i>i</i> is the origin node of the link
	 * <i>j</i>; '-1', if node <i>i</i> is the destination node of the link
	 * <i>j</i>; and '0', otherwise.
	 *
	 * @return Incidence matrix
	 */
	private DoubleMatrix2D getIncidenceMatrix()
	{
		if (incidenceMatrix == null)
		{
			incidenceMatrix = DoubleFactory2D.sparse.make(N, E);
			for (Link link : linkMap)
			{
				int e = link.getIndex ();

				int a_e = link.getOriginNode().getIndex ();
				int b_e = link.getDestinationNode().getIndex ();
				incidenceMatrix.setQuick(a_e, e, 1);
				incidenceMatrix.setQuick(b_e, e, -1);
			}
		}

		return incidenceMatrix;
	}

	/**
	 * Returns the laplacian matrix of the network. The laplacian matrix is equal
	 * to the product of the incidence matrix by its transpose matrix.
	 *
	 * @return Laplacian matrix
	 */
	private DoubleMatrix2D getLaplacianMatrix()
	{
		if (laplacianMatrix == null)
		{
			DoubleMatrix2D A_ne = getIncidenceMatrix().copy();
			laplacianMatrix = A_ne.zMult(A_ne.viewDice(), null);
		}

		return laplacianMatrix;
	}

	/**
	 * Returns the eigenvalues of the laplacian matrix of the network.
	 *
	 * @return Eigenvalues of the laplacian matrix
	 */
	private double[] getLaplacianMatrixEigenvalues()
	{
		if (laplacianMatrixEigenvalues == null)
		{
			DenseDoubleAlgebra alg = new DenseDoubleAlgebra();
			DenseDoubleEigenvalueDecomposition eig = alg.eig(getLaplacianMatrix());

			laplacianMatrixEigenvalues = eig.getRealEigenvalues().toArray();
			DoubleUtils.sort(laplacianMatrixEigenvalues, Constants.OrderingType.ASCENDING);
		}

		return laplacianMatrixEigenvalues;
	}

	/**
	 * <p>Returns the betweeness centrality of each link. The betweeness
	 * centrality of a link is equal to the number of node-pair shortest paths which traverses
	 * the link.</p>
	 *
	 * <p>Internally it makes use of the Brandes' algorithm.</p>
	 *
	 * @return Betweeness centrality of each link
	 */
	public DoubleMatrix1D getLinkBetweenessCentrality()
	{
		if (linkBetweenessCentrality == null) computeBetweenessCentrality();

		return linkBetweenessCentrality;
	}

	/**
	 * <p>Returns the link connectivity. The link connectivity is equal to the smallest
	 * number of link-disjoint paths between each node pair.</p>
	 *
	 * <p>Internally it makes use of the Edmonds-Karp algorithm to compute the maximum
	 * flow between each node pair, assuming a link capacity equal to one for every link.</p>
	 *
	 * @return Link connectivity
	 */
	public int getLinkConnectivity()
	{
		if (E == 0) return 0;

		DirectedGraph<Node, Link> graph = getGraph_JGraphT();
		EdmondsKarpMaximumFlow<Node, Node> ek = new EdmondsKarpMaximumFlow(graph);
		int k = Integer.MAX_VALUE;

		for (Node originNode : nodes)
		{
			for (Node destinationNode : nodes)
			{
				if (originNode.equals(destinationNode)) continue;

				ek.calculateMaximumFlow(originNode, destinationNode);
				k = Math.min(k, ek.getMaximumFlowValue().intValue());

				if (k == 0) break;
			}
		}

		return k == Integer.MAX_VALUE ? 0 : k;
	}
	
	/**
	 * Returns the set of nodes reachable from a given node.
	 *
	 * @param node Node
	 * @return Collection of reachable nodes
	 */
	public Collection<Node> getNeighbors(Node node)
	{
		return Collections.unmodifiableCollection(getGraph_JUNG().getSuccessors(node));
	}

	/**
	 * <p>Returns the betweeness centrality of each node. The betweeness
	 * centrality of a node is equal to the number of node-pair shortest paths which traverses
	 * the node.</p>
	 *
	 * <p>Internally it makes use of the Brandes' algorithm.</p>
	 *
	 * @return Betweeness centrality of each node
	 */
	public DoubleMatrix1D getNodeBetweenessCentrality()
	{
		if (nodeBetweenessCentrality == null) computeBetweenessCentrality();

		return nodeBetweenessCentrality;
	}
	
	/**
	 * Returns the node connectivity. The node connectivity is equal to the smallest
	 * number of node-disjoint paths between each node pair.
	 *
	 * <p>Internally it makes use of the (modified) Edmonds-Karp algorithm to compute the maximum
	 * flow between each node pair, assuming a link capacity equal to one for every link.</p>
	 *
	 * @return Node connectivity
	 */
	public int getNodeConnectivity()
	{
		if (E == 0) return 0;
		
		DirectedGraph<Node, Link> graph = getGraph_JGraphT();
		int k = Integer.MAX_VALUE;

		for (Node originNode : nodes)
		{
			for (Node destinationNode : nodes)
			{
				if (originNode.equals(destinationNode)) continue;
				
				DirectedGraph<Node, Link> auxGraph = (DirectedGraph<Node, Link>) JGraphTUtils.buildAuxiliaryNodeDisjointGraph(graph, originNode, destinationNode);
				EdmondsKarpMaximumFlow<Node, Node> ek = new EdmondsKarpMaximumFlow(auxGraph);
				ek.calculateMaximumFlow(originNode, destinationNode);
				k = Math.min(k, ek.getMaximumFlowValue().intValue());

				if (k == 0) break;
			}
		}

		return k == Integer.MAX_VALUE ? 0 : k;
	}

	/**
	 * Returns the number of outgoing links for each node.
	 *
	 * @return Number of outgoing links per node
	 */
	public DoubleMatrix1D getOutNodeDegree()
	{
		if (outNodeDegree == null)
		{
			Graph<Node, Link> aux_graph = getGraph_JUNG();
			
			outNodeDegree = DoubleFactory1D.dense.make (N);
			for (Node node : nodes) 
			{ 
				outNodeDegree.set(node.getIndex (), aux_graph.outDegree(node));
			}
		}

		return outNodeDegree;
	}

	/**
	 * Returns the spectral radius of the network. The spectral radius is equal
	 * to the largest eigenvalue of the adjacency matrix.
	 *
	 * @return Spectral radius
	 */
	public double getSpectralRadius()
	{
		if (E == 0) return 0;

		double[] eig = getAdjacencyMatrixEigenvalues();
		return eig[eig.length - 1];
	}

	/**
	 * Returns the symmetry ratio. The symmetry ratio is equal to the number
	 * of distinct eigenvalues of the adjacency matrix divided by the network
	 * density plus one.
	 *
	 * @return Symmetry ratio
	 */
	public double getSymmetryRatio()
	{
		if (E == 0) return 0;

		double[] eig = getAdjacencyMatrixEigenvalues();
		return (double) DoubleUtils.unique(eig).length / (getDensity() + 1);
	}
}
