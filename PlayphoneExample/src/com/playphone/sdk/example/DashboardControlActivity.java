package com.playphone.sdk.example;

import com.playphone.multinet.MNDirect;
import com.playphone.multinet.MNDirectUIHelper;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class DashboardControlActivity extends CustomTitleActivity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.dashboard_control);
	
		// set the breadcrumbs text
		TextView txtBreadCrumbs = (TextView) findViewById(R.id.txtBreadCrumbs);
		txtBreadCrumbs.setText("Home > Dashboard Control");
		
		Button btnLoginScreen = (Button) findViewById(R.id.btnLoginScreen);
		btnLoginScreen.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				MNDirect.execAppCommand("jumpToUserLogin",null);
				MNDirectUIHelper.showDashboard();
			}
		});
		
		Button btnLeaderboards = (Button) findViewById(R.id.btnLeaderboards);
		btnLeaderboards.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				MNDirect.execAppCommand("jumpToLeaderboard",null);
				MNDirectUIHelper.showDashboard();
			}
		});

		
		Button btnAchievements = (Button) findViewById(R.id.btnAchievements);
		btnAchievements.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				MNDirect.execAppCommand("jumpToAchievements",null);
				MNDirectUIHelper.showDashboard();
			}
		});


		/*
		Button btnStore = (Button) findViewById(R.id.btnStore);
		btnStore.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				MNDirect.execAppCommand("jumpToStore",null);
				MNDirectUIHelper.showDashboard();
			}
		});
		*/

		Button btnHome = (Button) findViewById(R.id.btnHome);
		btnHome.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				MNDirect.execAppCommand("jumpToUserHome",null);
				MNDirectUIHelper.showDashboard();
			}
		});

	
	}
		

}
