package com.gecko.canvass.adapters

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.gecko.canvass.R
import com.gecko.canvass.activity.WifiSelectionActivity
import com.gecko.canvass.custom.BTListHolder
import com.gecko.canvass.custom.DeviceScanListHolder
import com.gecko.canvass.logic.WifiSetup
import com.gecko.canvass.utility.UtilityClass
import org.json.JSONObject

class DeviceScanListAdapter : RecyclerView.Adapter<DeviceScanListHolder>() {
    private lateinit var backgroundColorList:Array<Drawable>
    private  val devices = ArrayList<JSONObject>()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceScanListHolder {
        if(!::backgroundColorList.isInitialized){
            val first = parent.context.resources.getDrawable(R.drawable.yellow_background_view,null)
            val second = parent.context.resources.getDrawable(R.drawable.pink_background_view,null)
            val third = parent.context.resources.getDrawable(R.drawable.purple_background_view,null)
            val fourth = parent.context.resources.getDrawable(R.drawable.turquoise_background_view,null)
            backgroundColorList = arrayOf(first,second,third,fourth)
        }
        val view = LayoutInflater.from(parent.context).inflate(R.layout.device_scan_list_item,parent,false)
        return DeviceScanListHolder(view)
    }

    override fun getItemCount(): Int {
        return devices.size
    }

    override fun onBindViewHolder(holder: DeviceScanListHolder, position: Int) {
        holder.deviceName.findViewById<TextView>(R.id.device_name).text = devices[position].getString("host")
        holder.icon.background = backgroundColorList[position%4]
        holder.deviceName.setOnClickListener(View.OnClickListener {
            val bundle = Bundle()
            //bundle.putString("device",devices[position].toString())
            WifiSetup.deviceToSetup = devices[position]
            UtilityClass.startActivity(holder.deviceName.context,WifiSelectionActivity::class.java,bundle)
        })
    }

    fun releaseAdapter(){
    }

    fun addDevice(json:JSONObject){
        devices.add(json)
        notifyItemInserted(devices.size-1)
    }

    fun clearDeviceList(){
        devices.clear()
        notifyDataSetChanged()
    }
}