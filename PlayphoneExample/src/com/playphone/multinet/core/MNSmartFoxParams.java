//
//  MNSmartFoxParams.java
//  MultiNet client
//
//  Copyright 2009 PlayPhone. All rights reserved.
//

package com.playphone.multinet.core;

class MNSmartFoxParams
 {
  public MNSmartFoxParams (String serverAddr, int serverPort,
                           String blueBoxAddr, int blueBoxPort,
                           boolean smartConnect)
   {
    this.serverAddr   = serverAddr;
    this.serverPort   = serverPort;
    this.blueBoxAddr  = blueBoxAddr;
    this.blueBoxPort  = blueBoxPort;
    this.smartConnect = smartConnect;
   }

  public String  serverAddr;
  public int     serverPort;
  public String  blueBoxAddr;
  public int     blueBoxPort;
  public boolean smartConnect;

  public static final int SMARTFOX_DEFAULT_PORT = 9339;
  public static final int BLUEBOX_DEFAULT_PORT  = 8080;
 }

