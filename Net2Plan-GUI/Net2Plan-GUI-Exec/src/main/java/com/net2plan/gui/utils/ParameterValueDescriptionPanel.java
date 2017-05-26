/*******************************************************************************
 * Copyright (c) 2017 Pablo Pavon Marino and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the 2-clause BSD License 
 * which accompanies this distribution, and is available at
 * https://opensource.org/licenses/BSD-2-Clause
 *
 * Contributors:
 *     Pablo Pavon Marino and others - initial API and implementation
 *******************************************************************************/


package com.net2plan.gui.utils;

import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.internal.Constants.RunnableCodeType;
import com.net2plan.internal.ErrorHandling;
import com.net2plan.internal.SystemUtils;
import com.net2plan.utils.Pair;
import com.net2plan.utils.StringUtils;
import com.net2plan.utils.Triple;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Allows to define parameters.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
@SuppressWarnings("unchecked")
public class ParameterValueDescriptionPanel extends JPanel
{
    private final static TableCellRenderer CHECKBOX_RENDERER;
    private final static String[] HEADER;
    private final static Comparator<Triple<String, String, String>> PARAMETER_COMPARATOR;

    private File ALGORITHM_DIRECTORY;
    private final Map<String, Pair<RunnableSelector, Integer>> algorithms;
    private boolean haveData;
    private final AdvancedJTable table;
    private Map<String, Triple<String, String, Boolean>> currentValuesParam;

    static
    {
        CHECKBOX_RENDERER = new CheckBoxRenderer();
        HEADER = StringUtils.arrayOf("Parameter", "Value", "Description");
        PARAMETER_COMPARATOR = new SortByParameterNameComparator();
    }

    /**
     * Default constructor.
     *
     * @since 0.2.0
     */
    public ParameterValueDescriptionPanel()
    {
        setLayout(new BorderLayout());
        Object[][] data = {{null, null, null}};

        TableModel model = new ClassAwareTableModelImpl(data, HEADER);

        table = new AdvancedJTable(model);
        table.setEnabled(false);
        haveData = false;
        add(new JScrollPane(table), BorderLayout.CENTER);

        File CURRENT_DIRECTORY = SystemUtils.getCurrentDir();
        ALGORITHM_DIRECTORY = new File(CURRENT_DIRECTORY + SystemUtils.getDirectorySeparator() + "workspace");
        ALGORITHM_DIRECTORY = ALGORITHM_DIRECTORY.isDirectory() ? ALGORITHM_DIRECTORY : CURRENT_DIRECTORY;
        algorithms = new LinkedHashMap<String, Pair<RunnableSelector, Integer>>();
    }

    @Override
    public void setEnabled(boolean enabled)
    {
        super.setEnabled(enabled);
        table.setEnabled(enabled);
        if (!haveData) table.setEnabled(false);
    }

    private void addActionCellEditor(int rowModel, int columnModel)
    {
        JTextField textField = new JTextField();
        textField.setEnabled(false);
        textField.setBorder(BorderFactory.createEmptyBorder());
        DefaultCellEditor editor = new DefaultCellEditor(textField);
        editor.setClickCountToStart(1);

        table.setCellEditor(rowModel, columnModel, new ActionTableCellEditor(editor)
        {
            @Override
            protected void editCell(JTable table, int row, int column)
            {
                try
                {
                    int rowModel = table.convertRowIndexToModel(row);
                    String algorithm = table.getModel().getValueAt(rowModel, 0).toString();

                    RunnableSelector current = algorithms.get(algorithm).getFirst();

                    while (true)
                    {
                        try
                        {
                            int result = JOptionPane.showOptionDialog(null, current, "", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null);
                            if (result != JOptionPane.OK_OPTION) return;

                            TableModel model = table.getModel();
                            model.setValueAt(current.getRunnable().getFirst().getCanonicalPath(), rowModel, 1);
                            model.setValueAt(current.getRunnable().getSecond(), rowModel + 1, 1);
                            model.setValueAt(StringUtils.mapToString(current.getRunnableParameters(), "=", ", "), rowModel + 2, 1);

                            break;
                        } catch (Net2PlanException ex)
                        {
                            if (ErrorHandling.isDebugEnabled())
                                ErrorHandling.addErrorOrException(ex, ParameterValueDescriptionPanel.class);
                            ErrorHandling.showErrorDialog(ex.getMessage(), "Error");
                        }
                    }
                } catch (Throwable ex)
                {
                    ErrorHandling.addErrorOrException(ex, ParameterValueDescriptionPanel.class);
                    ErrorHandling.showErrorDialog("An error happened");
                }
            }
        });
    }

    private void addCheckboxCellEditor(boolean defaultValue, int rowIndex, int columnIndex)
    {
        JCheckBox checkBox = new JCheckBox();
        checkBox.setHorizontalAlignment(JLabel.CENTER);
        checkBox.setSelected(defaultValue);
        table.setCellEditor(rowIndex, columnIndex, new DefaultCellEditor(checkBox));
        table.setCellRenderer(rowIndex, columnIndex, CHECKBOX_RENDERER);
    }

    private void addComboCellEditor(String[] options, int rowIndex, int columnIndex)
    {
        JComboBox comboBox = new JComboBox();
        for (String option : options) comboBox.addItem(option);
        table.setCellEditor(rowIndex, columnIndex, new DefaultCellEditor(comboBox));
    }

    private void addFileChooserCellEditor(int rowIndex, int columnIndex, String defaultValue)
    {
        final JTextField textField = new JTextField();
        textField.setEnabled(false);
        textField.setBorder(BorderFactory.createEmptyBorder());

        final DefaultCellEditor editor = new DefaultCellEditor(textField);
        editor.setClickCountToStart(1);

        table.getModel().setValueAt(defaultValue, rowIndex, columnIndex);

        table.setCellEditor(rowIndex, columnIndex, new ActionTableCellEditor(editor)
        {
            @Override
            protected void editCell(JTable table, int row, int column)
            {
                final int rowModel = table.convertRowIndexToModel(row);
                final TableModel model = table.getModel();

                final JFileChooser fileChooser = new JFileChooser();
                fileChooser.setMultiSelectionEnabled(false);
                fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

                final int returnVal = fileChooser.showOpenDialog(null);

                if (returnVal == JFileChooser.APPROVE_OPTION)
                {
                    model.setValueAt(fileChooser.getSelectedFile().getAbsolutePath(), rowModel, 1);
                }
            }
        });
    }

    private void addFileMultiChooserCellEditor(int rowIndex, int columnIndex, String defaultValue)
    {
        final JTextField textField = new JTextField();
        textField.setEnabled(false);
        textField.setBorder(BorderFactory.createEmptyBorder());

        final DefaultCellEditor editor = new DefaultCellEditor(textField);
        editor.setClickCountToStart(1);

        table.getModel().setValueAt(defaultValue, rowIndex, columnIndex);

        table.setCellEditor(rowIndex, columnIndex, new ActionTableCellEditor(editor)
        {
            @Override
            protected void editCell(JTable table, int row, int column)
            {
                final int rowModel = table.convertRowIndexToModel(row);
                final TableModel model = table.getModel();

                final String fileSeparator = ">";
                final JFileChooser fileChooser = new JFileChooser();
                fileChooser.setMultiSelectionEnabled(true);
                fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

                final int returnVal = fileChooser.showOpenDialog(null);

                if (returnVal == JFileChooser.APPROVE_OPTION)
                {
                    final File[] selectedFiles = fileChooser.getSelectedFiles();
                    final StringBuilder builder = new StringBuilder();

                    for (int i = 0; i < selectedFiles.length; i++)
                    {
                        final File file = selectedFiles[i];

                        builder.append(file.getAbsolutePath());

                        if (i != selectedFiles.length - 1)
                        {
                            builder.append(fileSeparator);
                        }
                    }

                    model.setValueAt(builder.toString(), rowModel, 1);
                }
            }
        });
    }

    private void addPathChooserCellEditor(int rowIndex, int columnIndex, String defaultValue)
    {
        final JTextField textField = new JTextField();
        textField.setEnabled(false);
        textField.setBorder(BorderFactory.createEmptyBorder());

        final DefaultCellEditor editor = new DefaultCellEditor(textField);
        editor.setClickCountToStart(1);

        table.getModel().setValueAt(defaultValue, rowIndex, columnIndex);

        table.setCellEditor(rowIndex, columnIndex, new ActionTableCellEditor(editor)
        {
            @Override
            protected void editCell(JTable table, int row, int column)
            {
                final int rowModel = table.convertRowIndexToModel(row);
                final TableModel model = table.getModel();

                final JFileChooser fileChooser = new JFileChooser();
                fileChooser.setMultiSelectionEnabled(false);
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

                final int returnVal = fileChooser.showOpenDialog(null);

                if (returnVal == JFileChooser.APPROVE_OPTION)
                {
                    model.setValueAt(fileChooser.getSelectedFile().getAbsolutePath(), rowModel, 1);
                }
            }
        });
    }

    /**
     * Returns the parameter-value map.
     *
     * @return Parameter-value map
     * @since 0.2.0
     */
    public Map<String, String> getParameters()
    {
        Map<String, String> out = new LinkedHashMap<String, String>();

        if (haveData)
        {
            TableModel model = table.getModel();

            int numRows = model.getRowCount();
            for (int rowId = 0; rowId < numRows; rowId++)
                out.put(model.getValueAt(rowId, 0).toString(), model.getValueAt(rowId, 1).toString());
        }

        return out;
    }

    /**
     * Resets the data.
     *
     * @since 0.2.0
     */
    public void reset()
    {
        setParameters(new LinkedList<Triple<String, String, String>>());
    }

    /**
     * Sets parameter values.
     *
     * @param parameters Key-value map
     * @since 0.3.0
     */
    public void setParameterValues(Map<String, String> parameters)
    {
        if (!haveData) return;

        TableModel model = table.getModel();
        int numRows = model.getRowCount();
        for (int row = 0; row < numRows; row++)
        {
            String paramName = model.getValueAt(row, 0).toString();
            if (parameters.containsKey(paramName))
            {
                String value = parameters.get(paramName);

                Pattern p = Pattern.compile("#.*?#");
                Matcher m = p.matcher(value);

                String filteredValue = value;
                if (m.find())
                {
                    final String option = m.group();

                    filteredValue = filteredValue.replace(option, "").trim();
                }

                model.setValueAt(filteredValue, row, 1);
            }
        }
    }

    /**
     * Configures the list of parameters.
     *
     * @param parameters List of parameters. Each parameter is determined by its name, default value, and description
     * @since 0.2.0
     */
    public void setParameters(List<Triple<String, String, String>> parameters)
    {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        while (model.getRowCount() > 0) model.removeRow(0);

        Object[][] data = new Object[][]{{null, null, null}};
        model.setDataVector(data, HEADER);
        table.setEnabled(false);
        haveData = false;

        int numParameters = parameters.size();
        if (numParameters > 0)
        {
            model.removeRow(0);

            List<Triple<String, String, String>> sortedList = new LinkedList<Triple<String, String, String>>(parameters);
            Collections.sort(sortedList, PARAMETER_COMPARATOR);

            Iterator<Triple<String, String, String>> it = sortedList.iterator();
            while (it.hasNext())
            {
                Triple<String, String, String> aux = it.next();
                RunnableCodeType runnableCodeType = RunnableCodeType.find(aux.getSecond());
                if (runnableCodeType == null)
                {
                    String defaultValue = aux.getSecond().toLowerCase(Locale.getDefault());
                    if (defaultValue.startsWith("#select#"))
                    {
                        String auxOptions = aux.getSecond().replaceFirst("#select#", "").trim();
                        String[] options = StringUtils.split(auxOptions, ", ");
                        if (options.length > 0)
                        {
                            model.addRow(StringUtils.arrayOf(aux.getFirst(), options[0], aux.getThird()));
                            addComboCellEditor(options, model.getRowCount() - 1, 1);
                            continue;
                        }
                    } else if (defaultValue.startsWith("#boolean#"))
                    {
                        boolean isSelected = Boolean.parseBoolean(defaultValue.replaceFirst("#boolean#", "").trim());
                        model.addRow(StringUtils.arrayOf(aux.getFirst(), Boolean.toString(isSelected), aux.getThird()));
                        addCheckboxCellEditor(isSelected, model.getRowCount() - 1, 1);
                        continue;
                    } else if (defaultValue.startsWith("#file#"))
                    {
                        final String fileDefaultValue = aux.getSecond().replaceFirst("#file#", "").trim();

                        model.addRow(StringUtils.arrayOf(aux.getFirst(), "", aux.getThird()));
                        addFileChooserCellEditor(model.getRowCount() - 1, 1, fileDefaultValue);
                        continue;
                    } else if (defaultValue.startsWith("#files#"))
                    {
                        final String fileDefaultValue = aux.getSecond().replaceFirst("#files#", "").trim();

                        model.addRow(StringUtils.arrayOf(aux.getFirst(), "", aux.getThird()));
                        addFileMultiChooserCellEditor(model.getRowCount() - 1, 1, fileDefaultValue);
                        continue;
                    } else if (defaultValue.startsWith("#path#"))
                    {
                        final String fileDefaultValue = aux.getSecond().replaceFirst("#path#", "").trim();

                        model.addRow(StringUtils.arrayOf(aux.getFirst(), "", aux.getThird()));
                        addPathChooserCellEditor(model.getRowCount() - 1, 1, fileDefaultValue);
                        continue;
                    }

                    model.addRow(StringUtils.arrayOf(aux.getFirst(), aux.getSecond(), aux.getThird()));
                } else
                {
                    String runnableLabel = runnableCodeType.getRunnableLabel();
                    String fileName, algorithmName, algorithmParameters;
                    RunnableSelector runnable = new RunnableSelector(runnableLabel, "File", runnableCodeType.getRunnableClass(), ALGORITHM_DIRECTORY, new ParameterValueDescriptionPanel());
                    runnable.setPreferredSize(new Dimension(640, 480));

                    try
                    {
                        Triple<File, String, Class> aux1 = runnable.getRunnable();
                        fileName = aux1.getFirst().getCanonicalPath();
                        algorithmName = aux1.getSecond();
                        algorithmParameters = StringUtils.mapToString(runnable.getRunnableParameters(), "=", ", ");
                    } catch (Throwable e)
                    {
                        fileName = "";
                        algorithmName = "";
                        algorithmParameters = "";
                    }

                    model.addRow(StringUtils.arrayOf(aux.getFirst() + "_file", fileName, aux.getThird()));
                    model.addRow(StringUtils.arrayOf(aux.getFirst() + "_classname", algorithmName, runnableLabel + " class name"));
                    model.addRow(StringUtils.arrayOf(aux.getFirst() + "_parameters", algorithmParameters, runnableLabel + " parameters"));
                    algorithms.put(aux.getFirst() + "_file", Pair.of(runnable, model.getRowCount() - 3));

                    addActionCellEditor(model.getRowCount() - 3, 1);
                }
            }

            table.setEnabled(true);
            haveData = true;
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
            return String.class;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex)
        {
            if (columnIndex == 1)
            {
                int rowView = table.convertRowIndexToModel(rowIndex);
                int columnView = table.convertColumnIndexToView(columnIndex);
                TableCellEditor tce = table.getCellEditor(rowView, columnView);

                if (tce instanceof ActionTableCellEditor)
                {
                    return true;
                } else if (tce instanceof DefaultCellEditor)
                {
                    Component cellComponent = ((DefaultCellEditor) tce).getComponent();
                    if (cellComponent instanceof JComboBox || cellComponent instanceof JCheckBox)
                    {
                        return true;
                    } else
                    {
                        RunnableCodeType runnableCodeType = RunnableCodeType.find(getValueAt(rowIndex, 1).toString());
                        if (runnableCodeType == null) return true;
                    }
                }
            }

            return false;
        }

        @Override
        public void setValueAt(Object value, int row, int column)
        {
            super.setValueAt(value, row, column);
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
