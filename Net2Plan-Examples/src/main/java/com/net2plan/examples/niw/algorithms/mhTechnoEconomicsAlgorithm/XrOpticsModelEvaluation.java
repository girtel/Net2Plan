package com.net2plan.examples.niw.algorithms.mhTechnoEconomicsAlgorithm;

import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.niw.WNet;
import com.net2plan.utils.InputParameter;
import org.apache.xmlbeans.impl.tool.XSTCTester;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class XrOpticsModelEvaluation extends XSTCTester.TestCase {


    @Test
    public void testXrOpticsEvaluation()
    {
        final NetPlan np = new NetPlan();
        final Map<String, String> algorithmParameters = InputParameter.getDefaultParameters(new ImporterFromTimBulkFiles_forConfTimData().getParameters());
        final Map<String, String> capacityParameters = InputParameter.getDefaultParameters(new CapacityPlanningAlgorithm_v1().getParameters());
        final Map<String, String> xrPostProcessingParameters = InputParameter.getDefaultParameters(new XrOpticsPostprocessing().getParameters());
//        final Map<String, String> bomParameters = InputParameter.getDefaultParameters(new BillOfMaterialsOptical_v1().getParameters());

        final String excelFile = "Traffic_Small_M-H_D2.3";
        final String nodeOption = "option2";
        final String year = "2025";

        final Boolean faultTolerance_boolean = false;
        String faultTolerance = "";
        if (faultTolerance_boolean) faultTolerance = "faultTol";
        else faultTolerance = "noFaultTol";

        final String planApproach = "-1";
        String dynamicStatic = "";
        if (planApproach.equalsIgnoreCase("-1")) dynamicStatic = "static";
        else dynamicStatic = "dynamic";

        final String pathFolder = "resources/outputs/" + excelFile + "/" + faultTolerance + "/" + year + "/" + nodeOption + "/" + dynamicStatic;
        final String fileString = pathFolder + "/out_scenario.n2p" ;

        xrPostProcessingParameters.put("alpha", "2.5");
        xrPostProcessingParameters.put("isXRopticsType", "true");
        xrPostProcessingParameters.put("isTrafficAware", "true");

        System.out.println(fileString);

        Path path = Paths.get(pathFolder);

        if (!Files.exists(Paths.get(fileString))) {

            algorithmParameters.put("excelFilePath", "resources/excelFiles/"+excelFile+".xlsx");
            algorithmParameters.put("opticalSwiInputParameter",nodeOption);   // Option 1: All Roadm, Option2: with D&W Filterless
            algorithmParameters.put("initialIndex",planApproach); // -1: Static planning, 0: Dynamic planning
            algorithmParameters.put("yearDataForAllTrafficTypes",year); // 2019 2022 2025
            capacityParameters.put("addFaultToleranceToMcenBBFailureAndWdmDuctFailureIfPossible",faultTolerance_boolean.toString()); // false: Without failure tolerance, true: with faultTolerance
            capacityParameters.put("indexOfTimeSlotToAplyMinus1MakesWorstCaseAllSlots", planApproach);

            new ImporterFromTimBulkFiles_forConfTimData().executeAlgorithm(np, algorithmParameters, new HashMap<>());
            new CapacityPlanningAlgorithm_v1().executeAlgorithm(np, capacityParameters, new HashMap<>());
            new WNet(np).checkConsistency();
            new XrOpticsPostprocessing().executeAlgorithm(np, xrPostProcessingParameters, new HashMap<>());

            // if directory exists?
            if (!Files.exists(path))
            {
                try
                {
                    Files.createDirectories(path);
                } catch (IOException e)
                {
                    // fail to create directory
                    e.printStackTrace();
                }
            }

            np.saveToFile(new File(fileString));

        }
        else
        {
            final NetPlan np2 = new NetPlan(new File(fileString));
            new XrOpticsPostprocessing().executeAlgorithm(np2, xrPostProcessingParameters, new HashMap<>());
        }
    }




}
