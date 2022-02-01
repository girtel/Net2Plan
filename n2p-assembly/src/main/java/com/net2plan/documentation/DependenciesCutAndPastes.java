package com.net2plan.documentation;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.input.CharSequenceReader;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.SystemOutHandler;

import com.net2plan.constants.Constants;
import com.net2plan.utils.Wget;
import com.net2plan.utils.Wget.WgetStatus;

public class DependenciesCutAndPastes 
{
	private final static Map<File , List<File>> cache_dependenciesFromPom = new HashMap<> ();	
	public static List<File> getExternalDependencies_n2pCore ()  { return getDependenciesOfMavenModuleAllFilesExistOrError(AssemblerCodeAndDocumentationMain.pomFile_n2pCore);  }

	public static List<File> getExternalDependencies_n2pGui () 
	{ 
		final List<File> depsIncludingN2pCore = getDependenciesOfMavenModuleAllFilesExistOrError(AssemblerCodeAndDocumentationMain.pomFile_n2pCore);
		final List<File> depsIncludingN2pGui = getDependenciesOfMavenModuleAllFilesExistOrError(AssemblerCodeAndDocumentationMain.pomFile_n2pGui);
		for (File f: depsIncludingN2pCore) if (!depsIncludingN2pGui.contains(f)) depsIncludingN2pGui.add(f);
				
		final File temporaryDirectory = new File (System.getProperty("java.io.tmpdir"));
		if (!temporaryDirectory.exists()) throw new RuntimeException("Temporary directory not found");
		
		for (File f : new ArrayList<> (depsIncludingN2pGui))
		{
			final String name = f.getName().substring(0, f.getName().length());
			final String versionName = f.getParentFile().getName();
			final String arhtypeName3 = f.getParentFile().getParentFile().getName();
			final String arhtypeName2 = f.getParentFile().getParentFile().getParentFile().getName();
			final String arhtypeName1 = f.getParentFile().getParentFile().getParentFile().getParentFile().getName();
			final String arhtypeName0 = f.getParentFile().getParentFile().getParentFile().getParentFile().getParentFile().getName();
			String 	url = "https://repo1.maven.org/maven2/" + arhtypeName0 + "/" + arhtypeName1 + "/" + arhtypeName2 + "/" + arhtypeName3 + "/" + versionName + "/" + name;
			
			if (arhtypeName0.equals("com") && arhtypeName1.equals("github"))
			{
				url = "https://jitpack.io/" + arhtypeName0 + "/" + arhtypeName1 + "/" + arhtypeName2 + "/" + arhtypeName3 + "/" + versionName + "/" + name;

			}
			if (arhtypeName0.equals("repository"))
			{
				url = "https://repo1.maven.org/maven2/" + arhtypeName1 + "/" + arhtypeName2 + "/" + arhtypeName3 + "/" + versionName + "/" + name;
			}
			if (arhtypeName1.equals("repository"))
			{
				url = "https://repo1.maven.org/maven2/" + arhtypeName2 + "/" + arhtypeName3 + "/" + versionName + "/" + name;
			}
		
			System.out.println("URL: "+url);
			final File outputFileTemporal = new File (temporaryDirectory , name);
			depsIncludingN2pGui.add(outputFileTemporal);
			if (!outputFileTemporal.isFile())
			{
				try
				{
					/* file not exists also in temporary folder */
					System.out.println("Downloading into the temporary folder: " + outputFileTemporal);
					final WgetStatus res = Wget.wGet(url , outputFileTemporal);
					if (res != WgetStatus.Success && (arhtypeName0.equals("com") && arhtypeName1.equals("github")))
					{
						url = "https://jitpack.io/" + arhtypeName0 + "/" + arhtypeName1 + "/" + arhtypeName2 + "/" +arhtypeName3 + "/" + arhtypeName3 + "-" + versionName + "/" + arhtypeName3 + "-" + name;
						System.out.println("New url: " + url);
						try
						{
							final WgetStatus res2 = Wget.wGet(url , outputFileTemporal);
							if (res2 != WgetStatus.Success) throw new RuntimeException ("Error accessing the url: " + url + ". Res: " + res2);
						}catch (Exception e) { e.printStackTrace(); throw new RuntimeException ("Error accessing the file"); }
					}
				} catch (Exception e) { e.printStackTrace(); throw new RuntimeException ("Error accessing the file"); }
			}
			else System.out.println("Already found in the temporary folder: " + outputFileTemporal);
				
			if (!outputFileTemporal.isFile()) continue;
		}	
			
			final List<File> filesWithoutCoreJars = new ArrayList<> ();
//			final String myClassfierEnd = ".jar";
			for (File f : depsIncludingN2pCore)
			{
				assert f.exists();
				if (f.getName().contains("n2p-core")) continue;
				filesWithoutCoreJars.add(f);
			}
			return filesWithoutCoreJars;		
	}

	public static void main (String[] args) throws Throwable
	{
	}

    private static List<File> getDependenciesOfMavenModuleAllFilesExistOrError (File pomFile) 
    {
    	if (cache_dependenciesFromPom.containsKey(pomFile)) return cache_dependenciesFromPom.get(pomFile);
    	try
    	{
	    	final String dependenciesfileName = "dependencies.txt";
	    	final File dependenciesFile = new File (pomFile.getParentFile() , dependenciesfileName);
	    	
	    	final InvocationRequest request = new DefaultInvocationRequest();
	    	request.setPomFile(pomFile);
	    	request.setGoals(Arrays.asList("dependency:list"));
	    	final Properties properties = new Properties();
	    	properties.setProperty("outputFile", dependenciesfileName); // redirect output to a file
	    	properties.setProperty("outputAbsoluteArtifactFilename", "true"); // with paths
	    	properties.setProperty("includeScope", "runtime"); // only runtime (scope compile + runtime). if only interested in scope runtime, you may replace with excludeScope = compile
	    	properties.setProperty("appendOutput", "true"); // in multi module, if not the last module overwrites
	    	
	    	request.setProperties(properties);
//		    	request.setLocalRepositoryDirectory(mavenRepositoryUsedInEclipse);
	    	
	    	final Invoker invoker = new DefaultInvoker();
	    	// the Maven home can be omitted if the "maven.home" system property is set
	    	invoker.setMavenHome(Constants.computerThisIsRunning.folderOfMavenInstallation);
	    	//invoker.setOutputHandler(null); // not interested in Maven output itself
	    	invoker.setOutputHandler(new SystemOutHandler(true)); // null = not interested in Maven output itself
	    	
	    	final InvocationResult result = invoker.execute(request);
	    	final int exitCode = result.getExitCode();
	    	if (exitCode != 0) 
	    	    throw new IllegalStateException("Build failed. Exit code: " + exitCode);
	    	
	    	final List<File> dependencyFiles = new ArrayList<> ();
	    	Pattern pattern = Pattern.compile("(?:compile|runtime):(.*)");
	    	
	    	try (BufferedReader reader = Files.newBufferedReader(dependenciesFile.toPath())) 
	    	{
	    	    while (!"The following files have been resolved:".equals(reader.readLine()));
	    	    String line;
	    	    while ((line = reader.readLine()) != null && !line.isEmpty()) 
	    	    {
	    	        Matcher matcher = pattern.matcher(line);
	    	        if (matcher.find()) 
	    	        {
	    	            // group 1 contains the path to the file
	    	            //System.out.println(matcher.group(1));
	    	            final File f = new File (matcher.group(1));
	    	            if (!f.isFile()) throw new RuntimeException("The dependency file " + f + " should exist but it does not");
	    	            dependencyFiles.add(f);
	    	        }
	    	    }
	    	}
	    	FileUtils.deleteQuietly(dependenciesFile);
	    	if (dependenciesFile.exists()) throw new RuntimeException ("Could not delete de file: " + dependenciesFile);
	    	
			final List<File> resSorted = dependencyFiles.stream().sorted((f1,f2)->f1.getAbsolutePath().compareTo(f2.getAbsolutePath())).collect(Collectors.toCollection(ArrayList::new));
			
	    	cache_dependenciesFromPom.put(pomFile , resSorted);
			return resSorted;
    	} catch (Throwable e) 
    	{
    		e.printStackTrace();
    		throw new RuntimeException(e.getMessage());
    	}
    }

		
}

