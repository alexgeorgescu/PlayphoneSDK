package com.playphone.sdk.example;

import java.util.Arrays;
import java.util.List;

import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.playphone.multinet.MNDirect;
import com.playphone.multinet.providers.MNVShopProvider.VShopPackInfo;

public class BuyVShopPacksActivity extends CustomTitleActivity {
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.buy_vshop_packs);
		
		if (MNDirect.getVItemsProvider().isGameVItemsListNeedUpdate())
		{
			Log.d("playphone","Updating the items...");
			MNDirect.getVItemsProvider().doGameVItemsListUpdate();
		}
		
		// set the breadcrumbs text
		TextView txtBreadCrumbs = (TextView) findViewById(R.id.txtBreadCrumbs);
		txtBreadCrumbs.setText("Home > Virtual Economy > Store > Buy VShop Packs");
				
		List<VShopPackInfo> vShopPacks = Arrays.asList(MNDirect.getVShopProvider().getVShopPackList());
		LinearLayout layout = (LinearLayout) findViewById(R.id.virtualItemListLayout);
		
		// for each item create a button and add it to the layout
		for(VShopPackInfo vShopPack : vShopPacks)
		{
			TextView txtVShopCategory = new TextView(layout.getContext());
			txtVShopCategory.setText(vShopPack.name + " (" + getPriceString(vShopPack.priceValue) + ")");
			//if(vShopPack.priceValue > 0) 
				layout.addView(txtVShopCategory);
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
