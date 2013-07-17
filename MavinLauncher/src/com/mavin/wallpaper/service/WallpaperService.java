
package com.mavin.wallpaper.service;

import java.io.File;

import android.app.IntentService;
import android.app.Service;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import com.mavin.ad.AdHandler;
import com.mavin.ad.AdKey;
import com.mavin.ad.ApplicationType;
import com.mavin.ad.ImageAd;
import com.mavin.util.ConfigurationManager;
import com.mavin.util.Utils;

public class WallpaperService extends IntentService {
    private static String TAG = WallpaperService.class.getSimpleName();

    private static boolean USE_SCALED_BITMAP;

    public WallpaperService() {
        super("WallpaperService");
    }

    public WallpaperService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent arg0) {
        Utils.log("Entering");
        setWallpaper();
        Utils.log("Exiting");
    }

    public void setWallpaper() {
        Utils.log("Entering");
        try {
            String useScaledBitmap = ConfigurationManager.getInstance(
                    new File(Utils.getStorageDirectory() + File.separator
                            + "config.properties")).getProperty(
                    "mavin.wallpaper.use.scaled.bitmap");
            USE_SCALED_BITMAP = useScaledBitmap.equalsIgnoreCase("Y") ? true
                    : false;

            WallpaperManager _wallpaperManager = WallpaperManager
                    .getInstance(getApplicationContext());
            if (_wallpaperManager != null) {

                setWallpaperDimension();

                AdKey key = new AdKey();
                key.setApplication(ApplicationType.WALLPAPER.getType());
                // always get ads from the AdHandler and never from the AdCache
                final ImageAd _imageAd = (ImageAd) AdHandler.getAd(
                        WallpaperService.this.getApplicationContext(), key);
                AdHandler.setActiveWallpaperAd(_imageAd);

                if (_imageAd != null) {
                    Bitmap useThisBitmap = null;
                    if (USE_SCALED_BITMAP) {
                        Log.i(TAG, " Setting Wallpaper: using scaled image "
                                + _imageAd.getUri());
                        useThisBitmap = Bitmap.createScaledBitmap(Utils
                                .getBitmap(new File(Utils.getStorageDirectory()
                                        + File.separator + _imageAd.getUri())),
                                _wallpaperManager.getDesiredMinimumWidth(),
                                _wallpaperManager.getDesiredMinimumHeight(),
                                true);
                    } else {
                        useThisBitmap = Bitmap.createBitmap(Utils
                                .getBitmap(new File(Utils.getStorageDirectory()
                                        + File.separator + _imageAd.getUri())));
                    }
                    _wallpaperManager.setBitmap(useThisBitmap);
                    Utils.log("Setting Wallpaper: " + _imageAd.getUri());

                    // broadcast the ad that was displayed
                    Intent _message = new Intent();
                    _message.setAction("com.android.wallpaper.AD_DISPLAYED_ACTION");
                    sendBroadcast(_message);
                } else {
                    Utils.warn("No Ad Found");
                }
            } else {
                Utils.warn("WallpaperManager Not Found");
            }

        } catch (Throwable e) {
            Log.e(TAG,
                    "error: "
                            + ((e.getMessage() != null) ? e.getMessage()
                                    .replace(" ", "_") : ""), e);
        } finally {
            Utils.log("Exiting");
        }

    }

    protected void setWallpaperDimension() {
        Utils.log("Entering");
        WallpaperManager _wallpaperManager = WallpaperManager
                .getInstance(getApplicationContext());
        if (_wallpaperManager != null) {
            WindowManager window = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
            Display display = window.getDefaultDisplay();
            int width = display.getWidth();
            int height = display.getHeight();
            _wallpaperManager.suggestDesiredDimensions(width, height);
            Utils.log("done suggestDesiredDimensions()");
        }
        Utils.log("Exiting");
    }

}
