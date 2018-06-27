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

package com.net2plan.research.metrohaul.networkModel;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.research.metrohaul.networkModel.ExcelImporterConstants.COLUMNS_FIBERSTAB;
import com.net2plan.research.metrohaul.networkModel.ExcelImporterConstants.COLUMNS_NODESTAB;
import com.net2plan.research.metrohaul.networkModel.ExcelImporterConstants.COLUMNS_PERNODEANDSERVICETIMEINTENSITYGBPS;
import com.net2plan.research.metrohaul.networkModel.ExcelImporterConstants.COLUMNS_USERSERVICES;
import com.net2plan.research.metrohaul.networkModel.ExcelImporterConstants.COLUMNS_VNFTYPES;
import com.net2plan.utils.Pair;

/** This class is an importer for the metro network
 */
public class ImportMetroNetwork
{
	/** Creates a network object, importing the data from the given Excel file
	 * @param excelFile  see above
	 * @return see above
	 */
	public static WNet importFromExcelFile (File excelFile)
    {
        final SortedMap<String, Object[][]> fileData = new TreeMap<>(ExcelReader.readFile(excelFile));
        final WNet net = WNet.createEmptyDesign ();
        
        /* Nodes sheet */
        System.out.println("###################### Reading Nodes sheet ######################");
        Object[][] sheet = fileData.get(ExcelImporterConstants.EXCELSHEETS.NODES.getTabName());
        if (sheet == null) throw new Net2PlanException ("Cannot read the excel sheet");
        for (int i = 1; i < sheet.length; i++)
        {
        	System.out.println("-------------------------------------------------");
        	System.out.println("Row number "+i);
        	System.out.println();
        	final Object[] thisRowData = sheet [i];
        	final String name = readString (thisRowData , COLUMNS_NODESTAB.NODEUNIQUENAME.ordinal());
        	System.out.println("Name loaded: "+name);
        	final String type = readString (thisRowData , COLUMNS_NODESTAB.NODETYPESTRING.ordinal(), "");
        	System.out.println("Type loaded: "+type);
        	final double xCoord = readDouble (thisRowData , COLUMNS_NODESTAB.POSITIONLONGITUDE_DEGREEES.ordinal());
        	System.out.println("xCoord loaded: "+xCoord);
        	final double yCoord = readDouble (thisRowData , COLUMNS_NODESTAB.POSITIONLATITUDE_DEGREES.ordinal());
        	System.out.println("yCoord loaded: "+yCoord);
        	final boolean isConnectedToCoreNode = readBoolean(thisRowData, COLUMNS_NODESTAB.ISCONNECTEDTOCORENODE.ordinal()); 
        	System.out.println("isConnectedToCoreNode loaded: "+isConnectedToCoreNode);
        	final double nodeBasePopulation = readDouble (thisRowData , COLUMNS_NODESTAB.NODEBASEPOPULATION.ordinal());
        	System.out.println("nodeBasePopulation loaded: "+nodeBasePopulation);
        	final double nodeCpus = readDouble (thisRowData , COLUMNS_NODESTAB.TOTALNUMCPUS.ordinal(), 0.0);
        	System.out.println("nodeCpus loaded: "+nodeCpus);
        	final double nodeRamGb = readDouble (thisRowData , COLUMNS_NODESTAB.TOTALRAM_GB.ordinal(), 0.0);
        	System.out.println("nodeRamGb loaded: "+nodeRamGb);
        	final double nodeHdGb = readDouble (thisRowData , COLUMNS_NODESTAB.TOTALHD_GB.ordinal(), 0.0);
        	System.out.println("nodeHdGb loaded: "+nodeHdGb);
        	final String arbitraryParamsString = readString (thisRowData , COLUMNS_NODESTAB.ARBITRARYPARAMS.ordinal() , "");
        	System.out.println("arbitraryParamsString loaded: "+arbitraryParamsString);
        	
        	//if(!type.equals("CoreMetro") && !type.equals("EdgeMetro")) throw new Net2PlanException ("Unkown node type: "+type+". Only CoreMetro and EdgeMetro are valid names");
        	
        	final WNode n = net.addNode(xCoord, yCoord, name, type);
        	n.setIsConnectedToNetworkCore(isConnectedToCoreNode);
        	n.setPoputlation(nodeBasePopulation);
        	n.setTotalNumCpus(nodeCpus);
        	n.setTotalRamGB(nodeRamGb);
        	n.setTotalHdGB(nodeHdGb);
        	n.setArbitraryParamString(arbitraryParamsString);
        }
        

        /* Fibers sheet */
        System.out.println("###################### Reading Fibers sheet ######################");
        sheet = fileData.get(ExcelImporterConstants.EXCELSHEETS.FIBERS.getTabName());
        if (sheet == null) throw new Net2PlanException ("Cannot read the excel sheet");
        for (int i = 1; i < sheet.length; i++)
        {
        	System.out.println("-------------------------------------------------");
        	System.out.println("Row number "+i);
        	System.out.println();
        	final Object[] thisRowData = sheet [i];
        	final String ORIGINNODEUNIQUENAME = readString (thisRowData , COLUMNS_FIBERSTAB.ORIGINNODEUNIQUENAME.ordinal());
        	System.out.println("ORIGINNODEUNIQUENAME loaded: "+ORIGINNODEUNIQUENAME);
        	final String DESTINATIONNODEUNIQUENAME = readString (thisRowData , COLUMNS_FIBERSTAB.DESTINATIONNODEUNIQUENAME.ordinal());
        	System.out.println("DESTINATIONNODEUNIQUENAME loaded: "+DESTINATIONNODEUNIQUENAME);
        	final double LENGTH_KM = readDouble (thisRowData , COLUMNS_FIBERSTAB.LENGTH_KM.ordinal());
        	System.out.println("LENGTH_KM loaded: "+LENGTH_KM);
        	final boolean ISBIDIRECTIONAL = readBoolean(thisRowData, COLUMNS_FIBERSTAB.ISBIDIRECTIONAL.ordinal()); 
        	System.out.println("ISBIDIRECTIONAL loaded: "+ISBIDIRECTIONAL);
        	final List<Double> VALIDOPTICALSLOTRANGES = readDoubleList(thisRowData , COLUMNS_FIBERSTAB.VALIDOPTICALSLOTRANGES.ordinal() , WNetConstants.LISTSEPARATORANDINVALIDNAMECHARACTER, WNetConstants.WFIBER_DEFAULT_VALIDOPTICALSLOTRANGES);
        	System.out.println("VALIDOPTICALSLOTRANGES loaded: "+VALIDOPTICALSLOTRANGES);
        	final double FIBERATTENUATIONCOEFFICIENT_DBPERKM = readDouble (thisRowData , COLUMNS_FIBERSTAB.FIBERATTENUATIONCOEFFICIENT_DBPERKM.ordinal(), WNetConstants.WFIBER_DEFAULT_ATTCOEFFICIENTDBPERKM);
        	System.out.println("FIBERATTENUATIONCOEFFICIENT_DBPERKM loaded: "+FIBERATTENUATIONCOEFFICIENT_DBPERKM);
        	final double FIBERCHROMATICDISPERSIONCOEFFICIENT_PSPERNMPERKM = readDouble (thisRowData , COLUMNS_FIBERSTAB.FIBERCHROMATICDISPERSIONCOEFFICIENT_PSPERNMPERKM.ordinal(),WNetConstants.WFIBER_DEFAULT_CDCOEFF_PSPERNMKM);
        	System.out.println("FIBERCHROMATICDISPERSIONCOEFFICIENT_PSPERNMPERKM loaded: "+FIBERCHROMATICDISPERSIONCOEFFICIENT_PSPERNMPERKM);
        	final double FIBERLINKDESIGNVALUEPMD_PSPERSQRKM = readDouble (thisRowData , COLUMNS_FIBERSTAB.FIBERLINKDESIGNVALUEPMD_PSPERSQRKM.ordinal(),WNetConstants.WFIBER_DEFAULT_PMDCOEFF_PSPERSQRKM);
        	System.out.println("FIBERLINKDESIGNVALUEPMD_PSPERSQRKM loaded: "+FIBERLINKDESIGNVALUEPMD_PSPERSQRKM);
        	final List<Double> AMPLIFIERSPOSITIONFROMORIGIN_KM = readDoubleList(thisRowData , COLUMNS_FIBERSTAB.AMPLIFIERSPOSITIONFROMORIGIN_KM.ordinal() , WNetConstants.LISTSEPARATORANDINVALIDNAMECHARACTER,WNetConstants.WFIBER_DEFAULT_AMPLIFIERPOSITION);
        	System.out.println("AMPLIFIERSPOSITIONFROMORIGIN_KM loaded: "+AMPLIFIERSPOSITIONFROMORIGIN_KM);
        	final List<Double> AMPLIFIERGAINS_DB = readDoubleList(thisRowData , COLUMNS_FIBERSTAB.AMPLIFIERGAINS_DB.ordinal() , WNetConstants.LISTSEPARATORANDINVALIDNAMECHARACTER,WNetConstants.WFIBER_DEFAULT_AMPLIFIERGAIN_DB);
        	System.out.println("AMPLIFIERGAINS_DB loaded: "+AMPLIFIERGAINS_DB);
        	final List<Double> AMPLIFIERNOISEFACTOR_DB = readDoubleList(thisRowData , COLUMNS_FIBERSTAB.AMPLIFIERNOISEFACTOR_DB.ordinal() , WNetConstants.LISTSEPARATORANDINVALIDNAMECHARACTER,WNetConstants.WFIBER_DEFAULT_AMPLIFIERNOISEFACTOR_DB);
        	System.out.println("AMPLIFIER_NOISE FACTOR loaded: "+AMPLIFIERNOISEFACTOR_DB);
        	final List<Double> AMPLIFIERPMD_PS = readDoubleList(thisRowData , COLUMNS_FIBERSTAB.AMPLIFIERPMD_PS.ordinal() , WNetConstants.LISTSEPARATORANDINVALIDNAMECHARACTER, WNetConstants.WFIBER_DEFAULT_AMPLIFIERPMD_PS);
        	System.out.println("AMPLIFIERPMD_PS loaded: "+AMPLIFIERPMD_PS);
        	final String arbitraryParamsString = readString (thisRowData , COLUMNS_FIBERSTAB.ARBITRARYPARAMS.ordinal() ,"");
        	System.out.println("arbitraryParamsString loaded: "+arbitraryParamsString);
        	
        	final WNode a = net.getNodeByName(ORIGINNODEUNIQUENAME).orElseThrow(()->new Net2PlanException ("Unkown node name: " + ORIGINNODEUNIQUENAME));
        	final WNode b = net.getNodeByName(DESTINATIONNODEUNIQUENAME).orElseThrow(()->new Net2PlanException ("Unkown node name: " + DESTINATIONNODEUNIQUENAME));
        	
        	final List<Integer> validOpticalSlotRanges = VALIDOPTICALSLOTRANGES.stream().map(d->d.intValue()).collect(Collectors.toList());
        	final Pair<WFiber,WFiber> pair = net.addFiber(a, b, validOpticalSlotRanges, LENGTH_KM, ISBIDIRECTIONAL);
        	for (WFiber e : Arrays.asList(pair.getFirst() , pair.getSecond()))
        	{
        		if (e == null) continue;
            	e.setAttenuationCoefficient_dbPerKm(FIBERATTENUATIONCOEFFICIENT_DBPERKM);
            	e.setChromaticDispersionCoeff_psPerNmKm(FIBERCHROMATICDISPERSIONCOEFFICIENT_PSPERNMPERKM);
            	e.setPmdLinkDesignValueCoeff_psPerSqrtKm(FIBERLINKDESIGNVALUEPMD_PSPERSQRKM);
            	e.setAmplifiersTraversed_dB(AMPLIFIERSPOSITIONFROMORIGIN_KM, AMPLIFIERGAINS_DB, AMPLIFIERNOISEFACTOR_DB, AMPLIFIERPMD_PS);
            	e.setArbitraryParamString(arbitraryParamsString);
        	}
        }
        
        /* VNF types sheet */
        System.out.println("###################### Reading VNF types sheet ######################");
        sheet = fileData.get(ExcelImporterConstants.EXCELSHEETS.VNFTYPES.getTabName());
        if (sheet == null) throw new Net2PlanException ("Cannot read the excel sheet");
        for (int i = 1; i < sheet.length; i++)
        {
        	System.out.println("-------------------------------------------------");
        	System.out.println("Row number "+i);
        	System.out.println();
        	final Object[] thisRowData = sheet [i];
        	final String VNFTYPEUNIQUENAME = readString (thisRowData , COLUMNS_VNFTYPES.VNFTYPEUNIQUENAME.ordinal(), "");
        	System.out.println("VNFTYPEUNIQUENAME loaded: "+VNFTYPEUNIQUENAME);
        	final double VNFINSTANCECAPACITY_GBPS = readDouble (thisRowData , COLUMNS_VNFTYPES.VNFINSTANCECAPACITY_GBPS.ordinal(), 0.0);
        	System.out.println("VNFINSTANCECAPACITY_GBPS loaded: "+VNFINSTANCECAPACITY_GBPS);
        	final double OCCUPCPU = readDouble (thisRowData , COLUMNS_VNFTYPES.OCCUPCPU.ordinal(), 0.0);
        	System.out.println("OCCUPCPU loaded: "+OCCUPCPU);
        	final double OCCUPRAM_GB = readDouble (thisRowData , COLUMNS_VNFTYPES.OCCUPRAM_GB.ordinal(), 0.0);
        	System.out.println("OCCUPRAM_GB loaded: "+OCCUPRAM_GB);
        	final double OCCUPHD_GB = readDouble (thisRowData , COLUMNS_VNFTYPES.OCCUPHD_GB.ordinal(), 0.0);
        	System.out.println("OCCUPHD_GB loaded: "+OCCUPHD_GB);
        	final double PROCESSINGTIME_MS = readDouble (thisRowData , COLUMNS_VNFTYPES.PROCESSINGTIME_MS.ordinal(), 0.0);
        	System.out.println("PROCESSINGTIME_MS loaded: "+PROCESSINGTIME_MS);
        	final boolean ISCONSTRAINEDITSPLACEMENTTOSOMENODES = readBoolean(thisRowData, COLUMNS_VNFTYPES.ISCONSTRAINEDITSPLACEMENTTOSOMENODES.ordinal()); 
        	System.out.println("ISCONSTRAINEDITSPLACEMENTTOSOMENODES loaded: "+ISCONSTRAINEDITSPLACEMENTTOSOMENODES);
        	final List<String> LISTUNIQUENODENAMESOFNODESVALIDFORINSTANTIATION = readStringList(thisRowData , COLUMNS_VNFTYPES.LISTUNIQUENODENAMESOFNODESVALIDFORINSTANTIATION.ordinal() , WNetConstants.LISTSEPARATORANDINVALIDNAMECHARACTER);
        	System.out.println("LISTUNIQUENODENAMESOFNODESVALIDFORINSTANTIATION loaded: "+LISTUNIQUENODENAMESOFNODESVALIDFORINSTANTIATION);
        	final String arbitraryParamsString = readString (thisRowData , COLUMNS_VNFTYPES.ARBITRARYPARAMS.ordinal() , "");
        	System.out.println("arbitraryParamsString loaded: "+arbitraryParamsString);
    		
        	for(String node : new TreeSet<> (LISTUNIQUENODENAMESOFNODESVALIDFORINSTANTIATION)) {
    			net.getNodeByName(node).orElseThrow(()->new Net2PlanException ("Unkown node name: " + node));
    		}
        	
        	final WVnfType vnfType = new WVnfType(VNFTYPEUNIQUENAME, 
        			VNFINSTANCECAPACITY_GBPS, 
        					OCCUPCPU, OCCUPRAM_GB, OCCUPHD_GB, PROCESSINGTIME_MS , 
        					ISCONSTRAINEDITSPLACEMENTTOSOMENODES, 
        					new TreeSet<> (LISTUNIQUENODENAMESOFNODESVALIDFORINSTANTIATION), 
        			arbitraryParamsString);
        	net.addOrUpdateVnfType(vnfType);
        }

        /* UserService sheet */
        System.out.println("###################### Reading UserService sheet ######################");
        sheet = fileData.get(ExcelImporterConstants.EXCELSHEETS.USERSERVICES.getTabName());
        if (sheet == null) throw new Net2PlanException ("Cannot read the excel sheet");
        for (int i = 1; i < sheet.length; i++)
        {
        	System.out.println("-------------------------------------------------");
        	System.out.println("Row number "+i);
        	System.out.println();
        	final Object[] thisRowData = sheet [i];
        	final String UNIQUEIDSTRING = readString (thisRowData , COLUMNS_USERSERVICES.UNIQUEIDSTRING.ordinal());
        	System.out.println("UNIQUEIDSTRING loaded: "+UNIQUEIDSTRING);
        	final List<String> LISTVNFTYPESCOMMASEPARATED_UPSTREAM = readStringList(thisRowData , COLUMNS_USERSERVICES.LISTVNFTYPESCOMMASEPARATED_UPSTREAM.ordinal() , WNetConstants.LISTSEPARATORANDINVALIDNAMECHARACTER);
        	System.out.println("LISTVNFTYPESCOMMASEPARATED_UPSTREAM loaded: "+LISTVNFTYPESCOMMASEPARATED_UPSTREAM);
        	final List<String> LISTVNFTYPESCOMMASEPARATED_DOWNSTREAM = readStringList(thisRowData , COLUMNS_USERSERVICES.LISTVNFTYPESCOMMASEPARATED_DOWNSTREAM.ordinal() , WNetConstants.LISTSEPARATORANDINVALIDNAMECHARACTER);
        	System.out.println("LISTVNFTYPESCOMMASEPARATED_DOWNSTREAM loaded: "+LISTVNFTYPESCOMMASEPARATED_DOWNSTREAM);
        	final List<Double> SEQUENCETRAFFICEXPANSIONFACTORRESPECTTOINITIAL_UPSTREAM = readDoubleList(thisRowData , COLUMNS_USERSERVICES.SEQUENCETRAFFICEXPANSIONFACTORRESPECTTOINITIAL_UPSTREAM.ordinal() , WNetConstants.LISTSEPARATORANDINVALIDNAMECHARACTER);
        	System.out.println("SEQUENCETRAFFICEXPANSIONFACTORRESPECTTOINITIAL_UPSTREAM loaded: "+SEQUENCETRAFFICEXPANSIONFACTORRESPECTTOINITIAL_UPSTREAM);
        	final List<Double> SEQUENCETRAFFICEXPANSIONFACTORRESPECTTOINITIAL_DOWNSTREAM = readDoubleList(thisRowData , COLUMNS_USERSERVICES.SEQUENCETRAFFICEXPANSIONFACTORRESPECTTOINITIAL_DOWNSTREAM.ordinal() , WNetConstants.LISTSEPARATORANDINVALIDNAMECHARACTER);
        	System.out.println("SEQUENCETRAFFICEXPANSIONFACTORRESPECTTOINITIAL_DOWNSTREAM loaded: "+SEQUENCETRAFFICEXPANSIONFACTORRESPECTTOINITIAL_DOWNSTREAM);
        	final List<Double> LISTMAXLATENCYFROMINITIALTOVNFSTART_MS_UPSTREAM = readDoubleList(thisRowData , COLUMNS_USERSERVICES.LISTMAXLATENCYFROMINITIALTOVNFSTART_MS_UPSTREAM.ordinal() , WNetConstants.LISTSEPARATORANDINVALIDNAMECHARACTER);
        	System.out.println("LISTMAXLATENCYFROMINITIALTOVNFSTART_MS_UPSTREAM loaded: "+LISTMAXLATENCYFROMINITIALTOVNFSTART_MS_UPSTREAM);
        	final List<Double> LISTMAXLATENCYFROMINITIALTOVNFSTART_MS_DOWNSTREAM = readDoubleList(thisRowData , COLUMNS_USERSERVICES.LISTMAXLATENCYFROMINITIALTOVNFSTART_MS_DOWNSTREAM.ordinal() , WNetConstants.LISTSEPARATORANDINVALIDNAMECHARACTER);
        	System.out.println("LISTMAXLATENCYFROMINITIALTOVNFSTART_MS_DOWNSTREAM loaded: "+LISTMAXLATENCYFROMINITIALTOVNFSTART_MS_DOWNSTREAM);
        	final double INJECTIONDOWNSTREAMEXPANSIONFACTORRESPECTTOINITIALUPSTREAM = readDouble (thisRowData , COLUMNS_USERSERVICES.INJECTIONDOWNSTREAMEXPANSIONFACTORRESPECTTOINITIALUPSTREAM.ordinal());
        	System.out.println("INJECTIONDOWNSTREAMEXPANSIONFACTORRESPECTTOINITIALUPSTREAM loaded: "+INJECTIONDOWNSTREAMEXPANSIONFACTORRESPECTTOINITIALUPSTREAM);
        	final boolean ISENDINGINCORENODE = readBoolean(thisRowData, COLUMNS_USERSERVICES.ISENDINGINCORENODE.ordinal()); 
        	System.out.println("ISENDINGINCORENODE loaded: "+ISENDINGINCORENODE);
        	final String arbitraryParamString = readString (thisRowData , COLUMNS_USERSERVICES.ARBITRARYPARAMS.ordinal() , "");
        	System.out.println("arbitraryParamString loaded: "+arbitraryParamString);
        	   
        	for(String vnfType : new TreeSet<> (LISTVNFTYPESCOMMASEPARATED_UPSTREAM)) {
            	net.getVnfTypeNames().stream().filter(n->n.equals(vnfType)).findFirst().orElseThrow(()->new Net2PlanException ("Unkown VNF type: " + vnfType));

    		}
        	for(String vnfType : new TreeSet<> (LISTVNFTYPESCOMMASEPARATED_DOWNSTREAM)) {
            	net.getVnfTypeNames().stream().filter(n->n.equals(vnfType)).findFirst().orElseThrow(()->new Net2PlanException ("Unkown VNF type: " + vnfType));

    		}
        	
        	final int numberVNFs = LISTVNFTYPESCOMMASEPARATED_UPSTREAM.size();
        	
        	if(LISTVNFTYPESCOMMASEPARATED_DOWNSTREAM.size() != numberVNFs ||
        			SEQUENCETRAFFICEXPANSIONFACTORRESPECTTOINITIAL_UPSTREAM.size() != numberVNFs ||
        			SEQUENCETRAFFICEXPANSIONFACTORRESPECTTOINITIAL_DOWNSTREAM.size() != numberVNFs ||
        			LISTMAXLATENCYFROMINITIALTOVNFSTART_MS_UPSTREAM.size() != numberVNFs + 1 ||
        			LISTMAXLATENCYFROMINITIALTOVNFSTART_MS_DOWNSTREAM.size() != numberVNFs + 1) throw new Net2PlanException ("Wrong number of values in UserServices sheet.");
        	        	
        	
        	final WUserService userService = new WUserService(UNIQUEIDSTRING, LISTVNFTYPESCOMMASEPARATED_UPSTREAM,
        			LISTVNFTYPESCOMMASEPARATED_DOWNSTREAM,
        			SEQUENCETRAFFICEXPANSIONFACTORRESPECTTOINITIAL_UPSTREAM,
        			SEQUENCETRAFFICEXPANSIONFACTORRESPECTTOINITIAL_DOWNSTREAM,
        			LISTMAXLATENCYFROMINITIALTOVNFSTART_MS_UPSTREAM,
        			LISTMAXLATENCYFROMINITIALTOVNFSTART_MS_DOWNSTREAM,
        			INJECTIONDOWNSTREAMEXPANSIONFACTORRESPECTTOINITIALUPSTREAM, 
        			ISENDINGINCORENODE , 
        			arbitraryParamString);
        	net.addOrUpdateUserService(userService);	
        }
        
        /* Per node and service time intensity sheet */
        System.out.println("###################### Reading Per node and service time intensity sheet ######################");
        sheet = fileData.get(ExcelImporterConstants.EXCELSHEETS.PERNODEANDSERVICETIMETRAFFIC.getTabName());
        if (sheet == null) throw new Net2PlanException ("Cannot read the excel sheet");
        for (int i = 1; i < sheet.length; i++)
        {
        	System.out.println("-------------------------------------------------");
        	System.out.println("Row number "+i);
        	System.out.println();
        	final Object[] thisRowData = sheet [i];
        	final String serviceChainInjectionNodeUniqueName = readString (thisRowData , COLUMNS_PERNODEANDSERVICETIMEINTENSITYGBPS.INJECTIONNODEUIQUENAME.ordinal());
        	System.out.println("serviceChainInjectionNodeUniqueName loaded: "+serviceChainInjectionNodeUniqueName);
        	final String serviceChainUserServiceUniqueName = readString (thisRowData , COLUMNS_PERNODEANDSERVICETIMEINTENSITYGBPS.USERSERVICEUNIQUEID.ordinal());
        	System.out.println("serviceChainUserServiceUniqueName loaded: "+serviceChainUserServiceUniqueName);
        	final WUserService userService = net.getUserServicesInfo().getOrDefault(serviceChainUserServiceUniqueName, null);
        	final WNode userInjectionNode = net.getNodeByName(serviceChainInjectionNodeUniqueName).orElse(null);
    		
        	net.getNodeByName(serviceChainInjectionNodeUniqueName).orElseThrow(()->new Net2PlanException ("Unkown node name: " + serviceChainInjectionNodeUniqueName));
    		net.getUserServiceNames().stream().filter(n->n.equals(serviceChainUserServiceUniqueName)).findFirst().orElseThrow(()->new Net2PlanException ("Unkown service: " + serviceChainUserServiceUniqueName));
        	
    		if (userService == null || userInjectionNode == null) { System.out.println("Not readable row: " + serviceChainInjectionNodeUniqueName + " ; " + serviceChainUserServiceUniqueName); continue; }
        	final List<Pair<String,Double>> intervalNameAndTrafficUpstream_Gbps = new ArrayList<> ();
        	for (int col = 2 ; col < thisRowData.length ; col ++)
        	{
            	final String timeSlotName = readString (sheet [0] , col , "");
            	final Double trafficUpstreamInitialGbps = readDouble(thisRowData , col , 0.0);
            	intervalNameAndTrafficUpstream_Gbps.add(Pair.of(timeSlotName, trafficUpstreamInitialGbps));
            	System.out.println("timeSlotName: "+timeSlotName+", "+"trafficUpstreamInitialGbps: "+trafficUpstreamInitialGbps);
        	}
        	final WServiceChainRequest upstreamScReq = net.addServiceChainRequest(userInjectionNode, true, userService);
        	upstreamScReq.setTimeSlotNameAndInitialInjectionIntensityInGbpsList(intervalNameAndTrafficUpstream_Gbps);
        	final double injectionDownstreamExpansionFactorRespecToBaseTrafficUpstream = userService.getInjectionDownstreamExpansionFactorRespecToBaseTrafficUpstream(); 
        	if (injectionDownstreamExpansionFactorRespecToBaseTrafficUpstream > 0)
        	{
            	final WServiceChainRequest downstreamScReq = net.addServiceChainRequest(userInjectionNode, false, userService);
            	final List<Pair<String,Double>> intervalNameAndTrafficDownstream_Gbps = intervalNameAndTrafficUpstream_Gbps.stream().map(p->Pair.of(p.getFirst(), injectionDownstreamExpansionFactorRespecToBaseTrafficUpstream * p.getSecond())).collect(Collectors.toList());
            	downstreamScReq.setTimeSlotNameAndInitialInjectionIntensityInGbpsList(intervalNameAndTrafficDownstream_Gbps);
        	}
        }
        return net;
    }

	
	private static double readDouble (Object [] cells , int index , Double...defaultVal)
	{
		if (index >= cells.length) return defaultVal[0];
		if (cells [index] == null) if (defaultVal.length > 0) return defaultVal[0]; else throw new Net2PlanException ("Cell unkown instance " + (cells[index]).getClass().getName());
		if (cells [index] instanceof Number) return ((Number) cells[index]).doubleValue();
		if (cells [index] instanceof String) return Double.parseDouble((String) cells[index]);
		if (defaultVal.length > 0) return defaultVal[0]; else throw new Net2PlanException ("Cell unkown instance " + (cells[index]).getClass().getName());	
	}
	private static int readInt (Object [] cells , int index)
	{
		if (index >= cells.length) throw new Net2PlanException ("Unexisting cell of column: " + index + ". Num columns in this row: " + cells.length);
		if (cells [index] == null) return 0;
		if (cells [index] instanceof Number) return ((Number) cells[index]).intValue();
		if (cells [index] instanceof String) return Integer.parseInt((String) cells[index]);
		throw new Net2PlanException ("Cell unkown instance " + (cells[index]).getClass().getName());
	}
	private static String readString (Object [] cells , int index , String... defaultVal)
	{
		if (index >= cells.length) return defaultVal[0];
		if (cells [index] == null) if (defaultVal.length > 0) return defaultVal[0]; else throw new Net2PlanException("Cell unkown instance " + (cells[index]).getClass().getName());
		if (cells [index] instanceof Number) return ((Number) cells[index]).toString();
		if (cells [index] instanceof String) return (String) cells[index];
		if (defaultVal.length > 0) return defaultVal[0]; else throw new Net2PlanException("Cell unkown instance " + (cells[index]).getClass().getName());	}
	private static boolean readBoolean (Object [] cells , int index)
	{
		return readDouble (cells , index) != 0;
	}
	/*JLRG 12/06*/
	private static List<Integer> readIntegerList (Object [] cells , int index , String separator)
	{
		final String st = readString (cells , index);
		return Arrays.asList(st.split(separator)).stream().map(s->s.trim()).map(s->Integer.parseInt(s)).collect(Collectors.toCollection(ArrayList::new));

	}
	
	private static List<Double> readDoubleList (Object [] cells , int index , String separator,List<Double>... defaultVal)
	{
		final String st = readString (cells , index,"");
		if( st.length() == 0) return defaultVal[0];
		else return Arrays.asList(st.split(separator)).stream().map(s->s.trim()).map(s->Double.parseDouble(s)).collect(Collectors.toCollection(ArrayList::new));
	}
	private static List<String> readStringList (Object [] cells , int index , String separator)
	{
		final String st = readString (cells , index);
		return Arrays.asList(st.split(separator)).stream().map(s->s.trim()).collect(Collectors.toCollection(ArrayList::new));
	}

}

