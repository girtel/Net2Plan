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

public class TrafficPredictor_fromMonit_exponential extends TrafficPredictor
{
    final double intialDateConsidered;
    final double coefLinearApproxToLog_a;
    final double coefLinearApproxToLog_b;
    final double totalSumSquaresResidualsInLog;
    
    private TrafficPredictor_fromMonit_exponential(double intialDateConsidered , double coefLinearApproxToLog_a , double coefLinearApproxToLog_b , double totalSumSquaresResidualsInLog , Statistics stat)
    {
        super(TRAFFICPREDICTORTYPE.EXPONENTIALFIT , stat);
        this.coefLinearApproxToLog_a = coefLinearApproxToLog_a;
        this.coefLinearApproxToLog_b = coefLinearApproxToLog_b;
        this.intialDateConsidered = intialDateConsidered;
        this.totalSumSquaresResidualsInLog = totalSumSquaresResidualsInLog;
    }

    static Optional<TrafficPredictor_fromMonit_exponential> createFromInitString (String curveInitString , String statInitString)
    {
        try
        {
            final Statistics stat = new Statistics(statInitString);
            final String [] vals = curveInitString.split(" ");
            return Optional.of(new TrafficPredictor_fromMonit_exponential(Double.parseDouble(vals [0]), Double.parseDouble(vals [1]), Double.parseDouble(vals [2]) , Double.parseDouble(vals [3]) , stat));
        } catch (Exception e) { return Optional.empty(); }
    }
    
    static Optional<TrafficPredictor_fromMonit_exponential> createFromData (SortedMap<Date, Double> inputDataApplied)
    {
    	final SortedMap<Date, Double> inputDataAppliedFiltered = new TreeMap<>();
    	inputDataApplied.entrySet().stream().filter(e->e.getValue() > 0).forEach(e->inputDataAppliedFiltered.put(e.getKey(), e.getValue()));
        final int N = inputDataAppliedFiltered.size();
        if (N < 3) return Optional.empty();
        final double intialDateConsidered = inputDataAppliedFiltered.firstKey().getTime();
        final SimpleRegression reg = new SimpleRegression(true);
        final double [][] data = new double [N][];
        int cont = 0;
        for (Entry<Date,Double> entry : inputDataAppliedFiltered.entrySet())
        {
            final double [] vals = new double [2];
            vals [0] = entry.getKey().getTime() - intialDateConsidered;
            if (entry.getValue() <= 0) continue; // 0 samples are removed in exponential fitting
            vals [1] = Math.log(entry.getValue());
            data [cont ++] = vals;
        }
        reg.addData(data);
        final double aCoefOfLog = reg.regress().getParameterEstimate(0);
        final double bCoefOfLog = reg.regress().getParameterEstimate(1);
        final double sumOfResidualsSquaredOfLinear = reg.regress().getErrorSumSquares();
        final Function<Date,Double> predictionAverage = d ->Math.exp(aCoefOfLog + bCoefOfLog * (d.getTime() - intialDateConsidered));
        final Statistics stat = new Statistics (inputDataApplied , predictionAverage);
        final TrafficPredictor_fromMonit_exponential res = new TrafficPredictor_fromMonit_exponential (intialDateConsidered , aCoefOfLog , bCoefOfLog , sumOfResidualsSquaredOfLinear , stat);
        return Optional.of (res);
    }
    
    @Override
    public Function<Date, Double> getPredictorFunctionNoConfidenceInterval()
    {
        return d ->Math.exp(coefLinearApproxToLog_a + coefLinearApproxToLog_b * (d.getTime() - this.intialDateConsidered));
    }
    
	@Override
	public Function<Double, Optional<Date>> getInversePredictorFunctionNoConfidenceInterval() 
	{
		return traf->{ if (traf <= 0) return Optional.empty(); final double date = (Math.log(traf) - coefLinearApproxToLog_a) / coefLinearApproxToLog_b + this.intialDateConsidered; if (date < 0) return Optional.empty(); else return Optional.of(new Date((long) date)); };
	}
    
    @Override
    public Function<Date, Double> getPredictorFunction(double probSubestimation)
    {
        final Statistics stat = this.getStatistics();
        if (stat == null) throw new Net2PlanException ("Cannot compute the estimate");
        final double n = stat.getNumSamples();
        final double estimIntercept = coefLinearApproxToLog_a;
        final double estimSlope = coefLinearApproxToLog_b;
        final double tStudentNMinus2ThisConfidenceInter = new TDistribution(n - 2).inverseCumulativeProbability(1-probSubestimation);
//        final double typicalDevEstimOfResidualOfTraffic = Math.sqrt(stat.getTotalSumSquaresResidual() / (n-2)); // this is summing the exponentials, we need the residuals of the logs
        final double typicalDevEstimOfResidualOfTraffic = Math.sqrt(this.totalSumSquaresResidualsInLog / (n-2)); // this is summing the exponentials, we need the residuals of the logs
        final double sumSqDevFromAv_shifteddate = stat.getTotalSumSquaresShiftedDateDeviationRespectToShiftedDateAverage(); // getXSumSquares() in the regression original object, but not in RegressionResults
        final double averageShiftedDate = stat.getAverageShiftedDate();
        return d -> 
        { 
            final double shiftedDate = d.getTime() - intialDateConsidered;
            return Math.exp(estimIntercept + estimSlope * shiftedDate + 
                 tStudentNMinus2ThisConfidenceInter * typicalDevEstimOfResidualOfTraffic * Math.sqrt(1 + (1/n) + Math.pow(shiftedDate - averageShiftedDate , 2) / sumSqDevFromAv_shifteddate)); 
        };
    }

    @Override
    public String computeInitializationString()
    {
        return this.intialDateConsidered + " " + coefLinearApproxToLog_a + " " + coefLinearApproxToLog_b + " " + totalSumSquaresResidualsInLog;
    }

    public double getAverageCagrFactor ()
    {
        return Math.exp((365.0 * 24.0 * 3600.0 * 1000.0 * coefLinearApproxToLog_b)) - 1;
    }

    @Override
    public String getDescription()
    {
        return "EXP: " + coefLinearApproxToLog_a + " + * EXP(" + coefLinearApproxToLog_b+ " * (t - " + this.intialDateConsidered + ")";
    }

	@Override
	public boolean isRegressionBasedFit() { return true; }

}
