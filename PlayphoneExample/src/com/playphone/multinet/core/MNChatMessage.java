//
//  MNChatMessage.java
//  MultiNet client
//
//  Copyright 2009 PlayPhone. All rights reserved.
//

package com.playphone.multinet.core;

import com.playphone.multinet.MNUserInfo;

/**
 * A class representing chat message.
 */
public class MNChatMessage
 {
  /**
   * Message sender information
   */
  public MNUserInfo sender;

  /**
   * Message text
   */
  public String     message;

  /**
   * A boolean value that determines whether message is private
   */
  public boolean    isPrivate;

  /**
   * Constructs a new <code>MNChatMessage</code> object.
   *
   * @param sender message sender information
   * @param message message text
   * @param isPrivate boolean value that determines whether message is private
   */
  public MNChatMessage (MNUserInfo sender, String message, boolean isPrivate)
   {
    this.sender    = sender;
    this.message   = message;
    this.isPrivate = isPrivate;
   }
 }

