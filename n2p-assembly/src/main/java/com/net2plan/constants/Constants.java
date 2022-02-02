package com.net2plan.constants;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Constants 
{
	public static final ComputerThisIsRunning computerThisIsRunning = ComputerThisIsRunning.JoseManuelLinux;
	public static final List<String> platformDependentNamesOfJarFiles = Arrays.asList (
				"org.openjfx.javafx-fxml",
				"org.openjfx.javafx-base",
				"org.openjfx.javafx-swing",
				"org.openjfx.javafx-web"
				);
	
	
	public enum ComputerThisIsRunning 
	{ 
		PabloHome (new File ("c:\\n2pAssembly"), 
				true,
				"C:\\Program Files\\Java\\jdk-16.0.1\\bin\\javadoc" , 
				new File("C:\\apache-maven-3.8.4") , 
				new File ("C:\\Users\\pablo\\.m2\\repository") , 
				";"
				) , 
		PabloWork(new File ("c:\\n2pAssembly"), 
				true,
				"C:\\Program Files\\Java\\jdk-16.0.1\\bin\\javadoc" ,
				new File("C:\\apache-maven-3.6.3"),
				new File ("C:\\Users\\pablo\\.m2\\repository"),
				";"
				) , 
		JoseManuelLinux (new File ("/home/jmmartinez/n2pAssembly"), 
				false,
				"/usr/lib/jvm/jdk-17/bin/javadoc",
				new File("/opt/maven"),
				new File ("/home/jmmartinez/.m2/repository"),
				":"
				),
	      JoseManuelWin(new File ("c:\\n2pAssembly"),
				true, 
	            "C:\\Program Files\\Java\\jdk-16.0.1\\bin\\javadoc" ,
	            new File("C:\\apache-maven-3.8.4"),
	            new File ("C:\\Users\\JoseM\\.m2\\repository"),
	            ";"
	            ); 
		
		private ComputerThisIsRunning (File localFileForAssembly, boolean isTheLocalComputerRunningWindows, String pathToJavadocFile , File folderOfMavenInstallation , 
				File folderOfMavenRepository , String interDocletFolderSeparatorInJavadocCalls) 
		{
			this.localFileForAssembly = localFileForAssembly;
			this.isTheLocalComputerRunningWindows = isTheLocalComputerRunningWindows;
			this.pathToJavadocFile = pathToJavadocFile;
			this.folderOfMavenInstallation = folderOfMavenInstallation;
			this.folderOfMavenRepository = folderOfMavenRepository;
			this.interDocletFolderSeparatorInJavadocCalls = interDocletFolderSeparatorInJavadocCalls;
		}
		
		public final File localFileForAssembly;
		public final boolean isTheLocalComputerRunningWindows;
		public final String pathToJavadocFile;
		public final File folderOfMavenInstallation;
		public final File folderOfMavenRepository;
		public final String interDocletFolderSeparatorInJavadocCalls;
		
	};
	
}
