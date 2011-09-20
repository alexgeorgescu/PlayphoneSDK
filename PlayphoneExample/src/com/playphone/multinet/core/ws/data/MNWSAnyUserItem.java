//
//  MNWSAnyUserItem.java
//  MultiNet client
//
//  Copyright 2011 PlayPhone. All rights reserved.
//

package com.playphone.multinet.core.ws.data;

public class MNWSAnyUserItem extends MNWSGenericItem
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

  public Long getUserGamePoints ()
   {
    return getLongValue("user_gamepoints");
   }

  public Integer getMyFriendLinkStatus ()
   {
    return getIntegerValue("my_friend_link_status");
   }
 }
