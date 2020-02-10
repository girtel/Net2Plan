package com.net2plan.niw;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;

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
	public default OadmArchitecture_generic getAsGenericArchitecture () { return (OadmArchitecture_generic) this; }
	public default boolean isGeneriOadmeArchitecture () { return this instanceof OadmArchitecture_generic; }

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

	public abstract boolean isColorless ();
	
	
}
