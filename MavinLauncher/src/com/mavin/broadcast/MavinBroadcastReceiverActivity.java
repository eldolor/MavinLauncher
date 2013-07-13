
package com.mavin.broadcast;

import java.io.File;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;

import com.google.android.gcm.GCMRegistrar;
import com.mavin.ad.AdType;
import com.mavin.ad.ApplicationType;
import com.mavin.ads.AdView;
import com.mavin.device.sync.DeviceRegistrationService;
import com.mavin.gcm.GcmUtilities;
import com.mavin.util.ConfigurationManager;
import com.mavin.util.Utils;

public class MavinBroadcastReceiverActivity extends Activity {
    private BroadcastReceiver mPowerDisconnectedReceiver;
    // private BroadcastReceiver mChargingReceiver;

    // private static final Handler mHandler = new Handler();
    // private static HashMap<String, Article> mArticles = new HashMap<String,
    // Article>();
    // private static ArrayList<String> mArticleIds = new ArrayList<String>();
    private static final String TAG = MavinBroadcastReceiverActivity.class
            .getSimpleName();
    private static final String GCM_ENABLED = ConfigurationManager.getInstance(
            new File(Utils.getStorageDirectory() + File.separator
                    + "config.properties")).getProperty("mavin.gcm.enabled");
    private static final String GOOGLE_API_PROJECT_NUMBER = ConfigurationManager
            .getInstance(
                    new File(Utils.getStorageDirectory() + File.separator
                            + "config.properties")).getProperty(
                    "mavin.google.api.project.number");

    private AsyncTask<String, Void, Void> mRegisterTask;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startDeviceRegistration();
        startWallpaperAlarm();
        setupBroadcastReceivers();
        if (GCM_ENABLED.equalsIgnoreCase("Y")) {
            registerWithGcm();
        }
        setupAd();

        Log.i(TAG, TAG + " onCreate()");
        finish();
    }

    private void startDeviceRegistration() {
        Log.d(TAG, "Entering startDeviceRegistration() !");
        try {
            Intent intent = new Intent(this, DeviceRegistrationService.class);
            PendingIntent pintent = PendingIntent
                    .getService(this, 0, intent, 0);

            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

            int interval = Integer.valueOf(ConfigurationManager.getInstance(
                    new File(Utils.getStorageDirectory() + File.separator
                            + "config.properties")).getProperty(
                    "mavin.device.registration.interval.milliseconds"));

            long timeToRefresh = SystemClock.elapsedRealtime() + interval;
            alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    timeToRefresh, interval, pintent);
        } catch (Throwable t) {
            Log.e(TAG, t.getMessage(), t);
        }
        Log.d(TAG, "Exiting startDeviceRegistration() !");
    }

    private void registerWithGcm() {
        // Make sure the device has the proper dependencies.
        GCMRegistrar.checkDevice(MavinBroadcastReceiverActivity.this
                .getApplicationContext());
        // Make sure the manifest was properly set - comment out this line
        // while developing the app, then uncomment it when it's ready.
        // GCMRegistrar.checkManifest(ChargingScreenBroadcastReceiverActivity.this.getApplicationContext());

        String regId = GCMRegistrar
                .getRegistrationId(MavinBroadcastReceiverActivity.this
                        .getApplicationContext());
        Log.i(TAG, "Registration Id: " + regId);
        if (regId.equals("")) {
            // register this device
            GCMRegistrar
                    .register(MavinBroadcastReceiverActivity.this
                            .getApplicationContext(), GOOGLE_API_PROJECT_NUMBER);
            regId = GCMRegistrar
                    .getRegistrationId(MavinBroadcastReceiverActivity.this
                            .getApplicationContext());
            Log.i(MavinBroadcastReceiverActivity.class.getSimpleName(),
                    "Registration Id: " + regId);
        } else {
            // Device is already registered on GCM, check server.
            if (GCMRegistrar
                    .isRegisteredOnServer(MavinBroadcastReceiverActivity.this
                            .getApplicationContext())) {
                // Skips registration.
                // mDisplay.append(getString(R.string.already_registered) +
                // "\n");
                Log.i(TAG, "Device already registered");
            } else {
                // Try to register again, but not in the UI thread.
                // It's also necessary to cancel the thread onDestroy(),
                // hence the use of AsyncTask instead of a raw thread.
                final Context context = MavinBroadcastReceiverActivity.this
                        .getApplicationContext();
                mRegisterTask = new AsyncTask<String, Void, Void>() {

                    @Override
                    protected Void doInBackground(String... params) {
                        String _regId = params[0];
                        Log.i(AsyncTask.class.getSimpleName(),
                                "Registration Id: " + _regId);

                        boolean registered = GcmUtilities.register(context,
                                _regId);
                        // At this point all attempts to register with the app
                        // server failed, so we need to unregister the device
                        // from GCM - the app will try to register again when
                        // it is restarted. Note that GCM will send an
                        // unregistered callback upon completion, but
                        // GCMIntentService.onUnregistered() will ignore it.
                        if (!registered) {
                            GCMRegistrar.unregister(context);
                        }
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void result) {
                        mRegisterTask = null;
                    }

                };
                // execute the async task
                mRegisterTask.execute(regId, null, null);
            }
        }
    }

    private void startWallpaperAlarm() {
        Log.d(TAG, "Entering startWallpaperAlarm() !");
        try {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent();
            intent.setAction("com.mavin.wallpaper.ALARM_ACTION");
            PendingIntent alarmIntent = PendingIntent.getBroadcast(this, 0,
                    intent, PendingIntent.FLAG_UPDATE_CURRENT);

            int interval = Integer.valueOf(ConfigurationManager.getInstance(
                    new File(Utils.getStorageDirectory() + File.separator
                            + "config.properties")).getProperty(
                    "mavin.wallpaper.refresh.interval.milliseconds"));
            long timeToRefresh = SystemClock.elapsedRealtime() + interval;
            alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    timeToRefresh, interval, alarmIntent);
            Log.d(TAG, "Exiting startWallpaperAlarm() !");
        } catch (Throwable t) {
            Log.e(TAG, t.getMessage(), t);
        }
    }

    private void setupAd() {
        Log.d(TAG, "Entering setupAd() !");
        AdView adView = new AdView(this, AdType.INTERSTITIAL.getType(),
                ApplicationType.CHARGE_SCREEN.getType(), null);
        if (!adView.displayAd()) {
            Utils.triggerCampaignDownload(this);
        }

        // do this first
        // setContentView(R.layout.charging_screen);
        // AdView adView = (AdView) this.findViewById(R.id.adView);
        // if(!adView.displayAd())
        // finish();
        // else {
        // ImageView _home = ((ImageView) MavinBroadcastReceiverActivity.this
        // .findViewById(R.id.home));
        // _home.setOnClickListener(new View.OnClickListener() {
        //
        // @Override
        // public void onClick(View v) {
        // // TODO: delete later
        // if (!RegisteredDevice.isDeviceRegistered()) {
        // Intent deviceRegistrationService = new Intent(
        // MavinBroadcastReceiverActivity.this,
        // DeviceRegistrationService.class);
        // MavinBroadcastReceiverActivity.this
        // .startService(deviceRegistrationService);
        // Log.i(TAG, "Starting DeviceRegistrationService");
        // }
        // Intent campaignDownloadService = new Intent(
        // MavinBroadcastReceiverActivity.this,
        // CampaignDownloadService.class);
        // MavinBroadcastReceiverActivity.this
        // .startService(campaignDownloadService);
        // Log.i(TAG, "Starting CampaignDownloadService");
        //
        // MavinBroadcastReceiverActivity.this.finish();
        // }
        // });
        //
        // }

        /*
         * AdKey key = new AdKey();
         * key.setApplication(ApplicationType.CHARGE_SCREEN.getType()); Ad ad =
         * com.mavin.ad.AdHandler.getAd(this, key); if (ad != null &&
         * RegisteredDevice.isDeviceRegistered()) { if
         * (ad.getType().equals(AdType.IMAGE_AD.getType()) ||
         * ad.getType().equals(AdType.REMINDER_AD.getType())) { if
         * (com.mavin.ad.AdHandler.displayMavinAd(this, R.id.mavin_adview, ad,
         * key)) { ImageView _home = ((ImageView)
         * MavinBroadcastReceiverActivity.this .findViewById(R.id.home));
         * _home.setOnClickListener(new View.OnClickListener() {
         * @Override public void onClick(View v) { // TODO: delete later if
         * (!RegisteredDevice.isDeviceRegistered()) { Intent
         * deviceRegistrationService = new Intent(
         * MavinBroadcastReceiverActivity.this,
         * DeviceRegistrationService.class); MavinBroadcastReceiverActivity.this
         * .startService(deviceRegistrationService); Log.i(TAG,
         * "Starting DeviceRegistrationService"); } Intent
         * campaignDownloadService = new Intent(
         * MavinBroadcastReceiverActivity.this, CampaignDownloadService.class);
         * MavinBroadcastReceiverActivity.this
         * .startService(campaignDownloadService); Log.i(TAG,
         * "Starting CampaignDownloadService");
         * MavinBroadcastReceiverActivity.this.finish(); } }); } else {
         * Log.e(TAG, "Unable to display Ad!...closing activity");
         * MavinBroadcastReceiverActivity.this.finish(); } } else if
         * (ad.getType().equals(AdType.VIDEO_AD.getType())) { // play the video
         * final VideoAd _videoAd = (VideoAd) ad; Intent intent = new
         * Intent(this, PlayMedia.class); Bundle extras = new Bundle();
         * intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
         * intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY); if
         * ((_videoAd.getUri().startsWith("http://")) ||
         * (_videoAd.getUri().startsWith("https://"))) { extras.putString("URI",
         * _videoAd.getUri()); Log.i(TAG, " URI: " + _videoAd.getUri()); } else
         * { extras.putString("URI", Utils.getStorageDirectory() +
         * File.separator + _videoAd.getUri()); Log.i(TAG, " URI: " +
         * Utils.getStorageDirectory() + File.separator + _videoAd.getUri()); }
         * // play muted extras.putBoolean("PLAY_MUTED", true);
         * extras.putBoolean("CLOSE_ON_FINISH", false);
         * extras.putSerializable("MAVIN_AD", _videoAd); Log.i(TAG,
         * " MAVIN_AD: " + _videoAd.getUri()); intent.putExtras(extras);
         * startActivity(intent); MavinBroadcastReceiverActivity.this.finish();
         * } else if (ad.getType().equals(AdType.VOICE_AD.getType())) { // play
         * the video final VoiceAd _voiceAd = (VoiceAd) ad; Intent intent = new
         * Intent(this, PlayMedia.class); Bundle extras = new Bundle();
         * intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
         * intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY); if
         * ((_voiceAd.getUri().startsWith("http://")) ||
         * (_voiceAd.getUri().startsWith("https://"))) { extras.putString("URI",
         * _voiceAd.getUri()); Log.i(TAG, " URI: " + _voiceAd.getUri()); } else
         * { extras.putString("URI", Utils.getStorageDirectory() +
         * File.separator + _voiceAd.getUri()); Log.i(TAG, " URI: " +
         * Utils.getStorageDirectory() + File.separator + _voiceAd.getUri()); }
         * extras.putSerializable("MAVIN_AD", _voiceAd); Log.i(TAG,
         * " MAVIN_AD: " + _voiceAd.getUri()); intent.putExtras(extras);
         * startActivity(intent); MavinBroadcastReceiverActivity.this.finish();
         * } else if (ad.getType().equals(AdType.SURVEY.getType())) { Intent
         * intent = new Intent(this, ViewSurvey.class);
         * intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
         * intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY); Log.i(TAG,
         * " MAVIN_SURVEY_ID: " + ad.getId());
         * intent.putExtra("MAVIN_SURVEY_ID", ad.getId());
         * startActivity(intent); MavinBroadcastReceiverActivity.this.finish();
         * } } // if no ads were found then trigger registration and campaign //
         * download, // and end the activity else { if
         * (!RegisteredDevice.isDeviceRegistered()) { // Device registration
         * will automatically download the new // campaign Log.w(TAG,
         * "Device is not Registered"); Toast.makeText(getApplicationContext(),
         * R.string.begin_device_registration, Toast.LENGTH_LONG) .show();
         * Intent deviceRegistrationService = new Intent(
         * MavinBroadcastReceiverActivity.this,
         * DeviceRegistrationService.class); MavinBroadcastReceiverActivity.this
         * .startService(deviceRegistrationService); Log.i(TAG,
         * "Starting DeviceRegistrationService"); } else if (ad == null) {
         * Log.w(TAG, "No ads found for Charge Screen"); Intent
         * campaignDownloadService = new Intent(
         * MavinBroadcastReceiverActivity.this, CampaignDownloadService.class);
         * MavinBroadcastReceiverActivity.this
         * .startService(campaignDownloadService); Log.i(TAG,
         * "Starting CampaignDownloadService"); }
         * MavinBroadcastReceiverActivity.this.finish(); }
         */
        Log.d(TAG, "Exiting setupAd() !");
    }

    /**
     * 
     */
    private void setupBroadcastReceivers() {
        Log.d(TAG, "Entering setupBroadcastReceivers() !");
        try {
            mPowerDisconnectedReceiver = new PowerDisconnectedReceiver(this);
            IntentFilter powerDisconnectedFilter = new IntentFilter(
                    Intent.ACTION_POWER_DISCONNECTED);
            registerReceiver(mPowerDisconnectedReceiver,
                    powerDisconnectedFilter);

            // mChargingReceiver = new ChargingReceiver(this);
            // IntentFilter chargingFilter = new IntentFilter(
            // Intent.ACTION_BATTERY_CHANGED);
            // registerReceiver(mChargingReceiver, chargingFilter);

        } catch (Throwable t) {
            Log.e(TAG, t.getMessage(), t);
        }
        Log.d(TAG, "Exiting setupBroadcastReceivers() !");
    }

    // @Override
    // protected void onStart() {
    // super.onStart();
    // Log.i(TAG, TAG + " onStart()");
    // setupAd();
    // //
    // AdUtil.loadInterstitialAd(BatteryStatusBroadcastReceiverActivity.this);
    // // AdUtil.setupAdMob(this);
    // // new AsyncGetWeatherInfoTask().execute("");
    // // new AsyncGetNewsFeedTask().execute("");
    //
    // }

    @Override
    protected void onResume() {
        super.onResume();
        // AdUtil.loadInterstitialAd(BatteryStatusBroadcastReceiverActivity.this);
        // AdUtil.setupAdMob(this);
        // new AsyncGetWeatherInfoTask().execute("");
        // new AsyncGetNewsFeedTask().execute("");
        Log.i(TAG, TAG + " onResume()");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, TAG + " onDestroy()");
        unregisterReceiver(mPowerDisconnectedReceiver);
        // unregisterReceiver(mChargingReceiver);
        if (mRegisterTask != null) {
            mRegisterTask.cancel(true);
        }
        GCMRegistrar.onDestroy(MavinBroadcastReceiverActivity.this
                .getApplicationContext());
    }

    /**************************************************************************************/

    /**
     * @author anshu
     */
    class PowerDisconnectedReceiver extends BroadcastReceiver {
        private Activity activity;
        private String TAG = PowerDisconnectedReceiver.class.getSimpleName();

        public PowerDisconnectedReceiver(Activity activity) {
            super();
            this.activity = activity;
            Log.i("PowerDisconnectedReceiver", TAG + " created");
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            activity.finish();
            Log.i("PowerDisconnectedReceiver", TAG + " onReceive()");

        }

    }

    /**************************************************************************************/

    /**
     * @author anshu
     */
    /*
     * class ChargingReceiver extends BroadcastReceiver { private Activity
     * activity; private TextView charging; private String TAG =
     * ChargingReceiver.class.getSimpleName(); public ChargingReceiver(Activity
     * activity) { super(); this.activity = activity; charging = (TextView)
     * activity.findViewById(R.id.charging); Log.i("ChargingReceiver", TAG +
     * " created"); }
     * @Override public void onReceive(Context context, Intent intent) { // Are
     * we charging charged? int status =
     * intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1); boolean isCharging =
     * status == BatteryManager.BATTERY_STATUS_CHARGING || status ==
     * BatteryManager.BATTERY_STATUS_FULL; Log.i("ChargingReceiver", TAG +
     * " isCharging: " + ((isCharging) ? "Y" : "N")); if (isCharging) { int
     * level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1); int scale =
     * intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1); float batteryPct =
     * level / (float) scale; if (charging != null) {
     * charging.setText("Charging, " + (batteryPct * 100) + "%"); if
     * (charging.getVisibility() == View.GONE) { //
     * charging.setVisibility(View.VISIBLE); } } } Date now = new Date();
     * SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM d");
     * SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm aaa");
     * Configuration userConfig = new Configuration(); Calendar cal =
     * Calendar.getInstance(userConfig.locale); TimeZone tz = cal.getTimeZone();
     * timeFormat.setTimeZone(tz); dateFormat.setTimeZone(tz); ((TextView)
     * findViewById(R.id.date)) .setText(dateFormat.format(now)); ((TextView)
     * findViewById(R.id.time)) .setText(timeFormat.format(now));
     * Log.i("ChargingReceiver", TAG + " onReceive()"); } }
     */
    /**************************************************************************************/

    /**************************************************************************************/
    /*
     * private class AsyncGetWeatherInfoTask extends AsyncTask<Object, Void,
     * Void> { private String TAG =
     * AsyncGetWeatherInfoTask.class.getSimpleName(); private Document doc;
     * private String condition, temp, humidity, iconUrl, windCondition,
     * station; private Drawable drawable;
     *//**
     * @param args
     * @return null
     */
    /*
     * protected Void doInBackground(Object... args) { Log.i(TAG,
     * "doInBackground starting"); try { LocationManager locationManager =
     * (LocationManager) getSystemService(LOCATION_SERVICE); Criteria criteria =
     * new Criteria(); String bestProvider =
     * locationManager.getBestProvider(criteria, false); Location location =
     * locationManager.getLastKnownLocation(bestProvider); if (location != null)
     * { String url = "http://api.wxbug.net/getLiveWeatherRSS.aspx?ACode=" +
     * AppConfig.WEATHERBUG_API_KEY + "&lat=" + location.getLatitude() +
     * "&long=" + location.getLongitude() + "&unittype=0&outputtype=1";
     * Log.i(TAG, "URL: " + url); String xml = Utils .getXmlFromUrl(url); //
     * String xml = //
     * "<aws:weather xmlns:aws='http://www.aws.com/aws'><aws:api version='2.0' /><aws:WebURL>http://weather.weatherbug.com/India/Delhi Cantonment-weather.html?ZCode=Z5546&amp;Units=0&amp;stat=VIDP</aws:WebURL><aws:InputLocationURL>http://weather.weatherbug.com/India/Delhi-weather.html?ZCode=Z5546&amp;Units=0</aws:InputLocationURL><aws:ob><aws:ob-date><aws:year number='2012' /><aws:month number='7' text='July' abbrv='Jul' /><aws:day number='2' text='Monday' abbrv='Mon' /><aws:hour number='5' hour-24='05' /><aws:minute number='30' /><aws:second number='00' /><aws:am-pm abbrv='AM' /><aws:time-zone offset='5.5' text='Indian Standard Time' abbrv='IST' /></aws:ob-date><aws:requested-station-id /><aws:station-id>VIDP</aws:station-id><aws:station>Delhi (Indira Gandhi)</aws:station><aws:city-state citycode='64469'>Delhi,  IN</aws:city-state><aws:country>India</aws:country><aws:latitude>28.5666675567627</aws:latitude><aws:longitude>77.1166687011719</aws:longitude><aws:site-url /><aws:aux-temp units='&amp;deg;F'>0</aws:aux-temp><aws:aux-temp-rate units='&amp;deg;F'>-52.9</aws:aux-temp-rate><aws:current-condition icon='http://deskwx.weatherbug.com/images/Forecast/icons/cond003.gif'>Partly Cloudy</aws:current-condition><aws:dew-point units='&amp;deg;F'>75</aws:dew-point><aws:elevation units='ft'>721</aws:elevation><aws:feels-like units='&amp;deg;F'>102</aws:feels-like><aws:gust-time><aws:year number='2012' /><aws:month number='7' text='July' abbrv='Jul' /><aws:day number='2' text='Monday' abbrv='Mon' /><aws:hour number='12' hour-24='00' /><aws:minute number='30' /><aws:second number='00' /><aws:am-pm abbrv='AM' /><aws:time-zone offset='5.5' text='Indian Standard Time' abbrv='IST' /></aws:gust-time><aws:gust-direction>ESE</aws:gust-direction><aws:gust-direction-degrees>110</aws:gust-direction-degrees><aws:gust-speed units='mph'>11</aws:gust-speed><aws:humidity units='%'>59</aws:humidity><aws:humidity-high units='%'>6</aws:humidity-high><aws:humidity-low units='%'>6</aws:humidity-low><aws:humidity-rate>0</aws:humidity-rate><aws:indoor-temp units='&amp;deg;F'>0</aws:indoor-temp><aws:indoor-temp-rate units='&amp;deg;F'>0</aws:indoor-temp-rate><aws:light>0</aws:light><aws:light-rate>0</aws:light-rate><aws:moon-phase moon-phase-img='http://api.wxbug.net/images/moonphase/mphase13.gif'>-95</aws:moon-phase><aws:pressure units='&quot;'>29.44</aws:pressure><aws:pressure-high units='&quot;'>29.44</aws:pressure-high><aws:pressure-low units='&quot;'>29.44</aws:pressure-low><aws:pressure-rate units='&quot;/h'>0</aws:pressure-rate><aws:rain-month units='&quot;'>0</aws:rain-month><aws:rain-rate units='&quot;/h'>0</aws:rain-rate><aws:rain-rate-max units='&quot;/h'>0</aws:rain-rate-max><aws:rain-today units='&quot;'>0</aws:rain-today><aws:rain-year units='&quot;'>0</aws:rain-year><aws:temp units='&amp;deg;F'>91.4</aws:temp><aws:temp-high units='&amp;deg;F'>99</aws:temp-high><aws:temp-low units='&amp;deg;F'>91</aws:temp-low><aws:temp-rate units='&amp;deg;F/h'>0</aws:temp-rate><aws:sunrise><aws:year number='2012' /><aws:month number='7' text='July' abbrv='Jul' /><aws:day number='2' text='Monday' abbrv='Mon' /><aws:hour number='5' hour-24='05' /><aws:minute number='27' /><aws:second number='52' /><aws:am-pm abbrv='AM' /><aws:time-zone offset='5.5' text='Indian Standard Time' abbrv='IST' /></aws:sunrise><aws:sunset><aws:year number='2012' /><aws:month number='7' text='July' abbrv='Jul' /><aws:day number='2' text='Monday' abbrv='Mon' /><aws:hour number='7' hour-24='19' /><aws:minute number='23' /><aws:second number='16' /><aws:am-pm abbrv='PM' /><aws:time-zone offset='5.5' text='Indian Standard Time' abbrv='IST' /></aws:sunset><aws:wet-bulb units='&amp;deg;F'>79.52</aws:wet-bulb><aws:wind-speed units='mph'>9</aws:wind-speed><aws:wind-speed-avg units='mph'>9</aws:wind-speed-avg><aws:wind-direction>ESE</aws:wind-direction><aws:wind-direction-degrees>109</aws:wind-direction-degrees><aws:wind-direction-avg>ESE</aws:wind-direction-avg></aws:ob></aws:weather>"
     * ; doc = Utils.getDomElement(xml, true); // getting DOM element } else {
     * Log.w(TAG, "URL is Null"); } if (doc != null) { XPath xpath =
     * XPathFactory.newInstance().newXPath(); xpath.setNamespaceContext(new
     * NamespaceContext() {
     * @Override public Iterator getPrefixes(String namespaceURI) { return null;
     * }
     * @Override public String getPrefix(String namespaceURI) { if
     * ("http://www.aws.com/aws".equals(namespaceURI)) { return "aws"; } return
     * null; }
     * @Override public String getNamespaceURI(String prefix) { if
     * ("aws".equals(prefix)) { return "http://www.aws.com/aws"; } return null;
     * } }); // station = (String) xpath.evaluate( //
     * "/aws:weather/aws:ob/aws:station", // doc.getDocumentElement()); //
     * station = station + ", " + (String) xpath.evaluate( //
     * "/aws:weather/aws:ob/city-state", // doc.getDocumentElement()); //
     * station = station + ", " + (String) xpath.evaluate( //
     * "/aws:weather/aws:ob/aws:country", // doc.getDocumentElement());
     * condition = (String) xpath.evaluate(
     * "/aws:weather/aws:ob/aws:current-condition", doc.getDocumentElement());
     * iconUrl = (String) xpath.evaluate(
     * "/aws:weather/aws:ob/aws:current-condition/@icon",
     * doc.getDocumentElement()); drawable =
     * DrawableManager.getInstance().fetchDrawable( iconUrl); temp = (String)
     * xpath.evaluate( "/aws:weather/aws:ob/aws:temp", doc.getDocumentElement())
     * + "\u00B0" + "F"; humidity = "Humidity: " + (String) xpath.evaluate(
     * "/aws:weather/aws:ob/aws:humidity", doc.getDocumentElement()); humidity =
     * humidity + (String) xpath.evaluate(
     * "/aws:weather/aws:ob/aws:humidity/@units", doc.getDocumentElement());
     * windCondition = "Wind Speed: " + (String) xpath.evaluate(
     * "/aws:weather/aws:ob/aws:gust-speed", doc.getDocumentElement());
     * windCondition = windCondition + (String) xpath.evaluate(
     * "/aws:weather/aws:ob/aws:gust-speed/@units", doc.getDocumentElement());
     * Log.i(TAG, condition + temp + humidity + iconUrl + windCondition);
     * ChargingScreenBroadcastReceiverActivity.this.runOnUiThread(new Runnable()
     * {
     * @Override public void run() { try { // TextView _station = ((TextView) //
     * ChargingScreenBroadcastReceiverActivity.this //
     * .findViewById(R.id.station)); // _station.setText(station); //
     * _station.setVisibility(View.VISIBLE); ImageView _icon = ((ImageView)
     * ChargingScreenBroadcastReceiverActivity.this .findViewById(R.id.icon));
     * _icon.setImageDrawable(drawable); _icon.setVisibility(View.VISIBLE);
     * TextView _condition = ((TextView)
     * ChargingScreenBroadcastReceiverActivity.this
     * .findViewById(R.id.condition)); _condition.setText(condition);
     * _condition.setVisibility(View.VISIBLE); TextView _temp = ((TextView)
     * ChargingScreenBroadcastReceiverActivity.this .findViewById(R.id.temp));
     * _temp.setText(temp); _temp.setVisibility(View.VISIBLE); TextView
     * _humidity = ((TextView) ChargingScreenBroadcastReceiverActivity.this
     * .findViewById(R.id.humidity)); _humidity.setText(humidity);
     * _humidity.setVisibility(View.VISIBLE); TextView _windCondition =
     * ((TextView) ChargingScreenBroadcastReceiverActivity.this
     * .findViewById(R.id.wind_condition));
     * _windCondition.setText(windCondition);
     * _windCondition.setVisibility(View.VISIBLE); } catch (Throwable e) {
     * Log.e(TAG, "error: " + ((e.getMessage() != null) ? e.getMessage()
     * .replace(" ", "_") : ""), e); } } }); } } catch (Throwable e) {
     * Log.e(TAG, "error: " + ((e.getMessage() != null) ? e.getMessage()
     * .replace(" ", "_") : ""), e); } Log.i(TAG, "doInBackground finished");
     * return null; }
     * @Override protected void onPostExecute(Void result) { Log.i(TAG,
     * "onPostExecute starting"); try { } catch (Throwable e) { Log.e(TAG,
     * "error: " + ((e.getMessage() != null) ? e.getMessage() .replace(" ", "_")
     * : ""), e); } Log.i(TAG, "onPostExecute finished"); } }
     *//**************************************************************************************/
    /*
     * private class AsyncGetNewsFeedTask extends AsyncTask<Object, Void, Void>
     * { private String TAG = AsyncGetNewsFeedTask.class.getSimpleName();
     * private Document doc;
     * @Override protected void onPreExecute() { // TODO Auto-generated method
     * stub super.onPreExecute(); }
     *//**
     * @param args
     * @return null
     */
    /*
     * protected Void doInBackground(Object... args) { Log.i(TAG,
     * "doInBackground starting"); try { String url =
     * "http://feeds.bbci.co.uk/news/rss.xml?edition=us"; Log.i(TAG, "URL: " +
     * url); String xml = Utils .getXmlFromUrl(url); // String xml =
     * "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" // + //
     * "<?xml-stylesheet title=\"XSL_formatting\" type=\"text/xsl\" href=\"/shared/bsp/xsl/rss/nolsol.xsl\"?>"
     * // // + //
     * "<rss xmlns:media=\"http://search.yahoo.com/mrss/\" xmlns:atom=\"http://www.w3.org/2005/Atom\" version=\"2.0\">  "
     * // + "<channel> " // + "<title>BBC News - Home</title>  " // + //
     * "    <link>http://www.bbc.co.uk/news/#sa-ns_mchannel=rss&amp;ns_source=PublicRSS20-sa</link>  "
     * // + //
     * "  <description>The latest stories from the Home section of the BBC News web site.</description> "
     * // + "<language>en-gb</language>  " // + //
     * " <lastBuildDate>Wed, 11 Jul 2012 15:43:04 GMT</lastBuildDate> " // + //
     * "<copyright>Copyright: (C) British Broadcasting Corporation, see http://news.bbc.co.uk/2/hi/help/rss/4498287.stm for terms and conditions of reuse.</copyright>"
     * // + "<image> " // + //
     * "<url>http://news.bbcimg.co.uk/nol/shared/img/bbc_news_120x60.gif</url>  "
     * // + "<title>BBC News - Home</title>  " // + //
     * "<link>http://www.bbc.co.uk/news/#sa-ns_mchannel=rss&amp;ns_source=PublicRSS20-sa</link>  "
     * // + "<width>120</width>  " // + "<height>60</height> " // + "</image>  "
     * // + "<ttl>15</ttl>  " // + //
     * "<atom:link href=\"http://feeds.bbci.co.uk/news/rss.xml\" rel=\"self\" type=\"application/rss+xml\"/>  "
     * // + "<item> " // + " <title>Spain plans sweeping budget cuts</title>  "
     * // + //
     * " <description>Spanish Prime Minister Mariano Rajoy announces sweeping budget cuts and tax rises, as police clash with protesters in Madrid.</description>"
     * // + //
     * " <link>http://www.bbc.co.uk/news/world-europe-18792427#sa-ns_mchannel=rss&amp;ns_source=PublicRSS20-sa</link>  "
     * // + //
     * " <guid isPermaLink=\"false\">http://www.bbc.co.uk/news/world-europe-18792427</guid>  "
     * // + " <pubDate>Wed, 11 Jul 2012 14:18:42 GMT</pubDate>  " // + //
     * " <media:thumbnail width=\"66\" height=\"49\" url=\"http://news.bbcimg.co.uk/media/images/61507000/jpg/_61507559_61506548.jpg\"/>  "
     * // + //
     * " <media:thumbnail width=\"144\" height=\"81\" url=\"http://news.bbcimg.co.uk/media/images/61507000/jpg/_61507560_61506548.jpg\"/> "
     * // + " </item>  " // + " <item> " // +
     * "  <title>Clinton on historic visit to Laos</title>  " // + //
     * "   <description>Hillary Clinton becomes the first US secretary of state to visit Laos in 57 years, in a historic trip discussing economic ties and unexploded bombs.</description>"
     * // + //
     * "  <link>http://www.bbc.co.uk/news/world-asia-18792282#sa-ns_mchannel=rss&amp;ns_source=PublicRSS20-sa</link> "
     * // + //
     * "  <guid isPermaLink=\"false\">http://www.bbc.co.uk/news/world-asia-18792282</guid>  "
     * // + "  <pubDate>Wed, 11 Jul 2012 08:00:58 GMT</pubDate>  " // + //
     * "  <media:thumbnail width=\"66\" height=\"49\" url=\"http://news.bbcimg.co.uk/media/images/61492000/jpg/_61492616_bkel0ks0.jpg\"/> "
     * // + //
     * "  <media:thumbnail width=\"144\" height=\"81\" url=\"http://news.bbcimg.co.uk/media/images/61492000/jpg/_61492617_bkel0ks0.jpg\"/> "
     * // + " </item>  " // + "</channel> " // + "</rss>"; // Log.i(TAG, "RSS: "
     * + xml); doc = Utils.getDomElement(xml, true); // getting DOM element if
     * (doc != null) { XPath xpath = XPathFactory.newInstance().newXPath();
     * xpath.setNamespaceContext(new NamespaceContext() {
     * @Override public Iterator getPrefixes(String namespaceURI) { return null;
     * }
     * @Override public String getPrefix(String namespaceURI) { if
     * ("http://search.yahoo.com/mrss/".equals(namespaceURI)) { return "media";
     * } else if ("http://www.w3.org/2005/Atom".equals(namespaceURI)) { return
     * "atom"; } return null; }
     * @Override public String getNamespaceURI(String prefix) { if
     * ("media".equals(prefix)) { return "http://search.yahoo.com/mrss/"; } else
     * if ("atom".equals(prefix)) { return "http://www.w3.org/2005/Atom"; }
     * return null; } }); // remove old articles mArticleIds.clear();
     * mArticles.clear(); TextView _newsFeedTitle = ((TextView)
     * ChargingScreenBroadcastReceiverActivity.this
     * .findViewById(R.id.newsFeedTitle)); NodeList nl =
     * doc.getElementsByTagName("item"); for (int i = 0; i < nl.getLength();
     * i++) { Node node = nl.item(i); String _title = (String) xpath.evaluate(
     * "title", node); String _link = (String) xpath.evaluate( "link", node);
     * String _description = (String) xpath.evaluate( "description", node);
     * String _articleId = (String) xpath.evaluate( "guid", node); Article
     * _article = new Article(); _article.articleId = _articleId;
     * _article.feedId = "http://feeds.bbci.co.uk/news/rss.xml?edition=us";
     * _article.title = _title; _article.description = _description;
     * _article.link = _link; Log.i(TAG, "Title: " + _title);
     * mArticles.put(_articleId, _article); mArticleIds.add(_articleId); // do
     * it once if (i == 0) { mHandler.post(displayArticles(false)); } } //
     * trigger mHandler.post(displayArticles(true)); } else { Log.w(TAG,
     * "News Feed is NULL"); } } catch (Throwable e) { Log.e(TAG, "error: " +
     * ((e.getMessage() != null) ? e.getMessage() .replace(" ", "_") : ""), e);
     * } Log.i(TAG, "doInBackground finished"); return null; }
     *//**
     * @param recurse
     * @return
     */
    /*
     * private Runnable displayArticles(final boolean recurse) { // for (int i =
     * 0; i < mArticles.keySet().size(); i++) { Runnable notification = new
     * Runnable() { public void run() { try { Log.i(TAG,
     * "displayArticles begin"); int _random = Utils.getRandomInt(0,
     * (mArticleIds.size() - 1)); final Article _article = mArticles
     * .get(mArticleIds.get(_random)); TextView _newsFeedTitle = ((TextView)
     * ChargingScreenBroadcastReceiverActivity.this
     * .findViewById(R.id.newsFeedTitle)); if (_newsFeedTitle != null) {
     * _newsFeedTitle.setText(_article.title); Log.i(TAG, "Setting Title: " +
     * _article.title); _newsFeedTitle.setOnClickListener(new
     * View.OnClickListener() {
     * @Override public void onClick(View v) { Intent myIntent = new
     * Intent(Intent.ACTION_VIEW, Uri .parse(_article.link));
     * startActivity(myIntent); } }); TextView _newsFeedDescription =
     * ((TextView) ChargingScreenBroadcastReceiverActivity.this
     * .findViewById(R.id.newsFeedDescription));
     * _newsFeedDescription.setText(_article.description); _newsFeedDescription
     * .setOnClickListener(new View.OnClickListener() {
     * @Override public void onClick(View v) { Intent myIntent = new Intent(
     * Intent.ACTION_VIEW, Uri .parse(_article.link)); startActivity(myIntent);
     * } }); } // recursion if (recurse) {
     * mHandler.postDelayed(displayArticles(true), 10000); } Log.i(TAG,
     * "displayArticles finished"); } catch (Throwable e) { Log .e(TAG,
     * (e.getMessage() != null) ? e .getMessage().replace(" ", "_") : "", e); }
     * } }; // } return notification; }
     * @Override protected void onPostExecute(Void result) { Log.i(TAG,
     * "onPostExecute starting"); try { } catch (Throwable e) { Log.e(TAG,
     * "error: " + ((e.getMessage() != null) ? e.getMessage() .replace(" ", "_")
     * : ""), e); } Log.i(TAG, "onPostExecute finished"); } }
     */
}
