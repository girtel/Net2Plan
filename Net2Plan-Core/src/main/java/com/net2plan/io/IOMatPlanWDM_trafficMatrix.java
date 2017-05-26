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



 




package com.net2plan.io;

import cern.colt.list.tdouble.DoubleArrayList;
import cern.colt.matrix.tdouble.DoubleFactory2D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import com.net2plan.interfaces.networkDesign.Net2PlanException;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.internal.Constants;
import com.net2plan.internal.plugins.IOFilter;
import com.net2plan.utils.Triple;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Importer filter for traffic matrices from MatPlanWDM tool ({@code .xml}).
 * 
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.3.1
 * @see <a href='http://girtel.upct.es/~ppavon/matplanwdm/'>MatPlanWDM: An educational tool for network planning in wavelength-routing networks</a>
 */
public class IOMatPlanWDM_trafficMatrix extends IOFilter
{
	private final static String TITLE = "MatPlanWDM";
	
	/**
	 * Default constructor.
	 *
	 * @since 0.3.1
	 */
	public IOMatPlanWDM_trafficMatrix()
	{
		super(TITLE, EnumSet.of(Constants.IOFeature.LOAD_DEMANDS), "traff");
	}
	
	@Override
	public String getName()
	{
		return TITLE + " traffic matrix import filter";
	}

	@Override
	public List<Triple<String, String, String>> getParameters()
	{
		return null;
	}
	
	@Override
	public NetPlan readDemandSetFromFile(File file)
	{
		int N = -1;
		DoubleMatrix2D trafficMatrix = null;
		int currentRow = 0;
		try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)))
		{
			String line;
			while ((line = in.readLine()) != null)
			{
				line = line.trim();
				if (line.isEmpty() || line.startsWith("//")) continue;

				if (N == -1)
				{
					N = Integer.parseInt(line);
					if (N <= 1) throw new Net2PlanException("Bad - Number of nodes must be greater than one");

					trafficMatrix = DoubleFactory2D.dense.make(N, N);
				}
				else
				{
					DoubleArrayList aux = new DoubleArrayList();
					StringTokenizer tokenizer = new StringTokenizer(line, " \t");

					while (tokenizer.hasMoreTokens())
						aux.add(Double.parseDouble(tokenizer.nextToken()));

					aux.trimToSize();
					if (aux.size() != N) throw new Net2PlanException("Offered traffic for node " + currentRow + " does not match the number of nodes");
					trafficMatrix.viewRow(currentRow++).assign(aux.elements());

					if (currentRow == N) break;
				}
			}
		}
		catch (Throwable e)
		{
			throw new RuntimeException(e);
		}

		if (N == -1 || currentRow != N) throw new Net2PlanException("Bad - Wrong format");

		NetPlan netPlan = new NetPlan();
		netPlan.setTrafficMatrix(trafficMatrix);

		return netPlan;
	}
}
