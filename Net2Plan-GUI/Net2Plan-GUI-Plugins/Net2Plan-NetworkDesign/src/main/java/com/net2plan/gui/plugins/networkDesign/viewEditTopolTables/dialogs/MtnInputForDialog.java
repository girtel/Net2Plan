package com.net2plan.gui.plugins.networkDesign.viewEditTopolTables.dialogs;

import java.awt.Component;
import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JTextField;

import com.net2plan.interfaces.networkDesign.Net2PlanException;

public class MtnInputForDialog<T>
{
    private final String helpMessage;
    private final String leftExplanationMessage;
    private final Component rightComponent;
    private final Integer minimumLength;
//    private final Function<T,Boolean> isValidRange;
    private final Supplier<T> getValueFunction;
    private final Supplier<Boolean> isValidCastWhatInComponent;
    
//    private MtnInputForDialog(String helpMessage, String leftExplanationMessage, Component rightComponent, Integer minimumLength, Function<T, Boolean> isValidRange, Supplier<T> getValueFunction, Supplier<Boolean> isValidWhatInComponent)
    private MtnInputForDialog(String helpMessage, String leftExplanationMessage, Component rightComponent, Integer minimumLength, Supplier<T> getValueFunction , Supplier<Boolean> isValidCastWhatInComponent)
    {
        super();
        this.helpMessage = helpMessage;
        this.leftExplanationMessage = leftExplanationMessage;
        this.rightComponent = rightComponent;
        this.minimumLength = minimumLength;
//        this.isValidRange = isValidRange;
        this.getValueFunction = getValueFunction;
        this.isValidCastWhatInComponent = isValidCastWhatInComponent;
    }
    
    final public void setComponentAsEnabled (boolean enabled) { this.rightComponent.setEnabled(enabled); }
    
    final public Component getRightComponent () 
    {
        return rightComponent;
    }
//    final public T get () { return getValueFunction.get(); }

    final public T get () { if (!isValidCastWhatInComponent.get()) throw new Net2PlanException ("The value in the input field is not correct"); return getValueFunction.get(); }
    final public boolean isValidCastWhatInComponent () { return isValidCastWhatInComponent.get(); }
//    final boolean isValidRange (T val) { return isValidRange.apply(val); }
    public String getHelpMessage()
    {
        return helpMessage;
    }
    public String getLeftExplanationMessage()
    {
        return leftExplanationMessage;
    }
    public int getMinimumLength()
    {
        return minimumLength == null? 1 : minimumLength;
    }

    public static MtnInputForDialog<Boolean> inputCheckBox (String leftExplanationMessage , String helpMessage , boolean defaultValue , Consumer<Boolean> actionIfSelected)
    {
        final JCheckBox c = new JCheckBox();
        c.setEnabled(true);
        c.setSelected(defaultValue);
        if (actionIfSelected != null)
            c.addActionListener(e->actionIfSelected.accept(c.isSelected()));
        final MtnInputForDialog<Boolean> input = new MtnInputForDialog<Boolean>
            (helpMessage, 
                    leftExplanationMessage, 
                    c, 
                    null, 
                    ()->c.isSelected(), 
                    ()-> true);
        return input;
    }
    public static MtnInputForDialog<String> inputTfString (String leftExplanationMessage , String helpMessage , Integer minimumLength , String defaultValue)
    {
        final JTextField c = new JTextField(minimumLength);
        c.setEnabled(true);
        c.setText(defaultValue);
        final MtnInputForDialog<String> input = new MtnInputForDialog<String>
        (helpMessage, 
                leftExplanationMessage, 
                c, 
                minimumLength, 
                ()->c.getText(), 
                ()-> true);
        return input;
    }
    public static MtnInputForDialog<Integer> inputTfInt (String leftExplanationMessage , String helpMessage , Integer minimumLength , Integer defaultValue)
    {
        final JTextField c = new JTextField(minimumLength);
        c.setEnabled(true);
        c.setText(defaultValue == null? "" : "" + defaultValue);
        final MtnInputForDialog<Integer> input = new MtnInputForDialog<Integer>
        (helpMessage, 
                leftExplanationMessage, 
                c, 
                minimumLength, 
                ()-> Integer.parseInt(c.getText()), 
                ()-> { try { final int val = Integer.parseInt(c.getText()); return true; } catch (Exception e) { return false; }    } );
        return input;
    }
    public static MtnInputForDialog<Double> inputTfDouble (String leftExplanationMessage , String helpMessage , Integer minimumLength , Double defaultValue)
    {
        final JTextField c = new JTextField(minimumLength);
        c.setEnabled(true);
        c.setText(defaultValue == null? "" : "" + defaultValue);
        final MtnInputForDialog<Double> input = new MtnInputForDialog<Double>
            (helpMessage, 
                leftExplanationMessage, 
                c, 
                minimumLength, 
                ()-> Double.parseDouble(c.getText()), 
                ()-> { try { final double val = Double.parseDouble(c.getText()); return true; } catch (Exception e) { return false; }    } );
        return input;
    }

    public static <T> MtnInputForDialog<T> inputTfCombo (String leftExplanationMessage , String helpMessage , 
            Integer minimumLength , 
            T defaultValue , 
            List<T> values , 
            List<String> stringsPerValue , 
            Consumer<T> actionsPerSelection)
    {
        if (values.isEmpty()) throw new Net2PlanException("Empty options list");
        if (stringsPerValue == null) stringsPerValue = values.stream().map(e->e.toString()).collect(Collectors.toList());
        assert values.size() == stringsPerValue.size();
        assert stringsPerValue.size() == new HashSet<> (stringsPerValue).size ();
        final String [] stringsInArray = new String [stringsPerValue.size()];
        final JComboBox<String> c = new JComboBox<String>(stringsPerValue.toArray(stringsInArray));
        int initialSelectedIndex = 0;
        if (defaultValue != null)
            for (int index = 0 ; index <values.size() ; index ++)
                if (values.get(index).equals(defaultValue))
                    { initialSelectedIndex = index; break; }
        c.setEditable(false);
        c.setSelectedIndex(initialSelectedIndex);
        if (actionsPerSelection != null)
            c.addActionListener(e-> actionsPerSelection.accept(values.get(c.getSelectedIndex())));
        final MtnInputForDialog<T> input = new MtnInputForDialog<>
        (helpMessage, 
            leftExplanationMessage, 
            c, 
            minimumLength, 
            ()-> values.get(c.getSelectedIndex()), 
            ()-> true);
        return input;
    }
    

}
