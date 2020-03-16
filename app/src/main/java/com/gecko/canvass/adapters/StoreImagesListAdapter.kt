package com.gecko.canvass.adapters

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.gecko.canvass.R
import com.gecko.canvass.activity.ImageExpansionActivity
import com.gecko.canvass.custom.CustomLruCache
import com.gecko.canvass.custom.ImageMetadata
import com.gecko.canvass.custom.StoreImagesListHolder
import com.gecko.canvass.interfaces.ImageLoaderListener
import com.gecko.canvass.logic.ImageLoader
import com.gecko.canvass.utility.Constants
import com.gecko.canvass.utility.Handler
import com.gecko.canvass.utility.ThreadPool
import com.gecko.canvass.utility.UtilityClass

class StoreImagesListAdapter: RecyclerView.Adapter<StoreImagesListHolder>(),ImageLoaderListener {
    private val imageMap = HashMap<Int, HashMap<Int,ImageMetadata>>()
    private val cache = CustomLruCache(Constants.getLruCacheSize())
    //private lateinit var imageLoader:ImageLoader
    private val TAG = "StoreImagesListAdapter"
    private var count = 0
    private var previousSize = 0
    lateinit var imageLoader:ImageLoader

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoreImagesListHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.recycler_image_item_four,parent,false)
        //val view2 = LayoutInflater.from(parent.context).inflate(R.layout.recycler_image_item_one,parent,false)
        //Log.d(TAG,"ImageOne width is ${view2.findViewById<View>(R.id.image_one).width}")
        return StoreImagesListHolder(view)
    }

    override fun getItemCount(): Int {
        return imageMap.size //* 3
    }

    override fun onBindViewHolder(holder: StoreImagesListHolder, position: Int) {
        /**
         * we need to enable 1 of the three available layouts. If it's a multiple of 3 we'd
         * enable layout 1, if it's a multiple of 6 then layout 2 else layout 0
         */
        Log.d(TAG,"Position passed is $position")
        val layoutPosition = if((position+1)%6==0) 2 else if((position+1)%3==0) 1 else 0
        //Log.d(TAG,"LayoutPosition is  $layoutPosition")
        val layout = holder.enableLayout(layoutPosition)
        val imageOne = layout.findViewById<ImageView>(R.id.image_one)
        val imageTwo = layout.findViewById<ImageView>(R.id.image_two)
        val imageThree = layout.findViewById<ImageView>(R.id.image_three)
        //clear the old bitmaps on the image views
        imageOne.setImageBitmap(null)
        imageTwo.setImageBitmap(null)
        imageThree.setImageBitmap(null)
        //Log.d(TAG,"Image width is ${imageOne.width} and from screen is ${UtilityClass.screenWidth/3}")
        for(i in 0 until 3){
            when(i){
                0->{setImageDimensions(imageOne,i,position); setImageClickListener(imageOne,i,position)}
                1->{setImageDimensions(imageTwo,i,position);setImageClickListener(imageTwo,i,position)}
                2->{setImageDimensions(imageThree,i,position);setImageClickListener(imageThree,i,position)}
            }
        }
        if((position.toDouble().div(imageMap.size))>=0.7 && (imageMap.size-previousSize)>=9) {
            Log.d(TAG, "Approaching the bottom ${(position.toDouble().div(imageMap.size))}")
            previousSize = imageMap.size
            imageLoader.fetchNextImage()
        }
    }

    private fun setImageClickListener(imageView: ImageView,xPosition:Int,yPosition: Int){
        imageView.setOnClickListener(View.OnClickListener {
            val hashMap = imageMap[yPosition]
            val bundle = Bundle()
            Log.d(TAG,"Image at X $xPosition and Y $yPosition")
            bundle.putString("url", hashMap?.get(xPosition)!!.imageUrl)
            UtilityClass.startActivity(it.context,ImageExpansionActivity::class.java,bundle)
        })
    }

    private fun setImageDimensions(imageView: ImageView,xPosition:Int,yPosition:Int){
        loadImageToImageView(xPosition,yPosition,imageView)
        if(yPosition%3==2 && xPosition==0){
            imageView.layoutParams.width=UtilityClass.screenWidth/3 *2
            imageView.layoutParams.height=UtilityClass.screenWidth/3 *2
        }
        else
        {
            imageView.layoutParams.width=UtilityClass.screenWidth/3
            imageView.layoutParams.height=UtilityClass.screenWidth/3
        }
    }

    private fun loadImageToImageView(xPosition: Int,yPosition: Int,imageView: ImageView){
        val position = 3*yPosition+xPosition
        if(cache.get(position)!=null)
            imageView.setImageBitmap(cache.get(position))
        else
        {
            if(yPosition<imageMap.size){
                val hashMap = imageMap[yPosition]
                if(hashMap?.containsKey(xPosition) == true)
                    UtilityClass.loadImageToImageView(hashMap[xPosition]!!,imageView,this,xPosition,yPosition)
                /*{

                    val bitmap = BitmapFactory.decodeFile()
                    addImageToLruCache(bitmap,xPosition,yPosition)
                    Handler.handler.post(Runnable {
                        imageView.setImageBitmap(bitmap)
                        imageView.scaleType=ImageView.ScaleType.FIT_CENTER
                    })
                }*/
            }
        }
    }

    override fun addImageToLruCache(bitmap: Bitmap, xPosition: Int, yPosition: Int){
        cache.put(3*yPosition+xPosition,bitmap)
    }

     fun addImage(imageMetadata:ImageMetadata,bitmap: Bitmap,xPosition:Int,yPosition:Int){
         Log.d(TAG,"Image map is size is $yPosition and $xPosition")
        lateinit var hashMap: HashMap<Int,ImageMetadata>
        if (!imageMap.containsKey(yPosition))
            hashMap = HashMap()
        else
            hashMap = imageMap[yPosition]!!
        hashMap[xPosition] = imageMetadata
        imageMap[yPosition]=hashMap
        addImageToLruCache(bitmap,xPosition,yPosition)
        Log.d(TAG,"Image map size is ${imageMap.size} and inserted pos is ${3*yPosition+xPosition}")
        //notifyItemRangeChanged (int positionStart,int itemCount)
         //notifyItemChanged(3*yPosition+xPosition)
         notifyItemChanged(yPosition)
         //notifyDataSetChanged()
    }
}
