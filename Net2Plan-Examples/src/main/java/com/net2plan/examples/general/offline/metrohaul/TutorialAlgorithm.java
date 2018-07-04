package com.net2plan.examples.general.offline.metrohaul;

import com.net2plan.interfaces.networkDesign.IAlgorithm;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.research.niw.networkModel.*;
import com.net2plan.utils.InputParameter;
import com.net2plan.utils.Triple;

import java.io.File;
import java.util.*;

public class TutorialAlgorithm implements IAlgorithm
{
	private InputParameter linerate_Gbps            = new InputParameter("linerate_Gbps", 40.0, "Linerate (in Gbps) per ligthpath", 1.0, true, 1000.0, true);
	private InputParameter slotsPerLightpath        = new InputParameter("slotsPerLightpath", 4, "Number of occupied slots per lightpath", 1, Integer.MAX_VALUE);
	private InputParameter K                        = new InputParameter("K", 3, "Number of candidate shortest paths to compute per LP/SC", 1, 100);
	private InputParameter trafficIntensityTimeSlot = new InputParameter("trafficIntensityTimeSlot", "#select# Morning Afternoon Evening", "Traffic intensity per time slot (as defined in the design/spreadsheet");

	public static void main(String[] args)
	{
		NetPlan netPlan = NetPlan.loadFromFile(new File("Tutorial_excel.n2p"));
		Map<String, String> parameters = new HashMap<>();
		parameters.put("linerate_Gbps", String.valueOf(40.0));
		parameters.put("slotsPerLightpath", String.valueOf(4));
		parameters.put("K", String.valueOf(3));
		parameters.put("trafficIntensityTimeSlot", "Morning");

		TutorialAlgorithm tutorialAlgorithm = new TutorialAlgorithm();
		tutorialAlgorithm.getParameters();
		System.out.println(tutorialAlgorithm.executeAlgorithm(netPlan, parameters, null));
		netPlan.saveToFile(new File("RESULT.n2p"));
	}

	@Override
	public String executeAlgorithm(NetPlan netPlan, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
	{
		//First of all, initialize all parameters
		InputParameter.initializeAllInputParameterFieldsOfObject(this, algorithmParameters);

		final WNet wNet = new WNet(netPlan);
		final OpticalSpectrumManager osm = OpticalSpectrumManager.createFromRegularLps(wNet);
		//Perform here initial checks

		/* Remove any existing lightpath, VNF instances and any existing service chain */
		for(WLightpathRequest lpr : wNet.getLightpathRequests())
			lpr.remove(); // this removes also the lps

		for(WVnfInstance vnf : wNet.getVnfInstances())
			vnf.remove();

		for(WServiceChain sc : wNet.getServiceChains())
			sc.remove();

		/* Add full mesh of lightpaths and IP links */
		for(WNode node1 : wNet.getNodes())
		{
			for(WNode node2 : wNet.getNodes())
			{
				if(node1.equals(node2)) continue;
				List<List<WFiber>> paths = wNet.getKShortestWdmPath(K.getInt(), node1, node2, Optional.empty());
				Optional<SortedSet<Integer>> slotsRange = Optional.empty();
				List<WFiber> selectedPath = null;
				for(List<WFiber> path : paths)
				{
					slotsRange = osm.spectrumAssignment_firstFit(path, slotsPerLightpath.getInt(), Optional.empty());
					if(slotsRange.isPresent())
					{
						selectedPath = path;
						break;
					}
				}
				if(! slotsRange.isPresent()) throw new Net2PlanException("No wavelengths found to allocate a lightpath between " + node1.getName() + " and " + node2.getName());
				WLightpathRequest lpr = wNet.addLightpathRequest(node1, node2, linerate_Gbps.getDouble(), false);
				WLightpathUnregenerated lp = lpr.addLightpathUnregenerated(selectedPath, slotsRange.get(), false);
				osm.allocateOccupation(lp, selectedPath, slotsRange.get());

				// Create IP link and couple it with the LP
				WIpLink ipLink = wNet.addIpLink(node1, node2, linerate_Gbps.getDouble(), false).getFirst();
				lpr.coupleToIpLink(ipLink);
			}
		}

		/* Create one VNF per type in all the nodes */
		for(WNode n : wNet.getNodes())
			for(WVnfType vnfType : wNet.getVnfTypes())
			{
				if(vnfType.isConstrainedToBeInstantiatedOnlyInUserDefinedNodes())
					if(! vnfType.getValidMetroNodesForInstantiation().contains(n.getName()))
						continue;
				wNet.addVnfInstance(n, vnfType.getVnfTypeName(), vnfType);
			}

		/* Deploy service chain */
		for(WServiceChainRequest serviceChainRequest : wNet.getServiceChainRequests())
		{
			final Optional<Double> ti = serviceChainRequest.getTrafficIntensityInfo(trafficIntensityTimeSlot.getString());
			if(! ti.isPresent())
			{
				System.out.println("No traffic intensity defined (" + trafficIntensityTimeSlot.getInt() + ") for SC " + serviceChainRequest.getId());
				continue;
			}
			final SortedSet<WNode> potentialOrigins = serviceChainRequest.getPotentiallyValidOrigins();
			final SortedSet<WNode> potentialDestinations = serviceChainRequest.getPotentiallyValidDestinations();
			final List<String> vnfsToTraverse = serviceChainRequest.getSequenceVnfTypes();

			boolean isSCAllocated = false;
			for(WNode origin : potentialOrigins)
			{
				for(WNode destination : potentialDestinations)
				{
					final List<List<? extends WAbstractNetworkElement>> paths = wNet.getKShortestServiceChainInIpLayer(K.getInt(), origin, destination, vnfsToTraverse, Optional.empty(), Optional.empty());
					if(paths.isEmpty()) continue;
					isSCAllocated = true;
					WServiceChain serviceChain = serviceChainRequest.addServiceChain(paths.get(0), ti.get());
				}
				if(isSCAllocated) break;
			}
		}

		/* Dimension the VNF instances, consuming the resources CPU, HD, RAM */
		for(WVnfInstance vnf : wNet.getVnfInstances())
			vnf.scaleVnfCapacityAndConsumptionToBaseInstanceMultiple();

		/* Remove those VNF instances that have zero capacity */
		for(WVnfInstance vnf : wNet.getVnfInstances())
			if(vnf.getOccupiedCapacityInGbps() == 0)
				vnf.remove();

		// Extra: consider service chain injected traffic as the worst case among the morning/afternoon/night

		// Extra: make each lightpath be 1+1 protected with a link disjoint lightpath if possible [e.g. choose the main path as now, and the backup choose it as the one in the k-ranking with less common links, and among them, the one of lower length]

		// Extra: some nodes may have consumed more CPUs/RAM/HD than they have... what to do with them

		// Extra: some IP links may have more traffic than they can carry... what to do with them

		return "Ok";
	}

	@Override
	public String getDescription()
	{
		return null;
	}

	@Override
	public List<Triple<String, String, String>> getParameters()
	{
		return InputParameter.getInformationAllInputParameterFieldsOfObject(this);
	}
}
