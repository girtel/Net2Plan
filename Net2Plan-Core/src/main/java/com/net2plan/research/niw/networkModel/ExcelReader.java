package com.net2plan.research.niw.networkModel;

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
 */
public final class ExcelReader
{
    private static ExcelReader reader;
    enum ExcelExtension
    {
        OLE2("xls"),
        OOXML("xlsx");
        private final String text;
        ExcelExtension(final String text){ this.text = text; }
        public static ExcelExtension parseString(final String txt) { return Arrays.asList(ExcelExtension.values()).stream().filter(e->e.text.equals(txt)).findFirst().orElseThrow(()->new Net2PlanException ()); }
        @Override
        public String toString() { return text; }
    }
    
    
    
    private ExcelReader() {}

    /** Reads the Excel file and returns a map with key the sheet name, value the matrix of objects with the values of the cells in the sheet
     * @param file see above
     * @return see above
     */
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
     * @return Read-only map of: Sheet name - Sheet data
     */
    private Map<String, Object[][]> read(File file, String sheetName)
    {
        if (file == null) throw new IllegalArgumentException("Target file cannot be null");

        final ExcelExtension fileExtension = ExcelExtension.parseString(FilenameUtils.getExtension(file.getAbsolutePath()));
        try
        {
            if (fileExtension == null) throw new NullPointerException();

            switch (fileExtension)
            {
                case OLE2:
                    return Collections.unmodifiableMap(readWorkbook(new HSSFWorkbook(new FileInputStream(file)), sheetName));
                case OOXML:
                    return Collections.unmodifiableMap(readWorkbook(new XSSFWorkbook(new FileInputStream(file)), sheetName));
                default:
                    throw new Net2PlanException("Unknown file extension");
            }
        } catch (Exception e)
        {
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