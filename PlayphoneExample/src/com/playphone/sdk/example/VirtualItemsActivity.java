package com.playphone.sdk.example;

import com.playphone.multinet.MNDirect;
import com.playphone.multinet.MNDirectUIHelper;
import com.playphone.multinet.providers.MNVShopProvider;
import com.playphone.multinet.providers.MNVShopProvider.VShopPackInfo;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class VirtualItemsActivity extends Activity implements OnClickListener{

	
	TextView txtResult;
	
	
		@Override
    	public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			setContentView(R.layout.post_virtual_items);
	        
	        Button btnUpload = (Button) findViewById(R.id.btnUpload);
	        btnUpload.setOnClickListener(this);
	        
	        txtResult = (TextView) findViewById(R.id.txtResult);
	        
	        
	        Button btnBack = (Button) findViewById(R.id.btnBack);
	        btnBack.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					finish();
				}
			});
        
		}
	
		@Override
		public void onClick(View arg0) {
			// used for testing
			//first update the info
			if (MNDirect.getVShopProvider().isVShopInfoNeedUpdate())
			{
				Log.d("playphone","Updating the items...");
				MNDirect.getVShopProvider().doVShopInfoUpdate();
			}
			
			MNVShopProvider.VShopPackInfo[] vShopPacks = MNDirect.getVShopProvider().getVShopPackList();
			 
			ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.list_item);
			
			
			for (VShopPackInfo pack : vShopPacks)
			 {
			  Log.d("playphone","Pack name: " + pack.name + " , pack description: " + pack.description + 
					  " ,pack params: " + pack.appParams + " ,pack value: " + pack.priceValue +
					  " ,currency: " + pack.priceItemId);
			  adapter.add(pack.name);
			 }
			
			Log.d("playphone","ok so far");
			ListView lv = (ListView) findViewById(R.id.listView1);
			lv.setAdapter(adapter);
			lv.invalidate();
			
		}
	
	 	@Override
		protected void onPause() {
			super.onPause();
			MNDirectUIHelper.setHostActivity(null);
		}
	 
		@Override
		protected void onResume() {
			super.onResume();
			MNDirectUIHelper.setHostActivity(this);
		}

}
