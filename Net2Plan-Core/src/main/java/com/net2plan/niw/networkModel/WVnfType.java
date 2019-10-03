/*******************************************************************************
 * This program and the accompanying materials are made available under the terms of the MIT License available at
 * https://opensource.org/licenses/MIT
 *******************************************************************************/

package com.net2plan.niw.networkModel;

import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

import com.net2plan.interfaces.networkDesign.Net2PlanException;

/**
 * This class represents the information of a type of VNF defined for the network. This is used when a VNF is
 * instantiated in a node, since instantiated VNFs must be of a previously defined type.
 */
public class WVnfType implements Comparable<WVnfType>
{
	final private String vnfTypeName;
	final private double processingTime_ms;
	final private double maxInputTrafficPerVnfInstance_Gbps;
	final private double occupCpu, occupRamGB, occupHdGB;
	final private boolean isConstrainedToBeInstantiatedOnlyInUserDefinedNodes;
	final private SortedSet<String> validMetroNodesForInstantiation;
	private String arbitraryParamString;

	/**
	 * Creates a VNF type with the provided information
	 * @param vnfTypeName see above
	 * @param maxInputTrafficPerVnfInstance_Gbps see above
	 * @param occupCpu see above
	 * @param occupRam see above
	 * @param occupHd see above
	 * @param processingTime_ms see above
	 * @param validMetroNodesForInstantiation see above
	 * @param arbitraryParamString see above
	 */
	public WVnfType(String vnfTypeName, double maxInputTrafficPerVnfInstance_Gbps, double occupCpu, double occupRam, double occupHd, double processingTime_ms, Optional<SortedSet<String>> validMetroNodesForInstantiation, String arbitraryParamString)
	{
		super();
		if (vnfTypeName.contains(WNetConstants.LISTSEPARATORANDINVALIDNAMECHARACTER)) throw new Net2PlanException("Names cannot contain the character: " + WNetConstants.LISTSEPARATORANDINVALIDNAMECHARACTER);
		this.vnfTypeName = vnfTypeName;
		this.maxInputTrafficPerVnfInstance_Gbps = maxInputTrafficPerVnfInstance_Gbps;
		this.occupCpu = occupCpu;
		this.occupRamGB = occupRam;
		this.occupHdGB = occupHd;
		this.processingTime_ms = processingTime_ms;
		this.isConstrainedToBeInstantiatedOnlyInUserDefinedNodes = validMetroNodesForInstantiation.isPresent();
		this.validMetroNodesForInstantiation = validMetroNodesForInstantiation.isPresent() ? validMetroNodesForInstantiation.get() : new TreeSet<>();
		this.arbitraryParamString = arbitraryParamString;
	}

	/**
	 * Returns the arbitrary user-defined string attached to this VNF type
	 * @return see above
	 */
	public String getArbitraryParamString()
	{
		return arbitraryParamString == null ? "" : arbitraryParamString;
	}

	/**
	 * Sets the arbitrary user-defined string attached to this VNF type
	 * @param arbitraryParamString see above
	 */
	public void setArbitraryParamString(String arbitraryParamString)
	{
		if (arbitraryParamString == null) throw new Net2PlanException("Param string cannot be null");
		this.arbitraryParamString = arbitraryParamString;
	}

	/**
	 * Returns the type name
	 * @return see above
	 */
	public String getVnfTypeName()
	{
		return vnfTypeName;
	}

	/**
	 * Returns the maximum input traffic per VNF instance in Gbps
	 * @return see above
	 */
	public double getMaxInputTrafficPerVnfInstance_Gbps()
	{
		return maxInputTrafficPerVnfInstance_Gbps;
	}

	/**
	 * Returns the CPU occupation of each VNF instance of this type
	 * @return see above
	 */
	public double getOccupCpu()
	{
		return occupCpu;
	}

	/**
	 * Returns the RAM occupation of each VNF instance of this type in giga bytes
	 * @return see above
	 */
	public double getOccupRamGBytes()
	{
		return occupRamGB;
	}

	/**
	 * Returns the hard disk occupation of each VNF instance of this type in giga bytes
	 * @return see above
	 */
	public double getOccupHdGBytes()
	{
		return occupHdGB;
	}

	/**
	 * Returns the processing time added to the traffic traversing VNF instances of this type
	 * @return see above
	 */
	public double getProcessingTime_ms()
	{
		return this.processingTime_ms;
	}

	/**
	 * Indicates if this VNF type is constrained so instances can only be instantiated in some user-defined nodes
	 * @return see above
	 */
	public boolean isConstrainedToBeInstantiatedOnlyInUserDefinedNodes()
	{
		return isConstrainedToBeInstantiatedOnlyInUserDefinedNodes;
	}

	/**
	 * Returns the user-defined set of node names, so that instances of this VNF type can only be instantiated in those
	 * nodes (applicable only when constrained instantiation is activated for this VNF type)
	 * @return see above
	 */
	public SortedSet<String> getValidMetroNodesForInstantiation()
	{
		return validMetroNodesForInstantiation;
	}

	@Override
	public String toString()
	{
		return "VnfType(" + this.getVnfTypeName() + ")";
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((vnfTypeName == null) ? 0 : vnfTypeName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		WVnfType other = (WVnfType) obj;
		if (vnfTypeName == null)
		{
			if (other.vnfTypeName != null) return false;
		} else if (!vnfTypeName.equals(other.vnfTypeName)) return false;
		return true;
	}

	@Override
	public final int compareTo(WVnfType o)
	{
		if (this.equals(o)) return 0;
		if (o == null) throw new NullPointerException();
		return this.vnfTypeName.compareTo(o.vnfTypeName);
	}

}
