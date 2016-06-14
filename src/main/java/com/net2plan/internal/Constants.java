/*******************************************************************************
 * Copyright (c) 2015 Pablo Pavon Mari�o.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Contributors:
 * Pablo Pavon Mari�o - initial API and implementation
 ******************************************************************************/


package com.net2plan.internal;

import com.net2plan.interfaces.networkDesign.IAlgorithm;
import com.net2plan.interfaces.networkDesign.IReport;
import com.net2plan.interfaces.simulation.IEventGenerator;
import com.net2plan.interfaces.simulation.IEventProcessor;

import java.util.Locale;

/**
 * Internal constants.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.2.2
 */
public class Constants {
    /**
     * Dialog type to handle {@link com.net2plan.interfaces.networkDesign.NetPlan NetPlan} objects.
     *
     * @since 0.3.0
     */
    public enum DialogType {
        /**
         * Network design.
         *
         * @since 0.3.0
         */
        NETWORK_DESIGN,

        /**
         * Traffic demands.
         *
         * @since 0.3.0
         */
        DEMANDS
    }

    ;

    /**
     * Capabilities of {@link com.net2plan.internal.io.IOFilter IOFilter} implementations.
     *
     * @since 0.3.0
     */
    public enum IOFeature {
        /**
         * {@link com.net2plan.internal.io.IOFilter IOFilter} implementation able to
         * load designs.
         *
         * @since 0.3.1
         */
        LOAD_DESIGN,

        /**
         * {@link com.net2plan.internal.io.IOFilter IOFilter} implementation able to
         * save designs.
         *
         * @since 0.3.1
         */
        SAVE_DESIGN,

        /**
         * {@link com.net2plan.internal.io.IOFilter IOFilter} implementation able to
         * load only traffic demands.
         *
         * @since 0.3.0
         */
        LOAD_DEMANDS,

        /**
         * {@link com.net2plan.internal.io.IOFilter IOFilter} implementation able to
         * save only traffic demands.
         *
         * @since 0.3.0
         */
        SAVE_DEMANDS
    }

    ;

    /**
     * Constants for each network element type.
     *
     * @since 0.3.0
     */
    public static enum NetworkElementType {
        /**
         * Network type.
         *
         * @since 0.3.0
         */
        NETWORK("network"),

        /**
         * Layer type.
         *
         * @since 0.3.0
         */
        LAYER("layer"),

        /**
         * Node type.
         *
         * @since 0.3.0
         */
        NODE("node"),

        /**
         * Link type.
         *
         * @since 0.3.0
         */
        LINK("link"),

        /**
         * Demand type.
         *
         * @since 0.3.0
         */
        DEMAND("demand"),

        /**
         * Multicat demand type.
         *
         * @since 0.3.0
         */
        MULTICAST_DEMAND("multicast demand"),

        /**
         * Route type.
         *
         * @since 0.3.0
         */
        ROUTE("route"),

        /**
         * Multicast tree type.
         *
         * @since 0.3.1
         */
        MULTICAST_TREE("multicast tree"),

        /**
         * Forwarding rule type.
         *
         * @since 0.3.0
         */
        FORWARDING_RULE("forwarding rule"),

        /**
         * Protection segment type.
         *
         * @since 0.3.0
         */
        PROTECTION_SEGMENT("protection segment"),

        /**
         * Shared-risk group type.
         *
         * @since 0.3.0
         */
        SRG("SRG");

        private final String label;

        NetworkElementType(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    ;

    /**
     * Constants for each runnable code type (algorithms, reports...). If used
     * as default value for a parameter, the kernel identifies the parameter as
     * a loader for an external class.
     *
     * @since 0.3.0
     */
    public enum RunnableCodeType {
        /**
         * Network design algorithm.
         *
         * @since 0.3.0
         */
        ALGORITHM("#algorithm#", "Algorithm", IAlgorithm.class),

        /**
         * Report.
         *
         * @since 0.3.0
         */
        REPORT("#report#", "Report", IReport.class),

        /**
         * Event generator.
         *
         * @since 0.3.0
         */
        EVENT_GENERATOR("#eventGenerator#", "Event generator", IEventGenerator.class),

        /**
         * Event processor.
         *
         * @since 0.3.0
         */
        EVENT_PROCESSOR("#eventProcessor#", "Event processor", IEventProcessor.class);

        private final String prefix, label;
        private final Class<? extends IExternal> _class;

        RunnableCodeType(String prefix, String label, Class<? extends IExternal> _class) {
            this.prefix = prefix;
            this.label = label;
            this._class = _class;
        }

        /**
         * Returns the runnable code type corresponding to the first characters
         * of the input value.
         *
         * @param value Input value
         * @return RunnableCodeType, or null if not found
         * @since 0.3.0
         */
        public static RunnableCodeType find(String value) {
            if (value != null && !value.isEmpty()) {
                value = value.toLowerCase(Locale.getDefault());
                for (RunnableCodeType runnableCodeType : values())
                    if (value.startsWith(runnableCodeType.prefix.toLowerCase(Locale.getDefault())))
                        return runnableCodeType;
            }

            return null;
        }

        /**
         * Returns the class associated to the runnable code type (IAlgorithm, IReport, ...).
         *
         * @return Runnable code type class
         * @since 0.3.0
         */
        public Class<? extends IExternal> getRunnableClass() {
            return _class;
        }

        /**
         * Returns the label associated to the runnable code type (Algorithm, Report, ...).
         *
         * @return Runnable code type label
         * @since 0.3.0
         */
        public String getRunnableLabel() {
            return label;
        }

        /**
         * Returns the prefix associated to the runnable code type (algorithm_, report_, ...).
         *
         * @return Runnable code type prefix
         * @since 0.3.0
         */
        public String getRunnablePrefix() {
            return prefix;
        }
    }

    /**
     * Types of user inteface.
     *
     * @since 0.2.2
     */
    public enum UserInterface {
        /**
         * Command-line interface.
         *
         * @since 0.2.2
         */
        CLI,

        /**
         * Graphical user interface.
         *
         * @since 0.2.2
         */
        GUI
    }

    ;


}
