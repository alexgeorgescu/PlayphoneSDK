//
//  IMNDirectEventHandler.java
//  MultiNet client
//
//  Copyright 2009 PlayPhone. All rights reserved.
//

package com.playphone.multinet;

import com.playphone.multinet.core.MNSession;

/**
 * The handler interface for receiving important MultiNet events.
 * By implementing <code>IMNDirectEventHandler</code> interface,
 * class can respond to most important MultiNet events.
 */
public interface IMNDirectEventHandler
 {
  /**
   * Invoked when game must be started.
   *
   * @param params parameters for game to be started
   * @see   MNGameParams
   */
  void mnDirectDoStartGameWithParams  (MNGameParams   params);

  /**
   * Invoked when game must be finished (on server event).
   */
  void mnDirectDoFinishGame           ();

  /**
   * Invoked when game has been canceled.
   */
  void mnDirectDoCancelGame           ();

  /**
   * Invoked when user clicks on "Go back" button in MultiNet view.
   */
  void mnDirectViewDoGoBack           ();

  /**
   * Invoked when game message has been received.
   *
   * @param message received message
   * @param sender message sender (may be null if message has been sended by server)
   */
  void mnDirectDidReceiveGameMessage  (String         message,
                                       MNUserInfo     sender);

  /**
   * Invoked when MultiNet session status has been changed.
   *
   * @param newStatus new status
   */
  void mnDirectSessionStatusChanged   (int            newStatus);

  /**
   * Invoked when some error occurred
   * @param error error description
   * @see MNErrorInfo
   */
  void mnDirectErrorOccurred          (MNErrorInfo    error);

  /**
   * Invoked when MultiNet session has been initialized and it is safe to call its methods.
   * For example, plugins initialization can be placed in this method.
   * @param session initialized MNSession object
   */
  void mnDirectSessionReady           (MNSession      session);
 }

