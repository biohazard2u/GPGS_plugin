package com.h2origamestudio.gpgs;

import android.content.Intent;
import com.google.android.gms.games.leaderboard.OnScoreSubmittedListener;
import com.google.android.gms.games.leaderboard.SubmitScoreResult;

public class GPGS_LeaderBoard extends GPGS_BaseUtil implements OnScoreSubmittedListener {

	// We declare a TAG constant to follow debug convention.
	//private static String TAG = "GPGS_LeaderBoard";
	
	// PluginAdapter reference.
	GPGS_PluginAdapter adapter;

	/**
	 * Constructor 
	 * @param adapter - a reference to the GPGS_PluginAdapter.
	 */
	GPGS_LeaderBoard(GPGS_PluginAdapter adapter) {
		this.adapter = adapter;
	}

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
