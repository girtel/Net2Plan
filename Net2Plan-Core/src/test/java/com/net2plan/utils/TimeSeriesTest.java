package com.net2plan.utils;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Sets;
import com.net2plan.interfaces.networkDesign.Demand;
import com.net2plan.interfaces.networkDesign.Link;
import com.net2plan.interfaces.networkDesign.MulticastDemand;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.interfaces.networkDesign.Node;
import com.net2plan.libraries.GraphUtils;
import com.net2plan.libraries.IPUtils;
import com.net2plan.libraries.TrafficMatrixForecastUtils;
import com.net2plan.libraries.TrafficMatrixForecastUtils.TmEstimationResults;
import com.net2plan.libraries.TrafficSeries;
import com.net2plan.libraries.TrafficSeries.FITTINGTYPE;
import com.net2plan.libraries.TrafficSeries.TrafficPredictor;
import com.net2plan.utils.Constants.RoutingType;

public class TimeSeriesTest 
{
	private NetPlan np;
	private Node n1,n2,n3,n4,n5;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception 
	{
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception 
	{
		this.np = new NetPlan ();
		this.n1 = np.addNode(0, 0, "n1", null);
		this.n2 = np.addNode(0, 0, "n2", null);
		this.n3 = np.addNode(0, 0, "n3", null);
		this.n4 = np.addNode(0, 0, "n4", null);
		this.n5 = np.addNode(0, 0, "n5", null);
		np.addLinkBidirectional(n1, n2, 100, 10, 200000, null);
		np.addLinkBidirectional(n2, n3, 100, 10, 200000, null);
		np.addLinkBidirectional(n3, n4, 100, 10, 200000, null);
		np.addLinkBidirectional(n4, n1, 100, 10, 200000, null);
		np.addLinkBidirectional(n1, n5, 100, 10, 200000, null);
		np.addLinkBidirectional(n5, n4, 100, 10, 200000, null);
		final Random rng = new Random (1);
		for (Node n : np.getNodes())
			for (Node nn : np.getNodes())
			{
				if (n == nn) continue;
				np.addDemand(n, nn, rng.nextDouble(), RoutingType.HOP_BY_HOP_ROUTING, null);
			}
		IPUtils.setECMPForwardingRulesFromLinkWeights(np, null);
	}

	@After
	public void tearDown() throws Exception 
	{
	}

	@Test
	public void testTrafficForecast_hopbyhop ()
	{
		final List<Demand>demands = np.getDemands();
		final List<MulticastDemand>mdemands = np.getMulticastDemands();
		final List<Link> links = np.getLinks();
		final SortedMap<Demand,Double> originalDemandOffered = new TreeMap<> (np.getDemands().stream().collect(Collectors.toMap(e->e, e->e.getOfferedTraffic())));
		final SortedMap<MulticastDemand,Double> originalMDemandOffered = new TreeMap<> (np.getMulticastDemands().stream().collect(Collectors.toMap(e->e, e->e.getOfferedTraffic())));
		final SortedMap<Link,Double> originalLinkCarried = new TreeMap<> (np.getLinks().stream().collect(Collectors.toMap(e->e, e->e.getCarriedTraffic())));
		final SortedMap<Link,Double> originalEvenLinkCarried = new TreeMap<> (np.getLinks().stream().filter(e->e.getIndex()%2 == 0).collect(Collectors.toMap(e->e, e->e.getCarriedTraffic())));
		final SortedMap<Demand,Double> originalEvenDemandOffered = new TreeMap<> (np.getDemands().stream().filter(d->d.getIndex()%2 == 0).collect(Collectors.toMap(e->e, e->e.getOfferedTraffic())));
		final SortedMap<MulticastDemand,Double> originalEvenMDemandOffered = new TreeMap<> (np.getMulticastDemands().stream().filter(d->d.getIndex()%2 == 0).collect(Collectors.toMap(e->e, e->e.getOfferedTraffic())));

		final NetworkLayer layer = np.getNetworkLayerDefault();
		final SortedMap<Demand,Double> origGravityModel = TrafficMatrixForecastUtils.getGravityModelEstimationFromCurrentCarriedTraffic(layer);
		TmEstimationResults tm;
		
		/* Minimize distance to true demand and link traffic --> should match it, unless demand info is disregarded */
		tm = TrafficMatrixForecastUtils.getTmEstimation_minErrorSquares(layer, originalLinkCarried, originalDemandOffered, originalMDemandOffered, 1.0);
		tm.setOfferedTrafficsToTheOnesEstimated ();
		for (Demand d : demands) assertEquals (originalDemandOffered.get(d) , d.getOfferedTraffic() , 0.01);
		for (MulticastDemand d : mdemands) assertEquals (originalMDemandOffered.get(d) , d.getOfferedTraffic() , 0.01);
		for (Link e : links) assertEquals (originalLinkCarried.get(e) , e.getCarriedTraffic() , 0.01);
		
		tm = TrafficMatrixForecastUtils.getTmEstimation_minErrorSquares(layer, originalLinkCarried, originalDemandOffered, originalMDemandOffered, 0.5);
		tm.setOfferedTrafficsToTheOnesEstimated ();
		for (MulticastDemand d : mdemands) assertEquals (originalMDemandOffered.get(d) , d.getOfferedTraffic() , 0.01);
		for (Demand d : demands) assertEquals (originalDemandOffered.get(d) , d.getOfferedTraffic() , 0.01);
		for (Link e : links) assertEquals (originalLinkCarried.get(e) , e.getCarriedTraffic() , 0.01);
	
		tm = TrafficMatrixForecastUtils.getTmEstimation_minErrorSquares(layer, originalEvenLinkCarried, originalDemandOffered, originalMDemandOffered, 0.5);
		tm.setOfferedTrafficsToTheOnesEstimated ();
		for (Demand d : demands) assertEquals (originalDemandOffered.get(d) , d.getOfferedTraffic() , 0.01);
		for (MulticastDemand d : mdemands) assertEquals (originalMDemandOffered.get(d) , d.getOfferedTraffic() , 0.01);
		for (Link e : links) assertEquals (originalLinkCarried.get(e) , e.getCarriedTraffic() , 0.01);
	
		tm = TrafficMatrixForecastUtils.getTmEstimation_minErrorSquares(layer, null, originalDemandOffered, originalMDemandOffered, 0.5);
		tm.setOfferedTrafficsToTheOnesEstimated ();
		for (Demand d : demands) assertEquals (originalDemandOffered.get(d) , d.getOfferedTraffic() , 0.01);
		for (MulticastDemand d : mdemands) assertEquals (originalMDemandOffered.get(d) , d.getOfferedTraffic() , 0.01);
		for (Link e : links) assertEquals (originalLinkCarried.get(e) , e.getCarriedTraffic() , 0.01);
	
		tm = TrafficMatrixForecastUtils.getTmEstimation_minErrorSquares(layer, originalLinkCarried, originalDemandOffered, originalMDemandOffered, 0.0);
		tm.setOfferedTrafficsToTheOnesEstimated ();
		for (Link e : links) assertEquals (originalLinkCarried.get(e) , e.getCarriedTraffic() , 0.01);

		tm = TrafficMatrixForecastUtils.getTmEstimation_minErrorSquares(layer, originalLinkCarried, originalEvenDemandOffered, originalEvenMDemandOffered, 0.0);
		tm.setOfferedTrafficsToTheOnesEstimated ();
		for (Link e : links) assertEquals (originalLinkCarried.get(e) , e.getCarriedTraffic() , 0.01);
		
		/* Look like gravity model if we force to */
		if (np.getNumberOfMulticastDemands() == 0)
		{
			tm = TrafficMatrixForecastUtils.getTmEstimation_minErrorSquares(layer, originalLinkCarried, origGravityModel, null, 1.0);
			tm.setOfferedTrafficsToTheOnesEstimated ();
			for (Demand d : demands) assertEquals (origGravityModel.get(d) , d.getOfferedTraffic() , 0.01);
		}
	}

	@Test
	public void testTrafficForecast_sourcerouting ()
	{
		np.setRoutingTypeAllDemands(RoutingType.SOURCE_ROUTING);
		this.testTrafficForecast_hopbyhop();
	}
	
	@Test
	public void testTrafficForecast_withMulticastDemand ()
	{
		np.setRoutingTypeAllDemands(RoutingType.SOURCE_ROUTING);
		final MulticastDemand md = np.addMulticastDemand(n1, Sets.newHashSet(n2,n4,n5), 2.0, null);
		np.addMulticastTree(md, 1.5, 1.5, GraphUtils.getMinimumCostMulticastTree(null, null, null, null, n1, Sets.newHashSet(n2,n4,n5), -1, -1, -1.0, -1.0, null, null, -1.0), null);
		np.addMulticastTree(md, 0.5, 0.5, GraphUtils.getMinimumCostMulticastTree(null, null, null, null, n1, Sets.newHashSet(n5), -1, -1, -1.0, -1.0, null, null, -1.0), null);
		this.testTrafficForecast_hopbyhop();
	}

	
	@Test
	public void testSetMaximumLatencyE2E ()
	{
		TrafficSeries tts;
		TrafficPredictor tp;
		FITTINGTYPE fittingType = FITTINGTYPE.LINEAR;
		Date initialDate = new Date ();
		Date oneYearAfterInitialDate = new Date (initialDate.getTime() + 365L*24L*3600L*1000L); 
		Date twoYearsAfterInitialDate = new Date (initialDate.getTime() + 2L*365L*24L*3600L*1000L); 
		double intervalBetweenSamplesInSeconds = 3600 * 24;
		int numberOfSamples = 365;
		double initialTraffic = 2.0;
		double growthFactorPerYear = 0.35;
		double noiseMaxAmplitudeRespectToTraffic = 0.0;
		tts = new TrafficSeries ().addSyntheticMonitoringTrace(fittingType, 
				initialDate, intervalBetweenSamplesInSeconds, numberOfSamples, initialTraffic, growthFactorPerYear, noiseMaxAmplitudeRespectToTraffic);
		tp = tts.getFunctionPredictionSoProbSubestimationIsBounded(fittingType);
		
		System.out.println("First date: " + initialDate + ", one year after: " + oneYearAfterInitialDate);
		System.out.println("Reg results parameters estimate: " + Arrays.toString(tp.getRegResuls().getParameterEstimates()));
		System.out.println("Prediction at initial date: " + tp.getPredictorFunctionNoConfidenceInterval().apply(initialDate));
		System.out.println("Prediction one year after: " + tp.getPredictorFunctionNoConfidenceInterval().apply(oneYearAfterInitialDate));
		System.out.println("Prediction two years after: " + tp.getPredictorFunctionNoConfidenceInterval().apply(twoYearsAfterInitialDate));

		assertEquals (tp.getPredictorFunctionNoConfidenceInterval().apply(initialDate) , initialTraffic, 0.01);
		assertEquals (tp.getPredictorFunctionNoConfidenceInterval().apply(oneYearAfterInitialDate) , initialTraffic + growthFactorPerYear, 0.01);
		assertEquals (tp.getPredictorFunctionNoConfidenceInterval().apply(twoYearsAfterInitialDate) , initialTraffic + 2 * growthFactorPerYear, 0.01);
		assertEquals (tp.getAverageLinearGrowthFactorPerYearIfLinearPredictor() , growthFactorPerYear , 0.01);

		/* Exponential */
		fittingType = FITTINGTYPE.EXPONENTIAL;
		tts = new TrafficSeries ().addSyntheticMonitoringTrace(fittingType, 
				initialDate, intervalBetweenSamplesInSeconds, numberOfSamples, initialTraffic, growthFactorPerYear, noiseMaxAmplitudeRespectToTraffic);
		tp = tts.getFunctionPredictionSoProbSubestimationIsBounded(fittingType);

		System.out.println("First date: " + initialDate + ", one year after: " + oneYearAfterInitialDate);
		System.out.println("Reg results parameters estimate: " + Arrays.toString(tp.getRegResuls().getParameterEstimates()));
		System.out.println("Prediction at initial date: " + tp.getPredictorFunctionNoConfidenceInterval().apply(initialDate));
		System.out.println("Prediction one year after: " + tp.getPredictorFunctionNoConfidenceInterval().apply(oneYearAfterInitialDate));
		System.out.println("Prediction two years after: " + tp.getPredictorFunctionNoConfidenceInterval().apply(twoYearsAfterInitialDate) + ", should be: " + initialTraffic * Math.pow(1 + growthFactorPerYear , 2));
		System.out.println("CAGR estimated: " + tp.getAverageCagrFactorIfExponentialPredictor() + ", should be: " + growthFactorPerYear);

		assertEquals (tp.getPredictorFunctionNoConfidenceInterval().apply(initialDate) , initialTraffic, 0.01);
		assertEquals (tp.getPredictorFunctionNoConfidenceInterval().apply(oneYearAfterInitialDate) , initialTraffic * (1 + growthFactorPerYear), 0.01);
		assertEquals (tp.getPredictorFunctionNoConfidenceInterval().apply(twoYearsAfterInitialDate) , initialTraffic * Math.pow(1 + growthFactorPerYear , 2), 0.01);
		assertEquals (tp.getAverageCagrFactorIfExponentialPredictor() , growthFactorPerYear , 0.01);

	
		/* Exponential with noise */
		fittingType = FITTINGTYPE.EXPONENTIAL;
		noiseMaxAmplitudeRespectToTraffic = 0.05;
		tts = new TrafficSeries ().addSyntheticMonitoringTrace(fittingType, 
				initialDate, intervalBetweenSamplesInSeconds, numberOfSamples, initialTraffic, growthFactorPerYear, noiseMaxAmplitudeRespectToTraffic);
		tp = tts.getFunctionPredictionSoProbSubestimationIsBounded(fittingType);
		assertEquals (tp.getPredictorFunctionNoConfidenceInterval().apply(initialDate) , initialTraffic, 0.1);
		assertEquals (tp.getPredictorFunctionNoConfidenceInterval().apply(oneYearAfterInitialDate) , initialTraffic * (1 + growthFactorPerYear), 0.1);
		assertEquals (tp.getPredictorFunctionNoConfidenceInterval().apply(twoYearsAfterInitialDate) , initialTraffic * Math.pow(1 + growthFactorPerYear , 2), 0.1);
		assertEquals (tp.getAverageCagrFactorIfExponentialPredictor() , growthFactorPerYear , 0.1);
		System.out.println("First date: " + initialDate + ", one year after: " + oneYearAfterInitialDate);
		System.out.println("Reg results parameters estimate: " + Arrays.toString(tp.getRegResuls().getParameterEstimates()));
		System.out.println("Prediction at initial date: " + tp.getPredictorFunctionNoConfidenceInterval().apply(initialDate));
		System.out.println("Prediction one year after: " + tp.getPredictorFunctionNoConfidenceInterval().apply(oneYearAfterInitialDate));
		System.out.println("Prediction two years after: " + tp.getPredictorFunctionNoConfidenceInterval().apply(twoYearsAfterInitialDate) + ", should be: " + initialTraffic * Math.pow(1 + growthFactorPerYear , 2));
		System.out.println("CAGR estimated: " + tp.getAverageCagrFactorIfExponentialPredictor() + ", should be: " + growthFactorPerYear);

		/* Linear with noise */
		fittingType = FITTINGTYPE.LINEAR;
		noiseMaxAmplitudeRespectToTraffic = 0.05;
		tts = new TrafficSeries ().addSyntheticMonitoringTrace(fittingType, 
				initialDate, intervalBetweenSamplesInSeconds, numberOfSamples, initialTraffic, growthFactorPerYear, noiseMaxAmplitudeRespectToTraffic);
		tp = tts.getFunctionPredictionSoProbSubestimationIsBounded(fittingType);
		System.out.println("First date: " + initialDate + ", one year after: " + oneYearAfterInitialDate);
		System.out.println("Reg results parameters estimate: " + Arrays.toString(tp.getRegResuls().getParameterEstimates()));
		System.out.println("Prediction at initial date: " + tp.getPredictorFunctionNoConfidenceInterval().apply(initialDate));
		System.out.println("Prediction one year after: " + tp.getPredictorFunctionNoConfidenceInterval().apply(oneYearAfterInitialDate));
		System.out.println("Prediction two years after: " + tp.getPredictorFunctionNoConfidenceInterval().apply(twoYearsAfterInitialDate));

		assertEquals (tp.getPredictorFunctionNoConfidenceInterval().apply(initialDate) , initialTraffic, 0.1);
		assertEquals (tp.getPredictorFunctionNoConfidenceInterval().apply(oneYearAfterInitialDate) , initialTraffic + growthFactorPerYear, 0.1);
		assertEquals (tp.getPredictorFunctionNoConfidenceInterval().apply(twoYearsAfterInitialDate) , initialTraffic + 2 * growthFactorPerYear, 0.1);
		assertEquals (tp.getAverageLinearGrowthFactorPerYearIfLinearPredictor() , growthFactorPerYear , 0.1);

		assertEquals (tp.getPredictorFunctionNoConfidenceInterval().apply(twoYearsAfterInitialDate) , tp.getPredictorFunction(0.5).apply(twoYearsAfterInitialDate) , 0.1);
		assertEquals (tp.getPredictorFunctionNoConfidenceInterval().apply(twoYearsAfterInitialDate) , tp.getPredictorFunction(0.5).apply(twoYearsAfterInitialDate) , 0.1);
	}

}
