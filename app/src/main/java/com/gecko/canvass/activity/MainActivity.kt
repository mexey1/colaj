package com.gecko.canvass.activity

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.gecko.canvass.R
import com.gecko.canvass.utility.UtilityClass
import kotlin.math.log

class MainActivity:AppCompatActivity(){

    private lateinit var parent:ViewGroup
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        UtilityClass.reportException()
        UtilityClass.applicationContext = applicationContext//we set and hold a reference to application context from UtilityClass
        UtilityClass.context = this
        setContentView(R.layout.activity_main)
        parent = findViewById(R.id.logo)
        val logo = UtilityClass.inflateLogoLayout(layoutInflater,parent)
        parent.addView(logo)
        findViewById<View>(R.id.email).setOnClickListener(View.OnClickListener {
            UtilityClass.startActivity(this@MainActivity,EmailActivity::class.java)
        })
    }

}