//
//  MNPlayerListProvider.java
//  MultiNet client
//
//  Copyright 2010 PlayPhone. All rights reserved.
//

package com.playphone.multinet.providers;

import java.util.Hashtable;
import java.util.LinkedList;

import com.playphone.multinet.MNConst;
import com.playphone.multinet.MNUserInfo;
import com.playphone.multinet.core.MNSession;
import com.playphone.multinet.core.MNSessionEventHandlerAbstract;
import com.playphone.multinet.core.MNEventHandlerArray;

/**
 * A class representing "PlayerList" MultiNet provider.
 * "PlayerList" provider provides information on players state in game room
 * and notifies its client on states changes.
 */
public class MNPlayerListProvider
 {
  public static final String PROVIDER_NAME = "com.playphone.mn.psi";

  /**
   * Interface handling players state modification events.
   */
  public interface IEventHandler
   {
    /**
     * Invoked when new player joined the game.
     *
     * @param player player information
     */
    void onPlayerJoin (MNUserInfo player);

    /**
     * Invoked when player left the game.
     *
     * @param player player information
     */
    void onPlayerLeft (MNUserInfo player);
   }

  /**
   * A class which implements IEventHandler interface by ignoring all
   * received events.
   */
  public static class EventHandlerAbstract implements IEventHandler
   {
    public void onPlayerJoin (MNUserInfo player)
     {
     }

    public void onPlayerLeft (MNUserInfo player)
     {
     }
   }

  /**
   * Constructs a new <code>MNPlayerListProvider</code> object.
   *
   * @param session         MultiNet session instance
   */
  public MNPlayerListProvider (MNSession session)
   {
    sessionEventHandler = new SessionEventHandler(session);
   }

  /**
   * Returns a list of players in current game roon.
   *
   * @return list of <code>MNUserInfo</code> objects.
   */
  public MNUserInfo[] getPlayerList ()
   {
    return sessionEventHandler.getPlayerList();
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

  private static class SessionEventHandler extends MNSessionEventHandlerAbstract
   {
    public SessionEventHandler (MNSession session)
     {
      this.session       = session;
      this.eventHandlers = new MNEventHandlerArray<IEventHandler>();

      playerInfos = new Hashtable<Integer,PlayerInfo>();

      session.addEventHandler(this);
     }

    public synchronized MNUserInfo[] getPlayerList ()
     {
      LinkedList<MNUserInfo> playerList = new LinkedList<MNUserInfo>();

      for (Integer sfid : playerInfos.keySet())
       {
        PlayerInfo playerInfo = playerInfos.get(sfid);

        if (playerInfo.userStatus == MNConst.MN_USER_PLAYER)
         {
          playerList.add(playerInfo.userInfo);
         }
       }

      return playerList.toArray(new MNUserInfo[playerList.size()]);
     }

    public synchronized void shutdown ()
     {
      session.removeEventHandler(this);

      playerInfos.clear();
     }

    private void dispatchPlayerLeftEvent (MNUserInfo userInfo)
     {
      eventHandlers.beginCall();

      try
       {
        int count = eventHandlers.size();

        for (int index = 0; index < count; index++)
         {
          eventHandlers.get(index).onPlayerLeft(userInfo);
         }
       }
      finally
       {
        eventHandlers.endCall();
       }
     }

    private void dispatchPlayerJoinEvent (MNUserInfo userInfo)
     {
      eventHandlers.beginCall();

      try
       {
        int count = eventHandlers.size();

        for (int index = 0; index < count; index++)
         {
          eventHandlers.get(index).onPlayerJoin(userInfo);
         }
       }
      finally
       {
        eventHandlers.endCall();
       }
     }

    public synchronized void mnSessionRoomUserLeave (MNUserInfo userInfo)
     {
      PlayerInfo playerInfo = playerInfos.get(userInfo.userSFId);

      if (playerInfo != null)
       {
        if (playerInfo.userInfo.userId != session.getMyUserId())
         {
          Integer sfid = new Integer(userInfo.userSFId);

          if (playerInfo.userStatus == MNConst.MN_USER_PLAYER)
           {
            dispatchPlayerLeftEvent(playerInfo.userInfo);
           }

          playerInfos.remove(sfid);
         }
       }
     }

    private synchronized void processInitMessage (String message)
     {
      playerInfos.clear();

      String[]       entries        = message.split(";");
      SFIdStatusPair sfIdStatusPair = new SFIdStatusPair();

      for (String entry : entries)
       {
        if (SFIdStatusPair.parse(sfIdStatusPair,entry))
         {
          MNUserInfo userInfo = session.getUserInfoBySFId(sfIdStatusPair.sfId);

          if (userInfo != null)
           {
            PlayerInfo playerInfo = new PlayerInfo(userInfo,sfIdStatusPair.status);

            playerInfos.put(new Integer(sfIdStatusPair.sfId),playerInfo);
           }
         }
       }
     }

    private synchronized void processModifyMessage (String message)
     {
      SFIdStatusPair sfIdStatusPair = new SFIdStatusPair();

      if (SFIdStatusPair.parse(sfIdStatusPair,message))
       {
        Integer    sfIdKey    = new Integer(sfIdStatusPair.sfId);
        PlayerInfo playerInfo = playerInfos.get(sfIdKey);

        if (playerInfo != null)
         {
          int oldStatus = playerInfo.userStatus;

          playerInfo.userStatus = sfIdStatusPair.status;

          if (sfIdStatusPair.status == MNConst.MN_USER_PLAYER)
           {
            if (oldStatus != MNConst.MN_USER_PLAYER)
             {
              dispatchPlayerJoinEvent(playerInfo.userInfo);
             }
           }
          else if (oldStatus == MNConst.MN_USER_PLAYER)
           {
            dispatchPlayerLeftEvent(playerInfo.userInfo);
           }
         }
        else
         {
          MNUserInfo userInfo = session.getUserInfoBySFId(sfIdStatusPair.sfId);

          if (userInfo != null)
           {
            playerInfo = new PlayerInfo(userInfo,sfIdStatusPair.status);

            playerInfos.put(sfIdKey,playerInfo);

            if (sfIdStatusPair.status == MNConst.MN_USER_PLAYER)
             {
              dispatchPlayerJoinEvent(userInfo);
             }
           }
         }
       }
     }

    public void mnSessionPluginMessageReceived (String     pluginName,
                                                String     message,
                                                MNUserInfo sender)
     {
      if (pluginName.equals(PROVIDER_NAME))
       {
        if      (message.startsWith("i"))
         {
          processInitMessage(message.substring(1));
         }
        else if (message.startsWith("m"))
         {
          processModifyMessage(message.substring(1));
         }
       }
     }

    public synchronized void mnSessionStatusChanged (int newStatus, int oldStatus)
     {
      if (newStatus == MNConst.MN_OFFLINE ||
          newStatus == MNConst.MN_LOGGEDIN)
       {
        playerInfos.clear();
       }
     }

    private static class SFIdStatusPair
     {
      public int sfId;
      public int status;

      public SFIdStatusPair ()
       {
        sfId   = 0;
        status = 0;
       }

      public static boolean parse (SFIdStatusPair dest, String str)
       {
        int pos = str.indexOf(":");

        if (pos < 0)
         {
          return false;
         }

        try
         {
          dest.sfId   = Integer.parseInt(str.substring(0,pos));
          dest.status = Integer.parseInt(str.substring(pos + 1));

          return true;
         }
        catch (NumberFormatException e)
         {
          return false;
         }
       }
     }

    private static class PlayerInfo
     {
      public PlayerInfo (MNUserInfo userInfo, int userStatus)
       {
        this.userInfo   = userInfo;
        this.userStatus = userStatus;
       }

      public MNUserInfo userInfo;
      public int        userStatus;
     }

    public MNSession                          session;
    public MNEventHandlerArray<IEventHandler> eventHandlers;
    public Hashtable<Integer,PlayerInfo>      playerInfos;
   }

  private SessionEventHandler sessionEventHandler;
 }

