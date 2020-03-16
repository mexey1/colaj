package com.gecko.canvass.activity

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import com.gecko.canvass.R
import com.gecko.canvass.utility.UtilityClass
import java.util.*


class EmailActivity : AppCompatActivity(){
    private lateinit var submit:Button
    private lateinit var emailView:ViewGroup
    private lateinit var emailCodeView:ViewGroup
    private lateinit var stepOneTextView: TextView
    private lateinit var stepTwoTextView: TextView
    private lateinit var stepOneProgress:View
    private lateinit var stepTwoProgress:View
    private lateinit var parent:ViewGroup
    private lateinit var turquoiseDrawable:Drawable
    private lateinit var greyDrawable:Drawable
    private  var turquoise = 0
    private  var grey = 0
    private val stack=Stack<ViewGroup>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        UtilityClass.reportException()
        UtilityClass.applicationContext = applicationContext//we set and hold a reference to application context from UtilityClass
        UtilityClass.context = this
        UtilityClass.applicationContext = applicationContext//we set and hold a reference to application context from UtilityClass
        setContentView(R.layout.email_activity)
        init()
    }

    /**
     * initialize actions for different UI components
     */
    private fun init(){
        //retrieve colors
        turquoise = resources.getColor(R.color.fourth)
        grey = resources.getColor(R.color.grey)
        //retrieve drawables for turquoise and grey drawables
        turquoiseDrawable = resources.getDrawable(R.drawable.turquoise_oval_background,theme)
        greyDrawable = resources.getDrawable(R.drawable.grey_oval_background,theme)
        //retrieve progress bars
        stepOneProgress = findViewById(R.id.step_1_progress)
        stepTwoProgress = findViewById(R.id.step_2_progress)
        //retrieve text views
        stepOneTextView = findViewById(R.id.step_1_txt_vw)
        stepTwoTextView = findViewById(R.id.step_2_txt_vw)

        findViewById<View>(R.id.close).setOnClickListener(View.OnClickListener { finish() })
        parent = findViewById(R.id.parent)
        emailView = layoutInflater.inflate(R.layout.email_fragment,parent,false) as ViewGroup
        parent.addView(emailView)
        stack.push(emailView)
        val logo = UtilityClass.inflateLogoLayout(layoutInflater,parent,11)
        findViewById<ViewGroup>(R.id.logo).addView(logo)
        submit = findViewById(R.id.next)
        submit.setOnClickListener(){//define what happens when the submit button is clicked
            showEmailCodeLayout()
        }
    }

    private fun showEmailCodeLayout(){
        if(!this::emailCodeView.isInitialized){
            emailCodeView = layoutInflater.inflate(R.layout.email_code_fragment,parent,false) as ViewGroup
            emailCodeView.findViewById<Button>(R.id.verify).setOnClickListener(View.OnClickListener {
                val intent = Intent(applicationContext, DeviceScanActivity::class.java)
                intent.flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            })
        }

        if(stack.peek()!==emailCodeView)
        {
            //flip text colors
            stepOneTextView.setTextColor(grey)
            stepTwoTextView.setTextColor(turquoise)
            //flip progress background
            stepOneProgress.background=greyDrawable
            stepTwoProgress.background=turquoiseDrawable
            //remove all the views in the parent layout and add the email code view
            parent.removeAllViews()
            parent.addView(emailCodeView)
            stack.push(emailCodeView)
        }
    }

    private fun showEmailLayout(){
        stepOneTextView.setTextColor(turquoise)
        stepTwoTextView.setTextColor(grey)
        //flip progress background
        stepOneProgress.background=turquoiseDrawable
        stepTwoProgress.background=greyDrawable
        //remove all the views in the parent layout and add the email code view
        parent.removeAllViews()
        parent.addView(emailView)
    }

    override fun onBackPressed() {
        val size = stack.size
        Log.d("On back Pressed","On back pressed $size")
        stack.pop()
        if(size==2)
            showEmailLayout()
        else
            super.onBackPressed()
    }
}