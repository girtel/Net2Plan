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
package com.net2plan.gui.plugins.networkDesign.topologyPane.jung.osmSupport;

import org.jxmapviewer.viewer.TileFactoryInfo;

/**
 * OpenStreetMap raster tiles using the URL and scheme required by the OSMF tile
 * usage policy ({@code https://tile.openstreetmap.org/{z}/{x}/{y}.png}). The stock
 * {@link org.jxmapviewer.OSMTileFactoryInfo} still points at {@code http://}, which
 * is no longer supported.
 *
 * @see <a href="https://operations.osmfoundation.org/policies/tiles/">Tile Usage Policy</a>
 */
final class Net2PlanOSMTileFactoryInfo extends TileFactoryInfo
{
    private static final int OSM_MAX_NATIVE_ZOOM = 19;

    Net2PlanOSMTileFactoryInfo()
    {
        super(
                "OpenStreetMap",
                1,
                OSM_MAX_NATIVE_ZOOM - 2,
                OSM_MAX_NATIVE_ZOOM,
                256,
                true,
                true,
                "https://tile.openstreetmap.org",
                "x",
                "y",
                "z");
    }

    @Override
    public String getTileUrl(int x, int y, int zoom)
    {
        int z = OSM_MAX_NATIVE_ZOOM - zoom;
        return baseURL + "/" + z + "/" + x + "/" + y + ".png";
    }
}
