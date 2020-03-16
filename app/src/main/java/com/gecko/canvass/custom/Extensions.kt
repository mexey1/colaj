package com.gecko.canvass.custom

import android.bluetooth.BluetoothDevice
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import com.gecko.canvass.utility.IEEE8021X
import com.gecko.canvass.utility.WEP
import com.gecko.canvass.utility.WPA
import com.gecko.canvass.utility.WPA2


    //var  t = BluetoothDevice.
    /**
     * extension to the WifiManager class to help check if a given scan result is an open network or
     * not.
     */
    fun WifiManager.isOpen(scanResult: ScanResult):Boolean{
        val capabilities = scanResult.capabilities
        if(capabilities.contains(WPA) || capabilities.contains(WPA2) || capabilities.contains(WEP) || capabilities.contains(IEEE8021X))
            return false
        return true
    }
