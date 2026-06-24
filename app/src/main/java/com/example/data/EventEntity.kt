package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val startTime: Long, // timestamp in ms
    val endTime: Long, // timestamp in ms
    val location: String = "",
    val color: Int = 0xFF3F51B5.toInt(), // Default material color
    val isGoogleEvent: Boolean = false,
    val googleEventId: String? = null,
    val calendarName: String = "Personal"
)
