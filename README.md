This repository contains the necessary code to build a Unity3D plugin.
This plugin adds to the Unity3D game engine all of the Google Play Game Services funcionality.

These are:
	*LeaderBoard
	*Achievements
	*Cloud Save
	*Multiplayer

Important Note:
The GPGS API has been build to allow multiplayer functionality by sending P2P (peer 2 peer) messages only. Therefore, both players have to be connected at the same time.
This means that multiplayer games with a turn-based gameplay functionality will require another approach.

From Google Documentation:
You can send messages only when your GamesClient is connected to the room, and only to participants who are also connected to the room. Messages that you send when you're not connected or when the recipient is not connected will not be delivered.

Also, Google is currently working on a similar plugin.

At this moment, I have yet to upload the C# part of the plugin. 

This code strictly follows the Google Documentation.
This code has been written by Marcos Zalacain.
