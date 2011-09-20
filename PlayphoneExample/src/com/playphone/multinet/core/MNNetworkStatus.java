//
//  MNNetworkStatus.java
//  MultiNet client
//
//  Copyright 2009 PlayPhone. All rights reserved.
//

package com.playphone.multinet.core;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

class MNNetworkStatus
 {
  public MNNetworkStatus (IMNPlatform platform)
   {
    this.context = ((MNPlatformAndroid)platform).getContext();
   }

  public boolean haveInternetConnection ()
   {
    ConnectivityManager connManager = (ConnectivityManager)context
                                       .getSystemService
                                        (Context.CONNECTIVITY_SERVICE);
    NetworkInfo netInfo = connManager.getActiveNetworkInfo();

    if (netInfo == null)
     {
      return false;
     }

    return netInfo.isConnected();
   }

  private Context context;
 }

