//
//  MNOfflineScores.java
//  MultiNet client
//
//  Copyright 2010 PlayPhone. All rights reserved.
//

package com.playphone.multinet.core;

class MNOfflineScores
 {
  public static boolean saveScore (MNVarStorage varStorage, long userId, int gameSetId, long score)
   {
    String varNamePrefix   = getVarNamePrefix(userId,gameSetId);
    String scoreStr        = Long.toString(score);
    String timeStr         = Long.toString(MNUtils.getUnixTime());
    String minScoreVarName = varNamePrefix + "min.score";
    String maxScoreVarName = varNamePrefix + "max.score";

    String minScoreString = varStorage.getValue(minScoreVarName);
    String maxScoreString = varStorage.getValue(maxScoreVarName);

    Long minScore = minScoreString == null ? null : MNUtils.parseLong(minScoreString);
    Long maxScore = maxScoreString == null ? null : MNUtils.parseLong(maxScoreString);

    if (minScore == null || score <= minScore)
     {
      varStorage.setValue(minScoreVarName,scoreStr);
      varStorage.setValue(varNamePrefix + "min.date",timeStr);
     }

    if (maxScore == null || score >= maxScore)
     {
      varStorage.setValue(maxScoreVarName,scoreStr);
      varStorage.setValue(varNamePrefix + "max.date",timeStr);
     }

    varStorage.setValue(varNamePrefix + "last.score",scoreStr);
    varStorage.setValue(varNamePrefix + "last.date",timeStr);

    return true;
   }

  private static String getVarNamePrefix (long userId, int gameSetId)
   {
    return "offline." + Long.toString(userId) + ".score_pending." + Integer.toString(gameSetId) + ".";
   }
 }

