package com.net2plan.examples.niw.research.technoec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.net2plan.examples.niw.research.technoec.TimExcel_NodeListSheet_forConfTimData.NODETYPE;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.libraries.GraphUtils;
import com.net2plan.niw.WFiber;
import com.net2plan.niw.WNet;
import com.net2plan.niw.WNode;

public class AmenGenericIsland
{
	final private SortedSet<WNode> amensInvolved;

	public AmenGenericIsland(SortedSet<WNode> amensInvolved)
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
	}

	public boolean isLinear () 
	{
		for (WNode amen : getAmens ())
		{
			if (amen.getOutgoingFibers().size() > 2) return false;
			if (amen.getIncomingFibers().size() > 2) return false;
		}
		return true;
	}
	
	public SortedSet<WNode> getMcensConnectedViaOutFibers()
	{
		return getOutFibersToMcens().stream().map(e -> e.getB()).collect(Collectors.toCollection(TreeSet::new));
	}

	public SortedSet<WNode> getMcensConnectedViaInFibers()
	{
		return getInFibersFromMcens().stream().map(e -> e.getB()).collect(Collectors.toCollection(TreeSet::new));
	}

	public SortedSet<WFiber> getOutFibersToMcens()
	{
		return amensInvolved.stream().map(e -> e.getOutgoingFibers()).flatMap(ee -> ee.stream()).filter(e -> NODETYPE.isMcenBbOrNot(e.getB())).collect(Collectors.toCollection(TreeSet::new));
	}

	public SortedSet<WFiber> getInFibersFromMcens()
	{
		return amensInvolved.stream().map(e -> e.getIncomingFibers()).flatMap(ee -> ee.stream()).filter(e -> NODETYPE.isMcenBbOrNot(e.getA())).collect(Collectors.toCollection(TreeSet::new));
	}

	public SortedSet<WNode> getAmens()
	{
		return Collections.unmodifiableSortedSet(this.amensInvolved);
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.amensInvolved == null) ? 0 : this.amensInvolved.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		AmenGenericIsland other = (AmenGenericIsland) obj;
		if (this.amensInvolved == null)
		{
			if (other.amensInvolved != null) return false;
		} else if (!this.amensInvolved.equals(other.amensInvolved)) return false;
		return true;
	}

	
}
