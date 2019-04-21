package com.net2plan.libraries;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
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
import java.util.stream.Collectors;

import org.apache.commons.math3.distribution.TDistribution;
import org.apache.commons.math3.stat.regression.SimpleRegression;

import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.utils.Pair;

public class TrafficSeries 
{
	public enum FITTINGTYPE 
	{ 
		LINEAR("Linear progression") , EXPONENTIAL ("Exponential progression"); 
		final private String s; 
		private FITTINGTYPE (String s) { this.s = s; }
		public String getName () { return s; }
		public boolean isLinear () { return this == LINEAR; }
		public boolean isExponential () { return this == EXPONENTIAL; }
	}
	private SortedMap<Date , Double> monitValues = new TreeMap<> ();
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

	public void applyPercentileFiltering (Date initDate , Date endDate , String intervalTimeType , double percentile)
	{
		if (percentile <= 0 || percentile > 1) throw new Net2PlanException("Percentil info must be between 0 (non-inclusive) and one (inclusive)");
		final SortedMap<Date , Double> newMonitValues = new TreeMap<> ();
		LocalDateTime currentDateLt = null;
		final LocalDateTime initDateLt = dateToLocalDateTime(initDate);
		final LocalDateTime endDateLt = dateToLocalDateTime(endDate);
		final TemporalUnit unitToAdd;
		if (intervalTimeType.equalsIgnoreCase("hour"))
		{
			unitToAdd = ChronoUnit.HOURS;
			currentDateLt = initDateLt.with(LocalDateTime.of(initDateLt.getYear(), initDateLt.getMonthValue() , initDateLt.getDayOfMonth() , initDateLt.getHour(), 0));
		} else if (intervalTimeType.equalsIgnoreCase("day"))
		{
			unitToAdd = ChronoUnit.DAYS;
			currentDateLt = initDateLt.with(LocalDateTime.of(initDateLt.getYear(), initDateLt.getMonthValue() , initDateLt.getDayOfMonth() , 0, 0));
		} else if (intervalTimeType.equalsIgnoreCase("month"))
		{
			unitToAdd = ChronoUnit.MONTHS;
			currentDateLt = initDateLt.with(LocalDateTime.of(initDateLt.getYear(), initDateLt.getMonthValue() , 1 , 0, 0));
		} else if (intervalTimeType.equalsIgnoreCase("year"))
		{
			unitToAdd = ChronoUnit.YEARS;
			currentDateLt = initDateLt.with(LocalDateTime.of(initDateLt.getYear(), 1 , 1 , 0, 0));
		}
		else throw new Net2PlanException ("Wrong percentile interval type: " + intervalTimeType);
		while (!currentDateLt.isAfter(endDateLt))
		{
			final LocalDateTime endTimeThisIntervalLt = currentDateLt.plus(1, unitToAdd);
			final Instant endTimeThisInterval = localDateTimeToDate(endTimeThisIntervalLt).toInstant();
			final Date currentDate = localDateTimeToDate(currentDateLt);
			final List<Entry<Date,Double>> vals = monitValues.tailMap(currentDate).entrySet ().stream().
					filter(e->e.getKey().toInstant().isBefore(endTimeThisInterval)).
					sorted((e1,e2)->Double.compare(e1.getValue (), e2.getValue())).
					collect (Collectors.toCollection(ArrayList::new));
			currentDateLt = endTimeThisIntervalLt;
			
			if (vals.isEmpty()) continue;
			final double positionOfSample = vals.size() * percentile;
			final double remainder = positionOfSample - Math.floor(positionOfSample);
			final int prevIndex = (int) Math.max(0, Math.floor(positionOfSample) - 1);
			final int nextIndex = (int) Math.min(Math.ceil(positionOfSample) - 1 , vals.size() - 1);
			if (prevIndex == nextIndex) 
			{
				newMonitValues.put(vals.get(prevIndex).getKey(), vals.get(prevIndex).getValue());
			} else
			{
				final Date d1 = vals.get(prevIndex).getKey();
				final Date d2 = vals.get(nextIndex).getKey();
				final double v1 = vals.get(prevIndex).getValue();
				final double v2 = vals.get(nextIndex).getValue();
				final Date midDate = new Date ((long) ((1-remainder)*d1.getTime() + remainder * d2.getTime()));
				final double midVal = (1-remainder) * v1 + remainder * v2;
				newMonitValues.put(midDate, midVal);
			}
		}
		this.monitValues = newMonitValues;
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
			double initialTrafficOfPeak , 
			double growthFactorPerYear , 
			double dayVariation_peakFactor , 
            double dayVariation_startHour ,
            double dayVariation_durationHours ,
			double noiseRelativeTypicalDeviationRespectToAverage , 
			Random rng)
	{
		assert type.isLinear() || type.isExponential();
		final boolean applyDayShaping = dayVariation_peakFactor >= 1 && (dayVariation_startHour >= 0) && (dayVariation_startHour < 24) && (dayVariation_durationHours > 0) && (dayVariation_startHour + dayVariation_durationHours <=24);
		/* Add values */
		for (int cont = 0 ; cont < numberOfSamples ; cont ++)
		{
			final Date currentDate = new Date (initialDate.getTime() + (long) (1000 * cont * intervalBetweenSamplesInSeconds));
			final double yearsSinceInitialDate =  cont * intervalBetweenSamplesInSeconds / (365.0*24.0*3600.0);
			{
    			final double currentTrafficNoNoise = type.isLinear()? initialTrafficOfPeak + growthFactorPerYear * yearsSinceInitialDate : initialTrafficOfPeak * Math.pow(1+growthFactorPerYear , yearsSinceInitialDate);
    			final double noise = rng.nextGaussian() * noiseRelativeTypicalDeviationRespectToAverage * currentTrafficNoNoise;
    			double traffic = Math.max(0, currentTrafficNoNoise + noise);
    			if (applyDayShaping)
    			{
    				final LocalDateTime currentDateLt = dateToLocalDateTime(currentDate);
    				final LocalDateTime startOfDayLt = currentDateLt.with(LocalDateTime.of(currentDateLt.getYear(), currentDateLt.getMonthValue() , currentDateLt.getDayOfMonth() , 0, 0));
    				final long millisSinceStartDay = startOfDayLt.until(currentDateLt, ChronoUnit.MILLIS);
    				final long millisInADay = 24L*3600L*1000L;
    				if (millisSinceStartDay < 0 || millisSinceStartDay > millisInADay) 
    					throw new RuntimeException ();
    				final double fractionInsideDay = ((double) millisSinceStartDay) / ((double) millisInADay);
    				final boolean inPeak = (fractionInsideDay >= dayVariation_startHour / 24.0) && (fractionInsideDay <= (dayVariation_startHour + dayVariation_durationHours) / 24.0);
    				if (!inPeak) traffic /= dayVariation_peakFactor;
    			}
    			addValue(currentDate, traffic);
			}
		}
		return this;
	}
	

	private static LocalDateTime dateToLocalDateTime(Date date) { return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()); 	}

	private static Date localDateTimeToDate(LocalDateTime localDateTime) { return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant()); 	}
}


