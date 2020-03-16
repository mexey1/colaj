package com.gecko.canvass.custom

import android.view.View
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.gecko.canvass.R
import kotlinx.android.synthetic.main.search_result_item.view.*

class SearchResultsListHolder(view:View) : RecyclerView.ViewHolder(view){
    val image = view.findViewById<ImageView>(R.id.image)
}