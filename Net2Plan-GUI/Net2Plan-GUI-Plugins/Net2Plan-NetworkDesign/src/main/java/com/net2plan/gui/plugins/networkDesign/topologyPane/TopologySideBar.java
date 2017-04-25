package com.net2plan.gui.plugins.networkDesign.topologyPane;

import com.net2plan.gui.plugins.GUINetworkDesign;
import com.net2plan.gui.plugins.networkDesign.interfaces.ITopologyCanvas;
import com.net2plan.gui.plugins.networkDesign.visualizationControl.VisualizationConstants;
import com.net2plan.gui.plugins.networkDesign.visualizationControl.VisualizationState;
import com.net2plan.gui.utils.JPopUpButton;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author Jorge San Emeterio
 * @date 25/04/17
 */
public class TopologySideBar extends JToolBar implements ActionListener
{
    private final GUINetworkDesign callback;
    private final ITopologyCanvas canvas;

    private final JButton btn_increaseInterLayerDistance, btn_decreaseInterLayerDistance;
    private final JButton btn_npChangeUndo, btn_npChangeRedo;
    private final JToggleButton btn_showLowerLayerInfo, btn_showUpperLayerInfo, btn_showThisLayerInfo;

    private final MultiLayerControlPanel multilayerControlPanel;

    public TopologySideBar(GUINetworkDesign callback, ITopologyCanvas canvas)
    {
        super();

        this.callback = callback;
        this.canvas = canvas;

        this.setOrientation(JToolBar.VERTICAL);
        this.setRollover(true);
        this.setFloatable(false);
        this.setOpaque(false);

        this.multilayerControlPanel = new MultiLayerControlPanel(callback);

        // MultiLayer control window
        final JPopupMenu multiLayerPopUp = new JPopupMenu();
        multiLayerPopUp.add(multilayerControlPanel);

        final JPopUpButton btn_multilayer = new JPopUpButton("", multiLayerPopUp);

        /* Multilayer buttons */
        btn_increaseInterLayerDistance = new JButton();
        btn_increaseInterLayerDistance.setToolTipText("Increase the distance between layers (when more than one layer is visible)");
        btn_decreaseInterLayerDistance = new JButton();
        btn_decreaseInterLayerDistance.setToolTipText("Decrease the distance between layers (when more than one layer is visible)");
        btn_showLowerLayerInfo = new JToggleButton();
        btn_showLowerLayerInfo.setToolTipText("Shows the links in lower layers that carry traffic of the picked element");
        btn_showLowerLayerInfo.setSelected(callback.getVisualizationState().isShowInCanvasLowerLayerPropagation());
        btn_showUpperLayerInfo = new JToggleButton();
        btn_showUpperLayerInfo.setToolTipText("Shows the links in upper layers that carry traffic that appears in the picked element");
        btn_showUpperLayerInfo.setSelected(callback.getVisualizationState().isShowInCanvasUpperLayerPropagation());
        btn_showThisLayerInfo = new JToggleButton();
        btn_showThisLayerInfo.setToolTipText("Shows the links in the same layer as the picked element, that carry traffic that appears in the picked element");
        btn_showThisLayerInfo.setSelected(callback.getVisualizationState().isShowInCanvasThisLayerPropagation());
        btn_npChangeUndo = new JButton();
        btn_npChangeUndo.setToolTipText("Navigate back to the previous state of the network (last time the network design was changed)");
        btn_npChangeRedo = new JButton();
        btn_npChangeRedo.setToolTipText("Navigate forward to the next state of the network (when network design was changed");

        btn_increaseInterLayerDistance.setIcon(new ImageIcon(TopologyPanel.class.getResource("/resources/gui/increaseLayerDistance.png")));
        btn_decreaseInterLayerDistance.setIcon(new ImageIcon(TopologyPanel.class.getResource("/resources/gui/decreaseLayerDistance.png")));
        btn_multilayer.setIcon(new ImageIcon(TopologyPanel.class.getResource("/resources/gui/showLayerControl.png")));
        btn_showThisLayerInfo.setIcon(new ImageIcon(TopologyPanel.class.getResource("/resources/gui/showLayerPropagation.png")));
        btn_showUpperLayerInfo.setIcon(new ImageIcon(TopologyPanel.class.getResource("/resources/gui/showLayerUpperPropagation.png")));
        btn_showLowerLayerInfo.setIcon(new ImageIcon(TopologyPanel.class.getResource("/resources/gui/showLayerLowerPropagation.png")));
        btn_npChangeUndo.setIcon(new ImageIcon(TopologyPanel.class.getResource("/resources/gui/undoButton.png")));
        btn_npChangeRedo.setIcon(new ImageIcon(TopologyPanel.class.getResource("/resources/gui/redoButton.png")));

        btn_increaseInterLayerDistance.addActionListener(this);
        btn_decreaseInterLayerDistance.addActionListener(this);
        btn_showLowerLayerInfo.addActionListener(this);
        btn_showUpperLayerInfo.addActionListener(this);
        btn_showThisLayerInfo.addActionListener(this);
        btn_npChangeUndo.addActionListener(this);
        btn_npChangeRedo.addActionListener(this);

        this.add(btn_multilayer);
        this.add(btn_increaseInterLayerDistance);
        this.add(btn_decreaseInterLayerDistance);
        this.add(btn_showLowerLayerInfo);
        this.add(btn_showUpperLayerInfo);
        this.add(btn_showThisLayerInfo);
        this.add(Box.createVerticalGlue());
        //multiLayerToolbar.add(btn_npChangeUndo);
        //multiLayerToolbar.add(btn_npChangeRedo);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        Object src = e.getSource();
        final VisualizationState vs = callback.getVisualizationState();
        if (src == btn_increaseInterLayerDistance)
        {
            if (vs.getCanvasNumberOfVisibleLayers() == 1) return;

            final int currentInterLayerDistance = vs.getInterLayerSpaceInPixels();
            final int newInterLayerDistance = currentInterLayerDistance + (int) Math.ceil(currentInterLayerDistance * (VisualizationConstants.SCALE_IN - 1));

            vs.setInterLayerSpaceInPixels(newInterLayerDistance);
            canvas.updateInterLayerDistanceInNpCoordinates(newInterLayerDistance);
            canvas.updateAllVerticesXYPosition();
            canvas.refresh();
        } else if (src == btn_decreaseInterLayerDistance)
        {
            if (vs.getCanvasNumberOfVisibleLayers() == 1) return;

            final int currentInterLayerDistance = vs.getInterLayerSpaceInPixels();
            int newInterLayerDistance = currentInterLayerDistance - (int) Math.ceil(currentInterLayerDistance * (1 - VisualizationConstants.SCALE_OUT));

            if (newInterLayerDistance <= 0)
                newInterLayerDistance = 1;

            vs.setInterLayerSpaceInPixels(newInterLayerDistance);
            canvas.updateInterLayerDistanceInNpCoordinates(newInterLayerDistance);
            canvas.updateAllVerticesXYPosition();

            canvas.refresh();
        } else if (src == btn_showLowerLayerInfo)
        {
            vs.setShowInCanvasLowerLayerPropagation(btn_showLowerLayerInfo.isSelected());
            canvas.refresh();
        } else if (src == btn_showUpperLayerInfo)
        {
            vs.setShowInCanvasUpperLayerPropagation(btn_showUpperLayerInfo.isSelected());
            canvas.refresh();
        } else if (src == btn_showThisLayerInfo)
        {
            vs.setShowInCanvasThisLayerPropagation(btn_showThisLayerInfo.isSelected());
            canvas.refresh();
        } else if (src == btn_npChangeUndo)
        {
            callback.requestUndoAction();
        } else if (src == btn_npChangeRedo)
        {
            callback.requestRedoAction();
        }
    }

    public MultiLayerControlPanel getMultilayerControlPanel()
    {
        return multilayerControlPanel;
    }
}
