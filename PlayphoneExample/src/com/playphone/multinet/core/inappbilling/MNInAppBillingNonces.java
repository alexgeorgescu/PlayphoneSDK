//
//  MNInAppBillingNonces.java
//  MultiNet client
//
//  Copyright 2011 PlayPhone. All rights reserved.
//

package com.playphone.multinet.core.inappbilling;

import java.util.HashSet;
import java.security.SecureRandom;

public class MNInAppBillingNonces
 {
  public synchronized static long generate ()
   {
    long nonce = rng.nextLong();

    nonces.add(Long.valueOf(nonce));

    return nonce;
   }

  public synchronized static boolean checkAndRemove (long nonce)
   {
    return nonces.remove(Long.valueOf(nonce));
   }

  private static final SecureRandom  rng    = new SecureRandom();
  private static final HashSet<Long> nonces = new HashSet<Long>();
 }

