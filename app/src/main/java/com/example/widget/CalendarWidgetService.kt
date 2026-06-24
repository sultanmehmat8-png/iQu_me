package com.example.widget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.R
import com.example.data.AppDatabase
import com.example.data.EventEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CalendarWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return CalendarWidgetFactory(applicationContext)
    }
}

class CalendarWidgetFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

    private var eventsList: List<EventEntity> = emptyList()
    private val database: AppDatabase by lazy { AppDatabase.getDatabase(context) }

    override fun onCreate() {
        // Init cache
        loadEvents()
    }

    override fun onDataSetChanged() {
        // Triggered when notifyAppWidgetViewDataChanged is called
        loadEvents()
    }

    private fun loadEvents() {
        // Run blocking is acceptable in widget loader as it runs on a background binder thread
        runBlocking {
            try {
                // Fetch upcoming events from now onwards
                val now = System.currentTimeMillis()
                val allEvents = database.eventDao().getAllEvents().first()
                // Sort and filter: display next 10 events
                eventsList = allEvents
                    .filter { it.endTime >= now }
                    .take(10)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroy() {
        eventsList = emptyList()
    }

    override fun getCount(): Int = eventsList.size

    override fun getViewAt(position: Int): RemoteViews {
        if (position >= count) return RemoteViews(context.packageName, R.layout.widget_event_item)

        val event = eventsList[position]
        val views = RemoteViews(context.packageName, R.layout.widget_event_item)

        views.setTextViewText(R.id.item_event_title, event.title)

        // Format times
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val startTimeStr = timeFormat.format(Date(event.startTime))
        val endTimeStr = timeFormat.format(Date(event.endTime))
        views.setTextViewText(R.id.item_event_time, "$startTimeStr - $endTimeStr")

        // Set left bar color: RemoteViews allows setting color filter on Views
        views.setInt(R.id.item_color_indicator, "setBackgroundColor", event.color)

        // Show/Hide Google indicator badge
        if (event.isGoogleEvent) {
            views.setViewVisibility(R.id.item_event_source, android.view.View.VISIBLE)
        } else {
            views.setViewVisibility(R.id.item_event_source, android.view.View.GONE)
        }

        // Fill-in intent to handle clicking individual list items and launch main app
        val fillInIntent = Intent()
        views.setOnClickFillInIntent(R.id.item_event_title, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true
}
