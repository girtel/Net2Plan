package com.net2plan.gui.plugins.networkDesign.io.excel;

import org.apache.commons.io.FilenameUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * @author Jorge San Emeterio
 * @date 11-Nov-16
 */
public class ExcelWriter
{
    private static File file;
    private static ExcelExtension fileExtension;
    private static Object[][] data;

    public static void writeToFile(@Nonnull File file, @Nonnull Object[][] data) throws ExcelParserException
    {
        final ExcelExtension fileExtension = ExcelExtension.parseString(FilenameUtils.getExtension(file.getAbsolutePath()));

        ExcelWriter.file = file;
        ExcelWriter.fileExtension = fileExtension;
        ExcelWriter.data = data;

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
        final Workbook workbook = new HSSFWorkbook();

        doWrite(workbook);
    }

    private static void writeOOXML()
    {
        final Workbook workbook = new XSSFWorkbook();

        doWrite(workbook);
    }

    private static void doWrite(@Nonnull final Workbook workbook)
    {
        final CreationHelper helper = workbook.getCreationHelper();
        final Sheet sheet = workbook.createSheet();

        int rowNum = 0;
        for (Object[] dataRow : data)
        {
            final Row row = sheet.createRow(rowNum++);

            int colNum = 0;
            for (Object field : dataRow)
            {
                final Cell cell = row.createCell(colNum++);

                cell.setCellValue(helper.createRichTextString(field.toString()));
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
            e.printStackTrace();
        } catch (IOException e)
        {
            e.printStackTrace();
        } finally
        {
            try
            {
                workbook.close();

                if (fileOut != null)
                    fileOut.close();
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }
}

