package com.playphone.sdk.example;

import com.playphone.multinet.MNDirect;
import com.playphone.multinet.MNDirectButton;
import com.playphone.multinet.MNDirectPopup;
import com.playphone.multinet.MNDirectUIHelper;

import android.app.Activity;
import android.os.Bundle;

public class PlayphoneExampleActivity extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
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