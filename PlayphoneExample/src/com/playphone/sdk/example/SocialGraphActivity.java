package com.playphone.sdk.example;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import com.playphone.multinet.core.ws.data.MNWSBuddyListItem;
import com.playphone.multinet.providers.MNVItemsProvider.GameVItemInfo;

public class SocialGraphActivity extends CustomTitleActivity implements Callback {
	
	LinearLayout layout;
	Handler handler = new Handler(this);
	Map<String,MNWSBuddyListItem> buddies = new HashMap<String,MNWSBuddyListItem>();
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.social_graph);
		
		// set the breadcrumbs text
		TextView txtBreadCrumbs = (TextView) findViewById(R.id.txtBreadCrumbs);
		txtBreadCrumbs.setText("Home > Social Graph");
		
		
		// send request
		// create content object
		MNWSRequestContent content = new MNWSRequestContent();
		 
		// add "friend list" request to request content.
		// store block name returned by the "add..." call to use it later to extract friends information from response
		String blockName = content.addCurrUserBuddyList();
		 
		// create "request sender" object
		MNWSRequestSender sender = new MNWSRequestSender(MNDirect.getSession());
		 
		// send "authorized" request, passing created content object and event handler
		sender.sendWSRequestAuthorized(content,new MyBuddyListResponseHandler(blockName));
		
		
		
		
		
		layout = (LinearLayout) findViewById(R.id.virtualItemListLayout);
		
	}
	
	
	protected void recreateLayout(List<String> friends)
	{
		layout = (LinearLayout) findViewById(R.id.virtualItemListLayout);
		for (String friend : friends)
	     {
	    	Log.d("playphone","It does reach here");
	    	final String currentFriend = friend;
	    	
	    	Button btnVirtualItem = new Button(layout.getContext());
			btnVirtualItem.setText(friend);
			btnVirtualItem.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					//generate the intent for the friend details and pass in the item id
					Intent intent = new Intent(SocialGraphActivity.this,SocialGraphDetailActivity.class);
					intent.putExtra("social", "yes");
					intent.putExtra("username", currentFriend);
					intent.putExtra("userid",buddies.get(currentFriend).getFriendUserId());
					intent.putExtra("locale",buddies.get(currentFriend).getFriendUserLocale());
					intent.putExtra("online",buddies.get(currentFriend).getFriendUserOnlineNow());
					startActivity(intent);
				}
			});
			Log.d("playphone","Adding friend " + friend);
			layout.addView(btnVirtualItem);
	     }
		layout.invalidate();
	}
	
	
	
	
	protected class MyBuddyListResponseHandler implements IMNWSRequestEventHandler
	 {
	  // store the block name which is used to access data in onRequestCompleted method
	  public MyBuddyListResponseHandler (String blockName)
	   {
	    this.blockName = blockName;
	   }
	 
	  public void onRequestCompleted (MNWSResponse     response)
	   {
	    Log.d("playphone","Request completed");
	    // get friend list from response, using block name which was previously received from the 
	    // MNWSRequestContent.addCurrUserBuddyList call
	    // the "friend list" request returns data as a List of MNWSBuddyListItem objects, so
	    // it is safe to explicitly cast the result of this call to List<MNWSBuddyListItem>
	    List<MNWSBuddyListItem> friends = (List<MNWSBuddyListItem>)response.getDataForBlock(blockName);
	    
	    Log.d("playphone","Size of friends list: " + String.valueOf(friends.size()));
	    ArrayList<String> users = new ArrayList<String>();
	    
	    // Iterate over the returned list and print the name of each friend
	    buddies.clear();
	    for (MNWSBuddyListItem friend : friends)
	     {
	    	users.add(friend.getFriendUserNickName());
	    	buddies.put(friend.getFriendUserNickName(),friend);
	     }
	    
	    Message msg = Message.obtain();
		Bundle bundle = new Bundle();
		bundle.putStringArrayList("usernames", users);
		msg.setData(bundle);
		SocialGraphActivity.this.handler.sendMessage(msg);
	   }
	 
	  public void onRequestError (MNWSRequestError error)
	   {
	    // log error message
	    Log.e("BuddyListResponse",error.getMessage());
	   }
	 
	  private String blockName;
	 }




	@Override
	public boolean handleMessage(Message msg) {
		if(msg.getData().containsKey("usernames"))
		{
			List<String> friends = msg.getData().getStringArrayList("usernames");
			recreateLayout(friends);
		}
		return true;
	}

}
