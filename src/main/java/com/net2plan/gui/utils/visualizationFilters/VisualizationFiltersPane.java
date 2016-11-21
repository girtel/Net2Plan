package com.net2plan.gui.utils.visualizationFilters;

import com.net2plan.gui.utils.*;
import com.net2plan.gui.utils.offlineExecPane.OfflineExecutionPanel;
import com.net2plan.interfaces.networkDesign.Configuration;
import com.net2plan.interfaces.networkDesign.IAlgorithm;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.internal.Constants;
import com.net2plan.internal.ErrorHandling;
import com.net2plan.internal.SystemUtils;
import com.net2plan.internal.plugins.IGUIModule;
import com.net2plan.utils.ClassLoaderUtils;
import com.net2plan.utils.StringUtils;
import com.net2plan.utils.Triple;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @author César
 * @date 19/11/2016
 */
public class VisualizationFiltersPane extends JPanel
{
    private final INetworkCallback mainWindow;
    private JButton load, delete, deleteAll;
    private JRadioButton andButton, orButton;
    private static JFileChooser fileChooser;
    private File selectedFile;
    private final AdvancedJTable table;
    private final static TableCellRenderer CHECKBOX_RENDERER;
    private final static Object[] HEADER;

    static
    {
        CHECKBOX_RENDERER = new CheckBoxRenderer();
        HEADER = StringUtils.arrayOf("Filter","Description","Active");
    }

    public VisualizationFiltersPane(INetworkCallback mainWindow)
    {
        super();

        this.mainWindow = mainWindow;

        Object[][] data = {{null, null, null}};
        fileChooser = new JFileChooser();
        TableModel model = new ClassAwareTableModelImpl(data, HEADER);

        table = new AdvancedJTable(model);
        setLayout(new MigLayout("insets 0 0 0 0", "[grow]", "[grow]"));

        File FILTERS_DIRECTORY = new File(IGUIModule.CURRENT_DIR + SystemUtils.getDirectorySeparator() + "workspace");
        FILTERS_DIRECTORY = FILTERS_DIRECTORY.isDirectory() ? FILTERS_DIRECTORY : IGUIModule.CURRENT_DIR;

        load = new JButton("Load Visualization Filter");
        load.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                try
                {
                    if(!mainWindow.getDesign().hasNodes()){
                        throw new Net2PlanException("A topology is necessary to add filters");
                    }
                    int rc = fileChooser.showOpenDialog(null);
                    if (rc != JFileChooser.APPROVE_OPTION) return;
                    selectedFile = fileChooser.getSelectedFile();
                    loadFilter();
                } catch (NoRunnableCodeFound ex)
                {
                    ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to load");
                } catch (Throwable ex)
                {
                    ErrorHandling.addErrorOrException(ex, RunnableSelector.class);
                    ErrorHandling.showErrorDialog("Error loading runnable code");
                }
            }
        });
        delete = new JButton("Delete Visualization Filter");
        delete.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                if (VisualizationFiltersController.getCurrentVisualizationFilters().size() == 0)
                    JOptionPane.showMessageDialog(null, "No filters to remove");
                else
                {
                    Set<String> filtersSet = new LinkedHashSet<String>();
                    for (IVisualizationFilter vf : VisualizationFiltersController.getCurrentVisualizationFilters())
                    {
                        filtersSet.add(vf.getUniqueName());
                    }
                    Object out = JOptionPane.showInputDialog(null, "Please, select a filter to remove",
                            "Remove visualization filter",
                            JOptionPane.QUESTION_MESSAGE, null, filtersSet.toArray(new String[filtersSet.size()]),
                            filtersSet.iterator().next());

                    String filterNameToRemove = (String) out;
                    VisualizationFiltersController.removeVisualizationFilter(filterNameToRemove);
                    updateFiltersTable();
                    mainWindow.updateNetPlanView();

                }
            }
        });
        deleteAll = new JButton("Delete All Visualization Filters");
        deleteAll.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                VisualizationFiltersController.removeAllVisualizationFilters();
                updateFiltersTable();
                mainWindow.updateNetPlanView();
            }
        });
        andButton = new JRadioButton("AND filtering mode");
        andButton.addItemListener(new ItemListener()
        {
            @Override
            public void itemStateChanged(ItemEvent e)
            {
                if (andButton.isSelected())
                {
                    orButton.setSelected(false);
                    VisualizationFiltersController.updateFilteringMode("AND");
                    mainWindow.updateNetPlanView();
                }
            }
        });
        orButton = new JRadioButton("OR filtering mode");
        orButton.addItemListener(new ItemListener()
        {
            @Override
            public void itemStateChanged(ItemEvent e)
            {
                if (orButton.isSelected())
                {
                    andButton.setSelected(false);
                    VisualizationFiltersController.updateFilteringMode("OR");
                    mainWindow.updateNetPlanView();
                }
            }
        });
        andButton.setSelected(true);
        setLayout(new MigLayout("", "[][grow][]", "[][][][][grow]"));
        add(new JLabel("Filtering Options"), "top, growx, spanx 2, wrap, wmin 100");
        add(andButton, "wrap");
        add(orButton, "wrap");
        add(new JLabel("Filters"), "spanx 3, wrap");
        add(new JScrollPane(table), "spanx 3, grow, wrap");
        add(load, "spanx 3, wrap");
        add(delete, " spanx 3, wrap");
        add(deleteAll, " spanx, wrap");


    }



    public void loadFilter() throws IOException
    {
        IVisualizationFilter newFilter = ClassLoaderUtils.getInstance(selectedFile,"", IVisualizationFilter.class);
        VisualizationFiltersController.addVisualizationFilter(newFilter);
        updateFiltersTable();
        mainWindow.updateNetPlanView();
        ((Closeable) newFilter.getClass().getClassLoader()).close();


    }

    public void updateFiltersTable(){

        TableModel tm = table.getModel();
        ArrayList<IVisualizationFilter> currentVisFilters = VisualizationFiltersController.getCurrentVisualizationFilters();
        int length = currentVisFilters.size();
        Object[][] newData = new Object[length][HEADER.length];
        IVisualizationFilter vf;
        for(int i = 0;i<length;i++)
        {
            vf = currentVisFilters.get(i);
            newData[i][0] = (Object)vf.getUniqueName();
            newData[i][1] = (Object)vf.getDescription();
            newData[i][2] = (Object)vf.isActive();
        }


        ((DefaultTableModel) tm).setDataVector(newData,HEADER);
        for(int j = 0;j<length;j++){

            table.setCellRenderer(j,2,new CheckBoxRenderer());
        }

    }


    private static class CheckBoxRenderer extends JCheckBox implements TableCellRenderer
    {
        public CheckBoxRenderer()
        {
            setHorizontalAlignment(JLabel.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
        {
            if (isSelected)
            {
                setForeground(table.getSelectionForeground());
                setBackground(table.getSelectionBackground());
            } else
            {
                setForeground(table.getForeground());
                setBackground(table.getBackground());
            }

            setSelected(value != null && Boolean.parseBoolean(value.toString()));
            return this;
        }
    }

        private class ClassAwareTableModelImpl extends ClassAwareTableModel
        {
            public ClassAwareTableModelImpl(Object[][] dataVector, Object[] columnIdentifiers)
            {
                super(dataVector, columnIdentifiers);
            }

            @Override
            public Class getColumnClass(int col)
            {
                if(col == 2) return Boolean.class;

                return String.class;
            }

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex)
            {
                if(columnIndex == 2) return true;
                return false;
            }

            @Override
            public void setValueAt(Object value, int row, int column)
            {
                if(column == 2)
                {
                    if(value == null) return;
                    boolean visible = (Boolean) value;
                    String filterToChange = (String)table.getModel().getValueAt(row,0);
                    IVisualizationFilter vf = VisualizationFiltersController.getVisualizationFilterByName(filterToChange);
                    vf.setActive(visible);
                    super.setValueAt(value, row, column);
                    updateFiltersTable();
                    mainWindow.updateNetPlanView();
                }
            }
        }

            private static class SortByParameterNameComparator implements Comparator<Triple<String, String, String>>
            {
                @Override
                public int compare(Triple<String, String, String> o1, Triple<String, String, String> o2)
                {
                    return o1.getFirst().compareTo(o2.getFirst());
                }
            }
    }







