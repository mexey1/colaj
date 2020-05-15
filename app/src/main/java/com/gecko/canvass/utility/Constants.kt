package com.gecko.canvass.utility

import android.bluetooth.BluetoothClass


const val BLUETOOTH_STATE:Int = 5
const val BLUETOOTH_ACTIVITY = "BLUETOOTH_ACT"
const val COLAJ_UUID = "2d266186-01fb-47c2-8d9f-10b8ec891363"//"2D266186-01FB-47C2-8D9F-10B8EC891363"
const val COLAJ_NETWORK_NAME_UUID="12345678-02FB-47C2-8D9F-10B8EC891363";//network name gatt characteristic UUID
const val COLAJ_PASSWORD_UUID="12345679-02FB-47C2-8D9F-10B8EC891363"//password gatt characteristic UUID
const val COLAJ_NETWORK_STATUS_UUID="12345670-02FB-47C2-8D9F-10B8EC891363"//connection status of the colaj device
const val CUSTOM_DIALOG = "CUSTOM_DIALOG"
const val REQUESTED_BLUETOOTH="REQUESTED_BLUETOOTH"
const val COLAJ_SETUP_SUCCESSFUL="COLAJ_SETUP_SUCCESSFUL"
const val TV = "\uf26c"
const val PHONE = "\uf10b"
const val UNKNOWN = "\uf071"
const val IMAGE = "\uf1c5"
const val COMPUTER="\uf109"
const val WIFI="\uf1eb"
const val LOCK = "\uf023"
const val BLUETOOTH_DISABLED=1
const val LOCATION_DISABLED=2
const val WIFI_DISABLED = 3
const val LOCATION_REQUEST_CODE=20
const val WIFI_REQUEST_CODE = 30
const val BT_CONNECT_TIMEOUT=30000L
// Constants used for different security types
const val WPA2 = "WPA2"
const val WPA = "WPA"
const val WEP = "WEP"
const val OPEN = "Open"
const val IEEE8021X = "IEEE8021X"
const val INFO_DIALOG=1
const val INPUT_DIALOG=2
const val PROGRESS_DIALOG=3
const val PEXEL_API_KEY="563492ad6f91700001000001f29718be0ba544fc8f4f0a54167c2004"
const val PEXEL_URL="https://api.pexels.com/v1/"
const val DATA_MUSE_URL="https://api.datamuse.com/"
const val PAGE_COUNT=30
const val THUMBNAIL_DIR="thumbnail"
const val FULL_SIZED_IMAGES_DIR="images"
const val IMAGES_FROM_SEARCH_DIR="search"
const val COLAJ_MULTICAST_PORT = 50000
const val SOCKET_TIMEOUT=3000
const val COLAJ_IPV4_MULTICAST_ADDR="230.0.2.12"
const val COLAJ_IPV4__ADDR="192.168.4.1"//"10.42.0.2"
const val APP_NAME="COLAJ"
const val WIFI_DISCOVERY_TIMER=5000L
const val WIFI_PASSWORD_TIMER=3000L
const val PASSWORD_MIN_LENGTH = 8
const val PASSWORD_MAX_LENGTH = 63
const val IMAGE_LOADER_CURATE_MODE=0
const val IMAGE_LOADER_SEARCH_MODE=1

const val SECURITY_NONE = 0
const val SECURITY_WEP = 1
const val SECURITY_PSK = 2
const val SECURITY_EAP = 3
const val SECURITY_OWE = 4
const val SECURITY_SAE = 5
const val SECURITY_EAP_SUITE_B = 6
const val SECURITY_PSK_SAE_TRANSITION = 7
const val SECURITY_OWE_TRANSITION = 8
const val SECURITY_MAX_VAL = 9 // Has to be the last


object Constants{
    private var deviceType:HashMap<Int,String> = HashMap()
    init {
        deviceType[BluetoothClass.Device.Major.AUDIO_VIDEO] = TV
        deviceType[BluetoothClass.Device.Major.COMPUTER] = COMPUTER
        deviceType[BluetoothClass.Device.Major.IMAGING] = IMAGE
        deviceType[BluetoothClass.Device.PHONE_SMART]= PHONE
    }

    fun getIcon(int: Int):String{
        return if(deviceType.containsKey(int)) deviceType[int]!! else UNKNOWN
    }

    fun getLruCacheSize():Int{
        val LRU_CACHE_SIZE = (Runtime.getRuntime().maxMemory()/1024).toInt()/8
        return LRU_CACHE_SIZE
    }
}
