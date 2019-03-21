package com.net2plan.libraries;

import java.util.Date;
import java.util.Optional;
import java.util.function.Function;

import com.net2plan.interfaces.networkDesign.Net2PlanException;

public class TrafficPredictor_manual_exponential extends TrafficPredictor
{
    final double intialDateConsidered;
    final double coefLinearApproxToLog_a;
    final double coefLinearApproxToLog_b;

    private TrafficPredictor_manual_exponential(double intialDateConsidered , double coefLinearApproxToLog_a , double coefLinearApproxToLog_b , Statistics stat)
    {
        super(TRAFFICPREDICTORTYPE.EXPONENTIALFIT , stat);
        this.coefLinearApproxToLog_a = coefLinearApproxToLog_a;
        this.coefLinearApproxToLog_b = coefLinearApproxToLog_b;
        this.intialDateConsidered = intialDateConsidered;
    }

    static Optional<TrafficPredictor_manual_exponential> createFromInitString (String curveInitString)
    {
        try
        {
            final String [] vals = curveInitString.split(" ");
            return Optional.of(new TrafficPredictor_manual_exponential(Double.parseDouble(vals [0]), Double.parseDouble(vals [1]), Double.parseDouble(vals [2]) , null));
        } catch (Exception e) { return Optional.empty(); }
    }
    
    public static Optional<TrafficPredictor_manual_exponential> createFromData (double intialDateConsidered , double trafficAtInitialDateGbps , double cagr)
    {
    	if (trafficAtInitialDateGbps <= 0) return Optional.empty();
    	final double coef_a = Math.log(trafficAtInitialDateGbps);
    	final double coef_b = Math.log(cagr + 1) / (365.0 * 24.0 * 3600.0 * 1000.0);
    	return Optional.of(new TrafficPredictor_manual_exponential (intialDateConsidered , coef_a , coef_b , null));
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
    	throw new Net2PlanException ("Not possible to obtain this information");
    }

    @Override
    public String computeInitializationString()
    {
        return this.intialDateConsidered + " " + coefLinearApproxToLog_a + " " + coefLinearApproxToLog_b;
    }

    public double getAverageCagrFactor ()
    {
        return Math.exp((365.0 * 24.0 * 3600.0 * 1000.0 * coefLinearApproxToLog_b)) - 1;
    }

    @Override
    public String getDescription()
    {
        return "EXP-MANUAL: " + coefLinearApproxToLog_a + " + * EXP(" + coefLinearApproxToLog_b+ " * (t - " + this.intialDateConsidered + ")";
    }

	@Override
	public boolean isRegressionBasedFit() { return false; }

}
