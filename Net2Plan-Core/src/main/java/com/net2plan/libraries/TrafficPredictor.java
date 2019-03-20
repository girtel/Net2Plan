package com.net2plan.libraries;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.SortedMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.net2plan.interfaces.networkDesign.Net2PlanException;

public abstract class TrafficPredictor 
{
	public static enum TRAFFICPREDICTORTYPE
	{
		LINEARFIT("Linear fit") , EXPONENTIALFIT ("Exponential fit") , EXPONENTIALMANUAL("Manual (EXP)") , LINEARMANUAL ("Manual (LINEAR)"); 
		final private String s; 
		private TRAFFICPREDICTORTYPE (String s) { this.s = s; }
		public String getName () { return s; }
		public boolean isLinear () { return this == LINEARFIT; }
		public boolean isExponential () { return this == EXPONENTIALFIT; }
		public boolean isManual () { return this == EXPONENTIALMANUAL || this == LINEARMANUAL; }
	}	
	
	
	private final TRAFFICPREDICTORTYPE tpType;
	private final Statistics computedPredictionStatistics;

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((computedPredictionStatistics == null) ? 0 : computedPredictionStatistics.hashCode());
		result = prime * result + ((tpType == null) ? 0 : tpType.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TrafficPredictor other = (TrafficPredictor) obj;
		if (computedPredictionStatistics == null) {
			if (other.computedPredictionStatistics != null)
				return false;
		} else if (!computedPredictionStatistics.equals(other.computedPredictionStatistics))
			return false;
		if (tpType != other.tpType)
			return false;
		return true;
	}
	protected TrafficPredictor (TRAFFICPREDICTORTYPE tpType , Statistics stat)
	{
		this.tpType = tpType;
		this.computedPredictionStatistics = stat;
	}
	public static Optional<TrafficPredictor> createFromInitStrings (TRAFFICPREDICTORTYPE trafPredType , String curveInitializationString , Optional<String> statisticsInitString)
	{
	    switch (trafPredType)
	    {
        case EXPONENTIALFIT:
            return (Optional<TrafficPredictor>) (Optional<?>) TrafficPredictor_fromMonit_exponential.createFromInitString(curveInitializationString , statisticsInitString.get());
        case LINEARFIT:
            return (Optional<TrafficPredictor>) (Optional<?>) TrafficPredictor_fromMonit_linear.createFromInitString(curveInitializationString , statisticsInitString.get());
		case EXPONENTIALMANUAL:
            return (Optional<TrafficPredictor>) (Optional<?>) TrafficPredictor_manual_exponential.createFromInitString(curveInitializationString);
		case LINEARMANUAL:
            return (Optional<TrafficPredictor>) (Optional<?>) TrafficPredictor_manual_linear.createFromInitString(curveInitializationString);
        default:
            return Optional.empty();
	    }
	}
    public static Optional<TrafficPredictor> createFromMonitData (TRAFFICPREDICTORTYPE trafPredType , SortedMap<Date, Double> inputDataApplied)
    {
        switch (trafPredType)
        {
        case EXPONENTIALFIT:
            return (Optional<TrafficPredictor>) (Optional<?>) TrafficPredictor_fromMonit_exponential.createFromData(inputDataApplied);
        case LINEARFIT:
            return (Optional<TrafficPredictor>) (Optional<?>) TrafficPredictor_fromMonit_linear.createFromData(inputDataApplied);
        default:
        	throw new Net2PlanException ("Wrong traffic predictor. Only monitoring-based predictors should use this method");
        }
    }

    public abstract Function<Date,Double> getPredictorFunctionNoConfidenceInterval ();
    public abstract Function<Double,Optional<Date>> getInversePredictorFunctionNoConfidenceInterval ();
	public abstract Function<Date,Double> getPredictorFunction (double probSubestimation);
	public abstract boolean isRegressionBasedFit ();
	public abstract String computeInitializationString ();

	public final TRAFFICPREDICTORTYPE getTpType () { return this.tpType; }
	public final Statistics getStatistics () { return this.computedPredictionStatistics; }
	
	public static class Statistics
	{
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Statistics other = (Statistics) obj;
			if (Math.abs(N - other.N) > 1e-3) return false;
			if (endDate == null) {
				if (other.endDate != null)
					return false;
			} else if (!endDate.equals(other.endDate))
				return false;
			if (initialDate == null) {
				if (other.initialDate != null)
					return false;
			} else if (!initialDate.equals(other.initialDate))
				return false;
			if (Math.abs(sum_resid - other.sum_resid) > 1e-3) return false;
			if (Math.abs(sum_resid2 - other.sum_resid2) > 1e-3) return false;
			if (Math.abs(sum_x - other.sum_x) > 1e-3) return false;
			if (Math.abs(sum_x2 - other.sum_x2) > 1e-3) return false;
			if (Math.abs(sum_y - other.sum_y) > 1e-3) return false;
			if (Math.abs(sum_y2 - other.sum_y2) > 1e-3) return false;
			return true;
		}

		@Override
		public int hashCode() {  return super.hashCode(); }

		
		
		final double N;
        final Date initialDate , endDate;
		double sum_x = 0;
		double sum_x2 = 0;
		double sum_y = 0;
		double sum_y2 = 0;
		double sum_resid = 0;
		double sum_resid2 = 0; // SSE
		protected Statistics (String initializationString)
		{
		    final String [] vals = initializationString.split(" ");
		    assert vals.length == 9;
		    this.N = Double.parseDouble(vals [0]);
            this.initialDate = new Date (Long.parseLong(vals [1]));
            this.endDate = new Date (Long.parseLong(vals [2]));
            this.sum_x = Double.parseDouble(vals [3]);
            this.sum_x2 = Double.parseDouble(vals [4]);
            this.sum_y = Double.parseDouble(vals [5]);
            this.sum_y2 = Double.parseDouble(vals [6]);
            this.sum_resid = Double.parseDouble(vals [7]);
            this.sum_resid2 = Double.parseDouble(vals [8]);
		}
		public String getInitializationString ()
		{
            final List<Number> vals = Arrays.asList(N , initialDate.getTime() , endDate.getTime() , sum_x , sum_x2 , sum_y , sum_y2 , sum_resid , sum_resid2);
            return vals.stream().map(v->v.toString()).collect(Collectors.joining(" "));
		}
		
		protected Statistics (SortedMap<Date,Double> inputDataApplied , Function<Date,Double> pred)
		{
			this.N = inputDataApplied.size();
            this.initialDate = inputDataApplied.firstKey();
            final double initialDateDouble = initialDate.getTime();
            this.endDate = inputDataApplied.lastKey();
			for (Entry<Date,Double> sample : inputDataApplied.entrySet())
			{
				final Date xDate = sample.getKey();
				final double x = xDate.getTime() - initialDateDouble;
				final double y = sample.getValue();
				final double yPred = pred.apply(xDate);
				final double residual = y - yPred;
				sum_x += x;
				sum_x2 += Math.pow(x, 2);
				sum_y += y;
				sum_y2 += Math.pow(y, 2);
				sum_resid += residual;
				sum_resid2 += Math.pow(residual, 2);
			}
		}
		public int getNumSamples () { return (int) N; }
		public double getAverageTraffic () { return sum_y / N; }
        public double getAverageShiftedDate () { return sum_x / N; }
		public double getAverageResidual () { return sum_resid / N; }
		public double getBiasedVarianceInputTraffic ()
		{
			return (sum_y2/N) - Math.pow(getAverageTraffic() , 2);
		}
		public double getUnbiasedVarianceInputTraffic ()
		{
			return   (N/(N-1)) * getBiasedVarianceInputTraffic ();
		}
		public double getBiasedVarianceResiduals ()
		{
			return (sum_resid2/getNumSamples()) - Math.pow(getAverageResidual() , 2);
		}
		public double getUnbiasedVarianceResiduals ()
		{
			return   (N/(N-1)) * getBiasedVarianceResiduals ();
		}
		public double getUnbiasedTypicalDeviationResiduals ()
		{
			return   Math.sqrt(getUnbiasedVarianceResiduals ());
		}
		public double getTotalSumSquaresTrafficDeviationRespectToAverage () { return this.sum_y2 - Math.pow(this.sum_y , 2) / N; }

		public double getTotalSumSquaresResidual () { return this.sum_resid2;  }

		public double getTotalSumSquaresShiftedDateDeviationRespectToShiftedDateAverage () { return this.sum_x2 - Math.pow(this.sum_x , 2) / N; }

		public double getRsquared () { return sum_y2 <= 0.0 ? 1.0 : 1 - sum_resid2 / sum_y2; } // can be read (controversy) as fraction of variance explained by the predictor
	}
	
	public abstract String getDescription ();

    public final TrafficPredictor_fromMonit_exponential getAsExponential () { return (TrafficPredictor_fromMonit_exponential) this; }

    public final TrafficPredictor_fromMonit_linear getAsLinear () { return (TrafficPredictor_fromMonit_linear) this; }
}    

