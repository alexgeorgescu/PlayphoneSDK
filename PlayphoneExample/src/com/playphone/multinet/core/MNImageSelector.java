//
//  MNImageSelector.java
//  MultiNet client
//
//  Copyright 2010 PlayPhone. All rights reserved.
//

package com.playphone.multinet.core;

import android.app.Activity;
import android.os.Bundle;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

class MNImageSelector extends MNProxyActivity.ActivityDelegate
 {
  public static interface IEventHandler
   {
    void onImageSelected (Uri imageUri);
   }

  public static boolean showImageSelector (Context context, IEventHandler eventHandler)
   {
    MNImageSelector imageSelector = new MNImageSelector(eventHandler);

    if (!MNProxyActivity.startProxyActivity(context,imageSelector))
     {
      imageSelector.cleanup();

      return false;
     }
    else
     {
      return true;
     }
   }

  public MNImageSelector (IEventHandler eventHandler)
   {
    this.eventHandler = eventHandler;
   }

  public void cleanup ()
   {
    this.eventHandler = null;
   }

  public void onCreate (Activity activity, Bundle savedInstanceState)
   {
    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);

    intent.setType("image/*");

    activity.startActivityForResult(intent,0);
   }

  public void onDestroy (Activity activity)
   {
    cleanup();
   }

  public void onActivityResult (Activity activity, int requestCode, int resultCode, Intent data)
   {
    Uri uri = null;

    if (resultCode != Activity.RESULT_CANCELED)
     {
      uri = data.getData();
     }

    try
     {
      if (uri != null)
       {
        eventHandler.onImageSelected(uri);
       }
     }
    catch (Exception e)
     {
      e.printStackTrace();
     }

    activity.finish();
   }

  private IEventHandler eventHandler;
 }

