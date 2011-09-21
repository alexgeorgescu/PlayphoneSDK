package com.playphone.sdk.example;


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
	
	
	public static MNEventHandler getMNEventHandler(){return eventHandler;}
	
	protected interface Entry {
		public String toString();
		public void run();
	}

	protected Entry[] getEntries() {
		return new Entry[] {
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
                    @Override public String toString() { return "VirtualItem Basics"; }
                    @Override public void run() 	{ startActivity(new Intent(MainActivity.this, VirtualItemsActivity.class));}
                },
                new Entry() {
                    @Override public String toString() { return "MultiPlayer Basics"; }
                    @Override public void run() 	{ 
                    									/*startActivity(new Intent(MainActivity.this, MultiPlayerActivity.class));*/
                    									MNDirect.execAppCommand("jumpToUserHome",null);
                    									MNDirectUIHelper.showDashboard();
														Toast.makeText(MainActivity.this, "Click \"Play Now\" to join a multiplayer room and send messages", Toast.LENGTH_LONG).show();}
                },
				new Entry() {
					@Override public String toString() { return "Logout from Playphone"; }
					@Override public void run()        { startActivity(new Intent(MainActivity.this, LogoutActivity.class));}
				}
		};
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

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

		MNDirectButton.initWithLocation(MNDirectButton.MNDIRECTBUTTON_BOTTOMRIGHT);
		
		MNDirectPopup.init(MNDirectPopup.MNDIRECTPOPUP_ALL);
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
		}
	}
}
