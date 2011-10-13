//
//  MNVShopInAppBilling.java
//  MultiNet client
//
//  Copyright 2011 PlayPhone. All rights reserved.
//

package com.playphone.multinet.providers;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.ArrayList;

import android.util.Log;
import android.os.RemoteException;
import android.app.Activity;
import android.app.PendingIntent;

import com.playphone.multinet.MNConst;
import com.playphone.multinet.core.MNSession;
import com.playphone.multinet.core.MNSessionEventHandlerAbstract;
import com.playphone.multinet.core.MNUtils;
import com.playphone.multinet.core.MNBase64;
import com.playphone.multinet.core.MNPlatformAndroid;
import com.playphone.multinet.core.inappbilling.MNInAppBilling;
import com.playphone.multinet.core.inappbilling.MNInAppBillingService;
import com.playphone.multinet.core.inappbilling.MNInAppBillingNonces;

class MNVShopInAppBilling implements MNInAppBilling.IEventHandler
 {
  public MNVShopInAppBilling (MNSession session, MNVShopProvider vShopProvider)
   {
    this.session       = session;
    this.vShopProvider = vShopProvider;

    requestHelper = new MNVShopWSRequestHelper(session,new RequestHelperEventHandler());

    sessionEventHandler = new SessionEventHandler();

    orderNotificationLinks = new HashMap<String,String>();
    pendingPurchaseStateChanges = new ArrayList<PendingPurchaseStateChange>();
    requestIdClientTransactionIdMapping = new HashMap<Long,Long>();

    serviceStarted   = false;
    billingAvailable = false;

    inAppBillingActivityManager = null;

    MNInAppBilling.setEventHandler(this);
    MNInAppBilling.start(((MNPlatformAndroid)session.getPlatform()).getActivity());

    session.addEventHandler(sessionEventHandler);
   }

  public void shutdown ()
   {
    requestHelper.shutdown();

    session.removeEventHandler(sessionEventHandler);

    MNInAppBilling.setEventHandler(null);
    MNInAppBilling.stop();
   }

  private void startPurchase (String productId, long clientTransactionId)
   {
    if (!isBillingAvailable())
     {
      dispatchCheckoutFailedEvent
       (MNVShopProvider.IEventHandler.ERROR_CODE_GENERIC,
        "in-app billing service is not started or is unavailable - purchase request ignored",
        clientTransactionId);

      return;
     }

    MNInAppBilling.SyncResponse syncResponse;

    try
     {
      syncResponse = MNInAppBilling.sendRequestPurchaseRequest
                      (productId,makeDevPayload(clientTransactionId));
     }
    catch (RemoteException e)
     {
      dispatchCheckoutFailedEvent
       (MNVShopProvider.IEventHandler.ERROR_CODE_GENERIC,
        "unable to send purchase request to in-app billing service (" + e.toString() + ")",
        clientTransactionId);

      return;
     }

    if (syncResponse.requestSucceeded())
     {
      MNVShopProvider.IInAppBillingActivityManager currInAppBillingActivityManager;

      synchronized (this)
       {
        currInAppBillingActivityManager = inAppBillingActivityManager;
       }

      final Activity currActivity = ((MNPlatformAndroid)session.getPlatform()).getActivity();

      if (currInAppBillingActivityManager != null || currActivity != null)
       {
        synchronized (requestIdClientTransactionIdMapping)
         {
          requestIdClientTransactionIdMapping.put
           (Long.valueOf(syncResponse.getRequestId()),
            Long.valueOf(clientTransactionId));
         }

        processPurchasePendingEvent(productId,clientTransactionId);

        final PendingIntent purchaseIntent = syncResponse.getPurchaseIntent();

        if (currInAppBillingActivityManager != null)
         {
          currInAppBillingActivityManager.startInAppBillingActivity(purchaseIntent);
         }
        else
         {
          currActivity.runOnUiThread(new Runnable()
           {
            public void run ()
             {
              MNInAppBilling.startPurchaseActivity(currActivity,purchaseIntent);
             }
           });
         }
       }
      else
       {
        dispatchCheckoutFailedEvent
         (MNVShopProvider.IEventHandler.ERROR_CODE_GENERIC,
          "unable to display in-app billing chekout UI - current activity is undefined",
          clientTransactionId);
       }
     }
    else
     {
      dispatchCheckoutFailedEvent
       (MNVShopProvider.IEventHandler.ERROR_CODE_GENERIC,
        "in-app billing purchase request failed with response code " +
         Integer.toString(syncResponse.getResponseCode()),
        clientTransactionId);
     }
   }

  /* package */ synchronized void setInAppBillingActivityManager (MNVShopProvider.IInAppBillingActivityManager manager)
   {
    inAppBillingActivityManager = manager;
   }

  /* MNVShopInAppBilling.IEventHandler methods */

  public synchronized void onServiceBecomeAvailable ()
   {
    int status;

    try
     {
      status = MNInAppBilling.checkBillingSupport();
     }
    catch (RemoteException e)
     {
      status = MNInAppBillingService.RESPONSE_CODE_RESULT_SERVICE_UNAVAILABLE;
     }

    billingAvailable = status == MNInAppBillingService.RESPONSE_CODE_RESULT_OK;

    if (!billingAvailable)
     {
      Log.w(TAG,"in-app billing is unavailable (with status " +
                 Integer.toString(status) + ")");
     }

    serviceStarted = true;
   }

  public synchronized void onServiceBecomeUnavailable ()
   {
    serviceStarted = false;
   }

  private synchronized boolean isServiceStarted ()
   {
    return serviceStarted;
   }

  private synchronized boolean isBillingAvailable ()
   {
    return serviceStarted && billingAvailable;
   }

  public void onInAppNotifyReceived (String notificationId)
   {
    MNInAppBilling.SyncResponse syncResponse = null;

    try
     {
      syncResponse = MNInAppBilling.sendGetPurchaseInformationRequest
                      (MNInAppBillingNonces.generate(),
                       new String[] { notificationId });
     }
    catch (RemoteException e)
     {
      syncResponse = null;
     }

    if (!syncResponse.requestSucceeded())
     {
      Log.w(TAG,"sending 'get purchase information' request failed with status " +
                 Integer.toString(syncResponse.getResponseCode()));
     }
   }

  private static int inAppBillingResponseCodeToVShopErrorCode (int responseCode)
   {
    if (responseCode == MNInAppBillingService.RESPONSE_CODE_RESULT_USER_CANCELED)
     {
      return MNVShopProvider.IEventHandler.ERROR_CODE_USER_CANCEL;
     }
    else
     {
      return MNVShopProvider.IEventHandler.ERROR_CODE_GENERIC;
     }
   }

  public void onResponseCodeReceived (long requestId, int responseCode)
   {
    if (responseCode != MNInAppBillingService.RESPONSE_CODE_RESULT_OK)
     {
      Long clientTransactionId = null;

      synchronized (requestIdClientTransactionIdMapping)
       {
        clientTransactionId = requestIdClientTransactionIdMapping.remove(Long.valueOf(requestId));
       }

      if (clientTransactionId != null)
       {
        dispatchCheckoutFailedEvent
         (inAppBillingResponseCodeToVShopErrorCode(responseCode),
          "inapp-billing \"REQUEST_PURCHASE\" request failed with response code " + Integer.toString(responseCode),
          clientTransactionId.longValue());
       }
     }
   }

  private void sendWSRequest (MNUtils.HttpPostBodyStringBuilder params)
   {
    final String webServerUrl = session.getWebServerURL();

    if (webServerUrl != null)
     {
      requestHelper.sendWSRequest
       (webServerUrl + "/" + InAppBillingWebServicePath,
        params,
        MNVItemsProvider.TRANSACTION_ID_UNDEFINED);
     }
    else
     {
      Log.w(TAG,"web service url is unknown, unable to register purchase state change");
     }
   }

  private void sendSysEvent (String name, String params)
   {
    session.postSysEvent(name,params,null);
   }

  private void callPurchaseSucceededWSRequest
                (String signedData, String signature,
                 MNInAppBilling.PurchaseStateResponse.Order order,
                 String clientTransactionId,
                 String transactionStatus)
   {
    MNUtils.HttpPostBodyStringBuilder wsParams = new MNUtils.HttpPostBodyStringBuilder();

    wsParams.addParam("proc_transaction_id",order.getOrderId());
    wsParams.addParam("proc_transaction_status",transactionStatus);
    wsParams.addParam("proc_transaction_receipt",MNBase64.encode(signedData.getBytes()));
    wsParams.addParam("proc_transaction_receipt_signature",signature);
    wsParams.addParam("proc_transaction_app_store_item_id",order.getProductId());
    wsParams.addParam("proc_transaction_purchase_amount","1");
    wsParams.addParam("proc_client_transaction_id",clientTransactionId);

    sendWSRequest(wsParams);
   }

  private void processPurchaseSucceededEvent
                (String signedData, String signature,
                 MNInAppBilling.PurchaseStateResponse.Order order)
   {
    MNUtils.HttpPostBodyStringBuilder sysEventParams = new MNUtils.HttpPostBodyStringBuilder();

    String clientTransactionId = Long.toString(devPayloadGetClientTransactionId(order.getDeveloperPayload()));

    addOrderNotificationLink(order.getOrderId(),order.getNotificationId());

    sysEventParams.addParam("transactionId",order.getOrderId());
    sysEventParams.addParam("transactionPurchaseItemId",order.getProductId());
    sysEventParams.addParam("transactionPurchaseItemCount","1");
    sysEventParams.addParam("clientTransactionId",clientTransactionId);

    sendSysEvent("sys.onAppPurhcaseWillSendTransactionDoneNotify",sysEventParams.toString());

    callPurchaseSucceededWSRequest
     (signedData,signature,order,clientTransactionId,WSTransactionStatusPurchased);
   }

  private void processPurchaseRefundedEvent
                (String signedData, String signature,
                 MNInAppBilling.PurchaseStateResponse.Order order)
   {
    MNUtils.HttpPostBodyStringBuilder sysEventParams = new MNUtils.HttpPostBodyStringBuilder();

    String clientTransactionId = Long.toString(devPayloadGetClientTransactionId(order.getDeveloperPayload()));

    addOrderNotificationLink(order.getOrderId(),order.getNotificationId());

    sysEventParams.addParam("transactionId",order.getOrderId());
    sysEventParams.addParam("transactionPurchaseItemId",order.getProductId());
    sysEventParams.addParam("transactionPurchaseItemCount","1");
    sysEventParams.addParam("clientTransactionId",clientTransactionId);

    sendSysEvent("sys.onAppPurhcaseWillSendTransactionRefundNotify",sysEventParams.toString());

    callPurchaseSucceededWSRequest
     (signedData,signature,order,clientTransactionId,WSTransactionStatusRefunded);
   }


  private void processPurchaseCanceledEvent
                (String signedData, String signature,
                 MNInAppBilling.PurchaseStateResponse.Order order)
   {
    MNUtils.HttpPostBodyStringBuilder wsParams       = new MNUtils.HttpPostBodyStringBuilder();
    MNUtils.HttpPostBodyStringBuilder sysEventParams = new MNUtils.HttpPostBodyStringBuilder();

    final String errorCode    = Integer.toString(MNInAppBillingService.RESPONSE_CODE_RESULT_USER_CANCELED);
    final String errorMessage = "purchase canceled by user";

    String clientTransactionId = Long.toString(devPayloadGetClientTransactionId(order.getDeveloperPayload()));

    addOrderNotificationLink(order.getOrderId(),order.getNotificationId());

    wsParams.addParam("proc_transaction_id",order.getOrderId());
    wsParams.addParam("proc_transaction_status",WSTransactionStatusFailed);
    wsParams.addParam("proc_transaction_receipt",MNBase64.encode(signedData.getBytes()));
    wsParams.addParam("proc_transaction_receipt_signature",signature);
    wsParams.addParam("proc_transaction_app_store_item_id",order.getProductId());
    wsParams.addParam("proc_transaction_error_code",errorCode);
    wsParams.addParam("proc_transaction_error_message",errorMessage);
    wsParams.addParam("proc_client_transaction_id",clientTransactionId);

    sysEventParams.addParam("transactionId",order.getOrderId());
    sysEventParams.addParam("transactionPurchaseItemId",order.getProductId());
    sysEventParams.addParam("transactionPurchaseItemCount","1");
    sysEventParams.addParam("errorCode",errorCode);
    sysEventParams.addParam("errorMessage",errorMessage);
    sysEventParams.addParam("clientTransactionId",clientTransactionId);

    sendSysEvent("sys.onAppPurhcaseWillSendTransactionFailNotify",sysEventParams.toString());
    sendWSRequest(wsParams);
   }

  private void processPurchasePendingEvent (String productId, long clientTransactionId)
   {
    MNUtils.HttpPostBodyStringBuilder sysEventParams = new MNUtils.HttpPostBodyStringBuilder();

    sysEventParams.addParam("transactionPurchaseItemId",productId);
    sysEventParams.addParam("transactionPurchaseItemCount","1");
    sysEventParams.addParam("clientTransactionId",Long.toString(clientTransactionId));

    sendSysEvent("sys.onAppPurhcaseWillSendTransactionPendingNotify",sysEventParams.toString());
   }

  private void onPurchaseStateChangedForOrder
                (String signedData, String signature,
                 MNInAppBilling.PurchaseStateResponse.Order order)
   {
    switch (order.getPurchaseState())
     {
      case (MNInAppBilling.PURCHASE_STATE_PURCHASED) :
       {
        processPurchaseSucceededEvent(signedData,signature,order);
       } break;

      case (MNInAppBilling.PURCHASE_STATE_CANCELED)  :
       {
        processPurchaseCanceledEvent(signedData,signature,order);
       } break;

      case (MNInAppBilling.PURCHASE_STATE_REFUNDED)  :
       {
        processPurchaseRefundedEvent(signedData,signature,order);
       } break;

      default :
       {
        Log.e(TAG,"invalid purchase state (" +
                   Integer.toString(order.getPurchaseState()) +
                    "for order " + order.getOrderId());
       }
     }
   }

  public void onPurchaseStateChangedReceived (String signedData, String signature)
   {
    final MNInAppBilling.PurchaseStateResponse purchase =
     MNInAppBilling.parsePurchaseChangedData(signedData);

    if (MNInAppBillingNonces.checkAndRemove(purchase.getNonce()))
     {
      for (MNInAppBilling.PurchaseStateResponse.Order order : purchase.getOrders())
       {
        if (session.isOnline())
         {
          onPurchaseStateChangedForOrder(signedData,signature,order);
         }
        else
         {
          synchronized (pendingPurchaseStateChanges)
           {
            pendingPurchaseStateChanges.add
             (new PendingPurchaseStateChange(signedData,signature,order));
           }
         }
       }
     }
    else
     {
      Log.e(TAG,"invalid nonce value received in in-app billing response, ignoring response");
     }
   }

  private void processPendingPurchaseStateChanges ()
   {
    synchronized (pendingPurchaseStateChanges)
     {
      for (PendingPurchaseStateChange change : pendingPurchaseStateChanges)
       {
        onPurchaseStateChangedForOrder
         (change.getSignedData(),change.getSignature(),change.getOrder());
       }

      pendingPurchaseStateChanges.clear();
     }
   }

  /* IMNSessionEventHandler methods */

  private class SessionEventHandler extends MNSessionEventHandlerAbstract
   {
    private boolean isLoggedOnline (int status)
     {
      return status != MNConst.MN_OFFLINE && status != MNConst.MN_CONNECTING;
     }

    public void mnSessionStatusChanged (int newStatus, int oldStatus)
     {
      if (isLoggedOnline(newStatus) && !isLoggedOnline(oldStatus))
       {
        processPendingPurchaseStateChanges();
       }
     }

    private void handleWebCheckInAppBillingSupport (String callbackId)
     {
      session.postSysEvent("sys.checkInAppBillingSupport.Response",
                           isServiceStarted() ? (isBillingAvailable() ? "1" : "0" ) : "-1",
                           callbackId);
     }

    private void handleWebAppPurchaseStartTransaction (String eventParam)
     {
      HashMap<String,String> params = null;

      try
       {
        params = MNUtils.httpGetRequestParseParams(eventParam);
       }
      catch (UnsupportedEncodingException e)
       {
        dispatchCheckoutFailedEvent
         (MNVShopProvider.IEventHandler.ERROR_CODE_GENERIC,
          "invalid parameters in web command received",
          MNVItemsProvider.TRANSACTION_ID_UNDEFINED);

        return;
       }

      String productId           = params.get("transactionPurchaseItemId");
      long   productCount        = MNUtils.parseLongWithDefault(params.get("transactionPurchaseItemCount"),-1);
      long   clientTransactionId = MNUtils.parseLongWithDefault(params.get("clientTransactionId"),MNVItemsProvider.TRANSACTION_ID_UNDEFINED);

      if (productId == null || productCount != 1)
       {
        dispatchCheckoutFailedEvent
         (MNVShopProvider.IEventHandler.ERROR_CODE_GENERIC,
          "invalid or absent product identifier or product count in web command",
          MNVItemsProvider.TRANSACTION_ID_UNDEFINED);

        return;
       }

      startPurchase(productId,clientTransactionId);
     }

    public void mnSessionWebEventReceived (String eventName, String eventParam, String callbackId)
     {
      if      (eventName.equals("web.checkInAppBillingSupport"))
       {
        handleWebCheckInAppBillingSupport(callbackId);
       }
      else if (eventName.equals("web.doAppPurchaseStartTransaction"))
       {
        handleWebAppPurchaseStartTransaction(eventParam);
       }
     }
   }

  private void dispatchCheckoutFailedEvent (int errorCode, String errorMessage, long clientTransactionId)
   {
    vShopProvider.dispatchCheckoutFailedEvent
     (errorCode,errorMessage,clientTransactionId);

    Log.e(TAG,errorMessage);
   }

  private class RequestHelperEventHandler implements MNVShopWSRequestHelper.IEventHandler
   {
    public boolean vShopShouldParseResponse  (long   userId)
     {
      return userId == session.getMyUserId();
     }

    public void    vShopPostVItemTransaction (long    srvTransactionId,
                                              long    cliTransactionId,
                                              String  itemsToAddStr,
                                              boolean vShopTransactionEnabled)
     {
      vShopProvider.processPostVItemTransactionCmd
       (srvTransactionId,cliTransactionId,itemsToAddStr,vShopTransactionEnabled);
     }

    public void    vShopFinishTransaction    (String orderId)
     {
      String notificationId = removeOrderNotificationLink(orderId);

      if (notificationId == null)
       {
        Log.w(TAG,"cannot finish transaction for order " + orderId +
                  " - correspondent notification id is not found");

        return;
       }

      MNInAppBilling.SyncResponse response     = null;
      String                      errorMessage = null;

      try
       {
        response = MNInAppBilling.sendConfirmNotificationsRequest
                    (new String [] { notificationId });

        if (!response.requestSucceeded())
         {
          errorMessage = "response code: " + Integer.toString(response.getResponseCode());
         }
       }
      catch (RemoteException e)
       {
        errorMessage = "RemoteException thrown (" + e.toString() + ")";
       }

      if (errorMessage != null)
       {
        Log.w(TAG,"cannot finish transaction for order " + orderId +
                   " (notification id - " + notificationId + "), " +
                   errorMessage);
       }
     }

    public void    vShopWSRequestFailed      (long   clientTransactionId,
                                              int    errorCode,
                                              String errorMessage)
     {
      Log.w(TAG,"ws request failed (" + errorMessage + ") with error code " +
                 Integer.toString(errorCode) + ", client transaction id: " +
                 Long.toString(clientTransactionId));
     }
   }

  private void addOrderNotificationLink (String orderId, String notificationId)
   {
    synchronized (orderNotificationLinks)
     {
      orderNotificationLinks.put(orderId,notificationId);
     }
   }

  private String removeOrderNotificationLink (String orderId)
   {
    synchronized (orderNotificationLinks)
     {
      return orderNotificationLinks.remove(orderId);
     }
   }

  private static String makeDevPayload (long clientTransactionId)
   {
    MNUtils.HttpPostBodyStringBuilder payload = new MNUtils.HttpPostBodyStringBuilder();

    payload.addParam("client_transaction_id",Long.toString(clientTransactionId));

    return payload.toString();
   }

  private static long devPayloadGetClientTransactionId (String developerPayload)
   {
    long id = MNVItemsProvider.TRANSACTION_ID_UNDEFINED;

    if (developerPayload != null)
     {
      try
       {
        HashMap<String,String> params = MNUtils.httpGetRequestParseParams(developerPayload);

        id = MNUtils.parseLongWithDefault
              (params.get("client_transaction_id"),
               MNVItemsProvider.TRANSACTION_ID_UNDEFINED);
       }
      catch (UnsupportedEncodingException e)
       {
       }
     }

    return id;
   }

  private static class PendingPurchaseStateChange
   {
    public PendingPurchaseStateChange
            (String signedData, String signature,
             MNInAppBilling.PurchaseStateResponse.Order order)
     {
      this.signedData = signedData;
      this.signature  = signature;
      this.order      = order;
     }

    public String getSignedData ()
     {
      return signedData;
     }

    public String getSignature ()
     {
      return signature;
     }

    private MNInAppBilling.PurchaseStateResponse.Order getOrder ()
     {
      return order;
     }

    private final String                                     signedData;
    private final String                                     signature;
    private final MNInAppBilling.PurchaseStateResponse.Order order;
   }

  private final MNSession              session;
  private final MNVShopProvider        vShopProvider;
  private final SessionEventHandler    sessionEventHandler;
  private boolean                      serviceStarted;
  private boolean                      billingAvailable;
  private final HashMap<String,String> orderNotificationLinks;
  private final ArrayList<PendingPurchaseStateChange> pendingPurchaseStateChanges;
  private final HashMap<Long,Long>     requestIdClientTransactionIdMapping;
  private final MNVShopWSRequestHelper requestHelper;
  private MNVShopProvider.IInAppBillingActivityManager inAppBillingActivityManager;

  private static final String TAG = "MNVShopInAppBilling";
  private static final String InAppBillingWebServicePath = "user_ajax_proc_app_purchase.php";

  private static final String WSTransactionStatusPurchased = "0";
  private static final String WSTransactionStatusFailed    = "-1";
  private static final String WSTransactionStatusRefunded  = "1";
 }

