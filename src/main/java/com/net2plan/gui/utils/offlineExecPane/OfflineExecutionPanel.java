package com.net2plan.gui.utils.offlineExecPane;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Closeable;
import java.io.File;
import java.util.HashSet;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.apache.commons.collections15.BidiMap;

import com.net2plan.gui.utils.IVisualizationCallback;
import com.net2plan.gui.utils.ParameterValueDescriptionPanel;
import com.net2plan.gui.utils.RunnableSelector;
import com.net2plan.gui.utils.ThreadExecutionController;
import com.net2plan.gui.utils.topologyPane.visualizationControl.VisualizationState;
import com.net2plan.interfaces.networkDesign.Configuration;
import com.net2plan.interfaces.networkDesign.IAlgorithm;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.interfaces.networkDesign.NetworkLayer;
import com.net2plan.internal.ErrorHandling;
import com.net2plan.internal.SystemUtils;
import com.net2plan.internal.plugins.IGUIModule;
import com.net2plan.utils.ClassLoaderUtils;
import com.net2plan.utils.Pair;
import com.net2plan.utils.Triple;

import net.miginfocom.swing.MigLayout;

public class OfflineExecutionPanel extends JPanel implements ThreadExecutionController.IThreadExecutionHandler
{
	private final IVisualizationCallback mainWindow;
    private ThreadExecutionController algorithmController;
    private RunnableSelector algorithmSelector;
    private long start;
    final JButton btn_solve;
	
	public OfflineExecutionPanel (IVisualizationCallback mainWindow)
	{
		super ();

		this.mainWindow = mainWindow;
		
		setLayout(new MigLayout("insets 0 0 0 0", "[grow]", "[grow]"));
        
        File ALGORITHMS_DIRECTORY = new File(IGUIModule.CURRENT_DIR + SystemUtils.getDirectorySeparator() + "workspace");
        ALGORITHMS_DIRECTORY = ALGORITHMS_DIRECTORY.isDirectory() ? ALGORITHMS_DIRECTORY : IGUIModule.CURRENT_DIR;

        ParameterValueDescriptionPanel algorithmParameters = new ParameterValueDescriptionPanel();
        algorithmSelector = new RunnableSelector("Algorithm", null, IAlgorithm.class, ALGORITHMS_DIRECTORY, algorithmParameters);
        algorithmController = new ThreadExecutionController(this);
        JPanel pnl_buttons = new JPanel(new MigLayout("", "[center, grow]", "[]"));

        btn_solve = new JButton("Execute");
        pnl_buttons.add(btn_solve);
        btn_solve.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                algorithmController.execute();
            }
        });
		
		add(algorithmSelector, "grow");
        add(pnl_buttons, "dock south");

		
	}
	
	public void reset ()
	{
		algorithmSelector.reset();
	}

	@Override
	public Object execute(ThreadExecutionController controller) 
	{
        start = System.nanoTime();
        final Triple<File, String, Class> algorithm = algorithmSelector.getRunnable();
        final Map<String, String> algorithmParameters = algorithmSelector.getRunnableParameters();
        Configuration.updateSolverLibraryNameParameter(algorithmParameters); // put default path to libraries if solverLibraryName is ""
        final Map<String, String> net2planParameters = Configuration.getNet2PlanOptions();
        NetPlan netPlan = mainWindow.getDesign().copy();
        IAlgorithm instance = ClassLoaderUtils.getInstance(algorithm.getFirst(), algorithm.getSecond(), IAlgorithm.class);
//		System.out.println ("BEFORE EXECUTING");
        String out = instance.executeAlgorithm(netPlan, algorithmParameters, net2planParameters);
//		System.out.println ("AFTER EXECUTING");
        try {
            ((Closeable) instance.getClass().getClassLoader()).close();
        } catch (Throwable e) {
        }
        netPlan.setNetworkLayerDefault(netPlan.getNetworkLayer((int) 0));
        mainWindow.getDesign().assignFrom(netPlan); // do not update undo/redo here -> the visualization state should be updated before
        return out;
	}

	@Override
	public void executionFinished(ThreadExecutionController controller, Object out) 
	{
        try {
            double execTime = (System.nanoTime() - start) / 1e9;
            final VisualizationState vs = mainWindow.getVisualizationState();
    		Pair<BidiMap<NetworkLayer, Integer>, Map<NetworkLayer,Boolean>> res = 
    				vs.suggestCanvasUpdatedVisualizationLayerInfoForNewDesign(new HashSet<> (mainWindow.getDesign().getNetworkLayers()));
    		vs.setCanvasLayerVisibilityAndOrder(mainWindow.getDesign() , res.getFirst() , res.getSecond());
            mainWindow.updateVisualizationAfterNewTopology();
            mainWindow.getUndoRedoNavigationManager().addNetPlanChange();
            String outMessage = String.format("Algorithm executed successfully%nExecution time: %.3g s%nExit message: %s", execTime, out);
            JOptionPane.showMessageDialog(null, outMessage, "Solve design", JOptionPane.PLAIN_MESSAGE);
        } catch (Throwable ex) {
            ErrorHandling.addErrorOrException(ex, OfflineExecutionPanel.class);
            ErrorHandling.showErrorDialog("Error executing algorithm");
        }
	}

	@Override
	public void executionFailed(ThreadExecutionController controller) 
	{
		ErrorHandling.showErrorDialog("Error executing algorithm");
	}

	public void doClickInExecutionButton () { btn_solve.doClick();}
}
