package com.gecko.canvass.activity

import android.database.MatrixCursor
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.widget.AutoCompleteTextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.cursoradapter.widget.CursorAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gecko.canvass.R
import com.gecko.canvass.adapters.SearchSuggestionsAdapter
import com.gecko.canvass.adapters.StoreImagesListAdapter
import com.gecko.canvass.custom.ImageMetadata
import com.gecko.canvass.interfaces.DownloadListener
import com.gecko.canvass.interfaces.SearchSuggestionsInterface
import com.gecko.canvass.logic.ImageLoader
import com.gecko.canvass.logic.Searcher
import com.gecko.canvass.utility.Handler
import com.gecko.canvass.utility.IMAGE_LOADER_CURATE_MODE
import com.gecko.canvass.utility.UtilityClass
import org.json.JSONArray


class HomeActivity:AppCompatActivity(),DownloadListener,SearchSuggestionsInterface {

    private lateinit var storeImagesRecyclerView:RecyclerView
    private lateinit var storeImagesListAdapter: StoreImagesListAdapter
    private val TAG = "HomeActivity"
    private lateinit var imageLoader:ImageLoader
    private lateinit var searchView:SearchView
    private lateinit var searchViewAdapter: SearchSuggestionsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        UtilityClass.reportException()
        UtilityClass.applicationContext = applicationContext//we set and hold a reference to application context from UtilityClass
        UtilityClass.context = this
        setContentView(R.layout.home_layout)
        searchView = findViewById(R.id.search_view)
        /**
         * searchview doesn't display suggestions for 1 character. this is a fix
         */
        //androidx.appcompat.R.id.search_src_text
        //val autoCompleteTextViewID: Int = resources.getIdentifier("android:id/search_src_text", null, null)
        val searchAutoCompleteTextView = searchView.findViewById(androidx.appcompat.R.id.search_src_text) as AutoCompleteTextView
        searchAutoCompleteTextView.threshold = 1
        //setSupportActionBar(findViewById(R.id.toolbar))
        storeImagesRecyclerView = findViewById(R.id.images)
        //btRecyclerView.visibility = RecyclerView.VISIBLE
        storeImagesListAdapter = StoreImagesListAdapter()
        //wifiAdapter.setContext(this@WifiSelectionActivity)
        storeImagesRecyclerView.adapter = storeImagesListAdapter
        //storeImagesRecyclerView.layoutManager = GridLayoutManager(this,3)
        storeImagesRecyclerView.layoutManager = LinearLayoutManager(this)
        //implement scroll listener
        /*storeImagesRecyclerView.addOnScrollListener(object: RecyclerView.OnScrollListener(){
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                Log.d(TAG,"Scrolled by DX $dx and DY $dy")
            }
        })*/
        //implement what happens upon typing in search view
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener
        {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText != null) {
                    Searcher.searchForSuggestions(newText,this@HomeActivity)
                }
                return true
            }
        })

        //define what happens when the user selects one of the suggestions
        searchView.setOnSuggestionListener(object : SearchView.OnSuggestionListener{
            override fun onSuggestionSelect(position: Int): Boolean {
                return false
            }

            override fun onSuggestionClick(position: Int): Boolean {
                searchViewAdapter.cursor.moveToPosition(position)
                val searchText = searchViewAdapter.cursor.getString(1)
                Log.d(TAG,"String to search for is $searchText")
                val bundle = Bundle()
                bundle.putString("search_text",searchText)
                UtilityClass.startActivity(this@HomeActivity,SearchResultsActivity::class.java,bundle)
                return true
            }
        })

        imageLoader = ImageLoader(this, IMAGE_LOADER_CURATE_MODE)
        storeImagesListAdapter.imageLoader = imageLoader
        imageLoader.fetchImageJSONFromPexel()
    }

    override fun downloadCompleted(imageMetadata: ImageMetadata, bitmap: Bitmap, xPosition: Int, yPosition: Int) {
        storeImagesListAdapter.addImage(imageMetadata,bitmap,xPosition,yPosition)
    }

    override fun onDestroy() {
        super.onDestroy()
        imageLoader.releaseDownloadListenerReference()
    }

    override fun onSuggestionsAvailable(sugs: JSONArray) {
        Handler.handler.post(Runnable {
            val columns = arrayOf("_id","suggs")
            val matrixCursor = MatrixCursor(columns)
            for (pos in 0 until sugs.length())
            {
                val json = sugs.getJSONObject(pos)
                Log.d(TAG,json.getString("word"))
                matrixCursor.addRow(arrayOf(pos,json.getString("word")))
            }
            if(!::searchViewAdapter.isInitialized)
            {
                searchViewAdapter = SearchSuggestionsAdapter(this,matrixCursor,CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER)
                searchView.suggestionsAdapter = searchViewAdapter
            }
            else
                searchViewAdapter.changeCursor(matrixCursor)
            searchViewAdapter.notifyDataSetChanged()
        })
    }
}