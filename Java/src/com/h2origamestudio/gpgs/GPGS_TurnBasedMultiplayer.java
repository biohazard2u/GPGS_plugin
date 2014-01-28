package com.h2origamestudio.gpgs;

import java.util.ArrayList;

import android.content.Intent;
import android.os.Bundle;
import android.util.Base64;

import com.google.android.gms.games.GamesClient;
import com.google.android.gms.games.multiplayer.Invitation;
import com.google.android.gms.games.multiplayer.Participant;
import com.google.android.gms.games.multiplayer.realtime.RoomConfig;
import com.google.android.gms.games.multiplayer.turnbased.LoadMatchesResponse;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatchConfig;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMultiplayerListener;

/**
 * This adapter class is to allow turned based Multiplayer functionality into Unity3D Game Engine.	
 * Tones of code on this plugin strictly follows the Google Play Game Services API / Documentation @:
 * 		https://developers.google.com/games/services/android/turnbasedMultiplayer
 * 
 * @author Marcos Zalacain 
 */
public class GPGS_TurnBasedMultiplayer extends GPGS_BaseUtil implements TurnBasedMultiplayerListener {
	
	// The minimum and maximum number of players to select (not including the current player).
	final static int TB_MIN_PLAYERS = 1;
	final static int TB_MAX_PLAYERS = 1;
		
	// The turned based match we are currently on.
	TurnBasedMatch currTBMatch = null;	
	// The data from the current match
	byte[] currGameData;	
	// The participants in the currently active game.
	ArrayList<Participant> currMatchParticipants = null;
	// The participants IDs in the currently active game. Note that ID change across multiple rooms or matches.
	ArrayList<String> currMatchParticipantsIds = null;
	
	
	// *********************************************
	// Methods to get data - called from Unity3D C#:
	// *********************************************
	
    /**
     *  Returns current match id. 
     */
	public String getCurrMatchId() { 
		return currTBMatch.getMatchId();
	} 	
	
	/**
	 *  Display a participant display name.
	 *  @param participantNumber - if participantNumber = 0 => we want the match creator.
	 */
	public String getCurrMatchParticipantDN(int participantNumber) {
		return currMatchParticipants.get(participantNumber).getDisplayName();
	}
	 
	/**
	 *  Returns a String[] that we can pass to Unity with all participants IDs.
	 */
	public String[] getCurrMatchParticipantsId() {  
		String[] participantsIdsArray = new String[currTBMatch.getParticipantIds().size()];
		participantsIdsArray = currTBMatch.getParticipantIds().toArray(participantsIdsArray);
		return participantsIdsArray; 
	}
	
	/**
	 * Return the ID of the participant who updated the match most recently. pN
	 */
	public String getCurrMatchLastUpdatedParticipantId() { 
		debugLog("getCurrMatchLastUpdatedParticipantId: " + currTBMatch.getLastUpdaterId());
		return currTBMatch.getLastUpdaterId();
	}
	
	/**
	 * Retrieves current match data.
	 */
	public String getCurrMatchData() { 
		String data = Base64.encodeToString(currTBMatch.getData(), Base64.DEFAULT);
		debugLog("CurrMatchData: " + data);
		return data;
	}  
	
	
	// *********************************************
	// Action Methods - also called from Unity3D C#:
	// *********************************************
	
	/**
	 *  Displays your games inbox. <br/>
	 *  It uses the GPGS_ResponseActivity to startActivityForResult.
	 *  a pseudo onActivityResult will be called from GPGS_ResponseActivity.
	 */
	public void displayGameInbox(){  
		startMyPersonalActivityForResult(GPGS_PluginAdapter.RC_TB_LOOK_AT_MATCHES);
	}
	 
	/**
	 *	This method launches the default user interface to select players. It Open the create-game UI. <br/><br/>
	 *  The game will display a default player selection UI that prompts the user to select match invitees and a minimum and maximum number of players for auto-matching.
	 *  It uses the GPGS_ResponseActivity to startActivityForResult.
	 *  You will get back an onActivityResult with the user’s player selection criteria returned as Intent and then we'll figure out what to do.
	 */
	public void startMatchClicked(){ 
		startMyPersonalActivityForResult(GPGS_PluginAdapter.RC_TB_SELECT_PLAYERS);
	}
	
	/**
	 *	This method launches the default user interface to select players. It Open the create-game UI. <br/><br/>
	 *  Overloaded version to set any number of minimum / maximum amount of players.
	 *  @param minPlayers - minimum amount of players to play the game.
	 *  @param maxPlayers - maximum amount of players to play the game.
	 */
	public void startMatchClicked(int minPlayers, int maxPlayers){ 
		// TODO: We should pass additional extras with the intent:
		//			-> intent.putExtra("minPlayers", minPlayers);
		//			-> intent.putExtra("maxPlayers", maxPlayers);
		// So we can then use something like: startMyPersonalActivityForResult(3, minPlayers, maxPlayers); 
		// And then will have to catch those extras at GPGS_ResponseActivity.java

		// To start a match from the default player selection UI, we call getSelectPlayersIntent() to get an Intent, 
		//Intent intent = adapter.mGamesClient.getSelectPlayersIntent(minPlayers, maxPlayers, true);	// true is to allow auto-match.
		/*Intent intent = GPGS_PluginAdapter.mCurrentGPGSAdapter.mGamesClient.getSelectPlayersIntent(minPlayers, maxPlayers, true);	// true is to allow auto-match.		
		GPGS_PluginAdapter.mParentActivity.startActivityForResult(intent, GPGS_PluginAdapter.RC_TB_SELECT_PLAYERS);*/ 
		debugLog("startMatchClicked 2: startActivityForResult - RC_TB_SELECT_PLAYERS");
	}	 
	
	/**
	 * Creates a one-on-one auto-match game. <br/>
	 * This method bypasses the default user interface. A match gets created but the caller can't choose who is playing against.
	 */
	public void startQuickMatch() { 
        Bundle autoMatchCriteria = RoomConfig.createAutoMatchCriteria(TB_MIN_PLAYERS, TB_MAX_PLAYERS, 0);	// zero for auto-match.
        TurnBasedMatchConfig tbmc = TurnBasedMatchConfig.builder().setAutoMatchCriteria(autoMatchCriteria).build();
        //UnitySendMessageSafe("showSpinner", "showSpinner");	
        // Kick the match off
        debugLog("startQuickMatch: Kick the match off...");
        GPGS_PluginAdapter.mCurrentGPGSAdapter.mGamesClient.createTurnBasedMatch(this, tbmc);     
    } 
	
	/**
	 * Cancel the game. 
	 * Should possibly wait until the game is cancelled before giving up on the view.
	 */
	public void cancelGame() {
		GPGS_PluginAdapter.mCurrentGPGSAdapter.mGamesClient.cancelTurnBasedMatch(this, currTBMatch.getMatchId());
	}
	
	/**
	 * Leave the game during your turn. 
	 * Note that there is a separate GamesClient.leaveTurnBasedMatch() if you want to leave NOT on your turn.
	 */
	public void leaveGameOnYourTurn() {  
        String nextParticipantId = getNextParticipantId();
        GPGS_PluginAdapter.mCurrentGPGSAdapter.mGamesClient.leaveTurnBasedMatchDuringTurn(this, currTBMatch.getMatchId(), nextParticipantId);
    }
	
	/**
	 *  Finish the game. Sometimes, this is your only choice.
	 */
    public void finishGame() { 
    	GPGS_PluginAdapter.mCurrentGPGSAdapter.mGamesClient.finishTurnBasedMatch(this, currTBMatch.getMatchId());
    }
        
    /**
     * <h1>Method to update data of a match.  -  Store new data and set next players turn.</h1> 
     * Upload your new game state, then take a turn, and pass it on to the next player.
     * @param newDataSentFromUnity - This is the data that contains the match to be updated: unit positions, turn count, special objects...
     */
    public void uploadNewGameState(String newDataSentFromUnity) {     	
    	// We decode the String received to byte[].
    	byte[] dataSentFromUnity = Base64.decode(newDataSentFromUnity, Base64.DEFAULT);
    	// We set the pending participant ID. 
        String nextParticipantId = getNextParticipantId();
        // We set the reference variable to the data sent from Unity			
        currGameData = dataSentFromUnity;		
        // We update the match (store new data and set next players turn) of current match and register the listener. 
        GPGS_PluginAdapter.mCurrentGPGSAdapter.mGamesClient.takeTurn(this, currTBMatch.getMatchId(), currGameData, nextParticipantId);        
        // Finally, we reset the reference variable to null after we've saved it on server with takeTurn.
        currGameData = null;
    }
    
	
	// **************************
	// Call from activityResult:
	// **************************
    
    /**
     * This method happens in response to the onActivityResult's createTurnBasedMatch(). 
     * The createTurnBasedMatch does a onTurnBasedMatchInitiated call back which in turn does a startMatch.
     * This is only called on success, so we should have a valid match object. 
     * We're taking this opportunity to setup the game, saving our initial state. 
     * Calling takeTurn() will callback to OnTurnBasedMatchUpdated(), which will show the game UI. 
     * <p>-?- startQuickMatch() should also end up calling this methos, I think -?-</p>
     */
    public void startMatch(TurnBasedMatch match) {
        // Some basic turn data.	Is Base 64 good - http://www.opinionatedgeek.com/dotnet/tools/base64decode/
        currGameData = Base64.decode("PlayDude", Base64.DEFAULT);  
        String myParticipantId = currTBMatch.getParticipantId(GPGS_PluginAdapter.mCurrentGPGSAdapter.mGamesClient.getCurrentPlayerId());

        // Taking this turn will cause turnBasedMatchUpdated.
        GPGS_PluginAdapter.mCurrentGPGSAdapter.mGamesClient.takeTurn(this, match.getMatchId(), currGameData, myParticipantId);
        debugLog("startMatch: takeTurn - therefore turnBasedMatchUpdated...");        
    }

    /**
     * If you choose to rematch, then call it and wait for a response. 
     */
    public void rematch() {
        //showSpinner();
    	GPGS_PluginAdapter.mCurrentGPGSAdapter.mGamesClient.rematchTurnBasedMatch(this, currTBMatch.getMatchId());
    	currTBMatch = null;
    	//setCurrRoomVariables(match);
    }
    
	
	// *******************
	// Listeners invoked:
	// *******************	
	
	/**
	 * Callback invoked when a new invitation is received.
	 * We let Unity know that an invitation has been received.	
	 */
	@Override
	public void onInvitationReceived(Invitation invitation) {
		UnitySendMessageSafe("onInvitationReceivedTB", invitation.getInvitationId());
	}

	/**
	 * Callback invoked when a previously received invitation has been removed from the local device.
	 */
	@Override
	public void onInvitationRemoved(String invitationId) {
		UnitySendMessageSafe("onInvitationRemovedTB", invitationId);			
	}

	/**
	 * Called when the match has been cancelled.
	 */
	@Override
	public void onTurnBasedMatchCanceled(int statusCode, String matchId) {
		if (statusCode != GamesClient.STATUS_OK) 
			return;
		UnitySendMessageSafe("onTurnBasedMatchCanceled", matchId);
	}

	/** 
	 * Receiving callbacks when a match has been initiated. <br/>
	 * If the createTurnBasedMatch() call is successful, Play Games services notifies
	 *  the onTurnBasedMatchInitiated() callback in the TurnBasedMultiplayerListener that is attached
	 *  to the player's GamesClient to signal that the player can take a turn.</br>
	 * Before proceeding, remember to initialize the game data as needed by your game. 
	 * For example, you might need to initialize the starting positions for players in a strategy game 
	 * or initialize the first hand for players in a card game.
	 */
	@Override
	public void onTurnBasedMatchInitiated(int statusCode, TurnBasedMatch match) {
		if (statusCode != GamesClient.STATUS_OK) {
			// Show error status.
			return;
		}
		
		// We set our current match reference to the match received from the lister.
		currTBMatch = match;
		
		// If this player is not the first player in this match, continue. 
		if (match.getData() != null) {
			debugLog("onTurnBasedMatchInitiated: not the first player in this match...");
			updateMatch(match);
			return;
		}
		// Otherwise, this is the first player. Initialize the game state.
		debugLog("onTurnBasedMatchInitiated: this is the first player. Initializing the game state...");
		startMatch(match);
	}
	
	/**
	 * Called when match has been updated.
	 */
	@Override
	public void onTurnBasedMatchUpdated(int statusCode, TurnBasedMatch match) {
		if (statusCode != GamesClient.STATUS_OK) {
			return;
		}
		
		// We set our current match reference to the match received from the lister.
		currTBMatch = match;
				
		if (match.canRematch()) {
            // TODO: askForRematch();
        }
		// If isDoingTurn = (match.getTurnStatus() == TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN);
		if (match.getTurnStatus() == TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN) {
            updateMatch(match);
            return;
        }	
	}

	/**
	 * Called when the player has left the match.
	 */
	@Override
	public void onTurnBasedMatchLeft(int statusCode, TurnBasedMatch match) {
		if (statusCode != GamesClient.STATUS_OK) {
			return;
		}
		UnitySendMessageSafe("onTurnBasedMatchLeft", match.getMatchId());
	}

	/**
	 * Callback invoked when a new update to a match arrives.
	 * Whenever the match is updated following a player's turn, 
	 * 	your listener is notified via the onTurnBasedMatchedReceived() callback.
	 */
	@Override
	public void onTurnBasedMatchReceived(TurnBasedMatch match) {
		UnitySendMessageSafe("onTurnBasedMatchReceived", match.getMatchId());	
	}

	/**
	 * Callback invoked when a match has been removed from the local device.
	 */
	@Override
	public void onTurnBasedMatchRemoved(String matchId) {
		UnitySendMessageSafe("onTurnBasedMatchRemoved", matchId);
	}

	/**
	 * Called when matches have been loaded.
	 */
	@Override
	public void onTurnBasedMatchesLoaded(int statusCode, LoadMatchesResponse response) {
		// TODO: I could let Unity know about the fact that all turn base matches have been loaded. We won't be using it for now.	
		//UnitySendMessageSafe("onTurnBasedMatchesLoaded", "success");
	}

	
	// ***************
	// Helper methods:
	// ***************
	
	/**
	 * This is the main function that gets called when players choose a match from the inbox, 
	 * or else create a match and want to start it.
	 */
	public void updateMatch(TurnBasedMatch match) { 
		
        currTBMatch = match; 
        setCurrRoomVariables(match);
        
        int status = match.getStatus();
        int turnStatus = match.getTurnStatus();

        switch (status) {
            case TurnBasedMatch.MATCH_STATUS_CANCELED:
            	debugLog("This game was canceled!");
            	UnitySendMessageSafe("updateMatch", "STATUS_CANCELED");
                return;
            case TurnBasedMatch.MATCH_STATUS_EXPIRED:
            	debugLog("This game is expired. So sad!");
            	UnitySendMessageSafe("updateMatch", "STATUS_EXPIRED");
                return;
            case TurnBasedMatch.MATCH_STATUS_AUTO_MATCHING:
            	debugLog("We're still waiting for an automatch partner.");
            	UnitySendMessageSafe("updateMatch", "AUTO_MATCHING");
                return;
            case TurnBasedMatch.MATCH_STATUS_COMPLETE:
                if (turnStatus == TurnBasedMatch.MATCH_TURN_STATUS_COMPLETE) {
                	debugLog("This game is over; someone finished it, and so did you! There is nothing to be done.");
                	UnitySendMessageSafe("updateMatch", "STATUS_COMPLETE");
                    break;
                }
        }
        
        // OK, it's active. Check on turn status.
        switch (turnStatus) {
            case TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN:
                currGameData = currTBMatch.getData();	
                debugLog("updateMatch: a match was choosen and it is my turn...");
                // Unity will switch to game-play view and prepare UI to send data.
                UnitySendMessageSafe("updateMatch", "MY_TURN");
                return;
            case TurnBasedMatch.MATCH_TURN_STATUS_THEIR_TURN:
            	currGameData = currTBMatch.getData();
            	debugLog("updateMatch: a match was choosen and it is someone elses turn...");
            	// Unity will switch to game-play view and show return results.
            	UnitySendMessageSafe("updateMatch", "THEIR_TURN");
            	return; 
            case TurnBasedMatch.MATCH_TURN_STATUS_INVITED:
            	debugLog("updateMatch: a match which the current player has been invited to....");
            	UnitySendMessageSafe("updateMatch", "STATUS_INVITED");
            	break;
        } 
        currGameData = null;
    }
		
	/**
	 * Get the next participant. 
	 * In this function, we assume that we are round-robin, with all known players going before all auto-match players.
	 * This is not a requirement; players can go in any order. However, you can take turns in any order.
	 *  @return participantId (p_N) of next player, or null if auto-matching.
	 */
	public String getNextParticipantId() {
 
		String myParticipantId = currTBMatch.getParticipantId(GPGS_PluginAdapter.mCurrentGPGSAdapter.mGamesClient.getCurrentPlayerId());	// p_1	
		ArrayList<String> participantIds = currTBMatch.getParticipantIds();																	// [p_1, p_2]
		// desiredIndex is the initial position on the players queue. so 1st player will be 1, 8th player will be 8...
		int desiredIndex = -1;
		for (int i = 0; i < participantIds.size(); i++) {
			if (participantIds.get(i).equals(myParticipantId)) {
				desiredIndex = i + 1;		
			}
		}
		// If the player position in the queue ain't the last one (not the last player in the queue)...
		if (desiredIndex < participantIds.size()) { 
			// We return the next participantId.
			return participantIds.get(desiredIndex);	// p_2
		}

		// If the maximum number of additional players that can be added to this match ain't a positive number,
		// therefore all players have joined the queue and we are the last player in the queue...
		if (currTBMatch.getAvailableAutoMatchSlots() <= 0) {
			// You've run out of auto-match slots, so we start over. So we return the initial player.
			return participantIds.get(0);
		} 
		// If we have not yet reached the maximum number of players...
		else {
			// You have not yet fully auto-matched, so null will find a new person to play against.
			return null;
		}
	}
	
	/**
	 *  Helper Method to set all the current room variables values.
	 */
	private void setCurrRoomVariables(TurnBasedMatch match) {
		currMatchParticipants = match.getParticipants();  
		currMatchParticipantsIds = match.getParticipantIds();		
	}
	
	/**
	 *  Launches the helper Activity GPGS_ResponseActivity, which in turn will do a startActivityForResult.
	 */
	private void startMyPersonalActivityForResult(int actionToTake){
		Intent intent = new Intent(GPGS_PluginAdapter.mParentActivity, GPGS_ResponseActivity.class);
		intent.putExtra("actionToTake", actionToTake);
		intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
		GPGS_PluginAdapter.mParentActivity.startActivity(intent);
	}
}
