package com.h2origamestudio.gpgs;

import android.content.Intent;
import com.google.android.gms.games.leaderboard.OnScoreSubmittedListener;
import com.google.android.gms.games.leaderboard.SubmitScoreResult;

/**
 * This class is to allow the leader board functionality into Unity3D Game Engine.	
 * @author Marcos Zalacain 
 */
public class GPGS_LeaderBoard extends GPGS_BaseUtil implements OnScoreSubmittedListener {

	// We declare a TAG constant to follow debug convention.
	//private static String TAG = "GPGS_LeaderBoard";
	
	// PluginAdapter reference.
	GPGS_PluginAdapter adapter = GPGS_PluginAdapter.mCurrentGPGSAdapter;

	/**
	 * Our method to submit Score Immediate.
	 */
	public void submitScore(String leaderboardId, long score) {
		if (!adapter.mSignedIn)
			return;
		// might want to use API that gives back result of submission
		adapter.mGamesClient.submitScoreImmediate(new OnScoreSubmittedListener() {
			@Override
			public void onScoreSubmitted(int i, SubmitScoreResult result) {
				if (i == com.google.android.gms.games.GamesClient.STATUS_OK) {
					debugLog("SubmitScore success");
					UnitySendMessageSafe("OnGPGSubmitScoreResult", "true");
				} else {
					debugLog("SubmitScore failed with code " + i);
					UnitySendMessageSafe("OnGPGSubmitScoreResult", "false");
				}
			}
		}, leaderboardId, score);
	}

	/**
	 * Our method to show leader-boards.
	 * Note that we don't care at this moment about the result of the activity we're launching with startActivityForResult.
	 */
	public void showLeaderBoards(String leaderBoardId) {
		if (!adapter.mSignedIn)
			return;
		// We get an intent to show a leaderboard for a game.
		Intent intent = adapter.mGamesClient.getLeaderboardIntent(leaderBoardId);
		if (intent != null)
			GPGS_PluginAdapter.mParentActivity.startActivityForResult(intent, GPGS_PluginAdapter.RC_UNUSED);
	}

	/**
	 * Our method to show all leader-boards.
	 * Note that we don't care at this moment about the result of the activity we're launching with startActivityForResult.
	 */
	public void showAllLeaderBoards() {
		if (!adapter.mSignedIn)
			return;
		Intent intent = adapter.mGamesClient.getAllLeaderboardsIntent();
		if (intent != null)
			GPGS_PluginAdapter.mParentActivity.startActivityForResult(intent, GPGS_PluginAdapter.RC_UNUSED);
	}
	
	/* 
	 * Called when a leaderboard score has been submitted.
	 */
	@Override
	public void onScoreSubmitted(int statusCode, SubmitScoreResult result) {
		// TODO: are we sending a response?
		debugLog("Score submitted");
	}
}
