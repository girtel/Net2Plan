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



 




package com.net2plan.examples.smartCity.utn;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;

import com.net2plan.interfaces.networkDesign.IReport;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.Route;
import com.net2plan.utils.InputParameter;
import com.net2plan.utils.Triple;

import cern.colt.matrix.tdouble.DoubleFactory1D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;

/** This is a report for estimating some performances of the traffic in a city, where traffic flows, road capacityies etc. are modeled according to 
 * BPR rules. E.g. this can be used to estimate the performances of the UE and SUE models applied in the example city, both provided in this package  
 * 
 * @net2plan.keywords SmartCity
 * @net2plan.inputParameters 
 * @author Pablo Pavon-Marino, Victoria Bueno-Delgado, Pilar Jimenez-Gomez 
 */
public class Report_urbanTransportationNetworkMetrics implements IReport
{
	private InputParameter alpha = new InputParameter ("alpha", (double) 1 , "Alpha parameter in the BPR model" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter beta = new InputParameter ("beta", (double) 4 , "Beta parameter in the BPR model" , 0 , true , Double.MAX_VALUE , true);

	@Override
	public String executeReport(NetPlan netPlan, Map<String, String> reportParameters, Map<String, String> net2planParameters)
	{
		/* Initialize all InputParameter objects defined in this object (this uses Java reflection) */
		InputParameter.initializeAllInputParameterFieldsOfObject(this, reportParameters);

		/* Some initial statistics */
		final int E = netPlan.getNumberOfLinks();
		DoubleMatrix1D vector_Ca = DoubleFactory1D.dense.make(E);
		DoubleMatrix1D vector_C0a = DoubleFactory1D.dense.make(E);
		for (Link a : netPlan.getLinks())
		{
			final double Qa = a.getCapacity();
			final double Va = a.getCarriedTraffic();
			final double c0 = Double.parseDouble(a.getAttribute(UtnConstants.ATTRNAME_C0A));
			final double Ca = UtnConstants.bprCaComputation(c0,alpha.getDouble(),Va,Qa,beta.getDouble());
			vector_Ca.set(a.getIndex() , Ca);
			vector_C0a.set(a.getIndex() , c0);
		}
		final int R = netPlan.getNumberOfRoutes();
		DoubleMatrix1D vector_Ck = DoubleFactory1D.dense.make(R);
		for (Route r : netPlan.getRoutes())
		{
			double sumCa = 0; for (Link a : r.getSeqLinks()) sumCa += vector_Ca.get(a.getIndex());
			vector_Ck.set(r.getIndex() , sumCa);
		}

		
		/* Construct the report */
		StringBuilder out = new StringBuilder();
		DecimalFormat df_6 = new DecimalFormat("#.###");
		out.append("<html><body>");
		out.append("<head><title>Availability report</title></head>");
		out.append("<h1>Introduction</h1>");
		out.append(getDescription());
		out.append("<h1>General information & statistics</h1>");
		out.append("<table border='1'>");
		out.append("<tr><td>Number of nodes</td><td>" + netPlan.getNumberOfNodes()  +"</td></tr>");
		out.append ("<tr><td>Number of links</td><td>" + netPlan.getNumberOfLinks()  +"</td></tr>");
		out.append ("<tr><td>Number of OD (origin-destination) traffic pairs</td><td>" + df_6.format(netPlan.getNumberOfDemands ())  +"</td></tr>");
		out.append ("<tr><td>Total traffic (sum of the OD pairs)</td><td>" + df_6.format(netPlan.getVectorDemandOfferedTraffic().zSum()) +"</td></tr>");
		out.append ("<tr><td>Total capacity in the arcs (sum of capacity in the arcs)</td><td>" + df_6.format(netPlan.getVectorLinkCapacity().zSum()) +"</td></tr>");
		out.append ("<tr><td>Average end-to-end cost of a vehicle</td><td>" + df_6.format(netPlan.getVectorRouteCarriedTraffic().zDotProduct(vector_Ck)/netPlan.getVectorDemandOfferedTraffic().zSum())  +"</td></tr>");
		out.append ("<tr><td>Worst-case arc utilization (traffic vs. capacity ratio)</td><td>" + df_6.format(netPlan.getVectorLinkUtilization().getMaxLocation()[0])  +"</td></tr>");
		out.append ("</table>");
		
		out.append("<h1>Input parameters report</h1>");
		out.append("<table border='1'>");
		out.append("<tr><th><b>Name</b></th><th><b>Value</b></th><th><b>Description</b></th>");
		for (Triple<String, String, String> paramDef : getParameters())
		{
			String name = paramDef.getFirst();
			String description = paramDef.getThird();
			String value = reportParameters.get(name);
			out.append("<tr><td>").append(name).append("</td><td>").append(value).append("</td><td>").append(description).append("</td></tr>");
		}
		out.append("</table>");

		out.append("<h1>Per link information</h1>");
		out.append("<table border='1'>");
		out.append("<tr><th><b>Index</b></th><th><b>Origin</b></th><th><b>Destination</b></th><th><b>C0a</b></th><th><b>Capacity (Qa)</b></th><th><b>Flow (va)</b></th><th><b>Cost (Ca)</b></th><th><b>Warnings</b></th>");
		for (Link a : netPlan.getLinks())
		{
			out.append("<tr>");
			final double c0 = Double.parseDouble(a.getAttribute(UtnConstants.ATTRNAME_C0A));
			final double Qa = a.getCapacity();
			final double Va = a.getCarriedTraffic();
			final double Ca = UtnConstants.bprCaComputation(c0,alpha.getDouble(),Va,Qa,beta.getDouble());
			out.append("<td>" + a.getIndex() + "</td>");
			out.append("<td>" + a.getOriginNode().getName() + "</td>");
			out.append("<td>" + a.getDestinationNode().getName() + "</td>");
			out.append("<td>" + df_6.format(c0) + "</td>");
			out.append("<td>" + df_6.format(Qa) + "</td>");
			out.append("<td>" + df_6.format(Va) + "</td>");
			out.append("<td>" + df_6.format(Ca) + "</td>");
			out.append("<td>");
			
			if (Va > Qa)
				out.append("<p>The traffic in this arc exceeds its capacity</p>");
			else if (Va >= Qa*0.8)
				out.append("<p>The traffic in this arc exceeds the 80% of the arc capacity</p>");
			else if (Va <= Qa*0.1)
				out.append("<p>The traffic in this arc is below the 10% of its capacity</p>");
			out.append("</td>");
			
			out.append("</tr>");
		}
		out.append("</table>");

		out.append("<h1>Per route information</h1>");
		out.append("<table border='1'>");
		out.append("<tr><th><b>Route index</b></th><th><b>Origin</b></th><th><b>Destination</b></th><th><b>OD pair index and offered traffic</b></th><th><b>Carried traffic</b></th><th><b>Resulting cost</b></th><th><b>Minimum possible route cost</b></th><th><b>Route worst arc utilization</b></th><th><b>Warnings</b></th>");
		for (Route r : netPlan.getRoutes())
		{
			out.append("<tr>");
			out.append("<td>" + r.getIndex() + "</td>");
			out.append("<td>" + r.getIngressNode().getName() + "</td>");
			out.append("<td>" + r.getEgressNode().getName() + "</td>");
			out.append("<td>" + r.getDemand().getIndex() + " (" + df_6.format(r.getDemand().getOfferedTraffic()) + ")</td>");
			out.append("<td>" + df_6.format(r.getCarriedTraffic()) + "</td>");
			double sumC0a = 0; for (Link a : r.getSeqLinks()) sumC0a += vector_C0a.get(a.getIndex());
			double worstUtilization = 0; for (Link a : r.getSeqLinks()) worstUtilization = Math.max(worstUtilization , a.getUtilization());
			out.append("<td>" + df_6.format(vector_Ck.get(r.getIndex())) + "</td>");
			out.append("<td>" + df_6.format(sumC0a) + "</td>");
			out.append("<td>" + df_6.format(worstUtilization) + "</td>");
			out.append("<td>");
			
			if (worstUtilization > 1)
				out.append("<p>This route traverses one or more congested links (utilization higher than 100%)</p>");
			else if (worstUtilization > 0.8)
				out.append("<p>This route traverses one or more quasi-congested links (utilization higher than 80%)</p>");
			out.append("</td>");
			out.append("</tr>");
		}
		out.append("</table>");
		

		
		out.append("</body></html>");

		
		return out.toString();
	}

	@Override
	public String getDescription()
	{
		return "<p>This report computes several metrics regarding the routes in the Urban Transportation Network received as an input."
				+ " This network could be created according to different models (Wardrop, Stochastic User Equilibrium, or others) </p>";
	}

	@Override
	public List<Triple<String, String, String>> getParameters()
	{
		/* Returns the parameter information for all the InputParameter objects defined in this object (uses Java reflection) */
		return InputParameter.getInformationAllInputParameterFieldsOfObject(this);
	}

	@Override
	public String getTitle()
	{
		return "UTN metrics report";
	}

}
