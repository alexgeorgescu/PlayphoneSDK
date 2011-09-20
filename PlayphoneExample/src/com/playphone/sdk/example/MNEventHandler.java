package com.playphone.sdk.example;

import android.app.Activity;
import android.content.Intent;

import com.playphone.multinet.MNDirectEventHandlerAbstract;
import com.playphone.multinet.MNGameParams;

public class MNEventHandler extends MNDirectEventHandlerAbstract {
	 
		private Activity activity;
	
		public MNEventHandler(Activity activity) {
			this.activity = activity;
		}
	
		@Override
		public void mnDirectDoStartGameWithParams(MNGameParams params) {
			activity.startActivity(new Intent(activity, PlayphoneExampleActivity.class));
		}
	
}
