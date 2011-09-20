//
//  MNSocNetSessionFB.java
//  MultiNet client
//
//  Copyright 2009 PlayPhone. All rights reserved.
//

package com.playphone.multinet.core;

import java.net.MalformedURLException;
import java.io.IOException;

import android.content.Context;

import com.facebook.android.Facebook;

public class MNSocNetSessionFB
 {
  public static final int USER_ID_UNDEFINED = -1;
  public static final int SOCNET_ID = 1;

  public MNSocNetSessionFB (IMNPlatform platform,IEventHandler eventHandler)
   {
    this.platform     = (MNPlatformAndroid)platform;
    this.eventHandler = eventHandler;
    this.connecting   = false;
    this.facebook     = null;
   }

  public synchronized String connect (String   applicationId,
                                      String[] permissions)
   {
    if (connecting)
     {
      return MNI18n.getLocalizedString
              ("Facebook connection already have been initiated",
               MNI18n.MESSAGE_CODE_FACEBOOK_CONNECTION_ALREADY_INITIATED_ERROR);
     }

    connecting = true;

    facebook = new Facebook(applicationId);

    MNSocNetSessionFBUI.authorize(platform.getContext(),
                                  facebook,
                                  permissions,
                                  new MNSocNetSessionFBUI.IFBDialogEventHandler()
     {
      public void onSuccess ()
       {
        connecting = false;
        eventHandler.socNetFBLoginOk(MNSocNetSessionFB.this);
       }

      public void onError   (String message)
       {
        connecting = false;
        eventHandler.socNetFBLoginFailedWithError("Facebook connection failed (" + message + ")");
       }

      public void onCancel  ()
       {
        connecting = false;
        eventHandler.socNetFBLoginCanceled();
       }
     });

    return null;
   }

  public synchronized String resume (String applicationId)
   {
    return connect(applicationId,null);
   }

  public synchronized void logout ()
   {
    if (facebook != null)
     {
      // logout call is a blocking call, so we have to run it on separate
      // thread to prevent ANR
      final Facebook fb      = facebook;
      final Context  context = platform.getContext();

      (new Thread()
        {
         public void run ()
          {
           try
            {
             fb.logout(context);
            }
           catch (MalformedURLException e)
            {
            }
           catch (IOException e)
            {
            }
          }
        }).start();

      facebook = null;
     }

    connecting = false;
   }

  public synchronized boolean isConnected ()
   {
    return facebook != null && facebook.isSessionValid();
   }

  public Facebook getFBConnect ()
   {
    return facebook;
   }

  public long getUserId ()
   {
    return USER_ID_UNDEFINED;
   }

  public synchronized String getSessionKey ()
   {
    if (isConnected())
     {
      return "";
     }
    else
     {
      return null;
     }
   }

  public synchronized String getSessionSecret ()
   {
    if (isConnected())
     {
      return facebook.getAccessToken();
     }
    else
     {
      return null;
     }
   }

  public synchronized boolean didUserStoreCredentials ()
   {
    return isConnected() && facebook.getAccessExpires() != 0;
   }

  public synchronized String showStreamDialog (String prompt,
                                               String attachment,
                                               String targetId,
                                               String actionLinks,
                                               final IStreamDialogEventHandler eventHandler)
   {
    if (isConnected())
     {
      MNSocNetSessionFBUI.publish(platform.getContext(),facebook,
                                  prompt,attachment,targetId,actionLinks,
                                  new MNSocNetSessionFBUI.IFBDialogEventHandler()
       {
        public void onSuccess ()
         {
          eventHandler.socNetFBStreamDialogOk();
         }

        public void onError   (String message)
         {
          eventHandler.socNetFBStreamDialogFailedWithError(message);
         }

        public void onCancel  ()
         {
          eventHandler.socNetFBStreamDialogCanceled();
         }
       });
     }

    return null;
   }

  public synchronized String showPermissionDialog (String permission,
                                                   final IPermissionDialogEventHandler eventHandler)
   {
    if (isConnected())
     {
      MNSocNetSessionFBUI.askPermissions(platform.getContext(),facebook,
                                         permission,
                                         new MNSocNetSessionFBUI.IFBDialogEventHandler()
       {
        public void onSuccess ()
         {
          eventHandler.socNetFBPermissionDialogOk();
         }

        public void onError   (String message)
         {
          eventHandler.socNetFBPermissionDialogFailedWithError(message);
         }

        public void onCancel  ()
         {
          eventHandler.socNetFBPermissionDialogCanceled();
         }
       });
     }

    return null;
   }

  interface IEventHandler
   {
    void socNetFBLoginOk              (MNSocNetSessionFB session);
    void socNetFBLoginCanceled        ();
    void socNetFBLoginFailedWithError (String            error);
    void socNetFBLoggedOut            ();
   }

  interface IStreamDialogEventHandler
   {
    void socNetFBStreamDialogOk                 ();
    void socNetFBStreamDialogCanceled           ();
    void socNetFBStreamDialogFailedWithError    (String    error);
   }

  interface IPermissionDialogEventHandler
   {
    void socNetFBPermissionDialogOk             ();
    void socNetFBPermissionDialogCanceled       ();
    void socNetFBPermissionDialogFailedWithError(String    error);
   }

  Facebook getFacebook ()
   {
    return facebook;
   }

  MNPlatformAndroid platform;
  private IEventHandler eventHandler;
  private Facebook facebook;
  private boolean connecting;
 }

