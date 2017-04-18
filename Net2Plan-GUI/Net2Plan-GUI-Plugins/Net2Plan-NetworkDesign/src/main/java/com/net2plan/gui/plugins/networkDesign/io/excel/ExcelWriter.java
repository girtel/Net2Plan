package com.net2plan.gui.plugins.networkDesign.io.excel;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

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
    private File saveFile;
    private Net2PlanExcelExtension fileExtension;

    public ExcelWriter(final File file, final Net2PlanExcelExtension extension)
    {
        this.saveFile = file;
        this.fileExtension = extension;
    }

    public ExcelWriter(final File file, final String extension)
    {
        this.saveFile = file;
        this.fileExtension = Net2PlanExcelExtension.parseString(extension);
    }

    public void writeFile()
    {
        switch (fileExtension)
        {
            case OLE2:
                writeOLE2();
                break;
            case OOXML:
                writeOOXML();
                break;
        }
    }

    private void writeOLE2()
    {
        final Workbook workbook = new HSSFWorkbook();

        doWrite(workbook);
    }

    private void writeOOXML()
    {
        final Workbook workbook = new XSSFWorkbook();

        doWrite(workbook);
    }

    private void doWrite(final Workbook workbook)
    {

        final CreationHelper helper = workbook.getCreationHelper();
        final Sheet sheet = workbook.createSheet();

//        row.createCell(1).setCellValue(1.2);
//        row.createCell(2).setCellValue(
//                createHelper.createRichTextString("This is a string"));

        try
        {
            if (!saveFile.exists())
            {
                saveFile.mkdirs();
            }

            final FileOutputStream fileOut = new FileOutputStream(saveFile);
            workbook.write(fileOut);
            fileOut.close();

        } catch (FileNotFoundException e)
        {
            e.printStackTrace();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public void setSaveFile(final File file)
    {
        this.saveFile = file;
    }

    public void setFileExtension(final Net2PlanExcelExtension extension)
    {
        this.fileExtension = extension;
    }

    public void setFileExtension(final String extension)
    {
        this.fileExtension = Net2PlanExcelExtension.parseString(extension);
    }

    public enum Net2PlanExcelExtension
    {
        OLE2("xls"),
        OOXML("xlsx");

        private final String text;

        Net2PlanExcelExtension(final String text)
        {
            this.text = text;
        }

        public static Net2PlanExcelExtension parseString(final String txt)
        {
            if (txt.toLowerCase().equals(Net2PlanExcelExtension.OLE2))
            {
                return Net2PlanExcelExtension.OLE2;
            } else if (txt.toLowerCase().equals(Net2PlanExcelExtension.OOXML))
            {
                return Net2PlanExcelExtension.OOXML;
            }

            final StringBuilder builder = new StringBuilder();
            builder.append("Unknown file extension: " + txt + "\n");
            builder.append("Available extensions are: \n");

            for (Net2PlanExcelExtension value : Net2PlanExcelExtension.values())
            {
                builder.append("- " + value + "\n");
            }

            throw new ExcelParserException(builder.toString());
        }

        @Override
        public String toString()
        {
            return text;
        }
    }
}

