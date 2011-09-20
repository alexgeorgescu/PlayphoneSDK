//
//  MNDirectEventHandlerAbstract.java
//  MultiNet client
//
//  Copyright 2009 PlayPhone. All rights reserved.
//

package com.playphone.multinet;

import com.playphone.multinet.core.MNSession;

/**
 * A class representing empty MNDirect event handler.
 * This class is an {@link IMNDirectEventHandler IMNDirectEventHandler}
 * interface implementation in which all events are ignored.
 */
public class MNDirectEventHandlerAbstract implements IMNDirectEventHandler
 {
  public void mnDirectDoStartGameWithParams  (MNGameParams   params)
   {
   }

  public void mnDirectDoFinishGame           ()
   {
   }

  public void mnDirectDoCancelGame           ()
   {
   }

  public void mnDirectViewDoGoBack           ()
   {
   }

  public void mnDirectDidReceiveGameMessage  (String         message,
                                              MNUserInfo     sender)
   {
   }

  public void mnDirectSessionStatusChanged   (int            newStatus)
   {
   }

  public void mnDirectErrorOccurred          (MNErrorInfo    error)
   {
   }

  public void mnDirectSessionReady           (MNSession      session)
   {
   }
 }

