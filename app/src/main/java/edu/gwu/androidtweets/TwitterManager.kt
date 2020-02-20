package edu.gwu.androidtweets

import com.google.android.gms.maps.model.LatLng
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class TwitterManager {

    private val okHttpClient: OkHttpClient

    init {
        val builder = OkHttpClient.Builder()

        // Set up our OkHttpClient instance to log all network traffic to Logcat
        val loggingInterceptor = HttpLoggingInterceptor()
        loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
        builder.addInterceptor(loggingInterceptor)

        builder.connectTimeout(15, TimeUnit.SECONDS)
        builder.readTimeout(15, TimeUnit.SECONDS)
        builder.writeTimeout(15, TimeUnit.SECONDS)

        okHttpClient = builder.build()
    }

    fun retrieveTweets(latitude: Double, longitude: Double): List<Tweet> {
        val searchTerm = "Android"
        val radius = "30mi"

        val request = Request.Builder()
            .url("https://api.twitter.com/1.1/search/tweets.json?q=$searchTerm&geocode=$latitude,$longitude,$radius")
            .header("Authorization", "Bearer AAAAAAAAAAAAAAAAAAAAAJ6N8QAAAAAABppHnTpssd0Hrsdpsi6vYN%2BTfks%3DFY1iVemJdKF5HWRZhQnHRbGpwXJevg3sYyvYC3R53sHCfOJvFk")
            .build()

        val response = okHttpClient.newCall(request).execute()

        val tweets: MutableList<Tweet> = mutableListOf()
        val responseString: String? = response.body?.string()

        if (!responseString.isNullOrEmpty() && response.isSuccessful) {
            val json: JSONObject = JSONObject(responseString)
            val statuses: JSONArray = json.getJSONArray("statuses")

            for (i in 0 until statuses.length()) {
                val curr = statuses.getJSONObject(i)
                val content = curr.getString("text")

                val user = curr.getJSONObject("user")
                val name = user.getString("name")
                val handle = user.getString("screen_name")
                val profilePictureUrl = user.getString("profile_image_url")

                val tweet = Tweet(
                    username = name,
                    handle = handle,
                    content = content,
                    iconUrl = profilePictureUrl
                )

                tweets.add(tweet)
            }
        }

        return tweets
    }
}