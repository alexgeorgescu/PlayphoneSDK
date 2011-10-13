//
//  MNWSRequestContent.java
//  MultiNet client
//
//  Copyright 2011 PlayPhone. All rights reserved.
//

package com.playphone.multinet.core.ws;

import java.util.HashMap;

import com.playphone.multinet.core.MNUtils;

public class MNWSRequestContent
 {
  public static final int LEADERBOARD_PERIOD_ALL_TIME   = 0;
  public static final int LEADERBOARD_PERIOD_THIS_WEEK  = 1;
  public static final int LEADERBOARD_PERIOD_THIS_MONTH = 2;

  public static final int LEADERBOARD_SCOPE_GLOBAL = 0;
  public static final int LEADERBOARD_SCOPE_LOCAL  = 1;

  public MNWSRequestContent ()
   {
    infoList = new StringBuilder();
   }

  public String addInfoBlock (String infoBlockSelector)
   {
    if (infoList.length() > 0)
     {
      infoList.append(",");
     }

    infoList.append(infoBlockSelector);

    return infoBlockSelector;
   }

  protected String addInfoBlock (String blockName, String param1)
   {
    addInfoBlock(blockName + ":" + param1);

    return blockName;
   }

  protected String addInfoBlock (String blockName, String param1, String param2)
   {
    addInfoBlock(blockName + ":" + param1 + ":" + param2);

    return blockName;
   }

  protected String addInfoBlock (String blockName, String param1, String param2, String param3)
   {
    addInfoBlock(blockName + ":" + param1 + ":" + param2 + ":" + param3);

    return blockName;
   }

  public void addNameMapping (String blockName, String parserName)
   {
    if (mapping == null)
     {
      mapping = new HashMap<String,String>();
     }

    mapping.put(blockName,parserName);
   }

  public String addCurrentUserInfo ()
   {
    return addInfoBlock("currentUser");
   }

  public String addCurrUserBuddyList ()
   {
    return addInfoBlock("currentUserBuddyList");
   }

  public String addAnyUser (long userId)
   {
    return addInfoBlock("anyUser",Long.toString(userId));
   }

  public String addAnyGame (int gameId)
   {
    return addInfoBlock("anyGame",Integer.toString(gameId));
   }

  public String addCurrGameRoomList ()
   {
    return addInfoBlock("currentGameRoomList");
   }

  public String addCurrGameRoomUserList (int roomSFId)
   {
    return addInfoBlock("currentGameRoomUserList",Integer.toString(roomSFId));
   }

  private static String getPeriodNameByCode (int period)
   {
    if      (period == LEADERBOARD_PERIOD_THIS_WEEK)
     {
      return "ThisWeek";
     }
    else if (period == LEADERBOARD_PERIOD_THIS_MONTH)
     {
      return "ThisMonth";
     }
    else
     {
      return "AllTime";
     }
   }

  private static String getScopeNameByCode (int scope)
   {
    if (scope == LEADERBOARD_SCOPE_LOCAL)
     {
      return "Local";
     }
    else
     {
      return "Global";
     }
   }

  public String addCurrUserLeaderboard (int scope, int period)
   {
    return addInfoBlock("currentUserLeaderboard" +
                        getScopeNameByCode(scope) +
                        getPeriodNameByCode(period));
   }

  public String addAnyGameLeaderboardGlobal (int gameId, int gameSetId, int period)
   {
    return addInfoBlock("anyGameLeaderboardGlobal" + getPeriodNameByCode(period),
                         Integer.toString(gameId),Integer.toString(gameSetId));
   }

  public String addAnyUserAnyGameLeaderboardGlobal (long userId, int gameId, int gameSetId, int period)
   {
    return addInfoBlock("anyUserAnyGameLeaderboardGlobal" +
                         getPeriodNameByCode(period),
                          Long.toString(userId),
                           Integer.toString(gameId),
                            Integer.toString(gameSetId));
   }

  public String addCurrUserAnyGameLeaderboardLocal (int gameId, int gameSetId, int period)
   {
    return addInfoBlock("currentUserAnyGameLeaderboardLocal" +
                         getPeriodNameByCode(period),
                           Integer.toString(gameId),
                            Integer.toString(gameSetId));
   }

  public String addAnyUserGameCookies (long[] userIdList, int[] cookieKeyList)
   {
    return addInfoBlock("anyUserGameCookies",
                        MNUtils.stringMakeLongList(userIdList,"^"),
                        MNUtils.stringMakeIntList(cookieKeyList,"^"));
   }

  protected String addCurrUserSubscriptionStatus (int socNetId)
   {
    return addInfoBlock("currentUserSubscriptionStatus",Integer.toString(socNetId));
   }

  public String addCurrUserSubscriptionStatusPlayPhone ()
   {
    return addCurrUserSubscriptionStatus(SN_ID_PLAYPHONE);
   }

  /*package*/ String getRequestInfoListString ()
   {
    return infoList.toString();
   }

  /*package*/ HashMap<String,String> getMapping ()
   {
    return mapping;
   }

  private StringBuilder          infoList;
  private HashMap<String,String> mapping;

  private static final int SN_ID_PLAYPHONE = 4;
 }

