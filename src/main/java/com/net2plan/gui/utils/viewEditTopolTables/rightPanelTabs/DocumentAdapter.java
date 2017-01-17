package com.net2plan.gui.utils.viewEditTopolTables.rightPanelTabs;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import com.net2plan.gui.utils.IVisualizationCallback;

abstract class DocumentAdapter implements DocumentListener 
{
	private final IVisualizationCallback networkViewer;
	
	public DocumentAdapter(IVisualizationCallback networkViewer) { this.networkViewer = networkViewer; }
	
    @Override
    public void changedUpdate(DocumentEvent e) {
        processEvent(e);
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
        processEvent(e);
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        processEvent(e);
    }

    private void processEvent(DocumentEvent e) {
        if (!networkViewer.getVisualizationState().isNetPlanEditable()) return;

        Document doc = e.getDocument();
        try {
            updateInfo(doc.getText(0, doc.getLength()));
        } catch (BadLocationException ex) {
        }
    }

    protected abstract void updateInfo(String text);
}
