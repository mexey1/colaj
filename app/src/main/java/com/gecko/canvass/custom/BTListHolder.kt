package com.gecko.canvass.custom

import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.gecko.canvass.R

class BTListHolder(view: View) : RecyclerView.ViewHolder(view){
     var icon  = view.findViewById(R.id.icon) as FontAwesomeTextView
     var text = view.findViewById(R.id.text) as TextView
     var parent = view.findViewById(R.id.parent) as LinearLayout
}