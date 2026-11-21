package com.icywatermelonstudios.atmoraweather.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.icywatermelonstudios.atmoraweather.data.model.HourlyForecast
import com.icywatermelonstudios.atmoraweather.data.model.UnitSettings
import com.icywatermelonstudios.atmoraweather.data.model.WeatherCode
import com.icywatermelonstudios.atmoraweather.ui.WeatherState

@Composable
fun HourlyScreen(weather: WeatherState, onRefresh: () -> Unit) {
    StateHost(weather = weather, onRefresh = onRefresh) { report ->
        // "Today" at the forecast location, see DailyScreen for why not LocalDate.now()
        val today = report.current.time.toLocalDate()

        // groupBy keeps insertion order, so days stay chronological
        val days = report.hourly.groupBy { it.time.toLocalDate() }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            days.forEach { (date, hours) ->
                // One header and one card per day; the keys keep list state stable across refreshes
                item(key = "header-$date") {
                    Text(
                        text = dayLabel(date, today),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 4.dp, top = 8.dp),
                    )
                }
                item(key = "card-$date") {
                    DayCard(
                        hours = hours,
                        units = report.units,
                        // The forecast starts at the current hour, so the first row of today is "Now"
                        firstRowIsNow = date == today,
                    )
                }
            }
        }
    }
}

// A card holding all hours of one day, separated by thin dividers.
@Composable
private fun DayCard(hours: List<HourlyForecast>, units: UnitSettings, firstRowIsNow: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        hours.forEachIndexed { index, hour ->
            if (index > 0) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
            HourRow(
                hour = hour,
                units = units,
                label = if (index == 0 && firstRowIsNow) "Now" else timeFormatter.format(hour.time),
            )
        }
    }
}

@Composable
private fun HourRow(hour: HourlyForecast, units: UnitSettings, label: String) {
    // Second line, e.g. "Rain 20%  ·  Wind 12 km/h"
    val details = buildList {
        hour.precipitationProbability?.let { add("Rain $it%") }
        add("Wind ${formatWind(hour.windSpeed, units)}")
    }.joinToString("  ·  ")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Fixed width keeps the condition column aligned between rows
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.width(84.dp),
        )

        Column(Modifier.weight(1f)) {
            Text(
                text = WeatherCode.describe(hour.weatherCode),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = details,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Text(
            text = formatTemp(hour.temperature),
            style = MaterialTheme.typography.titleLarge,
        )
    }
}