package com.playphone.sdk.example;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class StoreHome extends CustomTitleActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.store_home);
	
		// set the breadcrumbs text
		TextView txtBreadCrumbs = (TextView) findViewById(R.id.txtBreadCrumbs);
		txtBreadCrumbs.setText("Home > Virtual Economy > Store");
		
		Button btnVShopCategoriesList = (Button) findViewById(R.id.btnVShopCategoriesList);
		btnVShopCategoriesList.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(StoreHome.this, VShopCategoriesListActivity.class));
			}
		});
	
		
		Button btnVShopPacksList = (Button) findViewById(R.id.btnVShopPacksList);
		btnVShopPacksList.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(StoreHome.this, VShopPacksListActivity.class));
			}
		});
		
		
		Button btnBuyVShopPacks = (Button) findViewById(R.id.btnBuyVShopPacks);
		btnBuyVShopPacks.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(StoreHome.this, VirtualCurrenciesListActivity.class));
			}
		});
	}

}
