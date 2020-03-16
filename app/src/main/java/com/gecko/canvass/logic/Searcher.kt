package com.gecko.canvass.logic

import com.gecko.canvass.interfaces.SearchSuggestionsInterface
import com.gecko.canvass.utility.*
import org.json.JSONArray
import java.net.URL

object Searcher {
    fun searchForSuggestions(text:String,suggestionsInterface: SearchSuggestionsInterface){
        ThreadPool.postTask(Runnable {
            val url = URL("${DATA_MUSE_URL}sug?s=$text&max=4")
            val response  = UtilityClass.fetchJSONFromURL(url)
            suggestionsInterface.onSuggestionsAvailable(JSONArray(response))
        })
    }
}