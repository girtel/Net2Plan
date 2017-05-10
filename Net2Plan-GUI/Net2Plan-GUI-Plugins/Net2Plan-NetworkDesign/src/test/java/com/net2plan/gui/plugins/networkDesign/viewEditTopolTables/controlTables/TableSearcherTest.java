package com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.controlTables;

import com.net2plan.gui.utils.AdvancedJTable;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.swing.table.DefaultTableModel;

/**
 * @author Jorge San Emeterio
 * @date 10/05/17
 */
@RunWith(JUnitParamsRunner.class)
public class TableSearcherTest
{
    
    @Test
    @Parameters({"A", "Z", "O"})
    public void searchForItemTest(String searchItem)
    {
        AdvancedJTable table = new AdvancedJTable();
        table.setModel(new DefaultTableModel());

        final Object[][] dataVector = new Object[][]
                {
                        {"A", "M", "Z"},
                        {"B", "O", "Y"},
                        {"C", "O", "Z"}
                };
        ((DefaultTableModel) table.getModel()).setDataVector(dataVector, dataVector[0]);

        TableSearcher searcher = new TableSearcher(table);
    }
}