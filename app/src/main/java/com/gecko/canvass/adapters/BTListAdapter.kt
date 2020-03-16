package com.gecko.canvass.adapters

import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gecko.canvass.R
import com.gecko.canvass.custom.BTListHolder
import com.gecko.canvass.logic.Bluetooth
import com.gecko.canvass.utility.*

class BTListAdapter : RecyclerView.Adapter<BTListHolder>(){
    private var btDevices = ArrayList<BluetoothDevice>()
    private lateinit var turquoise:Drawable
    private lateinit var yellow:Drawable
    private lateinit var pink:Drawable
    private lateinit var purple:Drawable
    private val drawablesHashMap = HashMap<Int,Drawable>()
    private var isInit = false
    override fun onBindViewHolder(holder: BTListHolder, position: Int) {
        //setIconBackground(holder,position)
        holder.icon.text = Constants.getIcon(btDevices[position].bluetoothClass.majorDeviceClass)
        holder.text.text = if(btDevices[position].name===null)"UNKNOWN" else btDevices[position].name
        holder.parent.setOnClickListener(null)
        holder.parent.setOnClickListener{
            if(Bluetooth.adapter !=null && Bluetooth.adapter.isDiscovering)
                Bluetooth.adapter.cancelDiscovery()
            Log.d("BTListAdapter","BT device ${btDevices[position].name} pressed")
            Log.d("BTListAdapter","current thread = ${Thread.currentThread().name}")
            ThreadPool.postTask(Runnable {
                //btDevices[position].createBond(0)
                Log.d("BTListAdapter","current thread = ${Thread.currentThread().name}")
                Bluetooth.pair(btDevices[position],holder.parent.context)
            })

        }
        Log.d("BTListAdapter","name= ${btDevices[position].name}")
    }

    /*private fun setIconBackground(holder: BTListHolder,position: Int){
        if(drawablesHashMap.containsKey(btDevices[position].bluetoothClass.majorDeviceClass))
            holder.icon.background = drawablesHashMap[btDevices[position].bluetoothClass.majorDeviceClass]
        else
            holder.icon.background = yellow
    }*/
    override fun getItemCount(): Int {
        return btDevices.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BTListHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.bt_item,parent,false)
        if(!isInit)
        {
            turquoise = parent.context.getDrawable(R.drawable.turquoise_background_view)!!
            yellow = parent.context.getDrawable(R.drawable.yellow_background_view)!!
            pink = parent.context.getDrawable(R.drawable.pink_background_view)!!
            purple = parent.context.getDrawable(R.drawable.purple_background_view)!!

            drawablesHashMap[BluetoothClass.Device.Major.AUDIO_VIDEO] = purple
            drawablesHashMap[BluetoothClass.Device.Major.COMPUTER] = turquoise
            drawablesHashMap[BluetoothClass.Device.Major.IMAGING] = yellow
            drawablesHashMap[BluetoothClass.Device.PHONE_SMART] = pink

            isInit = true
        }
        return BTListHolder(view)
    }

    fun addBluetoothDevice(btDevice: BluetoothDevice){
        btDevices.add(btDevice)
    }

    fun clearAdapter(){
        val size = btDevices.size
        btDevices.clear()
        notifyItemRangeRemoved(0,size)
    }
}