package com.gecko.canvass.interfaces

import org.json.JSONArray
import org.json.JSONObject

interface DialogClickListener {
    fun onClick(){}
    fun onClick(result:JSONObject){}
}