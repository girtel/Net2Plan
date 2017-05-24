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

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.OSMTileFactoryInfo;
import org.jxmapviewer.viewer.DefaultTileFactory;
import org.jxmapviewer.viewer.GeoPosition;
import org.jxmapviewer.viewer.TileFactoryInfo;

/**
 * Created by Jorge San Emeterio on 13/10/2016.
 */
public class OSMPanel extends JXMapViewer
{
    private static final int NUMBER_OF_THREADS = 8;
    private static final GeoPosition europe = new GeoPosition(47.20, 25.2);
    private static final int defaultZoom = 16;

    public OSMPanel()
    {
        // Create a TileFactoryInfo for OpenStreetMap
        final TileFactoryInfo info = new OSMTileFactoryInfo();
        final DefaultTileFactory tileFactory = new DefaultTileFactory(info);

        this.setTileFactory(tileFactory);

        // Use 8 threads in parallel to load the tiles
        tileFactory.setThreadPoolSize(NUMBER_OF_THREADS);
    }

    public void moveToDefaultPosition()
    {
        this.setCenterPosition(europe);
    }
    public void returnToDefaultZoom()
    {
        this.setZoom(defaultZoom);
    }

    public GeoPosition getDefaultPosition()
    {
        return europe;
    }
    public int getDefaultZoom()
    {
        return defaultZoom;
    }
}
