package com.net2plan.research.algorithm;

import com.google.common.collect.Lists;
import com.net2plan.interfaces.networkDesign.IAlgorithm;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.Resource;
import com.net2plan.libraries.GraphUtils;
import com.net2plan.research.niw.networkModel.*;
import com.net2plan.utils.InputParameter;
import com.net2plan.utils.Pair;
import com.net2plan.utils.Triple;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DimensioningPlanning implements IAlgorithm {

	@Override
	public String executeAlgorithm(NetPlan netPlan, Map<String, String> algorithmParameters, Map<String, String> net2planParameters) 
	{
		final long seed = Long.parseLong(algorithmParameters.get("randomSeed"));
		final Random rng = new Random (seed);
		final Double Lmax = Double.parseDouble(algorithmParameters.get("Lmax"));
		final int K = Integer.parseInt(algorithmParameters.get("K"));
		final int numServices = Integer.parseInt(algorithmParameters.get("numServices"));
		final String excelFile = algorithmParameters.get("excelFile");

		// First of all, initialize all parameters
		InputParameter.initializeAllInputParameterFieldsOfObject(this, algorithmParameters);

		File excelPath = new File(excelFile);
		WNet wNet = ImportMetroNetwork.importFromExcelFile(excelPath);
		netPlan.copyFrom(wNet.getNetPlan());
		wNet = new WNet(netPlan);
		final OpticalSpectrumManager osm = OpticalSpectrumManager.createFromRegularLps(wNet);

		final double linerate_Gbps = 10;
		final double Tmax_Gbps = 1.0;
		final int slotsPerLightpath = 4;

		// Perform here initial checks
		/*
		 * Remove any existing lightpath, VNF instances and any existing service chain
		 */
		for (WLightpathRequest lpr : wNet.getLightpathRequests())
			lpr.remove(); // this removes also the lps

		for (WVnfInstance vnf : wNet.getVnfInstances())
			vnf.remove();

		for (WServiceChain sc : wNet.getServiceChains())
			sc.remove();

		/* Create Services and VNFs */
		for (int i = 0; i < numServices; i++) {
			int numberOfVNFsInThisService = (int) randomWithRange(rng , 1, 5);
			List<String> vnfsUpstream = new ArrayList<String>();
			for (int j = 0; j < numberOfVNFsInThisService; j++) {
				WVnfType vnfType = new WVnfType("VNF" + j + "_Service" + i, // Name
												100000000, // Capacity_Gbps
												2, // CPU
												4, // RAM
												20, // HD
												1.0, // Processing Time
												false, // Constrained to some nodes?
												null, // Instantiation nodes
												""); // Arbitrary Params
				wNet.addOrUpdateVnfType(vnfType);

				vnfsUpstream.add("VNF" + j + "_Service" + i);
			}
			List<String> vnfsDownstream = Lists.reverse(vnfsUpstream);
			final double trafficPerUserInGbps = randomWithRange(rng, 0.1, 1.0);
			List<Double> trafficExpansion = Collections.nCopies(numberOfVNFsInThisService, 1.0);
			List<Double> latencyInMs = Collections.nCopies(numberOfVNFsInThisService + 1, randomWithRange(rng , 0.5, Lmax));
			System.out.println(
					"Service" + i + " latency is: " + latencyInMs.get(0) + " and it has " + vnfsUpstream.size() + " VNFs");

			WUserService userService = new WUserService("Service" + i, // Name
														vnfsUpstream, // VNF Upstream
														vnfsDownstream, // VNF Downstream
														trafficExpansion, // Traffic expansion Up
														trafficExpansion, // Traffic expansion Down
														latencyInMs, // Max Latency Up
														latencyInMs, // Max Latency Down
														randomWithRange(rng , 0.1, 1.0), // Injection expansion
														false, // Ending in core node?
														""); // Arbitrary params
			userService.setArbitraryParamString(trafficPerUserInGbps + "");
			wNet.addOrUpdateUserService(userService);
		}

		/* Adding lightpaths */
		for (WNode origin : wNet.getNodes()) {
			for (WNode destination : wNet.getNodes()) {
				if (!origin.equals(destination)) {

					List<List<WFiber>> cpl = wNet.getKShortestWdmPath(K, origin, destination, Optional.empty());
					if (cpl.get(0).size() == 1) { // adjacent nodes
						List<WFiber> uniqueFiber = cpl.get(0);
						WLightpathRequest lpr = wNet.addLightpathRequest(origin, destination, linerate_Gbps, false);
						Optional<SortedSet<Integer>> wl = osm.spectrumAssignment_firstFit(uniqueFiber,
								slotsPerLightpath, Optional.empty());
						if (wl.isPresent()) {
							WLightpathUnregenerated lp = lpr.addLightpathUnregenerated(uniqueFiber, wl.get(), false);
							osm.allocateOccupation(lp, uniqueFiber, wl.get());
							Pair<WIpLink, WIpLink> ipLink = wNet.addIpLink(origin, destination, linerate_Gbps, false);
							lpr.coupleToIpLink(ipLink.getFirst());
						}
					}

				}
			}
		}

		/* ############################# ALGORITHM ############################# */
		System.out.println("####################### STARTING ALGORITHM #######################");
		WNode greatestNode = getGreatestNode(wNet); // Necessary in order to calculate the traffic per service
		System.out.println(
				"The greatest node is " + greatestNode.getName() + " with " + greatestNode.getPopulation() + " people");

		/* Normalize so the maximum traffic of any service in any node is TMax */
		Map<Pair<WNode , WUserService> , Double> trafficGeneratedPerNodeServicePairNormalized = new HashMap<> ();
		for (WNode node : wNet.getNodes()) 
			for (WUserService service : wNet.getUserServicesInfo().values())
				trafficGeneratedPerNodeServicePairNormalized.put(Pair.of(node, service), node.getPopulation() * Double.parseDouble(service.getArbitraryParamString()));
		final List<Pair<WNode , WUserService>> orderedRequestsHigherToLowerTraffic = trafficGeneratedPerNodeServicePairNormalized.keySet().stream().
				sorted((p1,p2)->Double.compare(trafficGeneratedPerNodeServicePairNormalized.get(p2), trafficGeneratedPerNodeServicePairNormalized.get(p1))).
				collect(Collectors.toList());
		final double largestTrafficGeneratedGbps =  trafficGeneratedPerNodeServicePairNormalized.get(orderedRequestsHigherToLowerTraffic.get(0));
		for (Pair<WNode , WUserService> requestInfo : orderedRequestsHigherToLowerTraffic)
			trafficGeneratedPerNodeServicePairNormalized.put(requestInfo, trafficGeneratedPerNodeServicePairNormalized.get(requestInfo) * Tmax_Gbps / largestTrafficGeneratedGbps);
		
		/* Generate service chain requests */
		for (Pair<WNode , WUserService> requestInfo : orderedRequestsHigherToLowerTraffic)
		{
			final WNode node = requestInfo.getFirst();
			final WUserService service = requestInfo.getSecond();
			final double totalTrafficInGbpsOfThisServiceInThisNodeGbps = trafficGeneratedPerNodeServicePairNormalized.get(requestInfo);
			System.out.println(totalTrafficInGbpsOfThisServiceInThisNodeGbps);
			WServiceChainRequest scr = wNet.addServiceChainRequest(node, true, service);
			scr.setCurrentOfferedTrafficInGbps(totalTrafficInGbpsOfThisServiceInThisNodeGbps);
		}				

		/* Algorithm starts here serving service chain requests */
		final List<WServiceChainRequest> orderedServiceChainRequestsHigherToLowerTraffic = wNet.getServiceChainRequests().stream().
				sorted((s1,s2)->Double.compare(s2.getCurrentOfferedTrafficInGbps(), s1.getCurrentOfferedTrafficInGbps())  ).
				collect(Collectors.toList());
		for (WServiceChainRequest scr : orderedServiceChainRequestsHigherToLowerTraffic)
		{
			/*- intento en la topologia IP que hay, con ancho de banda libre. Si ok, y ok con latencia => FIN
					for (nodoDestino : de mas alejado a menos)
					{
						for (destinoIntermedio : nodos con los que mi destino final tiene un lp con capacidad sobrante en IP suficiente para mi)
						{
							intento lp directo desde yo a ese nodo intermedio. miro a ver si ok por latencia. Y si ok => FIN (creo lp, y la service chain va por 2 lps: lp nuevo + lp intermedioAFinal)
						}
						- Intento lp directo con el destino. SI es posible por latencia y espectro => FIN
					}
					bloqueo, no se puede, fin de algoritmo*/
		
			final WNode node = scr.getPotentiallyValidOrigins().first();
			final double totalTrafficInGbpsOfThisServiceInThisNodeGbps = scr.getCurrentOfferedTrafficInGbps();
			System.out.println("totalTrafficInGbpsOfThisServiceInThisNodeGbps: "+totalTrafficInGbpsOfThisServiceInThisNodeGbps);
			final WUserService service = wNet.getUserServicesInfo().get(scr.getUserServiceName());
			System.out.println("---------------------------------------------------------------");
			System.out.println("Iteration " + node.getName() + " & " + service.getUserServiceUniqueId());

			double serviceLatency_ms = service.getListMaxLatencyFromInitialToVnfStart_ms_upstream().get(0);
			System.out.println(service.getUserServiceUniqueId() + " latency: " + truncate(serviceLatency_ms, 2));

			final WNode coreNode = getNearestCoreNode(netPlan, wNet, node).getAsNode();
			System.out.println("Core Node selected: " + coreNode.getName());
			System.out.println(node.getName() + " -> " + coreNode.getName());

			// first, if the node is a CoreNode the VNFS must be instantiated on it. It's
			// the best solution possible.

			// Case 1: The node is Core Node
			if (coreNode.equals(node)) {
				System.out.println("Case 1: The node is a Core Node!!");
				System.out.println("Result: " + service.getUserServiceUniqueId() + " resources allocated in "
						+ node.getName());
				
				setServiceVNFSinEndingNode(wNet, service, node);

				final List<String> vnfsToTraverse = scr.getSequenceVnfTypes();
				final List<List<? extends WAbstractNetworkElement>> paths = wNet.getKShortestServiceChainInIpLayer(
						K, node, node, vnfsToTraverse, Optional.empty(), Optional.empty());
				scr.addServiceChain(paths.get(0),totalTrafficInGbpsOfThisServiceInThisNodeGbps);

				// else, we need to discover the option that meets the requirements.
			} else 
			{
				final Map<WFiber , Double> latencyInMsAssumingOeoInAllHops = wNet.getFibers().stream().collect(Collectors.toMap(e->e, e->0.2 + e.getLengthInKm () / 200.0 ));
				final List<List<WFiber>> kFiberLists = wNet.getKShortestWdmPath(K, node, coreNode, Optional.of(latencyInMsAssumingOeoInAllHops));
				//final double bestLatency = kFiberLists.get(0).stream().mapToDouble(e->latencyInMsAssumingOeoInAllHops.get(e)).sum();

				final List<WFiber> firstFiberLinks = kFiberLists.get(0);
				WNode endingNode = firstFiberLinks.get(firstFiberLinks.size() - 1).getB();
				if (!endingNode.equals(coreNode)) throw new Net2PlanException("Error. These objects must be equal intially.");
				
				while (firstFiberLinks.size() != 0) 
				{

					System.out.print(node.getName());
					Iterator it = firstFiberLinks.iterator();
					while (it.hasNext()) 
					{
						WFiber fiber = (WFiber) it.next();
						System.out.print(" #" + truncate(fiber.getLengthInKm(), 2) + "km,("
								+ truncate(fiber.getNe().getPropagationDelayInMs(), 2) + "ms)# "
								+ fiber.getB().getName());
					}
					
					//Si bestLatency es igual (que creo que sí), me cargo estas dos cosas.
					final double propagationDelay = wNet.getPropagationDelay(firstFiberLinks);
					final double latency = propagationDelay + 2 * 0.1 * firstFiberLinks.size();
					///////////////////////////////////////////////////////////////////////////////
					
					System.out.println();
					System.out.println("Number of links to traverse (hops): " + firstFiberLinks.size());
					System.out.println("Propagation delay is: " + truncate(propagationDelay, 2));
					//System.out.println("Best latency value is: "+bestLatency);
					System.out.println("Total latency between " + node.getName() + " and " + endingNode.getName()
							+ " = " + truncate(latency, 2));
					System.out.println(
							"Latency with direct lightpath is: " + truncate(propagationDelay + 2 * 0.1, 2));
					System.out.println("Service latency is: " + truncate(serviceLatency_ms, 2));

					// Case 2: Through each fiber link
					if (latency < serviceLatency_ms) {

						System.out.println("Case 2: The latency is lower than service latency, so this is the ending node to allocate resources: "+ endingNode.getName());
						System.out.println("Result: " + service.getUserServiceUniqueId()+ " resources allocated in " + endingNode.getName());
						
						setServiceVNFSinEndingNode(wNet, service, endingNode);

						final List<String> vnfsToTraverse = scr.getSequenceVnfTypes();
						final List<List<? extends WAbstractNetworkElement>> paths = wNet.getKShortestServiceChainInIpLayer(
								K, node, endingNode, vnfsToTraverse, Optional.empty(), Optional.empty());
						scr.addServiceChain(paths.get(0),totalTrafficInGbpsOfThisServiceInThisNodeGbps);
						break; //FINISH. GO TO THE NEXT SERVICE
					}
					// Case 3: Reusing direct lightpaths created in previous iterations
					
					boolean served = false;
					List<WLightpathUnregenerated> lpList = wNet.getLightpaths();
					for(WLightpathUnregenerated lp : lpList) {
						
						if(lp.getB().getName().equals(endingNode.getName()) && !existsDirectLightpath(wNet, node.getName(), lp.getB().getName())) {
							
							WNode A = lp.getA();
							List<WFiber> node2A = wNet.getKShortestWdmPath(K, node, A, Optional.empty()).get(0);
							List<WFiber> A2endingNode = wNet.getKShortestWdmPath(K, A, endingNode, Optional.empty()).get(0);
							List<WFiber> finalList = Stream.concat(node2A.stream(), A2endingNode.stream()).collect(Collectors.toList());
							double candidateLatency_ms = 2*0.2 + wNet.getPropagationDelay(finalList);

							
							if(candidateLatency_ms < serviceLatency_ms) {
								
								System.out.println("Case 3: We can reuse the direct lightpath between " + A.getName()
										+ " and " + endingNode.getName() + " to meet the latency requirements.");
								System.out.println("So the optical path is " + node.getName() + " -> " + A.getName()
										+ " -> " + endingNode.getName());
								System.out.println("Latency reusing lightpath: " + candidateLatency_ms);
								System.out.println("Result: " + service.getUserServiceUniqueId()
										+ " resources allocated in " + endingNode.getName());

								List<List<WFiber>> cpl = wNet.getKShortestWdmPath(K, node, A,
										Optional.empty());
								List<WFiber> uniqueFiber = cpl.get(0);
								WLightpathRequest lpr = wNet.addLightpathRequest(node, A, linerate_Gbps,
										false);
								Optional<SortedSet<Integer>> wl = osm.spectrumAssignment_firstFit(uniqueFiber,
										slotsPerLightpath, Optional.empty());

								if (wl.isPresent()) {
									WLightpathUnregenerated lpU = lpr.addLightpathUnregenerated(uniqueFiber, wl.get(),
											false);
									osm.allocateOccupation(lpU, uniqueFiber, wl.get());
									Pair<WIpLink, WIpLink> ipLink = wNet.addIpLink(node, A, linerate_Gbps,
											false);
									lpr.coupleToIpLink(ipLink.getFirst());
								}
						
								setServiceVNFSinEndingNode(wNet, service, endingNode);

								final List<String> vnfsToTraverse = scr.getSequenceVnfTypes();
								final List<List<? extends WAbstractNetworkElement>> paths = wNet.getKShortestServiceChainInIpLayer(
										K, node, endingNode, vnfsToTraverse, Optional.empty(), Optional.empty());
								scr.addServiceChain(paths.get(0),totalTrafficInGbpsOfThisServiceInThisNodeGbps);
								served=true;
								break;//FINISH. GO TO THE NEXT SERVICE
							}
							
						}

					}if(served) break;

					// Case 4: Through direct lightpath
					if ((propagationDelay + 2 * 0.1) < serviceLatency_ms) {// direct lightpath
						System.out.println("Case 4: Checking if there are direct lightpaths created previously");

						if (!existsDirectLightpath(wNet, node.getName(), endingNode.getName())) {
							System.out.println(
									"Case 4.1: No previous direct lightpaths.... enabling lightpath between "
											+ node.getName() + " and " + endingNode.getName());
							
							List<List<WFiber>> cpl = wNet.getKShortestWdmPath(K, node, endingNode,
									Optional.empty());
							List<WFiber> uniqueFiber = cpl.get(0);
							WLightpathRequest lpr = wNet.addLightpathRequest(node, endingNode, linerate_Gbps, false);
							Optional<SortedSet<Integer>> wl = osm.spectrumAssignment_firstFit(uniqueFiber, slotsPerLightpath, Optional.empty());
							
							if (wl.isPresent()) {
								WLightpathUnregenerated lp = lpr.addLightpathUnregenerated(uniqueFiber, wl.get(),false);
								osm.allocateOccupation(lp, uniqueFiber, wl.get());
								Pair<WIpLink, WIpLink> ipLink = wNet.addIpLink(node, endingNode, linerate_Gbps,false);
								lpr.coupleToIpLink(ipLink.getFirst());

							} else throw new Net2PlanException("Error. No spectrum available.");
						}
						System.out.println("Result: " + service.getUserServiceUniqueId()
								+ " resources allocated in " + endingNode.getName());
						
						setServiceVNFSinEndingNode(wNet, service, endingNode);

						final List<String> vnfsToTraverse = scr.getSequenceVnfTypes();
						final List<List<? extends WAbstractNetworkElement>> paths = wNet.getKShortestServiceChainInIpLayer(
								K, node, endingNode, vnfsToTraverse, Optional.empty(), Optional.empty());
						scr.addServiceChain(paths.get(0),totalTrafficInGbpsOfThisServiceInThisNodeGbps);
						break;//FINISH. GO TO THE NEXT SERVICE

					} else {
						// NO FINISH?... move ending node one step and repeat the loop.
						firstFiberLinks.remove(firstFiberLinks.size() - 1);
						if (firstFiberLinks.size() != 0) {
							endingNode = firstFiberLinks.get(firstFiberLinks.size() - 1).getB();
							System.out.println(
									"Latency is greater than the maximum established. Getting new endingNode: "
											+ endingNode.getName());
						}
					}
				} // while

				if (firstFiberLinks.size() == 0)
				{
					System.out.println(
							"No ending nodes meeting the specific latency requirements. The solution is to allocate the resources in the origin node -> "
									+ node.getName());
					System.out.println("Result: " + service.getUserServiceUniqueId()
							+ " resources are allocated in " + node.getName());
					setServiceVNFSinEndingNode(wNet, service, node);

					final List<String> vnfsToTraverse = scr.getSequenceVnfTypes();
					final List<List<? extends WAbstractNetworkElement>> paths = wNet.getKShortestServiceChainInIpLayer(
							K, node, node, vnfsToTraverse, Optional.empty(), Optional.empty());
					scr.addServiceChain(paths.get(0),totalTrafficInGbpsOfThisServiceInThisNodeGbps);
				}
			} // else
		}

		/* ############################# */
		System.out.println("############################################################################");
		System.out.println("LIGHTPATHS");
		
		List<WLightpathUnregenerated> lpList = wNet.getLightpaths();
		System.out.println("Number of lightpaths: "+lpList.size());
		for(WLightpathUnregenerated lp : lpList) System.out.println(lp.getA().getName()+" -> "+lp.getB().getName());

		/* Dimension the VNF instances, consuming the resources CPU, HD, RAM */
		/*
		 * for(WVnfInstance vnf : wNet.getVnfInstances())
		 * vnf.scaleVnfCapacityAndConsumptionToBaseInstanceMultiple();
		 */

		// HDD 	0.0425€/GB	(85€ 	-> 		2000GB)
		// CPU	35€/Core	(70€	->		2 Cores)
		// RAM 	12.5€/GB	(50€	->		4GB)
		double finalCost = 0;
		
		//COMPUTE FINAL COST
		for(WNode node : wNet.getNodes()) {
			finalCost = finalCost + Math.sqrt((85/2000)*node.getOccupiedHdGB()+(70/2)*node.getOccupiedCpus()+(50/4)*node.getOccupiedRamGB());
		}
		
		final File folder = new File (Lmax.toString());
		if (folder.exists() && folder.isFile()) throw new Net2PlanException ("The folder is a file");
		if (!folder.exists()) folder.mkdirs();
		
		//SORTED BY NAME
		final List<String> summaryString = new ArrayList<> ();
		summaryString.add(finalCost + "");
		for(WNode node : wNet.getNodes()) {		
			summaryString.add(node.getName() + "");
			summaryString.add(node.getOccupiedCpus() + "");
			summaryString.add(node.getOccupiedRamGB() + "");
			summaryString.add(node.getOccupiedHdGB() + "");
		}
		writeFile (new File (folder , "sortedByName" + 0  + ".txt") , summaryString);
		
		//SORTED BY POPULATION
		summaryString.clear();
		summaryString.add(finalCost + "");

		Comparator<WNode> comparator = new Comparator<WNode>() {
		    @Override
		    public int compare(WNode A, WNode B) {
		        return (int) (B.getPopulation() - A.getPopulation());
		    }
		};
		
		List<WNode> nodesSortedByPopulation = wNet.getNodes();
		Collections.sort(nodesSortedByPopulation, comparator); 
		
		for(WNode node : nodesSortedByPopulation) {
			summaryString.add(node.getName() + "");
			summaryString.add(node.getOccupiedCpus() + "");
			summaryString.add(node.getOccupiedRamGB() + "");
			summaryString.add(node.getOccupiedHdGB() + "");
		}
		writeFile (new File (folder , "sortedByPopulation" + 0  + ".txt") , summaryString);
		

		return "Ok";
	}

	@Override
	public String getDescription() {
		return null;
	}

	@Override
	public List<Triple<String, String, String>> getParameters() {
		final List<Triple<String, String, String>> param = new LinkedList<Triple<String, String, String>>();
		param.add(Triple.of("Lmax", "5.0", "Maximum latency value"));
		param.add(Triple.of("K", "5", "Number of shortest paths to evaluate"));
		param.add(Triple.of("numServices", "50", "Number of services"));
		param.add(Triple.of("excelFile", "MHTopology_Nodes_Links_95%.xlsx", "Selection of the Excel spreadsheet"));
		param.add(Triple.of("randomSeed", "1", "Seed of the random number generator"));
		return param;
	}

	public double compare(WNode a, WNode b) {
		return new Double(a.getPopulation()).compareTo(new Double(b.getPopulation()));
	}
	
	public double randomWithRange(Random rng , double min, double max) {
		final double range = (max - min);
		return rng.nextDouble() * range + min;
	}

	public double truncate(double value, int places) {
		return new BigDecimal(value).setScale(places, RoundingMode.DOWN).doubleValue();
	}
	
	public WNode getGreatestNode(WNet wNet) {

		List<WNode> nodesList = wNet.getNodes();
		WNode greatestNode = null;
		double maxPopulation = 0;

		Iterator it = nodesList.iterator();

		while (it.hasNext()) {
			WNode node = (WNode) it.next();

			if (node.getPopulation() > maxPopulation) {
				maxPopulation = node.getPopulation();
				greatestNode = node.getAsNode();
			}
		}

		return greatestNode;
	}

	public WNode getNearestCoreNode(NetPlan netPlan, WNet wNet, WNode node) {
		int minStepsBetweenNodes = Integer.MAX_VALUE;
		WNode nearestCoreNode = null;
		Iterator it = wNet.getNodesConnectedToCore().iterator();
		while (it.hasNext()) {
			WNode coreNode = (WNode) it.next();
			int currentStepsBetweenNodes = GraphUtils
					.getShortestPath(netPlan.getNodes(), netPlan.getLinks(), node.getNe(), coreNode.getNe(), null)
					.size();
			if (minStepsBetweenNodes > currentStepsBetweenNodes) {
				minStepsBetweenNodes = currentStepsBetweenNodes;
				nearestCoreNode = coreNode;
			}
		}

		return nearestCoreNode;
	}

	public Map<Integer, Integer> getBestPathInLatency(WNet wNet, List<List<WFiber>> kFiberLists) {

		int hops = Integer.MAX_VALUE;
		double finalLatency = Double.MAX_VALUE;
		WNode A = null;
		int currentK = -1;
		int finalK = -1;

		Iterator it1 = kFiberLists.iterator();
		while (it1.hasNext()) {
			List<WFiber> fiberList = (List<WFiber>) it1.next();
			Iterator it2 = fiberList.iterator();
			currentK++;
			boolean ok = true;

			while (it2.hasNext() && ok) {

				WFiber fiber = (WFiber) it2.next();
				String endingNode = fiberList.get(fiberList.size() - 1).getB().getName();
				System.out.println(fiber.getA().getName() + " ? " + endingNode);
				if (existsDirectLightpath(wNet, fiber.getA().getName(), endingNode)) {
					System.out.println("Match");
					hops = fiberList.indexOf(fiber) + 1;
					double currentLatency = wNet.getPropagationDelay(fiberList) + 2 * 0.1 * hops;
					if (currentLatency < finalLatency) {
						finalLatency = currentLatency;
						A = fiber.getA().getAsNode();
						ok = false;
						finalK = currentK;
					}
					// break;
				}
			}
		}
		return new HashMap<Integer, Integer>();
	}

	public void setServiceVNFSinEndingNode(WNet wNet, WUserService service, WNode endingNode) {
		List<String> serviceVNFS = service.getListVnfTypesToTraverseUpstream();
		Iterator it = serviceVNFS.iterator();
		while (it.hasNext()) {
			String vnfName = it.next().toString();
			wNet.addVnfInstance(endingNode, vnfName, wNet.getVnfType(vnfName).get());
		}
	}
	
	public boolean existsDirectLightpath(WNet wNet, String node, String endingNode) {
		
		for(WLightpathUnregenerated lp : wNet.getLightpaths()) if(lp.getA().getName().equals(node) && lp.getB().getName().equals(endingNode)) return true;
		return false;
	}

	public Optional<Pair<List<WIpLink>,Double>> getPossibleMinimumLatencyPathWithIdleCapacityUsingCurrentIpNetwork (WNet wNet , WNode origin , WNode destination , double trafficInGbps)
	{
		if (origin.equals(destination)) return Optional.ofNullable(Pair.of(new ArrayList<> () , 0.0));
		final List<WIpLink> ipLinksWithEnoughCapacity = wNet.getIpLinks().stream().filter(e->e.getCurrentCapacityGbps() - e.getCarriedTrafficGbps() >= trafficInGbps).collect(Collectors.toList());
		final Map<WIpLink,Double> mapOfCost = ipLinksWithEnoughCapacity.stream().collect(Collectors.toMap(e->e , e->0.2 + e.getWorstCasePropagationDelayInMs()));
		final List<List<WIpLink>> paths = wNet.getKShortestIpUnicastPath (1 , wNet.getNodes() , ipLinksWithEnoughCapacity , origin , destination , Optional.of(mapOfCost));
		return paths.isEmpty()? Optional.empty() : Optional.ofNullable(Pair.of(paths.get(0) , paths.get(0).stream().mapToDouble(e->mapOfCost.get(e)).sum()));
	}
	
	private static void writeFile (File file , List<String> rows)
	{
		PrintWriter of = null;
		try 
		{ 
			of = new PrintWriter (new FileWriter (file));
			for (String row : rows)
				of.println(row);
			of.close();
		} catch (Exception e) { e.printStackTrace(); if (of != null) of.close(); throw new Net2PlanException ("File error"); }
		
	}
	
}
