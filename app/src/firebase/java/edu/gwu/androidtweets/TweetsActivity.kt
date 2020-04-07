package edu.gwu.androidtweets

import android.location.Address
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
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
            getTweetsFromFirebase(currentAddress)
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
}