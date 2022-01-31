package com.net2plan.documentation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;


public class ManifestManipulator 
{

    public static void removeFilesWithUnwantedsuffixesAndEmptyFolders (SortedSet<String> unwantedSuffixes) throws IOException 
    {
    	
    }
	
    public static Manifest getEnpGeneralManifest (String jarNameWithoutVersion , String implementationVersion , List<String> relativePathsOfJarFilesInClasspathOfManifest , Optional<String> mainClass , Optional<String> moduleName) 
    {
        Manifest manifest = new Manifest();
        Attributes mainAttributes = manifest.getMainAttributes();
        
        mainAttributes.clear();
        mainAttributes.putValue(Attributes.Name.MANIFEST_VERSION.toString(), "1.0");
        mainAttributes.putValue(Attributes.Name.IMPLEMENTATION_TITLE.toString(), jarNameWithoutVersion);
        mainAttributes.putValue(Attributes.Name.IMPLEMENTATION_VERSION.toString(), implementationVersion);
        mainAttributes.putValue(Attributes.Name.IMPLEMENTATION_VENDOR.toString(), "Net2Plan");
        mainAttributes.putValue(Attributes.Name.SPECIFICATION_TITLE.toString(), jarNameWithoutVersion);
        mainAttributes.putValue(Attributes.Name.SPECIFICATION_VERSION.toString(), implementationVersion);
        mainAttributes.putValue(Attributes.Name.SPECIFICATION_VENDOR.toString(), "Net2Plan");
        if (mainClass.isPresent())
        	mainAttributes.putValue(Attributes.Name.MAIN_CLASS.toString(), mainClass.get());
        if (moduleName.isPresent())
        	mainAttributes.putValue("Automatic-Module-Name", moduleName.get());
        
        final String classPathString = relativePathsOfJarFilesInClasspathOfManifest.stream().map(e->e.trim()).collect(Collectors.joining(" "));
        mainAttributes.putValue(Attributes.Name.CLASS_PATH.toString(), classPathString);
        return manifest;
//        writeManifest(manifestPath, manifest);
    }
    
    
    public static void setNewManifestFileInJar (File jarFile , String jarNameWithoutVersion , String implementationVersion , List<String> relativePathsOfJarFilesInClasspathOfManifest) throws IOException 
    {
        try (FileSystem jarFS = FileSystems.newFileSystem(URI.create("jar:" + jarFile.toPath().toUri()), new HashMap<>())) 
        {
            Path manifestPath = jarFS.getPath("META-INF", "MANIFEST.MF");
            Manifest manifest = readManifest(manifestPath);
            Attributes mainAttributes = manifest.getMainAttributes();
            //System.out.println("Found main attribute names: " + mainAttributes.keySet());
            
            mainAttributes.clear();
            mainAttributes.putValue(Attributes.Name.MANIFEST_VERSION.toString(), "1.0");
            mainAttributes.putValue(Attributes.Name.IMPLEMENTATION_TITLE.toString(), jarNameWithoutVersion);
            mainAttributes.putValue(Attributes.Name.IMPLEMENTATION_VERSION.toString(), implementationVersion);
            mainAttributes.putValue(Attributes.Name.IMPLEMENTATION_VENDOR.toString(), "Net2Plan");
            mainAttributes.putValue(Attributes.Name.SPECIFICATION_TITLE.toString(), jarNameWithoutVersion);
            mainAttributes.putValue(Attributes.Name.SPECIFICATION_VERSION.toString(), implementationVersion);
            mainAttributes.putValue(Attributes.Name.SPECIFICATION_VENDOR.toString(), "Ne2Plan");
            
            final String classPathString = relativePathsOfJarFilesInClasspathOfManifest.stream().map(e->e.trim()).collect(Collectors.joining(""));
            mainAttributes.putValue(Attributes.Name.CLASS_PATH.toString(), classPathString);
            writeManifest(manifestPath, manifest);
        }
    }
    
    public static void setNewManifestFileInJar_2 (File jarFile , String jarNameWithoutVersion , String implementationVersion , List<String> relativePathsOfJarFilesInClasspathOfManifest , Optional<String> mainClass , Optional<String> moduleName) throws IOException 
    {
    	final Path zipFilePath = jarFile.toPath();
    	try (FileSystem zipFileSystem = FileSystems.newFileSystem(zipFilePath, (ClassLoader) null)) 
    	{
            Path manifestFile = zipFileSystem.getPath("META-INF/MANIFEST.MF");
            final Manifest newManifest = getEnpGeneralManifest(jarNameWithoutVersion, implementationVersion, relativePathsOfJarFilesInClasspathOfManifest , mainClass , moduleName);
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            newManifest.write(out);
            String newManifestContent = IOUtils.toString(new ByteArrayInputStream(out.toByteArray()), StandardCharsets.UTF_8); 
            // Replace MANIFEST.MF content.
            Files.write(manifestFile, newManifestContent.getBytes(StandardCharsets.UTF_8), StandardOpenOption.TRUNCATE_EXISTING);
        }
    }
    
    private static Manifest readManifest(Path manifestPath) throws IOException 
    {
        try (InputStream is = Files.newInputStream(manifestPath)) {
            return new Manifest(is);
        }
    }

    private static void writeManifest(Path manifestPath, Manifest manifest) throws IOException 
    {
        try (OutputStream os = Files.newOutputStream(manifestPath)) 
        {
            manifest.write(os);
        }
    }

}