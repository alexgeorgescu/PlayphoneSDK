//
//  MNException.java
//  MultiNet client
//
//  Copyright 2009 PlayPhone. All rights reserved.
//

package com.playphone.multinet.core;

/**
 * A class representing MultiNet exception.
 */
public class MNException extends RuntimeException
 {
  /**
   * Constructs new exception object with supplied message.
   *
   * @param msg error message
   */
  public MNException (String msg)
   {
    super(msg);
   }

  /**
   * Constructs new exception object with supplied message and cause.
   *
   * @param msg error message
   * @param t the cause
   */
  public MNException (String msg, Throwable t)
   {
    super(msg,t);
   }

  private static final long serialVersionUID = 97927487440199116L;
 }
