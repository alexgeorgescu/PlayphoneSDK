//
//  MNAppHostCallInfo.java
//  MultiNet client
//
//  Copyright 2010 PlayPhone. All rights reserved.
//

package com.playphone.multinet.core;

import java.util.Map;

public class MNAppHostCallInfo
 {
  public static final String CommandConnect                      = "apphost_connect.php";
  public static final String CommandReconnect                    = "apphost_reconnect.php";
  public static final String CommandGoBack                       = "apphost_goback.php";
  public static final String CommandLogout                       = "apphost_logout.php";
  public static final String CommandSendPrivateMessage           = "apphost_sendmess.php";
  public static final String CommandSendPublicMessage            = "apphost_chatmess.php";
  public static final String CommandJoinBuddyRoom                = "apphost_joinbuddyroom.php";
  public static final String CommandJoinAutoRoom                 = "apphost_joinautoroom.php";
  public static final String CommandPlayGame                     = "apphost_playgame.php";
  public static final String CommandLoginFacebook                = "apphost_sn_facebook_login.php";
  public static final String CommandResumeFacebook               = "apphost_sn_facebook_resume.php";
  public static final String CommandLogoutFacebook               = "apphost_sn_facebook_logout.php";
  public static final String CommandShowFacebookPublishDialog    = "apphost_sn_facebook_dialog_publish_show.php";
  public static final String CommandShowFacebookPermissionDialog = "apphost_sn_facebook_dialog_permission_req_show.php";
  public static final String CommandImportAddressBook            = "apphost_do_user_ab_import.php";
  public static final String CommandGetAddressBookData           = "apphost_get_user_ab_data.php";
  public static final String CommandNewBuddyRoom                 = "apphost_newbuddyroom.php";
  public static final String CommandStartRoomGame                = "apphost_start_room_game.php";
  public static final String CommandGetContext                   = "apphost_get_context.php";
  public static final String CommandGetRoomUserList              = "apphost_get_room_userlist.php";
  public static final String CommandGetGameResults               = "apphost_get_game_results.php";
  public static final String CommandLeaveRoom                    = "apphost_leaveroom.php";
  public static final String CommandImportUserPhoto              = "apphost_do_photo_import.php";
  public static final String CommandSetRoomUserStatus            = "apphost_set_room_user_status.php";
  public static final String CommandNavBarShow                   = "apphost_navbar_show.php";
  public static final String CommandNavBarHide                   = "apphost_navbar_hide.php";
  public static final String CommandScriptEval                   = "apphost_script_eval.php";
  public static final String CommandWebViewReload                = "apphost_webview_reload.php";
  public static final String CommandVarSave                      = "apphost_var_save.php"; // cannot be intercepted via mnSessionAppHostCallReceived:
  public static final String CommandVarsClear                    = "apphost_vars_clear.php"; // cannot be intercepted via mnSessionAppHostCallReceived:
  public static final String CommandVarsGet                      = "apphost_vars_get.php"; // cannot be intercepted via mnSessionAppHostCallReceived:
  public static final String CommandVoid                         = "apphost_void.php";
  public static final String CommandSetHostParam                 = "apphost_set_host_param.php"; // cannot be intercepted via mnSessionAppHostCallReceived:
  public static final String CommandPluginMessageSubscribe       = "apphost_plugin_message_subscribe.php";
  public static final String CommandPluginMessageUnSubscribe     = "apphost_plugin_message_unsubscribe.php";
  public static final String CommandPluginMessageSend            = "apphost_plugin_message_send.php";
  public static final String CommandSendHttpRequest              = "apphost_http_request.php"; // cannot be intercepted via mnSessionAppHostCallReceived:
  public static final String CommandSetGameResults               = "apphost_set_game_results.php";
  public static final String CommandExecUICommand                = "apphost_exec_ui_command.php";
  public static final String CommandAddSourceDomain              = "apphost_add_source_domain.php"; // cannot be intercepted via mnSessionAppHostCallReceived:
  public static final String CommandRemoveSourceDomain           = "apphost_remove_source_domain.php"; // cannot be intercepted via mnSessionAppHostCallReceived:
  public static final String CommandAppIsInstalledQuery          = "apphost_app_is_installed.php";
  public static final String CommandAppTryLaunch                 = "apphost_app_try_launch.php";
  public static final String CommandAppShowInMarket              = "apphost_app_show_in_market.php";

  public MNAppHostCallInfo (String             commandName,
                            Map<String,String> commandParams)
   {
    this.commandName   = commandName;
    this.commandParams = commandParams;
   }

  MNAppHostCallInfo (MNAppHostRequestURL appHostRequestUrl)
   {
    this.commandName   = MNAppHostRequestURL.REQUEST_PREFIX + appHostRequestUrl.getCmd();
    this.commandParams = appHostRequestUrl.getParams();
   }

  public String             getCommandName   ()
   {
    return commandName;
   }

  public Map<String,String> getCommandParams ()
   {
    return commandParams;
   }

  private String             commandName;
  private Map<String,String> commandParams;
 }

