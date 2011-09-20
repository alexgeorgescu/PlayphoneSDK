//
//  MNDirect.java
//  MultiNet client
//
//  Copyright 2009 PlayPhone. All rights reserved.
//

package com.playphone.multinet;

import android.content.Intent;

import com.playphone.multinet.core.IMNPlatform;
import com.playphone.multinet.core.MNPlatformAndroid;
import com.playphone.multinet.core.MNSession;
import com.playphone.multinet.core.MNSessionEventHandlerAbstract;
import com.playphone.multinet.core.MNUserProfileView;
import com.playphone.multinet.core.IMNUserProfileViewEventHandler;
import com.playphone.multinet.core.MNGameResult;
import com.playphone.multinet.core.MNUtils;

import com.playphone.multinet.providers.MNAchievementsProvider;
import com.playphone.multinet.providers.MNClientRobotsProvider;
import com.playphone.multinet.providers.MNGameCookiesProvider;
import com.playphone.multinet.providers.MNMyHiScoresProvider;
import com.playphone.multinet.providers.MNPlayerListProvider;
import com.playphone.multinet.providers.MNScoreProgressProvider;
import com.playphone.multinet.providers.MNVItemsProvider;
import com.playphone.multinet.providers.MNVShopProvider;

/**
 * A class representing MultiNet "Direct" API.
 * MNDirect interface provides minimal set of methods required to enable
 * MultiNet functionality in application.
 */
public class MNDirect
 {
  /**
   * Initializes MultiNet session and view.
   *
   * @param gameId       game id
   * @param gameSecret   game secret
   * @param eventHandler <code>MNDirect</code> events handler
   * @param activity     activity
   */
  public static void init (int gameId, String gameSecret,
                           IMNDirectEventHandler eventHandler,
                           android.app.Activity activity)
   {
    init(gameId,gameSecret,eventHandler,new MNPlatformAndroid(activity));
   }

  /**
   * Initializes MultiNet session and view.
   *
   * @param gameId       game id
   * @param gameSecret   game secret
   * @param eventHandler <code>MNDirect</code> events handler
   * @param platform     platform-dependent object implementing
   *                     <code>IMNPlatform</code> interface
   */
  protected static void init (int gameId, String gameSecret,
                              IMNDirectEventHandler eventHandler,
                              IMNPlatform platform)
   {
    if (eventHandler == null)
     {
      throw new RuntimeException("internal error: MNDirect's eventHandler cannot be null");
     }

    if (platform == null)
     {
      throw new RuntimeException("internal error: MNDirect's platform cannot be null");
     }

    session = new MNSession(gameId,gameSecret,platform);

    sessionEventHandler = new SessionEventHandler(eventHandler,platform,session);

    initProviders();

    eventHandler.mnDirectSessionReady(session);

    view = platform.createUserProfileView();

    view.addEventHandler(sessionEventHandler);
    view.bindToSession(session);

    session.addEventHandler(sessionEventHandler);
   }

  /**
   * Creates game secret string from components
   * @param secret1 first component of game secret
   * @param secret2 second component of game secret
   * @param secret3 third component of game secret
   * @param secret4 fourth component of game secret
   * @return game secret string
   */
  public static String makeGameSecretByComponents (int secret1, int secret2, int secret3, int secret4)
   {
    return MNUtils.makeGameSecretByComponents(secret1,secret2,secret3,secret4);
   }

  /**
   * Terminates MultiNet session and releases all acquired resources.
   */
  public static synchronized void shutdownSession ()
   {
    if (view != null)
     {
      view.destroy();

      view = null;
     }

    releaseProviders();

    if (session != null)
     {
      if (sessionEventHandler != null)
       {
        session.removeEventHandler(sessionEventHandler);
       }

      session.shutdown();

      session = null;
     }

    if (sessionEventHandler != null)
     {
      sessionEventHandler.shutdown();
      sessionEventHandler = null;
     }
   }

  /**
   * Returns MultiNet server connection status.
   *
   * @return <code>true</code> if user is connected to MultiNet server and
   * <code>false</code> otherwise.
   */
  public static synchronized boolean isOnline ()
   {
    return session != null ? session.isOnline() : false;
   }

  /**
   * Returns user login status.
   *
   * @return <code>true</code> if user is logged in and <code>false</code>
   * otherwise.
   */
  public static synchronized boolean isUserLoggedIn ()
   {
    return session != null ? session.isUserLoggedIn() : false;
   }

  /**
   * Returns status of MultiNet session.
   *
   * @return one of the <code>MN_OFFLINE</code>, <code>MN_CONNECTING</code>,
   * <code>MN_LOGGEDIN</code>, <code>MN_IN_GAME_WAIT</code>,
   * <code>MN_IN_GAME_START</code>, <code>MN_IN_GAME_PLAY</code>,
   * <code>MN_IN_GAME_END</code>.
   */
  public static synchronized int getSessionStatus ()
   {
    if (session != null)
     {
      return session.getStatus();
     }
    else
     {
      return MNConst.MN_OFFLINE;
     }
   }

  /**
   * Sends game score to MultiNet server.
   *
   * @param score game score
   */
  public static synchronized void postGameScore (long score)
   {
    if (session != null)
     {
      if (sessionEventHandler.gameParams == null)
       {
        sessionEventHandler.gameParams =
         new MNGameParams(session.getDefaultGameSetId(),"","",0,
                          MNGameParams.MN_PLAYMODEL_SINGLEPLAY);
       }

      MNGameResult gameResult = new MNGameResult(sessionEventHandler.gameParams);

      gameResult.score = score;

      session.finishGameWithResult(gameResult);

      sessionEventHandler.gameParams = null;
     }
   }

  /**
   * Schedules sending game score to MultiNet server.
   * Score will be posted as soon as player login to MultiNet.
   *
   * @param score game score
   */
  public static synchronized void postGameScorePending (long score)
   {
    if (session != null)
     {
      if (sessionEventHandler.gameParams == null)
       {
        sessionEventHandler.gameParams =
         new MNGameParams(session.getDefaultGameSetId(),"","",0,
                          MNGameParams.MN_PLAYMODEL_SINGLEPLAY);
       }

      MNGameResult gameResult = new MNGameResult(sessionEventHandler.gameParams);

      gameResult.score = score;

      session.schedulePostScoreOnLogin(gameResult);

      sessionEventHandler.gameParams = null;
     }
   }

  /**
   * Cancels game on user request (for example "quit" button pressed).
   */
  public static synchronized void cancelGame ()
   {
    if (session != null)
     {
      session.cancelPostScoreOnLogin();

      session.cancelGameWithParams(sessionEventHandler.gameParams);

      sessionEventHandler.gameParams = null;
     }
   }

  /**
   * Sets default game settings id
   * @param gameSetId game settings id
   */
  public static void setDefaultGameSetId (int gameSetId)
   {
    if (session != null)
     {
      session.setDefaultGameSetId(gameSetId);
     }
   }

  /**
   * Returns default game settings id
   * @return default game settings id
   */
  public static int getDefaultGameSetId ()
   {
    if (session != null)
     {
      return session.getDefaultGameSetId();
     }
    else
     {
      return 0;
     }
   }

  /**
   * Sends application custom "beacon".
   * Beacons are used for application actions usage statistic.
   *
   * @param actionName name of the action
   * @param beaconData "beacon" data
   */
  public static synchronized void sendAppBeacon (String actionName, String beaconData)
   {
    if (session != null)
     {
      session.sendAppBeacon(actionName,beaconData);
     }
   }

  /**
   * Executes application command
   *
   * @param name command name
   * @param param command parameter
   */
  public static void execAppCommand(String name, String param)
   {
    if (session != null)
     {
      session.execAppCommand(name,param);
     }
   }

  /**
   * Allows MultiNet framework to process start intent parameters
   * @param intent intent that started activity
   */
  public static void handleApplicationIntent (Intent intent)
   {
    if (session != null)
     {
      session.handleApplicationIntent(intent);
     }
   }

  /**
   * Sends game message to room.
   *
   * @param message a message to be sent
   */
  public static synchronized void sendGameMessage (String message)
   {
    if (session != null)
     {
      session.sendGameMessage(message);
     }
   }

  /**
   * Returns <code>MultiNet</code> session object instance.
   *
   * @return <code>MultiNet</code> session object
   */
  public static MNSession getSession ()
   {
    return session;
   }

  /**
   * Returns <code>MultiNet</code> view instance.
   *
   * @return <code>MultiNet</code> view object
   */
  public static MNUserProfileView getView ()
   {
    return view;
   }

  /**
   * Returns achievements provider instance.
   *
   * @return <code>MNAchievementsProvider</code> object
   */
  public static MNAchievementsProvider getAchievementsProvider ()
   {
    return achievementsProvider;
   }

  /**
   * Returns client robots provider instance.
   *
   * @return <code>MNClientRobotsProvider</code> object
   */
  public static MNClientRobotsProvider getClientRobotsProvider ()
   {
    return clientRobotsProvider;
   }

  /**
   * Returns game cookies provider instance.
   *
   * @return <code>MNGameCookiesProvider</code> object
   */
  public static MNGameCookiesProvider getGameCookiesProvider ()
   {
    return gameCookiesProvider;
   }

  /**
   * Returns hi-scores provider instance.
   *
   * @return <code>MNMyHiScoresProvider</code> object
   */
  public static MNMyHiScoresProvider getMyHiScoresProvider ()
   {
    return myHiScoresProvider;
   }

  /**
   * Returns player list provider instance.
   *
   * @return <code>MNPlayerListProvider</code> object
   */
  public static MNPlayerListProvider getPlayerListProvider ()
   {
    return playerListProvider;
   }

  /**
   * Returns score progress provider instance.
   *
   * @return <code>MNScoreProgressProvider</code> object
   */
  public static MNScoreProgressProvider getScoreProgressProvider ()
   {
    return scoreProgressProvider;
   }

  /**
   * Returns virtual items provider instance.
   *
   * @return <code>MNVItemsProvider</code> object
   */
  public static MNVItemsProvider getVItemsProvider ()
   {
    return vItemsProvider;
   }

  /**
   * Returns virtual shop provider instance.
   *
   * @return <code>MNVShopProvider</code> object
   */
  public static MNVShopProvider getVShopProvider ()
   {
    return vShopProvider;
   }

  private static class SessionEventHandler
                 extends MNSessionEventHandlerAbstract
                 implements IMNUserProfileViewEventHandler
   {
    public SessionEventHandler (IMNDirectEventHandler eventHandler,
                                IMNPlatform           platform,
                                MNSession             session)
     {
      this.eventHandler = eventHandler;
      this.platform     = platform;
      this.session      = session;
     }

    public synchronized void shutdown ()
     {
      this.eventHandler = null;
      this.platform     = null;
      this.session      = null;
     }

    public void mnSessionDoStartGameWithParams (final MNGameParams gameParams)
     {
      this.gameParams = gameParams;

      platform.runOnUiThread(new Runnable()
       {
        public void run ()
         {
          synchronized(SessionEventHandler.this)
           {
            if (eventHandler != null)
             {
              eventHandler.mnDirectDoStartGameWithParams(gameParams);
             }
           }
         }
       });
     }

    public void mnSessionDoFinishGame ()
     {
      platform.runOnUiThread(new Runnable()
       {
        public void run ()
         {
          synchronized(SessionEventHandler.this)
           {
            if (eventHandler != null)
             {
              eventHandler.mnDirectDoFinishGame();
             }
           }
         }
       });
     }

    public void mnSessionDoCancelGame ()
     {
      session.cancelPostScoreOnLogin();

      gameParams = null;

      platform.runOnUiThread(new Runnable()
       {
        public void run ()
         {
          synchronized(SessionEventHandler.this)
           {
            if (eventHandler != null)
             {
              eventHandler.mnDirectDoCancelGame();
             }
           }
         }
       });
     }

    public void mnSessionStatusChanged (final int newStatus, int oldStatus)
     {
      platform.runOnUiThread(new Runnable()
       {
        public void run ()
         {
          synchronized(SessionEventHandler.this)
           {
            if (eventHandler != null)
             {
              eventHandler.mnDirectSessionStatusChanged(newStatus);
             }
           }
         }
       });
     }

    public void mnSessionGameMessageReceived (final String     message,
                                              final MNUserInfo sender)
     {
      platform.runOnUiThread(new Runnable()
       {
        public void run ()
         {
          synchronized(SessionEventHandler.this)
           {
            if (eventHandler != null)
             {
              eventHandler.mnDirectDidReceiveGameMessage(message,sender);
             }
           }
         }
       });
     }

    public void mnSessionErrorOccurred (final MNErrorInfo errorInfo)
     {
      platform.runOnUiThread(new Runnable()
       {
        public void run ()
         {
          synchronized(SessionEventHandler.this)
           {
            if (eventHandler != null)
             {
              eventHandler.mnDirectErrorOccurred(errorInfo);
             }
           }
         }
       });
     }

    public void mnUserProfileViewDoGoBack ()
     {
      session.cancelPostScoreOnLogin();

      platform.runOnUiThread(new Runnable()
       {
        public void run ()
         {
          synchronized(SessionEventHandler.this)
           {
            if (eventHandler != null)
             {
              eventHandler.mnDirectViewDoGoBack();
             }
           }
         }
       });
     }

    private IMNDirectEventHandler eventHandler;
    private IMNPlatform           platform;
    public  MNGameParams          gameParams;
    private MNSession             session;
   }

  private static void initProviders ()
   {
    releaseProviders();

    achievementsProvider  = new MNAchievementsProvider(session);
    clientRobotsProvider  = new MNClientRobotsProvider(session);
    gameCookiesProvider   = new MNGameCookiesProvider(session);
    myHiScoresProvider    = new MNMyHiScoresProvider(session);
    playerListProvider    = new MNPlayerListProvider(session);
    scoreProgressProvider = new MNScoreProgressProvider(session);
    vItemsProvider        = new MNVItemsProvider(session);
    vShopProvider         = new MNVShopProvider(session,vItemsProvider);
   }

  private static void releaseProviders ()
   {
    if (achievementsProvider != null)
     {
      achievementsProvider.shutdown(); achievementsProvider = null;
     }

    if (clientRobotsProvider != null)
     {
      clientRobotsProvider.shutdown(); clientRobotsProvider = null;
     }

    if (gameCookiesProvider != null)
     {
      gameCookiesProvider.shutdown(); gameCookiesProvider = null;
     }

    if (myHiScoresProvider != null)
     {
      myHiScoresProvider.shutdown(); myHiScoresProvider = null;
     }

    if (playerListProvider != null)
     {
      playerListProvider.shutdown(); playerListProvider = null;
     }

    if (vItemsProvider != null)
     {
      vItemsProvider.shutdown(); vItemsProvider = null;
     }

    if (vShopProvider != null)
     {
      vShopProvider.shutdown(); vShopProvider = null;
     }

    scoreProgressProvider = null;
   }

  private static MNSession             session;
  private static MNUserProfileView     view;
  private static SessionEventHandler   sessionEventHandler;

  private static MNAchievementsProvider  achievementsProvider;
  private static MNClientRobotsProvider  clientRobotsProvider;
  private static MNGameCookiesProvider   gameCookiesProvider;
  private static MNMyHiScoresProvider    myHiScoresProvider;
  private static MNPlayerListProvider    playerListProvider;
  private static MNScoreProgressProvider scoreProgressProvider;
  private static MNVItemsProvider        vItemsProvider;
  private static MNVShopProvider         vShopProvider;
 }

