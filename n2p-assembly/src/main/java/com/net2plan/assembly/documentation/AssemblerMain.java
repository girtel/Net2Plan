package com.net2plan.assembly.documentation;



import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;

import com.net2plan.assembly.constants.Constants;
import com.net2plan.assembly.utils.ZipUtils;
import com.net2plan.examples.general.offline.Offline_ipOverWdm_routingSpectrumAndModulationAssignmentHeuristicNotGrooming;
import com.net2plan.examples.general.offline.nfv.Offline_nfvPlacementILP_v1;
import com.net2plan.examples.general.onlineSim.Online_evGen_doNothing;
import com.net2plan.examples.general.reports.Report_WDM_lineEngineering_GNModel;
import com.net2plan.examples.ocnbook.offline.Offline_ba_numFormulations;
import com.net2plan.examples.ocnbook.onlineSim.Online_evGen_generalGenerator;
import com.net2plan.examples.ocnbook.reports.Report_availability;
import com.net2plan.gui.GUILauncher;
import com.net2plan.interfaces.networkDesign.Configuration;
import com.net2plan.interfaces.networkDesign.dynamicSrgs.DynamicSrgAllBidiLinksTwoNodes;
import com.net2plan.interfaces.simulation.SimEvent;
import com.net2plan.libraries.GraphUtils;
import com.net2plan.niw.DefaultStatelessSimulator;
import com.net2plan.utils.RandomUtils;
import com.net2plan.utils.StringUtils;



/**
 */
public class AssemblerMain 
{

	private static List<String> log = new ArrayList<> ();
	public static void log (String s) { log.add(s); }
	
	/* Configuration variables */
	public final static String versionOfJars = "0.7.0"; // Change for a varible
	public final static File outputAssemblyDirectory = Constants.computerThisIsRunning.localFileForAssembly; // better if outside the GIT domain
	
	/* Control [non-configurable] variables */
	
	private final static Optional<SortedSet<String>> acceptedSuffixesForZippingInAssembly_n2pCore = Optional.of (new TreeSet<> (Arrays.asList(".class" , ".jar", ".txt" , ".MF" , ".properties" , ".css.map" , ".css" , ".js.map" , ".js", ".json", ".xsl" , ".gif" , ".xsd" , ".html" , ".n2p" , ".pdf"))); 
	private final static Optional<SortedSet<String>> acceptedSuffixesForZippingInAssembly_n2pGui = Optional.of (new TreeSet<> (CollectionUtils.union(acceptedSuffixesForZippingInAssembly_n2pCore.get() , Arrays.asList(".js" , "js.map" , ".png", ".ico", ".icns" , ".gif" , ".bmp" , ".jpg" , ".css", ".json", ".md", "out8", "out6", "package-list")))); 
	private final static Optional<SortedSet<String>> acceptedSuffixesForZippingInAssembly_n2pBuiltInExamples = Optional.of (new TreeSet<> (CollectionUtils.union(acceptedSuffixesForZippingInAssembly_n2pCore.get() , Arrays.asList(".js" , "js.map" , ".png", ".ico", ".icns" , ".gif" , ".bmp" , ".jpg" , ".css", ".json", ".md", ".java")))); 
	private final static Optional<SortedSet<String>> acceptedSuffixesForZippingInAssembly_javadoc = Optional.empty (); 
	private static final File currentSystemDirectory = new File (System.getProperty("user.dir")); 
	private static final File outputGlobalDirectory_zipFiles = new File (outputAssemblyDirectory , "compressedFiles"); 
	private static final File outputGlobalDirectory_unzipped = new File (outputAssemblyDirectory , "unzipped"); 
	public static final File outputGlobalTemporalDirectory = new File (outputAssemblyDirectory , "temporalFolderShouldBeRemoved"); 
	public static final File outputGlobalTemporalDirectory_workspace = new File (outputGlobalTemporalDirectory , "workspace"); 
	public static final File outputGlobalTemporalDirectory_data = new File (outputGlobalTemporalDirectory_workspace , "data"); 
	public static final File outputGlobalTemporalDirectory_doc = new File (outputGlobalTemporalDirectory , "doc"); 
	public static final File outputGlobalTemporalDirectory_javadoc = new File (outputGlobalTemporalDirectory_doc , "javadoc"); 
	public static final File parentFolderOfAllModules = currentSystemDirectory.getAbsoluteFile().getParentFile();
	private static final File assembledZipFile_core = new File (outputGlobalDirectory_zipFiles , "Net2Plan-core-" + versionOfJars + ".zip");
	private static final File assembledZipFile_gui = new File (outputGlobalDirectory_zipFiles , "Net2Plan-gui-" + versionOfJars + ".zip");
	private static final File assembledZipFile_javadocApi = new File (outputGlobalDirectory_zipFiles , "Net2Plan-javadoc-API-" + versionOfJars + ".zip");
	private static final File assembledZipFile_javadocExamples = new File (outputGlobalDirectory_zipFiles , "Net2Plan-javadoc-Examples-" + versionOfJars + ".zip");
	public static final File pomFile_general = new File (parentFolderOfAllModules , "pom.xml");
	public static final File pomFile_n2pGui = getChild (parentFolderOfAllModules , "n2p-gui" , "pom.xml");
	public static final File pomFile_n2pCore = getChild (parentFolderOfAllModules , "n2p-core" , "pom.xml");

	public static boolean getYesNo (Scanner in , boolean defaultVal , String message)
	{
		System.out.print(message + " [" + (defaultVal? "Y" : "y") + "/" + (defaultVal? "n" : "N") + "]: ");
		final String res = in.nextLine();
		if (res.equalsIgnoreCase("y")) return true;
		if (res.equalsIgnoreCase("yes")) return true;
		if (res.equalsIgnoreCase("n")) return false;
		if (res.equalsIgnoreCase("no")) return false;
		return defaultVal;
	}
	
	private static void assembleFileLaunch (Scanner in , File f , Runnable fileCreator)
	{
		System.out.print("Assembling the file: " + f.getName());
    	if (f.isFile())
    	{
    		final boolean create = getYesNo(in, false, " The zip file " + f.getName() + " already exists. Create again?");
    		if (create) fileCreator.run();;
    	}
    	else 
    	{
    		System.out.println(" The file does not exist. Creating it.");
    		fileCreator.run();
    	}
	}
	
	public static void main( String[] args ) throws Throwable
    {
		/* Checks */
//		ConfigurationData.checkConsistency();
		
    	System.out.println("System directory: " + currentSystemDirectory);
    	System.out.println("Output of the assembly elements in directory: " + outputGlobalDirectory_zipFiles);
    	if (!currentSystemDirectory.getName().equals("n2p-assembly")) throw new RuntimeException("The current directoy should be the n-assembly directory");
		final Scanner in = new Scanner(System.in);
    	
    	/* Initialize the output assembly directory */
		final boolean resetZips = getYesNo(in, false, "Do you want a fresh start, eliminaty any previous ZIP files with information in diectory " + outputGlobalDirectory_zipFiles.getCanonicalPath() + "?");
		if (resetZips)
    	{
			resetDirectory(outputAssemblyDirectory);
	    	resetDirectory(outputGlobalDirectory_zipFiles);
    	}
    	FileUtils.forceMkdir(getChild(outputGlobalDirectory_unzipped, "gui"));
    	
    	assembleFileLaunch (in , assembledZipFile_core , ()-> createZip_n2pCore () );
    	assembleFileLaunch (in , assembledZipFile_gui , ()-> createZip_n2pGuiApplication ());
    	assembleFileLaunch (in , assembledZipFile_javadocApi , ()-> createZip_javadocAPI () );
    	assembleFileLaunch (in , assembledZipFile_javadocExamples , ()-> createZip_javadocExamples () );
    	
    	/* Remove the temporal directory */
    	FileUtils.deleteQuietly(outputGlobalTemporalDirectory);

    	/* Unzip each in the n2pTests folder */
    	FileUtils.forceMkdir(getChild(outputGlobalDirectory_unzipped, "gui"));
    	FileUtils.forceMkdir(getChild(outputGlobalDirectory_unzipped, "server"));
    	
    	System.out.println("Unzipping in test folder...");
    	ZipUtils.unzip (assembledZipFile_core , getChild (outputGlobalDirectory_unzipped , "core"));
    	ZipUtils.unzip (assembledZipFile_gui , getChild (outputGlobalDirectory_unzipped , "gui"));
    	System.out.println("Finish (ok)");
    	
    	System.exit(0);
    }

    public static void createZip_n2pGuiApplication () 
    {
    	try
    	{
        	/* Reset the temporal directory for zipping */
        	resetDirectory(outputGlobalTemporalDirectory);
        	
        	/* Create the output directory */
        	final File inputDirectoryOfSubmodule = getChild(parentFolderOfAllModules , "n2p-gui");
        	assert inputDirectoryOfSubmodule.isDirectory();
        	
        	/* Copy README to root of output directory */
        	final String RET = System.getProperty("line.separator");
        	final String readString = 
        			"<p align=\"center\">" + RET
        			+ "    <img src=\"https://raw.githubusercontent.com/girtel/Net2Plan/develop/Net2Plan-GUI/Net2Plan-GUI-Plugins/Net2Plan-NetworkDesign/src/main/resources/resources/common/net2plan-logo.jpg\" height=\"300\">" + RET
        			+ "</p>" + RET
        			+ "<p align=\"center\">" + RET
        			+ "    <img src=\"https://travis-ci.org/girtel/Net2Plan.svg?branch=master\">" + RET
        			+ "    <img src=\"https://s-a.github.io/license/img/bsd-2-clause.svg\">" + RET
        			+ "</p>" + RET
        			+ "" + RET
        			+ "# Introduction" + RET
        			+ "Net2Plan is a Java developed tool for the planning, optimization and evaluation of communication networks." + RET
        			+ "" + RET
        			+ "For further information, please feel free to follow the next web pages:" + RET
        			+ "* The [Net2Plan website](www.net2plan.com)." + RET
        			+ "* The [Net2Plan user's guide](http://net2plan.com/documentation/current/help/usersGuide.pdf)." + RET
        			+ "* The [Net2Plan API Javadoc](http://net2plan.com/documentation/current/javadoc/api/index.html)." + RET
        			+ "" + RET
        			+ "# About Net2Plan" + RET
        			+ "Net2Plan is a free and open-source Java tool devoted to the planning, optimization and evaluation of communication networks. It was originally thought as a tool to assist on the teaching of communication networks courses. Eventually, it got converted into a powerful network optimization and planning tool for both the academia and the industry, together with a growing repository of network planning resources." + RET
        			+ "" + RET
        			+ "Net2Plan is built on top of an abstract network representation, so-called network plan, based on abstract components: nodes, links, traffic unicast and multicast demands, routes, multicast trees, shared-risk groups, resources and network layers. The network representation is technology-agnostic, thus Net2Plan can be adapted for planning networks of any technology. Technology-specific information can be introduced in the network representation via user-defined attributes attached to any of the abstract components mentioned above. Some attribute names has been fixed to ease the adaptation of well-known technologies (e.g. IP networks, WDM networks, NFV scenarios)." + RET
        			+ "" + RET
        			+ "Net2Plan is implemented as a Java library along with both command-line and graphical user interfaces (CLI and GUI, respectively). The GUI is specially useful for laboratory sessions as an educational resource, or for a visual inspection of the network. In its turn, the command-line interface is specifically devoted to in-depth research studies, making use of batch processing or large-scale simulation features. Therefore, Net2Plan is a tool intended for a broad spectrum of users: industry, research, and academia." + RET
        			+ "" + RET
        			+ "# Building instructions" + RET
        			+ "Since Net2Plan 0.4.1, the project is being built through the use of Maven." + RET
        			+ "" + RET
        			+ "The Maven command to build the project is the following:" + RET
        			+ "" + RET
        			+ "`clean package`" + RET
        			+ "" + RET
        			+ "The result is a _Net2Plan-VERSION.zip_ package containing the resulting program. By default, this file can be found under the _target_ folder of the _Net2Plan-Assembly_ module." + RET
        			+ "" + RET
        			+ "# Developer tools" + RET
        			+ "Net2Plan offers an alternative method of running the program without the need of going through the packaging phase. This is achieved by running the main method under:" + RET
        			+ "" + RET
        			+ "`com.net2plan.launcher.GUILauncher`" + RET
        			+ "" + RET
        			+ "Which can be found at:" + RET
        			+ "" + RET
        			+ "`Net2Plan -> Net2Plan-Launcher`" + RET
        			+ "" + RET
        			+ "Note that this is meant for running the graphical interface of the project." + RET
        			+ "" + RET
        			+ "# Learning materials" + RET
        			+ "New users may want to checkout the Net2Plan's [Youtube channel](https://www.youtube.com/channel/UCCgkr1wlMlO221yhFGmWZUg), alongside the [user's guide](http://net2plan.com/documentation/current/help/usersGuide.pdf), for an introduction into the basics of the provided tools. " + RET
        			+ "" + RET
        			+ "# License" + RET
        			+ "Net2Plan is licensed under the [Simplified BSD License](https://opensource.org/licenses/BSD-2-Clause). Meaning that it is completely free and open-source. As a consequence of this, you may use parts of Net2Plan or the complete package inside your own programs for free, can make money from them and do not have to disclose your code. Although, you are obliged to mention that you are using Net2Plan in your program.";
        	FileUtils.writeStringToFile(new File (outputGlobalTemporalDirectory , "README.md"), readString, "UTF-8");
        	
        	final String licenseString = "BSD 2-Clause License" + RET
        			+ "" + RET
        			+ "Copyright (c) 2017, Pablo Pavon Marino" + RET
        			+ "All rights reserved." + RET
        			+ "" + RET
        			+ "Redistribution and use in source and binary forms, with or without" + RET
        			+ "modification, are permitted provided that the following conditions are met:" + RET
        			+ "" + RET
        			+ "* Redistributions of source code must retain the above copyright notice, this" + RET
        			+ "  list of conditions and the following disclaimer." + RET
        			+ "" + RET
        			+ "* Redistributions in binary form must reproduce the above copyright notice," + RET
        			+ "  this list of conditions and the following disclaimer in the documentation" + RET
        			+ "  and/or other materials provided with the distribution." + RET
        			+ "" + RET
        			+ "THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS IS\"" + RET
        			+ "AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE" + RET
        			+ "IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE" + RET
        			+ "DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE" + RET
        			+ "FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL" + RET
        			+ "DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR" + RET
        			+ "SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER" + RET
        			+ "CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY," + RET
        			+ "OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE" + RET
        			+ "OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.";
        	FileUtils.writeStringToFile(new File (outputGlobalTemporalDirectory , "LICENSE.md"), licenseString, "UTF-8");
        	
        	final String changelogString = "# Changelog" + RET
        			+ "" + RET
        			+ "All notable changes to Net2Plan will be documented in this file." + RET
        			+ "" + RET
        			+ "The format is based on [Keep a Changelog](http://keepachangelog.com/) and this project adheres to [Semantic Versioning](http://semver.org/)." + RET
        			+ "## [0.6.5.0] - 2019-12-18" + RET
        			+ "" + RET
        			+ "### Added" + RET
        			+ "	- Significant improvements in the NIW framework. Model and graphical interface." + RET
        			+ "" + RET
        			+ "## [0.6.4.0] - 2019-10-20" + RET
        			+ "" + RET
        			+ "### Added" + RET
        			+ "	- Included NIW (NFV over IP over WDM) library in the core, and added GUI specific options for it. " + RET
        			+ "" + RET
        			+ "" + RET
        			+ "## [0.6.3.2] - 2019-07-15" + RET
        			+ "" + RET
        			+ "### Fixed" + RET
        			+ "	- Upgraded JGraphT library to 1.3.1 version to fix several bugs." + RET
        			+ "" + RET
        			+ "" + RET
        			+ "####" + RET
        			+ "" + RET
        			+ "" + RET
        			+ "## [0.6.3.1] - 2019-05-31" + RET
        			+ "" + RET
        			+ "### Added" + RET
        			+ "	- SAN based algorithm for distance geometry problem." + RET
        			+ "	- Property \"Nominal Color\" to link class." + RET
        			+ "	" + RET
        			+ "### Removed" + RET
        			+ "	- NIW classes." + RET
        			+ "" + RET
        			+ "### Fixed" + RET
        			+ "	- Several minor bugs associated to the tables." + RET
        			+ "" + RET
        			+ "" + RET
        			+ "####" + RET
        			+ "" + RET
        			+ "" + RET
        			+ "## [0.6.3] - 2019-04-29" + RET
        			+ "" + RET
        			+ "### Added" + RET
        			+ "	- Several new examples (reports)." + RET
        			+ "" + RET
        			+ "### Fixed" + RET
        			+ "	- Several minor bugs." + RET
        			+ "" + RET
        			+ "" + RET
        			+ "####" + RET
        			+ "" + RET
        			+ "## [0.6.2] - 2019-03-27" + RET
        			+ "" + RET
        			+ "### Added" + RET
        			+ "	- Monitoring Panel to show traffic monitoring of links, demands and multicast demands." + RET
        			+ "" + RET
        			+ "### Changed" + RET
        			+ "	- Focus Panel is now placed inside the tables panel, next to Monitoring Panel." + RET
        			+ "	- Several columns names have been renamed." + RET
        			+ "" + RET
        			+ "" + RET
        			+ "####" + RET
        			+ "" + RET
        			+ "## [0.6.1.2] - 2019-01-09" + RET
        			+ "" + RET
        			+ "### Fixed" + RET
        			+ "	- Bug in Report header when showed in navigator." + RET
        			+ "	" + RET
        			+ "####" + RET
        			+ "" + RET
        			+ "## [0.6.1.1] - 2018-11-05" + RET
        			+ "" + RET
        			+ "### Fixed" + RET
        			+ "	- Different bugs in Smart City algorithms." + RET
        			+ "	" + RET
        			+ "####" + RET
        			+ "" + RET
        			+ "## [0.6.1] - 2018-10-24" + RET
        			+ "" + RET
        			+ "### Added" + RET
        			+ "	- New GUI to add multicast demands and multicast trees." + RET
        			+ "	- Dialog confirmation when loading a design in a right way." + RET
        			+ "	- Waste collection algorithms." + RET
        			+ "	- NIW library to design NFV algorithms." + RET
        			+ "### Changed" + RET
        			+ "	- GUI improvements." + RET
        			+ "### Fixed" + RET
        			+ "	- Some bug fixes." + RET
        			+ "" + RET
        			+ "	" + RET
        			+ "####" + RET
        			+ "" + RET
        			+ "## [0.6.0.1] - 2018-07-11 " + RET
        			+ "" + RET
        			+ "### Fixed" + RET
        			+ "	- Some bug fixes." + RET
        			+ "" + RET
        			+ "####" + RET
        			+ "" + RET
        			+ "## [0.6.0] - 2018-07-05" + RET
        			+ "### Added" + RET
        			+ "	- Possibility to attach monitoring traces of carried traffic (series of pairs date-traffic) to the links, and also traces of offered traffic to demands and multicast demands. Methods to synthetically create, import and export from Excel, and manipulate those traces are included in the GUI. " + RET
        			+ "	- Basic forecasting functionalities, also accessible from the GUI, to predict traffic in links and demands from the monitoring traces. " + RET
        			+ "	- Basic traffic matrix estimation functionalities, also accessible from the GUI, to predict demands and multicast demands traffic, from link occupations. " + RET
        			+ "	- QoS functionalities. Demands and multicast demands can be tagged with a user-defined QoS. Then, the user can define scheduling policies, that assign link bandwidth to the traversing demands and multicast demands with user-defined priorities, according to their QoS." + RET
        			+ "	- Resources can now we dettached from the nodes, and attached later to other nodes." + RET
        			+ "	- Nodes now can have different (X,Y) positions at different user-defined layouts." + RET
        			+ "	- Links can now be coupled to demands in the same layer, as long as no coupling cycles occur. " + RET
        			+ "	- Demands can now be of the type source routing or hop-by-hop routing. Previously, all the demands in the same layer where forced to be of the same type." + RET
        			+ "	- Multicast trees can now be trees that reach just a subset of the egress nodes of the associated demand." + RET
        			+ "	- Shared risk groups can now also be dynamic, meaning that the associated links and nodes are defined by a function of the network state, not statically set." + RET
        			+ "	- Internally, most of the methods work with SortedSets instead of Set, so the ordering when iterating those sets is platform independent (in its turn, the order in Set is not defined, and can change from one from one program execution to the next). " + RET
        			+ "	- GUI: Possibility to navigate through the tables, e.g. picking the origin node of a link in the links table, picks that node in the nodes table. It is possible to move back and forward the picked elements." + RET
        			+ "	- GUI: Easier selection of the current active layer in a left panel." + RET
        			+ "	- GUI: The tabbed panel at the right, now shows ALL the layers information, not just the active layer." + RET
        			+ "    " + RET
        			+ "### Fixed" + RET
        			+ "	- Some bug fixes." + RET
        			+ "" + RET
        			+ "####" + RET
        			+ "" + RET
        			+ "" + RET
        			+ "" + RET
        			+ "## [0.5.3] - 2018-02-14" + RET
        			+ "### Added" + RET
        			+ "    - Added Optical Fiber Utils." + RET
        			+ "	- Report for Optical Fiber networks following calculations of Gaussian  Noise Model." + RET
        			+ "    " + RET
        			+ "### Fixed" + RET
        			+ "    - Site mode does not break the tool when loading a new topology." + RET
        			+ "	- Corrected layer table tags." + RET
        			+ "	- Sorting table columns works as it should." + RET
        			+ "	- Other bug fixes." + RET
        			+ "" + RET
        			+ "####" + RET
        			+ "" + RET
        			+ "" + RET
        			+ "## [0.5.2] - 2017-05-24" + RET
        			+ "### Added" + RET
        			+ "    - Added a new tab under the \"Demands\" table for traffic matrix visualization and control." + RET
        			+ "    - Added a new visualization option for giving color to links based on their current utilization." + RET
        			+ "    " + RET
        			+ "### Changed" + RET
        			+ "    - Reworked multi-layer control panel to work as a non-intrusive pop-up." + RET
        			+ "    - Separated canvas display from table display: Pick option." + RET
        			+ "    - Changed project license to BSD 2-clause." + RET
        			+ "    " + RET
        			+ "### Fixed" + RET
        			+ "    - Focus panel refreshes correctly again." + RET
        			+ "    - Returned non-n2p loading modules: SNDLib..." + RET
        			+ "    - Can now load designs with no population attribute." + RET
        			+ "    - Giving order to a table now works as intended." + RET
        			+ "    - Canvas no longer draws layers in the inverse order." + RET
        			+ "" + RET
        			+ "####" + RET
        			+ "" + RET
        			+ "## [0.5.1-Beta.1] - 2017-04-20" + RET
        			+ "### Added" + RET
        			+ "    - Added capacity for multiple line selection on tables." + RET
        			+ "    - Exporting function for writing tables on an Excel file." + RET
        			+ "    - Added a helper under the options window for looking for solvers under the PC." + RET
        			+ "    - New development tools." + RET
        			+ "### Changed" + RET
        			+ "    - Improved performance for large scale algorithms." + RET
        			+ "    - Tools now ask for confirmation when changing between them." + RET
        			+ "    - Tables' window can now run all keyboard shortcuts." + RET
        			+ "### Fixed" + RET
        			+ "    - Solved a problem on the focus panel where it crashed when an element on its list was removed." + RET
        			+ "    - Solved right-click pop-up options order issues." + RET
        			+ "    - What-If analysis now correctly shows the help box." + RET
        			+ "    - \"View/Edit backup routes\" now displays properly." + RET
        			+ "    - Solved a problem where forwarding rules were not displayed on the focus panel." + RET
        			+ "    ";
        	FileUtils.writeStringToFile(new File (outputGlobalTemporalDirectory , "CHANGELOG.md"), changelogString, "UTF-8");

        	/* Copy dependencies to lib folder: external and n2p-core  */
        	final List<File> n2pCoreExternalDependencies = DependenciesCutAndPastes.getExternalDependencies_n2pCore();
        	final File directory_lib = getChild(outputGlobalTemporalDirectory , "lib");
        	FileUtils.forceMkdir(directory_lib);
        	for (File f : n2pCoreExternalDependencies)
            	FileUtils.copyFileToDirectory (f, directory_lib);
        	/* Copy the n2p-core-jar file, with a modified MANIFEST */
            createAndSaveModifiedVersionOfN2pCoreJar (getChild (outputGlobalTemporalDirectory , "lib" , "n2p-core-" + versionOfJars + ".jar"));

        	/* Create the /data folder into workspace folder */
        	final File directory_data = getChild(outputGlobalTemporalDirectory_workspace , "data");
        	FileUtils.forceMkdir(directory_data);
        	
        	/* Write the modified version of the GUI (with new classpath) */
        	createAndSaveModifiedVersionOfGuiJar (getChild(outputGlobalTemporalDirectory));
        	
        	/* Write the BuildInExample.jar into workspace directory */
        	createAndSaveBuildInExampleJar(getChild(outputGlobalTemporalDirectory_workspace));
        	
        	/* Copy networkTopologies and trafficMatrices to data directory*/
        	final File networkTopologiesDirectoryCopyToGui = getChild(parentFolderOfAllModules , "n2p-examples", "src", "test", "resources", "data", "networkTopologies");
        	FileUtils.copyDirectoryToDirectory(networkTopologiesDirectoryCopyToGui, outputGlobalTemporalDirectory_data);
        	final File trafficMatricesDirectoryCopyToGui = getChild(parentFolderOfAllModules , "n2p-examples", "src", "test", "resources", "data", "trafficMatrices");
        	FileUtils.copyDirectoryToDirectory(trafficMatricesDirectoryCopyToGui, outputGlobalTemporalDirectory_data);
        	
           	/* Copy help folder to doc directory*/
        	final File helpDirectoryCopyToGui = getChild(parentFolderOfAllModules , "n2p-core", "src", "main", "resources", "help");
        	FileUtils.copyDirectoryToDirectory(helpDirectoryCopyToGui, outputGlobalTemporalDirectory_doc);

           	/* Copy parallelcolt folder to doc/javadoc directory*/
        	final File parallelcoltDirectoryCopyToGui = getChild(parentFolderOfAllModules , "n2p-examples", "src", "test", "resources", "parallelcolt");
        	FileUtils.copyDirectoryToDirectory(parallelcoltDirectoryCopyToGui, outputGlobalTemporalDirectory_javadoc);
        	
        	/* Create the ZIP file with all together */
        	ZipUtils.zipDirectory(outputGlobalTemporalDirectory, assembledZipFile_gui, false , acceptedSuffixesForZippingInAssembly_n2pGui);

        	/* Remove the temporal directory */
        	FileUtils.deleteQuietly(outputGlobalTemporalDirectory);

    	} catch (IOException e) { throw new RuntimeException (e.getMessage()); }
    }

    private static void createAndSaveBuildInExampleJar(File n2pCoreJarFileCopyInOutputFolder) throws IOException
    {
    	final File inputDirectoryOfSubmodule = getChild(parentFolderOfAllModules , "n2p-examples");
    	final File jarFileN2pCore = getChild(inputDirectoryOfSubmodule , "target" , "BuiltInExamples.jar");
    	final File rootFolderOfClassesOfCore = getChild(inputDirectoryOfSubmodule , "src" , "main", "java");
    	if (!rootFolderOfClassesOfCore.isDirectory()) throw new RuntimeException("Classes directory not found: " + rootFolderOfClassesOfCore.getAbsolutePath());
    	    	
    	/* Create JAR with its manifest */
    	final List<File> n2pCoreExternalDependencies = DependenciesCutAndPastes.getExternalDependencies_n2pCore();
    	final List<String> jarFilesInClassPathNames = n2pCoreExternalDependencies.stream().map(e->e.getName().trim()).collect(Collectors.toList());
    	final Manifest manifest = ManifestManipulator.getEnpGeneralManifest("n2p-examples", versionOfJars, jarFilesInClassPathNames , Optional.empty(), Optional.empty());
    	
    	try (JarOutputStream target = new JarOutputStream(new FileOutputStream(jarFileN2pCore), manifest))
    	{
        	JarUtils.add (rootFolderOfClassesOfCore , rootFolderOfClassesOfCore , target);
    		target.close();
        	if (!jarFileN2pCore.isFile()) throw new RuntimeException("File not found: " + jarFileN2pCore.getAbsolutePath());
        	if (n2pCoreJarFileCopyInOutputFolder.isDirectory()) FileUtils.copyFileToDirectory(jarFileN2pCore, n2pCoreJarFileCopyInOutputFolder);
        	else FileUtils.copyFile(jarFileN2pCore, n2pCoreJarFileCopyInOutputFolder);
    	}

    	/* Check there are no files with forbidden extensions */
    	ZipUtils.checkAcceptableFileSuffixes (jarFileN2pCore , acceptedSuffixesForZippingInAssembly_n2pBuiltInExamples);

	}

	private static void createAndSaveModifiedVersionOfN2pCoreJar (File n2pCoreJarFileCopyInOutputFolder) throws IOException
    {
    	final File inputDirectoryOfSubmodule = getChild(parentFolderOfAllModules , "n2p-core");
    	final File jarFileN2pCore = getChild(inputDirectoryOfSubmodule , "target" , "n2p-core-" + versionOfJars + ".jar");
    	final File rootFolderOfClassesOfCore = getChild(inputDirectoryOfSubmodule , "target" , "classes");
    	if (!rootFolderOfClassesOfCore.isDirectory()) throw new RuntimeException("Classes directory not found: " + rootFolderOfClassesOfCore.getAbsolutePath());
    	
    	/* Create JAR with its manifest */
    	final List<File> n2pCoreExternalDependencies = DependenciesCutAndPastes.getExternalDependencies_n2pCore();
    	final List<String> jarFilesInClassPathNames = n2pCoreExternalDependencies.stream().map(e->e.getName().trim()).collect(Collectors.toList());
    	final Manifest manifest = ManifestManipulator.getEnpGeneralManifest("n2p-core", versionOfJars, jarFilesInClassPathNames , Optional.empty() , Optional.of("com.ne2plan.core"));
    	try (JarOutputStream target = new JarOutputStream(new FileOutputStream(jarFileN2pCore), manifest))
    	{
        	JarUtils.add (rootFolderOfClassesOfCore , rootFolderOfClassesOfCore , target);
    		target.close();
        	if (!jarFileN2pCore.isFile()) throw new RuntimeException("File not found: " + jarFileN2pCore.getAbsolutePath());
        	FileUtils.copyFile(jarFileN2pCore, n2pCoreJarFileCopyInOutputFolder);
    	}

    	/* Check there are no files with forbidden extensions */
    	ZipUtils.checkAcceptableFileSuffixes (jarFileN2pCore , acceptedSuffixesForZippingInAssembly_n2pCore);
    	
//    	ManifestManipulator.setNewManifestFileInJar_2(n2pCoreJarFileCopyInOutputFolder , "n2p-core" , versionOfJars , jarFilesInClassPathNames , Optional.empty() , Optional.of("com.ne2plan.core"));
    }

    private static void createAndSaveModifiedVersionOfGuiJar (File n2pGuiJarFileCopyInOutputFolder) throws IOException
    {
    	final File inputDirectoryOfSubmodule = getChild(parentFolderOfAllModules , "n2p-gui");
    	final File jarFileN2pGui = getChild(inputDirectoryOfSubmodule , "target" , "Net2Plan.jar");
    	final File rootFolderOfClasses = getChild(inputDirectoryOfSubmodule , "target" , "classes");

    	/* Create the JAR */
    	final List<File> n2pGuiDependencies = DependenciesCutAndPastes.getExternalDependencies_n2pGui();
    	n2pGuiDependencies.add(new File ("n2p-core-" + versionOfJars + ".jar"));
    	final List<String> jarFilesInClassPathNames = n2pGuiDependencies.stream().map(e->"lib/" + e.getName().trim()).collect(Collectors.toList());
        final Manifest manifest = ManifestManipulator.getEnpGeneralManifest("Net2Plan", versionOfJars, jarFilesInClassPathNames , Optional.of(GUILauncher.class.getName()) , Optional.empty());
    	try (JarOutputStream target = new JarOutputStream(new FileOutputStream(jarFileN2pGui), manifest))
    	{
        	JarUtils.add (rootFolderOfClasses , rootFolderOfClasses , target);
    		target.close();
    	}

    	if (!jarFileN2pGui.isFile()) throw new RuntimeException("File not found: " + jarFileN2pGui.getAbsolutePath());
    	
    	if(n2pGuiJarFileCopyInOutputFolder.isDirectory()) FileUtils.copyFileToDirectory(jarFileN2pGui, n2pGuiJarFileCopyInOutputFolder);
    	else FileUtils.copyFile(jarFileN2pGui, n2pGuiJarFileCopyInOutputFolder);



    	/* Check there are no files with forbidden extensions */
    	ZipUtils.checkAcceptableFileSuffixes (jarFileN2pGui , acceptedSuffixesForZippingInAssembly_n2pGui);


//    	/* Change the manifest with what we want */
//    	ManifestManipulator.setNewManifestFileInJar_2(n2pGuiJarFileCopyInOutputFolder , "n2p-gui" , versionOfJars , jarFilesInClassPathNames , Optional.of(GUINet2Plan.class.getName()) , Optional.empty());
    }
    
    public static void createZip_n2pCore () 
    {
    	try
    	{
        	/* Reset the temporal directory for zipping */
        	resetDirectory(outputGlobalTemporalDirectory);
        	
        	/* Create the output directory */
        	final File inputDirectoryOfSubmodule = getChild(parentFolderOfAllModules , "n2p");
        	assert inputDirectoryOfSubmodule.isDirectory();
        	
        	/* Copy dependencies & README to output directory */
        	final List<File> n2pCoreExternalDependencies = DependenciesCutAndPastes.getExternalDependencies_n2pCore();
        	for (File f : n2pCoreExternalDependencies)
            	FileUtils.copyFileToDirectory (f, outputGlobalTemporalDirectory);
        	final String readString = 
        			"This ZIP file includes the n2p-core Java library files: the library with Software Development Kit (SDK) for interacting programmatically with Net2Plan. "
        			+ "The SDK is contained in the n2p-core-XXX.jar file (where XXX is a version number). "
        			+ "The rest of the jar files are the required dependencies for the library.";
        	FileUtils.writeStringToFile(new File (outputGlobalTemporalDirectory , "README.txt"), readString, "UTF-8");
        	
        	/* Copy the n2p-core-jar file, with a modified MANIFEST */
            createAndSaveModifiedVersionOfN2pCoreJar (getChild (outputGlobalTemporalDirectory , "n2p-core-" + versionOfJars + ".jar"));

            /* Copy the .jar into the Maven repository, so later it is found when creating the dependencies programmatically of GUI jars */
            FileUtils.copyFile(getChild (outputGlobalTemporalDirectory , "n2p-core-" + versionOfJars + ".jar") , 
            		getChild (Constants.computerThisIsRunning.folderOfMavenRepository , "com" , "net2plan" , "n2p-core" , "0.6.6.0" , "n2p-core-0.6.6.0.jar"));
            
        	/* Create the ZIP file with all together */
        	ZipUtils.zipDirectory(outputGlobalTemporalDirectory, assembledZipFile_core, false , acceptedSuffixesForZippingInAssembly_n2pCore);
        	
        	/* Remove the temporal directory */
        	FileUtils.deleteQuietly(outputGlobalTemporalDirectory);
    	} catch (IOException e) { throw new RuntimeException (e.getMessage()); }
    }

    private static void resetDirectory (File folder) throws IOException
    {
    	FileUtils.deleteQuietly(folder);
    	FileUtils.forceMkdir(folder);
    }

    public static File getChild (File f , String... subdirs)
    {
    	if (subdirs.length == 0) return f;
    	if (subdirs.length == 1) return new File (f , subdirs [0]);
    	final String [] subdirsButFirst = new String [subdirs.length - 1];
    	for (int c = 1; c < subdirs.length ; c ++) subdirsButFirst [c-1] = subdirs [c];
    	return getChild (new File (f , subdirs [0]) , subdirsButFirst);
    }

    
    // See: https://docs.oracle.com/javase/8/docs/technotes/tools/unix/javadoc.html#CHDFDACB
    // Add the files one by one: if different packages, generates the package info?
    // IMPORTANT: The taglet adds to the classloader in the register method, those JARs in documented classes, that are imported there (e.g. ogr.jgrapht). If not added, fails. 
    private static void createZip_javadocAPI () 
    {
    	try
    	{
        	final File tempFolder_javadocFolder = getChild(outputGlobalTemporalDirectory, "javadoc");
        	FileUtils.forceMkdir(tempFolder_javadocFolder);
        	
        	final String dependenciesJarFiles = DependenciesCutAndPastes.getExternalDependencies_n2pCore().stream().map(e->e.getAbsoluteFile().toString()).collect(Collectors.joining(Constants.computerThisIsRunning.interDocletFolderSeparatorInJavadocCalls));

        	final File srcFolderCore = getChild (parentFolderOfAllModules , "n2p-core" , "src" , "main" , "java");
        	final File classFolderAssembler = getChild (parentFolderOfAllModules , "n2p-assembly" , "target" , "classes");
        	final File classFolderCore = getChild (parentFolderOfAllModules , "n2p-core" , "target" , "classes");
        	assert srcFolderCore.isDirectory();
        	final List<String> commands = new ArrayList<> ();
        	commands.add(Constants.computerThisIsRunning.pathToJavadocFile);

        	
        	/* Add all the .java files in package mtn, mtn.nodes, optionsStructure, serverInterfaces. The ones that DO NOT start with "_" */
        	final Package networkDesignPackage = Configuration.class.getPackage();
        	final Package networkDesignDynamicSrgsPackage = DynamicSrgAllBidiLinksTwoNodes.class.getPackage();
        	final Package simulationPackage = SimEvent.class.getPackage();
        	final Package librariesPackage = GraphUtils.class.getPackage();
        	final Package niwPackage = DefaultStatelessSimulator.class.getPackage();
        	final Package utilsPackage = RandomUtils.class.getPackage();
        	for (Package pack : Arrays.asList(networkDesignPackage , networkDesignDynamicSrgsPackage , simulationPackage , librariesPackage , niwPackage , utilsPackage))
        	{
        		final String [] packageFolders = StringUtils.split(pack.getName() , ".");
        		final String packageFolderWithCorrectSeparator = Arrays.asList(packageFolders).stream().collect (Collectors.joining("/"));
        		System.out.println(packageFolderWithCorrectSeparator);
        		final File folderOfJavaFilesthisPackage = getChild(srcFolderCore, packageFolders);
        		System.out.println(folderOfJavaFilesthisPackage.toString());
        		assert folderOfJavaFilesthisPackage.isDirectory();
        		for (File javaFile : folderOfJavaFilesthisPackage.listFiles())
        		{
        			if (!javaFile.getName().toLowerCase().endsWith(".java")) continue;
        			if (javaFile.getName().toLowerCase().startsWith("_")) continue;
        			final String fileNameToAdd = packageFolderWithCorrectSeparator + "/" + javaFile.getName(); 
        			commands.add(fileNameToAdd);
        		}
        	}
        	
        	commands.addAll(Arrays.asList(
        	  
        	"-d" , tempFolder_javadocFolder.toPath().toString(),   // where to put the output files
        	"-sourcepath ", srcFolderCore.getAbsolutePath().toString(), // do not include since information
        	"-classpath ", dependenciesJarFiles, // the dependencies are then not looked for javadoc them
        	"-use", // create class and package use classes
        	"-splitindex", // split index into one file per letter
        	"-header " , "Ne2Plan Core Documentation",
        	"-windowtitle", "\"Net2Plan - Software Development Kit (SDK) Javadoc\"" ,  // split index into one file per letter
        	"-nosince" ,// do not include since information
        	"-public" ,// only public methods
        	"-source", "1.8", // provide source compatibility with the indicated release
        	"-Xdoclint:none", // skip any error, do not check e.g. param tags are all there etc
//        	"-link" , "https://docs.oracle.com/javase/8/docs/api/",  //, "https://docs.oracle.com/en/java/javase/8/docs/api/",
        	"-link" , "https://docs.oracle.com/en/java/javase/11/docs/api",  //, "https://docs.oracle.com/en/java/javase/8/docs/api/",
        	"-taglet" , Taglet_Description_old.class.getName(),
        	"-tagletpath" , classFolderAssembler.getAbsolutePath()+ 
        			Constants.computerThisIsRunning.interDocletFolderSeparatorInJavadocCalls + classFolderCore.getAbsolutePath() + 
        			Constants.computerThisIsRunning.interDocletFolderSeparatorInJavadocCalls + dependenciesJarFiles 
//        	"-quiet" // Shuts off messages so that only the warnings and errors appear to make them easier to view. It also suppresses the version string.
//        	"-verbose" // print ifo
        	)); //at the end: the packages to be documented
        	
        	System.out.println("Javadoc command: " + commands.stream().map(e->e.trim()).collect(Collectors.joining(" ")));
        	System.out.println("dependenciesJarFiles: " + dependenciesJarFiles);
        	
        	final ProcessBuilder builder = new ProcessBuilder(commands.stream().map(e->e.trim()).collect(Collectors.toList()));
    	    builder.directory(srcFolderCore);
    	    builder.inheritIO();
    	    final Process runningProcess = builder.start();
    	    final int exitCode = runningProcess.waitFor();
    	    System.out.println("Javadoc exit with exit code: " + exitCode);
//    	      String outputOk = IOUtils.toString(runningProcess.getInputStream(), StandardCharsets.UTF_8);
//    	      String outputError = IOUtils.toString(runningProcess.getErrorStream(), StandardCharsets.UTF_8);
//    	      System.out.println("--- Output ok");
//    	      System.out.println(outputOk);
//    	      System.out.println("--- Output error");
//    	      System.out.println(outputError);
//    	      int returnCode = runningProcess.waitFor();
//            System.out.printf("Program exited with code: %d", returnCode);
          System.out.println("Exit...");

    	  	/* Create the ZIP file with all together */
    	  	ZipUtils.zipDirectory(getChild(outputGlobalTemporalDirectory , "javadoc"), assembledZipFile_javadocApi, false , acceptedSuffixesForZippingInAssembly_javadoc);
          
          	/* ZIP the javadoc */
          
    	  	/* Remove the temporal directory */
    	  	FileUtils.deleteQuietly(outputGlobalTemporalDirectory);
    	} catch (IOException e) { throw new RuntimeException (e.getMessage()); } 
    	catch (InterruptedException e1) 
    	{
    		throw new RuntimeException (e1.getMessage());
    	}
    	
    }
    private static void createZip_javadocExamples () 
    {
    	try
    	{
        	final File tempFolder_javadocFolder = getChild(outputGlobalTemporalDirectory, "javadoc");
        	FileUtils.forceMkdir(tempFolder_javadocFolder);
        	
        	final String dependenciesJarFiles = DependenciesCutAndPastes.getExternalDependencies_n2pCore().stream().map(e->e.getAbsoluteFile().toString()).collect(Collectors.joining(Constants.computerThisIsRunning.interDocletFolderSeparatorInJavadocCalls));

        	final File srcFolderCore = getChild (parentFolderOfAllModules , "n2p-core" , "src" , "main" , "java");
        	final File classFolderAssembler = getChild (parentFolderOfAllModules , "n2p-assembly" , "target" , "classes");
        	final File classFolderCore = getChild (parentFolderOfAllModules , "n2p-core" , "target" , "classes");
        	assert srcFolderCore.isDirectory();
        	final List<String> commands = new ArrayList<> ();
        	commands.add(Constants.computerThisIsRunning.pathToJavadocFile);

        	
        	/* Add all the .java files in package mtn, mtn.nodes, optionsStructure, serverInterfaces. The ones that DO NOT start with "_" */
        	final Package generalOfflinePackage = Offline_ipOverWdm_routingSpectrumAndModulationAssignmentHeuristicNotGrooming.class.getPackage();
        	final Package generalOfflineNfvPackage = Offline_nfvPlacementILP_v1.class.getPackage();
        	final Package generalOnlineSimPackage = Online_evGen_doNothing.class.getPackage();
        	final Package generalReportsPackage = Report_WDM_lineEngineering_GNModel.class.getPackage();
        	final Package ocnbookOfflinePackage = Offline_ba_numFormulations.class.getPackage();
        	final Package ocnbookOnlineSimPackage = Online_evGen_generalGenerator.class.getPackage();
        	final Package ocnbookReportsPackage = Report_availability.class.getPackage();
        	for (Package pack : Arrays.asList(generalOfflinePackage , generalOfflineNfvPackage , generalOnlineSimPackage , generalReportsPackage , ocnbookOfflinePackage , ocnbookOnlineSimPackage, ocnbookReportsPackage))
        	{
        		final String [] packageFolders = StringUtils.split(pack.getName() , ".");
        		final String packageFolderWithCorrectSeparator = Arrays.asList(packageFolders).stream().collect (Collectors.joining("/"));
        		System.out.println(packageFolderWithCorrectSeparator);
        		final File folderOfJavaFilesthisPackage = getChild(srcFolderCore, packageFolders);
        		System.out.println(folderOfJavaFilesthisPackage.toString());
        		assert folderOfJavaFilesthisPackage.isDirectory();
        		for (File javaFile : folderOfJavaFilesthisPackage.listFiles())
        		{
        			if (!javaFile.getName().toLowerCase().endsWith(".java")) continue;
        			if (javaFile.getName().toLowerCase().startsWith("_")) continue;
        			final String fileNameToAdd = packageFolderWithCorrectSeparator + "/" + javaFile.getName(); 
        			commands.add(fileNameToAdd);
        		}
        	}
        	
        	commands.addAll(Arrays.asList(
        	  
        	"-d" , tempFolder_javadocFolder.toPath().toString(),   // where to put the output files
        	"-sourcepath ", srcFolderCore.getAbsolutePath().toString(), // do not include since information
        	"-classpath ", dependenciesJarFiles, // the dependencies are then not looked for javadoc them
        	"-use", // create class and package use classes
        	"-splitindex", // split index into one file per letter
        	"-header " , "Ne2Plan Core Documentation",
        	"-windowtitle", "\"Net2Plan - Software Development Kit (SDK) Javadoc\"" ,  // split index into one file per letter
        	"-nosince" ,// do not include since information
        	"-public" ,// only public methods
        	"-source", "1.8", // provide source compatibility with the indicated release
        	"-Xdoclint:none", // skip any error, do not check e.g. param tags are all there etc
//        	"-link" , "https://docs.oracle.com/javase/8/docs/api/",  //, "https://docs.oracle.com/en/java/javase/8/docs/api/",
        	"-link" , "https://docs.oracle.com/en/java/javase/11/docs/api",  //, "https://docs.oracle.com/en/java/javase/8/docs/api/",
        	"-taglet" , Taglet_Description_old.class.getName(),
        	"-tagletpath" , classFolderAssembler.getAbsolutePath()+ 
        			Constants.computerThisIsRunning.interDocletFolderSeparatorInJavadocCalls + classFolderCore.getAbsolutePath() + 
        			Constants.computerThisIsRunning.interDocletFolderSeparatorInJavadocCalls + dependenciesJarFiles 
//        	"-quiet" // Shuts off messages so that only the warnings and errors appear to make them easier to view. It also suppresses the version string.
//        	"-verbose" // print ifo
        	)); //at the end: the packages to be documented
        	
        	System.out.println("Javadoc command: " + commands.stream().map(e->e.trim()).collect(Collectors.joining(" ")));
        	System.out.println("dependenciesJarFiles: " + dependenciesJarFiles);
        	
        	final ProcessBuilder builder = new ProcessBuilder(commands.stream().map(e->e.trim()).collect(Collectors.toList()));
    	    builder.directory(srcFolderCore);
    	    builder.inheritIO();
    	    final Process runningProcess = builder.start();
    	    final int exitCode = runningProcess.waitFor();
    	    System.out.println("Javadoc exit with exit code: " + exitCode);
//    	      String outputOk = IOUtils.toString(runningProcess.getInputStream(), StandardCharsets.UTF_8);
//    	      String outputError = IOUtils.toString(runningProcess.getErrorStream(), StandardCharsets.UTF_8);
//    	      System.out.println("--- Output ok");
//    	      System.out.println(outputOk);
//    	      System.out.println("--- Output error");
//    	      System.out.println(outputError);
//    	      int returnCode = runningProcess.waitFor();
//            System.out.printf("Program exited with code: %d", returnCode);
          System.out.println("Exit...");

    	  	/* Create the ZIP file with all together */
    	  	ZipUtils.zipDirectory(getChild(outputGlobalTemporalDirectory , "javadoc"), assembledZipFile_javadocExamples, false , acceptedSuffixesForZippingInAssembly_javadoc);
          
          	/* ZIP the javadoc */
          
    	  	/* Remove the temporal directory */
    	  	FileUtils.deleteQuietly(outputGlobalTemporalDirectory);
    	} catch (IOException e) { throw new RuntimeException (e.getMessage()); } 
    	catch (InterruptedException e1) 
    	{
    		throw new RuntimeException (e1.getMessage());
    	}
    	
    }
}


