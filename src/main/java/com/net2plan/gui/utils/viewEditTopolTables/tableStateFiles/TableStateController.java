package com.net2plan.gui.utils.viewEditTopolTables.tableStateFiles;

import com.net2plan.gui.utils.viewEditTopolTables.specificTables.AdvancedJTableNetworkElement;
import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.internal.ErrorHandling;
import com.net2plan.internal.XMLUtils;
import com.net2plan.internal.plugins.IOFilter;
import com.net2plan.utils.Pair;
import org.codehaus.stax2.XMLInputFactory2;
import org.codehaus.stax2.XMLOutputFactory2;
import org.codehaus.stax2.XMLStreamReader2;
import org.codehaus.stax2.XMLStreamWriter2;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
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
    private static JFileChooser chooser, saveFileChooser;
    private static File selectedFile, fileToSave;
    private static InputStream inputStream;

    static{
        chooser = new JFileChooser();
        saveFileChooser = new JFileChooser();
        selectedFile = new File("");
        fileToSave = new File("");
    }
    private TableStateController(){

    }

    /*Loads a external file .n2pst and updates the state of table

     */

    public static void loadTableState(AdvancedJTableNetworkElement table) throws XMLStreamException {
        int rc = chooser.showOpenDialog(null);
        if (rc != JFileChooser.APPROVE_OPTION) return;
        selectedFile = chooser.getSelectedFile();
        try {
            inputStream = new FileInputStream(selectedFile);
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }
        XMLInputFactory2 xmlInputFactory = (XMLInputFactory2) XMLInputFactory2.newInstance();
        XMLStreamReader2 xmlStreamReader = null;
        try {
            xmlStreamReader = (XMLStreamReader2) xmlInputFactory.createXMLStreamReader(inputStream);
        } catch (XMLStreamException e) {
            e.printStackTrace();
        }

        int counter = 0;
        boolean continueFlag = false;
        while (xmlStreamReader.hasNext()) {
            int eventType = xmlStreamReader.next();
            switch (eventType) {
                case XMLEvent.START_ELEMENT:
                    String fileType = xmlStreamReader.getName().toString();
                    if (!fileType.equals("tableState"))
                        throw new RuntimeException("Bad. This is not a table state file");

                    String type = xmlStreamReader.getAttributeValue(0);
                    if(!type.equals(table.getNetworkElementType().toString()))
                        throw new RuntimeException("Bad. This Table Model is not from this table");

                    continueFlag = true;

                default:
                    break;
            }
            if(continueFlag)
                break;

        }

        updateTableFromTableState(xmlStreamReader, table);
        try {
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        xmlStreamReader.close();
    }

    /*Takes the current State from table and saves in a external file .n2pst

     */
    public static void saveTableState(AdvancedJTableNetworkElement table) throws XMLStreamException {

        int rc = saveFileChooser.showSaveDialog(null);
        if (rc != JFileChooser.APPROVE_OPTION) return;
        fileToSave = saveFileChooser.getSelectedFile();
        String path = fileToSave.getAbsolutePath();
        fileToSave = new File(path+".xml");

        OutputStream outputStream = null;

        try {
            OutputStream outputStream1 = outputStream = new FileOutputStream(fileToSave);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        XMLOutputFactory2 output = (XMLOutputFactory2) XMLOutputFactory2.newFactory();
        XMLStreamWriter2 writer = (XMLStreamWriter2) output.createXMLStreamWriter(outputStream);
        writer.writeStartDocument("UTF-8", "1.0");

        XMLUtils.indent(writer, 0);
        writer.writeStartElement("tableState");
        writer.writeAttribute("networkElementType", table.getNetworkElementType().toString());

        XMLUtils.indent(writer, 1);
        writer.writeStartElement("mainTableColumns");
        XMLUtils.indent(writer, 2);
        ArrayList<String> mainTableColumns = table.getMainTableColumns();
        for(int i = 0; i < mainTableColumns.size(); i++){

            writer.writeStartElement("column");
            writer.writeAttribute("name",mainTableColumns.get(i));
            writer.writeAttribute("position", String.valueOf(i));
            writer.writeEndElement();
        }

        writer.writeEndElement();
        XMLUtils.indent(writer, 1);
        writer.writeStartElement("fixedTableColumns");
        XMLUtils.indent(writer, 2);
        ArrayList<String> fixedTableColumns = table.getFixedTableColumns();
        for(int i = 0; i < fixedTableColumns.size(); i++){

            writer.writeStartElement("column");
            writer.writeAttribute("name",fixedTableColumns.get(i));
            writer.writeAttribute("position", String.valueOf(i));
            writer.writeEndElement();
        }

        writer.writeEndElement();
        XMLUtils.indent(writer, 1);
        writer.writeStartElement("hiddenTableColumns");
        XMLUtils.indent(writer, 2);
        HashMap<String, Integer> hiddenMap = table.getHiddenColumns();
        for(Map.Entry<String, Integer> entry : hiddenMap.entrySet()){

            writer.writeStartElement("column");
            writer.writeAttribute("name", entry.getKey());
            writer.writeAttribute("position", String.valueOf(entry.getValue()));
            writer.writeEndElement();
        }

        writer.writeEndElement();
        XMLUtils.indent(writer, 1);
        writer.writeStartElement("attributesState");
        writer.writeAttribute("expandAttributes", String.valueOf(table.areAttributesInDifferentColums()));
        writer.writeEndElement();

        XMLUtils.indent(writer, 0);
        writer.writeEndElement();
        writer.writeEndDocument();
        writer.flush();
        writer.close();

        JOptionPane.showMessageDialog(null,"Table State saved successfully!");

    }

    private static void updateTableFromTableState(XMLStreamReader2 xmlStreamReader, AdvancedJTableNetworkElement table) throws XMLStreamException
    {

        ArrayList<String> mainTableColumns = new ArrayList<>();
        ArrayList<String> fixedTableColumns = new ArrayList<>();
        HashMap<String,Integer>  hiddenColumns = new HashMap<>();
        boolean expandAttributes = false;

        boolean finish = false;

        while (xmlStreamReader.hasNext()) {
            xmlStreamReader.next();
            switch (xmlStreamReader.getEventType()) {
                case XMLEvent.START_ELEMENT:
                    String startElementName = xmlStreamReader.getName().toString();
                    System.out.println(startElementName);
                    if(startElementName.equals("mainTableColumns"))
                    {
                        mainTableColumns = parseMainTableColumns(xmlStreamReader);

                    }
                    if(startElementName.equals("fixedTableColumns")){

                        fixedTableColumns = parseFixedTableColumns(xmlStreamReader);

                    }
                    if(startElementName.equals("hiddenTableColumns")){

                        hiddenColumns = parseHiddenColumns(xmlStreamReader);

                    }
                    if(startElementName.equals("attributesState")){

                        expandAttributes = parseAttributesState(xmlStreamReader);

                    }
            }
        }

    }

    private static ArrayList<String> parseMainTableColumns(XMLStreamReader2 xmlStreamReader) throws XMLStreamException {
        ArrayList<String> mainTableColumns = new ArrayList<>();
        String columnName = "";
        boolean finish = false;
        while(xmlStreamReader.hasNext())
        {
            xmlStreamReader.next();

            switch(xmlStreamReader.getEventType())
            {
                case XMLEvent.START_ELEMENT:
                    String startElementName = xmlStreamReader.getName().toString();
                    switch(startElementName)
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

            if(finish)
                break;
        }

        return mainTableColumns;

    }

    private static ArrayList<String> parseFixedTableColumns(XMLStreamReader2 xmlStreamReader) throws XMLStreamException {
        ArrayList<String> fixedTableColumns = new ArrayList<>();
        String columnName = "";
        boolean finish = false;
        while(xmlStreamReader.hasNext())
        {
            xmlStreamReader.next();

            switch(xmlStreamReader.getEventType())
            {
                case XMLEvent.START_ELEMENT:
                    String startElementName = xmlStreamReader.getName().toString();
                    switch(startElementName)
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

            if(finish)
                break;
        }

        return fixedTableColumns;

    }

    private static HashMap<String, Integer> parseHiddenColumns(XMLStreamReader2 xmlStreamReader) throws XMLStreamException {

        HashMap<String, Integer> hiddenColumns = new HashMap<>();
        Pair<String, Integer> columnPosition;
        boolean finish = false;
        while(xmlStreamReader.hasNext())
        {
            xmlStreamReader.next();

            switch(xmlStreamReader.getEventType())
            {
                case XMLEvent.START_ELEMENT:
                    String startElementName = xmlStreamReader.getName().toString();
                    switch(startElementName)
                    {
                        case "column":
                            columnPosition = parseHiddenColumn(xmlStreamReader);
                            hiddenColumns.put(columnPosition.getFirst(),columnPosition.getSecond());
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

            if(finish)
                break;
        }

        return hiddenColumns;


    }


    private static String parseColumn(XMLStreamReader2 xmlStreamReader)
    {
        String columnName = "";

        int numAttributes = xmlStreamReader.getAttributeCount();
        for(int i = 0; i < numAttributes; i++)
        {
            String localName = xmlStreamReader.getAttributeLocalName(i);
            switch(localName)
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

    private static Pair<String,Integer> parseHiddenColumn(XMLStreamReader2 xmlStreamReader) throws XMLStreamException {
        String columnName = "";
        int columnPosition = 0;

        int numAttributes = xmlStreamReader.getAttributeCount();
        for(int i = 0; i < numAttributes; i++)
        {
            String localName = xmlStreamReader.getAttributeLocalName(i);
            switch(localName)
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


        return Pair.of(columnName,columnPosition);
    }

    private static boolean parseAttributesState(XMLStreamReader2 xmlStreamReader) throws XMLStreamException {

        boolean expandAttributes = false;

        int numAttributes = xmlStreamReader.getAttributeCount();
        for(int i = 0; i < numAttributes; i++)
        {
            String localName = xmlStreamReader.getAttributeLocalName(i);
            switch(localName)
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




