//
//  MNURLTextDownloader.java
//  MultiNet client
//
//  Copyright 2009 PlayPhone. All rights reserved.
//

package com.playphone.multinet.core;

import java.io.InputStream;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.charset.Charset;

public class MNURLTextDownloader extends MNURLDownloader
 {
  public interface IEventHandler extends MNURLDownloader.IErrorEventHandler
   {
    void downloaderDataReady (MNURLDownloader downloader, String[] data);
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
    ArrayList<String> content = new ArrayList<String>();

    BufferedReader reader = new BufferedReader
                                 (new InputStreamReader
                                       (inputStream,Charset.forName("UTF-8")));

    String line = reader.readLine();

    while (line != null)
     {
      content.add(line);

      line = reader.readLine();
     }

    if (!isCanceled())
     {
      ((IEventHandler)eventHandler).downloaderDataReady(this,content.toArray(new String[content.size()]));
     }
   }
 }

