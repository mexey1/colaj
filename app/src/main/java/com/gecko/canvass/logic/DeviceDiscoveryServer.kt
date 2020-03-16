package com.gecko.canvass.logic

import android.util.Log
import com.gecko.canvass.activity.DeviceScanActivity
import com.gecko.canvass.interfaces.DeviceScanListener
import com.gecko.canvass.utility.COLAJ_MULTICAST_PORT
import com.gecko.canvass.utility.ThreadPool
import org.json.JSONObject
import java.lang.StringBuilder
import java.net.ServerSocket
import java.net.Socket

object DeviceDiscoveryServer {
    private var  serverSocket: ServerSocket? = null
    private var shouldAcceptConnection = true
    private lateinit var deviceScanActivity: DeviceScanActivity
    private lateinit var deviceScanListener: DeviceScanListener
    private const val TAG = "DeviceDiscoveryServer"
    private val ipHashMap = HashMap<String,Int>()

    /**
     * method called to initialize a server socket object for receiving data from available colaj
     * device
     */
    fun acceptConnection(deviceScanActivity: DeviceScanActivity){
        ipHashMap.clear()
        this.deviceScanActivity = deviceScanActivity
        deviceScanListener = deviceScanActivity
        if(serverSocket == null || serverSocket?.isClosed == true){
            Log.d(TAG,"Awaiting TCP connection")
            serverSocket = ServerSocket(COLAJ_MULTICAST_PORT)
            ThreadPool.postTask(Runnable {
                while (shouldAcceptConnection){
                    try {
                        val socket = serverSocket!!.accept()
                        Log.d(TAG,"Connection received. Processing")
                        processConnection(socket)
                    }
                    catch (e:Exception){
                        e.printStackTrace()
                    }
                }
            })
        }
    }

    private fun processConnection(socket: Socket){
        ThreadPool.postTask(Runnable {
            val inputStream = socket.getInputStream()
            val data = ByteArray(8*1024)
            val resultBuilder = StringBuilder()
            var size = 0
            while (true){
                size = inputStream.read(data,0,data.size)
                if(size == -1)
                    break
                resultBuilder.append(String(data,0,size))
            }
            val response = JSONObject(resultBuilder.toString())
            Log.d(TAG,"Response from Colaj: $response")
            if(response.has("host") && !ipHashMap.containsKey(response.getString("ip"))){
                deviceScanListener.onProbeCompleted(response)
                ipHashMap[response.getString("ip")] = 0
            }
            socket.close()
        })
    }

    fun closeConnection(){
        serverSocket!!.close()
        shouldAcceptConnection = false
        serverSocket = null
    }
}