package com.gecko.canvass.logic

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import com.gecko.canvass.custom.ImageMetadata
import com.gecko.canvass.interfaces.DownloadListener
import com.gecko.canvass.utility.*
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.lang.StringBuilder
import java.net.URL
import java.net.URLConnection
import java.util.concurrent.atomic.AtomicInteger
import javax.net.ssl.HttpsURLConnection
import kotlin.math.abs

class ImageLoader(dListener: DownloadListener,mode:Int) {

    private val TAG = "ImageLoader"
    private  var downloadListener:DownloadListener? = null
    private var nextPage:String=""
    private var xPos=0
    private var yPos = 0
    private val MODE = mode
    private var isFetchingNextImage = false

    init{
        downloadListener = dListener
    }

    fun fetchImageJSONFromPexel(){
        //if we are not currently fetching the next set of images, then we can proceed
        if(!isFetchingNextImage){

            ThreadPool.postTask(Runnable {
                //lateinit var client: HttpsURLConnection
                //lateinit var inputStream: InputStream
                Log.d(TAG,"Fetching Pexel JSON")
                if(shouldVisitNextPage()){
                    val url = URL(nextPage)
                    val response = UtilityClass.fetchJSONFromURL(url)
                    parseImageJSON(JSONObject(response))
                }
            })
        }
    }

    fun fetchNextImage(){
        if(!isFetchingNextImage){
            ThreadPool.postTask(Runnable {
                if(nextPage!="EOF")
                {
                    val url = URL(nextPage)
                    val response = UtilityClass.fetchJSONFromURL(url)
                    parseImageJSON(JSONObject(response))
                }
            })
        }
    }

    private fun shouldVisitNextPage():Boolean{
        nextPage = if(nextPage.isEmpty())"${PEXEL_URL}curated?per_page=$PAGE_COUNT&page=1" else nextPage
        return (nextPage !="EOF")
    }

    fun fetchImagesForSearch(text:String){
        if(!isFetchingNextImage){
            ThreadPool.postTask(Runnable {
                Log.d(TAG,"Fetching Pexel Search JSON")
                val url = URL(if(nextPage=="")"${PEXEL_URL}search?per_page=$PAGE_COUNT&page=1&query=$text" else nextPage)
                val response = UtilityClass.fetchJSONFromURL(url)
                parseImageJSON(JSONObject(response))
                /*val obj = JSONObject(response)
                nextPage = if(obj.has("next_page"))obj.getString("next_page")else ""
                val array = obj.getJSONArray("photos")
                val perPage = array.length() //obj.getInt("per_page")//returns the number of items on the current page
                Log.d(TAG,"Total Images Returned : ${array.length()}")*/
            })
        }
    }

    private fun parseImageJSON(obj:JSONObject){
        isFetchingNextImage = true
        nextPage = if(obj.has("next_page"))obj.getString("next_page")else "EOF"
        val array = obj.getJSONArray("photos")
        val perPage = array.length() //obj.getInt("per_page")//returns the number of items on the current page
        Log.d(TAG,"Total Images Returned : ${array.length()}")
        lateinit var photo:String
        for(pos in 0 until perPage){
            photo = array.getJSONObject(pos).getJSONObject("src").getString("original")
            when(MODE){
                IMAGE_LOADER_CURATE_MODE->{
                    Log.d(TAG,"Position : $pos")
                    Log.d(TAG,"Image at $xPos and $yPos is $photo")
                    downloadThumbnail(photo,xPos,yPos)
                    Log.d(TAG,"Downloaded Image for  : X $xPos and Y $yPos")
                    xPos++
                    yPos += xPos / 3
                    xPos%=3
                }
                IMAGE_LOADER_SEARCH_MODE->{
                    downloadImage(photo,"?auto=compress&cs=tinysrgb&dpr=1&fit=crop&h=750&w=750", IMAGES_FROM_SEARCH_DIR,-1,pos*-1)
                }
            }
        }
        isFetchingNextImage = false
    }

    private fun getWidth(xPos:Int,yPos: Int):Int{
        var width = (UtilityClass.screenWidth/3)
        if((yPos%3) == 2 && xPos==0)//if we are downloading an element of a row which is a multiple of 3 and we are downloading the first element then, width/height is 2X
            width*=2
        return width
    }

    private fun downloadThumbnail(url: String,xPos: Int,yPos: Int){
        val width = getWidth(xPos,yPos)
        downloadImage(url,"?auto=compress&cs=tinysrgb&dpr=1&fit=crop&h=$width&w=$width", THUMBNAIL_DIR,xPos,yPos)
    }

    fun downloadImage(url:String,width:Int,height:Int,folder:String){
        downloadImage(url,"?auto=compress&cs=tinysrgb&dpr=1&fit=crop&h=$height&w=$width",folder,-1,-1)
    }

    private fun downloadImage(url:String,compression:String,folder: String,xPos: Int,yPos: Int){
        ThreadPool.postTask(Runnable {
            Log.d(TAG,"Downloading image : $url")
            val connection = URL("$url$compression").openConnection() as HttpsURLConnection
            connection.connect()
            val responseCode = connection.responseCode
            val inputStream = connection.inputStream
            val data = ByteArray(8*1024)
            var size = 0
            val fileName = "${abs(xPos)}${abs(yPos)}.jpeg"
            val file = File("${(downloadListener as Context).filesDir}/$folder/",fileName)
            file.createNewFile()
            val fileOutputStream = FileOutputStream(file)
            try {
                when(responseCode){
                    HttpsURLConnection.HTTP_OK->{
                        while (size!=-1){
                            size = inputStream.read(data,0,data.size)
                            if(size!=-1){
                                fileOutputStream.write(data,0,size)
                                fileOutputStream.flush()
                            }
                        }
                        Log.d(TAG,"Download of image successful $url")
                        Log.d(TAG,"About to load downloaded file as bitmap ${file.absolutePath}")
                        loadBitmap(file.absolutePath,url,xPos,yPos)
                    }
                }
            }
            catch (e:java.lang.Exception){
                Log.d(TAG,"${e.printStackTrace()}")
            }
            finally {
                fileOutputStream.close()
                connection.disconnect()
                inputStream.close()
            }
        })
    }

    private fun loadBitmap(file:String,url: String,xPos: Int,yPos: Int){
        Log.d(TAG,"Loaded Image for X $xPos and Y $yPos")
        val imageMetadata = ImageMetadata()
        imageMetadata.imageUrl = url
        imageMetadata.imageLocation = file
        Log.d(TAG,"File to decode to bitmap $file")
        val bitmap = BitmapFactory.decodeFile(file)
        Handler.handler.post(Runnable {
            if(xPos<0 && yPos<0){
                if(bitmap == null)
                    downloadImage(url,UtilityClass.screenWidth,UtilityClass.screenHeight,FULL_SIZED_IMAGES_DIR)
                else
                    downloadListener?.downloadCompleted(imageMetadata,bitmap)
            }
            else{
                if(bitmap==null)
                    downloadThumbnail(url,xPos,yPos)
                else
                    downloadListener?.downloadCompleted(imageMetadata,bitmap,xPos,yPos)
            }
        })
    }

    fun releaseDownloadListenerReference(){
        downloadListener = null
    }
}