package temporal.javier;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.net2plan.interfaces.networkDesign.Demand;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.interfaces.networkDesign.Route;
import com.net2plan.interfaces.simulation.IEventProcessor;
import com.net2plan.interfaces.simulation.SimEvent;
import com.net2plan.libraries.GraphUtils;
import com.net2plan.libraries.WDMUtils;
import com.net2plan.libraries.WDMUtils.ModulationFormat;
import com.net2plan.utils.CollectionUtils;
import com.net2plan.utils.Constants.SearchType;
import com.net2plan.utils.DoubleUtils;
import com.net2plan.utils.InputParameter;
import com.net2plan.utils.IntUtils;
import com.net2plan.utils.Pair;
import com.net2plan.utils.Quadruple;
import com.net2plan.utils.StringUtils;
import com.net2plan.utils.Triple;

import cern.colt.matrix.tdouble.DoubleFactory1D;
//import cern.colt.matrix.tdouble.DoubleFactory1D;
import cern.colt.matrix.tdouble.DoubleFactory2D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
//import com.net2plan.libraries.GraphUtils;
//import cern.colt.matrix.tdouble.DoubleFactory1D;

/**
 * <p>This class is intended to serve as a template for CAC algorithms solving the
 * online RSA problem.</p>
 * 
 * <p>Child classes must implement only the {@link #allocateConnection(com.net2plan.interfaces.networkDesign.NetPlan, com.net2plan.interfaces.simulation.SimAction) allocateConnection()} 
 * method, returning the allocation information for the connection request, or null 
 * if blocked.</p>
 * 
 * <p><b>Important</b>: Implementations have full access to the current slot occupancy, 
 * but it is totally discouraged to edit this status. Updates on ocuppancy state, and 
 * compilation of statistics, are made by this class automatically.</p>
 * 
 * @author Jose-Luis Izquierdo-Zaragoza, Pablo Pavon-Marino, Maria-Victoria Bueno-Delgado
 * @version 1.1, May 2015
 */
public class OnLine_EP_flexGrid_Javi extends IEventProcessor
{
	private List<Pair<Double, Quadruple<Double, long[], long[], Double>>> log;
	private long previousPeriodIndex;
	private int numServices;
	private double loadFactor;
	
	// Separar las estadisticas acumuladas (globales) de las por periodo. 
	// Para pasar de acumulada a media temporal, dividir por el tiempo (si a�n no se llego al fin del transitorio, se divide por el tiempo actual, si no, se divide por el tiempo que paso desde el transitorio)
	// imprimirlas por separado de las por periodo. 
	// Las por periodo no se ven afectadas por el transitorio
	// chequeo: que al final, la media temporal ofrecida tiene que ser igual a la que segun el trafico y rho debe ser
	// EXTRA DE SEGURIDAD (MODO DEBUG): Se guarda el acumulado de tr�fico ofrecido y cursado, por servicio Y demanda. De esta manera, se comprubea al final que el ofrecido de cada demanda (con un chequeo autom�tico, debe ser parecido al ofrecido te�rico de la demanda)
	
	private double stat_trafficOfferedConnections , stat_trafficCarriedConnections;
	private long stat_numOfferedConnections , stat_numCarriedConnections;
	
	private long[] stat_offeredConnectionsPerService, stat_carriedConnectionsPerService;
	private double stat_transitoryInitTime;
	private double stat_accumulatedCarriedTrafficInGbps , stat_accumulatedOfferedTrafficInGbps;
//	private double stat_offeredTrafficInGbpsThisSamplePeriod , stat_carriedTrafficInGbpsThisSamplePeriod;

	private Boolean dM;

	private InputParameter bandwidthInGbpsPerService = new InputParameter ("bandwidthInGbpsPerService", "400 100 40 10", "Set of bandwidth services");
	private InputParameter distanceAdaptive = new InputParameter ("distanceAdaptive", (Boolean) true, "Indicates whether distance-adaptive modulation formats are used");
	private InputParameter incrementalMode = new InputParameter ("incrementalMode", (Boolean) false,"Indicates whether simulation should end after the first blocking event");
	private InputParameter samplingTimeInSeconds = new InputParameter ("samplingTimeInSeconds", (double) 1, "Interval to gather partial results");
	private InputParameter slotGranularityInGHz = new InputParameter ("slotGranularityInGHz", (double) 12.5, "Slot granularity (in GHz)");
	private InputParameter guardBandInGHz = new InputParameter ("guardBandInGHz", (double) 0, "Guard-band size (in GHz)");
	private InputParameter kParameter = new InputParameter ("K", (int) 5, "Number of candidate paths per demand");
	private InputParameter shortestPathType = new InputParameter ("shortestPathType", "#select# hops km", "Set of k-shortest path is computed according to this type. Can be 'km' or 'hops'");
	private InputParameter rsaAlgorithmType = new InputParameter ("rsaAlgorithmType", "#select# firstFit fFFragmentationAware fFPartialSharing fFPathPriority", "Set of available RSA algorithms" );
	private InputParameter debugMode = new InputParameter ("debugMode", (Boolean) false, "True for activating Debug Mode");
	private InputParameter maxLightpathLengthInKm = new InputParameter ("MaxLightpathLengthInKm", (double) -1 , "Lightpaths longer than this are considered not admissible. A non-positive number means this limit does not exist");
	private InputParameter maxLightpathNumHops = new InputParameter ("MaxLightpathNumHops", (int) -1 , "A lightpath cannot have more than this number of hops. A non-positive number means this limit does not exist");
	private InputParameter storeResultsInAFile = new InputParameter ("storeResultsInAFile", (Boolean) false, "Indicates whether results of the simulation are written in a file");

	
	private Map<Pair<Node,Node>,List<List<Link>>> cpl;
	private int N;
	private int E;
	private DoubleMatrix2D fiberSlotOccupancyMap_fs; // 1 mens occupied, 0 means free
	private int totalAvailableSlotsPerFiber;
	private double[] bandwidthInGbpsPerServiceArray;
	private Map<List<Link>, ModulationFormat> modulationFormatPerPath;
	private Map<ModulationFormat, int[]> numSlotsPerModulationPerService;	
	private Map<List<Link>, Set<Link>> neighborLinks_p;

	public void finishTransitory(double simTime)
	{
		this.stat_transitoryInitTime = simTime;
		this.stat_accumulatedCarriedTrafficInGbps = 0;
		this.stat_accumulatedOfferedTrafficInGbps = 0;
	}
	
	@Override
	public String finish(StringBuilder output, double simTime)
	{
		final double trafficBlockingOnConnectionSetup = stat_trafficOfferedConnections == 0? 0 : 1 - (stat_trafficCarriedConnections / stat_trafficOfferedConnections );
		final double connectionBlockingOnConnectionSetup = stat_numOfferedConnections == 0? 0.0 : 1 - (((double) stat_numCarriedConnections) / ((double) stat_numOfferedConnections));
		final double dataTime = simTime - stat_transitoryInitTime;
		
		if (dataTime <= 0) { output.append ("<p>No time for data acquisition</p>"); return ""; }
		final int lf = (int) this.loadFactor;
		final String fileName;
		
		output.append("<ul>");
		output.append("<li>Current simulation time (s): ").append(simTime).append("</li>");
		
		output.append (String.format("<p>Total traffic of offered connections: number connections %d, Average of offered traffic per second (Gpbs): %f</p>", stat_numOfferedConnections, stat_trafficOfferedConnections / dataTime));
		output.append (String.format("<p>Total traffic of carried connections: number connections %d, Average of carried traffic per second (Gbps): %f</p>", stat_numCarriedConnections, stat_trafficCarriedConnections / dataTime));
		output.append (String.format("<p>Total traffic offered traffic in Gbps %.1f</p>", stat_accumulatedOfferedTrafficInGbps));
		output.append (String.format("<p>Total traffic carried traffic in Gbps: %.1f</p>", stat_accumulatedCarriedTrafficInGbps));
		output.append (String.format("<p>Blocking at connection setup: Probability of blocking a connection: %f, Fraction of blocked traffic: %f</p>", connectionBlockingOnConnectionSetup , trafficBlockingOnConnectionSetup));
		
		// This part is for writing the results into a file
		if(this.storeResultsInAFile.getBoolean()){
			if(this.incrementalMode.getBoolean()) fileName = "flexGridPaper/data/incrementalMode/"+rsaAlgorithmType.getString()+"/Results_"+rsaAlgorithmType.getString()+".txt";
			else fileName = "flexGridPaper/data/longRun/"+rsaAlgorithmType.getString()+"/Results_"+rsaAlgorithmType.getString()+"_"+Integer.toString(lf)+".txt";
					
			File file = new File(fileName);
			if (!file.exists()){
				try {
					file.createNewFile();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			try {
				FileWriter fw = new FileWriter(file,true);
				if (this.incrementalMode.getBoolean()) fw.write(Double.toString(stat_accumulatedCarriedTrafficInGbps)+"\r\n");
				else fw.write(Double.toString(connectionBlockingOnConnectionSetup)+"\r\n");
				fw.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		return "Blocking evolution";
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
		
		N = initialNetPlan.getNumberOfNodes();
		E = initialNetPlan.getNumberOfLinks();
	
		int D = initialNetPlan.getNumberOfDemands();
		if (N == 0 || E == 0 || D == 0) throw new Net2PlanException("A physical topology (nodes and links) and a set of demands are required");

		dM = this.debugMode.getBoolean();		
		
//		double[] u_e = initialNetPlan.getLinkCapacityVector();
		// Capacity in number of slots int ----> FlexigridUtils
		DoubleMatrix1D u_e = initialNetPlan.getVectorLinkCapacity();
		if (DoubleUtils.unique(u_e.toArray()).length > 1) throw new Net2PlanException("All fibers must have the same installed capacity");
		this.totalAvailableSlotsPerFiber = (int) u_e.get(0);
		
		final double slotGranularityInGHz = this.slotGranularityInGHz.getDouble();
		final double fiberCapacityInGHz = (int) Math.floor(totalAvailableSlotsPerFiber * slotGranularityInGHz);
		if (slotGranularityInGHz <= 0) throw new Net2PlanException("Slot granularity must be greater than zero");
		if (slotGranularityInGHz > fiberCapacityInGHz) throw new Net2PlanException("Slot granularity must be lower or equal than the fiber capacity");

		if (guardBandInGHz.getDouble() < 0) throw new Net2PlanException("Guard-band size must be greater or equal than zero");
		if (guardBandInGHz.getDouble() > fiberCapacityInGHz) throw new Net2PlanException("Guard-band size must be lower or equal than the fiber capacity");

		String[] aux_bandwidthInGbpsPerService = StringUtils.split(this.bandwidthInGbpsPerService.getString(), ", ");
		bandwidthInGbpsPerServiceArray = StringUtils.toDoubleArray(aux_bandwidthInGbpsPerService);
		numServices = bandwidthInGbpsPerServiceArray.length;
		if (numServices == 0) throw new Net2PlanException("Number of services must be greater than zero");

		fiberSlotOccupancyMap_fs = DoubleFactory2D.dense.make (E , totalAvailableSlotsPerFiber);
	
		
		if (samplingTimeInSeconds.getDouble() <= 0) throw new Net2PlanException("'samplingTimeInSeconds' must be greater than zero");

		previousPeriodIndex = 0;
		stat_offeredConnectionsPerService = new long[numServices];
		stat_carriedConnectionsPerService = new long[numServices];
		log = Collections.synchronizedList(new LinkedList<Pair<Double, Quadruple<Double, long[], long[], Double>>>());

		if (kParameter.getInt() < 1) throw new Net2PlanException("'K' must be greater or equal than one");

		if (!shortestPathType.getString().equalsIgnoreCase("km") && !shortestPathType.getString().equalsIgnoreCase("hops"))
			throw new Net2PlanException("Wrong shortestPathType parameter");
		boolean isShortestPathNumHops = shortestPathType.getString().equalsIgnoreCase("hops");

		/* Compute the candidate path list */
		Map<Link,Double> linkCostMap = isShortestPathNumHops? null : new HashMap<Link,Double> ();
		if (!isShortestPathNumHops) for (Link e : initialNetPlan.getLinks ()) linkCostMap.put (e , e.getLengthInKm());
		this.cpl = new HashMap<Pair<Node,Node>,List<List<Link>>> ();
		for (Node n1 : initialNetPlan.getNodes ())
			for (Node n2 : initialNetPlan.getNodes ())
				if (n1 != n2)
				{
					cpl.put (Pair.of (n1,n2) , GraphUtils.getKLooplessShortestPaths(initialNetPlan.getNodes () , initialNetPlan.getLinks () , n1 , n2 , linkCostMap , kParameter.getInt() , maxLightpathLengthInKm.getDouble() ,maxLightpathNumHops.getInt (),-1,-1,-1,-1));
					List<List<Link>> paths = cpl.get(Pair.of(n1, n2));
					if (paths.isEmpty()) throw new Net2PlanException("The number of paths between the nodes must be greater than zero");
				}
		
		if (rsaAlgorithmType.getString ().equalsIgnoreCase("fFFragmentationAware"))
		{
			neighborLinks_p = new LinkedHashMap<List<Link>, Set<Link>>();
			for (Collection<List<Link>> paths : cpl.values())
				for (List<Link> seqFibers : paths)
					neighborLinks_p.put(seqFibers, computeNeighborLinks(initialNetPlan, seqFibers));
		}
		
		DoubleMatrix1D linkLengthInKmMap = DoubleFactory1D.dense.make(E);
		for (Link e : initialNetPlan.getLinks()) linkLengthInKmMap.set(e.getIndex(), e.getLengthInKm());			
		
		int[] numSlotsPerService = new int[numServices];
		
		if (distanceAdaptive.getBoolean ())
		{
			Set<ModulationFormat> candidateModulationFormats = ModulationFormat.DEFAULT_MODULATION_SET;
			modulationFormatPerPath = WDMUtils.computeModulationFormatPerPath(cpl, candidateModulationFormats);

			numSlotsPerModulationPerService = new LinkedHashMap<ModulationFormat, int[]>();
			for (ModulationFormat modulationFormat : candidateModulationFormats)
			{
				for (int serviceId = 0; serviceId < numServices; serviceId++)
					numSlotsPerService[serviceId] = WDMUtils.computeNumberOfSlots(bandwidthInGbpsPerServiceArray[serviceId], slotGranularityInGHz, guardBandInGHz.getDouble (), modulationFormat);

				numSlotsPerModulationPerService.put(modulationFormat, numSlotsPerService);
			}
		}
		else
		{		
			modulationFormatPerPath = new LinkedHashMap<List<Link>, ModulationFormat>();
			ModulationFormat singleModulation = new ModulationFormat("SingleModulation", Double.MAX_VALUE, Double.MAX_VALUE);
			for (Collection<List<Link>> paths : cpl.values ())
				for (List<Link> path : paths)
					modulationFormatPerPath.put(path, singleModulation);
			numSlotsPerModulationPerService = new LinkedHashMap<ModulationFormat, int[]>();
			
			numSlotsPerService[0] = 32;
			numSlotsPerService[1] = 8;
			numSlotsPerService[2] = 4;
			numSlotsPerService[3] = 1;

			numSlotsPerModulationPerService.put(singleModulation, numSlotsPerService);
		}

		stat_accumulatedCarriedTrafficInGbps = 0;
		
		
		this.finishTransitory(0);
	}


	@Override
	public void processEvent(NetPlan currentNetPlan, SimEvent event)
	{
		double simTime = event.getEventTime();
		checkSamplingInterval(simTime);		
		if(dM) System.out.println(event.getEventObject().toString());
		
		if (event.getEventObject() instanceof WDMUtils.LightpathAdd)
		{
			
			if(dM) System.out.println("processEvent-LightpathAdd");
			
			WDMUtils.LightpathAdd addLpEvent = (WDMUtils.LightpathAdd) event.getEventObject ();

			final Demand demand = addLpEvent.demand;
			final double lineRateGbps = addLpEvent.lineRateGbps;

			final int serviceId = DoubleUtils.find (bandwidthInGbpsPerServiceArray , lineRateGbps , SearchType.FIRST) [0];
			
			this.stat_numOfferedConnections ++;
			this.stat_trafficOfferedConnections += lineRateGbps;

			if(dM) System.out.println("processEvent-serviceId :" + serviceId);
			
			this.stat_offeredConnectionsPerService[serviceId]++;
			this.stat_accumulatedOfferedTrafficInGbps += lineRateGbps;	
			
			Quadruple<List<Link>, ModulationFormat, Integer, Integer> allocation = findPotentialAllocationForConnection(currentNetPlan, demand , lineRateGbps , serviceId);
			if (allocation == null)
			{
				if(dM) System.out.println("Allocation is null");
				if (incrementalMode.getBoolean())
					endSimulation();
			}
			else
			{
				stat_carriedConnectionsPerService[serviceId]++;
				
				if(dM) System.out.println("processEvent-allcation ! null :" );
				
				Map<String, String> attributes = new HashMap<String, String>();
				
				stat_accumulatedCarriedTrafficInGbps += lineRateGbps;						

				List<Link> seqFibers = allocation.getFirst();
				ModulationFormat modulationFormat = allocation.getSecond();
				int initialSlotId = allocation.getThird();
				int numSlots = allocation.getFourth();
				
				int lastSlotId = initialSlotId + numSlots - 1;
				for (Link fiber : seqFibers)
				{
					for (int slotId = initialSlotId; slotId <= lastSlotId; slotId++)
					{
						if (fiberSlotOccupancyMap_fs.get (fiber.getIndex () , slotId) == 1) throw new RuntimeException(String.format("Bad - Slot %d is already occupied in fiber %d", slotId, fiber.getId()));
						fiberSlotOccupancyMap_fs.set (fiber.getIndex () , slotId , 1);
					}
				}
				
				if (dM){				
					System.out.println("Bandwidth In Gbps :" + lineRateGbps);
					System.out.println("Initial Slot Id: " + initialSlotId);
					System.out.println("Num Slots:" + numSlots);
					System.out.println("Modulation Format :" + modulationFormat.name);
					System.out.println("seqFibers: " + seqFibers.toString());
					System.out.println("numSlots: " + numSlots);
				}
							
				attributes.put("initialSlotId", Integer.toString(initialSlotId));
				attributes.put("numSlots", Integer.toString(numSlots));
				attributes.put("modulationFormat", modulationFormat.name);
				
				List<Link> finalseqFibers = new ArrayList<Link>();
				for(Link fiber : seqFibers)	finalseqFibers.add(currentNetPlan.getLinkFromId(fiber.getId()));
				
				this.stat_numCarriedConnections ++;
				this.stat_trafficCarriedConnections += lineRateGbps;				
				
				Route newRoute= currentNetPlan.addRoute(demand, lineRateGbps, numSlots, finalseqFibers, attributes);
				addLpEvent.lpAddedToFillByProcessor = newRoute;
				
				
			}

		}					
		else if (event.getEventObject () instanceof WDMUtils.LightpathRemove)
		{
			WDMUtils.LightpathRemove removeLpEvent = (WDMUtils.LightpathRemove) event.getEventObject ();
			
			if (dM) System.out.print("Release lp. Num lps before (occupied slots)  " + currentNetPlan.getNumberOfRoutes() + "/" + fiberSlotOccupancyMap_fs.zSum());
			
			Route removedLp = removeLpEvent.lp;
			
			List<Link> seqFibers = removedLp.getSeqLinksRealPath();
			Map<String, String> attributes = removedLp.getAttributes();
			int initialSlotId = Integer.parseInt(attributes.get("initialSlotId"));
			int numSlots = Integer.parseInt(attributes.get("numSlots"));
			int lastSlotId = initialSlotId + numSlots - 1;
			
			for (Link fiber : seqFibers)
			{
				for (int slotId = initialSlotId; slotId <= lastSlotId; slotId++)
				{
					if (fiberSlotOccupancyMap_fs.get(fiber.getIndex () , slotId) == 0) throw new RuntimeException("Bad");
					fiberSlotOccupancyMap_fs.set(fiber.getIndex () , slotId , 0);
				}
			}
			removedLp.remove();
			
			
			if (dM) System.out.println(" --> at the end: " + currentNetPlan.getNumberOfRoutes() + "/" + fiberSlotOccupancyMap_fs.zSum());

		}
		else if(event.getEventObject() instanceof Double){
			this.loadFactor = (Double) event.getEventObject ();			
		}
		else{
			if(dM) System.out.println("BAD: SHOULD NOT BE HERE!!!");
		}
	
	}

	private void checkSamplingInterval(double simTime)
	{
		if (simTime == 0) return;

		long currentPeriodIndex = (long) Math.floor(simTime / samplingTimeInSeconds.getDouble());
		if (previousPeriodIndex > currentPeriodIndex) throw new RuntimeException("Bad");

		if (previousPeriodIndex < currentPeriodIndex)
		{
			for (long periodIndex = previousPeriodIndex + 1; periodIndex < currentPeriodIndex; periodIndex++)
				log.add(Pair.of(samplingTimeInSeconds.getDouble() * (periodIndex + 1), Quadruple.of(this.stat_accumulatedCarriedTrafficInGbps, new long[numServices], new long[numServices], this.stat_accumulatedOfferedTrafficInGbps)));

			Quadruple<Double, long[], long[], Double> data = Quadruple.of(this.stat_accumulatedCarriedTrafficInGbps, stat_offeredConnectionsPerService, stat_carriedConnectionsPerService, this.stat_accumulatedOfferedTrafficInGbps);
			log.add(Pair.of(samplingTimeInSeconds.getDouble() * (previousPeriodIndex + 1), data));

			previousPeriodIndex = currentPeriodIndex;

			stat_offeredConnectionsPerService = new long[numServices];
			stat_carriedConnectionsPerService = new long[numServices];
		}
	}

	
	/**
	 * Execute the allocation algorithm.
	 * 
	 * @param currentNetPlan Current network state
	 * @param action {@code SimAction}
	 * @return A quadruple consisting of sequence of fibers, modulation format, initial slot id, and number of slots, or null if no allocation was found
	 * @since 1.1
	 */
	private Quadruple<List<Link>, ModulationFormat, Integer, Integer> findPotentialAllocationForConnection(NetPlan currentNetPlan, Demand thisDemand , double lineRateGbps , int serviceId)
	{
		List<List<Link>> paths = cpl.get (Pair.of (thisDemand.getIngressNode() , thisDemand.getEgressNode()));
		if (paths.isEmpty()) throw new Net2PlanException("No path between end nodes");
	
		if (this.rsaAlgorithmType.getString ().equalsIgnoreCase("firstFit"))
		{		
			if(dM)
			{
				System.out.println("-------------------");
				System.out.println("Demand Id: " + thisDemand.getId());
				System.out.println("Ingress Node id: " + thisDemand.getIngressNode().getId ());
				System.out.println("Egress Node id: " + thisDemand.getEgressNode().getId ());
				System.out.println("Service Id: " + serviceId);
			}	
			for (List<Link> seqFibers : paths)
			{
				ModulationFormat modulationFormat = modulationFormatPerPath.get(seqFibers);
				final int numSlots = numSlotsPerModulationPerService.get(modulationFormat)[serviceId];
				final int [] linkIndexes = IntUtils.toArray(NetPlan.getIndexes(seqFibers));
				DoubleMatrix1D isOccupiedInPath_s = fiberSlotOccupancyMap_fs.viewSelection(linkIndexes, null).viewDice().zMult(DoubleFactory1D.dense.make(linkIndexes.length, 1.0) , null);
				int initialSlot = -1; int numZeroSlotsSoFar = 0;
				for (int s = 0; s < isOccupiedInPath_s.size () ; s ++)
				{
					if (isOccupiedInPath_s.get(s) == 0)
					{
						if (initialSlot == -1) { initialSlot = s; numZeroSlotsSoFar = 1; }
						else { numZeroSlotsSoFar ++; }
					}
					else
					{
						initialSlot = -1; numZeroSlotsSoFar = 0;
					}
					if (numZeroSlotsSoFar == numSlots)
						return Quadruple.of(seqFibers, modulationFormat, initialSlot, numSlots);
				}
			}
		}
	
		else if (this.rsaAlgorithmType.getString ().equalsIgnoreCase("fFFragmentationAware"))
		{
			
			Quadruple<List<Link>, ModulationFormat, Integer, Integer> bestAllocation = null;
			double best_f_cmt = Double.MAX_VALUE;

			if (paths.isEmpty()) throw new Net2PlanException("No path between end nodes");
			for (List<Link> seqFibers : paths)
			{
				final ModulationFormat modulationFormat = modulationFormatPerPath.get(seqFibers);
				final int numSlots = numSlotsPerModulationPerService.get(modulationFormat)[serviceId];

				final TreeSet<Integer> pathSlotOccupancy = WDMUtils.computePathSlotOccupancy(seqFibers, fiberSlotOccupancyMap_fs.viewDice());
				final List<Pair<Integer, Integer>> candidateVoids = WDMUtils.computeAvailableSpectrumVoids(pathSlotOccupancy, totalAvailableSlotsPerFiber);

				int residualCapacity = 0;
				for (Pair<Integer, Integer> candidateVoid : candidateVoids)
					residualCapacity += candidateVoid.getSecond();

				for (Pair<Integer, Integer> candidateVoid : candidateVoids)
				{
					int numSlotsThisVoid = candidateVoid.getSecond();
					if (numSlotsThisVoid < numSlots) continue;

					int firstPossibleInitialSlotId = candidateVoid.getFirst();
					int lastPossibleInitialSlotId = firstPossibleInitialSlotId + numSlotsThisVoid - numSlots;

					for (int initialSlotId = firstPossibleInitialSlotId; initialSlotId <= lastPossibleInitialSlotId; initialSlotId++)
					{
						int f_c = compute_f_c(seqFibers, initialSlotId, numSlots, fiberSlotOccupancyMap_fs, totalAvailableSlotsPerFiber);

						Set<Link> neighborFibers = neighborLinks_p.get(seqFibers);
						int numNeighborLinks = neighborFibers.size();
						int f_m = compute_f_m(neighborFibers, initialSlotId, numSlots, fiberSlotOccupancyMap_fs);

						double f_cmt = compute_f_cmt(f_c, f_m, seqFibers, numSlots, numNeighborLinks, residualCapacity);

						if (bestAllocation == null || f_cmt < best_f_cmt)
						{
							bestAllocation = Quadruple.of(seqFibers, modulationFormat, initialSlotId, numSlots);
							best_f_cmt = f_cmt;
						}
					}
				}
			}

			return bestAllocation;
		}
		return null;
	}

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return null;
	}
	
	private static int compute_f_c(List<Link> seqFibers, int initialSlotId, int numSlots, DoubleMatrix2D fiberSlotOccupancyMap_fs, int totalAvailableSlotsPerFiber)
	{
		int lastSlotId = initialSlotId + numSlots - 1;
		int f_c = 0;

		for (Link fiber : seqFibers)
		{
			boolean freeAtLeftHandSide = initialSlotId == 0 || (fiberSlotOccupancyMap_fs.get(fiber.getIndex () , initialSlotId - 1) == 0);
			boolean freeAtRightHandSide = lastSlotId == totalAvailableSlotsPerFiber - 1 || (fiberSlotOccupancyMap_fs.get(fiber.getIndex () , lastSlotId + 1) == 0);
			if (freeAtLeftHandSide && freeAtRightHandSide) f_c++;
		}

		return f_c;
	}
	
	protected Set<Link> computeNeighborLinks(NetPlan netPlan, List<Link> seqFibers)
	{
		Set<Link> neighborLinks = new HashSet<Link>();

		for(Link fiber : seqFibers)
		{

			Node originNode = netPlan.getLinkFromId(fiber.getId()).getOriginNode();
			Node destinationNode =  netPlan.getLinkFromId(fiber.getId()).getDestinationNode();

			Set<Link> incomingLinks = originNode.getIncomingLinks();
			for (Link link : incomingLinks)
			{
				if (CollectionUtils.contains(seqFibers, link)) continue;				
				if (netPlan.getLinkFromId(link.getId()).getDestinationNode().getId() == destinationNode.getId()) continue;

				neighborLinks.add(link);
			}

			Set<Link> outgoingLinks = destinationNode.getOutgoingLinks();
			for (Link link : outgoingLinks)
			{
				if (CollectionUtils.contains(seqFibers, link)) continue;
				if (netPlan.getLinkFromId(link.getId()).getOriginNode().getId() == originNode.getId()) continue;

				neighborLinks.add(link);
			}
		}

		return neighborLinks;
	}
	
	private static double compute_f_cmt(int f_c, int f_m, List<Link> seqFibers, int numSlots, int numNeighborLinks, int residualCapacity)
	{
		if (residualCapacity == 0 || numNeighborLinks == 0) return Double.MAX_VALUE;

		int numHops = seqFibers.size();
		double misalignmentFactor = (double) f_m / (numSlots * numNeighborLinks);
		double trafficFactor = numHops * (double) numSlots / residualCapacity;
		double f_cmt = f_c + misalignmentFactor + trafficFactor;

		return f_cmt;
	}
	
	private static int compute_f_m(Set<Link> neighborFibers, int initialSlotId, int numSlots, DoubleMatrix2D fiberSlotOccupancyMap_fs)
	{
		int f_m = 0;
		int lastSlotId = initialSlotId + numSlots - 1;

		for (Link fiber : neighborFibers)
		{
			for (int slotId = initialSlotId; slotId <= lastSlotId; slotId++)
			{
				if (fiberSlotOccupancyMap_fs.get (fiber.getIndex () , slotId) == 1) f_m--;
				else f_m++;
			}
		}

		return f_m;
	}
	
}
