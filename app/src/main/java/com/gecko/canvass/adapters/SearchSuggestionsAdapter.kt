package com.gecko.canvass.adapters

import android.content.Context
import android.database.Cursor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cursoradapter.widget.CursorAdapter
import com.gecko.canvass.R
import com.gecko.canvass.custom.CustomLruCache
import com.gecko.canvass.utility.Constants

class SearchSuggestionsAdapter(context: Context,cursor:Cursor,flags:Int): CursorAdapter(context,cursor,flags) {

    override fun newView(context: Context, cursor: Cursor, parent: ViewGroup): View {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.suggestion_item,parent,false)
        return view
    }

    override fun bindView(view: View, context: Context, cursor: Cursor) {
        view.findViewById<TextView>(R.id.suggestion).text = cursor.getString(1)
    }
}