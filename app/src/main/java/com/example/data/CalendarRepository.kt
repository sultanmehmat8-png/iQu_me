package com.example.data

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.CalendarContract
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.Calendar

class CalendarRepository(private val context: Context, private val eventDao: EventDao) {

    val allLocalEvents: Flow<List<EventEntity>> = eventDao.getAllEvents()

    suspend fun insert(event: EventEntity): Long = withContext(Dispatchers.IO) {
        eventDao.insertEvent(event)
    }

    suspend fun update(event: EventEntity) = withContext(Dispatchers.IO) {
        eventDao.updateEvent(event)
    }

    suspend fun delete(event: EventEntity) = withContext(Dispatchers.IO) {
        eventDao.deleteEvent(event)
    }

    // Insert dummy Google Calendar events to showcase the sync feature instantly if permissions aren't fully configured
    suspend fun populateSimulatedGoogleEvents() = withContext(Dispatchers.IO) {
        // Clear any previous simulated google events
        eventDao.clearGoogleEvents()

        val calendar = Calendar.getInstance()
        val today = calendar.timeInMillis

        // Event 1: Today + 1 hour
        calendar.set(Calendar.HOUR_OF_DAY, 10)
        calendar.set(Calendar.MINUTE, 0)
        val start1 = calendar.timeInMillis
        calendar.add(Calendar.HOUR, 1)
        val end1 = calendar.timeInMillis
        eventDao.insertEvent(
            EventEntity(
                title = "🚀 Team Sync (Google Calendar)",
                description = "Weekly sprint review synced from Google Calendar primary feed.",
                startTime = start1,
                endTime = end1,
                location = "Google Meet",
                color = 0xFF4285F4.toInt(), // Google Blue
                isGoogleEvent = true,
                googleEventId = "g_sync_101",
                calendarName = "work@gmail.com"
            )
        )

        // Event 2: Today + 3 hours
        calendar.set(Calendar.HOUR_OF_DAY, 14)
        calendar.set(Calendar.MINUTE, 30)
        val start2 = calendar.timeInMillis
        calendar.add(Calendar.HOUR_OF_DAY, 1)
        calendar.add(Calendar.MINUTE, 30)
        val end2 = calendar.timeInMillis
        eventDao.insertEvent(
            EventEntity(
                title = "🎨 Design System Alignment",
                description = "Aligning Material 3 UI design specifications with the dev team.",
                startTime = start2,
                endTime = end2,
                location = "Design Room 4",
                color = 0xFF34A853.toInt(), // Google Green
                isGoogleEvent = true,
                googleEventId = "g_sync_102",
                calendarName = "work@gmail.com"
            )
        )

        // Event 3: Tomorrow + 2 hours
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 11)
        calendar.set(Calendar.MINUTE, 0)
        val start3 = calendar.timeInMillis
        calendar.add(Calendar.HOUR, 2)
        val end3 = calendar.timeInMillis
        eventDao.insertEvent(
            EventEntity(
                title = "💻 PC App Architecture Review",
                description = "Reviewing desktop widgets performance and layout resizing profiles.",
                startTime = start3,
                endTime = end3,
                location = "HQ Conference Room B",
                color = 0xFFF4B400.toInt(), // Google Yellow
                isGoogleEvent = true,
                googleEventId = "g_sync_103",
                calendarName = "personal@gmail.com"
            )
        )
    }

    /**
     * Reads real Google calendar events from the device CalendarContract if permissions are granted.
     * Merges them as temporary Google events or loads them directly.
     */
    suspend fun syncWithDeviceCalendar(): List<EventEntity> = withContext(Dispatchers.IO) {
        val googleEvents = mutableListOf<EventEntity>()
        try {
            val contentResolver: ContentResolver = context.contentResolver
            val uri: Uri = CalendarContract.Events.CONTENT_URI
            val projection = arrayOf(
                CalendarContract.Events._ID,
                CalendarContract.Events.TITLE,
                CalendarContract.Events.DESCRIPTION,
                CalendarContract.Events.DTSTART,
                CalendarContract.Events.DTEND,
                CalendarContract.Events.EVENT_LOCATION,
                CalendarContract.Events.DISPLAY_COLOR,
                CalendarContract.Events.CALENDAR_DISPLAY_NAME
            )

            // Query events from last 30 days to next 30 days
            val now = System.currentTimeMillis()
            val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000
            val selection = "(${CalendarContract.Events.DTSTART} >= ?) AND (${CalendarContract.Events.DTSTART} <= ?)"
            val selectionArgs = arrayOf((now - thirtyDaysMs).toString(), (now + thirtyDaysMs).toString())

            val cursor: Cursor? = contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                "${CalendarContract.Events.DTSTART} ASC"
            )

            cursor?.use { c ->
                val idIdx = c.getColumnIndex(CalendarContract.Events._ID)
                val titleIdx = c.getColumnIndex(CalendarContract.Events.TITLE)
                val descIdx = c.getColumnIndex(CalendarContract.Events.DESCRIPTION)
                val startIdx = c.getColumnIndex(CalendarContract.Events.DTSTART)
                val endIdx = c.getColumnIndex(CalendarContract.Events.DTEND)
                val locIdx = c.getColumnIndex(CalendarContract.Events.EVENT_LOCATION)
                val colorIdx = c.getColumnIndex(CalendarContract.Events.DISPLAY_COLOR)
                val calNameIdx = c.getColumnIndex(CalendarContract.Events.CALENDAR_DISPLAY_NAME)

                while (c.moveToNext()) {
                    val id = if (idIdx >= 0) c.getLong(idIdx) else 0L
                    val title = if (titleIdx >= 0) c.getString(titleIdx) ?: "No Title" else "No Title"
                    val desc = if (descIdx >= 0) c.getString(descIdx) ?: "" else ""
                    val start = if (startIdx >= 0) c.getLong(startIdx) else 0L
                    val end = if (endIdx >= 0) c.getLong(endIdx) else 0L
                    val loc = if (locIdx >= 0) c.getString(locIdx) ?: "" else ""
                    val color = if (colorIdx >= 0 && !c.isNull(colorIdx)) c.getInt(colorIdx) else 0xFF4285F4.toInt()
                    val calName = if (calNameIdx >= 0) c.getString(calNameIdx) ?: "Google Calendar" else "Google Calendar"

                    googleEvents.add(
                        EventEntity(
                            title = title,
                            description = desc,
                            startTime = start,
                            endTime = end,
                            location = loc,
                            color = color,
                            isGoogleEvent = true,
                            googleEventId = id.toString(),
                            calendarName = calName
                        )
                    )
                }
            }

            // Sync with local database: Cache device Google events
            if (googleEvents.isNotEmpty()) {
                eventDao.clearGoogleEvents()
                googleEvents.forEach { event ->
                    eventDao.insertEvent(event)
                }
            }
        } catch (e: SecurityException) {
            Log.e("CalendarRepository", "Calendar permissions not granted for native sync", e)
        } catch (e: Exception) {
            Log.e("CalendarRepository", "Failed to sync device calendar", e)
        }
        return@withContext googleEvents
    }

    /**
     * Adds an event to the device's Google Calendar via ContentContract.
     */
    suspend fun addEventToDeviceCalendar(event: EventEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            val contentResolver: ContentResolver = context.contentResolver
            val values = ContentValues().apply {
                put(CalendarContract.Events.TITLE, event.title)
                put(CalendarContract.Events.DESCRIPTION, event.description)
                put(CalendarContract.Events.DTSTART, event.startTime)
                put(CalendarContract.Events.DTEND, event.endTime)
                put(CalendarContract.Events.EVENT_LOCATION, event.location)
                put(CalendarContract.Events.CALENDAR_ID, 1) // Default primary calendar ID
                put(CalendarContract.Events.EVENT_TIMEZONE, java.util.TimeZone.getDefault().id)
            }
            val uri = contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            return@withContext uri != null
        } catch (e: SecurityException) {
            Log.e("CalendarRepository", "No calendar write permissions", e)
            return@withContext false
        } catch (e: Exception) {
            Log.e("CalendarRepository", "Failed to write to device calendar", e)
            return@withContext false
        }
    }
}
