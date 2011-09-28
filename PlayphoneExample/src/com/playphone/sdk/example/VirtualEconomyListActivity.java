package com.playphone.sdk.example;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class VirtualEconomyListActivity extends CustomTitleActivity {
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.virtual_economy_list);
		
		// set the breadcrumbs text
		TextView txtBreadCrumbs = (TextView) findViewById(R.id.txtBreadCrumbs);
		txtBreadCrumbs.setText("Home > Virtual Economy");
				
		
		Button btnVItems = (Button) findViewById(R.id.btnVItems);
		btnVItems.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(VirtualEconomyListActivity.this, VirtualItemsHomeActivity.class));
			}
		});
		
		Button btnPlayphoneStore = (Button) findViewById(R.id.btnPlayphoneStore);
		btnPlayphoneStore.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(VirtualEconomyListActivity.this, StoreHome.class));
			}
		});
		
		Button btnInventory = (Button) findViewById(R.id.btnInventory);
		btnInventory.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(VirtualEconomyListActivity.this, InventoryHomeActivity.class));
			}
		});
		
	}

}
