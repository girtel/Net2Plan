package com.net2plan.gui.plugins.networkDesign.io.excel;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.google.common.collect.Maps;
import com.net2plan.interfaces.networkDesign.Net2PlanException;

/**
 * Generic Excel reader.
 *
 * Reads the contents of a Excel file and transforms it into a generic bi-dimensional array of {@code Object}.
 *
 * @author Jorge San Emeterio
 * @date 1/06/17
 */
public final class ExcelReader
{
    private static ExcelReader reader;

    private ExcelReader() {}

    public static Map<String, Object[][]> readFile(File file)
    {
        final ExcelReader excelReader = ExcelReader.getInstance();
        return excelReader.read(file, null);
    }

    public static Object[][] readSheet(File file, String sheetName)
    {
        final ExcelReader excelReader = ExcelReader.getInstance();
        return excelReader.read(file, sheetName).get(sheetName);
    }

    private static ExcelReader getInstance()
    {
        if (reader == null) reader = new ExcelReader();
        return reader;
    }

    /**
     * Parse an Excel file
     *
     * @param file      Excel file
     * @param sheetName Name of the sheet to be read. {@code null} reads all sheets in the Excel file.
     * @return Read-only map of: Sheet name -> Sheet data
     */
    private Map<String, Object[][]> read(File file, String sheetName)
    {
        if (file == null) throw new IllegalArgumentException("Target file cannot be null");
        final ExcelExtension fileExtension = ExcelExtension.parseString(FilenameUtils.getExtension(file.getAbsolutePath()));
        FileInputStream fileToRead = null;
        try
        {
            if (fileExtension == null) throw new NullPointerException();
            fileToRead = new FileInputStream(file);
            final Map<String, Object[][]> res;
            switch (fileExtension)
            {
                case OLE2:
                    res = Collections.unmodifiableMap(readWorkbook(new HSSFWorkbook(fileToRead), sheetName));
                    fileToRead.close();
                    return res;
                case OOXML:
                    res = Collections.unmodifiableMap(readWorkbook(new XSSFWorkbook(fileToRead), sheetName));
                    fileToRead.close();
                    return res;
                default:
                    throw new Net2PlanException("Unknown file extension");
            }
        } catch (Exception e)
        {
            if (fileToRead != null) try { fileToRead.close(); } catch (Exception ee) {}
            throw new Net2PlanException(e.getMessage());
        }
    }

    private Map<String, Object[][]> readWorkbook(Workbook workbook, String sheetName)
    {
        final Map<String, Object[][]> dataMap = Maps.newHashMap();

        if (sheetName == null)
        {
            for (Sheet sheet : workbook)
                dataMap.put(sheet.getSheetName(), getSheetData(sheet));
        } else
        {
            final Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null)
                throw new Net2PlanException("Unknown sheet: " + sheet);
            dataMap.put(sheet.getSheetName(), getSheetData(sheet));
        }

        return dataMap;
    }

    private Object[][] getSheetData(Sheet sheet)
    {
        if (sheet == null) throw new Net2PlanException("Invalid sheet name...");
        if (sheet.getFirstRowNum() == -1)
            return new Object[0][0];

        final FormulaEvaluator ev = sheet.getWorkbook().getCreationHelper().createFormulaEvaluator();

        final List<Object[]> sheetData = new ArrayList<>();
        for (int i = sheet.getFirstRowNum(); i <= sheet.getLastRowNum(); i++)
        {
            final Row row = sheet.getRow(i);
            if (row == null) break;

            final int rowWidth = row.getLastCellNum();
            if (rowWidth == -1) continue;

            final Object[] dataVector = new Object[rowWidth];
            Arrays.fill(dataVector, null);

            for (int j = row.getFirstCellNum(); j < row.getLastCellNum(); j++)
            {
                final Cell cell = row.getCell(j);
                if (cell != null)
                {
                    final CellValue cellValue = ev.evaluate(cell);
                    if (cellValue != null)
                    {
                        switch (cellValue.getCellTypeEnum())
                        {
                            case NUMERIC:
                                dataVector[j] = cellValue.getNumberValue();
                                break;
                            case BOOLEAN:
                                dataVector[j] = cellValue.getBooleanValue();
                                break;
                            default:
                                dataVector[j] = cellValue.getStringValue();
                                break;
                        }
                    }
                }
            }

            sheetData.add(dataVector);
        }

        final Object[][] dataObject = new Object[sheetData.size()][];
        for (int i = 0; i < dataObject.length; i++)
            dataObject[i] = sheetData.get(i);

        return dataObject;
    }
}
