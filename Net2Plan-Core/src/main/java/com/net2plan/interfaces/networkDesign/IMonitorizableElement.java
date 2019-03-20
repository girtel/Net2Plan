package com.net2plan.interfaces.networkDesign;

import java.util.Optional;

import com.net2plan.libraries.TrafficPredictor;
import com.net2plan.libraries.TrafficSeries;

public interface IMonitorizableElement 
{
    public TrafficSeries getMonitoredOrForecastedCarriedTraffic ();
    public void setMonitoredOrForecastedCarriedTraffic (TrafficSeries newTimeSeries);
    public double getCurrentTrafficToAddMonitSample ();
    public boolean isPossibleToSetCurrentTrafficAsGivenMonitSample ();
    public void setCurrentTrafficToGivenMonitSample (double traffic);
    
    public Optional<TrafficPredictor> getTrafficPredictor (); 
    public void setTrafficPredictor (TrafficPredictor tp);
    public void removeTrafficPredictor ();
}
