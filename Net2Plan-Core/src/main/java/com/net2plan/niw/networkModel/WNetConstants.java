/*******************************************************************************
 * This program and the accompanying materials
 * are made available under the terms of the MIT License available at
 * https://opensource.org/licenses/MIT
 *******************************************************************************/

package com.net2plan.niw.networkModel;

import java.util.Arrays;
import java.util.List;

public class WNetConstants
{
	/**
	 * The central frequency of optical slot 0. The central frequency of slot i is given by 193.1 + 0.0125 i THz 
	 */
	public static final double CENTRALFREQUENCYOFOPTICALSLOTZERO_THZ = 193.1;
	/**
	 * The optical slot size in GHz 
	 */
	public static final double OPTICALSLOTSIZE_GHZ = 12.5;
	
	public static final String ipLayerName = "IP";
	public static final String wdmLayerName = "WDM";
	

	public static final String TAG_NETPLAN_ISNIWDESIGN = "iSNiwDesign";
	
	static String ATTNAME_LIGHTPATHUNREG_TRANSPONDERNAME = "ATTNAME_LIGHTPATHUNREG_TRANSPONDERNAME";
	static final String TAGNODE_INDICATIONVIRTUALORIGINNODE = "TAGNODE_INDICATIONVIRTUALORIGINNODE";
	static final String TAGNODE_INDICATIONVIRTUALDESTINATIONNODE = "TAGNODE_INDICATIONVIRTUALDESTINATIONNODE";
	static final String TAGDEMANDIP_INDICATIONISBUNDLE = "TAGDEMANDIP_INDICATIONISBUNDLE";
	static final String WNODE_NAMEOFANYCASTORIGINNODE = "ANYCASTORIGIN" + WNetConstants.LISTSEPARATORANDINVALIDNAMECHARACTER;
	static final String WNODE_NAMEOFANYCASTDESTINATION = "ANYCASTDESTINATION" + WNetConstants.LISTSEPARATORANDINVALIDNAMECHARACTER;
	static final String LISTSEPARATORANDINVALIDNAMECHARACTER = ",";
	
	static final boolean WNODE_DEFAULT_ISCONNECTEDTOCORE = false;
	static final String WNODE_DEFAULT_TYPE = "Node";
	static final double WNODE_DEFAULT_NUMCPU = 15;
	static final double WNODE_DEFAULT_RAM = 15;
	static final double WNODE_DEFAULT_HD = 15;
	static final double WFIBER_DEFAULT_ATTCOEFFICIENTDBPERKM = 0.25;
	static final double WFIBER_DEFAULT_PMDCOEFF_PSPERSQRKM = 0.5;
	static final double WFIBER_DEFAULT_CDCOEFF_PSPERNMKM = 15;
	static final List<Double> WFIBER_DEFAULT_AMPLIFIERGAIN_DB = Arrays.asList(15.0);
	static final List<Double> WFIBER_DEFAULT_AMPLIFIERPMD_PS = Arrays.asList(15.0);
	static final List<Double> WFIBER_DEFAULT_AMPLIFIERCDCOMPENSATION = Arrays.asList(0.0);
	static final List<Double> WFIBER_DEFAULT_AMPLIFIERNOISEFACTOR_DB = Arrays.asList(6.0);
	
	static final double WFIBER_DEFAULT_PROPAGATIONSPEEDKMPERSEC = 200000;
	public static final List<Double> WFIBER_DEFAULT_VALIDOPTICALSLOTRANGES = Arrays.asList(1.0,320.0);
	static final List<Double> WFIBER_DEFAULT_AMPLIFIERPOSITION = Arrays.asList(0.0);
	static final boolean WLPREQUEST_DEFAULT_ISTOBE11PROTECTED = false;
	static final boolean WSERVICECHAINREQUEST_DEFAULT_ISENDEDINCORENODE = false;
	
	static final double DEFAULT_SRG_MTTRHOURS = 12;
	static final double DEFAULT_SRG_MTTFHOURS = 24*365;
	
	
}
