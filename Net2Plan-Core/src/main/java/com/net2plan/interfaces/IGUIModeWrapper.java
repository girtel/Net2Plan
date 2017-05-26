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
package com.net2plan.interfaces;

import java.util.Map;

/**
 * Created by Jorge San Emeterio on 19/03/2017.
 */
public interface IGUIModeWrapper
{
    void launchRoutine(int mode, Map<String, String> parameters);
}
