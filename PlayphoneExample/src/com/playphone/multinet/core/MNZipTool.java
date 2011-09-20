//
//  MNZipTool.java
//  MultiNet client
//
//  Copyright 2010 PlayPhone. All rights reserved.
//

package com.playphone.multinet.core;

import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MNZipTool
 {
  public interface OperationStopper
   {
    boolean shouldStop ();
   }

  public static boolean unzipFile (File destDir, File srcFile, OperationStopper stopper)
   {
    boolean         ok        = true;
    FileInputStream srcStream = null;
    ZipInputStream  zipStream = null;

    try
     {
      srcStream = new FileInputStream(srcFile);
      zipStream = new ZipInputStream(new BufferedInputStream(srcStream,BUFFER_SIZE));

      ZipEntry        entry     = zipStream.getNextEntry();
      byte[]          buffer    = new byte[BUFFER_SIZE];

      while (ok && entry != null && (stopper == null || !stopper.shouldStop()))
       {
        String name      = entry.getName();
        File   entryFile = new File(destDir,name);

        if (entry.isDirectory())
         {
          if (!entryFile.exists())
           {
            if (!entryFile.mkdirs())
             {
              ok = false;
             }
           }
         }
        else
         {
          FileOutputStream     destStream     = null;
          BufferedOutputStream bufferedStream = null;

          try
           {
            destStream     = new FileOutputStream(entryFile);
            bufferedStream = new BufferedOutputStream(destStream,BUFFER_SIZE);

            int size = zipStream.read(buffer,0,BUFFER_SIZE);

            while (size > 0)
             {
              bufferedStream.write(buffer,0,size);

              size = zipStream.read(buffer,0,BUFFER_SIZE);
             }

            bufferedStream.flush();
           }
          finally
           {
            if      (bufferedStream != null)
             {
              bufferedStream.close();
             }
            else if (destStream != null)
             {
              destStream.close();
             }
           }
         }

        entry = zipStream.getNextEntry();
       }
     }
    catch (Exception e)
     {
      ok = false;
     }
    finally
     {
      try
       {
        if (zipStream != null)
         {
          zipStream.close();
         }
        else if (srcStream != null)
         {
          srcStream.close();
         }
       }
      catch (IOException e)
       {
       }
     }

    return ok;
   }

  public static byte[] getFileDataFromArchive (File zipFile, String fileName)
   {
    ZipFile zip  = null;
    byte[]  data = null;

    try
     {
      zip = new ZipFile(zipFile,ZipFile.OPEN_READ);

      ZipEntry entry = zip.getEntry(fileName);

      if (entry != null)
       {
        long entrySize = entry.getSize();

        if (entrySize <= Integer.MAX_VALUE)
         {
          InputStream dataStream = null;

          try
           {
            dataStream = zip.getInputStream(entry);

            data = new byte[(int)entrySize];

            if (dataStream.read(data) != entrySize)
             {
              data = null;
             }
           }
          catch (IOException e)
           {
            data = null;
           }
          finally
           {
            if (dataStream != null)
             {
              dataStream.close();
             }
           }
         }
       }
     }
    catch (IOException e)
     {
      data = null;
     }
    finally
     {
      if (zip != null)
       {
        try
         {
          zip.close();
         }
        catch (IOException e)
         {
         }
       }
     }

    return data;
   }

  public static byte[] getFileDataFromStream (InputStream inputStream, String fileName)
   {
    ZipInputStream zipStream = null;
    byte[]         data      = null;

    try
     {
      zipStream = new ZipInputStream(new BufferedInputStream(inputStream,BUFFER_SIZE));

      ZipEntry entry = zipStream.getNextEntry();
      boolean  ok    = true;

      while (ok && data == null && entry != null)
       {
        if (!entry.isDirectory())
         {
          if (entry.getName().equals(fileName))
           {
            long entrySize = entry.getSize();

            if (entrySize <= Integer.MAX_VALUE)
             {
              data = new byte[(int)entrySize];

              if (zipStream.read(data) < entrySize)
               {
                data = null;
                ok   = false;
               }
             }
            else
             {
              ok = false;
             }
           }
         }

        if (ok && data == null)
         {
          entry = zipStream.getNextEntry();
         }
       }
     }
    catch (IOException e)
     {
      data = null;
     }
    finally
     {
      closeInputStreamSafely(zipStream);
     }

    return data;
   }

  private static void closeInputStreamSafely (InputStream stream)
   {
    try
     {
      if (stream != null)
       {
        stream.close();
       }
     }
    catch (IOException e)
     {
     }
   }

  private static final int BUFFER_SIZE = 8 * 1024;
 }

