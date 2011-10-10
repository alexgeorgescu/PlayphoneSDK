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
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.playphone.multinet.MNDirect;
import com.playphone.multinet.providers.MNVItemsProvider;
import com.playphone.multinet.providers.MNVItemsProvider.GameVItemInfo;
import com.playphone.multinet.providers.MNVItemsProvider.PlayerVItemInfo;
import com.playphone.multinet.providers.MNVItemsProvider.TransactionError;
import com.playphone.multinet.providers.MNVItemsProvider.TransactionInfo;
import com.playphone.multinet.providers.MNVShopProvider.VShopPackInfo;

public class InventoryManageActivity extends CustomTitleActivity implements
		OnClickListener, Callback, OnItemSelectedListener {

	Map<String, Integer> itemMap = new HashMap<String, Integer>();
	Handler handler = new Handler(this);

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.inventory_manage);

		// set the breadcrumbs text
		TextView txtBreadCrumbs = (TextView) findViewById(R.id.txtBreadCrumbs);
		txtBreadCrumbs
				.setText("Home > Virtual Economy > Inventory > Manage Inventory");

		List<GameVItemInfo> gameItems = Arrays.asList(MNDirect
				.getVItemsProvider().getGameVItemsList());
		for (GameVItemInfo gameItem : gameItems) {
			itemMap.put(gameItem.name, gameItem.id);
		}

		Spinner spinner = (Spinner) findViewById(R.id.spinnerItem);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				R.layout.array_adapter_layout);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		for (String key : itemMap.keySet())
			adapter.add(key);
		spinner.setAdapter(adapter);
		spinner.setOnItemSelectedListener(this);

		Button btnAdd = (Button) findViewById(R.id.btnAdd);
		btnAdd.setOnClickListener(this);

		Button btnSubstract = (Button) findViewById(R.id.btnSubstract);
		btnSubstract.setOnClickListener(this);

		MNDirect.getVItemsProvider().addEventHandler(
				new MNVItemsProvider.EventHandlerAbstract() {
					public void onVItemsTransactionCompleted(
							TransactionInfo transaction) {
						// transaction succeeded, update the application
						Log.d("playphone", "Transaction "
								+ transaction.clientTransactionId
								+ " succeeded.");
						Message msg = new Message();
						Bundle bundle = new Bundle();
						bundle.putString("message",
								"Transaction was successful");
						msg.setData(bundle);
						handler.sendMessage(msg);
					}

					public void onVItemsTransactionFailed(TransactionError error) {
						// transaction succeeded, update the application
						Log.d("playphone", "Transaction "
								+ error.clientTransactionId + " failed.");
						Message msg = new Message();
						Bundle bundle = new Bundle();
						bundle.putString("message",
								"Transaction failed with error"
										+ error.errorMessage);
						msg.setData(bundle);
						handler.sendMessage(msg);
					}
				});

		// set initial amount
		setInitialAmount();
	}

	@Override
	public void onClick(View v) {
		InputMethodManager imm = (InputMethodManager) v.getContext()
				.getSystemService(INPUT_METHOD_SERVICE);
		switch (v.getId()) {
		case R.id.btnAdd:
			try {
				EditText editAdd = (EditText) findViewById(R.id.editAdd);
				Log.d("playphone", "Adding '" + editAdd.getText().toString()
						+ "' items");
				changeItem(Integer.valueOf(editAdd.getText().toString()));
				editAdd.setText("");
				editAdd.clearFocus();
				imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
			} catch (Exception e) {
				return;
			}
			break;
		case R.id.btnSubstract:
			try {
				EditText editSub = (EditText) findViewById(R.id.editSub);
				Log.d("playphone", "Removing '" + editSub.getText().toString()
						+ "' items");
				changeItem(0 - Integer.valueOf(editSub.getText().toString()));
				editSub.setText("");
				editSub.clearFocus();
				imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
			} catch (Exception e) {
				return;
			}
			break;
		default:
		}
	}

	private void changeItem(int count) {

		if (MNDirect.isUserLoggedIn()) {
			Spinner spinner = (Spinner) findViewById(R.id.spinnerItem);
			int itemToBuy = itemMap.get(spinner.getSelectedItem().toString());
			MNDirect.getVItemsProvider().reqAddPlayerVItem(itemToBuy, count,
					MNDirect.getVItemsProvider().getNewClientTransactionId());
			Toast.makeText(
					this,
					"Trying to change your item "
							+ spinner.getSelectedItem().toString() + " by "
							+ String.valueOf(count), Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public boolean handleMessage(Message msg) {
		Log.d("playphone", "Received new message");
		Bundle bundle = msg.getData();
		if (bundle.containsKey("message")) {
			Toast.makeText(this, bundle.getString("message"),
					Toast.LENGTH_SHORT).show();
			Spinner spinner = (Spinner) findViewById(R.id.spinnerItem);
			int itemChanged = itemMap.get(spinner.getSelectedItem().toString());
			TextView txtAmount = (TextView) findViewById(R.id.txtAmount);
			txtAmount.setText(String.valueOf(MNDirect.getVItemsProvider()
					.getPlayerVItemCountById(itemChanged)));
			txtAmount.invalidate();
		}
		return false;
	}

	private void setInitialAmount() {
		// set initial amount
		if (MNDirect.isUserLoggedIn()) {
			Spinner spinner = (Spinner) findViewById(R.id.spinnerItem);
			TextView txtAmount = (TextView) findViewById(R.id.txtAmount);
			txtAmount
					.setText(String.valueOf(MNDirect.getVItemsProvider()
							.getPlayerVItemCountById(
									itemMap.get(spinner.getSelectedItem()
											.toString()))));
		}
	}

	@Override
	public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
			long arg3) {
		// set initial amount
		setInitialAmount();
	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		// set initial amount
		TextView txtAmount = (TextView) findViewById(R.id.txtAmount);
		txtAmount.setText("0");
	}
}
