package com.gecko.canvass.adapters

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.nfc.Tag
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.gecko.canvass.R
import com.gecko.canvass.custom.CustomLruCache
import com.gecko.canvass.custom.ImageMetadata
import com.gecko.canvass.custom.SearchResultsListHolder
import com.gecko.canvass.custom.StoreImagesListHolder
import com.gecko.canvass.interfaces.ImageLoaderListener
import com.gecko.canvass.logic.ImageLoader
import com.gecko.canvass.utility.Constants
import com.gecko.canvass.utility.UtilityClass

class SearchResultsListAdapter : RecyclerView.Adapter<SearchResultsListHolder>(),ImageLoaderListener{

    private val imageArrayList = ArrayList<ImageMetadata>()
    private val cache = CustomLruCache(Constants.getLruCacheSize())
    lateinit var imageLoader: ImageLoader
    private var previousSize = 0
    private val TAG = "SearchResultsListAdapter"

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchResultsListHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.search_result_item,parent,false)
        return SearchResultsListHolder(view)
    }

    override fun getItemCount(): Int {
        return imageArrayList.size
    }

    @SuppressLint("LongLogTag")
    override fun onBindViewHolder(holder: SearchResultsListHolder, position: Int) {
        //val imageView = holder.image
        if(cache[position]!= null)
            holder.image.setImageBitmap(cache[position])
        else
            UtilityClass.loadImageToImageView(imageArrayList[position],holder.image,this,yPosition = position)
        if((position.toDouble().div(imageArrayList.size))>=0.7 && previousSize!=imageArrayList.size) {
            Log.d(TAG, "Approaching the bottom ${(position.toDouble().div(imageArrayList.size))}")
            previousSize = imageArrayList.size
            imageLoader.fetchNextImage()
        }
    }

    override fun addImageToLruCache(bitmap: Bitmap,position: Int){
        cache.put(position,bitmap)
    }

    fun addImage(bitmap: Bitmap,imageMetadata: ImageMetadata){
        imageArrayList.add(imageMetadata)
        addImageToLruCache(bitmap,imageArrayList.size-1)
        notifyItemInserted(imageArrayList.size-1)
    }

}