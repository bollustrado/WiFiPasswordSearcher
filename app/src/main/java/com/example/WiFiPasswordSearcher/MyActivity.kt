package com.example.WiFiPasswordSearcher

import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.*
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.location.LocationManager
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import com.example.WiFiPasswordSearcher.MyActivity
import com.example.WiFiPasswordSearcher.StartActivity
import com.example.WiFiPasswordSearcher.WPSActivity
import com.larvalabs.svgandroid.SVG
import com.larvalabs.svgandroid.SVGParser
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

class APData {
    var BSSID: String? = null
    var Keys: ArrayList<String>? = null
    var Generated: ArrayList<Boolean>? = null
    var WPS: ArrayList<String>? = null
}

internal class MyScanResult {
    var BSSID: String? = null
    var SSID: String? = null
    var frequency = 0
    var level = 0
    var capabilities: String? = null
}

internal class WiFiListSimpleAdapter(private val context: Context, data: List<MutableMap<String, *>?>, resource: Int, from: Array<String>?, to: IntArray?) : SimpleAdapter(context, data, resource, from, to) {
    private val DataList: List<*>
    private fun DeleteInTextTags(text: String): String {
        var text = text
        if (text.length > 2 && text.substring(0, 2) == "*[") {
            val stylePref = text.substring(2, text.indexOf("]*"))
            text = text.substring(stylePref.length + 4)
        }
        return text
    }

    private fun ParseInTextTags(txtView: TextView) {
        val text = "" + txtView.text
        if (text.length > 2 && text.substring(0, 2) == "*[") {
            val stylePref = text.substring(2, text.indexOf("]*"))
            txtView.text = text.substring(stylePref.length + 4)
            if (stylePref.indexOf(":") > 0) {
                val style = stylePref.substring(0, stylePref.indexOf(":"))
                val value = stylePref.substring(stylePref.indexOf(":") + 1)
                if (style == "color") {
                    when (value) {
                        "red" -> txtView.setTextColor(Color.rgb(153, 0, 0))
                        "green" -> txtView.setTextColor(Color.GREEN)
                        "greendark" -> txtView.setTextColor(Color.rgb(0, 153, 76))
                        "blue" -> txtView.setTextColor(Color.BLUE)
                        "yellow" -> txtView.setTextColor(Color.rgb(153, 153, 0))
                        "gray" -> txtView.setTextColor(Color.rgb(105, 105, 105))
                    }
                }
            }
        } else {
            txtView.setTextColor(Color.WHITE)
        }
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent)
        val imgSec = view.findViewById<View>(R.id.imgSec) as ImageView
        val imgWPS = view.findViewById<View>(R.id.imgWps) as ImageView
        val ElemWiFi: HashMap<String, String>
        ElemWiFi = DataList[position] as HashMap<String, String>
        val Capability = ElemWiFi["CAPABILITY"]
        imgSec.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        imgWPS.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        var svgImg: SVG
        if (Capability!!.contains("WPA2")) {
            var img = SvgImageCache["WPA2"]
            if (img == null) {
                svgImg = SVGParser.getSVGFromResource(context.resources, R.raw.wpa2_ico)
                img = svgImg.createPictureDrawable()
                SvgImageCache["WPA2"] = img
            }
            imgSec.setImageDrawable(img)
        } else if (Capability.contains("WPA")) {
            var img = SvgImageCache["WPA"]
            if (img == null) {
                svgImg = SVGParser.getSVGFromResource(context.resources, R.raw.wpa_ico)
                img = svgImg.createPictureDrawable()
                SvgImageCache["WPA"] = img
            }
            imgSec.setImageDrawable(img)
        } else if (Capability.contains("WEP")) {
            var img = SvgImageCache["WEP"]
            if (img == null) {
                svgImg = SVGParser.getSVGFromResource(context.resources, R.raw.wep_ico)
                img = svgImg.createPictureDrawable()
                SvgImageCache["WEP"] = img
            }
            imgSec.setImageDrawable(img)
        } else {
            var img = SvgImageCache["OPEN"]
            if (img == null) {
                svgImg = SVGParser.getSVGFromResource(context.resources, R.raw.open_ico)
                img = svgImg.createPictureDrawable()
                SvgImageCache["OPEN"] = img
            }
            imgSec.setImageDrawable(img)
        }
        if (Capability.contains("WPS")) {
            var img = SvgImageCache["WPS"]
            if (img == null) {
                svgImg = SVGParser.getSVGFromResource(context.resources, R.raw.wps_ico)
                img = svgImg.createPictureDrawable()
                SvgImageCache["WPS"] = img
            }
            imgWPS.setImageDrawable(img)
        } else {
            imgWPS.setImageResource(android.R.color.transparent)
        }
        val txtKey = view.findViewById<View>(R.id.KEY) as TextView
        val txtSignal = view.findViewById<View>(R.id.txtSignal) as TextView
        val txtRowId = view.findViewById<View>(R.id.txtRowId) as TextView
        val txtKeysCount = view.findViewById<View>(R.id.txtKeysCount) as TextView
        val txtWPS = view.findViewById<View>(R.id.txtWPS) as TextView
        val llKeys = view.findViewById<View>(R.id.llKeys) as LinearLayout
        llKeys.setOnClickListener(onKeyClick)
        ParseInTextTags(txtKey)
        ParseInTextTags(txtSignal)
        ParseInTextTags(txtKeysCount)
        ParseInTextTags(txtWPS)
        val keysCount = txtKeysCount.text.toString().toInt()
        llKeys.isClickable = keysCount > 1
        txtRowId.text = Integer.toString(position)
        return view
    }

    private val onKeyClick = View.OnClickListener { v ->
        if (MyActivity.WiFiKeys == null || MyActivity.WiFiKeys!!.size == 0) return@OnClickListener
        val llRow = v.parent.parent as LinearLayout
        val txtRowId = llRow.findViewById<View>(R.id.txtRowId) as TextView
        val rowId = txtRowId.text.toString().toInt()
        val keys = MyActivity.WiFiKeys!![rowId].Keys
        val wpss = MyActivity.WiFiKeys!![rowId].WPS
        if (keys!!.size <= 1) return@OnClickListener
        val keysList = arrayOfNulls<String>(keys.size)
        for (i in keysList.indices) {
            val WPS = wpss!![i]
            if (WPS.isEmpty()) {
                keysList[i] = context.getString(R.string.dialog_choose_key_key) + DeleteInTextTags(keys[i])
            } else {
                keysList[i] = context.getString(R.string.dialog_choose_key_key) + DeleteInTextTags(keys[i]) + context.getString(R.string.dialog_choose_key_wps) + DeleteInTextTags(WPS)
            }
        }
        val dialogBuilder = AlertDialog.Builder(context)
        dialogBuilder.setTitle(context.getString(R.string.dialog_choose_key))
        dialogBuilder.setItems(keysList) { dialog, item -> passwordChoose(rowId, item) }
        dialogBuilder.show()
    }

    private fun passwordChoose(rowID: Int, passId: Int) {
        var row: View? = null
        for (i in 0 until MyActivity.WiFiList!!.childCount) {
            row = MyActivity.WiFiList!!.getChildAt(i)
            val txtRowId = row.findViewById<View>(R.id.txtRowId) as TextView
            val rid = txtRowId.text.toString().toInt()
            row = if (rid != rowID) null else break
        }
        if (row == null) return
        val keys = MyActivity.WiFiKeys!![rowID].Keys
        val gen = MyActivity.WiFiKeys!![rowID].Generated
        val wps = MyActivity.WiFiKeys!![rowID].WPS
        val choosedPassword = keys!![passId]
        val isGen = gen!![passId]
        val curWPS = wps!![passId]
        val KeyColor: String
        keys[passId] = keys[0]
        keys[0] = choosedPassword
        gen[passId] = gen[0]
        gen[0] = isGen
        wps[passId] = wps[0]
        wps[0] = curWPS
        val txtKey = row.findViewById<View>(R.id.KEY) as TextView
        KeyColor = if (isGen) "*[color:red]*" else "*[color:green]*"
        txtKey.text = KeyColor + choosedPassword
        ParseInTextTags(txtKey)
        val txtWPS = row.findViewById<View>(R.id.txtWPS) as TextView
        txtWPS.text = if (curWPS.isEmpty()) "*[color:gray]*[unknown]" else "*[color:blue]*$curWPS"
        ParseInTextTags(txtWPS)
    }

    companion object {
        private val SvgImageCache = HashMap<String, Drawable?>()
    }

    init {
        DataList = data
    }
}

class MyActivity : Activity() {
    /**
     * Called when the activity is first created.
     */
    private var mSettings: Settings? = null
    private var User: UserManager? = null
    private var btnRefresh: Button? = null
    private var btnCheckFromBase: Button? = null
    private var WifiMgr: WifiManager? = null
    private var LocationMgr: LocationManager? = null
    private var sClipboard: ClipboardManager? = null
    protected var lastWiFiClickItem: LinearLayout? = null
    private var listContextMenuItems = arrayOfNulls<String>(6)
    fun btnRefreshOnClick(v: View?) {
        if (ScanInProcess) return
        if (WiFiKeys != null) WiFiKeys!!.clear()
        if (WiFiScanResult != null) WiFiScanResult!!.clear()
        val context = applicationContext
        val list = ArrayList<HashMap<String, String?>>()
        val adapter = SimpleAdapter(context, list, R.layout.row, arrayOf("ESSID", "BSSID"), intArrayOf(R.id.ESSID, R.id.BSSID))
        WiFiList!!.adapter = adapter
        ScanAndShowWiFi()
    }

    private val btnSettingsOnClick = View.OnClickListener {
        val intent = Intent(this@MyActivity, SettingsActivity::class.java)
        startActivity(intent)
    }
    private val btnStartGPSLogOnClick = View.OnClickListener { /*setContentView(R.layout.gpslogging);
            btnSettingsRevent = (ImageButton) findViewById(R.id.btnGPSLoggingRevent);
            btnSettingsRevent.setOnClickListener(btnSettingsReventOnClick);*/
    }
    private val btnCheckFromBaseOnClick = View.OnClickListener {
        if (ScanInProcess) return@OnClickListener
        if (WiFiKeys != null) WiFiKeys!!.clear()
        val dProccess = ProgressDialog(this@MyActivity)
        dProccess.setProgressStyle(ProgressDialog.STYLE_SPINNER)
        dProccess.setMessage(resources.getString(R.string.status_searching))
        dProccess.setCanceledOnTouchOutside(false)
        btnCheckFromBase!!.isEnabled = false
        dProccess.show()
        Thread(Runnable {
            CheckFromBase()
            dProccess.dismiss()
        }).start()
    }

    val activity: Activity
        get() = this

    private fun GetDataRowsFromLinLay(LinLay: LinearLayout?, Type: String): TextView? {
        when (Type) {
            "BSSID" -> return LinLay!!.findViewById<View>(R.id.BSSID) as TextView
            "ESSID" -> return LinLay!!.findViewById<View>(R.id.ESSID) as TextView
            "KEY" -> return LinLay!!.findViewById<View>(R.id.KEY) as TextView
        }
        return null
    }

    var WiFiListOnClick = OnItemClickListener { parent, linearLayout, position, id ->
        val item = linearLayout as LinearLayout
        lastWiFiClickItem = item
        val txtBSSID = GetDataRowsFromLinLay(item, "BSSID")
        val txtESSID = GetDataRowsFromLinLay(item, "ESSID")
        val dialogBuilder = AlertDialog.Builder(this@MyActivity)
        dialogBuilder.setTitle(if (txtESSID != null) txtESSID.text else "")
        val ESSDWps = txtESSID?.text?.toString() ?: ""
        val BSSDWps = txtBSSID?.text?.toString() ?: ""
        dialogBuilder.setItems(listContextMenuItems, DialogInterface.OnClickListener { dialog, item ->
            val apdata: APData
            var NeedToast = false
            val scanResult = WiFiScanResult!![id.toInt()]
            when (item) {
                0 -> {
                    val detailsActivityIntent = Intent(this@MyActivity, WifiDetails::class.java)
                    val WifiInfo = HashMap<String, String?>()
                    WifiInfo["BSSID"] = scanResult.BSSID
                    WifiInfo["SSID"] = scanResult.SSID
                    WifiInfo["Freq"] = Integer.toString(scanResult.frequency)
                    WifiInfo["Signal"] = Integer.toString(scanResult.level)
                    WifiInfo["Capabilities"] = scanResult.capabilities
                    detailsActivityIntent.putExtra("WifiInfo", WifiInfo)
                    startActivity(detailsActivityIntent)
                }
                1 -> {
                    val txtBSSID = GetDataRowsFromLinLay(lastWiFiClickItem, "ESSID")
                    val dataClip: ClipData
                    dataClip = ClipData.newPlainText("text", if (txtBSSID != null) txtBSSID.text else "")
                    sClipboard!!.setPrimaryClip(dataClip)
                    NeedToast = true
                }
                2 -> {
                    val txtESSID = GetDataRowsFromLinLay(lastWiFiClickItem, "BSSID")
                    val dataClip = ClipData.newPlainText("text", if (txtESSID != null) txtESSID.text else "")
                    sClipboard!!.setPrimaryClip(dataClip)
                    NeedToast = true
                }
                3 -> run {
                    if (WiFiKeys!!.isEmpty()) {
                        val toast = Toast.makeText(applicationContext,
                                getString(R.string.toast_no_data),
                                Toast.LENGTH_LONG)
                        toast.show()
                        return@run
                    }
                    apdata = WiFiKeys!![id.toInt()]
                    if (apdata.Keys!!.size < 1) {
                        val toast = Toast.makeText(applicationContext,
                                getString(R.string.toast_key_not_found), Toast.LENGTH_SHORT)
                        toast.show()
                        return@OnClickListener
                    }
                    val dataClip = ClipData.newPlainText("text", apdata.Keys!![0])
                    sClipboard!!.setPrimaryClip(dataClip)
                    NeedToast = true
                }
                4 -> run {
                    if (WiFiKeys!!.isEmpty()) {
                        val toast = Toast.makeText(applicationContext,
                                getString(R.string.toast_no_data),
                                Toast.LENGTH_LONG)
                        toast.show()
                        return@run
                    }
                    apdata = WiFiKeys!![id.toInt()]
                    if (apdata == null || apdata.Keys!!.size < 1) {
                        val toast = Toast.makeText(applicationContext,
                                getString(R.string.toast_key_not_found), Toast.LENGTH_SHORT)
                        toast.show()
                    }
                    if (!WifiMgr!!.isWifiEnabled) {
                        val toast = Toast.makeText(applicationContext,
                                getString(R.string.toast_wifi_disabled), Toast.LENGTH_SHORT)
                        toast.show()
                        return@run
                    }
                    val list = WifiMgr!!.configuredNetworks
                    var cnt = 0
                    for (wifi in list) {
                        if (wifi.SSID != null && wifi.SSID == "\"" + scanResult.SSID + "\"") cnt++
                    }
                    if (cnt > 0) {
                        val dialogClickListener = DialogInterface.OnClickListener { dialog, which ->
                            when (which) {
                                DialogInterface.BUTTON_POSITIVE -> addNetworkProfile(scanResult, apdata)
                                DialogInterface.BUTTON_NEGATIVE -> {
                                }
                            }
                            dialog.dismiss()
                        }
                        val builder = AlertDialog.Builder(this@MyActivity)
                        builder.setTitle(getString(R.string.dialog_are_you_sure))
                                .setMessage(String.format(getString(R.string.dialog_already_stored), scanResult.SSID, cnt))
                                .setPositiveButton(getString(R.string.dialog_yes), dialogClickListener)
                                .setNegativeButton(getString(R.string.dialog_no), dialogClickListener).show()
                    } else addNetworkProfile(scanResult, apdata)
                }
                5 -> run {
                    if (!scanResult.capabilities!!.contains("WPS")) {
                        val dialogClickListener = DialogInterface.OnClickListener { dialog, which ->
                            when (which) {
                                DialogInterface.BUTTON_POSITIVE -> wpsGenStart(ESSDWps, BSSDWps)
                                DialogInterface.BUTTON_NEGATIVE -> {
                                }
                            }
                            dialog.dismiss()
                        }
                        val builder = AlertDialog.Builder(this@MyActivity)
                        builder.setTitle(getString(R.string.dialog_are_you_sure))
                                .setMessage(String.format(getString(R.string.dialog_wps_disabled), scanResult.SSID))
                                .setPositiveButton(getString(R.string.dialog_yes), dialogClickListener)
                                .setNegativeButton(getString(R.string.dialog_no), dialogClickListener).show()
                        return@run
                    }
                    wpsGenStart(ESSDWps, BSSDWps)
                }
            }
            if (NeedToast) {
                val toast = Toast.makeText(applicationContext,
                        getString(R.string.toast_copied), Toast.LENGTH_SHORT)
                toast.show()
            }
            dialog.dismiss()
        })
        dialogBuilder.show()
    }

    private fun wpsGenStart(ESSDWps: String, BSSDWps: String) {
        val wpsActivityIntent = Intent(this@MyActivity, WPSActivity::class.java)
        wpsActivityIntent.putExtra("variable", ESSDWps)
        wpsActivityIntent.putExtra("variable1", BSSDWps)
        startActivity(wpsActivityIntent)
    }

    private fun addNetworkProfile(scanResult: MyScanResult, apdata: APData) {
        val WifiCfg = WifiConfiguration()
        WifiCfg.BSSID = scanResult.BSSID
        WifiCfg.SSID = String.format("\"%s\"", scanResult.SSID)
        WifiCfg.hiddenSSID = false
        WifiCfg.priority = 1000
        if (scanResult.capabilities!!.contains("WEP")) {
            WifiCfg.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
            WifiCfg.allowedProtocols.set(WifiConfiguration.Protocol.RSN)
            WifiCfg.allowedProtocols.set(WifiConfiguration.Protocol.WPA)
            WifiCfg.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN)
            WifiCfg.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED)
            WifiCfg.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP)
            WifiCfg.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP)
            WifiCfg.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40)
            WifiCfg.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104)
            WifiCfg.wepKeys[0] = String.format("\"%s\"", apdata.Keys!![0])
            WifiCfg.wepTxKeyIndex = 0
        } else {
            WifiCfg.preSharedKey = String.format("\"%s\"", apdata.Keys!![0])
        }
        val netId = WifiMgr!!.addNetwork(WifiCfg)
        val toast: Toast
        toast = if (netId > -1) {
            Toast.makeText(applicationContext,
                    getString(R.string.toast_network_stored), Toast.LENGTH_SHORT)
        } else {
            if (WifiMgr!!.isWifiEnabled) {
                Toast.makeText(applicationContext,
                        getString(R.string.toast_failed_to_store), Toast.LENGTH_SHORT)
            } else {
                Toast.makeText(applicationContext,
                        getString(R.string.toast_wifi_disabled), Toast.LENGTH_SHORT)
            }
        }
        toast.show()
    }

    public override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)
    }

    public override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (requestCode == WIFI_ENABLE_REQUEST || requestCode == LOCATION_ENABLE_REQUEST) {
            ScanAndShowWiFi()
        }
    }

    fun ApiDataTest() {
        if (!API_KEYS_VALID) {
            runOnUiThread {
                val t = Toast.makeText(applicationContext, getString(R.string.toast_enter_credentials), Toast.LENGTH_SHORT)
                t.show()
            }
            val startActivity = Intent(this, StartActivity::class.java)
            startActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(startActivity)
        }
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)
        val actionBar = actionBar
        actionBar!!.hide()
        listContextMenuItems = resources.getStringArray(R.array.menu_network)
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        APP_VERSION = resources.getString(R.string.app_version)
        mSettings = Settings(applicationContext)
        User = UserManager(applicationContext)
        API_READ_KEY = mSettings!!.AppSettings!!.getString(Settings.API_READ_KEY, "")
        API_WRITE_KEY = mSettings!!.AppSettings!!.getString(Settings.API_WRITE_KEY, "")
        API_KEYS_VALID = mSettings!!.AppSettings!!.getBoolean(Settings.API_KEYS_VALID, false)
        WiFiList = findViewById<View>(R.id.WiFiList) as ListView
        btnRefresh = findViewById<View>(R.id.btnRefresh) as Button
        btnCheckFromBase = findViewById<View>(R.id.btnCheckFromBase) as Button
        val btnSettings = findViewById<View>(R.id.btnSettings) as ImageButton
        val btnStartGPSLog = findViewById<View>(R.id.btnStartGPSLog) as Button
        WifiMgr = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        LocationMgr = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        sClipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        btnCheckFromBase!!.setOnClickListener(btnCheckFromBaseOnClick)
        btnStartGPSLog.setOnClickListener(btnStartGPSLogOnClick)
        btnSettings.setOnClickListener(btnSettingsOnClick)
        WiFiList!!.onItemClickListener = WiFiListOnClick
        if (adapter != null) {
            WiFiList!!.adapter = adapter
            btnCheckFromBase!!.isEnabled = true
        }
        if (ScanInProcess) {
            //   if(ScanWiFiReceiverIntent != null) unregisterReceiver(ScanWiFiReceiverIntent);
            //ScanAndShowWiFi();
        }
        ScanAndShowWiFi()
    }

    fun ScanAndShowWiFi() {
        val comparator = Comparator<MyScanResult> { lhs, rhs -> if (lhs.level < rhs.level) 1 else if (lhs.level == rhs.level) 0 else -1 }
        WiFiScanResult = null
        adapter = null
        if (!WifiMgr!!.isWifiEnabled) {
            val action = android.provider.Settings.ACTION_WIFI_SETTINGS
            val builder = AlertDialog.Builder(this@MyActivity)
            builder.setTitle(getString(R.string.toast_wifi_disabled))
            builder.setMessage(getString(R.string.dialog_message_please_enable_wifi))
            builder.setPositiveButton(getString(R.string.button_open_settings)
            ) { d, id ->
                this@MyActivity.startActivityForResult(Intent(action), WIFI_ENABLE_REQUEST)
                d.dismiss()
            }.setNegativeButton(getString(R.string.cancel)
            ) { d, id -> d.cancel() }
            val alert = builder.create()
            alert.show()
            return
        }
        if (Build.VERSION.SDK_INT > 27 && !LocationMgr!!.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            val action = android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS
            val builder = AlertDialog.Builder(this@MyActivity)
            builder.setMessage(getString(R.string.dialog_message_location_disabled))
            builder.setPositiveButton(getString(R.string.button_open_settings)
            ) { d, id ->
                this@MyActivity.startActivityForResult(Intent(action), LOCATION_ENABLE_REQUEST)
                d.dismiss()
            }.setNegativeButton(getString(R.string.cancel)
            ) { d, id -> d.cancel() }
            val alert = builder.create()
            alert.show()
            return
        }
        val dProccess = ProgressDialog(this@MyActivity)
        dProccess.setProgressStyle(ProgressDialog.STYLE_SPINNER)
        dProccess.setMessage(getString(R.string.status_scanning))
        dProccess.setCanceledOnTouchOutside(false)
        dProccess.show()
        ScanWiFiReceiverIntent = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val res = WifiMgr!!.scanResults
                val results: MutableList<MyScanResult> = ArrayList()
                for (result in res) {
                    val sc = MyScanResult()
                    sc.BSSID = result.BSSID
                    sc.SSID = result.SSID
                    sc.level = result.level
                    sc.frequency = result.frequency
                    sc.capabilities = result.capabilities
                    results.add(sc)
                }
                Collections.sort(results, comparator)
                WiFiScanResult = results
                val list = ArrayList<HashMap<String, String?>?>()
                var ElemWiFi: HashMap<String, String?>
                for (result in results) {
                    ElemWiFi = HashMap()
                    ElemWiFi["ESSID"] = result.SSID
                    ElemWiFi["BSSID"] = result.BSSID!!.toUpperCase()
                    ElemWiFi["KEY"] = "*[color:gray]*[no data]"
                    ElemWiFi["WPS"] = "*[color:gray]*[no data]"
                    ElemWiFi["SIGNAL"] = getStrSignal(result.level)
                    ElemWiFi["KEYSCOUNT"] = "*[color:gray]*0"
                    ElemWiFi["CAPABILITY"] = result.capabilities
                    list.add(ElemWiFi)
                }
                adapter = WiFiListSimpleAdapter(activity, list, R.layout.row, arrayOf("ESSID", "BSSID", "KEY", "WPS", "SIGNAL", "KEYSCOUNT", "CAPABILITY"), intArrayOf(R.id.ESSID, R.id.BSSID, R.id.KEY, R.id.txtWPS, R.id.txtSignal, R.id.txtKeysCount))
                WiFiList!!.adapter = adapter
                ScanInProcess = false
                btnRefresh!!.isEnabled = true
                btnCheckFromBase!!.isEnabled = true
                val toast = Toast.makeText(applicationContext,
                        getString(R.string.toast_scan_complete), Toast.LENGTH_SHORT)
                toast.show()
                unregisterReceiver(this)
                ScanWiFiReceiverIntent = null
                dProccess.dismiss()
            }
        }
        registerReceiver(ScanWiFiReceiverIntent, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
        ScanInProcess = true
        btnRefresh!!.isEnabled = false
        btnCheckFromBase!!.isEnabled = false
        WifiMgr!!.startScan()
    }

    private fun CheckFromBase() {
        var bss = JSONObject()
        val Reader: BufferedReader
        var ReadLine: String?
        val RawData = StringBuilder()
        val FETCH_ESS: Boolean
        try {
            val query = JSONObject()
            query.put("key", API_READ_KEY)
            val bssids = JSONArray()
            val essids = JSONArray()
            for (result in WiFiScanResult!!) {
                bssids.put(result.BSSID)
                essids.put(result.SSID)
            }
            mSettings!!.Reload()
            FETCH_ESS = mSettings!!.AppSettings!!.getBoolean(Settings.APP_FETCH_ESS, false)
            query.put("bssid", bssids)
            if (FETCH_ESS) query.put("essid", essids)
            val SERVER_URI = mSettings!!.AppSettings!!.getString(Settings.APP_SERVER_URI, resources.getString(R.string.SERVER_URI_DEFAULT))
            val Uri = URL("$SERVER_URI/api/apiquery")
            val Connection = Uri.openConnection() as HttpURLConnection
            Connection.requestMethod = "POST"
            Connection.doOutput = true
            Connection.setRequestProperty("Content-Type", "application/json")
            val writer = DataOutputStream(
                    Connection.outputStream)
            writer.writeBytes(query.toString())
            Connection.readTimeout = 10 * 1000
            Connection.connect()
            Reader = BufferedReader(InputStreamReader(Connection.inputStream))
            while (Reader.readLine().also { ReadLine = it } != null) {
                RawData.append(ReadLine)
            }
            try {
                val json = JSONObject(RawData.toString())
                val ret = json.getBoolean("result")
                if (!ret) {
                    // API failure
                    val error = json.getString("error")
                    val errorDesc = User!!.getErrorDesc(error, this)
                    if (error == "loginfail") {
                        mSettings!!.Editor!!.putBoolean(Settings.API_KEYS_VALID, false)
                        mSettings!!.Editor!!.commit()
                        API_KEYS_VALID = false
                        ApiDataTest()
                        return
                    }
                    runOnUiThread {
                        val t = Toast.makeText(applicationContext, errorDesc, Toast.LENGTH_SHORT)
                        t.show()
                        btnCheckFromBase!!.isEnabled = true
                    }
                    return
                }
                if (!json.isNull("data")) {
                    bss = try {
                        json.getJSONObject("data")
                    } catch (e: Exception) {
                        // add empty object
                        JSONObject()
                    }
                }
            } catch (e: Exception) {
                // JSON error
                runOnUiThread {
                    val t = Toast.makeText(applicationContext, getString(R.string.toast_database_failure), Toast.LENGTH_SHORT)
                    t.show()
                    btnCheckFromBase!!.isEnabled = true
                }
                return
            }
        } catch (e: Exception) {
            // Connection error
            runOnUiThread {
                val t = Toast.makeText(applicationContext, getString(R.string.status_no_internet), Toast.LENGTH_SHORT)
                t.show()
                btnCheckFromBase!!.isEnabled = true
            }
            return
        }
        val list = ArrayList<HashMap<String, String?>?>()
        var ElemWiFi: HashMap<String, String?>
        var KeyColor: String
        var i = 0
        for (result in WiFiScanResult!!) {
            val apdata = GetWiFiKeyByBSSID(bss, FETCH_ESS, result.SSID, result.BSSID!!.toUpperCase())
            ElemWiFi = HashMap()
            ElemWiFi["ESSID"] = result.SSID
            ElemWiFi["BSSID"] = result.BSSID!!.toUpperCase()
            ElemWiFi["SIGNAL"] = getStrSignal(result.level)
            if (apdata.Keys!!.size < 1) {
                ElemWiFi["KEY"] = "*[color:gray]*[unknown]"
                ElemWiFi["KEYSCOUNT"] = "*[color:gray]*" + Integer.toString(apdata.Keys!!.size)
            } else {
                KeyColor = if (apdata.Generated!![0]) "*[color:red]*" else "*[color:green]*"
                ElemWiFi["KEY"] = KeyColor + apdata.Keys!![0]
                ElemWiFi["KEYSCOUNT"] = "*[color:green]*" + Integer.toString(apdata.Keys!!.size)
            }
            if (apdata.WPS!!.size < 1 || apdata.WPS!![0].isEmpty()) {
                ElemWiFi["WPS"] = "*[color:gray]*[unknown]"
            } else {
                ElemWiFi["WPS"] = "*[color:blue]*" + apdata.WPS!![0]
            }
            ElemWiFi["CAPABILITY"] = result.capabilities
            list.add(ElemWiFi)
            WiFiKeys!!.add(i, apdata)
            i++
        }
        adapter = WiFiListSimpleAdapter(activity, list, R.layout.row, arrayOf("ESSID", "BSSID", "KEY", "WPS", "SIGNAL", "KEYSCOUNT", "CAPABILITY"), intArrayOf(R.id.ESSID, R.id.BSSID, R.id.KEY, R.id.txtWPS, R.id.txtSignal, R.id.txtKeysCount))
        runOnUiThread(Thread(Runnable {
            WiFiList!!.adapter = adapter
            btnCheckFromBase!!.isEnabled = true
        }
        ))
    }

    fun KeyWPSPairExists(keys: ArrayList<String>, pins: ArrayList<String>, key: String, pin: String): Boolean {
        for (i in keys.indices) {
            if (keys[i] == key && pins[i] == pin) return true
        }
        return false
    }

    fun GetWiFiKeyByBSSID(bss: JSONObject, fetchESS: Boolean, ESSID: String?, BSSID: String): APData {
        val keys = ArrayList<String>()
        val gen = ArrayList<Boolean>()
        val wpsPins = ArrayList<String>()
        try {
            val `val` = if (fetchESS) "$BSSID|$ESSID" else BSSID
            val Successes = !bss.isNull(`val`)
            if (Successes) {
                val rows = bss.getJSONArray(`val`)
                for (i in 0 until rows.length()) {
                    val row = rows.getJSONObject(i)
                    val key = row.getString("key")
                    val wps = row.getString("wps")
                    if (KeyWPSPairExists(keys, wpsPins, key, wps)) continue
                    keys.add(key)
                    wpsPins.add(wps)
                    gen.add(false)
                }
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        if (keys.size == 0) {
            val PassiveKey = PassiveVulnerabilityTest(ESSID, BSSID)
            if (!PassiveKey.isEmpty()) {
                keys.add(PassiveKey)
                gen.add(true)
                wpsPins.add("")
            }
        }
        val apdata = APData()
        apdata.BSSID = BSSID
        apdata.Keys = keys
        apdata.Generated = gen
        apdata.WPS = wpsPins
        return apdata
    }

    fun PassiveVulnerabilityTest(ESSID: String?, BSSID: String): String {
        var ret = ""
        if (ESSID!!.length > 9) {
            if (ESSID.substring(0, 9) == "MGTS_GPON") {
                ret = BSSID.replace(":", "")
                ret = ret.substring(4, 12)
            }
        }
        return ret
    }

    private fun getStrSignal(Signal: Int): String {
        var Signal = Signal
        var Color = ""
        Signal = (100 + Signal) * 2
        Signal = Math.min(Math.max(Signal, 0), 100)
        if (Signal < 48) Color = "*[color:red]*"
        if (Signal >= 48 && Signal < 65) Color = "*[color:yellow]*"
        if (Signal >= 65) Color = "*[color:greendark]*"
        return Color + Integer.toString(Signal) + "%"
    }

    companion object {
        var APP_VERSION = ""
        var API_READ_KEY: String? = ""
        var API_WRITE_KEY: String? = ""
        var API_KEYS_VALID = false
        var WiFiList: ListView? = null
        private var WiFiScanResult: MutableList<MyScanResult>? = null
        var WiFiKeys: ArrayList<APData>? = ArrayList()
        private var ScanInProcess = false
        private var ScanWiFiReceiverIntent: BroadcastReceiver? = null
        private var adapter: WiFiListSimpleAdapter? = null
        const val WIFI_ENABLE_REQUEST = 1
        const val LOCATION_ENABLE_REQUEST = 2
    }
}