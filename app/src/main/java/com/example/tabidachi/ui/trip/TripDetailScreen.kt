package com.example.tabidachi.ui.trip

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.tabidachi.TabidachiApp
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
import com.example.tabidachi.ui.components.transportInfo
import com.example.tabidachi.ui.theme.DarkBorder
import com.example.tabidachi.ui.theme.IndigoAccent
import com.example.tabidachi.ui.theme.TextMuted
import com.example.tabidachi.ui.theme.SuccessGreen
import com.example.tabidachi.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Mapping from continuous day number (0-based) to the LazyColumn item index
 * of that day's header. Built once when trip data changes.
 */
private fun buildDayIndexMap(data: ApiTripData): List<Int> {
    val map = mutableListOf<Int>()
    var itemIndex = 1 // skip hero header
    for ((legIdx, leg) in data.legs.withIndex()) {
        if (legIdx > 0) itemIndex++ // leg divider
        itemIndex++ // leg header
        if (leg.accommodation != null) itemIndex++ // accommodation
        if (leg.notes != null) itemIndex++ // leg notes
        for (day in leg.days) {
            map.add(itemIndex) // day header index
            itemIndex++ // day header
            if (day.notes != null) itemIndex++ // day notes
            itemIndex += day.events.size // events
        }
    }
    return map
}

private fun totalDays(data: ApiTripData): Int =
    data.legs.sumOf { it.days.size }

private fun todayDayIndex(data: ApiTripData): Int {
    val todayStr = LocalDate.now().toString()
    var idx = 0
    for (leg in data.legs) {
        for (day in leg.days) {
            if (day.date == todayStr) return idx
            idx++
        }
    }
    return -1
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailScreen(
    app: TabidachiApp,
    tripId: String,
) {
    val viewModel = remember { TripDetailViewModel(app, tripId) }
    val uiState by viewModel.uiState.collectAsState()
    var lightboxUrl by remember { mutableStateOf<String?>(null) }
    var lightboxCredit by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    val carouselState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val navBarPadding = WindowInsets.navigationBars.asPaddingValues()
    val navBarHeight = navBarPadding.calculateBottomPadding()
    val carouselHeight = 56.dp + navBarHeight

    val dayIndexMap = remember(uiState.data) {
        uiState.data?.let { buildDayIndexMap(it) } ?: emptyList()
    }
    val numDays = remember(uiState.data) {
        uiState.data?.let { totalDays(it) } ?: 0
    }

    var selectedDay by remember { mutableIntStateOf(-1) }

    val visibleDay by remember(dayIndexMap) {
        derivedStateOf {
            if (dayIndexMap.isEmpty()) -1
            else {
                val firstVisible = listState.firstVisibleItemIndex
                val idx = dayIndexMap.indexOfLast { it <= firstVisible + 1 }
                if (idx >= 0) idx else 0
            }
        }
    }

    LaunchedEffect(visibleDay) {
        if (visibleDay >= 0 && selectedDay != visibleDay) {
            selectedDay = visibleDay
            carouselState.animateScrollToItem(
                index = maxOf(0, visibleDay - 2),
            )
        }
    }

    LaunchedEffect(uiState.data) {
        val data = uiState.data ?: return@LaunchedEffect
        val todayIdx = todayDayIndex(data)
        if (todayIdx >= 0 && dayIndexMap.isNotEmpty()) {
            selectedDay = todayIdx
            listState.animateScrollToItem(dayIndexMap[todayIdx])
            carouselState.animateScrollToItem(maxOf(0, todayIdx - 2))
        }
    }

    when {
        uiState.isLoading && uiState.data == null -> {
            LoadingState()
        }

        uiState.error != null && uiState.data == null -> {
            ErrorState(
                message = uiState.error!!,
                onRetry = { viewModel.refresh() },
            )
        }

        uiState.data != null -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
            ) {
                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = { viewModel.refresh() },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = carouselHeight),
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

                if (numDays > 0) {
                    DayCarousel(
                        numDays = numDays,
                        data = uiState.data!!,
                        selectedDay = selectedDay,
                        carouselState = carouselState,
                        navBarHeight = navBarHeight,
                        onDayClick = { dayIdx ->
                            selectedDay = dayIdx
                            scope.launch {
                                if (dayIdx < dayIndexMap.size) {
                                    listState.animateScrollToItem(dayIndexMap[dayIdx])
                                }
                            }
                        },
                        onDayScrub = { dayIdx ->
                            selectedDay = dayIdx
                            scope.launch {
                                if (dayIdx < dayIndexMap.size) {
                                    listState.scrollToItem(dayIndexMap[dayIdx])
                                }
                            }
                        },
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            }
        }
    }

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
private fun DayCarousel(
    numDays: Int,
    data: ApiTripData,
    selectedDay: Int,
    carouselState: LazyListState,
    navBarHeight: androidx.compose.ui.unit.Dp,
    onDayClick: (Int) -> Unit,
    onDayScrub: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = LocalDate.now()
    val density = LocalDensity.current
    val view = LocalView.current
    val scrubPaddingPx = with(density) { 24.dp.toPx() } // dead zone on each side

    val allDays = remember(data) {
        data.legs.flatMap { it.days }
    }

    // Scrubber state
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubDay by remember { mutableIntStateOf(0) }
    var scrubX by remember { mutableStateOf(0f) }
    var barWidth by remember { mutableStateOf(1f) } // 1f avoids division by zero

    // Tile fade animation
    val tilesAlpha by animateFloatAsState(
        targetValue = if (isScrubbing) 0f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "tilesAlpha",
    )
    val scrubTrackAlpha by animateFloatAsState(
        targetValue = if (isScrubbing) 1f else 0f,
        animationSpec = tween(durationMillis = 150),
        label = "scrubTrackAlpha",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                        MaterialTheme.colorScheme.background,
                    ),
                    startY = 0f,
                    endY = 40f,
                ),
            )
            .padding(bottom = navBarHeight),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 8.dp)
                .onSizeChanged { barWidth = it.width.toFloat() }
                .pointerInput(numDays) {
                    fun xToDay(x: Float): Int {
                        val usable = barWidth - scrubPaddingPx * 2
                        val normalized = ((x - scrubPaddingPx) / usable).coerceIn(0f, 1f)
                        return (normalized * (numDays - 1)).toInt().coerceIn(0, numDays - 1)
                    }

                    val longPressTimeMs = viewConfiguration.longPressTimeoutMillis
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val longPressTriggered = withTimeoutOrNull(longPressTimeMs) {
                            waitForUpOrCancellation()
                        }
                        if (longPressTriggered != null) {
                            return@awaitEachGesture
                        }
                        // Long press triggered — activate scrubber
                        isScrubbing = true
                        scrubX = down.position.x.coerceIn(0f, barWidth)
                        val initialDay = xToDay(scrubX)
                        scrubDay = initialDay
                        onDayScrub(initialDay)
                        view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)

                        // Track drag until release
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            if (!change.pressed) {
                                isScrubbing = false
                                onDayClick(scrubDay)
                                break
                            }
                            change.consume()
                            scrubX = change.position.x.coerceIn(0f, barWidth)
                            val day = xToDay(scrubX)
                            if (day != scrubDay) {
                                scrubDay = day
                                onDayScrub(day)
                                view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                            }
                        }
                    }
                },
        ) {
            // Scrub track (visible during scrubbing)
            if (scrubTrackAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .alpha(scrubTrackAlpha),
                    contentAlignment = Alignment.Center,
                ) {
                    // Thin track line
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(1.5.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    )
                    // Progress fill
                    val progress = if (numDays > 1) scrubDay.toFloat() / (numDays - 1) else 0f
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(1.5.dp)),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .fillMaxHeight()
                                .background(IndigoAccent),
                        )
                    }
                }
            }

            // Day tiles (fade out during scrubbing)
            if (tilesAlpha > 0f) {
                LazyRow(
                    state = carouselState,
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.alpha(tilesAlpha),
                ) {
                    items(numDays) { idx ->
                        val isSelected = idx == selectedDay
                        val isToday = try {
                            idx < allDays.size && LocalDate.parse(allDays[idx].date) == today
                        } catch (_: Exception) {
                            false
                        }

                        val bgColor = when {
                            isSelected -> IndigoAccent
                            isToday -> IndigoAccent.copy(alpha = 0.2f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                        val textColor = when {
                            isSelected -> Color.White
                            isToday -> IndigoAccent
                            else -> TextSecondary
                        }

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(bgColor)
                                .clickable { onDayClick(idx) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "${idx + 1}",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 14.sp,
                                ),
                                color = textColor,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }

        // Floating day bubble (above touch point during scrubbing)
        if (isScrubbing) {
            val bubbleSizePx = with(density) { 48.dp.toPx() }
            val bubbleOffsetX = with(density) {
                val centered = scrubX - bubbleSizePx / 2
                centered.coerceIn(4.dp.toPx(), barWidth - bubbleSizePx - 4.dp.toPx()).toDp()
            }
            Box(
                modifier = Modifier
                    .offset(x = bubbleOffsetX, y = (-36).dp)
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(IndigoAccent)
                    .align(Alignment.BottomStart),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "${scrubDay + 1}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
            }
        }
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
    val contentPadding = PaddingValues(horizontal = 16.dp)

    LazyColumn(
        state = listState,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Hero header — full bleed, no padding, hugs top edge
        item(key = "hero") {
            TripHeroHeader(
                summary = summary,
                data = data,
            )
        }

        // Legs
        var dayNumber = 1
        for ((legIdx, leg) in data.legs.withIndex()) {
            // Decorative divider between legs
            if (legIdx > 0) {
                item(key = "leg_divider_$legIdx") {
                    LegDivider(modifier = Modifier.padding(contentPadding))
                }
            }

            item(key = "leg_$legIdx") {
                if (legIdx == 0) Spacer(modifier = Modifier.height(8.dp))
                LegHeader(
                    leg = leg,
                    legIndex = legIdx,
                    modifier = Modifier.padding(contentPadding),
                )
            }

            if (leg.accommodation != null) {
                item(key = "acc_$legIdx") {
                    AccommodationBanner(
                        accommodation = leg.accommodation,
                        modifier = Modifier.padding(contentPadding),
                    )
                }
            }

            if (leg.notes != null) {
                item(key = "leg_notes_$legIdx") {
                    ExpandableNotes(
                        notes = leg.notes,
                        label = "Leg notes",
                        modifier = Modifier.padding(contentPadding),
                    )
                }
            }

            for ((dayIdx, day) in leg.days.withIndex()) {
                val currentDayNumber = dayNumber

                val isToday = try {
                    LocalDate.parse(day.date) == today
                } catch (_: Exception) {
                    false
                }

                item(key = "day_${legIdx}_$dayIdx") {
                    DayHeader(
                        day = day,
                        dayNumber = currentDayNumber,
                        isToday = isToday,
                        modifier = Modifier.padding(contentPadding),
                    )
                }

                if (day.notes != null) {
                    item(key = "day_notes_${legIdx}_$dayIdx") {
                        ExpandableNotes(
                            notes = day.notes,
                            label = "Day notes",
                            modifier = Modifier.padding(contentPadding),
                        )
                    }
                }

                for ((eventIdx, event) in day.events.withIndex()) {
                    item(key = "event_${legIdx}_${dayIdx}_$eventIdx") {
                        Box(modifier = Modifier.padding(contentPadding)) {
                            if (event.type == "transit") {
                                TransitRoute(event = event)
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
                }

                dayNumber++
            }
        }

        // Bottom spacer so last day can scroll above carousel
        item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(64.dp))
        }
    }
}

@Composable
private fun LegDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Horizontal line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(DarkBorder),
        )
        // Center dot
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(TextMuted),
        )
    }
}

@Composable
private fun TransitRoute(
    event: com.example.tabidachi.network.ApiEvent,
    modifier: Modifier = Modifier,
) {
    val transport = transportInfo(event.transportMode)
    val lineColor = transport.color.copy(alpha = 0.4f)
    val dotColor = transport.color

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
    ) {
        // Left route line with dots
        Box(
            modifier = Modifier
                .width(24.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.TopCenter,
        ) {
            // Dashed vertical line
            Canvas(
                modifier = Modifier
                    .width(2.dp)
                    .fillMaxHeight()
                    .align(Alignment.TopCenter),
            ) {
                val dashEffect = PathEffect.dashPathEffect(
                    floatArrayOf(8f, 6f),
                    0f,
                )
                drawLine(
                    color = lineColor,
                    start = Offset(size.width / 2, 0f),
                    end = Offset(size.width / 2, size.height),
                    strokeWidth = 2f,
                    pathEffect = dashEffect,
                )
            }

            // Top dot (departure)
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .offset(y = 18.dp)
                    .clip(CircleShape)
                    .background(dotColor)
                    .align(Alignment.TopCenter),
            )

            // Bottom dot (arrival)
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .offset(y = (-10).dp)
                    .clip(CircleShape)
                    .background(dotColor)
                    .align(Alignment.BottomCenter),
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Transit card
        TransitCard(
            event = event,
            modifier = Modifier.weight(1f),
        )
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

    // Full-bleed box — no rounding, hugs top and sides
    Box(
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (hasCover) {
            AsyncImage(
                model = summary!!.coverImageUrl,
                contentDescription = data.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                contentScale = ContentScale.Crop,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.1f),
                                Color.Black.copy(alpha = 0.75f),
                            ),
                        ),
                    ),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (hasCover) Modifier.height(300.dp) else Modifier.padding(top = 48.dp))
                .padding(horizontal = 24.dp, vertical = 24.dp),
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
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
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
