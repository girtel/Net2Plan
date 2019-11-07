package com.net2plan.niw;

import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.net2plan.niw.OpticalSimulationModule.LpSignalState;


public interface IOadmArchitecture 
{
	public final static Class defaultClass = OadmArchitecture_roadm.class; 
	
	public abstract WNode getHostNode ();
	public abstract boolean isRoadm ();
	public abstract String getShortName ();
	public abstract boolean isDropAndWaste ();
	public abstract SortedSet<WFiber> getOutFibersIfAddToOutputFiber(WFiber outputFiber);
	public abstract SortedSet<WFiber> getOutFibersIfDropFromInputFiber(WFiber inputFiber);
	public abstract SortedSet<WFiber> getOutFibersIfExpressFromInputToOutputFiber(WFiber inputFiber, WFiber outputFiber);
	public abstract SortedSet<WFiber> getOutFibersUnavoidablePropagationFromInputFiber(WFiber inputFiber);
	public abstract void initialize (WNode node , String initString);
	public abstract String getInitializationString ();

	public abstract LpSignalState getOutLpStateForAddedLp (LpSignalState stateAtTheOutputOfTransponder , int inputAddModuleIndex , WFiber output);
	public abstract LpSignalState getOutLpStateForDroppedLp (LpSignalState stateAtTheInputOfOadmAfterPreamplif , WFiber inputFiber , int inputDropModuleIndex);
	public abstract LpSignalState getOutLpStateForExpressLp (LpSignalState stateAtTheInputOfOadmAfterPreamplif , WFiber inputFiber , WFiber outputFiber);

	
	
	
//	public default Optional<Integer> getInputExpressPortIndex(WFiber e) 
//	{
//		int cont = 0;
//		for (WFiber f : getHostNode().getIncomingFibers())
//		{
//			if (e.equals(f)) return Optional.of(cont);
//			cont ++;
//		}
//		return Optional.empty();
//	}
//
//	public default Optional<Integer> getOutputExpressPortIndex(WFiber e) 
//	{
//		int cont = 0;
//		for (WFiber f : getHostNode().getIncomingFibers())
//		{
//			if (e.equals(f)) return Optional.of(cont);
//			cont ++;
//		}
//		return Optional.empty();
//	}
//
	
}
