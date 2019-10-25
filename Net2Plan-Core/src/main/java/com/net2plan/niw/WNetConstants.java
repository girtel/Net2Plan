/*******************************************************************************
 * This program and the accompanying materials
 * are made available under the terms of the MIT License available at
 * https://opensource.org/licenses/MIT
 *******************************************************************************/

package com.net2plan.niw;

import java.util.Arrays;
import java.util.List;

import com.net2plan.utils.Pair;

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
	
	public static final boolean WNODE_DEFAULT_ISCONNECTEDTOCORE = false;
	public static final String WNODE_DEFAULT_TYPE = "Node";
	public static final double WNODE_DEFAULT_NUMCPU = 15;
	public static final double WNODE_DEFAULT_RAM = 15;
	public static final double WNODE_DEFAULT_HD = 15;
	public static final double WFIBER_DEFAULT_ATTCOEFFICIENTDBPERKM = 0.25;
	public static final double WFIBER_DEFAULT_PMDCOEFF_PSPERSQRKM = 0.5;
	public static final double WFIBER_DEFAULT_CDCOEFF_PSPERNMKM = 15;
	public static final List<Double> WFIBER_DEFAULT_AMPLIFIERGAIN_DB = Arrays.asList(15.0);
	public static final List<Double> WFIBER_DEFAULT_AMPLIFIERPMD_PS = Arrays.asList(15.0);
	public static final List<Double> WFIBER_DEFAULT_AMPLIFIERCDCOMPENSATION = Arrays.asList(0.0);
	public static final List<Double> WFIBER_DEFAULT_AMPLIFIERNOISEFACTOR_DB = Arrays.asList(6.0);
	public static final List<Double> WFIBER_DEFAULT_AMPLIFIERMINGAIN_DB = Arrays.asList(15.0);
	public static final List<Double> WFIBER_DEFAULT_AMPLIFIERMAXGAIN_DB = Arrays.asList(30.0);
	public static final List<Double> WFIBER_DEFAULT_AMPLIFIERMININPUTPOWER_DBM = Arrays.asList(-29.0);
	public static final List<Double> WFIBER_DEFAULT_AMPLIFIERMAXINPUTPOWER_DBM = Arrays.asList(2.0);

	public static final double WLIGHTPATH_DEFAULT_TRANSPONDERADDINJECTIONPOWER_DBM = 0.0;
	public static final double WLIGHTPATH_DEFAULT_MINIMUMACCEPTABLERECEPTIONPOWER_DBM = -20.0;
	public static final double WLIGHTPATH_DEFAULT_MAXIMUMACCEPTABLERECEPTIONPOWER_DBM = 10.0;
	public static final double WLIGHTPATH_DEFAULT_MINIMUMACCEPTABLEOSNRAT12_5GHZREFBW_DB = 20.0;
	public static final double WLIGHTPATH_DEFAULT_MAXIMUMABSOLUTE_CD_PSPERNM = 30000.0;
	public static final double WLIGHTPATH_DEFAULT_MAXIMUMPMD_PS = 35.0;
	
	public static final double WNODE_DEFAULT_ADDGAIN_DB = 0.0;
	public static final double WNODE_DEFAULT_EXPRESSGAIN_DB = 20.0;
	public static final double WNODE_DEFAULT_DROPGAIN_DB = 0.0;
	public static final double WNODE_DEFAULT_ADDNF_DB = -50.0;
	public static final double WNODE_DEFAULT_EXPRESSNF_DB = 5.0;
	public static final double WNODE_DEFAULT_DROPNF_DB = -50.0;
	public static final double WNODE_DEFAULT_ADDPMD_PS = 0.0;
	public static final double WNODE_DEFAULT_EXPRESSPMD_PS = 0.0;
	public static final double WNODE_DEFAULT_DROPPMD_PS = 0.0;
	
	static final double WFIBER_DEFAULT_PROPAGATIONSPEEDKMPERSEC = 200000;
	public static final List<Double> WFIBER_DEFAULT_VALIDOPTICALSLOTRANGES_LISTDOUBLE = Arrays.asList(1.0,320.0);
	public static final List<Pair<Integer,Integer>> WFIBER_DEFAULT_VALIDOPTICALSLOTRANGES = Arrays.asList(Pair.of(1,320));
	static final List<Double> WFIBER_DEFAULT_AMPLIFIERPOSITION = Arrays.asList(0.0);
	static final boolean WLPREQUEST_DEFAULT_ISTOBE11PROTECTED = false;
	static final boolean WSERVICECHAINREQUEST_DEFAULT_ISENDEDINCORENODE = false;
	
	static final double DEFAULT_SRG_MTTRHOURS = 12;
	static final double DEFAULT_SRG_MTTFHOURS = 24*365;
	
	public static enum WTYPE 
	{
		WFiber,
		WLayerIp,
		WLayerWdm,
		WIpLink,
		WIpUnicastDemand,
		WLightpath,
		WLightpathRequest,
		WIpSourceRoutedConnection,
		WNet,
		WNode,
		WServiceChain,
		WServiceChainRequest,
		WSharedRiskGroup,
		WVnfInstance;
		
		public boolean isWFiber () { return this == WTYPE.WFiber; }
		public boolean isIpLayer () { return this == WTYPE.WLayerIp; }
		public boolean isWdmLayer () { return this == WTYPE.WLayerWdm; }
		public boolean isIpLink() { return this == WIpLink; }
		public boolean isIpUnicastDemand() { return this == WIpUnicastDemand; }
		public boolean isLightpath() { return this == WTYPE.WLightpath; }
		public boolean isLightpathRequest () { return this == WLightpathRequest; }
		public boolean isIpSourceRoutedConnection() { return this == WTYPE.WIpSourceRoutedConnection; }
		public boolean isWNet () { return this == WTYPE.WNet; }
		public boolean isNode() { return this == WTYPE.WNode; }
		public boolean isServiceChain () { return this == WTYPE.WServiceChain; }
		public boolean isServiceChainRequest () { return this == WTYPE.WServiceChainRequest; }
		public boolean isSrg () { return this == WTYPE.WSharedRiskGroup; }
		public boolean isVnfInstance () { return this == WTYPE.WVnfInstance; }
		
	}
	
	
}
