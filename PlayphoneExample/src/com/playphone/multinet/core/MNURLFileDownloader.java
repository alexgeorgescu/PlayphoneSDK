//
//  MNURLFileDownloader.java
//  MultiNet client
//
//  Copyright 2010 PlayPhone. All rights reserved.
//

package com.playphone.multinet.core;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileOutputStream;

public class MNURLFileDownloader extends MNURLDownloader
 {
  public interface IEventHandler extends MNURLDownloader.IErrorEventHandler
   {
    void downloaderLoadSucceeded (MNURLDownloader downloader);
   }

  public synchronized void loadURL (File destFile, String url, IEventHandler eventHandler)
   {
    loadURL(destFile,url,null,eventHandler);
   }

  public synchronized void loadURL (File destFile, String url, String postBody, IEventHandler eventHandler)
   {
    this.file = destFile;

    super.loadURL(url,postBody,eventHandler);
   }

  protected void readData (InputStream inputStream) throws IOException
   {
    FileOutputStream fileStream = null;

    try
     {
      fileStream = new FileOutputStream(file);

      int    count;
      byte[] buffer = new byte[BUFFER_SIZE];

      while ((count = inputStream.read(buffer,0,BUFFER_SIZE)) != -1)
       {
        fileStream.write(buffer,0,count);
       }

      if (!isCanceled())
       {
        ((IEventHandler)eventHandler).downloaderLoadSucceeded(this);
       }
     }
    finally
     {
      if (fileStream != null)
       {
        try
         {
          fileStream.close();
         }
        catch (IOException e)
         {
         }
       }
     }
   }

  private File file;
  private static final int BUFFER_SIZE = 2 * 1024;
 }

