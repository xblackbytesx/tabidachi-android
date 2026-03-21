package com.example.tabidachi.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.tabidachi.ui.theme.DayArrival
import com.example.tabidachi.ui.theme.DayDeparture
import com.example.tabidachi.ui.theme.DayFlexible
import com.example.tabidachi.ui.theme.DayRest
import com.example.tabidachi.ui.theme.DayTravel

@Composable
fun DayTypeBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.15f),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

fun dayTypeColor(type: String): Color = when (type) {
    "arrival" -> DayArrival
    "departure" -> DayDeparture
    "travel" -> DayTravel
    "rest" -> DayRest
    "flexible" -> DayFlexible
    else -> Color.Transparent
}

fun dayTypeLabel(type: String): String? = when (type) {
    "arrival" -> "Arrival"
    "departure" -> "Departure"
    "travel" -> "Travel"
    "rest" -> "Rest"
    "flexible" -> "Flexible"
    else -> null
}
