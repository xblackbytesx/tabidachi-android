package com.example.tabidachi.ui.trip

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.tabidachi.TabidachiApp
import com.example.tabidachi.data.TripSummary
import com.example.tabidachi.network.ApiTripDetail
import com.example.tabidachi.ui.components.ErrorState
import com.example.tabidachi.ui.components.ImageLightbox
import com.example.tabidachi.ui.components.LoadingState
import kotlinx.coroutines.launch

@Composable
fun SharedTripScreen(
    app: TabidachiApp,
    serverUrl: String,
    shareToken: String,
    onNavigateBack: () -> Unit = {},
) {
    val viewModel = remember { SharedTripViewModel(app, serverUrl, shareToken) }
    val uiState by viewModel.uiState.collectAsState()

    when {
        uiState.isLoading -> LoadingState()
        uiState.error != null && uiState.detail == null -> ErrorState(
            message = uiState.error!!,
            onRetry = { viewModel.load() },
        )
        uiState.detail != null -> {
            SharedTripContent(
                detail = uiState.detail!!,
                isPinned = uiState.isPinned,
                isSaving = uiState.isSaving,
                onNavigateBack = onNavigateBack,
                onPin = { viewModel.pin() },
                onUnpin = { viewModel.unpin() },
            )
        }
    }
}

@Composable
private fun SharedTripContent(
    detail: ApiTripDetail,
    isPinned: Boolean,
    isSaving: Boolean,
    onNavigateBack: () -> Unit,
    onPin: () -> Unit,
    onUnpin: () -> Unit,
) {
    var lightboxUrl by remember { mutableStateOf<String?>(null) }
    var lightboxCredit by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    val carouselState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val navBarPadding = WindowInsets.navigationBars.asPaddingValues()
    val navBarHeight = navBarPadding.calculateBottomPadding()
    val carouselHeight = 56.dp + navBarHeight

    val dayIndexMap = remember(detail.data) { buildDayIndexMap(detail.data) }
    val numDays = remember(detail.data) { totalDays(detail.data) }

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
            carouselState.animateScrollToItem(maxOf(0, visibleDay - 2))
        }
    }

    LaunchedEffect(detail.data) {
        val todayIdx = todayDayIndex(detail.data)
        if (todayIdx >= 0 && dayIndexMap.isNotEmpty()) {
            selectedDay = todayIdx
            listState.animateScrollToItem(dayIndexMap[todayIdx])
            carouselState.animateScrollToItem(maxOf(0, todayIdx - 2))
        }
    }

    val summary = remember(detail) {
        TripSummary(
            id = detail.id,
            title = detail.title,
            startDate = detail.startDate,
            endDate = detail.endDate,
            homeLocation = detail.homeLocation,
            timezone = detail.timezone,
            coverColor = detail.coverColor,
            coverImageUrl = detail.coverImageUrl,
            coverImageCredit = detail.coverImageCredit,
            legCount = detail.legCount,
            updatedAt = detail.updatedAt,
            lastSyncedAt = System.currentTimeMillis(),
            hasDetail = true,
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = carouselHeight),
        ) {
            TripTimeline(
                summary = summary,
                data = detail.data,
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
                data = detail.data,
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
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }

        // Floating top bar: back + save/unsave
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent),
                    ),
                )
                .statusBarsPadding()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(24.dp)
                        .padding(end = 12.dp),
                    color = Color.White,
                    strokeWidth = 2.dp,
                )
            } else {
                IconButton(onClick = if (isPinned) onUnpin else onPin) {
                    Icon(
                        imageVector = if (isPinned) Icons.Filled.Bookmark else Icons.Outlined.BookmarkAdd,
                        contentDescription = if (isPinned) "Remove from my trips" else "Save offline",
                        tint = Color.White,
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
