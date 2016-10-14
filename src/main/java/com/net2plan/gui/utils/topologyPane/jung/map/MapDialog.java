package com.net2plan.gui.utils.topologyPane.jung.map;

import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
 * Created by Jorge San Emeterio on 14/10/2016.
 */
public class MapDialog extends JDialog
{
    private final JPanel panel;
    private final MapPanel mapViewer;
    private final JButton btn_enter;

    private File mapFile = null;

    public MapDialog()
    {
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.setSize(new Dimension(800, 600));
        this.setMinimumSize(new Dimension(800, 600));
        this.setLayout(new GridBagLayout());
        this.setTitle("Topology map selector");

        panel = new JPanel(new BorderLayout());

        this.mapViewer = new MapPanel();
        this.mapFile = null;
        this.btn_enter = new JButton("Enter");

        // Take photo trigger
        btn_enter.addActionListener(e ->
        {
            MapDialog.this.firePropertyChange("takeMap", false, true);
        });

        final JComponent mapComponent = mapViewer.getMapComponent();

        panel.add(mapComponent, BorderLayout.CENTER);
        panel.add(btn_enter, BorderLayout.SOUTH);

        final GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;

        this.getContentPane().add(panel, gbc);
    }

    public File getMapFile(final int width, final int height)
    {
        final int oldWidth = this.getWidth();
        final int oldHeight = this.getHeight();

        this.setSize(width, height);
        this.revalidate();
        this.repaint();

        mapFile = mapViewer.saveMap(width, height);

        this.setSize(oldWidth, oldHeight);
        this.revalidate();
        this.repaint();

        return mapFile;
    }

    public File getMapFileAndClose(final int width, final int height)
    {
        this.setSize(width, height);
        this.revalidate();
        this.repaint();

        mapFile = mapViewer.saveMap(width, height);

        this.setVisible(false);
        this.dispose();

        return mapFile;
    }
}
