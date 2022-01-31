package com.net2plan.documentation;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

public class JarUtils 
{
	public static void add(File sourceFolderOrFile, File mainFolderOfClassesForCreateRelativePaths, JarOutputStream target) throws IOException 
	{
		final Path sourceFolderOrFilePath = Paths.get(sourceFolderOrFile.getAbsolutePath());
		final Path mainFolderOfClassesForCreateRelativePaths_path = Paths.get(mainFolderOfClassesForCreateRelativePaths.getAbsolutePath());

		BufferedInputStream in = null;
		try 
		{
			String name = mainFolderOfClassesForCreateRelativePaths_path.relativize(sourceFolderOrFilePath).toFile().getPath().replace("\\", "/");
			if (sourceFolderOrFile.isDirectory()) 
			{
				//	      String name = sourceFolder.getPath().replace("\\", "/");
				if (!name.isEmpty()) {
					if (!name.endsWith("/"))
						name += "/";
					JarEntry entry = new JarEntry(name);
					entry.setTime(sourceFolderOrFile.lastModified());
					target.putNextEntry(entry);
					target.closeEntry();
				}
				for (File nestedFile : sourceFolderOrFile.listFiles())
					add(nestedFile, mainFolderOfClassesForCreateRelativePaths , target);
				return;
			}

//			JarEntry entry = new JarEntry(sourceFolderOrFile.getPath().replace("\\", "/"));
			JarEntry entry = new JarEntry(name);
			entry.setTime(sourceFolderOrFile.lastModified());
			target.putNextEntry(entry);
			in = new BufferedInputStream(new FileInputStream(sourceFolderOrFile));

			byte[] buffer = new byte[1024];
			while (true) {
				int count = in.read(buffer);
				if (count == -1)
					break;
				target.write(buffer, 0, count);
			}
			target.closeEntry();
		} finally {
			if (in != null)
				in.close();
		}
	}
	
}
