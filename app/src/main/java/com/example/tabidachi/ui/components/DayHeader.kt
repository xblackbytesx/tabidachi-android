package com.example.tabidachi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.tabidachi.network.ApiDay
import com.example.tabidachi.ui.theme.IndigoAccent
import com.example.tabidachi.ui.theme.SuccessGreen
import com.example.tabidachi.ui.theme.TextSecondary
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun DayHeader(
    day: ApiDay,
    dayNumber: Int,
    isToday: Boolean,
    modifier: Modifier = Modifier,
) {
    val formattedDate = try {
        val date = LocalDate.parse(day.date)
        val dayOfWeek = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
        val formatted = date.format(DateTimeFormatter.ofPattern("MMM d"))
        "$dayOfWeek, $formatted"
    } catch (_: Exception) {
        day.date
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isToday) IndigoAccent.copy(alpha = 0.08f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Day $dayNumber".uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isToday) IndigoAccent else TextSecondary,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (day.label != null) {
                    Text(
                        text = day.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val typeLabel = dayTypeLabel(day.type)
                if (typeLabel != null) {
                    DayTypeBadge(text = typeLabel, color = dayTypeColor(day.type))
                }
                if (isToday) {
                    DayTypeBadge(text = "Today", color = SuccessGreen)
                }
            }
        }
    }
}
