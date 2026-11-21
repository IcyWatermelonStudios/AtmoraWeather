package com.icywatermelonstudios.atmoraweather.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.icywatermelonstudios.atmoraweather.data.model.UnitSettings
import com.icywatermelonstudios.atmoraweather.data.model.WeatherCode
import com.icywatermelonstudios.atmoraweather.data.model.WeatherReport
import com.icywatermelonstudios.atmoraweather.ui.WeatherState
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.roundToInt
import androidx.compose.ui.platform.LocalLocale

// formatting helpers shared with daily view and hourly view

internal fun formatTemp(value: Double): String = "${value.roundToInt()}°"
internal fun formatWind(value: Double, units: UnitSettings): String = "${value.roundToInt()} ${units.windSpeed.label}"
internal fun formatPrecipitation(value: Double, units: UnitSettings): String = "${String.format(Locale.getDefault(), "%.1f", value)} ${units.precipitation.label}"
internal val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

// screen

@Composable
fun OverviewScreen(weather: WeatherState, onRefresh: () -> Unit) {
    StateHost(weather = weather, onRefresh = onRefresh) { report ->
        OverviewContent(report)
    }
}

@Composable
private fun OverviewContent(report: WeatherReport) {
    val current = report.current
    val units = report.units
    // daily list starts with today
    val today = report.daily.firstOrNull()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (report.location.subtitle.isNotEmpty()) {
            Text(
                text = report.location.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Text(
            text = formatTemp(current.temperature),
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 112.sp,
                fontWeight = FontWeight.Light,
            ),
            modifier = Modifier.padding(top = 8.dp),
        )

        Text(
            text = WeatherCode.describe(current.weatherCode),
            style = MaterialTheme.typography.headlineSmall,
        )

        Text(
            text = "Feels like ${formatTemp(current.apparentTemperature)}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )

        if (today != null) {
            Text(
                text = "High ${formatTemp(today.temperatureMax)}  ·  Low ${formatTemp(today.temperatureMin)}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }

        // detail tiles
        val details = buildList {
            add("Humidity" to "${current.humidity}%")
            add("Wind" to "${formatWind(current.windSpeed, units)} ${current.windDirectionLabel}")
            add("Wind gusts" to formatWind(current.windGusts, units))
            add("Pressure" to "${current.pressure.roundToInt()} hPa")
            add("Cloud cover" to "${current.cloudCover}%")
            add("Precipitation" to formatPrecipitation(current.precipitation, units))
            today?.precipitationProbabilityMax?.let { add("Chance of rain" to "$it%") }
            today?.uvIndexMax?.let { add("UV index" to String.format(LocalLocale.current.platformLocale, "%.1f", it)) }
            today?.sunrise?.let { add("Sunrise" to timeFormatter.format(it)) }
            today?.sunset?.let { add("Sunset" to timeFormatter.format(it)) }
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            details.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    InfoTile(label = pair[0].first, value = pair[0].second, modifier = Modifier.weight(1f))
                    if (pair.size > 1) {
                        InfoTile(label = pair[1].first, value = pair[1].second, modifier = Modifier.weight(1f))
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        Text(
            text = "Updated ${timeFormatter.format(report.fetchedAt)}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
        )
    }
}

// a single rounded tile with a small label on top of a larger value (apple weather ahh style)
@Composable
private fun InfoTile(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}