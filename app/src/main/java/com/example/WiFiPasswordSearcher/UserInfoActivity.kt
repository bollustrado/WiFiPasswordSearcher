package com.example.WiFiPasswordSearcher

import android.annotation.TargetApi
import android.app.Activity
import android.app.ProgressDialog
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import org.apache.http.client.ResponseHandler
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.BasicResponseHandler
import org.apache.http.impl.client.DefaultHttpClient
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by пк on 20.12.2015.
 */
class UserInfoActivity : Activity() {
    var txtLogin: TextView? = null
    var txtRegDate: TextView? = null
    var txtGroup: TextView? = null
    private var info: String? = null
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.user)
        val mSettings = Settings(applicationContext)
        info = try {
            Objects.requireNonNull(intent.extras).getString("showInfo")
        } catch (e: Exception) {
            "user"
        }
        txtLogin = findViewById(R.id.txtLogin) as TextView
        txtRegDate = findViewById(R.id.txtRegDate) as TextView
        txtGroup = findViewById(R.id.txtGroup) as TextView
        val Nick: String
        val Group: String
        val date: Date
        if (info != null && info == "wpspin") {
            val updater = AppVersion(applicationContext)
            updater.wpsCompanionInit(false)
            val lButtons = findViewById(R.id.buttonsLayout) as LinearLayout
            lButtons.visibility = LinearLayout.VISIBLE
            val btnRevert = findViewById(R.id.btnRevert) as Button
            btnRevert.isEnabled = !updater.wpsCompanionInternal()
            val lReg = findViewById(R.id.labRegDate) as TextView
            val lGroup = findViewById(R.id.labGroup) as TextView
            Nick = "WPS PIN Companion"
            lReg.text = getString(R.string.label_last_updated)
            date = updater.wpsCompanionGetDate()
            lGroup.text = getString(R.string.label_file_size)
            val size = updater.wpsCompanionGetSize()
            Group = updater.readableFileSize(size)
        } else {
            val User = UserManager(applicationContext)
            User.fromSettings
            val lButtons = findViewById(R.id.buttonsLayout) as LinearLayout
            lButtons.visibility = LinearLayout.GONE
            val format = SimpleDateFormat(resources.getString(R.string.DEFAULT_DATE_FORMAT), Locale.US)
            date = try {
                format.parse(User.RegDate)
            } catch (e: Exception) {
                Date()
            }
            Nick = User.NickName
            Group = User.getGroup(applicationContext)
        }
        txtLogin!!.text = Nick
        txtRegDate!!.text = DateFormat.getDateTimeInstance().format(date)
        txtGroup!!.text = Group
    }

    private inner class AsyncWpsUpdater : AsyncTask<String?, Void?, String>() {
        private var pd: ProgressDialog? = null
        override fun onPreExecute() {
            super.onPreExecute()
            pd = ProgressDialog(this@UserInfoActivity)
            pd!!.setProgressStyle(ProgressDialog.STYLE_SPINNER)
            pd!!.setMessage(getString(R.string.status_updating))
            pd!!.setCanceledOnTouchOutside(false)
            pd!!.show()
        }

        override fun doInBackground(input: Array<String?>): String {
            val hc = DefaultHttpClient()
            val res: ResponseHandler<String> = BasicResponseHandler()
            val mSettings = Settings(applicationContext)
            mSettings.Reload()
            val SERVER_URI = mSettings.AppSettings!!.getString(Settings.APP_SERVER_URI, resources.getString(R.string.SERVER_URI_DEFAULT))
            val http = HttpGet("$SERVER_URI/wpspin")
            var str = ""
            try {
                str = hc.execute(http, res)
            } catch (ignored: Exception) {
            }
            return str
        }

        override fun onPostExecute(str: String) {
            val msg: String
            pd!!.dismiss()
            if (str.contains("initAlgos();")) {
                val updater = AppVersion(applicationContext)
                updater.wpsCompanionUpdate(str, Date())
                txtRegDate!!.text = DateFormat.getDateTimeInstance().format(updater.wpsCompanionGetDate())
                txtGroup!!.text = updater.readableFileSize(updater.wpsCompanionGetSize())
                val btnRevert = findViewById(R.id.btnRevert) as Button
                btnRevert.isEnabled = !updater.wpsCompanionInternal()
                msg = getString(R.string.toast_updated_successful)
            } else if (str.length == 0) {
                msg = getString(R.string.status_no_internet)
            } else {
                msg = getString(R.string.toast_update_failed)
            }
            val toast = Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT)
            toast.show()
        }
    }

    fun btnUpdateOnClick(v: View?) {
        if (info != null && info == "wpspin") {
            AsyncWpsUpdater().execute()
        }
    }

    fun btnRevertOnClick(v: View) {
        if (info != null && info == "wpspin") {
            val updater = AppVersion(applicationContext)
            updater.wpsCompanionInit(true)
            txtRegDate!!.text = DateFormat.getDateTimeInstance().format(updater.wpsCompanionGetDate())
            txtGroup!!.text = updater.readableFileSize(updater.wpsCompanionGetSize())
            v.isEnabled = !updater.wpsCompanionInternal()
            val toast = Toast.makeText(applicationContext,
                    getString(R.string.toast_reverted_to_init_state), Toast.LENGTH_SHORT)
            toast.show()
        }
    }
}