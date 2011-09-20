//
//  MNInfoPanelNetwork.java
//  MultiNet client
//
//  Copyright 2010 PlayPhone. All rights reserved.
//

package com.playphone.multinet;


import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;

import com.playphone.multinet.core.IMNSessionEventHandler;
import com.playphone.multinet.core.MNSession;
import com.playphone.multinet.core.MNSessionEventHandlerAbstract;
import com.playphone.multinet.core.MNURLDownloader;
import com.playphone.multinet.MNDirectUIHelper.IEventHandler;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.ConditionVariable;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.conn.ssl.AllowAllHostnameVerifier;

public class MNInfoPanelNetwork {
	
	protected static final int WAIT_DELAY_AVATAR_LOAD = 10 * 1000;
	protected static final int PANEL_SHOW_TIME_IN_MILS = 4 * 1000;
	
	protected static WeakReference<View> binderViewRef = new WeakReference<View>(null);
	protected static Bitmap ourAvatarBitmap = null;
	protected static Long ourId = MNConst.MN_USER_ID_UNDEFINED;
	protected static Object loadAvatarLock = new Object();
	protected static final Object animationShowLock = new Object();
	protected static Boolean isAnimationShow = false;
	protected static boolean isAnimationState = false;	
	
	protected static void setOurAvatarBitmap(Bitmap avatarBitmap) {
		ourAvatarBitmap = avatarBitmap;
	}

	private static class ImageDownloader extends MNURLDownloader implements
			MNURLDownloader.IErrorEventHandler {
		public ConditionVariable lock = new ConditionVariable(); 
		
		public void loadURL(String url) {
			setHttpsHostnameVerifier(new AllowAllHostnameVerifier());
			super.loadURL(url, null, this);
		}

		public void downloaderLoadFailed(MNURLDownloader downloader,
				ErrorInfo errorInfo) {
			setOurAvatarBitmap(null);
			lock.open();
		}

		protected void readData(InputStream inputStream) throws IOException {
			setOurAvatarBitmap(BitmapFactory.decodeStream(inputStream));
			lock.open();
		}
	}
	
	protected static void animate() {
		new Thread(new Runnable() {
			final View panel    = Helper.panelView;
			final View hostView = binderViewRef.get();
			
			private void fillPanel(String message) {
				try {
					TextView welcome = (TextView) panel
					.findViewById(Helper.welcomeId);
					
					welcome.setText(message);
					
					if (ourAvatarBitmap != null) {
						ImageView avatar = (ImageView) panel
								.findViewById(Helper.avatarId);
						avatar.setImageBitmap(ourAvatarBitmap);
					}
				} catch (Exception e) {
				}
			}
			
			private void showPanel(final String message) {
				try {
					hostView.post(new Runnable() {
						@Override
						public void run() {
							fillPanel(message);
							panel.setVisibility(View.VISIBLE);
						}
					});
					
				} catch (Exception e) {
				}
			}
			
			private void hidePanel () {
				try {
					hostView.post(new Runnable() {
						@Override
						public void run() {
							panel.setVisibility(View.GONE);
						}
					});
					
				} catch (Exception e) {
				}
			}
			
			@Override
			public void run() {
				final MNSession session = MNDirect.getSession();
				final String welcomeMsg;
				
				isAnimationState = true;

				if ((session != null) && (session.getMyUserName() != null)) {
					welcomeMsg = Helper.welcomeTemplate.replace("{0}",
							session.getMyUserName());
				} else {
					welcomeMsg = Helper.welcomeTemplate.replace("{0}",".");
				}

				if ((ourAvatarBitmap == null)
						|| (ourId != session.getMyUserId())) {
					synchronized (loadAvatarLock) {
						if ((ourAvatarBitmap == null)
								|| (ourId != session.getMyUserId())) {
							ImageDownloader downloader = new ImageDownloader();

							downloader.loadURL(session.getMyUserInfo()
									.getAvatarUrl());

							downloader.lock.block(WAIT_DELAY_AVATAR_LOAD);
							ourId = session.getMyUserId();
						}
					}
				}

				synchronized (animationShowLock) {
					isAnimationShow = true;
					long showToTime = System.currentTimeMillis() + PANEL_SHOW_TIME_IN_MILS;
					showPanel(welcomeMsg);
				
					while (isAnimationShow) {
						if (showToTime <= System.currentTimeMillis()) {
							isAnimationShow = false;
							isAnimationState = false;
						}
						try {
							Thread.sleep(10);
						} catch (InterruptedException e) {
							isAnimationShow = false;
							isAnimationState = false;
						}
					}
					
					hidePanel();
				}
			}
		}).start();
	}

	static class Helper {
		private static Context context = null;
		private static View panelView = null;
		private static TextView welcomeLabel = null;
		private static String welcomeTemplate = null;
//		private static ImageView avatar = null;
		private static int welcomeId;
		private static int avatarId;
		

		public static View getNetworkPanel() {
			if (context == null) {
				return null;
			}
			
			if (panelView != null) {
				return panelView;
			}
			
			synchronized (Helper.class) {
				if (panelView == null) {
					LayoutInflater inflater = LayoutInflater.from(context);
					Resources res = context.getApplicationContext()
							.getResources();
					int panelId = res.getIdentifier("mninfopanelnetwork",
							"layout", context.getPackageName());
					welcomeId = res.getIdentifier("mndirect_popup_text","id", context.getPackageName());
					avatarId = res.getIdentifier("mndirect_popup_icon","id", context.getPackageName());
					panelView = inflater.inflate(panelId, null);
					welcomeLabel = (TextView) panelView.findViewById(welcomeId);
					welcomeTemplate = welcomeLabel.getText().toString();
					panelView.setVisibility(View.GONE);
				}
			}

			return panelView;
		}

		public synchronized static void bindTo(ViewGroup vg) {
			if (panelView != null) {
				ViewGroup parentVG = (ViewGroup) (panelView.getParent());

				if (parentVG != null) {
					parentVG.removeView(panelView);
				}
				
				if (isAnimationState) {
					panelView.setVisibility(View.VISIBLE);
				} else {
					panelView.setVisibility(View.GONE);
				}
			}
			
			vg.addView(getNetworkPanel());
		}

		public static void setContext(Context context) {
			Helper.context = context;
			if (context == null){
                panelView = null;
                welcomeLabel = null;
            }
		}
	}
	
	private static class EventHandler extends MNSessionEventHandlerAbstract {
		protected static long oldUserId = MNConst.MN_USER_ID_UNDEFINED;
		@Override
		public void mnSessionUserChanged(long userId) {
			if ((userId != oldUserId) && 
					(userId != MNConst.MN_USER_ID_UNDEFINED)) {
				animate();
			}
			oldUserId  = userId;
		}
	}

	protected static void install() {
		binderViewRef.get().post(new Runnable() {
			@Override
			public void run() {
				Helper.bindTo((ViewGroup) binderViewRef.get());
			}
		});
	}
	
	private static IMNSessionEventHandler sessionHandler = null;
	
	public static void bind(View v) {
		MNSession session = MNDirect.getSession();
		
		if (session == null) {
			Log.w("MNInfoPanelNetwork","unexpected MNSession is null");
			Helper.setContext(null);
			return;
		}
		
		if (sessionHandler == null)	{
			if (MNDirectPopup.isOldShowMode()) {
			  sessionHandler = new EventHandler();
			} else {
              sessionHandler = new ToastEventHandler(); 
			}
		}
		
		binderViewRef = new WeakReference<View>(v);
		
		if (v == null) {
			isAnimationShow = false;
			Helper.setContext(null);
			session.removeEventHandler(sessionHandler);
		} else {
			Helper.setContext(v.getContext());
			install();
			session.addEventHandler(sessionHandler);
			sessionHandler = null;
			if (isAnimationState) {
				animate();
			}
		}
	}

	public static void bind(Activity activity) {
		if (activity == null) {
			bind((View)null);
		} else {
			Window w = activity.getWindow();
			if (w == null) {
				Log.w("MNInfoPanelNetwork",
						"unexpected calling bind panel to invisible activity");
				bind((View) null);
			} else {
				bind(w.peekDecorView());
			}
		}
	}
	
	private static IEventHandler eventHandler = null; 
	
	public static IEventHandler getDirectUIEventHandler() {
		if (eventHandler != null) {
			return eventHandler;
		}
		eventHandler = new IEventHandler() {
			@Override
			public void onSetHostActivity(Activity newHostActivity) {
				bind(newHostActivity);
			}
			@Override
			public void onShowDashboard() {}
			@Override
			public void onHideDashboard() {}
		};
		return eventHandler;
	}
	
    // Popup by toast notification
	private static void loadAvatar(final MNUserInfo currUserInfo) {
		
		final String currAvatarUrl = currUserInfo.getAvatarUrl();
		if (currAvatarUrl == null) {
			Log.w("loadAvatar", "current user avatar is null");
			return;
		}
		
		synchronized (loadAvatarLock) {
			if ((ourAvatarBitmap == null) || (ourId != currUserInfo.userId)) {
				ImageDownloader downloader = new ImageDownloader();

				downloader.loadURL(currAvatarUrl);

				downloader.lock.block(WAIT_DELAY_AVATAR_LOAD);
				ourId = currUserInfo.userId;
			}
		}
	}
	
	private static void fillNotificationView(final View v, final MNUserInfo userInfo) {
		String userName = userInfo.userName;
		if (userName == null) {
			userName = "";
		}
		try {
		final String welcomeMsg = Helper.welcomeTemplate.replace("{0}",
				userName);

			TextView welcome = (TextView) v.findViewById(Helper.welcomeId);
			
			welcome.setText(welcomeMsg);
			
			if (ourAvatarBitmap != null) {
				ImageView avatar = (ImageView) v
						.findViewById(Helper.avatarId);
				avatar.setImageBitmap(ourAvatarBitmap);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static void showNotification(final MNUserInfo userInfo) {
		try {
			Resources res = Helper.context.getApplicationContext().getResources();
			int panelId = res.getIdentifier("mninfopanelnetwork","layout", Helper.context.getPackageName());
	        LayoutInflater li = (LayoutInflater) Helper.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			// View tv = Helper.getNetworkPanel();
	        View tv = li.inflate(panelId, null);

			fillNotificationView(tv, userInfo);

			Toast t = Toast.makeText(Helper.context, "Network notification",
					Toast.LENGTH_LONG);
			t.setView(tv);
			t.setGravity(Gravity.TOP, 0, 0);
			t.show();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static void showToast() {
		try {
			MNSession session = MNDirect.getSession();
			if (session == null) {
				Log.e("MNInfoPanelNetwork", "session on show popup is null");
				return;
			}
			final MNUserInfo userInfo = session.getMyUserInfo();
			if (userInfo == null) {
				Log.e("MNInfoPanelNetwork", "user info on show popup is null");
				return;
			}

			binderViewRef.get().post(new Runnable() {
				@Override
				public void run() {
					loadAvatar(userInfo);
					showNotification(userInfo);
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static class ToastEventHandler extends
			MNSessionEventHandlerAbstract {
		protected static long oldUserId = MNConst.MN_USER_ID_UNDEFINED;
		@Override
		public void mnSessionUserChanged(long userId) {
			if ((userId != oldUserId) && 
					(userId != MNConst.MN_USER_ID_UNDEFINED)) {
				showToast();
			}
			oldUserId  = userId;
		}
	}
}
