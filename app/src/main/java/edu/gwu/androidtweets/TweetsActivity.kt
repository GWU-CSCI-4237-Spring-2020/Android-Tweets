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

        val currentAddress: Address = intent.getParcelableExtra("address")

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
//        getTweetsFromFirebase(currentAddress)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable("TWEETS", ArrayList(currentTweets))
    }

    /**
     * Retrieves "Tweets" from our Firebase DB, where Tweets are grouped by the U.S. State (or,
     * more broadly by a country's "administrative area" as defined by the [Address] class).
     *
     * e.g. all users in Texas would see the same "Tweets" - just as a fun example
     */
    fun getTweetsFromFirebase(currentAddress: Address) {
        // Get a "reference" into the DB -- this the path / location in the DB where we will read / write data
        val state: String = currentAddress.adminArea ?: "unknown"
        val reference = firebaseDatabase.getReference("tweets/$state")

        addTweet.setOnClickListener {
            firebaseAnalytics.logEvent("add_tweet_clicked", null)

            // .push() generates a unique random ID at our reference - so that each Tweet can have
            // a unique ID associated with it and form a list of Tweets under each state
            val newTweetReference = reference.push()

            val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
            val email: String = firebaseAuth.currentUser!!.email!!
            val inputtedContent: String = tweetContent.text.toString()

            val tweet = Tweet(
                username = email,
                handle = email,
                content = inputtedContent,
                iconUrl = ""
            )

            // setValue actually does the writing of the data to Firebase
            newTweetReference.setValue(tweet)
        }

        // Set a listener on our reference to read the data there and also get a callback whenever
        // the data at the reference changes
        reference.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(databaseError: DatabaseError) {
                // DB disconnected - maybe a network connection issue
                firebaseAnalytics.logEvent("tweets_retrieval_failed_firebase", null)
                Toast.makeText(
                    this@TweetsActivity,
                    "Network error with database: ${databaseError.message}",
                    Toast.LENGTH_LONG
                ).show()
            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // Data was retrieved from the DB - either because we just connected to it *or* because
                // the data has changed (e.g. the real-time updates)
                firebaseAnalytics.logEvent("tweets_retrieval_success_firebase", null)

                val tweets = mutableListOf<Tweet>()

                // Loop thru each Tweet under the reference (U.S. state)
                dataSnapshot.children.forEach { data: DataSnapshot ->
                    // Cast each piece of data (a Tweet) to a Tweet object
                    val tweet: Tweet? = data.getValue(Tweet::class.java)
                    if (tweet != null) {
                        tweets.add(tweet)
                    }
                }

                val adapter = TweetAdapter(tweets)
                recyclerView.adapter = adapter
            }
        })
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

    fun getFakeTweets(): List<Tweet> {
        return listOf(
            Tweet(
                handle = "@nickcapurso",
                username = "Nick Capurso",
                content = "We're learning lists!",
                iconUrl = "https://...."
            ),
            Tweet(
                username = "Android Central",
                handle = "@androidcentral",
                content = "NVIDIA Shield TV vs. Shield TV Pro: Which should I buy?",
                iconUrl = "https://...."
            ),
            Tweet(
                username = "DC Android",
                handle = "@DCAndroid",
                content = "FYI - another great integration for the @Firebase platform",
                iconUrl = "https://...."
            ),
            Tweet(
                username = "KotlinConf",
                handle = "@kotlinconf",
                content = "Can't make it to KotlinConf this year? We have a surprise for you. We'll be live streaming the keynotes, closing panel and an entire track over the 2 main conference days. Sign-up to get notified once we go live!",
                iconUrl = "https://...."
            ),
            Tweet(
                username = "Android Summit",
                handle = "@androidsummit",
                content = "What a #Keynote! @SlatteryClaire is the Director of Performance at Speechless, and that's exactly how she left us after her amazing (and interactive!) #keynote at #androidsummit. #DCTech #AndroidDev #Android",
                iconUrl = "https://...."
            ),
            Tweet(
                username = "Fragmented Podcast",
                handle = "@FragmentedCast",
                content = ".... annnnnnnnnd we're back!\n\nThis week @donnfelker talks about how it's Ok to not know everything and how to set yourself up mentally for JIT (Just In Time [learning]). Listen in here: \nhttp://fragmentedpodcast.com/episodes/135/ ",
                iconUrl = "https://...."
            ),
            Tweet(
                username = "Jake Wharton",
                handle = "@JakeWharton",
                content = "Free idea: location-aware physical password list inside a password manager. Mostly for garage door codes and the like. I want to open my password app, switch to the non-URL password section, and see a list of things sorted by physical distance to me.",
                iconUrl = "https://...."
            ),
            Tweet(
                username = "Droidcon Boston",
                handle = "@droidconbos",
                content = "#DroidconBos will be back in Boston next year on April 8-9!",
                iconUrl = "https://...."
            ),
            Tweet(
                username = "AndroidWeekly",
                handle = "@androidweekly",
                content = "Latest Android Weekly Issue 327 is out!\nhttp://androidweekly.net/ #latest-issue  #AndroidDev",
                iconUrl = "https://...."
            ),
            Tweet(
                username = ".droidconSF",
                handle = "@droidconSF",
                content = "Drum roll please.. Announcing droidcon SF 2018! November 19-20 @ Mission Bay Conference Center. Content and programming by @tsmith & @joenrv.",
                iconUrl = "https://...."
            )
        )
    }
}