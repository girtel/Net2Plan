package com.net2plan.gui.utils;

import javax.swing.*;
import javax.swing.text.NumberFormatter;

/**
 * @author Jorge San Emeterio
 * @date 22/05/17
 */
public class JNumberField extends JSpinner
{
    public JNumberField(double minValue, double maxValue)
    {
        super(new SpinnerNumberModel(0d, minValue, maxValue, 1));

        final NumberEditor editor = (NumberEditor) this.getEditor();
        editor.getFormat().setMinimumFractionDigits(0);
        editor.getFormat().setMaximumFractionDigits(4);

        final JFormattedTextField spinnerTextEditor = editor.getTextField();
        spinnerTextEditor.setColumns(6);

        final NumberFormatter numberFormatter = new NumberFormatter(editor.getFormat());
        numberFormatter.setValueClass(Double.class);
        numberFormatter.setMinimum(0d);
        numberFormatter.setMaximum(Double.MAX_VALUE);
        numberFormatter.setAllowsInvalid(false);

        spinnerTextEditor.setFormatterFactory(new JFormattedTextField(numberFormatter).getFormatterFactory());
    }
}
