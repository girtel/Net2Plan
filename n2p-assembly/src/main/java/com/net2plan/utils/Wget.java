package com.net2plan.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.codec.binary.StringUtils;

public class Wget 
{
	public enum WgetStatus {
		  Success, MalformedUrl, IoException, UnableToCloseOutputStream;
		}

  public static Pair<String,WgetStatus> wGet(String urlOfFile) 
  {
    InputStream httpIn = null;
    ByteArrayOutputStream fileOutput = new ByteArrayOutputStream();
    OutputStream bufferedOut = null;
    try 
    {
      // check the http connection before we do anything to the fs
      httpIn = new BufferedInputStream(new URL(urlOfFile).openStream());
      // prep saving the file
      bufferedOut = new BufferedOutputStream(fileOutput, 1024);
      byte data[] = new byte[1024];
      boolean fileComplete = false;
      int count = 0;
      while (!fileComplete) {
        count = httpIn.read(data, 0, 1024);
        if (count <= 0) {
          fileComplete = true;
        } else {
          bufferedOut.write(data, 0, count);
        }
      }
    } catch (MalformedURLException e) {
      return Pair.of ("", WgetStatus.MalformedUrl);
    } catch (IOException e) {
      return Pair.of ("", WgetStatus.IoException);
    } finally {
      try {
        bufferedOut.close();
        fileOutput.close();
        httpIn.close();
      } catch (IOException e) {
        return Pair.of ("", WgetStatus.UnableToCloseOutputStream);
      }
    }
    final String fileContent = StringUtils.newStringUtf8(fileOutput.toByteArray());
    return Pair.of (fileContent, WgetStatus.Success);
  }


  public static WgetStatus wGet(String urlOfFile , File outputFile) 
  {
    InputStream httpIn = null;
    FileOutputStream fileOutput = null;
    OutputStream bufferedOut = null;
    try 
    {
        fileOutput = new FileOutputStream(outputFile);
//      ByteArrayOutputStream fileOutput = new ByteArrayOutputStream();
      // check the http connection before we do anything to the fs
      httpIn = new BufferedInputStream(new URL(urlOfFile).openStream());
      // prep saving the file
      bufferedOut = new BufferedOutputStream(fileOutput, 1024);
      byte data[] = new byte[1024];
      boolean fileComplete = false;
      int count = 0;
      while (!fileComplete) {
        count = httpIn.read(data, 0, 1024);
        if (count <= 0) {
          fileComplete = true;
        } else {
          bufferedOut.write(data, 0, count);
        }
      }
    } catch (MalformedURLException e) {
      return WgetStatus.MalformedUrl;
    } catch (IOException e) {
      return WgetStatus.IoException;
    } finally {
      try {
    	  if (bufferedOut != null) bufferedOut.close();
    	  if (fileOutput != null) fileOutput.close();
    	  if (httpIn !=null) httpIn.close();
      } catch (IOException e) {
        return WgetStatus.UnableToCloseOutputStream;
      }
    }
    return WgetStatus.Success;
  }

  
  public static void main (String [] args)
  {
	  final Pair<String,WgetStatus> res = Wget.wGet("https://www.google.es");
	  System.out.println(res.getFirst());
	  System.out.println("---------------------");
	  System.out.println(res.getSecond());
  }
}