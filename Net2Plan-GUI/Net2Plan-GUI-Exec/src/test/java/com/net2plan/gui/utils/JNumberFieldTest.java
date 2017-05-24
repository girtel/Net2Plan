/*******************************************************************************
 * Copyright (c) 2017 Pablo Pavon Marino and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the 2-clause BSD License 
 * which accompanies this distribution, and is available at
 * https://opensource.org/licenses/BSD-2-Clause
 *
 * Contributors:
 *     Pablo Pavon Marino and others - initial API and implementation
 *******************************************************************************/
package com.net2plan.gui.utils;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Jorge San Emeterio
 * @date 22/05/17
 */
@RunWith(JUnitParamsRunner.class)
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
    @Parameters({"0.5d"})
    public void customDefaultValueTest(double testValue)
    {
        final JNumberField numberField = new JNumberField(testValue, 0, Double.MAX_VALUE, 0.1);

        assertThat(numberField.getValue()).isEqualTo(testValue);
    }

    @Test(expected = IllegalArgumentException.class)
    @Parameters({"Test"})
    public void textValueTest(String text)
    {
        final JNumberField numberField = new JNumberField();
        numberField.setValue(text);
    }

    @Test
    @Parameters({"1d"})
    public void doubleValueTest(double doubleValue)
    {
        final JNumberField numberField = new JNumberField();
        numberField.setValue(doubleValue);

        assertThat(numberField.getValue()).isEqualTo(doubleValue);
    }

    @Test
    @Parameters({"1"})
    public void intValueTest(int intValue)
    {
        final JNumberField numberField = new JNumberField();
        numberField.setValue(intValue);

        assertThat(numberField.getValue()).isEqualTo(intValue);
    }

    @Test
    @Parameters({"0"})
    public void checkLowerLimit(int lowerLimit)
    {
        final JNumberField numberField = new JNumberField(lowerLimit, lowerLimit, Double.MAX_VALUE, 0.1d);

        assertThat(numberField.getModel().getPreviousValue()).isNull();

        numberField.setValue(lowerLimit);
        assertThat(numberField.getValue()).isEqualTo(lowerLimit);
    }

    @Test
    @Parameters({"1d"})
    public void checkUpperLimit(double upperLimit)
    {
        final JNumberField numberField = new JNumberField(upperLimit, 0, upperLimit, 0.1);

        assertThat(numberField.getModel().getNextValue()).isNull();

        numberField.setValue(upperLimit);
        assertThat(numberField.getValue()).isEqualTo(upperLimit);
    }

    @Test
    @Parameters({"0.5d|0.1d"})
    public void checkBetweenLimits(double middleValue, double increment)
    {
        final JNumberField numberField = new JNumberField(middleValue, 0, 1, increment);

        assertThat(numberField.getModel().getNextValue()).isEqualTo(middleValue + increment);
        assertThat(numberField.getModel().getPreviousValue()).isEqualTo(middleValue - increment);
    }

    @Test(expected = IllegalArgumentException.class)
    @Parameters({"-1"})
    public void outOfBoundValueTest(double invalidValue)
    {
        final JNumberField numberField = new JNumberField();
        numberField.setValue(-1);
    }
}