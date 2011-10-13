//
//  MNInAppBillingService.java
//  MultiNet client
//
//  Copyright 2011 PlayPhone. All rights reserved.
//

package com.playphone.multinet.core.inappbilling;

import android.os.IBinder;
import android.app.Service;
import android.content.Intent;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.ComponentName;

import com.android.vending.billing.IMarketBillingService;

public class MNInAppBillingService extends Service implements ServiceConnection
 {
  public static final String REQUEST_TYPE_CHECK_BILLING_SUPPORTED  = "CHECK_BILLING_SUPPORTED";
  public static final String REQUEST_TYPE_REQUEST_PURCHASE         = "REQUEST_PURCHASE";
  public static final String REQUEST_TYPE_GET_PURCHASE_INFORMATION = "GET_PURCHASE_INFORMATION";
  public static final String REQUEST_TYPE_CONFIRM_NOTIFICATIONS    = "CONFIRM_NOTIFICATIONS";

  public static final String REQUEST_BUNDLE_KEY_BILLING_REQUEST   = "BILLING_REQUEST";
  public static final String REQUEST_BUNDLE_KEY_API_VERSION       = "API_VERSION";
  public static final String REQUEST_BUNDLE_KEY_PACKAGE_NAME      = "PACKAGE_NAME";
  public static final String REQUEST_BUNDLE_KEY_ITEM_ID           = "ITEM_ID";
  public static final String REQUEST_BUNDLE_KEY_DEVELOPER_PAYLOAD = "DEVELOPER_PAYLOAD";
  public static final String REQUEST_BUNDLE_KEY_REQUEST_NONCE     = "NONCE";
  public static final String REQUEST_BUNDLE_KEY_NOTIFY_IDS        = "NOTIFY_IDS";

  public static final String RESPONSE_BUNDLE_KEY_RESPONSE_CODE   = "RESPONSE_CODE";
  public static final String RESPONSE_BUNDLE_KEY_REQUEST_ID      = "REQUEST_ID";
  public static final String RESPONSE_BUNDLE_KEY_PURCHASE_INTENT = "PURCHASE_INTENT";

  public static final int RESPONSE_CODE_RESULT_OK                  = 0;
  public static final int RESPONSE_CODE_RESULT_USER_CANCELED       = 1;
  public static final int RESPONSE_CODE_RESULT_SERVICE_UNAVAILABLE = 2;
  public static final int RESPONSE_CODE_RESULT_BILLING_UNAVAILABLE = 3;
  public static final int RESPONSE_CODE_RESULT_ITEM_UNAVAILABLE    = 4;
  public static final int RESPONSE_CODE_RESULT_DEVELOPER_ERROR     = 5;
  public static final int RESPONSE_CODE_RESULT_ERROR               = 6;

  public static final long INVALID_REQUEST_ID = -1;
  @Override
  public void onCreate ()
   {
    super.onCreate();

    MNInAppBillingLog.i(this,"service created");
   }

  @Override
  public void onDestroy ()
   {
    MNInAppBillingLog.i(this,"service destroyed");

    unbindMarketService();

    super.onDestroy();
   }

  @Override
  public void onStart (Intent intent, int startId)
   {
    handleStart();
   }

  @Override
  public int onStartCommand (Intent intent, int flags, int startId)
   {
    handleStart();

    return START_STICKY;
   }

  @Override
  public IBinder onBind (Intent intent)
   {
    return null;
   }

  private void handleStart ()
   {
    MNInAppBillingLog.i(this,"service started");

    bindMarketService();
   }

  private void bindMarketService ()
   {
    if (bindService(new Intent(MARKET_BILLING_SERVICE_ACTION),
                               this,
                               Context.BIND_AUTO_CREATE))
     {
      MNInAppBillingLog.i(this,"market service bound");
     }
    else
     {
      MNInAppBillingLog.i(this,"market service bind error");
     }
   }

  private void unbindMarketService ()
   {
    unbindService(this);
   }

  public void onServiceConnected (ComponentName name, IBinder service)
   {
    MNInAppBilling.attachMarketService
     (IMarketBillingService.Stub.asInterface(service));
   }

  public void onServiceDisconnected (ComponentName name)
   {
    MNInAppBilling.attachMarketService(null);
   }

  private static final String MARKET_BILLING_SERVICE_ACTION = "com.android.vending.billing.MarketBillingService.BIND";
 }

