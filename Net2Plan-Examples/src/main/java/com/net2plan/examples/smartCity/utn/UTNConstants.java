

package com.net2plan.examples.smartCity.utn;

public class UTNConstants 
{
	public static final String ATTRNAME_C0A = "UTN_C0A"; // the V0 or C0 (free speed) parameter of a link in the user equilibrium model
	public static final String ATTRNAME_CA = "UTN_CA"; // the velocity according to the BPR model
	public static final String ATTRNAME_MONITOREDVEHICUCLERATE = "UTN_MONITOREDVEHICLERATE"; // the monitored information (ground truth) coming for measures. Can appear in arcs and in demands 

	public static double bprCaComputation (double c0 , double alpha , double va , double Qa , double gamma)
	{
		return c0  * (1 + alpha * Math.pow(va / Qa , gamma)    );
	}
}
