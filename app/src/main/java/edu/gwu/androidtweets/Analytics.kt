package edu.gwu.androidtweets

object Analytics {
    object Login {
        val CLICKED = "login_clicked"
        val SUCCESS = "login_success"
        val FAILED = "login_failed"
        val ERROR_BUNDLE_KEY = "error_type"
        val ERROR_INVALID_CREDENTIALS = "invalid_credentials"
        val ERROR_UNKNOWN = "unknown_error"
    }
    object SignUp {
        val CLICKED = "signup_clicked"
        val SUCCESS = "signup_success"
        val FAILED = "signup_failed"
    }
    object Location {
        val CURRENT_LOCATION_CLICKED = "current_location_clicked"
        val RETRIEVED = "location_retrieved"
        val PERMISSION_NEEDED = "permission_location_needed"
        val PERMISSION_GRANTED = "permission_location_granted"
        val PERMISSION_ALREADY_GRANTED = "permission_location_already_granted"
        val PERMISSION_DENIED = "permission_location_denied"
    }
    object Geocoder {
        val SUCCESS = "location_geocoded"
        val NO_RESULTS = "location_no_geocode_results"
    }
}