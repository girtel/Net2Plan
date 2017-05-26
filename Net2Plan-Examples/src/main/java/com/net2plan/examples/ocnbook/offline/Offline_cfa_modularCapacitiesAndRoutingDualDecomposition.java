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



package com.net2plan.examples.ocnbook.offline;

import cern.colt.matrix.tdouble.DoubleFactory1D;
import cern.colt.matrix.tdouble.DoubleFactory2D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import cern.jet.math.tdouble.DoubleFunctions;
import cern.jet.math.tdouble.DoublePlusMultSecond;
import com.jom.OptimizationProblem;
import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.libraries.GraphUtils;
import com.net2plan.utils.Constants.RoutingType;
import com.net2plan.utils.InputParameter;
import com.net2plan.utils.Quintuple;
import com.net2plan.utils.TimeTrace;
import com.net2plan.utils.Triple;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Finds the routing and mocular capacities for a network that minimize the cost, using a dual decomposition approach
 * 
 * The time evolution of different metrics can be stored in output files, for later processing. 
 * As an example, see the <a href="../../../../../../graphGeneratorFiles/fig_sec11_7_modularCapacitiesAndRouting_dualDecomp.m">{@code fig_sec11_7_modularCapacitiesAndRouting_dualDecomp.m}</a> MATLAB file used for generating the graph/s of the case study in the 
 * <a href="http://eu.wiley.com/WileyCDA/WileyTitle/productCd-1119013356.html">book</a> using this algorithm.
 * @net2plan.description
 * @net2plan.keywords Capacity assignment (CA), Flow assignment (FA), Modular capacities , Dual decomposition
 * @net2plan.ocnbooksections Section 11.7
 * @net2plan.inputParameters 
 * @author Pablo Pavon-Marino
 */
public class Offline_cfa_modularCapacitiesAndRoutingDualDecomposition implements IAlgorithm
{
	private double PRECISIONFACTOR;
	private double numModulesUpperBound;
	
	private int N , E;
	
	private InputParameter moduleCapacity = new InputParameter ("moduleCapacity", 1.0 , "Capacity of one module" , 0 , false , Double.MAX_VALUE , true);
	private InputParameter solverName = new InputParameter ("solverName", "#select# glpk ipopt xpress cplex", "The solver name to be used by JOM. GLPK and IPOPT are free, XPRESS and CPLEX commercial. GLPK, XPRESS and CPLEX solve linear problems w/w.o integer contraints. IPOPT is can solve nonlinear problems (if convex, returns global optimum), but cannot handle integer constraints");
	private InputParameter solverLibraryName = new InputParameter ("solverLibraryName", "" , "The solver library full or relative path, to be used by JOM. Leave blank to use JOM default.");
	private InputParameter maxSolverTimeInSeconds = new InputParameter ("maxSolverTimeInSeconds", (double) -1 , "Maximum time granted to the solver to solve the problem. If this time expires, the solver returns the best solution found so far (if a feasible solution is found)");
	private InputParameter subgrad_gammaStep = new InputParameter ("subgrad_gammaStep", (double) 0.05 , "Gamma step in the algorithm iteration" , 0 , false , Double.MAX_VALUE , true);
	private InputParameter subgrad_type = new InputParameter ("subgrad_type", "#select# 1-over-t constant 1-over-square-root-t", "Type of gradient algorithm.  constant, 1-over-t, decreasing-square-t");	
	private InputParameter simulation_outFileNameRoot = new InputParameter ("simulation_outFileNameRoot", "modularCapacitiesAndRoutingDualDecomp" , "Root of the file name to be used in the output files. If blank, no output");
	private InputParameter simulation_numIterations = new InputParameter ("simulation_numIterations", (int) 500 , "Number o iterations of the algorithm" , 1 , Integer.MAX_VALUE);


	private DoubleMatrix2D tmColumnSumZero;
	private DoubleMatrix2D A_ne;

	private TimeTrace stat_bestObjFuncFeasible;
	private TimeTrace stat_objFuncFeasible;
	private TimeTrace stat_lowerBound;
	private TimeTrace stat_bestLowerBound;
	private TimeTrace stat_pie;

	@Override
	public String executeAlgorithm(NetPlan netPlan, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
	{
		/* Initialize all InputParameter objects defined in this object (this uses Java reflection) */
		InputParameter.initializeAllInputParameterFieldsOfObject(this, algorithmParameters);

		if (netPlan.getNumberOfLayers() != 1) throw new Net2PlanException ("This algorithm works in single layer networks");

		/* Initialize some variables */
		this.N = netPlan.getNumberOfNodes();
		this.E = netPlan.getNumberOfLinks();
		this.PRECISIONFACTOR = Double.parseDouble(net2planParameters.get("precisionFactor"));
		
		this.stat_objFuncFeasible = new TimeTrace ();
		this.stat_bestObjFuncFeasible = new TimeTrace ();
		this.stat_lowerBound = new TimeTrace ();
		this.stat_bestLowerBound = new TimeTrace ();
		this.stat_pie = new TimeTrace ();

		netPlan.removeAllUnicastRoutingInformation();
		netPlan.setRoutingType (RoutingType.HOP_BY_HOP_ROUTING);
		
	    /* The diagonal in the traffic matrix contains minus the amount of traffic destined to that node */
		double [][] trafficMatrix = netPlan.getMatrixNode2NodeOfferedTraffic().toArray();
		for (int n1 = 0 ; n1 < N ; n1 ++) for (int n2 = 0 ; n2 < N ; n2 ++) if (n1 == n2) continue; else trafficMatrix [n2][n2] -= trafficMatrix [n1][n2];
		this.tmColumnSumZero= DoubleFactory2D.dense.make(trafficMatrix);

		System.out.println(netPlan.getMatrixNode2NodeOfferedTraffic());
		
		this.numModulesUpperBound = Math.ceil(netPlan.getVectorDemandOfferedTraffic().zSum() / moduleCapacity.getDouble());
		this.A_ne = netPlan.getMatrixNodeLinkIncidence();
		
		DoubleMatrix1D multipliers_eIP = DoubleFactory1D.dense.make(E, 1.0);
    double highestDualCost = -Double.MAX_VALUE;
    double lowestPrimalCost = Double.MAX_VALUE;
    DoubleMatrix1D bestFeasible_ne = null;
    DoubleMatrix2D bestRelaxedAndFeasible_xte = null;
		for (int it = 1 ; it <= simulation_numIterations.getInt() ; it ++)
		{
			System.out.println("**** Iteration : " + it + ", mult: " + multipliers_eIP);

			Quintuple<DoubleMatrix2D,DoubleMatrix1D,DoubleMatrix1D,Double,DoubleMatrix1D> q = solveSubproblems (multipliers_eIP);
			final DoubleMatrix2D relaxedAndFeasible_xte = q.getFirst();
			final DoubleMatrix1D relaxed_ne = q.getSecond();
			final DoubleMatrix1D feasible_n_e = q.getThird();
			final double dualCost = q.getFourth();
			final DoubleMatrix1D gradient_e = q.getFifth ();
			
			final double objFunc = feasible_n_e.zSum();

			highestDualCost = Math.max(highestDualCost , dualCost);
			if (objFunc < lowestPrimalCost)
			{
				lowestPrimalCost = objFunc;
				bestFeasible_ne = feasible_n_e.copy();
				bestRelaxedAndFeasible_xte = relaxedAndFeasible_xte.copy();
			}

			if (highestDualCost > lowestPrimalCost + PRECISIONFACTOR) throw new RuntimeException ("Bad: highestDualCost: "+  highestDualCost + ", lowestPrimalCost: " + lowestPrimalCost); 
			
			System.out.println("* Feasible: Compute cost: " + objFunc);

			this.stat_objFuncFeasible.add (it , new double [] { objFunc } );
			this.stat_bestObjFuncFeasible.add(it ,  new double [] { lowestPrimalCost } );
			this.stat_lowerBound.add(it , new double [] { dualCost } );
			this.stat_bestLowerBound.add(it , new double [] { highestDualCost } );
			this.stat_pie.add(it, multipliers_eIP.toArray() );

			double gamma = 0; 
			if (this.subgrad_type.getString ().equalsIgnoreCase("constant"))
				gamma = this.subgrad_gammaStep.getDouble();
			else if (this.subgrad_type.getString ().equalsIgnoreCase("1-over-t"))
				gamma = this.subgrad_gammaStep.getDouble() / it;
			else if (this.subgrad_type.getString ().equalsIgnoreCase("1-over-square-root-t"))
				gamma = this.subgrad_gammaStep.getDouble() / Math.sqrt(it);
			else throw new Net2PlanException ("Unknown subgradient algorithm type");
			for (int e = 0 ; e < E ; e++)
				multipliers_eIP.set(e , Math.max(0 ,  multipliers_eIP.get(e) + gamma * gradient_e.get(e) ));
		}
		
		saveNetPlan (netPlan , bestRelaxedAndFeasible_xte , bestFeasible_ne);		
		
		finish (netPlan);

		return "Ok! Best solution. Cost: " + bestFeasible_ne.zSum();
	}

	@Override
	public String getDescription()
	{
		return "Given a network with a set of given nodes, and links, and a given known unicast offered traffic. This algorithm jointly computes (i) the routing of the traffic, and the capacity to assign to the links. The link capacities are constrained to be modular: an integer multiple of a link capacity quantum. Optimization target is minimizing the network cost, given by the number of capacity modules to install in the network. The problem in NP-hard. The algorithm implements a dual decomposition iterative, using a subgradient algorithm.";
	}

	@Override
	public List<Triple<String, String, String>> getParameters()
	{
		/* Returns the parameter information for all the InputParameter objects defined in this object (uses Java reflection) */
		return InputParameter.getInformationAllInputParameterFieldsOfObject(this);
	}
	
	private Quintuple<DoubleMatrix2D,DoubleMatrix1D,DoubleMatrix1D,Double,DoubleMatrix1D> solveSubproblems (DoubleMatrix1D pi_e)
	{
		/* Upper layer */
		DoubleMatrix2D x_te = DoubleFactory2D.dense.make(N,E);
		{
			OptimizationProblem op = new OptimizationProblem();
			op.setInputParameter("pi_e", pi_e.toArray() , "column");
	    op.setInputParameter("A_ne", A_ne.toArray());
			op.setInputParameter("TM", this.tmColumnSumZero.toArray());
      op.addDecisionVariable("x_te", false , new int[] { N , E }, 0, Double.MAX_VALUE); // dest-link formulation at IP layer
			op.setObjectiveFunction("minimize", "sum (x_te * pi_e)");
			op.addConstraint("A_ne * (x_te') == TM"); // the flow-conservation constraints (NxN constraints)
			/* Call the solver to solve the problem */
			op.solve(solverName.getString (), "solverLibraryName", solverLibraryName.getString () ,  "maxSolverTimeInSeconds" , maxSolverTimeInSeconds.getDouble ());

			/* If no solution is found, quit */
			if (op.feasibleSolutionDoesNotExist()) throw new Net2PlanException("The problem has no feasible solution");
			if (op.foundUnboundedSolution()) throw new Net2PlanException ("Found an unbounded solution");
			if (!op.solutionIsFeasible()) throw new Net2PlanException("A feasible solution was not found");
			x_te = op.getPrimalSolution("x_te").view2D();
		}
		
		DoubleMatrix1D n_e = DoubleFactory1D.dense.make(E);
		for (int e = 0 ; e < E ; e ++)
			n_e.set(e , (1 - (moduleCapacity.getDouble() * pi_e.get(e)) >= 0)? 0 : numModulesUpperBound);
		
		DoubleMatrix1D y_e = x_te.copy().zMult(DoubleFactory1D.dense.make(N, 1.0) , null , 1.0 , 0.0 , true);
		DoubleMatrix1D gradient_e = y_e.copy().assign(n_e , DoublePlusMultSecond.plusDiv(-moduleCapacity.getDouble()));

		/* Compute the dual cost */
		final double dualCost = x_te.copy().zMult(pi_e, null).zSum() - n_e.zDotProduct(pi_e) * moduleCapacity.getDouble();
		
		/* Compute a feasible solution from the relaxed x_te */
		DoubleMatrix1D feasible_n_e = y_e.copy().assign(DoubleFunctions.div(moduleCapacity.getDouble())).assign(DoubleFunctions.ceil);

//		System.out.println("carriedTraffic_e: " + carriedTraffic_e);
//		System.out.println("capacityLink_e: " + capacityLink_e);
//		System.out.println("barU_e: " + barU_e);
		
		return Quintuple.of(x_te.copy () ,  n_e.copy() ,  feasible_n_e.copy() ,  dualCost ,  gradient_e);
	}
	
	private void finish (NetPlan np)
	{
		/* If no output file, return */
		if (simulation_outFileNameRoot.getString ().equals("")) return;
		stat_objFuncFeasible.printToFile(new File (simulation_outFileNameRoot.getString () + "_objFunc.txt"));
		stat_bestObjFuncFeasible.printToFile(new File (simulation_outFileNameRoot.getString () + "_bestObjFunc.txt"));
		stat_lowerBound.printToFile(new File (simulation_outFileNameRoot.getString () + "_lowerBound.txt"));
		stat_bestLowerBound.printToFile(new File (simulation_outFileNameRoot.getString () + "_bestLowerBound.txt"));
		stat_pie.printToFile(new File (simulation_outFileNameRoot.getString () + "_pie.txt"));
		
		
		double lowerBoundNumLps_1 = 0;
		for (Node s : np.getNodes())
		{
			double outTraffic = 0; for (Demand d : s.getOutgoingDemands()) outTraffic += d.getOfferedTraffic();
			lowerBoundNumLps_1 += Math.ceil(outTraffic / moduleCapacity.getDouble());
		}
		double lowerBoundNumLps_2 = 0;
		for (Node t : np.getNodes())
		{
			double inTraffic = 0; for (Demand d : t.getIncomingDemands()) inTraffic += d.getOfferedTraffic();
			lowerBoundNumLps_2 += Math.ceil(inTraffic / moduleCapacity.getDouble());
		}
		double lowerBoundNumLps_3 = 0;
		for (Node t : np.getNodes())
			for (Node s : np.getNodes())
			{
				if (s == t) continue;
				final int numHopsSP = GraphUtils.getShortestPath(np.getNodes () , np.getLinks(), s, t, null).size();
				for (Demand d : np.getNodePairDemands(s,t,false)) lowerBoundNumLps_3 += numHopsSP * d.getOfferedTraffic();
			}
		lowerBoundNumLps_3 = Math.ceil(lowerBoundNumLps_3/moduleCapacity.getDouble());

		TimeTrace.printToFile(new File (simulation_outFileNameRoot.getString () + "_heuristicLBCost.txt") , new double [] { lowerBoundNumLps_1 , lowerBoundNumLps_2 , lowerBoundNumLps_3  }   );
		
	}
	
	void saveNetPlan (NetPlan netPlan , DoubleMatrix2D x_te , DoubleMatrix1D n_e)
	{
		/* Set the routing at the IP layer */
		netPlan.setRoutingType(RoutingType.HOP_BY_HOP_ROUTING);
		netPlan.removeAllForwardingRules();
		netPlan.setRoutingFromDestinationLinkCarriedTraffic(x_te , true);
		for (Link e : netPlan.getLinks())
			e.setCapacity(moduleCapacity.getDouble() * n_e.get(e.getIndex ()));
		for (Demand d : netPlan.getDemandsBlocked())
			if (d.getBlockedTraffic() > PRECISIONFACTOR) throw new RuntimeException ("Bad");
		for (Link e : netPlan.getLinksOversubscribed())
			if (e.getOccupiedCapacity() - e.getCapacity() > PRECISIONFACTOR) throw new RuntimeException ("Bad");
	}
	
}
