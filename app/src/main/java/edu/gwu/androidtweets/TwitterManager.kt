package edu.gwu.androidtweets

import android.util.Base64
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder

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

    /**
     * Twitter requires us to encoded our API Key and API Secret in a special way for the request.
     * https://developer.twitter.com/en/docs/basics/authentication/oauth-2-0/application-only
     */
    private fun encodeSecrets(apiKey: String, apiSecret: String): String {
        // Encoding for a URL -- converts things like spaces into %20
        val encodedKey = URLEncoder.encode(apiKey, "UTF-8")
        val encodedSecret = URLEncoder.encode(apiSecret, "UTF-8")

        // Concatenate the two together, with a colon inbetween
        val combinedEncoded = "$encodedKey:$encodedSecret"

        // Base-64 encode the combined string
        // https://en.wikipedia.org/wiki/Base64
        val base64Combined = Base64.encodeToString(
            combinedEncoded.toByteArray(), Base64.NO_WRAP)

        return base64Combined
    }

    /**
     * All of Twitter's APIs are also protected by OAuth.
     */
    fun retrieveOAuthToken(apiKey: String, apiSecret: String): String {
        // Twitter requires us to encoded our API Key and API Secret in a special way for the request.
        val encodedSecrets = encodeSecrets(apiKey, apiSecret)

        // OAuth is defined to be a POST call, which has a specific body / payload to let the server
        // know we are doing "application-only" OAuth (e.g. we will only access public information)
        val requestBody = "grant_type=client_credentials"
            .toRequestBody(
                contentType = "application/x-www-form-urlencoded".toMediaType()
            )

        // Build the request
        // The encoded secrets become a header on the request
        val request = Request.Builder()
            .url("https://api.twitter.com/oauth2/token")
            .header("Authorization", "Basic $encodedSecrets")
            .post(requestBody)
            .build()

        // "Execute" the request (.execute will block the current thread until the server replies with a response)
        val response = okHttpClient.newCall(request).execute()

        // Create an empty, mutable list to hold up the Tweets we will parse from the JSON
        val responseString: String? = response.body?.string()

        // If the response was successful (e.g. status code was a 200) AND the server sent us back
        // some JSON (which will contain the OAuth token), then we can go ahead and parse the JSON body.
        return if (!responseString.isNullOrEmpty() && response.isSuccessful) {
            val json: JSONObject = JSONObject(responseString)
            json.getString("access_token")
        } else {
            ""
        }
    }

    /**
     * Retrieves Tweets containing the word "Android" in a roughly 30 mile radius around the
     * GPS coordinates that are passed.
     *
     * The Search Tweets API is also protected by OAuth, so a token is required.
     */
    fun retrieveTweets(oAuthToken: String, latitude: Double, longitude: Double): List<Tweet> {
        val searchTerm = "Android"
        val radius = "30mi"

        // Build the request
        // The OAuth token becomes a header on the request
        val request = Request.Builder()
            .url("https://api.twitter.com/1.1/search/tweets.json?q=$searchTerm&geocode=$latitude,$longitude,$radius")
            .header("Authorization", "Bearer $oAuthToken")
            .build()

        // "Execute" the request (.execute will block the current thread until the server replies with a response)
        val response = okHttpClient.newCall(request).execute()

        // Create an empty, mutable list to hold up the Tweets we will parse from the JSON
        val tweets: MutableList<Tweet> = mutableListOf()

        // Get the JSON body from the response (if it exists)
        val responseString: String? = response.body?.string()

        // If the response was successful (e.g. status code was a 200) AND the server sent us back
        // some JSON (which will contain the Tweets), then we can go ahead and parse the JSON body.
        if (!responseString.isNullOrEmpty() && response.isSuccessful) {
            // The list of Tweets will be within the statuses array
            val json: JSONObject = JSONObject(responseString)
            val statuses: JSONArray = json.getJSONArray("statuses")

            // Loop thru the statuses array and parse each individual list, adding it to our `tweets`
            // list which we will return at the end.
            for (i in 0 until statuses.length()) {
                val curr = statuses.getJSONObject(i)
                val content = curr.getString("text")

                val user = curr.getJSONObject("user")
                val name = user.getString("name")
                val handle = user.getString("screen_name")
                val profilePictureUrl = user.getString("profile_image_url_https")

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