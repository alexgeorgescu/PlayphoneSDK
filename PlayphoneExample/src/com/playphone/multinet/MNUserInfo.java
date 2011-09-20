//
//  MNUserInfo.java
//  MultiNet client
//
//  Copyright 2009 PlayPhone. All rights reserved.
//

package com.playphone.multinet;

/**
 * A class representing user information.
 */
public class MNUserInfo
 {
  /**
   * MultiNet user id.
   * If user id is unknown or undefined, this field is set to
   * <code>MNConst.MN_USER_ID_UNDEFINED</code>.
   */
  public long   userId;


  /**
   * SmartFox user id.
   * If user id is unknown or undefined, this field is set to
   * <code>MNConst.MN_USER_SFID_UNDEFINED</code>.
   */
  public int    userSFId;

  /**
   * User name.
   */
  public String userName;

  /**
   * Constructs new <code>MNUserInfo</code> with default parameters.
   */
  public MNUserInfo ()
   {
    userId     = MNConst.MN_USER_ID_UNDEFINED;
    userSFId   = MNConst.MN_USER_SFID_UNDEFINED;
    userName   = null;
    webBaseUrl = null;
   }

  /**
   * Constructs new <code>MNUserInfo</code>.
   *
   * @param userId MultiNet user id
   * @param userSFId SmartFox user id
   * @param userName user name
   * @param webBaseUrl base Url of information server
   */
  public MNUserInfo (long userId, int userSFId, String userName, String webBaseUrl)
   {
    this.userId     = userId;
    this.userSFId   = userSFId;
    this.userName   = userName;
    this.webBaseUrl = webBaseUrl;
   }

  public String getAvatarUrl ()
   {
    if (webBaseUrl == null)
     {
      return null;
     }

    return webBaseUrl + "/" + AVATAR_URL_SUFFIX + Long.toString(userId);
   }

  private String webBaseUrl;
  private String AVATAR_URL_SUFFIX = "user_image_data.php?sn_id=0&user_id=";
 }

