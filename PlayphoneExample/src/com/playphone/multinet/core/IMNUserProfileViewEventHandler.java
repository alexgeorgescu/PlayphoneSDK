//
//  IMNUserProfileViewEventHandler.java
//  MultiNet client
//
//  Copyright 2009 PlayPhone. All rights reserved.
//

package com.playphone.multinet.core;

/**
 * The handler interface for receiving {@link MNUserProfileView MNUserProfileView} events.
 * By implementing <code>IMNUserProfileViewEventHandler</code> interface,
 * class can respond to user actions.
 */
public interface IMNUserProfileViewEventHandler
 {
  /**
   * Invoked when users clicks on "Go back" button or link.
   */
  void mnUserProfileViewDoGoBack ();
 }

