//
//  MNURLStringDownloader.java
//  MultiNet client
//
//  Copyright 2010 PlayPhone. All rights reserved.
//

package com.playphone.multinet.core;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;

public class MNURLStringDownloader extends MNURLDownloader
 {
  public interface IEventHandler extends MNURLDownloader.IErrorEventHandler
   {
    void downloaderDataReady (MNURLDownloader downloader, String str);
   }

  public void loadURL (String url, IEventHandler eventHandler)
   {
    loadURL(url,null,eventHandler);
   }

  public void loadURL (String url, String postBody, IEventHandler eventHandler)
   {
    super.loadURL(url,postBody,eventHandler);
   }

  protected void readData (InputStream inputStream) throws IOException
   {
    StringBuilder builder = new StringBuilder();

    BufferedReader reader = new BufferedReader
                                 (new InputStreamReader
                                       (inputStream,Charset.forName("UTF-8")));

    int    count;
    char[] buffer = new char[BUFFER_SIZE];

    while ((count = reader.read(buffer,0,BUFFER_SIZE)) != -1)
     {
      builder.append(buffer,0,count);
     }

    if (!isCanceled())
     {
      ((IEventHandler)eventHandler).downloaderDataReady(this,builder.toString());
     }
   }

  private static int BUFFER_SIZE = 256;
 }

