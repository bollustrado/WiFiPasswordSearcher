package com.example.WiFiPasswordSearcher;

import android.content.Context;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

/**
 * Created by пк on 20.12.2015.
 */
public class UserManager {
    private Context context;
    private Settings mSettings = null;
    private String APP_VERSION = "";
    private String API_READ_KEY = "";

    public String Login = "";
    public String NickName = "";
    public String RegDate = "";
    public Integer Level = -1;

    UserManager(Context context) {
        APP_VERSION = context.getResources().getString(R.string.app_version);
        mSettings = new Settings(context);
        API_READ_KEY = mSettings.AppSettings.getString(Settings.API_READ_KEY, "");
        Login = mSettings.AppSettings.getString(Settings.APP_SERVER_LOGIN, "");
    }

    public void getFromSettings()
    {
        NickName = mSettings.AppSettings.getString(Settings.USER_NICK, "");
        RegDate = mSettings.AppSettings.getString(Settings.USER_REGDATE, "");
        Level = mSettings.AppSettings.getInt(Settings.USER_GROUP, -1);
    }

    public String GetGroup(Context context)
    {
        return GetTextGroup(Level,context);
    }

    public String GetTextGroup(Integer Level, Context context)
    {
        switch (Level)
        {
            case -2: return context.getString(R.string.access_level_banned);
            case -1: return context.getString(R.string.access_level_no_logged);
            case 0: return context.getString(R.string.access_level_guest);
            case 1: return context.getString(R.string.access_level_user);
            case 2: return context.getString(R.string.access_level_developer);
            case 3: return context.getString(R.string.access_level_admin);
        }
        return "";
    }

    public String GetErrorDesc(String error, Context context)
    {
        if (error.equals("database"))
            return context.getString(R.string.error_database_maintenance);
        if (error.equals("loginfail"))
            return context.getString(R.string.error_incorrect_credentials);
        if (error.equals("form"))
            return context.getString(R.string.error_form_fields);
        if (error.equals("cooldown"))
            return context.getString(R.string.error_cooldown);
        return String.format(context.getString(R.string.unknown_error), error);
    }

}
