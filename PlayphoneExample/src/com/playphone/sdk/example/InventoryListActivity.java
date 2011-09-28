package com.playphone.sdk.example;

import java.util.Arrays;
import java.util.List;

import com.playphone.multinet.MNDirect;
import com.playphone.multinet.providers.MNVItemsProvider.PlayerVItemInfo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class InventoryListActivity extends CustomTitleActivity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.inventory_list);
	
		// set the breadcrumbs text
		TextView txtBreadCrumbs = (TextView) findViewById(R.id.txtBreadCrumbs);
		txtBreadCrumbs.setText("Home > Virtual Economy > Inventory > Inventory List");
		
		List<PlayerVItemInfo> playerItems = Arrays.asList(MNDirect.getVItemsProvider().getPlayerVItemList());
		
		
		
		LinearLayout layoutCurrencies = (LinearLayout) findViewById(R.id.layoutCurrencies);
		LinearLayout layoutItems = (LinearLayout) findViewById(R.id.layoutItems);
	}


}
