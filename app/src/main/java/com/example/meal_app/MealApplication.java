package com.example.meal_app;

import android.app.Application;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Custom Application class for the Meal App. This class is used to initialize
 * Firebase and configure Firestore settings.
 */
public class MealApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize Firebase SDK
        FirebaseApp.initializeApp(this);

        // Enable Firestore logging for debugging purposes
        FirebaseFirestore.setLoggingEnabled(true);
    }
}
