package com.gecko.canvass.logic

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.bluetooth.le.BluetoothLeScanner
import android.content.Context
import android.net.Uri
import android.os.ParcelUuid
import android.util.Log
import android.widget.Toast
import com.gecko.canvass.activity.WifiSelectionActivity
import com.gecko.canvass.custom.FallbackBluetoothSocket
import com.gecko.canvass.exceptions.FallbackException
import com.gecko.canvass.utility.BT_CONNECT_TIMEOUT
import com.gecko.canvass.utility.Handler
import com.gecko.canvass.utility.ThreadPool
import com.gecko.canvass.utility.UtilityClass
import org.json.JSONObject
import java.io.*
import java.lang.Exception
import java.util.concurrent.atomic.AtomicReference

object Bluetooth {
    val adapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothSocket:BluetoothSocket? = null
    var leScanner:BluetoothLeScanner? = BluetoothAdapter.getDefaultAdapter().bluetoothLeScanner
    //@Volatile private var btSocketHashMap = HashMap<Int,BluetoothSocket>()
    private var isBTInUse = false
    private val TAG = "Bluetooth"
    //private lateinit var context:Context//this code is only needed for testing
    //private lateinit var btSocket:BluetoothSocket
    private lateinit var url:Uri
    private var count=0
    private lateinit var jsonObject:JSONObject

    /**
     * method called to pair the local device with the remote device.
     */
    fun pair(device: BluetoothDevice,context: Context){
        if(!isBTInUse)//when bt isn't in use, we can proceed
        {
            //this.context = context
            lateinit var btSocket: BluetoothSocket
            //lateinit var atomicReference: AtomicReference<BluetoothSocket>
            try {
                isBTInUse = true
                bluetoothSocket?.close()
                btSocket = device.createRfcommSocketToServiceRecord((ParcelUuid.fromString("00001105-0000-1000-8000-00805F9B34FB").uuid))
                //btSocket = device.createRfcommSocketToServiceRecord(ParcelUuid.fromString("c7f94713-891e-496a-a0e7-983a0946126e").uuid);
                Log.d("Bluetooth","btSocket=$btSocket MAC=${device.address}")
                Log.d("Bluetooth","${device.uuids}")
                //atomicReference = AtomicReference(btSocket)
                //btSocketHashMap[btSocket.hashCode()] = btSocket
                closeConnectionIfTakesTooLong(btSocket)
                btSocket.connect()
                bluetoothSocket = btSocket
                Log.d(TAG,"${btSocket.isConnected}")
                isBTInUse = false
                startWifiActivity(context)
            }
            catch (e:IOException)   {
                Log.d(TAG,"${e.printStackTrace()}")
                Log.d(TAG,"Calling fallback bluetooth connection")
                fallbackConnection(btSocket,context)
            }
        }
        else Toast.makeText(UtilityClass.applicationContext,"Bluetooth is currently in use",Toast.LENGTH_LONG).show()
    }

    /**
     * When the traditional connect function is called and fails, this private method is called to attempt creating a
     * bluetooth connection using reflection.
     */
    private fun fallbackConnection(socket: BluetoothSocket,context: Context?){
        lateinit var  fbBTSocket: FallbackBluetoothSocket
        lateinit var atomicReference: AtomicReference<BluetoothSocket>
        try{
            fbBTSocket = FallbackBluetoothSocket(
                socket,
                1
            )//insecure
            //btSocketHashMap[fbBTSocket.underlyingSocket.hashCode()] = fbBTSocket.underlyingSocket
            closeConnectionIfTakesTooLong(fbBTSocket.underlyingSocket)
            Log.d(TAG,"FallBackBluetooth object created, calling connect")
            fbBTSocket.connect()
            bluetoothSocket = fbBTSocket.underlyingSocket
            Log.d(TAG,"Connected to device")
            /*val uuids = bluetoothSocket?.remoteDevice?.uuids
            uuids?.forEach {
                Log.d(TAG,"UUIDs ${it.uuid.toString()}")
            }*/
            if(count==0)
                startWifiActivity(context!!)
            else
                sendMessage(jsonObject)
            //atomicReference.getAndSet(fbBTSocket.underlyingSocket)
            //fbBTSocket.inputStream
            Log.d("Bluetooth","Connection Established ${fbBTSocket.underlyingSocket.isConnected}")
            Log.d("Bluetooth","isConnected = ${fbBTSocket.underlyingSocket.hashCode()}; ")
        }
        catch (e:FallbackException){
            e.printStackTrace()
        }
        finally {
            isBTInUse = false
            //fbBTSocket?.close()
            Toast.makeText(UtilityClass.applicationContext,"Bluetooth is ready for use",Toast.LENGTH_LONG).show()
        }
    }

    private fun startWifiActivity(context: Context){
        Handler.handler.post(Runnable {
            UtilityClass.startActivity(context,WifiSelectionActivity::class.java)
        })
    }

    /*private fun addToBTHashMap(bluetoothSocket: BluetoothSocket){
        if (btSocketHashMap.containsKey(bluetoothSocket))
            btSocketHashMap[bluetoothSocket] = btSocketHashMap[bluetoothSocket]!!.plus(1)
        else
            btSocketHashMap[bluetoothSocket] = 1
    }*/

    /**
     * private method called to close a socket if it takes too long to connect
     */
    private fun closeConnectionIfTakesTooLong(socket:  BluetoothSocket){
        ThreadPool.postTask(Runnable {
            try {
                Log.d("Bluetooth","Connection would be closed if it takes too long:${BT_CONNECT_TIMEOUT}")
                Thread.sleep(BT_CONNECT_TIMEOUT)
                //var socket = btSocketHashMap[socket.hashCode()]
                Log.d(TAG,"Thread awoke")
                if(!socket.isConnected){
                    socket?.close()
                    //btSocketHashMap.remove(socket.hashCode())
                    Log.d("Bluetooth","Connection was closed $socket with state ${socket?.isConnected}  ${socket.hashCode()}")
                }
            }
            catch (e:InterruptedException){
                e.printStackTrace()
            }
            catch (e:Exception){
                e.printStackTrace()
            }
        })
    }

    fun sendMessage(string: JSONObject){
        jsonObject = string
        /*Log.d(TAG,"$string")
        var intent = Intent(Intent.ACTION_OPEN_DOCUMENT,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        (context as WifiSelectionActivity).startActivityForResult(intent,50)*/
        ThreadPool.postTask(Runnable {
            try {
                Log.d(TAG,"Writing message $jsonObject")
                var dataOut = DataOutputStream(bluetoothSocket!!.outputStream)
                //val data = ByteArray(4*1024)
                dataOut.writeChars(string.toString())
                dataOut.flush()
                dataOut.close()
            }
            catch (e:IOException){
                Log.d(TAG,"IOException occurred ${e.message}")
                e.printStackTrace()
                if(count<2){
                    count++
                    fallbackConnection(bluetoothSocket!!,null)
                    //sendMessage(string)
                }
                else
                    count=0;
            }
            catch (e:Exception){
                e.printStackTrace()
                Log.d(TAG,"Exception occurred ${e.message}")
            }
        })
    }


    /*fun sendImage(uri:Uri,context: Context){
        this.url = uri
        this.cont = context
        ThreadPool.postTask(Runnable {
            try {
                var file = File(uri.path)
                var cont = context.contentResolver.openInputStream(uri)
                Log.d(TAG,"Image path = ${file.absolutePath} with name ${file.name} of size ${file.length()} socketState = ${bluetoothSocket?.isConnected}")
                var dataOut = DataOutputStream(bluetoothSocket!!.outputStream)
                var dataInp = DataInputStream(cont)
                var data = ByteArray(4*1024)
                var size = -1
                while(true)
                {
                    size=dataInp.read(data,0,data.size)
                    if(size==-1)
                        break
                    dataOut.write(data,0,size)
                    dataOut.flush()
                    Log.d(TAG,"writing image")
                }

                dataOut.close()
                dataInp.close()
                Log.d(TAG,"message written. Sockets closed")
            }
            catch (e:Exception){
                Log.d(TAG,"Exception occurred ${e.message}")
                Log.d(TAG,"Retrying to send message")
                e.printStackTrace()
                if(count<2)
                {
                    count++
                    fallbackConnection(bluetoothSocket!!,context)
                }
                bluetoothSocket?.close()
            }
        })
    }*/
}