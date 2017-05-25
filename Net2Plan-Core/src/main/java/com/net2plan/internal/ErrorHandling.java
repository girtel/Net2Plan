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




 





package com.net2plan.internal;

import com.net2plan.internal.Constants.UserInterface;
import com.net2plan.utils.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.*;

/**
 * Class handling errors within Net2Plan.
 * 
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public class ErrorHandling
{
	/**
	 * Reference to the STREAM that handles the output to the console dialog.
	 *
	 * @since 0.2.0
	 */
	public final static OutputStream STREAM;
	
	private final static JFrame consoleDialog;
	private final static JTextArea log;
	private final static String NEWLINE = StringUtils.getLineSeparator();
	public static boolean DEBUG = false;

	static
	{
		STREAM = new OutputStream()
		{
			@Override
			public void write(int b) throws IOException
			{
				log.append(new String(new byte[] { (byte) b }, StandardCharsets.UTF_8));
			}
		};

		if (SystemUtils.getUserInterface() == UserInterface.GUI)
		{
			consoleDialog = new JFrame();
			consoleDialog.setModalExclusionType(Dialog.ModalExclusionType.APPLICATION_EXCLUDE);
			Container contentPane = consoleDialog.getContentPane();
			if (contentPane instanceof JComponent)
			{
				((JComponent) contentPane).registerKeyboardAction(new ActionListener()
				{
					@Override
					public void actionPerformed(ActionEvent e)
					{
						consoleDialog.setVisible(false);
					}
				}, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
			}
			consoleDialog.setSize(new Dimension(500, 200));
			consoleDialog.setLocationRelativeTo(null);
			consoleDialog.setTitle("Console (close to hide)");
			consoleDialog.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
			consoleDialog.setLayout(new BorderLayout());

			log = new JTextArea();
			log.setFont(new JLabel().getFont());
			consoleDialog.add(new JScrollPane(log), BorderLayout.CENTER);

			JButton btn_reset = new JButton("Reset");
			btn_reset.setToolTipText("Clear the console");
			btn_reset.addActionListener(new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent ae)
				{
					log.setText("");
				}
			});

			JPanel bottomPanel = new JPanel(new FlowLayout());
			bottomPanel.add(btn_reset);
			consoleDialog.add(bottomPanel, BorderLayout.SOUTH);			
		}
		else
		{
			consoleDialog = null;
			log = null;
		}
	}

	/**
	 * Adds the output of a {@code Throwable} to the console.
	 * 
	 * @param throwable {@code Throwable}
	 * @since 0.2.0
	 */
	public static void addErrorOrException(Throwable throwable)
	{
		addErrorOrException(throwable, new LinkedHashSet<Class>());
	}

	/**
	 * Adds the output of a {@code Throwable} to the console. In case that debug 
	 * mode is disabled, the stack trace will stop once a stack element refers to 
	 * the given class.
	 * 
	 * @param throwable {@code Throwable}
	 * @param _class Class that should be omitted from the output
	 * @since 0.2.0
	 */
	public static void addErrorOrException(Throwable throwable, final Class _class)
	{
		Set<Class> aux = new LinkedHashSet<Class>();
		aux.add(_class);
		
		addErrorOrException(throwable, aux);
	}

	/**
	 * Adds the output of a {@code Throwable} to the console. In case that debug 
	 * mode is disabled, the stack trace will stop once a stack element refers to 
	 * one of the given classes.
	 * 
	 * @param throwable {@code Throwable}
	 * @param _classes Classes that should be omitted from the output (null means 'empty')
	 * @since 0.2.0
	 */
	public static void addErrorOrException(Throwable throwable, Collection<Class> _classes)
	{
		while(true)
		{
			Throwable aux = throwable.getCause();
			if (aux == null) break;
			
			throwable = aux;
		}

		StringBuilder text = new StringBuilder();
		text.append(throwable.toString()).append(NEWLINE);
		StackTraceElement[] stack = throwable.getStackTrace();
		
		Set<String> classNames = new LinkedHashSet<String>();
		Iterator<Class> it = _classes.iterator();
		while (it.hasNext()) classNames.add(it.next().getName());

		for (StackTraceElement line : stack)
		{
			String className = line.getClassName();
			int pos = className.indexOf('$');
			if (pos != -1) className = className.substring(0, pos);

			if (!DEBUG && classNames.contains(className)) break;
			text.append(line.toString()).append(NEWLINE);
		}
		
		addText(text.toString());
	}

	private static void addText(String text)
	{
		text = new Date().toString() + NEWLINE + NEWLINE + text;
		
		switch(SystemUtils.getUserInterface())
		{
			case CLI:
				System.out.println(text);
				break;
						
			case GUI:
				if (!log.getText().isEmpty()) text = NEWLINE + NEWLINE + text;
				log.append(text);
				break;
		}
	}

	/**
	 * Returns the most internal cause of a {@code Throwable}.
	 * 
	 * @param e Internal {@code Throwable}
	 * @return Internal cause of a {@code Throwable}
	 * @since 0.2.3
	 */
	public static Throwable getInternalThrowable(Throwable e)
	{
		Throwable cause = e.getCause();
		return cause == null ? e : getInternalThrowable(cause);
	}
	
	/**
	 * Indicates whether debug is enabled or not
	 * 
	 * @return {@code true} if debug is enabled. Otherwise, {@code false}
	 * @since 0.3.0
	 */
	public static boolean isDebugEnabled()
	{
		return DEBUG;
	}
	
	/**
	 * Prints the whole stack trace of a {@code Throwable}.
	 * 
	 * @param throwable Internal {@code Throwable}
	 * @since 0.3.0
	 */
	public static void printStackTrace(Throwable throwable)
	{
		System.out.println(throwable);
		System.out.println();

		StackTraceElement[] stack = throwable.getStackTrace();
		for (StackTraceElement line : stack) System.out.println(line);
	}

	/**
	 * Enables/disables the debug mode. If debug mode is enabled, the whole stack trace 
	 * of {@code Throwable} objects will be shown. Otherwise, developer can customize 
	 * what classes can be omitted from the output.
	 * 
	 * @param debug Indicates whether the debug mode is enabled ({@code true}) or disabled ({@code false})
	 * @since 0.2.3
	 */
	public static void setDebug(boolean debug)
	{
		DEBUG = debug;
	}

	/**
	 * Shows the output console (only for GUI).
	 * 
	 * @since 0.2.0
	 */
	public static void showConsole()
	{
		if (!consoleDialog.isVisible()) consoleDialog.setVisible(true);

		SwingUtilities.invokeLater(new Runnable()
		{
			private final WindowListener l = new WindowAdapter()
			{
				@Override
				public void windowDeiconified(WindowEvent e)
				{
					/* Window now deiconified so bring it to the front */
					bringToFront();

					/* Remove "one-shot" WindowListener to prevent memory leak */
					consoleDialog.removeWindowListener(this);
				}
			};

			@Override
			public void run()
			{
				if (consoleDialog.getExtendedState() == Frame.ICONIFIED)
				{
					consoleDialog.addWindowListener(l);
					consoleDialog.setExtendedState(Frame.NORMAL);
				}
				else
				{
					bringToFront();
				}
			}

			private void bringToFront()
			{
				consoleDialog.getGlassPane().setVisible(!consoleDialog.getGlassPane().isVisible());
				consoleDialog.toFront();
				/* Note: Calling repaint explicitly should not be necessary */
			}
		});
	}

	/**
	 * Prints a text into the console log in case the debug mode is activated.
	 * @param text Text to print.
	 */
	public static void log(String text)
	{
		if (isDebugEnabled())
		{
			Timestamp timestamp = new Timestamp(System.currentTimeMillis());
			System.out.println(timestamp + ": " + text);
		}
	}

	/**
	 * Shows a popup error indicating that user should check the output console (only for GUI).
	 *
	 * @since 0.2.0
	 */
	public static void showErrorDialog()
	{
		showErrorDialog("Error");
	}

	/**
	 * Shows a popup error indicating that user should check the output console (only for GUI).
	 * 
	 * @param title Title for the error dialog
	 * @since 0.2.0
	 */
	public static void showErrorDialog(String title)
	{
		showErrorDialog("Please, check console for more information", title);
	}

	/**
	 * Shows a popup error indicating an error to the user (only for GUI).
	 * 
	 * @param message Message to be shown
	 * @param title Title for the error dialog
	 * @since 0.2.0
	 */
	public static void showErrorDialog(String message, String title)
	{
		showMessage(message, title, JOptionPane.ERROR_MESSAGE);
	}

	/**
	 * Shows a popup indicating some information to the user (only for GUI).
	 * 
	 * @param message Message to be shown
	 * @param title Title for the information dialog
	 * @since 0.2.0
	 */
	public static void showInformationDialog(String message, String title)
	{
		showMessage(message, title, JOptionPane.INFORMATION_MESSAGE);
	}

	private static void showMessage(String message, String title, int type)
	{
		boolean wasVisible = consoleDialog.isVisible();
		if (wasVisible) consoleDialog.setVisible(false);
		
		Object data = message;

		JOptionPane.showMessageDialog(null, data, title, type);
		
		if (wasVisible) consoleDialog.setVisible(true);
	}

	/**
	 * Shows a popup indicating some message to the user (only for GUI).
	 * 
	 * @param message Message to be shown
	 * @param title Title for the message dialog
	 * @since 0.2.0
	 */
	public static void showMessageDialog(String message, String title)
	{
		showMessage(message, title, JOptionPane.PLAIN_MESSAGE);
	}

	/**
	 * Shows a popup indicating some warning to the user (only for GUI).
	 * 
	 * @param message Message to be shown
	 * @param title Title for the warning dialog
	 * @since 0.2.0
	 */
	public static void showWarningDialog(String message, String title)
	{
		showMessage(message, title, JOptionPane.WARNING_MESSAGE);
	}
}
