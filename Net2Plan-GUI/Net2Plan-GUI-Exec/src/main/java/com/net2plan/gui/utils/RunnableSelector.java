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

import com.net2plan.interfaces.networkDesign.Configuration;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.internal.ErrorHandling;
import com.net2plan.internal.IExternal;
import com.net2plan.internal.SystemUtils;
import com.net2plan.utils.ClassLoaderUtils;
import com.net2plan.utils.Pair;
import com.net2plan.utils.Triple;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

/**
 * This class construct a panel that can be used to load some runnable code
 * (i.e. algorithms) from {@code .class}/{@code .jar} files,
 * view description, and configure parameters.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
@SuppressWarnings("unchecked")
public class RunnableSelector extends JPanel {
    private final static Comparator<String> SORT_FQCN;
    private JButton load;
    private JComboBox algorithmSelector;
    private static JFileChooser fileChooser;
    private JTextField txt_file;
    private JTextArea txt_description;
    private ParameterValueDescriptionPanel parametersPanel;
    private String label;
    private Set<Class<? extends IExternal>> _classes;
    private Map<String, Class> implementations;
    private FileDrop fd;

    static {
        SORT_FQCN = new SortFQCN();
        try {
            String defaultRunnableCodePath = Configuration.getOption("defaultRunnableCodePath");

            if (!defaultRunnableCodePath.isEmpty()) {
                File file = new File(defaultRunnableCodePath);
                fileChooser = new JFileChooser(file);
            }
        } catch (Throwable e) {
            fileChooser = null;
        }
    }

    /**
     * Default constructor.
     *
     * @param label           Indicates the type of runnable code to be handled (i.e. 'Algorithm', 'Report'...)
     * @param labelForField   (Optional) label to be placed to the left of the "Load" field. If null, then will be equal to {@code label}
     * @param _class          Admissible class for runnable code
     * @param currentFolder   Starting folder for the {@code JFileChooser}.
     * @param parametersPanel Reference to the panel where parameters can be modified
     * @since 0.2.0
     */
    public RunnableSelector(final String label, final String labelForField, final Class<? extends IExternal> _class, File currentFolder, final ParameterValueDescriptionPanel parametersPanel) {
        this(label, labelForField, new LinkedHashSet<Class<? extends IExternal>>() {
            {
                add(_class);
            }
        }, currentFolder, parametersPanel);
    }

    /**
     * Extends the default constructor to load code from more than one class.
     *
     * @param label           Indicates the type of runnable code to be handled (i.e. 'Algorithm', 'Report'...)
     * @param labelForField   (Optional) label to be placed to the left of the "Load" field. If null, then will be equal to {@code label}
     * @param _classes        Set of admissible classes for runnable code
     * @param currentFolder   Starting folder for the {@code JFileChooser}.
     * @param parametersPanel Reference to the panel where parameters can be modified
     * @since 0.2.0
     */
    public RunnableSelector(final String label, final String labelForField, final Set<Class<? extends IExternal>> _classes, final File currentFolder, final ParameterValueDescriptionPanel parametersPanel) {
        this.label = label;
        this.parametersPanel = parametersPanel;

        this._classes = new LinkedHashSet<Class<? extends IExternal>>(_classes);

        txt_description = new JTextArea();
        txt_description.setFont(new JLabel().getFont());
        txt_description.setLineWrap(true);
        txt_description.setWrapStyleWord(true);
        txt_description.setEditable(false);

        txt_file = new JTextField();
        txt_file.setEditable(false);
        algorithmSelector = new WiderJComboBox();
        algorithmSelector.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (algorithmSelector.getItemCount() == 0 || algorithmSelector.getSelectedIndex() == -1) return;

                try {
                    File fileName = new File(txt_file.getText());
                    String className = (String) ((StringLabeller) algorithmSelector.getSelectedItem()).getObject();

                    Class<? extends IExternal> _class = implementations.get(className);
                    IExternal instance = ClassLoaderUtils.getInstance(fileName, className, _class , null);

                    String aux_description = instance.getDescription() == null ? "No description" : instance.getDescription();
                    List<Triple<String, String, String>> aux_parameters = instance.getParameters() == null ? new LinkedList<>() : instance.getParameters();

                    ((Closeable) instance.getClass().getClassLoader()).close();

                    txt_description.setText(aux_description);
                    if (!txt_description.getText().isEmpty()) txt_description.setCaretPosition(0);

                    parametersPanel.setParameters(aux_parameters);
                } catch (Throwable ex) {
                    ex.printStackTrace();
                    ErrorHandling.showErrorDialog("Error selecting " + label.toLowerCase(getLocale()));
                }
            }
        });

        load = new JButton("Load");
        load.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    int rc = fileChooser.showOpenDialog(null);
                    if (rc != JFileChooser.APPROVE_OPTION) return;
                    loadImplementations(fileChooser.getSelectedFile());
                } catch (NoRunnableCodeFound ex) {
                    ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to load");
                } catch (Throwable ex) {
                    ErrorHandling.addErrorOrException(ex, RunnableSelector.class);
                    ErrorHandling.showErrorDialog("Error loading runnable code");
                }
            }
        });

        setLayout(new MigLayout("", "[][grow][]", "[][][][][grow]"));
        add(new JLabel(labelForField == null ? label : labelForField));
        add(txt_file, "growx");
        add(load, "wrap");
        add(algorithmSelector, "skip, growx, spanx 2, wrap, wmin 100");
        add(new JLabel("Description"), "top");
        add(new JScrollPane(txt_description), "height 100::, spanx 2, grow, wrap");
        add(new JLabel("Parameters"), "spanx 3, wrap");
        add(parametersPanel, "spanx 3, grow");

        checkFileChooser(currentFolder);
        getDefaults();

        fd = new FileDrop(this, new LineBorder(Color.BLACK), true, new FileDrop.Listener() {
            @Override
            public void filesDropped(File[] files) {
                for (File file : files) {
                    try {
                        if (!file.getName().toLowerCase(Locale.getDefault()).endsWith(".jar") && !file.getName().toLowerCase(Locale.getDefault()).endsWith(".class"))
                            return;

                        checkFileChooser(file.getParentFile());
                        loadImplementations(file);
                        break;
                    } catch (NoRunnableCodeFound ex) {
                        ErrorHandling.showErrorDialog(ex.getMessage(), "Unable to load");
                        break;
                    } catch (Throwable ex) {
                        ErrorHandling.addErrorOrException(ex, RunnableSelector.class);
                        ErrorHandling.showErrorDialog("Error loading runnable code");
                        break;
                    }
                }
            }
        });
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        algorithmSelector.setEnabled(enabled);
        load.setEnabled(enabled);
        parametersPanel.setEnabled(enabled);
        fd.setActive(enabled);
    }

    private void checkFileChooser(File currentFolder) {
        if (fileChooser == null) {
            fileChooser = currentFolder == null ? new JFileChooser() : new JFileChooser(currentFolder);
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            for (FileFilter aux : fileChooser.getChoosableFileFilters()) fileChooser.removeChoosableFileFilter(aux);
            fileChooser.addChoosableFileFilter(ClassLoaderUtils.getFileFilter());
            fileChooser.setAcceptAllFileFilterUsed(false);
        }
    }

    private void getDefaults() {
        try {
            String defaultRunnableCodePath = Configuration.getOption("defaultRunnableCodePath");
            File file = new File(defaultRunnableCodePath);

            if (!file.exists() || file.isDirectory()) return;

            if (file.getName().toLowerCase(Locale.getDefault()).endsWith(".jar") || file.getName().toLowerCase(Locale.getDefault()).endsWith(".class"))
                loadImplementations(file);
        } catch (Throwable e) {
        }
    }

    /**
     * Returns the information required to call a runnable code.
     *
     * @return Runnable information
     * @since 0.2.0
     */
    public Triple<File, String, Class> getRunnable() {
        String filename = txt_file.getText();
        if (filename.isEmpty() || algorithmSelector.getSelectedIndex() == -1) {
            throw new Net2PlanException(label + " must be selected");
        }

        String algorithm = (String) ((StringLabeller) algorithmSelector.getSelectedItem()).getObject();

        return Triple.of(new File(filename), algorithm, implementations.get(algorithm));
    }

    /**
     * Returns the parameters introduced by user.
     *
     * @return Key-value map
     * @since 0.2.0
     */
    public Map<String, String> getRunnableParameters() {
        return new LinkedHashMap<String, String>(parametersPanel.getParameters());
    }

    private void loadImplementations(File f) {
        try {
            if (!f.isAbsolute()) f = new File(SystemUtils.getCurrentDir(), f.getPath());

            Map<String, Class> aux_implementations = new TreeMap<String, Class>();
            List<Class<IExternal>> aux = ClassLoaderUtils.getClassesFromFile(f, IExternal.class , null);
            for (Class<IExternal> implementation : aux) {
                Iterator<Class<? extends IExternal>> it = _classes.iterator();

                while (it.hasNext()) {
                    Class<? extends IExternal> _class = it.next();

                    if (_class.isAssignableFrom(implementation)) {
                        aux_implementations.put(implementation.getName(), _class);
                        break;
                    }
                }
            }

            if (aux_implementations.isEmpty()) throw new NoRunnableCodeFound(f, _classes);

            implementations = aux_implementations;

            txt_file.setText(f.getCanonicalPath());
            txt_description.setText("");
            parametersPanel.reset();

            algorithmSelector.removeAllItems();

            Set<String> sortedSet = new TreeSet<String>(SORT_FQCN);
            sortedSet.addAll(aux_implementations.keySet());

            ActionListener[] listeners = algorithmSelector.getActionListeners();
            for (ActionListener listener : listeners) algorithmSelector.removeActionListener(listener);

            for (String implementation : sortedSet) {
                Pair<String, String> aux1 = ClassLoaderUtils.getPackageAndClassName(implementation);

                String implementationLabel = aux1.getSecond();
                String packageName = aux1.getFirst();
                if (!packageName.isEmpty()) implementationLabel += " (" + packageName + ")";
                algorithmSelector.addItem(StringLabeller.unmodifiableOf(implementation, implementationLabel));

            }

            if (algorithmSelector.getItemCount() > 1) algorithmSelector.setSelectedIndex(-1);
            for (ActionListener listener : listeners) algorithmSelector.addActionListener(listener);
            if (algorithmSelector.getItemCount() == 1) algorithmSelector.setSelectedIndex(0);
        } catch (NoRunnableCodeFound e) {
            throw (e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Resets the component.
     *
     * @since 0.2.0
     */
    public void reset() {
        algorithmSelector.removeAllItems();
        txt_file.setText("");
        txt_description.setText("");
        parametersPanel.reset();

        getDefaults();
    }

    private static class SortFQCN implements Comparator<String> {
        @Override
        public int compare(String fqcn1, String fqcn2) {
            Pair<String, String> aux1 = ClassLoaderUtils.getPackageAndClassName(fqcn1);
            Pair<String, String> aux2 = ClassLoaderUtils.getPackageAndClassName(fqcn2);

            int compare1 = aux1.getSecond().compareTo(aux2.getSecond());
            if (compare1 != 0) return compare1;

            return aux1.getFirst().compareTo(aux2.getFirst());
        }
    }
}
