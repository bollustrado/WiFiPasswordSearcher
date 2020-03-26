package com.example.WiFiPasswordSearcher

import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import com.example.WiFiPasswordSearcher.StartActivity
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Created by пк on 20.12.2015.
 */
class StartActivity : Activity() {
    private var mSettings: Settings? = null
    private var User: UserManager? = null
    var edtLogin: EditText? = null
    var edtPassword: EditText? = null
    var btnGetKeys: Button? = null
    var btnStart: Button? = null
    var btnUserInfo: Button? = null
    var llMenu: LinearLayout? = null
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.start)
        onConfigurationChanged(resources.configuration)
        mSettings = Settings(applicationContext)
        User = UserManager(applicationContext)
        edtLogin = findViewById(R.id.edtLogin) as EditText
        edtPassword = findViewById(R.id.edtPassword) as EditText
        llMenu = findViewById(R.id.llStartMenu) as LinearLayout
        btnGetKeys = findViewById(R.id.btnGetApiKeys) as Button
        btnStart = findViewById(R.id.btnStart) as Button
        btnUserInfo = findViewById(R.id.btnUserInfo) as Button
        val API_KEYS_VALID = mSettings!!.AppSettings!!.getBoolean(Settings.API_KEYS_VALID, false)
        val SavedLogin = mSettings!!.AppSettings!!.getString(Settings.APP_SERVER_LOGIN, "")
        val SavedPassword = mSettings!!.AppSettings!!.getString(Settings.APP_SERVER_PASSWORD, "")
        val CHECK_UPDATES = mSettings!!.AppSettings!!.getBoolean(Settings.APP_CHECK_UPDATES, true)
        if (API_KEYS_VALID) {
            btnGetKeys!!.visibility = View.GONE
            edtLogin!!.isEnabled = false
            edtPassword!!.isEnabled = false
            llMenu!!.visibility = View.VISIBLE
            if (CHECK_UPDATES && !VersionAlreadyChecked) {
                Thread(Runnable {
                    val Version = AppVersion(applicationContext)
                    val Result = Version.isActualyVersion(applicationContext, false)
                    if (!Result) {
                        runOnUiThread { Version.ShowUpdateDialog(this@StartActivity) }
                    }
                    VersionAlreadyChecked = true
                }).start()
            }
        }
        edtLogin!!.setText(SavedLogin)
        edtPassword!!.setText(SavedPassword)
        btnGetKeys!!.setOnClickListener {
            val dProccess = ProgressDialog(this@StartActivity)
            dProccess.setProgressStyle(ProgressDialog.STYLE_SPINNER)
            dProccess.setMessage(resources.getString(R.string.status_signing_in))
            dProccess.setCanceledOnTouchOutside(false)
            dProccess.show()
            Thread(Runnable {
                var res = false
                try {
                    res = getApiKeys(edtLogin!!.text.toString(), edtPassword!!.text.toString())
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                if (res) {
                    User!!.fromSettings
                    runOnUiThread {
                        btnGetKeys!!.visibility = View.GONE
                        llMenu!!.visibility = View.VISIBLE
                        edtLogin!!.isEnabled = false
                        edtPassword!!.isEnabled = false
                    }
                }
                dProccess.dismiss()
            }).start()
        }
        btnUserInfo!!.setOnClickListener {
            val userActivity = Intent(this@StartActivity, UserInfoActivity::class.java)
            userActivity.putExtra("showInfo", "user")
            startActivity(userActivity)
        }
        btnStart!!.setOnClickListener {
            val mainActivity = Intent(this@StartActivity, MyActivity::class.java)
            startActivity(mainActivity)
            finish()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val LR = findViewById(R.id.rootLayout) as LinearLayout
        val LP = findViewById(R.id.layoutPadding) as LinearLayout
        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            LR.orientation = LinearLayout.VERTICAL
            LP.visibility = View.VISIBLE
        } else if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            LR.orientation = LinearLayout.HORIZONTAL
            LP.visibility = View.GONE
        }
    }

    @Throws(IOException::class)
    private fun getApiKeys(Login: String, Password: String): Boolean {
        val Args = "/api/apikeys"
        val Reader: BufferedReader
        var ReadLine: String?
        val RawData = StringBuilder()
        try {
            mSettings!!.Reload()
            val SERVER_URI = mSettings!!.AppSettings!!.getString(Settings.APP_SERVER_URI, resources.getString(R.string.SERVER_URI_DEFAULT))!!
            val Uri = URL(SERVER_URI + Args)
            val Connection = Uri.openConnection() as HttpURLConnection
            Connection.requestMethod = "POST"
            Connection.doOutput = true
            Connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            val writer = DataOutputStream(
                    Connection.outputStream)
            writer.writeBytes(
                    "login=" + URLEncoder.encode(Login, "UTF-8") +
                            "&password=" + URLEncoder.encode(Password, "UTF-8") +
                            "&genread=1")
            Connection.readTimeout = 10 * 1000
            Connection.connect()
            Reader = BufferedReader(InputStreamReader(Connection.inputStream))
            while (Reader.readLine().also { ReadLine = it } != null) {
                RawData.append(ReadLine)
            }
            try {
                var ReadApiKey: String? = null
                var WriteApiKey: String? = null
                val Json = JSONObject(RawData.toString())
                val Successes = Json.getBoolean("result")
                return if (Successes) {
                    val profile = Json.getJSONObject("profile")
                    val keys = Json.getJSONArray("data")
                    for (i in 0 until keys.length()) {
                        val keyData = keys.getJSONObject(i)
                        val access = keyData.getString("access")
                        if (access == "read") {
                            ReadApiKey = keyData.getString("key")
                        } else if (access == "write") {
                            WriteApiKey = keyData.getString("key")
                        }
                        if (ReadApiKey != null && WriteApiKey != null) break
                    }
                    if (ReadApiKey == null) {
                        runOnUiThread {
                            val t = Toast.makeText(applicationContext, resources.getString(R.string.toast_no_api_keys), Toast.LENGTH_SHORT)
                            t.show()
                        }
                        return false
                    }
                    mSettings!!.Editor!!.putString(Settings.APP_SERVER_LOGIN, Login)
                    mSettings!!.Editor!!.putString(Settings.APP_SERVER_PASSWORD, Password)
                    mSettings!!.Editor!!.putString(Settings.API_READ_KEY, ReadApiKey)
                    mSettings!!.Editor!!.putString(Settings.API_WRITE_KEY, WriteApiKey)
                    mSettings!!.Editor!!.putBoolean(Settings.API_KEYS_VALID, true)
                    mSettings!!.Editor!!.putString(Settings.USER_NICK, profile.getString("nick"))
                    mSettings!!.Editor!!.putString(Settings.USER_REGDATE, profile.getString("regdate"))
                    mSettings!!.Editor!!.putInt(Settings.USER_GROUP, profile.getInt("level"))
                    mSettings!!.Editor!!.commit()
                    true
                } else {
                    val error = Json.getString("error")
                    val errorDesc = User!!.getErrorDesc(error, this)
                    runOnUiThread {
                        val t = Toast.makeText(applicationContext, errorDesc, Toast.LENGTH_SHORT)
                        t.show()
                    }
                    false
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        runOnUiThread {
            val dialogClickListener = DialogInterface.OnClickListener { dialog, which ->
                when (which) {
                    DialogInterface.BUTTON_POSITIVE -> btnOffline(null)
                    DialogInterface.BUTTON_NEGATIVE -> {
                    }
                }
                dialog.dismiss()
            }
            val builder = AlertDialog.Builder(this@StartActivity)
            builder.setTitle(resources.getString(R.string.status_no_internet))
                    .setMessage(resources.getString(R.string.dialog_work_offline))
                    .setPositiveButton(resources.getString(R.string.dialog_yes), dialogClickListener)
                    .setNegativeButton(resources.getString(R.string.dialog_no), dialogClickListener).show()
        }
        return false
    }

    fun btnOffline(view: View?) {
        mSettings!!.Editor!!.putString(Settings.API_READ_KEY, "offline")
        mSettings!!.Editor!!.commit()
        val offlineActivityIntent = Intent(this@StartActivity, MyActivity::class.java)
        startActivity(offlineActivityIntent)
    }

    companion object {
        private var VersionAlreadyChecked = false
    }
}