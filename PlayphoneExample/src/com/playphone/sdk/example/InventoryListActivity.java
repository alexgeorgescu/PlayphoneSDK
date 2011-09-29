package com.playphone.sdk.example;

import java.util.Arrays;
import java.util.List;

import com.playphone.multinet.MNDirect;
import com.playphone.multinet.providers.MNVItemsProvider.GameVItemInfo;
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
		
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		LinearLayout layoutCurrencies = (LinearLayout) findViewById(R.id.layoutCurrencies);
		LinearLayout layoutItems = (LinearLayout) findViewById(R.id.layoutItems);
		layoutCurrencies.removeAllViews();
		layoutItems.removeAllViews();
		List<PlayerVItemInfo> playerItems = Arrays.asList(MNDirect.getVItemsProvider().getPlayerVItemList());
		for(PlayerVItemInfo playerItem : playerItems)
		{
			int itemId = playerItem.id;
			long amount = playerItem.count;
			GameVItemInfo gameItem = MNDirect.getVItemsProvider().findGameVItemById(itemId);
			String name = gameItem.name;
			String txtToDisplay = "ID" + String.valueOf(itemId) + " " + name + "      " + String.valueOf(amount);
			TextView txtItem = new TextView(this);
			txtItem.setText(txtToDisplay);
			
			// the model of a virtual item is a flag, 
			// so we can bitwise compare with 1 in this case,
			// since flag 1 is currency
			if((Integer.valueOf(gameItem.model) & 1) == 0)
			{
				//not currency, add to layoutItems
				layoutItems.addView(txtItem);
			}
			else
			{
				//currency, add to layoutCurrencies
				layoutCurrencies.addView(txtItem);
			}
		}
		layoutCurrencies.invalidate();
		layoutItems.invalidate();
	}


}
