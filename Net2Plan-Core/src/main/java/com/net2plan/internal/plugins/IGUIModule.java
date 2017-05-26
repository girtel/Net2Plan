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



 




package com.net2plan.internal.plugins;

import com.net2plan.interfaces.networkDesign.Configuration;
import com.net2plan.internal.CommandLineParser;
import com.net2plan.internal.SystemUtils;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.*;

/**
 * Generic template for plugins (tools) within Net2Plan.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.0
 */
public abstract class IGUIModule extends JPanel implements Plugin
{
	private final String title;
	
	/**
	 * Reference to the current execution directory.
	 * 
	 * @since 0.2.0
	 */
	public final static File CURRENT_DIR;

	static { CURRENT_DIR = SystemUtils.getCurrentDir(); }
	
	/**
	 * Default constructor.
	 * 
	 * @param title Title of the tool (null or empty means no title)
	 * @since 0.2.0
	 */
	public IGUIModule(String title)
	{
		this.title = title.toUpperCase(Locale.getDefault());
	}
	
	/**
	 * Adds a key combination triggering some action.
	 * 
	 * @param description Description of the action
	 * @param keyCombination Key combination (it must not be already defined)
	 * @param action Action to be triggered by {@code keyCombination}
	 * @since 0.3.0
	 */
	public final void addKeyCombinationAction(String description, Action action, KeyStroke... keyCombination)
	{
		InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		KeyStroke[] currentKeyCombinationArray = inputMap.keys();
		if (currentKeyCombinationArray != null)
		{
			Set<KeyStroke> currentKeyCombinationSet = new LinkedHashSet<KeyStroke>(Arrays.asList(currentKeyCombinationArray));
			for(KeyStroke i : keyCombination)
				if (currentKeyCombinationSet.contains(i))
					throw new RuntimeException("Cannot override previous action for " + i);
		}
		
		ActionMap actionMap = getActionMap();
		for(KeyStroke i : keyCombination)
		{
			inputMap.put(i, description);
			actionMap.put(description, action);
		}
	}
	
	@Override
	public final Map<String, String> getCurrentOptions()
	{
		return CommandLineParser.getParameters(getParameters(), Configuration.getOptions());
	}
	
	/**
	 * Returns the key combination mapping.
	 * 
	 * @return Key combination mapping: key is the description, and value is the key combination
	 * @since 0.3.0
	 */
	public Map<String, KeyStroke> getKeyCombinations()
	{
		Map<String, KeyStroke> keyCombinationMap = new TreeMap<String, KeyStroke>();
		InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		KeyStroke[] currentKeyCombinationArray = inputMap.keys();
		if (currentKeyCombinationArray != null)
		{
			Set<KeyStroke> currentKeyCombinationSet = new LinkedHashSet<KeyStroke>(Arrays.asList(currentKeyCombinationArray));
			for(KeyStroke i : currentKeyCombinationSet)
			{
				String description = inputMap.get(i).toString();
				keyCombinationMap.put(description, i);
			}
		}
		
		return keyCombinationMap;
	}

	/**
	 * Removes the action associated to a key combination.
	 * 
	 * @param keyCombination Key combination
	 * @since 0.3.0
	 */
	protected final void removeKeyCombinationAction(KeyStroke... keyCombination)
	{
		ActionMap actionMap = getActionMap();
		InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		for(KeyStroke i : keyCombination)
		{
			Object description = inputMap.get(i);
			inputMap.remove(i);
			
			actionMap.remove(description);
		}
	}
	
	/**
	 * This method starts the tool and it should only be invoked by the kernel.
	 * 
	 * @since 0.3.0
	 */
	public void start()
	{
		if (title == null || title.isEmpty())
		{
			setLayout(new MigLayout("insets 0 0 0 0, nocache", "[grow]", "[grow]"));
		}
		else
		{
			setLayout(new MigLayout("insets 0 0 0 0, nocache", "[grow]", "[50px][grow]"));
			JPanel pnl_title = new JPanel();

			JLabel lbl_title = new JLabel(title);
			pnl_title.setBackground(Color.YELLOW);
			pnl_title.add(lbl_title);
			lbl_title.setFont(new Font(lbl_title.getFont().getName(), Font.BOLD, 20));
			lbl_title.setForeground(Color.BLACK);
			add(pnl_title, "grow, wrap");
		}

		JPanel contentPane = new JPanel(new MigLayout("fill, insets 0 0 0 0, nocache", "", ""));
		add(contentPane, "grow");
		contentPane.revalidate();

		configure(contentPane);
	}

	/**
	 * Ask the tool to stop itself before exiting.
	 */
	public abstract void stop();

	/**
	 * Asks the tool to configure itself once started.
	 * 
	 * @param contentPane Container for the tool
	 * @since 0.3.0
	 */
	public abstract void configure(JPanel contentPane);

	/**
	 * <p>Returns the map of keyboard shortcuts associated to this module.</p>
	 * 
	 * @return Map of keyboard shortcurts, where the key is the description of the triggered action and the value is the keyboard shortcut
	 * @since 0.3.0
	 */
	public Map<String, KeyStroke> getKeyShortcutMap()
	{
		Map<String, KeyStroke> keyShortcutMap = new LinkedHashMap<String, KeyStroke>();
		InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		KeyStroke[] keyStrokes = inputMap.allKeys();
		for(KeyStroke keyStroke : keyStrokes)
			keyShortcutMap.put(inputMap.get(keyStroke).toString(), keyStroke);
		
		return keyShortcutMap;
	}

	/**
	 * Returns a shortcut for the tool. Use {@code null} for no shortcut.
	 * 
	 * @return Shortcut to this tool
	 * @since 0.3.0
	 */
	public abstract KeyStroke getKeyStroke();
	
	/**
	 * Returns the menu path to the tool. Use '|' for parent menus.
	 * 
	 * @return Menu path
	 * @since 0.3.0
	 */
	public abstract String getMenu();

	@Override
	public int getPriority()
	{
		return 0;
	}
	
}
