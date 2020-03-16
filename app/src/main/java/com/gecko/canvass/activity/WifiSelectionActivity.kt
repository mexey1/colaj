package com.gecko.canvass.activity

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gecko.canvass.R
import com.gecko.canvass.adapters.WifiSelectionListAdapter
import com.gecko.canvass.custom.CustomDialogFragment
import com.gecko.canvass.custom.isOpen
import com.gecko.canvass.interfaces.DialogClickListener
import com.gecko.canvass.interfaces.PermissionsInterface
import com.gecko.canvass.utility.*


class WifiSelectionActivity : AppCompatActivity(),PermissionsInterface {

    private val TAG ="WifiSelectionActivity"
    private lateinit var wifiManager: WifiManager
    private  var width: Int = 0
    private var height: Int =0
    private lateinit var wifiDisabledLayout:ViewGroup
    private lateinit var parent: ViewGroup
    private lateinit var receiver: BroadcastReceiver
    private lateinit var wifiRecyclerView: RecyclerView
    private lateinit var wifiSelectionAdapter: WifiSelectionListAdapter
    private  var dialog:CustomDialogFragment? = null
    private  var device:String? = null //device to be setup

    /**
     * method overridden to create wifi activity UI
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        UtilityClass.reportException()
        UtilityClass.applicationContext = applicationContext//we set and hold a reference to application context from UtilityClass
        UtilityClass.context = this
        setContentView(R.layout.wifi_activity)
        device = intent.getStringExtra("device")
        parent = findViewById(R.id.parent)
        wifiDisabledLayout = layoutInflater.inflate(R.layout.wifi_state_change_disabled,parent,false) as ViewGroup
        parent.addView(wifiDisabledLayout)
        showRecyclerView()
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        if(!isPermissionGranted(Manifest.permission.CHANGE_WIFI_STATE))
            requestPermissions(WIFI_REQUEST_CODE,arrayOf(Manifest.permission.CHANGE_WIFI_STATE))
        else{
            dialog = UtilityClass.showDialog("WiFi","Searching for WiFi networks", PROGRESS_DIALOG,supportFragmentManager,null,0,-1)
            registerWifiBroadcastListener()
            wifiManager.startScan()
        }
    }

    /**
     * method to register a listener for wifi related scan results
     */
    private fun registerWifiBroadcastListener(){
        var filter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        receiver = object : BroadcastReceiver(){
            override fun onReceive(context: Context, intent: Intent) {
                when(intent.action){
                    WifiManager.SCAN_RESULTS_AVAILABLE_ACTION->{
                        dialog?.dismiss()
                        dialog = null
                        if (!this@WifiSelectionActivity::wifiRecyclerView.isInitialized) {
                            wifiRecyclerView = parent.findViewById(R.id.wifi_list)
                            //btRecyclerView.visibility = RecyclerView.VISIBLE
                            wifiSelectionAdapter = WifiSelectionListAdapter()
                            //wifiSelectionAdapter.setContext(this@WifiSelectionActivity)
                            wifiRecyclerView.adapter = wifiSelectionAdapter
                            wifiRecyclerView.layoutManager =
                                LinearLayoutManager(this@WifiSelectionActivity)
                        }
                        var wifiNetworks = wifiManager.scanResults
                        showRecyclerView()
                        var count = 0
                        wifiNetworks.forEach {
                            Log.d("WifiSelectionActivity","network_name = ${it.SSID} capabilities=${it.capabilities}")
                            wifiSelectionAdapter.addWifiNetwork(it,wifiManager.isOpen(it))
                            wifiSelectionAdapter.notifyItemInserted(count++)
                        }
                    }
                }
            }
        }
        registerReceiver(receiver,filter)
    }

    /*override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG,"Media result ${data?.data} for request cod $requestCode")
        data!!.data?.let { Bluetooth.sendImage(it,this@WifiSelectionActivity) }
    }*/

    /**
     * method overridden to know if user agreed to grant location permission
     */
    override fun onRequestPermissionsResult(requestCode: Int,permissions: Array<out String>,grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(grantResults[0]==PackageManager.PERMISSION_GRANTED){
            dialog = UtilityClass.showDialog("WiFi","Searching for WiFi networks", PROGRESS_DIALOG,supportFragmentManager,null,0,-1)
            registerWifiBroadcastListener()
            wifiManager.startScan()
        }
        else
            permissionNotGranted(WIFI_DISABLED)
    }

    /**
     * this method is overridden to show rationale if user has denied or hasn't granted wifi
     * permissions
     */
    override fun showRequestPermissionRationale(requestCode: Int) {
        var listener = object : DialogClickListener {
            override fun onClick() {
                requestPermissions(requestCode,arrayOf(Manifest.permission.CHANGE_WIFI_STATE))
                Log.d(BLUETOOTH_ACTIVITY, "Dialog Button clicked. requesting permission")
            }
        }
        UtilityClass.showDialog("Wifi Access",
            "Permission is needed to scan for available WiFi networks to complete setup of the Canvass",
            INFO_DIALOG,supportFragmentManager,listener,width,height)
    }

    override fun requestPermissions(requestCode: Int,permissions: Array<String>) {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),20)
    }

    override fun permissionNotGranted(feature: Int) {
        when(feature){
            WIFI_DISABLED->hideRecyclerView()
        }
    }

    /**
     * method to hide the recycler view if wifi permission is denied
     */
    private fun hideRecyclerView(){
        parent.findViewById<ViewGroup>(R.id.wifi_layout).visibility=View.GONE
        wifiDisabledLayout?.visibility = View.VISIBLE
    }

    /**
     * method to show the recycler view if wifi permission is granted
     */
    private fun showRecyclerView(){
        parent.findViewById<ViewGroup>(R.id.wifi_layout).visibility=View.VISIBLE
        wifiDisabledLayout?.visibility = View.GONE
    }

    override fun isPermissionGranted(feature: String): Boolean {
        return ContextCompat.checkSelfPermission(this,feature)!= PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
        wifiSelectionAdapter.releaseAdapter()
    }
}