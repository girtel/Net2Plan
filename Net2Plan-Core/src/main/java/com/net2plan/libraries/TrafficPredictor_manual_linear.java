package com.net2plan.libraries;

import java.util.Date;
import java.util.Optional;
import java.util.function.Function;

import com.net2plan.interfaces.networkDesign.Net2PlanException;

public class TrafficPredictor_manual_linear extends TrafficPredictor
{
    final double intialDateConsidered;
    final double coef_a;
    final double coef_b;

    private TrafficPredictor_manual_linear(double intialDateConsidered , double coef_a , double coef_b , Statistics stat)
    {
        super(TRAFFICPREDICTORTYPE.LINEARMANUAL , stat);
        this.coef_a = coef_a;
        this.coef_b = coef_b;
        this.intialDateConsidered = intialDateConsidered;
    }

    static Optional<TrafficPredictor_manual_linear> createFromInitString (String curveInitString)
    {
        try
        {
            final String [] vals = curveInitString.split(" ");
            return Optional.of(new TrafficPredictor_manual_linear(Double.parseDouble(vals [0]), Double.parseDouble(vals [1]) , Double.parseDouble(vals [2]) , null));
        } catch (Exception e) { return Optional.empty(); }
    }
    
    public static Optional<TrafficPredictor_manual_linear> createFromData (double intialDateConsidered , double trafficAtInitialDateGbps , double growthFactor_GbpsPerYear)
    {
    	final double coef_a = trafficAtInitialDateGbps;
    	final double coef_b = growthFactor_GbpsPerYear / (365.0 * 24.0 * 3600.0 * 1000.0);
    	return Optional.of(new TrafficPredictor_manual_linear (intialDateConsidered , coef_a , coef_b , null));
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
    	throw new Net2PlanException ("Not possible to obtain this information");
    }

    @Override
    public String computeInitializationString()
    {
        return intialDateConsidered + " " + coef_a + " " + coef_b;
    }

    public double getAverageLinearGrowthFactor_GbpsPerYear ()
    {
        return coef_b * 365.0 * 24.0 * 3600.0 * 1000.0;
    }

    @Override
    public String getDescription()
    {
        return "LINEAR-MANUAL: " + coef_a + " + (t - " + this.intialDateConsidered  + ")  * " + coef_b;
    }

	@Override
	public boolean isRegressionBasedFit() { return false; }

}
