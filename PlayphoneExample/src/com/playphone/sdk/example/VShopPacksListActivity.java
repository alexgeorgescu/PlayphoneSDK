package com.playphone.sdk.example;

import java.util.Arrays;
import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.playphone.multinet.MNDirect;
import com.playphone.multinet.providers.MNVItemsProvider.GameVItemInfo;
import com.playphone.multinet.providers.MNVShopProvider.VShopPackInfo;

public class VShopPacksListActivity extends CustomTitleActivity {

	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.virtual_items_list);
		
		if (MNDirect.getVItemsProvider().isGameVItemsListNeedUpdate())
		{
			Log.d("playphone","Updating the items...");
			MNDirect.getVItemsProvider().doGameVItemsListUpdate();
		}
		
		// set the breadcrumbs text
		TextView txtBreadCrumbs = (TextView) findViewById(R.id.txtBreadCrumbs);
		txtBreadCrumbs.setText("Home > Virtual Economy > Store > VShop Packs List");
				
		
		List<VShopPackInfo> vShopCategories = Arrays.asList(MNDirect.getVShopProvider().getVShopPackList());
		LinearLayout layout = (LinearLayout) findViewById(R.id.virtualItemListLayout);
		
		// for each item create a button and add it to the layout
		for(VShopPackInfo vShopPackInfo : vShopCategories)
		{
			final VShopPackInfo currentItem = vShopPackInfo;
			
			Button btnVShopCategory = new Button(layout.getContext());
			btnVShopCategory.setText("ID: " + vShopPackInfo.id + "     " + vShopPackInfo.name + "    " + getPriceString(vShopPackInfo.priceValue));
			btnVShopCategory.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					//generate the intent for the virtual item details and pass in the item id
					Intent intent = new Intent(VShopPacksListActivity.this,VirtualPackDetailsActivity.class);
					Log.d("playphone","Adding item id: " + currentItem.id);
					intent.putExtra("itemID", currentItem.id);
					startActivity(intent);
				}
			});

			layout.addView(btnVShopCategory);
		}
		
		
		
	}
	
	
	private String getPriceString(long money){
		String currencySign = "$";
		String delimiter = ".";
		long subCoinConversion = 100;
		String coin = String.valueOf(money / subCoinConversion);
		String subcoin = String.valueOf(money % subCoinConversion);
		return currencySign + coin + delimiter + subcoin;				
	}
	

}
