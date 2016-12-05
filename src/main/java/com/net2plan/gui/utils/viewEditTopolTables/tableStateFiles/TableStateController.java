package com.net2plan.gui.utils.viewEditTopolTables.tableStateFiles;

import com.net2plan.gui.utils.viewEditTopolTables.specificTables.AdvancedJTableNetworkElement;
import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.internal.ErrorHandling;
import com.net2plan.internal.plugins.IOFilter;
import org.codehaus.stax2.XMLInputFactory2;
import org.codehaus.stax2.XMLStreamReader2;

import javax.swing.filechooser.FileFilter;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.events.XMLEvent;
import java.io.File;
import java.io.InputStream;

/**
 * @author César
 * @date 04/12/2016
 */
public final class TableStateController
{
    private TableStateController(){

    }

    /*Loads a external file .n2pst and updates the state of table

     */

    public static void loadTableState(AdvancedJTableNetworkElement table){

        XMLInputFactory2 xmlInputFactory = (XMLInputFactory2) XMLInputFactory2.newInstance();
        //XMLStreamReader2 xmlStreamReader = (XMLStreamReader2) xmlInputFactory.createXMLStreamReader(inputStream);
    }

    /*Takes the current State from table and saves in a external file .n2pst

     */
    public static void saveTableState(AdvancedJTableNetworkElement table){

        /*Hay que usar un JFileChooser y en el código de abajo pone como exportar algo a un archivo*/

    }

    /*private IOFilter getCurrentIOFilter() {
        FileFilter currentFilter = getFileFilter();
        if (!(currentFilter instanceof IOFilter))
            throw new RuntimeException("Bad filter");

        return (IOFilter) currentFilter;
    }*/

    /*public void saveNetPlan(NetPlan netPlan) {

        IOFilter exporter = getCurrentIOFilter();
        File file = getSelectedFile();
        exporter.saveToFile(netPlan, file);
    }*/
   /* public NetPlan(InputStream inputStream)
    {
        this();

        try
        {
            XMLInputFactory2 xmlInputFactory = (XMLInputFactory2) XMLInputFactory2.newInstance();
            XMLStreamReader2 xmlStreamReader = (XMLStreamReader2) xmlInputFactory.createXMLStreamReader(inputStream);

            while(xmlStreamReader.hasNext())
            {
                int eventType = xmlStreamReader.next();
                switch (eventType)
                {
                    case XMLEvent.START_ELEMENT:
                        String elementName = xmlStreamReader.getName().toString();
                        if (!elementName.equals("network")) throw new RuntimeException("Root element must be 'network'");

                        IReaderNetPlan netPlanFormat;
                        int index = xmlStreamReader.getAttributeIndex(null, "version");
                        if (index == -1)
                        {
                            System.out.println ("Version 1");
                            netPlanFormat = new ReaderNetPlan_v1();
                        }
                        else
                        {
                            int version = xmlStreamReader.getAttributeAsInt(index);
                            switch(version)
                            {
                                case 2:
                                    System.out.println ("Version 2");
                                    netPlanFormat = new ReaderNetPlan_v2();
                                    break;

                                case 3:
                                    System.out.println ("Version 3");
                                    netPlanFormat = new ReaderNetPlan_v3();
                                    break;

                                case 4:
                                    System.out.println ("Version 4");
                                    netPlanFormat = new ReaderNetPlan_v4();
                                    break;

                                default:
                                    throw new Net2PlanException("Wrong version number");
                            }
                        }

                        netPlanFormat.create(this, xmlStreamReader);
                        if (ErrorHandling.isDebugEnabled()) this.checkCachesConsistency();
                        return;

                    default:
                        break;
                }
            }
        }
        catch (Net2PlanException e)
        {
            if (ErrorHandling.isDebugEnabled()) ErrorHandling.printStackTrace(e);
            throw(e);
        }
        catch (FactoryConfigurationError | Exception e)
        {
            if (ErrorHandling.isDebugEnabled()) ErrorHandling.printStackTrace(e);
            throw new RuntimeException(e);
        }

        throw new Net2PlanException("Not a valid .n2p file");
    }*/




}
