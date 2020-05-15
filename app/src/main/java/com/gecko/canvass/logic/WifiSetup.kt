package com.gecko.canvass.logic

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.*
import android.os.Build
import android.os.CountDownTimer
import android.os.Looper
import android.os.NetworkOnMainThreadException
import android.text.TextUtils
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.gecko.canvass.activity.HomeActivity
import com.gecko.canvass.custom.CustomDialogFragment
import com.gecko.canvass.interfaces.CountDownListener
import com.gecko.canvass.utility.*
import org.json.JSONObject
import java.net.*
import java.util.concurrent.atomic.AtomicInteger


object WifiSetup {
    private val TAG  = "WifiSetup"
    private var isConnected = false
    private lateinit var ipvInet4Address: Inet4Address
    private lateinit var ipvInet6Address: Inet6Address
    private var isWifiPasswordCorrect = false
    private var progressDialog:CustomDialogFragment? = null
    private val wifiManager =
        UtilityClass.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager;
    private val connectivityManager = UtilityClass.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    var deviceToSetup:JSONObject? = null
    val openWifi:AtomicInteger = AtomicInteger()
    private val wifiAttempts = AtomicInteger()
    var countDownListener:CountDownListener? = null
    private var SSID = ""
    private var config:WifiConfiguration? = null
    private var isWifiDetailsSent = false

    /**
     * method called to connect to the wifi network identified by result. This method should not
     * be called from the UI thread
     */
    @Synchronized fun connectToWifiNetwork(result:ScanResult){
        try {
            Log.d(TAG,"Connecting to wifi called")
            if(Looper.myLooper() == Looper.getMainLooper())
                NetworkOnMainThreadException()
            wifiAttempts.set(0)
            Log.d(TAG,"Attempting to connect to ${result.SSID}")
            connect(result,null)
        }
        catch (e:java.lang.Exception){
            UtilityClass.sendMail(e)
            e.printStackTrace()
        }
    }

    /**
     * method called to connect to the wifi network identified by result and details contained in json. This method should not
     * be called from the UI thread.
     */
    @Synchronized fun connectToWifiNetwork(result:ScanResult,json: JSONObject){
        try{
            if(Looper.myLooper() == Looper.getMainLooper())
                NetworkOnMainThreadException()
            wifiAttempts.set(0)
            Handler.handler.post(Runnable {
                progressDialog = UtilityClass.showDialog("Checking Password","Verifying password provided is correct",
                    PROGRESS_DIALOG,(UtilityClass.context as AppCompatActivity).supportFragmentManager,null,0,-1)
            })
            Log.d(TAG,"Attempting to connect to ${result.SSID} with password $json")
            connect(result,json)
        }
        catch (e:java.lang.Exception){
            UtilityClass.sendMail(e)
            e.printStackTrace()
        }
    }

    /**
     * method called to connect to the open network identified by the ScanResult object passed.
     * This function would be called when connecting to the open wifi network to detect the
     * devices
     */
    private fun connect(scanResult: ScanResult?, json: JSONObject?){
        //do not remove try catch
        try {
            SSID = scanResult!!.SSID
            val password = if(json?.has("password") == true) json.getString("password") else ""
            config = getWifiConfig(scanResult,password,"")
            lateinit var networkRequest:NetworkRequest
            if(Build.VERSION.SDK_INT<29){
                //if password is empty and json is not null no need starting a countdown timer we'd just send the details to the
                //device.
                if(password.isEmpty()){
                    Log.d(TAG,"API Level<29 so, beginning to connect to $SSID ")
                    networkRequest = createNetworkRequest(false)
                    Log.d(TAG,"API Level<29 so, continuing to connect to $SSID ")
                    //connectivityManager.registerNetworkCallback(networkRequest, connectivityCallbackForDeviceScan())
                    //if json is not null, then we send the details to Colaj else this was called for
                    //wifi discovery
                    if(json is JSONObject)
                        sendWifiDetailsToColaj(json)
                }
                else{
                    Log.d(TAG,"API Level<29 so, beginning to connect to $SSID with password $password")
                    networkRequest = createNetworkRequest(true)
                    Log.d(TAG,"API Level<29 so, continuing to connect to $SSID ")
                    //connectivityManager.registerNetworkCallback(networkRequest, connectivityCallbackForWifiPasswordCheck())
                }
                connectUsingConfig(config!!)
                if(password.isEmpty())
                    sendMulticast()
                else
                    sendWifiDetailsToColaj(json)
            }
            else{
                Log.d(TAG,"API Level>29 so, beginning to connect to $SSID ")
                val security = UtilityClass.getWifiSecurityType(scanResult)
                val wifiNetworkSpecifier = WifiNetworkSpecifier.Builder().setSsid("$SSID").build()
                if(password.isEmpty()) {
                    networkRequest = createNetworkRequest(wifiNetworkSpecifier, false)
                    //connectivityManager.requestNetwork(networkRequest, connectivityCallbackForDeviceScan())
                    sendMulticast()
                    if(json is JSONObject)
                        sendWifiDetailsToColaj(json)
                }
                else {
                    networkRequest = createNetworkRequest(wifiNetworkSpecifier, true)
                    connectivityManager.requestNetwork(networkRequest, connectivityCallbackForWifiPasswordCheck())
                    sendWifiDetailsToColaj(json)
                }
            }
            Log.d(TAG,"Waiting for call back")
        }
        catch (e:Exception){
            UtilityClass.sendMail(e)
            e.printStackTrace()
        }
    }
    /**
     * we'd sleep for WIFI_PASSWORD_TIMER seconds and when we wake, we check if we are connected to the wifi network
     */
    private fun sendMulticast(){
        try {
            Thread.sleep(WIFI_PASSWORD_TIMER)
            if(isConnectedToWifi()){
                wifiAttempts.set(0)
                sendColajHello()
            }
            else if(wifiAttempts.incrementAndGet() < 3)
            {
                Log.d(TAG,"Connecting to wifi failed. Retrying... ${wifiAttempts.get()}")
                connectUsingConfig(config!!)
                sendMulticast()
            }
            else{
                Log.d(TAG,"Send multicast done")
                completedProcessingWifi()
            }
        }
        catch (e:java.lang.Exception){
            UtilityClass.sendMail(e)
            e.printStackTrace()
        }

    }

    /**
     * this method is called to connect to a wifi using the a config value
     */
    private fun connectUsingConfig(config:WifiConfiguration){
        try {
            //connectivityManager.registerNetworkCallback(networkRequest, connectivityCallbackForWifiPasswordCheck())
            var netID = wifiManager.addNetwork(config)
            UtilityClass.showToast("connecting to network $SSID with network ID $netID")
            if(netID==-1 && UtilityClass.hasWhiteSpace(SSID)){
                Log.d(TAG,"netID was -1. Attempting to connect by escaping white spaces")
                config.SSID = UtilityClass.convertToQuotedString(SSID,true)
                netID = wifiManager.addNetwork(config)
            }
            Log.d(TAG,"beginning to connect to $SSID with network id $netID")
            wifiManager.disconnect();
            wifiManager.enableNetwork(netID, true);
            wifiManager.reconnect();
        }
        catch (e:Exception){
            UtilityClass.sendMail(e)
            e.printStackTrace()
        }
    }

    private fun createNetworkRequest(requireInternet:Boolean):NetworkRequest{
        lateinit var value:NetworkRequest
        try {
            Log.d(TAG,"Getting IP Address")
            //this portion would only execute for API 29 and above so the suppresslint is only needed for compilation purposes
            value =  if(!requireInternet)
                NetworkRequest.Builder().removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build()
            else
                NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build()
        }
        catch (e:Exception){
            UtilityClass.sendMail(e)
            e.printStackTrace()
        }
        return value
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun createNetworkRequest(networkSpecifier: WifiNetworkSpecifier ?,requireInternet: Boolean):NetworkRequest{
        lateinit var value:NetworkRequest
        try {
            Log.d(TAG,"Getting IP Address")
            //this portion would only execute for API 29 and above so the suppresslint is only needed for compilation purposes
            value =  if(!requireInternet)
                NetworkRequest.Builder().removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).addTransportType(NetworkCapabilities.TRANSPORT_WIFI).setNetworkSpecifier(networkSpecifier).build()
            else
                NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).addTransportType(NetworkCapabilities.TRANSPORT_WIFI).setNetworkSpecifier(networkSpecifier).build()
        }
        catch (e:java.lang.Exception){
            UtilityClass.sendMail(e)
            e.printStackTrace()
        }
        return value
    }

    /**
     * method to check if the device is connected to internet through the wifi
     */
    private fun isConnectedToInternetThroughWifi():Boolean{
        val networkInfo = connectivityManager.activeNetworkInfo
        Log.d(TAG,"Network Info ${networkInfo?.type == ConnectivityManager.TYPE_WIFI}")
        return (networkInfo?.type == ConnectivityManager.TYPE_WIFI && isConnectedToWifi())
    }

    /**
     * method to check if the device is connected to a wifi network
     */
    private fun isConnectedToWifi():Boolean{
        var value = false
        try {
            val wifiInfo = wifiManager.connectionInfo
            Log.d(TAG,"Is wifi info null ${wifiInfo is WifiInfo}")
            Log.d(TAG,"Wifi Info is ${wifiInfo.ssid} && SSID is $SSID and IP is ${wifiInfo.ipAddress}")
            ipvInet4Address = Inet4Address.getByAddress(UtilityClass.convertIPToByteArray(wifiInfo.ipAddress)) as Inet4Address
            Log.d(TAG,"IP addy is ${ipvInet4Address.hostAddress}")
            //Thread.sleep(WIFI_PASSWORD_TIMER)
            value = (wifiInfo is WifiInfo && wifiInfo.ssid == UtilityClass.convertToQuotedString(SSID,false))// && wifiInfo.ipAddress!=0)
        }
        catch (e:Exception){
            e.printStackTrace()
            UtilityClass.sendMail(e)
        }
        return value
    }

    /**
     * creates a connection to the device being setup and writes the wifi details it should
     * connect to. This method is executed via the ThreadPool
     */
    private fun sendWifiDetailsToColaj(json: JSONObject?){
        Log.d(TAG,"The  Colaj is on this wifi network ${deviceToSetup?.getString("wifi")?:""}")
        val oldConfig = getWifiConfig(null,"",deviceToSetup?.getString("wifi")?:"")
        var dialog:CustomDialogFragment? = null
        fun sendData(){
            try {
                Thread.sleep(WIFI_PASSWORD_TIMER)
                SSID = deviceToSetup?.getString("wifi")?:""
                Log.d(TAG,"The  Colaj is on wifi $SSID")
                if(isConnectedToWifi()) {
                    Log.d(TAG, "Sending wifi details to ${deviceToSetup?.getString("ip")} $json")
                    val socket = Socket()
                    socket.connect(InetSocketAddress(InetAddress.getByName(deviceToSetup?.getString("ip")),
                        COLAJ_MULTICAST_PORT), SOCKET_TIMEOUT)
                    val outputStream = socket.getOutputStream()
                    outputStream.write(json.toString().toByteArray())
                    outputStream.flush()
                    outputStream.close()

                    connectUsingConfig(config!!)//connect to the wifi with internet
                    //give breather to connect to wifi with internet before we make calls to pexel
                    //without this delay, the device might not have connected to the internet
                    //and the connection to pexels would fail
                    Thread.sleep(WIFI_PASSWORD_TIMER*3)
                    //if we sent the data successfully to Colaj device, then we can start the HomeActivity
                    Handler.handler.post(Runnable {
                        dialog?.dismiss()
                        UtilityClass.startActivity(UtilityClass.context!!, HomeActivity::class.java)
                    })
                    //save to shared preferences that we setup the device successfully
                    UtilityClass.writeBooleanSharedPreference(COLAJ_SETUP_SUCCESSFUL,true)
                }
                //we have three attempts to send the wifi credentials to the Colaj device
                else if(wifiAttempts.incrementAndGet()<3)
                {
                    Log.d(TAG,"Attempt to connect to wifi failed during sending wifi details to Colaj..Retrying ${wifiAttempts.get()}")
                    //connect to the wifi with the colaj device
                    connectUsingConfig(oldConfig!!)
                    sendData()
                }
                else
                    Handler.handler.post(Runnable {
                        dialog?.dismiss()
                        UtilityClass.showDialog("Wifi Password","Could not connect to device ${deviceToSetup?.getString("host")}. Ensure device is on.",
                            INFO_DIALOG,(UtilityClass.context as AppCompatActivity).supportFragmentManager,null,0,-1)
                    })
            }
            catch (e:java.lang.Exception){
                e.printStackTrace()
                Handler.handler.post(Runnable {
                    dialog?.dismiss()
                    UtilityClass.showDialog("Failed to connect","Could not connect to device ${deviceToSetup?.getString("host")}. Ensure device is on.",
                        INFO_DIALOG,(UtilityClass.context as AppCompatActivity).supportFragmentManager,null,0,-1)
                })
            }
        }

        if(json is JSONObject){
            if(json.has("password")){
                wifiAttempts.set(0)
                Handler.handler.postDelayed(Runnable {
                    progressDialog?.dismiss()
                    //if when countdown is completed and we are connected to a wifi network, then the password provided
                    //was correct, else it wasn't'
                    //if(isConnectedToInternetThroughWifi())
                    if(isConnectedToWifi())
                    {
                        Log.d(TAG, "Connect to server and write the details $json")
                        ThreadPool.postTask(Runnable { sendData() })
                        dialog = UtilityClass.showDialog("Connecting","Sharing WiFi credentials with your device",
                            PROGRESS_DIALOG,(UtilityClass.context as AppCompatActivity).supportFragmentManager,null,0,-1)
                    }
                    else
                        UtilityClass.showDialog("Wifi Password","Password entered isn't correct",
                            INFO_DIALOG,(UtilityClass.context as AppCompatActivity).supportFragmentManager,null,0,-1)
                }, WIFI_PASSWORD_TIMER)
            }
            else
                sendData()
        }
    }

    private fun connectivityCallbackForWifiPasswordCheck():ConnectivityManager.NetworkCallback{
        return object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                Log.d(TAG,"Wifi Password Correct")
                isWifiPasswordCorrect = true
            }
            override fun onUnavailable() {
                super.onUnavailable()
                isWifiPasswordCorrect = false
                Log.d(TAG,"Wifi Password Incorrect")
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                Log.d(TAG,"Wifi Lost")
            }

            override fun onLosing(network: Network, maxMsToLive: Int) {
                super.onLosing(network, maxMsToLive)
                Log.d(TAG,"Wifi Losing")
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                super.onCapabilitiesChanged(network, networkCapabilities)
                Log.d(TAG,"Wifi Capabilities Changed")
            }
        }
    }

    /**
     * private method to send a packet to the multicast group. The colaj devices would be listening here
     *
    private fun connectToIPV4Multicast(){
        Log.d(TAG,"Sending packet")
        val jsonObject = JSONObject()
        jsonObject.put("ip", ipvInet4Address.toString().replace('/',' ',false).trim())
        jsonObject.put("app", APP_NAME)
        val multicastAddress = InetAddress.getByName(COLAJ_IPV4_MULTICAST_ADDR)
        val multicastSocket = MulticastSocket(COLAJ_MULTICAST_PORT)
        val jsonByteArray = jsonObject.toString().toByteArray()
        val packet = DatagramPacket(jsonByteArray,jsonByteArray.size,multicastAddress,COLAJ_MULTICAST_PORT)
        fun sendMulticastPacket(){
            multicastSocket.send(packet)
            Thread.sleep(1000)
            Log.d(TAG,"Packet sent")
        }
        //we'd send the packet 3 times with a delay of 1 second in-between
        sendMulticastPacket()
        sendMulticastPacket()
        sendMulticastPacket()
        completedProcessingWifi()
    }*/

    //do not remove try catch
    private fun sendColajHello(){
        var value: Exception? =  null
        lateinit var socket:Socket
        try {
            Log.d(TAG,"Sending Colaj hello")
            if(wifiAttempts.incrementAndGet()<3){
                val jsonObject = JSONObject()
                jsonObject.put("ip", ipvInet4Address.toString().replace('/',' ',false).trim())
                jsonObject.put("action", "hello")
                val socket = Socket()
                socket.connect(InetSocketAddress(COLAJ_IPV4__ADDR, COLAJ_MULTICAST_PORT), SOCKET_TIMEOUT)
                //write the hello message
                val outStream = socket.getOutputStream()
                outStream.write(jsonObject.toString().toByteArray())
                outStream.flush()
                outStream.close()
                //we'd wait for some time before disconnecting from this network to look for other
                //devices. This gives the colaj device some time to respond with details
                Thread.sleep(WIFI_PASSWORD_TIMER)
            }
        }
        //if there was a socket timeout while connecting, we'd retry again
        catch (e:Exception){
            value = e
            e.printStackTrace()
            sendColajHello()
        }
        finally {
            wifiAttempts.set(0)
            completedProcessingWifi()
            if(value !=null)
                UtilityClass.sendMail(value)
        }
    }

    /**
     * this method is called when we have successfully scanned a wifi network for available
     * colaj device
     */
    private fun completedProcessingWifi(){
        Log.d(TAG,"Counter is ${openWifi.get()}")
        if(openWifi.get()>0 && openWifi.decrementAndGet() == 0)
            countDownListener?.onFinish()
    }

    private fun getWifiConfig(scanResult: ScanResult?, password: String,ssid:String): WifiConfiguration? {
        val config = WifiConfiguration()
        try {
            config.SSID = UtilityClass.convertToQuotedString(scanResult?.SSID ?: ssid,false)
            config.status = WifiConfiguration.Status.ENABLED;
            Log.d(TAG,"Getting config for ${UtilityClass.convertToQuotedString(scanResult?.SSID ?: ssid,false)}")
            Log.d(TAG,"Getting security for ${UtilityClass.getWifiSecurityType(scanResult)}")
            when (val security: Int = UtilityClass.getWifiSecurityType(scanResult)) {
                SECURITY_NONE -> config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
                SECURITY_WEP -> {
                    Log.d(TAG,"WIFI is WEP")
                    config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
                    config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN)
                    config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED)
                    if (!TextUtils.isEmpty(password)) {
                        val length = password.length
                        // WEP-40, WEP-104, and 256-bit WEP (WEP-232?)
                        if ((length == 10 || length == 26 || length == 58) && password.matches(Regex("[0-9A-Fa-f]*")))
                            config.wepKeys[0] = password
                        else
                            config.wepKeys[0] = '"' + password + '"'
                    }
                }
                SECURITY_PSK -> {
                    Log.d(TAG,"WIFI is PSK")
                    config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK)
                    if (!TextUtils.isEmpty(password)) {
                        if (password.matches(Regex("[0-9A-Fa-f]{64}"))){
                            config.preSharedKey = password
                            Log.d(TAG,"plain password")
                        }
                        else {
                            val password = '"'+ password + '"'
                            config.preSharedKey = password
                            Log.d(TAG," password $password")
                        }
                        //config.preSharedKey = '"'+ password + '"'
                    }
                }
                SECURITY_EAP, SECURITY_EAP_SUITE_B -> {
                    Log.d(TAG,"WIFI is SECURITY_EAP")
                    config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_EAP)
                    config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.IEEE8021X)
                    if (security == SECURITY_EAP_SUITE_B) {
                        config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.SUITE_B_192)
                        //config.requirePMF = true
                        config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.GCMP_256)
                        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.GCMP_256)
                        //config.allowedGroupManagementCiphers.set(WifiConfiguration.GroupMgmtCipher.BIP_GMAC_256)
                        // allowedSuiteBCiphers will be set according to certificate type
                    }
                    if (!TextUtils.isEmpty(password))
                        config.enterpriseConfig.password = password
                }
                SECURITY_SAE -> {
                    Log.d(TAG,"WIFI is SAE")
                    config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.SAE)
                    //config.requirePMF = true
                    if (!TextUtils.isEmpty(password))
                        config.preSharedKey = '"'.toString() + password + '"'
                }
                SECURITY_OWE -> {
                    Log.d(TAG,"WIFI is OWE")
                    config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.OWE)
                    //config.requirePMF = true
                }
                else -> {
                }
            }
        }
        catch (e:Exception){
            UtilityClass.sendMail(e)
            e.printStackTrace()
        }
        return config
    }
}