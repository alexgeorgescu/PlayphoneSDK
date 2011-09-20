//
//  MNScoreProgressProvider.java
//  MultiNet client
//
//  Copyright 2009 PlayPhone. All rights reserved.
//

package com.playphone.multinet.providers;

import java.util.Hashtable;
import java.util.Comparator;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import com.playphone.multinet.MNUserInfo;
import com.playphone.multinet.core.MNSession;
import com.playphone.multinet.core.MNSessionEventHandlerAbstract;
import com.playphone.multinet.core.MNEventHandlerArray;

/**
 * A class representing "ScoreProgress" MultiNet provider.
 * "ScoreProgress" provider provides functionality which allows to
 * exchange information on scores achieved by players during gameplay.
 */
public class MNScoreProgressProvider
 {
  public static final String PROVIDER_NAME = "com.playphone.mn.ps1";

  /**
   * A class representing player score information.
   */
  public static class ScoreItem
   {
    /**
     * Player information.
     */
    public MNUserInfo userInfo;

    /**
     * Achieved score.
     */
    public long       score;

    /**
     * Taken place.
     */
    public int        place;

    /**
     * Constructs a new <code>ScoreItem</code> object.
     *
     * @param userInfo player information
     * @param score    achieved score
     * @param place    taken place
     */
    public ScoreItem (MNUserInfo userInfo, long score, int place)
     {
      this.userInfo = userInfo;
      this.score    = score;
      this.place    = place;
     }
   }

  /**
   * Interface handling score updates.
   */
  public interface IEventHandler
   {
    /**
     * Invoked when new score information arrived.
     *
     * @param scoreBoard array of players scores sorted using score comparator
     * passed to <code>setScoreComparator</code> method of <code>MNScoreProgressProvider</code>
     * object
     */
    void onScoresUpdated (ScoreItem[] scoreBoard);
   }

  /**
   * A class which implements IEventHandler interface by ignoring all
   * received events.
   */
  public static class EventHandlerAbstract implements IEventHandler
   {
    public void onScoresUpdated (ScoreItem[] scoreBoard)
     {
     }
   }

  /**
   * Constructs a new <code>MNScoreProgressProvider</code> object.
   *
   * @param session         MultiNet session instance
   * @param refreshInterval time in milliseconds between successive score information updates.
   * If <code>refreshInterval</code> is less or equal to zero, information on player score will be
   * sended immediately after <code>postScore</code> call.
   * @param updateDelay     time in milliseconds to wait for other player's score information. If
   * <code>refreshInterval</code> is less or equal to zero, this parameter is not used. If
   * this parameter is less or equal to zero and <code>refreshInterval</code> is
   * greater than zero, <code>refreshInterval / 3</code> will be used as update delay.
   */
  public MNScoreProgressProvider (MNSession     session,
                                int           refreshInterval,
                                int           updateDelay)
   {
    if (refreshInterval <= 0)
     {
      scoreProgress = new ScoreProgressASync(session);
     }
    else
     {
      scoreProgress = new ScoreProgressSync(session,refreshInterval,updateDelay);
     }
   }

  /**
   * Constructs a new <code>MNScoreProgressProvider</code> object with zero
   * <code>refreshInterval</code> and <code>updateDelay</code> values.
   *
   * @param session         MultiNet session instance
   */

  public MNScoreProgressProvider (MNSession session)
   {
    this(session,0,0);
   }

  /**
   * Sets refresh interval and update delay parameters.
   *
   * @param refreshInterval time in milliseconds between successive score information updates.
   * If <code>refreshInterval</code> is less or equal to zero, information on player score will be
   * sended immediately after <code>postScore</code> call.
   * @param updateDelay     time in milliseconds to wait for other player's score information. If
   * <code>refreshInterval</code> is less or equal to zero, this parameter is not used. If
   * this parameter is less or equal to zero and <code>refreshInterval</code> is
   * greater than zero, <code>refreshInterval / 3</code> will be used as update delay.
   */
  public void setRefreshIntervalAndUpdateDelay (int refreshInterval,
                                                int updateDelay)
   {
    if (scoreProgress.running)
     {
      return;
     }

    MNSession session = scoreProgress.session;

    stop();

    if (refreshInterval <= 0)
     {
      scoreProgress = new ScoreProgressASync(session);
     }
    else
     {
      scoreProgress = new ScoreProgressSync(session,refreshInterval,updateDelay);
     }
   }

  /**
   * Starts score information exchange.
   */
  public void start ()
   {
    scoreProgress.start();
   }

  /**
   * Finishes score information exchange.
   */
  public void stop ()
   {
    scoreProgress.stop();
   }

  /**
   * Sets score comparator.
   * <code>ScoreComparatorMoreIsBetter</code> instance can be used if greater
   * score means better place (this is default mode).
   * <code>ScoreComparatorLessIsBetter</code> instance can be used if lesser
   * score means better place.
   *
   * @params comparator score comparator object.
   */
  public void setScoreComparator (Comparator<ScoreItem> comparator)
   {
    scoreProgress.setScoreComparator(comparator);
   }

  /**
   * Schedules player score update.
   * Call this method after player score has been changed.
   *
   * @param score new player score
   */
  public void postScore (long score)
   {
    scoreProgress.postScore(score);
   }

  /**
   * Adds event handler
   *
   * @param eventHandler an object that implements
   * {@link IEventHandler IEventHandler} interface
   */
  public void addEventHandler (IEventHandler eventHandler)
   {
    scoreProgress.eventHandlers.add(eventHandler);
   }

  /**
   * Removes event handler
   *
   * @param eventHandler an object that implements
   * {@link IEventHandler IEventHandler} interface
   */
  public void removeEventHandler (IEventHandler eventHandler)
   {
    scoreProgress.eventHandlers.remove(eventHandler);
   }

  /**
   * A class representing score comparator which treats greater score as a better result.
   */
  public static class ScoreComparatorMoreIsBetter implements Comparator<ScoreItem>
   {
    public int compare (ScoreItem item1,
                        ScoreItem item2)
     {
      if      (item2.score == item1.score)
       {
        return 0;
       }
      else if (item2.score > item1.score)
       {
        return 1;
       }
      else
       {
        return -1;
       }
     }
   };

  /**
   * A class representing score comparator which treats lesser score as a better result.
   */
  public static class ScoreComparatorLessIsBetter implements Comparator<ScoreItem>
   {
    public int compare (ScoreItem item1,
                        ScoreItem item2)
     {
      if      (item1.score == item2.score)
       {
        return 0;
       }
      else if (item1.score > item2.score)
       {
        return 1;
       }
      else
       {
        return -1;
       }
     }
   };

  private static final class ScoreStateSlice
   {
    public ScoreStateSlice ()
     {
      scores = new Hashtable<Integer,ScoreItem>();
     }

    public void clear ()
     {
      scores.clear();
     }

    public void updateUser (MNUserInfo userInfo, long score)
     {
      ScoreItem item = new ScoreItem(userInfo,score,0);

      scores.put(userInfo.userSFId,item);
     }

    public ScoreItem[] getSortedScores (Comparator<ScoreItem> comparator)
     {
      ArrayList<ScoreItem> list = new ArrayList<ScoreItem>(scores.values());

      int count = list.size();

      if (count > 0)
       {
        Collections.sort(list,comparator);

        ScoreItem item = list.get(0);

        item.place = 1;

        long score = item.score;
        int  place = 1;

        for (int index = 1; index < count; index++)
         {
          item = list.get(index);

          if (item.score != score)
           {
            place++;
            score = item.score;
           }

          item.place = place;
         }
       }

      return list.toArray(new ScoreItem[count]);
     }

    Hashtable<Integer,ScoreItem> scores;
   }

  private static abstract class ScoreProgress extends MNSessionEventHandlerAbstract
   {
    public ScoreProgress (MNSession session)
     {
      this.session         = session;
      this.eventHandlers   = new MNEventHandlerArray<IEventHandler>();
      this.scoreComparator = null;

      running   = false;
      startTime = 0;
     }

    public void setScoreComparator (Comparator<ScoreItem> comparator)
     {
      if (comparator != null)
       {
        this.scoreComparator = comparator;
       }
      else
       {
        this.scoreComparator = new ScoreComparatorMoreIsBetter();
       }
     }

    public void start ()
     {
      if (running)
       {
        stop();
       }

      if (scoreComparator == null)
       {
        scoreComparator = new ScoreComparatorMoreIsBetter();
       }

      startTime = System.currentTimeMillis();

      session.addEventHandler(this);
     }

    public void stop ()
     {
      if (running)
       {
        session.removeEventHandler(this);
       }
     }

    public abstract void postScore (long score);

    public abstract void onScoreUpdateReceived (MNUserInfo userInfo,
                                                long       score,
                                                int        scoreTime);

    protected void notifyScoresChanged (ScoreItem[] scoreBoard)
     {
      eventHandlers.beginCall();

      try
       {
        int count = eventHandlers.size();

        for (int index = 0; index < count; index++)
         {
          eventHandlers.get(index).onScoresUpdated(scoreBoard);
         }
       }
      finally
       {
        eventHandlers.endCall();
       }
     }

    protected void sendScore (MNUserInfo userInfo, long score, long scoreTime)
     {
      String message = Long.toString(scoreTime) + ":" + Long.toString(score);

      session.sendPluginMessage(PROVIDER_NAME,message);
     }

    public void mnSessionPluginMessageReceived (String     pluginName,
                                                String     message,
                                                MNUserInfo sender)
     {
      if (!running)
       {
        return;
       }

      if (sender == null)
       {
        return;
       }

      if (!pluginName.equals(PROVIDER_NAME))
       {
        return;
       }

      String[] components = message.split(":");

      if (components.length != 2)
       {
        return;
       }

      Integer scoreTime = parseInteger(components[0]);

      if (scoreTime == null)
       {
        return;
       }

      Long score = parseLong(components[1]);

      if (score == null)
       {
        return;
       }

      onScoreUpdateReceived(sender,score,scoreTime);
     }

    private static final Integer parseInteger (String s)
     {
      try
       {
        return new Integer(s);
       }
      catch (NumberFormatException e)
       {
        return null;
       }
     }

    private static final Long parseLong (String s)
     {
      try
       {
        return new Long(s);
       }
      catch (NumberFormatException e)
       {
        return null;
       }
     }

    protected boolean       running;
    protected long          startTime;

    protected MNSession                          session;
    protected MNEventHandlerArray<IEventHandler> eventHandlers;
    protected Comparator<ScoreItem>              scoreComparator;
   }

  private static class ScoreProgressASync extends ScoreProgress
   {
    public ScoreProgressASync (MNSession             session)
     {
      super(session);

      scoreState = new ScoreStateSlice();
     }

    public void start ()
     {
      scoreState.clear();

      super.start();

      running = true;
     }

    public void stop ()
     {
      super.stop();

      if (!running)
       {
        return;
       }

      running = false;

      scoreState.clear();
     }

    public void postScore (long score)
     {
      if (!running || !session.isInGameRoom())
       {
        return;
       }

      long scoreTime = System.currentTimeMillis() - startTime;

      MNUserInfo myUserInfo = session.getMyUserInfo();

      if (myUserInfo == null)
       {
        return; // we're not logged in
       }

      scoreState.updateUser(myUserInfo,score);

      notifyScoresChanged(scoreState.getSortedScores(scoreComparator));

      sendScore(myUserInfo,score,scoreTime);
     }

    public void onScoreUpdateReceived (MNUserInfo userInfo,
                                       long       score,
                                       int        scoreTime)
     {
      if (running && session.isInGameRoom())
       {
        scoreState.updateUser(userInfo,score);

        notifyScoresChanged(scoreState.getSortedScores(scoreComparator));
       }
     }

    private ScoreStateSlice scoreState;
   }

  private static class ScoreProgressSync extends ScoreProgress
   {
    public ScoreProgressSync (MNSession     session,
                              int           refreshInterval,
                              int           updateDelay)
     {
      super(session);

      scoreSlices = new ScoreStateSlice[SLICES_COUNT];

      for (int index = 0; index < SLICES_COUNT; index++)
       {
        scoreSlices[index] = new ScoreStateSlice();
       }

      this.refreshInterval = refreshInterval > MIN_REFRESH_INTERVAL ?
                             refreshInterval : MIN_REFRESH_INTERVAL;

      this.updateDelay = updateDelay > 0 ?
                         updateDelay : this.refreshInterval / 3;

      postScoreTimer   = null;
      updateScoreTimer = null;
      currentScore     = 0;
     }

    public void postScoreTimerFired ()
     {
      if (!running || !session.isInGameRoom())
       {
        return;
       }

      long scoreTime = System.currentTimeMillis() - startTime +
                        refreshInterval / 2;

      scoreTime = (scoreTime / refreshInterval) * refreshInterval;

      MNUserInfo myUserInfo = session.getMyUserInfo();

      if (myUserInfo == null)
       {
        return; // we're not logged in
       }

      sendScore(myUserInfo,currentScore,scoreTime);

      if (scoreTime < baseTime)
       {
        return;
       }

      int index;
      int offset = (int)((scoreTime - baseTime) / refreshInterval);

      if (offset < SLICES_COUNT)
       {
        if (offset > 0)
         {
          for (index = offset; index < SLICES_COUNT; index++)
           {
            scoreSlices[index - offset] = scoreSlices[index];
           }

          for (index = SLICES_COUNT - offset; index < SLICES_COUNT; index++)
           {
            scoreSlices[index] = new ScoreStateSlice();
           }
         }
       }
      else
       {
        clearSlices();
       }

      baseTime = scoreTime;

      scoreSlices[0].updateUser(myUserInfo,currentScore);

      if (updateScoreTimer == null)
       {
        updateScoreTimer = new Timer();
        updateScoreTimer.schedule
         (new TimerTask()
           {
            public void run ()
             {
              notifyScoresChanged
               (scoreSlices[0].getSortedScores(scoreComparator));

               updateScoreTimer = null;
             }
           },
          updateDelay);
       }
     }

    public void start ()
     {
      clearSlices();

      super.start();

      postScoreTimer = new Timer();
      postScoreTimer.scheduleAtFixedRate
       (new TimerTask()
         {
          public void run ()
           {
            postScoreTimerFired();
           }
         },
        refreshInterval,
        refreshInterval);

      currentScore = 0;
      baseTime     = 0;
      running      = true;
     }

    public void stop ()
     {
      super.stop();

      if (!running)
       {
        return;
       }

      running = false;

      postScoreTimer.cancel();
      postScoreTimer = null;

      if (updateScoreTimer != null)
       {
        updateScoreTimer.cancel();
        updateScoreTimer = null;
       }

      clearSlices();
     }

    public void postScore (long score)
     {
      currentScore = score;
     }

    public void onScoreUpdateReceived (MNUserInfo userInfo,
                                       long       score,
                                       int        scoreTime)
     {
      if (!running || !session.isInGameRoom())
       {
        return;
       }

      if (scoreTime < baseTime)
       {
        return; /* too late for this score */
       }

      int sliceIndex = (int)((scoreTime - baseTime) / refreshInterval);

      if (sliceIndex >= SLICES_COUNT)
       {
        return; /* too far in the future */
       }

      scoreSlices[sliceIndex].updateUser(userInfo,score);
     }

    private void clearSlices ()
     {
      for (int index = 0; index < SLICES_COUNT; index++)
       {
        scoreSlices[index].clear();
       }
     }

    private ScoreStateSlice[] scoreSlices;

    private long  currentScore;
    private long  baseTime;

    private int   refreshInterval;
    private int   updateDelay;
    private Timer postScoreTimer;
    private Timer updateScoreTimer;

    private static final int SLICES_COUNT = 4;
    private static final int MIN_REFRESH_INTERVAL = 500;
   }

  private ScoreProgress scoreProgress;
 }

