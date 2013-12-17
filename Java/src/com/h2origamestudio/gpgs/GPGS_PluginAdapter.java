package com.h2origamestudio.gpgs;

import java.util.ArrayList;
import java.util.Vector;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.WindowManager;
import com.google.android.gms.appstate.AppStateClient;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.games.GamesActivityResultCodes;
import com.google.android.gms.games.GamesClient;
import com.google.android.gms.games.OnSignOutCompleteListener;
import com.google.android.gms.games.multiplayer.Invitation;
import com.google.android.gms.games.multiplayer.realtime.Room;
import com.google.android.gms.games.multiplayer.realtime.RoomConfig;
import com.google.android.gms.plus.PlusClient;
import com.unity3d.player.UnityPlayer;

/**
 * This adapter class is to allow GPGS functionality into Unity3D Game Engine.
 * It's the headquarters of the plugin.
 * It's also in charge of the signing in/out process.
 * Tones of code on this plugin strictly follows the Google Play Game Services API / Documentation @:
 * 		https://developers.google.com/games/services/
 * 
 * @author Marcos Zalacain 
 */
public class GPGS_PluginAdapter extends GPGS_BaseUtil implements GooglePlayServicesClient.ConnectionCallbacks,
		GooglePlayServicesClient.OnConnectionFailedListener, OnSignOutCompleteListener {

	// We declare a TAG constant to follow debug convention.
	private static String TAG = "GPGS_PluginAdapter";

	// Activity.
	public static Activity mParentActivity = null;
	// Activity to launch.
	public static Activity mDummyActivity = null; 
	// Reference to object of this class.
	public static GPGS_PluginAdapter mCurrentGPGSAdapter = null; 
	 
	// Our Plugin classes references
	GPGS_Achievements mAchievements = null;
	GPGS_LeaderBoard mLeaderBoard = null;
	GPGS_CloudSave mCloudSave = null;
	GPGS_Multiplayer mMultiplayer = null;

	// Client objects we manage. If a given client is not enabled, it is null.
	GamesClient mGamesClient = null;
	PlusClient mPlusClient = null;
	String[] mScopes = null;

	// What clients we manage (OR-able values, can be combined as flags).
	public final static int CLIENT_NONE = 0x00;
	public final static int CLIENT_GAMES = 0x01; 
	public final static int CLIENT_PLUS = 0x02;
	public final static int CLIENT_APPSTATE = 0x04;
	public final static int CLIENT_PLUSPROFILE = 0x08;
	public final static int CLIENT_ALL = CLIENT_GAMES | CLIENT_PLUS	| CLIENT_APPSTATE;

	// Request Code Resolve ( when coming back from an activity that was launched to resolve a connection problem.).
	final static int RC_RESOLVE = 9001;
	// Request code when invoking Activities whose result we don't care about.
	final static int RC_UNUSED = 9002;
	// What clients were requested? (bit flags)
	int mRequestedClients = CLIENT_NONE;
	// What clients are currently connected? (bit flags)
	int mConnectedClients = CLIENT_NONE;
	// What client are we currently connecting?
	int mClientCurrentlyConnecting = CLIENT_NONE;
	// Whether to automatically try to sign in on onStart().
	boolean mAutoSignIn = true;

	// Request code for Invitation in-box.
	final static int RC_INVITATION_INBOX = 10001;
	// Request code for the "select players" UI.
	final static int RC_SELECT_PLAYERS = 10000;
	// Arbitrary request code for the waiting room UI.
	final static int RC_WAITING_ROOM = 10002;	

	/*
	 * Whether user has specifically requested that the sign-in process begin.
	 * If mUserInitiatedSignIn is false, we're in the automatic sign-in attempt
	 * that we try once the Activity is started -- if true, then the user has
	 * already clicked a "Sign-In" button or something similar
	 */
	boolean mUserInitiatedSignIn = false;

	// The connection result we got from our last attempt to sign-in.
	public static ConnectionResult mConnectionResult = null;

	// Whether our sign-in attempt resulted in an error. In this case,
	// mConnectionResult indicates what was the error we failed to resolve.
	boolean mSignInError = false;
	// Whether we launched the sign-in dialog flow and therefore are expecting
	// an onActivityResult with the result of that.
	boolean mExpectingActivityResult = false;

	// Are we signed in?
	boolean mSignedIn = false;	
	// isGooglePlayServicesAvailable error code.
	static int gErrorCode = 0;

	/**
	 * Constructor. Sets the Unity current Activity.
	 */
	public GPGS_PluginAdapter() {
		mCurrentGPGSAdapter = this; 
		 mAchievements = new GPGS_Achievements(mCurrentGPGSAdapter);
		 mLeaderBoard = new GPGS_LeaderBoard(mCurrentGPGSAdapter);
		 mCloudSave = new GPGS_CloudSave(mCurrentGPGSAdapter);
		 mMultiplayer = new GPGS_Multiplayer(mCurrentGPGSAdapter);
		mParentActivity = UnityPlayer.currentActivity;
		debugLog("ParentActivity name is " + mParentActivity.getClass().getName());
	}	

	/**
	 * Performs setup on this object. This will create the clients and do a few
	 * other initialization tasks.
	 */
	public boolean init(final Activity activity) {
		if (activity == null) {
			Log.d(TAG, "ParentActivity handle required");
			return false;
		}

		mParentActivity = activity;
		debugLog("ParentActivity name is: "	+ mParentActivity.getClass().getName());
		gErrorCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(activity);
		// We show a dialog if Connection isn't successful.
		if (gErrorCode != ConnectionResult.SUCCESS) {
			debugLog("GooglePlay services not found on version " + gErrorCode);
			// We use runOnUiThread because UI operation cannot run on worker thread...
			UnityPlayer.currentActivity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Dialog dialog = GooglePlayServicesUtil.getErrorDialog(gErrorCode, activity, 69);
					if (dialog != null)
						dialog.show();
					else
						Log.d("GooglePlayServicesUtil", "Null dialog");
				}
			});
			return false;
		}

		// Connect all client types.
		mRequestedClients = CLIENT_ALL;
		// Set scopes in a synchronized collection.
		Vector<String> scopesVector = new Vector<String>();
		if ((mRequestedClients & CLIENT_GAMES) != 0)
			scopesVector.add(Scopes.GAMES);
		if ((mRequestedClients & CLIENT_PLUS) != 0)
			scopesVector.add(Scopes.PLUS_LOGIN);
		if ((mRequestedClients & CLIENT_APPSTATE) != 0)
			scopesVector.add(Scopes.APP_STATE);
		// scopesVector.add(Scopes.PLUS_PROFILE);

		// We copy scopesVector into our array mScopes.
		mScopes = new String[scopesVector.size()];
		scopesVector.copyInto(mScopes);

		// We build our Client.
		mGamesClient = new GamesClient.Builder(mParentActivity, this, this)
				.setGravityForPopups(Gravity.TOP | Gravity.CENTER_HORIZONTAL)
				.setScopes(mScopes).create();
		// We configure a PlusClient (entry point for Google+ integration).
		mPlusClient = new PlusClient.Builder(mParentActivity, this, this)
				.setScopes(mScopes).build();
		// We set our entry point for the AppState client.
		mCloudSave.mAppStateClient = new AppStateClient.Builder(
				mParentActivity, this, this).setScopes(mScopes).create();

		return false;
	}

	/**
	 * Overriding method. Sends a Unity message when sign out has completed.
	 */
	@Override
	public void onSignOutComplete() {
		if (mGamesClient.isConnected())
			mGamesClient.disconnect();
		UnitySendMessageSafe("GPGSAuthenticationResult", "signedout");

	}

	/**
	 * Overriding method. Handles a connection failure reported by a client.
	 * Tries to resolve error and sends a Unity message if error can't be
	 * resolved.
	 */
	@Override
	public void onConnectionFailed(ConnectionResult result) {
		// save connection result for later reference
		mConnectionResult = result;
		debugLog("onConnectionFailed: result " + result.getErrorCode());
		if (result.hasResolution())
			resolveConnectionResult();
		else
			UnityPlayer.UnitySendMessage(TAG, "GPGSAuthenticationResult", "error");

	}

	/**
	 * Overriding method. Called when we successfully obtain a connection to a
	 * client.
	 */
	@Override
	public void onConnected(Bundle connectionHint) {
		debugLog("onConnected: connected! client=" + mClientCurrentlyConnecting);

		// Mark the current client as connected
		mConnectedClients |= mClientCurrentlyConnecting;

		// =========================================================================ROOM RELATED
		// If this was the games client and it came with an invite, store it for
		// later retrieval.
		if (mClientCurrentlyConnecting == CLIENT_GAMES && connectionHint != null) {
			debugLog("onConnected: connection hint provided. Checking for invite."); 
			Invitation inv = connectionHint.getParcelable(GamesClient.EXTRA_INVITATION);
			if (inv != null && inv.getInvitationId() != null) { 
				// accept invitation
				debugLog("onConnected: connection hint has a room invite!");
				mMultiplayer.mInvitationId = inv.getInvitationId();
				debugLog("Invitation ID: " + mMultiplayer.mInvitationId);

				// 
				// We create a builder of room configuration, a
				// RoomConfigBuilder.
				RoomConfig.Builder roomConfigBuilder = mMultiplayer.makeBasicRoomConfigBuilder();
				// we set the ID of the invitation to accept. 
				roomConfigBuilder.setInvitationIdToAccept(mMultiplayer.mInvitationId);
				// we make our client to join the real-time room by accepting an
				// invitation.
				mGamesClient.joinRoom(roomConfigBuilder.build());
				// we prevent screen from sleeping during handshake. 
				// Don't forget to clear this flag at the end of game-play or
				// when the game is cancelled.
				mParentActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); 

				// We register an OnInvitationReceivedListener after the player
				// signs in successfully.
				mGamesClient.registerInvitationListener(mMultiplayer);
				// =================================================================ROOM RELATED
			}
		}

		// connect the next client in line, if any.
		connectNextClient();
	}

	/**
	 * Overriding method. Called when we are disconnected from a client. Sends a
	 * Unity message.
	 */
	@Override
	public void onDisconnected() {
		debugLog("onDisconnected.");
		mConnectionResult = null;
		mAutoSignIn = false;
		mSignedIn = false;
		mSignInError = false;
		mMultiplayer.mInvitationId = null;
		mConnectedClients = CLIENT_NONE;
		UnitySendMessageSafe("GPGSAuthenticationResult", "signedout");
	}
 
	/**
	 * Static method to show an android dialog.
	 */
	public static void showOkDialogWithText(Context context, String messageText) {
		Builder builder = new AlertDialog.Builder(context);
		builder.setMessage(messageText);
		builder.setCancelable(true);
		builder.setPositiveButton("OK", null);
		AlertDialog dialog = builder.create();
		dialog.show();
	}

	/**
	 * Getter for mSignedIn.
	 */
	public boolean ismSignedIn() {
		return mSignedIn;
	}

	/**
	 * Our method to check whether we are signed in.
	 */
	public boolean hasAuthorised() {
		return ismSignedIn();
	}

	/**
	 * Our method to get the player name.
	 */
	public String getPlayerName() {
		if (!mSignedIn)
			return null;
		return mGamesClient.getCurrentPlayer().getDisplayName();
	}

	/**
	 * Our method to get the player ID.
	 */
	public String getPlayerID() {
		if (!mSignedIn)
			return null;
		return mGamesClient.getCurrentPlayer().getPlayerId();
	}

	/** Sign out and disconnect from the APIs. */
	public void signOut() {
		mConnectionResult = null;
		mAutoSignIn = false;
		mSignedIn = false;
		mSignInError = false;

		// for the PlusClient, "signing out" means clearing the default account
		// and then disconnecting.
		if (mPlusClient != null && mPlusClient.isConnected()) {
			mPlusClient.clearDefaultAccount();
		}
		// For the games client, signing out means calling signOut and
		// disconnecting.
		if (mGamesClient != null && mGamesClient.isConnected()) {
			// showProgressDialog(false);
			mGamesClient.signOut(this);
		}

		// kill connects to all clients but games, which must remain
		// connected till we get onSignOutComplete()
		killConnections(CLIENT_ALL & ~CLIENT_GAMES);
	}

	/** kill Connections to all clients. */
	void killConnections(int whatClients) {
		if ((whatClients & CLIENT_GAMES) != 0 && mGamesClient != null && mGamesClient.isConnected()) {
			mConnectedClients &= ~CLIENT_GAMES;
			mGamesClient.disconnect();
		}
		if ((whatClients & CLIENT_PLUS) != 0 && mPlusClient != null	&& mPlusClient.isConnected()) {
			mConnectedClients &= ~CLIENT_PLUS;
			mPlusClient.disconnect();
		}
		if ((whatClients & CLIENT_APPSTATE) != 0
				&& mCloudSave.mAppStateClient != null
				&& mCloudSave.mAppStateClient.isConnected()) {
			mConnectedClients &= ~CLIENT_APPSTATE;
			mCloudSave.mAppStateClient.disconnect();
		}
	}

	/** ?? */
	public boolean silentSignIn() {
		return signIn();
	}

	/** ?? */
	public boolean signIn() {
		if (hasAuthorised())
			return true; // already authorised
		mConnectedClients = CLIENT_NONE;
		// mInvitationId = null;
		connectNextClient();
		return true;
	}

	/** ?? */
	private void connectNextClient() {
		// do we already have all the clients we need?
		int pendingClients = mRequestedClients & ~mConnectedClients;
		if (pendingClients == 0) {
			debugLog("All clients now connected. Sign-in successful.");
			succeedSignIn();
			return;
		}

		// which client should be the next one to connect?
		if (mGamesClient != null && (0 != (pendingClients & CLIENT_GAMES))) {
			debugLog("Connecting GamesClient.");
			mClientCurrentlyConnecting = CLIENT_GAMES;
		} else if (mPlusClient != null && (0 != (pendingClients & CLIENT_PLUS))) {
			debugLog("Connecting PlusClient.");
			mClientCurrentlyConnecting = CLIENT_PLUS;
		} else if (mCloudSave.mAppStateClient != null && (0 != (pendingClients & CLIENT_APPSTATE))) {
			debugLog("Connecting AppStateClient.");
			mClientCurrentlyConnecting = CLIENT_APPSTATE;
		} else {
			// hmmm, getting here would be a bug.
			throw new AssertionError("Not all clients connected, yet no one is next. R="
							+ mRequestedClients + ", C=" + mConnectedClients);
		}

		connectCurrentClient();
	}

	/** ?? */
	void connectCurrentClient() {
		switch (mClientCurrentlyConnecting) {
		case CLIENT_GAMES:
			mParentActivity.runOnUiThread(new Runnable() {
				public void run() {
					mGamesClient.connect();
				}
			});
			break;
		case CLIENT_APPSTATE:
			mParentActivity.runOnUiThread(new Runnable() {
				public void run() {
					mCloudSave.mAppStateClient.connect();
				}
			});
			break;
		case CLIENT_PLUS:
			mParentActivity.runOnUiThread(new Runnable() {
				public void run() {
					mPlusClient.connect();
				}
			});
			break;
		}
	}

	/** ?? */
	void succeedSignIn() {
		debugLog("All requested clients connected. Sign-in succeeded!");
		mSignedIn = true;
		mSignInError = false;
		mAutoSignIn = true;
		mUserInitiatedSignIn = false;
		UnitySendMessageSafe("GPGSAuthenticationResult", "success");
	}

	/**
	 * Attempts to resolve a connection failure. This will usually involve
	 * starting a UI flow that lets the user give the appropriate consents
	 * necessary for sign-in to work.
	 */
	void resolveConnectionResult() {
		// Try to resolve the problem
		debugLog("resolveConnectionResult: trying to resolve result: " + mConnectionResult);
		if (mConnectionResult.hasResolution()) {
			// This problem can be fixed. So let's try to fix it.
			debugLog("result has resolution. Starting it.");
			try {
				// launch appropriate UI flow (which might, for example, be the sign-in flow)
				mExpectingActivityResult = true;
				debugLog("Resolving intent with activity " + mParentActivity);
				// create dummy activity
				/*
				 * mDummyActivity = new GPGS_DummyActivity(); 
				 * Intent intent = new Intent(); 
				 * mDummyActivity.startActivity(intent);
				 */

				Intent intent = new Intent(mParentActivity, GPGS_DummyActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
				// mParentActivity.startActivity(intent);
				mParentActivity.startActivityForResult(intent, RC_UNUSED);

				// mConnectionResult.startResolutionForResult(mParentActivity, RC_RESOLVE);
				// PendingIntent intent = mConnectionResult.getResolution();
				// mParentActivity.startActivityForResult((Intent)intent, RC_RESOLVE);
			} catch (Exception e) {
				// Try connecting again
				debugLog("SendIntentException.");
				connectCurrentClient();
			}
		} else {
			// It's not a problem what we can solve, so give up and show an error.
			debugLog("resolveConnectionResult: result has no resolution. Giving up.");
			giveUp();
		}
	}

	/**
	 * Give up on signing in due to an error. Sends a Unity message to the user
	 * so he knows the cause of the error. That message should indicate to the
	 * user how the problem can be solved (for example, re-enable Google Play
	 * Services, upgrade to a new version, etc).
	 */
	void giveUp() {
		mSignInError = true;
		mAutoSignIn = false;
		debugLog("giveUp: giving up on connection. "
				+ ((mConnectionResult == null) ? "(no connection result)"
						: ("Status code: " + mConnectionResult.getErrorCode())));

		if (mConnectionResult != null) {
			UnitySendMessageSafe("GPGSAuthenticationResult", "error," + mConnectionResult.getErrorCode());
		} else {
			// this is a bug
			Log.e("GameHelper", "giveUp() called with no mConnectionResult");
			UnitySendMessageSafe("GPGSAuthenticationResult", "error");
		}
	}

	// ACTIVITY RESULT
	/**
	 * Handle activity result. Call this method from your Activity's
	 * onActivityResult callback. If the activity result pertains to the sign-in
	 * process, processes it appropriately. MZ
	 */
	public void onActivityResult(int requestCode, int responseCode,	Intent intent) {
		if (responseCode != Activity.RESULT_CANCELED) {
			if (requestCode == RC_RESOLVE) {
				// We're coming back from an activity that was launched to resolve a
				// connection problem. For example, the sign-in UI.
				mExpectingActivityResult = false;
				debugLog("onActivityResult, req " + requestCode + " response " + responseCode);
				if (responseCode == Activity.RESULT_OK) {
					// Ready to try to connect again.
					debugLog("responseCode == RESULT_OK. So connecting.");
					connectCurrentClient();
				} else {
					// Whatever the problem we were trying to solve, it was not solved.
					// So give up and show an error message.
					debugLog("responseCode != RESULT_OK, so not reconnecting.");
					giveUp();
				}
			}
		}

		// We got the result from the invitation inbox and we're ready to accept the selected invitation.
		if (requestCode == RC_INVITATION_INBOX) {
			if (responseCode != Activity.RESULT_OK)
				return;
			// get the selected invitation.
			Bundle extras = intent.getExtras();
			Invitation invitation = extras
					.getParcelable(GamesClient.EXTRA_INVITATION);
			// Accept it
			RoomConfig roomConfig = mMultiplayer.makeBasicRoomConfigBuilder()
					.setInvitationIdToAccept(invitation.getInvitationId())
					.build();
			mGamesClient.joinRoom(roomConfig);
			// prevent screen from sleeping during handshake
			mParentActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
			// go to game screen
			UnitySendMessageSafe("onActivityResult", "RC_INVITATION_INBOX");
			debugLog("requestCode == RC_INVITATION_INBOX");
		}

		// We got the result from the "select players" UI and we're ready to create the room.
		if (requestCode == RC_SELECT_PLAYERS) {
			if (responseCode != Activity.RESULT_OK)
				return;

			// get the invitee list
			// Bundle extras = intent.getExtras();
			final ArrayList<String> invitees = intent.getStringArrayListExtra(GamesClient.EXTRA_PLAYERS);
			// get auto-match criteria
			Bundle autoMatchCriteria = null;
			int minAutoMatchPlayers = intent.getIntExtra(GamesClient.EXTRA_MIN_AUTOMATCH_PLAYERS, 0);
			int maxAutoMatchPlayers = intent.getIntExtra(GamesClient.EXTRA_MAX_AUTOMATCH_PLAYERS, 0);
			if (minAutoMatchPlayers > 0) {
				autoMatchCriteria = RoomConfig.createAutoMatchCriteria(
						minAutoMatchPlayers, maxAutoMatchPlayers, 0);
			} else {
				autoMatchCriteria = null;
			}
			// create the room and specify a variant if appropriate
			RoomConfig.Builder roomConfigBuilder = mMultiplayer.makeBasicRoomConfigBuilder();
			roomConfigBuilder.addPlayersToInvite(invitees);
			if (autoMatchCriteria != null) {
				roomConfigBuilder.setAutoMatchCriteria(autoMatchCriteria);
			}
			RoomConfig roomConfig = roomConfigBuilder.build();
			mGamesClient.createRoom(roomConfig);
			// prevent screen from sleeping during handshake
			mParentActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
			UnitySendMessageSafe("onActivityResult", "RC_SELECT_PLAYERS");
			debugLog("requestCode == RC_SELECT_PLAYERS");
		}

		// We got the result from the "waiting room" and we're ready to beging the game.
		if (requestCode == RC_WAITING_ROOM) {
			if (responseCode == Activity.RESULT_OK) {
				UnitySendMessageSafe("onActivityResult", "RC_WAITING_ROOM");
				debugLog("requestCode == RC_WAITING_ROOM + responseCode == Activity.RESULT_OK");
				debugLog("Unity onActivityResult called");
			}
			// The user quit the waiting room UI.
			else if (responseCode == Activity.RESULT_CANCELED) {
				// Waiting room was dismissed with the back button. The meaning of this
				// action is up to the game. You may choose to leave the room and cancel the
				// match, or do something else like minimise the waiting room and
				// continue to connect in the background.

				// in this example, we take the simple approach and just leave the room:
				// NAI - NAI - NAI: we've commented this as backButton should do nothing.
				
				// get the selected Room.
				// Bundle extras = intent.getExtras();
				// Room room = extras.getParcelable(GamesClient.EXTRA_ROOM);
				// mGamesClient.leaveRoom(this, room.getRoomId());
				
				mParentActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
				UnitySendMessageSafe("onActivityResult", "RESULT_CANCELED");
				debugLog("requestCode == RC_WAITING_ROOM + responseCode == Activity.RESULT_CANCELED");
			}
			// The user explicitly cancelled the game.
			else if (responseCode == GamesActivityResultCodes.RESULT_LEFT_ROOM) {
				// player wants to leave the room.
				// get the selected Room.
				Bundle extras = intent.getExtras();
				Room room = extras.getParcelable(GamesClient.EXTRA_ROOM);
				mGamesClient.leaveRoom(mMultiplayer, room.getRoomId());
				mParentActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
				debugLog("requestCode == RC_WAITING_ROOM + responseCode == Activity.RESULT_LEFT_ROOM");
			}
		}
	}
	
	//********************************//
	//--------  GPGS Methods  --------//
	//********************************//
	
	// Achievements:
	public void unlockAchievement(String achievementId) { mAchievements.unlockAchievement(achievementId); }
	public void incrementAchievement(String achievementId, int numSteps) { mAchievements.incrementAchievement(achievementId, numSteps); }
	public void loadAchievements(boolean bForceReload) { mAchievements.loadAchievements(bForceReload); }
	public void showAchievements() { mAchievements.showAchievements(); }
	// Leader Board:
	public void submitScore(String leaderboardId, long score) { mLeaderBoard.submitScore(leaderboardId, score); }
	public void showLeaderBoards(String leaderBoardId) { mLeaderBoard.showLeaderBoards(leaderBoardId); }
	public void showAllLeaderBoards() { mLeaderBoard.showAllLeaderBoards(); }
	// Cloud Save: 
	public void saveToCloud(int keyNum, String bytes) { mCloudSave.saveToCloud(keyNum, bytes); }
	public void saveToCloudImmediate(int keyNum, String bytes) { mCloudSave.saveToCloudImmediate(keyNum, bytes); }
	public void loadFromCloud(int keyNum) { mCloudSave.loadFromCloud(keyNum); }
	public String getLoadedData(int keyNum) { return mCloudSave.getLoadedData(keyNum); }
	// Multiplayer:
	public String getCurrRoomId() { return mMultiplayer.getCurrRoomId(); } 
	public String getCurrRoomParticipantCreatorId() { return mMultiplayer.getCurrRoomParticipantCreatorId(); }
	public String getCurrRoomParticipantJoinerId() { return mMultiplayer.getCurrRoomParticipantJoinerId(); }
	public String getCurrRoomParticipantCreatorDN() { return mMultiplayer.getCurrRoomParticipantCreatorDN(); }
	public String getCurrRoomParticipantJoinerDN() { return mMultiplayer.getCurrRoomParticipantJoinerDN(); }
	public void acceptIncomingInvitation() { mMultiplayer.acceptIncomingInvitation(); }
	public void launchInvitationInbox() { mMultiplayer.launchInvitationInbox(); } 
	public void startQuickGame() { mMultiplayer.startQuickGame(); } 
	public void invitePlayers() { mMultiplayer.invitePlayers(); } 
	public int sendReliableRealTimeMessage(byte[] messageData, String roomId, String recipientParticipantId) { 
		return mMultiplayer.sendReliableRealTimeMessage(messageData, roomId, recipientParticipantId); }
	public void sendReliableRealTimeMessageToAll(byte[] messageData, String roomId) { mMultiplayer.sendReliableRealTimeMessageToAll(messageData, roomId); }
}
