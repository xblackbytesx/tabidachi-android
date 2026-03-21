package com.example.tabidachi.ui.trip

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.tabidachi.TabidachiApp
import com.example.tabidachi.network.ApiDay
import com.example.tabidachi.network.ApiLeg
import com.example.tabidachi.network.ApiTripData
import com.example.tabidachi.ui.components.AccommodationBanner
import com.example.tabidachi.ui.components.DayHeader
import com.example.tabidachi.ui.components.DayTypeBadge
import com.example.tabidachi.ui.components.ErrorState
import com.example.tabidachi.ui.components.EventCard
import com.example.tabidachi.ui.components.ImageLightbox
import com.example.tabidachi.ui.components.LegHeader
import com.example.tabidachi.ui.components.LoadingState
import com.example.tabidachi.ui.components.TransitCard
import com.example.tabidachi.ui.theme.IndigoAccent
import com.example.tabidachi.ui.theme.SuccessGreen
import com.example.tabidachi.ui.theme.TextSecondary
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailScreen(
    app: TabidachiApp,
    tripId: String,
    onBack: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    val viewModel = remember { TripDetailViewModel(app, tripId) }
    val uiState by viewModel.uiState.collectAsState()
    var lightboxUrl by remember { mutableStateOf<String?>(null) }
    var lightboxCredit by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()

    // Auto-scroll to today
    LaunchedEffect(uiState.data) {
        val data = uiState.data ?: return@LaunchedEffect
        val todayStr = LocalDate.now().toString()
        var itemIndex = 1 // skip hero header
        for (leg in data.legs) {
            itemIndex++ // leg header
            if (leg.accommodation != null) itemIndex++ // accommodation
            if (leg.notes != null) itemIndex++ // notes
            for (day in leg.days) {
                if (day.date == todayStr) {
                    listState.animateScrollToItem(itemIndex)
                    return@LaunchedEffect
                }
                itemIndex++ // day header
                itemIndex += day.events.size // events
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        uiState.summary?.title ?: "Trip",
                        maxLines = 1,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        when {
            uiState.isLoading && uiState.data == null -> {
                LoadingState(modifier = Modifier.padding(padding))
            }

            uiState.error != null && uiState.data == null -> {
                ErrorState(
                    message = uiState.error!!,
                    onRetry = { viewModel.refresh() },
                    modifier = Modifier.padding(padding),
                )
            }

            uiState.data != null -> {
                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = { viewModel.refresh() },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    TripTimeline(
                        summary = uiState.summary,
                        data = uiState.data!!,
                        listState = listState,
                        onImageClick = { url, credit ->
                            lightboxUrl = url
                            lightboxCredit = credit
                        },
                    )
                }
            }
        }
    }

    // Image lightbox
    if (lightboxUrl != null) {
        ImageLightbox(
            imageUrl = lightboxUrl!!,
            credit = lightboxCredit,
            onDismiss = {
                lightboxUrl = null
                lightboxCredit = null
            },
        )
    }
}

@Composable
private fun TripTimeline(
    summary: com.example.tabidachi.data.TripSummary?,
    data: ApiTripData,
    listState: LazyListState,
    onImageClick: (String, String?) -> Unit,
) {
    val today = LocalDate.now()

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Hero header
        item(key = "hero") {
            TripHeroHeader(
                summary = summary,
                data = data,
            )
        }

        // Legs
        for ((legIdx, leg) in data.legs.withIndex()) {
            // Leg header
            item(key = "leg_$legIdx") {
                Spacer(modifier = Modifier.height(8.dp))
                LegHeader(leg = leg, legIndex = legIdx)
            }

            // Accommodation
            if (leg.accommodation != null) {
                item(key = "acc_$legIdx") {
                    AccommodationBanner(accommodation = leg.accommodation)
                }
            }

            // Leg notes
            if (leg.notes != null) {
                item(key = "leg_notes_$legIdx") {
                    ExpandableNotes(notes = leg.notes, label = "Leg notes")
                }
            }

            // Days
            var dayNumber = 1
            for ((dayIdx, day) in leg.days.withIndex()) {
                val isToday = try {
                    LocalDate.parse(day.date) == today
                } catch (_: Exception) {
                    false
                }

                // Day header
                item(key = "day_${legIdx}_$dayIdx") {
                    DayHeader(
                        day = day,
                        dayNumber = dayNumber,
                        isToday = isToday,
                    )
                }

                // Day notes
                if (day.notes != null) {
                    item(key = "day_notes_${legIdx}_$dayIdx") {
                        ExpandableNotes(notes = day.notes, label = "Day notes")
                    }
                }

                // Events
                for ((eventIdx, event) in day.events.withIndex()) {
                    item(key = "event_${legIdx}_${dayIdx}_$eventIdx") {
                        if (event.type == "transit") {
                            TransitCard(event = event)
                        } else {
                            EventCard(
                                event = event,
                                onImageClick = { url ->
                                    onImageClick(url, event.imageCredit)
                                },
                            )
                        }
                    }
                }

                dayNumber++
            }
        }

        // Bottom spacer
        item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun TripHeroHeader(
    summary: com.example.tabidachi.data.TripSummary?,
    data: ApiTripData,
) {
    val hasCover = summary?.coverImageUrl != null

    val dateRange = try {
        val start = LocalDate.parse(data.startDate)
        val end = LocalDate.parse(data.endDate)
        val fmt = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
        "${start.format(fmt)} — ${end.format(fmt)}"
    } catch (_: Exception) {
        "${data.startDate} — ${data.endDate}"
    }

    val isActive = try {
        val today = LocalDate.now()
        val start = LocalDate.parse(data.startDate)
        val end = LocalDate.parse(data.endDate)
        !today.isBefore(start) && !today.isAfter(end)
    } catch (_: Exception) {
        false
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp)),
    ) {
        if (hasCover) {
            AsyncImage(
                model = summary!!.coverImageUrl,
                contentDescription = data.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
                contentScale = ContentScale.Crop,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.2f),
                                Color.Black.copy(alpha = 0.8f),
                            ),
                        ),
                    ),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (hasCover) Modifier.height(260.dp) else Modifier)
                .padding(24.dp),
            verticalArrangement = if (hasCover) Arrangement.Bottom else Arrangement.Top,
        ) {
            if (isActive) {
                DayTypeBadge(text = "Active now", color = SuccessGreen)
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                text = data.title,
                style = MaterialTheme.typography.headlineLarge,
                color = if (hasCover) Color.White else MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(8.dp))

            val contentColor = if (hasCover) Color.White.copy(alpha = 0.85f) else TextSecondary

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CalendarToday, null,
                    modifier = Modifier.size(14.dp), tint = contentColor,
                )
                Spacer(Modifier.width(6.dp))
                Text(dateRange, style = MaterialTheme.typography.bodySmall, color = contentColor)
            }

            if (data.homeLocation != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Home, null,
                        modifier = Modifier.size(14.dp), tint = contentColor,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(data.homeLocation, style = MaterialTheme.typography.bodySmall, color = contentColor)
                }
            }

            if (data.timezone != null && data.timezone != "UTC") {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Language, null,
                        modifier = Modifier.size(14.dp), tint = contentColor,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(data.timezone, style = MaterialTheme.typography.bodySmall, color = contentColor)
                }
            }
        }
    }
}

@Composable
private fun ExpandableNotes(
    notes: String,
    label: String,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .clickable { expanded = !expanded }
            .padding(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                modifier = Modifier.size(18.dp),
                tint = TextSecondary,
            )
        }
        AnimatedVisibility(visible = expanded) {
            Text(
                text = notes,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
