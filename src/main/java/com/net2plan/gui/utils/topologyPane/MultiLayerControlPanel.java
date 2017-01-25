package com.net2plan.gui.utils.topologyPane;

import com.net2plan.gui.utils.IVisualizationCallback;
import com.net2plan.gui.utils.viewEditTopolTables.multilayerTabs.AdvancedJTable_MultiLayerControlTable;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.internal.Constants;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Jorge San Emeterio
 * @date 20-Jan-17
 */
public class MultiLayerControlPanel extends JPanel
{
    private final IVisualizationCallback callback;
    private final AdvancedJTable_MultiLayerControlTable multiLayerTable;
    private final MultiLayerButtonPanel buttonPanel;

    public MultiLayerControlPanel(final IVisualizationCallback callback)
    {
        this.callback = callback;
        this.multiLayerTable = new AdvancedJTable_MultiLayerControlTable(callback);
        this.buttonPanel = new MultiLayerButtonPanel();

        this.setLayout(new BorderLayout());

        fillPanel();
    }

    private void fillPanel()
    {
        this.add(new JScrollPane(multiLayerTable), BorderLayout.CENTER);
        this.add(buttonPanel, BorderLayout.SOUTH);
    }

    public void refreshTable()
    {
        multiLayerTable.updateTable();
    }

    private class MultiLayerButtonPanel extends JPanel implements ActionListener
    {
        private final JButton btn_showAllLayers, btn_hideAllLayers;
        private final JButton btn_sortLayerByIndex, btn_sortLayersByTopology;
        private final JButton btn_showAllLayerLinks, btn_hideAllLayerLinks;

        private MultiLayerButtonPanel()
        {
            // NOTE: Grid layout for the time being.
            this.setLayout(new GridLayout(3,2));

            btn_showAllLayers = new JButton("Show all layers");
            btn_hideAllLayers = new JButton("Hide all layers");
            btn_sortLayerByIndex = new JButton("Sort layers by index");
            btn_sortLayersByTopology = new JButton("Sort layers by topology");
            btn_showAllLayerLinks = new JButton("Show all layer links");
            btn_hideAllLayerLinks = new JButton("Hide all layer links");

            btn_showAllLayers.addActionListener(this);
            btn_hideAllLayers.addActionListener(this);
            btn_sortLayerByIndex.addActionListener(this);
            btn_sortLayersByTopology.addActionListener(this);
            btn_showAllLayerLinks.addActionListener(this);
            btn_hideAllLayerLinks.addActionListener(this);

            this.add(btn_showAllLayers);
            this.add(btn_hideAllLayers);
            this.add(btn_sortLayerByIndex);
            this.add(btn_sortLayersByTopology);
            this.add(btn_showAllLayerLinks);
            this.add(btn_hideAllLayerLinks);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            final Object src = e.getSource();

            final NetPlan netPlan = callback.getDesign();
            final VisualizationState vs = callback.getVisualizationState();

            if (src == btn_showAllLayers)
            {
                for (NetworkLayer networkLayer : netPlan.getNetworkLayers())
                {
                    if (callback.getDesign().getNetworkLayerDefault() == networkLayer) continue;

                    // NOTE: This one is going to update the vs for each layer. Bad performance.
                    vs.setLayerVisibility(networkLayer, true);
                }
            } else if (src == btn_hideAllLayers)
            {
                for (NetworkLayer networkLayer : netPlan.getNetworkLayers())
                {
                    if (callback.getDesign().getNetworkLayerDefault() == networkLayer) continue;

                    vs.setLayerVisibility(networkLayer, false);
                }
            } else if (src == btn_sortLayerByIndex)
            {
                final Map<NetworkLayer, Integer> layerIndexOrderMap = new HashMap<>();

                for (NetworkLayer networkLayer : netPlan.getNetworkLayers())
                {
                    layerIndexOrderMap.put(networkLayer, networkLayer.getIndex());
                }

                vs.updateLayerVisualizationState(netPlan, layerIndexOrderMap);
            } else if (src == btn_sortLayersByTopology)
            {
                final Map<NetworkLayer, Integer> layerIndexOrderMap = new HashMap<>();

                for (NetworkLayer networkLayer : netPlan.getNetworkLayerInTopologicalOrder())
                {
                    layerIndexOrderMap.put(networkLayer, networkLayer.getIndex());
                }

                vs.updateLayerVisualizationState(netPlan, layerIndexOrderMap);
            } else if (src == btn_showAllLayerLinks)
            {
                for (NetworkLayer networkLayer : netPlan.getNetworkLayers())
                {
                    vs.setLayerLinksVisibility(networkLayer, true);
                }
            } else if (src == btn_hideAllLayerLinks)
            {
                for (NetworkLayer networkLayer : netPlan.getNetworkLayers())
                {
                    vs.setLayerLinksVisibility(networkLayer, false);
                }
            }

            refreshTable();
            callback.updateVisualizationAfterChanges(Collections.singleton(Constants.NetworkElementType.LAYER));
        }
    }
    public void refreshTable ()
    {
    	multiLayerTable.updateTable();
    }
}
