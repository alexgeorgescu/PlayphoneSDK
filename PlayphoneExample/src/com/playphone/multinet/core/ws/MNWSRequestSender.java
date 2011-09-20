//
//  MNWSRequestSender.java
//  MultiNet client
//
//  Copyright 2011 PlayPhone. All rights reserved.
//

package com.playphone.multinet.core.ws;

import java.util.HashMap;
import java.io.StringReader;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.playphone.multinet.core.MNURLDownloader;
import com.playphone.multinet.core.MNURLStringDownloader;
import com.playphone.multinet.core.MNSession;
import com.playphone.multinet.core.MNUtils;

import com.playphone.multinet.core.ws.parser.IMNWSXmlDataParser;
import com.playphone.multinet.core.ws.parser.MNWSXmlGenericItemListParser;
import com.playphone.multinet.core.ws.parser.MNWSXmlGenericItemParser;
import com.playphone.multinet.core.ws.data.MNWSGenericItem;
import com.playphone.multinet.core.ws.data.MNWSBuddyListItem;
import com.playphone.multinet.core.ws.data.MNWSAnyUserItem;
import com.playphone.multinet.core.ws.data.MNWSAnyGameItem;
import com.playphone.multinet.core.ws.data.MNWSLeaderboardListItem;
import com.playphone.multinet.core.ws.data.MNWSCurrUserSubscriptionStatus;
import com.playphone.multinet.core.ws.data.MNWSCurrentUserInfo;
import com.playphone.multinet.core.ws.data.MNWSRoomListItem;
import com.playphone.multinet.core.ws.data.MNWSRoomUserInfoItem;

public class MNWSRequestSender
 {
  public MNWSRequestSender (MNSession session)
   {
    this.session = session;

    parsers = new HashMap<String,IMNWSXmlDataParser>();

    setupStdParsers();
   }

  public IMNWSRequest sendWSRequest           (MNWSRequestContent content, IMNWSRequestEventHandler eventHandler)
   {
    return sendWSRequest(content,eventHandler,false);
   }

  public IMNWSRequest sendWSRequestAuthorized (MNWSRequestContent content, IMNWSRequestEventHandler eventHandler)
   {
    return sendWSRequest(content,eventHandler,true);
   }

  private IMNWSRequest sendWSRequest (MNWSRequestContent content, IMNWSRequestEventHandler eventHandler, boolean authorized)
   {
    String webServerUrl = session.getWebServerURL();

    if (webServerUrl == null)
     {
      if (eventHandler != null)
       {
        eventHandler.onRequestError
         (new MNWSRequestError
           (MNWSRequestError.TRANSPORT_ERROR,"request cannot be sent (server url is undefined)"));
       }

      return null;
     }

    String userSId = session.getMySId();

    if (authorized && userSId == null)
     {
      if (eventHandler != null)
       {
        eventHandler.onRequestError
         (new MNWSRequestError
           (MNWSRequestError.PARAMETERS_ERROR,"authorized request cannot be sent if user is not logged in"));
       }

      return null;
     }

    Request request = new Request(eventHandler,content.getMapping());

    webServerUrl = webServerUrl + "/" + WS_URL_PATH;

    MNUtils.HttpPostBodyStringBuilder postBodyBuilder = new MNUtils.HttpPostBodyStringBuilder();

    postBodyBuilder.addParam("ctx_game_id",Integer.toString(session.getGameId()));
    postBodyBuilder.addParam("ctx_gameset_id",Integer.toString(session.getDefaultGameSetId()));
    postBodyBuilder.addParam("ctx_dev_type",Integer.toString(session.getPlatform().getDeviceType()));
    postBodyBuilder.addParam("ctx_dev_id",MNUtils.stringGetMD5String
                                           (session.getPlatform().getUniqueDeviceIdentifier()));

    if (authorized)
     {
      postBodyBuilder.addParam("ctx_user_id",Long.toString(session.getMyUserId()));
      postBodyBuilder.addParam("ctx_user_sid",userSId);
     }

    postBodyBuilder.addParam("info_list",content.getRequestInfoListString());

    request.sendRequest(webServerUrl,postBodyBuilder.toString());

    return request;
   }

  private class Request implements IMNWSRequest, MNURLStringDownloader.IEventHandler
   {
    protected Request (IMNWSRequestEventHandler eventHandler, HashMap<String,String> nameMapping)
     {
      this.downloader   = null;
      this.eventHandler = eventHandler;
      this.nameMapping  = nameMapping;
     }

    protected void sendRequest (String url, String postBody)
     {
      downloader = new MNURLStringDownloader();
      downloader.loadURL(url,postBody,this);
     }

    public synchronized void cancel ()
     {
      if (downloader != null)
       {
        downloader.cancel();
       }

      cleanUp();
     }

    public synchronized void downloaderDataReady (MNURLDownloader downloader, String str)
     {
      if (eventHandler != null)
       {
        handleXmlResponse(str);
       }

      cleanUp();
     }

    public synchronized void downloaderLoadFailed (MNURLDownloader downloader, MNURLDownloader.ErrorInfo errorInfo)
     {
      if (eventHandler != null)
       {
        eventHandler.onRequestError
         (new MNWSRequestError
           (MNWSRequestError.TRANSPORT_ERROR,errorInfo.getMessage()));
       }

      cleanUp();
     }

    private void cleanUp ()
     {
      downloader   = null;
      eventHandler = null;
      nameMapping  = null;
     }

    private void handleXmlResponse (String xmlString)
     {
      DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();

      try
       {
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        Document        dom        = docBuilder.parse(new org.xml.sax.InputSource(new StringReader(xmlString)));
        Element         docElement = dom.getDocumentElement();

        if (!docElement.getTagName().equals("responseData"))
         {
          throw new Exception("invalid document element tag (" + docElement.getTagName() + ") in web-service response");
         }

        Element currElement = MNWSXmlTools.nodeGetFirstChildElement(docElement);

        if (currElement != null &&
            currElement.getTagName().equals("errorMessage"))
         {
          eventHandler.onRequestError
           (new MNWSRequestError
             (MNWSRequestError.SERVER_ERROR,
              MNWSXmlTools.nodeGetTextContent(currElement)));
         }
        else
         {
          MNWSResponse response = new MNWSResponse();

          while (currElement != null)
           {
            String tagName    = currElement.getTagName();
            String parserName = nameMapping == null ? tagName : nameMapping.get(tagName);

            IMNWSXmlDataParser parser = parsers.get(parserName != null ? parserName : tagName);

            if (parser != null)
             {
              response.addBlock(tagName,parser.parseElement(currElement));
             }
            else
             {
              response.addBlock(tagName,currElement);
             }

            currElement = MNWSXmlTools.nodeGetNextSiblingElement(currElement);
           }

          eventHandler.onRequestCompleted(response);
         }
       }
      catch (Exception e)
       {
        e.printStackTrace();

        eventHandler.onRequestError
         (new MNWSRequestError
           (MNWSRequestError.PARSE_ERROR,e.toString()));
       }
     }

    private MNURLStringDownloader    downloader;
    private IMNWSRequestEventHandler eventHandler;
    private HashMap<String,String>   nameMapping;
   }

  private void setupStdParsers ()
   {
    MNWSXmlGenericItemListParser buddyListParser =
     new MNWSXmlGenericItemListParser
      ("buddyItem",
       new MNWSXmlGenericItemParser()
        {
         public MNWSGenericItem createNewItem ()
          {
           return new MNWSBuddyListItem();
          }
        });

    MNWSXmlGenericItemListParser leaderBoardListParser =
     new MNWSXmlGenericItemListParser
      ("leaderboardItem",
       new MNWSXmlGenericItemParser()
        {
         public MNWSGenericItem createNewItem ()
          {
           return new MNWSLeaderboardListItem();
          }
        });

    parsers.put("currentUserBuddyList",buddyListParser);
    parsers.put("anyUser",new MNWSXmlGenericItemParser()
                           {
                            public MNWSGenericItem createNewItem ()
                             {
                              return new MNWSAnyUserItem();
                             }
                           });
    parsers.put("anyGame",new MNWSXmlGenericItemParser()
                           {
                            public MNWSGenericItem createNewItem ()
                             {
                              return new MNWSAnyGameItem();
                             }
                           });
    parsers.put("currentUserLeaderboardGlobalThisWeek",leaderBoardListParser);
    parsers.put("currentUserLeaderboardGlobalThisMonth",leaderBoardListParser);
    parsers.put("currentUserLeaderboardGlobalAllTime",leaderBoardListParser);
    parsers.put("currentUserLeaderboardLocalThisWeek",leaderBoardListParser);
    parsers.put("currentUserLeaderboardLocalThisMonth",leaderBoardListParser);
    parsers.put("currentUserLeaderboardLocalAllTime",leaderBoardListParser);
    parsers.put("anyGameLeaderboardGlobalThisWeek",leaderBoardListParser);
    parsers.put("anyGameLeaderboardGlobalThisMonth",leaderBoardListParser);
    parsers.put("anyGameLeaderboardGlobalAllTime",leaderBoardListParser);
    parsers.put("anyUserAnyGameLeaderboardGlobalThisWeek",leaderBoardListParser);
    parsers.put("anyUserAnyGameLeaderboardGlobalThisMonth",leaderBoardListParser);
    parsers.put("anyUserAnyGameLeaderboardGlobalAllTime",leaderBoardListParser);
    parsers.put("currentUserAnyGameLeaderboardLocalThisWeek",leaderBoardListParser);
    parsers.put("currentUserAnyGameLeaderboardLocalThisMonth",leaderBoardListParser);
    parsers.put("currentUserAnyGameLeaderboardLocalAllTime",leaderBoardListParser);
    parsers.put("currentUserSubscriptionStatus",
                 new MNWSXmlGenericItemParser()
                  {
                   public MNWSGenericItem createNewItem ()
                    {
                     return new MNWSCurrUserSubscriptionStatus();
                    }
                  });
    parsers.put("currentUser",
                 new MNWSXmlGenericItemParser()
                  {
                   public MNWSGenericItem createNewItem ()
                    {
                     return new MNWSCurrentUserInfo();
                    }
                  });
    parsers.put("currentGameRoomList",
                new MNWSXmlGenericItemListParser
                     ("roomInfoItem",
                      new MNWSXmlGenericItemParser()
                       {
                        public MNWSGenericItem createNewItem ()
                         {
                          return new MNWSRoomListItem();
                         }
                       }));
    parsers.put("currentGameRoomUserList",
                new MNWSXmlGenericItemListParser
                     ("roomUserInfoItem",
                      new MNWSXmlGenericItemParser()
                       {
                        public MNWSGenericItem createNewItem ()
                         {
                          return new MNWSRoomUserInfoItem();
                         }
                       }));
   }

  private MNSession                          session;
  private HashMap<String,IMNWSXmlDataParser> parsers;

  private static final String WS_URL_PATH = "user_ajax_host.php";
 }

