//
//  MNUIWebViewHttpReqQueue.java
//  MultiNet client
//
//  Copyright 2010 PlayPhone. All rights reserved.
//

package com.playphone.multinet.core;

import java.util.ArrayList;

class MNUIWebViewHttpReqQueue
 {
  public interface IEventHandler
   {
    void mnUiWebViewHttpReqSucceeded (String jsCode, int flags);
    void mnUiWebViewHttpReqFailed    (String jsCode, int flags);
   }

  public MNUIWebViewHttpReqQueue (IEventHandler eventHandler)
   {
    this.eventHandler = eventHandler;

    requests = new ArrayList<Request>();
   }

  public synchronized void shutdown ()
   {
    int count = requests.size();

    for (int i = 0; i < count; i++)
     {
      requests.get(i).cancel();
     }

    requests.clear();
   }

  public synchronized void addRequest (String url, String postBody, String successJSCode, String failJSCode, int flags)
   {
    Request req = new Request(successJSCode,failJSCode,flags);

    requests.add(req);

    req.load(url,postBody);
   }

  private int findRequest (Request request)
   {
    int count = requests.size();

    for (int i = 0; i < count; i++)
     {
      if (requests.get(i) == request)
       {
        return i;
       }
     }

    return -1;
   }

  private synchronized void handleDataReady (Request request, String data)
   {
    int i = findRequest(request);

    if (i >= 0)
     {
      String escapedString = MNUtils.stringAsJSString(data);
      String jsCode        = MNUtils.stringReplace
                              (request.successJSCode,
                               "RESPONSE_TEXT",
                               escapedString);

      eventHandler.mnUiWebViewHttpReqSucceeded(jsCode,request.flags);

      requests.remove(i);
     }
   }

  private synchronized void handleLoadFailed (Request request, MNURLTextDownloader.ErrorInfo errorInfo)
   {
    int i = findRequest(request);

    if (i >= 0)
     {
      String escapedTextString = MNUtils.stringAsJSString(errorInfo.getMessage());
      String escapedCodeString = MNUtils.stringAsJSString(Integer.toString(errorInfo.getHttpStatus()));

      String jsCode = MNUtils.stringReplace
                       (request.failJSCode,"RESPONSE_ERROR_CODE",escapedCodeString);

      jsCode = MNUtils.stringReplace(jsCode,"RESPONSE_ERROR_TEXT",escapedTextString);

      eventHandler.mnUiWebViewHttpReqFailed(jsCode,request.flags);

      requests.remove(i);
     }
   }

  private class Request implements MNURLStringDownloader.IEventHandler
   {
    public Request (String successJSCode, String failJSCode, int flags)
     {
      this.downloader    = new MNURLStringDownloader();
      this.successJSCode = successJSCode;
      this.failJSCode    = failJSCode;
      this.flags         = flags;
     }

    public void load (String url, String postBody)
     {
      downloader.loadURL(url,postBody,this);
     }

    public synchronized void cancel ()
     {
      downloader.cancel();
     }

    public synchronized void downloaderDataReady  (MNURLDownloader downloader, String str)
     {
      handleDataReady(this,str);
     }

    public synchronized void downloaderLoadFailed (MNURLDownloader downloader, MNURLDownloader.ErrorInfo errorInfo)
     {
      handleLoadFailed(this,errorInfo);
     }

    public MNURLStringDownloader downloader;
    public String                successJSCode;
    public String                failJSCode;
    public int                   flags;
   }

  private IEventHandler      eventHandler;
  private ArrayList<Request> requests;
 }

