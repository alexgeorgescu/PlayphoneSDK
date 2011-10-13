//
//  MNVShopProvider.java
//  MultiNet client
//
//  Copyright 2011 PlayPhone. All rights reserved.
//

package com.playphone.multinet.providers;

import com.playphone.multinet.MNConst;
import com.playphone.multinet.core.MNSession;
import com.playphone.multinet.core.MNUtils;
import com.playphone.multinet.core.MNSessionEventHandlerAbstract;
import com.playphone.multinet.core.MNGameVocabulary;
import com.playphone.multinet.core.MNEventHandlerArray;
import com.playphone.multinet.core.MNURLDownloader;
import com.playphone.multinet.core.MNURLStringDownloader;
import com.playphone.multinet.core.ws.MNWSXmlTools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.io.UnsupportedEncodingException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import android.app.PendingIntent;

/**
 * A class representing "Virtual shop" MultiNet provider.
 *
 * "Virtual shop" provider provides virtual shop support.
 */
public class MNVShopProvider
 {
  /**
   * Interface handling virtual shop events.
   */
  public interface IEventHandler
   {
    /**
     * Invoked when the virtual shop information has been updated as a
     * result of MNVShopProvider's doVShopInfoUpdate call.
     */
    void onVShopInfoUpdated ();

    /**
     * Invoked when the virtual shop asks to show dashboard
     */
    void showDashboard ();

    /**
     * Invoked when the virtual shop asks to hide dashboard
     */
    void hideDashboard ();

    /**
     * Invoked when purchase was successfully completed.
     * @param result information on completed transaction
     */
    void onCheckoutVShopPackSuccess (CheckoutVShopPackSuccessInfo result);

    /**
     * Invoked when purchase operation failed.
     * @param result information on error
     */
    void onCheckoutVShopPackFail (CheckoutVShopPackFailInfo result);

    public static class CheckoutVShopPackSuccessInfo
     {
      public CheckoutVShopPackSuccessInfo (MNVItemsProvider.TransactionInfo transaction)
       {
        this.transaction = transaction;
       }

      public MNVItemsProvider.TransactionInfo getTransaction ()
       {
        return transaction;
       }

      private MNVItemsProvider.TransactionInfo transaction;
     }

    public static class CheckoutVShopPackFailInfo
     {
      public CheckoutVShopPackFailInfo (int errorCode, String errorMessage, long cliTransactionId)
       {
        this.errorCode        = errorCode;
        this.errorMessage     = errorMessage;
        this.cliTransactionId = cliTransactionId;
       }

      public int getErrorCode ()
       {
        return errorCode;
       }

      public String getErrorMessage ()
       {
        return errorMessage;
       }

      public long getClientTransactionId ()
       {
        return cliTransactionId;
       }

      private int    errorCode;
      private String errorMessage;
      private long   cliTransactionId;
     }

    public static final int ERROR_CODE_NO_ERROR            = 0;
    public static final int ERROR_CODE_USER_CANCEL         = -999;
    public static final int ERROR_CODE_UNDEFINED           = -998;
    public static final int ERROR_CODE_XML_PARSE_ERROR     = -997;
    public static final int ERROR_CODE_XML_STRUCTURE_ERROR = -996;
    public static final int ERROR_CODE_NETWORK_ERROR       = -995;
    public static final int ERROR_CODE_GENERIC             = -994;
   }

  /**
   * A class which implements IEventHandler interface by ignoring all
   * received events.
   */
  public static class EventHandlerAbstract implements IEventHandler
   {
    public void onVShopInfoUpdated ()
     {
     }

    public void showDashboard ()
     {
     }

    public void hideDashboard ()
     {
     }

    public void onCheckoutVShopPackSuccess (CheckoutVShopPackSuccessInfo result)
     {
     }

    public void onCheckoutVShopPackFail (CheckoutVShopPackFailInfo result)
     {
     }
   }

  /**
   * A class representing virtual shop delivery.
   */
  public static class VShopDeliveryInfo
   {
    /**
     * Virtual item identifier - unique identifier of virtual item.
     */
    public int vItemId;

    /**
     * Amount of virtual items which will be delivered.
     */
    public long amount;

    public VShopDeliveryInfo (int vItemId, long amount)
     {
      this.vItemId = vItemId;
      this.amount  = amount;
     }
   }

  /**
   * A class representing virtual shop package information.
   */
  public static class VShopPackInfo
   {
    /**
     * Package identifier - unique identifier of shop package.
     */
    public int id;

    /**
     * Package name.
     */
    public String name;

    /**
     * Package model.
     */
    public int model;

    /**
     * Package description.
     */
    public String description;

    /**
     * Application-defined package parameters.
     */
    public String appParams;

    /**
     * Position of this package in package list.
     */
    public int sortPos;

    /**
     * Category identifier.
     */
    public int categoryId;

    /**
     * Array of deliveries.
     */
    public VShopDeliveryInfo delivery[];

    /**
     * Virtual currency item id, is 0 if price is in real currency.
     */
    public int priceItemId;

    /**
     * Price.
     */
    public long priceValue;

    /**
     * Constructs a new <code>VShopPackInfo</code> object.
     *
     * @param id package identifier
     * @param name package name
     */
    public VShopPackInfo (int id, String name)
     {
      this.id          = id;
      this.name        = name;
      this.model       = 0;
      this.description = "";
      this.appParams   = "";
      this.sortPos     = 0;
      this.categoryId  = 0;
      this.delivery    = null;
      this.priceItemId = 0;
      this.priceValue  = 0;
     }
   }

  /**
   * A class representing virtual shop category information.
   */
  public static class VShopCategoryInfo
   {
    /**
     * Category identifier - unique identifier of shop category.
     */
    public int id;

    /**
     * Category name.
     */
    public String name;

    /**
     * Position of this category in category list.
     */
    public int sortPos;

    /**
     * Constructs a new <code>VShopCategoryInfo</code> object.
     *
     * @param id category identifier
     * @param name category name
     */
    public VShopCategoryInfo (int id, String name)
     {
      this.id             = id;
      this.name           = name;
      this.sortPos        = 0;
     }
   }

  /**
   * A class representing a "buy" request.
   */
  public static class VShopPackBuyRequestItem
   {
    /**
     * Shop package identifier.
     */
    public int    id;

    /**
     * Amount.
     */
    public long   amount;

    /**
     * Constructs a new <code>VShopPackBuyRequestItem</code> object.
     *
     * @param id shop package identifier
     * @param amount amount of packages to buy
     */
    public VShopPackBuyRequestItem (int id, long amount)
     {
      this.id     = id;
      this.amount = amount;
     }
   }

  /**
   * An interface which is used to override default procedure of
   * starting In-app Billing purchase activity
   */
  public interface IInAppBillingActivityManager
   {
    /**
     * Starts In-app Billing purchase activity
     * @param intent intent which can be used to launch checkout UI
     * @return true if activity was started successfully and false - otherwise
     */
    public boolean startInAppBillingActivity (PendingIntent intent);
   }

  /**
   * Constructs a new <code>MNVShopProvider</code> object.
   *
   * @param session         MultiNet session instance
   */
  public MNVShopProvider (MNSession session, MNVItemsProvider vItemsProvider)
   {
    this.session        = session;
    this.vItemsProvider = vItemsProvider;

    inAppBilling = new MNVShopInAppBilling(session,this);

    requestHelper = new MNVShopWSRequestHelper(session,new RequestHelperEventHandler());

    eventHandlers = new MNEventHandlerArray<IEventHandler>();

    sessionEventHandler        = new SessionEventHandler();
    gameVocabularyEventHandler = new GameVocabularyEventHandler();

    session.getGameVocabulary().addEventHandler(gameVocabularyEventHandler);
   }

  /**
   * Stops provider and frees all allocated resources.
   */
  public void shutdown ()
   {
    inAppBilling.shutdown();
    requestHelper.shutdown();
    session.getGameVocabulary().removeEventHandler(gameVocabularyEventHandler);
    sessionEventHandler.shutdown();
   }

  private ArrayList<VShopPackInfo> getVShopPackListLow ()
   {
    ArrayList<VShopPackInfo> packs = new ArrayList<VShopPackInfo>();
    String fileString = null;
    byte[] fileData   = session.getGameVocabulary().getFileData(DATA_FILE_NAME);

    if (fileData != null)
     {
      fileString = MNUtils.stringFromUtf8ByteArray(fileData);
     }

    if (fileString != null)
     {
      DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();

      try
       {
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        Document        dom        = docBuilder.parse(new org.xml.sax.InputSource(new java.io.StringReader(fileString)));

        Element listElement = MNWSXmlTools.documentGetElementByPath(dom,VShopPackListEntriesXmlPath);

        if (listElement == null)
         {
          throw new Exception("cannot find \"VShopPacks\" element in document");
         }

        ArrayList<HashMap<String,String>> packsData = MNWSXmlTools.nodeParseItemList(listElement,"entry");

        for (HashMap<String,String> packData : packsData)
         {
          String data;
          int    id = 0;

          data = packData.get("id");

          if (data != null)
           {
            try
             {
              id = Integer.parseInt(data);
             }
            catch (NumberFormatException e)
             {
              data = null;
             }
           }

          if (data != null)
           {
            VShopPackInfo packInfo = new VShopPackInfo(id,MNUtils.parseStringWithDefault(packData.get("name"),""));

            packInfo.model          = MNUtils.parseIntWithDefault(packData.get("model"),0);
            packInfo.description    = MNUtils.parseStringWithDefault(packData.get("desc"),"");
            packInfo.appParams      = MNUtils.parseStringWithDefault(packData.get("params"),"");
            packInfo.sortPos        = MNUtils.parseIntWithDefault(packData.get("sortPos"),0);
            packInfo.categoryId     = MNUtils.parseIntWithDefault(packData.get("categoryId"),0);

            packInfo.delivery    = new VShopDeliveryInfo[1];
            packInfo.delivery[0] = new VShopDeliveryInfo
                                        (MNUtils.parseIntWithDefault
                                          (packData.get("deliveryItemId"),0),
                                         MNUtils.parseLongWithDefault
                                          (packData.get("deliveryItemAmount"),0));
            packInfo.priceItemId    = MNUtils.parseIntWithDefault(packData.get("priceItemId"),-1);
            packInfo.priceValue     = MNUtils.parseLongWithDefault(packData.get("priceValue"),0);

            packs.add(packInfo);
           }
          else
           {
            session.getPlatform().logWarning(TAG,"vshop pack with invalid or absent identifier ignored");
           }
         }
       }
      catch (Exception e)
       {
        packs.clear();

        session.getPlatform().logWarning(TAG,String.format("vshop data parsing failed (%s)",e.toString()));
       }
     }

    return packs;
   }

  private ArrayList<VShopCategoryInfo> getVShopCategoryListLow ()
   {
    ArrayList<VShopCategoryInfo> categories = new ArrayList<VShopCategoryInfo>();
    String fileString = null;
    byte[] fileData   = session.getGameVocabulary().getFileData(DATA_FILE_NAME);

    if (fileData != null)
     {
      fileString = MNUtils.stringFromUtf8ByteArray(fileData);
     }

    if (fileString != null)
     {
      DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();

      try
       {
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        Document        dom        = docBuilder.parse(new org.xml.sax.InputSource(new java.io.StringReader(fileString)));

        Element listElement = MNWSXmlTools.documentGetElementByPath(dom,VShopCategoryListEntriesXmlPath);

        if (listElement == null)
         {
          throw new Exception("cannot find \"VShopCategories\" element in document");
         }

        ArrayList<HashMap<String,String>> categoriesData = MNWSXmlTools.nodeParseItemList(listElement,"entry");

        for (HashMap<String,String> categoryData : categoriesData)
         {
          String data;
          int    id = 0;

          data = categoryData.get("id");

          if (data != null)
           {
            try
             {
              id = Integer.parseInt(data);
             }
            catch (NumberFormatException e)
             {
              data = null;
             }
           }

          if (data != null)
           {
            VShopCategoryInfo categoryInfo = new VShopCategoryInfo(id,MNUtils.parseStringWithDefault(categoryData.get("name"),""));

            categoryInfo.sortPos = MNUtils.parseIntWithDefault(categoryData.get("sortPos"),0);

            categories.add(categoryInfo);
           }
          else
           {
            session.getPlatform().logWarning(TAG,"vshop category with invalid or absent identifier ignored");
           }
         }
       }
      catch (Exception e)
       {
        categories.clear();

        session.getPlatform().logWarning(TAG,String.format("vshop data parsing failed (%s)",e.toString()));
       }
     }

    return categories;
   }

  /**
   * Returns a list of all available shop packages.
   *
   * @return array of shop packages.
   */
  public VShopPackInfo[] getVShopPackList ()
   {
    ArrayList<VShopPackInfo> packs = getVShopPackListLow();

    return packs.toArray(new VShopPackInfo[packs.size()]);
   }

  /**
   * Returns a list of all available categories.
   *
   * @return array of categories.
   */
  public VShopCategoryInfo[] getVShopCategoryList ()
   {
    ArrayList<VShopCategoryInfo> categories = getVShopCategoryListLow();

    return categories.toArray(new VShopCategoryInfo[categories.size()]);
   }

  /**
   * Returns vshop pack information by vshop pack identifier.
   *
   * @return VShopPackInfo object or null if there is no such vshop pack.
   */
  public VShopPackInfo findVShopPackById (int id)
   {
    ArrayList<VShopPackInfo> packs = getVShopPackListLow();

    boolean found = false;
    int     index = 0;
    int     count = packs.size();

    VShopPackInfo pack = null;

    while (!found && index < count)
     {
      pack = packs.get(index);

      if (pack.id == id)
       {
        found = true;
       }
      else
       {
        index++;
       }
     }

    return found ? pack : null;
   }

  /**
   * Returns vshop category information by vshop category identifier.
   *
   * @return VShopCategoryInfo object or null if there is no such vshop category.
   */
  public VShopCategoryInfo findVShopCategoryById (int id)
   {
    ArrayList<VShopCategoryInfo> categories = getVShopCategoryListLow();

    boolean found = false;
    int     index = 0;
    int     count = categories.size();

    VShopCategoryInfo category = null;

    while (!found && index < count)
     {
      category = categories.get(index);

      if (category.id == id)
       {
        found = true;
       }
      else
       {
        index++;
       }
     }

    return found ? category : null;
   }

  /**
   * Returns state of available virtual shop data.
   *
   * @return <code>true</code> if newer virtual shop data is available on server, <code>false</code> - otherwise.
   */
  public boolean isVShopInfoNeedUpdate ()
   {
    return session.getGameVocabulary().getVocabularyStatus() > 0;
   }

  /**
   * Starts virtual shop data update. On successfull completion eventHandlers's
   * onVShopInfoUpdated method will be called.
   */
  public void doVShopInfoUpdate ()
   {
    if (session.getGameVocabulary().getVocabularyStatus() !=
         MNGameVocabulary.MN_GV_UPDATE_STATUS_DOWNLOAD_IN_PROGRESS)
     {
      session.getGameVocabulary().startDownload();
     }
   }

  /**
   * Returns URL of virtual pack's image
   * @param packId package identifier identifier
   * @return image URL
   */
  public String getVShopPackImageURL (int packId)
   {
    String webServerUrl = session.getWebServerURL();

    if (webServerUrl != null)
     {
      StringBuilder builder = new StringBuilder(webServerUrl);

      builder.append("/data_game_shoppack_image.php?game_id=");
      builder.append(Integer.toString(session.getGameId()));
      builder.append("&gameshoppack_id=");
      builder.append(Integer.toString(packId));

      return builder.toString();
     }
    else
     {
      return null;
     }
   }

  public synchronized void execCheckoutVShopPacks (int[] packIdArray,
                                                   int[] packCountArray,
                                                   long  clientTransactionId)
   {
    session.execAppCommand("jumpToBuyVShopPackRequestDialogSimple",
                           "pack_id=" + joinIntegers(packIdArray) + "&" +
                           "buy_count=" + joinIntegers(packCountArray) + "&" +
                           "client_transaction_id=" + Long.toString(clientTransactionId));
   }

  public synchronized void procCheckoutVShopPacksSilent (int[] packIdArray,
                                                         int[] packCountArray,
                                                         long  clientTransactionId)
   {
    final String webServerUrl = session.getWebServerURL();

    if (webServerUrl != null)
     {
      MNUtils.HttpPostBodyStringBuilder postBodyBuilder = new MNUtils.HttpPostBodyStringBuilder();

      postBodyBuilder.addParam("proc_pack_id",joinIntegers(packIdArray));
      postBodyBuilder.addParam("proc_pack_count",joinIntegers(packCountArray));
      postBodyBuilder.addParam("proc_client_transaction_id",Long.toString(clientTransactionId));

      requestHelper.sendWSRequest
         (webServerUrl + "/" + SilentPurchaseWebServicePath,
          postBodyBuilder,clientTransactionId);
     }
    else
     {
      dispatchCheckoutFailedEvent
       (IEventHandler.ERROR_CODE_NETWORK_ERROR,
        "checkout endpoint is unreachable",
        clientTransactionId);
     }
   }

  /**
   * Adds event handler
   *
   * @param eventHandler an object that implements
   * {@link IEventHandler IEventHandler} interface
   */
  public void addEventHandler (IEventHandler eventHandler)
   {
    eventHandlers.add(eventHandler);
   }

  /**
   * Removes event handler
   *
   * @param eventHandler an object that implements
   * {@link IEventHandler IEventHandler} interface
   */
  public void removeEventHandler (IEventHandler eventHandler)
   {
    eventHandlers.remove(eventHandler);
   }

  /**
   * Sets custom in-app billing activity manager
   *
   * @param manager an object that implmements
   * {@link IInAppBillingActivityManager IInAppBillingActivityManager} interface or
   * null to restore default behavior
   */
  public void setInAppBillingActivityManager (IInAppBillingActivityManager manager)
   {
    inAppBilling.setInAppBillingActivityManager(manager);
   }

  private void onVShopPackListUpdated ()
   {
    eventHandlers.callHandlers(new MNEventHandlerArray.ICaller<IEventHandler>()
     {
      public void callHandler (IEventHandler handler)
       {
        handler.onVShopInfoUpdated();
       }
     });
   }

  private class GameVocabularyEventHandler extends MNGameVocabulary.EventHandlerAbstract
   {
    public void mnGameVocabularyDownloadFinished (int downloadStatus)
     {
      if (downloadStatus >= 0)
       {
        onVShopPackListUpdated();
       }
     }
   }

  private class SessionEventHandler extends MNSessionEventHandlerAbstract
   {
    public SessionEventHandler ()
     {
      session.addEventHandler(this);
     }

    public void shutdown ()
     {
      session.removeEventHandler(this);
     }

    public void mnSessionExecUICommandReceived (String cmdName, String cmdParam)
     {
      boolean ok = false;

      if      (cmdName.equals("onVShopNeedShowDashboard"))
       {
        eventHandlers.callHandlers(new MNEventHandlerArray.ICaller<IEventHandler>()
         {
          public void callHandler (final IEventHandler handler)
           {
            session.getPlatform().runOnUiThread(new Runnable()
             {
              public void run ()
               {
                handler.showDashboard();
               }
             });
           }
         });

        return;
       }
      else if (cmdName.equals("onVShopNeedHideDashboard"))
       {
        eventHandlers.callHandlers(new MNEventHandlerArray.ICaller<IEventHandler>()
         {
          public void callHandler (final IEventHandler handler)
           {
            session.getPlatform().runOnUiThread(new Runnable()
             {
              public void run ()
               {
                handler.hideDashboard();
               }
             });
           }
         });

        return;
       }
      else if (cmdName.equals("afterBuyVShopPackRequestSuccess"))
       {
        ok = true;
       }
      else if (cmdName.equals("afterBuyVShopPackRequestFail"))
       {
       }
      else
       {
        return;
       }

      HashMap<String,String> params = null;

      try
       {
        params = MNUtils.httpGetRequestParseParams(cmdParam);
       }
      catch (UnsupportedEncodingException e)
       {
        session.getPlatform().logWarning(TAG,"invalid UI command parameter received");
       }

      synchronized(MNVShopProvider.this)
       {
        if (ok)
         {
          MNVItemsProvider.TransactionInfo transactionInfo
           = vItemsProvider.applyTransactionWithParams(params,"\n","\t");

          if (transactionInfo != null)
           {
            dispatchCheckoutSucceededEvent(transactionInfo);
           }
          else
           {
            session.getPlatform().logWarning(TAG,"unable to process transaction - invalid parameters");
           }
         }
        else
         {
          final int errorCode         = MNUtils.parseIntWithDefault(params.get("error_code"),IEventHandler.ERROR_CODE_UNDEFINED);
          String    errorMessage      = params.get("error_message");
          final long cliTransactionId = MNUtils.parseLongWithDefault(params.get("client_transaction_id"),MNVItemsProvider.TRANSACTION_ID_UNDEFINED);

          if (errorMessage == null)
           {
            errorMessage = "undefined error";
           }

          dispatchCheckoutFailedEvent(errorCode,errorMessage,cliTransactionId);
         }
       }
     }
   }

  private void dispatchCheckoutSucceededEvent (MNVItemsProvider.TransactionInfo transactionInfo)
   {
    final IEventHandler.CheckoutVShopPackSuccessInfo info =
           new IEventHandler.CheckoutVShopPackSuccessInfo(transactionInfo);

    eventHandlers.callHandlers(new MNEventHandlerArray.ICaller<IEventHandler>()
     {
      public void callHandler (IEventHandler handler)
       {
        handler.onCheckoutVShopPackSuccess(info);
       }
     });
   }

  /* package */ void dispatchCheckoutFailedEvent (int errorCode, String errorMessage, long cliTransactionId)
   {
    final IEventHandler.CheckoutVShopPackFailInfo info =
           (new IEventHandler.CheckoutVShopPackFailInfo
             (errorCode,errorMessage,cliTransactionId));

    eventHandlers.callHandlers(new MNEventHandlerArray.ICaller<IEventHandler>()
     {
      public void callHandler (IEventHandler handler)
       {
        handler.onCheckoutVShopPackFail(info);
       }
     });
   }

  private static String joinIntegers (int[] values)
   {
    MNUtils.StringJoiner builder = new MNUtils.StringJoiner(",");

    for (int v : values)
     {
      builder.join(Integer.toString(v));
     }

    return builder.toString();
   }

  //Note: this method is also used by MNVShopInAppBilling
  /* package */ void processPostVItemTransactionCmd (long    srvTransactionId,
                                                     long    cliTransactionId,
                                                     String  itemsToAddStr,
                                                     boolean vShopTransactionEnabled)
   {
    HashMap<String,String> params = new HashMap<String,String>();

    params.put("server_transaction_id",Long.toString(srvTransactionId));
    params.put("client_transaction_id",Long.toString(cliTransactionId));
    params.put("items_to_add",itemsToAddStr);

    MNVItemsProvider.TransactionInfo transactionInfo
     = vItemsProvider.applyTransactionWithParams(params,",",":");

    if (transactionInfo != null)
     {
      if (vShopTransactionEnabled)
       {
        dispatchCheckoutSucceededEvent(transactionInfo);
       }
     }
    else
     {
      session.getPlatform().logWarning(TAG,"unable to process transaction - invalid parameters");
     }
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
      processPostVItemTransactionCmd
       (srvTransactionId,cliTransactionId,itemsToAddStr,vShopTransactionEnabled);
     }

    public void    vShopFinishTransaction    (String transactionId)
     {
      // this command shouldn't be issued for "silent" requests,
      // so just ignore it if it get called
     }

    public void    vShopWSRequestFailed      (long   clientTransactionId,
                                              int    errorCode,
                                              String errorMessage)
     {
      dispatchCheckoutFailedEvent(errorCode,errorMessage,clientTransactionId);
     }
   }

  private final MNSession                          session;
  private final GameVocabularyEventHandler         gameVocabularyEventHandler;
  private final SessionEventHandler                sessionEventHandler;
  private final MNEventHandlerArray<IEventHandler> eventHandlers;
  private final MNVItemsProvider                   vItemsProvider;
  private final MNVShopWSRequestHelper             requestHelper;
  private final MNVShopInAppBilling                inAppBilling;

  private static final String DATA_FILE_NAME = "MNVShopProvider.xml";
  private static final String[] VShopPackListEntriesXmlPath = { "GameVocabulary", "MNVShopProvider", "VShopPacks" };
  private static final String[] VShopCategoryListEntriesXmlPath = { "GameVocabulary", "MNVShopProvider", "VShopCategories" };
  private static final String TAG = "MNVShopProvider";
  private static final String SilentPurchaseWebServicePath = "user_ajax_proc_silent_purchase.php";
 }

