package com.net2plan.examples.general.onlineSim;
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


import com.net2plan.examples.ocnbook.onlineSim.Online_evGen_generalGenerator;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.simulation.SimEvent;
import com.net2plan.libraries.SRGUtils;
import com.net2plan.utils.InputParameter;
import com.net2plan.utils.Triple;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



/** 
 * Generates events for an IP over WDM multilayer network, with an IP/OSPF layer on top of a WDM layer where lightpaths are carried in a fixed grid of wavelengths
 * 
 * This class extends the {@code Online_evGen_generalGenerator} generator (see its Javadoc for further details), and is basically used to:
 * <ul>
 * <li>Send events to the IP layer to create used-defined IP traffic fluctuations (fast/slow), according to the general operation of the {@code Online_evGen_generalGenerator} module.</li>
 * <li>Send events related to node/link failures and repairs, also according to how the {@code Online_evGen_generalGenerator} module operates.</li>
 * </ul>
 *  
 * See the technology conventions used in Net2Plan built-in algorithms and libraries to represent IP and WDM networks. 
 * @net2plan.keywords IP/OSPF, WDM, Multilayer, Network recovery: protection, Network recovery: restoration, Multihour optimization
 * @net2plan.inputParameters 
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 */
public class Online_evGen_ipOverWdm extends Online_evGen_generalGenerator
{
	private final static String DATE_FORMAT = "MM/dd/YY HH:mm:ss";

	private InputParameter ipOverWdmFailureModel = new InputParameter ("ipOverWdmFailureModel", "#select# perBidirectionalLinkBundle none SRGfromNetPlan perNode perLink perDirectionalLinkBundle" , "Failure model selection: SRGfromNetPlan, perNode, perLink, perDirectionalLinkBundle, perBidirectionalLinkBundle");
	private InputParameter ipOverWdmFailureDefaultMTTFInHours = new InputParameter ("ipOverWdmFailureDefaultMTTFInHours", (double) 10 , "Default value for Mean Time To Fail (hours) (unused when failureModel=SRGfromNetPlan)" , 0 , false , Double.MAX_VALUE , true);
	private InputParameter ipOverWdmFailureDefaultMTTRInHours = new InputParameter ("ipOverWdmFailureDefaultMTTRInHours", (double) 12 , "Default value for Mean Time To Repair (hours) (unused when failureModel=SRGfromNetPlan)" , 0 , false , Double.MAX_VALUE , true);
	private InputParameter ipOverWDmFailureStatisticalPattern = new InputParameter ("ipOverWDmFailureStatisticalPattern", "#select# exponential-iid" , "Type of failure and repair statistical pattern");
	private InputParameter randomSeed = new InputParameter ("randomSeed", (long) 1 , "Seed for the random generator (-1 means random)");
	private InputParameter ipTFFastFluctuationType = new InputParameter ("ipTFFastFluctuationType", "#select# none random-truncated-gaussian" , "");
	private InputParameter ipTFFastTimeBetweenDemandFluctuationsHours = new InputParameter ("ipTFFastTimeBetweenDemandFluctuationsHours", (double) 0.1 , "Average time between two changes of demand offered traffic in a demand (demands behave independently)" , 0 , false , Double.MAX_VALUE , true);
	private InputParameter ipTFFastFluctuationCoefficientOfVariation = new InputParameter ("ipTFFastFluctuationCoefficientOfVariation", (double) 1.0 , "Average time between two changes of demand offered traffic in a demand (demands behave independently)" , 0 , false , Double.MAX_VALUE , true);
	private InputParameter ipTFFastMaximumFluctuationRelativeFactor = new InputParameter ("ipTFFastMaximumFluctuationRelativeFactor", (double) 1.0 , "The fluctuation of a demand cannot exceed this percentage from the media" , 0 , true , Double.MAX_VALUE , true);
	private InputParameter ipTFSlowStartDate = new InputParameter ("ipTFSlowStartDate", new SimpleDateFormat(DATE_FORMAT).format(Calendar.getInstance().getTime()) , "Initial date and time of the simulation");
	private InputParameter ipTFSlowTimeBetweenDemandFluctuationsHours = new InputParameter ("ipTFSlowTimeBetweenDemandFluctuationsHours", (double) 1.0 , "Average time between two changes of demand offered traffic in a demand (demands behave independently)" , 0 , false , Double.MAX_VALUE , true);
	private InputParameter ipTFSlowDefaultTimezone = new InputParameter ("ipTFSlowDefaultTimezone", (int) 0 , "Default timezone with respect to UTC (in range [-12, 12])" , -12 , 12);
	private InputParameter ipTFSlowFluctuationType = new InputParameter ("ipTFSlowFluctuationType", "#select# none time-zone-based" , "");

	@Override
	public String getDescription()
	{
		return "Generates events for an IP over WDM multilayer network, with an IP/OSPF layer on top of a WDM layer where lightpaths are carried in a fixed grid of wavelengths";
	}

	@Override
	public List<Triple<String, String, String>> getParameters()
	{
		/* Returns the parameter information for all the InputParameter objects defined in this object (uses Java reflection) */
		return InputParameter.getInformationAllInputParameterFieldsOfObject(this);
	}

	@Override
	public void initialize(NetPlan initialNetPlan, Map<String, String> algorithmParameters, Map<String, String> simulationParameters, Map<String, String> net2planParameters)
	{
		/* Initialize all InputParameter objects defined in this object (this uses Java reflection) */
		InputParameter.initializeAllInputParameterFieldsOfObject(this, algorithmParameters);

		final NetworkLayer ipLayer = initialNetPlan.getNetworkLayer("IP"); if (ipLayer == null) throw new Net2PlanException ("IP layer not found");
		final NetworkLayer wdmLayer = initialNetPlan.getNetworkLayer("WDM"); if (wdmLayer == null) throw new Net2PlanException ("WDM layer not found");
		if ((ipLayer == wdmLayer) || (initialNetPlan.getNumberOfLayers() != 2)) throw new Net2PlanException ("Wrong layer Ids (or the design does not have two layers)");

		/* Initialize slow changing traffic */
		if (!ipOverWdmFailureModel.getString ().equalsIgnoreCase("none"))
		{
			switch (ipOverWdmFailureModel.getString ())
			{
				case "SRGfromNetPlan":
					break;
				case "perNode":
					SRGUtils.configureSRGs(initialNetPlan, ipOverWdmFailureDefaultMTTFInHours.getDouble(), ipOverWdmFailureDefaultMTTRInHours.getDouble(), SRGUtils.SharedRiskModel.PER_NODE, true , wdmLayer);
					break;
				case "perLink":
					SRGUtils.configureSRGs(initialNetPlan, ipOverWdmFailureDefaultMTTFInHours.getDouble(), ipOverWdmFailureDefaultMTTRInHours.getDouble(), SRGUtils.SharedRiskModel.PER_LINK, true , wdmLayer);
					break;
				case "perDirectionalLinkBundle":
					SRGUtils.configureSRGs(initialNetPlan, ipOverWdmFailureDefaultMTTFInHours.getDouble(), ipOverWdmFailureDefaultMTTRInHours.getDouble(), SRGUtils.SharedRiskModel.PER_DIRECTIONAL_LINK_BUNDLE, true , wdmLayer);
					break;
				case "perBidirectionalLinkBundle":
					SRGUtils.configureSRGs(initialNetPlan, ipOverWdmFailureDefaultMTTFInHours.getDouble(), ipOverWdmFailureDefaultMTTRInHours.getDouble(), SRGUtils.SharedRiskModel.PER_BIDIRECTIONAL_LINK_BUNDLE, true , wdmLayer);
					break;
				default:
					throw new Net2PlanException("Failure model not valid. Please, check algorithm parameters description");
			}
		}

		Map<String,String> generalEventGeneratorParam = new HashMap<String,String> ();
		generalEventGeneratorParam.put ("_fail_failureModel" , ipOverWdmFailureModel.getString ().equalsIgnoreCase("none")? "none" : "SRGfromNetPlan"); // I create the SRGs here
		generalEventGeneratorParam.put ("_tfFast_fluctuationType" , ipTFFastFluctuationType.getString ()); 
		generalEventGeneratorParam.put ("_trafficType" , "non-connection-based"); 
		generalEventGeneratorParam.put ("_tfSlow_fluctuationType" , ipTFSlowFluctuationType.getString ()); 
		generalEventGeneratorParam.put ("_tfFast_fluctuationType" , ipTFFastFluctuationType.getString ()); 
		generalEventGeneratorParam.put ("cac_arrivalsPattern" , "deterministic"); 
		generalEventGeneratorParam.put ("trafficLayerId" , "" + wdmLayer.getId ()); 
		generalEventGeneratorParam.put ("randomSeed" , "" + randomSeed.getLong()); 
		generalEventGeneratorParam.put ("cac_avHoldingTimeHours" , "" + 1.0); 
		generalEventGeneratorParam.put ("cac_defaultConnectionSizeTrafficUnits" , "" + 1.0); 
		generalEventGeneratorParam.put ("tfFast_timeBetweenDemandFluctuationsHours" , "" + ipTFFastTimeBetweenDemandFluctuationsHours.getDouble()); 
		generalEventGeneratorParam.put ("tfFast_fluctuationCoefficientOfVariation" , "" + ipTFFastFluctuationCoefficientOfVariation.getDouble()); 
		generalEventGeneratorParam.put ("tfFast_maximumFluctuationRelativeFactor" , "" + ipTFFastMaximumFluctuationRelativeFactor.getDouble()); 
		generalEventGeneratorParam.put ("tfSlow_startDate" , ipTFSlowStartDate.getString()); 
		generalEventGeneratorParam.put ("tfSlow_timeBetweenDemandFluctuationsHours" , "" + ipTFSlowTimeBetweenDemandFluctuationsHours.getDouble()); 
		generalEventGeneratorParam.put ("tfSlow_defaultTimezone" , "" + ipTFSlowDefaultTimezone.getInt()); 
		generalEventGeneratorParam.put ("fail_defaultMTTFInHours" , "" + ipOverWdmFailureDefaultMTTFInHours.getDouble()); 
		generalEventGeneratorParam.put ("fail_defaultMTTRInHours" , "" + ipOverWdmFailureDefaultMTTRInHours.getDouble()); 
		generalEventGeneratorParam.put ("fail_statisticalPattern" , ipOverWDmFailureStatisticalPattern.getString ()); 
		super.initialize(initialNetPlan , generalEventGeneratorParam , simulationParameters , net2planParameters);


	
	}

	@Override
	public void processEvent(NetPlan currentNetPlan, SimEvent event)
	{
//		this.generalEventGenerator.processEvent(currentNetPlan , event);
		super.processEvent(currentNetPlan , event);
	}

}
