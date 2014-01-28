package com.h2origamestudio.gpgs;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;

/**
 * This is a helper activity class. It is to help with user connection issues. 
 */
public class GPGS_DummyActivity extends Activity { 

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);

	}

	protected void onStart() {
		super.onStart();
		Log.d("GPGS_BaseUtil", "GPGS_DummyActivity - onStart");
		try {
			if (GPGS_PluginAdapter.mConnectionResult != null)
				GPGS_PluginAdapter.mConnectionResult.startResolutionForResult(this,
						GPGS_PluginAdapter.RC_RESOLVE);
		} catch (SendIntentException e) {
			Log.e("GPGS_PluginAdapter",
					"Error in starting connection resolution " + e.getMessage());
		}

	}

	protected void onDestroy() { 
		super.onDestroy();
	}

	public void onActivityResult(int request, int response, Intent data) {
		super.onActivityResult(request, response, data);
		Log.d("GPGS_BaseUtil", "GPGS_DummyActivity - onActivityResult");
		if (GPGS_PluginAdapter.mCurrentGPGSAdapter != null)
			GPGS_PluginAdapter.mCurrentGPGSAdapter.onActivityResult(request, response, data);
		finish();
	}
}
