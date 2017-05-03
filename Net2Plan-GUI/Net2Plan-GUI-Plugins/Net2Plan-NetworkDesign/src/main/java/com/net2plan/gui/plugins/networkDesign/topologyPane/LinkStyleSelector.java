/*******************************************************************************
 * Copyright (c) 2015 Pablo Pavon MariÃ±o.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Contributors:
 * Pablo Pavon MariÃ±o - initial API and implementation
 ******************************************************************************/


package com.net2plan.gui.plugins.networkDesign.topologyPane;

import com.net2plan.gui.plugins.networkDesign.visualizationControl.VisualizationConstants;
import com.net2plan.gui.plugins.networkDesign.visualizationControl.VisualizationState;
import com.net2plan.internal.ErrorHandling;
import com.net2plan.utils.SwingUtils;
import java.util.List;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.util.LinkedList;

/**
 *
 * @author Javier Lopez
 */
public final class LinkStyleSelector extends JDialog implements ActionListener
{
	
    private final JTabbedPane tabbedPane;
    private final VisualizationState _visualizationState;
    JDialog _dialog;


    public LinkStyleSelector(VisualizationState visualizationState)
    {
        super();

        _dialog = this;
        _visualizationState = visualizationState;
        
        _dialog.setTitle("Link Style");
        _dialog.setLayout(new BorderLayout());

        tabbedPane = new JTabbedPane();
        _dialog.add(tabbedPane, BorderLayout.CENTER);

        //Link-utilization coloring Tab
        tabbedPane.addTab("Link-utilization coloring", getLinkUtilizationColoringPanel());

        //Link run-out time coloring Tab
        tabbedPane.addTab("Link run-out time coloring", getLinkRunoutTimeColoringPanel());
        
        //Link thickness Tab
        tabbedPane.addTab("Link relative thickness", getLinkThicknessPanel());

        SwingUtils.configureCloseDialogOnEscape(_dialog);
        _dialog.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
        _dialog.setSize(400, 440);
        _dialog.setLocationRelativeTo(null);
        _dialog.setResizable(false);
        _dialog.setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
    }

    public JPanel getLinkUtilizationColoringPanel()
    {
        List<Double> linkUtilizationColor = _visualizationState.getLinkUtilizationColor();
        if (linkUtilizationColor.size() != VisualizationConstants.DEFAULT_LINKCOLORINGRUNOUTTHRESHOLDS.size()) throw new RuntimeException();
        JTextField[] fieldArray = new JTextField[linkUtilizationColor.size()];
        
        for(int i = 0; i < linkUtilizationColor.size(); i++)
        {
            fieldArray[i] = new JTextField("" + linkUtilizationColor.get(i));
            fieldArray[i].setHorizontalAlignment(SwingConstants.RIGHT);
        }
        
        JButton btn1 = new JButton("");
        btn1.setPreferredSize(new Dimension(20, 20));
        btn1.setBackground(new Color(0, 255, 0));
        btn1.setEnabled(false);
        btn1.setContentAreaFilled(false);
        btn1.setOpaque(true);
        
        JButton btn2 = new JButton("");
        btn2.setPreferredSize(new Dimension(20, 20));
        btn2.setBackground(new Color(50, 255, 0));
        btn2.setEnabled(false);
        btn2.setContentAreaFilled(false);
        btn2.setOpaque(true);
        
        JButton btn3 = new JButton("");
        btn3.setPreferredSize(new Dimension(20, 20));
        btn3.setBackground(new Color(100, 255, 0));
        btn3.setEnabled(false);
        btn3.setContentAreaFilled(false);
        btn3.setOpaque(true);
        
        JButton btn4 = new JButton("");
        btn4.setPreferredSize(new Dimension(20, 20));
        btn4.setBackground(new Color(150, 255, 0));
        btn4.setEnabled(false);
        btn4.setContentAreaFilled(false);
        btn4.setOpaque(true);
        
        JButton btn5 = new JButton("");
        btn5.setPreferredSize(new Dimension(20, 20));
        btn5.setBackground(new Color(200, 255, 0));
        btn5.setEnabled(false);
        btn5.setContentAreaFilled(false);
        btn5.setOpaque(true);
        
        JButton btn6 = new JButton("");
        btn6.setPreferredSize(new Dimension(20, 20));
        btn6.setBackground(new Color(255, 255, 0));
        btn6.setEnabled(false);
        btn6.setContentAreaFilled(false);
        btn6.setOpaque(true);
        
        JButton btn7 = new JButton("");
        btn7.setPreferredSize(new Dimension(20, 20));
        btn7.setBackground(new Color(255, 200, 0));
        btn7.setEnabled(false);
        btn7.setContentAreaFilled(false);
        btn7.setOpaque(true);
        
        JButton btn8 = new JButton("");
        btn8.setPreferredSize(new Dimension(20, 20));
        btn8.setBackground(new Color(255, 150, 0));
        btn8.setEnabled(false);
        btn8.setContentAreaFilled(false);
        btn8.setOpaque(true);
        
        JButton btn9 = new JButton("");
        btn9.setPreferredSize(new Dimension(20, 20));
        btn9.setBackground(new Color(255, 100, 0));
        btn9.setEnabled(false);
        btn9.setContentAreaFilled(false);
        btn9.setOpaque(true);
        
        JButton btn10 = new JButton("");
        btn10.setPreferredSize(new Dimension(20, 20));
        btn10.setBackground(new Color(255, 50, 0));
        btn10.setEnabled(false);
        btn10.setContentAreaFilled(false);
        btn10.setOpaque(true);
        
        JButton btn11 = new JButton("");
        btn11.setPreferredSize(new Dimension(20, 20));
        btn11.setBackground(new Color(255, 0, 0));
        btn11.setEnabled(false);
        btn11.setContentAreaFilled(false);
        btn11.setOpaque(true);

        JToggleButton btn_apply = new JToggleButton("Is applied" , _visualizationState.getIsActiveLinkUtilizationColorThresholdList());
        btn_apply.setToolTipText("The link coloring per utilization is active or not");
        btn_apply.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
            	_visualizationState.setIsActiveLinkUtilizationColorThresholdList(btn_apply.isSelected());
            }
        });
        
        JButton btn_save = new JButton("Save");
        btn_save.setToolTipText("Save the current selection");
        btn_save.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                boolean isValid = true;
                
                for(int i = 0; i < linkUtilizationColor.size(); i++)
                {
                    double value = -1;
                    
                    if(fieldArray[i].getText().matches("[0-9]+\\.*[0-9]*"))
                    {
                        try
                        {
                        value = Double.parseDouble(fieldArray[i].getText());
                        }
                        catch(NumberFormatException nfe){}
                    }
                    
                    double previousValue = -1;
                    
                    if(i == linkUtilizationColor.size() - 1) 
                        previousValue = 100; 
                    else if(fieldArray[i+1].getText().matches("[0-9]+\\.*[0-9]*"))
                    {
                        try
                        {
                        previousValue = Double.parseDouble(fieldArray[i+1].getText());
                        }
                        catch(NumberFormatException nfe){}
                    }

                    if (value != -1)
                    {
                        if(previousValue != -1 && value >= previousValue)
                        {
                           fieldArray[i].setBackground(new Color(255, 51, 51));
                           isValid = false;
                        }
                        else
                            fieldArray[i].setBackground(Color.WHITE);
                    } else
                    {
                        fieldArray[i].setBackground(new Color(255, 51, 51));
                        isValid = false;
                    }
                }
                
                if(isValid)
                {
                    for(int i = 0; i < linkUtilizationColor.size(); i++)
                        linkUtilizationColor.set(i, Double.parseDouble(fieldArray[i].getText()));

                    _visualizationState.setLinkUtilizationColor(linkUtilizationColor);
                    
                    dispose();
                }
                else
                {
                    ErrorHandling.showErrorDialog("Some fields are incorrect!", "Error");
                }
            }
        });

        JButton btn_cancel = new JButton("Cancel");
        btn_cancel.setToolTipText("Close the dialog without saving");
        btn_cancel.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                dispose();
            }
        });
        
        JButton btn_reset = new JButton("Reset");
        btn_reset.setToolTipText("Reset all options");
        btn_reset.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
            	linkUtilizationColor.clear();
            	linkUtilizationColor.addAll (VisualizationConstants.DEFAULT_LINKCOLORINGUTILIZATIONTHRESHOLDS);
                for(int i = 0; i < linkUtilizationColor.size(); i++)
                {
                    fieldArray[i].setText("" + linkUtilizationColor.get(i));
                    fieldArray[i].setBackground(Color.WHITE);
                }
            }
        });
        
        JLabel label_100 = new JLabel("100");
        label_100.setHorizontalAlignment(SwingConstants.RIGHT);
        
        JLabel label_0 = new JLabel("0");
        label_0.setHorizontalAlignment(SwingConstants.RIGHT);
        
        JPanel pane = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(0, 5, 5, 5);
        gbc.gridwidth = 1;
        pane.add(new JLabel("Color"), gbc);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        pane.add(new JLabel("Utilization (%)"), gbc);

        gbc.gridwidth = 1;
        pane.add(btn11, gbc);
        pane.add(new Label(">="), gbc);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        pane.add(label_100, gbc);
        
        gbc.gridwidth = 1;
        pane.add(btn10, gbc);
        pane.add(new Label(">="), gbc);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        pane.add(fieldArray[8], gbc);

        gbc.gridwidth = 1;
        pane.add(btn9, gbc);
        pane.add(new Label(">="), gbc);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        pane.add(fieldArray[7], gbc);
        
        gbc.gridwidth = 1;
        pane.add(btn8, gbc);
        pane.add(new Label(">="), gbc);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        pane.add(fieldArray[6], gbc);
        
        gbc.gridwidth = 1;
        pane.add(btn7, gbc);
        pane.add(new Label(">="), gbc);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        pane.add(fieldArray[5], gbc);
        
        gbc.gridwidth = 1;
        pane.add(btn6, gbc);
        pane.add(new Label(">="), gbc);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        pane.add(fieldArray[4], gbc);
        
        gbc.gridwidth = 1;
        pane.add(btn5, gbc);
        pane.add(new Label(">="), gbc);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        pane.add(fieldArray[3], gbc);
        
        gbc.gridwidth = 1;
        pane.add(btn4, gbc);
        pane.add(new Label(">="), gbc);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        pane.add(fieldArray[2], gbc);
        
        gbc.gridwidth = 1;
        pane.add(btn3, gbc);
        pane.add(new Label(">="), gbc);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        pane.add(fieldArray[1], gbc);
        
        gbc.gridwidth = 1;
        pane.add(btn2, gbc);
        pane.add(new Label(">="), gbc);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        pane.add(fieldArray[0], gbc);
        
        gbc.gridwidth = 1;
        pane.add(btn1, gbc);
        pane.add(new Label(">="), gbc);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        pane.add(label_0, gbc);
        
        //Main pane
        JPanel mainPane = new JPanel(new BorderLayout());
        
        //Buttons bar
        JPanel buttonBar = new JPanel();

        buttonBar.add(btn_save);
        buttonBar.add(btn_reset);
        buttonBar.add(btn_cancel);

        mainPane.add(pane, BorderLayout.CENTER);
        mainPane.add(buttonBar, BorderLayout.SOUTH);
        
        return mainPane;
    }

    public JPanel getLinkRunoutTimeColoringPanel()
    {
        List<Double> linkRunoutTimeColor = _visualizationState.getLinkRunoutTimeColor();
                               
        JTextField[] fieldArray = new JTextField[linkRunoutTimeColor.size()];
        
        for(int i = 0; i < linkRunoutTimeColor.size(); i++)
        {
            fieldArray[i] = new JTextField("" + linkRunoutTimeColor.get(i));
            fieldArray[i].setHorizontalAlignment(SwingConstants.RIGHT);
        }
        
        JButton btn1 = new JButton("");
        btn1.setPreferredSize(new Dimension(20, 20));
        btn1.setBackground(new Color(0, 255, 0));
        btn1.setEnabled(false);
        btn1.setContentAreaFilled(false);
        btn1.setOpaque(true);
        
        JButton btn2 = new JButton("");
        btn2.setPreferredSize(new Dimension(20, 20));
        btn2.setBackground(new Color(50, 255, 0));
        btn2.setEnabled(false);
        btn2.setContentAreaFilled(false);
        btn2.setOpaque(true);
        
        JButton btn3 = new JButton("");
        btn3.setPreferredSize(new Dimension(20, 20));
        btn3.setBackground(new Color(100, 255, 0));
        btn3.setEnabled(false);
        btn3.setContentAreaFilled(false);
        btn3.setOpaque(true);
        
        JButton btn4 = new JButton("");
        btn4.setPreferredSize(new Dimension(20, 20));
        btn4.setBackground(new Color(150, 255, 0));
        btn4.setEnabled(false);
        btn4.setContentAreaFilled(false);
        btn4.setOpaque(true);
        
        JButton btn5 = new JButton("");
        btn5.setPreferredSize(new Dimension(20, 20));
        btn5.setBackground(new Color(200, 255, 0));
        btn5.setEnabled(false);
        btn5.setContentAreaFilled(false);
        btn5.setOpaque(true);
        
        JButton btn6 = new JButton("");
        btn6.setPreferredSize(new Dimension(20, 20));
        btn6.setBackground(new Color(255, 255, 0));
        btn6.setEnabled(false);
        btn6.setContentAreaFilled(false);
        btn6.setOpaque(true);
        
        JButton btn7 = new JButton("");
        btn7.setPreferredSize(new Dimension(20, 20));
        btn7.setBackground(new Color(255, 200, 0));
        btn7.setEnabled(false);
        btn7.setContentAreaFilled(false);
        btn7.setOpaque(true);
        
        JButton btn8 = new JButton("");
        btn8.setPreferredSize(new Dimension(20, 20));
        btn8.setBackground(new Color(255, 150, 0));
        btn8.setEnabled(false);
        btn8.setContentAreaFilled(false);
        btn8.setOpaque(true);
        
        JButton btn9 = new JButton("");
        btn9.setPreferredSize(new Dimension(20, 20));
        btn9.setBackground(new Color(255, 100, 0));
        btn9.setEnabled(false);
        btn9.setContentAreaFilled(false);
        btn9.setOpaque(true);
        
        JButton btn10 = new JButton("");
        btn10.setPreferredSize(new Dimension(20, 20));
        btn10.setBackground(new Color(255, 50, 0));
        btn10.setEnabled(false);
        btn10.setContentAreaFilled(false);
        btn10.setOpaque(true);
        
        JButton btn11 = new JButton("");
        btn11.setPreferredSize(new Dimension(20, 20));
        btn11.setBackground(new Color(255, 0, 0));
        btn11.setEnabled(false);
        btn11.setContentAreaFilled(false);
        btn11.setOpaque(true);
        
        JToggleButton btn_apply = new JToggleButton("Is applied" , _visualizationState.getIsActiveLinkRunoutTimeColorThresholdList());
        btn_apply.setToolTipText("The link coloring per run-out capacity time is active or not");
        btn_apply.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
            	_visualizationState.setIsActiveLinkRunoutTimeColorThresholdList(btn_apply.isSelected());
            }
        });

        JButton btn_save = new JButton("Save");
        btn_save.setToolTipText("Save the current selection");
        btn_save.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                boolean isValid = true;
                
                for(int i = 0; i < linkRunoutTimeColor.size(); i++)
                {
                    double value = -1;
                    
                    if(fieldArray[i].getText().matches("[0-9]+\\.*[0-9]*"))
                    {
                        try
                        {
                        value = Double.parseDouble(fieldArray[i].getText());
                        }
                        catch(NumberFormatException nfe){}
                    }
                    
                    double previousValue = -1;
                    
                    if(i == linkRunoutTimeColor.size() - 1) 
                        previousValue = Double.MAX_VALUE; 
                    else if(fieldArray[i+1].getText().matches("[0-9]+\\.*[0-9]*"))
                    {
                        try
                        {
                        previousValue = Double.parseDouble(fieldArray[i+1].getText());
                        }
                        catch(NumberFormatException nfe){}
                    }

                    if (value != -1)
                    {
                        if(previousValue != -1 && value >= previousValue)
                        {
                           fieldArray[i].setBackground(new Color(255, 51, 51));
                           isValid = false;
                        }
                        else
                            fieldArray[i].setBackground(Color.WHITE);
                    } else
                    {
                        fieldArray[i].setBackground(new Color(255, 51, 51));
                        isValid = false;
                    }
                }
                
                if(isValid)
                {
                    for(int i = 0; i < linkRunoutTimeColor.size(); i++)
                        linkRunoutTimeColor.set(i, Double.parseDouble(fieldArray[i].getText()));

                    _visualizationState.setLinkRunoutTimeColor(linkRunoutTimeColor);
                    dispose();
                }
                else
                {
                    ErrorHandling.showErrorDialog("Some fields are incorrect!", "Error");
                }
            }
        });

        JButton btn_cancel = new JButton("Cancel");
        btn_cancel.setToolTipText("Close the dialog without saving");
        btn_cancel.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                dispose();
            }
        });
        
        JButton btn_reset = new JButton("Reset");
        btn_reset.setToolTipText("Reset all options");
        btn_reset.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
            	linkRunoutTimeColor.clear();
            	linkRunoutTimeColor.addAll(VisualizationConstants.DEFAULT_LINKCOLORINGRUNOUTTHRESHOLDS);
                for(int i = 0; i < linkRunoutTimeColor.size(); i++)
                {
                    fieldArray[i].setText("" + linkRunoutTimeColor.get(i));
                    fieldArray[i].setBackground(Color.WHITE);
                }
            }
        });
        
        JLabel label_0 = new JLabel("0");
        label_0.setHorizontalAlignment(SwingConstants.RIGHT);
        
        JPanel pane = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(0, 5, 5, 5);
        gbc.gridwidth = 1;
        pane.add(new JLabel("Color"), gbc);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        pane.add(new JLabel("Run-out time (months)"), gbc);

        gbc.gridwidth = 1;
        pane.add(btn11, gbc);
        pane.add(new Label(">="), gbc);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        pane.add(fieldArray[9], gbc);
        
        gbc.gridwidth = 1;
        pane.add(btn10, gbc);
        pane.add(new Label(">="), gbc);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        pane.add(fieldArray[8], gbc);

        gbc.gridwidth = 1;
        pane.add(btn9, gbc);
        pane.add(new Label(">="), gbc);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        pane.add(fieldArray[7], gbc);
        
        gbc.gridwidth = 1;
        pane.add(btn8, gbc);
        pane.add(new Label(">="), gbc);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        pane.add(fieldArray[6], gbc);
        
        gbc.gridwidth = 1;
        pane.add(btn7, gbc);
        pane.add(new Label(">="), gbc);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        pane.add(fieldArray[5], gbc);
        
        gbc.gridwidth = 1;
        pane.add(btn6, gbc);
        pane.add(new Label(">="), gbc);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        pane.add(fieldArray[4], gbc);
        
        gbc.gridwidth = 1;
        pane.add(btn5, gbc);
        pane.add(new Label(">="), gbc);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        pane.add(fieldArray[3], gbc);
        
        gbc.gridwidth = 1;
        pane.add(btn4, gbc);
        pane.add(new Label(">="), gbc);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        pane.add(fieldArray[2], gbc);
        
        gbc.gridwidth = 1;
        pane.add(btn3, gbc);
        pane.add(new Label(">="), gbc);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        pane.add(fieldArray[1], gbc);
        
        gbc.gridwidth = 1;
        pane.add(btn2, gbc);
        pane.add(new Label(">="), gbc);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        pane.add(fieldArray[0], gbc);
        
        gbc.gridwidth = 1;
        pane.add(btn1, gbc);
        pane.add(new Label(">="), gbc);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        pane.add(label_0, gbc);
        
        //Main pane
        JPanel mainPane = new JPanel(new BorderLayout());
        
        //Buttons bar
        JPanel buttonBar = new JPanel();

        buttonBar.add(btn_save);
        buttonBar.add(btn_reset);
        buttonBar.add(btn_cancel);

        mainPane.add(pane, BorderLayout.CENTER);
        mainPane.add(buttonBar, BorderLayout.SOUTH);
        
        return mainPane;
    }
    
    public JPanel getLinkThicknessPanel()
    {
        List<Double> linkCapacityThickness = _visualizationState.getLinkCapacityThickness();
              
        JTextField[] fieldArray = new JTextField[linkCapacityThickness.size()];
        
        for(int i = 0; i < linkCapacityThickness.size(); i++)
        {
            fieldArray[i] = new JTextField("" + linkCapacityThickness.get(i));
            fieldArray[i].setHorizontalAlignment(SwingConstants.RIGHT);
        }
        
        JToggleButton btn_apply = new JToggleButton("Is applied" , _visualizationState.getIsActiveLinkCapacityThicknessThresholdList());
        btn_apply.setToolTipText("The link thickness dependent on its capacity is active or not");
        btn_apply.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
            	_visualizationState.setIsActiveLinkCapacityThicknessThresholdList(btn_apply.isSelected());
            }
        });

        
        JButton btn_save = new JButton("Save");
        btn_save.setToolTipText("Save the current selection");
        btn_save.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                boolean isValid = true;
                
                for(int i = 0; i < linkCapacityThickness.size(); i++)
                {
                    double value = -1;
                    
                    if(fieldArray[i].getText().matches("[0-9]+\\.*[0-9]*"))
                    {
                        try
                        {
                        value = Double.parseDouble(fieldArray[i].getText());
                        }
                        catch(NumberFormatException nfe){}
                    }
                    
                    double previousValue = -1;
                    
                    if(i == linkCapacityThickness.size() - 1) 
                        previousValue = Double.MAX_VALUE; 
                    else if(fieldArray[i+1].getText().matches("[0-9]+\\.*[0-9]*"))
                    {
                        try
                        {
                        previousValue = Double.parseDouble(fieldArray[i+1].getText());
                        }
                        catch(NumberFormatException nfe){}
                    }

                    if (value != -1)
                    {
                        if(previousValue != -1 && value >= previousValue)
                        {
                           fieldArray[i].setBackground(new Color(255, 51, 51));
                           isValid = false;
                        }
                        else
                            fieldArray[i].setBackground(Color.WHITE);
                    } else
                    {
                        fieldArray[i].setBackground(new Color(255, 51, 51));
                        isValid = false;
                    }
                }
                
                if(isValid)
                {
                    for(int i = 0; i < linkCapacityThickness.size(); i++)
                        linkCapacityThickness.set(i, Double.parseDouble(fieldArray[i].getText()));

                    _visualizationState.setLinkCapacityThickness(linkCapacityThickness);
                    
                    dispose();
                }
                else
                {
                    ErrorHandling.showErrorDialog("Some fields are incorrect!", "Error");
                }
            }
        });

        JButton btn_cancel = new JButton("Cancel");
        btn_cancel.setToolTipText("Close the dialog without saving");
        btn_cancel.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                dispose();
            }
        });
        
        JButton btn_reset = new JButton("Reset");
        btn_reset.setToolTipText("Reset all options");
        btn_reset.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                linkCapacityThickness.clear ();
                linkCapacityThickness.addAll(VisualizationConstants.DEFAULT_LINKTHICKNESSTHRESHPOLDS);
                for(int i = 0; i < linkCapacityThickness.size(); i++)
                {
                    fieldArray[i].setText("" + linkCapacityThickness.get(i));
                    fieldArray[i].setBackground(Color.WHITE);
                }
            }
        });
        
        JLabel label_0 = new JLabel("0");
        label_0.setHorizontalAlignment(SwingConstants.RIGHT);
        
        JPanel pane = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(0, 5, 5, 5);
        gbc.gridwidth = 1;
        pane.add(new JLabel("Thickness"), gbc);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        pane.add(new JLabel("Capacity (Gbps)"), gbc);

        gbc.gridwidth = 1;
        pane.add(new RectPane(11), gbc);
        pane.add(new Label(">="), gbc);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        pane.add(fieldArray[7], gbc);
       
        gbc.gridwidth = 1;
        pane.add(new RectPane(8), gbc);
        pane.add(new Label(">="), gbc);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        pane.add(fieldArray[6], gbc);
        
        gbc.gridwidth = 1;
        pane.add(new RectPane(7), gbc);
        pane.add(new Label(">="), gbc);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        pane.add(fieldArray[5], gbc);
        
        gbc.gridwidth = 1;
        pane.add(new RectPane(6), gbc);
        pane.add(new Label(">="), gbc);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        pane.add(fieldArray[4], gbc);
        
        gbc.gridwidth = 1;
        pane.add(new RectPane(5), gbc);
        pane.add(new Label(">="), gbc);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        pane.add(fieldArray[3], gbc);
        
        gbc.gridwidth = 1;
        pane.add(new RectPane(4), gbc);
        pane.add(new Label(">="), gbc);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        pane.add(fieldArray[2], gbc);
        
        gbc.gridwidth = 1;
        pane.add(new RectPane(3), gbc);
        pane.add(new Label(">="), gbc);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        pane.add(fieldArray[1], gbc);
        
        gbc.gridwidth = 1;
        pane.add(new RectPane(2), gbc);
        pane.add(new Label(">="), gbc);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        pane.add(fieldArray[0], gbc);
        
        gbc.gridwidth = 1;
        pane.add(new RectPane(1), gbc);
        pane.add(new Label(">="), gbc);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        pane.add(label_0, gbc);
        
        //Main pane
        JPanel mainPane = new JPanel(new BorderLayout());
        
        //Buttons bar
        JPanel buttonBar = new JPanel();

        buttonBar.add(btn_save);
        buttonBar.add(btn_reset);
        buttonBar.add(btn_cancel);

        mainPane.add(pane, BorderLayout.CENTER);
        mainPane.add(buttonBar, BorderLayout.SOUTH);
        
        return mainPane;
    }
}

class RectPane extends JPanel 
{
    private Point p1 = new Point(0, 8);
    private Point p2 = new Point(30, 8);
    private int height;

    public RectPane(int height) 
    {
        this.height = height;
    }

    @Override
    public Dimension getPreferredSize() 
    {
        return new Dimension(20, 20);
    }

    @Override
    protected void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setColor(Color.BLACK);
        for (double t = 0; t < 1; t += 0.01) 
        {
            Point2D p = between(p1, p2, t);
            g2d.fillRect((int)p.getX(), (int)p.getY(), 5, height);
        }
        g2d.dispose();
    }

    public Point2D between(Point p1, Point p2, double time) 
    {
        double deltaX = p2.getX() - p1.getX();
        double deltaY = p2.getY() - p1.getY();

        double x = p1.getX() + time * deltaX;
        double y = p1.getY() + time * deltaY;

        return new Point2D.Double(x, y);

    }
}


