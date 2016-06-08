/*******************************************************************************
 * Copyright (c) 2016 Pablo Pavon Mariño.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 ******************************************************************************/


package temporal.javier;


import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;


import com.net2plan.interfaces.networkDesign.Demand;
//import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
//import com.net2plan.interfaces.networkDesign.Node;
//import com.net2plan.interfaces.networkDesign.SharedRiskGroup;
import com.net2plan.interfaces.simulation.IEventGenerator;
import com.net2plan.interfaces.simulation.SimEvent;
//import com.net2plan.libraries.FlexGridUtilsJavi;
import com.net2plan.libraries.WDMUtils;
import com.net2plan.utils.DoubleUtils;
import com.net2plan.utils.InputParameter;
//import com.net2plan.utils.Pair;
import com.net2plan.utils.RandomUtils;
import com.net2plan.utils.StringUtils;
import com.net2plan.utils.Triple;

import cern.jet.random.tdouble.AbstractDoubleDistribution;
import cern.jet.random.tdouble.Exponential;
import cern.jet.random.tdouble.engine.MersenneTwister64;

/** 
 * This event generator does not produce any event. It is only needed with those event processors that work alone, generating and consuming their own events, 
 * and that cannot receive any event coming from the event generator module.
 * @net2plan.inputParameters 
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza, Francisco-Javier Moreno-Muro
 */
public class OnLine_EG_multilServiceExponentialConnectionGenerator extends IEventGenerator
{
	private InputParameter loadFactor = new InputParameter ("loadFactor", (double) 1 , "Network load factor (scale up/down the input traffic)");
	private InputParameter randomSeed = new InputParameter ("randomSeed", (long) -1 , "Seed for the random generator (-1 means random)");
	private InputParameter trafficLayerId = new InputParameter ("trafficLayerId", (long) -1 , "Layer containing traffic demands (-1 means default layer)");
	private InputParameter simulationModel = new InputParameter ("simulationModel", "#select# longRun incrementalMode" , "Simulation model: 'longRun' (connections are established and released), 'incrementalMode' (connections are never released)");
	private InputParameter bandwidthInGbpsPerService = new InputParameter ("bandwidthInGbpsPerService", "400 100 40 10" , "Binary rate (in Gbps) per service");
	private InputParameter connectionProportionPerService = new InputParameter ("connectionProportionPerService", "1 1 1 1", "In average, the number of requests of each service is proportional to this number");
	private InputParameter maxNumberOfConnectionRequests = new InputParameter ("maxNumberOfConnectionRequests", (long) 1000000, "Maximum number of connection requests");
	private InputParameter debugMode = new InputParameter ("debugMode", (Boolean) false, "True for activating Debug Mode");
	
	private boolean incrementalMode;
	private Random rng;
	private boolean dM;
	private double[] bandwidthInGbpsPerServiceArray;
	private double[] connectionProportionPerServiceArray;
	private Map<Demand, AbstractDoubleDistribution[]> interarrivalTimeGenerator_ds, holdingTimeGenerator_ds;
	private long numberOfConnectionsRequests = 0;

	
	@Override
	public String getDescription()
	{
		return "Multi-service connection generator";
	}
	
	@Override
	public List<Triple<String, String, String>> getParameters()
	{
		/* Returns the parameter information for all the InputParameter objects defined in this object (uses Java reflection) */
		return InputParameter.getInformationAllInputParameterFieldsOfObject(this);
	}
	

	@Override
	public void initialize(NetPlan initialNetPlan, Map<String, String> algorithmParameters, Map<String, String> simulationParameters, Map<String, String> net2planParameters)
	{
		/* Initialize all InputParameter objects defined in this object (this uses Java reflection) */
		InputParameter.initializeAllInputParameterFieldsOfObject(this, algorithmParameters);
		
		
		NetworkLayer trafficLayer = trafficLayerId.getLong () == -1? initialNetPlan.getNetworkLayerDefault () : initialNetPlan.getNetworkLayerFromId(trafficLayerId.getLong ());
		if (trafficLayer == null) throw new Net2PlanException ("Unknown layer id");
		
		this.dM = this.debugMode.getBoolean();
		
		final int D = initialNetPlan.getNumberOfDemands(trafficLayer);
		if (D == 0) throw new Net2PlanException("No demands were defined in the original design");
		
		if (randomSeed.getLong () == -1) randomSeed.initialize((long) RandomUtils.random(0, Long.MAX_VALUE - 1));
		this.rng = new Random(randomSeed.getLong ());	
		
		if (loadFactor.getDouble() <= 0) throw new Net2PlanException("Network load factor must be greater than zero");		
		
		if (!simulationModel.getString().equalsIgnoreCase("longRun") && !simulationModel.getString().equalsIgnoreCase("incrementalMode")) throw new Net2PlanException("Simulation model must be either 'longRun' or 'incrementalMode'");
		this.incrementalMode = simulationModel.getString().equalsIgnoreCase("incrementalMode");		
		
		this.bandwidthInGbpsPerServiceArray = StringUtils.toDoubleArray(StringUtils.split(bandwidthInGbpsPerService.getString(), ", "));
		final int numServices = bandwidthInGbpsPerServiceArray.length;
		if (numServices == 0) throw new Net2PlanException("Number of services must be greater than zero");
		
		/* The proportions input parameter are read. They are transformed so they sum 1 */
		final String[] aux_connectionProportionPerService = StringUtils.split(this.connectionProportionPerService.getString(), ", ");
		if (aux_connectionProportionPerService.length == bandwidthInGbpsPerServiceArray.length)
			connectionProportionPerServiceArray = StringUtils.toDoubleArray(aux_connectionProportionPerService);
		else
			connectionProportionPerServiceArray = DoubleUtils.ones(bandwidthInGbpsPerServiceArray.length);
		
		final double[] maxMinProportions = DoubleUtils.maxMinValues(this.connectionProportionPerServiceArray);
		if ((maxMinProportions[1] < 0) || (maxMinProportions[0] == 0)) throw new Net2PlanException("Wrong proportions");
		connectionProportionPerServiceArray = DoubleUtils.divide(this.connectionProportionPerServiceArray, DoubleUtils.sum(connectionProportionPerServiceArray));
		
		// Debug Mode only
		if (dM){
			System.out.println("Algorithm Parameters:");
			System.out.println("Debug Mode:" + this.dM);
			System.out.println("Bandwidth In Gps Per Service:" + this.bandwidthInGbpsPerServiceArray[1]);
			System.out.println("Simulation Mode:" + this.simulationModel.getString());
			System.out.println("Load Factor:" + this.loadFactor.getDouble());
			System.out.println("Random Seed:" + this.rng.toString());
			System.out.println("Connection Proportion Per Service:" + this.connectionProportionPerServiceArray[1]);
		}	
		
		interarrivalTimeGenerator_ds = new LinkedHashMap<Demand, AbstractDoubleDistribution[]>();
		if (!incrementalMode) holdingTimeGenerator_ds = new LinkedHashMap<Demand, AbstractDoubleDistribution[]>();		
		
		for (Demand originalDemand : initialNetPlan.getDemands(trafficLayer))
		{			
			final double h_d = originalDemand.getOfferedTraffic(); 
			if (h_d == 0) continue;
			
			interarrivalTimeGenerator_ds.put(originalDemand, new AbstractDoubleDistribution[numServices]);
			if (!incrementalMode) holdingTimeGenerator_ds.put(originalDemand, new AbstractDoubleDistribution[numServices]);			
			
			for(int serviceId = 0; serviceId < numServices; serviceId++)
			{
				final double iat_thisDemand_thisService = bandwidthInGbpsPerServiceArray[serviceId] / (this.loadFactor.getDouble() * h_d * connectionProportionPerServiceArray[serviceId]);
				interarrivalTimeGenerator_ds.get(originalDemand)[serviceId] = new Exponential(1 / iat_thisDemand_thisService, new MersenneTwister64(this.rng.nextInt()));
				if (!incrementalMode) holdingTimeGenerator_ds.get(originalDemand)[serviceId] = new Exponential(1, new MersenneTwister64(this.rng.nextInt()));
				final double nextArrivalTime = 0 + interarrivalTimeGenerator_ds.get(originalDemand)[serviceId].nextDouble();
				double nextDuration = incrementalMode ? Double.MAX_VALUE : holdingTimeGenerator_ds.get(originalDemand)[serviceId].nextDouble();
				
				scheduleEvent(new SimEvent(nextArrivalTime, SimEvent.DestinationModule.EVENT_PROCESSOR, -1, loadFactor.getDouble()));
				
				if(dM){
					System.out.println("------------ New Request---------------------");
					System.out.println("Demand Index: " + originalDemand.getIndex());
					System.out.println("Service Id: " + serviceId);
					System.out.println("Arrival Time: " + nextArrivalTime);
					System.out.println("Duration:" + nextDuration);
					System.out.println("Offered Traffic per Demand (Gbps):" + originalDemand.getOfferedTraffic());
					System.out.println("Bandwidth per Service (Gbps): " + bandwidthInGbpsPerServiceArray[serviceId]);
				}
				
				scheduleEvent(new SimEvent(nextArrivalTime, SimEvent.DestinationModule.EVENT_GENERATOR ,-1, new GenerateConnectionRequest(originalDemand,nextDuration,serviceId)));
				
			}
		}
		
	}

	@Override
	public void processEvent(NetPlan currentNetPlan, SimEvent event)
	{
		double simTime = event.getEventTime();
		Object eventObject = event.getEventObject();		
		
		if (eventObject instanceof GenerateConnectionRequest)
		{
			
			numberOfConnectionsRequests++;
			if (numberOfConnectionsRequests > maxNumberOfConnectionRequests.getLong() && maxNumberOfConnectionRequests.getLong() != -1){
				endSimulation();
			}
			
			GenerateConnectionRequest connectionRequest = (GenerateConnectionRequest) eventObject;
			final Demand originalDemand = connectionRequest.demand;
			final int serviceId = connectionRequest.serviceId;
			final double duration = connectionRequest.duration;
			
			WDMUtils.LightpathAdd routeInfo_add = new WDMUtils.LightpathAdd(originalDemand,bandwidthInGbpsPerServiceArray[serviceId]);
			scheduleEvent(new SimEvent (simTime, SimEvent.DestinationModule.EVENT_PROCESSOR , -1 , routeInfo_add));	
					
			if (!incrementalMode) scheduleEvent(new SimEvent(simTime + duration, SimEvent.DestinationModule.EVENT_GENERATOR , -1 , new GenerateConnectionRelease(routeInfo_add)));
			
			if(dM){
				System.out.println("---------------------------");
				System.out.println("Demand Id: " + originalDemand.getId());
				System.out.println("Egress Node (ID): " + originalDemand.getEgressNode().getId());
				System.out.println("Ingress Node (ID): " + originalDemand.getIngressNode().getId());
				System.out.println("Service Id: " + originalDemand.getAttribute("serviceId"));
				System.out.println("Service Bandwidth: " + bandwidthInGbpsPerServiceArray[serviceId]);
				System.out.println("Connction Duration: " + duration);
			}		
		
			if (!incrementalMode) scheduleEvent(new SimEvent(simTime + duration,SimEvent.DestinationModule.EVENT_GENERATOR,-1 ,routeInfo_add));

			final double nextArrivalTime = simTime + interarrivalTimeGenerator_ds.get(originalDemand)[serviceId].nextDouble();
			final double nextDuration = incrementalMode ? Double.MAX_VALUE : holdingTimeGenerator_ds.get(originalDemand)[serviceId].nextDouble();
			scheduleEvent(new SimEvent(nextArrivalTime, SimEvent.DestinationModule.EVENT_GENERATOR , -1 , new GenerateConnectionRequest(originalDemand,nextDuration,serviceId)));

		}
		else if (eventObject instanceof GenerateConnectionRelease)
		{
			if (incrementalMode) throw new Net2PlanException("In incremental model connections are never released");
			final GenerateConnectionRelease releaseEvent = (GenerateConnectionRelease) eventObject;
			
			if (releaseEvent.routeAddEvent.lpAddedToFillByProcessor != null)
			{
				WDMUtils.LightpathRemove routeInfo_remove = new WDMUtils.LightpathRemove(releaseEvent.routeAddEvent.lpAddedToFillByProcessor);
				scheduleEvent(new SimEvent (simTime , SimEvent.DestinationModule.EVENT_PROCESSOR , -1 , routeInfo_remove));
				if (dM) System.out.print("Route to Release index: " + releaseEvent.routeAddEvent.lpAddedToFillByProcessor.getIndex() );
			}
			
		}
		
	}

	private static class GenerateConnectionRequest
	{
		public final Demand demand;
		public final double duration;
		public final int serviceId;
		
		public GenerateConnectionRequest(Demand demand, double duration , int serviceId) {
			this.demand = demand; 
			this.duration = duration;
			this.serviceId = serviceId;
		}

	}
	private static class GenerateConnectionRelease
	{
		public final WDMUtils.LightpathAdd routeAddEvent;
		public GenerateConnectionRelease(WDMUtils.LightpathAdd routeAddEvent) { this.routeAddEvent = routeAddEvent; }
		@Override
		public String toString() { return "Generate connection release for route " + routeAddEvent.demand.getId (); }
	}
	
	

}

