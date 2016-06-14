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

package com.net2plan.libraries;

import cern.colt.matrix.tdouble.DoubleFactory1D;
import cern.colt.matrix.tdouble.DoubleFactory2D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import cern.colt.matrix.tint.IntFactory1D;
import com.net2plan.interfaces.networkDesign.*;
import com.net2plan.utils.Constants;
import com.net2plan.utils.IntUtils;
import com.net2plan.utils.Pair;
import com.net2plan.utils.StringUtils;

import java.util.*;

/**
 * Class to deal with optical topologies including wavelength assignment and regenerator placement.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 */
public class WDMUtils {
    /**
     * This class represents the request to add a new lightpath. It is used in online algorithms related to WDM networks, inside {@code SimEvent} objects.
     */
    public static class LightpathAdd {
        public Route lpAddedToFillByProcessor;
        public final NetworkLayer layer;
        public final Demand demand;
        public final Node ingressNode, egressNode;
        public final List<Link> seqLinks_primary;
        public final int[] seqWavelengths_primary;
        public final List<Link> seqLinks_backup;
        public final int[] seqWavelengths_backup;
        public final double lineRateGbps;

        /**
         * Constructor to generate a new {@code LightpathAdd} object.
         *
         * @param demand       Demand
         * @param lineRateGbps Line rate in Gbps
         */
        public LightpathAdd(Demand demand, double lineRateGbps) {
            this.demand = demand;
            this.seqLinks_primary = null;
            this.lineRateGbps = lineRateGbps;
            this.seqWavelengths_primary = null;
            this.seqLinks_backup = null;
            this.seqWavelengths_backup = null;
            this.ingressNode = demand.getIngressNode();
            this.egressNode = demand.getEgressNode();
            this.layer = demand.getLayer();
        }

        /**
         * Constructor to generate a new {@code LightpathAdd} object.
         *
         * @param demand         Demand
         * @param seqLinks       Sequence of fibers
         * @param seqWavelengths Sequence of wavelengths
         * @param lineRateGbps   Line rate in Gbps
         */
        public LightpathAdd(Demand demand, List<Link> seqLinks, int[] seqWavelengths, double lineRateGbps) {
            this.demand = demand;
            this.seqLinks_primary = seqLinks;
            this.lineRateGbps = lineRateGbps;
            this.seqWavelengths_primary = seqWavelengths;
            this.seqLinks_backup = null;
            this.seqWavelengths_backup = null;
            this.ingressNode = demand.getIngressNode();
            this.egressNode = demand.getEgressNode();
            this.layer = demand.getLayer();
        }

        /**
         * Constructor to generate a new {@code LightpathAdd} object.
         *
         * @param demand                 Demand
         * @param seqLinks_primary       Primary sequence of fibers
         * @param seqLinks_backup        Backup sequence of fibers
         * @param seqWavelengths_primary Primary sequence of wavelengths
         * @param seqWavelengths_backup  Backup sequence of wavelengths
         * @param lineRateGbps           Line rate in Gbps
         */
        public LightpathAdd(Demand demand, List<Link> seqLinks_primary, List<Link> seqLinks_backup, int[] seqWavelengths_primary, int[] seqWavelengths_backup, double lineRateGbps) {
            this.demand = demand;
            this.seqLinks_primary = seqLinks_primary;
            this.lineRateGbps = lineRateGbps;
            this.seqWavelengths_primary = seqWavelengths_primary;
            this.seqLinks_backup = seqLinks_backup;
            this.seqWavelengths_backup = seqWavelengths_backup;
            this.ingressNode = demand.getIngressNode();
            this.egressNode = demand.getEgressNode();
            this.layer = demand.getLayer();
        }

        /**
         * Constructor to generate a new {@code LightpathAdd} object.
         *
         * @param ingressNode  Ingress node
         * @param egressNode   Egress node
         * @param layer        Network layer
         * @param lineRateGbps Line rate in Gbps
         */
        public LightpathAdd(Node ingressNode, Node egressNode, NetworkLayer layer, double lineRateGbps) {
            this.demand = null;
            this.seqLinks_primary = null;
            this.seqLinks_backup = null;
            this.lineRateGbps = lineRateGbps;
            this.seqWavelengths_primary = null;
            this.seqWavelengths_backup = null;
            this.ingressNode = ingressNode;
            this.egressNode = egressNode;
            this.layer = layer;
        }

        /**
         * Constructor to generate a new {@code LightpathAdd} object.
         *
         * @param ingressNode    Ingress node
         * @param egressNode     Egress node
         * @param layer          Network layer
         * @param seqLinks       Sequence of fibers
         * @param seqWavelengths Sequence of wavelengths
         * @param lineRateGbps   Line rate in Gbps
         */
        public LightpathAdd(Node ingressNode, Node egressNode, NetworkLayer layer, List<Link> seqLinks, int[] seqWavelengths, double lineRateGbps) {
            this.demand = null;
            this.seqLinks_primary = seqLinks;
            this.seqLinks_backup = null;
            this.lineRateGbps = lineRateGbps;
            this.seqWavelengths_primary = seqWavelengths;
            this.seqWavelengths_backup = null;
            this.ingressNode = ingressNode;
            this.egressNode = egressNode;
            this.layer = layer;
        }

        /**
         * Constructor to generate a new {@code LightpathAdd} object.
         *
         * @param ingressNode            Ingress node
         * @param egressNode             Egress node
         * @param layer                  Network layer
         * @param seqLinks_primary       Primary sequence of links
         * @param seqLinks_backup        Backup sequence of links
         * @param seqWavelengths_primary Primary sequence of wavelengths
         * @param seqWavelengths_backup  Backup sequence of wavelengths
         * @param lineRateGbps           Line rate in Gbps
         */
        public LightpathAdd(Node ingressNode, Node egressNode, NetworkLayer layer, List<Link> seqLinks_primary, List<Link> seqLinks_backup, int[] seqWavelengths_primary, int[]
                seqWavelengths_backup, double lineRateGbps) {
            this.demand = null;
            this.seqLinks_primary = seqLinks_primary;
            this.seqLinks_backup = seqLinks_backup;
            this.lineRateGbps = lineRateGbps;
            this.seqWavelengths_primary = seqWavelengths_primary;
            this.seqWavelengths_backup = seqWavelengths_backup;
            this.ingressNode = ingressNode;
            this.egressNode = egressNode;
            this.layer = layer;
        }
    }

    ;

    /**
     * This class represents the request to remove an existing lightpath. It is used in online algorithms related to WDM networks, inside {@code SimEvent} objects.
     */
    public static class LightpathRemove {
        public final Route lp;

        /**
         * Constructor to generate a new {@code LightpathRemove} object.
         *
         * @param lp Route
         */
        public LightpathRemove(Route lp) {
            this.lp = lp;
        }
    }

    ;

    /**
     * This class represents the request to modify an existing lightpath. It is used in online algorithms related to WDM networks, inside {@code SimEvent} objects.
     */
    public static class LightpathModify {
        public final Route lp;
        public final List<Link> seqLinks;
        public final int[] seqWavelengths;
        public final double carriedTraffic;
        public final double
                occupiedLinkCapacity;

        /**
         * Constructor to generate a new {@code LightpathModify} object.
         *
         * @param lp                   Route to modify
         * @param seqLinks             New sequence of fibers
         * @param carriedTraffic       New carried traffic
         * @param occupiedLinkCapacity New occupied link capacity
         * @param seqWavelengths       New sequence of wavelengths
         */
        public LightpathModify(Route lp, List<Link> seqLinks, double carriedTraffic, double occupiedLinkCapacity, int[] seqWavelengths) {
            this.lp = lp;
            this.seqLinks = seqLinks;
            this
                    .carriedTraffic = carriedTraffic;
            this.occupiedLinkCapacity = occupiedLinkCapacity;
            this.seqWavelengths = seqWavelengths;
        }
    }

    ;

    /**
     * Route/protection segment attribute name for sequence of regenerators.
     *
     * @since 0.3.0
     */
    public final static String SEQUENCE_OF_REGENERATORS_ATTRIBUTE_NAME = "seqRegenerators";

    /**
     * Route/protection segment attribute name for sequence of wavelengths.
     *
     * @since 0.3.0
     */
    public final static String SEQUENCE_OF_WAVELENGTHS_ATTRIBUTE_NAME = "seqWavelengths";

    /**
     * Route/protection segment attribute name for sequence of wavelengths for the initial sequence of links (when the route was created)
     *
     * @since 0.3.0
     */
    public final static String SEQUENCE_OF_WAVELENGTHS_INITIAL_ROUTE_ATTRIBUTE_NAME = "seqWavelengthsInitialRoute";

    private static class WDMException extends Net2PlanException {
        public WDMException(String message) {
            super("WDM: " + message);
        }
    }

    private WDMUtils() {
    }

    /**
     * Creates a new lightpath and updates the wavelength occupancy.
     *
     * @param demand                   Demand
     * @param seqFibers                Sequence of fibers
     * @param binaryRatePerChannel     Binary rate per channel in Gbps
     * @param wavelengthId             Wavelength identifier (the same for all traversed fibers)
     * @param wavelengthFiberOccupancy Occupied fibers in each wavelength
     * @return The newly create lightpath (as a route)
     */
    public static Route addLightpathAndUpdateOccupancy(Demand demand, List<Link> seqFibers, double binaryRatePerChannel, int wavelengthId, DoubleMatrix2D wavelengthFiberOccupancy) {
        return addLightpathAndUpdateOccupancy(demand, seqFibers, binaryRatePerChannel, IntUtils.constantArray(seqFibers.size(), wavelengthId), null, wavelengthFiberOccupancy, null);
    }

    /**
     * Creates a new lightpath and updates the wavelength occupancy.
     *
     * @param demand                   Demand
     * @param seqFibers                Sequence of fibers
     * @param binaryRatePerChannel     Binary rate per channel in Gbps
     * @param seqWavelengths           Sequence of wavelengths
     * @param wavelengthFiberOccupancy Occupied fibers in each wavelength
     * @return The newly create lightpath (as a route)
     */
    public static Route addLightpathAndUpdateOccupancy(Demand demand, List<Link> seqFibers, double binaryRatePerChannel, int[] seqWavelengths, DoubleMatrix2D wavelengthFiberOccupancy) {
        return addLightpathAndUpdateOccupancy(demand, seqFibers, binaryRatePerChannel, seqWavelengths, null, wavelengthFiberOccupancy, null);
    }

    /**
     * Creates a new lightpath and updates the wavelength occupancy.
     *
     * @param demand                   Demand
     * @param seqFibers                Sequence of fibers
     * @param binaryRatePerChannel     Binary rate per channel in Gbps
     * @param seqWavelengths           Sequence of wavelengths
     * @param seqRegenerators          Sequence of regenerators
     * @param wavelengthFiberOccupancy Occupied fibers in each wavelength
     * @param nodeRegeneratorOccupancy Number of regenerators installed per node
     * @return The newly create lightpath (as a route)
     */
    public static Route addLightpathAndUpdateOccupancy(Demand demand, List<Link> seqFibers, double binaryRatePerChannel, int[] seqWavelengths, int[] seqRegenerators, DoubleMatrix2D
            wavelengthFiberOccupancy, DoubleMatrix1D nodeRegeneratorOccupancy) {
        NetPlan np = demand.getNetPlan();
        if (seqWavelengths == null) throw new Net2PlanException("Specify the wavelengths");
        if (seqWavelengths.length != seqFibers.size()) throw new Net2PlanException("Wrong wavelength array size");
        Route lp = np.addRoute(demand, binaryRatePerChannel, 1, seqFibers, null);
        allocateResources(seqFibers, seqWavelengths, wavelengthFiberOccupancy, seqRegenerators, nodeRegeneratorOccupancy);
        setLightpathSeqWavelengths(lp, seqWavelengths);
        setLightpathSeqWavelengthsInitialRoute(lp, seqWavelengths);
        if (seqRegenerators != null) if (seqRegenerators.length != 0) setLightpathSeqRegenerators(lp, seqRegenerators);
        return lp;
    }

    /**
     * Creates a new protection lightpath and updates the wavelength occupancy.
     *
     * @param seqFibers                Sequence of fibers
     * @param wavelengthId             Wavelength identifier (the same for all traversed fibers)
     * @param wavelengthFiberOccupancy Occupied fibers in each wavelength
     * @return The newly created lightpath (as a protection segment)
     */
    public static ProtectionSegment addLightpathAsProtectionSegmentAndUpdateOccupancy(List<Link> seqFibers, int wavelengthId, DoubleMatrix2D wavelengthFiberOccupancy) {
        return addLightpathAsProtectionSegmentAndUpdateOccupancy(seqFibers, IntUtils.constantArray(seqFibers.size(), wavelengthId), null, wavelengthFiberOccupancy, null);
    }

    /**
     * Creates a new protection lightpath and updates the wavelength occupancy.
     *
     * @param seqFibers                Sequence of fibers
     * @param seqWavelengths           Sequence of wavelengths
     * @param wavelengthFiberOccupancy Occupied fibers in each wavelength
     * @return The newly created lightpath (as a protection segment)
     */
    public static ProtectionSegment addLightpathAsProtectionSegmentAndUpdateOccupancy(List<Link> seqFibers, int[] seqWavelengths, DoubleMatrix2D wavelengthFiberOccupancy) {
        return addLightpathAsProtectionSegmentAndUpdateOccupancy(seqFibers, seqWavelengths, null, wavelengthFiberOccupancy, null);
    }

    /**
     * Creates a new protection lightpath and updates the wavelength occupancy.
     *
     * @param seqFibers                Sequence of fibers
     * @param seqWavelengths           Sequence of wavelengths
     * @param seqRegenerators          Sequence of regeneratos
     * @param wavelengthFiberOccupancy Occupied fibers in each wavelength
     * @param nodeRegeneratorOccupancy Number of regenerators installed per node
     * @return The newly created lightpath (as a protection segment)
     */
    public static ProtectionSegment addLightpathAsProtectionSegmentAndUpdateOccupancy(List<Link> seqFibers, int[] seqWavelengths, int[] seqRegenerators, DoubleMatrix2D wavelengthFiberOccupancy,
                                                                                      DoubleMatrix1D nodeRegeneratorOccupancy) {
        if (seqWavelengths == null) throw new Net2PlanException("Specify the wavelengths");
        if (seqWavelengths.length != seqFibers.size()) throw new Net2PlanException("Wrong wavelength array size");
        if (seqFibers.isEmpty()) throw new Net2PlanException("E");
        NetPlan np = seqFibers.get(0).getNetPlan();
        ProtectionSegment lp = np.addProtectionSegment(seqFibers, 1, null);
        allocateResources(seqFibers, seqWavelengths, wavelengthFiberOccupancy, seqRegenerators, nodeRegeneratorOccupancy);
//		System.out.println ("addLightpathAsProtectionSegmentAndUpdateOccupancy: seqwavelength: " + Arrays.toString (seqWavelengths));
        setLightpathSeqWavelengths(lp, seqWavelengths);
//		System.out.println ("after: " + Arrays.toString (WDMUtils.getLightpathSeqWavelengths(lp)));
        if (seqRegenerators != null) if (seqRegenerators.length != 0) setLightpathSeqRegenerators(lp, seqRegenerators);
//		System.out.println ("afterrrrrr: " + Arrays.toString (WDMUtils.getLightpathSeqWavelengths(lp)));
        return lp;
    }

    /**
     * <p>Performs extra checks to those applicable to every network design, especially
     * focused on WDM networks.</p>
     *
     * @param netPlan                     A {@link com.net2plan.interfaces.networkDesign.NetPlan} representing a WDM physical topology
     * @param countDownLightpathResources Wheter or not include lightpaths that are down
     * @param optionalLayerParameter      Network layer (optional)
     */
    public static void checkConsistency(NetPlan netPlan, boolean countDownLightpathResources, NetworkLayer... optionalLayerParameter) {
        NetworkLayer layer = netPlan.checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);

        getVectorFiberNumWavelengths(netPlan);

        Collection<Route> lpRoutes = netPlan.getRoutes(layer);
        for (Route lpRoute : lpRoutes) {
            int numHops_thisLp = lpRoute.getNumberOfHops();
            int[] seqWavelengths_thisLp = getLightpathSeqWavelengths(lpRoute);
            int[] seqRegenerators_thisLp = getLightpathSeqRegenerators(lpRoute);

            if (lpRoute.isDown()) {
//				if (seqWavelengths_thisLp.length != 0) throw new WDMException("Failing lightpath route " + lpRoute.getId () + " must not occupy wavelength resources");
//				if (seqRegenerators_thisLp.length != 0) throw new WDMException("Failing lightpath route " + lpRoute.getId () + " must not occupy regenerator resources");
            } else {
                double y_p_thisRoute = lpRoute.getOccupiedCapacity();
                if (y_p_thisRoute != 1)
                    throw new WDMException("Lightpath route " + lpRoute.getId() + " must occupy only one wavelength per fiber (current value: " + y_p_thisRoute + ")");
                if (seqWavelengths_thisLp.length != numHops_thisLp)
                    throw new WDMException("Length of sequence of wavelengths for lightpath " + lpRoute.getId() + " does not match the number of traversed fibers");
                if (seqRegenerators_thisLp.length > 0 && seqRegenerators_thisLp.length != numHops_thisLp)
                    throw new WDMException("Length of sequence of regenerators/wavelength converters for lightpath " + lpRoute.getId() + " does not match the number of traversed fibers");
            }
        }

        Collection<ProtectionSegment> protectionLps = netPlan.getProtectionSegments(layer);
        for (ProtectionSegment protectionLp : protectionLps) {
            double u_e_thisSegment = protectionLp.getReservedCapacityForProtection();
            if (u_e_thisSegment != 1)
                throw new WDMException("Protection lightpath " + protectionLp.getId() + " must occupy only one wavelength per fiber");

            int numHops_thisProtectionLp = protectionLp.getNumberOfHops();
            int[] seqWavelengths_thisProtectionLp = getProtectionLightpathSeqWavelengths(protectionLp);
            int[] seqRegenerators_thisProtectionLp = getProtectionLightpathSeqRegenerators(protectionLp);

            if (protectionLp.isDown()) {
//				if (seqWavelengths_thisProtectionLp.length != 0) throw new WDMException("Failing protection lightpath " + protectionLp.getId () + " must not occupy wavelength resources");
//				if (seqRegenerators_thisProtectionLp.length != 0) throw new WDMException("Failing protection lightpath " + protectionLp.getId () + " must not occupy regenerator resources");
            } else {
                if (seqWavelengths_thisProtectionLp.length != numHops_thisProtectionLp)
                    throw new WDMException("Length of sequence of wavelengths for protection lightpath " + protectionLp.getId() + " does not match the number of traversed fibers");
                if (seqRegenerators_thisProtectionLp.length > 0 && seqRegenerators_thisProtectionLp.length != numHops_thisProtectionLp)
                    throw new WDMException("Length of sequence of regenerators/wavelength converters for protection lightpath " + protectionLp.getId() + " does not match the number of traversed fibers");
            }
        }

        getVectorNodeRegeneratorOccupancy(netPlan, countDownLightpathResources, layer);
        getMatrixWavelength2FiberOccupancy(netPlan, countDownLightpathResources, layer);
    }

    /**
     * Returns the list of nodes within the lightpath route containing a regenerator,
     * only following a distance criterium, assuming no wavelength conversion is required.
     *
     * @param seqFibers                  Sequence of traversed fibers
     * @param maxRegeneratorDistanceInKm Maximum regeneration distance
     * @param l_f                        Physical length in km per fiber
     * @return A vector with as many elements as traversed links in the route/segment. Each element is a 1 if an optical regenerator is used at the origin node of the corresponding link, and a 0 if not. First element is always 0.
     */
    public static int[] computeRegeneratorPositions(List<Link> seqFibers, DoubleMatrix1D l_f, double maxRegeneratorDistanceInKm) {
        int numHops = seqFibers.size();

        double accumDistance = 0;
        int[] seqRegenerators = new int[numHops];

        ListIterator<Link> it = seqFibers.listIterator();
        while (it.hasNext()) {
            int hopId = it.nextIndex();
            Link fiber = it.next();
            double fiberLengthInKm = l_f.get(fiber.getIndex());

            if (fiberLengthInKm > maxRegeneratorDistanceInKm)
                throw new WDMException(String.format("Fiber %d is longer (%f km) than the maximum distance without regenerators (%f km)", fiber.getId(), fiberLengthInKm, maxRegeneratorDistanceInKm));

            accumDistance += fiberLengthInKm;

            if (accumDistance > maxRegeneratorDistanceInKm) {
                seqRegenerators[hopId] = 1;
                accumDistance = fiberLengthInKm;
            } else {
                seqRegenerators[hopId] = 0;
            }
        }

        return seqRegenerators;
    }

    /**
     * Returns the list of nodes within the lightpath route containing a regenerator,
     * only following a distance criterium, assuming wavelength conversion is required.
     *
     * @param seqFibers                  Sequence of traversed fibers
     * @param seqWavelengths             Sequence of wavelengths (as many as the number of links in the lightpath)
     * @param l_f                        Physical length in km per fiber
     * @param maxRegeneratorDistanceInKm Maximum regeneration distance
     * @return A vector with as many elements as traversed links in the route/segment. Each element is a 1 if an optical regenerator is used at the origin node of the corresponding link, and a 0 if not. First element is always 0.
     */
    public static int[] computeRegeneratorPositions(List<Link> seqFibers, int[] seqWavelengths, DoubleMatrix1D l_f, double maxRegeneratorDistanceInKm) {
        int numHops = seqFibers.size();

        double accumDistance = 0;
        int[] seqRegenerators = new int[numHops];

        ListIterator<Link> it = seqFibers.listIterator();
        while (it.hasNext()) {
            int hopId = it.nextIndex();
            Link fiber = it.next();
            double fiberLengthInKm = l_f.get(fiber.getIndex());

            if (fiberLengthInKm > maxRegeneratorDistanceInKm) {
                throw new WDMException(String.format("Fiber %d is longer (%f km) than the maximum distance without regenerators (%f km)", fiber.getId(), fiberLengthInKm, maxRegeneratorDistanceInKm));
            }

            accumDistance += fiberLengthInKm;

            if (accumDistance > maxRegeneratorDistanceInKm || (hopId > 0 && seqWavelengths[hopId - 1] != seqWavelengths[hopId])) {
                seqRegenerators[hopId] = 1;
                accumDistance = fiberLengthInKm;
            } else {
                seqRegenerators[hopId] = 0;
            }
        }

        return seqRegenerators;
    }

    /**
     * Returns the number of wavelengths for the given fiber. It is equivalent to
     * the method {@link Link#getCapacity() getCapacity()}
     * from the {@link com.net2plan.interfaces.networkDesign.Link Link} object,
     * but converting capacity value from {@code double} to {@code int}.
     *
     * @param fiber Link fiber
     * @return Number of wavelengths
     */
    public static int getFiberNumWavelengths(Link fiber) {
        double u_e = fiber.getCapacity();
        int w_f_thisFiber = (int) u_e;
        if (Math.abs(u_e - w_f_thisFiber) > 1e-6)
            throw new WDMException("Link capacity must be a non-negative integer representing the number of wavelengths of the fiber");
        return w_f_thisFiber;
    }


    /**
     * Returns the total number of wavelengths in each fiber.
     *
     * @param netPlan                A {@link com.net2plan.interfaces.networkDesign.NetPlan} representing a WDM physical topology
     * @param optionalLayerParameter Network layer (optional)
     * @return Number of wavelengths per fiber
     */
    public static DoubleMatrix1D getVectorFiberNumWavelengths(NetPlan netPlan, NetworkLayer... optionalLayerParameter) {
        NetworkLayer layer = netPlan.checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
        Collection<Link> fibers = netPlan.getLinks(layer);
        DoubleMatrix1D w_f = DoubleFactory1D.dense.make(fibers.size());
        for (Link fiber : fibers) w_f.set(fiber.getIndex(), getFiberNumWavelengths(fiber));
        return w_f;
    }

    /**
     * Returns {@code true} if the given sequence of wavelengths has not been allocated in the given sequence of links, {@code false} otherwise.
     *
     * @param links                    Sequence of links
     * @param seqWavelengths           Sequence of wavelengths
     * @param wavelengthFiberOccupancy Occupied fibers in each wavelength
     * @return See description above
     */
    public static boolean isNonConflictingRWA(List<Link> links, int[] seqWavelengths, DoubleMatrix2D wavelengthFiberOccupancy) {
        int counter = 0;
        for (Link e : links)
            if (wavelengthFiberOccupancy.get(seqWavelengths[counter++], e.getIndex()) != 0) return false;
        return true;
    }

    /**
     * Returns {@code true} if the given pair of sequences of wavelengths has not been allocated in the given pair of sequences of links, {@code false} otherwise.
     *
     * @param linksPrimary             Primary sequence of links
     * @param seqWavelengthsPrimary    Primary sequence of wavelengths
     * @param linksBackup              Backup sequence of links
     * @param seqWavelengthsBackup     Backup sequence of wavelengths
     * @param wavelengthFiberOccupancy Occupied fibers in each wavelength
     * @return See description above
     */
    public static boolean isNonConflictingRWAPair(List<Link> linksPrimary, int[] seqWavelengthsPrimary, List<Link> linksBackup, int[] seqWavelengthsBackup, DoubleMatrix2D wavelengthFiberOccupancy) {
        DoubleMatrix2D checkMatrix = DoubleFactory2D.sparse.make(wavelengthFiberOccupancy.rows(), wavelengthFiberOccupancy.columns());
        int counter = 0;
        for (Link e : linksPrimary) {
            if (wavelengthFiberOccupancy.get(seqWavelengthsPrimary[counter], e.getIndex()) != 0) return false;
            if (checkMatrix.get(seqWavelengthsPrimary[counter], e.getIndex()) != 0) return false;
            checkMatrix.set(seqWavelengthsPrimary[counter++], e.getIndex(), 1.0);
        }
        counter = 0;
        for (Link e : linksBackup) {
            if (wavelengthFiberOccupancy.get(seqWavelengthsBackup[counter], e.getIndex()) != 0) return false;
            if (checkMatrix.get(seqWavelengthsBackup[counter], e.getIndex()) != 0) return false;
            checkMatrix.set(seqWavelengthsBackup[counter++], e.getIndex(), 1.0);
        }
        return true;
    }

    /**
     * Returns the sequence of regenerators/wavelength converters for the given lightpath.
     *
     * @param route Ligthpath (as a route)
     * @return A 0-1 array indicating whether (1) or not (0) a regenerator/wavelength converter is required at the origin node of the corresponding fiber
     */
    public static int[] getLightpathSeqRegenerators(Route route) {
        return parseSeqRegenerators(route.getAttributes());
    }

    /**
     * Returns the sequence of regenerators/wavelength converters for the given lightpath.
     *
     * @param segment Lightpath (as a protection segment)
     * @return A 0-1 array indicating whether (1) or not (0) a regenerator/wavelength converter is required at the origin node of the corresponding fiber
     */
    public static int[] getLightpathSeqRegenerators(ProtectionSegment segment) {
        return parseSeqRegenerators(segment.getAttributes());
    }

    /**
     * Returns the sequence of wavelengths for the given lightpath.
     *
     * @param route Lightpath (as a route)
     * @return Sequence of wavelengths
     */
    public static int[] getLightpathSeqWavelengths(Route route) {
        final int[] seq = parseSeqWavelengths(route.getAttributes());
        if (seq.length != route.getSeqLinksRealPath().size())
            throw new Net2PlanException("Wrong size of the sequence of wavelengths");
        return seq;
    }

    /**
     * Returns the initial sequence of wavelengths for the given lightpath (when it was created).
     *
     * @param route Lightpath (as a route)
     * @return Sequence of wavelengths
     */
    public static int[] getLightpathSeqWavelengthsInitialRoute(Route route) {
        final int[] seq = parseSeqWavelengthsInitialRoute(route.getAttributes());
        if (seq.length != route.getInitialSequenceOfLinks().size())
            throw new Net2PlanException("Wrong size of the sequence of wavelengths");
        return seq;
    }

    /**
     * Returns the sequence of wavelengths for the given lightpath.
     *
     * @param segment Lightpath (as a protection sement)
     * @return Sequence of wavelengths
     */
    public static int[] getLightpathSeqWavelengths(ProtectionSegment segment) {
        final int[] seq = parseSeqWavelengths(segment.getAttributes());
        if (seq.length != segment.getSeqLinks().size())
            throw new Net2PlanException("Wrong size of the sequence of wavelengths");
        return seq;
    }

    /**
     * Returns the fiber occupied (columns) in each wavelength (rows).
     *
     * @param netPlan                     Current design
     * @param countDownLightpathResources Include lightpaths that are down
     * @param optionalLayerParameter      Network layer (optional)
     * @return Fibers occupied in each wavelength
     */
    public static DoubleMatrix2D getMatrixWavelength2FiberOccupancy(NetPlan netPlan, boolean countDownLightpathResources, NetworkLayer... optionalLayerParameter) {
        NetworkLayer layer = netPlan.checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
        final int E = netPlan.getLinks(layer).size();
        DoubleMatrix1D w_f = getVectorFiberNumWavelengths(netPlan, layer);
        final int W = w_f.size() == 0 ? 0 : (int) w_f.getMaxLocation()[0];
        DoubleMatrix2D wavelengthFiberOccupancy = DoubleFactory2D.dense.make(W, E);
        /* The wavelengths above the maximum number of wavelengths of a fiber, are set as occupied */
        for (int e = 0; e < E; e++) for (int w = (int) w_f.get(e); w < W; w++) wavelengthFiberOccupancy.set(e, w, 1);

		/* Wavlengths occupied by the lightpaths as routes */
        for (Route lpRoute : netPlan.getRoutes(layer)) {
            if (!countDownLightpathResources && lpRoute.isDown()) continue;
            if (lpRoute.getOccupiedCapacityInNoFailureState() == 0) continue;

            List<Link> seqFibers = lpRoute.getSeqLinksRealPath();
            int[] seqWavelengths = getLightpathSeqWavelengths(lpRoute);

            for (int hopIndex = 0; hopIndex < seqFibers.size(); hopIndex++) {
                Link fiber = seqFibers.get(hopIndex);
                int wavelengthId = seqWavelengths[hopIndex];

                int numWavelengths_thisFiber = (int) w_f.get(fiber.getIndex());
                if (numWavelengths_thisFiber <= wavelengthId)
                    throw new WDMException(String.format("Fiber %d only has %d wavelengths (lightpath %d, wavelength %d)", fiber.getId(), numWavelengths_thisFiber, lpRoute.getId(), wavelengthId));

                if (wavelengthFiberOccupancy.get(wavelengthId, fiber.getIndex()) != 0)
                    throw new WDMException(String.format("Two lightpaths/segments cannot share a wavelength (fiber %d, wavelength %d)", fiber.getId(), wavelengthId));

                wavelengthFiberOccupancy.set(wavelengthId, fiber.getIndex(), 1.0);
            }
        }

		/* Wavlengths occupied by the lightpaths as protection segments */
        for (ProtectionSegment segment : netPlan.getProtectionSegments(layer)) {
            if (segment.isDown()) continue;
            if (segment.getReservedCapacityForProtection() == 0) continue;
            if (segment.getTraversingRoutes().size() == 1) continue; // its occupancy was already updated
            if (segment.getTraversingRoutes().size() > 1) throw new RuntimeException("Bad");

            List<Link> seqFibers = segment.getSeqLinks();
            int[] seqWavelengths = getProtectionLightpathSeqWavelengths(segment);

            for (int hopIndex = 0; hopIndex < seqFibers.size(); hopIndex++) {
                Link fiber = seqFibers.get(hopIndex);
                int wavelengthId = seqWavelengths[hopIndex];

                int numWavelengths_thisFiber = (int) w_f.get(fiber.getIndex());
                if (numWavelengths_thisFiber <= wavelengthId)
                    throw new WDMException(String.format("Fiber %d only has %d wavelengths (segment %d, wavelength %d)", fiber.getId(), numWavelengths_thisFiber, segment.getId(), wavelengthId));

                if (wavelengthFiberOccupancy.get(wavelengthId, fiber.getIndex()) != 0) {
                    System.out.println("segment.getLInks(): " + segment.getSeqLinks());
                    System.out.println("segment.getAssociatedRoutesToWhichIsBackup(): " + segment.getAssociatedRoutesToWhichIsBackup());
                    for (Route r : segment.getAssociatedRoutesToWhichIsBackup()) {
                        System.out.println("r.getSeqLinksAndProtectionSegments(): " + r.getSeqLinksAndProtectionSegments());
                        System.out.println("r.getSeqLinksRealPath(): " + r.getSeqLinksRealPath());
                        System.out.println("r.getSeqLinksAndProtectionSegments(): " + r.getSeqLinksAndProtectionSegments());
                    }
                    throw new WDMException(String.format("Two lightpaths/segments cannot share a wavelength (fiber %d, wavelength %d)", fiber.getId(), wavelengthId));
                }
                wavelengthFiberOccupancy.set(wavelengthId, fiber.getIndex(), 1.0);
            }
        }

        return wavelengthFiberOccupancy;
    }

    /**
     * Returns the number of regenerators installed per node.
     *
     * @param netPlan                     A {@link com.net2plan.interfaces.networkDesign.NetPlan} representing a physical topology
     * @param countDownLightpathResources Wheter or not include lightpaths that are down
     * @param optionalLayerParameter      Network layer (optional)
     * @return Number of regenerators installed per node
     */
    public static DoubleMatrix1D getVectorNodeRegeneratorOccupancy(NetPlan netPlan, boolean countDownLightpathResources, NetworkLayer... optionalLayerParameter) {
        NetworkLayer layer = netPlan.checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
        List<Node> nodes = netPlan.getNodes();
        DoubleMatrix1D regeneratorOccupancy = DoubleFactory1D.dense.make(nodes.size());

        List<Route> lightpaths = netPlan.getRoutes(layer);
        List<ProtectionSegment> segments = netPlan.getProtectionSegments(layer);
        for (Route lightpath : lightpaths) {
            if (!countDownLightpathResources && lightpath.isDown()) continue;
            List<Link> seqFibers = lightpath.getSeqLinksRealPath();
            int[] seqRegenerators = getLightpathSeqRegenerators(lightpath);
            if (seqRegenerators.length == 0) {
                continue;
            }

            for (int hopIndex = 0; hopIndex < seqFibers.size(); hopIndex++) {
                if (seqRegenerators[hopIndex] == 0) {
                    continue;
                }

                Link fiber = seqFibers.get(hopIndex);
                Node node = fiber.getOriginNode();
                regeneratorOccupancy.set(node.getIndex(), regeneratorOccupancy.get(node.getIndex()) + 1);
            }
        }

        for (ProtectionSegment segment : segments) {
            if (!countDownLightpathResources && segment.isDown()) continue;
            List<Link> seqFibers = segment.getSeqLinks();
            int[] seqRegenerators = getProtectionLightpathSeqRegenerators(segment);
            if (seqRegenerators.length == 0) continue;

            for (int hopIndex = 0; hopIndex < seqFibers.size(); hopIndex++) {
                if (seqRegenerators[hopIndex] == 0) continue;

                Link fiber = seqFibers.get(hopIndex);
                Node node = fiber.getOriginNode();
                regeneratorOccupancy.set(node.getIndex(), regeneratorOccupancy.get(node.getIndex()) + 1);
            }
        }

        return regeneratorOccupancy;
    }

    /**
     * Returns the sequence of regenerators/wavelength converters for the given lightpath.
     *
     * @param segment Lightpath (as a protection segment)
     * @return A 0-1 array indicating whether (1) or not (0) a regenerator/wavelength converter is required at the origin node of the corresponding fiber
     */
    public static int[] getProtectionLightpathSeqRegenerators(ProtectionSegment segment) {
        return parseSeqRegenerators(segment.getAttributes());
    }

    /**
     * Returns the sequence of wavelengths for a given protection lightpath.
     *
     * @param segment Lightpath (as a protection segment)
     * @return Sequence of wavelengths
     */
    public static int[] getProtectionLightpathSeqWavelengths(ProtectionSegment segment) {
        return parseSeqWavelengths(segment.getAttributes());
    }

    /**
     * Returns the sequence of regenerators/wavelength converters from an attribute
     * map, where the associated key corresponds to {@link #SEQUENCE_OF_REGENERATORS_ATTRIBUTE_NAME}.
     *
     * @param attributeMap Attribute map, where key is the attribute name and the value is the parameter value
     * @return Sequence of regenerators/wavelength converters
     */
    public static int[] parseSeqRegenerators(Map<String, String> attributeMap) {
        String attributeValue = attributeMap.get(SEQUENCE_OF_REGENERATORS_ATTRIBUTE_NAME);
        if (attributeValue == null) return new int[0];

        return StringUtils.toIntArray(StringUtils.split(attributeValue, " "));
    }

    /**
     * Returns the sequence of wavelengths from an attribute map, where the associated
     * key corresponds to {@link #SEQUENCE_OF_WAVELENGTHS_ATTRIBUTE_NAME}.
     *
     * @param attributeMap Attribute map, where key is the attribute name and the value is the parameter value
     * @return Sequence of wavelengths
     */
    public static int[] parseSeqWavelengths(Map<String, String> attributeMap) {
        String attributeValue = attributeMap.get(SEQUENCE_OF_WAVELENGTHS_ATTRIBUTE_NAME);
        if (attributeValue == null)
            throw new Net2PlanException("No '" + SEQUENCE_OF_WAVELENGTHS_ATTRIBUTE_NAME + "' attribute defined");

        return StringUtils.toIntArray(StringUtils.split(attributeMap.get(SEQUENCE_OF_WAVELENGTHS_ATTRIBUTE_NAME), " "));
    }

    /**
     * Returns the initial sequence of wavelengths from an attribute map, where the associated
     * key corresponds to {@link #SEQUENCE_OF_WAVELENGTHS_ATTRIBUTE_NAME}.
     *
     * @param attributeMap Attribute map, where key is the attribute name and the value is the parameter value
     * @return Initial sequence of wavelengths
     */
    public static int[] parseSeqWavelengthsInitialRoute(Map<String, String> attributeMap) {
        String attributeValue = attributeMap.get(SEQUENCE_OF_WAVELENGTHS_INITIAL_ROUTE_ATTRIBUTE_NAME);
        if (attributeValue == null)
            throw new Net2PlanException("No '" + SEQUENCE_OF_WAVELENGTHS_INITIAL_ROUTE_ATTRIBUTE_NAME + "' attribute defined");

        return StringUtils.toIntArray(StringUtils.split(attributeMap.get(SEQUENCE_OF_WAVELENGTHS_INITIAL_ROUTE_ATTRIBUTE_NAME), " "));
    }

    /**
     * Removes a lightpath and updates the occupancy.
     *
     * @param lp                              Lightpath (as a route)
     * @param wavelengthFiberOccupancy        Occupied fibers in each wavelength
     * @param nodeRegeneratorOccupancy        Number of regenerators installed per node
     * @param releaseResourcesOfOriginalRoute Wheter or not release the initial or the current resources
     */
    public static void removeLightpathAndUpdateOccupancy(Route lp, DoubleMatrix2D wavelengthFiberOccupancy, DoubleMatrix1D nodeRegeneratorOccupancy, boolean releaseResourcesOfOriginalRoute) {
        releaseResources(releaseResourcesOfOriginalRoute ? lp.getInitialSequenceOfLinks() : lp.getSeqLinksRealPath(), WDMUtils.getLightpathSeqWavelengths(lp), wavelengthFiberOccupancy, WDMUtils.getLightpathSeqRegenerators(lp), nodeRegeneratorOccupancy);
        lp.remove();
    }

    /**
     * Removes a lightpath and updates the occupancy.
     *
     * @param lp                              Lightpath (as a route)
     * @param wavelengthFiberOccupancy        Occupied fibers in each wavelength
     * @param releaseResourcesOfOriginalRoute Wheter or not release the initial or the current resources
     */
    public static void removeLightpathAndUpdateOccupancy(Route lp, DoubleMatrix2D wavelengthFiberOccupancy, boolean releaseResourcesOfOriginalRoute) {
        releaseResources(releaseResourcesOfOriginalRoute ? lp.getInitialSequenceOfLinks() : lp.getSeqLinksRealPath(), WDMUtils.getLightpathSeqWavelengths(lp), wavelengthFiberOccupancy, null, null);
        lp.remove();
    }

    /**
     * Removes a protection lightpath and updates the occupancy.
     *
     * @param lp                       Protection lightpath (as a protection segment)
     * @param wavelengthFiberOccupancy Occupied fibers in each wavelength
     * @param nodeRegeneratorOccupancy Number of regenerators installed per node
     */
    public static void removeLightpathAndUpdateOccupancy(ProtectionSegment lp, DoubleMatrix2D wavelengthFiberOccupancy, DoubleMatrix1D nodeRegeneratorOccupancy) {
        releaseResources(lp.getSeqLinks(), WDMUtils.getLightpathSeqWavelengths(lp), wavelengthFiberOccupancy, WDMUtils.getLightpathSeqRegenerators(lp), nodeRegeneratorOccupancy);
        lp.remove();
    }

    /**
     * Removes a protection lightpath and updates the occupancy.
     *
     * @param lp                       Protection lightpath (as a protection segment)
     * @param wavelengthFiberOccupancy Number of regenerators installed per node
     */
    public static void removeLightpathAndUpdateOccupancy(ProtectionSegment lp, DoubleMatrix2D wavelengthFiberOccupancy) {
        releaseResources(lp.getSeqLinks(), WDMUtils.getLightpathSeqWavelengths(lp), wavelengthFiberOccupancy, null, null);
        lp.remove();
    }

    /**
     * Updates {@code wavelengthFiberOccupancy} to consider that a lightpath is releasing
     * used wavelengths.
     *
     * @param seqFibers                Sequence of traversed fibers
     * @param seqWavelengths           Sequence of wavelengths (as many as the number of links in the lightpath)
     * @param wavelengthFiberOccupancy Occupied fibers in each wavelength
     * @param seqRegenerators          A 0-1 array indicating whether (1) or not (0) a regenerator/wavelength converter is required at the origin node of the corresponding fiber
     * @param nodeRegeneratorOccupancy Number of regenerators installed per node
     */
    public static void releaseResources(List<Link> seqFibers, int[] seqWavelengths, DoubleMatrix2D wavelengthFiberOccupancy, int[] seqRegenerators, DoubleMatrix1D nodeRegeneratorOccupancy) {
        if (seqWavelengths.length == 0) return;

        ListIterator<Link> fiberIt = seqFibers.listIterator();
        while (fiberIt.hasNext()) {
            int hopId = fiberIt.nextIndex();
            Link fiber = fiberIt.next();
            int wavelengthId = seqWavelengths[hopId];
            boolean wasOccupied = wavelengthFiberOccupancy.get(wavelengthId, fiber.getIndex()) != 0;
            if (!wasOccupied)
                throw new WDMException("Wavelength " + wavelengthId + " was unused in fiber " + fiber.getId());
            wavelengthFiberOccupancy.set(wavelengthId, fiber.getIndex(), 0.0);
            if ((seqRegenerators != null) && (seqRegenerators.length == seqWavelengths.length) && (seqRegenerators[hopId] == 1)) {
                Node node = fiber.getOriginNode();
                nodeRegeneratorOccupancy.set(node.getIndex(), nodeRegeneratorOccupancy.get(node.getIndex()) - 1);
            }
        }
    }

    /**
     * Sets the number of wavelengths available on the given fiber.
     *
     * @param fiber          Link fiber
     * @param numWavelengths Number of wavelengths for the given fiber
     */
    public static void setFiberNumWavelengths(Link fiber, int numWavelengths) {
        if (numWavelengths < 0) throw new WDMException("'numWavelengths' must be a non-negative integer");
        fiber.setCapacity(numWavelengths);
    }

    /**
     * Sets the number of wavelengths available in each fiber to the same value.
     *
     * @param netPlan                A {@link com.net2plan.interfaces.networkDesign.NetPlan} representing a WDM physical topology
     * @param numWavelengths         Number of wavelengths for all fibers
     * @param optionalLayerParameter Network layer (optional)
     */
    public static void setFibersNumWavelengths(NetPlan netPlan, int numWavelengths, NetworkLayer... optionalLayerParameter) {
        NetworkLayer layer = netPlan.checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
        if (numWavelengths < 0) throw new WDMException("'numWavelengths' must be a non-negative integer");
        for (Link fiber : netPlan.getLinks(layer)) fiber.setCapacity(numWavelengths);
    }

    /**
     * Sets the number of wavelengths available in each fiber.
     *
     * @param netPlan                A {@link com.net2plan.interfaces.networkDesign.NetPlan} representing a WDM physical topology
     * @param optionalLayerParameter Network layer (optional)
     * @param w_f                    Number of wavelengths per fiber
     */
    public static void setFibersNumWavelengths(NetPlan netPlan, int[] w_f, NetworkLayer... optionalLayerParameter) {
        NetworkLayer layer = netPlan.checkInThisNetPlanOptionalLayerParameter(optionalLayerParameter);
        final int E = netPlan.getNumberOfLinks(layer);
        List<Link> fibers = netPlan.getLinks(layer);
        if (w_f.length != E) throw new Net2PlanException("Wrong array size");
        for (int w : w_f) if (w < 0) throw new WDMException("'numWavelengths' must be a non-negative integer");
        for (int e = 0; e < E; e++) fibers.get(e).setCapacity(w_f[e]);
    }

    /**
     * Sets the sequence of regenerators/wavelength converters for a given lightpath.
     *
     * @param lp              Lightpath (as a route)
     * @param seqRegenerators A 0-1 array indicating whether (1) or not (0) a regenerator/wavelength converter is required at the origin node of the corresponding fiber
     */
    public static void setLightpathSeqRegenerators(Route lp, int[] seqRegenerators) {
        lp.setAttribute(SEQUENCE_OF_REGENERATORS_ATTRIBUTE_NAME, IntUtils.join(seqRegenerators, " "));
    }

    /**
     * Sets the sequence of regenerators/wavelength converters for a given lightpath.
     *
     * @param lp              Lightpath (as a protection segment)
     * @param seqRegenerators A 0-1 array indicating whether (1) or not (0) a regenerator/wavelength converter is required at the origin node of the corresponding fiber
     */
    public static void setLightpathSeqRegenerators(ProtectionSegment lp, int[] seqRegenerators) {
        lp.setAttribute(SEQUENCE_OF_REGENERATORS_ATTRIBUTE_NAME, IntUtils.join(seqRegenerators, " "));
    }

    /**
     * Sets the current wavelength for the given lightpath, assuming no wavelength conversion.
     *
     * @param lp           Lightpath (as a route)
     * @param wavelengthId Wavelength identifier (the same for all traversed fibers)
     */
    public static void setLightpathSeqWavelengths(Route lp, int wavelengthId) {
        lp.setAttribute(SEQUENCE_OF_WAVELENGTHS_ATTRIBUTE_NAME, IntUtils.join(IntUtils.constantArray(lp.getNumberOfHops(), wavelengthId), " "));
    }

    /**
     * Sets the sequence of wavelengths for the given lightpath.
     *
     * @param lp             Lightpath (as a route)
     * @param seqWavelengths Sequence of wavelengths (as many as the number of links in the lightpath)
     */
    public static void setLightpathSeqWavelengths(Route lp, int[] seqWavelengths) {
        lp.setAttribute(SEQUENCE_OF_WAVELENGTHS_ATTRIBUTE_NAME, IntUtils.join(seqWavelengths, " "));
    }

    /**
     * Sets the initial sequence of wavelengths for the given lightpath.
     *
     * @param lp             Lightpath (as a route)
     * @param seqWavelengths Sequence of wavelengths (as many as the number of links in the lightpath)
     */
    public static void setLightpathSeqWavelengthsInitialRoute(Route lp, int[] seqWavelengths) {
        lp.setAttribute(SEQUENCE_OF_WAVELENGTHS_INITIAL_ROUTE_ATTRIBUTE_NAME, IntUtils.join(seqWavelengths, " "));
    }

    /**
     * Sets the current wavelength for the given lightpath, assuming no wavelength conversion.
     *
     * @param lp             Lightpath (as a route)
     * @param seqWavelengths Sequence of wavelengths
     */
    public static void setLightpathSeqWavelengths(ProtectionSegment lp, int[] seqWavelengths) {
        lp.setAttribute(SEQUENCE_OF_WAVELENGTHS_ATTRIBUTE_NAME, IntUtils.join(seqWavelengths, " "));
    }

    /**
     * Sets the sequence of regenerators/wavelength converters for a given protection lightpath.
     *
     * @param segment         Protection lightpath
     * @param seqRegenerators A 0-1 array indicating whether (1) or not (0) a regenerator/wavelength converter is required at the origin node of the corresponding fiber
     */
    public static void setProtectionLightpathSeqRegenerators(ProtectionSegment segment, int[] seqRegenerators) {
        segment.setAttribute(SEQUENCE_OF_REGENERATORS_ATTRIBUTE_NAME, IntUtils.join(seqRegenerators, " "));
    }

    /**
     * Sets the given wavelength for all fibers traversing a protection lightpath.
     *
     * @param segment      Protection lightpath
     * @param wavelengthId Wavelength identifier (the same for all traversed fibers)
     */
    public static void setProtectionLightpathSeqWavelengths(ProtectionSegment segment, int wavelengthId) {
        segment.setAttribute(SEQUENCE_OF_WAVELENGTHS_ATTRIBUTE_NAME, IntUtils.join(IntUtils.constantArray(segment.getNumberOfHops(), wavelengthId), " "));
    }

    /**
     * Sets the sequence of wavelengths for a protection lightpath.
     *
     * @param segment        Protection lightpath
     * @param seqWavelengths Sequence of wavelengths (as many as the number of links in the lightpath)
     */
    public static void setProtectionLightpathSeqWavelengths(ProtectionSegment segment, int[] seqWavelengths) {
        segment.setAttribute(SEQUENCE_OF_WAVELENGTHS_ATTRIBUTE_NAME, IntUtils.join(seqWavelengths, " "));
    }

    /**
     * <p>Wavelength assignment algorithm based on a first-fit fashion. Wavelengths
     * are indexed from 0 to <i>W<sub>f</sub></i>-1, where <i>W<sub>f</sub></i>
     * is the number of wavelengths supported by fiber <i>f</i>. Then, the wavelength
     * assigned to each lightpath (along the whole physical route) is the minimum
     * index among the common free-wavelength set for all traversed fibers.</p>
     * <p>
     * <p>In case a lightpath cannot be allocated, the output will be an empty array.</p>
     * <p>
     * <p><b>Important</b>: {@code wavelengthFiberOccupancy} is not updated, so
     * subsequent usage of {@link #allocateResources(List, int[], DoubleMatrix2D, int[], DoubleMatrix1D) allocateResources} method is encouraged.</p>
     *
     * @param seqFibers                Sequence of traversed fibers
     * @param wavelengthFiberOccupancy Occupied fibers in each wavelength
     * @return Sequence of wavelengths traversed by each lightpath
     */
    public static int[] WA_firstFit(List<Link> seqFibers, DoubleMatrix2D wavelengthFiberOccupancy) {
        GraphUtils.checkRouteContinuity(seqFibers, Constants.CheckRoutingCycleType.NO_REPEAT_LINK);
        final int W = wavelengthFiberOccupancy.rows();
        for (int wavelengthId = 0; wavelengthId < W; wavelengthId++) {
            boolean freePaths = true;
            for (Link link : seqFibers)
                if (wavelengthFiberOccupancy.get(wavelengthId, link.getIndex()) != 0) {
                    freePaths = false;
                    break;
                }
            if (!freePaths) continue;
            return IntFactory1D.dense.make(seqFibers.size(), wavelengthId).toArray();
        }
        return new int[0];
    }

    /**
     * <p>Wavelength assignment algorithm based on a first-fit fashion. Wavelengths
     * are indexed from 0 to <i>W<sub>f</sub></i>-1, where <i>W<sub>f</sub></i>
     * is the number of wavelengths supported by fiber <i>f</i>. Then, the wavelength
     * assigned to each lightpath (along the whole physical route) is the minimum
     * index among the common free-wavelength set for all traversed fibers.</p>
     * <p>
     * <p>In case a lightpath cannot be allocated, the output will be an empty array.</p>
     * <p>
     * <p><b>Important</b>: {@code wavelengthFiberOccupancy} is not updated, so
     * subsequent usage of {@link #allocateResources(List, int[], DoubleMatrix2D, int[], DoubleMatrix1D) allocateResources} method is encouraged.</p>
     *
     * @param seqFibers_1              First sequence of traversed fibers
     * @param seqFibers_2              Second sequence of traversed fibers
     * @param wavelengthFiberOccupancy Occupied fibers in each wavelength
     * @return Pair of sequences of wavelengths traversed by each lightpath
     */
    public static Pair<int[], int[]> WA_firstFitTwoRoutes(List<Link> seqFibers_1, List<Link> seqFibers_2, DoubleMatrix2D wavelengthFiberOccupancy) {
        GraphUtils.checkRouteContinuity(seqFibers_1, Constants.CheckRoutingCycleType.NO_REPEAT_LINK);
        GraphUtils.checkRouteContinuity(seqFibers_2, Constants.CheckRoutingCycleType.NO_REPEAT_LINK);
        final int W = wavelengthFiberOccupancy.rows();
        HashSet<Link> auxSet = new HashSet<Link>(seqFibers_1);
        auxSet.removeAll(seqFibers_2);
        final boolean haveLinksInCommon = !auxSet.isEmpty();
        for (int wavelengthId_1 = 0; wavelengthId_1 < W; wavelengthId_1++) {
            boolean freePath_1 = true;
            for (Link link : seqFibers_1)
                if (wavelengthFiberOccupancy.get(wavelengthId_1, link.getIndex()) != 0) {
                    freePath_1 = false;
                    break;
                }
            if (!freePath_1) continue;
            for (int wavelengthId_2 = 0; wavelengthId_2 < W; wavelengthId_2++) {
                if (haveLinksInCommon && (wavelengthId_1 == wavelengthId_2)) continue;
                boolean freePath_2 = true;
                for (Link link : seqFibers_2)
                    if (wavelengthFiberOccupancy.get(wavelengthId_2, link.getIndex()) != 0) {
                        freePath_2 = false;
                        break;
                    }
                if (!freePath_2) continue;
                return Pair.of(IntFactory1D.dense.make(seqFibers_1.size(), wavelengthId_1).toArray(), IntFactory1D.dense.make(seqFibers_2.size(), wavelengthId_2).toArray());
            }
        }
        return null;
    }

    /**
     * <p>Wavelength assignment algorithm based on a first-fit fashion assuming
     * full wavelength conversion and regeneration. Each node selects the first
     * free wavelength for its output fiber, and next nodes in the lightpath try
     * to maintain it. If not possible, or regeneration is needed, then include
     * a regenerator (can act also as a full wavelength converter) and search
     * for the first free wavelength, and so on.</p>
     * <p>
     * <p>In case a lightpath cannot be allocated, the corresponding sequence of
     * wavelengths ({@code seqWavelengths} parameter) will be an empty array.</p>
     *
     * @param seqFibers                  Sequence of traversed fibers
     * @param wavelengthFiberOccupancy   Occupied fibers in each wavelength
     * @param l_f                        Physical length in km per fiber
     * @param nodeRegeneratorOccupancy   Number of regenerators installed per node
     * @param maxRegeneratorDistanceInKm Maximum regeneration distance
     * @return Sequence of wavelengths traversed by each lightpath, and a 0-1 array indicating whether (1) or not (0) a regenerator/wavelength converter is required at the origin node of the corresponding fiber
     */
    public static Pair<int[], int[]> WA_RPP_firstFit(List<Link> seqFibers, DoubleMatrix2D wavelengthFiberOccupancy, DoubleMatrix1D l_f, DoubleMatrix1D nodeRegeneratorOccupancy, double maxRegeneratorDistanceInKm) {
        final int W = wavelengthFiberOccupancy.rows();

        List<Integer> seqWavelengths = new LinkedList<Integer>();
        List<Integer> seqRegenerators = new LinkedList<Integer>();

        double control_accumDistance = 0;
        Set<Integer> control_occupied_w = new LinkedHashSet<Integer>();
        int control_firstFitValidWavelengthForSubpath = -1;
        List<Link> control_currentSubpathSeqLinks = new LinkedList<Link>();

        boolean lpAllocated = true;

        Map<Link, Set<Integer>> avoidLoopWavelengthClash = new LinkedHashMap<Link, Set<Integer>>();

        for (Link fiber : seqFibers) {
            double fiberLengthInKm = l_f.get(fiber.getIndex());

            if (fiberLengthInKm > maxRegeneratorDistanceInKm) {
                throw new WDMException(String.format("Fiber %d is longer (%f km) than the maximum distance without regenerators (%f km)", fiber.getId(), fiberLengthInKm, maxRegeneratorDistanceInKm));
            }

			/* update the info as if this link was included in the subpath */
            final double plusLink_accumDistance = control_accumDistance + fiberLengthInKm;
            int plusLink_firstFitValidWavelengthForSubpath = -1;
            Set<Integer> plusLink_occupied_w = new LinkedHashSet<Integer>(control_occupied_w);

            if (avoidLoopWavelengthClash.containsKey(fiber)) {
                plusLink_occupied_w.addAll(avoidLoopWavelengthClash.get(fiber));
            }

            for (int wavelengthId = W - 1; wavelengthId >= 0; wavelengthId--) {
                if (!plusLink_occupied_w.contains(wavelengthId) && (wavelengthFiberOccupancy.get(wavelengthId, fiber.getIndex()) == 0)) {
                    plusLink_firstFitValidWavelengthForSubpath = wavelengthId;
                } else {
                    plusLink_occupied_w.add(wavelengthId);
                }
            }

            if (!control_currentSubpathSeqLinks.contains(fiber) && plusLink_accumDistance <= maxRegeneratorDistanceInKm && plusLink_firstFitValidWavelengthForSubpath != -1) {
				/* we do not have to put a regenerator in the origin node of e: the subpath is valid up to now */
                control_accumDistance = plusLink_accumDistance;
                control_occupied_w = plusLink_occupied_w;
                control_firstFitValidWavelengthForSubpath = plusLink_firstFitValidWavelengthForSubpath;
                control_currentSubpathSeqLinks.add(fiber);
                seqRegenerators.add(0);
                continue;
            }

			/* Here if we have to put a regenerator in initial node of this link, add a subpath */
            if (control_firstFitValidWavelengthForSubpath == -1) {
                lpAllocated = false;
                break;
            }

            seqRegenerators.add(1);
            int numFibersSubPath = control_currentSubpathSeqLinks.size();
            for (int cont = 0; cont < numFibersSubPath; cont++) {
                seqWavelengths.add(control_firstFitValidWavelengthForSubpath);

                Link aux_fiber = control_currentSubpathSeqLinks.get(cont);
                if (!avoidLoopWavelengthClash.containsKey(aux_fiber))
                    avoidLoopWavelengthClash.put(aux_fiber, new LinkedHashSet<Integer>());

                avoidLoopWavelengthClash.get(aux_fiber).add(control_firstFitValidWavelengthForSubpath);
            }

			/* new span includes just this link */
            control_accumDistance = fiberLengthInKm;
            control_currentSubpathSeqLinks = new LinkedList<Link>();
            control_currentSubpathSeqLinks.add(fiber);
            control_occupied_w = new LinkedHashSet<Integer>();
            if (avoidLoopWavelengthClash.containsKey(fiber))
                control_occupied_w.addAll(avoidLoopWavelengthClash.get(fiber));

            control_firstFitValidWavelengthForSubpath = -1;
            for (int wavelengthId = 0; wavelengthId < W; wavelengthId++) {
                if (wavelengthFiberOccupancy.get(wavelengthId, fiber.getIndex()) != 0)
                    control_occupied_w.add(wavelengthId);
                else if (control_firstFitValidWavelengthForSubpath == -1)
                    control_firstFitValidWavelengthForSubpath = wavelengthId;
            }

            if (control_firstFitValidWavelengthForSubpath == -1) {
                lpAllocated = false;
                break;
            }
        }

		/* Add the last subpath */
		/* Here if we have to put a regenerator in initial node of this link, add a subpath */
        if (control_firstFitValidWavelengthForSubpath == -1) lpAllocated = false;

        if (!lpAllocated) return Pair.of(new int[0], new int[0]);

        int numFibersSubPath = control_currentSubpathSeqLinks.size();
        for (int cont = 0; cont < numFibersSubPath; cont++)
            seqWavelengths.add(control_firstFitValidWavelengthForSubpath);

        return Pair.of(IntUtils.toArray(seqWavelengths), IntUtils.toArray(seqRegenerators));
    }

    /**
     * Updates {@code wavelengthFiberOccupancy} to consider that a new lightpath is occupying
     * a wavelength in each fiber.
     *
     * @param seqFibers                Sequence of traversed fibers
     * @param seqWavelengths           Sequence of wavelengths (as many as the number of links in the lightpath)
     * @param wavelengthFiberOccupancy Set of occupied fibers in each wavelength
     * @param seqRegenerators          A 0-1 array indicating whether (1) or not (0) a regenerator/wavelength converter is required at the origin node of the corresponding fiber
     * @param nodeRegeneratorOccupancy Number of regenerators installed per node
     */
    public static void allocateResources(List<Link> seqFibers, int[] seqWavelengths, DoubleMatrix2D wavelengthFiberOccupancy, int[] seqRegenerators, DoubleMatrix1D nodeRegeneratorOccupancy) {
        if (seqWavelengths.length == 0) return;

        ListIterator<Link> fiberIt = seqFibers.listIterator();
        while (fiberIt.hasNext()) {
            int hopId = fiberIt.nextIndex();
            Link fiber = fiberIt.next();
            int wavelengthId = seqWavelengths[hopId];
            if (wavelengthFiberOccupancy.get(wavelengthId, fiber.getIndex()) != 0)
                throw new Net2PlanException("Wavelength clashing: wavelength " + wavelengthId + ", fiber: " + fiber.getId());
            wavelengthFiberOccupancy.set(wavelengthId, fiber.getIndex(), 1.0);

            if (seqRegenerators != null && seqRegenerators[hopId] == 1) {
                Node node = fiber.getOriginNode();
                nodeRegeneratorOccupancy.set(node.getIndex(), nodeRegeneratorOccupancy.get(node.getIndex()) + 1);
            }
        }
    }
}
