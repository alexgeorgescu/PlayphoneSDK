//
//  MNWSUserGameCookie.java
//  MultiNet client
//
//  Copyright 2011 PlayPhone. All rights reserved.
//

package com.playphone.multinet.core.ws.data;

public class MNWSUserGameCookie extends MNWSGenericItem
 {
  public Long getUserId ()
   {
    return getLongValue("user_id");
   }

  public Integer getCookieKey ()
   {
    return getIntegerValue("cookie_id");
   }

  public String getCookieValue ()
   {
    return getValueByName("cookie_value");
   }
 }
