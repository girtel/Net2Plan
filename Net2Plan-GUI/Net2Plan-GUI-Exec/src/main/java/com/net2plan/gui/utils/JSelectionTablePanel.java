package com.net2plan.gui.utils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;
import java.util.Vector;

/**
 * <p>{@link JPanel} which contains two {@link JTable} to allow selecting elements.</p>
 *
 * @author Cesar San-Nicolas-Martinez, Elena Martin-Seoane
 * @since 1.7
 */

public class JSelectionTablePanel extends JPanel implements ActionListener
{

    private JTable	candidateTable, selectedTable;
    private JButton	addButton, removeButton, removeAllButton;
    private LinkedList<Object[]> candidateElements;

    /**
     * Constructor
     * @param header both table's header
     */
    public JSelectionTablePanel(String [] header, String elementTitle)
    {
        super(new GridBagLayout());

        candidateElements = new LinkedList<>();
        final JPanel sourceLabelPanel = new JPanel();
        final JLabel sourceLabel = new JLabel("Candidate "+elementTitle);
        sourceLabelPanel.add(sourceLabel);

        TableModel candidateModel = new NonEditableTableModel(header);
        TableModel selectedModel = new NonEditableTableModel(header);


        candidateTable = new JTable(candidateModel);
        JScrollPane candidateScroll = new JScrollPane(candidateTable);
        candidateScroll.setLayout(new FullScrollPaneLayout());
        selectedTable = new JTable(selectedModel);
        JScrollPane selectedScroll = new JScrollPane(selectedTable);
        selectedScroll.setLayout(new FullScrollPaneLayout());

        candidateTable.setFillsViewportHeight(true);
        selectedTable.setFillsViewportHeight(true);

        addButton = new JButton(">");
        addButton.setName("addButton");
        removeButton = new JButton("<");
        removeButton.setName("removeButton");
        removeAllButton = new JButton("<<");
        removeAllButton.setName("removeAllButton");
        addButton.addActionListener(this);
        removeButton.addActionListener(this);
        removeAllButton.addActionListener(this);
        removeButton.setEnabled(false);
        removeAllButton.setEnabled(false);

        final JPanel addPanel = new JPanel(new GridLayout(2, 1));
        addPanel.add(addButton);

        final JPanel removePanel = new JPanel(new GridLayout(2, 1));
        removePanel.add(removeButton);
        removePanel.add(removeAllButton);

        final JLabel destLabel = new JLabel("Selected "+elementTitle);

        this.add(sourceLabelPanel, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        this.add(candidateScroll, new GridBagConstraints(0, 1, 1, 5, .5, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        this.add(addPanel, new GridBagConstraints(1, 2, 1, 2, 0, .25, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        this.add(removePanel, new GridBagConstraints(1, 4, 1, 2, 0, .25, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
        this.add(destLabel, new GridBagConstraints(2, 0, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        this.add(selectedScroll, new GridBagConstraints(2, 1, 1, 5, .5, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));

    }

    @Override
    public void actionPerformed(ActionEvent e)
    {

        Object src = e.getSource();

        if (src == addButton)
        {
            final DefaultTableModel fromTableModel = (DefaultTableModel) candidateTable.getModel();
            final DefaultTableModel toTableModel = (DefaultTableModel) selectedTable.getModel();

            for (int rowIndex : candidateTable.getSelectedRows())
                toTableModel.addRow((Vector<?>) fromTableModel.getDataVector().get(rowIndex));

            int selectedRow = -1;
            while ((selectedRow = candidateTable.getSelectedRow()) != -1)
                fromTableModel.removeRow(selectedRow);


        } else if (src == removeButton)
        {
            final DefaultTableModel fromTableModel = (DefaultTableModel) selectedTable.getModel();
            final DefaultTableModel toTableModel = (DefaultTableModel) candidateTable.getModel();

            for (int rowIndex : selectedTable.getSelectedRows())
                toTableModel.addRow((Vector<?>) fromTableModel.getDataVector().get(rowIndex));

            int selectedRow = -1;
            while ((selectedRow = selectedTable.getSelectedRow()) != -1)
                fromTableModel.removeRow(selectedRow);


        } else if (src == removeAllButton)
        {
            final DefaultTableModel fromTableModel = (DefaultTableModel) selectedTable.getModel();
            final DefaultTableModel toTableModel = (DefaultTableModel) candidateTable.getModel();

            for (int rowIndex = 0; rowIndex < selectedTable.getRowCount(); rowIndex++)
                toTableModel.addRow((Vector<?>) fromTableModel.getDataVector().get(rowIndex));

            while ((selectedTable.getRowCount()) > 0)
                fromTableModel.removeRow(0);

        }

        setEnabledButtons();

    }

    /**
     * Sets new elements to select
     * @param elements elements to set
     */

    public void setCandidateElements(LinkedList<Object[]> elements)
    {
        this.resetTables();
        DefaultTableModel candidateModel = (DefaultTableModel)candidateTable.getModel();

        for(Object [] element : elements)
        {
            if(element.length != candidateModel.getColumnCount())
                throw new RuntimeException("Unsupported element length");

            candidateModel.addRow(element);
        }

        candidateElements.clear();
        candidateElements.addAll(elements);
    }

    public LinkedList<Object[]> getCandidateElements()
    {
        return candidateElements;
    }

    /**
     * Obtains the list of selected elements
     * @return List of selected elements
     */
    public LinkedList<Object[]> getSelectedElements()
    {
        DefaultTableModel selectionModel = (DefaultTableModel)selectedTable.getModel();
        LinkedList<Object[]> selectedElements = new LinkedList<>();

        for(int i = 0; i < selectionModel.getRowCount(); i++)
        {
            Object [] selectedElement = new Object[selectionModel.getColumnCount()];
            for(int j = 0; j < selectionModel.getColumnCount(); j++)
            {
                selectedElement[j] = selectionModel.getValueAt(i,j);
            }
            selectedElements.add(selectedElement);
        }

        return selectedElements;
    }

    /**
     * Enables or disables the add and remove buttons according to the remaining rows at the tables
     */
    private void setEnabledButtons()
    {
        addButton.setEnabled(candidateTable.getRowCount() > 0);

        final int selectedRowCount = selectedTable.getRowCount();
        removeButton.setEnabled(selectedRowCount > 0);
        removeAllButton.setEnabled(selectedRowCount > 0);
    }

    /**
     * Cleans the candidate and selected VNFD tables
     */
    private void resetTables()
    {
        final DefaultTableModel candidateModel = (DefaultTableModel) candidateTable.getModel();
        final DefaultTableModel selectedModel = (DefaultTableModel) selectedTable.getModel();
        while (candidateModel.getRowCount() > 0)
            candidateModel.removeRow(0);
        while (selectedModel.getRowCount() > 0)
            selectedModel.removeRow(0);
    }

    public JTable getCandidateTable()
    {
        return candidateTable;
    }

    public JTable getSelectedTable()
    {
        return selectedTable;
    }
}

