package com.example.tabidachi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.tabidachi.network.ApiAccommodation
import com.example.tabidachi.ui.theme.AccommodationAmber
import com.example.tabidachi.ui.theme.TextSecondary

@Composable
fun AccommodationBanner(
    accommodation: ApiAccommodation,
    modifier: Modifier = Modifier,
) {
    val checkInFormatted = formatAccommodationTime(accommodation.checkIn)
    val checkOutFormatted = formatAccommodationTime(accommodation.checkOut)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(AccommodationAmber.copy(alpha = 0.08f))
            .border(1.dp, AccommodationAmber.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = Icons.Default.Hotel,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = AccommodationAmber,
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = accommodation.name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )

            if (accommodation.neighborhood != null) {
                Text(
                    text = accommodation.neighborhood,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Check-in: $checkInFormatted",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )

            Text(
                text = "Check-out: $checkOutFormatted",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )

            if (accommodation.bookingReference != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Ref: ${accommodation.bookingReference}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                )
            }
        }
    }
}

private fun formatAccommodationTime(isoDateTime: String): String {
    // Input: "2025-04-17T15:00" → "Apr 17, 3:00 PM"
    return try {
        val parts = isoDateTime.split("T")
        if (parts.size == 2) {
            val date = java.time.LocalDate.parse(parts[0])
            val time = java.time.LocalTime.parse(parts[1])
            val dateFmt = java.time.format.DateTimeFormatter.ofPattern("MMM d")
            val timeFmt = java.time.format.DateTimeFormatter.ofPattern("h:mm a")
            "${date.format(dateFmt)}, ${time.format(timeFmt)}"
        } else {
            isoDateTime
        }
    } catch (_: Exception) {
        isoDateTime
    }
}
