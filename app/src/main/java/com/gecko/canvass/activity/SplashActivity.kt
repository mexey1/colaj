package com.gecko.canvass.activity

import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import com.gecko.canvass.R
import com.gecko.canvass.utility.*
import java.io.File


class SplashActivity: AppCompatActivity()
{
    private val TAG = "SplashActivity"
    override fun onCreate(savedInstanceState: android.os.Bundle?)
    {
        super.onCreate(savedInstanceState)
        UtilityClass.reportException()
        UtilityClass.applicationContext = applicationContext//we set and hold a reference to application context from UtilityClass
        UtilityClass.context = this
        UtilityClass.applicationContext = applicationContext//we set and hold a reference to application context from UtilityClass
        setContentView(R.layout.splash_activity)
        var logoParent = findViewById<RelativeLayout>(R.id.logo)
        LayoutInflater.from(this).inflate(R.layout.logo_layout,logoParent)
        //logoParent.addView(logo)
        init()
        Handler.handler.postDelayed(Runnable {
            if(UtilityClass.getBooleanSharedPreference(COLAJ_SETUP_SUCCESSFUL))
                UtilityClass.startActivity(this@SplashActivity,HomeActivity::class.java)
            else
                UtilityClass.startActivity(this@SplashActivity,MainActivity::class.java)
            this@SplashActivity.apply{
                finish()
            }
        },5000)
    }

    private fun init(){
        deleteAndCreateDirectory(THUMBNAIL_DIR)
        deleteAndCreateDirectory(FULL_SIZED_IMAGES_DIR)
        deleteAndCreateDirectory(IMAGES_FROM_SEARCH_DIR)
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        UtilityClass.screenHeight = displayMetrics.heightPixels
        UtilityClass.screenWidth= displayMetrics.widthPixels
    }

    private fun deleteAndCreateDirectory(dirName:String){
        var file = File("${filesDir}/$dirName/")
        if(file.exists())
            Log.d(TAG,"Old files are deleted? ${file.deleteRecursively()}")
        val res = file.mkdirs()
        Log.d(TAG,"$dirName directory created $res")
    }
}
