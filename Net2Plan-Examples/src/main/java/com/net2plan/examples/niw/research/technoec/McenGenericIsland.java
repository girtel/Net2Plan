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

public class McenGenericIsland
{
	final private SortedSet<WNode> mcensInvolved;
	final private SortedSet<WFiber> mcen2mcenFibers;

	public McenGenericIsland(SortedSet<WNode> mcensInvolved)
	{
		if (mcensInvolved.isEmpty()) throw new Net2PlanException("Empty AMEN set");
		this.mcensInvolved = new TreeSet<>(mcensInvolved);
		final WNet wNet = mcensInvolved.first().getNet();
		this.mcen2mcenFibers = wNet.getFibers().stream().filter(e -> mcensInvolved.contains(e.getA()) && mcensInvolved.contains(e.getB())).collect(Collectors.toCollection(TreeSet::new));
		if (mcen2mcenFibers.stream().anyMatch(e -> !NODETYPE.isMcenBbOrNot(e.getA()) || !NODETYPE.isMcenBbOrNot(e.getB()))) throw new Net2PlanException("Fibers are not inter-AMEN links");

		final SortedSet<Node> npMcensInvolved = mcensInvolved.stream().map(n -> n.getNe()).collect(Collectors.toCollection(TreeSet::new));
		final SortedSet<Link> npMcen2McenLink = mcen2mcenFibers.stream().map(e -> e.getNe()).collect(Collectors.toCollection(TreeSet::new));

		if (GraphUtils.getConnectedComponents(npMcensInvolved, new ArrayList<>(npMcen2McenLink)).size() > 1)
			throw new Net2PlanException("The MCEN-MCEN links are not a single island, BUT found these islans " + GraphUtils.getConnectedComponents(npMcensInvolved, new ArrayList<>(npMcen2McenLink)));
	}

	public SortedSet<WFiber> getMcen2McenFibers()
	{
		return Collections.unmodifiableSortedSet(mcen2mcenFibers);
	}

	public SortedSet<WNode> getMcensBbOrNot()
	{
		return Collections.unmodifiableSortedSet(this.mcensInvolved);
	}

	public SortedSet<WNode> getMcensBb()
	{
		return getMcensBbOrNot().stream().filter(n -> NODETYPE.isMcenBb(n)).collect(Collectors.toCollection(TreeSet::new));
	}

	public SortedSet<WNode> getMcensNotBb()
	{
		return getMcensBbOrNot().stream().filter(n -> NODETYPE.isMcenNotBb(n)).collect(Collectors.toCollection(TreeSet::new));
	}

}
