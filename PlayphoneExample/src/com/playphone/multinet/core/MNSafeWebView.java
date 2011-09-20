//
//  MNSafeWebView.java
//  MultiNet client
//
//  Copyright 2011 PlayPhone. All rights reserved.
//

package com.playphone.multinet.core;

import android.content.Context;
import android.webkit.WebView;
import android.webkit.WebSettings;
import android.webkit.WebChromeClient;
import android.webkit.WebViewClient;
import android.view.View;
import android.view.ViewGroup;
import android.util.Log;

class MNSafeWebView extends WebView
 {
  public MNSafeWebView (Context context)
   {
    super(context);

    alive = true;
   }

  public synchronized void loadUrl (String url)
   {
    if (alive)
     {
      super.loadUrl(url);
     }
    else
     {
      logDestroyedCall("loadUrl");
     }
   }

  public synchronized void loadDataWithBaseURL(String baseUrl, String data, String mimeType, String encoding, String historyUrl)
   {
    if (alive)
     {
      super.loadDataWithBaseURL(baseUrl,data,mimeType,encoding,historyUrl);
     }
    else
     {
      logDestroyedCall("loadDataWithBaseURL");
     }
   }

  public synchronized String getUrl ()
   {
    if (alive)
     {
      return super.getUrl();
     }
    else
     {
      logDestroyedCall("getUrl");

      return "";
     }
   }

  public synchronized void destroySafe ()
   {
    if (alive)
     {
      destroy();

      alive = false;
     }
    else
     {
      logDestroyedCall("destroy");
     }
   }

  public synchronized boolean postSafe (Runnable action)
   {
    if (alive)
     {
      return post(action);
     }
    else
     {
      logDestroyedCall("post");

      return false;
     }
   }

  public synchronized void setVisibilitySafe (int visibility)
   {
    if (alive)
     {
      setVisibility(visibility);
     }
    else
     {
      logDestroyedCall("setVisibility");
     }
   }

  public synchronized void setLayoutParamsSafe (ViewGroup.LayoutParams params)
   {
    if (alive)
     {
      setLayoutParams(params);
     }
    else
     {
      logDestroyedCall("setLayoutParams");
     }
   }

  public synchronized void requestLayoutSafe ()
   {
    if (alive)
     {
      requestLayout();
     }
    else
     {
      logDestroyedCall("requestLayout");
     }
   }

  public synchronized int getVisibilitySafe ()
   {
    if (alive)
     {
      return getVisibility();
     }
    else
     {
      logDestroyedCall("getVisibility");

      return View.GONE;
     }
   }

  public synchronized int getHeightSafe ()
   {
    if (alive)
     {
      return super.getHeight();
     }
    else
     {
      logDestroyedCall("getHeight");

      return 1;
     }
   }

  private void logDestroyedCall (String funcName)
   {
    Log.w(TAG,"method " + funcName + " called on destroyed webView instance");
   }

  private boolean alive;

  private static final String TAG = "MNSafeWebView";
 }
