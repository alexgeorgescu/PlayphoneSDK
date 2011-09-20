//
//  IMNSessionEventHandler.java
//  MultiNet client
//
//  Copyright 2009 PlayPhone. All rights reserved.
//

package com.playphone.multinet.core;

import com.playphone.multinet.MNGameParams;
import com.playphone.multinet.MNUserInfo;
import com.playphone.multinet.MNErrorInfo;

/**
 * The handler interface for receiving MultiNet session events.
 * By implementing <code>IMNSessionEventHandler</code> interface,
 * class can respond to MultiNet session events such as MultiNet session
 * state changes, incoming chat messages notifications and others.
 */
public interface IMNSessionEventHandler
 {
  /**
   * Invoked when login procedure has been started.
   */
  void mnSessionLoginInitiated             ();

  /**
   * Invoked when MultiNet session status has been changed.
   *
   * @param newStatus new status
   * @param oldStatus previous status
   */
  void mnSessionStatusChanged              (int newStatus, int oldStatus);

  /**
   * Invoked when logged user has been changed.
   *
   * @param userId user id of new user
   */
  void mnSessionUserChanged                (long userId);

  /**
   * Invoked when user status has been changed.
   *
   * @param userStatus new user status
   */
  void mnSessionRoomUserStatusChanged      (int userStatus);

  /**
   * Invoked when public message has been received.
   *
   * @param chatMessage received message data
   * @see MNChatMessage
   */
  void mnSessionChatPublicMessageReceived  (MNChatMessage chatMessage);

  /**
   * Invoked when private message has been received.
   *
   * @param chatMessage received message data
   * @see MNChatMessage
   */
  void mnSessionChatPrivateMessageReceived (MNChatMessage chatMessage);

  /**
   * Invoked when invitation to join game room has been received.
   *
   * @param params invitation data
   * @see MNJoinRoomInvitationParams
   */
  void mnSessionJoinRoomInvitationReceived (MNJoinRoomInvitationParams params);

  /**
   * Invoked when game countdown state has been changed.
   *
   * @param secondsLeft seconds to game start
   */
  void mnSessionGameStartCountdownTick     (int               secondsLeft);

  /**
   * Invoked when game has been finished.
   *
   * @param gameResult {@link MNGameResult MNGameResult} object containing
   * player result
   * @see MNGameResult
   */
  void mnSessionGameFinishedWithResult     (MNGameResult      gameResult);

  /**
   * Invoked when new or updated game results have been received.
   *
   * @param gameResults received game results
   * @see MNCurrGameResults
   */
  void mnSessionCurrGameResultsReceived    (MNCurrGameResults gameResults);

  /**
   * Invoked when game message has been received.
   *
   * @param message received message
   * @param sender message sender (may be null if message has been sended by server)
   */
  void mnSessionGameMessageReceived        (String            message,
                                            MNUserInfo        sender);

  /**
   * Invoked when plugin message has been received.
   *
   * @param pluginName plugin name
   * @param message received message
   * @param sender message sender (may be null if message has been sended by server)
   */
  void mnSessionPluginMessageReceived      (String            pluginName,
                                            String            message,
                                            MNUserInfo        sender);

  /**
   * Invoked when game must be started.
   *
   * @param params parameters for game to be started
   * @see MNGameParams
   */
  void mnSessionDoStartGameWithParams      (MNGameParams gameParams);

  /**
   * Invoked when game must be finished (on server event).
   *
   * @param params parameters for game to be started
   */
  void mnSessionDoFinishGame               ();

  /**
   * Invoked when game has been canceled.
   *
   * @param params parameters for game to be started
   */
  void mnSessionDoCancelGame               ();

  /**
   * Invoked when user joined current room.
   *
   * @param userInfo joined user data
   * @see MNUserInfo
   */
  void mnSessionRoomUserJoin               (MNUserInfo userInfo);

  /**
   * Invoked when user left current room.
   *
   * @param userInfo user data
   * @see MNUserInfo
   */
  void mnSessionRoomUserLeave              (MNUserInfo userInfo);

  /**
   * Invoked when connection with social network has been terminated.
   *
   * @param socNetId social network identifier
   */
  void mnSessionSocNetLoggedOut            (int socNetId);

  /**
   * Invoked when some error occurred
   * @param errorInfo error information
   * @see MNErrorInfo
   */
  void mnSessionErrorOccurred              (MNErrorInfo errorInfo);

  /**
   * Invoked when device users information has been changed
   */
  void mnSessionDevUsersInfoChanged        ();

  /**
   * Invoked when default game settings id has been changed
   */
  void mnSessionDefaultGameSetIdChangedTo  (int gameSetId);

  /**
   * Invoked when MultiNet configuration has been loaded
   */
  void mnSessionConfigLoaded               ();

  /**
   * Invoked when MultiNet configuration loading started
   */
  void mnSessionConfigLoadStarted          ();

  /**
   * Invoked when WebFront url become ready to use
   * @param url WebFront url
   */
  void mnSessionWebFrontURLReady           (String url);

  /**
   * Invoked when MNSession's execAppCommand method has been called
   * @param cmdName command name
   * @param cmdParam command parameter
   */
  void mnSessionExecAppCommandReceived    (String cmdName, String cmdParam);

  /**
   * Invoked when MNSession's execUICommand method has been called
   * @param cmdName command name
   * @param cmdParam command parameter
   */
  void mnSessionExecUICommandReceived    (String cmdName, String cmdParam);

  /**
   * Invoked when "apphost" call has been received and allows to override
   * default request handling
   * @return true if handler processed a call and default handling should
   * not be called, false - if handler did not process a call
   */
  boolean mnSessionAppHostCallReceived     (MNAppHostCallInfo appHostCallInfo);

  /**
   * Invoked when web event has been received
   * @param eventName event name
   * @param eventParam event-specific parameter
   * @param callbackId request identifier (optional, may be null)
   */
  void mnSessionWebEventReceived (String eventName, String eventParam, String callbackId);

  /**
   * Invoked when system event has been received
   * @param eventName event name
   * @param eventParameter event-specific parameter
   * @param callbackId request identifier (optional, may be null)
   */
  void mnSessionSysEventReceived (String eventName, String eventParam, String callbackId);

  /**
   * Invoked when MNSession's handleApplicationIntent method has been called
   * with intent which contains MultiNet framework's extra
   * @param param launch parameter
   */
  void mnSessionAppStartParamUpdated     (String param);
 }

