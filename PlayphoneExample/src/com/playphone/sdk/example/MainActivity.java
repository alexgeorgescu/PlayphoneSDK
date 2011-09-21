package com.playphone.sdk.example;


import com.playphone.multinet.*;
import com.playphone.multinet.core.MNSession;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
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
					@Override public String toString() { return "Current User"; }
					@Override public void run()        { startActivity(new Intent(MainActivity.this, PlayphoneExampleActivity.class));/*Toast.makeText(getApplicationContext(), "My username: " + MNDirect.getSession().getMyUserName(), Toast.LENGTH_LONG);*/ }
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
                    @Override public void run() 	{ startActivity(new Intent(MainActivity.this, PlayphoneExampleActivity.class));}
                },
				new Entry() {
					@Override public String toString() { return "Logout of Playphone"; }
					@Override public void run()        { if(MNDirect.isUserLoggedIn()) MNDirect.getSession().logout(); 
														MNDirectUIHelper.showDashboard();}
				},
				new Entry() {
					@Override public String toString() { return "Logout of Playphone (wipe credentials)"; }
					@Override public void run()        { if(MNDirect.isUserLoggedIn()) MNDirect.getSession().logoutAndWipeUserCredentialsByMode(MNSession.MN_CREDENTIALS_WIPE_USER);
														MNDirectUIHelper.showDashboard();}
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
		
		MNDirect.init(this._GAMEID, this._APISECRET,
				new MNEventHandler(), this);
		
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
    	 
		@Override
		public void mnDirectDoStartGameWithParams(MNGameParams params) {
			startActivity(new Intent(MainActivity.this, PlayphoneExampleActivity.class));
		}
	}
}
