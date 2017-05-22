package com.net2plan.gui.utils;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Jorge San Emeterio
 * @date 22/05/17
 */
public class JNumberFieldTest
{
    @Test
    public void getValueTest()
    {
        final JNumberField numberField = new JNumberField();

        assertThat(numberField.getValue()).isNotNull();
        assertThat(numberField.getValue()).isInstanceOf(Double.class);
    }

    @Test
    public void defaultValueTest()
    {
        final JNumberField numberField = new JNumberField();

        assertThat(numberField.getValue()).isEqualTo(0);
    }

    @Test
    public void customDefaultValueTest()
    {
        final double testValue = 0.5d;
        final JNumberField numberField = new JNumberField(testValue, 0, Double.MAX_VALUE, 0.1);

        assertThat(numberField.getValue()).isEqualTo(testValue);
    }

    @Test(expected = IllegalArgumentException.class)
    public void textValueTest()
    {
        final JNumberField numberField = new JNumberField();
        numberField.setValue("Test");
    }

    @Test
    public void doubleValueTest()
    {
        final JNumberField numberField = new JNumberField();
        numberField.setValue(1d);

        assertThat(numberField.getValue()).isEqualTo(1d);
    }

    @Test
    public void intValueTest()
    {
        final JNumberField numberField = new JNumberField();
        numberField.setValue(1);

        assertThat(numberField.getValue()).isEqualTo(1);
    }

    @Test
    public void checkLowerLimit()
    {
        final JNumberField numberField = new JNumberField();

        assertThat(numberField.getModel().getPreviousValue()).isNull();
    }

    @Test
    public void checkUpperLimit()
    {
        final JNumberField numberField = new JNumberField(1d, 0, 1, 0.1);

        assertThat(numberField.getModel().getNextValue()).isNull();
    }

    @Test
    public void checkBetweenLimits()
    {
        final JNumberField numberField = new JNumberField(0.5d, 0, 1, 0.1);

        assertThat(numberField.getModel().getNextValue()).isEqualTo(0.6d);
        assertThat(numberField.getModel().getPreviousValue()).isEqualTo(0.4d);
    }

    @Test(expected = IllegalArgumentException.class)
    public void outOfBoundValueTest()
    {
        final JNumberField numberField = new JNumberField();
        numberField.setValue(-1);
    }
}