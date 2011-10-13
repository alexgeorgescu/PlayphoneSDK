//
//  MNInAppBillingReceiver.java
//  MultiNet client
//
//  Copyright 2011 PlayPhone. All rights reserved.
//

package com.playphone.multinet.core.inappbilling;

import android.content.Intent;
import android.content.Context;
import android.content.BroadcastReceiver;

public class MNInAppBillingReceiver extends BroadcastReceiver
 {
  public void onReceive (Context context, Intent intent)
   {
    final String action = intent.getAction();

    if      (action.equals(ACTION_INAPP_NOTIFY))
     {
      handleInAppNotify(intent);
     }
    else if (action.equals(ACTION_RESPONSE_CODE))
     {
      handleResponseCode(intent);
     }
    else if (action.equals(ACTION_STATE_CHANGED))
     {
      handleStateChanged(intent);
     }
    else
     {
      MNInAppBillingLog.e(this,"unsupported action " + action + "received");
     }
   }

  private void handleInAppNotify (Intent intent)
   {
    MNInAppBilling.onInAppNotifyReceived(intent.getStringExtra(EXTRA_NOTIFICATION_ID));
   }

  private void handleResponseCode (Intent intent)
   {
    MNInAppBilling.onResponseCodeReceived
     (intent.getLongExtra(EXTRA_REQUEST_ID,MNInAppBillingService.INVALID_REQUEST_ID),
      intent.getIntExtra(EXTRA_RESPONSE_CODE,MNInAppBillingService.RESPONSE_CODE_RESULT_ERROR));
   }

  private void handleStateChanged (Intent intent)
   {
    MNInAppBilling.onPurchaseStateChangedReceived
     (intent.getStringExtra(EXTRA_INAPP_SIGNED_DATA),
      intent.getStringExtra(EXTRA_INAPP_SIGNATURE));
   }

  private static final String ACTION_INAPP_NOTIFY  = "com.android.vending.billing.IN_APP_NOTIFY";
  private static final String ACTION_RESPONSE_CODE = "com.android.vending.billing.RESPONSE_CODE";
  private static final String ACTION_STATE_CHANGED = "com.android.vending.billing.PURCHASE_STATE_CHANGED";

  private static final String EXTRA_NOTIFICATION_ID   = "notification_id";
  private static final String EXTRA_REQUEST_ID        = "request_id";
  private static final String EXTRA_RESPONSE_CODE     = "response_code";
  private static final String EXTRA_INAPP_SIGNED_DATA = "inapp_signed_data";
  private static final String EXTRA_INAPP_SIGNATURE   = "inapp_signature";
 }

