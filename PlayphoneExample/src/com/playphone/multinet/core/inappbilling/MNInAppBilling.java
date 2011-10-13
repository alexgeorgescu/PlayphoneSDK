//
//  MNInAppBilling.java
//  MultiNet client
//
//  Copyright 2011 PlayPhone. All rights reserved.
//

package com.playphone.multinet.core.inappbilling;

import java.util.ArrayList;
import java.lang.reflect.Method;

import android.os.Bundle;
import android.os.RemoteException;
import android.app.Activity;
import android.app.PendingIntent;

import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;

import org.json.JSONTokener;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import com.android.vending.billing.IMarketBillingService;

public class MNInAppBilling
 {
  public static final int PURCHASE_STATE_PURCHASED = 0;
  public static final int PURCHASE_STATE_CANCELED  = 1;
  public static final int PURCHASE_STATE_REFUNDED  = 2;

  public static interface IEventHandler
   {
    public void onServiceBecomeAvailable       ();
    public void onServiceBecomeUnavailable     ();
    public void onInAppNotifyReceived          (String notificationId);
    public void onResponseCodeReceived         (long   requestId, int responseCode);
    public void onPurchaseStateChangedReceived (String signedData, String signature);
   }

  public static class SyncResponse
   {
    public SyncResponse (Bundle responseBundle)
     {
      responseCode = responseBundle.getInt
                      (MNInAppBillingService.RESPONSE_BUNDLE_KEY_RESPONSE_CODE,
                       MNInAppBillingService.RESPONSE_CODE_RESULT_ERROR);

      requestId = responseBundle.getLong
                   (MNInAppBillingService.RESPONSE_BUNDLE_KEY_REQUEST_ID,
                    MNInAppBillingService.INVALID_REQUEST_ID);

      purchaseIntent = responseBundle.getParcelable
                        (MNInAppBillingService.RESPONSE_BUNDLE_KEY_PURCHASE_INTENT);
     }

    public SyncResponse (int responseCode, long requestId, PendingIntent purchaseIntent)
     {
      this.responseCode   = responseCode;
      this.requestId      = requestId;
      this.purchaseIntent = purchaseIntent;
     }

    public boolean requestSucceeded ()
     {
      return responseCode == MNInAppBillingService.RESPONSE_CODE_RESULT_OK;
     }

    public int getResponseCode ()
     {
      return responseCode;
     }

    public long getRequestId ()
     {
      return requestId;
     }

    public PendingIntent getPurchaseIntent ()
     {
      return purchaseIntent;
     }

    public static final SyncResponse ErrorResponse =
     new SyncResponse(MNInAppBillingService.RESPONSE_CODE_RESULT_ERROR,
                      MNInAppBillingService.INVALID_REQUEST_ID,
                      null);

    private int           responseCode;
    private long          requestId;
    private PendingIntent purchaseIntent;
   }

  public static class PurchaseStateResponse
   {
    public static class Order
     {
      public Order (String notificationId, String orderId, String packageName,
                    String productId, String developerPayload, long purchaseTime,
                    int purchaseState)
       {
        this.notificationId   = notificationId;
        this.orderId          = orderId;
        this.packageName      = packageName;
        this.productId        = productId;
        this.developerPayload = developerPayload;
        this.purchaseTime     = purchaseTime;
        this.purchaseState    = purchaseState;
       }

      public String getNotificationId ()
       {
        return notificationId;
       }

      public String getOrderId ()
       {
        return orderId;
       }

      public String getPackageName ()
       {
        return packageName;
       }

      public String getProductId ()
       {
        return productId;
       }

      public String getDeveloperPayload ()
       {
        return developerPayload;
       }

      public long getPurchaseTime ()
       {
        return purchaseTime;
       }

      public int getPurchaseState ()
       {
        return purchaseState;
       }

      private String notificationId;
      private String orderId;
      private String packageName;
      private String productId;
      private String developerPayload;
      private long   purchaseTime;
      private int    purchaseState;
     }

    public PurchaseStateResponse (long nonce, Order[] orders)
     {
      this.nonce  = nonce;
      this.orders = orders;
     }

    public long getNonce ()
     {
      return nonce;
     }

    public Order[] getOrders ()
     {
      return orders;
     }

    private long    nonce;
    private Order[] orders;
   }

  public static synchronized void setEventHandler (IEventHandler eventHandler)
   {
    MNInAppBilling.eventHandler = eventHandler;
   }

  public static synchronized boolean start (Context context) throws SecurityException
   {
    if (currentContext != null)
     {
      MNInAppBillingLog.e(MNInAppBilling.class,"start called in running state, ignored");

      return false;
     }

    if (!startService(context))
     {
      MNInAppBillingLog.e(MNInAppBilling.class,"unable to start service");

      return false;
     }

    currentContext = context;

    return true;
   }

  public static synchronized void stop ()
   {
    if (currentContext == null)
     {
      MNInAppBillingLog.e(MNInAppBilling.class,"stop called in stopped state, ignored");

      return;
     }

    try
     {
      if (!stopService(currentContext))
       {
        MNInAppBillingLog.w(MNInAppBilling.class,"unable to stop the service");
       }
     }
    finally
     {
      currentContext = null;
     }
   }

  public static synchronized int checkBillingSupport () throws RemoteException
   {
    if (marketService == null)
     {
      MNInAppBillingLog.e(MNInAppBilling.class,"unable to send 'check billing supported' request - service is not bound");

      return MNInAppBillingService.RESPONSE_CODE_RESULT_SERVICE_UNAVAILABLE;
     }

    Bundle requestBundle  = makeRequestBundle(MNInAppBillingService.REQUEST_TYPE_CHECK_BILLING_SUPPORTED);
    Bundle responseBundle = marketService.sendBillingRequest(requestBundle);

    return responseBundle.getInt(MNInAppBillingService.RESPONSE_BUNDLE_KEY_RESPONSE_CODE,
                                 MNInAppBillingService.RESPONSE_CODE_RESULT_ERROR);
   }

  public static synchronized SyncResponse sendRequestPurchaseRequest
                                           (String itemId,
                                            String developerPayload) throws RemoteException
   {
    if (marketService == null)
     {
      MNInAppBillingLog.e(MNInAppBilling.class,"unable to send 'request purchase' request - service is not bound");

      return SyncResponse.ErrorResponse;
     }

    Bundle requestBundle = makeRequestBundle(MNInAppBillingService.REQUEST_TYPE_REQUEST_PURCHASE);

    requestBundle.putString(MNInAppBillingService.REQUEST_BUNDLE_KEY_ITEM_ID,
                            itemId);

    if (developerPayload != null)
     {
      requestBundle.putString
       (MNInAppBillingService.REQUEST_BUNDLE_KEY_DEVELOPER_PAYLOAD,developerPayload);
     }

    return new SyncResponse(marketService.sendBillingRequest(requestBundle));
   }

  public static synchronized SyncResponse sendGetPurchaseInformationRequest
                                           (long     requestNonce,
                                            String[] notifyIdentifiers) throws RemoteException
   {
    if (marketService == null)
     {
      MNInAppBillingLog.e(MNInAppBilling.class,"unable to send 'get purchase information' request - service is not bound");

      return SyncResponse.ErrorResponse;
     }

    Bundle requestBundle = makeRequestBundle(MNInAppBillingService.REQUEST_TYPE_GET_PURCHASE_INFORMATION);

    requestBundle.putLong
     (MNInAppBillingService.REQUEST_BUNDLE_KEY_REQUEST_NONCE,requestNonce);
    requestBundle.putStringArray
     (MNInAppBillingService.REQUEST_BUNDLE_KEY_NOTIFY_IDS,notifyIdentifiers);

    return new SyncResponse(marketService.sendBillingRequest(requestBundle));
   }

  public static synchronized SyncResponse sendConfirmNotificationsRequest
                                           (String[] notifyIdentifiers) throws RemoteException
   {
    if (marketService == null)
     {
      MNInAppBillingLog.e(MNInAppBilling.class,"unable to send 'confirm notification' request - service is not bound");

      return SyncResponse.ErrorResponse;
     }

    Bundle requestBundle = makeRequestBundle(MNInAppBillingService.REQUEST_TYPE_CONFIRM_NOTIFICATIONS);

    requestBundle.putStringArray
     (MNInAppBillingService.REQUEST_BUNDLE_KEY_NOTIFY_IDS,notifyIdentifiers);

    return new SyncResponse(marketService.sendBillingRequest(requestBundle));
   }

  public static boolean startPurchaseActivity (Activity activity, PendingIntent purchaseIntent)
   {
    Method startIntentMethod;

    boolean result = false;
    Intent  intent = new Intent();

    try
     {
      startIntentMethod = activity.getClass().getMethod
                           ("startIntentSender",StartIntentSenderMethodSignature);
     }
    catch (Exception e)
     {
      startIntentMethod = null;
     }

    if (startIntentMethod != null)
     {
      //Android 2.x+
      Integer  zero = Integer.valueOf(0);
      Object[] args = new Object[]
       {
        purchaseIntent.getIntentSender(), intent, zero, zero, zero
       };

      try
       {
        startIntentMethod.invoke(activity,args);

        result = true;
       }
      catch (Exception e)
       {
        MNInAppBillingLog.e(MNInAppBilling.class,"cannot start purchase activity: " + e.toString());
       }
     }
    else
     {
      //Android 1.6

      try
       {
        purchaseIntent.send(activity,0,intent);

        result = true;
       }
      catch (PendingIntent.CanceledException e)
       {
        MNInAppBillingLog.e(MNInAppBilling.class,"cannot start purchase activity: " + e.toString());
       }
     }

    return result;
   }

  public static PurchaseStateResponse parsePurchaseChangedData (String purchaseData)
   {
    try
     {
      JSONObject stateChangeData = new JSONObject(purchaseData);

      long nonce = stateChangeData.optLong("nonce");

      JSONArray ordersData = stateChangeData.optJSONArray("orders");
      int       orderCount = ordersData == null ? 0 : ordersData.length();

      PurchaseStateResponse.Order[] orders = new PurchaseStateResponse.Order[orderCount];

      for (int orderIndex = 0; orderIndex < orderCount; orderIndex++)
       {
        JSONObject orderData = ordersData.getJSONObject(orderIndex);

        String notificationId   = orderData.optString("notificationId",null);
        String orderId          = orderData.optString("orderId","");
        String packageName      = orderData.getString("packageName");
        String productId        = orderData.getString("productId");
        String developerPayload = orderData.optString("developerPayload");
        long   purchaseTime     = orderData.getLong("purchaseTime");
        int    purchaseState    = orderData.getInt("purchaseState");

        orders[orderIndex] = new PurchaseStateResponse.Order
                                  (notificationId,orderId,packageName,productId,
                                    developerPayload,purchaseTime,purchaseState);
       }

      return new PurchaseStateResponse(nonce,orders);
     }
    catch (JSONException e)
     {
      MNInAppBillingLog.w(MNInAppBilling.class,
                          "product state change info cannot be parsed: " + e.toString());

      return null;
     }
   }

  /* attachMarketService is called by MNInAppBillingService when connection */
  /* to market billing service has been established or droped               */
  /* package */ static synchronized void attachMarketService (IMarketBillingService service)
   {
    marketService = service;

    if (eventHandler != null)
     {
      if (service != null)
       {
        eventHandler.onServiceBecomeAvailable();
       }
      else
       {
        eventHandler.onServiceBecomeUnavailable();
       }
     }
   }

  /* on... methods are called by MNInAppBillingReceiver  */
  /* package */ static synchronized void onInAppNotifyReceived (String notificationId)
   {
    if (eventHandler != null)
     {
      eventHandler.onInAppNotifyReceived(notificationId);
     }
    else
     {
      MNInAppBillingLog.w(MNInAppBilling.class,"IN_APP_NOTIFY with notification id " + notificationId + " ignored");
     }
   }

  /* package */ static synchronized void onResponseCodeReceived (long requestId, int responseCode)
   {
    if (eventHandler != null)
     {
      eventHandler.onResponseCodeReceived(requestId,responseCode);
     }
    else
     {
      MNInAppBillingLog.w(MNInAppBilling.class,"RESPONSE_CODE for request " + Long.toString(requestId) + " ignored (response code " + Integer.toString(responseCode) + ")");
     }
   }

  /* package */ static synchronized void onPurchaseStateChangedReceived (String signedData, String signature)
   {
    if (eventHandler != null)
     {
      eventHandler.onPurchaseStateChangedReceived(signedData,signature);
     }
    else
     {
      MNInAppBillingLog.w(MNInAppBilling.class,"PURCHASE_STATE_CHANGED ignored");
     }
   }

  private static Bundle makeRequestBundle (String requestType)
   {
    Bundle request = new Bundle();

    request.putString(MNInAppBillingService.REQUEST_BUNDLE_KEY_BILLING_REQUEST,requestType);
    request.putInt(MNInAppBillingService.REQUEST_BUNDLE_KEY_API_VERSION,1);
    request.putString(MNInAppBillingService.REQUEST_BUNDLE_KEY_PACKAGE_NAME,currentContext.getPackageName());

    return request;
   }

  private static boolean startService (Context context) throws SecurityException
   {
    return context.startService(getServiceIntent(context)) != null;
   }

  private static boolean stopService (Context context) throws SecurityException
   {
    return context.stopService(getServiceIntent(context));
   }

  private static Intent getServiceIntent (Context context)
   {
    return new Intent(context,MNInAppBillingService.class);
   }

  private static Context               currentContext = null;
  private static IMarketBillingService marketService  = null;
  private static IEventHandler         eventHandler   = null;

  private static final Class[] StartIntentSenderMethodSignature = new Class[]
   {
    IntentSender.class, Intent.class, int.class, int.class, int.class
   };
 }

