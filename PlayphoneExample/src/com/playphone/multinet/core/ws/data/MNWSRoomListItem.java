//
//  MNWSRoomListItem.java
//  MultiNet client
//
//  Copyright 2011 PlayPhone. All rights reserved.
//

package com.playphone.multinet.core.ws.data;

public class MNWSRoomListItem extends MNWSGenericItem
 {
  public Integer getRoomSFId ()
   {
    return getIntegerValue("room_sfid");
   }

  public String getRoomName ()
   {
    return getValueByName("room_name");
   }

  public Integer getRoomUserCount ()
   {
    return getIntegerValue("room_user_count");
   }

  public Boolean getRoomIsLobby ()
   {
    return getBooleanValue("room_is_lobby");
   }

  public Integer getGameId ()
   {
    return getIntegerValue("game_id");
   }

  public Integer getGameSetId ()
   {
    return getIntegerValue("gameset_id");
   }
 }
