package com.net2plan.gui.utils;

import javax.swing.*;
import javax.swing.text.NumberFormatter;

/**
 * @author Jorge San Emeterio
 * @date 22/05/17
 */
public class JNumberField extends JSpinner
{
    public JNumberField()
    {
        this(0d, 0, Double.MAX_VALUE, 1);
    }

    public JNumberField(double defaultNumber, double minValue, double maxValue, double increment)
    {
        super(new SpinnerNumberModel(defaultNumber, minValue, maxValue, increment));

        final NumberEditor editor = (NumberEditor) this.getEditor();
        editor.getFormat().setMinimumFractionDigits(2);
        editor.getFormat().setMaximumFractionDigits(6);

        final JFormattedTextField spinnerTextEditor = editor.getTextField();
        spinnerTextEditor.setColumns(6);
        spinnerTextEditor.setHorizontalAlignment(JTextField.CENTER);

        final NumberFormatter numberFormatter = new NumberFormatter(editor.getFormat());
        numberFormatter.setValueClass(Double.class);
        numberFormatter.setMinimum(0d);
        numberFormatter.setMaximum(Double.MAX_VALUE);
        numberFormatter.setAllowsInvalid(false);

        spinnerTextEditor.setFormatterFactory(new JFormattedTextField(numberFormatter).getFormatterFactory());
    }
}
