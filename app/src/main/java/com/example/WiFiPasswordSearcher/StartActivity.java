package com.example.WiFiPasswordSearcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

/**
 * Created by пк on 20.12.2015.
 */
public class StartActivity extends Activity {

    private Settings mSettings;
    private static Boolean VersionAlreadyChecked = false;
    private UserManager User;

    public EditText edtLogin = null;
    public EditText edtPassword = null;
    public Button btnGetKeys = null;
    public Button btnStart = null;
    public Button btnUserInfo = null;
    public LinearLayout llMenu = null;

    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.start);
        this.onConfigurationChanged(getResources().getConfiguration());

        mSettings = new Settings(getApplicationContext());
        User = new UserManager(getApplicationContext());

        edtLogin = (EditText)findViewById(R.id.edtLogin);
        edtPassword = (EditText)findViewById(R.id.edtPassword);

        llMenu = (LinearLayout)findViewById(R.id.llStartMenu);
        btnGetKeys = (Button)findViewById(R.id.btnGetApiKeys);
        btnStart = (Button)findViewById(R.id.btnStart);
        btnUserInfo = (Button)findViewById(R.id.btnUserInfo);

        Boolean API_KEYS_VALID = mSettings.AppSettings.getBoolean(Settings.API_KEYS_VALID, false);
        String SavedLogin = mSettings.AppSettings.getString(Settings.APP_SERVER_LOGIN, "");
        String SavedPassword = mSettings.AppSettings.getString(Settings.APP_SERVER_PASSWORD, "");
        Boolean CHECK_UPDATES = mSettings.AppSettings.getBoolean(Settings.APP_CHECK_UPDATES, true);

        if (API_KEYS_VALID)
        {
            btnGetKeys.setVisibility(View.GONE);
            edtLogin.setEnabled(false);
            edtPassword.setEnabled(false);
            llMenu.setVisibility(View.VISIBLE);

            if (CHECK_UPDATES && !VersionAlreadyChecked)
            {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        final AppVersion Version = new AppVersion(getApplicationContext());
                        Boolean Result = Version.isActualyVersion(getApplicationContext(), false);
                        if (!Result)
                        {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run()
                                {
                                    Version.ShowUpdateDialog(StartActivity.this);
                                }
                            });
                        }
                        VersionAlreadyChecked = true;
                    }
                }).start();
            }
        }

        edtLogin.setText(SavedLogin);
        edtPassword.setText(SavedPassword);

        btnGetKeys.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final ProgressDialog dProccess = new ProgressDialog(StartActivity.this);
                dProccess.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                dProccess.setMessage(getResources().getString(R.string.status_signing_in));
                dProccess.setCanceledOnTouchOutside(false);
                dProccess.show();

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Boolean res = false;

                        try {
                            res = getApiKeys(edtLogin.getText().toString(), edtPassword.getText().toString());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        if (res)
                        {
                            User.getFromSettings();

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    btnGetKeys.setVisibility(View.GONE);
                                    llMenu.setVisibility(View.VISIBLE);
                                    edtLogin.setEnabled(false);
                                    edtPassword.setEnabled(false);
                                }
                            });
                        }
                        dProccess.dismiss();
                    }
                }).start();
            }
        });

        btnUserInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent userActivity = new Intent(StartActivity.this, UserInfoActivity.class);
                userActivity.putExtra("showInfo", "user");
                startActivity(userActivity);
            }
        });
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent mainActivity = new Intent(StartActivity.this, MyActivity.class);
                startActivity(mainActivity);
                finish();
            }
        });
    }

    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);

        LinearLayout LR = (LinearLayout)findViewById(R.id.rootLayout);
        LinearLayout LP = (LinearLayout)findViewById(R.id.layoutPadding);

        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT)
        {
            LR.setOrientation(LinearLayout.VERTICAL);
            LP.setVisibility(View.VISIBLE);
        }
        else if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE)
        {
            LR.setOrientation(LinearLayout.HORIZONTAL);
            LP.setVisibility(View.GONE);
        }
    }

    private boolean getApiKeys(String Login, String Password) throws IOException
    {
        String Args = "/api/apikeys";
        BufferedReader Reader;
        String ReadLine;
        String RawData = "";

        try {
            mSettings.Reload();
            String SERVER_URI = mSettings.AppSettings.getString(Settings.APP_SERVER_URI, getResources().getString(R.string.SERVER_URI_DEFAULT));
            URL Uri = new URL(SERVER_URI + Args);

            HttpURLConnection Connection = (HttpURLConnection) Uri.openConnection();
            Connection.setRequestMethod("POST");
            Connection.setDoOutput(true);
            Connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            DataOutputStream  writer = new DataOutputStream (
                    Connection.getOutputStream());
            writer.writeBytes(
                "login=" + URLEncoder.encode(Login, "UTF-8") +
                "&password=" + URLEncoder.encode(Password, "UTF-8") +
                "&genread=1");

            Connection.setReadTimeout(10 * 1000);
            Connection.connect();

            Reader = new BufferedReader(new InputStreamReader(Connection.getInputStream()));

            while ((ReadLine = Reader.readLine()) != null) {
                RawData += ReadLine;
            }

            try
            {
                String ReadApiKey = null, WriteApiKey = null;
                JSONObject Json = new JSONObject(RawData);
                Boolean Successes = Json.getBoolean("result");
                if (Successes)
                {
                    JSONObject profile = Json.getJSONObject("profile");

                    JSONArray keys = Json.getJSONArray("data");
                    for (int i = 0; i < keys.length(); i++)
                    {
                        JSONObject keyData = keys.getJSONObject(i);
                        String access = keyData.getString("access");

                        if (access.equals("read"))
                        {
                            ReadApiKey = keyData.getString("key");
                        }
                        else if (access.equals("write"))
                        {
                            WriteApiKey = keyData.getString("key");
                        }
                        if (ReadApiKey != null && WriteApiKey != null)
                            break;
                    }

                    if (ReadApiKey == null)
                    {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast t = Toast.makeText(getApplicationContext(), getResources().getString(R.string.toast_no_api_keys), Toast.LENGTH_SHORT);
                                t.show();
                            }
                        });
                        return false;
                    }

                    mSettings.Editor.putString(Settings.APP_SERVER_LOGIN, Login);
                    mSettings.Editor.putString(Settings.APP_SERVER_PASSWORD, Password);
                    mSettings.Editor.putString(Settings.API_READ_KEY, ReadApiKey);
                    mSettings.Editor.putString(Settings.API_WRITE_KEY, WriteApiKey);
                    mSettings.Editor.putBoolean(Settings.API_KEYS_VALID, true);
                    mSettings.Editor.putString(Settings.USER_NICK, profile.getString("nick"));
                    mSettings.Editor.putString(Settings.USER_REGDATE, profile.getString("regdate"));
                    mSettings.Editor.putInt(Settings.USER_GROUP, profile.getInt("level"));
                    mSettings.Editor.commit();

                    return true;
                }
                else
                {
                    String error = Json.getString("error");
                    final String errorDesc = User.GetErrorDesc(error, this);

                    if (error != null) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast t = Toast.makeText(getApplicationContext(), errorDesc, Toast.LENGTH_SHORT);
                                t.show();
                            }
                        });
                    }
                    return false;
                }
            }
            catch (JSONException e)
            {
                e.printStackTrace();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        switch (which)
                        {
                            case DialogInterface.BUTTON_POSITIVE:
                                btnOffline(null);
                                break;
                            case DialogInterface.BUTTON_NEGATIVE:
                                break;
                        }
                        dialog.dismiss();
                    }
                };
                AlertDialog.Builder builder = new AlertDialog.Builder(StartActivity.this);
                builder.setTitle(getResources().getString(R.string.status_no_internet))
                    .setMessage(getResources().getString(R.string.dialog_work_offline))
                    .setPositiveButton(getResources().getString(R.string.dialog_yes), dialogClickListener)
                    .setNegativeButton(getResources().getString(R.string.dialog_no), dialogClickListener).show();
            }
        });
        return false;
    }

    public void btnOffline(View view)
    {
        mSettings.Editor.putString(Settings.API_READ_KEY, "offline");
        mSettings.Editor.commit();
        Intent offlineActivityIntent = new Intent(StartActivity.this, MyActivity.class);
        startActivity(offlineActivityIntent);
    }
}
