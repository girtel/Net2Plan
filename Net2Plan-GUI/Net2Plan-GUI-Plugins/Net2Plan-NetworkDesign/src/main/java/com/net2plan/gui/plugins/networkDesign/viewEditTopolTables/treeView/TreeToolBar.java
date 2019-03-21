package com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.treeView;

import javax.swing.*;

@SuppressWarnings("serial")
class TreeToolBar extends JToolBar
{
    private TreePanel parentPanel;

    private JButton btn_resetButton;

    public TreeToolBar(TreePanel treePanel)
    {
        super();

        this.parentPanel = treePanel;

        this.setFloatable(false);
        this.setRollover(true);
        this.setOpaque(false);

        this.setOrientation(JToolBar.HORIZONTAL);

        this.buildToolbar();

        this.setState(false);
    }

    private void buildToolbar()
    {
        this.add(Box.createHorizontalGlue());

        this.btn_resetButton = new JButton("X");
        this.btn_resetButton.setName("reset");
        this.btn_resetButton.setFocusable(false);
        this.btn_resetButton.addActionListener(e -> parentPanel.restoreView());

        this.add(btn_resetButton);
    }

    public void setState(boolean isActivated)
    {
        btn_resetButton.setEnabled(isActivated);
    }
}
