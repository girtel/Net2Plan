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

import com.jom.JOMException;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.internal.ErrorHandling;
import com.net2plan.utils.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * Class in charge of executing some tasks, using a dialog waiting for completion.
 * It allows to stop the execution of the task.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public class ThreadExecutionController {
    private final static ActionListener SHOW_CONSOLE;

    private Thread thread;
    private final ThreadExecutionController.IThreadExecutionHandler handler;

    static {
        SHOW_CONSOLE = new ShowConsole();
    }

    /**
     * Default constructor.
     *
     * @param handler Reference to the handler
     * @since 0.2.0
     */
    public ThreadExecutionController(ThreadExecutionController.IThreadExecutionHandler handler) {
        this.handler = handler;
    }

    /**
     * Executes the code into a separated thread.
     *
     * @since 0.2.0
     */
    public void execute() {
        final JDialog dialog;

        if (handler instanceof JComponent) {
            Container topLevel = ((JComponent) handler).getTopLevelAncestor();
            dialog = (topLevel instanceof Frame) ? new JDialog((Frame) topLevel) : new JDialog();
        } else {
            dialog = new JDialog();
        }

        dialog.setTitle("Executing algorithm (press stop to abort)");
        dialog.setLayout(new BorderLayout());
        dialog.setSize(new Dimension(200, 200));
        dialog.setLocationRelativeTo(null);
        dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        dialog.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);

        Container contentPane = dialog.getContentPane();
        if (contentPane instanceof JComponent)
            ((JComponent) contentPane).registerKeyboardAction(SHOW_CONSOLE, KeyStroke.getKeyStroke(KeyEvent.VK_F12, InputEvent.ALT_DOWN_MASK), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        final SwingWorker worker = new SwingWorkerCompletionWaiter(dialog, handler);

        JButton button = new JButton();
        button.setText("Stop");
        button.addActionListener(new StopExecution(worker));
        dialog.add(button, BorderLayout.SOUTH);
        dialog.pack();

        Object out;
        try {
            worker.execute();
            dialog.setVisible(true);

            if (worker.isCancelled()) return;

            out = worker.get();
        } catch (Throwable e) {
            out = e;
        }

        if (out instanceof Throwable) {
            out = ErrorHandling.getInternalThrowable((Throwable) out);

            if (out instanceof Net2PlanException) {
                if (ErrorHandling.isDebugEnabled()) ErrorHandling.printStackTrace((Throwable) out);
                ErrorHandling.showErrorDialog(((Throwable) out).getMessage(), "An error happened");
            } else if (out instanceof UnsatisfiedLinkError) {
                String newLine = StringUtils.getLineSeparator();
                StringBuilder msg = new StringBuilder();
                msg.append(((Throwable) out).getMessage());
                msg.append(newLine).append(newLine);
                msg.append("Possible causes:").append(newLine);
                msg.append("(1) .so in Linux, .dll in Windows or .dylib in Mac is not in the expected location, and/or").append(newLine);
                msg.append("(2) you are trying to load a 32-bit library using a 64-bit JVM, or viceversa (switch to a valid JVM)");

                ErrorHandling.showErrorDialog(msg.toString(), "Error loading dynamic library");
            } else if (out instanceof JOMException) {
                if (ErrorHandling.isDebugEnabled()) ErrorHandling.printStackTrace((Throwable) out);

                ErrorHandling.showErrorDialog(((Throwable) out).getMessage(), "Error executing JOM");
            } else if (out instanceof InterruptedException || out instanceof IllegalMonitorStateException) {
            } else {
                ErrorHandling.addErrorOrException(((Throwable) out), handler.getClass());
                handler.executionFailed(this);
            }
        } else {
            handler.executionFinished(this, out);
        }
    }

    /**
     * Interface for the handlers.
     *
     * @since 0.2.0
     */
    public interface IThreadExecutionHandler {
        /**
         * Executes the handler and returns an object.
         *
         * @param controller Reference to the controller
         * @return An object
         * @since 0.2.0
         */
        public Object execute(ThreadExecutionController controller);

        /**
         * Reports the end of execution.
         *
         * @param controller Reference to the controller
         * @param out        Object returned by the {@link #execute(ThreadExecutionController) execute} method
         * @since 0.2.0
         */
        public void executionFinished(ThreadExecutionController controller, Object out);

        /**
         * Reports the end of execution with errors.
         *
         * @param controller Reference to the controller
         * @since 0.2.0
         */
        public void executionFailed(ThreadExecutionController controller);
    }

    private class SwingWorkerCompletionWaiter extends SwingWorker implements PropertyChangeListener {
        private final JDialog dialog;
        private final ThreadExecutionController.IThreadExecutionHandler handler;

        public SwingWorkerCompletionWaiter(JDialog dialog, ThreadExecutionController.IThreadExecutionHandler handler) {
            super();

            this.dialog = dialog;
            this.handler = handler;
            addPropertyChangeListener(this);
        }

        @Override
        public void propertyChange(PropertyChangeEvent event) {
            if ("state".equals(event.getPropertyName()) && event.getNewValue() == SwingWorker.StateValue.DONE) {
                dialog.setVisible(false);
                dialog.dispose();
            }
        }

        @Override
        protected Object doInBackground() throws Exception {
            thread = Thread.currentThread();
            thread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread t, Throwable e) {
                }
            });

            Object out;
            try {
                out = handler.execute(ThreadExecutionController.this);
            } catch (Throwable e) {
                e.printStackTrace();
                out = e;
            }

            return out;
        }
    }

    private static class ShowConsole implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            ErrorHandling.showConsole();
        }
    }

    @SuppressWarnings("deprecation")
    private class StopExecution implements ActionListener {
        private final SwingWorker worker;

        public StopExecution(SwingWorker worker) {
            this.worker = worker;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                worker.cancel(true);
            } catch (Throwable ex) {
            }

            try {
                thread.stop();
            } catch (Throwable ex) {
            }
        }
    }
}
