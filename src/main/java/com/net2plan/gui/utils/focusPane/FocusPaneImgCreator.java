package com.net2plan.gui.utils.focusPane;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.Arrays;

import javax.swing.JFrame;

import com.net2plan.interfaces.networkDesign.Demand;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.interfaces.networkDesign.Resource;
import com.net2plan.interfaces.networkDesign.Route;

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

