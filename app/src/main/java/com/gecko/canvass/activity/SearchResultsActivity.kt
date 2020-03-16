package com.gecko.canvass.activity

import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gecko.canvass.R
import com.gecko.canvass.adapters.SearchResultsListAdapter
import com.gecko.canvass.custom.ImageMetadata
import com.gecko.canvass.interfaces.DownloadListener
import com.gecko.canvass.logic.ImageLoader
import com.gecko.canvass.utility.IMAGE_LOADER_SEARCH_MODE
import com.gecko.canvass.utility.UtilityClass

class SearchResultsActivity : AppCompatActivity(),DownloadListener{
    private val searchResultsListAdapter = SearchResultsListAdapter()
    private lateinit var searchResultsRecyclerView :RecyclerView
    private lateinit var imageLoader: ImageLoader

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        UtilityClass.reportException()
        setContentView(R.layout.search_result_activity)
        searchResultsRecyclerView = findViewById(R.id.results_recycler_view)
        searchResultsRecyclerView.adapter = searchResultsListAdapter
        searchResultsRecyclerView.layoutManager = LinearLayoutManager(this)
        val searchText = intent.getStringExtra("search_text")
        imageLoader = ImageLoader(this, IMAGE_LOADER_SEARCH_MODE)
        searchResultsListAdapter.imageLoader = imageLoader
        imageLoader.fetchImagesForSearch(searchText)
    }

    override fun downloadCompleted(image: ImageMetadata, bitmap: Bitmap) {
        searchResultsListAdapter.addImage(bitmap,image)
    }
}