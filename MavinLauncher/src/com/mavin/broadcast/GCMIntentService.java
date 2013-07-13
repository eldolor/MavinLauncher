package com.mavin.broadcast;

import java.io.File;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gcm.GCMBaseIntentService;
import com.google.android.gcm.GCMRegistrar;
import com.mavin.campaign.sync.AlarmType;
import com.mavin.gcm.GcmUtilities;
import com.mavin.util.ConfigurationManager;
import com.mavin.util.Utils;

public class GCMIntentService extends GCMBaseIntentService {

	private static final String TAG = GCMIntentService.class.getSimpleName();

	private static final String GOOGLE_API_PROJECT_NUMBER = ConfigurationManager
			.getInstance(
					new File(Utils.getStorageDirectory() + File.separator
							+ "config.properties")).getProperty(
					"mavin.google.api.project.number");

	public GCMIntentService() {
		super(GOOGLE_API_PROJECT_NUMBER);
	}

	@Override
	protected void onRegistered(Context context, String registrationId) {
		Log.i(TAG, "Device registered: regId = " + registrationId);
		// displayMessage(context, getString(R.string.gcm_registered));
		GcmUtilities.register(context, registrationId);
	}

	@Override
	protected void onUnregistered(Context context, String registrationId) {
		Log.i(TAG, "Device unregistered");
		// displayMessage(context, getString(R.string.gcm_unregistered));
		if (GCMRegistrar.isRegisteredOnServer(context)) {
			GcmUtilities.unregister(context, registrationId);
		} else {
			// This callback results from the call to unregister made on
			// ServerUtilities when the registration to the server failed.
			Log.i(TAG, "Ignoring unregister callback");
		}
	}

	@Override
	protected void onMessage(Context context, Intent intent) {
		Log.i(TAG, "Received message");
		// String message = getString(R.string.gcm_message);
		// displayMessage(context, message);
		// notifies user
		// generateNotification(context, message);
		Intent newIntent = new Intent();
		newIntent.setAction(AlarmType.CAMPAIGN_DOWNLOAD_BEGIN_ACTION.getType());
		sendBroadcast(newIntent);
		Log.i(TAG, "Sending broadcast "
				+ AlarmType.CAMPAIGN_DOWNLOAD_BEGIN_ACTION.getType());
	}

	@Override
	protected void onDeletedMessages(Context context, int total) {
		Log.i(TAG, "Received deleted messages notification");
		// String message = getString(R.string.gcm_deleted, total);
		// displayMessage(context, message);
		// notifies user
		// generateNotification(context, message);
	}

	@Override
	public void onError(Context context, String errorId) {
		Log.i(TAG, "Received error: " + errorId);
		// displayMessage(context, getString(R.string.gcm_error, errorId));
	}

	@Override
	protected boolean onRecoverableError(Context context, String errorId) {
		// log message
		Log.i(TAG, "Received recoverable error: " + errorId);
		// displayMessage(context, getString(R.string.gcm_recoverable_error,
		// errorId));
		return super.onRecoverableError(context, errorId);
	}


}
