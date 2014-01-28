package com.h2origamestudio.gpgs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;
import com.google.android.gms.games.GamesClient;
import com.google.android.gms.games.multiplayer.Invitation;
import com.google.android.gms.games.multiplayer.OnInvitationReceivedListener;
import com.google.android.gms.games.multiplayer.Participant;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessage;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessageReceivedListener;
import com.google.android.gms.games.multiplayer.realtime.RealTimeReliableMessageSentListener;
import com.google.android.gms.games.multiplayer.realtime.Room;
import com.google.android.gms.games.multiplayer.realtime.RoomConfig;
import com.google.android.gms.games.multiplayer.realtime.RoomStatusUpdateListener;
import com.google.android.gms.games.multiplayer.realtime.RoomUpdateListener;

/**
 * This adapter class is to allow Multiplayer functionality into Unity3D Game Engine.	
 * Tones of code on this plugin strictly follows the Google Play Game Services API / Documentation @:
 * 		https://developers.google.com/games/services/android/multiplayer
 * 
 * @author Marcos Zalacain 
 */
public class GPGS_Multiplayer extends GPGS_BaseUtil implements
		OnInvitationReceivedListener, RoomUpdateListener,
		RealTimeMessageReceivedListener, RoomStatusUpdateListener {

	// We declare a TAG constant to follow debug convention.
	// private static String TAG = "GPGS_Multiplayer";

	// PluginAdapter reference.
	GPGS_PluginAdapter adapter = GPGS_PluginAdapter.mCurrentGPGSAdapter;

	// At least 2 players required for our game.
	final static int MIN_PLAYERS = 2;
	// Minimum number of player to be connected to start a game.
	final static int MIN_NUMB_PLAYERS_CONNECTED_TO_START_GAME = 2;

	// If we got an invitation id when we connected to the games client, it's here.
	// Otherwise, it's null. (IncomingInvitationId)
	String mInvitationId;
	// A HashMap with all Rooms created.
	Map<String, Room> roomsMap = new HashMap<String, Room>();
	// The Room we are currently on (launching, connecting, closing, playing).
	Room currRoom = null;
	// Room ID where the currently active game is taking place; null if we're
	// not playing.
	String currRoomID = null;
	// The participants in the currently active game.
	ArrayList<Participant> currRoomParticipants = null;
	// The participants IDs in the currently active game.
	ArrayList<String> currRoomParticipantsIds;
	// My participant ID in the currently active game
	String currRoomMyId = null;
	// are we already playing?
	boolean currRoomPlaying = false;
	// can we destroy this room?
	boolean canDestroyCurrRoom = false;

	// Message buffer for sending messages.
	// byte[] mMsgBuf = new byte[2];
	// flag indicating whether we're dismissing the waiting room because the
	// game is starting
	// boolean mWaitRoomDismissedFromCode = false;

	// ****************
	// Update methods:
	// ****************

	/**
	 * This method is to easily update the current room. It updates room ID,
	 * participants, participantsID and the actual current room.
	 * 
	 * @param room
	 */
	private void updateCurrRoom(Room room) {
		if (room != null) { 
			// Update room Id.
			String roomId = room.getRoomId();
			if ((roomId != null) && (roomId.length() > 0)) {
				this.currRoomID = roomId;
			}
			// Update Participants.
			currRoomParticipants = room.getParticipants();
			currRoomParticipantsIds = room.getParticipantIds();
			// Update Room.
			currRoom = room;
			debugLog("updateCurrRoom - roomId = " + roomId);
		}
	}

	/**
	 * This method is to easily add or remove a Room from our roomsMap.
	 * 
	 * @param AddOrDelete
	 *            - true if we are adding this room to Map.
	 * @param room
	 *            - the room we are adding or deleting.
	 */
	private void updateRoomsMap(boolean AddOrDelete, Room room) {
		// If we are adding a room.
		if (AddOrDelete) {
			// If the map previously contained this room, the old one is
			// replaced.
			roomsMap.put(room.getRoomId(), room);
		}
		// If we are deleting a room.
		else {
			roomsMap.remove(room).getRoomId();
		}
		debugLog("updateRoomsMap - roomId = " + room.getRoomId());
	}

	// *********************************
	// Methods to call from Unity3D C#:
	// *********************************

	public String getCurrRoomId() {
		debugLog("getCurrRoomId - roomId = " + currRoom.getRoomId());
		return currRoom.getRoomId();
	}

	public String getCurrRoomParticipantCreatorId() {
		debugLog("getCurrRoomParticipantCreatorId - CreatorId = "
				+ currRoomParticipantsIds.get(0));
		// return currRoom.getCreatorId();
		return currRoomParticipantsIds.get(0);
	}

	public String getCurrRoomParticipantJoinerId() {
		debugLog("getCurrRoomParticipantJoinerId - JoinerId = "
				+ currRoomParticipantsIds.get(1));
		return currRoomParticipantsIds.get(1);
	}

	public String getCurrRoomParticipantCreatorDN() {
		debugLog("getCurrRoomParticipantCreatorDN - CreatorDN = "
				+ currRoomParticipants.get(0).getDisplayName());
		return currRoomParticipants.get(0).getDisplayName();
	}

	public String getCurrRoomParticipantJoinerDN() {
		debugLog("getCurrRoomParticipantJoinerDN - JoinerDN = "
				+ currRoomParticipants.get(1).getDisplayName());
		return currRoomParticipants.get(1).getDisplayName();
	}

	// *****************************************
	// Action Methods (also called from Unity):
	// *****************************************

	/**
	 * Accept Incoming Invitation.
	 */
	public void acceptIncomingInvitation() {
		RoomConfig.Builder roomConfigBuilder = makeBasicRoomConfigBuilder();
		roomConfigBuilder.setInvitationIdToAccept(mInvitationId);
		adapter.mGamesClient.joinRoom(roomConfigBuilder.build());
		// prevent screen from sleeping during handshake.
		GPGS_PluginAdapter.mParentActivity.getWindow().addFlags(
				WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		// Let Unity know we have accepted the invitation.
		UnitySendMessageSafe("acceptIncomingInvitation", "GoToGameScreen");
		debugLog("acceptIncomingInvitation ");
	}

	/**
	 * To launch the Invitation in-box. When the player selects an invitation
	 * from the Inbox, your app is notified via onActivityResult().
	 */
	public void launchInvitationInbox() {
		Intent intent = adapter.mGamesClient.getInvitationInboxIntent();
		GPGS_PluginAdapter.mParentActivity.startActivityForResult(intent,
				GPGS_PluginAdapter.RC_INVITATION_INBOX);
		debugLog("launchInvitationInbox ");
	}

	/**
	 * Creates a room, auto-matches the player to randomly selected opponents,
	 * then starts the game.
	 */
	public void startQuickGame() {
		// Auto-match criteria to invite 1 random auto-match opponent.
		// You can also specify more opponents (up to 3).
		Bundle am = RoomConfig.createAutoMatchCriteria(1, 1, 0);
		// Build the room configuration:
		RoomConfig.Builder roomConfigBuilder = makeBasicRoomConfigBuilder();
		roomConfigBuilder.setAutoMatchCriteria(am);
		RoomConfig roomConfig = roomConfigBuilder.build();
		// Create room:
		adapter.mGamesClient.createRoom(roomConfig);
		// Prevent screen from sleeping during handshake
		GPGS_PluginAdapter.mParentActivity.getWindow().addFlags(
				WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		// Let Unity Know we have successfully created a Room with random
		// opponents.
		UnitySendMessageSafe("onStartQuickGame", "GoToGameScreen");
		debugLog("startQuickGame ");
	}

	/**
	 * Application launches a player picker UI. From the UI, the player can
	 * select their friends to invite or select to be auto-matched with random
	 * players. Using the invitee information or the auto-match criteria, your
	 * app creates a room. Your app can then start a game, once players are
	 * connected in the room. MZ
	 */
	public void invitePlayers() {
		// Launch the player selection screen
		// minimum: 1 other player; maximum: 3 other players
		Intent intent = adapter.mGamesClient.getSelectPlayersIntent(1, 3);
		GPGS_PluginAdapter.mParentActivity.startActivityForResult(intent,
				GPGS_PluginAdapter.RC_SELECT_PLAYERS);
		debugLog("invitePlayers ");
	}

	/**
	 * To send a reliable message to a single participant. Use
	 * sendReliableRealTimeMessageToAll for now.
	 */
	public int sendReliableRealTimeMessage(byte[] messageData, String roomId, String recipientParticipantId) {
		// We can send a message with a listener to inform if message has been
		// received.
		return adapter.mGamesClient.sendReliableRealTimeMessage(
				new RealTimeReliableMessageSentListener() {
					@Override
					public void onRealTimeMessageSent(int statusCode,
							int tokenId, String recipientParticipantId) {
						UnitySendMessageSafe("sendReliableRealTimeMessage",
								"sendReliableRealTimeMessage: " + statusCode
										+ " " + tokenId + " "
										+ recipientParticipantId);
					}
				}, messageData, roomId, recipientParticipantId);
	}

	/**
	 * To send reliable message to all other participants, not to myself. We
	 * have NOT implemented a RealTimeReliableMessageSentListener because we
	 * should not need it at this point.
	 * 
	 * @param messageData
	 * @param roomId
	 */
	public void sendReliableRealTimeMessageToAll(byte[] messageData, String roomId) {
		for (Participant participant : currRoomParticipants) {
			if (!participant.getParticipantId().equals(currRoomMyId)) {
				debugLog("sendReliableRealTimeMessageToAll - participantID: "
						+ participant.getParticipantId());
				adapter.mGamesClient.sendReliableRealTimeMessage(null,
						messageData, roomId, participant.getParticipantId());
			}
		}
	}

	// *******************
	// Listeners invoked:
	// *******************

	/*
	 * Called when the client attempts to create a real-time room. The real-time
	 * room can be created by calling the createRoom(RoomConfig) operation.
	 */
	@Override
	public void onRoomCreated(int statusCode, Room room) {
		if (statusCode != GamesClient.STATUS_OK) {
			// Let screen go to sleep.
			GPGS_PluginAdapter.mParentActivity.getWindow().clearFlags(
					WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
			// show error message, return to main screen.
			UnitySendMessageSafe("onRoomCreated", "error when creating room: "
					+ statusCode);
			return;
		}
		// We update our current room.
		updateCurrRoom(room);
		// We add this room to Map.
		updateRoomsMap(true, room);
		// We let Unity3D know that Room has been created.
		UnitySendMessageSafe("onRoomCreated", "success");
		// We get the waiting room intent.
		Intent i = adapter.mGamesClient.getRealTimeWaitingRoomIntent(room,
				MIN_NUMB_PLAYERS_CONNECTED_TO_START_GAME);
		GPGS_PluginAdapter.mParentActivity.startActivityForResult(i,
				GPGS_PluginAdapter.RC_WAITING_ROOM);
		debugLog("onRoomCreated - roomID: " + room.getRoomId());
	}

	/*
	 * Called when the client attempts to join a real-time room. The real-time
	 * room can be joined by calling the joinRoom(RoomConfig) operation. From
	 * RoomUpdateListener Interface.
	 */
	@Override
	public void onJoinedRoom(int statusCode, Room room) {
		if (statusCode != GamesClient.STATUS_OK) {
			// let screen go to sleep
			GPGS_PluginAdapter.mParentActivity.getWindow().clearFlags(
					WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
			// show error message, return to main screen.
			UnitySendMessageSafe("onJoinedRoom", "error when joining room: "
					+ statusCode);
			return;
		}
		UnitySendMessageSafe("onJoinedRoom", "success");

		// Waiting Room - get waiting room intent
		Intent i = adapter.mGamesClient.getRealTimeWaitingRoomIntent(room,
				MIN_NUMB_PLAYERS_CONNECTED_TO_START_GAME);
		GPGS_PluginAdapter.mParentActivity.startActivityForResult(i,
				GPGS_PluginAdapter.RC_WAITING_ROOM);
		debugLog("onJoinedRoom - roomID: " + room.getRoomId());
	}

	/*
	 * Called when the client attempts to leaves the real-time room. Called when
	 * we've successfully left the room (this happens a result of voluntarily
	 * leaving via a call to leaveRoom(). If we get disconnected, we get
	 * onDisconnectedFromRoom()).
	 */
	@Override
	public void onLeftRoom(int code, String roomId) {
		// Left room. Ready to start or join another room.
		if (canDestroyCurrRoom) {
			roomsMap.remove(roomId);
			currRoomID = null;
			currRoom = null;
			currRoomParticipants = null;
			currRoomParticipantsIds = null;
			currRoomMyId = null;
			currRoomPlaying = false;
		}
		debugLog("onLeftRoom - roomID: " + roomId);
	}

	/**
	 * Called when we are connected to the room. We're not ready to play yet!
	 * (maybe not everybody is connected yet).
	 */
	@Override
	public void onConnectedToRoom(Room room) {
		// We update current room (Room, roomID and participants).
		updateCurrRoom(room);
		// We add this room to Map if we haven't done so, or update if
		// different.
		updateRoomsMap(true, room);
		// We update my ID.
		currRoomMyId = room.getParticipantId(adapter.mGamesClient
				.getCurrentPlayerId());
		// We let Unity Know we have connected to the Room.
		UnitySendMessageSafe("onConnectedToRoom", "success");
		debugLog("onConnectedToRoom - roomID: " + room.getRoomId());
	}

	/*
	 * Called when all the participants in a real-time room are fully connected.
	 * This gets called once all invitations are accepted and any necessary
	 * auto-matching has been completed. We should wait for the
	 * RoomUpdateListener.onRoomConnected() callback notification before you
	 * attempt to send or receive messages.
	 */
	@Override
	public void onRoomConnected(int statusCode, Room room) {
		if (statusCode != GamesClient.STATUS_OK) {
			// let screen go to sleep
			GPGS_PluginAdapter.mParentActivity.getWindow().clearFlags(
					WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
			// show error message, return to main screen.
			UnitySendMessageSafe("onRoomConnected",
					"error when connecting to room: " + statusCode);
		}
		updateCurrRoom(room);
		UnitySendMessageSafe("onRoomConnected", "success");
		debugLog("onRoomConnected - roomID: " + room.getRoomId());
	}

	/*
	 * Called when the client is disconnected from the connected set in a room.
	 * We shall return to the main screen.
	 */
	@Override
	public void onDisconnectedFromRoom(Room room) {
		// We leave the room.
		adapter.mGamesClient.leaveRoom(this, room.getRoomId());
		// clear the flag that keeps the screen on
		GPGS_PluginAdapter.mParentActivity.getWindow().clearFlags(
				WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		// show error message and return to main screen
		UnitySendMessageSafe("onDisconnectedFromRoom",
				"disconnected from room: " + room.getRoomId());
		// If we have to destroy room:
		if (canDestroyCurrRoom) {
			// TODO : Maybe I should be saving the RoomId in Cloud Save to
			// return to this room.
			roomsMap.remove(room.getRoomId());
			currRoomID = null;
			currRoom = null;
			currRoomParticipants = null;
			currRoomParticipantsIds = null;
			currRoomMyId = null;
			currRoomPlaying = false;
		}
		debugLog("onDisconnectedFromRoom - roomID: " + room.getRoomId());
	}

	/**
	 * Overriding method. Handles the OnInvitationReceivedListener.onInvitationReceived() callback.
	 * Sends a Unity message.
	 */
	@Override
	public void onInvitationReceived(Invitation invitation) {
		// We got an invitation to play a game! So, store it in mIncomingInvitationId.
		mInvitationId = invitation.getInvitationId();
		// We send a Message to Unity so we show a pop-up on the screen.
		UnitySendMessageSafe("onInvitationReceived", invitation.getInvitationId());
		debugLog("onInvitationReceived - invitation: " + invitation.getInvitationId());
	}
	
	/**
	 * Overriding method. New 4.1 google play services library version added!!
	 */
	@Override
	public void onInvitationRemoved(String arg0) {
		// TODO Auto-generated method stub		
	}

	/*
	 * Called to notify the client that a reliable or unreliable message was
	 * received for a room.
	 */
	@Override
	public void onRealTimeMessageReceived(RealTimeMessage rtm) {
		// get real-time message
		byte[] b = rtm.getMessageData();
		// TODO: process message...
		// Let Unity know.
		UnitySendMessageSafe("onRealTimeMessageReceived", b.toString());
		debugLog("onRealTimeMessageReceived - RealTimeMessage: " + rtm);
	}

	/*
	 * Called when the client is successfully connected to a peer participant.
	 */
	@Override
	public void onP2PConnected(String participantId) {
		// TODO Auto-generated method stub
		debugLog("onP2PConnected - participantId: " + participantId);
	}

	/*
	 * Called when client gets disconnected from a peer participant.
	 */
	@Override
	public void onP2PDisconnected(String participantId) {
		// TODO Auto-generated method stub
		debugLog("onP2PDisconnected - participantId: " + participantId);
	}

	/*
	 * Called when one or more peers decline the invitation to a room.
	 */
	@Override
	public void onPeerDeclined(Room room, List<String> peers) {
		// peer declined invitation -- see if game should be cancelled
		if (!currRoomPlaying && shouldCancelGame(room)) {
			adapter.mGamesClient.leaveRoom(this, room.getRoomId());
			GPGS_PluginAdapter.mParentActivity.getWindow().clearFlags(
					WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
			updateCurrRoom(room);
			debugLog("onPeerDeclined - roomId: " + room.getRoomId());
		}
	}

	/*
	 * Called when one or more peers are invited to a room.
	 */
	@Override
	public void onPeerInvitedToRoom(Room room, List<String> arg1) {
		updateCurrRoom(room);
		debugLog("onPeerInvitedToRoom - roomId: " + room.getRoomId());
	}

	/*
	 * Called when one or more peer participants join a room.
	 */
	@Override
	public void onPeerJoined(Room room, List<String> arg1) {
		updateCurrRoom(room);
		debugLog("onPeerJoined - roomId: " + room.getRoomId());
	}

	/*
	 * Called when one or more peer participant leave a room.
	 */
	@Override
	public void onPeerLeft(Room room, List<String> peers) {
		// peer left -- see if game should be cancelled
		if (!currRoomPlaying && shouldCancelGame(room)) {
			adapter.mGamesClient.leaveRoom(this, room.getRoomId());
			GPGS_PluginAdapter.mParentActivity.getWindow().clearFlags(
					WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
			updateCurrRoom(room);
			debugLog("onPeerLeft - roomId: " + room.getRoomId());
		}
	}

	/*
	 * Called when one or more peer participants are connected to a room.
	 */
	@Override
	public void onPeersConnected(Room room, List<String> peers) {
		if (currRoomPlaying) {
			// TODO: add new player to an ongoing game
		} else if (shouldStartGame(room)) {
			// TODO: start game!
		}
		updateCurrRoom(room);
		debugLog("onPeersConnected - roomId: " + room.getRoomId());
	}

	/*
	 * Called when one or more peer participants are disconnected from a room.
	 */
	@Override
	public void onPeersDisconnected(Room room, List<String> peers) {
		if (currRoomPlaying) {
			// TODO: game-specific handling of this -- remove player's avatar
			// from the screen, etc.
			// If not enough players are left for the game to go on, end the
			// game and leave the room.
		} else if (shouldCancelGame(room)) {
			// cancel the game
			adapter.mGamesClient.leaveRoom(this, room.getRoomId());
			GPGS_PluginAdapter.mParentActivity.getWindow().clearFlags(
					WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		}
		updateCurrRoom(room);
		debugLog("onPeersDisconnected - roomId: " + room.getRoomId());
	}

	/*
	 * Called when the server has started the process of auto-matching. Any
	 * invited participants must have joined and fully connected to each other
	 * before this will occur.
	 */
	@Override
	public void onRoomAutoMatching(Room room) {
		updateCurrRoom(room);
		// updateRoomsMap(true, room);
		// UnitySendMessageSafe("onRoomAutoMatching", room.getRoomId());
		debugLog("onRoomAutoMatching - roomId: " + room.getRoomId());
	}

	/*
	 * Called when one or more participants have joined the room and have
	 * started the process of establishing peer connections.
	 */
	@Override
	public void onRoomConnecting(Room room) {
		updateCurrRoom(room);
		// updateRoomsMap(true, room);
		// UnitySendMessageSafe("onRoomConnecting", room.getRoomId());
		debugLog("onRoomConnecting - roomId: " + room.getRoomId());
	}

	// ***************
	// Helper methods:
	// ***************

	/**
	 * Create a RoomConfigBuilder that's appropriate for our implementation.
	 * 
	 * @return - returns a RoomConfig.Builder.
	 */
	RoomConfig.Builder makeBasicRoomConfigBuilder() {
		// To use reliable or unreliable messaging, we set the message listener
		// (setMessageReceivedListener) here.
		return RoomConfig.builder(this).setMessageReceivedListener(this)
				.setRoomStatusUpdateListener(this);
	}

	/**
	 * Returns whether there are enough players to start the game. Note that
	 * game play should start only when all players are connected
	 * 
	 * @param room
	 *            - room we are acting upon.
	 * @return true if we have achieved the minimum of connected players.
	 */
	boolean shouldStartGame(Room room) {
		int connectedPlayers = 0;
		for (Participant p : room.getParticipants()) {
			if (p.isConnectedToRoom())
				++connectedPlayers;
		}
		return connectedPlayers >= MIN_PLAYERS;
	}

	/**
	 * Returns whether the room is in a state where the game should be
	 * cancelled. *
	 * 
	 * @param room
	 *            - room we are acting upon.
	 */
	boolean shouldCancelGame(Room room) {
		// We should cancel the game if enough people have declined the
		// invitation or left the room.
		int connectedPlayers = 0;
		for (Participant p : room.getParticipants()) {
			// We could check a participant's status with
			// Participant.getStatus() instead.
			if (p.isConnectedToRoom())
				++connectedPlayers;
		}
		debugLog("shouldCancelGame - connectedPlayers: " + connectedPlayers);
		if (connectedPlayers <= 0) {
			canDestroyCurrRoom = true;
			return true;
		}
		return false;
	}
}
