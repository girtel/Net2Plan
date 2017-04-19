package com.net2plan.gui.plugins.networkDesign.io.excel;

import org.apache.commons.io.FilenameUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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

    public static void writeToFile(@Nonnull File file, @Nullable String sheetName, @Nullable Object[][] data) throws ExcelParserException
    {
        ExcelWriter.file = file;
        ExcelWriter.data = data;
        ExcelWriter.sheetName = sheetName;

        final String extension = FilenameUtils.getExtension(file.getAbsolutePath());
        if (extension.isEmpty())
        {
            ExcelWriter.fileExtension = ExcelExtension.OLE2;
            ExcelWriter.file = new File(file.getAbsolutePath() + "." + ExcelWriter.fileExtension.toString());
        } else
        {
            ExcelWriter.fileExtension = ExcelExtension.parseString(extension);
        }

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
        } catch (FileNotFoundException e)
        {
            // No file was found
            workbook = new HSSFWorkbook();
        } catch (Exception e)
        {
            throw new ExcelParserException(e.getMessage());
        }

        doWrite(workbook);
    }

    private static void writeOOXML()
    {
        Workbook workbook;
        try
        {
            workbook = new XSSFWorkbook(new FileInputStream(file.getAbsoluteFile()));
        } catch (FileNotFoundException e)
        {
            // No file was found
            workbook = new XSSFWorkbook();
        } catch (Exception e)
        {
            throw new ExcelParserException(e.getMessage());
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

                    cell.setCellValue(helper.createRichTextString(field.toString()));
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

