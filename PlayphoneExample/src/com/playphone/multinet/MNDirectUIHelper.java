//
//  MNDirectUIHelper.java
//  MultiNet client
//
//  Copyright 2010 PlayPhone. All rights reserved.
//

package com.playphone.multinet;

import java.lang.ref.WeakReference;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;

import com.playphone.multinet.core.IMNUserProfileViewEventHandler;
import com.playphone.multinet.core.MNEventHandlerArray;
import com.playphone.multinet.core.MNSession;
import com.playphone.multinet.core.MNSessionEventHandlerAbstract;
import com.playphone.multinet.core.MNUserProfileView;

public class MNDirectUIHelper {
	
	protected static boolean showOnBind = false;
	
	public static interface IEventHandler {
		public void onSetHostActivity(Activity newHostActivity);

		public void onShowDashboard();

		public void onHideDashboard();
	}

	public static class EventHandlerAbstract implements IEventHandler {
		@Override
		public void onSetHostActivity(Activity newHostActivity) {
		}

		@Override
		public void onShowDashboard() {
		}

		@Override
		public void onHideDashboard() {
		}
	}

	private static MNEventHandlerArray<IEventHandler> eventHandlers = new MNEventHandlerArray<IEventHandler>();

	/**
	 * Adds event handler
	 * 
	 * @param eventHandler
	 *            an object that implements
	 *            {@link IEventHandler IEventHandler} interface
	 */
	public static synchronized void addEventHandler(IEventHandler eventHandler) {
		eventHandlers.add(eventHandler);
	}

	/**
	 * Removes event handler
	 * 
	 * @param eventHandler
	 *            an object that implements
	 *            {@link IEventHandler IEventHandler} interface
	 */
	public static synchronized void removeEventHandler(IEventHandler eventHandler) {
		eventHandlers.remove(eventHandler);
	}
		
    protected static Dashboard dashboard = null;
    protected static WeakReference<Activity> currHostActivity = new WeakReference<Activity>(null);  

	public static synchronized Activity getHostActivity() {
		return (currHostActivity.get());
	}

	/**
	 * Set current acytive activity for UI components
	 * 
	 * @param newHostActivity
	 *            current host activity (call in Activity.onResume)
	 *            optional: you can provide null as a newHostActivity parameter
	 *            when you activity is destroying (call in Activity.onDestroy)
	 */

	public static synchronized void setHostActivity(Activity newHostActivity) {

		// Assign activity here, to allow
		currHostActivity = new WeakReference<Activity>(newHostActivity);

		MNUserProfileView view = MNDirect.getView();

		if (view != null)
		{
			view.setHostActivity(newHostActivity);
		}

		eventHandlers.beginCall();

		try {
			int index;
			int count = eventHandlers.size();

			for (index = 0; index < count; index++) {
				eventHandlers.get(index).onSetHostActivity(newHostActivity);
			}
		} finally {
			eventHandlers.endCall();
		}
	}

	protected synchronized static void onShowDashboard() {
		eventHandlers.beginCall();
		try {
			int index;
			int count = eventHandlers.size();

			for (index = 0; index < count; index++) {
				eventHandlers.get(index).onShowDashboard();
			}
		} finally {
			eventHandlers.endCall();
		}
	}
	
	protected synchronized static void onHideDashboard() {
		eventHandlers.beginCall();
		try {
			int count = eventHandlers.size();

			for (int index = 0; index < count; index++) {
				eventHandlers.get(index).onHideDashboard();
			}
		} finally {
			eventHandlers.endCall();
		}
	}

	public synchronized static void showDashboard() {
		showOnBind = true;
		
		if (dashboard != null) {
			dashboard.restoreState();
		}
	}
	
	public synchronized static void hideDashboard() {
		showOnBind = false;
		
		if (dashboard != null) {
			dashboard.restoreState();
		}
	}

	public synchronized static boolean isDashboardHidden() {
		return !isDashboardVisible();
	}

	public synchronized static boolean isDashboardVisible() {
		return (dashboard == null) ? false : dashboard.isVisible();
	}

	protected static class EventHandler extends MNSessionEventHandlerAbstract {
		@Override
		public void mnSessionDoStartGameWithParams(MNGameParams gameParams) {
			Activity a = currHostActivity.get();
			if (a != null) {
				a.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						hideDashboard();
					}
				});
			}
		}
	}
	
	protected final static EventHandler eh = new EventHandler();
	protected final static IMNUserProfileViewEventHandler viewEventHandler = new IMNUserProfileViewEventHandler() {
		@Override
		public void mnUserProfileViewDoGoBack() {
			Activity a = currHostActivity.get();
			if (a != null) {
				a.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						hideDashboard();
					}
				});
			}
		}
	};
	
	protected synchronized static void bindDashboard (Activity a) {
		final MNUserProfileView upv = MNDirect.getView();
		final MNSession s = MNDirect.getSession(); 
		if (a != null) {
			dashboard = new Dashboard(a);
			s.addEventHandler(eh);
			upv.addEventHandler(viewEventHandler);
			dashboard.restoreState();
		} else {
			if (upv != null) {
				upv.removeEventHandler(viewEventHandler);
			}
			if (s != null) {
				s.removeEventHandler(eh);
			}
			if (dashboard != null) {
				dashboard.dismiss();
			}
		}
	}

	protected static class Dashboard extends AlertDialog {
		
		public void restoreState () {
			if (showOnBind) {
				dashboard.show();
				onShowDashboard();
			} else {
				dashboard.hide();
				onHideDashboard();
			}
		}
		
		protected Dashboard(Context context) {
			super(context);
		}

		@Override
		protected void onCreate(Bundle savedInstanceState) {
			final Window w = getWindow();
			w.requestFeature(Window.FEATURE_NO_TITLE);
			w.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
	                WindowManager.LayoutParams.FLAG_FULLSCREEN );
			
			super.onCreate(savedInstanceState);
			
			w.clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM); 
			
			View contentView = MNDirect.getView();
			
			ViewGroup parentView = (ViewGroup) (contentView.getParent());

			if (parentView != null) {
				parentView.removeView(contentView);
			}
			
			setContentView(contentView);
			
			w.setLayout(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
			
			setOnKeyListener(new OnKeyListener() {
				@Override
				public boolean onKey(DialogInterface paramDialogInterface, int paramInt,
						KeyEvent paramKeyEvent) {
					if (paramKeyEvent.getKeyCode() == KeyEvent.KEYCODE_BACK) {
						hideDashboard();
						return true;
					}
					return false;
				}
			});
			
			setOnDismissListener(new OnDismissListener() {
				@Override
				public void onDismiss(DialogInterface dialog) {
					onHideDashboard();
					dashboard = null;
				}
			});
			
			setOnCancelListener(new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					showOnBind = false;
				}
			});
		}

		public boolean isVisible() {
			return isShowing();
		}
	}
}
