package com.net2plan.gui.plugins;

import java.awt.Color;

import com.net2plan.interfaces.networkDesign.Demand;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.MulticastDemand;
import com.net2plan.interfaces.networkDesign.MulticastTree;
import com.net2plan.interfaces.networkDesign.NetworkElement;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.interfaces.networkDesign.Resource;
import com.net2plan.interfaces.networkDesign.Route;
import com.net2plan.interfaces.networkDesign.SharedRiskGroup;
import com.net2plan.internal.Constants.NetworkElementType;
import com.net2plan.utils.Pair;

public class GUINetworkDesignConstants
{

    public enum AJTableType
    {
    	NODES("Nodes" , NetworkElementType.NODE),
    	LINKS("Links" , NetworkElementType.LINK),
    	ROUTES("Routes" , NetworkElementType.ROUTE),
    	LAYERS ("Layers" , NetworkElementType.LAYER),
    	FORWARDINGRULES ("Forw. rules" , NetworkElementType.FORWARDING_RULE),
    	DEMANDS ("Demands" , NetworkElementType.DEMAND),
    	MULTICAST_DEMANDS ("Multicast demands" , NetworkElementType.MULTICAST_DEMAND),
    	MULTICAST_TREES ("Multicast trees" , NetworkElementType.MULTICAST_TREE),
    	RESOURCES ("Resources" , NetworkElementType.RESOURCE),
    	SRGS ("SRGs" , NetworkElementType.SRG);
        private final String tabName;
        private final NetworkElementType neType;
        
        private AJTableType(String tabName ,  NetworkElementType neType)
        {
            this.tabName = tabName; this.neType = neType;
        }

        public String getTabName()
        {
            return tabName;
        }

        public boolean isElementThatCanBeHidenInGui  () 
        {
            return this == NODES || this == LINKS; 
        }

        public NetworkElementType getNeType () { return neType; }
        
        public static AJTableType getTypeOfElement(Object e)
        {
            if (e instanceof Node) return AJTableType.NODES;
            if (e instanceof Link) return AJTableType.LINKS;
            if (e instanceof Route) return AJTableType.ROUTES;
            if (e instanceof NetworkLayer) return AJTableType.LAYERS;
            if (e instanceof Pair) return AJTableType.FORWARDINGRULES;
            if (e instanceof Demand) return AJTableType.DEMANDS;
            if (e instanceof MulticastDemand) return AJTableType.MULTICAST_DEMANDS;
            if (e instanceof MulticastTree) return AJTableType.MULTICAST_TREES;
            if (e instanceof Resource) return AJTableType.RESOURCES;
            if (e instanceof SharedRiskGroup) return AJTableType.SRGS;
            return null;
        }
        public static AJTableType getTypeOfElement(NetworkElementType elType )
        {
        	switch (elType)
        	{
			case DEMAND: return AJTableType.DEMANDS;
			case FORWARDING_RULE: return AJTableType.FORWARDINGRULES;
			case LAYER: return AJTableType.LAYERS;
			case LINK: return AJTableType.LINKS;
			case MULTICAST_DEMAND: return AJTableType.MULTICAST_DEMANDS;
			case MULTICAST_TREE: return AJTableType.MULTICAST_TREES;
			case NETWORK: return null;
			case NODE: return AJTableType.NODES;
			case RESOURCE: return AJTableType.RESOURCES;
			case ROUTE: return AJTableType.ROUTES;
			case SRG: return AJTableType.SRGS;
			default:
				break;
        	}
            return null;
        }
    }    
    
    public static Color YELLOW_BRANCH_COLOR = Color.YELLOW;
    public final static int PICKMANAGER_DEFAULTNUMBERELEMENTSPICKMEMORY = 10;
}
