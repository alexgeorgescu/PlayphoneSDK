package com.playphone.sdk.example;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class VirtualItemsHomeActivity extends CustomTitleActivity {
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.virtual_items_home);
	
		
		Button btnVItems = (Button) findViewById(R.id.btnVItems);
		btnVItems.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(VirtualItemsHomeActivity.this, VirtualItemsListActivity.class));
			}
		});
	
		
		Button btnVCurrencies = (Button) findViewById(R.id.btnVCurrencies);
		btnVCurrencies.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(VirtualItemsHomeActivity.this, NotImplementedActivity.class));
			}
		});
	}

}
