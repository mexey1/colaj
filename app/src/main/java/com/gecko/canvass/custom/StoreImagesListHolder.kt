package com.gecko.canvass.custom

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.gecko.canvass.R

class StoreImagesListHolder(view:View) : RecyclerView.ViewHolder(view){
    var layoutOne  = view.findViewById(R.id.layout_one) as LinearLayout
    var layoutTwo  = view.findViewById(R.id.layout_two) as LinearLayout
    var layoutThree  = view.findViewById(R.id.layout_three) as LinearLayout

    fun enableLayout(layout:Int):ViewGroup{
        lateinit var enabledLayout:LinearLayout
        layoutOne.visibility = if(layout == 0) {enabledLayout=layoutOne;View.VISIBLE} else View.GONE
        layoutTwo.visibility = if(layout == 1) {enabledLayout=layoutTwo;View.VISIBLE} else View.GONE
        layoutThree.visibility = if(layout == 2) {enabledLayout=layoutThree;View.VISIBLE} else View.GONE

        return enabledLayout
    }
    //var progress = view.findViewById(R.id.progress) as LinearLayout
}