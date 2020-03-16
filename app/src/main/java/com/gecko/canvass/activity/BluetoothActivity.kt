package com.gecko.canvass.activity

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.opengl.Visibility
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gecko.canvass.R
import com.gecko.canvass.adapters.BTListAdapter
import com.gecko.canvass.custom.CustomDialogFragment
import com.gecko.canvass.interfaces.DialogClickListener
import com.gecko.canvass.interfaces.PermissionsInterface
import com.gecko.canvass.logic.Bluetooth
import com.gecko.canvass.utility.*
import java.nio.ByteBuffer
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


class BluetoothActivity:AppCompatActivity(),PermissionsInterface {

    private val TAG = "BluetoothActivity"
    private val noBluetooth:String = "This device doesn't support bluetooth connectivity"
    private lateinit var parent:ViewGroup
    private lateinit var receiver:BroadcastReceiver
    private lateinit var callback: ScanCallback
    private  var width: Int = 0
    private var height: Int =0
    private lateinit var  bluetoothDisabledLayout: ViewGroup
    private lateinit var locationDisabled:ViewGroup
    private lateinit var deviceScan: ViewGroup
    private var isInitCalled = false
    private lateinit var btRecyclerView: RecyclerView
    private lateinit var btAdapter: BTListAdapter
    private var btDeviceHashMap = HashMap<String,BluetoothDevice>()
    private  var dialog:CustomDialogFragment? = null

    /**
     * overridden function to display the bluetooth activity
     */
    override fun onCreate(savedInstanceState: android.os.Bundle?)
    {
        super.onCreate(savedInstanceState)
        UtilityClass.reportException()
        UtilityClass.applicationContext = applicationContext//we set and hold a reference to application context from UtilityClass
        UtilityClass.context = this
        width = WindowManager.LayoutParams.MATCH_PARENT
        height = applicationContext.resources.getDimension(R.dimen._250dp).toInt()
        setContentView(R.layout.bluetooth_activity)
        parent = findViewById(R.id.parent)
        if(Bluetooth.adapter!=null){//if device has bluetooth adapter
            //we need to ask the user to grant certain permissions
            var preferences = applicationContext.getSharedPreferences("canvas", Context.MODE_PRIVATE)
            if(Build.VERSION.SDK_INT>22 && isPermissionGranted(Manifest.permission.ACCESS_COARSE_LOCATION)){
                if(!preferences.getBoolean(REQUESTED_BLUETOOTH,false)) {//have we requested bluetooth permission before. Default is false. If we haven't requested, go ahead and request
                    requestPermissions(LOCATION_REQUEST_CODE,arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION))
                    preferences.edit().putBoolean(REQUESTED_BLUETOOTH,true).apply()//let's make a note that we have requested bluetooth permission in the past
                }
                else if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.ACCESS_COARSE_LOCATION))//permission was denied before
                    showRequestPermissionRationale(LOCATION_REQUEST_CODE)
                else{
                    Log.d("Rationale?","false")
                    //bluetoothOrLocationPermissionDisabled(LOCATION_DISABLED)
                    permissionNotGranted(LOCATION_DISABLED)
                    //requestBluetoothPermission()
                }
            }
            else {//if we have permission
                registerBluetoothReceiver()//register receiver for bluetooth devices found
                init()
            }
        }
    }

    private fun isFirstChildViewInParent(view:ViewGroup):Boolean{
        Log.d(BLUETOOTH_ACTIVITY,"isFirstChildViewInParent = ${parent.getChildAt(0)===view}")
        return parent.getChildAt(0)===view
    }

    /**
     * method overridden to know if user agreed to grant location permission
     */
    override fun onRequestPermissionsResult(requestCode: Int,permissions: Array<out String>,grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(grantResults[0]==PackageManager.PERMISSION_GRANTED){
            registerBluetoothReceiver()
            init()
        }
        else
            permissionNotGranted(LOCATION_DISABLED)
    }

    override fun onStart() {
        super.onStart()
        Log.d(BLUETOOTH_ACTIVITY,"On start method called")
        //if(Build.VERSION.SDK_INT>22 && ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED){
    }

    /**
     * called to start up bluetooth layout, animations and start the discovery process
     */
    private fun init(){
        if(Bluetooth.adapter==null)//no bluetooth adapter available //
        {
            var title = "No Bluetooth ${resources.getString(R.string.bluetooth_b)}"
            UtilityClass.showDialog(noBluetooth, title,
                INFO_DIALOG, supportFragmentManager,null, width,height)
            //dialog.setDialogSize(width,height)
        }
        else if(!Bluetooth.adapter.isEnabled){//bluetooth isn't enabled
            val intent:Intent=Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(intent,BLUETOOTH_STATE)
        }
        else{//bluetooth is available and enabled
            isInitCalled = true
            //we inflate the device scan layout next
            deviceScan = layoutInflater.inflate(R.layout.device_scan,parent,false) as ViewGroup
            deviceScan.findViewById<Button>(R.id.retry).setOnClickListener(View.OnClickListener { retryBluetoothDiscovery() })
            //var logo = UtilityClass.inflateLogoLayout(layoutInflater,parent)
            //deviceScan.findViewById<LinearLayout>(R.id.logo).addView(logo,0)//add logo layout to the device scan layout
            parent.removeAllViews()
            parent.addView(deviceScan)//add device scan to the bluetooth layout
            //animateSearch(deviceScan as ViewGroup)//animate search
            Log.d(BLUETOOTH_ACTIVITY,"Starting bluetooth discovery")
            //show progress dialog while we are scanning
            dialog = UtilityClass.showDialog("Bluetooth","Scanning for nearby devices", PROGRESS_DIALOG,supportFragmentManager,null,0,-1)
            var res = Bluetooth.adapter.startDiscovery()//Bluetooth.leScanner?.startScan(registerBluetoothLECallback())
            Log.d(BLUETOOTH_ACTIVITY,"Discovery started $res")
            cancelDiscovery()
        }
    }

    private fun retryBluetoothDiscovery(){
        btAdapter.clearAdapter()
        btDeviceHashMap = HashMap()
        dialog = UtilityClass.showDialog("Bluetooth","Scanning for nearby devices", PROGRESS_DIALOG,supportFragmentManager,null,0,-1)
        Bluetooth.adapter?.startDiscovery()
        cancelDiscovery()
    }

    /**
     * private method to cancel bluetooth discovery 12 secs after it's started
     */
    private fun cancelDiscovery(){
        Handler.handler.postDelayed(Runnable {
            Bluetooth.adapter?.cancelDiscovery()
        },15000)
    }

    /**
     * method is overridden to receive the result of bluetooth selection operation
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG,"media ${resultCode== Activity.RESULT_OK}")
        when(requestCode){
            BLUETOOTH_STATE->{
                if(resultCode== Activity.RESULT_OK){//bluetooth is enabled
                    init()
                }
                else
                    permissionNotGranted(BLUETOOTH_DISABLED)
            }
            50->{
                Log.d(TAG,"Media result ${data}")
            }
            else ->Log.d(TAG,"Medias result ${data}")
        }
    }

    /**
     * method to register receiver for bluetooth devices found.
     */
    private fun registerBluetoothReceiver(){
        if(!this::receiver.isInitialized){
            val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            filter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST)
            receiver = object: BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    when (intent.action) {
                        BluetoothDevice.ACTION_FOUND -> {
                            Log.d(TAG,"Pairing method = ${intent.getStringExtra(BluetoothDevice.EXTRA_PAIRING_VARIANT)}")

                            if (!this@BluetoothActivity::btRecyclerView.isInitialized) {
                                btRecyclerView = parent.findViewById<RecyclerView>(R.id.bt_items)
                                //btRecyclerView.visibility = RecyclerView.VISIBLE
                                btAdapter = BTListAdapter()
                                btRecyclerView.adapter = btAdapter
                                btRecyclerView.layoutManager =
                                    LinearLayoutManager(this@BluetoothActivity)
                            }
                            if (!isFirstChildViewInParent(deviceScan)) {
                                parent.removeAllViews()
                                parent.addView(deviceScan)
                            }
                            if (btRecyclerView.visibility == View.GONE) {
                                deviceScan.findViewById<TextView>(R.id.text).visibility = View.GONE
                                btRecyclerView.visibility = View.VISIBLE
                            }
                            if (!btDeviceHashMap.containsKey(device.address)) {
                                btAdapter.addBluetoothDevice(device)
                                btAdapter.notifyItemInserted(btDeviceHashMap.size)
                                btDeviceHashMap[device.address] = device
                            }
                        }
                        BluetoothAdapter.ACTION_DISCOVERY_FINISHED->{dialog?.dismiss();dialog = null}
                        BluetoothDevice.ACTION_PAIRING_REQUEST->{
                            val pairingVariant = intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_VARIANT,-1)
                            val pairingKey = intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_KEY,-1)
                            /*if(pairingKey!=-1)
                            {
                                device.setPin(ByteBuffer.allocate(4).putInt(pairingKey).array())
                                device.createBond()
                            }*/
                            Log.d(TAG,"Pairing Variant $pairingVariant")
                            Log.d(TAG,"Pairing Key $pairingKey")
                        }
                    }
                }
            }
            registerReceiver(receiver,filter)
            Log.d(BLUETOOTH_ACTIVITY,"bluetooth receiver registered")
        }
    }

    /**
     * method to register receiver for bluetooth device found.
     */
    private fun registerBluetoothLECallback():ScanCallback{
        if(!this::callback.isInitialized){
            callback = object: ScanCallback(){
                override fun onScanResult(callbackType: Int, result: ScanResult?) {
                    super.onScanResult(callbackType, result)
                    Log.d(BLUETOOTH_ACTIVITY,"Result received")
                    var device = result?.device
                     var uuid = UUID.nameUUIDFromBytes(result!!.scanRecord!!.bytes).toString();
                    when(device!!.bluetoothClass.deviceClass){
                        BluetoothClass.Device.AUDIO_VIDEO_UNCATEGORIZED->{
                            Log.d(BLUETOOTH_ACTIVITY,"name=${device.name} : addy=${device.address} : uuids=${device.uuids}: uuid=$uuid class=AUDIO_VIDEO_VIDEO_UNCATEGORIZED")
                        }
                        BluetoothClass.Device.AUDIO_VIDEO_VIDEO_MONITOR->{
                            Log.d(BLUETOOTH_ACTIVITY,"name=${device.name} : addy=${device.address} : uuids=${device.uuids}: uuid=$uuid class=AUDIO_VIDEO_VIDEO_MONITOR")
                        }
                        BluetoothClass.Device.COMPUTER_UNCATEGORIZED->{
                            Log.d(BLUETOOTH_ACTIVITY,"name=${device.name} : addy=${device.address} : uuids=${device.uuids}: uuid=$uuid class=COMPUTER_UNCATEGORIZED")
                        }
                        else->{
                            Log.d(BLUETOOTH_ACTIVITY,"name=${device.name} : addy=${device.address} : uuids=${device.uuids}: uuid=$uuid class=UNKNOWN ${device!!.bluetoothClass.deviceClass}")
                        }
                    }
                    //Log.d(BLUETOOTH_ACTIVITY,"name=${device?.name} addy=${device?.address} class=${ device!!.bluetoothClass.deviceClass} uuids=${UUIDx}")
                }
            }
            Log.d(BLUETOOTH_ACTIVITY,"bluetooth receiver registered")
        }
        return callback
    }

    /**
     * once the activity is stopped, we don't want to keep running the handler
     */
    override fun onStop() {
        super.onStop()
    }

    /**
     * override this method to unregister the receiver once the activity is done
     */
    override fun onDestroy() {
        super.onDestroy()
        if(this::receiver.isInitialized)
            unregisterReceiver(receiver)
    }

    /**
     * this method is overridden to show dialog letting the user know why certain permission is needed
     */
    override fun showRequestPermissionRationale(requestCode: Int) {
        var listener = object : DialogClickListener {
            override fun onClick() {
                requestPermissions(requestCode,arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION))
                Log.d(BLUETOOTH_ACTIVITY, "Dialog Button clicked. requesting permission")
            }
        }
        UtilityClass.showDialog("Location Access",
            "Location service is needed for your phone to detect nearby Canvass ready to be setup",
            INFO_DIALOG,supportFragmentManager,listener,width,height)
    }

    /**
     * convenience method for requesting  permissions
     */
    override fun requestPermissions(requestCode: Int,permissions: Array<String>) {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),20)
    }

    /**
     * method to show necessary UI components when either user has refused to grant bluetooth
     * or location permissions
     */
    override fun permissionNotGranted(feature: Int) {
        lateinit var layout:ViewGroup
        when(feature){
            BLUETOOTH_DISABLED->{
                if(!this::bluetoothDisabledLayout.isInitialized){//reflection to check if the lateinit value has been initialed or not
                    //var parent = findViewById<ViewGroup>(R.id.parent)
                    bluetoothDisabledLayout= layoutInflater.inflate(R.layout.bluetooth_disabled,parent,false) as ViewGroup
                    layout = bluetoothDisabledLayout
                }
            }
            LOCATION_DISABLED->{
                if(!this::locationDisabled.isInitialized){//reflection to check if the lateinit value has been initialed or not
                    //var parent = findViewById<ViewGroup>(R.id.parent)
                    locationDisabled= layoutInflater.inflate(R.layout.location_disabled,parent,false) as ViewGroup
                    layout = locationDisabled
                }
            }
        }
        if(isFirstChildViewInParent(layout)) {
            if(layout.visibility!=View.VISIBLE)
                layout.visibility=View.VISIBLE
        }
        else{
            parent.removeAllViews()
            parent.addView(layout);
            layout.visibility=View.VISIBLE
        }
    }

    override fun isPermissionGranted(feature:String): Boolean {
        return ContextCompat.checkSelfPermission(this,feature)!=PackageManager.PERMISSION_GRANTED
    }
}