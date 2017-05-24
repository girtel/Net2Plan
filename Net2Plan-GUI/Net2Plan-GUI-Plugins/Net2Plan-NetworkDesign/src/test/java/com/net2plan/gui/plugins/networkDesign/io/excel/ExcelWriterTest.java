/*******************************************************************************
 * Copyright (c) 2017 Pablo Pavon Marino and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the 2-clause BSD License 
 * which accompanies this distribution, and is available at
 * https://opensource.org/licenses/BSD-2-Clause
 *
 * Contributors:
 *     Pablo Pavon Marino and others - initial API and implementation
 *******************************************************************************/
package com.net2plan.gui.plugins.networkDesign.io.excel;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Jorge San Emeterio
 * @date 19/04/17
 */
public class ExcelWriterTest
{
    private static File fileOLE2;
    private static File fileOOXML;

    @Before
    public void setUp()
    {
        try
        {
            fileOLE2 = File.createTempFile("temp", ".xls");
            fileOLE2.deleteOnExit();

            fileOOXML = File.createTempFile("temp", ".xlsx");
            fileOOXML.deleteOnExit();
        } catch (IOException e)
        {
            e.printStackTrace();
            fail();
        }
    }

    @Test(expected = ExcelParserException.class)
    public void invalidFileExtension()
    {
        try
        {
            final File temp = File.createTempFile("temp", ".jpg");
            temp.deleteOnExit();
            ExcelWriter.writeToFile(temp, null, null);
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    @Test(expected = ExcelParserException.class)
    public void noFileExtension()
    {
        try
        {
            final File temp = File.createTempFile("temp", "");
            temp.deleteOnExit();
            ExcelWriter.writeToFile(temp, null, null);
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    @Test
    public void nullCellsTest()
    {
        Object[][] data = {{null, null}, {null, null}};
        ExcelWriter.writeToFile(fileOLE2, null, data);
    }

    @Test
    public void writeOLE2()
    {
        ExcelWriter.writeToFile(fileOLE2, null, null);
    }

    @Test
    public void writeOOXML()
    {
        ExcelWriter.writeToFile(fileOOXML, null, null);
    }

    @Test
    public void writeDataOLE2()
    {
        Object[][] data = {{"Hello", "World"}, {1, 2}, {'a', 'b'}};
        ExcelWriter.writeToFile(fileOLE2, "Test", data);

        try
        {
            final Workbook workbook = new HSSFWorkbook(new FileInputStream(fileOLE2.getAbsoluteFile()));

            assertEquals(1, workbook.getNumberOfSheets());
            assertEquals("Test", workbook.getSheetName(workbook.getActiveSheetIndex()));

            final Sheet sheet = workbook.getSheet("Test");

            assertEquals(3, sheet.getPhysicalNumberOfRows());
            assertEquals(2, sheet.getRow(0).getPhysicalNumberOfCells());

            assertEquals("Hello", sheet.getRow(0).getCell(0).toString());
            assertEquals("World", sheet.getRow(0).getCell(1).toString());
            assertEquals("1", sheet.getRow(1).getCell(0).toString());
            assertEquals("2", sheet.getRow(1).getCell(1).toString());
            assertEquals("a", sheet.getRow(2).getCell(0).toString());
            assertEquals("b", sheet.getRow(2).getCell(1).toString());
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    @Test
    public void writeDataOOXML()
    {
        Object[][] data = {{"Hello", "World"}, {1, 2}, {'a', 'b'}};
        ExcelWriter.writeToFile(fileOOXML, "Test", data);

        try
        {
            final Workbook workbook = new XSSFWorkbook(new FileInputStream(fileOOXML.getAbsoluteFile()));

            assertEquals(1, workbook.getNumberOfSheets());
            assertEquals("Test", workbook.getSheetName(workbook.getActiveSheetIndex()));

            final Sheet sheet = workbook.getSheet("Test");

            assertEquals(3, sheet.getPhysicalNumberOfRows());
            assertEquals(2, sheet.getRow(0).getPhysicalNumberOfCells());

            assertEquals("Hello", sheet.getRow(0).getCell(0).toString());
            assertEquals("World", sheet.getRow(0).getCell(1).toString());
            assertEquals("1", sheet.getRow(1).getCell(0).toString());
            assertEquals("2", sheet.getRow(1).getCell(1).toString());
            assertEquals("a", sheet.getRow(2).getCell(0).toString());
            assertEquals("b", sheet.getRow(2).getCell(1).toString());
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}