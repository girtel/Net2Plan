package com.net2plan.documentation;



import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;

import com.net2plan.constants.Constants;
import com.net2plan.gui.GUINet2Plan;
import com.net2plan.utils.ZipUtils;



/**
 */
public class AssemblerCodeAndDocumentationMain 
{

	private static List<String> log = new ArrayList<> ();
	public static void log (String s) { log.add(s); }
	
	/* Configuration variables */
	public final static String versionOfJars = "0.6.6.0"; // Change for a varible
	public final static File outputAssemblyDirectory = Constants.computerThisIsRunning.localFileForAssembly; // better if outside the GIT domain
	
	/* Control [non-configurable] variables */
	
	private final static Optional<SortedSet<String>> acceptedSuffixesForZippingInAssembly_n2pCore = Optional.of (new TreeSet<> (Arrays.asList(".class" , ".jar", ".txt" , ".MF" , ".properties" , ".css.map" , ".css" , ".js.map" , ".js", ".json"))); 
	private final static Optional<SortedSet<String>> acceptedSuffixesForZippingInAssembly_n2pGui = Optional.of (new TreeSet<> (CollectionUtils.union(acceptedSuffixesForZippingInAssembly_n2pCore.get() , Arrays.asList(".js" , "js.map" , ".png", ".ico", ".icns" , ".gif" , ".bmp" , ".jpg" , ".css", ".json")))); 
	private final static Optional<SortedSet<String>> acceptedSuffixesForZippingInAssembly_javadoc = Optional.empty (); 
	private static final File currentSystemDirectory = new File (System.getProperty("user.dir")); 
	private static final File outputGlobalDirectory_zipFiles = new File (outputAssemblyDirectory , "compressedFiles"); 
	private static final File outputGlobalDirectory_unzipped = new File (outputAssemblyDirectory , "unzipped"); 
	public static final File outputGlobalTemporalDirectory = new File (outputAssemblyDirectory , "temporalFolderShouldBeRemoved"); 
	public static final File parentFolderOfAllModules = currentSystemDirectory.getAbsoluteFile().getParentFile();
	private static final File assembledZipFile_core = new File (outputGlobalDirectory_zipFiles , "assembly-n2p-core-" + versionOfJars + ".zip");
	private static final File assembledZipFile_gui = new File (outputGlobalDirectory_zipFiles , "assembly-n2p-gui-" + versionOfJars + ".zip");
	private static final File assembledZipFile_javadoc = new File (outputGlobalDirectory_zipFiles , "assembly-javadoc-" + versionOfJars + ".zip");
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
//	private static void assembleFileLaunch (Scanner in , List<File> ff , Runnable fileCreator)
//	{
//		final String fileNames = ff.stream().map(f->f.getName()).collect(Collectors.joining(",")); 
//		System.out.print("Assembling the file/s: " + fileNames);
//    	if (ff.stream().allMatch(f->f.isFile()))
//    	{
//    		final boolean create = AwsUtils.getYesNo(in, false, "All the zip files " + fileNames + " already exist. Create again (all of them)?");
//    		if (create) fileCreator.run();;
//    	}
//    	else 
//    	{
//    		System.out.println(" At least one of the files do not exist. Creating it.");
//    		fileCreator.run();
//    	}
//	}
	
	public static void main( String[] args ) throws Throwable
    {
		/* Checks */
//		ConfigurationData.checkConsistency();
		
    	System.out.println("System directory: " + currentSystemDirectory);
    	System.out.println("Output of the assembly elements in directory: " + outputGlobalDirectory_zipFiles);
    	if (!currentSystemDirectory.getName().equals("n2p-assembly")) throw new RuntimeException("The current directoy should be the n-assembly directory");
		final Scanner in = new Scanner(System.in);
    	
    	/* Initialize the output assembly directory */
    	resetDirectory(outputGlobalDirectory_unzipped);
		resetDirectory(outputAssemblyDirectory);
    	resetDirectory(outputGlobalDirectory_zipFiles);
    	FileUtils.forceMkdir(getChild(outputGlobalDirectory_unzipped, "gui"));
    	
    	assembleFileLaunch (in , assembledZipFile_core , ()-> createZip_n2pCore () );
    	assembleFileLaunch (in , assembledZipFile_gui , ()-> createZip_n2pGuiApplication ());
    	assembleFileLaunch (in , assembledZipFile_javadoc , ()-> createZip_javadoc () );
    	
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
        	final String readString = 
        			"This ZIP file includes the Graphical User Interface (GUI) of the Net2Plan. "
        			+ "The /lib directory contains the dependencies of the application.";
        	FileUtils.writeStringToFile(new File (outputGlobalTemporalDirectory , "README.txt"), readString, "UTF-8");

        	/* Copy dependencies to lib folder: external and n2p-core  */
        	final List<File> n2pCoreExternalDependencies = DependenciesCutAndPastes.getExternalDependencies_n2pCore();
        	final File directory_lib = getChild(outputGlobalTemporalDirectory , "lib");
        	FileUtils.forceMkdir(directory_lib);
        	for (File f : n2pCoreExternalDependencies)
            	FileUtils.copyFileToDirectory (f, directory_lib);
        	/* Copy the n2p-core-jar file, with a modified MANIFEST */
            createAndSaveModifiedVersionOfN2pCoreJar (getChild (outputGlobalTemporalDirectory , "lib" , "n2p-core-" + versionOfJars + ".jar"));

        	/* Create the /data folder */
        	final File directory_data = getChild(outputGlobalTemporalDirectory , "data");
        	FileUtils.forceMkdir(directory_data);

        	/* Write the modified version of the GUI (with new classpath) */
        	createAndSaveModifiedVersionOfGuiJar (getChild(outputGlobalTemporalDirectory));
        	
        	/* Create the ZIP file with all together */
        	ZipUtils.zipDirectory(outputGlobalTemporalDirectory, assembledZipFile_gui, false , acceptedSuffixesForZippingInAssembly_n2pGui);
        	
        	/* Remove the temporal directory */
        	FileUtils.deleteQuietly(outputGlobalTemporalDirectory);
    	} catch (IOException e) { throw new RuntimeException (e.getMessage()); }
    }

    private static void createAndSaveModifiedVersionOfN2pCoreJar (File n2pCoreJarFileCopyInOutputFolder) throws IOException
    {
    	final File inputDirectoryOfSubmodule = getChild(parentFolderOfAllModules , "n2p");
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
    	final File jarFileN2pGui = getChild(inputDirectoryOfSubmodule , "target" , "n2p-gui-" + versionOfJars + ".jar");
    	final File rootFolderOfClasses = getChild(inputDirectoryOfSubmodule , "target" , "classes");

    	/* Create the JAR */
    	final List<File> n2pGuiDependencies = DependenciesCutAndPastes.getExternalDependencies_n2pGui();
    	n2pGuiDependencies.add(new File ("n2p-core-" + versionOfJars + ".jar"));
    	final List<String> jarFilesInClassPathNames = n2pGuiDependencies.stream().map(e->"lib/" + e.getName().trim()).collect(Collectors.toList());
        final Manifest manifest = ManifestManipulator.getEnpGeneralManifest("n2p-gui", versionOfJars, jarFilesInClassPathNames , Optional.of(GUINet2Plan.class.getName()) , Optional.empty());
    	try (JarOutputStream target = new JarOutputStream(new FileOutputStream(jarFileN2pGui), manifest))
    	{
        	JarUtils.add (rootFolderOfClasses , rootFolderOfClasses , target);
    		target.close();
    	}

    	if (!jarFileN2pGui.isFile()) throw new RuntimeException("File not found: " + jarFileN2pGui.getAbsolutePath());
    	FileUtils.copyFile(jarFileN2pGui, n2pGuiJarFileCopyInOutputFolder);

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
    private static void createZip_javadoc () 
    {
    	try
    	{
        	final File tempFolder_javadocFolder = getChild(outputGlobalTemporalDirectory, "javadoc");
        	FileUtils.forceMkdir(tempFolder_javadocFolder);
        	
        	final String dependenciesJarFiles = DependenciesCutAndPastes.getExternalDependencies_n2pCore().stream().map(e->e.getAbsoluteFile().toString()).collect(Collectors.joining(Constants.computerThisIsRunning.interDocletFolderSeparatorInJavadocCalls));

        	final File srcFolderCore = getChild (parentFolderOfAllModules , "n2p" , "src" , "main" , "java");
        	final File classFolderAssembler = getChild (parentFolderOfAllModules , "n2p-assembly" , "target" , "classes");
        	final File classFolderCore = getChild (parentFolderOfAllModules , "n2p" , "target" , "classes");
        	assert srcFolderCore.isDirectory();
        	final List<String> commands = new ArrayList<> ();
        	commands.add(Constants.computerThisIsRunning.pathToJavadocFile);

        	
        	/* Add all the .java files in package mtn, mtn.nodes, optionsStructure, serverInterfaces. The ones that DO NOT start with "_" */
//        	final Package mtnPackage = Mtn.class.getPackage();
//        	final Package mtnPubUtilsPackage = Pair.class.getPackage();
//        	final Package mtnNodesPackage = ZGenericAbstractNode.class.getPackage();
//        	final Package serverInterfacesPackage = IEnpServerInterface.class.getPackage();
//        	final Package optionsStructurePackage = INavigableDocumentedObject.class.getPackage();
//        	final Package enumsPackage = OtuRecoveryType.class.getPackage();
//        	for (Package pack : Arrays.asList(mtnPackage , mtnNodesPackage , serverInterfacesPackage , optionsStructurePackage , enumsPackage , mtnPubUtilsPackage))
//        	{
//        		final String [] packageFolders = StringUtils.split(pack.getName() , ".");
//        		final String packageFolderWithCorrectSeparator = Arrays.asList(packageFolders).stream().collect (Collectors.joining("/"));
//        		final File folderOfJavaFilesthisPackage = getChild(srcFolderCore, packageFolders);
//        		assert folderOfJavaFilesthisPackage.isDirectory();
//        		for (File javaFile : folderOfJavaFilesthisPackage.listFiles())
//        		{
//        			if (!javaFile.getName().toLowerCase().endsWith(".java")) continue;
//        			if (javaFile.getName().toLowerCase().startsWith("_")) continue;
//        			final String fileNameToAdd = packageFolderWithCorrectSeparator + "/" + javaFile.getName(); 
//        			commands.add(fileNameToAdd);
//        		}
//        	}
        	
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
        	"-taglet" , Taglet_Description.class.getName(),
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
    	  	ZipUtils.zipDirectory(getChild(outputGlobalTemporalDirectory , "javadoc"), assembledZipFile_javadoc, false , acceptedSuffixesForZippingInAssembly_javadoc);
          
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


