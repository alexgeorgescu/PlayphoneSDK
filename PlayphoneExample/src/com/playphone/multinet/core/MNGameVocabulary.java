//
//  MNGameVocabulary.java
//  MultiNet client
//
//  Copyright 2011 PlayPhone. All rights reserved.
//

package com.playphone.multinet.core;

import java.util.Locale;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import com.playphone.multinet.MNErrorInfo;

public class MNGameVocabulary
 {
  public static final int MN_GV_DOWNLOAD_SUCCESS = 0;
  public static final int MN_GV_DOWNLOAD_FAIL    = -1;

  public static final int MN_GV_UPDATE_STATUS_DOWNLOAD_IN_PROGRESS = -200;
  public static final int MN_GV_UPDATE_STATUS_CHECK_IN_PROGRESS    = -100;
  public static final int MN_GV_UPDATE_STATUS_UNKNOWN              = -1;
  public static final int MN_GV_UPDATE_STATUS_UP_TO_DATE           = 0;
  public static final int MN_GV_UPDATE_STATUS_NEED_DOWNLOAD        = 1;

  public interface IEventHandler
   {
    void mnGameVocabularyStatusUpdated    (int updateStatus);
    void mnGameVocabularyDownloadStarted  ();
    void mnGameVocabularyDownloadFinished (int downloadStatus);
   }

  public static class EventHandlerAbstract implements IEventHandler
   {
    public void mnGameVocabularyStatusUpdated    (int updateStatus)
     {
     }

    public void mnGameVocabularyDownloadStarted  ()
     {
     }

    public void mnGameVocabularyDownloadFinished (int downloadStatus)
     {
     }
   }

  public MNGameVocabulary (MNSession session)
   {
    this.session     = session;
    eventHandlers    = new MNEventHandlerArray<IEventHandler>();
    vocabularyStatus = MN_GV_UPDATE_STATUS_UNKNOWN;

    sessionEventHandler = new SessionEventHandler();

    session.addEventHandler(sessionEventHandler);

    downloaderEventHandler = new DownloaderEventHandler();
    versionDownloader      = null;
    dataDownloader         = null;
    webServerURL           = null;
   }

  public synchronized int getVocabularyStatus ()
   {
    return vocabularyStatus;
   }

  public synchronized boolean startDownload ()
   {
    if (vocabularyStatus != MN_GV_UPDATE_STATUS_NEED_DOWNLOAD)
     {
      return false;
     }

    setVocabularyStatus(MN_GV_UPDATE_STATUS_DOWNLOAD_IN_PROGRESS);

    eventHandlers.callHandlers(new MNEventHandlerArray.ICaller<IEventHandler>()
     {
      public void callHandler (IEventHandler handler)
       {
        handler.mnGameVocabularyDownloadStarted();
       }
     });

    String webServerUrl = getWebServerURL();

    if (webServerUrl != null)
     {
      String dataUrl = webServerUrl + "/" + VOCABULARY_DATA_URL_PATH;

      dataDownloader = new MNURLFileDownloader();

      File tempFileDir = getTempDataDir();

      if (tempFileDir.exists() || tempFileDir.mkdirs())
       {
        dataDownloader.loadURL(getTempDataFile(),dataUrl,buildPostBodyString(),downloaderEventHandler);

        return true;
       }
     }

    sendDownloadFinishedEvent(MN_GV_DOWNLOAD_FAIL);

    setVocabularyStatus(MN_GV_UPDATE_STATUS_NEED_DOWNLOAD);

    return false;
   }

  public synchronized void checkForUpdate ()
   {
    if (vocabularyStatus == MN_GV_UPDATE_STATUS_DOWNLOAD_IN_PROGRESS ||
        vocabularyStatus == MN_GV_UPDATE_STATUS_CHECK_IN_PROGRESS ||
        vocabularyStatus == MN_GV_UPDATE_STATUS_NEED_DOWNLOAD)
     {
      return;
     }

    setVocabularyStatus(MN_GV_UPDATE_STATUS_CHECK_IN_PROGRESS);

    if (versionDownloader != null)
     {
      versionDownloader.cancel();
     }

    String webServerUrl = getWebServerURL();

    if (webServerUrl != null)
     {
      String versionUrl = webServerUrl + "/" + VOCABULARY_VERSION_URL_PATH;

      versionDownloader = new MNURLStringDownloader();

      versionDownloader.loadURL(versionUrl,buildPostBodyString(),downloaderEventHandler);
     }
    else
     {
      setVocabularyStatus(MN_GV_UPDATE_STATUS_UNKNOWN);
     }
   }

  private String buildPostBodyString ()
   {
    MNUtils.HttpPostBodyStringBuilder postBodyBuilder = new MNUtils.HttpPostBodyStringBuilder();

    postBodyBuilder.addParam("game_id",Integer.toString(session.getGameId()));
    postBodyBuilder.addParam("dev_type",Integer.toString(session.getPlatform().getDeviceType()));
    postBodyBuilder.addParam("client_ver",MNSession.CLIENT_API_VERSION);
    postBodyBuilder.addParam("client_locale",Locale.getDefault().toString());

    return postBodyBuilder.toString();
   }

  public static boolean isUpdateStatusFinal (int status)
   {
    return status > -100;
   }

  public byte[] getFileData (String fileName)
   {
    synchronized (dataFileLock)
     {
      if (cachedFileExists())
       {
        return getCachedFileData(fileName);
       }
      else
       {
        return getAssetsFileData(fileName);
       }
     }
   }

  public void addEventHandler (IEventHandler eventHandler)
   {
    eventHandlers.add(eventHandler);
   }

  public void removeEventHandler (IEventHandler eventHandler)
   {
    eventHandlers.remove(eventHandler);
   }

  private byte[] getCachedFileData (String fileName)
   {
    return MNZipTool.getFileDataFromArchive(getCachedFile(),fileName);
   }

  private byte[] getAssetsFileData (String fileName)
   {
    InputStream assetInputStream = session.getPlatform().openAssetForInput(GV_FILE_NAME);

    if (assetInputStream == null)
     {
      return null;
     }

    byte[] data = MNZipTool.getFileDataFromStream(assetInputStream,fileName);

    try
     {
      assetInputStream.close();
     }
    catch (IOException e)
     {
     }

    return data;
   }

  private File getCachedFile ()
   {
    return new File(getCachedDir(),GV_FILE_NAME);
   }

  private File getTempDataFile ()
   {
    return new File(getTempDataDir(),GV_FILE_NAME + ".tmp");
   }

  private File getCachedDir ()
   {
    return session.getPlatform().getMultiNetRootDir();
   }

  private File getTempDataDir ()
   {
    return getCachedDir();
   }

  private boolean cachedFileExists ()
   {
    return getCachedFile().exists();
   }

  private synchronized void onRemoteVersionArrived (String remoteVersion)
   {
    if (remoteVersion == null || remoteVersion.equals(""))
     {
      setVocabularyStatus(MN_GV_UPDATE_STATUS_UNKNOWN);
     }
    else
     {
      String localVersion = readLocalVersion();

      if (remoteVersion.equals(localVersion))
       {
        setVocabularyStatus(MN_GV_UPDATE_STATUS_UP_TO_DATE);
       }
      else
       {
        setVocabularyStatus(MN_GV_UPDATE_STATUS_NEED_DOWNLOAD);
       }
     }
   }

  private void sendDownloadFinishedEvent (final int downloadStatus)
   {
    eventHandlers.callHandlers(new MNEventHandlerArray.ICaller<IEventHandler>()
     {
      public void callHandler (IEventHandler handler)
       {
        handler.mnGameVocabularyDownloadFinished(downloadStatus);
       }
     });
   }

  class SessionEventHandler extends MNSessionEventHandlerAbstract
   {
    public void mnSessionConfigLoaded ()
     {
      String newWebServerURL = session.getWebServerURL();

      if (newWebServerURL != null)
       {
        webServerURL = newWebServerURL;
       }

      onRemoteVersionArrived(session.getConfigData().gameVocabularyVersion);
     }

    public void mnSessionConfigLoadStarted ()
     {
      setVocabularyStatus(MN_GV_UPDATE_STATUS_CHECK_IN_PROGRESS);
     }

    public void mnSessionErrorOccurred (MNErrorInfo errorInfo)
     {
      if (errorInfo.actionCode == MNErrorInfo.ACTION_CODE_LOAD_CONFIG)
       {
        setVocabularyStatus(MN_GV_UPDATE_STATUS_UNKNOWN);
       }
     }
   }

  class DownloaderEventHandler implements MNURLStringDownloader.IEventHandler,MNURLFileDownloader.IEventHandler
   {
    public void downloaderDataReady (MNURLDownloader downloader, String str)
     {
      onRemoteVersionArrived(str);
     }

    public void downloaderLoadSucceeded (MNURLDownloader downloader)
     {
      boolean ok = true;

      synchronized (dataFileLock)
       {
        File cachedFileDir = getCachedDir();

        if (!cachedFileDir.exists())
         {
          ok = cachedFileDir.mkdirs();
         }

        if (ok)
         {
          if (!getTempDataFile().renameTo(getCachedFile()))
           {
            getTempDataFile().delete();

            ok = false;
           }
         }
       }

      sendDownloadFinishedEvent(ok ? MN_GV_DOWNLOAD_SUCCESS : MN_GV_DOWNLOAD_FAIL);
      setVocabularyStatus(ok ? MN_GV_UPDATE_STATUS_UP_TO_DATE : MN_GV_UPDATE_STATUS_NEED_DOWNLOAD);
     }

    public void downloaderLoadFailed (MNURLDownloader downloader, MNURLDownloader.ErrorInfo errorInfo)
     {
      if (downloader == versionDownloader)
       {
        // if data download is in progress, new state will be set after
        // download finishes
        if (vocabularyStatus != MN_GV_UPDATE_STATUS_DOWNLOAD_IN_PROGRESS)
         {
          setVocabularyStatus(MN_GV_UPDATE_STATUS_UNKNOWN);
         }
       }
      else if (downloader == dataDownloader)
       {
        sendDownloadFinishedEvent(MN_GV_DOWNLOAD_FAIL);

        setVocabularyStatus(MN_GV_UPDATE_STATUS_NEED_DOWNLOAD);
       }
     }
   }

  private synchronized void setVocabularyStatus (final int newStatus)
   {
    vocabularyStatus = newStatus;

    eventHandlers.callHandlers(new MNEventHandlerArray.ICaller<IEventHandler>()
     {
      public void callHandler (IEventHandler handler)
       {
        handler.mnGameVocabularyStatusUpdated(newStatus);
       }
     });
   }

  private String readLocalVersion ()
   {
    byte[] data = getFileData(LOCAL_VERSION_FILE_NAME);

    if (data == null)
     {
      return "";
     }

    String localVersion = null;

    try
     {
      localVersion = new String(data,"UTF-8");
     }
    catch (UnsupportedEncodingException e)
     {
      localVersion = "";
     }

    return localVersion;
   }

  private String getWebServerURL ()
   {
    if (webServerURL == null)
     {
      webServerURL = session.getWebServerURL();
     }

    return webServerURL;
   }

  private final MNSession session;
  private final MNEventHandlerArray<IEventHandler> eventHandlers;
  private int vocabularyStatus;
  private final SessionEventHandler sessionEventHandler;
  private final DownloaderEventHandler downloaderEventHandler;
  private MNURLStringDownloader versionDownloader;
  private MNURLFileDownloader dataDownloader;
  private final Object dataFileLock = new Object();
  private String webServerURL;
  private static final String GV_FILE_NAME = "data_game_vocabulary.zip";
  private static final String LOCAL_VERSION_FILE_NAME = "data_game_vocabulary_version.txt";
  private static final String VOCABULARY_VERSION_URL_PATH = "data_game_vocabulary_version_txt.php";
  private static final String VOCABULARY_DATA_URL_PATH = "data_game_vocabulary_zip.php";
 }

