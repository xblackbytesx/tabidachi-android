package com.example.tabidachi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.tabidachi.network.ApiEvent
import com.example.tabidachi.ui.theme.AccommodationAmber
import com.example.tabidachi.ui.theme.DayDeparture
import com.example.tabidachi.ui.theme.IndigoAccent
import com.example.tabidachi.ui.theme.SuccessGreen
import com.example.tabidachi.ui.theme.TextMuted
import com.example.tabidachi.ui.theme.TextSecondary

@Composable
fun EventCard(
    event: ApiEvent,
    onImageClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val isAccommodation = event.type == "accommodation"
    val bgColor = if (isAccommodation) AccommodationAmber.copy(alpha = 0.06f)
    else MaterialTheme.colorScheme.surfaceVariant
    val borderColor = if (isAccommodation) AccommodationAmber.copy(alpha = 0.2f)
    else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        // Thumbnail (activity only)
        val thumbUrl = (event.imageThumbUrl ?: event.imageUrl)?.takeIf { it.isNotBlank() }
        val fullUrl = event.imageUrl?.takeIf { it.isNotBlank() }

        if (thumbUrl != null && event.type == "activity") {
            AsyncImage(
                model = thumbUrl,
                contentDescription = event.title,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                        if (fullUrl != null && onImageClick != null) {
                            onImageClick(fullUrl)
                        }
                    },
                contentScale = ContentScale.Crop,
            )
            Spacer(modifier = Modifier.width(12.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            // Title row with badges
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = if (isAccommodation) Icons.Default.Hotel
                    else Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (isAccommodation) AccommodationAmber else TextMuted,
                )
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (event.optional) {
                    DayTypeBadge(text = "Optional", color = IndigoAccent)
                }
                if (event.checkIn) {
                    DayTypeBadge(text = "Check-in", color = SuccessGreen)
                }
                if (event.checkOut) {
                    DayTypeBadge(text = "Check-out", color = DayDeparture)
                }
            }

            // Time
            val timeText = formatEventTime(event.startTime, event.endTime)
            if (timeText != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }

            // Location
            if (event.location != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = event.location,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }

            // Booking reference
            if (event.bookingReference != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Ref: ${event.bookingReference}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                )
            }

            // Notes
            if (event.notes != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = event.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
        }
    }
}

private fun formatEventTime(start: String?, end: String?): String? {
    if (start.isNullOrBlank()) return null
    return if (end.isNullOrBlank()) start else "$start — $end"
}
