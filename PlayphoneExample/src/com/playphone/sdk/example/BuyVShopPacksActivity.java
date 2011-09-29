package com.playphone.sdk.example;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.playphone.multinet.MNDirect;
import com.playphone.multinet.MNDirectUIHelper;
import com.playphone.multinet.providers.MNVShopProvider;
import com.playphone.multinet.providers.MNVShopProvider.VShopPackInfo;
import com.playphone.multinet.providers.MNVShopProvider.IEventHandler.CheckoutVShopPackFailInfo;
import com.playphone.multinet.providers.MNVShopProvider.IEventHandler.CheckoutVShopPackSuccessInfo;

public class BuyVShopPacksActivity extends CustomTitleActivity implements OnClickListener,Callback {
	
	Map<String,VShopPackInfo> packs = new HashMap<String,VShopPackInfo>();
	Handler handler = new Handler(this);
	
	
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
		
		
		// for each item add create a map of string and item
		packs.clear();
		for(VShopPackInfo vShopPack : vShopPacks)
		{
			String textToDisplay = vShopPack.name + " (" + getPriceString(vShopPack.priceValue) + ")";
			packs.put(textToDisplay, vShopPack);
		}
		
		Spinner spinner = (Spinner) findViewById(R.id.spinner);
	    ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,R.layout.array_adapter_layout);
	    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		for(String key : packs.keySet())
			adapter.add(key);
	    spinner.setAdapter(adapter);
				
				
				Button btnBuy = (Button) findViewById(R.id.btnBuy);
				btnBuy.setOnClickListener(this);
				
				MNDirect.getVShopProvider().addEventHandler(new MNVShopProvider.EventHandlerAbstract()
				 {
				  public void showDashboard ()
				   {
				    // application-specific code which shows dashboard
					  MNDirectUIHelper.showDashboard();
					  
				   }
				 
				  public void hideDashboard ()
				   {
				    // application-specific code which hides dashboard
					  MNDirectUIHelper.hideDashboard();
				   }
				 
				  public void onCheckoutVShopPackSuccess (final CheckoutVShopPackSuccessInfo result)
				   {
				    // purchase operation finished successfully, details of transaction
				    // can be accessed via result.getTransaction() call
				    Log.d("playphone","Transaction " + Long.toString(result.getTransaction().clientTransactionId) + " finished");
				      Message msg = new Message();
				      Bundle bundle = new Bundle();
				      bundle.putString("message", "Transaction successful, enjoy your items!");
				      msg.setData(bundle);
				      handler.sendMessage(msg);

				   }
				 
				  public void onCheckoutVShopPackFail (final CheckoutVShopPackFailInfo result)
				   {
				    // purchase operation was not completed successfully
				    if (result.getErrorCode() == ERROR_CODE_USER_CANCEL)
				     {
				      Log.d("playphone","Player cancelled operation");
				      Message msg = new Message();
				      Bundle bundle = new Bundle();
				      bundle.putString("message", "You successfully cancelled the transaction");
				      msg.setData(bundle);
				      handler.sendMessage(msg);
				     }
				    else
				     {
				      Log.e("playphone","Purchase failed with error message " + result.getErrorMessage());
				      Message msg = new Message();
				      Bundle bundle = new Bundle();
				      bundle.putString("message", "Purchase failed, please mention this error message: " + result.getErrorMessage());
				      msg.setData(bundle);
				      handler.sendMessage(msg);
				     }
				   }
				 });
				
				
				
	}
	
	private String getPriceString(long money){
		String currencySign = "$";
		String delimiter = ".";
		long subCoinConversion = 100;
		String coin = String.valueOf(money / subCoinConversion);
		String subcoin = String.valueOf(money % subCoinConversion);
		return currencySign + coin + delimiter + subcoin;				
	}

	@Override
	public void onClick(View v) {
		if(MNDirect.isUserLoggedIn())
		{
			Spinner spinner = (Spinner) findViewById(R.id.spinner);
			Log.d("playphone","Chosen pack " + spinner.getSelectedItem().toString());
			VShopPackInfo currentSelectedPack = packs.get(spinner.getSelectedItem().toString());
			Log.d("playphone","Attempting to buy pack id " + currentSelectedPack.id);
			final int[] packs   = { currentSelectedPack.id};
			final int[] amounts = { 1 };
			 
			MNDirect.getVShopProvider().execCheckoutVShopPacks
			 (packs,amounts,MNDirect.getVItemsProvider().getNewClientTransactionId());
		}
		
	}

	@Override
	public boolean handleMessage(Message msg) {
		Log.d("playphone","Received new message");
		Bundle bundle = msg.getData();
		if(bundle.containsKey("message"))
			Toast.makeText(this, bundle.getString("message"), Toast.LENGTH_SHORT).show();
		return false;
	}


}
