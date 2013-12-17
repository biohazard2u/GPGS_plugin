package com.h2origamestudio.gpgs;

import android.util.Log;
import com.unity3d.player.UnityPlayer;

public class GPGS_BaseUtil {

	// We declare a TAG constant to follow debug convention.
	private static String TAG = "GPGS_BaseUtil";	
	
	// Print debug logs?
	public boolean mDebugLog = true;
	
	// Unity game object name to send messages in C#.
	public static String gameObjectName = null;

	/**
	 * Debugging method. To easily debug.	 * 
	 * @param msg - A String identifying the message to debug.
	 */
	protected void debugLog(String msg) {
		if (mDebugLog)
			Log.d(TAG, msg);
	}

	/**
	 * This method sends a Unity Message *
	 * 
	 * @param method - A String identifying the method name.
	 * @param param  - A String identifying the parameter.
	 */
	protected void UnitySendMessageSafe(String method, String param) {
		if (gameObjectName == null || method == null)
			return;
		// We call a method in C# from Java via JNI.
		// UnityPlayer.UnitySendMessage("GameObjectName1", "MethodName1", "Message to send");
		UnityPlayer.UnitySendMessage(gameObjectName, method, param);
	}
}
