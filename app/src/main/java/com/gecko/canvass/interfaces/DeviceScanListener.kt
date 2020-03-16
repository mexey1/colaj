package com.gecko.canvass.interfaces

import org.json.JSONObject

interface DeviceScanListener {
    fun onProbeCompleted(response:JSONObject?)
}