package com.playphone.sdk.example;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.playphone.multinet.MNDirect;
import com.playphone.multinet.providers.MNVItemsProvider.GameVItemInfo;

public class VirtualItemsDetailsActivity extends CustomTitleActivity {
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.virtual_item_details);
		
		// set the breadcrumbs text
		TextView txtBreadCrumbs = (TextView) findViewById(R.id.txtBreadCrumbs);
		txtBreadCrumbs.setText("Home > Virtual Economy > VItem > Item List > Details");
		
		int itemID = getIntent().getIntExtra("itemID", -1);
		Log.d("playphone","Trying to find item with ID: " + itemID);
		GameVItemInfo item = MNDirect.getVItemsProvider().findGameVItemById(itemID);
		Log.d("playphone","Found item with name: " + item.name);
		
		if(itemID > -1)
		{
		// set the item id
		TextView txtItemId = (TextView) findViewById(R.id.txtItemId);
		txtItemId.setText("ID:"+ String.valueOf(item.id));
		
		// set the item name
		TextView txtItemName = (TextView) findViewById(R.id.txtItemName);
		txtItemName.setText(item.name);
		
		// set the unique flag
		CheckBox chkUnique = (CheckBox) findViewById(R.id.ckUnique);
		chkUnique.setChecked((item.model & 2) != 0);
		
		// set the consumable flag
		CheckBox chkConsumable = (CheckBox) findViewById(R.id.ckConsumable);
		chkConsumable.setChecked((item.model & 4) != 0);
				
		// set the unique flag
		CheckBox chkAllowedFromClient = (CheckBox) findViewById(R.id.ckAllowedFromClient);
		chkAllowedFromClient.setChecked((item.model & 512) != 0);
		
		// set the description
		TextView txtDescription = (TextView) findViewById(R.id.txtDescription);
		txtDescription.setText(item.description);
				
		// set the parameters
		EditText editApplicationParams = (EditText) findViewById(R.id.editApplicationParameters);
		editApplicationParams.setText(item.params);
		
		// set the Image
		ImageView imgItem = (ImageView) findViewById(R.id.imgItem);
		Bitmap bitmap = getImageBitmap(MNDirect.getVItemsProvider().getVItemImageURL(item.id));
		//Bitmap bitmap = getImageBitmap("http://images2.wikia.nocookie.net/__cb20110502062558/finalfantasy/images/1/1f/Buster_sword(AC_version)2.jpg");
		imgItem.setImageBitmap(bitmap);
		}
		
	}
	
	
	
	private Bitmap getImageBitmap(String url) { 
        Bitmap bm = null; 
        try { 
            URL aURL = new URL(url); 
            URLConnection conn = aURL.openConnection(); 
            conn.connect(); 
            InputStream is = conn.getInputStream(); 
            BufferedInputStream bis = new BufferedInputStream(is); 
            bm = BitmapFactory.decodeStream(bis); 
            bis.close(); 
            is.close(); 
       } catch (IOException e) { 
           Log.e("playphone", "Error getting bitmap", e); 
       } 
       return bm; 
	}

}
