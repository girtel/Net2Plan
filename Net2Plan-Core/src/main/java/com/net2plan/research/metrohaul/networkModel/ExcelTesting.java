package com.net2plan.research.metrohaul.networkModel;

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
