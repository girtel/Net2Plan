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

import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetworkElement;

/**
 * <p>Class to wrap an object with a {@code toString()} method independent of
 * the object's internal {@code toString()}. It is useful for {@code JComboBox}.</p>
 * <p>Example:</p>
 * <p>
 * {@code Integer number = new Integer(3);}
 * <br />
 * {@code StringLabeller labelledNumber = StringLabeller.of(number, "label");}
 * <br />
 * {@code System.out.println(number);} (returns 3);
 * <br />
 * {@code System.out.println(labelledNumber);} (returns "label");
 * <br />
 * </p>
 * <p><b>Important</b>: The object is not cloned or copied, so changes in the original
 * object are reflected here.</p>
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.2
 */
public class StringLabeller {
    private Object object;
    private String label;
    private final boolean isModifiable;

    /**
     * Default constructor.
     *
     * @param object       Object to be wrapped
     * @param label        New label for the wrapped object
     * @param isModifiable Indicates whether or not the elements can be changed after initialization
     * @since 0.3.1
     */
    public StringLabeller(Object object, String label, boolean isModifiable) {
        this.object = object;
        this.label = label;
        this.isModifiable = isModifiable;
        if (object instanceof NetworkElement)
            throw new RuntimeException("StringLabeller should not work with NetworkElements");
    }

    /**
     * Returns a {@code String} representation of the object. It is an alias of
     * the {@link #getLabel() getLabel()} method.
     *
     * @return {@code String} representation of the object
     * @since 0.2.2
     */
    @Override
    public String toString() {
        return getLabel();
    }

    private void checkIsModifiable() {
        if (!isModifiable) throw new Net2PlanException("StringLabeller is not modifiable");
    }

    /**
     * Factory method.
     *
     * @param object Object to be wrapped
     * @param label  New label for the wrapped object
     * @return A new {@code StringLabeller} object
     * @since 0.2.2
     */
    public static StringLabeller of(Object object, String label) {
        return new StringLabeller(object, label, true);
    }

    /**
     * Returns the label.
     *
     * @return Label
     * @since 0.3.1
     */
    public String getLabel() {
        return label;
    }

    /**
     * Returns the original object.
     *
     * @return The original object
     * @since 0.2.2
     */
    public Object getObject() {
        return object;
    }

    /**
     * Allows setting the label.
     *
     * @param label New label for the wrapped object
     * @since 0.3.1
     */
    public void setLabel(String label) {
        checkIsModifiable();
        this.label = label;
    }

    /**
     * Allows setting the object.
     *
     * @param object Object to be wrapped
     * @since 0.3.1
     */
    public void setObject(Object object) {
        checkIsModifiable();
        this.object = object;
        if (object instanceof NetworkElement)
            throw new RuntimeException("StringLabeller should not work with NetworkElements");
    }

    /**
     * Factory method for an unmodifiable object.
     *
     * @param object Object to be wrapped
     * @param label  New label for the wrapped object
     * @return A new {@code StringLabeller} object
     * @since 0.3.1
     */
    public static StringLabeller unmodifiableOf(Object object, String label) {
        return new StringLabeller(object, label, false);
    }

}
