package com.example.tabidachi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.tabidachi.data.TripSummary
import com.example.tabidachi.ui.theme.IndigoAccent
import com.example.tabidachi.ui.theme.SuccessGreen
import com.example.tabidachi.ui.theme.TextSecondary
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun TripCard(
    trip: TripSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isActive = try {
        val today = LocalDate.now()
        val start = LocalDate.parse(trip.startDate)
        val end = LocalDate.parse(trip.endDate)
        !today.isBefore(start) && !today.isAfter(end)
    } catch (_: Exception) {
        false
    }

    val dateRange = try {
        val start = LocalDate.parse(trip.startDate)
        val end = LocalDate.parse(trip.endDate)
        val fmt = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        "${start.format(fmt)} — ${end.format(fmt)}"
    } catch (_: Exception) {
        "${trip.startDate} — ${trip.endDate}"
    }

    val accentColor = try {
        if (trip.coverColor != null) Color(android.graphics.Color.parseColor(trip.coverColor))
        else IndigoAccent
    } catch (_: Exception) {
        IndigoAccent
    }

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (trip.coverImageUrl != null) {
                AsyncImage(
                    model = trip.coverImageUrl,
                    contentDescription = trip.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                // Gradient scrim
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.3f),
                                    Color.Black.copy(alpha = 0.8f),
                                ),
                            ),
                        ),
                )
            } else {
                // Accent color top bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(accentColor),
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.Bottom,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = trip.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = if (trip.coverImageUrl != null) Color.White
                        else MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )

                    if (isActive) {
                        Spacer(modifier = Modifier.width(8.dp))
                        DayTypeBadge(
                            text = "Active",
                            color = SuccessGreen,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                val contentColor = if (trip.coverImageUrl != null) Color.White.copy(alpha = 0.85f)
                else TextSecondary

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = contentColor,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = dateRange,
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor,
                    )
                }

                if (trip.homeLocation != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = contentColor,
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = trip.homeLocation,
                            style = MaterialTheme.typography.bodySmall,
                            color = contentColor,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Explore,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = contentColor,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${trip.legCount} leg${if (trip.legCount != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor,
                    )
                }
            }
        }
    }
}
