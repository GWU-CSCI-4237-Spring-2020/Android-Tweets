package edu.gwu.androidtweets

data class Tweet(
    val username: String,
    val handle: String,
    val content: String,
    val iconUrl: String
) {
    // Required by Firebase DB when using getValue on data retrieved from the DB
    constructor() : this("", "", "", "")
}