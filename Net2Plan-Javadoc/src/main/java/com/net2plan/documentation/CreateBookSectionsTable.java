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


import com.net2plan.utils.StringUtils;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.DocErrorReporter;
import com.sun.javadoc.RootDoc;
import com.sun.javadoc.Tag;

import java.io.File;
import java.io.PrintWriter;
import java.util.*;
import java.util.Map.Entry;

public class CreateBookSectionsTable
{
	private static Map<String,String> chapterNames= new LinkedHashMap<String,String> ();
	private static String navBar;
	private static Map<String,List<String>> sectionsToAlgorithmsMap = new HashMap<String,List<String>> ();
	private static Map<String,List<String>> exercisesToAlgorithmsMap = new HashMap<String,List<String>> ();
	private static Map<String,List<String>> algorithmToSectionsMap = new HashMap<String,List<String>> ();
	private static Map<String,List<String>> algorithmToExercisesMap = new HashMap<String,List<String>> ();
	private static Map<String,String> algorithmToFirstSentence = new HashMap<String,String> ();

	static
	{
		chapterNames.put ("1" , "Introduction");
		chapterNames.put ("2" , "Definitions and notation");
		chapterNames.put ("3" , "Performance Metrics in Networks");
		chapterNames.put ("4" , "Routing Problems");
		chapterNames.put ("5" , "Capacity Assignment Problems");
		chapterNames.put ("6" , "Congestion Control Problems");
		chapterNames.put ("7" , "Topology Design Problems");
		chapterNames.put ("8" , "Gradient Algorithms in Network Design");
		chapterNames.put ("9" , "Primal Gradient Algorithms");
		chapterNames.put ("10" , "Dual Gradient Algorithms");
		chapterNames.put ("11" , "Decomposition Techniques");
		chapterNames.put ("12" , "Heuristic Algorithms");
		chapterNames.put ("A" , "Convex Sets. Convex Functions.");
		chapterNames.put ("B" , "Mathematical Optimization Basics");
		chapterNames.put ("C" , "Complexity Theory");
		chapterNames.put ("D" , "Net2Plan");

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
		String tagName = "net2plan.ocnbooksections";
		String outputFolder = null;
		for (String [] option : root.options ()) if (option [0].equals ("-outputFolder")) outputFolder = option [1];
		if (outputFolder == null) throw new RuntimeException ("You should indicate the output file");
		writeContents(root.classes(), tagName , outputFolder);
		return true;
	}

	private static void writeContents(ClassDoc[] classes, String tagName , String outputFolder)
	{
		for (int i = 0; i < classes.length; i++)
		{
			final ClassDoc javaClass = classes [i];
			final String className = javaClass.qualifiedName();
			final Tag [] firstSentenceTags = javaClass.firstSentenceTags();
			if (firstSentenceTags.length == 0) System.out.println("Create book sections: a class without first sentence class: " + className);
			final String firstSentenceThisClass = (firstSentenceTags.length == 0)? "" : javaClass.firstSentenceTags() [0].text ();
			algorithmToFirstSentence.put (className , firstSentenceThisClass);
			algorithmToSectionsMap.put (className , new LinkedList<String> ());
			algorithmToExercisesMap.put (className , new LinkedList<String> ());
			String keywordsString = ""; for (Tag tag : javaClass.tags()) {  if (tag.name().equals ("@"+tagName)) keywordsString += " " + tag.text () + " "; }
			keywordsString = keywordsString.trim ();
			for (String keyword : StringUtils.split(keywordsString , ","))
			{
				final String keywordName = keyword.trim ();
				if (keywordName.startsWith("Section "))
				{
					final String sectionNumber = keywordName.substring ("Section ".length()).trim ();
					if (!sectionsToAlgorithmsMap.containsKey(sectionNumber)) sectionsToAlgorithmsMap.put (sectionNumber , new LinkedList<String> ());
					sectionsToAlgorithmsMap.get(sectionNumber).add (className);
					algorithmToSectionsMap.get (className).add (sectionNumber);
				}
				else if (keywordName.startsWith("Exercise "))
				{
					final String exerciseNumber = keywordName.substring ("Exercise ".length()).trim ();
					if (!exercisesToAlgorithmsMap.containsKey(exerciseNumber)) exercisesToAlgorithmsMap.put (exerciseNumber , new LinkedList<String> ());
					exercisesToAlgorithmsMap.get(exerciseNumber).add (className);
					algorithmToExercisesMap.get (className).add (exerciseNumber);
				}
				else throw new RuntimeException ("In the tag net2plan.ocnbooksections: wrong tag value: " + keyword  + ", in class: " + className + ", keywordsString: " + keywordsString);
			}
		}
		System.out.println ("sectionsToAlgorithmsMap: " + sectionsToAlgorithmsMap);
		System.out.println ("exercisesToAlgorithmsMap: " + exercisesToAlgorithmsMap);
		System.out.println ("algorithmToSectionsMap: " + algorithmToSectionsMap);
		System.out.println ("sections: " + sectionsToAlgorithmsMap.keySet());
		System.out.println ("exercises: " + exercisesToAlgorithmsMap.keySet());
		
		String htmlFile = createHtml(outputFolder);
		try 
		{
			File f = new File (outputFolder + "/ocnbookSections.html");
			System.out.println ("Save in : " + f.getAbsolutePath());
			PrintWriter pw = new PrintWriter (f);
			pw.append(htmlFile);
			pw.close ();
		} catch (Exception e) { e.printStackTrace(); throw new RuntimeException ("Not possible to write in ocnbookSections.html"); } 
		
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
	
	private static String createHtml (String outputFolder)
	{
		String s = "";
		s += "<html><head>\n";
		s+= "<title>Index of Net2Plan resources for the \"Optimization of computer networks...\" book</title>\n";
		s+= "<link rel=\"stylesheet\" type=\"text/css\" href=\"stylesheet.css\" title=\"Style\">\n";
		s += "<script type=\"text/javascript\" src=\"script.js\"></script>\n";
		s+= "</head>\n";
		s+= "<body>\n";
		s += navBar;
		s+= "<div class=\"contentContainer\">\n";
		s += "<table cellpadding=2 cellspacing=0><tr>\n";
		s += "<p>This page is a per-chapter index of the Net2Plan resources (online and offline algorithms, reports) appearing in the book:</p>\n";
		s += "</tr><tr>\n";
		s += "<td><a href=\"http://www.wiley.com/WileyCDA/WileyTitle/productCd-1119013356.html\">Pablo Pav�n Mariño, <cite>Optimization of computer networks. Modeling and algorithms. A hands-on approach</cite>, Wiley 2016.</a></td>\n";
		s += "<td><a href=\"http://www.wiley.com/WileyCDA/WileyTitle/productCd-1119013356.html\"><img src=\"doc-files/bookCover.jpg\" alt=\"Pablo Pav&oacute;n Mari&ntilde;o, \"Optimization of computer networks. Modeling and algorithms. A hands-on approach\", Wiley 2016\" style=\"width:60px;height:87px;\"></a></td>\n";
		s += "</tr></table>";
		s+= "</div>\n";
		for (Entry<String,String> entryChapter : chapterNames.entrySet())
		{
			final String chapterNumber = entryChapter.getKey ();
			final String chapterTitle = entryChapter.getValue ();
			List<String> sectionNumbersThisChapter = new LinkedList<String> ();
			List<String> exerciseNumbersThisChapter = new LinkedList<String> ();
			for (String sectionNumber : sectionsToAlgorithmsMap.keySet ()) if (sectionNumber.startsWith(chapterNumber + ".")) sectionNumbersThisChapter.add (sectionNumber.substring (sectionNumber.indexOf(".") + 1));
			for (String exerciseNumber : exercisesToAlgorithmsMap.keySet ()) if (exerciseNumber.startsWith(chapterNumber + ".")) exerciseNumbersThisChapter.add (exerciseNumber.substring (exerciseNumber.indexOf(".") + 1));
			Collections.sort (sectionNumbersThisChapter,  new MyComparator ());
			Collections.sort (exerciseNumbersThisChapter , new MyComparator ());
			if (sectionNumbersThisChapter.isEmpty() && exerciseNumbersThisChapter.isEmpty()) continue;
			s+= "<div class=\"contentContainer\">\n";
			s+= "<table class=\"overviewSummary\" border=\"0\" cellpadding=\"3\" cellspacing=\"0\" summary=\"Index of Net2Plan resources for the \"Optimization of computer networks...\" book\">\n";
			s+= "<caption><span>Chapter " + chapterNumber + "</span><span class=\"tabEnd\">&nbsp;</span></caption>\n";
			s+= "<tr>\n";
			s+= "<th class=\"colFirst\" scope=\"col\">" + chapterTitle + "</th>\n";
			s+= "<th class=\"colLast\" scope=\"col\">" + "" + "</th>\n";
			s+= "</tr>\n";
			s+= "<tbody>\n";
			for (String sectionNumber : sectionNumbersThisChapter)
			{
				final List<String> algorithms = sectionsToAlgorithmsMap.get (chapterNumber + "." + sectionNumber);
				if (algorithms == null) throw new RuntimeException ("Unexpected");
				s+= "<tr class=\"altColor\">\n";
				s+= "<td class=\"colFirst\"><a name=\"Section" + chapterNumber + "." + sectionNumber + "\">Section " + chapterNumber + "." + sectionNumber + "</a></td>\n";
				s+= "<td class=\"colLast\">\n";
				s+= "<div class=\"block\">";
				s+= "<ul>";
				for (String exampleName : algorithms)
				{
					String shortExampleName = exampleName.substring (exampleName.lastIndexOf(".") + 1);
					String linkToJavadoc = exampleName.replace(".".toCharArray() [0] , "/".toCharArray() [0]) + ".html";
					String firstSentence = algorithmToFirstSentence.get (exampleName);
					s+= "<li><a href=\"" + linkToJavadoc + "\">" + shortExampleName + "</a>: " + firstSentence + "</li>\n";
				}
				s += "</div>\n";
				s+= "</td>\n";
				s+= "</tr>\n";
			}
			s+= "</ul>";
			for (String exerciseNumber : exerciseNumbersThisChapter)
			{
				final List<String> algorithms = exercisesToAlgorithmsMap.get (chapterNumber + "." + exerciseNumber);
				if (algorithms == null) throw new RuntimeException ("Unexpected");
				s+= "<tr class=\"altColor\">\n";
				s+= "<td class=\"colFirst\"><a name=\"Exercise" + chapterNumber + "." + exerciseNumber + "\">Exercise " + chapterNumber + "." + exerciseNumber + "</a></td>\n";
				s+= "<td class=\"colLast\">\n";
				s+= "<div class=\"block\">";
				s+= "<ul>";
				for (String exampleName : algorithms)
				{
					String shortExampleName = exampleName.substring (exampleName.lastIndexOf(".") + 1);
					String linkToJavadoc = exampleName.replace(".".toCharArray() [0] , "/".toCharArray() [0]) + ".html";
					String firstSentence = algorithmToFirstSentence.get (exampleName);
					s+= "<li><a href=\"" + linkToJavadoc + "\">" + shortExampleName + "</a>: " + firstSentence + "</li>\n";
				}
				s+= "</ul>";
				s += "</div>\n";
				s+= "</td>\n";
				s+= "</tr>\n";
			}
			s+= "</tbody>\n";
			s+= "</table>\n";
			s+= "</div>\n";
		}	
		s+= "</body>\n";
		s+= "</html>\n";

		return s;
	}
	
	private static class MyComparator implements Comparator<String>
	{
		public int	compare(String o1, String o2)
		{
			int initialPosition_1 = 0;
			int initialPosition_2 = 0;
			do
			{
				final int o1NextIndex = o1.indexOf ("." , initialPosition_1);
				final int o2NextIndex = o2.indexOf ("." , initialPosition_2);
				String firstNumber = o1NextIndex != -1? o1.substring(initialPosition_1 , o1NextIndex ) : o1.substring(initialPosition_1);
				String secondNumber = o2NextIndex != -1? o2.substring(initialPosition_2 , o2NextIndex ) : o2.substring(initialPosition_2);
				if (Integer.parseInt(firstNumber) < Integer.parseInt (secondNumber)) return -1;
				if (Integer.parseInt(firstNumber) > Integer.parseInt (secondNumber)) return 1;
				if ((o1NextIndex == -1) && (o2NextIndex == -1)) return 0;
				if (o1NextIndex == -1) return -1;
				if (o2NextIndex == -1) return 1;
				initialPosition_1 = o1NextIndex + 1;
				initialPosition_2 = o2NextIndex + 1;
			} while (true);
		}
	}

}
