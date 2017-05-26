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


package com.net2plan.documentation;

import com.net2plan.interfaces.networkDesign.IAlgorithm;
import com.net2plan.interfaces.networkDesign.IReport;
import com.net2plan.interfaces.simulation.IEventGenerator;
import com.net2plan.interfaces.simulation.IEventProcessor;
import com.net2plan.utils.Pair;
import com.net2plan.utils.StringUtils;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.DocErrorReporter;
import com.sun.javadoc.RootDoc;
import com.sun.javadoc.Tag;

import java.io.File;
import java.io.PrintWriter;
import java.util.*;
import java.util.Map.Entry;

@SuppressWarnings("unchecked")
public class CreateHTMLKeywords
{
	public static Map<String,Pair<String,String>> keywordDescriptionMap = null;
	private static String navBar;
	public static void initializeKeywordDescriptionMap ()
	{
		keywordDescriptionMap = new TreeMap<String,Pair<String,String>> ();
		keywordDescriptionMap.put ("Ant Colony Optimization (ACO)" , Pair.of ("An example where a heuristic using an ant colony optimization (ACO) algorithmic approach is used" , "aco"));
		keywordDescriptionMap.put ("Backpressure routing" , Pair.of ("An example where the traffic routing is performed using a backpressure approach." , "backpressure"));
		keywordDescriptionMap.put ("Bandwidth assignment (BA)" , Pair.of ("An example where the volume of traffic to be carried by each demand, is an algorithm output (that includes congestion control algorithms)." , "ba"));
		keywordDescriptionMap.put ("Capacity assignment (CA)" , Pair.of ("An example where the capacities in the links are algorithm outputs." , "ca"));
		keywordDescriptionMap.put ("CAC (Connection-Admission-Control)" , Pair.of ("An example where an algorithm performing the admission control to incoming connection requests is involved." , "cac"));
		keywordDescriptionMap.put ("CSMA" , Pair.of ("An example where the wireless links are coordinated using a CSMA MAC." , "csma"));
		keywordDescriptionMap.put ("Destination-based routing" , Pair.of ("An example related to a problem where the traffic routing is destination-based (i.e. like in IP)" , "faDestination"));
		keywordDescriptionMap.put ("Destination-link formulation" , Pair.of ("An example where a destination-link formulation of the routing is involved." , "destLink"));
		keywordDescriptionMap.put ("Distributed algorithm" , Pair.of ("An example where a distributed algorithm (different agents operating more or less independently, coordinated by an implicit or explicit signaling) is involved." , "distributed"));
		keywordDescriptionMap.put ("Dual decomposition" , Pair.of ("An example where an algorithm based on a dual decomposition of the problem is used." , "dualDecomp"));
		keywordDescriptionMap.put ("Dual gradient algorithm" , Pair.of ("An example where a gradient or subgradient algorithm optimizing the dual function is used." , "dualGradient"));
		keywordDescriptionMap.put ("Evolutionary algorithm (EA)" , Pair.of ("An example where a heuristic using an evolutionary algorithm approach is used" , "ea"));
		keywordDescriptionMap.put ("Flow assignment (FA)" , Pair.of ("An example where the routing of the traffic is an algorithm output." , "fa"));
		keywordDescriptionMap.put ("Flow-link formulation" , Pair.of ("An example where a flow-link formulation of the routing is involved." , "flowLink"));
		keywordDescriptionMap.put ("Flow-path formulation" , Pair.of ("An example where a flow-path formulation of the routing is involved." , "flowPath"));
		keywordDescriptionMap.put ("Greedy heuristic" , Pair.of ("An example where a heuristic using a greedy approach is used" , "greedy"));
		keywordDescriptionMap.put ("GRASP" , Pair.of ("An example where a heuristic using a Greedy-Randomized Adaptive Search Procedure (GRASP) algorithmic approach is used" , "grasp"));
		keywordDescriptionMap.put ("IP/OSPF" , Pair.of ("An example where routing is based on the Open Shortest Path First (OSPF) protocol" , "ospf"));
		keywordDescriptionMap.put ("JOM" , Pair.of ("An example where the Java Optimization Modeler (JOM) library is used to solve a formulation" , "jom"));
		keywordDescriptionMap.put ("Local search (LS) heuristic" , Pair.of ("An example where a heuristic using a local search approach is used" , "ls"));
		keywordDescriptionMap.put ("Modular capacities" , Pair.of ("An example where link capacities are restricted to a discrete set of possible values" , "modularCap"));
		keywordDescriptionMap.put ("Multicast" , Pair.of ("An example where multicast traffic is involved" , "multicast"));
		keywordDescriptionMap.put ("Multilayer" , Pair.of ("An example where a multilayer representation of a network model is used" , "multilayer"));
		keywordDescriptionMap.put ("Multidomain network" , Pair.of ("An example where the network is composed of different domains or autonomous systems, controlled by different organizations" , "multidomain"));
		keywordDescriptionMap.put ("Multihour optimization" , Pair.of ("An example where traffic variations typical of multihour profiles (e.g. predictable day-night fluctuations in the traffic, or time-zone based traffic variations) are considered in the optimization" , "multihour"));
		keywordDescriptionMap.put ("Multiperiod optimization" , Pair.of ("An example where multiple time periods are considered in the network optimization (e.g. successive years with different traffics and equipment costs)" , "multiperiod"));
		keywordDescriptionMap.put ("NUM" , Pair.of ("An example where a Network Utility Maximization (NUM) model is used" , "num"));
		keywordDescriptionMap.put ("Primal decomposition" , Pair.of ("An example where an algorithm based on a primal decomposition of the problem is used." , "primalDecomp"));
		keywordDescriptionMap.put ("Primal gradient algorithm" , Pair.of ("An example where a gradient or subgradient algorithm optimizing the primal problem objective function is used." , "primalGradient"));
		keywordDescriptionMap.put ("Network recovery: protection" , Pair.of ("An example where traffic is protected using backup segments." , "protection"));
		keywordDescriptionMap.put ("Network recovery: restoration" , Pair.of ("An example where traffic is recovered using restoration." , "restoration"));
		keywordDescriptionMap.put ("Random-access MAC" , Pair.of ("An example where wireless links are coordinated by a random-access MAC, based on persistence probabilities." , "aloha"));
		keywordDescriptionMap.put ("Simulated annealing (SAN)" , Pair.of ("An example where a heuristic using a simulated annealing approach is used" , "san"));
		keywordDescriptionMap.put ("Tabu search (TS)" , Pair.of ("An example where a heuristic using a tabu search approach is used" , "ts"));
		keywordDescriptionMap.put ("TCP" , Pair.of ("An example where the Transmission Control Protocol (TCP) is involved" , "tcp"));
		keywordDescriptionMap.put ("Topology assignment (TA)" , Pair.of ("An example where the links and/or nodes in the network, are algorithm outputs." , "ta"));
		keywordDescriptionMap.put ("Transmission power optimization" , Pair.of ("An example where the links are wireless links, and its transmission power is optimized." , "transmissionPower"));
		keywordDescriptionMap.put ("Wireless" , Pair.of ("An example focused on the design of a wireless network" , "wireless"));
		keywordDescriptionMap.put ("WDM" , Pair.of ("An example where Wavelength Division Multiplexing (WDM) technology is used" , "wdm"));
		keywordDescriptionMap.put ("NFV" , Pair.of ("An example in the context of Network Function Virtualizatin (NFV)" , "nfv"));
	}
	static
	{
		initializeKeywordDescriptionMap();
		navBar = "<script type=\"text/javascript\"><!-- try { if (location.href.indexOf('is-external=true') == -1) { parent.document.title=\"Overview\";  } } catch(err) {  } //-->\n";
		navBar += "</script><noscript><div>JavaScript is disabled on your browser.</div></noscript>\n";
		navBar += "<!-- ========= START OF TOP NAVBAR ======= -->\n";
		navBar += "<div class=\"topNav\"><a name=\"navbar.top\"></a>\n";
		navBar += "<div class=\"skipNav\"><a href=\"#skip.navbar.top\" title=\"Skip navigation links\">Skip navigation links</a></div><a name=\"navbar.top.firstrow\">\n";
		navBar += "<!--   -->\n";
		navBar += "</a>\n";
		navBar += "<ul class=\"navList\" title=\"Navigation\">\n";
		navBar += "<li class=\"navBarCell1Rev\">Overview</li>\n";
		navBar += "<li><a href=\"com/net2plan/examples/general/onlineSim/package-summary.html\">Package</a></li>\n";
		navBar += "<li>Class</li>\n";
		navBar += "<li>Use</li>\n";
		navBar += "<li><a href=\"com/net2plan/examples/general/onlineSim/package-tree.html\">Tree</a></li>\n";
		navBar += "<li><a href=\"index-files/index-1.html\">Index</a></li>\n";
		navBar += "<li><a href=\"help-doc.html\">Help</a></li>\n";
		navBar += "</ul>\n";
		navBar += "</div>\n";
		navBar += "<div class=\"subNav\">\n";
		navBar += "<ul class=\"navList\">\n";
		navBar += "<li>Prev</li>\n";
		navBar += "<li>Next</li>\n";
		navBar += "</ul>\n";
		navBar += "<ul class=\"navList\">\n";
		navBar += "<li><a href=\"index.html?overview-summary.html\" target=\"_top\">Frames</a></li>\n";
		navBar += "<li><a href=\"overview-summary.html\" target=\"_top\">No&nbsp;Frames</a></li>\n";
		navBar += "</ul>\n";
		navBar += "<ul class=\"navList\" id=\"allclasses_navbar_top\">\n";
		navBar += "<li><a href=\"allclasses-noframe.html\">All&nbsp;Classes</a></li>\n";
		navBar += "</ul>\n";
		navBar += "<div>\n";
		navBar += "<script type=\"text/javascript\"><!--\n";
		navBar += "allClassesLink = document.getElementById(\"allclasses_navbar_top\");\n";
		navBar += " if(window==top) {\n";
		navBar += "allClassesLink.style.display = \"block\";\n";
		navBar += "}\n";
		navBar += "else {\n";
		navBar += "allClassesLink.style.display = \"none\";\n";
		navBar += "}\n";
		navBar += "//-->\n";
		navBar += "</script>\n";
		navBar += "</div>\n";
		navBar += "<a name=\"skip.navbar.top\">\n";
		navBar += "<!--   -->\n";
		navBar += "</a></div>\n";
		navBar += "<!-- ========= END OF TOP NAVBAR ========= -->\n";

	}
	public static boolean start(RootDoc root)
	{
		String tagName = "net2plan.keywords";
		String outputFolder = null;
		for (String [] option : root.options ()) if (option [0].equals ("-outputFolder")) outputFolder = option [1];
		if (outputFolder == null) throw new RuntimeException ("You should indicate the output file");
		writeContents(root.classes(), tagName , outputFolder);
		return true;
	}

	private static void writeContents(ClassDoc[] classes, String tagName , String outputFolder)
	{
		Map<String,List<String>> keywordsToAlgorithmsMap = new HashMap<String,List<String>> ();
		Map<String,List<String>> algorithmToKeywordsMap = new HashMap<String,List<String>> ();
		Map<String,String> algorithmToFirstSentence = new HashMap<String,String> ();
		
		for (int i = 0; i < classes.length; i++)
		{
			final ClassDoc javaClass = classes [i];
			final String className = javaClass.qualifiedName();
			final Tag [] firstSentenceTags = javaClass.firstSentenceTags();
			if (firstSentenceTags.length == 0) System.out.println("A class without first sentence!!: " + className);
			final String firstSentenceThisClass = (firstSentenceTags.length == 0)? "" : javaClass.firstSentenceTags() [0].text ();
			algorithmToFirstSentence.put (className , firstSentenceThisClass);
			algorithmToKeywordsMap.put (className , new LinkedList<String> ());
			String keywordsString = ""; for (Tag tag : javaClass.tags()) {  if (tag.name().equals ("@"+tagName)) keywordsString += " " + tag.text () + " "; }
			keywordsString = keywordsString.trim ();
			for (String keyword : StringUtils.split(keywordsString , ","))
			{
				final String keywordName = keyword.trim ();
				if (!keywordDescriptionMap.containsKey(keywordName)) throw new RuntimeException ("Bad: Keyword: " + keywordName + " in algorithm " + className + ", does not exist in the description");
				if (!keywordsToAlgorithmsMap.containsKey(keywordName)) keywordsToAlgorithmsMap.put (keywordName , new LinkedList<String> ());
				keywordsToAlgorithmsMap.get(keywordName).add (className);
				algorithmToKeywordsMap.get (className).add (keywordName);
			}
		}
		
		String htmlFile = createHtml(keywordsToAlgorithmsMap , algorithmToKeywordsMap , algorithmToFirstSentence , outputFolder);
		try 
		{
			File f = new File (outputFolder + "/keywords-all.html");
			System.out.println ("Save in : " + f.getAbsolutePath());
			PrintWriter pw = new PrintWriter (f);
			pw.append(htmlFile);
			pw.close ();
		} catch (Exception e) { e.printStackTrace(); throw new RuntimeException ("Not possible to write in keywords-all.html"); } 
		
	}

	public static int optionLength(String option)
	{
		if (option.equals("-outputFolder")) { return 2; }
		return 0;
	}

	public static boolean validOptions(String options[][], DocErrorReporter reporter)
	{
		return true;
	}
	
	private static String createHtml (Map<String,List<String>> keywordsToAlgorithmsMap , Map<String,List<String>> algorithmToKeywordsMap , Map<String,String> algorithmToFirstSentence , String outputFolder)
	{
		String s = "";
		s += "<html><head>\n";
		s+= "<title>Net2Plan built-in examples keywords</title>\n";
		s+= "<link rel=\"stylesheet\" type=\"text/css\" href=\"stylesheet.css\" title=\"Style\">\n";
		s += "<script type=\"text/javascript\" src=\"script.js\"></script>\n";
		s+= "</head>\n";
		s+= "<body>\n";
		s += navBar;
		s+= "<div class=\"contentContainer\">\n";
		s+= "<table class=\"overviewSummary\" border=\"0\" cellpadding=\"3\" cellspacing=\"0\" summary=\"Keywords describing the examples in this Javadoc\">\n";
		s+= "<caption><span>Keywords</span><span class=\"tabEnd\">&nbsp;</span></caption>\n";
		s+= "<tr>\n";
		s+= "<th class=\"colFirst\" scope=\"col\">Keyword</th>\n";
		s+= "<th class=\"colLast\" scope=\"col\">Description</th>\n";
		s+= "</tr>\n";
		s+= "<tbody>\n";
//		if (!new File (outputFolder + "/keywords").isDirectory())
//			new File (outputFolder + "/keywords").mkdir();
		for (Entry<String,Pair<String,String>> entry : keywordDescriptionMap.entrySet())
		{
			final String keywordName = entry.getKey ();
			final String keywordDescription = entry.getValue ().getFirst ();
			final String keywordFileSuffix = entry.getValue ().getSecond ();
			final File newFileThisKeyword = new File (outputFolder + "/keyword_" + keywordFileSuffix + ".html");
			s+= "<tr class=\"altColor\">\n";
			s+= "<td class=\"colFirst\"><a href=\"keyword_" + keywordFileSuffix + ".html\">" + keywordName + "</a></td>\n";
			s+= "<td class=\"colLast\">\n";
			s+= "<div class=\"block\">" + keywordDescription + "</div>\n";
			s+= "</td>\n";
			s+= "</tr>\n";
			try 
			{
				PrintWriter pw = new PrintWriter (newFileThisKeyword);
				List<String> examples = keywordsToAlgorithmsMap.get (keywordName);
				if (examples == null) System.out.println ("The keyword: " + keywordName + ", appears in no examples. REMOVE???");
				pw.append(createOneKeywordHTML (keywordName , examples == null? new LinkedList<String> () : examples , algorithmToFirstSentence , algorithmToKeywordsMap));
				pw.close ();
			} catch (Exception e) { e.printStackTrace(); throw new RuntimeException ("Not possible to write in " + newFileThisKeyword); } 
			
		}
		s+= "</tbody>\n";
		s+= "</table>\n";
		s+= "</div>\n";
		s+= "</body>\n";
		s+= "</html>\n";

		return s;
	}
	
	private static String createOneKeywordHTML (String keyword , List<String> exampleNames , Map<String,String> algorithmToFirstSentence , Map<String,List<String>> algorithmToKeywordsMap)
	{
		String tableOffline = "";
		String tableOnlineProc = "";
		String tableOnlineGen = "";
		String tableReport = "";
		for (String exampleName : exampleNames)
		{
			String shortExampleName = exampleName.substring (exampleName.lastIndexOf(".") + 1);
			String linkToJavadoc = exampleName.replace(".".toCharArray() [0] , "/".toCharArray() [0]) + ".html";
			String thisTable = "";
			thisTable+= "<tr class=\"altColor\">\n";
			thisTable+= "<td class=\"colFirst\"><a href=\"" + linkToJavadoc + "\">" + shortExampleName + "</a></td>\n";
			thisTable+= "<td class=\"colLast\">\n";
			thisTable+= "<div class=\"block\">" + algorithmToFirstSentence.get(exampleName) + "<p>Keywords: \n";
			boolean firstKey = true;
			for (String algKeyword : algorithmToKeywordsMap.get (exampleName))
			{
				if (!firstKey) thisTable += ", "; 
				firstKey = false; 
				thisTable += "<a href=\"keyword_" + keywordDescriptionMap.get(algKeyword).getSecond () + ".html\">" + algKeyword + "</a>";
			}
			thisTable+= "</p></div>\n";
			thisTable+= "</td>\n";
			thisTable+= "</tr>\n";
			Object algorithm = getAlgorithmAssociatedToFullyQualifiedName (exampleName);
      if (algorithm instanceof IAlgorithm) tableOffline += thisTable;
      else if (algorithm instanceof IReport) tableReport += thisTable;
      else if (algorithm instanceof IEventGenerator) tableOnlineGen += thisTable;
      else if (algorithm instanceof IEventProcessor) tableOnlineProc += thisTable;
      else throw new RuntimeException ("Bad");
		}

		String s = "";
		s += "<html><head>\n";
		s+= "<title>Net2Plan built-in examples with keyword: " + keyword + "</title>\n";
		s+= "<link rel=\"stylesheet\" type=\"text/css\" href=\"stylesheet.css\" title=\"Style\">\n";
		s += "<script type=\"text/javascript\" src=\"script.js\"></script>\n";
		s+= "</head>\n";
		s+= "<body>\n";
		s += navBar;
		s+= "<div class=\"contentContainer\">\n";
		s+= "<table class=\"overviewSummary\" border=\"0\" cellpadding=\"3\" cellspacing=\"0\" summary=\"Net2Plan built-in examples associated with keyword: " + keyword + "\">\n";
		s+= "<caption><span>Keyword: " + keyword + "</span><span class=\"tabEnd\">&nbsp;</span></caption>\n";
		if (!tableOffline.equals (""))
		{
			s+= "<tr>\n";
			s+= "<th class=\"colFirst\" scope=\"col\">Example (offline algorithm)</th>\n";
			s+= "<th class=\"colLast\" scope=\"col\">Description</th>\n";
			s+= "</tr>\n";
			s+= "<tbody>\n";
			s += tableOffline;
			s+= "</tbody>\n";
		}
		if (!tableOnlineProc.equals (""))
		{
			s+= "<tr>\n";
			s+= "<th class=\"colFirst\" scope=\"col\">Example (online event processor)</th>\n";
			s+= "<th class=\"colLast\" scope=\"col\">Description</th>\n";
			s+= "</tr>\n";
			s+= "<tbody>\n";
			s += tableOnlineProc;
			s+= "</tbody>\n";
		}
		if (!tableOnlineGen.equals (""))
		{
			s+= "<tr>\n";
			s+= "<th class=\"colFirst\" scope=\"col\">Example (online event generator)</th>\n";
			s+= "<th class=\"colLast\" scope=\"col\">Description</th>\n";
			s+= "</tr>\n";
			s+= "<tbody>\n";
			s += tableOnlineGen;
			s+= "</tbody>\n";
		}
		if (!tableReport.equals (""))
		{
			s+= "<tr>\n";
			s+= "<th class=\"colFirst\" scope=\"col\">Example (report)</th>\n";
			s+= "<th class=\"colLast\" scope=\"col\">Description</th>\n";
			s+= "</tr>\n";
			s+= "<tbody>\n";
			s += tableReport;
			s+= "</tbody>\n";
		}
		s+= "</table>\n";
		s+= "</div>\n";
		s+= "</body>\n";
		s+= "</html>\n";
		return s;
	}

  private static Object getAlgorithmAssociatedToFullyQualifiedName (String theClassName)
  {
  	Object alg = null;
    try 
    { 
      Class algorithmClass = Taglet_Description.class.getClassLoader().loadClass(theClassName); 
    	if (IAlgorithm.class.isAssignableFrom(algorithmClass)) return (IAlgorithm) algorithmClass.getConstructor().newInstance(); 
    	if (IReport.class.isAssignableFrom(algorithmClass)) return (IReport)algorithmClass.getConstructor().newInstance();
    	if (IEventGenerator.class.isAssignableFrom(algorithmClass)) return (IEventGenerator)algorithmClass.getConstructor().newInstance();
    	if (IEventProcessor.class.isAssignableFrom(algorithmClass)) return (IEventProcessor)algorithmClass.getConstructor().newInstance();
    	return null;
    } catch (Exception e)
		{
			e.printStackTrace();
			throw new RuntimeException ("Unexpected exception trying to load class of '" + theClassName + "'");
		}
  }


}
