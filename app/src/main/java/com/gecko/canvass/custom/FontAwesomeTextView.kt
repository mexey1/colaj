package com.gecko.canvass.custom

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.widget.TextView
import com.gecko.canvass.utility.UtilityClass

class FontAwesomeTextView: androidx.appcompat.widget.AppCompatTextView {

    init {

        var font = UtilityClass.getFontAwesomeTypeface(context)
        Log.d("Context val","$font")
        typeface = font
    }

    constructor(context: Context, attrs:AttributeSet, defStyleAttr:Int) : super(context,attrs,defStyleAttr){
        typeface = UtilityClass.getFontAwesomeTypeface(context)
    }
    constructor(context: Context):super(context){
        typeface = UtilityClass.getFontAwesomeTypeface(context)
    }
    constructor(context: Context,attrs: AttributeSet):super(context,attrs){
        typeface = UtilityClass.getFontAwesomeTypeface(context)
    }
}