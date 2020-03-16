package com.gecko.canvass.activity

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gecko.canvass.R
import com.gecko.canvass.adapters.DeviceScanListAdapter
import com.gecko.canvass.custom.CustomDialogFragment
import com.gecko.canvass.custom.isOpen
import com.gecko.canvass.interfaces.CountDownListener
import com.gecko.canvass.interfaces.DeviceScanListener
import com.gecko.canvass.interfaces.DialogClickListener
import com.gecko.canvass.interfaces.PermissionsInterface
import com.gecko.canvass.logic.DeviceDiscoveryServer
import com.gecko.canvass.logic.WifiSetup
import com.gecko.canvass.utility.*
import org.json.JSONObject


class DeviceScanActivity : AppCompatActivity(),PermissionsInterface,DeviceScanListener {

    private val TAG ="DeviceScanActivity"
    private lateinit var wifiManager: WifiManager
    private  var width: Int = 0
    private var height: Int =0
    private lateinit var wifiDisabledLayout:ViewGroup
    private lateinit var locationDisabledLayout:ViewGroup
    private lateinit var deviceNotFoundLayout:ViewGroup
    private lateinit var parent: ViewGroup
    private lateinit var receiver: BroadcastReceiver
    private lateinit var filter: IntentFilter
    private lateinit var deviceScanRecyclerView: RecyclerView
    private lateinit var deviceScanListAdapter: DeviceScanListAdapter
    private  var dialog:CustomDialogFragment? = null
    private var count = 0
    private lateinit var currentWifi:WifiInfo
    /**
     * method overridden to create wifi activity UI
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        UtilityClass.applicationContext = applicationContext//we set and hold a reference to application context from UtilityClass
        UtilityClass.context = this
        UtilityClass.reportException()
        setContentView(R.layout.device_scan_activity)
        parent = findViewById(R.id.parent)
        deviceNotFoundLayout = parent.findViewById(R.id.device_not_found)
        //deviceScanRecyclerView = parent.findViewById(R.id.device_list)
        wifiDisabledLayout = layoutInflater.inflate(R.layout.wifi_state_change_disabled,parent,false) as ViewGroup
        locationDisabledLayout = layoutInflater.inflate(R.layout.location_disabled,parent,false) as ViewGroup
        parent.addView(wifiDisabledLayout)
        parent.addView(locationDisabledLayout)
        showRecyclerView()
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        if(!isPermissionGranted(Manifest.permission.CHANGE_WIFI_STATE) || !isPermissionGranted(Manifest.permission.CHANGE_NETWORK_STATE)){
            Log.d(TAG,"Requesting Wifi Permission")
            requestPermissions(WIFI_REQUEST_CODE,arrayOf(Manifest.permission.CHANGE_WIFI_STATE,Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.CHANGE_NETWORK_STATE))
        }
        else{
            displaySearchingDialog()
            registerWifiBroadcastListener()
            wifiManager.startScan()
        }
        parent.findViewById<Button>(R.id.retry).setOnClickListener(View.OnClickListener {
            if(::deviceScanListAdapter.isInitialized)
                deviceScanListAdapter.clearDeviceList()
            displaySearchingDialog()
            registerReceiver(receiver,filter)
            wifiManager.startScan()
        })
    }

    /**
     * method to register a listener for wifi related scan results
     */
    private fun registerWifiBroadcastListener(){
        filter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        receiver = object : BroadcastReceiver(){
            override fun onReceive(context: Context, intent: Intent) {
                when(intent.action){
                    WifiManager.SCAN_RESULTS_AVAILABLE_ACTION->{
                        unregisterReceiver(receiver)//unregister the receiver so we don't keep getting results when switching between networks
                        //once we receive the scan results, we need to immediately start the server to await responses from discovered devices
                        DeviceDiscoveryServer.acceptConnection(this@DeviceScanActivity)
                        //val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                        fun scanCompleted(){
                            if(!::deviceScanListAdapter.isInitialized || deviceScanListAdapter.itemCount == 0)
                                showDeviceNotFoundLayout()
                            dialog?.dismiss()
                        }
                        val wifiNetworks = wifiManager.scanResults
                        /**
                         * we'd loop through each WiFi that's found and only attempt to connect to the
                         * open networks. First, we'd register a count down listener
                         */
                        WifiSetup.countDownListener = object : CountDownListener {
                            override fun onFinish() {
                                Handler.handler.post(Runnable {
                                    scanCompleted()
                                })
                            }
                        }
                        wifiNetworks.forEach {
                            Log.d("WifiSelectionActivity","network_name = ${it.SSID} capabilities=${it.capabilities}")
                            Log.d(TAG,"WiFi is open ${wifiManager.isOpen(it)}")
                            if(wifiManager.isOpen(it) && it.SSID.contains("colaj",true)){
                                UtilityClass.showToast("connecting to ${it.SSID}")
                                WifiSetup.openWifi.incrementAndGet()
                                ThreadPool.postTask(Runnable { WifiSetup.connectToWifiNetwork(it) })
                            }
                        }
                        if(WifiSetup.openWifi.get() ==0)
                            scanCompleted()
                        //startTimer()
                    }
                }
            }
        }
        /*if(!isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION)){
            Log.d(TAG,"Requesting Location Permission")
            requestPermissions(LOCATION_REQUEST_CODE,arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))

        }*/
        currentWifi = wifiManager.connectionInfo
        registerReceiver(receiver,filter)
    }

    private fun displaySearchingDialog(){
        dialog = UtilityClass.showDialog("Colaj","Searching for nearby devices", PROGRESS_DIALOG,supportFragmentManager,null,0,-1)
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
        if(grantResults[0]==PackageManager.PERMISSION_GRANTED && grantResults[1]==PackageManager.PERMISSION_GRANTED){
            displaySearchingDialog()
            registerWifiBroadcastListener()
            wifiManager.startScan()
        }
        else
            if(!isPermissionGranted(Manifest.permission.CHANGE_WIFI_STATE))permissionNotGranted(WIFI_DISABLED)
            else if (!isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION))permissionNotGranted(LOCATION_DISABLED)
    }

    /**
     * this method is overridden to show rationale if user has denied or hasn't granted wifi
     * permissions
     */
    override fun showRequestPermissionRationale(requestCode: Int) {
        var listener = object : DialogClickListener {
            override fun onClick() {
                requestPermissions(requestCode,arrayOf(Manifest.permission.CHANGE_WIFI_STATE))
                Log.d(TAG, "Dialog Button clicked. requesting permission")
            }
        }
        UtilityClass.showDialog("Wifi Access",
            "Permission is needed to complete setup of Colaj",
            INFO_DIALOG,supportFragmentManager,listener,width,height)
    }

    override fun requestPermissions(requestCode:Int,permissions: Array<String>) {
        ActivityCompat.requestPermissions(this, permissions,requestCode)
    }

    override fun permissionNotGranted(feature: Int) {
        when(feature){
            WIFI_DISABLED->hideRecyclerViewAndShowView(WIFI_DISABLED)
            LOCATION_DISABLED->hideRecyclerViewAndShowView(LOCATION_DISABLED)
        }
    }

    /**
     * method to hide the recycler view if wifi permission is denied
     */
    private fun hideRecyclerViewAndShowView(viewToShow: Int){
        parent.findViewById<ViewGroup>(R.id.wifi_layout).visibility=View.GONE
        wifiDisabledLayout?.visibility = View.GONE
        locationDisabledLayout?.visibility = View.GONE
        when(viewToShow){
            WIFI_DISABLED->wifiDisabledLayout?.visibility = View.VISIBLE
            LOCATION_DISABLED->locationDisabledLayout?.visibility = View.VISIBLE
        }
    }

    private fun showDeviceNotFoundLayout(){
        parent.findViewById<ViewGroup>(R.id.wifi_layout).visibility=View.GONE
        wifiDisabledLayout?.visibility = View.GONE
        locationDisabledLayout?.visibility = View.GONE
        deviceNotFoundLayout.visibility = View.VISIBLE
    }

    /**
     * method to show the recycler view if wifi permission is granted
     */
    private fun showRecyclerView(){
        parent.findViewById<ViewGroup>(R.id.wifi_layout).visibility=View.VISIBLE
        deviceNotFoundLayout.visibility = View.GONE
        wifiDisabledLayout?.visibility = View.GONE
        locationDisabledLayout?.visibility = View.GONE
    }

    override fun isPermissionGranted(feature: String): Boolean {
        return ContextCompat.checkSelfPermission(this,feature)!= PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        DeviceDiscoveryServer.closeConnection()
        unregisterReceiver(receiver)
        deviceScanListAdapter.releaseAdapter()
    }

    private fun dismissDialog(){
        if(count == 0){
            dialog?.dismiss()
            dialog = null
        }
    }

    /**
     * method overridden to implement action when scan is completed
     */
    override fun onProbeCompleted(response: JSONObject?) {
        Log.d(TAG,"Response received $response")
        Handler.handler.post(Runnable {
            if (!this@DeviceScanActivity::deviceScanRecyclerView.isInitialized) {
                deviceScanRecyclerView = parent.findViewById(R.id.device_list)
                //btRecyclerView.visibility = RecyclerView.VISIBLE
                deviceScanListAdapter = DeviceScanListAdapter()
                //wifiAdapter.setContext(this@WifiSelectionActivity)
                deviceScanRecyclerView.adapter = deviceScanListAdapter
                deviceScanRecyclerView.layoutManager =
                    LinearLayoutManager(this@DeviceScanActivity)
            }
            if(response!=null) {
                showRecyclerView()
                deviceScanListAdapter.addDevice(response)
            }
        })
    }
}