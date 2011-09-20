//
//  MNConst.java
//  MultiNet client
//
//  Copyright 2009 PlayPhone. All rights reserved.
//

package com.playphone.multinet;

public final class MNConst
 {
  public static final int MN_USER_STATUS_UNDEFINED = 0;
  public static final int MN_USER_PLAYER           = 1;
  public static final int MN_USER_CHATER           = 100;

  public static final int MN_USER_ACCOUNT_STATUS_GUEST       = 0;
  public static final int MN_USER_ACCOUNT_STATUS_PROVISIONAL = 10;
  public static final int MN_USER_ACCOUNT_STATUS_NORMAL      = 100;

  public static final long MN_USER_ID_UNDEFINED    = 0;
  public static final int  MN_USER_SFID_UNDEFINED  = -1;

  /**
   * Session state: session is inactive.
   */
  public static final int MN_OFFLINE       = 0;

  /**
   * Session state: login procedure is in progress.
   */
  public static final int MN_CONNECTING    = 1;

  /**
   * Session state: session is active, user is in lobby room.
   */
  public static final int MN_LOGGEDIN      = 50;

  /**
   * Session state: session is active, user is waiting for other players in game room.
   */
  public static final int MN_IN_GAME_WAIT  = 100;

  /**
   * Session state: session is active, countdown in progress.
   */
  public static final int MN_IN_GAME_START = 110;

  /**
   * Session state: session is active, game is in progress.
   */
  public static final int MN_IN_GAME_PLAY  = 120;

  /**
   * Session state: session is active, game had been ended recently.
   */
  public static final int MN_IN_GAME_END   = 180;
 }
