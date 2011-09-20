//
//  MNBuddyRoomParams.java
//  MultiNet client
//
//  Copyright 2009 PlayPhone. All rights reserved.
//

package com.playphone.multinet.core;

/**
 * A class representing room invitation parameters.
 */
public class MNBuddyRoomParams
 {
  /**
   * Room name
   */
  public String  roomName;

  /**
   * Room gameset id
   */
  public Integer gameSetId;

  /**
   * Comma-separated list of invitation receivers (list of MultiNet user ids)
   */
  public String  toUserIdList;

  /**
   * Comma-separated list of invitation receivers (list of SmartFox user ids)
   */
  public String  toUserSFIdList;

  /**
   * Text of invitation
   */
  public String  inviteText;

  /**
   * Constructs new <code>MNBuddyRoomParams</code> object.
   *
   * @param roomName room name
   * @param gameSetId room gameset id
   * @param toUserIdList comma-separated list of invitation receivers (list of MultiNet user ids)
   * @param toUserSFIdList comma-separated list of invitation receivers (list of SmartFox user ids)
   * @param inviteText text of invitation
   */
  public MNBuddyRoomParams (String  roomName,
                            Integer gameSetId,
                            String  toUserIdList,
                            String  toUserSFIdList,
                            String  inviteText)
   {
    this.roomName       = roomName;
    this.gameSetId      = gameSetId;
    this.toUserIdList   = toUserIdList;
    this.toUserSFIdList = toUserSFIdList;
    this.inviteText     = inviteText;
   }
 }

