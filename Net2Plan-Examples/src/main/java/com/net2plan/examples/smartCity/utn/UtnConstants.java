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


package com.net2plan.examples.smartCity.utn;

/** This class includes several constants used in this package
 */
class UtnConstants 
{
	/** The name of the Net2Plan attribute for storing the V0 or C0 (free speed) parameter of a link in the user equilibrium model 
	 */
	public static final String ATTRNAME_C0A = "UTN_C0A"; // the V0 or C0 (free speed) parameter of a link in the user equilibrium model
	
	/** The name of the Net2Plan attribute for storing the velocity according to the BPR model
	 */
	public static final String ATTRNAME_CA = "UTN_CA"; // the velocity according to the BPR model
	/** The name of the Net2Plan attribute for storing the monitored information (ground truth) coming from measures. Can appear in arcs (link) and in demands
	 * 
	 */
	public static final String ATTRNAME_MONITOREDVEHICUCLERATE = "UTN_MONITOREDVEHICLERATE"; // the monitored information (ground truth) coming for measures. Can appear in arcs and in demands 

	/** Help method for computing the Ca according to the BPR model
	 * @param c0 c0 parameter
	 * @param alpha alpha parameter
	 * @param va va parameter
	 * @param Qa Qa parameter
	 * @param gamma gamma parameter
	 * @return see above
	 */
	public static double bprCaComputation (double c0 , double alpha , double va , double Qa , double gamma)
	{
		return c0  * (1 + alpha * Math.pow(va / Qa , gamma)    );
	}
}
