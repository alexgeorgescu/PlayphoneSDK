package com.playphone.sdk.example;

import java.util.Arrays;
import java.util.List;

import com.playphone.multinet.MNDirect;
import com.playphone.multinet.providers.MNVItemsProvider.GameVItemInfo;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class VirtualItemsListActivity extends CustomTitleActivity {
	
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
		txtBreadCrumbs.setText("Home > Virtual Economy > VItem > Item List");
				
		
		List<GameVItemInfo> virtualItems = Arrays.asList(MNDirect.getVItemsProvider().getGameVItemsList());
		LinearLayout layout = (LinearLayout) findViewById(R.id.virtualItemListLayout);
		
		// for each item create a button and add it to the layout
		for(GameVItemInfo virtualItem : virtualItems)
		{
			Log.d("playphone","Processing item with id: " + virtualItem.id);
			final GameVItemInfo currentItem = virtualItem;
			
			// the model of a virtual item is a flag, 
			// so we can bitwise compare with 1 in this case,
			// since flag 1 is currency
			if((Integer.valueOf(virtualItem.model) & 1) == 0)
			{
			Button btnVirtualItem = new Button(layout.getContext());
			btnVirtualItem.setText("ID: " + virtualItem.id + "     " + virtualItem.name);
			btnVirtualItem.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					//generate the intent for the virtual item details and pass in the item id
					Intent intent = new Intent(VirtualItemsListActivity.this,VirtualItemsDetailsActivity.class);
					Log.d("playphone","Adding item id: " + currentItem.id);
					intent.putExtra("itemID", currentItem.id);
					startActivity(intent);
				}
			});
			layout.addView(btnVirtualItem);
			}
		}
		
		
		
	}

}
