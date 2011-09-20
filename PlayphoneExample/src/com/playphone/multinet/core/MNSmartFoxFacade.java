//
//  MNSmartFoxFacade.java
//  MultiNet client
//
//  Copyright 2009 PlayPhone. All rights reserved.
//

package com.playphone.multinet.core;

import com.playphone.multinet.MNConst;

import it.gotoandplay.smartfoxclient.data.SFSObject;
import it.gotoandplay.smartfoxclient.data.Room;
import it.gotoandplay.smartfoxclient.data.User;
import it.gotoandplay.smartfoxclient.SFSEvent;
import it.gotoandplay.smartfoxclient.ISFSEventListener;
import it.gotoandplay.smartfoxclient.SmartFoxClient;

class MNSmartFoxFacade implements ISFSEventListener,MNConfigData.IEventHandler
 {
  public MNSmartFoxFacade (IMNPlatform platform,
                           String      configUrl)
   {
    state = STATE_DISCONNECTED;

    configData = new MNConfigData(configUrl);

    loginOnConfigLoaded = false;

    lobbyRoomId = MNSession.MN_LOBBY_ROOM_ID_UNDEFINED;

    connectionActivity = new MNConnectionActivity
                              (new MNNetworkStatus(platform),this);

    smartFox.addEventListener(SFSEvent.onConnection,this);
    smartFox.addEventListener(SFSEvent.onConnectionLost,this);
    smartFox.addEventListener(SFSEvent.onLogin,this);
    smartFox.addEventListener(SFSEvent.onExtensionResponse,this);
    smartFox.addEventListener(SFSEvent.onRoomListUpdate,this);
   }

  public void setEventHandler (IEventHandler eventHandler)
   {
    this.eventHandler = eventHandler;
   }

  public void loadConfig ()
   {
    if (state == STATE_DISCONNECTED)
     {
      startConfigLoad();
     }
   }

  private void startConfigLoad ()
   {
    state = STATE_LOADING_CONFIG;

    eventHandler.onConfigLoadStarted();

    configData.load(this);
   }

  private void loginWithStoredLoginAndConfigInfo ()
   {
    state = STATE_CONNECTING;

    smartFox.ipAddress        = configData.smartFoxAddr;
    smartFox.port             = configData.smartFoxPort;
    smartFox.blueBoxIpAddress = configData.blueBoxAddr;
    smartFox.blueBoxPort      = configData.blueBoxPort;
    smartFox.smartConnect     = configData.smartConnect;

    smartFox.connect(smartFox.ipAddress,smartFox.port);
   }

  void loginWithStoredLoginInfo ()
   {
    autoJoinInvoked = true; /* to prevent autoJoin to to be invoked in */
                            /* onRoomListUpdate handler                */

    if (state != STATE_LOADING_CONFIG)
     {
      state = STATE_DISCONNECTED;
     }

    if (smartFox.isConnected())
     {
      smartFox.disconnect();
     }

    autoJoinInvoked = false;

    if (configData.isLoaded())
     {
      loginWithStoredLoginAndConfigInfo();
     }
    else
     {
      loginOnConfigLoaded = true;

      if (state != STATE_LOADING_CONFIG)
       {
        startConfigLoad();
       }
     }
   }

  public void login (String zone, String userLogin, String userPassword)
   {
    connectionActivity.cancel();

    autoJoinInvoked = true; /* to prevent autoJoin to to be invoked in */
                            /* onRoomListUpdate handler                */

    if (smartFox.isConnected())
     {
      smartFox.disconnect();
     }

    this.zone         = zone;
    this.userLogin    = userLogin;
    this.userPassword = userPassword;

    loginWithStoredLoginInfo();
   }

  public void logout ()
   {
    connectionActivity.cancel();

    smartFox.disconnect();

    configData.clear();
   }

  public void relogin ()
   {
//    loginWithStoredLoginInfo();

    connectionActivity.start();
   }

  public boolean haveLoginInfo ()
   {
    return userLogin != null;
   }

  public boolean isLoggedIn ()
   {
    return state == STATE_LOGGED_IN;
   }

  public String getLoginInfoLogin ()
   {
    return userLogin;
   }

  public void updateLoginInfo (String userLogin, String userPassword)
   {
    this.userLogin    = userLogin;
    this.userPassword = userPassword;
   }

  /*FIXME: not implemented */
  /*
  public void restoreConnection ()
   {
   }
  */

  public String getUserNameBySFId (int sfId)
   {
    if (!smartFox.isConnected())
     {
      return null;
     }

    Room currentRoom = smartFox.getRoom(smartFox.activeRoomId);

    if (currentRoom == null)
     {
      return null;
     }

    User userInfo = currentRoom.getUser(sfId);

    if (userInfo == null)
     {
      return null;
     }

    return userInfo.getName();
   }

  public void mnConfigDataLoaded     (MNConfigData configData)
   {
    state = STATE_DISCONNECTED;

    if (loginOnConfigLoaded)
     {
      loginOnConfigLoaded = false;

      loginWithStoredLoginAndConfigInfo();
     }

    if (eventHandler != null)
     {
      eventHandler.onConfigLoaded();
     }
   }

  public void mnConfigDataLoadFailed (String       errorMessage)
   {
    state = STATE_DISCONNECTED;

    if (loginOnConfigLoaded)
     {
      loginOnConfigLoaded = false;

      if (eventHandler != null)
       {
        eventHandler.onLoginFailed(errorMessage);
       }
     }

    if (eventHandler != null)
     {
      eventHandler.onConfigLoadFailed(errorMessage);
     }
   }

  public void handleEvent (final SFSEvent event)
   {
    if (event.getName().equals(SFSEvent.onConnection))
     {
      if (state == STATE_CONNECTING)
       {
        if (event.getParams().getBool("success"))
         {
          connectionActivity.connectionEstablished();

          state = STATE_CONNECTED;

          smartFox.login(zone,userLogin,userPassword);
         }
        else
         {
          sessionInfo = null;

          state = STATE_DISCONNECTED;

          if (eventHandler != null)
           {
            eventHandler.onLoginFailed(event.getParams().getString("error"));
           }

          connectionActivity.connectionFailed();
         }
       }
     }
    else if (event.getName().equals(SFSEvent.onLogin))
     {
      if (state == STATE_CONNECTED)
       {
        if (event.getParams().getBool("success"))
         {
          sessionInfo = new SessionInfo(MNConst.MN_USER_ID_UNDEFINED,
                                        event.getParams().getString("name"),
                                        null,
                                        MNSession.MN_LOBBY_ROOM_ID_UNDEFINED,
                                        null);
         }
        else
         {
          sessionInfo = null;

          state = STATE_DISCONNECTED;

          smartFox.disconnect();

          if (eventHandler != null)
           {
            eventHandler.onLoginFailed(event.getParams().getString("error"));
           }
         }
       }
     }
    else if (event.getName().equals(SFSEvent.onConnectionLost))
     {
      sessionInfo = null;

      if (state != STATE_DISCONNECTED)
       {
        state = STATE_DISCONNECTED;

        if (eventHandler != null)
         {
          eventHandler.onConnectionLost();
         }
       }
     }
    else if (event.getName().equals(SFSEvent.onExtensionResponse))
     {
      if (state == STATE_CONNECTED)
       {
        if (event.getParams().getString("type").equals(SmartFoxClient.XTMSG_TYPE_XML))
         {
          SFSObject data = (SFSObject)event.getParams().get("dataObj");
          String cmd = data.getString("_cmd");

          if (cmd != null)
           {
            if (cmd.equals("MN_logOK"))
             {
              boolean ok           = true;
              String  userName     = data.getString("MN_user_name");
              String  userSId      = data.getString("MN_user_sid");
              String  userAuthSign = data.getString("MN_user_auth_sign");
              long    userId       = MNConst.MN_USER_ID_UNDEFINED;
              int     userSFId     = MNConst.MN_USER_SFID_UNDEFINED;

              lobbyRoomId  = MNSession.MN_LOBBY_ROOM_ID_UNDEFINED;

              try
               {
                userId      = Long.parseLong(data.getString("MN_user_id"));
                userSFId    = Integer.parseInt(data.getString("MN_user_sfid"));
                lobbyRoomId = Integer.parseInt(data.getString("MN_lobby_room_sfid"));
               }
              catch (NumberFormatException e)
               {
                ok = false;
               }

              if (ok)
               {
                if (userName.length() == 0 || userSId.length() == 0)
                 {
                  ok = false;
                 }
               }

              if (ok)
               {
                smartFox.amIModerator = false;
                smartFox.myUserId     = userSFId;
                smartFox.myUserName   = userName;
                smartFox.playerId     = -1;

                sessionInfo = new SessionInfo(userId,userName,userSId,lobbyRoomId,userAuthSign);

                if (eventHandler != null)
                 {
                  eventHandler.onPreLoginSucceeded
                   (sessionInfo.userId,
                    sessionInfo.userName,
                    sessionInfo.sid,
                    sessionInfo.lobbyRoomId,
                    sessionInfo.userAuthSign);
                 }
               }
              else
               {
                state = STATE_DISCONNECTED;

                smartFox.disconnect();

                eventHandler.onLoginFailed
                 (MNI18n.getLocalizedString
                   ("login extension error - invalid user_id or lobby_room_sfid received",
                    MNI18n.MESSAGE_CODE_LOGIN_EXT_INVALID_USERID_OR_LOBBYROOMSFID_RECEIVED_ERROR));
               }
             }
            else if (cmd.equals("MN_logKO"))
             {
              state = STATE_DISCONNECTED;

              if (eventHandler != null)
               {
                eventHandler.onLoginFailed(data.getString("MN_err_msg"));
               }

              smartFox.disconnect();
             }
            else if (cmd.equals("initUserInfo"))
             {
              if (sessionInfo != null)
               {
                state = STATE_LOGGED_IN;

                if (eventHandler != null)
                 {
                  eventHandler.onLoginSucceeded();
                 }

                sessionInfo = null;
               }
             }
           }
         }
       }
     }
    else if (event.getName().equals(SFSEvent.onRoomListUpdate))
     {
      if (!autoJoinInvoked)
       {
        autoJoinInvoked = true;

        if (lobbyRoomId != MNSession.MN_LOBBY_ROOM_ID_UNDEFINED)
         {
          smartFox.joinRoom(lobbyRoomId);

          lobbyRoomId = MNSession.MN_LOBBY_ROOM_ID_UNDEFINED;
         }
       }
     }
   }

  interface IEventHandler
   {
    void onPreLoginSucceeded (long userId, String userName, String SId, int lobbyRoomId, String userAuthSign);
    void onLoginSucceeded ();
    void onLoginFailed (String error);
    void onConnectionLost ();

    void onConfigLoadStarted ();
    void onConfigLoaded ();
    void onConfigLoadFailed (String error);
   }

  private static class SessionInfo
   {
    public SessionInfo (long   userId,
                        String userName,
                        String sid,
                        int    lobbyRoomId,
                        String userAuthSign)
     {
      this.userId       = userId;
      this.userName     = userName;
      this.sid          = sid;
      this.lobbyRoomId  = lobbyRoomId;
      this.userAuthSign = userAuthSign;
     }

    public long   userId;
    public String userName;
    public String sid;
    public int    lobbyRoomId;
    public String userAuthSign;
   }

  public MNConfigData configData;

  SmartFoxClient smartFox = new SmartFoxClient(false);
  private MNConnectionActivity connectionActivity;
  private String zone;
  private String userLogin;
  private String userPassword;
  private IEventHandler eventHandler;
  private int state;
  private boolean autoJoinInvoked;
  private boolean loginOnConfigLoaded;
  private SessionInfo sessionInfo;
  private int         lobbyRoomId; // used to store lobby room id between "logOk" and "roomListUpdated" events

  private static final int STATE_DISCONNECTED   = 0;
  private static final int STATE_LOADING_CONFIG = 1;
  private static final int STATE_CONNECTING     = 2;
  private static final int STATE_CONNECTED      = 3;
  private static final int STATE_LOGGED_IN      = 4;
 }

