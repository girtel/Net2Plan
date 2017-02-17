package com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.tableStateFiles;

import com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.specificTables.AdvancedJTable_NetworkElement;
import com.net2plan.internal.Constants;
import com.net2plan.internal.XMLUtils;
import com.net2plan.utils.Pair;
import org.codehaus.stax2.XMLInputFactory2;
import org.codehaus.stax2.XMLOutputFactory2;
import org.codehaus.stax2.XMLStreamReader2;
import org.codehaus.stax2.XMLStreamWriter2;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * @author César
 * @date 04/12/2016
 */
public final class TableStateController
{
    private static final String fileFormatName = "XML definition file (*.xml)";
    private static final String fileFormat = ".xml";

    private TableStateController()
    {

    }

    /*Loads a external file .xml and updates the state of table

     */

    public static HashMap<Constants.NetworkElementType, TableState> loadTableState(Map<Constants.NetworkElementType, AdvancedJTable_NetworkElement> tables) throws XMLStreamException
    {
        final JFileChooser fileChooser = new JFileChooser();

        fileChooser.setAcceptAllFileFilterUsed(false);

        final FileNameExtensionFilter filter = new FileNameExtensionFilter(fileFormatName, fileFormat.replace(".", ""));
        fileChooser.addChoosableFileFilter(filter);

        XMLStreamReader2 xmlStreamReader = null;
        int rc = fileChooser.showOpenDialog(null);
        HashMap<Constants.NetworkElementType, TableState> tStateMap = new HashMap<>();
        if (rc == JFileChooser.APPROVE_OPTION)
        {
            final File selectedFile = fileChooser.getSelectedFile();

            InputStream inputStream = null;
            try
            {
                inputStream = new FileInputStream(selectedFile);

                XMLInputFactory2 xmlInputFactory = (XMLInputFactory2) XMLInputFactory2.newInstance();
                xmlStreamReader = (XMLStreamReader2) xmlInputFactory.createXMLStreamReader(inputStream);

                Xml_Reader_Loop:
                while (xmlStreamReader.hasNext())
                {
                    int eventType = xmlStreamReader.next();
                    switch (eventType)
                    {
                        case XMLEvent.START_ELEMENT:
                            String fileType = xmlStreamReader.getName().toString();

                            if (!fileType.equals("tableStateFile"))
                                throw new RuntimeException("Bad. This is not a table state file");

                            break Xml_Reader_Loop;
                        default:
                            break;
                    }
                }

                for (Map.Entry<Constants.NetworkElementType, AdvancedJTable_NetworkElement> entry : tables.entrySet())
                {
                    Pair<Constants.NetworkElementType, TableState> networkElementTypeTableStatePair = parseTableState(xmlStreamReader, entry.getKey());
                    tStateMap.put(networkElementTypeTableStatePair.getFirst(), networkElementTypeTableStatePair.getSecond());
                }
            } catch (FileNotFoundException ex)
            {
                ex.printStackTrace();
            } finally
            {
                try
                {
                    if (inputStream != null) inputStream.close();
                    if (xmlStreamReader != null) xmlStreamReader.close();
                } catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }

        return tStateMap;
    }

    /*Takes the current State from table and saves in a external file .n2pst

     */
    public static void saveTableState(Map<Constants.NetworkElementType, AdvancedJTable_NetworkElement> tables) throws XMLStreamException
    {
        final JFileChooser fileChooser = new JFileChooser();
        fileChooser.setAcceptAllFileFilterUsed(false);

        final FileNameExtensionFilter filter = new FileNameExtensionFilter(fileFormatName, fileFormat.replace(".", ""));
        fileChooser.addChoosableFileFilter(filter);

        int rc = fileChooser.showSaveDialog(null);
        if (rc != JFileChooser.APPROVE_OPTION) return;

        final File fileToSave =
                fileChooser.getSelectedFile().getAbsolutePath().endsWith(fileFormat) ? fileChooser.getSelectedFile() : new File(fileChooser.getSelectedFile().getAbsolutePath() + fileFormat);

        OutputStream outputStream = null;
        try
        {
            outputStream = new FileOutputStream(fileToSave);
            XMLOutputFactory2 output = (XMLOutputFactory2) XMLOutputFactory2.newFactory();
            XMLStreamWriter2 writer = (XMLStreamWriter2) output.createXMLStreamWriter(outputStream);
            writer.writeStartDocument("UTF-8", "1.0");

            XMLUtils.indent(writer, 0);
            writer.writeStartElement("tableStateFile");


            for (Map.Entry<Constants.NetworkElementType, AdvancedJTable_NetworkElement> entry : tables.entrySet())
            {

                AdvancedJTable_NetworkElement table = entry.getValue();
                XMLUtils.indent(writer, 1);
                writer.writeStartElement("tableState");
                writer.writeAttribute("networkElementType", table.getNetworkElementType().toString());



                XMLUtils.indent(writer, 2);
                writer.writeStartElement("mainTableColumns");
                XMLUtils.indent(writer, 3);
                ArrayList<String> mainTableColumns = table.getMainTableColumns();
                for (int i = 0; i < mainTableColumns.size(); i++)
                {
                    writer.writeEmptyElement("column");
                    writer.writeAttribute("name", mainTableColumns.get(i));
                    writer.writeAttribute("position", String.valueOf(i));
                    if (i != mainTableColumns.size() - 1) XMLUtils.indent(writer, 3);
                }
                XMLUtils.indent(writer, 2);
                writer.writeEndElement();
                XMLUtils.indent(writer, 2);
                writer.writeStartElement("fixedTableColumns");
                XMLUtils.indent(writer, 3);
                ArrayList<String> fixedTableColumns = table.getFixedTableColumns();
                for (int i = 0; i < fixedTableColumns.size(); i++)
                {
                    writer.writeEmptyElement("column");
                    writer.writeAttribute("name", fixedTableColumns.get(i));
                    writer.writeAttribute("position", String.valueOf(i));
                    if (i != fixedTableColumns.size() - 1) XMLUtils.indent(writer, 3);
                }
                XMLUtils.indent(writer, 2);
                writer.writeEndElement();
                XMLUtils.indent(writer, 2);
                writer.writeStartElement("hiddenTableColumns");

                HashMap<String, Integer> hiddenMap = table.getHiddenColumns();
                final ArrayList<Map.Entry<String, Integer>> hiddenMapEntries = new ArrayList<>(hiddenMap.entrySet());

                for (int i = 0; i < hiddenMapEntries.size(); i++)
                {
                    final Map.Entry<String, Integer> hiddenEntry = hiddenMapEntries.get(i);
                    writer.writeEmptyElement("column");
                    writer.writeAttribute("name", hiddenEntry.getKey());
                    writer.writeAttribute("position", String.valueOf(hiddenEntry.getValue()));
                    if (i != hiddenMapEntries.size() - 1) XMLUtils.indent(writer, 3);
                }

                XMLUtils.indent(writer, 2);
                writer.writeEndElement();
                XMLUtils.indent(writer, 2);
                writer.writeStartElement("attributesState");
                writer.writeAttribute("expandAttributes", String.valueOf(table.areAttributesInDifferentColums()));
                writer.writeEndElement();

                XMLUtils.indent(writer, 1);
                writer.writeEndElement();


            }

            XMLUtils.indent(writer, 0);
            writer.writeEndElement();
            writer.writeEndDocument();
            writer.flush();
            writer.close();

            JOptionPane.showMessageDialog(null, "Tables visualization profile successfully saved!");

        } catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
    }

    private static Pair<Constants.NetworkElementType, TableState> parseTableState(XMLStreamReader2 xmlStreamReader, Constants.NetworkElementType networkElementType) throws XMLStreamException
    {

        ArrayList<String> mainTableColumns = new ArrayList<>();
        ArrayList<String> fixedTableColumns = new ArrayList<>();
        HashMap<String, Integer> hiddenColumns = new HashMap<>();
        boolean expandAttributes = false;

        boolean finish = false;
        boolean cont = false;
        while (xmlStreamReader.hasNext())
        {
            xmlStreamReader.next();
            switch (xmlStreamReader.getEventType())
            {
                case XMLEvent.START_ELEMENT:
                    String startElementName = xmlStreamReader.getName().toString();
                    if (!cont && startElementName.equals("tableState"))
                    {
                        int numAtt = xmlStreamReader.getAttributeCount();
                        for (int i = 0; i < numAtt; i++)
                        {
                            String net = xmlStreamReader.getAttributeValue(i);
                            if (net.equals(networkElementType.toString()))
                            {
                                cont = true;
                            }
                        }

                    }

                    if (cont)
                    {
                        if (startElementName.equals("mainTableColumns"))
                        {

                            mainTableColumns = parseMainTableColumns(xmlStreamReader);
                        }
                        if (startElementName.equals("fixedTableColumns"))
                        {

                            fixedTableColumns = parseFixedTableColumns(xmlStreamReader);
                        }
                        if (startElementName.equals("hiddenTableColumns"))
                        {

                            hiddenColumns = parseHiddenColumns(xmlStreamReader);
                        }
                        if (startElementName.equals("attributesState"))
                        {

                            expandAttributes = parseAttributesState(xmlStreamReader);
                            finish = true;
                        }
                    }
                    break;
            }
            if (finish)
                break;
        }

        TableState tState = new TableState(networkElementType);
        tState.setExpandAttributes(expandAttributes);
        tState.setFixedTableColumns(fixedTableColumns);
        tState.setHiddenTableColumns(hiddenColumns);
        tState.setMainTableColumns(mainTableColumns);

        return Pair.of(networkElementType, tState);
    }

    private static ArrayList<String> parseMainTableColumns(XMLStreamReader2 xmlStreamReader) throws XMLStreamException
    {
        ArrayList<String> mainTableColumns = new ArrayList<>();
        String columnName = "";
        boolean finish = false;
        while (xmlStreamReader.hasNext())
        {
            xmlStreamReader.next();

            switch (xmlStreamReader.getEventType())
            {
                case XMLEvent.START_ELEMENT:
                    String startElementName = xmlStreamReader.getName().toString();
                    switch (startElementName)
                    {
                        case "column":
                            columnName = parseColumn(xmlStreamReader);
                            mainTableColumns.add(columnName);
                            break;

                        default:
                            break;
                    }
                    break;

                case XMLEvent.END_ELEMENT:
                    String endElementName = xmlStreamReader.getName().toString();
                    if (endElementName.equals("mainTableColumns"))
                        finish = true;
                    break;

                default:
                    break;
            }

            if (finish)
                break;
        }

        return mainTableColumns;

    }

    private static ArrayList<String> parseFixedTableColumns(XMLStreamReader2 xmlStreamReader) throws XMLStreamException
    {
        ArrayList<String> fixedTableColumns = new ArrayList<>();
        String columnName = "";
        boolean finish = false;
        while (xmlStreamReader.hasNext())
        {
            xmlStreamReader.next();

            switch (xmlStreamReader.getEventType())
            {
                case XMLEvent.START_ELEMENT:
                    String startElementName = xmlStreamReader.getName().toString();
                    switch (startElementName)
                    {
                        case "column":
                            columnName = parseColumn(xmlStreamReader);
                            fixedTableColumns.add(columnName);
                            break;

                        default:
                            finish = true;
                            break;
                    }
                    break;

                case XMLEvent.END_ELEMENT:
                    String endElementName = xmlStreamReader.getName().toString();
                    if (endElementName.equals("fixedTableColumns"))
                        finish = true;
                    break;

                default:
                    break;
            }

            if (finish)
                break;
        }

        return fixedTableColumns;

    }

    private static HashMap<String, Integer> parseHiddenColumns(XMLStreamReader2 xmlStreamReader) throws XMLStreamException
    {

        HashMap<String, Integer> hiddenColumns = new HashMap<>();
        Pair<String, Integer> columnPosition;
        boolean finish = false;
        while (xmlStreamReader.hasNext())
        {
            xmlStreamReader.next();

            switch (xmlStreamReader.getEventType())
            {
                case XMLEvent.START_ELEMENT:
                    String startElementName = xmlStreamReader.getName().toString();
                    switch (startElementName)
                    {
                        case "column":
                            columnPosition = parseHiddenColumn(xmlStreamReader);
                            hiddenColumns.put(columnPosition.getFirst(), columnPosition.getSecond());
                            break;

                        default:
                            finish = true;
                            break;
                    }
                    break;

                case XMLEvent.END_ELEMENT:
                    String endElementName = xmlStreamReader.getName().toString();
                    if (endElementName.equals("hiddenTableColumns"))
                        finish = true;
                    break;

                default:
                    break;
            }

            if (finish)
                break;
        }

        return hiddenColumns;


    }


    private static String parseColumn(XMLStreamReader2 xmlStreamReader)
    {
        String columnName = "";

        int numAttributes = xmlStreamReader.getAttributeCount();
        for (int i = 0; i < numAttributes; i++)
        {
            String localName = xmlStreamReader.getAttributeLocalName(i);
            switch (localName)
            {
                case "name":

                    columnName = xmlStreamReader.getAttributeValue(i);
                    break;

                default:
                    break;
            }
        }


        return columnName;
    }

    private static Pair<String, Integer> parseHiddenColumn(XMLStreamReader2 xmlStreamReader) throws XMLStreamException
    {
        String columnName = "";
        int columnPosition = 0;

        int numAttributes = xmlStreamReader.getAttributeCount();
        for (int i = 0; i < numAttributes; i++)
        {
            String localName = xmlStreamReader.getAttributeLocalName(i);
            switch (localName)
            {
                case "name":

                    columnName = xmlStreamReader.getAttributeValue(i);
                    break;

                case "position":
                    columnPosition = xmlStreamReader.getAttributeAsInt(i);
                    break;

                default:
                    break;
            }
        }


        return Pair.of(columnName, columnPosition);
    }

    private static boolean parseAttributesState(XMLStreamReader2 xmlStreamReader) throws XMLStreamException
    {

        boolean expandAttributes = false;

        int numAttributes = xmlStreamReader.getAttributeCount();
        for (int i = 0; i < numAttributes; i++)
        {
            String localName = xmlStreamReader.getAttributeLocalName(i);
            switch (localName)
            {
                case "expandAttributes":

                    expandAttributes = xmlStreamReader.getAttributeAsBoolean(i);
                    break;

                default:
                    break;
            }
        }

        return expandAttributes;
    }


}




