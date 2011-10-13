//
//  MNVShopWSRequestHelper.java
//  MultiNet client
//
//  Copyright 2011 PlayPhone. All rights reserved.
//

package com.playphone.multinet.providers;

import java.util.ArrayList;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.playphone.multinet.core.MNSession;
import com.playphone.multinet.core.MNUtils;
import com.playphone.multinet.core.MNURLDownloader;
import com.playphone.multinet.core.MNURLStringDownloader;
import com.playphone.multinet.core.ws.MNWSXmlTools;
import com.playphone.multinet.MNConst;

class MNVShopWSRequestHelper
 {
  public interface IEventHandler
   {
    public boolean vShopShouldParseResponse  (long   userId);
    public void    vShopPostVItemTransaction (long   srvTransactionId,
                                              long   cliTransactionId,
                                              String itemsToAddStr,
                                              boolean vShopTransactionEnabled);
    public void    vShopFinishTransaction    (String transactionId);
    public void    vShopWSRequestFailed      (long   clientTransactionId,
                                              int    errorCode,
                                              String errorMessage);
   }

  public MNVShopWSRequestHelper (MNSession session, IEventHandler eventHandler)
   {
    this.session      = session;
    this.eventHandler = eventHandler;

    requests = new RequestSet();
   }

  public void shutdown ()
   {
    requests.cancelAll();

    requests     = null;
    session      = null;
    eventHandler = null;
   }

  public void cancelAllRequests ()
   {
    requests.cancelAll();
   }

  public void sendWSRequest (String url, MNUtils.HttpPostBodyStringBuilder postBody, long clientTransactionId)
   {
    String userSId = session.getMySId();

    if (userSId == null)
     {
      onRequestFailed(clientTransactionId,
                      MNVShopProvider.IEventHandler.ERROR_CODE_NETWORK_ERROR,
                      "user is not logged in");
     }

    postBody.addParam("ctx_game_id",Integer.toString(session.getGameId()));
    postBody.addParam("ctx_gameset_id",Integer.toString(session.getDefaultGameSetId()));
    postBody.addParam("ctx_user_id",Long.toString(session.getMyUserId()));
    postBody.addParam("ctx_user_sid",userSId);
    postBody.addParam("ctx_dev_type",Integer.toString(session.getPlatform().getDeviceType()));
    postBody.addParam("ctx_dev_id",MNUtils.stringGetMD5String
                                    (session.getPlatform().getUniqueDeviceIdentifier()));
    postBody.addParam("ctx_client_ver",MNSession.CLIENT_API_VERSION);

    requests.sendRequest(url,postBody.toString(),clientTransactionId);
   }

  public void sendWSRequest (String url, MNUtils.HttpPostBodyStringBuilder postBody)
   {
    sendWSRequest(url,postBody,0);
   }

  private void processFinishTransactionCmd (Element cmdElement)
   {
    String transactionIdStr = null;

    Element currElement = MNWSXmlTools.nodeGetFirstChildElement(cmdElement);

    if (currElement.getTagName().equals("srcTransactionId"))
     {
      eventHandler.vShopFinishTransaction
       (MNWSXmlTools.nodeGetTextContent(currElement));
     }
    else
     {
      session.getPlatform().logWarning
       (TAG,"invalid paremeters in 'postVItemTransaction' command");
     }
   }

  private void processPostVItemTransactionCmd (Element cmdElement)
   {
    String  cliTransactionIdStr     = null;
    String  srvTransactionIdStr     = null;
    String  itemsToAddStr           = null;
    boolean vShopTransactionEnabled = true;

    Element currElement = MNWSXmlTools.nodeGetFirstChildElement(cmdElement);

    while (currElement != null)
     {
      String tagName = currElement.getTagName();

      if      (tagName.equals("clientTransactionId"))
       {
        cliTransactionIdStr = MNWSXmlTools.nodeGetTextContent(currElement);
       }
      else if (tagName.equals("serverTransactionId"))
       {
        srvTransactionIdStr = MNWSXmlTools.nodeGetTextContent(currElement);
       }
      else if (tagName.equals("itemsToAdd"))
       {
        itemsToAddStr = MNWSXmlTools.nodeGetTextContent(currElement);
       }
      else if (tagName.equals("callVShopTransactionSuccess"))
       {
        vShopTransactionEnabled =
         !MNWSXmlTools.nodeGetTextContent(currElement).trim().equals("0");
       }
      else
       {
        session.getPlatform().logWarning(TAG,"unknown element in 'postVItemTransaction' command");
       }

      currElement = MNWSXmlTools.nodeGetNextSiblingElement(currElement);
     }

    Long srvTransactionId = null;
    Long cliTransactionId = null;

    if (srvTransactionIdStr != null)
     {
      srvTransactionId = MNUtils.parseLong(srvTransactionIdStr);
     }

    if (cliTransactionIdStr != null)
     {
      cliTransactionId = MNUtils.parseLong(cliTransactionIdStr);
     }

    if (srvTransactionId != null && cliTransactionId != null && itemsToAddStr != null)
     {
      eventHandler.vShopPostVItemTransaction
       (srvTransactionId,cliTransactionId,itemsToAddStr,vShopTransactionEnabled);
     }
    else
     {
      session.getPlatform().logWarning
       (TAG,"invalid paremeters in 'postVItemTransaction' command");
     }
   }

  private void processPostSysEventCmd (Element cmdElement)
   {
    String eventName  = null;
    String eventParam = null;
    String callbackId = null;

    Element currElement = MNWSXmlTools.nodeGetFirstChildElement(cmdElement);

    while (currElement != null)
     {
      String tagName = currElement.getTagName();

      if      (tagName.equals("eventName"))
       {
        eventName = MNWSXmlTools.nodeGetTextContent(currElement);
       }
      else if (tagName.equals("eventParam"))
       {
        eventParam = MNWSXmlTools.nodeGetTextContent(currElement);
       }
      else if (tagName.equals("callbackId"))
       {
        callbackId = MNWSXmlTools.nodeGetTextContent(currElement);
       }
      else
       {
        session.getPlatform().logWarning(TAG,"unknown element in 'postSysEvent' command");
       }

      currElement = MNWSXmlTools.nodeGetNextSiblingElement(currElement);
     }

    if (eventName != null)
     {
      session.postSysEvent(eventName,eventParam != null ? eventParam : "",callbackId);
     }
    else
     {
      session.getPlatform().logWarning(TAG,"event name is absent in 'postSysEvent' command");
     }
   }

  private void processPostPluginMessageCmd (Element cmdElement)
   {
    String pluginName  = null;
    String message     = null;

    Element currElement = MNWSXmlTools.nodeGetFirstChildElement(cmdElement);

    while (currElement != null)
     {
      String tagName = currElement.getTagName();

      if      (tagName.equals("pluginName"))
       {
        pluginName = MNWSXmlTools.nodeGetTextContent(currElement);
       }
      else if (tagName.equals("pluginMessage"))
       {
        message = MNWSXmlTools.nodeGetTextContent(currElement);
       }
      else
       {
        session.getPlatform().logWarning(TAG,"unknown element in 'postPluginMessage' command");
       }

      currElement = MNWSXmlTools.nodeGetNextSiblingElement(currElement);
     }

    if (pluginName != null)
     {
      session.sendPluginMessage(pluginName,message != null ? message : "");
     }
    else
     {
      session.getPlatform().logWarning(TAG,"plugin name is absent in 'postPluginMessage' command");
     }
   }

  private void processWSResponse (String responseStr, long clientTransactionId)
   {
    DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
    String errorMessage = null;
    int    errorCode    = MNVShopProvider.IEventHandler.ERROR_CODE_UNDEFINED;

    try
     {
      DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
      Document        dom        = docBuilder.parse(new org.xml.sax.InputSource(new StringReader(responseStr)));
      Element         docElement = dom.getDocumentElement();

      if (docElement.getTagName().equals("responseData"))
       {
        Element currElement = MNWSXmlTools.nodeGetFirstChildElement(docElement);

        if (currElement != null)
         {
          String tagName = currElement.getTagName();

          if (tagName.equals("ctxUserId"))
           {
            long userId = MNUtils.parseLongWithDefault(MNWSXmlTools.nodeGetTextContent(currElement),MNConst.MN_USER_ID_UNDEFINED);

            if (userId != MNConst.MN_USER_ID_UNDEFINED)
             {
              if (eventHandler.vShopShouldParseResponse(userId))
               {
                currElement = MNWSXmlTools.nodeGetNextSiblingElement(currElement);

                while (currElement != null)
                 {
                  tagName = currElement.getTagName();

                  if      (tagName.equals("finishTransaction"))
                   {
                    processFinishTransactionCmd(currElement);
                   }
                  else if (tagName.equals("postVItemTransaction"))
                   {
                    processPostVItemTransactionCmd(currElement);
                   }
                  else if (tagName.equals("postSysEvent"))
                   {
                    processPostSysEventCmd(currElement);
                   }
                  else if (tagName.equals("postPluginMessage"))
                   {
                    processPostPluginMessageCmd(currElement);
                   }
                  else
                   {
                    session.getPlatform().logWarning(TAG,"invalid command in purchase ws request");
                   }

                  currElement = MNWSXmlTools.nodeGetNextSiblingElement(currElement);
                 }
               }
             }
            else
             {
              errorMessage = "response contains invalid user id value";
              errorCode    = MNVShopProvider.IEventHandler.ERROR_CODE_XML_STRUCTURE_ERROR;
             }
           }
          else if (tagName.equals("errorMessage"))
           {
            errorMessage = MNWSXmlTools.nodeGetTextContent(currElement);
            errorCode    = MNUtils.parseIntWithDefault(currElement.getAttribute("code"),WSFailedErrorCode);
           }
          else
           {
            errorMessage = "response contains neither 'ctxUserId' nor 'errorMessage' element";
            errorCode    = MNVShopProvider.IEventHandler.ERROR_CODE_XML_STRUCTURE_ERROR;
           }
         }
        else
         {
          errorMessage = "response contains no data";
          errorCode    = MNVShopProvider.IEventHandler.ERROR_CODE_XML_STRUCTURE_ERROR;
         }
       }
      else
       {
        errorMessage = "invalid document element in response";
        errorCode    = MNVShopProvider.IEventHandler.ERROR_CODE_XML_STRUCTURE_ERROR;
       }
     }
    catch (Exception e)
     {
      errorMessage = e.getMessage();
      errorCode    = MNVShopProvider.IEventHandler.ERROR_CODE_XML_PARSE_ERROR;
     }

    if (errorMessage != null)
     {
      eventHandler.vShopWSRequestFailed
      (clientTransactionId,errorCode,errorMessage);
     }
   }

  /*package*/ void onRequestSucceeded (long   clientTransactionId,
                                       String responseStr)
   {
    processWSResponse(responseStr,clientTransactionId);
   }

  /*package*/ void onRequestFailed    (long   clientTransactionId,
                                       int    errorCode,
                                       String errorMessage)
   {
    eventHandler.vShopWSRequestFailed
     (clientTransactionId,errorCode,errorMessage);
   }

  private class RequestSet implements MNURLStringDownloader.IEventHandler
   {
    public RequestSet ()
     {
      requests = new ArrayList<RequestInfo>();
     }

    public synchronized void sendRequest (String requestUrl,
                                          String postBody,
                                          long   clientTransactionId)
     {
      MNURLStringDownloader downloader = new MNURLStringDownloader();

      requests.add(new RequestInfo(downloader,clientTransactionId));

      downloader.loadURL(requestUrl,postBody,this);
     }

    public synchronized void cancelAll ()
     {
      while (!requests.isEmpty())
       {
        RequestInfo requestInfo = requests.remove(requests.size() - 1);

        requestInfo.downloader.cancel();
       }
     }

    private int findRequestIndex (MNURLDownloader downloader)
     {
      int         index       = 0;
      int         count       = requests.size();
      boolean     found       = false;

      while (index < count && !found)
       {
        RequestInfo requestInfo = requests.get(index);

        if (requestInfo.downloader == downloader)
         {
          found = true;
         }
        else
         {
          index++;
         }
       }

      return found ? index : -1;
     }

    private RequestInfo findRequest (MNURLDownloader downloader)
     {
      int index = findRequestIndex(downloader);

      return index >= 0 ? requests.get(index) : null;
     }

    private void finishRequest (MNURLDownloader downloader)
     {
      int index = findRequestIndex(downloader);

      if (index >= 0)
       {
        requests.remove(index);
       }
     }

    public synchronized void downloaderDataReady (MNURLDownloader downloader, String str)
     {
      RequestInfo requestInfo         = findRequest(downloader);
      long        clientTransactionId = requestInfo != null ? requestInfo.clientTransactionId : 0;

      onRequestSucceeded(clientTransactionId,str);

      finishRequest(downloader);
     }

    public synchronized void downloaderLoadFailed (MNURLDownloader downloader, MNURLDownloader.ErrorInfo errorInfo)
     {
      RequestInfo requestInfo         = findRequest(downloader);
      long        clientTransactionId = requestInfo != null ? requestInfo.clientTransactionId : 0;

      onRequestFailed(clientTransactionId,
                      MNVShopProvider.IEventHandler.ERROR_CODE_NETWORK_ERROR,
                      errorInfo.getMessage());

      finishRequest(downloader);
     }

    private class RequestInfo
     {
      public MNURLStringDownloader downloader;
      public long                  clientTransactionId;

      public RequestInfo (MNURLStringDownloader downloader,
                          long                  clientTransactionId)
       {
        this.downloader          = downloader;
        this.clientTransactionId = clientTransactionId;
       }
     }

    private final ArrayList<RequestInfo> requests;
   }

  private MNSession     session;
  private IEventHandler eventHandler;
  private RequestSet    requests;

  private static final String TAG = "MNVShopWSRequestHelper";
  private static final int    WSFailedErrorCode = 100;
 }

