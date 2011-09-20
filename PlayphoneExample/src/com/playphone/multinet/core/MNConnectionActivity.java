//
//  MNConnectionActivity.java
//  MultiNet client
//
//  Copyright 2009 PlayPhone. All rights reserved.
//

package com.playphone.multinet.core;

import java.util.Timer;
import java.util.TimerTask;

class MNConnectionActivity
 {
  public MNConnectionActivity (MNNetworkStatus  networkStatus,
                               MNSmartFoxFacade smartFoxFacade)
   {
    this.networkStatus  = networkStatus;
    this.smartFoxFacade = smartFoxFacade;

    state           = STATE_INACTIVE;
    netCheckCount   = 0;
    loginRetryCount = 0;

    timer = null;
   }

  public synchronized void start ()
   {
    if (state == STATE_INACTIVE)
     {
      state           = STATE_WAIT_NETWORK;
      netCheckCount   = 0;
      loginRetryCount = 0;

      checkNetwork();
     }
   }

  public synchronized void cancel ()
   {
    state = STATE_INACTIVE;

    cancelTimer();
   }

  public synchronized void connectionEstablished ()
   {
    cancel();
   }

  public synchronized void connectionFailed ()
   {
    if (state == STATE_WAIT_CONNECT)
     {
      if (loginRetryCount < LOGIN_RETRY_MAX_COUNT)
       {
        state = STATE_WAIT_NETWORK;

        netCheckCount = 0;

        scheduleNetworkCheck();
       }
      else
       {
        cancel();
       }
     }
    else
     {
      cancel();
     }
   }

  private void checkNetwork ()
   {
    cancelTimer();

    if (state == STATE_WAIT_NETWORK)
     {
      netCheckCount++;

      if (networkStatus.haveInternetConnection())
       {
        state = STATE_WAIT_CONNECT;

        tryConnect();
       }
      else
       {
        if (netCheckCount < NET_CHECK_MAX_COUNT)
         {
          scheduleNetworkCheck();
         }
        else
         {
          cancel();
         }
       }
     }
   }

  private void tryConnect ()
   {
    loginRetryCount++;

    smartFoxFacade.loginWithStoredLoginInfo();
   }

  private void scheduleNetworkCheck ()
   {
    cancelTimer();

    timer = new Timer();

    timer.schedule(new TimerTask()
                    {
                     public void run ()
                      {
                       checkNetwork();
                      }
                    },
                   NET_CHECK_INTERVAL);
   }

  private void cancelTimer ()
   {
    if (timer != null)
     {
      timer.cancel();
      timer.purge();

      timer = null;
     }
   }

  private int              state;
  private int              netCheckCount;
  private int              loginRetryCount;
  private MNNetworkStatus  networkStatus;
  private MNSmartFoxFacade smartFoxFacade;
  private Timer            timer;

  private static final int STATE_INACTIVE     = 0;
  private static final int STATE_WAIT_NETWORK = 1;
  private static final int STATE_WAIT_CONNECT = 2;

  private static final int NET_CHECK_MAX_COUNT   = 60;
  private static final int LOGIN_RETRY_MAX_COUNT = 5;
  private static final int NET_CHECK_INTERVAL    = 5 * 1000;
 }

