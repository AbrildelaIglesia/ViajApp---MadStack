
package com.example.viajapp_madstack;

import android.app.Application;

import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory;

public class ViajAppApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        FirebaseApp.initializeApp(this);

        // App Check para pruebas locales
        if (BuildConfig.DEBUG) {
            FirebaseAppCheck firebaseAppCheck =
                    FirebaseAppCheck.getInstance();

            firebaseAppCheck.installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance()
            );
        }
    }
}
