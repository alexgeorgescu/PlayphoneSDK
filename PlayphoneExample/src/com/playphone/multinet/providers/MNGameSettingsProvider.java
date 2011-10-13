//
//  MNGameSettingsProvider.java
//  MultiNet client
//
//  Copyright 2011 PlayPhone. All rights reserved.
//

package com.playphone.multinet.providers;

import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.playphone.multinet.core.MNSession;
import com.playphone.multinet.core.MNGameVocabulary;
import com.playphone.multinet.core.MNUtils;
import com.playphone.multinet.core.MNEventHandlerArray;
import com.playphone.multinet.core.ws.MNWSXmlTools;

/**
 * A class representing "Game settings" MultiNet provider.
 *
 * "Game settings" provider provides information on available game settings.
 */
public class MNGameSettingsProvider
 {
  /**
   * Interface handling game settings events.
   */
  public interface IEventHandler
   {
    /**
     * Invoked when the list of game settings has been updated as a
     * result of MNGameSettingsProvider's doGameSettingListUpdate call.
     */
    void onGameSettingListUpdated ();
   }

  /**
   * A class which implements IEventHandler interface by ignoring all
   * received events.
   */
  public static class EventHandlerAbstract implements IEventHandler
   {
    public void onGameSettingListUpdated ()
     {
     }
   }

  /**
   * A class representing game setting information.
   */
  public static class GameSettingInfo
   {
    /**
     * Constructs a new <code>GameSettingInfo</code> object.
     *
     * @param id game setting identifier
     * @param name game setting name
     * @param params game setting parameters
     * @param sysParams game setting system parameters
     * @param multiplayerEnabled flag which shows if multiplayer is enabled for given setting
     * @param leaderboardVisible flag which shows if leaderboard is visible for given setting
     */
    public GameSettingInfo (int id, String name, String params,String sysParams,
                            boolean multiplayerEnabled,
                            boolean leaderboardVisible)
     {
      this.id                 = id;
      this.name               = name;
      this.params             = params;
      this.sysParams          = sysParams;
      this.multiplayerEnabled = multiplayerEnabled;
      this.leaderboardVisible = leaderboardVisible;
     }

    /**
     * Returns game setting identifier
     *
     * @return game setting identifier
     */
    public int getId ()
     {
      return id;
     }

    /**
     * Returns name of the game setting
     *
     * @return name of the game setting
     */
    public String getName ()
     {
      return name;
     }

    /**
     * Returns game setting parameters
     *
     * @return game setting parameters
     */
    public String getParams ()
     {
      return params;
     }

    /**
     * Returns game setting system parameters
     *
     * @return game setting system parameters
     */
    public String getSysParams ()
     {
      return sysParams;
     }

    /**
     * Checks if multiplayer is enabled for game setting
     *
     * @return <code>true</code> if multiplayer mode is enabled for this
     * game setting and <code>false</code> otherwise.
     */
    public boolean isMultiplayerEnabled ()
     {
      return multiplayerEnabled;
     }

    /**
     * Checks if leaderboard is visible for game setting
     *
     * @return <code>true</code> if leaderboard is visible for this
     * game setting and <code>false</code> otherwise.
     */
    public boolean isLeaderboardVisible ()
     {
      return leaderboardVisible;
     }

    private int     id;
    private String  name;
    private String  params;
    private String  sysParams;
    private boolean multiplayerEnabled;
    private boolean leaderboardVisible;
   }

  /**
   * Constructs a new <code>MNGameSettingsProvider</code> object.
   *
   * @param session         MultiNet session instance
   */
  public MNGameSettingsProvider (MNSession session)
   {
    this.session = session;

    eventHandlers = new MNEventHandlerArray<IEventHandler>();

    gameVocabularyEventHandler = new GameVocabularyEventHandler();

    session.getGameVocabulary().addEventHandler(gameVocabularyEventHandler);
   }

  /**
   * Stops provider and frees all allocated resources.
   */
  public synchronized void shutdown ()
   {
    eventHandlers.clearAll();

    session.getGameVocabulary().removeEventHandler(gameVocabularyEventHandler);
   }

  /**
   * Returns a list of all available game settings.
   *
   * @return array of game settings.
   */
  public GameSettingInfo[] getGameSettingList ()
   {
    ArrayList<GameSettingInfo> gameSettings = getGameSettingListLow();

    return gameSettings.toArray(new GameSettingInfo[gameSettings.size()]);
   }

  /**
   * Returns game setting information by game setting identifier.
   *
   * @return GameSettingInfo object or null if there is no such game setting.
   */
  public GameSettingInfo findGameSettingById (int id)
   {
    ArrayList<GameSettingInfo> gameSettings = getGameSettingListLow();

    boolean found = false;
    int     index = 0;
    int     count = gameSettings.size();

    GameSettingInfo gameSetting = null;

    while (!found && index < count)
     {
      gameSetting = gameSettings.get(index);

      if (gameSetting.id == id)
       {
        found = true;
       }
      else
       {
        index++;
       }
     }

    return found ? gameSetting : null;
   }

  /**
   * Returns state of game setting list.
   *
   * @return <code>true</code> if newer game setting list is available on server, <code>false</code> - otherwise.
   */
  public boolean isGameSettingListNeedUpdate ()
   {
    return session.getGameVocabulary().getVocabularyStatus() > 0;
   }

  /**
   * Starts game settings information update. On successfull completion eventHandlers's
   * onGameSettingListUpdated method will be called.
   */
  public void doGameSettingListUpdate ()
   {
    if (session.getGameVocabulary().getVocabularyStatus() !=
         MNGameVocabulary.MN_GV_UPDATE_STATUS_DOWNLOAD_IN_PROGRESS)
     {
      session.getGameVocabulary().startDownload();
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


  private ArrayList<GameSettingInfo> getGameSettingListLow ()
   {
    ArrayList<GameSettingInfo> gameSettings = new ArrayList<GameSettingInfo>();
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

        Element listElement = MNWSXmlTools.documentGetElementByPath(dom,GameSettingEntriesXmlPath);

        if (listElement == null)
         {
          throw new Exception("cannot find \"GameSettings\" element in document");
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
            String  name               = MNUtils.parseStringWithDefault(itemData.get("name"),"");
            String  params             = MNUtils.parseStringWithDefault(itemData.get("params"),"");
            String  sysParams          = MNUtils.parseStringWithDefault(itemData.get("sysParams"),"");
            boolean multiplayerEnabled = parseBooleanString(itemData.get("isMultiplayerEnabled"));
            boolean leaderboardVisible = parseBooleanString(itemData.get("isLeaderboardVisible"));

            gameSettings.add(new GameSettingInfo(id,name,params,sysParams,multiplayerEnabled,leaderboardVisible));
           }
          else
           {
            session.getPlatform().logWarning(TAG,"game settings data with invalid or absent game setting id ignored");
           }
         }
       }
      catch (Exception e)
       {
        gameSettings.clear();

        session.getPlatform().logWarning(TAG,String.format("game setting data parsing failed (%s)",e.toString()));
       }
     }

    return gameSettings;
   }

  private synchronized void onGameSettingsUpdated()
   {
    eventHandlers.callHandlers(new MNEventHandlerArray.ICaller<IEventHandler>()
     {
      public void callHandler (IEventHandler handler)
       {
        handler.onGameSettingListUpdated();
       }
     });
   }

  private class GameVocabularyEventHandler extends MNGameVocabulary.EventHandlerAbstract
   {
    public void mnGameVocabularyDownloadFinished (int downloadStatus)
     {
      if (downloadStatus >= 0)
       {
        onGameSettingsUpdated();
       }
     }
   }

  private static boolean parseBooleanString (String s)
   {
    if (s == null)
     {
      return false;
     }

    return s.equals("true");
   }

  private final MNSession                          session;
  private final GameVocabularyEventHandler         gameVocabularyEventHandler;
  private final MNEventHandlerArray<IEventHandler> eventHandlers;

  private static final String[] GameSettingEntriesXmlPath = { "GameVocabulary", "MNGameSettingsProvider", "GameSettings" };
  private static final String DATA_FILE_NAME = "MNGameSettingsProvider.xml";
  private static final String TAG = "MNGameSettingsProvider";
 }

