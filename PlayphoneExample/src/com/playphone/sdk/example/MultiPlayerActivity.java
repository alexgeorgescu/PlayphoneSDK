package com.playphone.sdk.example;

import com.playphone.multinet.MNDirect;
import com.playphone.multinet.MNDirectEventHandlerAbstract;
import com.playphone.multinet.MNDirectUIHelper;
import com.playphone.multinet.MNGameParams;
import com.playphone.multinet.MNUserInfo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MultiPlayerActivity extends Activity {
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.post_multiplayer);
		
		Button btnUpload = (Button) findViewById(R.id.btnUpload);
        btnUpload.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				EditText textContentView = (EditText) findViewById(R.id.editInput);
				String textContent = textContentView.getText().toString();
				if(!"".equalsIgnoreCase(textContent)){
					MNDirect.sendGameMessage(textContent);
					Log.d("playphone","Sending game message: " + textContent);
				}
			}
		});
		
		Button btnBack = (Button) findViewById(R.id.btnBack);
        btnBack.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				MNDirect.getSession().leaveRoom();
				finish();
			}
		});
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		MNDirectUIHelper.setHostActivity(null);
	}
 
	@Override
	protected void onResume() {
		super.onResume();
		MNDirectUIHelper.setHostActivity(this);
	}
	
	
	protected class MNEventHandler extends MNDirectEventHandlerAbstract {
   	 
		
		@Override
		public void mnDirectDidReceiveGameMessage(String message,MNUserInfo sender)
		{
			Log.d("playphone","Received a message: " + message);
			if(sender != null)
			Toast.makeText(MultiPlayerActivity.this, "User " + sender.userName + 
					" sent message: " +  message, Toast.LENGTH_LONG).show();
		}
	}
	

}
