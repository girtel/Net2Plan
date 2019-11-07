package com.net2plan.niw;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import com.net2plan.niw.OpticalSimulationModule.LpSignalState;
import com.net2plan.utils.Quadruple;

public class OadmArchitecture_filterlessAndAddDropMux implements IOadmArchitecture
{
	private WNode node;
	public OadmArchitecture_filterlessAndAddDropMux ()
	{
	}
	
	@Override
	public WNode getHostNode () { return this.node; }

	@Override
	public String getShortName() 
	{
		return "Filterless D&W";
	}

	@Override
	public boolean isPotentiallyWastingSpectrum() 
	{
		return true;
	}

	@Override
	public SortedSet<WFiber> getOutFibersIfAddToOutputFiber(WFiber outputFiber) 
	{
		return new TreeSet<> (Arrays.asList(outputFiber));
	}

	@Override
	public SortedSet<WFiber> getOutFibersIfDropFromInputFiber(WFiber inputFiber) 
	{
		return new TreeSet<> ();
	}

	@Override
	public SortedSet<WFiber> getOutFibersIfExpressFromInputToOutputFiber(WFiber inputFiber, WFiber outputFiber) 
	{
		return new TreeSet<> (Arrays.asList(outputFiber));
	}

	@Override
	public SortedSet<WFiber> getOutFibersUnavoidablePropagationFromInputFiber(WFiber inputFiber) 
	{
		return new TreeSet<> ();
	}

	@Override
	public void initialize(WNode node) 
	{
		this.node = node;
	}

	private enum PARAMNAMES {AddDropModuleMuxLoss_dB , AddDropModuleMuxPmd_ps , PerWssInsertionLoss_dB , PerWssPmd_ps}

	@Override
	public List<Quadruple<String, String, String, String>> getParametersInfo_name_default_shortDesc_longDesc() 
	{
		return Arrays.asList(
				Quadruple.of(PARAMNAMES.AddDropModuleMuxLoss_dB.name (), "6.0", "Attenuation of add/drop module mux/demux (dB)", "The add/drop modules where the transponders are attached is realized with a multiplexer/demultiplexer. This is the added attenuation in dB"),
				Quadruple.of(PARAMNAMES.AddDropModuleMuxPmd_ps.name (), "0.5", "PMD added by the add/drop module mux/demux (ps)", "The add/drop modules where the transponders are attached is realized with a multiplexer/demultiplexer. This is the added PMD in ps"),
				Quadruple.of(PARAMNAMES.PerWssInsertionLoss_dB.name (), "7.0", "Attenuation of the WSSs in the out degrees, and for each drop module" , "Each out degree is implented with a WSS, and also drop modules are realized with it. This parameters indicates the insertion loss of those WSS, in dB"),
				Quadruple.of(PARAMNAMES.PerWssPmd_ps.name (), "0.5", "PMD added by the WSSs in the out degrees, and for each drop module" , "Each out degree is implented with a WSS, and also drop modules are realized with it. This parameters indicates the added PMD of those WSS, in ps")
				);
	}

	private Map<PARAMNAMES , Double> getParamToApply ()
	{
		try
		{
			final Map<String,String> param = getCurrentParameters().orElse(getDefaultParameters());
			final Map<PARAMNAMES,Double> res = new HashMap<> ();
			for (PARAMNAMES p : PARAMNAMES.values())
				res.put(p, Double.parseDouble(param.get(p.name())));
			return res;
		} catch (Exception e)
		{
			e.printStackTrace();
			final Map<String,String> param = getDefaultParameters();
			final Map<PARAMNAMES,Double> res = new HashMap<> ();
			for (PARAMNAMES p : PARAMNAMES.values())
				res.put(p, Double.parseDouble(param.get(p.name())));
			return res;
		}
	}
	
	@Override
	public LpSignalState getOutLpStateForAddedLp(LpSignalState stateAtTheOutputOfTransponder, int inputAddModuleIndex,
			WFiber output , int numOpticalSlotsNeededIfEqualization) 
	{
		final Map<PARAMNAMES,Double> params = getParamToApply();
		final int numOutputsAddSplitter = getHostNode().getOutgoingFibers().size();
		final double lossesAddSplitter_dB = 10 * Math.log10(numOutputsAddSplitter);
		final double outputPowerWithoutEqualization_dBm = stateAtTheOutputOfTransponder.getPower_dbm() - 
				params.get(PARAMNAMES.AddDropModuleMuxLoss_dB) - 
				lossesAddSplitter_dB - 
				params.get(PARAMNAMES.PerWssInsertionLoss_dB); 
		final double outputPowerIfEqualized_dBm = 10 * Math.log10(numOpticalSlotsNeededIfEqualization * output.getOriginOadmSpectrumEqualizationTargetBeforeBooster_mwPerGhz().get() * WNetConstants.OPTICALSLOTSIZE_GHZ); 
		if (output.isOriginOadmConfiguredToEqualizeOutput())
			if (outputPowerWithoutEqualization_dBm < outputPowerIfEqualized_dBm)
				System.out.println("Warning: the VOAs in the WSS would need to apply a negative attenuation to equalize");
		return new LpSignalState(
					output.isOriginOadmConfiguredToEqualizeOutput()? outputPowerIfEqualized_dBm : outputPowerWithoutEqualization_dBm, 
					stateAtTheOutputOfTransponder.getCd_psPerNm(), 
					stateAtTheOutputOfTransponder.getPmdSquared_ps2() + params.get(PARAMNAMES.AddDropModuleMuxPmd_ps) + params.get(PARAMNAMES.AddDropModuleMuxPmd_ps), 
					stateAtTheOutputOfTransponder.getOsnrAt12_5GhzRefBw());
	}

	@Override
	public LpSignalState getOutLpStateForDroppedLp(LpSignalState stateAtTheInputOfOadmAfterPreamplif, WFiber inputFiber,
			int inputDropModuleIndex) 
	{
		final Map<PARAMNAMES,Double> params = getParamToApply();
		final int numOutputsInDegreeSplitter = getHostNode().getOutgoingFibers().size() + getHostNode().getOadmNumDropModules();
		final double lossesInDegreeSplitter_dB = 10 * Math.log10(numOutputsInDegreeSplitter);
		final double outputPower_dBm = stateAtTheInputOfOadmAfterPreamplif.getPower_dbm() - 
				lossesInDegreeSplitter_dB - 
				params.get(PARAMNAMES.PerWssInsertionLoss_dB) - 
				params.get(PARAMNAMES.AddDropModuleMuxLoss_dB); 
		return new LpSignalState(
				outputPower_dBm , 
					stateAtTheInputOfOadmAfterPreamplif.getCd_psPerNm(), 
					stateAtTheInputOfOadmAfterPreamplif.getPmdSquared_ps2() + params.get(PARAMNAMES.AddDropModuleMuxPmd_ps) + params.get(PARAMNAMES.AddDropModuleMuxPmd_ps), 
					stateAtTheInputOfOadmAfterPreamplif.getOsnrAt12_5GhzRefBw());
	}

	@Override
	public LpSignalState getOutLpStateForExpressLp(LpSignalState stateAtTheInputOfOadmAfterPreamplif, WFiber inputFiber,
			WFiber outputFiber) 
	{
		final Map<PARAMNAMES,Double> params = getParamToApply();
		final int numOutputsInDegreeSplitter = getHostNode().getOutgoingFibers().size() + getHostNode().getOadmNumDropModules();
		final double lossesInDegreeSplitter_dB = 10 * Math.log10(numOutputsInDegreeSplitter);
		final double outputPower_dBm = stateAtTheInputOfOadmAfterPreamplif.getPower_dbm() - 
				lossesInDegreeSplitter_dB - 
				params.get(PARAMNAMES.PerWssInsertionLoss_dB); 
		return new LpSignalState(
				outputPower_dBm , 
					stateAtTheInputOfOadmAfterPreamplif.getCd_psPerNm(), 
					stateAtTheInputOfOadmAfterPreamplif.getPmdSquared_ps2() + params.get(PARAMNAMES.AddDropModuleMuxPmd_ps), 
					stateAtTheInputOfOadmAfterPreamplif.getOsnrAt12_5GhzRefBw());
	}
	
}
