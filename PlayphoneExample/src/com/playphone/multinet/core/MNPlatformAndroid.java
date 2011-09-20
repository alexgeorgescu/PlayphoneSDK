//
//  MNPlatformAndroid.java
//  MultiNet client
//
//  Copyright 2009 PlayPhone. All rights reserved.
//

package com.playphone.multinet.core;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Locale;
import java.util.TimeZone;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlPullParserException;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.provider.Settings.Secure;
import android.os.Build;
import android.util.Log;

/**
 * A class representing Android platform abstraction.
 * This class is an {@link IMNPlatform IMNPlatform} interface implementation
 * for Android platform.
 */
public class MNPlatformAndroid implements IMNPlatform
 {
  /**
   * Constructs a new <code>MNPlatformAndroid</code> object.
   *
   * @param activity activity
   */
  public MNPlatformAndroid (Activity activity)
   {
    this.activity = activity;
   }

  public int    getDeviceType ()
   {
    return DEVICE_TYPE_CODE;
   }

  public String getUniqueDeviceIdentifier ()
   {
    String id = Secure.getString(activity.getContentResolver(),Secure.ANDROID_ID);

    return id != null ? id : EMULATOR_UNIQUE_DEVICE_ID;
   }

  private static String replaceBarWithSpace (String s)
   {
    return s.replace('|',' ');
   }

  public String getDeviceInfoString ()
   {
    Locale   locale   = Locale.getDefault();
    TimeZone timezone = TimeZone.getDefault();
    String   timezoneId = timezone.getID();

    String info = replaceBarWithSpace(Build.MODEL) + "|" +
                  replaceBarWithSpace(ANDROID_OS_NAME) + "|" +
                  replaceBarWithSpace(Build.VERSION.RELEASE) + "|" +
                  replaceBarWithSpace(locale.toString()) + "|{" +
                  Integer.toString(timezone.getRawOffset() / 1000) + "+" +
                  TIMEZONE_ABBR_NOT_AVAILABLE + "+" +
                  replaceBarWithSpace(timezoneId) + "}";

    return info;
   }

  public String getAppVerExternal ()
   {
    try
     {
      PackageInfo packageInfo = activity.getPackageManager().
                                 getPackageInfo(activity.getPackageName(),0);

      return packageInfo.versionName;
     }
    catch (PackageManager.NameNotFoundException e)
     {
      return null;
     }
   }

  public String getAppVerInternal ()
   {
    try
     {
      PackageInfo packageInfo = activity.getPackageManager().
                                 getPackageInfo(activity.getPackageName(),0);

      return Integer.toString(packageInfo.versionCode);
     }
    catch (PackageManager.NameNotFoundException e)
     {
      return null;
     }
   }

  public String getMultiNetConfigURL ()
   {
    try
     {
      Dictionary<String,String> params = loadMultiNetConfFile();

      return params.get(MULTINET_CONFIG_URL_PARAM);
     }
    catch (MNException e)
     {
      return null;
     }
   }

  public InputStream  openFileForInput  (String path) throws FileNotFoundException
   {
    return activity.openFileInput(path);
   }

  public OutputStream openFileForOutput (String path) throws FileNotFoundException
   {
    return activity.openFileOutput(path,0);
   }

  public InputStream  openAssetForInput  (String name)
   {
    InputStream stream = null;

    try
     {
      stream = activity.getAssets().open(name);
     }
    catch (IOException e)
     {
     }

    return stream;
   }

  public File         getCacheDir       ()
   {
    return activity.getCacheDir();
   }

  public File         getMultiNetRootDir()
   {
    return new File(activity.getFilesDir(),MULTINET_ROOT_PATH);
   }

  public MNUserProfileView createUserProfileView ()
   {
    return new MNUserProfileView(activity);
   }

  public void runOnUiThread (Runnable action)
   {
    activity.runOnUiThread(action);
   }

  public void logWarning (String tag, String message)
   {
    Log.w(tag,message);
   }

  public void setBaseView(MNUserProfileView view)
   {
    baseView = view;
   }

  public MNUserProfileView getBaseView()
   {
    return baseView;
   }

  private Dictionary<String,String> loadMultiNetConfFile () throws MNException
   {
    Dictionary<String,String> dict = null;

    try
     {
      InputStream inputStream = activity.getResources()
                                 .getAssets().open(MULTINET_CONF_FILENAME);

      try
       {
        dict = loadMultiNetConfFile(inputStream);
       }
      finally
       {
        inputStream.close();
       }
     }
    catch (IOException e)
     {
      throw new MNException(MULTINET_CONF_FILE_ERROR,e);
     }

    return dict;
   }

  private Dictionary<String,String> loadMultiNetConfFile (InputStream source) throws MNException
   {
    Hashtable<String,String> dict = new Hashtable<String,String>();

    try
     {
      XmlPullParserFactory factory = XmlPullParserFactory.newInstance();

      factory.setNamespaceAware(true);

      XmlPullParser parser = factory.newPullParser();

      parser.setInput(source,"utf-8");

      while (parser.getEventType() != XmlPullParser.END_DOCUMENT &&
             parser.getEventType() != XmlPullParser.START_TAG)
       {
        parser.next();
       }

      if (parser.getEventType() == XmlPullParser.START_TAG &&
          parser.getName().equals("plist"))
       {
        parser.next();
       }
      else
       {
        throw new MNException(MULTINET_CONF_FILE_ERROR);
       }

      while (parser.getEventType() != XmlPullParser.END_DOCUMENT &&
             parser.getEventType() != XmlPullParser.START_TAG)
       {
        parser.next();
       }

      if (parser.getEventType() == XmlPullParser.START_TAG &&
          parser.getName().equals("dict"))
       {
        parser.next();
       }
      else
       {
        throw new MNException(MULTINET_CONF_FILE_ERROR);
       }

      boolean done = false;

      while (!done)
       {
        while (parser.getEventType() != XmlPullParser.END_DOCUMENT &&
               parser.getEventType() != XmlPullParser.START_TAG &&
               !done)
         {
          if (parser.getEventType() == XmlPullParser.END_TAG)
           {
            if (parser.getName().equals("dict"))
             {
              done = true;
             }
           }

          if (!done)
           {
            parser.next();
           }
         }

        if (!done)
         {
          if (parser.getEventType() != XmlPullParser.START_TAG ||
              !parser.getName().equals("key"))
           {
            throw new MNException(MULTINET_CONF_FILE_ERROR);
           }

          String key = parser.nextText();

          parser.next();

          while (parser.getEventType() != XmlPullParser.END_DOCUMENT &&
                 parser.getEventType() != XmlPullParser.START_TAG)
           {
            parser.next();
           }

          String value = null;

          if (parser.getEventType() == XmlPullParser.START_TAG)
           {
            if      (parser.getName().equals("string"))
             {
              value = parser.nextText();
             }
            else if (parser.getName().equals("true"))
             {
              value = "true";

              parser.next();
             }
            else if (parser.getName().equals("false"))
             {
              value = "false";

              parser.next();
             }
            else
             {
              throw new MNException(MULTINET_CONF_FILE_ERROR);
             }
           }
          else
           {
            throw new MNException(MULTINET_CONF_FILE_ERROR);
           }

          dict.put(key,value);
         }
       }
     }
    catch (XmlPullParserException e)
     {
      throw new MNException(MULTINET_CONF_FILE_ERROR,e);
     }
    catch (IOException e)
     {
      throw new MNException(MULTINET_CONF_FILE_ERROR,e);
     }

    return dict;
   }

  public Context getContext ()
   {
    return activity;
   }

  public Activity getActivity ()
   {
    return activity;
   }

  private Activity activity;
  private MNUserProfileView baseView;

  private static final String ANDROID_OS_NAME = "Android OS";

  private static final int DEVICE_TYPE_CODE = 2000;

  private static final String EMULATOR_UNIQUE_DEVICE_ID = "00000000";

  private static final String TIMEZONE_ABBR_NOT_AVAILABLE = "*";

  private static final String MULTINET_CONF_FILENAME   = "multinet.plist";
  private static final String MULTINET_CONF_FILE_ERROR = "MultiNet connection settings could not be read. Error in MultiNet.plist file.";

  private static final String MULTINET_CONFIG_URL_PARAM = "MultiNetConfigServerURL";
  private static final String MULTINET_ROOT_PATH = "multinet";
 }

