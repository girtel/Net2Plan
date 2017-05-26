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




 





package com.net2plan.examples.general.reports.robustness;

import cern.colt.matrix.tdouble.DoubleMatrix1D;
import com.net2plan.interfaces.networkDesign.IReport;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.libraries.GraphTheoryMetrics;
import com.net2plan.utils.DoubleUtils;
import com.net2plan.utils.HTMLUtils;
import com.net2plan.utils.Triple;
import org.codehaus.stax2.XMLOutputFactory2;
import org.codehaus.stax2.XMLStreamWriter2;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * This report analyzes a network design (all the layers) in terms of robustness 
 * under different multiple-failures scenarios.
 * 
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @version 1.1, May 2015
 */
public class Report_robustness implements IReport
{
	@Override
	public String executeReport(NetPlan netPlan, Map<String, String> reportParameters, Map<String, String> net2planParameters)
	{
		boolean doReport = false;
		for(NetworkLayer layer : netPlan.getNetworkLayers ()) if (netPlan.hasLinks(layer)) { doReport = true; break; }
		if (!doReport) throw new Net2PlanException("A network with nodes and links is required");
		
		try (ByteArrayOutputStream os = new ByteArrayOutputStream())
		{
			XMLOutputFactory2 output = (XMLOutputFactory2) XMLOutputFactory.newFactory();
			XMLStreamWriter2 writer = (XMLStreamWriter2) output.createXMLStreamWriter(os);
			
			writer.writeStartDocument("UTF-8", "1.0");
			writer.writeStartElement("network");
			
			int N = netPlan.getNumberOfNodes();
			for(NetworkLayer layer : netPlan.getNetworkLayers())
			{
				int E = netPlan.getNumberOfLinks(layer);
				if (E == 0) continue;

				GraphTheoryMetrics metrics = new GraphTheoryMetrics(netPlan.getNodes (), netPlan.getLinks (layer), null);
				DoubleMatrix1D linkBetweenessCentrality = metrics.getLinkBetweenessCentrality();
				DoubleMatrix1D nodeBetweenessCentrality = metrics.getNodeBetweenessCentrality();
				DoubleMatrix1D outNodeDegreeMap = metrics.getOutNodeDegree();

				double a2tr = metrics.getAverageTwoTermReliability();
				double algebraicConnectivity = metrics.getAlgebraicConnectivity();
				double assortativity = metrics.getAssortativity();
				double averageNeighborConnectivity = metrics.getAverageNeighborConnectivity();
				double averageLinkBC = DoubleUtils.average(linkBetweenessCentrality.toArray());
				double averageNodeBC = DoubleUtils.average(nodeBetweenessCentrality.toArray());
				double averageOutNodeDegree = DoubleUtils.average(outNodeDegreeMap.toArray());
				double averagePathLength = metrics.getAverageShortestPathDistance();
				double clusteringCoeff = metrics.getClusteringCoefficient();
				double density = metrics.getDensity();
				int diameter = (int) metrics.getDiameter();
				double heterogeneity = metrics.getHeterogeneity();
				int linkConnectivity = metrics.getLinkConnectivity();
				int maxNodeDegree = outNodeDegreeMap.size () > 0? (int) outNodeDegreeMap.getMaxLocation() [0] : 0;
				int nodeConnectivity = metrics.getNodeConnectivity();
				double spectralRadius = metrics.getSpectralRadius();
				double symmetryRatio = metrics.getSymmetryRatio();

				writer.writeStartElement("layer");
				writer.writeAttribute("index", "" + layer.getIndex ());
				writer.writeAttribute("name", layer.getName ());
				writer.writeAttribute("algebraicConnectivity", String.format("%.3f", algebraicConnectivity));
				writer.writeAttribute("comments_algebraicConnectivity", "-");
				writer.writeAttribute("assortativity", String.format("%.3f", assortativity));
				writer.writeAttribute("comments_assortativity", assortativity < 0 ? "Your network is vulnerable to static attacks" : "Your network is robust to static attacks");
				writer.writeAttribute("averageLinkBC", String.format("%.3f", averageLinkBC));
				writer.writeAttribute("comments_averageLinkBC", "-");
				writer.writeAttribute("averageNeighborConnectivity", String.format("%.3f", averageNeighborConnectivity));
				writer.writeAttribute("comments_averageNeighborConnectivity", "-");
				writer.writeAttribute("outNodeDegree", String.format("%.3f", averageOutNodeDegree));
				writer.writeAttribute("comments_outNodeDegree", "-");
				writer.writeAttribute("averageNodeBC", String.format("%.3f", averageNodeBC));
				writer.writeAttribute("comments_averageNodeBC", "-");
				writer.writeAttribute("avgPathLength", String.format("%.3f", averagePathLength));
				writer.writeAttribute("comments_avgPathLength", "-");
				writer.writeAttribute("a2tr", String.format("%.3f", a2tr));
				writer.writeAttribute("comments_a2tr", "-");
				writer.writeAttribute("clusteringCoeff", String.format("%.3f", clusteringCoeff));
				writer.writeAttribute("comments_clusteringCoeff", "-");
				writer.writeAttribute("density", String.format("%.3f", density));
				writer.writeAttribute("comments_density", "-");
				writer.writeAttribute("diameter", String.format("%d", diameter));
				writer.writeAttribute("comments_diameter", "-");
				writer.writeAttribute("heterogeneity", String.format("%.3f", heterogeneity));
				writer.writeAttribute("comments_heterogeneity", "-");
				writer.writeAttribute("linkConnectivity", String.format("%d", linkConnectivity));
				writer.writeAttribute("comments_linkConnectivity", "-");
				writer.writeAttribute("numberOfLinks", String.format("%d", E));
				writer.writeAttribute("comments_numberOfLinks", "-");
				writer.writeAttribute("maxNodeDegree", String.format("%d", maxNodeDegree));
				writer.writeAttribute("comments_maxNodeDegree", "-");
				writer.writeAttribute("nodeConnectivity", String.format("%d", nodeConnectivity));
				writer.writeAttribute("comments_nodeConnectivity", "-");
				writer.writeAttribute("numberOfNodes", String.format("%d", N));
				writer.writeAttribute("comments_numberOfNodes", "-");
				writer.writeAttribute("spectralRadius", String.format("%.3f", spectralRadius));
				writer.writeAttribute("comments_spectralRadius", "-");
				writer.writeAttribute("symmetryRatio", String.format("%.3f", symmetryRatio));
				writer.writeAttribute("comments_symmetryRatio", symmetryRatio < 1 ? "-" : "Your network performs equally in response to a random (SR) or a target attack (ST)");
				writer.writeEndElement();
			}

			writer.writeEndElement();
			writer.writeEndDocument();
			writer.flush();
			writer.close();

			String xml = os.toString(StandardCharsets.UTF_8.name());
			return HTMLUtils.getHTMLFromXML(xml, Report_robustness.class.getResource("/com/net2plan/examples/reports/robustness/Report_robustness.xsl").toURI().toURL());
		}
		catch (IOException | URISyntaxException | XMLStreamException ex)
		{
			throw new RuntimeException(ex);
		}
	}
	
	@Override
	public String getDescription()
	{
		return "This report analyzes a network design (all the layers) in terms of robustness under different multiple-failures scenarios";
	}

	@Override
	public List<Triple<String, String, String>> getParameters()
	{
		return null;
	}

	@Override
	public String getTitle()
	{
		return "Robustness report";
	}
	
}
