package com.net2plan.niw;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;

import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.niw.OpticalSimulationModule.LpSignalState;
import com.net2plan.utils.Quadruple;


public interface IOadmArchitecture 
{
	public final static Class defaultClass = OadmArchitecture_generic.class; 
	public final static List<IOadmArchitecture> availableRepresentatives = Arrays.asList(new OadmArchitecture_generic ()); 
	public abstract WNode getHostNode ();
	public default boolean isNeverCreatingWastedSpectrum () { return !isPotentiallyWastingSpectrum(); }
	public abstract String getShortName ();
	public abstract boolean isPotentiallyWastingSpectrum ();

	public abstract SortedSet<OsmOpticalSignalPropagationElement> getOutElements(OsmOpticalSignalPropagationElement inputElement , Optional<OsmOpticalSignalPropagationElement> outputElement);

//	public abstract SortedSet<OsmOpticalSignalPropagationElement> getOutElements(WFiber outputFiber , boolean usingDirectionlessAddModule);
//	public abstract SortedSet<OsmOpticalSignalPropagationElement> getOutElementsIfDropFromInputFiber(WFiber inputFiber , boolean usingDirectionlessDropModule);
//	public abstract SortedSet<OsmOpticalSignalPropagationElement> getOutElementsIfExpressFromInputToOutputFiber(WFiber inputFiber, WFiber outputFiber);
	public abstract SortedSet<WFiber> getOutFibersUnavoidablePropagationFromInputFiber(WFiber inputFiber);

	public abstract void initialize (WNode node);
	public abstract List<Quadruple<String,String,String,String>> getParametersInfo_name_default_shortDesc_longDesc ();
	public default SortedMap<String,String> getDefaultParameters () 
	{
		final SortedMap<String,String> res = new TreeMap<> ();
		getParametersInfo_name_default_shortDesc_longDesc().stream().forEach(e->res.put(e.getFirst(), e.getSecond())); 
		return res;
	}
	public default Optional<SortedMap<String,String>> getCurrentParameters ()
	{
		final List<List<String>> initString = getHostNode().getNe().getAttributeAsStringMatrix(WNode.ATTNAMECOMMONPREFIX + WNode.ATTNAME_OPTICALSWITCHTYPEINITSTRING, null);
		if (initString == null) 
			return Optional.empty();
		final SortedMap<String,String> initParam = new TreeMap<> ();
		initString.stream().forEach(e->initParam.put(e.get(0), e.get(1)));
		return Optional.of(initParam);
	}
	public default void updateCurrentParameters (SortedMap<String,String> param)
	{
		final List<List<String>> initString = new ArrayList<> ();
		param.entrySet().stream().forEach(e->initString.add(Arrays.asList(e.getKey() , e.getValue())));
		getHostNode().getNe().setAttributeAsStringMatrix(WNode.ATTNAMECOMMONPREFIX + WNode.ATTNAME_OPTICALSWITCHTYPEINITSTRING, initString);
	}
	
	public abstract LpSignalState getOutLpStateForAddedLp (LpSignalState stateAtTheOutputOfTransponder , Optional<Integer> inputAddModuleIndex , WFiber output , int numOpticalSlotsNeededIfEqualization);
	public abstract LpSignalState getOutLpStateForDroppedLp (LpSignalState stateAtTheInputOfOadmAfterPreamplif , WFiber inputFiber , Optional<Integer> inputDropModuleIndex);
	public abstract LpSignalState getOutLpStateForExpressLp (LpSignalState stateAtTheInputOfOadmAfterPreamplif , WFiber inputFiber , WFiber outputFiber , int numOpticalSlotsNeededIfEqualization);

	public default Optional<Double> getExpressAttenuation_dB (WFiber inFiber , WFiber outFiber)
	{
		if (outFiber.isOriginOadmConfiguredToEqualizeOutput()) return Optional.empty();
		final WNode node = this.getHostNode();
		if (!inFiber.getB().equals(node) || !outFiber.getA().equals (node)) throw new Net2PlanException ("Wrong fiber");
		final LpSignalState initialState = new LpSignalState(0, 0, 0, 0);
		final LpSignalState endState = getOutLpStateForExpressLp (initialState , inFiber , outFiber , 1);
		return Optional.of(endState.getPower_dbm());
	}
	public default double getDropAttenuation_dB  (WFiber inFiber , Optional<Integer> inputDropModuleIndex)
	{
		final WNode node = this.getHostNode();
		if (!inFiber.getB().equals(node)) throw new Net2PlanException ("Wrong fiber");
		final LpSignalState initialState = new LpSignalState(0, 0, 0, 0);
		final LpSignalState endState = getOutLpStateForDroppedLp (initialState , inFiber , inputDropModuleIndex);
		return endState.getPower_dbm();
	}
	public default double getAddAttenuation_dB  (WFiber outFiber , Optional<Integer> inputAddModuleIndex , int numOpticalSlotsNeededIfEqualization)
	{
		final WNode node = this.getHostNode();
		if (!outFiber.getA().equals(node)) throw new Net2PlanException ("Wrong fiber");
		final LpSignalState initialState = new LpSignalState(0, 0, 0, 0);
		final LpSignalState endState = getOutLpStateForAddedLp (initialState , inputAddModuleIndex , outFiber , numOpticalSlotsNeededIfEqualization);
		return endState.getPower_dbm();
	}
	
	public abstract boolean isColorless ();
	
	public default boolean isOadmGeneric () { return this instanceof OadmArchitecture_generic; }
	public default OadmArchitecture_generic getAsOadmGeneric () { return (OadmArchitecture_generic) this; } 
	
}
