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
package com.net2plan.examples.ocnbook.onlineSim;


import cern.colt.matrix.tdouble.DoubleFactory1D;
import cern.colt.matrix.tdouble.DoubleFactory2D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import cern.jet.math.tdouble.DoublePlusMultFirst;
import cern.jet.math.tdouble.DoublePlusMultSecond;
import com.jom.DoubleMatrixND;
import com.jom.OptimizationProblem;
import com.net2plan.examples.ocnbook.offline.Offline_fa_xteFormulations;
import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.interfaces.simulation.IEventProcessor;
import com.net2plan.interfaces.simulation.SimEvent;
import com.net2plan.libraries.GraphUtils.ClosedCycleRoutingException;
import com.net2plan.utils.Constants.RoutingType;
import com.net2plan.utils.Constants.SearchType;
import com.net2plan.utils.*;

import java.io.File;
import java.util.*;

/** 
 * This module implements a distributed primal-decomposition-based gradient algorithm, for a coordinated adjustment of the routing in multiple domains (or cluster, or autonomous systems) in a network, so that domains do not need to exchange sensitive internal information, and minimize the average number of hops in the network.
 *
 * Ths event processor is adapted to permit observing the algorithm performances under user-defined conditions, 
 * including asynchronous distributed executions, where signaling can be affected by losses and/or delays, and/or measurement errors. 
 * The time evolution of different metrics can be stored in output files, for later processing. 
 * As an example, see the <a href="../../../../../../graphGeneratorFiles/fig_sec11_6_multidomainRouting_primalDecomp.m">{@code fig_sec11_6_multidomainRouting_primalDecomp.m}</a> MATLAB file used for generating the graph/s of the case study in the 
 * <a href="http://eu.wiley.com/WileyCDA/WileyTitle/productCd-1119013356.html">book</a> using this algorithm.
 * 
 * To simulate a network with this module, use the {@code Online_evGen_doNothing} generator.
 * 
 * @net2plan.keywords Multidomain network, Primal decomposition, Distributed algorithm, Flow assignment (FA), Destination-based routing, Destination-link formulation
 * @net2plan.ocnbooksections Section 11.6
 * @net2plan.inputParameters 
 * @author Pablo Pavon-Marino
 */
public class Online_evProc_multidomainRoutingPrimalDecomp extends IEventProcessor
{
	private InputParameter routing_isDomainRoutingRecomputationSynchronous = new InputParameter("routing_isDomainRoutingRecomputationSynchronous", false, "true if all domains wake up synchronously to recompute its routing");
	private InputParameter routing_averageDomainRoutingRecomputationInterval = new InputParameter("routing_averageDomainRoutingRecomputationInterval", 1.0, "Average time between two routing recomputation of a domain", 0, false, Double.MAX_VALUE, true);
	private InputParameter routing_maxFluctuationDomainRoutingRecomputationInterval = new InputParameter("routing_maxFluctuationDomainRoutingRecomputationInterval", 0.5, "Max fluctuation in time between two routing recomputation of a domain", 0, true, Double.MAX_VALUE, true);
	private InputParameter routing_averageMasterUpdateInterval = new InputParameter("routing_averageMasterUpdateInterval", 1.0, "Average time between two master recomputation", 0, false, Double.MAX_VALUE, true);
	private InputParameter routing_maxFluctuationMasterUpdateInterval = new InputParameter("routing_maxFluctuationMasterUpdateInterval", 0.5, "Max fluctuation in time between two master updates", 0, true, Double.MAX_VALUE, true);
	private InputParameter gradient_gammaStep = new InputParameter("gradient_gammaStep", 0.1, "Gamma step in the gradient algorithm", 0, false, Double.MAX_VALUE, true);
	private InputParameter simulation_maxNumberOfMasterUpdateIntervals = new InputParameter("simulation_maxNumberOfMasterUpdateIntervals", 100.0, "Maximum number of update intervals in average per agent", 0, false, Double.MAX_VALUE, true);
	private InputParameter simulation_randomSeed = new InputParameter("simulation_randomSeed", (long) 1, "Seed of the random number generator");
	private InputParameter simulation_outFileNameRoot = new InputParameter("simulation_outFileNameRoot", "multidomainRoutingPrimalDecompostion", "Root of the file name to be used in the output files. If blank, no output");

	private int N, E, C, Ef, D; // total number of nodes and links in the network. Ef is the number of frontier links
	private int[] allT; // array of constants 0...N-1
	private int[] allE; // array of constants 0...E-1
	private double PENALIZATIONOVERSUBSCRIBED = 1E1;
	private double PRECISIONFACTOR;

	Random rng;

	private Map<String, int[]> nodeGlobalIndexes_c;
	private Map<String, int[]> frontierGlobalLinkIndexes_c;
	private Map<String, int[]> internalAndFrontierGlobalLinkIndexes_c;

	private Set<String> clusterIds;
	private Map<Node, String> clusterIdOfNode;
	//	private Map<Long,Integer> globalLinkIndex;
	//	private long [] globalLinkId;
	//	private Map<Long,Integer> globalNodeIndex;
	//	private long [] globalNodeId;
	private Map<String, Integer> globalClusterIndex;
	private String[] globalClusterId;

	private int[] frontierLinkGlobalIndex_ef; // one element per frontier link (in m_te and A_ce), returns its global index 

	private DoubleMatrix2D global_x_te; // indexes: global node, global link 
	private DoubleMatrix1D global_u_c; // indexes: global cluster
	private DoubleMatrix2D global_m_te;// indexes: global node, global link (although only frontier link columns are used)

	private Map<String, DoubleMatrix2D> mostUpdated_vte_fromC;

	private DoubleMatrix2D global_h_nt;
	private DoubleMatrix2D global_A_ne;
	private DoubleMatrix1D global_u_e;
	private DoubleMatrix2D global_h_ct;
	private DoubleMatrix2D global_A_cef;

	private Map<String, String> net2planParameters;

	static final int ROUTING_MASTERUPDATE = 300;
	static final int ROUTING_DOMAINRECOMPUTE = 301;

	private TimeTrace stat_traceOf_objFun;
	private TimeTrace stat_traceOf_x_te;
	private TimeTrace stat_traceOf_y_e;

	private NetPlan currentNetPlan;

	@Override
	public String getDescription()
	{
		return "This module implements a distributed primal-decomposition-based gradient algorithm, for a coordinated adjustment of the routing in multiple domains (or cluster, or autonomous systems) in a network, so that domains do not need to exchange sensitive internal information, and minimize the average number of hops in the network.";
	}

	@Override
	public List<Triple<String, String, String>> getParameters()
	{
		/* Returns the parameter information for all the InputParameter objects defined in this object (uses Java reflection) */
		return InputParameter.getInformationAllInputParameterFieldsOfObject(this);
	}

	@Override
	public void initialize(NetPlan currentNetPlan, Map<String, String> algorithmParameters, Map<String, String> simulationParameters, Map<String, String> net2planParameters)
	{
		try
		{
			/* Initialize all InputParameter objects defined in this object (this uses Java reflection) */
			InputParameter.initializeAllInputParameterFieldsOfObject(this, algorithmParameters);

			if (currentNetPlan.getNumberOfLayers() != 1) throw new Net2PlanException("This algorithm works in single layer networks");

			this.rng = new Random(simulation_randomSeed.getLong());
			this.currentNetPlan = currentNetPlan;
			this.net2planParameters = net2planParameters;

			this.currentNetPlan.removeAllUnicastRoutingInformation();
			this.currentNetPlan.setRoutingType(RoutingType.HOP_BY_HOP_ROUTING);

			this.N = this.currentNetPlan.getNumberOfNodes(); // total number of nodes in the network
			this.E = this.currentNetPlan.getNumberOfLinks(); // total number of nodes in the network
			this.D = this.currentNetPlan.getNumberOfDemands(); // total number of nodes in the network
			if ((E == 0) || (D == 0)) throw new Net2PlanException ("The input design should have links and demands");
			this.allT = new int[N];
			for (int n = 0; n < N; n++)
				allT[n] = n;
			this.allE = new int[E];
			for (int e = 0; e < E; e++)
				allE[e] = e;
			this.PRECISIONFACTOR = 1e-2;//Double.parseDouble (net2planParameters.get("precisionFactor"));

			this.clusterIds = new HashSet<String>();
			this.clusterIdOfNode = new HashMap<Node, String>();
			this.nodeGlobalIndexes_c = new HashMap<String, int[]>();
			this.frontierGlobalLinkIndexes_c = new HashMap<String, int[]>();
			this.internalAndFrontierGlobalLinkIndexes_c = new HashMap<String, int[]>();
			for (Node n : this.currentNetPlan.getNodes())
			{
				final int indexN = n.getIndex();
				final String stClusterId = n.getAttribute("clusterId");
				if (stClusterId == null) throw new Net2PlanException("Node " + n + " does not belong to any cluster");
				clusterIds.add(stClusterId);
				clusterIdOfNode.put(n, stClusterId);
				if (!nodeGlobalIndexes_c.containsKey(stClusterId))
				{
					nodeGlobalIndexes_c.put(stClusterId, new int[] {});
					frontierGlobalLinkIndexes_c.put(stClusterId, new int[] {});
					internalAndFrontierGlobalLinkIndexes_c.put(stClusterId, new int[] {});
				}
				nodeGlobalIndexes_c.put(stClusterId, IntUtils.concatenate(nodeGlobalIndexes_c.get(stClusterId), new int[] { indexN }));
			}

			this.C = nodeGlobalIndexes_c.size();
			this.globalClusterIndex = new HashMap<String, Integer>();
			this.globalClusterId = new String[C];
			int counter = 0;
			for (String cId : clusterIds)
			{
				globalClusterId[counter] = cId;
				globalClusterIndex.put(cId, counter);
				counter++;
			}

			/* For each cluster, the first links in xte are the frontier links, in the same order as they appear in cluster mte */
			this.frontierLinkGlobalIndex_ef = new int[0];
			for (Link e : this.currentNetPlan.getLinks())
			{
				final int indexE = e.getIndex();
				final Node a_e = e.getOriginNode();
				final Node b_e = e.getDestinationNode();
				final String clusterId_ae = clusterIdOfNode.get(a_e);
				final String clusterId_be = clusterIdOfNode.get(b_e);
				if (!clusterId_ae.equals(clusterId_be))
				{
					frontierGlobalLinkIndexes_c.put(clusterId_ae, IntUtils.concatenate(frontierGlobalLinkIndexes_c.get(clusterId_ae), new int[] { indexE }));
					frontierGlobalLinkIndexes_c.put(clusterId_be, IntUtils.concatenate(frontierGlobalLinkIndexes_c.get(clusterId_be), new int[] { indexE }));
					internalAndFrontierGlobalLinkIndexes_c.put(clusterId_ae, IntUtils.concatenate(internalAndFrontierGlobalLinkIndexes_c.get(clusterId_ae), new int[] { indexE }));
					internalAndFrontierGlobalLinkIndexes_c.put(clusterId_be, IntUtils.concatenate(internalAndFrontierGlobalLinkIndexes_c.get(clusterId_be), new int[] { indexE }));
					frontierLinkGlobalIndex_ef = IntUtils.concatenate(frontierLinkGlobalIndex_ef, new int[] { indexE });
				}
			}
			for (Link e : this.currentNetPlan.getLinks())
			{
				final int indexE = e.getIndex();
				final Node a_e = e.getOriginNode();
				final Node b_e = e.getDestinationNode();
				final String clusterId_ae = clusterIdOfNode.get(a_e);
				final String clusterId_be = clusterIdOfNode.get(b_e);
				if (clusterId_ae.equals(clusterId_be)) internalAndFrontierGlobalLinkIndexes_c.put(clusterId_ae, IntUtils.concatenate(internalAndFrontierGlobalLinkIndexes_c.get(clusterId_ae), new int[] { indexE }));
			}

			this.Ef = frontierLinkGlobalIndex_ef.length;

			this.global_x_te = DoubleFactory2D.sparse.make(N, E);
			this.global_u_c = DoubleFactory1D.dense.make(C);
			this.global_m_te = DoubleFactory2D.dense.make(N, E);
			this.mostUpdated_vte_fromC = new HashMap<String, DoubleMatrix2D>();
			for (String cId : clusterIds)
				mostUpdated_vte_fromC.put(cId, null);
			this.global_A_ne = this.currentNetPlan.getMatrixNodeLinkIncidence();
			this.global_u_e = this.currentNetPlan.getVectorLinkCapacity();
			this.global_h_nt = this.currentNetPlan.getMatrixNode2NodeOfferedTraffic();
			for (int t = 0; t < N; t++)
			{
				double h_t = 0;
				for (int n = 0; n < N; n++)
					h_t += global_h_nt.get(n, t);
				global_h_nt.set(t, t, -h_t);
			}

			this.global_h_ct = DoubleFactory2D.dense.make(C, N);
			for (String clusterId : clusterIds)
			{
				final int index_c = globalClusterIndex.get(clusterId);
				for (Node t : this.currentNetPlan.getNodes())
				{
					final int index_t = t.getIndex();
					final int index_cluster_t = globalClusterIndex.get(clusterIdOfNode.get(t));
					final boolean targetInsideTheCluster = (index_c == index_cluster_t);
					if (targetInsideTheCluster)
					{
						/* h_ct is what other nodes outside cluster generate towards t (in negative) */
						for (Node n : this.currentNetPlan.getNodes())
						{
							final int index_n = n.getIndex();
							final int index_cluster_n = globalClusterIndex.get(clusterIdOfNode.get(n));
							if (index_cluster_n == index_c) continue; // only outside cluster nodes matter
							global_h_ct.set(index_c, index_t, global_h_ct.get(index_c, index_t) - global_h_nt.get(index_n, index_t));
						}
					} else
					{
						/* h_ct is what this cluster generate towards t (in negative) */
						for (int index_n : nodeGlobalIndexes_c.get(clusterId))
						{
							final int index_cluster_n = globalClusterIndex.get(clusterIdOfNode.get(currentNetPlan.getNode(index_n)));
							if (index_cluster_n != index_c) throw new RuntimeException("Bad");
							global_h_ct.set(index_c, index_t, global_h_ct.get(index_c, index_t) + global_h_nt.get(index_n, index_t));
						}
					}
				}
			}

			this.global_A_cef = DoubleFactory2D.dense.make(C, Ef);
			for (int index_ef = 0; index_ef < frontierLinkGlobalIndex_ef.length; index_ef++)
			{
				final int index_e = frontierLinkGlobalIndex_ef[index_ef];
				final Link e = currentNetPlan.getLink(index_e);
				final Node a_e = e.getOriginNode();
				final Node b_e = e.getDestinationNode();
				final int indexC_ae = this.globalClusterIndex.get(clusterIdOfNode.get(a_e));
				final int indexC_be = this.globalClusterIndex.get(clusterIdOfNode.get(b_e));
				if (indexC_ae != indexC_be)
				{
					global_A_cef.set(indexC_ae, index_ef, 1.0);
					global_A_cef.set(indexC_be, index_ef, -1.0);
				}
			}

			stat_traceOf_objFun = new TimeTrace();
			stat_traceOf_x_te = new TimeTrace();
			stat_traceOf_y_e = new TimeTrace();

			/* Initialize the master variables. The feasible, closest to zero */
			this.global_m_te = projectMaster(global_m_te, null);

			/* Create the cluster topologies */
			/* Initially all nodes receive a "wake up to transmit" event, aligned at time zero or y asynchr => randomly chosen */
			for (String cId : clusterIds)
				scheduleEvent(new SimEvent(0, SimEvent.DestinationModule.EVENT_PROCESSOR, ROUTING_DOMAINRECOMPUTE, cId));
			scheduleEvent(new SimEvent(0.0001, SimEvent.DestinationModule.EVENT_PROCESSOR, ROUTING_MASTERUPDATE, -1));

			checksInitialize();
		} catch (Exception e)
		{
			e.printStackTrace();
			throw e;
		}
	}

	@Override
	public void processEvent(NetPlan currentNetPlan, SimEvent event)
	{
			final double time = event.getEventTime();
			switch (event.getEventType())
			{
			case ROUTING_DOMAINRECOMPUTE:
			{
				final String clusterId = (String) event.getEventObject();
				final int index_c = globalClusterIndex.get(clusterId);

				/* Takes the most updated master information it has */
				Triple<DoubleMatrix2D, DoubleMatrix2D, Double> p = computeSubproblem(clusterId, this.currentNetPlan);
				DoubleMatrix2D cluster_xte = p.getFirst();
				DoubleMatrix2D cluster_vte = p.getSecond();
				double u_c = p.getThird();

				final int[] cluster_Eif = internalAndFrontierGlobalLinkIndexes_c.get(clusterId);

				/* global x_te can be not feasible, if different domains operate asynchornously */
				global_x_te.viewSelection(allT, cluster_Eif).assign(cluster_xte.copy().viewSelection(allT, cluster_Eif));
				global_u_c.set(index_c, u_c);
				mostUpdated_vte_fromC.put(clusterId, cluster_vte.copy());

				/* They will be the new forwarding rules */
				DoubleMatrix2D new_fde = this.currentNetPlan.getMatrixDemandBasedForwardingRules();

				/* Remove all the forwarding rules of the cluster nodes */
				for (int index_n : nodeGlobalIndexes_c.get(clusterId))
					for (Link e : currentNetPlan.getNode(index_n).getOutgoingLinks())
						new_fde.viewColumn(e.getIndex()).assign(0);

				/* Create new forwarding rules according to the routing computed */
				for (int index_n : nodeGlobalIndexes_c.get(clusterId))
					for (Node t : this.currentNetPlan.getNodes())
					{
						final Node n = this.currentNetPlan.getNode(index_n);
						if (n == t) continue;
						final int index_t = t.getIndex();
						final Set<Demand> demandIdsEndingIn_t = t.getIncomingDemands();
						double outTraffic_nt = 0;
						for (Link e : n.getOutgoingLinks())
							outTraffic_nt += global_x_te.get(index_t, e.getIndex());

						Link outLinkMaxFte = null;
						double maxFte = -1;
						double accumFteToForward = 0;
						for (Link e : n.getOutgoingLinks())
						{
							final double f_te = (outTraffic_nt == 0) ? 0 : global_x_te.get(index_t, e.getIndex()) / outTraffic_nt;
							if (f_te > maxFte)
							{
								maxFte = f_te;
								outLinkMaxFte = e;
							}
							if (f_te > PRECISIONFACTOR)
								for (Demand d : demandIdsEndingIn_t)
									new_fde.set(d.getIndex(), e.getIndex(), f_te); //changingFR.put (Pair.of (d,e),f_te);
							else
								accumFteToForward += f_te;
						}
						for (Demand d : demandIdsEndingIn_t)
							new_fde.set(d.getIndex(), outLinkMaxFte.getIndex(), new_fde.get(d.getIndex(), outLinkMaxFte.getIndex()) + accumFteToForward);
					}

				DoubleMatrix2D outFR_dn = new_fde.zMult(currentNetPlan.getMatrixNodeLinkOutgoingIncidence().viewDice(), null);
				for (int n = 0; n < N; n++)
					for (int d = 0; d < D; d++)
						if ((Math.abs(outFR_dn.get(d, n) - 1) > 1e-3) && (Math.abs(outFR_dn.get(d, n)) > 1e-3)) throw new RuntimeException("Bad. outFR_dn:  " + outFR_dn);

				/* In some rare occasions, the routing may create loops. These routings are not applied to the network */
				try { this.currentNetPlan.setForwardingRules(new_fde); } catch (ClosedCycleRoutingException e) { System.out.println ("The routing after this domain update would produce a closed loop. It is not applied.");	}

				if (time > 0)
				{
					DoubleMatrix1D blockedTraffic = currentNetPlan.getVectorDemandBlockedTraffic();
					DoubleMatrix1D linkOversubscription = currentNetPlan.getVectorLinkOversubscribedTraffic();
					if (blockedTraffic.getMaxLocation()[0] > 1e-3) System.out.println ("Some traffic is lost. Sum blocked traffic of the demands: " + blockedTraffic.zSum());
					if (linkOversubscription.getMaxLocation()[0] > 1e-3) System.out.println ("Some links are ovsersubscribed. Sum oversubscription traffic in the links: " + linkOversubscription.zSum());
					stat_traceOf_objFun.add(time, computeObjectiveFunctionFromNetPlan(this.currentNetPlan));
					stat_traceOf_x_te.add(time, this.currentNetPlan.getMatrixDestination2LinkTrafficCarried());
					stat_traceOf_y_e.add(time, this.currentNetPlan.getVectorLinkCarriedTraffic());
				}

				if (time > this.simulation_maxNumberOfMasterUpdateIntervals.getDouble() * routing_averageMasterUpdateInterval.getDouble())
				{
					this.endSimulation();
				}

				final double nextWakeUpTime = (routing_isDomainRoutingRecomputationSynchronous.getBoolean()) ? time + routing_averageDomainRoutingRecomputationInterval.getDouble() : Math.max(time, time + routing_averageDomainRoutingRecomputationInterval.getDouble() + routing_maxFluctuationDomainRoutingRecomputationInterval.getDouble() * (rng.nextDouble() - 0.5));
				this.scheduleEvent(new SimEvent(nextWakeUpTime, SimEvent.DestinationModule.EVENT_PROCESSOR, ROUTING_DOMAINRECOMPUTE, clusterId));

				break;
			}
			case ROUTING_MASTERUPDATE: // A node receives from an out neighbor the q_nt for any destination
			{
				DoubleMatrix2D gradient_te = DoubleFactory2D.dense.make(N, Ef, 1.0); // initially all ones

				for (String clusterId : clusterIds)
				{
					DoubleMatrix2D v_te_fromC = mostUpdated_vte_fromC.get(clusterId);
					gradient_te.assign(v_te_fromC.viewSelection(allT, frontierLinkGlobalIndex_ef), DoublePlusMultFirst.plusMult(1)); // -1 no
				}

				//final double gamma = gradient_gammaStep.getDouble() / (1+time*time);
				final double gamma = gradient_gammaStep.getDouble();
				//	    final double gamma = gradient_gammaStep.getDouble() / (1+time);
				//	    final double gamma = time < 20? gradient_gammaStep.getDouble() : gradient_gammaStep.getDouble() / (Math.sqrt(1+time));
				DoubleMatrix2D candidate_mte = global_m_te.copy();
				candidate_mte.viewSelection(allT, frontierLinkGlobalIndex_ef).assign(gradient_te, DoublePlusMultSecond.plusMult(-gamma));

				/* Now we have to project the candidate master variables into the set of feasible master variables */
				this.global_m_te = projectMaster(candidate_mte, global_m_te);

				if (time > this.simulation_maxNumberOfMasterUpdateIntervals.getDouble() * routing_averageDomainRoutingRecomputationInterval.getDouble())
				{
					endSimulation();
				}

				final double nextMasterTime = Math.max(time, time + routing_averageMasterUpdateInterval.getDouble() + routing_maxFluctuationMasterUpdateInterval.getDouble() * (rng.nextDouble() - 0.5));
				scheduleEvent(new SimEvent(nextMasterTime, SimEvent.DestinationModule.EVENT_PROCESSOR, ROUTING_MASTERUPDATE, -1));
				break;
			}

			default:
				throw new RuntimeException("Unexpected received event");
			}
	}

	public String finish(StringBuilder st, double simTime)
	{
		if (simulation_outFileNameRoot.getString().equals("")) return null;
		stat_traceOf_objFun.printToFile(new File(simulation_outFileNameRoot.getString() + "_objFunc.txt"));
		stat_traceOf_x_te.printToFile(new File(simulation_outFileNameRoot.getString() + "_xte.txt"));
		stat_traceOf_y_e.printToFile(new File(simulation_outFileNameRoot.getString() + "_ye.txt"));
		final NetPlan optNetPlan = computeOptimumSolution();
		TimeTrace.printToFile(new File(simulation_outFileNameRoot.getString() + "_jom_objFunc.txt"), new double[] { computeObjectiveFunctionFromNetPlan(optNetPlan) });
		TimeTrace.printToFile(new File(simulation_outFileNameRoot.getString() + "_jom_xte.txt"), optNetPlan.getMatrixDestination2LinkTrafficCarried());
		TimeTrace.printToFile(new File(simulation_outFileNameRoot.getString() + "_jom_ye.txt"), optNetPlan.getVectorLinkCarriedTraffic());
		return null;
	}

	/* They use the most updated m_te information */
	private Triple<DoubleMatrix2D, DoubleMatrix2D, Double> computeSubproblem(String clusterId, NetPlan np)
	{
		final int[] cluster_Ef = frontierGlobalLinkIndexes_c.get(clusterId);
		final int[] cluster_N = nodeGlobalIndexes_c.get(clusterId);

		DoubleMatrix2D h_nt = global_h_nt.copy().viewSelection(cluster_N, allT);
		DoubleMatrix2D A_ne = global_A_ne.copy().viewSelection(cluster_N, allE);
		DoubleMatrix1D u_e = global_u_e.copy();

		OptimizationProblem op = new OptimizationProblem();

		op.setInputParameter("h_nt", h_nt.toArray());
		op.setInputParameter("A_ne", A_ne.toArray());
		op.setInputParameter("u_e", u_e.toArray(), "row");
		op.setInputParameter("m_te", global_m_te.toArray());
		op.setInputParameter("allT", allT, "row");
		op.setInputParameter("frontC", cluster_Ef, "row");
		op.setInputParameter("M", PENALIZATIONOVERSUBSCRIBED);

		op.addDecisionVariable("x_te", false, new int[] { N, E }, 0, Double.MAX_VALUE);
		op.addDecisionVariable("u", false, new int[] { 1, 1 }, 0, Double.MAX_VALUE);

		/* Set some input parameters */
		op.setObjectiveFunction("minimize", "sum(x_te) + M*u");

		op.addConstraint("A_ne * (x_te') == h_nt"); // the flow-conservation constraints (NxN constraints)
		op.addConstraint("sum(x_te,1) <= u_e + u"); // the capacity constraints (E constraints)
		op.addConstraint("x_te(allT,frontC) == m_te(allT,frontC)", "v_te"); // the capacity constraints (E constraints)

		/* Call the solver to solve the problem */
		op.solve("cplex");

		/* If an optimal solution was not found, quit */
		if (!op.solutionIsOptimal()) throw new RuntimeException("An optimal solution was not found");

		/* Retrieve the optimum solutions. Convert the bps into Erlangs */
		DoubleMatrix2D x_te = op.getPrimalSolution("x_te").view2D();
		double u = op.getPrimalSolution("u").toValue();

		/* v_te: zeros in no frontier links, the appropriate values in this cluster frontier links */
		DoubleMatrix2D v_te = DoubleFactory2D.dense.make(N, E);
		v_te.viewSelection(allT, cluster_Ef).assign(op.getMultipliersOfConstraint("v_te").view2D());

		return Triple.of(x_te, v_te, u);
	}

	private DoubleMatrix2D projectMaster(DoubleMatrix2D can_mte, DoubleMatrix2D old_mte)
	{
		OptimizationProblem op = new OptimizationProblem();

		op.addDecisionVariable("m_te", false, new int[] { N, Ef }, 0, Double.MAX_VALUE);
		op.setInputParameter("h_ct", global_h_ct.toArray());
		op.setInputParameter("A_ce", global_A_cef.toArray());
		op.setInputParameter("can_mte", can_mte.viewSelection(allT, frontierLinkGlobalIndex_ef).toArray());
		op.setInputParameter("u_e", global_u_e.viewSelection(frontierLinkGlobalIndex_ef).toArray(), "row");

		/* Set some input parameters */
		op.setObjectiveFunction("minimize", "sum((m_te-can_mte)^2)");
		op.addConstraint("A_ce * (m_te') == h_ct"); // the flow-conservation constraints (NxN constraints)
		op.addConstraint("sum(m_te,1) <= u_e"); // the capacity constraints (E constraints)

		for (String cId : clusterIds)
		{
			final int indexC = this.globalClusterIndex.get(cId);
			final int[] nIdsCluster = nodeGlobalIndexes_c.get(cId);
			final int[] frontierOutLinkIndexesInMte = DoubleUtils.find(global_A_cef.viewRow(indexC).toArray(), 1.0, SearchType.ALL);
			op.setInputParameter("destCluster", nIdsCluster, "row");
			op.setInputParameter("clusterOutLinks", frontierOutLinkIndexesInMte, "row");
			op.addConstraint("m_te(destCluster,clusterOutLinks) == 0"); // traffic to a cluster does not leave the cluster
		}
		if (old_mte != null) op.setInitialSolution("m_te", new DoubleMatrixND(old_mte.viewSelection(allT, frontierLinkGlobalIndex_ef).copy()));

		/* Call the solver to solve the problem */
		try
		{
			op.solve("ipopt");
		} catch (Exception e)
		{
			e.printStackTrace();
		} // sometimes IPOPT is unstable. do not stop the algorithm then

		/* If an optimal solution was not found, quit */
		if (!op.solutionIsOptimal()) return old_mte.copy(); // sometimes IPOPT is unstable. do not stop the algorithm then

		/* Retrieve the optimum solutions. Convert the bps into Erlangs */
		double[][] m_te_array = (double[][]) op.getPrimalSolution("m_te").toArray();

		for (String cId : clusterIds)
		{
			final int indexC = this.globalClusterIndex.get(cId);
			final int[] nIdsCluster = nodeGlobalIndexes_c.get(cId);
			final int[] frontierOutLinkIndexesInMte = DoubleUtils.find(global_A_cef.viewRow(indexC).toArray(), 1.0, SearchType.ALL);
			for (int t : nIdsCluster)
				for (int e : frontierOutLinkIndexesInMte)
					if (m_te_array[t][e] > PRECISIONFACTOR)
						throw new RuntimeException("Bad: m_te_array[t][e]: " + m_te_array[t][e]);
					else
						m_te_array[t][e] = 0;
		}

		DoubleMatrix2D m_te = DoubleFactory2D.dense.make(N, E);
		m_te.viewSelection(allT, frontierLinkGlobalIndex_ef).assign(m_te_array);
		return m_te;
	}

	private double computeObjectiveFunctionFromNetPlan(NetPlan np)
	{
		double optCost = 0;
		double maxOverssubs = 0;
		for (Link e : np.getLinks())
		{
			final double y_e = e.getCarriedTraffic();
			final double u_e = e.getCapacity();
			maxOverssubs = Math.max(maxOverssubs, y_e - u_e);
			optCost += Math.min(y_e, u_e);
		}
		optCost += PENALIZATIONOVERSUBSCRIBED * maxOverssubs;
		return optCost;
	}

	private NetPlan computeOptimumSolution()
	{
		/* Compute the optimum solution */
		Map<String, String> m = new HashMap<String, String>();
		m.put("optimizationTarget", "min-av-num-hops");
		m.put("solverName", "cplex"); // use any linear solver reachable from JOM
		m.put("solverLibraryName", "");
		m.put("maxSolverTimeInSeconds", "-1");
		m.put("binaryRatePerTrafficUnit_bps", "1");
		m.put("averagePacketLengthInBytes", "1");
		m.put("nonBifurcatedRouting", "false");
		NetPlan npCopy = this.currentNetPlan.copy();
		new Offline_fa_xteFormulations().executeAlgorithm(npCopy, m, this.net2planParameters);
		return npCopy;
	}

	private void checksInitialize()
	{
		/* Check in h_ct, the columns sum zero */
		for (int t = 0; t < this.currentNetPlan.getNumberOfNodes(); t++)
			if (Math.abs(global_h_ct.viewColumn(t).zSum()) > PRECISIONFACTOR) throw new RuntimeException("Bad");
		/* Check in h_nt, the columns sum zero */
		for (int t = 0; t < this.currentNetPlan.getNumberOfNodes(); t++)
			if (Math.abs(global_h_nt.viewColumn(t).zSum()) > PRECISIONFACTOR) throw new RuntimeException("Bad");
	}

}
