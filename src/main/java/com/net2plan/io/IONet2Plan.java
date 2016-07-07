/*******************************************************************************
 * Copyright (c) 2015 Pablo Pavon Mariño.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 * Contributors:
 *     Pablo Pavon Mariño - initial API and implementation
 ******************************************************************************/



 




package com.net2plan.io;

import com.net2plan.internal.plugins.IOFilter;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.internal.Constants.IOFeature;
import com.net2plan.utils.Triple;
import java.io.File;
import java.util.EnumSet;
import java.util.List;

/**
 * Default IO filter for Net2Plan.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.2
 */
public class IONet2Plan extends IOFilter
{
	private final static String title = "Net2Plan";
	
	/**
	 * Default constructor.
	 *
	 * @since 0.2.2
	 */
	public IONet2Plan()
	{
		super(title, EnumSet.allOf(IOFeature.class), "n2p");
	}

	@Override
	public String getName()
	{
		return title + " import/export filter";
	}

	@Override
	public List<Triple<String, String, String>> getParameters()
	{
		return null;
	}
	
	@Override
	public NetPlan readDemandSetFromFile(File file)
	{
		NetPlan netPlan = readFromFile(file);
		for (NetworkLayer layer : netPlan.getNetworkLayers ())
			if (!layer.equals(netPlan.getNetworkLayerDefault()))
					netPlan.removeNetworkLayer (layer);
		netPlan.removeAllLinks();
		netPlan.removeAllUnicastRoutingInformation();
		netPlan.removeAllSRGs();
		
		return netPlan;
	}
	
	@Override
	public NetPlan readFromFile(File file)
	{
		return new NetPlan(file);
	}

	@Override
	public void saveDemandSetToFile(NetPlan netPlan, File file)
	{
		for (NetworkLayer layer : netPlan.getNetworkLayers ())
			if (!layer.equals(netPlan.getNetworkLayerDefault()))
					netPlan.removeNetworkLayer (layer);
		netPlan.removeAllLinks();
		netPlan.removeAllUnicastRoutingInformation();
		netPlan.removeAllSRGs();
		netPlan.saveToFile(file);
	}

	@Override
	public void saveToFile(NetPlan netPlan, File file)
	{
		netPlan.saveToFile(file);
	}
}
