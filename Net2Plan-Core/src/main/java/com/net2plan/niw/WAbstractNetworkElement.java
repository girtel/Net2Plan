/*******************************************************************************
 * This program and the accompanying materials are made available under the terms of the MIT License available at
 * https://opensource.org/licenses/MIT
 *******************************************************************************/

package com.net2plan.niw;

import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkElement;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.niw.WNetConstants.WTYPE;

/**
 * This base class is inherited by all the classes in the library representing relevant network elements. Provides some
 * base implementations for common methods. It wraps an underlying Net2Plan object, providing the baseline support.
 */
public abstract class WAbstractNetworkElement implements Comparable<WAbstractNetworkElement>
{
	public static final String NIWNAMEPREFIX = "$$$";

	/**
	 * The Net2Plan object associated to this library object
	 */
	protected final NetworkElement associatedNpElement;
	private final Optional<Integer> indexIfDummyElement;

	public abstract WTYPE getWType (); 

	protected WAbstractNetworkElement(NetworkElement associatedNpElement , Optional<Integer> indexIfDummyElement)
	{
		// if associated element == null, this is a dummy fiber, used in some graphs
		this.associatedNpElement = associatedNpElement;
		this.indexIfDummyElement = indexIfDummyElement;
		if (this.getWType() != WTYPE.WNet)
		{
			final WTYPE type1 = getNet().getWType(associatedNpElement).orElse(null);
			final WTYPE type2 = this.getWType();
			if (type1 != type2) 
			{
				System.out.println("TYPE ACCORDING TO associtatedElement: " + type1 + " . According to this.getType(): " + type2);
				System.out.println("associatedNpElement: " + associatedNpElement);
			}
			assert type1 == type2;
			
		}
	}

	/**
	 * Returns the underlying Net2Plan object. Users should NOT modify or even read this object, since all manipulations
	 * should be made through the provided methods in he library. This method is just provided for debugging for those users
	 * who really know what they are doing
	 * @return see above
	 */
	public abstract NetworkElement getNe();

	/**
	 * Returns the value of a user-defined attributes
	 * @param name the name of the attribute
	 * @param defaultValue if no attribute with this name exists, returns this default value
	 * @return see above
	 */
	public String getAttributeOrDefault(String name, String defaultValue)
	{
		return associatedNpElement.getAttribute(name, defaultValue);
	}

	/**
	 * Returns the value associated to a user-defined attribute, as a boolean, or a default is the attribute does not exist,
	 * or cannot be parsed to a boolen
	 * @param name the name of the attribute
	 * @param defaultValue see above
	 * @return see above
	 */
	public Boolean getAttributeAsBooleanOrDefault(String name, Boolean defaultValue)
	{
		final String s = associatedNpElement.getAttribute(name);
		if (s == null) return defaultValue;
		try
		{
			return Boolean.parseBoolean(s);
		} catch (Exception ee)
		{
			return defaultValue;
		}
	}


	/**
	 * Adds to this element the given attribute
	 * @param name the attribute name
	 * @param value the value
	 */
	public void setAttribute(String name, String value)
	{
		associatedNpElement.setAttribute(name, value);
	}

	/**
	 * Adds to this element the given attribute
	 * @param name the attribute name
	 * @param value the value
	 */
	public void setAttributeAsBoolean(String name, Boolean value)
	{
		associatedNpElement.setAttribute(name, value.toString());
	}

	/**
	 * Adds to this element the given attribute
	 * @param name the attribute name
	 * @param value the value
	 */
	public void setAttributeAsDouble(String name, Double value)
	{
		associatedNpElement.setAttribute(name, value);
	}

	/**
	 * Gets the double value stored for the given attribute name, or a default value if something fails (no attribute of
	 * such name, or not the appropriate type)
	 * @param name the attribute name
	 * @param defaultValue the default value
	 * @return see above
	 */
	public double getAttributeAsDoubleOrDefault(String name, Double defaultValue)
	{
		return associatedNpElement.getAttributeAsDouble(name, defaultValue);
	}

	public int getAttributeAsIntegerOrDefault(String name, Integer defaultValue)
	{
		final String s = associatedNpElement.getAttribute(name);
		if (s == null) return defaultValue;
		try
		{
			return Integer.parseInt(s);
		} catch (Exception ee)
		{
			return defaultValue;
		}
	}

	public List<Double> getAttributeAsListDoubleOrDefault(String name, List<Double> defaultValue)
	{
		return associatedNpElement.getAttributeAsDoubleList(name, defaultValue);
	}
	public List<String> getAttributeAsListStringOrDefault(String name, List<String> defaultValue)
	{
		return associatedNpElement.getAttributeAsStringList(name, defaultValue);
	}
	public SortedSet<Integer> getAttributeAsSortedSetIntegerOrDefault(String name, SortedSet<Integer> defaultValue)
	{
		final List<Double> res = associatedNpElement.getAttributeAsDoubleList(name, null);
		if (res == null) return defaultValue;
		return res.stream().map(nn -> new Integer(nn.intValue())).collect(Collectors.toCollection(TreeSet::new));
	}

	Optional<NetworkLayer> getIpNpLayer()
	{
		final WLayerIp ipLayer = getNet().getIpLayer().orElse(null);
		return ipLayer == null? Optional.empty() : Optional.of(ipLayer.getNe());
	}

	Optional<NetworkLayer> getWdmNpLayer()
	{
		final WLayerWdm wdmLayer = getNet().getWdmLayer().orElse(null);
		return wdmLayer == null? Optional.empty() : Optional.of(wdmLayer.getNe());
	}

	/**
	 * Returns the underlying net2plan object that is giving support for this network. Regular users should NOT access the
	 * underlying NetPlan objtec in any form. This method is just provided for debugging for those users who really know
	 * what they are doing
	 * @return see above
	 */
	public NetPlan getNetPlan()
	{		
		return associatedNpElement == null? null : associatedNpElement.getNetPlan();
	}

	/**
	 * Returns true if this element was removed from the network
	 * @return see above
	 */
	public boolean wasRemoved()
	{
		return associatedNpElement.wasRemoved();
	}

	/**
	 * Returns an internally generated id associated with this object, that is unique among all network elements of all
	 * types in this network
	 * @return see above
	 */
	public long getId()
	{
		return associatedNpElement.getId();
	}

	/**
	 * Returns the network object that this element belongs to
	 * @return see above
	 */
	public WNet getNet()
	{
		return new WNet(getNetPlan());
	}

	/**
	 * Returns true if this element is an VNF instance
	 * @return see above
	 */
	public boolean isVnfInstance()
	{
		return this instanceof WVnfInstance;
	}

	/**
	 * Returns true if this element is a WDM fiber
	 * @return see above
	 */
	public boolean isFiber()
	{
		return this instanceof WFiber;
	}

	/**
	 * Returns true if this element is an IP link
	 * @return see above
	 */
	public boolean isWIpLink()
	{
		return this instanceof WIpLink;
	}

	/**
	 * Returns true if this element is an IP source routde connection
	 * @return see above
	 */
	public boolean isIpConnection()
	{
		return this instanceof WIpSourceRoutedConnection;
	}


	/**
	 * Returns true if this element is an IP layer
	 * @return see above
	 */
	public boolean isLayerIp()
	{
		return this instanceof WLayerIp;
	}

	/**
	 * Returns true if this element is a WDM layer element
	 * @return see above
	 */
	public boolean isLayerWdm()
	{
		return this instanceof WLayerWdm;
	}

	/**
	 * Returns true if this element is a lightpath request element
	 * @return see above
	 */
	public boolean isLightpathRequest()
	{
		return this instanceof WLightpathRequest;
	}

	/**
	 * Returns true if this element is a lightpath unregenerated (without intermediate regenerations)
	 * @return see above
	 */
	public boolean isLightpath()
	{
		return this instanceof WLightpath;
	}

	/**
	 * Returns true if this element is a network node
	 * @return see above
	 */
	public boolean isNode()
	{
		return this instanceof WNode;
	}

	/**
	 * Returns true if this element is a service chain request
	 * @return see above
	 */
	public boolean isServiceChainRequest()
	{
		return this instanceof WServiceChainRequest;
	}

	/**
	 * Returns true if this element is a unicast IP demand
	 * @return see above
	 */
	public boolean isWIpUnicastDemand()
	{
		return this instanceof WIpUnicastDemand;
	}

	/**
	 * Returns true if this element is a service chain
	 * @return see above
	 */
	public boolean isServiceChain()
	{
		return this instanceof WServiceChain;
	}

	/**
	 * Casts this element to a WFiber object (fails if it is not an object of such type)
	 * @return see above
	 */
	public WFiber getAsFiber()
	{
		return (WFiber) this;
	}

	/**
	 * Casts this element to a WIpLink object (fails if it is not an object of such type)
	 * @return see above
	 */
	public WIpLink getAsIpLink()
	{
		return (WIpLink) this;
	}

	/**
	 * Casts this element to a WIpSourceRoutedConnection object (fails if it is not an object of such type)
	 * @return see above
	 */
	public WIpSourceRoutedConnection getAsIpSourceRoutedConnection()
	{
		return (WIpSourceRoutedConnection) this;
	}


	/**
	 * Casts this element to a WLayerIp object (fails if it is not an object of such type)
	 * @return see above
	 */
	public WLayerIp getAsIpLayer()
	{
		return (WLayerIp) this;
	}

	/**
	 * Casts this element to a WLayerWdm object (fails if it is not an object of such type)
	 * @return see above
	 */
	public WLayerWdm getAsWdmLayer()
	{
		return (WLayerWdm) this;
	}

	/**
	 * Casts this element to a WLightpathRequest object (fails if it is not an object of such type)
	 * @return see above
	 */
	public WLightpathRequest getAsLightpathRequest()
	{
		return (WLightpathRequest) this;
	}

	/**
	 * Casts this element to a WLightpathUnregenerated object (fails if it is not an object of such type)
	 * @return see above
	 */
	public WLightpath getAsLightpath()
	{
		return (WLightpath) this;
	}
	/**
	 * Casts this element to a WSharedRiskGroup object (fails if it is not an object of such type)
	 * @return see above
	 */
	public WSharedRiskGroup getAsSrg()
	{
		return (WSharedRiskGroup) this;
	}

	/**
	 * Casts this element to a WNode object (fails if it is not an object of such type)
	 * @return see above
	 */
	public WNode getAsNode()
	{
		return (WNode) this;
	}

	/**
	 * Casts this element to a WServiceChain object (fails if it is not an object of such type)
	 * @return see above
	 */
	public WServiceChain getAsServiceChain()
	{
		return (WServiceChain) this;
	}

	/**
	 * Casts this element to a WServiceChainRequest object (fails if it is not an object of such type)
	 * @return see above
	 */
	public WServiceChainRequest getAsServiceChainRequest()
	{
		return (WServiceChainRequest) this;
	}

	/**
	 * Casts this element to a WIpUnicastDemand
	 * @return see above
	 */
	public WIpUnicastDemand getAsIpUnicastDemand()
	{
		return (WIpUnicastDemand) this;
	}


	/**
	 * Casts this element to a WVnfInstance object (fails if it is not an object of such type)
	 * @return see above
	 */
	public WVnfInstance getAsVnfInstance()
	{
		return (WVnfInstance) this;
	}

//	@Override
//	public final int hashCode()
//	{
//		final int prime = 31;
//		int result = 1;
//		result = prime * result + ((this.associatedNpElement == null) ? 0 : this.associatedNpElement.hashCode());
//		return result;
//	}
//
//	@Override
//	public final boolean equals(Object obj)
//	{
//		if (this == obj) return true;
//		if (obj == null) return false;
//		if (getClass() != obj.getClass()) return false;
//		WAbstractNetworkElement other = (WAbstractNetworkElement) obj;
//		if (this.associatedNpElement == null)
//		{
//			if (other.associatedNpElement != null) return false;
//			
//			
//			
//		} else if (!this.associatedNpElement.equals(other.associatedNpElement)) return false;
//		return true;
//	}


	
	@Override
	public final int compareTo(WAbstractNetworkElement o)
	{
		if (this.equals(o)) return 0;
		if (o == null) throw new NullPointerException();
		if (o.getNe() == null) return -1;
		if (this.getNe() == null) return 1;
        if (this.getNetPlan() != o.getNetPlan()) {
            System.out.println("N2P this Links: " + this.getNetPlan().getLinks().size());
            System.out.println("N2P O: " + o.getNetPlan());
//            System.out.println("N2P O Links: " + o.getNetPlan().getLinks().size());
//            System.out.println(this.getNetPlan().isDeepCopy(o.getNetPlan()));
        }

        if (this.getNetPlan() != o.getNetPlan()) throw new Net2PlanException("Different netplan object!");
		return Long.compare(this.getId(), o.getId());
	}
	
	@Override
	public final int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.associatedNpElement == null) ? 0 : this.associatedNpElement.hashCode());
		result = prime * result + ((this.indexIfDummyElement == null) ? 0 : this.indexIfDummyElement.hashCode());
		return result;
	}

	@Override
	public final boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		final WAbstractNetworkElement other = (WAbstractNetworkElement) obj;
		if (this.associatedNpElement == null)
		{
			if (other.associatedNpElement != null) return false;
		} else if (!this.associatedNpElement.equals(other.associatedNpElement)) return false;
		if (this.indexIfDummyElement == null)
		{
			if (other.indexIfDummyElement != null) return false;
		} else if (!this.indexIfDummyElement.equals(other.indexIfDummyElement)) return false;
		return true;
	}

	abstract void checkConsistency ();
	
}
