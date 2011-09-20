//
//  MNInstallReferrerReceiver.java
//  MultiNet client
//
//  Copyright 2011 PlayPhone. All rights reserved.
//

package com.playphone.multinet.core;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileNotFoundException;

import android.os.Bundle;
import android.content.Context;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.content.pm.ActivityInfo;
import android.util.Log;

public class MNInstallReferrerReceiver extends BroadcastReceiver
 {
  @Override
  public void onReceive (Context context, Intent intent)
   {
    final String referrer = intent.getStringExtra("referrer");

    if (referrer != null)
     {
      try
       {
        final MNVarStorage storage = getVarStorage(context);

        storage.setValue(MNSession.INSTALL_REFERRER_VAR_NAME,referrer);
        storage.writeToFile(MNSession.VAR_STORAGE_FILE_NAME);
       }
      catch (Exception e)
       {
        Log.e(TAG,"storing install referrer caused exception to be thrown",e);
       }
     }

    callSubReceivers(context,intent);
   }

  private void callSubReceivers (Context context, Intent intent)
   {
    Bundle         metaData       = null;
    PackageManager packageManager = context.getPackageManager();
    final String   selfClassName  = getClass().getName();

    try
     {
      ActivityInfo receiverInfo = packageManager.getReceiverInfo
                                   (new ComponentName(context,selfClassName),
                                    PackageManager.GET_META_DATA);

      metaData = receiverInfo.metaData;
     }
    catch (PackageManager.NameNotFoundException e)
     {
      Log.w(TAG,e);
     }

    if (metaData != null)
     {
      String subReceiversList = metaData.getString(SUBRECEIVERS_META_NAME);

      if (subReceiversList != null)
       {
        String[] subReceivers = subReceiversList.split(" ");

        for (String subReceiverName : subReceivers)
         {
          if (!selfClassName.equals(subReceiverName))
           {
            try
             {
              Class  subReceiverClass = Class.forName(subReceiverName);
              Object subReceiver      = subReceiverClass.newInstance();

              if (subReceiver instanceof BroadcastReceiver)
               {
                ((BroadcastReceiver)subReceiver).onReceive(context,intent);
               }
              else
               {
                Log.w(TAG,"class " + subReceiverName + " is not a broadcast receiver");
               }
             }
            catch (Throwable e)
             {
              Log.w(TAG,e);
             }
           }
          else
           {
            Log.w(TAG,selfClassName + " should not be used as a sub-receiver");
           }
         }
       }
     }
   }

  private static MNVarStorage getVarStorage (final Context context)
   {
    return new MNVarStorage(new MNVarStorage.IFileStreamFactory()
     {
      public InputStream  openFileForInput  (String name) throws FileNotFoundException
       {
        return context.openFileInput(name);
       }

      public OutputStream openFileForOutput (String name) throws FileNotFoundException
       {
        return context.openFileOutput(name,0);
       }
     },
     MNSession.VAR_STORAGE_FILE_NAME);
   }

  private static final String SUBRECEIVERS_META_NAME = "com.playphone.multinet.subreceivers";
  private static final String TAG = "MNInstallReferrerReceiver";
 }
