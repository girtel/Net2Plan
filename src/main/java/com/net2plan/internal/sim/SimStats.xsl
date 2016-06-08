<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	<xsl:output method="html"/>
	<xsl:template match="/">
		<h2>Contents</h2>
		
		<ul>
			<li><a href='#sectionNetworkInformation'>Network information</a></li>
			<li><a href='#sectionNodeInformation'>Node information</a></li>
			<xsl:for-each select="network/layer">
				<xsl:variable name="layerId" select="@id"/>
				<xsl:choose>
					<xsl:when test="@name=''">
						<li><a href='#sectionResults_layer{$layerId}'>Layer <xsl:value-of select="@id" /> information</a></li>
					</xsl:when>
					<xsl:otherwise>
						<li><a href='#sectionResults_layer{$layerId}'>Layer <xsl:value-of select="@id" /> (<xsl:value-of select="@name" />) information</a></li>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:for-each>
		</ul>
		
		<a name='sectionNetworkInformation'></a><h2>Network information</h2>
		
		<dl>
			<dt>Number of layers (avg./min./max.)</dt>
			<dd><xsl:value-of select="network/@avgNumLayers" /> / <xsl:value-of select="network/@minNumLayers" /> / <xsl:value-of select="network/@maxNumLayers" /></dd>
			<dt>Number of nodes (avg./min./max.)</dt>
			<dd><xsl:value-of select="network/@avgNumNodes" /> / <xsl:value-of select="network/@minNumNodes" /> / <xsl:value-of select="network/@maxNumNodes" /></dd>
		</dl>
		
		<a name='sectionNodeInformation'></a><h2>Node information</h2>
		
		<table border="1">
			<thead>
				<tr>
					<th>Identifier</th>
					<th>Name</th>
					<th>Up time</th>
					<th>Total time</th>
				</tr>
			</thead>
			<xsl:for-each select="network/node">
				<tr>
					<td><xsl:value-of select="@id" /></td>
					<td><xsl:value-of select="@name" /></td>
					<td><xsl:value-of select="@upTime" /> (<xsl:value-of select="@upTimePercentage" /> %)</td>
					<td><xsl:value-of select="@totalTime" /></td>
				</tr>
			</xsl:for-each>
		</table>
		
		<xsl:for-each select="network/layer">
			<xsl:variable name="layerId" select="@id"/>
			<a name='sectionResults_layer{$layerId}'></a>
			<xsl:choose>
				<xsl:when test="@name=''">
					<h2>Layer <xsl:value-of select="@id" /> information</h2>
				</xsl:when>
				<xsl:otherwise>
					<h2>Layer <xsl:value-of select="@id" /> (<xsl:value-of select="@name" />) information</h2>
				</xsl:otherwise>
			</xsl:choose>			
			
			<h3>Aggregated metrics</h3>
			<dl>
				<dt>Number of links (avg./min./max.)</dt>
				<dd><xsl:value-of select="@avgNumLinks" /> / <xsl:value-of select="@minNumLinks" /> / <xsl:value-of select="@maxNumLinks" /></dd>
				<dt>Number of demands (avg./min./max.)</dt>
				<dd><xsl:value-of select="@avgNumDemands" /> / <xsl:value-of select="@minNumDemands" /> / <xsl:value-of select="@maxNumDemands" /></dd>
				<dt>Total time</dt>
				<dd><xsl:value-of select="@totalTime" /></dd>
				<dt>Traffic units name</dt>
				<dd><xsl:value-of select="@trafficUnitsName" /></dd>
				<dt>Average offered traffic (avg./min./max.)</dt>
				<dd><xsl:value-of select="@avgOfferedTraffic" /> / <xsl:value-of select="@minOfferedTraffic" /> / <xsl:value-of select="@maxOfferedTraffic" /></dd>
				<dt>Average carried traffic (avg./min./max.)</dt>
				<dd><xsl:value-of select="@avgCarriedTraffic" /> / <xsl:value-of select="@minCarriedTraffic" /> / <xsl:value-of select="@maxCarriedTraffic" /></dd>
				<dt>Capacity units name</dt>
				<dd><xsl:value-of select="@capacityUnitsName" /></dd>
				<dt>Average total capacity installed (avg./min./max.)</dt>
				<dd><xsl:value-of select="@avgTotalCapacity" /> / <xsl:value-of select="@minTotalCapacity" /> / <xsl:value-of select="@maxTotalCapacity" /></dd>
				<dt>Layer congestion (avg./min./max.)</dt>
				<dd><xsl:value-of select="@avgCongestion" /> / <xsl:value-of select="@minCongestion" /> / <xsl:value-of select="@maxCongestion" /></dd>
				<dt>Layer availability (classic / weighted)</dt>
				<dd><xsl:value-of select="@availabilityClassic" /> / <xsl:value-of select="@availabilityWeighted" /></dd>
				<dt>Worst demand availability (classic / weighted)</dt>
				<dd><xsl:value-of select="@worstDemandAvailabilityClassic" /> / <xsl:value-of select="@worstDemandAvailabilityWeighted" /></dd>
			</dl>
			
			<h3>Node information</h3>
			
			<table border="1">
				<thead>
					<tr>
						<th>Identifier</th>
						<th>Name</th>
						<th>In-degree (avg./min./max.)</th>
						<th>Out-degree (avg./min./max.)</th>
						<th>Ingress traffic (avg./min./max.)</th>
						<th>Egress traffic (avg./min./max.)</th>
						<th>Traversing traffic (avg./min./max.)</th>
					</tr>
				</thead>
				<xsl:for-each select="node">
					<tr>
						<td><xsl:value-of select="@id" /></td>
						<td><xsl:value-of select="@name" /></td>
						<td><xsl:value-of select="@avgInDegree" /> / <xsl:value-of select="@minInDegree" /> / <xsl:value-of select="@maxInDegree" /></td>
						<td><xsl:value-of select="@avgOutDegree" /> / <xsl:value-of select="@minOutDegree" /> / <xsl:value-of select="@maxOutDegree" /></td>
						<td><xsl:value-of select="@avgIngressTraffic" /> / <xsl:value-of select="@minIngressTraffic" /> / <xsl:value-of select="@maxIngressTraffic" /></td>
						<td><xsl:value-of select="@avgEgressTraffic" /> / <xsl:value-of select="@minEgressTraffic" /> / <xsl:value-of select="@maxEgressTraffic" /></td>
						<td><xsl:value-of select="@avgTraversingTraffic" /> / <xsl:value-of select="@minTraversingTraffic" /> / <xsl:value-of select="@maxTraversingTraffic" /></td>
					</tr>
				</xsl:for-each>
			</table>

			<h3>Link information</h3>
			
			<table border="1">
				<thead>
					<tr>
						<th>Identifier</th>
						<th>Origin node</th>
						<th>Destination node</th>
						<th>Length (in km) (avg./min./max.)</th>
						<th>Capacity (avg./min./max.)</th>
						<th>Carried traffic (avg./min./max.)</th>
						<th>Reserved bandwidth for protection (avg./min./max.)</th>
						<th>Utilization (avg./min./max.)</th>
						<th>Utilization without reserved bandwidth (avg./min./max.)</th>
						<th>Over-subscribed capacity (avg./min./max.)</th>
						<th>Over-subscribed time</th>
						<th>Up time</th>
						<th>Total time</th>
					</tr>
				</thead>
				<xsl:for-each select="link">
					<tr>
						<td><xsl:value-of select="@id" /></td>
						<td><xsl:value-of select="@originNode" /></td>
						<td><xsl:value-of select="@destinationNode" /></td>
						<td><xsl:value-of select="@avgLengthInKm" /> / <xsl:value-of select="@minLengthInKm" /> / <xsl:value-of select="@maxLengthInKm" /></td>
						<td><xsl:value-of select="@avgCapacity" /> / <xsl:value-of select="@minCapacity" /> / <xsl:value-of select="@maxCapacity" /></td>
						<td><xsl:value-of select="@avgCarriedTraffic" /> / <xsl:value-of select="@minCarriedTraffic" /> / <xsl:value-of select="@maxCarriedTraffic" /></td>
						<td><xsl:value-of select="@avgReservedBandwidth" /> / <xsl:value-of select="@minReservedBandwidth" /> / <xsl:value-of select="@maxReservedBandwidth" /></td>
						<td><xsl:value-of select="@avgUtilization" /> / <xsl:value-of select="@minUtilization" /> / <xsl:value-of select="@maxUtilization" /></td>
						<td><xsl:value-of select="@avgUtilizationWithoutReservedBandwidth" /> / <xsl:value-of select="@minUtilizationWithoutReservedBandwidth" /> / <xsl:value-of select="@maxUtilizationWithoutReservedBandwidth" /></td>
						<td><xsl:value-of select="@avgOversubscribedCapacity" /> / <xsl:value-of select="@minOversubscribedCapacity" /> / <xsl:value-of select="@maxOversubscribedCapacity" /></td>
						<td><xsl:value-of select="@oversubscribedTime" /> (<xsl:value-of select="@oversubscribedTimePercentage" /> %)</td>
						<td><xsl:value-of select="@upTime" /> (<xsl:value-of select="@upTimePercentage" /> %)</td>
						<td><xsl:value-of select="@totalTime" /></td>
					</tr>
				</xsl:for-each>
			</table>

			<h3>Demand information</h3>
			
			<table border="1">
				<thead>
					<tr>
						<th>Identifier</th>
						<th>Ingress node</th>
						<th>Egress node</th>
						<th>Offered traffic (avg./min./max.)</th>
						<th>Carried traffic (avg./min./max.)</th>
						<th>Blocked traffic (avg./min./max.)</th>
						<th>Availability (classic / weighted)</th>
						<th>Excess carried traffic (avg./min./max.)</th>
						<th>Excess carried traffic time</th>
						<th>Total time</th>
					</tr>
				</thead>
				<xsl:for-each select="demand">
					<tr>
						<td><xsl:value-of select="@id" /></td>
						<td><xsl:value-of select="@ingressNode" /></td>
						<td><xsl:value-of select="@egressNode" /></td>
						<td><xsl:value-of select="@avgOfferedTraffic" /> / <xsl:value-of select="@minOfferedTraffic" /> / <xsl:value-of select="@maxOfferedTraffic" /></td>
						<td><xsl:value-of select="@avgCarriedTraffic" /> / <xsl:value-of select="@minCarriedTraffic" /> / <xsl:value-of select="@maxCarriedTraffic" /></td>
						<td><xsl:value-of select="@avgBlockedTraffic" /> / <xsl:value-of select="@minBlockedTraffic" /> / <xsl:value-of select="@maxBlockedTraffic" /></td>
						<td><xsl:value-of select="@availabilityClassic" /> / <xsl:value-of select="@availabilityWeighted" /></td>
						<td><xsl:value-of select="@avgExcessCarriedTraffic" /> / <xsl:value-of select="@minExcessCarriedTraffic" /> / <xsl:value-of select="@maxExcessCarriedTraffic" /></td>
						<td><xsl:value-of select="@excessCarriedTrafficTime" /> (<xsl:value-of select="@excessCarriedTrafficTimePercentage" /> %)</td>
						<td><xsl:value-of select="@totalTime" /></td>
					</tr>
				</xsl:for-each>
			</table>

		</xsl:for-each>

	</xsl:template>

</xsl:stylesheet>
