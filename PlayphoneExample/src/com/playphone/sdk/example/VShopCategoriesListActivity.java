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
import com.playphone.multinet.providers.MNVShopProvider.VShopCategoryInfo;

public class VShopCategoriesListActivity extends CustomTitleActivity{
	
	
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
		txtBreadCrumbs.setText("Home > Virtual Economy > Store > VShop Categories List");
				
		
		List<VShopCategoryInfo> vShopCategories = Arrays.asList(MNDirect.getVShopProvider().getVShopCategoryList());
		LinearLayout layout = (LinearLayout) findViewById(R.id.virtualItemListLayout);
		
		// for each item create a button and add it to the layout
		for(VShopCategoryInfo vShopCategory : vShopCategories)
		{
			TextView txtVShopCategory = new TextView(layout.getContext());
			txtVShopCategory.setText("ID: " + vShopCategory.id + "     " + vShopCategory.name);
			layout.addView(txtVShopCategory);
		}
		
		
		
	}

}
