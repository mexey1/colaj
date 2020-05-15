package com.gecko.canvass.adapters

import android.annotation.SuppressLint
import android.net.wifi.ScanResult
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.gecko.canvass.R
import com.gecko.canvass.activity.HomeActivity
import com.gecko.canvass.custom.WifiSelectionListHolder
import com.gecko.canvass.interfaces.DialogClickListener
import com.gecko.canvass.logic.Bluetooth
import com.gecko.canvass.logic.BluetoothLE
import com.gecko.canvass.logic.WifiSetup
import com.gecko.canvass.utility.*
import org.json.JSONObject

class WifiSelectionListAdapter: RecyclerView.Adapter<WifiSelectionListHolder>() {
    private var wifiNetworks= ArrayList<ScanResult>()
    private val wifiAccessState = HashMap<ScanResult,Boolean>()
    private var current:View? = null
    private var selectedPosition = -1
    private val TAG = "WifiSelectionListAdapter"
    //private lateinit var contex:Context

    /*fun setContext(con:Context)
    {
        this.contex=con
    }*/

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WifiSelectionListHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.wifi_item,parent,false)
        return WifiSelectionListHolder(view)
    }

    override fun getItemCount(): Int {
        return wifiNetworks.size
    }

    override fun onBindViewHolder(holderSelection: WifiSelectionListHolder, position: Int) {
        holderSelection.wifiName.text = wifiNetworks[position].SSID
        holderSelection.lock.text = LOCK
        holderSelection.wifi.text = WIFI
        holderSelection.parent.setOnClickListener(View.OnClickListener {
            current?.isSelected=false
            it.isSelected = true
            current = it
            selectedPosition = position
            if(!wifiAccessState[wifiNetworks[selectedPosition]]!!)//if wifi is locked
                createInputDialog()
            else
                UtilityClass.startActivity(current!!.context,HomeActivity::class.java)//this code to be modified to communicate with the WifiSetup class
        })
        holderSelection.parent.isSelected = position==selectedPosition
        holderSelection.lock.visibility = if(!wifiAccessState[wifiNetworks[position]]!!) View.VISIBLE else View.INVISIBLE
    }

    /**
     * method called to add a wifi-network to the list of networks backing the view
     */
    fun addWifiNetwork(wifi: ScanResult, open: Boolean){
        wifiNetworks.add(wifi)
        wifiAccessState[wifi] = open
    }

    /**
     * this method is called from the WifiSelectionActivity class when it's about to be destroyed
     */
    fun releaseAdapter(){
        current = null
    }

    /**
     * if wifi is locked, display dialog for user to provide password
     */
    private fun createInputDialog(){
        val fragmentManager = (current!!.context as AppCompatActivity).supportFragmentManager
        val listener = object: DialogClickListener{
            @SuppressLint("LongLogTag")
            override fun onClick(result: JSONObject) {
                super.onClick(result)
                result.put("action","wifiCred")
                result.put("wifi",wifiNetworks[selectedPosition].SSID)
                Log.d(TAG,"Connecting to ${wifiNetworks[selectedPosition].SSID}")
                //WifiSetup.connectToWifiNetwork should not be run on UI Thread
                /*ThreadPool.postTask(Runnable {
                    Log.d(TAG,"Running runnable")
                    //WifiSetup.connectToWifiNetwork(wifiNetworks[selectedPosition],result)
                })*/
                //val dialog = UtilityClass.showDialog("Verifying WiFi Password",
                // "Please wait while we verify the WiFi credentials", PROGRESS_DIALOG,fragmentManager,null,0,-1)
                val dialog = UtilityClass.showDialog("Sharing credentials",
                     "Please wait while we share the WiFi credentials with your device", PROGRESS_DIALOG,fragmentManager,null,0,-1)
                //Bluetooth.sendMessage(dialog,result)
                BluetoothLE.sendMessage(dialog,result)
                //UtilityClass.startActivity(current!!.context,HomeActivity::class.java)
            }
        }
        val title = wifiNetworks[selectedPosition].SSID

        //val height = (current!!.context as AppCompatActivity).resources.getDimension(R.dimen._500dp).toInt()
        UtilityClass.showDialog(title = title,listener = listener, dialogType = INPUT_DIALOG, fragmentManager = fragmentManager, width = 0,height = -1,
            message = "Enter the password to $title")
    }
}