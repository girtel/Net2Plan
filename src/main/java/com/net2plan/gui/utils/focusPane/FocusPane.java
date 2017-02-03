package com.net2plan.gui.utils.focusPane;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.commons.lang3.StringUtils;

import com.net2plan.gui.utils.IVisualizationCallback;
import com.net2plan.gui.utils.topologyPane.VisualizationState;
import com.net2plan.interfaces.networkDesign.Demand;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.MulticastDemand;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkElement;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.interfaces.networkDesign.Resource;
import com.net2plan.interfaces.networkDesign.Route;
import com.net2plan.interfaces.networkDesign.SharedRiskGroup;
import com.net2plan.internal.Constants.NetworkElementType;
import com.net2plan.libraries.GraphUtils;
import com.net2plan.utils.Constants.RoutingCycleType;
import com.net2plan.utils.Pair;
import com.net2plan.utils.Triple;

import net.miginfocom.swing.MigLayout;

public class FocusPane extends JPanel
{
	private final IVisualizationCallback callback;
	private LinkedList<Pair<NetworkElement,Pair<Demand,Link>>> pastShownInformation;
	private final static int maxSizeNavigationList = 50;
	
	public FocusPane (IVisualizationCallback callback)
	{
		super ();
        
		this.callback = callback;
		this.pastShownInformation = new LinkedList<> ();
		
        this.setVisible(true);
        this.setLayout(new BorderLayout(0,0));
	}

	@Override
	public Dimension getPreferredSize ()
	{
		return new Dimension (600 , 300);
	}
	
	public void updateView () 
	{
		final VisualizationState vs = callback.getVisualizationState();
		final NetworkElementType elementType = vs.getPickedElementType();

		/* Check if remove everything */
		if (elementType == null) { this.removeAll(); this.revalidate(); this.repaint(); return; }

		/* Update the list of past picked elements */
		if (elementType.equals(NetworkElementType.FORWARDING_RULE))
			pastShownInformation.add(Pair.of(null , vs.getPickedForwardingRule()));
		else if (elementType.equals(NetworkElementType.FORWARDING_RULE))
			pastShownInformation.add(Pair.of(vs.getPickedNetworkElement() , null));
		if (pastShownInformation.size() > maxSizeNavigationList) pastShownInformation.removeFirst();

		this.removeAll(); this.revalidate(); this.repaint(); 

		
		/* Here if there is something new to show */
		if (elementType == NetworkElementType.ROUTE)
		{
			final Route r = (Route) vs.getPickedNetworkElement();
			final LinkSequencePanel fig = new LinkSequencePanel(r.getPath() , r.getLayer() , r.getSeqOccupiedCapacitiesIfNotFailing() , "Route " + r.getIndex() , r.getCarriedTraffic());
			this.add(fig , BorderLayout.WEST);
			this.add(createPanelInfo(getRouteInfoTables(r), r) , BorderLayout.CENTER);
		}
		else if (elementType == NetworkElementType.DEMAND)
		{
			final Demand d = (Demand) vs.getPickedNetworkElement();
//			final LinkSequencePanel fig = new LinkSequencePanel(r.getPath() , r.getLayer() , r.getSeqOccupiedCapacitiesIfNotFailing() , "Route " + r.getIndex() , r.getCarriedTraffic());
//			this.add(fig , BorderLayout.WEST);
			this.add(createPanelInfo(getDemandInfoTables(d), d) , BorderLayout.CENTER);
		}
		
		this.revalidate(); 
		this.repaint ();
	}
	
//	private void addNodeInfo (Node n , StringBuffer sb)
//	{
//		final NetPlan np = n.getNetPlan();
//		sb.append("<h1>" + getNodeName(n) + "</h1>");
//		sb.append("<table>");
//		sb.append("<tr><td>Resources</td>");
//		final Set<String> resourceTypes = n.getResources().stream().map(e->e.getType()).collect(Collectors.toSet());
//		sb.append("<td><ul>");
//		sb.append("<li>Num: " + getInternalHL ("resources" , "" + n.getSRGs().size()) + "</li>");
//		if (!resourceTypes.isEmpty())
//			sb.append("<li>Types: " + String.join(", " , resourceTypes) + "</li>");
//		sb.append("</ul></td></tr>");
//		sb.append("<tr><td>SRGs</td>");
//		sb.append("<td><ul>");
//		sb.append("<li>Num: " + getInternalHL ("srgs" , "" + n.getSRGs().size()) + "</li>");
//		sb.append("</ul></td></tr>");
//		for (NetworkLayer layer : np.getNetworkLayers())
//		{
//			sb.append("<th>" + getRichString(layer) + "</th>");
//			final String capUnits = np.getLinkCapacityUnitsName(layer);
//			final String trafUnits = np.getDemandTrafficUnitsName(layer);
//			final int numLinksIn = n.getIncomingLinks(layer).size(); 
//			final int numDemandsIn = n.getIncomingDemands(layer).size(); 
//			final int numMDemandsIn = n.getIncomingMulticastDemands(layer).size();
//			final int numLinksOut = n.getOutgoingLinks(layer).size(); 
//			final int numDemandsOut = n.getOutgoingDemands(layer).size(); 
//			final int numMDemandsOut = n.getOutgoingMulticastDemands(layer).size();
//			final double inLinkCapacity = n.getIncomingLinks(layer).stream().mapToDouble(e->e.getCapacity()).sum(); 
//			final double outLinkCapacity = n.getOutgoingLinks(layer).stream().mapToDouble(e->e.getCapacity()).sum(); 
//			final double inOfferedUnicast = n.getIncomingDemands(layer).stream().mapToDouble(e->e.getOfferedTraffic()).sum(); 
//			final double outOfferedUnicast = n.getOutgoingDemands(layer).stream().mapToDouble(e->e.getOfferedTraffic()).sum(); 
//			final double inOfferedMulticast = n.getIncomingMulticastDemands(layer).stream().mapToDouble(e->e.getOfferedTraffic()).sum(); 
//			final double outOfferedMulticast = n.getOutgoingMulticastDemands(layer).stream().mapToDouble(e->e.getOfferedTraffic()).sum();
//			sb.append("<tr><td>");
//			sb.append("<table border=\"0\">");
//			sb.append("<tr>");
//			sb.append ("<td>Links (in/out)</td>");
//			sb.append ("<td>" + getInternalHL ("linksLayer" + layer.getIndex() , "<p>Num: " + numLinksIn + "/" + numLinksOut + "</p><p>Agg. capacity (" + capUnits + "):" + String.format("%.2f" , inLinkCapacity) + "/" + String.format("%.2f" , outLinkCapacity)) + "</p></td>");
//			sb.append("</tr>");
//			sb.append("<tr>");
//			sb.append ("<td>Demands (in/out)</td>");
//			sb.append ("<td>" + getInternalHL ("demandsLayer" + layer.getIndex() , "<p>Num: " + numDemandsIn + "/" + numDemandsOut + "</p><p>Agg. offered traffic (" + trafUnits + "): " + String.format("%.2f" , inOfferedUnicast) + "/" + String.format("%.2f" , outOfferedUnicast)) + "</p></td>");
//			sb.append("</tr>");
//			sb.append("<tr>");
//			sb.append ("<td>Multicast demands (in/out)</td>");
//			sb.append ("<td>" + getInternalHL ("multicastDemandsLayer" + layer.getIndex() , "<p>Num: " + numMDemandsIn + "/" + numMDemandsOut + "</p><p>Agg. offered traffic (" + trafUnits + "): " + String.format("%.2f" , inOfferedMulticast) + "/" + String.format("%.2f" , outOfferedMulticast)) + "</p></td>");
//			sb.append("</tr>");
//			sb.append("</table>");
//			sb.append("</td></tr>");
//		}
//		sb.append("</table>");
//		for (NetworkLayer layer :  np.getNetworkLayers())
//		{
//			final Set<Link> inLinks = n.getIncomingLinks(layer);
//			final Set<Link> outLinks = n.getOutgoingLinks(layer);
//			final Set<Demand> inDemands = n.getIncomingDemands(layer);
//			final Set<Demand> outDemands = n.getOutgoingDemands(layer);
//			final Set<MulticastDemand> inMDemands = n.getIncomingMulticastDemands(layer);
//			final Set<MulticastDemand> outMDemands = n.getOutgoingMulticastDemands(layer);
//			sb.append("<h2>" + getRichString(layer) + "</h2>");
//			sb.append("<a name='linksLayer'" + layer.getIndex() + "><h3>Links</h3>");
//			sb.append (getEnumerationString(inLinks.stream().map(e->getRichString(e)).collect (Collectors.toList())));
//			sb.append (getEnumerationString(outLinks.stream().map(e->getRichString(e)).collect (Collectors.toList())));
//			sb.append("<a name='demandsLayer'" + layer.getIndex() + "><h3>Unicast traffic demands</h3>");
//			sb.append (getEnumerationString(inDemands.stream().map(e->getRichString(e)).collect (Collectors.toList())));
//			sb.append (getEnumerationString(outDemands.stream().map(e->getRichString(e)).collect (Collectors.toList())));
//			sb.append("<a name='multicastDemandsLayer'" + layer.getIndex() + "><h3>Multicast traffic demands</h3>");
//			sb.append (getEnumerationString(inMDemands.stream().map(e->getRichString(e)).collect (Collectors.toList())));
//			sb.append (getEnumerationString(outMDemands.stream().map(e->getRichString(e)).collect (Collectors.toList())));
//		}
//	}
	private void addLinkInfo (Link e , StringBuffer sb)
	{
		sb.append(e);
	}
	private void addDemandInfo (Demand d , StringBuffer sb)
	{
		sb.append(d);
	}
	private void addMulticastDemandInfo (MulticastDemand d , StringBuffer sb)
	{
		sb.append(d);
	}
	private List<Triple<String,String,String>> getRouteInfoTables (Route r)
	{
		final DecimalFormat df = new DecimalFormat("###.##");
		final NetPlan np = r.getNetPlan();
		final List<Triple<String,String,String>> res = new ArrayList <> ();
		res.add(Triple.of("Route index/id" , "Route " + r.getIndex() + " (id " + r.getId() + ")", "route" + r.getId()));
		res.add(Triple.of("Layer" , "" + getLayerName(r.getLayer()) , "layer" + r.getLayer().getId()));
		res.add(Triple.of("Route demand index/id" , "" + r.getDemand().getIndex() + " (id " + r.getDemand().getId() + ")" , "demand" + r.getDemand().getId()));
		res.add(Triple.of("Demand offered traffic" , "" + df.format(r.getDemand().getOfferedTraffic()) + " " + np.getDemandTrafficUnitsName(r.getLayer()) , ""));
		res.add(Triple.of("Demand carried traffic" , "" + df.format(r.getDemand().getCarriedTraffic()) + " " + np.getDemandTrafficUnitsName(r.getLayer()) , ""));
		res.add(Triple.of("Route carried traffic" , "" + df.format(r.getCarriedTraffic()) + " " + np.getDemandTrafficUnitsName(r.getLayer()), ""));
		res.add(Triple.of("Is up?" , "" + np.isUp(r.getPath()), ""));
		res.add(Triple.of("Is service chain?" , "" + r.getDemand().isServiceChainRequest(), ""));
		res.add(Triple.of("Route length (km)" , "" + df.format(r.getLengthInKm()) + " km", ""));
		res.add(Triple.of("Route length (ms)" , "" + df.format(r.getPropagationDelayInMiliseconds()) + " ms", ""));
		res.add(Triple.of("Is backup route?" , "" + r.isBackupRoute(), ""));
		for (Route pr : r.getRoutesIAmBackup())
			res.add(Triple.of("-- Primary route" , "Route " + pr.getIndex() , "route" + pr.getId()));
		res.add(Triple.of("Has backup routes?" , "" + r.hasBackupRoutes(), ""));
		for (Route br : r.getBackupRoutes())
			res.add(Triple.of("-- Backup route" , "Route " + br.getIndex() , "route" + br.getId()));
		return res;
	}
	private List<Triple<String,String,String>> getDemandInfoTables (Demand d)
	{
		final DecimalFormat df = new DecimalFormat("###.##");
		final NetPlan np = d.getNetPlan();
		final NetworkLayer layer = d.getLayer();
		final List<Triple<String,String,String>> res = new ArrayList <> ();
		final RoutingCycleType cycleType = d.getRoutingCycleType();
		final boolean isLoopless = cycleType.equals(RoutingCycleType.LOOPLESS);
		res.add(Triple.of("Demand index/id" , "Route " + d.getIndex() + " (id " + d.getId() + ")", "demand" + d.getId()));
		res.add(Triple.of("Layer" , "" + getLayerName(layer) , "layer" + layer.getId()));
		res.add(Triple.of("Offered traffic" , "" + df.format(d.getOfferedTraffic()) , ""));
		res.add(Triple.of("Carried traffic" , "" + df.format(d.getCarriedTraffic()) , ""));
		res.add(Triple.of("Lost traffic" , "" + df.format(d.getBlockedTraffic()) , ""));
		res.add(Triple.of("Is service chain?" , "" + d.isServiceChainRequest(), ""));
		if (d.isServiceChainRequest())
			res.add(Triple.of("- Seq. resource types" , StringUtils.join(d.getServiceChainSequenceOfTraversedResourceTypes(),","), ""));
		res.add(Triple.of("Has loops?" , isLoopless? "No" : cycleType.equals(RoutingCycleType.CLOSED_CYCLES)? "Yes (closed loops)" : "Yes (open loops)", ""));
		res.add(Triple.of("Routing type" , layer.isSourceRouting()? "Source routing" : "Hop by hop", ""));
		if (layer.isSourceRouting())
		{
			res.add(Triple.of("Num. routes (total/backup)" , "" + d.getRoutes().size() + "/" + d.getRoutesAreBackup().size(), ""));
			for (Route r : d.getRoutes())
				res.add(Triple.of("Route index/id" , "Route " + r.getIndex() + " (id " + r.getId() + ")" + (r.isBackupRoute()? " [backup]" : ""), "route" + r.getId()));
		}
		res.add(Triple.of("Worst case e2e latency" , df.format(d.getWorstCasePropagationTimeInMs()) + " ms", ""));
		return res;
	}

	private String getNodeName (Node n) { return "Node " + n.getIndex() + " (" + (n.getName().length() == 0? "No name" : n.getName()) + ")"; }
	private String getResourceName (Resource e) { return "Resource " + e.getIndex() + " (" + (e.getName().length() == 0? "No name" : e.getName()) + "). Type: " + e.getType(); }
	private String getLayerName (NetworkLayer e) { return "Layer " + e.getIndex() + " (" + (e.getName().length() == 0? "No name" : e.getName()) + ")"; }

	class LabelWithLink extends JLabel implements MouseListener
	{
		private final String internalLink;
		public LabelWithLink(Pair<String,String> t , boolean bold)
		{
			super (t.getFirst());
			if (bold) this.setFont(new Font(getFont().getName(), Font.BOLD, getFont().getSize()));
			this.internalLink = t.getSecond();
			if (!internalLink.equals("")) this.setForeground(Color.BLUE);
		}
		@Override
		public void mouseClicked(MouseEvent arg0)
		{
			final NetPlan np = callback.getDesign();
			final VisualizationState vs = callback.getVisualizationState();
			
			if (internalLink.equals("")) return;
			if (internalLink.startsWith("demand"))
			{
				final long id = Long.parseLong(internalLink.substring((int) "demand".length()));
				final Demand e = np.getDemandFromId(id);
				vs.pickDemand(e);
				callback.updateVisualizationAfterPick();
			} else if (internalLink.startsWith("route"))
			{
				final long id = Long.parseLong(internalLink.substring((int) "route".length()));
				final Route e = np.getRouteFromId(id);
				vs.pickRoute(e);
				callback.updateVisualizationAfterPick();
			} else if (internalLink.startsWith("node"))
			{
				final long id = Long.parseLong(internalLink.substring((int) "node".length()));
				final Node e = np.getNodeFromId(id);
				vs.pickNode(e);
				callback.updateVisualizationAfterPick();
			} else if (internalLink.startsWith("multicastDemand"))
			{
				final long id = Long.parseLong(internalLink.substring((int) "multicastDemand".length()));
				final MulticastDemand e = np.getMulticastDemandFromId(id);
				vs.pickMulticastDemand(e);
				callback.updateVisualizationAfterPick();
			} else if (internalLink.startsWith("multicastTree"))
			{
				final long id = Long.parseLong(internalLink.substring((int) "multicastTree".length()));
				final MulticastDemand e = np.getMulticastDemandFromId(id);
				vs.pickMulticastDemand(e);
				callback.updateVisualizationAfterPick();
			} else if (internalLink.startsWith("link"))
			{
				final long id = Long.parseLong(internalLink.substring((int) "link".length()));
				final Link e = np.getLinkFromId(id);
				vs.pickLink(e);
				callback.updateVisualizationAfterPick();
			} else if (internalLink.startsWith("forwardingRule"))
			{
				final long demandId = Long.parseLong(internalLink.substring((int) "forwardingRule".length() , internalLink.indexOf(",")));
				final long linkId = Long.parseLong(internalLink.substring(internalLink.indexOf(",") + 1));
				final Pair<Demand,Link> e = Pair.of(np.getDemandFromId(demandId) , np.getLinkFromId(linkId));
				vs.pickForwardingRule(e);
				callback.updateVisualizationAfterPick();
			} else if (internalLink.startsWith("resource"))
			{
				final long id = Long.parseLong(internalLink.substring((int) "resource".length()));
				final Resource e = np.getResourceFromId(id);
				vs.pickResource(e);
				callback.updateVisualizationAfterPick();
			} else if (internalLink.startsWith("resource"))
			{
				final long id = Long.parseLong(internalLink.substring((int) "srg".length()));
				final SharedRiskGroup e = np.getSRGFromId(id);
				vs.pickSRG(e);
				callback.updateVisualizationAfterPick();
			} else if (internalLink.startsWith("layer"))
			{
				final long id = Long.parseLong(internalLink.substring((int) "layer".length()));
				final NetworkLayer e = np.getNetworkLayerFromId(id);
				vs.pickLayer(e);
				callback.updateVisualizationAfterPick();
			} else throw new RuntimeException ();
		}
		@Override
		public void mouseEntered(MouseEvent arg0)
		{
		}
		@Override
		public void mouseExited(MouseEvent arg0)
		{
		}
		@Override
		public void mousePressed(MouseEvent arg0)
		{
		}
		@Override
		public void mouseReleased(MouseEvent arg0)
		{
		}
	}
	
	
	private JPanel createPanelInfo (List<Triple<String,String,String>> infoRows , NetworkElement e)
	{
		final JPanel tablePanel = new JPanel ();
		tablePanel.setLayout(new MigLayout("gap rel 0"));
		tablePanel.add(new LabelWithLink(Pair.of("Information table" ,  ""), true) , "newline, gaptop 0 , gapbottom 0");
		tablePanel.add(new JLabel ("────────────────") , "newline, gaptop 0, gapbottom 2");
		for (Triple<String,String,String> info : infoRows)
		{
			tablePanel.add(new LabelWithLink(Pair.of(info.getFirst() + ":" ,  info.getThird()), true) , "newline, gaptop 0, gapright 5");
			tablePanel.add(new LabelWithLink(Pair.of(info.getSecond() ,  info.getThird()), false) , "gaptop 0");
		}
		tablePanel.add(new LabelWithLink(Pair.of("User-defined attributes" ,  ""), true) , "newline, gaptop 15 , gapbottom 0");
		tablePanel.add(new JLabel ("────────────────────") , "newline, gaptop 0, gapbottom 2");
		if (e.getAttributes().isEmpty())
			tablePanel.add(new LabelWithLink(Pair.of("No attributes defined", "") , false) , "newline, gaptop 0, gapright 5");
		for (Entry<String,String> entry : e.getAttributes().entrySet())
		{
			tablePanel.add(new LabelWithLink(Pair.of(entry.getKey() + ":" ,  ""), true) , "newline, gaptop 0, gapright 5");
			tablePanel.add(new LabelWithLink(Pair.of(entry.getValue() ,  "") , false) , "gaptop 0");
		}
		return tablePanel;
	}
}
