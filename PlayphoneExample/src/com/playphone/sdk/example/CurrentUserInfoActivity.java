package com.playphone.sdk.example;

import com.playphone.multinet.MNDirect;
import com.playphone.multinet.MNDirectUIHelper;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class CurrentUserInfoActivity extends Activity{
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.post_userinfo);
		
		TextView txtResult = (TextView) findViewById(R.id.txtResult);
		StringBuffer str = new StringBuffer().
				append("Username: ").append(MNDirect.getSession().getMyUserName()).append("\n").
				append("User id: ").append(MNDirect.getSession().getMyUserId()).append("\n").
				append("Current room: ").append(MNDirect.getSession().getCurrentRoomId());
		txtResult.setText(str.toString());
		
		Button btnBack = (Button) findViewById(R.id.btnBack);
        btnBack.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
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

}
