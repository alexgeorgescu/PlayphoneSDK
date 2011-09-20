//
//  MNWSAnyGameItem.java
//  MultiNet client
//
//  Copyright 2011 PlayPhone. All rights reserved.
//

package com.playphone.multinet.core.ws.data;

public class MNWSAnyGameItem extends MNWSGenericItem
 {
  public Integer getGameId ()
   {
    return getIntegerValue("game_id");
   }

  public String getGameName ()
   {
    return getValueByName("game_name");
   }

  public String getGameDesc ()
   {
    return getValueByName("game_desc");
   }

  public Integer getGameGenreId ()
   {
    return getIntegerValue("gamegenre_id");
   }

  public Long getGameFlags ()
   {
    return getLongValue("game_flags");
   }

  public Integer getGameStatus ()
   {
    return getIntegerValue("game_status");
   }

  public Integer getGamePlayModel ()
   {
    return getIntegerValue("game_play_model");
   }

  public String getGameIconUrl ()
   {
    return getValueByName("game_icon_url");
   }

  public Long getDeveloperId ()
   {
    return getLongValue("developer_id");
   }
 }
