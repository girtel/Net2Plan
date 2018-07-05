
package com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.dialogs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import com.net2plan.interfaces.networkDesign.Net2PlanException;

import net.miginfocom.swing.MigLayout;

public class MtnDialogBuilder
{   
    public static MtnDialog launchBaseDialog (String dialogTitle , String headInfoMessage , String dialogHelpString , 
            JPanel middleJPanel ,
            JComponent parentComponent, Runnable doActionIfOk )
    {
        final MtnDialog dialog = new MtnDialog (dialogTitle);
        final JButton btn_ok = new JButton("OK");
        final JButton btn_cancel = new JButton("Cancel");
        
        JRootPane rootPane = SwingUtilities.getRootPane(dialog); 
        rootPane.setDefaultButton(btn_ok);
        rootPane.registerKeyboardAction(e -> {
            dialog.dispose();
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        btn_ok.addActionListener(e ->
        { 
        	try
        	{
        		doActionIfOk.run(); 
        		dialog.setVisible(false);
        		dialog.dispose();
        	}
        	catch (MtnDialogException ex)
        	{
        		JOptionPane.showMessageDialog(null, ex.getMessage(), "Info", JOptionPane.INFORMATION_MESSAGE);
        	}
        	catch (Net2PlanException ex)
            {
        		dialog.setException(ex);
        		dialog.setVisible(false);
        		dialog.dispose();
                ex.printStackTrace();
            } catch (Exception ex)
            {
            	dialog.setException(ex);
            	dialog.setVisible(false);
        		dialog.dispose();
                ex.printStackTrace();
//                ErrorHandling.showErrorDialog();
            }
        });
        btn_cancel.addActionListener(e -> { dialog.setVisible(false); dialog.dispose(); } );
        dialog.addWindowListener(new WindowAdapter()
            {
                @Override
                public void windowClosing(WindowEvent windowEvent)
                {
                    dialog.setVisible(false);
                    dialog.dispose();
                }
            });
        
        final JTextArea txt_infoMessage = new JTextArea();
        txt_infoMessage.setFont(new JLabel().getFont());
        txt_infoMessage.setBackground(new Color(255, 255, 255, 0));
        txt_infoMessage.setOpaque(false);
        txt_infoMessage.setLineWrap(true);
        txt_infoMessage.setEditable(false);
        txt_infoMessage.setWrapStyleWord(true);
        txt_infoMessage.setText(headInfoMessage != null ? headInfoMessage : "");
                      
        final JPanel buttonPanel = new JPanel(new MigLayout("fill, wrap 2"));
        buttonPanel.add(btn_ok, "grow");
        buttonPanel.add(btn_cancel, "grow");
        
        if (!txt_infoMessage.getText().isEmpty() || !dialogHelpString.isEmpty())
        {
        	final JPanel infoPanel = new JPanel(new MigLayout("fill, wrap 2"));
            
            if (! dialogHelpString.isEmpty())
            {
            	final JLabel helpIcon = new JLabel(new ImageIcon(MtnDialogBuilder.class.getResource("/resources/gui/question.png")));
            	helpIcon.setToolTipText(dialogHelpString);
            	infoPanel.add(helpIcon, "al label");
            }
            
            if (!txt_infoMessage.getText().isEmpty())
            	infoPanel.add(txt_infoMessage, "grow");
            
            dialog.add(infoPanel, BorderLayout.NORTH);
        }

        dialog.add(middleJPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(SwingUtilities.getRoot(parentComponent));
        
        return dialog;
    }
     
    public static void launch (
            String dialogTitle , 
            String dialogInitialMessage , 
            String dialogHelpString , 
            JComponent parentComponent , 
            List<MtnInputForDialog<?>> inputs , 
            Consumer<List<MtnInputForDialog<?>>> doActionIfOk)
    {
        final JPanel middleJPanel = new JPanel(new MigLayout("fill, wrap 2"));
        for (MtnInputForDialog<?> input : inputs)
        {       
        	final JLabel leftLabel = MtnDialogBuilder.createJLabel(input.getLeftExplanationMessage(), input.getHelpMessage());
            middleJPanel.add(leftLabel, "align label");
            middleJPanel.add(input.getRightComponent(), "growx");
        }
        
        final Runnable doActionIfOkComplete = () -> 
        {
        	doActionIfOk.accept(inputs);
        };
        
        MtnDialog dialog = launchBaseDialog (dialogTitle , dialogInitialMessage , dialogHelpString ,
                middleJPanel ,
                parentComponent , doActionIfOkComplete);  
        
        dialog.setVisible(true);

        if(dialog.getException().isPresent())
        	throw new Net2PlanException(dialog.getException().get().getMessage());
    }
    
    public static JLabel createJLabel(String label, String tooltip)
    {
    	final JLabel res = new JLabel(label);
    	res.setToolTipText(tooltip);
    	
    	return res;    	
    }



	static class MtnDialog extends JDialog 
	{
		private static final long serialVersionUID = 1L;
		private Optional<? extends Exception> exception = Optional.empty();
    	
    	public MtnDialog (String dialogTitle) {
    		super();
    		
    		// Parent frame
            this.setTitle(dialogTitle != null ? dialogTitle : "");
            this.setModal(true);
            this.setModalityType(ModalityType.APPLICATION_MODAL);
            this.setResizable(false);
            this.setLayout(new BorderLayout());
    	}
    	    	
        public Optional<? extends Exception> getException()
        {
        	return exception;
        }
        <T extends Exception> void setException( T ex) {
        	exception = Optional.of(ex);
        }
    }
	
}
