package com.playphone.sdk.example;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.playphone.multinet.MNDirect;
import com.playphone.multinet.core.ws.MNWSRequestContent;
import com.playphone.multinet.core.ws.MNWSRequestSender;
import com.playphone.sdk.example.SocialGraphActivity.MyBuddyListResponseHandler;

public class SocialGraphDetailActivity extends CustomTitleActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.social_graph_detail);
		
		// set the breadcrumbs text
		TextView txtBreadCrumbs = (TextView) findViewById(R.id.txtBreadCrumbs);
		txtBreadCrumbs.setText("Home > Social Graph > Detail");
		
		if(getIntent().hasExtra("social"))
		{
		LinearLayout layout = (LinearLayout) findViewById(R.id.layoutSocialGraphDetail);
		TextView txtName = new TextView(this);
		txtName.setText("User Name: " + getIntent().getStringExtra("username"));
		layout.addView(txtName);
		
		TextView txtUserId = new TextView(this);
		txtUserId.setText("User ID: " + String.valueOf(getIntent().getLongExtra("userid",0)));
		layout.addView(txtUserId);
		
		TextView txtLocale = new TextView(this);
		txtLocale.setText("Locale: " + getIntent().getStringExtra("locale"));
		layout.addView(txtLocale);
		
		TextView txtOnline = new TextView(this);
		txtOnline.setText("Online: " + String.valueOf(getIntent().getBooleanExtra("online",true)));
		layout.addView(txtOnline);
		
		
		}
		
	}

	
}
