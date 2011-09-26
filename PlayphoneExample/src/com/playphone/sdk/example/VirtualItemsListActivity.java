package com.playphone.sdk.example;

import java.util.Arrays;
import java.util.List;

import com.playphone.multinet.MNDirect;
import com.playphone.multinet.providers.MNVItemsProvider.GameVItemInfo;
import com.playphone.multinet.providers.MNVShopProvider;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;

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
		
		List<GameVItemInfo> virtualItems = Arrays.asList(MNDirect.getVItemsProvider().getGameVItemsList());
		LinearLayout layout = (LinearLayout) findViewById(R.id.virtualItemListLayout);
		
		// for each item create a button and add it to the layout
		for(GameVItemInfo virtualItem : virtualItems)
		{
			
			// the model of a virtual item is a flag, 
			// so we can bitwise compare with 1 in this case,
			// since flag 1 is currency
			if((Integer.valueOf(virtualItem.model) & 1) == 0)
			{
			Button btnVirtualItem = new Button(layout.getContext());
			btnVirtualItem.setText("ID: " + virtualItem.id + "     " + virtualItem.name);
			layout.addView(btnVirtualItem);
			}
		}
		
		
		
	}

}
