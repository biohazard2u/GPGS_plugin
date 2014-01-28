This repository contains the necessary code to build a Unity3D plugin.
This plugin adds to the Unity3D game engine all of the Google Play Game Services funcionality.

These are:
	*SignIn - SignOut
	*LeaderBoard
	*Achievements
	*Cloud Save
	*Multiplayer P2P
	*Turn Base Multiplayer

Important Note:
Make sure you have the latest google-play-services library to access to the latest Turn Based Multiplayer funcionality.

Also, Google is currently working on a similar plugin. 
Their plugin doesn't have the Multiplayer P2P and Turn Base Funcionality yet, but I'm sure it won't be long.

At this moment, I have yet to upload the C# part of the plugin.
You may request it to me if you need it.

For this pluging to work, you will need to add in your Unity3D project / Assets/Plugins/Android folder:
	- Your Android Manifest file (AndroidManifest.xml)
	- The android suport library (android-support-v4.jar)
	- The latest google play services library (google-play-services.jar)
	- This plugin library (a jar file with all java clases in it: gpgs-XXX-plugin.jar)

Any doubts on how to use it, drop me a line.
 

This code strictly follows the Google Documentation.

This code has been written by Marcos Zalacain.
