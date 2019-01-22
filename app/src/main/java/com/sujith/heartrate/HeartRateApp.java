package com.sujith.heartrate;

import android.app.Application;

import timber.log.Timber;

/**
 * Created by sujit on 17-03-2018.
 */

public class HeartRateApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Timber.plant(new Timber.DebugTree());
    }
}
