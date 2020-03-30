package edu.gwu.androidtweets

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import com.google.android.gms.tasks.Task
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.*
import java.lang.Exception

class MainActivity : AppCompatActivity() {

    /*

    We use `lateinit var` for our UI variables because they cannot be initialized until
    setContentView(...) is called in onCreate(...) below.

    For example, this line would cause a crash:
        private val username: EditText = findViewById(R.id.username)

    Alternatively, could also use a nullable variable, but it'd be inconvenient to do a null-check on each usage:
        private var username: EditText? = null

    So `lateinit var` acts as a "promise" to the compiler that we cannot initialize the variable right now,
    but we will later *and* when we do it'll be non-null.

    If you forget to initialize a `lateinit` and then try and use it, the app will crash.

    */

    private lateinit var firebaseAuth: FirebaseAuth

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    private lateinit var username: EditText

    private lateinit var password: EditText

    private lateinit var login: Button

    private lateinit var signUp: Button

    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        firebaseAuth = FirebaseAuth.getInstance()
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        val preferences: SharedPreferences = getSharedPreferences(
            "android-tweets",
            Context.MODE_PRIVATE
        )

        // Tells Android which layout file should be used for this screen.
        setContentView(R.layout.activity_main)

        // The IDs we are using here should match what was set in the "id" field in our XML layout
        // Note: findViewById only works here because we've already called setContentView above.
        username = findViewById(R.id.username)
        password = findViewById(R.id.password)
        login = findViewById(R.id.login)
        signUp = findViewById(R.id.signUp)
        progressBar = findViewById(R.id.progressBar)

        signUp.setOnClickListener {
            firebaseAnalytics.logEvent("signup_clicked", null)

            // Save user credentials to file
            val inputtedUsername: String = username.text.toString()
            val inputtedPassword: String = password.text.toString()

            firebaseAuth
                .createUserWithEmailAndPassword(inputtedUsername, inputtedPassword)
                .addOnCompleteListener { task: Task<AuthResult> ->
                    if (task.isSuccessful) {
                        firebaseAnalytics.logEvent("signup_success", null)

                        val currentUser: FirebaseUser = firebaseAuth.currentUser!!
                        val email = currentUser.email

                        Toast.makeText(
                            this,
                            "Registered successfully as $email!",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        firebaseAnalytics.logEvent("signup_failed", null)

                        val exception = task.exception!!
                        Toast.makeText(
                            this,
                            "Failed to register: $exception!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }

        // Using a lambda to implement a View.OnClickListener interface. We can do this because
        // an OnClickListener is an interface that only requires *one* function.
        login.setOnClickListener { view: View ->
            Log.d("MainActivity", "onClick() called")

            // Save user credentials to file
            val inputtedUsername: String = username.text.toString()
            val inputtedPassword: String = password.text.toString()

            firebaseAuth
                .signInWithEmailAndPassword(inputtedUsername, inputtedPassword)
                .addOnCompleteListener { task: Task<AuthResult> ->
                    firebaseAnalytics.logEvent("login_clicked", null)

                    if (task.isSuccessful) {
                        firebaseAnalytics.logEvent("login_success", null)

                        val currentUser: FirebaseUser = firebaseAuth.currentUser!!
                        val email = currentUser.email

                        Toast.makeText(
                            this,
                            "Signed in as $email!",
                            Toast.LENGTH_SHORT
                        ).show()

                        preferences
                            .edit()
                            .putString("username", inputtedUsername)
                            .putString("password", inputtedPassword)
                            .apply()

                        // An Intent is used to start a new Activity and also send data to it (via `putExtra(...)`)
                        val intent: Intent = Intent(this, MapsActivity::class.java)
                        startActivity(intent)
                    } else {
                        val exception = task.exception!!
                        val errorType = if (exception is FirebaseAuthInvalidCredentialsException) {
                            "invalid_credentials"
                        } else {
                            "unknown_error"
                        }

                        // Track an analytic with the specific failure reason
                        val bundle = Bundle()
                        bundle.putString("error_type", errorType)
                        firebaseAnalytics.logEvent("login_failed", bundle)

                        Toast.makeText(
                            this,
                            "Failed to sign in: $exception!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }

        // Kotlin shorthand for login.setEnabled(false).
        // If getter / setter is unambiguous, Kotlin lets you use the dot-style syntax
        login.isEnabled = false

        username.addTextChangedListener(textWatcher)
        password.addTextChangedListener(textWatcher)

        val savedUsername = preferences.getString("username", "")
        val savedPassword = preferences.getString("password", "")

        // By calling setText now, *after* having called addTextChangedListener above, causes my TextWatcher
        // code to execute. This is useful because it runs the logic to enable / disable the Login button,
        // so that it will be enabled if I fill the username / password from SharedPreferences.
        username.setText(savedUsername)
        password.setText(savedPassword)
    }

    // Another example of explicitly implementing an interface (TextWatcher). We cannot use
    // a lambda in this case since there are multiple (3) functions we need to implement.
    //
    // We're defining an "anonymous class" here using the `object` keyword (basically created
    // a new, dedicated object to implement a TextWatcher for this variable assignment.
    private val textWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable) {}

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            // username.text == Kotlin shorthand for username.getText()
            val inputtedUsername: String = username.text.toString()
            val inputtedPassword: String = password.text.toString()
            val enable: Boolean = inputtedUsername.trim().isNotEmpty() && inputtedPassword.trim().isNotEmpty()

            login.isEnabled = enable
        }
    }
}
