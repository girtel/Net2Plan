package com.net2plan.gui.plugins.networkDesign.io.excel;

import java.io.File;

/**
 * @author Jorge San Emeterio
 * @date 18/04/17
 */
public class ExcelTest
{

    public static void main(String[] args)
    {
        Object[][] data =
                {
                        {"Hola", "Mundo", 2}
                };
        File file = new File("/home/arch/Downloads", "hola.xls");

        try
        {
            ExcelWriter.writeToFile(file, data);
        } catch (ExcelParserException e)
        {
            e.printStackTrace();
        }
    }
}
