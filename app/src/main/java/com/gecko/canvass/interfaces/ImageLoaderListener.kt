package com.gecko.canvass.interfaces

import android.graphics.Bitmap

interface ImageLoaderListener {
    fun addImageToLruCache(bitmap:Bitmap,xPosition:Int,yPosition:Int){}
    fun addImageToLruCache(bitmap:Bitmap,position:Int){}
}