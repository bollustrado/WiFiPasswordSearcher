package com.example.WiFiPasswordSearcher

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.media.AudioManager
import android.media.SoundPool
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Bundle
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.Viewport
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import java.util.*

class WifiDetails : Activity() {
    private var WiFiInfo: ScanResult? = null
    private var DetectorThread: Thread? = null
    private var mSoundPool: SoundPool? = null
    private var WifiMgr: WifiManager? = null
    private var NetworkBSSID: String? = null
    private var NetworkESSID: String? = null
    private var txtBSSID: TextView? = null
    private var txtESSID: TextView? = null
    private var txtFreq: TextView? = null
    private var txtSignal: TextView? = null
    private var txtChannel: TextView? = null
    private var ScanThreadActive = false
    private var UseWifiDetector = false
    private var LastSignal = 0
    private var LastFreq = -1
    private var LastBSSID: String? = null
    private var LastESSID: String? = null
    private var mSettings: Settings? = null
    private var graphSeries: LineGraphSeries<DataPoint>? = null
    private var graphView: GraphView? = null
    private var iGraphPointCount = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.wifi_details)
        onConfigurationChanged(resources.configuration)
        UseWifiDetector = false
        val StartWifiInfo = intent.getSerializableExtra("WifiInfo") as HashMap<String, String>
        NetworkBSSID = StartWifiInfo["BSSID"]
        NetworkESSID = StartWifiInfo["SSID"]
        txtBSSID = findViewById(R.id.txtDetailsBSSID) as TextView
        txtESSID = findViewById(R.id.txtDetailsESSID) as TextView
        txtFreq = findViewById(R.id.txtDetailsFreq) as TextView
        txtSignal = findViewById(R.id.txtDetailsSignal) as TextView
        txtChannel = findViewById(R.id.txtDetailsChannel) as TextView
        val chkbUseDetector = findViewById(R.id.chkbUseDetector) as CheckBox
        val llGrphView = findViewById(R.id.llGrphView) as LinearLayout
        mSoundPool = SoundPool(1, AudioManager.STREAM_MUSIC, 0)
        // Graph init
        iGraphPointCount = 0
        graphSeries = LineGraphSeries()
        graphView = GraphView(this)
        graphView!!.gridLabelRenderer.numVerticalLabels = 2
        graphView!!.gridLabelRenderer.isHorizontalLabelsVisible = false
        graphView!!.viewport.setMinY(0.0)
        graphView!!.viewport.setMaxY(100.0)
        graphView!!.viewport.yAxisBoundsStatus = Viewport.AxisBoundsStatus.FIX
        graphView!!.viewport.isYAxisBoundsManual = true
        graphView!!.title = getString(R.string.label_signal_graph)
        graphView!!.addSeries(graphSeries)
        llGrphView.addView(graphView)
        chkbUseDetector.setOnCheckedChangeListener { buttonView, isChecked ->
            UseWifiDetector = isChecked
            mSettings!!.Editor!!.putBoolean(Settings.WIFI_SIGNAL, UseWifiDetector)
            mSettings!!.Editor!!.commit()
            if (UseWifiDetector) {
                DetectorThread = Thread(Runnable { DetectorWorker() })
                DetectorThread!!.start()
            }
        }
        mSettings = Settings(applicationContext)
        mSettings!!.Reload()
        val wifiSignal = mSettings!!.AppSettings!!.getBoolean(Settings.WIFI_SIGNAL, false)
        chkbUseDetector.isChecked = wifiSignal
        setBSSID(StartWifiInfo["BSSID"])
        setESSID(StartWifiInfo["SSID"])
        setFreq(StartWifiInfo["Freq"])
        setSignal(StartWifiInfo["Signal"])
        WifiMgr = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val ScanThread = Thread(Runnable { ScanWorker() })
        ScanThreadActive = true
        ScanThread.start()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val LR = findViewById(R.id.rootLayout) as LinearLayout
        val LI = findViewById(R.id.layoutInfo) as LinearLayout
        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            LR.orientation = LinearLayout.VERTICAL
            val layoutParams = LI.layoutParams
            layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
            LI.layoutParams = layoutParams
        } else if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            LR.orientation = LinearLayout.HORIZONTAL
            val layoutParams = LI.layoutParams
            layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
            LI.layoutParams = layoutParams
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        UseWifiDetector = false
        ScanThreadActive = false
        graphView!!.removeAllSeries()
        graphSeries = null
    }

    private fun Update() {
        setBSSID(WiFiInfo!!.BSSID)
        setESSID(WiFiInfo!!.SSID)
        setFreq(Integer.toString(WiFiInfo!!.frequency))
        setSignal(Integer.toString(WiFiInfo!!.level))
    }

    private fun DetectorWorker() {
        val PickSoundId = mSoundPool!!.load(applicationContext, R.raw.pick, 1)
        while (UseWifiDetector) {
            if (LastSignal > 0) mSoundPool!!.play(PickSoundId, 1f, 1f, 100, 0, 1f)
            val SleepTime = 2100 - 2000 / 100 * LastSignal
            try {
                Thread.sleep(SleepTime.toLong(), 0)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }

    private fun ScanWorker() // THREADED!
    {
        while (ScanThreadActive) {
            var results: List<ScanResult>
            var Founded = false
            WifiMgr!!.startScan()
            results = WifiMgr!!.scanResults
            var match: Boolean
            for (result in results) {
                match = if (!NetworkBSSID!!.isEmpty() && !NetworkESSID!!.isEmpty()) {
                    result.BSSID == NetworkBSSID && result.SSID == NetworkESSID
                } else if (NetworkBSSID!!.isEmpty() && !NetworkESSID!!.isEmpty()) {
                    result.SSID == NetworkESSID
                } else if (!NetworkBSSID!!.isEmpty() && NetworkESSID!!.isEmpty()) {
                    result.BSSID == NetworkBSSID
                } else {
                    true
                }
                if (match) {
                    if (NetworkBSSID!!.isEmpty()) NetworkBSSID = result.BSSID
                    if (NetworkESSID!!.isEmpty()) NetworkESSID = result.SSID
                    WiFiInfo = result
                    Update()
                    Founded = true
                    break
                }
            }
            if (!Founded) setSignal("-100")
            try {
                Thread.sleep(1000.toLong(), 0)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }

    private fun setBSSID(BSSID: String?) {
        var BSSID = BSSID
        BSSID = BSSID!!.toUpperCase()
        if (BSSID == LastBSSID) return
        val text = "BSSID: " + if (BSSID.isEmpty()) "Unknown" else BSSID
        runOnUiThread { txtBSSID!!.text = text }
        LastBSSID = BSSID
    }

    private fun setESSID(ESSID: String?) {
        if (ESSID == LastESSID) return
        val text = if (ESSID!!.isEmpty()) "<unknown>" else ESSID
        runOnUiThread { txtESSID!!.text = text }
        LastESSID = ESSID
    }

    private fun setFreq(Freq: String?) {
        var sDiap = ""
        var Channel = 0
        val iFreq = Freq!!.toInt()
        if (iFreq == LastFreq) return
        if (iFreq > 0) {
            when (iFreq) {
                in 2401..2483 -> {
                    sDiap = "2.4 GHz"
                    Channel = (iFreq - 2412) / 5 + 1
                }
                in 5150..5250 -> {
                    sDiap = "UNII 1"
                    Channel = (5000 + iFreq) / 5
                }
                in 5250..5350 -> {
                    sDiap = "UNII 2"
                    Channel = (5000 + iFreq) / 5
                }
                in 5470..5725 -> {
                    sDiap = "UNII 2 Extended"
                    Channel = iFreq / 5 + 1
                }
                in 5725..5825 -> {
                    sDiap = "UNII 3"
                    Channel = (5000 + iFreq) / 5
                }
            }
        }
        val sText = getString(R.string.label_freq) + if (iFreq <= 0) "Unknown" else "$Freq MHz ($sDiap)"
        runOnUiThread { txtFreq!!.text = sText }
        setChannel(Channel)
        LastFreq = iFreq
    }

    private fun setSignal(Signal: String?) {
        var iSignal = Signal!!.toInt()
        iSignal = (100 + iSignal) * 2
        iSignal = Math.min(Math.max(iSignal, 0), 100)
        val fSignal = iSignal
        LastSignal = iSignal
        runOnUiThread {
            graphSeries!!.appendData(
                    DataPoint(iGraphPointCount.toDouble(), fSignal.toDouble()),
                    true,
                    25
            )
            iGraphPointCount++
            txtSignal!!.text = getString(R.string.label_signal) + fSignal + "%"
        }
    }

    private fun setChannel(Channel: Int) {
        runOnUiThread { txtChannel!!.text = getString(R.string.label_channel) + if (Channel <= 0) "N/A" else Channel }
    }
}