package com.playphone.sdk.example;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.playphone.multinet.MNDirect;
import com.playphone.multinet.MNDirectUIHelper;
import com.playphone.multinet.providers.MNAchievementsProvider.GameAchievementInfo;
import com.playphone.multinet.providers.MNAchievementsProvider.PlayerAchievementInfo;

public class PostAchievementActivity extends Activity implements OnClickListener{
	EditText editInput;
	TextView txtResult;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.post_achievement);
        
        Button btnUpload = (Button) findViewById(R.id.btnUpload);
        btnUpload.setOnClickListener(this);
        
        editInput = (EditText) findViewById(R.id.editInput);
        txtResult = (TextView) findViewById(R.id.txtResult);
        
        editInput.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
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


	@Override
	public void onClick(View arg0) {
		List<Integer> achievementList = new ArrayList<Integer>();
		for( GameAchievementInfo info : MNDirect.getAchievementsProvider().getGameAchievementsList())
		{
			Log.d("Playphone","Found ID" + info.id + " Description: " + info.description);
			achievementList.add(info.id);
		}
		
		Integer response = Integer.valueOf(editInput.getText().toString());
		
		if(achievementList.contains(response))
		{
			MNDirect.getAchievementsProvider().unlockPlayerAchievement(response);
			txtResult.setText("Unlocked achievement " + response);
		}
		else
		{
			txtResult.setText("Achievement " + response + " does not exist");
		}
		
	}

}
