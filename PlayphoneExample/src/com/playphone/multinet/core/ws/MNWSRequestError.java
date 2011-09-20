//
//  MNWSRequestError.java
//  MultiNet client
//
//  Copyright 2011 PlayPhone. All rights reserved.
//

package com.playphone.multinet.core.ws;

public class MNWSRequestError
 {
  public static final int TRANSPORT_ERROR  = 0;
  public static final int SERVER_ERROR     = 1;
  public static final int PARSE_ERROR      = 2;
  public static final int PARAMETERS_ERROR = 3;

  public MNWSRequestError (int    domain,
                           String message)
   {
    this.domain  = domain;
    this.message = message;
   }

  public int getDomain ()
   {
    return domain;
   }

  public String getMessage ()
   {
    return message;
   }

  private int    domain;
  private String message;
 }

