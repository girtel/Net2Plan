package com.net2plan.gui.utils.viewEditTopolTables.tableStateFiles;

import com.net2plan.gui.utils.AdvancedJTable;
import com.net2plan.gui.utils.viewEditTopolTables.specificTables.AdvancedJTableNetworkElement;
import com.net2plan.internal.Constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * @author César
 * @date 04/12/2016
 */
public class TableState
{
    ArrayList<String> mainTableColumns, fixedTableColumns;
    HashMap<String, Integer> hiddenTableColumns;
    boolean expandAttributes;
    Constants.NetworkElementType networkElementType;

    public TableState(Constants.NetworkElementType networkElementType){

        mainTableColumns = new ArrayList<>();
        fixedTableColumns = new ArrayList<>();
        hiddenTableColumns = new HashMap<>();
        expandAttributes = false;
        this.networkElementType = networkElementType;

    }

    public void setMainTableColumns(ArrayList<String> mainTableColumnsAux){

        mainTableColumns.clear();
        for(String col : mainTableColumnsAux)
        {
            mainTableColumns.add(col);
        }

    }

    public ArrayList<String> getMainTableColumns(){
        return mainTableColumns;
    }

    public void setFixedTableColumns(ArrayList<String> fixedTableColumnsAux){

        fixedTableColumns.clear();
        for(String col : fixedTableColumnsAux)
        {
            fixedTableColumns.add(col);
        }

    }

    public ArrayList<String> getFixedTableColumns(){
        return fixedTableColumns;
    }

    public void setHiddenTableColumns(HashMap<String, Integer> hiddenColumnsAux){

        hiddenTableColumns.clear();
        for(Map.Entry<String, Integer> entry : hiddenColumnsAux.entrySet()){

            hiddenTableColumns.put(entry.getKey(),entry.getValue());
        }
    }

    public HashMap<String, Integer> getHiddenTableColumns(){

        return hiddenTableColumns;
    }

    public void setExpandAttributes(boolean expandAttributes){

        this.expandAttributes = expandAttributes;
    }

    public boolean getExpandAttributes(){

        return expandAttributes;
    }

    public Constants.NetworkElementType getNetworkElementType(){

        return networkElementType;
    }

}
