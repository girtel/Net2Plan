package com.net2plan.examples.niw.research.technoec;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.net2plan.examples.niw.research.technoec.TimExcel_NodeListSheet_forConfTimData.NODETYPE;
import com.net2plan.interfaces.networkDesign.IAlgorithm;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.niw.OpticalSpectrumManager;
import com.net2plan.niw.WFiber;
import com.net2plan.niw.WLightpathRequest;
import com.net2plan.niw.WNet;
import com.net2plan.niw.WNode;
import com.net2plan.niw.WVnfInstance;
import com.net2plan.niw.WVnfType;
import com.net2plan.utils.InputParameter;
import com.net2plan.utils.Quintuple;
import com.net2plan.utils.StringUtils;
import com.net2plan.utils.Triple;

public class AssessmentBenefitsOfDynamicity implements IAlgorithm
{

    InputParameter initialIndex = new InputParameter("initialIndex", -1, "InitialIndex");
    private InputParameter excelFilePath = new InputParameter("excelFilePath", "resources/excelFiles/MetroHaulProject.TIM.TopologyAndTraffic.DenseUrbanMetro.xlsx", "The path to the Excel file in the TIM format");
    private InputParameter opticalSwiInputParameter = new InputParameter("opticalSwiInputParameter","#select# option2 option1","Option 1: All = ROADM; Option 2: MCEN = ROADM, AMEN (>2) AMEN (=2) = Filterless D&W");
    private InputParameter yearDataForAllTrafficTypes = new InputParameter("yearDataForAllTrafficTypes", "#select# 2019 2022 2025", "Available yearly data for all the traffic types.");
    private InputParameter addFaultToleranceToMcenBBFailureAndWdmDuctFailureIfPossible = new InputParameter("addFaultToleranceToMcenBBFailureAndWdmDuctFailureIfPossible", true , "");

//    CapacityPlanningAlgorithm_v1 capacityAlgorithm;

	@Override
	public String executeAlgorithm(NetPlan np, Map<String, String> algorithmParameters, Map<String, String> net2planParameters)
	{
        // First of all, initialize all parameters
	    InputParameter.initializeAllInputParameterFieldsOfObject(this, algorithmParameters);

        final Map<String, String> importerParameters = InputParameter.getDefaultParameters(new ImporterFromTimBulkFiles_forConfTimData().getParameters());
        importerParameters.put("excelFilePath", excelFilePath.getString());
        importerParameters.put("opticalSwiInputParameter",opticalSwiInputParameter.getString());
        importerParameters.put("yearDataForAllTrafficTypes", yearDataForAllTrafficTypes.getString());

        new ImporterFromTimBulkFiles_forConfTimData().executeAlgorithm(np, importerParameters, new HashMap<>());

		WNet wNet = new WNet(np);

//		capacityAlgorithm = new CapacityPlanningAlgorithm_v1();
        final String faultTolerance = (addFaultToleranceToMcenBBFailureAndWdmDuctFailureIfPossible.getBoolean() ? "faultTol" : "noFaultTol" );

        final String nodeOption = opticalSwiInputParameter.getString();
        final String year = yearDataForAllTrafficTypes.getString();
        String excelFile = StringUtils.split(algorithmParameters.get("excelFilePath"), "/")[2].replace(".xlsx", "");

        final String file = "resources/outputs/" + excelFile + "/" + faultTolerance + "/" + year + "/" + nodeOption;
        Path path = null;

		/* Assess dynamic case */
		if (initialIndex.getInt() > -1 && initialIndex.getInt() < 24 )
		{
            for (int timeIndex = initialIndex.getInt(); timeIndex < 24; timeIndex++) 
            {
                final NetPlan npCopy = np.copy();
            	final JltTecnoec1_capacityPlanningAlgorithm capacityAlgorithm = new JltTecnoec1_capacityPlanningAlgorithm();
                final Map<String, String> param = InputParameter.getDefaultParameters(capacityAlgorithm.getParameters());
                param.put("indexOfTimeSlotToAplyMinus1MakesWorstCaseAllSlots", Integer.toString(timeIndex));
                param.put("addFaultToleranceToMcenBBFailureAndWdmDuctFailureIfPossible",Boolean.toString(addFaultToleranceToMcenBBFailureAndWdmDuctFailureIfPossible.getBoolean()));

                capacityAlgorithm.executeAlgorithm(npCopy, param, net2planParameters);
                final Statistics statDynamicCase = new Statistics(new WNet(npCopy), capacityAlgorithm, capacityAlgorithm.getOpticalSpectrumManager());

                final String fileDynamic = file + "/dynamic";

                path = Paths.get(fileDynamic);

                // if directory exists?
                if (!Files.exists(path)) {
                    try {
                        Files.createDirectories(path);
                    } catch (IOException e) {
                        // fail to create directory
                        e.printStackTrace();
                    }
                }

                // First, Dynamic case data stored in different files

                statDynamicCase.storeStatsAsIntegerList(statDynamicCase.getCoreOrAggTypePerFiber(), fileDynamic + "/out_fibersTypes_dynamic.txt");
                statDynamicCase.storeStatsAsIntegerList(statDynamicCase.getFinalDegreePerNode(), fileDynamic + "/out_nodesDegree_" + Integer.toString(timeIndex) + ".txt");
                statDynamicCase.storeStatsAsIntegerList(capacityAlgorithm.getNewFibers(),fileDynamic + "/out_newFibers_" + Integer.toString(timeIndex) + ".txt");
                statDynamicCase.storeStatsAsIntegerList(capacityAlgorithm.getNewFibersIndexes(),fileDynamic + "/out_newFibersIndexes_" + Integer.toString(timeIndex) + ".txt");

                statDynamicCase.storeStatsAsIntegerList(statDynamicCase.getNumSlotsOccupiedOrWastedPerFiber(), fileDynamic + "/out_NumSlotsOccupiedByLpOrWastePerFiber_" + Integer.toString(timeIndex) + ".txt");
                statDynamicCase.storeStatsAsIntegerList(statDynamicCase.getFsuPerFiber(), fileDynamic + "/out_FsuPerFiber_" + Integer.toString(timeIndex) + ".txt");
                statDynamicCase.storeStatsAsIntegerMap(statDynamicCase.getTranspoderTrafficPerNode(), fileDynamic + "/out_TranspoderTrafficPerNode_" + Integer.toString(timeIndex) + ".txt");
                statDynamicCase.storeStatsAsDoubleMap(statDynamicCase.getVNFsTrafficPerNode(), fileDynamic + "/out_VNFsTrafficPerNode_" + Integer.toString(timeIndex) + ".txt");
            }
        }else if(initialIndex.getInt() == -1){
            		/* Tests for static case */
            final NetPlan npCopy = np.copy();
            final JltTecnoec1_capacityPlanningAlgorithm capacityAlgorithm = new JltTecnoec1_capacityPlanningAlgorithm();
            final Map<String, String> param = InputParameter.getDefaultParameters(capacityAlgorithm.getParameters());
            param.put("indexOfTimeSlotToAplyMinus1MakesWorstCaseAllSlots", "-1");
            param.put("addFaultToleranceToMcenBBFailureAndWdmDuctFailureIfPossible",Boolean.toString(addFaultToleranceToMcenBBFailureAndWdmDuctFailureIfPossible.getBoolean()));
            capacityAlgorithm.executeAlgorithm(npCopy, param, net2planParameters);
            final Statistics statStaticCase = new Statistics(new WNet(npCopy), capacityAlgorithm , capacityAlgorithm.getOpticalSpectrumManager());

            // final Statistics statsStaticCase = new Statistics(new WNet (np.copy()), capacityAlgorithm);

            String fileStatic = file + "/static";

            path = Paths.get(fileStatic);

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

            // First, Static case data stored in different files

            statStaticCase.storeStatsAsIntegerList(statStaticCase.getCoreOrAggTypePerFiber(), fileStatic + "/out_fibersTypes_static.txt");
            statStaticCase.storeStatsAsIntegerList(statStaticCase.getFinalDegreePerNode(), fileStatic + "/out_nodesDegree_static.txt");
            statStaticCase.storeStatsAsIntegerList(capacityAlgorithm.getNewFibers(),fileStatic + "/out_newFibers_static.txt");
            statStaticCase.storeStatsAsIntegerList(capacityAlgorithm.getNewFibersIndexes(),fileStatic + "/out_newFibersIndexes_static.txt");

            statStaticCase.storeStatsAsIntegerList(statStaticCase.getNumSlotsOccupiedOrWastedPerFiber(), fileStatic + "/out_NumSlotsOccupiedByLpOrWastePerFiber_static.txt");
            statStaticCase.storeStatsAsIntegerList(statStaticCase.getFsuPerFiber(), fileStatic + "/out_FsuPerFiber_static.txt");
            statStaticCase.storeStatsAsIntegerMap(statStaticCase.getTranspoderTrafficPerNode(), fileStatic + "/out_TranspoderTrafficPerNode_static.txt");
            statStaticCase.storeStatsAsDoubleMap(statStaticCase.getVNFsTrafficPerNode(), fileStatic + "/out_VNFsTrafficPerNode_static.txt");
        }
        else
            new Net2PlanException("Wrong time index, it must be between -1 and 23;");

		np.saveToFile(new File(file+"out_scenario.n2p"));

		return "Ok";

	}

	@Override
	public String getDescription()
	{
        return "This importer file converts from an Excel file as provided by TIM, and creates the N2P file with all the information there";
	}

	@Override
    public List<Triple<String, String, String>> getParameters()
    {
		/* Returns the parameter information for all the InputParameter objects defined in this object (uses Java reflection) */
        return InputParameter.getInformationAllInputParameterFieldsOfObject(this);
    }

	private static class Statistics
	{
		final WNet wNet;
		final List<Integer> fsuPerLink_e;
		final List<Integer> numOccupiedSlotsPerLink_e;
		final List<Integer> indexesCoreOrAggType_e;
		final List <Integer> newNodeDegree_n;
		final SortedMap<String, List<Integer>> numTransponders_type_n;
		final SortedMap<String, List<Double>> trafficGbpsTraversingVnf_type_n;
		final OpticalSpectrumManager osm;
		JltTecnoec1_capacityPlanningAlgorithm algorithm;

		public Statistics(WNet wNet, JltTecnoec1_capacityPlanningAlgorithm algorithm , OpticalSpectrumManager osm)
		{
			super();
			this.wNet = wNet;
			this.algorithm = algorithm;
			this.osm = osm;
			this.fsuPerLink_e = wNet.getFibers().stream().map(e -> getFsu(e)).collect(Collectors.toList());
			this.indexesCoreOrAggType_e = wNet.getFibers().stream().map(e-> getFiberCoreOrAggregationType(e)).collect(Collectors.toList());
			this.newNodeDegree_n = wNet.getNodes().stream().map(e-> getNewNodeDegree(e)).collect(Collectors.toList());
			this.numOccupiedSlotsPerLink_e = wNet.getFibers().stream ().map(e->getNumSlotsOccupied(e)).collect(Collectors.toList());
			this.numTransponders_type_n = getTrafficUsedbyTranspondersPerNode(wNet);
			this.trafficGbpsTraversingVnf_type_n = getTotalofTrafficPerVNFTypeAndPerNode(wNet);

		}

		public List<Integer> getFsuPerFiber()
		{
			return Collections.unmodifiableList(this.fsuPerLink_e);
		}

		public List<Integer> getCoreOrAggTypePerFiber()
        {
            return Collections.unmodifiableList(this.indexesCoreOrAggType_e);
        }

		public List<Integer> getFinalDegreePerNode()
        {
            return Collections.unmodifiableList(this.newNodeDegree_n);
        }

		public List<Integer> getNumSlotsOccupiedOrWastedPerFiber()
		{
			return Collections.unmodifiableList(this.numOccupiedSlotsPerLink_e);
		}

		public SortedMap<String, List<Integer>> getTranspoderTrafficPerNode()
		{
			return Collections.unmodifiableSortedMap(this.numTransponders_type_n);
		}

		public SortedMap<String, List<Double>> getVNFsTrafficPerNode()
		{
			return Collections.unmodifiableSortedMap(this.trafficGbpsTraversingVnf_type_n);
		}

		public int getFsu(WFiber fiber)
		{
			return this.osm.getOccupiedOpticalSlotIds(fiber).stream().mapToInt(e -> e).max().orElse(0);
			// return fiber.getTraversingLps().stream().mapToInt (lp->lp.getOpticalSlotIds().size()).max().orElse(0);
		}
		public int getNumSlotsOccupied (WFiber fiber)
		{
			return this.osm.getOccupiedOpticalSlotIds(fiber).size();
		}

		public SortedMap<String, List<Integer>> getTrafficUsedbyTranspondersPerNode(WNet wNet)
		{
			final List<Quintuple<String, Double, Double, Double, Integer>> listAmenTransponders = algorithm.getAmenTransponders();
			final List<Quintuple<String, Double, Double, Double, Integer>> listMcenTransponders = algorithm.getMcenTransponders();

			SortedMap<String, List<Integer>> res = new TreeMap<>();

			for (Quintuple<String, Double, Double, Double, Integer> tp : listAmenTransponders)
				res.put(tp.getFirst(), getTransponderLineRateUsedPerNode(wNet, tp.getFirst()));

			for (Quintuple<String, Double, Double, Double, Integer> tp : listMcenTransponders)
				res.put(tp.getFirst(), getTransponderLineRateUsedPerNode(wNet, tp.getFirst()));

			return res;
		}

		public static List<Integer> getTransponderLineRateUsedPerNode(WNet net, String tpName)
		{
			List<Integer> res = new ArrayList<>();

			for (WNode n : net.getNodes())
			{
				int accumulateLpsPerTransponder = 0;
				for (WLightpathRequest lpr : n.getOutgoingLigtpathRequests())
				{
					if (lpr.getTransponderName().get().equals(tpName)) accumulateLpsPerTransponder++;
				}
				res.add(accumulateLpsPerTransponder);
			}

			return res;
		}

		public SortedMap<String, List<Double>> getTotalofTrafficPerVNFTypeAndPerNode(WNet wNet)
		{
			SortedMap<String, List<Double>> res = new TreeMap<>();

			for (WVnfType vnf : wNet.getVnfTypes())
				res.put(vnf.getVnfTypeName(), getTrafficInGbpsofVNFTypePerNode(wNet, vnf.getVnfTypeName()));

			return res;
		}

		public static List<Double> getTrafficInGbpsofVNFTypePerNode(WNet net, String vnfName)
		{
			List<Double> res = new ArrayList<>();

			for (WNode n : net.getNodes())
			{
				double accumulateTrafficPerVNF = 0;
				for (WVnfInstance vnfi : n.getVnfInstances())
				{
					if (vnfi.getVnfType().isPresent()) if (vnfi.getVnfType().get().getVnfTypeName().equals(vnfName)) accumulateTrafficPerVNF += vnfi.getOccupiedCapacityInGbps();
				}
				res.add(accumulateTrafficPerVNF);
			}

			return res;
		}

		public void storeStatsAsIntegerList(List<Integer> list, String fileName)
		{
			File file = new File(fileName);
            try
            {
                if (!file.exists()) file.createNewFile();
                FileWriter fw = new FileWriter(file);
                BufferedWriter bw = new BufferedWriter(fw);
                for (int i = 0; i < list.size(); i++)
                    bw.write(list.get(i).toString() + " ");
                bw.flush();
                bw.close();
            } catch (IOException e)
            {
                e.printStackTrace();
                throw new Net2PlanException("Error writing the file");
            }
		}

		public void storeStatsAsDoubleMap(SortedMap<String, List<Double>> list, String fileName)
		{
			File file = new File(fileName);
			try
			{
				if (!file.exists()) file.createNewFile();
				FileWriter fw = new FileWriter(file);
				BufferedWriter bw = new BufferedWriter(fw);
				for (String key : list.keySet())
				{
					bw.write(key + " ");
					for (int i = 0; i < list.get(key).size(); i++)
						bw.write(list.get(key).get(i).toString() + " ");
					bw.write("\n");
				}
				bw.flush();
				bw.close();
			} catch (IOException e)
			{
				e.printStackTrace();
			}
		}

		public void storeStatsAsIntegerMap(SortedMap<String, List<Integer>> list, String fileName)
		{
			File file = new File(fileName);
			try
			{
				if (!file.exists()) file.createNewFile();
				FileWriter fw = new FileWriter(file);

				BufferedWriter bw = new BufferedWriter(fw);

				for (String key : list.keySet())
				{
					bw.write(key + " ");
					for (int i = 0; i < list.get(key).size(); i++)
						bw.write(list.get(key).get(i).toString() + " ");
					bw.write("\n");
				}
				bw.flush();
				bw.close();
			} catch (IOException e)
			{
				e.printStackTrace();
			}
		}

        private int getFiberCoreOrAggregationType(WFiber fiber) {
            int res = 0;

            if(NODETYPE.isAmen(fiber.getA()) || NODETYPE.isAmen(fiber.getB()))
                res = 1;


            return res;
        }

        private int getNewNodeDegree(WNode node) {

            return node.getIncomingFibers().stream().mapToInt(e->(int) Math.ceil((1+getFsu(e))/ 320)).sum();

        }


	}
}