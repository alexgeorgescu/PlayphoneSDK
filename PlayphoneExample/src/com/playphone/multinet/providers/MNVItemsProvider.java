//
//  MNVItemsProvider.java
//  MultiNet client
//
//  Copyright 2010 PlayPhone. All rights reserved.
//

package com.playphone.multinet.providers;

import java.util.ArrayList;
import java.util.HashMap;
import java.io.UnsupportedEncodingException;

import com.playphone.multinet.MNConst;
import com.playphone.multinet.MNUserInfo;

import com.playphone.multinet.core.MNSession;
import com.playphone.multinet.core.MNUtils;
import com.playphone.multinet.core.MNSessionEventHandlerAbstract;
import com.playphone.multinet.core.MNGameVocabulary;
import com.playphone.multinet.core.MNEventHandlerArray;
import com.playphone.multinet.core.ws.MNWSXmlTools;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * A class representing "Virtual items" MultiNet provider.
 *
 * "Virtual items" provider provides virtual items support.
 */
public class MNVItemsProvider
 {
  public static final int TRANSACTION_ID_UNDEFINED = 0;

  public static final int VITEM_IS_CURRENCY     = 0x0001;
  public static final int VITEM_IS_UNIQUE       = 0x0002;
  public static final int VITEM_IS_CONSUMABLE   = 0x0004;
  public static final int VITEM_ISSUE_ON_CLIENT = 0x0200;

  /**
   * Interface handling virtual items events.
   */
  public interface IEventHandler
   {
    /**
     * Invoked when the list of virtual items has been updated as a
     * result of MNVItemsProvider's doGameVItemsListUpdate call.
     */
    void onVItemsListUpdated          ();

    /**
     * Invoked when server completed virtual item(s) transaction for player.
     */
    void onVItemsTransactionCompleted (TransactionInfo  transaction);

    /**
     * Invoked when virtual item(s) transaction failed.
     */
    void onVItemsTransactionFailed    (TransactionError error);
   }

  /**
   * A class which implements IEventHandler interface by ignoring all
   * received events.
   */
  public static class EventHandlerAbstract implements IEventHandler
   {
    public void onVItemsListUpdated ()
     {
     }

    public void onVItemsTransactionCompleted (TransactionInfo  transaction)
     {
     }

    public void onVItemsTransactionFailed    (TransactionError error)
     {
     }
   }

  /**
   * A class representing virtual item information.
   */
  public static class GameVItemInfo
   {
    /**
     * Virtual item identifier - unique identifier of virtual item.
     */
    public int    id;

    /**
     * Name of virtual item.
     */
    public String name;

    /**
     * Virtual item model.
     */
    public int    model;

    /**
     * Virtual item description.
     */
    public String description;

    /**
     * Virtual item params.
     */
    public String params;

    /**
     * Constructs a new <code>GameVItemInfo</code> object.
     *
     * @param id virtial item identifier
     * @param name virtual item name
     * @param model virtual item model
     * @param description virtual item description
     * @param params virtual item parameters
     */
    public GameVItemInfo (int id, String name, int model, String description, String params)
     {
      this.id          = id;
      this.name        = name;
      this.model       = model;
      this.description = description;
      this.params      = params;
     }
   }

  /**
   * A class representing player virtual item information.
   */
  public static class PlayerVItemInfo
   {
    /**
     * Virtual item identifier - unique identifier of virtual item.
     */
    public int    id;

    /*
     * Number of virtual items.
     */
    public long   count;

    /**
     * Constructs a new <code>PlayerVItemInfo</code> object.
     *
     * @param id virtual item identifier
     * @param count number of virtual items
     */
    public PlayerVItemInfo (int id, long count)
     {
      this.id    = id;
      this.count = count;
     }
   }

  /**
   * A class representing information on virtual item transaction.
   */
  public static class TransactionVItemInfo
   {
    /**
     * Virtual item identifier - unique identifier of virtual item.
     */
    public int    id;

    /**
     * Change of virtual items count introduced by this transaction.
     */
    public long   delta;

    /**
     * Constructs a new <code>TransactionVItemInfo</code> object.
     *
     * @param id virtual item identifier
     * @param delta change of virtual items count introduced by this transaction
     */
    public TransactionVItemInfo (int id, long delta)
     {
      this.id    = id;
      this.delta = delta;
     }
   }

  /**
   * A class representing transaction response object.
   */
  public static class TransactionInfo
   {
    /**
     * Client transaction identifier.
     */
    public long                   clientTransactionId;

    /**
     * Server transaction identifier.
     */
    public long                   serverTransactionId;

    /**
     * Correspondent user identifier (if applicable).
     */
    public long                   corrUserId;

    /**
     * Array of <code>TransactionVItemInfo</code> objects.
     */
    public TransactionVItemInfo[] vItems;

    /**
     * Constructs a new <code>TransactionInfo</code> object.
     *
     * @param clientTransactionId client transaction id
     * @param serverTransactionId server transaction id
     * @param corrUserId correspondent userId
     * @param vItems array of <code>TransactionVItemInfo</code> objects
     */
    public TransactionInfo (long clientTransactionId,
                            long serverTransactionId,
                            long corrUserId,
                            TransactionVItemInfo[] vItems)
     {
      this.clientTransactionId = clientTransactionId;
      this.serverTransactionId = serverTransactionId;
      this.corrUserId          = corrUserId;
      this.vItems              = vItems;
     }
   }

  /**
   * A class representing transaction error.
   */
  public static class TransactionError
   {
    /**
     * Client transaction identifier.
     */
    public long   clientTransactionId;

    /**
     * Server transaction identifier.
     */
    public long   serverTransactionId;

    /**
     * Correspondent user identifier (if applicable).
     */
    public long   corrUserId;

    /**
     * Fail reason.
     */
    public int    failReasonCode;

    /**
     * Error message.
     */
    public String errorMessage;

    /**
     * Constructs a new <code>TransactionError</code> object.
     *
     * @param clientTransactionId client transaction id
     * @param serverTransactionId server transaction id
     * @param corrUserId correspondent userId
     * @param failReasonCode fail reason
     * @param errorMessage error message
     */
    public TransactionError (long   clientTransactionId,
                             long   serverTransactionId,
                             long   corrUserId,
                             int    failReasonCode,
                             String errorMessage)
     {
      this.clientTransactionId = clientTransactionId;
      this.serverTransactionId = serverTransactionId;
      this.corrUserId          = corrUserId;
      this.failReasonCode      = failReasonCode;
      this.errorMessage        = errorMessage;
     }
   }

  /**
   * Constructs a new <code>MNVItemsProvider</code> object.
   *
   * @param session         MultiNet session instance
   */
  public MNVItemsProvider (MNSession session)
   {
    sessionEventHandler = new SessionEventHandler(session);
    playerVItemsOwnerId = MNConst.MN_USER_ID_UNDEFINED;
    playerVItems        = new ArrayList<PlayerVItemInfo>();
    clientTransactionId = generateInitialClientTransactionId();

    gameVocabularyEventHandler = new GameVocabularyEventHandler();

    session.getGameVocabulary().addEventHandler(gameVocabularyEventHandler);
   }

  /**
   * Stops provider and frees all allocated resources.
   */
  public synchronized void shutdown ()
   {
    sessionEventHandler.session.getGameVocabulary().removeEventHandler(gameVocabularyEventHandler);

    sessionEventHandler.shutdown();

    playerVItems.clear();
    playerVItems = null;
   }

  public ArrayList<GameVItemInfo> getGameVItemsListLow ()
   {
    ArrayList<GameVItemInfo> vitems = new ArrayList<GameVItemInfo>();
    String fileString = null;
    byte[] fileData   = sessionEventHandler.session.getGameVocabulary().getFileData(DATA_FILE_NAME);

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

        Element listElement = MNWSXmlTools.documentGetElementByPath(dom,GameVItemEntriesXmlPath);

        if (listElement == null)
         {
          throw new Exception("cannot find \"VItems\" element in document");
         }

        ArrayList<HashMap<String,String>> items = MNWSXmlTools.nodeParseItemList(listElement,"entry");

        for (HashMap<String,String> itemData : items)
         {
          String data;
          int    id = 0;

          data = itemData.get("id");

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
            String name;
            int    model;
            String desc;
            String params;

            name   = MNUtils.parseStringWithDefault(itemData.get("name"),"");
            model  = MNUtils.parseIntWithDefault(itemData.get("model"),0);
            desc   = MNUtils.parseStringWithDefault(itemData.get("desc"),"");
            params = MNUtils.parseStringWithDefault(itemData.get("params"),"");

            vitems.add(new GameVItemInfo(id,name,model,desc,params));
           }
          else
           {
            sessionEventHandler.session.getPlatform().logWarning(TAG,"game vitem data with invalid or absent vitem id ignored");
           }
         }
       }
      catch (Exception e)
       {
        vitems.clear();

        sessionEventHandler.session.getPlatform().logWarning(TAG,String.format("game vitem data parsing failed (%s)",e.toString()));
       }
     }

    return vitems;
   }

  /**
   * Returns a list of all available virtual items.
   *
   * @return array of virtual items.
   */
  public GameVItemInfo[] getGameVItemsList ()
   {
    ArrayList<GameVItemInfo> vitems = getGameVItemsListLow();

    return vitems.toArray(new GameVItemInfo[vitems.size()]);
   }

  /**
   * Returns game vitem information by vitem identifier.
   *
   * @return GameVItemInfo object or null if there is no such vitem.
   */
  public GameVItemInfo findGameVItemById (int id)
   {
    ArrayList<GameVItemInfo> vitems = getGameVItemsListLow();

    boolean found = false;
    int     index = 0;
    int     count = vitems.size();

    GameVItemInfo vitem = null;

    while (!found && index < count)
     {
      vitem = vitems.get(index);

      if (vitem.id == id)
       {
        found = true;
       }
      else
       {
        index++;
       }
     }

    return found ? vitem : null;
   }

  /**
   * Returns state of available virtual items list.
   *
   * @return <code>true</code> if newer virtual items list is available on server, <code>false</code> - otherwise.
   */
  public boolean isGameVItemsListNeedUpdate ()
   {
    return sessionEventHandler.session.getGameVocabulary().getVocabularyStatus() > 0;
   }

  /**
   * Starts virtual items info update. On successfull completion eventHandlers's
   * onVItemsUpdated method will be called.
   */
  public synchronized void doGameVItemsListUpdate ()
   {
    if (sessionEventHandler.session.getGameVocabulary().getVocabularyStatus() !=
         MNGameVocabulary.MN_GV_UPDATE_STATUS_DOWNLOAD_IN_PROGRESS)
     {
      sessionEventHandler.session.getGameVocabulary().startDownload();
     }
   }

  /**
   * Asks server to add virtual item(s) to player.
   * @param vItemId virtual item identifier
   * @param count virtual items count
   * @param clientTransactionId transaction identifier - non-zero positive value, which will be sent back by server in transaction response
   */
  public synchronized void reqAddPlayerVItem (int vItemId, long count, long clientTransactionId)
   {
    MNSession session = sessionEventHandler.session;

    if (session.isUserLoggedIn() && session.isOnline())
     {
      session.sendPluginMessage
       (PROVIDER_NAME,"A" + Long.toString(clientTransactionId) + "\n" +
                      Integer.toString(vItemId) + "\t" + Long.toString(count));
     }
   }

  /**
   * Asks server to add virtual item(s) to player.
   * @param transactionVItems array of TransactionVItemInfo objects
   * @param clientTransactionId transaction identifier - non-zero positive value, which will be sent back by server in transaction response
   */
  public synchronized void reqAddPlayerVItemTransaction (TransactionVItemInfo[] transactionVItems, long clientTransactionId)
   {
    MNSession session = sessionEventHandler.session;

    if (session.isUserLoggedIn() && session.isOnline())
     {
      int count = transactionVItems.length;

      if (count > 0)
       {
        StringBuilder message = new StringBuilder("A" + Long.toString(clientTransactionId));

        for (int index = 0; index < count; index++)
         {
          TransactionVItemInfo vItem = transactionVItems[index];

          message.append("\n" + Integer.toString(vItem.id) +
                          "\t" + Long.toString(vItem.delta));
         }

        session.sendPluginMessage(PROVIDER_NAME,message.toString());
       }
     }
   }

  /**
   * Asks server to transfer virtual item(s) between players.
   * @param vItemId virtual item identifier
   * @param count virtual items count
   * @param toPlayerId identifier of player items will be transfered to
   * @param clientTransactionId transaction identifier - non-zero positive value, which will be sent back by server in transaction response
   */
  public synchronized void reqTransferPlayerVItem
                                     (int  vItemId,
                                      long count,
                                      long toPlayerId,
                                      long clientTransactionId)
   {
    MNSession session = sessionEventHandler.session;

    if (session.isUserLoggedIn() && session.isOnline())
     {
      session.sendPluginMessage
       (PROVIDER_NAME,"T" + Long.toString(clientTransactionId) + "\t" +
                             Long.toString(toPlayerId) + "\n" +
                              Integer.toString(vItemId) + "\t" +
                               Long.toString(count));
     }
   }

  /**
   * Asks server to transfer virtual item(s) between players.
   * @param transactionVItems array of TransactionVItemInfo objects
   * @param toPlayerId identifier of player items will be transfered to
   * @param clientTransactionId transaction identifier - non-zero positive value, which will be sent back by server in transaction response
   */
  public synchronized void reqTransferPlayerVItemTransaction
                            (TransactionVItemInfo[] transactionVItems,
                             long                   toPlayerId,
                             long                   clientTransactionId)
   {
    MNSession session = sessionEventHandler.session;

    if (session.isUserLoggedIn() && session.isOnline())
     {
      int count = transactionVItems.length;

      if (count > 0)
       {
        StringBuilder message = new StringBuilder("T" + Long.toString(clientTransactionId) + "\t" + Long.toString(toPlayerId));

        for (int index = 0; index < count; index++)
         {
          TransactionVItemInfo vItem = transactionVItems[index];

          message.append("\n" + Integer.toString(vItem.id) +
                          "\t" + Long.toString(vItem.delta));
         }

        session.sendPluginMessage(PROVIDER_NAME,message.toString());
       }
     }
   }

  /**
   * Returns list of player's virtual items.
   *
   * @return array of virtual items.
   */
  public synchronized PlayerVItemInfo[] getPlayerVItemList ()
   {
    PlayerVItemInfo[] result;

    result = playerVItems.toArray(new PlayerVItemInfo[playerVItems.size()]);

    return result;
   }

  /**
   * Returns count of player's virtual items.
   *
   * @param vItemId virtual item identifier
   * @return count of player's virtual items.
   */
  public synchronized long getPlayerVItemCountById (int vItemId)
   {
    PlayerVItemInfo vItem = sessionEventHandler.searchPlayerVItemInfoById(vItemId);

    return vItem != null ? vItem.count : 0;
   }

  /**
   * Returns URL of virtual item image
   * @param vItemId virtual item identifier
   * @return image URL
   */
  public synchronized String getVItemImageURL (int vItemId)
   {
    String webServerUrl = sessionEventHandler.session.getWebServerURL();

    if (webServerUrl != null)
     {
      StringBuilder builder = new StringBuilder(webServerUrl);

      builder.append("/data_game_item_image.php?game_id=");
      builder.append(Integer.toString(sessionEventHandler.session.getGameId()));
      builder.append("&game_item_id=");
      builder.append(Integer.toString(vItemId));

      return builder.toString();
     }
    else
     {
      return null;
     }
   }

  /**
   * Adds event handler
   *
   * @param eventHandler an object that implements
   * {@link IEventHandler IEventHandler} interface
   */
  public synchronized void addEventHandler (IEventHandler eventHandler)
   {
    sessionEventHandler.eventHandlers.add(eventHandler);
   }

  /**
   * Removes event handler
   *
   * @param eventHandler an object that implements
   * {@link IEventHandler IEventHandler} interface
   */
  public synchronized void removeEventHandler (IEventHandler eventHandler)
   {
    sessionEventHandler.eventHandlers.remove(eventHandler);
   }

  /**
   * Generates new client transaction identifier
   *
   * @return client transaction identifier
   */
  public synchronized long getNewClientTransactionId ()
   {
    clientTransactionId++;

    return clientTransactionId;
   }

  /*package*/ TransactionInfo applyTransactionWithParams (HashMap<String,String> params, String vItemsItemSeparator, String vItemsFieldSeparator)
   {
    return sessionEventHandler.applyTransaction(params,vItemsItemSeparator,vItemsFieldSeparator);
   }

  private class SessionEventHandler extends MNSessionEventHandlerAbstract
   {
    public SessionEventHandler (MNSession session)
     {
      this.session       = session;
      this.eventHandlers = new MNEventHandlerArray<IEventHandler>();

      session.addEventHandler(this);
     }

    public void shutdown ()
     {
      session.removeEventHandler(this);

      session       = null;
      eventHandlers = null;
     }

    private Object parsePlayerVItemInfoField (String line, boolean transactionVItemMode, String fieldSeparator)
     {
      int separatorPos = line.indexOf(fieldSeparator);

      if (separatorPos > 0)
       {
        try
         {
          int vItemId = Integer.parseInt(line.substring(0,separatorPos));
          long count  = Long.parseLong(line.substring(separatorPos + 1));

          if (transactionVItemMode)
           {
            return new TransactionVItemInfo(vItemId,count);
           }
          else
           {
            return new PlayerVItemInfo(vItemId,count);
           }
         }
        catch (NumberFormatException e)
         {
          return null;
         }
       }
      else
       {
        return null;
       }
     }

    private ArrayList<PlayerVItemInfo> parsePlayerVItemsListMessage (String message)
     {
      String[] vItemsInfoArray = message.split("\n");
      int index  = 0;
      int count  = vItemsInfoArray.length;
      boolean ok = true;

      ArrayList<PlayerVItemInfo> result = new ArrayList<PlayerVItemInfo>();

      while (index < count && ok)
       {
        PlayerVItemInfo vItemInfo = (PlayerVItemInfo)parsePlayerVItemInfoField(vItemsInfoArray[index],false,"\t");

        if (vItemInfo != null)
         {
          result.add(vItemInfo);
         }
        else
         {
          ok = false;
         }

        index++;
       }

      if (ok)
       {
        return result;
       }
      else
       {
        return new ArrayList<PlayerVItemInfo>();
       }
     }

    private ArrayList<TransactionVItemInfo> parseTransactionVItemsListMessage (String message, String vItemsItemsSeparator, String vItemsFieldSeparator)
     {
      String[] vItemsInfoArray = message.split(vItemsItemsSeparator);
      int index  = 0;
      int count  = vItemsInfoArray.length;
      boolean ok = true;

      ArrayList<TransactionVItemInfo> result = new ArrayList<TransactionVItemInfo>();

      while (index < count && ok)
       {
        TransactionVItemInfo vItemInfo = (TransactionVItemInfo)parsePlayerVItemInfoField(vItemsInfoArray[index],true,vItemsFieldSeparator);

        if (vItemInfo != null)
         {
          result.add(vItemInfo);
         }
        else
         {
          ok = false;
         }

        index++;
       }

      if (ok)
       {
        return result;
       }
      else
       {
        return null;
       }
     }

    PlayerVItemInfo searchPlayerVItemInfoById (int vItemId)
     {
      PlayerVItemInfo vItem = null;
      boolean found         = false;
      int     index         = 0;
      int     count         = playerVItems.size();

      while (index < count && !found)
       {
        vItem = playerVItems.get(index);

        if (vItem.id == vItemId)
         {
          found = true;
         }
        else
         {
          index++;
         }
       }

      return found ? vItem : null;
     }

    private PlayerVItemInfo playerVItemInfoById (int vItemId)
     {
      PlayerVItemInfo vItem = searchPlayerVItemInfoById(vItemId);

      if (vItem == null)
       {
        vItem = new PlayerVItemInfo(vItemId,0);

        playerVItems.add(vItem);
       }

      return vItem;
     }

    private TransactionInfo applyTransaction (long srvTransactionId,
                                              long cliTransactionId,
                                              long corrUserId,
                                              TransactionVItemInfo[] vItems)
     {
      for (TransactionVItemInfo transactionVItemInfo : vItems)
       {
        PlayerVItemInfo playerVItemInfo = playerVItemInfoById(transactionVItemInfo.id);

        playerVItemInfo.count += transactionVItemInfo.delta;
       }

      TransactionInfo transactionInfo =
       new TransactionInfo(cliTransactionId,srvTransactionId,corrUserId,vItems);

      sessionEventHandler.eventHandlers.beginCall();

      try
       {
        int count = sessionEventHandler.eventHandlers.size();

        for (int index = 0; index < count; index++)
         {
          sessionEventHandler.eventHandlers.get(index).onVItemsTransactionCompleted(transactionInfo);
         }
       }
      finally
       {
        sessionEventHandler.eventHandlers.endCall();
       }

      return transactionInfo;
     }

    private TransactionInfo applyTransaction (HashMap<String,String> params, String vItemsItemSeparator, String vItemsFieldSeparator)
     {
      final long cliTransactionId = MNUtils.parseLongWithDefault(params.get("client_transaction_id"),0);
      final long srvTransactionId = MNUtils.parseLongWithDefault(params.get("server_transaction_id"),0);
      final long corrUserId       = MNUtils.parseLongWithDefault(params.get("corr_user_id"),0);
      final String itemsToAdd     = params.get("items_to_add");

      if (cliTransactionId < 0 || srvTransactionId < 0)
       {
        return null;
       }

      final ArrayList<TransactionVItemInfo> vItemChanges = parseTransactionVItemsListMessage(itemsToAdd,vItemsItemSeparator,vItemsFieldSeparator);

      if (vItemChanges == null)
       {
        return null;
       }

      synchronized (MNVItemsProvider.this)
       {
        return applyTransaction(srvTransactionId,
                                cliTransactionId,
                                corrUserId,
                                vItemChanges.toArray
                                 (new TransactionVItemInfo[vItemChanges.size()]));
       }
     }

    private void processAddVItemsMessage (String message)
     {
      TransactionMessageHeaderInfo headerInfo = TransactionMessageHeaderInfo.getHeaderInfoFromMessage(message);

      if (headerInfo == null)
       {
        return;
       }

      ArrayList<TransactionVItemInfo> vItemChanges = parseTransactionVItemsListMessage(message.substring(headerInfo.headerLength),"\n","\t");

      if (vItemChanges == null)
       {
        return;
       }

      applyTransaction(headerInfo.srvTransactionId,
                       headerInfo.cliTransactionId,
                       headerInfo.corrUserId,
                       vItemChanges.toArray
                        (new TransactionVItemInfo[vItemChanges.size()]));
     }

    private void processFailMessage (String message)
     {
      TransactionMessageHeaderInfo headerInfo = TransactionMessageHeaderInfo.getHeaderInfoFromMessage(message);

      if (headerInfo == null)
       {
        return;
       }

      String errorInfoStr = message.substring(headerInfo.headerLength);
      int    separatorPos = errorInfoStr.indexOf(MESSAGE_FIELD_SEPARATOR);

      try
       {
        int failReasonCode = Integer.parseInt(errorInfoStr.substring(0,separatorPos));

        TransactionError transactionError =
         new TransactionError(headerInfo.cliTransactionId,
                              headerInfo.srvTransactionId,
                              headerInfo.corrUserId,
                              failReasonCode,
                              errorInfoStr.substring(separatorPos + 1));

        sessionEventHandler.eventHandlers.beginCall();

        try
         {
          int count = sessionEventHandler.eventHandlers.size();

          for (int index = 0; index < count; index++)
           {
            sessionEventHandler.eventHandlers.get(index).onVItemsTransactionFailed(transactionError);
           }
         }
        finally
         {
          sessionEventHandler.eventHandlers.endCall();
         }
       }
      catch (NumberFormatException e)
       {
       }
     }

    public void mnSessionPluginMessageReceived (String     pluginName,
                                                String     message,
                                                MNUserInfo sender)
     {
      synchronized (MNVItemsProvider.this)
       {
        if (sender != null || !pluginName.equals(PROVIDER_NAME))
         {
          return;
         }

        if (message.length() == 0)
         {
          return;
         }

        char   cmd  = message.charAt(0);
        String data = message.substring(MESSAGE_CMD_PREFIX_LEN);

        if      (cmd == 'g')
         {
          // ignore this message, it was used before 1.4.0 to get server
          // data version
         }
        else if (cmd == 'p')
         {
          playerVItemsOwnerId = session.getMyUserId();

          if (data.length() > 0)
           {
            playerVItems = parsePlayerVItemsListMessage(data);
           }
          else
           {
            playerVItems = new ArrayList<PlayerVItemInfo>();
           }
         }
        else if (cmd == 'a')
         {
          processAddVItemsMessage(data);
         }
        else if (cmd == 'f')
         {
          processFailMessage(data);
         }
       }
     }

    public void mnSessionWebEventReceived (String eventName, String eventParam, String callbackId)
     {
      if (!eventName.equals("web.onUserDoAddItems"))
       {
        return;
       }

      if (eventParam == null)
       {
        return;
       }

      try
       {
        applyTransaction(MNUtils.httpGetRequestParseParams(eventParam),"\n","\t");
       }
      catch (UnsupportedEncodingException e)
       {
       }
     }

    public void mnSessionUserChanged (long userId)
     {
      synchronized (MNVItemsProvider.this)
       {
        if (userId == MNConst.MN_USER_ID_UNDEFINED ||
            userId != playerVItemsOwnerId)
         {
          playerVItemsOwnerId = userId;
          playerVItems.clear();
         }
       }
     }

    MNSession                          session;
    MNEventHandlerArray<IEventHandler> eventHandlers;
   }

  private synchronized void onGameVItemsUpdated ()
   {
    sessionEventHandler.eventHandlers.beginCall();

    try
     {
      int count = sessionEventHandler.eventHandlers.size();

      for (int index = 0; index < count; index++)
       {
        sessionEventHandler.eventHandlers.get(index).onVItemsListUpdated();
       }
     }
    finally
     {
      sessionEventHandler.eventHandlers.endCall();
     }
   }

  private class GameVocabularyEventHandler extends MNGameVocabulary.EventHandlerAbstract
   {
    public void mnGameVocabularyDownloadFinished (int downloadStatus)
     {
      if (downloadStatus >= 0)
       {
        onGameVItemsUpdated();
       }
     }
   }

  private static String getVItemsDataUrl (String webServerUrl, MNSession session)
   {
    return webServerUrl + String.format(VITEMS_DATA_URL_FORMAT,session.getGameId(),MNSession.CLIENT_API_VERSION);
   }

  private long generateInitialClientTransactionId ()
   {
    return System.currentTimeMillis();
   }

  private static class TransactionMessageHeaderInfo
   {
    public long srvTransactionId;
    public long cliTransactionId;
    public long corrUserId;
    public int  headerLength;

    public TransactionMessageHeaderInfo (long srvTransactionId,
                                         long cliTransactionId,
                                         long corrUserId,
                                         int headerLength)
     {
      this.srvTransactionId = srvTransactionId;
      this.cliTransactionId = cliTransactionId;
      this.corrUserId       = corrUserId;
      this.headerLength     = headerLength;
     }

    public static TransactionMessageHeaderInfo getHeaderInfoFromMessage (String message)
     {
      int headerLength = message.indexOf('\n');

      if (headerLength < 0)
       {
        return null;
       }

      String[] fields = message.substring(0,headerLength).split("\t");

      if (fields.length != 3)
       {
        return null;
       }

      try
       {
        long cliTransactionId = Long.parseLong(fields[0]);
        long srvTransactionId = Long.parseLong(fields[1]);
        long corrUserId       = Long.parseLong(fields[2]);

        return new TransactionMessageHeaderInfo(cliTransactionId,srvTransactionId,corrUserId,headerLength + 1);
       }
      catch (NumberFormatException e)
       {
        return null;
       }
     }
   }

  private SessionEventHandler               sessionEventHandler;
  private GameVocabularyEventHandler        gameVocabularyEventHandler;
  private long                              playerVItemsOwnerId;
  private ArrayList<PlayerVItemInfo>        playerVItems;
  private long                              clientTransactionId;

  private static final int    MESSAGE_CMD_PREFIX_LEN  = 1;
  private static final String PROVIDER_NAME           = "com.playphone.mn.vi";
  private static final String VITEMS_DATA_URL_FORMAT  = "/data_game_item_list.php?game_id=%d&api_ver=%s";
  private static final char   MESSAGE_FIELD_SEPARATOR = '\t';
  private static final String DATA_FILE_NAME        = "MNVItemsProvider.xml";
  private static final String[] GameVItemEntriesXmlPath = { "GameVocabulary", "MNVItemsProvider", "VItems" };
  private static final String TAG = "MNVItemsProvider";
 }

