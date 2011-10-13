//
//  MNDirectButton.java
//  MultiNet client
//
//  Copyright 2010 PlayPhone. All rights reserved.
//

package com.playphone.multinet;

import java.lang.ref.WeakReference;

import com.playphone.multinet.MNDirectUIHelper.IEventHandler;
import com.playphone.multinet.core.MNSession;
import com.playphone.multinet.core.MNSessionEventHandlerAbstract;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.PopupWindow.OnDismissListener;

/**
 * @author playphone
 * 
 */
public class MNDirectButton  {
	
	/**
	 * Bind gravity parameter
	 */
	public static final int MNDIRECTBUTTON_TOPLEFT = Gravity.TOP  | Gravity.LEFT;
	/**
	 * Bind gravity parameter
	 */
	public static final int MNDIRECTBUTTON_TOPRIGHT = Gravity.TOP | Gravity.RIGHT;
	/**
	 * Bind gravity parameter
	 */
	public static final int MNDIRECTBUTTON_BOTTOMRIGHT = Gravity.BOTTOM | Gravity.RIGHT;
	/**
	 * Bind gravity parameter
	 */
	public static final int MNDIRECTBUTTON_BOTTOMLEFT = Gravity.BOTTOM | Gravity.LEFT;
	/**
	 * Bind gravity parameter
	 */
	public static final int MNDIRECTBUTTON_LEFT = Gravity.LEFT | Gravity.CENTER_VERTICAL;
	/**
	 * Bind gravity parameter
	 */
	public static final int MNDIRECTBUTTON_TOP = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
	/**
	 * Bind gravity parameter
	 */
	public static final int MNDIRECTBUTTON_RIGHT = Gravity.RIGHT | Gravity.CENTER_VERTICAL;
	/**
	 * Bind gravity parameter
	 */
	public static final int MNDIRECTBUTTON_BOTTOM = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;

	private static WeakReference<Activity> binderActivityRef = new WeakReference<Activity>(null);
	protected static ImageView networkStatus = null;
	protected static int visibilityMode = View.VISIBLE;
	
	protected static class Helper {
		private static Context context;
		private static WeakReference<PopupWindow> buttonPopupRef = new WeakReference<PopupWindow>(null);
		protected static View buttonView = null;
		
		protected static void setContext(Context context) {
			/* on change context clean button */
			if (Helper.context != context) {
				PopupWindow pb = buttonPopupRef.get();
				if (pb != null) {
					pb.dismiss();
					Log.d("MNDirectButton.Helper", "call button dismiss()");
				}
				buttonPopupRef.clear();
			}
			Helper.context = context;
		}

		protected static Context getContext() {
			return context;
		}
		

		protected  static PopupWindow getButton() {
			if (context == null) {
				return null;
			}
			
			PopupWindow pb = buttonPopupRef.get();
			
			if (pb != null) {
				return pb;
			}
			
			synchronized (context) {
				if (buttonPopupRef.get() == null) {
					Log.d("MNDirectButton", "getButton() -- create new PopupView");
					LayoutInflater inflater = LayoutInflater.from(context);
					Resources res = context.getApplicationContext()
							.getResources();
					int buttonId = res.getIdentifier("mndirectbutton",
							"layout", context.getPackageName());

					buttonView = inflater.inflate(buttonId, null, false);
					networkStatus = (ImageView) Helper.buttonView
							.findViewById(res.getIdentifier("mnbuttonimg",
									"id", Helper.getContext().getPackageName()));

					pb = new PopupWindow(buttonView,
							ViewGroup.LayoutParams.WRAP_CONTENT,
							ViewGroup.LayoutParams.WRAP_CONTENT, false);
					pb.getContentView().setVisibility(visibilityMode);

					pb.setTouchable(true);
					// Workaround for working onTouch
					pb.setBackgroundDrawable(new BitmapDrawable());
					pb.setTouchInterceptor(new OnTouchListener() {
						@Override
						public boolean onTouch(final View v, MotionEvent event) {
							if (event.getAction() == MotionEvent.ACTION_UP) {
								MNDirectUIHelper.showDashboard();
							}
							return true;
						}
					});

					pb.setOnDismissListener(new OnDismissListener() {
						@Override
						public void onDismiss() {
							Log.d("MNDirectButton", "onDismiss()");
							buttonPopupRef.clear();
							Helper.context = null;
						}
					});
					buttonPopupRef = new WeakReference<PopupWindow>(pb);
				}
			}
			
			Log.d("MNDirectButton", "getButton() -- " + buttonPopupRef.get());
			return buttonPopupRef.get();
		}
	}
	
	private static int lastImgId = 0;

	protected static class EventHandler extends MNSessionEventHandlerAbstract {
		@Override
		public void mnSessionStatusChanged(final int newStatus, final int oldStatus) {
			Activity ba = binderActivityRef.get();
			
			if (ba == null) {
				return;
			}
			
			ba.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (MNDirect.getSession() == null) {
						return;
					}
					
					int newImgId = getIdOfButtonPic(newStatus, MNDirect
							.getSession().getMyUserId());
					if (lastImgId != newImgId) {
						networkStatus.setImageResource(newImgId);
						lastImgId = newImgId;
					}
				}
			});
		}
	}
	
	private static String getNamePicByPos() {
		StringBuffer result = new StringBuffer("mn_direct_button_");

		if (MNDIRECTBUTTON_TOPLEFT == defaultLocation) {
			result.append("tl");
		} else if (MNDIRECTBUTTON_TOPRIGHT == defaultLocation) {
			result.append("tr");
		} else if (MNDIRECTBUTTON_BOTTOMRIGHT == defaultLocation) {
			result.append("br");
		} else if (MNDIRECTBUTTON_BOTTOMLEFT == defaultLocation) {
			result.append("bl");
		} else if (MNDIRECTBUTTON_LEFT == defaultLocation) {
			result.append("ml");
		} else if (MNDIRECTBUTTON_TOP == defaultLocation) {
			result.append("tc");
		} else if (MNDIRECTBUTTON_RIGHT == defaultLocation) {
			result.append("mr");
		} else if (MNDIRECTBUTTON_BOTTOM == defaultLocation) {
			result.append("bc");
		}
		
		return result.toString();
	}
	
	private static int getIdOfButtonPic(int status, long userId) {
		final Context context = Helper.getContext();
		if (context == null) {
			return 0;
		}

		StringBuffer fileName = new StringBuffer(getNamePicByPos());
		StringBuffer result = new StringBuffer(fileName.toString());

		if (userId == MNConst.MN_USER_ID_UNDEFINED) {
			result.append("_ns");
		} else {
			if (status == MNConst.MN_OFFLINE) {
				result.append("_ou");
			} else if (status >= MNConst.MN_LOGGEDIN) {
				result.append("_au");
			}
		}

		final Resources res = context.getApplicationContext()
				.getResources();

		int id = res.getIdentifier(result.toString(), "drawable",
				context.getPackageName());
		if (id == 0) { // not such pic
			id = res.getIdentifier(fileName.toString(), "drawable",
					context.getPackageName());
		}

		return id;
	}
	
	private static EventHandler sessionEventHandler = new EventHandler();
	
	/**
	 * @param v
	 */
	protected static volatile int mBindCount = 0;

	protected synchronized static void bind(final Activity activity, final int gravity) {
		lastImgId = 0; // drop button image cash
		Log.d("MNDirectButton", "bind Activity " + activity);
		if (activity == null) {
			Helper.setContext(null);
		} else {
			binderActivityRef = new WeakReference<Activity>(activity);
			mBindCount++;
			Helper.setContext(activity);
			final PopupWindow btn = Helper.getButton();
			final MNSession session = MNDirect.getSession();
			
			if (btn != null) {
				session.removeEventHandler(sessionEventHandler);
				session.addEventHandler(sessionEventHandler);
			}
			final View bindView = activity.getWindow().peekDecorView();
			
			bindView.post(new Runnable() {
				@Override
				public void run() {
					if (mBindCount > 1) {
						mBindCount--;
						return;
					}
					
					Log.d("MNDirectButton", "hostView.post() run "+activity+" btn "+btn);

					if ((btn == null) || (activity == null)) {
						return;
					}
					
					if (isHidden() || MNDirectUIHelper.isDashboardVisible()) {
						btn.getContentView().setVisibility(View.GONE);
					}

					
					if (session != null) {
						int newImgId = getIdOfButtonPic(session.getStatus(),
								session.getMyUserId());

						if (lastImgId != newImgId) {
							networkStatus.setImageResource(newImgId);
							lastImgId = newImgId;
						}
					}
					

					btn.showAtLocation(bindView, gravity, 0, 0);
					
					mBindCount = 0;
				}
			});
		}
		
		MNDirectUIHelper.bindDashboard(activity);	
	}
	
	public static boolean isVisible () {
		return (visibilityMode == View.VISIBLE);
	}

	public static boolean isHidden () {
		return (!isVisible ());
	}
	
	private static int defaultLocation = MNDirectButton.MNDIRECTBUTTON_TOPRIGHT;
	private static boolean isInited  = false;
	
	/**
	 * @param isInited the isInited to set
	 */
	public static void setInited(boolean isInited) {
		MNDirectButton.isInited = isInited;
	}

	public synchronized static void initWithLocation(int location) {
		defaultLocation = location;
		if (!isInited) {
			MNDirectUIHelper.addEventHandler(eventHandler);
			isInited = true;
		}
	}

	private static void setVisibility(boolean visibility) {
		if (Helper.context == null) {
			return;
		}
		
		final PopupWindow pw = Helper.getButton();
		final int visibilityMode = (visibility) ? View.VISIBLE : View.GONE;
		final Activity ba = binderActivityRef.get();
		
		if ((pw != null) && (ba != null)) {
			ba.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					pw.getContentView().setVisibility(visibilityMode);
				}
			});
		}
	}

	/**
	 * 
	 */
	public synchronized static void show() {
		visibilityMode = View.VISIBLE;
		if (MNDirectUIHelper.isDashboardHidden()) {
			setVisibility(true);
		}
	}

	/**
	 * 
	 */
	public synchronized static void hide() {
		visibilityMode = View.GONE;
		setVisibility(false);
	}
	
	public static IEventHandler eventHandler = new IEventHandler() {
		@Override
		public void onSetHostActivity(Activity newHostActivity) {
			bind(newHostActivity, defaultLocation); 
		}

		@Override
		public void onShowDashboard() {
			if (!isHidden()) {
				setVisibility(false);
			}
		}

		@Override
		public void onHideDashboard() {
			if (!isHidden()) {
				setVisibility(true);
			}
		}
	};
}
