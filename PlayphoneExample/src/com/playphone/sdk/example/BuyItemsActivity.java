package com.playphone.sdk.example;

import com.playphone.multinet.MNDirect;
import com.playphone.multinet.MNDirectUIHelper;
import com.playphone.multinet.providers.MNVItemsProvider;
import com.playphone.multinet.providers.MNVItemsProvider.TransactionError;
import com.playphone.multinet.providers.MNVItemsProvider.TransactionInfo;
import com.playphone.multinet.providers.MNVShopProvider;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class BuyItemsActivity extends Activity implements Callback {

	final int HEALTH_POTION = 2;
	final int HEALTH_POTION_PACK = 1002;
	final String HEALTH_POTION_NAME = "health potion(s)";
	Handler handler = new Handler(this);
	TextView txtTitle;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.buy_items);
        
		
		txtTitle = (TextView) findViewById(R.id.textView1);
		
        //Button btnUpload = (Button) findViewById(R.id.btnUpload);
        //btnUpload.setOnClickListener(this);
        
		MNDirect.getVItemsProvider().addEventHandler(new MNVItemsProvider.EventHandlerAbstract()
		 {
		  public void onVItemsTransactionCompleted (TransactionInfo  transaction)
		   {
			  //transaction succeeded, update the application
			  Log.d("playphone","Transaction " + transaction.clientTransactionId + " succeeded.");
			  Message msg = new Message();
			  Bundle bundle = new Bundle();
			  bundle.putInt("updateField", R.id.textView1);
			  msg.setData(bundle);
			  BuyItemsActivity.this.handler.sendMessage(msg);
		   }
		 
		  public void onVItemsTransactionFailed (TransactionError error)
		   {
		    
			  // transaction failed, error contains client and server identifiers of the failed transaction and the error message
		   }
		 });
		
		
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
		
		
		
		if(MNDirect.isUserLoggedIn())
        	updateGameItemsOnScreen();
        
        Button btnGetFreePotion = (Button) findViewById(R.id.btnGetFreePotion);
        btnGetFreePotion.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(MNDirect.isUserLoggedIn())
				{
					if (MNDirect.getVItemsProvider().getPlayerVItemCountById(HEALTH_POTION) > 2)
					{
						Toast.makeText(v.getContext(), "You cannot get any more new potions, try buying some!", Toast.LENGTH_SHORT).show();
						return;
					}
					MNDirect.getVItemsProvider().reqAddPlayerVItem(HEALTH_POTION,1,
						MNDirect.getVItemsProvider().getNewClientTransactionId());
					Toast.makeText(v.getContext(), "Added a new potion to your inventory!", Toast.LENGTH_SHORT).show();
				}
			}
		});
        
        
        Button btnUsePotion = (Button) findViewById(R.id.btnUsePotion);
        btnUsePotion.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(MNDirect.isUserLoggedIn())
				{
					if (MNDirect.getVItemsProvider().getPlayerVItemCountById(HEALTH_POTION) == 0)
					{
						Toast.makeText(v.getContext(), "You do not have any potions to use!", Toast.LENGTH_SHORT).show();
						return;
					}
					MNDirect.getVItemsProvider().reqAddPlayerVItem(HEALTH_POTION,-1,
						MNDirect.getVItemsProvider().getNewClientTransactionId());
					Toast.makeText(v.getContext(), "You have just used a potion!", Toast.LENGTH_SHORT).show();
				}
			}
		});
        
        
        Button btnBuy = (Button) findViewById(R.id.btnBuy);
        btnBuy.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(MNDirect.isUserLoggedIn())
				{
					final int[] packs   = { HEALTH_POTION_PACK};
					final int[] amounts = { 1 };
					 
					MNDirect.getVShopProvider().execCheckoutVShopPacks
					 (packs,amounts,MNDirect.getVItemsProvider().getNewClientTransactionId());
					//Toast.makeText(v.getContext(), "You have just used a potion!", Toast.LENGTH_SHORT).show();
				}
			}
		});
        
        Button btnBack = (Button) findViewById(R.id.btnBack);
        btnBack.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				finish();
			}
		});
    
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
	
	private void updateGameItemsOnScreen()
	{
		long items = MNDirect.getVItemsProvider().getPlayerVItemCountById(HEALTH_POTION);
    	txtTitle.setText("Currently, you have " + items + " " + HEALTH_POTION_NAME);
        txtTitle.invalidate();
	}


	@Override
	public boolean handleMessage(Message msg) {
		Log.d("playphone","Received new message");
		Bundle bundle = msg.getData();
		if(bundle.containsKey("updateField"))
		{
			Log.d("playphone","updateField key found, refreshing UI");
			updateGameItemsOnScreen();
			Log.d("playphone","Invalidated field " + bundle.getInt("updateField"));
		}
		if(bundle.containsKey("message"))
			Toast.makeText(this, bundle.getString("message"), Toast.LENGTH_SHORT).show();
		return false;
	}

}
