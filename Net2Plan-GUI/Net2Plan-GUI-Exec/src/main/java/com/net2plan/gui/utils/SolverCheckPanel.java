package com.net2plan.gui.utils;

import com.jom.SolverTester;
import com.net2plan.interfaces.networkDesign.Configuration;
import com.net2plan.utils.Pair;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created by jorge on 2/03/17.
 */
public class SolverCheckPanel extends JPanel implements ActionListener
{
    private final JButton btn_launch;
    private final JTextArea txt_info;

    private enum Solvers
    {
        glpk, ipopt, cplex, xpress
    }

    private enum OS
    {
        windows, linux, macintosh, unknown
    }

    private final String NEW_LINE = "\n";
    private final String MESSAGE_HEADER = "MESSAGE: ";
    private final String WARNING_HEADER = "WARNING: ";
    private final String ERROR_HEADER = "ERROR: ";

    public SolverCheckPanel()
    {
        super();

        this.setLayout(new BorderLayout());

        this.btn_launch = new JButton("Check solvers");
        this.setFocusable(false);
        this.btn_launch.addActionListener(this);

        this.txt_info = new JTextArea();
        this.txt_info.setText("");


        this.add(btn_launch, BorderLayout.NORTH);
        this.add(new JScrollPane(txt_info), BorderLayout.CENTER);
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent)
    {
        // Previous steps

        // Clean window
        txt_info.setText("");

        // Do not allow to click again
        btn_launch.setEnabled(false);

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
                txt_info.append(ERROR_HEADER + "Tester shutting down..." + NEW_LINE);
                return;
        }

        txt_info.append(NEW_LINE);

        txt_info.append(MESSAGE_HEADER + "Checking current runtime environment..." + NEW_LINE);

        final String jnaDefaultPath = System.getProperty("jna.library.path");

        if (jnaDefaultPath != null)
        {
            txt_info.append(MESSAGE_HEADER + "Default JNA library path found at: " + jnaDefaultPath + NEW_LINE);
        } else
        {
            txt_info.append(WARNING_HEADER + "Default JNA library path is not currently defined..." + NEW_LINE);
        }

        final String javaDefaultPath = System.getProperty("java.library.path");

        if (javaDefaultPath != null)
        {
            txt_info.append(MESSAGE_HEADER + "Default JAVA library path found at: " + javaDefaultPath + NEW_LINE);
        } else
        {
            txt_info.append(WARNING_HEADER + "Default JAVA library path is not currently defined..." + NEW_LINE);
        }

        txt_info.append(NEW_LINE);

        txt_info.append(MESSAGE_HEADER + "Checking for current installed solvers..." + NEW_LINE);

        txt_info.append(NEW_LINE);

        // Checking solvers
        for (Solvers solvers : Solvers.values())
        {
            final String solverPath;
            final String message;
            switch (solvers)
            {
                case glpk:
                    txt_info.append(MESSAGE_HEADER + "Looking for solver: GLPK" + NEW_LINE);
                    solverPath = Configuration.getDefaultSolverLibraryName("glpk");
                    message = SolverTester.check_glpk(solverPath);
                    if (message.isEmpty())
                    {
                        txt_info.append(MESSAGE_HEADER + "Solver GLPK has been found at directory: " + solverPath + NEW_LINE);
                    } else
                    {
                        txt_info.append(WARNING_HEADER + "Solver GLPK could not be found at directory: " + solverPath + NEW_LINE);
                        txt_info.append(WARNING_HEADER + "JOM library has this to say: " + NEW_LINE);
                        txt_info.append(message + NEW_LINE);
                    }
                    break;
                case ipopt:
                    solverPath = Configuration.getDefaultSolverLibraryName("ipopt");
                    message = SolverTester.check_ipopt(solverPath);
                    if (message.isEmpty())
                        txt_info.append(MESSAGE_HEADER + "Solver IPOPT has been found at directory: " + solverPath + NEW_LINE);
                    break;
                case cplex:
                    message = SolverTester.check_cplex(Configuration.getDefaultSolverLibraryName("cplex"));
                    break;
                case xpress:
                    message = SolverTester.check_xpress(Configuration.getDefaultSolverLibraryName("xpress"));
                    break;
                default:
                    txt_info.append(ERROR_HEADER + "Unknown solver has been provided: " + solvers.name() + NEW_LINE);
                    txt_info.append(ERROR_HEADER + "The tester is trying to work with unknown solvers and cannot continue." + NEW_LINE);
                    txt_info.append(ERROR_HEADER + "Tester shutting down..." + NEW_LINE);
                    return;
            }
        }
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
