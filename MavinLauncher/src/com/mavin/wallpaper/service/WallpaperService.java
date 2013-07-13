
package com.mavin.wallpaper.service;

import java.io.File;

import android.app.Activity;
import android.app.Service;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.mavin.ad.AdHandler;
import com.mavin.ad.AdKey;
import com.mavin.ad.ApplicationType;
import com.mavin.ad.ImageAd;
import com.mavin.util.ConfigurationManager;
import com.mavin.util.Utils;

public class WallpaperService extends Service {
    private static String TAG;

    private Service mService;
    private SharedPreferences mPreferences;
    private String mDeviceId;
    private boolean isRunning = false;
    private static boolean USE_SCALED_BITMAP;

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    @Override
    public void onCreate() {
        // setup TAG
        TAG = this.getClass().getSimpleName();
        mService = this;
        mPreferences = getSharedPreferences("mavin",
                Activity.MODE_PRIVATE);
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        mDeviceId = telephonyManager.getDeviceId();
    }

    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand");
        try {
            String useScaledBitmap = ConfigurationManager.getInstance(
                    new File(Utils.getStorageDirectory() + File.separator
                            + "config.properties")).getProperty(
                    "mavin.wallpaper.use.scaled.bitmap");
            USE_SCALED_BITMAP = useScaledBitmap
                    .equalsIgnoreCase("Y") ? true : false;
            final int startId_ = startId;
            if (!isRunning)
            {
                isRunning = true;
            } else
            {
                Log.d(TAG,
                        "Wallpaper service is already running, stop and return!!!");
                // For whatever reason, the previous onStartCommand is still
                // running,
                // perhaps the service got
                // killed by runtime and this service is not STICKY, so it never
                // reach end
                // to call stopSelf?
                // So just kill it and let another alarm to start it again.
                stopSelf(startId_);
                return Service.START_NOT_STICKY;
            }
            if ((flags & Service.START_FLAG_RETRY) == Service.START_FLAG_RETRY)
            {
                Log.d(TAG, "Wallpaper service retry");
            }

            WallpaperManager _wallpaperManager = WallpaperManager
                    .getInstance(getApplicationContext());
            if (_wallpaperManager != null) {

                AdKey key = new AdKey();
                key.setApplication(ApplicationType.WALLPAPER.getType());
                // always get ads from the AdHandler and never from the AdCache
                final ImageAd _imageAd = (ImageAd) AdHandler.getAd(
                        WallpaperService.this.getApplicationContext(), key);
                AdHandler.setActiveWallpaperAd(_imageAd);

                if (_imageAd != null) {
                    Bitmap useThisBitmap = null;
                    if (USE_SCALED_BITMAP) {
                        Log.i(TAG, " Setting Wallpaper: using scaled image " + _imageAd.getUri());
                        useThisBitmap = Bitmap.createScaledBitmap(
                                Utils.getBitmap(new File(Utils.getStorageDirectory()
                                        + File.separator
                                        + _imageAd.getUri())),
                                _wallpaperManager.getDesiredMinimumWidth(),
                                _wallpaperManager.getDesiredMinimumHeight(), true);
                    } else {
                        useThisBitmap = Bitmap.createBitmap(Utils.getBitmap(new File(Utils
                                .getStorageDirectory() + File.separator
                                + _imageAd.getUri())));
                    }
                    _wallpaperManager.setBitmap(useThisBitmap);
                    Log.i(TAG, " Setting Wallpaper: " + _imageAd.getUri());

                    // broadcast the ad that was displayed
                    Intent _message = new Intent();
                    _message.setAction("com.android.wallpaper.AD_DISPLAYED_ACTION");
                    sendBroadcast(_message);
                } else {
                    Log.w(TAG, "No Ad Found");
                }
            } else {
                Log.w(TAG, "WallpaperManager Not Found");
            }

        } catch (Throwable e)
        {
            Log.e(TAG,
                    "error: "
                            + ((e.getMessage() != null) ? e.getMessage()
                                    .replace(" ", "_") : ""), e);
        }
        return Service.START_NOT_STICKY;

    }
}