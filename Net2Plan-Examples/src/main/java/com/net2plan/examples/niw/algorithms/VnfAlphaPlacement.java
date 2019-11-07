package com.net2plan.examples.niw.algorithms;

import com.google.common.collect.Lists;
import com.net2plan.interfaces.networkDesign.IAlgorithm;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.Resource;
import com.net2plan.libraries.GraphUtils;
import com.net2plan.niw.*;
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

public class VnfAlphaPlacement implements IAlgorithm {

	@Override
	public String executeAlgorithm(NetPlan netPlan, Map<String, String> algorithmParameters,Map<String, String> net2planParameters) {

		final Double Lmax = Double.parseDouble(algorithmParameters.get("Lmax"));
		final File folder = new File ("Results");
		if (folder.exists() && folder.isFile()) throw new Net2PlanException ("The folder is a file");
		if (!folder.exists()) folder.mkdirs();

			
			WNet wNet = runAlgorithm(netPlan, algorithmParameters);

			/* Dimension the VNF instances, consuming the resources CPU, HD, RAM */
			/*
			 * for(WVnfInstance vnf : wNet.getVnfInstances())
			 * vnf.scaleVnfCapacityAndConsumptionToBaseInstanceMultiple();
			 */

			// HDD 	0.0425€/GB	(85€ 	-> 		2000GB)
			// CPU	35€/Core	(70€	->		2 Cores)
			// RAM 	12.5€/GB	(50€	->		4GB)

			
			//SORTED BY NAME
			double finalCost = 0;
			
			final List<String> namesString =  new ArrayList<>();
			final List<String> summaryString = new ArrayList<> ();
			
			for(WNode node : wNet.getNodes()) {
				namesString.add("'"+node.getName()+"' ");
				finalCost = Math.sqrt((85/2000)*node.getOccupiedHdGB()+(70/2)*node.getOccupiedCpus()+(50/4)*node.getOccupiedRamGB());
				summaryString.add(Double.toString(finalCost));
			}
	
			writeFileInOneLine (new File (folder , "names_sortedByName"+ ".txt") , namesString);
			writeFile (new File (folder , "sortedByName" + Lmax  + ".txt") , summaryString);

			
			//SORTED BY POPULATION
			namesString.clear();
			summaryString.clear();
			finalCost = 0;

			Comparator<WNode> comparator = new Comparator<WNode>() {
			    @Override
			    public int compare(WNode A, WNode B) {
			        return (int) (B.getAllVnfInstances().size() - A.getAllVnfInstances().size());
			    }
			};
			
			List<WNode> nodesSortedByPopulation = wNet.getNodes();
			Collections.sort(nodesSortedByPopulation, comparator); 
			
			for(WNode node : nodesSortedByPopulation) {
				namesString.add("'"+node.getName()+"' ");
				finalCost = Math.sqrt((85/2000)*node.getOccupiedHdGB()+(70/2)*node.getOccupiedCpus()+(50/4)*node.getOccupiedRamGB());
				summaryString.add(Double.toString(finalCost));
			}
			
			writeFileInOneLine (new File (folder , "names_sortedByNumberOfVNFs"+ ".txt") , namesString);
			writeFile (new File (folder , "sortedByNumberOfVNFs" + Lmax  + ".txt") , summaryString);

		return "Ok";
	}
	
	public WNet runAlgorithm (NetPlan netPlan, Map<String, String> algorithmParameters){
		final long seed = Long.parseLong(algorithmParameters.get("randomSeed"));
		final Random rng = new Random (seed);
		final Double Lmax = Double.parseDouble(algorithmParameters.get("Lmax"));
		final int K = Integer.parseInt(algorithmParameters.get("K"));
		final int numServices = Integer.parseInt(algorithmParameters.get("numServices"));
		final String excelFile = algorithmParameters.get("excelFile");
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
				WVnfType vnfType = new WVnfType("VNF"+j+"_Service"+i, 	//Name
						100000000, 				//Capacity_Gbps
						2, 						//CPU
						4, 						//RAM
						20, 					//HD
						1.0 , 					//Processing Time
						Optional.empty(), 		//Instantiation nodes
						"");					//Arbitrary Params
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
					
					List<List<WFiber>> cpl = wNet.getKShortestWdmPath(K, origin, destination,Optional.empty());
					if(cpl.get(0).size() == 1) { // adjacent nodes
						List<WFiber> uniqueFiber = cpl.get(0);
						//if (wNet.getLightpaths().stream().filter(l->l.getA() == destination && l.getB() == origin).count() > 0) continue;
						WLightpathRequest lpr = wNet.addLightpathRequest(origin, destination, linerate_Gbps,false);
						Optional<SortedSet<Integer>> wl = osm.spectrumAssignment_firstFit(uniqueFiber,slotsPerLightpath, Optional.empty());
						if (wl.isPresent()) {
							WLightpath lp = lpr.addLightpathUnregenerated(uniqueFiber, wl.get(), false);
							osm.allocateOccupation(lp, uniqueFiber, wl.get());
							Pair<WIpLink,WIpLink> ipLink = wNet.addIpLinkBidirectional(origin, destination, linerate_Gbps);
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
			//System.out.println(totalTrafficInGbpsOfThisServiceInThisNodeGbps);
			WServiceChainRequest scr = wNet.addServiceChainRequest(node, true, service);
			scr.setCurrentOfferedTrafficInGbps(totalTrafficInGbpsOfThisServiceInThisNodeGbps);
		}				

		/* Algorithm starts here serving service chain requests */
		final List<WServiceChainRequest> orderedServiceChainRequestsHigherToLowerTraffic = wNet.getServiceChainRequests().stream().
				sorted((s1,s2)->Double.compare(s2.getCurrentOfferedTrafficInGbps(), s1.getCurrentOfferedTrafficInGbps())  ).
				collect(Collectors.toList());
		
		int counter = 0;
		
		/* Iterate each ServiceChainRequest */
		for (WServiceChainRequest scr : orderedServiceChainRequestsHigherToLowerTraffic)
		{
		
			final WNode node = scr.getPotentiallyValidOrigins().first();
			final double totalTrafficInGbpsOfThisServiceInThisNodeGbps = scr.getCurrentOfferedTrafficInGbps();
			//final WUserService service = wNet.getUserServicesInfo().get(scr.getli);
			//double serviceLatency_ms = service.getListMaxLatencyFromInitialToVnfStart_ms_upstream().get(0);
			double serviceLatency_ms = scr.getListMaxLatencyFromOriginToVnStartAndToEndNode_ms().get(0);
			final WNode coreNode = getNearestCoreNode(netPlan, wNet, node).getAsNode();
			
			/*######################################### Some prints ############################################*/
			System.out.println("---------------------------------------------------------------");
			System.out.println("Iteration number "+ ++counter + " out of " +orderedServiceChainRequestsHigherToLowerTraffic.size());
			System.out.println("totalTrafficInGbpsOfThisServiceInThisNodeGbps: "+totalTrafficInGbpsOfThisServiceInThisNodeGbps);
			//System.out.println("Iteration " + node.getName() + " & " + service.getUserServiceUniqueId());
			System.out.println("Iteration " + node.getName() + " & " + scr.getId());
			//System.out.println(service.getUserServiceUniqueId() + " latency: " + truncate(serviceLatency_ms, 2));
			System.out.println(scr.getId() + " latency: " + truncate(serviceLatency_ms, 2));
			System.out.println("Core Node selected: " + coreNode.getName());
			System.out.println(node.getName() + " -> " + coreNode.getName());
			/*##################################################################################################*/

			
			// first, if the node is a CoreNode the VNFS must be instantiated on it. It's the best solution possible.
			// #### Case 1: The node is Core Node ####
			if (coreNode.equals(node)) {
				System.out.println("Case 1: The node is a Core Node!!");
				System.out.println("Result: " + scr.getId() + " resources allocated in "
						+ node.getName());
				
				setServiceVNFSinEndingNode(wNet, scr, node);

				final List<String> vnfsToTraverse = scr.getSequenceVnfTypes();
				final List<List<WAbstractNetworkElement>> paths = wNet.getKShortestServiceChainInIpLayer(
						K, node, node, vnfsToTraverse, Optional.empty(), Optional.empty());
				System.out.println("#1 Allocating resources in the own node");
				System.out.println(paths.get(0));
				scr.addServiceChain(paths.get(0),totalTrafficInGbpsOfThisServiceInThisNodeGbps);

				// else, we need to discover the option that meets the requirements.
			} else 
			{

				final Map<WFiber , Double> latencyInMsAssumingOeoInAllHops = wNet.getFibers().stream().collect(Collectors.toMap(e->e, e->0.2 + e.getLengthInKm () / 200.0 ));
				final List<List<WFiber>> kFiberLists = wNet.getKShortestWdmPath(K, node, coreNode, Optional.of(latencyInMsAssumingOeoInAllHops));
				final List<WFiber> firstFiberLinks = kFiberLists.get(0);
				
				/* Ending node must be equal to core node initially */
				WNode endingNode = firstFiberLinks.get(firstFiberLinks.size() - 1).getB();
				if (!endingNode.equals(coreNode)) throw new Net2PlanException("Error. These objects must be equal intially.");
				
				
				while (firstFiberLinks.size() != 0) /* We decrease the list.size() in each iteration moving one step the endingNode */
				{
					/* Worst latency case in this fiber path*/
					final double propagationDelay = wNet.getPropagationDelay(firstFiberLinks);
					final double latency = propagationDelay + 2 * 0.1 * firstFiberLinks.size();

					/*######################################### Some prints ############################################*/
					System.out.print(node.getName());
					for(WFiber fiber : firstFiberLinks) {
						System.out.print(" #" + truncate(fiber.getLengthInKm(), 2) + "km,("
								+ truncate(fiber.getNe().getPropagationDelayInMs(), 2) + "ms)# "
								+ fiber.getB().getName());
					}

					System.out.println();
					System.out.println("Number of links to traverse (hops): " + firstFiberLinks.size());
					System.out.println("Propagation delay is: " + truncate(propagationDelay, 2));
					System.out.println("Total latency between " + node.getName() + " and " + endingNode.getName()
							+ " = " + truncate(latency, 2));
					System.out.println(
							"Latency with direct lightpath is: " + truncate(propagationDelay + 2 * 0.1, 2));
					System.out.println("Service latency is: " + truncate(serviceLatency_ms, 2));
					/*##################################################################################################*/
					
					List<WAbstractNetworkElement> finalIpPath = new ArrayList<WAbstractNetworkElement>();
					
					// Case 2: Through each fiber link
					if (latency < serviceLatency_ms) {

						System.out.println("Case 2: The latency is lower than service latency, so this is the ending node to allocate resources: "+ endingNode.getName());
						System.out.println("Result: " + scr.getId()+ " resources allocated in " + endingNode.getName());
						
						/* Adding lightpath per each linkFiber */
						for (WFiber fiber : firstFiberLinks) {
							if (!existsDirectLightpath(wNet, fiber.getA().getName(), fiber.getB().getName())) {
								WLightpathRequest lpr = wNet.addLightpathRequest(fiber.getA(), fiber.getB(),
										linerate_Gbps, false);
								Optional<SortedSet<Integer>> wl = osm.spectrumAssignment_firstFit(Arrays.asList(fiber),
										slotsPerLightpath, Optional.empty());
								if (wl.isPresent()) {
									WLightpath lp = lpr.addLightpathUnregenerated(Arrays.asList(fiber), wl.get(),false);
									osm.allocateOccupation(lp, Arrays.asList(fiber), wl.get());
									Pair<WIpLink, WIpLink> ipLink = wNet.addIpLinkBidirectional(fiber.getA(), fiber.getB(),
											linerate_Gbps);
									lpr.coupleToIpLink(ipLink.getFirst());
									//finalIpPath.add(ipLink.getFirst());
								}else
									throw new Net2PlanException("Error. No spectrum available.");
							}
							
							List <WIpLink> ipLinksReady = wNet.getIpLinks().stream().filter(e->e.getCurrentCapacityGbps() - e.getCarriedTrafficGbps() >= totalTrafficInGbpsOfThisServiceInThisNodeGbps).collect(Collectors.toList());
							List<WIpLink> ipList = ipLinksReady.stream().filter(e-> e.getA().equals(fiber.getA()) && e.getB().equals(fiber.getB())).collect(Collectors.toList());
							if(ipList.size() != 0) finalIpPath.add(ipList.get(0)); else finalIpPath.add(null);	
						}
						
							prepareAndAddServiceChain(wNet, scr, finalIpPath, endingNode);
							break;

					}
					
					// Case 3: Reusing direct lightpaths created in previous iterations
					List<WLightpath> lpList = wNet.getLightpaths();
					boolean done = false;
					for (WLightpath lp : lpList) {

						finalIpPath.clear();
						finalIpPath.add(null);// Two entries in Case 3. Here we use .set() instead .add()
						finalIpPath.add(null);//

						if(lp.getB().getName().equals(endingNode.getName()) && !existsDirectLightpath(wNet, node.getName(), lp.getB().getName())) {
							
							WNode A = lp.getA();
							List<WFiber> node2A = wNet.getKShortestWdmPath(K, node, A, Optional.empty()).get(0);
							List<WFiber> A2endingNode = wNet.getKShortestWdmPath(K, A, endingNode, Optional.empty()).get(0);
							node2A.addAll(A2endingNode);//join both List<WFiber>
							double candidateLatency_ms = 2*0.2 + wNet.getPropagationDelay(node2A); //compute latency
							System.out.println("candidateLatency with this path: "+candidateLatency_ms);

							if (candidateLatency_ms < serviceLatency_ms) {

								List<WIpLink> ipLinksReady = wNet.getIpLinks().stream().filter(e -> e.getCurrentCapacityGbps()- e.getCarriedTrafficGbps() >= totalTrafficInGbpsOfThisServiceInThisNodeGbps).collect(Collectors.toList());
								List<WIpLink> ipList = ipLinksReady.stream().filter(e -> e.getA().equals(lp.getA()) && e.getB().equals(lp.getB())).collect(Collectors.toList());
								if (ipList.size() != 0) finalIpPath.set(1, ipList.get(0));else finalIpPath.set(1, null);

								if (!existsDirectLightpath(wNet, node.getName(), lp.getA().getName()) && finalIpPath.get(1) != null) {

									
									List<List<WFiber>> cpl = wNet.getKShortestWdmPath(K, node, A, Optional.empty());
									List<WFiber> uniqueFiber = cpl.get(0);
									WLightpathRequest lpr = wNet.addLightpathRequest(node, A, linerate_Gbps, false);
									Optional<SortedSet<Integer>> wl = osm.spectrumAssignment_firstFit(uniqueFiber,
											slotsPerLightpath, Optional.empty());

									if (wl.isPresent()) {
										System.out.println("Creating direct ligthpath between "+node.getName()+" and "+A.getName());
										WLightpath lpU = lpr.addLightpathUnregenerated(uniqueFiber,
												wl.get(), false);
										osm.allocateOccupation(lpU, uniqueFiber, wl.get());
										Pair<WIpLink, WIpLink> ipLink = wNet.addIpLinkBidirectional(node, A, linerate_Gbps);
										lpr.coupleToIpLink(ipLink.getFirst());
										finalIpPath.set(0, ipLink.getFirst());
										System.out.println("Content in finalIpPath(0)"+ finalIpPath.get(0));
										System.out.println("Content in finalIpPath(1)"+ finalIpPath.get(1));
										
										prepareAndAddServiceChain(wNet, scr, finalIpPath, endingNode);
										done = true;
										break;
										
									}else
										throw new Net2PlanException("Error. No spectrum available.");
								}else {
									
									ipLinksReady = wNet.getIpLinks().stream().filter(e -> e.getCurrentCapacityGbps()- e.getCarriedTrafficGbps() >= totalTrafficInGbpsOfThisServiceInThisNodeGbps).collect(Collectors.toList());
									ipList = ipLinksReady.stream().filter(e -> e.getA().equals(node) && e.getB().equals(lp.getA())).collect(Collectors.toList());
									if (ipList.size() != 0) finalIpPath.set(0, ipList.get(0));else finalIpPath.set(0, null);
									System.out.println("Content in finalIpPath(0)"+ finalIpPath.get(0));
									System.out.println("Content in finalIpPath(1)"+ finalIpPath.get(1));
									
										prepareAndAddServiceChain(wNet, scr, finalIpPath, endingNode);
										done = true;
										break;

								}
							}
						}

					}if(done) break;

					// Case 4: Through direct lightpath
					if ((propagationDelay + 2 * 0.1) < serviceLatency_ms) {// direct lightpath
						
						finalIpPath.clear();
						finalIpPath.add(null);
						
						System.out.println("Case 4: Checking if there are direct lightpaths created previously");
						final WNode finalEndingNode = endingNode.getAsNode();
						
						List<WIpLink> ipLinksReady = wNet.getIpLinks().stream().filter(e -> e.getCurrentCapacityGbps()- e.getCarriedTrafficGbps() >= totalTrafficInGbpsOfThisServiceInThisNodeGbps).collect(Collectors.toList());
						List<WIpLink> ipList = ipLinksReady.stream().filter(e -> e.getA().equals(node) && e.getB().equals(finalEndingNode)).collect(Collectors.toList());
						if (ipList.size() != 0) finalIpPath.set(0, ipList.get(0));else finalIpPath.set(0, null);

						if (!existsDirectLightpath(wNet, node.getName(), endingNode.getName()) || finalIpPath.contains(null)) {

							List<List<WFiber>> cpl = wNet.getKShortestWdmPath(K, node, endingNode, Optional.empty());
							List<WFiber> uniqueFiber = cpl.get(0);
							WLightpathRequest lpr = wNet.addLightpathRequest(node, endingNode, linerate_Gbps, false);
							Optional<SortedSet<Integer>> wl = osm.spectrumAssignment_firstFit(uniqueFiber,
									slotsPerLightpath, Optional.empty());

							if (wl.isPresent()) {
								System.out.println("Creating direct ligthpath between "+node.getName()+" and "+endingNode.getName());
								WLightpath lpU = lpr.addLightpathUnregenerated(uniqueFiber,
										wl.get(), false);
								osm.allocateOccupation(lpU, uniqueFiber, wl.get());
								Pair<WIpLink, WIpLink> ipLink = wNet.addIpLinkBidirectional(node, endingNode, linerate_Gbps);
								lpr.coupleToIpLink(ipLink.getFirst());
								finalIpPath.set(0, ipLink.getFirst());
								System.out.println("Content in finalIpPath(0)"+ finalIpPath.get(0));
								
								prepareAndAddServiceChain(wNet, scr, finalIpPath, endingNode);
								break;
								
							}else
								throw new Net2PlanException("Error. No spectrum available.");
							
						}else {
								prepareAndAddServiceChain(wNet, scr, finalIpPath, endingNode);
								break;
						}

					} else {
						// NOT FINISH?... move ending node one step and repeat the loop.
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
					System.out.println("Result: " + scr.getId()
							+ " resources are allocated in " + node.getName());
					setServiceVNFSinEndingNode(wNet, scr, node);

					final List<String> vnfsToTraverse = scr.getSequenceVnfTypes();
					final List<List<WAbstractNetworkElement>> paths = wNet.getKShortestServiceChainInIpLayer(
							K, node, node, vnfsToTraverse, Optional.empty(), Optional.empty());
					System.out.println(paths.get(0));
					scr.addServiceChain(paths.get(0),totalTrafficInGbpsOfThisServiceInThisNodeGbps);
				}
			} // else
		}

		/* ############################# */
		System.out.println("############################################################################");
		System.out.println("LIGHTPATHS");
		
		List<WLightpath> lpList = wNet.getLightpaths();
		System.out.println("Number of lightpaths: "+lpList.size());
		for(WLightpath lp : lpList) System.out.println(lp.getA().getName()+" -> "+lp.getB().getName());

		return wNet;
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

		for (WNode node : nodesList) {

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
		for (WNode coreNode : wNet.getNodesConnectedToCore()) {
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

	public void setServiceVNFSinEndingNode(WNet wNet, WServiceChainRequest service, WNode endingNode) {
		//List<String> serviceVNFS = service.getListVnfTypesToTraverseUpstream();
		List<String> serviceVNFS = service.getSequenceVnfTypes();
		for(String vnfName : serviceVNFS) {
			wNet.addVnfInstance(endingNode, vnfName, wNet.getVnfType(vnfName).get());
		}
	}
	
	public boolean existsDirectLightpath(WNet wNet, String node, String endingNode) {
		
		for(WLightpath lp : wNet.getLightpaths()) if(lp.getA().getName().equals(node) && lp.getB().getName().equals(endingNode)) return true;
		return false;
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
	
	private static void writeFileInOneLine (File file , List<String> rows)
	{
		PrintWriter of = null;
		try 
		{ 
			of = new PrintWriter (new FileWriter (file));
			for (String row : rows)
				of.print(row);
			of.close();
		} catch (Exception e) { e.printStackTrace(); if (of != null) of.close(); throw new Net2PlanException ("File error"); }
		
	}
	
	private static void prepareAndAddServiceChain(WNet wNet, WServiceChainRequest scr,
			List<WAbstractNetworkElement> finalIpPath, WNode endingNode) {

		double totalTrafficInGbpsOfThisServiceInThisNodeGbps = scr.getCurrentOfferedTrafficInGbps();
		//final WUserService service = wNet.getUserServicesInfo().get(scr.getUserServiceName());
		if (!finalIpPath.contains(null)) {
			//List<String> serviceVNFS = service.getListVnfTypesToTraverseUpstream();
			List<String> serviceVNFS = scr.getSequenceVnfTypes();
			for (String vnfName : serviceVNFS) {
				WVnfInstance vnf = wNet.addVnfInstance(endingNode, vnfName, wNet.getVnfType(vnfName).get());
				finalIpPath.add(vnf);
			}

			System.out.println("#4 Using the custom way...");
			System.out.println(finalIpPath);
			scr.addServiceChain(finalIpPath, totalTrafficInGbpsOfThisServiceInThisNodeGbps);
		}
	}
	
}
