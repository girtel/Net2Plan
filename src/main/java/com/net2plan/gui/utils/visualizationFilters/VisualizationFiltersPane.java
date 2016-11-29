package com.net2plan.gui.utils.visualizationFilters;

import com.net2plan.gui.utils.*;
import com.net2plan.gui.utils.offlineExecPane.OfflineExecutionPanel;
import com.net2plan.interfaces.networkDesign.Configuration;
import com.net2plan.interfaces.networkDesign.IAlgorithm;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.internal.Constants;
import com.net2plan.internal.ErrorHandling;
import com.net2plan.internal.IExternal;
import com.net2plan.internal.SystemUtils;
import com.net2plan.internal.plugins.IGUIModule;
import com.net2plan.utils.ClassLoaderUtils;
import com.net2plan.utils.Pair;
import com.net2plan.utils.StringUtils;
import com.net2plan.utils.Triple;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.event.CellEditorListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

/**
 * @author César
 * @date 19/11/2016
 * Class TableButton seen in
 * http://stackoverflow.com/questions/1475543/how-to-add-button-in-a-row-of-jtable-in-swing-java/1475625#1475625?newreg=e3d8c64a54b44aa6980ff4b0c6a5a123
 */
public class VisualizationFiltersPane extends JPanel
{
    private final INetworkCallback mainWindow;
    private JButton load, deleteAll, activeAll, deactiveAll;
    private JRadioButton andButton, orButton;
    private static JFileChooser fileChooser;
    private File selectedFile;
    private final AdvancedJTable table;
    private final static TableCellRenderer CHECKBOX_RENDERER;
    private final static Object[] HEADER;
    private final JTextArea descriptionArea;
    private final JTextField txt_file;
    private final Class [] columnClass;
    private Map<String, Class> implementations;

    static
    {
        CHECKBOX_RENDERER = new CheckBoxRenderer();
        HEADER = StringUtils.arrayOf("Remove","Filter","Active");
    }

    public VisualizationFiltersPane(INetworkCallback mainWindow)
    {
        super();

        this.mainWindow = mainWindow;

        Object[][] data = {{null, null, null}};
        //HAY QUE CAMBIARLO
        File FILTERS_DIRECTORY = new File("C:/Users/cesar_000/Desktop/N2P Work/target/classes/com/net2plan/prooves");
        FILTERS_DIRECTORY = FILTERS_DIRECTORY.isDirectory() ? FILTERS_DIRECTORY : IGUIModule.CURRENT_DIR;

        fileChooser = new JFileChooser(FILTERS_DIRECTORY);
        TableModel model = new ClassAwareTableModelImpl(data, HEADER);

        table = new AdvancedJTable(model);
        TableColumn removeColumn = table.getColumn("Remove");
        TableColumn activeColumn = table.getColumn("Active");
        removeColumn.setResizable(false);
        removeColumn.setMinWidth(90);
        removeColumn.setMaxWidth(90);
        activeColumn.setResizable(false);
        activeColumn.setMinWidth(60);
        activeColumn.setMaxWidth(60);
        updateFiltersTable();

        descriptionArea = new JTextArea();
        descriptionArea.setFont(new JLabel().getFont());
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setEditable(false);
        descriptionArea.setColumns(80);
        descriptionArea.setRows(15);

        txt_file = new JTextField();
        txt_file.setEditable(false);

        columnClass = new Class[3];
        columnClass[0] = TableButton.class;
        columnClass[1] = String.class;
        columnClass[2] = Boolean.class;

        setLayout(new MigLayout("insets 0 0 0 0", "[grow]", "[grow]"));


        load = new JButton("Load");
        load.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                try
                {
                    int rc = fileChooser.showOpenDialog(null);
                    if (rc != JFileChooser.APPROVE_OPTION) return;
                    selectedFile = fileChooser.getSelectedFile();
                    loadImplementations(selectedFile);
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

        deleteAll = new JButton("Remove All");
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
        activeAll = new JButton("Active All");
        activeAll.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                for(IVisualizationFilter vf : VisualizationFiltersController.getCurrentVisualizationFilters())
                {
                    vf.setActive(true);
                }
                updateFiltersTable();
                mainWindow.updateNetPlanView();
            }
        });
        deactiveAll = new JButton("Deactive All");
        deactiveAll.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                for(IVisualizationFilter vf : VisualizationFiltersController.getCurrentVisualizationFilters())
                {
                    vf.setActive(false);
                }
                updateFiltersTable();
                mainWindow.updateNetPlanView();
            }
        });
        andButton.setSelected(true);
        setLayout(new MigLayout("", "[][grow][]", "[][][][][grow]"));
        add(new JLabel("Filters File"));
        add(txt_file, "grow");
        add(load, "spanx");
        add(new JLabel("Filtering Options"), "top, growx, spanx 2, wrap, wmin 100");
        add(andButton);
        add(orButton, "wrap");
        add(new JLabel("Filters"), "spanx 3, wrap");
        add(deleteAll);
        add(activeAll);
        add(deactiveAll, "wrap");
        add(new JScrollPane(table), "spanx 3, grow, wrap");
        add(new JLabel("Description"), "spanx 3, wrap");
        add(new JScrollPane(descriptionArea),"spanx 3, grow, wrap");

        table.addMouseListener(new MouseListener()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                int clickedRow = table.rowAtPoint(e.getPoint());
                int clickedColumn = table.columnAtPoint(e.getPoint());
                String selectedFilter = (String)table.getModel().getValueAt(clickedRow,1);

                if(clickedColumn == 0)
                {
                    VisualizationFiltersController.removeVisualizationFilter(selectedFilter);
                    descriptionArea.setText("");
                    updateFiltersTable();
                    mainWindow.updateNetPlanView();
                }
                else{

                    IVisualizationFilter vf = VisualizationFiltersController.getVisualizationFilterByName(selectedFilter);
                    descriptionArea.setText(vf.getDescription());

                }

            }

            @Override
            public void mousePressed(MouseEvent e)
            {

            }

            @Override
            public void mouseReleased(MouseEvent e)
            {

            }

            @Override
            public void mouseEntered(MouseEvent e)
            {

            }

            @Override
            public void mouseExited(MouseEvent e)
            {

            }
        });

    }

    private void loadImplementations(File f) {
        try {
            if (!f.isAbsolute()) f = new File(SystemUtils.getCurrentDir(), f.getPath());

            Map<String, Class> aux_implementations = new TreeMap<String, Class>();
            List<Class<IVisualizationFilter>> aux = ClassLoaderUtils.getClassesFromFile(f, IVisualizationFilter.class);
            for (Class<IVisualizationFilter> implementation : aux) {

                if (IVisualizationFilter.class.isAssignableFrom(implementation)) {

                    aux_implementations.put(implementation.getName(), IVisualizationFilter.class);
                    }
                }


            if (aux_implementations.isEmpty())
                throw new NoRunnableCodeFound(f, new LinkedHashSet<Class<? extends IExternal>>() {
                {
                    add(IVisualizationFilter.class);
                }
                });

            implementations = aux_implementations;

            txt_file.setText(f.getCanonicalPath());

            for(Map.Entry<String,Class> implValue : implementations.entrySet())
            {
                IVisualizationFilter instance = ClassLoaderUtils.getInstance(f,implValue.getKey(),IVisualizationFilter.class);
                VisualizationFiltersController.addVisualizationFilter(instance);
                instance.setActive(false);
                ((Closeable) instance.getClass().getClassLoader()).close();
            }
            updateFiltersTable();
            mainWindow.updateNetPlanView();

        } catch (NoRunnableCodeFound e) {
            throw (e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
            newData[i][0] = new TableButton("Remove");
            newData[i][1] = vf.getUniqueName();
            newData[i][2] = vf.isActive();
        }


        ((DefaultTableModel) tm).setDataVector(newData,HEADER);
        for(int j = 0;j<length;j++){
            table.setCellRenderer(j,0,new TableButton("Remove"));
            table.setCellRenderer(j,2,CHECKBOX_RENDERER);
        }

        TableColumn removeColumn = table.getColumn("Remove");
        TableColumn activeColumn = table.getColumn("Active");
        removeColumn.setResizable(false);
        removeColumn.setMinWidth(90);
        removeColumn.setMaxWidth(90);
        activeColumn.setResizable(false);
        activeColumn.setMinWidth(60);
        activeColumn.setMaxWidth(60);


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
                return columnClass[col];
            }

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex)
            {
                if(VisualizationFiltersController.getCurrentVisualizationFilters().size() == 0) return false;
                if(columnIndex == 2 || columnIndex == 0) return true;
                return false;
            }

            @Override
            public void setValueAt(Object value, int row, int column)
            {
                if(column == 2)
                {
                    if(value == null) return;
                    boolean visible = (Boolean) value;
                    String filterToChange = (String)table.getModel().getValueAt(row,1);
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

    private static class TableButton extends JButton implements TableCellRenderer, TableCellEditor {
        private int selectedRow;
        private int selectedColumn;

        public TableButton(String text) {
            super(text);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table,
                                                       Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            return this;
        }

        @Override
        public Component getTableCellEditorComponent(JTable table,
                                                     Object value, boolean isSelected, int row, int col) {
            selectedRow = row;
            selectedColumn = col;
            return this;
        }

        @Override
        public void addCellEditorListener(CellEditorListener arg0) {
        }

        @Override
        public void cancelCellEditing() {
        }

        @Override
        public Object getCellEditorValue() {
            return "";
        }

        @Override
        public boolean isCellEditable(EventObject arg0) {
            return true;
        }

        @Override
        public void removeCellEditorListener(CellEditorListener arg0) {
        }

        @Override
        public boolean shouldSelectCell(EventObject arg0) {
            return true;
        }

        @Override
        public boolean stopCellEditing() {
            return true;
        }
    }
    }







