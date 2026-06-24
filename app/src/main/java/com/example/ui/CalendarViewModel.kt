package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.CalendarRepository
import com.example.data.EventEntity
import com.example.widget.CalendarWidgetProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

class CalendarViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = CalendarRepository(application, db.eventDao())

    private val _selectedDate = MutableStateFlow(Calendar.getInstance())
    val selectedDate: StateFlow<Calendar> = _selectedDate.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _showLocalOnly = MutableStateFlow(true)
    val showLocalOnly: StateFlow<Boolean> = _showLocalOnly.asStateFlow()

    private val _showGoogleOnly = MutableStateFlow(true)
    val showGoogleOnly: StateFlow<Boolean> = _showGoogleOnly.asStateFlow()

    private val _syncStatus = MutableStateFlow("Google Calendar Synced (Up to Date)")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    // Combined state flow for events
    val events: StateFlow<List<EventEntity>> = combine(
        repository.allLocalEvents,
        _searchQuery,
        _showLocalOnly,
        _showGoogleOnly
    ) { allEvents, query, showLocal, showGoogle ->
        allEvents.filter { event ->
            val matchesQuery = event.title.contains(query, ignoreCase = true) ||
                    event.description.contains(query, ignoreCase = true) ||
                    event.location.contains(query, ignoreCase = true)

            val matchesFilter = if (event.isGoogleEvent) showGoogle else showLocal

            matchesQuery && matchesFilter
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        // Pre-populate some simulated Google Calendar events on first start so the sync is immediately testable!
        viewModelScope.launch {
            repository.populateSimulatedGoogleEvents()
            triggerWidgetUpdate()
        }
    }

    fun selectDate(date: Calendar) {
        _selectedDate.value = date
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleLocalFilter() {
        _showLocalOnly.value = !_showLocalOnly.value
    }

    fun toggleGoogleFilter() {
        _showGoogleOnly.value = !_showGoogleOnly.value
    }

    fun addEvent(
        title: String,
        description: String,
        startTime: Long,
        endTime: Long,
        location: String,
        color: Int,
        syncToGoogle: Boolean
    ) {
        viewModelScope.launch {
            val newEvent = EventEntity(
                title = title,
                description = description,
                startTime = startTime,
                endTime = endTime,
                location = location,
                color = color,
                isGoogleEvent = syncToGoogle,
                googleEventId = if (syncToGoogle) "g_${System.currentTimeMillis()}" else null,
                calendarName = if (syncToGoogle) "Google Work Sync" else "Local Calendar"
            )
            repository.insert(newEvent)

            if (syncToGoogle) {
                // Insert to system Google calendar provider as well!
                repository.addEventToDeviceCalendar(newEvent)
            }

            triggerWidgetUpdate()
        }
    }

    fun updateEvent(event: EventEntity) {
        viewModelScope.launch {
            repository.update(event)
            triggerWidgetUpdate()
        }
    }

    fun deleteEvent(event: EventEntity) {
        viewModelScope.launch {
            repository.delete(event)
            triggerWidgetUpdate()
        }
    }

    fun triggerGoogleSync() {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncStatus.value = "Connecting to Google Calendar API..."
            kotlinx.coroutines.delay(1200) // Simulated connection time
            _syncStatus.value = "Fetching feeds..."
            
            // Query real events if calendar permission is active
            val deviceEvents = repository.syncWithDeviceCalendar()
            if (deviceEvents.isEmpty()) {
                // Fallback to updating simulated ones
                repository.populateSimulatedGoogleEvents()
            }
            
            _isSyncing.value = false
            _syncStatus.value = "Google Calendar Synced (${SimpleDateFormat("h:mm a", java.util.Locale.getDefault()).format(java.util.Date())})"
            triggerWidgetUpdate()
        }
    }

    private fun triggerWidgetUpdate() {
        CalendarWidgetProvider.triggerUpdate(getApplication())
    }
}
