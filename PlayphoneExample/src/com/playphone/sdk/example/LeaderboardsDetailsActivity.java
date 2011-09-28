package com.playphone.sdk.example;

import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.playphone.multinet.MNDirect;
import com.playphone.multinet.core.ws.IMNWSRequestEventHandler;
import com.playphone.multinet.core.ws.MNWSRequestContent;
import com.playphone.multinet.core.ws.MNWSRequestError;
import com.playphone.multinet.core.ws.MNWSRequestSender;
import com.playphone.multinet.core.ws.MNWSResponse;
import com.playphone.multinet.core.ws.data.MNWSLeaderboardListItem;

public class LeaderboardsDetailsActivity extends CustomTitleActivity implements Callback {
	
	Handler handler = new Handler(this);
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.leaderboard_details);
		
		// set the breadcrumbs text
		TextView txtBreadCrumbs = (TextView) findViewById(R.id.txtBreadCrumbs);
		txtBreadCrumbs.setText("Home > Leaderboards > Details");
				
		
		LinearLayout layout = (LinearLayout) findViewById(R.id.layoutLeaderboards);
		
		
		// get intent and set the gamesetid
		int gamesetId = getIntent().getExtras().getInt("leaderboardID");
		MNDirect.setDefaultGameSetId(gamesetId);
		TextView txtLeaderboardName = (TextView) findViewById(R.id.txtLeaderboardName);
		txtLeaderboardName.setText(getLeaderboardName(gamesetId));
		
		
			Button btnLeaderboard = (Button) findViewById(R.id.btnUpdateScore);
			btnLeaderboard.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					Intent intent = new Intent(LeaderboardsDetailsActivity.this,PostScoreActivity.class);
					startActivity(intent);
				}
			});
		
		
	}
	
	
	@Override
	protected void onResume() {
		super.onResume();
		// create content object
		MNWSRequestContent content = new MNWSRequestContent();
				
		
		// add the "leaderboard" request which returns the global leaderboard for the last game to request content.
		// store the block name returned by "add..." call to use it later to extract leaderboard data from response
		String blockName = content.addCurrUserLeaderboard
		                 (MNWSRequestContent.LEADERBOARD_SCOPE_GLOBAL,
		                 MNWSRequestContent.LEADERBOARD_PERIOD_ALL_TIME);
				 
		// create "request sender" object
		MNWSRequestSender sender = new MNWSRequestSender(MNDirect.getSession());
		 
		// send the "authorized" request, passing the created content object and event handler
		sender.sendWSRequestAuthorized(content,new MyLeaderboardResponseHandler(blockName));
	}

	
	private String getLeaderboardName(int gamesetId) {
		if(gamesetId == 1) return "Simple";
		if(gamesetId == 2) return "Advanced";
		return "Default";
	}


	protected class MyLeaderboardResponseHandler implements IMNWSRequestEventHandler
	 {
		  
		
			// store the block name which is used to access data in the onRequestCompleted method
		  public MyLeaderboardResponseHandler (String blockName)
		   {
		    this.blockName = blockName;
		   }
		 
		  public void onRequestCompleted (MNWSResponse     response)
		   {
		    // get leaderboard data from response, using block name which was previously retrieved from 
		    // MNWSRequestContent.addCurrUserLeaderboard call
		    // "leaderboard" requests return data as a list of MNWSLeaderboardListItem objects, so
		    // it is safe to cast explicit result of this call to List<MNWSLeaderboardListItem>
		    List<MNWSLeaderboardListItem> leaderboard = (List<MNWSLeaderboardListItem>)response.getDataForBlock(blockName);
		 
		    // Iterate over the returned list and print the name of the player and his/her highest score
		    ArrayList<String> usernames = new ArrayList<String>();
		    ArrayList<String> scores = new ArrayList<String>();
		    
		    
		    for (MNWSLeaderboardListItem item : leaderboard)
		 
		     {
		      Log.d("playphone","Player : " + item.getUserNickName() +
		    		  			 " gamesetid: " + String.valueOf(item.getGamesetId()) + 
		                         " score: " + item.getOutHiScoreText());
		      usernames.add(item.getUserNickName());
		      scores.add(item.getOutHiScoreText());
		     }
		    
			Message msg = Message.obtain();
			Bundle bundle = new Bundle();
			bundle.putStringArrayList("usernames",usernames);
			bundle.putStringArrayList("scores",scores);
			msg.setData(bundle);
			LeaderboardsDetailsActivity.this.handler.sendMessage(msg);

		   }
		 
		  public void onRequestError (MNWSRequestError error)
		   {
		    // log error message
		    Log.e("LeaderboardResponse",error.getMessage());
		   }
		 
		  private String blockName;
	 }


	@Override
	public boolean handleMessage(Message msg) {
		ArrayList<String> usernames = msg.getData().getStringArrayList("usernames");
		ArrayList<String> scores = msg.getData().getStringArrayList("scores");
		
		LinearLayout layout = (LinearLayout) findViewById(R.id.layoutLeaderboards);
		layout.removeAllViews();	
			
		// for each item create a button and add it to the layout
		for(int i=0;i<usernames.size();i++)
		{
			String place = String.valueOf(i + 1);
			TextView txtLeaderboardItem = new TextView(layout.getContext());
			txtLeaderboardItem.setText(place + ". " + usernames.get(i) + "                  " + scores.get(i));
			layout.addView(txtLeaderboardItem);
			Log.d("playphone","added user" + usernames.get(i));
		}
		
		layout.invalidate();
		return false;
	}

}
