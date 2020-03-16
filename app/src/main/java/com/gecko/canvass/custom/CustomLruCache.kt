package com.gecko.canvass.custom

import android.graphics.Bitmap
import android.util.LruCache

class CustomLruCache(size:Int): LruCache<Int, Bitmap>(size) {

    override fun sizeOf(key: Int?, value: Bitmap?): Int {
        return value!!.byteCount/1024
    }
}