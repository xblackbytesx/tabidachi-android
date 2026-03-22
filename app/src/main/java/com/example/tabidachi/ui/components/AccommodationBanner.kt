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
    val stayInfo = formatStayInfo(accommodation.checkIn, accommodation.checkOut)

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
                text = stayInfo,
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

private fun parseDate(isoDateTime: String): java.time.LocalDate? {
    return try {
        java.time.LocalDate.parse(isoDateTime.substringBefore("T"))
    } catch (_: Exception) {
        null
    }
}

private fun formatDayShort(date: java.time.LocalDate): String {
    val dow = date.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH)
    val day = date.dayOfMonth
    val suffix = when {
        day in 11..13 -> "th"
        day % 10 == 1 -> "st"
        day % 10 == 2 -> "nd"
        day % 10 == 3 -> "rd"
        else -> "th"
    }
    return "$dow $day$suffix"
}

private fun formatStayInfo(checkIn: String, checkOut: String): String {
    val inDate = parseDate(checkIn)
    val outDate = parseDate(checkOut)
    if (inDate == null || outDate == null) return "$checkIn – $checkOut"
    val nights = java.time.temporal.ChronoUnit.DAYS.between(inDate, outDate)
    val nightLabel = if (nights == 1L) "1 night" else "$nights nights"
    return "in ${formatDayShort(inDate)}  ·  out ${formatDayShort(outDate)} ($nightLabel)"
}
