package com.net2plan.codeAutomation;

import java.io.File;
import java.util.Optional;

import org.apache.commons.io.FileUtils;

import com.elighthouse.deploymentCases.telefonicaVivo.ConfigurationData;
import com.elighthouse.sdk.internal.constants.DeploymentRestrictionLevel;
import com.elighthouse.sdk.internal.constants._CodeAutomation_ReleaseConstants;
import com.elighthouse.sdk.utils.StringUtils;

public class JavaCodeGenerationPhase_SdkReleaseConstants 
{
	private static final String RET = System.getProperty("line.separator");
	private static final File currentSystemDirectory = new File (System.getProperty("user.dir")); 
	static final File parentFolderOfAllModules = currentSystemDirectory.getAbsoluteFile().getParentFile();
	private static final File rootfFolderOfSdkSource = getChild(parentFolderOfAllModules , "eli" , "src" , "main" , "java");
	private static File getSourceFileInSdk (Class<?> cl)  throws Throwable
	{
		final Package pack = cl.getPackage();
		final String [] packageFolders = StringUtils.split(pack.getName() , ".");
		final File folderOfJavaFilesthisPackage = getChild(rootfFolderOfSdkSource, packageFolders);
		final File res = getChild (folderOfJavaFilesthisPackage, cl.getSimpleName() + ".java");
		return res;
	}
	private static Optional<String> getSourceTextInSdk (Class<?> cl) throws Throwable
	{
		final File f = getSourceFileInSdk (cl);
		if (!f.isFile()) return Optional.empty();
		final String res = FileUtils.readFileToString(f , "UTF-8");
		return Optional.of (res);
	}
	
	public static void automaticallyGenerateFileAndHaltSystemIfNeeded () 
	{
		try
		{
			final Class<?> cl = _CodeAutomation_ReleaseConstants.class;
			final File f = getSourceFileInSdk (cl);
			if (!f.isFile()) throw new RuntimeException ("File " + f.getAbsolutePath() + " for class " + cl + " does not exist");

			final String contentShouldBe = getUpdatedFileContentToSave();
			final String currentContent = getSourceTextInSdk(cl).orElse(null);
			if (!contentShouldBe.equals(currentContent))
			{
				FileUtils.writeStringToFile(f , contentShouldBe , "UTF-8");
				System.out.println("IMPORTANT!!! SCRIPT HALTED. The content of " + _CodeAutomation_ReleaseConstants.class.getSimpleName() + " has changed. PLEASE RECOMPILE ALL IN ECLIPSE AND RUN ASSEMBLER AGAIN");
				System.exit(-1);
			}
		} catch (Throwable e)
		{
			System.out.println("Error checking the automatic creation of " + _CodeAutomation_ReleaseConstants.class.getSimpleName() + ". SYSTEM HALTED... SOLVE IT!!");
			System.exit(-1);
		}
	}
	
	private static String getUpdatedFileContentToSave () 
	{
		  final StringBuffer st = new StringBuffer ();
		  
		  st.append ("/* CLASS GENERATED AUTOMATICALLY BY SCRIPT " + JavaCodeGenerationPhase_SdkReleaseConstants.class.getSimpleName () + ". DO NOT MODIFY MANUALLY */" + RET);
		  st.append ("/* CLASS GENERATED AUTOMATICALLY BY SCRIPT " + JavaCodeGenerationPhase_SdkReleaseConstants.class.getSimpleName () + ". DO NOT MODIFY MANUALLY */" + RET);
		  st.append ("/* CLASS GENERATED AUTOMATICALLY BY SCRIPT " + JavaCodeGenerationPhase_SdkReleaseConstants.class.getSimpleName () + ". DO NOT MODIFY MANUALLY */" + RET + RET + RET);
		  st.append ("package com.elighthouse.sdk.internal.constants;" + RET);
		  st.append ("public class " + _CodeAutomation_ReleaseConstants.class.getSimpleName() + RET);
		  st.append ("{" + RET);
		  st.append ("  public static final String softwareVersion = \"" + ConfigurationData.ReleaseCostants_softwareVersion + "\";" + RET);
		  st.append ("  public static final String globalResourcesUrlInElighthouseWebNoSlashAtTheEnd = \"https://www.e-lighthouse.com/globalResources\";" + RET);
		  st.append ("  public static final " + (DeploymentRestrictionLevel.class.getSimpleName ()) + " deploymentRestrictionLevel = " + (DeploymentRestrictionLevel.class.getSimpleName ()) + "." + ConfigurationData.deploymentRestrictionLevel.name () + ";" + RET);
		  st.append ("}" + RET);
		  return st.toString();
	}
	
  public static void main (String [] args) throws Throwable
  {
	  automaticallyGenerateFileAndHaltSystemIfNeeded();
    }

  private static File getChild (File f , String... subdirs)
  {
  	if (subdirs.length == 0) return f;
  	if (subdirs.length == 1) return new File (f , subdirs [0]);
  	final String [] subdirsButFirst = new String [subdirs.length - 1];
  	for (int c = 1; c < subdirs.length ; c ++) subdirsButFirst [c-1] = subdirs [c];
  	return getChild (new File (f , subdirs [0]) , subdirsButFirst);
  }

}
