package com.playphone.sdk.example;

import android.app.Activity;
import android.content.Intent;
import android.widget.Toast;

import com.playphone.multinet.MNDirectEventHandlerAbstract;
import com.playphone.multinet.MNGameParams;
import com.playphone.multinet.MNUserInfo;

public class MNEventHandler extends MNDirectEventHandlerAbstract {
	 
		private Activity activity;
	
		public MNEventHandler(Activity activity) {
			this.activity = activity;
		}
	
		@Override
		public void mnDirectDoStartGameWithParams(MNGameParams params) {
			activity.startActivity(new Intent(activity, MainActivity.class));
		}
		
		@Override
		public void mnDirectDidReceiveGameMessage(String message,MNUserInfo sender)
		{
			if(sender != null)
			Toast.makeText(activity, "User " + sender.userName + 
					" sent message: " +  message, Toast.LENGTH_LONG).show();
		}
	
}
