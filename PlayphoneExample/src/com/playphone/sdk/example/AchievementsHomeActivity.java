package com.playphone.sdk.example;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.playphone.multinet.MNDirect;
import com.playphone.multinet.providers.MNAchievementsProvider.GameAchievementInfo;
import com.playphone.multinet.providers.MNAchievementsProvider.PlayerAchievementInfo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class AchievementsHomeActivity extends CustomTitleActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.achievement_home);
		
		// set the breadcrumbs text
		TextView txtBreadCrumbs = (TextView) findViewById(R.id.txtBreadCrumbs);
		txtBreadCrumbs.setText("Home > Achievements");
		
			Button btnLeaderboard = (Button) findViewById(R.id.btnUnlockAchievement);
			btnLeaderboard.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					Intent intent = new Intent(AchievementsHomeActivity.this,PostAchievementActivity.class);
					startActivity(intent);
				}
			});
		
		
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		LinearLayout layout = (LinearLayout) findViewById(R.id.layoutAchievements);
		layout.removeAllViews();
		//List<PlayerAchievementInfo> playerAchievements = Arrays.asList(MNDirect.getAchievementsProvider().getPlayerAchievementsList());
		//MNDirect.getAchievementsProvider().getAchievementImageURL(id);
		List<GameAchievementInfo> achievements = Arrays.asList(MNDirect.getAchievementsProvider().getGameAchievementsList());
		
		for(GameAchievementInfo achievement : achievements)
		{
			String textToSet = "ID" + String.valueOf(achievement.id) + "   " +
					achievement.name + "   " + String.valueOf(achievement.points);
			TextView txtAchievement = new TextView(this);
			txtAchievement.setText(textToSet);
			layout.addView(txtAchievement);
		}
		layout.invalidate();
	}
	
}
