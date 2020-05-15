package com.gecko.canvass.custom

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.gecko.canvass.R
import com.gecko.canvass.interfaces.DialogClickListener
import com.gecko.canvass.utility.*
import kotlinx.android.synthetic.main.info_dialog.*
import org.json.JSONObject

class CustomDialogFragment:DialogFragment {

    private lateinit var myDialog:Dialog
    private var listener: DialogClickListener?
    private var dialogType = -1;
    private lateinit var views:Array<View>
    private var loop = 0
    @Volatile private var shouldLoop = false
    private val TAG = "CustomDialogFragment"

    constructor(listener: DialogClickListener?,dialogType:Int){
        this.listener = listener
        this.dialogType = dialogType
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        Log.d("create dialog activity", "$activity")
        myDialog = super.onCreateDialog(savedInstanceState)
        return myDialog
    }

    /**
     * this method is overridden so we can specify the width and height of the dialog
     */
    override fun onResume() {
        super.onResume()
        var args = arguments
        var width = args!!.getInt("width")
        var height = args.getInt("height")
        setDialogSize(width,height)
    }

    override fun onCreateView(inflater: LayoutInflater,container: ViewGroup?,savedInstanceState: Bundle?): View? {
        var args = arguments
        val view =  inflater.inflate(args!!.getInt("xml"),container,false)
        if(args.containsKey("title"))
            view.findViewById<TextView>(R.id.title)?.text = args.getString("title")
        if(args.containsKey("message"))
           view.findViewById<TextView>(R.id.message)?.text = args.getString("message")
        Log.d("args value", "$args")
        when(dialogType){
            INFO_DIALOG->{
                view.findViewById<Button>(R.id.got_it).setOnClickListener{
                        Log.d(CUSTOM_DIALOG,"GOT IT Clicked")
                        myDialog.dismiss()
                        listener?.onClick()
                }
            }
            INPUT_DIALOG->{
                val editText = view.findViewById<EditText>(R.id.password)
                val connectButton  = view.findViewById<Button>(R.id.connect)
                //we'd disable the connect button and only enable it when the password length is valid
                connectButton.isEnabled = false
                view.findViewById<CheckBox>(R.id.show_password).setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener{ compoundButton: CompoundButton, isChecked: Boolean ->
                    //val editText = myDialog.findViewById<EditText>(R.id.password);
                    if (isChecked)
                        editText.transformationMethod=null
                    else
                        editText.transformationMethod = PasswordTransformationMethod()
                    editText.setSelection(editText.text.length)
                })

                //listen for text inputs and enable the edit text button if & only if the password
                //length is valid
                editText.addTextChangedListener(object: TextWatcher{
                    override fun afterTextChanged(s: Editable?) {
                    }

                    override fun beforeTextChanged(
                        s: CharSequence?,
                        start: Int,
                        count: Int,
                        after: Int
                    ) {

                    }

                    override fun onTextChanged(
                        s: CharSequence?,
                        start: Int,
                        before: Int,
                        count: Int
                    ) {
                        Log.d(TAG,"text changed $s is password valid = ${UtilityClass.isWifiPasswordValid(s.toString())}")
                        connectButton.isEnabled = UtilityClass.isWifiPasswordValid(s.toString())
                    }
                })
                //set listeners for the connect and cancel buttons
                    connectButton.setOnClickListener{
                        val password = (myDialog.findViewById<EditText>(R.id.password) as EditText).text.toString()
                        if(UtilityClass.isWifiPasswordValid(password)){
                            val jsonObject = JSONObject()
                            jsonObject.put("password",password)
                            myDialog.dismiss()
                            listener?.onClick(jsonObject)
                        }
                    }
                    view.findViewById<Button>(R.id.cancel).setOnClickListener{
                        Log.d(CUSTOM_DIALOG,"GOT IT Clicked")
                        myDialog.dismiss()
                        //listener!!.onClick()
                    }
            }
            PROGRESS_DIALOG->{
                shouldLoop = true
                animateProgress(view)
            }
        }
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        Log.d("$javaClass","$activity")
        //show(activity!!.supportFragmentManager,"")
    }

    /**
     * private method to set the dialog dimensions. Provide a value of 0 to match parents and -1 to
     * wrap contents
     */
    private fun setDialogSize(width:Int,height:Int){
        var layoutParams = WindowManager.LayoutParams()
        Log.d("dialog", "$myDialog,$dialog")
        layoutParams.copyFrom(myDialog.window?.attributes)
        layoutParams.width= if(width==0)  WindowManager.LayoutParams.MATCH_PARENT else if(width==-1) WindowManager.LayoutParams.WRAP_CONTENT  else width
        layoutParams.height = if(height==0)  WindowManager.LayoutParams.MATCH_PARENT else if(height==-1) WindowManager.LayoutParams.WRAP_CONTENT  else height
        dialog?.window?.attributes=layoutParams
    }

    /**
     * method called to animate the search while user waits for bluetooth devices to be found
     */
    private fun animateProgress(deviceScan:View){
         views = arrayOf(deviceScan.findViewById<View>(R.id.view_one),
            deviceScan.findViewById<View>(R.id.view_two),deviceScan.findViewById<View>(R.id.view_three),
            deviceScan.findViewById<View>(R.id.view_four))
        views[1].isEnabled=false
        views[2].isEnabled=false
        views[3].isEnabled=false
        animateLoop(loopFun)
        //lambda to enable/disable views
    }

    /**
     * recursive higher order function to animate the search
     */
    private fun animateLoop(loop:()->Unit){
        Handler.handler.postDelayed({
            loop.invoke()
            //if(shouldLoop)
            if(shouldLoop)
                animateLoop (loop)
        },1000)
    }

    //lambda expression for performing animation
    private var loopFun:()->Unit = {
        views[loop%4].isEnabled = false
        views[++loop%4].isEnabled = true
        //Log.d("Looping","$loop + $shouldLoop")
    }

    /**
     * method overridden to also make the animation for progress dialog to exit
     */
    override fun dismiss() {
        super.dismiss()
        myDialog.dismiss()
        shouldLoop = false
    }
}