/*******************************************************************************
 * Copyright (c) 2016 Pablo Pavon-Marino.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Contributors:
 * Pablo Pavon-Marino - Jose-Luis Izquierdo-Zaragoza, up to version 0.3.1
 * Pablo Pavon-Marino - from version 0.4.0 onwards
 ******************************************************************************/

package com.net2plan.utils;

/**
 * <p>Auxiliary class with several application-wide constants.</p>
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 */
public class Constants {
    private Constants() {
    }

    /**
     * Earth radius (in kilometers).
     */
    public final static double EARTH_RADIUS_IN_KM = 6371;

    /**
     * Indicates whether (and how) or not to check routing cycles.
     */
    public enum CheckRoutingCycleType {
        /**
         * Routing cycles are not checked.
         */
        NO_CHECK,

        /**
         * No node is traversed more than once.
         */
        NO_REPEAT_NODE,

        /**
         * No link is traversed more than once.
         */
        NO_REPEAT_LINK
    }

    ;

    /**
     * Constants for choosing the ordering type.
     */
    public enum OrderingType {
        /**
         * Ascending order.
         */
        ASCENDING,

        /**
         * Descending order.
         */
        DESCENDING
    }

    ;

    /**
     * Types of routing cycles. In case of {@link com.net2plan.utils.Constants.RoutingType#SOURCE_ROUTING SOURCE_ROUTING},
     * only loopless and open cycles are allowed. In case of {@link com.net2plan.utils.Constants.RoutingType#HOP_BY_HOP_ROUTING HOP_BY_HOP_ROUTING},
     * also closed loops are allowed.
     */
    public enum RoutingCycleType {
        /**
         * Traffic goes from ingress node to egress node without traversing any
         * node more than once.
         */
        LOOPLESS("Loopless"),

        /**
         * Traffic goes from ingress node to egress node traversing some node
         * more than once.
         */
        OPEN_CYCLES("Open cycles"),

        /**
         * Traffic does not reach the egress node.
         */
        CLOSED_CYCLES("Closed cycles");

        private final String label;

        RoutingCycleType(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    /**
     * Routing type determining how to get (or send traffic) from a node to another one.
     *
     * @see <a href='http://www.corecom.com/external/OSNconnexions/OSNconnexions.html#Source%20Routing'>Source Routing and "Hop-by-Hop" Routing</a>
     */
    public enum RoutingType {
        /**
         * All the information about how to get (or send traffic) from a node to
         * another one is known by the ingress node thanks to end-to-end paths (or
         * routes in {@link com.net2plan.interfaces.networkDesign.NetPlan NetPlan}
         * object).
         */
        SOURCE_ROUTING("Source routing"),

        /**
         * Ingress node is not expected to have all the information about how to
         * get (or send traffic) from a node to another one; it is sufficient for
         * the ingress node to know only how to get (or send traffic) to the
         * "next hop", and so on until the destination is reached.
         */
        HOP_BY_HOP_ROUTING("Hop-by-hop routing");

        private final String label;

        RoutingType(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    /**
     * Constants for search methods.
     */
    public enum SearchType {
        /**
         * Returns all elements matching the condition(s).
         */
        ALL,

        /**
         * Returns the first element matching the condition(s).
         */
        FIRST,

        /**
         * Returns the last element matching the condition(s).
         */
        LAST
    }

    ;
}
