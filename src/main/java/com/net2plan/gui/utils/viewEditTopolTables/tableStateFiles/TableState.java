package com.net2plan.gui.utils.viewEditTopolTables.tableStateFiles;

import com.net2plan.gui.utils.AdvancedJTable;
import com.net2plan.gui.utils.viewEditTopolTables.specificTables.AdvancedJTableNetworkElement;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author César
 * @date 04/12/2016
 */
public class TableState
{
    ArrayList<String> mainTableColumns, fixedTableColumns;
    HashMap<String, Integer> hiddenTableColumns;
    boolean expandAttributes;

    public TableState(){

        mainTableColumns = new ArrayList<>();
        fixedTableColumns = new ArrayList<>();
        hiddenTableColumns = new HashMap<>();
        expandAttributes = false;

    }

    public void setMainTableColumns(ArrayList<String> mainTableColumnsAux){


    }

}
