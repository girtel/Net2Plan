package com.net2plan.examples.smartCity.logistics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.collections15.BidiMap;
import org.apache.commons.collections15.bidimap.DualHashBidiMap;

import com.jom.OptimizationProblem;
import com.net2plan.interfaces.networkDesign.Demand;
import com.net2plan.interfaces.networkDesign.IAlgorithm;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.interfaces.networkDesign.Route;
import com.net2plan.utils.Constants.RoutingType;
import com.net2plan.utils.InputParameter;
import com.net2plan.utils.Triple;

import cern.colt.matrix.tdouble.DoubleFactory2D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;

// PABLO: If there is no penalization for using direct o->d links, or fixed cost for using link with > 0 traffic, 
// then there will be no gain in using distribution centers!!!! 

/** This algorithm implements a waste collection scheme. More details in future versions
 *
 */
public class LogisticVarious implements IAlgorithm 
{
	public enum NODETYPE { PLANT , DISTRIBUTIONCENTER , DESTINATION  };
	
	private final static String ATTNAME_NODE_INPUTNODETYPE = "input_nodeType";
	private static NODETYPE getNodeType (Node n , NODETYPE defalutValue) { try { return NODETYPE.valueOf(n.getAttribute(ATTNAME_NODE_INPUTNODETYPE)); } catch (Exception e) {return defalutValue;  } }
	private static void setNodeType (Node n , NODETYPE type) { n.setAttribute(ATTNAME_NODE_INPUTNODETYPE , type.name()); }

	private final static String ATTNAME_NODE_FIXEDSETTINGCOST = "input_fixedSettingCost";
	private static Double getFixedSettingCost (Node n , Double defaultValue) { checkIsPlantOrStockCandidateLocation(n); return n.getAttributeAsDouble (ATTNAME_NODE_FIXEDSETTINGCOST , defaultValue); }
	private static void setFixedSettingCost (Node n , Double value) { checkIsPlantOrStockCandidateLocation(n); n.setAttribute (ATTNAME_NODE_FIXEDSETTINGCOST , value); }
	
	private final static String ATTNAME_NODE_MINPRODUCTIONIFPLANTEXISTS = "input_minProductionPerPlantIfPlantExists";
	private static Double getMinProductionPerPlantIfPlantExists (Node n , Double defaultValue) { checkIsPlantCandidateLocation(n);return n.getAttributeAsDouble (ATTNAME_NODE_MINPRODUCTIONIFPLANTEXISTS , defaultValue); }
	private static void setMinProductionPerPlantIfPlantExists (Node n , Double value) { checkIsPlantCandidateLocation(n); n.setAttribute (ATTNAME_NODE_MINPRODUCTIONIFPLANTEXISTS , value); }

	private final static String ATTNAME_NODE_MAXPRODUCTIONIFPLANTEXISTS = "input_maxProductionPerPlantIfPlantExists";
	private static Double getMaxProductionPerPlantIfPlantExists (Node n , Double defaultValue) { checkIsPlantCandidateLocation(n);return n.getAttributeAsDouble (ATTNAME_NODE_MAXPRODUCTIONIFPLANTEXISTS , defaultValue); }
	private static void setMaxProductionPerPlantIfPlantExists (Node n , Double value) { checkIsPlantCandidateLocation(n); n.setAttribute (ATTNAME_NODE_MAXPRODUCTIONIFPLANTEXISTS , value); }
	
	private final static String ATTNAME_NODE_MINNUMPLANINLOCATION  = "input_minNumPlantsInLocation";
	private static int getMinNumPlantsInLocation (Node n , Integer defaultValue) { checkIsPlantCandidateLocation(n); final Double res = n.getAttributeAsDouble (ATTNAME_NODE_MINNUMPLANINLOCATION , defaultValue == null? (Double) null : defaultValue.doubleValue()); return res == null? null : res.intValue(); }
	private static void setMinNumPlantsInLocation (Node n , int value) { checkIsPlantCandidateLocation(n); n.setAttribute (ATTNAME_NODE_MINNUMPLANINLOCATION , value); }
	
	private final static String ATTNAME_NODE_MAXNUMPLANINLOCATION  = "input_maxNumPlantsInLocation";
	private static int getMaxNumPlantsInLocation (Node n , Integer defaultValue) { checkIsPlantCandidateLocation(n); final Double res = n.getAttributeAsDouble (ATTNAME_NODE_MAXNUMPLANINLOCATION , defaultValue == null? (Double) null : defaultValue.doubleValue()); return res == null? null : res.intValue(); }
	private static void setMaxNumPlantsInLocation (Node n , int value) { checkIsPlantCandidateLocation(n); n.setAttribute (ATTNAME_NODE_MAXNUMPLANINLOCATION , value); }

	private final static String ATTNAME_NODE_MINNUMDISTRIBUTIONCENTERINLOCATION = "input_minNumDistributionCentersInLocation";
	private static int getMinNumDistributionCentersInLocation (Node n , Integer defaultValue) { checkIsStockCandidateLocation(n); final Double res = n.getAttributeAsDouble (ATTNAME_NODE_MINNUMDISTRIBUTIONCENTERINLOCATION , defaultValue == null? (Double) null : defaultValue.doubleValue()); return res == null? null : res.intValue(); }
	private static void setMinNumDistributionCentersInLocation (Node n , int value) { checkIsStockCandidateLocation(n); n.setAttribute (ATTNAME_NODE_MINNUMDISTRIBUTIONCENTERINLOCATION , value); }
	
	private final static String ATTNAME_NODE_MAXNUMDISTRIBUTIONCENTERINLOCATION = "input_maxNumDistributionCentersInLocation";
	private static int getMaxNumDistributionCentersInLocation (Node n , Integer defaultValue) { checkIsStockCandidateLocation(n); final Double res = n.getAttributeAsDouble (ATTNAME_NODE_MAXNUMDISTRIBUTIONCENTERINLOCATION , defaultValue == null? (Double) null : defaultValue.doubleValue()); return res == null? null : res.intValue(); }
	private static void setMaxNumDistributionCentersInLocation (Node n , int value) { checkIsStockCandidateLocation(n); n.setAttribute (ATTNAME_NODE_MAXNUMDISTRIBUTIONCENTERINLOCATION , value); }

	private final static String ATTNAME_NODE_PENALIZATIONPERUNDELIVEREDUNIT  = "input_penalizationPerUndeliveredUnit";
	private static Double getPenalizationPerUndeliveredUnit (Node n , Double defaultValue) { checkIsDestinationCandidateLocation(n);return n.getAttributeAsDouble (ATTNAME_NODE_PENALIZATIONPERUNDELIVEREDUNIT , defaultValue); }
	private static void setPenalizationPerUndeliveredUnit (Node n , Double value) { checkIsDestinationCandidateLocation(n); n.setAttribute (ATTNAME_NODE_PENALIZATIONPERUNDELIVEREDUNIT , value); }
	
	private final static String ATTNAME_NODE_MAXBLOCKEDGOODS = "input_maxBlockedGoods";
	private static Double getMaxBlockedGoods (Node n , Double defaultValue) { checkIsDestinationCandidateLocation(n);return n.getAttributeAsDouble (ATTNAME_NODE_MAXBLOCKEDGOODS , defaultValue); }
	private static void setMaxBlockedGoods (Node n , Double value) { checkIsDestinationCandidateLocation(n); n.setAttribute (ATTNAME_NODE_MAXBLOCKEDGOODS , value); }

	private final static String ATTNAME_NODE_DEMANDSIZE = "input_demandSizeThisNode";
	private static Double getDemandSize (Node n , Double defaultValue) { checkIsDestinationCandidateLocation(n);return n.getAttributeAsDouble (ATTNAME_NODE_DEMANDSIZE , defaultValue); }
	private static void setDemandSize (Node n , Double value) { checkIsDestinationCandidateLocation(n); n.setAttribute (ATTNAME_NODE_DEMANDSIZE , value); }

	private final static String ATTNAME_NODE_MAXSTOCKCAPACITY = "input_maxStorageCapacity";
	private static Double getMaxStorageCapacity  (Node n , Double defaultValue) { checkIsStockCandidateLocation(n);return n.getAttributeAsDouble (ATTNAME_NODE_MAXSTOCKCAPACITY , defaultValue); }
	private static void setMaxStorageCapacity (Node n , Double value) { checkIsStockCandidateLocation(n); n.setAttribute (ATTNAME_NODE_MAXSTOCKCAPACITY , value); }

	private final static String ATTNAME_NODE_MINSTOCKCAPACITY = "input_minStorageCapacity";
	private static Double getMinStorageCapacity  (Node n , Double defaultValue) { checkIsStockCandidateLocation(n);return n.getAttributeAsDouble (ATTNAME_NODE_MINSTOCKCAPACITY , defaultValue); }
	private static void setMinStorageCapacity (Node n , Double value) { checkIsStockCandidateLocation(n); n.setAttribute (ATTNAME_NODE_MINSTOCKCAPACITY , value); }

	private final static String ATTNAME_LINK_COSTPERDEMANDUNIT = "input_costPerDemandUnit";
	private static Double getCostPerDemandUnit (Link n , Double defaultValue) { return n.getAttributeAsDouble (ATTNAME_LINK_COSTPERDEMANDUNIT , defaultValue); }
	private static void setCostPerDemandUnit (Link n , Double value) { n.setAttribute (ATTNAME_LINK_COSTPERDEMANDUNIT , value); }

	private final static String ATTNAME_NODE_NUMBEROFPLACEDPLANTS = "output_numberOfPlacedPlants";
	private static int getNumberOfPlacedPlants (Node n , Integer defaultValue) { checkIsPlantCandidateLocation(n); final Double res = n.getAttributeAsDouble (ATTNAME_NODE_NUMBEROFPLACEDPLANTS , defaultValue == null? (Double) null : defaultValue.doubleValue()); return res == null? null : res.intValue(); }
	private static void setNumberOfPlacedPlants (Node n , int value) { checkIsPlantCandidateLocation(n); n.setAttribute (ATTNAME_NODE_NUMBEROFPLACEDPLANTS , value); }
	
	private final static String ATTNAME_NODE_NUMBEROFDISTRIBUTIONCENTERS = "output_numberOfDistributionCenters";
	private static int getNumberOfPlacedDistributionCenters (Node n , Integer defaultValue) { checkIsStockCandidateLocation(n); final Double res = n.getAttributeAsDouble (ATTNAME_NODE_NUMBEROFDISTRIBUTIONCENTERS , defaultValue == null? (Double) null : defaultValue.doubleValue()); return res == null? null : res.intValue(); }
	private static void setNumberOfPlacedDistributionCenters (Node n , int value) { checkIsStockCandidateLocation(n); n.setAttribute (ATTNAME_NODE_NUMBEROFDISTRIBUTIONCENTERS , value); }
	
	private static SortedSet<Node> getCandidatePlants (NetPlan np) { return np.getNodes().stream().filter(n->getNodeType(n, null) == NODETYPE.PLANT).collect(Collectors.toCollection(TreeSet::new)); }  
	private static SortedSet<Node> getCandidateDistributionCenters (NetPlan np) { return np.getNodes().stream().filter(n->getNodeType(n, null) == NODETYPE.DISTRIBUTIONCENTER).collect(Collectors.toCollection(TreeSet::new)); }  
	private static SortedSet<Node> getCandidateDestinations (NetPlan np) { return np.getNodes().stream().filter(n->getNodeType(n, null) == NODETYPE.DESTINATION).collect(Collectors.toCollection(TreeSet::new)); }  
	
	private static double getCapacity (Link n) { return n.getCapacity();  }
	private static void setCapacity (Link n , double value) { n.setCapacity(value); }
	
	private InputParameter minNumberOfPlants = new InputParameter ("minNumberOfPlants", (int) 0 , "Minimum number of plants to be placed" , 0 , Integer.MAX_VALUE);
	private InputParameter maxNumberOfPlants = new InputParameter ("maxNumberOfPlants", (int) 100 , "Maximum number of plants to be placed" , 0 , Integer.MAX_VALUE);
	private InputParameter minNumberOfDistributionCenters = new InputParameter ("minNumberOfDistributionCenters", (int) 0 , "Minimum number of distribution centers to be placed" , 0 , Integer.MAX_VALUE);
	private InputParameter maxNumberOfDistributionCenters = new InputParameter ("maxNumberOfDistributionCenters", (int) 100 , "Maximum number of distribution centers to be placed" , 0 , Integer.MAX_VALUE);
	private InputParameter forbidRoutesNotTraversingADistributionCenter = new InputParameter ("forbidRoutesNotTraversingADistributionCenter", (boolean) true , "If true the direct connections between the origin and destinations, not traversing a distribution center, are not permited");
	private InputParameter useGeographicalDistanceAsLinkCosts = new InputParameter ("useGeographicalDistanceAsLinkCosts", (boolean) true , "If true the link cost is made equal to the link lengths, if not, the cost attribute in the links is used");
	private InputParameter maxDistanceOriginToDestinationInKm = new InputParameter ("maxDistanceOriginToDestinationInKm", (double) 1000.0 , "Maximum distance allowed in any route from the plant to the destination" , 0.0 , true , Double.MAX_VALUE , false);
	private InputParameter solverName = new InputParameter ("solverName", "#select# cplex mipcl glpk xpress", "The solver name to be used by JOM. GLPK and IPOPT are free, XPRESS and CPLEX commercial. GLPK, XPRESS and CPLEX solve linear problems w/w.o integer contraints. IPOPT is can solve nonlinear problems (if convex, returns global optimum), but cannot handle integer constraints");
	private InputParameter solverLibraryName = new InputParameter ("solverLibraryName", "" , "The solver library full or relative path, to be used by JOM. Leave blank to use JOM default.");
	private InputParameter maxSolverTimeInSeconds = new InputParameter ("maxSolverTimeInSeconds", (double) -1 , "Maximum time granted to the solver to solve the problem. If this time expires, the solver returns the best solution found so far (if a feasible solution is found)");
	private InputParameter defaultLinkCapacityInTraversableGoods = new InputParameter ("defaultLinkCapacityInTraversableGoods", (double) 1e12 , "Maximum amount if goods that can traverse a link" , 0.0 , true , Double.MAX_VALUE , false);
	private InputParameter aaa_createInitialTopologyNotApplyAlgorithm = new InputParameter ("aaa_createInitialTopologyNotApplyAlgorithm", true , "Creates an initial topology, and does not run the algorithm");
	private InputParameter aaa_numOrigins = new InputParameter ("aaa_numOrigins", 5 , "Number of origin nodes" , 1 , Integer.MAX_VALUE);
	private InputParameter aaa_numDcs = new InputParameter ("aaa_numDcs", 3 , "Number of origin nodes" , 0 , Integer.MAX_VALUE);
	private InputParameter aaa_numDestinations = new InputParameter ("aaa_numDestinations", 5 , "Number of origin nodes" , 1 , Integer.MAX_VALUE);

	
	
	@Override
	public String executeAlgorithm(NetPlan netPlan, Map<String, String> algorithmParameters, Map<String, String> net2planParameters) 
	{
		InputParameter.initializeAllInputParameterFieldsOfObject(this, algorithmParameters);

		if (aaa_createInitialTopologyNotApplyAlgorithm.getBoolean()) { createInitialTopology(netPlan); return "Topology created."; }
		
		if (netPlan.getNodes().stream().anyMatch(n->getNodeType(n, null) == null)) throw new Net2PlanException ("Unknown node type. Valid types are: " + Arrays.toString(NODETYPE.values()));
		
		/* Remove links that are between destinations, between origins, between stocks, or from destination to origin */
		for (Link e : new ArrayList<> (netPlan.getLinks()))
		{
			if (getNodeType(e.getOriginNode(), null) == NODETYPE.PLANT && getNodeType(e.getDestinationNode(), null) == NODETYPE.PLANT) e.remove();
			else if (getNodeType(e.getOriginNode(), null) == NODETYPE.DISTRIBUTIONCENTER && getNodeType(e.getDestinationNode(), null) == NODETYPE.DISTRIBUTIONCENTER) e.remove();
			else if (getNodeType(e.getOriginNode(), null) == NODETYPE.DESTINATION && getNodeType(e.getDestinationNode(), null) == NODETYPE.DESTINATION) e.remove();
			else if (getNodeType(e.getOriginNode(), null) == NODETYPE.DESTINATION && getNodeType(e.getDestinationNode(), null) == NODETYPE.PLANT) e.remove();
			else if (getNodeType(e.getOriginNode(), null) == NODETYPE.DESTINATION && getNodeType(e.getDestinationNode(), null) == NODETYPE.DISTRIBUTIONCENTER) e.remove();
			else if (getNodeType(e.getOriginNode(), null) == NODETYPE.DISTRIBUTIONCENTER && getNodeType(e.getDestinationNode(), null) == NODETYPE.PLANT) e.remove();
			if (forbidRoutesNotTraversingADistributionCenter.getBoolean())
				if (getNodeType(e.getOriginNode(), null) == NODETYPE.PLANT && getNodeType(e.getDestinationNode(), null) == NODETYPE.DESTINATION) e.remove();
		}
		/* Add links if they are not there. If added, put length as cost */
		for (Node origin : getCandidatePlants(netPlan))
			for (Node dc : getCandidateDistributionCenters(netPlan))
			{
				final SortedSet<Link> nodePairLinks = netPlan.getNodePairLinks(origin, dc, false);
				if (nodePairLinks.size() > 1) nodePairLinks.forEach(e->e.remove());
				if (nodePairLinks.isEmpty())
					netPlan.addLink(origin, dc, defaultLinkCapacityInTraversableGoods.getDouble(), netPlan.getNodePairEuclideanDistance(origin, dc), 90.0/3600, null);
			}
		for (Node origin : getCandidatePlants(netPlan))
			for (Node dest : getCandidateDestinations(netPlan))
			{
				final SortedSet<Link> nodePairLinks = netPlan.getNodePairLinks(origin, dest, false);
				if (nodePairLinks.size() > 1) nodePairLinks.forEach(e->e.remove());
				if (nodePairLinks.isEmpty() && !forbidRoutesNotTraversingADistributionCenter.getBoolean())
					netPlan.addLink(origin, dest, defaultLinkCapacityInTraversableGoods.getDouble(), netPlan.getNodePairEuclideanDistance(origin, dest), 90.0/3600, null);
			}
		for (Node dc : getCandidateDistributionCenters(netPlan))
			for (Node dest : getCandidateDestinations(netPlan))
			{
				final SortedSet<Link> nodePairLinks = netPlan.getNodePairLinks(dc, dest, false);
				if (nodePairLinks.size() > 1) nodePairLinks.forEach(e->e.remove());
				if (nodePairLinks.isEmpty())
					netPlan.addLink(dc, dest, defaultLinkCapacityInTraversableGoods.getDouble(), netPlan.getNodePairEuclideanDistance(dc, dest), 90.0/3600, null);
			}		
		
		/* */
		netPlan.removeAllDemands();
		
		final List<Node> list_cand_o = new ArrayList<> (getCandidatePlants(netPlan));
		final List<Node> list_cand_a = new ArrayList<> (getCandidateDistributionCenters(netPlan));
		final List<Node> list_cand_d = new ArrayList<> (getCandidateDestinations(netPlan));
		final BidiMap<Node,Integer> cand_o = createBidiMap(getCandidatePlants(netPlan));
		final BidiMap<Node,Integer> cand_a = createBidiMap(getCandidateDistributionCenters(netPlan));
		final BidiMap<Node,Integer> cand_d = createBidiMap(getCandidateDestinations(netPlan));
		final int NUM_O = cand_o.size();
		final int NUM_A = cand_a.size();
		final int NUM_D = cand_d.size();
		
		for (Node orig : cand_o.keySet())
			for (Node dest : cand_d.keySet())
			{
					
				final Demand d = netPlan.addDemand(orig, dest, 0.0, RoutingType.SOURCE_ROUTING, null);
				if (!forbidRoutesNotTraversingADistributionCenter.getBoolean())
					netPlan.addRoute(d, 0.0, 0.0, Arrays.asList (getLink(orig, dest)), null);
				for (Node dc : cand_a.keySet())
					netPlan.addRoute(d, 0.0, 0.0, Arrays.asList (getLink (orig , dc) , getLink(dc , dest)), null);
			}
		final int P = netPlan.getNumberOfRoutes();
		if (P == 0) throw new Net2PlanException ("The problem has no meaningful solution: no route from origin to destination exists");
		
		OptimizationProblem op = new OptimizationProblem();

		op.setInputParameter("c_e", netPlan.getLinks().stream().map(e->useGeographicalDistanceAsLinkCosts.getBoolean()? e.getLengthInKm () : getCostPerDemandUnit(e, e.getLengthInKm ())).collect (Collectors.toList()) , "row");
		op.setInputParameter("u_e", netPlan.getLinks().stream().map(e->e.getCapacity()).collect (Collectors.toList()) , "row");
		op.setInputParameter("A_ep", netPlan.getMatrixLink2RouteAssignment()); /* 1 in position (e,p) if link e is traversed by path p, 0 otherwise */
		final DoubleMatrix2D A_op = DoubleFactory2D.sparse.make(NUM_O , P);
		final DoubleMatrix2D A_ap = DoubleFactory2D.sparse.make(NUM_A , P);
		final DoubleMatrix2D A_destp = DoubleFactory2D.sparse.make(NUM_D , P);
		for (Route r : netPlan.getRoutes())
		{
			final Node o = r.getIngressNode();
			final Node d = r.getEgressNode();
			final int oIndex = cand_o.get(o);
			final int dIndex = cand_d.get(d);
			if (r.getSeqLinks().size() > 1) 
			{
				final Node a = r.getSeqLinks().get(0).getDestinationNode();
				final int aIndex = cand_a.get(a);
				A_ap.set(aIndex , r.getIndex() , 1.0);
			}
			A_op.set(oIndex , r.getIndex() , 1.0);
			A_destp.set(dIndex , r.getIndex() , 1.0);
		}
		
		final double [] y_a_min = new double [NUM_A]; for (int cont = 0 ; cont < NUM_A ; cont ++) y_a_min [cont] = Math.max(0 , (double) getMinNumDistributionCentersInLocation(cand_a.inverseBidiMap().get(cont) , 0));
		final double [] y_a_max = new double [NUM_A]; for (int cont = 0 ; cont < NUM_A ; cont ++) y_a_max [cont] = Math.max(0 , (double) getMaxNumDistributionCentersInLocation(cand_a.inverseBidiMap().get(cont) , 0));
		final double [] y_o_min = new double [NUM_O]; for (int cont = 0 ; cont < NUM_O ; cont ++) y_o_min [cont] = Math.max(0 , (double) getMinNumPlantsInLocation(cand_o.inverseBidiMap().get(cont) , 0));
		final double [] y_o_max = new double [NUM_O]; for (int cont = 0 ; cont < NUM_O ; cont ++) y_o_max [cont] = Math.max(0 , (double) getMaxNumPlantsInLocation(cand_o.inverseBidiMap().get(cont) , 0));
		final double [] h_dest = new double [NUM_D]; for (int cont = 0 ; cont < NUM_D ; cont ++) h_dest [cont] = Math.max(0, getDemandSize(cand_d.inverseBidiMap().get(cont) , 0.0));
		final double [] minHDestAndMaxBlockedGoods_dest = new double [NUM_D]; for (int cont = 0 ; cont < NUM_D ; cont ++) minHDestAndMaxBlockedGoods_dest [cont] = Math.max(0, Math.min(h_dest [cont] , getMaxBlockedGoods(cand_d.inverseBidiMap().get(cont) , 0.0)));
		final double [] maxTraffic_p = new double [P]; for (int cont = 0 ; cont < P ; cont ++) maxTraffic_p [cont] = maxDistanceOriginToDestinationInKm.getDouble() > 0? (netPlan.getRoute(cont).getLengthInKm() > maxDistanceOriginToDestinationInKm.getDouble()? 0.0 : Double.MAX_VALUE  ) : Double.MAX_VALUE;

		if (NUM_A > 0)
		{
			op.setInputParameter("A_ap", A_ap); /* 1 in position (e,p) if link e is traversed by path p, 0 otherwise */
			op.setInputParameter("cf_a", list_cand_a.stream().map(a->getFixedSettingCost(a, 0.0)).collect(Collectors.toList()) , "row"); /* fixed costs */
			op.setInputParameter("hMax_a", list_cand_a.stream().map(d->getMaxStorageCapacity(d, Double.MAX_VALUE)).collect(Collectors.toList()) , "row"); /* fixed costs */
			op.setInputParameter("hMin_a", list_cand_a.stream().map(d->getMinStorageCapacity(d, 0.0)).collect(Collectors.toList()) , "row"); /* fixed costs */
		}
		op.setInputParameter("A_op", A_op); /* 1 in position (e,p) if link e is traversed by path p, 0 otherwise */
		op.setInputParameter("A_destp", A_destp); /* 1 in position (d,p) if demand d is served by path p, 0 otherwise */ 
		op.setInputParameter("cf_o", list_cand_o.stream().map(o->getFixedSettingCost(o, 0.0)).collect(Collectors.toList()) , "row"); /* fixed costs */
		op.setInputParameter("cBlock_d", list_cand_d.stream().map(d->getPenalizationPerUndeliveredUnit(d, 1000.0)).collect(Collectors.toList()) , "row"); /* fixed costs */
		op.setInputParameter("hMax_o", list_cand_o.stream().map(d->getMaxProductionPerPlantIfPlantExists(d, Double.MAX_VALUE)).collect(Collectors.toList()) , "row"); /* fixed costs */
		op.setInputParameter("hMin_o", list_cand_o.stream().map(d->getMinProductionPerPlantIfPlantExists(d, 0.0)).collect(Collectors.toList()) , "row"); /* fixed costs */
		op.setInputParameter("h_dest", h_dest , "row"); /* demanded goods */
		op.setInputParameter("u_e", netPlan.getVectorLinkCapacity(), "row"); /* for each link, its unused capacity (the one not used by any mulitcast trees) */

		op.addDecisionVariable("x_p", false, new int[]{1,P}, new double [P] , maxTraffic_p); // goods carried in path p. Includes limit in max distance
		if (NUM_A > 0)
			op.addDecisionVariable("y_a", true, new int[]{1,NUM_A}, y_a_min , y_a_max); // number of distribution centers in location a. Includes constraints in max and min number of distribution centers
		op.addDecisionVariable("y_o", true, new int[]{1,NUM_O}, y_o_min , y_o_max); // number of plants in origin nodes. Includes constraints in min max number of plants
		op.addDecisionVariable("b_d", false, new int[]{1,NUM_D}, new double [NUM_D] , minHDestAndMaxBlockedGoods_dest); // traffic to each destination that is blocked. Includes constraint in max penalization

		if (NUM_A > 0)
			op.setObjectiveFunction("minimize", "((c_e * A_ep) * x_p') + (cf_o * y_o') + (cf_a * y_a') + (cBlock_d * b_d')");
		else
			op.setObjectiveFunction("minimize", "((c_e * A_ep) * x_p') + (cf_o * y_o') + (cBlock_d * b_d')");
		
//		op.setObjectiveFunction("minimize", "(cf_o * y_o') + (cf_a * y_a') + (cBlock_d * b_d')");

		op.addConstraint("A_op * x_p' <= (y_o .* hMax_o)'"); 
		op.addConstraint("A_op * x_p' >= (y_o .* hMin_o)'"); 
		op.addConstraint("A_destp * x_p' == (h_dest + b_d)'"); 
		op.addConstraint("A_ep * x_p' <= u_e'"); /* the traffic in each link cannot exceed its capacity  */
		op.addConstraint("sum (y_o) <= " + maxNumberOfPlants.getInt()); 
		op.addConstraint("sum (y_o) >= " + minNumberOfPlants.getInt()); 
		if (NUM_A > 0)
		{
			op.addConstraint("sum (y_a) <= " + maxNumberOfDistributionCenters.getInt()); 
			op.addConstraint("sum (y_a) >= " + minNumberOfDistributionCenters.getInt()); 
			op.addConstraint("A_ap * x_p' <= (y_a .* hMax_a)'"); 
			op.addConstraint("A_ap * x_p' >= (y_a .* hMin_a)'"); 
		}

		op.solve(solverName.getString (), "solverLibraryName", solverLibraryName.getString () , "maxSolverTimeInSeconds" , maxSolverTimeInSeconds.getDouble ());

		/* If no solution is found, quit */
		if (op.feasibleSolutionDoesNotExist()) throw new Net2PlanException("The problem has no feasible solution");
		if (!op.solutionIsFeasible()) throw new Net2PlanException("A feasible solution was not found");

		final DoubleMatrix1D x_p = op.getPrimalSolution("x_p").view1D();
		final DoubleMatrix1D y_a = NUM_A > 0? op.getPrimalSolution("y_a").view1D() : null;
		final DoubleMatrix1D y_o = op.getPrimalSolution("y_o").view1D();
		final DoubleMatrix1D b_d = op.getPrimalSolution("b_d").view1D();
		final double totalCost = op.getOptimalCost();

		/* Save the output solution in the netplan object */
		for (Route r : netPlan.getRoutes())
		{
			final int p = r.getIndex();
			final double traf = x_p.get(p) < 1e-3? 0.0 : x_p.get(p);
			r.setCarriedTraffic(traf , traf);
		}
		
		/* Remove unused routes and demands, and make offered traffic equal to carried traffic */
		for (Demand d : netPlan.getDemands()) d.setOfferedTraffic(d.getCarriedTraffic());
		for (Demand d : new ArrayList<> (netPlan.getDemands()))
			if (d.getCarriedTraffic() <= 1e-3)
				d.remove();
		
		for (Node n : list_cand_a)
		{
			final int aIndex = cand_a.inverseBidiMap().getKey(n);
			setNumberOfPlacedDistributionCenters(n, (int) Math.round(y_a.get(aIndex)));
		}
		for (Node n : list_cand_o)
		{
			final int oIndex = cand_o.inverseBidiMap().getKey(n);
			setNumberOfPlacedPlants(n, (int) Math.round(y_o.get(oIndex)));
		}

		/* Checks are performed in the NetPlan object, instead of the op variables. Then, we test the true final result returned */

		/* Long routes are not used */
		if (maxDistanceOriginToDestinationInKm.getDouble() > 0) check (netPlan.getRoutes().stream().filter(r->r.getCarriedTraffic() > 0).allMatch(r->r.getLengthInKm() <= maxDistanceOriginToDestinationInKm.getDouble()));

		for (Link e : netPlan.getLinks())
			check (e.getCapacity() >= e.getCarriedTraffic() - 1e-3);
		
		int totalNumDcs = 0; int totalNumPlants = 0; double totalBlockedTraffic = 0;
		for (Node n : netPlan.getNodes())
		{
			check (getNodeType(n, null) != null);
			switch (getNodeType(n, null))
			{
			case DESTINATION:
			{
				final double trafReceived = n.getIncomingRoutes().stream().mapToDouble(r->r.getCarriedTraffic()).sum();
				final double maxBlockedTraffic = Math.max(0, getMaxBlockedGoods(n, 0.0));
				final double demandedTraffic = getDemandSize(n, 0.0);
				final double blockedTraffic = demandedTraffic - trafReceived;
				totalBlockedTraffic += blockedTraffic;
				check (trafReceived <= demandedTraffic + 1e-3);
				check (demandedTraffic - trafReceived <= maxBlockedTraffic + 1e-3);
				break;
			}
			case DISTRIBUTIONCENTER:
			{
				final int num = getNumberOfPlacedDistributionCenters(n, -1); check (num >= 0);
				totalNumDcs += num;
				check (num <= (int) getMaxNumDistributionCentersInLocation(n, Integer.MAX_VALUE));
				check (num >= (int) getMinNumDistributionCentersInLocation(n, 0));
				check (n.getIncomingRoutes().isEmpty());
				check (n.getOutgoingRoutes().isEmpty());
				final double traf = n.getAssociatedRoutes().stream().mapToDouble(r->r.getCarriedTraffic()).sum ();
				final int numStocks = getMaxNumDistributionCentersInLocation(n, 0);
				check (traf <= numStocks * getMaxStorageCapacity(n, 0.0) + 1e-3);
				check (traf >= numStocks * getMinStorageCapacity(n, 0.0) - 1e-3);
				break;
			}
			case PLANT:
			{
				final int num = getNumberOfPlacedPlants(n, -1); check (num >= 0);
				totalNumPlants += num;
				check (num <= (int) getMaxNumPlantsInLocation(n, Integer.MAX_VALUE));
				check (num >= (int) getMinNumPlantsInLocation(n, 0));
				final double outTraf = n.getOutgoingRoutes().stream().mapToDouble(r->r.getCarriedTraffic()).sum ();
				if (num == 0) check (outTraf < 1e-3);
				final double maxOutTrafPerPlant = getMaxProductionPerPlantIfPlantExists(n, 0.0);
				final double minOutTrafPerPlant = getMinProductionPerPlantIfPlantExists(n, 0.0);
				check (outTraf <= num * maxOutTrafPerPlant + 1e-3);
				check (outTraf >= minOutTrafPerPlant * num - 1e-3);
				break;
			}
			default:
				check (false);
			}
		}
		
		check (totalNumDcs >= minNumberOfDistributionCenters.getInt());
		check (totalNumDcs <= maxNumberOfDistributionCenters.getInt());
		check (totalNumPlants >= minNumberOfPlants.getInt());
		check (totalNumPlants <= maxNumberOfPlants.getInt());
		check (Math.abs(totalBlockedTraffic - b_d.zSum()) < 1e-3);
		return "#Plants: " + totalNumPlants + ", #DCs: " + totalNumDcs + ", #paths: " + netPlan.getRoutes().size() + ", Total length (km): " + netPlan.getRoutes().stream().mapToDouble(r->r.getLengthInKm()).sum() + ", blocked traffic: " + totalBlockedTraffic + ", total cost: " + totalCost;
	}

	@Override
	public String getDescription()
	{
		return "This algorithm solves a several variants of logistic problems.";
	}

	@Override
	public List<Triple<String, String, String>> getParameters() 
	{
		/* Returns the parameter information for all the InputParameter objects defined in this object (uses Java reflection) */
		return InputParameter.getInformationAllInputParameterFieldsOfObject(this);
	}
	
	private static void checkIsPlantOrStockCandidateLocation (Node n) { if (getNodeType(n , null) != NODETYPE.PLANT && getNodeType(n , null) != NODETYPE.DISTRIBUTIONCENTER) throw new Net2PlanException (); }
	private static void checkIsPlantCandidateLocation (Node n) { if (getNodeType(n , null) != NODETYPE.PLANT) throw new Net2PlanException (); }
	private static void checkIsStockCandidateLocation (Node n) { if (getNodeType(n , null) != NODETYPE.DISTRIBUTIONCENTER) throw new Net2PlanException (); }
	private static void checkIsDestinationCandidateLocation (Node n) { if (getNodeType(n , null) != NODETYPE.DESTINATION) throw new Net2PlanException (); }

	private static <T> BidiMap<T,Integer> createBidiMap (Collection<T> col)
	{
		final BidiMap<T,Integer> res = new DualHashBidiMap<> ();
		for (T e : col) res.put(e, res.size());
		return res;
	}
	private static Link getLink (Node a  , Node b)
	{
		final NetPlan np = a.getNetPlan();
		final SortedSet<Link> links = np.getNodePairLinks(a, b, false);
		if (links.size () != 1) throw new RuntimeException ();
		return links.iterator().next();
				
	}

	private static void check (boolean val) { if (!val) throw new RuntimeException (); }

	private void createInitialTopology (NetPlan netPlan)
	{
		netPlan.assignFrom(new NetPlan ());
		final int num_o = aaa_numOrigins.getInt();
		final int num_a = aaa_numDcs.getInt();
		final int num_d = aaa_numDestinations.getInt();
		for (int cont = 0; cont < num_o ; cont ++)
		{
			final Node n = netPlan.addNode(0, cont, "Origin-" + cont, null);
			setNodeType (n , NODETYPE.PLANT);
			setFixedSettingCost(n, 100.0);
			setMinProductionPerPlantIfPlantExists(n, 0.0);
			setMaxProductionPerPlantIfPlantExists(n, Double.MAX_VALUE);
			setMinNumPlantsInLocation (n , 0);
			setMaxNumPlantsInLocation (n , Integer.MAX_VALUE);
		}
		for (int cont = 0; cont < num_a ; cont ++)
		{
			final Node n = netPlan.addNode(5, cont, "DistributionCenter-" + cont, null);
			setNodeType (n , NODETYPE.DISTRIBUTIONCENTER);
			setFixedSettingCost(n, 100.0);
			setMinStorageCapacity(n, 0.0);
			setMaxStorageCapacity(n, Double.MAX_VALUE);
			setMinNumDistributionCentersInLocation (n , 0);
			setMaxNumDistributionCentersInLocation (n , Integer.MAX_VALUE);
		}
		for (int cont = 0; cont < num_d ; cont ++)
		{
			final Node n = netPlan.addNode(10, cont, "Destination-" + cont, null);
			setNodeType (n , NODETYPE.DESTINATION);
			setPenalizationPerUndeliveredUnit(n, 100.0);
			setMaxBlockedGoods(n, 0.0);
			setDemandSize(n, 50.0);
		}
	}
	
}
