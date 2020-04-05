package com.example.WiFiPasswordSearcher

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.*
import android.widget.AdapterView.OnItemClickListener

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
                val AboutInfoIntent = Intent(this@SettingsActivity, AboutActivity::class.java)
                startActivity(AboutInfoIntent)
            }
            3 -> {
                val Version = AppVersion(applicationContext)
                if (!Version.isActualyVersion(applicationContext, true)) {
                    Version.ShowUpdateDialog(this@SettingsActivity)
                }
            }
            4 -> {
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