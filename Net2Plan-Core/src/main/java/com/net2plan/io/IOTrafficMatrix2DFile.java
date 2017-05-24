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

import cern.colt.matrix.tdouble.DoubleMatrix2D;
import com.net2plan.interfaces.networkDesign.NetPlan;
import com.net2plan.internal.Constants.IOFeature;
import com.net2plan.internal.plugins.IOFilter;
import com.net2plan.utils.DoubleUtils;
import com.net2plan.utils.Triple;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.List;

/**
 * This IO filter is targeted to handle traffic matrices stored in files where 
 * each line in the file is a row of the matrix. Items can be separated by 
 * spaces or tabs. Lines starting with '#' are skipped.
 *
 * @author Pablo Pavon-Marino, Jose-Luis Izquierdo-Zaragoza
 * @since 0.3.0
 */
public class IOTrafficMatrix2DFile extends IOFilter
{
	private final static String title = "2D traffic matrix";
	
	/**
	 * Default constructor.
	 *
	 * @since 0.3.0
	 */
	public IOTrafficMatrix2DFile()
	{
		super(title, EnumSet.of(IOFeature.LOAD_DEMANDS, IOFeature.SAVE_DEMANDS), "csv", "txt");
	}
		
	@Override
	public String getName()
	{
		return title + " import/export filter";
	}

	@Override
	public List<Triple<String, String, String>> getParameters()
	{
		return null;
	}
	
	@Override
	public NetPlan readDemandSetFromFile(File file)
	{
		DoubleMatrix2D trafficMatrix = DoubleUtils.read2DMatrixFromFile(file);
		NetPlan netPlan = new NetPlan();
		netPlan.setTrafficMatrix(trafficMatrix);
		
		return netPlan;
	}
	
	@Override
	public void saveDemandSetToFile(NetPlan netPlan, File file)
	{
		DoubleMatrix2D trafficMatrix = netPlan.getMatrixNode2NodeOfferedTraffic();
		int N = trafficMatrix.rows();
		
		PrintStream out;
		try
		{
			out = new PrintStream(new FileOutputStream(file), true, StandardCharsets.UTF_8.name());
			for(int ingressNodeId = 0; ingressNodeId < N; ingressNodeId++)
			{
				for(int egressNodeId = 0; egressNodeId < N; egressNodeId++)
					out.print(trafficMatrix.getQuick(ingressNodeId, egressNodeId) + " ");

				out.println();
			}
			
			out.flush();
			out.close();
		}
		catch(FileNotFoundException | UnsupportedEncodingException e)
		{
			throw new RuntimeException(e);
		}
	}
}
