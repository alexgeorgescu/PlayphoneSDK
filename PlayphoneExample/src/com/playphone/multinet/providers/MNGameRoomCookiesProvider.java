//
//  MNGameRoomCookiesProvider.java
//  MultiNet client
//
//  Copyright 2011 PlayPhone. All rights reserved.
//


package com.playphone.multinet.providers;

import java.util.HashMap;

import it.gotoandplay.smartfoxclient.SmartFoxClient;
import it.gotoandplay.smartfoxclient.data.Room;
import it.gotoandplay.smartfoxclient.data.SFSVariable;
import it.gotoandplay.smartfoxclient.data.RoomVariableRequest;

import com.playphone.multinet.core.MNSession;
import com.playphone.multinet.core.MNSessionEventHandlerAbstract;
import com.playphone.multinet.core.MNEventHandlerArray;

import com.playphone.multinet.MNUserInfo;

/**
 * A class representing "game room cookies" MultiNet provider.
 * "game room cookies" provider provides ability to store and
 * retrieve custom game room information
 */
public class MNGameRoomCookiesProvider
 {
  public static final String PROVIDER_NAME = "com.playphone.mn.grc";

  /**
   * Interface handling game room cookies - related events.
   */
  public interface IEventHandler
   {
    /**
     * Invoked when game room cookie has been successfully retrieved.
     * @param roomSFId SmartFox room identifier
     * @param key cookie's key
     * @param cookie cookie's data
     */
    void onGameRoomCookieDownloadSucceeded (int roomSFId, int key, String cookie);

    /**
     * Invoked when game room cookie retrieval failed.
     * @param roomSFId SmartFox room identifier
     * @param key cookie's key
     * @param error error message
     */
    void onGameRoomCookieDownloadFailedWithError (int roomSFId, int key, String error);

    /**
     * Invoked when game room cookie was updated in current game room (not supported)
     * @param key cookie's key
     * @param newCookieValue new value of cookie
     */
    void onCurrentGameRoomCookieUpdated (int key, String newCookieValue);
   }

  /**
   * A class which implements IEventHandler interface by ignoring all
   * received events.
   */
  public static class EventHandlerAbstract implements IEventHandler
   {
    public void onGameRoomCookieDownloadSucceeded (int roomSFId, int key, String cookie)
     {
     }

    public void onGameRoomCookieDownloadFailedWithError (int roomSFId, int key, String error)
     {
     }

    public void onCurrentGameRoomCookieUpdated (int key, String newCookieValue)
     {
     }
   }

  /**
   * Constructs a new <code>MNGameRoomCookiesProvider</code> object.
   *
   * @param session MultiNet session instance
   */
  public MNGameRoomCookiesProvider (MNSession session)
   {
    sessionEventHandler = new SessionEventHandler(session);
   }

  /**
   * Stops provider and frees all allocated resources.
   */
  public synchronized void shutdown ()
   {
    sessionEventHandler.shutdown();
   }

  /**
   * Retrieves game room cookie
   * @param roomSFId SmartFox room identifier
   * @param key key of cookie to be retrieved
   */
  public void downloadGameRoomCookie (int roomSFId, int key)
   {
    sessionEventHandler.session.sendPluginMessage
     (PROVIDER_NAME,"g" + Integer.toString(roomSFId) +
                    ":" + Integer.toString(key) + ":" +
                    Integer.toString(REQUEST_NUMBER_API));
   }

  /**
   * Stores game room cookie for current room
   * @param key key of game cookie to be stored
   * @param cookie data to be stored
   */
  public void setCurrentGameRoomCookie (int key, String cookie)
   {
    if (key < COOKIE_MIN_KEY || key > COOKIE_MAX_KEY ||
        (cookie != null && cookie.length() > COOKIE_DATA_MAX_LENGTH))
     {
      logError("unable to set room cookie - invalid cookie");

      return;
     }

    SmartFoxClient smartFox = sessionEventHandler.session.getSmartFox();

    HashMap<String,RoomVariableRequest> vars = new HashMap<String,RoomVariableRequest>();

    RoomVariableRequest varRequest =
     new RoomVariableRequest(cookie,
                             cookie != null ? SFSVariable.TYPE_STRING
                                            : SFSVariable.TYPE_NULL,
                             false,true);

    vars.put(getVarNameByCookieKey(key),varRequest);

    smartFox.setRoomVariables(vars,smartFox.activeRoomId);
   }

  /**
   * Retrieves game room cookie for current room
   * @param roomSFId SmartFox room identifier
   * @param key key of cookie to be retrieved
   */
  public String getCurrentGameRoomCookie (int key)
   {
    Room activeRoom = sessionEventHandler.session.getSmartFox().getActiveRoom();

    if (activeRoom == null)
     {
      logError("unable to get room cookie - no active room");

      return null;
     }

    SFSVariable var = activeRoom.getVariable(getVarNameByCookieKey(key));

    return var != null ? var.getValue() : null;
   }

  private static String getVarNameByCookieKey (int key)
   {
    return "MN_RV_" + Integer.toString(key);
   }

  /**
   * Adds event handler
   *
   * @param eventHandler an object that implements
   * {@link IEventHandler IEventHandler} interface
   */
  public void addEventHandler (IEventHandler eventHandler)
   {
    sessionEventHandler.eventHandlers.add(eventHandler);
   }

  /**
   * Removes event handler
   *
   * @param eventHandler an object that implements
   * {@link IEventHandler IEventHandler} interface
   */
  public void removeEventHandler (IEventHandler eventHandler)
   {
    sessionEventHandler.eventHandlers.remove(eventHandler);
   }

  private class SessionEventHandler extends MNSessionEventHandlerAbstract
   {
    public SessionEventHandler (MNSession session)
     {
      this.session       = session;
      this.eventHandlers = new MNEventHandlerArray<IEventHandler>();

      session.addEventHandler(this);
     }

    public synchronized void shutdown ()
     {
      session.removeEventHandler(this);
      eventHandlers.clearAll();
     }

    private void handleGetResponse (String[] responseComponents)
     {
      if (responseComponents.length < 4)
       {
        return;
       }

      try
       {
        final int roomSFId = Integer.parseInt(responseComponents[0]);
        final int key      = Integer.parseInt(responseComponents[1]);

        //check if request number is valid
        int requestNumber = Integer.parseInt(responseComponents[2]);

        if (requestNumber != REQUEST_NUMBER_API)
         {
          return;
         }

        if      (responseComponents[3].equals(RESPONSE_STATUS_OK))
         {
          final String cookie = responseComponents.length < 5 ? null :
                                 responseComponents[4];

          eventHandlers.callHandlers(new MNEventHandlerArray.ICaller<IEventHandler>()
           {
            public void callHandler (IEventHandler handler)
             {
              handler.onGameRoomCookieDownloadSucceeded(roomSFId,key,cookie);
             }
           });
         }
        else if (responseComponents[3].equals(RESPONSE_STATUS_ERROR))
         {
          final String message = responseComponents.length < 5 ? "game room cookie retrieval failed" :
                                  responseComponents[4];

          eventHandlers.callHandlers(new MNEventHandlerArray.ICaller<IEventHandler>()
           {
            public void callHandler (IEventHandler handler)
             {
              handler.onGameRoomCookieDownloadFailedWithError(roomSFId,key,message);
             }
           });
         }
       }
      catch (NumberFormatException e)
       {
        return;
       }
     }

    public void mnSessionPluginMessageReceived (String     pluginName,
                                                String     message,
                                                MNUserInfo sender)
     {
      if (sender != null || !pluginName.equals(PROVIDER_NAME))
       {
        return;
       }

      if (message.length() == 0)
       {
        return;
       }

      char     cmd        = message.charAt(0);
      String[] components = message.substring(1).split(":",5);

      if (cmd == REQUEST_CMD_GET)
       {
        handleGetResponse(components);
       }
     }

    final MNSession                          session;
    final MNEventHandlerArray<IEventHandler> eventHandlers;

    private static final char   REQUEST_CMD_GET       = 'g';
    private static final String RESPONSE_STATUS_OK    = "s";
    private static final String RESPONSE_STATUS_ERROR = "e";
   }

  private void logError (String message)
   {
    sessionEventHandler.session.getPlatform().logWarning(getClass().getSimpleName(),message);
   }

  private static final int    REQUEST_NUMBER_API = 0;

  private static final int    COOKIE_MIN_KEY = 0;
  private static final int    COOKIE_MAX_KEY = 99;
  private static final int    COOKIE_DATA_MAX_LENGTH = 1024;

  private final SessionEventHandler sessionEventHandler;
 }

