package com.net2plan.libraries;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;

import org.apache.commons.math3.distribution.TDistribution;
import org.apache.commons.math3.stat.regression.RegressionResults;
import org.apache.commons.math3.stat.regression.SimpleRegression;

import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.utils.Pair;

public class TrafficSeries 
{
	public class TrafficPredictor
	{
		final private FITTINGTYPE fittingType;
		final private RegressionResults regResuls;
		final private double sumSquaresOfX; // needed for estimation confidence band
		
		public TrafficPredictor(FITTINGTYPE fittingType, RegressionResults regResuls , double sumSquaresOfX) {
			super();
			this.fittingType = fittingType;
			this.regResuls = regResuls;
			this.sumSquaresOfX = sumSquaresOfX;
		}

		public Function<Date,Double> getPredictorFunctionNoConfidenceInterval ()
		{
			final int n = (int) regResuls.getN();
			final double estimIntercept = regResuls.getParameterEstimate(0);
			final double estimSlope = regResuls.getParameterEstimate(1);
			if (fittingType.isLinear())
				return d ->estimIntercept + estimSlope * d.getTime();
			else if (fittingType.isExponential())
				return d ->Math.exp(estimIntercept + estimSlope * d.getTime());
			throw new Net2PlanException ("Unknown");
		}

		
		public Function<Date,Double> getPredictorFunction (double probSubestimation)
		{
			final int n = (int) regResuls.getN();
			final double estimIntercept = regResuls.getParameterEstimate(0);
			final double estimSlope = regResuls.getParameterEstimate(1);
			final double tStudentNMinus2ThisConfidenceInter = new TDistribution(regResuls.getN() - 2).inverseCumulativeProbability(1-probSubestimation);
			final double sy = Math.sqrt(regResuls.getTotalSumSquares() / (n-2));
			final double sx2 = sumSquaresOfX; // getXSumSquares() in the regression original object, but not in RegressionResults
			final double meanX = monitValues.values().stream().mapToDouble(e->e).sum() / monitValues.size();
			if (fittingType.isLinear())
				return d ->estimIntercept + estimSlope * d.getTime() + tStudentNMinus2ThisConfidenceInter * sy * Math.sqrt(1 + (1/n) + Math.pow(d.getTime() - meanX , 2) / ((n-1) * sx2));
			else if (fittingType.isExponential())
				return d ->Math.exp(estimIntercept + estimSlope * d.getTime() + tStudentNMinus2ThisConfidenceInter * sy * Math.sqrt(1 + (1/n) + Math.pow(d.getTime() - meanX , 2) / ((n-1) * sx2)));
			throw new Net2PlanException ("Unknown");
		}
		public FITTINGTYPE getFittingType() 
		{
			return fittingType;
		}

		public RegressionResults getRegResuls() 
		{
			return regResuls;
		}

		public double getAverageLinearGrowthFactorPerYearIfLinearPredictor ()
		{
			if (!fittingType.isLinear()) throw new Net2PlanException ("Not valid option");
			return regResuls.getParameterEstimate(1) * 365.0 * 24.0 * 3600.0 * 1000.0;
		}
		public double getAverageCagrFactorIfExponentialPredictor ()
		{
			if (!fittingType.isExponential()) throw new Net2PlanException ("Not valid option");
			return Math.exp((365.0 * 24.0 * 3600.0 * 1000.0 * regResuls.getParameterEstimate(1))) - 1;
		}
	}
	
	public enum FITTINGTYPE 
	{ 
		LINEAR("Linear progression") , EXPONENTIAL ("Exponential progression"); 
		final private String s; 
		private FITTINGTYPE (String s) { this.s = s; }
		public String getName () { return s; }
		public boolean isLinear () { return this == LINEAR; }
		public boolean isExponential () { return this == EXPONENTIAL; }
	};
	final SortedMap<Date , Double> monitValues = new TreeMap<> ();
	public TrafficSeries () { }
	public TrafficSeries (SortedMap<Date , Double> monitValues) { this.monitValues.putAll (monitValues);  }
	
	public TrafficSeries addValues (Collection<Date> dates , Collection<Double> values) 
	{ 
		if (dates.size() != values.size()) throw new Net2PlanException ("Wrong format");
		final Iterator<Date> itdate = dates.iterator();
		final Iterator<Double> itvals = values.iterator();
		while (itdate.hasNext())
		{
			final Date d = itdate.next();
			final Double val = itvals.next();
			this.monitValues.put(d, val);
		}
		return this;
	}
	public TrafficSeries addValues (Collection<Pair<Date,Double>> pairs) 
	{ 
		pairs.forEach(p->monitValues.put(p.getFirst(), p.getSecond()));
		return this;
	}
	public TrafficSeries addValues (SortedMap<Date,Double> values) 
	{ 
		this.monitValues.putAll(values);
		return this;
	}

	public SortedSet<Date> getDatesWithValue () { return new TreeSet<>(monitValues.keySet()); }
	public SortedMap<Date , Double> getValues () { return Collections.unmodifiableSortedMap(monitValues); }
	public int getSize () { return monitValues.size(); }
	public Double getValueOrNull (Date d) { return monitValues.get(d); }
	public boolean hasValue (Date d) { return monitValues.containsKey(d); }
	public Date getFirstDate () { return monitValues.isEmpty()? null : monitValues.firstKey(); }
	public Date getLastDate () { return monitValues.isEmpty()? null : monitValues.lastKey(); }
	public TrafficSeries addValue (Date date , Double val) { if (val == null) throw new Net2PlanException ("Null values are not accepted");  this.monitValues.put(date, val); return this; }
	public TrafficSeries removeValue (Date date) { this.monitValues.remove(date); return this; }
	public TrafficSeries removeAllValues () { this.monitValues.clear(); return this; }
	public TrafficSeries removeAllValuesBeforeOrEqual (Date d) 
	{ 
		this.monitValues.remove(d);
		for (Date beforeDate : new ArrayList<> (monitValues.headMap(d).keySet()))
			monitValues.remove(beforeDate);
		return this;
	}
	public TrafficSeries removeAllValuesAfterOrEqual (Date d) 
	{ 
		for (Date beforeDate : new ArrayList<> (monitValues.tailMap(d).keySet()))
			monitValues.remove(beforeDate);
		return this;
	}
	public TrafficSeries getCopy () { return new TrafficSeries (this.monitValues); }
	public Double getValueOrInterpolation (Date d) 
	{
		if (this.getSize() < 2) return null;
		final Double valExact = this.getValueOrNull(d); if (valExact != null) return valExact;
		Date firstDate = null; double firstVal = 0;
		Date secondDate = null; double secondVal = 0;
		if (d.getTime() < getFirstDate().getTime())
		{
			for (Entry<Date,Double> entry : this.monitValues.entrySet())
			{
				if (firstDate == null) { firstDate = entry.getKey(); firstVal = entry.getValue(); }
				else if (secondDate == null) {  secondDate = entry.getKey(); secondVal = entry.getValue(); break; }
			}
		}
		else if (d.getTime() > getLastDate().getTime())
		{
			secondDate = this.monitValues.lastKey(); secondVal = this.getValueOrNull(secondDate);
			firstDate = this.monitValues.headMap(d).lastKey(); firstVal = this.getValueOrNull(firstDate);
		}
		else
		{
			firstDate = this.monitValues.headMap(d).lastKey(); firstVal = this.getValueOrNull(firstDate);
			secondDate = this.monitValues.tailMap(d).lastKey(); firstVal = this.getValueOrNull(firstDate);
		}
		assert !firstDate.equals(secondDate); assert firstDate.getTime() < secondDate.getTime();
		final double deltaY = secondVal - firstVal;
		final double deltaX = secondDate.getTime() - firstDate.getTime();
		final double slope = deltaY / deltaX;
		return firstVal + slope * (d.getTime() - firstDate.getTime());
	}

	public TrafficPredictor getFunctionPredictionSoProbSubestimationIsBounded (FITTINGTYPE fittingType)
	{
		if (getSize() < 3) throw new Net2PlanException ("Not enough significance for the test");
		if (!fittingType.isLinear() && !fittingType.isExponential()) throw new Net2PlanException ("Unknown fitting type");
		final SimpleRegression reg = new SimpleRegression(true);
		final double [][] data = new double [this.getSize()][];
		int cont = 0;
		for (Entry<Date,Double> entry : this.monitValues.entrySet())
		{
			final double [] vals = new double [2];
			vals [0] = (double) entry.getKey().getTime();
			if (fittingType.isExponential() && entry.getValue() <= 0) continue; // 0 samples are removed in exponential fitting
			vals [1] = fittingType.isLinear()? entry.getValue() : Math.log(entry.getValue());
			data [cont ++] = vals;
		}
		reg.addData(data);
		return new TrafficPredictor(fittingType, reg.regress(), reg.getXSumSquares());
//		
//		final int n = (int) reg.getN();
//		final double confIntervalModifiedToSingleTail = 1 - (2 * probSubestimation); 
//		final double estimIntercept = reg.getIntercept();
//		final double estimSlope = reg.getSlope();
//		final double tStudentNMinus2ThisConfidenceInter = new TDistribution(reg.getN() - 2).inverseCumulativeProbability(confIntervalModifiedToSingleTail);
//		final double sy = Math.sqrt(reg.getTotalSumSquares() / (n-2));
//		final double sx2 = reg.getXSumSquares();
//		final double meanX = monitValues.values().stream().mapToDouble(e->e).sum() / monitValues.size();
//		final Function<Date,Double> predFunction;
//		if (isLinearFitting)
//			predFunction = d ->estimIntercept + estimSlope * d.getTime() + tStudentNMinus2ThisConfidenceInter * sy * Math.sqrt(1 + (1/n) + Math.pow(d.getTime() - meanX , 2) / ((n-1) * sx2));
//		else
//			predFunction = d ->Math.exp(estimIntercept + estimSlope * d.getTime() + tStudentNMinus2ThisConfidenceInter * sy * Math.sqrt(1 + (1/n) + Math.pow(d.getTime() - meanX , 2) / ((n-1) * sx2)));
//		return new TrafficPredictor(fittingType, regResuls)	
	}
	
	public Double getPredictionSoProbSubestimationIsBounded (FITTINGTYPE fittingType , Date d , double probSubestimation)
	{
		return getFunctionPredictionSoProbSubestimationIsBounded(fittingType).getPredictorFunction(probSubestimation).apply(d);
	}
	
	public static void main (String [] args)
	{
		final double [] x = new double [] {1.47 ,1.50,	1.52,1.55,1.57,1.60,1.63,1.65,1.68,1.70,1.73,1.75,1.78,1.80,1.83};
		final double [] y = new double [] {52.21,53.12,54.48,55.84,57.20,58.57,59.93,61.29,63.11,64.47,66.28,68.10,69.92,72.19,74.46};
		final SimpleRegression reg = new SimpleRegression(true);
		for (int cont = 0; cont < x.length ; cont ++)
			reg.addData(x[cont], y[cont]);
		final double estimIntercept = reg.getIntercept();
		final double estimSlope = reg.getSlope();
		final double tStudentNMinus2_0975 = new TDistribution(reg.getN() - 2).inverseCumulativeProbability(0.975);
		final double tStudentNMinus2_095 = new TDistribution(reg.getN() - 2).inverseCumulativeProbability(0.95);
		final double tStudentNMinus2_090 = new TDistribution(reg.getN() - 2).inverseCumulativeProbability(0.90);
		System.out.println("Sum square errors: " + reg.getSumSquaredErrors());
		System.out.println("estimIntercept: " + estimIntercept + ", estimSlopr: " + estimSlope);
		System.out.println("tStudentNMinus2_0975: " + tStudentNMinus2_0975 + ", tStudentNMinus2_095: " + tStudentNMinus2_095 + ", tStudentNMinus2_090: " + tStudentNMinus2_090);
		
	}
	
	
	public List<String> toStringList () 
	{
		final List<String> res = new ArrayList<> (monitValues.size());
		for (Entry<Date , Double> entry : monitValues.entrySet())
			res.add(entry.getKey().getTime() + " " + entry.getValue());
		return res;
	}
	public static TrafficSeries createFromStringList (List<String> stringList) 
	{
		final TrafficSeries res = new TrafficSeries ();
		for (String st : stringList)
		{
			try 
			{ 			
				final String [] vals = st.split(" ");
				res.addValue(new Date (Long.parseLong(vals [0])), Double.parseDouble(vals [1]));
			} catch (Exception e) { }
		}
		return res;
	}
	public static TrafficSeries createFromValuesAndInterval (Date initialTime , long intervalInMiliseconds , double [] values) 
	{
		final TrafficSeries res = new TrafficSeries ();
		int cont = 0;
		for (double val : values)
		{
			final Date d = new Date (initialTime.getTime() + (intervalInMiliseconds * (cont++) ));
			res.addValue(d, val);
		}
		return res;
	}

	
	
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((monitValues == null) ? 0 : monitValues.hashCode());
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
		TrafficSeries other = (TrafficSeries) obj;
		if (monitValues == null) {
			if (other.monitValues != null)
				return false;
		} else if (!monitValues.equals(other.monitValues))
			return false;
		return true;
	}

	public TrafficSeries addSyntheticMonitoringTrace (FITTINGTYPE type , 
			Date initialDate , 
			double intervalBetweenSamplesInSeconds , 
			int numberOfSamples ,
			double initialTraffic , 
			double growthFactorPerYear , 
			double noiseRelativeTypicalDeviationRespectToAverage)
	{
		assert type.isLinear() || type.isExponential();
		final Random rng = new Random (1);
		
		/* Add values */
		for (int cont = 0 ; cont < numberOfSamples ; cont ++)
		{
			final Date currentDate = new Date (initialDate.getTime() + (long) (1000 * cont * intervalBetweenSamplesInSeconds));
			final double yearsSinceInitialDate =  cont * intervalBetweenSamplesInSeconds / (365.0*24.0*3600.0);
			{
    			final double currentTrafficNoNoise = type.isLinear()? initialTraffic + growthFactorPerYear * yearsSinceInitialDate : initialTraffic * Math.pow(1+growthFactorPerYear , yearsSinceInitialDate);
    			final double noise = rng.nextGaussian() * noiseRelativeTypicalDeviationRespectToAverage * currentTrafficNoNoise;
    			addValue(currentDate, Math.max(0, currentTrafficNoNoise + noise));
			}
		}
		return this;
	}
	
	
}


