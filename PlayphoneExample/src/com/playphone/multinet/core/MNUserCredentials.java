//
//  MNUserCredentials.java
//  MultiNet client
//
//  Copyright 2009 PlayPhone. All rights reserved.
//

package com.playphone.multinet.core;

import java.util.Date;
import java.util.Map;
import java.util.Hashtable;

class MNUserCredentials
 {
  public long   userId;
  public String userName;
  public String userAuthSign;
  public Date   lastLoginTime;
  public String userAuxInfoText;

  public MNUserCredentials (long userId, String userName,
                            String userAuthSign, Date lastLoginTime,
                            String userAuxInfoText)
   {
    this.userId          = userId;
    this.userName        = userName;
    this.userAuthSign    = userAuthSign;
    this.lastLoginTime   = lastLoginTime;
    this.userAuxInfoText = userAuxInfoText;
   }

  private static
  Hashtable<Long,MNUserCredentials> getAllCredentialsAsHash (MNVarStorage varStorage)
   {
    Map<String,String> variables = varStorage.getVariablesByMask("cred.*");
    Hashtable<Long,MNUserCredentials> users = new Hashtable<Long,MNUserCredentials>();

    for (String varName : variables.keySet())
     {
      String[] varNameParts = varName.split("\\.");

      if (varNameParts.length == 3)
       {
        Long userId = MNUtils.parseLong(varNameParts[1]);

        if (userId != null)
         {
          MNUserCredentials credentials = users.get(userId);

          if (credentials == null)
           {
            credentials = new MNUserCredentials(userId,null,null,null,null);

            users.put(userId,credentials);
           }

          String fieldName = varNameParts[2];

          if (fieldName.equals("user_id"))
           {
            /* skip var */
           }
          else if (fieldName.equals("user_name"))
           {
            credentials.userName = variables.get(varName);
           }
          else if (fieldName.equals("user_auth_sign"))
           {
            credentials.userAuthSign = variables.get(varName);
           }
          else if (fieldName.equals("user_last_login_time"))
           {
            Long interval = MNUtils.parseLong(variables.get(varName));

            if (interval != null)
             {
              credentials.lastLoginTime = new Date(interval);
             }
           }
          else if (fieldName.equals("user_aux_info_text"))
           {
            credentials.userAuxInfoText = variables.get(varName);
           }
         }
       }
     }

    return users;
   }

  public static
  MNUserCredentials[]   loadAllCredentials      (MNVarStorage varStorage)
   {
    Hashtable<Long,MNUserCredentials> users = getAllCredentialsAsHash(varStorage);

    return users.values().toArray(new MNUserCredentials[users.size()]);
   }

  public static void    wipeCredentialsByUserId (MNVarStorage varStorage,long userId)
   {
    varStorage.removeVariablesByMask(String.format("cred.%d.*",userId));
   }

  public static void    wipeAllCredentials (MNVarStorage varStorage)
   {
    varStorage.removeVariablesByMask("cred.*");
   }

  public static void    updateCredentials       (MNVarStorage varStorage,MNUserCredentials newCredentials)
   {
    long userId = newCredentials.userId;

    varStorage.setValue(String.format("cred.%d.user_id",userId),
                        Long.toString(userId));

    if (newCredentials.userName != null)
     {
      varStorage.setValue(String.format("cred.%d.user_name",userId),
                          newCredentials.userName);
     }

    if (newCredentials.userAuthSign != null)
     {
      varStorage.setValue(String.format("cred.%d.user_auth_sign",userId),
                          newCredentials.userAuthSign);
     }

    if (newCredentials.lastLoginTime != null)
     {
      varStorage.setValue(String.format("cred.%d.user_last_login_time",userId),
                          Long.toString(newCredentials.lastLoginTime.getTime()));
     }

    if (newCredentials.userAuxInfoText != null)
     {
      varStorage.setValue(String.format("cred.%d.user_aux_info_text",userId),
                          newCredentials.userAuxInfoText);
     }
   }

  public static
  MNUserCredentials     getMostRecentlyLoggedUserCredentials (MNVarStorage varStorage)
   {
    MNUserCredentials[] allCredentials = loadAllCredentials(varStorage);
    Date mostRecentLoginDate = null;
    MNUserCredentials mostRecentCredentials = null;

    for (MNUserCredentials credentials : allCredentials)
     {
      if (credentials.lastLoginTime != null)
       {
        if (mostRecentLoginDate == null ||
            credentials.lastLoginTime.after(mostRecentLoginDate))
         {
          mostRecentLoginDate   = credentials.lastLoginTime;
          mostRecentCredentials = credentials;
         }
       }
     }

    return mostRecentCredentials;
   }

  public static
  MNUserCredentials     getCredentialsByUserId (MNVarStorage varStorage, long userId)
   {
    Hashtable<Long,MNUserCredentials> users = getAllCredentialsAsHash(varStorage);

    return users.get(userId);
   }
 }

