package com.example.tabidachi.ui.components

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
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
import com.example.tabidachi.ui.theme.TicketOrange

@Composable
fun EventCard(
    event: ApiEvent,
    onImageClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
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
                if (event.ticketRequired) {
                    DayTypeBadge(text = "Ticket", color = TicketOrange)
                }
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
            val timeText = formatTimeRange(event.startTime, event.endTime)
            if (timeText != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }

            // Location (clickable when coordinates or address available)
            if (event.location != null) {
                Spacer(modifier = Modifier.height(4.dp))
                val hasCoords = !event.latitude.isNullOrBlank() && !event.longitude.isNullOrBlank()
                val isClickable = hasCoords || !event.address.isNullOrBlank()

                Text(
                    text = event.location,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isClickable) IndigoAccent else TextSecondary,
                    modifier = if (isClickable) Modifier.clickable {
                        val uri = if (hasCoords) {
                            Uri.parse("geo:${event.latitude},${event.longitude}?q=${event.latitude},${event.longitude}(${Uri.encode(event.location)})")
                        } else {
                            Uri.parse("geo:0,0?q=${Uri.encode(event.address ?: event.location)}")
                        }
                        try { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) } catch (_: Exception) {}
                    } else Modifier,
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

            // URL link
            if (!event.url.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        val uri = if (event.url.startsWith("http://") || event.url.startsWith("https://")) {
                            Uri.parse(event.url)
                        } else {
                            Uri.parse("https://${event.url}")
                        }
                        try { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) } catch (_: Exception) {}
                    },
                ) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = IndigoAccent,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = event.url.removePrefix("https://").removePrefix("http://").trimEnd('/'),
                        style = MaterialTheme.typography.labelSmall,
                        color = IndigoAccent,
                        textDecoration = TextDecoration.Underline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
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

internal fun formatTimeRange(start: String?, end: String?): String? {
    if (start.isNullOrBlank()) return null
    return if (end.isNullOrBlank()) start else "$start — $end"
}
