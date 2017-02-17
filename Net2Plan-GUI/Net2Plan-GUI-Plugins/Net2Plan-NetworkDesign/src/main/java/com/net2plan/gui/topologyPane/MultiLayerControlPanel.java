package com.net2plan.gui.topologyPane;

import com.net2plan.gui.utils.visualizationControl.VisualizationState;
import com.net2plan.gui.viewEditTopolTables.multilayerTabs.AdvancedJTable_MultiLayerControlTable;
import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.internal.Constants;
import org.apache.commons.collections15.BidiMap;
import org.apache.commons.collections15.bidimap.DualHashBidiMap;

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
    private final GUINetworkDesign callback;
    private final AdvancedJTable_MultiLayerControlTable multiLayerTable;
    private final MultiLayerButtonPanel buttonPanel;

    public MultiLayerControlPanel(final GUINetworkDesign callback)
    {
        this.callback = callback;
        this.multiLayerTable = new AdvancedJTable_MultiLayerControlTable(callback);
        this.buttonPanel = new MultiLayerButtonPanel();

        this.setLayout(new BorderLayout());

        fillPanel();
    }

    private void fillPanel()
    {
        final JPanel auxPanel = new JPanel(new BorderLayout());
        auxPanel.add(multiLayerTable);
        auxPanel.add(multiLayerTable.getTableHeader(), BorderLayout.NORTH);

        this.add(auxPanel, BorderLayout.CENTER);
        this.add(buttonPanel, BorderLayout.WEST);
    }

    private class MultiLayerButtonPanel extends JToolBar implements ActionListener
    {
        private final JButton btn_showAllLayers, btn_hideAllLayers;
        private final JButton btn_sortLayerByIndex, btn_sortLayersByTopology;
        private final JButton btn_showAllLayerLinks, btn_hideAllLayerLinks;

        private MultiLayerButtonPanel()
        {
            super();
            this.setOrientation(JToolBar.VERTICAL);
            this.setRollover(true);
            this.setFloatable(false);
            this.setOpaque(false);
            this.setBorderPainted(false);

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
            	Map<NetworkLayer,Boolean> visibilityInfo = new HashMap <>();
                for (NetworkLayer networkLayer : netPlan.getNetworkLayers())
                	visibilityInfo.put(networkLayer , true);
                if (!visibilityInfo.equals(vs.getCanvasLayerVisibilityMap()))
                {
                	vs.setCanvasLayerVisibilityAndOrder(netPlan , null , visibilityInfo);
                	callback.getVisualizationState().resetPickedState();
                    callback.updateVisualizationAfterChanges(Collections.singleton(Constants.NetworkElementType.LAYER));
                }
            } else if (src == btn_hideAllLayers)
            {
            	Map<NetworkLayer,Boolean> visibilityInfo = new HashMap <>();
                for (NetworkLayer networkLayer : netPlan.getNetworkLayers())
                	visibilityInfo.put(networkLayer , networkLayer.equals(netPlan.getNetworkLayerDefault()));
                if (!visibilityInfo.equals(vs.getCanvasLayerVisibilityMap()))
                {
                	vs.setCanvasLayerVisibilityAndOrder(netPlan , null , visibilityInfo);
                    callback.updateVisualizationAfterChanges(Collections.singleton(Constants.NetworkElementType.LAYER));
                }
            } else if (src == btn_sortLayerByIndex)
            {
                final BidiMap<NetworkLayer, Integer> layerIndexOrderMap = new DualHashBidiMap<>();
                for (NetworkLayer networkLayer : netPlan.getNetworkLayers())
                    layerIndexOrderMap.put(networkLayer, networkLayer.getIndex());
                if (!layerIndexOrderMap.equals(vs.getCanvasLayerOrderIndexMap(true)))
                {
                    vs.setCanvasLayerVisibilityAndOrder(netPlan, layerIndexOrderMap , null);
                    callback.updateVisualizationAfterChanges(Collections.singleton(Constants.NetworkElementType.LAYER));
                }
            } else if (src == btn_sortLayersByTopology)
            {
                final BidiMap<NetworkLayer, Integer> layerIndexOrderMap = new DualHashBidiMap<>();
                for (NetworkLayer networkLayer : netPlan.getNetworkLayerInTopologicalOrder())
                    layerIndexOrderMap.put(networkLayer, networkLayer.getIndex());
                if (!layerIndexOrderMap.equals(vs.getCanvasLayerOrderIndexMap(true)))
                {
                    vs.setCanvasLayerVisibilityAndOrder(netPlan, layerIndexOrderMap , null);
                    callback.updateVisualizationAfterChanges(Collections.singleton(Constants.NetworkElementType.LAYER));
                }
            } else if (src == btn_showAllLayerLinks)
            {
                for (NetworkLayer networkLayer : netPlan.getNetworkLayers())
                    vs.setLayerLinksVisibilityInCanvas(networkLayer, true);
                callback.updateVisualizationJustCanvasLinkNodeVisibilityOrColor ();
            } else if (src == btn_hideAllLayerLinks)
            {
                for (NetworkLayer networkLayer : netPlan.getNetworkLayers())
                    vs.setLayerLinksVisibilityInCanvas(networkLayer, false);
                callback.updateVisualizationJustCanvasLinkNodeVisibilityOrColor ();
            }

            refreshTable();
        }
    }
    public void refreshTable ()
    {
    	multiLayerTable.updateTable();
    }
}
