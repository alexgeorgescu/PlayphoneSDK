package com.playphone.sdk.example;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class InventoryHomeActivity extends CustomTitleActivity{

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.inventory_home);
	
		// set the breadcrumbs text
		TextView txtBreadCrumbs = (TextView) findViewById(R.id.txtBreadCrumbs);
		txtBreadCrumbs.setText("Home > Virtual Economy > Inventory");
		
		Button btnInventoryList = (Button) findViewById(R.id.btnInventoryList);
		btnInventoryList.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(InventoryHomeActivity.this, InventoryListActivity.class));
			}
		});
	
		Button btnManageInventory = (Button) findViewById(R.id.btnManageInventory);
		btnManageInventory.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(InventoryHomeActivity.this, InventoryManageActivity.class));
			}
		});
		
		
	}

}
