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

package com.net2plan.research.metrohaul.networkModel;

import java.util.List;

import com.net2plan.interfaces.networkDesign.Net2PlanException;

/** This class represents information of a user service that can be defined in the network, and used to create service chain requests.
 * Users services are bidirectional. Upstream traffic originates in the user, and ends in the network, and downstream 
 * traffic ends in the user, and is originated in the network. 
 * the characteristics of upstream and downstream traffic can be defined differently. 
 * The information comprised is:
 * <ul>
 * <li> Upstream: The sequence of types of VNF that the upstream traffic should traverse </li>
 * <li> Downstream: The sequence of types of VNF that the downstream traffic should traverse </li>
 * <li> Upstream: The expansion factor when traversing each upstream VNF: the ratio between the traffic injected by the user in the origin node, 
 * and the traffic at the OUTPUT of the VNF.</li>
 * <li> Downstream: The expansion factor when traversing each downstream VNF: the ratio between the traffic injected by the origin network node,  
 * and the traffic at the OUTPUT of the VNF.</li>
 * <li> Upstream: The maximum acceptable latency from the origin node of the upstream flow, to the input of each VNF, and the maximum latency from the 
 * origin node of the flow to the end node of the flow  </li>
 * <li> Downstream: The maximum acceptable latency from the origin node of the downstream flow, to the input of each VNF, and the maximum latency from the 
 * origin node of the downstream flow to the end node of the downstream flow  </li>
 * <li> Downstream: The expansion factor for the initial downstream traffic. This is, the amount of traffic requested to come in downstream direction, 
 * for each unit requested in upstream direction </li>
 * <li>An indication if the user service should end in a core node. If true, the end nodes of the upstream and the origin nodes of the downstream are constrained 
 * to be nodes connected to the core. If not, end nodes of upstream or origin nodes of downstream can be any node in the network. </li>
 * <li> Arbitrary: Users services can be attached an arbitrary user-defined string</li>
 * </ul>
 *
 */
public class WUserService
{
	final private String userServiceUniqueId;
	final private List<String> listVnfTypesToTraverseUpstream;
	final private List<String> listVnfTypesToTraverseDownstream;
	final private List<Double> sequenceTrafficExpansionFactorsRespectToBaseTrafficUpstream;
	final private List<Double> sequenceTrafficExpansionFactorsRespectToBaseTrafficDownstream;
	final private List<Double> listMaxLatencyFromInitialToVnfStartAndToEndNode_ms_upstream; 
	final private List<Double> listMaxLatencyFromInitialToVnfStartAndToEndNode_ms_downstream; 
	final private double injectionDownstreamExpansionFactorRespecToBaseTrafficUpstream;
	final private boolean isEndingInCoreNode;
	private String arbitraryParamString;
	public WUserService(String userServiceUniqueId, List<String> listVnfTypesToTraverseUpstream,
			List<String> listVnfTypesToTraverseDownstream,
			List<Double> sequenceTrafficExpansionFactorsRespectToBaseTrafficUpstream,
			List<Double> sequenceTrafficExpansionFactorsRespectToBaseTrafficDownstream,
			List<Double> listMaxLatencyFromInitialToVnfStart_ms_upstream,
			List<Double> listMaxLatencyFromInitialToVnfStart_ms_downstream,
			double injectionDownstreamExpansionFactorRespecToBaseTrafficUpstream, boolean isEndingInCoreNode,
			String arbitraryParamString)
	{
		super();
		if (userServiceUniqueId.contains(WNetConstants.LISTSEPARATORANDINVALIDNAMECHARACTER)) throw new Net2PlanException("Names cannot contain the character: " + WNetConstants.LISTSEPARATORANDINVALIDNAMECHARACTER);  
		this.userServiceUniqueId = userServiceUniqueId;
		this.listVnfTypesToTraverseUpstream = listVnfTypesToTraverseUpstream;
		this.listVnfTypesToTraverseDownstream = listVnfTypesToTraverseDownstream;
		this.sequenceTrafficExpansionFactorsRespectToBaseTrafficUpstream = sequenceTrafficExpansionFactorsRespectToBaseTrafficUpstream;
		this.sequenceTrafficExpansionFactorsRespectToBaseTrafficDownstream = sequenceTrafficExpansionFactorsRespectToBaseTrafficDownstream;
		this.listMaxLatencyFromInitialToVnfStartAndToEndNode_ms_upstream = listMaxLatencyFromInitialToVnfStart_ms_upstream;
		this.listMaxLatencyFromInitialToVnfStartAndToEndNode_ms_downstream = listMaxLatencyFromInitialToVnfStart_ms_downstream;
		this.injectionDownstreamExpansionFactorRespecToBaseTrafficUpstream = injectionDownstreamExpansionFactorRespecToBaseTrafficUpstream;
		this.isEndingInCoreNode = isEndingInCoreNode;
		this.arbitraryParamString = arbitraryParamString;
	}
	/** Returns the arbitrary user-defined parameters string
	 * @return see above
	 */
	public String getArbitraryParamString()
	{
		return arbitraryParamString;
	}
	/** Sets the arbitrary user-defined parameters string
	 * @param arbitraryParamString see above
	 */
	public void setArbitraryParamString(String arbitraryParamString)
	{
		this.arbitraryParamString = arbitraryParamString;
	}
	/** Returns the user service name, that works as a unique identifier
	 * @return see above
	 */
	public String getUserServiceUniqueId()
	{
		return userServiceUniqueId;
	}
	/** Gets the list of VNF types to traverse by the upstream traffic
	 * @return see above
	 */
	public List<String> getListVnfTypesToTraverseUpstream()
	{
		return listVnfTypesToTraverseUpstream;
	}
	/** Gets the list of VNF types to traverse by the downstream traffic
	 * @return see above
	 */
	public List<String> getListVnfTypesToTraverseDownstream()
	{
		return listVnfTypesToTraverseDownstream;
	}
	/** Gets the traffic expansion factors of the upstream traffic. This is a list with as many elements as the number 
	 * of VNFs to traverse, with the ratio between the output traffic of each VNF, respect to the input traffic at the stream ORIGIN node.
	 * @return see above
	 */
	public List<Double> getSequenceTrafficExpansionFactorsRespectToBaseTrafficUpstream()
	{
		return sequenceTrafficExpansionFactorsRespectToBaseTrafficUpstream;
	}
	/** Gets the traffic expansion factors of the downstream traffic. This is a list with as many elements as the number 
	 * of VNFs to traverse, with the ratio between the output traffic of each VNF, respect to the input traffic at the stream ORIGIN node.
	 * @return see above
	 */
	public List<Double> getSequenceTrafficExpansionFactorsRespectToBaseTrafficDownstream()
	{
		return sequenceTrafficExpansionFactorsRespectToBaseTrafficDownstream;
	}
	/** Gets the list of maximum acceptable latency limits in ms, for the upstream traffic. This is a list with as many elements 
	 * as the number of VNFs to traverse by the stream plus one. First elements correspond to the maximum latency from the stream 
	 * origin node to the i-th VNF input. The last element of the list is the maximum traffic from the stream origin node to the 
	 * stream destination node.
	 * @return see above
	 */
	public List<Double> getListMaxLatencyFromInitialToVnfStart_ms_upstream()
	{
		return listMaxLatencyFromInitialToVnfStartAndToEndNode_ms_upstream;
	}
	/** Gets the list of maximum acceptable latency limits in ms, for the downtream traffic. This is a list with as many elements 
	 * as the number of VNFs to traverse by the stream plus one. First elements correspond to the maximum latency from the stream 
	 * origin node to the i-th VNF input. The last element of the list is the maximum traffic from the stream origin node to the 
	 * stream destination node.
	 * @return see above
	 */
	public List<Double> getListMaxLatencyFromInitialToVnfStart_ms_downstream()
	{
		return listMaxLatencyFromInitialToVnfStartAndToEndNode_ms_downstream;
	}
	/** Returns the expansion factor for the initial downstream traffic. This is, the amount of traffic requested to come in downstream direction, 
	 * 	for each unit requested in upstream direction 
	 * @return see above
	 */
	public double getInjectionDownstreamExpansionFactorRespecToBaseTrafficUpstream()
	{
		return injectionDownstreamExpansionFactorRespecToBaseTrafficUpstream;
	}
	/** Indicates if this user service is expected to finalize in a core node
	 * @return see above
	 */
	public boolean isEndingInCoreNode()
	{
		return isEndingInCoreNode;
	}
	
	public String toString () { return "UserService(" + this.getUserServiceUniqueId() +")"; }
	
}
