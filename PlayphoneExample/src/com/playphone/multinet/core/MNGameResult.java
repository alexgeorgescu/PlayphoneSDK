//
//  MNGameResult.java
//  MultiNet client
//
//  Copyright 2009 PlayPhone. All rights reserved.
//

package com.playphone.multinet.core;

import com.playphone.multinet.MNGameParams;

/**
 * A class representing game result
 */
public class MNGameResult
 {
  /**
   * Score value.
   */
  public long   score;

  /**
   * Score accounting service link (issued by server).
   */
  public String scorePostLinkId;

  /**
   * Gameset id.
   */
  public int    gameSetId;

  /**
   * Constructs new <code>MNGameResult</code>.
   *
   * @param gameParams game parameters
   */
  public MNGameResult (MNGameParams gameParams)
   {
    score = 0;

    if (gameParams != null)
     {
      this.scorePostLinkId = gameParams.scorePostLinkId;
      this.gameSetId       = gameParams.gameSetId;
     }
    else
     {
      this.scorePostLinkId = null;
      this.gameSetId       = MNGameParams.MN_GAMESET_ID_DEFAULT;
     }
   }
 }

