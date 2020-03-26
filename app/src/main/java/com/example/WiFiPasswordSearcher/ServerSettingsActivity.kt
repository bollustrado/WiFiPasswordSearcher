package com.example.WiFiPasswordSearcher

import android.app.Activity
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Switch

class ServerSettingsActivity : Activity() {
    private var txtServerLogin: EditText? = null
    private var txtServerPassword: EditText? = null
    private var txtServerUri: EditText? = null
    private var swFetchESS: Switch? = null
    private var swCheckUpd: Switch? = null
    private var btnCancel: Button? = null
    private var btnSave: Button? = null
    private var mSettings: Settings? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.server_settings)
        txtServerLogin = findViewById(R.id.txt_settings_3wifi_login) as EditText
        txtServerPassword = findViewById(R.id.txt_settings_3wifi_password) as EditText
        txtServerUri = findViewById(R.id.txt_settings_3wifi_server) as EditText
        swFetchESS = findViewById(R.id.sw_fetch_ess) as Switch
        swCheckUpd = findViewById(R.id.sw_check_upd) as Switch
        btnCancel = findViewById(R.id.btn_settitgs_3wifi_cancel) as Button
        btnSave = findViewById(R.id.btn_settitgs_3wifi_save) as Button
        mSettings = Settings(applicationContext)
        mSettings!!.Reload()
        val SERVER_LOGIN = mSettings!!.AppSettings!!.getString(Settings.APP_SERVER_LOGIN, "")
        val SERVER_PASSWORD = mSettings!!.AppSettings!!.getString(Settings.APP_SERVER_PASSWORD, "")
        val SERVER_URI = mSettings!!.AppSettings!!.getString(Settings.APP_SERVER_URI, resources.getString(R.string.SERVER_URI_DEFAULT))
        val FETCH_ESS = mSettings!!.AppSettings!!.getBoolean(Settings.APP_FETCH_ESS, false)
        val CHECK_UPDATES = mSettings!!.AppSettings!!.getBoolean(Settings.APP_CHECK_UPDATES, true)
        txtServerLogin!!.setText(SERVER_LOGIN)
        txtServerPassword!!.setText(SERVER_PASSWORD)
        txtServerUri!!.setText(SERVER_URI)
        swFetchESS!!.isChecked = FETCH_ESS
        swCheckUpd!!.isChecked = CHECK_UPDATES
        btnCancel!!.setOnClickListener(btnCloseOnClick)
        btnSave!!.setOnClickListener(btnSaveOnClick)
    }

    private val btnCloseOnClick = View.OnClickListener { finish() }
    private val btnSaveOnClick = View.OnClickListener {
        val Login = txtServerLogin!!.text.toString()
        val Password = txtServerPassword!!.text.toString()
        val Uri = txtServerUri!!.text.toString()
        val FetchESS = swFetchESS!!.isChecked
        val CheckUpdates = swCheckUpd!!.isChecked
        // Save
        mSettings!!.Editor!!.putString(Settings.APP_SERVER_LOGIN, Login)
        mSettings!!.Editor!!.putString(Settings.APP_SERVER_PASSWORD, Password)
        mSettings!!.Editor!!.putString(Settings.APP_SERVER_URI, Uri)
        mSettings!!.Editor!!.putBoolean(Settings.APP_FETCH_ESS, FetchESS)
        mSettings!!.Editor!!.putBoolean(Settings.APP_CHECK_UPDATES, CheckUpdates)
        mSettings!!.Editor!!.commit()
        finish()
    }

    fun cbUnmaskClick(view: View) {
        val eType = txtServerPassword!!.inputType
        if ((view as CheckBox).isChecked) {
            txtServerPassword!!.inputType = eType and InputType.TYPE_TEXT_VARIATION_PASSWORD.inv()
        } else {
            txtServerPassword!!.inputType = eType or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
    }
}