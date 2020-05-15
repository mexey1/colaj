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
import androidx.appcompat.app.AppCompatActivity
import com.gecko.canvass.activity.HomeActivity
import com.gecko.canvass.activity.WifiSelectionActivity
import com.gecko.canvass.custom.CustomDialogFragment
import com.gecko.canvass.custom.FallbackBluetoothSocket
import com.gecko.canvass.exceptions.FallbackException
import com.gecko.canvass.utility.*
import com.sun.mail.imap.Utility
import org.json.JSONObject
import java.io.*
import java.lang.Exception
import java.util.*
import java.util.concurrent.atomic.AtomicReference

object Bluetooth {
    val adapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothSocket:BluetoothSocket? = null
    private var isBTInUse = false
    private val TAG = "Bluetooth"
    private lateinit var url:Uri
    private var count=0
    private lateinit var jsonObject:JSONObject
    private lateinit var dataOutputStream: DataOutputStream
    private lateinit var scanner: Scanner
    private var dialog:CustomDialogFragment? = null

    /**
     * method called to pair the device with the Colaj.
     */
    fun pair(device: BluetoothDevice,context: Context){
        if(!isBTInUse)//when bt isn't in use, we can proceed
        {
            //this.context = context
            //lateinit var btSocket: BluetoothSocket
            //lateinit var atomicReference: AtomicReference<BluetoothSocket>
            try {
                isBTInUse = true
                bluetoothSocket?.close()
                bluetoothSocket = device.createRfcommSocketToServiceRecord((ParcelUuid.fromString(
                    COLAJ_UUID).uuid))
                //btSocket = device.createRfcommSocketToServiceRecord(ParcelUuid.fromString("c7f94713-891e-496a-a0e7-983a0946126e").uuid);
                Log.d("Bluetooth","btSocket=$bluetoothSocket MAC=${device.address}")
                Log.d("Bluetooth","${device.uuids}")
                //atomicReference = AtomicReference(btSocket)
                //btSocketHashMap[btSocket.hashCode()] = btSocket
                closeConnectionIfTakesTooLong(bluetoothSocket!!)
                bluetoothSocket?.connect()
                Log.d(TAG,"${bluetoothSocket?.isConnected}")

                //create streams for communicating with the device
                dataOutputStream = DataOutputStream(bluetoothSocket!!.outputStream)
                scanner = Scanner(bluetoothSocket!!.inputStream)
                startWifiActivity(context)
            }
            catch (e:IOException)   {
                /*Log.d(TAG,"${e.printStackTrace()}")
                Log.d(TAG,"Calling fallback bluetooth connection")
                fallbackConnection(btSocket,context)*/
                UtilityClass.showDialog("Connection Failed",
                    "Failed to establish a connection with the Colaj device",
                    INFO_DIALOG,(UtilityClass.context as AppCompatActivity).supportFragmentManager,null,0,-1)
            }
            finally {
                isBTInUse = false
            }
        }
        else Toast.makeText(UtilityClass.applicationContext,"Bluetooth is currently in use",Toast.LENGTH_LONG).show()
    }

    /**
     * When the traditional connect function is called and fails, this private method is called to attempt creating a
     * bluetooth connection using reflection.
     *
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
    }*/

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

    /**
     * method called to send message to the connected bluetooth device. This method can be called
     * on the UI thread as it spawns it's own thread to make network related calls.
     */
    fun sendMessage(dialogFragment: CustomDialogFragment,string: JSONObject){
        dialog = dialogFragment
        jsonObject = string
        /*Log.d(TAG,"$string")
        var intent = Intent(Intent.ACTION_OPEN_DOCUMENT,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        (context as WifiSelectionActivity).startActivityForResult(intent,50)*/
        readMessage()
        ThreadPool.postTask(Runnable {
            try {
                Log.d(TAG,"Writing message $jsonObject")
                //val data = ByteArray(4*1024)
                //dataOutputStream.writeChars(string.toString())
                //dataOutputStream.writeChars("\n")
                dataOutputStream.write(jsonObject.toString().toByteArray())
                dataOutputStream.write("\n".toByteArray())
                dataOutputStream.flush()
                //dataOut.close()
            }
            catch (e:IOException){
                e.printStackTrace()
            }
            /*catch (e:IOException){
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
            }*/
        })
    }

    /**
     * method to read response from the Colaj device after we've sent wifi
     * credentials to it
     */
    private fun readMessage(){
        ThreadPool.postTask(Runnable {
            val scanner = BufferedReader(InputStreamReader(bluetoothSocket?.inputStream))
            var response = ""
            lateinit var json:JSONObject
            try {
                response = scanner.readLine()
                Log.d(TAG,"Server response $response")
                UtilityClass.showToast("Bluetooth server response is $response")
                json = JSONObject(response)
                when(json.getString("action")){
                    "OK"->{
                        Handler.handler.post(Runnable {
                            dialog?.dismiss()
                            dialog = null
                            UtilityClass.showToast("WiFi connection was established successfully")
                            UtilityClass.startActivity(UtilityClass.context!!, HomeActivity::class.java)
                            //save to shared preferences that we setup the device successfully
                            UtilityClass.writeBooleanSharedPreference(COLAJ_SETUP_SUCCESSFUL,true)
                        })
                        //if we get ok response, we close the bluetooth socket connection afterwards
                        closeStreams()
                    }
                    "FAILED"->{
                        Handler.handler.post(Runnable {
                            dialog?.dismiss()
                            dialog = null
                            UtilityClass.showDialog("Incorrect WiFi Password",
                                "The password provided to the WiFi network was incorrect. Please try again",
                                INFO_DIALOG,(UtilityClass.context as AppCompatActivity).supportFragmentManager,null,0,-1)
                        })
                    }
                }
            }
            catch (e:IOException){
                e.printStackTrace()
            }
        })
    }

    /**
     * on receiving response from the Colaj device, we need to close the streams and
     * bluetooth connection.
     */
    private fun closeStreams(){
        scanner.close()
        dataOutputStream.close()
        bluetoothSocket?.close()
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