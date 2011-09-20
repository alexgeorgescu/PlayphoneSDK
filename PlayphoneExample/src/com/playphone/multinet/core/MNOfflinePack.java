//
//  MNOfflinePack.java
//  MultiNet client
//
//  Copyright 2010 PlayPhone. All rights reserved.
//

package com.playphone.multinet.core;

import java.util.Locale;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;

class MNOfflinePack implements MNURLTextDownloader.IEventHandler,
                               MNURLFileDownloader.IEventHandler
 {
  public interface IEventHandler
   {
    void onOfflinePackStartPageReady (String url);
    void onOfflinePackUnavailable    (String error);
   }

  public MNOfflinePack (IMNPlatform platform, int gameId, IEventHandler eventHandler)
   {
    this.platform     = platform;
    this.gameId       = gameId;
    this.rootDir      = platform.getMultiNetRootDir();
    this.eventHandler = eventHandler;

    startPageUrl = null;
    webServerUrl = null;

    retrievalState = RETRIEVAL_STATE_IDLE;

    startFromDownloadedPack = false;
    packUnavailable         = false;
    waitingForServerUrl     = false;

    textDownloader = null;
    fileDownloader = null;

    updateThread           = null;
    setupInitialPackThread = null;

    removeTempFiles();
   }

  public synchronized void shutdown ()
   {
    platform     = null;
    eventHandler = null;

    if (textDownloader != null)
     {
      textDownloader.cancel();

      textDownloader = null;
     }

    if (fileDownloader != null)
     {
      fileDownloader.cancel();

      fileDownloader = null;
     }

    if (updateThread != null)
     {
      updateThread.cancel();
     }

    if (setupInitialPackThread != null)
     {
      setupInitialPackThread.cancel();
     }

    removeTempFiles();
   }

  public synchronized String getStartPageUrl ()
   {
    if (startPageUrl == null)
     {
      if (updatePackExists())
       {
        updateOfflineDirectory();
       }
      else
       {
        if (offlineDirExists())
         {
          startPageUrl = makeOfflineUrl();

          checkUpdate();
         }
        else
         {
          setupInitialPack();
         }
       }
     }

    return startPageUrl;
   }

  public synchronized void setWebServerUrl (String url)
   {
    if (webServerUrl == null && url != null)
     {
      webServerUrl = url;

      if (waitingForServerUrl)
       {
        if (startFromDownloadedPack)
         {
          downloadPack();
         }
        else
         {
          requestRemotePackVersion();
         }
       }
     }
   }

  public synchronized boolean isPackUnavailable ()
   {
    return packUnavailable;
   }

  private String getLocalOfflinePackVersion ()
   {
    String version     = null;
    File   versionFile = getOfflineDirVersionFile();

    if (!versionFile.exists())
     {
      return null;
     }

    FileInputStream fileStream = null;
    BufferedReader  reader     = null;

    try
     {
      fileStream = new FileInputStream(versionFile);
      reader     = new BufferedReader(new InputStreamReader(fileStream));

      version = reader.readLine();
     }
    catch (IOException e)
     {
     }
    finally
     {
      if (reader != null)
       {
        try
         {
          reader.close();
         }
        catch (IOException e)
         {
         }
       }
      else
       {
        if (fileStream != null)
         {
          try
           {
            fileStream.close();
           }
          catch (IOException e)
           {
           }
         }
       }
     }

    return version;
   }

  private String createURLRequestWithPath (String path)
   {
    return String.format("%s/%s?game_id=%d&dev_type=%d&client_ver=%s&client_locale=%s",
                         webServerUrl,
                         path,
                         gameId,
                         platform.getDeviceType(),
                         MNSession.CLIENT_API_VERSION,
                         Locale.getDefault().toString());
   }

  private void requestRemotePackVersion ()
   {
    if (retrievalState == RETRIEVAL_STATE_IDLE)
     {
      retrievalState = RETRIEVAL_STATE_VERSION;

      textDownloader = new MNURLTextDownloader();

      textDownloader.loadURL
       (createURLRequestWithPath(REMOTE_PACK_VERSION_URL_PATH),this);
     }
   }

  private void downloadPack ()
   {
    if (retrievalState == RETRIEVAL_STATE_IDLE)
     {
      retrievalState = RETRIEVAL_STATE_DATA;

      if (!rootDir.exists())
       {
        mkdirsNoThrow(rootDir);
       }

      fileDownloader = new MNURLFileDownloader();

      fileDownloader.loadURL
       (getUpdatePackPartFile(),
        createURLRequestWithPath(REMOTE_PACK_DATA_URL_PATH),this);
     }
   }

  private void checkUpdate ()
   {
    if (webServerUrl != null)
     {
      requestRemotePackVersion();
     }
    else
     {
      waitingForServerUrl = true;
     }
   }

  private void onRemoteVersionLoadFailed (String errorMessage)
   {
    retrievalState = RETRIEVAL_STATE_IDLE;
    textDownloader = null;
   }

  private void onPackDownloadFailed (String errorMessage)
   {
    if (startFromDownloadedPack)
     {
      startFromDownloadedPack = false;
      packUnavailable         = true;

      if (eventHandler != null)
       {
        eventHandler.onOfflinePackUnavailable(errorMessage);
       }
     }

    deleteFileNoThrow(getUpdatePackPartFile());
   }

  public synchronized void downloaderDataReady (MNURLDownloader downloader, String[] data)
   {
    retrievalState = RETRIEVAL_STATE_IDLE;
    textDownloader = null;

    String remoteVersion;

    if (data.length > 0)
     {
      remoteVersion = data[0];

      if (remoteVersion == null)
       {
        remoteVersion = "";
       }
     }
    else
     {
      remoteVersion = "";
     }

    String localVersion = getLocalOfflinePackVersion();

    if (localVersion == null || !localVersion.equals(remoteVersion))
     {
      downloadPack();
     }
   }

  public synchronized void downloaderLoadFailed (MNURLDownloader downloader, MNURLDownloader.ErrorInfo errorInfo)
   {
    retrievalState = RETRIEVAL_STATE_IDLE;
    fileDownloader = null;

    if (downloader == textDownloader)
     {
      onRemoteVersionLoadFailed(errorInfo.getMessage());
     }
    else if (downloader == fileDownloader)
     {
      onPackDownloadFailed(errorInfo.getMessage());
     }
   }

  public synchronized void downloaderLoadSucceeded (MNURLDownloader downloader)
   {
    retrievalState = RETRIEVAL_STATE_IDLE;
    fileDownloader = null;

    if (renameToNoThrow(getUpdatePackPartFile(),getUpdatePackFile()))
     {
      if (startFromDownloadedPack)
       {
        updateOfflineDirectory();
       }
     }
    else
     {
      onRemoteVersionLoadFailed("offline data unpacking failed");
     }
   }

  synchronized void onOfflineDirectoryUpdateSucceeded ()
   {
    updateThread = null;

    startPageUrl = makeOfflineUrl();

    if (eventHandler != null)
     {
      eventHandler.onOfflinePackStartPageReady(startPageUrl);
     }

    if (!startFromDownloadedPack)
     {
      checkUpdate();
     }
   }

  synchronized void onOfflineDirectoryUpdateFailed (String errorMessage)
   {
    File offlineDirFile = getOfflineDirFile();

    updateThread = null;

    if (offlineDirFile.exists())
     {
      startPageUrl = makeOfflineUrl();

      if (eventHandler != null)
       {
        eventHandler.onOfflinePackStartPageReady(startPageUrl);
       }
     }
    else
     {
      if (startFromDownloadedPack)
       {
        packUnavailable = true;

        if (eventHandler != null)
         {
          eventHandler.onOfflinePackUnavailable("offline data unpacking failed");
         }
       }
      else
       {
        setupInitialPack();
       }
     }
   }

  synchronized void onInitialPackSetupSucceeded ()
   {
    setupInitialPackThread = null;

    checkUpdate();

    startPageUrl = makeOfflineUrl();

    if (eventHandler != null)
     {
      eventHandler.onOfflinePackStartPageReady(startPageUrl);
     }
   }

  synchronized void onInitialPackSetupFailed ()
   {
    setupInitialPackThread = null;

    startFromDownloadedPack = true;

    if (webServerUrl != null)
     {
      downloadPack();
     }
    else
     {
      waitingForServerUrl = true;
     }
   }

  private void removeTempFiles ()
   {
    removeDirRecursive(getOfflineBackupDirFile());

    removeFile(getUpdatePackPartFile());
    removeFile(getUpdatePackTempFile());
   }

  File getUpdatePackFile ()
   {
    return new File(rootDir,UPDATE_PACK_NAME);
   }

  File getUpdatePackPartFile ()
   {
    return new File(rootDir,UPDATE_PACK_PART_NAME);
   }

  File getUpdatePackTempFile ()
   {
    return new File(rootDir,UPDATE_PACK_TEMP_NAME);
   }

  File getOfflineDirFile ()
   {
    return new File(rootDir,OFFLINE_DIR_NAME);
   }

  private File getOfflineDirVersionFile ()
   {
    return new File(getOfflineDirFile(),OFFLINE_DIR_VERSION_FILE_NAME);
   }

  File getOfflineBackupDirFile ()
   {
    return new File(rootDir,OFFLINE_BACKUP_DIR_NAME);
   }

  File getOfflineNewDirFile ()
   {
    return new File(rootDir,OFFLINE_NEW_DIR_NAME);
   }

  private boolean updatePackExists ()
   {
    return getUpdatePackFile().exists();
   }

  private boolean offlineDirExists ()
   {
    return getOfflineDirFile().exists();
   }

  private String makeOfflineUrl ()
   {
    return "file://" + getOfflineDirFile().getAbsolutePath();
   }

  private void updateOfflineDirectory ()
   {
    if (updateThread != null)
     {
      //log this

      return;
     }

    updateThread = new UpdateThread();

    updateThread.setPriority(Thread.MIN_PRIORITY);
    updateThread.start();
   }

  private void setupInitialPack ()
   {
    if (setupInitialPackThread != null)
     {
      //log this

      return;
     }

    setupInitialPackThread = new SetupInitialPackThread();

    setupInitialPackThread.setPriority(Thread.MIN_PRIORITY);
    setupInitialPackThread.start();
   }

  static boolean removeDirRecursive (File dir, int level)
   {
    level++;

    if (level > MAX_RECURSION_DEPTH)
     {
      return false;
     }

    if (!dir.exists())
     {
      return false;
     }

    if (dir.isDirectory())
     {
      File[] fileList = dir.listFiles();
      int    count    = fileList.length;

      for (int index = 0; index < count; index++)
       {
        File file = fileList[index];

        if (file.isDirectory())
         {
          if (!removeDirRecursive(file,level))
           {
            return false;
           }
         }
        else
         {
          if (!deleteFileNoThrow(file))
           {
            return false;
           }
         }
       }
     }

    return deleteFileNoThrow(dir);
   }

  static boolean removeDirRecursive (File dir)
   {
    return removeDirRecursive(dir,0);
   }

  static boolean removeFile (File file)
   {
    if (file.exists())
     {
      return file.delete();
     }
    else
     {
      return false;
     }
   }

  static boolean deleteFileNoThrow (File file)
   {
    boolean result = false;

    try
     {
      result = file.delete();
     }
    catch (SecurityException e)
     {
     }

    return result;
   }

  static boolean mkdirsNoThrow (File dir)
   {
    boolean result = false;

    try
     {
      result = dir.mkdirs();
     }
    catch (SecurityException e)
     {
     }

    return result;
   }

  static boolean renameToNoThrow (File from, File to)
   {
    boolean result = false;

    try
     {
      result = from.renameTo(to);
     }
    catch (SecurityException e)
     {
     }

    return result;
   }

  private class ThreadCancelable extends Thread
   {
    public ThreadCancelable ()
     {
      canceled = false;
     }

    public synchronized void cancel ()
     {
      canceled = true;
     }

    public synchronized boolean isCanceled ()
     {
      return canceled;
     }

    private boolean canceled;
   }

  private class UpdateThread extends ThreadCancelable implements MNZipTool.OperationStopper
   {
    public void run ()
     {
      doUpdate();
     }

    public boolean shouldStop ()
     {
      return isCanceled();
     }

    protected void doUpdate ()
     {
      File offlineDirFile       = getOfflineDirFile();
      File offlineNewDirFile    = getOfflineNewDirFile();
      File offlineBackupDirFile = getOfflineBackupDirFile();
      File updatePackFile       = getUpdatePackFile();
      File updatePackTempFile   = getUpdatePackTempFile();

      if (offlineNewDirFile.exists())
       {
        if (!removeDirRecursive(offlineNewDirFile))
         {
          callErrorHandler("temporary unpacked directory removal failed");

          return;
         }
       }

      if (isCanceled())
       {
        return;
       }

      if (!mkdirsNoThrow(offlineNewDirFile))
       {
        callErrorHandler("temporary unpack directory creation failed");

        return;
       }

      if (isCanceled())
       {
        return;
       }

      deleteFileNoThrow(updatePackTempFile);

      if (!renameToNoThrow(updatePackFile,updatePackTempFile))
       {
        callErrorHandler("can not rename update pack to temporary name");

        return;
       }

      if (!MNZipTool.unzipFile(offlineNewDirFile,updatePackTempFile,this))
       {
        removeDirRecursive(offlineNewDirFile);

        deleteFileNoThrow(updatePackTempFile);

        callErrorHandler("update pack can not be unpacked");

        return;
       }

      if (isCanceled())
       {
        return;
       }

      if (offlineDirFile.exists())
       {
        if (offlineBackupDirFile.exists())
         {
          if (!removeDirRecursive(offlineBackupDirFile))
           {
            removeDirRecursive(offlineNewDirFile);

            callErrorHandler("stalled backup directory removal failed");

            return;
           }
         }

        if (!renameToNoThrow(offlineDirFile,offlineBackupDirFile))
         {
          removeDirRecursive(offlineNewDirFile);

          callErrorHandler("can not create backup copy of offline directory");

          return;
         }
       }

      if (isCanceled())
       {
        return;
       }

      if (!renameToNoThrow(offlineNewDirFile,offlineDirFile))
       {
        if (offlineBackupDirFile.exists())
         {
          renameToNoThrow(offlineBackupDirFile,offlineDirFile);
         }

        removeDirRecursive(offlineNewDirFile);

        callErrorHandler("can not setup updated directory");

        return;
       }

      if (isCanceled())
       {
        return;
       }

      removeDirRecursive(offlineBackupDirFile);
      deleteFileNoThrow(updatePackTempFile);

      callOkHandler();
     }

    protected void callErrorHandler (String message)
     {
      if (!isCanceled())
       {
        onOfflineDirectoryUpdateFailed(message);
       }
     }

    protected void callOkHandler ()
     {
      if (!isCanceled())
       {
        onOfflineDirectoryUpdateSucceeded();
       }
     }
   }

  private class SetupInitialPackThread extends UpdateThread
   {
    public void run ()
     {
      if (doSetup())
       {
        if (!isCanceled())
         {
          doUpdate();
         }
       }
      else
       {
        callErrorHandler("initial pack setup failed");
       }
     }

    // copy from file from assets
    // rename from .zip.part to .zip
    protected boolean doSetup ()
     {
      boolean ok               = true;
      IMNPlatform tempPlatform = platform;

      if (tempPlatform == null)
       {
        return false;
       }

      InputStream assetStream  = tempPlatform.openAssetForInput(UPDATE_PACK_NAME);
      File updatePackPartFile  = getUpdatePackPartFile();

      if (assetStream != null)
       {
        OutputStream updatePackPartStream = null;

        try
         {
          rootDir = tempPlatform.getMultiNetRootDir();

          if (!rootDir.exists())
           {
            mkdirsNoThrow(rootDir);
           }

          updatePackPartStream = new FileOutputStream(updatePackPartFile);

          ok = MNUtils.copyStream(assetStream,updatePackPartStream);
         }
        catch (IOException e)
         {
          ok = false;

          deleteFileNoThrow(updatePackPartFile);
         }
        finally
         {
          if (updatePackPartStream != null)
           {
            try
             {
              updatePackPartStream.close();
             }
            catch (IOException e)
             {
             }
           }

          try
           {
            assetStream.close();
           }
          catch (IOException e)
           {
           }
         }
       }
      else
       {
        ok = false;
       }

      if (ok)
       {
        File updatePackFile = getUpdatePackFile();

        if (!renameToNoThrow(updatePackPartFile,updatePackFile))
         {
          ok = false;

          deleteFileNoThrow(updatePackPartFile);
         }
       }

      return ok;
     }

    protected void callErrorHandler (String message)
     {
      if (!isCanceled())
       {
        onInitialPackSetupFailed();
       }
     }

    protected void callOkHandler ()
     {
      if (!isCanceled())
       {
        onInitialPackSetupSucceeded();
       }
     }
   }

  private IMNPlatform   platform;
  private File          rootDir;
  private int           gameId;
  private IEventHandler eventHandler;

  private MNURLTextDownloader     textDownloader;
  private MNURLFileDownloader     fileDownloader;
  private UpdateThread            updateThread;
  private SetupInitialPackThread  setupInitialPackThread;

  private String        startPageUrl;
  private String        webServerUrl;
  private int           retrievalState;
  private boolean       startFromDownloadedPack;
  private boolean       packUnavailable;
  private boolean       waitingForServerUrl;

  private static final int RETRIEVAL_STATE_IDLE    = 0;
  private static final int RETRIEVAL_STATE_VERSION = 1;
  private static final int RETRIEVAL_STATE_DATA    = 2;

  private static final String UPDATE_PACK_NAME = "data_game_web_front.zip";
  private static final String UPDATE_PACK_TEMP_NAME = "data_game_web_front.zip.tmp";
  private static final String UPDATE_PACK_PART_NAME = "data_game_web_front.zip.part";
  private static final String OFFLINE_DIR_NAME = "web";
  private static final String OFFLINE_BACKUP_DIR_NAME = "web.bak";
  private static final String OFFLINE_NEW_DIR_NAME = "web.new";
  private static final String OFFLINE_DIR_VERSION_FILE_NAME = "data_game_web_front_version.txt";

  private static final String REMOTE_PACK_VERSION_URL_PATH = "data_game_web_front_version_txt.php";
  private static final String REMOTE_PACK_DATA_URL_PATH    = "data_game_web_front_zip.php";

  private static final int MAX_RECURSION_DEPTH = 256;
 }

