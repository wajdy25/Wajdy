package com.animecharacter.services

import android.content.Context
import android.util.Log
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.gson.Gson
import com.animecharacter.models.TrendingTopicResponse

class TrendingTopicsService(private val context: Context) {

    private val TAG = "TrendingTopicsService"
    private val API_KEY = "YOUR_SEARCHAPI_KEY" // TODO: Replace with actual API key
    private val BASE_URL = "https://www.searchapi.io/api/v1/search"

    fun getTrendingTopics(geo: String = "US", time: String = "past_24_hours", callback: (List<TrendingTopicResponse.Trend>?) -> Unit) {
        val url = "$BASE_URL?engine=google_trends_trending_now&geo=$geo&time=$time&api_key=$API_KEY"

        val stringRequest = StringRequest(Request.Method.GET, url,
            { response ->
                try {
                    val gson = Gson()
                    val trendingTopicResponse = gson.fromJson(response, TrendingTopicResponse::class.java)
                    callback(trendingTopicResponse.trends)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing JSON: ${e.message}")
                    callback(null)
                }
            },
            { error ->
                Log.e(TAG, "Error fetching trending topics: ${error.message}")
                callback(null)
            })

        Volley.newRequestQueue(context).add(stringRequest)
    }
}


