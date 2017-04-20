package com.net2plan.gui.plugins.networkDesign.io.excel;

/**
 * @author Jorge San Emeterio
 * @date 18/04/17
 */
public enum ExcelExtension
{
    OLE2("xls"),
    OOXML("xlsx");

    private final String text;

    ExcelExtension(final String text)
    {
        this.text = text;
    }

    public static ExcelExtension parseString(final String txt) throws ExcelParserException
    {
        if (txt.toLowerCase().equals(ExcelExtension.OLE2.toString()))
        {
            return ExcelExtension.OLE2;
        } else if (txt.toLowerCase().equals(ExcelExtension.OOXML.toString()))
        {
            return ExcelExtension.OOXML;
        }

        final StringBuilder builder = new StringBuilder();
        builder.append("Unknown file extension: " + txt + "\n");
        builder.append("Available extensions are: \n");

        for (ExcelExtension value : ExcelExtension.values())
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
