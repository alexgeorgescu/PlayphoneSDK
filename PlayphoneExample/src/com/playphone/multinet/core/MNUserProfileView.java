//
//  MNUserProfileView.java
//  MultiNet client
//
//  Copyright 2009 PlayPhone. All rights reserved.
//

package com.playphone.multinet.core;

import java.net.URL;
import java.net.MalformedURLException;
import java.util.Locale;
import java.util.Collections;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.io.InputStream;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.content.DialogInterface;
import android.view.View;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.webkit.JsResult;
import android.util.AttributeSet;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.Canvas;
import android.net.Uri;
import android.util.Log;

import com.playphone.multinet.MNConst;
import com.playphone.multinet.MNGameParams;
import com.playphone.multinet.MNUserInfo;
import com.playphone.multinet.MNErrorInfo;

/**
 * A View that allows user to interact with MultiNet server.
 *
 * @see IMNUserProfileViewEventHandler
 */
public class MNUserProfileView extends FrameLayout
                               implements IMNSessionEventHandler,
                                          MNSession.SocNetFBEventHandler,
                                          MNUIWebViewHttpReqQueue.IEventHandler
 {
  /**
   * Constructs a new <code>MNUserProfileView</code> with a <code>Context</code> object.
   *
   * @params context A context object
   */
  public MNUserProfileView (Context context)
   {
    super(context);

    initialize(context);
   }

  /**
   * Constructs a new <code>MNUserProfileView</code> with layout parameters.
   *
   * @params context A context object
   * @params attrs An attribute set
   */
  public MNUserProfileView (Context context, AttributeSet attrs)
   {
    super(context,attrs);

    initialize(context);
   }

  private void webViewLoadStartUrl ()
   {
    runOnUiThread(new Runnable()
     {
      public void run ()
       {
        webServerUrl = session.getWebFrontURL();

        if (webServerUrl != null)
         {
          URL multiNetWebServerURL = null;

          try
           {
            multiNetWebServerURL = new URL(webServerUrl);
           }
          catch (Exception e)
           {
            Log.d(TAG,"can not create web-server URL",e);
           }

          if (multiNetWebServerURL != null)
           {
            String baseHost = multiNetWebServerURL.getHost();

            if (baseHost != null)
             {
              trustedHosts.add(baseHost);
             }

            String startUrl = String.format((webServerUrl.startsWith("file:") ? "%s/welcome.php.html?" : "%s/welcome.php?") +
                                            "game_id=%d&" +
                                            "dev_id=%s&" +
                                            "dev_type=%d&" +
                                            "client_ver=%s&" +
                                            "client_locale=%s",
                                            webServerUrl,
                                            session.getGameId(),
                                            MNUtils.stringGetMD5String(session.getPlatform().getUniqueDeviceIdentifier()),
                                            session.getPlatform().getDeviceType(),
                                            MNSession.CLIENT_API_VERSION,
                                            Locale.getDefault().toString());

            errorPageLoaded = false;

            webView.loadUrl(startUrl);
           }
         }
       }
     });
   }

/**
 * Binds view to MultiNet session and loads MultiNet start page.
 *
 * @param session MNSession object to bind current view to
 */
  public void bindToSession (MNSession session) throws MNException
   {
    if (this.session != null)
     {
      this.session.removeEventHandler(this);
     }

    trustedHosts = Collections.synchronizedSet(new HashSet<String>());

    this.session = session;
    this.session.addEventHandler(this);

    webViewLoadStartUrl();
   }

/**
 * Destroys view's binding to MultiNet session.
 */
  public void unbindFromSession ()
   {
    if (session != null)
     {
      session.removeEventHandler(this);
     }

    session = null;
   }

  /**
   * Performs cleanup action.
   *
   * Call this method if view is not needed anymore.
   */

  /* not destroying web view's causes problems  */
  /* during application restart (probably a bug */
  /* in Android's WebViewCore), so it is */
  public void destroy ()
   {
    httpReqQueue.shutdown();
    eventHandlers.clearAll();

    if (session != null)
     {
      session.removeEventHandler(this);

      runOnUiThread(new Runnable()
       {
        public void run ()
         {
          webView.destroySafe();
          navBarView.destroySafe();
         }
       });

      session = null;
     }
    else
     {
      /* session is not available, so runOnUiThread() can not be used.        */
      /* we try our best to call webview's destroy (we can not guarantee that */
      /* it will be called because Android can ignore "post" call if view is  */
      /* not visible)                                                         */

      webView.postSafe(new Runnable()
       {
        public void run ()
         {
          webView.destroySafe();
          navBarView.destroySafe();
         }
       });
     }

    hostActivity = null;
   }

/**
 * Sets event handler.
 *
 * @param eventHandler an object that implements
 * {@link IMNUserProfileViewEventHandler IMNUserProfileViewEventHandler}
 * interface
 *
 * @deprecated use <code>addEventHandler</code> and
 * <code>removeEventHandler</code> instead
 */
  public synchronized void setEventHandler (IMNUserProfileViewEventHandler eventHandler)
   {
    eventHandlers.set(eventHandler);
   }

  /**
   * Adds event handler
   *
   * @param eventHandler an object that implements
   * {@link IMNUserProfileViewEventHandler IMNUserProfileViewEventHandler}
   * interface
   */
  public synchronized void addEventHandler (IMNUserProfileViewEventHandler eventHandler)
   {
    eventHandlers.add(eventHandler);
   }

  /**
   * Removes event handler
   *
   * @param eventHandler an object that implements
   * {@link IMNUserProfileViewEventHandler IMNUserProfileViewEventHandler}
   * interface
   */
  public synchronized void removeEventHandler (IMNUserProfileViewEventHandler eventHandler)
   {
    eventHandlers.remove(eventHandler);
   }

  /**
   * Controls whether MultiNet sessions's {@link MNSession.cancelGameWithParams cancelGameWithParams} method will be called if user clicks on "Go back" button.
   *
   * @param enable boolean flag indicating if
   * {@link MNSession.cancelGameWithParams cancelGameWithParams} method should
   * be called (<code>true</code>), or not (<code>false</code>).
   */
  public void setAutoCancelGameOnGoBack (boolean enable)
   {
    autoCancelGameOnGoBack = enable;
   }

  /**
   * Returns "auto-cancel" mode state
   *
   * @return <code>true</code> if {@link MNSession.cancelGameWithParams cancelGameWithParams}
   * method will be called if user clicks on "Go back" button and <code>false</code>
   * otherwise.
   */
  public boolean getAutoCancelGameOnGoBack ()
   {
    return autoCancelGameOnGoBack;
   }

  public void setContextCallWaitLoad (boolean enable)
   {
    contextCallWaitLoad = enable;
   }

  public boolean getContextCallWaitLoad ()
   {
    return contextCallWaitLoad;
   }

  private void changeContext (Context context)
   {
    if (context == null)
     {
      context = ((MNPlatformAndroid)session.getPlatform()).getContext();

      if (context == null)
       {
        return;
       }
     }

    try
     {
      Class<?> clazz = webView.getClass().getSuperclass().getSuperclass().getSuperclass().getSuperclass();
      Field property = clazz.getDeclaredField("mContext");
      property.setAccessible(true);
      property.set(webView,context);
     }
    catch (Exception e)
     {
      Log.e(this.getClass().getName(),"Can't change activity context");
     }
   }

  public void setHostActivity (Activity newHostActivity)
   {
    hostActivity = newHostActivity;

    changeContext(hostActivity);
   }

  public Activity getHostActivity ()
   {
    if (hostActivity != null)
     {
      return hostActivity;
     }
    else
     {
      return ((MNPlatformAndroid)session.getPlatform()).getActivity();
     }
   }

  private boolean isWebViewLocationAtLocalFile (MNSafeWebView webView)
   {
    String urlString = webView.getUrl();
    URL url = null;

    try
     {
      url = new URL(urlString);
     }
    catch (MalformedURLException e)
     {
      return false;
     }

    String protocol = url.getProtocol();

    return protocol.equals(URL_PROTOCOL_FILE);
   }

  private boolean isHostTrusted (String hostName)
   {
    return trustedHosts.contains(hostName);
   }

  private boolean isWebViewLocationAtTrustedHost (MNSafeWebView webView)
   {
    String urlString = webView.getUrl();
    URL url = null;

    try
     {
      url = new URL(urlString);
     }
    catch (MalformedURLException e)
     {
      return false;
     }

    String protocol = url.getProtocol();

    if (!protocol.equals(URL_PROTOCOL_HTTP) &&
        !protocol.equals(URL_PROTOCOL_HTTPS))
     {
      return false;
     }

    return isHostTrusted(url.getHost());
   }

  private boolean isWebViewLocationTrusted (MNSafeWebView webView)
   {
    return isWebViewLocationAtLocalFile(webView) ||
           isWebViewLocationAtTrustedHost(webView);
   }

  private void runOnUiThread (Runnable action)
   {
    ((MNPlatformAndroid)session.getPlatform())
     .getActivity().runOnUiThread(action);
   }

  private void scheduleLoadUrl (final MNSafeWebView webView,
                                final String url)
   {
    runOnUiThread(new Runnable()
     {
      //@Override
      public void run ()
       {
        webView.loadUrl(url);
       }
     });
   }

  private static String stringAddJSScheme (String jsCode)
   {
    return "javascript:" + jsCode;
   }

  private void callJSScriptNoScheme (String javaScriptCode)
   {
    callJSScript(stringAddJSScheme(javaScriptCode));
   }

  private void callJSScript (String javaScriptCode)
   {
    callJSScript(javaScriptCode,false);
   }

  private void callJSScript (final String javaScriptCode, final boolean forceFlag)
   {
    callJSScript(javaScriptCode,forceFlag,true,true);
   }

  private void callJSScript (final String javaScriptCode, final boolean forceFlag, final boolean callWebView, final boolean callNavBar)
   {
    runOnUiThread(new Runnable()
     {
      //@Override
      public void run ()
       {
        if (callWebView && (forceFlag || isWebViewLocationTrusted(webView)))
         {
          webView.loadUrl(javaScriptCode);
         }

        if (callNavBar && (forceFlag || isWebViewLocationTrusted(navBarView)))
         {
          navBarView.loadUrl(javaScriptCode);
         }
       }
     });
   }

  public void mnUiWebViewHttpReqSucceeded (String jsCode, int flags)
   {
    callJSScript(stringAddJSScheme(jsCode),
                 false,
                 (flags & HTTPREQ_FLAG_EVAL_IN_MAINWEBVIEW_MASK) != 0,
                 (flags & HTTPREQ_FLAG_EVAL_IN_NAVBARWEBVIEW_MASK) != 0);
   }

  public void mnUiWebViewHttpReqFailed    (String jsCode, int flags)
   {
    callJSScript(stringAddJSScheme(jsCode),
                 false,
                 (flags & HTTPREQ_FLAG_EVAL_IN_MAINWEBVIEW_MASK) != 0,
                 (flags & HTTPREQ_FLAG_EVAL_IN_NAVBARWEBVIEW_MASK) != 0);
   }

  /* IMNUserProfileViewEventHandler methods */

  public void mnSessionStatusChanged (int newStatus, int oldStatus)
   {
    if (oldStatus == MNConst.MN_OFFLINE && errorPageLoaded)
     {
      webViewLoadStartUrl();
     }
    else
     {
      scheduleUpdateContext();
     }
   }

  public void mnSessionUserChanged (long userId)
   {
    scheduleUpdateContext();
   }

  public void mnSessionRoomUserStatusChanged (int userStatus)
   {
    String newStatus;

    if (userStatus == MNConst.MN_USER_STATUS_UNDEFINED)
     {
      newStatus = null;
     }
    else
     {
      newStatus = Integer.toString(userStatus);
     }

    callJSScript(String.format("javascript:MN_UpdateRoomUserStatus(%s)",
                               MNUtils.stringAsJSString(newStatus)));
   }

  public void mnSessionLoginInitiated ()
   {
   }

  public void mnSessionDefaultGameSetIdChangedTo (int gameSetId)
   {
    callJSScript("javascript:MN_UpdateDefaultGameSetId(" +
                 Integer.toString(gameSetId) +
                 ")");
   }

  private void scheduleChatMessageNotification (MNChatMessage chatMessage)
   {
    String userInfo;

    userInfo = String.format("new MN_SF_UserInfo(%d,%s,'%d')",
                             chatMessage.sender.userSFId,
                             MNUtils.stringAsJSString(chatMessage.sender.userName),
                             chatMessage.sender.userId);

    String javaScriptSrc;

    javaScriptSrc = String.format("javascript:%s(%d,%s,%s);",
                                  chatMessage.isPrivate ?
                                   "MN_InChatPrivateMessage" :
                                   "MN_InChatPublicMessage",
                                  chatMessage.sender.userSFId,
                                  userInfo,
                                  MNUtils.stringAsJSString(chatMessage.message));


    callJSScript(javaScriptSrc);
   }

  public void mnSessionChatPublicMessageReceived (MNChatMessage chatMessage)
   {
    scheduleChatMessageNotification(chatMessage);
   }

  public void mnSessionChatPrivateMessageReceived (MNChatMessage chatMessage)
   {
    scheduleChatMessageNotification(chatMessage);
   }

  public void mnSessionGameMessageReceived (String     message,
                                            MNUserInfo sender)
   {
   }

  public void mnSessionPluginMessageReceived (String     pluginName,
                                              String     message,
                                              MNUserInfo sender)
   {
    if (trackedPluginsStorage.checkString(pluginName))
     {
      String userInfo;

      if (sender != null)
       {
        userInfo = String.format("new MN_SF_UserInfo(%d,%s,'%d')",
                                 sender.userSFId,
                                 MNUtils.stringAsJSString(sender.userName),
                                 sender.userId);
       }
      else
       {
        userInfo = "null";
       }

      String javaScriptSrc = String.format
                              ("javascript:MN_RecvPluginMessage(" +
                                MNUtils.stringAsJSString(pluginName) +
                                "," +
                                MNUtils.stringAsJSString(message) +
                                "," +
                                userInfo +
                                ")");

      callJSScript(javaScriptSrc);
     }
   }

  public void mnSessionJoinRoomInvitationReceived (MNJoinRoomInvitationParams params)
   {
    long   userId;
    String userName;

    MNUtils.UserNameComponents nameInfo = MNUtils.parseMNUserName(params.fromUserName);

    if (nameInfo != null)
     {
      userId   = nameInfo.userId;
      userName = nameInfo.userName;
     }
    else
     {
      userId   = MNConst.MN_USER_ID_UNDEFINED;
      userName = params.fromUserName;
     }

    String javaScriptSrc = String.format("javascript:MN_InJoinRoomInvitation(%d," +
                                         "new MN_SF_UserInfo(%d,%s,'%d')," +
                                         "new MN_SF_InviteRoom(%d,%s,%d,%d),%s)",
                                         params.fromUserSFId,
                                         params.fromUserSFId,
                                         MNUtils.stringAsJSString(userName),
                                         userId,
                                         params.roomSFId,
                                         MNUtils.stringAsJSString(params.roomName),
                                         params.roomGameId,
                                         params.roomGameSetId,
                                         MNUtils.stringAsJSString(params.inviteText));

    callJSScript(javaScriptSrc);
   }

  public void mnSessionGameStartCountdownTick (int secondsLeft)
   {
    callJSScript(String.format("javascript:MN_InGameStartCountdown(%d)",
                               secondsLeft));
   }

  public void mnSessionCurrGameResultsReceived (MNCurrGameResults gameResults)
   {
    StringBuilder javaScriptSrc =
     new StringBuilder("javascript:MN_InCurrGameResults(new Array(");

    MNUtils.StringJoiner infoStr = new MNUtils.StringJoiner(",");

    for (int index = 0; index < gameResults.users.length; index++)
     {
      MNUserInfo userInfo = gameResults.users[index];

      infoStr.join
       (String.format
         ("new MN_UserGameResult(%d,new MN_SF_UserInfo(%d,%s,'%d'),%d,%d)",
          userInfo.userSFId,
          userInfo.userSFId,
          MNUtils.stringAsJSString(userInfo.userName),
          userInfo.userId,
          gameResults.userPlaces[index],
          gameResults.userScores[index]));
     }

    javaScriptSrc.append(infoStr.toString());
    javaScriptSrc.append(String.format("),%d)",gameResults.finalResult ? 1 : 0));

    callJSScript(javaScriptSrc.toString());
   }

  public void mnSessionErrorOccurred (MNErrorInfo errorInfo)
   {
    if (errorInfo.actionCode == MNErrorInfo.ACTION_CODE_LOAD_CONFIG &&
        webServerUrl == null)
     {
      loadErrorMessagePage(MNI18n.getLocalizedString
                            ("Internet connection is not available",
                             MNI18n.MESSAGE_CODE_INTERNET_CONNECTION_NOT_AVAILABLE_ERROR));
     }
    else
     {
      callSetErrorMessage(errorInfo.errorMessage,errorInfo.actionCode);
     }
   }

  public void mnSessionDoStartGameWithParams (MNGameParams gameParams)
   {
   }

  public void mnSessionDoFinishGame ()
   {
   }

  public void mnSessionDoCancelGame ()
   {
   }

  public void mnSessionGameFinishedWithResult (MNGameResult gameResult)
   {
    callJSScript
     (String.format("javascript:MN_InFinishGameNotify(%d,%s,%d)",
                    gameResult.score,
                    MNUtils.stringAsJSString(gameResult.scorePostLinkId),
                    gameResult.gameSetId));
   }

  public void mnSessionRoomUserJoin (MNUserInfo userInfo)
   {
    callJSScript
     (String.format("javascript:MN_InRoomUserJoin(%d,new MN_SF_UserInfo(%d,%s,'%d'))",
                    userInfo.userSFId,
                    userInfo.userSFId,
                    MNUtils.stringAsJSString(userInfo.userName),
                    userInfo.userId));
   }

  public void mnSessionRoomUserLeave (MNUserInfo userInfo)
   {
    callJSScript
     (String.format("javascript:MN_InRoomUserLeave(%d,new MN_SF_UserInfo(%d,%s,'%d'))",
                    userInfo.userSFId,
                    userInfo.userSFId,
                    MNUtils.stringAsJSString(userInfo.userName),
                    userInfo.userId));
   }

  public void mnSessionSocNetLoggedOut (int socNetId)
   {
    if (socNetId == MNSocNetSessionFB.SOCNET_ID)
     {
      callJSScript("javascript:MN_SetSNContextFacebook(null,null)");
     }
   }

  public void mnSessionDevUsersInfoChanged ()
   {
    devUsersInfoSrcCache = null;
   }

  public void mnSessionConfigLoaded ()
   {
   }

  public void mnSessionConfigLoadStarted ()
   {
   }

  public void mnSessionWebFrontURLReady    (String url)
   {
    if (session != null && webServerUrl == null)
     {
      webViewLoadStartUrl();
     }
   }

  public void mnSessionConfigLoadingFailed (String error)
   {
    if (session != null && webServerUrl == null)
     {
      loadErrorMessagePage(MNI18n.getLocalizedString
                            ("Internet connection is not available",
                             MNI18n.MESSAGE_CODE_INTERNET_CONNECTION_NOT_AVAILABLE_ERROR));
     }
   }

  public void mnSessionExecAppCommandReceived (String cmdName, String cmdParam)
   {
    callJSScript
     (String.format
       ("javascript:MN_RecvAppCommand(%s,%s)",
        MNUtils.stringAsJSString(cmdName),
        MNUtils.stringAsJSString(cmdParam)));
   }

  public void mnSessionExecUICommandReceived (String cmdName, String cmdParam)
   {
   }

  public boolean mnSessionAppHostCallReceived (MNAppHostCallInfo appHostCallInfo)
   {
    return false;
   }

  public void mnSessionWebEventReceived (String eventName, String eventParam, String callbackId)
   {
   }

  public void mnSessionSysEventReceived (String eventName, String eventParam, String callbackId)
   {
    callJSScript
     (String.format
       ("javascript:MN_HandleSysEvent({ 'eventName' : %s, 'eventParam' : %s, 'callbackId' : %s})",
        MNUtils.stringAsJSString(eventName),
        MNUtils.stringAsJSString(eventParam),
        MNUtils.stringAsJSString(callbackId)));
   }

  public void mnSessionAppStartParamUpdated (String param)
   {
    scheduleUpdateContext();
   }

  /* MNSession.SocNetFBEventHandler */

  public void socNetFBLoginOk (MNSocNetSessionFB socNetSession)
   {
    callJSScript
     (String.format
       ("javascript:MN_SetSNContextFacebook(new MN_SNContextFacebook('%d',%s,%s,%d),null)",
        socNetSession.getUserId(),
        MNUtils.stringAsJSString(socNetSession.getSessionKey()),
        MNUtils.stringAsJSString(socNetSession.getSessionSecret()),
       socNetSession.didUserStoreCredentials() ? 1 : 0));

    if (fbLoginSuccessJS != null)
     {
      callJSScriptNoScheme(fbLoginSuccessJS);
     }

    fbLoginSuccessJS = null;
    fbLoginCancelJS  = null;
   }

  public void socNetFBLoginCancelled ()
   {
    if (fbLoginCancelJS != null)
     {
      callJSScriptNoScheme(fbLoginCancelJS);
     }

    fbLoginSuccessJS = null;
    fbLoginCancelJS  = null;
   }

  public void socNetFBLoginFailed (String error)
   {
    callJSScript
     (String.format
       ("javascript:MN_SetSNContextFacebook(null,%s)",
        MNUtils.stringAsJSString(error)));

    fbLoginSuccessJS = null;
    fbLoginCancelJS  = null;
   }

  private void initialize (Context context)
   {
    LinearLayout webViewLayout = new LinearLayout(context);

    webViewLayout.setOrientation(LinearLayout.VERTICAL);

    autoCancelGameOnGoBack = true;
    trustedHosts = Collections.synchronizedSet(new HashSet<String>());
    devUsersInfoSrcCache = null;

    eventHandlers = new MNEventHandlerArray<IMNUserProfileViewEventHandler>();

    trackedPluginsStorage = new MNStrMaskStorage();

    httpReqQueue = new MNUIWebViewHttpReqQueue(this);

    fbLoginSuccessJS = null;
    fbLoginCancelJS  = null;

    WebChromeClient webChromeClient = new MNWebChromeClient();
    MNWebViewClient webViewClient   = new MNWebViewClient();

    webView = new MNSafeWebView(context);
    webView.getSettings().setJavaScriptEnabled(true);
    webView.setWebChromeClient(webChromeClient);
    webView.setWebViewClient(webViewClient);
    webView.addJavascriptInterface(new MNWebViewAppHost(webView),"MNWebViewAppHost");
    webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);

    webViewLayout.addView(webView,new LinearLayout.LayoutParams
                                       (LayoutParams.FILL_PARENT,
                                        /*  LayoutParams.WRAP_CONTENT */ 0,
                                        1));

    loadBootPage();

    navBarView = new MNSafeWebView(context);
    navBarView.getSettings().setJavaScriptEnabled(true);
    navBarView.setWebChromeClient(webChromeClient);
    navBarView.setWebViewClient(webViewClient);
    navBarView.addJavascriptInterface(new MNWebViewAppHost(navBarView),"MNWebViewAppHost");
    navBarView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);

    webViewLayout.addView(navBarView,new LinearLayout.LayoutParams
                                          (LayoutParams.FILL_PARENT,
                                           convertDpToPx(context,NAV_BAR_DEFAULT_HEIGHT),
                                           0));

    navBarView.setVisibilitySafe(View.GONE);

    addView(webViewLayout,new FrameLayout.LayoutParams
                               (LayoutParams.FILL_PARENT,
                                LayoutParams.FILL_PARENT,
                                Gravity.FILL));

    if (progressBarEnabled)
     {
      progressBar = new ProgressBar(context);

      addView(progressBar,new FrameLayout.LayoutParams
                               (LayoutParams.WRAP_CONTENT,
                                LayoutParams.WRAP_CONTENT,
                                Gravity.CENTER));

      progressBar.setVisibility(View.INVISIBLE);
     }

    errorPageLoaded = false;

    webViewIsLoading    = false;
    navBarViewIsLoading = false;
   }

  private String getDeviceUsersInfoJSSrc ()
   {
    if (devUsersInfoSrcCache == null)
     {
      StringBuilder devUsersInfoSrc = new StringBuilder();
      MNUserCredentials[] devUsersInfoArray = MNUserCredentials.loadAllCredentials(session.getVarStorage());

      for (int index = 0; index < devUsersInfoArray.length; index++)
       {
        if (index > 0)
         {
          devUsersInfoSrc.append(",");
         }

        devUsersInfoSrc.append(String.format("new MN_DevUserInfo('%d',%s,%s,%d,%s)",
                                             devUsersInfoArray[index].userId,
                                             MNUtils.stringAsJSString(devUsersInfoArray[index].userName),
                                             MNUtils.stringAsJSString(devUsersInfoArray[index].userAuthSign),
                                             devUsersInfoArray[index].lastLoginTime.getTime() / 1000,
                                             MNUtils.stringAsJSString(devUsersInfoArray[index].userAuxInfoText)));
       }

      devUsersInfoSrcCache = devUsersInfoSrc.toString();
     }

    return devUsersInfoSrcCache;
   }

  private void callUpdateContext (boolean setMode)
   {
    int  status = session.getStatus();
    long userId = session.getMyUserId();

    String roomId = null;

    if (session.isInGameRoom())
     {
      roomId = String.format("%d",session.getCurrentRoomId());
     }

    int roomUserStatus = session.getRoomUserStatus();

    MNSocNetSessionFB fbSession = session.getSocNetSessionFB();
    String fbContext;

    if (fbSession.isConnected())
     {
      fbContext = "new MN_SNContextFacebook('" +
                  Long.toString(fbSession.getUserId()) +
                  "'," +
                  MNUtils.stringAsJSString(fbSession.getSessionKey()) +
                  "," +
                  MNUtils.stringAsJSString(fbSession.getSessionSecret()) +
                  "," +
                  (fbSession.didUserStoreCredentials() ? "1" : "0") +
                  ")";
     }
    else
     {
      fbContext = "null";
     }

    String javaScriptSrc = String.format
                            ("javascript:%s(" +
                             "new MN_Context(%d,%d,%s,%s,%s,%d,%s," +
                             "new Array(%s)," +
                             "new Array(new MN_SNSessionInfo(%d,%d,%s)),%d,%s)," +
                             "new MN_RoomContext(%s,%d,null,%s)",
                             setMode ? "MN_SetContext" : "MN_UpdateContext",
                             status,
                             session.getGameId(),
                             userId == MNConst.MN_USER_ID_UNDEFINED ? "null" : String.format("'%d'",userId),
                             MNUtils.stringAsJSString(session.getMyUserName()),
                             MNUtils.stringAsJSString(session.getMySId()),
                             session.getPlatform().getDeviceType(),
                             MNUtils.stringAsJSString
                              (MNUtils.stringGetMD5String
                                (session.getPlatform().getUniqueDeviceIdentifier())),
                             getDeviceUsersInfoJSSrc(),
                             MNSocNetSessionFB.SOCNET_ID,
                             fbSession.isConnected() ? 10 :
                              (fbSession.didUserStoreCredentials() ? 1 : 0),
                             fbContext,
                             session.getDefaultGameSetId(),
                             MNUtils.stringAsJSString(session.getApplicationStartParam()),
                             MNUtils.stringAsJSString(roomId),
                             session.getRoomGameSetId(),
                             roomUserStatus == MNConst.MN_USER_STATUS_UNDEFINED ? "null" : String.format("'%d'",roomUserStatus));

    if (setMode)
     {
      javaScriptSrc = javaScriptSrc + ");";
     }
    else
     {
      javaScriptSrc = javaScriptSrc + ",null,null);";
     }

    if (setMode)
     {
      if (isWebViewLocationTrusted(webView))
       {
        webView.loadUrl(javaScriptSrc);
       }
     }
    else
     {
      if (isWebViewLocationTrusted(webView))
       {
        webView.loadUrl(javaScriptSrc);
       }

      if (isWebViewLocationTrusted(navBarView))
       {
        navBarView.loadUrl(javaScriptSrc);
       }
     }
   }

  private void callSetErrorMessage (String message)
   {
    callSetErrorMessage(message,MNErrorInfo.ACTION_CODE_UNDEFINED);
   }

  private void callSetErrorMessage (String message, int actionCode)
   {
    callJSScript("javascript:MN_SetErrorMessage(" +
                 MNUtils.stringAsJSString(message) +
                 ",new MN_ErrorContext(" +
                 Integer.toString(actionCode) +
                 "))");
   }

  private void scheduleUpdateContext ()
   {
    runOnUiThread(new Runnable()
     {
      //@Override
      public void run ()
       {
        callUpdateContext(false);
       }
     });
   }

  private void importUserPhoto (Uri uri)
   {
    Bitmap srcBitmap  = null;
    Bitmap destBitmap = null;

    try
     {
      srcBitmap = android.provider.MediaStore.Images.Media.getBitmap
                   (getHostActivity().getContentResolver(),uri);
     }
    catch (IOException e)
     {
     }

    if (srcBitmap != null)
     {
      int srcWidth  = srcBitmap.getWidth();
      int srcHeight = srcBitmap.getHeight();

      ByteArrayOutputStream pngData = new ByteArrayOutputStream();

      if (srcWidth  != AVATAR_IMAGE_DIMENSION ||
          srcHeight != AVATAR_IMAGE_DIMENSION)
       {
        float scaleFactor;

        if (srcWidth > srcHeight)
         {
          scaleFactor = (float)AVATAR_IMAGE_DIMENSION / srcBitmap.getWidth();
         }
        else
         {
          scaleFactor = (float)AVATAR_IMAGE_DIMENSION / srcBitmap.getHeight();
         }

        int newWidth  = (int)(srcWidth  * scaleFactor);
        int newHeight = (int)(srcHeight * scaleFactor);
        int origX     = (AVATAR_IMAGE_DIMENSION - newWidth)  / 2;
        int origY     = (AVATAR_IMAGE_DIMENSION - newHeight) / 2;

        destBitmap = Bitmap.createBitmap(AVATAR_IMAGE_DIMENSION,
                                         AVATAR_IMAGE_DIMENSION,
                                         Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(destBitmap);

        canvas.drawBitmap(srcBitmap,
                          null,
                          new Rect(origX,origY,
                                   origX + newWidth,origY + newHeight),
                          null);

        destBitmap.compress(Bitmap.CompressFormat.PNG,100,pngData);
       }
      else
       {
        srcBitmap.compress(Bitmap.CompressFormat.PNG,100,pngData);
       }

      String javaScriptSrc = "javascript:MN_DoUserPhotoImport(" +
                             MNUtils.stringAsJSString
                              (MNBase64.encode(pngData.toByteArray())) +
                             ")";

      callJSScript(javaScriptSrc);
     }

    if (destBitmap != null)
     {
      destBitmap.recycle();
     }

    if (srcBitmap != null)
     {
      srcBitmap.recycle();
     }
   }

  private class MNWebViewAppHost
          implements MNSocNetSessionFB.IStreamDialogEventHandler,
                     MNSocNetSessionFB.IPermissionDialogEventHandler

   {
    public MNWebViewAppHost (MNSafeWebView webView)
     {
      this.webView = webView;
     }

    public void callAppHost (String requestURLString)
     {
      if (!isWebViewLocationTrusted(webView))
       {
        return;
       }

      try
       {
        MNAppHostRequestURL url = new MNAppHostRequestURL(requestURLString);

        if (url.isAppHostRequest())
         {
          String cmd = url.getCmd();

          String requestInArgParam = url.getStringParam("apphost_req_in_arg");

          if (requestInArgParam != null)
           {
            callAppRequestGuard(requestInArgParam,false);
           }

          if (!session.preprocessAppHostCall(new MNAppHostCallInfo(url)))
           {
            if      (cmd.equals("goback.php"))
             {
              goBack();
             }
            else if (cmd.equals("logout.php"))
             {
              logout(url);
             }
            else if (cmd.equals("navbar_hide.php"))
             {
              navBarHide();
             }
            else if (cmd.equals("navbar_show.php"))
             {
              navBarShow(url);
             }
            else if (cmd.equals("get_context.php"))
             {
              scheduleUpdateContext();
             }
            else if (cmd.equals("connect.php"))
             {
              connect(url);
             }
            else if (cmd.equals("sn_facebook_login.php"))
             {
              facebook_login(url);
             }
            else if (cmd.equals("sn_facebook_resume.php"))
             {
              facebook_resume(url);
             }
            else if (cmd.equals("sn_facebook_logout.php"))
             {
              facebook_logout(url);
             }
            else if (cmd.equals("sn_facebook_dialog_publish_show.php"))
             {
              facebookShowPublishDialog(url);
             }
            else if (cmd.equals("sn_facebook_dialog_permission_req_show.php"))
             {
              facebookShowPermissionDialog(url);
             }
            else if (cmd.equals("webview_reload.php"))
             {
              reload(url);
             }
            else if (cmd.equals("reconnect.php"))
             {
              reconnect();
             }
            else if (cmd.equals("get_room_userlist.php"))
             {
              getRoomUserList();
             }
            else if (cmd.equals("get_game_results.php"))
             {
              getGameResults();
             }
            else if (cmd.equals("chatmess.php"))
             {
              sendPublicMessage(url);
             }
            else if (cmd.equals("sendmess.php"))
             {
              sendPrivateMessage(url);
             }
            else if (cmd.equals("joinbuddyroom.php"))
             {
              joinBuddyRoom(url);
             }
            else if (cmd.equals("joinautoroom.php"))
             {
              joinAutoRoom(url);
             }
            else if (cmd.equals("playgame.php"))
             {
              playGame(url);
             }
            else if (cmd.equals("newbuddyroom.php"))
             {
              newBuddyRoom(url);
             }
            else if (cmd.equals("start_room_game.php"))
             {
              startRoomGame();
             }
            else if (cmd.equals("leaveroom.php"))
             {
              leaveRoom();
             }
            else if (cmd.equals("set_room_user_status.php"))
             {
              setRoomUserStatus(url);
             }
            else if (cmd.equals("script_eval.php"))
             {
              scriptEval(url);
             }
            else if (cmd.equals("http_request.php"))
             {
              sendHttpRequest(url);
             }
            else if (cmd.equals("var_save.php"))
             {
              varSave(url);
             }
            else if (cmd.equals("vars_get.php"))
             {
              varsGet(url);
             }
            else if (cmd.equals("vars_clear.php"))
             {
              varsClear(url);
             }
            else if (cmd.equals("config_vars_get.php"))
             {
              configVarsGet(url);
             }
            else if (cmd.equals("plugin_message_subscribe.php"))
             {
              pluginMessageSubscribe(url);
             }
            else if (cmd.equals("plugin_message_unsubscribe.php"))
             {
              pluginMessageUnSubscribe(url);
             }
            else if (cmd.equals("plugin_message_send.php"))
             {
              pluginMessageSend(url);
             }
            else if (cmd.equals("void.php"))
             {
             }
            else if (cmd.equals("set_host_param.php"))
             {
              setHostParam(url);
             }
            else if (cmd.equals("exec_ui_command.php"))
             {
              execUiCommand(url);
             }
            else if (cmd.equals("navbar_cancel_url_load.php"))
             {
              loadNavBarExtRequest = false;
             }
            else if (cmd.equals("do_photo_import.php"))
             {
              importUserPhoto();
             }
            else if (cmd.equals("set_game_results.php"))
             {
              setGameResults(url);
             }
            else if (cmd.equals("add_source_domain.php"))
             {
              addSourceDomain(url);
             }
            else if (cmd.equals("remove_source_domain.php"))
             {
              removeSourceDomain(url);
             }
            else if (cmd.equals("post_web_event.php"))
             {
              postWebEvent(url);
             }
            else if (cmd.equals("get_user_ab_data.php"))
             {
              getUserABData();
             }
            else if (cmd.equals("app_is_installed.php"))
             {
              performIsAppInstalledQuery(url);
             }
            else if (cmd.equals("app_try_launch.php"))
             {
              appLaunch(url);
             }
            else if (cmd.equals("app_show_in_market.php"))
             {
              appShowInMarket(url);
             }
            else
             {
              Log.d(TAG,"unsupported command (" + cmd + ") received\n");
             }
           }

          String requestOutArgParam = url.getStringParam("apphost_req_out_arg");

          if (requestOutArgParam != null)
           {
            callAppRequestGuard(requestOutArgParam,true);
           }
         }
       }
      catch (Exception e)
       {
        Log.d(TAG,"exception in callAppHost: " + e.getMessage() + "\n");
       }
     }

    private void callAppRequestGuard (String param, boolean isPostGuard)
     {
      scheduleLoadUrl(webView,
                      "javascript:" +
                      (isPostGuard ? "MN_AppHostReqOut(" : "MN_AppHostReqIn(") +
                      MNUtils.stringAsJSString(param) +
                      ")");
     }

    private void goBack ()
     {
      if (autoCancelGameOnGoBack && session != null && session.isOnline())
       {
        session.cancelGameWithParams(null);
       }

      eventHandlers.beginCall();

      try
       {
        int index;
        int count = eventHandlers.size();

        for (index = 0; index < count; index++)
         {
          eventHandlers.get(index).mnUserProfileViewDoGoBack();
         }
       }
      finally
       {
        eventHandlers.endCall();
       }
     }

    private void navBarHide ()
     {
      runOnUiThread(new Runnable()
       {
        //@Override
        public void run ()
         {
          if (navBarView.getVisibilitySafe() == View.VISIBLE)
           {
            navBarView.setVisibilitySafe(View.GONE);
           }
         }
       });
     }

    private void navBarShow (MNAppHostRequestURL url)
     {
      final String navBarUrl = url.getStringParam("navbar_url");

      if (navBarUrl != null)
       {
        Integer newHeightParam = MNUtils.parseInteger
                                  (url.getStringParam("navbar_height"));

        if (newHeightParam == null || newHeightParam < 0)
         {
          newHeightParam = NAV_BAR_DEFAULT_HEIGHT;
         }

        final int newHeight = convertDpToPx(getContext(),newHeightParam);

        runOnUiThread(new Runnable()
         {
          //@Override
          public void run ()
           {
            navBarView.loadUrl(navBarUrl);
            navBarView.setVisibilitySafe(View.VISIBLE);

            if (navBarView.getVisibilitySafe() != View.VISIBLE ||
                navBarView.getHeightSafe() != newHeight)
             {
              navBarView.setLayoutParamsSafe
               (new LinearLayout.LayoutParams
                     (LayoutParams.FILL_PARENT,
                      newHeight,
                      0));

              navBarView.requestLayoutSafe();
             }
           }
         });
       }
      else
       {
        Log.d(TAG,"navbar_url is not set in navbar_show request");
       }
     }

    private void connect (MNAppHostRequestURL url) throws MNException
     {
      String mode = url.getStringParam("mode");

      if (mode != null)
       {
        if (mode.equals("login_multinet_by_user_id_and_phash"))
         {
          Long   userId        = MNUtils.parseLong(url.getStringParam("user_id"));
          String passwordHash  = url.getStringParam("user_password_hash");
          String devSetHomeStr = url.getStringParam("user_dev_set_home");

          if (userId == null)
           {
            Log.d(TAG,"invalid user id");
           }
          else if (passwordHash == null)
           {
            Log.d(TAG,"password hash is null");
           }
          else
           {
            boolean devSetHome;

            devSetHome = (devSetHomeStr != null && devSetHomeStr.equals("1"));

            session.loginWithUserIdAndPasswordHash(userId,passwordHash,devSetHome);
           }
         }
        else if (mode.equals("login_multinet_user_id_and_auth_sign"))
         {
          Long   userId        = MNUtils.parseLong(url.getStringParam("user_id"));
          String authSign      = url.getStringParam("user_auth_sign");

          if (userId == null)
           {
            Log.d(TAG,"invalid user id");
           }
          else if (authSign == null)
           {
            Log.d(TAG,"auth sign is null");
           }
          else
           {
            session.loginWithUserIdAndAuthSign(userId,authSign);
           }
         }
        else if (mode.equals("login_multinet_user_id_and_auth_sign_offline"))
         {
          Long   userId        = MNUtils.parseLong(url.getStringParam("user_id"));
          String authSign      = url.getStringParam("user_auth_sign");

          if (userId == null)
           {
            Log.d(TAG,"invalid user id");
           }
          else if (authSign == null)
           {
            Log.d(TAG,"auth sign is null");
           }
          else
           {
            session.loginOfflineWithUserIdAndAuthSign(userId,authSign);
           }
         }
        else if (mode.equals("login_multinet_auto"))
         {
          session.loginAuto();
         }
        else if (mode.equals("login_multinet"))
         {
          String userLogin     = url.getStringParam("user_login");
          String userPassword  = url.getStringParam("user_password");
          String devSetHomeStr = url.getStringParam("user_dev_set_home");

          if (userLogin == null)
           {
            Log.d(TAG,"user login is not set in login_multinet login");
           }
          else if (userPassword == null)
           {
            Log.d(TAG,"user password is not set in login_multinet login");
           }
          else
           {
            boolean devSetHome;

            devSetHome = (devSetHomeStr != null && devSetHomeStr.equals("1"));

            session.loginWithUserLoginAndPassword(userLogin,userPassword,devSetHome);
           }
         }
        else if (mode.equals("login_multinet_signup_offline"))
         {
          session.signupOffline();
         }
        else
         {
          Log.d(TAG,"invalid mode : [" + mode + "]\n");
         }
       }
      else
       {
        Log.d(TAG,"mode parameter not set");
       }
     }

    private void logout (MNAppHostRequestURL url)
     {
      if (session != null)
       {
        Integer wipeHome = MNUtils.parseInteger
                            (url.getStringParam("user_dev_wipe_home"));

        if (wipeHome == null)
         {
          wipeHome = MNSession.MN_CREDENTIALS_WIPE_NONE;
         }

        session.logoutAndWipeUserCredentialsByMode(wipeHome);

        devUsersInfoSrcCache = null;
       }
     }

    private void reload (MNAppHostRequestURL url)
     {
      final String webViewUrl   = url.getStringParam("webview_url");
      final MNSafeWebView mainWebView = MNUserProfileView.this.webView;

      if (webViewUrl != null)
       {
        runOnUiThread(new Runnable()
         {
          //@Override
          public void run ()
           {
            mainWebView.loadUrl(webViewUrl);
           }
         });
       }
      else
       {
        Log.d(TAG,"webview_url is not set in webview_reload request");
       }
     }

    private void reconnect ()
     {
      if (session != null)
       {
        if (session.getStatus() == MNConst.MN_OFFLINE &&
            session.isReLoginPossible())
         {
          session.reLogin();
         }
        else
         {
          webViewLoadStartUrl();
         }
       }
     }

    private void getRoomUserList ()
     {
      if (session == null || !session.isOnline())
       {
        return;
       }

      StringBuilder javaScriptSrc = new StringBuilder("javascript:MN_InRoomUserList(new Array(");
      MNUtils.StringJoiner userListStr = new MNUtils.StringJoiner(",");
      MNUserInfo[] userList = session.getRoomUserList();

      for (MNUserInfo userInfo : userList)
       {
        userListStr.join(String.format("new MN_SF_UserInfo(%d,%s,'%d')",
                                       userInfo.userSFId,
                                       MNUtils.stringAsJSString
                                        (userInfo.userName),
                                       userInfo.userId));
       }

      javaScriptSrc.append(userListStr.toString());
      javaScriptSrc.append("));");

      callJSScript(javaScriptSrc.toString());
     }

    private void getGameResults ()
     {
      if (session != null)
       {
        session.reqCurrentGameResults();
       }
     }

    private void sendPublicMessage (MNAppHostRequestURL url)
     {
      if (session == null || !session.isOnline())
       {
        return;
       }

      String message = url.getStringParam("mess_text");

      if (message != null)
       {
        session.sendChatMessage(message);
       }
      else
       {
        Log.d(TAG,"message is not set in chatmess request");
       }
     }

    private void sendPrivateMessage (MNAppHostRequestURL url)
     {
      if (session == null || !session.isOnline())
       {
        return;
       }

      String  message    = url.getStringParam("mess_text");
      Integer toUserSFId = MNUtils.parseInteger(url.getStringParam("to_user_sfid"));

      if (message == null)
       {
        Log.d(TAG,"message is not set in sendmess request");
       }
      else if (toUserSFId == null)
       {
        Log.d(TAG,"to_user_sfid is not set or invalid in sendmess request");
       }
      else
       {
        session.sendPrivateMessage(message,toUserSFId);
       }
     }

    private void newBuddyRoom (MNAppHostRequestURL url)
     {
      if (session == null)
       {
        return;
       }

      String roomName       = url.getStringParam("room_name");
      Integer gameSetId     = MNUtils.parseInteger(url.getStringParam("gameset_id"));
      String toUserIdList   = url.getStringParam("to_user_id_list");
      String toUserSFIdList = url.getStringParam("to_user_sfid_list");
      String inviteText     = url.getStringParam("mess_text");

      if (roomName != null && gameSetId != null && toUserIdList != null &&
           toUserSFIdList != null && inviteText != null)
       {
        MNBuddyRoomParams buddyRoomParams =
         new MNBuddyRoomParams(roomName,gameSetId,toUserIdList,toUserSFIdList,inviteText);

        session.reqCreateBuddyRoom(buddyRoomParams);
       }
      else
       {
        Log.d(TAG,"not enough parameters in newbuddyroom request");
       }
     }

    private void startRoomGame ()
     {
      if (session == null || session.getStatus() != MNConst.MN_IN_GAME_WAIT)
       {
        callSetErrorMessage(MNI18n.getLocalizedString("Room not ready",MNI18n.MESSAGE_CODE_ROOM_IS_NOT_READY_TO_START_A_GAME_ERROR));
       }
      else
       {
        session.reqStartBuddyRoomGame();
       }
     }

    private void joinBuddyRoom (MNAppHostRequestURL url)
     {
      if (session == null || !session.isOnline())
       {
        return;
       }

      Integer roomSFId = MNUtils.parseInteger(url.getStringParam("room_sfid"));

      if (roomSFId != null)
       {
        session.reqJoinBuddyRoom(roomSFId);
       }
      else
       {
        Log.d(TAG,"room_sfid is not set or invalid in join buddy room request");
       }
     }

    private void joinAutoRoom (MNAppHostRequestURL url)
     {
      if (session == null || !session.isOnline())
       {
        callSetErrorMessage(MNI18n.getLocalizedString("you must be in lobby room to join",MNI18n.MESSAGE_CODE_MUST_BE_IN_LOBBY_ROOM_TO_JOIN_RANDOM_ROOM_ERROR));

        return;
       }

      String gameSetId = url.getStringParam("gameset_id");

      if (gameSetId != null)
       {
        session.reqJoinRandomRoom(gameSetId);
       }
      else
       {
        Log.d(TAG,"gameset_id is not set in join auto-room request");
       }
     }

    private void playGame (MNAppHostRequestURL url)
     {
      if (session == null)
       {
        return;
       }

      Integer gameSetId = MNUtils.parseInteger(url.getStringParam("gameset_id"));
      String  gameSetParams = url.getStringParam("gameset_params");
      String  scorePostLinkId = url.getStringParam("game_scorepostlink_id");
      Integer gameSeed = MNUtils.parseInteger(url.getStringParam("game_seed"));

      if (gameSetId != null && gameSetParams != null &&
          scorePostLinkId != null && gameSeed != null)
       {
        MNGameParams gameParams =
         new MNGameParams(gameSetId,gameSetParams,
                          scorePostLinkId,
                          gameSeed,
                          scorePostLinkId.length() > 0 ?
                           MNGameParams.MN_PLAYMODEL_SINGLEPLAY_NET :
                           MNGameParams.MN_PLAYMODEL_SINGLEPLAY);

        String[] playParams = url.getParamNamesWithPrefix
                               (GAMESET_PLAY_PARAM_PREFIX);

        for (String playParam : playParams)
         {
          gameParams.addGameSetPlayParam
           (playParam.substring(GAMESET_PLAY_PARAM_PREFIX.length()),
            url.getStringParam(playParam));
         }

        session.startGameWithParams(gameParams);
       }
      else
       {
        Log.d(TAG,"one of the 'play game' request parameters is not set or invalid");
       }
     }

    private void leaveRoom ()
     {
      if (session == null)
       {
        return;
       }

      session.leaveRoom();
     }

    private void setRoomUserStatus (MNAppHostRequestURL url)
     {
      if (session == null || !session.isOnline())
       {
        return;
       }

      Integer userStatus = MNUtils.parseInteger(url.getStringParam("mn_user_status"));

      if (userStatus != null)
       {
        session.reqSetUserStatus(userStatus);
       }
      else
       {
        Log.d(TAG,"mn_user_status is not set in 'set user status' request");
       }
     }

    private void pluginMessageSubscribe (MNAppHostRequestURL url)
     {
      String mask = url.getStringParam("plugin_mask");

      if (mask != null)
       {
        trackedPluginsStorage.addMask(mask);
       }
     }

    private void setGameResults (MNAppHostRequestURL url)
     {
      if (session == null)
       {
        return;
       }

      String scoreStr        = url.getStringParam("score");
      String scorePostLinkId = url.getStringParam("score_post_link_id");
      String gameSetIdStr    = url.getStringParam("gameset_id");

      if (scoreStr != null && gameSetIdStr != null)
       {
        try
         {
          long         score      = Long.parseLong(scoreStr);
          int          gameSetId  = Integer.parseInt(gameSetIdStr);
          MNGameResult gameResult = new MNGameResult(null);

          gameResult.score           = score;
          gameResult.scorePostLinkId = scorePostLinkId;
          gameResult.gameSetId       = gameSetId;

          session.finishGameWithResult(gameResult);
         }
        catch (NumberFormatException e)
         {
          Log.d(TAG,"score or game_set_id parameter cannot be converted to int in 'set_game_results' request");
         }
       }
      else
       {
        Log.d(TAG,"score or game_set_id parameter is not set in 'set_game_results' request");
       }
     }

    private void addSourceDomain (MNAppHostRequestURL url)
     {
      String domainName = url.getStringParam("domain_name");

      if (domainName != null)
       {
        trustedHosts.add(domainName);
       }
      else
       {
        Log.d(TAG,"domain name parameter is not set in 'add_source_domain' request");
       }
     }

    private void removeSourceDomain (MNAppHostRequestURL url)
     {
      String domainName = url.getStringParam("domain_name");

      if (domainName != null)
       {
        trustedHosts.remove(domainName);
       }
      else
       {
        Log.d(TAG,"domain name parameter is not set in 'remove_source_domain' request");
       }
     }

    private void performIsAppInstalledQuery (MNAppHostRequestURL url)
     {
      String appPackageName = url.getStringParam("app_install_bundle_id");

      if (appPackageName != null)
       {
        boolean isInstalled = MNLauncherTools.isApplicationInstalled
                               (getHostActivity(),appPackageName);

        callJSScript("javascript:MN_AppCheckInstalledCallback(" +
                       MNUtils.stringAsJSString(appPackageName) +
                        "," + (isInstalled ? "true" : "false") + ")");
       }
      else
       {
        Log.d(TAG,"app_install_bundle_id parameter is not set in 'app_is_installed' request");
       }
     }

    private void appLaunch (MNAppHostRequestURL url)
     {
      String appPackageName = url.getStringParam("app_launch_bundle_id");
      String launchParam    = url.getStringParam("app_launch_param");

      if (appPackageName != null && launchParam != null)
       {
        boolean ok = MNLauncherTools.launchApplication
                      (getHostActivity(),appPackageName,launchParam);

        callJSScript("javascript:MN_AppLaunchCallback(" +
                       MNUtils.stringAsJSString(appPackageName) +
                        "," + (ok ?  "true" : "false") + ")");
       }
      else
       {
        Log.d(TAG,"app_launch_bundle_id or app_launch_param parameter is not set in 'app_try_launch' request");
       }
     }

    private void appShowInMarket (MNAppHostRequestURL url)
     {
      String marketUrl = url.getStringParam("app_market_url");

      if (marketUrl != null)
       {
        Intent intent = new Intent(Intent.ACTION_VIEW,Uri.parse(marketUrl));

        getHostActivity().startActivity(intent);
       }
      else
       {
        Log.d(TAG,"app_market_url parameter is not set in 'app_show_in_market' request");
       }
     }

    private void pluginMessageUnSubscribe (MNAppHostRequestURL url)
     {
      String mask = url.getStringParam("plugin_mask");

      if (mask != null)
       {
        trackedPluginsStorage.removeMask(mask);
       }
     }

    private void pluginMessageSend (MNAppHostRequestURL url)
     {
      if (session == null || !session.isOnline())
       {
        return;
       }

      String pluginName    = url.getStringParam("plugin_name");
      String pluginMessage = url.getStringParam("plugin_message");

      if (pluginName != null && pluginMessage != null)
       {
        try
         {
          session.sendPluginMessage(pluginName,pluginMessage);
         }
        catch (MNException e)
         {
         }
       }
     }

    private void setHostParam (MNAppHostRequestURL url)
     {
      if (session != null)
       {
        String contextCallWaitLoadParam = url.getStringParam("context_call_wait_load");

        if (contextCallWaitLoadParam != null)
         {
          setContextCallWaitLoad(!contextCallWaitLoadParam.equals("0"));
         }
       }
     }

    private void execUiCommand (MNAppHostRequestURL url)
     {
      if (session != null)
       {
        String cmdNameParam  = url.getStringParam("command_name");
        String cmdParamParam = url.getStringParam("command_param");

        if (cmdNameParam != null)
         {
          session.execUICommand(cmdNameParam,cmdParamParam);
         }
        else
         {
          Log.d(TAG,"'command_name' parameter is not set in 'exec_ui_command' request");
         }
       }
     }

    private void postWebEvent (MNAppHostRequestURL url)
     {
      if (session != null)
       {
        final String eventName  = url.getStringParam("event_name");
        final String eventParam = url.getStringParam("event_param");
        final String callbackId = url.getStringParam("callback_id");

        if (eventName != null)
         {
          session.processWebEvent(eventName,eventParam,callbackId);
         }
        else
         {
          Log.d(TAG,"'event_name' parameter is not set in 'post_web_event' request");
         }
       }
     }

    private void scriptEval (MNAppHostRequestURL url)
     {
      String jsCode = url.getStringParam("jscript_eval");

      if (jsCode != null)
       {
        String  forceEval = url.getStringParam("force_eval");
        boolean forceFlag = forceEval != null && forceEval.equals("1");

        callJSScript("javascript:" + jsCode,forceFlag);
       }
     }

    private void sendHttpRequest (MNAppHostRequestURL url)
     {
      String reqUrl     = url.getStringParam("req_url");
      String postParams = url.getStringParam("req_post_params");
      String okJSCode   = url.getStringParam("req_ok_eval");
      String failJSCode = url.getStringParam("req_fail_eval");
      String flagsStr   = url.getStringParam("req_flags");

      if (reqUrl != null && okJSCode != null && failJSCode != null && flagsStr != null)
       {
        try
         {
          long flags = Long.parseLong(flagsStr);

          if (flags >= 0)
           {
            httpReqQueue.addRequest(reqUrl,postParams,okJSCode,failJSCode,(int)flags);
           }
         }
        catch (NumberFormatException e)
         {
         }
       }
     }

    private void facebook_login (MNAppHostRequestURL url)
     {
      fbLoginSuccessJS = url.getStringParam("mn_callback");
      fbLoginCancelJS  = url.getStringParam("mn_cancel");

      final String permissionsList = url.getStringParam("permission");

      final String[] permissions = permissionsList == null ? null : permissionsList.split(",");

      runOnUiThread(new Runnable()
       {
        //@Override
        public void run ()
         {
          if (session != null)
           {
            ((MNPlatformAndroid)session.getPlatform()).setBaseView(MNUserProfileView.this);

            String error = session.socNetFBConnect(MNUserProfileView.this,permissions);

            if (error != null)
             {
              socNetFBLoginFailed(error);
             }
           }
         }
       });
     }

    private void facebook_resume (MNAppHostRequestURL url)
     {
      if (session != null)
       {
        String error = session.socNetFBResume(MNUserProfileView.this);

        if (error != null)
         {
          MNUserProfileView.this.socNetFBLoginFailed(error);
         }
       }
     }

    private void facebook_logout (MNAppHostRequestURL url)
     {
      if (session != null)
       {
        session.socNetFBLogout();
       }
     }

    private void facebookShowPublishDialog (MNAppHostRequestURL url)
     {
      final String messagePrompt = url.getStringParam("message_prompt");
      final String attachment    = url.getStringParam("attachment");
      final String actionLinks   = url.getStringParam("action_links");
      final String targetId      = url.getStringParam("target_id");

      String successJS = url.getStringParam("mn_callback");
      String cancelJS  = url.getStringParam("mn_cancel");

      if (messagePrompt != null && attachment != null && actionLinks != null &&
          targetId != null && successJS != null && cancelJS != null)
       {
        fbPublishSuccessJS = successJS;
        fbPublishCancelJS  = cancelJS;

        runOnUiThread(new Runnable()
         {
          public void run ()
           {
            if (session != null)
             {
              ((MNPlatformAndroid)session.getPlatform()).setBaseView(MNUserProfileView.this);

              String error = session.getSocNetSessionFB().showStreamDialog
                              (messagePrompt,attachment,targetId,actionLinks,MNWebViewAppHost.this);

              if (error != null)
               {
                socNetFBStreamDialogFailedWithError(error);
               }
             }
           }
         });
       }
     }

    private void facebookShowPermissionDialog (MNAppHostRequestURL url)
     {
      final String permission = url.getStringParam("permission");

      String successJS = url.getStringParam("mn_callback");
      String cancelJS  = url.getStringParam("mn_cancel");

      if (permission != null && successJS != null && cancelJS != null)
       {
        fbPermissionSuccessJS = successJS;
        fbPermissionCancelJS  = cancelJS;

        runOnUiThread(new Runnable()
         {
          public void run ()
           {
            if (session != null)
             {
              ((MNPlatformAndroid)session.getPlatform()).setBaseView(MNUserProfileView.this);

              String error = session.getSocNetSessionFB().showPermissionDialog(permission,MNWebViewAppHost.this);

              if (error != null)
               {
                socNetFBPermissionDialogFailedWithError(error);
               }
             }
           }
         });
       }
     }

    public void socNetFBStreamDialogOk                 ()
     {
      if (fbPublishSuccessJS != null)
       {
        callJSScriptNoScheme(fbPublishSuccessJS);
       }

      fbPublishSuccessJS = null;
      fbPublishCancelJS  = null;
     }

    public void socNetFBStreamDialogCanceled           ()
     {
      if (fbPublishCancelJS != null)
       {
        callJSScriptNoScheme(fbPublishCancelJS);
       }

      fbPublishSuccessJS = null;
      fbPublishCancelJS  = null;
     }

    public void socNetFBStreamDialogFailedWithError    (String    error)
     {
      callSetErrorMessage(error);

      fbPublishSuccessJS = null;
      fbPublishCancelJS  = null;
     }

    public void socNetFBPermissionDialogOk             ()
     {
      if (fbPermissionSuccessJS != null)
       {
        callJSScriptNoScheme(fbPermissionSuccessJS);
       }

      fbPermissionSuccessJS = null;
      fbPermissionCancelJS  = null;
     }

    public void socNetFBPermissionDialogCanceled       ()
     {
      if (fbPermissionCancelJS != null)
       {
        callJSScriptNoScheme(fbPermissionCancelJS);
       }

      fbPermissionSuccessJS = null;
      fbPermissionCancelJS  = null;
     }

    public void socNetFBPermissionDialogFailedWithError(String    error)
     {
      callSetErrorMessage(error);

      fbPermissionSuccessJS = null;
      fbPermissionCancelJS  = null;
     }

    private void varSave (MNAppHostRequestURL url)
     {
      if (session != null)
       {
        String name  = url.getStringParam("var_name");
        String value = url.getStringParam("var_value");

        if (name != null)
         {
          session.varStorageSetValue(name,value);
         }
       }
     }

    private void varsClear (MNAppHostRequestURL url)
     {
      if (session != null)
       {
        String list = url.getStringParam("var_name_list");

        if (list != null)
         {
          session.varStorageRemoveVariablesByMasks(list.split(","));
         }
       }
     }

    private String makeConfigVarsList (Map<String,String> vars)
     {
      MNUtils.StringJoiner varListSrc = new MNUtils.StringJoiner(",");

      for (String name : vars.keySet())
       {
        varListSrc.join("new MN_HostVar(" +
                        MNUtils.stringAsJSString(name) + "," +
                        MNUtils.stringAsJSString(vars.get(name)) + ")");
       }

      return varListSrc.toString();
     }

    private void varsGet (MNAppHostRequestURL url)
     {
      if (session == null)
       {
        return;
       }

      String list = url.getStringParam("var_name_list");

      if (list != null)
       {
        callJSScript("javascript:MN_HostVarUpdate(new Array(" +
                     makeConfigVarsList
                      (session.varStorageGetValuesByMasks(list.split(","))) +
                     "))");
       }
     }

    private void configVarsGet (MNAppHostRequestURL url)
     {
      if (session == null)
       {
        return;
       }

      callJSScript("javascript:MN_ConfigVarUpdate(new Array(" +
                    makeConfigVarsList
                     (session.getTrackingSystem().getTrackingVars()) +
                    "),new Array(" +
                    makeConfigVarsList
                     (session.getAppConfigVars()) +
                   "))");
     }

    private void importUserPhoto ()
     {
      if (session != null && session.isUserLoggedIn())
       {
        try
         {
          MNImageSelector.showImageSelector
           (getHostActivity(),
            new MNImageSelector.IEventHandler()
             {
              public void onImageSelected (Uri imageUri)
               {
                MNUserProfileView.this.importUserPhoto(imageUri);
               }
             });
         }
        catch (MNException e)
         {
          Log.e(TAG,e.toString());

          callSetErrorMessage(e.toString());
         }
       }
      else
       {
        callSetErrorMessage(MNI18n.getLocalizedString("You must be connected to import your photo",MNI18n.MESSAGE_CODE_YOU_MUST_BE_CONNECTED_TO_IMPORT_YOUR_PHOTO_ERROR));
       }
     }

    private void getUserABData ()
     {
      if (session != null)
       {
        List<MNAddrBookAccess.AddressInfo> contactInfoList = MNAddrBookAccess.getAddressBookRecords
                                                              (getHostActivity().getContentResolver());

        if (contactInfoList != null)
         {
          StringBuilder javaScriptSrc = new StringBuilder("javascript:MN_ProcUserABData(new Array(");
          MNUtils.StringJoiner contactListStr = new MNUtils.StringJoiner(",");

          for (MNAddrBookAccess.AddressInfo contactInfo : contactInfoList)
           {
            contactListStr.join
             (String.format("new MN_AB_UserInfo(%s,%s)",
                            MNUtils.stringAsJSString(contactInfo.name),
                            MNUtils.stringAsJSString(contactInfo.email)));
           }

          javaScriptSrc.append(contactListStr.toString());
          javaScriptSrc.append("));");

          callJSScript(javaScriptSrc.toString());
         }
        else
         {
          callSetErrorMessage(MNI18n.getLocalizedString("Cannot get contact list",MNI18n.MESSAGE_CODE_CANNOT_GET_CONTACT_LIST_ERROR));
         }
       }
     }

    private MNSafeWebView webView;
    private String  fbPublishSuccessJS;
    private String  fbPublishCancelJS;
    private String  fbPermissionSuccessJS;
    private String  fbPermissionCancelJS;
   }

  private void loadBootPage ()
   {
    try
     {
      Resources resources = getContext().getResources();

      InputStream inputStream = resources.getAssets().open
                                 (BOOT_PAGE_FILE_NAME);

      String bootPageContent = MNUtils.readInputStreamContent(inputStream);

      webView.loadDataWithBaseURL("file://android_asset/" + BOOT_PAGE_FILE_NAME,
                                  bootPageContent,
                                  "text/html",
                                  "utf-8",
                                  null);
     }
    catch (Exception e)
     {
     }
   }

  private static final String ERROR_PAGE_TEMPLATE_EMBEDDED =
   "<html>" +
    "<head>" +
      "<title>MultiNet: Error</title>" +
    "</head>" +
    "<body onclick=\"location.assign('apphost_goback.php');\">" +
     "<div align=\"center\" style=\"padding:20px;padding-top:150px;padding-bottom:150px;color:red\" " +
     "     onclick=\";\" " + // Empty event handler is needed to workaround browser bug (onclick not fired on body if no handler defined here)
     ">" +
      "<b>MultiNet: Error</b><br/>" +
      "<script>document.write({0});</script>" +
     "</div>" +
    "</body>" +
   "</html>";

  private void loadErrorMessagePage (String errorMessage)
   {
    String errorPageTemplate = null;

    try
     {
      Resources resources = getContext().getResources();

      InputStream inputStream = resources.getAssets().open
                                 (ERROR_PAGE_FILE_NAME);

      errorPageTemplate = MNUtils.readInputStreamContent(inputStream);
     }
    catch (Exception e)
     {
      Log.d(TAG,e.toString());
     }

    if (errorPageTemplate == null)
     {
      errorPageTemplate = ERROR_PAGE_TEMPLATE_EMBEDDED;
     }

    String pageContent = errorPageTemplate.replaceAll
                          ("\\{0\\}",MNUtils.stringAsJSString(errorMessage));

    navBarView.setVisibilitySafe(View.GONE);

    errorPageLoaded = true;

    String baseUrl = "file:///android_asset/" + ERROR_PAGE_FILE_NAME;

    webView.loadDataWithBaseURL(baseUrl,pageContent,"text/html","utf-8",baseUrl);
   }

  private class MNWebChromeClient extends WebChromeClient
   {
    //@Override
    public boolean onJsAlert (WebView view, String url, String message, final JsResult result)
     {
      new AlertDialog.Builder(MNUserProfileView.this.getContext())
           .setTitle("Alert")
            .setMessage(message)
             .setPositiveButton("Ok",new DialogInterface.OnClickListener()
                                          {
                                           public void onClick (DialogInterface dialog, int which)
                                            {
                                             result.confirm();
                                            }
                                          })
              .setCancelable(false)
               .create()
                .show();

      return true;
     }
   }

  private class MNWebViewClient extends WebViewClient
   {
    private void pageLoadStateChanged (WebView view, boolean started)
     {
      if (view == webView)
       {
        webViewIsLoading = started;
       }
      else if (view == navBarView)
       {
        navBarViewIsLoading = started;
       }

      if (progressBarEnabled)
       {
        if (webViewIsLoading || navBarViewIsLoading)
         {
          progressBar.setVisibility(View.VISIBLE);
         }
        else
         {
          progressBar.setVisibility(View.INVISIBLE);
         }
       }
     }

    //@Override
    public void onReceivedError (WebView view, int errorCode, String description, String failingUrl)
     {
      pageLoadStateChanged(view,false);

      loadErrorMessagePage(description);
     }

    public void onPageStarted(WebView view, String url, Bitmap favicon)
     {
      pageLoadStateChanged(view,true);
     }

    public void onPageFinished(WebView view, String url)
     {
      pageLoadStateChanged(view,false);
     }

    public boolean shouldOverrideUrlLoading (WebView view, String urlString)
     {
      if (view == navBarView)
       {
        try
         {
          URL url = new URL(urlString);

          if (url.getPath().startsWith(MNAppHostRequestURL.REQUEST_PREFIX))
           {
            return false;
           }

          if (!isHostTrusted(url.getHost()))
           {
            loadNavBarExtRequest = true;

            String javaScriptCode = "javascript:MN_NavBarCheckLoadUrl(" +
                                      MNUtils.stringAsJSString(urlString) +
                                       ",null)";

            navBarView.loadUrl(javaScriptCode);

            return !loadNavBarExtRequest;
           }
         }
        catch (MalformedURLException e)
         {
         }
       }

      return false;
     }
   }

  private static int convertDpToPx (Context context, int dp)
   {
    if (context != null)
     {
      float scale = context.getResources().getDisplayMetrics().density;

      return (int)(dp * scale + 0.5f);
     }
    else
     {
      return dp;
     }
   }

  private MNSession session;
  private MNEventHandlerArray<IMNUserProfileViewEventHandler> eventHandlers;

  private String webServerUrl;
  private MNSafeWebView webView;
  private MNSafeWebView navBarView;
  private ProgressBar progressBar;
  private static boolean progressBarEnabled = false;
  private Set<String> trustedHosts;
  private boolean autoCancelGameOnGoBack;
  private boolean contextCallWaitLoad;
  private String devUsersInfoSrcCache;
  private boolean errorPageLoaded;
  private MNStrMaskStorage trackedPluginsStorage;
  private Activity hostActivity;

  private boolean webViewIsLoading;
  private boolean navBarViewIsLoading;

  private boolean loadNavBarExtRequest;

  private MNUIWebViewHttpReqQueue httpReqQueue;

  private String  fbLoginSuccessJS;
  private String  fbLoginCancelJS;

  private static final int NAV_BAR_DEFAULT_HEIGHT = 49;
  private static final int AVATAR_IMAGE_DIMENSION = 55;
  private static final String GAMESET_PLAY_PARAM_PREFIX = "gameset_play_param_";

  private static final String URL_PROTOCOL_FILE  = "file";
  private static final String URL_PROTOCOL_HTTP  = "http";
  private static final String URL_PROTOCOL_HTTPS = "https";

  private static final String BOOT_PAGE_FILE_NAME = "multinet_boot.html";
  private static final String ERROR_PAGE_FILE_NAME = "multinet_http_error.html";

  private static final int HTTPREQ_FLAG_EVAL_IN_MAINWEBVIEW_MASK   = 0x0001;
  private static final int HTTPREQ_FLAG_EVAL_IN_NAVBARWEBVIEW_MASK = 0x0002;

  private static final String TAG = "MNUserProfileView";
 }

