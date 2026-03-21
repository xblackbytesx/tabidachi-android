package com.example.tabidachi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.tabidachi.network.ApiEvent
import com.example.tabidachi.ui.theme.TextMuted
import com.example.tabidachi.ui.theme.TextSecondary
import com.example.tabidachi.ui.theme.TransitBlue

@Composable
fun TransitCard(
    event: ApiEvent,
    modifier: Modifier = Modifier,
) {
    val transport = transportInfo(event.transportMode)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(TransitBlue.copy(alpha = 0.06f))
            .border(1.dp, TransitBlue.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        // Transport icon in colored circle
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(transport.color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = transport.icon,
                contentDescription = event.transportMode,
                modifier = Modifier.size(22.dp),
                tint = transport.color,
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = event.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )

            // Route: Departure → Arrival
            if (event.departure != null && event.arrival != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val depText = buildString {
                        append(event.departure.location)
                        if (event.departure.code != null) append(" (${event.departure.code})")
                    }
                    val arrText = buildString {
                        append(event.arrival.location)
                        if (event.arrival.code != null) append(" (${event.arrival.code})")
                    }

                    Text(
                        text = depText,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(12.dp),
                        tint = TextMuted,
                    )
                    Text(
                        text = arrText,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
            }

            // Time + Duration + Carrier
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val timeText = formatTransitTime(event.startTime, event.endTime)
                if (timeText != null) {
                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
                if (event.duration != null) {
                    Text(
                        text = formatDuration(event.duration),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                    )
                }
                val carrierText = buildString {
                    if (event.carrier != null) append(event.carrier)
                    if (event.flightNumber != null) {
                        if (isNotEmpty()) append(" ")
                        append(event.flightNumber)
                    }
                }
                if (carrierText.isNotBlank()) {
                    Text(
                        text = carrierText,
                        style = MaterialTheme.typography.labelSmall,
                        color = transport.color,
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

private fun formatTransitTime(start: String?, end: String?): String? {
    if (start.isNullOrBlank()) return null
    return if (end.isNullOrBlank()) start else "$start — $end"
}

private fun formatDuration(iso: String): String {
    // Parse ISO8601 duration like "PT9H45M" → "9h 45m"
    return try {
        val duration = java.time.Duration.parse(iso)
        val hours = duration.toHours()
        val minutes = duration.toMinutesPart()
        buildString {
            if (hours > 0) append("${hours}h")
            if (minutes > 0) {
                if (isNotEmpty()) append(" ")
                append("${minutes}m")
            }
            if (isEmpty()) append("0m")
        }
    } catch (_: Exception) {
        iso
    }
}
