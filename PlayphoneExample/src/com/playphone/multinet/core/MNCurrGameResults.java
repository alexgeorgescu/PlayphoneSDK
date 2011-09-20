//
//  MNCurrGameResults.java
//  MultiNet client
//
//  Copyright 2009 PlayPhone. All rights reserved.
//

package com.playphone.multinet.core;

import com.playphone.multinet.MNUserInfo;

/**
 * A class representing game results.
 */
public class MNCurrGameResults
 {
  /**
   * Game id.
   */
  public int          gameId;

  /**
   * Gameset id.
   */
  public int          gameSetId;

  /**
   * A boolean value that determines whether results are final.
   */
  public boolean      finalResult;

  /**
   * Round number.
   */
  public long         playRoundNumber;

  /**
   * Array of places players scored.
   */
  public int[]        userPlaces;

  /**
   * Array of scores players achieved.
   */
  public long[]        userScores;

  /**
   * Array of players.
   */
  public MNUserInfo[] users;

  /**
   * Constructs a new <code>MNCurrGameResults</code> object.
   *
   * @param gameId game id
   * @param gameSetId gameset id
   * @param finalResult a boolean value that determines whether results are final
   * @param playRoundNumber round number
   * @param userPlaces array of places players scored
   * @param userScores array of scores players achieved
   * @param users array of players
   */
  public MNCurrGameResults (int gameId, int gameSetId, boolean finalResult,
                            long playRoundNumber, int[] userPlaces,
                            long[] userScores, MNUserInfo[] users)
   {
    this.gameId          = gameId;
    this.gameSetId       = gameSetId;
    this.finalResult     = finalResult;
    this.playRoundNumber = playRoundNumber;
    this.userPlaces      = userPlaces;
    this.userScores      = userScores;
    this.users           = users;
   }
 }

