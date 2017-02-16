package com.net2plan.interfaces.networkDesign;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ DemandTest.class, InterLayerPropagationGraphTest.class, LinkTest.class, MulticastDemandTest.class,
		MulticastTreeTest.class, NetPlanTest.class, NodeTest.class, Offline_nfvPlacementTest.class, ResourceTest.class,
		RouteTest.class, SharedRiskGroupTest.class })
public class AllTests
{

}
