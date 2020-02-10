package com.net2plan.examples.niw.algorithms.mhTechnoEconomicsAlgorithm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.net2plan.examples.niw.algorithms.mhTechnoEconomicsAlgorithm.TimExcel_NodeListSheet_forConfTimData.NODETYPE;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.libraries.GraphUtils;
import com.net2plan.niw.WFiber;
import com.net2plan.niw.WNet;
import com.net2plan.niw.WNode;

public class AmenLinearIsland
{
	final private List<WFiber> fibersAb;
	final private List<WFiber> fibersBa;
	final private SortedSet<WNode> amensInvolved;

	public AmenLinearIsland(SortedSet<WNode> amensInvolved)
	{
		if (amensInvolved.isEmpty()) throw new Net2PlanException("Empty AMEN set");
		this.amensInvolved = new TreeSet<>(amensInvolved);
		final WNet wNet = amensInvolved.first().getNet();
		final SortedSet<WFiber> amen2amenLink = wNet.getFibers().stream().filter(e -> amensInvolved.contains(e.getA()) && amensInvolved.contains(e.getB())).collect(Collectors.toCollection(TreeSet::new));
		if (amen2amenLink.stream().anyMatch(e -> !NODETYPE.getByName(e.getA().getType()).isAmen() || !NODETYPE.getByName(e.getB().getType()).isAmen())) throw new Net2PlanException("Fibers are not inter-AMEN links");

		final SortedSet<Node> npAmensInvolved = amensInvolved.stream().map(n -> n.getNe()).collect(Collectors.toCollection(TreeSet::new));
		final SortedSet<Link> npAmen2AmenLink = amen2amenLink.stream().map(e -> e.getNe()).collect(Collectors.toCollection(TreeSet::new));

		if (GraphUtils.getConnectedComponents(npAmensInvolved, new ArrayList<>(npAmen2AmenLink)).size() > 1)
			throw new Net2PlanException("The AMEN-AMEN links are not a single island, BUT found these islans " + GraphUtils.getConnectedComponents(npAmensInvolved, new ArrayList<>(npAmen2AmenLink)));
		for (WNode amen : amensInvolved)
		{
			if (amen.getOutgoingFibers().size() != 2) throw new Net2PlanException("AMEN node " + amen + " does not have degree two. Links involved: " + amen.getOutgoingFibers());
			if (amen.getIncomingFibers().size() != 2) throw new Net2PlanException("AMEN node " + amen + " does not have degree two. Links involved: " + amen.getIncomingFibers());
		}
		final SortedSet<WNode> amensConnectedToMcen = amensInvolved.stream().filter(n -> n.getNeighborNodesViaOutgoingFibers().stream().anyMatch(nn -> NODETYPE.isMcenBbOrNot(nn))).collect(Collectors.toCollection(TreeSet::new));
		if (amensConnectedToMcen.size() != 2) throw new Net2PlanException("There should be two AMENs connected to an MCEN");
		final WNode amenA = amensConnectedToMcen.first();
		final WNode amenB = amensConnectedToMcen.last();
		final List<Link> pathAB = GraphUtils.getShortestPath(npAmensInvolved, npAmen2AmenLink, amenA.getNe(), amenB.getNe(), null);
		final List<Link> pathBA = GraphUtils.getShortestPath(npAmensInvolved, npAmen2AmenLink, amenB.getNe(), amenA.getNe(), null);
		if (pathAB.isEmpty() || pathBA.isEmpty()) throw new Net2PlanException("No path A-B or B-A found");
		this.fibersAb = pathAB.stream().map(e -> new WFiber(e)).collect(Collectors.toList());
		this.fibersBa = pathBA.stream().map(e -> new WFiber(e)).collect(Collectors.toList());
	}

	public WNode getAmenA()
	{
		return fibersAb.get(0).getA();
	}

	public WNode getAmenB()
	{
		return fibersBa.get(0).getA();
	}

	public List<WFiber> getAmen2AmenFibersAB()
	{
		return Collections.unmodifiableList(fibersAb);
	}

	public List<WFiber> getAmen2AmenFibersBA()
	{
		return Collections.unmodifiableList(fibersBa);
	}

	public WFiber getMcenOutFiberInA()
	{
		final List<WFiber> outFibersToMcens = getAmenA().getOutgoingFibers().stream().filter(e -> NODETYPE.isMcenBbOrNot(e.getB())).collect(Collectors.toList());
		if (outFibersToMcens.size() != 1) throw new Net2PlanException("The AMEN has more than one output fiber to an MCEN: " + outFibersToMcens);
		return outFibersToMcens.get(0);
	}

	public WFiber getMcenInFiberInA()
	{
		final List<WFiber> inFibersFromMcens = getAmenA().getIncomingFibers().stream().filter(e -> NODETYPE.isMcenBbOrNot(e.getA())).collect(Collectors.toList());
		if (inFibersFromMcens.size() != 1) throw new Net2PlanException("The AMEN has more than one output fiber to an MCEN: " + inFibersFromMcens);
		return inFibersFromMcens.get(0);
	}

	public WFiber getMcenOutFiberInB()
	{
		final List<WFiber> outFibersToMcens = getAmenB().getOutgoingFibers().stream().filter(e -> NODETYPE.isMcenBbOrNot(e.getB())).collect(Collectors.toList());
		if (outFibersToMcens.size() != 1) throw new Net2PlanException("The AMEN has more than one output fiber to an MCEN: " + outFibersToMcens);
		return outFibersToMcens.get(0);
	}

	public WFiber getMcenInFiberInB()
	{
		final List<WFiber> inFibersFromMcens = getAmenB().getIncomingFibers().stream().filter(e -> NODETYPE.isMcenBbOrNot(e.getA())).collect(Collectors.toList());
		if (inFibersFromMcens.size() != 1) throw new Net2PlanException("The AMEN has more than one output fiber to an MCEN: " + inFibersFromMcens);
		return inFibersFromMcens.get(0);
	}

	public WNode getMcenConnectedInA()
	{
		return getMcenOutFiberInA().getB();
	}

	public WNode getMcenConnectedInB()
	{
		return getMcenOutFiberInB().getB();
	}

	private static <T, TT> void addElement(SortedMap<T, SortedSet<TT>> map, T key, TT value)
	{
		SortedSet<TT> submap = map.get(key);
		if (submap == null)
		{
			submap = new TreeSet<>();
			map.put(key, submap);
		}
		submap.add(value);
	}

}
