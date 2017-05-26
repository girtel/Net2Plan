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
package com.net2plan.examples.general.onlineSim;

import com.jom.JOMException;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.internal.IExternal;
import com.net2plan.internal.sim.EndSimulationException;
import com.net2plan.internal.sim.IGUISimulationListener;
import com.net2plan.internal.sim.SimCore.SimState;
import com.net2plan.internal.sim.SimKernel;

import java.util.Map;

import static com.net2plan.internal.sim.SimCore.SimState.NOT_STARTED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class OnlineTestUtils implements IGUISimulationListener
{
    private SimKernel simKernel;
    private Thread simThread;
    private boolean simulationEndedWithInternalEndSimulation;
    private Thread mainThread;


    public void runSimulation(NetPlan np, IExternal eventGenerator, IExternal eventProcessor,
                              Map<String, String> simulationParameters, Map<String, String> net2planParameters,
                              Map<String, String> eventGeneratorParameters, Map<String, String> eventProcessorParameters,
                              double simTimeSeconds)
    {
        this.simKernel = new SimKernel();
        simKernel.setGUIListener(this);
        simKernel.reset();

        simKernel.setNetPlan(np);

        /* run simulation */
        assertEquals(simKernel.getSimCore().getSimulationState(), NOT_STARTED);
        simKernel.configureSimulation(simulationParameters, net2planParameters, eventGenerator, eventGeneratorParameters, eventProcessor, eventProcessorParameters);
        simKernel.initialize();
        simKernel.getSimCore().setSimulationState(SimState.RUNNING);
        this.simulationEndedWithInternalEndSimulation = false;

        simThread = new Thread(simKernel.getSimCore());
        simThread.start();

        this.mainThread = Thread.currentThread();

        try
        {
            try
            {
                mainThread.sleep((long) ((simTimeSeconds < 0) ? 5000 : simTimeSeconds * 1000));
                simKernel.getSimCore().setSimulationState(SimState.STOPPED);
                np.assignFrom(simKernel.getCurrentNetPlan());
                assertEquals(simThread, null);
                System.out.println("Simulation ended by tester");
            } catch (InterruptedException ee)
            {
                if (!simulationEndedWithInternalEndSimulation) fail();
                System.out.println("Simulation ended internally");
                np.assignFrom(simKernel.getCurrentNetPlan());
            }
        } catch (Exception e)
        {
            e.printStackTrace();
            fail();
        }

    }

    @Override
    public void refresh(boolean forceRefresh)
    {
    }

    @SuppressWarnings("deprecation")
    @Override
    public void simulationStateChanged(SimState simulationState, Throwable reason)
    {
        if (simThread != null)
        {
            try
            {
                simThread.stop();
                simThread = null;
            } catch (Throwable e)
            {
            }
        }
        if (reason == null) return;
        if (reason instanceof EndSimulationException)
        {
//        	reason.printStackTrace();
            /* Simulation ended internally (calling endSimulation) */
            this.simulationEndedWithInternalEndSimulation = true;
            mainThread.interrupt();

        } else if (reason instanceof Net2PlanException || reason instanceof JOMException)
        {
            reason.printStackTrace();
            fail();
        } else
        {
            reason.printStackTrace();
            fail();
        }


    }

}
