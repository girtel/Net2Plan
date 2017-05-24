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


package com.net2plan.gui;

import com.net2plan.gui.utils.AdvancedJTable;
import com.net2plan.gui.utils.ClassAwareTableModel;
import com.net2plan.gui.utils.ClassPathEditor;
import com.net2plan.gui.utils.ColumnFitAdapter;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.internal.Constants;
import com.net2plan.internal.ErrorHandling;
import com.net2plan.internal.SystemUtils;
import com.net2plan.internal.Version;
import com.net2plan.internal.plugins.IGUIModule;
import com.net2plan.internal.plugins.Plugin;
import com.net2plan.internal.plugins.PluginSystem;
import com.net2plan.utils.HTMLUtils;
import com.net2plan.utils.ImageUtils;
import com.net2plan.utils.StringUtils;
import com.net2plan.utils.SwingUtils;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.PrintStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Main class for the graphical user interface (GUI).
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
@SuppressWarnings("unchecked")
public class GUINet2Plan extends JFrame implements ActionListener {
    private final static String USERSGUIDEFILENAME = "usersGuide.pdf";
    private static GUINet2Plan instance;
    private Set<KeyStroke> usedKeyStrokes;
    private JPanel container;
    private JMenuBar menu;
    private JMenuItem exitItem, optionsItem, errorConsoleItem, classPathEditorItem, keyCombinationItem;
    private JMenuItem aboutItem, helpItem, javadocItem, javadocExamplesItem;
    private Map<JMenuItem, Object> itemObject;
    private IGUIModule runningModule;

    private static InputMap inputMap = new InputMap();
    private static ActionMap actionMap = new ActionMap();

    private final static WindowAdapter CLOSE_NET2PLAN;
    private final static String ABOUT_TEXT = "<html><p align='justify'>Welcome to "
            + "Net2Plan " + Version.getVersion() + ": an open-source multilayer network planner "
            + "and simulator.</p><br><p align='justify'>Net2Plan is a free and open-source "
            + "Java tool devoted to the planning, optimization and evaluation of "
            + "communication networks.</p><br><p align='justify'>For more information, please "
            + "visit Net2Plan website: http://www.net2plan.com</p><br>"
            + "<p>Map support provided by: © OpenStreetMap contributors (Provided under ODbL license: http://www.openstreetmap.org/copyright)</p></html>";

    static {
        CLOSE_NET2PLAN = new CloseNet2PlanListener();
    }

    /**
     * Default constructor.
     *
     * @since 0.2.0
     */
    private GUINet2Plan() {
    }

    /**
     * Adds Net2Plan's global key combinations to a given input and action map.
     * The added actions correspond to the ones that are found on the top menu at the main window.
     * Useful for adding these actions to new windows that are not contained within GUINet2Plan.
     * @param iMap Input window input map.
     * @param aMap Input window action map.
     */
    public static void addGlobalActions(InputMap iMap, ActionMap aMap)
    {
        assert inputMap != null;
        assert actionMap != null;

        if (inputMap.allKeys() == null) return;
        if (actionMap.allKeys() == null) return;

        for (KeyStroke keyStroke : inputMap.allKeys())
        {
            final Object o = inputMap.get(keyStroke);
            iMap.put(keyStroke, inputMap.get(keyStroke));
            aMap.put(o, actionMap.get(o));
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            if (!(e.getSource() instanceof JMenuItem)) throw new RuntimeException("Bad");

            JMenuItem item = (JMenuItem) e.getSource();
            if (item.equals(optionsItem)) {
                JDialog dialog = new GUIConfiguration();
                dialog.setVisible(true);
            } else if (item.equals(errorConsoleItem)) {
                ErrorHandling.showConsole();
            } else if (item.equals(classPathEditorItem)) {
                ClassPathEditor.showGUI();
            } else if (item.equals(keyCombinationItem)) {
                showKeyCombinations();
            } else if (item.equals(exitItem)) {
                askForClose();
            } else if (item.equals(helpItem)) {
                loadHelp();
            } else if (item.equals(javadocItem)) {
                loadJavadocLib();
            } else if (item.equals(javadocExamplesItem)) {
                loadExamples();
            } else {
                Object object = itemObject.get(item);

                if (this.runningModule != null)
                {
                    final int result = JOptionPane.showConfirmDialog(instance, "Are you sure you want to exit the tool?\nAll unsaved changes will be lost", "Exit?", JOptionPane.YES_NO_OPTION);

                    if (result == JOptionPane.YES_OPTION)
                    {
                        runningModule.stop();
                    } else if (result == JOptionPane.NO_OPTION || result == JOptionPane.CLOSED_OPTION)
                    {
                        return;
                    }
                }

                if (object != null) {
                    if (object instanceof Class) {
                        Object classInstance = ((Class) object).newInstance();

                        if (classInstance instanceof IGUIModule) {
                            IGUIModule module = (IGUIModule) classInstance;
                            module.start();

                            object = module;
                            this.runningModule = module;
                        }
                    } else
                    {
                        this.runningModule = null;
                    }

                    if (object instanceof JPanel) {
                        container.removeAll();
                        container.add((JPanel) object, "grow");
                        container.revalidate();
                        container.updateUI();
                    }
                }
            }
        } catch (Throwable ex) {
            ErrorHandling.addErrorOrException(ex, GUINet2Plan.class);
            ErrorHandling.showErrorDialog("Unable to execute option");
        }
    }

    private static void askForClose() {
        int result = JOptionPane.showConfirmDialog(null, "Are you sure you want to exit Net2Plan?", "Exit from Net2Plan", JOptionPane.YES_NO_OPTION);

        if (result == JOptionPane.YES_OPTION) System.exit(0);
    }

    private static JMenuItem getCurrentMenu(JMenuBar menubar, JMenu parent, String itemName) {
        int pos = itemName.indexOf('|');
        if (pos == -1) {
            if (parent == null) throw new RuntimeException("Bad");

            JMenuItem menuItem = new JMenuItem(itemName);
            parent.add(menuItem);

            return menuItem;
        } else {
            String parentName = itemName.substring(0, pos);
            JMenu new_parent = null;

            MenuElement[] children;
            if (menubar != null) children = menubar.getSubElements();
            else if (parent != null) children = parent.getSubElements();
            else throw new RuntimeException("Bad");

            for (MenuElement item : children) {
                if (!(item instanceof JMenu) || !((JMenu) item).getText().equalsIgnoreCase(parentName))
                    continue;

                new_parent = (JMenu) item;
                break;
            }

            if (new_parent == null) {
                new_parent = new JMenu(parentName);

                if (menubar != null) {
                    if (parentName.equals("Tools")) {
                        menubar.add(new_parent, 1);
                        new_parent.setMnemonic('T');
                    } else {
                        menubar.add(new_parent, menubar.getComponentCount() - 1);
                    }
                } else if (parent != null) {
                    parent.add(new_parent);
                } else {
                    throw new RuntimeException("Bad");
                }
            }

            String new_itemName = itemName.substring(pos + 1);

            return getCurrentMenu(null, new_parent, new_itemName);
        }
    }

    private static GUINet2Plan getInstance() {
        if (instance == null) instance = new GUINet2Plan();
        return instance;
    }

    private static void loadExamples() {
        try {
            HTMLUtils.browse(new File(SystemUtils.getCurrentDir() + SystemUtils.getDirectorySeparator() + "doc" + SystemUtils.getDirectorySeparator() + "javadoc" + SystemUtils.getDirectorySeparator() + "examples" + SystemUtils.getDirectorySeparator() + "index.html").toURI());
        } catch (Throwable ex) {
            ErrorHandling.addErrorOrException(ex, GUINet2Plan.class);
            ErrorHandling.showErrorDialog("Error showing Examples page");
        }
    }

    private static void loadHelp() {
        File helpFile = new File(SystemUtils.getCurrentDir() + SystemUtils.getDirectorySeparator() + "doc" + SystemUtils.getDirectorySeparator() + "help" + SystemUtils.getDirectorySeparator() + USERSGUIDEFILENAME);

        try {
            HTMLUtils.browse(helpFile.toURI());
        } catch (Throwable ex) {
            ErrorHandling.showErrorDialog("Unable to show the User's guide. Please, visit http://www.net2plan.com for the latest version", "Error showing User's guide");
        }
    }

    private static void loadJavadocLib() {
        File javadocFile = new File(SystemUtils.getCurrentDir() + SystemUtils.getDirectorySeparator() + "doc" + SystemUtils.getDirectorySeparator() + "javadoc" + SystemUtils.getDirectorySeparator() + "api" + SystemUtils.getDirectorySeparator() + "index.html");

        try {
            HTMLUtils.browse(javadocFile.toURI());
        } catch (Throwable ex) {
            ErrorHandling.showErrorDialog("Unable to show the Library API Javadoc. Please, visit http://www.net2plan.com for the latest version", "Error showing Library API Javadoc");
        }
    }

    /**
     * Refresh main menu with currently loaded GUI plugins.
     *
     * @since 0.3.1
     */
    public static void refreshMenu() {
        instance.usedKeyStrokes.clear();

        while (instance.menu.getComponentCount() > 2) instance.menu.remove(1);

        for (Class<? extends Plugin> plugin : PluginSystem.getPlugins(IGUIModule.class)) {
            try {
                IGUIModule pluginInstance = ((Class<? extends IGUIModule>) plugin).newInstance();
                String menuName = pluginInstance.getMenu();
                JMenuItem item = getCurrentMenu(instance.menu, null, menuName);
                item.addActionListener(instance);

                KeyStroke keystroke = pluginInstance.getKeyStroke();
                if (keystroke != null && !instance.usedKeyStrokes.contains(keystroke)) {
                    item.setAccelerator(keystroke);
                    instance.usedKeyStrokes.add(keystroke);
                }

                instance.itemObject.put(item, plugin);
            } catch (NoClassDefFoundError e) {
                throw new Net2PlanException("Class " + e.getMessage() + " cannot be found. A dependence for " + plugin.getSimpleName() + " is missing?");
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }

        instance.menu.revalidate();
    }

    private static JPanel showAbout() {
        final JPanel aboutPanel = new JPanel();

        ImageIcon image = new ImageIcon(ImageUtils.readImageFromURL(GUINet2Plan.class.getResource("/resources/gui/logo.png")));
        JLabel label = new JLabel("", image, JLabel.CENTER);

        aboutPanel.setLayout(new MigLayout("insets 0 0 0 0", "[grow]", "[grow][grow]"));
        aboutPanel.add(label, "alignx center, aligny bottom, wrap");
        aboutPanel.add(new JLabel(ABOUT_TEXT), "alignx center, aligny top");
        aboutPanel.setFocusable(true);
        aboutPanel.requestFocusInWindow();

        aboutPanel.addKeyListener(new KeyAdapter() {
            private final int[] sequence = new int[]{KeyEvent.VK_UP, KeyEvent.VK_UP, KeyEvent.VK_DOWN, KeyEvent.VK_DOWN, KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_A, KeyEvent.VK_B};
            private int currentButton = 0;

            @Override
            public void keyPressed(KeyEvent e) {
                int keyPressed = e.getKeyCode();

                if (keyPressed == sequence[currentButton]) {
                    currentButton++;

                    if (currentButton == sequence.length) {
                        ErrorHandling.setDebug(true);
                        aboutPanel.removeKeyListener(this);
                    }
                } else {
                    currentButton = 0;
                }
            }
        });

        return aboutPanel;
    }

    private void showKeyCombinations() {
        Component component = container.getComponent(0);
        if (!(component instanceof IGUIModule)) {
            ErrorHandling.showErrorDialog("No tool is active", "Unable to show key associations");
            return;
        }

        final JDialog dialog = new JDialog();
        dialog.setTitle("Key combinations");
        SwingUtils.configureCloseDialogOnEscape(dialog);
        dialog.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(new Dimension(500, 300));
        dialog.setLocationRelativeTo(null);
        dialog.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        dialog.setLayout(new MigLayout("fill, insets 0 0 0 0"));

        final String[] tableHeader = StringUtils.arrayOf("Key combination", "Action");

        DefaultTableModel model = new ClassAwareTableModel();
        model.setDataVector(new Object[1][tableHeader.length], tableHeader);

        AdvancedJTable table = new AdvancedJTable(model);
        JScrollPane scrollPane = new JScrollPane(table);
        dialog.add(scrollPane, "grow");

        RowSorter<TableModel> sorter = new TableRowSorter<TableModel>(model);
        table.setRowSorter(sorter);

        table.getTableHeader().addMouseListener(new ColumnFitAdapter());

        IGUIModule module = (IGUIModule) component;
        Map<String, KeyStroke> keyCombinations = module.getKeyCombinations();
        if (!keyCombinations.isEmpty()) {
            model.removeRow(0);

            for (Entry<String, KeyStroke> keyCombination : keyCombinations.entrySet()) {
                String description = keyCombination.getKey();
                KeyStroke keyStroke = keyCombination.getValue();
                model.addRow(StringUtils.arrayOf(description, keyStroke.toString().replaceAll(" pressed ", " ")));
            }
        }

        dialog.setVisible(true);
    }

    private void start() {
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setMinimumSize(new Dimension(800, 600));

        itemObject = new HashMap<>();

        URL iconURL = GUINet2Plan.class.getResource("/resources/gui/icon.png");
        ImageIcon icon = new ImageIcon(iconURL);
        setIconImage(icon.getImage());
        setTitle("Net2Plan");
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(CLOSE_NET2PLAN);

        getContentPane().setLayout(new MigLayout("insets 0 0 0 0", "[grow]", "[grow]"));
        container = new JPanel(new MigLayout("", "[]", "[]"));
        container.setBorder(new LineBorder(Color.BLACK));
        container.setLayout(new MigLayout("fill"));
        getContentPane().add(container, "grow");

		/* Create menu bar */
        menu = new JMenuBar();
        setJMenuBar(menu);

		/* File menu */
        JMenu file = new JMenu("File");
        file.setMnemonic('F');
        menu.add(file);

        optionsItem = new JMenuItem("Options");
        optionsItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.ALT_DOWN_MASK));
        optionsItem.addActionListener(this);
        addKeyCombination(optionsItem);
        file.add(optionsItem);

        classPathEditorItem = new JMenuItem("Classpath editor");
        classPathEditorItem.addActionListener(this);
        file.add(classPathEditorItem);

        errorConsoleItem = new JMenuItem("Show Java console");
        errorConsoleItem.addActionListener(this);
        errorConsoleItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F12, InputEvent.ALT_DOWN_MASK));
        addKeyCombination(errorConsoleItem);
        file.add(errorConsoleItem);

        exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(this);
        exitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F4, InputEvent.ALT_DOWN_MASK));
        addKeyCombination(exitItem);
        file.add(exitItem);

		/* Help menu */
        JMenu help = new JMenu("Help");
        help.setMnemonic('H');
        menu.add(help);

        aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(this);
        help.add(aboutItem);
        itemObject.put(aboutItem, showAbout());

        helpItem = new JMenuItem("User's guide");
        helpItem.addActionListener(this);
        helpItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, KeyEvent.VK_UNDEFINED));
        addKeyCombination(helpItem);
        help.add(helpItem);

        javadocItem = new JMenuItem("Library API Javadoc");
        javadocItem.addActionListener(this);
        help.add(javadocItem);

        javadocExamplesItem = new JMenuItem("Built-in Examples Javadoc");
        javadocExamplesItem.addActionListener(this);
        help.add(javadocExamplesItem);

        keyCombinationItem = new JMenuItem("Show tool key combinations");
        keyCombinationItem.addActionListener(this);
        keyCombinationItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_K, KeyEvent.ALT_DOWN_MASK));
        addKeyCombination(keyCombinationItem);
        help.add(keyCombinationItem);

        usedKeyStrokes = new LinkedHashSet<>();
        refreshMenu();

        container.add(showAbout(), "align center");
        container.revalidate();

        new JFileChooser(); /* Do not remove! It is used to avoid slow JFileChooser first-time loading once Net2Plan is shown to the user */

        setVisible(true);
    }

    /**
     * Adds a key combination to the registry of global actions.
     * Saves into the input map a copy of the keystrokes contained within the menu item and a call to the action listener of it.
     * @param menuItem Input menu item.
     */
    private void addKeyCombination(final JMenuItem menuItem)
    {
        assert menuItem != null;
        assert inputMap != null;
        assert actionMap != null;

        String itemMessage = menuItem.getText();
        final KeyStroke[] keyStrokes = menuItem.getRegisteredKeyStrokes();

        for (KeyStroke keyStroke : keyStrokes)
        {
            inputMap.put(keyStroke, itemMessage);
            actionMap.put(itemMessage, new AbstractAction()
            {
                @Override
                public void actionPerformed(ActionEvent actionEvent)
                {
                    menuItem.doClick();
                }
            });
        }
    }

    /**
     * <p>Main method</p>
     *
     * @param args Command-line parameters (unused)
     * @since 0.2.0
     */
    public static void main(String[] args) {
        SystemUtils.configureEnvironment(GUINet2Plan.class, Constants.UserInterface.GUI);

        PrintStream stdout = System.out;
        PrintStream stderr = System.err;

        try {
            PrintStream out = new PrintStream(ErrorHandling.STREAM, true, StandardCharsets.UTF_8.name());
            System.setOut(out);
            System.setErr(out);

            getInstance().start();
        } catch (Throwable ex) {
            System.setOut(stdout);
            System.setErr(stderr);

            System.out.println("Error loading the graphic environment. Please, try the command line interface.");
            if (!(ex instanceof Net2PlanException) || ErrorHandling.isDebugEnabled())
                ErrorHandling.printStackTrace(ex);

            if (ex instanceof Net2PlanException) System.out.println(ex.getMessage());
        }
    }

    private static class CloseNet2PlanListener extends WindowAdapter {
        @Override
        public void windowClosing(WindowEvent e) {
            askForClose();
        }
    }
}
