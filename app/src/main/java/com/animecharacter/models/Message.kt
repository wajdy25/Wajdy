package com.animecharacter.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Message(
    val text: String,
    val isFromUser: Boolean,
    val timestamp: Long,
    val id: String = generateId()
) : Parcelable {
    
    companion object {
        private fun generateId(): String {
            return System.currentTimeMillis().toString() + (0..999).random()
        }
    }
    
    fun getFormattedTime(): String {
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = timestamp
        
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val minute = calendar.get(java.util.Calendar.MINUTE)
        
        return String.format("%02d:%02d", hour, minute)
    }
    
    fun isToday(): Boolean {
        val today = java.util.Calendar.getInstance()
        val messageDate = java.util.Calendar.getInstance()
        messageDate.timeInMillis = timestamp
        
        return today.get(java.util.Calendar.YEAR) == messageDate.get(java.util.Calendar.YEAR) &&
                today.get(java.util.Calendar.DAY_OF_YEAR) == messageDate.get(java.util.Calendar.DAY_OF_YEAR)
    }
}

