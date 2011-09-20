//
//  MNAchievementsProvider.java
//  MultiNet client
//
//  Copyright 2010 PlayPhone. All rights reserved.
//

package com.playphone.multinet.providers;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import com.playphone.multinet.MNUserInfo;
import com.playphone.multinet.MNConst;
import com.playphone.multinet.core.MNUtils;
import com.playphone.multinet.core.MNSession;
import com.playphone.multinet.core.MNSessionEventHandlerAbstract;
import com.playphone.multinet.core.MNGameVocabulary;
import com.playphone.multinet.core.MNEventHandlerArray;
import com.playphone.multinet.core.ws.MNWSXmlTools;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * A class representing "Achievements" MultiNet provider.
 *
 * "Achievements" provider provides game achievement support. It allows to get available game achievements
 * information, get information on achievements unlocked by player as well as unlock player achievements.
 */
public class MNAchievementsProvider
 {
  public static final String PROVIDER_NAME = "com.playphone.mn.at";

  /**
   * Interface handling achievements events.
   */
  public interface IEventHandler
   {
    /**
     * Invoked when the list of game achievements has been updated as a
     * result of MNAchievementProvider's doGameAchievementListUpdate call.
     */
    void onGameAchievementListUpdated ();


    /**
     * Invoked when server unlocks achievement for player.
     * @param achievementId identifier of unlocked achievement.
     */
    void onPlayerAchievementUnlocked  (int achievementId);
   }

  /**
   * A class which implements IEventHandler interface by ignoring all
   * received events.
   */
  public static class EventHandlerAbstract implements IEventHandler
   {
    public void onGameAchievementListUpdated ()
     {
     }

    public void onPlayerAchievementUnlocked  (int achievementId)
     {
     }
   }

  /**
   * A class representing game achievement information.
   */
  public static class GameAchievementInfo
   {
    /**
     * Achievement identifier - unique identifier of game achievement.
     */
    public int    id;

    /**
     * Name of achievement.
     */
    public String name;

    /**
     * Achievement flags.
     */
    public int    flags;

    /**
     * Achievement description.
     */
    public String description;

    /**
     * Achievement parameters.
     */
    public String params;

    /**
     * Achievement points.
     */
    public int points;

    /**
     * Constructs a new <code>GameAchievementInfo</code> object.
     *
     * @param id achievement identifier
     * @param name achievement name
     * @param flags achievement flags
     * @param description achievement description
     * @param params achievement parameters
     * @param points achievement points
     */
    public GameAchievementInfo (int id, String name, int flags, String description, String params, int points)
     {
      this.id          = id;
      this.name        = name;
      this.flags       = flags;
      this.description = description;
      this.params      = params;
      this.points      = points;
     }
   }

  /**
   * A class representing player achievement information.
   */
  public static class PlayerAchievementInfo
   {
    /**
     * Achievement identifier - unique identifier of game achievement.
     */
    public int id;

    /**
     * Constructs a new <code>PlayerAchievementInfo</code> object.
     *
     * @param id achievement identifier
     */
    public PlayerAchievementInfo (int id)
     {
      this.id = id;
     }
   }

  /**
   * Constructs a new <code>MNAchievementsProvider</code> object.
   *
   * @param session         MultiNet session instance
   */
  public MNAchievementsProvider (MNSession session)
   {
    sessionEventHandler  = new SessionEventHandler(session);
    unlockedAchievements = new ArrayList<PlayerAchievementInfo>();

    fillUnlockedAchievementsArray(session);

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

    unlockedAchievements.clear();
    unlockedAchievements = null;
   }

  private ArrayList<GameAchievementInfo> getGameAchievementsListLow ()
   {
    ArrayList<GameAchievementInfo> achievements = new ArrayList<GameAchievementInfo>();
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

        Element listElement = MNWSXmlTools.documentGetElementByPath(dom,AchievementsEntriesXmlPath);

        if (listElement == null)
         {
          throw new Exception("cannot find \"Achievements\" element in document");
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
            int    flags;
            String desc;
            String params;
            int    points;

            name   = MNUtils.parseStringWithDefault(itemData.get("name"),"");
            flags  = MNUtils.parseIntWithDefault(itemData.get("flags"),0);
            desc   = MNUtils.parseStringWithDefault(itemData.get("desc"),"");
            params = MNUtils.parseStringWithDefault(itemData.get("params"),"");
            points = MNUtils.parseIntWithDefault(itemData.get("points"),0);

            achievements.add(new GameAchievementInfo(id,name,flags,desc,params,points));
           }
          else
           {
            sessionEventHandler.session.getPlatform().logWarning(TAG,"game achievement data with invalid or absent achievement id ignored");
           }
         }
       }
      catch (Exception e)
       {
        achievements.clear();

        sessionEventHandler.session.getPlatform().logWarning(TAG,String.format("game achievement data parsing failed (%s)",e.toString()));
       }
     }

    return achievements;
   }

  /**
   * Returns a list of all available game achivements.
   *
   * @return array of game achievements.
   */
  public GameAchievementInfo[] getGameAchievementsList ()
   {
    ArrayList<GameAchievementInfo> achievements = getGameAchievementsListLow();

    return achievements.toArray(new GameAchievementInfo[achievements.size()]);
   }

  /**
   * Returns achievement information by achievement identifier.
   *
   * @return GameAchievementInfo object or null if there is no such achievement.
   */
  public GameAchievementInfo findGameAchievementById (int id)
   {
    ArrayList<GameAchievementInfo> achievements = getGameAchievementsListLow();

    boolean found = false;
    int     index = 0;
    int     count = achievements.size();

    GameAchievementInfo achievement = null;

    while (!found && index < count)
     {
      achievement = achievements.get(index);

      if (achievement.id == id)
       {
        found = true;
       }
      else
       {
        index++;
       }
     }

    return found ? achievement : null;
   }

  /**
   * Returns state of game achievements list.
   *
   * @return <code>true</code> if newer achievement list is available on server, <code>false</code> - otherwise.
   */
  public boolean isGameAchievementListNeedUpdate ()
   {
    return sessionEventHandler.session.getGameVocabulary().getVocabularyStatus() > 0;
   }

  /**
   * Starts game achievements info update. On successfull completion eventHandlers's
   * onGameAchievementListUpdated method will be called.
   */
  public void doGameAchievementListUpdate ()
   {
    if (sessionEventHandler.session.getGameVocabulary().getVocabularyStatus() !=
         MNGameVocabulary.MN_GV_UPDATE_STATUS_DOWNLOAD_IN_PROGRESS)
     {
      sessionEventHandler.session.getGameVocabulary().startDownload();
     }
   }

  /**
   * Checks if achievement had been unlocked by player.
   *
   * @param id achievement identifier
   * @return <code>true</code> if player unlocked achievement, <code>false</code> - otherwise.
   */
  public boolean isPlayerAchievementUnlocked (int id)
   {
    int count = unlockedAchievements.size();

    for (int i = 0; i < count; i++)
     {
      if (unlockedAchievements.get(i).id == id)
       {
        return true;
       }
     }

    return false;
   }

  private synchronized boolean addUniqueAchievement (int id)
   {
    if (!isPlayerAchievementUnlocked(id))
     {
      unlockedAchievements.add(new PlayerAchievementInfo(id));

      return true;
     }
    else
     {
      return false;
     }
   }

  /**
   * Unlocks player achievement.
   *
   * @param id achievement identifier to unlock
   */
  public synchronized void unlockPlayerAchievement (int id)
   {
    MNSession session = sessionEventHandler.session;

    if (session.isUserLoggedIn())
     {
      addUniqueAchievement(id);

      if (session.isOnline())
       {
        session.sendPluginMessage(PROVIDER_NAME,"A" + Integer.toString(id));
       }
      else
       {
        session.varStorageSetValue
         (getOfflineUnlockedAchievementVarName(session.getMyUserId(),id),
          Long.toString(MNUtils.getUnixTime()));

        sessionEventHandler.eventHandlers.beginCall();

        try
         {
          int count = sessionEventHandler.eventHandlers.size();

          for (int index = 0; index < count; index++)
           {
            sessionEventHandler.eventHandlers.get(index).onPlayerAchievementUnlocked(id);
           }
         }
        finally
         {
          sessionEventHandler.eventHandlers.endCall();
         }
       }
     }
   }

  /**
   * Returns a list of unlocked achivements.
   *
   * @return array of unlocked achievements.
   */
  public synchronized PlayerAchievementInfo[] getPlayerAchievementsList ()
   {
    PlayerAchievementInfo[] result;

    result = unlockedAchievements.toArray
              (new PlayerAchievementInfo[unlockedAchievements.size()]);

    return result;
   }

  /**
   * Adds event handler
   *
   * @param eventHandler an object that implements
   * {@link IEventHandler IEventHandler} interface
   */
  public void addEventHandler (IEventHandler eventHandler)
   {
    sessionEventHandler.eventHandlers.add(eventHandler);
   }

  /**
   * Removes event handler
   *
   * @param eventHandler an object that implements
   * {@link IEventHandler IEventHandler} interface
   */
  public void removeEventHandler (IEventHandler eventHandler)
   {
    sessionEventHandler.eventHandlers.remove(eventHandler);
   }

  /**
   * Returns URL of achievement image
   *
   * @return image URL
   */
  public String getAchievementImageURL (int id)
   {
    String webServerUrl = sessionEventHandler.session.getWebServerURL();

    if (webServerUrl != null)
     {
      StringBuilder builder = new StringBuilder(webServerUrl);

      builder.append("/data_game_achievement_image.php?game_id=");
      builder.append(Integer.toString(sessionEventHandler.session.getGameId()));
      builder.append("&game_achievement_id=");
      builder.append(Integer.toString(id));

      return builder.toString();
     }
    else
     {
      return null;
     }
   }

  private class SessionEventHandler extends MNSessionEventHandlerAbstract
   {
    public SessionEventHandler (MNSession      session)
     {
      this.session       = session;
      this.eventHandlers = new MNEventHandlerArray<IEventHandler>();

      session.addEventHandler(this);
     }

    public synchronized void shutdown ()
     {
      session.removeEventHandler(this);

      session       = null;
      eventHandlers = null;
     }

    private ArrayList<PlayerAchievementInfo> parsePlayerAchievementsListMessage (String message)
     {
      String[] achievementsInfoArray = message.split("\n");
      int index  = 0;
      int count  = achievementsInfoArray.length;
      boolean ok = true;

      ArrayList<PlayerAchievementInfo> result = new ArrayList<PlayerAchievementInfo>();

      while (index < count && ok)
       {
        String info = achievementsInfoArray[index];
        int    id;

        if (info.length() > 0)
         {
          try
           {
            id = Integer.parseInt(messageGetFirstField(info));

            result.add(new PlayerAchievementInfo(id));
           }
          catch (NumberFormatException e)
           {
            ok = false;
           }
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

    private void processUserAddAchievementMessage(String message)
     {
      try
       {
        int id = Integer.parseInt(messageGetFirstField(message));

        synchronized(MNAchievementsProvider.this)
         {
          if (addUniqueAchievement(id))
           {
            session.varStorageSetValue
             (getServerUnlockedAchievementsVarName(session.getMyUserId()),
              getCommaSeparatedAchievemensList(unlockedAchievements));
           }

          eventHandlers.beginCall();

          try
           {
            int count = eventHandlers.size();

            for (int index = 0; index < count; index++)
             {
              eventHandlers.get(index).onPlayerAchievementUnlocked(id);
             }
           }
          finally
           {
            eventHandlers.endCall();
           }
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
        ArrayList<PlayerAchievementInfo> newPlayerAchievements =
         parsePlayerAchievementsListMessage(data);

        if (newPlayerAchievements != null)
         {
          synchronized(MNAchievementsProvider.this)
           {
            int count = newPlayerAchievements.size();

            for (int index = 0; index < count; index++)
             {
              addUniqueAchievement(newPlayerAchievements.get(index).id);
             }

            session.varStorageSetValue
             (getServerUnlockedAchievementsVarName(session.getMyUserId()),
              getCommaSeparatedAchievemensList(unlockedAchievements));
           }
         }
       }
      else if (cmd == 'a')
       {
        processUserAddAchievementMessage(data);
       }
     }

    public void mnSessionUserChanged (long userId)
     {
      if (userId == MNConst.MN_USER_ID_UNDEFINED)
       {
        unlockedAchievements.clear();
       }
      else
       {
        fillUnlockedAchievementsArray(session);
       }
     }

    MNSession     session;
    MNEventHandlerArray<IEventHandler> eventHandlers;
   }

  static String messageGetFirstField (String message)
   {
    int separatorIndex = message.indexOf(MESSAGE_FIELD_SEPARATOR);

    if (separatorIndex < 0)
     {
      return message;
     }
    else
     {
      return message.substring(0,separatorIndex);
     }
   }

  private static String getServerUnlockedAchievementsVarName (long userId)
   {
    return "offline." + Long.toString(userId) + ".achievement_saved_list";
   }

  private static String getOfflineUnlockedAchievementVarName (long userId, int achievementId)
   {
    return "offline." + Long.toString(userId) + ".achievement_pending." +
            Integer.toString(achievementId) + ".date";
   }

  private static String getCommaSeparatedAchievemensList (ArrayList<PlayerAchievementInfo> achievements)
   {
    MNUtils.StringJoiner joiner = new MNUtils.StringJoiner(",");
    int count = achievements.size();

    for (int index = 0; index < count; index++)
     {
      joiner.join(Integer.toString(achievements.get(index).id));
     }

    return joiner.toString();
   }

  private void fillUnlockedAchievementsArray (MNSession session)
   {
    unlockedAchievements.clear();

    long userId = session.getMyUserId();

    if (userId == MNConst.MN_USER_ID_UNDEFINED)
     {
      return;
     }

    /* load achievements confirmed by server */

    String   serverAchievementsList = session.varStorageGetValueForVariable
                                       (getServerUnlockedAchievementsVarName(userId));
    String[] serverAchievements = serverAchievementsList == null ?
                                   null :
                                   serverAchievementsList.split(",");

    Integer achievementId;

    if (serverAchievements != null)
     {
      for (int index = 0; index < serverAchievements.length; index++)
       {
        achievementId = MNUtils.parseInteger(serverAchievements[index]);

        if (achievementId != null)
         {
          addUniqueAchievement(achievementId);
         }
       }
     }

    /* load achievements unlocked in offline mode */

    String[] masks = { "offline." + Long.toString(userId) + ".achievement.pending.*" };
    Map<String,String> offlineAchievements = session.varStorageGetValuesByMasks(masks);
    String[] varNameComponents;

    for (String varName : offlineAchievements.keySet())
     {
      varNameComponents = varName.split("\\.");

      if (varNameComponents.length > 3)
       {
        achievementId = MNUtils.parseInteger(varNameComponents[3]);

        if (achievementId != null)
         {
          addUniqueAchievement(achievementId);
         }
       }
     }
   }

  private synchronized void onGameAchievementsUpdated()
   {
    sessionEventHandler.eventHandlers.beginCall();

    try
     {
      int count = sessionEventHandler.eventHandlers.size();

      for (int index = 0; index < count; index++)
       {
        sessionEventHandler.eventHandlers.get(index).onGameAchievementListUpdated();
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
        onGameAchievementsUpdated();
       }
     }
   }

  private SessionEventHandler              sessionEventHandler;
  private GameVocabularyEventHandler       gameVocabularyEventHandler;
  ArrayList<PlayerAchievementInfo>         unlockedAchievements;

  private static final int MESSAGE_CMD_PREFIX_LEN   = 1;
  private static final char MESSAGE_FIELD_SEPARATOR = '\t';
  private static final String DATA_FILE_NAME        = "MNAchievementsProvider.xml";
  private static final String[] AchievementsEntriesXmlPath = { "GameVocabulary", "MNAchievementsProvider", "Achievements" };
  private static final String TAG = "MNAchievementsProvider";
 }

