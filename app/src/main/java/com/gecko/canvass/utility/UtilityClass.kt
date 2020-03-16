package com.gecko.canvass.utility

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.net.Uri
import android.net.wifi.ScanResult
import android.os.Bundle
import android.os.Looper
import android.os.NetworkOnMainThreadException
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.FragmentManager
import com.gecko.canvass.R
import com.gecko.canvass.custom.CustomDialogFragment
import com.gecko.canvass.custom.ImageMetadata
import com.gecko.canvass.interfaces.DialogClickListener
import com.gecko.canvass.interfaces.ImageLoaderListener
import com.gecko.canvass.logic.WifiSetup
import org.json.JSONObject
import java.io.InputStream
import java.io.PrintStream
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.StringBuilder
import java.net.InetAddress
import java.net.URL
import java.util.*
import javax.mail.Message
import javax.mail.MessagingException
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import javax.net.ssl.HttpsURLConnection


object UtilityClass{
    private var fontAwesomeTypeface:Typeface?=null
    var context:Context? = null
    lateinit var applicationContext: Context
    var screenWidth = 0
    var screenHeight = 0
    private const val TAG = "UtilityClass"

     fun getFontAwesomeTypeface(context:Context):Typeface{
         return fontAwesomeTypeface?:Typeface.createFromAsset(context.assets,"font/FontAwesome.ttf")
     }

    fun <T>startActivity(context:Context,`class`:Class<T>){
        var intent = Intent(context, `class`)
        context.startActivity(intent)
    }

    fun <T>startActivity(context:Context,`class`:Class<T>,bundle: Bundle?){
        var intent = Intent(context, `class`)
        if(bundle!=null)
            intent.putExtras(bundle)
        context.startActivity(intent)
    }

    /*fun showInformationDialog(title:String,message:String,fragmentManager: FragmentManager,listener: DialogClickListener?, width:Int,height:Int):CustomDialogFragment{
        var dialog:CustomDialogFragment = CustomDialogFragment(listener, INFO_DIALOG)
        var bundle:Bundle = Bundle()
        bundle.apply {
            putInt("xml",R.layout.info_dialog)
            putInt("width",width)
            putInt("height",height)
            putString("title",title)
            putString("message",message)
        }
        dialog.arguments = bundle

        return dialog.apply {
            showsDialog=true
            isCancelable = false
            showNow(fragmentManager,"")
        }
    }*/

    /**
     * private method to set the dialog dimensions. Provide a value of 0 to match parents and -1 to
     * wrap contents
     */
    fun showDialog(title:String,message:String,dialogType:Int,fragmentManager: FragmentManager,listener: DialogClickListener?, width:Int,height:Int):CustomDialogFragment{
        var dialog = CustomDialogFragment(listener, dialogType)
        var bundle:Bundle = Bundle()
        bundle.apply {
            putInt("xml",if(dialogType== INPUT_DIALOG) R.layout.input_dialog else if(dialogType== INFO_DIALOG)R.layout.info_dialog else R.layout.progress_dialog_layout)
            putInt("width",width)
            putInt("height",height)
            putString("title",title)
            putString("message",message)
        }
        dialog.arguments = bundle

        return dialog.apply {
            showsDialog=true
            isCancelable = false
            showNow(fragmentManager,"")
        }
    }

    fun inflateLogoLayout(inflater:LayoutInflater,group:ViewGroup):ViewGroup{
        return inflater.inflate(R.layout.logo_layout,group,false) as ViewGroup
    }
    fun inflateLogoLayout(inflater: LayoutInflater,group: ViewGroup,textSize:Int):ViewGroup{
        val parent = inflateLogoLayout(inflater,group)
        val size = convertToPixels(textSize,parent.context)
        return parent.apply {
            findViewById<TextView>(R.id.first).textSize = size.toFloat()
            findViewById<TextView>(R.id.second).textSize = size.toFloat()
            findViewById<TextView>(R.id.third).textSize = size.toFloat()
            findViewById<TextView>(R.id.fourth).textSize = size.toFloat()
            findViewById<TextView>(R.id.fifth).textSize = size.toFloat()
        }
    }

    fun isWifiPasswordValid(password:String):Boolean{
        return password.length >= PASSWORD_MIN_LENGTH && password.length< (PASSWORD_MAX_LENGTH+1)
    }

    private fun convertToPixels(dp: Int, context:Context): Int {
        val wm =
            context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val dm = DisplayMetrics()
        wm.defaultDisplay.getMetrics(dm)
        return (dm.density * dp).toInt()
    }

    fun showToast(text: String){
        Handler.handler.post(Runnable {
            Toast.makeText(applicationContext,text,Toast.LENGTH_LONG).show()
        })
    }
    fun convertToQuotedString(ssid:String,shouldEscapeWhiteSpace:Boolean):String{
        //first we'd replace all the white spaces with a \ followed by the number of white spaces
        var newSSID = ""
        newSSID = if(shouldEscapeWhiteSpace)
            "\\s+".toRegex().replace(ssid){
                    m->"\\${m.value}"
            }
        else
            ssid
        return "\"${newSSID}\""
    }

    fun hasWhiteSpace(text:String):Boolean{
        return "\\s+".toRegex().containsMatchIn(text)
    }

    fun getWifiSecurityType(scanResult: ScanResult?):Int{
        return when {
            scanResult?.capabilities?.contains("WEP") ==true -> SECURITY_WEP
            scanResult?.capabilities?.contains("SAE") ==true-> SECURITY_SAE
            scanResult?.capabilities?.contains("PSK")==true -> SECURITY_PSK
            scanResult?.capabilities?.contains("EAP_SUITE_B_192")==true -> SECURITY_EAP_SUITE_B
            scanResult?.capabilities?.contains("EAP")==true -> SECURITY_EAP
            scanResult?.capabilities?.contains("OWE")==true -> SECURITY_OWE
            else -> SECURITY_NONE
        }
    }

    fun convertIPToByteArray(ip:Int):ByteArray{
        val ipArray = UByteArray(4)
        Log.d(TAG,"OP ${ip.ushr(24)}")
        Log.d(TAG,"OP ${ip.ushr(16) and 255}")
        Log.d(TAG,"OP ${ip.ushr(8) and 255}")
        Log.d(TAG,"OP ${ip.ushr(0) and 255}")
        ipArray[3] = ip.ushr(24).toUByte()
        ipArray[2] = (ip.ushr(16) and 255).toUByte()
        ipArray[1] = ip.ushr(8 and 255).toUByte()
        ipArray[0] = ip.ushr(0 and 255).toUByte()

        return ipArray.toByteArray()
    }

    fun writeBooleanSharedPreference(property:String,value:Boolean){
        var preferences = applicationContext.getSharedPreferences("canvas", Context.MODE_PRIVATE)
        preferences.edit().putBoolean(property,value).apply()
    }

    fun getBooleanSharedPreference(property: String):Boolean{
        var preferences = UtilityClass.applicationContext.getSharedPreferences("canvas", Context.MODE_PRIVATE)
        return preferences.getBoolean(property,false)
    }

    fun fetchJSONFromURL(url:URL):String{
        if(Looper.getMainLooper()==Looper.myLooper())
            NetworkOnMainThreadException()
        lateinit var client: HttpsURLConnection
        lateinit var inputStream: InputStream
        lateinit var response:StringBuilder
        try{
            Log.d(TAG,"Fetching JSON")

            Log.d(TAG,"URL is ${url.toString()}")
            client = url.openConnection() as HttpsURLConnection
            client.setRequestProperty("Authorization", PEXEL_API_KEY)
            client.doInput=true
            client.connect()
            val responseCode = client.responseCode
            inputStream = client.inputStream
            val data = ByteArray(8*1024)
            response = StringBuilder()
            var size= 0
            when(responseCode){
                HttpsURLConnection.HTTP_OK->{
                    while (true){
                        size = inputStream.read(data,0,data.size)
                        if(size==-1)
                            break
                        else
                            response.append(String(data,0,size))
                    }
                    //create the JSONObject to parse response from the server
                    //response = JSONObject(stringBuilder.toString())
                    Log.d(TAG,"Response is: $response")
                    Log.d(TAG,"Response Msg is ${client.responseMessage}")
                }
            }
        }
        catch (e:Exception){
            Log.d(TAG,"${e.printStackTrace()}")
        }
        finally {
            client.disconnect()
            inputStream.close()
        }
        return response.toString()
    }

    fun loadImageToImageView(imageMetadata: ImageMetadata,imageView: ImageView,listener:ImageLoaderListener,xPosition:Int =-1,yPosition:Int=-1){
        ThreadPool.postTask(Runnable {
            val bitmap = BitmapFactory.decodeFile(imageMetadata.imageLocation)
            if(xPosition<0 || yPosition<0)
                listener.addImageToLruCache(bitmap, xPosition.coerceAtLeast(yPosition))
            else
                listener.addImageToLruCache(bitmap,xPosition,yPosition)
            Handler.handler.post(Runnable {
                imageView.setImageBitmap(bitmap)
                imageView.scaleType= ImageView.ScaleType.FIT_CENTER
            })
        })
    }

    fun reportException(){
        Thread.UncaughtExceptionHandler(){ thread: Thread, throwable: Throwable ->
            Log.d(TAG,"Mailing exception")
            sendMail(throwable)
        }
    }

    fun sendMail(throwable: Throwable){
        val mailTo = "mgbachi_anthony@yahoo.com"
        val from  = "rexjay8@gmail.com"
        val host = "localhost";//or IP address

        //Get the session object
        val properties = Properties()
        properties.setProperty("mail.smtp.host", host);
        val session = Session.getDefaultInstance(properties);

        //compose the message
        try
        {
            /*val stringWriter = StringWriter()
            throwable.printStackTrace(PrintWriter(stringWriter))
            val text = stringWriter.toString()
            val message = MimeMessage(session)
            message.setFrom( InternetAddress(from));
            message.addRecipient(Message.RecipientType.TO, InternetAddress(mailTo));
            message.setSubject("Colaj Exception");
            message.setText(text);

            // Send message
            Transport.send(message);
            System.out.println("message sent successfully....");*/
            val stringWriter = StringWriter()
            throwable.printStackTrace(PrintWriter(stringWriter))
            val text = stringWriter.toString()

            /*val emailIntent =  Intent(Intent.ACTION_SENDTO, Uri.fromParts(
            "mailto","mgbachi_anthony@yahoo.com", null));
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Exception in Colaj");
            emailIntent.putExtra(Intent.EXTRA_TEXT, text);
            emailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf("mgbachi_anthony@yahoo.com"))*/

            val mailto = "mailto:mgbachi_anthony@yahoo.com" +
            "?cc=" + "" +
            "&subject=" + Uri.encode("Exception in Colaj") +
            "&body=" + Uri.encode(text);

            val emailIntent =  Intent(Intent.ACTION_SENDTO);
            emailIntent.data = Uri.parse(mailto);
            context!!.startActivity(Intent.createChooser(emailIntent, "Send email..."));
        }
        catch (mex:MessagingException)
        {
            mex.printStackTrace();
        }
    }
}