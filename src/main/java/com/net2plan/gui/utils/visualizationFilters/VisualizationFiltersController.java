package com.net2plan.gui.utils.visualizationFilters;

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
    private static ArrayList<IVisualizationFilter> currentVisualizationFilters;
    private static String filteringMode = "AND";
    static{

        currentVisualizationFilters = new ArrayList<>();
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
                break;
            }
        }
    }
    public static ArrayList<IVisualizationFilter> getCurrentVisualizationFilters(){

        return currentVisualizationFilters;
    }


    public static boolean containsVisualizationFilter(IVisualizationFilter vf){

        return currentVisualizationFilters.contains(vf);
    }

    public static void removeAllVisualizationFilters(){

        currentVisualizationFilters.clear();
    }

    public static void updateFilteringMode(String newFilteringMode){

        FilteringModes currentMode = FilteringModes.fromStringToEnum(newFilteringMode);

        switch(currentMode){

            case AND:
                filteringMode = "AND";
                break;
            case OR:
                filteringMode = "OR";
                break;
        }
    }

    public static IVisualizationFilter getVisualizationFilterByName(String visFilterName){
        
        IVisualizationFilter finalVf = null;
        for(IVisualizationFilter vf : currentVisualizationFilters){
            if(visFilterName.equals(vf.getUniqueName())){
                finalVf = vf;
                break;
            }
        }
        return finalVf;
    }

    public static void activateVisualizationFilter(IVisualizationFilter vf){

        vf.setActive(true);
    }

    public static void deactivateVisualizationFilter(IVisualizationFilter vf){

        vf.setActive(false);

    }

    public static boolean areAllFiltersInactive(){
        int counter = 0;
        for(IVisualizationFilter vf : currentVisualizationFilters){

            if(!vf.isActive()) counter++;
        }

        return counter == currentVisualizationFilters.size();
    }

    public static boolean isVisibleNetworkElement(NetworkElement element)
    {

        boolean isVisible = true;
        if(currentVisualizationFilters.size() == 0) return true;
        if(areAllFiltersInactive()) return true;

        else{
            if(filteringMode.equals("AND"))
            {
                for(IVisualizationFilter vf : currentVisualizationFilters)
                {
                    if(vf.isActive())
                    {
                        if(vf.isVisibleNetworkElement(element) == false){
                            isVisible = false;
                            break;
                        }

                    }
                }

            }
            else{
                isVisible = false;
                for(IVisualizationFilter vf : currentVisualizationFilters)
                {
                    if (vf.isActive())
                    {
                        if(vf.isVisibleNetworkElement(element) == true){
                            isVisible = true;
                            break;
                        }
                    }

                }
            }

        }
        return isVisible;
    }

    public static boolean isVisibleForwardingRules(Pair<Demand,Link> fRuleKey, Double fRuleValue)
    {
        boolean isVisible = true;
        if(currentVisualizationFilters.size() == 0) return true;
        if(areAllFiltersInactive()) return true;

        else{
            if(filteringMode.equals("AND"))
            {
                for(IVisualizationFilter vf : currentVisualizationFilters)
                {
                    if(vf.isActive())
                    {
                        if(vf.isVisibleForwardingRules(fRuleKey, fRuleValue) == false){
                            isVisible = false;
                            break;
                        }

                    }
                }

            }
            else{
                isVisible = false;
                for(IVisualizationFilter vf : currentVisualizationFilters)
                {
                    if (vf.isActive())
                    {
                        if(vf.isVisibleForwardingRules(fRuleKey, fRuleValue) == true){
                            isVisible = true;
                            break;
                        }
                    }

                }
            }

        }
        return isVisible;
    }

    public enum FilteringModes
    {
        AND ("AND"),
        OR ("OR");

        private final String text;
        private FilteringModes(final String text) {this.text = text;}

        public static FilteringModes fromStringToEnum(final String text)
        {
            switch (text)
            {
                case "AND":
                    return FilteringModes.AND;
                case "OR":
                    return FilteringModes.OR;
            }
            return null;
        }
    }
}
