package com.icywatermelonstudios.atmoraweather.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.icywatermelonstudios.atmoraweather.data.model.DailyForecast
import com.icywatermelonstudios.atmoraweather.data.model.UnitSettings
import com.icywatermelonstudios.atmoraweather.data.model.WeatherCode
import com.icywatermelonstudios.atmoraweather.ui.WeatherState
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

internal val dayFormatter = DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.ENGLISH)

@Composable
fun DailyScreen(weather: WeatherState, onRefresh: () -> Unit) {
    StateHost(weather = weather, onRefresh = onRefresh) { report ->
        val today = report.current.time.toLocalDate()

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(report.daily, key = { it.date }) { day ->
                DailyRow(day = day, today = today, units = report.units)
            }
        }
    }
}

@Composable
private fun DailyRow(day: DailyForecast, today: LocalDate, units: UnitSettings) {
    // Short secondary line built from whatever data is available, e.g. "Rain 40% · 1.2 mm · Wind 18 km/h"
    val details = buildList {
        day.precipitationProbabilityMax?.let { add("Rain $it%") }
        if (day.precipitationSum > 0.0) add(formatPrecipitation(day.precipitationSum, units))
        add("Wind ${formatWind(day.windSpeedMax, units)}")
    }.joinToString("  ·  ")

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Left side takes all the free width; the temperatures stay pinned to the right
            Column(Modifier.weight(1f)) {
                Text(
                    text = dayLabel(day.date, today),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = WeatherCode.describe(day.weatherCode),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = details,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            // high is emphasized, low is dimmed
            Row {
                Text(
                    text = formatTemp(day.temperatureMax),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = " / ${formatTemp(day.temperatureMin)}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

internal fun dayLabel(date: LocalDate, today: LocalDate): String = when (date) {
    today -> "Today"
    today.plusDays(1) -> "Tomorrow"
    else -> dayFormatter.format(date)
}