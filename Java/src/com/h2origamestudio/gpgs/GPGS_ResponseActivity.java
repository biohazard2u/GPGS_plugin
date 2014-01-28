package com.h2origamestudio.gpgs;

import com.google.android.gms.games.GamesClient;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;

public class GPGS_ResponseActivity extends Activity{
	
	GamesClient gC;
		
	int actionToTake = 0; 

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
	}

	@Override
	protected void onStart() {
		super.onStart();
		gC = GPGS_PluginAdapter.mCurrentGPGSAdapter.mGamesClient;
		Intent intent = getIntent();
		actionToTake = intent.getIntExtra("actionToTake", 0);
		Log.d("GPGS_BaseUtil", "GPGS_ResponseActivity - onStart: intent value = " + actionToTake);
		Intent newIntent;
		switch (actionToTake) {  
		case GPGS_PluginAdapter.RC_TB_LOOK_AT_MATCHES:
			newIntent = gC.getMatchInboxIntent(); 
			startActivityForResult(newIntent, GPGS_PluginAdapter.RC_TB_LOOK_AT_MATCHES);
			break;
		case GPGS_PluginAdapter.RC_TB_SELECT_PLAYERS:
			newIntent = gC.getSelectPlayersIntent(GPGS_TurnBasedMultiplayer.TB_MIN_PLAYERS, GPGS_TurnBasedMultiplayer.TB_MAX_PLAYERS, true);	// true is to allow auto-match.
			startActivityForResult(newIntent, GPGS_PluginAdapter.RC_TB_SELECT_PLAYERS);  
			break;
		// TODO: bellow cases could be handle, although we don't care about them at this moment.
		/*case 3: 
			// same as above with minimum and maximum number of players specified. 
			break;
		case GPGS_PluginAdapter.RC_INVITATION_INBOX:
			// startActivityForResult from GPGS_Multiplayer with GPGS_PluginAdapter.RC_INVITATION_INBOX.
			break;
		case GPGS_PluginAdapter.RC_SELECT_PLAYERS:
			// startActivityForResult from GPGS_Multiplayer with GPGS_PluginAdapter.RC_SELECT_PLAYERS.
			break;
		case GPGS_PluginAdapter.RC_WAITING_ROOM:
			// startActivityForResult from GPGS_Multiplayer with GPGS_PluginAdapter.RC_WAITING_ROOM.
			break;
		case GPGS_Achievements.showAchievements() methods startActivityForResult:
			// startActivityForResult from GPGS_Achievements.showAchievements() with GPGS_PluginAdapter.RC_UNUSED.
			break;	
		case GPGS_LeaderBoard.showLeaderBoards() methods startActivityForResult:
			// startActivityForResult from GPGS_Achievements.showLeaderBoards() with GPGS_PluginAdapter.RC_UNUSED.
			break;	
		case GPGS_LeaderBoard.showAllLeaderBoards() methods startActivityForResult:
			// startActivityForResult from GPGS_LeaderBoard.showAllLeaderBoards() with GPGS_PluginAdapter.RC_UNUSED.
			break;	
		*/
		} 
		
	}

	@Override
	protected void onDestroy() {
		super.onDestroy(); 
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		Log.d("GPGS_BaseUtil", "GPGS_ResponseActivity - onActivityResult: resultCode = " + resultCode);

		GPGS_PluginAdapter.mCurrentGPGSAdapter.onActivityResult(requestCode, resultCode, data);
		finish();
	}

}
