<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE xsl:stylesheet [
<!ENTITY Delta "&#916;">
<!ENTITY beta "&#946;">
<!ENTITY delta "&#948;">
<!ENTITY isin "&#8712;">
<!ENTITY lambda "&#955;">
<!ENTITY le "&#8804;">
<!ENTITY tau "&#964;">
]>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	<xsl:output method="html"/>
	<xsl:template match="/">
		<html>
			<head>
				<title>Robustness metrics</title>
			</head>
			<body>

				<h1>Contents</h1>

				<ul>
					<li><a href='#sectionIntroduction'>Introduction</a></li>
					<li>
						<a href='#sectionBackground'>Network impairments</a>
						<ul>
							<li><a href='#sectionBackgroundStatic'>Static</a></li>
							<li><a href='#sectionBackgroundDynamic'>Dynamic</a></li>
						</ul>
					</li>
					<li><a href='#sectionMetrics'>Metrics</a>
						<ul>
							<xsl:for-each select="network/layer">
								<xsl:variable name="layerId" select="@index"/>
								<xsl:choose>
									<xsl:when test="@name=''">
										<li><a href='#sectionMetrics_layer{$layerId}'>Layer <xsl:value-of select="@id" /> metrics</a></li>
									</xsl:when>
									<xsl:otherwise>
										<li><a href='#sectionMetrics_layer{$layerId}'>Layer <xsl:value-of select="@id" /> (<xsl:value-of select="@name" />) metrics</a></li>
									</xsl:otherwise>
								</xsl:choose>
							</xsl:for-each>
						</ul>
					</li>
					<li><a href='#sectionReferences'>References</a></li>
					<li><a href='#sectionCredits'>Credits</a></li>
				</ul>

				<a name='sectionIntroduction'></a>
				<h1>Introduction</h1>
				<p>As any engineering system, a network (or a part thereof) can fail for a number of reasons: faulty hardware, software bugs, breakage of physical medium (e.g. fiber cables), and even because of power outages. These are examples of failures that affect specific and, generally, separated network elements. It is typical to assume that such failures are independent events, and that the probability that two events happen simultaneously is quite low. The resulting setting is commonly referred to as a <i>single failure</i> scenario.</p>

				<p>A large number of techniques exist for dealing with failures, collectively known as network recovery techniques. The fundamental idea underlying recovery is that of redundancy, whereby network elements deemed to be unreliable are backed up with one or more spare resources that come into play upon a failure. Almost all of the recovery techniques focus on single failures and aim to offer a specific trade-off between resilience guarantee and resource consumption. The recovery techniques are mature and their efficacy is proven.</p>

				<p>As size, integration and complexity of networks increase, multiple failure become more probable during the operation of the system. The distinguishing trait here is that a significant portion of the network fails simultaneously. From the point of view of the traditional recovery techniques, a large-scale failure is difficult to handle due to the fact that the redundancy-based approach that is effective for single failures is no longer suitable: the cost of implementing massive redundancy for rarely occurring events is simply prohibitive.</p>

				<p>Although large-scale failures may be relatively rare, they usually have serious consequences in terms of the economic loss they cause and the disruption they bring upon thoushands or even millions of users. Therefore, it is mandatory to have methods and tools that can be used in the design and operation of the communications infrastructure so that essential services can be preserved as much as possible when large-scale failures occur. In this context, a key requirement is the ability to evaluate the <i>robustness</i> of the network, that is, be able to assess the performance degradation that should be expected as a consequence of the failure.</p>

				<a name='sectionBackground'></a>
				<h1>Network impairments</h1>

				<p>The robustness of a network depends on the type of impairment that occurs. From here on, the term <i>impairment</i> refers to any kind of attack, multiple or cascading failures that can happen within a network (it does not refer to physical layer impairments such as  cross-talk or chromatic dispersion). Several taxonomies have been proposed in order to classify network attacks [<a href='#attacksTaxonomy1'>1</a>,<a href='#attacksTaxonomy2'>2</a>]. Consequently, the discussion presented here simplifies such previous categorizations and focuses on classifying the types of impairments that can occur on the nodes of a network. Nonetheless, it has also been defined in order to be easy extended, so as to consider other componentes of a network, such as links.</p>

				<p>Impairments or multiple failures are basically divided into two groups: <i>static</i> and <i>dynamic</i>. The former is related to the idea of affecting a network permanently and just once, while the latter is related to an impairment that has a temporal dimension.</p>

				<a name='sectionBackgroundStatic'></a>
				<h2>Static</h2>

				Static impairments are essentially one-off attacks that affect one or more nodes at any given point. There are, in essence, two forms of static impairments:

				<dl>
					<dt>Random (SR, Static Random)</dt>
					<dd>
						<p>In the SR case, nodal attacks occur indiscriminately selecting nodes at random. Fig. <a href='#fig1'>1</a> shows this kind of impairments.</p>

					<center>
						<a name='fig1'></a>
						<img src='fig1.png' />
						<br />
						<p>Figure 1: Examples of a SR impairment. (a) and (b) show that nodes are chosen randomly</p>
					</center>
					</dd>

					<dt>Target (ST, Static Target)</dt>
					<dd>
					<p>Nodes in an ST attack are chosen in order to maximize the effect of that attack; there is an element of discrimination in the impairment. The choice of attack target may be a function of network-defined features such as nodal degree, betweeness centrality... as well as other "real-world" features, such as the number of users potentially affected and socio-pollitical and economic considerations. Figs. <a href='#fig2'>2</a> and <a href='#fig3'>3</a> show some examples of ST attacks, considering different elements of discrimination.</p>

					<center>
						<a name='fig2'></a>
						<img src='fig2.png' />
						<br />
						<p>Figure 2: Example of a ST impairment. The target of the attack is affecting the node with the largest node degree</p>
					</center>

					<center>
						<a name='fig3'></a>
						<img src='fig3.png' />
						<br />
						<p>Figure 3: Example of a ST impairment. The target of the attack is partitioning the network into two disconnected sub-networks</p>
					</center>
					</dd>
				</dl>

				<a name='sectionBackgroundDynamic'></a>
				<h2>Dynamic</h2>

				This second type of failures (commonly related to multiple failures such as cascading failures) has a temporal dimension. Two types are defined:

				<dl>
					<dt>Epidemical (DE, Dynamic Epidemical)</dt>
					<dd>
						<p>Considering a DE, a failure occurs in a node (or a set of nodes of the network) and the failure can spread through the network (becoming an epidemic) or not. Fig. <a href='#fig4'>4</a> shows an example of how an epidemic can affect a network.</p>

					<center>
						<a name='fig4'></a>
						<img src='fig4.png' />
						<br />
						<p>Figure 4: Example of a DE impairment. A failure occurs on a node, and after a period of time, it spreads to its neighbours</p>
					</center>

					<p>This type of failures is based on epidemic models (EM) and there are several forms of them, depending on the evolution of susceptible nodes (nodes in contact with another infected node) once become infected: infected nodes will remain infected forever (<i>Susceptible-Infected</i> model, used for "worst case propagation" analysis), infected nodes will eventually become susceptible again (<i>Susceptible-Infected-Susceptible</i> model), infected nodes will be able to recover and become immune so as will no longer pass the infection onto others (<i>Susceptible-Infected-Recovered</i> model)... In [<a href='#wirelessEM'>3</a>, <a href='#gmplsEM'>4</a>] different applications of epidemic models to communication networks has been studied.</p>
				</dd>
				<dt>Periodical (DP, Dynamic Periodical)</dt>
				<dd>
					<p>A DP is, simply, any kind of impairment that occurs periodically following its characteristic cycle.</p>
				</dd>
				</dl>

				<a name='sectionMetrics'></a>
				<h1>Metrics</h1>

				<p>Many measures have been proposed in the literature, with the aim of capturing or characterizing some properties in the network, measuring in different ways if the network is "well prepared" or not to survive (i.e. to remain as a connected network) under multiple failures in nodes and/or links. Most of these metrics are computed depending only on the network topology.</p>

				<xsl:for-each select="network/layer">
					<xsl:variable name="layerId" select="@id"/>
					<a name='sectionMetrics_layer{$layerId}'></a>
					<xsl:choose>
						<xsl:when test="@name=''">
							<h2>Layer <xsl:value-of select="@id" /> metrics</h2>
						</xsl:when>
						<xsl:otherwise>
							<h2>Layer <xsl:value-of select="@id" /> (<xsl:value-of select="@name" />) metrics</h2>
						</xsl:otherwise>
					</xsl:choose>			

					<center>
						<table border="1">
							<tr><th><b>Metric</b></th><th><b>Value</b></th><th><b>Comment</b></th></tr>
							<tr><td><a href='#metrics_algebraicConnectivity'>Algebraic connectivity</a></td><td><xsl:value-of select="@algebraicConnectivity" /></td><td><xsl:value-of select="@comments_algebraicConnectivity" /></td></tr>
							<tr><td><a href='#metrics_assortativity'>Assortativity</a></td><td><xsl:value-of select="@assortativity" /></td><td><xsl:value-of select="@comments_assortativity" /></td></tr>
							<tr><td><a href='#metrics_betweenessCentrality'>Average link betweeness centrality</a></td><td><xsl:value-of select="@averageLinkBC" /></td><td><xsl:value-of select="@comments_averageLinkBC" /></td></tr>
							<tr><td><a href='#metrics_averageNeighborConnectivity'>Average neighbor connectivity</a></td><td><xsl:value-of select="@averageNeighborConnectivity" /></td><td><xsl:value-of select="@comments_averageNeighborConnectivity" /></td></tr>
							<tr><td><a href='#metrics_outNodeDegree'>Average (out) node degree</a></td><td><xsl:value-of select="@outNodeDegree" /></td><td><xsl:value-of select="@comments_outNodeDegree" /></td></tr>
							<tr><td><a href='#metrics_betweenessCentrality'>Average node betweeness centrality</a></td><td><xsl:value-of select="@averageNodeBC" /></td><td><xsl:value-of select="@comments_averageNodeBC" /></td></tr>
							<tr><td><a href='#metrics_avgPathLength'>Average shortest path length</a></td><td><xsl:value-of select="@avgPathLength" /></td><td><xsl:value-of select="@comments_avgPathLength" /></td></tr>
							<tr><td><a href='#metrics_a2tr'>Average two-term reliability</a></td><td><xsl:value-of select="@a2tr" /></td><td><xsl:value-of select="@comments_a2tr" /></td></tr>
							<tr><td><a href='#metrics_clusteringCoeff'>Clustering coefficient</a></td><td><xsl:value-of select="@clusteringCoeff" /></td><td><xsl:value-of select="@comments_clusteringCoeff" /></td></tr>
							<tr><td><a href='#metrics_density'>Density</a></td><td><xsl:value-of select="@density" /></td><td><xsl:value-of select="@comments_density" /></td></tr>
							<tr><td><a href='#metrics_diameter'>Diameter</a></td><td><xsl:value-of select="@diameter" /></td><td><xsl:value-of select="@comments_diameter" /></td></tr>
							<tr><td><a href='#metrics_heterogeneity'>Heterogeneity</a></td><td><xsl:value-of select="@heterogeneity" /></td><td><xsl:value-of select="@comments_heterogeneity" /></td></tr>
							<tr><td><a href='#metrics_linkConnectivity'>Link connectivity</a></td><td><xsl:value-of select="@linkConnectivity" /></td><td><xsl:value-of select="@comments_linkConnectivity" /></td></tr>
							<tr><td><a href='#metrics_numberOfLinks'>Link set size (|E|)</a></td><td><xsl:value-of select="@numberOfLinks" /></td><td><xsl:value-of select="@comments_numberOfLinks" /></td></tr>
							<tr><td><a href='#metrics_maxNodeDegree'>Maximum node degree</a></td><td><xsl:value-of select="@maxNodeDegree" /></td><td><xsl:value-of select="@comments_maxNodeDegree" /></td></tr>
							<tr><td><a href='#metrics_nodeConnectivity'>Node connectivity</a></td><td><xsl:value-of select="@nodeConnectivity" /></td><td><xsl:value-of select="@comments_nodeConnectivity" /></td></tr>
							<tr><td><a href='#metrics_numberOfNodes'>Node set size (|N|)</a></td><td><xsl:value-of select="@numberOfNodes" /></td><td><xsl:value-of select="@comments_numberOfNodes" /></td></tr>
							<tr><td><a href='#metrics_spectralRadius'>Spectral radius</a></td><td><xsl:value-of select="@spectralRadius" /></td><td><xsl:value-of select="@comments_spectralRadius" /></td></tr>
							<tr><td><a href='#metrics_symmetryRatio'>Symmetry ratio</a></td><td><xsl:value-of select="@symmetryRatio" /></td><td><xsl:value-of select="@comments_symmetryRatio" /></td></tr>
						</table>
					</center>
				</xsl:for-each>
				
				<a name='sectionDefinitions'></a><h2>Definitions</h2>
				<dl>
					<dt><a name='metrics_algebraicConnectivity'></a>Algebraic connectivity (<i>&lambda;<sub>2</sub></i>)</dt>
					<dd><p>This metric is defined as the second smallest eigenvalue of the Laplacian matrix of the graph. It measures how difficult it is to break the network into islands or individual components. The larger the &lambda;<sub>2</sub>, the greater the robustness of a topology against both node and link removal [<a href='#algebraicConnectivityBounds'>5</a>]. The value of <i>&lambda;<sub>2</sub></i> is in range:</p>

					<center><img src='algebraicConnectivity.png' /></center>

					<p>where <i>d<sub>i</sub></i> is the (out) nodal degree of node <i>i</i>.</p>

					<br />

					<p>Given the <i>NxN</i> adjacency matrix <i>A</i> of a graph <i>G</i> and the <i>NxN</i> <i>&Delta;=diag(d<sub>i</sub>)</i> matrix, the Laplacian matrix <i>L</i> of the graph is given by [<a href='#laplacianMatrix'>6</a>]:</p>

					<center><img src='laplacianMatrix.png' /></center>
				</dd>

				<dt><a name='metrics_assortativity'>Assortativity</a></dt>
				<dd><p>The assortativity coefficient <i>r</i>, can take values between <i>-1&le;r&le;1</i>. When <i>r&lt;0</i> the network is called to be dissassortative, which means that has an excess of links connecting nodes of dissimilar degrees. Such networks are vulnerable to both static random and targeted attacks (SR and ST). The opposite properties apply to assortative networks with <i>r>0</i> that have an excess of links connecting nodes of similar degrees. Many social networks have significant assortative structure, while technological and biological networks seem to be disassortative.</p>

					<p>The global assortativity coefficient <i>r</i> can be determined taking into account the Pearson correlation coefficient of the (out) degrees at both end of the edges:</p>

				<center><img src='assortativity1.png' /></center>

				<!--
				r = \frac{\left[\frac{1}{M}\sum_{e}(j_{e}k_{e})\right]-y^{2}}{\left[\frac{1}{2M}\sum_{e}(j^{2}_{e}+k^{2}_{e})\right]-y^{2}}
				-->

				<p>where <i>j<sub>e</sub></i> and <i>k<sub>e</sub></i> are the degrees of the vertices at the end of edge <i>e&isin;E</i>, <i>M=|E|</i> and</p>

				<center><img src='assortativity2.png' /></center>

				<!--
				y = \frac{1}{2M}\sum_{e}(j_{e}+k_{e})
				-->
				</dd>

				<dt><a name='metrics_averageNeighborConnectivity'>Average neighbor connectivity</a></dt>
				<dd><p>This metric provides information about 1-hop neighborhoods around a node. It is a summary statistic of the Joint degree distribution (JDD) and it is simply calculated as the average neighbor degree of the average k-degree node [<a href='#averageNeighborConnectivity'>7</a>].</p></dd>

				<dt><a name='metrics_outNodeDegree'>Average (out) node degree</a></dt>
				<dd><p>This metric is the average (out) node degree of a network.</p></dd>

				<dt><a name='metrics_avgPathLength'>Average shortest path length</a></dt>
				<dd><p>Average shortest path length (ASPL) is calculated as an average of all the shortest paths between all the possible origin-destination node pairs of the network. Networks with small ASPL are more robust because, in response to any kind of impairment (SR, ST, DE or DP), they are likely to close fewer connections.</p></dd>

				<dt><a name='metrics_a2tr'>Average two-terminal reliability</a></dt>
				<dd><p>This metric is the probability that a randomly chosen pair of nodes is connected. If the network is fully connected the value of A2TR is 1. Otherwise, it is the sum over the number of node pair in every connected component divided by the total number of node pairs in the network. This ratio gives the fraction of node pairs that are connected to each other. Therefore, the higher the value (for a given number of removed nodes), the more robust the network is in response to an static random (SR) attack that affects the same number of nodes.</p></dd>

				<dt><a name='metrics_betweenessCentrality'>Betweeness centrality</a></dt>
				<dd><p>This metric is equal to the number of shortest paths from all vertices to all others that pass through a node (or link). Betweenness centrality is a more useful measure of the load placed on the given node (or link) in the network as well as the importance of the node (or link) to the network than just connectivity. The latter is only a local effect while the former is more global to the network.</p></dd>

				<dt><a name='metrics_clusteringCoeff'>Clustering coefficient</a></dt>
				<dd><p>This metric measures the degree to which nodes in a network tend to cluster together. Clustering expresses local robustness in the network and thus has practical implications: the higher the clustering, the more interconnected are neighbors from a given node, thus increasing the path diversity locally around that node.</p>

					<p>The clustering coefficient of the network is the average of the local coefficients. The local coefficient of a node <i>i</i> is given by:</p>

					<ul>
						<li>0, if <i>d<sub>i</sub>=0</i></li>
						<li>1, if <i>d<sub>i</sub>=1</i></li>
						<li>fraction of node <i>i</i> neighbors that are also neighbors of each other, if <i>d<sub>i</sub>&gt;1</i></li>
					</ul>
				</dd>

				<dt><a name='metrics_density'>Density</a></dt>
				<dd><p>This metric measures how many links are compared to the maximum possible number of links between nodes. If a network has no loops and a simple graph model is assumed, that network can have at most <i>|N|x(|N|-1)</i> links.</p>

				<center><img src='density.png' /></center>

				<!--
				\text{Density} = \frac{|E|}{|N|\cdot(|N|-1)}
				-->
				</dd>

				<dt title="prueba diametro"><a name='metrics_diameter'>Diameter</a></dt>
				<dd><p>This metric is the longest of all the shortest paths between pairs of nodes. In general, low-diameter networks are more robust, but only if node connectivity is high.</p></dd>

				<dt><a name='metrics_heterogeneity'>Heterogeneity</a></dt>
				<dd><p>Heterogeneity is the standard deviation among the all-pairs shortest path lengths divided by the average all-pairs shortest path length. The lower the magnitude of its heterogeneity, the greater the robustness of the topology.</p></dd>

				<dt><a name='metrics_linkConnectivity'>Link connectivity</a></dt>
				<dd><p>This metric represents the smallest number of edge-disjoint paths between any pair of nodes. This metric gives a crude indication of the robustness of a network in response to any of the impairments.</p></dd>

				<dt><a name='metrics_numberOfLinks'>Link set size (|E|)</a></dt>
				<dd><p>This metric is equal to the number of directional links in the network.</p></dd>

				<dt><a name='metrics_maxNodeDegree'>Maximum node degree</a></dt>
				<dd><p>This metric is equal to the highest (out) node degree</p></dd>

				<dt><a name='metrics_nodeConnectivity'>Node connectivity</a></dt>
				<dd><p>This metric represents the smallest number of node-disjoint paths between any pair of nodes. This metric gives a crude indication of the robustness of a network in response to any of the impairments.</p></dd>

				<dt><a name='metrics_numberOfNodes'>Node set size (|N|)</a></dt>
				<dd><p>This metric is equal to the number of nodes in the network.</p></dd>

				<dt><a name='metrics_spectralRadius'>Spectral radius</a></dt>
				<dd><p>This metric is defined as the largest eigenvalue of the adjacency matrix (<i>A<sub>nn</sub></i>), often referred to simply as <i>&lambda;<sub>1</sub></i>, which plays an important role in determining epidemic thresholds, which correlates with the severity of dynamic epidemical (DE) failures on a network. For example, in a <i>Susceptible-Infected-Susceptible</i> (SIS) model, if the infection rate along each link is <i>&beta;</i>, while the cure rate for each node is <i>&delta;</i>, then the effective spreading rate of the virus can be defined as <i>&beta;/&delta;</i>. The epidemic threshold can be defined as follows: for effective spreading rates below <i>&tau;</i> the virus contamination in the network dies out, while for effective spreading rates above <i>&tau;</i>, the virus is prevalent, i.e. a persisting fraction of nodes remains infected. It was shown in [<a href='#epidemicThreshold'>8</a>] that <i>&tau;=1/&lambda;<sub>1</sub></i>. The value of <i>&lambda;<sub>1</sub></i> is upper bounded by the maximum (out) nodal degree.</p><p>For non-bidirectional topologies, it is computed from the symmetric adjacency matrix, defined as <i>A'<sub>nn</sub>=max{A<sub>nn</sub>, A<sup>T</sup><sub>nn</sub>}</i>, where the <i>max</i> operator is applied element-wise.</p></dd>

				<dt><a name='metrics_symmetryRatio'>Symmetry ratio</a></dt>
				<dd><p>This ratio is essentially the quotient between the number of distinct eigenvalues (obtained from the adjacency matrix, or symmetric adjacency matrix for non-bidirectional topologies) of the network and the network diameter. Therefore, on high-symmetry networks, with symmetry values between 1 and 3 (with the network diameter measured in hops), the impact of losing a node does not depend on which node is lost, what means that network performs equally in response to a random (SR) or a target attack (ST).</p></dd>

				</dl>

				<a name='sectionReferences'></a>
				<h1>References</h1>

				<a name='attacksTaxonomy1'></a>
				[1] S. Hansman and R. Hunt, "A taxonomy of network and computer attacks", <i>Computers &amp; Security (COMPSEC)</i>, vol. 24, no. 1, pp. 31-43, 2005.
				<br />
				<a name='attacksTaxonomy2'></a>
				[2] S. Shiva <i>et al.</i>, "AVOIDIT: A cyber attack taxonomy", University of Memphis, Technical Report, 2009.
				<br />
				<a name='wirelessEM'></a>
				[3] S. Tang and B. Mark, "Analysis of Virus Spread in Wireless Sensor Networks: An Epidemic Model", in <i>Proceedings of the 7th International Workshop on Design of Reliable Communication Networks (DRCN 2009)</i>, pp. 86-91, 2009.
				<br />
				<a name='gmplsEM'></a>
				[4] E. Calle <i>et al.</i>, "A Multiple Failure Propagation Model in GMPLS-Based Networks", <i>IEEE Network Magazine</i>, vol. 24, no. 6, pp. 17-22, 2010.
				<br />
				<a name='algebraicConnectivityBounds'></a>
				[5] A. Jamakovic and S. Uhlig, "Influence of the network structure on robustness", in <i>Proceedings of the 15th IEEE International Conference on Networks (ICON 2007)</i>, pp. 278-283, 2007
				<br />
				<a name='laplacianMatrix'></a>
				[6] R. Olfati-Saber and R.M. Murray, "Consensus problems in networks of agents with switching topology and time-delays", <i>IEEE Transactions on Automatic Control</i>, vol. 49, no. 9, pp. 1520-1533, 2004.
				<br />
				<a name='averageNeighborConnectivity'></a>
				[7] P. Mahadevan <i>et al.</i>, "The Internet AS-Level Topology: Three Data Sources and One Definitive Metric", <i>SIGCOMM Computer Communications Review</i>, vol. 36, pp. 17-26, 2006.
				<br />
				<a name='epidemicThreshold'></a>
				[8] Y. Wang <i>et al.</i>, "Epidemic spreading in real networks: An eigenvalue viewpoint", in <i>Proceedings of the 22nd Symposium in Reliable Distributed Computing</i>, pp. 25-34, 2003.

				<a name='sectionCredits'></a>
				<h1>Credits</h1>

				This report has been completed in collaboration with the <a href='http://bcds.udg.edu/'><b>Broadband Network Control and Management and Distributed Systems</b></a> group of the University of Girona, in the framework of the FIERRO project (TEC2010-12250-E).
			</body>
		</html>
	</xsl:template>
</xsl:stylesheet>
