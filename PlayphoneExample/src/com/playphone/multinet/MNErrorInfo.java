//
//  MNErrorInfo.java
//  MultiNet client
//
//  Copyright 2009 PlayPhone. All rights reserved.
//

package com.playphone.multinet;

/**
 * A class representing error information.
 */
public class MNErrorInfo
 {
  /**
   * Constructs a new <code>MNErrorInfo</code> object.
   *
   * @param actionCode action which caused error
   * @param errorMessage error message
   */
  public MNErrorInfo (int actionCode, String errorMessage)
   {
    this.actionCode   = actionCode;
    this.errorMessage = errorMessage;
   }

  /**
   * Action which caused error.
   */
  public int    actionCode;

  /**
   * Error message
   */
  public String errorMessage;

  public static final int ACTION_CODE_UNDEFINED             = 0;

  public static final int ACTION_CODE_LOGIN                 = 11;
  public static final int ACTION_CODE_CONNECT               = 12;

  public static final int ACTION_CODE_FB_CONNECT            = 21;
  public static final int ACTION_CODE_FB_RESUME             = 22;

  public static final int ACTION_CODE_POST_GAME_RESULT      = 51;

  public static final int ACTION_CODE_JOIN_GAME_ROOM        = 101;
  public static final int ACTION_CODE_CREATE_BUDDY_ROOM     = 102;

  public static final int ACTION_CODE_LEAVE_ROOM            = 111;

  public static final int ACTION_CODE_SET_USER_STATUS       = 121;

  public static final int ACTION_CODE_START_BUDDY_ROOM_GAME = 151;
  public static final int ACTION_CODE_STOP_ROOM_GAME        = 152;

  public static final int ACTION_CODE_LOAD_CONFIG           = 401;

  public static final int ACTION_CODE_OTHER_MIN_VALUE       = 1001;
 }

