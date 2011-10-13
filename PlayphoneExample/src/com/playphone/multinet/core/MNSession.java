//
//  MNSession.java
//  MultiNet client
//
//  Copyright 2009 PlayPhone. All rights reserved.
//

package com.playphone.multinet.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.Map;
import java.util.Random;
import java.util.Date;
import java.util.Locale;
import android.content.Intent;

import com.facebook.android.Facebook;

import it.gotoandplay.smartfoxclient.data.SFSObject;
import it.gotoandplay.smartfoxclient.data.SFSVariable;
import it.gotoandplay.smartfoxclient.data.Room;
import it.gotoandplay.smartfoxclient.data.User;
import it.gotoandplay.smartfoxclient.SmartFoxClient;
import it.gotoandplay.smartfoxclient.SFSEvent;
import it.gotoandplay.smartfoxclient.ISFSEventListener;

import com.playphone.multinet.MNConst;
import com.playphone.multinet.MNGameParams;
import com.playphone.multinet.MNUserInfo;
import com.playphone.multinet.MNErrorInfo;

/**
 * A class representing MultiNet session.
 * This class is responsible for all interactions with MultiNet server.
 */
public class MNSession implements MNSmartFoxFacade.IEventHandler,
                                  ISFSEventListener,
                                  MNSocNetSessionFB.IEventHandler,
                                  MNOfflinePack.IEventHandler
 {
  /**
   * Constructs a new MNSession object.
   *
   * @param gameId       game id
   * @param gameSecret   game secret
   * @param activity     activity
   */
  public MNSession (int gameId, String gameSecret, android.app.Activity activity)
   {
    this(gameId,gameSecret,new MNPlatformAndroid(activity));
   }

  /**
   * Constructs a new MNSession object.
   *
   * @param gameId       game id
   * @param gameSecret   game secret
   * @param platform     platform-dependent object implementing
   *                     <code>IMNPlatform</code> interface
   * @see IMNPlatform
   */
  public MNSession (int gameId, String gameSecret, IMNPlatform platform)
   {
    this.gameId     = gameId;
    this.gameSecret = gameSecret;
    this.platform   = platform;
    status          = MNConst.MN_OFFLINE;
    userId          = MNConst.MN_USER_ID_UNDEFINED;
    lobbyRoomId     = MNSession.MN_LOBBY_ROOM_ID_UNDEFINED;
    userStatus      = MNConst.MN_USER_STATUS_UNDEFINED;
    eventHandlers   = new MNEventHandlerArray<IMNSessionEventHandler>();

    roomExtraInfoReceived = false;

    smartFoxFacade = new MNSmartFoxFacade
                          (platform,
                           platform.getMultiNetConfigURL() +
                            "?game_id=" + Integer.toString(gameId) +
                            "&dev_type=" + Integer.toString(platform.getDeviceType()) +
                            "&client_ver=" + CLIENT_API_VERSION +
                            "&client_locale=" + Locale.getDefault().toString());
    smartFoxFacade.setEventHandler(this);

    smartFoxFacade.smartFox.addEventListener(SFSEvent.onPublicMessage,this);
    smartFoxFacade.smartFox.addEventListener(SFSEvent.onPrivateMessage,this);
    smartFoxFacade.smartFox.addEventListener(SFSEvent.onRoomVariablesUpdate,this);
    smartFoxFacade.smartFox.addEventListener(SFSEvent.onUserVariablesUpdate,this);
    smartFoxFacade.smartFox.addEventListener(SFSEvent.onJoinRoom,this);
    smartFoxFacade.smartFox.addEventListener(SFSEvent.onUserEnterRoom,this);
    smartFoxFacade.smartFox.addEventListener(SFSEvent.onUserLeaveRoom,this);
    smartFoxFacade.smartFox.addEventListener(SFSEvent.onExtensionResponse,this);

    socNetSessionFB = new MNSocNetSessionFB(platform,this);

    varStorage = new MNVarStorage(platform,VAR_STORAGE_FILE_NAME);

    offlinePack = new MNOfflinePack(platform,gameId,this);

    webServerUrl = null;
    fbAppId      = null;

    launchParam  = null;

    launchTime = System.currentTimeMillis() / 1000;
    launchId   = makeLaunchId(platform);

    trackingSystem = null;
    appConfigVars  = new HashMap<String,String>();

    gameVocabulary = new MNGameVocabulary(this);
   }

  public static String makeGameSecretByComponents (int secret1, int secret2, int secret3, int secret4)
   {
    return MNUtils.makeGameSecretByComponents(secret1,secret2,secret3,secret4);
   }

  private static String replaceCommaWithDash (String s)
   {
    return s.replace(',','-');
   }

  private static String replaceBarWithSpace (String s)
   {
    return s.replace('|',' ');
   }

  private String makeStructuredPasswordFromParams (String loginModel, String gameSecret, String passwordHash, boolean userDevSetHome)
   {
    String appVerInternal = platform.getAppVerInternal();
    String appVerExternal = platform.getAppVerExternal();

    if (appVerInternal == null)
     {
      appVerInternal = "";
     }

    if (appVerExternal == null)
     {
      appVerExternal = "";
     }

    return CLIENT_API_VERSION + "," +
           loginModel + "," +
           passwordHash + "," +
           gameSecret + "," +
           Integer.toString(platform.getDeviceType()) + "," +
           MNUtils.stringGetMD5String
            (platform.getUniqueDeviceIdentifier()) + "," +
           (userDevSetHome ? "1" : "0") + "," +
           replaceCommaWithDash(platform.getDeviceInfoString()) + "," +
           replaceCommaWithDash(launchId + "|" +
                                replaceBarWithSpace(appVerInternal) + "|" +
                                replaceBarWithSpace(appVerExternal));
   }

  private String makeNewGuestPassword ()
   {
    Random rng = new Random();

    return MNUtils.stringGetMD5String
            (platform.getUniqueDeviceIdentifier() +
             Long.toString(System.currentTimeMillis()) +
             Long.toString(System.nanoTime()) +
             Integer.toString(rng.nextInt()) +
             Integer.toString(rng.nextInt()));
   }

  static private String makeLaunchId (IMNPlatform platform)
   {
    StringBuilder builder = new StringBuilder();

    builder.append(platform.getUniqueDeviceIdentifier());
    builder.append(':');
    builder.append(Long.toString(System.currentTimeMillis() / 1000));
    builder.append(':');
    builder.append(Long.toString(System.nanoTime()));
    builder.append(':');
    builder.append(Integer.toString((new Random()).nextInt()));

    return MNUtils.stringGetMD5String(builder.toString());
   }

  private void dispatchUserChangedEvent (long userId)
   {
    eventHandlers.beginCall();

    try
     {
      int count = eventHandlers.size();

      for (int index = 0; index < count; index++)
       {
        eventHandlers.get(index).mnSessionUserChanged(userId);
       }
     }
    finally
     {
      eventHandlers.endCall();
     }
   }

 /**
  * Sends login request to MultiNet server using user login and password.
  * Event handlers {@link IMNSessionEventHandler#mnSessionStatusChanged mnSessionStatusChanged}
  * method will be called in case of successfull login. In case of login
  * failure event handlers {@link IMNSessionEventHandler#mnSessionErrorOccurred mnSessionErrorOccurred}
  * method will be called.
  *
  * @param login user login
  * @param password user password
  * @param saveCredentials boolean flag that specifies if user credentials must be stored
  * @return <code>true</code> if login procedure started successfully and
  * <code>false</code> if some error occured.
  */
  public boolean loginWithUserLoginAndPassword (String login, String password, boolean saveCredentials)
   {
    return loginWithUserLoginAndStructuredPassword
            (login,makeStructuredPasswordFromParams
                    (LOGIN_MODEL_LOGIN_PLUS_PASSWORD,
                     gameSecret,
                     MNUtils.stringGetMD5String(password),
                     saveCredentials));
   }

 /**
  * Sends login request to MultiNet server using user id and password hash.
  * Event handlers {@link IMNSessionEventHandler#mnSessionStatusChanged mnSessionStatusChanged}
  * method will be called in case of successfull login. In case of login
  * failure event handlers {@link IMNSessionEventHandler#mnSessionErrorOccurred mnSessionErrorOccurred}
  * method will be called.
  *
  * @param id user id
  * @param passwordHash password hash
  * @param saveCredentials boolean flag that specifies if user credentials must be stored
  * @return <code>true</code> if login procedure started successfully and
  * <code>false</code> if some error occured.
  */
  public boolean loginWithUserIdAndPasswordHash (long id, String passwordHash, boolean saveCredentials)
   {
    return loginWithUserLoginAndStructuredPassword
            (Long.toString(id),makeStructuredPasswordFromParams
                                (LOGIN_MODEL_ID_PLUS_PASSWORD_HASH,
                                 gameSecret,
                                 passwordHash,
                                 saveCredentials));
   }

 /**
  * Sends login request to MultiNet server using unique device identifier.
  * Event handlers {@link IMNSessionEventHandler#mnSessionStatusChanged mnSessionStatusChanged}
  * method will be called in case of successfull login. In case of login
  * failure event handlers {@link IMNSessionEventHandler#mnSessionErrorOccurred mnSessionErrorOccurred}
  * method will be called.
  *
  * @return <code>true</code> if login procedure started successfully and
  * <code>false</code> if some error occured.
  */
  public boolean loginWithDeviceCredentials ()
   {
    return loginWithUserLoginAndStructuredPassword
            (LOGIN_MODEL_GUEST_USER_LOGIN,
             makeStructuredPasswordFromParams
              (LOGIN_MODEL_GUEST,
               gameSecret,
               makeNewGuestPassword(),
               true));
   }

 /**
  * Sends login request to MultiNet server using authentication sign.
  * Event handlers {@link IMNSessionEventHandler#mnSessionStatusChanged mnSessionStatusChanged}
  * method will be called in case of successfull login. In case of login
  * failure event handlers {@link IMNSessionEventHandler#mnSessionErrorOccurred mnSessionErrorOccurred}
  * method will be called.
  *
  * @param id user id
  * @param authSign authentication sign
  * @return <code>true</code> if login procedure started successfully and
  * <code>false</code> if some error occured.
  */
  public boolean loginWithUserIdAndAuthSign (long id, String authSign)
   {
    return loginWithUserLoginAndStructuredPassword
            (Long.toString(id),makeStructuredPasswordFromParams
                                (LOGIN_MODEL_AUTH_SIGN,
                                 gameSecret,
                                 authSign,
                                 true));
   }

  private boolean registerLoginOffline (long id, String name, String authSign)
   {
    userId      = id;
    userName    = name;
    userSId     = null;
    lobbyRoomId = MNSession.MN_LOBBY_ROOM_ID_UNDEFINED;

    MNUserCredentials.updateCredentials(varStorage,new MNUserCredentials(id,name,authSign,new Date(),null));

    notifyDevUsersInfoChanged();

    dispatchUserChangedEvent(id);

    return true;
   }

 /**
  * Logins to MultiNet (offline) using authentication sign.
  * Event handlers {@link IMNSessionEventHandler#mnSessionUserChanged mnSessionUserChanged}
  * method will be called in case of successfull login. In case of login
  * failure event handlers {@link IMNSessionEventHandler#mnSessionErrorOccurred mnSessionErrorOccurred}
  * method will be called.
  *
  * @param id user id
  * @param authSign authentication sign
  * @return <code>true</code> if login procedure started successfully and
  * <code>false</code> if some error occured.
  */
  public boolean loginOfflineWithUserIdAndAuthSign (long id, String authSign)
   {
    if (status != MNConst.MN_OFFLINE)
     {
      notifyLoginFailed(MNI18n.getLocalizedString("Cannot login offline while connected to server",MNI18n.MESSAGE_CODE_OFFLINE_CANNOT_LOGIN_OFFLINE_WHILE_CONNECTED_TO_SERVER_ERROR));

      return false;
     }

    MNUserCredentials credentials = MNUserCredentials.getCredentialsByUserId(varStorage,id);

    if (credentials == null || !authSign.equals(credentials.userAuthSign))
     {
      notifyLoginFailed(MNI18n.getLocalizedString("Invalid login or password",MNI18n.MESSAGE_CODE_OFFLINE_INVALID_AUTH_SIGN_ERROR));

      return false;
     }

    return registerLoginOffline(userId,credentials.userName,authSign);
   }

 /**
  * Signup to MultiNet (offline).
  * Event handlers {@link IMNSessionEventHandler#mnSessionUserChanged mnSessionUserChanged}
  * method will be called in case of successfull signup/login. In case of signup/login
  * failure event handlers {@link IMNSessionEventHandler#mnSessionErrorOccurred mnSessionErrorOccurred}
  * method will be called.
  *
  * @return <code>true</code> if login procedure started successfully and
  * <code>false</code> if some error occured.
  */

  public boolean signupOffline ()
   {
    if (status != MNConst.MN_OFFLINE)
     {
      notifyLoginFailed(MNI18n.getLocalizedString("Cannot login offline while connected to server",MNI18n.MESSAGE_CODE_OFFLINE_CANNOT_LOGIN_OFFLINE_WHILE_CONNECTED_TO_SERVER_ERROR));

      return false;
     }

    long currentTime = MNUtils.getUnixTime();
    long id          = -currentTime;

    while (MNUserCredentials.getCredentialsByUserId(varStorage,id) != null)
     {
      id++;
     }

    return registerLoginOffline
            (id,"Guest_" + Long.toString(currentTime),
             "TMP_" + Long.toString(currentTime) + Long.toString(id));
   }

 /**
  * Sends login request to MultiNet server last used user account, or as a guest if there is no stored login info.
  * Event handlers {@link IMNSessionEventHandler#mnSessionStatusChanged mnSessionStatusChanged}
  * method will be called in case of successfull login. In case of login
  * failure event handlers {@link IMNSessionEventHandler#mnSessionErrorOccurred mnSessionErrorOccurred}
  * method will be called.
  *
  * @return <code>true</code> if login procedure started successfully and
  * <code>false</code> if some error occured.
  */
  public boolean loginAuto ()
   {
    MNUserCredentials lastUserCredentials = MNUserCredentials.getMostRecentlyLoggedUserCredentials(varStorage);

    if (lastUserCredentials != null)
     {
      return loginWithUserIdAndAuthSign
              (lastUserCredentials.userId,lastUserCredentials.userAuthSign);
     }
    else
     {
      return loginWithDeviceCredentials();
     }
   }

  private boolean loginWithUserLoginAndStructuredPassword (String login, String structuredPassword)
   {
    if (status != MNConst.MN_OFFLINE)
     {
      if (OFFLINE_MODE_DISABLED)
       {
        logout();
       }
      else
       {
        smartFoxFacade.logout();
       }
     }

    eventHandlers.beginCall();

    try
     {
      int count = eventHandlers.size();

      for (int index = 0; index < count; index++)
       {
        eventHandlers.get(index).mnSessionLoginInitiated();
       }
     }
    finally
     {
      eventHandlers.endCall();
     }

    String zone = GAME_ZONE_NAME_PREFIX + Integer.toString(gameId);

    setNewStatus(MNConst.MN_CONNECTING);

    smartFoxFacade.login(zone,login,structuredPassword);

    return true;
   }

  /**
   * Terminates MultiNet session.
   */
  public void logout ()
   {
    logoutAndWipeUserCredentialsByMode(MN_CREDENTIALS_WIPE_NONE);
   }

  /**
   * Terminates MultiNet session and (optionaly) remove stored user credentials.
   *
   * @param wipeMode mode of saved credentials removal (one of the
   * <code>MN_CREDENTIALS_WIPE_NONE</code>, <code>MN_CREDENTIALS_WIPE_USER</code>
   * or <code>MN_CREDENTIALS_WIPE_ALL</code>)
   */
  public void logoutAndWipeUserCredentialsByMode (int wipeMode)
   {
    if      (wipeMode == MN_CREDENTIALS_WIPE_ALL)
     {
      MNUserCredentials.wipeAllCredentials(varStorage);

      varStorage.removeVariablesByMask(PERSISTENT_VAR_USER_ALL_USERS_MASK);
     }
    else if (wipeMode == MN_CREDENTIALS_WIPE_USER)
     {
      if (isUserLoggedIn())
       {
        MNUserCredentials.wipeCredentialsByUserId(varStorage,userId);

        varStorage.removeVariablesByMask
         (String.format(PERSISTENT_VAR_USER_SINGLE_USER_MASK_FORMAT,userId));
       }
     }

    notifyDevUsersInfoChanged();

    if (wipeMode != 0)
     {
      if (socNetSessionFB.isConnected())
       {
        socNetSessionFB.logout();
       }
     }

    userSId     = null;
    lobbyRoomId = MNSession.MN_LOBBY_ROOM_ID_UNDEFINED;

    if (status != MNConst.MN_OFFLINE)
     {
      setNewStatus(MNConst.MN_OFFLINE);

      smartFoxFacade.logout();
     }

    if (userId != MNConst.MN_USER_ID_UNDEFINED)
     {
      userId   = MNConst.MN_USER_ID_UNDEFINED;
      userName = null;

      dispatchUserChangedEvent(userId);
     }

    varStorage.writeToFile(VAR_STORAGE_FILE_NAME);
   }

  /**
   * Terminates MultiNet session and releases all acquired resources.
   */
  public synchronized void shutdown ()
   {
    getTrackingSystem().trackShutdown(this);

    logout();

    eventHandlers.clearAll();

    varStorage.writeToFile(VAR_STORAGE_FILE_NAME);

    offlinePack.shutdown();
   }

  /**
   * Checks if relogin is possible (it is possible if one of the login... methods was called previously).
   *
   * @return <code>true</code> if one of the <code>login...</code> methods was
   * called previously and <code>false</code> otherwise.
   */
  public boolean isReLoginPossible ()
   {
    return smartFoxFacade.haveLoginInfo();
   }

  /**
   * Sends login requests to MultiNet server using parameters previously passed to <code>login...</code> method.
   */
  public void reLogin ()
   {
    if (isReLoginPossible())
     {
      smartFoxFacade.relogin();
     }
   }

  /**
   * Returns MultiNet server connection status.
   *
   * @return <code>true</code> if user is connected to MultiNet server and
   * <code>false</code> otherwise.
   */
  public boolean isOnline ()
   {
    return status != MNConst.MN_OFFLINE && status != MNConst.MN_CONNECTING;
   }

  /**
   * Returns user login status.
   *
   * @return <code>true</code> if user is logged in and <code>false</code>
   * otherwise.
   */
  public boolean isUserLoggedIn ()
   {
    return userId != MNConst.MN_USER_ID_UNDEFINED;
   }

  /**
   * Returns current session's game id.
   *
   * @return game id
   */
  public int getGameId ()
   {
    return gameId;
   }

  /**
   * Returns status of MultiNet session.
   *
   * @return one of the <code>MN_OFFLINE</code>, <code>MN_CONNECTING</code>,
   * <code>MN_LOGGEDIN</code>, <code>MN_IN_GAME_WAIT</code>,
   * <code>MN_IN_GAME_START</code>, <code>MN_IN_GAME_PLAY</code>,
   * <code>MN_IN_GAME_END</code>.
   */
  public int getStatus ()
   {
    return status;
   }

  /**
   * Returns <code>true</code> if user is in the game room and <code>false</code> otherwise.
   *
   * @return <code>true</code> if user is in the game room and <code>false</code> otherwise.
   */
  public boolean isInGameRoom ()
   {
    return status == MNConst.MN_IN_GAME_WAIT || status == MNConst.MN_IN_GAME_START ||
           status == MNConst.MN_IN_GAME_PLAY || status == MNConst.MN_IN_GAME_END;
   }

  /**
   * Returns user name.
   *
   * @return user name if user is logged in or null otherwise.
   */
  public String getMyUserName ()
   {
    if (isUserLoggedIn())
     {
      return userName;
     }
    else
     {
      return null;
     }
   }

  /**
   * Returns MultiNet user id.
   *
   * @return MultiNet user id if user is logged in or
   * <code>MNConst.MN_USER_ID_UNDEFINED</code> otherwise.
   */
  public long getMyUserId ()
   {
    if (isUserLoggedIn())
     {
      return userId;
     }
    else
     {
      return MNConst.MN_USER_ID_UNDEFINED;
     }
   }

  /**
   * Returns SmartFox user id.
   *
   * @return SmartFox user id if user is logged in or
   * <code>MNConst.MN_USER_SFID_UNDEFINED</code> otherwise.
   */
  public int getMyUserSFId ()
   {
    if (smartFoxFacade.isLoggedIn())
     {
      return smartFoxFacade.smartFox.myUserId;
     }
    else
     {
      return MNConst.MN_USER_SFID_UNDEFINED;
     }
   }

  /**
   * Returns user information.
   *
   * @return <code>MNUserInfo</code> instance which contains information
   * about current user.
   */
  public MNUserInfo getMyUserInfo ()
   {
    if (isUserLoggedIn())
     {
      return new MNUserInfo(getMyUserId(),getMyUserSFId(),getMyUserName(),webServerUrl);
     }
    else
     {
      return null;
     }
   }

  /**
   * Returns current room SmartFox id.
   *
   * @return current room SmartFox id.
   */
  public int getCurrentRoomId ()
   {
    if (smartFoxFacade.isLoggedIn())
     {
      return smartFoxFacade.smartFox.activeRoomId;
     }
    else
     {
      return MNSession.MN_ROOM_ID_UNDEFINED;
     }
   }

  public String getWebServerURL ()
   {
    if (smartFoxFacade.configData.isLoaded())
     {
      return webServerUrl;
     }
    else
     {
      smartFoxFacade.loadConfig();

      return null;
     }
   }

  public String getWebFrontURL ()
   {
    if (OFFLINE_MODE_DISABLED || offlinePack.isPackUnavailable())
     {
      return getWebServerURL();
     }

    String startPageUrl = offlinePack.getStartPageUrl();

    if (!smartFoxFacade.configData.isLoaded())
     {
      smartFoxFacade.loadConfig();
     }

    return startPageUrl;
   }

  private MNUserInfo createUserInfoBySFIdAndName (int userSFId, String structuredName)
   {
    MNUtils.UserNameComponents nameInfo = MNUtils.parseMNUserName(structuredName);

    if (nameInfo == null)
     {
      return new MNUserInfo(MNConst.MN_USER_ID_UNDEFINED,
                            userSFId,
                            structuredName,
                            webServerUrl);
     }
    else
     {
      return new MNUserInfo(nameInfo.userId,userSFId,nameInfo.userName,webServerUrl);
     }
   }

  /**
   * Returns list of users in current room.
   *
   * @return array of {@link MNUserInfo} objects describing users located in
   * the same room with current user. Empty array is returned if user is not
   * logged in.
   */
  public MNUserInfo[] getRoomUserList ()
   {
    if (!isOnline())
     {
      return new MNUserInfo[0];
     }

    ArrayList<MNUserInfo> userList = new ArrayList<MNUserInfo>();

    Room currentRoom = smartFoxFacade.smartFox.getRoom
                        (smartFoxFacade.smartFox.activeRoomId);

    if (currentRoom == null)
     {
      return new MNUserInfo[0];
     }

    java.util.Map<java.lang.Integer,User> sfUserList = currentRoom.getUserList();

    for (User sfUser : sfUserList.values())
     {
      userList.add(createUserInfoBySFIdAndName(sfUser.getId(),sfUser.getName()));
     }

    return userList.toArray(new MNUserInfo[userList.size()]);
   }

  /**
   * Returns information on user by user's SmartFox id
   *
   * @param sfId SmartFox user id
   * @return MNUserInfo object or null if there is no such user in current room
   */
   public MNUserInfo getUserInfoBySFId (int sfId)
    {
     if (!isOnline())
      {
       return null;
      }

     Room currentRoom = smartFoxFacade.smartFox.getRoom
                         (smartFoxFacade.smartFox.activeRoomId);

     if (currentRoom == null)
      {
       return null;
      }

     User sfUser = currentRoom.getUser(sfId);

     if (sfUser == null)
      {
       return null;
      }

     MNUtils.UserNameComponents nameInfo = MNUtils.parseMNUserName(sfUser.getName());

     if (nameInfo == null)
      {
       return null;
      }

     return new MNUserInfo(nameInfo.userId,sfId,nameInfo.userName,webServerUrl);
    }

  /**
   * Returns user status in game room.
   *
   * @return one of the <code>MN_USER_PLAYER</code>, <code>MN_USER_CHATER</code>,
   * or <code>MN_USER_STATUS_UNDEFINED</code>.
   */
  public int getRoomUserStatus ()
   {
    return isOnline() ? userStatus : MNConst.MN_USER_STATUS_UNDEFINED;
   }

  /**
   * Returns game settings id of current room.
   *
   * @return game settings id of current game room, or zero if player is not
   * in game room, or id is not set for this room.
   */
  public int getRoomGameSetId ()
   {
    int gameSetId = 0;

    if (isInGameRoom())
     {
      Room activeRoom = smartFoxFacade.smartFox.getActiveRoom();

      if (activeRoom != null)
       {
        SFSVariable gameSetIdVar = activeRoom.getVariable(SF_GAME_ROOM_VAR_NAME_GAMESET_ID);

        if (gameSetIdVar != null)
         {
          Integer gameSetIdValue = MNUtils.parseInteger(gameSetIdVar.getValue());

          if (gameSetIdValue != null)
           {
            gameSetId = gameSetIdValue;
           }
         }
       }
     }

    return gameSetId;
   }

  /**
   * Returns current session id.
   *
   * @return current session id or <code>null</code> if user is not logged in.
   */
  public String getMySId ()
   {
    if (status != MNConst.MN_OFFLINE)
     {
      return userSId;
     }
    else
     {
      return null;
     }
   }

  /**
   * Sends application custom "beacon".
   * Beacons are used for application actions usage statistic.
   *
   * @param actionName name of the action
   * @param beaconData "beacon" data
   */
  public void sendAppBeacon (String actionName, String beaconData)
   {
    getTrackingSystem().sendBeacon(actionName,beaconData,this);
   }

  /**
   * Executes application command
   * @param name command name
   * @param param command parameter
   */
  public void execAppCommand(String name, String param)
   {
    if (name.startsWith(APP_COMMAND_SET_APP_PROPERTY_PREFIX))
     {
      String varName = APP_PROPERTY_VAR_PATH_PREFIX +
                        name.substring(APP_COMMAND_SET_APP_PROPERTY_PREFIX_LEN);

      if (param == null)
       {
        varStorage.removeVariablesByMask(varName);
       }
      else
       {
        varStorage.setValue(varName,param);
       }
     }

    eventHandlers.beginCall();

    try
     {
      int count = eventHandlers.size();

      for (int index = 0; index < count; index++)
       {
        eventHandlers.get(index).mnSessionExecAppCommandReceived(name,param);
       }
     }
    finally
     {
      eventHandlers.endCall();
     }
   }

  /**
   * Executes application command (initiated by UI or some other external subsystem)
   * @param name command name
   * @param param command parameter
   */
  public void execUICommand(String name, String param)
   {
    eventHandlers.beginCall();

    try
     {
      int count = eventHandlers.size();

      for (int index = 0; index < count; index++)
       {
        eventHandlers.get(index).mnSessionExecUICommandReceived(name,param);
       }
     }
    finally
     {
      eventHandlers.endCall();
     }
   }

  private static final int EVENT_SOURCE_SYSTEM = 0;
  private static final int EVENT_SOURCE_WEB    = 1;

  private void postEventLow (int eventSource, String eventName, String eventParam, String callbackId)
   {
    eventHandlers.beginCall();

    try
     {
      int count = eventHandlers.size();

      for (int index = 0; index < count; index++)
       {
        if (eventSource == EVENT_SOURCE_WEB)
         {
          eventHandlers.get(index).mnSessionWebEventReceived(eventName,eventParam,callbackId);
         }
        else
         {
          eventHandlers.get(index).mnSessionSysEventReceived(eventName,eventParam,callbackId);
         }
       }
     }
    finally
     {
      eventHandlers.endCall();
     }
   }

  /**
   * Processes web-event (initiated by UI or some other external subsystem)
   * @param eventName event name
   * @param eventParam event-specific parameter
   * @param callbackId request identifier (optional, may be null)
   */
  public void processWebEvent(String eventName, String eventParam, String callbackId)
   {
    postEventLow(EVENT_SOURCE_WEB,eventName,eventParam,callbackId);
   }

  /**
   * Posts sys-event
   * @param eventName event name
   * @param eventParameter event-specific parameter
   * @param callbackId request identifier (optional, may be null)
   */
  public void postSysEvent (String eventName, String eventParam, String callbackId)
   {
    postEventLow(EVENT_SOURCE_SYSTEM,eventName,eventParam,callbackId);
   }

  /**
   * Notifies that apphost call has been received and allows to override default apphost call processing
   * @return true if event handler processed a call and default handling
   * should not be called, false - if event handler did not process a call
   */
  public boolean preprocessAppHostCall (MNAppHostCallInfo appHostCallInfo)
   {
    String cmdName = appHostCallInfo.getCommandName();

    if (cmdName.equals(MNAppHostCallInfo.CommandVarSave)         ||
        cmdName.equals(MNAppHostCallInfo.CommandVarsClear)       ||
        cmdName.equals(MNAppHostCallInfo.CommandVarsGet)         ||
        cmdName.equals(MNAppHostCallInfo.CommandSetHostParam)    ||
        cmdName.equals(MNAppHostCallInfo.CommandSendHttpRequest) ||
        cmdName.equals(MNAppHostCallInfo.CommandAddSourceDomain) ||
        cmdName.equals(MNAppHostCallInfo.CommandRemoveSourceDomain))
     {
      return false;
     }

    boolean result = false;

    eventHandlers.beginCall();

    try
     {
      int count = eventHandlers.size();

      for (int index = 0; index < count; index++)
       {
        result = result || eventHandlers.get(index).mnSessionAppHostCallReceived(appHostCallInfo);
       }
     }
    finally
     {
      eventHandlers.endCall();
     }

    return result;
   }

  /**
   * Allows MultiNet framework to process start intent parameters
   * @param intent intent that started activity
   */
  public void handleApplicationIntent (Intent intent)
   {
    String param = MNLauncherTools.getLaunchParam(intent);

    if (param != null)
     {
      launchParam = param;

      eventHandlers.beginCall();

      try
       {
        int count = eventHandlers.size();

        for (int index = 0; index < count; index++)
         {
          eventHandlers.get(index).mnSessionAppStartParamUpdated(param);
         }
       }
      finally
       {
        eventHandlers.endCall();
       }
     }
   }

  /**
   * Returns launch parameter set by MultiNet framework
   *
   * @return launch parameter value, or null if there is no such parameter in
   * start intent
   */
  public String getApplicationStartParam ()
   {
    return launchParam;
   }

  /**
   * Sends private message.
   *
   * @param message a message text to be sent
   * @param toUserSFId receiver smartFox user id
   */
  public void sendPrivateMessage (String message, int toUserSFId)
   {
    if (isOnline())
     {
      smartFoxFacade.smartFox.sendPrivateMessage(message,toUserSFId);
     }
   }

  /**
   * Sends public message.
   *
   * @param message a message text to be sent
   */
  public void sendChatMessage (String message)
   {
    if (isOnline())
     {
      smartFoxFacade.smartFox.sendPublicMessage(message);
     }
   }

  public void sendMultiNetXtMessage (String cmd, SFSObject params)
   {
    smartFoxFacade.smartFox.sendXtMessage(SMARTFOX_EXT_NAME,cmd,params);
   }

  /**
   * Sends game message to room.
   *
   * @param message a message to be sent
   */
  public void sendGameMessage (String message) throws MNException
   {
    if (isInGameRoom())
     {
      String[] params = {
                         EXTCMD_SEND_GAME_MESSAGE_RAW_PREFIX +
                         MNUtils.stringEscapeSimple
                          (message,
                           smartFoxFacade.smartFox.getRawProtocolSeparator(),
                           GAME_MESSAGE_ESCAPE_CHAR)
                        };

      smartFoxFacade.smartFox.sendXtMessage
       (SMARTFOX_EXT_NAME,
        SF_EXTCMD_SEND_GAME_MESSAGE,
        params,
        smartFoxFacade.smartFox.activeRoomId);
     }
   }

  /**
   * Sends plugin message.
   *
   * @param pluginName plugin name
   * @param message a message to be sent
   */
  public void sendPluginMessage (String pluginName, String message) throws MNException
   {
    if (isOnline())
     {
      String escapedPluginName = MNUtils.stringEscapeCharSimple
                                  (MNUtils.stringEscapeSimple
                                    (pluginName,
                                     smartFoxFacade.smartFox.getRawProtocolSeparator(),
                                     GAME_MESSAGE_ESCAPE_CHAR),
                                   PLUGIN_MESSAGE_PLUGIN_NAME_TERM_CHAR,
                                   GAME_MESSAGE_ESCAPE_CHAR);

      String escapedMessage = MNUtils.stringEscapeCharSimple
                                  (MNUtils.stringEscapeSimple
                                    (message,
                                     smartFoxFacade.smartFox.getRawProtocolSeparator(),
                                     GAME_MESSAGE_ESCAPE_CHAR),
                                   PLUGIN_MESSAGE_PLUGIN_NAME_TERM_CHAR,
                                   GAME_MESSAGE_ESCAPE_CHAR);

      String[] params = {
                         EXTCMD_SEND_PLUGIN_MESSAGE_RAW_PREFIX +
                         escapedPluginName +
                         PLUGIN_MESSAGE_PLUGIN_NAME_TERM_CHAR +
                         escapedMessage
                        };

      smartFoxFacade.smartFox.sendXtMessage
       (SMARTFOX_EXT_NAME,
        SF_EXTCMD_SEND_PLUGIN_MESSAGE,
        params,
        smartFoxFacade.smartFox.activeRoomId);
     }
   }

  /**
   * Sends a request to join buddy room.
   * Event handlers {@link IMNSessionEventHandler#mnSessionStatusChanged mnSessionStatusChanged}
   * method will be called if user successfully joined buddy room. In case of
   * error, event handlers {@link IMNSessionEventHandler#mnSessionErrorOccurred mnSessionErrorOccurred}
   * method will be called.
   *
   * @param roomSFId a SmartFox room id to join to
   *
   * @deprecated This method is deprecated. {@link MNSession#sendJoinRoomInvitationResponse}
   * should be used instead.
   */
  public void reqJoinBuddyRoom (int roomSFId)
   {
    if (isOnline())
     {
      SFSObject params = new SFSObject();

      params.put(SF_EXTCMD_JOIN_BUDDY_ROOM_PARAM_ROOM_SFID,
                 Integer.toString(roomSFId));

      sendMultiNetXtMessage(SF_EXTCMD_JOIN_BUDDY_ROOM,params);
     }
   }

  /**
   * Sends a response to "join room" invitation.
   *
   * @param invitationParams an invitation parameters.
   * @param accept true if invite should be accepted, false if invite should be rejected.
   */
  public void sendJoinRoomInvitationResponse (MNJoinRoomInvitationParams invitationParams,
                                              boolean                    accept)
   {
    if (isOnline())
     {
      if (accept)
       {
        reqJoinBuddyRoom(invitationParams.roomSFId);
       }
      else
       {
        sendPrivateMessage("\tInvite reject for room:" + Integer.toString(invitationParams.roomSFId),
                           invitationParams.fromUserSFId);
       }
     }
   }

  /**
   * Sends a request to join random room with choosen gameset id.
   * Event handlers {@link IMNSessionEventHandler#mnSessionStatusChanged mnSessionStatusChanged}
   * method will be called if user successfully joined the room. In case of
   * error, event handlers {@link IMNSessionEventHandler#mnSessionErrorOccurred mnSessionErrorOccurred}
   * method will be called.
   *
   * @param gameSetId choosen gameset id
   */
  public void reqJoinRandomRoom (String gameSetId)
   {
    if (isOnline())
     {
      SFSObject params = new SFSObject();

      params.put(SF_EXTCMD_PARAM_GAMESET_ID,gameSetId);

      sendMultiNetXtMessage(SF_EXTCMD_JOIN_RANDOM_ROOM,params);
     }
   }

  /**
   * Sends a request to create game room and send invitation to buddies to join the game.
   * Event handlers {@link IMNSessionEventHandler#mnSessionStatusChanged mnSessionStatusChanged}
   * method will be called if room have been created successfully. In case of
   * error, event handlers {@link IMNSessionEventHandler#mnSessionErrorOccurred mnSessionErrorOccurred}
   * method will be called.
   *
   * @param buddyRoomParams parameters of room to be created, invitation text and buddy list
   * invitations must be sent to.
   * @see MNBuddyRoomParams.
   */
  public void reqCreateBuddyRoom (MNBuddyRoomParams buddyRoomParams)
   {
    if (isOnline())
     {
      SFSObject params = new SFSObject();

      params.put(SF_EXTCMD_CREATE_BUDDY_ROOM_PARAM_ROOM_NAME,buddyRoomParams.roomName);
      params.put(SF_EXTCMD_PARAM_GAMESET_ID,Integer.toString(buddyRoomParams.gameSetId));
      params.put(SF_EXTCMD_CREATE_BUDDY_ROOM_PARAM_TO_USERID_LIST,buddyRoomParams.toUserIdList);
      params.put(SF_EXTCMD_CREATE_BUDDY_ROOM_PARAM_TO_USERSFID_LIST,buddyRoomParams.toUserSFIdList);
      params.put(SF_EXTCMD_CREATE_BUDDY_ROOM_PARAM_MSG_TEXT,buddyRoomParams.inviteText);

      sendMultiNetXtMessage(SF_EXTCMD_CREATE_BUDDY_ROOM,params);
     }
   }

  /**
   * Sends a request to start game in previously created buddy room.
   */
  public void reqStartBuddyRoomGame ()
   {
    if (status == MNConst.MN_IN_GAME_WAIT)
     {
      sendMultiNetXtMessage(SF_EXTCMD_START_BUDDY_ROOM_GAME,new SFSObject());
     }
    else
     {
      String      errorMessage = MNI18n.getLocalizedString
                                  ("Room not ready",
                                    MNI18n.MESSAGE_CODE_ROOM_IS_NOT_READY_TO_START_A_GAME_ERROR);
      MNErrorInfo errorInfo    = new MNErrorInfo(MNErrorInfo.ACTION_CODE_START_BUDDY_ROOM_GAME,errorMessage);

      eventHandlers.beginCall();

      try
       {
        int count = eventHandlers.size();

        for (int index = 0; index < count; index++)
         {
          eventHandlers.get(index).mnSessionErrorOccurred(errorInfo);
         }
       }
      finally
       {
        eventHandlers.endCall();
       }
     }
   }

  /**
   * Sends a request to stop game in game room.
   */
  public void reqStopRoomGame ()
   {
    if (status == MNConst.MN_IN_GAME_PLAY)
     {
      sendMultiNetXtMessage(SF_EXTCMD_STOP_ROOM_GAME,new SFSObject());
     }
   }

  /**
   * Sends a request to get updated game results from MultiNet server (asynchronous).
   * Event handlers {@link IMNSessionEventHandler#mnSessionCurrGameResultsReceived mnSessionCurrGameResultsReceived}
   * method will be called when results will be received.
   */
  public void reqCurrentGameResults ()
   {
    if (isInGameRoom())
     {
      sendMultiNetXtMessage(SF_EXTCMD_CURR_GAME_RESULTS,new SFSObject());
     }
   }

  /**
   * Sends a request to change user status.
   * Event handlers {@link IMNSessionEventHandler#mnSessionErrorOccurred mnSessionErrorOccurred}
   * method will be called if user status can not be changed.
   *
   * @param userStatus new user status (must be <code>MN_USER_PLAYER</code> or
   * <code>MN_USER_CHATER</code>)
   */
  public void reqSetUserStatus (int userStatus)
   {
    if (isInGameRoom())
     {
      if (userStatus == MNConst.MN_USER_PLAYER || userStatus == MNConst.MN_USER_CHATER)
       {
        SFSObject params = new SFSObject();

        params.put(SF_EXTCMD_SET_USER_STATUS_PARAM_USER_STATUS,Integer.toString(userStatus));

        sendMultiNetXtMessage(SF_EXTCMD_SET_USER_STATUS,params);
       }
      else
       {
        String      errorMessage = MNI18n.getLocalizedString
                                    ("invalid player status value",
                                     MNI18n.MESSAGE_CODE_INVALID_PLAYER_STATUS_VALUE_ERROR);
        MNErrorInfo errorInfo    = new MNErrorInfo(MNErrorInfo.ACTION_CODE_SET_USER_STATUS,errorMessage);

        eventHandlers.beginCall();

        try
         {
          int count = eventHandlers.size();

          for (int index = 0; index < count; index++)
           {
            eventHandlers.get(index).mnSessionErrorOccurred(errorInfo);
           }
         }
        finally
         {
          eventHandlers.endCall();
         }
       }
     }
   }

  /**
   * Invokes {@link IMNSessionEventHandler#mnSessionDoStartGameWithParams mnSessionDoStartGameWithParams} event handler's method.
   * It is the responsibility of event handlers to start game logic.
   *
   * @param gameParams the parameters of game to be started
   * @see MNGameParams
   */
  public void startGameWithParams (MNGameParams gameParams)
   {
    eventHandlers.beginCall();

    try
     {
      int count = eventHandlers.size();

      for (int index = 0; index < count; index++)
       {
        eventHandlers.get(index).mnSessionDoStartGameWithParams(gameParams);
       }
     }
    finally
     {
      eventHandlers.endCall();
     }
   }

  private void startGameWithParamsFromActiveRoom ()
   {
    Room activeRoom = smartFoxFacade.smartFox.getActiveRoom();
    SFSVariable gameSetIdVar = activeRoom.getVariable(SF_GAME_ROOM_VAR_NAME_GAMESET_ID);
    SFSVariable gameSetParamsVar = activeRoom.getVariable(SF_GAME_ROOM_VAR_NAME_GAMESET_PARAM);
    SFSVariable gameSeedVar = activeRoom.getVariable(SF_GAME_ROOM_VAR_NAME_GAME_SEED);

    if (gameSetIdVar != null && gameSetParamsVar != null && gameSeedVar != null)
     {
      Integer gameSetId     = MNUtils.parseInteger(gameSetIdVar.getValue());
      String  gameSetParams = gameSetParamsVar.getValue();
      Integer gameSeed      = MNUtils.parseInteger(gameSeedVar.getValue());

      if (gameSetId != null && gameSetParams != null && gameSeed != null)
       {
        MNGameParams gameParams =
         new MNGameParams(gameSetId,gameSetParams,"",
                          gameSeed,MNGameParams.MN_PLAYMODEL_MULTIPLAY);

        Map<String,SFSVariable> roomVars = activeRoom.getVariables();

        String prefix = SF_GAMESET_PLAY_PARAM_VAR_NAME_PREFIX;
        int    prefixLen = prefix.length();

        for (String varName : roomVars.keySet())
         {
          if (varName.startsWith(prefix))
           {
            gameParams.addGameSetPlayParam(varName.substring(prefixLen),
                                           roomVars.get(varName).getValue());
           }
         }

        startGameWithParams(gameParams);
       }
      else
       {
        //Log("MN_gameset_id, MN_gameset_param, MN_game_seed variable(s) have invalid type during game start");
       }
     }
    else
     {
      //Log("MN_gameset_id, MN_gameset_param, MN_game_seed variable(s) not set during game start");
     }
   }

  /**
   * Sends finished game score to MultiNet server.
   * Event handlers {@link IMNSessionEventHandler#mnSessionErrorOccurred mnSessionErrorOccurred}
   * method will be called if score sending failed.
   *
   * @param gameResult MNGameResuls object to be sent to server
   * @see MNGameResult
   */
  public void finishGameWithResult (MNGameResult gameResult)
   {
    if (isUserLoggedIn())
     {
      if (isOnline())
       {
        SFSObject params = new SFSObject();

        if ((status == MNConst.MN_IN_GAME_PLAY || status == MNConst.MN_IN_GAME_END) &&
            (userStatus == MNConst.MN_USER_PLAYER))
         {
          params.put(SF_EXTCMD_FINISH_PARAM_SCORE,
                     Long.toString(gameResult.score));
          params.put(SF_EXTCMD_FINISH_PARAM_OUT_TIME,"-1");

          sendMultiNetXtMessage(SF_EXTCMD_FINISH_GAME_IN_ROOM,params);
         }
        else
         {
          params.put(SF_EXTCMD_FINISH_PARAM_SCORE,
                     Long.toString(gameResult.score));
          params.put(SF_EXTCMD_FINISH_PARAM_OUT_TIME,"-1");
          params.put(SF_EXTCMD_FINISH_PARAM_SCORE_POSTLINK_ID,
                     gameResult.scorePostLinkId);
          params.put(SF_EXTCMD_PARAM_GAMESET_ID,
                     Integer.toString(gameResult.gameSetId));

          sendMultiNetXtMessage(SF_EXTCMD_FINISH_GAME_PLAIN,params);
         }
       }
      else
       {
        MNOfflineScores.saveScore
         (varStorage,userId,gameResult.gameSetId,gameResult.score);
       }
     }

    eventHandlers.beginCall();

    try
     {
      int count = eventHandlers.size();

      for (int index = 0; index < count; index++)
       {
        eventHandlers.get(index).mnSessionGameFinishedWithResult(gameResult);
       }
     }
    finally
     {
      eventHandlers.endCall();
     }
   }

  /**
   * Schedules sending game score to MultiNet server.
   * Score will be posted as soon as player login to MultiNet. Sending game
   * score can be canceled using cancelPostScoreOnLogin method.
   * @param gameResult MNGameResuls object to be sent to server
   * @see MNGameResult
   */
  public void schedulePostScoreOnLogin (MNGameResult gameResult)
   {
    pendingGameResult = gameResult;
   }

  /**
   * Cancels scheduled game score sending.
   */
  public void cancelPostScoreOnLogin ()
   {
    pendingGameResult = null;
   }

  /**
   * Cancels game on user request.
   * @param gameParams the parameters of game been canceled
   * @see MNGameParams
   */
  public void cancelGameWithParams (MNGameParams gameParams)
   {
    if (isInGameRoom() && userStatus == MNConst.MN_USER_PLAYER)
     {
      reqSetUserStatus(MNConst.MN_USER_CHATER);
     }
   }

  /**
   * Sets default game settings id
   * @param gameSetId game settings id
   */
  public void setDefaultGameSetId (int gameSetId)
   {
    defaultGameSetId = gameSetId;

    eventHandlers.beginCall();

    try
     {
      int count = eventHandlers.size();

      for (int index = 0; index < count; index++)
       {
        eventHandlers.get(index).mnSessionDefaultGameSetIdChangedTo(gameSetId);
       }
     }
    finally
     {
      eventHandlers.endCall();
     }
   }

  /**
   * Returns default game settings id
   * @return default game settings id
   */
  public int getDefaultGameSetId ()
   {
    return defaultGameSetId;
   }

  /**
   * Leaves current room
   */
  public void leaveRoom ()
   {
    if (isInGameRoom())
     {
      sendMultiNetXtMessage(SF_EXTCMD_LEAVE_ROOM,new SFSObject());
     }
   }

  /**
   * Adds event handler
   *
   * @param eventHandler an object that implements
   * {@link IMNSessionEventHandler IMNSessionEventHandler} interface
   */
  public synchronized void addEventHandler (IMNSessionEventHandler eventHandler)
   {
    eventHandlers.add(eventHandler);
   }

  /**
   * Removes event handler
   *
   * @param eventHandler an object that implements
   * {@link IMNSessionEventHandler IMNSessionEventHandler} interface
   */
  public synchronized void removeEventHandler (IMNSessionEventHandler eventHandler)
   {
    eventHandlers.remove(eventHandler);
   }

  private void setNewStatus (int newStatus)
   {
    /*
    if (newStatus == status)
     {
      return;
     }
    */

    int oldStatus = status;

    status = newStatus;

    eventHandlers.beginCall();

    try
     {
      int count = eventHandlers.size();

      for (int index = 0; index < count; index++)
       {
        eventHandlers.get(index).mnSessionStatusChanged(newStatus,oldStatus);
       }
     }
    finally
     {
      eventHandlers.endCall();
     }
   }

  public SmartFoxClient getSmartFox ()
   {
    return smartFoxFacade.smartFox;
   }

  /* IMNSmartFoxFacadeEventHandler methods */

  public void onPreLoginSucceeded (long userId, String userName, String SId, int lobbyRoomId, String userAuthSign)
   {
    /*FIXME: disabling smartFox to prevent blueBox connection after disconnect */
    /*        smartFox bug ?                                                   */
    smartFoxFacade.smartFox.smartConnect = false;

    boolean userChanged = this.userId != userId;
    MNUtils.UserNameComponents nameInfo = MNUtils.parseMNUserName(userName);

    this.userId = userId;

    if (nameInfo != null)
     {
      this.userName = nameInfo.userName;
     }
    else
     {
      this.userName = userName;
     }

    this.userSId  = SId;

    this.lobbyRoomId = lobbyRoomId;

    if (userAuthSign != null && userAuthSign.length() > 0)
     {
      if (smartFoxFacade.getLoginInfoLogin().equals
           (LOGIN_MODEL_GUEST_USER_LOGIN))
       {
        smartFoxFacade.updateLoginInfo
         (Long.toString(userId),
          makeStructuredPasswordFromParams
           (LOGIN_MODEL_AUTH_SIGN,gameSecret,userAuthSign,true));
       }

      MNUserCredentials.updateCredentials
       (varStorage,new MNUserCredentials(userId,this.userName,userAuthSign,new Date(),null));

      notifyDevUsersInfoChanged();

      varStorage.writeToFile(VAR_STORAGE_FILE_NAME);
     }

    if (userChanged)
     {
      dispatchUserChangedEvent(userId);
     }
   }

  public void onLoginSucceeded ()
   {
    setNewStatus(MNConst.MN_LOGGEDIN);
   }

  private void notifyLoginFailed (String error)
   {
    MNErrorInfo errorInfo = new MNErrorInfo(MNErrorInfo.ACTION_CODE_LOGIN,error);

    eventHandlers.beginCall();

    try
     {
      int count = eventHandlers.size();

      for (int index = 0; index < count; index++)
       {
        eventHandlers.get(index).mnSessionErrorOccurred(errorInfo);
       }
     }
    finally
     {
      eventHandlers.endCall();
     }
   }

  public void onLoginFailed (String error)
   {
    setNewStatus(MNConst.MN_OFFLINE);

    notifyLoginFailed(error);
   }

  public void onConnectionLost ()
   {
    userSId     = null;
    lobbyRoomId = MNSession.MN_LOBBY_ROOM_ID_UNDEFINED;

    if (status != MNConst.MN_OFFLINE)
     {
      setNewStatus(MNConst.MN_OFFLINE);
     }

    if (OFFLINE_MODE_DISABLED)
     {
      if (userId != MNConst.MN_USER_ID_UNDEFINED)
       {
        userId   = MNConst.MN_USER_ID_UNDEFINED;
        userName = null;

        dispatchUserChangedEvent(userId);
       }
     }
   }

  public void onConfigLoadStarted ()
   {
    eventHandlers.callHandlers(new MNEventHandlerArray.ICaller<IMNSessionEventHandler>()
     {
      public void callHandler (IMNSessionEventHandler handler)
       {
        handler.mnSessionConfigLoadStarted();
       }
     });
   }

  public void onConfigLoaded ()
   {
    MNTrackingSystem trackingSystem = getTrackingSystem();

    if (smartFoxFacade.configData.launchTrackerUrl != null)
     {
      trackingSystem.trackLaunchWithUrlTemplate(smartFoxFacade.configData.launchTrackerUrl,this);
     }

    if (smartFoxFacade.configData.shutdownTrackerUrl != null)
     {
      trackingSystem.setShutdownUrlTemplate(smartFoxFacade.configData.shutdownTrackerUrl,this);
     }

    if (smartFoxFacade.configData.beaconTrackerUrl != null)
     {
      trackingSystem.setBeaconUrlTemplate(smartFoxFacade.configData.beaconTrackerUrl,this);
     }

    webServerUrl = smartFoxFacade.configData.webServerUrl;

    if (OFFLINE_MODE_DISABLED || offlinePack.isPackUnavailable())
     {
      eventHandlers.beginCall();

      try
       {
        int count = eventHandlers.size();

        for (int index = 0; index < count; index++)
         {
          eventHandlers.get(index).mnSessionWebFrontURLReady(webServerUrl);
         }
       }
      finally
       {
        eventHandlers.endCall();
       }
     }

    if (!OFFLINE_MODE_DISABLED)
     {
      offlinePack.setWebServerUrl(webServerUrl);
     }

    fbAppId = smartFoxFacade.configData.facebookAppId;

    eventHandlers.beginCall();

    try
     {
      int count = eventHandlers.size();

      for (int index = 0; index < count; index++)
       {
        eventHandlers.get(index).mnSessionConfigLoaded();
       }
     }
    finally
     {
      eventHandlers.endCall();
     }
   }

  public void onConfigLoadFailed (String errorMessage)
   {
    eventHandlers.beginCall();

    try
     {
      int         count     = eventHandlers.size();
      MNErrorInfo errorInfo = new MNErrorInfo(MNErrorInfo.ACTION_CODE_LOAD_CONFIG,errorMessage);

      for (int index = 0; index < count; index++)
       {
        eventHandlers.get(index).mnSessionErrorOccurred(errorInfo);
       }
     }
    finally
     {
      eventHandlers.endCall();
     }
   }

  /* ISFSEventListener */

  private void handleSFOnMessage (final SFSEvent event, boolean isPrivate)
   {
    String message  = event.getParams().getString("message");
    User sfUserInfo = (User)event.getParams().get("sender");

    if (!isPrivate ||
        sfUserInfo == null ||
        sfUserInfo.getId() != smartFoxFacade.smartFox.myUserId)
     {
      MNUserInfo userInfo;

      if (sfUserInfo != null)
       {
        userInfo = createUserInfoBySFIdAndName(sfUserInfo.getId(),
                                               sfUserInfo.getName());
       }
      else
       {
        userInfo = new MNUserInfo(MNConst.MN_USER_ID_UNDEFINED,
                                  MNConst.MN_USER_SFID_UNDEFINED,
                                  "",
                                  webServerUrl);
       }

      MNChatMessage chatMessage = new MNChatMessage
                                   (userInfo,
                                    message == null ? "" : message,
                                    isPrivate);

      eventHandlers.beginCall();

      try
       {
        int count = eventHandlers.size();

        if (isPrivate)
         {
          for (int index = 0; index < count; index++)
           {
            eventHandlers.get(index).mnSessionChatPrivateMessageReceived(chatMessage);
           }
         }
        else
         {
          for (int index = 0; index < count; index++)
           {
            eventHandlers.get(index).mnSessionChatPublicMessageReceived(chatMessage);
           }
         }
       }
      finally
       {
        eventHandlers.endCall();
       }
     }
   }

  private Integer getUserVariableAsInteger (String name)
   {
    Room room = smartFoxFacade.smartFox.getActiveRoom();
    User user = room.getUser(smartFoxFacade.smartFox.myUserId);
    SFSVariable var = user.getVariable(name);

    if (var != null)
     {
      return MNUtils.parseInteger(var.getValue());
     }
    else
     {
      return null;
     }
   }

  private Integer getRoomUserStatusVariable ()
   {
    return getUserVariableAsInteger(SF_GAME_USER_VAR_NAME_USER_STATUS);
   }

  private void handleSFOnJoinRoom (final SFSEvent event)
   {
    boolean needStartGame = false;
    Room room = (Room)event.getParams().get("room");
    int oldUserStatus = userStatus;

    roomExtraInfoReceived = false;

    if (status == MNConst.MN_LOGGEDIN &&
        lobbyRoomId != MNSession.MN_LOBBY_ROOM_ID_UNDEFINED &&
        room.getId() != lobbyRoomId)
     {
      Integer newStatusVal  = MNUtils.parseInteger(room.getVariable(SF_GAME_ROOM_VAR_NAME_GAME_STATUS).getValue());
      Integer newUserStatus = getRoomUserStatusVariable();

      if (newUserStatus != null)
       {
        userStatus = newUserStatus.intValue();
       }

      if (newStatusVal != null)
       {
        int newStatus = newStatusVal.intValue();

        if (isStatusValid(newStatus))
         {
          setNewStatus(newStatus);

          if (newStatus  == MNConst.MN_IN_GAME_PLAY &&
              userStatus == MNConst.MN_USER_PLAYER &&
              roomExtraInfoReceived)
           {
            needStartGame = true;
           }
         }
       }
     }
    else if (lobbyRoomId != MNSession.MN_LOBBY_ROOM_ID_UNDEFINED &&
             room.getId() == lobbyRoomId)
     {
      setNewStatus(MNConst.MN_LOGGEDIN);

      if (pendingGameResult != null)
       {
        finishGameWithResult(pendingGameResult);

        pendingGameResult = null;
       }
     }

    if (userStatus != oldUserStatus)
     {
      eventHandlers.beginCall();

      try
       {
        int count = eventHandlers.size();

        for (int index = 0; index < count; index++)
         {
          eventHandlers.get(index).mnSessionRoomUserStatusChanged(userStatus);
         }
       }
      finally
       {
        eventHandlers.endCall();
       }
     }

    if (needStartGame)
     {
      startGameWithParamsFromActiveRoom();
     }
   }

  /*FIXME: remove warning suppression as soon as smartFox      */
  /* will allow to get changed variables list without warnings */
  @SuppressWarnings("unchecked")
  private void handleSFOnRoomVariablesUpdate (final SFSEvent event)
   {
    if (!isInGameRoom())
     {
      return;
     }

    boolean needStartGame  = false;
    boolean needFinishGame = false;
    boolean needCancelGame = false;

    Set<String> changedVarNames = (Set<String>)event.getParams().get("changedVars");
    Room room = (Room)event.getParams().get("room");

    for (String varName : changedVarNames)
     {
      if (varName.equals(SF_GAME_ROOM_VAR_NAME_GAME_STATUS))
       {
        Integer newStatusValue = MNUtils.parseInteger(room.getVariable(SF_GAME_ROOM_VAR_NAME_GAME_STATUS).getValue());

        if (newStatusValue != null && isStatusValid(newStatusValue.intValue()))
         {
          int newStatus = newStatusValue.intValue();

          if (userStatus == MNConst.MN_USER_PLAYER)
           {
            if (newStatus == MNConst.MN_IN_GAME_PLAY && roomExtraInfoReceived)
             {
              needStartGame = true;
             }
            else if (newStatus == MNConst.MN_IN_GAME_END)
             {
              needFinishGame = true;
             }
            else if (status == MNConst.MN_IN_GAME_PLAY &&
                     newStatus == MNConst.MN_IN_GAME_WAIT)
             {
              needCancelGame = true;
             }
           }

          setNewStatus(newStatus);
         }
        else
         {
          //Log("warning: invalid status in room variable have been ignored\n");
         }
       }
      else if (varName.equals(SF_GAME_ROOM_VAR_NAME_GAME_START_COUNTDOWN))
       {
        Integer secondsLeft= MNUtils.parseInteger(room.getVariable(SF_GAME_ROOM_VAR_NAME_GAME_START_COUNTDOWN).getValue());

        if (secondsLeft != null)
         {
          eventHandlers.beginCall();

          try
           {
            int count = eventHandlers.size();

            for (int index = 0; index < count; index++)
             {
              eventHandlers.get(index).mnSessionGameStartCountdownTick(secondsLeft.intValue());
             }
           }
          finally
           {
            eventHandlers.endCall();
           }
         }
       }
     }

    if      (needStartGame)
     {
      startGameWithParamsFromActiveRoom();
     }
    else if (needFinishGame)
     {
      eventHandlers.beginCall();

      try
       {
        int count = eventHandlers.size();

        for (int index = 0; index < count; index++)
         {
          eventHandlers.get(index).mnSessionDoFinishGame();
         }
       }
      finally
       {
        eventHandlers.endCall();
       }
     }
    else if (needCancelGame)
     {
      eventHandlers.beginCall();

      try
       {
        int count = eventHandlers.size();

        for (int index = 0; index < count; index++)
         {
          eventHandlers.get(index).mnSessionDoCancelGame();
         }
       }
      finally
       {
        eventHandlers.endCall();
       }
     }
   }

  /*FIXME: remove warning suppression as soon as smartFox      */
  /* will allow to get changed variables list without warnings */
  @SuppressWarnings("unchecked")
  private void handleSFOnUserVariablesUpdate (final SFSEvent event)
   {
    User user = (User)event.getParams().get("user");

    if (user.getId() == smartFoxFacade.smartFox.myUserId)
     {
      Set<String> changedVars = (Set<String>)event.getParams().get("changedVars");

      if (changedVars.contains(SF_GAME_USER_VAR_NAME_USER_STATUS))
       {
        int oldUserStatus = userStatus;
        Integer newUserStatus = getRoomUserStatusVariable();

        if (newUserStatus != null)
         {
          if (status == MNConst.MN_IN_GAME_PLAY)
           {
            if (userStatus == MNConst.MN_USER_CHATER &&
                newUserStatus == MNConst.MN_USER_PLAYER &&
                roomExtraInfoReceived)
             {
              startGameWithParamsFromActiveRoom();
             }
            else if (userStatus == MNConst.MN_USER_PLAYER &&
                     newUserStatus == MNConst.MN_USER_CHATER)
             {
              eventHandlers.beginCall();

              try
               {
                int count = eventHandlers.size();

                for (int index = 0; index < count; index++)
                 {
                  eventHandlers.get(index).mnSessionDoCancelGame();
                 }
               }
              finally
               {
                eventHandlers.endCall();
               }
             }
           }

          userStatus = newUserStatus;
         }
        else
         {
          userStatus = MNConst.MN_USER_STATUS_UNDEFINED;
         }

        if (oldUserStatus != userStatus)
         {
          eventHandlers.beginCall();

          try
           {
            int count = eventHandlers.size();

            for (int index = 0; index < count; index++)
             {
              eventHandlers.get(index).mnSessionRoomUserStatusChanged(userStatus);
             }
           }
          finally
           {
            eventHandlers.endCall();
           }
         }
       }
     }
   }

  private void handleSFOnUserEnterRoom (SFSEvent event)
   {
    User user = (User)event.getParams().get("user");
    MNUserInfo userInfo = createUserInfoBySFIdAndName(user.getId(),user.getName());

    eventHandlers.beginCall();

    try
     {
      int count = eventHandlers.size();

      for (int index = 0; index < count; index++)
       {
        eventHandlers.get(index).mnSessionRoomUserJoin(userInfo);
       }
     }
    finally
     {
      eventHandlers.endCall();
     }
   }

  private void handleSFOnUserLeaveRoom (SFSEvent event)
   {
    String userName = event.getParams().getString("userName");
    int userSFId = (Integer)event.getParams().get("userId");

    MNUserInfo userInfo = createUserInfoBySFIdAndName(userSFId,userName);

    eventHandlers.beginCall();

    try
     {
      int count = eventHandlers.size();

      for (int index = 0; index < count; index++)
       {
        eventHandlers.get(index).mnSessionRoomUserLeave(userInfo);
       }
     }
    finally
     {
      eventHandlers.endCall();
     }
   }

  private void handleCurrGameResultsResponse (SFSObject data)
   {
    long[] userIdArray    = MNUtils.parseCSLongs(data.getString(SF_EXTRSP_CURR_GAME_RESULTS_PARAM_USERID_LIST));
    int[] userSFIdArray   = MNUtils.parseCSIntegers(data.getString(SF_EXTRSP_CURR_GAME_RESULTS_PARAM_USERSFID_LIST));
    int[] placeArray      = MNUtils.parseCSIntegers(data.getString(SF_EXTRSP_CURR_GAME_RESULTS_PARAM_USER_PLACE_LIST));
    long[] scoreArray     = MNUtils.parseCSLongs(data.getString(SF_EXTRSP_CURR_GAME_RESULTS_PARAM_USER_SCORE_LIST));
    Integer resultIsFinal = MNUtils.parseInteger(data.getString(SF_EXTRSP_CURR_GAME_RESULTS_PARAM_RESULT_IS_FINAL));
    Integer gameId        = MNUtils.parseInteger(data.getString(SF_EXTRSP_CURR_GAME_RESULTS_PARAM_GAME_ID));
    Integer gameSetId     = MNUtils.parseInteger(data.getString(SF_EXTRSP_CURR_GAME_RESULTS_PARAM_GAMESET_ID));
    Long playRoundNumber  = MNUtils.parseLong(data.getString(SF_EXTRSP_CURR_GAME_RESULTS_PARAM_PLAYROUND_NUMBER));

    if (userIdArray != null && userSFIdArray != null && placeArray != null &&
        scoreArray != null && resultIsFinal != null && gameId != null &&
        gameSetId != null && playRoundNumber != null)
     {
      if (userIdArray.length == userSFIdArray.length &&
          userIdArray.length == placeArray.length &&
          userIdArray.length == scoreArray.length)
       {
        Room activeRoom    = smartFoxFacade.smartFox.getActiveRoom();
        MNUserInfo[] users = new MNUserInfo[userIdArray.length];

        for (int index = 0; index < userSFIdArray.length; index++)
         {
          User sfUser = activeRoom.getUser(userSFIdArray[index]);

          String name = null;

          if (sfUser != null)
           {
            MNUtils.UserNameComponents nameInfo = MNUtils.parseMNUserName(sfUser.getName());

            if (nameInfo != null)
             {
              name = nameInfo.userName;
             }
           }

          users[index] = new MNUserInfo(userIdArray[index],
                                        userSFIdArray[index],
                                        name,
                                        webServerUrl);
         }

        MNCurrGameResults results =
         new MNCurrGameResults(gameId,gameSetId,
                               resultIsFinal != 0 ? true : false,
                               playRoundNumber,
                               placeArray,scoreArray,users);

        eventHandlers.beginCall();

        try
         {
          int count = eventHandlers.size();

          for (int index = 0; index < count; index++)
           {
            eventHandlers.get(index).mnSessionCurrGameResultsReceived(results);
           }
         }
        finally
         {
          eventHandlers.endCall();
         }
       }
     }
   }

  private void handleJoinRoomInvitation (SFSObject data)
   {
    Integer fromUserSFId  = MNUtils.parseInteger(data.getString(SF_EXTCMD_JOIN_ROOM_INV_PARAM_FROM_USERSFID));
    String  fromUserName  = data.getString(SF_EXTCMD_JOIN_ROOM_INV_PARAM_FROM_USERNAME);
    Integer roomSFId      = MNUtils.parseInteger(data.getString(SF_EXTCMD_JOIN_ROOM_INV_PARAM_ROOM_SFID));
    String  roomName      = data.getString(SF_EXTCMD_JOIN_ROOM_INV_PARAM_ROOM_NAME);
    Integer roomGameId    = MNUtils.parseInteger(data.getString(SF_EXTCMD_JOIN_ROOM_INV_PARAM_ROOM_GAME_ID));
    Integer roomGameSetId = MNUtils.parseInteger(data.getString(SF_EXTCMD_JOIN_ROOM_INV_PARAM_ROOM_GAMESET_ID));
    String  messText      = data.getString(SF_EXTCMD_JOIN_ROOM_INV_PARAM_MSG_TEXT);

    if (fromUserSFId != null && fromUserName != null && roomSFId != null &&
        roomName != null && roomGameId != null && roomGameSetId != null &&
        messText != null)
     {
      MNJoinRoomInvitationParams joinRoomInvitationParams =
       new MNJoinRoomInvitationParams(fromUserSFId,fromUserName,roomSFId,
                                      roomName,roomGameId,roomGameSetId,
                                      messText);

      eventHandlers.beginCall();

      try
       {
        int count = eventHandlers.size();

        for (int index = 0; index < count; index++)
         {
          eventHandlers.get(index).mnSessionJoinRoomInvitationReceived(joinRoomInvitationParams);
         }
       }
      finally
       {
        eventHandlers.endCall();
       }
     }
   }

  private void handleInitRoomUserInfo ()
   {
    roomExtraInfoReceived = true;

    if (status     == MNConst.MN_IN_GAME_PLAY &&
        userStatus == MNConst.MN_USER_PLAYER)
     {
      startGameWithParamsFromActiveRoom();
     }
   }

  private void handleRawMessage (String[] messages)
   {
    for (String message : messages)
     {
      boolean isGameMessage   = false;
      boolean isPluginMessage = false;
      String  gameMessage     = null;

      if      (message.startsWith(EXTCMD_RECV_GAME_MESSAGE_RAW_PREFIX))
       {
        isGameMessage = true;
        gameMessage   = message.substring(EXTCMD_RECV_GAME_MESSAGE_RAW_PREFIX.length());
       }
      else if (message.startsWith(EXTCMD_RECV_GAME_MESSAGE_RAW_PREFIX2))
       {
        isGameMessage = true;
        gameMessage   = message.substring(EXTCMD_RECV_GAME_MESSAGE_RAW_PREFIX2.length());
       }
      else if (message.startsWith(EXTCMD_RECV_PLUGIN_MESSAGE_RAW_PREFIX))
       {
        isPluginMessage = true;
        gameMessage     = message.substring(EXTCMD_RECV_PLUGIN_MESSAGE_RAW_PREFIX.length());
       }
      else if (message.startsWith(EXTCMD_RECV_PLUGIN_MESSAGE_RAW_PREFIX2))
       {
        isPluginMessage = true;
        gameMessage     = message.substring(EXTCMD_RECV_PLUGIN_MESSAGE_RAW_PREFIX2.length());
       }

      MNUserInfo senderInfo  = null;
      boolean    validFormat = true;

      if (isGameMessage || isPluginMessage)
       {
        if (gameMessage.length() > 0)
         {
          char startChar = gameMessage.charAt(0);

          if      (startChar == '~') // there is no sender info
           {
            gameMessage = gameMessage.substring(1);
           }
          else if (startChar == '^') // sender info present in message
           {
            int endPos = gameMessage.indexOf('~',1);

            if (endPos > 0)
             {
              Integer senderSFId = MNUtils.parseInteger(gameMessage.substring(1,endPos));

              if (senderSFId != null)
               {
                senderInfo = createUserInfoBySFIdAndName
                              (senderSFId,
                               smartFoxFacade.getUserNameBySFId(senderSFId));

                gameMessage = gameMessage.substring(endPos + 1);
               }
              else
               {
                validFormat = false;
               }
             }
            else
             {
              validFormat = false;
             }
           }
          else
           {
            validFormat = false;
           }
         }
       }

      if (isGameMessage && validFormat)
       {
        try
         {
          gameMessage = MNUtils.stringUnEscapeSimple
                         (gameMessage,
                          smartFoxFacade.smartFox.getRawProtocolSeparator(),
                          GAME_MESSAGE_ESCAPE_CHAR);

          eventHandlers.beginCall();

          try
           {
            int count = eventHandlers.size();

            for (int index = 0; index < count; index++)
             {
              eventHandlers.get(index).mnSessionGameMessageReceived(gameMessage,senderInfo);
             }
           }
          finally
           {
            eventHandlers.endCall();
           }
         }
        catch (MNException e)
         {
          /* ignore message if it can not be unescaped */
         }
       }
      else if (isPluginMessage && validFormat)
       {
        int offset = gameMessage.indexOf((int)PLUGIN_MESSAGE_PLUGIN_NAME_TERM_CHAR);

        if (offset >= 0)
         {
          String pluginName = gameMessage.substring(0,offset);

          gameMessage = gameMessage.substring(offset + 1);

          try
           {
            pluginName = MNUtils.stringUnEscapeSimple
                          (MNUtils.stringUnEscapeCharSimple
                            (pluginName,
                             PLUGIN_MESSAGE_PLUGIN_NAME_TERM_CHAR,
                             GAME_MESSAGE_ESCAPE_CHAR),
                           smartFoxFacade.smartFox.getRawProtocolSeparator(),
                           GAME_MESSAGE_ESCAPE_CHAR);

            gameMessage = MNUtils.stringUnEscapeSimple
                           (MNUtils.stringUnEscapeCharSimple
                             (gameMessage,
                              PLUGIN_MESSAGE_PLUGIN_NAME_TERM_CHAR,
                              GAME_MESSAGE_ESCAPE_CHAR),
                            smartFoxFacade.smartFox.getRawProtocolSeparator(),
                            GAME_MESSAGE_ESCAPE_CHAR);

            eventHandlers.beginCall();

            try
             {
              int count = eventHandlers.size();

              for (int index = 0; index < count; index++)
               {
                eventHandlers.get(index).mnSessionPluginMessageReceived(pluginName,gameMessage,senderInfo);
               }
             }
            finally
             {
              eventHandlers.endCall();
             }
           }
          catch (MNException e)
           {
            /* ignore message if it can not be unescaped */
           }
         }
       }
     }
   }

  private void handleSFOnExtensionResponse (SFSEvent event)
   {
    String eventType = event.getParams().getString("type");

    if (eventType.equals(SmartFoxClient.XTMSG_TYPE_STR))
     {
      String[] params = (String[])event.getParams().get("dataObj");

      if (params != null)
       {
        handleRawMessage(params);
       }
     }
    else if (eventType.equals(SmartFoxClient.XTMSG_TYPE_XML))
     {
      SFSObject data = (SFSObject)event.getParams().get("dataObj");
      String    cmd  = data.getString("_cmd");

      if (cmd != null)
       {
        if      (cmd.equals(SF_EXTCMD_ERROR))
         {
          String errorCall = data.getString(SF_EXTCMD_ERROR_PARAM_CALL);
          boolean callHandler = true;
          int actionCode = -1;

          if (errorCall != null)
           {
            if (errorCall.equals(SF_EXTCMD_JOIN_RANDOM_ROOM) ||
                errorCall.equals(SF_EXTCMD_JOIN_BUDDY_ROOM))
             {
              actionCode = MNErrorInfo.ACTION_CODE_JOIN_GAME_ROOM;
             }
            else if (errorCall.equals(SF_EXTCMD_FINISH_GAME_IN_ROOM) ||
                     errorCall.equals(SF_EXTCMD_FINISH_GAME_PLAIN))
             {
              actionCode = MNErrorInfo.ACTION_CODE_POST_GAME_RESULT;
             }
            else if (errorCall.equals(SF_EXTCMD_CREATE_BUDDY_ROOM))
             {
              actionCode = MNErrorInfo.ACTION_CODE_CREATE_BUDDY_ROOM;
             }
            else if (errorCall.equals(SF_EXTCMD_LEAVE_ROOM))
             {
              actionCode = MNErrorInfo.ACTION_CODE_LEAVE_ROOM;
             }
            else if (errorCall.equals(SF_EXTCMD_START_BUDDY_ROOM_GAME))
             {
              actionCode = MNErrorInfo.ACTION_CODE_START_BUDDY_ROOM_GAME;
             }
            else if (errorCall.equals(SF_EXTCMD_STOP_ROOM_GAME))
             {
              actionCode = MNErrorInfo.ACTION_CODE_STOP_ROOM_GAME;
             }
            else if (errorCall.equals(SF_EXTCMD_SET_USER_STATUS))
             {
              actionCode = MNErrorInfo.ACTION_CODE_SET_USER_STATUS;
             }
            else
             {
              callHandler = false;
             }
           }
          else
           {
            callHandler = false;
           }

          if (callHandler)
           {
            String errorMessage = data.getString(SF_EXTCMD_ERROR_PARAM_ERROR_MSG);
            MNErrorInfo errorInfo = new MNErrorInfo(actionCode,errorMessage);

            eventHandlers.beginCall();

            try
             {
              int count = eventHandlers.size();

              for (int index = 0; index < count; index++)
               {
                eventHandlers.get(index).mnSessionErrorOccurred(errorInfo);
               }
             }
            finally
             {
              eventHandlers.endCall();
             }
           }
         }
        else if (cmd.equals(SF_EXTCMD_JOIN_ROOM_INVITATION))
         {
          handleJoinRoomInvitation(data);
         }
        else if (cmd.equals(SF_EXTCMD_CURR_GAME_RESULTS))
         {
          handleCurrGameResultsResponse(data);
         }
        else if (cmd.equals(SF_EXTCMD_INIT_ROOM_USER_INFO))
         {
          handleInitRoomUserInfo();
         }
       }
     }
   }

  public void handleEvent (final SFSEvent event)
   {
    String eventName = event.getName();

    if      (eventName.equals(SFSEvent.onPublicMessage))
     {
      handleSFOnMessage(event,false);
     }
    else if (eventName.equals(SFSEvent.onPrivateMessage))
     {
      handleSFOnMessage(event,true);
     }
    else if (eventName.equals(SFSEvent.onExtensionResponse))
     {
      handleSFOnExtensionResponse(event);
     }
    else if (eventName.equals(SFSEvent.onRoomVariablesUpdate))
     {
      handleSFOnRoomVariablesUpdate(event);
     }
    else if (eventName.equals(SFSEvent.onUserVariablesUpdate))
     {
      handleSFOnUserVariablesUpdate(event);
     }
    else if (eventName.equals(SFSEvent.onJoinRoom))
     {
      handleSFOnJoinRoom(event);
     }
    else if (eventName.equals(SFSEvent.onUserEnterRoom))
     {
      handleSFOnUserEnterRoom(event);
     }
    else if (eventName.equals(SFSEvent.onUserLeaveRoom))
     {
      handleSFOnUserLeaveRoom(event);
     }
    else
     {
      //Log("unexpected smartFox event has come (" + eventName + ")");
     }
   }

  private static boolean isStatusValid (int status)
   {
    return status == MNConst.MN_OFFLINE       || status == MNConst.MN_CONNECTING   ||
           status == MNConst.MN_LOGGEDIN      || status == MNConst.MN_IN_GAME_WAIT ||
           status == MNConst.MN_IN_GAME_START || status == MNConst.MN_IN_GAME_PLAY ||
           status == MNConst.MN_IN_GAME_END;
   }

  public String socNetFBConnect (SocNetFBEventHandler eventHandler)
   {
    return socNetFBConnect(eventHandler,null);
   }

  public String socNetFBConnect (SocNetFBEventHandler eventHandler,
                                 String[]             permissions)
   {
    if (status != MNConst.MN_OFFLINE && status != MNConst.MN_LOGGEDIN)
     {
      return MNI18n.getLocalizedString
              ("You must not be in the gameplay to use Facebook connect",
               MNI18n.MESSAGE_CODE_YOU_MUST_NOT_BE_IN_GAMEPLAY_TO_USE_FACEBOOK_CONNECT_ERROR);
     }

    if (fbAppId == null)
     {
      return MNI18n.getLocalizedString
              ("Facebook API key and/or session proxy URL is invalid or not set",
               MNI18n.MESSAGE_CODE_FACEBOOK_API_KEY_OR_SESSION_PROXY_URL_IS_INVALID_OR_NOT_SET_ERROR);
     }

    socNetSessionFBEventHandler = eventHandler;

    return socNetSessionFB.connect(fbAppId,permissions);
   }

  public String socNetFBResume  (SocNetFBEventHandler eventHandler)
   {
    if (status != MNConst.MN_OFFLINE && status != MNConst.MN_LOGGEDIN)
     {
      return MNI18n.getLocalizedString
              ("You must not be in the gameplay to use Facebook connect",
               MNI18n.MESSAGE_CODE_YOU_MUST_NOT_BE_IN_GAMEPLAY_TO_USE_FACEBOOK_CONNECT_ERROR);
     }

    if (fbAppId == null)
     {
      return MNI18n.getLocalizedString
              ("Facebook API key and/or session proxy URL is invalid or not set",
               MNI18n.MESSAGE_CODE_FACEBOOK_API_KEY_OR_SESSION_PROXY_URL_IS_INVALID_OR_NOT_SET_ERROR);
     }

    socNetSessionFBEventHandler = eventHandler;

    return socNetSessionFB.resume(fbAppId);
   }

  public void socNetFBLogout ()
   {
    socNetSessionFB.logout();
   }

  public MNSocNetSessionFB getSocNetSessionFB ()
   {
    return socNetSessionFB;
   }

  /**
   * Returns facebook object instance
   * Usage of returned object is restricted. Consult documentation for further information. In particular,
   * authorize and logout calls are not allowed.
   * @return facebook object instance
   */
  public Facebook getFBConnect ()
   {
    return socNetSessionFB.getFBConnect();
   }

  /* MNOfflinePack.IEventHandler implementation */

  public void onOfflinePackStartPageReady (String url)
   {
    eventHandlers.beginCall();

    try
     {
      int count = eventHandlers.size();

      for (int index = 0; index < count; index++)
       {
        eventHandlers.get(index).mnSessionWebFrontURLReady(url);
       }
     }
    finally
     {
      eventHandlers.endCall();
     }
   }

  public void onOfflinePackUnavailable    (String error)
   {
    if (webServerUrl != null)
     {
      onOfflinePackStartPageReady(webServerUrl);
     }
   }

  /* MNSocNetSessionFB.IEventHandler implementation */

  public void socNetFBLoginOk              (MNSocNetSessionFB session)
   {
    if (socNetSessionFBEventHandler != null)
     {
      socNetSessionFBEventHandler.socNetFBLoginOk(session);
     }

    socNetSessionFBEventHandler = null;
   }

  public void socNetFBLoginCanceled ()
   {
    if (socNetSessionFBEventHandler != null)
     {
      socNetSessionFBEventHandler.socNetFBLoginCancelled();
     }

    socNetSessionFBEventHandler = null;
   }

  public void socNetFBLoginFailedWithError (String            error)
   {
    if (socNetSessionFBEventHandler != null)
     {
      socNetSessionFBEventHandler.socNetFBLoginFailed
       (MNI18n.getLocalizedString(error,MNI18n.MESSAGE_CODE_FACEBOOK_LOGIN_ERROR));
     }

    socNetSessionFBEventHandler = null;
   }

  public void socNetFBLoggedOut            ()
   {
    eventHandlers.beginCall();

    try
     {
      int count = eventHandlers.size();

      for (int index = 0; index < count; index++)
       {
        eventHandlers.get(index).mnSessionSocNetLoggedOut(MNSocNetSessionFB.SOCNET_ID);
       }
     }
    finally
     {
      eventHandlers.endCall();
     }
   }

  public IMNPlatform getPlatform ()
   {
    return platform;
   }

  public void varStorageSetValue (String name, String value)
   {
    varStorage.setValue(name,value);

    notifyDevUsersInfoChanged();

    varStorage.writeToFile(VAR_STORAGE_FILE_NAME);
   }

  public String varStorageGetValueForVariable (String name)
   {
    return varStorage.getValue(name);
   }

  public Map<String,String> varStorageGetValuesByMasks (String[] masks)
   {
    return varStorage.getVariablesByMasks(masks);
   }

  public void varStorageRemoveVariablesByMask (String mask)
   {
    varStorage.removeVariablesByMask(mask);

    notifyDevUsersInfoChanged();

    varStorage.writeToFile(VAR_STORAGE_FILE_NAME);
   }

  public void varStorageRemoveVariablesByMasks (String[] masks)
   {
    varStorage.removeVariablesByMasks(masks);

    notifyDevUsersInfoChanged();

    varStorage.writeToFile(VAR_STORAGE_FILE_NAME);
   }

  public MNVarStorage getVarStorage ()
   {
    return varStorage;
   }

  interface SocNetFBEventHandler
   {
    void socNetFBLoginOk (MNSocNetSessionFB socNetSession);
    void socNetFBLoginFailed (String error);
    void socNetFBLoginCancelled ();
   }

  private void notifyDevUsersInfoChanged ()
   {
    eventHandlers.beginCall();

    try
     {
      int count = eventHandlers.size();

      for (int index = 0; index < count; index++)
       {
        eventHandlers.get(index).mnSessionDevUsersInfoChanged();
       }
     }
    finally
     {
      eventHandlers.endCall();
     }
   }

  /*package*/ long getLaunchTime ()
   {
    return launchTime;
   }

  /*package*/ String getLaunchId ()
   {
    return launchId;
   }

  /*package*/ synchronized MNTrackingSystem getTrackingSystem()
   {
    if (trackingSystem == null)
     {
      trackingSystem = new MNTrackingSystem(this);
     }

    return trackingSystem;
   }

  /*package*/ Map<String,String> getAppConfigVars ()
   {
    return appConfigVars;
   }

  /*package*/ MNConfigData getConfigData ()
   {
    return smartFoxFacade.configData;
   }

  /**
   * Returns game vocabulary object
   * @return game vocabulary object
   */
  public MNGameVocabulary getGameVocabulary ()
   {
    return gameVocabulary;
   }

  private int              gameId;
  private String           gameSecret;
  private IMNPlatform      platform;
  private MNSmartFoxFacade smartFoxFacade;
  private MNSocNetSessionFB socNetSessionFB;
  private SocNetFBEventHandler socNetSessionFBEventHandler;
  private MNVarStorage     varStorage;
  private String           webServerUrl;
  private String           fbAppId;

  private int              status;
  private long             userId;
  private String           userName;
  private String           userSId;
  private int              lobbyRoomId;
  private int              userStatus;
  private boolean          roomExtraInfoReceived;
  private int              defaultGameSetId;
  private MNGameResult     pendingGameResult;

  private MNEventHandlerArray<IMNSessionEventHandler> eventHandlers;

  private MNGameVocabulary gameVocabulary;
  private MNOfflinePack offlinePack;

  private String           launchParam;

  private long             launchTime;
  private String           launchId;

  private MNTrackingSystem trackingSystem;
  private HashMap<String,String> appConfigVars;

  public static final int  MN_LOBBY_ROOM_ID_UNDEFINED = -1;
  public static final int  MN_ROOM_ID_UNDEFINED       = -1;

  public static final int MN_CREDENTIALS_WIPE_NONE = 0;
  public static final int MN_CREDENTIALS_WIPE_USER = 1;
  public static final int MN_CREDENTIALS_WIPE_ALL  = 2;

  public static final String VAR_STORAGE_FILE_NAME = "mn_vars.dat";
  public static final String INSTALL_REFERRER_VAR_NAME = "app.install.referrer.params";

  public static final String PERSISTENT_VAR_USER_ALL_USERS_MASK = "user.*";
  public static final String PERSISTENT_VAR_USER_SINGLE_USER_MASK_FORMAT = "user.%d.*";

  private static final String APP_COMMAND_SET_APP_PROPERTY_PREFIX = "set";
  private static final String APP_PROPERTY_VAR_PATH_PREFIX = "prop.";
  private static final int APP_COMMAND_SET_APP_PROPERTY_PREFIX_LEN = APP_COMMAND_SET_APP_PROPERTY_PREFIX.length();

  public static final String CLIENT_API_VERSION = "1_4_5";

  private static final String GAME_ZONE_NAME_PREFIX = "Game_";
  private static final String SMARTFOX_EXT_NAME = "MultiNetExtension";
  private static final String SF_EXTCMD_JOIN_BUDDY_ROOM = "joinBuddyRoom";
  private static final String SF_EXTCMD_JOIN_RANDOM_ROOM = "joinRandomRoom";
  private static final String SF_EXTCMD_FINISH_GAME_IN_ROOM = "finishGameInRoom";
  private static final String SF_EXTCMD_FINISH_GAME_PLAIN = "finishGamePlain";
  private static final String SF_EXTCMD_CREATE_BUDDY_ROOM = "createBuddyRoom";
  private static final String SF_EXTCMD_START_BUDDY_ROOM_GAME = "startBuddyRoomGame";
  private static final String SF_EXTCMD_STOP_ROOM_GAME = "stopRoomGame";
  private static final String SF_EXTCMD_JOIN_ROOM_INVITATION = "joinRoomInvitation";
  private static final String SF_EXTCMD_CURR_GAME_RESULTS = "currGameResults";
  private static final String SF_EXTCMD_INIT_ROOM_USER_INFO = "initRoomUserInfo";
  private static final String SF_EXTCMD_LEAVE_ROOM = "leaveRoom";
  private static final String SF_EXTCMD_SET_USER_STATUS = "setUserStatus";
  private static final String SF_EXTCMD_SEND_GAME_MESSAGE = "sendRGM";
  private static final String SF_EXTCMD_SEND_PLUGIN_MESSAGE = "sendRPM";
  private static final String SF_EXTCMD_ERROR = "MN_error";

  private static final String SF_EXTCMD_PARAM_GAMESET_ID = "MN_gameset_id";

  private static final String SF_EXTCMD_JOIN_BUDDY_ROOM_PARAM_ROOM_SFID = "MN_room_sfid";

  private static final String SF_EXTCMD_FINISH_PARAM_SCORE = "MN_out_score";
  private static final String SF_EXTCMD_FINISH_PARAM_OUT_TIME = "MN_out_time";
  private static final String SF_EXTCMD_FINISH_PARAM_SCORE_POSTLINK_ID = "MN_game_scorepostlink_id";

  private static final String SF_EXTCMD_CREATE_BUDDY_ROOM_PARAM_ROOM_NAME = "MN_room_name";
  private static final String SF_EXTCMD_CREATE_BUDDY_ROOM_PARAM_TO_USERID_LIST = "MN_to_user_id_list";
  private static final String SF_EXTCMD_CREATE_BUDDY_ROOM_PARAM_TO_USERSFID_LIST = "MN_to_user_sfid_list";
  private static final String SF_EXTCMD_CREATE_BUDDY_ROOM_PARAM_MSG_TEXT = "MN_mess_text";

  private static final String SF_EXTCMD_SET_USER_STATUS_PARAM_USER_STATUS = "MN_user_status";

  private static final String SF_EXTRSP_CURR_GAME_RESULTS_PARAM_USERID_LIST = "MN_play_user_id_list";
  private static final String SF_EXTRSP_CURR_GAME_RESULTS_PARAM_USERSFID_LIST = "MN_play_user_sfid_list";
  private static final String SF_EXTRSP_CURR_GAME_RESULTS_PARAM_USER_PLACE_LIST = "MN_play_user_place_list";
  private static final String SF_EXTRSP_CURR_GAME_RESULTS_PARAM_USER_SCORE_LIST = "MN_play_user_score_list";
  private static final String SF_EXTRSP_CURR_GAME_RESULTS_PARAM_RESULT_IS_FINAL = "MN_play_result_is_final";
  private static final String SF_EXTRSP_CURR_GAME_RESULTS_PARAM_GAME_ID = "MN_game_id";
  private static final String SF_EXTRSP_CURR_GAME_RESULTS_PARAM_GAMESET_ID = "MN_gameset_id";
  private static final String SF_EXTRSP_CURR_GAME_RESULTS_PARAM_PLAYROUND_NUMBER = "MN_play_round_number";

  private static final String SF_EXTCMD_JOIN_ROOM_INV_PARAM_FROM_USERSFID = "MN_from_user_sfid";
  private static final String SF_EXTCMD_JOIN_ROOM_INV_PARAM_FROM_USERNAME = "MN_from_user_name";
  private static final String SF_EXTCMD_JOIN_ROOM_INV_PARAM_ROOM_SFID = "MN_room_sfid";
  private static final String SF_EXTCMD_JOIN_ROOM_INV_PARAM_ROOM_NAME = "MN_room_name";
  private static final String SF_EXTCMD_JOIN_ROOM_INV_PARAM_ROOM_GAME_ID = "MN_room_game_id";
  private static final String SF_EXTCMD_JOIN_ROOM_INV_PARAM_ROOM_GAMESET_ID = "MN_room_gameset_id";
  private static final String SF_EXTCMD_JOIN_ROOM_INV_PARAM_MSG_TEXT = "MN_mess_text";

  private static final String SF_EXTCMD_ERROR_PARAM_CALL = "MN_call";
  private static final String SF_EXTCMD_ERROR_PARAM_ERROR_MSG = "MN_err_msg";

  private static final String SF_GAME_ROOM_VAR_NAME_GAME_STATUS = "MN_game_status";
  private static final String SF_GAME_ROOM_VAR_NAME_GAMESET_ID = "MN_gameset_id";
  private static final String SF_GAME_ROOM_VAR_NAME_GAMESET_PARAM = "MN_gameset_param";
  private static final String SF_GAME_ROOM_VAR_NAME_GAME_START_COUNTDOWN = "MN_gamestart_countdown";
  private static final String SF_GAME_ROOM_VAR_NAME_GAME_SEED = "MN_game_seed";

  private static final String SF_GAME_USER_VAR_NAME_USER_STATUS = "MN_user_status";

  private static final String SF_GAMESET_PLAY_PARAM_VAR_NAME_PREFIX = "MN_gameset_play_param_";

  private static final String LOGIN_MODEL_LOGIN_PLUS_PASSWORD   = "L";
  private static final String LOGIN_MODEL_ID_PLUS_PASSWORD_HASH = "I";
  private static final String LOGIN_MODEL_GUEST                 = "G";
  private static final String LOGIN_MODEL_AUTH_SIGN             = "A";

  private static final String LOGIN_MODEL_GUEST_USER_LOGIN = "*";

  private static final String EXTCMD_RECV_GAME_MESSAGE_RAW_PREFIX  = "~MNRGM";
  private static final String EXTCMD_RECV_GAME_MESSAGE_RAW_PREFIX2 =
                               "~" + EXTCMD_RECV_GAME_MESSAGE_RAW_PREFIX;

  private static final String EXTCMD_SEND_GAME_MESSAGE_RAW_PREFIX =
                               EXTCMD_RECV_GAME_MESSAGE_RAW_PREFIX2 + "~";

  private static final char GAME_MESSAGE_ESCAPE_CHAR = '~';

  private static final String EXTCMD_RECV_PLUGIN_MESSAGE_RAW_PREFIX = "~MNRPM";
  private static final String EXTCMD_RECV_PLUGIN_MESSAGE_RAW_PREFIX2 =
                               "~" + EXTCMD_RECV_PLUGIN_MESSAGE_RAW_PREFIX;

  private static final String EXTCMD_SEND_PLUGIN_MESSAGE_RAW_PREFIX =
                               EXTCMD_RECV_PLUGIN_MESSAGE_RAW_PREFIX2 + "~";

  private static final char PLUGIN_MESSAGE_PLUGIN_NAME_TERM_CHAR = '^';

  private static final boolean OFFLINE_MODE_DISABLED = true;
 }

