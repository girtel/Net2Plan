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
package com.net2plan.gui.plugins.networkDesign.focusPane;

import com.net2plan.interfaces.networkDesign.*;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

public class FocusPaneImgCreator {

	public static void main(String[] args) throws Exception 
	{
    	final NetPlan np = new NetPlan ();
    	final Node n1 = np.addNode(0 , 0 , "name1" , null);
    	final Node n2 = np.addNode(0 , 0 , "name2" , null);
    	final Node n3 = np.addNode(0 , 0 , "name3" , null);
    	final Link e12 = np.addLink(n1,n2,3,100,200000,null);
    	final Link e23 = np.addLink(n2,n3,3,100,200000,null);
    	final Resource r1 = np.addResource("CPU" , "nameRes1" , n1 , 10 , "Mbps" , null , 5 , null);
    	final Resource r2 = np.addResource("RAM" , "nameRes2" , n2 , 10 , "Mbps" , null , 5 , null);
    	final Demand d = np.addDemand(n1 , n3 , 0 , null);
    	d.setServiceChainSequenceOfTraversedResourceTypes(Arrays.asList("CPU" , "RAM"));
    	final Route r = np.addServiceChain(d , 2.0 , Arrays.asList(1.0,2.0,3.0,4.0) , Arrays.asList(r1,e12,r2,e23) , null);
    	final NetworkLayer layer = np.getNetworkLayerDefault();
    	layer.setName("WDM");
		
		JFrame frame = new JFrame("Example");
		frame.getContentPane().setLayout(new FlowLayout());
		frame.setPreferredSize(new Dimension (600,600));

		//frame.add(new FigureLinkSequencePanel(r.getPath() , layer , r.getSeqOccupiedCapacitiesIfNotFailing() , "Route " + r.getIndex() , r.getCarriedTraffic()));

		frame.pack();
	    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	    frame.setVisible(true);   
	}
	
}

