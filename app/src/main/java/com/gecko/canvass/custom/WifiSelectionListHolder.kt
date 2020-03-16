package com.gecko.canvass.custom

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.gecko.canvass.R

class WifiSelectionListHolder(view: View) :RecyclerView.ViewHolder(view){
    var wifiName  = view.findViewById(R.id.wifi_name) as TextView
    var lock = view.findViewById(R.id.lock) as FontAwesomeTextView
    var wifi = view.findViewById(R.id.wifi) as FontAwesomeTextView
    var parent = view.findViewById(R.id.parent) as ViewGroup
}