package com.gecko.canvass.interfaces

import android.graphics.Bitmap
import com.gecko.canvass.custom.ImageMetadata

interface DownloadListener{
    fun downloadCompleted(image:ImageMetadata,bitmap: Bitmap){}
    fun downloadCompleted(image:ImageMetadata,bitmap: Bitmap,xPosition:Int,yPosition:Int){}
}