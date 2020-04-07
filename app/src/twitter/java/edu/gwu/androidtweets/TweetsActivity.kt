package edu.gwu.androidtweets

import android.location.Address
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isGone
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import org.jetbrains.anko.doAsync
import java.util.ArrayList

class TweetsActivity : AppCompatActivity() {

    private lateinit var tweetContent: EditText

    private lateinit var addTweet: FloatingActionButton

    private lateinit var recyclerView: RecyclerView

    private lateinit var firebaseDatabase: FirebaseDatabase

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    private val currentTweets: MutableList<Tweet> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tweets)

        firebaseDatabase = FirebaseDatabase.getInstance()
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        tweetContent = findViewById(R.id.tweet_content)
        addTweet = findViewById(R.id.add_tweet)

        val currentAddress: Address = intent.getParcelableExtra(IntentKeys.KEY_ADDRESS)

        title = getString(R.string.tweets_title, currentAddress.getAddressLine(0))

        recyclerView = findViewById(R.id.recyclerView)

        // Set the RecyclerView direction to vertical (the default)
        recyclerView.layoutManager = LinearLayoutManager(this)

        if (savedInstanceState != null) {
            Log.d("TweetsActivity", "After rotation - Using saved Tweets")

            val savedTweets: List<Tweet> = savedInstanceState.getSerializable("TWEETS") as List<Tweet>
            currentTweets.addAll(savedTweets)

            val adapter = TweetAdapter(currentTweets)
            recyclerView.adapter = adapter
        } else {
            Log.d("TweetsActivity", "First time - getting Tweets from Twitter")
            getTweetsFromTwitter(currentAddress)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable("TWEETS", ArrayList(currentTweets))
    }

    /**
     * Retrieves Tweets from Twitter using the Twitter API. Tweets are retrieved by searching for the
     * word "Android" nearby the passed [Address].
     */
    fun getTweetsFromTwitter(currentAddress: Address) {
        addTweet.hide()
        tweetContent.isGone = true

        // Networking is required to happen on a background thread, especially since server response
        // times can be long if the user has a poor internet connection.
        doAsync {
            val twitterManager = TwitterManager()

            try {
                // Read our Twitter API key and secret from our XML file.
                val apiKey = getString(R.string.twitter_key)
                val apiSecret = getString(R.string.twitter_secret)

                // Get an OAuth token -- all of Twitter's APIs are protected by OAuth
                val oAuthToken = twitterManager.retrieveOAuthToken(
                    apiKey = apiKey,
                    apiSecret = apiSecret
                )

                // Use the OAuth token to call the Search Tweets API
                val tweets = twitterManager.retrieveTweets(
                    oAuthToken = oAuthToken,
                    latitude = currentAddress.latitude,
                    longitude = currentAddress.longitude
                )

                currentTweets.clear()
                currentTweets.addAll(tweets)

                firebaseAnalytics.logEvent("tweets_retrieval_success_twitter", null)

                // Switch back to the UI Thread (required to update the UI)
                runOnUiThread {
                    val adapter = TweetAdapter(tweets)
                    recyclerView.adapter = adapter
                }
            } catch (exception: Exception) {
                firebaseAnalytics.logEvent("tweets_retrieval_failed_twitter", null)
                exception.printStackTrace()
                // Switch back to the UI Thread (required to update the UI)
                runOnUiThread {
                    Toast.makeText(
                        this@TweetsActivity,
                        "Failed to retrieve Tweets",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}