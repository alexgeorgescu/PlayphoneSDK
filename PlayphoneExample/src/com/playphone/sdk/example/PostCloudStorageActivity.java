package com.playphone.sdk.example;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.net.ssl.HandshakeCompletedListener;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.os.*;

import com.playphone.multinet.MNDirect;
import com.playphone.multinet.MNDirectUIHelper;
import com.playphone.multinet.providers.MNAchievementsProvider.GameAchievementInfo;
import com.playphone.multinet.providers.MNAchievementsProvider.PlayerAchievementInfo;
import com.playphone.multinet.providers.MNGameCookiesProvider.IEventHandler;

public class PostCloudStorageActivity extends CustomTitleActivity implements OnClickListener, Handler.Callback{
	EditText editInput;
	TextView txtResult;
	
	
	
	Random rand = new Random();
	Handler handler = new Handler(this);
	
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.post_cloudstorage);
        
        Button btnUpload = (Button) findViewById(R.id.btnWrite);
        btnUpload.setOnClickListener(this);
        
        editInput = (EditText) findViewById(R.id.editInput);
        txtResult = (TextView) findViewById(R.id.txtResult);
        
        editInput.setOnFocusChangeListener(new View.OnFocusChangeListener() {
        	@Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
                else
                {
                	InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                	imm.hideSoftInputFromWindow(editInput.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
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
        
        MNDirect.getGameCookiesProvider().addEventHandler(new IEventHandler() {
			
			@Override
			public void onGameCookieUploadSucceeded(int key) {
				Log.w("playphone", "Upload succeeded for id " + String.valueOf(key));
				Toast.makeText(PostCloudStorageActivity.this, "Upload of cookie id " + String.valueOf(key) + " succeeded", Toast.LENGTH_SHORT).show();
				
			}
			
			@Override
			public void onGameCookieUploadFailedWithError(int key, String error) {
				Log.w("playphone", "Upload failed for id " + String.valueOf(key) + " with errror " + error);
				txtResult.setText("Upload failed with error " + error);
				Toast.makeText(PostCloudStorageActivity.this, "Upload failed with error " + error, Toast.LENGTH_LONG).show();
				
			}
			
			@Override
			public void onGameCookieDownloadSucceeded(int key, String cookie) {
				Log.d("playphone", "Download succeded for id " + String.valueOf(key) + " " + cookie);
				Message msg = Message.obtain();
				Bundle bundle = new Bundle();
				bundle.putString("action","addToResult");
				bundle.putString("append", "ID: " + String.valueOf(key) + " " + cookie + "\n");
				msg.setData(bundle);
				PostCloudStorageActivity.this.handler.sendMessage(msg);

			}
			
			@Override
			public void onGameCookieDownloadFailedWithError(int key, String error) {
				Log.w("playphone", "Download failed for id " + String.valueOf(key) + " with errror " + error);
				txtResult.setText("Download failed with error " + error);
				Toast.makeText(PostCloudStorageActivity.this, "Download failed for id " + String.valueOf(key) + " with errror " + error, Toast.LENGTH_LONG).show();
				
			}
		});
        
        
        Button btnRead = (Button) findViewById(R.id.btnRead);
        btnRead.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				editInput.clearFocus();
				if(MNDirect.isUserLoggedIn())
				{
					txtResult.setText("");
					for(int i=0;i<5;i++)
					{
							MNDirect.getGameCookiesProvider().downloadUserCookie(i);
							Log.d("playphone", "Download started for cookie with id " + String.valueOf(i));
					}
				}
				else
				{
					Log.d("playphone","User not logged in yet, cloud functionlity disabled");
					Toast.makeText(v.getContext(), "User needs to be logged in to for cloud storage functionality to work",Toast.LENGTH_LONG).show();
				}
			}
		});
        
        //handler = new Handler();
    }
    
    
    
	@Override
	public void onClick(View arg0) {
		editInput.clearFocus();
		if(MNDirect.isUserLoggedIn())
		{
		int random_int = rand.nextInt(5);
		MNDirect.getGameCookiesProvider().
				uploadUserCookie(random_int,editInput.getText().toString());
		Toast.makeText(this, "Uploaded message to cloud with id " + String.valueOf(random_int), Toast.LENGTH_SHORT).show();
		}
		else
		{
			Log.d("playphone","User not logged in yet, cloud functionlity disabled");
			Toast.makeText(this, "User needs to be logged in to for cloud storage functionality to work",Toast.LENGTH_LONG).show();
		}
	}


	@Override
	public boolean handleMessage(Message msg) {
		if("addToResult".equalsIgnoreCase(msg.getData().getString("action")))
		{
			String dataToAdd = msg.getData().getString("append");
			txtResult.setText(txtResult.getText() + dataToAdd);
			txtResult.invalidate();
			Log.d("playphone", "HandleMessage: " + txtResult.getText().toString());
			// takes too long and not necessary
			//Toast.makeText(PostCloudStorageActivity.this, txtResult.getText().toString(), Toast.LENGTH_SHORT).show();;
		}
		return true;
	}

	
	
	
}
