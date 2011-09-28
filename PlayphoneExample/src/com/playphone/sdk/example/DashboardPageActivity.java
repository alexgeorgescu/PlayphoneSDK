package com.playphone.sdk.example;

import com.playphone.multinet.MNDirectButton;
import com.playphone.multinet.MNDirectPopup;
import com.playphone.multinet.MNDirectUIHelper;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class DashboardPageActivity extends CustomTitleActivity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.dashboard_page);
	
		// set the breadcrumbs text
		TextView txtBreadCrumbs = (TextView) findViewById(R.id.txtBreadCrumbs);
		txtBreadCrumbs.setText("Home > Dashboard");
		
		Button btnShowLauncher = (Button) findViewById(R.id.btnShowLauncher);
		btnShowLauncher.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				MNDirectButton.show();
			}
		});
	
		
		Button btnHideLauncher = (Button) findViewById(R.id.btnHideLauncher);
		btnHideLauncher.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				MNDirectButton.hide();
			}
		});
		
		
		Button btnShowDashboard = (Button) findViewById(R.id.btnShowDashboard);
		btnShowDashboard.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				MNDirectUIHelper.showDashboard();
			}
		});
	}

}
