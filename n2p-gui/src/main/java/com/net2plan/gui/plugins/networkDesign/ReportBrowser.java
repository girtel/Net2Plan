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


package com.net2plan.gui.plugins.networkDesign;

import com.net2plan.gui.utils.FileChooserConfirmOverwrite;
import com.net2plan.internal.CustomHTMLEditorKit;
import com.net2plan.internal.CustomHTMLEditorKit.CustomJEditorPane;
import com.net2plan.internal.ErrorHandling;
import com.net2plan.utils.HTMLUtils;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.URI;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import javax.swing.text.Document;
import javax.swing.text.html.StyleSheet;

/**
 * Class to show HTML files with images in a panel.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public class ReportBrowser extends JPanel {
    private final JEditorPane editor;
    private final CustomHTMLEditorKit htmlEditorKit;

    /**
     * Default constructor.
     *
     * @param html HTML to be shown (version 3.2 compatible, no Javascript)
     */
    public ReportBrowser(String html) {
        editor = new CustomJEditorPane();
        editor.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);

        editor.setContentType("text/html;charset=UTF-8");
        editor.setEditable(false);
        
	/* Configure HTML viewer */
        htmlEditorKit = new CustomHTMLEditorKit();
        editor.setEditorKit(htmlEditorKit);
                
        StyleSheet styleSheet = htmlEditorKit.getStyleSheet();
        styleSheet.addRule("body {padding: 5px 5px 5px 5px;");  
        styleSheet.addRule("p, ul, ol, table {font-family: Tahoma, Verdana, Segoe, sans-serif; font-size: 11px; "
                + "font-style: normal; font-variant: normal; text-align: justify;}");
        styleSheet.addRule("h1, h2, h3 {font-family: Tahoma, Verdana, Segoe, sans-serif; font-style: normal; "
                + "font-variant: normal; font-weight: 500;}");
	styleSheet.addRule("h1 {font-size: 16px; line-height: 20px;}");
	styleSheet.addRule("h2 {font-size: 14px; line-height: 16px;}");
	styleSheet.addRule("h3 {font-size: 12px; line-height: 12px;}");
        styleSheet.addRule("table, td {font-size: 10px; text-align: justify;}");
        styleSheet.addRule("table, tr, th {border: 0px;}");
        styleSheet.addRule("td {border: 2px solid #ccccff;}");
        styleSheet.addRule("th {background-color: #ccccff; text-align: center; height: 10px; padding: 5px;}");
        styleSheet.addRule("caption {caption-side: bottom; padding-bottom: 15px; font-weight: bold;}");
        
        Document doc = htmlEditorKit.createDefaultDocument();
        doc.putProperty("IgnoreCharsetDirective", true);
        editor.setDocument(doc);
                
        editor.setText(html);
        ((DefaultCaret) editor.getCaret()).setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
        editor.addHyperlinkListener(new HyperlinkListener() {
            @Override
            public void hyperlinkUpdate(HyperlinkEvent e) {
                try {
                    if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                        String href = e.getDescription();
                        if (href.toLowerCase(Locale.getDefault()).startsWith("http")) HTMLUtils.browse(new URI(href));
                        else editor.scrollToReference(href.substring(1));
                    }
                } catch (Throwable ex) {
                    ErrorHandling.addErrorOrException(ex, ReportBrowser.class);
                    ErrorHandling.showErrorDialog("Please, check console for more information", "Error exporting report");
                }
            }
        });
        
		/* Generate toolbar */
        JToolBar buttonBar = new JToolBar();
        JButton viewInNavigator = new JButton("View in navigator");
        viewInNavigator.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                try {
                    File htmlName = File.createTempFile("tmp_", ".html");
                    saveToFile(htmlName);
                    HTMLUtils.browse(htmlName.toURI());
                } catch (Throwable ex) {
                    ErrorHandling.addErrorOrException(ex, ReportBrowser.class);
                    ErrorHandling.showErrorDialog("Please, check console for more information", "Error exporting report");
                }
            }
        });

        JButton saveToFile = new JButton("Save to file");
        saveToFile.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                try {
                    JFileChooser fc = new FileChooserConfirmOverwrite();
                    for (FileFilter existingFilter : fc.getChoosableFileFilters())
                        fc.removeChoosableFileFilter(existingFilter);
                    fc.setFileFilter(new FileNameExtensionFilter("HTML files", "html"));
                    fc.setAcceptAllFileFilterUsed(false);
                    fc.setFileSelectionMode(JFileChooser.FILES_ONLY);

                    int rc = fc.showSaveDialog(null);
                    if (rc != JFileChooser.APPROVE_OPTION) return;

                    File htmlName = fc.getSelectedFile();
                    saveToFile(htmlName);
                    ErrorHandling.showInformationDialog("Report saved successfully", "Save report to file");
                } catch (Throwable ex) {
                    ErrorHandling.addErrorOrException(ex, ReportBrowser.class);
                    ErrorHandling.showErrorDialog("Please, check console for more information", "Error exporting report");
                }
            }
        });

        buttonBar.setRollover(true);
        buttonBar.setFloatable(false);
        buttonBar.add(viewInNavigator);
        buttonBar.add(saveToFile);

        setLayout(new BorderLayout());
        add(buttonBar, BorderLayout.NORTH);
        add(new JScrollPane(editor), BorderLayout.CENTER);
    }

    private void saveToFile(File file) {
        String html = CustomHTMLEditorKit.includeNet2PlanHeader(editor.getText());
        html = CustomHTMLEditorKit.includeStyle(html);
        JEditorPane editor1 = new CustomJEditorPane();
        editor1.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
        CustomHTMLEditorKit htmlEditorKit1 = new CustomHTMLEditorKit();
        editor1.setEditorKit(htmlEditorKit1);
        editor1.setContentType("text/html");
        editor1.setEditable(false);
        editor1.setText(html);

        Map<String, Image> path2Image = htmlEditorKit.getImages();
        Map<String, Image> aux = htmlEditorKit1.getImages();
        for (Entry<String, Image> entry : aux.entrySet()) {
            String path = entry.getKey();
            if (!path.endsWith("/resources/common/reportHeader.png")) continue;

            path2Image.put(path, entry.getValue());
        }

        CustomHTMLEditorKit.saveToFile(file, html, path2Image);
    }
}
