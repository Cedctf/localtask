package com.example.assignment2;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "UserSession";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USER_NAME = "userName";
    private static final String KEY_USER_TYPE = "userType";
    private static final String KEY_USER_EMAIL = "userEmail";
    private static final String KEY_USER_ID = "userId";

    private final SharedPreferences pref;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    public void createLoginSession(String userName, String userType, String userEmail, String userId) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_USER_NAME, userName);
        editor.putString(KEY_USER_TYPE, userType);
        editor.putString(KEY_USER_EMAIL, userEmail);
        editor.putString(KEY_USER_ID, userId);
        editor.apply();
    }

    public void createLoginSession(String userName, String userType, String userEmail) {
        createLoginSession(userName, userType, userEmail, null);
    }

    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public void logoutUser() {
        editor.clear();
        editor.apply();
    }

    public String getUserName() {
        return pref.getString(KEY_USER_NAME, null);
    }

    public String getUserType() {
        return pref.getString(KEY_USER_TYPE, null);
    }

    public String getUserEmail() {
        return pref.getString(KEY_USER_EMAIL, null);
    }

    public String getUserId() {
        return pref.getString(KEY_USER_ID, null);
    }
}