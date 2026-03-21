package com.example.tabidachi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.tabidachi.network.ApiLeg
import com.example.tabidachi.ui.theme.TextSecondary
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun LegHeader(
    leg: ApiLeg,
    legIndex: Int,
    modifier: Modifier = Modifier,
) {
    val dateRange = try {
        val start = LocalDate.parse(leg.startDate)
        val end = LocalDate.parse(leg.endDate)
        val fmt = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        "${start.format(fmt)} — ${end.format(fmt)}"
    } catch (_: Exception) {
        "${leg.startDate} — ${leg.endDate}"
    }

    val hasCover = leg.coverImageUrl != null

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp)),
    ) {
        if (hasCover) {
            AsyncImage(
                model = leg.coverImageUrl,
                contentDescription = leg.destination,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentScale = ContentScale.Crop,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.2f),
                                Color.Black.copy(alpha = 0.75f),
                            ),
                        ),
                    ),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (hasCover) Modifier.height(180.dp) else Modifier
                )
                .padding(20.dp),
            verticalArrangement = if (hasCover) androidx.compose.foundation.layout.Arrangement.Bottom
            else androidx.compose.foundation.layout.Arrangement.Top,
        ) {
            Text(
                text = "Leg ${legIndex + 1}".uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = if (hasCover) Color.White.copy(alpha = 0.7f) else TextSecondary,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = leg.destination,
                style = MaterialTheme.typography.headlineMedium,
                color = if (hasCover) Color.White else MaterialTheme.colorScheme.onSurface,
            )

            if (leg.region != null) {
                Text(
                    text = leg.region,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (hasCover) Color.White.copy(alpha = 0.8f) else TextSecondary,
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = dateRange,
                style = MaterialTheme.typography.bodySmall,
                color = if (hasCover) Color.White.copy(alpha = 0.7f) else TextSecondary,
            )
        }
    }
}
