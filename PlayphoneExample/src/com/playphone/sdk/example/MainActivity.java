package com.playphone.sdk.example;


import java.util.List;

import com.playphone.multinet.*;
import com.playphone.multinet.core.MNSession;

import android.app.ActivityManager;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Toast;


public class MainActivity extends ListActivity {

	// application specific information
	private int _GAMEID = MyPlayphoneCredentials._GAMEID;
	private String _APISECRET = MyPlayphoneCredentials._APISECRET;
	private static MNEventHandler eventHandler = null;
	private String TAB = "       ";
	
	public static MNEventHandler getMNEventHandler(){return eventHandler;}
	
	protected interface Entry {
		public String toString();
		public void run();
	}

	protected Entry[] getEntries() {
		return new Entry[] {
				new Entry() {
					@Override public String toString() { return "1. Required Integration"; }
					@Override public void run()        { }
				},
				new Entry() {
					@Override public String toString() { return TAB + "Login User"; }
					@Override public void run()        { startActivity(new Intent(MainActivity.this, LoginUserActivity.class));}
				},
				new Entry() {
					@Override public String toString() { return TAB + "Dashboard"; }
					@Override public void run()        { startActivity(new Intent(MainActivity.this, DashboardPageActivity.class));}
				},
				new Entry() {
                    @Override public String toString() { return TAB + "Virtual Economy"; }
                    @Override public void run() 	{ startActivity(new Intent(MainActivity.this, VirtualEconomyListActivity.class));}
                },
                new Entry() {
					@Override public String toString() { return "2. Advanced Features"; }
					@Override public void run()        { }
				},
				new Entry() {
					@Override public String toString() { return TAB + "Current User Info"; }
					@Override public void run()        { startActivity(new Intent(MainActivity.this, CurrentUserInfoActivity.class));}
				},
				new Entry() {
					@Override public String toString() { return TAB + "Leaderboards"; }
					@Override public void run()        { startActivity(new Intent(MainActivity.this, PostScoreActivity.class));	}
				},
				new Entry() {
					@Override public String toString() { return TAB + "Achievements"; }
					@Override public void run()        { startActivity(new Intent(MainActivity.this, PostAchievementActivity.class));}
				},
				new Entry() {
					@Override public String toString() { return TAB + "Social Graph"; }
					@Override public void run()        { startActivity(new Intent(MainActivity.this, SocialGraphActivity.class));}
				},
				new Entry() {
					@Override public String toString() { return TAB + "Dashboard Control"; }
					@Override public void run()        { startActivity(new Intent(MainActivity.this, DashboardControlActivity.class));	}
				},
				new Entry() {
					@Override public String toString() { return TAB + "Notifications"; }
					@Override public void run()        { startActivity(new Intent(MainActivity.this, NotificationPanelActivity.class));	}
				},
				new Entry() {
					@Override public String toString() { return TAB + "Cloud Storage"; }
					@Override public void run()        { { startActivity(new Intent(MainActivity.this, PostCloudStorageActivity.class)); }
														}
				},
				new Entry() {
					@Override public String toString() { return TAB + "Settings"; }
					@Override public void run()        { MNDirect.execAppCommand("jumpToBuddyList",null); 
														MNDirectUIHelper.showDashboard();
														}
				},
				new Entry() {
					@Override public String toString() { return TAB + "Multiplayer Basics"; }
					@Override public void run()        { 
						//startActivity(new Intent(MainActivity.this, MultiPlayerActivity.class));
						MNDirect.execAppCommand("jumpToUserHome",null);
						MNDirectUIHelper.showDashboard();
						Toast.makeText(MainActivity.this, "Click \"Play Now\" to join a multiplayer room and send messages", Toast.LENGTH_LONG).show();}
														
				}
				/*
				new Entry() {
					@Override public String toString() { return "Dashboard"; }
					@Override public void run()        { MNDirect.execAppCommand("jumpToUserProfile",null);
														MNDirectUIHelper.showDashboard();
														}
				},
				new Entry() {
					@Override public String toString() { return "Dashboard: Friends"; }
					@Override public void run()        { MNDirect.execAppCommand("jumpToBuddyList",null); 
														MNDirectUIHelper.showDashboard();
														}
				},
				new Entry() {
					@Override public String toString() { return "Dashboard: Leaderboards"; }
					@Override public void run()        { MNDirect.execAppCommand("jumpToLeaderboard",null); 
														MNDirectUIHelper.showDashboard();
														}
				},
				new Entry() {
					@Override public String toString() { return "Dashboard: Achievements"; }
					@Override public void run()        { MNDirect.execAppCommand("jumpToAchievements",null); 
														MNDirectUIHelper.showDashboard();
														}
				},
				new Entry() {
					@Override public String toString() { return "Post score in Leaderboard"; }
					@Override public void run()        { startActivity(new Intent(MainActivity.this, PostScoreActivity.class)); }
				},
				new Entry() {
					@Override public String toString() { return "Unlock an Achievement"; }
					@Override public void run()        { startActivity(new Intent(MainActivity.this, PostAchievementActivity.class)); }
				},
				new Entry() {
					@Override public String toString() { return "Current User Info"; }
					@Override public void run()        { startActivity(new Intent(MainActivity.this, CurrentUserInfoActivity.class));}
				},
				new Entry() {
					@Override public String toString() { return "Cloud Storage basics"; }
					@Override public void run()        { startActivity(new Intent(MainActivity.this, PostCloudStorageActivity.class)); }
				},
				new Entry() {
					@Override public String toString() { return "Purchase and manage potions (sample in-game item)"; }
					@Override public void run()        { startActivity(new Intent(MainActivity.this, BuyItemsActivity.class)); }
				},
				new Entry() {
                    @Override public String toString() { return "VirtualItem Basics (OBSOLETE)"; }
                    @Override public void run() 	{ startActivity(new Intent(MainActivity.this, VirtualItemsActivity.class));}
                },
                new Entry() {
                    @Override public String toString() { return "Virtual Economy"; }
                    @Override public void run() 	{ startActivity(new Intent(MainActivity.this, VirtualEconomyListActivity.class));}
                },
                new Entry() {
                    @Override public String toString() { return "MultiPlayer Basics"; }
                    @Override public void run() 	{ 
                    									//startActivity(new Intent(MainActivity.this, MultiPlayerActivity.class));
                    									MNDirect.execAppCommand("jumpToUserHome",null);
                    									MNDirectUIHelper.showDashboard();
														Toast.makeText(MainActivity.this, "Click \"Play Now\" to join a multiplayer room and send messages", Toast.LENGTH_LONG).show();}
                },
				new Entry() {
					@Override public String toString() { return "Logout from Playphone"; }
					@Override public void run()        { startActivity(new Intent(MainActivity.this, LogoutActivity.class));}
				}*/
		};
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.d("playphone","onCreate has been called for MainActivity");
		super.onCreate(savedInstanceState);
		
		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		
		final Entry[] entries = getEntries();
		setListAdapter(new ArrayAdapter<Entry>(this, R.layout.main_menu_item, entries));
		
		ListView lv = getListView();
		lv.setOnItemClickListener(new OnItemClickListener() {

            @Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				entries[position].run();
			}
		});
		
		eventHandler = new MNEventHandler();
		MNDirect.init(this._GAMEID, this._APISECRET,
			eventHandler, this);
		MNDirect.handleApplicationIntent(getIntent());
		MNDirectButton.initWithLocation(MNDirectButton.MNDIRECTBUTTON_TOPLEFT);
		MNDirectPopup.init(MNDirectPopup.MNDIRECTPOPUP_ALL);
		
		
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);
	}

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MNDirect.shutdownSession();
    };

    @Override
    protected void onResume() {
        super.onResume();
        MNDirectUIHelper.setHostActivity(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        MNDirectUIHelper.setHostActivity(null);
    }
    
    protected class MNEventHandler extends MNDirectEventHandlerAbstract {
    	 
		private Handler handler;
		
		public Handler getHandler() {
			return handler;
		}

		public void setHandler(Handler handler) {
			this.handler = handler;
		}

		@Override
		public void mnDirectDoStartGameWithParams(MNGameParams params) {
			startActivity(new Intent(MainActivity.this, MultiPlayerActivity.class));
		}
		
		
		
		@Override
		public void mnDirectDidReceiveGameMessage(String message,MNUserInfo sender)
		{
			Log.d("playphone","Received message: " + message);
			if(sender != null)
			Toast.makeText(getApplicationContext(), "User " + sender.userName + 
					" sent message: " +  message, Toast.LENGTH_LONG).show();
		}
		
		@Override
		public void mnDirectSessionStatusChanged(int newStatus)
		{
			if(handler != null)
			{
				Message msg = new Message();
				Bundle bundle = new Bundle();
				bundle.putInt("statusChange", newStatus);
				msg.setData(bundle); 
				handler.sendMessage(msg);
			}
			Log.d("playphone","The new status is " + newStatus);
			
			//notify the login activity if it is the top activity
			//detectActivityOnTop(LoginUserActivity.class.getCanonicalName());
			/*
			Intent intent = new Intent(MainActivity.this, LoginUserActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
			Bundle bundle = new Bundle();
			bundle.putInt("loginStatus", newStatus);
			intent.putExtras(bundle);
			startActivity(intent);
			*/
		}
		
		
	}
    
    
    private boolean detectActivityOnTop(String classname){
		Log.d("playphone","Check for classname: " + classname);
    	// get a list of running processes and iterate through them
	    ActivityManager am = (ActivityManager) this
	                .getSystemService(ACTIVITY_SERVICE);
	 
	    // get the info from the currently running task
	        List<ActivityManager.RunningTaskInfo> taskInfo = am.getRunningTasks(1);
	 
	        Log.d("playphone", "CURRENT Activity :"
	                + taskInfo.get(0).topActivity.getClassName());
	        return false;

    }
}
