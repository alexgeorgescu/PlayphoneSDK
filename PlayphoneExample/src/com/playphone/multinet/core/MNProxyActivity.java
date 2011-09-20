//
//  MNProxyActivity.java
//  MultiNet client
//
//  Copyright 2010 PlayPhone. All rights reserved.
//

package com.playphone.multinet.core;

import java.util.HashMap;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.content.Context;
import android.content.ActivityNotFoundException;
import android.util.Log;

public class MNProxyActivity extends Activity
 {
  public static boolean startProxyActivity (Context context, ActivityDelegate delegate) throws MNException
   {
    boolean ok  = false;
    Long    key = null;

    if (delegate == null)
     {
      Log.w(TAG,"starting activity without delegate");
     }

    try
     {
      key = registerDelegate(delegate);

      Intent intent = new Intent(context,MNProxyActivity.class);

      intent.putExtra(DELEGATE_ID_EXTRA_NAME,key.longValue());

      context.startActivity(intent);

      ok = true;
     }
    catch (ActivityNotFoundException e)
     {
      Log.e(TAG,"MNProxyActivity is not registered in AndroidManifest.xml");
     }
    catch (Exception e)
     {
      e.printStackTrace();
     }

    if (!ok && key != null)
     {
      synchronized (delegates)
       {
        delegates.remove(key);
       }
     }

    return ok;
   }

  @Override
  public void onCreate(Bundle savedInstanceState)
   {
    super.onCreate(savedInstanceState);

    long id = getIntent().getLongExtra(DELEGATE_ID_EXTRA_NAME,Long.MAX_VALUE);

    synchronized (delegates)
     {
      Long key = new Long(id);

      delegate = delegates.get(key);

      delegates.remove(key);
     }

    if (delegate != null)
     {
      delegate.onCreate(this,savedInstanceState);
     }
    else
     {
      Log.w(TAG,"couldn't find correspondent delegate in onCreate");
     }
   }

  @Override
  public void onActivityResult (int requestCode, int resultCode, Intent data)
   {
    if (delegate != null)
     {
      delegate.onActivityResult(this,requestCode,resultCode,data);
     }
   }

  @Override
  public void onDestroy ()
   {
    if (delegate != null)
     {
      delegate.onDestroy(this);

      delegate = null;
     }

    super.onDestroy();
   }

  private static Long registerDelegate (ActivityDelegate delegate)
   {
    Long key = null;

    synchronized (delegates)
     {
      long id = 0;

      do
       {
        id++;

        if (id < Long.MAX_VALUE)
         {
          key = new Long(id);
         }
        else
         {
          throw new RuntimeException("registerDelegate failed with id been to large");
         }
       }
      while (delegates.containsKey(key));

      delegates.put(key,delegate);
     }

    return key;
   }

  public static class ActivityDelegate
   {
    public void onCreate (Activity activity, Bundle savedInstanceState)
     {
     }

    public void onDestroy (Activity activity)
     {
     }

    public void onActivityResult (Activity activity, int requestCode, int resultCode, Intent data)
     {
     }
   }

  private ActivityDelegate delegate;

  private static HashMap<Long,ActivityDelegate> delegates = new HashMap<Long,ActivityDelegate>();

  private static final String DELEGATE_ID_EXTRA_NAME = "MNProxyActivityDelegateId";
  private static final String TAG = "MNProxyActivity";
 }

