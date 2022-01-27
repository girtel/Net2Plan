/*******************************************************************************
 * This program and the accompanying materials
 * are made available under the terms of the MIT License available at
 * https://opensource.org/licenses/MIT
 *******************************************************************************/
package com.net2plan.niw;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class ExcelTesting 
{
	
	public static void main(String[]args){

		File file = new File("MHTopology_Nodes_Links.xlsx");		
		ImportMetroNetwork.importFromExcelFile(file);	
	}
}
