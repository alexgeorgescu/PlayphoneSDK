//
//  MNConfigData.java
//  MultiNet client
//
//  Copyright 2009 PlayPhone. All rights reserved.
//

package com.playphone.multinet.core;

import java.util.Hashtable;

class MNConfigData implements MNURLTextDownloader.IEventHandler
 {
  public interface IEventHandler
   {
    void mnConfigDataLoaded     (MNConfigData configData);
    void mnConfigDataLoadFailed (String       errorMessage);
   }

  public MNConfigData (String configUrl)
   {
    this.configUrl = configUrl;
    loaded         = false;
    downloader     = new MNURLTextDownloader();
   }

  public synchronized boolean isLoaded ()
   {
    return loaded;
   }

  public synchronized void clear ()
   {
    loaded = false;

    smartFoxAddr       = null;
    smartFoxPort       = 0;
    blueBoxAddr        = null;
    blueBoxPort        = 0;
    smartConnect       = false;
    webServerUrl       = null;
    facebookAPIKey     = null;
    facebookAppId      = null;
    launchTrackerUrl   = null;
    shutdownTrackerUrl = null;
    beaconTrackerUrl   = null;
    gameVocabularyVersion = null;
   }

  public synchronized void load (IEventHandler eventHandler)
   {
    clear();

    this.eventHandler = eventHandler;

    if (configUrl != null)
     {
      downloader.loadURL(configUrl,this);
     }
    else
     {
      eventHandler.mnConfigDataLoadFailed("Configuration URL is not set");
     }
   }

  public void downloaderDataReady  (MNURLDownloader downloader, String[] data)
   {
    boolean ok = true;

    Hashtable<String,String> params = parseConfig(data);

    if (params != null)
     {
      try
       {
        smartFoxAddr       = parseParamString(params,SMARTFOX_SERVER_ADDR_PARAM);
        smartFoxPort       = parseParamInteger(params,SMARTFOX_SERVER_PORT_PARAM);
        blueBoxAddr        = parseParamString(params,BLUEBOX_SERVER_ADDR_PARAM);
        blueBoxPort        = parseParamInteger(params,BLUEBOX_SERVER_PORT_PARAM);
        smartConnect       = parseParamBoolean(params,SMARTFOX_SMART_CONNECT_PARAM);
        webServerUrl       = parseParamString(params,MULTINET_WEBSERVER_URL_PARAM);
        facebookAPIKey     = parseParamString(params,FACEBOOK_API_KEY_PARAM);
        facebookAppId      = parseParamString(params,FACEBOOK_APP_ID_PARAM);
        launchTrackerUrl   = params.get(LAUNCH_TRACKER_URL_PARAM);
        shutdownTrackerUrl = params.get(SHUTDOWN_TRACKER_URL_PARAM);
        beaconTrackerUrl   = params.get(BEACON_TRACKER_URL_PARAM);
        gameVocabularyVersion = params.get(GAME_VOCABULARY_VERSION_PARAM);
       }
      catch (IllegalArgumentException e)
       {
        ok = false;
       }
     }
    else
     {
      ok = false;
     }

    if (ok)
     {
      loaded = true;

      eventHandler.mnConfigDataLoaded(this);
     }
    else
     {
      clear();

      eventHandler.mnConfigDataLoadFailed("Invalid configuration file format");
     }

    eventHandler = null;
   }

  public void downloaderLoadFailed (MNURLDownloader downloader, MNURLDownloader.ErrorInfo errorInfo)
   {
    eventHandler.mnConfigDataLoadFailed(errorInfo.getMessage());

    eventHandler = null;
   }

  private Hashtable<String,String> parseConfig (String[] content)
   {
    Hashtable<String,String> result = new Hashtable<String,String>();
    int index  = 0;
    int count  = content.length;
    boolean ok = true;
    String   line;
    String[] parts;

    while (index < count && ok)
     {
      line = content[index].trim();

      if (line.length() > 0)
       {
        parts = line.split("=",2);

        if (parts.length == 2)
         {
          result.put(parts[0].trim(),parts[1].trim());
         }
        else
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

  private String parseParamString (Hashtable<String,String> params, String name)
                 throws IllegalArgumentException
   {
    String result = params.get(name);

    if (result == null)
     {
      throw new IllegalArgumentException();
     }

    return result;
   }

  private int parseParamInteger (Hashtable<String,String> params, String name)
              throws IllegalArgumentException
   {
    int    result = 0;
    String value  = params.get(name);

    if (value == null)
     {
      throw new IllegalArgumentException();
     }

    try
     {
      result = Integer.parseInt(value,10);
     }
    catch (NumberFormatException e)
     {
      throw new IllegalArgumentException();
     }

    return result;
   }

  private boolean parseParamBoolean (Hashtable<String,String> params, String name)
                  throws IllegalArgumentException
   {
    boolean result = false;
    String value   = params.get(name);

    if (value == null)
     {
      throw new IllegalArgumentException();
     }

    if      (value.equals("true"))
     {
      result = true;
     }
    else if (value.equals("false"))
     {
      result = false;
     }
    else
     {
      throw new IllegalArgumentException();
     }

    return result;
   }

  private String              configUrl;
  private boolean             loaded;
  private MNURLTextDownloader downloader;
  private IEventHandler       eventHandler;

  public String  smartFoxAddr;
  public int     smartFoxPort;
  public String  blueBoxAddr;
  public int     blueBoxPort;
  public boolean smartConnect;
  public String  webServerUrl;
  public String  facebookAPIKey;
  public String  facebookAppId;
  public String  launchTrackerUrl;
  public String  beaconTrackerUrl;
  public String  shutdownTrackerUrl;
  public String  gameVocabularyVersion;

  private static final String SMARTFOX_SERVER_ADDR_PARAM   = "SmartFoxServerAddr";
  private static final String SMARTFOX_SERVER_PORT_PARAM   = "SmartFoxServerPort";
  private static final String BLUEBOX_SERVER_ADDR_PARAM    = "BlueBoxServerAddr";
  private static final String BLUEBOX_SERVER_PORT_PARAM    = "BlueBoxServerPort";
  private static final String SMARTFOX_SMART_CONNECT_PARAM = "BlueBoxSmartConnect";
  private static final String MULTINET_WEBSERVER_URL_PARAM = "MultiNetWebServerURL";
  private static final String FACEBOOK_API_KEY_PARAM       = "FacebookApiKey";
  private static final String FACEBOOK_APP_ID_PARAM        = "FacebookAppId";
  private static final String LAUNCH_TRACKER_URL_PARAM     = "LaunchTrackerURL";
  private static final String SHUTDOWN_TRACKER_URL_PARAM   = "ShutdownTrackerURL";
  private static final String BEACON_TRACKER_URL_PARAM     = "BeaconTrackerURL";
  private static final String GAME_VOCABULARY_VERSION_PARAM = "GameVocabularyVersion";
 }

