package com.animecharacter.models

data class DailyReport(
    val newFollowers: Int = 0,
    val newLikes: Int = 0,
    val newComments: Int = 0,
    val newShares: Int = 0,
    val newViews: Int = 0,
    val encouragementMessage: String = ""
)


