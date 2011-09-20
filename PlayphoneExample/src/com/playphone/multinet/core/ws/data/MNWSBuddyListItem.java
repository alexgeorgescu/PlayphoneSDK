//
//  MNWSBuddyListItem.java
//  MultiNet client
//
//  Copyright 2011 PlayPhone. All rights reserved.
//

package com.playphone.multinet.core.ws.data;

public class MNWSBuddyListItem extends MNWSGenericItem
 {
  public Long getFriendUserId ()
   {
    return getLongValue("friend_user_id");
   }

  public String getFriendUserNickName ()
   {
    return getValueByName("friend_user_nick_name");
   }

  public String getFriendSnIdList ()
   {
    return getValueByName("friend_sn_id_list");
   }

  public String getFriendSnUserAsnIdList ()
   {
    return getValueByName("friend_sn_user_asnid_list");
   }

  public Integer getFriendInGameId ()
   {
    return getIntegerValue("friend_in_game_id");
   }

  public String getFriendInGameName ()
   {
    return getValueByName("friend_in_game_name");
   }

  public String getFriendInGameIconUrl ()
   {
    return getValueByName("friend_in_game_icon_url");
   }

  public Boolean getFriendHasCurrentGame ()
   {
    return getBooleanValue("friend_has_current_game");
   }

  public String getFriendUserLocale ()
   {
    return getValueByName("friend_user_locale");
   }

  public String getFriendUserAvatarUrl ()
   {
    return getValueByName("friend_user_avatar_url");
   }

  public Boolean getFriendUserOnlineNow ()
   {
    return getBooleanValue("friend_user_online_now");
   }

  public Integer getFriendUserSfid ()
   {
    return getIntegerValue("friend_user_sfid");
   }

  public Integer getFriendSnId ()
   {
    return getIntegerValue("friend_sn_id");
   }

  public Long getFriendSnUserAsnId ()
   {
    return getLongValue("friend_sn_user_asnid");
   }

  public Long getFriendFlags ()
   {
    return getLongValue("friend_flags");
   }

  public Boolean getFriendIsIgnored ()
   {
    return getBooleanValue("friend_is_ignored");
   }

  public Integer getFriendInRoomSfid ()
   {
    return getIntegerValue("friend_in_room_sfid");
   }

  public Boolean getFriendInRoomIsLobby ()
   {
    return getBooleanValue("friend_in_room_is_lobby");
   }

  public String getFriendCurrGameAchievementsList ()
   {
    return getValueByName("friend_curr_game_achievemenets_list");
   }
 }
