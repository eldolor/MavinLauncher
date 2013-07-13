package com.mavin.wallpaper.service;

import java.io.File;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.mavin.ad.AdHandler;
import com.mavin.ad.ImageAd;
import com.mavin.ads.MavinAdActivity;
import com.mavin.util.Utils;

public class AlarmActionReceiver extends BroadcastReceiver {
	private static final String TAG = AlarmActionReceiver.class.getSimpleName();

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(TAG, "Entering onReceive()::" + intent.getAction());

		if (intent.getAction().equals("com.mavin.wallpaper.ALARM_ACTION")) {
			Log.d(TAG, "com.mavin.wallpaper.ALARM_ACTION broadcast received!");
			startWallpaperService(context);
		}
		Log.d(TAG, "Exiting onReceive()::" + intent.getAction());

	}

	/**
	 * 
	 * @param context
	 */
	private void startWallpaperService(Context context) {
		Log.d(TAG, "Entering startWallpaperService()");
		Intent intent = new Intent(context, WallpaperService.class);
		context.startService(intent);
		Log.d(TAG, "Exiting startWallpaperService()");
	}


}