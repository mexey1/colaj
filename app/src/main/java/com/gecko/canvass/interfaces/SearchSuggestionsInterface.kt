package com.gecko.canvass.interfaces

import org.json.JSONArray

interface SearchSuggestionsInterface {
    fun onSuggestionsAvailable(sugs:JSONArray)
}