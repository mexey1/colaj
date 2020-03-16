package com.gecko.canvass.custom

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.gecko.canvass.R

class DeviceScanListHolder(view: View) : RecyclerView.ViewHolder(view){
    var deviceName: TextView = view.findViewById<TextView>(R.id.device_name)
    var icon:FontAwesomeTextView = view.findViewById(R.id.icon)
}