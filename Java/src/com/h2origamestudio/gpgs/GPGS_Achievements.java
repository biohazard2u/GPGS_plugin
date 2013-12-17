package com.h2origamestudio.gpgs;

import android.content.Intent;
import com.google.android.gms.games.achievement.Achievement;
import com.google.android.gms.games.achievement.AchievementBuffer;
import com.google.android.gms.games.achievement.OnAchievementUpdatedListener;
import com.google.android.gms.games.achievement.OnAchievementsLoadedListener;

public class GPGS_Achievements extends GPGS_BaseUtil implements OnAchievementUpdatedListener {

	// We declare a TAG constant to follow debug convention.
	//private static String TAG = "GPGS_Achievements";		

	// PluginAdapter reference. 
	GPGS_PluginAdapter adapter; 

	// Data structure providing access to a list of achievements. 
	AchievementBuffer mAchievements = null;	
	 
	/**
	 * Constructor
	 * @param adapter - a reference to the GPGS_PluginAdapter.
	 */
	GPGS_Achievements(GPGS_PluginAdapter adapter){
		this.adapter = adapter;
	}	

	/**
	 * Our method to unlock Achievement Immediate.
	 */
	public void unlockAchievement(String achievementId) {
		if (!adapter.mSignedIn)
			return;
		adapter.mGamesClient.unlockAchievementImmediate(this, achievementId);
	}

	/**
	 * Our method to increment Achievement Immediate.
	 */
	public void incrementAchievement(String achievementId, int numSteps) {
		if (!adapter.mSignedIn)
			return;
		adapter.mGamesClient.incrementAchievementImmediate(this, achievementId,
				numSteps);
	}

	/**
	 * Our method to load Achievements. Asynchronously load achievement data for
	 * the currently signed in player. uses a listener that is called on the
	 * main thread when the load is complete.
	 * 
	 * @param boolean bForceReload: if true, this call will clear any locally
	 *        cached data and attempt to fetch the latest data from the server.
	 *        This would commonly be used for something like a user-initiated
	 *        refresh. Normally, this should be set to false to gain advantages
	 *        of data caching.
	 */
	public void loadAchievements(boolean bForceReload) {
		adapter.mGamesClient.loadAchievements(
				new OnAchievementsLoadedListener() {
					@Override
					public void onAchievementsLoaded(int i,	AchievementBuffer achievements) {
						if (i == com.google.android.gms.games.GamesClient.STATUS_OK) {
							String achs = "";
							for (int a = 0; a < achievements.getCount(); a++) {
								Achievement ach = achievements.get(a);
								debugLog("Achievement name " + ach.getName()
										+ " type " + ach.getType() + " state "
										+ ach.getState());
								String totSteps = "0", currentSteps = "0";
								if (ach.getType() == Achievement.TYPE_INCREMENTAL) {
									totSteps = "" + ach.getTotalSteps();
									currentSteps = "" + ach.getCurrentSteps();
								}

								String sach = ach.getAchievementId() + ";"
										+ ach.getName() + ";" + ach.getType()
										+ ";" + ach.getDescription() + ";"
										+ ach.getState() + ";" + totSteps + ";"
										+ currentSteps;

								achs = achs + sach + "\n";
							}
							UnitySendMessageSafe("OnAchievementsLoaded", achs);
						} else {
							String result = null;
							switch (i) {
							case com.google.android.gms.games.GamesClient.STATUS_NETWORK_ERROR_NO_DATA:
								result = "STATUS_NETWORK_ERROR_NO_DATA";
								break;
							case com.google.android.gms.games.GamesClient.STATUS_NETWORK_ERROR_STALE_DATA:
								result = "STATUS_NETWORK_ERROR_STALE_DATA";
								break;
							case com.google.android.gms.games.GamesClient.STATUS_CLIENT_RECONNECT_REQUIRED:
								result = "STATUS_CLIENT_RECONNECT_REQUIRED";
								break;
							case com.google.android.gms.games.GamesClient.STATUS_LICENSE_CHECK_FAILED:
								result = "STATUS_LICENSE_CHECK_FAILED";
								break;
							case com.google.android.gms.games.GamesClient.STATUS_INTERNAL_ERROR:
								result = "STATUS_INTERNAL_ERROR";
								break;
							}
							UnitySendMessageSafe("OnAchievementsLoadFailed", result);
						}
					}
				}, bForceReload);
	}

	/**
	 * Our method to show Achievements.
	 */
	public void showAchievements() {
		if (!adapter.mSignedIn)
			return;
		Intent intent = adapter.mGamesClient.getAchievementsIntent();
		if (intent != null)
			GPGS_PluginAdapter.mParentActivity.startActivityForResult(intent, GPGS_PluginAdapter.RC_UNUSED);
	}

	/**
	 * Overriding method. Sends message with success/fail achievement sync.
	 * Called when achievement data has been loaded.
	 * 
	 * @param statusCode - int: status code
	 * @param achievementID - String: achievement ID
	 */
	@Override
	public void onAchievementUpdated(int statusCode, String achievementID) {
		if (statusCode == com.google.android.gms.games.GamesClient.STATUS_OK)
			debugLog("Achievement sync success");
		else
			debugLog("Achievement sync failed with code " + statusCode);

		UnitySendMessageSafe(
				"OnGPGUnlockAchievementResult",
				((statusCode == com.google.android.gms.games.GamesClient.STATUS_OK) ? "true"
						: "false"));
	}
}
