//
//  MNGameParams.java
//  MultiNet client
//
//  Copyright 2009 PlayPhone. All rights reserved.
//

package com.playphone.multinet;

import java.util.concurrent.ConcurrentHashMap;

/**
 * A class representing game parameters.
 */
public class MNGameParams
 {
  public static final int MN_GAMESET_ID_DEFAULT = 0;

  public static final int MN_PLAYMODEL_SINGLEPLAY     = 0x0000;
  public static final int MN_PLAYMODEL_SINGLEPLAY_NET = 0x0100;
  public static final int MN_PLAYMODEL_MULTIPLAY      = 0x1000;

  /**
   * Gameset id
   */
  public int                              gameSetId;

  /**
   * Gameset parameters
   */
  public String                           gameSetParams;

  /**
   * Score accounting service link (issued by server)
   */
  public String                           scorePostLinkId;

  /**
   * Random number generator seed
   */
  public int                              gameSeed;

  /**
   * Gameset play parameters
   */
  public ConcurrentHashMap<String,String> gameSetPlayParams;

  /**
   * Play model, one of the MN_PLAYMODEL_SINGLEPLAY, MN_PLAYMODEL_SINGLEPLAY_NET,
   * or MN_PLAYMODEL_MULTIPLAY.
   */
  public int playModel;

  /**
   * Constructs new <code>MNGameParams</code> object.
   *
   * @param gameSetId gameset id
   * @param gameSetParams gameset parameters
   * @param scorePostLinkId score accounting service link (issued by server)
   * @param gameSeed random number generator seed
   */
  public MNGameParams (int gameSetId, String gameSetParams,
                       String scorePostLinkId, int gameSeed, int playModel)
   {
    this.gameSetId         = gameSetId;
    this.gameSetParams     = gameSetParams;
    this.scorePostLinkId   = scorePostLinkId;
    this.gameSeed          = gameSeed;
    this.gameSetPlayParams = new ConcurrentHashMap<String,String>();
    this.playModel         = playModel;
   }

  /**
   * Appends the specified parameter to the set of gameset play parameters.
   *
   * @param paramName parameter name
   * @param paramValue parameter value
   */
  public void addGameSetPlayParam (String paramName, String paramValue)
   {
    gameSetPlayParams.put(paramName,paramValue);
   }

  /**
   * Returns value for specified gameset play parameter.
   *
   * @param paramValue gameset parameter
   * @return specified parameter value or <code>null</code> if
   * parameter could not be found
   */
  public String getGameSetPlayParamByName (String paramName)
   {
    return gameSetPlayParams.get(paramName);
   }
 }

