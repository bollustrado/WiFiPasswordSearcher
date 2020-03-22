package com.example.WiFiPasswordSearcher;

import android.content.Context;

/**
 * Created by пк on 20.12.2015.
 */
class UserManager {
    private Settings mSettings;

    String NickName = "";
    String RegDate = "";
    private Integer Level = -1;

    UserManager(Context context) {
        String APP_VERSION = context.getResources().getString(R.string.app_version);
        mSettings = new Settings(context);
        String API_READ_KEY = mSettings.AppSettings.getString(Settings.API_READ_KEY, "");
        String login = mSettings.AppSettings.getString(Settings.APP_SERVER_LOGIN, "");
    }

    void getFromSettings()
    {
        NickName = mSettings.AppSettings.getString(Settings.USER_NICK, "");
        RegDate = mSettings.AppSettings.getString(Settings.USER_REGDATE, "");
        Level = mSettings.AppSettings.getInt(Settings.USER_GROUP, -1);
    }

    String GetGroup(Context context)
    {
        return GetTextGroup(Level,context);
    }

    private String GetTextGroup(Integer Level, Context context)
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

    String GetErrorDesc(String error, Context context)
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
