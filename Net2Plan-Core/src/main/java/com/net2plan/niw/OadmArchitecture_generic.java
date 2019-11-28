package com.net2plan.niw;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.niw.OpticalSimulationModule.LpSignalState;
import com.net2plan.utils.Pair;
import com.net2plan.utils.Quadruple;

public class OadmArchitecture_generic implements IOadmArchitecture
{
	private WNode node;
	public OadmArchitecture_generic ()
	{
	}
	
	@Override
	public WNode getHostNode () { return this.node; }

	@Override
	public String getShortName() 
	{
		return "Generic OADM";
	}

	@Override
	public boolean isPotentiallyWastingSpectrum() 
	{
		return new Parameters(getCurrentParameters().orElse(getDefaultParameters())).isFilterless();
	}

	@Override
	public SortedSet<OsmOpticalSignalPropagationElement> getOutElements(OsmOpticalSignalPropagationElement inputElement , Optional<OsmOpticalSignalPropagationElement> outputElement)
	{
		final Parameters p = new Parameters(getCurrentParameters().orElse(getDefaultParameters()));
		if (inputElement.isDirfulAdd())
		{
			assert this.getHostNode().isOadmWithDirectedAddDropModulesInTheDegrees();
			if (outputElement.isPresent())
			{
				assert outputElement.get().isFiber();
				assert outputElement.get().getFiber().getA().equals(this.getHostNode());
				return new TreeSet<> (Arrays.asList(outputElement.get()));
			}
			else
				return new TreeSet<> ();
		}
		if (inputElement.isDirlessAdd())
		{
			if (outputElement.isPresent())
			{
				assert outputElement.get().isFiber();
				assert outputElement.get().getFiber().getA().equals(this.getHostNode());
			}
			if (!p.isFilterless())
				return outputElement.isPresent()? new TreeSet<> (Arrays.asList(outputElement.get())) : new TreeSet<> ();
			/* Filterless & directionless => all out fibers and dirless drop modules, but my opposite drop module  */
			final SortedSet<OsmOpticalSignalPropagationElement> res = new TreeSet<> ();
			for (WFiber e : getHostNode().getOutgoingFibers())
				res.add(OsmOpticalSignalPropagationElement.asFiber(e));
			for (int module = 0 ; module < this.getHostNode().getOadmNumAddDropDirectionlessModules() ; module ++)
				if (module != inputElement.getDirlessAddModule().getSecond())
					res.add(OsmOpticalSignalPropagationElement.asDropDirless(Pair.of(this.getHostNode(), module)));
			return res;
		}
		if (inputElement.isFiber())
		{
			assert inputElement.getFiber().getB().equals(this.getHostNode());
			if (outputElement.isPresent()) if (outputElement.get().isFiber()) assert outputElement.get().getFiber().getA().equals(this.getHostNode());

			if (!p.isFilterless())
				return outputElement.isPresent()? new TreeSet<> (Arrays.asList(outputElement.get())) : new TreeSet<> ();

			/* Filterless: in directionless or not is the same: the dirful drop (if the node has), all the output fibers but the input fiber opposite, and all the dirless drops */
			final SortedSet<OsmOpticalSignalPropagationElement> res = new TreeSet<> ();
			if (this.getHostNode().isOadmWithDirectedAddDropModulesInTheDegrees())
				res.add(OsmOpticalSignalPropagationElement.asDropDirful(inputElement.getFiber()));
			for (WFiber e : getHostNode().getOutgoingFibers())
				if (e.isBidirectional())
					if (!e.getBidirectionalPair().equals(inputElement.getFiber()))
						res.add(OsmOpticalSignalPropagationElement.asFiber(e));
			for (int module = 0 ; module < this.getHostNode().getOadmNumAddDropDirectionlessModules() ; module ++)
				res.add(OsmOpticalSignalPropagationElement.asDropDirless(Pair.of(this.getHostNode(), module)));
			return res;
		}

		if (inputElement.isDirfulDrop()) assert this.getHostNode().isOadmWithDirectedAddDropModulesInTheDegrees();

		if (inputElement.isDirfulDrop() || inputElement.isDirlessDrop()) // input is drop element => output empty
			return new TreeSet<> ();
		
		throw new Net2PlanException ("Unkown optical element");
	}

	@Override
	public SortedSet<WFiber> getOutFibersUnavoidablePropagationFromInputFiber(WFiber inputFiber) 
	{
		final Parameters p = new Parameters(getCurrentParameters().orElse(getDefaultParameters()));
		if (!p.isFilterless())
			return new TreeSet<> ();
		/* Filterless: in directionless or not is the same: all the output fibers but the input fiber opposite */
		final SortedSet<WFiber> affectedFibers = new TreeSet<> (getHostNode().getOutgoingFibers());
		if (inputFiber.isBidirectional())
			affectedFibers.remove(inputFiber.getBidirectionalPair());
		return affectedFibers;
	}

	@Override
	public void initialize(WNode node) 
	{
		this.node = node;
	}

	public enum PARAMNAMES {ArchitectureType , IsDirectionLess , AddDropModuleType , MuxDemuxLoss_dB , MuxDemuxPmd_ps , WssLoss_dB , WssPmd_ps}

	@Override
	public List<Quadruple<String, String, String, String>> getParametersInfo_name_default_shortDesc_longDesc() 
	{
		return Arrays.asList(
				Quadruple.of(PARAMNAMES.ArchitectureType.name (), "B&S", "Opt. Sw. Arch." , "Indication of the architecture type: R&S is route and select (WSS in the in degrees and out degrees), B&S is broadcast-and-select (coupler at the input degree, WSS at the output), filterless: means a coupler at the input and output degrees."),
				Quadruple.of(PARAMNAMES.IsDirectionLess.name (), "0", "Directionless?" , "Indication is the architecture is directionless.  If so, each add/drop module has a similar structure (R&S, B&S, filterless) as a regular degree, so add/drop lightpaths can chage its in/out direction without manual reconfiguration. If not directionless, one add/drop module exist per regular degree, hosting the transponders of the ligtpaths add/dropped in that degree. Then, changing the in/out degree of an add/dropped lightpath, requires phisically moving the transponder to other module"),
				Quadruple.of(PARAMNAMES.AddDropModuleType.name (), "Mux/Demux-based", "Add/Drop type", "The optical element used to build the add/drop modules. Two options: 1) Mux/Demux means using a passive (de)multiplexer. This is not a colorless option, since to change the lightpath wavelength, we should phisically change the port of the transponder in the mux/demux. 2) WSS: a WSS for add and other for drop, remotely reconfigurable. This is a colorless option, so retuning a transponder can be made without the need of phisically changing its port in the WSS"),
				Quadruple.of(PARAMNAMES.MuxDemuxLoss_dB.name (), "6.0", "Mux/Demux loss (dB)", "The add/drop modules where the transponders are attached can be realized with a multiplexer/demultiplexer. This is the added attenuation in dB"),
				Quadruple.of(PARAMNAMES.MuxDemuxPmd_ps.name (), "0.5", "Mux/Demux PMD (ps)", "The add/drop modules where the transponders are attached can be realized with a multiplexer/demultiplexer. This is the added PMD in ps"),
				Quadruple.of(PARAMNAMES.WssLoss_dB.name (), "7.0", "WSS loss (dB)", "The add/drop modules and/or degrees can be realized with WSSa. This is the added attenuation in dB"),
				Quadruple.of(PARAMNAMES.WssPmd_ps.name (), "0.5", "WSS PMD (ps)", "The add/drop modules and/or degrees can be realized with WSSa. This is the added PMD in ps")
				);
	}

	public void updateParameters (Parameters newParameters)
	{
		final SortedMap<String,String> param = new TreeMap<> ();
		param.put(PARAMNAMES.ArchitectureType.name(), newParameters.getArchitectureType());
		param.put(PARAMNAMES.IsDirectionLess.name(), newParameters.isDirectionless()? "1" : "0");
		param.put(PARAMNAMES.AddDropModuleType.name(), newParameters.getAddDropModuleType());
		param.put(PARAMNAMES.MuxDemuxLoss_dB.name(), newParameters.getMuxDemuxLoss_dB() + "");
		param.put(PARAMNAMES.MuxDemuxPmd_ps.name(), newParameters.getMuxDemuxPmd_ps() + "");
		param.put(PARAMNAMES.WssLoss_dB.name(), newParameters.getWssLoss_dB() + "");
		param.put(PARAMNAMES.WssPmd_ps.name(), newParameters.getWssPmd_ps() + "");
		this.updateCurrentParameters(param);
	}
	
	public Parameters getParameters ()
	{
		return new Parameters(getCurrentParameters().orElse(getDefaultParameters()));
	}
	

	public class Parameters
	{
		private String architectureType;
		private boolean isDirectionless;
		private String addDropModuleType;
		private double muxDemuxLoss_dB;
		private double muxDemuxPmd_ps;
		private double wssLoss_dB;
		private double wssPmd_ps;
		
		private Parameters (Map<String,String> params)
		{
			try
			{
//				final Map<String,String> param = getCurrentParameters().orElse(getDefaultParameters());
				this.architectureType = params.getOrDefault(PARAMNAMES.ArchitectureType.name() , "B&S");
				this.isDirectionless = Double.parseDouble(params.getOrDefault(PARAMNAMES.IsDirectionLess.name(), "0")) != 0;
				this.addDropModuleType = params.getOrDefault(PARAMNAMES.AddDropModuleType.name() , "Mux/Demux-based");
				this.muxDemuxLoss_dB = Double.parseDouble(params.getOrDefault(PARAMNAMES.MuxDemuxLoss_dB.name() , "6.0"));
				this.muxDemuxPmd_ps = Double.parseDouble(params.getOrDefault(PARAMNAMES.MuxDemuxPmd_ps.name() , "0.5"));
				this.wssLoss_dB = Double.parseDouble(params.getOrDefault(PARAMNAMES.WssLoss_dB.name() , "7.0"));
				this.wssPmd_ps = Double.parseDouble(params.getOrDefault(PARAMNAMES.WssPmd_ps.name() , "0.5"));
			} catch (Exception e)
			{
				e.printStackTrace();
				this.architectureType = "B&S";
				this.isDirectionless = false;
				this.addDropModuleType = "Mux/Demux-based";
				this.muxDemuxLoss_dB = 6.0;
				this.muxDemuxPmd_ps = 0.5;
				this.wssLoss_dB = 7.0;
				this.wssPmd_ps = 0.5;
			}
		}
		
		public Parameters setArchitectureTypeAsBroadcastAndSelect () { this.architectureType = "B&S"; return this; }
		public Parameters setArchitectureTypeAsRouteAndSelect () { this.architectureType = "R&S"; return this; }
		public Parameters setArchitectureTypeAsFilterless () { this.architectureType = "filterless"; return this; }
		public Parameters setIsDirectionless (boolean isDirectionless) { this.isDirectionless = isDirectionless; return this; }
		public Parameters setAddDropModuleTypeAsMuxBased () { this.addDropModuleType = "Mux/Demux-based"; return this; }
		public Parameters setAddDropModuleTypeAsWssBased () { this.addDropModuleType = "Wss-based"; return this; }
		public Parameters setMuxDemuxLoss_dB (double lossInDb) { this.muxDemuxLoss_dB = lossInDb; return this; }
		public Parameters setMuxDemuxPmd_ps (double pmd_ps) { this.muxDemuxPmd_ps = pmd_ps; return this; }
		public Parameters setWssLoss_dB (double lossInDb) { this.wssLoss_dB = lossInDb; return this; }
		public Parameters setWssPmd_ps (double pmd_ps) { this.wssPmd_ps = pmd_ps; return this; }

		public String getArchitectureType() {return architectureType; }
		public boolean isDirectionless() { return isDirectionless; }
		public String getAddDropModuleType() { return addDropModuleType; }
		public double getMuxDemuxLoss_dB() { return muxDemuxLoss_dB; }
		public double getMuxDemuxPmd_ps() { return muxDemuxPmd_ps; }
		public double getWssLoss_dB() { return wssLoss_dB; }
		public double getWssPmd_ps() { return wssPmd_ps; }

		public boolean isRouteAndSelect () { return getArchitectureType().equals("R&S"); }
		public boolean isBroadcastAndSelect () { return getArchitectureType().equals("B&S"); }
		public boolean isFilterless () { return getArchitectureType().equals("filterless"); }
		public boolean isAddDropTypeMuxBased () { return getAddDropModuleType().equals("Mux/Demux-based"); }
		public boolean isAddDropTypeWssBased () { return !isAddDropTypeMuxBased (); }
}

	
	@Override
	public LpSignalState getOutLpStateForAddedLp(LpSignalState stateAtTheOutputOfTransponder, Optional<Integer> inputAddModuleIndex,
			WFiber output , int numOpticalSlotsNeededIfEqualization) 
	{
		final Parameters p = new Parameters(getCurrentParameters().orElse(getDefaultParameters()));

		final double lossesAddPart_dB;
		final double pmdAddPart_ps2;
		if (p.isDirectionless()) 
		{
			// A/D is like another degree
			final double justAddPart_dB = p.isAddDropTypeMuxBased()? p.getMuxDemuxLoss_dB() : p.getWssLoss_dB();
			final double justAddPart_ps2 = Math.pow(p.isAddDropTypeMuxBased()? p.getMuxDemuxPmd_ps() : p.getWssPmd_ps() , 2);
			final double addDegreePart_dB = p.isRouteAndSelect()? p.getWssLoss_dB() : 10 * Math.log10(getHostNode().getOutgoingFibers().size());
			final double addDegreePart_ps2 = p.isRouteAndSelect()? p.getWssPmd_ps() : 0.0;
			lossesAddPart_dB = justAddPart_dB + addDegreePart_dB;
			pmdAddPart_ps2 = justAddPart_ps2 + addDegreePart_ps2;
		}
		else
		{
			// A/D directly connectec to output degree
			lossesAddPart_dB = p.isAddDropTypeMuxBased()? p.getMuxDemuxLoss_dB() : p.getWssLoss_dB();
			pmdAddPart_ps2 = Math.pow(p.isAddDropTypeMuxBased()? p.getMuxDemuxPmd_ps() : p.getWssPmd_ps() , 2);
		}
		final double lossOutDegreePart_dB;
		final double pmdOutDegreePart_ps2;
		if (p.isFilterless())
		{
			// out degree is coupler based
			final int numInputsOfOutDegreeCoupler = p.isDirectionless()? 
					getHostNode().getOadmNumAddDropDirectionlessModules() + getHostNode().getIncomingFibers().size() - 1: // all add/drop modules, and all in degrees but me 
					1 + getHostNode().getIncomingFibers().size() - 1; // add/drop module, and all in degrees but myself
			lossOutDegreePart_dB = 10 * Math.log(numInputsOfOutDegreeCoupler);
			pmdOutDegreePart_ps2 = 0.0;
		}
		else
		{
			// out degree is WSS based
			lossOutDegreePart_dB = p.getWssLoss_dB();
			pmdOutDegreePart_ps2 = Math.pow(p.getWssPmd_ps(), 2);
		}
		
		final double outputPowerWithoutEqualization_dBm = stateAtTheOutputOfTransponder.getPower_dbm() - 
				lossesAddPart_dB - 
				lossOutDegreePart_dB; 
		final double outputPowerIfEqualized_dBm = 10 * Math.log10(numOpticalSlotsNeededIfEqualization * output.getOriginOadmSpectrumEqualizationTargetBeforeBooster_mwPerGhz().get() * WNetConstants.OPTICALSLOTSIZE_GHZ); 
		if (output.isOriginOadmConfiguredToEqualizeOutput())
			if (outputPowerWithoutEqualization_dBm < outputPowerIfEqualized_dBm)
				System.out.println("Warning: the VOAs in the WSS would need to apply a negative attenuation to equalize");
		return new LpSignalState(
					output.isOriginOadmConfiguredToEqualizeOutput()? outputPowerIfEqualized_dBm : outputPowerWithoutEqualization_dBm, 
					stateAtTheOutputOfTransponder.getCd_psPerNm(), 
					stateAtTheOutputOfTransponder.getPmdSquared_ps2() + pmdAddPart_ps2 + pmdOutDegreePart_ps2, 
					stateAtTheOutputOfTransponder.getOsnrAt12_5GhzRefBw());
	}

	@Override
	public LpSignalState getOutLpStateForDroppedLp(LpSignalState stateAtTheInputOfOadmAfterPreamplif, WFiber inputFiber,
			Optional<Integer> inputDropModuleIndex) 
	{
		final Parameters p = new Parameters(getCurrentParameters().orElse(getDefaultParameters()));

		final double lossInDegreePart_dB;
		final double pmdInDegreePart_ps2;
		if (p.isRouteAndSelect())
		{
			// in degree is WSS based
			lossInDegreePart_dB = p.getWssLoss_dB();
			pmdInDegreePart_ps2 = Math.pow(p.getWssPmd_ps(), 2);
		}
		else
		{
			// in degree is coupler based
			final int numOutputOfInDegreeSplitter = p.isDirectionless()? 
					getHostNode().getOadmNumAddDropDirectionlessModules() + getHostNode().getOutgoingFibers().size() - 1: // all add/drop modules, and all out degrees but me 
					1 + getHostNode().getOutgoingFibers().size() - 1; // add/drop module, and all out degrees but myself
			lossInDegreePart_dB = 10 * Math.log(numOutputOfInDegreeSplitter);
			pmdInDegreePart_ps2 = 0.0;
		}
		
		final double lossesDropPart_dB;
		final double pmdDropPart_ps2;
		if (p.isDirectionless()) // A/D directly connectec to output degree
		{
			final double dropDegreePart_dB = p.isFilterless()? 10 * Math.log10(getHostNode().getIncomingFibers().size()) : p.getWssLoss_dB();
			final double dropDegreePart_ps2 = p.isFilterless()? 0.0 : p.getWssPmd_ps();
			final double justDropPart_dB = p.isAddDropTypeMuxBased()? p.getMuxDemuxLoss_dB() : p.getWssLoss_dB();
			final double justDropPart_ps2 = Math.pow(p.isAddDropTypeMuxBased()? p.getMuxDemuxPmd_ps() : p.getWssPmd_ps() , 2);
			lossesDropPart_dB = justDropPart_dB + dropDegreePart_dB;
			pmdDropPart_ps2 = justDropPart_ps2 + dropDegreePart_ps2;
		}
		else
		{
			lossesDropPart_dB = p.isAddDropTypeMuxBased()? p.getMuxDemuxLoss_dB() : p.getWssLoss_dB();
			pmdDropPart_ps2 = Math.pow(p.isAddDropTypeMuxBased()? p.getMuxDemuxPmd_ps() : p.getWssPmd_ps() , 2);
		}
		final double dropPowerWithoutEqualization_dBm = stateAtTheInputOfOadmAfterPreamplif.getPower_dbm() - 
				lossesDropPart_dB - 
				lossInDegreePart_dB; 
		return new LpSignalState(
				dropPowerWithoutEqualization_dBm, 
				stateAtTheInputOfOadmAfterPreamplif.getCd_psPerNm(), 
				stateAtTheInputOfOadmAfterPreamplif.getPmdSquared_ps2() + pmdDropPart_ps2 + pmdInDegreePart_ps2, 
				stateAtTheInputOfOadmAfterPreamplif.getOsnrAt12_5GhzRefBw());
	}

	@Override
	public LpSignalState getOutLpStateForExpressLp(LpSignalState stateAtTheInputOfOadmAfterPreamplif, WFiber inputFiber,
			WFiber outputFiber , int numOpticalSlotsNeededIfEqualization) 
	{
		final Parameters p = new Parameters(getCurrentParameters().orElse(getDefaultParameters()));

		final double lossInDegreePart_dB;
		final double pmdInDegreePart_ps2;
		if (p.isRouteAndSelect())
		{
			// in degree is WSS based
			lossInDegreePart_dB = p.getWssLoss_dB();
			pmdInDegreePart_ps2 = Math.pow(p.getWssPmd_ps(), 2);
		}
		else
		{
			// in degree is coupler based
			final int numOutputOfInDegreeSplitter = p.isDirectionless()? 
					getHostNode().getOadmNumAddDropDirectionlessModules() + getHostNode().getOutgoingFibers().size() - 1: // all add/drop modules, and all out degrees but me 
					1 + getHostNode().getOutgoingFibers().size() - 1; // add/drop module, and all out degrees but myself
			lossInDegreePart_dB = 10 * Math.log(numOutputOfInDegreeSplitter);
			pmdInDegreePart_ps2 = 0.0;
		}
		
		final double lossOutDegreePart_dB;
		final double pmdOutDegreePart_ps2;
		if (p.isFilterless())
		{
			// out degree is coupler based
			final int numInputsOfOutDegreeCoupler = p.isDirectionless()? 
					getHostNode().getOadmNumAddDropDirectionlessModules() + getHostNode().getIncomingFibers().size() - 1: // all add/drop modules, and all in degrees but me 
					1 + getHostNode().getIncomingFibers().size() - 1; // add/drop module, and all in degrees but myself
			lossOutDegreePart_dB = 10 * Math.log(numInputsOfOutDegreeCoupler);
			pmdOutDegreePart_ps2 = 0.0;
		}
		else
		{
			// out degree is WSS based
			lossOutDegreePart_dB = p.getWssLoss_dB();
			pmdOutDegreePart_ps2 = Math.pow(p.getWssPmd_ps(), 2);
		}
		final double outputPowerWithoutEqualization_dBm = stateAtTheInputOfOadmAfterPreamplif.getPower_dbm() - 
				lossInDegreePart_dB - 
				lossOutDegreePart_dB; 
		final double outputPowerIfEqualized_dBm = 10 * Math.log10(numOpticalSlotsNeededIfEqualization * outputFiber.getOriginOadmSpectrumEqualizationTargetBeforeBooster_mwPerGhz().get() * WNetConstants.OPTICALSLOTSIZE_GHZ); 
		if (outputFiber.isOriginOadmConfiguredToEqualizeOutput())
			if (outputPowerWithoutEqualization_dBm < outputPowerIfEqualized_dBm)
				System.out.println("Warning: the VOAs in the WSS would need to apply a negative attenuation to equalize");
		return new LpSignalState(
				outputFiber.isOriginOadmConfiguredToEqualizeOutput()? outputPowerIfEqualized_dBm : outputPowerWithoutEqualization_dBm, 
						stateAtTheInputOfOadmAfterPreamplif.getCd_psPerNm(), 
						stateAtTheInputOfOadmAfterPreamplif.getPmdSquared_ps2() + pmdInDegreePart_ps2 + pmdOutDegreePart_ps2, 
						stateAtTheInputOfOadmAfterPreamplif.getOsnrAt12_5GhzRefBw());
	}

	@Override
	public boolean isColorless() 
	{
		return new Parameters(getCurrentParameters().orElse(getDefaultParameters())).isAddDropTypeWssBased();
	}

}
