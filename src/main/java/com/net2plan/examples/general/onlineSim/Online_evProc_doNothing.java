/*******************************************************************************
 * Copyright (c) 2016 Pablo Pavon Mari�o.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 ******************************************************************************/


package com.net2plan.examples.general.onlineSim;

import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.simulation.IEventProcessor;
import com.net2plan.interfaces.simulation.SimEvent;
import com.net2plan.utils.Triple;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * This event processor does not react to any received event. In general, it is only for testing purposes.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @net2plan.keywords
 * @net2plan.inputParameters
 */
public class Online_evProc_doNothing extends IEventProcessor {
    @Override
    public String getDescription() {
        return "This event processor does not react to any received event. In general, it is only for testing purposes";
    }

    @Override
    public List<Triple<String, String, String>> getParameters() {
        return new LinkedList<Triple<String, String, String>>();
    }

    @Override
    public void initialize(NetPlan initialNetPlan, Map<String, String> algorithmParameters, Map<String, String> simulationParameters, Map<String, String> net2planParameters) {
    }

    @Override
    public void processEvent(NetPlan currentNetPlan, SimEvent event) {
    }
}
