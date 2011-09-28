package com.playphone.sdk.example;

import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.playphone.multinet.MNDirectPopup;

public class NotificationPanelActivity extends CustomTitleActivity {
	
	private Button btnActivate;
	private Button btnDeactivate;
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.notification_panel);
	
		// set the breadcrumbs text
		TextView txtBreadCrumbs = (TextView) findViewById(R.id.txtBreadCrumbs);
		txtBreadCrumbs.setText("Home > Notification Panel");
		
		btnActivate = (Button) findViewById(R.id.btnActivate);
		btnActivate.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				MNDirectPopup.setActive(true);
				updateStatusText();
			}
		});
		
		btnDeactivate = (Button) findViewById(R.id.btnDeactivate);
		btnDeactivate.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				MNDirectPopup.setActive(false);
				updateStatusText();
			}
		});
		
		updateStatusText();

		
	}
	
	private void updateStatusText(){
		TextView txtCurrentStatus = (TextView) findViewById(R.id.txtCurrentStatus);
		if (MNDirectPopup.isActive()) 
		{
			txtCurrentStatus.setText("Current Status: Activated");
			btnActivate.setEnabled(false);
			btnDeactivate.setEnabled(true);
		}
		else
		{
			txtCurrentStatus.setText("Current Status: Deactivated");
			btnActivate.setEnabled(true);
			btnDeactivate.setEnabled(false);
		
		}
	}

}
