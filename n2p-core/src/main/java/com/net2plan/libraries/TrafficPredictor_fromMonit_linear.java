package com.net2plan.libraries;

import java.util.Date;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;

import org.apache.commons.math3.distribution.TDistribution;
import org.apache.commons.math3.stat.regression.SimpleRegression;

import com.net2plan.interfaces.networkDesign.Net2PlanException;

public class TrafficPredictor_fromMonit_linear extends TrafficPredictor
{
    final double intialDateConsidered;
    final double coef_a;
    final double coef_b;

    private TrafficPredictor_fromMonit_linear(double intialDateConsidered , double coef_a , double coef_b , Statistics stat)
    {
        super(TRAFFICPREDICTORTYPE.LINEARFIT , stat);
        this.coef_a = coef_a;
        this.coef_b = coef_b;
        this.intialDateConsidered = intialDateConsidered;
    }

    static Optional<TrafficPredictor_fromMonit_linear> createFromInitString (String curveInitString , String statInitString)
    {
        try
        {
            final Statistics stat = new Statistics(statInitString);
            final String [] vals = curveInitString.split(" ");
            return Optional.of(new TrafficPredictor_fromMonit_linear(Double.parseDouble(vals [0]), Double.parseDouble(vals [1]) , Double.parseDouble(vals [2]) , stat));
        } catch (Exception e) { return Optional.empty(); }
    }
    
    static Optional<TrafficPredictor_fromMonit_linear> createFromData (SortedMap<Date, Double> inputDataApplied)
    {
        final int N = inputDataApplied.size();
        if (N < 3) return Optional.empty();
        final double intialDateConsidered = inputDataApplied.firstKey().getTime();
        final SimpleRegression reg = new SimpleRegression(true);
        final double [][] data = new double [N][];
        int cont = 0;
        for (Entry<Date,Double> entry : inputDataApplied.entrySet())
        {
            final double [] vals = new double [2];
            vals [0] = entry.getKey().getTime() - intialDateConsidered;
            vals [1] = entry.getValue();
            data [cont ++] = vals;
        }
        reg.addData(data);
        final double aCoef = reg.regress().getParameterEstimate(0);
        final double bCoef = reg.regress().getParameterEstimate(1);
        final Function<Date,Double> predictionAverage = d ->aCoef + bCoef * (d.getTime() - intialDateConsidered);
        final Statistics stat = new Statistics (inputDataApplied , predictionAverage);
        final TrafficPredictor_fromMonit_linear res = new TrafficPredictor_fromMonit_linear (intialDateConsidered , aCoef , bCoef , stat);
        return Optional.of (res);
    }
    
    @Override
    public Function<Date, Double> getPredictorFunctionNoConfidenceInterval()
    {
        return d ->coef_a + coef_b * (d.getTime() - this.intialDateConsidered);
    }
    
	@Override
	public Function<Double, Optional<Date>> getInversePredictorFunctionNoConfidenceInterval() 
	{
		return traf->{ final double date = (traf - coef_a) / coef_b + this.intialDateConsidered; if (date < 0) return Optional.empty(); else return Optional.of(new Date((long) date)); };
	}
    
    @Override
    public Function<Date, Double> getPredictorFunction(double probSubestimation)
    {
        final Statistics stat = this.getStatistics();
        if (stat == null) throw new Net2PlanException ("Cannot compute the estimate");
        final double n = stat.getNumSamples();
        final double estimIntercept = coef_a;
        final double estimSlope = coef_b;
        final double tStudentNMinus2ThisConfidenceInter = new TDistribution(n - 2).inverseCumulativeProbability(1-probSubestimation);
        final double typicalDevEstimOfResidualOfTraffic = Math.sqrt(stat.getTotalSumSquaresResidual() / (n-2));
        final double sumSqDevFromAv_siftedDate = stat.getTotalSumSquaresShiftedDateDeviationRespectToShiftedDateAverage(); // getXSumSquares() in the regression original object, but not in RegressionResults
        final double averageShiftedDate = stat.getAverageShiftedDate();
        return d ->
        {
            final double shiftedDate = d.getTime() - intialDateConsidered;
            return estimIntercept + estimSlope * shiftedDate + 
                    tStudentNMinus2ThisConfidenceInter * typicalDevEstimOfResidualOfTraffic * Math.sqrt(1 + (1/n) + Math.pow(shiftedDate - averageShiftedDate , 2) / sumSqDevFromAv_siftedDate);
        };
    }

    @Override
    public String computeInitializationString()
    {
        return intialDateConsidered + " " + coef_a + " " + coef_b;
    }

    public double getAverageLinearGrowthFactor_trafficPerYear ()
    {
        return coef_b * 365.0 * 24.0 * 3600.0 * 1000.0;
    }

    @Override
    public String getDescription()
    {
        return "LINEAR: " + coef_a + " + (t - " + this.intialDateConsidered  + ")  * " + coef_b;
    }

	@Override
	public boolean isRegressionBasedFit() { return true; }

}
