//
//  MNGameCookiesProvider.java
//  MultiNet client
//
//  Copyright 2010 PlayPhone. All rights reserved.
//


package com.playphone.multinet.providers;

import com.playphone.multinet.core.MNSession;
import com.playphone.multinet.core.MNSessionEventHandlerAbstract;
import com.playphone.multinet.core.MNEventHandlerArray;
import com.playphone.multinet.MNUserInfo;

/**
 * A class representing "game cookies" MultiNet provider.
 * "game cookies" provider provides ability to store small pieces of information
 * per player on server and retrieve it later.
 */
public class MNGameCookiesProvider
 {
  public static final String PROVIDER_NAME = "com.playphone.mn.guc";

  /**
   * Interface handling game cookies - related events.
   */
  public interface IEventHandler
   {
    /**
     * Invoked when game cookie has been successfully retrieved from server.
     * @param key cookie's key
     * @param cookie cookie's data
     */
    void onGameCookieDownloadSucceeded (int key, String cookie);

    /**
     * Invoked when game cookie retrieval failed.
     * @param key cookie's key
     * @param error error message
     */
    void onGameCookieDownloadFailedWithError (int key, String error);

    /**
     * Invoked when game cookie has been successfully stored on server.
     * @param key cookie's key
     */
    void onGameCookieUploadSucceeded (int key);

    /**
     * Invoked when game cookie upload failed.
     * @param key cookie's key
     * @param error error message
     */
    void onGameCookieUploadFailedWithError (int key, String error);
   }

  /**
   * A class which implements IEventHandler interface by ignoring all
   * received events.
   */
  public static class EventHandlerAbstract implements IEventHandler
   {
    public void onGameCookieDownloadSucceeded (int key, String cookie)
     {
     }

    public void onGameCookieDownloadFailedWithError (int key, String error)
     {
     }

    public void onGameCookieUploadSucceeded (int key)
     {
     }

    public void onGameCookieUploadFailedWithError (int key, String error)
     {
     }
   }

  /**
   * Constructs a new <code>MNGameCookiesProvider</code> object.
   *
   * @param session         MultiNet session instance
   */
  public MNGameCookiesProvider (MNSession session)
   {
    sessionEventHandler = new SessionEventHandler(session);
   }

  /**
   * Stops provider and frees all allocated resources.
   */
  public synchronized void shutdown ()
   {
    sessionEventHandler.shutdown();

    sessionEventHandler = null;
   }

  /**
   * Retrieves game cookie for current player
   * @param key key of cookie to be retrieved
   */
  public void downloadUserCookie (int key)
   {
    sessionEventHandler.session.sendPluginMessage
     (PROVIDER_NAME,"g" + Integer.toString(key) + ":0");
   }

  /**
   * Stores game cookie for current player
   * @param key key of game cookie to be stored
   * @param cookie data to be stored
   */
  public void uploadUserCookie (int key, String cookie)
   {
    if (cookie != null)
     {
      if (cookie.length() <= SessionEventHandler.COOKIE_DATA_MAX_LENGTH)
       {
        sessionEventHandler.session.sendPluginMessage
         (PROVIDER_NAME,"p" + Integer.toString(key) + ":0:" + cookie);
       }
      else
       {
        sessionEventHandler.eventHandlers.beginCall();

        try
         {
          int count = sessionEventHandler.eventHandlers.size();

          for (int index = 0; index < count; index++)
           {
            sessionEventHandler.eventHandlers.get(index).onGameCookieUploadFailedWithError(key,"game cookie data length exceeds allowed limit");
           }
         }
        finally
         {
          sessionEventHandler.eventHandlers.endCall();
         }
       }
     }
    else
     {
      sessionEventHandler.session.sendPluginMessage
       (PROVIDER_NAME,"d" + Integer.toString(key) + ":0");
     }
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
    public SessionEventHandler (MNSession      session)
     {
      this.session       = session;
      this.eventHandlers = new MNEventHandlerArray<IEventHandler>();

      session.addEventHandler(this);
     }

    public synchronized void shutdown ()
     {
      session.removeEventHandler(this);

      session       = null;
      eventHandlers = null;
     }

    private void handleGetResponse (String[] responseComponents)
     {
      if (responseComponents.length < 3)
       {
        return;
       }

      int key           = 0;
      int requestNumber;

      try
       {
        key           = Integer.parseInt(responseComponents[0]);
        requestNumber = Integer.parseInt(responseComponents[1]);
       }
      catch (NumberFormatException e)
       {
        return;
       }

      if      (responseComponents[2].equals(RESPONSE_STATUS_OK))
       {
        String cookie = responseComponents.length < 4 ? null :
                                                        responseComponents[3];

        eventHandlers.beginCall();

        try
         {
          int count = eventHandlers.size();

          for (int index = 0; index < count; index++)
           {
            eventHandlers.get(index).onGameCookieDownloadSucceeded(key,cookie);
           }
         }
        finally
         {
          eventHandlers.endCall();
         }
       }
      else if (responseComponents[2].equals(RESPONSE_STATUS_ERROR))
       {
        String message = responseComponents.length < 4 ? null :
                                                         responseComponents[3];

        eventHandlers.beginCall();

        try
         {
          int count = eventHandlers.size();

          for (int index = 0; index < count; index++)
           {
            eventHandlers.get(index).onGameCookieDownloadFailedWithError(key,message);
           }
         }
        finally
         {
          eventHandlers.endCall();
         }
       }
     }

    private void handlePutResponse (String[] responseComponents)
     {
      if (responseComponents.length < 3)
       {
        return;
       }

      int key = 0;
      int requestNumber = 0;

      try
       {
        key           = Integer.parseInt(responseComponents[0]);
        requestNumber = Integer.parseInt(responseComponents[1]);
       }
      catch (NumberFormatException e)
       {
        return;
       }

      if (requestNumber != REQUEST_NUMBER_API)
       {
        return;
       }

      if      (responseComponents[2].equals(RESPONSE_STATUS_OK))
       {
        eventHandlers.beginCall();

        try
         {
          int count = eventHandlers.size();

          for (int index = 0; index < count; index++)
           {
            eventHandlers.get(index).onGameCookieUploadSucceeded(key);
           }
         }
        finally
         {
          eventHandlers.endCall();
         }
       }
      else if (responseComponents[2].equals(RESPONSE_STATUS_ERROR))
       {
        String message = responseComponents.length < 4 ? null :
                                                         responseComponents[3];

        eventHandlers.beginCall();

        try
         {
          int count = eventHandlers.size();

          for (int index = 0; index < count; index++)
           {
            eventHandlers.get(index).onGameCookieUploadFailedWithError(key,message);
           }
         }
        finally
         {
          eventHandlers.endCall();
         }
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
      String[] components = message.substring(1).split(":",4);

      if      (cmd == REQUEST_CMD_GET)
       {
        handleGetResponse(components);
       }
      else if (cmd == REQUEST_CMD_PUT || cmd == REQUEST_CMD_DEL)
       {
        handlePutResponse(components);
       }
     }

    MNSession                          session;
    MNEventHandlerArray<IEventHandler> eventHandlers;

    private static final char   REQUEST_CMD_GET       = 'g';
    private static final char   REQUEST_CMD_PUT       = 'p';
    private static final char   REQUEST_CMD_DEL       = 'd';
    private static final String RESPONSE_STATUS_OK    = "s";
    private static final String RESPONSE_STATUS_ERROR = "e";

    private static final int    REQUEST_NUMBER_API = 0;

    private static final int    COOKIE_DATA_MAX_LENGTH = 1024;
   }

  private SessionEventHandler sessionEventHandler;
 }

