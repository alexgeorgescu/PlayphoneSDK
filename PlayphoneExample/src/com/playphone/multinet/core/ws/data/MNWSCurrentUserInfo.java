//
//  MNWSCurrentUserInfo.java
//  MultiNet client
//
//  Copyright 2011 PlayPhone. All rights reserved.
//

package com.playphone.multinet.core.ws.data;

public class MNWSCurrentUserInfo extends MNWSGenericItem
 {
  public Long getUserId ()
   {
    return getLongValue("user_id");
   }

  public String getUserNickName ()
   {
    return getValueByName("user_nick_name");
   }

  public Boolean getUserAvatarExists ()
   {
    return getBooleanValue("user_avatar_exists");
   }

  public String getUserAvatarUrl ()
   {
    return getValueByName("user_avatar_url");
   }

  public Boolean getUserOnlineNow ()
   {
    return getBooleanValue("user_online_now");
   }

  public String getUserEmail ()
   {
    return getValueByName("user_email");
   }

  public Integer getUserStatus ()
   {
    return getIntegerValue("user_status");
   }

  public Boolean getUserAvatarHasCustomImg ()
   {
    return getBooleanValue("user_avatar_has_custom_img");
   }

  public Boolean getUserAvatarHasExternalUrl ()
   {
    return getBooleanValue("user_avatar_has_external_url");
   }

  public Integer getUserGamePoints ()
   {
    return getIntegerValue("user_gamepoints");
   }
 }
