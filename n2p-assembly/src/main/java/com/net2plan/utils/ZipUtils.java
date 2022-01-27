package com.net2plan.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.SortedSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;


public class ZipUtils 
{
    public static void zipDirectory (File directoryToZip , File outputZipFile , boolean includeDirectoryName , Optional<SortedSet<String>> acceptableFileSuffixes) throws IOException 
    {
        FileOutputStream fos = new FileOutputStream(outputZipFile);
        ZipOutputStream zipOut = new ZipOutputStream(fos);
        zipFile(directoryToZip, includeDirectoryName? directoryToZip.getName() : "", zipOut , acceptableFileSuffixes);
        zipOut.close();
        fos.close();
    }

    private static void zipFile(File fileToZip, String fileName, ZipOutputStream zipOut , Optional<SortedSet<String>> acceptableFileSuffixes) throws IOException 
    {
        if (fileToZip.isHidden()) return;
        if (fileToZip.isDirectory()) 
        {
            if (fileName.endsWith("/")) 
            {
                zipOut.putNextEntry(new ZipEntry(fileName));
                zipOut.closeEntry();
            } else 
            {
                zipOut.putNextEntry(new ZipEntry(fileName + "/"));
                zipOut.closeEntry();
            }
            File[] children = fileToZip.listFiles();
            for (File childFile : children) 
            {
            	if (fileName.isEmpty())
            		zipFile(childFile, childFile.getName(), zipOut , acceptableFileSuffixes);
            	else
            		zipFile(childFile, fileName + "/" + childFile.getName(), zipOut , acceptableFileSuffixes);
            }
            return;
        }
        
        if (acceptableFileSuffixes.isPresent()) if (!acceptableFileSuffixes.get().stream().anyMatch(e->fileToZip.getName().endsWith(e))) throw new RuntimeException("Forbidden extension of file in the ZIP: " + fileToZip.getName());

        FileInputStream fis = new FileInputStream(fileToZip);
        ZipEntry zipEntry = new ZipEntry(fileName);
        zipOut.putNextEntry(zipEntry);
        byte[] bytes = new byte[4096];
        int length;
        while ((length = fis.read(bytes)) >= 0) {
            zipOut.write(bytes, 0, length);
        }
        fis.close();
    }
    
    public static void checkAcceptableFileSuffixes (File fileZip , Optional<SortedSet<String>> acceptableSuffixes) 
    {
    	if (!acceptableSuffixes.isPresent()) return;
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(fileZip)))
        {
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) 
            {
        	     if (zipEntry.isDirectory()) { zipEntry = zis.getNextEntry(); continue; }
        	     boolean ok = false;
        	     for (String s : acceptableSuffixes.get()) if (zipEntry.getName().endsWith(s)) { ok = true; break; }
        	     if (!ok) throw new RuntimeException("Not accepted suffix of file : " + zipEntry.getName() + ", in ZIP file " + fileZip);
        	     zipEntry = zis.getNextEntry();
            }
            zis.closeEntry();
            zis.close();
        }
        catch (Exception e)
        {
        	throw new RuntimeException (e.getMessage());
        }
    	
    }

    public static void unzip (File fileZip , File destDir) throws IOException
    {
        final byte[] buffer = new byte[4096];
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(fileZip)))
        {
            ZipEntry zipEntry = zis.getNextEntry();
        	while (zipEntry != null) {
        	     File newFile = newFile(destDir, zipEntry);
        	     if (zipEntry.isDirectory()) {
        	         if (!newFile.isDirectory() && !newFile.mkdirs()) {
        	             throw new IOException("Failed to create directory " + newFile);
        	         }
        	     } else {
        	         // fix for Windows-created archives
        	         File parent = newFile.getParentFile();
        	         if (!parent.isDirectory() && !parent.mkdirs()) {
        	             throw new IOException("Failed to create directory " + parent);
        	         }
        	         
        	         // write file content
        	         FileOutputStream fos = new FileOutputStream(newFile);
        	         int len;
        	         while ((len = zis.read(buffer)) > 0) {
        	             fos.write(buffer, 0, len);
        	         }
        	         fos.close();
        	     }
        	 zipEntry = zis.getNextEntry();
        	}
	        zis.closeEntry();
	        zis.close();
        }
    }

    private static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException 
    {
        File destFile = new File(destinationDir, zipEntry.getName());
        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();
        if (!destFilePath.startsWith(destDirPath /* + File.separator*/)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }
        return destFile;
    }
}
