//
//  MNWSGenericItem.java
//  MultiNet client
//
//  Copyright 2011 PlayPhone. All rights reserved.
//

package com.playphone.multinet.core.ws.data;

import java.util.HashMap;

public class MNWSGenericItem
 {
  public MNWSGenericItem ()
   {
    data = new HashMap<String,String>();
   }

  public String getValueByName (String name)
   {
    return data.get(name);
   }

  public void putValue (String name, String value)
   {
    data.put(name,value);
   }

  public Integer getIntegerValue (String name)
   {
    String val = data.get(name);

    if (val == null)
     {
      return null;
     }

    try
     {
      return Integer.valueOf(val);
     }
    catch (NumberFormatException e)
     {
      return null;
     }
   }

  public Long getLongValue (String name)
   {
    String val = data.get(name);

    if (val == null)
     {
      return null;
     }

    try
     {
      return Long.valueOf(val);
     }
    catch (NumberFormatException e)
     {
      return null;
     }
   }

  public Boolean getBooleanValue (String name)
   {
    String val = data.get(name);

    if (val == null)
     {
      return null;
     }
    else if (val.equals("true"))
     {
      return Boolean.TRUE;
     }
    else if (val.equals("false"))
     {
      return Boolean.FALSE;
     }
    else
     {
      return null;
     }
   }

  protected HashMap<String,String> data;
 }

