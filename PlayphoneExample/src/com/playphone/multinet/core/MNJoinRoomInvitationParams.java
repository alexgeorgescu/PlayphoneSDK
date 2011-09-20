//
//  MNJoinRoomInvitationParams.java
//  MultiNet client
//
//  Copyright 2009 PlayPhone. All rights reserved.
//

package com.playphone.multinet.core;

/**
 * A class representing room invitation.
 */
public class MNJoinRoomInvitationParams
 {
  /**
   * Invitation sender SmartFox user id.
   */
  public int    fromUserSFId;

  /**
   * Invitation sender name.
   */
  public String fromUserName;

  /**
   * SmartFox room id.
   */
  public int    roomSFId;

  /**
   * Room name.
   */
  public String roomName;

  /**
   * Game id.
   */
  public int    roomGameId;

  /**
   * Gameset id.
   */
  public int    roomGameSetId;

  /**
   * Invitation text.
   */
  public String inviteText;

  /**
   * Constructs new <code>MNJoinRoomInvitationParams</code> object.
   *
   * @param fromUserSFId invitation sender SmartFox id
   * @param fromUserName invitation sender name
   * @param roomSFId SmartFox room id
   * @param roomname room name
   * @param roomGameId game id
   * @param roomGameSetId gameset id
   * @param inviteText invitation text
   */
  public MNJoinRoomInvitationParams (int fromUserSFId, String fromUserName,
                                     int roomSFId, String roomName,
                                     int roomGameId, int roomGameSetId,
                                     String inviteText)
   {
    this.fromUserSFId  = fromUserSFId;
    this.fromUserName  = fromUserName;
    this.roomSFId      = roomSFId;
    this.roomName      = roomName;
    this.roomGameId    = roomGameId;
    this.roomGameSetId = roomGameSetId;
    this.inviteText    = inviteText;
   }
 }

