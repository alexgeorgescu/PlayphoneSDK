//
//  MNStrMaskStorage.java
//  MultiNet client
//
//  Copyright 2009 PlayPhone. All rights reserved.
//

package com.playphone.multinet.core;

import java.util.HashSet;

public class MNStrMaskStorage
 {
  public MNStrMaskStorage ()
   {
    masks = new HashSet<String>(STORAGE_AVERAGE_SIZE);
   }

  public synchronized void addMask (String mask)
   {
    masks.add(mask);
   }

  public synchronized void removeMask (String mask)
   {
    masks.remove(mask);
   }

  public synchronized boolean checkString (String str)
   {
    for (String mask : masks)
     {
      if (checkString(mask,str))
       {
        return true;
       }
     }

    return false;
   }

  private static boolean checkString (String mask, String str)
   {
    if (mask.endsWith(WILDCARD_STR))
     {
      return str.startsWith(mask.substring(0,mask.length() - 1));
     }
    else
     {
      return mask.equals(str);
     }
   }

  private static final int    STORAGE_AVERAGE_SIZE = 5;
  private static final String WILDCARD_STR = "*";

  private HashSet<String> masks;
 }

