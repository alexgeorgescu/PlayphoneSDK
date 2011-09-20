//
//  IMNWSRequestEventHandler.java
//  MultiNet client
//
//  Copyright 2011 PlayPhone. All rights reserved.
//

package com.playphone.multinet.core.ws;

public interface IMNWSRequestEventHandler
 {
  public void onRequestCompleted (MNWSResponse     response);
  public void onRequestError     (MNWSRequestError error);
//public void onRequestCancelled ();
 }

