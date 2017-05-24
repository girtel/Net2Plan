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

import org.apache.commons.io.FilenameUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;

/**
 * @author Jorge San Emeterio
 * @date 11-Nov-16
 */
public class ExcelWriter
{
    private static File file;
    private static Object[][] data;
    private static String sheetName;
    private static ExcelExtension fileExtension;

    public static void writeToFile( File file,  String sheetName,  Object[][] data) throws ExcelParserException
    {
        // Reboot
        ExcelWriter.file = null;
        ExcelWriter.data = null;
        ExcelWriter.sheetName = null;
        ExcelWriter.fileExtension = null;

        ExcelWriter.file = file;
        ExcelWriter.data = data;
        ExcelWriter.sheetName = sheetName;
        ExcelWriter.fileExtension = ExcelExtension.parseString(FilenameUtils.getExtension(file.getAbsolutePath()));

        assert ExcelWriter.fileExtension != null;

        switch (ExcelWriter.fileExtension)
        {
            case OLE2:
                writeOLE2();
                break;
            case OOXML:
                writeOOXML();
                break;
            default:
                throw new ExcelParserException();
        }
    }

    private static void writeOLE2()
    {
        Workbook workbook;
        try
        {
            // Trying to open file
            workbook = WorkbookFactory.create(new FileInputStream(file.getAbsoluteFile()));
        } catch (Exception e)
        {
            // No file was found
            workbook = new HSSFWorkbook();
        }

        doWrite(workbook);
    }

    private static void writeOOXML()
    {
        Workbook workbook;
        try
        {
            workbook = new XSSFWorkbook(new FileInputStream(file.getAbsoluteFile()));
        } catch (Exception e)
        {
            // No file was found
            workbook = new XSSFWorkbook();
        }
        doWrite(workbook);
    }

    private static void doWrite(Workbook workbook)
    {
        assert workbook != null;

        final CreationHelper helper = workbook.getCreationHelper();

        if (workbook.getActiveSheetIndex() > 0)
            workbook.setActiveSheet(workbook.getActiveSheetIndex() + 1);

        final Sheet sheet;
        if (sheetName != null)
            sheet = workbook.createSheet(sheetName);
        else
            sheet = workbook.createSheet();

        int rowNum = 0;
        if (data != null)
        {
            for (Object[] dataRow : data)
            {
                final Row row = sheet.createRow(rowNum++);

                int colNum = 0;
                for (Object field : dataRow)
                {
                    final Cell cell = row.createCell(colNum++);

                    final String fieldContent = field != null ? field.toString() : "null";
                    cell.setCellValue(helper.createRichTextString(fieldContent));
                }
            }
        }

        FileOutputStream fileOut = null;
        try
        {
            file.getParentFile().mkdirs();

            fileOut = new FileOutputStream(file);
            workbook.write(fileOut);
        } catch (FileNotFoundException e)
        {
            throw new ExcelParserException(e.getMessage());
        } catch (IOException e)
        {
            throw new ExcelParserException(e.getMessage());
        } finally
        {
            try
            {
                workbook.close();

                if (fileOut != null)
                    fileOut.close();
            } catch (IOException e)
            {
                throw new ExcelParserException(e.getMessage());
            }
        }
    }
}

