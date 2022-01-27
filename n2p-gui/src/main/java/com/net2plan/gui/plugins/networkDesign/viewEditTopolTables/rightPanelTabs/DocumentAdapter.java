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
package com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.rightPanelTabs;


import com.net2plan.gui.plugins.GUINetworkDesign;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

abstract class DocumentAdapter implements DocumentListener 
{
	private final GUINetworkDesign networkViewer;
	
	public DocumentAdapter(GUINetworkDesign networkViewer) { this.networkViewer = networkViewer; }
	
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
