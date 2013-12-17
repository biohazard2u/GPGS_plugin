package com.h2origamestudio.gpgs;

import android.util.Base64;
import com.google.android.gms.appstate.AppStateClient;
import com.google.android.gms.appstate.OnStateLoadedListener;

/**
 * This class is to allow Cloud Save functionality into Unity3D Game Engine.
 * Tones of code on this class strictly follows the Google Play Game Services API / Documentation @:
 * 		https://developers.google.com/games/services/android/cloudsave
 * 
 * @author Marcos Zalacain 
 */
public class GPGS_CloudSave extends GPGS_BaseUtil implements OnStateLoadedListener {

	// We declare a TAG constant to follow debug convention.
	//private static String TAG = "GPGS_CloudSave";

	// PluginAdapter reference. 
	GPGS_PluginAdapter adapter;			// We are NOT using the adapter at this moment!

	AppStateClient mAppStateClient = null;

	// byte[] to store data to cloud.
	public byte[] mKey0Data = null;
	public byte[] mKey1Data = null;
	public byte[] mKey2Data = null;
	public byte[] mKey3Data = null;

	/**
	 * Constructor 
	 * @param adapter - a reference to the GPGS_PluginAdapter.
	 */
	GPGS_CloudSave(GPGS_PluginAdapter adapter) {
		this.adapter = adapter;
	}	

	/**
	 * Our method to load from cloud. 
	 * It will trigger a call to onStateConflict or onStateLoaded.
	 * @param stateKey - The key to load data for. Must be a non-negative integer less than getMaxNumKeys(). 
	 */
	public void loadFromCloud(int stateKey) {
		debugLog("loadFromCloud: begingning to load from Cloud");			
		// Asynchronously loads saved state for the current app. 
		mAppStateClient.loadState(this, stateKey);
	}

	/**
	 * 1st method to save to cloud. It's the fire-and-forget version and requires no listener.
	 * Updates app state for the current app. 
	 * This method updates the local copy of the app state and syncs the changes to the server. 
	 * If the local data conflicts with the data on the server, 
	 * this will be indicated the next time you call loadState(OnStateLoadedListener, int). 
	 * @param stateKey - The key to update data for. Must be a non-negative integer less than getMaxNumKeys().
	 * @param bytes - The data to store. May be a maximum of getMaxStateSize() bytes. 
	 */
	public void saveToCloud(int stateKey, String bytes) {
		debugLog("Length of bytes received " + bytes.length());
		byte[] data = Base64.decode(bytes, Base64.DEFAULT);
		// Fire-and-forget form of the API.
		mAppStateClient.updateState(stateKey, data);
	}
	
	/**
	 * 2nd method to save to cloud. It's the Immediate version and it has a listener.
	 * This method updates the local copy of the app state and syncs the changes to the server. 
	 * If the local data conflicts with the data on the server, 
	 * this will be indicated the next time you call loadState(OnStateLoadedListener, int). 
	 * @param stateKey - The key to update data for. Must be a non-negative integer less than getMaxNumKeys().
	 * @param bytes - The data to store. May be a maximum of getMaxStateSize() bytes.
	 */
	public void saveToCloudImmediate(int stateKey, String bytes) { 
		debugLog("Length of bytes received " + bytes.length());
		byte[] data = Base64.decode(bytes, Base64.DEFAULT);
		// The listener is called when the write operation is complete. 
		mAppStateClient.updateStateImmediate (this, stateKey, data);
	}	

	/**
	 * Our method to get Loaded data. 
	 */
	public String getLoadedData(int keyNum) {
		String data = null;
		switch (keyNum) {
		case 0:
			data = Base64.encodeToString(mKey0Data, Base64.DEFAULT);
			break;
		case 1:
			data = Base64.encodeToString(mKey1Data, Base64.DEFAULT);
			break;
		case 2:
			data = Base64.encodeToString(mKey2Data, Base64.DEFAULT);
			break;
		case 3:
			data = Base64.encodeToString(mKey3Data, Base64.DEFAULT); 
			break;
		}
		debugLog("getLoadedData: getLoadedData from key " + keyNum);
		return data;
	} 

	/**
	 * Overriding method. To resolve a previously detected conflict in application
	 * state data. Called when a conflict is detected while loading app state.
	 * @param stateKey - The key to update data for. Must be a non-negative integer less than getMaxNumKeys().
	 * @param resolvedVersion - This value must be passed to resolveState method.
	 * @param localData - The data we had in our phone and attempted to deliver to cloud.
	 * @param serverData - The data that the cloud had when conflict aroused.
	 */
	@Override
	public void onStateConflict(int stateKey, String resolvedVersion, byte[] localData, byte[] serverData) {
		debugLog("onStateConflict: Conflict detected in data for key => " + stateKey);

		// TODO: Need to resolve conflict between the two states.
		// localState = localData;
		// serverState = serverData;
		// Check whether locasState or serverState is the right one or latest.
		// BattleRoom battleRoom = localState.unionWithMethod(serverState);
		// mAppStateClient.resolveState(this, stateKey, resolvedVersion, battleRoom.toBytes());
		
		// Currently we are just favouring the localState version.
		mAppStateClient.resolveState(this, stateKey, resolvedVersion, localData);
	}

	/**
	 * Overriding method. 
	 * Called when app state data has been loaded.
	 * @param statusCode - This value indicates whether the data was successfully loaded from the cloud.
	 * @param stateKey - The key to update data for. Must be a non-negative integer less than getMaxNumKeys().	 
	 * @param localData - The data we had in our phone and attempted to deliver to cloud.
	 */
	@Override
	public void onStateLoaded(int statusCode, int stateKey, byte[] localData) {
		
		// If Data was successfully loaded from the cloud: merge with local data.
		if(statusCode == AppStateClient.STATUS_OK){ 
			updateLocalData(stateKey, localData);
			debugLog("onStateLoaded: STATUS_OK");
		}
		// If key not found means there is no saved data. This is the same as having empty data, so we treat this as a success.
		else if (statusCode == AppStateClient.STATUS_STATE_KEY_NOT_FOUND) {
			// No need to update local data here, so no: updateLocalData(stateKey, localData);
			debugLog("onStateLoaded: STATUS_STATE_KEY_NOT_FOUND");
		}
		// If can't reach cloud, and we have no local state. Warn user that they may not see their existing progress, but any new progress won't be lost.
		else if (statusCode == AppStateClient.STATUS_NETWORK_ERROR_NO_DATA) {
			unsucunsuccessfulLoading(statusCode, stateKey);
		}
		// If can't reach cloud, but we have locally cached data.
		else if (statusCode == AppStateClient.STATUS_NETWORK_ERROR_STALE_DATA) {
			unsucunsuccessfulLoading(statusCode, stateKey);
		}
		// If can't reach cloud, but we have locally cached data.
		else if (statusCode == AppStateClient.STATUS_CLIENT_RECONNECT_REQUIRED) {
			unsucunsuccessfulLoading(statusCode, stateKey);
			// We'll try to reconnect client here.
			try {
				adapter.init(GPGS_PluginAdapter.mParentActivity);
			} catch (Exception e) {
				debugLog("onStateLoaded: Reconnectiong after STATUS_CLIENT_RECONNECT_REQUIRED failed.");
				e.printStackTrace();
			}  			
		}
		// If an unidentified error occur.
		else {
			unsucunsuccessfulLoading(statusCode, stateKey);
		}
	}
	
	
	//**************** 
	// Helper Methods:
	//****************
	
	
	/**
	 * To Update local data from data received from server.
	 */
	private void updateLocalData(int stateKey, byte[] localData){
		switch (stateKey) {
		case 0:
			mKey0Data = localData;
			break;
		case 1:
			mKey1Data = localData;
			break;
		case 2:
			mKey2Data = localData;
			break;
		case 3: 
			mKey3Data = localData;
			break;
		}
		UnitySendMessageSafe("OnGPGCloudLoadResult", "success;" + stateKey + ";" + localData.length);
	}
	
	/**
	 * To warn about an error while loading data from server.
	 */
	private void unsucunsuccessfulLoading(int statusCode, int stateKey){
		UnitySendMessageSafe("OnGPGCloudLoadResult", "error;" + stateKey + ";0");
		debugLog("Error in loading key data " + statusCode + " " + stateKey);
		//return;
	} 
}
