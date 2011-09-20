//
//  MNSessionEventHandlerAbstract.java
//  MultiNet client
//
//  Copyright 2009 PlayPhone. All rights reserved.
//

package com.playphone.multinet.core;

import com.playphone.multinet.*;

/**
 * A class representing empty MultiNet session event handler.
 * This class is an {@link IMNSessionEventHandler IMNSessionEventHandler}
 * interface implementation in which all events are ignored.
 */
public class MNSessionEventHandlerAbstract implements IMNSessionEventHandler
 {
  public void mnSessionLoginInitiated ()
   {
   }

  public void mnSessionStatusChanged (int newStatus, int oldStatus)
   {
   }

  public void mnSessionUserChanged (long userId)
   {
   }

  public void mnSessionRoomUserStatusChanged (int userStatus)
   {
   }

  public void mnSessionChatPublicMessageReceived (MNChatMessage chatMessage)
   {
   }

  public void mnSessionChatPrivateMessageReceived (MNChatMessage chatMessage)
   {
   }

  public void mnSessionJoinRoomInvitationReceived (MNJoinRoomInvitationParams params)
   {
   }

  public void mnSessionGameStartCountdownTick (int secondsLeft)
   {
   }

  public void mnSessionGameFinishedWithResult (MNGameResult gameResult)
   {
   }

  public void mnSessionCurrGameResultsReceived (MNCurrGameResults gameResults)
   {
   }

  public void mnSessionGameMessageReceived (String     message,
                                            MNUserInfo sender)
   {
   }

  public void mnSessionPluginMessageReceived   (String     pluginName,
                                                String     message,
                                                MNUserInfo sender)
   {
   }

  public void mnSessionDoStartGameWithParams (MNGameParams gameParams)
   {
   }

  public void mnSessionDoFinishGame ()
   {
   }

  public void mnSessionDoCancelGame ()
   {
   }

  public void mnSessionRoomUserJoin (MNUserInfo userInfo)
   {
   }

  public void mnSessionRoomUserLeave (MNUserInfo userInfo)
   {
   }

  public void mnSessionSocNetLoggedOut (int socNetId)
   {
   }

  public void mnSessionErrorOccurred (MNErrorInfo errorInfo)
   {
   }

  public void mnSessionDevUsersInfoChanged ()
   {
   }

  public void mnSessionDefaultGameSetIdChangedTo (int gameSetId)
   {
   }

  public void mnSessionConfigLoaded ()
   {
   }

  public void mnSessionConfigLoadStarted ()
   {
   }

  public void mnSessionWebFrontURLReady (String url)
   {
   }

  public void mnSessionExecAppCommandReceived (String cmdName, String cmdParam)
   {
   }

  public void mnSessionExecUICommandReceived (String cmdName, String cmdParam)
   {
   }

  public boolean mnSessionAppHostCallReceived (MNAppHostCallInfo appHostCallInfo)
   {
    return false;
   }

  public void mnSessionWebEventReceived (String eventName, String eventParam, String callbackId)
   {
   }

  public void mnSessionSysEventReceived (String eventName, String eventParam, String callbackId)
   {
   }

  public void mnSessionAppStartParamUpdated (String param)
   {
   }
 }

