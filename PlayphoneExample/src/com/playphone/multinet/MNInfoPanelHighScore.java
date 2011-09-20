//
//  MNInfoPanelHighScore.java
//  MultiNet client
//
//  Copyright 2010 PlayPhone. All rights reserved.
//

package com.playphone.multinet;

import java.lang.ref.WeakReference;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Toast;

import com.playphone.multinet.MNDirectUIHelper.IEventHandler;
import com.playphone.multinet.core.MNSession;
import com.playphone.multinet.providers.MNMyHiScoresProvider;

public class MNInfoPanelHighScore {
	protected static WeakReference<View> binderViewRef = new WeakReference<View>(null);
	
	protected static boolean isAnimationState = false;
	
	
	static Thread animThread() {
		return new Thread(new Runnable() {
			@Override
			public void run() {
				if (Helper.panelView != null) {
					if (binderViewRef.get() != null) {
						binderViewRef.get().post(new Runnable() {
							@Override
							public void run() {
								Helper.panelView.setVisibility(View.VISIBLE);
							}
						});
					}
				}
				isAnimationState = true;
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
				}
				isAnimationState = false;
				if (Helper.panelView != null) {
					if (binderViewRef.get() != null) {
						binderViewRef.get().post(new Runnable() {
							@Override
							public void run() {
								Helper.panelView.setVisibility(View.GONE);
							}
						});
					}
				}
			}
		});
	}
	
	static class  Helper {
		private static Context context = null;
		private static View panelView = null;
		
		public synchronized static View getMyHiScorePanel () {
			if (context == null) {
				return null;
			}
			
			if (panelView == null) {
				LayoutInflater inflater = LayoutInflater.from(context);
				Resources res = context.getApplicationContext().getResources();
				int panelId = res.getIdentifier("mninfopanelhighscore", "layout",
						context.getPackageName());
				panelView = inflater.inflate(panelId, null, false);
				panelView.setVisibility(View.GONE);
			}
			
			return panelView;
		}

		private synchronized static void bindTo(ViewGroup vg) {
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
			
			vg.addView(getMyHiScorePanel());
		}

		public static void setContext(Context context) {
			Helper.context = context;
			if (context == null){
                panelView = null;
            }
		}
	}
	
	protected static void animate() {
		animThread().start();
	}
	
	protected static MNMyHiScoresProvider.IEventHandler hiScoreEvent = new MNMyHiScoresProvider.IEventHandler() {
		@Override
		public void onNewHiScore(long newScore, int gameSetId, int periodMask) {
			animate();
		}
	};
	
	protected static void install() {
		MNMyHiScoresProvider.IEventHandler hsEventHandler;
		MNMyHiScoresProvider hiScoreProvider = MNDirect.getMyHiScoresProvider();
		
		if (MNDirectPopup.isOldShowMode()) {
			hsEventHandler = hiScoreEvent;
		} else {
			hsEventHandler = hiToastScoreEvent;
		}
		
		hiScoreProvider.removeEventHandler(hsEventHandler);
		hiScoreProvider.addEventHandler(hsEventHandler);
		
		binderViewRef.get().post(new Runnable() {
			@Override
			public void run() {
				Helper.bindTo((ViewGroup) binderViewRef.get());
			}
		});
	}
	
	public static void bind(View v) {
		MNSession session = MNDirect.getSession();
		
		if (session == null) {
			Log.w("MNInfoPanelHighScore","unexpected MNSession is null");
			Helper.setContext(null);
			return;
		}
		
		binderViewRef = new WeakReference<View>(v);
		if (v == null) {
			Helper.setContext(null);
		} else {
			Helper.setContext(v.getContext());
			install();
		}
	}

	public static void bind(Activity activity) {
		if (activity == null) {
			bind((View)null);
		} else {
			Window w = activity.getWindow(); 
			
			if (w == null) {
				Log.w("MNInfoPanelHighScore",
					"unexpected calling bind panel to invisible activity");
				bind((View) null);
			}else {
				bind(w.peekDecorView());
			}
		}
	}
	
	private static IEventHandler eventHandler = null; 
	
	public static IEventHandler getDirectUIEventHandler () {
		if (eventHandler == null) {
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
		}
		return eventHandler; 
	}
	
	
	
	private static void showNotification() {
		try {
			Resources res = Helper.context.getApplicationContext().getResources();
			int panelId = res.getIdentifier("mninfopanelhighscore","layout", Helper.context.getPackageName());
	        LayoutInflater li = (LayoutInflater) Helper.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	        
	        View tv = li.inflate(panelId, null);

			Toast t = Toast.makeText(Helper.context, "High score notification",
					Toast.LENGTH_SHORT);
			t.setView(tv);
			t.setGravity(Gravity.TOP, 0, 0);
			t.show();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static void showToast() {
		try {
			binderViewRef.get().post(new Runnable() {
				@Override
				public void run() {
					showNotification();
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	protected static MNMyHiScoresProvider.IEventHandler hiToastScoreEvent = new MNMyHiScoresProvider.IEventHandler() {
		@Override
		public void onNewHiScore(long newScore, int gameSetId, int periodMask) {
			showToast();
		}
	};
}
