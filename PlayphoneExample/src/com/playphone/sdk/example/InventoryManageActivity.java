package com.playphone.sdk.example;

import java.util.Arrays;
import java.util.List;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.playphone.multinet.MNDirect;
import com.playphone.multinet.providers.MNVItemsProvider.PlayerVItemInfo;

public class InventoryManageActivity extends CustomTitleActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.inventory_manage);
	
		// set the breadcrumbs text
		TextView txtBreadCrumbs = (TextView) findViewById(R.id.txtBreadCrumbs);
		txtBreadCrumbs.setText("Home > Virtual Economy > Inventory > Manage Inventory");
		
		List<PlayerVItemInfo> playerItems = Arrays.asList(MNDirect.getVItemsProvider().getPlayerVItemList());
		
		
		
		//LinearLayout layoutCurrencies = (LinearLayout) findViewById(R.id.layoutCurrencies);
		//LinearLayout layoutItems = (LinearLayout) findViewById(R.id.layoutItems);
	}

}
