package com.gecko.canvass.logic

import android.bluetooth.*
import android.bluetooth.le.BluetoothLeScanner
import android.content.Context
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import com.gecko.canvass.R
import com.gecko.canvass.activity.HomeActivity
import com.gecko.canvass.activity.WifiSelectionActivity
import com.gecko.canvass.custom.CustomDialogFragment
import com.gecko.canvass.utility.*
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.util.*


object BluetoothLE {
    val leScanner: BluetoothLeScanner? = BluetoothAdapter.getDefaultAdapter().bluetoothLeScanner
    const val TAG = "BluetoothLE"
    private lateinit var btGatt: BluetoothGatt
    private var gattService: BluetoothGattService? = null
    private var networkCharacteristic: BluetoothGattCharacteristic? = null
    private var passwordCharacteristic: BluetoothGattCharacteristic? = null
    private var connectionStatusCharacteristic: BluetoothGattCharacteristic? = null
    private lateinit var dialog:CustomDialogFragment
    private var ssidWrite = false
    private var passWrite = false
    private lateinit var wifi:String
    private lateinit var password:String
    private lateinit var title:String
    private lateinit var message:String
    private  var colajStatus:String = ""
    private lateinit var context:WeakReference<Context>

    fun pair(device: BluetoothDevice, context: Context){
        try {
            this.context = WeakReference(context)
            device.createBond()
            btGatt =  device.connectGatt(context,false, getBTGattCallback())
            Log.d(TAG,"gatt object is $btGatt")
        }
        catch (e:Exception){
            e.printStackTrace()
        }
    }

    /**
     * method to instantiate gatt callback object
     */
    private fun getBTGattCallback():BluetoothGattCallback{
        return object :BluetoothGattCallback(){
            override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                Log.d(TAG,"service was discovered $gatt")
                when(status){
                    BluetoothGatt.GATT_FAILURE->{
                        Log.d(TAG,"Service discovery failed")
                    }
                    BluetoothGatt.GATT_SUCCESS->{
                        Log.d(TAG,"Service discovery successful")
                        //once we have connected, we need to get the service object and then the characteristics of the service
                        gattService = gatt?.getService(UUID.fromString(COLAJ_UUID))
                        networkCharacteristic = gattService?.getCharacteristic(UUID.fromString(COLAJ_NETWORK_NAME_UUID))
                        passwordCharacteristic = gattService?.getCharacteristic(UUID.fromString(COLAJ_PASSWORD_UUID))
                        connectionStatusCharacteristic = gattService?.getCharacteristic(UUID.fromString(COLAJ_NETWORK_STATUS_UUID))
                        //let's enable notifications for network status characteristic
                        enableNotification(gatt)
                        startWifiActivity(UtilityClass.context!!)
                        //gattCharacteristic?.setValue("NETWORK52")
                        //btGatt.writeCharacteristic(gattCharacteristic)
                    }
                }
            }

            override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                when(newState){
                    BluetoothProfile.STATE_CONNECTED->{
                        Log.d(TAG,"Device connected")
                        gatt?.discoverServices()
                    }
                    BluetoothProfile.STATE_DISCONNECTED->{
                        Log.d(TAG,"Device disconnected")
                    }
                }
            }

            override fun onCharacteristicChanged(gatt: BluetoothGatt?,characteristic: BluetoothGattCharacteristic?) {
                //once we get the notification that the connection status characteristic has changed,
                //we can then read the data
                gatt?.readCharacteristic(characteristic)
                Log.d(TAG,"Characteristic has changed")
            }

            override fun onCharacteristicWrite(
                gatt: BluetoothGatt?,
                characteristic: BluetoothGattCharacteristic?,
                status: Int
            ) {
                when(status){
                    BluetoothGatt.GATT_FAILURE->{
                        Log.d(TAG,"Write failed")
                    }
                    BluetoothGatt.GATT_SUCCESS->{
                        if(!ssidWrite){
                            ssidWrite = true
                            passwordCharacteristic?.setValue(password)
                            btGatt.writeCharacteristic(passwordCharacteristic)
                            updateDialog(title,message)
                        }
                        else if(!passWrite){
                            ssidWrite = false
                            passWrite = false
                        }
                        Log.d(TAG,"Write was successful")
                    }
                }
            }

            override fun onCharacteristicRead(
                gatt: BluetoothGatt?,
                characteristic: BluetoothGattCharacteristic?,
                status: Int
            ) {
                when(status){
                    BluetoothGatt.GATT_SUCCESS->{
                        //when we read the characteristic, we can notify the user
                        colajStatus = characteristic?.getStringValue(0)?:""
                        Log.d(TAG,"Status is $colajStatus")
                        when(colajStatus){
                            "200"->{
                                dialog?.dismiss()
                                UtilityClass.showToast("WiFi connection was established successfully")
                                UtilityClass.startActivity(UtilityClass.context!!, HomeActivity::class.java)
                                //save to shared preferences that we setup the device successfully
                                UtilityClass.writeBooleanSharedPreference(COLAJ_SETUP_SUCCESSFUL,true)
                            }
                            "201"->{
                                val title = "Error"
                                val message = "Error occurred while connecting to your network. Kindly ensure password provided is correct and try again"
                                showUpdate(title=title,message = message)
                            }
                            "202"->{
                                val title = "Error"
                                val message = "Ensure your WiFi name and password do not contain spaces"
                                showUpdate(title=title,message = message)
                            }
                            else -> {
                                val title = "Error"
                                val message = "Error occurred while reading response from your Colaj"
                                showUpdate(title=title,message = message)
                            }

                        }
                        Log.d(TAG,"Read was successful")
                    }
                }
            }
        }
    }

    /**
     * method called to allow notification on changes to the network status characteristic on the
     * remote device
     */
    private fun enableNotification(gatt: BluetoothGatt?)
    {
        //let's enable notifications for network status characteristic

        val uuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        val descriptor: BluetoothGattDescriptor? = connectionStatusCharacteristic?.getDescriptor(uuid)
        Log.d(TAG,"descriptor vale $descriptor")
        descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        gatt?.writeDescriptor(descriptor)
        Log.d(TAG,"Notification enabled ${gatt?.setCharacteristicNotification(connectionStatusCharacteristic,true)}")
    }

    /**
     * private method to start wifi activity
     */
    private fun startWifiActivity(context: Context){
        Handler.handler.post(Runnable {
            UtilityClass.startActivity(context, WifiSelectionActivity::class.java)
        })
    }

    private fun showUpdate(title: String,message:String){
        Handler.handler.post(Runnable {
            dialog?.dismiss()
            val fragmentManager = (context.get() as AppCompatActivity).supportFragmentManager
            UtilityClass.showDialog(title,message,
                INFO_DIALOG, fragmentManager,null,0,-1)
        })
    }

    /**
     * method called to send message to the connected bluetooth device. This method can be called
     * on the UI thread as it spawns it's own thread to make network related calls.
     */
    fun sendMessage(dialogFragment: CustomDialogFragment, string: JSONObject){
        dialog = dialogFragment
        //let's strip the contents of the JSON object and write them to the device
        ThreadPool.postTask(Runnable {
            wifi = string.getString("wifi")
            password = string.getString("password")
            Log.d(TAG,"WiFi credentials are wifi=$wifi and password is $password")
            title = "Connecting to WiFI"
            message ="Please wait while your device connects to your WiFi network"
            networkCharacteristic?.setValue(wifi)
            btGatt.writeCharacteristic(networkCharacteristic)
        })
    }

    /**
     * method to update the contents of the progress dialog
     */
    private fun updateDialog(title:String,description:String){
        Handler.handler.post(Runnable {
            dialog.view?.findViewById<TextView>(R.id.title)?.text = title
            dialog.view?.findViewById<TextView>(R.id.message)?.text = description
        })
    }
}