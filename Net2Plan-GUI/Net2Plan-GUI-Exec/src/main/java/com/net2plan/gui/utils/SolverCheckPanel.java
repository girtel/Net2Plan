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

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.jom.OptimizationProblem.JOMSolver;
import com.jom.SolverTester;
import com.net2plan.interfaces.networkDesign.Configuration;
import com.net2plan.utils.Pair;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Jorge San Emeterio Villalain
 * @date 2/03/17
 */
public class SolverCheckPanel extends JPanel implements ActionListener
{
    private final JPanel pn_text;
    private final JPanel stackPanel; // Stacks up the dialogs for saving solver paths.
    private final JToolBar tb_buttons;
    private final JTextArea txt_info;

    private enum OS
    {
        windows, linux, macintosh, unknown
    }

    private final String NEW_LINE = "\n";
    private final String MESSAGE_HEADER = "MESSAGE: ";
    private final String WARNING_HEADER = "WARNING: ";
    private final String ERROR_HEADER = "ERROR: ";

    private OS currentOS;

    private boolean isJNAPathSet;
    private boolean isJAVAPathSet;
    private boolean isLinuxPathSet;

    private String JNAPath;
    private String JAVAPath;
    private String linuxPath;

    public SolverCheckPanel()
    {
        super();

        this.setLayout(new BorderLayout());

        this.stackPanel = new JPanel();
        this.stackPanel.setLayout(new BoxLayout(stackPanel, BoxLayout.PAGE_AXIS));

        this.tb_buttons = new JToolBar(JToolBar.VERTICAL);
        this.tb_buttons.setFloatable(false);
        this.tb_buttons.setFocusable(false);
        this.tb_buttons.setBorderPainted(false);
        this.tb_buttons.setRollover(false);

        // Adding as many buttons as solvers there are.
        for (JOMSolver solver : JOMSolver.values())
        {
            final JButton btn = new JButton("Check " + solver.name());
            btn.setFocusable(false);
            btn.addActionListener(this);

            tb_buttons.add(btn);
        }

        // Add check all
        JButton btn_checkAll = new JButton("Check all");
        btn_checkAll.setFocusable(false);
        btn_checkAll.addActionListener(this);
        this.tb_buttons.add(btn_checkAll);

        this.txt_info = new JTextArea();
        this.txt_info.setText("");

        // Building main window
        this.pn_text = new JPanel(new BorderLayout());
        this.pn_text.add(new JScrollPane(txt_info), BorderLayout.CENTER);
        this.pn_text.add(stackPanel, BorderLayout.SOUTH);

        this.add(tb_buttons, BorderLayout.EAST);
        this.add(pn_text, BorderLayout.CENTER);
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent)
    {
        try
        {
            // *Previous steps*

            // Clean window
            txt_info.setText("");

            // Do not allow to click again
            tb_buttons.setEnabled(false);

            // Cleaning old dialogs
            stackPanel.removeAll();
            stackPanel.setVisible(false);

            // Check for JOM version
            final String jomVersion = SolverTester.class.getPackage().getImplementationVersion();

            if (jomVersion != null)
            {
                final List<String> splitVersion = Lists.newArrayList(Splitter.on(".").split(jomVersion));

                if (Integer.parseInt(splitVersion.get(1)) < 2 || Integer.parseInt(splitVersion.get(3)) < 2)
                {
                    txt_info.append(ERROR_HEADER + "JOM library version is below 0.2.0.2." + NEW_LINE + "Please update your library to continue, current version is: " + jomVersion + NEW_LINE);
                    return;
                }
            } else
            {
                txt_info.append(WARNING_HEADER + "JOM library version could not be detected. Correct functioning is not guaranteed." + NEW_LINE);
                txt_info.append(NEW_LINE);
            }

            // Getting OS
            final Pair<OS, String> foundOS = getOS();
            final OS currentOS = foundOS.getFirst();
            final String OSName = foundOS.getSecond();

            txt_info.append(MESSAGE_HEADER + "Checking for current operating system..." + NEW_LINE);

            switch (currentOS)
            {
                case windows:
                    txt_info.append(MESSAGE_HEADER + "Found Windows operating system: " + OSName + NEW_LINE);
                    break;
                case linux:
                    txt_info.append(MESSAGE_HEADER + "Found Linux operating system: " + OSName + NEW_LINE);
                    break;
                case macintosh:
                    txt_info.append(MESSAGE_HEADER + "Found Macintosh operating system: " + OSName + NEW_LINE);
                    break;
                case unknown:
                default:
                    txt_info.append(ERROR_HEADER + "Found an unknown operating system." + NEW_LINE);
                    txt_info.append(ERROR_HEADER + "The tester cannot continue without knowing the operating system it is working on." + NEW_LINE);
                    throw new RuntimeException("Unknown operating system: " + OSName);
            }

            this.currentOS = currentOS;

            txt_info.append(NEW_LINE);

            // Checking for VM variables
            txt_info.append(MESSAGE_HEADER + "Checking current runtime environment..." + NEW_LINE);

            // JNA
            final String jnaDefaultPath = System.getProperty("jna.library.path");

            if (jnaDefaultPath != null)
            {
                txt_info.append(MESSAGE_HEADER + "Default JNA library path set to: " + jnaDefaultPath + NEW_LINE);
                isJNAPathSet = true;
                JNAPath = jnaDefaultPath;
            } else
            {
                txt_info.append(WARNING_HEADER + "Default JNA library path is not currently defined..." + NEW_LINE);
                isJNAPathSet = false;
                JNAPath = null;
            }

            // JAVA
            final String javaDefaultPath = System.getProperty("java.library.path");

            if (javaDefaultPath != null)
            {
                txt_info.append(MESSAGE_HEADER + "Default JAVA library path set to: " + javaDefaultPath + NEW_LINE);
                isJAVAPathSet = true;
                JAVAPath = javaDefaultPath;
            } else
            {
                txt_info.append(WARNING_HEADER + "Default JAVA library path is not currently defined..." + NEW_LINE);
                isJAVAPathSet = false;
                JAVAPath = null;
            }

            // LINUX
            if (currentOS == OS.linux)
            {
                final String linuxDefaultPath = System.getenv("LD_LIBRARY_PATH");
                if (linuxDefaultPath != null)
                {
                    txt_info.append(MESSAGE_HEADER + "Default Linux library path set to: " + linuxDefaultPath + NEW_LINE);
                    isLinuxPathSet = true;
                    linuxPath = linuxDefaultPath;
                } else
                {
                    txt_info.append(WARNING_HEADER + "Default Linux library path is not currently defined..." + NEW_LINE);
                    isLinuxPathSet = false;
                }
            } else
            {
                isLinuxPathSet = false;
            }

            // Calculating selected solvers
            final List<JOMSolver> selectedSolvers = new ArrayList<>();

            // The solver is found in the name of the clicked button.
            final JButton src = (JButton) actionEvent.getSource();
            final String selectedSolverName = src.getText().replace("Check ", "").trim();

            try
            {
                // Enum from string
                final JOMSolver selectedSolver = JOMSolver.valueOf(selectedSolverName);
                selectedSolvers.add(selectedSolver);
            } catch (IllegalArgumentException e)
            {
                // If the selected choice is not on the enum, select all of them...
                selectedSolvers.addAll(Arrays.asList(JOMSolver.values()));
            }

            // Could not find solvers...
            if (selectedSolvers.isEmpty())
            {
                txt_info.append(ERROR_HEADER + "Internal problem: no solver was selected for testing." + NEW_LINE);
                throw new RuntimeException("Could not find a solver for testing. Meaning that the provided solver is unknown or built poorly.");
            }

            // Running tester...
            txt_info.append(NEW_LINE);

            txt_info.append(MESSAGE_HEADER + "Checking for solvers: " + Arrays.toString(selectedSolvers.toArray()) + NEW_LINE);

            txt_info.append(NEW_LINE);

            // Checking solvers individually
            for (JOMSolver solvers : selectedSolvers)
            {
                switch (solvers)
                {
                    case glpk:
                        checkForSolver(JOMSolver.glpk);
                        break;
                    case ipopt:
                        checkForSolver(JOMSolver.ipopt);
                        break;
                    case cplex:
                        checkForSolver(JOMSolver.cplex);
                        break;
                    case xpress:
                        checkForSolver(JOMSolver.xpress);
                        break;
                    default:
                        txt_info.append(ERROR_HEADER + "Unknown solver has been provided: " + solvers.name() + NEW_LINE);
                        txt_info.append(ERROR_HEADER + "The tester is trying to work with unknown solvers and cannot continue." + NEW_LINE);
                        throw new RuntimeException("Unknown solver was provided: " + solvers.name());
                }
            }

        } catch (Exception ex)
        {
            txt_info.append(ERROR_HEADER + "An error has been found while running the solver tester..." + NEW_LINE);
            txt_info.append(ERROR_HEADER + "Check the console for more information." + NEW_LINE);
            txt_info.append(ERROR_HEADER + "Tester shutting down..." + NEW_LINE);
            ex.printStackTrace();
        } finally
        {
            tb_buttons.setEnabled(true);
        }
    }

    private void checkForSolver(JOMSolver solver)
    {
        final String solverPath;
        final String solverName = solver.name();
        final String solverNameUppercase = solverName.toUpperCase();

        String message;

        txt_info.append(MESSAGE_HEADER + "Looking for solver: " + solverNameUppercase + NEW_LINE);
        solverPath = Configuration.getDefaultSolverLibraryName(solverName);

        // No path has been provided by the user.
        if (solverPath.isEmpty())
        {
            txt_info.append(WARNING_HEADER + "Directory for " + solverNameUppercase + " solver has been left blank. Using default path..." + NEW_LINE);
            checkSolverAtDefaultFolder(solver);
        } else
        {
            File dir = new File(solverPath);

            if (dir.isDirectory())
            {
                File[] files = dir.listFiles((file, name) -> name.toLowerCase().contains(solver.name()));

                if (files != null)
                {
                    for (File file : files)
                    {
                        // Checking at custom location.
                        message = callJOM(solver, file.getAbsolutePath());

                        if (message.isEmpty())
                        {
                            txt_info.append(MESSAGE_HEADER + "Solver " + solverNameUppercase + " has been found at directory: " + file.getAbsolutePath() + NEW_LINE);
                            txt_info.append(NEW_LINE);
                            showSaveDialog(solver, file.getAbsolutePath());
                            return;
                        } else
                        {
                            txt_info.append(WARNING_HEADER + "Solver " + solverNameUppercase + " could not be found at directory: " + file.getAbsolutePath() + NEW_LINE);
                        }
                    }
                }
            } else
            {
                message = callJOM(solver, dir.getAbsolutePath());

                if (message.isEmpty())
                {
                    txt_info.append(MESSAGE_HEADER + "Solver " + solverNameUppercase + " has been found at directory: " + dir.getAbsolutePath() + NEW_LINE);
                    txt_info.append(NEW_LINE);
                    showSaveDialog(solver, dir.getAbsolutePath());
                    return;
                }
            }

            txt_info.append(WARNING_HEADER + "Solver " + solver.name().toUpperCase() + " could not be found at directory: " + solverPath + NEW_LINE);

            txt_info.append(MESSAGE_HEADER + "Retrying..." + NEW_LINE);
            txt_info.append(MESSAGE_HEADER + "Trying to find solver at default location..." + NEW_LINE);
            checkSolverAtDefaultFolder(solver);
        }

        txt_info.append(NEW_LINE);
    }

    private void checkSolverAtDefaultFolder(JOMSolver solver)
    {
        String message;

        // Checking at JNA
        if (isJNAPathSet)
        {
            txt_info.append(MESSAGE_HEADER + "Checking for solver at JNA library path: " + JNAPath + NEW_LINE);

            final List<String> strings = splitPath(JNAPath);
            if (strings != null)
            {
                for (String separatedPath : strings)
                {
                    File dir = new File(separatedPath);
                    File[] files = dir.listFiles((file, name) -> name.toLowerCase().contains(solver.name()));

                    if (files != null)
                    {
                        for (File file : files)
                        {
                            message = callJOM(solver, file.getAbsolutePath());

                            if (message.isEmpty())
                            {
                                txt_info.append(MESSAGE_HEADER + "Solver " + solver.name().toUpperCase() + " has been found at directory: " + file.getAbsolutePath() + NEW_LINE);
                                showSaveDialog(solver, file.getAbsolutePath());
                                return;
                            } else
                            {
                                txt_info.append(WARNING_HEADER + "Solver " + solver.name().toUpperCase() + " could not be found at directory: " + file.getAbsolutePath() + NEW_LINE);
                            }
                        }
                    } else
                    {
                        txt_info.append(WARNING_HEADER + "Solver " + solver.name().toUpperCase() + " could not be found at directory: " + separatedPath + NEW_LINE);
                    }
                }
            }
        } else
        {
            txt_info.append(WARNING_HEADER + "JNA library path not set. Ignoring..." + NEW_LINE);
        }

        // Checking at JAVA
        if (isJAVAPathSet)
        {
            txt_info.append(MESSAGE_HEADER + "Checking for solver at JAVA library path: " + JAVAPath + NEW_LINE);

            final List<String> strings = splitPath(JAVAPath);
            if (strings != null)
            {
                for (String separatedPath : strings)
                {
                    File dir = new File(separatedPath);
                    File[] files = dir.listFiles((file, name) -> name.toLowerCase().contains(solver.name()));

                    if (files != null)
                    {
                        for (File file : files)
                        {
                            message = callJOM(solver, file.getAbsolutePath());

                            if (message.isEmpty())
                            {
                                txt_info.append(MESSAGE_HEADER + "Solver " + solver.name().toUpperCase() + " has been found at directory: " + file.getAbsolutePath() + NEW_LINE);
                                showSaveDialog(solver, file.getAbsolutePath());
                                return;
                            } else
                            {
                                txt_info.append(WARNING_HEADER + "Solver " + solver.name().toUpperCase() + " could not be found at directory: " + file.getAbsolutePath() + NEW_LINE);
                            }
                        }
                    } else
                    {
                        txt_info.append(WARNING_HEADER + "Solver " + solver.name().toUpperCase() + " could not be found at directory: " + separatedPath + NEW_LINE);
                    }
                }
            } else
            {
                throw new RuntimeException("Internal: String not properly split.");
            }
        } else
        {
            txt_info.append(WARNING_HEADER + "JAVA library path not set. Ignoring..." + NEW_LINE);
        }

        // Checking Linux
        if (currentOS == OS.linux)
        {
            if (isLinuxPathSet)
            {
                txt_info.append(MESSAGE_HEADER + "Checking for solver at Linux library path: " + linuxPath + NEW_LINE);

                final List<String> strings = splitPath(linuxPath);
                if (strings != null)
                {
                    for (String separatedPath : strings)
                    {
                        File dir = new File(separatedPath);
                        File[] files = dir.listFiles((file, name) -> name.toLowerCase().contains(solver.name()));

                        if (files != null)
                        {
                            for (File file : files)
                            {
                                message = callJOM(solver, file.getAbsolutePath());

                                if (message.isEmpty())
                                {
                                    txt_info.append(MESSAGE_HEADER + "Solver " + solver.name().toUpperCase() + " has been found at directory: " + file.getAbsolutePath() + NEW_LINE);
                                    showSaveDialog(solver, file.getAbsolutePath());
                                    return;
                                } else
                                {
                                    txt_info.append(WARNING_HEADER + "Solver " + solver.name().toUpperCase() + " could not be found at directory: " + file.getAbsolutePath() + NEW_LINE);
                                }
                            }
                        } else
                        {
                            txt_info.append(WARNING_HEADER + "Solver " + solver.name().toUpperCase() + " could not be found at directory: " + separatedPath + NEW_LINE);
                        }
                    }
                } else
                {
                    throw new RuntimeException("Internal: String not properly split.");
                }
            } else
            {
                txt_info.append(WARNING_HEADER + "Linux library path not set. Ignoring..." + NEW_LINE);
            }
        }

        txt_info.append(MESSAGE_HEADER + "Checking for solver by using system defaults..." + NEW_LINE);

        // Checking without giving a path
        final String solverDefaultPath;
        switch (currentOS)
        {
            case windows:
                solverDefaultPath = solver.name() + ".dll";
                message = callJOM(solver, solverDefaultPath);
                break;
            case linux:
            case macintosh:
                solverDefaultPath = "lib" + solver.name();
                message = callJOM(solver, solverDefaultPath);
                break;
            default:
            case unknown:
                throw new RuntimeException("Unknown OS, cannot proceed...");
        }

        if (message.isEmpty())
        {
            txt_info.append(MESSAGE_HEADER + "Solver " + solver.name().toUpperCase() + " has been found at: " + solverDefaultPath + NEW_LINE);
            showSaveDialog(solver, solverDefaultPath);
        } else
        {
            txt_info.append(ERROR_HEADER + "Solver " + solver.name().toUpperCase() + " could not be found in the system." + NEW_LINE);
            txt_info.append(NEW_LINE);
            txt_info.append(MESSAGE_HEADER + " * Check that the solver has been correctly installed on your system." + NEW_LINE);
            txt_info.append(MESSAGE_HEADER + " * If the solver has been installed in a custom path, make sure that Net2Plan's configuration is correctly pointing to the solver's binary file." + NEW_LINE);
            if (currentOS == OS.windows)
            {
                txt_info.append(MESSAGE_HEADER + " * Under windows, try installing your solver dll file under: c:/Windows/system32" + NEW_LINE);
            } else if (currentOS == OS.linux)
            {
                txt_info.append(MESSAGE_HEADER + " * Under linux, try installing the solver using your distribution package manager." + NEW_LINE);
            }
        }
    }

    private String callJOM(JOMSolver solver, String path)
    {
        String message;

        switch (solver)
        {
            case glpk:
                message = SolverTester.check_glpk(path);
                break;
            case ipopt:
                message = SolverTester.check_ipopt(path);
                break;
            case cplex:
                message = SolverTester.check_cplex(path);
                break;
            case xpress:
                message = SolverTester.check_xpress(path);
                break;
            default:
                throw new RuntimeException("Unknown solver. Cannot proceed...");
        }

        return message;
    }

    private void showSaveDialog(final JOMSolver solver, final String path)
    {
        // Do not show if the new path is already on the configuration screen
        if (Configuration.getDefaultSolverLibraryName(solver.name()).equals(path)) return;

        // Container
        final JPanel pn_saveConfirm = new JPanel(new BorderLayout());

        // Buttons
        final JButton btn_accept, btn_refuse;

        btn_accept = new JButton("Save");
        btn_accept.setFocusable(false);

        btn_refuse = new JButton("Cancel");
        btn_refuse.setFocusable(false);

        btn_accept.addActionListener(e ->
        {
            savePathToConfiguration(solver, path);
            pn_saveConfirm.setVisible(false);
            pn_text.remove(pn_saveConfirm);
        });

        btn_refuse.addActionListener(e ->
        {
            pn_saveConfirm.setVisible(false);
            pn_text.remove(pn_saveConfirm);
        });

        pn_saveConfirm.add(new JLabel("New path has been found for solver: " + solver.name() + ". Save it under configuration?: "), BorderLayout.CENTER);

        final JPanel buttonPanel = new JPanel(new GridBagLayout());
        buttonPanel.add(btn_accept);
        buttonPanel.add(btn_refuse);

        // Adding to main panel
        pn_saveConfirm.add(buttonPanel, BorderLayout.EAST);
        stackPanel.add(pn_saveConfirm);
        stackPanel.setVisible(true);
    }

    private void savePathToConfiguration(final JOMSolver solver, final String path)
    {
        Configuration.setDefaultSolverLibraryName(solver.name(), path);
        Configuration.saveOptions();
    }

    private List<String> splitPath(final String path)
    {
        // TODO: Check for windows...
        if (currentOS == OS.linux || currentOS == OS.macintosh)
        {
            final String[] ideSplit = path.split("::");

            final List<String> separatedPaths = new ArrayList<>();
            for (String s : ideSplit)
            {
                final String[] aux = s.split(":");

                Collections.addAll(separatedPaths, aux);
            }

            return separatedPaths;
        } else if (currentOS == OS.windows)
        {
            final String[] ideSplit = path.split(";;");

            final List<String> separatedPaths = new ArrayList<>();
            for (String s : ideSplit)
            {
                final String[] aux = s.split(";");

                Collections.addAll(separatedPaths, aux);
            }

            return separatedPaths;
        }

        return null;
    }

    private static Pair<OS, String> getOS()
    {
        final String osName = System.getProperty("os.name");
        final String osNameLowerCase = osName.toLowerCase();

        if (osNameLowerCase.startsWith("windows"))
        {
            return Pair.of(OS.windows, osName);
        } else if (osNameLowerCase.startsWith("linux"))
        {
            return Pair.of(OS.linux, osName);
        } else if (osNameLowerCase.startsWith("mac"))
        {
            return Pair.of(OS.macintosh, osName);
        } else
        {
            return Pair.of(OS.unknown, "");
        }
    }
}
