package com.app.android;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;

import com.app.InterfaceListener;
import com.app.Main;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;


import com.google.firebase.FirebaseApp;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

// android launcher
public class AndroidLauncher extends AndroidApplication implements InterfaceListener {
    Main app;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        FirebaseApp.initializeApp(this);

        FirebaseRemoteConfig mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();


        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(3600)
                .build();
        mFirebaseRemoteConfig.setConfigSettingsAsync(configSettings);

        mFirebaseRemoteConfig.fetchAndActivate()
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {

                        String privacyUrl = mFirebaseRemoteConfig.getString("privacy_url");

                        if (!privacyUrl.isEmpty()) {
                            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(privacyUrl));
                            startActivity(browserIntent);
                        }
                    } else {
                        // Обработка ошибок
                        Log.e("RemoteConfig", "Fetch failed");
                    }
                });

        // run
        app = new Main(this);
        runOnUiThread(() -> {
            AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
            config.useImmersiveMode = true;
            ((ViewGroup) findViewById(R.id.app)).addView(initializeForView(app, config));
        });
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void rate() {
        // called if need to rate the App
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + getPackageName())));
    }

}