package com.example.meal_app.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * SessionManager is a utility class to manage user session data using
 * SharedPreferences. It provides methods to create, retrieve, and clear session
 * data.
 */
public class SessionManager {

    // SharedPreferences file name
    private static final String PREF_NAME = "MealAppSession";

    // Keys for storing user session data
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USER_EMAIL = "userEmail";
    private static final String KEY_USER_NAME = "userName";
    private static final String KEY_USER_ADDRESS = "userAddress";
    private static final String KEY_USER_PHONE = "userPhone";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";

    private SharedPreferences pref; // SharedPreferences instance
    private SharedPreferences.Editor editor; // Editor for SharedPreferences
    private Context context; // Application context

    /**
     * Constructor to initialize the SessionManager with the application
     * context.
     *
     * @param context The application context.
     */
    public SessionManager(Context context) {
        this.context = context;
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    /**
     * Creates a login session by storing user details in SharedPreferences.
     *
     * @param userId The user's ID.
     * @param email The user's email.
     * @param name The user's name.
     * @param address The user's address.
     * @param phone The user's phone number.
     */
    public void createLoginSession(String userId, String email, String name, String address, String phone) {
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_USER_EMAIL, email);
        editor.putString(KEY_USER_NAME, name);
        editor.putString(KEY_USER_ADDRESS, address);
        editor.putString(KEY_USER_PHONE, phone);
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.commit(); // Save changes
    }

    /**
     * Checks if the user is logged in.
     *
     * @return True if the user is logged in, false otherwise.
     */
    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    /**
     * Retrieves the user's ID from the session.
     *
     * @return The user's ID, or null if not found.
     */
    public String getUserId() {
        return pref.getString(KEY_USER_ID, null);
    }

    /**
     * Retrieves the user's email from the session.
     *
     * @return The user's email, or null if not found.
     */
    public String getUserEmail() {
        return pref.getString(KEY_USER_EMAIL, null);
    }

    /**
     * Retrieves the user's name from the session.
     *
     * @return The user's name, or null if not found.
     */
    public String getUserName() {
        return pref.getString(KEY_USER_NAME, null);
    }

    /**
     * Retrieves the user's address from the session.
     *
     * @return The user's address, or null if not found.
     */
    public String getUserAddress() {
        return pref.getString(KEY_USER_ADDRESS, null);
    }

    /**
     * Retrieves the user's phone number from the session.
     *
     * @return The user's phone number, or null if not found.
     */
    public String getUserPhone() {
        return pref.getString(KEY_USER_PHONE, null);
    }

    /**
     * Logs the user out by clearing all session data.
     */
    public void logout() {
        editor.clear(); // Clear all stored data
        editor.commit(); // Save changes
    }
}
