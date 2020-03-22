package com.example.WiFiPasswordSearcher;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;


/**
 * Created by пк on 19.11.2015.
 */
public class Settings
{
    private static final String APP_PREFERENCES = "settings";
    static final String APP_SERVER_LOGIN = "SERVER_LOGIN";
    static final String APP_SERVER_PASSWORD = "SERVER_PASSWORD";
    static final String APP_SERVER_URI = "SERVER_URI";
    static final String APP_FETCH_ESS = "FETCH_ESS";
    static final String APP_CHECK_UPDATES = "CHECK_UPDATES";
    static final String API_READ_KEY = "READ_KEY";
    static final String API_WRITE_KEY = "WRITE_KEY";
    static final String API_KEYS_VALID = "KEYS_VALID";
    static final String USER_REGDATE = "USER_REGDATE";
    static final String USER_NICK = "USER_NICK";
    static final String USER_GROUP = "USER_GROUP";
    static final String WIFI_SIGNAL = "WIFI_SIGNAL";
    static final String WPS_SOURCE = "WPS_SOURCE";


    SharedPreferences AppSettings = null;
    SharedPreferences.Editor Editor = null;

    private Context context;

    public Settings(Context _context)
    {
        context = _context;
        Init();
    }

    @SuppressLint("CommitPrefEdits")
    private void Init()
    {
        AppSettings = context.getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);
        Editor = AppSettings.edit();
    }

    void Reload()
    {
        Init();
    }
}
