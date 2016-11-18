package com.net2plan.gui.utils.viewEditTopolTables.visualizationFilters;

import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.utils.Pair;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * @author César
 * @date 15/11/2016
 */
public final class VisualizationFiltersController
{
    private static Set<IVisualizationFilter> currentVisualizationFilters;
    static{

        currentVisualizationFilters = new HashSet<>();
    }
    private VisualizationFiltersController(){


    }
    public static void addVisualizationFilter(IVisualizationFilter vf){

        if(containsVisualizationFilter(vf)){
            throw new Net2PlanException("This visualization filter has already been added");
        }
        if(vf == null){
            throw new Net2PlanException("A null visualization filter cannot be added");
        }
        else{
            currentVisualizationFilters.add(vf);
        }

    }

    public static void removeVisualizationFilter(String visFilterName){

        for(IVisualizationFilter vf : currentVisualizationFilters){
            if(visFilterName.equals(vf.getUniqueName())){
                currentVisualizationFilters.remove(vf);
            }
        }
    }
    public static Set<IVisualizationFilter> getCurrentVisualizationFilters(){

        return currentVisualizationFilters;
    }

    public static boolean containsVisualizationFilter(IVisualizationFilter vf){

        return currentVisualizationFilters.contains(vf);
    }

    public static void removeAllVisualizationFilters(){

        currentVisualizationFilters.clear();
    }

    public static boolean isVisibleNetworkElement(NetworkElement element)
    {

        boolean isVisible = true;
        if(currentVisualizationFilters.size() == 0)
            return true;
        else{
            for(IVisualizationFilter vf : currentVisualizationFilters)
            {
                if(vf.isVisibleNetworkElement(element) == false){
                    isVisible = false;
                    break;
                }

            }
        }
        return isVisible;
    }

    public static boolean isVisibleForwardingRules(Pair<Demand,Link> fRuleKey, Double fRuleValue)
    {
        boolean isVisible = true;
        if(currentVisualizationFilters.isEmpty())
            return true;
        else{
            for(IVisualizationFilter vf : currentVisualizationFilters)
            {
                if(vf.isVisibleForwardingRules(fRuleKey,fRuleValue) == false)
                {
                    isVisible = false;
                    break;
                }
            }
        }
        return isVisible;
    }
}
