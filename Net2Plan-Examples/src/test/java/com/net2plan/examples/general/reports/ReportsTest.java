package com.net2plan.examples.general.reports;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.net2plan.examples.general.offline.Offline_ipOverWdm_routingSpectrumAndModulationAssignmentHeuristicNotGrooming;
import com.net2plan.examples.general.onlineSim.Online_evProc_ipOverWdm;
import com.net2plan.examples.ocnbook.reports.Report_availability;
import com.net2plan.examples.ocnbook.reports.Report_delay;
import com.net2plan.examples.ocnbook.reports.Report_perSRGFailureAnalysis;
import com.net2plan.interfaces.networkDesign.IReport;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.libraries.SRGUtils;
import com.net2plan.libraries.SRGUtils.SharedRiskModel;
import com.net2plan.libraries.WDMUtils;
import com.net2plan.utils.InputParameter;
import com.net2plan.utils.StringUtils;
public class ReportsTest
{
	private NetPlan np;
	private NetworkLayer wdmLayer, ipLayer;

	@Before
	public void setUp() throws Exception
	{
		this.np = new NetPlan (new File ("src/test/resources/data/networkTopologies/example7nodes_ipOverWDM.n2p"));
		this.wdmLayer = np.getNetworkLayer(0);
		this.ipLayer = np.getNetworkLayer(1);
		np.removeAllSRGs(); 
		SRGUtils.configureSRGs(np, 1, 1, SharedRiskModel.PER_BIDIRECTIONAL_LINK_BUNDLE, true, wdmLayer);
		WDMUtils.isWDMFormatCorrect(np , wdmLayer);
		
		/* Creates a design with 1+1 for the tests */
		Map<String,String> paramFor11 = InputParameter.getDefaultParameters(new Offline_ipOverWdm_routingSpectrumAndModulationAssignmentHeuristicNotGrooming ().getParameters());
		paramFor11.put("networkRecoveryType" , "1+1-srg-disjoint-lps");
		new Offline_ipOverWdm_routingSpectrumAndModulationAssignmentHeuristicNotGrooming().executeAlgorithm(np , paramFor11 , null);
		
	}

	@After
	public void tearDown() throws Exception
	{
	}

	@Test
	public void testReportWDMLineEngineering()
	{
		final IReport report = new Report_WDM_lineEngineering ();
		Map<String,List<String>> testingParameters = new HashMap<> ();
		List<Map<String,String>> testsParam = InputParameter.getCartesianProductOfParameters (testingParameters);
		if (testsParam.isEmpty()) testsParam = Arrays.asList(InputParameter.getDefaultParameters(report.getParameters()));
		for (Map<String,String> params : testsParam)
		{
			Map<String,String> paramsUsedToCall = InputParameter.getDefaultParameters(report.getParameters());
			paramsUsedToCall.putAll(params); // so default parameters that are also in param, are replaced
			//System.out.println(paramsUsedToCall);
			String result = report.executeReport(np , paramsUsedToCall , null);
			assertTrue (result.length() > 100);
		}
	}

	@Test
	public void testReportWDMPointToPointLineEngineering()
	{
		final IReport report = new Report_WDM_pointToPointLineEngineering();
		Map<String,List<String>> testingParameters = new HashMap<> ();
		testingParameters.put("report__checkCDOnlyAtTheReceiver" , Arrays.asList("true" , "false"));
		List<Map<String,String>> testsParam = InputParameter.getCartesianProductOfParameters (testingParameters);
		if (testsParam.isEmpty()) testsParam = Arrays.asList(InputParameter.getDefaultParameters(report.getParameters()));
		for (Map<String,String> params : testsParam)
		{
			Map<String,String> paramsUsedToCall = InputParameter.getDefaultParameters(report.getParameters());
			paramsUsedToCall.putAll(params); // so default parameters that are also in param, are replaced
			//System.out.println(paramsUsedToCall);
			String result = report.executeReport(np , paramsUsedToCall , null);
			assertTrue (result.length() > 100);
		}
	}

	@Test
	public void testReportWdmRoutingSpectrumAndModulationAssignments()
	{
		final IReport report = new Report_wdm_routingSpectrumAndModulationAssignments();
		Map<String,List<String>> testingParameters = new HashMap<> ();
		List<Map<String,String>> testsParam = InputParameter.getCartesianProductOfParameters (testingParameters);
		if (testsParam.isEmpty()) testsParam = Arrays.asList(InputParameter.getDefaultParameters(report.getParameters()));
		for (Map<String,String> params : testsParam)
		{
			Map<String,String> paramsUsedToCall = InputParameter.getDefaultParameters(report.getParameters());
			paramsUsedToCall.putAll(params); // so default parameters that are also in param, are replaced
			//System.out.println(paramsUsedToCall);
			String result = report.executeReport(np , paramsUsedToCall , null);
			assertTrue (result.length() > 100);
		}
	}
	
	@Test
	public void testReportAvailability ()
	{
		final IReport report = new Report_availability();
		List<String> ipOverWdmEvProcessorRecoveryTypes = Arrays.asList("static-lps-OSPF-rerouting" , "1+1-lps-OSPF-rerouting" , "lp-restoration-OSPF-rerouting");
		for (String ipOverWdmEvProcessorRecoveryType : ipOverWdmEvProcessorRecoveryTypes)
		{
			/* create the input design so that is single SRG failure tolerant in such case */
			//if (!ipOverWdmEvProcessorRecoveryType.equals("1+1-lps-OSPF-rerouting")) 
			
			/* input parameters of the event processor */
			Map<String,String> provAlgorithmParam = InputParameter.getDefaultParameters(new Online_evProc_ipOverWdm().getParameters());
			//System.out.println(provAlgorithmParam);
			provAlgorithmParam.put("ipOverWdmNetworkRecoveryType" , ipOverWdmEvProcessorRecoveryType);

			/* input parameters of the report */
			Map<String,List<String>> testingParameters = new HashMap<> ();
			final String evProcClassRelativePath = "target/classes/com/net2plan/examples/general/onlineSim/Online_evProc_ipOverWdm.class";
			final String evProcClassQualifiedName = "com.net2plan.examples.general.onlineSim.Online_evProc_ipOverWdm";
			testingParameters.put("provisioningAlgorithm_file" , Arrays.asList(evProcClassRelativePath));
			testingParameters.put("provisioningAlgorithm_classname" , Arrays.asList(evProcClassQualifiedName));
			testingParameters.put("provisioningAlgorithm_parameters" , Arrays.asList(StringUtils.mapToString(provAlgorithmParam)));
			testingParameters.put("analyzeDoubleFailures" , Arrays.asList("true" , "false"));
			testingParameters.put("failureModel" , Arrays.asList("perBidirectionalLinkBundle" , "SRGfromNetPlan" , "perLink" , "perDirectionalLinkBundle"));
			testingParameters.put("considerTrafficInOversubscribedLinksAsLost" , Arrays.asList("true" , "false"));
			List<Map<String,String>> testsParam = InputParameter.getCartesianProductOfParameters (testingParameters);
			if (testsParam.isEmpty()) testsParam = Arrays.asList(InputParameter.getDefaultParameters(report.getParameters()));
			for (Map<String,String> params : testsParam)
			{
				Map<String,String> paramsUsedToCall = InputParameter.getDefaultParameters(report.getParameters());
				paramsUsedToCall.putAll(params); // so default parameters that are also in param, are replaced
				String result = report.executeReport(np , paramsUsedToCall , ImmutableMap.of("precisionFactor" , "0.0001"));
				assertTrue (result.length() > 100);
			}
		}
	}
	

	@Test
	public void testReportDelay ()
	{
		final IReport report = new Report_delay();
		Map<String,List<String>> testingParameters = new HashMap<> ();
		List<Map<String,String>> testsParam = InputParameter.getCartesianProductOfParameters (testingParameters);
		if (testsParam.isEmpty()) testsParam = Arrays.asList(InputParameter.getDefaultParameters(report.getParameters()));
		for (Map<String,String> params : testsParam)
		{
			Map<String,String> paramsUsedToCall = InputParameter.getDefaultParameters(report.getParameters());
			paramsUsedToCall.putAll(params); // so default parameters that are also in param, are replaced
			String result = report.executeReport(np , paramsUsedToCall , ImmutableMap.of("precisionFactor" , "0.0001"));
			assertTrue (result.length() > 100);
		}
	}

	@Test
	public void testReportPerSRGFailureAnalysis ()
	{
		final IReport report = new Report_perSRGFailureAnalysis();
		List<String> ipOverWdmEvProcessorRecoveryTypes = Arrays.asList("static-lps-OSPF-rerouting" , "1+1-lps-OSPF-rerouting" , "lp-restoration-OSPF-rerouting");
		for (String ipOverWdmEvProcessorRecoveryType : ipOverWdmEvProcessorRecoveryTypes)
		{
			/* create the input design so that is single SRG failure tolerant in such case */
			//if (!ipOverWdmEvProcessorRecoveryType.equals("1+1-lps-OSPF-rerouting")) 
			
			/* input parameters of the event processor */
			Map<String,String> provAlgorithmParam = InputParameter.getDefaultParameters(new Online_evProc_ipOverWdm().getParameters());
			//System.out.println(provAlgorithmParam);
			provAlgorithmParam.put("ipOverWdmNetworkRecoveryType" , ipOverWdmEvProcessorRecoveryType);

			/* input parameters of the report */
			Map<String,List<String>> testingParameters = new HashMap<> ();
			final String evProcClassRelativePath = "target/classes/com/net2plan/examples/general/onlineSim/Online_evProc_ipOverWdm.class";
			final String evProcClassQualifiedName = "com.net2plan.examples.general.onlineSim.Online_evProc_ipOverWdm";
			testingParameters.put("provisioningAlgorithm_file" , Arrays.asList(evProcClassRelativePath));
			testingParameters.put("provisioningAlgorithm_classname" , Arrays.asList(evProcClassQualifiedName));
			testingParameters.put("provisioningAlgorithm_parameters" , Arrays.asList(StringUtils.mapToString(provAlgorithmParam)));
			testingParameters.put("considerTrafficInOversubscribedLinksAsLost" , Arrays.asList("true" , "false"));
			testingParameters.put("failureModel" , Arrays.asList("perBidirectionalLinkBundle" , "SRGfromNetPlan" , "perNode" , "perLink" , "perDirectionalLinkBundle"));
			List<Map<String,String>> testsParam = InputParameter.getCartesianProductOfParameters (testingParameters);
			if (testsParam.isEmpty()) testsParam = Arrays.asList(InputParameter.getDefaultParameters(report.getParameters()));
			for (Map<String,String> params : testsParam)
			{
				Map<String,String> paramsUsedToCall = InputParameter.getDefaultParameters(report.getParameters());
				paramsUsedToCall.putAll(params); // so default parameters that are also in param, are replaced
				String result = report.executeReport(np , paramsUsedToCall , ImmutableMap.of("precisionFactor" , "0.0001"));
				assertTrue (result.length() > 100);
			}
		}
	}

}
