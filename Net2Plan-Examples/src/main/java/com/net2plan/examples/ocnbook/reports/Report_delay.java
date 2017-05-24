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

package com.net2plan.examples.ocnbook.reports;

import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.utils.Constants.RoutingType;
import com.net2plan.utils.InputParameter;
import com.net2plan.utils.Triple;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;

/**
 * This report receives as an input a network design, where the network is assumed to be based on packet switching, and estimates the packet delay 
 * of the different flows.
 * 
 * @net2plan.keywords 
 * @net2plan.ocnbooksections Section 3.2
 * @net2plan.inputParameters 
 * @author Pablo Pavon-Marino
 */
public class Report_delay implements IReport
{
	private InputParameter hurstParameter = new InputParameter ("hurstParameter" , (double) 0.5 , "Hurst parameter (H) of self-similar sources [0.5, 1) (M/M/1 model when H = 0.5)" , 0.5 , true, 1, false);
	private InputParameter averagePacketLength_bits = new InputParameter ("averagePacketLength_bits" , (double) 500*8 , "Average packet length in bits" , 0 , false , Double.MAX_VALUE, true);
	private InputParameter linkCapacityUnits_bps = new InputParameter ("linkCapacityUnits_bps" , (double) 1e6 , "Units in bps in which the capacity of the links is measured" , 0 , false , Double.MAX_VALUE, true);

	@Override
	public String executeReport(NetPlan netPlan, Map<String, String> reportParameters, Map<String, String> net2planParameters)
	{
		/* Initialize all InputParameter objects defined in this object (this uses Java reflection) */
		InputParameter.initializeAllInputParameterFieldsOfObject(this, reportParameters);

		if (netPlan.getRoutingType() != RoutingType.SOURCE_ROUTING)
		{
			return "<html><body><p>No delay information available. The routing must be of the form SOURCE ROUTING.</p></body></html>";
		}
		
		if (!netPlan.hasRoutes())
		{
			return "<html><body><p>No delay information available. Routing is not defined.</p></body></html>";
		}

		final int E = netPlan.getNumberOfLinks();
		final int D = netPlan.getNumberOfDemands();
		final int R = netPlan.getNumberOfRoutes();

		StringBuilder out = new StringBuilder();
		DecimalFormat df_6 = new DecimalFormat("#.######");
		out.append("<html><body>");
		out.append("<head><title>Packet delay report</title></head>");
		out.append("<h1>Introduction</h1>");
		out.append("<p>This report receives as an input a network design, where the network is assumed to be based on packet switching, and estimates the packet delay of the different flows.</p>");
		out.append("<h1>Motivation</h1>");
		out.append("<p>In packet-switched networks, traffic sources split data into smaller pieces called packets, and transmit them attached to a header with control information. Per each packet received, packet-switching nodes read its header and take appropriate forwarding decisions, according to the routing plan configured. In real networks, traffic is highly unpredictable and thus modelled as random processes. When we say that a traffic source <em>d</em> generates <em>h_d</em> traffic units, we refer to a time average. Instantaneous traffic generated oscillates very fast and uncontrollably from peak intervals (generating more traffic than the average) to valley intervals (generating less traffic than the average).</p>");
		out.append("<p>The traffic carried by a link is the aggregation (multiplexing) of the traffic from all the sources routed through this link. At a given moment, it is very likely that some of the sources aggregated are in a peak, while others are in a valley. They compensate each other. Thus, you do not need a link capacity equal to the sum of the peak traffics of all the sources, but a capacity between the sum of the averages and the sum of the peaks. We say that multiplexing sources provides an <em>statistical multiplexing gain</em>.</p>");
		out.append("<p>Statistical multiplexing gain is the powering fact propeling packet switching. Since it is very common that sources have peaks of traffic several times higher than their average (e.g. 2 or 3 times). However, at unpredictable moments in time, peak traffic intervals coincide and link capacities are not enough to forward traffic. Then, nodes store packets in queues, so they are delayed until they can be transmitted (this delay is known as queuing delay). If this situation remains, queues are filled provoking packet drops. We say that the link is congested or saturated. Note that if the link capacity is below the sum of the average traffic generated by the traversing sources, a large amount of packet drops will always occur, whatever buffer you allocate in the nodes. Network designs must enforce always that link capacities are not below the sum of the averages of the traffics to be carried.</p>");
		out.append("<p>Network design tries to model statistically delays and drops in order to minimize their effects. Traffic models capture not only the average of each traffic source, but also a measure of its burstiness. Intuitively, it is clear that the more steep and long that the peak-valley intervals are (or the more bursty the traffic is), the higher the queuing delay. This is because, during low load intervals, the link is underutilized with negligible delays, but during peak traffic intervals packets need to be buffered and can suffer large queuing delays or drops. Naturally, a zero queuing delay occurs when the traffic is perfectly constant (not random).</p>");
		out.append("<p>This report estimates the link, path and demand delays using a self-similar model, where the average queueing delay in a link depends on the link capacity, its average traffic, and a so-called <em>Hurst parameter</em> measuring its degree of self-similarity.</p>");
		out.append("<p>For more information:</p>");
		out.append("<ul><li><a href=\"http://www.wiley.com/WileyCDA/WileyTitle/productCd-1119013356.html\">Pablo Pav&oacute;n Mari&ntilde;o, \"Optimization of computer networks. Modeling and algorithms. A hands-on approach\", Wiley 2016\"</a></li></ul>");

		// Link metrics
		double[] T_e_prop = new double [E];
		double[] T_e_tx = new double [E];
		double[] T_e_buf = new double [E];
		double[] T_e = new double [E];
		for (Link e : netPlan.getLinks())
		{
			final double rho_e = e.getUtilization();
			T_e_prop [e.getIndex()] = e.getPropagationDelayInMs();
			T_e_tx [e.getIndex()] = 1000 * averagePacketLength_bits.getDouble() / (e.getCapacity() * linkCapacityUnits_bps.getDouble()); 
			T_e_buf [e.getIndex()] = 1000 * averagePacketLength_bits.getDouble() / (e.getCapacity() * linkCapacityUnits_bps.getDouble()) * Math.pow(rho_e, 1/(2*(1-hurstParameter.getDouble()))) / Math.pow(1-rho_e , hurstParameter.getDouble()/(1 - hurstParameter.getDouble())); 
			T_e [e.getIndex()] = T_e_prop [e.getIndex()] + T_e_tx [e.getIndex()] + T_e_buf [e.getIndex()];  
		}
		
		// Route metrics
		double[] T_r_prop = new double [R];
		double[] T_r_tx = new double [R];
		double[] T_r_buf = new double [R];
		double[] T_r = new double [R];
		for (Route r : netPlan.getRoutes())
			for (Link e : r.getSeqLinks())
			{
				T_r_prop [r.getIndex()] += T_e_prop [e.getIndex()];
				T_r_tx [r.getIndex()] += T_e_tx [e.getIndex()];
				T_r_buf [r.getIndex()] += T_e_buf [e.getIndex()];
				T_r [r.getIndex()] += T_e [e.getIndex()];
			}

		// Demand metrics
		double[] T_d_prop = new double [D];
		double[] T_d_tx = new double [D];
		double[] T_d_buf = new double [D];
		double[] T_d = new double [D];
		for (Demand d : netPlan.getDemands())
			for (Route r : d.getRoutes())
			{
				final double weightFactor = d.getCarriedTraffic() == 0? 0 : r.getCarriedTraffic() / d.getCarriedTraffic();
				T_d_prop [d.getIndex()] += weightFactor * T_r_prop [r.getIndex()];
				T_d_tx [d.getIndex()] += weightFactor * T_r_tx [r.getIndex()];
				T_d_buf [d.getIndex()] += weightFactor * T_r_buf [r.getIndex()];
				T_d [d.getIndex()] += weightFactor * T_r [r.getIndex()];
			}

		// Network metrics
		final double sumCarriedTraffic = netPlan.getVectorRouteCarriedTraffic().zSum();
		double avNetDelay_T = 0; 
		double avNetDelay_T_prop = 0; 
		for (Route r : netPlan.getRoutes())
		{
			avNetDelay_T += r.getCarriedTraffic() * T_r [r.getIndex()];
			avNetDelay_T_prop += r.getCarriedTraffic() * T_r_prop [r.getIndex()];
		}
		avNetDelay_T /= sumCarriedTraffic;
		avNetDelay_T_prop /= sumCarriedTraffic;
		
		out.append("<h2>Link delay information</h2>");
		out.append("<table border='1'>");
		out.append("<tr><th><b>Link index</a></b></th><th><b>Origin node</b></th><th><b>Destination node</b></th><th><b>Propagation delay (ms)</b></th><th><b>Transmission delay (ms)</b></th><th><b>Queuing delay (ms)</b></th><th><b>Total delay (ms)</b></th><th><b>Attributes</a></b></th>");
		for (Link e : netPlan.getLinks())
		{
			final int linkId = e.getIndex();
			out.append(String.format("<tr><td>%d</td><td>%d (%s)</td><td>%d (%s)</td><td>%.3g</td><td>%.3g</td><td>%.3g</td><td>%.3g</td><td>%s</td></tr>", e.getIndex(), e.getOriginNode().getIndex() , e.getOriginNode().getName(), e.getDestinationNode().getIndex(),e.getDestinationNode().getName(), T_e_prop[linkId], T_e_tx[linkId], T_e_buf[linkId], T_e[linkId], e.getAttributes()));
		}
		out.append("</table>");

		// Route information table
		out.append("<h2>Path delay information</h2>");
		out.append("<table border='1'>");
		out.append("<tr><th><b>Route id</a></b></th><th><b>Demand id</a></b></th><th><b>Ingress node</b></th><th><b>Egress node</b></th><th><b>Sequence of links</b></th><th><b>Propagation delay (ms)</b></th><th><b>Transmission delay (ms)</b></th><th><b>Queuing delay (ms)</b></th><th><b>Total delay (ms)</b></th><th><b>Attributes</a></b></th></tr>");
		for (Route r : netPlan.getRoutes())
		{
			final int rIndex = r.getIndex();
			final Demand d = r.getDemand();
			out.append(String.format("<tr><td>%d</td><td>%d</td><td>%d (%s)</td><td>%d (%s)</td><td>%s</td><td>%.3g</td><td>%.3g</td><td>%.3g</td><td>%.3g</td><td>%s</td></tr>", r.getIndex () , d.getIndex(), r.getIngressNode().getIndex() , r.getIngressNode().getName(), r.getEgressNode().getIndex() , r.getEgressNode().getName(), r.getSeqLinks(), T_r_prop[rIndex], T_r_tx[rIndex], T_r_buf[rIndex], T_r[rIndex], r.getAttributes()));
		}
		out.append("</table>");

		// Demand information table
		out.append("<h2>Demand delay information</h2>");
		out.append("<table border='1'>");
		out.append("<tr><th><b>Demand id</a></b></th><th><b>Ingress node</b></th><th><b>Egress node</b></th><th><b>Propagation delay (ms)</b></th><th><b>Transmission delay (ms)</b></th><th><b>Queuing delay (ms)</b></th><th><b>Total delay (ms)</b></th><th><b>Attributes</a></b></th></tr>");
		for (Demand d : netPlan.getDemands())
		{
			final int dIndex = d.getIndex();
			out.append(String.format("<tr><td>%d</td><td>%d (%s)</td><td>%d (%s)</td><td>%.3g</td><td>%.3g</td><td>%.3g</td><td>%.3g</td><td>%s</td></tr>", dIndex, d.getIngressNode().getIndex() , d.getIngressNode().getName(), d.getEgressNode().getIndex() , d.getEgressNode().getName(), T_d_prop[dIndex] , T_d_tx[dIndex] , T_d_buf[dIndex] , T_d[dIndex] , d.getAttributes()));
		}
		out.append("</table>");

		// Network information table
		out.append("<table border='1'>");
		out.append("<tr><td>Average network delay (ms) [UNICAST]</td><td>").append(String.format("%.3g", avNetDelay_T * 1000)).append("</td></tr>");
		out.append("<tr><td>Average network delay only considering propagation (ms) [UNICAST]</td><td>").append(String.format("%.3g", avNetDelay_T_prop * 1000)).append("</td></tr>");
		out.append("</table>");

		out.append("</body></html>");

		return out.toString();
	}

	@Override
	public String getDescription()
	{
		return "This report shows delay information considering a packet-switched network";
	}

	@Override
	public String getTitle()
	{
		return "Delay metrics";
	}

	@Override
	public List<Triple<String, String, String>> getParameters()
	{
		/* Returns the parameter information for all the InputParameter objects defined in this object (uses Java reflection) */
		return InputParameter.getInformationAllInputParameterFieldsOfObject(this);
	}

	


}