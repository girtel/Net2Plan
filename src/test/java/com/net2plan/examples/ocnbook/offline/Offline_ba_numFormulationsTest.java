package com.net2plan.examples.ocnbook.offline;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.libraries.SRGUtils;
import com.net2plan.libraries.WDMUtils;
import com.net2plan.libraries.SRGUtils.SharedRiskModel;


public class Offline_ba_numFormulationsTest
{
	private NetPlan np;

	@Before
	public void setUp() throws Exception
	{
		this.np = new NetPlan (new File ("src/main/resources/data/networkTopologies/example4nodes.n2p"));
		this.nPDemands = new NetPlan (new File ("src/main/resources/data/networkTopologies/example4nodes.n2p"));
		this.wdmLayerIndex = 0;
		this.ipLayerIndex = 1;
		np.removeAllSRGs(); 
		SRGUtils.configureSRGs(np, 1, 1, SharedRiskModel.PER_BIDIRECTIONAL_LINK_BUNDLE, true, np.getNetworkLayer(wdmLayerIndex));
		WDMUtils.isWDMFormatCorrect(np , np.getNetworkLayer(wdmLayerIndex));

	}

	@After
	public void tearDown() throws Exception
	{
	}

	@Test
	public void testOffline_ba_numFormulationsTest()
	{

	}

}
