package com.playphone.sdk.example;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.playphone.multinet.MNDirect;
import com.playphone.multinet.providers.MNVItemsProvider.GameVItemInfo;
import com.playphone.multinet.providers.MNVShopProvider.VShopDeliveryInfo;
import com.playphone.multinet.providers.MNVShopProvider.VShopPackInfo;

public class VirtualPackDetailsActivity extends CustomTitleActivity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.vshop_pack_details);
		
		// set the breadcrumbs text
		TextView txtBreadCrumbs = (TextView) findViewById(R.id.txtBreadCrumbs);
		txtBreadCrumbs.setText("Home > Virtual Economy > Store > VShop Packs List > Details");
		
		int itemID = getIntent().getIntExtra("itemID", -1);
		VShopPackInfo item = MNDirect.getVShopProvider().findVShopPackById(itemID);
		Log.d("playphone","Flags: " + String.valueOf(item.model));
		
		if(itemID > -1)
		{
		//get item inside the delivery
		int itemIDInsidePack  = item.delivery[0].vItemId;
		GameVItemInfo itemInsidePack = MNDirect.getVItemsProvider().findGameVItemById(itemIDInsidePack);
		long itemQuantity = item.delivery[0].amount;
		
		// set the item id
		TextView txtItemId = (TextView) findViewById(R.id.txtItemId);
		txtItemId.setText("Pack ID:"+ String.valueOf(item.id));
		
		// set the item name
		TextView txtItemName = (TextView) findViewById(R.id.txtItemName);
		txtItemName.setText(item.name);
		
		// set the category
		TextView txtCategory = (TextView) findViewById(R.id.txtCategory);
		txtCategory.setText("Category: "+ MNDirect.getVShopProvider().findVShopCategoryById(item.categoryId).name);
		
		// set the name of the contained item
		TextView txtContainsItemName = (TextView) findViewById(R.id.txtContainsItemName);
		txtContainsItemName.setText("Item name: "+ itemInsidePack.name);
				
		// set the quantity
		TextView txtQuantity = (TextView) findViewById(R.id.txtQuantity);
		txtQuantity.setText("Quantity: "+ itemQuantity);
				
		// set the price
		TextView txtPrice = (TextView) findViewById(R.id.txtPrice);
		txtPrice.setText("Price: "+ getPriceString(item.priceValue));
		
		// set the hidden property
		CheckBox ckIsHidden = (CheckBox) findViewById(R.id.ckIsHidden);
		ckIsHidden.setChecked((item.model & 1) != 0);
		
		// set the hold sales property
		CheckBox ckHoldSales = (CheckBox) findViewById(R.id.ckHoldSales);
		ckHoldSales.setChecked((item.model & 2) != 0);
		
		// set the description
		TextView txtDescription = (TextView) findViewById(R.id.txtDescription);
		txtDescription.setText(item.description);
				
		// set the parameters
		EditText editApplicationParams = (EditText) findViewById(R.id.editApplicationParameters);
		editApplicationParams.setText(item.appParams);
		
		// set the Image
		ImageView imgItem = (ImageView) findViewById(R.id.imgItem);
		Bitmap bitmap = getImageBitmap(MNDirect.getVShopProvider().getVShopPackImageURL(item.id));
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
	
	private String getPriceString(long money){
		String currencySign = "$";
		String delimiter = ".";
		long subCoinConversion = 100;
		String coin = String.valueOf(money / subCoinConversion);
		String subcoin = String.valueOf(money % subCoinConversion);
		return currencySign + coin + delimiter + subcoin;				
	}

}
