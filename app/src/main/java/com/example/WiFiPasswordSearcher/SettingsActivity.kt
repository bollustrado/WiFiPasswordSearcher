package com.example.WiFiPasswordSearcher

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import java.util.*

class SettingsActivity : Activity() {
    /**
     * Called when the activity is first created.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings)
        val GeneralListView = findViewById(R.id.SettingsListView) as ListView
        val strSettingsRows = resources.getStringArray(R.array.strings_settings_rows)
        val adapterSettingsListView = ArrayAdapter(this, android.R.layout.simple_list_item_1, strSettingsRows)
        GeneralListView.adapter = adapterSettingsListView
        GeneralListView.onItemClickListener = GeneralListOnClick
    }

    private val GeneralListOnClick = OnItemClickListener { parent, Item, position, id ->
        val txtItem = Item as TextView
        when (id.toInt()) {
            0 -> {
                val ServerSettingsIntent = Intent(this@SettingsActivity, ServerSettingsActivity::class.java)
                startActivity(ServerSettingsIntent)
            }
            1 -> {
                val userActivity = Intent(this@SettingsActivity, UserInfoActivity::class.java)
                userActivity.putExtra("showInfo", "wpspin")
                startActivity(userActivity)
            }
            2 -> {
                val lay = LinearLayout(this@SettingsActivity)
                lay.orientation = LinearLayout.VERTICAL
                val ebss = EditText(this@SettingsActivity)
                ebss.hint = getString(R.string.hint_enter_bssid)
                ebss.inputType = InputType.TYPE_CLASS_TEXT
                lay.addView(ebss)
                val eess = EditText(this@SettingsActivity)
                eess.hint = getString(R.string.hint_enter_essid)
                eess.inputType = InputType.TYPE_CLASS_TEXT
                lay.addView(eess)
                val alert = AlertDialog.Builder(this@SettingsActivity)
                alert.setTitle(getString(R.string.dialog_network_properties))
                alert.setView(lay)
                alert.setPositiveButton(getString(R.string.ok)) { dialog, which ->
                    val detailsActivityIntent = Intent(this@SettingsActivity, WifiDetails::class.java)
                    val WifiInfo = HashMap<String, String>()
                    WifiInfo["BSSID"] = ebss.text.toString().toLowerCase()
                    WifiInfo["SSID"] = eess.text.toString()
                    WifiInfo["Freq"] = "0"
                    WifiInfo["Signal"] = "-100"
                    finish()
                    detailsActivityIntent.putExtra("WifiInfo", WifiInfo)
                    startActivity(detailsActivityIntent)
                }
                alert.setNegativeButton(getString(R.string.cancel)) { dialog, which -> dialog.cancel() }
                alert.show()
            }
            3 -> {
                val AboutInfoIntent = Intent(this@SettingsActivity, AboutActivity::class.java)
                startActivity(AboutInfoIntent)
            }
            4 -> {
                val Version = AppVersion(applicationContext)
                if (!Version.isActualyVersion(applicationContext, true)) {
                    Version.ShowUpdateDialog(this@SettingsActivity)
                }
            }
            5 -> {
                val mSettings = Settings(applicationContext)
                mSettings.Reload()
                mSettings.Editor!!.remove(Settings.APP_SERVER_LOGIN)
                mSettings.Editor!!.remove(Settings.APP_SERVER_PASSWORD)
                mSettings.Editor!!.remove(Settings.API_READ_KEY)
                mSettings.Editor!!.remove(Settings.API_WRITE_KEY)
                mSettings.Editor!!.remove(Settings.API_KEYS_VALID)
                mSettings.Editor!!.commit()
                val StartPage = Intent(applicationContext, StartActivity::class.java)
                StartPage.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(StartPage)
            }
        }
    }
}