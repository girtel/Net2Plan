package com.net2plan.libraries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.jom.OptimizationProblem;
import com.net2plan.interfaces.networkDesign.Configuration;
import com.net2plan.interfaces.networkDesign.Demand;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.MulticastDemand;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.Node;

import cern.colt.matrix.tdouble.DoubleFactory2D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;

public class TrafficMatrixForecastUtils 
{
	public static class TmEstimationResults
	{
    	private final NetworkLayer input_layer;
		private final double output_averageDeviation_d;
    	private final double output_averageDeviation_md;
    	private final double output_averageDeviation_e;
    	private final SortedMap<Demand,Double> output_estimTraffic_d;
    	private final SortedMap<MulticastDemand,Double> output_estimTraffic_md;
    	
    	private TmEstimationResults(NetworkLayer input_layer, double output_averageDeviation_d,
				double output_averageDeviation_md, double output_averageDeviation_e, Map<Demand,Double> output_estimTraffic_d,
				Map<MulticastDemand,Double> output_estimTraffic_md) {
			super();
			this.input_layer = input_layer;
			this.output_averageDeviation_d = output_averageDeviation_d;
			this.output_averageDeviation_md = output_averageDeviation_md;
			this.output_averageDeviation_e = output_averageDeviation_e;
			this.output_estimTraffic_d = new TreeMap<> (output_estimTraffic_d);
			this.output_estimTraffic_md = new TreeMap<> (output_estimTraffic_md);
		}

    	public void setOfferedTrafficsToTheOnesEstimated ()
    	{
    		final NetPlan np = input_layer.getNetPlan();
    		if (!new HashSet<> (np.getDemands(input_layer)).equals(output_estimTraffic_d.keySet()))throw new Net2PlanException ("The set of demands is different");
    		if (!new HashSet<> (np.getMulticastDemands(input_layer)).equals(output_estimTraffic_md.keySet()))throw new Net2PlanException ("The set of multicast demands is different");
    		for (Entry<Demand,Double> val : output_estimTraffic_d.entrySet())
    			val.getKey().setOfferedTraffic(val.getValue());
    		for (Entry<MulticastDemand,Double> val : output_estimTraffic_md.entrySet())
    			val.getKey().setOfferedTraffic(val.getValue());
    	}
    	
    	public Double getEstimationDemand (Demand d) { return output_estimTraffic_d.get(d); }

    	public Double getEstimationMDemand (MulticastDemand d) { return output_estimTraffic_md.get(d); }

    	public SortedMap<Demand,Double> getEstimationDemands () { return Collections.unmodifiableSortedMap(output_estimTraffic_d); }

    	public SortedMap<MulticastDemand,Double> getEstimationMulticastDemands () { return Collections.unmodifiableSortedMap(output_estimTraffic_md); }

		private static TmEstimationResults  createTakingEstimatedValuesFromCurrentNp (NetworkLayer layer , 
    			Map<Link,Double> monitOfSomeLinkTraffics , 
        		Map<Demand,Double> monitOfSomeDemands , 
        		Map<MulticastDemand,Double> monitOfSomeMDemands)
    	{
			final NetPlan np = layer.getNetPlan();
        	final double output_averageDeviation_d = Math.sqrt(monitOfSomeDemands.entrySet().stream().mapToDouble(e-> { Demand d = e.getKey(); double est = e.getValue(); double val = d.getOfferedTraffic(); return Math.pow (est - val , 2);    }).sum ());
        	final double output_averageDeviation_md = Math.sqrt(monitOfSomeMDemands.entrySet().stream().mapToDouble(e-> { MulticastDemand d = e.getKey(); double est = e.getValue(); double val = d.getOfferedTraffic(); return Math.pow (est - val , 2);    }).sum ());
        	final double output_averageDeviation_e = Math.sqrt(monitOfSomeLinkTraffics.entrySet().stream().mapToDouble(e-> { Link d = e.getKey(); double est = e.getValue(); double val = d.getCarriedTraffic(); return Math.pow (est - val , 2);    }).sum ());
        	final Map<Demand,Double> sol_offered_d = np.getDemands(layer).stream().collect(Collectors.toMap(d->d , d->d.getOfferedTraffic ()));
        	final Map<MulticastDemand,Double> sol_offered_md = np.getMulticastDemands(layer).stream().collect(Collectors.toMap(d->d , d->d.getOfferedTraffic ()));
    		final TmEstimationResults tm = new TmEstimationResults(layer, output_averageDeviation_d, output_averageDeviation_md, output_averageDeviation_e, 
    				sol_offered_d, 
    				sol_offered_md);
        	return tm;
    	}
    	public String toStringQualityMerits ()
    	{
    		return "Demand: Typical deviation monitored from estimated: " +  output_averageDeviation_d + String.format("%n") + 
    				"M-Demand: Typical deviation monitored from estimated: " +  output_averageDeviation_md + String.format("%n") +
    				"Link traffic: Typical deviation monitored from estimated: " + output_averageDeviation_e;
    	}
	}
	
    public static TmEstimationResults getTmEstimation_minErrorSquares (NetworkLayer layer , 
    		Map<Link,Double> inputMonitInfo_someLinks , 
    		Map<Demand,Double> inputMonitInfo_someDemands , 
    		Map<MulticastDemand,Double> inputMonitInfo_someMDemands,
    		double coeff_preferFitRouting0PreferFitDemand1)
    {
    	if (coeff_preferFitRouting0PreferFitDemand1 < 0 || coeff_preferFitRouting0PreferFitDemand1 > 1) throw new Net2PlanException ("Wrong parameter. Coefficient must be in [0 , 1]");
    	
    	/* Remove links/demnads/mdemands of a different layer */
    	if (inputMonitInfo_someDemands == null) inputMonitInfo_someDemands = new HashMap<> (); else inputMonitInfo_someDemands = inputMonitInfo_someDemands.entrySet().stream ().filter(e->e.getKey().getLayer().equals(layer)).collect (Collectors.toMap(e->e.getKey (), e->e.getValue()));
    	if (inputMonitInfo_someMDemands == null) inputMonitInfo_someMDemands = new HashMap<> ();  else inputMonitInfo_someMDemands = inputMonitInfo_someMDemands.entrySet().stream ().filter(e->e.getKey().getLayer().equals(layer)).collect (Collectors.toMap(e->e.getKey (), e->e.getValue()));
    	if (inputMonitInfo_someLinks == null) inputMonitInfo_someLinks = new HashMap<> ();  else inputMonitInfo_someLinks = inputMonitInfo_someLinks.entrySet().stream ().filter(e->e.getKey().getLayer().equals(layer)).collect (Collectors.toMap(e->e.getKey (), e->e.getValue()));
    	
    	final NetPlan np = layer.getNetPlan();
    	final List<Link> links = np.getLinks (layer);
    	final List<Demand> demands = np.getDemands(layer);
    	final List<MulticastDemand> mdemands = np.getMulticastDemands(layer);
    	final int E = links.size();
    	final int D = demands.size();
    	final int MD = mdemands.size();
    	final int EST_E = inputMonitInfo_someLinks.size();
    	final int EST_D = inputMonitInfo_someDemands.size();
    	final int EST_MD = inputMonitInfo_someMDemands.size();
    	

    	final List<Double> offeredTraffic_d = new ArrayList<> (Collections.nCopies(D, 0.0));
    	final List<Double> offeredTraffic_md = new ArrayList<> (Collections.nCopies(MD, 0.0));
    	if (D == 0 && MD == 0) return TmEstimationResults.createTakingEstimatedValuesFromCurrentNp(layer, inputMonitInfo_someLinks, inputMonitInfo_someDemands, inputMonitInfo_someMDemands); // no demands meand no traffic

    	final OptimizationProblem op = new OptimizationProblem();
    	if (D>0) op.addDecisionVariable("h_d", false, new int [] { 1 , D} , 0 , Double.MAX_VALUE);
    	if (MD>0) op.addDecisionVariable("h_md", false, new int [] { 1 , MD} , 0 , Double.MAX_VALUE);
    	op.setInputParameter("a", coeff_preferFitRouting0PreferFitDemand1);
    	
    	final StringBuffer sbObjFunction = new StringBuffer ();
    	final boolean existsSummandOfLinkTrafficConstraints = EST_E > 0 && coeff_preferFitRouting0PreferFitDemand1 != 1; 
    	final boolean existsSummandOfDemandTrafficConstraints = (EST_D > 0 || EST_MD > 0) && coeff_preferFitRouting0PreferFitDemand1 != 0; 
    	if (!existsSummandOfDemandTrafficConstraints && !existsSummandOfLinkTrafficConstraints) throw new Net2PlanException ("No input monitoring information exists for estimating the matrix");
    	
    	if (existsSummandOfLinkTrafficConstraints)
    	{
        	final double [] linkCarriedTrafficEstim_e = new double [E];
        	final List<Integer> linkIndexes_withEstim = new ArrayList<> (inputMonitInfo_someLinks.size());
        	for (Entry<Link,Double> info_e : inputMonitInfo_someLinks.entrySet())
        	{
        		linkIndexes_withEstim.add (info_e.getKey().getIndex());
        		linkCarriedTrafficEstim_e [info_e.getKey().getIndex()] = info_e.getValue();
        	}
        	op.setInputParameter("estIndexes_e", linkIndexes_withEstim , "row");
        	op.setInputParameter("estValues_e", linkCarriedTrafficEstim_e , "row");
        	final DoubleMatrix2D x_de = DoubleFactory2D.sparse.make(D , E);
        	final DoubleMatrix2D x_mde = DoubleFactory2D.sparse.make(MD , E);
        	for (Demand d : demands)
        		for (Entry<Link,Double> fr : d.getTraversedLinksAndCarriedTraffic(true).entrySet())
    				x_de.set(d.getIndex(), fr.getKey().getIndex(), fr.getValue());
        	for (MulticastDemand d : mdemands)
        		for (Entry<Link,Double> fr : d.getTraversedLinksAndCarriedTraffic(true).entrySet())
        			x_mde.set(d.getIndex(), fr.getKey().getIndex(), fr.getValue());
        	if (D>0) op.setInputParameter("x_de", x_de);
        	if (MD>0) op.setInputParameter("x_mde", x_mde);
        	
        	if (D > 0 && MD > 0) sbObjFunction.append ("(1-a) * ( sum( ( h_d*x_de(all,estIndexes_e) + h_md*x_mde(all,estIndexes_e) - estValues_e(estIndexes_e))^2) )");
        	else if (D > 0 && MD == 0) sbObjFunction.append ("(1-a) * ( sum( ( h_d*x_de(all,estIndexes_e) - estValues_e(estIndexes_e))^2) )");
        	else if (D == 0 && MD > 0) sbObjFunction.append ("(1-a) * ( sum( ( h_md*x_mde(all,estIndexes_e) - estValues_e(estIndexes_e))^2) )");
    	}
    	if (existsSummandOfLinkTrafficConstraints && existsSummandOfDemandTrafficConstraints) sbObjFunction.append(" + ");
    	if (existsSummandOfDemandTrafficConstraints)
    	{
        	final List<Integer> demandIndexesWithEstim_estd = new ArrayList<> ();
        	final List<Integer> mdemandIndexesWithEstim_estmd = new ArrayList<> ();
        	final double [] demandTrafficsEstim_d = new double [D];
        	final double [] demandTrafficsEstim_md = new double [MD];
    		for (Entry<Demand,Double> dEstim : inputMonitInfo_someDemands.entrySet())
    		{
    			final Demand d = dEstim.getKey();
    			final double traf = dEstim.getValue();
    			demandIndexesWithEstim_estd.add(d.getIndex());
    			demandTrafficsEstim_d [d.getIndex()] = traf;
    		}
    		for (Entry<MulticastDemand,Double> dEstim : inputMonitInfo_someMDemands.entrySet())
    		{
    			final MulticastDemand d = dEstim.getKey();
    			final double traf = dEstim.getValue();
    			mdemandIndexesWithEstim_estmd.add(d.getIndex());
    			demandTrafficsEstim_md [d.getIndex()] = traf;
    		}
    		if (!demandIndexesWithEstim_estd.isEmpty()) op.setInputParameter("estIndexes_estd", demandIndexesWithEstim_estd , "row");
        	if (!mdemandIndexesWithEstim_estmd.isEmpty()) op.setInputParameter("estIndexes_estmd", mdemandIndexesWithEstim_estmd , "row");
        	if (D>0) op.setInputParameter("estValues_d", demandTrafficsEstim_d , "row");
        	if (MD>0) op.setInputParameter("estValues_md", demandTrafficsEstim_md , "row");
        	
        	if (EST_D > 0 && EST_MD > 0) sbObjFunction.append ("a* ( sum ( (h_d(estIndexes_estd) - estValues_d(estIndexes_estd))^2 ) + sum ( (h_md(estIndexes_estmd) - estValues_md(estIndexes_estmd))^2 ) )");
        	else if (EST_D > 0 && EST_MD == 0) sbObjFunction.append ("a* ( sum ( (h_d(estIndexes_estd) - estValues_d(estIndexes_estd))^2 )  )");
        	else if (EST_D == 0 && EST_MD > 0) sbObjFunction.append ("a* ( sum ( (h_md(estIndexes_estmd) - estValues_md(estIndexes_estmd))^2 ) )");
    	}
    	
//    	System.out.println("a: " + op.getInputParameter("a").toValue());
//    	if (op.getInputParameter("x_de") != null)
//    		System.out.println("x_de: " + op.getInputParameter("x_de").view2D());
//    	if (op.getInputParameter("x_mde") != null)
//    		System.out.println("x_mde: " + op.getInputParameter("x_mde").view2D());
//    	if (existsSummandOfDemandTrafficConstraints) System.out.println("estIndexes_estd: (" + op.getInputParameter("estIndexes_estd").toList().size() + "): "  + op.getInputParameter("estIndexes_estd").toList());
//    	if (existsSummandOfDemandTrafficConstraints) System.out.println("estValues_d: (" + op.getInputParameter("estValues_d").toList().size() + "): " + op.getInputParameter("estValues_d").toList());
//    	if (existsSummandOfLinkTrafficConstraints) System.out.println("estIndexes_e: (" + op.getInputParameter("estIndexes_e").toList().size() + "): "  + op.getInputParameter("estIndexes_e").toList());
//    	if (existsSummandOfLinkTrafficConstraints) System.out.println("estValues_e: (" + op.getInputParameter("estValues_e").toList().size() + "): "  + op.getInputParameter("estValues_e").toList());
//    	System.out.println("Ob func: " + sbObjFunction.toString());
    	
    	op.setObjectiveFunction("minimize", sbObjFunction.toString());
    	
    	op.solve("ipopt" , "solverLibraryName" , Configuration.getOption("ipoptSolverLibraryName"));
    	
    	if (!op.solutionIsFeasible()) throw new Net2PlanException ("A feasible solution was not found");
    	
    	final DoubleMatrix1D originalDemandOffered_d = np.getVectorDemandOfferedTraffic(layer);
    	final DoubleMatrix1D originalDemandOffered_md = np.getVectorMulticastDemandOfferedTraffic(layer);
    	if (D>0)
    	{
    		final double [] h_d = op.getPrimalSolution("h_d").to1DArray();
    		for (int dIndex = 0; dIndex < h_d.length ; dIndex ++)
    		{
    			offeredTraffic_d.set(dIndex, h_d [dIndex]);
    			np.getDemand(dIndex, layer).setOfferedTraffic(h_d [dIndex]);
    		}
    	}
    	if (MD>0)
    	{
    		final double [] h_md = op.getPrimalSolution("h_md").to1DArray();
    		for (int dIndex = 0; dIndex < h_md.length ; dIndex ++)
    		{
    			offeredTraffic_md.set(dIndex, h_md [dIndex]);
    			np.getMulticastDemand(dIndex, layer).setOfferedTraffic(h_md [dIndex]);
    		}
    	}
    	
    	final double averageDeviation_d = inputMonitInfo_someDemands.entrySet().stream().mapToDouble(e-> { Demand d = e.getKey(); double est = e.getValue(); double val = d.getOfferedTraffic(); return Math.pow (est - val , 2);    }).sum ();
    	final double averageDeviation_md = inputMonitInfo_someMDemands.entrySet().stream().mapToDouble(e-> { MulticastDemand d = e.getKey(); double est = e.getValue(); double val = d.getOfferedTraffic(); return Math.pow (est - val , 2);    }).sum ();
    	final double averageDeviation_e = inputMonitInfo_someLinks.entrySet().stream().mapToDouble(e-> { Link d = e.getKey(); double est = e.getValue(); double val = d.getCarriedTraffic(); return Math.pow (est - val , 2);    }).sum ();
//    	System.out.println("averageDeviation_d: " + Math.sqrt(averageDeviation_d));
//    	System.out.println("averageDeviation_md: " + Math.sqrt(averageDeviation_md));
//    	System.out.println("averageDeviation_e: " + Math.sqrt(averageDeviation_e));
    	
    	final TmEstimationResults res = TmEstimationResults.createTakingEstimatedValuesFromCurrentNp(layer, inputMonitInfo_someLinks, inputMonitInfo_someDemands, inputMonitInfo_someMDemands);
    	
    	/* Restore original values */
    	np.setVectorDemandOfferedTraffic(originalDemandOffered_d, layer);
    	np.setVectorMulticastDemandOfferedTraffic(originalDemandOffered_md, layer);
    	
    	return res; 
    }
    
    public static SortedSet<Date> getDatesWithAtLeastOneLinkMonitorInfo (NetworkLayer layer)
    {
    	return layer.getNetPlan().getLinks(layer).stream().map(e->e.getMonitoredOrForecastedCarriedTraffic().getDatesWithValue()).flatMap(e->e.stream()).collect(Collectors.toCollection(TreeSet::new));
    }
    public static SortedSet<Date> getDatesWithAtLeastOneUnicastDemandMonitorInfo (NetworkLayer layer)
    {
    	return layer.getNetPlan().getDemands(layer).stream().map(e->e.getMonitoredOrForecastedOfferedTraffic().getDatesWithValue()).flatMap(e->e.stream()).collect(Collectors.toCollection(TreeSet::new));
    }
    public static SortedSet<Date> getDatesWithAtLeastOneMulticastDemandMonitorInfo (NetworkLayer layer)
    {
    	return layer.getNetPlan().getMulticastDemands(layer).stream().map(e->e.getMonitoredOrForecastedOfferedTraffic().getDatesWithValue()).flatMap(e->e.stream()).collect(Collectors.toCollection(TreeSet::new));
    }
    public static boolean isGravityModelApplicableWithLinkMonitoringInfo (NetworkLayer layer , Date date)
    {
    	final NetPlan np = layer.getNetPlan();
    	final List<Link> links = np.getLinks(layer);
    	final List<Demand> demands = np.getDemands(layer);
    	if (links.isEmpty()) return false; 
    	if (np.hasMulticastDemands(layer)) return false; 
    	final SortedSet<Node> demandEndNodes = demands.stream().map(d->d.getEndNodes()).flatMap(d->d.stream()).collect(Collectors.toCollection(TreeSet::new));
    	final SortedSet<Link> linksWeNeedToHaveFullInOutInfo = new TreeSet<> ();
    	if (demands.stream().map(d->d.getIngressNode()).anyMatch(n->n.getOutgoingLinks(layer).isEmpty())) return false; 
    	if (demands.stream().map(d->d.getEgressNode()).anyMatch(n->n.getIncomingLinks(layer).isEmpty())) return false; 
    	demandEndNodes.forEach(n->linksWeNeedToHaveFullInOutInfo.addAll(n.getOutgoingLinks(layer)));
    	demandEndNodes.forEach(n->linksWeNeedToHaveFullInOutInfo.addAll(n.getIncomingLinks(layer)));
		return linksWeNeedToHaveFullInOutInfo.stream().allMatch(ee->ee.getMonitoredOrForecastedCarriedTraffic().hasValue (date));
    }
    public static SortedSet<Date> getDatesWhereGravityModelCanBeApplied (NetworkLayer layer)
    {
    	final NetPlan np = layer.getNetPlan();
    	final List<Link> links = np.getLinks(layer);
    	final List<Demand> demands = np.getDemands(layer);
    	if (links.isEmpty()) return new TreeSet<> (); 
    	if (np.hasMulticastDemands(layer)) return new TreeSet<> (); 
    	final SortedSet<Node> demandEndNodes = demands.stream().map(d->d.getEndNodes()).flatMap(d->d.stream()).collect(Collectors.toCollection(TreeSet::new));
    	final SortedSet<Link> linksWeNeedToHaveFullInOutInfo = new TreeSet<> ();
    	if (demands.stream().map(d->d.getIngressNode()).anyMatch(n->n.getOutgoingLinks(layer).isEmpty())) return new TreeSet<> (); 
    	if (demands.stream().map(d->d.getEgressNode()).anyMatch(n->n.getIncomingLinks(layer).isEmpty())) return new TreeSet<> (); 
    	demandEndNodes.forEach(n->linksWeNeedToHaveFullInOutInfo.addAll(n.getOutgoingLinks(layer)));
    	demandEndNodes.forEach(n->linksWeNeedToHaveFullInOutInfo.addAll(n.getIncomingLinks(layer)));
    	final SortedSet<Date> datesWithEnoughInformationFromGM = new TreeSet<> ();
    	for (Date dateFirstLink : linksWeNeedToHaveFullInOutInfo.first().getMonitoredOrForecastedCarriedTraffic().getDatesWithValue())
    		if (linksWeNeedToHaveFullInOutInfo.stream().allMatch(ee->ee.getMonitoredOrForecastedCarriedTraffic().hasValue (dateFirstLink)))
    			datesWithEnoughInformationFromGM.add(dateFirstLink);
    	return datesWithEnoughInformationFromGM;
    }
    public static SortedMap<Demand,Double> getGravityModelEstimationFromMonitorTraffic (NetworkLayer layer , Date date)
    {
    	if (!isGravityModelApplicableWithLinkMonitoringInfo(layer, date)) throw new Net2PlanException ("Gravity model not applicable");
    	final NetPlan np = layer.getNetPlan();
    	final List<Link> links = np.getLinks(layer);
    	final List<Demand> demands = np.getDemands(layer);
		final int N = np.getNumberOfNodes();
		final double [] ingressTrafficPerNode = new double [N];
		final double [] egressTrafficPerNode = new double [N];
		for (Node n : np.getNodes())
		{
			ingressTrafficPerNode [n.getIndex()] = n.getIncomingLinks(layer).stream().mapToDouble(e->e.getMonitoredOrForecastedCarriedTraffic().getValueOrNull(date)).sum();
			egressTrafficPerNode [n.getIndex()] = n.getOutgoingLinks(layer).stream().mapToDouble(e->e.getMonitoredOrForecastedCarriedTraffic().getValueOrNull(date)).sum();
		}
		final DoubleMatrix2D tm = TrafficMatrixGenerationModels.gravityModel(ingressTrafficPerNode, egressTrafficPerNode);
		final SortedMap<Demand,Double> res = new TreeMap<> ();
		for (Node n1 : np.getNodes())
			for (Node n2 : np.getNodes())
			{
				if (n1 == n2) continue;
				final SortedSet<Demand> nodePairDemands = np.getNodePairDemands(n1, n2, false, layer);
				final double traf = tm.get(n1.getIndex(), n2.getIndex());
				nodePairDemands.forEach(d->res.put(d, traf / nodePairDemands.size()));
			}
		return res;
    }

    public static SortedMap<Demand,Double> getGravityModelEstimationFromCurrentCarriedTraffic (NetworkLayer layer)
    {
    	final NetPlan np = layer.getNetPlan();
		final int N = np.getNumberOfNodes();
		final double [] ingressTrafficPerNode = new double [N];
		final double [] egressTrafficPerNode = new double [N];
		for (Node n : np.getNodes())
		{
			ingressTrafficPerNode [n.getIndex()] = n.getIncomingLinks(layer).stream().mapToDouble(e->e.getCarriedTraffic()).sum();
			egressTrafficPerNode [n.getIndex()] = n.getOutgoingLinks(layer).stream().mapToDouble(e->e.getCarriedTraffic()).sum();
		}
		final DoubleMatrix2D tm = TrafficMatrixGenerationModels.gravityModel(ingressTrafficPerNode, egressTrafficPerNode);
		final SortedMap<Demand,Double> res = new TreeMap<> ();
		for (Node n1 : np.getNodes())
			for (Node n2 : np.getNodes())
			{
				if (n1 == n2) continue;
				final SortedSet<Demand> nodePairDemands = np.getNodePairDemands(n1, n2, false, layer);
				final double traf = tm.get(n1.getIndex(), n2.getIndex());
				nodePairDemands.forEach(d->res.put(d, traf / nodePairDemands.size()));
			}
		return res;
    }

    
    
}
