package com.gecko.canvass.activity

import android.graphics.Bitmap
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.gecko.canvass.R
import com.gecko.canvass.custom.CustomDialogFragment
import com.gecko.canvass.custom.ImageMetadata
import com.gecko.canvass.interfaces.DownloadListener
import com.gecko.canvass.logic.ImageLoader
import com.gecko.canvass.utility.FULL_SIZED_IMAGES_DIR
import com.gecko.canvass.utility.IMAGE_LOADER_SEARCH_MODE
import com.gecko.canvass.utility.PROGRESS_DIALOG
import com.gecko.canvass.utility.UtilityClass

class ImageExpansionActivity: AppCompatActivity(),DownloadListener {
    private lateinit var image:ImageView
    private lateinit var imageMetadata: ImageMetadata
    private lateinit var imageLoader: ImageLoader
    private val TAG = "ImageExpansionActivity"
    private lateinit var dialog:CustomDialogFragment
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        UtilityClass.reportException()
        UtilityClass.applicationContext = applicationContext//we set and hold a reference to application context from UtilityClass
        UtilityClass.context = this
        setContentView(R.layout.image_expand_layout)
        image = findViewById(R.id.image_fs)
        val imageUrl = intent.extras?.getString("url") ?:""
        Log.d(TAG,"image url is $imageUrl")
        if(imageUrl.isNotEmpty()){
            imageLoader = ImageLoader(this, IMAGE_LOADER_SEARCH_MODE)
            //for some reason, the Pexel API, returns "rotated" images if the phone width and height values are passed. As a workaround, I pass the phone height as width and phone width as height
            imageLoader.downloadImage(imageUrl,UtilityClass.screenWidth,UtilityClass.screenHeight, FULL_SIZED_IMAGES_DIR)
            dialog = UtilityClass.showDialog("Loading Image","fetching image data", PROGRESS_DIALOG, supportFragmentManager,null,0,-1)
        }
    }

    override fun downloadCompleted(imageMetadata: ImageMetadata, bitmap: Bitmap) {
        dialog.dismiss()
        image.setImageBitmap(bitmap)
        this.imageMetadata = imageMetadata
    }

    override fun onDestroy() {
        super.onDestroy()
        imageLoader.releaseDownloadListenerReference()
    }
}