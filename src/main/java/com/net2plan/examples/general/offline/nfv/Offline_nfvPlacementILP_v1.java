package com.net2plan.examples.general.offline.nfv;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.jom.OptimizationProblem;
import com.net2plan.interfaces.networkDesign.Demand;
import com.net2plan.interfaces.networkDesign.IAlgorithm;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkElement;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.interfaces.networkDesign.Resource;
import com.net2plan.interfaces.networkDesign.Route;
import com.net2plan.utils.Constants.RoutingType;
import com.net2plan.utils.InputParameter;
import com.net2plan.utils.Pair;
import com.net2plan.utils.Triple;

import cern.colt.matrix.tdouble.DoubleFactory1D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;

public class Offline_nfvPlacementILP_v1 implements IAlgorithm
{
	private InputParameter k = new InputParameter ("k", (int) 5 , "Maximum number of admissible service chain paths per demand" , 1 , Integer.MAX_VALUE);
	private InputParameter shortestPathType = new InputParameter ("shortestPathType", "#select# hops km" , "Criteria to compute the shortest path. Valid values: 'hops' or 'km'");
	private InputParameter nfvsInfo = new InputParameter ("nfvsInfo", "NAT 1 1 1 1 1 ; FW 1 1 1 1 1" , "Info of NFVs that could be placed, separated by ';'. Each NFV info has six space-separated parameters: 1) type, 2) cost (measured in same units as the cost of one BW unit in a link),  3) CPU use, 4) RAM use, 5) HD use, 6) capacity in same units as traffic");
	private InputParameter nonBifurcatedRouting = new InputParameter ("nonBifurcatedRouting", false , "True if the routing is constrained to be non-bifurcated");
	private InputParameter overideBaseResourcesInfo = new InputParameter ("overideBaseResourcesInfo", true , "If true, the current resources in tne input n2p are removed, and for each node aone CPU, RAM and HD resources are created, with the capacities defined in input parameter defaultCPU_RAM_HD_Capacities");
	private InputParameter defaultCPU_RAM_HD_Capacities = new InputParameter ("defaultCPU_RAM_HD_Capacities", "100 100 100" , "THe default capacity values (space separated) of CPU, RAM, HD");
	private InputParameter overideSequenceTraversedNFVs = new InputParameter ("overideSequenceTraversedNFVs", true , "If true, all demands will reset the sequence of NFVs to traverse, to this (NFV types in this param are ; separated)");
	private InputParameter defaultSequenceNFVsToTraverse = new InputParameter ("defaultSequenceNFVsToTraverse", "FW NAT" , "The default sequence of NFVs that demands must traverse");
	private InputParameter solverName = new InputParameter ("solverName", "#select# glpk ipopt xpress cplex", "The solver name to be used by JOM. GLPK and IPOPT are free, XPRESS and CPLEX commercial. GLPK, XPRESS and CPLEX solve linear problems w/w.o integer contraints. IPOPT is can solve nonlinear problems (if convex, returns global optimum), but cannot handle integer constraints");
	private InputParameter solverLibraryName = new InputParameter ("solverLibraryName", "" , "The solver library full or relative path, to be used by JOM. Leave blank to use JOM default.");
	private InputParameter maxSolverTimeInSeconds = new InputParameter ("maxSolverTimeInSeconds", (double) -1 , "Maximum time granted to the solver to solve the problem. If this time expires, the solver returns the best solution found so far (if a feasible solution is found)");

	private InputParameter optimizationTarget = new InputParameter ("optimizationTarget", "#select# min-total-cost" , "Type of optimization target. Choose among (i) minimize the link BW plus the cost of instantiated resources (assumed measured in cost units equal to the cost of one link BW unit)");
	private InputParameter maxLengthInKmPerSubpath = new InputParameter ("maxLengthInKmPerSubpath", (double) -1 , "Subpaths (parts of the path split by resources) longer than this in km are considered not admissible. A non-positive number means this limit does not exist");
	private InputParameter maxNumHopsPerSubpath = new InputParameter ("maxNumHopsPerSubpath", (int) -1 , "Subpaths (parts of the path split by resources) longer than this in number of hops are considered not admissible. A non-positive number means this limit does not exist");
	private InputParameter maxPropDelayInMsPerSubpath = new InputParameter ("maxPropDelayInMsPerSubpath", (double) -1 , "Subpaths (parts of the path split by resources) longer than this in propagation delay (in ms) are considered not admissible. A non-positive number means this limit does not exist");

	@Override
	public String executeAlgorithm(NetPlan netPlan, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
	{
		/* Initialize all InputParameter objects defined in this object (this uses Java reflection) */
		InputParameter.initializeAllInputParameterFieldsOfObject(this, algorithmParameters);
		if (solverName.getString ().equalsIgnoreCase("ipopt")) throw new Net2PlanException ("With IPOPT solver, the routing cannot be constrained to be non-bifurcated");
		
		/* Initialize variables */
		final int E = netPlan.getNumberOfLinks();
		final int D = netPlan.getNumberOfDemands();
		final int N = netPlan.getNumberOfNodes();
		final double PRECISION_FACTOR = Double.parseDouble(net2planParameters.get("precisionFactor"));
		if (E == 0 || D == 0) throw new Net2PlanException("This algorithm requires a topology with links and a demand set");
	
		/* Remove all unicast routed traffic. Any multicast routed traffic is kept */
		netPlan.removeAllUnicastRoutingInformation();
		netPlan.setRoutingType(RoutingType.SOURCE_ROUTING);
	
		/* Add all the k-shortest candidate routes to the netPlan object carrying no traffic */
		final DoubleMatrix1D linkCostVector = shortestPathType.getString().equalsIgnoreCase("hops")? DoubleFactory1D.dense.make (E , 1.0) : netPlan.getVectorLinkLengthInKm();
	
		if (overideBaseResourcesInfo.getBoolean())
		{
			final List<Double> cpuRamHdCap = Arrays.stream(StringUtils.split(defaultCPU_RAM_HD_Capacities.getString(), " ")).map(e -> Double.parseDouble(e)).collect (Collectors.toList());
			netPlan.removeAllResources();
			for (Node n : netPlan.getNodes())
			{
				netPlan.addResource("CPU", "", n, cpuRamHdCap.get(0), "", null, 0.0, null);
				netPlan.addResource("RAM", "", n, cpuRamHdCap.get(1), "", null, 0.0, null);
				netPlan.addResource("HD", "", n, cpuRamHdCap.get(2), "", null, 0.0, null);
			}
		}
		if (overideSequenceTraversedNFVs.getBoolean())
		{
			final List<String> nfvsToTraverse = Arrays.stream(StringUtils.split(defaultSequenceNFVsToTraverse.getString() , " ")).collect(Collectors.toList());
			for (Demand d : netPlan.getDemands())
				d.setServiceChainSequenceOfTraversedResourceTypes(nfvsToTraverse);
		}
		
		String [] nfvsInfoArray = StringUtils.split(nfvsInfo.getString() , ";");
		final int NUMNFV = nfvsInfoArray.length;
		List<String> nfv_type = new ArrayList<String> ();
		DoubleMatrix1D nfv_cost = DoubleFactory1D.dense.make(NUMNFV);
		DoubleMatrix1D nfv_cpu = DoubleFactory1D.dense.make(NUMNFV);
		DoubleMatrix1D nfv_ram = DoubleFactory1D.dense.make(NUMNFV);
		DoubleMatrix1D nfv_hd = DoubleFactory1D.dense.make(NUMNFV);
		DoubleMatrix1D nfv_cap = DoubleFactory1D.dense.make(NUMNFV);
		for (String nfvInfo : nfvsInfoArray)
		{
			final String [] fields = StringUtils.split(nfvInfo , " ");
			if (fields.length != 6) throw new Net2PlanException ("Wrong parameter format for NFV info");
			final String type = fields [0];
			if (nfv_type.contains(type)) throw new Net2PlanException ("Wrong parameter format for NFV info: cannot repeat NFV types");
			final int index = nfv_type.size();
			nfv_type.add (type);
			nfv_cost.set(index, Double.parseDouble(fields [1]));
			nfv_cpu.set(index, Double.parseDouble(fields [2]));
			nfv_ram.set(index, Double.parseDouble(fields [3]));
			nfv_hd.set(index, Double.parseDouble(fields [4]));
			nfv_cap.set(index, Double.parseDouble(fields [5]));
		}

		DoubleMatrix1D cpu_n = DoubleFactory1D.dense.make(N);
		DoubleMatrix1D ram_n = DoubleFactory1D.dense.make(N);
		DoubleMatrix1D hd_n = DoubleFactory1D.dense.make(N);
		for (int index_n = 0; index_n < N ; index_n ++)
		{
			final Node n = netPlan.getNode(index_n);
			Set<Resource> cpuResources = n.getResources("CPU"); if (cpuResources.size() > 1) throw new Net2PlanException ("A node cannot have more than one resource of type CPU, or RAM or HD");
			Set<Resource> ramResources = n.getResources("RAM"); if (ramResources.size() > 1) throw new Net2PlanException ("A node cannot have more than one resource of type CPU, or RAM or HD");
			Set<Resource> hdResources = n.getResources("HD"); if (hdResources.size() > 1) throw new Net2PlanException ("A node cannot have more than one resource of type CPU, or RAM or HD");
			cpu_n.set(index_n, cpuResources.iterator().next().getCapacity());
			ram_n.set(index_n, ramResources.iterator().next().getCapacity());
			hd_n.set(index_n, hdResources.iterator().next().getCapacity());
		}

		/* Instantiate "preliminary" NFV resources in the nodes, not consuming any base resource, and with a capacity as if one single instance existed. 
		 * This is needed so service chain paths can be precomputed. 
		 * If a node has not enough CPU/RAM/HD to even instantiate one single instance of a NFV type, do not add this resource in that node */
		for (Node n : netPlan.getNodes())
		{
			final double cpuNode = cpu_n.get(n.getIndex());
			final double ramNode = ram_n.get(n.getIndex());
			final double hdNode = hd_n.get(n.getIndex());
			for (int indexNFVType = 0 ; indexNFVType < NUMNFV ; indexNFVType ++)
			{
				if (nfv_cpu.get(indexNFVType) > cpuNode) continue;
				if (nfv_ram.get(indexNFVType)  > ramNode) continue;
				if (nfv_hd.get(indexNFVType)  > hdNode) continue;
				netPlan.addResource(nfv_type.get(indexNFVType), "", n, nfv_cap.get(indexNFVType), "", n.getResources("CPU", "RAM", "HD").stream().collect(Collectors.toMap(e -> e ,  e-> 0.0)) , 0, null);
			}
		}
		
		/* Create the candidate service path list */
		final Map<Demand,List<List<NetworkElement>>> cpl = netPlan.computeUnicastCandidateServiceChainList (linkCostVector, 
				null , k.getInt(), -1 , maxLengthInKmPerSubpath.getDouble(), maxNumHopsPerSubpath.getInt(), maxPropDelayInMsPerSubpath.getDouble());
		for (Demand d : netPlan.getDemands())
		{
			for (List<NetworkElement> path : cpl.get(d))
				netPlan.addServiceChain(d, 0, Collections.nCopies(path.size() , 0.0), path, null);
		}
		final int P = netPlan.getNumberOfRoutes(); 
	
		/* Create the optimization problem object (JOM library) */
		OptimizationProblem op = new OptimizationProblem();
	
		/* Set some input parameters to the problem */
		op.setInputParameter("u_e", netPlan.getVectorLinkSpareCapacity(), "row"); /* for each link, its unused capacity (the one not used by any mulitcast trees) */
		op.setInputParameter("A_dp", netPlan.getMatrixDemand2RouteAssignment()); /* 1 in position (d,p) if demand d is served by path p, 0 otherwise */ 
		op.setInputParameter("A_ep", netPlan.getMatrixLink2RouteAssignment()); /* 1 in position (e,p) if link e is traversed by path p, 0 otherwise */
		op.setInputParameter("h_d", netPlan.getVectorDemandOfferedTraffic(), "row"); /* for each demand, its offered traffic */
		op.setInputParameter("h_p", netPlan.getVectorRouteOfferedTrafficOfAssociatedDemand () , "row"); /* for each path, the offered traffic of its demand */
		op.setInputParameter("c_f", nfv_cost , "row"); /* for each NFV type, its cost */
		op.setInputParameter("cpu_f", nfv_cpu , "row"); /* for each NFV type, its CPU */
		op.setInputParameter("ram_f", nfv_ram , "row"); /* for each NFV type, its RAM */
		op.setInputParameter("HD_f", nfv_hd , "row"); /* for each NFV type, its HD */
		op.setInputParameter("cap_f", nfv_cap , "row"); /* for each NFV type, its capacity */
		op.setInputParameter("cpu_n", cpu_n , "row"); /* for each node, CPU capacity  */
		op.setInputParameter("ram_n", ram_n , "row"); /* for each node, RAM capacity  */
		op.setInputParameter("hd_n", hd_n , "row"); /* for each node, HD capacity  */
		
		/* Write the problem formulations */
		if (optimizationTarget.getString ().equals ("min-total-cost")) 
		{
			op.setInputParameter("l_p", netPlan.getVectorRouteNumberOfLinks() , "row"); /* for each path, the number of traversed links */
			op.addDecisionVariable("xx_p", nonBifurcatedRouting.getBoolean() , new int[] { 1, P }, 0, 1); /* the FRACTION of traffic of demand d(p) that is carried by p */
			op.addDecisionVariable("y_nf", true , new int[] { N, NUMNFV }, 0, Integer.MAX_VALUE); /* the number of NFVs of each type that are instantiated */
	
			op.setObjectiveFunction("minimize", "sum (l_p .* h_p .* xx_p) + sum (y_nf * c_f') "); 

			op.addConstraint("A_dp * xx_p' == 1"); /* for each demand, the 100% of the traffic is carried (summing the associated paths) */
			op.addConstraint("A_ep * (h_p .* xx_p)' <= u_e'"); /* the traffic in each link cannot exceed its capacity  */
			op.addConstraint("y_nf * cpu_f' <= cpu_n'"); 
			op.addConstraint("y_nf * ram_f' <= ram_n'"); 
			op.addConstraint("y_nf * hd_f' <= hd_n'"); 
			
			for (int indexNFVType = 0 ; indexNFVType < NUMNFV ; indexNFVType ++)
			{
				final String type = nfv_type.get(indexNFVType);
				Pair<List<Resource>,DoubleMatrix2D> info = netPlan.getMatrixResource2RouteAssignment(type); 
				op.setInputParameter("f", indexNFVType); /* K in position (res,p) if resource res is traversed by path p K times */
				op.setInputParameter("A_nfvp", info.getSecond()); /* K in position (res,p) if resource res is traversed by path p K times */
				op.setInputParameter("u_nfv", nfv_cap.get(indexNFVType)); /* for each resource link, its unused capacity (the one not used by any mulitcast trees) */
				op.addConstraint("A_nfvp * (h_p .* xx_p)' <= y_nf(all,f) * u_nfv"); /* the traffic in each link cannot exceed its capacity  */
			}
		}
		else throw new Net2PlanException ("Unknown optimization target " + optimizationTarget.getString());
	
		
		System.out.println ("solverLibraryName: " +  solverLibraryName.getString ());
		op.solve(solverName.getString (), "solverLibraryName", solverLibraryName.getString () , "maxSolverTimeInSeconds" , maxSolverTimeInSeconds.getDouble ());
		//op.solve(solverName.getString (), "maxSolverTimeInSeconds" , maxSolverTimeInSeconds.getDouble ());
	
		System.out.println ("solverLibraryName: " +  solverLibraryName.getString ());
	
		/* If no solution is found, quit */
		if (op.feasibleSolutionDoesNotExist()) throw new Net2PlanException("The problem has no feasible solution");
		if (!op.solutionIsFeasible()) throw new Net2PlanException("A feasible solution was not found");
		
		/* Save the solution found in the netPlan object */
		final DoubleMatrix1D xx_p = op.getPrimalSolution("xx_p").view1D();
		final DoubleMatrix2D y_nf = op.getPrimalSolution("y_nf").view2D();

		for (Route r : netPlan.getRoutes())
		{
			final double carriedTrafficAndOccupiedLinkCapacity = xx_p.get(r.getIndex()) * r.getDemand().getOfferedTraffic();
			r.setCarriedTraffic(carriedTrafficAndOccupiedLinkCapacity, carriedTrafficAndOccupiedLinkCapacity);
		}

		for (Node n : netPlan.getNodes())
		{
			final Resource cpu = n.getResources("CPU").iterator().next(); 
			final Resource ram = n.getResources("RAM").iterator().next(); 
			final Resource hd = n.getResources("HD").iterator().next(); 
			for (int indexNFVType = 0 ; indexNFVType < NUMNFV ; indexNFVType ++)
			{
				final String type = nfv_type.get(indexNFVType);
				if (n.getResources(type).isEmpty()) continue;
				final Resource nfv = n.getResources(type).iterator().next();
				final int this_ynf = (int) y_nf.get(n.getIndex(), indexNFVType);
				if (this_ynf == 0) nfv.remove();
				Map<Resource,Double> occupyInBaseResources = new HashMap<Resource,Double> ();
				occupyInBaseResources.put(cpu, this_ynf * nfv_cpu.get(indexNFVType));
				occupyInBaseResources.put(ram, this_ynf * nfv_ram.get(indexNFVType));
				occupyInBaseResources.put(hd, this_ynf * nfv_hd.get(indexNFVType));
				nfv.setCapacity(this_ynf * nfv_cap.get(indexNFVType), occupyInBaseResources);
			}
		}
		
		netPlan.removeAllRoutesUnused(PRECISION_FACTOR); // routes with zero traffic (or close to zero, with PRECISION_FACTOR tolerance)
	
		return "Ok!: The solution found is guaranteed to be optimal: " + op.solutionIsOptimal() + ". Number routes = " + netPlan.getNumberOfRoutes();
	}
	
	@Override
	public String getDescription()
	{
		return "Algorithm for NFV placement";
	}
	
	
	@Override
	public List<Triple<String, String, String>> getParameters()
	{
		/* Returns the parameter information for all the InputParameter objects defined in this object (uses Java reflection) */
		return InputParameter.getInformationAllInputParameterFieldsOfObject(this);
	}

}