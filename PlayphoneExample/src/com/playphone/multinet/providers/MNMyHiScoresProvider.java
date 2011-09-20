//
//  MNMyHiScoresProvider.java
//  MultiNet client
//
//  Copyright 2010 PlayPhone. All rights reserved.
//

package com.playphone.multinet.providers;

import java.util.Map;
import java.util.Hashtable;

import com.playphone.multinet.MNUserInfo;
import com.playphone.multinet.core.MNSession;
import com.playphone.multinet.core.MNSessionEventHandlerAbstract;
import com.playphone.multinet.core.MNEventHandlerArray;

/**
 * A class representing "MyHiScores" MultiNet provider.
 *
 * "MyHiScores" provider provides access to player's high scores information and allows application
 * to be notified when high scores updates occur.
 */
public class MNMyHiScoresProvider
 {
  public static final String PROVIDER_NAME = "com.playphone.mn.scorenote";

  public static final int MN_HS_PERIOD_MASK_ALLTIME = 0x0001;
  public static final int MN_HS_PERIOD_MASK_WEEK    = 0x0002;
  public static final int MN_HS_PERIOD_MASK_MONTH   = 0x0004;

  /**
   * Interface handling player's high score update events.
   */
  public interface IEventHandler
   {
    /**
     * Invoked when player's high score information has been updated.
     *
     * @param newScore updated score
     * @param gameSetId game settings id new score belongs to
     * @param periodMask bit mask which describes what kind of high scores
     *        has been updated, it is a combination of MN_HS_PERIOD_MASK_ALLTIME,
     *        MN_HS_PERIOD_MASK_MONTH and MN_HS_PERIOD_MASK_WEEK constants
     */
    void onNewHiScore (long newScore, int gameSetId, int periodMask);
   }

  /**
   * A class which implements IEventHandler interface by ignoring all received events.
   */
  public static class EventHandlerAbstract implements IEventHandler
   {
    public void onNewHiScore (long newScore, int gameSetId, int periodMask)
     {
     }
   }

  /**
   * Constructs a new <code>MNMyHiScoreProvider</code> object.
   *
   * @param session         MultiNet session instance
   */
  public MNMyHiScoresProvider (MNSession      session)
   {
    sessionEventHandler = new SessionEventHandler(session);
   }

  /**
   * Returns player's high score for given game settings id.
   *
   * @param gameSetId game settings id for which high score must be returned
   * @return player's high score or null if high score for given gameSetId does not exist
   */
  public synchronized Long getMyHiScore (int gameSetId)
   {
    return sessionEventHandler.scores.get(new Integer(gameSetId));
   }

  /**
   * Returns player's high scores for all game setting ids.
   *
   * @return "game setting id" - "high score" mapping
   */
  public synchronized Map<Integer,Long> getMyHiScores ()
   {
    Hashtable<Integer,Long> result = new Hashtable<Integer,Long>();

    for (Integer gameSetId : sessionEventHandler.scores.keySet())
     {
      result.put(gameSetId,sessionEventHandler.scores.get(gameSetId));
     }

    return result;
   }

  /**
   * Stops provider and frees all allocated resources.
   */
  public synchronized void shutdown ()
   {
    sessionEventHandler.shutdown();
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
    public SessionEventHandler (MNSession      session)
     {
      this.session       = session;
      this.eventHandlers = new MNEventHandlerArray<IEventHandler>();

      scores = new Hashtable<Integer,Long>();

      session.addEventHandler(this);
     }

    public synchronized void shutdown ()
     {
      session.removeEventHandler(this);
      session       = null;
      eventHandlers = null;
      scores        = null;
     }

    private synchronized void processInitMessage (String message)
     {
      scores.clear();

      String[] entries = message.split(";");

      for (String entry : entries)
       {
        String[] parts = entry.split(":");

        if (parts.length == 2)
         {
          try
           {
            Integer gameSetId = new Integer(parts[0]);
            Long    score     = new Long(parts[1]);

            scores.put(gameSetId,score);
           }
          catch (NumberFormatException e)
           {
           }
         }
       }
     }

    private synchronized void processModifyMessage (String message)
     {
      String[] parts = message.split(":");

      if (parts.length == 3)
       {
        try
         {
          Integer gameSetId = new Integer(parts[0]);
          Long    score     = new Long(parts[1]);
          String  periodStr = parts[2];

          int periodMask   = 0;
          int periodStrLen = periodStr.length();

          for (int index = 0; index < periodStrLen; index++)
           {
            char periodChar = periodStr.charAt(index);

            if      (periodChar == 'W')
             {
              periodMask |= MN_HS_PERIOD_MASK_WEEK;
             }
            else if (periodChar == 'M')
             {
              periodMask |= MN_HS_PERIOD_MASK_MONTH;
             }
            else if (periodChar == 'A')
             {
              periodMask |= MN_HS_PERIOD_MASK_ALLTIME;
             }
           }

          if ((periodMask & MN_HS_PERIOD_MASK_ALLTIME) != 0)
           {
            scores.put(gameSetId,score);
           }

          if (periodMask != 0)
           {
            eventHandlers.beginCall();

            try
             {
              int count = eventHandlers.size();

              for (int index = 0; index < count; index++)
               {
                eventHandlers.get(index).onNewHiScore(score,gameSetId,periodMask);
               }
             }
            finally
             {
              eventHandlers.endCall();
             }
           }
         }
        catch (NumberFormatException e)
         {
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

    public  Hashtable<Integer,Long>            scores;
    private MNSession                          session;
    private MNEventHandlerArray<IEventHandler> eventHandlers;
   }

  private SessionEventHandler sessionEventHandler;
 }
