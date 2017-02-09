package com.net2plan.examples.general.onlineSim;

import static com.net2plan.internal.sim.SimCore.SimState.NOT_STARTED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Map;

import com.jom.JOMException;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.internal.IExternal;
import com.net2plan.internal.sim.EndSimulationException;
import com.net2plan.internal.sim.IGUISimulationListener;
import com.net2plan.internal.sim.SimCore;
import com.net2plan.internal.sim.SimCore.SimState;
import com.net2plan.internal.sim.SimKernel;

public class OnlineTestUtils implements IGUISimulationListener
{
	private SimKernel simKernel;
	private Thread simThread;
	
	public void runSimulation (NetPlan np , IExternal eventGenerator , IExternal eventProcessor , 
			Map<String,String> simulationParameters , Map<String,String> net2planParameters , 
			Map<String,String> eventGeneratorParameters , Map<String,String> eventProcessorParameters , 
			double simTimeSeconds)
	{
		this.simKernel = new SimKernel();
        simKernel.setGUIListener(this);
        simKernel.reset();

        simKernel.setNetPlan(np);

        /* run simulation */
        assertEquals (simKernel.getSimCore().getSimulationState() , NOT_STARTED);
        simKernel.configureSimulation(simulationParameters, net2planParameters, eventGenerator, eventGeneratorParameters, eventProcessor, eventProcessorParameters);
        simKernel.initialize();
        simKernel.getSimCore().setSimulationState(SimCore.SimState.RUNNING);

        simThread = new Thread(simKernel.getSimCore());
        simThread.start();
        try { Thread.currentThread().sleep((long) (simTimeSeconds * 1000)); } catch (Exception e) { e.printStackTrace(); fail (); }
        
        simKernel.getSimCore().setSimulationState(SimState.STOPPED);
        
        assertEquals(simThread , null);
	}

	@Override
	public void refresh(boolean forceRefresh)
	{
	}

	@Override
	public void simulationStateChanged(SimState simulationState, Throwable reason)
	{
        if (simThread != null) 
        {
            try 
            {
                simThread.stop();
                simThread = null;
            } catch (Throwable e) { }
        }
        if (reason == null) return;
        if (reason instanceof EndSimulationException) 
        {
        } else if (reason instanceof Net2PlanException || reason instanceof JOMException) 
        {
        	reason.printStackTrace();
        	fail ();
        } else 
        {
        	reason.printStackTrace();
        	fail ();
        }

        
	}
	
}
