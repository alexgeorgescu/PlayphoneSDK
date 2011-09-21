package com.playphone.sdk.example;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import com.playphone.multinet.MNDirect;
import com.playphone.multinet.MNDirectUIHelper;
import com.playphone.multinet.core.MNSession;

public class LogoutActivity extends Activity{
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.post_logout);
		
		Button btnLogout = (Button) findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(MNDirect.isUserLoggedIn()) 
					MNDirect.getSession().logout(); 
				MNDirectUIHelper.showDashboard();
				LogoutActivity.this.finish();
			}
		});
		
        
        
        Button btnLogoutWipe = (Button) findViewById(R.id.btnLogoutWipe);
        btnLogoutWipe.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(MNDirect.isUserLoggedIn()) 
					MNDirect.getSession().logoutAndWipeUserCredentialsByMode(MNSession.MN_CREDENTIALS_WIPE_USER); 
				MNDirectUIHelper.showDashboard();
				LogoutActivity.this.finish();
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
}
