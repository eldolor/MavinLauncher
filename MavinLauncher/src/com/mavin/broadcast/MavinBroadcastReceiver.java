package com.mavin.broadcast;

import java.util.Random;

import com.mavin.campaign.sync.CampaignDownloadService;
import com.mavin.device.sync.DeviceRegistrationService;
import com.mavin.util.Utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class MavinBroadcastReceiver extends BroadcastReceiver {
	private static final String LOG_TAG = MavinBroadcastReceiver.class
			.getSimpleName();
	private static final Random random = new Random();
	private static int MAX_ATTEMPTS = 5;
	private static int BACKOFF_MILLI_SECONDS = 5000;

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.i(LOG_TAG, " onReceive(): Entering");
		Log.i(LOG_TAG, " onReceive(): Received intent " + intent.getAction());

		if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
			long backoff = BACKOFF_MILLI_SECONDS + random.nextInt(1000);
			// wait till the external storage state is ready. Mainly on account
			// of
			// boot completed action, where the external storage state might not
			// ready yet
			for (int i = 1; i <= MAX_ATTEMPTS; i++) {
				Log.d(LOG_TAG, "onReceive(): Attempt #" + i + " to process intent " + intent.getAction());
				if (Utils.isExternalStorageReady()) {
//					Intent deviceRegistrationService = new Intent(context,
//							DeviceRegistrationService.class);
//					context.startService(deviceRegistrationService);
//					Log.i(LOG_TAG, "Starting DeviceRegistrationService");
//					Intent campaignDownloadService = new Intent(context,
//							CampaignDownloadService.class);
//					context.startService(campaignDownloadService);
//					Log.i(LOG_TAG, "Starting CampaignDownloadService");
					
					Intent newIntent = new Intent(context,
							MavinBroadcastReceiverActivity.class);
					newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					context.startActivity(newIntent);

					// success; end loop
					i = MAX_ATTEMPTS;

				} else {
					Log.w(LOG_TAG, "External storage state is not ready yet");
					try {
						Log.d(LOG_TAG, "Sleeping for " + backoff
								+ " ms before retry");
						Thread.sleep(backoff);
					} catch (InterruptedException e1) {
						// Activity finished before we complete - exit.
						Log.d(LOG_TAG,
								"Thread interrupted: abort remaining retries!");
						Thread.currentThread().interrupt();
					}
					// increase backoff exponentially
					backoff *= 2;
				}
			}
		} else {
			Log.i(LOG_TAG, " onReceive(): Processing intent " + intent.getAction());
			Intent newIntent = new Intent(context,
					MavinBroadcastReceiverActivity.class);
			newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(newIntent);

		}
		Log.i(LOG_TAG, " onReceive(): Exiting");

	}
}
