package com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.treeView;

/**
 * @author Jorge San Emeterio
 * @date 7/14/17
 */
class NetPlanTreeBuilder
{
//    private final GUINetworkDesign callback;
//    private final NetPlanTreeView treeView;
//
//    NetPlanTreeBuilder(GUINetworkDesign callback, NetPlanTreeView treeView)
//    {
//        this.callback = callback;
//        this.treeView = treeView;
//    }
//
//    void buildNetPlanLeaf(TreeNode netPlanNode)
//    {
////        if (netPlanNode.getValueAt(COLUMN_TYPE) != EntryType.NETPLAN)
////            throw new IllegalArgumentException("Given node is not a NetPlan node");
//
//        final Mtn mtn = callback.getDesign();
//
//        switch (treeView.getTreeDirection())
//        {
//            case DOWNWARDS:
//                // Internal Nodes
//                TreeNode internalNodeExpandNode = this.buildTreeNode("Internal IP Nodes", "", "", EntryType.GENERIC);
//                treeView.addChildTo(netPlanNode, internalNodeExpandNode);
//
//                for (Node node : callback.getMla().getAllNodes(NODETYPE.IPROUTER_INTERNAL))
//                {
//                    TreeNode internalNode = this.buildTreeNode(node);
//
//                    treeView.addChildTo(internalNodeExpandNode, internalNode);
//                }
//
//                // External Nodes
//                TreeNode externalNodeExpandNode = this.buildTreeNode("External IP Nodes", "", "", EntryType.GENERIC);
//                treeView.addChildTo(netPlanNode, externalNodeExpandNode);
//
//                for (Node node : callback.getMla().getAllNodes(NODETYPE.IPROUTER_EXTERNAL))
//                {
//                    TreeNode externalNode = this.buildTreeNode(node);
//
//                    treeView.addChildTo(externalNodeExpandNode, externalNode);
//                }
//
//                // Demands
//                TreeNode demandExpandNode = this.buildTreeNode("IP Demands", "", "", EntryType.GENERIC);
//                treeView.addChildTo(netPlanNode, demandExpandNode);
//
//                for (Demand demand : mla.getWrapperIp().getAllPureIpDemands_privateAndNotPrivate())
//                {
//                    TreeNode demandNode = this.buildTreeNode(demand);
//                    treeView.addChildTo(demandExpandNode, demandNode);
//                }
//
//                // Links
//                TreeNode linkExpandNode = this.buildTreeNode("IP Links", "", "", EntryType.GENERIC);
//                treeView.addChildTo(netPlanNode, linkExpandNode);
//
//                for (Link link : callback.getMla().getAllIpLinks())
//                {
//                    TreeNode linkNode = this.buildTreeNode(link);
//                    treeView.addChildTo(linkExpandNode, linkNode);
//                }
//                break;
//        }
//    }
//
//    void buildIPNodeLeaf(TreeNode IPNodeTreeNode)
//    {
//        if (IPNodeTreeNode.getValueAt(COLUMN_TYPE) != EntryType.IP_NODE)
//            throw new IllegalArgumentException("Given node is not an IP node leaf");
//
//        final NetPlan netPlan = callback.getDesign();
//        final Node node = netPlan.getNodeFromId((Long) IPNodeTreeNode.getValueAt(COLUMN_ID));
//
//        assert node != null;
//
//        switch (treeView.getTreeDirection())
//        {
//            case DOWNWARDS:
//                // Info
//                final Set<Link> incomingLinks = node.getIncomingLinks(callback.getMla().getIpLayer());
//                final Set<Link> outgoingLinks = node.getOutgoingLinks(callback.getMla().getIpLayer());
//
//                final Set<Demand> incomingDemands = node.getIncomingDemands(callback.getMla().getIpLayer());
//                final Set<Demand> outgoingDemands = node.getOutgoingDemands(callback.getMla().getIpLayer());
//
//                TreeNode incomingLinksExpandNode = this.buildTreeNode("Incoming IP Links", "", "", EntryType.GENERIC);
//                treeView.addChildTo(IPNodeTreeNode, incomingLinksExpandNode);
//
//                for (Link incomingLink : incomingLinks)
//                {
//                    if (!WrapperIPLayer.isLagMember(incomingLink))
//                    {
//                        TreeNode linkNode = this.buildTreeNode(incomingLink);
//                        treeView.addChildTo(incomingLinksExpandNode, linkNode);
//                    }
//
//                }
//
//                TreeNode outgoingLinksExpandNode = this.buildTreeNode("Outgoing IP Links", "", "", EntryType.GENERIC);
//                treeView.addChildTo(IPNodeTreeNode, outgoingLinksExpandNode);
//
//                for (Link outgoingLink : outgoingLinks)
//                {
//                    if (!WrapperIPLayer.isLagMember(outgoingLink))
//                    {
//                        TreeNode linkNode = this.buildTreeNode(outgoingLink);
//                        treeView.addChildTo(outgoingLinksExpandNode, linkNode);
//                    }
//                }
//
//                TreeNode incomingDemandsExpandNode = this.buildTreeNode("Incoming IP Demands", "", "", EntryType.GENERIC);
//                treeView.addChildTo(IPNodeTreeNode, incomingDemandsExpandNode);
//
//                for (Demand incomingDemand : incomingDemands)
//                {
//                    // Demand tree node
//                    TreeNode demandNode = this.buildTreeNode(incomingDemand);
//                    treeView.addChildTo(incomingDemandsExpandNode, demandNode);
//                }
//
//                TreeNode outgoingDemandsExpandNode = this.buildTreeNode("Outgoing IP Demands", "", "", EntryType.GENERIC);
//                treeView.addChildTo(IPNodeTreeNode, outgoingDemandsExpandNode);
//
//                for (Demand outgoingDemand : outgoingDemands)
//                {
//                    // Demand tree node
//                    TreeNode demandNode = this.buildTreeNode(outgoingDemand);
//                    treeView.addChildTo(outgoingDemandsExpandNode, demandNode);
//                }
//
//                break;
//            case UPWARDS:
//                TreeNode netPlanNode = this.buildTreeNode(netPlan);
//                treeView.addChildTo(IPNodeTreeNode, netPlanNode);
//
//                break;
//        }
//    }
//
//    void buildOTNNodeLeaf(TreeNode otnNode)
//    {
//        if (otnNode.getValueAt(COLUMN_TYPE) != EntryType.OTN_NODE)
//            throw new IllegalArgumentException("Given node is not a OTN node leaf");
//
//        final NetPlan netPlan = callback.getDesign();
//        final Node node = netPlan.getNodeFromId((Long) otnNode.getValueAt(COLUMN_ID));
//
//        assert node != null;
//
//        switch (treeView.getTreeDirection())
//        {
//            case DOWNWARDS:
//                final Set<Link> incomingOtus = node.getIncomingLinks(callback.getMla().getOtuLayer());
//                final Set<Link> outgoingOtus = node.getOutgoingLinks(callback.getMla().getOtuLayer());
//
//                TreeNode incomingOtusExpandNode = this.buildTreeNode("Incoming OTUs", "", "", EntryType.GENERIC);
//                treeView.addChildTo(otnNode, incomingOtusExpandNode);
//
//                for (Link otu : incomingOtus)
//                {
//                    TreeNode otuLinkNode = this.buildTreeNode(otu);
//                    treeView.addChildTo(incomingOtusExpandNode, otuLinkNode);
//                }
//
//                TreeNode outgoingOtusExpandNode = this.buildTreeNode("Outgoing OTUs", "", "", EntryType.GENERIC);
//                treeView.addChildTo(otnNode, outgoingOtusExpandNode);
//
//                for (Link otu : outgoingOtus)
//                {
//                    TreeNode otuLinkNode = this.buildTreeNode(otu);
//                    treeView.addChildTo(outgoingOtusExpandNode, otuLinkNode);
//                }
//                break;
//            case UPWARDS:
//                treeView.addChildTo(otnNode, this.buildTreeNode(netPlan));
//                break;
//        }
//    }
//
//    void buildIPDemandLeaf(TreeNode demandNode)
//    {
//        if (demandNode.getValueAt(COLUMN_TYPE) != EntryType.IP_DEMAND)
//            throw new IllegalArgumentException("Given node is not an IP demand node");
//
//        final NetPlan netPlan = callback.getDesign();
//        final Demand demand = netPlan.getDemandFromId((Long) demandNode.getValueAt(COLUMN_ID));
//
//        assert demand != null;
//
//        final Node ingressNode = demand.getIngressNode();
//        final Node egressNode = demand.getEgressNode();
//
//        // Demand tree node children
//
//        // Nodes
//        treeView.addChildTo(demandNode, this.buildTreeNode(ingressNode));
//        treeView.addChildTo(demandNode, this.buildTreeNode(egressNode));
//
//        switch (treeView.getTreeDirection())
//        {
//            case DOWNWARDS:
//
//                // Adding IP Link
//                TreeNode linkExpandNode = this.buildTreeNode("Traversed IP Links", "", "", EntryType.GENERIC);
//                treeView.addChildTo(demandNode, linkExpandNode);
//
//                for (Link link : demand.getLinksWithNonZeroForwardingRules())
//                {
//                    TreeNode linkNode = this.buildTreeNode(link);
//                    treeView.addChildTo(linkExpandNode, linkNode);
//                }
//                break;
//            case UPWARDS:
//                TreeNode netPlanNode = this.buildTreeNode(netPlan);
//                treeView.addChildTo(demandNode, netPlanNode);
//
//                break;
//        }
//    }
//
//    void buildIPLinkLeaf(TreeNode linkNode)
//    {
//        if (linkNode.getValueAt(COLUMN_TYPE) != EntryType.IP_LINK)
//            throw new IllegalArgumentException("Given node is not an IP link node");
//
//        final NetPlan netPlan = callback.getDesign();
//        final Link link = netPlan.getLinkFromId((Long) linkNode.getValueAt(COLUMN_ID));
//
//        final WrapperIPLayer wrapperIP = callback.getMla().getWrapperIp();
//        final WrapperMplsTe wrapperMplsTe = wrapperIP.getWrapperMplsTe();
//
//        assert link != null;
//
//        // Link Info
//        final Node originNode = link.getOriginNode();
//        final Node destinationNode = link.getDestinationNode();
//
//        // Link tree node children
//        treeView.addChildTo(linkNode, this.buildTreeNode(originNode));
//        treeView.addChildTo(linkNode, this.buildTreeNode(destinationNode));
//
//        // Nodes
//        switch (treeView.getTreeDirection())
//        {
//            case DOWNWARDS:
//            {
//                if (wrapperIP.isLagBundle(link))
//                {
//                    // LAG members
//                    TreeNode memberExpandNode = this.buildTreeNode("LAG members", "", "", EntryType.GENERIC);
//                    treeView.addChildTo(linkNode, memberExpandNode);
//
//                    for (Link member : wrapperIP.getLagMembers(link))
//                    {
//                        TreeNode memberNode = this.buildTreeNode(member);
//                        treeView.addChildTo(memberExpandNode, memberNode);
//                    }
//                } else
//                {
//                    // Link routes
//                    Demand coupledDemand = link.getCoupledDemand();
//                    if (coupledDemand != null)
//                    {
//                        for (Route route : coupledDemand.getRoutes())
//                        {
//                            TreeNode routeNode = this.buildTreeNode(route);
//                            treeView.addChildTo(linkNode, routeNode);
//                        }
//                    }
//                }
//                break;
//            }
//            case UPWARDS:
//            {
//                if (wrapperIP.isLagMember(link))
//                {
//                    TreeNode bundleNode = this.buildTreeNode(wrapperIP.getLagBundle(link));
//                    treeView.addChildTo(linkNode, bundleNode);
//
//                    break;
//                } else
//                {
//                    // Normal link / LAG bundle
//                    final Set<Demand> ipDemands = link.getDemandsWithNonZeroForwardingRules();
//
//                    TreeNode ipDemandExpandNode = this.buildTreeNode("Traversing IP demands", "", "", EntryType.GENERIC);
//                    treeView.addChildTo(linkNode, ipDemandExpandNode);
//
//                    for (Demand ipDemand : ipDemands)
//                        treeView.addChildTo(ipDemandExpandNode, this.buildTreeNode(ipDemand));
//                }
//            }
//        }
//    }
//
//    void buildODULeaf(TreeNode oduNode)
//    {
//        if (oduNode.getValueAt(COLUMN_TYPE) != EntryType.ODU)
//            throw new IllegalArgumentException("Given node is not an ODU leaf");
//
//        final NetPlan netPlan = callback.getDesign();
//        final Route oduRoute = netPlan.getRouteFromId((Long) oduNode.getValueAt(COLUMN_ID));
//
//        assert oduRoute != null;
//
//        // ODU Tree node children
//
//        switch (treeView.getTreeDirection())
//        {
//            case DOWNWARDS:
//            {
//                TreeNode otuExpandNode = this.buildTreeNode("Traversed OTUs", "", "", EntryType.GENERIC);
//                treeView.addChildTo(oduNode, otuExpandNode);
//
//                for (Link link : oduRoute.getSeqLinks())
//                {
//                    TreeNode otuNode = this.buildTreeNode(link);
//                    treeView.addChildTo(otuExpandNode, otuNode);
//                }
//
//                break;
//            }
//            case UPWARDS:
//            {
//                final Link ipLink = oduRoute.getDemand().getCoupledLink();
//                treeView.addChildTo(oduNode, this.buildTreeNode(ipLink));
//
//                break;
//            }
//        }
//    }
//
//    void buildOTULeaf(TreeNode otuNode)
//    {
//        if (otuNode.getValueAt(COLUMN_TYPE) != EntryType.OTU)
//            throw new IllegalArgumentException("Given node is not a OTU node");
//
//        final NetPlan netPlan = callback.getDesign();
//        final Link otuLink = netPlan.getLinkFromId((Long) otuNode.getValueAt(COLUMN_ID));
//
//        assert otuLink != null;
//
//        // OTU Info
//        final Node originNode = otuLink.getOriginNode();
//        final Node destinationNode = otuLink.getDestinationNode();
//
//        // OTU Tree node children
//
//        // Nodes
//        treeView.addChildTo(otuNode, this.buildTreeNode(originNode));
//        treeView.addChildTo(otuNode, this.buildTreeNode(destinationNode));
//
//        switch (treeView.getTreeDirection())
//        {
//            case DOWNWARDS:
//            {
//                Demand coupledDemand = otuLink.getCoupledDemand();
//                if (coupledDemand != null)
//                {
//                    for (Route lightPath : coupledDemand.getRoutes())
//                    {
//                        TreeNode lightPathNode = this.buildTreeNode(lightPath);
//                        treeView.addChildTo(otuNode, lightPathNode);
//                    }
//                }
//                break;
//            }
//            case UPWARDS:
//            {
//                TreeNode oduExpandNode = this.buildTreeNode("Traversing ODUs", "", "", EntryType.GENERIC);
//                treeView.addChildTo(otuNode, oduExpandNode);
//
//                final Set<Route> traversingRoutes = otuLink.getTraversingRoutes();
//
//                for (Route oduRoute : traversingRoutes)
//                    treeView.addChildTo(oduExpandNode, this.buildTreeNode(oduRoute));
//
//                break;
//            }
//        }
//    }
//
//    void buildMulticastFlowLeaf(TreeNode multicastFlowNode)
//    {
//        if (multicastFlowNode.getValueAt(COLUMN_TYPE) != EntryType.MULTICAST_FLOW)
//            throw new IllegalArgumentException("Given node is not a Multicast Flow node");
//
//        final NetPlan netPlan = callback.getDesign();
//        final MulticastDemand multicastDemand = netPlan.getMulticastDemandFromId((Long) multicastFlowNode.getValueAt(COLUMN_ID));
//
//        try
//        {
//            final IpMulticastFlow multicastFlow = new IpMulticastFlow(callback.getMla().getWrapperIp(), multicastDemand);
//
//            assert multicastFlow != null;
//
//            // Origin Node
//            TreeNode originNodeExpandNode = this.buildTreeNode("Origin nodes", "", "", EntryType.GENERIC);
//            treeView.addChildTo(multicastFlowNode, originNodeExpandNode);
//
//            treeView.addChildTo(originNodeExpandNode, this.buildTreeNode(multicastDemand.getIngressNode()));
//
//            // Destination Node
//            TreeNode destinationNodeExpandNode = this.buildTreeNode("Destination nodes", "", "", EntryType.GENERIC);
//            treeView.addChildTo(multicastFlowNode, destinationNodeExpandNode);
//
//            for (Node node : multicastDemand.getEgressNodes())
//                treeView.addChildTo(destinationNodeExpandNode, this.buildTreeNode(node));
//
//            switch (treeView.getTreeDirection())
//            {
//                case DOWNWARDS:
//                    // Links
//                    final MulticastTree multicastTree = multicastFlow.getAssociatedMulticastTree();
//
//                    if (multicastTree != null)
//                    {
//                        TreeNode linkExpandNode = this.buildTreeNode("IP Links", "", "", EntryType.GENERIC);
//                        treeView.addChildTo(multicastFlowNode, linkExpandNode);
//
//                        for (Link link : multicastTree.getLinkSet())
//                            treeView.addChildTo(linkExpandNode, this.buildTreeNode(link));
//                    }
//
//                    break;
//                case UPWARDS:
//                    TreeNode netPlanNode = this.buildTreeNode(netPlan);
//                    treeView.addChildTo(multicastFlowNode, netPlanNode);
//                    break;
//            }
//        } catch (IllegalArgumentException ex)
//        {
//            ErrorHandling.log("Cannot wrap into MulticastDemand: " + multicastDemand.getId() + " into MulticastFlow.");
//
//            throw new IllegalArgumentException();
//        }
//    }
//
//    void buildAlienWavelengthLeaf(TreeNode alienwavelengthNode)
//    {
//        if (alienwavelengthNode.getValueAt(COLUMN_TYPE) != EntryType.ALIEN_WAVELENGTH)
//            throw new IllegalArgumentException("Given node is not a Alien Wavelength node");
//
//        final NetPlan netPlan = callback.getDesign();
//        final Route alienRoute = netPlan.getRouteFromId((Long) alienwavelengthNode.getValueAt(COLUMN_ID));
//
//        assert alienRoute != null;
//
//        switch (treeView.getTreeDirection())
//        {
//            case DOWNWARDS:
//                TreeNode ochExpandNode = this.buildTreeNode("OCh Paths", "", "", EntryType.GENERIC);
//                treeView.addChildTo(alienwavelengthNode, ochExpandNode);
//
//                for (Link semilightPath : alienRoute.getSeqLinks())
//                {
//                    final Demand coupledDemand = semilightPath.getCoupledDemand();
//
//                    if (coupledDemand != null)
//                    {
//                        for (Route ochRoute : coupledDemand.getRoutes())
//                        {
//                            TreeNode ochNode = this.buildTreeNode(ochRoute);
//                            treeView.addChildTo(ochExpandNode, ochNode);
//                        }
//                    }
//                }
//                break;
//            case UPWARDS:
//                TreeNode netPlanNode = this.buildTreeNode(netPlan);
//                treeView.addChildTo(alienwavelengthNode, netPlanNode);
//
//                break;
//        }
//    }
//
//    void buildLightPathLeaf(TreeNode lightPathNode)
//    {
//        if (lightPathNode.getValueAt(COLUMN_TYPE) != EntryType.LIGHT_PATH)
//            throw new IllegalArgumentException("Given node is not a LightPath node");
//
//        final NetPlan netPlan = callback.getDesign();
//        final Route lightPath = netPlan.getRouteFromId((Long) lightPathNode.getValueAt(COLUMN_ID));
//
//        assert lightPath != null;
//
//        // LightPath Tree node children
//
//        switch (treeView.getTreeDirection())
//        {
//            case DOWNWARDS:
//            {
//                TreeNode ochExpandNode = this.buildTreeNode("OCh Paths", "", "", EntryType.GENERIC);
//                treeView.addChildTo(lightPathNode, ochExpandNode);
//
//                for (Link semilightPath : lightPath.getSeqLinks())
//                {
//                    final Demand coupledDemand = semilightPath.getCoupledDemand();
//
//                    if (coupledDemand != null)
//                    {
//                        for (Route ochRoute : coupledDemand.getRoutes())
//                        {
//                            TreeNode ochNode = this.buildTreeNode(ochRoute);
//                            treeView.addChildTo(ochExpandNode, ochNode);
//                        }
//                    }
//                }
//                break;
//            }
//            case UPWARDS:
//            {
//                final Link otuLink = lightPath.getDemand().getCoupledLink();
//                treeView.addChildTo(lightPathNode, this.buildTreeNode(otuLink));
//
//                break;
//            }
//        }
//    }
//
//    void buildOCHLeaf(TreeNode ochNode)
//    {
//        if (ochNode.getValueAt(COLUMN_TYPE) != EntryType.OCH)
//            throw new IllegalArgumentException("Given node is not a OCH node");
//
//        final NetPlan netPlan = callback.getDesign();
//        final Route ochRoute = netPlan.getRouteFromId((Long) ochNode.getValueAt(COLUMN_ID));
//
//        assert ochRoute != null;
//
//        switch (treeView.getTreeDirection())
//        {
//            case DOWNWARDS:
//            {
//                TreeNode wdmExpandNode = this.buildTreeNode("WDM Links", "", "", EntryType.GENERIC);
//                treeView.addChildTo(ochNode, wdmExpandNode);
//
//                for (Link wdmLink : ochRoute.getSeqLinks())
//                {
//                    TreeNode wdmNode = this.buildTreeNode(wdmLink);
//                    treeView.addChildTo(wdmExpandNode, wdmNode);
//                }
//                break;
//            }
//
//            case UPWARDS:
//            {
//                final Set<Route> traversingRoutes = ochRoute.getDemand().getCoupledLink().getTraversingRoutes();
//
//                if (traversingRoutes.size() == 1)
//                {
//                    final Route lighpath = traversingRoutes.iterator().next();
//                    treeView.addChildTo(ochNode, this.buildTreeNode(lighpath));
//                } else
//                {
//                    throw new Net2PlanException("Invalid network element");
//                }
//
//                break;
//            }
//        }
//    }
//
//    void buildIPRoute(TreeNode routeNode)
//    {
//        if (routeNode.getValueAt(COLUMN_TYPE) != EntryType.IP_ROUTE)
//            throw new IllegalArgumentException("Given node is not an IP route node");
//
//        final NetPlan netPlan = callback.getDesign();
//        final Route route = netPlan.getRouteFromId((Long) routeNode.getValueAt(COLUMN_ID));
//
//        assert route != null;
//
//        switch (treeView.getTreeDirection())
//        {
//            case DOWNWARDS:
//                TreeNode routeExpandNode = this.buildTreeNode("IP Links", "", "", EntryType.GENERIC);
//                treeView.addChildTo(routeNode, routeExpandNode);
//
//                for (Link link : route.getSeqLinks())
//                {
//                    TreeNode linkNode = this.buildTreeNode(link);
//                    treeView.addChildTo(routeExpandNode, linkNode);
//                }
//                break;
//            case UPWARDS:
//                final Link mplsTunnel = route.getDemand().getCoupledLink();
//
//                if (WrapperMplsTe.isMplsTeTunnel(mplsTunnel))
//                {
//                    final TreeNode mplsNode = this.buildTreeNode(mplsTunnel);
//                    treeView.addChildTo(routeNode, mplsNode);
//                }
//
//                break;
//        }
//    }
//
//    void buildWDMLeaf(TreeNode wdmNode)
//    {
//        if (wdmNode.getValueAt(COLUMN_TYPE) != EntryType.WDM)
//            throw new IllegalArgumentException("Given node is not a WDM node");
//
//        final Mtn mtn = callback.getDesign();
//        final UnidiLinkWdm wdmLink = mtn.getAssociatedWdmUnidiLink((Long) wdmNode.getValueAt(COLUMN_ID));
//
//        assert wdmLink != null;
//
//        // WDM Info
//        final MtnNode originNode = wdmLink.getA();
//        final MtnNode destinationNode = wdmLink.getB();
//
//        // WDM Tree node children
//        treeView.addChildTo(wdmNode, this.buildTreeNode(originNode));
//        treeView.addChildTo(wdmNode, this.buildTreeNode(destinationNode));
//
//        // Nodes
//        switch (treeView.getTreeDirection())
//        {
//            case DOWNWARDS:
//            {
//                TreeNode fiberSpanExpandNode = this.buildTreeNode("Fiber-Spans", "", "", EntryType.GENERIC);
//                treeView.addChildTo(wdmNode, fiberSpanExpandNode);
//
//                Demand coupledDemand = wdmLink.getCoupledDemand();
//                if (coupledDemand != null)
//                {
//                    for (Route fiberSpanRoute : coupledDemand.getRoutes())
//                    {
//                        for (Link fiberSpan : fiberSpanRoute.getSeqLinks())
//                        {
//                            TreeNode fiberSpanNode = this.buildTreeNode(fiberSpan);
//                            treeView.addChildTo(fiberSpanExpandNode, fiberSpanNode);
//                        }
//                    }
//                }
//
//                break;
//            }
//            case UPWARDS:
//            {
//                final Set<Route> traversingRoutes = wdmLink.getTraversingRoutes();
//
//                TreeNode ochExpandNode = this.buildTreeNode("Traversing OChs", "", "", EntryType.GENERIC);
//                treeView.addChildTo(wdmNode, ochExpandNode);
//
//                // OChs
//                for (Route ochRoutes : traversingRoutes)
//                    treeView.addChildTo(ochExpandNode, this.buildTreeNode(ochRoutes));
//
//                break;
//            }
//        }
//    }
//
//    void buildFiberSpanLeaf(TreeNode fiberSpanNode)
//    {
//        if (fiberSpanNode.getValueAt(COLUMN_TYPE) != EntryType.FIBER_SPAN)
//            throw new IllegalArgumentException("Given node is not a FiberSpan node");
//
//        final NetPlan netPlan = callback.getDesign();
//        final Link fiberSpanLink = netPlan.getLinkFromId((Long) fiberSpanNode.getValueAt(COLUMN_ID));
//
//        assert fiberSpanLink != null;
//
//        // FiberSpan Info
//        final Node originNode = fiberSpanLink.getOriginNode();
//        final Node destinationNode = fiberSpanLink.getDestinationNode();
//
//        // FiberSpan Tree node children
//        treeView.addChildTo(fiberSpanNode, this.buildTreeNode(originNode));
//        treeView.addChildTo(fiberSpanNode, this.buildTreeNode(destinationNode));
//
//        // Nodes
//        switch (treeView.getTreeDirection())
//        {
//            case UPWARDS:
//            {
//                final Set<Route> traversingRoutes = fiberSpanLink.getTraversingRoutes();
//
//                if (traversingRoutes.size() == 1)
//                {
//                    final Route fiberSpanRoute = traversingRoutes.iterator().next();
//
//                    final Link wdmLink = fiberSpanRoute.getDemand().getCoupledLink();
//
//                    if (wdmLink != null)
//                        treeView.addChildTo(fiberSpanNode, this.buildTreeNode(wdmLink));
//                } else
//                {
//                    throw new Net2PlanException("Invalid network element");
//                }
//
//                break;
//            }
//        }
//    }
//
//    void buildOADMLeaf(TreeNode oadmNode)
//    {
//        if (oadmNode.getValueAt(COLUMN_TYPE) != EntryType.OADM_NODE)
//            throw new IllegalArgumentException("Given node is not a OADM node");
//
//        final NetPlan netPlan = callback.getDesign();
//        final Node node = netPlan.getNodeFromId((Long) oadmNode.getValueAt(COLUMN_ID));
//
//        assert node != null;
//
//        final MultilayerAlgorithm mla = callback.getMla();
//
//        // OADM node Info
//
//        // OADM tree node children
//
//        switch (treeView.getTreeDirection())
//        {
//            case DOWNWARDS:
//            {
//                TreeNode incomingWDMLinksExpandNode = this.buildTreeNode("Incoming WDM links", "", "", EntryType.GENERIC);
//                treeView.addChildTo(oadmNode, incomingWDMLinksExpandNode);
//
//                for (Link wdmLink : node.getIncomingLinks(mla.getWdmLayer()))
//                    treeView.addChildTo(incomingWDMLinksExpandNode, this.buildTreeNode(wdmLink));
//
//                TreeNode outgoingWDMLinksExpandNode = this.buildTreeNode("Outgoing WDM links", "", "", EntryType.GENERIC);
//                treeView.addChildTo(oadmNode, outgoingWDMLinksExpandNode);
//
//                for (Link wdmLink : node.getOutgoingLinks(mla.getWdmLayer()))
//                    treeView.addChildTo(outgoingWDMLinksExpandNode, this.buildTreeNode(wdmLink));
//                break;
//            }
//            case UPWARDS:
//            {
//                treeView.addChildTo(oadmNode, this.buildTreeNode(netPlan));
//                break;
//            }
//        }
//    }
//
//    public TreeNode buildTreeNode(MtnNetworkElement element)
//    {
//        Object[] data = new Object[4];
//
//        String header = null;
//        String desc = null;
//        EntryType type = null;
//        try
//        {
//            final StringBuilder descBuilder = new StringBuilder();
//            final Mtn mla = callback.getDesign();
//
//            if (element == null)
//            {
//                final Mtn mtn = callback.getDesign();
//                header = "Network Design";
//                desc = "Name: " + (mtn.getName().isEmpty() ? "Unknown" : mtn.getName());
//                type = EntryType.NETPLAN;
//            } else if (element instanceof UnidiLink)
//            {
//                if (element instanceof UnidiLinkWdm)
//                {
//                    final UnidiLinkWdm link = (UnidiLinkWdm) element;
//                    descBuilder.append("Length: " + this.checkForInfiniteValue(link.getLengthInKmWorstCase()) + " Km, ");
//                    descBuilder.append("Number of line amplifiers: " + link.getTraversedOpticalLineAmplifiers().size());
//                    header = "WDM Link (" + link.getA().getName() + " -> " + link.getB().getName() + ")";
//                    desc = descBuilder.toString();
//                    type = EntryType.WDM;
//                } else if (element instanceof Uni)
//                {
//                    // OTU
//                    final Node originNode = link.getOriginNode();
//                    final Node destinationNode = link.getDestinationNode();
//
//                    try
//                    {
//                        final OTNUtils.OTUType otuType = OTNUtils.getOtuType(link);
//
//                        descBuilder.append("OTU type: " + otuType + ", ");
//                        descBuilder.append("Capacity: " + this.checkForInfiniteValue(otuType.getClientLineRateInGbps()) + " Gbps");
//                    } catch (Net2PlanException e)
//                    {
//                        descBuilder.append("OTU type: Unknown");
//                    }
//
//                    header = "OTU (" + originNode.getName() + " -> " + destinationNode.getName() + ")";
//                    desc = descBuilder.toString();
//                    type = EntryType.OTU;
//                } else if (link.getLayer() == mla.getIpLayer())
//                {
//                    final WrapperIPLayer wrapperIP = mla.getWrapperIp();
//
//                    // IP Link
//                    final Node originNode = link.getOriginNode();
//                    final Node destinationNode = link.getDestinationNode();
//
//                    Double carriedTraffic = link.getCarriedTraffic();
//                    Double utilization = link.getUtilization();
//
//                    final String headerHead;
//                    if (wrapperIP.isLagBundle(link))
//                    {
//                        headerHead = "IP Link LAG bundle";
//                    } else if (wrapperIP.isLagMember(link))
//                    {
//                        headerHead = "IP Link LAG member";
//
//                        if (link.getCapacity() == 0)
//                            utilization = 0d;
//                        else
//                            utilization = WrapperIPLayer.getLagBundle(link).getUtilization();
//
//                        carriedTraffic = link.getCapacity() * utilization;
//                    } else if (WrapperMplsTe.isMplsTeTunnel(link))
//                    {
//                        final WrapperMplsTe wrapperMplsTe = wrapperIP.getWrapperMplsTe();
//
//                        headerHead = "MPLS-TE Tunnel";
//
//                        if (wrapperMplsTe.isMplsTePrivateTunnel(link))
//                            descBuilder.append("Private tunnel" + ", ");
//
//                        if (wrapperMplsTe.isTunnelOfForwardingAdjacencyType(link))
//                            descBuilder.append("Forwarding adjacency (FA) type" + ", ");
//
//                        if (wrapperMplsTe.isTunnelOfAutorouteType(link))
//                            descBuilder.append("Auto-route type" + ", ");
//
//                        utilization = null;
//                    } else
//                    {
//                        headerHead = "IP Link";
//                    }
//
//                    descBuilder.append("Capacity: " + this.checkForInfiniteValue(link.getCapacity()) + " Gbps, ");
//
//                    if (carriedTraffic != null)
//                    descBuilder.append("Carried traffic: " + this.checkForInfiniteValue(carriedTraffic) + " Gbps");
//
//                    if (utilization != null)
//                        descBuilder.append(", Utilization: " + this.checkForInfiniteValue(utilization * 100) + "%");
//
//                    header = headerHead + " (" + originNode.getName() + " -> " + destinationNode.getName() + ")";
//                    desc = descBuilder.toString();
//                    type = EntryType.IP_LINK;
//                } else if (link.getLayer() == mla.getPhysLayer())
//                {
//                    // FiberSpan
//                    final Node originNode = link.getOriginNode();
//                    final Node destinationNode = link.getDestinationNode();
//
//                    descBuilder.append("Length: " + this.checkForInfiniteValue(link.getLengthInKm()) + " Km");
//
//                    header = "Fiber-Span (" + originNode.getName() + " -> " + destinationNode.getName() + ")";
//                    desc = descBuilder.toString();
//                    type = EntryType.FIBER_SPAN;
//                }
//            } else if (element instanceof Route)
//            {
//                final Route route = (Route) element;
//
//                if (route.getLayer() == mla.getOtuLayer())
//                {
//                    // OTU
//                    final boolean isBackUp = route.isBackupRoute();
//
//                    final Node ingressNode = route.getIngressNode();
//                    final Node egressNode = route.getEgressNode();
//
//                    descBuilder.append("Is back-up: " + route.isBackupRoute());
//
//                    desc = descBuilder.toString();
//
//                    if (mla.isAlienBidirectionalLightpath(route.getDemand()))
//                    {
//                        // Alien-wavelength
//                        header = "Alien wavelength [" + (!isBackUp ? "Main" : "Back-Up") + "] (" + ingressNode.getName() + " -> " + egressNode.getName() + ")";
//                        type = EntryType.ALIEN_WAVELENGTH;
//                    } else
//                    {
//                        // LightPath
//                        header = "LightPath [" + (!isBackUp ? "Main" : "Back-Up") + "] (" + ingressNode.getName() + " -> " + egressNode.getName() + ")";
//                        type = EntryType.LIGHT_PATH;
//                    }
//                } else if (route.getLayer() == mla.getOduLayer())
//                {
//                    // ODU
//                    final boolean isBackUp = route.isBackupRoute();
//
//                    final Node ingressNode = route.getIngressNode();
//                    final Node egressNode = route.getEgressNode();
//
//                    try
//                    {
//                        final OTNUtils.ODUType oduType = OTNUtils.getOduType(route);
//
//                        descBuilder.append("ODU Type: " + oduType + ", ");
//                        descBuilder.append("Capacity: " + this.checkForInfiniteValue(oduType.getClientLineRateGbps()) + " Gbps, ");
//                    } catch (Net2PlanException e)
//                    {
//                        descBuilder.append("ODU Type: Unknown, ");
//                    }
//
//                    descBuilder.append("Is back-up: " + route.isBackupRoute());
//
//                    header = "Associated ODU [" + (!isBackUp ? "Main" : "Back-Up") + "] (" + ingressNode.getName() + " -> " + egressNode.getName() + ")";
//                    desc = descBuilder.toString();
//                    type = EntryType.ODU;
//                } else if (route.getLayer() == mla.getWdmLayer())
//                {
//                    // OCH
//                    final Node ingressNode = route.getIngressNode();
//                    final Node egressNode = route.getEgressNode();
//
//                    final RSAUnidirSemilp rsa = new RSAUnidirSemilp(route);
//
//                    descBuilder.append("Length: " + this.checkForInfiniteValue(rsa.getLengthInKm()) + " Km, ");
//                    descBuilder.append("Spectrum slots(# " + rsa.getNumSlots() + "): " + rsa.occupiedSlots.toString());
//
//                    header = "OCh (" + ingressNode.getName() + " -> " + egressNode.getName() + ")";
//                    desc = descBuilder.toString();
//                    type = EntryType.OCH;
//                } else if (route.getLayer() == mla.getIpLayer())
//                {
//                    final WrapperMplsTe wrapperMplsTe = mla.getWrapperIp().getWrapperMplsTe();
//
//                    // IP Route
//                    final boolean isBackUp = route.isBackupRoute();
//
//                    final Node ingressNode = route.getIngressNode();
//                    final Node egressNode = route.getEgressNode();
//
//                    final Optional<Route> activeRoute = wrapperMplsTe.getTunnelCurrentRouteOkDataAndControlPlane(route.getDemand().getCoupledLink());
//                    if (activeRoute.isPresent())
//                        if (route == activeRoute.get())
//                            descBuilder.append("Is active" + ", ");
//
//                    descBuilder.append("Is back-up: " + isBackUp);
//
//                    header = "IP route (" + ingressNode.getName() + ", " + egressNode.getName() + ")";
//                    desc = descBuilder.toString();
//                    type = EntryType.IP_ROUTE;
//                }
//            } else if (element instanceof Node)
//            {
//                final Node node = (Node) element;
//
//                final NODETYPE nodeType = NODETYPE.getType(node);
//
//                switch (nodeType)
//                {
//                    // IP Node
//                    case IPROUTER_INTERNAL:
//                    {
//                        descBuilder.append("Name: " + node.getName() + ", ");
//                        descBuilder.append("Site: " + node.getSiteName());
//
//                        header = "Internal IP Node (" + node.getName() + ")";
//                        desc = descBuilder.toString();
//                        type = EntryType.IP_NODE;
//                        break;
//                    }
//                    case IPROUTER_EXTERNAL:
//                    {
//                        descBuilder.append("Name: " + node.getName() + ", ");
//                        descBuilder.append("Site: " + node.getSiteName());
//
//                        header = "External IP Node (" + node.getName() + ")";
//                        desc = descBuilder.toString();
//                        type = EntryType.IP_NODE;
//                        break;
//                    }
//                    case OTNSWITCH:
//                    {
//                        descBuilder.append("Name: " + node.getName() + ", ");
//
//                        header = "OTU Node (" + node.getName() + ")";
//                        desc = descBuilder.toString();
//                        type = EntryType.IP_NODE;
//                        break;
//                    }
//                    case OADM:
//                    {
//                        descBuilder.append("Number of incoming WDM links: " + node.getIncomingLinks(mla.getWdmLayer()).size() + ", ");
//                        descBuilder.append("Number of outgoing WDM links: " + node.getOutgoingLinks(mla.getWdmLayer()).size());
//
//                        header = "OADM Node (" + node.getName() + ")";
//                        desc = descBuilder.toString();
//                        type = EntryType.OADM_NODE;
//                        break;
//                    }
//                    case OA:
//                    {
//                        descBuilder.append("Name: " + node.getName());
//
//                        header = "OA Node (" + node.getName() + ")";
//                        desc = descBuilder.toString();
//                        type = EntryType.OA_NODE;
//                        break;
//                    }
//                }
//            } else if (element instanceof Demand)
//            {
//                final WrapperMplsTe wrapperMplsTe = mla.getWrapperIp().getWrapperMplsTe();
//
//                // IP Demand
//                final Demand demand = (Demand) element;
//
//                final Node ingressNode = demand.getIngressNode();
//                final Node egressNode = demand.getEgressNode();
//
//                if (wrapperMplsTe.isIpDemandPrivateMplsTeRouted(demand))
//                    descBuilder.append("Private demand" + ", ");
//
//                descBuilder.append("Offered traffic: " + demand.getOfferedTraffic() + ", ");
//                descBuilder.append("Carried traffic: " + demand.getCarriedTraffic() + ", ");
//                descBuilder.append("Blocked traffic: " + demand.getBlockedTraffic());
//
//                header = "IP Demand (" + ingressNode.getName() + " -> " + egressNode.getName() + ")";
//                desc = descBuilder.toString();
//                type = EntryType.IP_DEMAND;
//            } else if (element instanceof MulticastDemand)
//            {
//                try
//                {
//                    final MulticastDemand multicastDemand = (MulticastDemand) element;
//                    final IpMulticastFlow multicastFlow = new IpMulticastFlow(mla.getWrapperIp(), multicastDemand);
//
//                    descBuilder.append("Offered traffic: " + multicastDemand.getOfferedTraffic() + ", ");
//                    descBuilder.append("Carried traffic: " + multicastDemand.getCarriedTraffic() + ", ");
//                    descBuilder.append("Blocked traffic: " + multicastDemand.getBlockedTraffic());
//
//                    header = "MulticastFlow (" + multicastDemand.getIngressNode().getName() + " -> " + StringUtils.collectionToString(Lists.newArrayList(multicastDemand.getEgressNodes())) + ")";
//                    desc = descBuilder.toString();
//                    type = EntryType.MULTICAST_FLOW;
//                } catch (IllegalArgumentException ex)
//                {
//                    ErrorHandling.log("Cannot cast into MulticastFlow: " + element.getId());
//                }
//            }
//
//            if (header == null || type == null || desc == null) return this.buildUnknownNode();
//        } catch (ClassCastException e)
//        {
//            return this.buildUnknownNode();
//        }
//
//        data[COLUMN_TREE] = header;
//        data[COLUMN_DESC] = desc;
//        data[COLUMN_ID] = element.getId();
//        data[COLUMN_TYPE] = type;
//
//        TreeNode treeNode = new TreeNode(data);
//
//        return treeNode;
//    }
//
//    public TreeNode buildTreeNode(String header, String description, Object elementId, EntryType type)
//    {
//        Object[] data = new Object[4];
//
//        data[COLUMN_TREE] = header;
//        data[COLUMN_DESC] = description;
//        data[COLUMN_ID] = elementId;
//        data[COLUMN_TYPE] = type;
//
//        TreeNode treeNode = new TreeNode(data);
//
//        return treeNode;
//    }
//
//    public TreeNode buildUnknownNode()
//    {
//        final TreeNode unknownNode = this.buildTreeNode("Unknown element", "Could not find data for the picked element.", "", EntryType.UNKNOWN);
//        unknownNode.setAllowsChildren(false);
//
//        return unknownNode;
//    }
//
//    private static String checkForInfiniteValue(double value)
//    {
//        final DecimalFormat decimalFormat = new DecimalFormat("##.##");
//        return value == Double.MAX_VALUE ? "\u221e" : decimalFormat.format(value);
//    }
}
