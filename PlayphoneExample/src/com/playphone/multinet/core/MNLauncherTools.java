//
//  MNLauncherTools.java
//  MultiNet client
//
//  Copyright 2010 PlayPhone. All rights reserved.
//
package com.playphone.multinet.core;

import android.content.Intent;
import android.content.Context;

class MNLauncherTools
 {
  static boolean isApplicationInstalled (Context context, String appPackageName)
   {
    boolean isInstalled = true;

    try
     {
      context.getPackageManager().getApplicationInfo(appPackageName,0);
     }
    catch (Exception e)
     {
      isInstalled = false;
     }

    return isInstalled;
   }

  static boolean launchApplication (Context context, String appPackageName, String param)
   {
    boolean ok = false;

    try
     {
      Intent intent = context.getPackageManager().getLaunchIntentForPackage(appPackageName);

      if (intent != null)
       {
        intent.putExtra(LAUNCH_PARAM_INTENT_EXTRA_NAME,param);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        context.startActivity(intent);

        ok = true;
       }
     }
    catch (Exception e)
     {
     }

    return ok;
   }

  static String getLaunchParam (Intent intent)
   {
    return intent.getStringExtra(LAUNCH_PARAM_INTENT_EXTRA_NAME);
   }

  private static final String INSTANCE_ID                    = "playphone";
  private static final String LAUNCH_PARAM_INTENT_EXTRA_NAME = "com_" + INSTANCE_ID + "_game_start_param";
 }

