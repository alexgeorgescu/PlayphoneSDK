//
//  MNWSLeaderboardListItem.java
//  MultiNet client
//
//  Copyright 2011 PlayPhone. All rights reserved.
//

package com.playphone.multinet.core.ws.data;

public class MNWSLeaderboardListItem extends MNWSGenericItem
 {
  public Long getUserId ()
   {
    return getLongValue("user_id");
   }

  public String getUserNickName ()
   {
    return getValueByName("user_nick_name");
   }

  public String getUserAvatarUrl ()
   {
    return getValueByName("user_avatar_url");
   }

  public Boolean getUserIsFriend ()
   {
    return getBooleanValue("user_is_friend");
   }

  public Boolean getUserOnlineNow ()
   {
    return getBooleanValue("user_online_now");
   }

  public Integer getUserSfid ()
   {
    return getIntegerValue("user_sfid");
   }

  public Boolean getUserIsIgnored ()
   {
    return getBooleanValue("user_is_ignored");
   }

  public String getUserLocale ()
   {
    return getValueByName("user_locale");
   }

  public Long getOutHiScore ()
   {
    return getLongValue("out_hi_score");
   }

  public String getOutHiScoreText ()
   {
    return getValueByName("out_hi_score_text");
   }

  public Long getOutHiDateTime ()
   {
    return getLongValue("out_hi_datetime");
   }

  public Long getOutHiDateTimeDiff ()
   {
    return getLongValue("out_hi_datetime_diff");
   }

  public Long getOutUserPlace ()
   {
    return getLongValue("out_user_place");
   }

  public Integer getGameId ()
   {
    return getIntegerValue("game_id");
   }

  public Integer getGamesetId ()
   {
    return getIntegerValue("gameset_id");
   }

  public String getUserAchievementsList ()
   {
    return getValueByName("user_achievemenets_list");
   }
 }
