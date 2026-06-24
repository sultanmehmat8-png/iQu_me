package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.EventEntity
import java.text.SimpleDateFormat
import java.util.*

// Colors mapping
private val themeBackground = Color(0xFFFCF8FF)
private val cardBackground = Color(0xFFFFFFFF)
private val borderStrokeColor = Color(0xFFCAC4D0)
private val accentNeonColor = Color(0xFF6750A4)
private val secondaryAccentColor = Color(0xFFD0BCFF)
private val backgroundPurpleLight = Color(0xFFEADDFF)
private val backgroundPurpleMedium = Color(0xFFD0BCFF)
private val primaryTextColor = Color(0xFF1C1B1F)
private val secondaryTextColor = Color(0xFF49454F)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(viewModel: CalendarViewModel) {
    val context = LocalContext.current
    val events by viewModel.events.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val showLocalOnly by viewModel.showLocalOnly.collectAsStateWithLifecycle()
    val showGoogleOnly by viewModel.showGoogleOnly.collectAsStateWithLifecycle()
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var showDetailDialog by remember { mutableStateOf<EventEntity?>(null) }
    var calendarMonthOffset by remember { mutableStateOf(0) } // Offset from current month

    // Calendar permissions state launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.READ_CALENDAR] == true &&
                permissions[Manifest.permission.WRITE_CALENDAR] == true
        if (granted) {
            Toast.makeText(context, "Google Calendar synced with local system accounts!", Toast.LENGTH_SHORT).show()
            viewModel.triggerGoogleSync()
        } else {
            Toast.makeText(context, "Using secure sandbox sync simulation instead.", Toast.LENGTH_LONG).show()
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(themeBackground)
    ) {
        val isWideScreen = maxWidth > 650.dp

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                // Sleek integrated top bar showing beautiful banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.calendar_banner),
                        contentDescription = "Cosmic Calendar Banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Gradient overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        themeBackground.copy(alpha = 0.85f),
                                        themeBackground
                                    )
                                )
                            )
                    )

                    // Hero Text overlay
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "App Icon",
                                tint = accentNeonColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "DESK CALENDAR HUB",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryTextColor,
                                letterSpacing = 2.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFE8DEF8))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isSyncing) Color(0xFFFFB400) else Color(0xFF6750A4))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = syncStatus,
                                fontSize = 11.sp,
                                color = Color(0xFF21005D)
                            )
                        }
                    }

                    // Sync & Control Actions
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                val readGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
                                val writeGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED
                                if (readGranted && writeGranted) {
                                    viewModel.triggerGoogleSync()
                                } else {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.READ_CALENDAR,
                                            Manifest.permission.WRITE_CALENDAR
                                        )
                                    )
                                }
                            },
                            modifier = Modifier
                                .background(Color(0xFFE8DEF8), CircleShape)
                                .size(40.dp)
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(color = Color(0xFF6750A4), modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Sync", tint = Color(0xFF1D192B))
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
            ) {
                // Filters and Search Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        placeholder = { Text("Search agenda events...", color = secondaryTextColor, fontSize = 13.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .border(1.dp, borderStrokeColor, RoundedCornerShape(12.dp)),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = cardBackground,
                            unfocusedContainerColor = cardBackground,
                            disabledContainerColor = cardBackground,
                            focusedTextColor = primaryTextColor,
                            unfocusedTextColor = primaryTextColor,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = secondaryTextColor) }
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    FilterChip(
                        selected = showLocalOnly,
                        onClick = { viewModel.toggleLocalFilter() },
                        label = { Text("Local", fontSize = 11.sp, color = if (showLocalOnly) Color(0xFF21005D) else secondaryTextColor) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFE8DEF8),
                            selectedLabelColor = Color(0xFF21005D)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    FilterChip(
                        selected = showGoogleOnly,
                        onClick = { viewModel.toggleGoogleFilter() },
                        label = { Text("Google", fontSize = 11.sp, color = if (showGoogleOnly) Color(0xFF21005D) else secondaryTextColor) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFE8DEF8),
                            selectedLabelColor = Color(0xFF21005D)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                if (isWideScreen) {
                    // Widescreen/Desktop responsive dual pane layout
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Left Pane: Premium Month Calendar View
                        Box(
                            modifier = Modifier
                                .weight(1.1f)
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.dp, borderStrokeColor, RoundedCornerShape(16.dp))
                                .background(cardBackground)
                                .padding(16.dp)
                        ) {
                            CalendarGridView(
                                offset = calendarMonthOffset,
                                selectedDate = selectedDate,
                                events = events,
                                onDayClick = { viewModel.selectDate(it) },
                                onPrevMonth = { calendarMonthOffset-- },
                                onNextMonth = { calendarMonthOffset++ }
                            )
                        }

                        // Right Pane: Timeline Schedule View for Selected Date
                        Column(
                            modifier = Modifier
                                .weight(1.3f)
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.dp, borderStrokeColor, RoundedCornerShape(16.dp))
                                .background(cardBackground)
                                .padding(16.dp)
                        ) {
                            ScheduleHeader(
                                selectedDate = selectedDate,
                                onAddClick = { showAddDialog = true }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            ScheduleList(
                                events = events,
                                selectedDate = selectedDate,
                                onEventClick = { showDetailDialog = it }
                            )
                        }
                    }
                } else {
                    // Mobile Portrait Stack Layout
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(1.dp, borderStrokeColor, RoundedCornerShape(16.dp))
                                    .background(cardBackground)
                                    .padding(16.dp)
                            ) {
                                CalendarGridView(
                                    offset = calendarMonthOffset,
                                    selectedDate = selectedDate,
                                    events = events,
                                    onDayClick = { viewModel.selectDate(it) },
                                    onPrevMonth = { calendarMonthOffset-- },
                                    onNextMonth = { calendarMonthOffset++ }
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(selectedDate.time),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryTextColor
                                )
                                Button(
                                    onClick = { showAddDialog = true },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF6750A4),
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add", fontSize = 12.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        val dayEvents = events.filter { event ->
                            val calEvent = Calendar.getInstance().apply { timeInMillis = event.startTime }
                            calEvent.get(Calendar.YEAR) == selectedDate.get(Calendar.YEAR) &&
                                    calEvent.get(Calendar.DAY_OF_YEAR) == selectedDate.get(Calendar.DAY_OF_YEAR)
                        }

                        if (dayEvents.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No events scheduled for today.",
                                        color = Color.Gray,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        } else {
                            items(dayEvents) { event ->
                                EventItemRow(event = event, onClick = { showDetailDialog = event })
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    // Add Event Dialog
    if (showAddDialog) {
        AddEventDialog(
            selectedDate = selectedDate,
            onDismiss = { showAddDialog = false },
            onSave = { title, desc, start, end, loc, color, sync ->
                viewModel.addEvent(title, desc, start, end, loc, color, sync)
                showAddDialog = false
            }
        )
    }

    // Details Modal
    showDetailDialog?.let { event ->
        EventDetailsDialog(
            event = event,
            onDismiss = { showDetailDialog = null },
            onDelete = {
                viewModel.deleteEvent(event)
                showDetailDialog = null
            }
        )
    }
}

@Composable
fun CalendarGridView(
    offset: Int,
    selectedDate: Calendar,
    events: List<EventEntity>,
    onDayClick: (Calendar) -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    // Compute current viewing month
    val viewingMonth = remember(offset) {
        Calendar.getInstance().apply {
            add(Calendar.MONTH, offset)
        }
    }

    val monthName = remember(viewingMonth) {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(viewingMonth.time)
    }

    // Days in viewing month
    val daysInMonth = remember(viewingMonth) {
        val calendar = viewingMonth.clone() as Calendar
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1 // 0-indexed

        val days = mutableListOf<Calendar?>()
        // Prepend padding
        for (i in 0 until firstDayOfWeek) {
            days.add(null)
        }
        // Add days
        for (i in 1..maxDays) {
            val day = viewingMonth.clone() as Calendar
            day.set(Calendar.DAY_OF_MONTH, i)
            days.add(day)
        }
        days
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Month Selector Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = monthName,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = primaryTextColor
            )

            Row {
                IconButton(onClick = onPrevMonth) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Prev Month", tint = primaryTextColor)
                }
                IconButton(onClick = onNextMonth) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next Month", tint = primaryTextColor)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Weekday Subheaders
        Row(modifier = Modifier.fillMaxWidth()) {
            val daysOfWeek = listOf("S", "M", "T", "W", "T", "F", "S")
            daysOfWeek.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = secondaryTextColor
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Day Grid
        var i = 0
        while (i < daysInMonth.size) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0..6) {
                    val cellIndex = i + col
                    if (cellIndex < daysInMonth.size) {
                        val day = daysInMonth[cellIndex]
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1.1f)
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (day != null) {
                                val isToday = remember(day) {
                                    val today = Calendar.getInstance()
                                    today.get(Calendar.YEAR) == day.get(Calendar.YEAR) &&
                                            today.get(Calendar.DAY_OF_YEAR) == day.get(Calendar.DAY_OF_YEAR)
                                }

                                val isSelected = remember(day, selectedDate) {
                                    selectedDate.get(Calendar.YEAR) == day.get(Calendar.YEAR) &&
                                            selectedDate.get(Calendar.DAY_OF_YEAR) == day.get(Calendar.DAY_OF_YEAR)
                                }

                                // Fetch event dot colors
                                val dayEvents = remember(day, events) {
                                    events.filter { event ->
                                        val calEvent = Calendar.getInstance().apply { timeInMillis = event.startTime }
                                        calEvent.get(Calendar.YEAR) == day.get(Calendar.YEAR) &&
                                                calEvent.get(Calendar.DAY_OF_YEAR) == day.get(Calendar.DAY_OF_YEAR)
                                    }
                                }

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            when {
                                                isSelected -> Color(0xFF6750A4)
                                                isToday -> Color(0xFFE8DEF8)
                                                else -> Color.Transparent
                                            }
                                        )
                                        .border(
                                            width = if (isToday && !isSelected) 1.dp else 0.dp,
                                            color = if (isToday && !isSelected) Color(0xFF6750A4) else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { onDayClick(day) }
                                        .padding(4.dp),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = day.get(Calendar.DAY_OF_MONTH).toString(),
                                        fontSize = 13.sp,
                                        fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else if (isToday) Color(0xFF6750A4) else primaryTextColor
                                    )

                                    if (dayEvents.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            dayEvents.take(3).forEach { event ->
                                                Box(
                                                    modifier = Modifier
                                                        .size(4.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(event.color))
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                i += 7
            }
        }
    }
}

@Composable
fun ScheduleHeader(selectedDate: Calendar, onAddClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "DAY AGENDA",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF6750A4),
                letterSpacing = 1.5.sp
            )
            Text(
                text = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(selectedDate.time),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = primaryTextColor
            )
        }

        Button(
            onClick = onAddClick,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF6750A4),
                contentColor = Color.White
            ),
            modifier = Modifier.testTag("add_event_btn")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add New Event", modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Add Event", fontSize = 12.sp)
        }
    }
}

@Composable
fun ScheduleList(
    events: List<EventEntity>,
    selectedDate: Calendar,
    onEventClick: (EventEntity) -> Unit
) {
    val dayEvents = remember(events, selectedDate) {
        events.filter { event ->
            val calEvent = Calendar.getInstance().apply { timeInMillis = event.startTime }
            calEvent.get(Calendar.YEAR) == selectedDate.get(Calendar.YEAR) &&
                    calEvent.get(Calendar.DAY_OF_YEAR) == selectedDate.get(Calendar.DAY_OF_YEAR)
        }
    }

    if (dayEvents.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "No Events",
                tint = Color(0xFFCAC4D0),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No events scheduled for this day.",
                color = secondaryTextColor,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(dayEvents) { event ->
                EventItemRow(event = event, onClick = { onEventClick(event) })
            }
        }
    }
}

@Composable
fun EventItemRow(event: EventEntity, onClick: () -> Unit) {
    val startTimeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(event.startTime))
    val endTimeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(event.endTime))

    val cardBg = remember(event.color) {
        when (event.color) {
            0xFF4285F4.toInt() -> Color(0xFFE8F0FE)
            0xFF34A853.toInt() -> Color(0xFFE6F4EA)
            0xFFF4B400.toInt() -> Color(0xFFFEF7E0)
            0xFF673AB7.toInt() -> Color(0xFFF3E8FD)
            0xFFFF5722.toInt() -> Color(0xFFFCE8E6)
            else -> Color(0xFFEADDFF)
        }
    }
    val cardStrokeColor = Color(event.color)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(cardBg)
            .border(1.dp, cardStrokeColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Vertical Category Indicator line
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(38.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(cardStrokeColor)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = event.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF21005D),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = "Location",
                    tint = Color(0xFF49454F),
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = event.location.ifEmpty { "No Location" },
                    fontSize = 11.sp,
                    color = Color(0xFF49454F),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = startTimeStr,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF21005D)
            )
            Text(
                text = endTimeStr,
                fontSize = 11.sp,
                color = Color(0xFF49454F)
            )

            if (event.isGoogleEvent) {
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF4285F4).copy(alpha = 0.15f))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Google Sync",
                        color = Color(0xFF4285F4),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// Dialog to add simple event
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventDialog(
    selectedDate: Calendar,
    onDismiss: () -> Unit,
    onSave: (String, String, Long, Long, String, Int, Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var syncToGoogle by remember { mutableStateOf(false) }

    // Dropdown choices
    val startHour = remember { mutableIntStateOf(10) }
    val startMinute = remember { mutableIntStateOf(0) }
    val endHour = remember { mutableIntStateOf(11) }
    val endMinute = remember { mutableIntStateOf(0) }

    val colors = listOf(
        0xFF4285F4.toInt(), // Blue
        0xFF34A853.toInt(), // Green
        0xFFF4B400.toInt(), // Yellow
        0xFF673AB7.toInt(), // Purple
        0xFFFF5722.toInt()  // Coral
    )
    var selectedColor by remember { mutableIntStateOf(colors[0]) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(1.dp, Color(0xFFCAC4D0), RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFCF8FF))
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "New Agenda Event",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1C1B1F)
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Event Title", color = Color(0xFF49454F)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF1C1B1F),
                        unfocusedTextColor = Color(0xFF1C1B1F),
                        focusedBorderColor = Color(0xFF6750A4),
                        unfocusedBorderColor = Color(0xFFCAC4D0)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("add_title_input")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description", color = Color(0xFF49454F)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF1C1B1F),
                        unfocusedTextColor = Color(0xFF1C1B1F),
                        focusedBorderColor = Color(0xFF6750A4),
                        unfocusedBorderColor = Color(0xFFCAC4D0)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location/Meet Link", color = Color(0xFF49454F)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF1C1B1F),
                        unfocusedTextColor = Color(0xFF1C1B1F),
                        focusedBorderColor = Color(0xFF6750A4),
                        unfocusedBorderColor = Color(0xFFCAC4D0)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Simpler time picker fields
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = "${startHour.intValue}:${startMinute.intValue.toString().padStart(2, '0')}",
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Start Time", color = Color(0xFF49454F)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF1C1B1F),
                            unfocusedTextColor = Color(0xFF1C1B1F),
                            unfocusedBorderColor = Color(0xFFCAC4D0)
                        ),
                        modifier = Modifier.weight(1f),
                        trailingIcon = {
                            IconButton(onClick = {
                                if (startHour.intValue < 23) startHour.intValue++ else startHour.intValue = 0
                            }) { Icon(Icons.Default.ArrowDropDown, "Increase hour", tint = Color(0xFF49454F)) }
                        }
                    )

                    OutlinedTextField(
                        value = "${endHour.intValue}:${endMinute.intValue.toString().padStart(2, '0')}",
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("End Time", color = Color(0xFF49454F)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF1C1B1F),
                            unfocusedTextColor = Color(0xFF1C1B1F),
                            unfocusedBorderColor = Color(0xFFCAC4D0)
                        ),
                        modifier = Modifier.weight(1f),
                        trailingIcon = {
                            IconButton(onClick = {
                                if (endHour.intValue < 23) endHour.intValue++ else endHour.intValue = 0
                            }) { Icon(Icons.Default.ArrowDropDown, "Increase hour", tint = Color(0xFF49454F)) }
                        }
                    )
                }

                // Category colors select
                Text("Category Color", color = Color(0xFF49454F), fontSize = 12.sp)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    colors.forEach { colorVal ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(colorVal))
                                .border(
                                    width = if (selectedColor == colorVal) 3.dp else 0.dp,
                                    color = if (selectedColor == colorVal) Color(0xFF6750A4) else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = colorVal }
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = syncToGoogle,
                        onCheckedChange = { syncToGoogle = it },
                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFF6750A4))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Sync with Google Calendar API", color = Color(0xFF1C1B1F), fontSize = 13.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color(0xFF49454F))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isNotEmpty()) {
                                val sCal = selectedDate.clone() as Calendar
                                sCal.set(Calendar.HOUR_OF_DAY, startHour.intValue)
                                sCal.set(Calendar.MINUTE, startMinute.intValue)

                                val eCal = selectedDate.clone() as Calendar
                                eCal.set(Calendar.HOUR_OF_DAY, endHour.intValue)
                                eCal.set(Calendar.MINUTE, endMinute.intValue)

                                onSave(
                                    title,
                                    description,
                                    sCal.timeInMillis,
                                    eCal.timeInMillis,
                                    location,
                                    selectedColor,
                                    syncToGoogle
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4)),
                        modifier = Modifier.testTag("submit_event_btn")
                    ) {
                        Text("Create")
                    }
                }
            }
        }
    }
}

@Composable
fun EventDetailsDialog(
    event: EventEntity,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(Date(event.startTime))
    val startTimeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(event.startTime))
    val endTimeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(event.endTime))

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(1.dp, Color(0xFFCAC4D0), RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFCF8FF))
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color(event.color))
                    )
                    Text(
                        text = if (event.isGoogleEvent) "Google Synced Event" else "Local Calendar",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (event.isGoogleEvent) Color(0xFF6750A4) else Color(0xFF49454F)
                    )
                }

                Text(
                    text = event.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1C1B1F)
                )

                HorizontalDivider(color = Color(0xFFCAC4D0))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DateRange, contentDescription = "Date", tint = Color(0xFF49454F), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "$dateStr ($startTimeStr - $endTimeStr)", color = Color(0xFF1C1B1F), fontSize = 13.sp)
                    }

                    if (event.location.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Place, contentDescription = "Location", tint = Color(0xFF49454F), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = event.location, color = Color(0xFF1C1B1F), fontSize = 13.sp)
                        }
                    }

                    if (event.description.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.Info, contentDescription = "Description", tint = Color(0xFF49454F), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = event.description, color = Color(0xFF1C1B1F), fontSize = 13.sp)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccountBox, contentDescription = "Source", tint = Color(0xFF49454F), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Source Feed: ${event.calendarName}", color = Color(0xFF49454F), fontSize = 12.sp)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDelete,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("delete_event_btn")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete", fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = onDismiss) {
                        Text("Close", color = Color(0xFF49454F))
                    }
                }
            }
        }
    }
}
