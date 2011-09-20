//
//  MNTrackingSystem.java
//  MultiNet client
//
//  Copyright 2011 PlayPhone. All rights reserved.
//

package com.playphone.multinet.core;

import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Locale;
import java.util.TimeZone;

import com.playphone.multinet.MNConst;

public class MNTrackingSystem
 {
  public MNTrackingSystem (MNSession session)
   {
    beaconUrlTemplate   = null;
    shutdownUrlTemplate = null;
    launchTracked       = false;;
    trackingVars        = setupTrackingVars(session);
   }

  public void trackLaunchWithUrlTemplate (String urlTemplate, MNSession session)
   {
    synchronized (this)
     {
      if (launchTracked)
       {
        return;
       }
      else
       {
        launchTracked = true;
       }
     }

    UrlTemplate launchUrlTemplate = new UrlTemplate(urlTemplate,trackingVars);

    launchUrlTemplate.sendLaunchTrackingRequest(session);
   }

  public synchronized void setShutdownUrlTemplate (String urlTemplate, MNSession session)
   {
    shutdownUrlTemplate = new UrlTemplate(urlTemplate,trackingVars);
   }

  public synchronized void trackShutdown (MNSession session)
   {
    if (shutdownUrlTemplate != null)
     {
      shutdownUrlTemplate.sendShutdownTrackingRequest(session);
     }
   }

  public synchronized void setBeaconUrlTemplate (String urlTemplate, MNSession session)
   {
    beaconUrlTemplate = new UrlTemplate(urlTemplate,trackingVars);
   }

  public synchronized void sendBeacon (String beaconAction, String beaconData, MNSession session)
   {
    if (beaconUrlTemplate != null)
     {
      beaconUrlTemplate.sendBeacon(beaconAction,beaconData,session);
     }
   }

  public Map<String,String> getTrackingVars ()
   {
    return trackingVars;
   }

  private HashMap<String,String> setupTrackingVars (MNSession session)
   {
    IMNPlatform            platform = session.getPlatform();
    Locale                 locale   = Locale.getDefault();
    TimeZone               timeZone = TimeZone.getDefault();

    HashMap<String,String> vars = new HashMap<String,String>();

    vars.put("tv_udid",platform.getUniqueDeviceIdentifier());
//    vars.put("tv_device_name","");
    vars.put("tv_device_type",android.os.Build.MODEL);
    vars.put("tv_os_version",android.os.Build.VERSION.RELEASE);
    vars.put("tv_country_code",locale.getISO3Country());
    vars.put("tv_language_code",locale.getLanguage());
    vars.put("mn_game_id",Integer.toString(session.getGameId()));
    vars.put("mn_dev_type",Integer.toString(platform.getDeviceType()));
    vars.put("mn_dev_id",MNUtils.stringGetMD5String(platform.getUniqueDeviceIdentifier()));
    vars.put("mn_client_ver",MNSession.CLIENT_API_VERSION);
    vars.put("mn_client_locale",locale.toString());

    String appVerExternal = platform.getAppVerExternal();
    String appVerInternal = platform.getAppVerInternal();

    vars.put("mn_app_ver_ext",appVerExternal != null ? appVerExternal : null);
    vars.put("mn_app_ver_int",appVerInternal != null ? appVerInternal : null);

    vars.put("mn_launch_time",Long.toString(session.getLaunchTime()));
    vars.put("mn_launch_id",session.getLaunchId());

    vars.put("mn_tz_info",Integer.toString(timeZone.getRawOffset() / 1000) +
                          "+*+" +
                          timeZone.getID().replace('|',' ').replace(',','-'));

    return vars;
   }

  private static class UrlTemplate
   {
    public UrlTemplate (String urlTemplate, HashMap<String,String> trackingVars)
     {
      parseTemplate(urlTemplate,trackingVars);
     }

    public void sendLaunchTrackingRequest (MNSession session)
     {
      sendBeacon(null,null,session);
     }

    public void sendShutdownTrackingRequest (MNSession session)
     {
      sendBeacon(null,null,session);
     }

    public void sendBeacon (String beaconAction, String beaconData, MNSession session)
     {
      String postBody;

      if (userIdVars == null       &&
          userSIdVars == null      &&
          beaconActionVars == null &&
          beaconDataVars == null)
       {
        postBody = postBodyStringBuilder.toString();
       }
      else
       {
        MNUtils.HttpPostBodyStringBuilder builder = new MNUtils.HttpPostBodyStringBuilder(postBodyStringBuilder);

        long   userId     = session.getMyUserId();
        String userSIdStr = session.getMySId();

        String userIdStr = userId != MNConst.MN_USER_ID_UNDEFINED ?
                            Long.toString(userId) : "";

        if (userSIdStr == null)
         {
          userSIdStr = "";
         }

        if (userIdVars != null)
         {
          for (int i = 0; i < userIdVars.size(); i++)
           {
            builder.addParamWithEncodingFlags(userIdVars.get(i),userIdStr,false,true);
           }
         }

        if (userSIdVars != null)
         {
          for (int i = 0; i < userSIdVars.size(); i++)
           {
            builder.addParamWithEncodingFlags(userSIdVars.get(i),userSIdStr,false,true);
           }
         }

        if (beaconActionVars != null)
         {
          if (beaconAction == null)
           {
            beaconAction = "";
           }

          for (int i = 0; i < beaconActionVars.size(); i++)
           {
            builder.addParamWithEncodingFlags(beaconActionVars.get(i),beaconAction,false,true);
           }
         }

        if (beaconDataVars != null)
         {
          if (beaconData == null)
           {
            beaconData = "";
           }

          for (int i = 0; i < beaconDataVars.size(); i++)
           {
            builder.addParamWithEncodingFlags(beaconDataVars.get(i),beaconData,false,true);
           }
         }

        postBody = builder.toString();
       }

      MNURLTextDownloader downloader = new MNURLTextDownloader();

      downloader.loadURL(url,postBody,new MNURLTextDownloader.IEventHandler()
       {
        public void downloaderDataReady (MNURLDownloader downloader, String[] data)
         {
         }

        public void downloaderLoadFailed (MNURLDownloader downloader, MNURLDownloader.ErrorInfo errorInfo)
         {
         }
       });
     }

    private void parseTemplate (String urlTemplate, HashMap<String,String> trackingVars)
     {
      int    pos;
      String paramString;

      pos = urlTemplate.indexOf('?');

      if (pos >= 0)
       {
        url         = urlTemplate.substring(0,pos);
        paramString = urlTemplate.substring(pos + 1);
       }
      else
       {
        url         = urlTemplate;
        paramString = "";
       }

      postBodyStringBuilder = new MNUtils.HttpPostBodyStringBuilder();

      userIdVars       = null;
      userSIdVars      = null;
      beaconActionVars = null;
      beaconDataVars   = null;

      if (paramString.length() > 0)
       {
        String[] params = paramString.split("&");

        for (int index = 0; index < params.length; index++)
         {
          String param = params[index];
          String name;
          String value;

          pos = param.indexOf('=');

          if (pos >= 0)
           {
            name  = param.substring(0,pos);
            value = param.substring(pos + 1);
           }
          else
           {
            name  = param;
            value = "";
           }

          String metaVarName = getMetaVarName(value);

          if (metaVarName != null)
           {
            value = trackingVars.get(metaVarName);

            if (value != null)
             {
              postBodyStringBuilder.addParamWithEncodingFlags(name,value,false,true);
             }
            else if (metaVarName.equals("mn_user_id"))
             {
              if (userIdVars == null)
               {
                userIdVars = new ArrayList<String>();
               }

              userIdVars.add(name);
             }
            else if (metaVarName.equals("mn_user_sid"))
             {
              if (userSIdVars == null)
               {
                userSIdVars = new ArrayList<String>();
               }

              userSIdVars.add(name);
             }
            else if (metaVarName.equals("bt_beacon_action_name"))
             {
              if (beaconActionVars == null)
               {
                beaconActionVars = new ArrayList<String>();
               }

              beaconActionVars.add(name);
             }
            else if (metaVarName.equals("bt_beacon_data"))
             {
              if (beaconDataVars == null)
               {
                beaconDataVars = new ArrayList<String>();
               }

              beaconDataVars.add(name);
             }
            else
             {
              postBodyStringBuilder.addParamWithoutEncoding(name,"");
             }
           }
          else
           {
            postBodyStringBuilder.addParamWithoutEncoding(name,value);
           }
         }
       }
     }

    private static String getMetaVarName (String str)
     {
      if (str.startsWith("{") && str.endsWith("}"))
       {
        return str.substring(1,str.length() - 1);
       }
      else
       {
        return null;
       }
     }

    private String                            url;
    private MNUtils.HttpPostBodyStringBuilder postBodyStringBuilder;
    private ArrayList<String>                 userIdVars;
    private ArrayList<String>                 userSIdVars;
    private ArrayList<String>                 beaconActionVars;
    private ArrayList<String>                 beaconDataVars;
   }

  private UrlTemplate            beaconUrlTemplate;
  private UrlTemplate            shutdownUrlTemplate;
  private boolean                launchTracked;
  private HashMap<String,String> trackingVars;
 }

