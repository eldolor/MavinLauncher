package com.mavin.wallpaper.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.mavin.util.Utils;

public class AlarmActionReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		Utils.log("Entering onReceive()::" + intent.getAction());

		if (intent.getAction().equals("com.mavin.wallpaper.ALARM_ACTION")) {
			Utils.log("com.mavin.wallpaper.ALARM_ACTION broadcast received!");
			Intent service = new Intent(context, WallpaperService.class);
			context.startService(service);
		}
		Utils.log("Exiting onReceive()::" + intent.getAction());

	}
}